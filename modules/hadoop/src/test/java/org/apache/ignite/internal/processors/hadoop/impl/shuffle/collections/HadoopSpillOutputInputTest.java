package org.apache.ignite.internal.processors.hadoop.impl.shuffle.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
import org.apache.ignite.internal.processors.hadoop.shuffle.HadoopShuffleJob;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopMergingTaskInput;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopMultimap;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSkipList;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSpillableMultimap;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopSpillingTaskOutput;
import org.apache.ignite.internal.util.io.GridUnsafeDataInput;
import org.apache.ignite.internal.util.offheap.unsafe.GridUnsafeMemory;
import org.jetbrains.annotations.Nullable;

/**
 * Tests Hadoop
 */
public class HadoopSpillOutputInputTest extends HadoopAbstractMapTest {
    // TODO: 1) HadoopMultimap#write() return value should be consistent with its size, not a "mem" size.

    /** */
    private final int totalKeys = 50;

    /** */
    private final int valuesPerKey = 2;

    /**
     * Limit for one multimap (now in k-v s, not in bytes).
     */
    private final long limit = 70;

    /** */
    private static final RawComparator<Text> rawCmp = new Text.Comparator();

    public void testOutput() throws Exception {
        GridUnsafeMemory mem = new GridUnsafeMemory(0);

        HadoopJobInfo job = new JobInfo();

        HadoopTaskContext taskCtx = new TaskContext() {
            @Override public HadoopSerialization keySerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(Text.class);
            }

            @Override public HadoopSerialization valueSerialization() throws IgniteCheckedException {
                return new HadoopWritableSerialization(Text.class);
            }

            @Override public Comparator<Object> groupComparator() {
                return (Comparator)rawCmp;
            }
        };

        // 1. Output
        final HadoopSpillingTaskOutput out = new HadoopSpillingTaskOutput(
            new HadoopSpillableMultimap[] {
                new HadoopSpillableMultimap(taskCtx, job, mem, limit),
                new HadoopSpillableMultimap(taskCtx, job, mem, limit)
        }, /*Base names*/ new String[] { "alpha", "beta" }, taskCtx, log());

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

        // TODO: need a sync with spilling threads there. Otherwise not all files can be taken,
        // TODO: objects can be between the queues, etc.
        Thread.sleep(2000); //

        HadoopMultimap currMap = out.currOut();

        List<String> files = out.getFiles();

        out.close();

        System.out.println("files = " + files);

        // 2. Input.
        final HadoopMergingTaskInput input =
            new HadoopMergingTaskInput(new HadoopMultimap[] { currMap },
            files.toArray(new String[files.size()]), rawCmp);

        HadoopTaskInput rawInput = input.rawInput();
        HadoopTaskInput groupedRawInput = new HadoopSkipList.GroupedRawInput(rawInput, rawCmp);
        //HadoopTaskInput objectInput = new HadoopSkipList.InputForRawInput(rawInput, taskCtx);

//        int keyCnt = 0;
//        int valCnt = 0;
//
//        Text prevKey = new Text();
//
//        groupedRawInput
//
//        while (objectInput.next()) {
//            Text key = (Text)objectInput.key();
//
//            keyCnt++;
//
//            System.out.println("k = " + key);
//
//            if (prevKey.toString() != null) {
//                int cmp = rawCmp.compare(prevKey, key);
//
//                assertTrue(cmp <= 0);
//            }
//
//            prevKey.set(key); // Save value.
//
//            Iterator<Text> it = (Iterator)objectInput.values();
//
//            Text val;
//
//            while (it.hasNext()) {
//                val = it.next();
//
//                valCnt++;
//
//                System.out.println("     v = " + val);
//            }
//        }
//
//        assertEquals(totalKeys, keyCnt); // TODO: now fails because ungrouped.
//        assertEquals(totalKeys * valuesPerKey , valCnt);

        checkRawOutput(groupedRawInput, taskCtx, totalKeys, totalKeys * valuesPerKey);
    }

    private void checkRawOutput(HadoopTaskInput in, HadoopTaskContext taskCtx,
            int expKeyCnt, int expValCnt) throws Exception {
        final HadoopSerialization keySer = taskCtx.keySerialization();
        final HadoopSerialization valSer = taskCtx.valueSerialization();

        int keyCnt = 0;
        int valCnt = 0;

        Text k = new Text();
        Text v = new Text();

        final HadoopShuffleJob.UnsafeValue prevKey = new HadoopShuffleJob.UnsafeValue();

        try {
            HadoopShuffleJob.UnsafeValue key;
            HadoopShuffleJob.UnsafeValue val;

            while (in.next()) {
                key = (HadoopShuffleJob.UnsafeValue)in.key();

                keyCnt++;

                k = (Text)deserialize(keySer, key, k);

                System.out.println("k = " + k);

                if (prevKey.hasData()) {
                    int cmp = rawCmp.compare(
                        prevKey.getBuf(), prevKey.getOff(), prevKey.size(),
                        key.getBuf(), key.getOff(), key.size());

                    assertTrue(cmp <= 0);
                }

                prevKey.readFrom(key); // Save value.

                Iterator<HadoopShuffleJob.UnsafeValue> it = (Iterator)in.values();

                while (it.hasNext()) {
                    val = it.next();

                    v = (Text) deserialize(valSer, val, v);

                    valCnt++;

                    System.out.println("     v = " + v);
                }
            }
        }
        finally {
            in.close();
        }

        assertEquals(expKeyCnt, keyCnt);
        assertEquals(expValCnt, valCnt);
    }

    private final GridUnsafeDataInput dataInput = new GridUnsafeDataInput();

    private Object deserialize(HadoopSerialization ser, HadoopShuffleJob.UnsafeValue uv, @Nullable Object reuse)
        throws IgniteCheckedException {
        dataInput.bytes(uv.getBuf(), uv.getOff(), uv.size() + uv.getOff());

        return ser.read(dataInput, reuse);
    }

    private String key(int keyIdx) {
        return "key-" + keyIdx;
    }

    private String value(int keyIdx, int valIdx) {
        return "value-" + keyIdx + "-" + valIdx;
    }

    private Iterator<Integer> createIterator() {
        Iterator<Integer> it00 = new ArrayList<Integer>().iterator();
        Iterator<Integer> it0 = new ArrayList<>(Arrays.asList(new Integer[] { 0, 1, 2, 3 })).iterator();
        Iterator<Integer> it1 = new ArrayList<>(Arrays.asList(new Integer[] { 4, 5, 6, 7 })).iterator();
        Iterator<Integer> it2 = new ArrayList<>(Arrays.asList(new Integer[] { 8, 9, 10, 11 })).iterator();
        Iterator<Integer> it20 = new ArrayList<Integer>().iterator();

        Iterator<Iterator<Integer>> itIt = (Iterator)Arrays.asList(
            new Iterator[] { it00, it0, it1, it2, it20 }).iterator();

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
}