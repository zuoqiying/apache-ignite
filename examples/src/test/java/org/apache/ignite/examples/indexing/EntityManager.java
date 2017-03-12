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
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import javax.cache.Cache;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The <code>EntityManager</code> which support only manual ids assignment.
 */
public class EntityManager<K, V> {
    /** */
    private ThreadLocal<StringBuilder> builder = new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };

    /** */
    protected final String name;

    /** */
    protected final Map<String, IgniteClosure<Object, String>> incices;

    /** */
    protected Ignite ignite;

    /**
     * @param name      Name.
     * @param incices Fields map.
     */
    public EntityManager(String name, Map<String, IgniteClosure<Object, String>> incices) {
        this.name = name;

        this.incices = incices == null ? Collections.<String, IgniteClosure<Object, String>>emptyMap() : incices;
    }

    /**
     * Returns cache configurations.
     */
    public CacheConfiguration[] cacheConfigurations() {
        CacheConfiguration[] ccfgs = new CacheConfiguration[incices.size() + 1];

        int c = 0;

        ccfgs[c] = new CacheConfiguration(entityCacheName());
        ccfgs[c].setCacheMode(CacheMode.REPLICATED);
        ccfgs[c].setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        c++;

        for (Map.Entry<String, IgniteClosure<Object, String>> idx : incices.entrySet()) {
            String idxName = idx.getKey();
            ccfgs[c] = new CacheConfiguration(indexCacheName(idxName));
            ccfgs[c].setCacheMode(CacheMode.REPLICATED);
            ccfgs[c].setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
            ccfgs[c].setIndexedTypes(IndexFieldKey.class, IndexFieldValue.class);

            c++;
        }

        return ccfgs;
    }

    /**
     * @param idxName Index name.
     */
    protected IgniteCache<IndexFieldKey, IndexFieldValue> indexCache(String idxName) {
        return ignite.getOrCreateCache(indexCacheName(idxName));
    }

    /**
     * @param idxName Index name.
     */
    protected String indexCacheName(String idxName) {
        return name + "_" + idxName;
    }

    /**
     * Attaches ignite instance to a manager.
     *
     * @param ignite Ignite.
     */
    public void attach(Ignite ignite) {
        this.ignite = ignite;
    }

    /**
     * @param key key;
     * @param val Value.
     * @return List of indexed fields.
     */
    protected IndexChange<K> indexChange(K key, V val) {
        IndexChange<K> idxChange = new IndexChange<>(name, key);

        for (Map.Entry<String, IgniteClosure<Object, String>> idx : incices.entrySet())
            idxChange.addChange(idx.getKey(), idx.getValue().apply(val));

        return idxChange;
    }

    public V get(K key) {
        return (V) entityCache().get(key);
    }

    public K save(K key, V val) {
        try (Transaction tx = ignite.transactions().txStart(TransactionConcurrency.OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
            V oldVal = null;

            IndexChange<K> oldChange = null;

            if (key == null)
                key = nextKey(); // New value.
            else {
                oldVal = get(key);

                if (oldVal != null)
                    oldChange = indexChange(key, oldVal);
            }

            // Merge indices.
            final IndexChange<K> newChange = indexChange(key, val);

            IgnitePredicate<String> exclSameValsPred = null;

            if (oldChange != null) {
                final IndexChange<K> finalOldChange = oldChange;

                exclSameValsPred = new IgnitePredicate<String>() {
                    @Override public boolean apply(String s) {
                        return !finalOldChange.changes().get(s).equals(newChange.changes().get(s));
                    }
                };

                // Remove only changed index values.
                removeEntry(key, F.view(oldChange.changes(), exclSameValsPred));
            }

            // Insert only changed index values.
            addEntry(key, exclSameValsPred == null ? newChange.changes() : F.view(newChange.changes(), exclSameValsPred));

            entityCache().put(key, val);

            tx.commit();

            return key;
        }
    }

    protected K nextKey() {
        throw new IllegalArgumentException();
    }

    public boolean delete(K key) {
        V v = get(key);

        if (v == null)
            return false;

        IndexChange<K> indexChange = indexChange(key, v);

        removeEntry(indexChange.id(), indexChange.changes());

        entityCache().remove(key);

        return true;
    }

    /** */
    protected void addEntry(K key, Map<String, String> changes) {
        if (changes == null)
            return;

        for (Map.Entry<String, String> change : changes.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(change.getValue(), key);

            indexCache(change.getKey()).put(idxKey, IndexFieldValue.MARKER);
        }
    }

    /** */
    protected void removeEntry(K key, Map<String, String> changes) {
        if (changes == null)
            return;

        for (Map.Entry<String, String> change : changes.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(change.getValue(), key);

            indexCache(change.getKey()).remove(idxKey);
        }
    }

    /**
     * @param idxName Field.
     * @param val   Value.
     * @param id    Id.
     */
    public boolean contains(String idxName, Object val, K id) {
        IgniteClosure<Object, String> clo = incices.get(idxName);

        String strVal = clo.apply(val);

        IndexFieldKey idxKey = new IndexFieldKey(strVal, id);

        return indexCache(idxName).containsKey(idxKey);
    }

    /**
     * Returns all entities matching the example.
     *
     * @param val Value.
     * @param idxName Index name.
     *
     * @return Entities iterator.
     */
    @SuppressWarnings("unchecked")
    public Collection<T2<K, V>> findAll(V val, String idxName) {
        IgniteClosure<Object, String> clo = incices.get(idxName);

        String strVal = clo.apply(val);

        SqlQuery<IndexFieldKey, IndexFieldValue> sqlQry = new SqlQuery<>(IndexFieldValue.class, "fieldValue = ?");

        sqlQry.setArgs(strVal);

        // TODO set partition when IGNITE-4523 will be ready.

        QueryCursor<Cache.Entry<IndexFieldKey, IndexFieldValue>> cur = indexCache(idxName).query(sqlQry);

        List<Cache.Entry<IndexFieldKey, IndexFieldValue>> rows = U.arrayList(cur, 16);

        return F.viewReadOnly(rows, new IgniteClosure<Cache.Entry<IndexFieldKey, IndexFieldValue>, T2<K, V>>() {
            @Override public T2<K, V> apply(Cache.Entry<IndexFieldKey, IndexFieldValue> e) {
                Object entryKey = e.getKey().getPayload();

                return new T2(entryKey, entityCache().get((K) entryKey)); // TODO use batching
            }
        });
    }

    /** */
    private IgniteCache<K, V> entityCache() {
        return ignite.getOrCreateCache(entityCacheName());
    }

    /** */
    private String entityCacheName() {
        return name + "_entity";
    }

    /** */
    private StringBuilder builder() {
        StringBuilder builder = this.builder.get();

        builder.setLength(0);

        return builder;
    }

    /**
     * @param field Field.
     */
    public int indexSize(String field) {
        return indexCache(field).size();
    }
}