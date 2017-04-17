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

package org.apache.ignite.internal.processors.cache.distributed.near;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import javax.cache.Cache;
import javax.cache.CacheException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.IgniteException;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.affinity.AffinityFunction;
import org.apache.ignite.cache.affinity.AffinityFunctionContext;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.util.GridRandom;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.util.AttributeNodeFilter;
import org.jsr166.ThreadLocalRandom8;

/**
 * Tests distributed queries over set of partitions.
 *
 * The test assigns partition ranges to specific grid nodes using special affinity implementation.
 */
public class IgniteCacheDistributedPartitionQuerySelfTest extends GridCommonAbstractTest {
    /** Region node attribute name. */
    private static final String REGION_ATTR_NAME = "reg";

    /** Grids count. */
    private static final int GRIDS_COUNT = 10;

    /** IP finder. */
    private static final TcpDiscoveryVmIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);

    /** Partitions per region distribution. */
    private static final int[] PARTS_PER_REGION = new int[] {100, 200, 300, 400, 24};

    /** Unmapped region id. */
    private static final int UNMAPPED_REGION = PARTS_PER_REGION.length;

    /** Clients per partition. */
    private static final int CLIENTS_PER_PARTITION = 1;

    /** Total clients. */
    private static final int TOTAL_CLIENTS;

    /** Affinity function to use on partitioned caches. */
    private static final AffinityFunction AFFINITY = new RegionAwareAffinityFunction();

    /** Partitions count. */
    private static final int PARTS_COUNT;

    /** Regions to partitions mapping. */
    private static final NavigableMap<Integer, List<Integer>> REGION_TO_PART_MAP = new TreeMap<>();

    /** Query threads count. */
    private static final int QUERY_THREADS_CNT = 4;

    /** Restarting threads count. */
    private static final int RESTART_THREADS_CNT = 2;

    /** Node stop time. */
    public static final int NODE_STOP_TIME = 1_000;

    static {
        int total = 0, parts = 0, p = 0, regionId = 1;

        for (int regCnt : PARTS_PER_REGION) {
            total += regCnt * CLIENTS_PER_PARTITION;

            parts += regCnt;

            REGION_TO_PART_MAP.put(regionId++, Arrays.asList(p, regCnt));

            p += regCnt;
        }

        /** Last region was left empty intentionally, see {@link #UNMAPPED_REGION} */
        TOTAL_CLIENTS = total - PARTS_PER_REGION[PARTS_PER_REGION.length - 1] * CLIENTS_PER_PARTITION;

        PARTS_COUNT = parts;
    }

    /** Deposits per client. */
    public static final int DEPOSITS_PER_CLIENT = 10;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);
        TcpDiscoverySpi spi = (TcpDiscoverySpi)cfg.getDiscoverySpi();
        spi.setIpFinder(IP_FINDER);

        /** Clients cache */
        CacheConfiguration<ClientKey, Client> clientCfg = new CacheConfiguration<>();
        clientCfg.setName("cl");
        clientCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        clientCfg.setBackups(2);
        clientCfg.setAffinity(AFFINITY);
        clientCfg.setIndexedTypes(ClientKey.class, Client.class);

        /** Deposits cache */
        CacheConfiguration<DepositKey, Deposit> depoCfg = new CacheConfiguration<>();
        depoCfg.setName("de");
        depoCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        depoCfg.setBackups(2);
        depoCfg.setAffinity(AFFINITY);
        depoCfg.setIndexedTypes(DepositKey.class, Deposit.class);

        /** Regions cache. Uses default affinity. */
        CacheConfiguration<Integer, Region> regionCfg = new CacheConfiguration<>();
        regionCfg.setName("re");
        regionCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        regionCfg.setCacheMode(CacheMode.REPLICATED);
        regionCfg.setIndexedTypes(Integer.class, Region.class);

        cfg.setCacheConfiguration(clientCfg, depoCfg, regionCfg);

        if ("client".equals(gridName))
            cfg.setClientMode(true);
        else {
            Integer reg = regionForGrid(gridName);

            cfg.setUserAttributes(F.asMap(REGION_ATTR_NAME, reg));

            log().info("Assigned region " + reg + " to grid " + gridName);
        }

        return cfg;
    }

    /** */
    private static final class RegionAwareAffinityFunction implements AffinityFunction {
        /** {@inheritDoc} */
        @Override public void reset() {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public int partitions() {
            return PARTS_COUNT;
        }

        /** {@inheritDoc} */
        @Override public int partition(Object key) {
            Integer regionId;

            if (key instanceof RegionKey)
                regionId = ((RegionKey)key).regionId;
            else if (key instanceof BinaryObject) {
                BinaryObject bo = (BinaryObject)key;

                regionId = bo.field("regionId");
            }
            else
                throw new IgniteException("Unsupported key for region aware affinity");

            List<Integer> range = REGION_TO_PART_MAP.get(regionId);

            Integer cnt = range.get(1);

            return U.safeAbs(key.hashCode() % cnt) + range.get(0); // Assign partition in region's range.
        }

        /** {@inheritDoc} */
        @Override public List<List<ClusterNode>> assignPartitions(AffinityFunctionContext affCtx) {
            List<ClusterNode> nodes = affCtx.currentTopologySnapshot();

            List<List<ClusterNode>> assignment = new ArrayList<>(PARTS_COUNT);

            for (int p = 0; p < PARTS_COUNT; p++) {
                // Get region for partition.
                int regionId = regionForPart(p);

                // Filter all nodes for region.
                AttributeNodeFilter f = new AttributeNodeFilter(REGION_ATTR_NAME, regionId);

                List<ClusterNode> regionNodes = new ArrayList<>();

                for (ClusterNode node : nodes)
                    if (f.apply(node))
                        regionNodes.add(node);

                final int cp = p;

                Collections.sort(regionNodes, new Comparator<ClusterNode>() {
                    @Override public int compare(ClusterNode o1, ClusterNode o2) {
                        return Long.compare(hash(cp, o1), hash(cp, o2));
                    }
                });

                assignment.add(regionNodes);
            }

            return assignment;
        }

        /** {@inheritDoc} */
        @Override public void removeNode(UUID nodeId) {
            // No-op.
        }

        /**
         * @param part Partition.
         */
        protected int regionForPart(int part) {
            for (Map.Entry<Integer, List<Integer>> entry : REGION_TO_PART_MAP.entrySet()) {
                List<Integer> range = entry.getValue();

                if (range.get(0) <= part && part < range.get(0) + range.get(1))
                    return entry.getKey();
            }

            throw new IgniteException("Failed to find zone for partition");
        }

        /**
         * @param part Partition.
         * @param obj Object.
         */
        private long hash(int part, Object obj) {
            long x = ((long)part << 32) | obj.hashCode();
            x ^= x >>> 12;
            x ^= x << 25;
            x ^= x >>> 27;
            return x * 2685821657736338717L;
        }
    }

    /**
     * Assigns a region to grid part.
     *
     * @param gridName Grid name.
     */
    private Integer regionForGrid(String gridName) {
        char c = gridName.charAt(gridName.length() - 1);
        switch (c) {
            case '0':
                return 1;
            case '1':
            case '2':
                return 2;
            case '3':
            case '4':
            case '5':
                return 3;
            default:
                return 4;
        }
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        int sum1 = 0;
        for (List<Integer> range : REGION_TO_PART_MAP.values())
            sum1 += range.get(1);

        assertEquals("Illegal partition per region distribution", PARTS_COUNT, sum1);

        startGridsMultiThreaded(GRIDS_COUNT);

        startGrid("client");

        // Fill caches.
        int clientId = 1;
        int depositId = 1;
        int regionId = 1;
        int p = 1; // Percents counter. Log message will be printed 10 times.

        try (IgniteDataStreamer<ClientKey, Client> clStr = grid(0).dataStreamer("cl");
             IgniteDataStreamer<DepositKey, Deposit> depStr = grid(0).dataStreamer("de")) {
            for (int cnt : PARTS_PER_REGION) {
                // Last region was left empty intentionally.
                if (regionId < PARTS_PER_REGION.length) {
                    for (int i = 0; i < cnt * CLIENTS_PER_PARTITION; i++) {
                        ClientKey ck = new ClientKey(clientId, regionId);

                        Client cl = new Client();
                        cl.firstName = "First_Name_" + clientId;
                        cl.lastName = "Last_Name_" + clientId;
                        cl.passport = clientId * 1_000;

                        clStr.addData(ck, cl);

                        for (int j = 0; j < DEPOSITS_PER_CLIENT; j++) {
                            DepositKey dk = new DepositKey(depositId++, new ClientKey(clientId, regionId));

                            Deposit depo = new Deposit();
                            depo.amount = ThreadLocalRandom8.current().nextLong(1_000_001);
                            depStr.addData(dk, depo);
                        }

                        if (clientId / (float)TOTAL_CLIENTS >= p / 10f) {
                            log().info("Loaded " + clientId + " of " + TOTAL_CLIENTS);

                            p++;
                        }

                        clientId++;
                    }
                }

                Region region = new Region();
                region.name = "Region_" + regionId;
                region.code = regionId * 10;

                grid(0).cache("re").put(regionId, region);

                regionId++;
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();

        stopAllGrids();
    }

    /** Tests query within region. */
    public void testRegionQuery() {
        doTestRegionQuery(grid(0));
    }

    /** Tests query within region (client). */
    public void testRegionQueryClient() throws Exception {
        doTestRegionQuery(grid("client"));
    }

    /** Test query within partitions. */
    public void testPartitionsQuery() {
        doTestPartitionsQuery(grid(0));
    }

    /** Test query within partitions (client). */
    public void testPartitionsQueryClient() throws Exception {
        doTestPartitionsQuery(grid("client"));
    }

    /** Tests join query within region. */
    public void testJoinQuery() {
        doTestJoinQuery(grid(0));
    }

    /** Tests join query within region. */
    public void testJoinQueryClient() throws Exception {
        doTestJoinQuery(grid("client"));
    }

    /** Tests join query within region on unstable topology. */
    public void testJoinQueryUnstableTopology() throws Exception {
        final AtomicBoolean stop = new AtomicBoolean();

        final GridRandom r = new GridRandom();

        final AtomicIntegerArray states = new AtomicIntegerArray(GRIDS_COUNT);

        final Ignite client = grid("client");

        log().info("Starting test");

        final AtomicInteger cnt = new AtomicInteger();

        IgniteInternalFuture<?> fut = multithreadedAsync(new Runnable() {
            @Override public void run() {
                while (!stop.get()) {
                    doTestJoinQuery(client, r.nextInt(1) + 3);

                    int cur = cnt.incrementAndGet();

                    if (cur % 100 == 0)
                        log().info("Queries count: " + cur);
                }
            }
        }, QUERY_THREADS_CNT);

        final AtomicIntegerArray restartStats = new AtomicIntegerArray(GRIDS_COUNT);

        IgniteInternalFuture<?> fut2 = multithreadedAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                while(!stop.get()) {
                    int grid = r.nextInt(GRIDS_COUNT);

                    String name = getTestIgniteInstanceName(grid);

                    Integer regionId = regionForGrid(name);

                    // Restart nodes only from region with enough number of nodes.
                    if (regionId != 3 && regionId != 4)
                        continue;

                    if (states.compareAndSet(grid, 0, 1)) {
                        restartStats.incrementAndGet(grid);

                        try {
                            stopGrid(grid);

                            Thread.sleep(r.nextInt(NODE_STOP_TIME));

                            startGrid(grid);

                            Thread.sleep(r.nextInt(NODE_STOP_TIME));
                        } finally {
                            states.set(grid, 0);
                        }
                    }
                }

                return null;
            }
        }, RESTART_THREADS_CNT);

        grid(0).scheduler().runLocal(new Runnable() {
            @Override public void run() {
                stop.set(true);
            }
        }, 60, TimeUnit.SECONDS);

        fut2.get();

        try {
            fut.get();
        }
        finally {
            log().info("Queries count: " + cnt.get());

            for (int i = 0; i < GRIDS_COUNT; i++)
                log().info("Grid [name = " + getTestIgniteInstanceName(i) + ", idx=" + i + " ] restarts count: " +
                    restartStats.get(i));
        }
    }

    /** Tests local query over partitions. */
    public void testLocalQuery() {
        Affinity<Object> affinity = grid(0).affinity("cl");

        int[] parts = affinity.primaryPartitions(grid(0).localNode());

        Arrays.sort(parts);

        IgniteCache<ClientKey, Client> cl = grid(0).cache("cl");

        SqlQuery<ClientKey, Client> qry1 = new SqlQuery<>(Client.class, "1=1");
        qry1.setLocal(true);
        qry1.setPartitions(parts[0]);

        List<Cache.Entry<ClientKey, Client>> clients = cl.query(qry1).getAll();

        for (Cache.Entry<ClientKey, Client> client : clients)
            assertEquals("Incorrect partition", parts[0], affinity.partition(client.getKey()));

        SqlFieldsQuery qry2 = new SqlFieldsQuery("select cl._KEY, cl._VAL from \"cl\".Client cl");
        qry2.setLocal(true);
        qry2.setPartitions(parts[0]);

        List<List<?>> rows = cl.query(qry2).getAll();

        for (List<?> row : rows)
            assertEquals("Incorrect partition", parts[0], affinity.partition(row.get(0)));
    }

    /**
     * @param orig Originator.
     */
    private void doTestRegionQuery(Ignite orig) {
        IgniteCache<ClientKey, Client> cl = orig.cache("cl");

        for (int regionId = 1; regionId <= PARTS_PER_REGION.length; regionId++) {
            SqlQuery<ClientKey, Client> qry1 = new SqlQuery<>(Client.class, "regionId=?");
            qry1.setArgs(regionId);

            List<Cache.Entry<ClientKey, Client>> clients1 = cl.query(qry1).getAll();

            int expRegionCnt = regionId == 5 ? 0 : PARTS_PER_REGION[regionId - 1] * CLIENTS_PER_PARTITION;

            assertEquals("Region " + regionId + " count", expRegionCnt, clients1.size());

            validateClients(regionId, clients1);

            // Repeat the same query with partition set condition.
            List<Integer> range = REGION_TO_PART_MAP.get(regionId);

            SqlQuery<ClientKey, Client> qry2 = new SqlQuery<>(Client.class, "1=1");
            qry2.setPartitions(createRange(range.get(0), range.get(1)));

            try {
                List<Cache.Entry<ClientKey, Client>> clients2 = cl.query(qry2).getAll();

                assertEquals("Region " + regionId + " count with partition set", expRegionCnt, clients2.size());

                // Query must produce only results from single region.
                validateClients(regionId, clients2);

                if (regionId == UNMAPPED_REGION)
                    fail();
            } catch (CacheException ignored) {
                if (regionId != UNMAPPED_REGION)
                    fail();
            }
        }
    }

    /** */
    private int[] createRange(int start, int cnt) {
        int[] vals = new int[cnt];

        for (int i = 0; i < cnt; i++)
            vals[i] = start + i;

        return vals;
    }

    /**
     * @param orig Originator.
     */
    private void doTestPartitionsQuery(Ignite orig) {
        IgniteCache<ClientKey, Client> cl = orig.cache("cl");

        for (int regionId = 1; regionId <= PARTS_PER_REGION.length; regionId++) {
            log().info("Running test queries for region " + regionId);

            List<Integer> range = REGION_TO_PART_MAP.get(regionId);

            int p1 = range.get(0) + 10, p2 = range.get(0) + 20;

            // Collect keys for partitions.
            List<Integer> p1List = new ArrayList<>(), p2List = new ArrayList<>();

            int minClientId = range.get(0) * CLIENTS_PER_PARTITION,
                    maxClientId = minClientId + range.get(1) * CLIENTS_PER_PARTITION;

            for (int clientId = minClientId; clientId < maxClientId; clientId++) {
                ClientKey clientKey = new ClientKey(clientId, regionId);

                int p = orig.affinity("cl").partition(clientKey);

                assertTrue("Partition in range", range.get(0) <= p && p < range.get(0) + range.get(1));

                if (p == p1)
                    p1List.add(clientId);
                else if (p == p2)
                    p2List.add(clientId);
            }

            SqlQuery<ClientKey, Client> qry = new SqlQuery<>(Client.class, "1=1");

            qry.setPartitions(p1, p2);

            try {
                List<Cache.Entry<ClientKey, Client>> clients = cl.query(qry).getAll();

                // Query must produce only results from two partitions.
                for (Cache.Entry<ClientKey, Client> client : clients) {
                    assertTrue("Incorrect partition for key",
                            p1List.contains(client.getKey().clientId) || p2List.contains(client.getKey().clientId));
                }

                if (regionId == UNMAPPED_REGION)
                    fail();
            } catch (CacheException ignored) {
                if (regionId != UNMAPPED_REGION)
                    fail();
            }
        }
    }

    /**
     * @param orig Query originator.
     * @param regions Region ids.
     */
    private void doTestJoinQuery(Ignite orig, int... regions) {
        IgniteCache<ClientKey, Client> cl = orig.cache("cl");

        if (regions == null) {
            regions = new int[PARTS_PER_REGION.length];

            for (int i = 0; i < regions.length; i++)
                regions[i] = i + 1;
        }

        for (int regionId : regions) {
            List<Integer> range = REGION_TO_PART_MAP.get(regionId);

            SqlFieldsQuery qry = new SqlFieldsQuery("select cl.clientId, de.regionId from " +
                "\"cl\".Client cl, \"de\".Deposit de, \"re\".Region re where cl.clientId=de.clientId and de.regionId=re._KEY");

            qry.setPartitions(createRange(range.get(0), range.get(1)));

            try {
                List<List<?>> rows = cl.query(qry).getAll();

                int expRegionCnt = regionId == 5 ? 0 : PARTS_PER_REGION[regionId - 1] * CLIENTS_PER_PARTITION;

                assertEquals("Clients with deposits", expRegionCnt * DEPOSITS_PER_CLIENT, rows.size());

                // Query must produce only results from single region.
                for (List<?> row : rows)
                    assertEquals("Region id", regionId, ((Integer)row.get(1)).intValue());

                if (regionId == UNMAPPED_REGION)
                    fail();
            }
            catch (CacheException ignored) {
                if (regionId != UNMAPPED_REGION)
                    fail();
            }
        }
    }

    /**
     * @param regionId Region id.
     * @param clients Clients.
     */
    private void validateClients(int regionId, List<Cache.Entry<ClientKey, Client>> clients) {
        for (Cache.Entry<ClientKey, Client> entry : clients) {
            List<Integer> range = REGION_TO_PART_MAP.get(regionId);

            int start = range.get(0) * CLIENTS_PER_PARTITION;
            int end = start + range.get(1) * CLIENTS_PER_PARTITION;

            int clientId = entry.getKey().clientId;

            assertTrue("Client id in range", start < clientId && start <= end);
        }
    }

    /** */
    private static class ClientKey extends RegionKey {
        /** Client id. */
        @QuerySqlField(index = true)
        protected int clientId;

        /**
         * @param clientId Client id.
         * @param regionId Region id.
         */
        public ClientKey(int clientId, int regionId) {
            this.clientId = clientId;
            this.regionId = regionId;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ClientKey clientKey = (ClientKey)o;

            return clientId == clientKey.clientId;

        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return clientId;
        }
    }

    /** */
    private static class DepositKey extends RegionKey {
        @QuerySqlField(index = true)
        protected int depositId;

        @QuerySqlField(index = true)
        protected int clientId;

        /** Client id. */
        @AffinityKeyMapped
        protected ClientKey clientKey;

        /**
         * @param depositId Client id.
         * @param clientKey Client key.
         */
        public DepositKey(int depositId, ClientKey clientKey) {
            this.depositId = depositId;
            this.clientId = clientKey.clientId;
            this.regionId = clientKey.regionId;
            this.clientKey = clientKey;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            DepositKey that = (DepositKey)o;

            return depositId == that.depositId;

        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return depositId;
        }
    }

    /** */
    private static class RegionKey implements Serializable {
        /** Region id. */
        @QuerySqlField(index = true)
        protected int regionId;
    }

    /** */
    private static class Client {
        @QuerySqlField
        protected String firstName;

        @QuerySqlField
        protected String lastName;

        @QuerySqlField(index = true)
        protected int passport;
    }

    /** */
    private static class Deposit {
        @QuerySqlField
        protected long amount;
    }

    /** */
    private static class Region {
        @QuerySqlField
        protected String name;

        @QuerySqlField
        protected int code;
    }
}