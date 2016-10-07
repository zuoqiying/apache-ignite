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

package org.apache.ignite.internal.trace.atomic;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.trace.TraceCluster;
import org.apache.ignite.internal.trace.TraceNodeResult;

import java.util.Collection;

/**
 * Atomic trace runner.
 */
public class AtomicTraceRunner {
    /** Cache name. */
    private static final String CACHE_NAME = "cache";

    /**
     * Entry point.
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        try {
            Ignition.start(config("srv", false));

            Ignite cliNode = Ignition.start(config("cli", true));

            TraceCluster srvTrace = new TraceCluster(cliNode.cluster().forServers());
            TraceCluster cliTrace = new TraceCluster(cliNode.cluster().forClients());

            srvTrace.enable();
            cliTrace.enable();

            IgniteCache cache = cliNode.cache(CACHE_NAME);

            cache.put(1, 1);
            cache.put(2, 2);
            cache.put(3, 3);
            cache.put(4, 4);

            Collection<AtomicTraceResult> ress = AtomicTraceResult.parse(cliTrace.collect(
                AtomicTrace.GRP_CLIENT_REQ_SND,
                AtomicTrace.GRP_CLIENT_REQ_SND_IO
            ));

            for (AtomicTraceResult res : ress)
                System.out.println(res);
        }
        finally {
            Ignition.stopAll(true);
        }
    }

    /**
     * Create configuration.
     *
     * @param name Name.
     * @param client Client flag.
     * @return Configuration.
     */
    private static IgniteConfiguration config(String name, boolean client) {
        IgniteConfiguration cfg = new IgniteConfiguration();

        cfg.setGridName(name);
        cfg.setClientMode(client);
        cfg.setLocalHost("127.0.0.1");

        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setName(CACHE_NAME);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }
}
