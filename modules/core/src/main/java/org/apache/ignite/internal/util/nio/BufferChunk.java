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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 *
 */
public class BufferChunk {
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
        return state.get() == 0 && state.compareAndSet(0, 1);
    }

    public void release() {
        next = -1;
        buf.clear();
        subchunksCreated = 0;
        subchunksReleased.set(0);
        state.set(0);
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
        int c = subchunksReleased.incrementAndGet();

        if (c == subchunksCreated)
            release();
    }

    void onBeforeRead() {
        buf.flip();
    }

    BufferReadSubChunk nextSubChunk() {
        if (buf.remaining() < 12)
            return null;

        long threadId = buf.getLong();
        int expLen = buf.getInt();

        if (buf.remaining() < expLen) {
            buf.position(buf.position() - 12);

            return null;
        }

        ByteBuffer subBuf = buf.slice();

        subBuf.limit(expLen);

        BufferReadSubChunk ret = new BufferReadSubChunk(threadId, subBuf, this);

        subchunksCreated--;

        buf.position(buf.position() + expLen);

        return ret;
    }

    void copyTo(BufferChunk dst) {
        assert dst.reserved();

        dst.buffer().put(buf);
    }

    void tryRelease() {
        int c = subchunksCreated;

        if (c == 0) {
            release();

            return;
        }

        assert c < 0;

        subchunksCreated = -c;

        if (subchunksReleased.get() == -c)
            release();
    }

    public void onBeforeWrite() {
        buf.clear(); // TODO once threw java.nio.BufferOverflowException
        buf.putLong(Thread.currentThread().getId());
        buf.putInt(0); // Reserve space for size.
    }

    public void onAfterWrite() {
        buf.putInt(8, buf.position() - 12);
        buf.flip();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(BufferChunk.class, this);
    }
}
