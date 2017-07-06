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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

/**
 * Test for value copy in entry processor.
 */
public class CacheEntryProcessorTxSelfTest extends GridCommonAbstractTest {

    private static class GetAfterInvokeWithCreateParams {

        private CacheMode cacheMode;

        private CacheAtomicityMode atomicityMode;

        private TransactionConcurrency transactionConcurrency;

        private TransactionIsolation transactionIsolation;

        public GetAfterInvokeWithCreateParams(
            CacheMode cacheMode,
            CacheAtomicityMode atomicityMode,
            TransactionConcurrency transactionConcurrency,
            TransactionIsolation transactionIsolation
        ) {
            this.cacheMode = cacheMode;
            this.atomicityMode = atomicityMode;
            this.transactionConcurrency = transactionConcurrency;
            this.transactionIsolation = transactionIsolation;
        }

        public CacheMode getCacheMode() {
            return cacheMode;
        }

        public CacheAtomicityMode getAtomicityMode() {
            return atomicityMode;
        }

        public TransactionConcurrency getTransactionConcurrency() {
            return transactionConcurrency;
        }

        public TransactionIsolation getTransactionIsolation() {
            return transactionIsolation;
        }
    }

    /** */
    private static final int NUM_SERVER_NODES = 2;

    /** */
    private static final String CACHE_NAME = "testCache";

    /** */
    private Ignite clientNode;

    /** */
    private IgniteCache<Integer, String> cache;

    /**
     * Returns the parameters for the getAfterInvokeWithCreate test.
     */
    private static GetAfterInvokeWithCreateParams[] getGetAfterInvokeWithCreateParams() {
        List<GetAfterInvokeWithCreateParams> params = new ArrayList<>();

        CacheMode[] cacheModes = {CacheMode.REPLICATED, CacheMode.PARTITIONED};

        TransactionIsolation[] transactionIsolations = TransactionIsolation.values();

        // Add TRANSACTIONAL mode parameters.
        for (CacheMode cacheMode : cacheModes) {
            for (TransactionConcurrency transactionConcurrency : TransactionConcurrency.values()) {
                for (TransactionIsolation transactionIsolation : transactionIsolations) {
                    params.add(new GetAfterInvokeWithCreateParams(
                        cacheMode, CacheAtomicityMode.TRANSACTIONAL, transactionConcurrency, transactionIsolation
                    ));
                }
            }
        }

        // Add ATOMIC mode parameters.
        for (CacheMode cacheMode : cacheModes) {
            params.add(new GetAfterInvokeWithCreateParams(cacheMode, CacheAtomicityMode.ATOMIC, null, null));
        }

        return params.toArray(new GetAfterInvokeWithCreateParams[params.size()]);
    }

    /**
     * Returns the client node index.
     */
    private static int clientIndex() {
        return NUM_SERVER_NODES;
    }

    /**
     * Returns whether the node with the specified grid name
     * is a client node.
     *
     * @param gridName Grid name.
     * @return {@code true} if the node is a client node, otherwise {@code false}.
     */
    private static boolean isClient(String gridName) {
        return gridName.contains(Integer.toString(clientIndex()));
    }

    private static CacheConfiguration<Integer, String> getCacheConfiguration(
        String cacheName, CacheMode cacheMode, CacheAtomicityMode atomicityMode, int backups
    ) {
        CacheConfiguration<Integer, String> ccfg = new CacheConfiguration<>(cacheName);

        ccfg.setCacheMode(cacheMode);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setBackups(backups);

        return ccfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        // Start server nodes.
        startGridsMultiThreaded(NUM_SERVER_NODES);

        // Start client node.
        clientNode = startGrid(clientIndex());

        cache = clientNode.cache(CACHE_NAME);
    }

    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        super.afterTest();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setPeerClassLoadingEnabled(true);

        if (isClient(gridName))
            cfg.setClientMode(true);

        return isDebug() ? applyDebugConfig(cfg) : cfg;
    }

    private IgniteConfiguration applyDebugConfig(IgniteConfiguration cfg) {
        cfg.setFailureDetectionTimeout(TimeUnit.DAYS.toMillis(1));
        cfg.setNetworkTimeout(TimeUnit.DAYS.toMillis(1));

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testGetAfterInvokeWithCreateReplicatedPessimisticRepeatableRead() throws Exception {
        getAfterInvokeWithCreate(
            GridTestUtils.createRandomizedName("replicatedPessimisticRepeatableRead"),
            CacheMode.REPLICATED,
            CacheAtomicityMode.TRANSACTIONAL,
            TransactionConcurrency.PESSIMISTIC,
            TransactionIsolation.REPEATABLE_READ
        );
    }

    /**
     * @throws Exception If failed.
     */
    private void getAfterInvokeWithCreate(
        String cacheName,
        CacheMode cacheMode,
        CacheAtomicityMode atomicityMode,
        TransactionConcurrency transactionConcurrency,
        TransactionIsolation transactionIsolation
    ) throws Exception {
        cache = clientNode.createCache(getCacheConfiguration(cacheName, cacheMode, atomicityMode, 1));

        System.out.println("localThread=" + Thread.currentThread());
        System.out.println("localNode=" + clientNode.cluster().localNode().id());

        final IgniteTransactions txns = clientNode.transactions();

        Integer key = 42;
        final String newVal = "NEW_VAL";

        try (final Transaction tx =txns.txStart(transactionConcurrency, transactionIsolation)) {
            final String entryProcResToReturn = "ENTRY_PROCESSOR_RESULT";

            String entryProcRes = cache.invoke(key, new CacheEntryProcessor<Integer, String, String>() {
                @IgniteInstanceResource
                private Ignite ignite;

                @Override public String process(MutableEntry<Integer, String> entry, Object... args) {
                    String valLast = entry.getValue();
                    entry.setValue(newVal);
                    System.out.println();
                    System.out.println("CacheEntryProcessor: thread=" + Thread.currentThread());
                    System.out.println("CacheEntryProcessor: valueLast=" + valLast);

                    if (ignite != null)
                        System.out.println("CacheEntryProcessor: remoteNode=" + ignite.cluster().localNode().id() +
                            ", isClient=" + ignite.cluster().localNode().isClient());

                    return entryProcResToReturn;
                }
            });

            System.out.println("entryProcRes=" + entryProcRes);
            assertEquals(entryProcResToReturn, entryProcRes);

            System.out.println("BEFORE GET IN TX");
            String inTxReceivedVal = cache.get(key);
            System.out.println("In TX received value=" + inTxReceivedVal);
            assertEquals(newVal, inTxReceivedVal);

            if (tx != null) {
                System.out.println("BEFORE COMMIT");
                tx.commit();
                System.out.println("AFTER COMMIT");
            }
        }

        assertEquals(newVal, cache.get(key));
    }
}