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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Random;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.jsr166.ThreadLocalRandom8;

/**
 *
 */
public class ZipEntity {
    static final String ALPHABETH = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890_";

    public static final int RND_STRING_LEN = 24;

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
    public String ESMP;
    public String FOAGGRCODE;
    public String HOSTPRODID;
    public String INSTRUMENTEXPIRYDATE;
    public String INSTRUMENTMATURITYDATE;
    public String INSTRUMENTTYPE;
    public String ISIN;
    public String PROXYINSTRUMENTID;
    public String PROXYINSTRUMENTIDTYPE;
    public String PROXYINSTRUMENTTYPE;
    public Double QUANTITY;
    public String REGION;
    public String RIC;
    public String RISKFACTORNAME;
    public String RISKPARENTINSTRUMENTID;
    public String RISKPARENTINSTRUMENTIDTYPE;
    public String RISKSOURCESYSTEM;
    public String RISKSUBJECTCHORUSBOOKID;
    @QuerySqlField(index = true)
    public String RISKSUBJECTID;
    public String RISKSUBJECTINSTRUMENTCOUNTERPARTYID;
    public String RISKSUBJECTINSTRUMENTID;
    public String RISKSUBJECTINSTRUMENTIDTYPE;
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

    public static ZipEntity generate() {
        ZipEntity entity = new ZipEntity();

        ThreadLocalRandom8 rnd = ThreadLocalRandom8.current();

        try {
            for (Field fld : ZipEntity.class.getFields()) {
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

    public static ZipEntity generateHard(double rndCoef, int len) {
        ZipEntity entity = new ZipEntity();

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
        entity.ESMP = strs[i++ % strings];
        entity.FOAGGRCODE = strs[i++ % strings];
        entity.HOSTPRODID = strs[i++ % strings];
        entity.INSTRUMENTEXPIRYDATE = strs[i++ % strings];
        entity.INSTRUMENTMATURITYDATE = strs[i++ % strings];
        entity.INSTRUMENTTYPE = strs[i++ % strings];
        entity.ISIN = strs[i++ % strings];
        entity.PROXYINSTRUMENTID = strs[i++ % strings];
        entity.PROXYINSTRUMENTIDTYPE = strs[i++ % strings];
        entity.PROXYINSTRUMENTTYPE = strs[i++ % strings];
        entity.QUANTITY = rnd.nextDouble();
        entity.REGION = strs[i++ % strings];
        entity.RIC = strs[i++ % strings];
        entity.RISKFACTORNAME = strs[i++ % strings];
        entity.RISKPARENTINSTRUMENTID = strs[i++ % strings];
        entity.RISKPARENTINSTRUMENTIDTYPE = strs[i++ % strings];
        entity.RISKSOURCESYSTEM = strs[i++ % strings];
        entity.RISKSUBJECTCHORUSBOOKID = strs[i++ % strings];
        entity.RISKSUBJECTID = strs[i++ % strings];
        entity.RISKSUBJECTINSTRUMENTCOUNTERPARTYID = strs[i++ % strings];
        entity.RISKSUBJECTINSTRUMENTID = strs[i++ % strings];
        entity.RISKSUBJECTINSTRUMENTIDTYPE = strs[i++ % strings];
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

    public static void notIndexedDataHard(ZipEntity entity, OutputStream out) throws IOException {
        out.write(entity.ACCOUNTCODE.getBytes());
        out.write(entity.ASSETTYPE.getBytes());
        out.write(entity.ASSETUNIT.getBytes());
        out.write(entity.ATLASFOLDERID.getBytes());
        out.write(entity.ATLASINSTRUMENTSTRUCTUREPATH.getBytes());
        out.write(entity.BOOKSOURCESYSTEM.getBytes());
        out.write(entity.BOOKSOURCESYSTEMCODE.getBytes());
        out.write(entity.CUSIP.getBytes());
        out.write(entity.DATASETFILTER.getBytes());
        out.write(entity.DATASETLABEL.getBytes());
        out.write(toByteArray(entity.EODTOTALVALUE));
        out.write(entity.ESMP.getBytes());
        out.write(entity.FOAGGRCODE.getBytes());
        out.write(entity.HOSTPRODID.getBytes());
        out.write(entity.INSTRUMENTEXPIRYDATE.getBytes());
        out.write(entity.INSTRUMENTMATURITYDATE.getBytes());
        out.write(entity.INSTRUMENTTYPE.getBytes());
        out.write(entity.ISIN.getBytes());
        out.write(entity.PROXYINSTRUMENTID.getBytes());
        out.write(entity.PROXYINSTRUMENTIDTYPE.getBytes());
        out.write(entity.PROXYINSTRUMENTTYPE.getBytes());
        out.write(toByteArray(entity.QUANTITY));
        out.write(entity.REGION.getBytes());
        out.write(entity.RIC.getBytes());
        out.write(entity.RISKFACTORNAME.getBytes());
        out.write(entity.RISKPARENTINSTRUMENTID.getBytes());
        out.write(entity.RISKPARENTINSTRUMENTIDTYPE.getBytes());
        out.write(entity.RISKSOURCESYSTEM.getBytes());
        out.write(entity.RISKSUBJECTCHORUSBOOKID.getBytes());
        out.write(entity.RISKSUBJECTINSTRUMENTCOUNTERPARTYID.getBytes());
        out.write(entity.RISKSUBJECTINSTRUMENTID.getBytes());
        out.write(entity.RISKSUBJECTINSTRUMENTIDTYPE.getBytes());
        out.write(entity.RISKSUBJECTSOURCE.getBytes());
        out.write(entity.RISKSUBJECTTYPE.getBytes());
        out.write(entity.SENSITIVITYTYPE.getBytes());
        out.write(entity.SERIESDAY.getBytes());
        out.write(toByteArray(entity.STRIKEVALUE));
        out.write(entity.SYS_AUDIT_TRACE.getBytes());
        out.write(toByteArray(entity.THEOPRICE));
        out.write(toByteArray(entity.TOTALVALUE));
        out.write(entity.UNDERLYINGSECURITYID.getBytes());
        out.write(entity.UNDERLYINGSECURITYIDTYPE.getBytes());
        out.write(entity.VALUATIONSOURCECONTEXTLABELNAME.getBytes());
        out.write(toByteArray(entity.VALUE));
    }

    public static byte[] notIndexedData(ZipEntity entity) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);

        try {
            for (Field fld : ZipEntity.class.getFields()) {
                if (fld.getAnnotation(QuerySqlField.class) == null) {
                    if (fld.getType() == String.class)
                        bout.write(((String)fld.get(entity)).getBytes());
                    else if (fld.getType() == Double.class)
                        bout.write(toByteArray((Double)fld.get(entity)));
                }
            }
        }
        catch (Exception e) {
            throw new IgniteException(e);
        }

        return bout.toByteArray();
    }

    private static byte[] toByteArray(double d) {
        long l = Double.doubleToRawLongBits(d);
        return new byte[] {
            (byte)((l >> 56) & 0xff),
            (byte)((l >> 48) & 0xff),
            (byte)((l >> 40) & 0xff),
            (byte)((l >> 32) & 0xff),
            (byte)((l >> 24) & 0xff),
            (byte)((l >> 16) & 0xff),
            (byte)((l >> 8) & 0xff),
            (byte)((l >> 0) & 0xff),
        };
    }

    static String randomString(Random rnd, int len) {
        StringBuilder b = new StringBuilder(len);

        for (int i = 0; i < len; i++)
            b.append(ALPHABETH.charAt(rnd.nextInt(ALPHABETH.length())));

        return b.toString();
    }

    public static String generateRestString() {
        return randomString(ThreadLocalRandom8.current(), RND_STRING_LEN * 44);
    }
}
