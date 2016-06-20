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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

/**
 * Service which starts another services.
 */
class ServiceProducer extends NoopService {
    /** */
    private static final String INNER_SERVICE = "inner-service-";

    /** */
    private transient List<String> srvcNames;

    /** */
    @IgniteInstanceResource
    private Ignite ignite;

    /** {@inheritDoc} */
    @Override public void init(ServiceContext ctx) throws Exception {
        super.init(ctx);

        srvcNames = new ArrayList<>();

        for (int i = 0; i < 10; i++)
            srvcNames.add(INNER_SERVICE + UUID.randomUUID());
    }

    /** {@inheritDoc} */
    @Override public void execute(ServiceContext ctx) throws Exception {
        Ignite ignite0 = this.ignite;

        if (ignite0 != null) {
            IgniteServices igniteSrvcs = ignite0.services();

            for (String name : srvcNames)
                igniteSrvcs.deployClusterSingleton(name, new NoopService());
        }

        super.execute(ctx);
    }

    /** {@inheritDoc} */
    @Override public void cancel(ServiceContext ctx) {
        Ignite ignite0 = ignite;
        if (ignite0 != null && srvcNames != null) {
            List<String> names = new ArrayList<>(srvcNames);

            for (String name : names)
                ignite0.services().cancel(name);
        }

        super.cancel(ctx);
    }

    /**
     * @return Random integer.
     */
    protected int randomInt() throws InterruptedException {
        latch.await();

        return ThreadLocalRandom.current().nextInt();
    }
}
