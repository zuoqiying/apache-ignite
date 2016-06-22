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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.services.ServiceConfiguration;
import org.apache.ignite.yardstick.IgniteAbstractBenchmark;
import org.apache.ignite.yardstick.IgniteNode;
import org.apache.ignite.yardstick.compute.model.NoopTask;
import org.yardstickframework.BenchmarkUtils;

/**
 *
 */
public class IgniteServiceLoadTest extends IgniteAbstractBenchmark {
    /** Test service name. */
    private static String SERVICE_NAME = "test-service-name-";

    /** */
    final AtomicBoolean restarter = new AtomicBoolean(false);

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        if (!restarter.get() && restarter.compareAndSet(false, true)) {
            BenchmarkUtils.println(cfg, "The thread is restarter. Thread id: " + Thread.currentThread().getName());

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.SECONDS.sleep(10);

                    IgniteNode node = new IgniteNode(false);

                    node.setGridName("restart-grid-name");

                    node.start(cfg);

                    TimeUnit.SECONDS.sleep(10);

                    node.stop();
                }
                catch (InterruptedException e) {
                    BenchmarkUtils.println(cfg, "Restarter thread was interrupted: "
                        + Thread.currentThread().getName());

                    Thread.currentThread().interrupt();
                }
            }

            return true;
        }

        if (isStartService()) {
            final IgniteServices igniteSrvs = ignite().services();

            final String srvName = SERVICE_NAME + UUID.randomUUID() + "-" + UUID.randomUUID();

            ServiceConfiguration srvCfg = new ServiceConfiguration();

            srvCfg.setMaxPerNodeCount(nextRandom(1, 2));
            srvCfg.setTotalCount(nextRandom(1, 2));
            srvCfg.setName(srvName);
            srvCfg.setService(new ServiceProducer());

            igniteSrvs.deploy(srvCfg);

            executeTask();

            TestService srvc = igniteSrvs.serviceProxy(srvName, TestService.class, false);

            try {
                srvc.randomInt();
            }
            catch (Exception e) {
                BenchmarkUtils.println(cfg, "Failed to perform operation.");
            }

            igniteSrvs.cancel(srvName);

            srvc = igniteSrvs.service(srvName);

            if (srvc != null)
                throw new IgniteException("Service wasn't cancelled.");
        }
        else {
            CacheConfiguration cfg = cacheConfiguration();

            IgniteCache cache = ignite().createCache(cfg);

            cache.put(1, 1);

            executeTask();

            ignite().destroyCache(cfg.getName());
        }

        return true;
    }

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
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * @return Cache configuration.
     */
    private CacheConfiguration<Integer, Integer> cacheConfiguration() {
        return new CacheConfiguration<Integer, Integer>("test-cache-name-" + UUID.randomUUID())
            .setCacheMode(CacheMode.PARTITIONED)
            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
            .setBackups(0)
            .setAffinity(new RendezvousAffinityFunction(true, 256));
    }
}
