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

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.internal.util.lang.GridMetadataAwareAdapter;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.jetbrains.annotations.Nullable;

/**
 * TODO
 */
public class DirectReaderQueue<T> {
    private final Queue<T> q = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean reserved = new AtomicBoolean();

    private final GridNioSession ses;

    MessageReader rdr;

    Message msg;

    public DirectReaderQueue(
        GridNioSession ses
    ) {
        this.ses = new SessionWrapper(ses);
    }

    public DirectReaderQueue() {
        this(null);
    }

    public void add(T t) {
        q.add(t);
    }

    public T poll() {
        return q.poll();
    }

    public boolean reserved() {
        return reserved.get();
    }

    public void release() {
        reserved.set(false);
    }

    public GridNioSession session() {
        return ses;
    }

    public boolean reserve() {
        return !reserved.get() && reserved.compareAndSet(false, true);
    }

    public boolean isEmpty() {
        return q.isEmpty();
    }

    public void reader(MessageReader rdr) {
        this.rdr = rdr;
    }

    public MessageReader reader() {
        return rdr;
    }

    public Message message() {
        return msg;
    }

    public void message(Message msg) {
        this.msg = msg;
    }

    private static class SessionWrapper extends GridMetadataAwareAdapter implements GridNioSession {
        /** */
        private final GridNioSession ses;

        private SessionWrapper(GridNioSession ses) {
            this.ses = ses;
        }

        @Nullable @Override public InetSocketAddress localAddress() {
            return ses.localAddress();
        }

        @Nullable @Override public InetSocketAddress remoteAddress() {
            return ses.remoteAddress();
        }

        @Override public long bytesSent() {
            return ses.bytesSent();
        }

        @Override public long bytesReceived() {
            return ses.bytesReceived();
        }

        @Override public long createTime() {
            return ses.createTime();
        }

        @Override public long closeTime() {
            return ses.closeTime();
        }

        @Override public long lastReceiveTime() {
            return ses.lastReceiveTime();
        }

        @Override public long lastSendTime() {
            return ses.lastSendTime();
        }

        @Override public long lastSendScheduleTime() {
            return ses.lastSendScheduleTime();
        }

        @Override public GridNioFuture<Boolean> close() {
            return ses.close();
        }

        @Override public GridNioFuture<?> send(Object msg) {
            return ses.send(msg);
        }

        @Override public boolean accepted() {
            return ses.accepted();
        }

        @Override public GridNioFuture<?> resumeReads() {
            return ses.resumeReads();
        }

        @Override public GridNioFuture<?> pauseReads() {
            return ses.pauseReads();
        }

        @Override public boolean readsPaused() {
            return ses.readsPaused();
        }

        @Override public void recoveryDescriptor(GridNioRecoveryDescriptor recoveryDesc) {
            throw new UnsupportedOperationException();
        }

        @Nullable @Override public GridNioRecoveryDescriptor recoveryDescriptor() {
            return ses.recoveryDescriptor();
        }

        @Nullable @Override public <V> V meta(int key) {
            if (key == TcpCommunicationSpi.NODE_ID_META)
                return ses.meta(TcpCommunicationSpi.NODE_ID_META);

            return super.meta(key);
        }
    }
}
