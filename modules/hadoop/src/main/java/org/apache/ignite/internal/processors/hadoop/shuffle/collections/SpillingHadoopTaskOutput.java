package org.apache.ignite.internal.processors.hadoop.shuffle.collections;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskOutput;
import org.apache.ignite.internal.util.worker.GridWorker;
import org.apache.ignite.thread.IgniteThread;

/**
 * "Changer" output that switches between several (at least 2) underlying outputs:
 * when one output is full, schedule it for spilling (writing to disk) and take another output.
 * Class is thread-safe, many threads can write to it.
 * One worker thread currently performs the spill work.
 */
public class SpillingHadoopTaskOutput implements HadoopTaskOutput {
    private final HadoopTaskOutput[] inOuts; // In fact, spillable outputs assumed.

    /** Current in-out we're writing into. */
    private final AtomicStampedReference<OutputAndSpills> curInOut = new AtomicStampedReference<>(null, 0);

    /** Full in-outs ready fro spilling. */
    private final BlockingQueue<OutputAndSpills> toBeSpilledQueue;

    /** Empty in-outs ready for writing. */
    private final BlockingQueue<OutputAndSpills> spilledQueue;

    /** */
    private final GridWorker spillWorker;

    /** */
    private final IgniteThread spillThread;

    /**
     * Constructor.
     */
    public SpillingHadoopTaskOutput(HadoopTaskOutput[] inOuts, String[] baseNames) {
        assert inOuts.length > 1; // at least 2

        this.inOuts = inOuts;

        this.toBeSpilledQueue = new ArrayBlockingQueue<>(inOuts.length);
        this.spilledQueue = new ArrayBlockingQueue<>(inOuts.length);

        // initially all in-outs assumed to be free (ready to write).
        // put all them to "spilled" queue, but the last one: it will be the current in-out:
        for (int i = 0; i<this.inOuts.length - 1; i++)
            spilledQueue.offer(new OutputAndSpills(inOuts[i], baseNames[i]));

        boolean set = curInOut.compareAndSet(null, spilledQueue.peek(), 0, 1);

        assert set;

        // Takes in-outs from "toBeSpilled" queue, spills them, then puts into "spilled" queue.
        spillWorker = new GridWorker("any-grid", "spill-worker", null, null) {
            @Override protected void body() {
                try {
                    while (!isCancelled()) {
                        OutputAndSpills io = toBeSpilledQueue.take();

                        assert io != null;

                        DataOutput dout = io.nextSpill();

                        try {
                            ((HadoopSpillable)io.out).spill(dout);
                        }
                        finally {
                            ((AutoCloseable)dout).close();
                        }

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

    private static class OutputAndSpills {
        final HadoopTaskOutput out;
        private final String base;
        private final List<String> spills = new ArrayList<>(4);

        OutputAndSpills(HadoopTaskOutput out0, String base) {
            this.out = out0;
            this.base = base;
        }

        private void nextSpillName() {
            int index = spills.size();

            spills.add(base + "-" + index);
        }

        /**
         * Generates *new* spill name and opens DataOutput on it.
         * @return the new spill output.
         * @throws IOException
         */
        DataOutput nextSpill() throws IOException {
            nextSpillName();

            String name = spills.get(spills.size() - 1);

            return new DataOutputStream(new FileOutputStream(name, false));
        }

        /**
         * Gets all spill names used.
         * @return
         */
        Collection<String> getSpills() {
            return spills;
        }
    }

    /**
     * Implements base logic of k-v pair writing.
     *
     * @param key
     * @param val
     * @return
     * @throws IgniteCheckedException
     * @throws InterruptedException
     */
    private boolean write0(Object key, Object val) throws IgniteCheckedException, InterruptedException {
        final int[] stampHolder = new int[1];

        while (true) {
            OutputAndSpills cur = curInOut.get(stampHolder);

            assert cur != null;

            if (cur.out.write(key, val))
                return true; // write succeeded.
            else {
                // Current in-out is full, we should spill it.
                OutputAndSpills other = spilledQueue.poll(10, TimeUnit.MILLISECONDS);

                if (other == null)
                    continue; // Likely the empty in-out is already taken by a concurrent thread, loop again.

                // Swap output:
                if (curInOut.compareAndSet(cur, other, stampHolder[0], stampHolder[0] + 1)) {
                    boolean put = toBeSpilledQueue.offer(cur); // schedule spilling

                    assert put;

                    // May be we should go to the next loop there?
                    return other.out.write(key, val); // write to fresh empty in-out.
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

        for (HadoopTaskOutput inOut: inOuts)
            inOut.close();
    }
}
