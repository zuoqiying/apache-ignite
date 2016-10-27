package org.apache.ignite.internal.processors.hadoop.shuffle.collections;

import java.io.DataInput;
import java.io.DataOutput;
import org.apache.ignite.IgniteCheckedException;

/**
 *
 */
public interface HadoopSpillable {
    /**
     * Spills (writes) this spillable into a given output.
     * Note that output is not closed upon this method return.
     * @param dout
     * @return
     * @throws IgniteCheckedException
     */
    long spill(DataOutput dout) throws IgniteCheckedException;

    /**
     *
     * @param din
     * @return
     * @throws IgniteCheckedException
     */
    long unSpill(DataInput din) throws IgniteCheckedException;
}
