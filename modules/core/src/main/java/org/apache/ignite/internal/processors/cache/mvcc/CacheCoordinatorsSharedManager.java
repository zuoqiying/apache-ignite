/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.mvcc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.cluster.ClusterTopologyCheckedException;
import org.apache.ignite.internal.managers.communication.GridMessageListener;
import org.apache.ignite.internal.managers.eventstorage.GridLocalEventListener;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCacheSharedManagerAdapter;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.future.GridFinishedFuture;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiTuple;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.events.EventType.EVT_NODE_FAILED;
import static org.apache.ignite.events.EventType.EVT_NODE_LEFT;
import static org.apache.ignite.internal.GridTopic.TOPIC_COORDINATOR;
import static org.apache.ignite.internal.managers.communication.GridIoPolicy.SYSTEM_POOL;

/**
 *
 */
public class CacheCoordinatorsSharedManager<K, V> extends GridCacheSharedManagerAdapter<K, V> {
    /** */
    private static final int ASSIGN_HIST_SIZE = 1000;

    /** */
    private CoordinatorsAssignmentCache assignCache;

    /** */
    private ConcurrentMap<Long, TxCounterFuture> cntrFuts;

    /** */
    private ConcurrentMap<Long, AckFuture> ackFuts;

    /** */
    private ConcurrentMap<Long, QueryCountersFuture> qryCntrFuts;

    /** */
    private final AtomicLong cntr = new AtomicLong();

    /** */
    private final AtomicLong futIdCntr = new AtomicLong();

    /** */
    private ConcurrentMap<GridCacheVersion, Long> activeTxs = new ConcurrentHashMap<>();

    /** */
    private ConcurrentMap<Long, IntersectTxHistory> intersectTxHist;

    /** {@inheritDoc} */
    @Override protected void start0() throws IgniteCheckedException {
        super.start0();

        assignCache = new CoordinatorsAssignmentCache(cctx.kernalContext().clientNode());

        cntrFuts = new ConcurrentHashMap<>();

        ackFuts = new ConcurrentHashMap<>();

        qryCntrFuts = new ConcurrentHashMap<>();

        if (!cctx.kernalContext().clientNode())
            intersectTxHist = new ConcurrentHashMap<>();
    }

    /** {@inheritDoc} */
    @Override protected void onKernalStart0(boolean reconnect) throws IgniteCheckedException {
        super.onKernalStart0(reconnect);

        cctx.gridEvents().addLocalEventListener(new GridLocalEventListener() {
                @Override public void onEvent(Event evt) {
                    assert evt instanceof DiscoveryEvent : evt;

                    DiscoveryEvent discoEvt = (DiscoveryEvent)evt;

                    UUID nodeId = discoEvt.eventNode().id();

                    for (TxCounterFuture fut : cntrFuts.values())
                        fut.onNodeLeft(nodeId);

                    for (AckFuture fut : ackFuts.values())
                        fut.onNodeLeft(nodeId);

                    for (QueryCountersFuture fut : qryCntrFuts.values())
                        fut.onNodeLeft(nodeId);
                }
            },
            EVT_NODE_FAILED, EVT_NODE_LEFT);

        cctx.gridIO().addMessageListener(TOPIC_COORDINATOR, new CoordinatorMessageListener());
    }

    /** {@inheritDoc} */
    @Override protected void onKernalStop0(boolean cancel) {
        cctx.gridIO().removeMessageListener(TOPIC_COORDINATOR);

        super.onKernalStop0(cancel);
    }

    /**
     * @param topVer Topology version.
     * @return Counter request future.
     */
    public ClusterNode localNodeCoordinator(AffinityTopologyVersion topVer) {
        return assignCache.localNodeCoordinator(topVer);
    }

    /**
     * @param cctx Cache context.
     * @param keys Keys.
     * @param topVer Topology version.
     * @return Future.
     */
    public IgniteInternalFuture<MvccQueryVersions> requestQueryCounters(GridCacheContext cctx,
        Collection<KeyCacheObject> keys,
        AffinityTopologyVersion topVer) {
        QueryCountersFuture fut = new QueryCountersFuture(futIdCntr.getAndIncrement());

        qryCntrFuts.put(fut.id, fut);

        fut.map(cctx, keys, topVer);

        return fut;
    }

