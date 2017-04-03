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

import org.apache.ignite.math.Matrix;
import org.apache.ignite.math.Vector;
import org.apache.ignite.math.impls.storage.matrix.DenseOffHeapMatrixStorage;
import org.apache.ignite.math.impls.vector.DenseLocalOffHeapVector;

/**
 * TODO add description
 */
public class DenseLocalOffHeapMatrix extends AbstractMatrix {
    /** */
    public DenseLocalOffHeapMatrix(){
        // No-op.
    }

    /**
     *
     * @param data
     */
    public DenseLocalOffHeapMatrix(double[][] data){
        assert data != null;

        setStorage(new DenseOffHeapMatrixStorage(data));
    }

    /**
     *
     * @param rows
     * @param cols
     */
    public DenseLocalOffHeapMatrix(int rows, int cols){
        assert rows > 0;
        assert cols > 0;

        setStorage(new DenseOffHeapMatrixStorage(rows, cols));
    }

    /** {@inheritDoc} */
    @Override public Matrix copy() {
        DenseLocalOffHeapMatrix copy = new DenseLocalOffHeapMatrix(getStorage().rowSize(), getStorage().columnSize());

        copy.assign(this);

        return copy;
    }

    /** {@inheritDoc} */
    @Override public Matrix like(int rows, int cols) {
        return new DenseLocalOffHeapMatrix(rows, cols);
    }

    /** {@inheritDoc} */
    @Override public Vector likeVector(int crd) {
        return new DenseLocalOffHeapVector(crd);
    }

    /** {@inheritDoc} */
    @Override public void destroy() {
        getStorage().destroy();
    }
}