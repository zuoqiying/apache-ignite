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

import java.nio.ByteBuffer;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;

/**
 * JCache metrics snapshot.
 */
public class JCacheMetricsSnapshot implements Message {
    /** */
    private static final long serialVersionUID = 0L;

    /** Number of puts. */
    private long puts = 0;

    /** Number of hits. */
    private long hits = 0;

    /** Number of misses. */
    private long misses = 0;

    /** Number of evictions. */
    private long evicts = 0;

    /** Number of removed entries. */
    private long removes = 0;

    /**
     * Default constructor.
     */
    public JCacheMetricsSnapshot() {
        // No-op.
    }

    /**
     * Create snapshot for the given {@code metrics}.
     *
     * @param metrics Cache metrics.
     */
    public JCacheMetricsSnapshot(CacheMetricsImpl metrics) {
        puts = metrics.getCachePuts();

        hits = metrics.getCacheHits();

        misses = metrics.getCacheMisses();

        evicts = metrics.getCacheEvictions();

        removes = metrics.getCacheRemovals();
    }

    /**
     * Create copy of the given {@code metrics}.
     *
     * @param metrics Cache metrics.
     */
    public JCacheMetricsSnapshot(JCacheMetricsSnapshot metrics) {
        puts = metrics.getCachePuts();

        hits = metrics.getCacheHits();

        misses = metrics.getCacheMisses();

        evicts = metrics.getCacheEvictions();

        removes = metrics.getCacheRemovals();
    }

    /**
     * The number of get requests that were satisfied by the cache.
     *
     * @return The number of hits.
     */
    public long getCacheHits() {
        return hits;
    }

    /**
     * A miss is a get request that is not satisfied.
     *
     * @return The number of misses.
     */
    public long getCacheMisses() {
        return misses;
    }

    /**
     * The total number of puts to the cache.
     *
     * @return The number of puts.
     */
    public long getCachePuts() {
        return puts;
    }

    /**
     * The total number of removals from the cache. This does not include evictions,
     * where the cache itself initiates the removal to make space.
     *
     * @return The number of removals.
     */
    public long getCacheRemovals() {
        return removes;
    }

    /**
     * The total number of evictions from the cache. An eviction is a removal
     * initiated by the cache itself to free up space. An eviction is not treated as
     * a removal and does not appear in the removal counts.
     *
     * @return The number of evictions.
     */
    public long getCacheEvictions() {
        return evicts;
    }

    /**
     * Updates this snapshot with the given {@code metrics}.
     *
     * @param metrics Metrics snapshot.
     * @return Updated instance of {@code JCacheMetricsSnapshot}
     */
    public JCacheMetricsSnapshot merge(JCacheMetricsSnapshot metrics) {
        if (metrics == null)
            return this;

        this.puts += metrics.puts;

        this.hits += metrics.hits;

        this.misses += metrics.misses;

        this.evicts += metrics.evicts;

        this.removes += metrics.removes;

        return this;
    }

    /**
     * Updates the given {@code metrics} using this snapshot.
     *
     * @param metrics Cache metrics to be updated.
     */
    public void updateCacheMetrics(CacheMetricsImpl metrics) {
        if (metrics == null)
            return;

        for (int i = 0; i < hits + misses; i++)
            metrics.onRead(i < hits);

        for (int i = 0; i < puts; i++)
            metrics.onWrite();

        for (int i = 0; i < evicts; i++)
            metrics.onEvict();

        for (int i = 0; i < removes; i++)
            metrics.onRemove();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(JCacheMetricsSnapshot.class, this);
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 0:
                if (!writer.writeLong("evicts", evicts))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeLong("hits", hits))
                    return false;

                writer.incrementState();

            case 2:
                if (!writer.writeLong("misses", misses))
                    return false;

                writer.incrementState();

            case 3:
                if (!writer.writeLong("puts", puts))
                    return false;

                writer.incrementState();

            case 4:
                if (!writer.writeLong("removes", removes))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        if (!reader.beforeMessageRead())
            return false;

        switch (reader.state()) {
            case 0:
                evicts = reader.readLong("evicts");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                hits = reader.readLong("hits");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 2:
                misses = reader.readLong("misses");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 3:
                puts = reader.readLong("puts");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 4:
                removes = reader.readLong("removes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(JCacheMetricsSnapshot.class);
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return -62;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 5;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }
}
