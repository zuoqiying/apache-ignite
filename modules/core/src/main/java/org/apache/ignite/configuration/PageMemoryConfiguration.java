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
 *
 */
public class PageMemoryConfiguration {

    private final PageMemoryConfigurationLink link;

    /** Size. */
    private long size;

    /** path for memory-mapped file (optional) */
    private String tmpFsPath;

    /** Concurrency level. */
    private int concLvl;

    /**
     * @param link Link.
     * @param size Size.
     * @param concLvl Conc lvl.
     * @param tmpFsPath Tmp fs path.
     */
    public PageMemoryConfiguration(PageMemoryConfigurationLink link, long size, int concLvl, String tmpFsPath) {
        this.link = link;
        this.size = size;
        this.concLvl = concLvl;
        this.tmpFsPath = tmpFsPath;
    }

    /**
     *
     */
    public PageMemoryConfigurationLink getLink() {
        return link;
    }

    /**
     *
     */
    public long getSize() {
        return size;
    }

    /**
     *
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

    public void setSize(long size) {
        this.size = size;
    }

    public void setTmpFsPath(String tmpFsPath) {
        this.tmpFsPath = tmpFsPath;
    }

    public void setConcLvl(int concLvl) {
        this.concLvl = concLvl;
    }
}
