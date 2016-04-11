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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.ignite.Ignite;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.managers.communication.GridIoMessage;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsFullMessage;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsSingleMessage;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 *
 */
public class IgniteOptimizedPartitionsExchangeTest extends GridCommonAbstractTest {
    /** */
    protected static final TcpDiscoveryIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);

    /** */
    private boolean client;

    /** */
    private ThreadLocal<IgnitePredicate<GridIoMessage>> blockP = new ThreadLocal<>();

    /** */
    private final Map<String, TestRecordingCommunicationSpi> commSpis = new HashMap<>();

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setIpFinder(ipFinder);

        cfg.setClientMode(client);

        CacheConfiguration ccfg = new CacheConfiguration();

        cfg.setCacheConfiguration(ccfg);

        TestRecordingCommunicationSpi commSpi = new TestRecordingCommunicationSpi();

        IgnitePredicate<GridIoMessage> p = blockP.get();

        if (p != null) {
            commSpi.blockMessages(p);

            blockP.remove();
        }

        synchronized (commSpis) {
            commSpis.put(gridName, commSpi);

            commSpis.notifyAll();
        }

        cfg.setCommunicationSpi(commSpi);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        super.afterTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoin() throws Exception {
        Ignite ignite0 = startGrid(0);

        TestRecordingCommunicationSpi spi0 = waitSpi(getTestGridName(0));

        spi0.record(GridDhtPartitionsFullMessage.class);

        IgniteInternalFuture<?> fut1 = GridTestUtils.runAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                blockP.set(blockSinglePartitions(2));

                startGrid(1);

                return null;
            }
        }, "start-1");

        ((IgniteKernal)ignite0).context().discovery().topologyFuture(2).get();

        IgniteInternalFuture<?> fut2 = GridTestUtils.runAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                blockP.set(blockSinglePartitions(3));

                startGrid(2);

                return null;
            }
        }, "start-2");

        TestRecordingCommunicationSpi spi1 = waitSpi(getTestGridName(1));
        TestRecordingCommunicationSpi spi2 = waitSpi(getTestGridName(2));

        ((IgniteKernal)ignite0).context().discovery().topologyFuture(3).get();

        spi1.stopBlock();
        spi2.stopBlock();

        fut1.get();
        fut2.get();

        assertEquals(2, spi0.recordedMessages().size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinJoinedLeft() throws Exception {
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinClients() throws Exception {

    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinCoordinatorLeft() throws Exception {
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleJoinJoinedStartCache() throws Exception {
    }

    /**
     * @throws Exception If failed.
     */
    public void testJoiningNodeBecomeCoordinator() throws Exception {
    }

    /**
     * @param topVer Message topology version.
     * @return Message block predicate.
     */
    private IgnitePredicate<GridIoMessage> blockSinglePartitions(final long topVer) {
        return new IgnitePredicate<GridIoMessage>() {
            @Override public boolean apply(GridIoMessage msg0) {
                Message msg = msg0.message();

                if (msg instanceof GridDhtPartitionsSingleMessage) {
                    GridDhtPartitionsSingleMessage pMsg = (GridDhtPartitionsSingleMessage)msg;

                    return pMsg.exchangeId().topologyVersion().topologyVersion() == topVer;
                }

                return false;
            }
        };
    }

    /**
     * @param nodeName Node name.
     * @return Communication SPI.
     * @throws Exception If failed.
     */
    private TestRecordingCommunicationSpi waitSpi(String nodeName) throws Exception {
        synchronized (commSpis) {
            while (!commSpis.containsKey(nodeName))
                commSpis.wait();

            return commSpis.get(nodeName);
        }
    }
}
