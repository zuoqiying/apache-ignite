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

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 * Test for testing cache memory usage
 *
 * Recomend memory usage -Xmx1500m -Xms1500m
 */
public class CacheMemoryUsageSelfTest extends GridCommonAbstractTest {
    private static final int CACHE_SIZE = 50;

    public void testMemoryUsageDafault() throws Exception {
        startGrid(1);

        filleCache(grid(1), false);
    }

    public void testMemoryUsageCopyOnRead() throws Exception {
        startGrid(1);

        filleCache(grid(1), true);
    }

    public void testIgnateOnEmptyConfiguration() throws Exception {
        IgniteConfiguration igniteCfg = new IgniteConfiguration();

        igniteCfg.setPeerClassLoadingEnabled(true);

        Ignite ignite = Ignition.start(igniteCfg);

        filleCache(ignite, false);
    }

    public void filleCache(Ignite node, boolean copyOnRead) throws Exception {

        CacheConfiguration<Integer, CacheMemoryUsageObject> cacheCfg = new CacheConfiguration<>("cacheBigObject");

        cacheCfg.setCacheMode(CacheMode.PARTITIONED);

        //cacheCfg.setMemoryMode(CacheMemoryMode.OFFHEAP_TIERED);

        cacheCfg.setCopyOnRead(copyOnRead);

        IgniteCache<Integer, CacheMemoryUsageObject> cache = node.getOrCreateCache(cacheCfg);

        memoryReport("Before filling data, cache is empty");

        for (Integer ind =1; ind <= CACHE_SIZE; ind++) {
            CacheMemoryUsageObject obj = new CacheMemoryUsageObject(ind);
            cache.put(ind, obj);
            Thread.sleep(500);
        }

        memoryReport("Before reading, cache is full");

        for (Integer ind = 1; ind <= CACHE_SIZE; ind++) {
            CacheMemoryUsageObject obj = cache.get(ind);
            Thread.sleep(500);
        }

        memoryReport("Read cache again");

        for (Integer ind = 1; ind <= CACHE_SIZE; ind++) {
            CacheMemoryUsageObject obj = cache.get(ind);
            Thread.sleep(500);
        }
    }

    public void memoryReport(String header) throws Exception {
        int sizeMb = 1024 * 1024;

        System.gc();

        Runtime runtime = Runtime.getRuntime();
        System.out.println(System.lineSeparator() + header);
        System.out.print("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / sizeMb + " Mb,");
        System.out.print("free Memory: " + runtime.freeMemory() / sizeMb + " Mb, ");
        System.out.print("total Memory: " + runtime.totalMemory() / sizeMb + " Mb, ");
        System.out.println("max Memory: " + runtime.maxMemory() / sizeMb + " Mb");
    }
}