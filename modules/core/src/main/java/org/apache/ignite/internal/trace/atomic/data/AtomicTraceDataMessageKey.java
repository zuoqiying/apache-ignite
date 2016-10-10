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
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.io.Serializable;
import java.util.UUID;

/**
 * Trace message key.
 */
public class AtomicTraceDataMessageKey implements Serializable, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Source node ID. */
    public UUID fromNodeId;

    /** Destination node DI. */
    public UUID toNodeId;

    /** Message ID. */
    public long msgId;

    /**
     * Default constructor.
     */
    public AtomicTraceDataMessageKey() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param fromNodeId Source node ID.
     * @param toNodeId Destination node ID.
     * @param msgId Message ID.
     */
    public AtomicTraceDataMessageKey(UUID fromNodeId, UUID toNodeId, long msgId) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.msgId = msgId;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return 31 * (31 * fromNodeId.hashCode() + toNodeId.hashCode()) + (int)msgId;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        return obj != null &&
            obj instanceof AtomicTraceDataMessageKey &&
            F.eq(((AtomicTraceDataMessageKey)obj).fromNodeId, fromNodeId) &&
            F.eq(((AtomicTraceDataMessageKey)obj).toNodeId, toNodeId) &&
            F.eq(((AtomicTraceDataMessageKey)obj).msgId, msgId);
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeUuid(fromNodeId);
        rawWriter.writeUuid(toNodeId);
        rawWriter.writeLong(msgId);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        fromNodeId = rawReader.readUuid();
        toNodeId = rawReader.readUuid();
        msgId = rawReader.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AtomicTraceDataMessageKey.class, this);
    }
}
