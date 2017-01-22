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

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.internal.managers.discovery.DiscoveryCustomMessage;
import org.apache.ignite.lang.IgniteUuid;
import org.jetbrains.annotations.Nullable;

/**
 * Message indicating that a snapshot has been started.
 */
public class StartSnapshotOperationAckDiscoveryMessage implements DiscoveryCustomMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** Snapshot operation. */
    private final SnapshotOperation snapshotOperation;

    /** Custom message ID. */
    private IgniteUuid id = IgniteUuid.randomUuid();

    /** */
    private Exception err;

    /** */
    private UUID initiatorNodeId;

    /**
     * @param err Error.
     */
    public StartSnapshotOperationAckDiscoveryMessage(
            SnapshotOperation snapshotOperation,
            Exception err,
            UUID initiatorNodeId
    ) {
        this.snapshotOperation = snapshotOperation;
        this.err = err;
        this.initiatorNodeId = initiatorNodeId;
    }

    /** {@inheritDoc} */
    @Override public IgniteUuid id() {
        return id;
    }

    /**
     *
     */
    public SnapshotOperation snapshotOperation() {
        return snapshotOperation;
    }

    /**
     * @return Initiator node id.
     */
    public UUID initiatorNodeId() {
        return initiatorNodeId;
    }

    /**
     * @return Error if start this process is not successfully.
     */
    public Exception error() {
        return err;
    }

    /**
     * @return {@code True} if message has error otherwise {@code false}.
     */
    public boolean hasError() {
        return err != null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public DiscoveryCustomMessage ackMessage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean isMutable() {
        return false;
    }
}
