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

package org.apache.ignite.internal.processors.cache.mvcc;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 *
 */
public class MvccQueryVersions {
    /** */
    private final AffinityTopologyVersion topVer;

    /** */
    private final Map<UUID, Long> counters;

    /** */
    private final Collection<GridCacheVersion> activeTxs;

    /**
     * @param topVer Topology version.
     * @param counters Coordinator counters.
     * @param activeTxs Active transactions.
     */
    MvccQueryVersions(AffinityTopologyVersion topVer,
        Map<UUID, Long> counters,
        Collection<GridCacheVersion> activeTxs) {
        this.topVer = topVer;
        this.counters = counters;
        this.activeTxs = activeTxs;
    }

    /**
     * @param ctx Context.
     * @param nodeId Node ID.
     * @return Mvcc query version for given node.
     */
    public MvccQueryVersion versionForNode(GridCacheSharedContext ctx, UUID nodeId) {
        ClusterNode crd = ctx.coordinators().nodeCoordinator(topVer, nodeId);

        Long cntr = counters.get(crd.id());

        assert cntr != null;

        return new MvccQueryVersion(cntr, activeTxs);
    }

    /**
     * @return Topology version.
     */
    public AffinityTopologyVersion topologyVersion() {
        return topVer;
    }

    /**
     * @return Coordinator counters.
     */
    public Map<UUID, Long> counters() {
        return counters;
    }

    /**
     * @return Active transactions.
     */
    public Collection<GridCacheVersion> activeTransactions() {
        return activeTxs;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(MvccQueryVersions.class, this);
    }
}
