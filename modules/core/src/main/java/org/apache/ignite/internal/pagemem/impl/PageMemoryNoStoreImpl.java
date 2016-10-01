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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.configuration.MemoryPoolConfiguration;
import org.apache.ignite.configuration.MemoryPoolLink;
import org.apache.ignite.internal.mem.DirectMemoryRegion;
import org.apache.ignite.internal.mem.DirectMemoryProvider;
import org.apache.ignite.internal.mem.OutOfMemoryException;
import org.apache.ignite.internal.mem.file.MappedFileMemoryProvider;
import org.apache.ignite.internal.mem.unsafe.UnsafeMemoryProvider;
import org.apache.ignite.internal.pagemem.Page;
import org.apache.ignite.internal.pagemem.PageIdUtils;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.database.freelist.FreeList;
import org.apache.ignite.internal.processors.cache.database.freelist.FreeListImpl;
import org.apache.ignite.internal.processors.cache.database.tree.reuse.ReuseList;
import org.apache.ignite.internal.util.GridInt2IntOpenHashMap;
import org.apache.ignite.internal.util.GridUnsafe;
import org.apache.ignite.internal.util.OffheapReadWriteLock;
import org.apache.ignite.internal.util.offheap.GridOffHeapOutOfMemoryException;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lifecycle.LifecycleAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.JavaNioAccess;
import sun.misc.SharedSecrets;

/**
 * Page header structure is described by the following diagram.
 *
 * When page is not allocated (in a free list):
 * <pre>
 * +--------+--------+---------------------------------------------+
 * |8 bytes |8 bytes |     PAGE_SIZE + PAGE_OVERHEAD - 16 bytes    |
 * +--------+--------+---------------------------------------------+
 * |Next ptr|Rel ptr |              Empty                          |
 * +--------+--------+---------------------------------------------+
 * </pre>
 * <p/>
 * When page is allocated and is in use:
 * <pre>
 * +--------+--------+--------+--------+---------------------------+
 * |8 bytes |8 bytes |8 bytes |8 bytes |        PAGE_SIZE          |
 * +--------+--------+--------+--------+---------------------------+
 * | Marker |Page ID |Pin CNT |  Lock  |        Page data          |
 * +--------+--------+--------+--------+---------------------------+
 * </pre>
 *
 * Note that first 8 bytes of page header are used either for page marker or for next relative pointer depending
 * on whether the page is in use or not.
 */
@SuppressWarnings({"LockAcquiredButNotSafelyReleased", "FieldAccessedSynchronizedAndUnsynchronized"})
public class PageMemoryNoStoreImpl implements PageMemory {
    /** */
    public static final long PAGE_MARKER = 0xBEEAAFDEADBEEF01L;

    /** Full relative pointer mask. */
    private static final long RELATIVE_PTR_MASK = 0xFFFFFFFFFFFFFFL;

    /** Invalid relative pointer value. */
    private static final long INVALID_REL_PTR = RELATIVE_PTR_MASK;

    /** Address mask to avoid ABA problem. */
    private static final long ADDRESS_MASK = 0xFFFFFFFFFFFFFFL;

    /** Counter mask to avoid ABA problem. */
    private static final long COUNTER_MASK = ~ADDRESS_MASK;

    /** Counter increment to avoid ABA problem. */
    private static final long COUNTER_INC = ADDRESS_MASK + 1;

    /** Page ID offset. */
    public static final int PAGE_ID_OFFSET = 8;

    /** Page pin counter offset. */
    public static final int PIN_CNT_OFFSET = 16;

    /** Page pin counter offset. */
    public static final int LOCK_OFFSET = 24;

    /**
     * Need a 8-byte pointer for linked list, 8 bytes for internal needs (flags),
     * 4 bytes cache ID, 8 bytes timestamp.
     */
    public static final int PAGE_OVERHEAD = LOCK_OFFSET + OffheapReadWriteLock.LOCK_SIZE;

    /** Page size. */
    private int sysPageSize;

    /** Direct byte buffer factory. */
    private JavaNioAccess nioAccess;

    /** */
    private final IgniteLogger log;

    /** Segments array. */
    private Segment[] segments;

    /** Number of bits required to store segment index. */
    private int segBits;

    /** Number of bits left to store page index. */
    private int idxBits;

    /** */
    private int segMask;

    /** */
    private int idxMask;

    private volatile GridInt2IntOpenHashMap cachePageMemory;

