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

package org.apache.ignite.examples.indexing;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jsr166.ThreadLocalRandom8;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * The <code>IndexingExample</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class IndexingExampleSelfTest extends GridCommonAbstractTest {
    /** Entity manager. */
    private EntityManager<Long, TestUser> mgr;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setCacheConfiguration(mgr.cacheConfigurations());

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        mgr = new EntityManager<>("user",
                new HashMap<String, IgniteClosure<Object, String>>() {{
                    put("firstName", new IgniteClosure<Object, String>() {
                        @Override public String apply(Object val) {
                            return U.field(val, "firstName").toString().toLowerCase();
                        }
                    });
                    put("lastName", new IgniteClosure<Object, String>() {
                        @Override public String apply(Object val) {
                            return U.field(val, "lastName").toString().toLowerCase();
                        }
                    });
                    put("email", new IgniteClosure<Object, String>() {
                        @Override public String apply(Object val) {
                            return U.field(val, "email").toString().toLowerCase();
                        }
                    });
                    put("age", new IgniteClosure<Object, String>() {
                        @Override public String apply(Object val) {
                            return U.field(val, "age").toString().toLowerCase();
                        }
                    });
                    put("fio", new IgniteClosure<Object, String>() {
                        @Override public String apply(Object val) {
                            return U.field(val, "lastName").toString().toLowerCase() + U.field(val, "firstName").toString().toLowerCase();
                        }
                    });
                }}
        );
    }

    /**
     * Tests entity creation.
     */
    public void testCreate() throws Exception {
        try {
            IgniteEx igniteEx = startGrid(0);

            mgr.attach(igniteEx);

            TestUser user1 = new TestUser("Ivan", "Petrov", "IvanPetrov@email.com", ThreadLocalRandom8.current().nextInt(10, 80));

            mgr.save(1L, user1);

            TestUser user2 = new TestUser("Petr", "Sidorov", "PetrSidorov@email.com", ThreadLocalRandom8.current().nextInt(10, 80));

            mgr.save(2L, user2);

            TestUser user3 = new TestUser("Ivan", "Sidorov", "IvanSidorov@email.com", ThreadLocalRandom8.current().nextInt(10, 80));

            mgr.save(3L, user3);
        } finally {
            stopAllGrids();
        }
    }

//    /**
//     * Tests entity creation.
//     */
//    public void testCreate() throws Exception {
//        try {
//            IgniteEx igniteEx = startGrid(0);
//
//            mgr.attach(igniteEx);
//
//            final int total = 100_000;
//
//            final String field = "firstName";
//
//            final AtomicInteger cnt = new AtomicInteger();
//
//            multithreaded(new Runnable() {
//                @Override public void run() {
//                    int i = 0;
//
//                    while((i = cnt.getAndIncrement()) < total) {
//                        TestUser user1 = new TestUser("Ivan", "Petrov", "test" + i + "@email.com", ThreadLocalRandom8.current().nextInt(10, 80));
//
//                        long id = 1;
//
//                        mgr.save(id, user1);
//
//                        TestUser user2 = mgr.get(id);
//
//                        assertEquals(user1, user2);
//
//                        assertTrue(id + "", mgr.contains(field, "Ivan", id));
//
//                        assertFalse(id + "", mgr.contains(field, "Petr", id));
//
//                        if ((i + 1) % 10_000 == 0)
//                            log().info("Processed " + (i + 1) + " of " + total);
//                    }
//                }
//            }, 1);
//
//            assertEquals(4, mgr.indexSize(field));
//        } finally {
//            stopAllGrids();
//        }
//    }
}
