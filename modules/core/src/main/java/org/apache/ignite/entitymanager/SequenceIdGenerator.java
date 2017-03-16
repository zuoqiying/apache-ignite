package org.apache.ignite.entitymanager;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;

/** */
public class SequenceIdGenerator implements IdGenerator<Long> {
    /** */
    private IgniteAtomicSequence seq;

    /** {@inheritDoc} */
    @Override public Long nextId() {
        return seq.getAndIncrement();
    }

    /** {@inheritDoc} */
    public void attach(Ignite ignite, String name) {
        seq = ignite.atomicSequence(name + "_seq", 0, true);
    }
}