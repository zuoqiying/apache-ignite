package org.apache.ignite.internal.processors.hadoop.impl.shuffle.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.hadoop.HadoopJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopSerialization;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInput;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskOutput;
import org.apache.ignite.internal.processors.hadoop.impl.v2.HadoopWritableSerialization;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopMergingTaskInput;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSkipList;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSpillableMultimap;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSpillingTaskOutput;
import org.apache.ignite.internal.util.offheap.unsafe.GridUnsafeMemory;
import org.apache.ignite.internal.util.typedef.T2;

/**
 * Tests Hadoop
 */
public class HadoopSpillOutputInputTest extends HadoopAbstractMapTest {
    // TODO: 1) HadoopMultimap#write() return value should be consistent with its size, not a "mem" size.

    /** */
    private final int totalKeys = 3;

    /** */
    private final int valuesPerKey = 2;

    /**
     * Limit for one multimap (now in k-v s, not in bytes).
     */
    private final long limit = 4;

    /** */
    private static final RawComparator<Text> rawCmp = new Text.Comparator();

    /** */
    private static final RawComparator<IntWritable> rawIntCmp = new IntWritable.Comparator() {
        @Override public int compare(WritableComparable a, WritableComparable b) {
            return super.compare(a, b);
        }

        @Override public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return super.compare(b1, s1, l1, b2, s2, l2);
        }
    };

    /** */
    private static final RawComparator<IntWritable> rawGrpCmp = new IntWritable.Comparator() {
        @Override public int compare(WritableComparable a, WritableComparable b) {
            int x = ((IntWritable)a).get();
            int y = ((IntWritable)b).get();

            return cmp(x, y);
        }

        private int cmp(int x, int y) {
            if (x / 3 == y / 3)
                return 0;

            return (x < y ? -1 : (x == y ? 0 : 1));
        }

        /** {@inheritDoc} */
        @Override public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            int thisValue = readInt(b1, s1);
            int thatValue = readInt(b2, s2);

            return cmp(thisValue, thatValue);
        }
    };


    public void testOutput() throws Exception {
        GridUnsafeMemory mem1 = new GridUnsafeMemory(0);
        GridUnsafeMemory mem2 = new GridUnsafeMemory(0);

        HadoopJobInfo job = new JobInfo();

        HadoopTaskContext taskCtx = new TaskContext() {
            @Override public HadoopSerialization keySerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(Text.class);
            }

            @Override public HadoopSerialization valueSerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(Text.class);
            }

