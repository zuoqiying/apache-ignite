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

package org.apache.ignite.internal.visor.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Data transfer object for information about keys in cache partition.
 */
public class VisorCachePartition extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private int part;

    /** */
    private int heap;

    /** */
    private long offheap;

    /**
     * Default constructor.
     */
    public VisorCachePartition() {
        // No-op.
    }

    /**
     * Full constructor.
     *
     * @param part Partition id.
     * @param heap Number of keys in heap.
     * @param offheap Number of keys in offheap.
     */
    public VisorCachePartition(int part, int heap, long offheap) {
        this.part = part;
        this.heap = heap;
        this.offheap = offheap;
    }

    /**
     * @return Partition id.
     */
    public int getPartition() {
        return part;
    }

    /**
     * @return Number of keys in heap.
     */
    public int getHeap() {
        return heap;
    }

    /**
     * @return Number of keys in offheap.
     */
    public long getOffheap() {
        return offheap;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorCachePartition.class, this);
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeInt(part);
        out.writeInt(heap);
        out.writeLong(offheap);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        part = in.readInt();
        heap = in.readInt();
        offheap = in.readLong();
    }
}
