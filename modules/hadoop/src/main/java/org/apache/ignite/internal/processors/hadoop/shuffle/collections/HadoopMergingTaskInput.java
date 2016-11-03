package org.apache.ignite.internal.processors.hadoop.shuffle.collections;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.hadoop.HadoopJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopSerialization;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInput;
import org.apache.ignite.internal.processors.hadoop.shuffle.HadoopShuffleJob;
import org.apache.ignite.internal.processors.hadoop.shuffle.HadoopShuffleJob.*;
import org.apache.ignite.internal.util.io.GridUnsafeDataInput;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.T2;
import org.jetbrains.annotations.Nullable;

/**
 * TODO: support limiting the number of semultaneously open files (io.merge.factor).
 */
public class HadoopMergingTaskInput {
    /** */
    private final RawComparator sortingRawCmp;

    /** */
    private final HadoopPriorityQueue<Segment> pq;

    private final HadoopJobInfo info; // TODO: remove

    /** */
    final Comparator<Segment> cmp = new Comparator<Segment> () {
        @Override public int compare(Segment s1, Segment s2) {
            UnsafeValue p1 = (UnsafeValue)s1.key();
            UnsafeValue p2 = (UnsafeValue)s2.key();

            assert p1 != p2;

            return sortingRawCmp.compare(
                p1.getBuf(), p1.getOff(), p1.size(),
                p2.getBuf(), p2.getOff(), p2.size());
        }
    };

    public HadoopMergingTaskInput(final HadoopMultimap[] inMemorySegments, final String[] files,
        RawComparator sortingRawCmp, HadoopTaskContext taskCtx, HadoopJobInfo info) throws IgniteCheckedException {
        this.sortingRawCmp = sortingRawCmp;
        this.info = info;

        // init inputs from files;
        // Create the priority queue and fill it up.
        pq = new HadoopPriorityQueue<Segment>(cmp) {
            {
                initialize(inMemorySegments.length + files.length);
            }
        };

        pq.clear();

        if (!F.isEmpty(inMemorySegments)) {
            for (HadoopMultimap hm: inMemorySegments) {
                HadoopTaskInput ri = hm.rawInput();

//                System.out.println("#### memory segment: ");
//                checkRawInput(ri, taskCtx, sortingRawCmp);

                Segment seg = new Segment(true, ri);

                pq.put(seg);
            }
        }

        for (String f: files) {
            DataInput din = getDataInput(f);

//            // TODO: experimantal:
//            HadoopSpillableMultimap m = new HadoopSpillableMultimap(taskCtx, info, new GridUnsafeMemory(0));
//            m.unSpill(din);
//            HadoopTaskInput fri = m.rawInput(); //new FileRawInput(din);

            FileRawInput fri = new FileRawInput(din);

//            System.out.println("#### file : " + f);
//            checkRawInput(fri, taskCtx, sortingRawCmp);

            Segment seg = new Segment(false, fri);

            pq.put(seg);
        }
    }

    /**
     * @param file The file to read.
     * @return The input.
     */
    private DataInput getDataInput(String file) {
        try {
            return new DataInputStream(new FileInputStream(file));
        } catch (IOException ioe) {
            throw new IgniteException(ioe);
        }
    }

    /**
     * Entry point method. This input will be used
     * Creates new raw input merged from all the original sources.
     * @return
     */
    public HadoopTaskInput rawInput() {
        // TODO: the number of inputs should be limited, since each input creates N input streams.
        return new MergedInput(pq);
    }

    // TODO: ? implement accept() method to make simpler
    // TODO: to send ShuffleMessages , see
    // TODO: org.apache.ignite.internal.processors.hadoop.shuffle.HadoopShuffleJob.collectUpdatesAndSend()
    // TODO: problem is that HadoopMultimap.Visitor accepts values in "unsafe" form (long ptr, long size), while
    // TODO: the raw Iterator we have gives "UnsafeValue" byte[] buffers.
//    public void accept(HadoopMultimap.Visitor v) {
//        HadoopTaskInput hti = rawInput();
//
//        while (hti.next()) {
//            UnsafeValue k = (UnsafeValue)hti.key();
//
//            v.visitKey();
//
//            Iterator<UnsafeValue> it = hti.values();
//
//
//        }
//    }

