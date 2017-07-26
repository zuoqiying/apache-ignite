package org.apache.ignite.sqltester;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.internal.util.typedef.internal.A;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Created by oostanin on 25.07.17.
 */
public class IgniteSqlRunner {

    public HashMap<Integer, ArrayList<String>> runStatements(QueryTypeConfiguration igniteConf, Collection<HashMap<String, Object>> sqlStatements){

        QueryTestRunner runner = igniteConf.getType().createRunner();

        HashMap<Integer, ArrayList<String>> results = new HashMap<>();

        try {
            runner.beforeTest(igniteConf);

            Class.forName(runner.driverClassName());

            Connection conn;

            String connStr = igniteConf.getConnectionString();

            if (igniteConf.getProperties() != null) {
                Properties p = new Properties();

                p.putAll(igniteConf.getProperties());

                conn = DriverManager.getConnection(connStr, p);
            }
            else
                conn = DriverManager.getConnection(connStr);

            Statement stmt = conn.createStatement();


            for(HashMap<String, Object> statement : sqlStatements){
                int id = Integer.valueOf(statement.get("id").toString());

                ArrayList<String> resultList = new ArrayList<>();

                String query = statement.get("ignite").toString();

                String ex = null;
                ResultSet result = null;

                try {
                    stmt.execute(query);
                }
                catch (Exception e) {
                    ex = e.getMessage();
                }

                if(ex == null)
                    result = stmt.getResultSet();
                else {
                    resultList.add(ex);
                    resultList.add(query);
                    results.put(id, resultList);
                    continue;
                }

                if (result != null){
                    resultList = setToList(result);
                    results.put(id, resultList);
                }
                else
                    results.put(id, resultList);
            }


        }
        catch (Exception e) {
            // Do cleanup TODO
            runner.afterTest();
        }

        runner.afterTest();

        return results;

    }

    private ArrayList<String> setToList(ResultSet set) throws Exception{

        ArrayList<ArrayList<String>> resultTbl = new ArrayList<>();
        ArrayList<String> columnNames = new ArrayList<>();

        ArrayList<String> result = new ArrayList<>();

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

        StringBuilder colNamesStr = new StringBuilder();

        for(String columnName : columnNames)
            colNamesStr.append(columnName + "    ");

        result.add(colNamesStr.toString());

        for(ArrayList<String> row : resultTbl){
            StringBuilder rowString = new StringBuilder();

            for(String res : row)
                rowString.append(res + "    ");

            result.add(rowString.toString());
        }
        return result;
    }
}
