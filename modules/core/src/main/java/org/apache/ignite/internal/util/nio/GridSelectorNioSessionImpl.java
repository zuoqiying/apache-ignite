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

package org.apache.ignite.internal.util.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.typedef.internal.LT;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;
import org.jsr166.ConcurrentLinkedDeque8;

/**
 * Session implementation bound to selector API and socket API.
 * Note that this implementation requires non-null values for local and remote
 * socket addresses.
 */
class GridSelectorNioSessionImpl extends GridNioSessionImpl {
    /** */
    public static final int BUF_CHUNK_SIZE = IgniteSystemProperties.getInteger(
        "IGNITE_CONN_BUF_CHUNK_SIZE",
        256);
    public static final int IGNITE_CONN_BUF_CHUNK_CNT = IgniteSystemProperties.getInteger(
        "IGNITE_CONN_BUF_CHUNK_CNT",
        2048);

    public static final int READ_BUF_CHUNK_SIZE = IgniteSystemProperties.getInteger(
        "IGNITE_READ_BUF_CHUNK_SIZE",
        65536);
    public static final int IGNITE_READ_CONN_BUF_CHUNK_CNT = IgniteSystemProperties.getInteger(
        "IGNITE_READ_CONN_BUF_CHUNK_CNT",
        256);

    /** Pending write requests. */
    private final ConcurrentLinkedDeque8<GridNioFuture<?>> queue = new ConcurrentLinkedDeque8<>();

    /** Selection key associated with this session. */
    @GridToStringExclude
    private SelectionKey key;

    /** Worker index for server */
    private final int selectorIdx;

    /** Size counter. */
    private final AtomicInteger queueSize = new AtomicInteger();

    /** Semaphore. */
    @GridToStringExclude
    private final Semaphore sem;

    /** Write buffer. */
    private ByteBuffer writeBuf;

    /** Read buffer. */
    private ByteBuffer readBuf;

    /** Recovery data. */
    private GridNioRecoveryDescriptor recovery;

    /** Logger. */
    private final IgniteLogger log;

    /** */
    private final ByteBuffer pool = ByteBuffer.allocateDirect(
        BUF_CHUNK_SIZE *
            IGNITE_CONN_BUF_CHUNK_CNT);

    private final BufferChunk writeChunks[] = new BufferChunk[
        IGNITE_CONN_BUF_CHUNK_CNT];

    private final BufferChunk readChunks[] = new BufferChunk[
        IGNITE_READ_CONN_BUF_CHUNK_CNT];

    private final BufferChunkList chunkList;

    /**
     * Creates session instance.
     *
     * @param log Logger.
     * @param selectorIdx Selector index for this session.
     * @param filterChain Filter chain that will handle requests.
     * @param locAddr Local address.
     * @param rmtAddr Remote address.
     * @param accepted Accepted flag.
     * @param sndQueueLimit Send queue limit.
     * @param writeBuf Write buffer.
     * @param readBuf Read buffer.
     */
    GridSelectorNioSessionImpl(
        IgniteLogger log,
        int selectorIdx,
        GridNioFilterChain filterChain,
        InetSocketAddress locAddr,
        InetSocketAddress rmtAddr,
        boolean accepted,
        int sndQueueLimit,
        @Nullable ByteBuffer writeBuf,
        @Nullable ByteBuffer readBuf
    ) {
        super(filterChain, locAddr, rmtAddr, accepted);

        assert selectorIdx >= 0;
        assert sndQueueLimit >= 0;

        assert locAddr != null : "GridSelectorNioSessionImpl should have local socket address.";
        assert rmtAddr != null : "GridSelectorNioSessionImpl should have remote socket address.";

        assert log != null;

        this.log = log;

        this.selectorIdx = selectorIdx;

        sem = sndQueueLimit > 0 ? new Semaphore(sndQueueLimit) : null;

        if (writeBuf != null) {
            writeBuf.clear();

            this.writeBuf = writeBuf;
        }

        if (readBuf != null) {
            readBuf.clear();

            this.readBuf = readBuf;
        }

        for (int i = 0; i < writeChunks.length; i++) {
            //pool.position(i * BUF_CHUNK_SIZE).limit((i + 1) * BUF_CHUNK_SIZE);

            writeChunks[i] = new BufferChunk(i, ByteBuffer.allocateDirect(BUF_CHUNK_SIZE));
        }

        for (int i = 0; i < IGNITE_READ_CONN_BUF_CHUNK_CNT; i++) {
            readChunks[i] = new BufferChunk(i, ByteBuffer.allocateDirect(READ_BUF_CHUNK_SIZE));
        }

        chunkList = new BufferChunkList(Math.max(16, 65536 / BUF_CHUNK_SIZE));

        U.quietAndInfo(log, "info [chunkSize=" + BUF_CHUNK_SIZE + ", chunksCnt=" + writeChunks.length +
            ", chunkListSize=" + chunkList.maxSize() + ']');
    }

    /**
     * Sets selection key for this session.
     *
     * @param key Selection key.
     */
    void key(SelectionKey key) {
        assert this.key == null;

        this.key = key;
    }

    /**
     * @return Write buffer.
     */
    public ByteBuffer writeBuffer() {
        return writeBuf;
    }

    /**
     * @return Read buffer.
     */
    public ByteBuffer readBuffer() {
        return readBuf;
    }

    /**
     * @return Registered selection key for this session.
     */
    SelectionKey key() {
        return key;
    }

