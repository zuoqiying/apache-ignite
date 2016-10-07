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

package org.apache.ignite.internal.trace;

import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryRawReader;
import org.apache.ignite.binary.BinaryRawWriter;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Trace thread group result.
 */
public class TraceThreadGroupResult implements Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Node ID. */
    private UUID nodeId;

    /** Group name. */
    private String grpName;

    /** Threads data. */
    @GridToStringInclude
    private List<TraceThreadResult> threads;

    /**
     * Default constructor.
     */
    public TraceThreadGroupResult() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param nodeId Node ID.
     * @param grpName Group name.
     * @param threads Threads data.
     */
    public TraceThreadGroupResult(UUID nodeId, String grpName, List<TraceThreadResult> threads) {
        assert threads != null;

        this.nodeId = nodeId;
        this.grpName = grpName;
        this.threads = threads;
    }

    /**
     * @return Node ID.
     */
    public UUID nodeId() {
        return nodeId;
    }

    /**
     * @return Group name.
     */
    public String groupName() {
        return grpName;
    }

    /**
     * @return Threads data.
     */
    public List<TraceThreadResult> threads() {
        return threads;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeUuid(nodeId);
        rawWriter.writeString(grpName);

        rawWriter.writeInt(threads.size());

        for (TraceThreadResult thread : threads)
            rawWriter.writeObject(thread);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        nodeId = rawReader.readUuid();
        grpName = rawReader.readString();

        int size = rawReader.readInt();

        threads = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
            threads.add((TraceThreadResult)rawReader.readObject());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(TraceThreadGroupResult.class, this);
    }
}
