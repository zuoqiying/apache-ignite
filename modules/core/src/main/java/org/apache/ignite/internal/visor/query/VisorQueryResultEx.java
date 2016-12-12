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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Result for cache query tasks.
 */
public class VisorQueryResultEx extends VisorQueryResult {
    /** */
    private static final long serialVersionUID = 0L;

    /** Node where query executed. */
    private UUID resNodeId;

    /** Query ID to store in node local. */
    private String qryId;

    /** Query columns descriptors. */
    private Collection<VisorQueryField> cols;

    /**
     * Default constructor.
     */
    public VisorQueryResultEx() {
        // No-op.
    }

    /**
     * @param resNodeId Node where query executed.
     * @param qryId Query ID for future extraction in nextPage() access.
     * @param cols Columns descriptors.
     * @param rows Rows fetched from query.
     * @param hasMore Whether query has more rows to fetch.
     * @param duration Query duration.
     */
    public VisorQueryResultEx(
        UUID resNodeId,
        String qryId,
        Collection<VisorQueryField> cols,
        List<Object[]> rows,
        boolean hasMore,
        long duration
    ) {
        super(rows, hasMore, duration);

        this.resNodeId = resNodeId;
        this.qryId = qryId;
        this.cols = cols;
    }

    /**
     * @return Response node id.
     */
    public UUID getResponseNodeId() {
        return resNodeId;
    }

    /**
     * @return Query id.
     */
    public String getQueryId() {
        return qryId;
    }

    /**
     * @return Columns.
     */
    public Collection<VisorQueryField> getColumns() {
        return cols;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        super.writeExternalData(out);

        U.writeUuid(out, resNodeId);
        U.writeString(out, qryId);
        U.writeCollection(out, cols);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternalData(in);

        resNodeId = U.readUuid(in);
        qryId = U.readString(in);
        cols = U.readCollection(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorQueryResultEx.class, this);
    }
}
