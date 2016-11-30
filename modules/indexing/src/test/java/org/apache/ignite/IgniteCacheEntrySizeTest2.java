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
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Testing for the cache entry estimated size
 *
 * @author Alexandr Kuramshin <ein.nsk.ru@gmail.com>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IgniteCacheEntrySizeTest2 extends GridCommonAbstractTest implements AutoCloseable {

    /** */
    private static final String TEST_CACHE_NAME = "cache";
    /** */
    private static final long TEST_EMPTY_ENTRIES_NUMBER = 1_000_000;
    /** */
    private static final long TEST_FULL_ENTRIES_NUMBER = 1_000;
    /** */
    private static final int ENTRY_ARRAY_SIZE = 100_000;
    /** */
    private static final long MEMORY_LIMIT = 8L << 30;
    /** */
    private static final long CACHE_WRITE_TIMEOUT = 5 * 60_000;
    /** */
    private static final Pattern MEM_USED_P = Pattern.compile("Память:\\s*([\\d\\h]+?)\\sКБ");
    /** */
    private final String lineSeparator = System.getProperty("line.separator");
    /** */
    private StringWriter summary = new StringWriter();
    /** */
    private PrintStream out = System.out;
    /** */
    private long estimatedJvmFootprint = 50L << 20;
    /** */
    private long estimatedNodeFootprint = 100L << 20; // 50 Mb on big number of nodes
    /** */
    private long estimatedEmptyEntryFootprint = 230L;
    /** */
    private double estimatedArrayElementFootprint = 1.1D;

    /** */
    public IgniteCacheEntrySizeTest2() {
        summary.append("SUMMARY: ").append(System.getProperty("line.separator"));
    }

    /** */
    private static long sizeInMegabytes(long sizeInBytes) {
        return sizeInBytes >> 20;
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
    private static IgniteConfiguration config(String gridName) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setGridName(gridName);
        cfg.setPeerClassLoadingEnabled(false);

        TcpDiscoverySpi discovery = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47510"));
        discovery.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discovery);

        CacheConfiguration cacheCache = new CacheConfiguration();
        cacheCache.setName("cache");
        cacheCache.setCacheMode(CacheMode.PARTITIONED);
        cacheCache.setBackups(0);
        cacheCache.setAtomicityMode(CacheAtomicityMode.ATOMIC);
//        cacheCache.setIndexedTypes(CacheKey.class, CacheValue.class);

        cacheCache.setMemoryMode(CacheMemoryMode.OFFHEAP_TIERED);
        cacheCache.setOffHeapMaxMemory(MEMORY_LIMIT);

        cfg.setCacheConfiguration(cacheCache);
        return cfg;
    }

    /** */
    public static void main(String[] args) {
        try (final IgniteCacheEntrySizeTest2 app = new IgniteCacheEntrySizeTest2()) {
//            app.test01_nodeFootprint();
//            app.test02_emptyEntryFootprint();
            app.test03_fullEntryFootprint();
        }
        catch (Exception ex) {
            Logger.getLogger(IgniteCacheEntrySizeTest2.class.getName()).log(Level.SEVERE, null, ex);
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
    private long processUsedMemory() throws IgniteException {
        try {
            final Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            Thread.sleep(2000);
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
                usedMemory = Long.parseLong(s.replaceAll("\\h", "")) << 10;
            }
            printf("Used memory = %d M%n", sizeInMegabytes(usedMemory));
            return usedMemory;
        }
        catch (IOException ex) {
            throw new IgniteException(ex);
        }
        catch (InterruptedException ex) {
            throw new IgniteInterruptedException(ex);
        }
    }

    /** */
    public void test01_nodeFootprint() throws Exception {
        printlnSummary();
        printlnSummary("Test: nodeFootprint");

        long usedMemoryOnStart = processUsedMemory(), estimatedNodeFootprint = Long.MAX_VALUE;
        printfSummary("Estimated JVM mem used = %d M%n", sizeInMegabytes(usedMemoryOnStart));
        try {
            int nodes = 0;
            while (true) {
                Ignition.getOrStart(config("testIgniteFootprint-" + nodes));
                ++nodes;
                long f = (processUsedMemory() - usedMemoryOnStart) / nodes;
                if (estimatedNodeFootprint > f)
                    estimatedNodeFootprint = f;
                printfSummary("Estimated node mem used = %d M, minimum = %d M%n",
                    sizeInMegabytes(f), sizeInMegabytes(estimatedNodeFootprint));
            }
        }
        catch (OutOfMemoryError ex) {
            long usedMemoryOnStop = processUsedMemory();
            printfSummary("Maximum JVM mem used = %d M%n", sizeInMegabytes(usedMemoryOnStop));
            printfSummary("Estimated JVM mem used = %d M%n", sizeInMegabytes(usedMemoryOnStart));
            printfSummary("Estimated node mem used = %d M%n", sizeInMegabytes(estimatedNodeFootprint));
            System.exit(1);
        }
    }

    /** */
    public void test02_emptyEntryFootprint() throws Exception {
        printlnSummary();
        printlnSummary("Test: emptyEntryFootprint");

        testEntryFootprint(TEST_EMPTY_ENTRIES_NUMBER, 0, new IgniteInClosure<Long>() {
            long estimatedEmptyEntryFootprint = Long.MAX_VALUE;

            @Override public void apply(Long f) {
                if (estimatedEmptyEntryFootprint > f)
                    estimatedEmptyEntryFootprint = f;
                printfSummary("Estimated empty entry mem used = %d, minimum = %d%n",
                    f, estimatedEmptyEntryFootprint);
            }
        });
    }

    /** */
    public void test03_fullEntryFootprint() throws Exception {
        printlnSummary();
        printlnSummary("Test: fullEntryFootprint");

        testEntryFootprint(TEST_FULL_ENTRIES_NUMBER, ENTRY_ARRAY_SIZE, new IgniteInClosure<Long>() {
            double estimatedArrayElementFootprint = Double.MAX_VALUE;

            @Override public void apply(Long f) {
                double d = ((double)(f - estimatedEmptyEntryFootprint)) / ENTRY_ARRAY_SIZE;
                if (estimatedArrayElementFootprint > d)
                    estimatedArrayElementFootprint = d;
                printfSummary("Estimated array element mem used = %.3f, minimum = %.3f%n",
                    d, estimatedArrayElementFootprint);
            }
        });
    }

    /** */
    private void testEntryFootprint(long entriesNumber, int arraySize,
        IgniteInClosure<Long> callback) throws Exception {
        long count = 0;
        final Thread th = Thread.currentThread();
        Ignite ignite = Ignition.getOrStart(config("testEntryFootprint"));
        IgniteCache<CacheKey, CacheValue> cache = ignite.getOrCreateCache(TEST_CACHE_NAME);
        CacheKey cacheKey = new CacheKey();
        CacheValue cacheValue = new CacheValue();
        cacheValue.bytes = new byte[arraySize];
        for (int j = 0; j < arraySize; ++j)
            cacheValue.bytes[j] = (byte)j;
        final AtomicBoolean writeTimeout = new AtomicBoolean();
        try {
            while (true) {
                Timer writeTimeoutTimer = new Timer("writeTimeoutTimer", true);
                writeTimeoutTimer.schedule(new TimerTask() {
                    @Override public void run() {
                        synchronized (IgniteCacheEntrySizeTest2.this) {
                            writeTimeout.set(true);
                            th.interrupt();
                        }
                    }
                }, CACHE_WRITE_TIMEOUT);
                for (long i = 0; i < entriesNumber; ++i, ++count) {
                    if (writeTimeout.get()) {
                        updateUsedMemory(count, callback);
                        throw new RuntimeException("Timeout writing to the cache");
                    }
                    cacheKey.value = count;
                    cacheValue.value = count;
//                    cacheValue.value0 = count;
//                    cacheValue.value1 = String.valueOf(count);
                    cache.put(cacheKey, cacheValue);
                }
                writeTimeoutTimer.cancel();
                updateUsedMemory(count, callback);
            }
        }
        catch (Throwable ex) {
            synchronized (this) {
                Thread.interrupted();
                System.err.println(ex);
                long cacheSize = cache.sizeLong();
                printfSummary("Entry count = %d, cache size = %d%n", count, cacheSize);
                try {
                    updateUsedMemory(count, callback);
                }
                catch (Throwable ignore) {
                }
            }
            while (true)
                Thread.sleep(1000);
        }

    }

    private void updateUsedMemory(long count, IgniteInClosure<Long> callback) {
        long usedMemory = processUsedMemory();
        long f = (usedMemory - estimatedNodeFootprint - estimatedJvmFootprint) / count;
        callback.apply(f);
        if (usedMemory > MEMORY_LIMIT)
            throw new RuntimeException("Memory limit was exceeded");
    }

    /** */
    private static class CacheKey {

        /** */
        public long value;

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
//        @QuerySqlField(index = true)
        public long value;
//        /** */
//        @QuerySqlField(index = true)
//        public long value0;
//        /** */
//        @QuerySqlField(index = true)
//        public String value1;
        /** */
        public byte[] bytes;
    }
}
