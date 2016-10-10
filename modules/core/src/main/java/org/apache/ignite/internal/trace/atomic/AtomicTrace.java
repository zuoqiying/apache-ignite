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
import org.apache.ignite.internal.processors.cache.distributed.dht.atomic.GridNearAtomicUpdateResponse;
import org.apache.ignite.internal.trace.TraceProcessor;
import org.apache.ignite.internal.trace.TraceThreadData;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataClient;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataSendIo;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataReceiveIo;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataMessageKey;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataServer;
import org.apache.ignite.internal.util.nio.GridNioFuture;
import org.apache.ignite.internal.util.nio.GridNioServer;
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
    public static final String GRP_CLI = "CLIENT";

    /** */
    public static final String GRP_IO_SND = "IO_SND";

    /** */
    public static final String GRP_IO_RCV = "IO_RCV";

    /** */
    public static final String GRP_SYS = "SYS";

    /** Trace processor. */
    private static final TraceProcessor PROC = TraceProcessor.shared();

    /* --- BEGIN SIDE --- */

    /**
     * Invoked on operation start.
     *
     * @param fut Future.
     */
    public static void onClientStarted(GridNearAtomicAbstractUpdateFuture fut) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_CLI);

            trace.begin();

            trace.objectValue(0, new AtomicTraceDataClient(fut.id()));
        }
    }

    /**
     * Invoked before request offer.
     *
     * @param fromNode From node.
     * @param toNode To node.
     */
    public static void onClientBeforeOffer(GridCacheMessage msg, UUID fromNode, UUID toNode) {
        if (PROC.isEnabled()) {
            if (msg instanceof GridNearAtomicUpdateRequest) {
                TraceThreadData trace = PROC.threadData(GRP_CLI);

                trace.objectValue(1, fromNode);
                trace.objectValue(2, toNode);
            }
        }
    }

    /**
     * Invoked on message offer.
     *
     * @param fut Future.
     */
    public static void onClientOffered(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridNearAtomicUpdateRequest req = requestFromFuture(fut);

            if (req != null) {
                TraceThreadData trace = PROC.threadData(GRP_CLI);

                AtomicTraceDataClient data = trace.objectValue(0);
                UUID fromNode = trace.objectValue(1);
                UUID toNode = trace.objectValue(2);

                if (data != null && fromNode != null && toNode != null)
                    data.addRequestTrace(fromNode, toNode, req.messageId());
            }
        }
    }

    /**
     * Invoked when client part is finished.
     */
    public static void onClientFinished() {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_CLI);

            AtomicTraceDataClient data = trace.objectValue(0);

            if (data != null)
                trace.pushData(data);

            trace.end();
        }
    }

    /* --- IO write. --- */

    /**
     * Invoked on write start.
     */
    public static void onIoWriteStarted() {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_IO_SND);

            trace.begin();

            trace.objectValue(0, new HashMap<>());
        }
    }

    /**
     * Invoked when data is polled from the queue.
     *
     * @param fut Future.
     */
    public static void onIoWritePolled(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridCacheMessage msg = requestFromFuture(fut);

            if (msg == null)
                msg = responseFromFuture(fut);

            if (msg != null) {
                TraceThreadData trace = PROC.threadData(GRP_IO_SND);

                HashMap<Long, AtomicTraceDataSendIo> map = trace.objectValue(0);

                if (map != null)
                    map.put(msg.messageId(), new AtomicTraceDataSendIo(System.nanoTime()));
            }
        }
    }

    /**
     * Invoked when message is marshalled.
     *
     * @param fut Future.
     */
    public static void onIoWriteMarshalled(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridCacheMessage msg = requestFromFuture(fut);

            if (msg == null)
                msg = responseFromFuture(fut);

            if (msg != null) {
                TraceThreadData trace = PROC.threadData(GRP_IO_SND);

                HashMap<Long, AtomicTraceDataSendIo> map = trace.objectValue(0);

                if (map != null) {
                    AtomicTraceDataSendIo io = map.get(msg.messageId());

                    if (io != null)
                        io.onMarshalled(System.nanoTime());
                }
            }
        }
    }

    /**
     * Invoked when write is finished.
     *
     * @param dataLen Data length.
     */
    public static void onIoWriteFinished(int dataLen) {
        if (PROC.isEnabled()) {
            // Process requests.
            TraceThreadData trace = PROC.threadData(GRP_IO_SND);

            HashMap<Long, AtomicTraceDataSendIo> map = trace.objectValue(0);

            if (map != null) {
                long sndTime = System.nanoTime();

                HashMap<Long, AtomicTraceDataSendIo> res = new HashMap<>();

                Iterator<Map.Entry<Long, AtomicTraceDataSendIo>> iter = map.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<Long, AtomicTraceDataSendIo> entry = iter.next();

                    AtomicTraceDataSendIo msg = entry.getValue();

                    if (msg.marshalled != 0) {
                        msg.sent = sndTime;
                        msg.bufLen = dataLen;

                        res.put(entry.getKey(), msg);

                        iter.remove();
                    }
                }

                int msgCnt = res.size();

                for (AtomicTraceDataSendIo msg : res.values())
                    msg.msgCnt = msgCnt;

                if (!res.isEmpty())
                    trace.pushData(res);
            }

            trace.end();
        }
    }

    /* --- IO read. --- */

    /**
     * Invoked when IO read started.
     *
     * @param len Data length.
     */
    public static void onIoReadStarted(int len) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_IO_RCV);

            trace.begin();

            trace.intValue(0, len);
            trace.longValue(0, System.nanoTime());
            trace.objectValue(0, new IdentityHashMap<GridCacheMessage, AtomicTraceDataReceiveIo>());
        }
    }

    /**
     * Invoked when IO message is unmarshalled.
     *
     * @param obj Message.
     */
    public static void onIoReadUnmarshalled(Object obj) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_IO_RCV);

            GridCacheMessage msg = requestFromMessage(obj);

            if (msg == null)
                msg = responseFromMessage(obj);

            if (msg != null) {
                IdentityHashMap<GridCacheMessage, AtomicTraceDataReceiveIo> reqMap = trace.objectValue(0);

                if (reqMap == null) {
                    reqMap = new IdentityHashMap<>();

                    trace.objectValue(0, reqMap);
                }

                AtomicTraceDataReceiveIo io = new AtomicTraceDataReceiveIo(
                    trace.intValue(0),
                    trace.longValue(0),
                    System.nanoTime()
                );

                reqMap.put(msg, io);
            }
        }
    }

    /**
     * Invoked when IO message is offered to the thread pool.
     *
     * @param fromNode From node ID.
     * @param toNode To node ID.
     * @param obj Message.
     */
    public static void onIoReadOffered(UUID fromNode, UUID toNode, Object obj) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_IO_RCV);

            GridCacheMessage msg = requestFromMessage(obj);

            if (msg == null)
                msg = responseFromMessage(obj);

            if (msg != null) {
                IdentityHashMap<GridCacheMessage, AtomicTraceDataReceiveIo> reqMap = trace.objectValue(0);

                if (reqMap != null) {
                    AtomicTraceDataReceiveIo io = reqMap.get(msg);

                    if (io != null) {
                        io.onOffer(System.nanoTime());

                        Map<AtomicTraceDataMessageKey, AtomicTraceDataReceiveIo> resMap = trace.objectValue(1);

                        if (resMap == null) {
                            resMap = new HashMap<>();

                            trace.objectValue(1, resMap);
                        }

                        resMap.put(new AtomicTraceDataMessageKey(fromNode, toNode, msg.messageId()), io);
                    }
                }
            }
        }
    }

    /**
     * Invoked when IO read finished.
     */
    public static void onIoReadFinished() {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_IO_RCV);

            IdentityHashMap<AtomicTraceDataMessageKey, AtomicTraceDataReceiveIo> resMap = trace.objectValue(1);

            if (resMap != null)
                trace.pushData(resMap);

            trace.end();
        }
    }

    /* --- System pool. --- */

    /**
     * Onvoked when server processing started.
     *
     * @param fromNode From node.
     * @param toNode To node.
     * @param msgId Message ID.
     */
    public static void onServerStarted(UUID fromNode, UUID toNode, long msgId) {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_SYS);

            trace.begin();

            trace.objectValue(0, new AtomicTraceDataServer(fromNode, toNode, msgId, System.nanoTime()));
        }
    }

    /**
     * Invoked on message offer.
     *
     * @param fut Future.
     */
    public static void onServerOffered(GridNioFuture fut) {
        if (PROC.isEnabled()) {
            GridNearAtomicUpdateResponse resp = responseFromFuture(fut);

            if (resp != null) {
                TraceThreadData trace = PROC.threadData(GRP_SYS);

                AtomicTraceDataServer data = trace.objectValue(0);

                if (data != null)
                    data.onOffer(resp.messageId(), System.nanoTime());
            }
        }
    }

    /**
     * Invoked when server processing is finished.
     */
    public static void onServerFinished() {
        if (PROC.isEnabled()) {
            TraceThreadData trace = PROC.threadData(GRP_SYS);

            AtomicTraceDataServer data = trace.objectValue(0);

            if (data != null)
                trace.pushData(data);

            trace.end();
        }
    }

    /**
     * Get request from message.
     *
     * @param msg Message.
     * @return Request.
     */
    @Nullable private static GridNearAtomicUpdateRequest requestFromMessage(Object msg) {
        if (msg instanceof GridIoMessage) {
            Message msg0 = ((GridIoMessage)msg).message();

            if (msg0 instanceof GridNearAtomicUpdateRequest)
                return (GridNearAtomicUpdateRequest)msg0;
        }

        return null;
    }

    /**
     * Get request from NIO future.
     *
     * @param fut Future.
     * @return Request.
     */
    @Nullable private static GridNearAtomicUpdateRequest requestFromFuture(GridNioFuture fut) {
        if (fut instanceof GridNioServer.NioOperationFuture) {
            GridNioServer.NioOperationFuture fut0 = (GridNioServer.NioOperationFuture)fut;

            if (fut0.directMessage() instanceof GridIoMessage)
                return requestFromMessage(fut0.directMessage());
        }

        return null;
    }

    /**
     * Get response from message.
     *
     * @param msg Message.
     * @return Response.
     */
    @Nullable private static GridNearAtomicUpdateResponse responseFromMessage(Object msg) {
        if (msg instanceof GridIoMessage) {
            Message msg0 = ((GridIoMessage)msg).message();

            if (msg0 instanceof GridNearAtomicUpdateResponse)
                return (GridNearAtomicUpdateResponse)msg0;
        }

        return null;
    }

    /**
     * Get response from NIO future.
     *
     * @param fut Future.
     * @return Response.
     */
    @Nullable private static GridNearAtomicUpdateResponse responseFromFuture(GridNioFuture fut) {
        if (fut instanceof GridNioServer.NioOperationFuture) {
            GridNioServer.NioOperationFuture fut0 = (GridNioServer.NioOperationFuture)fut;

            if (fut0.directMessage() instanceof GridIoMessage)
                return responseFromMessage(fut0.directMessage());
        }

        return null;
    }
}
