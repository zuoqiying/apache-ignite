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

package org.apache.ignite.internal.processors.query;

import java.util.Arrays;
import java.util.List;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 * Created by apaschenko on 09.06.17.
 */
public class IgniteArraysHandlingTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrid(0);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /**
     *
     */
    public void testTest() {
        QueryEntity queryEntity = new QueryEntity(Integer.class.getName(), Integer[].class.getName());
        queryEntity.setTableName("ints");
        CacheConfiguration ccfg = new CacheConfiguration().setName("foo").setQueryEntities(Arrays.asList(queryEntity));
        IgniteCache<Integer, Integer[]> cache = grid(0).createCache(ccfg);

        cache.query(new SqlFieldsQuery("insert into ints (_key, _val) values (?, ?)")
            .setArgs(1, new Integer[] {1, 2})).getAll();  // throws

        List<List<?>> res = cache.query(new SqlFieldsQuery("SELECT * FROM ints")).getAll();

        assertEquals(1, res.size());

        assertEquals(1, res.get(0).get(0));

        System.out.println(res.get(0).get(1).getClass());

        Object o = cache.get(1);
    }
}
