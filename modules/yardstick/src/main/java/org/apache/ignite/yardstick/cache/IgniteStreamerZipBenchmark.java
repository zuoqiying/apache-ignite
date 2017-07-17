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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;
import org.apache.ignite.IgniteBinary;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.IgniteException;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.yardstick.IgniteAbstractBenchmark;
import org.apache.ignite.yardstick.cache.model.ZipEntity;
import org.apache.ignite.yardstick.cache.model.ZipSmallEntity;
import org.xerial.snappy.SnappyOutputStream;
import org.yardstickframework.BenchmarkConfiguration;
import org.yardstickframework.BenchmarkUtils;

/**
 *
 */
public class IgniteStreamerZipBenchmark extends IgniteAbstractBenchmark {
    /** */
    private List<String> cacheNames;

    /** */
    private ExecutorService executor;

    /** Generator executor. */
    private ExecutorService generatorExecutor;

    /** */
    private int entries;

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        entries = args.range();

        if (entries <= 0)
            throw new IllegalArgumentException("Invalid number of entries: " + entries);

        if (cfg.threads() != 1)
            throw new IllegalArgumentException("IgniteStreamerZipBenchmark should be run with single thread. " +
                "Internally it starts multiple threads.");

        String cacheNamePrefix = args.streamerCachesPrefix();

        if (cacheNamePrefix == null || cacheNamePrefix.isEmpty())
            throw new IllegalArgumentException("Streamer caches prefix not set.");

        List<String> caches = new ArrayList<>();

        for (String cacheName : ignite().cacheNames()) {
            if (cacheName.startsWith(cacheNamePrefix))
                caches.add(cacheName);
        }

        if (caches.isEmpty())
            throw new IllegalArgumentException("Failed to find for IgniteStreamerZipBenchmark caches " +
                "starting with '" + cacheNamePrefix + "'");

        BenchmarkUtils.println("Found " + caches.size() + " caches for IgniteStreamerZipBenchmark: " + caches);

        if (args.streamerCacheIndex() >= caches.size()) {
            throw new IllegalArgumentException("Invalid streamer cache index: " + args.streamerCacheIndex() +
                ", there are only " + caches.size() + " caches.");
        }

        if (args.streamerCacheIndex() + args.streamerConcurrentCaches() > caches.size()) {
            throw new IllegalArgumentException("There are no enough caches [cacheIndex=" + args.streamerCacheIndex() +
                ", concurrentCaches=" + args.streamerConcurrentCaches() +
                ", totalCaches=" + caches.size() + ']');
        }

        Collections.sort(caches);

        cacheNames = new ArrayList<>(caches.subList(args.streamerCacheIndex(),
            args.streamerCacheIndex() + args.streamerConcurrentCaches()));

        executor = Executors.newFixedThreadPool(args.streamerConcurrentCaches());

        final int num = args.compressThreads();

        generatorExecutor = Executors.newFixedThreadPool(num);

        BenchmarkUtils.println("IgniteStreamerZipBenchmark start [cacheIndex=" + args.streamerCacheIndex() +
            ", concurrentCaches=" + args.streamerConcurrentCaches() +
            ", entries=" + entries +
            ", bufferSize=" + args.streamerBufferSize() +
            ", cachesToUse=" + cacheNames +
            ", compressorType=" + args.compressorType() +
            ", compressThreads=" + args.compressThreads() +
            ", stringRandomization=" + args.stringRandomization() +
            ']');

        final CompressionType type = CompressionType.valueOf(args.compressorType());

