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

package org.apache.ignite.internal.processors.cache.distributed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.managers.communication.GridIoMessage;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsFullMessage;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsSingleMessage;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.cache.CacheAtomicWriteOrderMode.PRIMARY;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cache.CacheRebalanceMode.ASYNC;
import static org.apache.ignite.cache.CacheRebalanceMode.SYNC;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;

/**
 *
 */
public class IgniteOptimizedPartitionsExchangeTest extends GridCommonAbstractTest {
    /** */
    protected static final TcpDiscoveryIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);

    /** */
    private ThreadLocal<Boolean> client = new ThreadLocal<>();

    /** */
    private ThreadLocal<IgnitePredicate<GridIoMessage>> blockP = new ThreadLocal<>();

    /** */
    private final Map<String, TestRecordingCommunicationSpi> commSpis = new HashMap<>();

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setIpFinder(ipFinder);

        Boolean client0 = client.get();

        if (client0 != null) {
            cfg.setClientMode(client0);

            client.remove();
        }

        List<CacheConfiguration> ccfgs = new ArrayList<>();

        ccfgs.addAll(cacheConfigurations(ATOMIC));
        ccfgs.addAll(cacheConfigurations(TRANSACTIONAL));

        cfg.setCacheConfiguration(ccfgs.toArray(new CacheConfiguration[ccfgs.size()]));

        TestRecordingCommunicationSpi commSpi = new TestRecordingCommunicationSpi();

        IgnitePredicate<GridIoMessage> p = blockP.get();

        if (p != null) {
            commSpi.blockMessages(p);

            blockP.remove();
        }

        synchronized (commSpis) {
            commSpis.put(gridName, commSpi);

            commSpis.notifyAll();
        }

        cfg.setCommunicationSpi(commSpi);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        super.afterTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoin1() throws Exception {
        multipleJoin(2, null);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoin2() throws Exception {
        multipleJoin(10, null);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinWithClients1() throws Exception {
        multipleJoin(3, F.asSet(3));
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinWithClients2() throws Exception {
        multipleJoin(4, F.asSet(3, 4));
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinWithClients4() throws Exception {
        multipleJoin(10, F.asSet(3, 5, 7, 9));
    }

    /**
     * @param nodes Number of nodes joining concurrently.
     * @param clientIdxs Client nodes indexes.
     * @throws Exception If failed.
     */
    private void multipleJoin(int nodes, @Nullable final Set<Integer> clientIdxs) throws Exception {
        assert nodes > 1;

        Ignite ignite0 = startGrid(0);

        TestRecordingCommunicationSpi spi0 = waitSpi(getTestGridName(0));

        spi0.record(GridDhtPartitionsFullMessage.class);

        List<IgniteInternalFuture<?>> futs = new ArrayList<>();

        for (int i = 0; i < nodes; i++) {
            final int nodeIdx = i + 1;
            final int topVer = i + 2;

            IgniteInternalFuture<?> fut = GridTestUtils.runAsync(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    blockP.set(blockSinglePartitions(topVer));

                    if (clientIdxs != null && clientIdxs.contains(nodeIdx))
                        client.set(true);

                    startGrid(nodeIdx);

                    return null;
                }
            }, "start-" + getTestGridName(nodeIdx));

            futs.add(fut);

            ((IgniteKernal)ignite0).context().discovery().topologyFuture(topVer).get();
        }

        List<TestRecordingCommunicationSpi> spis = new ArrayList<>();

        for (int i = 0; i < nodes; i++)
            spis.add(waitSpi(getTestGridName(i + 1)));

        ((IgniteKernal)ignite0).context().discovery().topologyFuture(nodes + 1).get();

        for (TestRecordingCommunicationSpi spi : spis)
            spi.stopBlock();

        for (IgniteInternalFuture<?> fut : futs)
            fut.get();

        assertEquals(nodes, exchangeMessageCount(spi0));

        checkNodes(topVer(nodes + 1), nodes + 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testConcurrentJoinDelayMessage() throws Exception {
        final AtomicInteger idx = new AtomicInteger(0);

        final int THREADS = 20;

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                blockP.set(delaySinglePartitions());

                startGrid(idx.getAndIncrement());

                return null;
            }
        }, THREADS, "start");

        checkNodes(topVer(THREADS), THREADS);
    }

    /**
     * @throws Exception If failed.
     */
    public void testConcurrentJoinDelayMessageWithClients() throws Exception {
        startGrid(0);

        final AtomicInteger idx = new AtomicInteger(1);

        final int THREADS = 30;

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                blockP.set(delaySinglePartitions());

                int idx0 = idx.getAndIncrement();

                client.set(idx0 % 2 == 1);

                startGrid(idx0);

                return null;
            }
        }, THREADS, "start");

        checkNodes(topVer(THREADS + 1), THREADS + 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinJoinedLeft1() throws Exception {
        multipleJoinJoinedLeft(1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinJoinedLeft2() throws Exception {
        multipleJoinJoinedLeft(2);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinJoinedLeft3() throws Exception {
        multipleJoinJoinedLeft(3);
    }

    /**
     * @param failedIdx Failed node index.
     * @throws Exception If failed.
     */
    public void multipleJoinJoinedLeft(int failedIdx) throws Exception {
        Ignite ignite0 = startGrid(0);

        List<IgniteInternalFuture<?>> futs = new ArrayList<>();

        final int NODES = 3;

        for (int i = 0; i < NODES; i++) {
            final int nodeIdx = i + 1;
            final int topVer = i + 2;

            IgniteInternalFuture<?> fut = GridTestUtils.runAsync(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    blockP.set(blockSinglePartitions(topVer));

                    startGrid(nodeIdx);

                    return null;
                }
            }, "start-" + getTestGridName(nodeIdx));

            futs.add(fut);

            ((IgniteKernal)ignite0).context().discovery().topologyFuture(topVer).get();
        }

        List<TestRecordingCommunicationSpi> spis = new ArrayList<>();

        for (int i = 0; i < NODES; i++) {
            int nodeIdx = i + 1;

            if (nodeIdx != failedIdx)
                spis.add(waitSpi(getTestGridName(nodeIdx)));
        }

        ((IgniteKernal)ignite0).context().discovery().topologyFuture(NODES + 1).get();

        IgniteKernal failNode = IgnitionEx.gridx(getTestGridName(failedIdx));

        ((TcpDiscoverySpi)failNode.configuration().getDiscoverySpi()).simulateNodeFailure();

        for (TestRecordingCommunicationSpi spi : spis)
            spi.stopBlock();

        for (int i = 0; i < NODES; i++) {
            int nodeIdx = i + 1;

            IgniteInternalFuture<?> fut = futs.get(i);

            if (nodeIdx != failedIdx)
                fut.get();
            else {
                try {
                    fut.cancel();

                    fut.get();
                }
                catch (Exception ignore) {
                    // No-op.
                }
            }
        }

        checkNodes(topVer(NODES + 2), NODES);
    }

    /**
     * @throws Exception If failed.
     */
    public void _testMultipleJoinCoordinatorLeft1() throws Exception {
        multipleJoinCoordinatorLeft(0);
    }

    /**
     * @throws Exception If failed.
     */
    public void _testMultipleJoinCoordinatorLeft2() throws Exception {
        multipleJoinCoordinatorLeft(1);
    }

    /**
     * @throws Exception If failed.
     */
    public void _testMultipleJoinCoordinatorLeft3() throws Exception {
        multipleJoinCoordinatorLeft(2);
    }

    /**
     * @param joinFailCnt Number of joining nodes to fail.
     * @throws Exception If failed.
     */
    private void multipleJoinCoordinatorLeft(int joinFailCnt) throws Exception {
        Ignite ignite0 = startGrid(0);

        List<IgniteInternalFuture<?>> futs = new ArrayList<>();

        final int NODES = 5;

        for (int i = 0; i < NODES; i++) {
            final int nodeIdx = i + 1;
            final int topVer = i + 2;

            IgniteInternalFuture<?> fut = GridTestUtils.runAsync(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    blockP.set(blockSinglePartitions(topVer));

                    startGrid(nodeIdx);

                    return null;
                }
            }, "start-" + getTestGridName(nodeIdx));

            futs.add(fut);

            ((IgniteKernal)ignite0).context().discovery().topologyFuture(topVer).get();
        }

        TestRecordingCommunicationSpi spi0 = waitSpi(getTestGridName(0));

        spi0.blockMessages(new IgnitePredicate<GridIoMessage>() {
            @Override public boolean apply(GridIoMessage msg) {
                return msg.message() instanceof GridDhtPartitionsFullMessage;
            }
        });

        List<TestRecordingCommunicationSpi> spis = new ArrayList<>();

        // TODO

        for (int i = 0; i < NODES; i++) {
            int nodeIdx = i + 1;

            //if (nodeIdx != failedIdx)
                spis.add(waitSpi(getTestGridName(nodeIdx)));
        }

        for (TestRecordingCommunicationSpi spi : spis)
            spi.stopBlock();

        stopGrid(0);

        for (int i = 0; i < NODES - 1; i++) {
            int nodeIdx = i + 1;

            IgniteInternalFuture<?> fut = futs.get(i);

            if (true)
                fut.get();
            else {
                try {
                    fut.cancel();

                    fut.get();
                }
                catch (Exception ignore) {
                    // No-op.
                }
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinAnotherExchangeInProgress() throws Exception {
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinJoinedStartCache() throws Exception {
    }

    /**
     *
     * @param expTopVer Expected topology version.
     * @param expNodes Expected nodes number.
     * @throws Exception If failed.
     */
    private void checkNodes(AffinityTopologyVersion expTopVer, int expNodes) throws Exception {
        List<Ignite> nodes = G.allGrids();

        assertEquals(expNodes, nodes.size());

        log.info("Check nodes [topVer=" + expTopVer + ", nodes=" + nodes.size() + ']');

        Map<String, List<List<ClusterNode>>> aff = new HashMap<>();

        for (Ignite node : nodes) {
            GridKernalContext ctx = ((IgniteKernal)node).context();

            IgniteInternalFuture<?> fut = ctx.cache().context().exchange().affinityReadyFuture(expTopVer);

            if (fut != null) {
                log.info("Wait future [node=" + node.name() +
                    ", readyVer=" + ctx.cache().context().exchange().readyAffinityVersion() +
                    ", fut=" + fut + ']');

                fut.get();
            }

            assertEquals(expTopVer, ctx.cache().context().exchange().readyAffinityVersion());

            for (GridCacheContext cctx : ctx.cache().context().cacheContexts()) {
                List<List<ClusterNode>> aff1 = aff.get(cctx.name());
                List<List<ClusterNode>> aff2 = cctx.affinity().assignments(expTopVer);

                if (aff1 == null)
                    aff.put(cctx.name(), aff2);
                else
                    assertEquals(aff1, aff2);
            }
        }

        checkCaches(nodes);

        awaitPartitionMapExchange();
    }

    /**
     * @param nodes Nodes.
     */
    private void checkCaches(List<Ignite> nodes) {
        for (Ignite node : nodes) {
            Collection<String> cacheNames = node.cacheNames();

            log.info("Check caches [node=" + node.name() + ", caches=" + cacheNames + ']');

            assertFalse(cacheNames.isEmpty());

            for (String cacheName : cacheNames) {
                IgniteCache<Object, Object> cache = node.cache(cacheName);

                assertNotNull(cache);

                Long val = System.currentTimeMillis();

                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                for (int i = 0; i < 50; i++) {
                    int key = rnd.nextInt(100_000);

                    cache.put(key, val);

                    assertEquals(val, cache.get(key));

                    cache.remove(key);

                    assertNull(cache.get(key));
                }
            }
        }
    }

    /**
     * @return Message delay predicate.
     */
    private IgnitePredicate<GridIoMessage> delaySinglePartitions() {
        return new IgnitePredicate<GridIoMessage>() {
            @Override public boolean apply(GridIoMessage msg) {
                if (msg.message() instanceof GridDhtPartitionsSingleMessage) {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
                    }
                    catch (InterruptedException ignore) {
                        // No-op.
                    }
                }

                return false;
            }
        };
    }

    /**
     * @param topVer Message topology version.
     * @return Message block predicate.
     */
    private IgnitePredicate<GridIoMessage> blockSinglePartitions(final long topVer) {
        return new IgnitePredicate<GridIoMessage>() {
            @Override public boolean apply(GridIoMessage msg0) {
                Message msg = msg0.message();

                if (msg instanceof GridDhtPartitionsSingleMessage) {
                    GridDhtPartitionsSingleMessage pMsg = (GridDhtPartitionsSingleMessage)msg;

                    return pMsg.exchangeId().topologyVersion().topologyVersion() == topVer;
                }

                return false;
            }
        };
    }

    /**
     * @param atomicityMode Cache atomicity mode.
     * @return Cache configurations.
     */
    private List<CacheConfiguration> cacheConfigurations(CacheAtomicityMode atomicityMode) {
        List<CacheConfiguration> caches = new ArrayList<>();

        int idx = 0;

        {
            CacheConfiguration ccfg = cacheConfiguration("cache-" + idx++ + "-" + atomicityMode, atomicityMode);
            ccfg.setRebalanceMode(ASYNC);
            ccfg.setBackups(0);

            caches.add(ccfg);
        }

        {
            CacheConfiguration ccfg = cacheConfiguration("cache-" + idx++ + "-" + atomicityMode, atomicityMode);
            ccfg.setRebalanceMode(SYNC);
            ccfg.setBackups(0);

            caches.add(ccfg);
        }

        {
            CacheConfiguration ccfg = cacheConfiguration("cache-" + idx++ + "-" + atomicityMode, atomicityMode);
            ccfg.setRebalanceMode(ASYNC);
            ccfg.setBackups(1);

            caches.add(ccfg);
        }

        {
            CacheConfiguration ccfg = cacheConfiguration("cache-" + idx++ + "-" + atomicityMode, atomicityMode);
            ccfg.setRebalanceMode(SYNC);
            ccfg.setBackups(1);

            caches.add(ccfg);
        }

        {
            CacheConfiguration ccfg = cacheConfiguration("cache-" + idx++ + "-" + atomicityMode, atomicityMode);
            ccfg.setRebalanceMode(ASYNC);
            ccfg.setBackups(2);

            caches.add(ccfg);
        }

        {
            CacheConfiguration ccfg = cacheConfiguration("cache-" + idx++ + "-" + atomicityMode, atomicityMode);
            ccfg.setRebalanceMode(SYNC);
            ccfg.setBackups(2);

            caches.add(ccfg);
        }

        {
            CacheConfiguration ccfg = cacheConfiguration("cache-" + idx++ + "-" + atomicityMode, atomicityMode);
            ccfg.setRebalanceMode(ASYNC);
            ccfg.setCacheMode(REPLICATED);

            caches.add(ccfg);
        }

        {
            CacheConfiguration ccfg = cacheConfiguration("cache-" + idx + "-" + atomicityMode, atomicityMode);
            ccfg.setRebalanceMode(SYNC);
            ccfg.setCacheMode(REPLICATED);

            caches.add(ccfg);
        }

        return caches;
    }

    /**
     * @param name Cache name.
     * @param atomicityMode Atomicity mode.
     * @return Cache configuration.
     */
    private CacheConfiguration cacheConfiguration(String name, CacheAtomicityMode atomicityMode) {
        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setName(name);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setAtomicWriteOrderMode(PRIMARY);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setStartSize(1024);

        return ccfg;
    }

    /**
     * @param nodeName Node name.
     * @return Communication SPI.
     * @throws Exception If failed.
     */
    private TestRecordingCommunicationSpi waitSpi(String nodeName) throws Exception {
        synchronized (commSpis) {
            while (!commSpis.containsKey(nodeName))
                commSpis.wait();

            return commSpis.get(nodeName);
        }
    }

    /**
     * @param spi Message recording SPI.
     * @return Number of recorded {@link GridDhtPartitionsFullMessage} sent for exchange.
     */
    private int exchangeMessageCount(TestRecordingCommunicationSpi spi) {
        List<Object> msgs = spi.recordedMessages();

        int cnt = 0;

        for (Object msg : msgs) {
            assert msg instanceof GridDhtPartitionsFullMessage;

            if (((GridDhtPartitionsFullMessage) msg).exchangeId() != null)
                cnt++;
        }

        return cnt;
    }
}