    /**
     * @return Selector index.
     */
    int selectorIndex() {
        return selectorIdx;
    }

    /**
     * Adds write future at the front of the queue without acquiring back pressure semaphore.
     *
     * @param writeFut Write request.
     * @return Updated size of the queue.
     */
    int offerSystemFuture(GridNioFuture<?> writeFut) {
        writeFut.messageThread(true);

        boolean res = queue.offerFirst(writeFut);

        assert res : "Future was not added to queue";

        return queueSize.incrementAndGet();
    }

    /**
     * Adds write future to the pending list and returns the size of the queue.
     * <p>
     * Note that separate counter for the queue size is needed because in case of concurrent
     * calls this method should return different values (when queue size is 0 and 2 concurrent calls
     * occur exactly one call will return 1)
     *
     * @param writeFut Write request to add.
     * @return Updated size of the queue.
     */
    int offerFuture(GridNioFuture<?> writeFut) {
        boolean msgThread = GridNioBackPressureControl.threadProcessingMessage();

        if (sem != null && !msgThread)
            sem.acquireUninterruptibly();

        writeFut.messageThread(msgThread);

        boolean res = queue.offer(writeFut);

        assert res : "Future was not added to queue";

        return queueSize.incrementAndGet();
    }

    /**
     * @param futs Futures to resend.
     */
    void resend(Collection<GridNioFuture<?>> futs) {
        assert queue.isEmpty() : queue.size();

        boolean add = queue.addAll(futs);

        assert add;

        boolean set = queueSize.compareAndSet(0, futs.size());

        assert set;
    }

    /**
     * @return Message that is in the head of the queue, {@code null} if queue is empty.
     */
    @Nullable GridNioFuture<?> pollFuture() {
        GridNioFuture<?> last = queue.poll();

        if (last != null) {
            queueSize.decrementAndGet();

            if (sem != null && !last.messageThread())
                sem.release();

            if (recovery != null) {
                if (!recovery.add(last)) {
                    LT.warn(log, null, "Unacknowledged messages queue size overflow, will attempt to reconnect " +
                        "[remoteAddr=" + remoteAddress() +
                        ", queueLimit=" + recovery.queueLimit() + ']');

                    if (log.isDebugEnabled())
                        log.debug("Unacknowledged messages queue size overflow, will attempt to reconnect " +
                            "[remoteAddr=" + remoteAddress() +
                            ", queueSize=" + recovery.messagesFutures().size() +
                            ", queueLimit=" + recovery.queueLimit() + ']');

                    close();
                }
            }
        }

        return last;
    }

    /**
     * @param fut Future.
     * @return {@code True} if future was removed from queue.
     */
    boolean removeFuture(GridNioFuture<?> fut) {
        assert closed();

        return queue.removeLastOccurrence(fut);
    }

    /**
     * Gets number of write requests in a queue that have not been processed yet.
     *
     * @return Number of write requests.
     */
    int writeQueueSize() {
        return queueSize.get();
    }

    /**
     * @return Write requests.
     */
    Collection<GridNioFuture<?>> writeQueue() {
        return queue;
    }

    /** {@inheritDoc} */
    @Override public void recoveryDescriptor(GridNioRecoveryDescriptor recoveryDesc) {
        assert recoveryDesc != null;

        recovery = recoveryDesc;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridNioRecoveryDescriptor recoveryDescriptor() {
        return recovery;
    }

    /** {@inheritDoc} */
    @Override public <T> T addMeta(int key, @Nullable T val) {
        if (val instanceof GridNioRecoveryDescriptor) {
            recovery = (GridNioRecoveryDescriptor)val;

            if (!accepted())
                recovery.connected();

            return null;
        }
        else
            return super.addMeta(key, val);
    }

    /**
     *
     */
    void onServerStopped() {
        onClosed();
    }

    /**
     *
     */
    void onClosed() {
        if (sem != null)
            sem.release(1_000_000);
    }

    /**
     * @return First buffer chunk.
     */
    public BufferChunk reserveWriteChunk() {
        int start = U.hash(Thread.currentThread().getId()) & (writeChunks.length - 1);

        return reserveWriteChunk(start); // TODO random start?
    }

    /**
     * @return First buffer chunk.
     */
    BufferChunk reserveWriteChunk(int start) {
        return reserveChunk(start, writeChunks);
    }

    public BufferChunk expandWriteChunk(BufferChunk chunk) {
        BufferChunk newChunk = reserveWriteChunk(chunk.index() + 1);

        if (newChunk == null)
            return null;

        // TODO alter passed in chunk if buffer is next to it

        chunk.next(newChunk.index());

        return newChunk;
    }

    /**
     * @return Buffer chunk to read.
     */
    public BufferChunk reserveReadChunk(int idx) {
        return reserveChunk(idx, readChunks); // TODO random start?
    }

    private BufferChunk reserveChunk(int start, BufferChunk[] chunks) {
        for (int i = start; i < chunks.length + start; i++) {
            BufferChunk chunk = chunks[i >= chunks.length ? i - chunks.length : i];

            if (chunk.reserve()) {
                if ((i - start) > 60)
                    U.debug(log, "iter on reserve: " + (i - start));

                return chunk;
            }
        }

        return null;

    }

    /**
     * @param idx
     * @return
     */
    BufferChunk writeChunk(int idx) {
        return writeChunks[idx];
    }

    /**
     * @return
     */
    public BufferChunkList list() {
        return chunkList;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridSelectorNioSessionImpl.class, this, super.toString());
    }
}
