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

import java.util.HashMap;

/**
 * Group of similar threads.
 */
public class TraceThreadGroup {
    /** Mutex. */
    private final Object mux = new Object();

    /** Processor. */
    private final TraceProcessor proc;

    /** Name. */
    private final String name;

    /** Thread data. */
    private final HashMap<Thread, TraceThreadData> datas = new HashMap<>();

    /**
     * Constructor.
     *
     * @param proc Processor.
     * @param name Name.
     */
    public TraceThreadGroup(TraceProcessor proc, String name) {
        this.proc = proc;
        this.name = name;
    }

    /**
     * Register thread.
     *
     * @param thread Thread.
     * @return Thread data.
     */
    public TraceThreadData registerThread(Thread thread) {
        synchronized (mux) {
            TraceThreadData data = new TraceThreadData(this, thread);

            datas.put(thread, data);

            return data;
        }
    }

    /**
     * Get all threads data.
     *
     * @return Threads data.
     */
    public HashMap<Thread, TraceThreadData> threads() {
        synchronized (mux) {
            return new HashMap<>(datas);
        }
    }

    /**
     * @return Thread processor.
     */
    public TraceProcessor processor() {
        return proc;
    }

    /**
     * @return Name.
     */
    public String name() {
        return name;
    }
}
