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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteClientReconnectAbstractTest;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.managers.discovery.CustomEventListener;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.query.GridQueryProcessor;
import org.apache.ignite.internal.processors.query.QueryField;
import org.apache.ignite.internal.processors.query.QueryUtils;
import org.apache.ignite.internal.processors.query.h2.IgniteH2Indexing;
import org.apache.ignite.internal.processors.query.schema.message.SchemaFinishDiscoveryMessage;
import org.apache.ignite.internal.util.typedef.T3;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgnitePredicate;

import static org.apache.ignite.internal.IgniteClientReconnectAbstractTest.TestTcpDiscoverySpi;

/**
 * Concurrency tests for dynamic index create/drop.
 */
@SuppressWarnings("unchecked")
public abstract class DynamicColumnsAbstractConcurrentSelfTest extends DynamicColumnsAbstractTest {
    /** Test duration. */
    private static final long TEST_DUR = 10_000L;

    /** Large cache size. */
    private static final int LARGE_CACHE_SIZE = 100_000;

    private static final String TBL_NAME = "PERSON";

    private static final String CACHE_NAME = QueryUtils.createTableCacheName(QueryUtils.DFLT_SCHEMA, TBL_NAME);

    /** Attribute to filter node out of cache data nodes. */
    protected static final String ATTR_FILTERED = "FILTERED";

    private static final String s = CREATE_SQL + " WITH \"template=TPL\"";

    /** Latches to block certain index operations. */
    private static final ConcurrentHashMap<UUID, T3<CountDownLatch, AtomicBoolean, CountDownLatch>> BLOCKS =
        new ConcurrentHashMap<>();

    /** Cache mode. */
    private final CacheMode cacheMode;

    /** Atomicity mode. */
    private final CacheAtomicityMode atomicityMode;