        if (cfg.warmup() > 0) {
            BenchmarkUtils.println("IgniteStreamerZipBenchmark start warmup [warmupTimeMillis=" + cfg.warmup() + ']');

            final long warmupEnd = System.currentTimeMillis() + cfg.warmup();

            final AtomicBoolean stop = new AtomicBoolean();

            try {
                List<Future<Void>> futs = new ArrayList<>();

                for (final String cacheName : cacheNames) {
                    futs.add(executor.submit(new Callable<Void>() {
                        @Override public Void call() throws Exception {
                            Thread.currentThread().setName("streamer-" + cacheName);

                            BenchmarkUtils.println("IgniteStreamerZipBenchmark start warmup for cache " +
                                "[name=" + cacheName + ']');

                            final int KEYS = Math.min(100_000, entries);

                            try (IgniteDataStreamer<Object, Object> streamer = ignite().dataStreamer(cacheName)) {
                                streamer.perNodeBufferSize(args.streamerBufferSize());
                                streamer.perNodeParallelOperations(Runtime.getRuntime().availableProcessors() * 4);

                                List<Future<Object>> futs = new ArrayList<>(num);

                                for (int i = 0; i < num; i++) {
                                    Future<Object> fut = generatorExecutor.submit(new Callable<Object>() {
                                        @Override public Object call() throws Exception {
                                            IgniteBinary binary = ignite().binary();

                                            int key = 1;

                                            while (System.currentTimeMillis() < warmupEnd && !stop.get()) {
                                                for (int i = 0; i < 10; i++) {
                                                    streamer.addData(String.valueOf(-key++),
                                                        create(binary, type, args.stringRandomization(), false));

                                                    if (key >= KEYS)
                                                        key = 1;
                                                }
                                            }

                                            return null;
                                        }
                                    });

                                    futs.add(fut);
                                }

                                for (Future<Object> fut : futs)
                                    fut.get();
                            }

                            ignite().cache(cacheName).clear();

                            BenchmarkUtils.println("IgniteStreamerZipBenchmark finished warmup for cache " +
                                "[name=" + cacheName + ']');

                            return null;
                        }
                    }));
                }

                for (Future<Void> fut : futs)
                    fut.get();
            }
            finally {
                stop.set(true);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> map) throws Exception {
        BenchmarkUtils.println("IgniteStreamerZipBenchmark start test.");

        long start = System.currentTimeMillis();

        final AtomicBoolean stop = new AtomicBoolean();

        try {
            List<Future<Void>> futs = new ArrayList<>();

            for (final String cacheName : cacheNames) {
                futs.add(executor.submit(new Callable<Void>() {
                    @Override public Void call() throws Exception {
                        Thread.currentThread().setName("streamer-" + cacheName);

                        final CompressionType type = CompressionType.valueOf(args.compressorType());

                        final long start = System.currentTimeMillis();

                        BenchmarkUtils.println("IgniteStreamerZipBenchmark start load cache [name=" + cacheName + ']');

                        try (final IgniteDataStreamer<Object, Object> streamer = ignite().dataStreamer(cacheName)) {
                            streamer.perNodeBufferSize(args.streamerBufferSize());
                            streamer.perNodeParallelOperations(Runtime.getRuntime().availableProcessors() * 4);

                            final int num = args.compressThreads();

                            List<Future<Object>> futs = new ArrayList<>(num);

                            for (int i = 0; i < num; i++) {
                                Future<Object> fut = generatorExecutor.submit(new Callable<Object>() {
                                    @Override public Object call() throws Exception {
                                        IgniteBinary binary = ignite().binary();

                                        // Use random keys to allow submit entries from multiple clients independently.
                                        Random rnd = new Random();

                                        for (int i = 0; i < entries / num; i++) {
                                            streamer.addData(String.valueOf(rnd.nextLong()), create(binary, type,
                                                args.stringRandomization(), false));

                                            if (i > 0 && i % 1000 == 0) {
                                                if (stop.get())
                                                    break;

                                                if (i % 100_000 == 0) {
                                                    BenchmarkUtils.println("IgniteStreamerZipBenchmark cache load progress [name=" + cacheName +
                                                        ", entries=" + i +
                                                        ", timeMillis=" + (System.currentTimeMillis() - start) + ']');
                                                }
                                            }
                                        }

                                        return null;
                                    }
                                });

                                futs.add(fut);
                            }

                            for (Future<Object> fut : futs)
                                fut.get();

                            long time = System.currentTimeMillis() - start;

                            BenchmarkUtils.println("IgniteStreamerZipBenchmark finished load cache [name=" + cacheName +
                                ", entries=" + entries +
                                ", bufferSize=" + args.streamerBufferSize() +
                                ", totalTimeMillis=" + time +
                                ", entriesInCache=" + ignite().cache(cacheName).size() + ']');

                            return null;
                        }
                    }
                }));
            }

            for (Future<Void> fut : futs)
                fut.get();
        }
        finally {
            stop.set(true);
        }

        long time = System.currentTimeMillis() - start;

        BenchmarkUtils.println("IgniteStreamerZipBenchmark finished [totalTimeMillis=" + time +
            ", entries=" + entries +
            ", bufferSize=" + args.streamerBufferSize() + ']');

        for (String cacheName : cacheNames) {
            BenchmarkUtils.println("Cache size [cacheName=" + cacheName +
                ", size=" + ignite().cache(cacheName).size() + ']');
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public void tearDown() throws Exception {
        if (executor != null)
            executor.shutdown();

        if (generatorExecutor != null)
            generatorExecutor.shutdownNow();

        super.tearDown();
    }

    /**
     * @param binary Binary.
     * @param type Compressor type.
     */
    public static BinaryObject create(IgniteBinary binary, CompressionType type, double strRandomization, boolean small) {
        if (type != CompressionType.NONE && !small) {
            OutputStream gout = null;

            try {
                ZipEntity entity = ZipEntity.generateHard(strRandomization, ZipEntity.RND_STRING_LEN);

                BinaryObjectBuilder builder = binary.builder("TestZip");

                builder.setField("BUSINESSDATE", entity.BUSINESSDATE);
                builder.setField("RISKSUBJECTID", entity.RISKSUBJECTID);
                builder.setField("SERIESDATE", entity.SERIESDATE);
                builder.setField("SNAPVERSION", entity.SNAPVERSION);
                builder.setField("VARTYPE", entity.VARTYPE);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();

                if (type == CompressionType.ZIP)
                    gout = new GZIPOutputStream(bout);
                else if (type == CompressionType.SNAPPY)
                    gout = new SnappyOutputStream(bout);
                else
                    throw new IllegalArgumentException("Unsupported compressor: " + type);

                ZipEntity.notIndexedDataHard(entity, gout);

                if (type == CompressionType.ZIP)
                    ((GZIPOutputStream)gout).finish();

                gout.flush();

                byte[] bytes = bout.toByteArray();

                builder.setField("zipped", bytes);

                return builder.build();
            }
            catch (Exception e) {
                throw new IgniteException(e);
            }
            finally {
                U.closeQuiet(gout);
            }
        }

        if (small)
            return binary.toBinary(ZipSmallEntity.generateHard(ZipSmallEntity.RND_STRING_LEN));

        return binary.toBinary(ZipEntity.generate());
    }

    /**
     *
     */
    public enum CompressionType {
        /** None. */NONE, /** Zip. */ZIP, /** Snappy. */SNAPPY
    }
}
