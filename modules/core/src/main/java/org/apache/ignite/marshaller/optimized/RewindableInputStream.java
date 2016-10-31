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

package org.apache.ignite.marshaller.optimized;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Stream delegates all read operations to original stream but caches all data, that allows rewind
 * it back to beginning. Internal buffer expands with no bounds, so may be used only for
 * small cached data.
 */
class RewindableInputStream extends InputStream {
    /** */
    private ByteBuffer buf;

    /** */
    private InputStream delegate;

    /** */
    private boolean readBuf;

    /**
     *
     */
    RewindableInputStream() {
        buf = ByteBuffer.allocate(512);
    }

    /** {@inheritDoc} */
    @Override public int read() throws IOException {
        if (readBuf) {
            if (buf.remaining() > 0)
                return buf.get() & 0xFF;
        }

        final int res = delegate.read();

        if (res >= 0) {
            ensureBuffer(1);

            buf.put((byte) res);
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public int read(@NotNull final byte[] b, final int off, final int len) throws IOException {
        if (readBuf) {
            int toRead = Math.min(buf.remaining(), len);

            buf.get(b, off, toRead);

            if (toRead == len)
                return toRead;
        }

        readBuf = false;

        final int read = delegate.read(b, off, len);

        if (read > 0) {
            ensureBuffer(read);

            buf.put(b, off, read);
        }

        return read;
    }

    /** {@inheritDoc} */
    @Override public int read(@NotNull final byte[] b) throws IOException {
        if (readBuf) {
            int toRead = Math.min(buf.remaining(), b.length);

            buf.get(b, 0, toRead);

            if (toRead == b.length)
                return toRead;
        }

        final int read = delegate.read(b);

        if (read > 0) {
            ensureBuffer(read);

            buf.put(b, 0, read);
        }

        return read;
    }

    /**
     * @param len Length.
     */
    private void ensureBuffer(int len) {
        buf.limit(buf.capacity());

        if (buf.remaining() < len) {
            ByteBuffer newBuf = ByteBuffer.allocate(Math.max(buf.capacity() * 2, len));

            buf.rewind();

            newBuf.get(buf.array(), 0, buf.capacity());

            buf = newBuf;
        }
    }

    /**
     *
     */
    void rewind() {
        buf.flip();

        readBuf = true;
    }

    /**
     * @param in Original stream.
     */
    void original(InputStream in) {
        delegate = in;
    }

    /**
     * @return Original stream.
     */
    InputStream original() {
        return delegate;
    }
}
