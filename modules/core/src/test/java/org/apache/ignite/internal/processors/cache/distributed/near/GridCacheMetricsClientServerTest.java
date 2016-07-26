package org.apache.ignite.internal.processors.cache.distributed.near;

import java.util.Random;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 */
public class GridCacheMetricsClientServerTest extends GridCommonAbstractTest {

    /** Client node. */
    public static final int CLIENT_NODE = 1;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        if (gridName.equals(getTestGridName(CLIENT_NODE)))
            cfg.setClientMode(true);
        else
            cfg.setCacheConfiguration(cacheConfiguration(gridName));

        return cfg;
    }

    /** {@inheritDoc} */
    protected CacheConfiguration cacheConfiguration(String gridName) throws Exception {
        CacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setStatisticsEnabled(true);

        return cfg;
    }

    /**
     * @throws Exception If fail.
     */
    public void testAvgPutGetRemove() throws Exception {
        try {
            startGrid(0);

            Ignite client = startGrid(CLIENT_NODE);

            IgniteCache<Integer, Integer> cache = client.cache(null);

            Random rand = new Random();

            ClusterGroup servers = client.cluster().forServers();

            long end = System.currentTimeMillis() + 60_000;

            for (int i = 0; i < 5_000 || System.currentTimeMillis() - end > 0; i++) {

                cache.put(i, rand.nextInt(19_000_000));

                cache.get(i);

                cache.remove(i);

                CacheMetrics metrics = cache.metrics(servers);

                System.out.println("totalGet = " + metrics.getCacheGets() + "avg get = " + metrics.getAverageGetTime()
                    + ", totalPut = " + metrics.getCachePuts() + ", avg put = " + metrics.getAveragePutTime()
                    + ", totalRemove = " + metrics.getCacheRemovals() + ", avg remove = " + metrics.getAverageRemoveTime());
            }

            CacheMetrics metrics = cache.metrics(servers);

            assertTrue(metrics.getCacheGets() > 0);
            assertTrue(metrics.getCachePuts() > 0);
            assertTrue(metrics.getCacheRemovals() > 0);

            assertTrue(metrics.getAverageGetTime() > 0);
            assertTrue(metrics.getAveragePutTime() > 0);
            assertTrue(metrics.getAverageRemoveTime() > 0);

            assertTrue(metrics.getCacheGets() > metrics.getAverageGetTime());
            assertTrue(metrics.getCachePuts() > metrics.getAveragePutTime());
            assertTrue(metrics.getCacheRemovals() > metrics.getAverageRemoveTime());
        }
        finally {
            stopAllGrids();
        }

    }
}
