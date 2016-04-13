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

package org.apache.ignite.internal.processors.cache.distributed.dht.preloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.cache.affinity.AffinityCentralizedFunction;
import org.apache.ignite.cache.affinity.AffinityFunction;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.internal.IgniteFutureTimeoutCheckedException;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.cluster.ClusterTopologyCheckedException;
import org.apache.ignite.internal.managers.discovery.GridDiscoveryTopologySnapshot;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.DynamicCacheChangeRequest;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCacheMvccCandidate;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridClientPartitionTopology;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtAssignmentFetchFuture;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtPartitionTopology;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtTopologyFuture;
import org.apache.ignite.internal.processors.cache.transactions.IgniteTxKey;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.processors.timeout.GridTimeoutObject;
import org.apache.ignite.internal.processors.timeout.GridTimeoutObjectAdapter;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.CI1;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.LT;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgnitePredicate;
import org.jetbrains.annotations.Nullable;
import org.jsr166.ConcurrentHashMap8;

import static org.apache.ignite.events.EventType.EVT_NODE_FAILED;
import static org.apache.ignite.events.EventType.EVT_NODE_JOINED;
import static org.apache.ignite.events.EventType.EVT_NODE_LEFT;
import static org.apache.ignite.internal.events.DiscoveryCustomEvent.EVT_DISCOVERY_CUSTOM_EVT;
import static org.apache.ignite.internal.managers.communication.GridIoPolicy.SYSTEM_POOL;

/**
 * Future for exchanging partition maps.
 */
