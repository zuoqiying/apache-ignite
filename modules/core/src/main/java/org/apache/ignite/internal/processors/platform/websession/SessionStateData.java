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

package org.apache.ignite.internal.processors.platform.websession;

import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryRawReader;
import org.apache.ignite.binary.BinaryRawWriter;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Web session state data.
 */
@SuppressWarnings({"ReturnOfDateField", "AssignmentToDateFieldFromParameter"})
public class SessionStateData implements Binarylizable {
    /** Items. */
    private KeyValueDirtyTrackedCollection items;

    /** Static objects. */
    @GridToStringExclude
    private byte[] staticObjects;

    /** Timeout. */
    private int timeout;

    /** Lock ID. */
    private long lockId;

    /** Lock node ID. */
    private UUID lockNodeId;

    /** Lock time. */
    private Timestamp lockTime;

    /**
     * @return Lock ID.
     */
    public long lockId() {
        return lockId;
    }

    /**
     * @return Lock node ID.
     */
    public UUID lockNodeId() {
        return lockNodeId;
    }

    /**
     * @return Lock time.
     */
    public Timestamp lockTime() {
        return lockTime;
    }

    /**
     * @return {@code True} if locked.
     */
    public boolean isLocked() {
        return lockTime != null;
    }

    /**
     * Set lock info.
     *
     * @param lock Lock.
     */
    public SessionStateData lock(SessionStateLockInfo lock) {
        assert !isLocked();

        SessionStateData res = getCopy();

        res.lockId = lock.id();
        res.lockNodeId = lock.nodeId();
        res.lockTime = lock.time();

        return res;
    }

    /**
     * Clear lock info.
     */
    public SessionStateData unlock() {
        assert isLocked();

        return getCopy();
    }

    /**
     * Apply changes from another instance.
     *
     * @param other Data.
     */
    public void applyChanges(SessionStateData other) {
        assert other != null;
        assert items != null;

        timeout = other.timeout;
        staticObjects = other.staticObjects;
        items.applyChanges(other.items);
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter raw = writer.rawWriter();

        raw.writeInt(timeout);
        raw.writeUuid(lockNodeId);
        raw.writeLong(lockId);
        raw.writeTimestamp(lockTime);
        raw.writeObject(items);
        raw.writeByteArray(staticObjects);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader raw = reader.rawReader();

        timeout = raw.readInt();
        lockNodeId = raw.readUuid();
        lockId = raw.readLong();
        lockTime = raw.readTimestamp();
        items = raw.readObject();
        staticObjects = raw.readByteArray();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(SessionStateData.class, this);
    }

    /**
     * Gets a copy of this instance with non-lock properties set.
     */
    private SessionStateData getCopy() {
        SessionStateData res = new SessionStateData();

        res.staticObjects = staticObjects;
        res.items = items;
        res.timeout = timeout;
        return res;
    }
}
