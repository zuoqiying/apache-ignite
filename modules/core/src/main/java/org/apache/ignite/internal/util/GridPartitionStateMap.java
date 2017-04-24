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

package org.apache.ignite.internal.util;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Set;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtPartitionState;

/**
 * Grid partition state map implemented using bit sets.
 */
public class GridPartitionStateMap extends AbstractMap<Integer, GridDhtPartitionState> {
    private static final int REQ_BITS = Integer.SIZE - Integer.numberOfLeadingZeros(GridDhtPartitionState.values().length + 1);

    private BitSet states = new BitSet();

    @Override public Set<Entry<Integer, GridDhtPartitionState>> entrySet() {
        return null;
    }

    private GridDhtPartitionState setState(int part, GridDhtPartitionState st) {
        GridDhtPartitionState old = state(part);

        int offset = part * REQ_BITS;

        int ist = st.ordinal();
        int off;

        for (int i = 0; i < REQ_BITS && ist != 0; i += off) {
            off = Integer.numberOfTrailingZeros(ist);

            if (off == 0) {
                states.set(offset + i);

                off = 1;
            }

            ist >>>= off;
        }

        return old;
    }

    private GridDhtPartitionState state(int part) {
        int offset = part * REQ_BITS;

        int x = ((states.get(offset + 2) ? 1 : 0) << 2) | ((states.get(offset + 1) ? 1 : 0) << 1) | (states.get(offset) ? 1 : 0);

        return GridDhtPartitionState.values()[x];
    }

    public static void main(String[] args) {
        GridPartitionStateMap zzz = new GridPartitionStateMap();

        zzz.setState(0, GridDhtPartitionState.MOVING);
        zzz.setState(1, GridDhtPartitionState.RENTING);
        zzz.setState(2, GridDhtPartitionState.LOST);
        zzz.setState(3, GridDhtPartitionState.OWNING);
        zzz.setState(4, GridDhtPartitionState.EVICTED);

        zzz.setState(5, GridDhtPartitionState.MOVING);
        zzz.setState(6, GridDhtPartitionState.RENTING);
        zzz.setState(7, GridDhtPartitionState.LOST);
        zzz.setState(8, GridDhtPartitionState.OWNING);
        zzz.setState(9, GridDhtPartitionState.EVICTED);

        for (int i = 0; i < GridDhtPartitionState.values().length * 2; i++)
            System.out.println(zzz.state(i));
    }
}