public class GridDhtPartitionsExchangeFuture extends GridFutureAdapter<AffinityTopologyVersion>
    implements Comparable<GridDhtPartitionsExchangeFuture>, GridDhtTopologyFuture {
    /** */
    private static final int DUMP_PENDING_OBJECTS_THRESHOLD =
        IgniteSystemProperties.getInteger(IgniteSystemProperties.IGNITE_DUMP_PENDING_OBJECTS_THRESHOLD, 10);

    /** */
    private static final long serialVersionUID = 0L;

    /** Dummy flag. */
    private final boolean dummy;

    /** Force preload flag. */
    private final boolean forcePreload;

    /** Dummy reassign flag. */
    private final boolean reassign;

    /** Discovery event. */
    private volatile DiscoveryEvent discoEvt;

    /** */
    @GridToStringInclude
    private final Set<UUID> remaining = new HashSet<>();

    /** */
    private List<ClusterNode> srvNodes;

    /** */
    private ClusterNode crd;

    /** ExchangeFuture id. */
    private final GridDhtPartitionExchangeId exchId;

    /** Timeout object. */
    @GridToStringExclude
    private volatile GridTimeoutObject timeoutObj;

    /** Cache context. */
    private final GridCacheSharedContext<?, ?> cctx;

    /** Busy lock to prevent activities from accessing exchanger while it's stopping. */
    private ReadWriteLock busyLock;

    /** Event latch. */
    @GridToStringExclude
    private CountDownLatch evtLatch = new CountDownLatch(1);

    /** */
    private GridFutureAdapter<Boolean> initFut;

    /** Topology snapshot. */
    private AtomicReference<GridDiscoveryTopologySnapshot> topSnapshot = new AtomicReference<>();

    /** Last committed cache version before next topology version use. */
    private AtomicReference<GridCacheVersion> lastVer = new AtomicReference<>();

    /**
     * Messages received on non-coordinator are stored in case if this node
     * becomes coordinator.
     */
    private final Map<ClusterNode, GridDhtPartitionsSingleMessage> singleMsgs = new ConcurrentHashMap8<>();

    /** Messages received from new coordinator. */
    private final Map<ClusterNode, GridDhtPartitionsFullMessage> fullMsgs = new ConcurrentHashMap8<>();

    /** */
    private List<DiscoveryEvent> addedJoinEvts;

    /** */
    private GridDhtPartitionExchangeId resExchId;

    /** */
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    @GridToStringInclude
    private volatile IgniteInternalFuture<?> partReleaseFut;

    /** */
    private final Object mux = new Object();

    /** Logger. */
    private IgniteLogger log;

    /** Dynamic cache change requests. */
    private Collection<DynamicCacheChangeRequest> reqs;

    /** Cache validation results. */
    private volatile Map<Integer, Boolean> cacheValidRes;

    /** Skip preload flag. */
    private boolean skipPreload;

    /** */
    private boolean clientOnlyExchange;

    /** Init timestamp. Used to track the amount of time spent to complete the future. */
    private long initTs;

    /**
     * Dummy future created to trigger reassignments if partition
     * topology changed while preloading.
     *
     * @param cctx Cache context.
     * @param reassign Dummy reassign flag.
     * @param exchFut Future.
     */
    public GridDhtPartitionsExchangeFuture(
        GridCacheSharedContext cctx,
        boolean reassign,
        GridDhtPartitionsExchangeFuture exchFut
    ) {
        assert exchFut.isDone();

        dummy = true;
        forcePreload = false;

        this.exchId = exchFut.exchId;
        this.resExchId = exchFut.resExchId;
        this.reassign = reassign;
        this.discoEvt = exchFut.discoEvt;
        this.cctx = cctx;

        onDone(exchFut.topologyVersion());
    }

    /**
     * Force preload future created to trigger reassignments if partition
     * topology changed while preloading.
     *
     * @param cctx Cache context.
     * @param exchFut Future.
     */
    public GridDhtPartitionsExchangeFuture(GridCacheSharedContext cctx, GridDhtPartitionsExchangeFuture exchFut) {
        assert exchFut.isDone();

        dummy = false;
        forcePreload = true;

        this.exchId = exchFut.exchId;
        this.resExchId = exchFut.resExchId;
        this.discoEvt = exchFut.discoEvt;
        this.cctx = cctx;

        reassign = true;

        onDone(exchFut.topologyVersion());
    }

    /**
     * @param cctx Cache context.
     * @param busyLock Busy lock.
     * @param exchId Exchange ID.
     * @param reqs Cache change requests.
     */
    public GridDhtPartitionsExchangeFuture(
        GridCacheSharedContext cctx,
        ReadWriteLock busyLock,
        GridDhtPartitionExchangeId exchId,
        Collection<DynamicCacheChangeRequest> reqs
    ) {
        assert busyLock != null;
        assert exchId != null;

        dummy = false;
        forcePreload = false;
        reassign = false;

        this.cctx = cctx;
        this.busyLock = busyLock;
        this.exchId = exchId;
        this.resExchId = exchId;
        this.reqs = reqs;

        log = cctx.logger(getClass());

        initFut = new GridFutureAdapter<>();

        if (log.isDebugEnabled())
            log.debug("Creating exchange future [localNode=" + cctx.localNodeId() + ", fut=" + this + ']');
    }

    /**
     * @param reqs Cache change requests.
     */
    public void cacheChangeRequests(Collection<DynamicCacheChangeRequest> reqs) {
        this.reqs = reqs;
    }

    /** {@inheritDoc} */
    @Override public AffinityTopologyVersion topologyVersion() {
        assert isDone();

        return resExchId.topologyVersion();
    }

    /**
     * @return Exchange start topology version.
     */
    public AffinityTopologyVersion startTopologyVersion() {
        return exchId.topologyVersion();
    }

    /**
     * @return Skip preload flag.
     */
    public boolean skipPreload() {
        return skipPreload;
    }

    /**
     * @return {@code True} if this is future for discovery event.
     */
    public boolean realExchange() {
        return !dummy && !reassign;
    }

    /**
     * @return Dummy flag.
     */
    public boolean dummy() {
        return dummy;
    }

    /**
     * @return Force preload flag.
     */
    public boolean forcePreload() {
        return forcePreload;
    }

    /**
     * @return Dummy reassign flag.
     */
    public boolean reassign() {
        return reassign;
    }

    /**
     * @return {@code True} if dummy reassign.
     */
    public boolean dummyReassign() {
        return (dummy() || forcePreload()) && reassign();
    }

    /**
     * @param cacheId Cache ID to check.
     * @param topVer Topology version.
     * @return {@code True} if cache was added during this exchange.
     */
    public boolean isCacheAdded(int cacheId, AffinityTopologyVersion topVer) {
        if (cacheStarted(cacheId))
            return true;

        GridCacheContext<?, ?> cacheCtx = cctx.cacheContext(cacheId);

        return cacheCtx != null && F.eq(cacheCtx.startTopologyVersion(), topVer);
    }

    /**
     * @param cacheId Cache ID.
     * @return {@code True} if non-client cache was added during this exchange.
     */
    private boolean cacheStarted(int cacheId) {
        if (!F.isEmpty(reqs)) {
            for (DynamicCacheChangeRequest req : reqs) {
                if (req.start() && !req.clientStartOnly()) {
                    if (CU.cacheId(req.cacheName()) == cacheId)
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * @param cacheCtx Cache context.
     * @throws IgniteCheckedException If failed.
     */
    private void initTopology(GridCacheContext cacheCtx) throws IgniteCheckedException {
        if (stopping(cacheCtx.cacheId()))
            return;

        if (canCalculateAffinity(cacheCtx)) {
            if (log.isDebugEnabled())
                log.debug("Will recalculate affinity [locNodeId=" + cctx.localNodeId() + ", exchId=" + exchId + ']');

            cacheCtx.affinity().calculateAffinity(exchId.topologyVersion(), discoEvt);
        }
        else {
            if (log.isDebugEnabled())
                log.debug("Will request affinity from remote node [locNodeId=" + cctx.localNodeId() + ", exchId=" +
                    exchId + ']');

            // Fetch affinity assignment from remote node.
            GridDhtAssignmentFetchFuture fetchFut = new GridDhtAssignmentFetchFuture(cacheCtx,
                exchId.topologyVersion(),
                CU.affinityNodes(cacheCtx, exchId.topologyVersion()));

            fetchFut.init();

            List<List<ClusterNode>> affAssignment = fetchFut.get();

            if (log.isDebugEnabled())
                log.debug("Fetched affinity from remote node, initializing affinity assignment [locNodeId=" +
                    cctx.localNodeId() + ", topVer=" + exchId.topologyVersion() + ']');

            if (affAssignment == null) {
                affAssignment = new ArrayList<>(cacheCtx.affinity().partitions());

                List<ClusterNode> empty = Collections.emptyList();

                for (int i = 0; i < cacheCtx.affinity().partitions(); i++)
                    affAssignment.add(empty);
            }

            cacheCtx.affinity().initializeAffinity(exchId.topologyVersion(), affAssignment);
        }
    }

    /**
     * @param cacheCtx Cache context.
     * @return {@code True} if local node can calculate affinity on it's own for this partition map exchange.
     */
    private boolean canCalculateAffinity(GridCacheContext cacheCtx) {
        AffinityFunction affFunc = cacheCtx.config().getAffinity();

        // Do not request affinity from remote nodes if affinity function is not centralized.
        if (!U.hasAnnotation(affFunc, AffinityCentralizedFunction.class))
            return true;

        // If local node did not initiate exchange or local node is the only cache node in grid.
        Collection<ClusterNode> affNodes = CU.affinityNodes(cacheCtx, exchId.topologyVersion());

        return cacheStarted(cacheCtx.cacheId()) ||
            !exchId.nodeId().equals(cctx.localNodeId()) ||
            (affNodes.size() == 1 && affNodes.contains(cctx.localNode()));
    }

    /**
     * Event callback.
     *
     * @param exchId Exchange ID.
     * @param discoEvt Discovery event.
     */
    public void onEvent(GridDhtPartitionExchangeId exchId, DiscoveryEvent discoEvt) {
        assert exchId.equals(this.exchId);

        this.discoEvt = discoEvt;

        evtLatch.countDown();
    }

    /**
     * @return Discovery event.
     */
    public DiscoveryEvent discoveryEvent() {
        return discoEvt;
    }

    /**
     * @return Exchange ID.
     */
    public GridDhtPartitionExchangeId exchangeId() {
        return exchId;
    }

    /**
     * @return {@code true} if entered to busy state.
     */
    private boolean enterBusy() {
        if (busyLock.readLock().tryLock())
            return true;

        if (log.isDebugEnabled())
            log.debug("Failed to enter busy state (exchanger is stopping): " + this);

        return false;
    }

    /**
     *
     */
    private void leaveBusy() {
        busyLock.readLock().unlock();
    }

    /**
     * Starts activity.
     *
     * @throws IgniteInterruptedCheckedException If interrupted.
     */
    public void init() throws IgniteInterruptedCheckedException {
        if (isDone())
            return;

        initTs = U.currentTimeMillis();

        try {
            // Wait for event to occur to make sure that discovery
            // will return corresponding nodes.
            U.await(evtLatch);

            assert discoEvt != null : this;
            assert !dummy && !forcePreload : this;

            srvNodes = new ArrayList<>(cctx.discovery().aliveServerNodesWithCaches(startTopologyVersion()));

            remaining.addAll(F.nodeIds(F.view(srvNodes, F.remoteNodes(cctx.localNodeId()))));

            crd = srvNodes.isEmpty() ? null : srvNodes.get(0);

            if (!F.isEmpty(reqs))
                blockGateways();

            startCaches();

            // True if client node joined or failed.
            boolean clientNodeEvt;

            if (F.isEmpty(reqs)) {
                int type = discoEvt.type();

                assert type == EVT_NODE_JOINED || type == EVT_NODE_LEFT || type == EVT_NODE_FAILED : discoEvt;

                clientNodeEvt = CU.clientNode(discoEvt.eventNode());
            }
            else {
                assert discoEvt.type() == EVT_DISCOVERY_CUSTOM_EVT : discoEvt;

                boolean clientOnlyCacheEvt = true;

                for (DynamicCacheChangeRequest req : reqs) {
                    if (req.clientStartOnly() || req.close())
                        continue;

                    clientOnlyCacheEvt = false;

                    break;
                }

                clientNodeEvt = clientOnlyCacheEvt;
            }

            if (clientNodeEvt) {
                ClusterNode node = discoEvt.eventNode();

                // Client need to initialize affinity for local join event or for stated client caches.
                if (!node.isLocal() || clientCacheClose()) {
                    for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                        if (cacheCtx.isLocal())
                            continue;

                        GridDhtPartitionTopology top = cacheCtx.topology();

                        top.updateTopologyVersion(exchId, this, -1, stopping(cacheCtx.cacheId()));

                        if (cacheCtx.affinity().affinityTopologyVersion() == AffinityTopologyVersion.NONE) {
                            initTopology(cacheCtx);

                            top.beforeExchange(this);
                        }
                        else
                            cacheCtx.affinity().clientEventTopologyChange(discoEvt, exchId.topologyVersion());

                        if (!exchId.isJoined())
                            cacheCtx.preloader().unwindUndeploys();
                    }

                    if (exchId.isLeft())
                        cctx.mvcc().removeExplicitNodeLocks(exchId.nodeId(), exchId.topologyVersion());

                    onDone(exchId.topologyVersion());

                    skipPreload = cctx.kernalContext().clientNode();

                    return;
                }
            }

            clientOnlyExchange = clientNodeEvt || cctx.kernalContext().clientNode();

            if (clientOnlyExchange) {
                skipPreload = cctx.kernalContext().clientNode();

                for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                    if (cacheCtx.isLocal())
                        continue;

                    GridDhtPartitionTopology top = cacheCtx.topology();

                    top.updateTopologyVersion(exchId, this, -1, stopping(cacheCtx.cacheId()));
                }

                for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                    if (cacheCtx.isLocal())
                        continue;

                    initTopology(cacheCtx);
                }

                if (crd != null) {
                    initFut.onDone(true);

                    if (log.isDebugEnabled())
                        log.debug("Initialized future: " + this);

                    if (cctx.localNode().equals(crd)) {
                        for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                            boolean updateTop = !cacheCtx.isLocal() &&
                                exchId.topologyVersion().equals(cacheCtx.startTopologyVersion());

                            if (updateTop) {
                                for (GridClientPartitionTopology top : cctx.exchange().clientTopologies()) {
                                    if (top.cacheId() == cacheCtx.cacheId()) {
                                        cacheCtx.topology().update(exchId,
                                            top.partitionMap(true),
                                            top.updateCounters());

                                        break;
                                    }
                                }

                            }
                        }

                        onDone(exchId.topologyVersion());
                    }
                    else
                        sendPartitions(crd);
                }
                else
                    onDone(exchId.topologyVersion());

                return;
            }

            assert crd != null;

            for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                if (isCacheAdded(cacheCtx.cacheId(), exchId.topologyVersion())) {
                    if (cacheCtx.discovery().cacheAffinityNodes(cacheCtx.name(), startTopologyVersion()).isEmpty())
                        U.quietAndWarn(log, "No server nodes found for cache client: " + cacheCtx.namex());
                }

                cacheCtx.preloader().onExchangeFutureAdded();
            }

            List<String> cachesWithoutNodes = null;

            if (exchId.isLeft()) {
                for (String name : cctx.cache().cacheNames()) {
                    if (cctx.discovery().cacheAffinityNodes(name, startTopologyVersion()).isEmpty()) {
                        if (cachesWithoutNodes == null)
                            cachesWithoutNodes = new ArrayList<>();

                        cachesWithoutNodes.add(name);

                        // Fire event even if there is no client cache started.
                        if (cctx.gridEvents().isRecordable(EventType.EVT_CACHE_NODES_LEFT)) {
                            Event evt = new CacheEvent(
                                name,
                                cctx.localNode(),
                                cctx.localNode(),
                                "All server nodes have left the cluster.",
                                EventType.EVT_CACHE_NODES_LEFT,
                                0,
                                false,
                                null,
                                null,
                                null,
                                null,
                                false,
                                null,
                                false,
                                null,
                                null,
                                null
                            );

                            cctx.gridEvents().record(evt);
                        }
                    }
                }
            }

            if (cachesWithoutNodes != null) {
                StringBuilder sb =
                    new StringBuilder("All server nodes for the following caches have left the cluster: ");

                for (int i = 0; i < cachesWithoutNodes.size(); i++) {
                    String cache = cachesWithoutNodes.get(i);

                    sb.append('\'').append(cache).append('\'');

                    if (i != cachesWithoutNodes.size() - 1)
                        sb.append(", ");
                }

                U.quietAndWarn(log, sb.toString());

                U.quietAndWarn(log, "Must have server nodes for caches to operate.");
            }

            assert discoEvt != null;

            assert exchId.nodeId().equals(discoEvt.eventNode().id());

            for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                GridClientPartitionTopology clientTop = cctx.exchange().clearClientTopology(
                    cacheCtx.cacheId());

                long updSeq = clientTop == null ? -1 : clientTop.lastUpdateSequence();

                // Update before waiting for locks.
                if (!cacheCtx.isLocal())
                    cacheCtx.topology().updateTopologyVersion(exchId, this, updSeq, stopping(cacheCtx.cacheId()));
            }

            AffinityTopologyVersion topVer = exchId.topologyVersion();

            for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                if (cacheCtx.isLocal())
                    continue;

                // Must initialize topology after we get discovery event.
                initTopology(cacheCtx);

                cacheCtx.preloader().onTopologyChanged(exchId.topologyVersion());

                cacheCtx.preloader().updateLastExchangeFuture(this);
            }

            IgniteInternalFuture<?> partReleaseFut = cctx.partitionReleaseFuture(topVer);

            // Assign to class variable so it will be included into toString() method.
            this.partReleaseFut = partReleaseFut;

            if (exchId.isLeft())
                cctx.mvcc().removeExplicitNodeLocks(exchId.nodeId(), exchId.topologyVersion());

            if (log.isDebugEnabled())
                log.debug("Before waiting for partition release future: " + this);

            int dumpedObjects = 0;

            while (true) {
                try {
                    partReleaseFut.get(2 * cctx.gridConfig().getNetworkTimeout(), TimeUnit.MILLISECONDS);

                    break;
                }
                catch (IgniteFutureTimeoutCheckedException ignored) {
                    // Print pending transactions and locks that might have led to hang.
                    if (dumpedObjects < DUMP_PENDING_OBJECTS_THRESHOLD) {
                        dumpPendingObjects();

                        dumpedObjects++;
                    }
                }
            }

            if (log.isDebugEnabled())
                log.debug("After waiting for partition release future: " + this);

            IgniteInternalFuture<?> locksFut = cctx.mvcc().finishLocks(exchId.topologyVersion());

            dumpedObjects = 0;

            while (true) {
                try {
                    locksFut.get(2 * cctx.gridConfig().getNetworkTimeout(), TimeUnit.MILLISECONDS);

                    break;
                }
                catch (IgniteFutureTimeoutCheckedException ignored) {
                    if (dumpedObjects < DUMP_PENDING_OBJECTS_THRESHOLD) {
                        U.warn(log, "Failed to wait for locks release future. " +
                            "Dumping pending objects that might be the cause: " + cctx.localNodeId());

                        U.warn(log, "Locked keys:");

                        for (IgniteTxKey key : cctx.mvcc().lockedKeys())
                            U.warn(log, "Locked key: " + key);

                        for (IgniteTxKey key : cctx.mvcc().nearLockedKeys())
                            U.warn(log, "Locked near key: " + key);

                        Map<IgniteTxKey, Collection<GridCacheMvccCandidate>> locks =
                            cctx.mvcc().unfinishedLocks(exchId.topologyVersion());

                        for (Map.Entry<IgniteTxKey, Collection<GridCacheMvccCandidate>> e : locks.entrySet())
                            U.warn(log, "Awaited locked entry [key=" + e.getKey() + ", mvcc=" + e.getValue() + ']');

                        dumpedObjects++;
                    }
                }
            }

            boolean topChanged = discoEvt.type() != EVT_DISCOVERY_CUSTOM_EVT;

            for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                if (cacheCtx.isLocal())
                    continue;

                // Notify replication manager.
                GridCacheContext drCacheCtx = cacheCtx.isNear() ? cacheCtx.near().dht().context() : cacheCtx;

                if (drCacheCtx.isDrEnabled())
                    drCacheCtx.dr().beforeExchange(topVer, exchId.isLeft());

                if (topChanged)
                    cacheCtx.continuousQueries().beforeExchange(exchId.topologyVersion());

                // Partition release future is done so we can flush the write-behind store.
                cacheCtx.store().forceFlush();

                if (!exchId.isJoined())
                    // Process queued undeploys prior to sending/spreading map.
                    cacheCtx.preloader().unwindUndeploys();

                GridDhtPartitionTopology top = cacheCtx.topology();

                assert topVer.equals(top.topologyVersion()) :
                    "Topology version is updated only in this class instances inside single ExchangeWorker thread.";

                top.beforeExchange(this);
            }

            for (GridClientPartitionTopology top : cctx.exchange().clientTopologies()) {
                top.updateTopologyVersion(exchId, this, -1, stopping(top.cacheId()));

                top.beforeExchange(this);
            }
        }
        catch (IgniteInterruptedCheckedException e) {
            onDone(e);

            throw e;
        }
        catch (Throwable e) {
            U.error(log, "Failed to reinitialize local partitions (preloading will be stopped): " + exchId, e);

            onDone(e);

            if (e instanceof Error)
                throw (Error)e;

            return;
        }

        if (crd.isLocal()) {
            if (F.isEmpty(remaining)) {
                coordinatorAllReceived();

                return;
            }
        }
        else
            sendPartitions(crd);

        initFut.onDone(true);

        if (log.isDebugEnabled())
            log.debug("Initialized future: " + this);

        scheduleRecheck();
    }

    /**
     * @return {@code True} if exchange initiated for client cache close.
     */
    private boolean clientCacheClose() {
        return reqs != null && reqs.size() == 1 && reqs.iterator().next().close();
    }

    /**
     *
     */
    private void dumpPendingObjects() {
        U.warn(log, "Failed to wait for partition release future [topVer=" + startTopologyVersion() +
            ", node=" + cctx.localNodeId() + "]. Dumping pending objects that might be the cause: ");

        cctx.exchange().dumpDebugInfo();
    }

    /**
     * @param cacheId Cache ID to check.
     * @return {@code True} if cache is stopping by this exchange.
     */
    private boolean stopping(int cacheId) {
        boolean stopping = false;

        if (!F.isEmpty(reqs)) {
            for (DynamicCacheChangeRequest req : reqs) {
                if (cacheId == CU.cacheId(req.cacheName())) {
                    stopping = req.stop();

                    break;
                }
            }
        }

        return stopping;
    }

    /**
     * Starts dynamic caches.
     * @throws IgniteCheckedException If failed.
     */
    private void startCaches() throws IgniteCheckedException {
        cctx.cache().prepareCachesStart(F.view(reqs, new IgnitePredicate<DynamicCacheChangeRequest>() {
            @Override public boolean apply(DynamicCacheChangeRequest req) {
                return req.start();
            }
        }), exchId.topologyVersion());
    }

    /**
     *
     */
    private void blockGateways() {
        for (DynamicCacheChangeRequest req : reqs) {
            if (req.stop() || req.close())
                cctx.cache().blockGateway(req);
        }
    }

    /**
     * @param node Node.
     * @param id ID.
     * @throws IgniteCheckedException If failed.
     */
    private void sendLocalPartitions(ClusterNode node, @Nullable GridDhtPartitionExchangeId id)
        throws IgniteCheckedException {
        GridDhtPartitionsSingleMessage m = new GridDhtPartitionsSingleMessage(id,
            clientOnlyExchange,
            cctx.versions().last());

        for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
            if (!cacheCtx.isLocal()) {
                GridDhtPartitionMap2 locMap = cacheCtx.topology().localPartitionMap();

                if (node.version().compareTo(GridDhtPartitionMap2.SINCE) < 0)
                    locMap = new GridDhtPartitionMap(locMap.nodeId(), locMap.updateSequence(), locMap.map());

                m.addLocalPartitionMap(cacheCtx.cacheId(), locMap);

                m.partitionUpdateCounters(cacheCtx.cacheId(), cacheCtx.topology().updateCounters());
            }
        }

        if (log.isDebugEnabled())
            log.debug("Sending local partitions [nodeId=" + node.id() + ", exchId=" + exchId + ", msg=" + m + ']');

        cctx.io().send(node, m, SYSTEM_POOL);
    }

    /**
     * @param nodes Nodes.
     * @throws IgniteCheckedException If failed.
     */
    private void sendAllPartitions(Collection<? extends ClusterNode> nodes)
        throws IgniteCheckedException {
        AffinityTopologyVersion resTopVer = resExchId.topologyVersion();

        GridDhtPartitionsFullMessage m = new GridDhtPartitionsFullMessage(resExchId,
            lastVer.get(),
            resTopVer);

        boolean useOldApi = false;

        for (ClusterNode node : nodes) {
            if (node.version().compareTo(GridDhtPartitionMap2.SINCE) < 0)
                useOldApi = true;
        }

        for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
            if (!cacheCtx.isLocal()) {
                AffinityTopologyVersion startTopVer = cacheCtx.startTopologyVersion();

                boolean ready = startTopVer == null || startTopVer.compareTo(resTopVer) <= 0;

                if (ready) {
                    GridDhtPartitionFullMap locMap = cacheCtx.topology().partitionMap(true);

                    if (useOldApi) {
                        locMap = new GridDhtPartitionFullMap(locMap.nodeId(),
                            locMap.nodeOrder(),
                            locMap.updateSequence(),
                            locMap);
                    }

                    m.addFullPartitionsMap(cacheCtx.cacheId(), locMap);

                    m.addPartitionUpdateCounters(cacheCtx.cacheId(), cacheCtx.topology().updateCounters());
                }
            }
        }

        // It is important that client topologies be added after contexts.
        for (GridClientPartitionTopology top : cctx.exchange().clientTopologies()) {
            m.addFullPartitionsMap(top.cacheId(), top.partitionMap(true));

            m.addPartitionUpdateCounters(top.cacheId(), top.updateCounters());
        }

        if (log.isDebugEnabled())
            log.debug("Sending full partition map [nodeIds=" + F.viewReadOnly(nodes, F.node2id()) +
                ", exchId=" + exchId + ", msg=" + m + ']');

        cctx.io().safeSend(nodes, m, SYSTEM_POOL, null);
    }

    /**
     * @param oldestNode Oldest node.
     */
    private void sendPartitions(ClusterNode oldestNode) {
        try {
            sendLocalPartitions(oldestNode, exchId);
        }
        catch (ClusterTopologyCheckedException ignore) {
            if (log.isDebugEnabled())
                log.debug("Oldest node left during partition exchange [nodeId=" + oldestNode.id() +
                    ", exchId=" + exchId + ']');
        }
        catch (IgniteCheckedException e) {
            scheduleRecheck();

            U.error(log, "Failed to send local partitions to oldest node (will retry after timeout) [oldestNodeId=" +
                oldestNode.id() + ", exchId=" + exchId + ']', e);
        }
    }

    /**
     * @return {@code True} if succeeded.
     */
    private void spreadPartitions() {
        // TODO IGNITE-1837.
//        try {
//            sendAllPartitions(srvNodes, exchId);
//
//            return true;
//        }
//        catch (IgniteCheckedException e) {
//            scheduleRecheck();
//
//            if (!X.hasCause(e, InterruptedException.class))
//                U.error(log, "Failed to send full partition map to nodes (will retry after timeout) [nodes=" +
//                    F.nodeId8s(srvNodes) + ", exchangeId=" + exchId + ']', e);
//
//            return false;
//        }
    }

    /** {@inheritDoc} */
    @Override public boolean onDone(AffinityTopologyVersion res, Throwable err) {
        Map<Integer, Boolean> m = null;

        for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
            if (cacheCtx.config().getTopologyValidator() != null && !CU.isSystemCache(cacheCtx.name())) {
                if (m == null)
                    m = new HashMap<>();

                m.put(cacheCtx.cacheId(), cacheCtx.config().getTopologyValidator().validate(discoEvt.topologyNodes()));
            }
        }

        cacheValidRes = m != null ? m : Collections.<Integer, Boolean>emptyMap();

        cctx.cache().onExchangeDone(exchId.topologyVersion(), reqs, err);

