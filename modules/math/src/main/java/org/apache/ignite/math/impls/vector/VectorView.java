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

import org.apache.ignite.math.Matrix;
import org.apache.ignite.math.VectorStorage;
import org.apache.ignite.math.exceptions.UnsupportedOperationException;
import org.apache.ignite.math.impls.storage.vector.DelegateVectorStorage;
import org.apache.ignite.math.Vector;

import java.io.*;

/**
 * TODO: add description.
 */
public class VectorView extends AbstractVector {
    /**
     * Constructor for {@link Externalizable} interface.
     */
    public VectorView(){
        // No-op.
    }

    /**
     *
     * @param parent
     * @param off
     * @param len
     */
    public VectorView(Vector parent, int off, int len) {
        super(new DelegateVectorStorage(parent.getStorage(), off, len));
    }

    /**
     *
     * @param sto
     * @param off
     * @param len
     */
    public VectorView(VectorStorage sto, int off, int len) {
        super(new DelegateVectorStorage(sto, off, len));
    }

    private DelegateVectorStorage storage() {
        return (DelegateVectorStorage)getStorage();
    }

    /** {@inheritDoc} */
    @Override public Vector copy() {
        DelegateVectorStorage sto = storage();

        return new VectorView(sto.delegate(), sto.offset(), sto.length());
    }

    /** {@inheritDoc} */
    @Override public Vector like(int crd) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public Matrix likeMatrix(int rows, int cols) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        return this == o ||
            ((o!=null)
                && o.getClass() == getClass()
                && (getStorage().equals(((VectorView)o).getStorage())));
    }
}