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

package org.apache.ignite.yardstick.cisco;

import java.util.HashMap;
import java.util.Map;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.yardstick.cache.IgniteCacheAbstractBenchmark;
import org.yardstickframework.BenchmarkConfiguration;

import static org.yardstickframework.BenchmarkUtils.println;

/**
 *
 */
public class IgniteBenchmarkCiscoQuery extends IgniteCacheAbstractBenchmark {
    /** Map file on classes. */
    private Map<String, Class> maps = new HashMap<String, Class>() {{
        put("cpr_user_info_vw.txt", CprUserInfoVw.class);
        put("ds2_brm_master.txt", Ds2BrmMaster.class);
        put("dw_ua_url.txt", DwUaUrl.class);
        put("ua_parsed_attrs.txt", UaParsedAttrs.class);
        put("unqvstr_rvrs_ip_rep.txt", UnqvstrRvrsIpRep.class);
        put("wbx_telephony_participant_f.txt", WbxTelephonyParticipantF.class);
        put("web_data_uri_sessionized_2.txt", WebDataUriSessionized.class);
        put("XXRPT_HGSMEETINGREPORT.txt", XxrptHgsMeetingReport.class);
        put("XXRPT_HGSMEETINGUSERREPORT.txt", XxrptHgsMeetingUserReport.class);
    }};

    /** */
    private static String NQ1 =
        "SELECT " +
        "  s.key,s.ip,s.userid,s.dattime,s.useragent,s.calltype,s.url,s.referrerUrl,s.dwelltime " +
        "FROM \"web_data_uri_sessionized_2\".WEBDATAURISESSIONIZED s " +
        "WHERE s.dwelltime BETWEEN '2016-01-01' AND '2016-12-12' " +
        "LIMIT 99999";

    /** */
    private static String NQ2 =
        "SELECT " +
        " dwelltime, count(key) AS `events` " +
        "FROM \"web_data_uri_sessionized_2\".WEBDATAURISESSIONIZED " +
        "WHERE dwelltime BETWEEN '2016-01-01' AND '2016-12-12' " +
        "  AND calltype = 'pg' " +
        "  AND url = '//www.cisco.com/go/license' " +
        "GROUP BY dwelltime";

    /** */
    private static String NQ3 =
        "SELECT " +
        "  cg.arealevel5, cg.arealevel6, count(web.key) Views, count(distinct web.sessionid) Visits, month(web.dwelltime), year(web.dwelltime) " +
        "FROM \"web_data_uri_sessionized_2\".WEBDATAURISESSIONIZED web " +
        "INNER JOIN \"dw_ua_url\".DWUAURL cg " +
        "ON web.url = cg.url " +
        "  AND calltype = 'pg' " +
        "  AND cg.arealevel4 = 'Tools' " +
        "  AND web.dwelltime BETWEEN '2016-01-01' AND '2016-12-31' " +
        "GROUP BY cg.arealevel5, cg.arealevel6, month(web.dwelltime), year(web.dwelltime)";

    /** */
    private static String NQ4 =
        "SELECT " +
        "  ua.browsername as Browser, max(web.dattime) as Lastvisit, count(web.key) as Views, count(distinct web.SessionId) as Visits, month(web.dwelltime) " +
        "FROM \"web_data_uri_sessionized_2\".WEBDATAURISESSIONIZED web  " +
        "LEFT JOIN \"ua_parsed_attrs\".UAPARSEDATTRS ua " +
        "  ON web.useragent = ua.useragent " +
        "LEFT JOIN \"dw_ua_url\".DWUAURL mdf  " +
        "  ON web.url=mdf.url  " +
        "WHERE  ua.browsername IS NOT NULL   " +
        "  AND ua.browsername NOT IN ('-') " +
        "  AND mdf.arealevel4 = 'Tools' " +
        "  AND web.dwelltime BETWEEN '2016-01-01' AND '2016-12-31' " +
        "  AND web.calltype = 'pg' " +
        "GROUP BY ua.browsername, month(web.dwelltime)";

