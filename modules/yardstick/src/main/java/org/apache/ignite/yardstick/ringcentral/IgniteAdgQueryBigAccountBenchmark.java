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

package org.apache.ignite.yardstick.ringcentral;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.yardstickframework.BenchmarkConfiguration;

/**
 *
 */
public class IgniteAdgQueryBigAccountBenchmark extends IgniteAdgQueryAbstractBenchmark {
    /** */
    private static String firstQry = "select extensionId, " +
        "min(extensionStatus), " +
        "max(concat(firstName,' ',lastName)), " +
        "min(phoneNumber), " +
        "max(extensionNumber) " +
        "from \"AdgEntry\".ADGENTITY " +
        "where accountId = ? " +
        "and extensionType in (?,?) " +
        "and extensionStatus in (?,?) " +
        "and deleteTime > ? " +
        "and ((lcase(concat(firstName,' ',lastName)) like '%john%') " +
        "OR (lcase(phoneNumber) like '%john%') " +
        "OR (lcase(extensionNumber) like '%john%')) " +
        "group by extensionId  order by 2 asc, 3 desc, 4 asc, 5 desc limit 100 offset 0";

    /** */
    private static String secondQry = "select _val FROM \"AdgEntry\".ADGENTITY " +
        "join table(temp_extensionId VARCHAR = ?) i " +
        "on (extensionId = i.temp_extensionId and deleteTime > ?)";

    /** */
    private static String thirdQry = "select count(distinct extensionId) from \"AdgEntry\".ADGENTITY " +
        "where accountId = ? " +
        "and extensionType in (?,?) " +
        "and extensionStatus in (?,?) " +
        "and deleteTime > ? " +
        "and ((lcase(concat(firstName,' ',lastName)) like '%john%') " +
        "or (lcase(phoneNumber) like '%john%') " +
        "or (lcase(extensionNumber) like '%john%'))";

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        SqlFieldsQuery qry = new SqlFieldsQuery(firstQry);

        qry.setArgs((Object[])argumentForQuery());

        List<List<?>> all = cache.query(qry).getAll();

        if (!all.isEmpty()) {
            Set<String> extIds = new HashSet<>();

            for (List<?> fields : all)
                extIds.add((String)fields.get(0));

            qry = new SqlFieldsQuery(secondQry).setArgs((Object)extIds.toArray(), System.currentTimeMillis());

            cache.query(qry).getAll();

            qry = new SqlFieldsQuery(thirdQry).setArgs((Object[])argumentForQuery());

            cache.query(qry).getAll();
        }

        return true;
    }

    /**
     * @return Argument for first query.
     */
    protected String[] argumentForQuery() {
        return new String[]{
            "10k",
            AdgEntity.ExtensionType.randomValue(),
            AdgEntity.ExtensionType.randomValue(),
            AdgEntity.ExtensionState.randomValue(),
            AdgEntity.ExtensionState.randomValue(),
            String.valueOf(System.currentTimeMillis())
        };
    }

    /**
     * @param args Arguments.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        try (Ignite ignore =
            Ignition.start("D:\\projects\\incubator-ignite\\modules\\yardstick\\config\\ignite-localhost-config.xml")) {

            IgniteAdgQueryBigAccountBenchmark b = new IgniteAdgQueryBigAccountBenchmark();

            BenchmarkConfiguration cfg = new BenchmarkConfiguration();

            cfg.output(System.out);
            cfg.error(System.err);

            cfg.commandLineArguments(new String[] {"-r", "10"});

            b.setUp(cfg);

            //TimeUnit.HOURS.sleep(1);

            b.test(null);
        }
    }
}
