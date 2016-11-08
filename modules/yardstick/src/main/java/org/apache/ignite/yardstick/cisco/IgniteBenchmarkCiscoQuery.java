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
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.yardstick.IgniteAbstractBenchmark;
import org.yardstickframework.BenchmarkConfiguration;

import static org.yardstickframework.BenchmarkUtils.println;

/**
 *
 */
public class IgniteBenchmarkCiscoQuery extends IgniteAbstractBenchmark {
    /** Map file on classes. */
    private Map<String, Class> maps = new HashMap<String, Class>() {{
        put("cpr_user_info_vw.txt", CprUserInfoVw.class);
        put("ds2_brm_master.txt", Ds2BrmMaster.class);
        put("dw_ua_url.txt", DwUaUrl.class);
        put("unqvstr_rvrs_ip_rep.txt", UnqvstrRvrsIpRep.class);
        put("wbx_telephony_participant_f.txt", WbxTelephonyParticipantF.class);
        put("web_data_uri_sessionized_2.txt", WebDataUriSessionized.class);
        put("XXRPT_HGSMEETINGREPORT.txt", XxrptHgsMeetingReport.class);
        put("XXRPT_HGSMEETINGUSERREPORT.txt", XxrptHgsMeetingUserReport.class);
    }};

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        println(cfg, "Populating query data...");

        long start = System.nanoTime();

        for (Map.Entry<String, Class> e : maps.entrySet())
            populateCacheFromCsv(e.getKey(), e.getValue());

        println(cfg, "Finished populating query data in " + ((System.nanoTime() - start) / 1_000_000) + " ms.");
    }

    /**
     * @param fileName File name.
     * @param clazz Class.
     * @throws Exception If failed.
     */
    private void populateCacheFromCsv(String fileName, Class clazz) throws Exception {
        try (IgniteDataStreamer<Integer, Object> dataLdr = ignite().dataStreamer(fileName);
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

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        return false;
    }
}
