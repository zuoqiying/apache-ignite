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
package org.apache.ignite.internal.pagemem.impl;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMemoryMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TooMuchOffheapEntries
 *
 * @author Alexandr Kuramshin <ein.nsk.ru@gmail.com>
 */
public class TooMuchOffheapEntries {

    /** Grid name */
    private static final String GRID_NAME = "TooMuchOffheapEntriesGrid";

    /** Cache name */
    private static final String CACHE_NAME = "TooMuchOffheapEntriesCache";

    /** Max off-heap memory */
    private static final long OFF_HEAP_MEMORY = 4L << 30;

    /** Entries pack size */
    private static final long PACK_SIZE = 1_000_000L;

    private static final AtomicBoolean wasError = new AtomicBoolean();

    private static final byte[] SAMPLE_ARRAY = new byte[100];

    /** Create config */
    private static IgniteConfiguration config(String gridName) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setGridName(gridName);

        TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500"));
        discoSpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoSpi);

        CacheConfiguration ccfg = new CacheConfiguration(CACHE_NAME);
        ccfg.setMemoryMode(CacheMemoryMode.OFFHEAP_TIERED);
        ccfg.setOffHeapMaxMemory(OFF_HEAP_MEMORY);

        cfg.setCacheConfiguration(ccfg);
        return cfg;
    }

    /** Main */
    public static void main(String[] args) throws Exception {
        IgniteConfiguration cfg = config(GRID_NAME);
        try (Ignite ignite = Ignition.start(cfg)) {
            IgniteCache<CacheKey, CacheKey> cache = ignite.cache(CACHE_NAME);
            CompletionService<Long> cs = new ExecutorCompletionService<>(Executors.newCachedThreadPool());
            System.out.println("Loading the cache");
            int procs = Runtime.getRuntime().availableProcessors();
            long portion = PACK_SIZE / procs;
            long rem = PACK_SIZE % procs;
            long totalCnt = 0;
            while (true) {
                long start = totalCnt;
                for (int i = 1; i <= procs; ++i) {
                    long end = start + portion;
                    if (i == procs)
                        end += rem;
                    cs.submit(new LoadCacheTask(ignite, start, end));
                    start = end;
                }
                long lastSz = 0;
                for (int i = 0; i < procs; ) {
                    Future<Long> fut = cs.poll(10, TimeUnit.SECONDS);
                    if (fut == null) {
                        long sz = cache.sizeLong();
                        if (lastSz == sz) {
                            System.out.println("Timeout waiting load task");
                            System.exit(-1);
                        }
                        System.out.println("Cache size = " + sz);
                        lastSz = sz;
                    }
                    else {
                        long cnt = fut.get();
                        totalCnt += cnt;
                        ++i;
                    }
                }
                if (wasError.get()) {
                    System.out.println("Total count = " + totalCnt);
                    System.exit(-2);
                }
                System.out.println(((double)totalCnt) / PACK_SIZE + " M");
            }
        }
    }

    /** Loads cache with key interval */
    private static class LoadCacheTask implements Callable<Long> {

        /** Ignite instance */
        private final Ignite ignite;

        /** Key interval start (including) */
        private final long start;
        /** Key interval end (excluding) */
        private final long end;

        /** Constructor */
        public LoadCacheTask(Ignite ignite, long start, long end) {
            this.ignite = ignite;
            this.start = start;
            this.end = end;
        }

        /**
         * Loads the cache
         *
         * @return the number of entries loaded
         */
        @Override public Long call() throws Exception {
            IgniteCache<CacheKey, CacheValue> cache = ignite.cache(CACHE_NAME);
            long cnt = 0;
            try {
                CacheKey key = new CacheKey();
                final CacheValue val = new CacheValue();
                CacheEntryProcessor<CacheKey, CacheValue, Object> processor =
                    new CacheEntryProcessor<CacheKey, CacheValue, Object>() {
                        @Override
                        public Object process(MutableEntry<CacheKey, CacheValue> entry,
                            Object... arguments) throws EntryProcessorException {
                            entry.setValue(val);
                            return null;
                        }
                    };
                for (long i = start; i < end; ++i) {
                    key.val = i;
                    val.data = SAMPLE_ARRAY;
                    cache.invoke(key, processor);
                    key.val = i + 1L;
                    val.data = null;
                    cache.invoke(key, processor);
                    ++cnt;
                }
            }
            catch (Throwable ex) {
                wasError.compareAndSet(false, true);
            }
            return cnt;
        }
    }

    /** Mutable cache key */
    private static class CacheKey {

        /** Key value */
        public long val;

        @Override public boolean equals(Object obj) {
            return obj == this || (obj instanceof CacheKey && ((CacheKey)obj).val == val);
        }

        @Override public int hashCode() {
            return (int)val;
        }
    }

    /** Cache value */
    private static class CacheValue {

        public byte[] data;
    }
}
