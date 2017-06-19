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

package org.apache.ignite.internal.processors.query.h2.opt.database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.pagemem.PageUtils;
import org.apache.ignite.internal.pagemem.wal.IgniteWriteAheadLogManager;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.database.RootPage;
import org.apache.ignite.internal.processors.cache.database.tree.BPlusTree;
import org.apache.ignite.internal.processors.cache.database.tree.io.BPlusIO;
import org.apache.ignite.internal.processors.cache.database.tree.io.BPlusInnerIO;
import org.apache.ignite.internal.processors.cache.database.tree.io.BPlusLeafIO;
import org.apache.ignite.internal.processors.cache.database.tree.io.IOVersions;
import org.apache.ignite.internal.processors.cache.database.tree.reuse.ReuseList;
import org.apache.ignite.internal.processors.cache.database.tree.util.PageHandler;
import org.apache.ignite.internal.processors.query.h2.opt.GridLuceneFile;
import org.apache.ignite.internal.processors.query.h2.opt.GridLuceneInputStream;
import org.apache.ignite.internal.processors.query.h2.opt.GridLuceneLockFactory;
import org.apache.ignite.internal.processors.query.h2.opt.GridLuceneOutputStream;
import org.apache.ignite.internal.util.lang.GridCursor;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

/**
 */
public class LuceneDirectory extends BaseDirectory {
    /** Maximum filename length in bytes. */
    private static final int MAX_FILENAME_LEN = 768;

    /** Index tree. */
    private final DirectoryTree fileTree;

