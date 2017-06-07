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

package org.apache.ignite.cache.database.standbycluster.reconnect;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.PersistentStoreConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.managers.discovery.GridDiscoveryManager;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.spi.discovery.DiscoverySpiCustomMessage;
import org.apache.ignite.spi.discovery.DiscoverySpiListener;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.events.EventType.EVT_CLIENT_NODE_DISCONNECTED;

/**
 *
 */
public class IgniteStandByClientReconnectTest extends GridCommonAbstractTest {
    private static final TcpDiscoveryIpFinder vmIpFinder = new TcpDiscoveryVmIpFinder(true);

    @Override protected IgniteConfiguration getConfiguration(String name) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(name);

        cfg.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(vmIpFinder));
        cfg.setPersistentStoreConfiguration(new PersistentStoreConfiguration());
        cfg.setConsistentId(name);

        return cfg;
    }

    public void testActiveClientReconnectToActiveCluster() throws Exception {
        IgniteConfiguration cfg1 = getConfiguration("node1");
        IgniteConfiguration cfg2 = getConfiguration("node2");
        IgniteConfiguration cfg3 = getConfiguration("client");

        CountDownLatch activateLatch = new CountDownLatch(1);

        cfg3.setDiscoverySpi(new AwaitTcpDiscoverySpi(activateLatch).setIpFinder(vmIpFinder));

        cfg3.setClientMode(true);

        IgniteEx ig1 = startGrid(cfg1);
        IgniteEx ig2 = startGrid(cfg2);
        IgniteEx client = startGrid(cfg3);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!client.active());

        client.active(true);

        assertTrue(ig1.active());
        assertTrue(ig2.active());
        assertTrue(client.active());

        final CountDownLatch disconnectedLatch = new CountDownLatch(1);
        final CountDownLatch reconnectedLatch = new CountDownLatch(1);

        GridDiscoveryManager discMngr = client.context().discovery();

        client.events().localListen(new IgnitePredicate<Event>() {

            @Override public boolean apply(Event event) {
                switch (event.type()) {
                    case EventType.EVT_CLIENT_NODE_DISCONNECTED:
                        info("Client disconnected");

                        disconnectedLatch.countDown();

                        break;
                    case EventType.EVT_CLIENT_NODE_RECONNECTED:
                        info("Client reconnected");

                        reconnectedLatch.countDown();
                }

                return true;
            }
        }, EventType.EVT_CLIENT_NODE_DISCONNECTED, EventType.EVT_CLIENT_NODE_RECONNECTED);

        stopGrid("node1");
        stopGrid("node2");

        disconnectedLatch.await(10, TimeUnit.SECONDS);

        ig1 = startGrid(getConfiguration("node1"));
        ig2 = startGrid(getConfiguration("node2"));

        ig1.active(true);

        assertTrue(ig1.active());
        assertTrue(ig2.active());

        activateLatch.countDown();

        reconnectedLatch.await(10, TimeUnit.SECONDS);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!client.active());
    }

    public void testActiveClientReconnectToInActiveCluster() throws Exception {
        IgniteConfiguration cfg1 = getConfiguration("node1");
        IgniteConfiguration cfg2 = getConfiguration("node2");
        IgniteConfiguration cfg3 = getConfiguration("client");

        cfg3.setClientMode(true);

        IgniteEx ig1 = startGrid(cfg1);
        IgniteEx ig2 = startGrid(cfg2);
        IgniteEx client = startGrid(cfg3);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!client.active());

        client.active(true);

        assertTrue(ig1.active());
        assertTrue(ig2.active());
        assertTrue(client.active());

        final CountDownLatch disconnectedLatch = new CountDownLatch(1);
        final CountDownLatch reconnectedLatch = new CountDownLatch(1);

        client.events().localListen(new IgnitePredicate<Event>() {

            @Override public boolean apply(Event event) {
                switch (event.type()) {
                    case EventType.EVT_CLIENT_NODE_DISCONNECTED:
                        info("Client disconnected");

                        disconnectedLatch.countDown();

                        break;
                    case EventType.EVT_CLIENT_NODE_RECONNECTED:
                        info("Client reconnected");

                        reconnectedLatch.countDown();
                }

                return true;
            }
        }, EventType.EVT_CLIENT_NODE_DISCONNECTED, EventType.EVT_CLIENT_NODE_RECONNECTED);

        stopGrid("node1");
        stopGrid("node2");

        assertTrue(client.active());

        disconnectedLatch.await(10, TimeUnit.SECONDS);

        ig1 = startGrid(getConfiguration("node1"));
        ig2 = startGrid(getConfiguration("node2"));

        reconnectedLatch.await(10, TimeUnit.SECONDS);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!client.active());

        client.active(true);

        assertTrue(ig1.active());
        assertTrue(ig2.active());
        assertTrue(client.active());
    }

    public void testInActiveClientReconnectToActiveCluster() throws Exception {
        IgniteConfiguration cfg1 = getConfiguration("node1");
        IgniteConfiguration cfg2 = getConfiguration("node2");
        IgniteConfiguration cfg3 = getConfiguration("client");

        CountDownLatch activateLatch = new CountDownLatch(1);

        cfg3.setDiscoverySpi(new AwaitTcpDiscoverySpi(activateLatch).setIpFinder(vmIpFinder));

        cfg3.setClientMode(true);

        IgniteEx ig1 = startGrid(cfg1);
        IgniteEx ig2 = startGrid(cfg2);
        IgniteEx client = startGrid(cfg3);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!client.active());

        final CountDownLatch disconnectedLatch = new CountDownLatch(1);
        final CountDownLatch reconnectedLatch = new CountDownLatch(1);

        GridDiscoveryManager discMngr = client.context().discovery();

        client.events().localListen(new IgnitePredicate<Event>() {

            @Override public boolean apply(Event event) {
                switch (event.type()) {
                    case EventType.EVT_CLIENT_NODE_DISCONNECTED:
                        info("Client disconnected");

                        disconnectedLatch.countDown();

                        break;
                    case EventType.EVT_CLIENT_NODE_RECONNECTED:
                        info("Client reconnected");

                        reconnectedLatch.countDown();
                }

                return true;
            }
        }, EventType.EVT_CLIENT_NODE_DISCONNECTED, EventType.EVT_CLIENT_NODE_RECONNECTED);

        stopGrid("node1");
        stopGrid("node2");

        disconnectedLatch.await(10, TimeUnit.SECONDS);

        ig1 = startGrid(getConfiguration("node1"));
        ig2 = startGrid(getConfiguration("node2"));

        ig1.active(true);

        assertTrue(ig1.active());
        assertTrue(ig2.active());

        activateLatch.countDown();

        reconnectedLatch.await(10, TimeUnit.SECONDS);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!client.active());
    }

    public void testInActiveClientReconnectToInActiveCluster() throws Exception {
        IgniteConfiguration cfg1 = getConfiguration("node1");
        IgniteConfiguration cfg2 = getConfiguration("node2");
        IgniteConfiguration cfg3 = getConfiguration("client");

        cfg3.setClientMode(true);

        IgniteEx ig1 = startGrid(cfg1);
        IgniteEx ig2 = startGrid(cfg2);
        IgniteEx client = startGrid(cfg3);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!client.active());

        final CountDownLatch disconnectedLatch = new CountDownLatch(1);
        final CountDownLatch reconnectedLatch = new CountDownLatch(1);

        client.events().localListen(new IgnitePredicate<Event>() {

            @Override public boolean apply(Event event) {
                switch (event.type()) {
                    case EventType.EVT_CLIENT_NODE_DISCONNECTED:
                        info("Client disconnected");

                        disconnectedLatch.countDown();

                        break;
                    case EventType.EVT_CLIENT_NODE_RECONNECTED:
                        info("Client reconnected");

                        reconnectedLatch.countDown();
                }

                return true;
            }
        }, EventType.EVT_CLIENT_NODE_DISCONNECTED, EventType.EVT_CLIENT_NODE_RECONNECTED);

        stopGrid("node1");
        stopGrid("node2");

        assertTrue(!client.active());

        disconnectedLatch.await(10, TimeUnit.SECONDS);

        ig1 = startGrid(getConfiguration("node1"));
        ig2 = startGrid(getConfiguration("node2"));

        reconnectedLatch.await(10, TimeUnit.SECONDS);

        assertTrue(!ig1.active());
        assertTrue(!ig2.active());
        assertTrue(!client.active());

        client.active(true);

        assertTrue(ig1.active());
        assertTrue(ig2.active());
        assertTrue(client.active());
    }

    private static class AwaitTcpDiscoverySpi extends TcpDiscoverySpi {

        private final CountDownLatch latch;

        private AwaitTcpDiscoverySpi(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override public void setListener(@Nullable DiscoverySpiListener lsnr) {
            super.setListener(new AwaitDiscoverySpiListener(latch, lsnr));
        }
    }

    private static class AwaitDiscoverySpiListener implements DiscoverySpiListener {

        private final CountDownLatch latch;

        private final DiscoverySpiListener delegate;

        private AwaitDiscoverySpiListener(
            CountDownLatch latch,
            DiscoverySpiListener delegate
        ) {
            this.latch = latch;
            this.delegate = delegate;
        }

        @Override public void onLocalNodeInitialized(ClusterNode locNode) {
            delegate.onLocalNodeInitialized(locNode);
        }

        @Override public void onDiscovery(
            int type,
            long topVer,
            ClusterNode node,
            Collection<ClusterNode> topSnapshot,
            @Nullable Map<Long, Collection<ClusterNode>> topHist,
            @Nullable DiscoverySpiCustomMessage data
        ) {
            delegate.onDiscovery(type, topVer, node, topSnapshot, topHist, data);

            if (type == EVT_CLIENT_NODE_DISCONNECTED)
                try {
                    System.out.println("Await cluster change state");

                    latch.await();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        stopAllGrids();

        deleteRecursively(U.resolveWorkDirectory(U.defaultWorkDirectory(), "db", true));
    }

    @Override protected void afterTest() throws Exception {
        super.beforeTest();

        stopAllGrids();

        deleteRecursively(U.resolveWorkDirectory(U.defaultWorkDirectory(), "db", true));
    }
}
