package org.apache.ignite.sqltester;

/**
 *
 */
public class MYSQLQueryTestRunner implements QueryTestRunner {

    private final String type = "mysql";

    @Override public String getType() {
        return type;
    }

    @Override public void beforeTest(QueryTypeConfiguration cfg) {
        //String cfgPath = F.isEmpty(cfg.getProperties()) ? "ignite-localhost-config.xml" :
        //        (String) cfg.getProperties().get("igniteCfgPath");

        //Ignite ignite = Ignition.start(cfgPath);
    }

    @Override public String driverClassName() {
        return "com.mysql.jdbc.Driver";
    }

    @Override public void afterTest() {

    }
}