    /**
     *
     */
    public static class FileRawInput implements HadoopTaskInput {
        private final DataInput din;

        private final UnsafeValue curKey = new UnsafeValue();

        private final List<UnsafeValue> curValues = new ArrayList<>(1);

        /**
         * Means that the current read position is beyond the key type marker.
         */
        boolean keyMarkerRead = false;

        /** Read bytes counter. */
        private int read;

        FileRawInput(DataInput din) {
            this.din = din;
        }

        @Override public boolean next() {
            //System.out.println(" fri: next");

            try {
                int valCnt = 0;

                try {
                    valCnt = 0;

                    if (!keyMarkerRead) {
                        byte marker = din.readByte();
                        read++;

                        assert marker == HadoopSpillableMultimap.MARKER_KEY;

                        keyMarkerRead = true;
                    }

                    int size = din.readInt(); // read or just skip these bytes;
                    read += 4;

                    curKey.readFrom(din, size);
                    read += size;

                    //System.out.println("### Key: " + curKey);

                    while (true) {
                        byte marker = din.readByte();
                        read++;

                        if (marker == HadoopSpillableMultimap.MARKER_KEY) {
                            // Next key in the stream:
                            keyMarkerRead = true;

                            assert valCnt > 0 ; // Ensure we have at least 1 value, otherwise file is corrupted.

                            // cut & clear extra buffers
                            while (curValues.size() > valCnt) {
                                UnsafeValue uv = curValues.remove(curValues.size() - 1);

                                uv.close();
                            }

                            assert curValues.size() == valCnt;

                            return true; // have value
                        }
                        else if (marker == HadoopSpillableMultimap.MARKER_VALUE) {
                            size = din.readInt();
                            read += 4;

                            if (valCnt == curValues.size())
                                // The list is full:
                                curValues.add(new UnsafeValue(size));

                            curValues.get(valCnt).readFrom(din, size);
                            read += size;

                            //System.out.println("    ### Val: " + curValues.get(valCnt));

                            valCnt++;
                        } else
                            throw new IgniteException("Unknown marker");
                    }
                }
                catch (EOFException eof) {
                    //System.out.println("EOF. read bytes = " + read);

                    return (valCnt > 0);
                }
            } catch (IOException e) {
                throw new IgniteException(e);
            }
        }

        @Override public Object key() {
            if (!curKey.hasData())
                throw new NoSuchElementException();

            //System.out.println(" fri: Key: " + curKey);

            return curKey;
        }

        @Override public Iterator<?> values() {
            if (!curKey.hasData() || curValues.isEmpty())
                throw new NoSuchElementException();

//            for (UnsafeValue uv: curValues)
//                System.out.println("   fri: val: " + uv);

            return curValues.iterator();
        }

        @Override public void close() throws IgniteCheckedException {
            //System.out.println(" fri: close");

            curKey.close();

            if (curValues != null) {
                for (UnsafeValue v: curValues)
                    v.close();

                curValues.clear();
            }
        }
    }

    static class MergedInput implements HadoopTaskInput {
        /**
         * The priority queue.
         */
        private final HadoopPriorityQueue<Segment> pq;

        /** */
        private boolean first = true;

        /**
         * The current (least) segment.
         */
        private Segment curSeg;

        MergedInput(HadoopPriorityQueue<Segment> pq) {
            //System.out.println("Merged input created.");

            this.pq = pq;
        }

