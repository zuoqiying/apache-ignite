package org.apache.ignite.internal.trace;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Collected trace data.
 */
public class TraceData {
    /** Group data. */
    private final Map<String, List<TraceThreadResult>> grps;

    /**
     * Constructor.
     *
     * @param grps Group mapping.
     */
    public TraceData(Map<String, List<TraceThreadResult>> grps) {
        this.grps = grps;
    }

    /**
     * Get available groups.
     *
     * @return Available groups.
     */
    public Collection<String> groups() {
        return grps.keySet();
    }

    /**
     * Get group data.
     *
     * @param grpName Group name.
     * @return Group data.
     */
    @SuppressWarnings("unchecked")
    public List<TraceThreadResult> groupData(String grpName) {
        List<TraceThreadResult> res = grps.get(grpName);

        if (res == null)
            return Collections.emptyList();
        else
            return res;
    }
}
