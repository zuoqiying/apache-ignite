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
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 *
 */
public class BufferChunk {
    public static final byte FIRST_MASK = 0x1;
    public static final byte LAST_MASK = 0x2;
    public static final byte ORDERED_MASK = 0x4;

    private final int idx;

    private int next = -1;

    private final ByteBuffer buf;

    private final AtomicInteger state;

    private volatile int subchunksCreated;

    private final AtomicInteger subchunksReleased;

    public BufferChunk(
        int idx,
        ByteBuffer buf
    ) {
        this.idx = idx;
        this.buf = buf;

        state = new AtomicInteger();
        subchunksReleased = new AtomicInteger();
    }

    boolean reserved() {
        return state.get() != 0;
    }

    public boolean reserve() {
        int i = state.get();

        return (i & 1) == 0 && state.compareAndSet(i, i + 1);
    }

    public void release(int curState) {
        if (curState == -1)
            // Already locked for release.
            return;

        if (curState == 0)
            curState = state.get();

        else if (!(state.get() == curState && state.compareAndSet(curState, -1)))
            return;

        next = -1;
        buf.clear();
        subchunksCreated = 0;
        subchunksReleased.set(0);
        state.set(curState + 1);
    }

    public ByteBuffer buffer() {
        return buf;
    }

    public int index() {
        return idx;
    }

    public void next(int next) {
        assert next != idx;

        this.next = next;
    }

    public int next() {
        return next;
    }

    public boolean hasNext() {
        return next != -1;
    }

    public void releaseSubChunk() {
        int state0 = state.get();
        int c = subchunksReleased.incrementAndGet();

        if (c == subchunksCreated)
            release(state0);
    }

    void onBeforeRead() {
        buf.flip();
    }

    public BufferReadSubChunk nextSubChunk() {
        if (buf.remaining() < 12)
            return null;

        long threadId = buf.getLong();
        int expLen = buf.getInt();

        byte plc = (byte)(expLen >> 24);
        byte flags = (byte)((expLen >> 16) & 0xFF);

        expLen = expLen & 0xFFFF;

        if (buf.remaining() < expLen) {
            buf.position(buf.position() - 12);

            return null;
        }

        ByteBuffer subBuf = buf.slice();

        subBuf.limit(expLen);

        BufferReadSubChunk ret = new BufferReadSubChunk(threadId, subBuf, this, plc, flags);

        buf.position(buf.position() + expLen);

        subchunksCreated--;

        return ret;
    }

    void copyTo(BufferChunk dst) {
        assert dst.reserved();

        dst.buffer().put(buf);
    }

    public int state() {
        return state.get();
    }

    void tryRelease() {
        int c = subchunksCreated;

        if (c == 0) {
            release(0);

            return;
        }

        assert c < 0;

        int state0 = state.get();

        subchunksCreated = -c;

        if (subchunksReleased.get() == -c)
            release(state0);
    }

    public void onBeforeWrite() {
        buf.clear(); // TODO once threw java.nio.BufferOverflowException
        buf.putLong(Thread.currentThread().getId());
        buf.putInt(0); // Reserve space for size.
    }

    public void onAfterWrite(byte plc, boolean ordered, boolean first, boolean last) {
        int flags = 0;

        if (ordered)
            flags |= ORDERED_MASK;

        if (last)
            flags |= LAST_MASK;

        if (first)
            flags |= FIRST_MASK;

        int data = ((plc & 0xFF) << 24) | (flags << 16) | (buf.position() - 12);

        buf.putInt(8, data);
        buf.flip();
    }

    public static void putChunkHeader(
        ByteBuffer buf,
        int pos,
        byte plc,
        boolean ordered,
        boolean first,
        boolean last
    ) {
        if (pos == buf.position() - 12) {
            buf.position(pos);

            return;
        }

        buf.putLong(pos, Thread.currentThread().getId());

        int flags = 0;

        if (ordered)
            flags |= ORDERED_MASK;

        if (last)
            flags |= LAST_MASK;

        if (first)
            flags |= FIRST_MASK;

        int data = ((plc & 0xFF) << 24) | (flags << 16) | (buf.position() - pos - 12);

        buf.putInt(pos + 8, data);

//        U.debug(log, "Header has been put [threadId=" + Thread.currentThread().getId() + ", len=" + (buf.position() - pos - 12) +
//            ", ordered=" + ordered + ", first=" + first + ", last=" + last + ']');
    }

    public static int reserveChunkHeader(ByteBuffer buf) {
        if (buf.remaining() < 12)
            return -1;

        int pos = buf.position();

        buf.position(pos + 12);

        return pos;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(BufferChunk.class, this);
    }
}
