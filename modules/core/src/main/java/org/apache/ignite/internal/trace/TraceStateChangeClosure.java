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
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;

/**
 * Trace state change closure.
 */
public class TraceStateChangeClosure implements IgniteCallable<Void>, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Ignite instance */
    @IgniteInstanceResource
    private Ignite ignite;

    /** Enable flag. */
    private boolean enable;

    /**
     * Default constructor.
     */
    public TraceStateChangeClosure() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param enable Enable flag.
     */
    public TraceStateChangeClosure(boolean enable) {
        this.enable = enable;
    }

    /** {@inheritDoc} */
    @Override public Void call() throws Exception {
        TraceProcessor proc = TraceProcessor.shared();

        if (enable)
            proc.enable();
        else
            proc.disable();

        return null;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        writer.rawWriter().writeBoolean(enable);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        enable = reader.rawReader().readBoolean();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(TraceStateChangeClosure.class, this);
    }
}
