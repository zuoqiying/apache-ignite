package org.apache.ignite.internal.processors.hadoop.impl.shuffle.collections;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.ignite.internal.processors.hadoop.HadoopJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInput;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskOutput;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopMultimap;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSpillableMultimap;
import org.apache.ignite.internal.util.GridRandom;
import org.apache.ignite.internal.util.GridUnsafe;
import org.apache.ignite.internal.util.io.GridDataInput;
import org.apache.ignite.internal.util.io.GridUnsafeDataInput;
import org.apache.ignite.internal.util.offheap.unsafe.GridUnsafeMemory;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 *
 */
public class HadoopSpillableMultimapTest extends HadoopAbstractMapTest {
    /** */
    private final Random rnd = new Random(0L); // Seed set for reproducibility.

    /** */
    private final int mapSize = 16 << rnd.nextInt(6);

    /** */
    private final String dataDir = U.getIgniteHome() + File.separatorChar + "work"
        + File.separatorChar + getClass().getName() + "-spill";

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        File dir = new File(dataDir);

        U.delete(new File(dataDir));

        assertFalse(dir.exists());

        dir.mkdirs();

        assertTrue(dir.exists());
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        File dir = new File(dataDir);

        U.delete(new File(dataDir));

        assertFalse(dir.exists());
    }

    /** */
    public void testHadoopSpillableMultimap() throws Exception {
        GridUnsafeMemory mem = new GridUnsafeMemory(0);

        HadoopJobInfo job = new JobInfo();

        HadoopTaskContext taskCtx = new TaskContext();

        HadoopSpillableMultimap m = new HadoopSpillableMultimap(taskCtx, job, mem);

        final Multimap<Integer, Integer> mm = ArrayListMultimap.create();

        putData(m, taskCtx, mm);

        check(m, mm, taskCtx, true);
        // Do it second time to ensure the check does not change state:
        check(m, mm, taskCtx, true);

        String file = dataDir + "/spill00";

        DataOutput dout = getDout(file);

        long written;

        try {
            written = m.spill(dout);
        }
        finally {
            ((AutoCloseable)dout).close();
        }

        //System.out.println("Written bytes: " + written);

        assertTrue(written > 0);

        long len = new File(file).length();

        assertEquals(len, written);

        DataInput din = getDin(file);

        // assert m is empty
        long read;
        try {
            read = m.unSpill(din);
        }
        finally {
            ((AutoCloseable)din).close();
        }

        //System.out.println("read: " + read);

        assertEquals(read, written);

        check(m, mm, taskCtx, true);
    }

    /**
     */
    public void testMapSimple() throws Exception {
        GridUnsafeMemory mem = new GridUnsafeMemory(0);

        HadoopJobInfo job = new JobInfo();

        HadoopTaskContext taskCtx = new TaskContext();

        HadoopMultimap m = new HadoopSpillableMultimap(taskCtx, job, mem);

        testMap(m, taskCtx, mem);
    }

    /**
     */
    private DataOutput getDout(String name) throws IOException {
        return new DataOutputStream(new FileOutputStream(name, false));
    }

    /**
     */
    private DataInput getDin(String name) throws IOException {
        return new DataInputStream(new FileInputStream(name));
    }

    /**
     */
    private void putData(HadoopMultimap m, HadoopTaskContext taskCtx, Multimap<Integer, Integer> mm) throws Exception {
        try (HadoopTaskOutput a = m.startAdding(taskCtx)) {
            boolean added;

            for (int i = 0, vals = 4 * mapSize + rnd.nextInt(25); i < vals; i++) {
                int key = rnd.nextInt(mapSize);
                int val = rnd.nextInt();

                //System.out.println("gen: key: " + key + ", value: " + val);

                added = a.write(new IntWritable(key), new IntWritable(val));
                assert added;

                mm.put(key, val);
                //System.out.println("mm: key: " + key + ", value: " + mm.get(key));
            }
        }
    }

    /**
     */
    private void testMap(HadoopMultimap m, HadoopTaskContext taskCtx, GridUnsafeMemory mem) throws Exception {
        HadoopTaskOutput a = m.startAdding(taskCtx);

        Multimap<Integer, Integer> mm = ArrayListMultimap.create();

        for (int i = 0, vals = 4 * mapSize + rnd.nextInt(25); i < vals; i++) {
            int key = rnd.nextInt(mapSize);
            int val = rnd.nextInt();

            boolean written = a.write(new IntWritable(key), new IntWritable(val));
            assertTrue(written);

            mm.put(key, val);

            X.println("written & put:  K=" + key + "  V=" + val);

            a.close();

            check(m, mm, taskCtx, true);

            a = m.startAdding(taskCtx);
        }

        a.close();

        X.println("Alloc: " + mem.allocatedSize());

        m.close();

        assertEquals(0, mem.allocatedSize());
    }

    /**
     */
    private void check(HadoopMultimap m, Multimap<Integer, Integer> mm, HadoopTaskContext taskCtx,
            boolean valuesAnyOrder)
        throws Exception {
        // 1. Check via the iterating input:
        final HadoopTaskInput in = m.input(taskCtx);

        Map<Integer, Collection<Integer>> mmm = mm.asMap();

        int keys = 0;

        int prevKey = Integer.MIN_VALUE;

        while (in.next()) {
            keys++;

            IntWritable k = (IntWritable)in.key();

            assertNotNull(k);

            //System.out.println("K: " + k.get());

            assertTrue(k.get() > prevKey);

            prevKey = k.get();

            Deque<Integer> vs = new LinkedList<>(); // values

            Iterator<?> it = in.values();

            while (it.hasNext()) {
                //vs.addFirst(((IntWritable) it.next()).get());
                Integer v = ((IntWritable)it.next()).get();

                //vs.add(v);
                vs.addFirst(v); // NB: ! For some reason the values are read in reverse order !

                //System.out.println("Read: " + v);
            }

            Collection<Integer> exp = mmm.get(k.get());

            // NB: values are read with Input in reversed order. When spill/unspill data,
            // we get them in direct or reversed order. So, check both orders:
            if (valuesAnyOrder)
                assertEqualsAnyOrder(vs, exp);
            else
                assertEquals(vs, exp);
        }

        assertEquals(mmm.size(), keys);

        // 2. Check visitor.
        final Multimap<Integer, Integer> vis = ArrayListMultimap.create();

        final byte[] buf = new byte[4];

        final GridDataInput dataInput = new GridUnsafeDataInput();

        boolean accepted = m.accept(true/*false*/, new HadoopMultimap.Visitor() {
            /** */
            final IntWritable key = new IntWritable();

            /** */
            final IntWritable val = new IntWritable();

            @Override public void visitKey(long keyPtr, int keySize) {
                read(keyPtr, keySize, key);
            }

            @Override public void visitValue(long valPtr, int valSize) {
                read(valPtr, valSize, val);

                vis.put(key.get(), val.get());
            }

            private void read(long ptr, int size, Writable w) {
                assert buf.length >= size; // Otherwise we likely have segmentation fault.

                GridUnsafe.copyMemory(null, ptr, buf, GridUnsafe.BYTE_ARR_OFF, size);

                dataInput.bytes(buf, size);

                try {
                    w.readFields(dataInput);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        assertTrue(accepted);

        compareMultimaps(mm, vis, valuesAnyOrder);

        in.close();
    }

    private static void compareMultimaps(Multimap m1, Multimap m2, boolean valuesAnyOrder) {
        assertEquals(m1.size(), m2.size());

        for (Object k : m1.keySet()) {
            Collection v1 = m1.get(k);
            Collection v2 = m2.get(k);

            assertNotNull(v1);
            assertNotNull(v2);

            if (valuesAnyOrder)
                assertEqualsAnyOrder(v1, v2);
            else
                assertEquals(v1, v2);
        }
    }

    public static void assertEqualsAnyOrder(Collection c1, Collection c2) {
        List list1 = new ArrayList<>(c1);
        List list2 = new ArrayList<>(c2);

        if (list1.equals(list2))
            return;

        Collections.reverse(list1);

        assertEquals(list1, list2);
    }

    /**
     * @throws Exception if failed.
     */
    public void testMultiThreaded() throws Exception {
        GridUnsafeMemory mem = new GridUnsafeMemory(0);

        X.println("___ Started");

        Random rnd = new GridRandom();

        for (int i = 0; i < 20; i++) {
            HadoopJobInfo job = new JobInfo();

            final HadoopTaskContext taskCtx = new TaskContext();

            final HadoopMultimap m = new HadoopSpillableMultimap(taskCtx, job, mem);

            final ConcurrentMap<Integer, Collection<Integer>> mm = new ConcurrentHashMap<>();

            X.println("___ MT");

            multithreaded(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    X.println("___ TH in");

                    Random rnd = new GridRandom();

                    IntWritable key = new IntWritable();
                    IntWritable val = new IntWritable();

                    HadoopMultimap.Adder a = (HadoopMultimap.Adder)m.startAdding(taskCtx);

                    for (int i = 0; i < 50000; i++) {
                        int k = rnd.nextInt(32000);
                        int v = rnd.nextInt();

                        key.set(k);
                        val.set(v);

                        a.write(key, val);

                        Collection<Integer> list = mm.get(k);

                        if (list == null) {
                            list = new ConcurrentLinkedQueue<>();

                            Collection<Integer> old = mm.putIfAbsent(k, list);

                            if (old != null)
                                list = old;
                        }

                        list.add(v);
                    }

                    a.close();

                    X.println("___ TH out");

                    return null;
                }
            }, 3 + rnd.nextInt(27));

            HadoopTaskInput in = m.input(taskCtx);

            int prevKey = Integer.MIN_VALUE;

            while (in.next()) {
                IntWritable key = (IntWritable)in.key();

                assertTrue(key.get() > prevKey);

                prevKey = key.get();

                Iterator<?> valsIter = in.values();

                Collection<Integer> vals = mm.remove(key.get());

                assertNotNull(vals);

                while (valsIter.hasNext()) {
                    IntWritable val = (IntWritable) valsIter.next();

                    assertTrue(vals.remove(val.get()));
                }

                assertTrue(vals.isEmpty());
            }

            in.close();
            m.close();

            assertEquals(0, mem.allocatedSize());
        }
    }
}
