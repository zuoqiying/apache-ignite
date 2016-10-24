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
import java.util.Map;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.yardstick.cache.IgniteCacheAbstractBenchmark;
import org.yardstickframework.BenchmarkConfiguration;

/**
 *
 */
public class IgniteAdgQueryAbstractBenchmark extends IgniteCacheAbstractBenchmark<String, Object> {
    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        try (IgniteDataStreamer<Object, Object> str = ignite().dataStreamer(cache.getName())) {
            int key = 0;
            String accountId = "accId";

            for (AdgEntity e : loadFromFileBigAccount())
                str.addData(e.getKey(++key), e);

            while (key < args.range() && !Thread.currentThread().isInterrupted()) {
                int key0 = key;

                AdgEntity e = new AdgEntity();

                e.setAccountId(accountId + key0);
                e.setExtensionId(key + "_" + accountId + key0);
                e.setFirstName("John" + key0);
                e.setLastName("Doe" + key0);

                ++key;

                str.addData(e.getKey(key0), e);
            }
        }

        super.setUp(cfg);
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected IgniteCache<String, Object> cache() {
        return ignite().cache("adgCache");
    }

    private AdgEntity[] loadFromFileBigAccount() throws Exception {
        Gson gson = new Gson();

        FileReader rd = new FileReader(Thread.currentThread().getContextClassLoader()
            .getResource("extension_lookup_entry.txt").getFile());

        return gson.fromJson(rd, AdgEntity[].class);
    }
}
