/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ignite;

import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMemoryMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteBiInClosure;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Testing for the cache entry estimated size
 *
 * @author Alexandr Kuramshin <ein.nsk.ru@gmail.com>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IgniteCacheEntrySizeTest extends GridCommonAbstractTest implements AutoCloseable {

    /** */
    private static final int IGNITE_NODES_MAX_NUMBER = 10;
    /** */
    private static final String TEST_CACHE_NAME = "cache";
    /** */
    private static final long TEST_EMPTY_ENTRIES_NUMBER = 1_000_000;
    /** */
    private static final long TEST_FULL_ENTRIES_NUMBER = 1_000_000;
    /** */
    private static final int ENTRY_ARRAY_SIZE = 1_000;
    /** */
    private final String lineSeparator = System.getProperty("line.separator");
    /** */
    private final long[] usedMemoryOnStart = new long[IGNITE_NODES_MAX_NUMBER + 1];
    /** */
    private final long[] usedMemoryOnStop = new long[IGNITE_NODES_MAX_NUMBER + 1];
    /** */
    private StringWriter summary = new StringWriter();
    /** */
    private PrintStream out = System.out;
    /** */
    private boolean keepNodesRunningAfterTest;
    /** */
    private boolean startEventOnStartNode = true;
    /** */
    private boolean stopEventOnStopNode = true;
    /** */
    private long estimatedNodeFootprint = 15L << 20;
    /** */
    private long estimatedEmptyEntryFootprint = 300L;
    /** */
    private double estimatedArrayElementFootprint = 1D;

    /** */
    public IgniteCacheEntrySizeTest() {
        summary.append("SUMMARY: ").append(System.getProperty("line.separator"));
    }

    /** */
    private static long sizeInMegabytes(long sizeInBytes) {
        return sizeInBytes >> 20;
    }

    /** */
    private static IgniteConfiguration config(String gridName) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setGridName(gridName);
        cfg.setPeerClassLoadingEnabled(false);

        TcpDiscoverySpi discovery = new TcpDiscoverySpi();
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47510"));
        discovery.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discovery);

        CacheConfiguration cacheCache = new CacheConfiguration();
        cacheCache.setName("cache");
        cacheCache.setCacheMode(CacheMode.PARTITIONED);
        cacheCache.setBackups(0);
        cacheCache.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        //cacheCache.setIndexedTypes(CacheKey.class, CacheValue.class);

        /** ONHEAP_TIERED
        cacheCache.setMemoryMode(CacheMemoryMode.ONHEAP_TIERED);
        cacheCache.setOffHeapMaxMemory(0); */

        /** OFFHEAP_TIERED */
         cacheCache.setMemoryMode(CacheMemoryMode.OFFHEAP_TIERED);
         cacheCache.setOffHeapMaxMemory(512L << 20);

        cfg.setCacheConfiguration(cacheCache);
        return cfg;
    }

    /** */
    public static void main(String[] args) {
        try (final IgniteCacheEntrySizeTest app = new IgniteCacheEntrySizeTest()) {
            //app.test01_nodeFootprint();
            //app.keepNodesRunningAfterTest = true;
            //app.startEventOnStartNode = false;
            app.test02_emptyEntryFootprint();
            app.test03_fullEntryFootprint();
        }
        catch (Exception ex) {
            Logger.getLogger(IgniteCacheEntrySizeTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** */
    private void println(String str) {
        out.println(str);
    }

    /** */
    private void printlnSummary() {
        out.println();
        summary.append(lineSeparator);
    }

    /** */
    private void printlnSummary(String str) {
        out.println(str);
        summary.append(str).append(lineSeparator);
    }

    /** */
    private void printf(String format, Object... args) {
        out.printf(format, args);
    }

    /** */
    private void printfSummary(String format, Object... args) {
        final String str = String.format(format, args);
        out.print(str);
        summary.write(str);
    }

    /** */
    @Override
    public void close() throws Exception {
        if (summary == null)
            return;
        summary.close();
        out.println();
        out.append(summary.getBuffer()).println();
        summary = null;
        out = null;
    }

    /** */
    private long runtimeUsedMemory() throws IgniteInterruptedException {
        try {
            final Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            Thread.sleep(1000);
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            printf("Used memory = %d M%n", sizeInMegabytes(usedMemory));
            return usedMemory;
        }
        catch (InterruptedException ex) {
            throw new IgniteInterruptedException(ex);
        }
    }

    /** */
    public void test01_nodeFootprint() throws Exception {
        printlnSummary();
        printlnSummary("Test: nodeFootprint");

        testFootprint((ignite, cnt) -> {
            final IgniteCountDownLatch latch = ignite.
                countDownLatch("testIgniteFootprintLatch", cnt, true, true);
            ignite.compute().broadcast(() -> {
                latch.countDown();
                latch.await();
            });
            latch.await();
        }, null);

        printfSummary(
            "Mem used: nodes = 0, on start = %d M, on stop = %d M%n",
            sizeInMegabytes(usedMemoryOnStart[0]), sizeInMegabytes(usedMemoryOnStop[0]));
        long totalNodeFootprint = 0;
        for (int cnt = 1; cnt <= IGNITE_NODES_MAX_NUMBER; ++cnt) {
            long onStartPerNode = usedMemoryOnStart[cnt] - usedMemoryOnStart[cnt - 1];
            long onStopPerNode = usedMemoryOnStop[cnt] - usedMemoryOnStop[cnt - 1];
            totalNodeFootprint += onStartPerNode;
            printfSummary("Mem used per node: nodes = %d, on start = %d M, on stop = %d M%n",
                cnt, sizeInMegabytes(onStartPerNode), sizeInMegabytes(onStopPerNode));
        }
        estimatedNodeFootprint = totalNodeFootprint / IGNITE_NODES_MAX_NUMBER;
        printfSummary("Estimated node mem used = %d M%n", sizeInMegabytes(estimatedNodeFootprint));
    }

    /** */
    public void test02_emptyEntryFootprint() throws Exception {
        printlnSummary();
        printlnSummary("Test: emptyEntryFootprint");

        testEntryFootprint(TEST_EMPTY_ENTRIES_NUMBER, 0);
        long totalEmptyEntryFootprint = 0;
        for (int cnt = 1; cnt <= IGNITE_NODES_MAX_NUMBER; ++cnt) {
            final long estimatedNodesFootprint = estimatedNodeFootprint * cnt;
            final long onStart = (usedMemoryOnStart[cnt] - usedMemoryOnStart[0]
                - estimatedNodesFootprint) / TEST_EMPTY_ENTRIES_NUMBER;
            final long onStop = (usedMemoryOnStop[cnt] - usedMemoryOnStop[0]
                - estimatedNodesFootprint) * IGNITE_NODES_MAX_NUMBER
                / TEST_EMPTY_ENTRIES_NUMBER / cnt;
            totalEmptyEntryFootprint += onStart;
            printfSummary("Mem used per entry: nodes = %d, on start = %d, on stop = %d%n",
                cnt, onStart, onStop);
        }
        estimatedEmptyEntryFootprint = totalEmptyEntryFootprint / IGNITE_NODES_MAX_NUMBER;
        printfSummary("Estimated empty entry mem used = %d%n", estimatedEmptyEntryFootprint);
    }

    /** */
    public void test03_fullEntryFootprint() throws Exception {
        printlnSummary();
        printlnSummary("Test: fullEntryFootprint");

        testEntryFootprint(TEST_FULL_ENTRIES_NUMBER, ENTRY_ARRAY_SIZE);
        double totalArrElementFootprint = 0;
        for (int cnt = 1; cnt <= IGNITE_NODES_MAX_NUMBER; ++cnt) {
            final long estimatedNodesFootprint = estimatedNodeFootprint * cnt;
            final long estimatedEntriesFootprint = estimatedEmptyEntryFootprint * TEST_FULL_ENTRIES_NUMBER;
            final double onStart = ((double)(usedMemoryOnStart[cnt] - usedMemoryOnStart[0]
                - estimatedNodesFootprint - estimatedEntriesFootprint))
                / TEST_FULL_ENTRIES_NUMBER / ENTRY_ARRAY_SIZE;
            final double onStop = ((double)(usedMemoryOnStop[cnt] - usedMemoryOnStop[0]
                - estimatedNodesFootprint - estimatedEntriesFootprint))
                * IGNITE_NODES_MAX_NUMBER / cnt
                / TEST_FULL_ENTRIES_NUMBER / ENTRY_ARRAY_SIZE;
            totalArrElementFootprint += onStart;
            printfSummary("Mem used per array element: nodes = %d, on start = %.3f, on stop = %.3f%n",
                cnt, onStart, onStop);
        }
        estimatedArrayElementFootprint = totalArrElementFootprint / IGNITE_NODES_MAX_NUMBER;
        printfSummary("Estimated array element mem used = %.3f%n", estimatedArrayElementFootprint);
    }

    /** */
    private void testEntryFootprint(final long entryNumber, final int arraySize) throws Exception {
        testFootprint((ignite, cnt) -> {
            final IgniteCache<CacheKey, CacheValue> cache = ignite.getOrCreateCache(TEST_CACHE_NAME);
            cache.clear();
            try (IgniteDataStreamer<CacheKey, CacheValue> streamer = ignite.dataStreamer(TEST_CACHE_NAME)) {
                for (long i = 0; i < entryNumber; ++i) {
                    final CacheValue cacheValue = new CacheValue(i, arraySize);
                    for (int j = 0; j < arraySize; ++j)
                        cacheValue.bytes[j] = (byte)j;
                    streamer.addData(new CacheKey(i), cacheValue);
                }
            }
            /*for (long i = 0; i < entryNumber; ++i) {
                final CacheValue cacheValue = new CacheValue(i, arraySize);
                for (int j = 0; j < arraySize; ++j)
                    cacheValue.bytes[j] = (byte)j;
                cache.put(new CacheKey(i), cacheValue);
            }*/
            final IgniteBiTuple<Long, Long> minMax = ignite.compute().broadcast(new IgniteCallable<IgniteBiTuple<Long, Long>>() {

                @IgniteInstanceResource
                private Ignite ignite;

                final long[] count = {0};

                @Override public IgniteBiTuple<Long, Long> call() throws Exception {
                    final IgniteBiTuple<Long, Long> tuple = new IgniteBiTuple<>(Long.MAX_VALUE, Long.MIN_VALUE);
                    cache.localEntries().forEach((e) -> {
                        tuple.set1(Long.min(tuple.get1(), e.getValue().value));
                        tuple.set2(Long.max(tuple.get2(), e.getValue().value));
                        ++count[0];
                    });
                    printf("Local cache values: node=%s, count = %d, min = %d, max = %d%n",
                        ignite.name(), count[0], tuple.get1(), tuple.get2());
                    return tuple;
                }
            }).stream().reduce(new IgniteBiTuple<Long, Long>(Long.MAX_VALUE, Long.MIN_VALUE),
                (result, tuple) -> {
                    result.set1(Long.min(result.get1(), tuple.get1()));
                    result.set2(Long.max(result.get2(), tuple.get2()));
                    return result;
                });
            /*final IgniteBiTuple<Long, Long> minMax = new IgniteBiTuple<>();
            final long[] count = {0};
            try (QueryCursor<List<?>> cursor = cache.query(new SqlFieldsQuery("select count(value), min(value), max(value) from CacheValue"))) {
                final List<?> vals = cursor.iterator().next();
                count[0] = (Long)vals.get(0);
                minMax.set((Long)vals.get(1), (Long)vals.get(2));
            }*/
            cache.rebalance().get();
            printf("Cache size = %d, min = %d, max = %d%n",
                cache.sizeLong(), minMax.get1(), minMax.get2());
        }, (ignite, cnt) -> {
            if (ignite == null)
                return;

            final IgniteCache<Object, Object> cache = ignite.getOrCreateCache(TEST_CACHE_NAME);
            cache.rebalance().get();
            printf("Cache size = %d%n", cache.sizeLong());
        });
    }

    /** */
    private void testFootprint(IgniteBiInClosure<Ignite, Integer> onStart,
        IgniteBiInClosure<Ignite, Integer> onStop) throws Exception {

        final Queue<Ignite> running = new ArrayBlockingQueue<>(IGNITE_NODES_MAX_NUMBER);

        int cnt = 0;
        usedMemoryOnStart[cnt] = runtimeUsedMemory();
        while (cnt < IGNITE_NODES_MAX_NUMBER) {
            ++cnt;
            printf("Starting node = %d%n", cnt);
            final Ignite ignite = Ignition.getOrStart(config("testIgniteFootprint-" + cnt));
            printf("Started node = %d%n", cnt);
            running.add(ignite);
            if (onStart != null && (startEventOnStartNode || cnt == IGNITE_NODES_MAX_NUMBER))
                onStart.apply(ignite, cnt);
            usedMemoryOnStart[cnt] = runtimeUsedMemory();
        }

        usedMemoryOnStop[cnt] = usedMemoryOnStart[cnt];
        if (keepNodesRunningAfterTest) {
            while (cnt > 0) {
                --cnt;
                usedMemoryOnStop[cnt] = usedMemoryOnStart[cnt];
            }
        }
        else
            while (cnt > 0) {
                printf("Stopping node = %d%n", cnt);
                Ignite ignite = running.poll();
                ignite.close();
                printf("Stopped node = %d%n", cnt);
                --cnt;
                if (onStop != null && (stopEventOnStopNode || cnt == 0))
                    onStop.apply(running.peek(), cnt);
                usedMemoryOnStop[cnt] = runtimeUsedMemory();
            }
    }

    /** */
    private static class CacheKey {

        /** */
        //@QuerySqlField(index = true)
        public final long value;

        /** */
        CacheKey(long value) {
            this.value = value;
        }

        /** */
        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof CacheKey
                && ((CacheKey)obj).value == value;
        }

        /** */
        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }
    }

    /** */
    private static class CacheValue {

        /** */
        public final long value;
        /** */
        public final byte[] bytes;

        /** */
        CacheValue(long value, int arraySize) {
            this.value = value;
            this.bytes = new byte[arraySize];
        }
    }
}
