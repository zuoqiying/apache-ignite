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
package org.apache.ignite.sqltester.model.simple;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 * This class represents organization object.
 */
public class Organization {
    /** */
    private static final AtomicLong ID_GEN = new AtomicLong();

    /** Organization ID (indexed). */
    @QuerySqlField(index = true)
    private Long id;

    /** Organization name (indexed). */
    @QuerySqlField(index = true)
    private String name;

    /** Address. */
    @QuerySqlField(index = true)
    private String addr;

    /** Type. */
    @QuerySqlField(index = true)
    private String type;

    /** Last update time. */
    private Timestamp lastUpdated;

    /**
     * Required for binary deserialization.
     */
    public Organization() {
        // No-op.
    }

    /**
     * @param id Organization id.
     */
    public Organization(Long id) {
        this.id = id;
        this.name = "Organization_" + Long.toString(id);
        this.addr = "Address_" + Long.toString(id);
        if (id % 2 == 0)
            this.type = "PROFIT";
        else
            this.type = "NON-PROFIT";
    }


    /**
     * @return Organization ID.
     */
    public Long id() {
        return id;
    }

    /**
     * @return Name.
     */
    public String name() {
        return name;
    }

    /**
     * @return Address.
     */
    public String address() {
        return addr;
    }

    /**
     * @return Last update time.
     */
    public Timestamp lastUpdated() {
        return lastUpdated;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "Organization [id=" + id +
            ", name=" + name +
            ", address=" + addr +
            ", type=" + type + ']';
    }
}
