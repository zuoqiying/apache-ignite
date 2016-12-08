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
import org.apache.ignite.cache.CacheMemoryMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

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
        ccfg.setOffHeapMaxMemory(4L << 30);

        cfg.setCacheConfiguration(ccfg);
        return cfg;
    }

    /** Main */
    public static void main(String[] args) {
        IgniteConfiguration cfg = config(GRID_NAME);
        try (Ignite ignite = Ignition.start(cfg)) {
            IgniteCache<CacheKey, CacheKey> cache = ignite.cache(CACHE_NAME);
            long cnt = 0;
            CacheKey key = new CacheKey();
            System.out.println("Loading the cache");
            while (true) {
                for (int i = 0; i < 1_000_000L; ++i) {
                    ++cnt;
                    key.val = cnt;
                    cache.put(key, key);
                }
                System.out.println(cnt);
            }
        }
    }

    /** Mutable cache key */
    private static class CacheKey {

        /** Key value */
        public long val;
    }
}
