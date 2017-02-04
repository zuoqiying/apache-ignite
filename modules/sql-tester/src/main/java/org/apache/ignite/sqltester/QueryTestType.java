package org.apache.ignite.sqltester;

/**
 *
 */
public enum QueryTestType {
    IGNITE {
        @Override public QueryTestRunner createRunner() {
            return new IgniteQueryTestRunner();
        }
    },

    MYSQL {
        @Override public QueryTestRunner createRunner() {
            throw new UnsupportedOperationException();
        }
    },
    ;

    public abstract QueryTestRunner createRunner();
}
