package org.apache.ignite.internal.processors.hadoop.impl.shuffle.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
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
                keyText.set(key(k));
                valText.set(value(k, v));

                assertTrue(out.write(keyText, valText));

                totalCnt++;

                //if (totalCnt % 10 == 0)
                    System.out.println("written cnt: " + totalCnt + " k=" + keyText + " v=" + valText);
            }
        }

        // 2. Input.
        final HadoopTaskInput rawInput = out.getInput(taskCtx, rawCmp);

        // TODO: enable grouped input
        //HadoopSkipList.GroupedRawInput gin = new HadoopSkipList.GroupedRawInput(rawInput, rawCmp);

        T2<Long, Long> counts = HadoopMergingTaskInput.checkRawInput(rawInput, taskCtx, rawCmp);

        assertEquals(totalKeys, (long)counts.get1());
        assertEquals(totalKeys * valuesPerKey, (long)counts.get2());
    }

    private String key(int keyIdx) {
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

    public void testGroupedRawInput() throws Exception {
        GridUnsafeMemory mem = new GridUnsafeMemory(0);

        HadoopJobInfo info = new JobInfo();

        HadoopSkipList list = new HadoopSkipList(info, mem);

        HadoopTaskContext taskCtx = new TaskContext() {
            @Override public HadoopSerialization keySerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(Text.class);
            }

            @Override public HadoopSerialization valueSerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(IntWritable.class);
            }

            @Override public Comparator<Object> groupComparator() {
                return (Comparator)rawCmp;
            }
        };

        try (HadoopTaskOutput out = list.startAdding(taskCtx)) {
            final Text keyText = new Text();
            final IntWritable intVal = new IntWritable();

            int totalCnt = 0;

            for (int v = 0; v < 3; v++) {
                for (int k = 0; k < 2; k++) {
                    keyText.set(key(k));
                    intVal.set(totalCnt);

                    assertTrue(out.write(keyText, intVal));

                    totalCnt++;

                    if (totalCnt % 10 == 0)
                        System.out.println("written: " + totalCnt);
                }
            }

            HadoopTaskInput rawIn = list.rawInput();
            HadoopTaskInput groupedRawInput = new HadoopSkipList.GroupedRawInput(rawIn, rawCmp);
            HadoopTaskInput objectInput = new HadoopSkipList.InputForRawInput(groupedRawInput, taskCtx);

            int keyCnt = 0;
            int valCnt = 0;

            final Text prevKey = new Text();

            while (objectInput.next()) {
                Text key = (Text)objectInput.key();

                keyCnt++;

                System.out.println("k = " + key);

                if (prevKey.toString() != null) {
                    int cmp = rawCmp.compare(prevKey, key);

                    assertTrue(cmp <= 0);
                }

                prevKey.set(key); // Save value.

                Iterator<IntWritable> it = (Iterator)objectInput.values();

                IntWritable val;

                while (it.hasNext()) {
                    val = it.next();

                    valCnt++;

                    System.out.println("     v = " + val);
                }
            }

            assertEquals(2, keyCnt);
            assertEquals(3 * 2, valCnt);
        }
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 1000 * super.getTestTimeout();
    }
}