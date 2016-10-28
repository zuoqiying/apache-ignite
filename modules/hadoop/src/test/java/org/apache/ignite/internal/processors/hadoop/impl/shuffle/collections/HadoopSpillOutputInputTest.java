package org.apache.ignite.internal.processors.hadoop.impl.shuffle.collections;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.hadoop.HadoopJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopSerialization;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInput;
import org.apache.ignite.internal.processors.hadoop.impl.v2.HadoopWritableSerialization;
import org.apache.ignite.internal.processors.hadoop.shuffle.HadoopShuffleJob;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopMergingTaskInput;
import org.apache.ignite.internal.processors.hadoop.shuffle.collections.HadoopMultimap;
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
    // TODO: 2) grouping of rawValues (using taskCtx.groupingCmp) should be implemented and checked.

    /** */
    private final int totalKeys = 300;

    /** */
    private final int valuesPerKey = 10;

    /**
     * Limit for one multimap (now in k-v s, not in bytes).
     */
    private final long limit = 100L;

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

        final HadoopSerialization keySer = taskCtx.keySerialization();
        final HadoopSerialization valSer = taskCtx.valueSerialization();

        // 1. Output
        final HadoopSpillingTaskOutput out = new HadoopSpillingTaskOutput(
            new HadoopSpillableMultimap[] {
                new HadoopSpillableMultimap(taskCtx, job, mem, limit),
                new HadoopSpillableMultimap(taskCtx, job, mem, limit)
        },  new String[] { "alpha", "beta" }, taskCtx, log());

        final Text keyText = new Text();
        final Text valText = new Text();

        int totalCnt = 0;

        for (int v = 0; v<valuesPerKey; v++) {
            for (int k = 0; k< totalKeys; k++) {
                keyText.set(key(k));
                valText.set(value(k, v));

                assertTrue(out.write(keyText, valText));

                totalCnt++;

                if (totalCnt % 10 == 0)
                    System.out.println("written: " + totalCnt);
            }
        }

        HadoopMultimap currMap = out.currOut();

        // TODO: need a sync with spilling threads there. Otherwise not all files can be taken,
        // TODO: objects can be between the queues, etc.
        List<String> files = out.getFiles();

        System.out.println("files = " + files);

        // 2. Input.
        final HadoopMergingTaskInput input = new HadoopMergingTaskInput(new HadoopMultimap[] { currMap },
            files.toArray(new String[files.size()]), rawCmp);

        int keyCnt = 0;
        int valCnt = 0;

        Text k = new Text();
        Text v = new Text();

        final HadoopShuffleJob.UnsafeValue prevKey = new HadoopShuffleJob.UnsafeValue();

        try (HadoopTaskInput rawIn = input.rawInput()) {
            HadoopShuffleJob.UnsafeValue key;
            HadoopShuffleJob.UnsafeValue val;

            while (rawIn.next()) {
                key = (HadoopShuffleJob.UnsafeValue)rawIn.key();

                keyCnt++;

                k = (Text)deserialize(keySer, key, k);

                System.out.println("k = " + k);

                if (prevKey.hasData()) {
                    int cmp = rawCmp.compare(
                        prevKey.getBuf(), prevKey.getOff(), prevKey.size(),
                        key.getBuf(), key.getOff(), key.size());

                    assertTrue(cmp < 0); // Sorting; // TODO: check why this passes? this w/o = it should fail, no?
                }

                prevKey.readFrom(key); // Save value.

                Iterator<HadoopShuffleJob.UnsafeValue> it = (Iterator)rawIn.values();

                while (it.hasNext()) {
                    val = it.next();

                    v = (Text) deserialize(valSer, val, v);

                    valCnt++;

                    System.out.println("     v = " + v);
                }
            }
        }

        assertEquals(totalKeys, keyCnt); // TODO: now fails because ungrouped.
        assertEquals(totalKeys * valuesPerKey , valCnt);
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

}