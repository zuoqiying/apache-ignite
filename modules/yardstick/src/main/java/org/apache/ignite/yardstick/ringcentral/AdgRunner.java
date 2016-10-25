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

import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Query runner.
 */
@SuppressWarnings("FieldCanBeLocal")
public class AdgRunner {
    /** Node count. */
    private static final int NODE_CNT = 1;

    /** Number of small accounts. */
    private static final int SMALL_ACC_CNT = 200_000;

    /** Repeat count. */
    private static final long REPEAT_CNT = 1;

    /** Cache name. */
    private static final String CACHE_NAME = "cache";

    /** */
    private static String QRY_FIRST = "select extensionId, " +
        "min(extensionStatus), " +
        "max(concat(firstName,' ',lastName)), " +
        "min(phoneNumber), " +
        "max(extensionNumber) " +
        "from \"AdgEntry\".ADGENTITY " +
        "where accountId = ? " +
        "and extensionType in (?,?) " +
        "and extensionStatus in (?,?) " +
        "and deleteTime > ? " +
        "and ((lcase(concat(firstName,' ',lastName)) like '%john%') " +
        "OR (lcase(phoneNumber) like '%john%') " +
        "OR (lcase(extensionNumber) like '%john%')) " +
        "group by extensionId  order by 2 asc, 3 desc, 4 asc, 5 desc limit 100 offset 0";

    /** */
    private static String QRY_SECOND = "select _val FROM \"AdgEntry\".ADGENTITY " +
        "join table(temp_extensionId VARCHAR = ?) i " +
        "on (extensionId = i.temp_extensionId and deleteTime > ?)";

    /** */
    private static String QRY_THIRD = "select count(distinct extensionId) from \"AdgEntry\".ADGENTITY " +
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
        try {
            Ignite ignite = start();

            IgniteCache<AdgAffinityKey, AdgEntity> cache = ignite.cache(CACHE_NAME);

            loadData(ignite, cache);

            for (int i = 0; i < REPEAT_CNT; i++) {
                long start = System.nanoTime();

                SqlFieldsQuery qry = new SqlFieldsQuery(QRY_FIRST);

                qry.setArgs((Object[])argumentForQuery());

                Set<String> extIds = new HashSet<>();

                for (List<?> next : cache.query(qry))
                    extIds.add((String)next.get(0));

                if (!extIds.isEmpty()) {
                    qry = new SqlFieldsQuery(QRY_SECOND).setArgs(extIds.toArray(), System.currentTimeMillis());

                    consumeResult(cache.query(qry));

                    qry = new SqlFieldsQuery(QRY_THIRD).setArgs((Object[])argumentForQuery());

                    consumeResult(cache.query(qry));
                }

                long dur = (System.nanoTime() - start) / 1_000_000;

                System.out.println("Run=" + i + ", dur=" + dur + ']');
            }
        }
        finally {
            Ignition.stopAll(true);
        }
    }

    /**
     * @return Argument for first query.
     */
    private static String[] argumentForQuery() {
        return new String[]{
            "10k",
            AdgEntity.ExtensionType.randomValue(),
            AdgEntity.ExtensionType.randomValue(),
            AdgEntity.ExtensionState.randomValue(),
            AdgEntity.ExtensionState.randomValue(),
            String.valueOf(System.currentTimeMillis())
        };
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
            for (AdgEntity e : loadFromFileBigAccount())
                str.addData(e.getKey(++key), e);

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
                    String extType = generateExtType();
                    String extStatus = generateExtStatus();

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
            }
        }
    }

    /**
     * @return Ext type.
     */
    private static String generateExtType() {
        double rnd = ThreadLocalRandom.current().nextDouble();

        if (rnd < 0.25)
            return AdgEntity.ExtensionType.User.toString();
        else if (rnd < 0.5)
            return AdgEntity.ExtensionType.DigitalUser.toString();
        else if (rnd < 0.75)
            return AdgEntity.ExtensionType.VirtualUser.toString();
        else
            return AdgEntity.ExtensionType.SharedLinesGroup.toString();
    }

    /**
     * @return Ext status.
     */
    private static String generateExtStatus() {
        double rnd = ThreadLocalRandom.current().nextDouble();

        if (rnd < 0.5)
            return AdgEntity.ExtensionState.Enabled.toString();
        else if (rnd < 0.75)
            return AdgEntity.ExtensionState.Disabled.toString();
        else
            return AdgEntity.ExtensionState.Unassigned.toString();
    }

    /**
     * Load data for big account.
     *
     * @return Entities.
     * @throws Exception If failed.
     */
    @SuppressWarnings("ConstantConditions")
    private static AdgEntity[] loadFromFileBigAccount() throws Exception {
        Gson gson = new Gson();

        FileReader rd = new FileReader(Thread.currentThread().getContextClassLoader()
            .getResource("extension_lookup_entry.txt").getFile());

        return gson.fromJson(rd, AdgEntity[].class);
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

        return Ignition.start(config(i + 1, true));
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
        ccfg.setIndexedTypes(AdgAffinityKey.class, AdgAffinityKeyMapper.class);
        ccfg.setAffinityMapper(new AdgAffinityKeyMapper());

        cfg.setCacheConfiguration(ccfg);

        cfg.setLocalHost("127.0.0.1");

        return cfg;
    }
}
