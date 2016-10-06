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
import java.util.List;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.yardstick.cache.IgniteIoTestSendAllBenchmark;
import org.apache.ignite.yardstick.cache.IgniteIoTestSendRandomBenchmark;
import org.yardstickframework.BenchmarkDriver;
import org.yardstickframework.BenchmarkDriverStartUp;

/**
 * Utils.
 */
public class IgniteBenchmarkUtilsTmp {
    /**
     * Starts nodes/driver in single JVM for quick benchmarks testing.
     *
     * -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=delay=60s,duration=60s,filename=myrecording.jfr -XX:FlightRecorderOptions=defaultrecording=true
     * -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=delay=60s,duration=60s,filename=myrecording.jfr -XX:FlightRecorderOptions=defaultrecording=true
     * @param args Command line arguments.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        final Class<? extends BenchmarkDriver> benchmark = IgniteIoTestSendRandomBenchmark.class;
//        final Class<? extends BenchmarkDriver> benchmark = IgniteIoTestSendAllBenchmark.class;

        final int threads = 16;

        final boolean clientDriverNode = true;

        final int extraNodes = 6;

        final int warmUp = 30;
        final int duration = 60;

        final int range = 1_000_000;

        final boolean throughputLatencyProbe = true;

        for (int i = 0; i < extraNodes; i++) {
            final String cfg = "modules/yardstick/config/ignite-localhost-config-srv.xml";

            IgniteConfiguration nodeCfg = Ignition.loadSpringBean(cfg, "grid.cfg");

            nodeCfg.setGridName("node-" + i);
            nodeCfg.setMetricsLogFrequency(0);

            Ignition.start(nodeCfg);
        }

        final String cfg = "modules/yardstick/config/ignite-localhost-config2.xml";

        ArrayList<String> args0 = new ArrayList<>();

        addArg(args0, "-t", threads);
        addArg(args0, "-w", warmUp);
        addArg(args0, "-d", duration);
        addArg(args0, "-r", range);
        addArg(args0, "-dn", benchmark.getSimpleName());
        addArg(args0, "-sn", "IgniteNode");
        addArg(args0, "-cfg", cfg);

        if (throughputLatencyProbe) {
            addArg(args0, "-pr", "ThroughputLatencyProbe");
            addArg(args0, "--writer", "MyBenchmarkProbePointCsvWriter");
        }

        if (clientDriverNode)
            args0.add("-cl");

        BenchmarkDriverStartUp.main(args0.toArray(new String[args0.size()]));
    }

    /**
     * @param args Arguments.
     * @param arg Argument name.
     * @param val Argument value.
     */
    private static void addArg(List<String> args, String arg, Object val) {
        args.add(arg);
        args.add(val.toString());
    }
}

// -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=delay=40s,duration=30s,filename=myrecording.jfr -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true
