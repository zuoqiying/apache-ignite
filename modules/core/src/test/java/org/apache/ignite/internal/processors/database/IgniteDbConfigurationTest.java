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

package org.apache.ignite.internal.processors.database;

import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DatabaseConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 *
 */
public class IgniteDbConfigurationTest extends GridCommonAbstractTest {
    /** */
    private static final TcpDiscoveryIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);

    /** */
    private DatabaseConfiguration dbCfg;

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        dbCfg = null;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setDatabaseConfiguration(dbCfg);

        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        ccfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);

        ccfg.setRebalanceMode(CacheRebalanceMode.SYNC);

        CacheConfiguration ccfg2 = new CacheConfiguration("non-primitive");

        ccfg2.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        ccfg2.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);

        ccfg2.setRebalanceMode(CacheRebalanceMode.SYNC);

        cfg.setCacheConfiguration(ccfg, ccfg2);

        TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        cfg.setMarshaller(null);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testDefaultDbConfig() throws Exception {
        tryStartGrid("Failed to start grid with default Db config.");
    }

    /**
     * @throws Exception If failed.
     */
    public void testIncorrectPageSize() throws Exception {
        dbCfg = createDefaultDbConfig();

        dbCfg.setPageSize(0);

        tryStartGrid("Failed to start with zero page size.");

        dbCfg.setPageSize(-1);

        tryStartGrid("Failed to start with negative page size.");
    }

    /**
     * @throws Exception If failed.
     */
    public void testIncorrectPageCacheSize() throws Exception {
        dbCfg = createDefaultDbConfig();

        dbCfg.setPageCacheSize(0);

        tryStartGrid("Failed to start with zero page cache size.");

        dbCfg.setPageCacheSize(-1);

        tryStartGrid("Failed to start with negative page cache size.");
    }

    /**
     * @param msg Message to be printed in case of failure.
     * @throws Exception If failed.
     */
    @SuppressWarnings("EmptyTryBlock")
    private void tryStartGrid(final String msg) throws Exception {
        try (final Ignite ignored = startGrid()) {
            // No op.
        }
        catch (Throwable e) {
            e.printStackTrace();

            fail(msg + " " + e.getMessage());
        }
    }

    /**
     * @return Database configuration with preset configs.
     */
    private DatabaseConfiguration createDefaultDbConfig() {
        final DatabaseConfiguration dbCfg = new DatabaseConfiguration();

        dbCfg.setConcurrencyLevel(Runtime.getRuntime().availableProcessors() * 4);

        dbCfg.setPageSize(1024);

        dbCfg.setPageCacheSize(100 * 1024 * 1024);

        return dbCfg;
    }
}
