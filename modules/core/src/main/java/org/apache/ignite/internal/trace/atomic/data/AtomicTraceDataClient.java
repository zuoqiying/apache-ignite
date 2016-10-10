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
 * Trace for client processing.
 */
public class AtomicTraceDataClient implements Serializable, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Future ID. */
    public UUID futId;

    /** Request ID. */
    public long reqId;

    /** Response ID. */
    public long respId;

    /** Start time. */
    public long started;

    /** Time when mini future was completed. */
    public long completed;

    /** Finish time. */
    public long finished;

    /**
     * Default constructor.
     */
    public AtomicTraceDataClient() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param respId Request ID.
     * @param started Start time.
     */
    public AtomicTraceDataClient(long respId, long started) {
        this.respId = respId;
        this.started = started;
    }

    /**
     * Invoked when mini-future is completed.
     *
     * @param futId Future ID.
     * @param reqId Request ID.
     * @param completed Complete time.
     */
    public void onMiniFutureCompleted(UUID futId, long reqId, long completed) {
        this.futId = futId;
        this.reqId = reqId;
        this.completed = completed;
    }

    /**
     * Invoked when processing is finished.
     *
     * @param finished Finish time.
     */
    public void onFinished(long finished) {
        this.finished = finished;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeUuid(futId);
        rawWriter.writeLong(reqId);
        rawWriter.writeLong(respId);
        rawWriter.writeLong(started);
        rawWriter.writeLong(completed);
        rawWriter.writeLong(finished);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        futId = rawReader.readUuid();
        reqId = rawReader.readLong();
        respId = rawReader.readLong();
        started = rawReader.readLong();
        completed = rawReader.readLong();
        finished = rawReader.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AtomicTraceDataClient.class, this);
    }
}
