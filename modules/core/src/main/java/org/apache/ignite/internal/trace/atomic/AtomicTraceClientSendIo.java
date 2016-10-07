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
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;

import java.util.Map;

/**
 * Client send IO data.
 */
public class AtomicTraceClientSendIo implements Binarylizable {
    /** Messages. */
    public Map<Integer, AtomicTraceClientSendIoMessage> msgs;

    /**
     * Default constructor.
     */
    public AtomicTraceClientSendIo() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param msgs Messages.
     */
    public AtomicTraceClientSendIo(Map<Integer, AtomicTraceClientSendIoMessage> msgs) {
        this.msgs = msgs;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        writer.rawWriter().writeMap(msgs);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        msgs = reader.rawReader().readMap();
    }
}
