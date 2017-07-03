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

package org.apache.ignite.internal.processors.cache;

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

import javax.cache.Cache;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.events.EventType.EVT_NODE_METRICS_UPDATED;

public class IgniteCacheAtomicMetricsTest extends GridCommonAbstractTest {
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

    /**
     * Wait for {@link EventType#EVT_NODE_METRICS_UPDATED} event will be receieved.
     */
    private void awaitMetricsUpdate() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch((GRID_CNT + 1) * 2);

        IgnitePredicate<Event> lsnr = new IgnitePredicate<Event>() {
            @Override public boolean apply(Event ignore) {
                latch.countDown();

                return true;
            }
        };

        grid(CLIENT_NODE).events().localListen(lsnr, EVT_NODE_METRICS_UPDATED);

        latch.await();
    }

    public void testGetCacheGets() throws Exception {
        try {
            startServerAndClientNodes();

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(getCacheConfiguration());

            Integer key = new Integer(12);

            Integer value = cache.get(key);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertTrue(value == null);

            assertTrue(clientMetrics.getCacheGets() == 1);
            assertTrue(clientMetrics.getCacheMisses() == 1);

            assertTrue(serverMetrics.getCacheGets() == 0);
            assertTrue(serverMetrics.getCacheMisses() == 0);
        }
        finally {
            stopAllGrids();
        }
    }

    public void testGetCacheGetsWithCacheStoreEnabled() throws Exception {
        try {
            startServerAndClientNodes();

            CacheConfiguration<Integer, Integer> cacheCfg = getCacheConfiguration();
            cacheCfg.setReadThrough(true);
            cacheCfg.setWriteThrough(true);
            cacheCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(TestCacheStore.class));

            IgniteCache<Integer, Integer> cache = grid(CLIENT_NODE).getOrCreateCache(cacheCfg);

            Integer value = cache.get(TestCacheStore.TEST_KEY);

            awaitMetricsUpdate();

            ClusterGroup clientGroup = grid(CLIENT_NODE).cluster().forClients();
            ClusterGroup serverGroup = grid(SERVER_NODE).cluster().forServers();

            CacheMetrics clientMetrics = cache.metrics(clientGroup);
            CacheMetrics serverMetrics = cache.metrics(serverGroup);

            assertTrue(TestCacheStore.TEST_VAL.equals(value));

            assertTrue(clientMetrics.getCacheGets() == 1);
            assertTrue(clientMetrics.getCacheMisses() == 1);

            assertTrue(serverMetrics.getCacheGets() == 0);
            assertTrue(serverMetrics.getCacheMisses() == 0);
        }
        finally {
            stopAllGrids();
        }
    }
}
