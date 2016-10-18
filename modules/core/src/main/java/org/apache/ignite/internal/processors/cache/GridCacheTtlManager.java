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

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearCacheEntry;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.GridConcurrentSkipListSet;
import org.apache.ignite.internal.util.lang.IgniteInClosure2X;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;
import org.jsr166.LongAdder8;

/**
 * Eagerly removes expired entries from cache when
 * {@link CacheConfiguration#isEagerTtl()} flag is set.
 */
@SuppressWarnings("NakedNotify")
public class GridCacheTtlManager extends GridCacheManagerAdapter {
    /** Entries pending removal. */
    private  GridConcurrentSkipListSetEx pendingEntries;

    /** */
    private final IgniteInClosure2X<GridCacheEntryEx, GridCacheVersion> expireC =
            new IgniteInClosure2X<GridCacheEntryEx, GridCacheVersion>() {
                @Override public void applyx(GridCacheEntryEx entry, GridCacheVersion obsoleteVer)
                        throws IgniteCheckedException {
                    boolean touch = !entry.isNear();

                    while (true) {
                        try {
                            if (log.isTraceEnabled())
                                log.trace("Trying to remove expired entry from cache: " + entry);

                            if (entry.onTtlExpired(obsoleteVer))
                                touch = false;

                            break;
                        }
                        catch (GridCacheEntryRemovedException e0) {
                            entry = entry.context().cache().entryEx(entry.key());

                            touch = true;
                        }
                    }

                    if (touch)
                        entry.context().evicts().touch(entry, null);
                }
            };

    /** {@inheritDoc} */
    @Override protected void start0() throws IgniteCheckedException {
        boolean cleanupDisabled = cctx.kernalContext().isDaemon() ||
            !cctx.config().isEagerTtl() ||
            CU.isAtomicsCache(cctx.name()) ||
            CU.isMarshallerCache(cctx.name()) ||
            CU.isUtilityCache(cctx.name()) ||
            (cctx.kernalContext().clientNode() && cctx.config().getNearConfiguration() == null);

        if (cleanupDisabled)
            return;

        pendingEntries = cctx.config().getNearConfiguration() != null ? new GridConcurrentSkipListSetEx() : null;
        cctx.shared().ttlCleanup().register(cctx.ttl());
    }

    /**
     * @return {@code True} if eager expired entries cleanup is enabled for cache.
     */
    public boolean eagerTtlEnabled() {
        assert cctx != null : "Manager not started";

        return cctx.config().isEagerTtl();
    }

    /** {@inheritDoc} */
    @Override protected void onKernalStop0(boolean cancel) {
        if (null != pendingEntries)
            pendingEntries.clear();
        cctx.shared().ttlCleanup().unregister(cctx.ttl());
    }

    /**
     * Adds tracked entry to ttl processor.
     *
     * @param entry Entry to add.
     */
    void addTrackedEntry(GridNearCacheEntry entry) {
        assert Thread.holdsLock(entry);
        assert pendingEntries != null;

        EntryWrapper e = new EntryWrapper(entry);

        pendingEntries.add(e);
    }

    /**
     * @param entry Entry to remove.
     */
    void removeTrackedEntry(GridNearCacheEntry entry) {
        assert Thread.holdsLock(entry);
        assert pendingEntries != null;

        pendingEntries.remove(new EntryWrapper(entry));
    }

    /**
     * @return The size of pending entries.
     * @throws IgniteCheckedException If failed.
     */
    public long pendingSize() throws IgniteCheckedException {
        return (pendingEntries != null ? pendingEntries.sizex() : 0) + cctx.offheap().expiredSize();
    }

    /** {@inheritDoc} */
    @Override public void printMemoryStats() {
        try {
            X.println(">>>");
            X.println(">>> TTL processor memory stats [grid=" + cctx.gridName() + ", cache=" + cctx.name() + ']');
            X.println(">>>   pendingEntriesSize: " + pendingSize());
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to print statistics: " + e, e);
        }
    }

    /**
     * Expires entries by TTL.
     */
    public void expire() {
        expire(-1);
    }

