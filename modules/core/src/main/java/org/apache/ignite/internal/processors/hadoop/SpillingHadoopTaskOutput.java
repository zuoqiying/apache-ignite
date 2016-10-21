package org.apache.ignite.internal.processors.hadoop;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.util.worker.GridWorker;
import org.apache.ignite.thread.IgniteThread;

/**
 * TODO: shall we also implement HadoopTaskOutput there?
 *
 * "Changer" output that switches between several underlying output:
 * when one output is full, schedule it for spilling and take another output.
 * Class is thread-safe, many threads can write to it.
 * One worker thread currently performs the spill work.
 */
public class SpillingHadoopTaskOutput implements HadoopTaskOutput {
    private final HadoopSpillableInputOutput[] inOuts;

    /** Current in-out we're writing into. */
    private final AtomicStampedReference<HadoopSpillableInputOutput> curInOut = new AtomicStampedReference<>(null, 0);

    /** */
    private final BlockingQueue<HadoopSpillableInputOutput> toBeSpilledQueue;

    /** */
    private final BlockingQueue<HadoopSpillableInputOutput> spilledQueue;

    /** */
    private final GridWorker spillWorker;

    /** */
    private final IgniteThread spillThread;

    /**
     * Constructor.
     */
    public SpillingHadoopTaskOutput(HadoopSpillableInputOutput[] inOuts) {
        assert inOuts.length > 1; // at least 2

        this.inOuts = inOuts;

        this.toBeSpilledQueue = new ArrayBlockingQueue<>(inOuts.length);
        this.spilledQueue = new ArrayBlockingQueue<>(inOuts.length);

        // initially all in-outs assumed to be free (ready to write).
        // put all them to "spilled" queue, but the last one: it will be the current in-out:
        for (int i = 0; i<this.inOuts.length - 1; i++)
            spilledQueue.offer(inOuts[i]);

        boolean set = curInOut.compareAndSet(null, inOuts[inOuts.length - 1], 0, 1);

        assert set;

        // Takes in-outs from "toBeSpilled" queue, spills them, then puts into "spilled" queue.
        spillWorker = new GridWorker("any-grid", "spill-worker", null, null) {
            @Override protected void body() {
                try {
                    while (!isCancelled()) {
                        HadoopSpillableInputOutput io = toBeSpilledQueue.take();

                        assert io != null;

                        io.spill();

                        boolean put = spilledQueue.offer(io);

                        assert put;
                    }
                }
                catch (Exception e) {
                    throw new IgniteException(e);
                }
            }
        };

        spillThread = new IgniteThread(spillWorker);

        spillThread.start();
    }

    /**
     * @param key
     * @param val
     * @return
     * @throws IgniteCheckedException
     * @throws InterruptedException
     */
    private boolean write0(Object key, Object val) throws IgniteCheckedException, InterruptedException {
        final int[] stampHolder = new int[1];

        while (true) {
            HadoopSpillableInputOutput cur = curInOut.get(stampHolder);

            assert cur != null;

            if (cur.getOut().write(key, val))
                return true; // write succeeded.
            else {
                // Current in-out is full, we should spill it.
                HadoopSpillableInputOutput other = spilledQueue.poll(10, TimeUnit.MILLISECONDS);

                if (other == null)
                    continue; // Likely the empty in-out is already taken by a concurrent thread, loop again.

                // Swap output:
                if (curInOut.compareAndSet(cur, other, stampHolder[0], stampHolder[0] + 1)) {
                    boolean put = toBeSpilledQueue.offer(cur); // schedule spilling

                    assert put;

                    // May be we should go to the next loop there?
                    return other.getOut().write(key, val); // write to fresh empty in-out.
                }
                else {
                    // Return the taken in-out back:
                    boolean returned = spilledQueue.offer(other);

                    assert returned;
                    // And go to next loop.
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override public boolean write(Object key, Object val) throws IgniteCheckedException {
        try {
            if (!write0(key, val))
                // If this failed, we're in trouble:
                throw new IgniteException("Write failed.");
        }
        catch (InterruptedException ie) {
            throw new IgniteCheckedException(ie);
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public void close() throws IgniteCheckedException {
        spillWorker.cancel();

        try {
            spillThread.join();
        }
        catch (InterruptedException ie) {
            throw new IgniteCheckedException(ie);
        }

        for (HadoopSpillableInputOutput inOut: inOuts)
            inOut.close();

    }
}
