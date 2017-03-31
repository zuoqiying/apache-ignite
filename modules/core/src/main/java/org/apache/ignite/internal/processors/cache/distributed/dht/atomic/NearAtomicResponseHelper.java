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

/**
 *
 */
public class NearAtomicResponseHelper {

    /** */
    private final GridNearAtomicFullUpdateRequest req;

    /** */
    private GridNearAtomicUpdateResponse res;

    /** */
    private int cnt;

    /**
     * @param stripeNum Stripes number.
     */
    public NearAtomicResponseHelper(GridNearAtomicFullUpdateRequest req, int stripeNum) {
        this.req = req;
        this.cnt = stripeNum;
    }

    /**
     * @param res Response.
     * @return {@code true} if all responses added.
     */
    public GridNearAtomicUpdateResponse addResponse(GridNearAtomicUpdateResponse res) {
        synchronized (this) {
            if (res.stripe() == -1)
                return res;

            mergeResponse(res);
            return --cnt == 0 ? this.res : null;
        }
    }

    /**
     * @param res Response.
     */
    private void mergeResponse(GridNearAtomicUpdateResponse res) {
        if (this.res == null)
            this.res = res;
        else {
            if (res.nearValuesIndexes() != null)
                for (int i = 0; i < res.nearValuesIndexes().size(); i++)
                    this.res.addNearValue(
                        res.nearValuesIndexes().get(i),
                        res.nearValue(i),
                        res.nearTtl(i),
                        res.nearExpireTime(i)
                    );

            if (res.failedKeys() != null)
                this.res.addFailedKeys(res.failedKeys(), res.error());

            if (res.skippedIndexes() != null)
                for (Integer skippedIndex : res.skippedIndexes()) {
                    this.res.addSkippedIndex(skippedIndex);
                }

        }
    }

    /**
     *
     * @return
     */
    public GridNearAtomicFullUpdateRequest req() {
        return req;
    }
}
