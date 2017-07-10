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

public class StatementGeneratorNew {
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

    public StatementGeneratorNew(Properties props){
        sqlCfgFile = props.getProperty("sqlCfgFile");
        igniteCfgFile = props.getProperty("igniteCfgFile");
        workDir = props.getProperty("workDir");
        filePrefix = props.getProperty("filePrefix");

    }


    public CopyOnWriteArrayList<HashMap<String, Object>> generate(String[] args) throws IgniteException, IgniteCheckedException {

        // Read YAML SQL combination file
        sqlCombinations = readYaml(sqlCfgFile);

        sqlStatements = generateSqlStatements(sqlCombinations);

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

        System.out.println(">>> The result: " + passedStmtCounter + "/" + sqlStatements.size() +
                " statements passed (" + failedStmtCounter + " failed, " + (sqlStatements.size() - failedStmtCounter.get() - passedStmtCounter.get()) + " skipped)");

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

        CopyOnWriteArrayList<HashMap<String, Object>> result = new CopyOnWriteArrayList<>();

        return result;
    }



}