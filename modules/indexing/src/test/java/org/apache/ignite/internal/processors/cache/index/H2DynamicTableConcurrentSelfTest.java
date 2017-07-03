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

package org.apache.ignite.internal.processors.cache.index;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.cache.DynamicCacheDescriptor;
import org.apache.ignite.internal.processors.cache.query.IgniteQueryErrorCode;
import org.apache.ignite.internal.processors.query.IgniteSQLException;

/**
 * Test to check cluster behavior under concurrent fire of {@code CREATE TABLE} and {@code DROP TABLE} statements
 * from different nodes.
 */
public class H2DynamicTableConcurrentSelfTest extends AbstractSchemaSelfTest {
    /** Name of PERSON cache. */
    private final static String PERSON = "Person";

    /** Name of CITY cache. */
    private final static String CITY = "City";

    /** Name of COMPANY cache. */
    private final static String COMPANY = "Company";

    /** Names of caches to use. */
    private final static String[] CACHES = new String[] { PERSON, CITY, COMPANY };

    /** Total number of nodes running. */
    private final static int NODES_CNT = 4;

    /** Test duration in ms. */
    private final static long TEST_DURATION = TimeUnit.SECONDS.toMillis(1);

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        for (IgniteConfiguration c : configurations())
            Ignition.start(c);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /**
     * Test cluster behavior under concurrent fire of {@code CREATE TABLE} and {@code DROP TABLE} statements
     * from different nodes.
     * @throws Exception if failed.
     */
    public void testConcurrentCreateDrop() throws Exception {
        final AtomicBoolean stopped = new AtomicBoolean();

        IgniteInternalFuture<?> fut = multithreadedAsync(new Callable<Object>() {
            @Override public Object call() throws Exception {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                while (!stopped.get()) {
                    int nodeIdx = rnd.nextInt(0, NODES_CNT);

                    String cacheName = CACHES[rnd.nextInt(0, CACHES.length)];

                    boolean create = rndBool();

                    boolean exFlag = rndBool();

                    execute(nodeIdx, cacheName, create, exFlag);
                }

                return null;
            }
        }, 8);

        Thread.sleep(TEST_DURATION);

        stopped.set(true);

        fut.get();

        IgniteEx node = grid(0);

        for (String cacheName : CACHES) {
            IgniteCache<BinaryObject, BinaryObject> cache = node.cache(cacheName);

            if (cache == null)
                continue;

            cache.put(key(cacheName), val(cacheName));

            assertEquals(val(cacheName), cache.get(key(cacheName)));

            executeSql(node, "DELETE FROM " + cacheName);

            executeSql(node, "INSERT INTO " + cacheName + " (id, name) values(1, 'SomeName')");

            assertEquals(Collections.singletonList(Arrays.asList(1, "SomeName")),
                executeSql(node, "SELECT id, name from " + cacheName));

            assertEquals(val(cacheName), cache.get(key(cacheName)));
        }
    }

    /**
     * @param cacheName Cache name.
     * @return Cache key to test cache work.
     */
    private BinaryObject key(String cacheName) {
        IgniteEx node = grid(0);

        DynamicCacheDescriptor desc = node.context().cache().cacheDescriptor(cacheName);

        assertNotNull(desc);

        assertEquals(1, desc.schema().entities().size());

        BinaryObjectBuilder bldr = node.binary().builder(desc.schema().entities().iterator().next().getKeyType());

        bldr.setField("id", 1);

        return bldr.build();
    }

    /**
     * @param cacheName Cache name.
     * @return Cache value to test cache work.
     */
    private BinaryObject val(String cacheName) {
        IgniteEx node = grid(0);

        DynamicCacheDescriptor desc = node.context().cache().cacheDescriptor(cacheName);

        assertNotNull(desc);

        assertEquals(1, desc.schema().entities().size());

        BinaryObjectBuilder bldr = node.binary().builder(desc.schema().entities().iterator().next().getValueType());

        bldr.setField("name", "SomeName");

        return bldr.build();
    }

    /**
     * @return {@code true} or {@code false} randomly with pseudo equal probability.
     */
    private static boolean rndBool() {
        return ThreadLocalRandom.current().nextInt(0, 2) == 0;
    }

    /**
     * Perform either {@code CREATE TABLE} or {@code DROP TABLE} for cache with given name with given params.
     * @param nodeIdx Node index to issue statement from.
     * @param cacheName Cache (and table) name.
     * @param create If {@code true}, then {@code CREATE TABLE} will be performed, otherwise {@code DROP TABLE} will be
     *     performed.
     * @param exFlag If {@code true}, then SQL statement will contain {@code IF (NOT) EXISTS} clause - depending
     *     on the type of operation.
     */
    private void execute(int nodeIdx, String cacheName, boolean create, boolean exFlag) {
        IgniteEx node = grid(nodeIdx);

        assert node != null;

        String sql;

        if (create) {
            CacheMode cacheMode = rndBool() ? CacheMode.PARTITIONED : CacheMode.REPLICATED;

            CacheAtomicityMode atomicityMode = rndBool() ? CacheAtomicityMode.ATOMIC :
                CacheAtomicityMode.TRANSACTIONAL;

            sql = "CREATE TABLE " + (exFlag ? "IF NOT EXISTS " : "") + cacheName +
                " (id int primary key, name varchar) WITH \"template=" + cacheMode.name() + ",atomicity=" +
                atomicityMode.name() + '"';
        }
        else
            sql = "DROP TABLE " + (exFlag ? "IF EXISTS " : "") + cacheName;

        try {
            executeSql(node, sql);
        }
        catch (IgniteSQLException e) {
            int code = e.statusCode();

            if (create && code == IgniteQueryErrorCode.TABLE_ALREADY_EXISTS) {
                if (exFlag)
                    throw new AssertionError(e); // Should never happen with this flag.
                else
                    return;
            }

            if (!create && code == IgniteQueryErrorCode.TABLE_NOT_FOUND) {
                if (exFlag)
                    throw new AssertionError(e); // Should never happen with this flag.
                else
                    return;
            }

            throw e;
        }
    }

    /**
     * @return configurations for nodes participating in this test.
     * @throws Exception if failed.
     */
    private IgniteConfiguration[] configurations() throws Exception {
        return new IgniteConfiguration[] {
            serverConfiguration(0),
            serverConfiguration(1),
            clientConfiguration(2),
            serverConfiguration(3)
        };
    }

    /**
     * Create server configuration.
     *
     * @param idx Index.
     * @return Configuration.
     * @throws Exception If failed.
     */
    private IgniteConfiguration serverConfiguration(int idx) throws Exception {
        return commonConfiguration(idx);
    }

    /**
     * Create client configuration.
     *
     * @param idx Index.
     * @return Configuration.
     * @throws Exception If failed.
     */
    private IgniteConfiguration clientConfiguration(int idx) throws Exception {
        return commonConfiguration(idx).setClientMode(true);
    }

    /**
     * Create common node configuration.
     *
     * @param idx Index.
     * @return Configuration.
     * @throws Exception If failed.
     */
    private IgniteConfiguration commonConfiguration(int idx) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(getTestIgniteInstanceName(idx));

        return optimize(cfg);
    }
}
