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

package org.apache.ignite.internal.processors.cache;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import javax.cache.Cache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.pagemem.FullPageId;
import org.apache.ignite.internal.pagemem.Page;
import org.apache.ignite.internal.pagemem.PageIdUtils;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.database.CacheDataRow;
import org.apache.ignite.internal.processors.cache.database.IgniteCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cache.database.RowStore;
import org.apache.ignite.internal.processors.cache.database.freelist.FreeList;
import org.apache.ignite.internal.processors.cache.database.tree.BPlusTree;
import org.apache.ignite.internal.processors.cache.database.tree.io.BPlusIO;
import org.apache.ignite.internal.processors.cache.database.tree.io.BPlusInnerIO;
import org.apache.ignite.internal.processors.cache.database.tree.io.BPlusLeafIO;
import org.apache.ignite.internal.processors.cache.database.tree.io.DataPageIO;
import org.apache.ignite.internal.processors.cache.database.tree.io.IOVersions;
import org.apache.ignite.internal.processors.cache.database.tree.reuse.ReuseList;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtInvalidPartitionException;
import org.apache.ignite.internal.processors.cache.query.GridCacheQueryManager;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.GridCloseableIteratorAdapter;
import org.apache.ignite.internal.util.lang.GridCloseableIterator;
import org.apache.ignite.internal.util.lang.GridCursor;
import org.apache.ignite.internal.util.lang.GridIterator;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiTuple;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.internal.pagemem.PageIdUtils.dwordsOffset;
import static org.apache.ignite.internal.pagemem.PageIdUtils.pageId;

/**
 *
 */
@SuppressWarnings("WeakerAccess")
public class IgniteCacheOffheapManager extends GridCacheManagerAdapter {
    /** */
    private CacheDataRowStore rowStore;

    /** */
    private CacheDataTree dataTree;

    /** */
    private final boolean indexingEnabled;

    /** */
    private FreeList freeList;

    /** */
    private ReuseList reuseList;

    /** */
    private PageMemory pageMem;

