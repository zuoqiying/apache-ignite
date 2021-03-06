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

package org.apache.ignite.internal.processors.cache.distributed.replicated;

import org.apache.ignite.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.cache.query.*;
import org.apache.ignite.cache.query.annotations.*;
import org.apache.ignite.events.*;
import org.apache.ignite.internal.*;
import org.apache.ignite.internal.processors.cache.*;
import org.apache.ignite.internal.processors.cache.query.*;
import org.apache.ignite.internal.util.future.*;
import org.apache.ignite.internal.util.lang.*;
import org.apache.ignite.internal.util.typedef.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.apache.ignite.lang.*;
import org.apache.ignite.testframework.*;
import org.apache.ignite.transactions.*;
import org.springframework.util.*;

import javax.cache.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static org.apache.ignite.cache.CacheMode.*;
import static org.apache.ignite.events.EventType.*;

/**
 * Tests replicated query.
 */
public class IgniteCacheReplicatedQuerySelfTest extends IgniteCacheAbstractQuerySelfTest {
    /** */
    private static final boolean TEST_DEBUG = false;

    /** Grid1. */
    private static Ignite ignite1;

    /** Grid2. */
    private static Ignite ignite2;

    /** Grid3. */
    private static Ignite ignite3;

    /** Cache1. */
    private static IgniteCache<CacheKey, CacheValue> cache1;

    /** Cache2. */
    private static IgniteCache<CacheKey, CacheValue> cache2;

    /** Cache3. */
    private static IgniteCache<CacheKey, CacheValue> cache3;

    /** Key serialization cnt. */
    private static volatile int keySerCnt;

    /** Key deserialization count. */
    private static volatile int keyDesCnt;

    /** Value serialization count. */
    private static volatile int valSerCnt;

    /** Value deserialization count. */
    private static volatile int valDesCnt;

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected CacheMode cacheMode() {
        return REPLICATED;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        ignite1 = grid(0);
        ignite2 = grid(1);
        ignite3 = grid(2);

        cache1 = ignite1.jcache(null);
        cache2 = ignite2.jcache(null);
        cache3 = ignite3.jcache(null);
    }

