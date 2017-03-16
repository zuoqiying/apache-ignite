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
    public OptimizedEntityManager(String name, Map<String, IgniteBiClosure<StringBuilder, Object, String>> indices) {
        super(name, indices, new SequenceIdGenerator());
    }

    /** {@inheritDoc} */
    public boolean contains(String idxName, Object value, Long id) {
        long seg = id / CAPACITY;

        int off = (int) (id % CAPACITY);

        IgniteBiClosure<StringBuilder, Object, String> clo = incices.get(idxName);

        String strVal = clo.apply(builder(), value);

        IndexFieldKey idxKey = new IndexFieldKey(strVal, seg);

        IndexFieldValue idxVal = indexCache(idxName).get(idxKey);

        return idxVal != null && ((BitSet)idxVal.getValue()).get(off);
    }

    /** {@inheritDoc} */
    protected void addEntry(Long key, Map<String, String> changes) {
        long seg = key / CAPACITY;

        int off = (int) (key % CAPACITY);

        for (Map.Entry<String, String> field : changes.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(field.getValue(), seg);

            IgniteCache<IndexFieldKey, IndexFieldValue> cache = indexCache(field.getKey());

            IndexFieldValue idxVal = cache.get(idxKey);

            if (idxVal == null)
                idxVal = new IndexFieldValue(new BitSet());

            ((BitSet)idxVal.getValue()).set(off);

            cache.put(idxKey, idxVal);
        }
    }

    /** {@inheritDoc} */
    protected void removeEntry(Long key, Map<String, String> changes) {
        long seg = key / CAPACITY;

        int off = (int) (key % CAPACITY);

        for (Map.Entry<String, String> field : changes.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(field.getValue(), seg);

            IgniteCache<IndexFieldKey, IndexFieldValue> cache = indexCache(field.getKey());

            IndexFieldValue val = cache.get(idxKey);

            if (val == null)
                continue;

            BitSet set = (BitSet) val.getValue();

            if (set.cardinality() == 0)
                cache.remove(idxKey);
            else {
                set.clear(off);

                cache.put(idxKey, val);
            }
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