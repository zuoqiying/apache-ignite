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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Trace node-wide result.
 */
public class TraceNodeResult implements Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Node ID. */
    private UUID nodeId;

    /** Groups data. */
    private Map<String, TraceThreadGroupResult> grps;

    /**
     * Default constructor.
     */
    public TraceNodeResult() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param nodeId Node ID.
     * @param grps Groups.
     */
    public TraceNodeResult(UUID nodeId, Map<String, TraceThreadGroupResult> grps) {
        this.nodeId = nodeId;
        this.grps = grps;
    }

    /**
     * @return Node ID.
     */
    public UUID nodeId() {
        return nodeId;
    }

    /**
     * @return Groups data.
     */
    public Map<String, TraceThreadGroupResult> groups() {
        return grps;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeUuid(nodeId);

        rawWriter.writeInt(grps.size());

        for (TraceThreadGroupResult grp : grps.values()) {
            rawWriter.writeObject(grp);
        }
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        nodeId = rawReader.readUuid();

        int size = rawReader.readInt();

        grps = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            TraceThreadGroupResult grp = rawReader.readObject();

            assert grp != null;

            grps.put(grp.groupName(), grp);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(TraceNodeResult.class, this);
    }
}
