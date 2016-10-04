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

package org.apache.ignite.internal.managers.communication;

import java.nio.ByteBuffer;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;

/**
 *
 */
public class IgniteIoTestMessage implements Message {
    /** */
    private static byte FLAG_PROC_FROM_NIO = 1;

    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private long id;

    /** */
    private byte flags;

    /** */
    private boolean req;

    /** */
    private byte payload[];

    public long reqSndTime;

    public long reqWriteTime;

    public long reqReadTime;

    public long resSndTime;

    public long resWriteTime;

    public long resReadTime;

    public long resTime;

    /**
     *
     */
    public IgniteIoTestMessage() {
        // No-op.
    }

    public void sendTime() {
        if (req)
            reqSndTime = U.currentTimeMillis();
        else
            resSndTime = U.currentTimeMillis();
    }

    public void writeTime() {
        if (req)
            reqWriteTime = U.currentTimeMillis();
        else
            resWriteTime = U.currentTimeMillis();
    }

    public void readTime() {
        if (req)
            reqReadTime = U.currentTimeMillis();
        else
            resReadTime = U.currentTimeMillis();
    }

    public void responseTime() {
        assert !req;

        resTime = U.currentTimeMillis();
    }

    /**
     * @param id ID.
     * @param req {@code True} for request.
     * @param payload Payload.
     */
    public IgniteIoTestMessage(long id, boolean req, byte[] payload) {
        this.id = id;
        this.req = req;
        this.payload = payload;
    }

    public static IgniteIoTestMessage createResponse(IgniteIoTestMessage req) {
        IgniteIoTestMessage msg = new IgniteIoTestMessage(req.id(), false, null);

        msg.flags = req.flags;
        msg.reqSndTime = req.reqSndTime;
        msg.reqWriteTime = req.reqWriteTime;
        msg.reqReadTime = req.reqReadTime;
        msg.resSndTime = req.resSndTime;
        msg.resWriteTime = req.resWriteTime;

        return msg;
    }

    /**
     * @return Process from NIO thread flag.
     */
    public boolean processFromNioThread() {
        return isFlag(FLAG_PROC_FROM_NIO);
    }

    /**
     * @param procFromNioThread Process from NIO thread flag.
     */
    public void processFromNioThread(boolean procFromNioThread) {
        setFlag(procFromNioThread, FLAG_PROC_FROM_NIO);
    }

    /**
     * @param flags Flags.
     */
    public void flags(byte flags) {
        this.flags = flags;
    }

    /**
     * @return Flags.
     */
    public byte flags() {
        return flags;
    }

    /**
     * Sets flag mask.
     *
     * @param flag Set or clear.
     * @param mask Mask.
     */
    private void setFlag(boolean flag, int mask) {
        flags = flag ? (byte)(flags | mask) : (byte)(flags & ~mask);
    }

    /**
     * Reads flag mask.
     *
     * @param mask Mask to read.
     * @return Flag value.
     */
    private boolean isFlag(int mask) {
        return (flags & mask) != 0;
    }

    /**
     * @return {@code True} for request.
     */
    public boolean request() {
        return req;
    }

    /**
     * @return ID.
     */
    public long id() {
        return id;
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 0:
                if (!writer.writeByte("flags", flags))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeLong("id", id))
                    return false;

                writer.incrementState();

            case 2:
                if (!writer.writeByteArray("payload", payload))
                    return false;

                writer.incrementState();

            case 3:
                if (!writer.writeBoolean("req", req))
                    return false;

                writer.incrementState();

            case 4:
                if (!writer.writeLong("reqReadTime", reqReadTime))
                    return false;

                writer.incrementState();

            case 5:
                if (!writer.writeLong("reqSndTime", reqSndTime))
                    return false;

                writer.incrementState();

            case 6:
                if (!writer.writeLong("reqWriteTime", reqWriteTime))
                    return false;

                writer.incrementState();

            case 7:
                if (!writer.writeLong("resReadTime", resReadTime))
                    return false;

                writer.incrementState();

            case 8:
                if (!writer.writeLong("resSndTime", resSndTime))
                    return false;

                writer.incrementState();

            case 9:
                if (!writer.writeLong("resTime", resTime))
                    return false;

                writer.incrementState();

            case 10:
                if (!writer.writeLong("resWriteTime", resWriteTime))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        if (!reader.beforeMessageRead())
            return false;

        switch (reader.state()) {
            case 0:
                flags = reader.readByte("flags");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                id = reader.readLong("id");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 2:
                payload = reader.readByteArray("payload");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 3:
                req = reader.readBoolean("req");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 4:
                reqReadTime = reader.readLong("reqReadTime");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 5:
                reqSndTime = reader.readLong("reqSndTime");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 6:
                reqWriteTime = reader.readLong("reqWriteTime");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 7:
                resReadTime = reader.readLong("resReadTime");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 8:
                resSndTime = reader.readLong("resSndTime");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 9:
                resTime = reader.readLong("resTime");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 10:
                resWriteTime = reader.readLong("resWriteTime");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(IgniteIoTestMessage.class);
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 126;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 11;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(IgniteIoTestMessage.class, this);
    }
}
