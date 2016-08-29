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

package org.apache.ignite.internal.util.nio;

import java.nio.ByteBuffer;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 *
 */
public class BufferReadSubChunk {
    private final long threadId;

    private final ByteBuffer buf;

    @GridToStringExclude
    private final BufferChunk parent;

    private final byte plc;

    @GridToStringExclude
    private final byte flags;

    public BufferReadSubChunk(
        long threadId,
        ByteBuffer buf,
        BufferChunk parent,
        byte plc,
        byte flags
    ) {
        this.threadId = threadId;
        this.buf = buf;
        this.parent = parent;
        this.plc = plc;
        this.flags = flags;
    }

    public long threadId() {
        return threadId;
    }

    public void release() {
        parent.releaseSubChunk();
    }

    public ByteBuffer buffer() {
        return buf;
    }

    public byte policy() {
        return plc;
    }

    public boolean first() {
        return (flags & BufferChunk.FIRST_MASK) != 0;
    }

    public boolean last() {
        return (flags & BufferChunk.LAST_MASK) != 0;
    }

    public boolean ordered() {
        return (flags & BufferChunk.ORDERED_MASK) != 0;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(BufferReadSubChunk.class, this, "first", first(), "last", last(),
            "ordered", ordered(),
            "remaining", buf.remaining());
    }
}
