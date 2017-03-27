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

package org.apache.ignite.math.impls.storage.matrix;

import org.apache.ignite.math.MurmurHash;
import org.apache.ignite.math.exceptions.UnsupportedOperationException;
import org.apache.ignite.math.MatrixStorage;
import java.io.*;
import java.nio.*;

/**
 *
 */
public class RandomMatrixStorage implements MatrixStorage {
    private static final int PRIME1 = 104047;
    private static final int PRIME2 = 101377;
    private static final int PRIME3 = 64661;
    private static final long SCALE = 1L << 32;

    private int seed;
    private int rows, cols;
    private boolean fastHash;

    /**
     * For externalization.
     */
    public RandomMatrixStorage(){
        // No-op.
    }

    /**
     *
     * @param rows
     * @param cols
     * @param fastHash
     */
    public RandomMatrixStorage(int rows, int cols, boolean fastHash) {
        assert rows > 0;
        assert cols > 0;

        this.rows = rows;
        this.cols = cols;
        this.fastHash = fastHash;
    }

    /** {@inheritDoc} */
    @Override public double get(int x, int y) {
        if (!fastHash) {
            ByteBuffer buf = ByteBuffer.allocate(8);

            buf.putInt(x);
            buf.putInt(y);
            buf.flip();

            return (MurmurHash.hash64A(buf, seed) & (SCALE - 1)) / (double) SCALE;
        } else
            // This isn't a fantastic random number generator, but it is just fine for random projections.
            return ((((x * PRIME1) + y * PRIME2 + x * y * PRIME3) & 8) * 0.25) - 1;
    }

    /**
     *
     * @return
     */
    public boolean isFastHash() {
        return fastHash;
    }

    /** {@inheritDoc} */
    @Override public void set(int x, int y, double v) {
        throw new UnsupportedOperationException("Random matrix storage is a read-only storage.");
    }

    /** {@inheritDoc} */
    @Override public int columnSize() {
        return cols;
    }

    /** {@inheritDoc} */
    @Override public int rowSize() {
        return rows;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(rows);
        out.writeInt(cols);
        out.writeInt(seed);
        out.writeBoolean(fastHash);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        rows = in.readInt();
        cols = in.readInt();
        seed = in.readInt();
        fastHash = in.readBoolean();
    }

    /** {@inheritDoc} */
    @Override public boolean isSequentialAccess() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean isDense() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean isRandomAccess() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean isDistributed() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean isArrayBased() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int result = 1;

        result = result * 37 + Boolean.hashCode(fastHash);
        result = result * 37 + seed;
        result = result * 37 + cols;
        result = result * 37 + rows;

        return result;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        RandomMatrixStorage that = (RandomMatrixStorage) o;

        return rows == that.rows && cols == that.cols && seed == that.seed && fastHash == that.fastHash;
    }
}