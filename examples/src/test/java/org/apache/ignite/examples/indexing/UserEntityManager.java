package org.apache.ignite.examples.indexing;

import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteClosure;

/**
 * <p>
 * The <code>UserEntityManager</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class UserEntityManager extends EntityManager<TestUser> {
    /**
     * @param fields Fields. Each field has name and property value to string transformer.
     */
    @SafeVarargs
    public UserEntityManager(IgniteBiTuple<String, IgniteClosure<Object, String>>... fields) {
        super("user", fields);
    }

    /** {@inheritDoc} */
    @Override public IndexedFields indexedFields(long key, TestUser testUser) {
        IndexedFields indexedFields = new IndexedFields(name, key);

        for (IgniteBiTuple<String, IgniteClosure<Object, String>> field : fields) 
            indexedFields.addField(new Field(field.get1(), field.get2().apply(U.field(testUser, field.get1()))));

        return indexedFields;
    }
}
