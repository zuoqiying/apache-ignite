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

package org.apache.ignite.internal.visor.database;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.internal.processors.cache.database.IgniteMemoryPoolMetrics;

import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Create data transfer object for memory pool metrics.
 */
public class VisorMemoryPoolMetrics extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Memory pool name. */
    private String name;

    /** Total number of pages available. */
    private long totalPages;

    /**
     * Default constructor.
     */
    public VisorMemoryPoolMetrics() {
        // No-op.
    }

    /**
     * Create data transfer object.
     *
     * @param metrics Memory pool metrics.
     */
    public VisorMemoryPoolMetrics(IgniteMemoryPoolMetrics metrics) {
        name = metrics.name();
        totalPages = metrics.totalPages();
    }

    /**
     * @return Memory pool name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Total number of pages available.
     */
    public long getTotalPages() {
        return totalPages;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, name);
        out.writeLong(totalPages);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        name = U.readString(in);
        totalPages = in.readLong();
    }
}
