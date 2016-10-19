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

package org.apache.ignite.internal.processors.pool;

import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.lang.IgniteOutClosure;

/**
 * User waitable primitive.
 */
public class UserWaitable<T> {
    /** Mutex. */
    private final Object mux = new Object();

    private IgniteInternalFuture<T> fut;

    /** Current state. */
    private State state = State.INITIALIZED;

    /** Closure set by completion thread. */
    private IgniteOutClosure<T> clo;

    /**
     * Await for result.
     *
     * @return Result.
     */
    public T get() {
        synchronized (mux) {
            // TODO
        }

        return null;
    }

    /**
     * Set completion closure.
     *
     * @param clo Closure.
     * @return If closure will be processed by the user thread.
     */
    public boolean onDone(IgniteOutClosure<T> clo) {
        synchronized (mux) {
            // TODO
        }

        return false;
    }

    private enum State {
        /** Initialized. */
        INITIALIZED,

        /** User thread is caught and waiting. */
        WAITING,

        /** Future is abandoned, user thread will not process it. */
        ABANDONED,

        /** Execution is passed to async engine. */
        ASYNC;
    }
}
