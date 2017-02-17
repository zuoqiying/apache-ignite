package org.apache.ignite.math.impls.storage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import it.unimi.dsi.fastutil.ints.Int2DoubleRBTreeMap;
import org.apache.ignite.math.Functions;
import org.apache.ignite.math.VectorStorage;

/**
 * TODO wip
 */
public class SequentialAccessSparseVectorStorage implements VectorStorage {

    private static final double DEFAULT_VALUE = 0.0;
    private Int2DoubleRBTreeMap data;
    /**
     * If true, doesn't allow DEFAULT_VALUEs in the mapping (adding a zero discards it). Otherwise, a DEFAULT_VALUE is
     * treated like any other value.
     */
    private boolean noDefault = true;

    /** For serialization. */
    public SequentialAccessSparseVectorStorage(){
        // No-op.
    }

    public SequentialAccessSparseVectorStorage(int[] keys, double[] values, boolean noDefault) {
        this.noDefault = noDefault;
        this.data = new Int2DoubleRBTreeMap(keys, values);
    }

    private SequentialAccessSparseVectorStorage(VectorStorage storage, boolean noDefault) {
        this.data = new Int2DoubleRBTreeMap();
        this.noDefault = noDefault;
        for (int i = 0; i < storage.size(); i++) {
            data.put(i, storage.get(i));
        }
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return data.size();
    }

    /** {@inheritDoc} */
    @Override public double get(int i) {
        return data.get(i);
    }

    /** {@inheritDoc} */
    @Override public void set(int i, double v) {
        data.put(i, v); // TODO wip, default/nodefault
    }

    /** {@inheritDoc} */
    @Override public double[] data() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        // TODO wip
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // TODO wip
    }

    /** {@inheritDoc} */
    @Override public boolean isSequentialAccess() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean isDense() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override public double getLookupCost() {
        return Math.max(1, Math.round(Functions.LOG2.apply(data.values().stream().filter(this::nonDefault).count())));
    }

    private boolean nonDefault(double x) {
        return Double.compare(x, DEFAULT_VALUE) != 0;
    }

    /** {@inheritDoc} */
    @Override public boolean isAddConstantTime() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean isArrayBased() {
        return false;
    }
}
