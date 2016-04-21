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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.fair.FairAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;

/**
 *
 */
public class Bench1 {
    /*
    Coordinator start

    nohup java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=10222 \
        -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false \
        -DIGNITE_QUIET=false -server -Xmx6g -cp ignite-core-rebal-master.jar \
        -Xloggc:gccoord.log -verbose:gc -XX:+PrintGCDateStamps  org.apache.ignite.Bench1 1 1 > coord.log 2>&1 &

    Main topology start

    nohup java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=10223 \
        -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false \
        -DIGNITE_QUIET=false -server -Xmx12g -cp ignite-core-rebal-master.jar \
        -Xloggc:gctop.log -verbose:gc -XX:+PrintGCDateStamps org.apache.ignite.Bench1 32 70 > top.log 2>&1 &

    nohup java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=10225 \
        -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false \
        -DIGNITE_QUIET=false -server -Xmx26g -cp ignite-core-rebal-master.jar \
        -Xloggc:gctop`date +"%m%d%H%M%S"`.log -verbose:gc -XX:+PrintGCDateStamps org.apache.ignite.Bench1 1 60 > top`date +"%m%d%H%M%S"`.log 2>&1 &

     */

    public static void main(String[] args) throws InterruptedException {
        int threadCnt = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        int nodeCnt = args.length > 1 ? Integer.parseInt(args[1]) : 1;

        System.out.println("Starting [threads=" + threadCnt + ", nodes=" + nodeCnt + ']');

        ExecutorService svc = Executors.newFixedThreadPool(threadCnt);

        final AtomicInteger idGen = new AtomicInteger();

        for (int i = 0; i < nodeCnt; i++) {
            svc.submit(
                new Callable<Object>() {
                    @Override public Object call() throws Exception {
                        try {
                            int id = idGen.incrementAndGet();

                            long start = System.currentTimeMillis();

                            Ignition.start(config(
                                "" + id,
                                false));

                            System.out.println("Finished start [id=" + id +
                                ", duration=" + (System.currentTimeMillis() - start) + ']');
                        }
                        catch (Exception e) {
                            e.printStackTrace();

                            System.exit(1);
                        }

                        return null;
                    }
                }
            );
        }

        svc.shutdown();
        svc.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        System.out.println("Finished.");
    }

    /**
     * @return
     */
    private static CacheConfiguration<Integer, Integer> cacheConfig() {
        return new CacheConfiguration<Integer, Integer>()
            .setName("1")
            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
//            .setAtomicWriteOrderMode(CacheAtomicWriteOrderMode.PRIMARY)
            .setAffinity(new FairAffinityFunction(1024))
            .setBackups(1)
            .setRebalanceMode(CacheRebalanceMode.SYNC)
            .setCopyOnRead(false)
            .setStartSize(16)
            .setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
    }

    private static IgniteConfiguration config(
        String name,
        boolean client
    ) {
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();

        commSpi.setSharedMemoryPort(-1);
        commSpi.setLocalPortRange(1000);
        commSpi.setConnectTimeout(60000);
        commSpi.setIdleConnectionTimeout(600000);
        commSpi.setSocketWriteTimeout(600000);
        commSpi.setMaxConnectTimeout(600000);
        commSpi.setSelectorsCount(1);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();

        disco.setLocalPortRange(1000);
        disco.setStatisticsPrintFrequency(5000);

        IgniteConfiguration c = new IgniteConfiguration();

        c.setFailureDetectionTimeout(600_000);
        c.setMetricsLogFrequency(5000);

        return c
            .setTimeServerPortRange(1000)
            .setGridName(name)
            .setClientMode(client)
            .setCommunicationSpi(commSpi)
            .setIgfsThreadPoolSize(1)
            .setManagementThreadPoolSize(1)
            .setMarshallerCachePoolSize(1)
            .setPublicThreadPoolSize(2)
            .setUtilityCachePoolSize(2)
            .setSystemThreadPoolSize(4)
            .setDiscoverySpi(disco)
            .setCacheConfiguration(cacheConfig());
        //.setCommunicationSpi(new CommunicationSpi());
    }
}
