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

import java.lang.reflect.Field;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.configuration.PageMemoryConfiguration;
import org.apache.ignite.configuration.PageMemoryConfigurationLink;
import org.apache.ignite.internal.managers.discovery.GridDiscoveryManager;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.testframework.junits.logger.GridTestLog4jLogger;
import org.jetbrains.annotations.NotNull;

public class PageMemoryTestUtils {

    public static PageMemory memory(
        int sizeInMb,
        int pageSize,
        int concurrencyLevel,
        int cacheIdToRegister,
        String mappedFilePath,
        Boolean clean
    ) throws IgniteCheckedException {
        return memory(sizeInMb, pageSize, concurrencyLevel, new int[] {cacheIdToRegister}, mappedFilePath, clean);
    }


    public static PageMemory memory(
        int sizeInMb,
        int pageSize,
        int concurrencyLevel,
        int[] cacheIdsToRegister,
        String mappedFilePath,
        Boolean clean
    ) throws IgniteCheckedException {
        MemoryConfiguration memCfg = new MemoryConfiguration();

        try {
            memCfg.setPageSize(pageSize);
        } catch (IllegalArgumentException ex) {
            setPageSize(memCfg, pageSize);
        }

        PageMemoryConfigurationLink link = new PageMemoryConfigurationLink("default");

        memCfg.setDefaultConfiguration(new PageMemoryConfiguration(link,
            sizeInMb * 1024 * 1024, concurrencyLevel, mappedFilePath));

        PageMemoryNoStoreImpl memory = new PageMemoryNoStoreImpl(memCfg, getGridCacheSharedContext(), new GridTestLog4jLogger(), clean);

        memory.start();

        for (int cacheId : cacheIdsToRegister)
            memory.registerCache(cacheId, link);

        return memory;
    }

    private static void setPageSize(MemoryConfiguration cfg, int size) {
        try {
            Field pageSizeField = MemoryConfiguration.class.getDeclaredField("pageSize");

            pageSizeField.setAccessible(true);

            pageSizeField.set(cfg, size);
        } catch (Exception ex) {
            throw new IllegalStateException();
        }
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    @NotNull private static GridCacheSharedContext getGridCacheSharedContext() {
        return new GridCacheSharedContext(null, null, null, null, null, null, null, null, null, null, null, null, null) {
            @Override public GridDiscoveryManager discovery() {
                return new GridDiscoveryManager(null) {
                    @Override public Object consistentId() {
                        return "abc";
                    }
                };
            }
        };
    }
}
