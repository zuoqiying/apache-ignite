package org.apache.ignite.sqltester;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.ignite.IgniteCheckedException;
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

    private static CopyOnWriteArrayList<HashMap<String, Object>> sqlStatements;

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        //if (F.isEmpty(args) || args.length != 2)
        //    throw new IgniteException();

        Properties props = new Properties();

        try (FileInputStream is = new FileInputStream(args[0])) {
            props.load(is);
        }
        catch (FileNotFoundException ignore) {
            System.out.println("Properties file not found");
        }

        try {
            sqlStatements = new StatementGenerator(props).generate(args);
        }
        catch (IgniteCheckedException e) {
            e.printStackTrace();
        }

        String cfgPath = props.getProperty("cfgPath");

        // Initialize Spring factory.
        FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext(cfgPath);

        SqlTesterConfiguration cfg = ctx.getBean(SqlTesterConfiguration.class);

        String testPath = props.getProperty("testPath");

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
                /**
                 if (!F.isEmpty(typeConf.getDbInitScriptPath())) {
                 Statement stmt = conn.createStatement();

                 try (BufferedReader br = new BufferedReader(new FileReader(typeConf.getDbInitScriptPath()))) {
                 for (String testStr; (testStr = br.readLine()) != null; )
                 stmt.execute(testStr);
                 }
                 catch (Exception e) {
                 U.closeQuiet(stmt);
                 U.closeQuiet(conn);

                 throw e;
                 }
                 }
                 */

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

        for (HashMap<String, Object> entry : sqlStatements) {
            for (RunContext runCtx : runners) {
                Statement stmt = runCtx.conn.createStatement();

                String type = runCtx.runner.getType();

                String st = (String)entry.get(type);

                if (!type.equals("ignite")) {
                    System.out.println(st);

                    stmt.execute(st);

                    if (!(stmt.getResultSet() == null))
                        runCtx.res = stmt.getResultSet();
                }
            }

            try {
                System.out.println(compareSets(runners));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            // ...do cleanup...
        }

    }

    private static boolean compareSets(List<RunContext> runners) throws Exception {

        for (RunContext runCtx : runners) {

            if (runCtx.runner.getType().equals("ignite"))
                continue;

            if (!(runCtx.res == null)) {

                ArrayList<ArrayList<String>> resultTbl = new ArrayList<>();

                int colsCnt = runCtx.res.getMetaData().getColumnCount();
                System.out.println(colsCnt);


                while (runCtx.res.next()) {

                    ArrayList<String> row = new ArrayList<>(colsCnt);

                    for (int i = 1; i <= colsCnt; i++) {
                        Object colVal = runCtx.res.getObject(i);

                        row.add(colVal.toString());

                        System.out.println(runCtx.res.getMetaData().getColumnName(i) + " -> " + colVal.toString());
                    }

                    resultTbl.add(row);

                }

            }
        }
        return true;

    }

    private static class RunContext {
        QueryTestRunner runner;

        Connection conn;

        ResultSet res;
    }

}
