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

package org.apache.ignite.internal.trace;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.A;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trace cluster.
 */
public class TraceCluster {
    /** Cluster group. */
    private final ClusterGroup cluster;

    /** Compute instance. */
    private final IgniteCompute compute;

    /**
     * Constructor.
     *
     * @param ignite Ignite instance.
     */
    public TraceCluster(Ignite ignite) {
        this(ignite.cluster());
    }

    /**
     * Constructor.
     *
     * @param cluster Cluster group.
     */
    public TraceCluster(ClusterGroup cluster) {
        assert cluster != null;

        this.cluster = cluster;

        compute = cluster.ignite().compute(cluster);
    }

    /**
     * Enable tracing.
     */
    public void enable() {
        compute.broadcast(new TraceStateChangeClosure(true));
    }

    /**
     * Disable tracing.
     */
    public void disable() {
        compute.broadcast(new TraceStateChangeClosure(false));
    }

    /**
     * Collect trace data.
     *
     * @param grpNames Group names.
     * @return Result.
     */
    public TraceData collect(String... grpNames) {
        A.notNull(grpNames, "grpNames");

        return collect(F.asList(grpNames));
    }

    /**
     * Collect trace data.
     *
     * @param grpNames Group names.
     * @return Result.
     */
    public TraceData collect(Collection<String> grpNames) {
        return collect0(grpNames, false);
    }

    /**
     * Collect trace data.
     *
     * @param grpNames Group names.
     * @return Result.
     */
    public TraceData collectAndReset(String... grpNames) {
        A.notNull(grpNames, "grpNames");

        return collectAndReset(F.asList(grpNames));
    }

    /**
     * Collect trace data.
     *
     * @param grpNames Group names.
     * @return Result.
     */
    public TraceData collectAndReset(Collection<String> grpNames) {
        return collect0(grpNames, true);
    }

    /**
     * Collect trace data.
     *
     * @param grpNames Group names.
     * @param reset Reset flag.
     * @return Result.
     */
    @SuppressWarnings("unchecked")
    private TraceData collect0(Collection<String> grpNames, boolean reset) {
        Collection<TraceNodeResult> ress = compute.broadcast(new TraceCollectClosure(grpNames, reset));

        Map<String, List<TraceThreadResult>> grps = new HashMap<>();

        for (TraceNodeResult res : ress) {
            for (String grpName : grpNames) {
                TraceThreadGroupResult grpRes = res.groups().get(grpName);

                if (grpRes != null) {
                    List<TraceThreadResult> grpData = grps.get(grpName);

                    if (grpData == null) {
                        grpData = new ArrayList();

                        grps.put(grpName, grpData);
                    }

                    grpData.addAll(grpRes.threads());
                }
            }
        }

        return new TraceData(grps);
    }

    /**
     * @return Cluster.
     */
    public ClusterGroup cluster() {
        return cluster;
    }
}
