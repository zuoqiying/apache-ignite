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

package org.apache.ignite.math.impls.matrix;

import org.apache.ignite.math.*;
import org.apache.ignite.math.UnsupportedOperationException;
import org.apache.ignite.math.Vector;
import org.apache.ignite.math.impls.vector.DenseLocalOnHeapVector;
import org.apache.ignite.math.impls.storage.matrix.ArrayMatrixStorage;

import java.util.*;

/**
 * Basic implementation for matrix.
 * 
 * This is a trivial implementation for matrix assuming dense logic, local on-heap JVM storage
 * based on <code>double[][]</code> array. It is only suitable for data sets where
 * local, non-distributed execution is satisfactory and on-heap JVM storage is enough
 * to keep the entire data set.
 */
public class DenseLocalOnHeapMatrix extends AbstractMatrix {
    /**
     *
     */
    public DenseLocalOnHeapMatrix() {
        // No-op.
    }

   /**
     *
     * @param rows
     * @param cols
     */
    public DenseLocalOnHeapMatrix(int rows, int cols) {
        assert rows > 0;
        assert cols > 0;

        setStorage(new ArrayMatrixStorage(rows, cols));
    }

    /**
     *
     * @param mtx
     */
    public DenseLocalOnHeapMatrix(double[][] mtx) {
        assert mtx != null;

        setStorage(new ArrayMatrixStorage(mtx));
    }

    /**
     *
     * @param orig
     */
    private DenseLocalOnHeapMatrix(DenseLocalOnHeapMatrix orig) {
        assert orig != null;

        setStorage(new ArrayMatrixStorage(orig.rowSize(), orig.columnSize()));

        assign(orig);
    }

    /**
     *
     * @param args
     */
    public DenseLocalOnHeapMatrix(Map<String, Object> args) {
        assert args != null;

        if (args.containsKey("rows") && args.containsKey("cols"))
            setStorage(new ArrayMatrixStorage((int)args.get("rows"), (int)args.get("cols")));
        else if (args.containsKey("arr"))
            setStorage(new ArrayMatrixStorage((double[][])args.get("arr")));
        else
            throw new UnsupportedOperationException("Invalid constructor argument(s).");
    }

    @Override
    public Matrix copy() {
        return new DenseLocalOnHeapMatrix(this);
    }

    @Override
    public Matrix like(int rows, int cols) {
        return new DenseLocalOnHeapMatrix(rows, cols);
    }

    @Override
    public Vector likeVector(int crd) {
        return new DenseLocalOnHeapVector(crd);
    }
}