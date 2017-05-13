package org.apache.ignite.sqltester;

import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 *
 */
public class IgniteSqlTester {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        //if (F.isEmpty(args) || args.length != 2)
        //    throw new IgniteException();

        //String configPath = args[0];
        String configPath = "../incubator-ignite/modules/sql-tester/src/main/resources/sql-tester-cfg.xml";

        // Initialize Spring factory.
        FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext(configPath);

        SqlTesterConfiguration cfg = ctx.getBean(SqlTesterConfiguration.class);

        //String testPath = args[1];
        String testPath = "../incubator-ignite/modules/sql-tester/src/main/resources/test-script.sql";

        {
            Set<QueryTestType> types = new HashSet<>();

            for (QueryTypeConfiguration typeConf : cfg.getTypeConfigurations()) {
                QueryTestType t = typeConf.getType();

                if (!types.add(t))
                    throw new IgniteException(); // Duplicate types
            }
        }

        List<RunContext> runners = new ArrayList<>();

        List<QueryTestType> runnerTypes = new ArrayList<>();

        for (QueryTypeConfiguration typeConf : cfg.getTypeConfigurations()) {
            QueryTestType t = typeConf.getType();

            QueryTestRunner runner = t.createRunner();

            try {
                runner.beforeTest(typeConf);

                Class.forName(runner.driverClassName());

                Connection conn;

                String connStr = typeConf.getConnectionString();

                if (typeConf.getProperties() != null) {
                    Properties p = new Properties();

                    p.putAll(typeConf.getProperties());

                    conn = DriverManager.getConnection(connStr, p);
                }
                else
                    conn = DriverManager.getConnection(connStr);

                if (!F.isEmpty(typeConf.getDbInitScriptPath())) {
                    Statement stmt = conn.createStatement();

                    try (BufferedReader br = new BufferedReader(new FileReader(typeConf.getDbInitScriptPath()))) {
                        for (String testStr; (testStr = br.readLine()) != null; ) {
                            stmt.execute(testStr);
                        }
                    }
                    catch (Exception e) {
                        U.closeQuiet(stmt);
                        U.closeQuiet(conn);

                        throw e;
                    }
                }

                RunContext runCtx = new RunContext();

                runCtx.runner = runner;
                runCtx.conn = conn;

                runners.add(runCtx);
                runnerTypes.add(t);
            }
            catch (Exception e) {
                // Do cleanup
                for (RunContext r : runners)
                    r.runner.afterTest();

                throw e;
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(testPath))) {
            for (String testStr; (testStr = br.readLine()) != null; ) {
                for (RunContext runCtx : runners) {
                    Statement stmt = runCtx.conn.createStatement();

                    runCtx.res = stmt.executeQuery(testStr);
                }

                // ...verify...

                RunContext first = runners.get(0);

                int colsCnt = first.res.getMetaData().getColumnCount();

                while (first.res.next()) {
                    for (int i = 1; i <= colsCnt; i++) {
                        Object colVal = first.res.getObject(i);

                        System.out.println(first.res.getMetaData().getColumnName(i) + " -> " + colVal.toString());
                    }
                }

                // ...do cleanup...
            }
        }
    }

    private static class RunContext {
        QueryTestRunner runner;

        Connection conn;

        ResultSet res;
    }
}
