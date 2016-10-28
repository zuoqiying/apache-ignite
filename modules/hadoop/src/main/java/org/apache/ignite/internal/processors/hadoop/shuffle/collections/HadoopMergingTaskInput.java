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
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInput;
import org.apache.ignite.internal.processors.hadoop.shuffle.HadoopShuffleJob.*;
import org.apache.ignite.internal.util.typedef.F;

/**
 * TODO: support limiting the number of semultaneously open files (io.merge.factor).
 */
public class HadoopMergingTaskInput {
    /** */
    private final RawComparator sortingRawCmp;

    /** */
    private final HadoopPriorityQueue<Segment> pq;

    /** */
    final Comparator <Segment> cmp = new Comparator<Segment> () {
        @Override public int compare(Segment s1, Segment s2) {
            UnsafeValue p1 = (UnsafeValue)s1.key();
            UnsafeValue p2 = (UnsafeValue)s2.key();

            return sortingRawCmp.compare(
                p1.getBuf(), p1.getOff(), p1.size(),
                p2.getBuf(), p2.getOff(), p2.size());
        }
    };

    public HadoopMergingTaskInput(final HadoopMultimap[] inMemorySegments, final String[] files,
        RawComparator sortingRawCmp) throws IgniteCheckedException {
        this.sortingRawCmp = sortingRawCmp;

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

                Segment seg = new Segment(true, ri);

                seg.next();

                pq.put(seg);
            }
        }

        for (String f: files) {
            DataInput din = getDataInput(f);

            HadoopTaskInput fri = new FileRawInput(din);

            Segment seg = new Segment(false, fri);

            seg.next();

            pq.put(seg);
        }
    }

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
        return new MergedInput();
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

        boolean keyMarkerRead = false;

        FileRawInput(DataInput din) {
            this.din = din;
        }

        @Override public boolean next() {
            try {
                try {
                    if (!keyMarkerRead) {
                        byte marker = din.readByte();

                        assert marker == HadoopSpillableMultimap.MARKER_KEY;

                        keyMarkerRead = true;
                    }

                    int size = din.readInt(); // read or just skip these bytes;

                    curKey.readFrom(din, size);

                    int valIdx = 0;

                    while (true) {
                        byte marker = din.readByte();

                        if (marker == HadoopSpillableMultimap.MARKER_KEY) {
                            // Next key in the stream:
                            keyMarkerRead = true;

                            assert valIdx > 0 ; // Ensure we have at least 1 value, otherwise file is corrupted.

                            // truncate extra buffers
                            while (curValues.size() > valIdx + 1)
                                curValues.remove(curValues.size() - 1);

                            return true; // have value
                        }
                        else if (marker == HadoopSpillableMultimap.MARKER_VALUE) {
                            size = din.readInt();

                            if (valIdx == curValues.size())
                                curValues.add(new UnsafeValue(size));

                            curValues.get(valIdx).readFrom(din, size);

                            valIdx++;
                        } else
                            throw new IgniteException("Unknown marker");
                    }
                }
                catch (EOFException eof) {
                    close();

                    return false; // No more value.
                }
            } catch (IOException | IgniteCheckedException e) {
                throw new IgniteException(e);
            }
        }

        @Override public Object key() {
            if (!curKey.hasData())
                throw new NoSuchElementException();

            return curKey;
        }

        @Override public Iterator<?> values() {
            if (!curKey.hasData() || curValues.isEmpty())
                throw new NoSuchElementException();

            return curValues.iterator();
        }

        @Override public void close() throws IgniteCheckedException {
            curKey.close();

            if (curValues != null) {
                for (UnsafeValue v: curValues)
                    v.close();

                curValues .clear();
            }
        }
    }

    class MergedInput implements HadoopTaskInput {
        /** The current (least) segment. */
        private HadoopTaskInput curSeg;

        MergedInput() {
            // noop
        }

        @Override public boolean next() {
            Segment seg = pq.top(); // ~ peek()

            if (seg.next())
                pq.adjustTop(); // heapify the top again.
            else {
                Segment endedSegment = pq.pop(); // ~ poll()
                // NB: this operation will re-heapify the queue automatically.

                assert endedSegment == seg;
            }

            // Update top:
            curSeg = pq.top();

            return curSeg != null;
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
            return currentSegment().key();
        }

        @Override public Iterator<?> values() {
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
        private final HadoopTaskInput rawInput;
        private final boolean inMem;

        Segment(boolean inMem, HadoopTaskInput rawInput) {
            this.inMem = inMem;
            this.rawInput = rawInput;
        }

        public final boolean isInMemory() {
            return inMem;
        }

        @Override public Object key() {
            return rawInput.key();
        }

        @Override public boolean next() {
            return rawInput.next();
        }

        @Override public Iterator<?> values() {
            return rawInput.values();
        }

        @Override public void close() throws IgniteCheckedException {
            rawInput.close();
        }
    }
}
