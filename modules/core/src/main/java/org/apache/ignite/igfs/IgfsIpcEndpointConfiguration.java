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

package org.apache.ignite.igfs;

import org.apache.ignite.internal.util.typedef.internal.*;

import static org.apache.ignite.igfs.IgfsIpcEndpointType.*;

/**
 *
 */
public class IgfsIpcEndpointConfiguration {
    /** */
    public static IgfsIpcEndpointType DFLT_TYP = U.isWindows() ? TCP : SHMEM;

    /** */
    public static String DFLT_HOST = "127.0.0.1";

    /** */
    public static int DFLT_PORT = 10500;

    /** Default shared memory space in bytes. */
    public static final int DFLT_SPACE_SIZE = 256 * 1024;

    /**
     * Default token directory. Note that this path is relative to {@code IGNITE_HOME/work} folder
     * if {@code IGNITE_HOME} system or environment variable specified, otherwise it is relative to
     * {@code work} folder under system {@code java.io.tmpdir} folder.
     *
     * @see org.apache.ignite.configuration.IgniteConfiguration#getWorkDirectory()
     */
    public static final String DFLT_TOKEN_DIR_PATH = "ipc/shmem";

    /** */
    private IgfsIpcEndpointType typ = DFLT_TYP;

    /** */
    private String host = DFLT_HOST;

    /** */
    private int port = DFLT_PORT;

    /** */
    private int spaceSize = DFLT_SPACE_SIZE;

    /** */
    private String tokenDirPath = DFLT_TOKEN_DIR_PATH;

    /**
     *
     * @return
     */
    public IgfsIpcEndpointType getType() {
        return typ;
    }

    /**
     *
     * @param typ
     */
    public void setType(IgfsIpcEndpointType typ) {
        this.typ = typ;
    }
}
