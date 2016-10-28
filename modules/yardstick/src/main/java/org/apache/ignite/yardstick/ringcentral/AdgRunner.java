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

package org.apache.ignite.yardstick.ringcentral;

import com.google.gson.Gson;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.marshaller.optimized.OptimizedMarshaller;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Query runner.
 */
@SuppressWarnings("FieldCanBeLocal")
public class AdgRunner {
    /** Node count. */
    private static final int NODE_CNT = 1; // Switch to 4 to see better throughput.

    /** Number of small accounts. */
    private static final int SMALL_ACC_CNT = 10_000;

    /** Cache name. */
    private static final String CACHE_NAME = "cache";

    /** Thread count. */
    private static final int THREAD_CNT = 8;

    /** Whether to use big account or not. */
    private static final boolean USE_BIG = true;

    /** Execution count. */
    private static final AtomicLong CNT = new AtomicLong();

    /** Verbose mode. */
    private static final boolean VERBOSE = false;

    /** */
    private static final String QRY_FIRST = "select extensionId, " +
        "min(extensionStatus), " +
        "max(concat(firstName,' ',lastName)), " +
        "min(phoneNumber), " +
        "max(extensionNumber) " +
        "from ADGENTITY " +
        "    JOIN table(et INT = ?, es INT = ?) t ON (t.et = extensionType and t.es = extensionStatus) " +
        "where accountId = ? " +
        "and deleteTime > ?" +
        "and ((lcase(concat(firstName,' ',lastName)) like '%john%') " +
        "OR (lcase(phoneNumber) like '%john%') " +
        "OR (lcase(extensionNumber) like '%john%')) " +
        "group by extensionId order by 2 asc, 3 desc, 4 asc, 5 desc limit 100 offset 0";

    /** */
    private static final String QRY_FIRST2 = "select extensionId, " +
        "extensionStatus, " +
        "concat(firstName,' ',lastName), " +
        "phoneNumber, " +
        "extensionNumber " +
        "from ADGENTITY " +
        "where accountId = ? " +
        "and extensionType in (?,?) " +
        "and extensionStatus in (?,?) " +
        "and deleteTime > ?";

    /** */
    private static final String QRY_SECOND = "select _val FROM ADGENTITY " +
        "join table(temp_extensionId VARCHAR = ?) i " +
        "on (extensionId = i.temp_extensionId and deleteTime > ?)";

    /** */
    private static final String QRY_THIRD = "select count(distinct extensionId) from ADGENTITY " +
        "where accountId = ? " +
        "and extensionType in (?,?) " +
        "and extensionStatus in (?,?) " +
        "and deleteTime > ? " +
        "and ((lcase(concat(firstName,' ',lastName)) like '%john%') " +
        "or (lcase(phoneNumber) like '%john%') " +
        "or (lcase(extensionNumber) like '%john%'))";

    /**
     * Entry point.
     */
    public static void main(String[] args) throws Exception {
        Ignite ignite = start();

        IgniteCache<AdgAffinityKey, AdgEntity> cache = ignite.cache(CACHE_NAME);

        loadData(ignite, cache);

        System.out.println("Loaded data: " + cache.size());

        explain(cache, QRY_FIRST, true);

        SqlFieldsQuery testQry = new SqlFieldsQuery("SELECT * FROM table(et INT = ?, es INT = ?)");

        TreeSet<Integer> extTypes = new TreeSet<>();

        extTypes.add(AdgEntity.ExtensionType.randomValue());
        extTypes.add(AdgEntity.ExtensionType.randomValue());

        TreeSet<Integer> extStates = new TreeSet<>();

        extStates.add(AdgEntity.ExtensionState.randomValue());
        extStates.add(AdgEntity.ExtensionState.randomValue());

        List<Integer> extTypesRes = new ArrayList<>();
        List<Integer> extStatesRes = new ArrayList<>();

        for (Integer extType : extTypes) {
            for (Integer extState : extStates) {
                extTypesRes.add(extType);
                extStatesRes.add(extState);
            }
        }

        testQry.setArgs(extTypesRes.toArray(), extStatesRes.toArray());

        //System.out.println(cache.query(testQry).getAll());

        for (int i = 0; i < THREAD_CNT; i++)
            startDaemon("qry-exec-" + i, new QueryExecutor(cache));

        startDaemon("printer", new ThroughputPrinter());
    }

    /**
     * Explain execution plan.
     *
     * @param cache Cache.
     * @param sql SQL.
     * @param args Whether to set arguments.
     */
    private static void explain(IgniteCache<AdgAffinityKey, AdgEntity> cache, String sql, boolean args) {
        SqlFieldsQuery qry = new SqlFieldsQuery("EXPLAIN " + sql);

        if (args)
            qry.setArgs(argumentForQuery());

        System.out.println(cache.query(qry).getAll());
        System.out.println();
    }

