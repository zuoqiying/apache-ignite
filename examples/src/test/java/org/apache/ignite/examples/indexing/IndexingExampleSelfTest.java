package org.apache.ignite.examples.indexing;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * The <code>IndexingExample</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class IndexingExampleSelfTest extends GridCommonAbstractTest {
    private UserEntityManager mgr;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setCacheConfiguration(mgr.cacheConfigurations());

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        mgr = new UserEntityManager(
                new IgniteBiTuple<String, IgniteClosure<Object, String>>("firstName", new IgniteClosure<Object, String>() {
                    @Override public String apply(Object s) {
                        return s.toString();
                    }
                }));
    }

    /**
     * Tests entity creation.
     */
    public void testCreate() throws Exception {
        try {
            IgniteEx igniteEx = startGrid(0);

            mgr.attach(igniteEx);

            int segments = 4;

            final int total = EntityManager.CAPACITY * segments;

            final String field = "firstName";

            final AtomicInteger cnt = new AtomicInteger();

            multithreaded(new Runnable() {
                @Override public void run() {
                    int i = 0;

                    while((i = cnt.getAndIncrement()) < total) {
                        TestUser user1 = new TestUser("Ivan", "Petrov", "test@email.com", 20);

                        long id = mgr.create(user1);

                        TestUser user2 = mgr.get(id);

                        assertEquals(user1, user2);

                        assertTrue(id + "", mgr.contains(field, "Ivan", id));

                        assertFalse(id + "", mgr.contains(field, "Petr", id));

                        if ((i + 1) % 10_000 == 0)
                            log().info("Processed " + (i + 1) + " of " + total);
                    }
                }
            }, 1);

            assertEquals(4, mgr.indexSize(field));
        } finally {
            stopAllGrids();
        }
    }
}
