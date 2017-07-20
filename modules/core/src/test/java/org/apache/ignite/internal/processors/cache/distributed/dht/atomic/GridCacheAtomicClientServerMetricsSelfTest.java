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

package org.apache.ignite.internal.processors.cache.distributed.dht.atomic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import javax.cache.Cache;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

import static org.apache.ignite.cache.CacheAtomicWriteOrderMode.PRIMARY;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_ASYNC;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.events.EventType.EVT_NODE_METRICS_UPDATED;

public class GridCacheAtomicClientServerMetricsSelfTest extends GridCommonAbstractTest {
    private final static int GRID_CNT = 2;

    private static final int SERVER_NODE = 0;

    private static final int CLIENT_NODE = 1;

    private static TcpDiscoveryIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);

    public static class TestCacheStore extends CacheStoreAdapter<Integer, Integer> {
        public static final Integer TEST_KEY = new Integer(12);

        public static final Integer TEST_VAL = new Integer(144);

        public TestCacheStore() {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public Integer load(Integer key) throws CacheLoaderException {
            if (TEST_KEY.equals(key))
                return TEST_VAL;

            return null;
        }

        /** {@inheritDoc} */
        @Override public void write(Cache.Entry<? extends Integer, ? extends Integer> entry)
            throws CacheWriterException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void delete(Object key) throws CacheWriterException {
            // No-op.
        }
    }

    public static class ExpireOnCreationPolicy implements ExpiryPolicy {
        /** {@inheritDoc} */
        @Override public Duration getExpiryForCreation() {
            return Duration.ZERO;
        }

        /** {@inheritDoc} */
        @Override public Duration getExpiryForAccess() {
            return Duration.ZERO;
        }

        /** {@inheritDoc} */
        @Override public Duration getExpiryForUpdate() {
            return Duration.ZERO;
        }
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    private int gridCount() {
        return GRID_CNT;
    }

    /** {@inheritDoc} */
    protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrid(SERVER_NODE);

        Ignition.setClientMode(true);

        startGrid(CLIENT_NODE);
    }

    /** {@inheritDoc} */
    protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();

        stopAllGrids();
    }

    private CacheConfiguration<Integer, Integer> getCacheConfiguration() {
        CacheConfiguration<Integer, Integer> cacheCfg = new CacheConfiguration<>();

        cacheCfg.setCacheMode(PARTITIONED);
        cacheCfg.setAtomicityMode(ATOMIC);
        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);
        cacheCfg.setStatisticsEnabled(true);
        cacheCfg.setName("metrics");

        return cacheCfg;
    }

    private void awaitMetricsUpdate() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(GRID_CNT * 2);

        IgnitePredicate<Event> lsnr = new IgnitePredicate<Event>() {
            @Override public boolean apply(Event ignore) {
                latch.countDown();
                return true;
            }
        };

        for (int i = 0; i < gridCount(); ++i)
            grid(i).events().localListen(lsnr, EVT_NODE_METRICS_UPDATED);

        latch.await();
    }

    public void testGet() throws Exception {
        IgniteCache<Integer, Integer> cache = null;
        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer value = cache.get(12);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertTrue(value == null);

            assertEquals(1, clientMetrics.getCacheGets());
            assertEquals(0, clientMetrics.getCacheHits());
            assertEquals(1, clientMetrics.getCacheMisses());

            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testGetWithCacheStore() throws Exception {
        IgniteCache<Integer, Integer> cache = null;
        try {
            CacheConfiguration<Integer, Integer> cacheCfg = getCacheConfiguration();
            cacheCfg.setReadThrough(true);
            cacheCfg.setWriteThrough(true);
            cacheCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(TestCacheStore.class));

            cache = grid(CLIENT_NODE).getOrCreateCache(cacheCfg);

            Integer value = cache.get(TestCacheStore.TEST_KEY);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertTrue(value != null);

            assertEquals(1, clientMetrics.getCacheGets());
            assertEquals(0, clientMetrics.getCacheHits());
            assertEquals(1, clientMetrics.getCacheMisses());

            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testGetAll() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Set<Integer> keys = new HashSet<>();
            keys.add(12);
            keys.add(13);

            Map<Integer, Integer> vals = cache.getAll(keys);

            assertTrue(vals.isEmpty());

            cache.put(12, 13);

            vals = cache.getAll(keys);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertTrue(!vals.isEmpty());
            assertEquals(1, vals.size());

            assertEquals(4, clientMetrics.getCacheGets());
            assertEquals(1, clientMetrics.getCacheHits());
            assertEquals(3, clientMetrics.getCacheMisses());
            assertEquals(1, clientMetrics.getCachePuts());

            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testPut() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            for (int i = 0; i < 500; ++i)
                cache.put(i, i);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(500, clientMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public static class TestEntryProcessor implements EntryProcessor<Integer, Integer, Boolean> {
        @Override public Boolean process(MutableEntry<Integer, Integer> entry, Object... arguments)
            throws EntryProcessorException {
            entry.remove();

            return Boolean.TRUE;
        }
    }

    public void testEntryProcessor() throws Exception {
        IgniteCache<Integer, Integer> cache = null;
        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            cache.put(15, 12);
            cache.put(17, 289);
            HashSet<Integer> keys = new HashSet<>(Arrays.asList(15, 17));
            Map<Integer, EntryProcessorResult<Boolean>> result = cache.invokeAll(keys, new TestEntryProcessor());

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(2, clientMetrics.getCachePuts());
            assertEquals(2, clientMetrics.getCacheGets());
            assertEquals(2, clientMetrics.getCacheHits());
            assertEquals(0, clientMetrics.getCacheMisses());
            assertEquals(2, clientMetrics.getCacheRemovals());

            assertEquals(0, serverMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
            assertEquals(0, serverMetrics.getCacheRemovals());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testPutIfAbsent() throws Exception {
        IgniteCache<Integer, Integer> cache = null;
        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            boolean success = cache.putIfAbsent(12, 144);

            assertTrue(success);

            success = cache.putIfAbsent(12, 144);

            assertFalse(success);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, clientMetrics.getCachePuts());
            assertEquals(0, clientMetrics.getCacheGets());
            assertEquals(0, clientMetrics.getCacheHits());
            assertEquals(0, clientMetrics.getCacheMisses());

            assertEquals(0, serverMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testPutAll() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Map<Integer, Integer> values = new HashMap<>();

            values.put(12, 144);

            values.put(13, 169);

            cache.putAll(values);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(2, clientMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testRemove() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            cache.remove(12);

            cache.put(12, 144);

            cache.remove(12);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, clientMetrics.getCacheRemovals());
            assertEquals(1, clientMetrics.getCachePuts());

            assertEquals(0, serverMetrics.getCacheRemovals());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testConditionalRemove() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            cache.remove(12, 144);

            cache.put(12, 144);

            cache.remove(12, 144);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, clientMetrics.getCachePuts());
            assertEquals(1, clientMetrics.getCacheRemovals());
            assertEquals(2, clientMetrics.getCacheGets());
            assertEquals(1, clientMetrics.getCacheHits());
            assertEquals(1, clientMetrics.getCacheMisses());

            assertEquals(0, serverMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCacheRemovals());
            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testDistributedGetAndRemove() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer retval = cache.getAndRemove(12);

            assertNull(retval);

            cache.put(12, 144);

            retval = cache.getAndRemove(12);

            assertNotNull(retval);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, clientMetrics.getCacheRemovals());
            assertEquals(2, clientMetrics.getCacheGets());
            assertEquals(1, clientMetrics.getCacheHits());
            assertEquals(1, clientMetrics.getCacheMisses());
            assertEquals(1, clientMetrics.getCachePuts());

            assertEquals(0, serverMetrics.getCacheRemovals());
            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testDistributedRemoveSet() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Set<Integer> keys = new HashSet<>();
            keys.add(12);
            keys.add(13);

            cache.removeAll(keys);

            cache.put(12, 144);

            cache.removeAll(keys);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientsMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, clientsMetrics.getCacheRemovals());
            assertEquals(1, clientsMetrics.getCachePuts());

            assertEquals(0, serverMetrics.getCacheRemovals());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testDistributedGetAndReplace() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer retval = cache.getAndReplace(12, 144);

            assertNull(retval);

            cache.put(12, 144);

            retval = cache.getAndReplace(12, new Integer(169));

            assertNotNull(retval);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(2, clientMetrics.getCacheGets());
            assertEquals(1, clientMetrics.getCacheHits());
            assertEquals(1, clientMetrics.getCacheMisses());
            assertEquals(2, clientMetrics.getCachePuts());

            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testDistributedReplace() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            boolean success = cache.replace(12, 144);

            assertFalse(success);

            cache.put(12, 144);

            success = cache.replace(12, 169);

            assertTrue(success);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(2, clientMetrics.getCacheGets());
            assertEquals(1, clientMetrics.getCacheHits());
            assertEquals(1, clientMetrics.getCacheMisses());
            assertEquals(2, clientMetrics.getCachePuts());

            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testDistributedConditionalReplace() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer key = new Integer(12);

            Integer val = new Integer(144);

            Integer newVal = new Integer(169);

            int missCount = 0;
            int hitCount = 0;
            int putCount = 0;

            boolean success = cache.replace(12, 144, 169);
            missCount++;

            assertFalse(success);

            cache.put(12, 144);
            putCount++;

            success = cache.replace(12, 144, 169);
            hitCount++;
            putCount++;

            assertTrue(success);

            success = cache.replace(12, 144, 169);
            hitCount++;

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            //assertEquals(2, clientMetrics.getCacheGets());
            assertEquals(hitCount, clientMetrics.getCacheHits());
            assertEquals(missCount, clientMetrics.getCacheMisses());
            assertEquals(putCount, clientMetrics.getCachePuts());

            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testExpirationPolicy() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            CacheConfiguration<Integer, Integer> cfg = getCacheConfiguration();
            cfg.setExpiryPolicyFactory(FactoryBuilder.factoryOf(ExpireOnCreationPolicy.class));

            cache = grid(CLIENT_NODE).getOrCreateCache(cfg);

            HashMap<Integer, Integer> vals = new HashMap<>();
            vals.put(12, 144);
            vals.put(13, 169);

            cache.putAll(vals);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(0, clientMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }

    public void testIgnite3495() throws Exception {
        IgniteCache<Integer, Integer> cache = null;

        try {
            cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            ThreadLocalRandom rand = ThreadLocalRandom.current();

            final int numOfKeys = 5_000;
            for (int i = 0; i < numOfKeys; ++i)
                cache.put(i, rand.nextInt(12_000_000));

            for (int i = 0; i < numOfKeys; ++i)
                cache.get(i);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(numOfKeys, clientMetrics.getCachePuts());
            assertEquals(numOfKeys, clientMetrics.getCacheGets());
            assertEquals(numOfKeys, clientMetrics.getCacheHits());
            assertEquals(0, clientMetrics.getCacheMisses());
            assertTrue(clientMetrics.getAveragePutTime() > 0.0);
            assertTrue(clientMetrics.getAverageGetTime() > 0.0);

            assertEquals(0, serverMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
        }
        finally {
            if (cache != null)
                cache.destroy();
        }
    }
}
