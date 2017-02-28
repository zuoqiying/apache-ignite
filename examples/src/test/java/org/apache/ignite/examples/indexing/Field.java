package org.apache.ignite.examples.indexing;

/**
 * <p>
 * The <code>Field</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class Field {
    private String name;
    private String value;

    public Field(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Field field = (Field) o;

        if (!name.equals(field.name)) return false;

        return value.equals(field.value);
    }

    @Override public int hashCode() {
        int result = name.hashCode();

        result = 31 * result + value.hashCode();

        return result;
    }
}
