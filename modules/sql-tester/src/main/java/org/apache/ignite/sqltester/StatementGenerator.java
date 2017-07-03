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





}