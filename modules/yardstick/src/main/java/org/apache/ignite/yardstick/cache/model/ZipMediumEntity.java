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

package org.apache.ignite.yardstick.cache.model;

import java.lang.reflect.Field;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.jsr166.ThreadLocalRandom8;

import static org.apache.ignite.yardstick.cache.model.ZipEntity.randomString;

/**
 *
 */
public class ZipMediumEntity {
    public static final int RND_STRING_LEN = ZipEntity.RND_STRING_LEN;

    public String ACCOUNTCODE;
    public String ASSETTYPE;
    public String ASSETUNIT;
    public String ATLASFOLDERID;
    public String ATLASINSTRUMENTSTRUCTUREPATH;
    public String BOOKSOURCESYSTEM;
    public String BOOKSOURCESYSTEMCODE;
    @QuerySqlField(index = true)
    public String BUSINESSDATE;
    public String CUSIP;
    public String DATASETFILTER;
    public String DATASETLABEL;
    public Double EODTOTALVALUE;
    public String RISKSUBJECTCHORUSBOOKID;
    @QuerySqlField(index = true)
    public String RISKSUBJECTID;
    public String RISKSUBJECTINSTRUMENTCOUNTERPARTYID;
    public String RISKSUBJECTSOURCE;
    public String RISKSUBJECTTYPE;
    public String SENSITIVITYTYPE;
    @QuerySqlField(index = true)
    public String SERIESDATE;
    public String SERIESDAY;
    @QuerySqlField(index = true)
    public String SNAPVERSION;
    public Double STRIKEVALUE;
    public String SYS_AUDIT_TRACE;
    public Double THEOPRICE;
    public Double TOTALVALUE;
    public String UNDERLYINGSECURITYID;
    public String UNDERLYINGSECURITYIDTYPE;
    public String VALUATIONSOURCECONTEXTLABELNAME;
    public Double VALUE;
    @QuerySqlField(index = true)
    public String VARTYPE;

    public static ZipMediumEntity generate() {
        ZipMediumEntity entity = new ZipMediumEntity();

        ThreadLocalRandom8 rnd = ThreadLocalRandom8.current();

        try {
            for (Field fld : ZipMediumEntity.class.getFields()) {
                if (fld.getType() == String.class)
                    fld.set(entity, randomString(rnd, RND_STRING_LEN));
                else if (fld.getType() == Double.class)
                    fld.set(entity, rnd.nextDouble());
            }
        }
        catch (ReflectiveOperationException e) {
            throw new IgniteException(e);
        }

        return entity;
    }

    public static ZipMediumEntity generateHard(double rndCoef, int len) {
        ZipMediumEntity entity = new ZipMediumEntity();

        ThreadLocalRandom8 rnd = ThreadLocalRandom8.current();

        int strings = (int)(rndCoef * 38);

        String[] strs = new String[strings];

        for (int i = 0; i < strings; i++)
            strs[i] = randomString(rnd, len);

        int i = 0;

        entity.ACCOUNTCODE = strs[i++ % strings];
        entity.ASSETTYPE = strs[i++ % strings];
        entity.ASSETUNIT = strs[i++ % strings];
        entity.ATLASFOLDERID = strs[i++ % strings];
        entity.ATLASINSTRUMENTSTRUCTUREPATH = strs[i++ % strings];
        entity.BOOKSOURCESYSTEM = strs[i++ % strings];
        entity.BOOKSOURCESYSTEMCODE = strs[i++ % strings];
        entity.BUSINESSDATE = strs[i++ % strings];
        entity.CUSIP = strs[i++ % strings];
        entity.DATASETFILTER = strs[i++ % strings];
        entity.DATASETLABEL = strs[i++ % strings];
        entity.EODTOTALVALUE = rnd.nextDouble();
        entity.RISKSUBJECTCHORUSBOOKID = strs[i++ % strings];
        entity.RISKSUBJECTID = strs[i++ % strings];
        entity.RISKSUBJECTINSTRUMENTCOUNTERPARTYID = strs[i++ % strings];
        entity.RISKSUBJECTSOURCE = strs[i++ % strings];
        entity.RISKSUBJECTTYPE = strs[i++ % strings];
        entity.SENSITIVITYTYPE = strs[i++ % strings];
        entity.SERIESDATE = strs[i++ % strings];
        entity.SERIESDAY = strs[i++ % strings];
        entity.SNAPVERSION = strs[i++ % strings];
        entity.STRIKEVALUE = rnd.nextDouble();
        entity.SYS_AUDIT_TRACE = strs[i++ % strings];
        entity.THEOPRICE = rnd.nextDouble();
        entity.TOTALVALUE = rnd.nextDouble();
        entity.UNDERLYINGSECURITYID = strs[i++ % strings];
        entity.UNDERLYINGSECURITYIDTYPE = strs[i++ % strings];
        entity.VALUATIONSOURCECONTEXTLABELNAME = strs[i++ % strings];
        entity.VALUE = rnd.nextDouble();
        entity.VARTYPE = strs[i++ % strings];

        return entity;
    }
}
