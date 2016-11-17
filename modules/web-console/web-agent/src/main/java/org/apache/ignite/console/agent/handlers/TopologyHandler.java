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
import org.apache.ignite.console.agent.AgentConfiguration;
import org.apache.ignite.console.agent.RestExecutor;
import org.apache.ignite.console.agent.RestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TopologyHandler {
    /** */
    private static final String EVENT_TOPOLOGY = "cluster:topology";

    /** Default timeout. */
    private static final long DFLT_TIMEOUT = 5000L;

    /** */
    private static final Logger log = LoggerFactory.getLogger(TopologyHandler.class);

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
    public TopologyHandler(Socket client, RestExecutor restExecutor) {
        this.client = client;
        this.restExecutor = restExecutor;
    }

    /**
     * Listener for start topology send.
     */
    public Emitter.Listener start() {
        return new Emitter.Listener() {
            @Override public void call(Object... args) {
                final boolean demo = args.length > 0 && boolean.class.equals(args[0].getClass()) && (boolean)args[0];
                long timeout = args.length >= 2  && long.class.equals(args[1].getClass()) ? (long)args[1] : DFLT_TIMEOUT;

                refreshTask = pool.scheduleWithFixedDelay(new Runnable() {
                    @Override public void run() {
                        try {
                            RestResult top = restExecutor.topology(demo);

                            client.emit(EVENT_TOPOLOGY, top);
                        }
                        catch (IOException e) {
                            log.info("Failed to collect topology");
                        }
                    }
                }, 0L, timeout, TimeUnit.MILLISECONDS);
            }
        };
    }

    /**
     * Listener for stop topology send.
     */
    public Emitter.Listener stop() {
        return new Emitter.Listener() {
            @Override public void call(Object... args) {
                refreshTask.cancel(true);
            }
        };
    }
}
