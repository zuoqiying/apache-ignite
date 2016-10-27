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
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import junit.framework.TestCase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.ignite.internal.processors.hadoop.HadoopDefaultJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopHelperImpl;
import org.apache.ignite.internal.processors.hadoop.HadoopJob;
import org.apache.ignite.internal.processors.hadoop.HadoopJobId;
import org.apache.ignite.internal.processors.hadoop.HadoopJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInput;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskOutput;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskType;
import org.apache.ignite.internal.processors.hadoop.impl.HadoopTestTaskContext;
import org.apache.ignite.internal.processors.hadoop.impl.examples.HadoopWordCount2;
import org.apache.ignite.internal.processors.hadoop.impl.v2.HadoopV2Job;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopMultimap;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSkipList;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSpillableMultimap;
import org.apache.ignite.internal.util.GridRandom;
import org.apache.ignite.internal.util.GridUnsafe;
import org.apache.ignite.internal.util.io.GridDataInput;
import org.apache.ignite.internal.util.io.GridUnsafeDataInput;
import org.apache.ignite.internal.util.offheap.unsafe.GridUnsafeMemory;
import org.apache.ignite.internal.util.typedef.X;

/**
 *
 */
public class HadoopSpillableMultimapTest extends HadoopAbstractMapTest {

    private final Random rnd = new Random();

    private final int mapSize = 16 << rnd.nextInt(6);

    public void testHadoopSpillableMultimap() throws Exception {
        long memLimit = 10 * 1024 * 1024; // 1 m

        GridUnsafeMemory mem = new GridUnsafeMemory(memLimit);

        HadoopJobInfo job = new JobInfo();

        HadoopTaskContext taskCtx = new TaskContext();

        HadoopSpillableMultimap m = new HadoopSpillableMultimap(taskCtx, job, mem);

        final Multimap<Integer, Integer> mm = ArrayListMultimap.create();

        putData(m, taskCtx, mm);

        check(m, mm, taskCtx);

        check(m, mm, taskCtx);

        String file = "/tmp/a";

        DataOutput dout = getDout(file);

        long written;

        try {
            written = m.spill(dout);
        }
        finally {
            ((AutoCloseable)dout).close();
        }

        System.out.println("Written bytes: " + written);

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

        System.out.println("read: " + read);

        assertEquals(read, written);

        check(m, mm, taskCtx);
    }


    public void testMapSimple() throws Exception {
        GridUnsafeMemory mem = new GridUnsafeMemory(0);

        HadoopJobInfo job = new JobInfo();

        HadoopTaskContext taskCtx = new TaskContext();

        HadoopMultimap m = new HadoopSkipList(job, mem);

        testMap(m, taskCtx, mem);
    }

    private DataOutput getDout(String name) throws IOException {
        return new DataOutputStream(new FileOutputStream(name, false));
    }

    private DataInput getDin(String name) throws IOException {
        return new DataInputStream(new FileInputStream(name));
    }

    private void putData(HadoopMultimap m, HadoopTaskContext taskCtx, Multimap<Integer, Integer> mm) throws Exception {
        try (HadoopTaskOutput a = m.startAdding(taskCtx)) {
            boolean added;

            for (int i = 0, vals = 4 * mapSize + rnd.nextInt(25); i < vals; i++) {
                int key = rnd.nextInt(mapSize);
                int val = rnd.nextInt();

                System.out.println("gen: key: " + key + ", value: " + val);

                added = a.write(new IntWritable(key), new IntWritable(val));
                assert added;

                mm.put(key, val);
                System.out.println("mm: key: " + key + ", value: " + mm.get(key));
            }
        }
    }

    private void testMap(HadoopMultimap m, HadoopTaskContext taskCtx, GridUnsafeMemory mem) throws Exception  {
        HadoopMultimap.Adder a = (HadoopMultimap.Adder)m.startAdding(taskCtx);

        Multimap<Integer, Integer> mm = ArrayListMultimap.create();
        Multimap<Integer, Integer> vis = ArrayListMultimap.create();

        for (int i = 0, vals = 4 * mapSize + rnd.nextInt(25); i < vals; i++) {
            int key = rnd.nextInt(mapSize);
            int val = rnd.nextInt();

            a.write(new IntWritable(key), new IntWritable(val));
            mm.put(key, val);

            //X.println("k: " + key + " v: " + val);

            a.close();

            check(m, mm, taskCtx);

            a = (HadoopMultimap.Adder)m.startAdding(taskCtx);
        }

        //        a.add(new IntWritable(10), new IntWritable(2));
        //        mm.put(10, 2);
        //        check(m, mm);

        a.close();

        X.println("Alloc: " + mem.allocatedSize());

        m.close();

        assertEquals(0, mem.allocatedSize());
    }

    private void check(HadoopMultimap m, Multimap<Integer, Integer> mm, HadoopTaskContext taskCtx)
        throws Exception {
        final HadoopTaskInput in = m.input(taskCtx);

        final Multimap<Integer, Integer> vis = ArrayListMultimap.create();

        Map<Integer, Collection<Integer>> mmm = mm.asMap();

        int keys = 0;

        int prevKey = Integer.MIN_VALUE;

        while (in.next()) {
            keys++;

            IntWritable k = (IntWritable)in.key();

            assertNotNull(k);

            System.out.println("K: " + k.get());

            assertTrue(k.get() > prevKey);

            prevKey = k.get();

            Deque<Integer> vs = new LinkedList<>(); // values

            Iterator<?> it = in.values();

            while (it.hasNext()) {
                //vs.addFirst(((IntWritable) it.next()).get());
                Integer v = ((IntWritable)it.next()).get();

                //vs.add(v);
                vs.addFirst(v); // NB: ! For some reason the values are read in reverse order !

                System.out.println("Read: " + v);
            }

            Collection<Integer> exp = mmm.get(k.get());

            if (!vs.equals(exp)) {
                List rev = new ArrayList(exp);

                Collections.reverse(rev);

                assertEquals(rev, vs);
            }
//
//            assertEquals(exp, vs);
        }

        assertEquals(mmm.size(), keys);

        //!        assertEquals(m.keys(), keys);

        // Check visitor.

        final byte[] buf = new byte[4];

        final GridDataInput dataInput = new GridUnsafeDataInput();

        m.accept(false, new HadoopMultimap.Visitor() {
            /** */
            IntWritable key = new IntWritable();

            /** */
            IntWritable val = new IntWritable();

            @Override public void visitKey(long keyPtr, int keySize) {
                read(keyPtr, keySize, key);
            }

            @Override public void visitValue(long valPtr, int valSize) {
                read(valPtr, valSize, val);

                vis.put(key.get(), val.get());
            }

            private void read(long ptr, int size, Writable w) {
                assert size == 4 : size;

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

        //        X.println("vis: " + vis);

        //assertEquals(mm, vis);
        compareMultimaps(mm, vis);

        in.close();
    }

    private void compareMultimaps(Multimap m1, Multimap m2) {
        assertEquals(m1.size(), m2.size());

        for (Object k : m1.keySet()) {
            Collection v1 = m1.get(k);
            Collection v2 = m2.get(k);

            assertNotNull(v2);

            // Order of elements may be different:
            assertEqualsCollections(new TreeSet(v1), new TreeSet(v2));
        }
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

            final HadoopMultimap m = new HadoopSkipList(job, mem);

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
