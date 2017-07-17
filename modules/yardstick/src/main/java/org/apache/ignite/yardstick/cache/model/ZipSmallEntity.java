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

import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.jsr166.ThreadLocalRandom8;

import static org.apache.ignite.yardstick.cache.model.ZipEntity.randomString;

/**
 *
 */
public class ZipSmallEntity {
    public static final int RND_STRING_LEN = ZipEntity.RND_STRING_LEN;

    @QuerySqlField(index = true)
    public String BUSINESSDATE;
    @QuerySqlField(index = true)
    public String RISKSUBJECTID;
    @QuerySqlField(index = true)
    public String SERIESDATE;
    @QuerySqlField(index = true)
    public String SNAPVERSION;
    @QuerySqlField(index = true)
    public String VARTYPE;

    public static ZipSmallEntity generateHard(int len) {
        ZipSmallEntity entity = new ZipSmallEntity();

        ThreadLocalRandom8 rnd = ThreadLocalRandom8.current();

        entity.BUSINESSDATE = randomString(rnd, len);
        entity.RISKSUBJECTID = randomString(rnd, len);
        entity.SERIESDATE = randomString(rnd, len);
        entity.SNAPVERSION = randomString(rnd, len);
        entity.VARTYPE = randomString(rnd, len);

        return entity;
    }
}
