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

package org.apache.ignite.internal.visor.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Data transfer object for memory configuration.
 */
public class VisorMemoryConfiguration extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Concurrency level. */
    private int concLvl;

    /** File cache allocation path. */
    private String fileCacheAllocationPath;

    /** Amount of memory allocated for the page cache. */
    private long pageCacheSize;

    /** Page size. */
    private int pageSize;

    /**
     * Default constructor.
     */
    public VisorMemoryConfiguration() {
        // No-op.
    }

    /**
     * Create data transfer object.
     *
     * @param memCfg Memory configuration.
     */
    public VisorMemoryConfiguration(MemoryConfiguration memCfg) {
        concLvl = memCfg.getConcurrencyLevel();
        fileCacheAllocationPath = memCfg.getFileCacheAllocationPath();
        pageCacheSize = memCfg.getPageCacheSize();
        pageSize = memCfg.getPageSize();
    }

    /**
     * @return Concurrency level.
     */
    public int getConcurrencyLevel() {
        return concLvl;
    }

    /**
     * @return File allocation path.
     */
    public String getFileCacheAllocationPath() {
        return fileCacheAllocationPath;
    }

    /**
     * @return Page cache size, in bytes.
     */
    public long getPageCacheSize() {
        return pageCacheSize;
    }

    /**
     * @return Page size.
     */
    public int getPageSize() {
        return pageSize;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeInt(concLvl);
        U.writeString(out, fileCacheAllocationPath);
        out.writeLong(pageCacheSize);
        out.writeInt(pageSize);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        concLvl = in.readInt();
        fileCacheAllocationPath = U.readString(in);
        pageCacheSize = in.readLong();
        pageSize = in.readInt();
    }
}
