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

import org.apache.ignite.PersistentMemoryMetrics;
import org.apache.ignite.configuration.MemoryPolicyConfiguration;
import org.apache.ignite.internal.processors.cache.ratemetrics.HitRateMetrics;
import org.jsr166.LongAdder8;

/**
 *
 */
public class PersistentMemoryMetricsImpl extends MemoryMetricsImpl implements PersistentMemoryMetrics {
    /** */
    private HitRateMetrics pageEvictionRate;

    /** */
    private HitRateMetrics pageReloadRate;

    /** */
    private LongAdder8 dirtyPages = new LongAdder8();

    /** */
    private LongAdder8 pagesOnDisk = new LongAdder8();

    /**
     * @param memPlcCfg Mem policy config.
     */
    public PersistentMemoryMetricsImpl(MemoryPolicyConfiguration memPlcCfg) {
        super(memPlcCfg);

        pageEvictionRate = new HitRateMetrics(getRateTimeInterval(), getSubIntervals());

        pageReloadRate = new HitRateMetrics(getRateTimeInterval(), getSubIntervals());
    }

    /** {@inheritDoc} */
    @Override public long getPageEvictionRate() {
        if (metricsEnabled)
            return pageEvictionRate.getRate() / getRateTimeInterval();

        return 0;
    }

    /**
     *
     */
    void incrementEvictedPages() {
        if (metricsEnabled)
            pageEvictionRate.onHit();
    }

    /**
     *
     */
    public void incrementReloadedPages() {
        if (metricsEnabled)
            pageReloadRate.onHit();
    }

    /** {@inheritDoc} */
    @Override public long getPageReloadRate() {
        if (metricsEnabled)
            return pageReloadRate.getRate() / getRateTimeInterval();

        return 0;
    }

    /** {@inheritDoc} */
    @Override public long getDirtyPagesAmount() {
        if (metricsEnabled)
            return dirtyPages.longValue();

        return 0L;
    }

    /** {@inheritDoc} */
    @Override public long getPagesOnDiskAmount() {
        if (metricsEnabled)
            return pagesOnDisk.longValue();

        return 0L;
    }

    /**
     *
     */
    public void incrementDirtyPages() {
        if (metricsEnabled)
            dirtyPages.increment();
    }

    /**
     *
     */
    public void decrementDirtyPages() {
        if (metricsEnabled)
            dirtyPages.decrement();
    }

    /**
     *
     */
    public void resetDirtyPages() {
        if (metricsEnabled)
            dirtyPages.reset();
    }

    /**
     *
     */
    public void incrementPagesOnDisk() {
        if (metricsEnabled)
            pagesOnDisk.increment();
    }
}