    /** */
    private static String NQ5 =
        "SELECT " +
        "  ua.osname as Platform, max(web.DWELLTIME) as Lastvisit, count(web.key) as Views, count(distinct web.Sessionid) as Visits, month(web.DWELLTIME), year(web.DWELLTIME) " +
        "FROM \"web_data_uri_sessionized_2\".WEBDATAURISESSIONIZED  web " +
        "LEFT JOIN \"ua_parsed_attrs\".UAPARSEDATTRS ua " +
        "  ON web.useragent = ua.useragent " +
        "LEFT JOIN \"dw_ua_url\".DWUAURL mdf " +
        "  ON web.url=mdf.url " +
        "WHERE ua.osname IS NOT NULL " +
        "  AND ua.osname NOT IN ('-') " +
        "  AND mdf.arealevel4 = 'TechSupport' " +
        "  AND web.DWELLTIME BETWEEN '2016-08-01' AND '2016-08-31' " +
        "  AND web.calltype = 'pg' " +
        "GROUP BY ua.osname, month(web.DWELLTIME), year(web.DWELLTIME);";

    /** Query. */
    private String query;

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        query();

        println(cfg, "Populating query data...");

        long start = System.nanoTime();

        for (Map.Entry<String, Class> e : maps.entrySet())
            populateCacheFromCsv(e.getKey(), e.getValue());

        println(cfg, "Finished populating query data in " + ((System.nanoTime() - start) / 1_000_000) + " ms.");
        println(cfg, "Will benchmark query: " + query);
    }

    /**
     * Init query.
     */
    private void query() {
        switch (args.queryNumber()) {
            case 1:
                query = NQ1;
                break;

            case 2:
                query = NQ2;
                break;

            case 3:
                query = NQ3;
                break;

            case 4:
                query = NQ4;
                break;

            case 5:
                query = NQ5;
                break;

            default:
                throw new IllegalArgumentException("Invalid query number: " + args.queryNumber());
        }
    }

    /**
     * @param fileName File name.
     * @param clazz Class.
     * @throws Exception If failed.
     */
    private void populateCacheFromCsv(String fileName, Class clazz) throws Exception {
        try (IgniteDataStreamer<Integer, Object> dataLdr = ignite().dataStreamer(cacheName(fileName));
            CsvImporter imp = new CsvImporter(fileName, clazz)) {
            Object o;
            int keyGen = 0;

            while (!Thread.currentThread().isInterrupted() && ((o = imp.readObject()) != null)) {
                dataLdr.addData(++keyGen, o);

                if (keyGen % 100 == 0)
                    println(cfg, "Populated " + clazz.getSimpleName() + ": " + keyGen);
            }
        }
    }

    /**
     * @param fileName File name.
     * @return Cache name.
     */
    private String cacheName(String fileName) {
        return fileName.replace(".txt", "");
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        SqlFieldsQuery qry = new SqlFieldsQuery(query);

        cache.query(qry).getAll();

        return true;
    }

    /** {@inheritDoc} */
    @Override protected IgniteCache cache() {
        return ignite().cache("cpr_user_info_vw");
    }

    /**
     * @param args Arguments.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        try (Ignite ignore = Ignition.start("D:\\projects\\incubator-ignite\\modules\\yardstick\\" +
            "config\\ignite-localhost-config.xml")) {

            IgniteBenchmarkCiscoQuery b = new IgniteBenchmarkCiscoQuery();

            BenchmarkConfiguration cfg = new BenchmarkConfiguration();

            cfg.output(System.out);
            cfg.error(System.err);

            cfg.commandLineArguments(new String[] {"-r", "10", "-q", "5"});

            b.setUp(cfg);

//            TimeUnit.HOURS.sleep(1);

            b.test(null);
        }
    }
}
