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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 *
 */
public class BufferChunkList {
    private final ByteBuffer bufs[];
    private final BufferChunk bufChunks[];

    private int offs; // todo always 0?

    private int len;

    public BufferChunkList(int size) {
        bufs = new ByteBuffer[size];
        bufChunks = new BufferChunk[size];
    }

    boolean hasRemaining() {
        for (int i = 0; i < len; i++) {
            if (bufs[i].hasRemaining())
                return true;
        }

        return false;
    }

    void compact() {
        int newStart = 0;

        for (int i = 0; i < len; i++) {
            if (bufs[i].remaining() == 0) {
                newStart = i + 1;

                bufChunks[i].release();
            }
            else
                break;
        }

        if (newStart == 0)
            return;

        if (newStart == len) {
            clear();

            return;
        }

        System.arraycopy(bufs, newStart, bufs, 0, len - newStart);
        System.arraycopy(bufChunks, newStart, bufChunks, 0, len - newStart);

        int oldLen = len;
        len -= newStart;

        for (int i = len; i < oldLen; i++) {
            bufs[i] = null;
            bufChunks[i] = null;
        }
    }

    private void clear() {
        for (int i = 0; i < len && bufs[i] != null; i++)
            bufs[i] = null;

        len = offs = 0;
    }

    long write(SocketChannel ch) throws IOException {
        long cnt = ch.write(bufs, offs, len);

        compact();

        return cnt;
    }

    void add(BufferChunk chunk) {
        assert chunk.reserved() : chunk;

        if (len + 1 == bufChunks.length)
            throw new IllegalStateException();

        bufChunks[len] = chunk;
        bufs[len] = chunk.buffer();

        len++;
    }

    public boolean hasFreeChunks() {
        return bufs.length - len > 1;
    }

    public boolean isEmpty() {
        return len == 0;
    }

    public int maxSize() {
        return bufs.length;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(BufferChunkList.class, this, "chunks",
            Arrays.toString(bufChunks));
    }
}
