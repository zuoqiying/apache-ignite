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
import org.apache.ignite.internal.processors.cache.GridCacheMessage;
import org.apache.ignite.internal.processors.cache.distributed.dht.atomic.GridNearAtomicAbstractUpdateFuture;
import org.apache.ignite.internal.processors.cache.distributed.dht.atomic.GridNearAtomicUpdateRequest;
import org.apache.ignite.internal.trace.TraceProcessor;
import org.apache.ignite.internal.trace.TraceThreadData;
import org.apache.ignite.internal.util.nio.GridNioFuture;
import org.apache.ignite.internal.util.nio.GridNioServer;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Atomic cache trace utils.
 */
@SuppressWarnings("UnusedParameters")
public class AtomicTrace {
    /** */
    public static final String GRP_CLIENT_REQ_SND = "CLI_REQ_SND";

    /** */
    public static final String GRP_CLIENT_REQ_SND_IO = "CLI_REQ_SND_IO";

    /** */
    public static final String GRP_RCV_IO = "RCV_IO";

    /** */
    public static final String GRP_SRV_REQ_RCV = "SRV_REQ_RCV";

    /** Trace processor. */
    private static final TraceProcessor PROC = TraceProcessor.shared();

    public static void _01_onClientSendStart(GridNearAtomicAbstractUpdateFuture fut) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND);

            trace.begin();

            trace.incrementState();

            trace.objectValue(0, fut.id());
            trace.longValue(1, System.nanoTime());
        }
    }

    public static void _02_onClientSendBeforeIo(GridCacheMessage msg) {
        if (PROC.isEnabled()) {
            if (msg instanceof GridNearAtomicUpdateRequest) {
                TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND);

                trace.incrementState();

                trace.longValue(0, msg.messageId());
                trace.longValue(2, System.nanoTime());
            }
        }
    }

    public static void _03_onClientSendOffer(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridNearAtomicUpdateRequest req = requestFromFuture(fut);

            if (req != null) {
                TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND);

                trace.incrementState();

                trace.longValue(3, System.nanoTime());

                if (trace.state() == 3) {
                    trace.pushData(new AtomicTraceClientSend(
                        (UUID)trace.objectValue(0),
                        trace.longValue(0),
                        trace.longValue(1),
                        trace.longValue(2),
                        trace.longValue(3)
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

                HashMap<Long, AtomicTraceClientSendIo> map = trace.objectValue(0);

                if (map == null) {
                    trace.begin();

                    map = new HashMap<>();

                    trace.objectValue(0, map);
                }

                map.put(req.messageId(), new AtomicTraceClientSendIo(System.nanoTime(), 0, 0, 0, 0));
            }
        }
    }

    public static void _05_onClientSendIoMarshalled(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridNearAtomicUpdateRequest req = requestFromFuture(fut);

            if (req != null) {
                TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND_IO);

                HashMap<Long, AtomicTraceClientSendIo> map = trace.objectValue(0);

                if (map != null) {
                    AtomicTraceClientSendIo msg = map.get(req.messageId());

                    if (msg != null)
                        msg.marshalled = System.nanoTime();
                }
            }
        }
    }

    public static void _06_onClientSendIoWritten(int bufLen) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_CLIENT_REQ_SND_IO);

            HashMap<Long, AtomicTraceClientSendIo> map = trace.objectValue(0);

            if (map != null) {
                long sndTime = System.nanoTime();

                HashMap<Long, AtomicTraceClientSendIo> res = new HashMap<>();

                Iterator<Map.Entry<Long, AtomicTraceClientSendIo>> iter = map.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<Long, AtomicTraceClientSendIo> entry = iter.next();

                    AtomicTraceClientSendIo msg = entry.getValue();

                    if (msg.marshalled != 0) {
                        msg.sent = sndTime;
                        msg.bufLen = bufLen;

                        res.put(entry.getKey(), msg);

                        iter.remove();
                    }
                }

                int msgCnt = res.size();

                for (AtomicTraceClientSendIo msg : res.values())
                    msg.msgCnt = msgCnt;

                if (!res.isEmpty())
                    trace.pushData(res);

                if (map.isEmpty())
                    trace.end();
            }
        }
    }

    /**
     * Invoked when IO read started.
     *
     * @param len Data length.
     */
    public static void onIoReadStarted(int len) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_RCV_IO);

            trace.begin();

            trace.intValue(0, len);
            trace.longValue(0, System.nanoTime());
        }
    }

    /**
     * Invoked when IO message is unmarshalled.
     *
     * @param msg Message.
     */
    public static void onIoReadUnmarshalled(Object msg) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_RCV_IO);

            GridNearAtomicUpdateRequest req = requestFromMessage(msg);

            if (req != null) {
                IdentityHashMap<GridNearAtomicUpdateRequest, AtomicTraceReceiveIo> reqMap = trace.objectValue(0);

                if (reqMap == null) {
                    reqMap = new IdentityHashMap<>();

                    trace.objectValue(0, reqMap);
                }

                AtomicTraceReceiveIo io = new AtomicTraceReceiveIo(
                    trace.intValue(0),
                    null,
                    req.messageId(),
                    trace.longValue(0),
                    System.nanoTime(),
                    0
                );

                reqMap.put(req, io);
            }
        }
    }

    /**
     * Invoked when IO message is offered to the thread pool.
     *
     * @param msg Message.
     */
    public static void onIoReadOffered(UUID nodeId, GridIoMessage msg) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_RCV_IO);

            GridNearAtomicUpdateRequest req = requestFromMessage(msg);

            if (req != null) {
                IdentityHashMap<GridNearAtomicUpdateRequest, AtomicTraceReceiveIo> reqMap = trace.objectValue(0);

                AtomicTraceReceiveIo io = reqMap.get(req);

                io.nodeId = nodeId;
                io.offered = System.nanoTime();
            }
        }
    }

    /**
     * Invoked when IO read finished.
     */
    public static void onIoReadFinished() {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_RCV_IO);

            IdentityHashMap<GridNearAtomicUpdateRequest, AtomicTraceReceiveIo> reqMap = trace.objectValue(0);

            if (reqMap != null) {
                Map<AtomicTraceReceiveMessageKey, AtomicTraceReceiveIo> res = new HashMap<>(reqMap.size());

                Iterator<Map.Entry<GridNearAtomicUpdateRequest, AtomicTraceReceiveIo>> iter =
                    reqMap.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<GridNearAtomicUpdateRequest, AtomicTraceReceiveIo> entry = iter.next();

                    AtomicTraceReceiveIo io = entry.getValue();

                    if (io.offered != 0L) {
                        res.put(new AtomicTraceReceiveMessageKey(io.nodeId, io.msgId), io);

                        iter.remove();
                    }
                }

                trace.pushData(res);
            }

            if (F.isEmpty(reqMap))
                trace.end();
        }
    }

    @Nullable private static GridNearAtomicUpdateRequest requestFromMessage(Object msg) {
        if (msg instanceof GridIoMessage) {
            Message msg0 = ((GridIoMessage)msg).message();

            if (msg0 instanceof GridNearAtomicUpdateRequest)
                return (GridNearAtomicUpdateRequest)msg0;
        }

        return null;
    }

    @Nullable private static GridNearAtomicUpdateRequest requestFromFuture(GridNioFuture fut) {
        if (fut instanceof GridNioServer.NioOperationFuture) {
            GridNioServer.NioOperationFuture fut0 = (GridNioServer.NioOperationFuture)fut;

            if (fut0.directMessage() instanceof GridIoMessage)
                return requestFromMessage((GridIoMessage)fut0.directMessage());
        }

        return null;
    }
}
