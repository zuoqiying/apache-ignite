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

package org.apache.ignite.internal.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks class as it has transient fields that should be serialized.
 * Annotated class must have method that returns list of transient
 * fields that should be serialized.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SerializableTransient {
    /**
     * If after deserialization that field is null Optimized marshaller
     * will try to deserialize object with new fields set (normal fields, transient from
     * list that returns specified method).
     *
     * @return Field that must be not null after successful deserialization.
     */
    String notNullField();

    /**
     * Name of the method that returns list of transient fields
     * that should be serialized (String[]), and accepts boolean flag, indicating
     * marshalling or unmarshalling procedure.
     * <p>
     *     If it returns empty array or null all transient fields will be normally
     *     ignored.
     * </p>
     *
     * @return Name of the method.
     */
    String methodName();
}
