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

import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.typedef.internal.U;

import java.io.File;

/**
 * Atomic trace utility methods.
 */
public class AtomicTraceUtils {
    /** Cache name. */
    public static final String CACHE_NAME = "cache";

    /** Trace directory. */
    //private static final String TRACE_DIR = System.getProperty("TRACE_DIR");
    private static final String TRACE_DIR = "C:\\Personal\\atomic_trace";

    /**
     * Get trace directory.
     *
     * @return Trace directory.
     */
    @SuppressWarnings("ConstantConditions")
    public static File traceDir() {
        if (TRACE_DIR == null || TRACE_DIR.isEmpty())
            throw new RuntimeException("TRACE_DIR is not defined.");

        return new File(TRACE_DIR);
    }

    /**
     * Get trace file with the given index.
     *
     * @param idx Index.
     * @return Trace file.
     */
    public static File traceFile(int idx) {
        return new File(traceDir(), "trace-" + idx + ".log");
    }

    /**
     * Clear trace directory.
     *
     * @return Directory.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File prepareTraceDir() {
        File dir = traceDir();

        if (dir.exists()) {
            if (!U.delete(dir))
                throw new RuntimeException("Failed to clear trace dir: " + dir.getAbsolutePath());
        }

        dir.mkdirs();

        return dir;
    }

    /**
     * Create configuration.
     *
     * @param name Name.
     * @param client Client flag.
     * @return Configuration.
     */
    public static IgniteConfiguration config(String name, boolean client) {
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
     * Private constructor.
     */
    private AtomicTraceUtils() {
        // No-op.
    }
}