    /**
     * @param mvccVers Query counters.
     */
    public void ackQueryDone(MvccQueryVersions mvccVers) {
        for (Map.Entry<UUID, Long> crdCntr : mvccVers.counters().entrySet()) {
            try {
                cctx.gridIO().send(crdCntr.getKey(),
                    TOPIC_COORDINATOR,
                    new CoordinatorQueryAckRequest(crdCntr.getValue()),
                    SYSTEM_POOL);
            }
            catch (ClusterTopologyCheckedException e) {
                if (log.isDebugEnabled())
                    log.debug("Failed to send query ack, node left [crd=" + crdCntr.getKey() + ']');
            }
            catch (IgniteCheckedException e) {
                U.error(log, "Failed to send query ack [crd=" + crdCntr.getKey() +
                    ", cntr=" + crdCntr.getValue() + ']', e);
            }
        }
    }

    /**
     * @param crd Coordinator.
     * @param txId Transaction ID.
     * @return Counter request future.
     */
    public IgniteInternalFuture<Long> requestTxCounter(ClusterNode crd, GridCacheVersion txId) {
        if (crd.equals(cctx.localNode()))
            return new GridFinishedFuture<>(assignTxCounter(txId));

        TxCounterFuture fut = new TxCounterFuture(futIdCntr.incrementAndGet(), crd);

        cntrFuts.put(fut.id, fut);

        try {
            cctx.gridIO().send(crd, TOPIC_COORDINATOR, new CoordinatorTxCounterRequest(fut.id, txId), SYSTEM_POOL);
        }
        catch (IgniteCheckedException e) {
            if (cntrFuts.remove(fut.id) != null)
                fut.onDone(e);
        }

        return fut;
    }

    /**
     * @param topVer Topology version.
     * @param nodeId Node ID.
     * @return Node coordinator.
     */
    public ClusterNode nodeCoordinator(AffinityTopologyVersion topVer, UUID nodeId) {
        CoordinatorsAssignment assign = assignCache.assignment(topVer);

        return assign.nodeCoordinator(nodeId);
    }

    /**
     * @param topVer Topology version.
     * @param discoEvt Discovery event.
     */
    public void assignCoordinators(AffinityTopologyVersion topVer, DiscoveryEvent discoEvt) {
        // TODO: reassign only on server join/fail.
        // TODO: another(configurable?) assignment logic.

        List<ClusterNode> nodes = cctx.discovery().serverNodes(topVer);

        ClusterNode coord = null;

        Map<UUID, ClusterNode> assignment = U.newHashMap(nodes.size());

        int coordsCnt = 0;

        for (int i = 0; i < nodes.size(); i++) {
            if (i % 3 == 0) {
                coord = nodes.get(i);

                coordsCnt++;
            }

            assignment.put(nodes.get(i).id(), coord);
        }

        assignCache.newAssignment(topVer, cctx.localNode(), new CoordinatorsAssignment(assignment, coordsCnt));
    }

    /**
     * @param nodeId Sender node ID.
     * @param msg Message.
     */
    private void processCoordinatorQueryAckRequest(UUID nodeId, CoordinatorQueryAckRequest msg) {
        // TODO: remove counter from 'in-use' list.
    }

    private void processCoordinatorQueryAckRequest(UUID nodeId, long cntr) {
        // TODO: remove counter from 'in-use' list.
    }

