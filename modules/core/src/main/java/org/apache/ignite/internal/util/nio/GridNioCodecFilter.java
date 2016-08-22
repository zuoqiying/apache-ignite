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

package org.apache.ignite.internal.util.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;

/**
 * Filter that transforms byte buffers to user-defined objects and vice-versa
 * with specified {@link GridNioParser}.
 */
public class GridNioCodecFilter extends GridNioFilterAdapter {
    /** Parser used. */
    private GridNioParser parser;

    /** Grid logger. */
    @GridToStringExclude
    private IgniteLogger log;

    /** Whether direct mode is used. */
    private boolean directMode;

    /**
     * Creates a codec filter.
     *
     * @param parser Parser to use.
     * @param log Log instance to use.
     * @param directMode Whether direct mode is used.
     */
    public GridNioCodecFilter(GridNioParser parser, IgniteLogger log, boolean directMode) {
        super("GridNioCodecFilter");

        this.parser = parser;
        this.log = log;
        this.directMode = directMode;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNioCodecFilter.class, this);
    }

    /** {@inheritDoc} */
    @Override public void onSessionOpened(GridNioSession ses) throws IgniteCheckedException {
        proceedSessionOpened(ses);
    }

    /** {@inheritDoc} */
    @Override public void onSessionClosed(GridNioSession ses) throws IgniteCheckedException {
        proceedSessionClosed(ses);
    }

    /** {@inheritDoc} */
    @Override public void onExceptionCaught(GridNioSession ses, IgniteCheckedException ex) throws IgniteCheckedException {
        proceedExceptionCaught(ses, ex);
    }

    /** {@inheritDoc} */
    @Override public GridNioFuture<?> onSessionWrite(GridNioSession ses, Object msg) throws IgniteCheckedException {
        // No encoding needed in direct mode.
        if (directMode)
            return proceedSessionWrite(ses, msg);

        try {
            ByteBuffer res = parser.encode(ses, msg);

            return proceedSessionWrite(ses, res);
        }
        catch (IOException e) {
            throw new GridNioException(e);
        }
    }

    private ThreadPoolExecutor exec;

    private final ConcurrentMap<DirectReaderQueueKey, DirectReaderQueue<BufferReadSubChunk>> qMap = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override public void onMessageReceived(GridNioSession ses, Object msg) throws IgniteCheckedException {
//        if (!(msg instanceof ByteBuffer))
//            throw new GridNioException("Failed to decode incoming message (incoming message is not a byte buffer, " +
//                "is filter properly placed?): " + msg.getClass());

        BufferChunk chunk = ses.meta(GridNioSessionMetaKey.READ_CHUNK.ordinal());

        try {
            for (;;) {
                BufferReadSubChunk subChunk = chunk.nextSubChunk();

                if (subChunk == null)
                    return;

                UUID nodeId = ses.meta(TcpCommunicationSpi.NODE_ID_META);

                if (nodeId == null)
                    processSubChunk(ses, subChunk);
                else {
                    DirectReaderQueueKey key = new DirectReaderQueueKey(nodeId, subChunk.threadId());

                    DirectReaderQueue<BufferReadSubChunk> q = qMap.get(key);

                    if (q == null) {
                        DirectReaderQueue<BufferReadSubChunk> old = qMap.putIfAbsent(key, q = new DirectReaderQueue<>(ses));

                        assert old == null;
                    }

                    q.add(subChunk);

                    if (q.reserved())
                        continue;

                    final DirectReaderQueue<BufferReadSubChunk> finalQ = q;

//                    exec.submit(
//                        new Runnable() {
//                            @Override public void run() {
                                for (;;) {
                                    if (finalQ.reserved() || !finalQ.reserve())
                                        return;

                                    try {
                                        // TODO need to limit max chunks count processed at a time.
                                        for (BufferReadSubChunk t = finalQ.poll(); t != null; t = finalQ.poll())
                                            processSubChunk(finalQ.session(), t);
                                    }
                                    catch (IOException | IgniteCheckedException e) {
                                        // Print stack trace only if has runtime exception in it's cause.
                                        if (X.hasCause(e, IOException.class))
                                            U.warn(log, "Closing NIO session because of unhandled exception " +
                                                "[cls=" + e.getClass() +
                                                ", msg=" + e.getMessage() + ']');
                                        else
                                            U.error(log, "Closing NIO session because of unhandled exception.", e);

                                        finalQ.session().close();
                                    }
                                    finally {
                                        finalQ.release();
                                    }

                                    if (finalQ.isEmpty())
                                        break;
                                }

//                            }
//                        });
                }
            }
        }
        catch (IOException e) {
            throw new GridNioException(e);
        }
    }

    private void processSubChunk(
        GridNioSession ses,
        BufferReadSubChunk subChunk
    ) throws IOException, IgniteCheckedException {
        try {
            Object res = parser.decode(
                ses,
                subChunk.buffer());

            assert !subChunk.buffer().hasRemaining();

            if (res != null)
                proceedMessageReceived(
                    ses,
                    res);
        }
        finally {
            subChunk.release();
        }
    }

    /** {@inheritDoc} */
    @Override public GridNioFuture<Boolean> onSessionClose(GridNioSession ses) throws IgniteCheckedException {
        return proceedSessionClose(ses);
    }

    /** {@inheritDoc} */
    @Override public void onSessionIdleTimeout(GridNioSession ses) throws IgniteCheckedException {
        proceedSessionIdleTimeout(ses);
    }

    /** {@inheritDoc} */
    @Override public void onSessionWriteTimeout(GridNioSession ses) throws IgniteCheckedException {
        proceedSessionWriteTimeout(ses);
    }
}
