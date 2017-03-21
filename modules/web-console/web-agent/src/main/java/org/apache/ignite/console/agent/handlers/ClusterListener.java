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

package org.apache.ignite.console.agent.handlers;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.console.agent.rest.RestExecutor;
import org.apache.ignite.console.agent.rest.RestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ClusterListener {
    /** */
    private static final String EVENT_CLUSTER_CONNECTED = "cluster:connected";

    /** */
    private static final String EVENT_CLUSTER_TOPOLOGY = "cluster:topology";

    private enum State {
        CONNECTED,
        BROADCAST,
        DISCONNECTED
    }

    private static State state = State.DISCONNECTED;

    private static boolean connected = false;


    /** Default timeout. */
    private static final long DFLT_TIMEOUT = 3000L;

    /** */
    private static final Logger log = LoggerFactory.getLogger(ClusterListener.class);

    /** */
    private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

    /** */
    private ScheduledFuture<?> refreshTask;

    /** */
    private Socket client;

    /** */
    private RestExecutor restExecutor;

    /**
     * @param client Client.
     * @param restExecutor Client.
     */
    public ClusterListener(Socket client, RestExecutor restExecutor) {
        this.client = client;
        this.restExecutor = restExecutor;
    }

    /**
     *
     */
    public void watch() {
        refreshTask = pool.scheduleWithFixedDelay(new Runnable() {
            @Override public void run() {
                try {
                    RestResult top = restExecutor.topology(false, false);

                    if (state == State.DISCONNECTED) {
                        state = State.CONNECTED;

                        log.info("Connection to cluster successfully established");

                        client.emit(EVENT_CLUSTER_CONNECTED, top);
                    }
                }
                catch (IOException e) {
                    log.debug("Failed to execute topology command on cluster", e);
                }
            }
        }, 0L, DFLT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Start broadcast topology to server-side.
     */
    public Emitter.Listener start() {
        return new Emitter.Listener() {
            @Override public void call(Object... args) {
                refreshTask.cancel(true);

                state = State.BROADCAST;

                final long timeout = args.length > 1  && args[1] instanceof Long ? (long)args[1] : DFLT_TIMEOUT;

                refreshTask = pool.scheduleWithFixedDelay(new Runnable() {
                    @Override public void run() {
                        try {
                            RestResult top = restExecutor.topology(false, true);

                            client.emit(EVENT_CLUSTER_TOPOLOGY, top);
                        }
                        catch (IOException e) {
                            log.info("Lost connection to the cluster", e);

                            stop();
                        }
                    }
                }, 0L, timeout, TimeUnit.MILLISECONDS);
            }
        };
    }

    /**
     * Stop broadcast topology to server-side.
     */
    public Emitter.Listener stop() {
        return new Emitter.Listener() {
            @Override public void call(Object... args) {
                refreshTask.cancel(true);

                state = State.DISCONNECTED;

                watch();
            }
        };
    }
}
