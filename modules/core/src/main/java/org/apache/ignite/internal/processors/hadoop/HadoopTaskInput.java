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

package org.apache.ignite.internal.processors.hadoop;

import java.util.Iterator;
import org.apache.ignite.IgniteCheckedException;

/**
 * Task input.
 * TODO: add <K,V> parametrization!
 */
public interface HadoopTaskInput extends AutoCloseable {
    /**
     * Moves cursor to the next Key.
     *
     * @return {@code false} If input is exceeded.
     */
    boolean next();

    /**
     * Gets current key.
     *
     * @return Key. Never null. In case of deserializing iterator this is a deserialized object,
     * in case of byte[] iterator this is a buffer. If previous invocation of #next() returned false,
     * throws NoSuchElementException .
     */
    Object key();

    /**
     * Gets values for current key.
     *
     * @return Values. Never null. If previous invocation of #next() returned false,
     * throws NoSuchElementException .
     */
    Iterator<?> values();

    /**
     * Closes input.
     *
     * @throws IgniteCheckedException If failed.
     */
    @Override public void close() throws IgniteCheckedException;
}