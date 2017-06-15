package org.apache.ignite.sqltester;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteJdbcDriver;
import org.apache.ignite.Ignition;
import org.apache.ignite.internal.util.typedef.F;

/**
 *
 */
public class H2QueryTestRunner implements QueryTestRunner {

    private final String type = "h2";

    @Override public String getType() {
        return type;
    }

    @Override public void beforeTest(QueryTypeConfiguration cfg) {
        //String cfgPath = F.isEmpty(cfg.getProperties()) ? "ignite-localhost-config.xml" :
        //        (String) cfg.getProperties().get("igniteCfgPath");

        //Ignite ignite = Ignition.start(cfgPath);
    }

    @Override public String driverClassName() {
        return "org.h2.Driver";
    }

    @Override public void afterTest() {

    }
}
