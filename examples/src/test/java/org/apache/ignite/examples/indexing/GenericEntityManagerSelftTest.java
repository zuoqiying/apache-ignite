package org.apache.ignite.examples.indexing;

import java.util.Map;
import org.apache.ignite.lang.IgniteBiClosure;

/** */
public class GenericEntityManagerSelftTest extends AbstractEntityManagerSelfTest {
    /** {@inheritDoc} */
    @Override protected EntityManager<Long, TestUser> mgr(String name,
        Map<String, IgniteBiClosure<StringBuilder, Object, String>> indices, IdGenerator<Long> gen) {
        return new EntityManager<>(name, indices, gen);
    }
}