    /**
     * @param nodeId Sender node ID.
     * @param msg Message.
     */
    private void processCoordinatorQueryStateRequest(UUID nodeId, CoordinatorQueryStateRequest msg) {
        ClusterNode node = cctx.discovery().node(nodeId);

        if (node == null) {
            if (log.isDebugEnabled())
                log.debug("Ignore query counter request processing, node left [msg=" + msg + ", node=" + nodeId + ']');

            return;
        }

        CoordinatorQueryStateResponse res = requestQueryState(msg.futureId());

        try {
            cctx.gridIO().send(nodeId,
                TOPIC_COORDINATOR,
                res,
                SYSTEM_POOL);
        }
        catch (ClusterTopologyCheckedException e) {
            if (log.isDebugEnabled())
                log.debug("Failed to send query counter response, node left [msg=" + msg + ", node=" + nodeId + ']');
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to send query counter response [msg=" + msg + ", node=" + nodeId + ']', e);
        }
    }

    /**
     * @param futId Future ID.
     * @return Response message.
     */
    private CoordinatorQueryStateResponse requestQueryState(long futId) {
        // TODO: need mark counter as 'in-use'.
        // TODO: do not return counter from 'activeTxs'.
        // TODO: return transactions history data.
        // TODO: handle sender node failures.

        long qryCntr = cntr.get();

        Collection<GridCacheVersion> qryActiveTxs = new ArrayList<>();

        for (GridCacheVersion ver : activeTxs.keySet())
            qryActiveTxs.add(ver);

        return new CoordinatorQueryStateResponse(futId, qryCntr, qryActiveTxs);
    }

    /**
     * @param nodeId Sender node ID.
     * @param msg Message.
     */
    private void processCoordinatorQueryStateResponse(UUID nodeId, CoordinatorQueryStateResponse msg) {
        QueryCountersFuture fut = qryCntrFuts.get(msg.futureId());

        if (fut != null)
            fut.onResponse(nodeId, msg);
        else {
            if (cctx.discovery().alive(nodeId))
                U.warn(log, "Failed to find query counter future [node=" + nodeId + ", msg=" + msg + ']');
            else if (log.isDebugEnabled())
                log.debug("Failed to find query counter future [node=" + nodeId + ", msg=" + msg + ']');
        }
    }

    /**
     * @param nodeId Sender node ID.
     * @param msg Message.
     */
    private void processCoordinatorTxCounterRequest(UUID nodeId, CoordinatorTxCounterRequest msg) {
        ClusterNode node = cctx.discovery().node(nodeId);

        if (node == null) {
            if (log.isDebugEnabled())
                log.debug("Ignore tx counter request processing, node left [msg=" + msg + ", node=" + nodeId + ']');

            return;
        }

        long nextCtr = assignTxCounter(msg.txId());

        try {
            cctx.gridIO().send(node,
                TOPIC_COORDINATOR,
                new CoordinatorTxCounterResponse(nextCtr, msg.futureId()),
                SYSTEM_POOL);
        }
        catch (ClusterTopologyCheckedException e) {
            if (log.isDebugEnabled())
                log.debug("Failed to send tx counter response, node left [msg=" + msg + ", node=" + nodeId + ']');
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to send tx counter response [msg=" + msg + ", node=" + nodeId + ']', e);
        }
    }

    /**
     * @param txId Transaction ID.
     * @return Counter.
     */
    private long assignTxCounter(GridCacheVersion txId) {
        long nextCtr = cntr.incrementAndGet();

        Long old = activeTxs.put(txId, nextCtr);

        assert old == null : txId;

        return nextCtr;
    }

    /**
     * @param nodeId Sender node ID.
     * @param msg Message.
     */
    private void processCoordinatorTxAckResponse(UUID nodeId, CoordinatorTxAckResponse msg) {
        AckFuture fut = ackFuts.get(msg.futureId());

        if (fut != null)
            fut.onResponse(nodeId);
        else
            U.warn(log, "Failed to find coordinator ack future: " + msg);
    }

    /**
     * @param nodeId Sender node ID.
     * @param msg Message.
     */
    private void processCoordinatorTxAckRequest(UUID nodeId, CoordinatorTxAckRequest msg) {
        activeTxs.remove(msg.txId());

        if (msg.coordinatorCounters() != null) {
            Long topVer = msg.topologyVersion();

            IntersectTxHistory hist = intersectTxHist.get(topVer);

            if (hist == null) {
                IntersectTxHistory old = intersectTxHist.putIfAbsent(topVer, hist = new IntersectTxHistory());

                if (old != null)
                    hist = old;
            }

            hist.addCounters(msg.coordinatorCounters());
        }

        if (!msg.skipResponse()) {
            try {
                cctx.gridIO().send(nodeId,
                    TOPIC_COORDINATOR,
                    new CoordinatorTxAckResponse(msg.futureId()),
                    SYSTEM_POOL);
            }
            catch (ClusterTopologyCheckedException e) {
                if (log.isDebugEnabled())
                    log.debug("Failed to send tx ack response, node left [msg=" + msg + ", node=" + nodeId + ']');
            }
            catch (IgniteCheckedException e) {
                U.error(log, "Failed to send tx ack response [msg=" + msg + ", node=" + nodeId + ']', e);
            }
        }
    }

    /**
     * @param msg Message.
     */
    private void processCoordinatorTxCounterResponse(CoordinatorTxCounterResponse msg) {
        TxCounterFuture fut = cntrFuts.remove(msg.futureId());

        if (fut != null)
            fut.onResponse(msg);
        else
            U.warn(log, "Failed to find coordinator counter future: " + msg);
    }

    /**
     * @param txId Transaction ID.
     * @param crds Coordinators.
     */
    public void ackTransactionRollback(GridCacheVersion txId, Collection<ClusterNode> crds) {
        CoordinatorTxAckRequest msg = new CoordinatorTxAckRequest(0, txId, 0, null);

        msg.skipResponse(true);

        for (ClusterNode crd : crds) {
            try {
                cctx.gridIO().send(crd,
                    TOPIC_COORDINATOR,
                    msg,
                    SYSTEM_POOL);
            }
            catch (ClusterTopologyCheckedException e) {
                if (log.isDebugEnabled())
                    log.debug("Failed to send tx rollback ack, node left [msg=" + msg + ", node=" + crd.id() + ']');
            }
            catch (IgniteCheckedException e) {
                U.error(log, "Failed to send tx rollback ack [msg=" + msg + ", node=" + crd.id() + ']', e);
            }
        }
    }

    /**
     * @param txId Transaction ID.
     * @param topVer Topology version.
     * @param cntrs Counters.
     * @return Acknowledge future.
     */
    public IgniteInternalFuture<Void> ackTransactionCommit(GridCacheVersion txId,
        AffinityTopologyVersion topVer,
        Map<ClusterNode, Long> cntrs) {
        assert !F.isEmpty(cntrs);

        Map<UUID, Long> cntrs0;

        Set<UUID> crdIds = U.newHashSet(cntrs.size());

        // TODO: optimize counters store.
        // No need to send counters if single coordinator participated in tx.
        cntrs0 = cntrs.size() > 1 ? U.<UUID, Long>newHashMap(cntrs.size()) : null;

        for (Map.Entry<ClusterNode, Long> e : cntrs.entrySet()) {
            if (cntrs0 != null)
                cntrs0.put(e.getKey().id(), e.getValue());

            crdIds.add(e.getKey().id());
        }

        AckFuture fut = new AckFuture(crdIds, futIdCntr.incrementAndGet());

        ackFuts.put(fut.id, fut);

        CoordinatorTxAckRequest msg = new CoordinatorTxAckRequest(fut.id, txId, topVer.topologyVersion(), cntrs0);

        for (Map.Entry<ClusterNode, Long> entry : cntrs.entrySet()) {
            ClusterNode crd = entry.getKey();

            try {
                cctx.gridIO().send(crd,
                    TOPIC_COORDINATOR,
                    msg,
                    SYSTEM_POOL);
            }
            catch (ClusterTopologyCheckedException e) {
                fut.onNodeLeft(crd.id());

                if (log.isDebugEnabled())
                    log.debug("Failed to send tx ack, node left [msg=" + msg + ", node=" + crd.id() + ']');
            }
            catch (IgniteCheckedException e) {
                U.error(log, "Failed to send tx ack [msg=" + msg + ", node=" + crd.id() + ']', e);

                fut.onDone(e);
            }
        }

        return fut;
    }

    /**
     *
     */
    private class AckFuture extends GridFutureAdapter<Void> {
        /** */
        private final Set<UUID> nodes;

        /** */
        private final Long id;


        /**
         * @param nodes Nodes.
         * @param id Future ID.
         */
        AckFuture(Set<UUID> nodes, Long id) {
            this.nodes = nodes;
            this.id = id;
        }

        /**
         * @param nodeId Node ID.
         */
        private void onResponse(UUID nodeId) {
            boolean done;

            synchronized (nodes) {
                done = nodes.remove(nodeId) && nodes.isEmpty();
            }

            if (done)
                onDone();
        }

        /**
         * @param nodeId Node ID.
         */
        private void onNodeLeft(UUID nodeId) {
            onResponse(nodeId);
        }

        /** {@inheritDoc} */
        @Override public boolean onDone(@Nullable Void res, @Nullable Throwable err) {
            if (super.onDone(res, err)) {
                ackFuts.remove(id);

                return true;
            }

            return false;
        }
    }

    /**
     *
     */
    private class IntersectTxHistory {
        /** */
        private final ConcurrentMap<UUID, ConcurrentNavigableMap<Long, Long>> hist = new ConcurrentHashMap<>();

        /**
         * @param cntrs Counters.
         */
        void addCounters(Map<UUID, Long> cntrs) {
            Long thisCntr = cntrs.get(cctx.localNodeId());

            assert thisCntr != null : cntrs;

            for (Map.Entry<UUID, Long> e : cntrs.entrySet()) {
                if (!e.getKey().equals(cctx.localNodeId())) {
                    ConcurrentNavigableMap<Long, Long> hist0 = hist.get(e.getKey());

                    if (hist0 == null) {
                        ConcurrentNavigableMap<Long, Long> old = hist.putIfAbsent(e.getKey(), hist0 = new ConcurrentSkipListMap<>());

                        if (old != null)
                            hist0 = old;
                    }

                    hist0.put(e.getValue(), thisCntr);
                }
            }
        }
    }

    /**
     *
     */
    private class TxCounterFuture extends GridFutureAdapter<Long> {
        /** */
        private final Long id;

        /** */
        private final ClusterNode crd;

        /**
         * @param id Future ID.
         * @param crd Coordinator.
         */
        TxCounterFuture(Long id, ClusterNode crd) {
            this.id = id;
            this.crd = crd;
        }

        /**
         * @param msg Message.
         */
        void onResponse(CoordinatorTxCounterResponse msg) {
            onDone(msg.counter());
        }

        /**
         * @param nodeId Failed node ID.
         */
        void onNodeLeft(UUID nodeId) {
            if (crd.id().equals(nodeId) && cntrFuts.remove(id) != null)
                onDone(new ClusterTopologyCheckedException("Node failed: " + nodeId));
        }
    }

    /**
     *
     */
    private static class CoordinatorsAssignmentCache {
        /** */
        private volatile NavigableMap<AffinityTopologyVersion, CoordinatorsAssignment> assignHist;

        /** */
        private volatile NavigableMap<AffinityTopologyVersion, ClusterNode> locCoordHist;

        /** */
        private volatile IgniteBiTuple<AffinityTopologyVersion, ClusterNode> curLoc;

        /** */
        private final boolean client;

        /**
         * @param client Client node flag.
         */
        CoordinatorsAssignmentCache(boolean client) {
            this.client = client;

            assignHist = new TreeMap<>();

            if (!client)
                locCoordHist = new TreeMap<>();
        }

        /**
         * @param topVer Topology version.
         * @param locNode Local node.
         * @param assign Assignment,
         */
        void newAssignment(AffinityTopologyVersion topVer, ClusterNode locNode, CoordinatorsAssignment assign) {
            if (!client) {
                ClusterNode locCoord = assign.nodeCoordinator(locNode.id());

                curLoc = new IgniteBiTuple<>(topVer, locCoord);

                NavigableMap<AffinityTopologyVersion, ClusterNode> hist = new TreeMap<>(locCoordHist);

                hist.put(topVer, locCoord);

                onHistoryAdded(hist, ASSIGN_HIST_SIZE);

                this.locCoordHist = hist;
            }

            NavigableMap<AffinityTopologyVersion, CoordinatorsAssignment> hist = new TreeMap<>(assignHist);

            hist.put(topVer, assign);

            assignHist = hist;
        }

        /**
         * @param topVer Topology version.
         * @return Coordinators assignment.
         */
        CoordinatorsAssignment assignment(AffinityTopologyVersion topVer) {
            CoordinatorsAssignment assignment = assignHist.get(topVer);

            assert assignment != null;

            return assignment;
        }

        /**
         * @param topVer Topology version.
         * @return Local node coordinator.
         */
        ClusterNode localNodeCoordinator(AffinityTopologyVersion topVer) {
            assert !client;

            IgniteBiTuple<AffinityTopologyVersion, ClusterNode> curLoc = this.curLoc;

            assert curLoc != null;

            if (topVer.equals(curLoc.get1()))
                return curLoc.get2();

            ClusterNode coord = locCoordHist.get(topVer);

            assert coord != null : "No coordinator [topVer=" + topVer + ", hist=" + locCoordHist + ']';

            return coord;
        }

        /**
         * @param hist History map.
         * @param maxSize Maximum size.
         */
        private void onHistoryAdded(NavigableMap<?, ?> hist, int maxSize) {
            while (hist.size() > maxSize)
                hist.remove(hist.firstKey());
        }
    }

    /**
     *
     */
    private static class CoordinatorsAssignment {
        /** */
        final Map<UUID, ClusterNode> assignment;

        /** */
        final int coordsCnt;

        /**
         * @param assignment Assignment.
         * @param coordsCnt Number of coordinators.
         */
        CoordinatorsAssignment(Map<UUID, ClusterNode> assignment, int coordsCnt) {
            assert coordsCnt > 0;

            this.assignment = assignment;
            this.coordsCnt = coordsCnt;
        }

        /**
         * @param nodeId Node ID.
         * @return Node coordinator.
         */
        ClusterNode nodeCoordinator(UUID nodeId) {
            ClusterNode crd = assignment.get(nodeId);

            assert crd != null;

            return crd;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(CoordinatorsAssignment.class, this);
        }
    }

    /**
     *
     */
    private class QueryCountersFuture extends GridFutureAdapter<MvccQueryVersions> {
        /** */
        private final Long id;

        /** */
        private AffinityTopologyVersion topVer;

        /** */
        private boolean initDone;

        /** */
        private int resCnt;

        /** */
        private final Map<UUID, Long> crdCounters = new HashMap<>();

        /** */
        private Set<GridCacheVersion> activeTxs;

        /** */
        private Map<UUID, IgniteCheckedException> failedCrds;

        /**
         * @param id Future ID.
         */
        QueryCountersFuture(Long id) {
            this.id = id;
        }

        /**
         * @param cctx Cache context.
         * @param keys Keys to query.
         * @param topVer Topology version.
         */
        void map(GridCacheContext cctx, Collection<KeyCacheObject> keys, AffinityTopologyVersion topVer) {
            this.topVer = topVer;

            ClusterNode loc = null;

            for (KeyCacheObject key : keys) {
                ClusterNode node = cctx.affinity().primary(key, topVer);

                assert node != null;

                ClusterNode crd = nodeCoordinator(topVer, node.id());

                boolean snd = false;

                synchronized (this) {
                    if (!crdCounters.containsKey(crd.id())) {
                        snd = true;

                        crdCounters.put(crd.id(), null);
                    }
                }

                if (snd) {
                    try {
                        if (!cctx.localNodeId().equals(crd.id())) {
                            cctx.gridIO().send(crd,
                                TOPIC_COORDINATOR,
                                new CoordinatorQueryStateRequest(id),
                                SYSTEM_POOL);
                        }
                        else
                            loc = crd;

                    }
                    catch (ClusterTopologyCheckedException e) {
                        onNodeError(crd.id(), e);
                    }
                    catch (IgniteCheckedException e) {
                        U.error(log, "Failed to send query counter request [crd=" + crd.id() + ']', e);

                        onNodeError(crd.id(), e);
                    }
                }
            }

            if (loc != null) {
                CoordinatorQueryStateResponse res = requestQueryState(id);

                onResponse(loc.id(), res);
            }

            boolean done;

            synchronized (this) {
                initDone = true;

                done = resCnt == crdCounters.size();
            }

            if (done)
                completeFuture();
        }

        /**
         * @param nodeId Node ID.
         * @param msg Response.
         */
        private void onResponse(UUID nodeId, CoordinatorQueryStateResponse msg) {
            assert crdCounters.containsKey(nodeId);

            boolean done;

            synchronized (this) {
                if (failedCrds != null && !failedCrds.containsKey(nodeId))
                    return;

                crdCounters.put(nodeId, msg.counter());

                Collection<GridCacheVersion> txs = msg.activeTransactions();

                if (txs != null) {
                    if (activeTxs == null)
                        activeTxs = new HashSet<>();

                    activeTxs.addAll(txs);
                }

                resCnt++;

                done = initDone && resCnt == crdCounters.size();
            }

            if (done)
                completeFuture();
        }

        /**
         * @param nodeId Failed node ID.
         */
        private void onNodeLeft(UUID nodeId) {
            onNodeError(nodeId, null);
        }

        /**
         * @param nodeId Node ID.
         * @param e Error.
         */
        private void onNodeError(UUID nodeId, @Nullable IgniteCheckedException e) {
            boolean done;

            synchronized (this) {
                if (failedCrds != null && failedCrds.containsKey(nodeId))
                    return;

                if (!crdCounters.containsKey(nodeId) || crdCounters.get(nodeId) != null)
                    return;

                if (failedCrds == null)
                    failedCrds = new HashMap<>();

                failedCrds.put(nodeId, e != null ? e : new ClusterTopologyCheckedException("Failed to request counter, " +
                    "coordinator failed: " + nodeId));

                resCnt++;

                done = initDone && resCnt == crdCounters.size();
            }

            if (done)
                completeFuture();
        }

        /**
         *
         */
        private void completeFuture() {
            if (failedCrds != null) {
                for (Map.Entry<UUID, Long> crdCntr : crdCounters.entrySet()) {
                    if (crdCntr.getValue() != null) {
                        try {
                            UUID crdNodeId = crdCntr.getKey();

                            if (!cctx.localNodeId().equals(crdNodeId)) {
                                cctx.gridIO().send(crdNodeId,
                                    TOPIC_COORDINATOR,
                                    new CoordinatorQueryAckRequest(crdCntr.getValue()),
                                    SYSTEM_POOL);
                            }
                            else
                                processCoordinatorQueryAckRequest(cctx.localNodeId(), crdCntr.getValue());
                        }
                        catch (ClusterTopologyCheckedException e) {
                            if (log.isDebugEnabled())
                                log.debug("Failed to ack query completion, coordinator failed: " + crdCntr.getKey());
                        }
                        catch (IgniteCheckedException e) {
                            U.error(log, "Failed to ack query completion [crd=" + crdCntr.getKey() + ']');
                        }
                    }
                }

                onDone(failedCrds.values().iterator().next());
            }
            else
                onDone(new MvccQueryVersions(topVer, crdCounters, activeTxs));
        }

        /** {@inheritDoc} */
        @Override public boolean onDone(@Nullable MvccQueryVersions res, @Nullable Throwable err) {
            if (super.onDone(res, err)) {
                qryCntrFuts.remove(id);

                return true;
            }

            return false;
        }
    }

    /**
     *
     */
    private class CoordinatorMessageListener implements GridMessageListener {
        /** {@inheritDoc} */
        @Override public void onMessage(UUID nodeId, Object msg) {
            if (msg instanceof CoordinatorTxCounterRequest)
                processCoordinatorTxCounterRequest(nodeId, (CoordinatorTxCounterRequest)msg);
            else if (msg instanceof CoordinatorTxCounterResponse)
                processCoordinatorTxCounterResponse((CoordinatorTxCounterResponse)msg);
            else if (msg instanceof CoordinatorTxAckRequest)
                processCoordinatorTxAckRequest(nodeId, (CoordinatorTxAckRequest)msg);
            else if (msg instanceof CoordinatorTxAckResponse)
                processCoordinatorTxAckResponse(nodeId, (CoordinatorTxAckResponse)msg);
            else if (msg instanceof CoordinatorQueryAckRequest)
                processCoordinatorQueryAckRequest(nodeId, (CoordinatorQueryAckRequest)msg);
            else if (msg instanceof CoordinatorQueryStateRequest)
                processCoordinatorQueryStateRequest(nodeId, (CoordinatorQueryStateRequest)msg);
            else if (msg instanceof CoordinatorQueryStateResponse)
                processCoordinatorQueryStateResponse(nodeId, (CoordinatorQueryStateResponse)msg);
            else
                U.warn(log, "Unexpected message received: " + msg);
        }
    }
}
