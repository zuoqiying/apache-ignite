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

package org.apache.ignite.internal.visor.cache;

import java.io.Serializable;
import java.util.Map;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtPartitionState;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionMap2;

/**
 * Data transfer object for partitions map.
 */
public class VisorPartitionMap implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Map of partition states. */
    private Map<Integer, GridDhtPartitionState> parts;

    /**
     * @param map Partitions map.
     */
    public VisorPartitionMap(GridDhtPartitionMap2 map) {
        parts = map.map();
    }

    /**
     * @return Partitions map.
     */
    public Map<Integer, GridDhtPartitionState> getPartitions() {
        return parts;
    }

    /**
     * @return Partitions map size.
     */
    public int size() {
        return parts.size();
    }


    /**
     * @param part Partition.
     * @return Partition state.
     */
    public GridDhtPartitionState get(Integer part) {
        return parts.get(part);
    }
}