    private final long cachePageMemoryOff;

    private final List<MemoryPool> memoryPools;

    private final GridCacheSharedContext<?, ?> sharedCtx;

    /** */
    private OffheapReadWriteLock rwLock;

    public PageMemoryNoStoreImpl(@NotNull  MemoryConfiguration memCfg,
        @NotNull GridCacheSharedContext<?, ?> sharedCtx,
        @Nullable IgniteLogger log,
        boolean clean
    ) {
        this.sharedCtx = sharedCtx;

        this.log = log != null ? log : sharedCtx.logger(PageMemoryNoStoreImpl.class) ;
        // TODO configure concurrency level.
        this.rwLock = new OffheapReadWriteLock(128);

        sysPageSize = memCfg.getPageSize() + PAGE_OVERHEAD;

        String consId = String.valueOf(sharedCtx.discovery().consistentId());

        consId = consId.replaceAll("[:,\\.]", "_");

        List<MemoryPool> map = new ArrayList<>();

        int segmentCnt = 0;

        String dfltAllocationPath = memCfg.getFileCacheAllocationPath();

        for (MemoryPoolConfiguration pool : memCfg.getMemoryPoolConfigurations()) {
            int concLvl = pool.getConcurrencyLevel();

            if (concLvl < 2)
                pool.setConcLvl(concLvl = Runtime.getRuntime().availableProcessors());

            segmentCnt += concLvl;

            long fragmentSize = pool.getSize() / concLvl;

            if (fragmentSize < 1024 * 1024)
                fragmentSize = 1024 * 1024;

            long[] sizes = new long[concLvl];

            for (int i = 0; i < concLvl; i++)
                sizes[i] = fragmentSize;

            String path = pool.getTmpFsPath();

            File allocPath = path != null ? buildPath(path, consId) :
                dfltAllocationPath != null ? buildPath(dfltAllocationPath, consId) : null;

            DirectMemoryProvider memProvider = allocPath == null ?
                new UnsafeMemoryProvider(sizes) :
                new MappedFileMemoryProvider(log, allocPath, clean, sizes);

            map.add(new MemoryPool(pool.getLink(), memProvider, segmentCnt - concLvl, segmentCnt));
        }

        memoryPools = map;

        try {
            this.cachePageMemoryOff = GridUnsafe.objectFieldOffset(PageMemoryNoStoreImpl.class.getDeclaredField("cachePageMemory"));
        }
        catch (NoSuchFieldException e) {
            throw new IllegalStateException();
        }
    }

    /**
     * @param path   Path to the working directory.
     * @param consId Consistent ID of the local node.
     * @return DB storage path.
     */
    private File buildPath(String path, String consId) {
        File pathAsFile = new File(path);

        if (pathAsFile.isAbsolute())
            return new File(pathAsFile, consId);

        String igniteHomeStr = U.getIgniteHome();

        File igniteHome = igniteHomeStr != null ? new File(igniteHomeStr) : null;

        File workDir = igniteHome == null ? pathAsFile : new File(igniteHome, path);

        return new File(workDir, consId);
    }