    /**
     * @param indexingEnabled {@code True} if indexing is enabled for cache.
     */
    public IgniteCacheOffheapManager(boolean indexingEnabled) {
        this.indexingEnabled = indexingEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void start0() throws IgniteCheckedException {
        super.start0();

        if (cctx.affinityNode()) {
            IgniteCacheDatabaseSharedManager dbMgr = cctx.shared().database();

            IgniteBiTuple<FullPageId, Boolean> page = dbMgr.meta().getOrAllocateForIndex(cctx.cacheId(), cctx.namexx());

            int cpus = Runtime.getRuntime().availableProcessors();

            pageMem = dbMgr.pageMemory();
            reuseList = new ReuseList(cctx.cacheId(), pageMem, cpus * 2, dbMgr.meta());
            freeList = new FreeList(cctx, reuseList);

            rowStore = new CacheDataRowStore(cctx, freeList);

            dataTree = new CacheDataTree(reuseList,
                rowStore,
                cctx,
                dbMgr.pageMemory(),
                page.get1(),
                page.get2());
        }
    }

    /**
     * @return Reuse list.
     */
    public ReuseList reuseList() {
        return reuseList;
    }

    /**
     * @return Free list.
     */
    public FreeList freeList() {
        return freeList;
    }

    /**
     * @param pageId Page ID.
     * @return Page.
     * @throws IgniteCheckedException If failed.
     */
    private Page page(long pageId) throws IgniteCheckedException {
        return pageMem.page(new FullPageId(pageId, cctx.cacheId()));
    }

    /**
     * @param key  Key.
     * @param val  Value.
     * @param ver  Version.
     * @param expireTime Expire time.
     * @param part Partition.
     * @throws IgniteCheckedException If failed.
     */
    public void update(KeyCacheObject key,
        CacheObject val,
        GridCacheVersion ver,
        long expireTime,
        int part) throws IgniteCheckedException {
        if (indexingEnabled) {
            GridCacheQueryManager qryMgr = cctx.queries();

            assert qryMgr.enabled();

            if (qryMgr.store(key, part, val, ver, expireTime))
                return;
        }

        DataRow dataRow = new DataRow(key.hashCode(), key, val, ver, part);

        rowStore.addRow(dataRow);

        dataTree.put(dataRow);
    }

    /**
     * @param key Key.
     * @param prevVal Previous value.
     * @param prevVer Previous version.
     * @param part Partition.
     * @throws IgniteCheckedException If failed.
     */
    public void remove(KeyCacheObject key, CacheObject prevVal, GridCacheVersion prevVer, int part)
        throws IgniteCheckedException {
        if (indexingEnabled) {
            GridCacheQueryManager qryMgr = cctx.queries();

            assert qryMgr.enabled();

            if (qryMgr.remove(key, part, prevVal, prevVer))
                return;
        }

        DataRow dataRow = dataTree.remove(new KeySearchRow(key.hashCode(), key, 0));

        if (dataRow != null) {
            assert dataRow.link != 0 : dataRow;

            rowStore.removeRow(dataRow.link);
        }
    }

    /**
     * @param key  Key to read.
     * @param part Partition.
     * @return Value tuple, if available.
     * @throws IgniteCheckedException If failed.
     */
    @SuppressWarnings("unchecked")
    @Nullable public IgniteBiTuple<CacheObject, GridCacheVersion> read(KeyCacheObject key, int part)
        throws IgniteCheckedException {
        if (indexingEnabled) {
            IgniteBiTuple<CacheObject, GridCacheVersion> t = cctx.queries().read(key, part);

            if (t != null)
                return t.get1() != null ? t : null;
        }

        DataRow dataRow = dataTree.findOne(new KeySearchRow(key.hashCode(), key, 0));

        return dataRow != null ? F.t(dataRow.value(), dataRow.version()) : null;
    }

    /**
     * TODO: GG-10884, used on only from initialValue.
     */
    public boolean containsKey(KeyCacheObject key, int part) {
        try {
            return read(key, part) != null;
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to read value", e);

            return false;
        }
    }

    /**
     * Clears offheap entries.
     *
     * @param readers {@code True} to clear readers.
     */
    @SuppressWarnings("unchecked")
    public void clear(boolean readers) {
        if (dataTree != null)
            clear(dataTree, readers);

        if (indexingEnabled) {
            BPlusTree<?, ? extends CacheDataRow> idx = cctx.queries().pkIndex();

            if (idx != null)
                clear(idx, readers);
        }
    }

    /**
     * @param ldr Class loader.
     * @return Number of undeployed entries.
     */
    public int onUndeploy(ClassLoader ldr) {
        // TODO: GG-11141.
        return 0;
    }

    /**
     * @param tree    Tree.
     * @param readers {@code True} to clear readers.
     */
    private void clear(BPlusTree<?, ? extends CacheDataRow> tree, boolean readers) {
        try {
            GridCursor<? extends CacheDataRow> cur = tree.find(null, null);

            Collection<KeyCacheObject> keys = new ArrayList<>();

            while (cur.next()) {
                CacheDataRow row = cur.get();

                keys.add(row.key());
            }

            GridCacheVersion obsoleteVer = null;

            for (KeyCacheObject key : keys) {
                try {
                    if (obsoleteVer == null)
                        obsoleteVer = cctx.versions().next();

                    GridCacheEntryEx entry = cctx.cache().entryEx(key);

                    entry.clear(obsoleteVer, readers);
                }
                catch (GridDhtInvalidPartitionException ignore) {
                    // Ignore.
                }
                catch (IgniteCheckedException e) {
                    U.error(log, "Failed to clear cache entry: " + key, e);
                }
            }
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to clear cache entries.", e);
        }
    }

    /**
     * @param part Partition.
     * @return Number of entries in given partition.
     */
    public long entriesCount(int part) {
        return 0;
    }

    /**
     * @param primary Include primary node keys.
     * @param backup Include backup node keys.
     * @param topVer Topology version.
     * @return Entries count.
     * @throws IgniteCheckedException If failed.
     */
    @SuppressWarnings("unchecked")
    public long entriesCount(boolean primary, boolean backup, AffinityTopologyVersion topVer)
        throws IgniteCheckedException {
        long cnt = 0;

        if (dataTree != null)
            cnt += entriesCount(dataTree, primary, backup, topVer);

        if (indexingEnabled) {
            BPlusTree<?, ? extends CacheDataRow> idx = cctx.queries().pkIndex();

            if (idx != null)
                cnt += entriesCount(idx, primary, backup, topVer);
        }

        return cnt;
    }

    /**
     * @param tree    Tree.
     * @param primary Include primary node keys.
     * @param backup  Include backup node keys.
     * @param topVer  Topology version.
     * @return Entries count.
     * @throws IgniteCheckedException If failed.
     */
    private long entriesCount(BPlusTree<?, ? extends CacheDataRow> tree,
        boolean primary,
        boolean backup,
        AffinityTopologyVersion topVer) throws IgniteCheckedException {
        GridCursor<? extends CacheDataRow> cur = tree.find(null, null);

        ClusterNode locNode = cctx.localNode();

        long cnt = 0;

        while (cur.next()) {
            CacheDataRow row = cur.get();

            if (primary) {
                if (cctx.affinity().primary(locNode, row.partition(), topVer)) {
                    cnt++;

                    continue;
                }
            }

            if (backup) {
                if (cctx.affinity().backup(locNode, row.partition(), topVer))
                    cnt++;
            }
        }

        return cnt;
    }

    public long offHeapAllocatedSize() {
        // TODO GG-10884.
        return 0;
    }

    // TODO GG-10884: moved from GridCacheSwapManager.
    void writeAll(Iterable<GridCacheBatchSwapEntry> swapped) throws IgniteCheckedException {
        // No-op.
    }

    /**
     * @param primary {@code True} if need return primary entries.
     * @param backup {@code True} if need return backup entries.
     * @param topVer Topology version to use.
     * @return Entries iterator.
     * @throws IgniteCheckedException If failed.
     */
    @SuppressWarnings("unchecked")
    public <K, V> GridCloseableIterator<Cache.Entry<K, V>> entriesIterator(final boolean primary,
        final boolean backup,
        final AffinityTopologyVersion topVer,
        final boolean keepBinary)
        throws IgniteCheckedException {
        final GridCursor<CacheDataRow> cur = cursor();

        return new GridCloseableIteratorAdapter<Cache.Entry<K, V>>() {
            /** */
            private CacheEntryImplEx next;

            @Override protected Cache.Entry<K, V> onNext() throws IgniteCheckedException {
                CacheEntryImplEx ret = next;

                next = null;

                return ret;
            }

            @Override protected boolean onHasNext() throws IgniteCheckedException {
                if (next != null)
                    return true;

                CacheDataRow nextRow = null;

                while (cur.next()) {
                    CacheDataRow row = cur.get();

                    boolean pass;

                    if (primary && backup)
                        pass = true;
                    else if (primary)
                        pass = cctx.affinity().primary(cctx.localNode(), row.partition(), topVer);
                    else
                        pass = cctx.affinity().backup(cctx.localNode(), row.partition(), topVer);

                    if (pass) {
                        nextRow = row;

                        break;
                    }
                }

                if (nextRow != null) {
                    KeyCacheObject key = nextRow.key();
                    CacheObject val = nextRow.value();

                    Object key0 = cctx.unwrapBinaryIfNeeded(key, keepBinary, false);
                    Object val0 = cctx.unwrapBinaryIfNeeded(val, keepBinary, false);

                    next = new CacheEntryImplEx(key0, val0, nextRow.version());

                    return true;
                }

                return false;
            }
        };
    }

    /**
     * @return Iterator.
     * @throws IgniteCheckedException If failed.
     */
    public GridCloseableIterator<KeyCacheObject> keysIterator() throws IgniteCheckedException {
        final GridCursor<CacheDataRow> cur = cursor();

        return new GridCloseableIteratorAdapter<KeyCacheObject>() {
            /** */
            private KeyCacheObject next;

            @Override protected KeyCacheObject onNext() throws IgniteCheckedException {
                KeyCacheObject ret = next;

                next = null;

                return ret;
            }

            @Override protected boolean onHasNext() throws IgniteCheckedException {
                if (next != null)
                    return true;

                if (cur.next())
                    next = cur.get().key();

                return next != null;
            }
        };
    }

    /**
     * @param part Partition.
     * @return Iterator.
     * @throws IgniteCheckedException If failed.
     */
    public GridCloseableIterator<KeyCacheObject> keysIterator(final int part) throws IgniteCheckedException {
        final GridCursor<CacheDataRow> cur = cursor();

        return new GridCloseableIteratorAdapter<KeyCacheObject>() {
            /** */
            private KeyCacheObject next;

            @Override protected KeyCacheObject onNext() throws IgniteCheckedException {
                KeyCacheObject res = next;

                next = null;

                return res;
            }

            @Override protected boolean onHasNext() throws IgniteCheckedException {
                if (next != null)
                    return true;

                while (cur.next()) {
                    CacheDataRow row = cur.get();

                    if (row.partition() == part) {
                        next = row.key();

                        break;
                    }
                }

                return next != null;
            }
        };
    }

    public GridIterator<CacheDataRow> iterator(final boolean backups, final AffinityTopologyVersion topVer)
        throws IgniteCheckedException {
        final GridCursor<CacheDataRow> cur = cursor();

        return new GridCloseableIteratorAdapter<CacheDataRow>() {
            /** */
            private CacheDataRow next;

            @Override protected CacheDataRow onNext() throws IgniteCheckedException {
                CacheDataRow res = next;

                next = null;

                return res;
            }

            @Override protected boolean onHasNext() throws IgniteCheckedException {
                if (next != null)
                    return true;

                while (cur.next()) {
                    CacheDataRow row = cur.get();

                    if (backups || cctx.affinity().primary(cctx.localNode(), row.partition(), topVer)) {
                        next = row;

                        break;
                    }
                }

                return next != null;
            }
        };
    }

    public boolean empty(int part) throws IgniteCheckedException {
        return !iterator(part).hasNext();
    }

    public GridIterator<CacheDataRow> iterator(final int part) throws IgniteCheckedException {
        final GridCursor<CacheDataRow> cur = cursor();

        return new GridCloseableIteratorAdapter<CacheDataRow>() {
            /** */
            private CacheDataRow next;

            @Override protected CacheDataRow onNext() throws IgniteCheckedException {
                CacheDataRow res = next;

                next = null;

                return res;
            }

            @Override protected boolean onHasNext() throws IgniteCheckedException {
                if (next != null)
                    return true;

                while (cur.next()) {
                    CacheDataRow row = cur.get();

                    if (row.partition() == part) {
                        next = row;

                        break;
                    }
                }

                return next != null;
            }
        };
    }

    /**
     * @return Cursor.
     * @throws IgniteCheckedException If failed.
     */
    @SuppressWarnings("unchecked")
    private GridCursor<CacheDataRow> cursor() throws IgniteCheckedException {
        GridCursor<? extends CacheDataRow> cur1 = dataTree.find(null, null);

        GridCursor<? extends CacheDataRow> cur2 = indexingEnabled ? cctx.queries().pkIndex().find(null, null) : null;

        return cur2 != null ? new CompoundCursor(cur1, cur2) : cur1;
    }

    /**
     *
     */
    private static class CompoundCursor<T> implements GridCursor<T> {
        /** */
        private final GridCursor<T> c1;

        /** */
        private final GridCursor<T> c2;

        /** */
        private GridCursor<T> c;

        /**
         * @param c1 First cursor.
         * @param c2 Second cursor.
         */
        CompoundCursor(GridCursor<T> c1, GridCursor<T> c2) {
            this.c1 = c1;
            this.c2 = c2;

            c = c1;
        }

        /** {@inheritDoc} */
        @Override public boolean next() throws IgniteCheckedException {
            if (c.next())
                return true;

            if (c == c1) {
                c = c2;

                return c.next();
            }

            return false;
        }

        /** {@inheritDoc} */
        @Override public T get() throws IgniteCheckedException {
            return c.get();
        }
    }

    /**
     *
     */
    private class KeySearchRow {
        /** */
        int hash;

        /** */
        KeyCacheObject key;

        /** */
        long link;

        /**
         * @param hash Hash code.
         * @param key Key.
         * @param link Link.
         */
        KeySearchRow(int hash, KeyCacheObject key, long link) {
            this.hash = hash;
            this.key = key;
            this.link = link;
        }

        /**
         * @param buf Buffer.
         * @throws IgniteCheckedException If failed.
         */
        protected void doInitData(ByteBuffer buf) throws IgniteCheckedException {
            key = cctx.cacheObjects().toKeyCacheObject(cctx.cacheObjectContext(), buf);
        }

        /**
         * Init data.
         */
        protected final void initData() {
            if (key != null)
                return;

            assert link != 0;

            try (Page page = page(pageId(link))) {
                ByteBuffer buf = page.getForRead();

                try {
                    DataPageIO io = DataPageIO.VERSIONS.forPage(buf);

                    int dataOff = io.getDataOffset(buf, dwordsOffset(link));

                    buf.position(dataOff);

                    // Skip entry size.
                    buf.getShort();

                    doInitData(buf);
                }
                finally {
                    page.releaseRead();
                }
            }
            catch (IgniteCheckedException e) {
                throw new IgniteException(e);
            }
        }

        public KeyCacheObject key() {
            initData();

            return key;
        }

        /** {@inheritDoc} */
        public String toString() {
            return S.toString(KeySearchRow.class, this);
        }
    }

    /**
     *
     */
    private class DataRow extends KeySearchRow implements CacheDataRow {
        /** */
        private CacheObject val;

        /** */
        private GridCacheVersion ver;

        /** */
        private int part = -1;

        /**
         * @param hash Hash code.
         * @param link Link.
         */
        DataRow(int hash, long link) {
            super(hash, null, link);

            part = PageIdUtils.partId(link);
        }

        /**
         * @param hash Hash code.
         * @param key Key.
         * @param val Value.
         * @param ver Version.
         * @param part Partition.
         */
        DataRow(int hash, KeyCacheObject key, CacheObject val, GridCacheVersion ver, int part) {
            super(hash, key, 0);

            this.val = val;
            this.ver = ver;
            this.part = part;
        }

        /** {@inheritDoc} */
        @Override protected void doInitData(ByteBuffer buf) throws IgniteCheckedException {
            key = cctx.cacheObjects().toKeyCacheObject(cctx.cacheObjectContext(), buf);
            val = cctx.cacheObjects().toCacheObject(cctx.cacheObjectContext(), buf);

            int topVer = buf.getInt();
            int nodeOrderDrId = buf.getInt();
            long globalTime = buf.getLong();
            long order = buf.getLong();

            ver = new GridCacheVersion(topVer, nodeOrderDrId, globalTime, order);
        }

        /** {@inheritDoc} */
        @Override public CacheObject value() {
            initData();

            return val;
        }

        /** {@inheritDoc} */
        @Override public GridCacheVersion version() {
            initData();

            return ver;
        }

        /** {@inheritDoc} */
        @Override public int partition() {
            assert part != -1;

            return part;
        }

        /** {@inheritDoc} */
        @Override public long link() {
            return link;
        }

        /** {@inheritDoc} */
        @Override public void link(long link) {
            this.link = link;
        }

        /** {@inheritDoc} */
        public String toString() {
            return S.toString(DataRow.class, this);
        }
    }

    /**
     *
     */
    private static class CacheDataTree extends BPlusTree<KeySearchRow, DataRow> {
        /** */
        private final CacheDataRowStore rowStore;

        /** */
        private final GridCacheContext cctx;

        /**
         * @param reuseList Reuse list.
         * @param rowStore Row store.
         * @param cctx Context.
         * @param pageMem Page memory.
         * @param metaPageId Meta page ID.
         * @param initNew Initialize new index.
         * @throws IgniteCheckedException If failed.
         */
        CacheDataTree(
            ReuseList reuseList,
            CacheDataRowStore rowStore,
            GridCacheContext cctx,
            PageMemory pageMem,
            FullPageId metaPageId,
            boolean initNew) throws IgniteCheckedException {
            super(cctx.cacheId(), pageMem, metaPageId, reuseList, DataInnerIO.VERSIONS, DataLeafIO.VERSIONS);

            assert rowStore != null;

            this.rowStore = rowStore;
            this.cctx = cctx;

            if (initNew)
                initNew();
        }

        /** {@inheritDoc} */
        @Override protected int compare(BPlusIO<KeySearchRow> io, ByteBuffer buf, int idx, KeySearchRow row)
            throws IgniteCheckedException {
            KeySearchRow row0 = io.getLookupRow(this, buf, idx);

            int cmp = Integer.compare(row0.hash, row.hash);

            if (cmp != 0)
                return cmp;

            return compareKeys(row0.key(), row.key());
        }

        /** {@inheritDoc} */
        @Override protected DataRow getRow(BPlusIO<KeySearchRow> io, ByteBuffer buf, int idx)
            throws IgniteCheckedException {
            int hash = ((RowLinkIO)io).getHash(buf, idx);
            long link = ((RowLinkIO)io).getLink(buf, idx);

            return rowStore.dataRow(hash, link);
        }

        /**
         * @param key1 First key.
         * @param key2 Second key.
         * @return Compare result.
         * @throws IgniteCheckedException If failed.
         */
        private int compareKeys(CacheObject key1, CacheObject key2) throws IgniteCheckedException {
            byte[] bytes1 = key1.valueBytes(cctx.cacheObjectContext());
            byte[] bytes2 = key2.valueBytes(cctx.cacheObjectContext());

            int len = Math.min(bytes1.length, bytes2.length);

            for (int i = 0; i < len; i++) {
                byte b1 = bytes1[i];
                byte b2 = bytes2[i];

                if (b1 != b2)
                    return b1 > b2 ? 1 : -1;
            }

            return Integer.compare(bytes1.length, bytes2.length);
        }
    }

    /**
     *
     */
    private class CacheDataRowStore extends RowStore {
        /**
         * @param cctx Cache context.
         * @param freeList Free list.
         */
        CacheDataRowStore(GridCacheContext<?, ?> cctx, FreeList freeList) {
            super(cctx, freeList);
        }

        /**
         * @param hash Hash code.
         * @param link Link.
         * @return Search row.
         * @throws IgniteCheckedException If failed.
         */
        private KeySearchRow keySearchRow(int hash, long link) throws IgniteCheckedException {
            return new KeySearchRow(hash, null, link);
        }

        /**
         * @param hash Hash code.
         * @param link Link.
         * @return Data row.
         * @throws IgniteCheckedException If failed.
         */
        private DataRow dataRow(int hash, long link) throws IgniteCheckedException {
            return new DataRow(hash, link);
        }
    }

    /**
     *
     */
    private interface RowLinkIO {
        /**
         * @param buf Buffer.
         * @param idx Index.
         * @return Row link.
         */
        public long getLink(ByteBuffer buf, int idx);

        /**
         * @param buf Buffer.
         * @param idx Index.
         * @return Key hash code.
         */
        public int getHash(ByteBuffer buf, int idx);
    }

    /**
     *
     */
    private static class DataInnerIO extends BPlusInnerIO<KeySearchRow> implements RowLinkIO {
        /** */
        public static final IOVersions<DataInnerIO> VERSIONS = new IOVersions<>(
            new DataInnerIO(1)
        );

        /**
         * @param ver Page format version.
         */
        DataInnerIO(int ver) {
            super(T_DATA_REF_INNER, ver, true, 12);
        }

        /** {@inheritDoc} */
        @Override public void store(ByteBuffer buf, int idx, KeySearchRow row) {
            assert row.link != 0;

            setHash(buf, idx, row.hash);
            setLink(buf, idx, row.link);
        }

        /** {@inheritDoc} */
        @Override public KeySearchRow getLookupRow(BPlusTree<KeySearchRow,?> tree, ByteBuffer buf, int idx)
            throws IgniteCheckedException {
            int hash = getHash(buf, idx);
            long link = getLink(buf, idx);

            return ((CacheDataTree)tree).rowStore.keySearchRow(hash, link);
        }

        /** {@inheritDoc} */
        @Override public void store(ByteBuffer dst, int dstIdx, BPlusIO<KeySearchRow> srcIo, ByteBuffer src, int srcIdx) {
            int hash = ((RowLinkIO)srcIo).getHash(src, srcIdx);
            long link = ((RowLinkIO)srcIo).getLink(src, srcIdx);

            setHash(dst, dstIdx, hash);
            setLink(dst, dstIdx, link);
        }

        /** {@inheritDoc} */
        @Override public long getLink(ByteBuffer buf, int idx) {
            assert idx < getCount(buf): idx;

            return buf.getLong(offset(idx, SHIFT_LINK));
        }

        /**
         * @param buf Buffer.
         * @param idx Index.
         * @param link Link.
         */
        private void setLink(ByteBuffer buf, int idx, long link) {
            buf.putLong(offset(idx, SHIFT_LINK), link);

            assert getLink(buf, idx) == link;
        }


        /** {@inheritDoc} */
        @Override public int getHash(ByteBuffer buf, int idx) {
            return buf.getInt(offset(idx) + 8);
        }

        /**
         * @param buf Buffer.
         * @param idx Index.
         * @param hash Hash.
         */
        private void setHash(ByteBuffer buf, int idx, int hash) {
            buf.putInt(offset(idx) + 8, hash);
        }
    }

    /**
     *
     */
    private static class DataLeafIO extends BPlusLeafIO<KeySearchRow> implements RowLinkIO {
        /** */
        public static final IOVersions<DataLeafIO> VERSIONS = new IOVersions<>(
            new DataLeafIO(1)
        );

        /**
         * @param ver Page format version.
         */
        DataLeafIO(int ver) {
            super(T_DATA_REF_LEAF, ver, 12);
        }

        /** {@inheritDoc} */
        @Override public void store(ByteBuffer buf, int idx, KeySearchRow row) {
            DataRow row0 = (DataRow)row;

            assert row0.link != 0;

            setHash(buf, idx, row0.hash);
            setLink(buf, idx, row0.link);
        }

        /** {@inheritDoc} */
        @Override public void store(ByteBuffer dst, int dstIdx, BPlusIO<KeySearchRow> srcIo, ByteBuffer src, int srcIdx)
            throws IgniteCheckedException {
            setHash(dst, dstIdx, getHash(src, srcIdx));
            setLink(dst, dstIdx, getLink(src, srcIdx));
        }

        /** {@inheritDoc} */
        @Override public KeySearchRow getLookupRow(BPlusTree<KeySearchRow,?> tree, ByteBuffer buf, int idx)
            throws IgniteCheckedException {

            int hash = getHash(buf, idx);
            long link = getLink(buf, idx);

            return ((CacheDataTree)tree).rowStore.keySearchRow(hash, link);
        }

        /** {@inheritDoc} */
        @Override public long getLink(ByteBuffer buf, int idx) {
            assert idx < getCount(buf): idx;

            return buf.getLong(offset(idx));
        }

        /**
         * @param buf Buffer.
         * @param idx Index.
         * @param link Link.
         */
        private void setLink(ByteBuffer buf, int idx, long link) {
            buf.putLong(offset(idx), link);

            assert getLink(buf, idx) == link;
        }

        /** {@inheritDoc} */
        @Override public int getHash(ByteBuffer buf, int idx) {
            return buf.getInt(offset(idx) + 8);
        }

        /**
         * @param buf Buffer.
         * @param idx Index.
         * @param hash Hash.
         */
        private void setHash(ByteBuffer buf, int idx, int hash) {
            buf.putInt(offset(idx) + 8, hash);
        }
    }
}
