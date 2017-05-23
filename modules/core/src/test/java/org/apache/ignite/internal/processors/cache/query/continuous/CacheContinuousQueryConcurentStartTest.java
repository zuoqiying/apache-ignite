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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheEntryEventSerializableFilter;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMemoryMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.PA;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteAsyncCallback;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.spi.eventstorage.memory.MemoryEventStorageSpi;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;

import static org.apache.ignite.cache.CacheAtomicWriteOrderMode.PRIMARY;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMemoryMode.OFFHEAP_TIERED;
import static org.apache.ignite.cache.CacheMemoryMode.OFFHEAP_VALUES;
import static org.apache.ignite.cache.CacheMemoryMode.ONHEAP_TIERED;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.PRIMARY_SYNC;

/**
 *
 */
public class CacheContinuousQueryConcurentStartTest extends GridCommonAbstractTest {
    /** */
    public static final int LISTENER_CNT = 3;

    /** */
    public static final int KEYS = 10;

    /** */
    private static TcpDiscoveryIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);

    /** */
    private static final int NODES = 1;

    /** */
    public static final int ITERATION_CNT = 100;

    /** */
    private boolean client;

    /** */
    private static volatile boolean fail;

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

        startGridsMultiThreaded(NODES);

        //client = true;

        //startGrid(NODES - 1);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        fail = false;
    }

    /**
     * @throws Exception If failed.
     */
    public void testAtomicOnheapTwoBackup() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(PARTITIONED, 2, ATOMIC,
            ONHEAP_TIERED, PRIMARY_SYNC);

        doOrderingTest(ccfg, false);
    }

    /**
     * @param ccfg Cache configuration.
     * @param async Async filter.
     * @throws Exception If failed.
     */
    protected void doOrderingTest(
        final CacheConfiguration ccfg,
        final boolean async)
        throws Exception {
        final IgniteCache cache = ignite(0).createCache(ccfg);

        final String cacheName = ccfg.getName();
        int partition = 0;

        Affinity aff = affinity(cache);

        final List<Integer> keys = new ArrayList<>();

        for (int i = 0; i < 1_000_000; i++) {
            if (aff.partition(i) == 0)
                keys.add(i);

            if (keys.size() == 100)
                break;
        }

        for (int nodeNumber = 0; nodeNumber < NODES; nodeNumber++) {
            final int key = keys.get(0);

            final List<Object> evts = new ArrayList<>();

            ContinuousQuery qry = new ContinuousQuery();

            qry.setLocalListener(new CacheEntryUpdatedListener() {
                @Override public void onUpdated(Iterable iterable) throws CacheEntryListenerException {
                    for (Object o : iterable)
                        evts.add(o);
                }
            });

            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger cnt = new AtomicInteger();

            IgniteInternalFuture<Long> f = GridTestUtils.runMultiThreadedAsync(new Runnable() {
                @Override public void run() {
                    while (!stop.get() && !Thread.currentThread().isInterrupted()) {
                        int val = ThreadLocalRandom.current().nextInt(100);

                        cache.put(keys.get(val), val);
                    }
                }
            }, 4, "putter");

            U.sleep(700);

            QueryCursor qryCur = cache.query(qry);

            U.sleep(100);

            stop.set(true);

            f.get();

            U.sleep(5000);

            evts.clear();

            for (int i = 0; i < 10; i++)
                cache.put(key, i);

            GridTestUtils.waitForCondition(new PA() {
                @Override public boolean apply() {
                    return evts.size() == 10;
                }
            }, 5_000);

            assertEquals(10, evts.size());

            qryCur.close();
        }
    }

    /**
     * @param queue Event queue.
     * @throws Exception If failed.
     */
    private void checkEvents(BlockingQueue<CacheEntryEvent<QueryTestKey, QueryTestValue>> queue, int expCnt)
        throws Exception {
        CacheEntryEvent<QueryTestKey, QueryTestValue> evt;
        int cnt = 0;
        Map<QueryTestKey, Integer> vals = new HashMap<>();

        while ((evt = queue.poll(100, TimeUnit.MILLISECONDS)) != null) {
            assertNotNull(evt);
            assertNotNull(evt.getKey());

            Integer preVal = vals.get(evt.getKey());

            if (preVal == null)
                assertEquals(new QueryTestValue(0), evt.getValue());
            else {
                if (!new QueryTestValue(preVal + 1).equals(evt.getValue()))
                    assertEquals("Key event: " + evt.getKey(), new QueryTestValue(preVal + 1), evt.getValue());
            }

            vals.put(evt.getKey(), evt.getValue().val1);

            ++cnt;
        }

        assertEquals(expCnt, cnt);
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return TimeUnit.MINUTES.toMillis(8);
    }

    /**
     *
     */
    @IgniteAsyncCallback
    private static class CacheTestRemoteFilterAsync extends CacheTestRemoteFilter {
        /**
         * @param cacheName Cache name.
         */
        public CacheTestRemoteFilterAsync(String cacheName) {
            super(cacheName);
        }
    }

    /**
     *
     */
    private static class CacheTestRemoteFilter implements
        CacheEntryEventSerializableFilter<QueryTestKey, QueryTestValue> {
        /** */
        private Map<QueryTestKey, QueryTestValue> prevVals = new ConcurrentHashMap<>();

        /** */
        @IgniteInstanceResource
        private Ignite ignite;

        /** */
        private String cacheName;

        /**
         * @param cacheName Cache name.
         */
        public CacheTestRemoteFilter(String cacheName) {
            this.cacheName = cacheName;
        }

        /** {@inheritDoc} */
        @Override public boolean evaluate(CacheEntryEvent<? extends QueryTestKey, ? extends QueryTestValue> e) {
            if (affinity(ignite.cache(cacheName)).isPrimary(ignite.cluster().localNode(), e.getKey())) {
                QueryTestValue prevVal = prevVals.put(e.getKey(), e.getValue());

                if (prevVal != null) {
                    if (!new QueryTestValue(prevVal.val1 + 1).equals(e.getValue()))
                        fail = true;
                }
            }

            return true;
        }
    }

    /**
     *
     */
    @IgniteAsyncCallback
    private static class TestCacheAsyncEventListener extends TestCacheEventListener {
        /**
         * @param queue Queue.
         * @param cntr Received events counter.
         */
        public TestCacheAsyncEventListener(BlockingQueue<CacheEntryEvent<QueryTestKey, QueryTestValue>> queue,
            AtomicInteger cntr) {
            super(queue, cntr);
        }
    }

    /**
     *
     */
    private static class TestCacheEventListener implements CacheEntryUpdatedListener<QueryTestKey, QueryTestValue> {
        /** */
        private final BlockingQueue<CacheEntryEvent<QueryTestKey, QueryTestValue>> queue;

        /** */
        private final AtomicInteger cntr;

        /**
         * @param queue Queue.
         * @param cntr Received events counter.
         */
        public TestCacheEventListener(BlockingQueue<CacheEntryEvent<QueryTestKey, QueryTestValue>> queue,
            AtomicInteger cntr) {
            this.queue = queue;
            this.cntr = cntr;
        }

        /** {@inheritDoc} */
        @Override public void onUpdated(Iterable<CacheEntryEvent<? extends QueryTestKey,
            ? extends QueryTestValue>> evts) {
            for (CacheEntryEvent<? extends QueryTestKey, ? extends QueryTestValue> e : evts) {
                queue.add((CacheEntryEvent<QueryTestKey, QueryTestValue>)e);

                cntr.incrementAndGet();
            }
        }
    }

    /**
     * @param cacheMode Cache mode.
     * @param backups Number of backups.
     * @param atomicityMode Cache atomicity mode.
     * @param memoryMode Cache memory mode.
     * @param writeMode Cache write mode.
     * @return Cache configuration.
     */
    protected CacheConfiguration<Object, Object> cacheConfiguration(
        CacheMode cacheMode,
        int backups,
        CacheAtomicityMode atomicityMode,
        CacheMemoryMode memoryMode,
        CacheWriteSynchronizationMode writeMode) {
        CacheConfiguration<Object, Object> ccfg = new CacheConfiguration<>();

        ccfg.setName("test-cache-" + atomicityMode + "-" + cacheMode + "-" + memoryMode + "-" + memoryMode + "-"
            + backups);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setCacheMode(cacheMode);
        ccfg.setMemoryMode(memoryMode);
        ccfg.setWriteSynchronizationMode(writeMode);
        ccfg.setAtomicWriteOrderMode(PRIMARY);

        if (cacheMode == PARTITIONED)
            ccfg.setBackups(backups);

        return ccfg;
    }

    /**
     *
     */
    public static class QueryTestKey implements Serializable, Comparable {
        /** */
        private final Integer key;

        /**
         * @param key Key.
         */
        public QueryTestKey(Integer key) {
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            QueryTestKey that = (QueryTestKey)o;

            return key.equals(that.key);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return key.hashCode();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(QueryTestKey.class, this);
        }

        /** {@inheritDoc} */
        @Override public int compareTo(Object o) {
            return key - ((QueryTestKey)o).key;
        }
    }

    /**
     *
     */
    public static class QueryTestValue implements Serializable {
        /** */
        @GridToStringInclude
        protected final Integer val1;

        /** */
        @GridToStringInclude
        protected final String val2;

        /**
         * @param val Value.
         */
        public QueryTestValue(Integer val) {
            this.val1 = val;
            this.val2 = String.valueOf(val);
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            QueryTestValue that = (QueryTestValue) o;

            return val1.equals(that.val1) && val2.equals(that.val2);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int res = val1.hashCode();

            res = 31 * res + val2.hashCode();

            return res;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(QueryTestValue.class, this);
        }
    }
}
