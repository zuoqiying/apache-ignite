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

package org.apache.ignite.internal.visor.file;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;
import org.apache.ignite.lang.IgniteBiTuple;

import static org.apache.ignite.internal.visor.util.VisorTaskUtils.readBlock;

/**
 * Task to read file block.
 */
@GridInternal
public class VisorFileBlockTask extends VisorOneNodeTask<VisorFileBlockArg,
    IgniteBiTuple<? extends IOException, VisorFileBlock>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorFileBlockJob job(VisorFileBlockArg arg) {
        return new VisorFileBlockJob(arg, debug);
    }

    /**
     * Job that read file block on node.
     */
    private static class VisorFileBlockJob
        extends VisorJob<VisorFileBlockArg, IgniteBiTuple<? extends IOException, VisorFileBlock>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Descriptor of file block to read.
         * @param debug Debug flag.
         */
        private VisorFileBlockJob(VisorFileBlockArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected IgniteBiTuple<? extends IOException, VisorFileBlock> run(VisorFileBlockArg arg) {
            try {
                URL url = U.resolveIgniteUrl(arg.getPath());

                if (url == null)
                    return new IgniteBiTuple<>(new NoSuchFileException("File path not found: " + arg.getPath()), null);

                VisorFileBlock block = readBlock(new File(url.toURI()), arg.getOffset(), arg.getBlockSize(), arg.getLastModified());

                return new IgniteBiTuple<>(null, block);
            }
            catch (IOException e) {
                return new IgniteBiTuple<>(e, null);
            }
            catch (URISyntaxException ignored) {
                return new IgniteBiTuple<>(new NoSuchFileException("File path not found: " + arg.getPath()), null);
            }
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorFileBlockJob.class, this);
        }
    }
}
