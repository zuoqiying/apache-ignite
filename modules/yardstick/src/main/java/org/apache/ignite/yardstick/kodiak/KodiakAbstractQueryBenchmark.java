package org.apache.ignite.yardstick.kodiak;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCountDownLatch;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.thread.IgniteThreadPoolExecutor;
import org.apache.ignite.yardstick.cache.jdbc.JdbcAbstractBenchmark;
import org.yardstickframework.BenchmarkConfiguration;

/**
 *
 */
public abstract class KodiakAbstractQueryBenchmark extends JdbcAbstractBenchmark {
    /** Dummy cache name. */
    static final String DUMMY_CACHE = "DG";

    /** */
    static final int noOfMdn = 100_000;

    /** */
    static final int noOfContacts = 100;

    /** */
    static final long MDN_START_VALUE = 919200000001L;

    /** */
    public static final int LOADER_POOL_SIZE = 16;

    /** */
    private AtomicInteger mCorpListID = new AtomicInteger(1);

    /** */
    private ExecutorService loaderExec;

    /** */
    private ThreadLocal<Long> mdnCntr = new ThreadLocal<Long>() {
        @Override protected Long initialValue() {
            return 919200000001L;
        }
    };

    /** */
    private void createPocSubscr(long longMdnStartValue, final String pttID, int noOfMdns) throws Exception {

        final AtomicLong mdnCounter = new AtomicLong(longMdnStartValue);

        final AtomicLong finished = new AtomicLong();

        final Semaphore sem = new Semaphore(200);

        for (int i = 1; i <= noOfMdns; i++) {
            sem.acquire();

            loaderExec.submit(new Runnable() {
                @Override public void run() {
                    try {
                        try {
                            final long longMdn = mdnCounter.getAndIncrement();

                            insertPocsubTable(longMdn, pttID, 2, 0, 1, 0, 1, 1, 1);

                            insertCorpcontactcount(longMdn);

                            addContacts(longMdn, noOfContacts);

                            long fin = finished.incrementAndGet();
                            if (fin % 1000 == 0)
                                U.quietAndInfo(ignite().log(), "Loaded " + (100 * fin / noOfMdn) + "%");
                        }
                        finally {
                            sem.release();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        loaderExec.shutdown();

        try {
            while (!loaderExec.awaitTermination(1, TimeUnit.SECONDS)) {

            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** */
    private void insertPocsubTable(long longMDN, String pttID, int pvMajVer, int publicSubType, int corpSubType,
        int DispatchGroupMemb, long ActiveFs, int lCorpId, int clientType) throws SQLException {
        try (PreparedStatement stmt = conn.get().prepareStatement(
            "INSERT INTO \"DG\".Pocsubscrinfo(mdn,pochome,presencehome,xdmshome,subscrcreationtime," +
                "lastprofileupdatetime,subscrname,serviceauthstatus,publicsubscriptiontype,corpsubscriptiontype," +
                "corpid,corpcontactlistid,corpcontactpairingind,imei,clientPassword,affliateid,paytype,useragent," +
                "email, clientType,dispatchGrpMember,subscriberfs1,clientfs1,activefs1,clientpvMajorversion," +
                "clientpvMinorversion,accountId,pamaccid,opsfs1,statsreportintvl,statscollectionintvl," +
                "statsconfigbitmap,corpadminfs1,lastActivationTime,clientuistatssample,clientuistatsreportintvl," +
                "clientPlatformType,clientSwInfo,dynamicqosflag,vocoderid) " +
                "VALUES (?,'010111','010111','010111',1476342288535L,1488345097L,'Apoorv'," +
                "2,1,1,34,23,1,'12345678912345','30038c540b7323d71d253e045610cc8e','1234567890',1," +
                "'PoC-client/OMA2.0 samsung/GT-I9100M Android/4.3 knpoc-07_008_01_01_01E/6.0 Android_PoC_Client_07_008_00_01D I9500DDUEMK9'," +
                "'messaging@kodiakptt.com',1,1,8140644361945431,17592331796481,1,1,1,'465646',23,8140644361945431,13,12,1,2247264328204671," +
                "1491905657172,1,5,1,1,1,123)")) {
            // Populate persons.
            stmt.setString(1, String.valueOf(longMDN));
            stmt.execute();
        }
    }

    /** */
    private void insertCorpcontactcount(long longMDN) throws SQLException {
        try (PreparedStatement stmt = conn.get().prepareStatement(
            "INSERT INTO \"DG\".Corpcontactcount (mdn,contactcount,lastupdatetime) values(?,?,?)")) {

            stmt.setString(1, String.valueOf(longMDN));
            stmt.setInt(2, 1);
            stmt.setTimestamp(3, new Timestamp(1496038522000L));
            stmt.execute();
        }
    }

    /** */
    private void addContacts(long longMDN, int numCont) throws SQLException {
        int corpListID = mCorpListID.getAndIncrement();
        insertCorplistinfo(corpListID);

        insertCorplistDistinfo(corpListID, longMDN);

        long addCont = longMDN;

        while (numCont > 0) {
            insertCorpMemberbTable(corpListID, ++addCont);

            --numCont;
        }
    }

    /** */
    private void insertCorplistinfo(int corpListId) throws SQLException {
        try (PreparedStatement stmt = conn.get().prepareStatement(
            "INSERT INTO \"DG\".Corplistinfo (corplistid,corpid,listdisplayname,listdistributionpolicy,listtype,etag," +
                "lastupdatetime,listdisplayName1) values(?,?,?,?,?,?,?,?)")) {

            stmt.setInt(1, corpListId);
            stmt.setInt(2, 1);
            stmt.setString(3, "kodiak");
            stmt.setByte(4, (byte)1);
            stmt.setByte(5, (byte)1);
            stmt.setInt(6, 1);
            stmt.setLong(7, 1);
            stmt.setString(8, "");
            stmt.execute();
        }
    }

    /** */
    private void insertCorplistDistinfo(int corpListID, long longMDN) throws SQLException {
        try (PreparedStatement stmt = conn.get().prepareStatement("INSERT INTO \"DG\".Corplistdistinfo (corplistid,recipientmdn)" +
            " values(?,?)")) {
            stmt.setInt(1, corpListID);
            stmt.setString(2, String.valueOf(longMDN));

            stmt.execute();
        }
    }

    /** */
    private void insertCorpMemberbTable(int corpListID, long contMdn) throws SQLException {
        try (PreparedStatement stmt = conn.get().prepareStatement(
            "INSERT INTO \"DG\".Corplistmember (corplistid,membermdn,membercorpid)" +
                " values(?,?,?)")) {
            stmt.setInt(1, corpListID);
            stmt.setString(2, String.valueOf(contMdn));
            stmt.setInt(3, 2);

            stmt.execute();
        }
    }

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        final int nodeID = cfg.memberId();

        final int driverCnt = cfg.driverNames().size();

        IgniteCountDownLatch latch = ignite().countDownLatch("fillCacheLock", driverCnt, false, true);

        U.quietAndInfo(ignite().log(), "Populating query data...");

        long start = System.nanoTime();

        loaderExec = new IgniteThreadPoolExecutor(
            "loaderPool",
            "loaderNode",
            LOADER_POOL_SIZE,
            LOADER_POOL_SIZE,
            10_000,
            new LinkedBlockingQueue<Runnable>());

        int mdnPerDriver = noOfMdn / driverCnt;

        createPocSubscr(MDN_START_VALUE + (nodeID * mdnPerDriver), "010111",
            (nodeID + 1 == driverCnt) ? (noOfMdn - nodeID * mdnPerDriver) : mdnPerDriver);

        U.quietAndInfo(ignite().log(), "Finished populating join query data in " + ((System.nanoTime() - start) / 1_000_000) + " ms.");

        latch.countDown();

        latch.await();
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        Long mdnCntr = this.mdnCntr.get();

        if (mdnCntr >= MDN_START_VALUE + noOfMdn)
            mdnCntr = MDN_START_VALUE;

        IgniteCache<Object, Object> cache = ignite().cache(DUMMY_CACHE);

        SqlFieldsQuery qry = new SqlFieldsQuery(getQuery());
        qry.setArgs(mdnCntr);

        this.mdnCntr.set(mdnCntr + 1);

        try (QueryCursor cursor = cache.query(qry)) {
            long rowCnt = 0;

            Iterator it = cursor.iterator();

            while (it.hasNext()) {
                it.next();

                rowCnt++;
            }
        }

        return true;
    }

    protected abstract String getQuery();

    /** {@inheritDoc} */
    @Override public void tearDown() throws Exception {
        super.tearDown();

        ignite().cache(DUMMY_CACHE).removeAll();
    }
}
