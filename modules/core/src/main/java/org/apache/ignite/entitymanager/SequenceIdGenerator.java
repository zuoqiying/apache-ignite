package org.apache.ignite.entitymanager;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.Ignite;

/** */
public class SequenceIdGenerator implements IdGenerator<Long> {
    /** */
    //private IgniteAtomicSequence seq;
    private AtomicLong seq;

    /** Thread local counter. */
    private ThreadLocal<Long> threadLocCntr = new ThreadLocal<>();

    public static final int SEQ_BATCH = 8192;

    /** {@inheritDoc} */
    @Override public Long nextId() {
        Long val = threadLocCntr.get();
        if (val == null || val % SEQ_BATCH == SEQ_BATCH - 1)
            val = seq.getAndAdd(SEQ_BATCH);
        else
            val++;

        threadLocCntr.set(val);

        return val;
        //return seq.getAndIncrement();
    }

    /** {@inheritDoc} */
    public void attach(Ignite ignite, String name) {
        //seq = ignite.atomicSequence(name + "_seq", 0, true);
        seq = new AtomicLong();
    }
}