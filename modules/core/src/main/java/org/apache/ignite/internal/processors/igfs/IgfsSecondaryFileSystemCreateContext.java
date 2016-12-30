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

package org.apache.ignite.internal.processors.igfs;

import org.apache.ignite.igfs.IgfsFile;
import org.apache.ignite.igfs.IgfsPath;
import org.apache.ignite.igfs.secondary.IgfsSecondaryFileSystem;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.util.Map;

/**
 * Context for secondary file system create request.
 * Note that it is never used for dual mode append operation.
 */
public class IgfsSecondaryFileSystemCreateContext {
    /** File system. */
    private final IgfsSecondaryFileSystem fs;

    /** Path. */
    private final IgfsPath path;

    /** Overwrite flag. */
    private final boolean overwrite;

    /** Simple create flag. */
    private final boolean simpleCreate;

    /** Properties. */
    private final Map<String, String> props;

    /** Replication. */
    private final short replication;

    /** Block size. */
    private final long blockSize;

    /** Buffer size. */
    private final int bufSize;

    /**
     * Constructor.
     *
     * @param fs File system.
     * @param path Path.
     * @param overwrite Overwrite flag.
     * @param simpleCreate Simple create flag.
     * @param props Properties.
     * @param replication Replication.
     * @param blockSize Group block size, that is, IGFS-configured block size multiplied by the group size. It
     *    represents the block size on the underlying secondary file system.
     * @param bufSize Buffer size.
     */
    public IgfsSecondaryFileSystemCreateContext(IgfsSecondaryFileSystem fs, IgfsPath path, boolean overwrite,
        boolean simpleCreate, @Nullable Map<String, String> props, short replication, long blockSize, int bufSize) {
        assert fs != null;

        this.fs = fs;
        this.path = path;
        this.overwrite = overwrite;
        this.simpleCreate = simpleCreate;
        this.props = props;
        this.replication = replication;
        this.blockSize = blockSize;
        this.bufSize = bufSize;
    }

    /**
     * Create file in the secondary file system.
     *
     * @return Output stream.
     */
    public OutputStream create() {
        return simpleCreate ? fs.create(path, overwrite) :
            fs.create(path, bufSize, overwrite, replication, blockSize, props);
    }

    /**
     * Get file info.
     *
     * @return File.
     */
    public IgfsFile info() {
        return fs.info(path);
    }

    /**
     * @return Secondary file system.
     */
    public IgfsSecondaryFileSystem fileSystem() {
        return fs;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(IgfsSecondaryFileSystemCreateContext.class, this);
    }
}
