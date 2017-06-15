package org.apache.ignite.sqltester;

/**
 *
 */
public class SqlTesterConfiguration {
    private QueryTypeConfiguration[] typeConfigs;

    public QueryTypeConfiguration[] getTypeConfigurations() {
        return typeConfigs;
    }

    public void setTypeConfigurations(QueryTypeConfiguration[] typeConfigurations) {
        this.typeConfigs = typeConfigurations;
    }
}
