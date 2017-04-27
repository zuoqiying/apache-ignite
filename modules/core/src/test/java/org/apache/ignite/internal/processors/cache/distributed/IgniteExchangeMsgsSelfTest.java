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

package org.apache.ignite.internal.processors.cache.distributed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.processors.cache.GridCacheProcessor;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsFullMessage;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsSingleMessage;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.spi.communication.CommunicationSpi;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 * Measure exchange overhead.
 */
public class IgniteExchangeMsgsSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setClientMode(gridName.startsWith("client"));

        cfg.setPeerClassLoadingEnabled(true);

        //TestRecordingCommunicationSpi spi = new TestRecordingCommunicationSpi();

//        spi.record(GridDhtPartitionsFullMessage.class, GridDhtPartitionsSingleMessage.class);
//
//        cfg.setCommunicationSpi(spi);

        MemoryConfiguration memCfg = new MemoryConfiguration();

        memCfg.setPageSize(1024);

        memCfg.setPageCacheSize(700 * 1024 * 1024);

        cfg.setMemoryConfiguration(memCfg);

        if (!cfg.isClientMode()) {
            List<CacheConfiguration> ccfgs = new ArrayList<>();

            for (int i = 0; i < 15; i++) {
                CacheConfiguration ccfg = new CacheConfiguration();

                ccfg.setName("test" + i);
                ccfg.setAffinity(new RendezvousAffinityFunction(false, 64));

                ccfgs.add(ccfg);
            }

            cfg.setCacheConfiguration(ccfgs.toArray(new CacheConfiguration[ccfgs.size()]));
        }

        return cfg;
    }

    /** */
    public void testExchange1() throws Exception {
        try {
            System.setProperty(IgniteSystemProperties.IGNITE_START_CACHES_ON_JOIN, "false");

            IgniteEx grid0 = startGrid(0);

            awaitPartitionMapExchange();

            Ignite client1 = startGrid("client1");

            Collection<ClusterNode> nodes = client1.cluster().forClientNodes("test0").nodes();

            assertTrue(nodes.size() == 0);

            //Ignite client2 = startGrid("client2");

        }
        finally {
            stopAllGrids();

            System.clearProperty(IgniteSystemProperties.IGNITE_START_CACHES_ON_JOIN);
        }
    }

    /** */
    public void testExchange2() throws Exception {
        try {
            System.setProperty(IgniteSystemProperties.IGNITE_START_CACHES_ON_JOIN, "true");

            IgniteEx grid0 = startGrid(0);

            awaitPartitionMapExchange();

            Ignite client1 = startGrid("client1");

            Collection<ClusterNode> nodes = client1.cluster().forClientNodes("test0").nodes();

            assertTrue(nodes.size() == 1);

            Ignite client2 = startGrid("client2");

            Collection<ClusterNode> nodes2 = client2.cluster().forClientNodes("test0").nodes();

            assertTrue(nodes2.size() == 1);
        }
        finally {
            stopAllGrids();

            System.clearProperty(IgniteSystemProperties.IGNITE_START_CACHES_ON_JOIN);
        }
    }
}
