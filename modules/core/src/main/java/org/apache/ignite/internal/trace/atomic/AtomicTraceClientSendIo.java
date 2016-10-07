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

/**
 * Client send message processing result.
 */
public class AtomicTraceClientSendIo implements Binarylizable {
    /** */
    public long started;

    /** */
    public long marshalled;

    /** */
    public long sent;

    /**
     * Default constructor.
     */
    public AtomicTraceClientSendIo() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param started Start time.
     * @param marshalled Marshal time.
     * @param sent Send time
     */
    public AtomicTraceClientSendIo(long started, long marshalled, long sent) {
        this.started = started;
        this.marshalled = marshalled;
        this.sent = sent;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeLong(started);
        rawWriter.writeLong(marshalled);
        rawWriter.writeLong(sent);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        started = rawReader.readLong();
        marshalled = rawReader.readLong();
        sent = rawReader.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AtomicTraceClientSendIo.class, this);
    }
}
