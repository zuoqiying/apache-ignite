/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.cache.database.standbycluster;

import java.util.Map;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.PersistentStoreConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.cache.GridCacheAdapter;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 *
 */
public class IgniteStandByClusterTest extends GridCommonAbstractTest {
    private static final TcpDiscoveryIpFinder vmIpFinder = new TcpDiscoveryVmIpFinder(true);

    public void testNotStartDynamicCachesOnClientAfterActivation() throws Exception {
        final String cacheName0 = "cache0";
        final String cacheName = "cache";

        IgniteConfiguration cfg1 = getConfiguration("serv1");
        cfg1.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(vmIpFinder));
        cfg1.setPersistentStoreConfiguration(new PersistentStoreConfiguration());

        IgniteConfiguration cfg2 = getConfiguration("serv2");
        cfg2.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(vmIpFinder));
        cfg2.setPersistentStoreConfiguration(new PersistentStoreConfiguration());

        IgniteConfiguration cfg3 = getConfiguration("client");
        cfg3.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(vmIpFinder));
        cfg3.setPersistentStoreConfiguration(new PersistentStoreConfiguration());
        cfg3.setCacheConfiguration(new CacheConfiguration(cacheName0));

        cfg3.setClientMode(true);

        IgniteEx ig1 = startGrid(cfg1);
        IgniteEx ig2 = startGrid(cfg2);
        IgniteEx ig3 = startGrid(cfg3);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!ig3.active());

        ig3.active(true);

        assertTrue(ig1.active());
        assertTrue(ig2.active());
        assertTrue(ig3.active());

        ig3.createCache(new CacheConfiguration<>(cacheName));

        assertNotNull(ig3.cache(cacheName));
        assertNotNull(ig1.cache(cacheName));
        assertNotNull(ig2.cache(cacheName));

        assertNotNull(ig1.cache(cacheName0));
        assertNotNull(ig3.cache(cacheName0));
        assertNotNull(ig2.cache(cacheName0));

        ig3.active(false);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!ig3.active());

        ig3.active(true);

        assertTrue(ig1.active());
        assertTrue(ig2.active());
        assertTrue(ig3.active());

        assertNotNull(ig1.cache(cacheName));
        assertNotNull(ig2.cache(cacheName));

        Map<String, GridCacheAdapter<?, ?>> caches = U.field(ig3.context().cache(), "caches");

        // Only system caches and cache0
        assertTrue(caches.size() == 3);

        assertNull(caches.get(cacheName));

        assertNotNull(caches.get(cacheName0));

        assertNotNull(ig3.cache(cacheName));
    }

    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        deleteRecursively(U.resolveWorkDirectory(U.defaultWorkDirectory(), "db", true));
    }
    @Override protected void afterTest() throws Exception {
        super.beforeTest();

        deleteRecursively(U.resolveWorkDirectory(U.defaultWorkDirectory(), "db", true));
    }
}
