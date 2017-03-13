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

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiClosure;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jsr166.ThreadLocalRandom8;

/**
 * <p> The <code>IndexingExample</code> </p>
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
            new HashMap<String, IgniteBiClosure<StringBuilder, Object, String>>() {{
                put("firstName", new IgniteBiClosure<StringBuilder, Object, String>() {
                    @Override public String apply(StringBuilder builder, Object val) {
                        return builder.append(U.field(val, "firstName").toString().toLowerCase()).toString();
                    }
                });
                put("lastName", new IgniteBiClosure<StringBuilder, Object, String>() {
                    @Override public String apply(StringBuilder builder, Object val) {
                        return builder.append(U.field(val, "lastName").toString().toLowerCase()).toString();
                    }
                });
                put("email", new IgniteBiClosure<StringBuilder, Object, String>() {
                    @Override public String apply(StringBuilder builder, Object val) {
                        return builder.append(U.field(val, "email").toString().toLowerCase()).toString();
                    }
                });
                put("age", new IgniteBiClosure<StringBuilder, Object, String>() {
                    @Override public String apply(StringBuilder builder, Object val) {
                        return builder.append(U.field(val, "age").toString().toLowerCase()).toString();
                    }
                });
                put("fio", new IgniteBiClosure<StringBuilder, Object, String>() {
                    @Override public String apply(StringBuilder builder, Object val) {
                        return builder.append(U.field(val, "lastName").toString().toLowerCase()).
                            append(U.field(val, "firstName").toString().toLowerCase()).toString();
                    }
                });
            }},
            new SequenceIdGenerator()
        );

        IgniteEx grid = startGrid(0);

        mgr.attach(grid);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * Tests entity creation with manual key assignment.
     */
    public void testCreate() throws Exception {
        TestUser user1 = new TestUser("Ivan", "Petrov", "IvanPetrov@email.com", 20);

        long id = mgr.save(null, user1);

        assertEquals(0, id);

        TestUser u = mgr.get(id);

        assertEquals(user1, u);

        TestUser user2 = new TestUser("Petr", "Sidorov", "PetrSidorov@email.com", 30);

        id = mgr.save(null, user2);

        assertEquals(1, id);

        u = mgr.get(id);

        assertEquals(user2, u);

        TestUser user3 = new TestUser("Ivan", "Sidorov", "IvanSidorov@email.com", 30);

        id = mgr.save(null, user3);

        assertEquals(2, id);

        u = mgr.get(id);

        assertEquals(user3, u);

        assertTrue(mgr.contains("firstName", user1, 0L));

        assertTrue(mgr.contains("firstName", user1, 2L));

        assertTrue(mgr.contains("firstName", user2, 1L));

        TestUser example = new TestUser("ivan", "sidorov", "some@email.com", 10);

        assertTrue(mgr.contains("fio", example, 2L));

        example.setLastName("sidorov1");

        assertFalse(mgr.contains("fio", example, 2L));

        assertEquals(2, mgr.findAll(user1, "firstName").size());

        assertEquals(1, mgr.findAll(user1, "fio").size());

        assertEquals(0, mgr.findAll(example, "fio").size());
    }

    /**
     * Tests entity update.
     */
    public void testUpdate() {
        TestUser u1 = new TestUser("Ivan", "Petrov", "IvanPetrov@email.com", 20);

        long id = mgr.save(null, u1);

        TestUser u1Cp = mgr.get(id);

        TestUser u2 = new TestUser("Petr", "Sidorov", "PetrSidorov@email.com", 30);

        long id2 = mgr.save(null, u2);

        assertEquals(1, mgr.findAll(u1, "firstName").size());
        assertEquals(1, mgr.findAll(u1, "lastName").size());
        assertEquals(1, mgr.findAll(u1, "email").size());
        assertEquals(1, mgr.findAll(u1, "age").size());
        assertEquals(1, mgr.findAll(u1, "fio").size());

        u1.setFirstName("Fedor");
        u1.setAge(30);

        mgr.save(id, u1);

        assertEquals(0, mgr.findAll(u1Cp, "firstName").size());
        assertEquals(1, mgr.findAll(u1Cp, "lastName").size());
        assertEquals(1, mgr.findAll(u1Cp, "email").size());
        assertEquals(0, mgr.findAll(u1Cp, "age").size());
        assertEquals(0, mgr.findAll(u1Cp, "fio").size());

        assertEquals(1, mgr.findAll(u1, "firstName").size());
        assertEquals(1, mgr.findAll(u1, "lastName").size());
        assertEquals(1, mgr.findAll(u1, "email").size());
        assertEquals(2, mgr.findAll(u1, "age").size());
        assertEquals(1, mgr.findAll(u1, "fio").size());
    }

    /**
     * Tests entity removal.
     */
    public void testRemove() {
        TestUser u1 = new TestUser("Ivan", "Petrov", "IvanPetrov@email.com", 20);

        long id = mgr.save(null, u1);

        assertEquals(1, mgr.findAll(u1, "firstName").size());
        assertEquals(1, mgr.findAll(u1, "lastName").size());
        assertEquals(1, mgr.findAll(u1, "email").size());
        assertEquals(1, mgr.findAll(u1, "age").size());
        assertEquals(1, mgr.findAll(u1, "fio").size());

        mgr.delete(id);

        assertEquals(0, mgr.findAll(u1, "firstName").size());
        assertEquals(0, mgr.findAll(u1, "lastName").size());
        assertEquals(0, mgr.findAll(u1, "email").size());
        assertEquals(0, mgr.findAll(u1, "age").size());
        assertEquals(0, mgr.findAll(u1, "fio").size());
    }

    /**
     * Tests multithreaded entity creation.
     */
    public void testCreateMultithreaded() throws Exception {
        final int total = 100_000;

        final AtomicInteger cnt = new AtomicInteger();

        final int[] firstNamesCnt = new int[600];
        final int[] lastNamesCnt = new int[15_000];
        final int[] agesCnt = new int[100];

        multithreaded(new Runnable() {
            @Override public void run() {
                int i = 0;

                while ((i = cnt.getAndIncrement()) < total) {
                    int fnIdx = ThreadLocalRandom8.current().nextInt(firstNamesCnt.length);
                    int lnIdx = ThreadLocalRandom8.current().nextInt(lastNamesCnt.length);
                    int age = ThreadLocalRandom8.current().nextInt(agesCnt.length);

                    TestUser user1 = new TestUser("fname" + fnIdx,
                        "lname" + lnIdx,
                        "test" + i + "@email.com",
                        age);

                    mgr.save(null, user1);

                    if ((i + 1) % 10_000 == 0)
                        log().info("Processed " + (i + 1) + " of " + total);
                }
            }
        }, 1);

        TestUser u = new TestUser();

        for (int i = 0; i < total; i++) {
            u.setEmail("test" + i + "@email.com");

            Collection<T2<Long, TestUser>> entities = mgr.findAll(u, "email");

            assertEquals(1, entities.size());

            assertEquals(i, entities.iterator().next().get1().intValue());

            if ((i + 1) % 10_000 == 0)
                log().info("Verified " + (i + 1) + " of " + total);
        }
    }
}