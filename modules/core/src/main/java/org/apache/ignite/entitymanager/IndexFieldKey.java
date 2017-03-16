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

package org.apache.ignite.entitymanager;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 * <p>
 * The <code>IndexFieldEntry</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class IndexFieldKey {
    @QuerySqlField(index = true)
    private String fieldValue;

    private Object payload;

    public IndexFieldKey(String fieldVal, Object payload) {
        this.fieldValue = fieldVal;
        this.payload = payload;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexFieldKey that = (IndexFieldKey) o;

        if (!fieldValue.equals(that.fieldValue)) return false;
        return payload.equals(that.payload);

    }

    @Override public int hashCode() {
        int result = fieldValue.hashCode();
        result = 31 * result + payload.hashCode();
        return result;
    }
}
