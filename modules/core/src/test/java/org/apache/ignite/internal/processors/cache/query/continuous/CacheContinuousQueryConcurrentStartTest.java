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

package org.apache.ignite.internal.processors.cache.query.continuous;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMemoryMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteFutureTimeoutCheckedException;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.util.typedef.PA;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.spi.eventstorage.memory.MemoryEventStorageSpi;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.NotNull;

import static org.apache.ignite.cache.CacheAtomicWriteOrderMode.PRIMARY;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheMemoryMode.OFFHEAP_TIERED;
import static org.apache.ignite.cache.CacheMemoryMode.ONHEAP_TIERED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.PRIMARY_SYNC;

/**
 *
 */
public class CacheContinuousQueryConcurrentStartTest extends GridCommonAbstractTest {
    /** */
    public static final int KEYS = 10;

    /** */
    private static TcpDiscoveryIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);

    /** */
    private static final int NODES = 5;

    /** */
    public static final int ITERATION_CNT = 100;

    /** */
    public static boolean client;

    /** */
    private static volatile Integer blockedKey;

    /** */
    private static CountDownLatch latch;

    /** */
    private static CountDownLatch waitingLatch;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setIpFinder(ipFinder);
        ((TcpCommunicationSpi)cfg.getCommunicationSpi()).setSharedMemoryPort(-1);

        cfg.setClientMode(client);

        MemoryEventStorageSpi storeSpi = new MemoryEventStorageSpi();
        storeSpi.setExpireCount(100);

        cfg.setEventStorageSpi(storeSpi);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        client = false;

        startGridsMultiThreaded(nodes() - 1);

        client = true;

        startGrid(nodes());
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        latch = new CountDownLatch(1);
        waitingLatch = new CountDownLatch(1);
    }

    /**
     * @return Count of nodes.
     */
    protected int nodes() {
        return NODES;
    }

    /**
     * @throws Exception If failed.
     */
    public void testAtomicOnheap() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(ATOMIC, ONHEAP_TIERED, PRIMARY_SYNC);

        doConcurrentStart(ccfg);
    }

    /**
     * @throws Exception If failed.
     */
    public void testAtomicOffheap() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(ATOMIC, OFFHEAP_TIERED, PRIMARY_SYNC);

        doConcurrentStart(ccfg);
    }

    /**
     * @param ccfg Cache configuration.
     * @throws Exception If failed.
     */
    private void doConcurrentStart(final CacheConfiguration ccfg) throws Exception {
        for (int n = 0; n < nodes() - 1; n++) {
            ccfg.setName(ccfg.getName() + n);

            final IgniteCache cache = ignite(n).createCache(ccfg);

            int partition = 0;

            final List<Integer> keys = keysByPartition(partition, 10, affinity(cache));

            assertEquals(10, keys.size());

            blockedKey = keys.get(0);
            latch = new CountDownLatch(1);
            waitingLatch = new CountDownLatch(1);

            IgniteInternalFuture<Object> f = GridTestUtils.runAsync(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    cache.put(blockedKey, new TestValue(1));

                    return null;
                }
            }, "putter");

            U.await(waitingLatch, 5, TimeUnit.SECONDS);
            assertFalse(f.isDone());

            final List<Object> evts = new ArrayList<>();

            ContinuousQuery qry = new ContinuousQuery();

            qry.setLocalListener(new CacheEntryUpdatedListener() {
                @Override public void onUpdated(Iterable iterable) throws CacheEntryListenerException {
                    for (Object o : iterable)
                        evts.add(o);
                }
            });

            QueryCursor queryCur = cache.query(qry);

            int middle = 5;

            for (int i = 1; i < middle; i++)
                cache.put(keys.get(i), new TestValue(ThreadLocalRandom.current().nextInt(100)));

            latch.countDown();
            f.get();

            for (int i = middle; i < keys.size(); i++)
                cache.put(keys.get(i), new TestValue(ThreadLocalRandom.current().nextInt(100)));

            final int expSize = keys.size() - 1;

            GridTestUtils.waitForCondition(new PA() {
                @Override public boolean apply() {
                    return evts.size() >= expSize;
                }
            }, 10_000);

            assertEquals("Unexpected count of events: " + n, expSize, evts.size());

            queryCur.close();

            ignite(n).destroyCache(ccfg.getName());
        }
    }

    /**
     * @param partition Partition.
     * @param cntKeys Count of keys.
     * @param aff Affinity.
     * @return List of keys per partition.
     */
    @NotNull private List<Integer> keysByPartition(int partition, int cntKeys, Affinity aff) {
        final List<Integer> keys = new ArrayList<>();

        for (int i = 0; i < 1_000_000; i++) {
            if (aff.partition(i) == partition)
                keys.add(i);

            if (keys.size() == cntKeys)
                break;
        }
        return keys;
    }

    /**
     * @param atomicityMode Cache atomicity mode.
     * @param memoryMode Cache memory mode.
     * @param writeMode Cache write mode.
     * @return Cache configuration.
     */
    protected CacheConfiguration<Object, Object> cacheConfiguration(
        CacheAtomicityMode atomicityMode,
        CacheMemoryMode memoryMode,
        CacheWriteSynchronizationMode writeMode) {
        CacheConfiguration<Object, Object> ccfg = new CacheConfiguration<>();

        ccfg.setName("test-cache-" + atomicityMode + "-" + memoryMode);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setMemoryMode(memoryMode);
        ccfg.setWriteSynchronizationMode(writeMode);
        ccfg.setAtomicWriteOrderMode(PRIMARY);

        ccfg.setWriteThrough(true);
        ccfg.setCacheStoreFactory((Factory<? extends CacheStore<? super Object, ? super Object>>)
            FactoryBuilder.factoryOf(TestCacheStore.class));

        return ccfg;
    }

    /**
     *
     */
    public static class TestCacheStore extends CacheStoreAdapter<Integer, Integer> {
        /** {@inheritDoc} */
        @Override public Integer load(Integer key) throws CacheLoaderException {
            throw new UnsupportedOperationException("Unsupported");
        }

        /** {@inheritDoc} */
        @Override public void write(Cache.Entry<? extends Integer, ? extends Integer> entry)
            throws CacheWriterException {
            if (blockedKey.equals(entry.getKey())) {
                try {
                    waitingLatch.countDown();

                    U.await(latch);
                }
                catch (IgniteInterruptedCheckedException e) {
                    throw new CacheWriterException(e);
                }
            }
        }

        /** {@inheritDoc} */
        @Override public void delete(Object key) throws CacheWriterException {
            // No-op.
        }
    }

    /**
     *
     */
    public static class TestValue {
        /** */
        int val;

        /**
         * @param val Value.
         */
        public TestValue(int val) {
            this.val = val;
        }
    }
}
