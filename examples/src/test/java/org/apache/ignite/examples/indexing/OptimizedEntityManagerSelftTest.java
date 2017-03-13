package org.apache.ignite.examples.indexing;

import java.util.Map;
import org.apache.ignite.lang.IgniteBiClosure;

/** */
public class OptimizedEntityManagerSelftTest extends AbstractEntityManagerSelfTest {
    /** {@inheritDoc} */
    @Override protected EntityManager<Long, TestUser> mgr(String name,
        Map<String, IgniteBiClosure<StringBuilder, Object, String>> indices, IdGenerator<Long> gen) {
        return new OptimizedEntityManager<>(name, indices);
    }
}
