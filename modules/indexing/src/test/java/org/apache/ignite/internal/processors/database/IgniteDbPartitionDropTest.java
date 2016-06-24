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

package org.apache.ignite.internal.processors.database;

import java.util.Random;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DatabaseConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.IgniteCacheOffheapManager;
import org.apache.ignite.internal.processors.cache.database.tree.BPlusTree;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtLocalPartition;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jsr166.ThreadLocalRandom8;

/**
 * Partitions drop test.
 */
public class IgniteDbPartitionDropTest extends GridCommonAbstractTest {
    /** */
    private static final TcpDiscoveryIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);

    /** */
    public static final int MAX_PARTITIONS = 10;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        DatabaseConfiguration dbCfg = new DatabaseConfiguration();

        dbCfg.setConcurrencyLevel(Runtime.getRuntime().availableProcessors() * 4);

        dbCfg.setPageSize(1024);

        dbCfg.setPageCacheSize(200 * 1024 * 1024);

        cfg.setDatabaseConfiguration(dbCfg);

        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setIndexedTypes(Integer.class, String.class);
        ccfg.setAffinity(new RendezvousAffinityFunction(false, MAX_PARTITIONS));
        ccfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        ccfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
        ccfg.setRebalanceMode(CacheRebalanceMode.SYNC);

        cfg.setCacheConfiguration(ccfg);

        TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);
        cfg.setMarshaller(null);

        return cfg;
    }

    /**
     * Tests partition destruction.
     */
    public void testPartitionDestroy() throws Exception {
        IgniteEx grid = startGrid(0);

        IgniteCache<Integer, String> cache = grid.cache(null);

        int total = 10_000;

        for(int i = 0; i < total; i++)
            cache.put(i, String.valueOf(i));

        assertEquals(total, cache.size());

        int partToDestroy = ThreadLocalRandom8.current().nextInt(MAX_PARTITIONS);

        GridCacheContext<Object, Object> ctx = grid.context().cache().cache().context();

        IgniteCacheOffheapManager offheapMgr = ctx.offheap();

        GridDhtLocalPartition part = ctx.topology().localPartition(partToDestroy, AffinityTopologyVersion.NONE, false);

        offheapMgr.destroy(part);

        // Validate if the data was actually deleted.
        for(int i = 0; i < total; i++) {
            int keyPart = grid.affinity(null).partition(i);

            if (partToDestroy == keyPart) {
                assertFalse("Deleted partition data not empty", cache.containsKey(i));

                assertNull("Deleted partition cache entry not null", cache.get(i));
            } else {
                assertTrue("Other partition data is empty", cache.containsKey(i));

                assertNotNull("Other partition cache entry is null", cache.get(i));
            }
        }
    }
}
