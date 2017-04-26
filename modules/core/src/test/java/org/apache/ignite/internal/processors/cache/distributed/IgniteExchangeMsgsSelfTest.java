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
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
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

        if (gridName.startsWith("client"))
            cfg.setClientMode(true);

        cfg.setPeerClassLoadingEnabled(true);

        //TestRecordingCommunicationSpi spi = new TestRecordingCommunicationSpi();

//        spi.record(GridDhtPartitionsFullMessage.class, GridDhtPartitionsSingleMessage.class);
//
//        cfg.setCommunicationSpi(spi);

        MemoryConfiguration memCfg = new MemoryConfiguration();

        memCfg.setPageSize(1024);

        memCfg.setPageCacheSize(700 * 1024 * 1024);

        cfg.setMemoryConfiguration(memCfg);

        List<CacheConfiguration> ccfgs = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            CacheConfiguration ccfg = new CacheConfiguration();

            ccfg.setName("test" + i);
            ccfg.setAffinity(new RendezvousAffinityFunction(false, 20_000));

            ccfgs.add(ccfg);
        }

        cfg.setCacheConfiguration(ccfgs.toArray(new CacheConfiguration[ccfgs.size()]));

        return cfg;
    }

    /** */
    public void testExchange() throws Exception {
        try {
            IgniteEx grid0 = startGrid(0);

            awaitPartitionMapExchange();

            IgniteCache<Object, Object> cache = grid0.getOrCreateCache(new CacheConfiguration<Object, Object>());

//            TestRecordingCommunicationSpi spi0 = (TestRecordingCommunicationSpi)grid0.configuration().getCommunicationSpi();
//
//            List<Object> crdMessages1 = spi0.recordedMessages(false);

            IgniteEx grid1 = startGrid(1);

            awaitPartitionMapExchange();


            int c = 10;

            while(c-- > 0) {
                IgniteEx grid2 = startGrid(2);

                awaitPartitionMapExchange();

                Thread.sleep(2_000);

                stopGrid(2);

                Thread.sleep(2_000);
            }

            //TestRecordingCommunicationSpi spi1 = (TestRecordingCommunicationSpi)grid1.configuration().getCommunicationSpi();

//            List<Object> crdMessages2 = spi0.recordedMessages(false);
//            List<Object> messages3 = spi1.recordedMessages(false);
//
//            System.out.println("clienttime 0");
//
//            Ignite client1 = startGrid("client1");
//            TestRecordingCommunicationSpi spi2 = (TestRecordingCommunicationSpi)grid0.configuration().getCommunicationSpi();
//
//            long t1 = System.nanoTime();
//
//            IgniteCache<Object, Object> cache2 = client1.getOrCreateCache("x");
//
//            List<Object> objects = spi2.recordedMessages(false);
//
//            long t2 = System.nanoTime();
//
//            System.out.println("clienttime 1: " + (t2-t1)/1000/1000. + " ms");

            LockSupport.park();
        }
        finally {
            stopAllGrids();
        }
    }
}
