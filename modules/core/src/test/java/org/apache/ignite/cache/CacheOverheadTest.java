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
package org.apache.ignite.cache;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteInterruptedException;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteNodeAttributes;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.testframework.junits.multijvm.IgniteProcessProxy;

/**
 * CacheOverheadTest
 */
public class CacheOverheadTest extends GridCommonAbstractTest {
    /** Total size of the page cache */
    private static final long PAGE_CACHE_SIZE = 100L << 20; // Mb

    /** Number of started grids (at the most) */
    private static final int GRID_COUNT = 10;

    /** Number of caches started on every node */
    private static final int CACHE_COUNT = 10;


    /** Bytes in Megabyte */
    private static final double MB = (double)(1L << 20); // Mb

    /** */
    private final PrintStream out = System.out;


    /** Cache count to start */
    private int cacheCnt;

    /** Cache count to start */
    private int remoteNodeIdx;

    /** Start grid index */
    private int gridIdx;

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 3600_000L;
    }

    /** {@inheritDoc} */
    @Override protected boolean isRemoteJvm(String gridName) {
        return getTestGridIndex(gridName) == remoteNodeIdx;
    }

    /** */
    private static double sizeInMegabytes(double sizeInBytes) {
        return sizeInBytes / MB;
    }

    /** */
    private void printf(String format, Object... args) {
        out.printf(format, args);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();
    }

    /** {@inheritDoc} */
    @Override protected IgniteEx startGrid(int gridIdx) throws Exception {
        this.gridIdx = gridIdx;
        return super.startGrid(gridIdx);
    }

    /** */
    private IgniteEx startGridWithCaches(int gridIdx) throws Exception {
        printf("Starting grid index = %d%n", gridIdx);
        IgniteEx grid = startGrid(gridIdx);
        awaitPartitionMapExchange();
        return grid;
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setLateAffinityAssignment(false);

        // MemoryConfiguration
        MemoryConfiguration mcfg = new MemoryConfiguration();

        mcfg.setPageCacheSize(PAGE_CACHE_SIZE);
        cfg.setMemoryConfiguration(mcfg);

        // Start caches only on the first grid
        if (gridIdx == 0) {
            // CacheConfiguration
            CacheConfiguration[] ccfg = new CacheConfiguration[cacheCnt];

            for (int i = 0; i < cacheCnt; ++i) {
                CacheConfiguration c = new CacheConfiguration();

                c.setName(cacheName(gridName, i));
                c.setBackups(3);
//                c.setNodeFilter(new CacheNodeFilter(gridName));

                RendezvousAffinityFunction aff = new RendezvousAffinityFunction();
                aff.setPartitions(1024);

                c.setAffinity(aff);
                c.setAtomicityMode(CacheAtomicityMode.ATOMIC);

                ccfg[i] = c;
            }
            cfg.setCacheConfiguration(ccfg);

            // ConnectorConfiguration for Visor
            ConnectorConfiguration con = new ConnectorConfiguration();

            cfg.setConnectorConfiguration(con);
        }

        return cfg;
    }

    /** Generate cache name by index
     *
     * @param gridName Grid name.
     * @param cacheIdx Cache index.
     * @return Cache name.
     */
    private String cacheName(String gridName, int cacheIdx) {
        return gridName + "_cache" + cacheIdx;
    }

    /**
     * Get cache by index
     *
     * @param ignite Ignite instance.
     * @param cacheIdx Cache index.
     * @return Ignite cache.
     */
    private <K, V> IgniteCache<K, V> cache(Ignite ignite, int cacheIdx) {
        return ignite.cache(cacheName(ignite.name(), cacheIdx));
    }

    /** Calculating memory usage by runtime heap usage
     *
     * @return Memory in bytest is used by JVM.
     * @throws IgniteInterruptedException On error.
     */
    private long usedMemory() throws IgniteInterruptedException {
        try {
            final Runtime runtime = Runtime.getRuntime();

            runtime.gc();

            Thread.sleep(100);

            return runtime.totalMemory() - runtime.freeMemory();
        }
        catch (InterruptedException ex) {
            throw new IgniteInterruptedException(ex);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testCacheOverhead() throws Exception {
        List<Double> overheads = new ArrayList<>();

        for (int i = 2; i < GRID_COUNT; ++i)
            overheads.add(cacheOverhead(i));

        for (int i = 2; i < GRID_COUNT; ++i)
            printf("Nodes: %d, overhead = %01.3f M\n", i + 1, sizeInMegabytes(overheads.get(i - 2)));
    }



    /**
     * @param gridCount Count nodes in the grid.
     *
     * @return Overhead for one cache.
     * @throws Exception On error.
     */
    private double cacheOverhead(int gridCount) throws Exception {
        cacheCnt = 0;

        remoteNodeIdx = gridCount;

        for (int i = 0; i < gridCount; ++i)
            startGrid(i);

        IgniteProcessProxy remoteNode = (IgniteProcessProxy)startGrid(gridCount);

        Thread.sleep(10_000);

        long memWithoutCaches = remoteMemUsage(remoteNode);

        stopAllGrids();

        cacheCnt = CACHE_COUNT;

        for (int i = 0; i < gridCount; ++i)
            startGridWithCaches(i);

        awaitPartitionMapExchange();

        remoteNode = (IgniteProcessProxy)startGrid(gridCount);

        Thread.sleep(10_000);

        long memWithCaches = remoteMemUsage(remoteNode);

        double overhead = (double)(memWithCaches - memWithoutCaches) / cacheCnt;

        printf("+++ Nodes: %d, overhead = %01.3f M\n", gridCount, sizeInMegabytes(overhead));

        stopAllGrids();

        Thread.sleep(500);

        return overhead;
    }

    /**
     * @param remoteNode Remote (separate JVM) Ignite node.
     * @return Momory usage.
     */
    private long remoteMemUsage(IgniteProcessProxy remoteNode) {
        return remoteNode.remoteCompute().call(new IgniteCallable<Long>() {
                @Override public Long call() throws Exception {
                    Thread.sleep(10_000);

                    System.gc();

                    Thread.sleep(1000);

                    return usedMemory();
                }
            });
    }

    /** Starts the cache on the specific grid node (filter by name) */
    private static class CacheNodeFilter implements IgnitePredicate<ClusterNode> {

        /** */
        private final String gridName;

        /** */
        public CacheNodeFilter(String gridName) {
            this.gridName = gridName;
        }

        /** {@inheritDoc} */
        @Override public boolean apply(ClusterNode node) {
            String nodeGridName = node.attribute(IgniteNodeAttributes.ATTR_GRID_NAME);

            return nodeGridName.equals(gridName);
        }
    }
}
