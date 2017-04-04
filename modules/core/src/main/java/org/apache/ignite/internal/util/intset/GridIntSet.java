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

import java.util.BitSet;
import org.apache.ignite.internal.util.typedef.internal.U;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds set of integers.
 * <p>
 * Structure:
 * <p>
 * Each segment stores SEGMENT_SIZE values, possibly in compressed format.
 * <p>
 * Used storage format depends on segmen's fill factor.
 * <p>
 * Note: implementation is not thread safe.
 * <p>
 * TODO equals/hashcode
 * TODO FIXME cache bit masks like 1 << shift ?
 * TODO HashSegment worth it?
 * TODO replace power of two arythmetics with bit ops.
 * TODO fixme overflows ?
 */
public class GridIntSet implements Externalizable {
    public static final GridIntSet EMPTY = new GridIntSet();

    /** */
    private static final long serialVersionUID = 0L;

    static final short SEGMENT_SIZE = 8192; // Must be power of two.

    private static final int SEGMENT_SHIFT_BITS = Integer.numberOfTrailingZeros(SEGMENT_SIZE);

    private static final int SHORT_BITS = Short.SIZE;

    private static final int MAX_WORDS = SEGMENT_SIZE / SHORT_BITS;

    private static final int WORD_SHIFT_BITS = 4;

    private static final int THRESHOLD = MAX_WORDS;

    private static final int THRESHOLD2 = SEGMENT_SIZE - THRESHOLD;

    private static final int WORD_MASK = 0xFFFF;

    private Segment indices = new ArraySegment(16);

    private Map<Short, Segment> segments = new HashMap<>();

    private int size;

    /**
     * @param v V.
     */
    public boolean add(int v) {
        short segIdx = (short) (v >> SEGMENT_SHIFT_BITS);

        short segVal = (short) (v & (SEGMENT_SIZE - 1));

        Segment seg;

        try {
            if (indices.add(segIdx))
                segments.put(segIdx, (seg = new ArraySegment()));
            else
                seg = segments.get(segIdx);
        } catch (ConversionException e) {
            indices = e.segment;

            segments.put(segIdx, (seg = new ArraySegment()));
        }

        try {
            boolean added = seg.add(segVal);

            if (added)
                size++;

            return added;
        } catch (ConversionException e) {
            segments.put(segIdx, e.segment);

            size++;
        }

        return true;
    }

