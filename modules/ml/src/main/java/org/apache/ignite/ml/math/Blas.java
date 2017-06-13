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

package org.apache.ignite.ml.math;

import com.github.fommil.netlib.BLAS;

import com.github.fommil.netlib.F2jBLAS;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Set;
import org.apache.ignite.ml.math.exceptions.CardinalityException;
import org.apache.ignite.ml.math.exceptions.MathIllegalArgumentException;
import org.apache.ignite.ml.math.exceptions.NonSquareMatrixException;
import org.apache.ignite.ml.math.impls.matrix.DenseLocalOnHeapMatrix;
import org.apache.ignite.ml.math.impls.matrix.SparseLocalOnHeapMatrix;
import org.apache.ignite.ml.math.impls.vector.DenseLocalOnHeapVector;
import org.apache.ignite.ml.math.impls.vector.SparseLocalVector;

/**
 * Useful subset of BLAS operations.
 * This class is based on 'BLAS' class from Apache Spark MLlib.
 */
public class Blas {
    /** F2J implementation of BLAS. */
    transient static private BLAS f2jBlas = new F2jBLAS();

    /** Native implementation of BLAS. F2J implementation will be used as fallback if no native implementation is found
     */
    transient static private BLAS nativeBlas = BLAS.getInstance();

    /**
     * Performs y += a * x
     *
     * @param a Scalar a.
     * @param x Vector x.
     * @param y Vector y.
     */
    public static void axpy(Double a, Vector x, Vector y) {
        if (x.size() != y.size())
            throw new CardinalityException(x.size(), y.size());

        if (x.isArrayBased() && y.isArrayBased())
            axpy(a, x.getStorage().data(), y.getStorage().data());
        else if (x instanceof SparseLocalVector && y.isArrayBased())
            axpy(a, (SparseLocalVector)x, y.getStorage().data());
        else
            throw new MathIllegalArgumentException("Operation 'axpy' doesn't support this combintaion of parameters");
    }

    /** */
    private static void axpy(Double a, double[] x, double[] y) {
        f2jBlas.daxpy(x.length, a, x, 1, y, 1);
    }

    /** */
    private static void axpy(Double a, SparseLocalVector x, double[] y) {
        int xSize = x.size();

        if (a == 1.0) {
            int k = 0;

            while (k < xSize) {
                y[k] += x.getX(k);
                k++;
            }
        } else {
            int k = 0;

            while (k < xSize) {
                y[k] += a * x.getX(k);
                k++;
            }
        }
    }

    /**
     * Returns dot product of vectors x and y.
     *
     * @param x Vector x.
     * @param y Vector y.
     * @return Dot product of x and y.
     **/
    public static Double dot(Vector x, Vector y) {
        return x.dot(y);
    }

    /**
     * Copies Vector x into Vector y. (y = x)
     *
     * @param x Vector x.
     * @param y Vector y.
     */
    public void copy(Vector x, Vector y) {
        int n = y.size();

        if (x.size() != n)
            throw new CardinalityException(x.size(), n);

        if (y.isArrayBased()) {
            double[] yData = y.getStorage().data();
            if (x.isArrayBased())
                System.arraycopy(x.getStorage().data(), 0, y.getStorage().data(), 0, n);
            else {
                if (y instanceof SparseLocalVector) {
                    for (int i = 0; i < n; i++)
                        yData[i] = x.getX(i);
                }
            }
        } else
            throw new IllegalArgumentException("y must be array based in copy");
    }


    /**
     * Performs in-place multiplication of vector x by a real scalar a. (x = a * x)
     *
     * @param a Scalar a.
     * @param x Vector x.
     **/
    public static void scal(Double a, Vector x) {
        if (x.isArrayBased())
            f2jBlas.dscal(x.size(), a, x.getStorage().data(), 1);
        else if (x instanceof SparseLocalVector) {
            Set<Integer> indexes = ((SparseLocalVector)x).indexes();

            for (Integer i : indexes)
                x.compute(i, (ind, v) -> v * a);
        } else
            throw new IllegalArgumentException();
    }