    /**
     * @throws Exception If failed.
     */
    public void testClientOnlyNode() throws Exception {
        try {
            Ignite g = startGrid("client");

            IgniteCache<Integer, Integer> c = g.jcache(null);

            for (int i = 0; i < 10; i++)
                c.put(i, i);

            // Client cache should be empty.
            assertEquals(0, c.localSize());

            Collection<Cache.Entry<Integer, Integer>> res =
                c.query(new SqlQuery(Integer.class, "_key >= 5 order by _key")).getAll();

            assertEquals(5, res.size());

            int i = 5;

            for (Cache.Entry<Integer, Integer> e : res) {
                assertEquals(i, e.getKey().intValue());
                assertEquals(i, e.getValue().intValue());

                i++;
            }
        }
        finally {
            stopGrid("client");
        }
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testIterator() throws Exception {
        int keyCnt = 100;

        for (int i = 0; i < keyCnt; i++)
            cache1.put(new CacheKey(i), new CacheValue("val" + i));

        assertEquals(keyCnt, cache1.localSize());
        assertEquals(keyCnt, cache2.localSize());
        assertEquals(keyCnt, cache3.localSize());

        QueryCursor<Cache.Entry<CacheKey, CacheValue>> qry =
            cache1.query(new SqlQuery(CacheValue.class, "true"));

        Iterator<Cache.Entry<CacheKey, CacheValue>> iter = qry.iterator();

        assert iter.hasNext();

        int cnt = 0;

        while (iter.hasNext()) {
            iter.next();

            cnt++;
        }

        assertEquals(keyCnt, cnt);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testLocalQuery() throws Exception {
        cache1.clear();

        Transaction tx = ignite1.transactions().txStart();

        try {
            cache1.put(new CacheKey(1), new CacheValue("1"));
            cache1.put(new CacheKey(2), new CacheValue("2"));
            cache1.put(new CacheKey(3), new CacheValue("3"));
            cache1.put(new CacheKey(4), new CacheValue("4"));

            tx.commit();

            info("Committed transaction: " + tx);
        }
        catch (IgniteException e) {
            tx.rollback();

            throw e;
        }

        checkQueryResults(cache1);
        checkQueryResults(cache2);
        checkQueryResults(cache3);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testDistributedQuery() throws Exception {
        int keyCnt = 4;

        final CountDownLatch latch = new CountDownLatch(keyCnt * 2);

        IgnitePredicate<Event> lsnr = new IgnitePredicate<Event>() {
            @Override public boolean apply(Event evt) {
                latch.countDown();

                return true;
            }
        };

        ignite2.events().localListen(lsnr, EventType.EVT_CACHE_OBJECT_PUT);
        ignite3.events().localListen(lsnr, EventType.EVT_CACHE_OBJECT_PUT);

        Transaction tx = ignite1.transactions().txStart();

        try {
            for (int i = 1; i <= keyCnt; i++)
                cache1.put(new CacheKey(i), new CacheValue(String.valueOf(i)));

            tx.commit();

            info("Committed transaction: " + tx);
        }
        catch (IgniteException e) {
            tx.rollback();

            throw e;
        }

        latch.await();

        QueryCursor<Cache.Entry<CacheKey, CacheValue>> qry =
            cache1.query(new SqlQuery(CacheValue.class, "val > 1 and val < 4"));

        // Distributed query.
        assertEquals(2, qry.getAll().size());

        // Create new query, old query cannot be modified after it has been executed.
        qry = cache3.localQuery(new SqlQuery(CacheValue.class, "val > 1 and val < 4"));

        // Tests execute on node.
        Iterator<Cache.Entry<CacheKey, CacheValue>> iter = qry.iterator();

        assert iter != null;
        assert iter.hasNext();

        iter.next();

        assert iter.hasNext();

        iter.next();

        assert !iter.hasNext();
    }

    /**
     * Returns private field {@code qryIters} of {@link GridCacheQueryManager} for the given grid.
     *
     * @param g Grid which {@link GridCacheQueryManager} should be observed.
     * @return {@code qryIters} of {@link GridCacheQueryManager}.
     */
    private ConcurrentMap<UUID,
        Map<Long, GridFutureAdapter<GridCloseableIterator<IgniteBiTuple<CacheKey, CacheValue>>>>>
        distributedQueryManagerQueryItersMap(Ignite g) {
        GridCacheContext ctx = ((IgniteKernal)g).internalCache().context();

        Field qryItersField = ReflectionUtils.findField(ctx.queries().getClass(), "qryIters");

        qryItersField.setAccessible(true);

        return (ConcurrentMap<UUID,
            Map<Long, GridFutureAdapter<GridCloseableIterator<IgniteBiTuple<CacheKey, CacheValue>>>>>)
            ReflectionUtils.getField(qryItersField, ctx.queries());
    }

    /**
     * @throws Exception If test failed.
     */
    public void testToString() throws Exception {
        int keyCnt = 4;

        for (int i = 1; i <= keyCnt; i++)
            cache1.put(new CacheKey(i), new CacheValue(String.valueOf(i)));

        // Create query with key filter.

        QueryCursor<Cache.Entry<CacheKey, CacheValue>> qry =
            cache1.query(new SqlQuery(CacheValue.class, "val > 0"));

        assertEquals(keyCnt, qry.getAll().size());
    }

    /**
     * TODO
     *
     * @throws Exception If failed.
     */
    public void _testLostIterator() throws Exception {
        IgniteCache<Integer, Integer> cache = ignite.jcache(null);

        for (int i = 0; i < 1000; i++)
            cache.put(i, i);

        QueryCursor<Cache.Entry<Integer, Integer>> fut = null;

        for (int i = 0; i < GridCacheQueryManager.MAX_ITERATORS + 1; i++) {
            QueryCursor<Cache.Entry<Integer, Integer>> q =
                cache.query(new SqlQuery(Integer.class, "_key >= 0 order by _key"));

            assertEquals(0, (int)q.iterator().next().getKey());

            if (fut == null)
                fut = q;
        }

        final QueryCursor<Cache.Entry<Integer, Integer>> fut0 = fut;

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                int i = 0;

                Cache.Entry<Integer, Integer> e;

                while ((e = fut0.iterator().next()) != null)
                    assertEquals(++i, (int)e.getKey());

                return null;
            }
        }, IgniteException.class, null);
    }

