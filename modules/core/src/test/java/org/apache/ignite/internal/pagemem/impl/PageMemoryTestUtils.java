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

import java.nio.ByteBuffer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.configuration.PageMemoryConfiguration;
import org.apache.ignite.configuration.PageMemoryConfigurationLink;
import org.apache.ignite.internal.managers.discovery.GridDiscoveryManager;
import org.apache.ignite.internal.pagemem.FullPageId;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.util.typedef.CIX2;
import org.apache.ignite.testframework.junits.GridTestKernalContext;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

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

        memCfg.setPageSize(pageSize);

        PageMemoryConfigurationLink link = new PageMemoryConfigurationLink("default");

        memCfg.setDefaultConfiguration(new PageMemoryConfiguration(link,
            sizeInMb * 1024 * 1024, concurrencyLevel, mappedFilePath));

        PageMemoryNoStoreImpl memory = new PageMemoryNoStoreImpl(memCfg, getGridCacheSharedContext(), null, clean);

        memory.start();

        for (int cacheId : cacheIdsToRegister)
            memory.registerCache(cacheId, link);

        return memory;
    }


    @NotNull private static GridCacheSharedContext getGridCacheSharedContext() {
        GridCacheSharedContext cctx = Mockito.mock(GridCacheSharedContext.class);

        GridDiscoveryManager discovery = Mockito.mock(GridDiscoveryManager.class);

        Mockito.when(cctx.discovery()).thenReturn(discovery);

        Mockito.when(discovery.consistentId()).thenReturn("abc");

        return cctx;
    }
}
