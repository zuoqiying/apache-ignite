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

package org.apache.ignite.internal.trace.atomic.data;

import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryRawReader;
import org.apache.ignite.binary.BinaryRawWriter;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.io.Serializable;

/**
 * Client send message processing result.
 */
public class AtomicTraceDataSendIo implements Serializable, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    public long polled;

    /** */
    public long marshalled;

    /** */
    public long sent;

    /** */
    public int bufLen;

    /** */
    public int msgCnt;

    /**
     * Default constructor.
     */
    public AtomicTraceDataSendIo() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param polled Poll time.
     * @param marshalled Marshal time.
     * @param sent Send time
     * @param bufLen Buffer length.
     * @param msgCnt Message count.
     */
    public AtomicTraceDataSendIo(long polled, long marshalled, long sent, int bufLen, int msgCnt) {
        this.polled = polled;
        this.marshalled = marshalled;
        this.sent = sent;
        this.bufLen = bufLen;
        this.msgCnt = msgCnt;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeLong(polled);
        rawWriter.writeLong(marshalled);
        rawWriter.writeLong(sent);
        rawWriter.writeInt(bufLen);
        rawWriter.writeInt(msgCnt);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        polled = rawReader.readLong();
        marshalled = rawReader.readLong();
        sent = rawReader.readLong();
        bufLen = rawReader.readInt();
        msgCnt = rawReader.readInt();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AtomicTraceDataSendIo.class, this);
    }
}
