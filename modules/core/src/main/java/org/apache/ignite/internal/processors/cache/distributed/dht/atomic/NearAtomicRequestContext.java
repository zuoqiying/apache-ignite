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

package org.apache.ignite.internal.processors.cache.distributed.dht.atomic;

import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtPartitionTopology;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;

/**
 *
 */
public class NearAtomicRequestContext {
    /** */
    private final ClusterNode node;

    /** */
    private GridNearAtomicUpdateResponse res;

    /** */
    private int cnt;

    /** */
    private final GridDhtPartitionTopology top;

    /** */
    private final GridCacheVersion ver;

    /**
     * @param size Stripes number.
     * @param top Partition topology.
     */
    public NearAtomicRequestContext(ClusterNode node, int size, GridDhtPartitionTopology top, GridCacheVersion ver) {
        this.node = node;
        this.top = top;
        this.ver = ver;

        cnt = size;
    }

    /**
     * @param res Response.
     * @return {@code true} if all responses added.
     */
    public GridNearAtomicUpdateResponse addResponse(GridNearAtomicUpdateResponse res) {
        synchronized (this) {
            if (cnt == 0)
                return null;

            if (res.stripe() == -1)
                return res;

            if (this.res == null)
                this.res = res;
            else
                this.res.merge(res);

            return --cnt == 0 ? this.res : null;
        }
    }

    /**
     * @return GridDhtPartitionTopology.
     */
    public GridDhtPartitionTopology topology() {
        return top;
    }

    /**
     *
     * @return
     */
    public GridCacheVersion ver() {
        return ver;
    }

    /**
     * @return Node.
     */
    public ClusterNode node() {
        return node;
    }
}
