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

package org.apache.ignite.internal.processors.cache;

import java.util.UUID;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;

/**
 * Test for value copy in entry processor.
 */
public class CacheEntryProcessorTxSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setPeerClassLoadingEnabled(true);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testEntryProcessor() throws Exception {
        startGrids(1);

        IgniteConfiguration cfg = getConfiguration();

        cfg.setClientMode(true);

        Ignite clientNode = Ignition.start(cfg);

        CacheConfiguration ccfg = defaultCacheConfiguration();

        ccfg.setCacheMode(CacheMode.PARTITIONED);
        ccfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        IgniteCache<Integer, String> cache = clientNode.getOrCreateCache(ccfg);

        System.out.println("localThread=" + Thread.currentThread());
        System.out.println("localNode=" + clientNode.cluster().localNode().id());

        final IgniteTransactions txns = clientNode.transactions();

        Integer key = 42;
        try (final Transaction tx = txns.txStart()) {
            String localValue = UUID.randomUUID().toString();

            cache.put(key, localValue);

            String remoteValue = cache.invoke(key, new CacheEntryProcessor<Integer, String, String>() {
                @IgniteInstanceResource
                private Ignite ignite;

                @Override public String process(MutableEntry<Integer, String> entry, Object... args) {
                    String valueLast = entry.getValue();

                    System.out.println();
                    System.out.println("CacheEntryProcessor: thread=" + Thread.currentThread());
                    System.out.println("CacheEntryProcessor: valueLast=" + valueLast);

                    if (ignite != null)
                        System.out.println("CacheEntryProcessor: remoteNode=" + ignite.cluster().localNode().id());

                    return valueLast;
                }
            });

            tx.commit();
            assertEquals(localValue, remoteValue);
        }
    }
}
