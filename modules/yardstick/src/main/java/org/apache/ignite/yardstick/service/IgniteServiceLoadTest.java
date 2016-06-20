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
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteServices;
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
            IgniteServices igniteSrvs = ignite().services();

            String srvName = SERVICE_NAME + UUID.randomUUID();

            ServiceConfiguration srvCfg = new ServiceConfiguration();

            srvCfg.setMaxPerNodeCount(nextRandom(1, 2));
            srvCfg.setTotalCount(nextRandom(1, ignite().cluster().nodes().size()));
            srvCfg.setName(srvName);
            srvCfg.setService(ThreadLocalRandom.current().nextBoolean() ? new NoopService() : new NoopService2());

            igniteSrvs.deploy(srvCfg);

            executeTask();

            NoopService srvc = igniteSrvs.service(srvName);

            if (srvc != null)
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
        ignite().compute().execute(new NoopTask(ignite().cluster().nodes().size()), null);
    }

    /**
     * @return {@code True} if need to start/stop service or perform cache operation.
     */
    private boolean isStartService() {
        return ThreadLocalRandom.current().nextDouble() < 0.8;
    }
}
