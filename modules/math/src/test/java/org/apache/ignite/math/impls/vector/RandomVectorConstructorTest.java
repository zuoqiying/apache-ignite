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

package org.apache.ignite.math.impls.vector;

import org.apache.ignite.math.exceptions.UnsupportedOperationException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/** */
public class RandomVectorConstructorTest {
    /** */ private static final int IMPOSSIBLE_SIZE = -1;

    /** */ @Test(expected = org.apache.ignite.math.exceptions.UnsupportedOperationException.class)
    public void mapInvalidArgsTest() {
        assertEquals("Expect exception due to invalid args.", IMPOSSIBLE_SIZE,
            new RandomVector(new HashMap<String, Object>(){{put("invalid", 99);}}).size());
    }

    /** */ @Test(expected = UnsupportedOperationException.class)
    public void mapMissingArgsTest() {
        final Map<String, Object> test = new HashMap<String, Object>(){{
            put("paramMissing", "whatever");
        }};

        assertEquals("Expect exception due to missing args.",
            -1, new RandomVector(test).size());
    }

    /** */ @Test(expected = ClassCastException.class)
    public void mapInvalidParamTypeTest() {
        final Map<String, Object> test = new HashMap<String, Object>(){{
            put("size", "whatever");
            put("fastHash", true);
        }};

        assertEquals("Expect exception due to invalid param type.", IMPOSSIBLE_SIZE,
            new RandomVector(test).size());
    }

    /** */ @Test(expected = AssertionError.class)
    public void mapNullTest() {
        //noinspection ConstantConditions
        assertEquals("Null map args.", IMPOSSIBLE_SIZE,
            new RandomVector(null).size());
    }

    /** */ @Test
    public void mapTest() {
        assertEquals("Size from args.", 99,
            new RandomVector(new HashMap<String, Object>(){{ put("size", 99); }}).size());

        final int test = 99;

        assertEquals("Size from args with fastHash false.", test,
            new RandomVector(new HashMap<String, Object>(){{
                put("size", test);
                put("fastHash", false);
            }}).size());

        assertEquals("Size from args with fastHash true.", test,
            new RandomVector(new HashMap<String, Object>(){{
                put("size", test);
                put("fastHash", true);
            }}).size());
    }

    /** */ @Test(expected = AssertionError.class)
    public void negativeSizeTest() {
        assertEquals("Negative size.", IMPOSSIBLE_SIZE,
            new RandomVector(-1).size());
    }
}