    /** {@inheritDoc} */
    @Override public void start() throws IgniteException {
        if (segments != null)
            throw new IllegalStateException("Already started");

        List<Segment> segments = new ArrayList<>();

        for (MemoryPool pool : memoryPools) {
            if (pool.directMemoryProvider instanceof LifecycleAware)
                ((LifecycleAware)pool.directMemoryProvider).start();

            for (int i = pool.startIdx; i < pool.lastIdx; i++) {
                segments.add(new Segment(i, pool.directMemoryProvider.memory().regions().get(i - pool.startIdx)));

                segments.get(i).init();
            }
        }

        nioAccess = SharedSecrets.getJavaNioAccess();

        this.segments = segments.toArray(new Segment[segments.size()]);

        segBits = Integer.SIZE - Integer.numberOfLeadingZeros(segments.size() - 1);
        idxBits = PageIdUtils.PAGE_IDX_SIZE - segBits;

        segMask = ~(-1 << segBits);
        idxMask = ~(-1 << idxBits);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("OverlyStrongTypeCast")
    @Override public void stop() throws IgniteException {
        if (log.isDebugEnabled())
            log.debug("Stopping page memory.");

        for (MemoryPool region : memoryPools) {
            DirectMemoryProvider directMemoryProvider = region.directMemoryProvider;

            if (directMemoryProvider instanceof LifecycleAware)
                ((LifecycleAware)directMemoryProvider).stop();

            if (directMemoryProvider instanceof Closeable) {
                try {
                    ((Closeable)directMemoryProvider).close();
                }
                catch (IOException e) {
                    throw new IgniteException(e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override public long allocatePage(int cacheId, int partId, byte flags) {
        long relPtr = INVALID_REL_PTR;
        long absPtr = 0;

        MemoryPool region = memoryPools.get(cachePageMemory.get(cacheId));

        for (int i = region.startIdx; i < region.lastIdx; i++) {
            Segment seg = segments[i];

            relPtr = seg.borrowFreePage();

            if (relPtr != INVALID_REL_PTR) {
                absPtr = seg.absolute(relPtr);

                break;
            }
        }

        // No segments conatined a free page.
        if (relPtr == INVALID_REL_PTR) {
            int segAllocIdx = region.nextRoundRobinIndex();

            for (int i = region.startIdx; i < region.lastIdx; i++) {
                int idx = segAllocIdx + i;

                if (idx >= region.lastIdx)
                    idx = idx % (region.lastIdx - region.startIdx)  + region.startIdx;

                Segment seg = segments[idx];

                relPtr = seg.allocateFreePage();

                if (relPtr != INVALID_REL_PTR) {
                    absPtr = seg.absolute(relPtr);

                    break;
                }
            }
        }

        if (relPtr == INVALID_REL_PTR)
            throw new OutOfMemoryException();

        assert (relPtr & ~PageIdUtils.PAGE_IDX_MASK) == 0;

        // Assign page ID according to flags and partition ID.
        long pageId = PageIdUtils.pageId(partId, flags, (int)relPtr);

        writePageId(absPtr, pageId);

        // Clear pin counter.
        GridUnsafe.putLong(absPtr + PIN_CNT_OFFSET, 0);

        // TODO pass an argument to decide whether the page should be cleaned.
        GridUnsafe.setMemory(absPtr + PAGE_OVERHEAD, sysPageSize - PAGE_OVERHEAD, (byte)0);

        return pageId;
    }

    /** {@inheritDoc} */
    @Override public boolean freePage(int cacheId, long pageId) {
        Segment seg = segment(pageId);

        seg.releaseFreePage(pageId);

        return true;
    }

    /** {@inheritDoc} */
    @Override public Page page(int cacheId, long pageId) throws IgniteCheckedException {
        Segment seg = segment(pageId);

        return seg.acquirePage(cacheId, pageId);
    }

    /** {@inheritDoc} */
    @Override public void releasePage(Page p) {
        PageNoStoreImpl page = (PageNoStoreImpl)p;

        Segment seg = segments[page.segmentIndex()];

        seg.releasePage(page);
    }

    /** {@inheritDoc} */
    @Override public int pageSize() {
        return sysPageSize - PAGE_OVERHEAD;
    }

    /** {@inheritDoc} */
    @Override public int systemPageSize() {
        return sysPageSize;
    }

    @Override public void registerCache(int cacheId, MemoryPoolLink configuration) {
        int idx = -1;

        for (int i = 0; i < memoryPools.size(); i++) {
            if (memoryPools.get(i).link.equals(configuration)) {
                idx = i;

                break;
            }
        }

        assert idx != -1;


        while (true) {
            GridInt2IntOpenHashMap map = cachePageMemory;

            GridInt2IntOpenHashMap cp;

            if (map != null)
                cp = new GridInt2IntOpenHashMap(map, 0.33f);
            else
                cp = new GridInt2IntOpenHashMap(1, 0.33f);

            cp.put(cacheId, idx);


            if (GridUnsafe.compareAndSwapObject(this, cachePageMemoryOff, map, cp))
                break;
        }
    }

    @Override public ReuseList reuseList(int cacheId) {
        return getFreeListImpl(cacheId);
    }

    private FreeListImpl getFreeListImpl(int cacheId) {
        MemoryPool region = memoryPools.get(cachePageMemory.get(cacheId));

        FreeListImpl list = region.freeList;

        if (list == null) {

            try {
                MemoryPool.freeListUpdater.compareAndSet(region, null,
                    new FreeListImpl(cacheId, "", this, null, sharedCtx.wal(), 0L, true));

                list = region.freeList;
            }
            catch (IgniteCheckedException e) {
                throw new IllegalStateException();
            }

        }

        return list;
    }

    @Override public FreeList freeList(int cacheId) {
        return getFreeListImpl(cacheId);
    }

    /**
     * @return Total number of loaded pages in memory.
     */
    public long loadedPages() {
        long total = 0;

        for (Segment seg : segments) {
            seg.readLock().lock();

            try {
                total += seg.allocatedPages();
            }
            finally {
                seg.readLock().unlock();
            }
        }

        return total;
    }

    /**
     * @return Total number of acquired pages.
     */
    public long acquiredPages() {
        long total = 0;

        for (Segment seg : segments) {
            seg.readLock().lock();

            try {
                total += seg.acquiredPages();
            }
            finally {
                seg.readLock().unlock();
            }
        }

        return total;
    }

    /**
     * @param absPtr Page absolute address.
     */
    boolean readLockPage(long absPtr, int tag) {
        return rwLock.readLock(absPtr + LOCK_OFFSET, tag);
    }

    /**
     * @param absPtr Page absolute address.
     */
    void readUnlockPage(long absPtr) {
        rwLock.readUnlock(absPtr + LOCK_OFFSET);
    }

    /**
     * @param absPtr Page absolute address.
     */
    boolean writeLockPage(long absPtr, int tag) {
        return rwLock.writeLock(absPtr + LOCK_OFFSET, tag);
    }

    /**
     * @param absPtr Page absolute address.
     * @return {@code True} if locked page.
     */
    boolean tryWriteLockPage(long absPtr, int tag) {
        return rwLock.tryWriteLock(absPtr + LOCK_OFFSET, tag);
    }

    /**
     * @param absPtr Page absolute address.
     */
    void writeUnlockPage(long absPtr, int newTag) {
        rwLock.writeUnlock(absPtr + LOCK_OFFSET, newTag);
    }

    /**
     * @param absPtr Absolute pointer to the page.
     * @return {@code True} if write lock acquired for the page.
     */
    boolean isPageWriteLocked(long absPtr) {
        return rwLock.isWriteLocked(absPtr + LOCK_OFFSET);
    }

    /**
     * @param absPtr Absolute pointer to the page.
     * @return {@code True} if read lock acquired for the page.
     */
    boolean isPageReadLocked(long absPtr) {
        return rwLock.isReadLocked(absPtr + LOCK_OFFSET);
    }

    /**
     * @param ptr Pointer to wrap.
     * @param len Memory location length.
     * @return Wrapped buffer.
     */
    ByteBuffer wrapPointer(long ptr, int len) {
        ByteBuffer buf = nioAccess.newDirectByteBuffer(ptr, len, null);

        buf.order(ByteOrder.nativeOrder());

        return buf;
    }

    /**
     * Reads page ID from the page at the given absolute position.
     *
     * @param absPtr Absolute memory pointer to the page header.
     * @return Page ID written to the page.
     */
    long readPageId(long absPtr) {
        return GridUnsafe.getLong(absPtr + PAGE_ID_OFFSET);
    }

    /**
     * Writes page ID to the page at the given absolute position.
     *
     * @param absPtr Absolute memory pointer to the page header.
     * @param pageId Page ID to write.
     */
    void writePageId(long absPtr, long pageId) {
        GridUnsafe.putLong(absPtr + PAGE_ID_OFFSET, pageId);
    }

    /**
     * @param pageId Page ID.
     * @return Segment.
     */
    private Segment segment(long pageId) {
        long pageIdx = PageIdUtils.pageIndex(pageId);

        int segIdx = segmentIndex(pageIdx);

        return segments[segIdx];
    }

    /**
     * @param pageIdx Page index to extract segment index from.
     * @return Segment index.
     */
    private int segmentIndex(long pageIdx) {
        return (int)((pageIdx >> idxBits) & segMask);
    }

    /**
     * @param segIdx Segment index.
     * @param pageIdx Page index.
     * @return Full page index.
     */
    private long fromSegmentIndex(int segIdx, long pageIdx) {
        long res = 0;

        res = (res << segBits) | (segIdx & segMask);
        res = (res << idxBits) | (pageIdx & idxMask);

        return res;
    }

    /**
     *
     */
    private class Segment extends ReentrantReadWriteLock {
        /** */
        private static final long serialVersionUID = 0L;

        /** Segment index. */
        private int idx;

        /** Direct memory chunk. */
        private DirectMemoryRegion region;

        /** Pointer to the address of the free page list. */
        private long freePageListPtr;

        /** Last allocated page index. */
        private long lastAllocatedIdxPtr;

        /** Base address for all pages. */
        private long pagesBase;

        /** */
        private final AtomicInteger allocatedPages;

        /** */
        private final AtomicInteger acquiredPages;

        /**
         * @param idx Index.
         * @param region Memory region to use.
         */
        private Segment(int idx, DirectMemoryRegion region) {
            this.idx = idx;
            this.region = region;

            allocatedPages = new AtomicInteger();
            acquiredPages = new AtomicInteger();
        }

        /**
         * Initializes page memory segment.
         */
        private void init() {
            long base = region.address();

            freePageListPtr = base;

            base += 8;

            lastAllocatedIdxPtr = base;

            base += 8;

            // Align by 8 bytes.
            pagesBase = (base + 7) & ~0x7;

            GridUnsafe.putLong(freePageListPtr, INVALID_REL_PTR);
            GridUnsafe.putLong(lastAllocatedIdxPtr, 0);
        }

        /**
         * @param pageId Page ID to pin.
         * @return Pinned page impl.
         */
        @SuppressWarnings("TypeMayBeWeakened")
        private PageNoStoreImpl acquirePage(int cacheId, long pageId) {
            long absPtr = absolute(pageId);

            long marker = GridUnsafe.getLong(absPtr);

            if (marker != PAGE_MARKER)
                throw new IllegalStateException("Page was not allocated [absPtr=" + U.hexLong(absPtr) +
                    ", cacheId=" + cacheId + ", pageId=" + U.hexLong(pageId) +
                    ", marker=" + U.hexLong(marker) + ']');

            while (true) {
                long pinCnt = GridUnsafe.getLong(absPtr + PIN_CNT_OFFSET);

                if (pinCnt < 0)
                    throw new IllegalStateException("Page has been deallocated [absPtr=" + U.hexLong(absPtr) +
                        ", cacheId=" + cacheId + ", pageId=" + U.hexLong(pageId) + ", pinCnt=" + pinCnt + ']');

                if (GridUnsafe.compareAndSwapLong(null, absPtr + PIN_CNT_OFFSET, pinCnt, pinCnt + 1))
                    break;
            }

            acquiredPages.incrementAndGet();

            return new PageNoStoreImpl(PageMemoryNoStoreImpl.this, idx, absPtr, cacheId, pageId);
        }

        /**
         * @param pinnedPage Page to unpin.
         */
        private void releasePage(PageNoStoreImpl pinnedPage) {
            long absPtr = pinnedPage.absolutePointer();

            while (true) {
                long pinCnt = GridUnsafe.getLong(absPtr + PIN_CNT_OFFSET);

                assert pinCnt > 0 : "Releasing a page that was not pinned [page=" + pinnedPage +
                    ", pinCnt=" + pinCnt + ']';

                if (GridUnsafe.compareAndSwapLong(null, absPtr + PIN_CNT_OFFSET, pinCnt, pinCnt - 1))
                    break;
            }

            acquiredPages.decrementAndGet();
        }

        /**
         * @param relativePtr Relative pointer.
         * @return Absolute pointer.
         */
        private long absolute(long relativePtr) {
            int pageIdx = PageIdUtils.pageIndex(relativePtr);

            pageIdx &= idxMask;

            long offset = pageIdx * sysPageSize;

            return pagesBase + offset;
        }

        /**
         * @return Total number of loaded pages for the segment.
         */
        private int allocatedPages() {
            return allocatedPages.get();
        }

        /**
         * @return Total number of currently acquired pages.
         */
        private int acquiredPages() {
            return acquiredPages.get();
        }

        /**
         * @param pageId Page ID to release.
         */
        private void releaseFreePage(long pageId) {
            // Clear out flags and file ID.
            long relPtr = PageIdUtils.pageId(0, (byte)0, PageIdUtils.pageIndex(pageId));

            long absPtr = absolute(relPtr);

            // Prepare page to free.
            // First, swap pin counter down to -1.
            while (true) {
                long pinCnt = GridUnsafe.getLong(absPtr + PIN_CNT_OFFSET);

                assert pinCnt >= 0 : "pinCnt=" + pinCnt + ", relPtr=" + U.hexLong(relPtr);

                if (pinCnt > 0)
                    throw new IllegalStateException("Releasing a page being in use: " + U.hexLong(relPtr));

                if (GridUnsafe.compareAndSwapLong(null, absPtr + PIN_CNT_OFFSET, 0, -1))
                    break;
            }

            // Second, write clean relative pointer instead of page ID.
            writePageId(absPtr, relPtr);

            // Third, link the free page.
            while (true) {
                long freePageRelPtrMasked = GridUnsafe.getLong(freePageListPtr);

                long freePageRelPtr = freePageRelPtrMasked & RELATIVE_PTR_MASK;

                GridUnsafe.putLong(absPtr, freePageRelPtr);

                if (GridUnsafe.compareAndSwapLong(null, freePageListPtr, freePageRelPtrMasked, relPtr)) {
                    allocatedPages.decrementAndGet();

                    return;
                }
            }
        }

        /**
         * @return Relative pointer to a free page that was borrowed from the allocated pool.
         */
        private long borrowFreePage() {
            while (true) {
                long freePageRelPtrMasked = GridUnsafe.getLong(freePageListPtr);

                long freePageRelPtr = freePageRelPtrMasked & ADDRESS_MASK;
                long cnt = ((freePageRelPtrMasked & COUNTER_MASK) + COUNTER_INC) & COUNTER_MASK;

                if (freePageRelPtr != INVALID_REL_PTR) {
                    long freePageAbsPtr = absolute(freePageRelPtr);

                    long nextFreePageRelPtr = GridUnsafe.getLong(freePageAbsPtr) & ADDRESS_MASK;

                    if (GridUnsafe.compareAndSwapLong(null, freePageListPtr, freePageRelPtrMasked, nextFreePageRelPtr | cnt)) {
                        GridUnsafe.putLong(freePageAbsPtr, PAGE_MARKER);

                        allocatedPages.incrementAndGet();

                        return freePageRelPtr;
                    }
                }
                else
                    return INVALID_REL_PTR;
            }
        }

        /**
         * @return Relative pointer of the allocated page.
         * @throws GridOffHeapOutOfMemoryException
         */
        private long allocateFreePage() throws GridOffHeapOutOfMemoryException {
            long limit = region.address() + region.size();

            while (true) {
                long lastIdx = GridUnsafe.getLong(lastAllocatedIdxPtr);

                // Check if we have enough space to allocate a page.
                if (pagesBase + (lastIdx + 1) * sysPageSize > limit)
                    return INVALID_REL_PTR;

                if (GridUnsafe.compareAndSwapLong(null, lastAllocatedIdxPtr, lastIdx, lastIdx + 1)) {
                    long absPtr = pagesBase + lastIdx * sysPageSize;

                    assert lastIdx <= PageIdUtils.MAX_PAGE_NUM : lastIdx;

                    long pageIdx = fromSegmentIndex(idx, lastIdx);

                    assert pageIdx != INVALID_REL_PTR;

                    writePageId(absPtr, pageIdx);

                    GridUnsafe.putLong(absPtr, PAGE_MARKER);

                    rwLock.init(absPtr + LOCK_OFFSET, 0);

                    allocatedPages.incrementAndGet();

                    return pageIdx;
                }
            }
        }
    }

    private static class MemoryPool {
        private static final AtomicIntegerFieldUpdater<MemoryPool> updater =
            AtomicIntegerFieldUpdater.newUpdater(MemoryPool.class, "selector");

        private static final AtomicReferenceFieldUpdater<MemoryPool, FreeListImpl> freeListUpdater =
            AtomicReferenceFieldUpdater.newUpdater(MemoryPool.class, FreeListImpl.class, "freeList");


        private final MemoryPoolLink link;
        /** Direct memory allocator. */
        private final DirectMemoryProvider directMemoryProvider;

        private final int startIdx; //inclusive
        private final int lastIdx;  //exclusive

        private volatile int selector = 0;

        private volatile FreeListImpl freeList;

        public MemoryPool(MemoryPoolLink link, DirectMemoryProvider directMemoryProvider, int startIdx, int lastIdx) {
            this.link = link;
            assert startIdx < lastIdx;

            this.directMemoryProvider = directMemoryProvider;
            this.startIdx = startIdx;
            this.lastIdx = lastIdx;
        }

        /** */
        private int nextRoundRobinIndex() {
            while (true) {
                int idx = selector;

                int nextIdx = idx + 1;

                if (nextIdx >= lastIdx)
                    nextIdx = startIdx;

                if (updater.compareAndSet(this, idx, nextIdx))
                    return nextIdx;
            }
        }
    }
}
