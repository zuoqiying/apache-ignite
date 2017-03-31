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

import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtPartitionTopology;
import org.apache.ignite.internal.util.GridIntList;

/**
 *
 */
public class NearAtomicRequestContext {

    /** */
    private GridNearAtomicUpdateResponse res;

    /** */
    private int cnt;

    /** */
    private final GridDhtAtomicCache.StripeMap map;

    /** */
    private final GridDhtPartitionTopology top;

    /**
     * @param map Stripe map.
     */
    public NearAtomicRequestContext(GridDhtAtomicCache.StripeMap map, GridDhtPartitionTopology top) {
        this.map = map;
        this.top = top;

        cnt = map.size();
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
     * @return Stripe Map.
     */
    public GridDhtAtomicCache.StripeMap stripeMap() {
        return map;
    }

    /**
     * @param stripe Stripe number.
     * @return Key indexes for stripe.
     */
    public GridIntList mapForStripe(int stripe) {
        return map.get(stripe);
    }

    /**
     * @return GridDhtPartitionTopology.
     */
    public GridDhtPartitionTopology topology() {
        return top;
    }
}
