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

package org.apache.ignite.internal.benchmarks.jmh;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.internal.util.GridInt2IntOpenHashMap;
import org.jsr166.ThreadLocalRandom8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@SuppressWarnings("CommentAbsent")
public class IntIntFastUtilHashMapBenchmark {

    @Param({"10", "100", "1000", "10000"})
    private int size;

    private GridInt2IntOpenHashMap map;

    private int[] keys;

    @Setup(Level.Iteration)
    public void setup() {
        ThreadLocalRandom8 random = ThreadLocalRandom8.current();

        keys = new int[size];

        HashMap<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < size; i++) {
            keys[i]  = random.nextInt();
            map.put(keys[i], random.nextInt(0, 10));
        }

        this.map = new GridInt2IntOpenHashMap(map, 0.33f);
    }

    @Benchmark
    @Threads(4)
    public int bench() {
        int res = 0;

        for (int i = 0; i < size; i++)
            res ^= map.get(keys[i]);

        return res;
    }
}