        @Override public boolean next() {
            if (first)
                // #next() is already called one time on each objects that
                // participated the comparison.
                first = false;
            else {
                Segment seg = pq.top(); // peek()

                if (seg.next())
                    pq.adjustTop(); // heapify the top again.
                else {
                    Segment endedSegment = pq.pop(); // ~ poll()
                    // NB: this operation will re-heapify the queue automatically.

                    //System.out.println("#### Segment closed: " + endedSegment);

                    assert endedSegment == seg;
                }
            }

            curSeg = pq.top(); // peek()

            boolean b = curSeg != null;

            //System.out.println("#### seg: next : " + b);

            return b;
        }

        /**
         * @return The current segment.
         */
        private HadoopTaskInput currentSegment() {
            if (curSeg == null)
                throw new NoSuchElementException();

            return curSeg;
        }

        @Override public Object key() {
            Object k = currentSegment().key();
            //System.out.println("#### seg: k : " + k);
            return k;
        }

        @Override public Iterator<?> values() {
            //System.out.println("#### seg: v : ");
            return currentSegment().values();
        }

        @Override public void close() throws IgniteCheckedException {
            if (curSeg != null)
                curSeg.close();
        }
    }

    /**
     * The element of a priority queue.
     */
    static class Segment implements AutoCloseable, HadoopTaskInput {
        /** */
        private final HadoopTaskInput rawInput;

        /** */
        private final boolean inMem;

        /** 'true' if #next() was called at least one time, 'false' otherwise. */
        private boolean initialized;

        Segment(boolean inMem, HadoopTaskInput rawInput) {
            this.inMem = inMem;
            this.rawInput = rawInput;
        }

        public final boolean isInMemory() {
            return inMem;
        }

        private void nextIfNeeded() {
            if (!initialized) {
                System.out.println("##### auto-next... inMem = " + inMem + ", this = " + this);

                boolean hasNext = next();

                if (!hasNext)
                    throw new NoSuchElementException();
            }
        }

        @Override public Object key() {
            nextIfNeeded();

            return rawInput.key();
        }

        @Override public boolean next() {
            initialized = true;

            boolean b = rawInput.next();

            //System.out.println("-- seg next: (" + inMem + ") -- " + b);

            return b;
        }

        @Override public Iterator<?> values() {
            nextIfNeeded();

            return rawInput.values();
        }

        @Override public void close() throws IgniteCheckedException {
            rawInput.close();
        }
    }

    /**
     * DIAGNOSTIC
     *
     * @param in
     * @param taskCtx
     * @return
     * @throws Exception
     */
    public static T2<Long, Long> checkRawInput(HadoopTaskInput in, HadoopTaskContext taskCtx, RawComparator rawCmp) throws IgniteCheckedException {
        final HadoopSerialization keySer = taskCtx.keySerialization();
        final HadoopSerialization valSer = taskCtx.valueSerialization();

        long keyCnt = 0;
        long valCnt = 0;

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

                System.out.println(" k = " + k);

                if (prevKey.hasData()) {
                    int cmp = rawCmp.compare(
                        prevKey.getBuf(), prevKey.getOff(), prevKey.size(),
                        key.getBuf(), key.getOff(), key.size());

                    assert (cmp <= 0);
                }

                prevKey.readFrom(key); // Save value.

                Iterator<HadoopShuffleJob.UnsafeValue> it = (Iterator)in.values();

                while (it.hasNext()) {
                    val = it.next();

                    v = (Text) deserialize(valSer, val, v);

                    valCnt++;

                    System.out.println("    v = " + v);
                }
            }
        }
        finally {
            in.close();
        }

        return new T2<>(keyCnt, valCnt);
    }

    private static final GridUnsafeDataInput dataInput = new GridUnsafeDataInput();

    private static Object deserialize(HadoopSerialization ser, HadoopShuffleJob.UnsafeValue uv, @Nullable Object reuse)
        throws IgniteCheckedException {
        //System.out.println("UV: buf=" + uv.getBuf().length + ", off=" + uv.getOff() + ", size=" + uv.size());

        dataInput.bytes(uv.getBuf(), uv.getOff(), uv.size() + uv.getOff()); //TODO: clarify if this is correct call.

        return ser.read(dataInput, reuse);
    }

}
