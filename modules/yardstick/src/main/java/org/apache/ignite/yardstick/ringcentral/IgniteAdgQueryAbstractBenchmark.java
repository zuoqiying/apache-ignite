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
import java.io.FileReader;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.yardstick.cache.IgniteCacheAbstractBenchmark;
import org.yardstickframework.BenchmarkConfiguration;

/**
 *
 */
abstract class IgniteAdgQueryAbstractBenchmark extends IgniteCacheAbstractBenchmark<String, Object> {
    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        try (IgniteDataStreamer<Object, Object> str = ignite().dataStreamer(cache.getName())) {
            int key = 0;

            // Loading one big account.
            for (AdgEntity e : loadFromFileBigAccount())
                str.addData(e.getKey(++key), e);

            int accId = 0;

            // Loading many small accounts.
            while (accId < args.range() && !Thread.currentThread().isInterrupted()) {
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
            }
        }
    }

    /**
     * @return Ext type.
     */
    private int generateExtType() {
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
    private int generateExtStatus() {
        double rnd = ThreadLocalRandom.current().nextDouble();

        if (rnd < 0.5)
            return AdgEntity.ExtensionState.Enabled.value();
        else if (rnd < 0.75)
            return AdgEntity.ExtensionState.Disabled.value();
        else
            return AdgEntity.ExtensionState.Unassigned.value();
    }

    /** {@inheritDoc} */
    @Override protected IgniteCache<String, Object> cache() {
        return ignite().cache("AdgEntry");
    }

    private AdgEntity[] loadFromFileBigAccount() throws Exception {
        Gson gson = new Gson();

        FileReader rd = new FileReader(Thread.currentThread().getContextClassLoader()
            .getResource("extension_lookup_entry.txt").getFile());

        AdgEntity[] entities = gson.fromJson(rd, AdgEntity[].class);

        return entities;
    }
}
