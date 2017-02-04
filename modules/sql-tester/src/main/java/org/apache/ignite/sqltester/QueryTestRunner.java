package org.apache.ignite.sqltester;

/**
 *
 */
public interface QueryTestRunner {
    public void beforeTest(QueryTypeConfiguration cfg);

    public String driverClassName();

    public void afterTest();
}
