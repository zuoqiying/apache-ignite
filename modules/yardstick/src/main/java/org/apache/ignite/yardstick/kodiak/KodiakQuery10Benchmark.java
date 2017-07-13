package org.apache.ignite.yardstick.kodiak;

/**
 *
 */
public class KodiakQuery10Benchmark extends KodiakAbstractQueryBenchmark {

  /*  "select count(*) from \"DG\".Pocsubscrinfo
  where MDN in (select MDN from \"DG\".XdmPoccontactlist where ContactMDN=?)
  and bitwiseAnd(ACTIVEFS1,1)=1" */

    /** Benchmarked query template */
    private static final String SELECT_QUERY =
        "select count(*) from \"DG\".Pocsubscrinfo p, \"DG\".XdmPoccontactlist x " +
            "where p.MDN = x.MDN and x.ContactMDN=? " +
            "and bitAnd(p.ACTIVEFS1,1)=1";

    @Override protected String getQuery() {
        return SELECT_QUERY;
    }
}
