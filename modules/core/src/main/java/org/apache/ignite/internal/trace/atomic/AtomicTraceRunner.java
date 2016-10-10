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

package org.apache.ignite.internal.trace.atomic;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.trace.TraceCluster;
import org.apache.ignite.internal.trace.TraceData;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Atomic trace runner.
 */
public class AtomicTraceRunner {
    /** Overall test duration. */
    private static final long DUR = 120000L;

    /** Cache name. */
    private static final String CACHE_NAME = "cache";

    /** Trace duration. */
    private static final long TRACE_DUR = 2000L;

    /** Sleep duration. */
    private static final long SLEEP_DUR = 5000L;

    /** Cache load threads count. */
    private static final int CACHE_LOAD_THREAD_CNT = 1;

    /** Cache size. */
    private static final int CACHE_SIZE = 1000;

    /**
     * Entry point.
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        AtomicTraceUtils.prepareTraceDir();

        try {
            // Start topology.
            Ignition.start(config("srv", false));

            Ignite node = Ignition.start(config("cli", true));

            // Prepare cache loaders.
            List<Thread> threads = new LinkedList<>();

            List<CacheLoader> ldrs = new LinkedList<>();

            for (int i = 0; i < CACHE_LOAD_THREAD_CNT; i++) {
                CacheLoader ldr = new CacheLoader(node);

                ldrs.add(ldr);

                threads.add(new Thread(ldr));
            }

            // Prepare tracer.
            TracePrinter printer = new TracePrinter(node);

            Thread printThread = new Thread(printer);

            threads.add(printThread);

            // Start threads.
            for (Thread thread : threads)
                thread.start();

            // Sleep.
            Thread.sleep(DUR);

            // Stop threads.
            for (CacheLoader ldr : ldrs)
                ldr.stop();

            printer.stop();

            for (Thread thread : threads)
                thread.join();
        }
        finally {
            Ignition.stopAll(true);
        }
    }

    /**
     * Create configuration.
     *
     * @param name Name.
     * @param client Client flag.
     * @return Configuration.
     */
    private static IgniteConfiguration config(String name, boolean client) {
        IgniteConfiguration cfg = new IgniteConfiguration();

        cfg.setGridName(name);
        cfg.setClientMode(client);
        cfg.setLocalHost("127.0.0.1");

        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setName(CACHE_NAME);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /**
     * Cache load generator.
     */
    private static class CacheLoader implements Runnable {
        /** Index generator. */
        private static final AtomicInteger IDX_GEN = new AtomicInteger();

        /** Node. */
        private final Ignite node;

        /** Index. */
        private final int idx;

        /** Stop flag. */
        private volatile boolean stopped;

        /**
         * Constructor.
         *
         * @param node Node.
         */
        public CacheLoader(Ignite node) {
            this.node = node;

            idx = IDX_GEN.incrementAndGet();
        }

        /** {@inheritDoc} */
        @Override public void run() {
            System.out.println(">>> Cache loader " + idx + " started.");

            try {
                IgniteCache<Integer, Integer> cache = node.cache(CACHE_NAME);

                ThreadLocalRandom rand = ThreadLocalRandom.current();

                // Ensure threads are more or less distributed in time.
                try {
                    Thread.sleep(rand.nextInt(100, 2000));
                }
                catch (InterruptedException e) {
                    // No-op.
                }

                // Payload.
                while (!stopped) {
                    int key = rand.nextInt(CACHE_SIZE);

                    cache.put(key, key);
                }
            }
            finally {
                System.out.println(">>> Cache loader " + idx + " stopped.");
            }
        }

        /**
         * Stop thread.
         */
        public void stop() {
            stopped = true;
        }
    }

    /**
     * Trace printer.
     */
    private static class TracePrinter implements Runnable {
        /** Node. */
        private final Ignite node;

        /** Stop flag. */
        private volatile boolean stopped;

        /**
         * Constructor.
         *
         * @param node Node.
         */
        public TracePrinter(Ignite node) {
            this.node = node;
        }

        /** {@inheritDoc} */
        @Override public void run() {
            System.out.println(">>> Trace printer started.");

            int idx = 0;

            try {
                TraceCluster cliTrace = new TraceCluster(node.cluster().forClients());
                TraceCluster srvTrace = new TraceCluster(node.cluster().forServers());

                while (!stopped) {
                    Thread.sleep(SLEEP_DUR);

                    cliTrace.enable();
                    srvTrace.enable();

                    System.out.println(">>> Enabled trace");

                    Thread.sleep(TRACE_DUR);

                    cliTrace.disable();
                    srvTrace.disable();

                    System.out.println(">>> Disabled trace");

                    TraceData data = cliTrace.collectAndReset(
                        AtomicTrace.GRP_CLI,
                        AtomicTrace.GRP_SND_IO_REQ
                    );

                    System.out.println(">>> Collected trace");

                    File traceFile = AtomicTraceUtils.traceFile(idx++);

                    data.save(traceFile);

                    System.out.println(">>> Saved trace");
                    System.out.println();
                }
            }
            catch (Exception e) {
                System.out.println(">>> Trace printer stopped due to exception: " + e);
            }
            finally {
                System.out.println(">>> Trace printer stopped.");
            }
        }

        /**
         * Stop thread.
         */
        public void stop() {
            stopped = true;
        }
    }
}
