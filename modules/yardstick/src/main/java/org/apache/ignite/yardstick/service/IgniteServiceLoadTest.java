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

package org.apache.ignite.yardstick.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.yardstick.IgniteAbstractBenchmark;
import org.apache.ignite.yardstick.compute.model.NoopTask;
import org.yardstickframework.BenchmarkUtils;

/**
 *
 */
public class IgniteServiceLoadTest extends IgniteAbstractBenchmark {
    /** Test service name. */
    private static String SERVICE_NAME = "test-service-name-";

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {

        if (isStartService()) {
            try {
                final IgniteServices igniteSrvs = ignite().services();

                final String srvName = SERVICE_NAME + UUID.randomUUID() + "-" + UUID.randomUUID();

                igniteSrvs.deployClusterSingleton(srvName, new NoopService());

                executeTask();

                igniteSrvs.cancel(srvName);
            }
            catch (Exception e) {
                BenchmarkUtils.println(cfg, "Failed to perform operation.");

                e.printStackTrace();
            }
        }
//        else {
//            try {
//                CacheConfiguration cfg = cacheConfiguration();
//
//                IgniteCache cache = ignite().createCache(cfg);
//
//                cache.put(1, 1);
//
//                executeTask();
//
//                ignite().destroyCache(cfg.getName());
//            }
//            catch (Exception e) {
//                BenchmarkUtils.println(cfg, "Failed to start/stop cache.");
//
//                e.printStackTrace();
//            }
//        }

        return true;
    }

    /**
     * @return Cache configuration.
     */
//    private CacheConfiguration<Integer, Integer> cacheConfiguration() {
//        return new CacheConfiguration<Integer, Integer>("test-cache-name-" + UUID.randomUUID())
//            .setCacheMode(CacheMode.PARTITIONED)
//            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
//            .setBackups(0)
//            .setAffinity(new RendezvousAffinityFunction(true, 256))
//            .setStartSize(8);
//    }

    /**
     * Execute noop task.
     */
    private void executeTask() {
        ignite().compute().execute(new NoopTask(1), null);
    }

    /**
     * @return {@code True} if need to start/stop service or perform cache operation.
     */
    private boolean isStartService() {
        return ThreadLocalRandom.current().nextDouble() < 0.8;
    }
}
