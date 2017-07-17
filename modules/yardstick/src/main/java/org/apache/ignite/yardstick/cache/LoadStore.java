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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteBinary;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.affinity.AffinityKey;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.PersistentStoreConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.lang.IgniteBiInClosure;
import org.apache.ignite.lang.IgniteUuid;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.yardstick.cache.model.ZipEntity;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.yardstick.cache.IgniteStreamerZipBenchmark.CompressionType;
import static org.apache.ignite.yardstick.cache.IgniteStreamerZipBenchmark.create;

/**
 *
 */
public class LoadStore implements CacheStore<Object, Object> {
    /** */
    @IgniteInstanceResource
    private Ignite ignite;

    /** */
    private static final String CACHE_NAME = IgniteSystemProperties.getString("CACHE_NAME", "cache");

    /** {@inheritDoc} */
    @Override public void loadCache(
        final IgniteBiInClosure<Object, Object> clo,
        @Nullable Object... args
    ) throws CacheLoaderException {
        final Arguments args0 = (Arguments)args[0];

        ignite.log().info("Start loading entries. " +
            "[entries=" + args0.range +
            ", threads=" + args0.loadThreads +
            ", compressionType=" + args0.compType +
            ", stringRandomization=" + args0.strRandomization +
            ", index=" + args0.idx +
            ", smallEntry=" + args0.smallEntry +
            ", persistenceEnabled=" + args0.persistenceEnabled +
            ", WALMode=" + args0.walMode +
            ']');

        ThreadPoolExecutor exec = new ThreadPoolExecutor(
            args0.loadThreads,
            args0.loadThreads,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

        int[] parts = ignite.affinity(CACHE_NAME).primaryPartitions(ignite.cluster().localNode());

        int partsLen0 = parts.length / args0.loadThreads;
        int partsLenDiff = parts.length % args0.loadThreads;
        int k = 0;

        final long start = System.currentTimeMillis();

        final int entriesPerThread = args0.range / args0.loadThreads;

        for (int i = 0; i < args0.loadThreads; i++) {
            final int[] parts0 = new int[partsLen0 + (partsLenDiff > 0 ? 1 : 0)];

            partsLenDiff--;

            for (int j = 0; j < parts0.length; j++)
                parts0[j] = parts[k++];

            exec.submit(
                new Callable<Object>() {
                    @Override public Object call() throws Exception {
                        ThreadLocalRandom rnd = ThreadLocalRandom.current();

                        IgniteBinary binary = ignite.binary();

                        ignite.log().info("Starting closure [parts=" + Arrays.toString(parts0) +
                            ", partsCnt=" + parts0.length + ']');

                        // TODO:
                        // 1. put condition to the loop.
                        // 2. put real values (read from files?).
                        for (int i = 0; i < entriesPerThread; i++) {
                            clo.apply(generateKey(parts0[rnd.nextInt(parts0.length)]),
                                create(binary, args0.compType, args0.strRandomization, args0.smallEntry));
                        }

                        return null;
                    }
                }
            );
        }

        exec.shutdown();

        try {
            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long time = System.currentTimeMillis() - start;

        ignite.log().info("Load finished." +
            "[entries=" + args0.range +
            ", threads=" + args0.loadThreads +
            ", compressionType=" + args0.compType +
            ", stringRandomization=" + args0.strRandomization +
            ", index=" + args0.idx +
            ", smallEntry=" + args0.smallEntry +
            ", persistenceEnabled=" + args0.persistenceEnabled +
            ", WALMode=" + args0.walMode +
            ", totalTime=" + time + ']');
    }

    /**
     * @param i Partition to map key to.
     * @return Generated affinity key.
     */
    private Object generateKey(
        int i
    ) {
        return new AffinityKey<>(IgniteUuid.randomUuid(), i);
    }

    /** {@inheritDoc} */
    @Override public void sessionEnd(boolean commit) throws CacheWriterException {

    }

    /** {@inheritDoc} */
    @Override public Object load(Object key) throws CacheLoaderException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public Map<Object, Object> loadAll(Iterable keys) throws CacheLoaderException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void write(Cache.Entry entry) throws CacheWriterException {

    }

    /** {@inheritDoc} */
    @Override public void writeAll(Collection col) throws CacheWriterException {

    }

    /** {@inheritDoc} */
    @Override public void delete(Object key) throws CacheWriterException {

    }

    /** {@inheritDoc} */
    @Override public void deleteAll(Collection keys) throws CacheWriterException {

    }

    /**
     * For test purposes only.
     *
     * @param args Args (none used).
     */
    public static void main(String[] args) {
        Arguments args0 = Arguments.parseArguments(args);

        Ignite ignite;

        if (args0.igniteCfgUrl != null)
            ignite = Ignition.start(args0.igniteCfgUrl);
        else {
            IgniteConfiguration cfg = new IgniteConfiguration().setLocalHost("127.0.0.1");

            TcpDiscoveryVmIpFinder finder = new TcpDiscoveryVmIpFinder();
            finder.setAddresses(Collections.singleton("127.0.0.1"));

            TcpDiscoverySpi spi = new TcpDiscoverySpi();
            spi.setIpFinder(finder);

            cfg.setDiscoverySpi(spi);

            if (args0.persistenceEnabled) {
                PersistentStoreConfiguration pcfg = new PersistentStoreConfiguration();

                pcfg.setWalMode(args0.walMode);

                cfg.setPersistentStoreConfiguration(pcfg);
            }

            ignite = Ignition.start(cfg);
        }

        CacheConfiguration<Object, Object> ccfg = new CacheConfiguration<>(CACHE_NAME)
            .setCacheStoreFactory(
                new Factory<CacheStore<? super Object, ? super Object>>() {
                    @Override public CacheStore<? super Object, ? super Object> create() {
                        return new LoadStore();
                    }
                });

        if (args0.idx)
            ccfg.setIndexedTypes(AffinityKey.class, ZipEntity.class);

        ignite.active(true);

        IgniteCache<Object, Object> cache = ignite.getOrCreateCache(
            ccfg);

        cache.loadCache(null, args0);
    }

    /**
     *
     */
    private static class Arguments {
        /** Compression type. */
        private CompressionType compType = CompressionType.NONE;

        /** Load threads. */
        private int loadThreads = Runtime.getRuntime().availableProcessors();

        /** String randomization. */
        private double strRandomization = 1.0;

        /** Range. */
        private int range = 1_000_000;

        /** Ignite config url. */
        private String igniteCfgUrl;

        /** Index. */
        private boolean idx;

        /** Small entry. */
        private boolean smallEntry;

        /** Persistence enabled. */
        private boolean persistenceEnabled;

        /** Wal mode. */
        private WALMode walMode = WALMode.NONE;

        /**
         * Default constructor.
         */
        public Arguments() {
        }

        /**
         * @param args Args.
         */
        private static Arguments parseArguments(String[] args) {
            Arguments args0 = new Arguments();

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-ct":
                    case "--compressorType":
                        args0.compType = CompressionType.valueOf(args[++i]);

                        break;

                    case "-t":
                    case "--threads":
                        args0.loadThreads = Integer.parseInt(args[++i]);

                        break;

                    case "-sr":
                    case "--stringRandomization":
                        args0.strRandomization = Double.parseDouble(args[++i]);

                        break;

                    case "-r":
                    case "--range":
                        args0.range = Integer.parseInt(args[++i]);

                        break;

                    case "-cfg":
                    case "--igniteConfigPath":
                        args0.igniteCfgUrl = args[++i];

                        break;

                    case "-idx":
                    case "--index":
                        args0.idx = true;

                        break;

                    case "-se":
                    case "--smallEntry":
                        args0.smallEntry = true;

                        break;

                    case "-pe":
                    case "--persistenceEnabled":
                        args0.persistenceEnabled = true;

                        break;

                    case "-wm":
                    case "--walMode":
                        args0.walMode = WALMode.valueOf(args[++i]);

                        break;
                }
            }

            return args0;
        }
    }
}
