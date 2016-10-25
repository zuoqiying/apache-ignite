package org.apache.ignite.internal.processors.hadoop.shuffle.collections;

import java.io.DataInput;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.apache.hadoop.io.RawComparator;
import org.apache.ignite.internal.processors.hadoop.HadoopJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;
import org.apache.ignite.internal.util.offheap.unsafe.GridUnsafeMemory;

/**
 * TODO: support limiting the number of semultaneously open files.
 */
public class HadoopMergingTaskInput {

    final RawComparator sortingRawCmp;

    int numSegments;

    DataInput[] inputs;

    PriorityQueue<Segment> pq;

    String[] files;

    /** */
    private final HadoopTaskContext ctx;
    /** */
    private final HadoopJobInfo info;
    /** */
    private final GridUnsafeMemory mem;

    Comparator <Segment> cmp = new Comparator<Segment> () {
        @Override public int compare(Segment s1, Segment s2) {
            ByteArrayPtr p1 = s1.currentKey();
            ByteArrayPtr p2 = s2.currentKey();

            return sortingRawCmp.compare(p1.bytes, p1.off, p1.size, p2.bytes, p2.off, p2.size);
        }
    };

    HadoopMergingTaskInput() {

    }

    void init() {
        pq = new PriorityQueue<>(10, cmp);

        for (String f: files) {
            String file = getFile();

            HadoopSpillableMultimap m = new HadoopSpillableMultimap(ctx, info, mem, file);

            m.unSpill(); // Read from the file.
        }
    }

    static class Segment {
        DataInput input;

        boolean isInMemory() {

        }

        boolean nextKey() {

        }

        ByteArrayPtr currentKey() {

        }
    }

    static class ByteArrayPtr {
        byte[] bytes;
        int off;
        int size;
    }

}
