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

    H2 {
        @Override public QueryTestRunner createRunner() {
            return new H2QueryTestRunner();
        }
    },

    MYSQL {
        @Override public QueryTestRunner createRunner() {
            return new MYSQLQueryTestRunner();
        }
    },
    ;

    public abstract QueryTestRunner createRunner();
}
