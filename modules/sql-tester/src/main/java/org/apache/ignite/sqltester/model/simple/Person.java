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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.cache.affinity.AffinityKey;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.cache.query.annotations.QueryTextField;

/**
 * Person class.
 */
public class Person implements Serializable {
    /** */
    private static final AtomicLong ID_GEN = new AtomicLong();

    /** Person ID (indexed). */
    @QuerySqlField(index = true)
    public Long id;

    /** Organization ID (indexed). */
    @QuerySqlField(index = true)
    public Long orgId;

    /** First name (not-indexed). */
    @QuerySqlField
    public String firstName;

    /** Last name (not indexed). */
    @QuerySqlField
    public String lastName;

    /** Resume text (create LUCENE-based TEXT index for this field). */
    @QueryTextField
    public String resume;

    /** Salary (indexed). */
    @QuerySqlField(index = true)
    public double salary;

    /** Custom cache key to guarantee that person is always collocated with its organization. */
    private transient AffinityKey<Long> key;

    /**
     * Default constructor.
     */
    public Person() {
        // No-op.
    }

    /**
     * Constructs person record.
     *
     * @param id Person ID.
     */
    public Person(Long id) {
        this.id = id;
        this.orgId =  Long.parseLong(String.valueOf(id/10));
        this.firstName = "First Name " + Long.toString(id);
        this.lastName = "Last Name " + Long.toString(id);
        Long num = id;
        if (num > 1000) {
            String numStr = Long.toString(num);
            num = Long.parseLong(numStr.substring(numStr.length() - 3));
        }
        this.salary = 1000 + num;
        this.resume = "Resume " + Long.toString(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
        return "Person [id=" + id +
                ", orgId=" + orgId +
                ", lastName=" + lastName +
                ", firstName=" + firstName +
                ", salary=" + salary +
                ", resume=" + resume + ']';
    }
}