    /**
     * Adds alpha * v * v.t to a matrix in-place. This is the same as BLAS's ?SPR.
     *
     * @param u the upper triangular part of the matrix in a [[DenseVector]](column major)
     */
    public static void spr(Double alpha, DenseLocalOnHeapVector v, DenseLocalOnHeapVector u) {
        nativeBlas.dspr("U", v.size(), alpha, v.getStorage().data(), 1, u.getStorage().data());
    }

    /** */
    public static void spr(Double alpha, SparseLocalVector v, DenseLocalOnHeapVector u) {
        int prevNonDfltInd = 0;
        int startInd = 0;
        double av;
        double[] uData = u.getStorage().data();

        for (Integer nonDefaultInd : v.indexes()) {
            startInd += (nonDefaultInd - prevNonDfltInd) * (nonDefaultInd + prevNonDfltInd + 1) / 2;
            av = alpha * v.get(nonDefaultInd);

            for (Integer i : v.indexes())
                if (i <= nonDefaultInd)
                    uData[startInd + i] += av * v.getX(i);

            prevNonDfltInd = nonDefaultInd;
        }
    }

    /**
     * A := alpha * x * x^T + A
     * @param alpha a real scalar that will be multiplied to x * x^T^.
     * @param x the vector x that contains the n elements.
     * @param a the symmetric matrix A. Size of n x n.
     */
    void syr(Double alpha, Vector x, DenseLocalOnHeapMatrix a) {
        int mA = a.rowSize();
        int nA = a.columnSize();

        if (mA != nA)
            throw new NonSquareMatrixException(mA, nA);

        if (mA != x.size())
            throw new CardinalityException(x.size(), mA);

        // TODO: Process DenseLocalOffHeapVector
        if (x instanceof DenseLocalOnHeapVector)
            syr(alpha, x, a);
        else if (x instanceof SparseLocalVector)
            syr(alpha, x, a);
        else
            throw new IllegalArgumentException("Operation 'syr' does not support vector class " + x.getClass().getName());
    }

    /** */
    static void syr(Double alpha, DenseLocalOnHeapVector x, DenseLocalOnHeapMatrix a) {
        int nA = a.rowSize();
        int mA = a.columnSize();

        nativeBlas.dsyr("U", x.size(), alpha, x.getStorage().data(), 1, a.getStorage().data(), nA);

        // Fill lower triangular part of A
        int i = 0;
        while (i < mA) {
            int j = i + 1;

            while (j < nA) {
                a.setX(i, j, a.getX(j, i));
                j++;
            }
            i++;
        }
    }

    /** */
    public static void syr(Double alpha, SparseLocalVector x, DenseLocalOnHeapMatrix a) {
        int mA = a.columnSize();

        for (Integer i : x.indexes()) {
            double mult = alpha * x.getX(i);
            for (Integer j : x.indexes())
                a.getStorage().data()[mA * i + j] += mult * x.getX(j);
        }
    }

    /**
     * For the moment we have no flags indicating if matrix is transposed or not. Therefore all dgemm parameters for
     * transposition are equal to 'N'.
     */
    public static void gemm(Double alpha, Matrix a, DenseLocalOnHeapMatrix b, Double beta, DenseLocalOnHeapMatrix c) {
        if (alpha == 0.0 && beta == 1.0)
            return;
        else if (alpha == 0.0)
            scal(c, beta);
        else {
            if (a instanceof SparseLocalOnHeapMatrix)
                gemm(alpha, (SparseLocalOnHeapMatrix)a, b, beta, c);
            else if (a instanceof DenseLocalOnHeapMatrix) {
                double[] fA = a.getStorage().data();
                double[] fB = b.getStorage().data();
                double[] fC = c.getStorage().data();

                nativeBlas.dgemm("N", "N", a.rowSize(), b.columnSize(), a.columnSize(), alpha, fA,
                    a.rowSize(), fB, b.rowSize(), beta, fC, c.rowSize());
            } else
                throw new IllegalArgumentException("Operation 'gemm' doesn't support matrix type "
                    + a.getClass().getName());
        }
    }

