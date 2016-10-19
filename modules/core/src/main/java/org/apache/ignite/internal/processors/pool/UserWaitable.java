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

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.cache.distributed.dht.atomic.GridNearAtomicAbstractUpdateFuture;
import org.apache.ignite.internal.processors.cache.distributed.dht.atomic.GridNearAtomicUpdateResponse;

import java.util.UUID;

/**
 * User waitable primitive.
 */
public class UserWaitable {
    /** Mutex. */
    private final Object mux = new Object();

    /** Target future. */
    private final GridNearAtomicAbstractUpdateFuture fut;

    /** Current state. */
    private State state = State.INITIALIZED;

    /** Closure set by completion thread. */
    private UUID nodeId;

    /** Response. */
    private GridNearAtomicUpdateResponse res;

    /**
     * Future.
     *
     * @param fut Future.
     */
    public UserWaitable(GridNearAtomicAbstractUpdateFuture fut) {
        this.fut = fut;
    }

    /**
     * Await for result.
     *
     * @return Result.
     * @throws IgniteCheckedException If failed.
     */
    public boolean get() throws IgniteCheckedException {
        UUID nodeId0 = null;
        GridNearAtomicUpdateResponse res0 = null;

        boolean interrupt = false;

        synchronized (mux) {
            switch (state) {
                case INITIALIZED:
                    state = State.WAITING;

                    try {
                        while (nodeId == null)
                            mux.wait();
                    }
                    catch (InterruptedException e) {
                        interrupt = true;

                        state = State.ABANDONED;
                    }

                    nodeId0 = nodeId;
                    res0 = res;

                    break;

                case WAITING:
                    throw new IllegalStateException("Cannot wait twice!");

                case ABANDONED:
                    throw new IllegalStateException("Already abandoned!");

                case ASYNC:
                    // We lost the race, just wait for the future.
                    break;

                default:
                    throw new IllegalStateException("Unknown state: " + state);
            }
        }

        // Complete the future if possible.
        if (nodeId0 != null) {
            assert res0 != null;

            fut.onResult(nodeId, res, false);
        }

        // Restore interrupt state if needed.
        if (interrupt)
            Thread.currentThread().interrupt();

        // Return the result.
        return (boolean)fut.get();
    }

    /**
     * Try setting response.
     *
     * @param nodeId Node ID.
     * @param res Response.
     * @return If result will be processed by user thread.
     */
    public boolean onDone(UUID nodeId, GridNearAtomicUpdateResponse res) {
        synchronized (mux) {
            switch (state) {
                case INITIALIZED:
                    state = State.ASYNC;

                    return false;

                case WAITING:
                    this.nodeId = nodeId;
                    this.res = res;

                    mux.notifyAll();

                    return true;

                case ABANDONED:
                    return false;

                case ASYNC:
                    throw new IllegalStateException("Cannot be in async state!");

                default:
                    throw new IllegalStateException("Unknown state: " + state);
            }
        }
    }

    /**
     * Current state.
     */
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
