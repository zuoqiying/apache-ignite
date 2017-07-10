package org.apache.ignite.sqltester;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import javax.cache.Cache;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteJdbcDriver;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.affinity.AffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteRunnable;

/**
 * Example sql date format
 */
public class IgniteSqlTesterExample {
    /**
     *
     */
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {

        final Ignite ignite = Ignition.start("/home/oostanin/gg/incubator-ignite/modules/sql-tester/src/main/resources/ignite-localhost-config.xml");


        String igniteConnStr = "jdbc:ignite:thin://127.0.0.1:10800";
        String h2ConnStr = "jdbc:h2:~/H2/testdb";

        Class.forName(IgniteJdbcDriver.class.getName());

        Connection conn;

        conn = DriverManager.getConnection(igniteConnStr);

        Statement stmt = conn.createStatement();

        ArrayList<String> qrs = new ArrayList<>();

        qrs.add("DROP TABLE IF EXISTS t1");
        qrs.add("CREATE TABLE t1 (id INT PRIMARY KEY, col_Date_1 DATE)");
        qrs.add("INSERT INTO t1 (id, col_Date_1) VALUES (1, '2001-09-09')");
        qrs.add("SELECT col_Date_1 FROM t1 WHERE id = 1");

        for (String q : qrs)
            stmt.execute(q);



        Collection<String> names = ignite.cacheNames();

        ArrayList<String> names1 = new ArrayList<>(names.size());

        for(String n : names)
            names1.add(n);

        final IgniteCache<Object, Object> cache1 = ignite.cache(names1.get(0));

        CacheConfiguration cfg = cache1.getConfiguration(CacheConfiguration.class);



        AffinityFunction af = cfg.getAffinity();

        int p = af.partitions();

        Integer key = 12;
        Integer val = 12;

        ignite.cache("query").put(key, val);

        IgniteRunnable job = new IgniteRunnable() {
            @Override public void run() {
                //System.out.println(ignite.cache("query").size());

                for(Cache.Entry entry : cache1.localEntries(CachePeekMode.PRIMARY)) {
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                }

            }
        };



        for (int i = 0; i < p; i++)
            ignite.compute().affinityRun(names, i, job);

        System.out.println();


        //ignite.close();
    }
}
