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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Date;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.springframework.core.annotation.Order;

/**
 *
 */
public class CsvImporter<T> implements AutoCloseable {
    /** File name. */
    private final String fileName;

    /** Entry class. */
    private final Class clazz;

    /** File. */
    private InputStream is;

    private BufferedReader br;

    public T readObject() throws Exception {
        if (is == null) {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            is = classloader.getResourceAsStream(fileName);

            InputStreamReader isr = new InputStreamReader(is);
            br = new BufferedReader(isr, 1);
        }

        String s = br.readLine();

        if (s == null) {
            is.close();

            return null;
        }

        return (T)parseLine(s, clazz);
    }

    /** {@inheritDoc} */
    public void close() throws Exception {
        is.close();
    }

    public CsvImporter(String fileName, Class clazz) {
        this.fileName = fileName;
        this.clazz = clazz;
    }

    private Object parseLine(String line, Class clazz) throws Exception {
        if (line == null || line.isEmpty())
            return null;

        String[] vals = line.split("\u0001");

        Object o = clazz.newInstance();

        Field[] fields = clazz.getDeclaredFields();

        for (Field f : fields) {
            for (Annotation a : f.getDeclaredAnnotations()) {
                if (a instanceof Order) {
                    int order = ((Order)a).value();

                    Class<?> type = f.getType();

                    Object val = convertValue(type, vals[order]);

                    f.setAccessible(true);
                    f.set(o, val);

                    break;
                }
            }
        }

        return o;
    }

    private Object convertValue(Class clazz, String val) throws Exception {
        if (val.equals("\\N"))
            return null;

        if (clazz.equals(String.class))
            return val;
        else if (clazz.equals(Long.class))
            return Long.valueOf(val);
        else if (clazz.equals(Integer.class))
            return Integer.valueOf(val);
        else if (clazz.equals(Double.class))
            return Double.valueOf(val);
        else if (clazz.equals(Date.class))
            return parseDate(val);
        else
            throw new IllegalArgumentException("Unsupported type: " + clazz + ", val=" + val);
    }

    private Date parseDate(String val) throws java.text.ParseException {
        try {
            return U.parse(val, "yyyy-mm-dd");
        }
        catch (Exception e) {//2014-08-25 18:57:58
            return U.parse(val, "yyyy-mm-dd hh:mm:ss");
        }
    }

    public static void main(String[] args) throws Exception {
        int cnt = 0;

        try (CsvImporter imp = new CsvImporter("XXRPT_HGSMEETINGUSERREPORT.txt", XxrptHgsMeetingUserReport.class)) {
            while(imp.readObject() != null) ++cnt;

            System.out.println("Object: " + cnt);
        }

        System.out.println("Counter: " + cnt);
    }
}
