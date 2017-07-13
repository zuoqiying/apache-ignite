package org.apache.ignite.yardstick.kodiak;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.yardstick.cache.jdbc.JdbcAbstractBenchmark;
import org.yardstickframework.BenchmarkConfiguration;

import static org.yardstickframework.BenchmarkUtils.println;

/**
 *
 */
public class KodiakQuery9Benchmark extends KodiakAbstractQueryBenchmark {

  /*  "select count(*) from  \"DG\".Pocsubscrinfo where MDN in (" +
        "select recipientMDN MDN from  \"DG\".Corplistmember b,  \"DG\".Corplistdistinfo c " +
        "where b.CORPLISTID=c.CORPLISTID and b.MemberMDN=? and c.RECIPIENTMDN!=b.MemberMDN ) " +
        "and bitwiseAnd(ACTIVEFS1,1)=1"*/

    /** Benchmarked query template */
    private static final String SELECT_QUERY =
        "select count(*) from  \"DG\".Pocsubscrinfo p, \"DG\".Corplistmember b,  \"DG\".Corplistdistinfo c  " +
            "where p.MDN = c.recipientMDN and b.CORPLISTID=c.CORPLISTID and " +
            "b.MemberMDN=? and " +
            "c.RECIPIENTMDN!=b.MemberMDN and bitAnd(ACTIVEFS1,1)=1";

    @Override protected String getQuery() {
        return SELECT_QUERY;
    }
}
