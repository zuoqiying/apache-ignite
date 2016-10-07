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

import org.apache.ignite.internal.managers.communication.GridIoMessage;
import org.apache.ignite.internal.processors.cache.distributed.dht.atomic.GridNearAtomicAbstractUpdateFuture;
import org.apache.ignite.internal.processors.cache.distributed.dht.atomic.GridNearAtomicUpdateRequest;
import org.apache.ignite.internal.trace.TraceProcessor;
import org.apache.ignite.internal.trace.TraceThreadData;
import org.apache.ignite.internal.util.nio.GridNioFuture;
import org.apache.ignite.internal.util.nio.GridNioServer;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Atomic cache trace utils.
 */
@SuppressWarnings("UnusedParameters")
public class AtomicTrace {
    /** */
    private static final String GRP_CLIENT_REQ_SND = "CLI_REQ_SND";

    /** */
    private static final String GRP_CLIENT_REQ_SND_IO = "CLI_REQ_SND_IO";

    /** */
    private static final String GRP_SRV_REQ_RCV_IO = "SRV_REQ_RCV_IO";

    /** */
    private static final String GRP_SRV_REQ_RCV = "SRV_REQ_RCV";

    /** Trace processor. */
    private static final TraceProcessor PROC = TraceProcessor.shared();

    public static void _01_onClientSendStart(GridNearAtomicAbstractUpdateFuture fut) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND);

            trace.begin();

            trace.incrementState();

            trace.intValue(0, System.identityHashCode(fut));
            trace.longValue(0, System.nanoTime());
        }
    }

    public static void _02_onClientSendBeforeIo(GridNearAtomicAbstractUpdateFuture fut,
        GridNearAtomicUpdateRequest req) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND);

            trace.incrementState();

            trace.intValue(1, System.identityHashCode(req));
            trace.longValue(1, System.nanoTime());
        }
    }

    public static void _03_onClientSendOffer(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridNearAtomicUpdateRequest req = requestFromFuture(fut);

            if (req != null) {
                TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND);

                trace.incrementState();

                trace.longValue(2, System.nanoTime());

                if (trace.state() == 3) {
                    trace.pushData(new AtomicTraceClientSend(
                        trace.intValue(0),
                        trace.intValue(1),
                        trace.longValue(0),
                        trace.longValue(1) - trace.longValue(0),
                        trace.longValue(2) - trace.longValue(1)
                    ));
                }

                trace.end();
            }
        }
    }

    public static void _04_onClientSendIoPolled(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridNearAtomicUpdateRequest req = requestFromFuture(fut);

            if (req != null) {
                TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND_IO);

                HashMap<Integer, AtomicTraceClientSendIoMessage> map = trace.objectValue(0);

                if (map == null) {
                    trace.begin();

                    map = new HashMap<>();

                    trace.objectValue(0, map);
                }

                map.put(System.identityHashCode(req), new AtomicTraceClientSendIoMessage(System.nanoTime(), 0, 0));
            }
        }
    }

    public static void _05_onClientSendIoMarshalled(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridNearAtomicUpdateRequest req = requestFromFuture(fut);

            if (req != null) {
                TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND_IO);

                HashMap<Integer, AtomicTraceClientSendIoMessage> map = trace.objectValue(0);

                if (map != null) {
                    AtomicTraceClientSendIoMessage msg = map.get(System.identityHashCode(req));

                    if (msg != null)
                        msg.marshalled = System.nanoTime();
                }
            }
        }
    }

    public static void _06_onClientSendIoWritten(int len) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND_IO);

            HashMap<Integer, AtomicTraceClientSendIoMessage> map = trace.objectValue(0);

            if (map != null) {
                long sndTime = System.nanoTime();

                HashMap<Integer, AtomicTraceClientSendIoMessage> res = new HashMap<>();

                Iterator<Map.Entry<Integer, AtomicTraceClientSendIoMessage>> iter = map.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<Integer, AtomicTraceClientSendIoMessage> entry = iter.next();

                    AtomicTraceClientSendIoMessage msg = entry.getValue();

                    if (msg.marshalled != 0) {
                        msg.sent = sndTime;

                        res.put(entry.getKey(), msg);

                        iter.remove();
                    }
                }

                if (!res.isEmpty())
                    trace.pushData(new AtomicTraceClientSendIo(res));

                if (map.isEmpty())
                    trace.end();
            }
        }
    }

    @Nullable private static GridNearAtomicUpdateRequest requestFromFuture(GridNioFuture fut) {
        if (fut instanceof GridNioServer.NioOperationFuture) {
            GridNioServer.NioOperationFuture fut0 = (GridNioServer.NioOperationFuture)fut;

            if (fut0.directMessage() instanceof GridIoMessage) {
                Message msg = ((GridIoMessage)fut0.directMessage()).message();

                if (msg instanceof GridNearAtomicUpdateRequest)
                    return (GridNearAtomicUpdateRequest)msg;
            }
        }

        return null;
    }
}
