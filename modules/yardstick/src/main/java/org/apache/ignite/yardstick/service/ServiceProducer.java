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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

/**
 * Noop service.
 */
class ServiceProducer implements Service {
    /** */
    private static final String INNER_SERVICE = "inner-service-";

    /** */
    private Set<String> srvcs = new HashSet<>();

    /** */
    private final Object mux = new Object();

    /** */
    @IgniteInstanceResource
    private Ignite ignite;

    /** {@inheritDoc} */
    @Override public void cancel(ServiceContext ctx) {
        synchronized (mux) {
            IgniteServices igniteSrvcs = ignite.services();

            for (String srvc : srvcs)
                igniteSrvcs.cancel(srvc);

            srvcs.clear();
            srvcs = null;
        }
    }

    /** {@inheritDoc} */
    @Override public void init(ServiceContext ctx) throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void execute(ServiceContext ctx) throws Exception {
        synchronized (mux) {
            IgniteServices igniteSrvcs = ignite.services();

            for (int i = 0; i < 10; i++) {
                String srvName = INNER_SERVICE + UUID.randomUUID();

                srvcs.add(srvName);

                igniteSrvcs.deployClusterSingleton(srvName, new NoopService());
            }
        }
    }

    /**
     * @return Random integer.
     */
    protected int randomInt() {
        return ThreadLocalRandom.current().nextInt();
    }
}
