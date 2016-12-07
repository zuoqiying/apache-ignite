package org.apache.ignite.internal.processors.query.h2;

import org.apache.ignite.*;
import org.apache.ignite.binary.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.cache.query.*;
import org.apache.ignite.cache.query.annotations.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.internal.binary.*;
import org.apache.ignite.internal.binary.mutabletest.*;
import org.apache.ignite.spi.discovery.tcp.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.*;
import org.apache.ignite.testframework.config.*;
import org.apache.ignite.testframework.junits.common.*;
import org.junit.*;

import java.util.*;


public class IndexingFieldInBinarySelfTest extends GridCommonAbstractTest {
    /** */
    private static final TcpDiscoveryIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);
    private static final int NODES_COUNT = 2;
    private static final int PERSON_COUNT = 50;
    private static final String PERSON_CACHE_NAME = "pers";

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        GridTestProperties.setProperty(GridTestProperties.MARSH_CLASS_NAME, BinaryMarshaller.class.getName());

        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setPeerClassLoadingEnabled(true);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();
        disco.setIpFinder(IP_FINDER);
        cfg.setDiscoverySpi(disco);

        //cfg.setCacheConfiguration(cacheConfig(PERSON_CACHE_NAME));

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGridsMultiThreaded(NODES_COUNT, false);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    private static CacheConfiguration cacheConfig(String cacheName, String valueName, boolean binary) {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
            .setName(cacheName)
            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
            .setBackups(1);

        if (binary)
            cacheConfiguration.setStoreKeepBinary(true);

        LinkedHashMap<String, String> queryFields = new LinkedHashMap<>();
        queryFields.put("id", Long.class.getName());
        queryFields.put("name", String.class.getName());
        queryFields.put("secId", String.class.getName());

        QueryIndex qiId = new QueryIndex("id");
        QueryIndex qiName = new QueryIndex("name");
        QueryIndex qiSI = new QueryIndex("secId");

        QueryEntity qe = new QueryEntity();
        qe.setKeyType(String.class.getName());
        qe.setValueType(valueName);
        qe.setFields(queryFields);
        qe.setIndexes(Arrays.asList(qiId, qiName, qiSI));

        cacheConfiguration.setQueryEntities(Arrays.asList(qe));

        return cacheConfiguration;
    }

    public void testJavaTypeWithIncorrectField() {
        IgniteCache<String, Person> pers = ignite(0)
            .getOrCreateCache(cacheConfig(PERSON_CACHE_NAME, Person.class.getName(), false));

        pers.put("pers0", new Person("pers0", "Name"));
    }

    public void testIncorrectBinaryFieldType() throws Exception {
        String valueName = Person.class.getName().toUpperCase();

        IgniteCache<String, BinaryObject> pers = ignite(0)
            .getOrCreateCache(cacheConfig(PERSON_CACHE_NAME, valueName, true)).withKeepBinary();

        try {
            awaitPartitionMapExchange();

            BinaryObject id = ignite(0).binary().builder("ORG").setField("someField", 111).build();

            BinaryObjectBuilder bob =  ignite(0).binary().builder(valueName);
            pers.put("pers0", binaryPerson(bob, id, "PersonName"));
        }
        finally {
            pers.destroy();
        }
    }

    private void populateDataIntoCaches(IgniteCache<String, BinaryObject> persCache) {
        IgniteBinary binary = ignite(0).binary();

        for (int id = 0; id < PERSON_COUNT; id++) {
            String personId = PERSON_CACHE_NAME + id;
            String name = "Person name #" + id;

            BinaryObject person = binaryPerson(binary.builder("Person"), personId, name);

            persCache.put(personId, person);
        }
    }

    private BinaryObject binaryPerson(BinaryObjectBuilder bob, Object id, Object name) {
        bob.setField("id", id);
        bob.setField("name", name);

        return bob.build();
    }

    private static class Person {
        private String id;
        private String name;
        private String secId;
        private Date createTime;

        public Person() {
        }

        public Person(String id, String name) {
            this.id = id;
            this.name = name;
            this.createTime = new Date();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSecId() {
            return secId;
        }

        public void setSecId(String secId) {
            this.secId = secId;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }
    }
}
