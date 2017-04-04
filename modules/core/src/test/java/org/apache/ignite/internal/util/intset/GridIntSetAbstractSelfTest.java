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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.ignite.internal.util.GridRandom;
import org.apache.ignite.lang.IgniteOutClosure;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jsr166.ThreadLocalRandom8;

/**
 * Base class for {@link GridIntSet} tests.
 */
public abstract class GridIntSetAbstractSelfTest extends GridCommonAbstractTest {
    /** Max words. */
    private static final int MAX_WORDS = GridIntSet.SEGMENT_SIZE / Short.SIZE;

    protected static final int MAX_VALUES = GridIntSet.SEGMENT_SIZE * GridIntSet.SEGMENT_SIZE;

    private GridRandom gridRandom = new GridRandom();

    private long seed;

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        setSeed(ThreadLocalRandom8.current().nextLong());
    }

    /** */
    private void setSeed(long seed) {
        this.seed = seed;

        gridRandom.setSeed(seed);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        // Log used seed for easier debugging.
        log.info("Used seed: " + seed);
    }

    /** Creates actual set implementation for test. */
    protected abstract TestIntSet set();

    /** */
    public void testRemoveAddRemoveIntSet() {
        testRemoveAddRemoveRnd0(set());
    }

    /** */
    public void testRemoveFirstIntSet() {
        testRemoveFirst0(set());
    }

    /** */
    public void testRemoveFirstIterIntSet() {
        testRemoveFirstIter0(set());
    }

    /** */
    public void testRemoveLastIntSet() {
        testRemoveLast0(set());
    }

    /** */
    public void testRemoveLastIterIntSet() {
        testRemoveLastIter0(set());
    }

    /** */
    public void testSkipForward() {
        testSkipForward0(set());
    }

    /** */
    public void testSkipBackward() {
        testSkipBackward0(set());
    }

    /** */
    public void testSkipRemoveForward() {
        testSkipRemoveForward0(set());
    }

    /** */
    public void testSkipRemoveBackward() {
        testSkipRemoveBackward0(set());
    }

    /** */
    public void testIterators() {
        final TestIntSet set = set();

        testIterators0(new IgniteOutClosure<GridIntSet.Iterator>() {
            @Override
            public GridIntSet.Iterator apply() {
                return set.iterator();
            }
        }, new IgniteOutClosure<GridIntSet.Iterator>() {
            @Override
            public GridIntSet.Iterator apply() {
                return set.reverseIterator();
            }
        });
    }

    /** */
    public void testIteratorRemovalBothDirectionsWithConversion() {

    }

    /**
     * Tests array set.
     */
    private void testRemoveAddRemoveRnd0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        List<Integer> vals = new ArrayList<>();

        // Fetch values to list.
        GridIntSet.Iterator it = set.iterator();

        while(it.hasNext())
            assertTrue(vals.add(it.next()));

        assertEquals(size, vals.size());

        // Double check containment.
        for (Integer val : vals)
            assertTrue(set.contains(val));

        int opCnt = set.size() - set.minSize();

        // Randomize operations count.
        int cnt = gridRandom.nextInt(opCnt);

        // Define random subset of set elements.
        List<Integer> rndVals = randomSublist(vals, cnt);

        List<Integer> sortedVals = new ArrayList<>(rndVals);

        Collections.sort(sortedVals);

        for (Integer val : rndVals) {
            validateSize(set);

            assertTrue(set.contains(val));

            assertTrue(set.remove(val));

            validateSize(set);
        }

        assertEquals("Size", size - cnt, set.size());

        assertContainsNothing(set, rndVals);

        for (Integer v : rndVals) {
            validateSize(set);

            boolean val = set.add(v);

            assertTrue("Added: " + v, val);

            assertTrue(set.contains(v));

            validateSize(set);
        }

        assertContainsAll(set, rndVals);

        // Randomize removal order.
        Collections.shuffle(rndVals);

        assertEquals("After", size, set.size());

        for (Integer v : rndVals) {
            validateSize(set);

            boolean val = set.remove(v);

            assertTrue("Removed: " + v, val);

            validateSize(set);
        }

        assertContainsNothing(set, rndVals);

        assertEquals("Size", size - cnt, set.size());
    }

    /**
     * Tests removal from left size.
     */
    private void testRemoveFirst0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        int origSize = size;

        int rmvCnt = set.size() - set.minSize();

        int i = rmvCnt;

        while(i-- > 0) {
            validateSize(set);

            int val = set.first();

            assertTrue(set.remove(val));

            assertEquals(--size, set.size());

            validateSize(set);
        }

        assertEquals(origSize - rmvCnt, set.size());
    }

    /**
     * Tests removal from left size.
     */
    private void testRemoveFirstIter0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        int origSize = size;

        GridIntSet.Iterator iter = set.iterator();

        int rmvCnt = set.size() - set.minSize();

        int i = rmvCnt;

        while(iter.hasNext() && i-- > 0) {
            validateSize(set);

            iter.next();

            iter.remove();

            assertEquals(--size, set.size());

            validateSize(set);

            assertTrue(set.size() >= set.minSize());
        }

        assertEquals(origSize - rmvCnt, set.size());
    }
    /**
     * Tests removal from right side.
     */
    private void testRemoveLast0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        int origSize = size;

        int rmvCnt = set.size() - set.minSize();

        int i = rmvCnt;

        while(i-- > 0) {
            validateSize(set);

            int val = set.last();

            assertTrue(set.remove(val));

            assertEquals(--size, set.size());

            validateSize(set);
        }

        assertEquals(origSize - rmvCnt, set.size());
    }

    /**
     * Tests removal from right side.
     */
    private void testRemoveLastIter0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        int origSize = size;

        GridIntSet.Iterator iter = set.reverseIterator();

        int rmvCnt = set.size() - set.minSize();

        int i = rmvCnt;

        while(iter.hasNext() && i-- > 0) {
            validateSize(set);

            iter.next();

            iter.remove();

            assertEquals(--size, set.size());

            validateSize(set);
        }

        assertEquals(origSize - rmvCnt, set.size());
    }

    /**
     * Tests forward skips.
     */
    private void testSkipForward0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        List<Integer> tmp = new ArrayList<>(size);

        GridIntSet.Iterator iter = set.iterator();

        while(iter.hasNext())
            assertTrue(tmp.add(iter.next()));

        assertEquals(set.size(), tmp.size());

        int i = set.size() - set.minSize();

        Iterator<Integer> it = tmp.iterator();

        while(it.hasNext() && i-- > 0) {
            Integer val = it.next();

            iter.skipTo(val);

            assertTrue(iter.hasNext());

            assertEquals(val.intValue(), iter.next());
        }
    }

    /**
     * Tests backward skips with removals.
     */
    private void testSkipBackward0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        List<Integer> tmp = new ArrayList<>(size);

        GridIntSet.Iterator iter = set.reverseIterator();

        while(iter.hasNext())
            assertTrue(tmp.add(iter.next()));

        assertEquals(set.size(), tmp.size());

        int i = set.size() - set.minSize();

        Iterator<Integer> it = tmp.iterator();

        while(it.hasNext() && i-- > 0) {
            Integer val = it.next();

            iter.skipTo(val);

            assertTrue(iter.hasNext());

            assertEquals(val.intValue(), iter.next());
        }
    }

    /**
     * Tests removal from right side.
     */
    private void testSkipRemoveForward0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        List<Integer> tmp = new ArrayList<>(size);

        GridIntSet.Iterator iter = set.iterator();

        while(iter.hasNext())
            assertTrue(tmp.add(iter.next()));

        assertEquals(set.size(), tmp.size());

        int i = set.size() - set.minSize();

        int rmvCnt = i;

        Iterator<Integer> it = tmp.iterator();

        while(it.hasNext() && i-- > 0) {
            Integer val = it.next();

            iter.skipTo(val);

            assertTrue(iter.hasNext());

            assertEquals(val.intValue(), iter.next());

            iter.remove();
        }

        assertEquals(size - rmvCnt, set.size());
    }

    /**
     * Tests removal from right side.
     */
    private void testSkipRemoveBackward0(TestIntSet set) throws GridIntSet.ConversionException {
        int size = set.size();

        List<Integer> tmp = new ArrayList<>(size);

        GridIntSet.Iterator iter = set.iterator();

        while(iter.hasNext())
            assertTrue(tmp.add(iter.next()));

        assertEquals(set.size(), tmp.size());

        int i = set.size() - set.minSize();

        int rmvCnt = i;

        ListIterator<Integer> it = tmp.listIterator(tmp.size());

        iter = set.reverseIterator();

        while(it.hasPrevious() && i-- > 0) {
            Integer val = it.previous();

            iter.skipTo(val);

            assertTrue(iter.hasNext());

            assertEquals(val.intValue(), iter.next());

            iter.remove();
        }

        assertEquals(size - rmvCnt, set.size());
    }

    /** */
    private void testIterators0(IgniteOutClosure<GridIntSet.Iterator> fwdFactoryClo, IgniteOutClosure<GridIntSet.Iterator> revFactoryClo) {
        List<Integer> fwd = new ArrayList<>();

        GridIntSet.Iterator fwdIt = fwdFactoryClo.apply();

        while(fwdIt.hasNext())
            fwd.add(fwdIt.next());

        List<Integer> rev = new ArrayList<>();

        GridIntSet.Iterator revIt = revFactoryClo.apply();

        while(revIt.hasNext())
            rev.add(revIt.next());

        Collections.reverse(rev);

        assertEquals(rev.toString(), fwd, rev);
    }

    /**
     * @param set Segment.
     */
    private void validateSize(TestIntSet set) {
        if (set instanceof TestIntSetSegImpl) {
            TestIntSetSegImpl tmp = (TestIntSetSegImpl) set;

            GridIntSet.Segment segment = tmp.segment();

            assertTrue(String.valueOf(segment.size()), segment.minSize() <= segment.size() && segment.size() <= segment.maxSize());

            assertTrue(String.valueOf(segment.data().length), segment.data().length <= MAX_WORDS);
        }
    }

    /**
     * @param set Segment.
     * @param vals Values.
     */
    public void assertContainsNothing(TestIntSet set, Collection<Integer> vals) {
        for (Integer v : vals) {
            boolean val = set.contains(v);

            assertFalse("Contains: " + v, val);
        }
    }

    /**
     * @param set Set.
     * @param vals Test values.
     */
    public void assertContainsAll(TestIntSet set, Collection<Integer> vals) {
        for (Integer v : vals) {
            boolean val = set.contains(v);

            assertTrue("Contains: " + v, val);
        }
    }

    /**
     * @param set Set.
     * @param size Resulting size.
     * @param max Max.
     */
    protected TestIntSet rndFill(TestIntSet set, int size, int max) {
        assert size <= max;

        boolean negate = size > max / 2;

        int tmpCnt = negate ? max - size : size;

        SortedSet<Integer> buf = new TreeSet<>();

        while(buf.size() != tmpCnt) {
            int rnd = gridRandom.nextInt(max);

            buf.add(rnd);
        }

        List<Short> buf1 = new ArrayList<>();

        if (negate) {
            Iterator<Integer> it = buf.iterator();

            int i = 0;

            int c = 0;

            while (it.hasNext()) {
                int id = it.next();

                for (; i < id; i++)
                    set.add((short) i);

                i = id + 1;
            }

            while(i < max)
                set.add((short) i++);
        } else
            for (Integer val : buf)
                set.add(val);

        assertEquals(size, set.size());

        return set;
    }

    /**
     * Fills set with random values until full capacity.
     *
     * @param set Segment.
     */
    protected TestIntSet rndErase(TestIntSet set, int size, int max) {
        assert size <= max;

        boolean negate  = size > max / 2;

        int tmpCnt = negate ? max - size : size;

        SortedSet<Integer> buf = new TreeSet<>();

        while(buf.size() != tmpCnt) {
            int rnd = gridRandom.nextInt(max);

            buf.add(rnd);
        }

        if (negate) {
            Iterator<Integer> it = buf.iterator();

            int i = 0;

            while (it.hasNext()) {
                int id = it.next();

                for (; i < id; i++)
                    set.remove((short) i);

                i = id + 1;
            }

            while(i < max)
                set.remove((short) i++);
        } else
            for (Integer val : buf)
                set.add(val);

        return set;
    }

    /** Returns random element from list. */
    private <T> T random(List<T> l) {
        return l.get(gridRandom.nextInt(l.size()));
    }

    /**
     * Return random sublist from a list.
     *
     * @param list List.
     * @param len Length of sublist.
     */
    private List<Integer> randomSublist(List<Integer> list, int len) {
        assert len <= list.size();

        List<Integer> src = new ArrayList<>(list);

        for (int i = 0; i < list.size(); i++) {
            int idx1 = gridRandom.nextInt(list.size());

            int idx2 = gridRandom.nextInt(list.size());

            Collections.swap(list, idx1, idx2);
        }

        List<Integer> ret = src.subList(0, len);

        assertEquals(len, ret.size());

        return ret;
    }

    interface TestIntSet {
        int size();

        boolean remove(int val);

        boolean contains(int val);

        boolean add(int val);

        int first();

        GridIntSet.Iterator iterator();

        int last();

        GridIntSet.Iterator reverseIterator();

        int minSize();

        int maxSize();
    }

    static class TestIntSetSegImpl implements TestIntSet {
        private GridIntSet.Segment seg;

        public TestIntSetSegImpl(GridIntSet.Segment seg) {
            this.seg = seg;
        }

        @Override public int size() {
            return seg.size();
        }

        @Override public int first() {
            return seg.first();
        }

        @Override public boolean remove(int val) {
            return seg.remove((short) val);
        }

        @Override public boolean contains(int val) {
            return seg.contains((short) val);
        }

        @Override public boolean add(int val) {
            return seg.add((short) val);
        }

        @Override public GridIntSet.Iterator iterator() {
            return seg.iterator();
        }

        @Override public int last() {
            return seg.last();
        }

        @Override public GridIntSet.Iterator reverseIterator() {
            return seg.reverseIterator();
        }

        GridIntSet.Segment segment() {
            return seg;
        }

        @Override public int minSize() {
            return seg.minSize();
        }

        @Override public int maxSize() {
            return seg.maxSize();
        }
    }

    static class TestIntSetImpl implements TestIntSet {
        private final GridIntSet set;

        public TestIntSetImpl() {
            set = new GridIntSet();
        }

        @Override public int size() {
            return set.size();
        }

        @Override public int first() {
            return set.first();
        }

        @Override public boolean remove(int val) {
            return set.remove(val);
        }

        @Override public boolean contains(int val) {
            return set.contains(val);
        }

        @Override public boolean add(int val) {
            return set.add(val);
        }

        @Override public GridIntSet.Iterator iterator() {
            return set.iterator();
        }

        @Override public int last() {
            return set.last();
        }

        @Override public GridIntSet.Iterator reverseIterator() {
            return set.reverseIterator();
        }

        @Override public int minSize() {
            return 0;
        }

        @Override public int maxSize() {
            return MAX_VALUES;
        }
    }

    static class TestIntSetArrayListImpl implements TestIntSet {
        private final ArrayList<Integer> list;

        public TestIntSetArrayListImpl() {
            list = new ArrayList<>();
        }

        @Override public int size() {
            return list.size();
        }

        @Override public int first() {
            return list.get(0);
        }

        @Override public boolean remove(int val) {
            return list.remove((Integer) val);
        }

        @Override public boolean contains(int val) {
            return list.contains(val);
        }

        @Override public boolean add(int val) {
            return list.add(val);
        }

        @Override public int last() {
            return list.get(list.size() - 1);
        }

        @Override public GridIntSet.Iterator iterator() {
            final ListIterator<Integer> it = list.listIterator();

            return new GridIntSet.Iterator() {
                @Override public boolean hasNext() {
                    return it.hasNext();
                }

                @Override public int next() {
                    return it.next();
                }

                @Override public void remove() {
                    it.remove();
                }

                @Override public void skipTo(int val) {

                }
            };
        }

        @Override public GridIntSet.Iterator reverseIterator() {
            final ListIterator<Integer> it = list.listIterator(list.size());

            return new GridIntSet.Iterator() {
                @Override public boolean hasNext() {
                    return it.hasPrevious();
                }

                @Override public int next() {
                    return it.previous();
                }

                @Override public void remove() {
                    it.remove();
                }

                @Override public void skipTo(int val) {
                }
            };
        }

        @Override public int minSize() {
            return 0;
        }

        @Override public int maxSize() {
            return MAX_VALUES;
        }
    }

    static class TestIntSetImpl3 implements TestIntSet {
        private final HashSet<Integer> list;

        public TestIntSetImpl3() {
            list = new HashSet<>();
        }

        @Override public int size() {
            return list.size();
        }

        @Override public int first() {
            return list.iterator().next();
        }

        @Override public boolean remove(int val) {
            return list.remove((Integer) val);
        }

        @Override public boolean contains(int val) {
            return list.contains(val);
        }

        @Override public boolean add(int val) {
            return list.add(val);
        }

        @Override public int last() {
            return list.iterator().next();
        }

        @Override public GridIntSet.Iterator iterator() {
            final Iterator<Integer> it = list.iterator();

            return new GridIntSet.Iterator() {
                @Override public boolean hasNext() {
                    return it.hasNext();
                }

                @Override public int next() {
                    return it.next();
                }

                @Override public void remove() {
                    it.remove();
                }

                @Override public void skipTo(int val) {
                }
            };
        }

        @Override public GridIntSet.Iterator reverseIterator() {
            return iterator();
        }

        @Override public int minSize() {
            return 0;
        }

        @Override public int maxSize() {
            return MAX_VALUES;
        }
    }
}
