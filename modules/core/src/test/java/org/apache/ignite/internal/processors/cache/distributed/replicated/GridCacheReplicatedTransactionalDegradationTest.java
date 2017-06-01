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

package org.apache.ignite.internal.processors.cache.distributed.replicated;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

/**
 * Tests replicated cache performance .
 */
public class GridCacheReplicatedTransactionalDegradationTest extends GridCommonAbstractTest {
    /** Keys. */
    private static final int KEYS = 100_000;

    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        MemoryConfiguration memCfg = new MemoryConfiguration();
        memCfg.setPageCacheSize(500 * 1024 * 1024);

        cfg.setMemoryConfiguration(memCfg);

        cfg.setClientMode(gridName.startsWith("client"));

        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setCacheMode(CacheMode.REPLICATED);
        ccfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        ccfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
        ccfg.setName("test");

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /** */
    public void testThroughput() throws Exception {
        try {
            IgniteEx grid0 = startGrid(0);

            Ignite client = startGrid("client");

            IgniteCache<Object, Object> cache = client.getOrCreateCache("test");

            doTest(client, cache);

            startGrid(1);

            doTest(client, cache);

            startGrid(2);

            doTest(client, cache);
        } finally {
            stopAllGrids();
        }
    }

    /**
     * @param client
     * @param cache Cache.
     */
    private void doTest(Ignite client, IgniteCache<Object, Object> cache) {
        long t1 = System.currentTimeMillis();

        for (int i = 0; i < KEYS; i++) {
            //try (Transaction tx = client.transactions().txStart(TransactionConcurrency.PESSIMISTIC, TransactionIsolation.REPEATABLE_READ)) {
                cache.put(i, i);

                //tx.commit();
            //}
        }

        log.info("TPS: " + Math.round(KEYS / ((float)(System.currentTimeMillis() - t1) / 1000.)));
    }
}