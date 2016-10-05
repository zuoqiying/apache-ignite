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

import java.util.Collection;
import java.util.UUID;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.configuration.MemoryPoolLink;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.pagemem.backup.StartFullBackupAckDiscoveryMessage;
import org.apache.ignite.internal.pagemem.impl.PageMemoryNoStoreImpl;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCacheSharedManagerAdapter;
import org.apache.ignite.internal.processors.cache.database.freelist.FreeList;
import org.apache.ignite.internal.processors.cache.database.freelist.FreeListImpl;
import org.apache.ignite.internal.processors.cache.database.tree.reuse.ReuseList;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsExchangeFuture;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;
import org.jsr166.ConcurrentHashMap8;

/**
 *
 */
public class IgniteCacheDatabaseSharedManager extends GridCacheSharedManagerAdapter {
    /** */
    protected PageMemory pageMem;

    /** Cache to free list. */
    protected ConcurrentHashMap8<Integer, FreeListImpl> cacheToFreeList = new ConcurrentHashMap8<>();

    /** Memory pool to free list. */
    protected ConcurrentHashMap8<MemoryPoolLink, FreeListImpl> memoryPoolToFreeList = new ConcurrentHashMap8<>();

    /** {@inheritDoc} */
    @Override protected void start0() throws IgniteCheckedException {
        MemoryConfiguration dbCfg = cctx.kernalContext().config().getMemoryConfiguration();

        if (!cctx.kernalContext().clientNode()) {
            if (dbCfg == null)
                dbCfg = new MemoryConfiguration();

            pageMem = initMemory(dbCfg);

            pageMem.start();

            initDataStructures();
        }
    }

    /**
     * @throws IgniteCheckedException If failed.
     */
    protected void initDataStructures() throws IgniteCheckedException {

    }

    /**
     * @param cacheId Cache id.
     */
    public FreeList freeList(int cacheId) {
        return getFreeListImpl(cacheId);
    }

    /**
     * @param cacheId Cache id.
     */
    public ReuseList reuseList(int cacheId) {
        return getFreeListImpl(cacheId);
    }

    /** {@inheritDoc} */
    @Override protected void stop0(boolean cancel) {
        if (pageMem != null)
            pageMem.stop();
    }


    /**
     * @param cacheId Cache id.
     */
    private FreeListImpl getFreeListImpl(int cacheId) {
        FreeListImpl freeList = cacheToFreeList.get(cacheId);

        if (freeList == null) {
            //actually there is only one FreeListImpl for each memory pool, cache to freeList map is used as index
            MemoryPoolLink pool = pageMem.getMemoryPool(cacheId);

            FreeListImpl poolFreeList = memoryPoolToFreeList.get(pool);

            if (poolFreeList == null) {
                try {
                    FreeListImpl newFreeList = new FreeListImpl(cacheId, "", pageMem, null, cctx.wal(), 0L, true);

                    FreeListImpl previousList = memoryPoolToFreeList.putIfAbsent(pool, newFreeList);

                    if (previousList != null)
                        freeList = previousList;
                    else
                        freeList = newFreeList;
                }
                catch (IgniteCheckedException e) {
                    throw new IllegalStateException();
                }
            } else
                freeList = poolFreeList;

            cacheToFreeList.putIfAbsent(cacheId, freeList);
        }

        return freeList;
    }

    /**
     *
     */
    public boolean persistenceEnabled() {
        return false;
    }

    /**
     * @return Page memory instance.
     */
    public PageMemory pageMemory() {
        return pageMem;
    }

    /**
     * No-op for non-persistent storage.
     */
    public void checkpointReadLock() {
        // No-op.
    }

    /**
     * No-op for non-persistent storage.
     */
    public void checkpointReadUnlock() {
        // No-op.
    }

    /**
     *
     */
    @Nullable public IgniteInternalFuture wakeupForCheckpoint(String reason) {
        return null;
    }

    /**
     * Waits until current state is checkpointed.
     *
     * @throws IgniteCheckedException If failed.
     */
    public void waitForCheckpoint(String reason) throws IgniteCheckedException {
        // No-op
    }

    /**
     *
     */
    @Nullable public IgniteInternalFuture wakeupForBackup(long backupId, UUID backupNodeId,
        Collection<String> cacheNames) {
        return null;
    }

    /**
     * @param discoEvt Before exchange for the given discovery event.
     */
    public void beforeExchange(GridDhtPartitionsExchangeFuture discoEvt) throws IgniteCheckedException {
        // No-op.
    }

    /**
     * @throws IgniteCheckedException If failed.
     */
    public void beforeCachesStop() throws IgniteCheckedException {
        // No-op.
    }

    /**
     * @param cctx Stopped cache context.
     */
    public void onCacheStop(GridCacheContext cctx) {
        // No-op
    }

    /**
     * @param backupMsg Backup message.
     * @param initiator Initiator node.
     * @return Backup init future or {@code null} if backup is not available.
     * @throws IgniteCheckedException If failed.
     */
    @Nullable public IgniteInternalFuture startLocalBackup(StartFullBackupAckDiscoveryMessage backupMsg, ClusterNode initiator)
        throws IgniteCheckedException {
        return null;
    }

    /**
     * @param dbCfg Database configuration.
     * @return Page memory instance.
     */
    protected PageMemory initMemory(MemoryConfiguration dbCfg) {
        return new PageMemoryNoStoreImpl(dbCfg, cctx, log, true);
    }
}
