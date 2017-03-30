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

package org.apache.ignite.internal.processors.hadoop.impl.igfs;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.igfs.secondary.IgfsSecondaryFileSystemPositionedReadable;
import org.apache.ignite.internal.processors.igfs.IgfsLazySecondaryFileSystemPositionedReadable;
import org.apache.ignite.internal.util.typedef.internal.U;

import java.io.IOException;

/**
 * Secondary file system input stream wrapper which actually opens input stream only in case it is explicitly
 * requested.
 */
public class HadoopIgfsSecondaryFileSystemPositionedReadable implements IgfsSecondaryFileSystemPositionedReadable {
    /** Secondary file system. */
    private final FileSystem fs;

    /** Path to the file to open. */
    private final Path path;

    /** Buffer size. */
    private final int bufSize;

    /** Lazy value. */
    private final IgfsLazySecondaryFileSystemPositionedReadable.LazyValue<FSDataInputStream> lazyVal
        = new IgfsLazySecondaryFileSystemPositionedReadable.LazyValue<FSDataInputStream>() {
        /** {@inheritDoc} */
        @Override protected FSDataInputStream create() throws IOException {
            FSDataInputStream in = fs.open(path, bufSize);

            if (in == null)
                 throw new IOException("Failed to open input stream (file system returned null): " + path);

            return in;
        }
    };

    /**
     * Constructor.
     *
     * @param fs Secondary file system.
     * @param path Path to the file to open.
     * @param bufSize Buffer size.
     */
    public HadoopIgfsSecondaryFileSystemPositionedReadable(FileSystem fs, Path path, int bufSize) {
        assert fs != null;
        assert path != null;

        this.fs = fs;
        this.path = path;
        this.bufSize = bufSize;
    }

    /** Get input stream. */
    private PositionedReadable in() throws IOException {
        try {
            FSDataInputStream is = lazyVal.getOrCreate();

            assert is != null;

            return is;
        }
        catch (IgniteCheckedException ice) {
            throw new IOException(ice);
        }
    }

    /**
     * Close wrapped input stream in case it was previously opened.
     */
    @Override public void close() {
        try {
            FSDataInputStream is = lazyVal.getAsIs();

            U.closeQuiet(is);
        } catch (IgniteCheckedException ice) {
            throw new IgniteException(ice);
        }
    }

    /** {@inheritDoc} */
    @Override public int read(long pos, byte[] buf, int off, int len) throws IOException {
        return in().read(pos, buf, off, len);
    }
}