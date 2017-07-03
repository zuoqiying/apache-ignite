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

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 *
 */
public class CacheObjectImpl extends CacheObjectAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private boolean cacheHit = true;

    /**
     *
     */
    public CacheObjectImpl() {
        // No-op.
    }

    /**
     * @param val Value.
     * @param valBytes Value bytes.
     */
    public CacheObjectImpl(Object val, byte[] valBytes) {
        assert val != null || valBytes != null;

        this.val = val;
        this.valBytes = valBytes;
    }

    /**
     * @param cp Value.
     */
    public CacheObjectImpl(CacheObjectImpl cp) {
        assert cp.val != null || cp.valBytes != null;

        this.val = cp.val;
        this.valBytes = cp.valBytes;
        this.cacheHit = cp.cacheHit;
    }

    /** {@inheritDoc} */
    @Override public boolean isPlatformType() {
        return true;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Nullable @Override public <T> T value(CacheObjectContext ctx, boolean cpy) {
        cpy = cpy && needCopy(ctx);

        try {
            if (cpy) {
                if (valBytes == null) {
                    assert val != null;

                    valBytes = ctx.processor().marshal(ctx, val);
                }

                return (T)ctx.processor().unmarshal(ctx, valBytes,
                    val == null ? ctx.kernalContext().config().getClassLoader() : val.getClass().getClassLoader());
            }

            if (val != null)
                return (T)val;

            assert valBytes != null;

            Object val = ctx.processor().unmarshal(ctx, valBytes, ctx.kernalContext().config().getClassLoader());

            if (ctx.storeValue())
                this.val = val;

            return (T)val;
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException("Failed to unmarshall object.", e);
        }
    }

    /** {@inheritDoc} */
    @Override public byte[] valueBytes(CacheObjectContext ctx) throws IgniteCheckedException {
        if (valBytes == null)
            valBytes = ctx.processor().marshal(ctx, val);

        return valBytes;
    }

    /** {@inheritDoc} */
    @Override public void prepareMarshal(CacheObjectContext ctx) throws IgniteCheckedException {
        assert val != null || valBytes != null;

        if (valBytes == null)
            valBytes = ctx.kernalContext().cacheObjects().marshal(ctx, val);
    }

    /** {@inheritDoc} */
    @Override public void finishUnmarshal(CacheObjectContext ctx, ClassLoader ldr) throws IgniteCheckedException {
        assert val != null || valBytes != null;

        if (val == null && ctx.storeValue())
            val = ctx.processor().unmarshal(ctx, valBytes, ldr);
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 89;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        assert false;

        return super.hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        assert false;

        return super.equals(obj);
    }

    /** {@inheritDoc} */
    @Override public CacheObject prepareForCache(CacheObjectContext ctx) {
        return this;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!super.writeTo(buf, writer))
            return false;

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 1:
                if (!writer.writeBoolean("cacheHit", cacheHit))
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

        if (!super.readFrom(buf, reader))
            return false;

        switch (reader.state()) {
            case 1:
                cacheHit = reader.readBoolean("cacheHit");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();
        }

        return reader.afterMessageRead(CacheObjectImpl.class);
    }

    public boolean cacheHit() {
        return cacheHit;
    }

    public void cacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
}
