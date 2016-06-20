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
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.util.lang.GridAbsPredicate;
import org.apache.ignite.internal.util.typedef.PA;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.services.ServiceConfiguration;
import org.apache.ignite.yardstick.IgniteAbstractBenchmark;
import org.apache.ignite.yardstick.compute.model.NoopTask;

/**
 *
 */
public class IgniteServiceLoadTest extends IgniteAbstractBenchmark {
    /** Test service name. */
    private static String SERVICE_NAME = "test-service-name-";

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        if (isStartService()) {
            final IgniteServices igniteSrvs = ignite().services();

            final String srvName = SERVICE_NAME + UUID.randomUUID();

            ServiceConfiguration srvCfg = new ServiceConfiguration();

            srvCfg.setMaxPerNodeCount(nextRandom(1, 2));
            srvCfg.setTotalCount(nextRandom(1, ignite().cluster().nodes().size()));
            srvCfg.setName(srvName);
            srvCfg.setService(ThreadLocalRandom.current().nextBoolean() ? new ServiceProducer() : new NoopService());

            igniteSrvs.deploy(srvCfg);

            executeTask();

            if (!waitForCondition(new PA() {
                @Override public boolean apply() {
                    return igniteSrvs.service(srvName) != null;
                }
            }, TimeUnit.SECONDS.toMillis(3))) {
                throw new IgniteException("Service wan't deployed.");
            }

            NoopService srvc = igniteSrvs.service(srvName);

            srvc.randomInt();

            igniteSrvs.cancel(srvName);

            srvc = igniteSrvs.service(srvName);

            if (srvc != null)
                throw new IgniteException("Service wasn't cancelled.");
        }
        else {
            for (int i = 0; i < args.jobs(); i++)
                executeTask();
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
        return ThreadLocalRandom.current().nextDouble() < 0.8;
    }


    /**
     * Waits for condition, polling in busy wait loop.
     *
     * @param cond Condition to wait for.
     * @param timeout Max time to wait in milliseconds.
     * @return {@code true} if condition was achieved, {@code false} otherwise.
     * @throws org.apache.ignite.internal.IgniteInterruptedCheckedException If interrupted.
     */
    public static boolean waitForCondition(GridAbsPredicate cond, long timeout)
        throws IgniteInterruptedCheckedException {
        long curTime = U.currentTimeMillis();
        long endTime = curTime + timeout;

        if (endTime < 0)
            endTime = Long.MAX_VALUE;

        while (curTime < endTime) {
            if (cond.apply())
                return true;

            U.sleep(200);

            curTime = U.currentTimeMillis();
        }

        return false;
    }
}