    /**
     * Start daemon thread.
     *
     * @param name Name.
     * @param r Runnable.
     */
    private static void startDaemon(String name, Runnable r) {
        Thread t = new Thread(r);

        t.setName(name);
        t.setDaemon(true);

        t.start();
    }

    /**
     * @return Argument for first query.
     */
    private static Object[] argumentForQuery() {
        TreeSet<Integer> extTypes = new TreeSet<>();

        extTypes.add(AdgEntity.ExtensionType.randomValue());
        extTypes.add(AdgEntity.ExtensionType.randomValue());

        TreeSet<Integer> extStates = new TreeSet<>();

        extStates.add(AdgEntity.ExtensionState.randomValue());
        extStates.add(AdgEntity.ExtensionState.randomValue());

        List<Integer> extTypesRes = new ArrayList<>();
        List<Integer> extStatesRes = new ArrayList<>();

        for (Integer extType : extTypes) {
            for (Integer extState : extStates) {
                extTypesRes.add(extType);
                extStatesRes.add(extState);
            }
        }

        Object[] res = new Object[]{
            extTypesRes.toArray(),
            extStatesRes.toArray(),
            "10k",
            String.valueOf(System.currentTimeMillis())
        };

        if (!USE_BIG)
            res[0] = AdgEntity.ACC_ID + ThreadLocalRandom.current().nextInt(0, SMALL_ACC_CNT);

        return res;
    }

    /**
     * Consume result.
     *
     * @param cur Cursor.
     * @return Hash.
     */
    private static int consumeResult(QueryCursor<List<?>> cur) {
        int hash = 0;

        for (List<?> next : cur)
            hash += next.get(0).hashCode();

        return hash;
    }

    /**
     * Load data into Ignite.
     *
     * @param ignite Ignite.
     * @param cache Cache.
     */
    private static void loadData(Ignite ignite, IgniteCache<AdgAffinityKey, AdgEntity> cache) throws Exception {
        try (IgniteDataStreamer<Object, Object> str = ignite.dataStreamer(cache.getName())) {
            int key = 0;

            // Load big account.
            AdgEntity[] entities = loadFromFileBigAccount();

            for (AdgEntity entity : entities)
                str.addData(entity.getKey(++key), entity);

            System.out.println("Loaded big account.");

            // Load small accounts.
            int accId = 0;

            while (accId < SMALL_ACC_CNT && !Thread.currentThread().isInterrupted()) {
                int extCnt = ThreadLocalRandom.current().nextInt(1, 4); // [1, 3]
                int phoneCnt = ThreadLocalRandom.current().nextInt(1, 3); // [1, 2]
                int devCnt = ThreadLocalRandom.current().nextInt(1, 4); // [1, 3]

                String accountId = AdgEntity.ACC_ID + accId;

                for (int ext = 0; ext < extCnt; ext++) {
                    String name = "John" + accId + "_" + ext;
                    String lastName = "Doe" + accId + "_" + ext;
                    String extId = key + "_" + accountId;
                    int extType = generateExtType();
                    int extStatus = generateExtStatus();

                    for (int phone = 0; phone < phoneCnt; phone++) {
                        String phoneNumberId = extId + "_" + phone;
                        String phoneNumber = "+91" + extId + "_" + phone;

                        for (int dev = 0; dev < devCnt; dev++) {
                            AdgEntity e = new AdgEntity();

                            e.setAccountId(accountId);
                            e.setExtensionId(extId);
                            e.setExtensionType(extType);
                            e.setExtensionStatus(extStatus);
                            e.setFirstName(name);
                            e.setLastName(lastName);

                            e.setPhoneNumber(phoneNumber);
                            e.setPhoneNumber(phoneNumberId);

                            if (ThreadLocalRandom.current().nextBoolean()) {
                                e.setDeviceId(extId + "_" + dev);
                                e.setDeviceType("OtherPhone");
                                e.setDeviceName("Assigned Other Phone");
                            }
                            else if (!ThreadLocalRandom.current().nextBoolean()) {
                                e.setDeviceId(extId + "_" + dev);
                                e.setDeviceType("HardPhone");
                                e.setDeviceName("IP Phone");
                            }

                            str.addData(e.getKey(key), e);

                            ++key;
                        }
                    }
                }

                ++accId;

                if (accId % 10_000 == 0)
                    System.out.println("Loading small accounts: " + accId);
            }
        }
    }

