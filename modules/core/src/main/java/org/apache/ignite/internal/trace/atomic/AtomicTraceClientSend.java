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

/**
 * Part of client processing.
 */
public class AtomicTraceClientSend implements Binarylizable {
    /** Future hash. */
    public int futHash;

    /** Request hash. */
    public int reqHash;

    /** Start time. */
    public long start;

    /** Duration between future creation and passing request to IO. */
    public long futDur;

    /** Send duration. */
    public long sendDur;

    /**
     * Default constructor.
     */
    public AtomicTraceClientSend() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param futHash Future hash.
     * @param reqHash Request hash.
     * @param start Start time.
     * @param futDur Duration between future creation and passing request to IO.
     * @param sendDur Send duration.
     */
    public AtomicTraceClientSend(int futHash, int reqHash, long start, long futDur, long sendDur) {
        this.futHash = futHash;
        this.reqHash = reqHash;
        this.start = start;
        this.futDur = futDur;
        this.sendDur = sendDur;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeInt(futHash);
        rawWriter.writeInt(reqHash);
        rawWriter.writeLong(start);
        rawWriter.writeLong(futDur);
        rawWriter.writeLong(sendDur);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        futHash = rawReader.readInt();
        reqHash = rawReader.readInt();
        start = rawReader.readLong();
        futDur = rawReader.readLong();
        sendDur = rawReader.readLong();
    }
}