    /**
     * C := alpha * A * B + beta * C
     * For `SparseMatrix` A.
     */
    private static void gemm(Double alpha, SparseLocalOnHeapMatrix a, DenseLocalOnHeapMatrix b, Double beta,
        DenseLocalOnHeapMatrix c) {
        int mA = a.rowSize();
        int nB = b.columnSize();
        int kA = a.columnSize();
        int kB = b.rowSize();

        if (kA != kB)
            throw new CardinalityException(kA, kB);

        if (mA != c.rowSize())
            throw new CardinalityException(mA, c.rowSize());

        if (nB != c.columnSize())
            throw new CardinalityException(nB, c.columnSize());

        if (beta != 1.0)
            scal(c, beta);

        Int2ObjectArrayMap<IntSet> im = a.indexesMap();
        IntIterator rowsIter = im.keySet().iterator();
        int row;
        // We use here this form of iteration instead of 'for' because of nextInt.
        while (rowsIter.hasNext()) {
            row = rowsIter.nextInt();

            for (int colInd = 0; colInd < nB; colInd++) {
                double sum = 0.0;

                IntIterator kIter = im.get(row).iterator();
                int k;

                while (kIter.hasNext()) {
                    k = kIter.nextInt();
                    sum += a.get(row, k) * b.get(k, colInd) * alpha;
                }

                c.setX(row, colInd, c.getX(row, colInd) + sum);
            }
        }
    }

    /**
     * y := alpha * A * x + beta * y.
     *
     * @param alpha Alpha.
     * @param a Matrix a.
     * @param x Vector x.
     * @param beta Beta.
     * @param y Vector y.
     */
    public static void gemv(double alpha, Matrix a, Vector x, double beta, DenseLocalOnHeapVector y) {
        checkCardinality(a, x);
        checkCardinality(a, y);

        if (alpha == 0.0 && beta == 1.0)
            return;

        if (alpha == 0.0) {
            scal(y, beta);
            return;
        }

        if (a instanceof SparseLocalOnHeapMatrix && x instanceof DenseLocalOnHeapVector)
            gemv(alpha, (SparseLocalOnHeapMatrix)a, (DenseLocalOnHeapVector)x, beta, y);
        else if (a instanceof SparseLocalOnHeapMatrix && x instanceof SparseLocalVector)
            gemv(alpha, (SparseLocalOnHeapMatrix)a, (SparseLocalVector)x, beta, y);
        else if (a instanceof DenseLocalOnHeapMatrix && x instanceof DenseLocalOnHeapVector)
            gemv(alpha, (DenseLocalOnHeapMatrix)a, (DenseLocalOnHeapVector)x, beta, y);
        else if (a instanceof DenseLocalOnHeapMatrix && x instanceof SparseLocalVector)
            gemv(alpha, (DenseLocalOnHeapMatrix)a, (SparseLocalVector)x, beta, y);
        else
            throw new IllegalArgumentException("Operation gemv doesn't support running on matrix type " +
                a.getClass().getSimpleName() + " and vector type " + x.getClass().getSimpleName());
    }

    /**
     * y := alpha * A * x + beta * y.
     *
     * @param alpha Alpha.
     * @param a Matrix a.
     * @param x Vector x.
     * @param beta Beta.
     * @param y Vector y.
     */
    private static void gemv(double alpha, SparseLocalOnHeapMatrix a, DenseLocalOnHeapVector x, double beta,
        DenseLocalOnHeapVector y) {

        if (beta != 1.0)
            scal(y, beta);

        IntIterator rowIter = a.indexesMap().keySet().iterator();
        while (rowIter.hasNext()) {
            int row = rowIter.nextInt();

            double sum = 0.0;
            IntIterator colIter = a.indexesMap().get(row).iterator();
            while (colIter.hasNext()) {
                int col = colIter.nextInt();
                sum += alpha * a.getX(row, col) * x.getX(col);
            }

            y.setX(row, y.getX(row) + sum);
        }
    }

