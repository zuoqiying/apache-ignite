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

package org.apache.ignite.internal.util.intset;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.util.io.GridByteArrayInputStream;
import org.apache.ignite.internal.util.io.GridByteArrayOutputStream;
import org.apache.ignite.internal.util.typedef.internal.U;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.ignite.marshaller.optimized.OptimizedMarshaller;

/**
 * Test for {@link GridIntSet}.
 */
public class GridIntSetContainerSelfTest extends GridIntSetAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected TestIntSet set() {
        TestIntSet set = new TestIntSetImpl();

        return rndFill(set, MAX_VALUES / 10, MAX_VALUES);
    }

    /** */
    public void testSet() {
        GridIntSet set = new GridIntSet();

        assertEquals(-1, set.first());

        assertEquals(-1, set.last());

        assertFalse(set.iterator().hasNext());

        assertFalse(set.reverseIterator().hasNext());

        int size = GridIntSet.SEGMENT_SIZE;

        for (int i = 0; i < size; i++)
            assertTrue(set.add(i * size));

        assertEquals("Size", size, set.size());

        List<Integer> vals = toList(set.iterator());

        List<Integer> vals2 = toList(set.reverseIterator());

        Collections.reverse(vals2);

        assertEqualsCollections(vals, vals2);

        assertEquals("First", 0, set.first());

        assertEquals("Last", (size - 1) * size, set.last());

        for (int i = 0; i < size; i++)
            assertTrue(set.contains(i * size));

        for (int i = 0; i < size; i++)
            assertTrue(set.remove(i * size));

        assertEquals("Size", 0, set.size());
    }

    /** */
    public void testSerializationSparseSet() throws IOException, ClassNotFoundException {
        GridIntSet set = new GridIntSet();

        int size = GridIntSet.SEGMENT_SIZE;

        for (int i = 0; i < size; i++)
            assertTrue(set.add(i * size));

        assertEquals("Size", size, set.size());

        byte[] bytes = serialize(set);

        System.out.println(bytes.length);

        assertEquals(set, deserialize(bytes));
    }

    /** */
    public void testSerializationDenseSet() throws IOException, ClassNotFoundException {
        GridIntSet set = new GridIntSet();

        int size = GridIntSet.SEGMENT_SIZE;

        for (int i = 0; i < size; i++)
            assertTrue(set.add(i));

        assertEquals("Size", size, set.size());

        byte[] bytes = serialize(set);

        System.out.println(bytes.length);

        assertEquals(set, deserialize(bytes));
    }

    /** */
    public void testSerializationSequential() throws IOException, ClassNotFoundException {
        GridIntSet set = new GridIntSet();
        GridIntSet set2 = new GridIntSet();

        for (int i = 0; i < 8192; i++) {
            set.add(i);

            set2.add(i);

            byte[] bytes = serialize(set);

            set = deserialize(bytes);

            assertEquals(set2, set);
        }
    }

    /** */
    private byte[] serialize(GridIntSet set) throws IOException {
        GridByteArrayOutputStream bos = new GridByteArrayOutputStream();

        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(set);

        oos.close();

        return bos.toByteArray();
    }

    /** */
    private GridIntSet deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new GridByteArrayInputStream(data));

        return  (GridIntSet) ois.readObject();
    }

    /** */
    private List<Integer> toList(GridIntSet.Iterator it) {
        List<Integer> l = new ArrayList<>();

        while(it.hasNext())
            l.add(it.next());

        return l;
    }
}