//            @Override public Comparator<Object> groupComparator() {
//                return (Comparator)rawCmp;
//            }
        };

        // 1. Output
        final HadoopSpillingTaskOutput out = new HadoopSpillingTaskOutput(
            new HadoopSpillableMultimap[] {
                new HadoopSpillableMultimap(taskCtx, job, mem1, limit),
                new HadoopSpillableMultimap(taskCtx, job, mem2, limit)
        }, /*Base names*/ new String[] { "alpha", "beta" }, taskCtx, log(), job);

        //HadoopSkipList list = new HadoopSkipList(job, mem);
        //HadoopTaskOutput out = list.startAdding(taskCtx);

        final Text keyText = new Text();
        final Text valText = new Text();

        int totalCnt = 0;

        for (int v = 0; v<valuesPerKey; v++) {
            for (int k = 0; k< totalKeys; k++) {
                keyText.set(keyStr(k));
                valText.set(value(k, v));

                assertTrue(out.write(keyText, valText));

                totalCnt++;

                //if (totalCnt % 10 == 0)
                    System.out.println("written cnt: " + totalCnt + " k=" + keyText + " v=" + valText);
            }
        }

        // 2. Input.
        HadoopTaskInput rawInput = out.getInput(taskCtx, rawCmp);

        HadoopTaskInput in = new HadoopSkipList.InputForRawInput(rawInput, taskCtx);

        HadoopTaskInput objectInput = new HadoopSkipList.GroupedObjectInput(in, new Text.Comparator());

        //rawInput = new HadoopSkipList.GroupedRawInput(rawInput, rawCmp);

        //T2<Long, Long> counts = HadoopMergingTaskInput.checkRawInput(objInput, taskCtx, rawCmp);
        //assertEquals(totalKeys, (long)counts.get1());
        //assertEquals(totalKeys * valuesPerKey, (long)counts.get2());

        int keyCnt = 0;
        int valCnt = 0;

        final Text prevKey = new Text();

        while (objectInput.next()) {
            Text key = (Text)objectInput.key();

            keyCnt++;

            System.out.println("k = " + key);

            if (prevKey.getLength() > 0) {
                int cmp = rawCmp.compare(prevKey, key);

                assertTrue(cmp <= 0);

                int grpCmp = rawCmp.compare(prevKey, key);

                assertTrue(grpCmp != 0);
            }

            prevKey.set(key.getBytes()); // Save value.

            Iterator<IntWritable> it = (Iterator)objectInput.values();

            IntWritable val;

            while (it.hasNext()) {
                val = it.next();

                valCnt++;

                System.out.println("     v = " + val);
            }
        }

        assertEquals(2, keyCnt);
        assertEquals(2 * valCnt, valCnt);
    }

    private int key(int keyIdx) {
        return keyIdx;
    }

    private String keyStr(int keyIdx) {
        return "key-" + keyIdx;
    }

    private String value(int keyIdx, int valIdx) {
        return "value-" + keyIdx + "-" + valIdx;
    }

    private Iterator<Integer> createIterator() {
        Iterator<Integer> it00 = Collections.<Integer>emptyList().iterator();
        Iterator<Integer> it0 = new ArrayList<>(Arrays.asList(new Integer[] { 0, 1, 2, 3 })).iterator();
        Iterator<Integer> it01 = Collections.<Integer>emptyList().iterator();
        Iterator<Integer> it1 = new ArrayList<>(Arrays.asList(new Integer[] { 4, 5, 6, 7 })).iterator();
        Iterator<Integer> it2 = new ArrayList<>(Arrays.asList(new Integer[] { 8, 9, 10, 11 })).iterator();
        Iterator<Integer> it20 = Collections.<Integer>emptyList().iterator();

        Iterator<Iterator<Integer>> itIt = (Iterator)Arrays.asList(
            new Iterator[] { it00, it0, it01, it1, it2, it20 }).iterator();

        return new HadoopSkipList.CompositeIterator<>(itIt);
    }

    /**
     *
     */
    public void testCompositeIterator() {
        Iterator<Integer> composite = createIterator();

        int i = 0;

        while (composite.hasNext()) {
            int x = composite.next();

            assertEquals(i, x);

            i++;
        }

        assertEquals(12, i);
   }

    /**
     *
     */
    public void testCompositeIteratorExtraHasNext() {
        Iterator<Integer> composite = createIterator();

        int i = 0;

        while (true) {
            boolean b1 = composite.hasNext();
            boolean b2 = composite.hasNext();
            boolean b3 = composite.hasNext();

            assertEquals(b1, b2);
            assertEquals(b2, b3);

            if (!b1)
                break;

            int x = composite.next();

            assertEquals(i, x);

            i++;
        }

        assertEquals(12, i);
    }

    public void testGroupedInput() throws Exception {
        GridUnsafeMemory mem = new GridUnsafeMemory(0);

        HadoopJobInfo info = new JobInfo();

        HadoopSkipList list = new HadoopSkipList(info, mem);

        HadoopTaskContext taskCtx = new TaskContext() {
            @Override public HadoopSerialization keySerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(IntWritable.class);
            }

            @Override public HadoopSerialization valueSerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(IntWritable.class);
            }

            @Override public Comparator<Object> sortComparator() {
                return (Comparator)rawIntCmp;
            }

            @Override public Comparator<Object> groupComparator() {
                return (Comparator)rawGrpCmp;
            }
        };

        try (HadoopTaskOutput out = list.startAdding(taskCtx)) {
            final IntWritable keyWrtbl = new IntWritable();
            final IntWritable valWrtbl = new IntWritable();

            int totalCnt = 0;

            final int keys = 5;
            final int vals = 3;

            for (int v = 0; v < vals; v++) {
                for (int k = 0; k < keys; k++) {
                    keyWrtbl.set(key(k));
                    valWrtbl.set(totalCnt);

                    assertTrue(out.write(keyWrtbl, valWrtbl));

                    totalCnt++;

                    //if (totalCnt % 10 == 0)
                    System.out.println("written: " + totalCnt + ", k= " + keyWrtbl + ", v = " + valWrtbl);
                }
            }

            //HadoopTaskInput in = list.rawInput();
            //in = new HadoopSkipList.GroupedRawInput(in, rawGrpCmp);
            HadoopTaskInput objectInput = list.input(taskCtx); //new HadoopSkipList.InputForRawInput(in, taskCtx);

            int keyCnt = 0;
            int valCnt = 0;

            final IntWritable prevKey = new IntWritable(-1);

            while (objectInput.next()) {
                IntWritable key = (IntWritable)objectInput.key();

                keyCnt++;

                System.out.println("k = " + key);

                if (prevKey.get() >= 0) {
                    int cmp = rawIntCmp.compare(prevKey, key);

                    assertTrue(cmp <= 0);

                    int grpCmp = rawGrpCmp.compare(prevKey, key);

                    assertTrue(grpCmp != 0);
                }

                prevKey.set(key.get()); // Save value.

                Iterator<IntWritable> it = (Iterator)objectInput.values();

                IntWritable val;

                while (it.hasNext()) {
                    val = it.next();

                    valCnt++;

                    System.out.println("     v = " + val);
                }
            }

            assertEquals(2, keyCnt);
            assertEquals(keys * vals, valCnt);
        }
    }

    // TODO: fix this test if we decide to enable raw processing.
    // TODO: currently disabled dues to problems with big-little endian incompatibility.
    public void testGroupedRawInput() throws Exception {
        GridUnsafeMemory mem = new GridUnsafeMemory(0);

        HadoopJobInfo info = new JobInfo();

        HadoopSkipList list = new HadoopSkipList(info, mem);

        HadoopTaskContext taskCtx = new TaskContext() {
            @Override public HadoopSerialization keySerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(IntWritable.class);
            }

            @Override public HadoopSerialization valueSerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(IntWritable.class);
            }

            @Override public Comparator<Object> sortComparator() {
                return (Comparator)rawIntCmp;
            }

            @Override public Comparator<Object> groupComparator() {
                return (Comparator)rawGrpCmp;
            }
        };

        try (HadoopTaskOutput out = list.startAdding(taskCtx)) {
            final IntWritable keyWrtbl = new IntWritable();
            final IntWritable valWrtbl = new IntWritable();

            int totalCnt = 0;

            final int keys = 5;
            final int vals = 3;

            for (int v = 0; v < vals; v++) {
                for (int k = 0; k < keys; k++) {
                    keyWrtbl.set(key(k));
                    valWrtbl.set(totalCnt);

                    assertTrue(out.write(keyWrtbl, valWrtbl));

                    totalCnt++;

                    //if (totalCnt % 10 == 0)
                        System.out.println("written: " + totalCnt + ", k= " + keyWrtbl + ", v = " + valWrtbl);
                }
            }

            HadoopTaskInput in = list.rawInput();
            in = new HadoopSkipList.InputForRawInput(in, taskCtx);
            HadoopTaskInput objInput = new HadoopSkipList.GroupedObjectInput(in, rawGrpCmp);

            int keyCnt = 0;
            int valCnt = 0;

            final IntWritable prevKey = new IntWritable(-1);

            while (objInput.next()) {
                IntWritable key = (IntWritable)objInput.key();

                keyCnt++;

                System.out.println("k = " + key);

                if (prevKey.get() >= 0) {
                    int cmp = rawIntCmp.compare(prevKey, key);

                    assertTrue(cmp <= 0);

                    int grpCmp = rawGrpCmp.compare(prevKey, key);

                    assertTrue(grpCmp != 0);
                }

                prevKey.set(key.get()); // Save value.

                Iterator<IntWritable> it = (Iterator)objInput.values();

                IntWritable val;

                while (it.hasNext()) {
                    val = it.next();

                    valCnt++;

                    System.out.println("     v = " + val);
                }
            }

            assertEquals(2, keyCnt);
            assertEquals(keys * vals, valCnt);
        }
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 1000 * super.getTestTimeout();
    }
}