    /**
     * @param v V.
     */
    public boolean remove(int v) {
        short segIdx = (short) (v >> SEGMENT_SHIFT_BITS);

        short segVal = (short) (v & (SEGMENT_SIZE - 1));

        Segment seg = segments.get(segIdx);

        if (seg == null)
            return false;

        try {
            boolean rmv = seg.remove(segVal);

            if (rmv) {
                size--;

                if (seg.size() == 0) {
                    try {
                        indices.remove(segIdx);
                    } catch (ConversionException e) {
                        indices = e.segment;
                    }

                    segments.remove(segIdx);
                }
            }

            return rmv;
        } catch (ConversionException e) {
            assert seg.size() != 0; // Converted segment cannot be empty.

            segments.put(segIdx, e.segment);

            size--;
        }

        return true;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(int v) {
        short segIdx = (short) (v >> SEGMENT_SHIFT_BITS);

        short segVal = (short) (v & (SEGMENT_SIZE - 1));

        Segment segment = segments.get(segIdx);

        return segment != null && segment.contains(segVal);
    }

    public int first() {
        short idx = indices.first();

        if (idx == -1)
            return -1;

        return segments.get(idx).first() + idx * SEGMENT_SIZE; // TODO FIXME use bit ops.
    }

    public int last() {
        short idx = indices.last();

        if (idx == -1)
            return -1;

        return segments.get(idx).last() + idx * SEGMENT_SIZE;
    }

    private abstract class IteratorImpl implements Iterator {
        /** Segment index. */
        private short idx;

        /** Segment index iterator. */
        private Iterator idxIter;

        /** Current segment value iterator. */
        private Iterator it;

        /** Current segment. */
        private Segment seg;

        /** Current value. */
        private short cur;

        public IteratorImpl() {
            this.idxIter = iter(indices);
        }

        /** */
        private void advance() {
            if (it == null || !it.hasNext())
                if (idxIter.hasNext()) {
                    idx = (short) idxIter.next();

                    seg = segments.get(idx);

                    it = iter(seg);
                } else
                    it = null;
        }

        protected abstract Iterator iter(Segment segment);

        @Override public boolean hasNext() {
            return idxIter.hasNext() || (it != null && it.hasNext());
        }

        @Override public int next() {
            advance();

            cur = (short) it.next();

            return cur + idx * SEGMENT_SIZE;
        }

        /** {@inheritDoc} */
        @Override public void remove() {
            try {
                it.remove();

                if (seg.size() == 0) {
                    try {
                        idxIter.remove();
                    } catch (ConversionException e) {
                        indices = e.segment;

                        // Re-crete iterator and move it to right position.
                        idxIter = iter(indices);

                        idxIter.skipTo(idx);
                    }

                    segments.remove(idx);
                }
            } catch (ConversionException e) {
                segments.put(idx, e.segment);

                // Segment was changed, fetch new iterator and reposition it.
                it = iter(seg = e.segment);

                it.skipTo(cur);

                advance();
            }

            size--;
        }

        /** {@inheritDoc} */
        @Override public void skipTo(int v) {
            short segIdx = (short) (v >> SEGMENT_SHIFT_BITS);

            short segVal = (short) (v & (SEGMENT_SIZE - 1));

            if (segIdx == idx) {
                it.skipTo(segVal);

                advance();

                return;
            }

            idxIter.skipTo(segIdx);

            it = null;

            advance(); // Force iterator refresh.

            it.skipTo(segVal);

            advance(); // Switch to next segment if needed.
        }
    }

    /** */
    public Iterator iterator() {
        return new IteratorImpl() {
            @Override protected Iterator iter(Segment segment) {
                return segment.iterator();
            }
        };
    }

    /** */
    public Iterator reverseIterator() {
        return new IteratorImpl() {
            @Override protected Iterator iter(Segment segment) {
                return segment.reverseIterator();
            }
        };
    }

    public int size() {
        return size;
    }

    public static interface Iterator {
        public boolean hasNext();

        public int next();

        public void remove();

        // Next call must be hasNext to ensure value is present.
        public void skipTo(int val);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        StringBuilder b = new StringBuilder("[");

        Iterator it = iterator();

        while(it.hasNext()) {
            b.append(it.next());

            if (it.hasNext()) b.append(", ");
        }

        b.append("]");

        return b.toString();
    }

    /** */
    interface Segment {
        /** Segment data. */
        public short[] data();

        /**
         * Returns number of element if array used to hold data.
         * @return used elements count.
         */
        public int used();

        public boolean add(short val) throws ConversionException;

        public boolean remove(short base) throws ConversionException;

        public boolean contains(short v);

        public int size();

        /**
         * Returns min allowed size.
         * @return Min size.
         */
        public int minSize();

        /**
         * Returns max allowed size.
         * @return Max size.
         */
        public int maxSize();

        public short first();

        public short last();

        public Iterator iterator();

        public Iterator reverseIterator();
    }

    /**
     * TODO store used counter.
     */
    static class BitSetSegment implements Segment {
        private short[] words;

        private int count;

        public BitSetSegment() {
            this(0);
        }

        public BitSetSegment(int maxVal) {
            assert maxVal <= SEGMENT_SIZE;

            words = new short[nextPowerOfTwo(wordIndex(maxVal))];
        }

        public BitSetSegment(short[] data) {
            words = data;

            assert U.isPow2(data.length);

            for (short word : words)
                count += Integer.bitCount(word & 0xFFFF);
        }

        @Override public int used() {
            return words.length;
        }

        @Override public boolean add(short val) throws ConversionException {
            int wordIdx = wordIndex(val);

            if (wordIdx >= words.length)
                words = Arrays.copyOf(words, Math.max(words.length * 2, wordIdx + 1)); // TODO shift

            short wordBit = (short) (val - wordIdx * SHORT_BITS); // TODO FIXME bits

            assert 0 <= wordBit && wordBit < SHORT_BITS : "Word bit is within range";

            int mask = (1 << wordBit);

            boolean exists = (words[(wordIdx)] & mask) == mask;

            if (exists)
                return false;

            if (count == THRESHOLD2) { // convert to inverted array set.
                Segment seg = convertToInvertedArraySet();

                seg.add(val);

                throw new ConversionException(seg);
            }

            count++;

            words[(wordIdx)] |= mask;

            return true;
        }

        @Override public boolean remove(short val) throws ConversionException {
            int wordIdx = wordIndex(val);

            short wordBit = (short) (val - wordIdx * SHORT_BITS); // TODO FIXME

            int mask = 1 << wordBit;

            boolean exists = (words[(wordIdx)] & mask) == mask;

            if (!exists)
                return false;

            count--;

            words[wordIdx] &= ~mask;

            if (count == THRESHOLD - 1)
                throw new ConversionException(convertToArraySet());

            return true;
        }

        /** */
        private Segment convertToInvertedArraySet() throws ConversionException {
            FlippedArraySegment seg = new FlippedArraySegment();

            Iterator it = iterator();

            int i = 0;

            while (it.hasNext()) {
                int id = it.next();

                for (; i < id; i++)
                    seg.remove((short) i);

                i = id + 1;
            }

            while(i < SEGMENT_SIZE)
                seg.remove((short) i++);

            return seg;
        }

        /** */
        private Segment convertToArraySet() throws ConversionException {
            ArraySegment seg = new ArraySegment(THRESHOLD);

            Iterator it = iterator();

            while (it.hasNext()) {
                int id = it.next();

                seg.add((short) id);
            }

            return seg;
        }

        /**
         * @param val Value.
         */
        private static int wordIndex(int val) {
            return val >> WORD_SHIFT_BITS;
        }

        /** TODO needs refactoring */
        private static short nextSetBit(short[] words, int fromIdx) {
            if (fromIdx < 0)
                return -1;

            int wordIdx = wordIndex(fromIdx);

            if (wordIdx >= words.length)
                return -1;

            int shift = fromIdx & (SHORT_BITS - 1);

            short word = (short)((words[wordIdx] & 0xFFFF) & (WORD_MASK << shift));

            while (true) {
                if (word != 0)
                    return (short) ((wordIdx * SHORT_BITS) + Integer.numberOfTrailingZeros(word & 0xFFFF));

                if (++wordIdx == words.length)
                    return -1;

                word = words[wordIdx];
            }
        }

        /** */
        private static short prevSetBit(short[] words, int fromIdx) {
            if (fromIdx < 0)
                return -1;

            int wordIdx = wordIndex(fromIdx);

            if (wordIdx >= words.length)
                return -1;

            int shift = SHORT_BITS - (fromIdx & (SHORT_BITS - 1)) - 1;

            short word = (short)((words[wordIdx] & 0xFFFF) & (WORD_MASK >> shift));

            while (true) {
                if (word != 0)
                    return (short) ((wordIdx * SHORT_BITS) + SHORT_BITS - 1 - (Integer.numberOfLeadingZeros(word & 0xFFFF) - SHORT_BITS));

                if (--wordIdx == -1)
                    return -1;

                word = words[wordIdx];
            }
        }

        /** {@inheritDoc} */
        @Override public boolean contains(short val) {
            int wordIdx = wordIndex(val);

            if (wordIdx >= used())
                return false;

            short wordBit = (short) (val - wordIdx * SHORT_BITS); // TODO FIXME

            int mask = 1 << wordBit;

            return (words[wordIdx] & mask) == mask;
        }

        @Override public short[] data() {
            return words;
        }

        @Override public int size() {
            return count;
        }

        @Override public int minSize() {
            return THRESHOLD;
        }

        @Override public int maxSize() {
            return THRESHOLD2;
        }

        @Override public short first() {
            return nextSetBit(words, 0);
        }

        @Override public short last() {
            return prevSetBit(words, (used() << WORD_SHIFT_BITS) - 1);
        }

        @Override public Iterator iterator() {
            return new BitSetIterator();
        }

        @Override public Iterator reverseIterator() {
            return new ReverseBitSetIterator();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            StringBuilder b = new StringBuilder("[");

            Iterator it = iterator();

            while(it.hasNext()) {
                b.append(it.next());

                if (it.hasNext()) b.append(", ");
            }

            b.append("]");

            return b.toString();
        }

        /** */
        private class BitSetIterator implements Iterator {
            /** */
            private int next;

            /** */
            private int cur;

            /** */
            public BitSetIterator() {
                this.next = nextSetBit(words, 0);
            }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return next != -1;
            }

            /** {@inheritDoc} */
            @Override public int next() {
                cur = next;

                next = nextSetBit(words, next + 1);

                return cur;
            }

            /** {@inheritDoc} */
            @Override public void remove() {
                BitSetSegment.this.remove((short) cur);
            }

            /** {@inheritDoc} */
            @Override public void skipTo(int val) {
                next = nextSetBit(words, val);
            }
        }

        /** */
        private class ReverseBitSetIterator implements Iterator {
            /** */
            private int next;

            /** */
            private int cur;

            /** */
            public ReverseBitSetIterator() {
                this.next = prevSetBit(words, (words.length << WORD_SHIFT_BITS) - 1);
            }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return next != -1;
            }

            /** {@inheritDoc} */
            @Override public int next() {
                cur = next;

                next = prevSetBit(words, next - 1);

                return cur;
            }

            /** {@inheritDoc} */
            @Override public void remove() {
                BitSetSegment.this.remove((short) cur);
            }

            /** {@inheritDoc} */
            @Override public void skipTo(int val) {
                next = prevSetBit(words, val);
            }
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BitSetSegment that = (BitSetSegment) o;

            return Arrays.equals(words, that.words);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return Arrays.hashCode(words);
        }
    }

    /** */
    static class ArraySegment implements Segment {
        /** */
        private short[] data;

        /** */
        private short used;

        /** */
        public ArraySegment() {
            this(0);
        }

        /** */
        public ArraySegment(int size) {
            this.data = new short[size];
        }

        /** */
        public ArraySegment(short[] data, short used) {
            this.data = data;

            // Segment length must be power of two.
            assert data.length == 0 || U.isPow2(data.length);

            this.used = used;
        }

        /** {@inheritDoc} */
        @Override public short[] data() {
            return data;
        }

        /**
         * @return Used items.
         */
        public int used() {
            return used;
        }

        /** */
        public boolean add(short val) throws ConversionException {
            assert val < SEGMENT_SIZE: val;

            if (used == 0) {
                data = new short[1];

                data[0] = val;

                used++;

                return true;
            }

            int freeIdx = used();

            assert 0 <= freeIdx;

            int idx = Arrays.binarySearch(data, 0, freeIdx, val);

            if (idx >= 0)
                return false; // Already exists.

            if (freeIdx == THRESHOLD) { // Convert to bit set on reaching threshold.
                Segment converted = convertToBitSetSegment(val);

                throw new ConversionException(converted);
            }

            int pos = -(idx + 1);

            // Insert a segment.

            if (freeIdx >= data.length) {
                int newSize = Math.min(data.length * 2, THRESHOLD);

                data = Arrays.copyOf(data, newSize);
            }

            System.arraycopy(data, pos, data, pos + 1, freeIdx - pos);

            data[pos] = val;

            used++;

            return true;
        }

        /** {@inheritDoc} */
        public boolean remove(short base) throws ConversionException {
            assert base < SEGMENT_SIZE : base;

            int idx = Arrays.binarySearch(data, 0, used, base);

            return idx >= 0 && remove0(idx);
        }

        /** */
        private boolean remove0(int idx) throws ConversionException {
            if (idx < 0)
                return false;

            short[] tmp = data;

            int moved = used - idx - 1;

            if (moved > 0)
                System.arraycopy(data, idx + 1, tmp, idx, moved);

            used--;

            data[used] = 0;

//            if (used == (data.length >> 1))
//                data = Arrays.copyOf(data, used);

            return true;
        }

        /** {@inheritDoc} */
        public boolean contains(short v) {
            return Arrays.binarySearch(data, 0, used, v) >= 0;
        }

        @Override public int size() {
            return used();
        }

        @Override public int minSize() {
            return 0;
        }

        @Override public int maxSize() {
            return THRESHOLD;
        }

        @Override public short first() {
            return size() == 0 ? -1 : data[0]; // TODO FIXME size issue.
        }

        @Override public short last() {
            return size() == 0 ? -1 : data[used() - 1];
        }

        /** {@inheritDoc} */
        public Iterator iterator() {
            return new ArrayIterator();
        }

        /** {@inheritDoc} */
        @Override public Iterator reverseIterator() {
            return new ReverseArrayIterator();
        }

        /** {@inheritDoc}
         * @param val*/
        private Segment convertToBitSetSegment(short val) throws ConversionException {
            Segment seg = new BitSetSegment(val);

            Iterator it = iterator();

            while(it.hasNext())
                seg.add((short) it.next());

            seg.add(val);

            return seg;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            StringBuilder b = new StringBuilder("[");

            Iterator it = iterator();

            while(it.hasNext()) {
                b.append(it.next());

                if (it.hasNext()) b.append(", ");
            }

            b.append("]");

            return b.toString();
        }

        /** */
        class ArrayIterator implements Iterator {
            /** Word index. */
            private int cur;

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return cur < used;
            }

            /** {@inheritDoc} */
            @Override public int next() {
                return data[cur++];
            }

            /** {@inheritDoc} */
            @Override public void remove() {
                ArraySegment.this.remove0((short) (--cur));
            }

            @Override public void skipTo(int val) {
                int idx = Arrays.binarySearch(data, 0, used(), (short) val);

                cur = idx >= 0 ? idx : -(idx + 1);
            }
        }

        /** */
        class ReverseArrayIterator implements Iterator {
            /** Word index. */
            private int cur = used - 1;

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return cur >= 0;
            }

            /** {@inheritDoc} */
            @Override public int next() {
                return data[cur--];
            }

            /** {@inheritDoc} */
            @Override public void remove() throws ConversionException {
                ArraySegment.this.remove0((short) (cur + 1));
            }

            /** {@inheritDoc} */
            @Override public void skipTo(int val) {
                int idx = Arrays.binarySearch(data, 0, used(), (short) val);

                cur = idx >= 0 ? idx : -(idx + 1) - 1;
            }
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ArraySegment that = (ArraySegment) o;

            if (used != that.used)
                return false;

            for (int i = 0; i < used; i++)
                if (data[i] != that.data[i])
                    return false;

            return true;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int res = 1;

            for (int i = 0; i < used; i++)
                res = 31 * res + data[i];

            return res;
        }
    }

    /** */
    static class FlippedArraySegment extends ArraySegment {
        /**
         * Default constructor.
         */
        public FlippedArraySegment() {
            this(0);
        }

        /**
         * @param size Size.
         */
        public FlippedArraySegment(int size) {
            super(size);
        }

        public FlippedArraySegment(short[] buf, short used) {
            super(buf, used);
        }

        /** {@inheritDoc} */
        @Override public boolean add(short val) throws ConversionException {
            return super.remove(val);
        }

        /** {@inheritDoc} */
        @Override public boolean remove(short base) throws ConversionException {
            try {
                return super.add(base);
            } catch (ConversionException e) {
                // Converted value will be included in converted set, have to remove it before proceeding.
                e.segment.remove(base);

                throw e;
            }
        }

        /** {@inheritDoc} */
        @Override public boolean contains(short v) {
            return !super.contains(v);
        }

        /** {@inheritDoc} */
        @Override public int size() {
            return SEGMENT_SIZE - super.size();
        }

        /** {@inheritDoc} */
        @Override public int minSize() {
            return THRESHOLD2;
        }

        /** {@inheritDoc} */
        @Override public int maxSize() {
            return SEGMENT_SIZE;
        }

        /** {@inheritDoc} */
        @Override public short first() {
            Iterator iter = iterator();

            if (!iter.hasNext())
                return -1;

            return (short) iter.next();
        }

        /** {@inheritDoc} */
        @Override public short last() {
            Iterator it = reverseIterator();

            return (short) (it.hasNext() ? it.next(): -1);
        }

        /** {@inheritDoc} */
        @Override public Iterator iterator() {
            return new FlippedArrayIterator();
        }

        /** {@inheritDoc} */
        @Override public Iterator reverseIterator() {
            return new FlippedReverseArrayIterator();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            StringBuilder b = new StringBuilder("[");

            Iterator it = iterator();

            while(it.hasNext()) {
                b.append(it.next());

                if (it.hasNext()) b.append(", ");
            }

            b.append("]");

            return b.toString();
        }

        /** */
        private class FlippedArrayIterator extends ArrayIterator {
            /** */
            private int skipVal = -1;

            /** */
            private int next = 0;

            /** */
            private int cur;

            /** */
            FlippedArrayIterator() {
                advance();
            }

            /** */
            private void advance() {
                if (skipVal == -1)
                    if (super.hasNext())
                        skipVal = super.next();

                while(skipVal == next && next < SEGMENT_SIZE) {
                    if (super.hasNext())
                        skipVal = super.next();

                    next++;
                }
            }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return next < SEGMENT_SIZE;
            }

            /** {@inheritDoc} */
            @Override public int next() {
                cur = next++;

                advance();

                return cur;
            }

            /** {@inheritDoc} */
            @Override public void remove() {
                FlippedArraySegment.this.remove((short) cur);
            }

            /** {@inheritDoc} */
            @Override public void skipTo(int val) {
                assert val >= 0;

                next = val;

                advance();
            }
        }

        /** */
        private class FlippedReverseArrayIterator extends ReverseArrayIterator {
            /** */
            private int next = SEGMENT_SIZE - 1;

            /** */
            private int cur;

            /** */
            FlippedReverseArrayIterator() {
                advance();
            }

            /** */
            private void advance() {
                while(super.hasNext() && super.next() == next && next-- >= 0);
            }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return next >= 0;
            }

            /** {@inheritDoc} */
            @Override public int next() {
                cur = next--;

                advance();

                return cur;
            }

            /** {@inheritDoc} */
            @Override public void remove() {
                FlippedArraySegment.this.remove((short) cur);
            }

            /** {@inheritDoc} */
            @Override public void skipTo(int val) {
                assert val >= 0;

                next = val;

                advance();
            }
        }
    }

    /** */
    static class ConversionException extends RuntimeException {
        private Segment segment;

        public ConversionException(Segment segment) {
            this.segment = segment;
        }

        /**
         * @return Segment.
         */
        public Segment segment() {
            return segment;
        }

        /**
         * @param segment New segment.
         */
        public void segment(Segment segment) {
            this.segment = segment;
        }
    }

    private int type(Segment seg) {
        return seg instanceof FlippedArraySegment ? 2 : seg instanceof BitSetSegment ? 1 : 0;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(0); // Version.

        out.writeByte(type(indices));

        writeSegment(out, indices);

        Iterator it = indices.iterator();

        BitSet t1 = new BitSet();
        BitSet t2 = new BitSet();

        int i = 0;

        while(it.hasNext()) {
            short id = (short) it.next();

            Segment segment = segments.get(id);

            int type = type(segment);

            if (type == 0)
                t1.set(i);
            else if (type == 1)
                t2.set(i);

            i++;
        }

        out.writeObject(t1);
        out.writeObject(t2);

        it = indices.iterator();

        while(it.hasNext()) {
            short id = (short) it.next();

            writeSegment(out, segments.get(id));
        }
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        in.readByte();

        byte type = in.readByte();

        indices = readSegment(in, type);

        BitSet t1 = (BitSet)in.readObject();
        BitSet t2 = (BitSet)in.readObject();

        Iterator it = indices.iterator();


        int i = 0;

        while(it.hasNext()) {
            short id = (short) it.next();

            boolean bit = t1.get(i);

            type = (byte)(bit ? 0 : t2.get(i) ? 1 : 2);

            Segment seg = readSegment(in, type);

            size += seg.size();

            segments.put(id, seg);

            i++;
        }
    }

    private void writeSegment(ObjectOutput out, Segment segment) throws IOException {
        int used = segment.used();

        out.writeShort(used);

        for(int i = 0; i < used; i++)
            out.writeShort(segment.data()[i]);
    }

    private Segment readSegment(ObjectInput in, byte type) throws IOException {
        short used = in.readShort();

        short[] buf = new short[nextPowerOfTwo(used)];

        for (int i = 0; i < used; i++)
            buf[i] = in.readShort();

        return type == 0 ? new ArraySegment(buf, used) : type == 1 ? new BitSetSegment(buf) :
            new FlippedArraySegment(buf, used);
    }

    /**
     * Returns next upper power of two or same value.
     * @param i value.
     * @return Next power of two.
     */
    private static int nextPowerOfTwo(int i) {
        return i <= 1 ? i : U.ceilPow2(i);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GridIntSet that = (GridIntSet) o;

        if (size != that.size) return false;

        if (!indices.equals(that.indices)) return false;

        return segments.equals(that.segments);
    }

    @Override public int hashCode() {
        int res = indices.hashCode();

        res = 31 * res + segments.hashCode();

        res = 31 * res + size;

        return res;
    }
}