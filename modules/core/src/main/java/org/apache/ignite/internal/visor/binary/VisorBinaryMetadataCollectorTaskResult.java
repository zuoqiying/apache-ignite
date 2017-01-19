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

package org.apache.ignite.internal.visor.binary;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Result for {@link VisorBinaryMetadataCollectorTask}
 */
@SuppressWarnings("PublicInnerClass")
public class VisorBinaryMetadataCollectorTaskResult extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Last binary metadata update date. */
    private Long lastUpdate;

    /** Remote data center IDs for which full state transfer was requested. */
    private Collection<VisorBinaryMetadata> binary;

    /**
     * Default constructor.
     */
    public VisorBinaryMetadataCollectorTaskResult() {
        // No-op.
    }

    /**
     * @param lastUpdate Last binary metadata update date.
     * @param binary Remote data center IDs for which full state transfer was requested.
     */
    public VisorBinaryMetadataCollectorTaskResult(Long lastUpdate, Collection<VisorBinaryMetadata> binary) {
        this.lastUpdate = lastUpdate;
        this.binary = binary;
    }

    /**
     * @return Last binary metadata update date.
     */
    public Long getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @return Remote data center IDs for which full state transfer was requested.
     */
    public Collection<VisorBinaryMetadata> getBinary() {
        return binary;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeLong(lastUpdate);
        U.writeCollection(out, binary);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        lastUpdate = in.readLong();
        binary = U.readCollection(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorBinaryMetadataCollectorTaskResult.class, this);
    }
}
