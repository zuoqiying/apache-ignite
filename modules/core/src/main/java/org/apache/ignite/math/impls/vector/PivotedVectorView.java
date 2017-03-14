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

package org.apache.ignite.math.impls.vector;

import org.apache.ignite.math.*;
import org.apache.ignite.math.exceptions.UnsupportedOperationException;
import org.apache.ignite.math.impls.storage.vector.PivotedVectorStorage;

import java.io.*;

/**
 * Pivoted (index mapped) view over another vector.
 */
public class PivotedVectorView extends AbstractVector {
    private Vector vec;

    /**
     *
     * @param vec
     * @param pivot Mapping from external index to internal.
     * @param unpivot Mapping from internal index to external.
     */
    public PivotedVectorView(Vector vec, int[] pivot, int[] unpivot) {
        setStorage(new PivotedVectorStorage(vec.getStorage(), pivot, unpivot));

        checkCardinality(pivot);
        checkCardinality(unpivot);

        this.vec = vec;
    }

    /**
     *
     * @param vec
     * @param pivot
     */
    public PivotedVectorView(Vector vec, int[] pivot) {
        setStorage(new PivotedVectorStorage(vec.getStorage(), pivot));

        checkCardinality(pivot);

        this.vec = vec;
    }

    /**
     *
     * @return
     */
    private PivotedVectorStorage storage() {
        return (PivotedVectorStorage)getStorage();
    }

    /**
     *
     */
    public PivotedVectorView() {
        // No-op.
    }

    /**
     *
     * @return
     */
    public Vector getBaseVector() {
        return vec;
    }

    /**
     *
     * @param i
     * @return
     */
    public int pivot(int i) {
        return storage().pivot()[i];
    }

    /**
     *
     * @param i
     * @return
     */
    public int unpivot(int i) {
        return storage().unpivot()[i];
    }

    /**
     *
     * @param idx
     * @return
     */
    protected Element makeElement(int idx) {
        checkIndex(idx);

        // External index.
        int exIdx = storage().pivot()[idx];

        return new Element() {
            /** {@inheritDoc */
            @Override public double get() {
                return storageGet(idx);
            }

            /** {@inheritDoc */
            @Override public int index() {
                return exIdx;
            }

            /** {@inheritDoc */
            @Override public void set(double val) {
                storageSet(idx, val);
            }
        };
    }

    /** {@inheritDoc} */
    @Override public Vector copy() {
        PivotedVectorStorage sto = storage();

        return new PivotedVectorView(vec, sto.pivot(), sto.unpivot());
    }

    /** {@inheritDoc} */
    @Override public Vector like(int crd) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public Matrix likeMatrix(int rows, int cols) {
        return null; // TODO
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeObject(vec);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        vec = (Vector)in.readObject();
    }
}