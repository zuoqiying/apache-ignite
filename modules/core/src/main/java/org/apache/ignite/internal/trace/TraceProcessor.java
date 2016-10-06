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

package org.apache.ignite.internal.trace;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * Trace processor.
 */
public class TraceProcessor {
    /** Instance. */
    private static final TraceProcessor INSTANCE = new TraceProcessor();

    /** Data bounded to the thread. */
    private final ThreadLocal<TraceThreadData> TL_DATA = new ThreadLocal<>();

    /** Mutex. */
    private final Object mux = new Object();

    /** Thread groups. */
    private volatile HashMap<String, TraceThreadGroup> grps;

    /** Enabled flag. */
    private volatile boolean enabled;

    /**
     * Get processor.
     *
     * @return Processor.
     */
    public static TraceProcessor get() {
        return INSTANCE;
    }

    /**
     * Private constructor.
     */
    private TraceProcessor() {
        // No-op.
    }

    /**
     * @return {@code True} if tracing is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable tracing.
     */
    public void enable() {
        enabled = true;
    }

    /**
     * Disable tracing,
     */
    public void disable() {
        enabled = false;
    }

    /**
     * Get thread in the given thread group registering the thread if needed.
     *
     * @param grpName Group name.
     * @return Thread data.
     */
    public TraceThreadData threadData(String grpName) {
        TraceThreadData data = TL_DATA.get();

        if (data == null) {
            TraceThreadGroup grp = threadGroup(grpName, true);

            assert grp != null;

            data = grp.registerThread(Thread.currentThread());

            TL_DATA.set(data);
        }

        return data;
    }

    /**
     * Get thread group.
     *
     * @param name Group name.
     * @return Thread group.
     */
    @Nullable public TraceThreadGroup threadGroup(String name) {
        return threadGroup(name, false);
    }

    /**
     * Get thread group creating it if necessary..
     *
     * @param name Name.
     * @param create Create flag.
     * @return Thread group.
     */
    @Nullable public TraceThreadGroup threadGroup(String name, boolean create) {
        HashMap<String, TraceThreadGroup> grps0 = grps;

        // Initialize map if needed.
        if (grps0 == null) {
            synchronized (mux) {
                grps0 = grps;

                if (grps0 == null) {
                    grps0 = new HashMap<>();

                    grps = grps0;
                }
            }
        }

        // Perform lookup.
        TraceThreadGroup grp = grps0.get(name);

        if (grp == null && create) {
            synchronized (mux) {
                grp = grps.get(name);

                if (grp == null) {
                    grp = new TraceThreadGroup(this, name);

                    HashMap<String, TraceThreadGroup> newGrps = new HashMap<>(grps);

                    newGrps.put(name, grp);

                    grps = newGrps;
                }
            }
        }

        return grp;
    }
}
