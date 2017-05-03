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

package org.apache.ignite.internal;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.cache.GridCachePartitionExchangeManager;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtTopologyFuture;
import org.apache.ignite.internal.util.future.GridFinishedFuture;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 *
 */
public class IgniteDiagnosticMessage implements Message {
    /** */
    private static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override protected DateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss.SSS");
        }
    };

    /** */
    private long futId;

    /** */
    private String msg;

    /** */
    private byte[] cBytes;

    public IgniteDiagnosticMessage() {
        // No-op.
    }

    public static IgniteDiagnosticMessage createRequest(GridKernalContext ctx, IgniteClosure<GridKernalContext, String> c, long futId) throws IgniteCheckedException {
        byte[] cBytes = U.marshal(ctx.config().getMarshaller(), c);

        IgniteDiagnosticMessage msg = new IgniteDiagnosticMessage();

        msg.futId = futId;
        msg.cBytes = cBytes;

        return msg;
    }

    public static IgniteDiagnosticMessage createResponse(String msg0, long futId) {
        IgniteDiagnosticMessage msg = new IgniteDiagnosticMessage();

        msg.futId = futId;
        msg.msg = msg0;

        return msg;
    }

    public IgniteClosure<GridKernalContext, String> unmarshalClosure(GridKernalContext ctx) throws IgniteCheckedException {
        assert cBytes != null;

        return U.unmarshal(ctx, cBytes, null);
    }

    public long futureId() {
        return futId;
    }

    public boolean request() {
        return cBytes != null;
    }

    public String message() {
        return msg;
    }

    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 0:
                if (!writer.writeByteArray("cBytes", cBytes))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeLong("futId", futId))
                    return false;

                writer.incrementState();

            case 2:
                if (!writer.writeString("msg", msg))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        if (!reader.beforeMessageRead())
            return false;

        switch (reader.state()) {
            case 0:
                cBytes = reader.readByteArray("cBytes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                futId = reader.readLong("futId");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 2:
                msg = reader.readString("msg");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(IgniteDiagnosticMessage.class);
    }

    @Override public byte directType() {
        return -46;
    }

    @Override public byte fieldsCount() {
        return 3;
    }

    @Override public void onAckReceived() {

    }

    /**
     *
     */
    public static class BaseClosure implements IgniteClosure<GridKernalContext, String> {
        /** */
        protected final UUID nodeId;

        /**
         * @param ctx Local node context.
         */
        public BaseClosure(GridKernalContext ctx) {
            this.nodeId = ctx.localNodeId();
        }

        @Override public String apply(GridKernalContext ctx) {
            try {
                StringBuilder sb = new StringBuilder();

                IgniteInternalFuture<String> commInfo = dumpCommunicationInfo(ctx, nodeId);

                sb.append(dumpNodeBasicInfo(ctx));

                sb.append(U.nl()).append(dumpExchangeInfo(ctx));

                String moreInfo = dumpInfo(ctx);

                sb.append(U.nl()).append(commInfo.get());

                if (moreInfo != null)
                    sb.append(U.nl()).append(moreInfo);

                return sb.toString();
            }
            catch (Exception e) {
                ctx.cluster().diagnosticLog().error("Failed to execute diagnostic message closure: " + e, e);

                return "Failed to execute diagnostic message closure: " + e;
            }
        }

        protected String dumpInfo(GridKernalContext ctx) {
            return null;
        }
    }

    public static String dumpNodeBasicInfo(GridKernalContext ctx) {
        StringBuilder sb = new StringBuilder("General node info [id=").append(ctx.localNodeId());

        sb.append(", client=").append(ctx.clientNode());
        sb.append(", discoTopVer=").append(ctx.discovery().topologyVersionEx());
        sb.append(", time=").append(formatTime(U.currentTimeMillis()));

        sb.append(']');

        return sb.toString();
    }

    static String dumpExchangeInfo(GridKernalContext ctx) {
        GridCachePartitionExchangeManager exchMgr = ctx.cache().context().exchange();

        StringBuilder sb = new StringBuilder("Partitions exchange info [readyVer=").append(exchMgr.readyAffinityVersion());
        sb.append("]");

        GridDhtTopologyFuture fut = exchMgr.lastTopologyFuture();

        sb.append(U.nl()).append("Last initialized exchange future: ").append(fut);
        
        return sb.toString();
    }

    public static IgniteInternalFuture<String> dumpCommunicationInfo(GridKernalContext ctx, UUID nodeId) {
        if (ctx.config().getCommunicationSpi() instanceof TcpCommunicationSpi)
            return ((TcpCommunicationSpi) ctx.config().getCommunicationSpi()).dumpNodeStatistics(nodeId);
        else
            return new GridFinishedFuture<>("Unexpected communication SPI: " + ctx.config().getCommunicationSpi());
    }
    /**
     * @param time Time.
     * @return Time string.
     */
    private static String formatTime(long time) {
        return dateFormat.get().format(new Date(time));
    }
}
