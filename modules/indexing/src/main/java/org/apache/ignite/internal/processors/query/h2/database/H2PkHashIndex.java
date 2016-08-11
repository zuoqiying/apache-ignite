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

package org.apache.ignite.internal.processors.query.h2.database;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.cache.CacheObject;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.IgniteCacheOffheapManager;
import org.apache.ignite.internal.processors.cache.database.IgniteCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cache.database.RootPage;
import org.apache.ignite.internal.processors.cache.database.tree.BPlusTree;
import org.apache.ignite.internal.processors.cache.database.tree.io.BPlusIO;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2IndexBase;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2Row;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2Table;
import org.apache.ignite.internal.util.lang.GridCursor;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.spi.indexing.IndexingQueryFilter;
import org.h2.engine.Session;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.index.SingleRowCursor;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;

public class H2PkHashIndex extends GridH2IndexBase {

    private final IgniteCacheOffheapManager offheapMgr;

    /**
     * @param cctx Cache context.
     * @param keyCol Key column.
     * @param valCol Value column.
     * @param tbl Table.
     * @param name Index name.
     * @param pk Primary key.
     * @param cols Index columns.
     * @throws IgniteCheckedException If failed.
     */
    public H2PkHashIndex(
        GridCacheContext<?, ?> cctx,
        int keyCol,
        int valCol,
        GridH2Table tbl,
        String name,
        boolean pk,
        IndexColumn[] cols
    ) throws IgniteCheckedException {
        super(keyCol, valCol);

        assert pk;

        // TODO: pass wrapper around per-partition hash indexes as base index.

        initBaseIndex(tbl, 0, name, cols, IndexType.createPrimaryKey(false, true));

        offheapMgr = cctx.offheap();
    }

    /** {@inheritDoc} */
    @Override public Cursor find(Session ses, SearchRow lower, SearchRow upper) {
        throw DbException.getUnsupportedException("find");
    }

    /** {@inheritDoc} */
    @Override public GridH2Row findOne(GridH2Row row) {
        try {
            for (IgniteCacheOffheapManager.CacheDataStore store : offheapMgr.cacheDataStores()) {
                IgniteBiTuple<CacheObject, GridCacheVersion> found = store.find(row.key);

                if (found != null) {
                    GridH2Row ret = new GridH2Row();

                    ret.key = row.key;
                    ret.partId = row.partId;
                    ret.val = found.get1();
                    ret.ver = found.get2();

                    return ret;
                }
            }

            return null;
        }
        catch (IgniteCheckedException e) {
            throw DbException.convert(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override public GridH2Row put(GridH2Row row) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public GridH2Row remove(SearchRow row) {
        if (row instanceof GridH2Row)
            return (GridH2Row) row;

        return null;
    }

    /** {@inheritDoc} */
    @Override public double getCost(Session ses, int[] masks, TableFilter filter, SortOrder sortOrder) {
        return getCostRangeIndex(masks, getRowCountApproximation(), filter, sortOrder);
    }

    /** {@inheritDoc} */
    @Override public long getRowCount(Session ses) {
        Cursor cursor = find(ses, null, null);

        long res = 0;

        while (cursor.next())
            res++;

        return res;
    }

    /** {@inheritDoc} */
    @Override public long getRowCountApproximation() {
        return 10_000; // TODO
    }

    /** {@inheritDoc} */
    @Override public boolean canGetFirstOrLast() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public Cursor findFirstOrLast(Session session, boolean b) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public void close(Session ses) {
    }

    /**
     * Cursor.
     */
    private static class H2Cursor implements Cursor {
        /** */
        final GridCursor<GridH2Row> cursor;

        /** */
        final IgniteBiPredicate<Object, Object> filter;

        /**
         * @param cursor Cursor.
         * @param filter Filter.
         */
        private H2Cursor(GridCursor<GridH2Row> cursor, IgniteBiPredicate<Object, Object> filter) {
            assert cursor != null;

            this.cursor = cursor;
            this.filter = filter;
        }

        /** {@inheritDoc} */
        @Override public Row get() {
            try {
                return cursor.get();
            }
            catch (IgniteCheckedException e) {
                throw DbException.convert(e);
            }
        }

        /** {@inheritDoc} */
        @Override public SearchRow getSearchRow() {
            return get();
        }

        /** {@inheritDoc} */
        @Override public boolean next() {
            try {
                while (cursor.next()) {
                    if (filter == null)
                        return true;

                    GridH2Row row = cursor.get();

                    Object key = row.getValue(0).getObject();
                    Object val = row.getValue(1).getObject();

                    assert key != null;
                    assert val != null;

                    if (filter.apply(key, val))
                        return true;
                }

                return false;
            }
            catch (IgniteCheckedException e) {
                throw DbException.convert(e);
            }
        }

        /** {@inheritDoc} */
        @Override public boolean previous() {
            throw DbException.getUnsupportedException("previous");
        }
    }
}
