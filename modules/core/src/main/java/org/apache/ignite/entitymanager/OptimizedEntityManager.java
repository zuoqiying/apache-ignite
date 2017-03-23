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

package org.apache.ignite.entitymanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.cache.Cache;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiClosure;

import java.util.BitSet;
import java.util.Map;

/**
 * OptimizedEntityManager works only with numeric monotonous keys.
 */
public class OptimizedEntityManager<V> extends EntityManager<Long, V> {
    /**
     * Segment capacity.
     */
    public static final int CAPACITY = 8192; // Compressed size fits in 2k page.

    /**
     * @param name   Name.
     * @param indices Indices.
     */
    public OptimizedEntityManager(int parts, String name, Map<String, IgniteBiClosure<StringBuilder, Object, String>> indices) {
        super(parts, name, indices, new SequenceIdGenerator());
    }

    /** {@inheritDoc} */
    public boolean contains(String idxName, Object val, Long id) {
        long seg = id / CAPACITY;

        int off = (int) (id % CAPACITY);

        IgniteBiClosure<StringBuilder, Object, String> clo = incices.get(idxName);

        String strVal = clo.apply(builder(), val);

        IndexFieldKey idxKey = new IndexFieldKey(strVal, seg);

        IndexFieldValue idxVal = indexCache(idxName).get(idxKey);

        return idxVal != null && ((BitSet)idxVal.getValue()).get(off);
    }

    /** {@inheritDoc} */
    protected void addEntry(Session ses, Long key, Map<String, String> changes) {
        long seg = key / CAPACITY;

        int off = (int) (key % CAPACITY);

        for (Map.Entry<String, String> change : changes.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(change.getValue(), seg);

            String idxCacheName = indexCacheName(change.getKey());

            Map<IndexFieldKey, IndexFieldValue> map = ses.getAdditions(idxCacheName);

            IgniteCache<IndexFieldKey, IndexFieldValue> cache = ignite.cache(idxCacheName);

            IndexFieldValue idxVal = map.get(idxKey);

            // Load latest stored value.
            if (idxVal == null)
                map.put(idxKey, (idxVal = cache.get(idxKey)));

            if (idxVal == null)
                map.put(idxKey, (idxVal = new IndexFieldValue(new BitSet())));

            ((BitSet)idxVal.getValue()).set(off);
        }
    }

    /** {@inheritDoc} */
    protected void removeEntry(Session ses, Long key, Map<String, String> changes) {
        long seg = key / CAPACITY;

        int off = (int) (key % CAPACITY);

        for (Map.Entry<String, String> change : changes.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(change.getValue(), seg);

            String idxCacheName = indexCacheName(change.getKey());

            Map<IndexFieldKey, IndexFieldValue> map = ses.getRemovals(idxCacheName);

            IgniteCache<IndexFieldKey, IndexFieldValue> cache = indexCache(change.getKey());

            IndexFieldValue idxVal = map.get(idxKey);

            // Load latest value.
            if (idxVal == null)
                idxVal = cache.get(idxKey);

            if (idxVal == null)
                continue;
            else
                map.put(idxKey, idxVal);

            BitSet set = (BitSet) idxVal.getValue();

            assert set.cardinality() > 0;

            set.clear(off);

            if (set.cardinality() == 0)
                map.put(idxKey, null); // Mark for removal.
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<T2<Long, V>> findAll(V example, String idxName) {
        if (incices.isEmpty())
            return Collections.EMPTY_LIST;

        IgniteBiClosure<StringBuilder, Object, String> clo = incices.get(idxName);

        String strVal = clo.apply(builder(), example);

        SqlQuery<IndexFieldKey, IndexFieldValue> sqlQry = new SqlQuery<>(IndexFieldValue.class, "fieldValue = ?");

        sqlQry.setArgs(strVal);

        // TODO set partition when IGNITE-4523 will be ready.

        QueryCursor<Cache.Entry<IndexFieldKey, IndexFieldValue>> cur = indexCache(idxName).query(sqlQry);

        List<Cache.Entry<IndexFieldKey, IndexFieldValue>> rows = U.arrayList(cur, 16);

        List<T2<Long, V>> ret = new ArrayList<>();

        for (Cache.Entry<IndexFieldKey, IndexFieldValue> row : rows) {
            BitSet val = (BitSet)row.getValue().getValue();

            Long seg = (Long)row.getKey().getPayload();

            if (val != null) {
                int id = -1;

                while(id != Integer.MAX_VALUE && (id = val.nextSetBit(id + 1)) != -1) {
                    long entityId = seg * CAPACITY + id;

                    V v = entityCache().get(entityId);

                    ret.add(new T2<>(entityId, v));
                }
            }
        }

        return ret;
    }
}