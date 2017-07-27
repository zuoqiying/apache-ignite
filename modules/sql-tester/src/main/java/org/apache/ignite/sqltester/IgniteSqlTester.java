package org.apache.ignite.sqltester;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 *
 */
public class IgniteSqlTester {

    private static Collection<HashMap<String, Object>> sqlStatements;
    private static int operID;
    private static int passedOPS;
    private static int failedOPS;
    private static int msgInterval;
    private static PrintWriter writer;
    private static HashMap<Integer, ArrayList<String>> igniteResult;

    public static void main(String[] args) throws Exception {
        //if (F.isEmpty(args) || args.length != 2)
        //    throw new IgniteException();

        igniteResult = new HashMap<>();

        Properties props = new Properties();

        try (FileInputStream is = new FileInputStream(args[0])) {
            props.load(is);
        }
        catch (FileNotFoundException ignore) {
            System.out.println("Properties file not found");
        }

        try {
            if (props.getProperty("mode").equals("generate"))
                sqlStatements = new StatementGenerator(props).generate(args);
            else
                sqlStatements = readStatements(props.getProperty("statementsPath"));
        }
        catch (IgniteCheckedException e) {
            e.printStackTrace();//TODO
        }

        writer = new PrintWriter(new FileOutputStream(
            new File(props.getProperty("workDir") + File.separator + "sqltestlog" + System.currentTimeMillis() + ".log"),
            true));

        // Initialize Spring factory.
        FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext(props.getProperty("cfgPath"));

        SqlTesterConfiguration cfg = ctx.getBean(SqlTesterConfiguration.class);

        msgInterval = Integer.valueOf(props.getProperty("msgInterval"));

        Set<QueryTestType> types = new HashSet<>();

        QueryTypeConfiguration igniteConf = null;

        for (QueryTypeConfiguration typeConf : cfg.getTypeConfigurations()) {
            QueryTestType t = typeConf.getType();

            if (t.equals(QueryTestType.IGNITE))
                igniteConf = typeConf;

            if (!types.add(t))
                throw new IgniteException(); // Duplicate types
        }

        if (props.getProperty("onlyIgnite").equals("true")) {
            IgniteSqlRunner ir = new IgniteSqlRunner();

            try {
                igniteResult = ir.runStatements(igniteConf, sqlStatements);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            Yaml yaml = new Yaml();
            writer.write(yaml.dumpAs(igniteResult, null, DumperOptions.FlowStyle.BLOCK));
            writer.flush();
            writer.close();

            System.exit(0);
        }

        List<RunContext> runners = new ArrayList<>();

        for (QueryTypeConfiguration typeConf : cfg.getTypeConfigurations()) {
            QueryTestType t = typeConf.getType();

            QueryTestRunner runner = t.createRunner();

            try {
                runner.beforeTest(typeConf);

                Class.forName(runner.driverClassName());

                Connection conn;

                String connStr = typeConf.getConnectionString();

                conn = DriverManager.getConnection(connStr);

                RunContext runCtx = new RunContext();

                runCtx.runner = runner;
                runCtx.conn = conn;

                runners.add(runCtx);
            }
            catch (Exception e) {
                // Do cleanup
                for (RunContext r : runners)
                    r.runner.afterTest();

                throw e;
            }
        }

        writer = new PrintWriter(new FileOutputStream(
            new File(props.getProperty("workDir") + File.separator + "sqltestlog" + System.currentTimeMillis() + ".log"),
            true));

        for (HashMap<String, Object> entry : sqlStatements) {

            Long startTime = System.currentTimeMillis();

            for (RunContext runCtx : runners) {
                Statement stmt = runCtx.conn.createStatement();

                String ex = null;

                if (entry.get(runCtx.runner.getType()) == null)
                    continue;

                String st = (String)entry.get(runCtx.runner.getType());

                try {
                    stmt.execute(st);
                }
                catch (Exception e) {
                    ex = e.getMessage();
                }

                runCtx.res = stmt.getResultSet();

                if (props.getProperty("onlyIgnite").equals("true")) {
                    if (runCtx.res != null)
                        igniteResult.put((int)entry.get("id"), null);

                }
            }

            try {
                if (!compareSets(runners)) {
                    failedOPS++;
                    writer.println("---------------------------------------------------------------------------");
                    writer.println("Statement ID = " + operID);
                    writer.println("===========================================================================");
                }
                else
                    passedOPS++;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            // ...do cleanup...
            operID++;

            if ((startTime + (msgInterval * 1000L)) < System.currentTimeMillis())
                System.out.println("Operations passed = " + passedOPS + ", failed = " + failedOPS + ", total = " +
                    operID);
        }
        writer.close();

        for (RunContext r : runners)
            r.runner.afterTest();

        System.out.println("All statements are processed");
        System.out.println("Operations passed = " + passedOPS + ", failed = " + failedOPS + ", total = " + operID);

    }

    private static boolean compareSets(List<RunContext> runners) throws Exception {
        for (RunContext runCtx : runners) {

            if (runCtx.res != null) {

                LinkedList<ArrayList<String>> resultTbl = new LinkedList<>();
                ArrayList<String> columnNames = new ArrayList<>();

                int colsCnt = runCtx.res.getMetaData().getColumnCount();

                while (runCtx.res.next()) {

                    ArrayList<String> row = new ArrayList<>(colsCnt);

                    for (int i = 1; i <= colsCnt; i++) {
                        Object colVal = runCtx.res.getObject(i);

                        row.add(colVal.toString());

                        if (columnNames.size() < colsCnt)
                            columnNames.add(runCtx.res.getMetaData().getColumnName(i));
                    }

                    resultTbl.add(row);

                }

                Collections.sort(resultTbl, new Comparator<ArrayList<String>>() {
                    @Override public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                        for (int i = 0; i < o1.size(); i++) {
                            if (o1.get(i).compareTo(o2.get(i)) > 0)
                                return 1;
                            if (o1.get(i).compareTo(o2.get(i)) < 0)
                                return -1;
                        }
                        return 0;
                    }
                });

                runCtx.resultTbl = resultTbl;
                runCtx.colNames = columnNames;
            }
        }
        return compareResTbls(runners);

    }

    private static boolean compareResTbls(List<RunContext> runners) {
        if (runners.get(0).res == null) {
            for (RunContext runner : runners) {
                if (runner.res != null)
                    return false;
            }
            return true;
        }

        if (!runners.get(0).colNames.equals(runners.get(1).colNames)) {
            writer.println("Warning! Column names do not match!");
            writer.println(runners.get(0).runner.getType());
            writer.println(runners.get(1).runner.getType());
            printDiff(runners);
            return false;
        }

        LinkedList<ArrayList<String>> main = runners.get(0).resultTbl;
        LinkedList<ArrayList<String>> checked = runners.get(1).resultTbl;
        if (!main.equals(checked)) {
            printDiff(runners);
            return false;
        }
        else
            return true;
    }

    private static void printDiff(List<RunContext> runners) {
        ArrayList<Integer> format = getMaxLength(runners);

        if (!runners.get(0).colNames.equals(runners.get(1).colNames)) {
            printLists(runners.get(0).colNames, runners.get(1).colNames, getMaxLength(runners));
            return;
        }

        writer.println(runners.get(0).runner.getType());
        writer.println(runners.get(1).runner.getType());

        for (int col = 0; col < runners.get(0).colNames.size(); col++)
            writer.print(String.format("%-" + (format.get(col) + 4) + "s", runners.get(0).colNames.get(col)));
        writer.println();

        for (int row = 0; row < runners.get(0).resultTbl.size(); row++)
            printLists(runners.get(0).resultTbl.get(row), runners.get(1).resultTbl.get(row), format);

    }

    private static ArrayList<Integer> getMaxLength(List<RunContext> runners) {
        ArrayList<Integer> res = new ArrayList<>(runners.get(0).colNames.size());
        for (int col = 0; col < runners.get(0).colNames.size(); col++) {
            res.add(runners.get(0).colNames.get(col).length());
            for (RunContext runner : runners) {
                if (runner.colNames.get(col).length() > res.get(col))
                    res.set(col, runner.colNames.get(col).length());
                for (ArrayList<String> row : runner.resultTbl)
                    if ((row.get(col).length() + 1) > res.get(col))
                        res.set(col, row.get(col).length());
            }
        }
        return res;
    }

    private static void printLists(List<String> l1, List<String> l2, List<Integer> format) {
        for (int col = 0; col < l1.size(); col++) {
            String prefix = l1.get(col).equals(l2.get(col)) ? " " : "+";
            String fmt = "%-" + (format.get(col) + 3) + "s";
            writer.print(prefix + String.format(fmt, l1.get(col)));
        }
        writer.println();
        for (int col = 0; col < l2.size(); col++) {
            String prefix = l1.get(col).equals(l2.get(col)) ? " " : "-";
            String fmt = "%-" + (format.get(col) + 3) + "s";
            writer.print(prefix + String.format(fmt, l2.get(col)));
        }
        writer.println();
    }

    private static Collection<HashMap<String, Object>> readStatements(String path) {
        File newConfiguration = new File(path);
        InputStream is = null;

        try {
            is = new FileInputStream(newConfiguration);
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }

        Yaml yaml = new Yaml();
        return (Collection<HashMap<String, Object>>)yaml.load(is);
    }

    private static LinkedList<ArrayList<String>> setToTable(ResultSet set) throws Exception {

        LinkedList<ArrayList<String>> resultTbl = new LinkedList<>();
        ArrayList<String> columnNames = new ArrayList<>();

        int colsCnt = set.getMetaData().getColumnCount();

        while (set.next()) {

            ArrayList<String> row = new ArrayList<>(colsCnt);

            for (int i = 1; i <= colsCnt; i++) {
                Object colVal = set.getObject(i);

                row.add(colVal.toString());

                if (columnNames.size() < colsCnt)
                    columnNames.add(set.getMetaData().getColumnName(i));
            }

            resultTbl.add(row);

        }

        resultTbl.add(columnNames);

        return resultTbl;

    }

    private static class RunContext {
        QueryTestRunner runner;

        Connection conn;

        ResultSet res;

        LinkedList<ArrayList<String>> resultTbl;
        ArrayList<String> colNames;
    }

}
