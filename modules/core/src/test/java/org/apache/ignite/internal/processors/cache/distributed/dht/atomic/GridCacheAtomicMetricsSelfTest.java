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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import javax.cache.Cache;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.events.EventType.EVT_NODE_METRICS_UPDATED;

public class GridCacheAtomicMetricsSelfTest  extends GridCommonAbstractTest {
    private final static int GRID_CNT = 2;

    private static final String SERVER_NODE = "server";

    private static final String CLIENT_NODE = "client";

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

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    private CacheConfiguration<Integer, Integer> getCacheConfiguration() {
        CacheConfiguration<Integer, Integer> cacheCfg = new CacheConfiguration<>();

        cacheCfg.setCacheMode(PARTITIONED);
        cacheCfg.setAtomicityMode(ATOMIC);
        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);
        cacheCfg.setStatisticsEnabled(true);

        return cacheCfg;
    }

    private void startServerAndClientNodes() throws Exception {
        Ignite server = startGrid(SERVER_NODE);

        Ignition.setClientMode(true);

        Ignite client = startGrid(CLIENT_NODE);
    }

    private void awaitMetricsUpdate(boolean serverOnly) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(((serverOnly? GRID_CNT -1: GRID_CNT) + 1) );

        IgnitePredicate<Event> lsnr = new IgnitePredicate<Event>() {
            @Override public boolean apply(Event ignore) {
                latch.countDown();
                return true;
            }
        };

        if (!serverOnly)
            grid(CLIENT_NODE).events().localListen(lsnr, EVT_NODE_METRICS_UPDATED);

        grid(SERVER_NODE).events().localListen(lsnr, EVT_NODE_METRICS_UPDATED);

        latch.await();
    }

    public void testLocalGet() throws Exception {
        try {
            Ignite server = startGrid(SERVER_NODE);

            IgniteCache<Integer, Integer> cache = server.getOrCreateCache(getCacheConfiguration());

            Integer value = cache.get(new Integer(12));

            awaitMetricsUpdate(true);

            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertTrue(value == null);

            assertEquals(1, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(1, serverMetrics.getCacheMisses());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testDistributedGet() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer value = cache.get(new Integer(12));

            awaitMetricsUpdate(false);

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
            stopAllGrids();
        }
    }

    public void testDistributedGetWithCacheStore() throws Exception {
        try {
            startServerAndClientNodes();

            CacheConfiguration<Integer, Integer> cacheCfg = getCacheConfiguration();
            cacheCfg.setReadThrough(true);
            cacheCfg.setWriteThrough(true);
            cacheCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(TestCacheStore.class));

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(cacheCfg);

            Integer value = cache.get(TestCacheStore.TEST_KEY);

            awaitMetricsUpdate(false);

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
            stopAllGrids();
        }
    }

    public void testLocalGetAll() throws Exception {
        try {
            Ignite server = startGrid(SERVER_NODE);

            IgniteCache<Integer, Integer> cache = server.getOrCreateCache(getCacheConfiguration());

            Integer key1 = new Integer(12);
            Integer key2 = new Integer(13);
            Set<Integer> keys = new HashSet<>();
            keys.add(key1);
            keys.add(key2);

            Map<Integer, Integer> vals = cache.getAll(keys);

            cache.put(key1, key2);

            vals = cache.getAll(keys);

            awaitMetricsUpdate(true);

            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertNotNull(vals);
            assertEquals(1, vals.size());

            assertEquals(4, serverMetrics.getCacheGets());
            assertEquals(1, serverMetrics.getCacheHits());
            assertEquals(3, serverMetrics.getCacheMisses());
            assertEquals(1, serverMetrics.getCachePuts());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testDistributedGetAll() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer key1 = new Integer(12);
            Integer key2 = new Integer(13);
            Set<Integer> keys = new HashSet<>();
            keys.add(key1);
            keys.add(key2);

            Map<Integer, Integer> vals = cache.getAll(keys);

            cache.put(key1, key2);

            vals = cache.getAll(keys);

            awaitMetricsUpdate(false);

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertNotNull(vals);
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
            stopAllGrids();
        }
    }

    public void testLocalPut() throws Exception {
        try {
            Ignite server = startGrid(SERVER_NODE);

            IgniteCache<Integer, Integer> cache = server.getOrCreateCache(getCacheConfiguration());

            cache.put(new Integer(12), new Integer(144));

            awaitMetricsUpdate(true);

            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, serverMetrics.getCachePuts());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testDistributedPut() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            cache.put(new Integer(12), new Integer(144));

            awaitMetricsUpdate(false);

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, clientMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testLocalPutIfAbsent() throws Exception {
        try {
            Ignite server = startGrid(SERVER_NODE);

            IgniteCache<Integer, Integer> cache = server.getOrCreateCache(getCacheConfiguration());

            boolean success = cache.putIfAbsent(new Integer(12), new Integer(144));

            assertTrue(success);

            success = cache.putIfAbsent(new Integer(12), new Integer(145));

            assertFalse(success);

            awaitMetricsUpdate(true);

            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, serverMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCacheGets());
            assertEquals(0, serverMetrics.getCacheHits());
            assertEquals(0, serverMetrics.getCacheMisses());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testDistributedPutIfAbsent() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            boolean success = cache.putIfAbsent(new Integer(12), new Integer(144));

            assertTrue(success);

            success = cache.putIfAbsent(new Integer(12), new Integer(144));

            assertFalse(success);

            awaitMetricsUpdate(false);

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
            stopAllGrids();
        }
    }

    public void testLocalPutAll() throws Exception {
        try {
            Ignite server = startGrid(SERVER_NODE);

            IgniteCache<Integer, Integer> cache = server.getOrCreateCache(getCacheConfiguration());

            Map<Integer, Integer> values = new HashMap<>();

            values.put(new Integer(12), new Integer(144));

            values.put(new Integer(13), new Integer(169));

            cache.putAll(values);

            awaitMetricsUpdate(true);

            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(2, serverMetrics.getCachePuts());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testDistributedPutAll() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Map<Integer, Integer> values = new HashMap<>();

            values.put(new Integer(12), new Integer(144));

            values.put(new Integer(13), new Integer(169));

            cache.putAll(values);

            awaitMetricsUpdate(false);

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(2, clientMetrics.getCachePuts());
            assertEquals(0, serverMetrics.getCachePuts());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testLocalRemove() throws Exception {
        try {
            Ignite server = startGrid(SERVER_NODE);

            IgniteCache<Integer, Integer> cache = server.getOrCreateCache(getCacheConfiguration());

            Integer key = new Integer(12);

            Integer val = new Integer(144);

            cache.remove(key);

            cache.put(key, val);

            cache.remove(key);

            awaitMetricsUpdate(true);

            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, serverMetrics.getCacheRemovals());
            assertEquals(1, serverMetrics.getCachePuts());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testDistributedRemove() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer key = new Integer(12);

            Integer val = new Integer(144);

            cache.remove(key);

            cache.put(key, val);

            cache.remove(key);

            awaitMetricsUpdate(false);

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
            stopAllGrids();
        }
    }

    public void testLocalRemoveOldValue() throws Exception {
        try {
            Ignite server = startGrid(SERVER_NODE);

            IgniteCache<Integer, Integer> cache = server.getOrCreateCache(getCacheConfiguration());

            Integer key = new Integer(12);

            Integer val = new Integer(144);

            cache.remove(key, val);

            cache.put(key, val);

            cache.remove(key, val);

            awaitMetricsUpdate(true);

            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertEquals(1, serverMetrics.getCachePuts());
            assertEquals(1, serverMetrics.getCacheRemovals());
            assertEquals(2, serverMetrics.getCacheGets());
            assertEquals(1, serverMetrics.getCacheHits());
            assertEquals(1, serverMetrics.getCacheMisses());
        }
        finally {
            stopAllGrids();
        }
    }

    public void testDistributedRemoveOldValue() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer key = new Integer(12);

            Integer val = new Integer(144);

            cache.remove(key, val);

            cache.put(key, val);

            cache.remove(key, val);

            awaitMetricsUpdate(false);

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
            stopAllGrids();
        }
    }

    public void testIgnite3495() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            ThreadLocalRandom rand = ThreadLocalRandom.current();

            final int numOfKeys = 5_000;
            for (int i = 0; i < numOfKeys; ++i)
                cache.put(i, rand.nextInt(12_000_000));

            for (int i = 0; i < numOfKeys; ++i)
                cache.get(i);

            awaitMetricsUpdate(false);

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
        stopAllGrids();
    }
    }
}
