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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Begin part of client processing.
 */
public class AtomicTraceDataClient implements Serializable, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Future ID. */
    public UUID futId;

    /** Start time. */
    public long started;

    /** Map from request to offer time. */
    Map<AtomicTraceDataMessageKey, Long> reqs = new HashMap<>(2, 1.0f);

    /**
     * Default constructor.
     */
    public AtomicTraceDataClient() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param futId Future ID.
     */
    public AtomicTraceDataClient(UUID futId) {
        this.futId = futId;

        started = System.nanoTime();
    }

    /**
     * Add request trace.
     *
     * @param fromNodeId From node ID.
     * @param toNodeId To node ID.
     */
    public void addRequestTrace(UUID fromNodeId, UUID toNodeId, long msgId) {
        reqs.put(new AtomicTraceDataMessageKey(fromNodeId, toNodeId, msgId), System.nanoTime());
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeUuid(futId);
        rawWriter.writeLong(started);
        rawWriter.writeMap(reqs);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        futId = rawReader.readUuid();
        started = rawReader.readLong();
        reqs = rawReader.readMap();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AtomicTraceDataClient.class, this);
    }
}
