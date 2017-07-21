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

package org.apache.ignite.internal.processors.cache.persistence.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * File I/O factory which provides RandomAccessFileIO implementation of FileIO.
 */
public class RandomAccessFileIOFactory implements FileIOFactory {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override public FileIO create(File file) throws IOException {
        return create(file, "rw");
    }

    /** {@inheritDoc} */
    @Override public FileIO create(File file, String mode) throws IOException {
        RandomAccessFile rf = new RandomAccessFile(file, mode);

        return new RandomAccessFileIO(rf);
    }
}
