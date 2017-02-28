package org.apache.ignite.examples.indexing;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The <code>IndexedFields</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class IndexedFields {
    /** */
    private final long id;

    /** */
    private final String name;

    /** */
    private List<Field> fields = new ArrayList<>();

    /**
     * @param name Name.
     * @param id Id.
     */
    public IndexedFields(String name, long id) {
        this.id = id;
        this.name = name;
    }

    /** */
    public long id() {
        return id;
    }

    /** */
    public String name() {
        return name;
    }

    /** */
    public void addField(Field field) {
        fields.add(field);
    }

    /** */
    public List<Field> fields() {
        return fields;
    }
}
