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

package org.apache.ignite.math.decompositions;

import org.apache.ignite.math.Matrix;
import org.apache.ignite.math.Vector;
import org.apache.ignite.math.impls.matrix.DenseLocalOnHeapMatrix;
import org.apache.ignite.math.impls.matrix.PivotedMatrixView;
import org.apache.ignite.math.impls.matrix.RandomMatrix;
import org.apache.ignite.math.impls.vector.DenseLocalOnHeapVector;

public abstract class DecompositionSupport {
    /**
     * Create the like matrix with read-only matrices support.
     *
     * @param matrix Matrix for like.
     * @return Like matrix.
     */
    protected Matrix like(Matrix matrix){
        if (matrix instanceof RandomMatrix || matrix instanceof PivotedMatrixView)
            return new DenseLocalOnHeapMatrix(matrix.rowSize(), matrix.columnSize());
        else
            return matrix.like(matrix.rowSize(), matrix.columnSize());
    }

    /**
     * Create the like vector with read-only matrices support.
     *
     * @param matrix Matrix for like.
     * @return Like vector.
     */
    protected Vector likeVector(Matrix matrix){
        if (matrix instanceof RandomMatrix || matrix instanceof PivotedMatrixView)
            return new DenseLocalOnHeapVector(matrix.rowSize());
        else
            return matrix.likeVector(matrix.rowSize());
    }

    /**
     * Create the copy of matrix with read-only matrices support.
     *
     * @param matrix Matrix for copy.
     * @return Copy.
     */
    protected Matrix copy(Matrix matrix){
        if (matrix instanceof RandomMatrix){
            DenseLocalOnHeapMatrix cp = new DenseLocalOnHeapMatrix(matrix.rowSize(), matrix.columnSize());

            cp.assign(matrix);

            return cp;
        } else
            return matrix.copy();
    }
}