    /**
     * TODO enable
     *
     * @throws Exception If failed.
     */
    public void _testNodeLeft() throws Exception {
        try {
            Ignite g = startGrid();

            IgniteCache<Integer, Integer> cache = g.jcache(null);

            for (int i = 0; i < 1000; i++)
                cache.put(i, i);

            QueryCursor<Cache.Entry<Integer, Integer>> q =
                cache.query(new SqlQuery(Integer.class, "_key >= 0 order by _key"));

            assertEquals(0, (int) q.iterator().next().getKey());

            final ConcurrentMap<UUID, Map<Long, GridFutureAdapter<GridCloseableIterator<
                IgniteBiTuple<Integer, Integer>>>>> map =
                U.field(((IgniteKernal)grid(0)).internalCache().context().queries(), "qryIters");

            // fut.nextX() does not guarantee the request has completed on remote node
            // (we could receive page from local one), so we need to wait.
            assertTrue(GridTestUtils.waitForCondition(new PA() {
                @Override public boolean apply() {
                    return map.size() == 1;
                }
            }, getTestTimeout()));

            Map<Long, GridFutureAdapter<GridCloseableIterator<IgniteBiTuple<Integer, Integer>>>> futs =
                map.get(g.cluster().localNode().id());

            assertEquals(1, futs.size());

            GridCloseableIterator<IgniteBiTuple<Integer, Integer>> iter =
                (GridCloseableIterator<IgniteBiTuple<Integer, Integer>>)((IgniteInternalFuture)F.first(futs.values()).get()).get();

            ResultSet rs = U.field(iter, "data");

            assertFalse(rs.isClosed());

            final UUID nodeId = g.cluster().localNode().id();
            final CountDownLatch latch = new CountDownLatch(1);

            grid(0).events().localListen(new IgnitePredicate<Event>() {
                @Override public boolean apply(Event evt) {
                    if (((DiscoveryEvent)evt).eventNode().id().equals(nodeId))
                        latch.countDown();

                    return true;
                }
            }, EVT_NODE_LEFT);

            stopGrid();

            latch.await();

            assertEquals(0, map.size());
            assertTrue(rs.isClosed());
        }
        finally {
            // Ensure that additional node is stopped.
            stopGrid();
        }
    }

    /**
     * @param cache Cache.
     * @throws Exception If check failed.
     */
    private void checkQueryResults(IgniteCache<CacheKey, CacheValue> cache) throws Exception {
        QueryCursor<Cache.Entry<CacheKey, CacheValue>> qry =
            cache.localQuery(new SqlQuery(CacheValue.class, "val > 1 and val < 4"));

        Iterator<Cache.Entry<CacheKey, CacheValue>> iter = qry.iterator();

        assert iter != null;
        assert iter.hasNext();

        Cache.Entry<CacheKey, CacheValue> entry = iter.next();

        assert entry.getKey().equals(new CacheKey(2)) || entry.getKey().equals(new CacheKey(3));

        assert iter.hasNext();

        entry = iter.next();

        assert entry.getKey().equals(new CacheKey(2)) || entry.getKey().equals(new CacheKey(3));
        assert !iter.hasNext();
    }

    /**
     * Cache key.
     */
    public static class CacheKey implements Externalizable {
        /** Key. */
        private int key;

        /**
         * @param key Key.
         */
        CacheKey(int key) {
            this.key = key;
        }

        /**
         *
         */
        public CacheKey() {
            /* No-op. */
        }

        /**
         * @return Key.
         */
        public int getKey() {
            return key;
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            key = in.readInt();

            keyDesCnt++;

            if (TEST_DEBUG)
                X.println("Deserialized demo key [keyDesCnt=" + keyDesCnt + ", key=" + this + ']');
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(key);

            keySerCnt++;

            if (TEST_DEBUG)
                X.println("Serialized demo key [serCnt=" + keySerCnt + ", key=" + this + ']');
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            CacheKey cacheKey;

            if (o instanceof CacheKey)
                cacheKey = (CacheKey)o;
            else
                return false;

            return key == cacheKey.key;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return key;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(CacheKey.class, this);
        }
    }

    /**
     * Cache value..
     */
    public static class CacheValue implements Externalizable {
        /** Value. */
        @QuerySqlField
        private String val;

        /**
         * @param val Value.
         */
        CacheValue(String val) {
            this.val = val;
        }

        /**
         *
         */
        public CacheValue() {
            /* No-op. */
        }

        /**
         * @return Value.
         */
        public String getValue() {
            return val;
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            val = U.readString(in);

            valDesCnt++;

            if (TEST_DEBUG)
                X.println("Deserialized demo value [valDesCnt=" + valDesCnt + ", val=" + this + ']');
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            U.writeString(out, val);

            valSerCnt++;

            if (TEST_DEBUG)
                X.println("Serialized demo value [serCnt=" + valSerCnt + ", val=" + this + ']');
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            CacheValue val = (CacheValue)o;

            return !(this.val != null ? !this.val.equals(val.val) : val.val != null);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return val != null ? val.hashCode() : 0;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(CacheValue.class, this);
        }
    }
}