    /**
     */
    public LuceneDirectory(final GridCacheContext cctx) {
        super(new GridLuceneLockFactory());

        try {
            String name = BPlusTree.treeName(cctx.name(), "Lucene");
            RootPage rootPage = cctx.offheap().rootPageForIndex(name);

            fileTree = new DirectoryTree(name,
                cctx.cacheId(),
                cctx.memoryPolicy().pageMemory(),
                cctx.shared().wal(),
                cctx.offheap().globalRemoveId(),
                rootPage.pageId().pageId(),
                cctx.offheap().reuseListForIndex(name),
                LuceneDirectoryInnerIO.VERSIONS,
                LuceneDirectoryLeafIO.VERSIONS,
                rootPage.isAllocated());
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public final String[] listAll() {
        ensureOpen();
        try {
            GridCursor<DirectoryItem> cursor = null;

            cursor = fileTree.find(null, null);

            List<String> fileNames = new ArrayList<>((int)fileTree.size());

            while (cursor.next())
                fileNames.add(new String(cursor.get().getFileName()));

            // NOTE: fileMap.keySet().toArray(new String[0]) is broken in non Sun JDKs,
            // and the code below is resilient to map changes during the array population.
            return fileNames.toArray(new String[fileNames.size()]);
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException(e);
        }

    }

    /** {@inheritDoc} */
    @Override public void renameFile(String source, String dest) throws IOException {
        ensureOpen();

        try {
            DirectoryItem srcFile = fileTree.findOne(new DirectoryItem(source.getBytes()));

            if (srcFile == null)
                throw new FileNotFoundException(source);

            DirectoryItem old = fileTree.put(new DirectoryItem(dest.getBytes(), srcFile.getPageId()));

            assert old == null : old;

            fileTree.remove(srcFile);
        }
        catch (IgniteCheckedException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public final long fileLength(String name) throws IOException {
        ensureOpen();

        DirectoryItem file;

        try {
            file = fileTree.findOne(new DirectoryItem(name.getBytes()));
        }
        catch (IgniteCheckedException e) {
            throw new IOException(e);
        }

        if (file == null)
            throw new FileNotFoundException(name);

        return file.getLength(); //TODO: move to IO. Read size from file meta page directly.
    }

    /** {@inheritDoc} */
    @Override public void deleteFile(String name) throws IOException {
        ensureOpen();

        doDeleteFile(name);
    }

    /**
     * Deletes file.
     *
     * @throws IOException If failed.
     */
    private void doDeleteFile(String name) throws IOException {

        DirectoryItem file = fileTree.remove(new DirectoryItem(name.getBytes()));

        if (file != null) {
            file.delete(); //TODO: add 'delete' method to DirectoryItem

            adjustDirectorySize(-file.getSizeInBytes());
        }
        else
            throw new FileNotFoundException(new String(item.fileName));
    }

    /**
     * Adjust index size.
     * @param delta index size delta in bytes.
     */
    private void adjustDirectorySize(long delta) {
        sizeInBytes.addAndGet(delta); //TODO: Track size locally or save DirectorySize to Metapage.
    }

    /** {@inheritDoc} */
    @Override public IndexOutput createOutput(final String name, final IOContext context) throws IOException {
        ensureOpen();

        DirectoryItem existing = fileTree.remove(new DirectoryItem(name.getBytes()));

        if (existing != null) {
            existing.delete();

            adjustDirectorySize(-existing.getSizeInBytes());
        }

        LuceneFile file = newRAMFile(name);

        fileTree.put(new DirectoryItem(name.getBytes(), file.getPageId());

        return new GridLuceneOutputStream(file);
    }

    /** {@inheritDoc} */
    @Override public void sync(final Collection<String> names) throws IOException {
        // Noop. No fsync needed as all data is in-memory.
    }

    /**
     * Returns a new {@link LuceneFile} for storing data. This method can be
     * overridden to return different {@link LuceneFile} impls, that e.g. override.
     *
     * @return New ram file.
     */
    protected LuceneFile newRAMFile(final String name) {
        return new LuceneFile(this, name.getBytes());
    }

    /** {@inheritDoc} */
    @Override public IndexInput openInput(final String name, final IOContext context) throws IOException {
        ensureOpen();

        LuceneFile file = fileTree.findOne(new DirectoryItem(name.getBytes()));

        if (file == null)
            throw new FileNotFoundException(name);

        return new GridLuceneInputStream(name, file);
    }

    /** {@inheritDoc} */
    @Override public void close() {
        isOpen = false;

        //TODO: We won't delete persistent index.
        /*for (String fileName : fileTree.keySet()) {
            try {
                doDeleteFile(fileName);
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        assert fileTree.isEmpty();
        */
    }

    /**
     * Store row to buffer.
     *
     * @param pageAddr Page address.
     * @param off Offset in buf.
     * @param row Row to store.
     */
    private static void storeRow(
        final long pageAddr,
        int off,
        final DirectoryItem row
    ) {
        byte[] fileName = row.getFileName();

        assert fileName.length <= Byte.MAX_VALUE;

        // Index name length.
        PageUtils.putByte(pageAddr, off, (byte)fileName.length);
        off++;

        // Index name.
        PageUtils.putBytes(pageAddr, off, fileName);
        off += fileName.length;

        // Page ID.
        PageUtils.putLong(pageAddr, off, row.getPageId());
    }

    /**
     * Copy row data.
     *
     * @param dstPageAddr Destination page address.
     * @param dstOff Destination buf offset.
     * @param srcPageAddr Source page address.
     * @param srcOff Src buf offset.
     */
    private static void storeRow(
        final long dstPageAddr,
        int dstOff,
        final long srcPageAddr,
        int srcOff
    ) {
        // Index name length.
        final byte len = PageUtils.getByte(srcPageAddr, srcOff);
        srcOff++;

        PageUtils.putByte(dstPageAddr, dstOff, len);
        dstOff++;

        PageHandler.copyMemory(srcPageAddr, srcOff, dstPageAddr, dstOff, len);
        srcOff += len;
        dstOff += len;

        // Page ID.
        PageUtils.putLong(dstPageAddr, dstOff, PageUtils.getLong(srcPageAddr, srcOff));
    }

    /**
     * Read row from buffer.
     *
     * @param pageAddr Page address.
     * @param off Offset.
     * @return Read row.
     */
    private static DirectoryItem readRow(final long pageAddr, int off) {
        // Index name length.
        final int len = PageUtils.getByte(pageAddr, off) & 0xFF;
        off++;

        // Index name.
        final byte[] idxName = PageUtils.getBytes(pageAddr, off, len);
        off += len;

        // Page ID.
        final long pageId = PageUtils.getLong(pageAddr, off);

        return new DirectoryItem(idxName, pageId);
    }

    /**
     *
     */
    private static class DirectoryTree extends BPlusTree<DirectoryItem, DirectoryItem> {
        /** Bytes in byte. */
        private static final int BYTE_LEN = 1;

        /**
         * @param pageMem Page memory.
         * @param metaPageId Meta page ID.
         * @param reuseList Reuse list.
         * @param innerIos Inner IOs.
         * @param leafIos Leaf IOs.
         * @throws IgniteCheckedException If failed.
         */
        private DirectoryTree(
            String name,
            final int cacheId,
            final PageMemory pageMem,
            final IgniteWriteAheadLogManager wal,
            final AtomicLong globalRmvId,
            final long metaPageId,
            final ReuseList reuseList,
            final IOVersions<? extends BPlusInnerIO<DirectoryItem>> innerIos,
            final IOVersions<? extends BPlusLeafIO<DirectoryItem>> leafIos,
            final boolean initNew
        ) throws IgniteCheckedException {
            super(name, cacheId, pageMem, wal, globalRmvId, metaPageId, reuseList, innerIos, leafIos);

            initTree(initNew);
        }

        /** {@inheritDoc} */
        @Override protected int compare(final BPlusIO<DirectoryItem> io, final long pageAddr, final int idx,
            final DirectoryItem row) throws IgniteCheckedException {
            final int off = ((DirectoryIO)io).getOffset(pageAddr, idx);

            int shift = 0;

            // Compare index names.
            final byte len = PageUtils.getByte(pageAddr, off + shift);

            shift += BYTE_LEN;

            byte[] fileName = row.getFileName();

            for (int i = 0; i < len && i < fileName.length; i++) {
                final int cmp = Byte.compare(PageUtils.getByte(pageAddr, off + i + shift), fileName[i]);

                if (cmp != 0)
                    return cmp;
            }

            return Integer.compare(len, fileName.length);
        }

        /** {@inheritDoc} */
        @Override protected DirectoryItem getRow(final BPlusIO<DirectoryItem> io, final long pageAddr,
            final int idx, Object ignore) throws IgniteCheckedException {
            return io.getLookupRow(this, pageAddr, idx);
        }

    }

    /**
     *
     */
    private interface DirectoryIO {
        /**
         * @param pageAddr Page address.
         * @param idx Index.
         * @return Offset in buffer according to {@code idx}.
         */
        int getOffset(long pageAddr, int idx);
    }

    /**
     *
     */
    private static class LuceneDirectoryInnerIO extends BPlusInnerIO<DirectoryItem> implements DirectoryIO {
        /** */
        public static final IOVersions<LuceneDirectoryInnerIO> VERSIONS = new IOVersions<>(
            new LuceneDirectoryInnerIO(1)
        );

        /**
         * @param ver Version.
         */
        public LuceneDirectoryInnerIO(int ver) {
            super(T_LUCENE_REF_INNER, ver, false, MAX_FILENAME_LEN + 1 + 8);
        }

        /** {@inheritDoc} */
        @Override public void storeByOffset(long pageAddr, int off, DirectoryItem row) throws IgniteCheckedException {
            storeRow(pageAddr, off, row);
        }

        /** {@inheritDoc} */
        @Override public void store(long dstPageAddr, int dstIdx, BPlusIO<DirectoryItem> srcIo, long srcPageAddr,
            int srcIdx) throws IgniteCheckedException {
            storeRow(dstPageAddr, offset(dstIdx), srcPageAddr, ((DirectoryIO)srcIo).getOffset(srcPageAddr, srcIdx));
        }

        /** {@inheritDoc} */
        @Override public DirectoryItem getLookupRow(BPlusTree<DirectoryItem, ?> tree, long pageAddr,
            int idx) throws IgniteCheckedException {
            return readRow(pageAddr, offset(idx));
        }

        /** {@inheritDoc} */
        @Override public int getOffset(long pageAddr, int idx) {
            return offset(idx);
        }
    }

    /**
     *
     */
    private static class LuceneDirectoryLeafIO extends BPlusLeafIO<DirectoryItem> implements DirectoryIO {
        /** */
        public static final IOVersions<LuceneDirectoryLeafIO> VERSIONS = new IOVersions<>(
            new LuceneDirectoryLeafIO(1)
        );

        /**
         * @param ver Version.
         */
        public LuceneDirectoryLeafIO(int ver) {
            super(T_LUCENE_REF_LEAF, ver, MAX_FILENAME_LEN + 1 + 8);
        }

        /** {@inheritDoc} */
        @Override public void storeByOffset(long pageAddr, int off, DirectoryItem row) throws IgniteCheckedException {
            storeRow(pageAddr, off, row);
        }

        /** {@inheritDoc} */
        @Override public void store(long dstPageAddr, int dstIdx, BPlusIO<DirectoryItem> srcIo, long srcPageAddr,
            int srcIdx) throws IgniteCheckedException {
            storeRow(dstPageAddr, offset(dstIdx), srcPageAddr, ((DirectoryIO)srcIo).getOffset(srcPageAddr, srcIdx));
        }

        /** {@inheritDoc} */
        @Override public DirectoryItem getLookupRow(BPlusTree<DirectoryItem, ?> tree, long pageAddr,
            int idx) throws IgniteCheckedException {
            return readRow(pageAddr, offset(idx));
        }

        /** {@inheritDoc} */
        @Override public int getOffset(long pageAddr, int idx) {
            return offset(idx);
        }
    }
}
