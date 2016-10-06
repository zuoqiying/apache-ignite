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

import org.apache.ignite.Ignite;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryRawReader;
import org.apache.ignite.binary.BinaryRawWriter;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Trace collect closure.
 */
public class TraceCollectClosure implements IgniteCallable<Map<String, TraceThreadGroupResult>>, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Ignite instance */
    @IgniteInstanceResource
    private Ignite ignite;

    /** Groups to collect. */
    private Collection<String> grpNames;

    /** Whether to reset data. */
    private boolean reset;

    /**
     * Default constructor.
     */
    public TraceCollectClosure() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param grpNames Groups to collect.
     */
    public TraceCollectClosure(Collection<String> grpNames, boolean reset) {
        this.grpNames = grpNames;
        this.reset = reset;
    }

    /** {@inheritDoc} */
    @Override public Map<String, TraceThreadGroupResult> call() throws Exception {
        UUID nodeId = ignite.cluster().localNode().id();

        TraceProcessor proc = TraceProcessor.shared();

        Map<String, TraceThreadGroupResult> res = new HashMap<>();

        for (String grpName : grpNames) {
            TraceThreadGroup grp = proc.threadGroup(grpName);

            if (grp != null) {
                List<TraceThreadResult> threadRess = new ArrayList<>();

                for (TraceThreadData data : grp.threads().values()) {
                    Thread t = data.thread();

                    threadRess.add(new TraceThreadResult(nodeId, grpName, t.getName(), t.getId(), data.data()));

                    if (reset)
                        data.reset();
                }

                TraceThreadGroupResult grpRes = new TraceThreadGroupResult(nodeId, grpName, threadRess);

                res.put(grpName, grpRes);
            }
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeCollection(grpNames);
        rawWriter.writeBoolean(reset);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader rawReader = reader.rawReader();

        grpNames = rawReader.readCollection();
        reset = rawReader.readBoolean();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(TraceCollectClosure.class, this);
    }
}