//        log.info("Finish exchange [topVer=" + startTopologyVersion() +
//            ", resTopVer=" + res +
//            ", err=" + err +
//            ", evt=" + discoEvt + ']');

        cctx.exchange().onExchangeDone(this, resExchId.topologyVersion(), err);

        if (super.onDone(res, err) && !dummy && !forcePreload) {
            if (log.isDebugEnabled())
                log.debug("Completed partition exchange [localNode=" + cctx.localNodeId() + ", exchange= " + this +
                    "duration=" + duration() + ", durationFromInit=" + (U.currentTimeMillis() - initTs) + ']');

            initFut.onDone(err == null);

            GridTimeoutObject timeoutObj = this.timeoutObj;

            // Deschedule timeout object.
            if (timeoutObj != null)
                cctx.kernalContext().timeout().removeTimeoutObject(timeoutObj);

            if (exchId.isLeft()) {
                for (GridCacheContext cacheCtx : cctx.cacheContexts())
                    cacheCtx.config().getAffinity().removeNode(exchId.nodeId());
            }

            return true;
        }

        return dummy;
    }

    /** {@inheritDoc} */
    @Override public Throwable validateCache(GridCacheContext cctx) {
        Throwable err = error();

        if (err != null)
            return err;

        if (cctx.config().getTopologyValidator() != null) {
            Boolean res = cacheValidRes.get(cctx.cacheId());

            if (res != null && !res) {
                return new IgniteCheckedException("Failed to perform cache operation " +
                    "(cache topology is not valid): " + cctx.name());
            }
        }

        return null;
    }

    /**
     * Cleans up resources to avoid excessive memory usage.
     */
    public void cleanUp() {
        topSnapshot.set(null);
        singleMsgs.clear();
        fullMsgs.clear();
        partReleaseFut = null;
    }

    /**
     * @param node Sender node.
     * @param msg Single partition info.
     */
    public void onReceive(final ClusterNode node, final GridDhtPartitionsSingleMessage msg) {
        assert msg != null;

        assert msg.exchangeId().compareTo(exchId) >= 0;

        // Update last seen version.
        while (true) {
            GridCacheVersion old = lastVer.get();

            if (old == null || old.compareTo(msg.lastVersion()) < 0) {
                if (lastVer.compareAndSet(old, msg.lastVersion()))
                    break;
            }
            else
                break;
        }

        if (isDone()) {
            if (log.isDebugEnabled())
                log.debug("Received message for finished future (will reply only to sender) [msg=" + msg +
                    ", fut=" + this + ']');

            log.info("Coordinator send message for finished future [node=" + node.id() +
                ", topVer=" + exchId.topologyVersion() + ']');

            sendAllPartitions(node.id(), cctx.gridConfig().getNetworkSendRetryCount());
        }
        else {
            initFut.listen(new CI1<IgniteInternalFuture<Boolean>>() {
                @Override public void apply(IgniteInternalFuture<Boolean> f) {
                    try {
                        if (!f.get())
                            return;
                    }
                    catch (IgniteCheckedException e) {
                        U.error(log, "Failed to initialize exchange future: " + this, e);

                        return;
                    }

                    boolean allReceived = false;

                    synchronized (mux) {
                        assert crd != null;

                        if (crd.isLocal()) {
                            if (remaining.remove(node.id())) {
//                                log.info("Coordinator removed remaining message [node=" + node.id() +
//                                    ", topVer=" + exchId.topologyVersion() +
//                                    ", remaining=" + remaining + ']');

                                updatePartitionSingleMap(msg);

                                allReceived = remaining.isEmpty();
                            }
//                            else
//                                log.info("Coordinator skipped message [node=" + node.id() +
//                                    ", topVer=" + exchId.topologyVersion() +
//                                    ", remaining=" + remaining + ']');
                        }
                        else
                            singleMsgs.put(node, msg);
                    }

                    if (allReceived)
                        coordinatorAllReceived();
                }
            });
        }
    }

    /**
     *
     */
    private void coordinatorAllReceived() {
        if (onAllReceived())
            return;

        try {
            List<ClusterNode> nodes;

            synchronized (mux) {
                srvNodes.remove(cctx.localNode());

                nodes = new ArrayList<>(srvNodes);
            }

//            log.info("Coordinator finish exchange [topVer=" + exchId.topologyVersion() +
//                ", resTopVer=" + resExchId.topologyVersion() +
//                ", nodes=" + U.nodeIds(srvNodes) + ']');

            if (!nodes.isEmpty())
                sendAllPartitions(nodes);
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to send partitions full message: " + e, e);
        }

        onDone(resExchId.topologyVersion());
    }

    public boolean joinExchangeAdded(GridDhtPartitionExchangeId exchId) {
        synchronized (mux) {
            if (addedJoinEvts != null) {
                for (DiscoveryEvent evt : addedJoinEvts) {
                    if (exchId.topologyVersion().equals(new AffinityTopologyVersion(evt.topologyVersion())))
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * @param evts Events.
     */
    public boolean processJoinExchanges(Collection<DiscoveryEvent> evts) {
        synchronized (mux) {
            if (addedJoinEvts == null)
                addedJoinEvts = new ArrayList<>();

            addedJoinEvts.addAll(evts);

            boolean added = false;

            for (DiscoveryEvent evt : evts) {
                ClusterNode node = evt.eventNode();

                if (!CU.clientNode(node) && cctx.discovery().alive(evt.eventNode())) {
                    added = true;

                    boolean add = remaining.add(evt.eventNode().id());

                    assert add : evt;

                    add = srvNodes.add(evt.eventNode());

//                    log.info("Coordinator added join message [node=" + evt.eventNode().id() +
//                        ", topVer=" + exchId.topologyVersion() +
//                        ", remaining=" + remaining + ']');

                    assert add : evt;
                }
            }

            return added;
        }
    }

    /**
     * @return {@code True} if there are pending exchanges which can be handled as part of current exchange.
     */
    private boolean onAllReceived() {
        if (discoEvt.type() == EVT_NODE_JOINED && !CU.clientNode(discoEvt.eventNode())) {
            if (cctx.exchange().beforeFinishJoinExchange(this))
                return true;
        }

        List<DiscoveryEvent> addedJoinEvts;

        synchronized (mux) {
            addedJoinEvts = this.addedJoinEvts;
        }

        if (addedJoinEvts != null) {
            for (DiscoveryEvent evt : addedJoinEvts) {
                AffinityTopologyVersion topVer = new AffinityTopologyVersion(evt.topologyVersion());

                for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                    if (cacheCtx.isLocal())
                        continue;

                    if (CU.clientNode(evt.eventNode()))
                        cacheCtx.affinity().clientEventTopologyChange(evt, topVer);
                    else
                        cacheCtx.affinity().calculateAffinity(topVer, evt);
                }
            }

            DiscoveryEvent last = addedJoinEvts.get(addedJoinEvts.size() - 1);

            assert resExchId.equals(exchId);

            resExchId = new GridDhtPartitionExchangeId(last.eventNode().id(),
                last.type(),
                new AffinityTopologyVersion(last.topologyVersion()));
        }

        return false;
    }

    /**
     * @param nodeId Node ID.
     * @param retryCnt Number of retries.
     */
    private void sendAllPartitions(final UUID nodeId, final int retryCnt) {
        ClusterNode n = cctx.node(nodeId);

        try {
            if (n != null)
                sendAllPartitions(F.asList(n));
        }
        catch (IgniteCheckedException e) {
            if (e instanceof ClusterTopologyCheckedException || !cctx.discovery().alive(n)) {
                log.debug("Failed to send full partition map to node, node left grid " +
                    "[rmtNode=" + nodeId + ", exchangeId=" + exchId + ']');

                return;
            }

            if (retryCnt > 0) {
                long timeout = cctx.gridConfig().getNetworkSendRetryDelay();

                LT.error(log, e, "Failed to send full partition map to node (will retry after timeout) " +
                    "[node=" + nodeId + ", exchangeId=" + exchId + ", timeout=" + timeout + ']');

                cctx.time().addTimeoutObject(new GridTimeoutObjectAdapter(timeout) {
                    @Override public void onTimeout() {
                        sendAllPartitions(nodeId, retryCnt - 1);
                    }
                });
            }
            else
                U.error(log, "Failed to send full partition map [node=" + n + ", exchangeId=" + exchId + ']', e);
        }
    }

    /**
     * @param node Sender node.
     * @param msg Full partition info.
     */
    public void onReceive(final ClusterNode node, final GridDhtPartitionsFullMessage msg) {
        assert msg != null;

        if (isDone()) {
            if (log.isDebugEnabled())
                log.debug("Received message for finished future [msg=" + msg + ", fut=" + this + ']');

            return;
        }

        if (log.isDebugEnabled())
            log.debug("Received full partition map from node [node=" + node + ", msg=" + msg + ']');

        assert msg.topologyVersion().compareTo(exchId.topologyVersion()) >= 0;

        initFut.listen(new CI1<IgniteInternalFuture<Boolean>>() {
            @Override public void apply(IgniteInternalFuture<Boolean> f) {
                try {
                    if (!f.get())
                        return;
                }
                catch (IgniteCheckedException e) {
                    U.error(log, "Failed to initialize exchange future: " + this, e);

                    return;
                }

                synchronized (mux) {
                    if (crd == null)
                        return;

                    if (!crd.equals(node)) {
                        if (log.isDebugEnabled())
                            log.debug("Received full partition map from unexpected node [oldest=" + crd.id() +
                                ", nodeId=" + node.id() + ']');

//                        log.info("Received full partitions message from unexpected node [node=" + node.id() +
//                            ", crd=" + crd.id() +
//                            ", msgTopVer=" + msg.topologyVersion() +
//                            ", exchTopVer=" + exchId.topologyVersion() + ']');

                        if (node.order() > crd.order())
                            fullMsgs.put(node, msg);

                        return;
                    }

//                    log.info("Received full partitions message [node=" + node.id() +
//                        ", msgTopVer=" + msg.topologyVersion() +
//                        ", exchTopVer=" + exchId.topologyVersion() + ']');

                    remaining.clear();
                }

                assert msg.exchangeId().compareTo(exchId) >= 0;

                assert msg.lastVersion() != null;

                cctx.versions().onReceived(node.id(), msg.lastVersion());

                updatePartitionFullMap(msg);

                final AffinityTopologyVersion resTopVer = msg.topologyVersion();

                if (!resTopVer.equals(startTopologyVersion())) {
                    assert resTopVer.compareTo(startTopologyVersion()) >= 0;

                    IgniteInternalFuture<NavigableMap<AffinityTopologyVersion, DiscoveryEvent>> discoFut =
                        cctx.exchange().discoveryFuture(startTopologyVersion(), resTopVer);

                    discoFut.listen(new CI1<IgniteInternalFuture<NavigableMap<AffinityTopologyVersion, DiscoveryEvent>>>() {
                        @Override public void apply(IgniteInternalFuture<NavigableMap<AffinityTopologyVersion, DiscoveryEvent>> fut) {
                            try {
                                NavigableMap<AffinityTopologyVersion, DiscoveryEvent> evts = fut.get();

                                for (Map.Entry<AffinityTopologyVersion, DiscoveryEvent> e : evts.entrySet()) {
                                    for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                                        if (cacheCtx.isLocal())
                                            continue;

                                        DiscoveryEvent evt = e.getValue();

                                        if (evt.eventNode().isClient())
                                            cacheCtx.affinity().clientEventTopologyChange(evt, e.getKey());
                                        else
                                            cacheCtx.affinity().calculateAffinity(e.getKey(), evt);
                                    }
                                }

                                Map.Entry<AffinityTopologyVersion, DiscoveryEvent> last = evts.lastEntry();

                                DiscoveryEvent evt = last.getValue();

                                resExchId =
                                    new GridDhtPartitionExchangeId(evt.eventNode().id(), evt.type(), last.getKey());

                                onDone(resTopVer);
                            }
                            catch (IgniteCheckedException e) {
                                onDone(e);
                            }
                        }
                    });

                    return;
                }

                onDone(resTopVer);
            }
        });
    }

    /**
     * Updates partition map in all caches.
     *
     * @param msg Partitions full messages.
     */
    private void updatePartitionFullMap(GridDhtPartitionsFullMessage msg) {
        for (Map.Entry<Integer, GridDhtPartitionFullMap> entry : msg.partitions().entrySet()) {
            Integer cacheId = entry.getKey();

            Map<Integer, Long> cntrMap = msg.partitionUpdateCounters(cacheId);

            GridCacheContext cacheCtx = cctx.cacheContext(cacheId);

            if (cacheCtx != null)
                cacheCtx.topology().update(msg.exchangeId(), entry.getValue(), cntrMap);
            else {
                ClusterNode oldest = CU.oldestAliveCacheServerNode(cctx, AffinityTopologyVersion.NONE);

                if (oldest != null && oldest.isLocal())
                    cctx.exchange().clientTopology(cacheId, this).update(msg.exchangeId(), entry.getValue(), cntrMap);
            }
        }
    }

    /**
     * Updates partition map in all caches.
     *
     * @param msg Partitions single message.
     */
    private void updatePartitionSingleMap(GridDhtPartitionsSingleMessage msg) {
        for (Map.Entry<Integer, GridDhtPartitionMap2> entry : msg.partitions().entrySet()) {
            Integer cacheId = entry.getKey();
            GridCacheContext cacheCtx = cctx.cacheContext(cacheId);

            GridDhtPartitionTopology top = cacheCtx != null ? cacheCtx.topology() :
                cctx.exchange().clientTopology(cacheId, this);

            top.update(msg.exchangeId(), entry.getValue(), msg.partitionUpdateCounters(cacheId));
        }
    }

    /**
     * @param node Left node.
     */
    public void onNodeLeft(final ClusterNode node) {
        if (isDone())
            return;

        if (!enterBusy())
            return;

        try {
            // Wait for initialization part of this future to complete.
            initFut.listen(new CI1<IgniteInternalFuture<Boolean>>() {
                @Override public void apply(IgniteInternalFuture<Boolean> f) {
                    try {
                        if (!f.get())
                            return;
                    }
                    catch (IgniteCheckedException e) {
                        U.error(log, "Failed to initialize exchange future: " + this, e);

                        return;
                    }

                    if (isDone())
                        return;

                    if (!enterBusy())
                        return;

                    try {
                        boolean crdChanged = false;
                        boolean allReceived = false;

                        ClusterNode crd0;

                        synchronized (mux) {
                            srvNodes.remove(node);

                            boolean rmvd = remaining.remove(node.id());

                            if (node.equals(crd)) {
                                crdChanged = true;

                                crd = srvNodes.size() > 0 ? srvNodes.get(0) : null;
                            }

                            if (crd != null && crd.isLocal() && rmvd) {
                                allReceived = remaining.isEmpty();

//                                log.info("Coordinator received node left event [node=" + node.id() +
//                                    ", topVer=" + exchId.topologyVersion() +
//                                    ", remaining=" + remaining + ']');
                            }

                            crd0 = crd;
                        }

                        if (crd0 == null) {
                            assert cctx.kernalContext().clientNode() || cctx.localNode().isDaemon() : cctx.localNode();

                            List<ClusterNode> empty = Collections.emptyList();

                            for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                                List<List<ClusterNode>> affAssignment = new ArrayList<>(cacheCtx.affinity().partitions());

                                for (int i = 0; i < cacheCtx.affinity().partitions(); i++)
                                    affAssignment.add(empty);

                                cacheCtx.affinity().initializeAffinity(startTopologyVersion(), affAssignment);
                            }

                            onDone(startTopologyVersion());

                            return;
                        }

                        if (crd0.isLocal()) {
                            if (crdChanged && exchId.isJoined() && exchId.nodeId().equals(cctx.localNodeId())) {
                                try {
                                    for (GridCacheContext cacheCtx : cctx.cacheContexts()) {
                                        if (!cacheCtx.isLocal())
                                            cacheCtx.topology().beforeExchange(GridDhtPartitionsExchangeFuture.this);
                                    }
                                }
                                catch (IgniteCheckedException e) {
                                    onDone(e);

                                    return;
                                }
                            }

                            if (allReceived) {
                                coordinatorAllReceived();

                                return;
                            }

                            for (Map.Entry<ClusterNode, GridDhtPartitionsSingleMessage> m : singleMsgs.entrySet())
                                onReceive(m.getKey(), m.getValue());
                        }
                        else {
                            if (crdChanged) {
                                sendPartitions(crd0);

                                for (Map.Entry<ClusterNode, GridDhtPartitionsFullMessage> m : fullMsgs.entrySet())
                                    onReceive(m.getKey(), m.getValue());
                            }
                        }
                    }
                    finally {
                        leaveBusy();
                    }
                }
            });
        }
        finally {
            leaveBusy();
        }
    }

    /**
     *
     */
    private void recheck() {
        ClusterNode oldest;

        synchronized (mux) {
            oldest = crd;
        }

        if (oldest == null)
            return;

        // TODO IGNITE-1837.

        // If this is the oldest node.
//        if (oldest.id().equals(cctx.localNodeId())) {
//            Collection<UUID> remaining = remaining();
//
//            if (!remaining.isEmpty()) {
//                try {
//                    cctx.io().safeSend(cctx.discovery().nodes(remaining),
//                        new GridDhtPartitionsSingleRequest(exchId), SYSTEM_POOL, null);
//                }
//                catch (IgniteCheckedException e) {
//                    U.error(log, "Failed to request partitions from nodes [exchangeId=" + exchId +
//                        ", nodes=" + remaining + ']', e);
//                }
//            }
//            // Resend full partition map because last attempt failed.
//            else {
//                if (spreadPartitions())
//                    onDone(exchId.topologyVersion());
//            }
//        }
//        else
//            sendPartitions(oldest);
//
//        // Schedule another send.
//        scheduleRecheck();
    }

    /**
     *
     */
    private void scheduleRecheck() {
        if (!isDone()) {
            GridTimeoutObject old = timeoutObj;

            if (old != null)
                cctx.kernalContext().timeout().removeTimeoutObject(old);

            GridTimeoutObject timeoutObj = new GridTimeoutObjectAdapter(
                cctx.gridConfig().getNetworkTimeout() * Math.max(1, cctx.gridConfig().getCacheConfiguration().length)) {
                @Override public void onTimeout() {
                    cctx.kernalContext().closure().runLocalSafe(new Runnable() {
                        @Override public void run() {
                            if (isDone())
                                return;

                            if (!enterBusy())
                                return;

                            try {
                                U.warn(log,
                                    "Retrying preload partition exchange due to timeout [done=" + isDone() +
                                        ", dummy=" + dummy + ", exchId=" + exchId +
                                        ", rmtIds=" + ", remaining=" +
                                        ", initFut=" + initFut.isDone() +
                                        ", oldest=" + crd + ", evtLatch=" + evtLatch.getCount() +
                                        ", locNodeOrder=" + cctx.localNode().order() +
                                        ", locNodeId=" + cctx.localNode().id() + ']',
                                    "Retrying preload partition exchange due to timeout.");

                                recheck();
                            }
                            finally {
                                leaveBusy();
                            }
                        }
                    });
                }
            };

            this.timeoutObj = timeoutObj;

            cctx.kernalContext().timeout().addTimeoutObject(timeoutObj);
        }
    }

    /** {@inheritDoc} */
    @Override public int compareTo(GridDhtPartitionsExchangeFuture fut) {
        return exchId.compareTo(fut.exchId);
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        GridDhtPartitionsExchangeFuture fut = (GridDhtPartitionsExchangeFuture)o;

        return exchId.equals(fut.exchId);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return exchId.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        ClusterNode oldestNode;
        Set<UUID> remaining;

        synchronized (mux) {
            oldestNode = this.crd;
            remaining = new HashSet<>(this.remaining);
        }

        return S.toString(GridDhtPartitionsExchangeFuture.class, this,
            "oldest", oldestNode == null ? "null" : oldestNode.id(),
            "oldestOrder", oldestNode == null ? "null" : oldestNode.order(),
            "evtLatch", evtLatch == null ? "null" : evtLatch.getCount(),
            "remaining", remaining,
            "super", super.toString());
    }
}
