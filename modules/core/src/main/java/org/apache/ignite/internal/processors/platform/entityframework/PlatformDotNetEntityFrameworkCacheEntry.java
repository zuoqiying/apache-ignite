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

package org.apache.ignite.internal.processors.platform.entityframework;

import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryRawReader;
import org.apache.ignite.binary.BinaryRawWriter;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;

import java.util.HashMap;
import java.util.Map;

/**
 * EntityFramework cache entry.
 */
public class PlatformDotNetEntityFrameworkCacheEntry implements Binarylizable {
    /** Map from entity set name to version. */
    private Map<String, Long> entitySets;

    /** Cached data bytes. */
    private byte[] data;

    /**
     * @return Dependent entity sets with versions.
     */
    public Map<String, Long> entitySets() {
        return entitySets;
    }

    /**
     * @return Cached data bytes.
     */
    public byte[] data() {
        return data;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        writeBinary(writer.rawWriter());
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        readBinary(reader.rawReader());
    }

    /**
     * Reads from a binary reader.
     *
     * @param reader Raw reader.
     */
    public void readBinary(BinaryRawReader reader) {
        int cnt = reader.readInt();

        if (cnt >= 0) {
            entitySets = new HashMap<>(cnt);

            for (int i = 0; i < cnt; i++)
                entitySets.put(reader.readString(), reader.readLong());
        }
        else
            entitySets = null;

        data = reader.readByteArray();
    }

    /**
     * Writes to a binary writer.
     *
     * @param writer Raw writer.
     */
    private void writeBinary(BinaryRawWriter writer) {
        if (entitySets != null) {
            writer.writeInt(entitySets.size());

            for (Map.Entry<String, Long> e : entitySets.entrySet()) {
                writer.writeString(e.getKey());
                writer.writeLong(e.getValue());
            }
        }
        else
            writer.writeInt(-1);

        writer.writeByteArray(data);
    }
}