    /**
     * Processes specified amount of expired entries.
     *
     * @param amount limit of processed entries by single call; '-1' - no limit.
     * @return boolean true, if unprocessed expired entries remains
     */
    public boolean expire(int amount) {
        long now = U.currentTimeMillis();

        try {
            if (pendingEntries != null) {
                GridCacheVersion obsoleteVer = null;

                int limit = (-1 != amount) ? amount : pendingEntries.sizex();

                for (int count = limit; count > 0; count--) {
                    EntryWrapper e = pendingEntries.firstx();

                    if (e == null || e.expireTime > now)
                        // all expired entries are processed
                        return false;

                    if (pendingEntries.remove(e)) {
                        if (obsoleteVer == null)
                            obsoleteVer = cctx.versions().next();

                        expireC.apply(e.entry, obsoleteVer);
                    }
                }
            }

            cctx.offheap().expire(expireC);
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to process entry expiration: " + e, e);
        }

        if ((-1 == amount) || (pendingEntries == null)) {
            // we processed all expired entries
            return false;
        } else {
            // check if expired entries remains in pending queue
            EntryWrapper e = pendingEntries.firstx();
            return e != null && e.expireTime <= now;
        }
    }

    /**
     * @param cctx1 First cache context.
     * @param key1 Left key to compare.
     * @param cctx2 Second cache context.
     * @param key2 Right key to compare.
     * @return Comparison result.
     */
    private static int compareKeys(GridCacheContext cctx1, CacheObject key1, GridCacheContext cctx2, CacheObject key2) {
        int key1Hash = key1.hashCode();
        int key2Hash = key2.hashCode();

        int res = Integer.compare(key1Hash, key2Hash);

        if (res == 0) {
            key1 = (CacheObject)cctx1.unwrapTemporary(key1);
            key2 = (CacheObject)cctx2.unwrapTemporary(key2);

            try {
                byte[] key1ValBytes = key1.valueBytes(cctx1.cacheObjectContext());
                byte[] key2ValBytes = key2.valueBytes(cctx2.cacheObjectContext());

                // Must not do fair array comparison.
                res = Integer.compare(key1ValBytes.length, key2ValBytes.length);

                if (res == 0) {
                    for (int i = 0; i < key1ValBytes.length; i++) {
                        res = Byte.compare(key1ValBytes[i], key2ValBytes[i]);

                        if (res != 0)
                            break;
                    }
                }

                if (res == 0)
                    res = Boolean.compare(cctx1.isNear(), cctx2.isNear());
            }
            catch (IgniteCheckedException e) {
                throw new IgniteException(e);
            }
        }

        return res;
    }

    /**
     * Entry wrapper.
     */
    private static class EntryWrapper implements Comparable<EntryWrapper> {
        /** Entry expire time. */
        private final long expireTime;

        /** Entry. */
        private final GridCacheMapEntry entry;

        /**
         * @param entry Cache entry to create wrapper for.
         */
        private EntryWrapper(GridCacheMapEntry entry) {
            expireTime = entry.expireTimeUnlocked();

            assert expireTime != 0;

            this.entry = entry;
        }

        /** {@inheritDoc} */
        @Override public int compareTo(EntryWrapper o) {
            int res = Long.compare(expireTime, o.expireTime);

            if (res == 0)
                res = compareKeys(entry.context(), entry.key(), o.entry.context(), o.entry.key());

            return res;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof EntryWrapper))
                return false;

            EntryWrapper that = (EntryWrapper)o;

            return expireTime == that.expireTime &&
                compareKeys(entry.context(), entry.key(), that.entry.context(), that.entry.key()) == 0;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int res = (int)(expireTime ^ (expireTime >>> 32));

            res = 31 * res + entry.key().hashCode();

            return res;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(EntryWrapper.class, this);
        }
    }

    /**
     * Provides additional method {@code #sizex()}. NOTE: Only the following methods supports this addition:
     * <ul>
     *     <li>{@code #add()}</li>
     *     <li>{@code #remove()}</li>
     *     <li>{@code #pollFirst()}</li>
     * <ul/>
     */
    private static class GridConcurrentSkipListSetEx extends GridConcurrentSkipListSet<EntryWrapper> {
        /** */
        private static final long serialVersionUID = 0L;

        /** Size. */
        private final LongAdder8 size = new LongAdder8();

        /**
         * @return Size based on performed operations.
         */
        public int sizex() {
            return size.intValue();
        }

        /** {@inheritDoc} */
        @Override public boolean add(EntryWrapper e) {
            boolean res = super.add(e);

            if (res)
                size.increment();

            return res;
        }

        /** {@inheritDoc} */
        @Override public boolean remove(Object o) {
            boolean res = super.remove(o);

            if (res)
                size.decrement();

            return res;
        }

        /** {@inheritDoc} */
        @Nullable @Override public EntryWrapper pollFirst() {
            EntryWrapper e = super.pollFirst();

            if (e != null)
                size.decrement();

            return e;
        }
    }
}
