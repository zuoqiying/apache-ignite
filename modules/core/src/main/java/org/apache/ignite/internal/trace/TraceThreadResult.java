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
import org.apache.ignite.internal.util.typedef.internal.S;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Trace thread result.
 */
public class TraceThreadResult implements Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Node ID. */
    private UUID nodeId;

    /** Group name. */
    private String grpName;

    /** Thread name. */
    private String threadName;

    /** Thread ID. */
    private long threadId;

    /** Data. */
    private List<Object> data;

    /**
     * Default constructor.
     */
    public TraceThreadResult() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param nodeId Node ID.
     * @param grpName Group name.
     * @param threadName Thread name.
     * @param threadId Thread ID.
     * @param data Data.
     */
    public TraceThreadResult(UUID nodeId, String grpName, String threadName, long threadId, List<Object> data) {
        assert data != null;

        this.nodeId = nodeId;
        this.grpName = grpName;
        this.threadName = threadName;
        this.threadId = threadId;
        this.data = data;
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
     * @return Thread name.
     */
    public String threadName() {
        return threadName;
    }

    /**
     * @return Thread ID.
     */
    public long threadId() {
        return threadId;
    }

    /**
     * @return Data.
     */
    public List<Object> data() {
        return data;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeUuid(nodeId);
        rawWriter.writeString(grpName);
        rawWriter.writeString(threadName);
        rawWriter.writeLong(threadId);

        rawWriter.writeInt(data.size());

        for (Object entry : data)
            rawWriter.writeObject(entry);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        nodeId = rawReader.readUuid();
        grpName = rawReader.readString();
        threadName = rawReader.readString();
        threadId = rawReader.readLong();

        int size = rawReader.readInt();

        data = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
            data.add(rawReader.readObject());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(TraceThreadResult.class, this);
    }
}
