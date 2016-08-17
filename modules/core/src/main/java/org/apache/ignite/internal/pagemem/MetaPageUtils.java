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
 *
 */

package org.apache.ignite.internal.pagemem;

import org.apache.ignite.IgniteCheckedException;

public class MetaPageUtils {

    /**
     * @param pageMem Page memory.
     * @param cacheId Cache ID.
     * @return Allocated pages.
     * @throws IgniteCheckedException
     */
    public static Metas getOrAllocateMetas(
        final PageMemory pageMem,
        final int cacheId
    ) throws IgniteCheckedException {
        try (Page metaPage = pageMem.metaPage(cacheId)) {
            final long metastoreRoot = metaPage.id();

            final int segments = Runtime.getRuntime().availableProcessors() * 2;

            final long[] rootIds = allocateMetas(pageMem, cacheId, 0, PageIdAllocator.FLAG_IDX, segments);

            return new Metas(rootIds, metastoreRoot, true);
        }
    }

    /**
     * @param pageMem Page memory.
     * @param cacheId Cache ID.
     * @return Allocated pages.
     * @throws IgniteCheckedException
     */
    public static Metas getOrAllocatePartMetas(
        final PageMemory pageMem,
        final int cacheId,
        final int partId
    ) throws IgniteCheckedException {
        try (Page metaPage = pageMem.partMetaPage(cacheId, partId)) {
            final long metastoreRoot = metaPage.id();

            final int segments = Runtime.getRuntime().availableProcessors() * 2;

            final long[] rootIds = allocateMetas(pageMem, cacheId, partId, PageIdAllocator.FLAG_DATA, segments);

            return new Metas(rootIds, metastoreRoot, true);
        }
    }

    /**
     * @param pageMem Page memory.
     * @param cacheId Cache ID.
     * @param partId Partition ID.
     * @param allocSpace Allocation space.
     * @return Allocated metapages.
     * @throws IgniteCheckedException
     */
    public static long[] allocateMetas(
        final PageIdAllocator pageMem,
        final int cacheId,
        final int partId,
        final byte allocSpace,
        int segments
    ) throws IgniteCheckedException {
        final long[] rootIds = new long[segments];

        for (int i = 0; i < segments; i++)
            rootIds[i] = pageMem.allocatePage(cacheId, partId, allocSpace);

        return rootIds;
    }

    /**
     *
     */
    public static class Metas {
        /** Meta root IDs. */
        public final long[] rootIds;

        /** Indicates whether pages were newly allocated. */
        public final boolean initNew;

        /** Metastore root page. */
        private final long metastoreRoot;

        /**
         * @param rootIds Meta root IDs.
         * @param metastoreRoot Indicates whether pages were newly allocated.
         * @param initNew Metastore root page.
         */
        public Metas(final long[] rootIds, final long metastoreRoot, final boolean initNew) {
            this.rootIds = rootIds;
            this.initNew = initNew;
            this.metastoreRoot = metastoreRoot;
        }

        /**
         * @return Meta root IDs.
         */
        public long[] rootIds() {
            return rootIds;
        }

        /**
         * @return Indicates whether pages were newly allocated.
         */
        public boolean isInitNew() {
            return initNew;
        }

        /**
         * @return Metastore root page.
         */
        public long metastoreRoot() {
            return metastoreRoot;
        }
    }

}
