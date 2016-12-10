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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.cache.GridCacheAdapter;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCacheEntryEx;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtCacheAdapter;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtPartitionTopology;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearCacheAdapter;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;
import org.apache.ignite.lang.IgniteUuid;
import org.jetbrains.annotations.Nullable;

/**
 * Data transfer object for {@link IgniteCache}.
 */
public class VisorCache extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Default cache size sampling. */
    private static final int DFLT_CACHE_SIZE_SAMPLING = 10;

    /** Cache name. */
    private String name;

    /** Cache deployment ID. */
    private IgniteUuid dynamicDeploymentId;

    /** Cache mode. */
    private CacheMode mode;

    /** Cache size in bytes. */
    private long memorySize;

    /** Cache size in bytes. */
    private long indexesSize;

    /** Number of all entries in cache. */
    private int size;

    /** Number of all entries in near cache. */
    private int nearSize;

    /** Number of all entries in DHT cache. */
    private int dhtSize;

    /** Number of primary entries in cache. */
    private int primarySize;

    /** Memory size allocated in off-heap. */
    private long offHeapAllocatedSize;

    /** Number of cache entries stored in off-heap memory. */
    private long offHeapEntriesCnt;

    /** Number of partitions. */
    private int partitions;

    /** Cache metrics. */
    private VisorCacheMetrics metrics;

    /** Cache partitions states. */
    private VisorPartitionMap parts;

    /** Flag indicating that cache has near cache. */
    private boolean near;

    /**
     * Create data transfer object for given cache.
     */
    public VisorCache() {
        // No-op.
    }

    /**
     * Create data transfer object for given cache.
     *
     * @param ca Internal cache.
     * @param sample Sample size.
     * @throws IgniteCheckedException If failed to create data transfer object.
     */
    public VisorCache(IgniteEx ignite, GridCacheAdapter ca, int sample) throws IgniteCheckedException {
        assert ca != null;

        name = ca.name();


        // Cache was not started.
        GridCacheContext cctx = ca.context();

        CacheConfiguration cfg = ca.configuration();

        mode = cfg.getCacheMode();

        boolean partitioned = (mode == CacheMode.PARTITIONED || mode == CacheMode.REPLICATED)
            && cctx.affinityNode();

        if (partitioned) {
            GridDhtCacheAdapter dca = null;

            if (ca instanceof GridNearCacheAdapter)
                dca = ((GridNearCacheAdapter)ca).dht();
            else if (ca instanceof GridDhtCacheAdapter)
                dca = (GridDhtCacheAdapter)ca;

            if (dca != null) {
                GridDhtPartitionTopology top = dca.topology();

                if (cfg.getCacheMode() != CacheMode.LOCAL && cfg.getBackups() > 0)
                    parts = new VisorPartitionMap(top.localPartitionMap());
            }
        }

        size = ca.size();
        nearSize = ca.nearSize();
        dynamicDeploymentId = cctx.dynamicDeploymentId();
        dhtSize = size - nearSize;
        primarySize = ca.primarySize();
        offHeapAllocatedSize = ca.offHeapAllocatedSize();
        offHeapEntriesCnt = ca.offHeapEntriesCount();
        partitions = ca.affinity().partitions();
        metrics = new VisorCacheMetrics(ignite, ca.name()); // TODO: GG-11683 Move to separate thing
        near = cctx.isNear();

        estimateMemorySize(ca, sample);
    }

    /**
     * Estimate memory size used by cache.
     *
     * @param ca Cache adapter.
     * @param sample Sample size.
     * @throws IgniteCheckedException If estimation failed.
     */
    protected void estimateMemorySize(GridCacheAdapter ca, int sample) throws IgniteCheckedException {
        int size = ca.size();

        Iterable<GridCacheEntryEx> set = ca.context().isNear()
            ? ((GridNearCacheAdapter)ca).dht().entries()
            : ca.entries();

        long memSz = 0;

        Iterator<GridCacheEntryEx> it = set.iterator();

        int sz = sample > 0 ? sample : DFLT_CACHE_SIZE_SAMPLING;

        int cnt = 0;

        while (it.hasNext() && cnt < sz) {
            memSz += it.next().memorySize();

            cnt++;
        }

        if (cnt > 0)
            memSz = (long)((double)memSz / cnt * size);

        memorySize = memSz;
    }

    /**
     * @return New instance suitable to store in history.
     */
    public VisorCache history() {
        VisorCache c = new VisorCache();

        c.name = name;
        c.mode = mode;
        c.memorySize = memorySize;
        c.indexesSize = indexesSize;
        c.size = size;
        c.nearSize = nearSize;
        c.dhtSize = dhtSize;
        c.primarySize = primarySize;
        c.offHeapAllocatedSize = offHeapAllocatedSize;
        c.offHeapEntriesCnt = offHeapEntriesCnt;
        c.partitions = partitions;
        c.metrics = metrics;
        c.near = near;

        return c;
    }

    /**
     * @return Cache name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets new value for cache name.
     *
     * @param name New cache name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Dynamic deployment ID.
     */
    public IgniteUuid getDynamicDeploymentId() {
        return dynamicDeploymentId;
    }

    /**
     * @return Cache mode.
     */
    public CacheMode getMode() {
        return mode;
    }

    /**
     * @return Cache size in bytes.
     */
    public long getMemorySize() {
        return memorySize;
    }

    /**
     * @return Indexes size in bytes.
     */
    public long getIndexesSize() {
        return indexesSize;
    }

    /**
     * @return Number of all entries in cache.
     */
    public int getSize() {
        return size;
    }

    /**
     * @return Number of all entries in near cache.
     */
    public int getNearSize() {
        return nearSize;
    }

    /**
     * @return Number of all entries in DHT cache.
     */
    public int getDhtSize() {
        return dhtSize;
    }

    /**
     * @return Number of primary entries in cache.
     */
    public int getPrimarySize() {
        return primarySize;
    }

    /**
     * @return Memory size allocated in off-heap.
     */
    public long getOffHeapAllocatedSize() {
        return offHeapAllocatedSize;
    }

    /**
     * @return Number of cache entries stored in off-heap memory.
     */
    public long getOffHeapEntriesCount() {
        return offHeapEntriesCnt;
    }

    /**
     * @return Number of partitions.
     */
    public int getPartitions() {
        return partitions;
    }

    /**
     * @return Cache metrics.
     */
    public VisorCacheMetrics getMetrics() {
        return metrics;
    }

    /**
     * @return Cache partitions states.
     */
    @Nullable public VisorPartitionMap getPartitionMap() {
        return parts;
    }

    /**
     * @return {@code true} if cache has near cache.
     */
    public boolean isNear() {
        return near;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, name);
        U.writeGridUuid(out, dynamicDeploymentId);
        U.writeEnum(out, mode);
        out.writeLong(memorySize);
        out.writeLong(indexesSize);
        out.writeInt(size);
        out.writeInt(nearSize);
        out.writeInt(dhtSize);
        out.writeInt(primarySize);
        out.writeLong(offHeapAllocatedSize);
        out.writeLong(offHeapEntriesCnt);
        out.writeInt(partitions);
        out.writeBoolean(near);
        metrics.writeExternal(out);

        out.writeBoolean(parts != null);

        if (parts != null)
            parts.writeExternal(out);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        name = U.readString(in);
        dynamicDeploymentId = U.readGridUuid(in);
        mode = CacheMode.fromOrdinal(in.readByte());
        memorySize = in.readLong();
        indexesSize = in.readLong();
        size = in.readInt();
        nearSize = in.readInt();
        dhtSize = in.readInt();
        primarySize = in.readInt();
        offHeapAllocatedSize = in.readLong();
        offHeapEntriesCnt = in.readLong();
        partitions = in.readInt();
        near = in.readBoolean();

        metrics = new VisorCacheMetrics();
        metrics.readExternal(in);

        if (in.readBoolean()) {
            parts = new VisorPartitionMap();
            parts.readExternal(in);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorCache.class, this);
    }
}
