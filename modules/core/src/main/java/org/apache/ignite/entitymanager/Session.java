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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import javax.cache.Cache;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

/**
 * Batch.
 */
public class Session implements AutoCloseable {
    /** */
    private final Transaction transaction;

    /** */
    private static ThreadLocal<Session> sesHolder = new ThreadLocal<>();

    /** */
    Map<String, Map<IndexFieldKey, IndexFieldValue>> additions;

    /** */
    Map<String, Map<IndexFieldKey, IndexFieldValue>> removals;

    /** */
    private Ignite ignite;

    /** */
    public Session(Ignite ignite, TransactionConcurrency concurrency, TransactionIsolation isolation) {
        this.ignite = ignite;

        this.transaction = ignite.transactions().txStart(concurrency, isolation);

        additions = new HashMap<>();

        removals = new HashMap<>();
    }

    /** */
    public static Session newSession(Ignite ignite, TransactionConcurrency concurrency, TransactionIsolation isolation) {
        Session ses;

        sesHolder.set(ses = new Session(ignite, concurrency, isolation));

        return ses;
    }

    /** */
    static Session current() {
        return sesHolder.get();
    }

    /** */
    public void flush() {
        Iterator<Map.Entry<String, Map<IndexFieldKey, IndexFieldValue>>> iter = additions.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, Map<IndexFieldKey, IndexFieldValue>> entry = iter.next();

            String idxName = entry.getKey();

            IgniteCache<IndexFieldKey, IndexFieldValue> idxCache = ignite.cache(idxName).withKeepBinary();

            for (Map.Entry<IndexFieldKey, IndexFieldValue> changes : entry.getValue().entrySet())
                idxCache.put(changes.getKey(), changes.getValue());

            iter.remove(); // Freeing heap.
        }

        Iterator<Map.Entry<String, Map<IndexFieldKey, IndexFieldValue>>> iter2 = removals.entrySet().iterator();

        while (iter2.hasNext()) {
            Map.Entry<String, Map<IndexFieldKey, IndexFieldValue>> entry = iter2.next();

            String idxName = entry.getKey();

            IgniteCache<IndexFieldKey, IndexFieldValue> idxCache = ignite.cache(idxName).withKeepBinary();

            for (Map.Entry<IndexFieldKey, IndexFieldValue> valueEntry : entry.getValue().entrySet()) {
                IndexFieldKey idxKey = valueEntry.getKey();

                IndexFieldValue idxVal = valueEntry.getValue();

                if (idxVal.getValue() == null)
                    idxCache.remove(idxKey);
                else
                    idxCache.put(idxKey, idxVal);
            }

            iter2.remove();
        }

        transaction.commit();
    }

    /** {@inheritDoc} */
    @Override public void close() {
        transaction.close();
    }

    /** */
    public Map<IndexFieldKey, IndexFieldValue> getAdditions(String idxName) {
        Map<IndexFieldKey, IndexFieldValue> changes = additions.get(idxName);

        if (changes == null)
            additions.put(idxName, (changes = new HashMap<>()));

        return changes;
    }

    /** */
    public Map<IndexFieldKey, IndexFieldValue> getRemovals(String idxName) {
        Map<IndexFieldKey, IndexFieldValue> changes = removals.get(idxName);

        if (changes == null)
            removals.put(idxName, (changes = new HashMap<>()));

        return changes;
    }
}
