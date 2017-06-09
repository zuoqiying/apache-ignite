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

package org.apache.ignite.internal.processors.cache.query.continuous;

import java.util.concurrent.atomic.AtomicLong;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryUpdatedListener;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteClientReconnectAbstractTest;
import org.apache.ignite.internal.util.typedef.PA;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.testframework.GridTestUtils;

import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;

/**
 *
 */
public class IgniteCacheContinuousQueryClientReconnectTest extends IgniteClientReconnectAbstractTest {
    /** {@inheritDoc} */
    @Override protected int serverCount() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override protected int clientCount() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setCacheMode(PARTITIONED);
        ccfg.setAtomicityMode(atomicMode());
        ccfg.setWriteSynchronizationMode(FULL_SYNC);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /**
     * @return Atomic mode.
     */
    protected CacheAtomicityMode atomicMode() {
        return ATOMIC;
    }

    /**
     * @throws Exception If failed.
     */
    public void testReconnectClient() throws Exception {
        Ignite client = grid(serverCount());

        Ignite srv = clientRouter(client);

        assertTrue(client.cluster().localNode().isClient());

        final CacheEventListener lsnr = new CacheEventListener();

        ContinuousQuery<Object, Object> qry = new ContinuousQuery<>();

        qry.setLocalListener(lsnr);

        IgniteCache<Object, Object> clnCache = client.cache(null);

        QueryCursor<?> cur = clnCache.query(qry);

        final int keyCnt = 1;

        for (int i = 0; i < 10; i++) {
            if (i == 2) {
                int z = 0;

                ++z;
            }

            System.out.println("Start iteration: " + i);
            lsnr.cntr.set(0);

            for (int key = 0; key < keyCnt; key++)
                clnCache.put(key, key);

            GridTestUtils.waitForCondition(new PA() {
                @Override public boolean apply() {
                    return keyCnt == lsnr.cntr.get();
                }
            }, 5000);

            assertEquals("Iteration: " + i, keyCnt, lsnr.cntr.get());

            reconnectClientNode(client, srv, null);

            lsnr.cntr.set(0);

            for (int key = 0; key < keyCnt; key++)
                clnCache.put(key, key);

            GridTestUtils.waitForCondition(new PA() {
                @Override public boolean apply() {
                    return keyCnt == lsnr.cntr.get();
                }
            }, 5000);

            System.out.println("Assertion iteration: " + i);
            assertEquals("Iteration: " + i, keyCnt, lsnr.cntr.get());
        }

        cur.close();
    }

    /**
     * @throws Exception If failed.
     */
    public void testReconnectClientAndLeftRouter() throws Exception {
        Ignite client = grid(serverCount());

        final Ignite srv = clientRouter(client);

        final String clnRouterName = srv.name();

        assertTrue(client.cluster().localNode().isClient());

        final CacheEventListener lsnr = new CacheEventListener();

        ContinuousQuery<Object, Object> qry = new ContinuousQuery<>();

        qry.setLocalListener(lsnr);

        IgniteCache<Object, Object> clnCache = client.cache(null);

        QueryCursor<?> cur = clnCache.query(qry);

        lsnr.cntr.set(0);

        final int keyCnt = 100;

        for (int key = 0; key < keyCnt; key++)
            clnCache.put(key, key);

        boolean r = GridTestUtils.waitForCondition(new PA() {
            @Override public boolean apply() {
                return keyCnt == lsnr.cntr.get();
            }
        }, 5000);

        assertTrue("Failed to wait for event.", r);

        reconnectClientNode(client, srv, new Runnable() {
            @Override public void run() {
                stopGrid(clnRouterName);
            }
        });

        assertFalse("Client connected to the same server node.", clnRouterName.equals(clientRouter(client).name()));

        lsnr.cntr.set(0);

        for (int key = 0; key < keyCnt; key++)
            clnCache.put(key, key);

        r = GridTestUtils.waitForCondition(new PA() {
            @Override public boolean apply() {
                return keyCnt == lsnr.cntr.get();
            }
        }, 5000);

        assertTrue("Failed to wait for event.", r);

        cur.close();
    }

    /**
     *
     */
    private static class CacheEventListener implements CacheEntryUpdatedListener<Object, Object> {
        /** */
        private AtomicLong cntr = new AtomicLong();

        /** */
        @LoggerResource
        private IgniteLogger log;

        /** {@inheritDoc} */
        @Override public void onUpdated(Iterable<CacheEntryEvent<?, ?>> evts) {
            for (CacheEntryEvent<?, ?> evt : evts) {
                log.info("Received cache event: " + evt);

                cntr.incrementAndGet();
            }
        }
    }
}