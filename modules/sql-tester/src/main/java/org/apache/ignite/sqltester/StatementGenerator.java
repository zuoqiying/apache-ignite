/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.sqltester;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.sqltester.model.DefaultTable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class StatementGenerator {
    private String igniteCfgFile;
    private String sqlCfgFile;
    private String version = "0.1.0";
    private String workDir;
    private String filePrefix;
    private int threadsNum = 4;
    private ArrayList<String> forceFailOptions = new ArrayList<>();
    private ArrayList<String> printParticularStatement = new ArrayList<>();
    private int exitCode = 0;

    private long stmtCounter = 0L;
    private HashMap<String, Object> sqlCombinations;
    private CopyOnWriteArrayList<HashMap<String, Object>> sqlStatements;
    private String jdbcRefConn = "jdbc:h2:~/test;AUTO_SERVER=TRUE";
    private String jdbcRefUser = "sa";
    private String jdbcRefPwd = "";
    private volatile ArrayList<Thread> threads;
    private boolean igniteSqlNativeMode = true;
    private ArrayList<String> dialects = new ArrayList<>(Arrays.asList("h2", "ignite", "mysql"));
    private AtomicLong passedStmtCounter = new AtomicLong(0L);
    private AtomicLong failedStmtCounter = new AtomicLong(0L);

    public StatementGenerator (Properties props){
        sqlCfgFile = props.getProperty("sqlCfgFile");
        igniteCfgFile = props.getProperty("igniteCfgFile");
        workDir = props.getProperty("workDir");
        filePrefix = props.getProperty("filePrefix");

    }


    public CopyOnWriteArrayList<HashMap<String, Object>> generate(String[] args) throws IgniteException, IgniteCheckedException {

        System.out.println(">>> GenSet " + version);

        /**
        // Process command line arguments in format -name=value
        for (int i = 0; i < args.length; i++) {
            boolean argFound = false;
            String rawArg = args[i];
            if (rawArg.startsWith("-sql-config=")) {
                sqlCfgFile = rawArg.substring(12);
                argFound = true;
            }
            else if (rawArg.startsWith("-jdbc-ref-conn=")) {
                jdbcRefConn = rawArg.substring(15);
                argFound = true;
            }
            else if (rawArg.startsWith("-jdbc-ref-user=")) {
                jdbcRefUser = rawArg.substring(15);
                argFound = true;
            }
            else if (rawArg.startsWith("-jdbc-ref-pwd=")) {
                jdbcRefPwd = rawArg.substring(14);
                argFound = true;
            }
            else if (rawArg.startsWith("-work-dir=")) {
                workDir = rawArg.substring(10);
                argFound = true;
            }
            else if (rawArg.startsWith("-file-prefix=")) {
                filePrefix = rawArg.substring(13);
                argFound = true;
            }
            else if (rawArg.startsWith("-ignite-config=")) {
                igniteCfgFile = rawArg.substring(15);
                argFound = true;
            }
            else if (rawArg.startsWith("-threads-num=")) {
                threadsNum = Integer.parseInt(rawArg.substring(13));
                argFound = true;
            }
            else if (rawArg.startsWith("-force-fail=")) {
                forceFailOptions.add(rawArg.substring(12));
                argFound = true;
            }
            else if (rawArg.startsWith("-print-stmt=")) {
                printParticularStatement.add(rawArg.substring(12));
                argFound = true;
            }
            if (argFound)
                System.out.println(">>> Argument " + rawArg);

            else  {
                System.out.println("Unknown argument " + rawArg);
                //System.exit(-1);
            }
        }
        if (sqlCfgFile == null) {
            System.out.println(">>> Error: -sql-config=<path> option must be defined");
            System.exit(1);
        }

        */

        // Read YAML SQL combination file
        sqlCombinations = readYaml(sqlCfgFile);

        //System.out.println(sqlCombinations);
        // Convert combinations to the list of the statements
        sqlStatements = generateSqlStatements(sqlCombinations);
        // Store SQL statements into YAML file
        System.out.println(">>> " + sqlStatements.size() + " statements generated");
        if (filePrefix != null) {
            String dumpPath = filePrefix + "_statements.yaml";
            if (workDir != null)
                dumpPath = new File(workDir, dumpPath).toString();

            Yaml yaml = new Yaml();
            try {
                FileWriter writer = new FileWriter(dumpPath);
                writer.write(yaml.dumpAs(sqlStatements, null, DumperOptions.FlowStyle.BLOCK));
                writer.flush();
                writer.close();
                System.out.println(">>> SQL statements dumped in " + dumpPath);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        //System.out.println(sqlStatements);
        // Process statements
        //processStatements();
        System.out.println(">>> The result: " + passedStmtCounter + "/" + sqlStatements.size() +
                " statements passed (" + failedStmtCounter + " failed, " + (sqlStatements.size() - failedStmtCounter.get() - passedStmtCounter.get()) + " skipped)");
        //System.exit(exitCode);

        return sqlStatements;
    }

    private HashMap<String, Object> readYaml (String yamlFile) {
        // Read combination config file
        File newConfiguration = new File(yamlFile);
        InputStream is = null;

        try {
            is = new FileInputStream(newConfiguration);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }

        Yaml yaml = new Yaml();
        return (HashMap<String, Object>) yaml.load(is);
    }

    private CopyOnWriteArrayList<HashMap<String, Object>> generateSqlStatements(HashMap<String, Object> input) throws IgniteCheckedException {
        CopyOnWriteArrayList<HashMap<String, Object>> output = new CopyOnWriteArrayList<>();
        HashMap<String, Object> in = new HashMap<String, Object>();
        // Run iterators
        for (String rootKey : input.keySet()) {
            ArrayList<Object> updateList = new ArrayList<>();
            for (Object el: (ArrayList<Object>)input.get(rootKey)) {
                if (el instanceof String) {
                    LinkedHashMap<String, String> newEl = new LinkedHashMap<>();
                    for (String dialect: dialects)
                        newEl.put(dialect, (String)el);

                    updateList.add(newEl);
                }
                else if (el instanceof LinkedHashMap) {
                    LinkedHashMap<String, String> element = (LinkedHashMap<String, String>) el;
                    if (element.containsKey("iterator")) {
                        ArrayList<Object> iterElements =  parseIterator(element.get("iterator"));

                        for (Object iterElement: iterElements)
                            processIterElement(iterElement, element, updateList);
                    }
                    else
                        updateList.add(el);

                }
            }
            in.put(rootKey, updateList);
        }

        // Construct statements
        ArrayList<String> rootKeys = new ArrayList<>(in.keySet());
        for (String rootKey: rootKeys) {
            ArrayList<LinkedHashMap<String, String>> rootValues
                    = (ArrayList<LinkedHashMap<String, String>>) in.get(rootKey);
            for (String dstKey : rootKeys) {
                ArrayList<LinkedHashMap<String, String>> newValues = new ArrayList<LinkedHashMap<String, String>>();
                for (LinkedHashMap<String, String> el : (ArrayList<LinkedHashMap<String, String>>) in.get(dstKey)) {
                    boolean needToProceed = false;
                    for (Object attrVal: el.values()) {
                        if (attrVal instanceof String) {
                            String s = (String)attrVal;
                            // Debug
                            if (s.matches(".*\\b" +rootKey + "\\b.*"))
                                needToProceed = true;

                            //if (needToProceed)
                            //    System.out.println(s + " ~ " + rootKey);
                        }
                    }
                    if (needToProceed) {
                        for (LinkedHashMap<String, String> rootValEl: rootValues ) {
                            // Make a full list of attributes
                            LinkedHashMap<String, String> newEl = new LinkedHashMap<>();
                            for (String attrKey: el.keySet()) {
                                if (dialects.contains(attrKey)) {
                                    String elVal = el.get(attrKey);
                                    if (rootValEl.get(attrKey).equalsIgnoreCase("pass")) {
                                        // Debug
                                        //System.out.println(rootKey + " ---key--> " + dstKey);
                                        //System.out.println(rootValEl.get(attrKey) + " ---val--> " + elVal);
                                        if (elVal.endsWith(" " + rootKey)) {
                                            // Debug
                                            //System.out.println(elVal + " ---rep--> " + elVal.replace(rootKey, "").trim());
                                            newEl.put(attrKey, elVal.replace(rootKey, "").trim());
                                        }
                                        else if (rootValEl.get(attrKey).equalsIgnoreCase("pass"))
                                            newEl.put(attrKey, rootValEl.get(attrKey));

                                    }
                                    else
                                        newEl.put(attrKey, elVal.replaceAll("\\b" + rootKey + "\\b", rootValEl.get(attrKey)));
                                }
                                else
                                    newEl.put(attrKey, el.get(attrKey));

                            }
                            for (String attrKey: rootValEl.keySet()) {
                                if (!el.containsKey(attrKey))
                                    newEl.put(attrKey, rootValEl.get(attrKey));

                            }
                            newValues.add(newEl);
                        }

                    }
                    else
                        newValues.add(el);

                }
                in.put(dstKey, newValues);
            }
        }
        // Add statement id to simplify the debugging of failed statements
        int stmtIdx = 1;
        for (HashMap<String,Object> stmtData: (ArrayList<HashMap<String,Object>>)in.get("main")) {
            stmtData.put("id", Integer.toString(stmtIdx));
            output.add(stmtData);
            stmtIdx++;
        }
        return output;
    }

    private void processIterElement(Object iterElement, LinkedHashMap<String, String> el, ArrayList<Object> updateList){
        if (iterElement instanceof HashMap) {
            LinkedHashMap<String, Object> newEl = new LinkedHashMap<>();
            for (String elKey : ((LinkedHashMap<String, String>) el).keySet()) {
                if (!elKey.equalsIgnoreCase("iterator")) {
                    Object newObj = ((LinkedHashMap<String, String>) el).get(elKey);
                    if (newObj instanceof String) {
                        String newVal = (String) newObj;
                        for (String varName : ((HashMap<String, String>) iterElement).keySet())
                            newVal = newVal.replace(varName, (String) ((HashMap<String, String>) iterElement).get(varName));

                        newEl.put(elKey, newVal);
                    }
                    else
                        newEl.put(elKey, newObj);

                }
            }
            updateList.add(newEl);
        }
    }

    // parse Iterator
    private ArrayList<Object> parseIterator(String input) throws IgniteCheckedException {
        ArrayList<Object> output = new ArrayList<>();
        // Split into separated commands
        ArrayList<String> commands = new ArrayList<>(Arrays.asList(input.split(";")));
        if (commands.get(commands.size()-1).trim().equals(""))
            commands.remove(commands.size()-1);

        for (String command: commands) {
            // Process command range
            command = command.trim();
            boolean cmdFound = false;
            Pattern varFuncArgs = Pattern.compile("^(\\$[1-9]+) *= *([a-z]+) +([a-z0-9_,\' ]+)$", Pattern.CASE_INSENSITIVE);
            Matcher m = varFuncArgs.matcher(command);
            if (m.find()) {
                cmdFound = true;
                String varName = m.group(1);
                String subName = m.group(2);
                String funcArgsStr = m.group(3);
                ArrayList<String> funcArgs = new ArrayList<>(Arrays.asList(funcArgsStr.split(" *, *")));
                Long step = 1L;
                Long fromPos = 1L;
                Long toPos = 2L;
                if (subName.equals("range") || subName.equals("mrange")) {
                    fromPos = Long.parseLong(funcArgs.get(0));
                    toPos =  Long.parseLong(funcArgs.get(1));
                    if (funcArgs.size() > 2)
                        step = Long.parseLong(funcArgs.get(2));
                }
                if (output.size() == 0) {
                    for (long i = fromPos; i < toPos; i = i + step) {
                        HashMap<String, String> item = new HashMap<>();
                        item.put(varName, Long.toString(i));
                        output.add(item);
                    }
                }
                else {
                    if (subName.equals("mrange")) {
                        int idx = 0;
                        while (idx < output.size()) {
                            ArrayList<Object> tmp = new ArrayList<>();
                            for (long i=fromPos; i<toPos; i=i+step) {
                                HashMap<String, String> curItem =  new HashMap<>((HashMap<String, String>)output.get(idx));
                                curItem.put(varName, Long.toString(i));
                                tmp.add(curItem);
                            }
                            output.remove(idx);
                            for (int i=0; i < tmp.size(); i++)
                                output.add(idx + i, tmp.get(i));

                            idx = idx + tmp.size();
                        }
                    }
                }
            }
            varFuncArgs = Pattern.compile("^ *(\\$[1-9]+) *= *Class\\.([a-zA-Z0-9\\_]+) +(\\$[1-9_, ]+), *([a-zA-Z']+)", Pattern.CASE_INSENSITIVE);
            m = varFuncArgs.matcher(command);
            if (m.find()) {
                cmdFound = true;
                String varName = m.group(1);
                String methodName = m.group(2);
                String funcArg1 = m.group(3);
                String funcArg2 = m.group(4);
                for (Object el: output) {
                    ArrayList<String> funcArgs = new ArrayList<>(Arrays.asList(funcArg1, funcArg2));
                    for (int argIdx=0; argIdx<funcArgs.size(); argIdx++) {
                        String val = funcArgs.get(argIdx);
                        for (String vn: ((HashMap<String, String>) el).keySet())
                            val = val.replace(vn, ((HashMap<String, String>) el).get(vn));

                        funcArgs.set(argIdx, val);
                    }
                    //
                    if (methodName.equals("toSqlString")) {
                        if (funcArgs.size() < 2)
                            throw new IgniteCheckedException("Iterator syntax error: method " + methodName
                                    + " requires 2 arguments");
                        if (!funcArgs.get(1).startsWith("'") || !funcArgs.get(1).endsWith("'"))
                            throw new IgniteCheckedException("Iterator syntax error: method " + methodName
                                    + " requires string 2nd argument");
                        else {
                            DefaultTable obj = new DefaultTable(Long.parseLong(funcArgs.get(0)));
                            try {
                                String val = obj.toSqlString(funcArgs.get(1).substring(1, funcArgs.get(1).length()-1));
                                ((HashMap<String, String>) el).put(varName, val);
                                //System.out.println(val);
                            }
                            catch (IllegalAccessException e) {
                                System.out.println(">>> ERROR " + e.getMessage());
                            }

                        }
                    }
                    else
                        throw new IgniteCheckedException("Iterator syntax error: unknown Class method " + methodName);

                }

            }
            if (!cmdFound)
                throw new IgniteCheckedException("Iterator syntax error: unknown command " + command);


        }
        return output;
    }

    private void processStatements() {
        try (Ignite ignite = Ignition.start(igniteCfgFile)) {
            ArrayList<HashMap<String, Integer>> joinGroups = new ArrayList<>();
            int startGrp = 0;
            int endGrp = 0;

            for (HashMap<String, Object> stmtData: sqlStatements) {
                if (stmtData.containsKey("join")) {
                    HashMap<String, Integer> grpRange = new HashMap<>();
                    grpRange.put("start", startGrp);
                    grpRange.put("end", endGrp);
                    joinGroups.add(grpRange);
                    startGrp = endGrp + 1;
                }
                endGrp++;
            }
            HashMap<String, Integer> grpRange = new HashMap<>();
            grpRange.put("start", startGrp);
            grpRange.put("end", endGrp);
            joinGroups.add(grpRange);
            for (HashMap<String, Integer> joinGroup: joinGroups) {
                final int startIdx = joinGroup.get("start");
                final int endIdx = joinGroup.get("end");
                System.out.println(">>>Main>>> Processing statements " + startIdx + " .. " + endIdx);
                threads = new ArrayList<>();
                for (int threadId = 0; threadId < threadsNum; threadId++) {
                    final int offset = threadId;
                    threads.add(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            processThreadStatements(startIdx, endIdx, offset, ignite);
                        }
                    }));
                }
                for (Thread t : threads) {
                    try {
                        t.start();
                    } catch (IllegalThreadStateException e) {
                        System.out.println(">>> ERROR " + e.getMessage());
                    }
                }
                for (Thread t : threads) {
                    try {
                        t.join();
                    } catch (IllegalThreadStateException e) {
                        System.out.println(">>> ERROR " + e.getMessage());
                    } catch (InterruptedException e) {
                        System.out.println(">>> ERROR " + e.getMessage());
                    }
                }
            }
        }

    }


    private Boolean runMethodForSqlFieldsQuery(SqlFieldsQuery sql, String methodName, Object methodValue) {
        Boolean output = false;
        for (Method m: sql.getClass().getMethods()) {
            if (m.getName().equals(methodName)) {
                output = true;
                try {
                    m.invoke(sql, methodValue);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return output;
    }

    private HashMap<String, Object> parseAndExecIgniteCommand(Ignite ignite, String cmd, HashMap<String, Object> attr) throws IgniteException, IgniteCheckedException {
        HashMap<String, Object> r = new HashMap<>();
        ArrayList<String> igniteRows = new ArrayList<>();
        String cacheName = (String)attr.get(".cache_name");
        r.put("executed", false);
        Pattern ptn = Pattern.compile("^ *(ignite) +.+", Pattern.CASE_INSENSITIVE);
        Matcher m = ptn.matcher(cmd);
        if (m.find()) {
            ptn = Pattern.compile("^ *ignite +(put) +entry +([0-9, ]+) *$", Pattern.CASE_INSENSITIVE);
            m = ptn.matcher(cmd);
            if (m.find()) {
                r.put("executed", true);
                String[] cmdArgs = m.group(2).split(" *, *");
                Long k = Long.parseLong(cmdArgs[0]);
                Object v = new DefaultTable(k);
                IgniteCache<Long, Object> cache = ignite.cache(cacheName);
                cache.put(k, v);
            }
            ptn = Pattern.compile("^ *ignite +create +cache +like +([a-z0-9\\_]+) *$", Pattern.CASE_INSENSITIVE);
            m = ptn.matcher(cmd);
            if (m.find()) {
                r.put("executed", true);
                for (CacheConfiguration cacheCfg: ignite.configuration().getCacheConfiguration()) {
                    if (cacheCfg.getName().equalsIgnoreCase(m.group(1))) {
                        CacheConfiguration newCacheCfg = new CacheConfiguration(cacheCfg);
                        newCacheCfg.setName(cacheName);
                        IgniteCache<Object, Object> cache = ignite.createCache(newCacheCfg);
                        //System.out.println(cache.size());
                    }
                }
            }
            ptn = Pattern.compile("^ *ignite +drop +cache +if +exists *$", Pattern.CASE_INSENSITIVE);
            m = ptn.matcher(cmd);
            if (m.find()) {
                r.put("executed", true);
                ignite.destroyCache(cacheName);
            }
            ptn = Pattern.compile("^ *ignite +clear +cache *$", Pattern.CASE_INSENSITIVE);
            m = ptn.matcher(cmd);
            if (m.find()) {
                r.put("executed", true);
                ignite.cache(cacheName).clear();
            }
            ptn = Pattern.compile("^ *ignite +show +cache +size *$", Pattern.CASE_INSENSITIVE);
            m = ptn.matcher(cmd);
            if (m.find()) {
                r.put("executed", true);
                IgniteCache<Long, Object> cache = ignite.cache(cacheName);
                if (cache != null)
                    igniteRows.add("[" + Integer.toString(cache.size()) + "]");
                else
                    igniteRows.add("ERROR: cache is null");
            }
            if (!(Boolean)r.get("executed"))
                throw new IgniteCheckedException("Ignite syntax error: unknown command " + cmd);

        }
        if (igniteSqlNativeMode) {
            r.put("executed", true);
            if (cmd.startsWith("SELECT")) {
                //System.out.println(cmd + " for " + attr.get(".cache_name"));
                try {
                    IgniteCache<Long, Object> cache = ignite.cache((String)attr.get(".cache_name"));
                    SqlFieldsQuery sql = new SqlFieldsQuery(cmd);
                    if (attr.containsKey(".args")) {
                        //sql.setArgs(sqlArgs);
                    }
                    for (String key: attr.keySet()) {
                        if (key.startsWith(".")) {
                            runMethodForSqlFieldsQuery(sql, key.substring(1), attr.get(key));
                        }
                    }
                    try (QueryCursor<List<?>> cursor = cache.query(sql)) {
                        for (List<?> row : cursor) {
                            String colData = "[";
                            for (int colIdx = 0; colIdx < row.size(); colIdx++) {
                                if (row.get(colIdx) != null)
                                    if (row.get(colIdx) instanceof Boolean)
                                        colData += row.get(colIdx).toString().toUpperCase() + ", ";
                                    else
                                        colData += row.get(colIdx).toString() + ", ";
                                else
                                    colData += "NULL, ";
                            }
                            colData = colData.substring(0, colData.length() - 2) + "]";
                            igniteRows.add(colData);
                            //System.out.println(">>> " + colData);
                        }
                    }
                }
                catch (IgniteException e) {
                    exitCode = 1;
                    e.printStackTrace();
                }
            }
        }
        r.put("result_set", igniteRows);
        return r;
    }

    private void printList(String prefix, ArrayList<String> input) {
        for (String el: input) {
            System.out.println(">>>" + prefix + ">>> " + el);
        }
    }

    private void printMap(String prefix, HashMap<String, Object> input) {
        System.out.println(">>>" + prefix + ">>> id: " + input.get("id"));
        for (String dialect: dialects) {
            if (input.containsKey(dialect)) {
                System.out.println(">>>" + prefix + ">>> " + dialect + ": " + input.get(dialect));
            }
        }
        for (String key: input.keySet()) {
            Object val = input.get(key);
            if (!dialects.contains(key) && !key.startsWith(".") && !key.equalsIgnoreCase("id")) {
                if (val instanceof String)
                    System.out.println(">>>" + prefix + ">>> " + key + ": " + val);
                else if (val instanceof Boolean) {
                    System.out.println(">>>" + prefix + ">>> " + key + ": " + ((Boolean) val).toString());
                }
            }
        }
        for (String key: input.keySet()) {
            if (key.startsWith(".")) {
                Object val = input.get(key);
                System.out.println(">>>" + prefix + ">>> " + key + ": " + val);
            }
        }
    }
    private void processThreadStatements(int startIdx, int endIdx, int offset, Ignite ignite) {
        Object[] stmts = sqlStatements.toArray();
        Connection conn = null;
        long startedTime = System.currentTimeMillis();
        String threadInfo = "Thread_" + Integer.toString(offset);
        System.out.println(">>>" + threadInfo + ">>> started");
        long stmtCnt = 0;
        String refName = "h2";
        try {
            if (jdbcRefConn.contains("h2")) {
                Class.forName("org.h2.Driver");
                conn = DriverManager.getConnection(jdbcRefConn, jdbcRefUser, jdbcRefPwd);
                System.out.println(">>>" + threadInfo + ">>> reference URL " + conn.toString());
                refName = "h2";
            }
            if (conn != null) {
                for (int i = startIdx + offset; i < endIdx; i = i + threadsNum) {
                    long iterFailedStmtCnt = 0L;
                    HashMap<String, Object> hm = (HashMap<String, Object>)stmts[i];
                    //System.out.println(threadInfo + hm.get("ref"));
                    ArrayList<String> refRows = new ArrayList<>();
                    ArrayList<String> ignRows = new ArrayList<>();
                    String refQuery = (String)hm.get(refName);
                    String ignQuery = (String)hm.get("ignite");
                    try {
                        // Reference statement
                        Statement stmt = conn.createStatement();
                        if (!refQuery.equalsIgnoreCase("pass")) {
                            if (refQuery.startsWith("SELECT")) {
                                ResultSet rs = stmt.executeQuery(refQuery);
                                ResultSetMetaData meta = rs.getMetaData();
                                int colCount = meta.getColumnCount();
                                while (rs.next()) {
                                    String row = "";
                                    for (int colIdx = 1; colIdx <= colCount; colIdx++) {
                                        if (rs.getString(colIdx) != null)
                                            row += rs.getString(colIdx) + ", ";
                                        else
                                            row += "NULL, ";
                                    }
                                    refRows.add("[" + row.substring(0, row.length() - 2) + "]");
                                }
                                rs.close();
                                // Print warnings for empty result sets
                                if (forceFailOptions.contains("empty-result-for-select") && (refRows.size() == 0)) {
                                    System.out.println(">>>" + threadInfo + ">>> WARNING: empty result for select");
                                    printMap(threadInfo, hm);
                                    if (exitCode == 0)
                                        exitCode = 2;
                                    iterFailedStmtCnt++;

                                }
                            } else {
                                stmt.execute(refQuery);
                            }
                            stmt.close();
                        }
                        stmtCnt++;
                    } catch (Exception e) {
                        exitCode = 1;
                        iterFailedStmtCnt++;
                        System.out.println(">>>" + threadInfo + ">>> ERROR exception:");
                        printMap(threadInfo, hm);
                        e.printStackTrace();
                    }
                    try {
                        // Trying to execute internal command for Ignite to support the native way
                        if (!ignQuery.equalsIgnoreCase("pass")) {
                            //System.out.println(threadInfo + hm.get("ignite"));
                            HashMap<String, Object> result = parseAndExecIgniteCommand(ignite, ignQuery, hm);
                            if ((boolean) result.get("executed")) {
                                if (result.containsKey("result_set")) {
                                    ignRows = (ArrayList<String>) result.get("result_set");
                                }
                            }
                        }
                    } catch (Exception e) {
                        exitCode = 1;
                        iterFailedStmtCnt++;
                        System.out.println(">>>" + threadInfo + ">>> ERROR exception:");
                        printMap(threadInfo, hm);
                        e.printStackTrace();
                    }
                    String printedId = "";
                    if (refQuery.startsWith("SELECT") && (ignQuery.startsWith("SELECT") || ignQuery.startsWith("IGNITE SHOW"))) {
                        if (hm.containsKey("sort")) {
                            //System.out.println(refRows);
                            //System.out.println(ignRows);
                            Collections.sort(refRows, String.CASE_INSENSITIVE_ORDER);
                            Collections.sort(ignRows, String.CASE_INSENSITIVE_ORDER);
                        }
                        Patch<String> patch = DiffUtils.diff(refRows, ignRows);
                        if (patch.getDeltas().size() > 0) {
                            printedId = (String)hm.get("id");
                            exitCode =1;
                            System.out.println(">>>" + threadInfo + ">>> Diff found:");
                            printMap(threadInfo, hm);
                            for (Delta<String> delta : patch.getDeltas()) {
                                System.out.println(">>>" + threadInfo + ">>> " + delta);
                            }
                            System.out.println(">>>" + threadInfo + ">>> Reference dump");
                            printList(threadInfo, refRows);
                            System.out.println(">>>" + threadInfo + ">>> Ignite dump");
                            printList(threadInfo, ignRows);
                            System.out.println(">>>" + threadInfo + ">>>");
                            iterFailedStmtCnt++;
                        }
                    }
                    if ((printParticularStatement.contains(hm.get("id")) || printParticularStatement.contains("*"))
                            && printedId.equals("")) {
                        System.out.println(">>>" + threadInfo + ">>> print statement " + hm.get("id") + ":");
                        printMap(threadInfo, hm);
                        System.out.println(">>>" + threadInfo + ">>> Reference dump");
                        printList(threadInfo, refRows);
                        System.out.println(">>>" + threadInfo + ">>> Ignite dump");
                        printList(threadInfo, ignRows);

                    }
                    if (iterFailedStmtCnt == 0)
                        passedStmtCounter.incrementAndGet();
                    else
                        failedStmtCounter.incrementAndGet();
                }
                conn.close();
            }
            else {
                System.out.println(">>>" + threadInfo + ">>> ERROR (thread " + Integer.toString(offset) + "): connection is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long execTime = System.currentTimeMillis() - startedTime;
        System.out.println(">>>" + threadInfo + ">>> finished. " + stmtCnt + " statement(s) proceed in " + execTime + "ms");
    }

}