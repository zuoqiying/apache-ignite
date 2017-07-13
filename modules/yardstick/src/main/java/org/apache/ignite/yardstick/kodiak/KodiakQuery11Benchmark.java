package org.apache.ignite.yardstick.kodiak;

/**
 *
 */
public class KodiakQuery11Benchmark extends KodiakAbstractQueryBenchmark {

  /*  "select distinct ContactMDN from (select recipientMDN MDN, MemberMDN ContactMDN,a.listtype listtype
  from \"DG\".Corplistinfo a , \"DG\".Corplistmember b, \"DG\".Corplistdistinfo c
  where a.CORPLISTID=b.CORPLISTID and a.CORPLISTID=c.CORPLISTID and c.RECIPIENTMDN=? and c.RECIPIENTMDN!=b.MemberMDN
  order by decode (listtype,2,-1,0),ContactMDN)" */

    /** Benchmarked query template */
    private static final String SELECT_QUERY =
        "select distinct ContactMDN from (select recipientMDN MDN, MemberMDN ContactMDN,a.listtype listtype " +
            "  from \"DG\".Corplistinfo a , \"DG\".Corplistmember b, \"DG\".Corplistdistinfo c " +
            "  where a.CORPLISTID=b.CORPLISTID and a.CORPLISTID=c.CORPLISTID and c.RECIPIENTMDN=? and c.RECIPIENTMDN!=b.MemberMDN " +
            "  order by decode (listtype,2,-1,0),ContactMDN)";

    @Override protected String getQuery() {
        return SELECT_QUERY;
    }
}
