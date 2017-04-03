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

import java.io.*;
import org.apache.ignite.math.MatrixStorage;

/**
 * TODO: add description.
 */
public class MatrixDelegateStorage implements MatrixStorage {
    private MatrixStorage sto;
    private int rowOff, colOff;
    private int rows, cols;

    /**
     *
     */
    public MatrixDelegateStorage() {
        // No-op.
    }

    /**
     *
     * @param sto
     * @param rowOff
     * @param colOff
     * @param rows
     * @param cols
     */
    public MatrixDelegateStorage(MatrixStorage sto, int rowOff, int colOff, int rows, int cols) {
        assert sto != null;
        assert rowOff >= 0;
        assert colOff >= 0;
        assert rows > 0;
        assert cols > 0;
        
        this.sto = sto;

        this.rowOff = rowOff;
        this.colOff = colOff;

        this.rows = rows;
        this.cols = cols;
    }

    /**
     * 
     * @return
     */
    public MatrixStorage delegate() {
        return sto;
    }

    /**
     *
     * @return
     */
    public int rowOffset() {
        return rowOff;
    }

    /**
     *
     * @return
     */
    public int columnOffset() {
        return colOff;
    }

    /**
     *
     * @return
     */
    public int rowsLength() {
        return rows;
    }

    /**
     *
     * @return
     */
    public int columnsLength() {
        return cols;
    }

    /** {@inheritDoc} */
    @Override public double get(int x, int y) {
        return sto.get(rowOff + x, colOff + y);
    }

    /** {@inheritDoc} */
    @Override public void set(int x, int y, double v) {
        sto.set(rowOff + x, colOff + y, v);
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
        return sto.isArrayBased() && rowOff == 0 && colOff == 0;
    }

    /** {@inheritDoc} */
    @Override public boolean isSequentialAccess() {
        return sto.isSequentialAccess();
    }

    /** {@inheritDoc} */
    @Override public boolean isDense() {
        return sto.isDense();
    }

    /** {@inheritDoc} */
    @Override public boolean isRandomAccess() {
        return sto.isRandomAccess();
    }

    /** {@inheritDoc} */
    @Override public boolean isDistributed() {
        return sto.isDistributed();
    }

    /** {@inheritDoc} */
    @Override public double[][] data() {
        return sto.data();
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(sto);

        out.writeInt(rowOff);
        out.writeInt(colOff);

        out.writeInt(rows);
        out.writeInt(cols);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sto = (MatrixStorage)in.readObject();

        rowOff = in.readInt();
        colOff = in.readInt();

        rows = in.readInt();
        cols = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int res = 1;

        res = res * 37 + rows;
        res = res * 37 + cols;
        res = res * 37 + rowOff;
        res = res * 37 + colOff;
        res = res * 37 + sto.hashCode();

        return res;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        MatrixDelegateStorage that = (MatrixDelegateStorage) o;

        return rows == that.rows && cols == that.cols && rowOff == that.rowOff && colOff == that.colOff &&
            (sto != null ? sto.equals(that.sto) : that.sto == null);
    }
}