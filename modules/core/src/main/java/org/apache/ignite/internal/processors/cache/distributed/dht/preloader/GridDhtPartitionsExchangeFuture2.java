///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.ignite.internal.processors.cache.distributed.dht.preloader;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.concurrent.locks.ReadWriteLock;
//import org.apache.ignite.IgniteCheckedException;
//import org.apache.ignite.IgniteLogger;
//import org.apache.ignite.IgniteSystemProperties;
//import org.apache.ignite.cluster.ClusterNode;
//import org.apache.ignite.events.CacheEvent;
//import org.apache.ignite.events.DiscoveryEvent;
//import org.apache.ignite.events.Event;
//import org.apache.ignite.events.EventType;
//import org.apache.ignite.internal.IgniteFutureTimeoutCheckedException;
//import org.apache.ignite.internal.IgniteInternalFuture;
//import org.apache.ignite.internal.IgniteInterruptedCheckedException;
//import org.apache.ignite.internal.cluster.ClusterTopologyCheckedException;
//import org.apache.ignite.internal.events.DiscoveryCustomEvent;
//import org.apache.ignite.internal.managers.discovery.GridDiscoveryTopologySnapshot;
//import org.apache.ignite.internal.pagemem.snapshot.SnapshotOperation;
//import org.apache.ignite.internal.pagemem.snapshot.SnapshotOperationType;
//import org.apache.ignite.internal.pagemem.snapshot.StartSnapshotOperationAckDiscoveryMessage;
//import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
//import org.apache.ignite.internal.processors.affinity.GridAffinityAssignmentCache;
//import org.apache.ignite.internal.processors.cache.CacheAffinityChangeMessage;
//import org.apache.ignite.internal.processors.cache.ClusterState;
//import org.apache.ignite.internal.processors.cache.DynamicCacheChangeBatch;
//import org.apache.ignite.internal.processors.cache.DynamicCacheChangeRequest;
//import org.apache.ignite.internal.processors.cache.DynamicCacheDescriptor;
//import org.apache.ignite.internal.processors.cache.GridCacheContext;
//import org.apache.ignite.internal.processors.cache.GridCacheMvccCandidate;
//import org.apache.ignite.internal.processors.cache.GridCacheProcessor;
//import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
//import org.apache.ignite.internal.processors.cache.distributed.dht.GridClientPartitionTopology;
//import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtPartitionTopology;
//import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtTopologyFuture;
//import org.apache.ignite.internal.processors.cache.transactions.IgniteTxKey;
//import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
//import org.apache.ignite.internal.processors.cluster.GridClusterStateProcessor;
//import org.apache.ignite.internal.processors.timeout.GridTimeoutObjectAdapter;
//import org.apache.ignite.internal.util.future.GridFutureAdapter;
//import org.apache.ignite.internal.util.tostring.GridToStringExclude;
//import org.apache.ignite.internal.util.tostring.GridToStringInclude;
//import org.apache.ignite.internal.util.typedef.CI1;
//import org.apache.ignite.internal.util.typedef.F;
//import org.apache.ignite.internal.util.typedef.T2;
//import org.apache.ignite.internal.util.typedef.internal.CU;
//import org.apache.ignite.internal.util.typedef.internal.LT;
//import org.apache.ignite.internal.util.typedef.internal.S;
//import org.apache.ignite.internal.util.typedef.internal.U;
//import org.apache.ignite.lang.IgniteInClosure;
//import org.apache.ignite.lang.IgniteRunnable;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.jsr166.ConcurrentHashMap8;
//
//import static org.apache.ignite.IgniteSystemProperties.IGNITE_THREAD_DUMP_ON_EXCHANGE_TIMEOUT;
//import static org.apache.ignite.events.EventType.EVT_NODE_FAILED;
//import static org.apache.ignite.events.EventType.EVT_NODE_JOINED;
//import static org.apache.ignite.events.EventType.EVT_NODE_LEFT;
//import static org.apache.ignite.internal.events.DiscoveryCustomEvent.EVT_DISCOVERY_CUSTOM_EVT;
//import static org.apache.ignite.internal.managers.communication.GridIoPolicy.SYSTEM_POOL;
//
///**
// * Future for exchanging partition maps.
// */
//@SuppressWarnings({"TypeMayBeWeakened", "unchecked"})
//public class GridDhtPartitionsExchangeFuture2 extends GridFutureAdapter<AffinityTopologyVersion>
//    implements Comparable<GridDhtPartitionsExchangeFuture2>, GridDhtTopologyFuture {
//    /** */
//    public static final int DUMP_PENDING_OBJECTS_THRESHOLD =
//        IgniteSystemProperties.getInteger(IgniteSystemProperties.IGNITE_DUMP_PENDING_OBJECTS_THRESHOLD, 10);
//
//    /** */
//    private static final long serialVersionUID = 0L;
//
//    /** Dummy flag. */
//    private final boolean dummy;
//
//    /** Force preload flag. */
//    private final boolean forcePreload;
//
//    /** Dummy reassign flag. */
//    private final boolean reassign;
//
//    /** Discovery event. */
//    private volatile DiscoveryEvent discoEvt;
//
//    /** */
//    @GridToStringExclude
//    private final Set<UUID> remaining = new HashSet<>();
//
//    /** */
//    @GridToStringExclude
//    private List<ClusterNode> srvNodes;
//
//    /** */
//    private ClusterNode crd;
//
//    /** ExchangeFuture id. */
//    private final GridDhtPartitionExchangeId exchId;
//
//    /** Cache context. */
//    private final GridCacheSharedContext<?, ?> cctx;
//
//    /** Busy lock to prevent activities from accessing exchanger while it's stopping. */
//    private ReadWriteLock busyLock;
//
//    /** */
//    private AtomicBoolean added = new AtomicBoolean(false);
//
//    /** Event latch. */
//    @GridToStringExclude
//    private final CountDownLatch evtLatch = new CountDownLatch(1);
//
//    /** */
//    private GridFutureAdapter<Boolean> initFut;
//
//    /** */
//    @GridToStringExclude
//    private final List<IgniteRunnable> discoEvts = new ArrayList<>();
//
//    /** */
//    private boolean init;
//
//    /** Topology snapshot. */
//    private AtomicReference<GridDiscoveryTopologySnapshot> topSnapshot = new AtomicReference<>();
//
//    /** Last committed cache version before next topology version use. */
//    private AtomicReference<GridCacheVersion> lastVer = new AtomicReference<>();
//
//    /**
//     * Messages received on non-coordinator are stored in case if this node
//     * becomes coordinator.
//     */
//    private final Map<ClusterNode, GridDhtPartitionsSingleMessage> singleMsgs = new ConcurrentHashMap8<>();
//
//    /** Messages received from new coordinator. */
//    private final Map<ClusterNode, GridDhtPartitionsFullMessage> fullMsgs = new ConcurrentHashMap8<>();
//
//    /** */
//    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
//    @GridToStringInclude
//    private volatile IgniteInternalFuture<?> partReleaseFut;
//
//    /** */
//    private final Object mux = new Object();
//
//    /** Logger. */
//    private final IgniteLogger log;
//
//    /** */
//    private CacheAffinityChangeMessage affChangeMsg;
//
//    /** */
//    private boolean clientOnlyExchange;
//
//    /** Init timestamp. Used to track the amount of time spent to complete the future. */
//    private long initTs;
//
//    /** */
//    private boolean centralizedAff;
//
//    /** Change global state exception. */
//    private Exception changeGlobalStateE;
//
//    /** Change global state exceptions. */
//    private final Map<UUID, Exception> changeGlobalStateExceptions = new ConcurrentHashMap8<>();
//
//    /** This exchange for change global state. */
//    private boolean exchangeOnChangeGlobalState;
//
//    /** */
//    private final ConcurrentMap<UUID, GridDhtPartitionsAbstractMessage> msgs = new ConcurrentHashMap8<>();
//
//    /** Forced Rebalance future. */
//    private GridFutureAdapter<Boolean> forcedRebFut;
//
//    /**
//     * Dummy future created to trigger reassignments if partition
//     * topology changed while preloading.
//     *
//     * @param cctx Cache context.
//     * @param discoEvt Discovery event.
//     * @param exchId Exchange id.
//     */
//    public GridDhtPartitionsExchangeFuture2(
//        GridCacheSharedContext cctx,
//        DiscoveryEvent discoEvt,
//        GridDhtPartitionExchangeId exchId
//    ) {
//        dummy = true;
//        forcePreload = false;
//
//        this.exchId = exchId;
//        this.discoEvt = discoEvt;
//        this.cctx = cctx;
//
//        log = cctx.logger(getClass());
//
//        onDone(exchId.topologyVersion());
//    }
//
//    /**
//     * Force preload future created to trigger reassignments if partition
//     * topology changed while preloading.
//     *
//     * @param cctx Cache context.
//     * @param discoEvt Discovery event.
//     * @param exchId Exchange id.
//     * @param forcedRebFut Forced Rebalance future.
//     */
//    public GridDhtPartitionsExchangeFuture2(GridCacheSharedContext cctx, DiscoveryEvent discoEvt,
//        GridDhtPartitionExchangeId exchId, GridFutureAdapter<Boolean> forcedRebFut) {
//        dummy = false;
//        forcePreload = true;
//
//        this.exchId = exchId;
//        this.discoEvt = discoEvt;
//        this.cctx = cctx;
//        this.forcedRebFut = forcedRebFut;
//
//        log = cctx.logger(getClass());
//
//        reassign = true;
//
//        onDone(exchId.topologyVersion());
//    }
//
//    /**
//     * @param cctx Cache context.
//     * @param busyLock Busy lock.
//     * @param exchId Exchange ID.
//     * @param reqs Cache change requests.
//     * @param affChangeMsg Affinity change message.
//     */
//    public GridDhtPartitionsExchangeFuture2(
//        GridCacheSharedContext cctx,
//        ReadWriteLock busyLock,
//        GridDhtPartitionExchangeId exchId,
//        Collection<DynamicCacheChangeRequest> reqs,
//        CacheAffinityChangeMessage affChangeMsg
//    ) {
//        assert busyLock != null;
//        assert exchId != null;
//        assert exchId.topologyVersion() != null;
//
//        dummy = false;
//        forcePreload = false;
//        reassign = false;
//
//        this.cctx = cctx;
//        this.busyLock = busyLock;
//        this.exchId = exchId;
//        this.affChangeMsg = affChangeMsg;
//
//        log = cctx.logger(getClass());
//
//        initFut = new GridFutureAdapter<>();
//
//        if (log.isDebugEnabled())
//            log.debug("Creating exchange future [localNode=" + cctx.localNodeId() + ", fut=" + this + ']');
//    }
//
//    /** {@inheritDoc} */
//    @Override public AffinityTopologyVersion topologyVersion() {
//        return exchId.topologyVersion();
//    }
//
//    /**
//     * @return Force preload flag.
//     */
//    public boolean forcePreload() {
//        return forcePreload;
//    }
//
//    /**
//     * @return Dummy reassign flag.
//     */
//    public boolean reassign() {
//        return reassign;
//    }
//
//    /**
//     * @return {@code True}
//     */
//    public boolean onAdded() {
//        return added.compareAndSet(false, true);
//    }
//
//    /**
//     * @return {@code true} if entered to busy state.
//     */
//    private boolean enterBusy() {
//        if (busyLock.readLock().tryLock())
//            return true;
//
//        if (log.isDebugEnabled())
//            log.debug("Failed to enter busy state (exchanger is stopping): " + this);
//
//        return false;
//    }
//
//    /**
//     *
//     */
//    private void leaveBusy() {
//        busyLock.readLock().unlock();
//    }
//
//    /**
//     * Starts activity.
//     *
//     * @throws IgniteInterruptedCheckedException If interrupted.
//     */
//    public void init() throws IgniteInterruptedCheckedException {
//        if (isDone())
//            return;
//
//        initTs = U.currentTimeMillis();
//
//        U.await(evtLatch);
//
//        assert discoEvt != null : this;
//        assert exchId.nodeId().equals(discoEvt.eventNode().id()) : this;
//        assert !dummy && !forcePreload : this;
//
//        try {
//            AffinityTopologyVersion topVer = topologyVersion();
//
//            srvNodes = new ArrayList<>(cctx.discovery().serverNodes(topVer));
//
//            remaining.addAll(F.nodeIds(F.view(srvNodes, F.remoteNodes(cctx.localNodeId()))));
//
//            crd = srvNodes.isEmpty() ? null : srvNodes.get(0);
//
//            boolean crdNode = crd != null && crd.isLocal();
//
//            skipPreload = cctx.kernalContext().clientNode();
//
//            ExchangeType exchange = null;
//
//
//            switch (exchange) {
//                case ALL: {
//                    distributedExchange();
//
//                    break;
//                }
//
//                case CLIENT: {
//                    initTopologies();
//
//                    clientOnlyExchange();
//
//                    break;
//                }
//
//                case NONE: {
//                    initTopologies();
//
//                    onDone(topVer);
//
//                    break;
//                }
//
//                default:
//                    assert false;
//            }
//        }
//        catch (IgniteInterruptedCheckedException e) {
//            onDone(e);
//
//            throw e;
//        }
//        catch (Throwable e) {
//            U.error(log, "Failed to reinitialize local partitions (preloading will be stopped): " + exchId, e);
//
//            onDone(e);
//
//            if (e instanceof Error)
//                throw (Error)e;
//        }
//    }
//
//    /**
//     * @param cache Cache.
//     * @param cacheNames Cache names.
//     * @param locNodeId Local node id.
//     */
//    @NotNull public static List<DynamicCacheChangeRequest> getStopCacheRequests(GridCacheProcessor cache,
//        Set<String> cacheNames, UUID locNodeId) {
//        List<DynamicCacheChangeRequest> destroyRequests = new ArrayList<>();
//
//        for (String cacheName : cacheNames) {
//            DynamicCacheDescriptor desc = cache.cacheDescriptor(CU.cacheId(cacheName));
//
//            if (desc == null)
//                continue;
//
//            DynamicCacheChangeRequest t = new DynamicCacheChangeRequest(UUID.randomUUID(), cacheName, locNodeId);
//
//            t.stop(true);
//            t.destroy(true);
//
//            t.deploymentId(desc.deploymentId());
//
//            t.restart(true);
//
//            destroyRequests.add(t);
//        }
//
//        return destroyRequests;
//    }
//
//    /**
//     * @throws IgniteCheckedException If failed.
//     */
//    private void initTopologies() throws IgniteCheckedException {
//        cctx.database().checkpointReadLock();
//
//        try {
//            if (crd != null) {
//                for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
//                    if (cacheCtx.isLocal())
//                        continue;
//
//                    cacheCtx.topology().beforeExchange(this, !centralizedAff);
//                }
//            }
//        }
//        finally {
//            cctx.database().checkpointReadUnlock();
//        }
//    }
//
//
//    /**
//     * @param crd Coordinator flag.
//     * @return Exchange type.
//     * @throws IgniteCheckedException If failed.
//     */
//    private ExchangeType onCacheChangeRequest(boolean crd) throws IgniteCheckedException {
//        assert !F.isEmpty(reqs) : this;
//
//        GridClusterStateProcessor stateProc = cctx.kernalContext().state();
//
//        if (exchangeOnChangeGlobalState = stateProc.changeGlobalState(reqs, topologyVersion())) {
//            changeGlobalStateE = stateProc.onChangeGlobalState();
//
//            if (crd && changeGlobalStateE != null)
//                changeGlobalStateExceptions.put(cctx.localNodeId(), changeGlobalStateE);
//        }
//
//        boolean clientOnly = cctx.affinity().onCacheChangeRequest(this, crd, reqs);
//
//        if (clientOnly) {
//            boolean clientCacheStarted = false;
//
//            for (DynamicCacheChangeRequest req : reqs) {
//                if (req.start() && req.clientStartOnly() && req.initiatingNodeId().equals(cctx.localNodeId())) {
//                    clientCacheStarted = true;
//
//                    break;
//                }
//            }
//
//            return clientCacheStarted ? ExchangeType.CLIENT : ExchangeType.NONE;
//        }
//        else
//            return cctx.kernalContext().clientNode() ? ExchangeType.CLIENT : ExchangeType.ALL;
//    }
//
//    /**
//     * @param crd Coordinator flag.
//     * @throws IgniteCheckedException If failed.
//     * @return Exchange type.
//     */
//    private ExchangeType onAffinityChangeRequest(boolean crd) throws IgniteCheckedException {
//        assert affChangeMsg != null : this;
//
//        cctx.affinity().onChangeAffinityMessage(this, crd, affChangeMsg);
//
//        if (cctx.kernalContext().clientNode())
//            return ExchangeType.CLIENT;
//
//        return ExchangeType.ALL;
//    }
//
//    /**
//     * @param crd Coordinator flag.
//     * @throws IgniteCheckedException If failed.
//     * @return Exchange type.
//     */
//    private ExchangeType onClientNodeEvent(boolean crd) throws IgniteCheckedException {
//        assert CU.clientNode(discoEvt.eventNode()) : this;
//
//        if (discoEvt.type() == EVT_NODE_LEFT || discoEvt.type() == EVT_NODE_FAILED) {
//            onLeft();
//
//            assert !discoEvt.eventNode().isLocal() : discoEvt;
//        }
//        else
//            assert discoEvt.type() == EVT_NODE_JOINED || discoEvt.type() == EVT_DISCOVERY_CUSTOM_EVT : discoEvt;
//
//        cctx.affinity().onClientEvent(this, crd);
//
//        return discoEvt.eventNode().isLocal() ? ExchangeType.CLIENT : ExchangeType.NONE;
//    }
//
//    /**
//     * @param crd Coordinator flag.
//     * @throws IgniteCheckedException If failed.
//     * @return Exchange type.
//     */
//    private ExchangeType onServerNodeEvent(boolean crd) throws IgniteCheckedException {
//        assert !CU.clientNode(discoEvt.eventNode()) : this;
//
//        if (discoEvt.type() == EVT_NODE_LEFT || discoEvt.type() == EVT_NODE_FAILED) {
//            onLeft();
//
//            warnNoAffinityNodes();
//
//            centralizedAff = cctx.affinity().onServerLeft(this);
//        }
//        else
//            cctx.affinity().onServerJoin(this, crd);
//
//        return cctx.kernalContext().clientNode() ? ExchangeType.CLIENT : ExchangeType.ALL;
//    }
//
//    /**
//     * @throws IgniteCheckedException If failed.
//     */
//    private void clientOnlyExchange() throws IgniteCheckedException {
//        clientOnlyExchange = true;
//
//        //todo checl invoke on client
//        if (crd != null) {
//            if (crd.isLocal()) {
//                for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
//                    boolean updateTop = !cacheCtx.isLocal() &&
//                        exchId.topologyVersion().equals(cacheCtx.startTopologyVersion());
//
//                    if (updateTop) {
//                        for (GridClientPartitionTopology top : cctx.exchange().clientTopologies()) {
//                            if (top.cacheId() == cacheCtx.cacheId()) {
//                                cacheCtx.topology().update(exchId,
//                                    top.partitionMap(true),
//                                    top.updateCounters(false));
//
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//            else {
//                if (!centralizedAff)
//                    sendLocalProgress(crd);
//
//                initDone();
//
//                return;
//            }
//        }
//        else {
//            if (centralizedAff) { // Last server node failed.
//                for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
//                    GridAffinityAssignmentCache aff = cacheCtx.affinity().affinityCache();
//
//                    aff.initialize(topologyVersion(), aff.idealAssignment());
//                }
//            }
//        }
//
//        onDone(topologyVersion());
//    }
//
//    /**
//     * @throws IgniteCheckedException If failed.
//     */
//    private void distributedExchange() throws IgniteCheckedException {
//        assert crd != null;
//
//        assert !cctx.kernalContext().clientNode();
//
//        if (crd.isLocal()) {
//            if (remaining.isEmpty())
//                onAllReceived();
//        }
//        else
//            sendProgress(crd);
//
//        initDone();
//    }
//
//    /**
//     *
//     */
//    private void onLeft() {
//
//    }
//
//    /**
//     * @param node Node.
//     * @throws IgniteCheckedException If failed.
//     */
//    private void sendLocalProgress(ClusterNode node) throws IgniteCheckedException {
//        assert node != null;
//
//        // Reset lost partition before send local partition to coordinator.
//        if (!F.isEmpty(reqs)) {
//            Set<String> caches = new HashSet<>();
//
//            for (DynamicCacheChangeRequest req : reqs) {
//                if (req.resetLostPartitions())
//                    caches.add(req.cacheName());
//            }
//
//            if (!F.isEmpty(caches))
//                resetLostPartitions(caches);
//        }
//
//        GridDhtPartitionsSingleMessage m = cctx.exchange().createPartitionsSingleMessage(
//            node, exchangeId(), clientOnlyExchange, true);
//
//        if (exchangeOnChangeGlobalState && changeGlobalStateE != null)
//            m.setException(changeGlobalStateE);
//
//        if (log.isDebugEnabled())
//            log.debug("Sending local partitions [nodeId=" + node.id() + ", exchId=" + exchId + ", msg=" + m + ']');
//
//        try {
//            cctx.io().send(node, m, SYSTEM_POOL);
//        }
//        catch (ClusterTopologyCheckedException ignore) {
//            if (log.isDebugEnabled())
//                log.debug("Node left during partition exchange [nodeId=" + node.id() + ", exchId=" + exchId + ']');
//        }
//    }
//
//    /**
//     * @param nodes Nodes.
//     * @throws IgniteCheckedException If failed.
//     */
//    private void sendAllPartitions(Collection<ClusterNode> nodes) throws IgniteCheckedException {
//
//
//        assert !nodes.contains(cctx.localNode());
//
//        if (log.isDebugEnabled())
//            log.debug("Sending full partition map [nodeIds=" + F.viewReadOnly(nodes, F.node2id()) +
//                ", exchId=" + exchId + ", msg=" + m + ']');
//
//        cctx.io().safeSend(nodes, m, SYSTEM_POOL, null);
//    }
//
//    /**
//     * @param oldestNode Oldest node.
//     */
//    private void sendProgress(ClusterNode oldestNode) {
//        try {
//            sendLocalProgress(oldestNode);
//        }
//        catch (ClusterTopologyCheckedException ignore) {
//            if (log.isDebugEnabled())
//                log.debug("Oldest node left during partition exchange [nodeId=" + oldestNode.id() +
//                    ", exchId=" + exchId + ']');
//        }
//        catch (IgniteCheckedException e) {
//            U.error(log, "Failed to send local partitions to oldest node (will retry after timeout) [oldestNodeId=" +
//                oldestNode.id() + ", exchId=" + exchId + ']', e);
//        }
//    }
//
//    /** {@inheritDoc} */
//    @Override public boolean onDone(@Nullable AffinityTopologyVersion res, @Nullable Throwable err) {
//        if (exchangeOnChangeGlobalState && err == null)
//            cctx.kernalContext().state().onExchangeDone();
//
//        if (super.onDone(res, err) && realExchange) {
//            if (log.isDebugEnabled())
//                log.debug("Completed partition exchange [localNode=" + cctx.localNodeId() + ", exchange= " + this +
//                    "duration=" + duration() + ", durationFromInit=" + (U.currentTimeMillis() - initTs) + ']');
//
//            initFut.onDone(err == null);
//
//            if (exchId.isLeft()) {
//                for (GridCacheContext cacheCtx : cctx.cacheContexts())
//                    cacheCtx.config().getAffinity().removeNode(exchId.nodeId());
//            }
//
//            reqs = null;
//
//            if (discoEvt instanceof DiscoveryCustomEvent)
//                ((DiscoveryCustomEvent)discoEvt).customMessage(null);
//
//            cctx.exchange().lastFinishedFuture(this);
//
//            return true;
//        }
//
//        return dummy;
//    }
//
//    /**
//     * Cleans up resources to avoid excessive memory usage.
//     */
//    public void cleanUp() {
//        topSnapshot.set(null);
//        singleMsgs.clear();
//        fullMsgs.clear();
//        msgs.clear();
//        changeGlobalStateExceptions.clear();
//        crd = null;
//        partReleaseFut = null;
//        changeGlobalStateE = null;
//    }
//
//
//    /**
//     * @param node Sender node.
//     * @param msg Single partition info.
//     */
//    public void onReceive(final ClusterNode node, final GridDhtPartitionsSingleMessage msg) {
//        assert msg != null;
//        assert msg.exchangeId().equals(exchId) : msg;
//        assert msg.lastVersion() != null : msg;
//
//        if (isDone()) {
//            if (log.isDebugEnabled())
//                log.debug("Received message for finished future (will reply only to sender) [msg=" + msg +
//                    ", fut=" + this + ']');
//
//            if (!centralizedAff)
//                sendAllPartitions(node.id(), cctx.gridConfig().getNetworkSendRetryCount());
//        }
//        else {
//            initFut.listen(new CI1<IgniteInternalFuture<Boolean>>() {
//                @Override public void apply(IgniteInternalFuture<Boolean> f) {
//                    try {
//                        if (!f.get())
//                            return;
//                    }
//                    catch (IgniteCheckedException e) {
//                        U.error(log, "Failed to initialize exchange future: " + this, e);
//
//                        return;
//                    }
//
//                    processMessage(node, msg);
//                }
//            });
//        }
//    }
//
//    /**
//     * @param node Sender node.
//     * @param msg Message.
//     */
//    private void processMessage(ClusterNode node, GridDhtPartitionsSingleMessage msg) {
//        boolean allReceived = false;
//
//        synchronized (mux) {
//            assert crd != null;
//
//            if (crd.isLocal()) {
//                if (remaining.remove(node.id())) {
//                    updatePartitionSingleMap(node, msg);
//
//                    if (exchangeOnChangeGlobalState && msg.getException() != null)
//                        changeGlobalStateExceptions.put(node.id(), msg.getException());
//
//                    allReceived = remaining.isEmpty();
//                }
//            }
//            else
//                singleMsgs.put(node, msg);
//        }
//
//        if (allReceived)
//            onAllReceived();
//    }
//
//    /**
//     * @param fut Affinity future.
//     */
//    private void onAffinityInitialized(IgniteInternalFuture<Map<Integer, Map<Integer, List<UUID>>>> fut) {
//        try {
//            assert fut.isDone();
//
//            Map<Integer, Map<Integer, List<UUID>>> assignmentChange = fut.get();
//
//            GridDhtPartitionsFullMessage m = createPartitionsMessage(null, false);
//
//            CacheAffinityChangeMessage msg = new CacheAffinityChangeMessage(exchId, m, assignmentChange);
//
//            if (log.isDebugEnabled())
//                log.debug("Centralized affinity exchange, send affinity change message: " + msg);
//
//            cctx.discovery().sendCustomEvent(msg);
//        }
//        catch (IgniteCheckedException e) {
//            onDone(e);
//        }
//    }
//
//    /**
//     * Detect lost partitions.
//     */
//    private void detectLostPartitions() {
//        synchronized (cctx.exchange().interruptLock()) {
//            if (Thread.currentThread().isInterrupted())
//                return;
//
//            for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
//                if (!cacheCtx.isLocal())
//                    cacheCtx.topology().detectLostPartitions(discoEvt);
//            }
//        }
//    }
//
//    /**
//     *
//     */
//    private void resetLostPartitions(Collection<String> cacheNames) {
//        synchronized (cctx.exchange().interruptLock()) {
//            if (Thread.currentThread().isInterrupted())
//                return;
//
//            for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
//                if (!cacheCtx.isLocal() && cacheNames.contains(cacheCtx.name()))
//                    cacheCtx.topology().resetLostPartitions();
//            }
//        }
//    }
//
//    /**
//     *
//     */
//    private void onAllReceived() {
//        try {
//            assert crd.isLocal();
//
//            if (!crd.equals(cctx.discovery().serverNodes(topologyVersion()).get(0))) {
//                for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
//                    if (!cacheCtx.isLocal())
//                        cacheCtx.topology().beforeExchange(this, !centralizedAff);
//                }
//            }
//
//            if (discoEvt.type() == EVT_NODE_JOINED) {
//                if (cctx.kernalContext().state().active())
//                    assignPartitionsStates();
//            }
//            else if (discoEvt.type() == EVT_DISCOVERY_CUSTOM_EVT) {
//                assert discoEvt instanceof DiscoveryCustomEvent;
//
//                if (((DiscoveryCustomEvent)discoEvt).customMessage() instanceof DynamicCacheChangeBatch) {
//                    DynamicCacheChangeBatch batch = (DynamicCacheChangeBatch)((DiscoveryCustomEvent)discoEvt)
//                        .customMessage();
//
//                    Set<String> caches = new HashSet<>();
//
//                    for (DynamicCacheChangeRequest req : batch.requests()) {
//                        if (req.resetLostPartitions())
//                            caches.add(req.cacheName());
//                        else if (req.globalStateChange() && req.state() != ClusterState.INACTIVE)
//                            assignPartitionsStates();
//                    }
//
//                    if (!F.isEmpty(caches))
//                        resetLostPartitions(caches);
//                }
//            }
//            else if (discoEvt.type() == EVT_NODE_LEFT || discoEvt.type() == EVT_NODE_FAILED)
//                detectLostPartitions();
//
//            updateLastVersion(cctx.versions().last());
//
//            cctx.versions().onExchange(lastVer.get().order());
//
//            if (centralizedAff) {
//                IgniteInternalFuture<Map<Integer, Map<Integer, List<UUID>>>> fut = cctx.affinity().initAffinityOnNodeLeft(this);
//
//                if (!fut.isDone()) {
//                    fut.listen(new IgniteInClosure<IgniteInternalFuture<Map<Integer, Map<Integer, List<UUID>>>>>() {
//                        @Override public void apply(IgniteInternalFuture<Map<Integer, Map<Integer, List<UUID>>>> fut) {
//                            onAffinityInitialized(fut);
//                        }
//                    });
//                }
//                else
//                    onAffinityInitialized(fut);
//            }
//            else {
//                List<ClusterNode> nodes;
//
//                synchronized (mux) {
//                    srvNodes.remove(cctx.localNode());
//
//                    nodes = new ArrayList<>(srvNodes);
//                }
//
//                if (!nodes.isEmpty())
//                    sendAllPartitions(nodes);
//
//                if (exchangeOnChangeGlobalState && !F.isEmpty(changeGlobalStateExceptions))
//                    cctx.kernalContext().state().onFullResponseMessage(changeGlobalStateExceptions);
//
//                onDone(exchangeId().topologyVersion());
//            }
//        }
//        catch (IgniteCheckedException e) {
//            onDone(e);
//        }
//    }
//
//
//    /**
//     * @param nodeId Node ID.
//     * @param retryCnt Number of retries.
//     */
//    private void sendAllPartitions(final UUID nodeId, final int retryCnt) {
//        ClusterNode n = cctx.node(nodeId);
//
//        try {
//            if (n != null)
//                sendAllPartitions(F.asList(n));
//        }
//        catch (IgniteCheckedException e) {
//            if (e instanceof ClusterTopologyCheckedException || !cctx.discovery().alive(n)) {
//                log.debug("Failed to send full partition map to node, node left grid " +
//                    "[rmtNode=" + nodeId + ", exchangeId=" + exchId + ']');
//
//                return;
//            }
//
//            if (retryCnt > 0) {
//                long timeout = cctx.gridConfig().getNetworkSendRetryDelay();
//
//                LT.error(log, e, "Failed to send full partition map to node (will retry after timeout) " +
//                    "[node=" + nodeId + ", exchangeId=" + exchId + ", timeout=" + timeout + ']');
//
//                cctx.time().addTimeoutObject(new GridTimeoutObjectAdapter(timeout) {
//                    @Override public void onTimeout() {
//                        sendAllPartitions(nodeId, retryCnt - 1);
//                    }
//                });
//            }
//            else
//                U.error(log, "Failed to send full partition map [node=" + n + ", exchangeId=" + exchId + ']', e);
//        }
//    }
//
//    /**
//     * @param node Sender node.
//     * @param msg Full partition info.
//     */
//    public void onReceive(final ClusterNode node, final GridDhtPartitionsFullMessage msg) {
//        assert msg != null;
//
//        final UUID nodeId = node.id();
//
//        if (isDone()) {
//            if (log.isDebugEnabled())
//                log.debug("Received message for finished future [msg=" + msg + ", fut=" + this + ']');
//
//            return;
//        }
//
//        if (log.isDebugEnabled())
//            log.debug("Received full partition map from node [nodeId=" + nodeId + ", msg=" + msg + ']');
//
//        initFut.listen(new CI1<IgniteInternalFuture<Boolean>>() {
//            @Override public void apply(IgniteInternalFuture<Boolean> f) {
//                try {
//                    if (!f.get())
//                        return;
//                }
//                catch (IgniteCheckedException e) {
//                    U.error(log, "Failed to initialize exchange future: " + this, e);
//
//                    return;
//                }
//
//                processMessage(node, msg);
//            }
//        });
//    }
//
//    /**
//     * @param node Sender node.
//     * @param msg Message.
//     */
//    private void processMessage(ClusterNode node, GridDhtPartitionsFullMessage msg) {
//        assert msg.exchangeId().equals(exchId) : msg;
//        assert msg.lastVersion() != null : msg;
//
//        synchronized (mux) {
//            if (crd == null)
//                return;
//
//            if (!crd.equals(node)) {
//                if (log.isDebugEnabled())
//                    log.debug("Received full partition map from unexpected node [oldest=" + crd.id() +
//                        ", nodeId=" + node.id() + ']');
//
//                if (node.order() > crd.order())
//                    fullMsgs.put(node, msg);
//
//                return;
//            }
//        }
//
//        updatePartitionFullMap(msg);
//
//        if (exchangeOnChangeGlobalState && !F.isEmpty(msg.getExceptionsMap()))
//            cctx.kernalContext().state().onFullResponseMessage(msg.getExceptionsMap());
//
//        onDone(exchId.topologyVersion());
//    }
//
//    /**
//     * Updates partition map in all caches.
//     *
//     * @param msg Partitions full messages.
//     */
//    private void updatePartitionFullMap(GridDhtPartitionsFullMessage msg) {
//        cctx.versions().onExchange(msg.lastVersion().order());
//
//        for (Map.Entry<Integer, GridDhtPartitionFullMap> entry : msg.partitions().entrySet()) {
//            Integer cacheId = entry.getKey();
//
//            Map<Integer, T2<Long, Long>> cntrMap = msg.partitionUpdateCounters(cacheId);
//
//            GridCacheContext cacheCtx = cctx.cacheContext(cacheId);
//
//            if (cacheCtx != null)
//                cacheCtx.topology().update(exchId, entry.getValue(), cntrMap);
//            else {
//                ClusterNode oldest = cctx.discovery().oldestAliveCacheServerNode(AffinityTopologyVersion.NONE);
//
//                if (oldest != null && oldest.isLocal())
//                    cctx.exchange().clientTopology(cacheId, this).update(exchId, entry.getValue(), cntrMap);
//            }
//        }
//    }
//
//    /**
//     * Updates partition map in all caches.
//     *
//     * @param msg Partitions single message.
//     */
//    private void updatePartitionSingleMap(ClusterNode node, GridDhtPartitionsSingleMessage msg) {
//        msgs.put(node.id(), msg);
//
//        for (Map.Entry<Integer, GridDhtPartitionMap2> entry : msg.partitions().entrySet()) {
//            Integer cacheId = entry.getKey();
//            GridCacheContext cacheCtx = cctx.cacheContext(cacheId);
//
//            GridDhtPartitionTopology top = cacheCtx != null ? cacheCtx.topology() :
//                cctx.exchange().clientTopology(cacheId, this);
//
//            top.update(exchId, entry.getValue(), msg.partitionUpdateCounters(cacheId));
//        }
//    }
//
//    /**
//     * Affinity change message callback, processed from the same thread as {@link #onNodeLeft}.
//     *
//     * @param node Message sender node.
//     * @param msg Message.
//     */
//    public void onAffinityChangeMessage(final ClusterNode node, final CacheAffinityChangeMessage msg) {
//        assert exchId.equals(msg.exchangeId()) : msg;
//
//        onDiscoveryEvent(new IgniteRunnable() {
//            @Override public void run() {
//                if (isDone() || !enterBusy())
//                    return;
//
//                try {
//                    assert centralizedAff;
//
//                    if (crd.equals(node)) {
//                        cctx.affinity().onExchangeChangeAffinityMessage(GridDhtPartitionsExchangeFuture2.this,
//                            crd.isLocal(),
//                            msg);
//
//                        if (!crd.isLocal()) {
//                            GridDhtPartitionsFullMessage partsMsg = msg.partitionsMessage();
//
//                            assert partsMsg != null : msg;
//                            assert partsMsg.lastVersion() != null : partsMsg;
//
//                            updatePartitionFullMap(partsMsg);
//                        }
//
//                        onDone(topologyVersion());
//                    }
//                    else {
//                        if (log.isDebugEnabled()) {
//                            log.debug("Ignore affinity change message, coordinator changed [node=" + node.id() +
//                                ", crd=" + crd.id() +
//                                ", msg=" + msg +
//                                ']');
//                        }
//                    }
//                }
//                finally {
//                    leaveBusy();
//                }
//            }
//        });
//    }
//
//    /**
//     * @param c Closure.
//     */
//    private void onDiscoveryEvent(IgniteRunnable c) {
//        synchronized (discoEvts) {
//            if (!init) {
//                discoEvts.add(c);
//
//                return;
//            }
//
//            assert discoEvts.isEmpty() : discoEvts;
//        }
//
//        c.run();
//    }
//
//    /**
//     *
//     */
//    private void initDone() {
//        while (!isDone()) {
//            List<IgniteRunnable> evts;
//
//            synchronized (discoEvts) {
//                if (discoEvts.isEmpty()) {
//                    init = true;
//
//                    break;
//                }
//
//                evts = new ArrayList<>(discoEvts);
//
//                discoEvts.clear();
//            }
//
//            for (IgniteRunnable c : evts)
//                c.run();
//        }
//
//        initFut.onDone(true);
//    }
//
//    /**
//     * Node left callback, processed from the same thread as {@link #onAffinityChangeMessage}.
//     *
//     * @param node Left node.
//     */
//    public void onNodeLeft(final ClusterNode node) {
//        if (isDone() || !enterBusy())
//            return;
//
//        try {
//            onDiscoveryEvent(new IgniteRunnable() {
//                @Override public void run() {
//                    if (isDone() || !enterBusy())
//                        return;
//
//                    try {
//                        boolean crdChanged = false;
//                        boolean allReceived = false;
//                        Set<UUID> reqFrom = null;
//
//                        ClusterNode crd0;
//
//                        synchronized (mux) {
//                            if (!srvNodes.remove(node))
//                                return;
//
//                            boolean rmvd = remaining.remove(node.id());
//
//                            if (node.equals(crd)) {
//                                crdChanged = true;
//
//                                crd = !srvNodes.isEmpty() ? srvNodes.get(0) : null;
//                            }
//
//                            if (crd != null && crd.isLocal()) {
//                                if (rmvd)
//                                    allReceived = remaining.isEmpty();
//
//                                if (crdChanged && !remaining.isEmpty())
//                                    reqFrom = new HashSet<>(remaining);
//                            }
//
//                            crd0 = crd;
//                        }
//
//                        if (crd0 == null) {
//                            assert cctx.kernalContext().clientNode() || cctx.localNode().isDaemon() : cctx.localNode();
//
//                            onDone(topologyVersion());
//
//                            return;
//                        }
//
//                        if (crd0.isLocal()) {
//                            if (exchangeOnChangeGlobalState && changeGlobalStateE !=null)
//                                changeGlobalStateExceptions.put(crd0.id(), changeGlobalStateE);
//
//                            if (allReceived) {
//                                onAllReceived();
//
//                                return;
//                            }
//
//                            if (crdChanged && reqFrom != null) {
//                                GridDhtPartitionsSingleRequest req = new GridDhtPartitionsSingleRequest(exchId);
//
//                                for (UUID nodeId : reqFrom) {
//                                    try {
//                                        // It is possible that some nodes finished exchange with previous coordinator.
//                                        cctx.io().send(nodeId, req, SYSTEM_POOL);
//                                    }
//                                    catch (ClusterTopologyCheckedException e) {
//                                        if (log.isDebugEnabled())
//                                            log.debug("Node left during partition exchange [nodeId=" + nodeId +
//                                                ", exchId=" + exchId + ']');
//                                    }
//                                    catch (IgniteCheckedException e) {
//                                        U.error(log, "Failed to request partitions from node: " + nodeId, e);
//                                    }
//                                }
//                            }
//
//                            for (Map.Entry<ClusterNode, GridDhtPartitionsSingleMessage> m : singleMsgs.entrySet())
//                                processMessage(m.getKey(), m.getValue());
//                        }
//                        else {
//                            if (crdChanged) {
//                                sendProgress(crd0);
//
//                                for (Map.Entry<ClusterNode, GridDhtPartitionsFullMessage> m : fullMsgs.entrySet())
//                                    processMessage(m.getKey(), m.getValue());
//                            }
//                        }
//                    }
//                    finally {
//                        leaveBusy();
//                    }
//                }
//            });
//        }
//        finally {
//            leaveBusy();
//        }
//    }
//
//    /** {@inheritDoc} */
//    @Override public int compareTo(GridDhtPartitionsExchangeFuture2 fut) {
//        return exchId.compareTo(fut.exchId);
//    }
//
//    /** {@inheritDoc} */
//    @Override public boolean equals(Object o) {
//        if (this == o)
//            return true;
//
//        GridDhtPartitionsExchangeFuture2 fut = (GridDhtPartitionsExchangeFuture2)o;
//
//        return exchId.equals(fut.exchId);
//    }
//
//    /** {@inheritDoc} */
//    @Override public int hashCode() {
//        return exchId.hashCode();
//    }
//
//    /**
//     *
//     */
//    enum ExchangeType {
//        /** */
//        CLIENT,
//        /** */
//        ALL,
//        /** */
//        NONE
//    }
//
//    /**
//     * Cache validation result.
//     */
//    private static class CacheValidation {
//        /** Topology validation result. */
//        private boolean valid;
//
//        /** Lost partitions on this topology version. */
//        private Collection<Integer> lostParts;
//
//        /**
//         * @param valid Valid flag.
//         * @param lostParts Lost partitions.
//         */
//        private CacheValidation(boolean valid, Collection<Integer> lostParts) {
//            this.valid = valid;
//            this.lostParts = lostParts;
//        }
//    }
//
//    /** {@inheritDoc} */
//    @Override public String toString() {
//        Set<UUID> remaining;
//        List<ClusterNode> srvNodes;
//
//        synchronized (mux) {
//            remaining = new HashSet<>(this.remaining);
//            srvNodes = this.srvNodes != null ? new ArrayList<>(this.srvNodes) : null;
//        }
//
//        return S.toString(GridDhtPartitionsExchangeFuture2.class, this,
//            "evtLatch", evtLatch == null ? "null" : evtLatch.getCount(),
//            "remaining", remaining,
//            "srvNodes", srvNodes,
//            "super", super.toString());
//    }
//
//    /**
//     *
//     */
//    private static class CounterWithNodes {
//        /** */
//        private final long cnt;
//
//        /** */
//        private final Set<UUID> nodes = new HashSet<>();
//
//        /**
//         * @param cnt Count.
//         * @param firstNode Node ID.
//         */
//        private CounterWithNodes(long cnt, UUID firstNode) {
//            this.cnt = cnt;
//
//            nodes.add(firstNode);
//        }
//
//        /** {@inheritDoc} */
//        @Override public String toString() {
//            return S.toString(CounterWithNodes.class, this);
//        }
//    }
//}
