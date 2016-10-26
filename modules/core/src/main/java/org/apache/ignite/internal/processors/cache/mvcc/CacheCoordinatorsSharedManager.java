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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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
import org.apache.ignite.internal.processors.cache.GridCacheSharedManagerAdapter;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.future.GridFinishedFuture;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiTuple;

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
    private final AtomicLong cntr = new AtomicLong();

    /** */
    private final AtomicLong futIdCntr = new AtomicLong();

    /** */
    private ConcurrentMap<GridCacheVersion, Long> activeTxs = new ConcurrentHashMap<>();

    /** */
    private final ConcurrentMap<Long, IntersectTxHistory> intersectTxHist = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override protected void start0() throws IgniteCheckedException {
        super.start0();

        assignCache = new CoordinatorsAssignmentCache(cctx.kernalContext().clientNode());

        if (!cctx.kernalContext().clientNode())
            cntrFuts = new ConcurrentHashMap<>();
    }

    /** {@inheritDoc} */
    @Override protected void onKernalStart0(boolean reconnect) throws IgniteCheckedException {
        super.onKernalStart0(reconnect);

        cctx.gridEvents().addLocalEventListener(new GridLocalEventListener() {
                @Override public void onEvent(Event evt) {
                    assert evt instanceof DiscoveryEvent : evt;

                    DiscoveryEvent discoEvt = (DiscoveryEvent)evt;

                    UUID nodeId = discoEvt.eventNode().id();

                    if (cntrFuts != null) {
                        for (TxCounterFuture fut : cntrFuts.values()) {
                            if (fut.crd.id().equals(nodeId) && cntrFuts.remove(fut.id) != null)
                                fut.onDone(new ClusterTopologyCheckedException("Node failed: " + nodeId));
                        }
                    }
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
            cctx.gridIO().send(crd, TOPIC_COORDINATOR, new CoordinatorCounterRequest(fut.id, txId), SYSTEM_POOL);
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
    private void processCoordinatorCounterRequest(UUID nodeId, CoordinatorCounterRequest msg) {
        ClusterNode node = cctx.discovery().node(nodeId);

        if (node == null) {
            if (log.isDebugEnabled())
                log.debug("Ignore counter request processing, node left [msg=" + msg + ", node=" + nodeId + ']');

            return;
        }

        long nextCtr = assignTxCounter(msg.txId());

        try {
            cctx.gridIO().send(node,
                TOPIC_COORDINATOR,
                new CoordinatorCounterResponse(nextCtr, msg.futureId()),
                SYSTEM_POOL);
        }
        catch (ClusterTopologyCheckedException e) {
            if (log.isDebugEnabled())
                log.debug("Failed to send coordinator counter response, node left [msg=" + msg + ", node=" + nodeId + ']');
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to send coordinator counter response [msg=" + msg + ", node=" + nodeId + ']', e);
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
     * @param msg Message.
     */
    private void processCoordinatorAckRequest(CoordinatorAckRequest msg) {
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
    }

    /**
     * @param msg Message.
     */
    private void processCoordinatorCounterResponse(CoordinatorCounterResponse msg) {
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
        CoordinatorAckRequest msg = new CoordinatorAckRequest(txId, 0, null);

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
     * @param cntrs Counters.
     * @return Acknowledge future.
     */
    public IgniteInternalFuture<Void> ackTransactionCommit(GridCacheVersion txId,
        AffinityTopologyVersion topVer,
        Map<ClusterNode, Long> cntrs) {
        Map<UUID, Long> cntrs0 = null;

        // No need to send counters if single coordinator participated in tx.
        if (cntrs != null && cntrs.size() > 1) {
            // TODO: optimize counters store.
            cntrs0 = U.newHashMap(cntrs.size());

            for (Map.Entry<ClusterNode, Long> e : cntrs.entrySet())
                cntrs0.put(e.getKey().id(), e.getValue());
        }

        CoordinatorAckRequest msg = new CoordinatorAckRequest(txId, topVer.topologyVersion(), cntrs0);



        return null;
    }

    /**
     *
     */
    private class CoordinatorMessageListener implements GridMessageListener {
        /** {@inheritDoc} */
        @Override public void onMessage(UUID nodeId, Object msg) {
            if (msg instanceof CoordinatorCounterRequest)
                processCoordinatorCounterRequest(nodeId, (CoordinatorCounterRequest)msg);
            else if (msg instanceof CoordinatorCounterResponse)
                processCoordinatorCounterResponse((CoordinatorCounterResponse)msg);
            else if (msg instanceof CoordinatorAckRequest)
                processCoordinatorAckRequest((CoordinatorAckRequest)msg);
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
        void onResponse(CoordinatorCounterResponse msg) {
            onDone(msg.counter());
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
}
