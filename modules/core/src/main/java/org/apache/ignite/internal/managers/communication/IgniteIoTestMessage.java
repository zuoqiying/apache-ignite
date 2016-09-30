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

import org.apache.ignite.internal.GridDirectTransient;
import org.apache.ignite.internal.util.GridUnsafe;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;

import java.nio.ByteBuffer;

/**
 *
 */
public class IgniteIoTestMessage implements Message {
    /** */
    private static byte FLAG_PROC_FROM_NIO = 1;

    /** */
    private static final long serialVersionUID = 0L;

    /** */
    @GridDirectTransient
    private long id;

    /** */
    @GridDirectTransient
    private byte flags;

    /** */
    @GridDirectTransient
    private boolean req;

    /** */
    @GridDirectTransient
    private byte payload[];

    private byte[] data;

    /**
     *
     */
    public IgniteIoTestMessage() {
        // No-op.
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

        data = new byte[10];

        GridUnsafe.putByte(data, 0, (byte)0);
        GridUnsafe.putBoolean(data, 1, req);
        GridUnsafe.putLong(data, 2, id);
    }

    /**
     * @return Process from NIO thread flag.
     */
    public boolean processFromNioThread() {
        return false;//isFlag(FLAG_PROC_FROM_NIO);
    }

    /**
     * @param procFromNioThread Process from NIO thread flag.
     */
    public void processFromNioThread(boolean procFromNioThread) {
        //setFlag(procFromNioThread, FLAG_PROC_FROM_NIO);
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
        return GridUnsafe.getBoolean(data, 1);
        //return req;
    }

    /**
     * @return ID.
     */
    public long id() {
        return GridUnsafe.getLong(data, 2);
        //return id;
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 126;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 4;
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
