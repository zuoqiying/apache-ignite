package org.apache.ignite.yardstick.kodiak;

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
