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

package org.apache.ignite.yardstick.cache;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionMetrics;
import org.apache.ignite.yardstick.cache.model.SampleValue;
import org.yardstickframework.BenchmarkConfiguration;
import org.yardstickframework.BenchmarkUtils;

/**
 * Ignite benchmark that performs transactional put operations.
 */
public class IgnitePutTxStringBenchmark extends IgniteCacheAbstractBenchmark<Integer, Object> {
    /** */
    private IgniteTransactions txs;

    /** */
    private ArrayList<IgniteCache<Object, Object>> cacheList;

    private String value;

    private Random random;

    /** */
    private Callable<Void> clo;

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        if (!IgniteSystemProperties.getBoolean("SKIP_MAP_CHECK"))
            ignite().compute().broadcast(new WaitMapExchangeFinishCallable());

        txs = ignite().transactions();

        cacheList = new ArrayList<>(args.cachesCount());

        for (int i = 0; i < args.cachesCount(); i++)
            cacheList.add(ignite().cache("tx-" + i));

        value = createVal(args.getStringLength());

        random = new Random();

        clo = new Callable<Void>() {
            @Override public Void call() throws Exception {
                IgniteCache<Integer, Object> cache = cacheForOperation();

                int key = nextRandom(args.range());

                cache.put(key, new SampleValue(key));

                return null;
            }
        };
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        IgniteTransactions transactions = ignite().transactions();

        long startTime;

        try (Transaction tx = transactions.txStart(args.txConcurrency(), args.txIsolation())) {
            for (int i = 0; i < args.scaleFactor(); i++){
                IgniteCache<Object, Object> currentCache = cacheList.get(random.nextInt(cacheList.size()));
                currentCache.put(random.nextLong(), value);
            }

            startTime = System.currentTimeMillis();

            tx.commit();
        }

        TransactionMetrics tm = transactions.metrics();
        if (tm.commitTime() - startTime > args.getWarningTime())
            BenchmarkUtils.println("Transaction commit time = " + (tm.commitTime() - startTime));

        if (tm.txRollbacks() > 0 && args.printRollBacks())
            BenchmarkUtils.println("Transaction rollbacks = " + tm.txRollbacks());

        return true;
    }

    /** {@inheritDoc} */
    @Override protected IgniteCache<Integer, Object> cache() {
        return ignite().cache("tx");
    }

    private String createVal(int length){
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; i++)
            sb.append('x');

        return sb.toString();
    }
}
