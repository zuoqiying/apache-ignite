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

package org.apache.ignite.yardstick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.yardstickframework.BenchmarkConfiguration;
import org.yardstickframework.BenchmarkDriver;
import org.yardstickframework.BenchmarkExecutionAwareProbe;
import org.yardstickframework.BenchmarkProbePoint;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.yardstickframework.BenchmarkUtils.println;

/**
 * Probe that calculates throughput and average latency.
 */
public class MyThroughputLatencyProbe implements BenchmarkExecutionAwareProbe {
    /** Operations executed. */
    private MyThroughputLatencyProbe.ThreadAgent[] agents;

    /** Collected points. */
    private Collection<BenchmarkProbePoint> collected = new ArrayList<>();

    /** Service building probe points. */
    private ExecutorService buildingService;

    /** */
    private BenchmarkConfiguration cfg;

    /** Last data collection time stamp. */
    private volatile long lastTstamp;

    /** {@inheritDoc} */
    @SuppressWarnings("BusyWait")
    @Override public void start(BenchmarkDriver drv, BenchmarkConfiguration cfg) throws Exception {
        this.cfg = cfg;

        agents = new MyThroughputLatencyProbe.ThreadAgent[cfg.threads()];

        for (int i = 0; i < agents.length; i++)
            agents[i] = new MyThroughputLatencyProbe.ThreadAgent();

        buildingService = Executors.newSingleThreadExecutor();

        println(cfg, getClass().getSimpleName() + " is started.");

        lastTstamp = System.currentTimeMillis();
    }

    /** {@inheritDoc} */
    @Override public void stop() throws Exception {
        if (buildingService != null) {
            buildingService.shutdownNow();

            buildingService.awaitTermination(1, MINUTES);

            println(cfg, getClass().getSimpleName() + " is stopped.");
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<String> metaInfo() {
        return Arrays.asList("Time, sec", "Operations/sec (more is better)", "Latency, nsec (less is better)");
    }

    /** {@inheritDoc} */
    @Override public synchronized Collection<BenchmarkProbePoint> points() {
        Collection<BenchmarkProbePoint> ret = collected;

        collected = new ArrayList<>(ret.size() + 5);

        double sum = 0;

        for (BenchmarkProbePoint p : ret)
            sum += p.values()[0];

        System.out.println("Average: " + sum / ret.size());

        return ret;
    }

    /** {@inheritDoc} */
    @Override public void buildPoint(final long time) {
        buildingService.execute(new Runnable() {
            @Override public void run() {
                long lastTstamp0 = lastTstamp;

                long lastTstamp1 = System.currentTimeMillis();

                lastTstamp = lastTstamp1;

                // Time delta in seconds, rounding is used because Thread.sleep(1000) can last less than a second.
                long delta = (long)Math.floor((lastTstamp1 - lastTstamp0) / 1000d + 0.5);

                MyThroughputLatencyProbe.ThreadAgent collector = new MyThroughputLatencyProbe.ThreadAgent();

                for (MyThroughputLatencyProbe.ThreadAgent agent : agents)
                    agent.collect(collector);

                double latency = collector.execCnt == 0 ? 0 : (double)collector.totalLatency / collector.execCnt;

                if (delta != 0) {
                    BenchmarkProbePoint pnt = new BenchmarkProbePoint(
                        TimeUnit.MILLISECONDS.toSeconds(time),
                        new double[] { delta == 0 ? Double.NaN : (double)collector.execCnt / delta, latency});

                    collectPoint(pnt);
                }
            }
        });
    }

    /**
     * @param pnt Probe point.
     */
    private synchronized void collectPoint(BenchmarkProbePoint pnt) {
        collected.add(pnt);
    }

    /** {@inheritDoc} */
    @Override public void beforeExecute(int threadIdx) {
        agents[threadIdx].beforeExecute();
    }

    /** {@inheritDoc} */
    @Override public void afterExecute(int threadIdx) {
        agents[threadIdx].afterExecute();
    }

    /**
     *
     */
    private static class ThreadAgent {
        /** Total execution count by thread. */
        private long execCnt;

        /** Total latency by  */
        private long totalLatency;

        /** Last before execute timestamp. */
        private long beforeTs;

        /**
         *
         */
        public void beforeExecute() {
            beforeTs = System.nanoTime();
        }

        /**
         *
         */
        public void afterExecute() {
            long latency = System.nanoTime() - beforeTs;

            beforeTs = 0;

            synchronized (this) {
                execCnt++;
                totalLatency += latency;
            }
        }

        /**
         * @param other Thread agent.
         */
        public synchronized void collect(MyThroughputLatencyProbe.ThreadAgent other) {
            other.execCnt += execCnt;
            other.totalLatency += totalLatency;

            execCnt = 0;
            totalLatency = 0;
        }
    }
}