    /**
     * y := alpha * A * x + beta * y.
     *
     * @param alpha Alpha.
     * @param a Matrix a.
     * @param x Vector x.
     * @param beta Beta.
     * @param y Vector y.
     */
    private static void gemv(double alpha, DenseLocalOnHeapMatrix a, DenseLocalOnHeapVector x, double beta,
        DenseLocalOnHeapVector y) {
        nativeBlas.dgemv("N", a.rowSize(), a.columnSize(), alpha, a.getStorage().data(), a.rowSize(), x.getStorage().data(), 1, beta,
            y.getStorage().data(), 1);
    }

    /**
     * y := alpha * A * x + beta * y.
     *
     * @param alpha Alpha.
     * @param a Matrix a.
     * @param x Vector x.
     * @param beta Beta.
     * @param y Vector y.
     */
    private static void gemv(double alpha, SparseLocalOnHeapMatrix a, SparseLocalVector x, double beta,
        DenseLocalOnHeapVector y) {


        if (beta != 1.0)
            scal(y, beta);

        IntIterator rowIter = a.indexesMap().keySet().iterator();
        while (rowIter.hasNext()) {
            int row = rowIter.nextInt();

            double sum = 0.0;
            IntIterator colIter = a.indexesMap().get(row).iterator();
            while (colIter.hasNext()) {
                int col = colIter.nextInt();

                sum += alpha * a.getX(row, col) * x.getX(col);
            }

            y.set(row, y.get(row) + sum);
        }
    }

    /**
     * y := alpha * A * x + beta * y.
     *
     * @param alpha Alpha.
     * @param a Matrix a.
     * @param x Vector x.
     * @param beta Beta.
     * @param y Vector y.
     */
    private static void gemv(double alpha, DenseLocalOnHeapMatrix a, SparseLocalVector x, double beta,
        DenseLocalOnHeapVector y) {
        int rowCntrForA = 0;
        int mA = a.rowSize();

        double[] aData = a.getStorage().data();

        IntSet indexes = x.indexes();

        double[] yValues = y.getStorage().data();

        while (rowCntrForA < mA) {
            double sum = 0.0;

            IntIterator iter = indexes.iterator();
            while (iter.hasNext()) {
                int xIdx = iter.nextInt();
                sum += x.getX(xIdx) * aData[xIdx * mA + rowCntrForA];
            }

            yValues[rowCntrForA] = sum * alpha + beta * yValues[rowCntrForA];
            rowCntrForA++;
        }
    }

    /**
     * M := alpha * M.
     * @param m Matrix M.
     * @param alpha Aplha.
     */
    private static void scal(Matrix m, double alpha) {
        if (alpha != 1.0)
            for (int i = 0; i < m.rowSize(); i++)
                for (int j = 0; j < m.columnSize(); j++)
                    m.setX(i, j, m.getX(i, j) * alpha);

    }

    /**
     * v := alpha * v.
     * @param v Vector v.
     * @param alpha Aplha.
     */
    private static void scal(Vector v, double alpha) {
        if (alpha != 1.0)
            for (int i = 0; i < v.size(); i++)
                v.compute(i, (ind, val) -> val * alpha);
    }

    /**
     * Checks if Matrix A can be multiplied by vector v, if not CardinalityException is thrown.
     *
     * @param a Matrix A.
     * @param v Vector v.
     */
    public static void checkCardinality(Matrix a, Vector v) throws CardinalityException {
        if (a.columnSize() != v.size())
            throw new CardinalityException(a.columnSize(), v.size());
    }
}
