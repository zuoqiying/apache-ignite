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
import org.apache.ignite.cache.eviction.EvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.internal.visor.util.VisorTaskUtils.compactClass;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.evictionPolicyMaxSize;

/**
 * Data transfer object for eviction configuration properties.
 */
public class VisorCacheEvictionConfiguration extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Eviction policy. */
    private String plc;

    /** Cache eviction policy max size. */
    private Integer plcMaxSize;

    /** Eviction filter to specify which entries should not be evicted. */
    private String filter;

    /** Synchronous eviction concurrency level. */
    private int syncConcurrencyLvl;

    /** Synchronous eviction timeout. */
    private long syncTimeout;

    /** Synchronized key buffer size. */
    private int syncKeyBufSize;

    /** Synchronous evicts flag. */
    private boolean evictSynchronized;

    /** Eviction max overflow ratio. */
    private float maxOverflowRatio;

    /**
     * Default constructor.
     */
    public VisorCacheEvictionConfiguration() {
        // No-op.
    }

    /**
     * Create data transfer object for eviction configuration properties.
     * @param ccfg Cache configuration.
     */
    public VisorCacheEvictionConfiguration(CacheConfiguration ccfg) {
        final EvictionPolicy evictionPlc = ccfg.getEvictionPolicy();

        plc = compactClass(evictionPlc);
        plcMaxSize = evictionPolicyMaxSize(evictionPlc);
        filter = compactClass(ccfg.getEvictionFilter());
        syncConcurrencyLvl = ccfg.getEvictSynchronizedConcurrencyLevel();
        syncTimeout = ccfg.getEvictSynchronizedTimeout();
        syncKeyBufSize = ccfg.getEvictSynchronizedKeyBufferSize();
        evictSynchronized = ccfg.isEvictSynchronized();
        maxOverflowRatio = ccfg.getEvictMaxOverflowRatio();
    }

    /**
     * @return Eviction policy.
     */
    @Nullable public String getPolicy() {
        return plc;
    }

    /**
     * @return Cache eviction policy max size.
     */
    @Nullable public Integer getPolicyMaxSize() {
        return plcMaxSize;
    }

    /**
     * @return Eviction filter to specify which entries should not be evicted.
     */
    @Nullable public String getFilter() {
        return filter;
    }

    /**
     * @return synchronized eviction concurrency level.
     */
    public int getSynchronizedConcurrencyLevel() {
        return syncConcurrencyLvl;
    }

    /**
     * @return synchronized eviction timeout.
     */
    public long getSynchronizedTimeout() {
        return syncTimeout;
    }

    /**
     * @return Synchronized key buffer size.
     */
    public int getSynchronizedKeyBufferSize() {
        return syncKeyBufSize;
    }

    /**
     * @return Synchronous evicts flag.
     */
    public boolean isEvictSynchronized() {
        return evictSynchronized;
    }

    /**
     * @return Eviction max overflow ratio.
     */
    public float getMaxOverflowRatio() {
        return maxOverflowRatio;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, plc);
        out.writeObject(plcMaxSize);
        U.writeString(out, filter);
        out.writeInt(syncConcurrencyLvl);
        out.writeLong(syncTimeout);
        out.writeInt(syncKeyBufSize);
        out.writeBoolean(evictSynchronized);
        out.writeFloat(maxOverflowRatio);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        plc = U.readString(in);
        plcMaxSize = (Integer)in.readObject();
        filter = U.readString(in);
        syncConcurrencyLvl = in.readInt();
        syncTimeout = in.readLong();
        syncKeyBufSize = in.readInt();
        evictSynchronized = in.readBoolean();
        maxOverflowRatio = in.readFloat();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorCacheEvictionConfiguration.class, this);
    }
}
