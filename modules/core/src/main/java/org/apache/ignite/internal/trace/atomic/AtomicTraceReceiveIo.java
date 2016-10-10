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

import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryRawReader;
import org.apache.ignite.binary.BinaryRawWriter;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.util.UUID;

/**
 * Trace for received message.
 */
public class AtomicTraceReceiveIo implements Binarylizable {
    /** Data length. */
    public int dataLen;

    /** Node ID. */
    public UUID nodeId;

    /** Message ID. */
    public long msgId;

    /** Read start time. */
    public long started;

    /** Unmarshal time. */
    public long unmarshalled;

    /** Offer time. */
    public long offered;

    /**
     * Default constructor.
     */
    public AtomicTraceReceiveIo() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param dataLen Data length.
     * @param nodeId Node ID.
     * @param msgId Message ID.
     * @param started Started.
     * @param unmarshalled Unmarshalled.
     * @param offered Offered.
     */
    public AtomicTraceReceiveIo(int dataLen, UUID nodeId, long msgId, long started, long unmarshalled,
        long offered) {
        this.dataLen = dataLen;
        this.nodeId = nodeId;
        this.msgId = msgId;
        this.started = started;
        this.unmarshalled = unmarshalled;
        this.offered = offered;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeInt(dataLen);
        rawWriter.writeUuid(nodeId);
        rawWriter.writeLong(msgId);
        rawWriter.writeLong(started);
        rawWriter.writeLong(unmarshalled);
        rawWriter.writeLong(offered);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        dataLen = rawReader.readInt();
        nodeId = rawReader.readUuid();
        msgId = rawReader.readLong();
        started = rawReader.readLong();
        unmarshalled = rawReader.readLong();
        offered = rawReader.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AtomicTraceReceiveIo.class, this);
    }
}
