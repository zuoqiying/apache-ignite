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

import java.util.HashSet;
import java.util.Set;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.configuration.MemoryPoolConfiguration;
import org.apache.ignite.configuration.MemoryPoolLink;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.mem.OutOfMemoryException;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

public class CacheMemoryLimitsTest extends GridCommonAbstractTest {

    public CacheMemoryLimitsTest() {
        super(true);
    }

    @Override protected IgniteConfiguration getConfiguration() throws Exception {
        IgniteConfiguration configuration = super.getConfiguration();

        MemoryPoolLink first = new MemoryPoolLink("first");
        MemoryPoolConfiguration memoryPoolConfiguration1 = new MemoryPoolConfiguration(first, 1024 * 1024 * 32, 4, null);

        MemoryPoolLink second = new MemoryPoolLink("second");
        MemoryPoolConfiguration memoryPoolConfiguration2 = new MemoryPoolConfiguration(second, 1024 * 1024 * 32, 4, null);

        MemoryConfiguration cfg = new MemoryConfiguration();

        cfg.addPageMemoryConfiguration(memoryPoolConfiguration1);
        cfg.addPageMemoryConfiguration(memoryPoolConfiguration2);

        configuration.setMemoryConfiguration(cfg);

        CacheConfiguration<Object, Object> ccfg1 = new CacheConfiguration<>();

        ccfg1.setName("cache-1");
        ccfg1.setPageMemoryConfiguration(first);


        CacheConfiguration<Object, Object> ccfg2 = new CacheConfiguration<>();

        ccfg2.setName("cache-2");

        ccfg2.setPageMemoryConfiguration(first);


        CacheConfiguration<Object, Object> ccfg3 = new CacheConfiguration<>();

        ccfg3.setName("cache-3");

        ccfg3.setPageMemoryConfiguration(second);

        configuration.setCacheConfiguration(ccfg1, ccfg2, ccfg3);


        return configuration;
    }


    public void test() throws Exception {
        try {
            IgniteEx ex = grid();

            IgniteCache<Object, Object> cache1 = ex.getOrCreateCache("cache-1");
            IgniteCache<Object, Object> cache2 = ex.getOrCreateCache("cache-2");
            IgniteCache<Object, Object> cache3 = ex.getOrCreateCache("cache-3");

            try {
                int i = 0;
                while (!Thread.currentThread().isInterrupted())
                    cache1.put(i++, i);

                fail();
            } catch (Exception e) {
                if (!oomWasThrown(e))
                    fail();
            }

            try {
                cache2.put(1, 1);
            } catch (Exception e) {
                if (!oomWasThrown(e))
                    fail();
            }

            for (int i = 0; i < 1000; i++)
                cache3.put(i, i);
        }
        finally {
            stopAllGrids();
        }
    }

    private boolean oomWasThrown(Throwable e) {
        return oomWasThrown0(e, new HashSet<>());
    }

    private boolean oomWasThrown0(Throwable e, Set<Throwable> processed) {
        if (!processed.add(e))
            return false;

        boolean found = e instanceof OutOfMemoryException;

        if (e.getCause() != null)
            found |= oomWasThrown0(e.getCause(), processed);

        for (Throwable throwable : e.getSuppressed())
            found |= oomWasThrown0(throwable, processed);

        return found;
    }
}
