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
package org.apache.ignite.internal.processors.cache.database;

import org.apache.ignite.MemoryMetrics;
import org.apache.ignite.configuration.MemoryPolicyConfiguration;
import org.apache.ignite.internal.processors.cache.database.freelist.FreeListImpl;
import org.apache.ignite.internal.processors.cache.ratemetrics.HitRateMetrics;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jsr166.LongAdder8;

/**
 *
 */
public class MemoryMetricsImpl implements MemoryMetrics {
    /** */
    private FreeListImpl freeList;

    /** */
    private final LongAdder8 totalAllocatedPages = new LongAdder8();

    /**
     * Counter for number of pages occupied by large entries (one entry is larger than one page).
     */
    private final LongAdder8 largeEntriesPages = new LongAdder8();

    /** */
    protected volatile boolean metricsEnabled;

    /** Time interval (in seconds) when allocations/evictions are counted to calculate rate.
     * Default value is 60 seconds. */
    private volatile int rateTimeInterval = 60;

    /** Number of subintervals the whole rateTimeInteval is split into to calculate rate. */
    private volatile int subInts = 5;

    /** Allocation rate calculator. */
    private volatile HitRateMetrics allocRate = new HitRateMetrics(60_000, 5);

    /** */
    private final MemoryPolicyConfiguration memPlcCfg;

    /**
     * @param memPlcCfg MemoryPolicyConfiguration.
     */
    public MemoryMetricsImpl(MemoryPolicyConfiguration memPlcCfg) {
        this.memPlcCfg = memPlcCfg;

        metricsEnabled = memPlcCfg.isMetricsEnabled();
    }

    /** {@inheritDoc} */
    @Override public String getName() {
        return U.maskName(memPlcCfg.getName());
    }

    /** {@inheritDoc} */
    @Override public long getTotalAllocatedPages() {
        return metricsEnabled ? totalAllocatedPages.longValue() : 0;
    }

    /** {@inheritDoc} */
    @Override public float getAllocationRate() {
        if (!metricsEnabled)
            return 0;

        return ((float) allocRate.getRate()) / rateTimeInterval;
    }

    /** {@inheritDoc} */
    @Override public float getEvictionRate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public float getLargeEntriesPagesPercentage() {
        if (!metricsEnabled)
            return 0;

        return totalAllocatedPages.longValue() != 0 ?
                (float) largeEntriesPages.doubleValue() / totalAllocatedPages.longValue()
                : 0;
    }

    /** {@inheritDoc} */
    @Override public float getPagesFillFactor() {
        if (!metricsEnabled || freeList == null)
            return 0;

        return freeList.fillFactor();
    }

    /**
     * Increments totalAllocatedPages counter.
     */
    public void incrementTotalAllocatedPages() {
        if (metricsEnabled) {
            totalAllocatedPages.increment();

            updateAllocationRateMetrics();
        }
    }

    /**
     *
     */
    private void updateAllocationRateMetrics() {
        allocRate.onHit();
    }

    /**
     *
     */
    public void incrementLargeEntriesPages() {
        if (metricsEnabled)
            largeEntriesPages.increment();
    }

    /**
     *
     */
    public void decrementLargeEntriesPages() {
        if (metricsEnabled)
            largeEntriesPages.decrement();
    }

    /**
     * Enable metrics.
     */
    public void enableMetrics() {
        metricsEnabled = true;
    }

    /**
     * Disable metrics.
     */
    public void disableMetrics() {
        metricsEnabled = false;
    }

    /**
     * @param rateTimeInterval Time interval used to calculate allocation/eviction rate.
     */
    public void rateTimeInterval(int rateTimeInterval) {
        this.rateTimeInterval = rateTimeInterval;
        allocRate = new HitRateMetrics(rateTimeInterval * 1000, subInts);
    }

    /**
     * Sets number of subintervals the whole rateTimeInterval will be split into to calculate allocation rate.
     *
     * @param subInts Number of subintervals.
     */
    public void subIntervals(int subInts) {
        assert subInts > 0;

        if (this.subInts == subInts)
            return;

        int rateIntervalMs = rateTimeInterval * 1000;

        if (rateIntervalMs / subInts < 10)
            subInts = rateIntervalMs / 10;

        allocRate = new HitRateMetrics(rateIntervalMs, subInts);
    }

    /**
     * @param freeList Free list.
     */
    void freeList(FreeListImpl freeList) {
        this.freeList = freeList;
    }

    /**
     * @return rateTimeInterval
     */
    protected int getRateTimeInterval() {
        return rateTimeInterval;
    }

    /**
     *
     */
    protected int getSubIntervals() {
        return subInts;
    }
}
