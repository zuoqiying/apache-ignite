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
import java.util.Collection;
import org.apache.ignite.cache.CacheAtomicWriteOrderMode;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.internal.visor.util.VisorTaskUtils.compactClass;

/**
 * Data transfer object for cache configuration properties.
 */
public class VisorCacheConfiguration implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache name. */
    private String name;

    /** Cache mode. */
    private CacheMode mode;

    /** Cache atomicity mode. */
    private CacheAtomicityMode atomicityMode;

    /** Cache atomicity write ordering mode. */
    private CacheAtomicWriteOrderMode atomicWriteOrderMode;

    /** Eager ttl flag. */
    private boolean eagerTtl;

    /** Write synchronization mode. */
    private CacheWriteSynchronizationMode writeSynchronizationMode;

    /** Invalidate. */
    private boolean invalidate;

    /** Start size. */
    private int startSize;

    /** Off-heap max memory. */
    private long offHeapMaxMemory;

    /** Max concurrent async operations. */
    private int maxConcurrentAsyncOps;

    /** Cache interceptor. */
    private String interceptor;

    /** Gets default lock acquisition timeout. */
    private long dfltLockTimeout;

    /** Cache affinityCfg config. */
    private VisorCacheAffinityConfiguration affinityCfg;

    /** Preload config. */
    private VisorCacheRebalanceConfiguration rebalanceCfg;

    /** Eviction config. */
    private VisorCacheEvictionConfiguration evictCfg;

    /** Near cache config. */
    private VisorCacheNearConfiguration nearCfg;

    /** Store config. */
    private VisorCacheStoreConfiguration storeCfg;

    /** Collection of type metadata. */
    private Collection<VisorCacheTypeMetadata> typeMeta;

    /** Whether statistics collection is enabled. */
    private boolean statisticsEnabled;

    /** Whether management is enabled. */
    private boolean mgmtEnabled;

    /** Class name of cache loader factory. */
    private String ldrFactory;

    /** Class name of cache writer factory. */
    private String writerFactory;

    /** Class name of expiry policy factory. */
    private String expiryPlcFactory;

    /** Query configuration. */
    private VisorCacheQueryConfiguration qryCfg;

    /** System cache flag. */
    private boolean sys;

    /**
     * Create data transfer object for cache configuration properties.
     *
     * @param ignite Grid.
     * @param ccfg Cache configuration.
     */
    public VisorCacheConfiguration(IgniteEx ignite, CacheConfiguration ccfg) {
        name = ccfg.getName();
        mode = ccfg.getCacheMode();
        atomicityMode = ccfg.getAtomicityMode();
        atomicWriteOrderMode = ccfg.getAtomicWriteOrderMode();
        eagerTtl = ccfg.isEagerTtl();
        writeSynchronizationMode = ccfg.getWriteSynchronizationMode();
        invalidate = ccfg.isInvalidate();
        startSize = ccfg.getStartSize();
        offHeapMaxMemory = ccfg.getOffHeapMaxMemory();
        maxConcurrentAsyncOps = ccfg.getMaxConcurrentAsyncOperations();
        interceptor = compactClass(ccfg.getInterceptor());
        dfltLockTimeout = ccfg.getDefaultLockTimeout();
        typeMeta = VisorCacheTypeMetadata.list(ccfg.getQueryEntities(), ccfg.getCacheStoreFactory(), ccfg.getTypeMetadata());
        statisticsEnabled = ccfg.isStatisticsEnabled();
        mgmtEnabled = ccfg.isManagementEnabled();
        ldrFactory = compactClass(ccfg.getCacheLoaderFactory());
        writerFactory = compactClass(ccfg.getCacheWriterFactory());
        expiryPlcFactory = compactClass(ccfg.getExpiryPolicyFactory());
        sys = ignite.context().cache().systemCache(ccfg.getName());

        affinityCfg = new VisorCacheAffinityConfiguration(ccfg);
        rebalanceCfg = new VisorCacheRebalanceConfiguration(ccfg);
        evictCfg = new VisorCacheEvictionConfiguration(ccfg);
        nearCfg = VisorCacheNearConfiguration.from(ccfg);

        storeCfg = new VisorCacheStoreConfiguration().from(ignite, ccfg);

        qryCfg = new VisorCacheQueryConfiguration().from(ccfg);
    }

    /**
     * @return Cache name.
     */
    @Nullable public String getName() {
        return name;
    }

    /**
     * @return Cache mode.
     */
    public CacheMode getMode() {
        return mode;
    }

    /**
     * @return Cache atomicity mode
     */
    public CacheAtomicityMode getAtomicityMode() {
        return atomicityMode;
    }

    /**
     * @return Cache atomicity write ordering mode.
     */
    public CacheAtomicWriteOrderMode getAtomicWriteOrderMode() {
        return atomicWriteOrderMode;
    }

    /**
     * @return Eager ttl flag
     */
    public boolean getEagerTtl() {
        return eagerTtl;
    }

    /**
     * @return Write synchronization mode.
     */
    public CacheWriteSynchronizationMode getWriteSynchronizationMode() {
        return writeSynchronizationMode;
    }

    /**
     * @return Invalidate.
     */
    public boolean isInvalidate() {
        return invalidate;
    }

    /**
     * @return Start size.
     */
    public int getStartSize() {
        return startSize;
    }

    /**
     * @return Off-heap max memory.
     */
    public long getOffsetHeapMaxMemory() {
        return offHeapMaxMemory;
    }

    /**
     * @return Max concurrent async operations
     */
    public int getMaxConcurrentAsyncOperations() {
        return maxConcurrentAsyncOps;
    }

    /**
     * @return Cache interceptor.
     */
    @Nullable public String getInterceptor() {
        return interceptor;
    }

    /**
     * @return Gets default lock acquisition timeout.
     */
    public long getDefaultLockTimeout() {
        return dfltLockTimeout;
    }

    /**
     * @return Collection of type metadata.
     */
    public Collection<VisorCacheTypeMetadata> getTypeMeta() {
        return typeMeta;
    }

    /**
     * @return {@code true} if cache statistics enabled.
     */
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    /**
     * @return Whether management is enabled.
     */
    public boolean isManagementEnabled() {
        return mgmtEnabled;
    }

    /**
     * @return Class name of cache loader factory.
     */
    public String getLoaderFactory() {
        return ldrFactory;
    }

    /**
     * @return Class name of cache writer factory.
     */
    public String getWriterFactory() {
        return writerFactory;
    }

    /**
     * @return Class name of expiry policy factory.
     */
    public String getExpiryPolicyFactory() {
        return expiryPlcFactory;
    }

    /**
     * @return Cache affinityCfg config.
     */
    public VisorCacheAffinityConfiguration getAffinityConfiguration() {
        return affinityCfg;
    }

    /**
     * @return Preload config.
     */
    public VisorCacheRebalanceConfiguration getRebalanceConfiguration() {
        return rebalanceCfg;
    }

    /**
     * @return Eviction config.
     */
    public VisorCacheEvictionConfiguration getEvictConfiguration() {
        return evictCfg;
    }

    /**
     * @return Near cache config.
     */
    public VisorCacheNearConfiguration getNearConfiguration() {
        return nearCfg;
    }

    /**
     * @return Store config
     */
    public VisorCacheStoreConfiguration getStoreConfiguration() {
        return storeCfg;
    }

    /**
     * @return Cache query configuration.
     */
    public VisorCacheQueryConfiguration getQueryConfiguration() {
        return qryCfg;
    }

    /**
     * @return System cache state.
     */
    public boolean isSystem() {
        return sys;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorCacheConfiguration.class, this);
    }
}
