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

package org.apache.ignite.jdbc.thin;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;

/**
 * Statement test.
 */
public class JdbcThinStatementSelfTest extends JdbcThinAbstractSelfTest {
    /** IP finder. */
    private static final TcpDiscoveryIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);

    /** URL. */
    private static final String URL = "jdbc:ignite:thin://127.0.0.1/";

    /** SQL query. */
    private static final String SQL = "select * from Person where age > 30";

    /** Connection. */
    private Connection conn;

    /** Statement. */
    private Statement stmt;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        CacheConfiguration<?,?> cache = defaultCacheConfiguration();

        cache.setCacheMode(PARTITIONED);
        cache.setBackups(1);
        cache.setWriteSynchronizationMode(FULL_SYNC);
        cache.setIndexedTypes(
            String.class, Person.class
        );

        cfg.setCacheConfiguration(cache);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGridsMultiThreaded(3);

        IgniteCache<String, Person> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        assert cache != null;

        cache.put("p1", new Person(1, "John", "White", 25));
        cache.put("p2", new Person(2, "Joe", "Black", 35));
        cache.put("p3", new Person(3, "Mike", "Green", 40));
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        conn = DriverManager.getConnection(URL);

        conn.setSchema(DEFAULT_CACHE_NAME);

        stmt = conn.createStatement();

        assert stmt != null;
        assert !stmt.isClosed();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        if (stmt != null && !stmt.isClosed()) {
            stmt.close();

            assert stmt.isClosed();
        }

        conn.close();

        assert stmt.isClosed();
        assert conn.isClosed();
    }

    /**
     * @throws Exception If failed.
     */
    public void testExecuteQuery() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        assert rs != null;

        int cnt = 0;

        while (rs.next()) {
            int id = rs.getInt("id");

            if (id == 2) {
                assert "Joe".equals(rs.getString("firstName"));
                assert "Black".equals(rs.getString("lastName"));
                assert rs.getInt("age") == 35;
            }
            else if (id == 3) {
                assert "Mike".equals(rs.getString("firstName"));
                assert "Green".equals(rs.getString("lastName"));
                assert rs.getInt("age") == 40;
            }
            else
                assert false : "Wrong ID: " + id;

            cnt++;
        }

        assert cnt == 2;
    }

    /**
     * @throws Exception If failed.
     */
    public void testExecute() throws Exception {
        assert stmt.execute(SQL);

        assert stmt.getUpdateCount() == -1 : "Update count must be -1 for SELECT query";

        ResultSet rs = stmt.getResultSet();

        assert rs != null;

        assert stmt.getResultSet() == null;

        int cnt = 0;

        while (rs.next()) {
            int id = rs.getInt("id");

            if (id == 2) {
                assert "Joe".equals(rs.getString("firstName"));
                assert "Black".equals(rs.getString("lastName"));
                assert rs.getInt("age") == 35;
            }
            else if (id == 3) {
                assert "Mike".equals(rs.getString("firstName"));
                assert "Green".equals(rs.getString("lastName"));
                assert rs.getInt("age") == 40;
            }
            else
                assert false : "Wrong ID: " + id;

            cnt++;
        }

        assert cnt == 2;
    }

    /**
     * @throws Exception If failed.
     */
    public void testMaxRows() throws Exception {
        stmt.setMaxRows(1);

        ResultSet rs = stmt.executeQuery(SQL);

        assert rs != null;

        int cnt = 0;

        while (rs.next()) {
            int id = rs.getInt("id");

            if (id == 2) {
                assert "Joe".equals(rs.getString("firstName"));
                assert "Black".equals(rs.getString("lastName"));
                assert rs.getInt("age") == 35;
            }
            else if (id == 3) {
                assert "Mike".equals(rs.getString("firstName"));
                assert "Green".equals(rs.getString("lastName"));
                assert rs.getInt("age") == 40;
            }
            else
                assert false : "Wrong ID: " + id;

            cnt++;
        }

        assert cnt == 1;

        stmt.setMaxRows(0);

        rs = stmt.executeQuery(SQL);

        assert rs != null;

        cnt = 0;

        while (rs.next()) {
            int id = rs.getInt("id");

            if (id == 2) {
                assert "Joe".equals(rs.getString("firstName"));
                assert "Black".equals(rs.getString("lastName"));
                assert rs.getInt("age") == 35;
            }
            else if (id == 3) {
                assert "Mike".equals(rs.getString("firstName"));
                assert "Green".equals(rs.getString("lastName"));
                assert rs.getInt("age") == 40;
            }
            else
                assert false : "Wrong ID: " + id;

            cnt++;
        }

        assert cnt == 2;
    }

    /**
     * @throws Exception If failed.
     */
    public void testCloseResultSet0() throws Exception {
        ResultSet rs0 = stmt.executeQuery(SQL);
        ResultSet rs1 = stmt.executeQuery(SQL);
        ResultSet rs2 = stmt.executeQuery(SQL);

        assert rs0.isClosed() : "ResultSet must be implicitly closed after re-execute statement";
        assert rs1.isClosed() : "ResultSet must be implicitly closed after re-execute statement";

        assert !rs2.isClosed() : "Last result set must be available";

        stmt.close();

        assert rs2.isClosed() : "ResultSet must be explicitly closed after close statement";
    }

    /**
     * @throws Exception If failed.
     */
    public void testCloseResultSet1() throws Exception {
        stmt.execute(SQL);

        ResultSet rs = stmt.getResultSet();

        stmt.close();

        assert rs.isClosed() : "ResultSet must be explicitly closed after close statement";
    }

    /**
     * @throws Exception If failed.
     */
    public void testCloseResultSetByConnectionClose() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        conn.close();

        assert stmt.isClosed() : "Statement must be implicitly closed after close connection";
        assert rs.isClosed() : "ResultSet must be implicitly closed after close connection";
    }

    /**
     * @throws Exception If failed.
     */
    public void testCloseOnCompletion() throws Exception {
        ResultSet rs0 = stmt.executeQuery(SQL);

        ResultSet rs1 = stmt.executeQuery(SQL);

        assert !stmt.isClosed() : "Statement must not be closed";

        rs1.close();

        assert !stmt.isClosed() : "Statement must not be closed";

        ResultSet rs2 = stmt.executeQuery(SQL);

        stmt.closeOnCompletion();

        rs2.close();

        assert stmt.isClosed() : "Statement must be closed";
    }

    /**
     * Person.
     */
    @SuppressWarnings("UnusedDeclaration")
    private static class Person implements Serializable {
        /** ID. */
        @QuerySqlField
        private final int id;

        /** First name. */
        @QuerySqlField(index = false)
        private final String firstName;

        /** Last name. */
        @QuerySqlField(index = false)
        private final String lastName;

        /** Age. */
        @QuerySqlField
        private final int age;

        /**
         * @param id ID.
         * @param firstName First name.
         * @param lastName Last name.
         * @param age Age.
         */
        private Person(int id, String firstName, String lastName, int age) {
            assert !F.isEmpty(firstName);
            assert !F.isEmpty(lastName);
            assert age > 0;

            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }
    }
}