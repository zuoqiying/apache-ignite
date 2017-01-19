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
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.VisorDataTransferObject;
import org.apache.ignite.internal.visor.util.VisorExceptionWrapper;

/**
 * Result for {@link VisorQueryTask}.
 */
public class VisorQueryTaskResult extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Exception on query execution. */
    private VisorExceptionWrapper error;

    /** Query result. */
    private VisorQueryResultEx res;

    /**
     * Default constructor.
     */
    public VisorQueryTaskResult() {
        // No-op.
    }

    /**
     * @param error Cache name for query.
     * @param res Query text.
     */
    public VisorQueryTaskResult(VisorExceptionWrapper error, VisorQueryResultEx res) {
        this.error = error;
        this.res = res;
    }

    /**
     * @return Exception on query execution.
     */
    public VisorExceptionWrapper getError() {
        return error;
    }

    /**
     * @return Query result.
     */
    public VisorQueryResultEx getResult() {
        return res;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeObject(error);
        out.writeObject(res);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        error = (VisorExceptionWrapper)in.readObject();
        res = (VisorQueryResultEx)in.readObject();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorQueryTaskResult.class, this);
    }
}
