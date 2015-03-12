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

package org.apache.ignite.internal.util.io;

import java.io.*;
import java.nio.*;

/**
 * Input stream that reads from provided {@link ByteBuffer}.
 */
public class GridByteBufferInputStream extends InputStream {
    /** */
    private final ByteBuffer buf;

    /**
     * @param buf Byte buffer.
     */
    public GridByteBufferInputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    /** {@inheritDoc} */
    @Override public int read() throws IOException {
        return buf.get();
    }

    /** {@inheritDoc} */
    @Override public int read(byte[] arr) throws IOException {
        int pos = buf.position();

        buf.get(arr);

        return buf.position() - pos;
    }

    /** {@inheritDoc} */
    @Override public int read(byte[] arr, int off, int len) throws IOException {
        int pos = buf.position();

        buf.get(arr, off, len);

        return buf.position() - pos;
    }

    /** {@inheritDoc} */
    @Override public long skip(long n) throws IOException {
        if (n <= 0)
            return 0;

        long skipped = Math.min(buf.remaining(), n);

        buf.position(buf.position() + (int)skipped);

        return skipped;
    }

    /** {@inheritDoc} */
    @Override public int available() throws IOException {
        return buf.remaining();
    }

    /** {@inheritDoc} */
    @Override public boolean markSupported() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public synchronized void mark(int readLimit) {
        buf.mark();
    }

    /** {@inheritDoc} */
    @Override public synchronized void reset() throws IOException {
        buf.reset();
    }
}
