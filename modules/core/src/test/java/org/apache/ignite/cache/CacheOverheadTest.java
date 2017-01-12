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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.output.WriterOutputStream;
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
import org.apache.ignite.internal.processors.cache.IgniteCacheProxy;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtCacheAdapter;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtLocalPartition;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtPartitionState;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 * CacheOverheadTest
 *
 * @author Alexandr Kuramshin <ein.nsk.ru@gmail.com>
 */
public class CacheOverheadTest extends GridCommonAbstractTest {
    /** Total size of the page cache */
    private static final long PAGE_CACHE_SIZE = 100L << 20; // Mb

    /** Number of started grids (at the most) */
    private static final int GRID_COUNT = 2;

    /** Number of caches started on every node */
    private static final int CACHE_COUNT = 10;

    /** */
    private static final Pattern MEM_USED_P = Pattern.compile("Память:\\s*([\\d\\xC2\\xA0]+?)\\sКБ");

    /** Bytes in Megabyte */
    private static final double MB = (double)(1L << 20); // Mb

    /** */
    private final PrintStream out = System.out;

    /** */
    private final StringWriter summaryStr = new StringWriter(10 << 10); // K

    /** */
    private final PrintStream summary = new PrintStream(new WriterOutputStream(summaryStr));

    /** Cache count to start */
    private int cacheCnt;

    /** Start grid index */
    private int gridIdx;

    /** Memory consumption by the grids without caches */
    private long[] emptyGridMemoryUsed = new long[GRID_COUNT + 1];

    @Override protected long getTestTimeout() {
        return 3600_000L;
    }

    /** {@inheritDoc} */
    @Override protected boolean isRemoteJvm(String gridName) {
        return getTestGridIndex(gridName) >= GRID_COUNT ;
    }

    /** */
    private static double sizeInMegabytes(double sizeInBytes) {
        return sizeInBytes / MB;
    }

    /** */
    private static long getProcessId() {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        int i = pid.indexOf('@');
        if (i > 0)
            pid = pid.substring(0, i);
        return Long.parseLong(pid);
    }

    /** */
    private void printf(String format, Object... args) {
        out.printf(format, args);
        summary.printf(format, args);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        summary.printf("Summary:%n");
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
        printCacheOverhead(gridIdx);
        return grid;
    }

    /**
     * @param gridCount Current grids count.
     */
    private void printCacheOverhead(int gridCount) throws InterruptedException {
        System.gc();
        Thread.sleep(1000);
        long usedMemory1 = usedMemory();
        int gridCnt = gridCount + 1;
        double cacheOverhead = ((double)(usedMemory1 - emptyGridMemoryUsed[gridCnt])) / cacheCnt / gridCnt;
        printf("Grid count = %d, cache overhead = %01.3f M%n",
            gridCnt, sizeInMegabytes(cacheOverhead));
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
            printf("Caches count = %d%n", cacheCnt);
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

    /** Get cache name by index */
    private String cacheName(String gridName, int cacheIdx) {
        return gridName + "_cache" + cacheIdx;
    }

    /** Get cache by index */
    private <K, V> IgniteCache<K, V> cache(Ignite ignite, int cacheIdx) {
        return ignite.cache(cacheName(ignite.name(), cacheIdx));
    }

    /** */
    private long usedMemory() throws IgniteException {
        long usedMemory = runtimeUsedMemory();
//        long usedMemory = processUsedMemory();
        printf("Used memory = %01.3f M%n", sizeInMegabytes(usedMemory));
        return usedMemory;
    }

    /** Calculating memory usage by runtime heap usage */
    private long runtimeUsedMemory() throws IgniteInterruptedException {
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

    /** Calculating memory usage by system process usage */
    private long processUsedMemory() throws IgniteException {
        try {
            final Runtime runtime = Runtime.getRuntime();
            long pid = getProcessId();
            Process p = runtime.exec(String.format("tasklist /FI \"PID eq %d\" /FO list", pid));
            long usedMemory = 0;
            try (BufferedReader pin = new BufferedReader(new InputStreamReader(p.getInputStream(), "ibm866"))) {
                String s;
                while ((s = pin.readLine()) != null) {
                    Matcher m = MEM_USED_P.matcher(s);
                    if (m.find()) {
                        s = m.group(1);
                        break;
                    }
                }
                if (s == null)
                    throw new IgniteException("Memory used pattern was not match");
                usedMemory = Long.parseLong(s.replaceAll("[\\xC2\\xA0]", "")) << 10;
            }
            return usedMemory;
        }
        catch (IOException ex) {
            throw new IgniteException(ex);
        }
    }

    /** */
    public void testCacheOverhead() throws Exception {
        printf("Empty grid memory usage%n");
        cacheCnt = 0;
        for (int i = 0; i <= GRID_COUNT; ++i) {
            printf("Grid count = %d%n", i);
            emptyGridMemoryUsed[i] = usedMemory();
            if (i < GRID_COUNT)
                startGrid(i);
            else
                stopAllGrids();
        }

        System.gc();

        Thread.sleep(1000);

        System.gc();
        printf("Empty cache memory usage%n");
        cacheCnt = CACHE_COUNT;
        for (int i = 0; i < GRID_COUNT; ++i) {
            startGridWithCaches(i);
            logLocalPartitions(0);
        }

        IgniteEx remoteNode = startGrid(GRID_COUNT);

        summary.flush();
        out.print(summaryStr.getBuffer());

        while(true) {
            Thread.sleep(10_000);
            remoteNode.compute().run(new IgniteRunnable() {
                @Override public void run() {
                    try {
                        printCacheOverhead(0);
                    }
                    catch (InterruptedException e) {
                        // swallow.
                    }
                }
            });
//            printCacheOverhead(GRID_COUNT - 1);
        }

//        stopAllGrids();
    }

    /** Log local partitions counts by type */
    private void logLocalPartitions(int gridIdx) {
        IgniteCacheProxy cache = (IgniteCacheProxy<Object, Object>)cache(ignite(gridIdx), 0);
        GridDhtCacheAdapter delegate = (GridDhtCacheAdapter)cache.delegate();
        List<GridDhtLocalPartition> locParts = delegate.topology().localPartitions();
        Map<GridDhtPartitionState, AtomicLong> stateCounts = new HashMap<GridDhtPartitionState, AtomicLong>() {
            @Override public AtomicLong get(Object key) {
                assert key instanceof GridDhtPartitionState;
                AtomicLong v = super.get(key);
                if (v == null) {
                    v = new AtomicLong(0L);
                    put((GridDhtPartitionState)key, v);
                }
                return v;
            }
        };
        for (GridDhtLocalPartition locPart : locParts) {
            GridDhtPartitionState state = locPart.state();
            AtomicLong cnt = stateCounts.get(state);
            cnt.incrementAndGet();
        }

        for (Map.Entry<GridDhtPartitionState, AtomicLong> entry : stateCounts.entrySet())
            printf("State = %s, count = %d%n", entry.getKey().toString(), entry.getValue().longValue());
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
