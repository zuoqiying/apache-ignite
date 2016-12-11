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

package org.apache.ignite.internal.visor.igfs;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;
import org.jetbrains.annotations.Nullable;

/**
 * IGFS endpoint descriptor.
 */
public class VisorIgfsEndpoint extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** IGFS name. */
    private String igfsName;

    /** Grid name. */
    private String gridName;

    /** Host address / name. */
    private String hostName;

    /** Port number. */
    private int port;

    /**
     * Default constructor.
     */
    public VisorIgfsEndpoint() {
        // No-op.
    }

    /**
     * Create IGFS endpoint descriptor with given parameters.
     *
     * @param igfsName IGFS name.
     * @param gridName Grid name.
     * @param hostName Host address / name.
     * @param port Port number.
     */
    public VisorIgfsEndpoint(@Nullable String igfsName, String gridName, @Nullable String hostName, int port) {
        this.igfsName = igfsName;
        this.gridName = gridName;
        this.hostName = hostName;
        this.port = port;
    }

    /**
     * @return IGFS name.
     */
    @Nullable public String getIgfsName() {
        return igfsName;
    }

    /**
     * @return Grid name.
     */
    public String getGridName() {
        return gridName;
    }

    /**
     * @return Host address / name.
     */
    @Nullable public String getHostName() {
        return hostName;
    }

    /**
     * @return Port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * @return URI Authority
     */
    public String getAuthority() {
        String addr = hostName + ":" + port;

        if (igfsName == null && gridName == null)
            return addr;
        else if (igfsName == null)
            return gridName + "@" + addr;
        else if (gridName == null)
            return igfsName + "@" + addr;
        else
            return igfsName + ":" + gridName + "@" + addr;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, igfsName);
        U.writeString(out, gridName);
        U.writeString(out, hostName);
        out.writeInt(port);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        igfsName = U.readString(in);
        gridName = U.readString(in);
        hostName = U.readString(in);
        port = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorIgfsEndpoint.class, this);
    }
}
