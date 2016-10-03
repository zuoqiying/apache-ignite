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

package org.apache.ignite.internal.pagemem;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.MemoryPoolLink;
import org.apache.ignite.internal.processors.cache.database.freelist.FreeList;
import org.apache.ignite.internal.processors.cache.database.tree.reuse.ReuseList;
import org.apache.ignite.lifecycle.LifecycleAware;

/**
 */
public interface PageMemory extends LifecycleAware, PageIdAllocator {
    /**
     * Gets the page associated with the given page ID. Each page obtained with this method must be released by
     * calling {@link #releasePage(Page)}. This method will allocate page with given ID if it doesn't exist.
     *
     * @param cacheId Cache ID.
     * @param pageId Page ID.
     * @return Page.
     */
    public Page page(int cacheId, long pageId) throws IgniteCheckedException;

    /**
     * @param page Page to release.
     */
    public void releasePage(Page page) throws IgniteCheckedException;

    /**
     * @return Page size in bytes.
     */
    public int pageSize();

    /**
     * @return Page size with system overhead, in bytes.
     */
    public int systemPageSize();

    /**
     * Connects cacheId with memory pool
     *
     * @param cacheId cache id which will be linked with memory pool
     * @param memoryPool memory pool which will be used for the cache
     */
    public void registerCache(int cacheId, MemoryPoolLink memoryPool);

    /**
     * @param cacheId cache id
     * @return link to memory pool which is used for the cache
     */
    public MemoryPoolLink getMemoryPool(int cacheId);
}
