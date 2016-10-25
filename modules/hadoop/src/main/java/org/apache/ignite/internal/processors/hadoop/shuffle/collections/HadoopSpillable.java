package org.apache.ignite.internal.processors.hadoop.shuffle.collections;

import org.apache.ignite.IgniteCheckedException;

/**
 *
 */
public interface HadoopSpillable {

    void spill() throws IgniteCheckedException;

    void unSpill() throws IgniteCheckedException;
}
