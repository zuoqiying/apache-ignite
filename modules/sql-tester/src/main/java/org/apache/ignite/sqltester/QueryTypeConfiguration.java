package org.apache.ignite.sqltester;

import java.util.Map;

/**
 *
 */
public class QueryTypeConfiguration {
    private QueryTestType type;

    private String connString;

    private String dbInitScriptPath;

    private Map<String, ?> props;

    public QueryTestType getType() {
        return type;
    }

    public void setType(QueryTestType type) {
        this.type = type;
    }

    public String getConnectionString() {
        return connString;
    }

    public void setConnectionString(String connectionString) {
        this.connString = connectionString;
    }

    public String getDbInitScriptPath() {
        return dbInitScriptPath;
    }

    public void setDbInitScriptPath(String dbInitScriptPath) {
        this.dbInitScriptPath = dbInitScriptPath;
    }

    public Map<String, ?> getProperties() {
        return props;
    }

    public void setProperties(Map<String, ?> properties) {
        this.props = properties;
    }
}
