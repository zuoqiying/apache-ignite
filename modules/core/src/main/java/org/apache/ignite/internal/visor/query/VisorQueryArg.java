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

package org.apache.ignite.internal.visor.query;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Arguments for {@link VisorQueryTask}.
 */
public class VisorQueryArg extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache name for query. */
    private String cacheName;

    /** Query text. */
    private String qryTxt;

    /** Distributed joins enabled flag. */
    private boolean distributedJoins;

    /** Flag whether to execute query locally. */
    private boolean loc;

    /** Result batch size. */
    private int pageSize;

    /**
     * Default constructor.
     */
    public VisorQueryArg() {
        // No-op.
    }

    /**
     * @param cacheName Cache name for query.
     * @param qryTxt Query text.
     * @param loc Flag whether to execute query locally.
     * @param pageSize Result batch size.
     */
    public VisorQueryArg(String cacheName, String qryTxt, boolean distributedJoins, boolean loc, int pageSize) {
        this.cacheName = cacheName;
        this.qryTxt = qryTxt;
        this.distributedJoins = distributedJoins;
        this.loc = loc;
        this.pageSize = pageSize;
    }

    /**
     * @return Cache name.
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     * @return Query txt.
     */
    public String queryTxt() {
        return qryTxt;
    }

    /**
     * @return Distributed joins enabled flag.
     */
    public boolean distributedJoins() {
        return distributedJoins;
    }

    /**
     * @return {@code true} if query should be executed locally.
     */
    public boolean local() {
        return loc;
    }

    /**
     * @return Page size.
     */
    public int pageSize() {
        return pageSize;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, cacheName);
        U.writeString(out, qryTxt);
        out.writeBoolean(distributedJoins);
        out.writeBoolean(loc);
        out.writeInt(pageSize);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        cacheName = U.readString(in);
        qryTxt = U.readString(in);
        distributedJoins = in.readBoolean();
        loc = in.readBoolean();
        pageSize = in.readInt();
    }
}
