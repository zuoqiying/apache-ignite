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
 *
 */
package org.apache.ignite.internal.pagemem.snapshot;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * Description and parameters of snapshot operation
 */
public class SnapshotOperation implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final SnapshotOperationType type;

    /**
     * Snapshot ID (the timestamp of snapshot creation).
     */
    private final long snapshotId;

    /** */
    private final Set<String> cacheNames;

    /** Message. */
    private final String msg;

    /** Additional parameter. */
    private final Object extraParam;

    /** Optional list of dependent snapshot IDs. */
    private final Set<Long> dependentSnapshotIds;

    /**
     * @param type Type.
     * @param snapshotId Snapshot id.
     * @param cacheNames Cache names.
     * @param msg Message.
     * @param extraParam Additional parameter.
     * @param dependentSnapshotIds Optional list of dependent snapshot IDs.
     */
    public SnapshotOperation(SnapshotOperationType type, long snapshotId, Set<String> cacheNames, String msg,
        Object extraParam, Set<Long> dependentSnapshotIds) {
        this.type = type;
        this.snapshotId = snapshotId;
        this.cacheNames = cacheNames;
        this.msg = msg;
        this.extraParam = extraParam;
        this.dependentSnapshotIds = dependentSnapshotIds;
    }

    /**
     *
     */
    public SnapshotOperationType type() {
        return type;
    }

    /**
     * Snapshot ID (the timestamp of snapshot creation).
     *
     * @return Snapshot ID.
     */
    public long id() {
        return snapshotId;
    }

    /**
     * Cache names included to this snapshot.
     *
     * @return Cache names.
     */
    public Set<String> cacheNames() {
        return cacheNames;
    }

    /**
     * Additional info which was provided by client
     */
    public String message() {
        return msg;
    }

    /**
     *
     */
    public Object extraParameter() {
        return extraParam;
    }

    /**
     * @return Optional dependent snapshot IDs.
     */
    public Set<Long> dependentSnapshotIds() {
        return dependentSnapshotIds;
    }

    /**
     * @param op Op.
     */
    public static Collection<File> getOptionalPathsParameter(SnapshotOperation op) {
        assert (op.type() == SnapshotOperationType.CHECK ||
                op.type() == SnapshotOperationType.RESTORE ||
                op.type() == SnapshotOperationType.RESTORE_2_PHASE)
            && (op.extraParameter() == null || op.extraParameter() instanceof Collection);

        return (Collection<File>)op.extraParameter();
    }

    /**
     * @param op Op.
     */
    public static Boolean getFullSnapshotParameter(SnapshotOperation op) {
        assert op.type() == SnapshotOperationType.CREATE && op.extraParameter() instanceof Boolean;

        return (Boolean)op.extraParameter();
    }

    /**
     * @param op Op.
     */
    public static File getMovingPathParameter(SnapshotOperation op) {
        assert op.type() == SnapshotOperationType.MOVE && op.extraParameter() instanceof File;

        return (File)op.extraParameter();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        SnapshotOperation operation = (SnapshotOperation)o;

        if (snapshotId != operation.snapshotId)
            return false;
        if (type != operation.type)
            return false;
        if (msg != null ? !msg.equals(operation.msg) : operation.msg != null)
            return false;
        return extraParam != null ? extraParam.equals(operation.extraParam) : operation.extraParam == null;

    }

    @Override public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (int)(snapshotId ^ (snapshotId >>> 32));
        result = 31 * result + (msg != null ? msg.hashCode() : 0);
        result = 31 * result + (extraParam != null ? extraParam.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "SnapshotOperation{" +
            "type=" + type +
            ", snapshotId=" + snapshotId +
            ", cacheNames=" + cacheNames +
            ", msg='" + msg + '\'' +
            ", extraParam=" + extraParam +
            ", dependentSnapshotIds=" + dependentSnapshotIds +
            '}';
    }
}
