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

                System.out.println(st);

                if(!type.equals("ignite")) {
                    stmt.execute(st);

                    if(!(stmt.getResultSet() == null))
                        runCtx.res = stmt.getResultSet();
                }


                System.out.println(st);
            }

            // ...verify...
            /**
             for(RunContext runner : runners){

             RunContext first = runner;

             int colsCnt = first.res.getMetaData().getColumnCount();

             System.out.println(first.conn.toString());
             System.out.println(first.res.getMetaData().getColumnCount());
             System.out.println(first.res.getMetaData().getColumnCount());

             while (first.res.next()) {
             for (int i = 1; i <= colsCnt; i++) {
             Object colVal = first.res.getObject(i);

             System.out.println(first.res.getMetaData().getColumnName(i) + " -> " + colVal.toString());
             }
             System.out.println("----------------------------------");
             }
             System.out.println("=================================");
             }
             */
            try {
                System.out.println(compareSets(runners.get(0), runners.get(1)));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            // ...do cleanup...
        }

    }

    private static boolean compareSets(RunContext rcIgnite, RunContext rcOther) throws Exception {
        int colsCnt = rcIgnite.res.getMetaData().getColumnCount();
        while (rcIgnite.res.next() && rcOther.res.next()) {
            int cntOther = 0;

            for (int i = 1; i <= colsCnt; i++) {

                String igniteColumnName = rcIgnite.res.getMetaData().getColumnName(i);

                if (igniteColumnName.equals("_KEY") || igniteColumnName.equals("_VAL"))
                    continue;

                cntOther++;

                String otherColumnName = rcOther.res.getMetaData().getColumnName(cntOther);

                if (!igniteColumnName.equals(otherColumnName))
                    return false;
                else {
                    String igniteVal = rcIgnite.res.getObject(i).toString();
                    String otherVal = rcOther.res.getObject(cntOther).toString();
                    if (!igniteVal.equals(otherVal))
                        return false;
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
