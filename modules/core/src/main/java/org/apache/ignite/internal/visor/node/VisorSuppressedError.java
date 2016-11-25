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

import java.io.Serializable;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.util.VisorExceptionWrapper;

/**
 * Data transfer object for suppressed errors.
 */
public class VisorSuppressedError implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private long order;

    /** */
    @GridToStringExclude
    private VisorExceptionWrapper error;

    /** */
    private long threadId;

    /** */
    private String threadName;

    /** */
    private long time;

    /** */
    private String msg;

    /**
     * Constructor.
     *
     * @param order Locally unique ID that is atomically incremented for each new error.
     * @param error Suppressed error.
     * @param msg Message that describe reason why error was suppressed.
     * @param threadId Thread ID.
     * @param threadName Thread name.
     * @param time Occurrence time.
     */
    public VisorSuppressedError(long order, VisorExceptionWrapper error, String msg, long threadId, String threadName, long time) {
        this.order = order;
        this.error = error;
        this.threadId = threadId;
        this.threadName = threadName;
        this.time = time;
        this.msg = msg;
    }

    /**
     * @return Locally unique ID that is atomically incremented for each new error.
     */
    public long getOrder() {
        return order;
    }

    /**
     * @return Gets message that describe reason why error was suppressed.
     */
    public String getMessage() {
        return msg;
    }

    /**
     * @return Suppressed error.
     */
    public VisorExceptionWrapper getError() {
        return error;
    }

    /**
     * @return Gets thread ID.
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * @return Gets thread name.
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * @return Gets time.
     */
    public long getTime() {
        return time;
    }

    /** {@inheritDoc} */
    public String toString() {
        return S.toString(VisorSuppressedError.class, this);
    }
}
