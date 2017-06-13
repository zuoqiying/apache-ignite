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

package org.apache.ignite.ml.math.impls.storage.matrix;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import org.apache.ignite.ml.math.MatrixStorage;
import org.apache.ignite.ml.math.StorageConstants;
import org.apache.ignite.ml.math.functions.IgniteIntIntToIntBiFunction;
import org.apache.ignite.ml.math.util.MatrixUtil;

/**
 * Array based {@link MatrixStorage} implementation.
 */
public class ArrayMatrixStorage implements MatrixStorage {
    /** Backing data array. */
    private double[] data;
    /** Amount of rows in the matrix. */
    private int rows;
    /** Amount of columns in the matrix. */
    private int cols;
    /** Mode specifying if this matrix is row-major or column-major. */
    private int acsMode;
    /** Index mapper */
    private IgniteIntIntToIntBiFunction indexMapper;

    /**
     *
     */
    public ArrayMatrixStorage() {
        // No-op.
    }

    /**
     * @param rows Amount of rows in the matrix.
     * @param cols Amount of columns in the matrix.
     */
    public ArrayMatrixStorage(int rows, int cols) {
        this(rows, cols, StorageConstants.ROW_STORAGE_MODE);
    }

    public ArrayMatrixStorage(int rows, int cols, int acsMode) {
        assert rows > 0;
        assert cols > 0;

        this.data = new double[rows * cols];
        this.rows = rows;
        this.cols = cols;
        indexMapper = indexMapper(acsMode);
        this.acsMode = acsMode;
    }

    /**
     * @param data Backing data array.
     */
    public ArrayMatrixStorage(double[][] data, int acsMode) {
        this(MatrixUtil.flatten(data, acsMode), data.length, acsMode);
    }

    /**
     * @param data Backing data array.
     */
    public ArrayMatrixStorage(double[][] data) {
        this(MatrixUtil.flatten(data, StorageConstants.ROW_STORAGE_MODE), data.length);
    }

    /**
     * @param data Backing data array.
     */
    public ArrayMatrixStorage(double[] data, int rows, int acsMode) {
        assert data != null;
        assert data.length % rows == 0;

        this.data = data;
        this.rows = rows;
        this.cols = data.length / rows;
        indexMapper = indexMapper(acsMode);
        this.acsMode = acsMode;

        assert rows > 0;
        assert cols > 0;
    }

    /**
     * @param data Backing data array.
     */
    public ArrayMatrixStorage(double[] data, int rows) {
        this(data, rows, StorageConstants.ROW_STORAGE_MODE);
    }

    /** {@inheritDoc} */
    @Override public double get(int x, int y) {
        return data[indexMapper.apply(x, y)];
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
    @Override public void set(int x, int y, double v) {
        data[indexMapper.apply(x, y)] = v;
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
    @Override public boolean isArrayBased() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public double[] data() {
        return data;
    }

    /**
     * Get the index mapper for given access mode.
     *
     * @param acsMode Access mode.
     */
    private IgniteIntIntToIntBiFunction indexMapper(int acsMode) {
        return acsMode == StorageConstants.ROW_STORAGE_MODE ? (r, c) -> r * cols + c :
            (r, c) -> c * rows + r;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(rows);
        out.writeInt(cols);
        out.writeInt(acsMode);

        out.writeObject(data);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        rows = in.readInt();
        cols = in.readInt();
        acsMode = in.readInt();
        indexMapper = indexMapper(acsMode);

        data = (double[])in.readObject();
    }

    /** Get the access mode of this storage. */
    public int accessMode() {
        return acsMode;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int res = 1;

        res += res * 37 + rows;
        res += res * 37 + cols;
        res += res * 37 + acsMode;
        res += res * 37 + Arrays.hashCode(data);

        return res;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        ArrayMatrixStorage that = (ArrayMatrixStorage)o;

        return acsMode == that.acsMode && Arrays.equals(data, that.data);
    }
}