    /**
     * Constructor.
     *
     * @param cacheMode Cache mode.
     * @param atomicityMode Atomicity mode.
     */
    DynamicColumnsAbstractConcurrentSelfTest(CacheMode cacheMode, CacheAtomicityMode atomicityMode) {
        this.cacheMode = cacheMode;
        this.atomicityMode = atomicityMode;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        GridQueryProcessor.idxCls = BlockingIndexing.class;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ConstantConditions")
    @Override protected void afterTest() throws Exception {
        GridQueryProcessor.idxCls = null;

        for (T3<CountDownLatch, AtomicBoolean, CountDownLatch> block : BLOCKS.values())
            block.get1().countDown();

        BLOCKS.clear();

        stopAllGrids();

        super.afterTest();
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 5 * 60 * 1000L;
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration commonConfiguration(int idx) throws Exception {
        return super.commonConfiguration(idx).setDiscoverySpi(new TestTcpDiscoverySpi());
    }

    /**
     * Make sure that coordinator migrates correctly between nodes.
     *
     * @throws Exception If failed.
     */
    public void testCoordinatorChange() throws Exception {
        CountDownLatch finishLatch = new CountDownLatch(2);

        // Start servers.
        Ignite srv1 = ignitionStart(serverConfiguration(1), null);
        Ignite srv2 = ignitionStart(serverConfiguration(2), null);
        ignitionStart(serverConfiguration(3, true), finishLatch);

        UUID srv1Id = srv1.cluster().localNode().id();
        UUID srv2Id = srv2.cluster().localNode().id();

        // Start client which will execute operations.
        IgniteEx cli = (IgniteEx)ignitionStart(clientConfiguration(4), finishLatch);

        createSqlCache(cli);

        run(cli, s);

        // Test migration between normal servers.
        CountDownLatch idxLatch = blockIndexing(srv1Id);

        IgniteInternalFuture<?> colFut1 = queryProcessor(cli)
            .dynamicColumnAdd(CACHE_NAME, QueryUtils.DFLT_SCHEMA, TBL_NAME,
                Collections.singletonList(c("age", Integer.class.getName())), null, null, false, false);

        U.await(idxLatch);

        //srv1.close();
        Ignition.stop(srv1.name(), true);

        unblockIndexing(srv1Id);

        colFut1.get();

        checkNodesState(TBL_NAME, "NAME", c("age", Integer.class.getName()));

        // Test migration from normal server to non-affinity server.
        idxLatch = blockIndexing(srv2Id);

        IgniteInternalFuture<?> colFut2 = queryProcessor(cli)
            .dynamicColumnAdd(CACHE_NAME, QueryUtils.DFLT_SCHEMA,
                TBL_NAME, Collections.singletonList(c("city", String.class.getName())), null, "NAME", false, false);

        idxLatch.countDown();

        //srv2.close();
        Ignition.stop(srv2.name(), true);

        U.await(idxLatch);

        colFut2.get();

        checkNodesState(TBL_NAME, "NAME", c("city", String.class.getName()));
    }

    /**
     * Test operations join.
     *
     * @throws Exception If failed.
     */
    public void testOperationChaining() throws Exception {
        CountDownLatch finishLatch = new CountDownLatch(7);

        Ignite srv1 = ignitionStart(serverConfiguration(1), finishLatch);

        ignitionStart(serverConfiguration(2), finishLatch);
        ignitionStart(serverConfiguration(3, true), finishLatch);
        ignitionStart(clientConfiguration(4), finishLatch);

        createSqlCache(srv1);

        run(srv1, s);

        CountDownLatch idxLatch = blockIndexing(srv1);

        QueryField c1 = c("age", Integer.class.getName());
        QueryField c2 = c("city", String.class.getName());

        IgniteInternalFuture<?> colFut1 = addCols(srv1, null, "ID", c1);

        IgniteInternalFuture<?> colFut2 = addCols(srv1, null, null, c2);

        U.await(idxLatch);

        // Start even more nodes of different flavors
        ignitionStart(serverConfiguration(5), finishLatch);
        ignitionStart(serverConfiguration(6, true), finishLatch);
        ignitionStart(clientConfiguration(7), finishLatch);

        assert !colFut1.isDone();
        assert !colFut2.isDone();

        unblockIndexing(srv1);

        colFut1.get();
        colFut2.get();

        U.await(finishLatch);

        checkNodesState(TBL_NAME, "ID", c1);
        checkNodesState(TBL_NAME, "NAME", c2);
    }

    /**
     * Test node join on pending operation.
     *
     * @throws Exception If failed.
     */
    public void testNodeJoinOnPendingOperation() throws Exception {
        CountDownLatch finishLatch = new CountDownLatch(4);

        Ignite srv1 = ignitionStart(serverConfiguration(1), finishLatch);

        createSqlCache(srv1);

        run(srv1, s);

        CountDownLatch idxLatch = blockIndexing(srv1);

        QueryField c = c("age", Integer.class.getName());

        IgniteInternalFuture<?> idxFut = addCols(srv1, null, null, c);

        U.await(idxLatch);

        ignitionStart(serverConfiguration(2), finishLatch);
        ignitionStart(serverConfiguration(3, true), finishLatch);
        ignitionStart(clientConfiguration(4), finishLatch);

        assertFalse(idxFut.isDone());

        unblockIndexing(srv1);

        idxFut.get();

        U.await(finishLatch);

        checkNodesState(TBL_NAME, "NAME", c);
    }

    /**
     * PUT/REMOVE data from cache and build index concurrently.
     *
     * @throws Exception If failed,
     */
    public void testConcurrentPutRemove() throws Exception {
        CountDownLatch finishLatch = new CountDownLatch(4);

        // Start several nodes.
        Ignite srv1 = ignitionStart(serverConfiguration(1), finishLatch);
        ignitionStart(serverConfiguration(2), finishLatch);
        ignitionStart(serverConfiguration(3), finishLatch);
        ignitionStart(serverConfiguration(4), finishLatch);

        awaitPartitionMapExchange();

        createSqlCache(srv1);

        run(srv1, s);

        // Start data change operations from several threads.
        final AtomicBoolean stopped = new AtomicBoolean();

        IgniteInternalFuture updateFut = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                while (!stopped.get()) {
                    Ignite node = grid(ThreadLocalRandom.current().nextInt(1, 5));

                    int key = ThreadLocalRandom.current().nextInt(0, LARGE_CACHE_SIZE);
                    int val = ThreadLocalRandom.current().nextInt();

                    IgniteCache<BinaryObject, BinaryObject> cache = node.cache(CACHE_NAME);

                    if (ThreadLocalRandom.current().nextBoolean())
                        cache.put(key(node, key), val(node, val));
                    else
                        cache.remove(key(node, key));
                }

                return null;
            }
        }, 4);

