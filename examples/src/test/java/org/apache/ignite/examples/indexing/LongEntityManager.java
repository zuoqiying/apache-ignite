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

package org.apache.ignite.examples.indexing;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteClosure;

import java.util.BitSet;
import java.util.Map;

/**
 * <p>
 * The <code>LongEntityManager</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class LongEntityManager<V> extends EntityManager<Long, V> {
    /**
     * Segment capacity.
     */
    public static final int CAPACITY = 16_000; // Compressed size fits in 2k page.

    /** */
    private String seqName;

    /** */
    private IgniteAtomicSequence seq;

    /**
     * @param name   Name.
     * @param fields Fields.
     */
    public LongEntityManager(String name, Map<String, IgniteClosure<Object, String>> fieldsMap) {
        super(name, fieldsMap);

        this.seqName = name + "_seq";
    }

    /** {@inheritDoc} */
    @Override protected Long nextKey() {
        return seq.getAndIncrement();
    }

    @Override public void attach(Ignite ignite) {
        super.attach(ignite);

        seq = ignite.atomicSequence(seqName, 0, true);
    }

    public boolean contains(String field, String val, Long id) {
        long seg = id / CAPACITY;

        int off = (int) (id % CAPACITY);

        IndexFieldKey indexKey = new IndexFieldKey(val, id);

        BitSet set = (BitSet) indexCache(field).get(indexKey);

        return set != null && set.get(off);
    }

    /** {@inheritDoc} */
    protected void addEntry(Long key, Map<String, String> fields) {
        long seg = key / CAPACITY;

        int off = (int) (key % CAPACITY);

        for (Map.Entry<String, String> field : fields.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(field.getValue(), seg);

            IgniteCache<IndexFieldKey, Object> cache = indexCache(field.getKey());

            BitSet set = (BitSet) cache.get(idxKey);

            if (set == null)
                set = new BitSet();

            set.set(off);

            cache.put(idxKey, set);
        }
    }

    /** {@inheritDoc} */
    protected void removeEntry(Long key, Map<String, String> fields) {
        long seg = key / CAPACITY;

        int off = (int) (key % CAPACITY);

        for (Map.Entry<String, String> field : fields.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(field.getValue(), seg);

            IgniteCache<IndexFieldKey, Object> cache = indexCache(field.getKey());

            BitSet set = (BitSet) cache.get(idxKey);

            if (set == null)
                continue;

            if (set.cardinality() == 0)
                cache.remove(idxKey);
            else {
                set.clear(off);

                cache.put(idxKey, set);
            }
        }
    }
}