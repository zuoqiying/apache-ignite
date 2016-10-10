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
import java.util.UUID;

/**
 * Atomic trace server data.
 */
public class AtomicTraceDataServer implements Serializable, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Start time. */
    public long started;

    /** Offer time. */
    public long offered;

    /** Source node ID. */
    public UUID fromNode;

    /** Destination node ID. */
    public UUID toNode;

    /** Request ID. */
    public long reqId;

    /** Response ID. */
    public long respId;

    /**
     * Default constructor.
     */
    public AtomicTraceDataServer() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param fromNode From node ID.
     * @param toNode To node ID.
     * @param reqId Request ID.
     * @param started Start time.
     */
    public AtomicTraceDataServer(UUID fromNode, UUID toNode, long reqId, long started) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.reqId = reqId;
        this.started = started;
    }

    /**
     * Invoked on offer.
     *
     * @param respId Response ID.
     * @param offered Offer time.
     */
    public void onOffer(long respId, long offered) {
        this.respId = respId;
        this.offered = offered;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeLong(started);
        rawWriter.writeLong(offered);
        rawWriter.writeUuid(fromNode);
        rawWriter.writeUuid(toNode);
        rawWriter.writeLong(reqId);
        rawWriter.writeLong(respId);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        started = rawReader.readLong();
        offered = rawReader.readLong();
        fromNode = rawReader.readUuid();
        toNode = rawReader.readUuid();
        reqId = rawReader.readLong();
        respId = rawReader.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AtomicTraceDataServer.class, this);
    }
}
