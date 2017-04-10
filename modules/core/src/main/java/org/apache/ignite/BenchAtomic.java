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

package org.apache.ignite;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.internal.NodeStoppingException;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.jsr166.LongAdder8;
import org.jsr166.ThreadLocalRandom8;

/**
 * Copy scp -i ../.ssh/yzhdanov_key ignite_core_jar/ignite-core.jar yzhdanov@172.25.1.26: Client. java -DCHEAT_CACHE=1
 * -DIGNITE_QUIET=false -DIGNITE_OVERRIDE_MCAST_GRP=228.1.2.6 -server -Xmx3g -Xms3g -cp ignite-core.jar
 * org.apache.ignite.BenchAtomic true 128 512
 *
 * Server java -DCHEAT_CACHE=1 -DIGNITE_QUIET=false -DIGNITE_OVERRIDE_MCAST_GRP=228.1.2.6 -server -Xmx3g -Xms3g -cp
 * ignite-core.jar org.apache.ignite.BenchAtomic false
 */
public class BenchAtomic {
    public static final int KEYS = 100_000;

    public static void main(String[] args) throws InterruptedException {
        boolean client = Boolean.parseBoolean(args[0]);
        int threadCnt = args.length > 1 ? Integer.parseInt(args[1]) : 64;
        final int payLoad = args.length > 2 ? Integer.parseInt(args[2]) : 512;
        final int cachesCnt = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        final int parts = args.length > 4 ? Integer.parseInt(args[4]) : 1024;

        System.out.println("Params [client=" + client +
            ", threads=" + threadCnt + ", payLoad=" + payLoad + ", caches=" + cachesCnt +
            ", parts=" + parts + ']');

        final Ignite ignite = Ignition.start(config(null, client));

        final Map<Integer, CacheConfiguration> cacheCfgs = new HashMap<>();

        for (int i = 0; i < cachesCnt; i++) {
            cacheCfgs.put(i, cacheConfig(i, parts));

            U.debug("Configured cache: " + cacheCfgs.get(i).getName());
        }

        if (System.getProperty("SEQUENTIAL") != null) {
            U.debug("Starting caches sequentially");

            for (CacheConfiguration cacheCfg : cacheCfgs.values())
                ignite.getOrCreateCache(cacheCfg);
        } else {
            U.debug("Starting caches concurrently");

            ignite.getOrCreateCaches(cacheCfgs.values());
        }

        ignite.getOrCreateCaches(cacheCfgs.values());

        final LongAdder8 cnt = new LongAdder8();

        for (int i = 0; i < threadCnt; i++) {
            new Thread(
                new Runnable() {
                    @Override public void run() {
                        for (; ; ) {
                            int key = ThreadLocalRandom8.current().nextInt(KEYS);

                            byte[] val = new byte[ThreadLocalRandom.current().nextInt(
                                64,
                                payLoad)];

                            IgniteCache<Integer, byte[]> cache0 = ignite.cache(cacheCfgs.
                                get(ThreadLocalRandom8.current().nextInt(cachesCnt)).getName());

                            boolean startTx = cache0.getConfiguration(CacheConfiguration.class).getAtomicityMode() ==
                                CacheAtomicityMode.TRANSACTIONAL;

                            try {
                                Transaction tx = startTx ?
                                    ignite.transactions().txStart(TransactionConcurrency.PESSIMISTIC,
                                        TransactionIsolation.REPEATABLE_READ) :
                                    null;

                                cache0.put(key, val);

                                if (tx != null)
                                    tx.commit();
                            }
                            catch (Exception e) {
                                if (!X.hasCause(e,
                                    NodeStoppingException.class))
                                    e.printStackTrace();
                                else
                                    U.debug("Process stopping.");

                                break;
                            }

                            cnt.increment();
                        }
                    }
                }
            ).start();
        }

        if (threadCnt > 0) {
            Timer t = new Timer("Stats", true);

            t.schedule(new TimerTask() {
                long avg = 0;
                int i = 0;

                @Override public void run() {
                    long c;

                    System.out.println("TPS [cnt=" + (c = cnt.sumThenReset()) + ']');

                    avg += c;

                    i++;

                    if (i == 30) {
                        i = 0;

                        System.out.println("30 sec avg: " + avg * 1.0 / 30);

                        avg = 0;
                    }
                }
            }, 0, 1000);
        }
    }

    /**
     * @return Cache config.
     */
    private static <K, V> CacheConfiguration<K, V> cacheConfig(int id, int parts) {
        return new CacheConfiguration<K, V>()
            .setName("cache" + id)
            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
//            .setAtomicWriteOrderMode(CacheAtomicWriteOrderMode.PRIMARY)
                // .setAffinity(new FairAffinityFunction(1024))
            .setBackups(0)
            .setRebalanceMode(CacheRebalanceMode.SYNC)
            .setCopyOnRead(false)
            .setAffinity(new RendezvousAffinityFunction(false, parts))
//            .setMemoryMode(CacheMemoryMode.OFFHEAP_TIERED)
            .setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
    }

    /**
     * @param name Name.
     * @param client Client.
     */
    private static IgniteConfiguration config(
        String name,
        boolean client
    ) {
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();

//        System.setProperty("IGNITE_IO_BALANCE_PERIOD", "0");

        if (System.getProperty("FIX_TIMEOUT") != null) {
            commSpi.setConnectTimeout(600_000);
            commSpi.setMaxConnectTimeout(600_000 * 2);
            commSpi.setReconnectCount(100);
            commSpi.setIdleConnectionTimeout(600_000);
            commSpi.setSocketWriteTimeout(600_000);
            commSpi.setSelectorsCount(18);
            commSpi.setConnectionsPerNode(8);
            commSpi.setSharedMemoryPort(-1);
            commSpi.setMessageQueueLimit(0);
        }

        MemoryConfiguration memCfg = new MemoryConfiguration();

        String prop = System.getProperty("PAGE_CACHE_SIZE");

        if (prop != null)
            memCfg.setPageCacheSize(Long.parseLong(prop));

        return new IgniteConfiguration()
            .setMemoryConfiguration(memCfg)
            .setGridName(name)
            .setClientMode(client)
            .setCommunicationSpi(commSpi)
            .setFailureDetectionTimeout(600_000);
    }
}