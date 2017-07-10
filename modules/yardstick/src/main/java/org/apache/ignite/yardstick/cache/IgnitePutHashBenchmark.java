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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.affinity.AffinityFunction;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.yardstick.cache.model.SampleValue;
import org.yardstickframework.BenchmarkConfiguration;

/**
 * Ignite benchmark that performs put operations.
 */
public class IgnitePutHashBenchmark extends IgniteCacheAbstractBenchmark<Integer, Object> {
    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        Collection<String> names = ignite().cacheNames();

        HashMap<String, HashMap<Integer,Long>> map = new HashMap<>();

        ArrayList<String> names1 = new ArrayList<>(names.size());

        for (String n : names)
            names1.add(n);

        for(String name : names) {
            final IgniteCache<Object, Object> cache = ignite().cache(name);

            HashMap<Integer, Long> partKeySum = new HashMap<>();

            CacheConfiguration cfg = cache.getConfiguration(CacheConfiguration.class);

            AffinityFunction af = cfg.getAffinity();
            int p = af.partitions();
            for (int i = 0; i < p; i++) {
                final int[] arr = {0};
                final long[] hash = {0L};
                arr[0] = i;
                Long res = ignite().compute().affinityCall(name, i, new IgniteCallable<Object>() {
                    @Override public Long call() {

                        QueryCursor entries = cache.query(new
                            ScanQuery<>(arr[0]));
                        List<Cache.Entry> list = entries.getAll();
                        for(Cache.Entry entry : list){
                            Object key = entry.getKey();
                            sum = sum + key.hashCode();
                        }
                        return  sum;
                    }
                });


            }

        }

        final IgniteCache<Object, Object> cache1 = ignite().cache("query");

        CacheConfiguration cfg = cache1.getConfiguration(CacheConfiguration.class);

        AffinityFunction af = cfg.getAffinity();

        int p = af.partitions();

        Integer key = 12;
        Integer val = 12;

        ignite().cache("query").put(key, val);

        final int[] sum = {0};

        for (int i = 0; i < p; i++) {
            final int[] arr = {0};
            arr[0] = i;
            ignite().compute().affinityRun(names, i, new IgniteRunnable() {
                IgniteEx ie;

                @Override public void run() {
                    //System.out.println(arr[0]);

                    QueryCursor entries = cache1.query(new
                        ScanQuery<>(arr[0]));
                    List<Cache.Entry> list = entries.getAll();

                }
            });

        }
        return true;
    }

    /** {@inheritDoc} */
    @Override protected IgniteCache<Integer, Object> cache() {
        return ignite().cache("atomic");
    }

    private class PartHashRunnable implements IgniteRunnable{
        String cacheName;
        int id;

        PartHashRunnable(String cacheName, int id){
            this.cacheName = cacheName;
            this.id = id;
        }

        @Override public void run() {
            QueryCursor entries = cache.query(new
                ScanQuery<>(id));
            List<Cache.Entry> list = entries.getAll();

        }
    }
}

