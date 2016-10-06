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

import org.apache.ignite.internal.util.typedef.F;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Trace processor.
 */
public class TraceProcessor {
    /** Instance. */
    private static final TraceProcessor INSTANCE = new TraceProcessor();

    /** Thread-local data. */
    private final ThreadLocal<TraceThreadData> TL_DATA = new ThreadLocal<>();

    /** Multiple thread-local data. */
    private final ThreadLocal<List<TraceThreadData>> TL_DATAS = new ThreadLocal<>();

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
    public static TraceProcessor shared() {
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
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public TraceThreadData threadData(String grpName) {
        TraceThreadData data = TL_DATA.get();

        if (data == null) {
            List<TraceThreadData> datas = TL_DATAS.get();

            if (datas == null) {
                data = registerThread(grpName);

                TL_DATA.set(data);
            }
            else {
                for (int i = 0; i < datas.size(); i++) {
                    TraceThreadData data0 = datas.get(i);

                    if (F.eq(data0.group().name(), grpName)) {
                        data = data0;

                        break;
                    }
                }

                if (data == null) {
                    data = registerThread(grpName);

                    datas.add(data);
                }
            }
        }
        else if (!F.eq(data.group().name(), grpName)) {
            List<TraceThreadData> datas = new ArrayList<>(2);

            datas.add(data);

            data = registerThread(grpName);

            datas.add(data);

            TL_DATA.set(null);
            TL_DATAS.set(datas);
        }

        assert data != null;

        return data;
    }

    /**
     * Register thread in the group.
     *
     * @param grpName Group name.
     * @return Thread data.
     */
    private TraceThreadData registerThread(String grpName) {
        TraceThreadGroup grp = threadGroup(grpName, true);

        assert grp != null;

        return grp.registerThread(Thread.currentThread());
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
