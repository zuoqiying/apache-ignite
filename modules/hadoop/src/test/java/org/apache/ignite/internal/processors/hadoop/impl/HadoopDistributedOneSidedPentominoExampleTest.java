package org.apache.ignite.internal.processors.hadoop.impl;

import org.apache.hadoop.examples.dancing.OneSidedPentomino;

/**
 * One-sided pentomino example in form of test.
 */
public class HadoopDistributedOneSidedPentominoExampleTest extends HadoopDistributedPentominoExampleTest {
    /** {@inheritDoc} */
    @Override protected int width() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected int height() {
        return 30;
    }

    /** {@inheritDoc} */
    @Override protected int expectedSolutionCount() {
        return 92;
    }

    /** {@inheritDoc} */
    @Override protected Class<?> pentominoClass() {
        return OneSidedPentomino.class;
    }
}