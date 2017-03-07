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

package org.apache.ignite.math.impls.storage.vector;

import static org.apache.ignite.math.impls.MathTestConstants.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DenseLocalOffHeapVectorStorage}.
 */
public class VectorOffheapStorageTest extends VectorBaseStorageTest<DenseLocalOffHeapVectorStorage> {
    /** */
    private static final double DOUBLE_ZERO = 0d;

    /** */
    @Before
    public void setUp() {
        storage = new DenseLocalOffHeapVectorStorage(STORAGE_SIZE);
    }

    /** */
    @Test
    public void isArrayBased() throws Exception {
        assertFalse(UNEXPECTED_VALUE, storage.isArrayBased());
    }

    /** */
    @Test
    public void data() throws Exception {
        assertNull(NULL_VALUE, storage.data());
    }

    /** */
    @Test
    public void isSequentialAccess() throws Exception {
        assertTrue(UNEXPECTED_VALUE, storage.isSequentialAccess());
    }

    /** */
    @Test
    public void isDense() throws Exception {
        assertTrue(UNEXPECTED_VALUE, storage.isDense());
    }

    /** */
    @Test
    public void getLookupCost() throws Exception {
        assertEquals(VALUE_NOT_EQUALS, storage.getLookupCost(), DOUBLE_ZERO, NIL_DELTA);
    }

    /** */
    @Test
    public void isAddConstantTime() throws Exception {
        assertTrue(UNEXPECTED_VALUE, storage.isAddConstantTime());
    }

    /** */
    @Test
    public void equalsTest(){
        assertTrue(VALUE_NOT_EQUALS, storage.equals(storage));

        assertFalse(VALUES_SHOULD_BE_NOT_EQUALS, storage.equals(new ArrayVectorStorage(STORAGE_SIZE)));
    }

}