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

package org.apache.ignite.internal.processors.cache.mvcc;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.internal.GridDirectMap;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageCollectionItemType;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;

/**
 *
 */
public class CoordinatorTxAckRequest implements Message {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private static final int SKIP_RESPONSE_FLAG_MASK = 0x01;

    /** */
    private long futId;

    /** */
    private GridCacheVersion txId;

    /** */
    private long topVer;

    /** */
    @GridDirectMap(keyType = UUID.class, valueType = Long.class)
    private Map<UUID, Long> cntrs;

    /** */
    private byte flags;

    /**
     *
     */
    public CoordinatorTxAckRequest() {
        // No-op.
    }

    /**
     * @param futId Future ID.
     * @param txId Transaction ID.
     * @param topVer Transaction topology version.
     * @param cntrs Counters generated for transaction.
     */
    CoordinatorTxAckRequest(long futId, GridCacheVersion txId, long topVer, Map<UUID, Long> cntrs) {
        this.futId = futId;
        this.txId = txId;
        this.topVer = topVer;
        this.cntrs = cntrs;
    }

    /**
     * @return Future ID.
     */
    long futureId() {
        return futId;
    }

    /**
     * @return {@code True} if response message is not needed.
     */
    boolean skipResponse() {
        return (flags & SKIP_RESPONSE_FLAG_MASK) != 0;
    }

    /**
     * @param val {@code True} if response message is not needed.
     */
    void skipResponse(boolean val) {
        if (val)
            flags |= SKIP_RESPONSE_FLAG_MASK;
        else
            flags &= ~SKIP_RESPONSE_FLAG_MASK;
    }

    /**
     * @return Transaction ID.s
     */
    public GridCacheVersion txId() {
        return txId;
    }

    /**
     * @return Topology version.
     */
    public long topologyVersion() {
        return topVer;
    }

    /**
     * @return Counters.
     */
    Map<UUID, Long> coordinatorCounters() {
        return cntrs;
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 0:
                if (!writer.writeMap("cntrs", cntrs, MessageCollectionItemType.UUID, MessageCollectionItemType.LONG))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeByte("flags", flags))
                    return false;

                writer.incrementState();

            case 2:
                if (!writer.writeLong("futId", futId))
                    return false;

                writer.incrementState();

            case 3:
                if (!writer.writeLong("topVer", topVer))
                    return false;

                writer.incrementState();

            case 4:
                if (!writer.writeMessage("txId", txId))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        if (!reader.beforeMessageRead())
            return false;

        switch (reader.state()) {
            case 0:
                cntrs = reader.readMap("cntrs", MessageCollectionItemType.UUID, MessageCollectionItemType.LONG, false);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                flags = reader.readByte("flags");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 2:
                futId = reader.readLong("futId");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 3:
                topVer = reader.readLong("topVer");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 4:
                txId = reader.readMessage("txId");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(CoordinatorTxAckRequest.class);
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return -30;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 5;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(CoordinatorTxAckRequest.class, this);
    }
}
