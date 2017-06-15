package org.apache.ignite.sqltester;

/**
 *
 */
public interface QueryTestRunner {

    String getType();

    public void beforeTest(QueryTypeConfiguration cfg);

    public String driverClassName();

    public void afterTest();
}