        // Let some to arrive.
        Thread.sleep(500L);

        // TODO make H2 row descriptor aware of what's going on
        addCols(srv1, "ID", null, c("v", Integer.class.getName())).get();

        // Stop updates once index is ready.
        stopped.set(true);

        updateFut.get();

        finishLatch.await();

        // Make sure index is there.
        checkNodesState(TBL_NAME, null, c("v", Integer.class.getName()));

        run(srv1, "update person set \"v\" = case when mod(id, 2) <> 0 then substring(name, 7, length(name) - 6) " +
            "else null end");

        // Get expected values.
        Map<Integer, Integer> expKeys = new HashMap<>();

        IgniteCache<BinaryObject, BinaryObject> cache = srv1.cache(CACHE_NAME);

        for (int i = 0; i < LARGE_CACHE_SIZE; i++) {
            BinaryObject val = cache.get(key(srv1, i));

            if (val != null) {
                int fldVal = val.field("v");

                expKeys.put(i, fldVal);
            }
        }

        // Validate query result.
        /*for (Ignite node : Ignition.allGrids()) {
            IgniteCache<BinaryObject, BinaryObject> nodeCache = node.cache(CACHE_NAME).withKeepBinary();

            SqlQuery qry = new SqlQuery(typeName(ValueClass.class), SQL_SIMPLE_FIELD_1).setArgs(SQL_ARG_1);

            List<Cache.Entry<BinaryObject, BinaryObject>> res = nodeCache.query(qry).getAll();

            assertEquals("Cache size mismatch [exp=" + expKeys.size() + ", actual=" + res.size() + ']',
                expKeys.size(), res.size());

            for (Cache.Entry<BinaryObject, BinaryObject> entry : res) {
                long key = entry.getKey().field(FIELD_KEY);
                Long fieldVal = entry.getValue().field(FIELD_NAME_1);

                assertTrue("Expected key is not in result set: " + key, expKeys.containsKey(key));

                assertEquals("Unexpected value [key=" + key + ", expVal=" + expKeys.get(key) +
                    ", actualVal=" + fieldVal + ']', expKeys.get(key), fieldVal);
            }

        }*/
    }

    private BinaryObject val(Ignite node, int val) {
        String valTypeName = ((IgniteEx)node).context().query().types(CACHE_NAME).iterator().next().valueTypeName();

        return node.binary().builder(valTypeName).setField("name", "person" + val).build();
    }

    private BinaryObject key(Ignite node, int key) {
        String keyTypeName = ((IgniteEx)node).context().query().types(CACHE_NAME).iterator().next().keyTypeName();

        return node.binary().builder(keyTypeName).setField("ID", key).build();
    }

    /**
     * Test index consistency on re-balance.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentRebalance() throws Exception {
        // Start cache and populate it with data.
        /*Ignite srv1 = ignitionStart(serverConfiguration(1));
        Ignite srv2 = ignitionStart(serverConfiguration(2));

        createSqlCache(srv1);

        awaitPartitionMapExchange();

        put(srv1, 0, LARGE_CACHE_SIZE);

        // Start index operation in blocked state.
        CountDownLatch idxLatch1 = blockIndexing(srv1);
        CountDownLatch idxLatch2 = blockIndexing(srv2);

        QueryIndex idx = index(IDX_NAME_1, field(FIELD_NAME_1));

        final IgniteInternalFuture<?> idxFut =
            queryProcessor(srv1).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, false);

        idxLatch1.countDown();
        idxLatch2.countDown();

        // Start two more nodes and unblock index operation in the middle.
        ignitionStart(serverConfiguration(3));

        unblockIndexing(srv1);
        unblockIndexing(srv2);

        ignitionStart(serverConfiguration(4));

        awaitPartitionMapExchange();

        // Validate index state.
        idxFut.get();

        assertIndex(CACHE_NAME, TBL_NAME, IDX_NAME_1, field(FIELD_NAME_1));

        assertIndexUsed(IDX_NAME_1, SQL_SIMPLE_FIELD_1, SQL_ARG_1);
        assertSqlSimpleData(SQL_SIMPLE_FIELD_1, LARGE_CACHE_SIZE - SQL_ARG_1);*/
    }

    /**
     * Check what happen in case cache is destroyed before operation is started.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentCacheDestroy() throws Exception {
        // Start complex topology.
        /*Ignite srv1 = ignitionStart(serverConfiguration(1));

        ignitionStart(serverConfiguration(2));
        ignitionStart(serverConfiguration(3, true));

        Ignite cli = ignitionStart(clientConfiguration(4));

        // Start cache and populate it with data.
        createSqlCache(cli);

        put(cli, KEY_AFTER);

        // Start index operation and block it on coordinator.
        CountDownLatch idxLatch = blockIndexing(srv1);

        QueryIndex idx = index(IDX_NAME_1, field(FIELD_NAME_1));

        final IgniteInternalFuture<?> idxFut =
            queryProcessor(srv1).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, false);

        idxLatch.await();

        // Destroy cache (drop table).
        destroySqlCache(cli);

        // Unblock indexing and see what happens.
        unblockIndexing(srv1);

        try {
            idxFut.get();

            fail("Exception has not been thrown.");
        }
        catch (SchemaOperationException e) {
            // No-op.
        }*/
    }

    /**
     * Make sure that contended operations on the same index from different nodes do not hang.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentOperationsMultithreaded() throws Exception {
        // Start complex topology.
        /*ignitionStart(serverConfiguration(1));
        ignitionStart(serverConfiguration(2));
        ignitionStart(serverConfiguration(3, true));

        Ignite cli = ignitionStart(clientConfiguration(4));

        createSqlCache(cli);

        final AtomicBoolean stopped = new AtomicBoolean();

        // Start several threads which will mess around indexes.
        final QueryIndex idx = index(IDX_NAME_1, field(FIELD_NAME_1));

        IgniteInternalFuture idxFut = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                boolean exists = false;

                while (!stopped.get()) {
                    Ignite node = grid(ThreadLocalRandom.current().nextInt(1, 5));

                    IgniteInternalFuture fut;

                    if (exists) {
                        fut = queryProcessor(node).dynamicIndexDrop(CACHE_NAME, CACHE_NAME, IDX_NAME_1, true);

                        exists = false;
                    }
                    else {
                        fut = queryProcessor(node).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, true);

                        exists = true;
                    }

                    try {
                        fut.get();
                    }
                    catch (SchemaOperationException e) {
                        // No-op.
                    }
                    catch (Exception e) {
                        fail("Unexpected exception: " + e);
                    }
                }

                return null;
            }
        }, 8);

        Thread.sleep(TEST_DUR);

        stopped.set(true);

        // Make sure nothing hanged.
        idxFut.get();

        queryProcessor(cli).dynamicIndexDrop(CACHE_NAME, CACHE_NAME, IDX_NAME_1, true).get();
        queryProcessor(cli).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, true).get();

        assertIndex(CACHE_NAME, TBL_NAME, IDX_NAME_1, field(FIELD_NAME_1));

        put(cli, 0, KEY_AFTER);

        assertIndexUsed(IDX_NAME_1, SQL_SIMPLE_FIELD_1, SQL_ARG_1);
        assertSqlSimpleData(SQL_SIMPLE_FIELD_1, KEY_AFTER - SQL_ARG_1);*/
    }

    /**
     * Make sure that contended operations on the same index from different nodes do not hang when we issue both
     * CREATE/DROP and SELECT statements.
     *
     * @throws Exception If failed.
     */
    public void testQueryConsistencyMultithreaded() throws Exception {
        // Start complex topology.
        /*ignitionStart(serverConfiguration(1));
        ignitionStart(serverConfiguration(2));
        ignitionStart(serverConfiguration(3, true));

        Ignite cli = ignitionStart(clientConfiguration(4));

        createSqlCache(cli);

        put(cli, 0, KEY_AFTER);

        final AtomicBoolean stopped = new AtomicBoolean();

        // Thread which will mess around indexes.
        final QueryIndex idx = index(IDX_NAME_1, field(FIELD_NAME_1));

        IgniteInternalFuture idxFut = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                boolean exists = false;

                while (!stopped.get()) {
                    Ignite node = grid(ThreadLocalRandom.current().nextInt(1, 5));

                    IgniteInternalFuture fut;

                    if (exists) {
                        fut = queryProcessor(node).dynamicIndexDrop(CACHE_NAME, CACHE_NAME, IDX_NAME_1, true);

                        exists = false;
                    }
                    else {
                        fut = queryProcessor(node).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, true);

                        exists = true;
                    }

                    try {
                        fut.get();
                    }
                    catch (SchemaOperationException e) {
                        // No-op.
                    }
                    catch (Exception e) {
                        fail("Unexpected exception: " + e);
                    }
                }

                return null;
            }
        }, 1);

        IgniteInternalFuture qryFut = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                while (!stopped.get()) {
                    Ignite node = grid(ThreadLocalRandom.current().nextInt(1, 5));

                    assertSqlSimpleData(node, SQL_SIMPLE_FIELD_1, KEY_AFTER - SQL_ARG_1);
                }

                return null;
            }
        }, 8);

        Thread.sleep(TEST_DUR);

        stopped.set(true);

        // Make sure nothing hanged.
        idxFut.get();
        qryFut.get();*/
    }

    /**
     * Make sure that client receives schema changes made while it was disconnected.
     *
     * @throws Exception If failed.
     */
    public void testClientReconnect() throws Exception {
        checkClientReconnect(false);
    }

    /**
     * Make sure that client receives schema changes made while it was disconnected, even with cache recreation.
     *
     * @throws Exception If failed.
     */
    public void testClientReconnectWithCacheRestart() throws Exception {
        checkClientReconnect(true);
    }

    /**
     * Make sure that client receives schema changes made while it was disconnected, optionally with cache restart
     * in the interim.
     *
     * @param restartCache Whether cache needs to be recreated during client's absence.
     * @throws Exception If failed.
     */
    private void checkClientReconnect(final boolean restartCache) throws Exception {
        // Start complex topology.
        final Ignite srv = ignitionStart(serverConfiguration(1));
        ignitionStart(serverConfiguration(2));
        ignitionStart(serverConfiguration(3, true));

        final Ignite cli = ignitionStart(clientConfiguration(4));

        createSqlCache(cli);

        run(cli, s);

        final QueryField[] cols =
            new QueryField[] { c("age", Integer.class.getName()), c("city", String.class.getName()) };

        // Check index create.
        reconnectClientNode(srv, cli, restartCache, new RunnableX() {
            @Override public void run() throws Exception {
                addCols(srv, null, null, cols).get();
            }
        });

        checkNodeState((IgniteEx)cli, TBL_NAME, "NAME", cols);

        // TODO add also column add-drop-add check combined with node restart.
    }

    /**
     * Reconnect the client and run specified actions while it's out.
     *
     * @param srvNode Server node.
     * @param cliNode Client node.
     * @param restart Whether cache has to be recreated prior to executing required actions.
     * @param clo Closure to run
     * @throws Exception If failed.
     */
    private void reconnectClientNode(final Ignite srvNode, final Ignite cliNode, final boolean restart,
        final RunnableX clo) throws Exception {
        IgniteClientReconnectAbstractTest.reconnectClientNode(log, cliNode, srvNode, new Runnable() {
            @Override public void run() {
                if (restart) {
                    DynamicColumnsAbstractConcurrentSelfTest.this.run(srvNode, DROP_SQL);

                    DynamicColumnsAbstractConcurrentSelfTest.this.run(srvNode, s);
                }

                try {
                    clo.run();
                }
                catch (Exception e) {
                    throw new IgniteException("Test reconnect runnable failed.", e);
                }
            }
        });

        if (restart)
            cliNode.cache(CACHE_NAME);
    }

    /**
     * Test concurrent node start/stop along with index operations. Nothing should hang.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentOperationsAndNodeStartStopMultithreaded() throws Exception {
        // Start several stable nodes.
        /*ignitionStart(serverConfiguration(1));
        ignitionStart(serverConfiguration(2));
        ignitionStart(serverConfiguration(3, true));

        final Ignite cli = ignitionStart(clientConfiguration(4));

        createSqlCache(cli);

        final AtomicBoolean stopped = new AtomicBoolean();

        // Start node start/stop worker.
        final AtomicInteger nodeIdx = new AtomicInteger(4);

        IgniteInternalFuture startStopFut = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                boolean exists = false;

                int lastIdx = 0;

                while (!stopped.get()) {
                    if (exists) {
                        stopGrid(lastIdx);

                        exists = false;
                    }
                    else {
                        lastIdx = nodeIdx.incrementAndGet();

                        IgniteConfiguration cfg;

                        switch (ThreadLocalRandom.current().nextInt(0, 3)) {
                            case 1:
                                cfg = serverConfiguration(lastIdx, false);

                                break;

                            case 2:

                                cfg = serverConfiguration(lastIdx, true);

                                break;

                            default:
                                cfg = clientConfiguration(lastIdx);
                        }

                        ignitionStart(cfg);

                        exists = true;
                    }

                    Thread.sleep(ThreadLocalRandom.current().nextLong(500L, 1500L));
                }

                return null;
            }
        }, 1);

        // Start several threads which will mess around indexes.
        final QueryIndex idx = index(IDX_NAME_1, field(FIELD_NAME_1));

        IgniteInternalFuture idxFut = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                boolean exists = false;

                while (!stopped.get()) {
                    Ignite node = grid(ThreadLocalRandom.current().nextInt(1, 5));

                    IgniteInternalFuture fut;

                    if (exists) {
                        fut = queryProcessor(node).dynamicIndexDrop(CACHE_NAME, CACHE_NAME, IDX_NAME_1, true);

                        exists = false;
                    }
                    else {
                        fut = queryProcessor(node).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, true);

                        exists = true;
                    }

                    try {
                        fut.get();
                    }
                    catch (SchemaOperationException e) {
                        // No-op.
                    }
                    catch (Exception e) {
                        fail("Unexpected exception: " + e);
                    }
                }

                return null;
            }
        }, 1);

        Thread.sleep(TEST_DUR);

        stopped.set(true);

        // Make sure nothing hanged.
        startStopFut.get();
        idxFut.get();

        // Make sure cache is operational at this point.
        createSqlCache(cli);

        queryProcessor(cli).dynamicIndexDrop(CACHE_NAME, CACHE_NAME, IDX_NAME_1, true).get();
        queryProcessor(cli).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, true).get();

        assertIndex(CACHE_NAME, TBL_NAME, IDX_NAME_1, field(FIELD_NAME_1));

        put(cli, 0, KEY_AFTER);

        assertIndexUsed(IDX_NAME_1, SQL_SIMPLE_FIELD_1, SQL_ARG_1);
        assertSqlSimpleData(SQL_SIMPLE_FIELD_1, KEY_AFTER - SQL_ARG_1);*/
    }

    /**
     * Multithreaded cache start/stop along with index operations. Nothing should hang.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentOperationsAndCacheStartStopMultithreaded() throws Exception {
        // Start complex topology.
        /*ignitionStart(serverConfiguration(1));
        ignitionStart(serverConfiguration(2));
        ignitionStart(serverConfiguration(3));

        Ignite cli = ignitionStart(clientConfiguration(4));

        final AtomicBoolean stopped = new AtomicBoolean();

        // Start cache create/destroy worker.
        IgniteInternalFuture startStopFut = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                boolean exists = false;

                while (!stopped.get()) {
                    Ignite node = grid(ThreadLocalRandom.current().nextInt(1, 5));

                    if (exists) {
                        destroySqlCache(node);

                        exists = false;
                    }
                    else {
                        createSqlCache(node);

                        exists = true;
                    }

                    Thread.sleep(ThreadLocalRandom.current().nextLong(200L, 400L));
                }

                return null;
            }
        }, 1);

        // Start several threads which will mess around indexes.
        final QueryIndex idx = index(IDX_NAME_1, field(FIELD_NAME_1));

        IgniteInternalFuture idxFut = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                boolean exists = false;

                while (!stopped.get()) {
                    Ignite node = grid(ThreadLocalRandom.current().nextInt(1, 5));

                    IgniteInternalFuture fut;

                    if (exists) {
                        fut = queryProcessor(node).dynamicIndexDrop(CACHE_NAME, CACHE_NAME, IDX_NAME_1, true);

                        exists = false;
                    }
                    else {
                        fut = queryProcessor(node).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, true);

                        exists = true;
                    }

                    try {
                        fut.get();
                    }
                    catch (SchemaOperationException e) {
                        // No-op.
                    }
                    catch (Exception e) {
                        fail("Unexpected exception: " + e);
                    }
                }

                return null;
            }
        }, 8);

        Thread.sleep(TEST_DUR);

        stopped.set(true);

        // Make sure nothing hanged.
        startStopFut.get();
        idxFut.get();

        // Make sure cache is operational at this point.
        createSqlCache(cli);

        queryProcessor(cli).dynamicIndexDrop(CACHE_NAME, CACHE_NAME, IDX_NAME_1, true).get();
        queryProcessor(cli).dynamicIndexCreate(CACHE_NAME, CACHE_NAME, TBL_NAME, idx, true).get();

        //assertIndex(CACHE_NAME, TBL_NAME, IDX_NAME_1, field(FIELD_NAME_1));

        put(cli, 0, KEY_AFTER);

        /*assertIndexUsed(IDX_NAME_1, SQL_SIMPLE_FIELD_1, SQL_ARG_1);
        assertSqlSimpleData(SQL_SIMPLE_FIELD_1, KEY_AFTER - SQL_ARG_1);*/
    }

    /**
     * Block indexing.
     *
     * @param node Node.
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    private static CountDownLatch blockIndexing(Ignite node) {
        UUID nodeId = ((IgniteEx)node).localNode().id();

        return blockIndexing(nodeId);
    }

    /**
     * Block indexing.
     *
     * @param nodeId Node.
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    private static CountDownLatch blockIndexing(UUID nodeId) {
        assertFalse(BLOCKS.contains(nodeId));

        CountDownLatch idxLatch = new CountDownLatch(1);

        BLOCKS.put(nodeId, new T3<>(new CountDownLatch(1), new AtomicBoolean(), idxLatch));

        return idxLatch;
    }

    /**
     * Unblock indexing.
     *
     * @param node Node.
     */
    private static void unblockIndexing(Ignite node) {
        UUID nodeId = ((IgniteEx)node).localNode().id();

        unblockIndexing(nodeId);
    }

    /**
     * Unblock indexing.
     *
     * @param nodeId Node ID.
     */
    @SuppressWarnings("ConstantConditions")
    private static void unblockIndexing(UUID nodeId) {
        T3<CountDownLatch, AtomicBoolean, CountDownLatch> blocker = BLOCKS.remove(nodeId);

        assertNotNull(blocker);

        blocker.get1().countDown();
    }

    /**
     * Await indexing.
     *
     * @param nodeId Node ID.
     */
    @SuppressWarnings("ConstantConditions")
    private static void awaitIndexing(UUID nodeId) {
        T3<CountDownLatch, AtomicBoolean, CountDownLatch> blocker = BLOCKS.get(nodeId);

        if (blocker != null) {
            assertTrue(blocker.get2().compareAndSet(false, true));

            blocker.get3().countDown();

            while (true) {
                try {
                    blocker.get1().await();

                    break;
                }
                catch (InterruptedException e) {
                    // No-op.
                }
            }
        }
    }

    /**
     * Blocking indexing processor.
     */
    private static class BlockingIndexing extends IgniteH2Indexing {
        /** {@inheritDoc} */
        @Override public void dynamicAddColumn(String schemaName, String tblName, List<QueryField> cols,
            String beforeColName, String afterColName, boolean ifTblExists, boolean ifColNotExists)
            throws IgniteCheckedException {
            awaitIndexing(ctx.localNodeId());

            super.dynamicAddColumn(schemaName, tblName, cols, beforeColName, afterColName, ifTblExists, ifColNotExists);
        }
    }

    private static IgniteInternalFuture<?> addCols(Ignite node, String beforeColName, String afterColName,
        QueryField... flds) {
        return queryProcessor(node).dynamicColumnAdd(CACHE_NAME, QueryUtils.DFLT_SCHEMA, TBL_NAME, Arrays.asList(flds),
            beforeColName, afterColName, false, false);
    }

    /**
     * Start SQL cache on given node.
     * @param node Node to create cache on.
     * @return Created cache.
     */
    private IgniteCache<?, ?> createSqlCache(Ignite node) throws IgniteCheckedException {
        node.addCacheConfiguration(new CacheConfiguration<>("TPL")
            .setCacheMode(cacheMode)
            .setAtomicityMode(atomicityMode)
            .setNodeFilter(new NodeFilter()));

        return node.getOrCreateCache(new CacheConfiguration<>("idx").setIndexedTypes(Integer.class, Integer.class));
    }

    private static Ignite ignitionStart(IgniteConfiguration cfg) {
        return ignitionStart(cfg, null);
    }

    /**
     * Spoof blocking indexing class and start new node.
     * @param cfg Node configuration.
     * @return New node.
     */
    private static Ignite ignitionStart(IgniteConfiguration cfg, final CountDownLatch latch) {
        // Have to do this for each starting node - see GridQueryProcessor ctor, it nulls
        // idxCls static field on each call.
        GridQueryProcessor.idxCls = BlockingIndexing.class;

        IgniteEx node = (IgniteEx)Ignition.start(cfg);

        if (latch != null)
            node.context().discovery().setCustomEventListener(SchemaFinishDiscoveryMessage.class,
                new CustomEventListener<SchemaFinishDiscoveryMessage>() {
                    @Override public void onCustomEvent(AffinityTopologyVersion topVer, ClusterNode snd,
                        SchemaFinishDiscoveryMessage msg) {
                        latch.countDown();
                    }
                });

        return node;
    }

    private static GridQueryProcessor queryProcessor(Ignite node) {
        return ((IgniteEx)node).context().query();
    }

    /**
     *
     * @param nodeIdx
     * @param filtered
     * @return
     * @throws Exception
     */
    private IgniteConfiguration serverConfiguration(int nodeIdx, boolean filtered) throws Exception {
        IgniteConfiguration cfg = serverConfiguration(nodeIdx);

        if (filtered)
            cfg.setUserAttributes(Collections.singletonMap(ATTR_FILTERED, true));

        return cfg;
    }

    /**
     * Runnable which can throw checked exceptions.
     */
    protected interface RunnableX {
        /**
         * Do run.
         *
         * @throws Exception If failed.
         */
        public void run() throws Exception;
    }

    /**
     * Node filter.
     */
    protected static class NodeFilter implements IgnitePredicate<ClusterNode>, Serializable {
        /** */
        private static final long serialVersionUID = 0L;

        /** {@inheritDoc} */
        @Override public boolean apply(ClusterNode node) {
            return node.attribute(ATTR_FILTERED) == null;
        }
    }
}
