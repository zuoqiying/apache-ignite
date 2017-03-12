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
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import javax.cache.Cache;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
    protected final Map<String, IgniteClosure<Object, String>> fieldsMap;

    /** */
    protected Ignite ignite;

    /**
     * @param name      Name.
     * @param fieldsMap Fields map.
     */
    public EntityManager(String name, Map<String, IgniteClosure<Object, String>> fieldsMap) {
        this.name = name;

        this.fieldsMap = fieldsMap == null ? Collections.<String, IgniteClosure<Object, String>>emptyMap() : fieldsMap;
    }

    /**
     * Returns cache configurations.
     */
    public CacheConfiguration[] cacheConfigurations() {
        CacheConfiguration[] ccfgs = new CacheConfiguration[fieldsMap.size() + 1];

        int c = 0;

        ccfgs[c] = new CacheConfiguration(entityCacheName());
        ccfgs[c].setCacheMode(CacheMode.REPLICATED);
        ccfgs[c].setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        c++;

        for (Map.Entry<String, IgniteClosure<Object, String>> field : fieldsMap.entrySet()) {
            String field1 = field.getKey();
            ccfgs[c] = new CacheConfiguration(indexCacheName(field1));
            ccfgs[c].setCacheMode(CacheMode.REPLICATED);
            ccfgs[c].setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            c++;
        }

        return ccfgs;
    }

    /**
     * @param field Field.
     */
    protected IgniteCache<IndexFieldKey, Object> indexCache(String field) {
        return ignite.getOrCreateCache(indexCacheName(field));
    }

    /**
     * @param field Field.
     */
    protected String indexCacheName(String field) {
        return name + "_" + field;
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
    protected IndexedFields<K> indexedFields(K key, V val) {
        IndexedFields<K> indexedFields = new IndexedFields<>(name, key);

        for (Map.Entry<String, IgniteClosure<Object, String>> field : fieldsMap.entrySet())
            indexedFields.addField(field.getKey(), field.getValue().apply(val));

        return indexedFields;
    }

    public V get(K key) {
        return (V) entityCache().get(key);
    }

    public K save(K key, V val) {
        try (Transaction tx = ignite.transactions().txStart(TransactionConcurrency.OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
            V oldVal = null;

            IndexedFields<K> oldFields = null;

            if (key == null)
                key = nextKey(); // New value.
            else {
                oldVal = get(key);

                if (oldVal != null)
                    oldFields = indexedFields(key, oldVal);
            }

            // Merge indices.
            final IndexedFields<K> newFields = indexedFields(key, val);

            IgnitePredicate<String> exclSameFieldsPred = null;

            if (oldFields != null) {
                final IndexedFields<K> finalOldFields = oldFields;

                exclSameFieldsPred = new IgnitePredicate<String>() {
                    @Override public boolean apply(String s) {
                        return !finalOldFields.fields().get(s).equals(newFields.fields().get(s));
                    }
                };

                // Remove only changed index fields.
                removeEntry(key, F.view(oldFields.fields(), exclSameFieldsPred));
            }

            // Insert only changed index fields.
            addEntry(key, exclSameFieldsPred == null ? newFields.fields() : F.view(newFields.fields(), exclSameFieldsPred));

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

        IndexedFields<K> indexedFields = indexedFields(key, v);

        removeEntry(indexedFields.id(), indexedFields.fields());

        entityCache().remove(key);

        return true;
    }

    /** */
    protected void addEntry(K key, Map<String, String> fields) {
        if (fields == null)
            return;

        for (Map.Entry<String, String> field : fields.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(field.getValue(), key);

            indexCache(field.getKey()).put(idxKey, null);
        }
    }

    /** */
    protected void removeEntry(K key, Map<String, String> fields) {
        if (fields == null)
            return;

        for (Map.Entry<String, String> field : fields.entrySet()) {
            IndexFieldKey idxKey = new IndexFieldKey(field.getValue(), key);

            indexCache(field.getKey()).remove(idxKey);
        }
    }

    /**
     * @param field Field.
     * @param val   Value.
     * @param id    Id.
     */
    public boolean contains(String field, String val, K id) {
        IndexFieldKey idxKey = new IndexFieldKey(val, id);

        return indexCache(field).containsKey(idxKey);
    }

    /**
     * Returns all entities matching the example.
     *
     * @param val Value.
     */
    @SuppressWarnings("unchecked")
    public Iterator<V> findByFieldValue(V val, String fieldName) {
        IgniteClosure<Object, String> clo = fieldsMap.get(fieldName);

        String strVal = clo.apply(val);

        SqlQuery<IndexFieldKey, V> sqlQry = new SqlQuery<>(Object.class, "fieldValue = ?");

        sqlQry.setArgs(strVal);

        // TODO set partition when IGNITE-4523 will be ready.

        QueryCursor<Cache.Entry<IndexFieldKey, V>> cur = indexCache(fieldName).query(sqlQry);

        return F.iterator(cur.iterator(), new IgniteClosure<Cache.Entry<IndexFieldKey, V>, V>() {
            @Override public V apply(Cache.Entry<IndexFieldKey, V> e) {
                Object entryKey = e.getKey().getPayload();

                return entityCache().get((K) entryKey); // TODO use batching
            }
        }, true);
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