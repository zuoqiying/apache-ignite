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
package org.apache.ignite.loadtest;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.eviction.EvictionPolicy;
import org.apache.ignite.cache.eviction.fifo.FifoEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;

/**
 * DPLEntityIdManagerTest
 *
 * @author Alexandr Kuramshin <ein.nsk.ru@gmail.com>
 */
public class DPLEntityIdManagerTest extends GridCommonAbstractTest {

    /** Total size of the page cache */
    private static final long PAGE_CACHE_SIZE = 100L << 20; // Mb

    private static final int GRID_COUNT = 4;

    private static final String CACHE_NAME = "DPLcache";

    private static final String SVC_NAME = "DPLservice";

    private static final String SEQ_NAME = "DPLsequence";

    private static final int CACHE_PACK_SIZE = 10;

    private static final int CACHE_MAX_SIZE = 1_000_000;

    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        // Wrong setting
        cfg.setLateAffinityAssignment(true);

        // MemoryConfiguration
        MemoryConfiguration mcfg = new MemoryConfiguration();
        mcfg.setPageCacheSize(PAGE_CACHE_SIZE);
        cfg.setMemoryConfiguration(mcfg);

        // Common cache
        CacheConfiguration ccfg = new CacheConfiguration();
        ccfg.setName(CACHE_NAME);
        ccfg.setCacheMode(CacheMode.PARTITIONED);
        ccfg.setBackups(3);
        ccfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        EvictionPolicy ep = new FifoEvictionPolicy(CACHE_MAX_SIZE);
        ccfg.setEvictionPolicy(ep);
        cfg.setCacheConfiguration(ccfg);

        // Connector
        ConnectorConfiguration con = new ConnectorConfiguration();
        cfg.setConnectorConfiguration(con);

        return cfg;
    }

    public void testMultipleNodes() throws Exception {
        IgniteEx grid = startGrid(0);
        grid.services().deployNodeSingleton(SVC_NAME, new DPLserivce());
        for (int i = 1; i < GRID_COUNT; ++i)
            startGrid(i);
        Random rnd = new Random();
        while (true) {
            Thread.sleep(60_000);
            int i = rnd.nextInt(GRID_COUNT);
            stopGrid(i);
            Thread.sleep(60_000);
            startGrid(i);
        }
    }

    private static class DPLserivce implements Service {

        private final AtomicBoolean running = new AtomicBoolean(true);
        private final CountDownLatch exited = new CountDownLatch(1);

        @IgniteInstanceResource
        public Ignite ignite;

        @LoggerResource
        public IgniteLogger logger;

        private DPLEntityIdManager idManager;

        /** {@inheritDoc} */
        @Override public void cancel(ServiceContext ctx) {
            if (!running.compareAndSet(true, false))
                return;
            try {
                exited.await();
            }
            catch (InterruptedException ignore) {
            }
//            idManager.destroySequence();
        }

        /** {@inheritDoc} */
        @Override public void init(ServiceContext ctx) throws Exception {
            idManager = new DPLEntityIdManager(ignite, SEQ_NAME, 0L);
        }

        /** {@inheritDoc} */
        @Override public void execute(ServiceContext ctx) throws Exception {
            try {
                IgniteCache<Long, Long> cache = ignite.cache(CACHE_NAME);
                while (running.get()) {
                    Transaction tx = ignite.transactions().txStart();
                    try {
                        for (int i = 0; i < CACHE_PACK_SIZE; ++i) {
                            long id = idManager.nextValue();
                            cache.put(id, id);
                        }
                        tx.commit();
                    }
                    catch (Exception ex) {
                        logger.error("DPLservice error", ex);
                        tx.rollback();
                    }
                }
            }
            finally {
                exited.countDown();
            }
        }
    }
}
