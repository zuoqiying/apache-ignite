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

package org.apache.ignite.internal.trace.atomic.runners;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.internal.trace.TraceCluster;
import org.apache.ignite.internal.trace.TraceData;
import org.apache.ignite.internal.trace.atomic.AtomicTrace;
import org.apache.ignite.internal.trace.atomic.AtomicTraceUtils;

import java.io.File;

/**
 * Atomic trace client runner.
 */
public class AtomicTraceRunner {
    /** Overall test duration. */
    private static final long DUR = 120000L;

    /** Trace duration. */
    private static final long TRACE_DUR = 2000L;

    /** Sleep duration. */
    private static final long SLEEP_DUR = 5000L;

    /**
     * Entry point.
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        AtomicTraceUtils.prepareTraceDir();

        try {
            // Start topology.
            Ignite node = Ignition.start(AtomicTraceUtils.config("cli_trace", true));

            // Prepare tracer.
            TracePrinter printer = new TracePrinter(node);

            Thread printThread = new Thread(printer);

            printThread.start();

            // Sleep.
            Thread.sleep(DUR);

            printer.stop();

            printThread.join();
        }
        finally {
            Ignition.stopAll(true);
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
                TraceCluster trace = new TraceCluster(node.cluster().forNodes(node.cluster().nodes()));

                while (!stopped) {
                    Thread.sleep(SLEEP_DUR);

                    trace.enable();

                    System.out.println(">>> Enabled trace");

                    Thread.sleep(TRACE_DUR);

                    trace.disable();

                    System.out.println(">>> Disabled trace");

                    TraceData data = trace.collectAndReset(
                        AtomicTrace.GRP_USR,
                        AtomicTrace.GRP_IO_SND,
                        AtomicTrace.GRP_IO_RCV,
                        AtomicTrace.GRP_SRV,
                        AtomicTrace.GRP_CLI
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
