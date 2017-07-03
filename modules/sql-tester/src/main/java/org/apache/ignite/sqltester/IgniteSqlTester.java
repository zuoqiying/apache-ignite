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

        int cnt = 0;

        for (HashMap<String, Object> entry : sqlStatements) {

            String st = new String();

            for (RunContext runCtx : runners) {
                Statement stmt = runCtx.conn.createStatement();

                String type = runCtx.runner.getType();

                st = (String)entry.get(type);

                //if (!type.equals("ignite")) {
                    //System.out.println(st);

                    stmt.execute(st);

                    runCtx.res = stmt.getResultSet();

                //System.out.println();
                //}
            }

            try {
                if (!compareSets(runners)) {
                    System.out.println("Statement = " + st);
                    System.out.println("Statement id = " + cnt);
                    System.out.println("======================================================");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            // ...do cleanup...
            cnt++;
        }
        return;
    }

    private static boolean compareSets(List<RunContext> runners) throws Exception {

        ArrayList<ArrayList<ArrayList<String>>> sets = new ArrayList<>(runners.size());

        for (RunContext runCtx : runners) {

            if (!(runCtx.res == null)) {

                ArrayList<ArrayList<String>> resultTbl = new ArrayList<>();
                ArrayList<String> columnNames = new ArrayList<>();



                int colsCnt = runCtx.res.getMetaData().getColumnCount();
                //System.out.println(colsCnt);


                while (runCtx.res.next()) {

                    ArrayList<String> row = new ArrayList<>(colsCnt);

                    for (int i = 1; i <= colsCnt; i++) {
                        Object colVal = runCtx.res.getObject(i);

                        row.add(colVal.toString());

                        if(columnNames.size() < colsCnt)
                            columnNames.add(runCtx.res.getMetaData().getColumnName(i));
                    }

                    resultTbl.add(row);

                }

                resultTbl.sort(new Comparator<ArrayList<String>>() {
                    @Override public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                        for(int i = 0; i < o1.size(); i++){
                            if(o1.get(i).compareTo(o2.get(i)) > 0)
                                return 1;
                            if(o1.get(i).compareTo(o2.get(i)) < 0)
                                return -1;
                        }
                        return 0;
                    }
                });

                runCtx.resultTbl = resultTbl;
                runCtx.colNames = columnNames;
                /**
                for(ArrayList<String> innerList : resultTbl){
                    for (String str : innerList)
                        System.out.print(str + "    ");
                    System.out.println();
                }
                 */

            }
        }
        return compareResTbls(runners);

    }

    private static boolean compareResTbls(List<RunContext> runners){
        boolean res = true;

        if (runners.get(0).res == null) {
            for (RunContext runner : runners) {
                if (runner.res != null)
                    return false;
            }
            return true;
        }


        ArrayList<ArrayList<String>> main = runners.get(0).resultTbl;
        for (int i = 0; i < runners.size(); i++){
            ArrayList<ArrayList<String>> checked = runners.get(i).resultTbl;
            for(int row = 0; row < main.size(); row++){
                for(int col = 0; col < main.get(row).size(); col++){
                    Object mainObj = main.get(row).get(col);
                    Object checkedObj = checked.get(row).get(col);


                    if (!main.get(row).get(col).equals(checked.get(row).get(col))){
                        printError(runners.get(0), runners.get(i), mainObj, checkedObj, runners.get(i).colNames.get(col));
                        res = false;
                    }
                    /**
                    else {
                        System.out.print(" Row = " + row);
                        System.out.print(" Col = " + col);
                        System.out.println(" All is fine");
                    }
                     */
                }
            }
        }

        return res;
    }

    private static void printError(RunContext r1,  RunContext r2, Object main, Object checked, String columnName){
        System.out.println("The results from " + r1.runner.getType() + " and " + r2.runner.getType() +
            " do not match:");

        System.out.println(r1.runner.getType() + " -> " + main);
        System.out.println(r2.runner.getType() + " -> " + checked);
        System.out.println("Column -> " + columnName);

        System.out.println();
    }

    private static class RunContext {
        QueryTestRunner runner;

        Connection conn;

        ResultSet res;

        ArrayList<ArrayList<String>> resultTbl;
        ArrayList<String> colNames;
    }

}