    /**
     * @return Ext type.
     */
    private static int generateExtType() {
        double rnd = ThreadLocalRandom.current().nextDouble();

        if (rnd < 0.25)
            return AdgEntity.ExtensionType.User.value();
        else if (rnd < 0.5)
            return AdgEntity.ExtensionType.DigitalUser.value();
        else if (rnd < 0.75)
            return AdgEntity.ExtensionType.VirtualUser.value();
        else
            return AdgEntity.ExtensionType.SharedLinesGroup.value();
    }

    /**
     * @return Ext status.
     */
    private static int generateExtStatus() {
        double rnd = ThreadLocalRandom.current().nextDouble();

        if (rnd < 0.5)
            return AdgEntity.ExtensionState.Enabled.value();
        else if (rnd < 0.75)
            return AdgEntity.ExtensionState.Disabled.value();
        else
            return AdgEntity.ExtensionState.Unassigned.value();
    }

    /**
     * Load data for big account.
     *
     * @return Entities.
     * @throws Exception If failed.
     */
    @SuppressWarnings("ConstantConditions")
    private static AdgEntity[] loadFromFileBigAccount() throws Exception {
        try {
            Gson gson = new Gson();

            FileReader rd = new FileReader(Thread.currentThread().getContextClassLoader()
                .getResource("extension_lookup_entry.txt").getFile());

            return gson.fromJson(rd, AdgEntity[].class);
        }
        finally {
            System.out.println("Parsed big account.");
        }
    }

    /**
     * Start topology.
     *
     * @return Client node.
     */
    private static Ignite start() {
        int i = 0;

        for (; i < NODE_CNT; i++)
            Ignition.start(config(i, false));

        return Ignition.start(config(i, true));
    }

    /**
     * Create configuration.
     *
     * @param idx Index.
     * @param client Client flag.
     * @return Configuration.
     */
    @SuppressWarnings("unchecked")
    private static IgniteConfiguration config(int idx, boolean client) {
        IgniteConfiguration cfg = new IgniteConfiguration();

        cfg.setGridName("grid-" + idx);
        cfg.setClientMode(client);

        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setName(CACHE_NAME);
        ccfg.setIndexedTypes(AdgAffinityKey.class, AdgEntity.class);
        ccfg.setAffinityMapper(new AdgAffinityKeyMapper());

        cfg.setMarshaller(new OptimizedMarshaller());

        cfg.setCacheConfiguration(ccfg);

        cfg.setLocalHost("127.0.0.1");

        return cfg;
    }

    /**
     * Query runner.
     */
    private static class QueryExecutor implements Runnable {
        /** Cache. */
        private final IgniteCache<AdgAffinityKey, AdgEntity> cache;

        /**
         * Constructor.
         *
         * @param cache Cache.
         */
        public QueryExecutor(IgniteCache<AdgAffinityKey, AdgEntity> cache) {
            this.cache = cache;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("InfiniteLoopStatement")
        @Override public void run() {
            System.out.println("Executor started: "+ Thread.currentThread().getName());

            while (true) {
                long start = System.nanoTime();

                SqlFieldsQuery qry = new SqlFieldsQuery(QRY_FIRST);

                qry.setArgs((Object[]) argumentForQuery());

                Set<String> extIds = new HashSet<>();

                for (List<?> next : cache.query(qry))
                    extIds.add((String) next.get(0));

                if (!extIds.isEmpty()) {
//                    qry = new SqlFieldsQuery(QRY_SECOND).setArgs(extIds.toArray(), System.currentTimeMillis());
//
//                    consumeResult(cache.query(qry));

//                    qry = new SqlFieldsQuery(QRY_THIRD).setArgs((Object[]) argumentForQuery());
//
//                    consumeResult(cache.query(qry));
                }

                long dur = (System.nanoTime() - start) / 1_000_000;

                CNT.incrementAndGet();

                if (VERBOSE)
                    System.out.println("[extIds=" + extIds.size() + ", dur=" + dur + ']');
            }
        }
    }

    /**
     * Throughput printer.
     */
    private static class ThroughputPrinter implements Runnable {
        /** {@inheritDoc} */
        @SuppressWarnings("InfiniteLoopStatement")
        @Override public void run() {
            while (true) {
                long before = CNT.get();
                long beforeTime = System.currentTimeMillis();

                try {
                    Thread.sleep(2000L);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    throw new RuntimeException(e);
                }

                long after = CNT.get();
                long afterTime = System.currentTimeMillis();

                double res = 1000 * ((double)(after - before)) / (afterTime - beforeTime);

                System.out.println((long)res + " ops/sec");
            }
        }
    }
}
