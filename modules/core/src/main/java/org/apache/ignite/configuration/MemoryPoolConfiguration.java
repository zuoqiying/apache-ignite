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

package org.apache.ignite.configuration;

/**
 * Memory pool is part of memory with certain size. Each cache is connected with certain pool (with default one
 * if nothing else set). Memory pool can belong to many caches.
 */
public class MemoryPoolConfiguration {
    /** Link. */
    private final MemoryPoolLink link;

    /** Size. */
    private final long size;

    /** path for memory-mapped file (optional) */
    private String tmpFsPath;

    /** Concurrency level. */
    private int concLvl;

    /**
     * @param link Link.
     * @param size Size.
     */
    public MemoryPoolConfiguration(MemoryPoolLink link, long size) {
        this(link, size, 0, null);
    }

    /**
     * @param link Link.
     * @param size Size.
     * @param concLvl Conc lvl.
     * @param tmpFsPath Tmp fs path.
     */
    public MemoryPoolConfiguration(MemoryPoolLink link, long size, int concLvl, String tmpFsPath) {
        this.link = link;
        this.size = size;
        this.concLvl = concLvl;
        this.tmpFsPath = tmpFsPath;
    }

    /**
     * @return link for this memory pool configuration
     */
    public MemoryPoolLink getLink() {
        return link;
    }

    /**
     * @return size of this memory pool
     */
    public long getSize() {
        return size;
    }

    /**
     * @return path to memory-mapped file (can be relative or absolute)
     */
    public String getTmpFsPath() {
        return tmpFsPath;
    }

    /**
     * @return Concurrency level.
     */
    public int getConcurrencyLevel() {
        return concLvl;
    }

    /**
     * Will update tmpFsPath
     * @param tmpFsPath path to memory-mapped file (can be relative or absolute)
     */
    public void setTmpFsPath(String tmpFsPath) {
        this.tmpFsPath = tmpFsPath;
    }

    /**
     * Update concurrency level
     * @param concLvl Concurrency level.
     */
    public void setConcurrencyLevel(int concLvl) {
        this.concLvl = concLvl;
    }
}
