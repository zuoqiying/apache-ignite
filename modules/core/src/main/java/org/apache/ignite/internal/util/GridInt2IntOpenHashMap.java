/*
 * Copyright (C) 2002-2016 Sebastiano Vigna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.util;


import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;

/**
 * A type-specific hash map with a fast, small-footprint implementation.
 *
 * <P>
 * Instances of this class use a hash table to represent a map. The table is
 * filled up to a specified <em>load factor</em>, and then doubled in size to
 * accommodate new entries. If the table is emptied below <em>one fourth</em> of
 * the load factor, it is halved in size. However, halving is not performed when
 * deleting entries from an iterator, as it would interfere with the iteration
 * process.
 *
 * <p>
 * Note that {@link #clear()} does not modify the hash table size. Rather, a
 * family of {@linkplain #trim() trimming methods} lets you control the size of
 * the table; this is particularly useful if you reuse instances of this class.
 *
 */

public class GridInt2IntOpenHashMap
    implements java.io.Serializable, Cloneable, Int2IntMap {
    /** 2<sup>32</sup> &middot; &phi;, &phi; = (&#x221A;5 &minus; 1)/2. */
    private static final int INT_PHI = 0x9E3779B9;
    /**
     * The default return value for <code>get()</code>, <code>put()</code> and
     * <code>remove()</code>.
     */

    protected int defRetValue;

    /** Returns the least power of two smaller than or equal to 2<sup>30</sup> and larger than or equal to <code>Math.ceil( expected / f )</code>.
     *
     * @param expected the expected number of elements in a hash table.
     * @param f the load factor.
     * @return the minimum possible size for a backing array.
     * @throws IllegalArgumentException if the necessary size is larger than 2<sup>30</sup>.
     */
    public static int arraySize( final int expected, final float f ) {
        final long s = Math.max( 2, nextPowerOfTwo( (long)Math.ceil( expected / f ) ) );
        if ( s > (1 << 30) ) throw new IllegalArgumentException( "Too large (" + expected + " expected elements with load factor " + f + ")" );
        return (int)s;
    }

    /** Return the least power of two greater than or equal to the specified value.
     *
     * <p>Note that this function will return 1 when the argument is 0.
     *
     * @param x a long integer smaller than or equal to 2<sup>62</sup>.
     * @return the least power of two greater than or equal to the specified value.
     */
    public static long nextPowerOfTwo( long x ) {
        if ( x == 0 ) return 1;
        x--;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return ( x | x >> 32 ) + 1;
    }

    /** Return the least power of two greater than or equal to the specified value.
     *
     * <p>Note that this function will return 1 when the argument is 0.
     *
     * @param x an integer smaller than or equal to 2<sup>30</sup>.
     * @return the least power of two greater than or equal to the specified value.
     */
    public static int nextPowerOfTwo( int x ) {
        if ( x == 0 ) return 1;
        x--;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        return ( x | x >> 16 ) + 1;
    }

    /** Quickly mixes the bits of an integer.
     *
     * @param x an integer.
     * @return a hash value obtained by mixing the bits of {@code x}.
     */
    public final static int mix( final int x ) {
        final int h = x * INT_PHI;
        return h ^ (h >>> 16);
    }

    /** Returns the maximum number of entries that can be filled before rehashing.
     *
     * @param n the size of the backing array.
     * @param f the load factor.
     * @return the maximum number of entries before rehashing.
     */
    public static int maxFill( final int n, final float f ) {
		/* We must guarantee that there is always at least
		 * one free entry (even with pathological load factors). */
        return Math.min( (int)Math.ceil( n * f ), n - 1 );
    }

    private static final long serialVersionUID = 0L;
    private static final boolean ASSERTS = false;

    /** The array of keys. */
    protected transient int[] key;

    /** The array of values. */
    protected transient int[] value;

    /** The mask for wrapping a position counter. */
    protected transient int mask;

    /** Whether this set contains the key zero. */
    protected transient boolean containsNullKey;
    /** The current table size. */
    protected transient int n;

    /**
     * Threshold after which we rehash. It must be the table size times
     * {@link #f}.
     */
    protected transient int maxFill;

    /** Number of entries in the set (including the key zero, if present). */
    protected int size;

    /** The acceptable load factor. */
    protected final float f;
    /** Cached set of entries. */
    protected transient MapEntrySet entries;

    /** Cached set of keys. */
    protected transient IntSet keys;

    /** Cached collection of values. */
    protected transient IntCollection values;
    /**
     * Creates a new hash map.
     *
     * <p>
     * The actual table size will be the least power of two greater than
     * <code>expected</code>/<code>f</code>.
     *
     * @param expected
     *            the expected number of elements in the hash set.
     * @param f
     *            the load factor.
     */

    public GridInt2IntOpenHashMap(final int expected, final float f) {

        if (f <= 0 || f > 1)
            throw new IllegalArgumentException(
                "Load factor must be greater than 0 and smaller than or equal to 1");
        if (expected < 0)
            throw new IllegalArgumentException(
                "The expected number of elements must be nonnegative");

        this.f = f;

        n = arraySize(expected, f);
        mask = n - 1;
        maxFill = maxFill(n, f);
        key = new int[n + 1];
        value = new int[n + 1];

    }
    /**
     * Creates a new hash map with DEFAULT_LOAD_FACTOR as load
     * factor.
     *
     * @param expected
     *            the expected number of elements in the hash map.
     */

    public GridInt2IntOpenHashMap(final int expected) {
        this(expected, .75f);
    }
    /**
     * Creates a new hash map with initial expected
     * DEFAULT_INITIAL_SIZE entries and
     * DEFAULT_LOAD_FACTOR as load factor.
     */

    public GridInt2IntOpenHashMap() {
        this(16, .75f);
    }
    /**
     * Creates a new hash map copying a given one.
     *
     * @param m
     *            a {@link Map} to be copied into the new hash map.
     * @param f
     *            the load factor.
     */

    public GridInt2IntOpenHashMap(
        final Map<? extends Integer, ? extends Integer> m, final float f) {
        this(m.size(), f);
        putAll(m);
    }
    /**
     * Creates a new hash map with DEFAULT_LOAD_FACTOR as load
     * factor copying a given one.
     *
     * @param m
     *            a {@link Map} to be copied into the new hash map.
     */

    public GridInt2IntOpenHashMap(final Map<? extends Integer, ? extends Integer> m) {
        this(m, .75f);
    }

    /**
     * Creates a new hash map using the elements of two parallel arrays.
     *
     * @param k
     *            the array of keys of the new hash map.
     * @param v
     *            the array of corresponding values in the new hash map.
     * @param f
     *            the load factor.
     * @throws IllegalArgumentException
     *             if <code>k</code> and <code>v</code> have different lengths.
     */

    public GridInt2IntOpenHashMap(final int[] k, final int[] v, final float f) {
        this(k.length, f);
        if (k.length != v.length)
            throw new IllegalArgumentException(
                "The key array and the value array have different lengths ("
                    + k.length + " and " + v.length + ")");
        for (int i = 0; i < k.length; i++)
            this.put(k[i], v[i]);
    }
    /**
     * Creates a new hash map with DEFAULT_LOAD_FACTOR as load
     * factor using the elements of two parallel arrays.
     *
     * @param k
     *            the array of keys of the new hash map.
     * @param v
     *            the array of corresponding values in the new hash map.
     * @throws IllegalArgumentException
     *             if <code>k</code> and <code>v</code> have different lengths.
     */

    public GridInt2IntOpenHashMap(final int[] k, final int[] v) {
        this(k, v, .75f);
    }
    private int realSize() {
        return containsNullKey ? size - 1 : size;
    }

    private void ensureCapacity(final int capacity) {
        final int needed = arraySize(capacity, f);
        if (needed > n)
            rehash(needed);
    }

    private void tryCapacity(final long capacity) {
        final int needed = (int) Math.min(
            1 << 30,
            Math.max(2, nextPowerOfTwo((long) Math.ceil(capacity
                / f))));
        if (needed > n)
            rehash(needed);
    }

    private int removeEntry(final int pos) {
        final int oldValue = value[pos];

        size--;

        shiftKeys(pos);
        if (size < maxFill / 4 && n > 16)
            rehash(n / 2);
        return oldValue;
    }

    private int removeNullEntry() {
        containsNullKey = false;

        final int oldValue = value[n];

        size--;

        if (size < maxFill / 4 && n > 16)
            rehash(n / 2);
        return oldValue;
    }

    /** {@inheritDoc} */
    public void putAll(Map<? extends Integer, ? extends Integer> m) {
        if (f <= .5)
            ensureCapacity(m.size()); // The resulting map will be sized for
            // m.size() elements
        else
            tryCapacity(size() + m.size()); // The resulting map will be
        // tentatively sized for size() +
        // m.size() elements
        int n = m.size();
        final Iterator<? extends Map.Entry<? extends Integer, ? extends Integer>> i = m
            .entrySet().iterator();

        if (m instanceof Int2IntMap) {
            Int2IntMap.Entry e;
            while (n-- != 0) {
                e = (Int2IntMap.Entry)i.next();
                put(e.getIntKey(), e.getIntValue());
            }
        }
        else {
            Map.Entry<? extends Integer, ? extends Integer> e;
            while (n-- != 0) {
                e = i.next();
                put(e.getKey(), e.getValue());
            }
        }
    }

    private int insert(final int k, final int v) {
        int pos;

        if (((k) == (0))) {
            if (containsNullKey)
                return n;
            containsNullKey = true;
            pos = n;
        } else {
            int curr;
            final int[] key = this.key;

            // The starting point.
            if (!((curr = key[pos = (mix((k)))
                & mask]) == (0))) {
                if (((curr) == (k)))
                    return pos;
                while (!((curr = key[pos = (pos + 1) & mask]) == (0)))
                    if (((curr) == (k)))
                        return pos;
            }
        }

        key[pos] = k;
        value[pos] = v;
        if (size++ >= maxFill)
            rehash(arraySize(size + 1, f));
        if (ASSERTS)
            checkTable();
        return -1;
    }

    public int put(final int k, final int v) {
        final int pos = insert(k, v);
        if (pos < 0)
            return defRetValue;
        final int oldValue = value[pos];
        value[pos] = v;
        return oldValue;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    public Integer put(final Integer ok, final Integer ov) {
        final int v = ((ov).intValue());

        final int pos = insert(((ok).intValue()), v);
        if (pos < 0)
            return (null);
        final int oldValue = value[pos];
        value[pos] = v;
        return (Integer.valueOf(oldValue));
    }

    private int addToValue(final int pos, final int incr) {
        final int oldValue = value[pos];

        value[pos] = oldValue + incr;

        return oldValue;
    }

    /**
     * Adds an increment to value currently associated with a key.
     *
     * <P>
     * Note that this method respects the {@linkplain #defaultReturnValue()
     * default return value} semantics: when called with a key that does not
     * currently appears in the map, the key will be associated with the default
     * return value plus the given increment.
     *
     * @param k
     *            the key.
     * @param incr
     *            the increment.
     * @return the old value, or the {@linkplain #defaultReturnValue() default
     *         return value} if no value was present for the given key.
     */
    public int addTo(final int k, final int incr) {
        int pos;

        if (((k) == (0))) {
            if (containsNullKey)
                return addToValue(n, incr);
            pos = n;
            containsNullKey = true;
        } else {
            int curr;
            final int[] key = this.key;

            // The starting point.
            if (!((curr = key[pos = (mix((k)))
                & mask]) == (0))) {
                if (((curr) == (k)))
                    return addToValue(pos, incr);
                while (!((curr = key[pos = (pos + 1) & mask]) == (0)))
                    if (((curr) == (k)))
                        return addToValue(pos, incr);
            }
        }

        key[pos] = k;

        value[pos] = defRetValue + incr;
        if (size++ >= maxFill)
            rehash(arraySize(size + 1, f));
        if (ASSERTS)
            checkTable();
        return defRetValue;
    }

    /**
     * Shifts left entries with the specified hash code, starting at the
     * specified position, and empties the resulting free entry.
     *
     * @param pos
     *            a starting position.
     */
    protected final void shiftKeys(int pos) {
        // Shift entries with the same hash.
        int last, slot;
        int curr;
        final int[] key = this.key;

        for (;;) {
            pos = ((last = pos) + 1) & mask;

            for (;;) {
                if (((curr = key[pos]) == (0))) {
                    key[last] = (0);

                    return;
                }
                slot = (mix((curr))) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot
                    && slot > pos)
                    break;
                pos = (pos + 1) & mask;
            }

            key[last] = curr;
            value[last] = value[pos];

        }
    }

    public int remove(final int k) {
        if (((k) == (0))) {
            if (containsNullKey)
                return removeNullEntry();
            return defRetValue;
        }

        int curr;
        final int[] key = this.key;
        int pos;

        // The starting point.
        if (((curr = key[pos = (mix((k)))
            & mask]) == (0)))
            return defRetValue;
        if (((k) == (curr)))
            return removeEntry(pos);
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == (0)))
                return defRetValue;
            if (((k) == (curr)))
                return removeEntry(pos);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    public Integer remove(final Object ok) {
        final int k = ((((Integer) (ok)).intValue()));
        if (((k) == (0))) {
            if (containsNullKey)
                return (Integer.valueOf(removeNullEntry()));
            return (null);
        }

        int curr;
        final int[] key = this.key;
        int pos;

        // The starting point.
        if (((curr = key[pos = (mix((k)))
            & mask]) == (0)))
            return (null);
        if (((curr) == (k)))
            return (Integer.valueOf(removeEntry(pos)));
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == (0)))
                return (null);
            if (((curr) == (k)))
                return (Integer.valueOf(removeEntry(pos)));
        }
    }
    /** @deprecated Please use the corresponding type-specific method instead. */
    @Deprecated
    public Integer get(final Integer ok) {
        if (ok == null)
            return null;
        final int k = ((ok).intValue());
        if (((k) == (0)))
            return containsNullKey ? (Integer.valueOf(value[n])) : (null);

        int curr;
        final int[] key = this.key;
        int pos;

        // The starting point.
        if (((curr = key[pos = (mix((k)))
            & mask]) == (0)))
            return (null);
        if (((k) == (curr)))
            return (Integer.valueOf(value[pos]));

        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == (0)))
                return (null);
            if (((k) == (curr)))
                return (Integer.valueOf(value[pos]));
        }
    }

    public int get(final int k) {
        if (((k) == (0)))
            return containsNullKey ? value[n] : defRetValue;

        int curr;
        final int[] key = this.key;
        int pos;

        // The starting point.
        if (((curr = key[pos = (mix((k)))
            & mask]) == (0)))
            return defRetValue;
        if (((k) == (curr)))
            return value[pos];
        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == (0)))
                return defRetValue;
            if (((k) == (curr)))
                return value[pos];
        }
    }

    public boolean containsKey(final int k) {
        if (((k) == (0)))
            return containsNullKey;

        int curr;
        final int[] key = this.key;
        int pos;

        // The starting point.
        if (((curr = key[pos = (mix((k)))
            & mask]) == (0)))
            return false;
        if (((k) == (curr)))
            return true;
        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == (0)))
                return false;
            if (((k) == (curr)))
                return true;
        }
    }

    public boolean containsValue(final int v) {
        final int value[] = this.value;
        final int key[] = this.key;
        if (containsNullKey && ((value[n]) == (v)))
            return true;
        for (int i = n; i-- != 0;)
            if (!((key[i]) == (0)) && ((value[i]) == (v)))
                return true;
        return false;
    }

    /*
     * Removes all elements from this map.
     *
     * <P>To increase object reuse, this method does not change the table size.
     * If you want to reduce the table size, you must use {@link #trim()}.
     */
    public void clear() {
        if (size == 0)
            return;
        size = 0;
        containsNullKey = false;

        Arrays.fill(key, (0));

    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * A no-op for backward compatibility.
     *
     * @param growthFactor
     *            unused.
     * @deprecated Since <code>fastutil</code> 6.1.0, hash tables are doubled
     *             when they are too full.
     */
    @Deprecated
    public void growthFactor(int growthFactor) {
    }

    /**
     * Gets the growth factor (2).
     *
     * @return the growth factor of this set, which is fixed (2).
     * @see #growthFactor(int)
     * @deprecated Since <code>fastutil</code> 6.1.0, hash tables are doubled
     *             when they are too full.
     */
    @Deprecated
    public int growthFactor() {
        return 16;
    }

    public boolean containsValue(Object ov) {
        if (ov == null)
            return false;
        return containsValue(((((Integer) (ov)).intValue())));
    }

    public void defaultReturnValue(final int rv) {
        defRetValue = rv;
    }

    public int defaultReturnValue() {
        return defRetValue;
    }

    public boolean containsKey(final Object ok) {
        if (ok == null)
            return false;
        return containsKey(((((Integer) (ok)).intValue())));
    }

    /**
     * Delegates to the corresponding type-specific method, taking care of
     * returning <code>null</code> on a missing key.
     *
     * <P>
     * This method must check whether the provided key is in the map using
     * <code>containsKey()</code>. Thus, it probes the map <em>twice</em>.
     * Implementors of subclasses should override it with a more efficient
     * method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    public Integer get(final Object ok) {

        if (ok == null)
            return null;

        final int k = ((((Integer) (ok)).intValue()));
        return containsKey(k) ? (Integer.valueOf(get(k))) : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObjectSet<Map.Entry<Integer, Integer>> entrySet() {
        return (ObjectSet) int2IntEntrySet();
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Map))
            return false;

        Map<?, ?> m = (Map<?, ?>) o;
        if (m.size() != size())
            return false;
        return entrySet().containsAll(m.entrySet());
    }

    public String toString() {
        final StringBuilder s = new StringBuilder();
        final ObjectIterator<? extends Map.Entry<Integer, Integer>> i = entrySet()
            .iterator();
        int n = size();
        Entry e;
        boolean first = true;

        s.append("{");

        while (n-- != 0) {
            if (first)
                first = false;
            else
                s.append(", ");

            e = (Entry) i.next();

            s.append(String.valueOf(e.getIntKey()));
            s.append("=>");

            s.append(String.valueOf(e.getIntValue()));
        }

        s.append("}");
        return s.toString();
    }

    /**
     * The entry class for a hash map does not record key and value, but rather
     * the position in the hash table of the corresponding entry. This is
     * necessary so that calls to {@link java.util.Map.Entry#setValue(Object)}
     * are reflected in the map
     */

    final class MapEntry
        implements
        Int2IntMap.Entry,
        Map.Entry<Integer, Integer> {
        // The table index this entry refers to, or -1 if this entry has been
        // deleted.
        int index;

        MapEntry(final int index) {
            this.index = index;
        }

        MapEntry() {
        }

        /**
         * {@inheritDoc}
         *
         * @deprecated Please use the corresponding type-specific method
         *             instead.
         */
        @Deprecated
        public Integer getKey() {
            return (Integer.valueOf(key[index]));
        }

        public int getIntKey() {
            return key[index];
        }

        /**
         * {@inheritDoc}
         *
         * @deprecated Please use the corresponding type-specific method
         *             instead.
         */
        @Deprecated
        public Integer getValue() {
            return (Integer.valueOf(value[index]));
        }

        public int getIntValue() {
            return value[index];
        }

        public int setValue(final int v) {
            final int oldValue = value[index];
            value[index] = v;
            return oldValue;
        }

        public Integer setValue(final Integer v) {
            return (Integer.valueOf(setValue(((v).intValue()))));
        }

        @SuppressWarnings("unchecked")
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<Integer, Integer> e = (Map.Entry<Integer, Integer>) o;

            return ((key[index]) == (((e.getKey()).intValue())))
                && ((value[index]) == (((e.getValue()).intValue())));
        }

        public int hashCode() {
            return (key[index]) ^ (value[index]);
        }

        public String toString() {
            return key[index] + "=>" + value[index];
        }
    }

    /**
     * Unwraps an iterator into an array starting at a given offset for a given
     * number of elements.
     *
     * <P>
     * This method iterates over the given type-specific iterator and stores the
     * elements returned, up to a maximum of <code>length</code>, in the given
     * array starting at <code>offset</code>. The number of actually unwrapped
     * elements is returned (it may be less than <code>max</code> if the
     * iterator emits less than <code>max</code> elements).
     *
     * @param i
     *            a type-specific iterator.
     * @param array
     *            an array to contain the output of the iterator.
     * @param offset
     *            the first element of the array to be returned.
     * @param max
     *            the maximum number of elements to unwrap.
     * @return the number of elements unwrapped.
     */
    public static <K> int unwrap(final Iterator<? extends K> i,
        final K array[], int offset, final int max) {
        if (max < 0)
            throw new IllegalArgumentException(
                "The maximum number of elements (" + max + ") is negative");
        if (offset < 0 || offset + max > array.length)
            throw new IllegalArgumentException();
        int j = max;
        while (j-- != 0 && i.hasNext())
            array[offset++] = i.next();
        return max - j - 1;
    }

    /**
     * Unwraps an iterator into an array.
     *
     * <P>
     * This method iterates over the given type-specific iterator and stores the
     * elements returned in the given array. The iteration will stop when the
     * iterator has no more elements or when the end of the array has been
     * reached.
     *
     * @param i
     *            a type-specific iterator.
     * @param array
     *            an array to contain the output of the iterator.
     * @return the number of elements unwrapped.
     */
    public static <K> int unwrap(final Iterator<? extends K> i, final K array[]) {
        return unwrap(i, array, 0, array.length);
    }

    /** An iterator over a hash map. */

    private class MapIterator {
        /**
         * The index of the last entry returned, if positive or zero; initially,
         * {@link #n}. If negative, the last entry returned was that of the key
         * of index {@code - pos - 1} from the {@link #wrapped} list.
         */
        int pos = n;
        /**
         * The index of the last entry that has been returned (more precisely,
         * the value of {@link #pos} if {@link #pos} is positive, or
         * {@link Integer#MIN_VALUE} if {@link #pos} is negative). It is -1 if
         * either we did not return an entry yet, or the last returned entry has
         * been removed.
         */
        int last = -1;
        /**
         * A downward counter measuring how many entries must still be returned.
         */
        int c = size;
        /**
         * A boolean telling us whether we should return the entry with the null
         * key.
         */
        boolean mustReturnNullKey = GridInt2IntOpenHashMap.this.containsNullKey;
        /**
         * A lazily allocated list containing keys of entries that have wrapped
         * around the table because of removals.
         */
        IntArrayList wrapped;

        public boolean hasNext() {
            return c != 0;
        }

        public int nextEntry() {
            if (!hasNext())
                throw new NoSuchElementException();

            c--;
            if (mustReturnNullKey) {
                mustReturnNullKey = false;
                return last = n;
            }

            final int key[] = GridInt2IntOpenHashMap.this.key;

            for (;;) {
                if (--pos < 0) {
                    // We are just enumerating elements from the wrapped list.
                    last = Integer.MIN_VALUE;
                    final int k = wrapped.getInt(-pos - 1);
                    int p = (mix((k))) & mask;
                    while (!((k) == (key[p])))
                        p = (p + 1) & mask;
                    return p;
                }
                if (!((key[pos]) == (0)))
                    return last = pos;
            }
        }

        /**
         * Shifts left entries with the specified hash code, starting at the
         * specified position, and empties the resulting free entry.
         *
         * @param pos
         *            a starting position.
         */
        private final void shiftKeys(int pos) {
            // Shift entries with the same hash.
            int last, slot;
            int curr;
            final int[] key = GridInt2IntOpenHashMap.this.key;

            for (;;) {
                pos = ((last = pos) + 1) & mask;

                for (;;) {
                    if (((curr = key[pos]) == (0))) {
                        key[last] = (0);

                        return;
                    }
                    slot = (mix((curr)))
                        & mask;
                    if (last <= pos ? last >= slot || slot > pos : last >= slot
                        && slot > pos)
                        break;
                    pos = (pos + 1) & mask;
                }

                if (pos < last) { // Wrapped entry.
                    if (wrapped == null)
                        wrapped = new IntArrayList(2);
                    wrapped.add(key[pos]);
                }

                key[last] = curr;
                value[last] = value[pos];
            }
        }

        public void remove() {
            if (last == -1)
                throw new IllegalStateException();
            if (last == n) {
                containsNullKey = false;

            } else if (pos >= 0)
                shiftKeys(last);
            else {
                // We're removing wrapped entries.

                GridInt2IntOpenHashMap.this.remove(wrapped.getInt(-pos - 1));

                last = -1; // Note that we must not decrement size
                return;
            }

            size--;
            last = -1; // You can no longer remove this entry.
            if (ASSERTS)
                checkTable();
        }

        public int skip(final int n) {
            int i = n;
            while (i-- != 0 && hasNext())
                nextEntry();
            return n - i - 1;
        }
    }

    private class EntryIterator extends MapIterator
        implements
        ObjectIterator<Int2IntMap.Entry> {
        private MapEntry entry;

        public Int2IntMap.Entry next() {
            return entry = new MapEntry(nextEntry());
        }

        @Override
        public void remove() {
            super.remove();
            entry.index = -1; // You cannot use a deleted entry.
        }
    }

    private class FastEntryIterator extends MapIterator
        implements
        ObjectIterator<Int2IntMap.Entry> {
        private final MapEntry entry = new MapEntry();
        public MapEntry next() {
            entry.index = nextEntry();
            return entry;
        }
    }
    private final class MapEntrySet extends AbstractCollection<Entry>
        implements
        Cloneable, ObjectSet<Entry>, Collection<Entry>, Iterable<Entry> {

        protected MapEntrySet() {
        }



        public ObjectIterator<Int2IntMap.Entry> iterator() {
            return new EntryIterator();
        }

        public ObjectIterator<Int2IntMap.Entry> fastIterator() {
            return new FastEntryIterator();
        }

        public boolean contains(final Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

            if (e.getKey() == null || !(e.getKey() instanceof Integer))
                return false;

            if (e.getValue() == null || !(e.getValue() instanceof Integer))
                return false;

            final int k = ((((Integer) (e.getKey())).intValue()));
            final int v = ((((Integer) (e.getValue())).intValue()));

            if (((k) == (0)))
                return GridInt2IntOpenHashMap.this.containsNullKey
                    && ((value[n]) == (v));

            int curr;
            final int[] key = GridInt2IntOpenHashMap.this.key;
            int pos;

            // The starting point.
            if (((curr = key[pos = (mix((k)))
                & mask]) == (0)))
                return false;
            if (((k) == (curr)))
                return ((value[pos]) == (v));
            // There's always an unused entry.
            while (true) {
                if (((curr = key[pos = (pos + 1) & mask]) == (0)))
                    return false;
                if (((k) == (curr)))
                    return ((value[pos]) == (v));
            }
        }

        public boolean remove(final Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

            if (e.getKey() == null || !(e.getKey() instanceof Integer))
                return false;

            if (e.getValue() == null || !(e.getValue() instanceof Integer))
                return false;

            final int k = ((((Integer) (e.getKey())).intValue()));
            final int v = ((((Integer) (e.getValue())).intValue()));

            if (((k) == (0))) {
                if (containsNullKey && ((value[n]) == (v))) {
                    removeNullEntry();
                    return true;
                }
                return false;
            }

            int curr;
            final int[] key = GridInt2IntOpenHashMap.this.key;
            int pos;

            // The starting point.
            if (((curr = key[pos = (mix((k)))
                & mask]) == (0)))
                return false;
            if (((curr) == (k))) {
                if (((value[pos]) == (v))) {
                    removeEntry(pos);
                    return true;
                }
                return false;
            }

            while (true) {
                if (((curr = key[pos = (pos + 1) & mask]) == (0)))
                    return false;
                if (((curr) == (k))) {
                    if (((value[pos]) == (v))) {
                        removeEntry(pos);
                        return true;
                    }
                }
            }
        }

        public int size() {
            return size;
        }

        public void clear() {
            GridInt2IntOpenHashMap.this.clear();
        }

        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Set))
                return false;

            Set<?> s = (Set<?>) o;
            if (s.size() != size())
                return false;
            return containsAll(s);
        }

        /**
         * Returns a hash code for this set.
         *
         * The hash code of a set is computed by summing the hash codes of its
         * elements.
         *
         * @return a hash code for this set.
         */

        public int hashCode() {
            int h = 0, n = size();
            ObjectIterator<Entry> i = iterator();
            Entry k;

            while (n-- != 0) {
                k = i.next(); // We need k because KEY2JAVAHASH() is a macro with
                // repeated evaluation.
                h += ((k) == null ? 0 : (k).hashCode());
            }
            return h;
        }

        public Object[] toArray() {
            final Object[] a = new Object[size()];
            unwrap(iterator(), a);
            return a;
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            final int size = size();
            if (a.length < size)
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                    .getComponentType(), size);
            unwrap(iterator(), a);
            if (size < a.length)
                a[size] = null;
            return a;
        }

        /**
         * Adds all elements of the given collection to this collection.
         *
         * @param c
         *            a collection.
         * @return <code>true</code> if this collection changed as a result of the
         *         call.
         */

        public boolean addAll(Collection<? extends Entry> c) {
            boolean retVal = false;
            final Iterator<? extends Entry> i = c.iterator();
            int n = c.size();

            while (n-- != 0)
                if (add(i.next()))
                    retVal = true;
            return retVal;
        }

        public boolean add(Entry k) {
            throw new UnsupportedOperationException();
        }

        /** Delegates to the new covariantly stronger generic method. */

        @Deprecated
        public ObjectIterator<Entry> objectIterator() {
            return iterator();
        }

        /**
         * Checks whether this collection contains all elements from the given
         * collection.
         *
         * @param c
         *            a collection.
         * @return <code>true</code> if this collection contains all elements of the
         *         argument.
         */

        public boolean containsAll(Collection<?> c) {
            int n = c.size();

            final Iterator<?> i = c.iterator();
            while (n-- != 0)
                if (!contains(i.next()))
                    return false;

            return true;
        }

        /**
         * Retains in this collection only elements from the given collection.
         *
         * @param c
         *            a collection.
         * @return <code>true</code> if this collection changed as a result of the
         *         call.
         */

        public boolean retainAll(Collection<?> c) {
            boolean retVal = false;
            int n = size();

            final Iterator<?> i = iterator();
            while (n-- != 0) {
                if (!c.contains(i.next())) {
                    i.remove();
                    retVal = true;
                }
            }

            return retVal;
        }

        /**
         * Remove from this collection all elements in the given collection. If the
         * collection is an instance of this class, it uses faster iterators.
         *
         * @param c
         *            a collection.
         * @return <code>true</code> if this collection changed as a result of the
         *         call.
         */

        public boolean removeAll(Collection<?> c) {
            boolean retVal = false;
            int n = c.size();

            final Iterator<?> i = c.iterator();
            while (n-- != 0)
                if (remove(i.next()))
                    retVal = true;

            return retVal;
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public String toString() {
            final StringBuilder s = new StringBuilder();
            final ObjectIterator<Entry> i = iterator();
            int n = size();
            Object k;
            boolean first = true;

            s.append("{");

            while (n-- != 0) {
                if (first)
                    first = false;
                else
                    s.append(", ");
                k = i.next();

                if (this == k)
                    s.append("(this collection)");
                else

                    s.append(String.valueOf(k));
            }

            s.append("}");
            return s.toString();
        }
    }

    public MapEntrySet int2IntEntrySet() {
        if (entries == null)
            entries = new MapEntrySet();

        return entries;
    }

    /**
     * An iterator on keys.
     *
     * <P>
     * We simply override the {@link java.util.ListIterator#next()}/
     * {@link java.util.ListIterator#previous()} methods (and possibly their
     * type-specific counterparts) so that they return keys instead of entries.
     */
    private final class KeyIterator extends MapIterator implements IntIterator {

        public KeyIterator() {
            super();
        }
        public int nextInt() {
            return key[nextEntry()];
        }

        public Integer next() {
            return (Integer.valueOf(key[nextEntry()]));
        }

    }

    private final class KeySet extends AbstractIntCollection implements Cloneable, IntSet {

        protected KeySet() {
        }

        public IntIterator iterator() {
            return new KeyIterator();
        }

        public int size() {
            return size;
        }

        public boolean contains(int k) {
            return containsKey(k);
        }

        public boolean remove(int k) {
            final int oldSize = size;
            GridInt2IntOpenHashMap.this.remove(k);
            return size != oldSize;
        }

        public void clear() {
            GridInt2IntOpenHashMap.this.clear();
        }

        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Set))
                return false;

            Set<?> s = (Set<?>) o;
            if (s.size() != size())
                return false;
            return containsAll(s);
        }

        /**
         * Returns a hash code for this set.
         *
         * The hash code of a set is computed by summing the hash codes of its
         * elements.
         *
         * @return a hash code for this set.
         */

        public int hashCode() {
            int h = 0, n = size();
            IntIterator i = iterator();
            int k;

            while (n-- != 0) {
                k = i.nextInt(); // We need k because KEY2JAVAHASH() is a macro with
                // repeated evaluation.
                h += (k);
            }
            return h;
        }

        /** Delegates to the corresponding type-specific method. */
        public boolean remove(final Object o) {
            return remove(((((Integer) (o)).intValue())));
        }
    }

    public IntSet keySet() {

        if (keys == null)
            keys = new KeySet();
        return keys;
    }

    /**
     * An iterator on values.
     *
     * <P>
     * We simply override the {@link java.util.ListIterator#next()}/
     * {@link java.util.ListIterator#previous()} methods (and possibly their
     * type-specific counterparts) so that they return values instead of
     * entries.
     */
    private final class ValueIterator extends MapIterator
        implements
        IntIterator {

        public ValueIterator() {
            super();
        }
        public int nextInt() {
            return value[nextEntry()];
        }

        /**
         * {@inheritDoc}
         *
         * @deprecated Please use the corresponding type-specific method
         *             instead.
         */
        @Deprecated
        @Override
        public Integer next() {
            return (Integer.valueOf(value[nextEntry()]));
        }

    }

    public IntCollection values() {
        if (values == null)
            values = new AbstractIntCollection() {

                public IntIterator iterator() {
                    return new ValueIterator();
                }

                public int size() {
                    return size;
                }

                public boolean contains(int v) {
                    return containsValue(v);
                }

                public void clear() {
                    GridInt2IntOpenHashMap.this.clear();
                }
            };

        return values;
    }

    /**
     * A no-op for backward compatibility. The kind of tables implemented by
     * this class never need rehashing.
     *
     * <P>
     * If you need to reduce the table size to fit exactly this set, use
     * {@link #trim()}.
     *
     * @return true.
     * @see #trim()
     * @deprecated A no-op.
     */

    @Deprecated
    public boolean rehash() {
        return true;
    }

    /**
     * Rehashes the map, making the table as small as possible.
     *
     * <P>
     * This method rehashes the table to the smallest size satisfying the load
     * factor. It can be used when the set will not be changed anymore, so to
     * optimize access speed and size.
     *
     * <P>
     * If the table size is already the minimum possible, this method does
     * nothing.
     *
     * @return true if there was enough memory to trim the map.
     * @see #trim(int)
     */

    public boolean trim() {
        final int l = arraySize(size, f);
        if (l >= n || size > maxFill(l, f))
            return true;
        try {
            rehash(l);
        } catch (OutOfMemoryError cantDoIt) {
            return false;
        }
        return true;
    }

    /**
     * Rehashes this map if the table is too large.
     *
     * <P>
     * Let <var>N</var> be the smallest table size that can hold
     * <code>max(n,{@link #size()})</code> entries, still satisfying the load
     * factor. If the current table size is smaller than or equal to
     * <var>N</var>, this method does nothing. Otherwise, it rehashes this map
     * in a table of size <var>N</var>.
     *
     * <P>
     * This method is useful when reusing maps. {@linkplain #clear() Clearing a
     * map} leaves the table size untouched. If you are reusing a map many times,
     * you can call this method with a typical size to avoid keeping around a
     * very large table just because of a few large transient maps.
     *
     * @param n
     *            the threshold for the trimming.
     * @return true if there was enough memory to trim the map.
     * @see #trim()
     */

    public boolean trim(final int n) {
        final int l = nextPowerOfTwo((int) Math.ceil(n / f));
        if (l >= n || size > maxFill(l, f))
            return true;
        try {
            rehash(l);
        } catch (OutOfMemoryError cantDoIt) {
            return false;
        }
        return true;
    }

    /**
     * Rehashes the map.
     *
     * <P>
     * This method implements the basic rehashing strategy, and may be overriden
     * by subclasses implementing different rehashing strategies (e.g.,
     * disk-based rehashing). However, you should not override this method
     * unless you understand the internal workings of this class.
     *
     * @param newN
     *            the new size
     */

    protected void rehash(final int newN) {
        final int key[] = this.key;
        final int value[] = this.value;

        final int mask = newN - 1; // Note that this is used by the hashing
        // macro
        final int newKey[] = new int[newN + 1];
        final int newValue[] = new int[newN + 1];
        int i = n, pos;

        for (int j = realSize(); j-- != 0;) {
            while (((key[--i]) == (0)));

            if (!((newKey[pos = (mix((key[i])))
                & mask]) == (0)))
                while (!((newKey[pos = (pos + 1) & mask]) == (0)));

            newKey[pos] = key[i];
            newValue[pos] = value[i];
        }

        newValue[newN] = value[n];

        n = newN;
        this.mask = mask;
        maxFill = maxFill(n, f);
        this.key = newKey;
        this.value = newValue;
    }

    /**
     * Returns a deep copy of this map.
     *
     * <P>
     * This method performs a deep copy of this hash map; the data stored in the
     * map, however, is not cloned. Note that this makes a difference only for
     * object keys.
     *
     * @return a deep copy of this map.
     */

    public GridInt2IntOpenHashMap clone() {
        GridInt2IntOpenHashMap c;
        try {
            c = (GridInt2IntOpenHashMap) super.clone();
        } catch (CloneNotSupportedException cantHappen) {
            throw new InternalError();
        }

        c.keys = null;
        c.values = null;
        c.entries = null;
        c.containsNullKey = containsNullKey;

        c.key = key.clone();
        c.value = value.clone();

        return c;
    }

    /**
     * Returns a hash code for this map.
     *
     * This method overrides the generic method provided by the superclass.
     * Since <code>equals()</code> is not overriden, it is important that the
     * value returned by this method is the same value as the one returned by
     * the overriden method.
     *
     * @return a hash code for this map.
     */

    public int hashCode() {
        int h = 0;
        for (int j = realSize(), i = 0, t = 0; j-- != 0;) {
            while (((key[i]) == (0)))
                i++;

            t = (key[i]);

            t ^= (value[i]);
            h += t;
            i++;
        }
        // Zero / null keys have hash zero.
        if (containsNullKey)
            h += (value[n]);
        return h;
    }

    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        final int key[] = this.key;
        final int value[] = this.value;
        final MapIterator i = new MapIterator();

        s.defaultWriteObject();

        for (int j = size, e; j-- != 0;) {
            e = i.nextEntry();
            s.writeInt(key[e]);
            s.writeInt(value[e]);
        }
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();

        n = arraySize(size, f);
        maxFill = maxFill(n, f);
        mask = n - 1;

        final int key[] = this.key = new int[n + 1];
        final int value[] = this.value = new int[n + 1];

        int k;
        int v;

        for (int i = size, pos; i-- != 0;) {

            k = s.readInt();
            v = s.readInt();

            if (((k) == (0))) {
                pos = n;
                containsNullKey = true;
            } else {
                pos = (mix((k))) & mask;
                while (!((key[pos]) == (0)))
                    pos = (pos + 1) & mask;
            }

            key[pos] = k;
            value[pos] = v;
        }
        if (ASSERTS)
            checkTable();
    }
    private void checkTable() {
    }
}

interface IntSet extends IntCollection, Set<Integer> {

    /**
     * Returns a type-specific iterator on the elements of this set.
     *
     * <p>
     * Note that this specification strengthens the one given in
     * {@link java.lang.Iterable#iterator()}, which was already strengthened in
     * the corresponding type-specific class, but was weakened by the fact that
     * this interface extends {@link Set}.
     *
     * @return a type-specific iterator on the elements of this set.
     */
    IntIterator iterator();

    /**
     * Removes an element from this set.
     *
     * <p>
     * Note that the corresponding method of the type-specific collection is
     * <code>rem()</code>. This unfortunate situation is caused by the clash
     * with the similarly named index-based method in the {@link java.util.List}
     * interface.
     *
     * @see java.util.Collection#remove(Object)
     */
    public boolean remove(int k);
}

interface IntCollection extends Collection<Integer>, Iterable<Integer> {
    /**
     * Returns a type-specific iterator on the elements of this collection.
     *
     * <p>
     * Note that this specification strengthens the one given in
     * {@link java.lang.Iterable#iterator()}, which was already strengthened in
     * the corresponding type-specific class, but was weakened by the fact that
     * this interface extends {@link Collection}.
     *
     * @return a type-specific iterator on the elements of this collection.
     */
    IntIterator iterator();

    /**
     * Returns a type-specific iterator on this elements of this collection.
     *
     * @see #iterator()
     * @deprecated As of <code>fastutil</code> 5, replaced by
     *             {@link #iterator()}.
     */
    @Deprecated
    IntIterator intIterator();

    /**
     * Returns an containing the items of this collection; the runtime type of
     * the returned array is that of the specified array.
     *
     * <p>
     * <strong>Warning</strong>: Note that, contrarily to
     * {@link Collection#toArray(Object[])}, this methods just writes all
     * elements of this collection: no special value will be added after the
     * last one.
     *
     * @param a
     *            if this array is big enough, it will be used to store this
     *            collection.
     * @return a primitive type array containing the items of this collection.
     * @see Collection#toArray(Object[])
     */
    <T> T[] toArray(T[] a);

    /**
     * @see Collection#contains(Object)
     */
    boolean contains(int key);

    /**
     * Returns a primitive type array containing the items of this collection.
     *
     * @return a primitive type array containing the items of this collection.
     * @see Collection#toArray()
     */
    int[] toIntArray();

    /**
     * Returns a primitive type array containing the items of this collection.
     *
     * <p>
     * Note that, contrarily to {@link Collection#toArray(Object[])}, this
     * methods just writes all elements of this collection: no special value
     * will be added after the last one.
     *
     * @param a
     *            if this array is big enough, it will be used to store this
     *            collection.
     * @return a primitive type array containing the items of this collection.
     * @see Collection#toArray(Object[])
     */
    int[] toIntArray(int a[]);

    /**
     * Returns a primitive type array containing the items of this collection.
     *
     * <p>
     * Note that, contrarily to {@link Collection#toArray(Object[])}, this
     * methods just writes all elements of this collection: no special value
     * will be added after the last one.
     *
     * @param a
     *            if this array is big enough, it will be used to store this
     *            collection.
     * @return a primitive type array containing the items of this collection.
     * @see Collection#toArray(Object[])
     */
    int[] toArray(int a[]);

    /**
     * @see Collection#add(Object)
     */
    boolean add(int key);

    /**
     * Note that this method should be called
     * {@link java.util.Collection#remove(Object) remove()}, but the clash with
     * the similarly named index-based method in the {@link java.util.List}
     * interface forces us to use a distinguished name. For simplicity, the set
     * interfaces reinstates <code>remove()</code>.
     *
     * @see Collection#remove(Object)
     */
    boolean rem(int key);

    /**
     * @see Collection#addAll(Collection)
     */
    boolean addAll(IntCollection c);

    /**
     * @see Collection#containsAll(Collection)
     */
    boolean containsAll(IntCollection c);

    /**
     * @see Collection#removeAll(Collection)
     */
    boolean removeAll(IntCollection c);

    /**
     * @see Collection#retainAll(Collection)
     */
    boolean retainAll(IntCollection c);

}

interface IntIterator extends Iterator<Integer> {

    /**
     * Returns the next element as a primitive type.
     *
     * @return the next element in the iteration.
     * @see Iterator#next()
     */

    int nextInt();

    /**
     * Skips the given number of elements.
     *
     * <P>
     * The effect of this call is exactly the same as that of calling
     * {@link #next()} for <code>n</code> times (possibly stopping if
     * {@link #hasNext()} becomes false).
     *
     * @param n
     *            the number of elements to skip.
     * @return the number of elements actually skipped.
     * @see Iterator#next()
     */

    int skip(int n);
}

interface Int2IntMap extends Map<Integer, Integer> {

    /**
     * Adds a pair to the map.
     *
     * @param key
     *            the key.
     * @param value
     *            the value.
     * @return the old value, or the {@linkplain #defaultReturnValue() default
     *         return value} if no value was present for the given key.
     */

    int put(int key, int value);

    /**
     * Returns the value to which the given key is mapped.
     *
     * @param key
     *            the key.
     * @return the corresponding value, or the
     *         {@linkplain #defaultReturnValue() default return value} if no
     *         value was present for the given key.
     */

    int get(int key);

    /**
     * Removes the mapping with the given key.
     *
     * @param key
     *            the key.
     * @return the old value, or the {@linkplain #defaultReturnValue() default
     *         return value} if no value was present for the given key.
     */

    int remove(int key);

    /**
     */

    boolean containsKey(int key);

    /**
     * Sets the default return value.
     *
     * This value must be returned by type-specific versions of
     * <code>get()</code>, <code>put()</code> and <code>remove()</code> to
     * denote that the map does not contain the specified key. It must be 0/
     * <code>false</code>/<code>null</code> by default.
     *
     * @param rv
     *            the new default return value.
     * @see #defaultReturnValue()
     */

    void defaultReturnValue(int rv);

    /**
     * Gets the default return value.
     *
     * @return the current default return value.
     */

    int defaultReturnValue();

    /** Associates the specified value with the specified key in this function (optional operation).
     *
     * @param key the key.
     * @param value the value.
     * @return the old value, or <code>null</code> if no value was present for the given key.
     * @see Map#put(Object,Object)
     */

    Integer put( Integer key, Integer value );

    /** Returns the value associated by this function to the specified key.
     *
     * @param key the key.
     * @return the corresponding value, or <code>null</code> if no value was present for the given key.
     * @see Map#get(Object)
     */

    Integer get( Object key );

    /** Returns true if this function contains a mapping for the specified key.
     *
     * <p>Note that for some kind of functions (e.g., hashes) this method
     * will always return true.
     *
     * @param key the key.
     * @return true if this function associates a value to <code>key</code>.
     * @see Map#containsKey(Object)
     */

    boolean containsKey( Object key );

    /** Removes this key and the associated value from this function if it is present (optional operation).
     *
     * @param key the key.
     * @return the old value, or <code>null</code> if no value was present for the given key.
     * @see Map#remove(Object)
     */

    Integer remove( Object key );

    /** Returns the intended number of keys in this function, or -1 if no such number exists.
     *
     * <p>Most function implementations will have some knowledge of the intended number of keys
     * in their domain. In some cases, however, this might not be possible.
     *
     *  @return the intended number of keys in this function, or -1 if that number is not available.
     */
    int size();

    /** Removes all associations from this function (optional operation).
     *
     * @see Map#clear()
     */

    void clear();

    /**
     * Returns a set view of the mappings contained in this map.
     * <P>
     * Note that this specification strengthens the one given in
     * {@link Map#entrySet()}.
     *
     * @return a set view of the mappings contained in this map.
     * @see Map#entrySet()
     */

    ObjectSet<Map.Entry<Integer, Integer>> entrySet();



    ObjectSet<Int2IntMap.Entry> int2IntEntrySet();

    /**
     * Returns a set view of the keys contained in this map.
     * <P>
     * Note that this specification strengthens the one given in
     * {@link Map#keySet()}.
     *
     * @return a set view of the keys contained in this map.
     * @see Map#keySet()
     */

    IntSet keySet();

    /**
     * Returns a set view of the values contained in this map.
     * <P>
     * Note that this specification strengthens the one given in
     * {@link Map#values()}.
     *
     * @return a set view of the values contained in this map.
     * @see Map#values()
     */

    IntCollection values();

    /**
     * @see Map#containsValue(Object)
     */

    boolean containsValue(int value);

    /**
     * A type-specific {@link java.util.Map.Entry}; provides some additional
     * methods that use polymorphism to avoid (un)boxing.
     *
     * @see java.util.Map.Entry
     */

    interface Entry extends Map.Entry<Integer, Integer> {

        /**
         * {@inheritDoc}
         *
         * @deprecated Please use the corresponding type-specific method
         *             instead.
         */
        @Deprecated
        @Override
        Integer getKey();

        /**
         * @see java.util.Map.Entry#getKey()
         */
        int getIntKey();

        /**
         * {@inheritDoc}
         *
         * @deprecated Please use the corresponding type-specific method
         *             instead.
         */
        @Deprecated
        @Override
        Integer getValue();

        /**
         * @see java.util.Map.Entry#setValue(Object)
         */
        int setValue(int value);

        /**
         * @see java.util.Map.Entry#getValue()
         */
        int getIntValue();

    }
}

class IntArrayList extends AbstractIntCollection
    implements
    RandomAccess,
    Cloneable,
    java.io.Serializable, IntList, IntStack {
    private static final long serialVersionUID = -7046029254386353130L;
    /** The initial default capacity of an array list. */
    public final static int DEFAULT_INITIAL_CAPACITY = 16;

    /** The backing array. */
    protected transient int a[];

    /**
     * The current actual size of the list (never greater than the backing-array
     * length).
     */
    protected int size;

    private static final boolean ASSERTS = false;

    /**
     * Creates a new array list using a given array.
     *
     * <P>
     * This constructor is only meant to be used by the wrapping methods.
     *
     * @param a
     *            the array that will be used to back this array list.
     */

    @SuppressWarnings("unused")
    protected IntArrayList(final int a[], boolean dummy) {
        this.a = a;

    }

    /**
     * Creates a new array list with given capacity.
     *
     * @param capacity
     *            the initial capacity of the array list (may be 0).
     */

    public IntArrayList(final int capacity) {
        if (capacity < 0)
            throw new IllegalArgumentException("Initial capacity (" + capacity
                + ") is negative");

        a = new int[capacity];

    }

    /**
     * Creates a new array list with {@link #DEFAULT_INITIAL_CAPACITY} capacity.
     */

    public IntArrayList() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new array list and fills it with a given collection.
     *
     * @param c
     *            a collection that will be used to fill the array list.
     */

    public IntArrayList(final Collection<? extends Integer> c) {
        this(c.size());

        size = unwrap(asIntIterator(c.iterator()), a);

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static IntIterator asIntIterator(final Iterator i) {
        if (i instanceof IntIterator)
            return (IntIterator) i;
        return new IteratorWrapper(i);
    }

    /**
     * Ensures that the given index is nonnegative and not greater than the list
     * size.
     *
     * @param index
     *            an index.
     * @throws IndexOutOfBoundsException
     *             if the given index is negative or greater than the list size.
     */
    protected void ensureIndex(final int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException("Index (" + index
                + ") is negative");
        if (index > size())
            throw new IndexOutOfBoundsException("Index (" + index
                + ") is greater than list size (" + (size()) + ")");
    }

    /**
     * Ensures that the given index is nonnegative and smaller than the list
     * size.
     *
     * @param index
     *            an index.
     * @throws IndexOutOfBoundsException
     *             if the given index is negative or not smaller than the list
     *             size.
     */
    protected void ensureRestrictedIndex(final int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException("Index (" + index
                + ") is negative");
        if (index >= size())
            throw new IndexOutOfBoundsException("Index (" + index
                + ") is greater than or equal to list size (" + (size())
                + ")");
    }

    /** Delegates to a more generic method. */
    public boolean addAll(final Collection<? extends Integer> c) {
        return addAll(size(), c);
    }

    /** Delegates to the new covariantly stronger generic method. */

    @Deprecated
    public IntListIterator intListIterator() {
        return listIterator();
    }

    /** Delegates to the new covariantly stronger generic method. */

    @Deprecated
    public IntListIterator intListIterator(final int index) {
        return listIterator(index);
    }

    public IntListIterator iterator() {
        return listIterator();
    }

    public IntListIterator listIterator() {
        return listIterator(0);
    }

    public boolean contains(final int k) {
        return indexOf(k) >= 0;
    }

    /** Delegates to the new covariantly stronger generic method. */

    @Deprecated
    public IntList intSubList(final int from, final int to) {
        return subList(from, to);
    }

    public void addElements(final int index, final int a[]) {
        addElements(index, a, 0, a.length);
    }

    private boolean valEquals(final Object a, final Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        final List<?> l = (List<?>) o;
        int s = size();
        if (s != l.size())
            return false;

        if (l instanceof IntList) {
            final IntListIterator i1 = listIterator(), i2 = ((IntList) l)
                .listIterator();
            while (s-- != 0)
                if (i1.nextInt() != i2.nextInt())
                    return false;
            return true;
        }

        final ListIterator<?> i1 = listIterator(), i2 = l.listIterator();

        while (s-- != 0)
            if (!valEquals(i1.next(), i2.next()))
                return false;

        return true;
    }

    /**
     * Compares this list to another object. If the argument is a
     * {@link List}, this method performs a lexicographical
     * comparison; otherwise, it throws a <code>ClassCastException</code>.
     *
     * @param l
     *            a list.
     * @return if the argument is a {@link List}, a negative integer,
     *         zero, or a positive integer as this list is lexicographically
     *         less than, equal to, or greater than the argument.
     * @throws ClassCastException
     *             if the argument is not a list.
     */

    public int compareTo(final List<? extends Integer> l) {
        if (l == this)
            return 0;

        if (l instanceof IntList) {

            final IntListIterator i1 = listIterator(), i2 = ((IntList) l)
                .listIterator();
            int r;
            int e1, e2;

            while (i1.hasNext() && i2.hasNext()) {
                e1 = i1.nextInt();
                e2 = i2.nextInt();
                if ((r = (Integer.compare((e1), (e2)))) != 0)
                    return r;
            }
            return i2.hasNext() ? -1 : (i1.hasNext() ? 1 : 0);
        }

        ListIterator<? extends Integer> i1 = listIterator(), i2 = l
            .listIterator();
        int r;

        while (i1.hasNext() && i2.hasNext()) {
            if ((r = ((Comparable<? super Integer>) i1.next()).compareTo(i2
                .next())) != 0)
                return r;
        }
        return i2.hasNext() ? -1 : (i1.hasNext() ? 1 : 0);
    }

    /**
     * Returns the hash code for this list, which is identical to
     * {@link List#hashCode()}.
     *
     * @return the hash code for this list.
     */
    public int hashCode() {
        IntIterator i = iterator();
        int h = 1, s = size();
        while (s-- != 0) {
            int k = i.nextInt();
            h = 31 * h + (k);
        }
        return h;
    }

    public void push(int o) {
        add(o);
    }

    public int popInt() {
        if (isEmpty())
            throw new NoSuchElementException();
        return removeInt(size() - 1);
    }

    public int topInt() {
        if (isEmpty())
            throw new NoSuchElementException();
        return getInt(size() - 1);
    }

    public int peekInt(int i) {
        return getInt(size() - 1 - i);
    }

    public boolean addAll(final IntList l) {
        return addAll(size(), l);
    }

    /** Delegates to the corresponding type-specific method. */
    public void add(final int index, final Integer ok) {
        add(index, ok.intValue());
    }

    /**
     * Delegates to the corresponding type-specific method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    public Integer set(final int index, final Integer ok) {
        return (Integer.valueOf(set(index, ok.intValue())));
    }

    /**
     * Delegates to the corresponding type-specific method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    public Integer get(final int index) {
        return (Integer.valueOf(getInt(index)));
    }

    /** Delegates to the corresponding type-specific method. */
    public int indexOf(final Object ok) {
        return indexOf(((((Integer) (ok)).intValue())));
    }

    /** Delegates to the corresponding type-specific method. */
    public int lastIndexOf(final Object ok) {
        return lastIndexOf(((((Integer) (ok)).intValue())));
    }

    /**
     * Delegates to the corresponding type-specific method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    public Integer remove(final int index) {
        return (Integer.valueOf(removeInt(index)));
    }

    /** Delegates to the corresponding type-specific method. */
    public void push(Integer o) {
        push(o.intValue());
    }

    /**
     * Delegates to the corresponding type-specific method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    public Integer pop() {
        return Integer.valueOf(popInt());
    }

    /**
     * Delegates to the corresponding type-specific method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    public Integer top() {
        return Integer.valueOf(topInt());
    }

    /**
     * Delegates to the corresponding type-specific method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    public Integer peek(int i) {
        return Integer.valueOf(peekInt(i));
    }

    public String toString() {
        final StringBuilder s = new StringBuilder();
        final IntIterator i = iterator();
        int n = size();
        int k;
        boolean first = true;

        s.append("[");

        while (n-- != 0) {
            if (first)
                first = false;
            else
                s.append(", ");
            k = i.nextInt();

            s.append(String.valueOf(k));
        }

        s.append("]");
        return s.toString();
    }

    private static class IteratorWrapper implements IntIterator {
        final Iterator<Integer> i;

        public IteratorWrapper(final Iterator<Integer> i) {
            this.i = i;
        }

        public boolean hasNext() {
            return i.hasNext();
        }
        public void remove() {
            i.remove();
        }

        public int nextInt() {
            return ((i.next()).intValue());
        }

        /**
         * Delegates to the corresponding type-specific method.
         *
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        public Integer next() {
            return Integer.valueOf(nextInt());
        }

        /**
         * This method just iterates the type-specific version of {@link #next()}
         * for at most <code>n</code> times, stopping if {@link #hasNext()} becomes
         * false.
         */

        public int skip(final int n) {
            int i = n;
            while (i-- != 0 && hasNext())
                nextInt();
            return n - i - 1;
        }
    }

    /**
     * Creates a new array list and fills it with a given type-specific
     * collection.
     *
     * @param c
     *            a type-specific collection that will be used to fill the array
     *            list.
     */

    public IntArrayList(final IntCollection c) {
        this(c.size());
        size = unwrap(c.iterator(), a);
    }

    /**
     * Creates a new array list and fills it with a given type-specific list.
     *
     * @param l
     *            a type-specific list that will be used to fill the array list.
     */

    public IntArrayList(final IntList l) {
        this(l.size());
        l.getElements(0, a, 0, size = l.size());
    }

    /**
     * Creates a new array list and fills it with the elements of a given array.
     *
     * @param a
     *            an array whose elements will be used to fill the array list.
     */

    public IntArrayList(final int a[]) {
        this(a, 0, a.length);
    }

    /**
     * Creates a new array list and fills it with the elements of a given array.
     *
     * @param a
     *            an array whose elements will be used to fill the array list.
     * @param offset
     *            the first element to use.
     * @param length
     *            the number of elements to use.
     */

    public IntArrayList(final int a[], final int offset, final int length) {
        this(length);
        System.arraycopy(a, offset, this.a, 0, length);
        size = length;
    }

    /**
     * Creates a new array list and fills it with the elements returned by an
     * iterator..
     *
     * @param i
     *            an iterator whose returned elements will fill the array list.
     */

    public IntArrayList(final Iterator<? extends Integer> i) {
        this();
        while (i.hasNext())
            this.add(i.next());
    }

    /**
     * Creates a new array list and fills it with the elements returned by a
     * type-specific iterator..
     *
     * @param i
     *            a type-specific iterator whose returned elements will fill the
     *            array list.
     */

    public IntArrayList(final IntIterator i) {
        this();
        while (i.hasNext())
            this.add(i.nextInt());
    }

    /**
     * Returns the backing array of this list.
     *
     * @return the backing array.
     */

    public int[] elements() {
        return a;
    }
    /**
     * Wraps a given array into an array list of given size.
     *
     * <P>
     * Note it is guaranteed that the type of the array returned by
     * {@link #elements()} will be the same (see the comments in the class
     * documentation).
     *
     * @param a
     *            an array to wrap.
     * @param length
     *            the length of the resulting array list.
     * @return a new array list of the given size, wrapping the given array.
     */

    public static IntArrayList wrap(final int a[], final int length) {
        if (length > a.length)
            throw new IllegalArgumentException("The specified length ("
                + length + ") is greater than the array size (" + a.length
                + ")");
        final IntArrayList l = new IntArrayList(a, false);
        l.size = length;
        return l;
    }

    public boolean addAll(int index, final Collection<? extends Integer> c) {
        ensureIndex(index);
        int n = c.size();
        if (n == 0)
            return false;
        Iterator<? extends Integer> i = c.iterator();
        while (n-- != 0)
            add(index++, i.next());
        return true;
    }


    public IntList subList(final int from, final int to) {
        ensureIndex(from);
        ensureIndex(to);
        if (from > to)
            throw new IndexOutOfBoundsException("Start index (" + from
                + ") is greater than end index (" + to + ")");

        return new IntSubList(this, from, to);
    }

    /**
     * Wraps a given array into an array list.
     *
     * <P>
     * Note it is guaranteed that the type of the array returned by
     * {@link #elements()} will be the same (see the comments in the class
     * documentation).
     *
     * @param a
     *            an array to wrap.
     * @return a new array list wrapping the given array.
     */

    public static IntArrayList wrap(final int a[]) {
        return wrap(a, a.length);
    }

    /**
     * Ensures that this array list can contain the given number of entries
     * without resizing.
     *
     * @param capacity
     *            the new minimum capacity for this array list.
     */

    public void ensureCapacity(final int capacity) {

        a = ensureCapacity(a, capacity, size);
        if (ASSERTS)
            assert size <= a.length;
    }

    public static int[] ensureCapacity(final int[] array, final int length,
        final int preserve) {
        if (length > array.length) {
            final int t[] =

                new int[length];

            System.arraycopy(array, 0, t, 0, preserve);
            return t;
        }
        return array;
    }

    /**
     * Grows this array list, ensuring that it can contain the given number of
     * entries without resizing, and in case enlarging it at least by a factor
     * of two.
     *
     * @param capacity
     *            the new minimum capacity for this array list.
     */

    private void grow(final int capacity) {

        a = grow(a, capacity, size);
        if (ASSERTS)
            assert size <= a.length;
    }

    public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    public static int[] grow(final int[] array, final int length,
        final int preserve) {

        if (length > array.length) {
            final int newLength = (int) Math.max(
                Math.min(2L * array.length, MAX_ARRAY_SIZE), length);

            final int t[] =

                new int[newLength];

            System.arraycopy(array, 0, t, 0, preserve);

            return t;
        }
        return array;

    }

    public void add(final int index, final int k) {
        ensureIndex(index);
        grow(size + 1);
        if (index != size)
            System.arraycopy(a, index, a, index + 1, size - index);
        a[index] = k;
        size++;
        if (ASSERTS)
            assert size <= a.length;
    }

    public boolean add(final int k) {
        grow(size + 1);
        a[size++] = k;
        if (ASSERTS)
            assert size <= a.length;
        return true;
    }

    public int getInt(final int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException("Index (" + index
                + ") is greater than or equal to list size (" + size + ")");
        return a[index];
    }

    public int indexOf(final int k) {
        for (int i = 0; i < size; i++)
            if (((k) == (a[i])))
                return i;
        return -1;
    }

    public int lastIndexOf(final int k) {
        for (int i = size; i-- != 0;)
            if (((k) == (a[i])))
                return i;
        return -1;
    }

    public int removeInt(final int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException("Index (" + index
                + ") is greater than or equal to list size (" + size + ")");
        final int old = a[index];
        size--;
        if (index != size)
            System.arraycopy(a, index + 1, a, index, size - index);

        if (ASSERTS)
            assert size <= a.length;
        return old;
    }

    public boolean rem(final int k) {
        int index = indexOf(k);
        if (index == -1)
            return false;
        removeInt(index);
        if (ASSERTS)
            assert size <= a.length;
        return true;
    }

    public int set(final int index, final int k) {
        if (index >= size)
            throw new IndexOutOfBoundsException("Index (" + index
                + ") is greater than or equal to list size (" + size + ")");
        int old = a[index];
        a[index] = k;
        return old;
    }

    public void clear() {

        size = 0;
        if (ASSERTS)
            assert size <= a.length;
    }

    public int size() {
        return size;
    }

    public void size(final int size) {
        if (size > a.length)
            ensureCapacity(size);
        if (size > this.size)
            Arrays.fill(a, this.size, size, (0));

        this.size = size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Trims this array list so that the capacity is equal to the size.
     *
     * @see java.util.ArrayList#trimToSize()
     */
    public void trim() {
        trim(0);
    }

    /**
     * Trims the backing array if it is too large.
     *
     * If the current array length is smaller than or equal to <code>n</code>,
     * this method does nothing. Otherwise, it trims the array length to the
     * maximum between <code>n</code> and {@link #size()}.
     *
     * <P>
     * This method is useful when reusing lists. {@linkplain #clear() Clearing a
     * list} leaves the array length untouched. If you are reusing a list many
     * times, you can call this method with a typical size to avoid keeping
     * around a very large array just because of a few large transient lists.
     *
     * @param n
     *            the threshold for the trimming.
     */

    public void trim(final int n) {
        // TODO: use Arrays.trim() and preserve type only if necessary
        if (n >= a.length || size == a.length)
            return;
        final int t[] = new int[Math.max(n, size)];
        System.arraycopy(a, 0, t, 0, size);
        a = t;
        if (ASSERTS)
            assert size <= a.length;
    }

    /**
     * Copies element of this type-specific list into the given array using
     * optimized system calls.
     *
     * @param from
     *            the start index (inclusive).
     * @param a
     *            the destination array.
     * @param offset
     *            the offset into the destination array where to store the first
     *            element copied.
     * @param length
     *            the number of elements to be copied.
     */

    public void getElements(final int from, final int[] a, final int offset,
        final int length) {
        ensureOffsetLength(a, offset, length);
        System.arraycopy(this.a, from, a, offset, length);
    }

    public static void ensureOffsetLength(final int[] a, final int offset,
        final int length) {
        ensureOffsetLength(a.length, offset, length);
    }

    public static void ensureOffsetLength( final int arrayLength, final int offset, final int length ) {
        if ( offset < 0 ) throw new ArrayIndexOutOfBoundsException( "Offset (" + offset + ") is negative" );
        if ( length < 0 ) throw new IllegalArgumentException( "Length (" + length + ") is negative" );
        if ( offset + length > arrayLength ) throw new ArrayIndexOutOfBoundsException( "Last index (" + ( offset + length ) + ") is greater than array length (" + arrayLength + ")" );
    }

    /**
     * Removes elements of this type-specific list using optimized system calls.
     *
     * @param from
     *            the start index (inclusive).
     * @param to
     *            the end index (exclusive).
     */
    public void removeElements(final int from, final int to) {
        ensureFromTo(size, from, to);
        System.arraycopy(a, to, a, from, size - to);
        size -= (to - from);

    }

    public static void ensureFromTo( final int arrayLength, final int from, final int to ) {
        if ( from < 0 ) throw new ArrayIndexOutOfBoundsException( "Start index (" + from + ") is negative" );
        if ( from > to ) throw new IllegalArgumentException( "Start index (" + from + ") is greater than end index (" + to + ")" );
        if ( to > arrayLength ) throw new ArrayIndexOutOfBoundsException( "End index (" + to + ") is greater than array length (" + arrayLength + ")" );
    }

    /**
     * Adds elements to this type-specific list using optimized system calls.
     *
     * @param index
     *            the index at which to add elements.
     * @param a
     *            the array containing the elements.
     * @param offset
     *            the offset of the first element to add.
     * @param length
     *            the number of elements to add.
     */
    public void addElements(final int index, final int a[], final int offset,
        final int length) {
        ensureIndex(index);
        ensureOffsetLength(a, offset, length);
        grow(size + length);
        System.arraycopy(this.a, index, this.a, index + length, size - index);
        System.arraycopy(a, offset, this.a, index, length);
        size += length;
    }

    public int[] toIntArray(int a[]) {
        if (a == null || a.length < size)
            a = new int[size];
        System.arraycopy(this.a, 0, a, 0, size);
        return a;
    }

    public boolean addAll(int index, final IntCollection c) {
        ensureIndex(index);
        int n = c.size();
        if (n == 0)
            return false;
        grow(size + n);
        if (index != size)
            System.arraycopy(a, index, a, index + n, size - index);
        final IntIterator i = c.iterator();
        size += n;
        while (n-- != 0)
            a[index++] = i.nextInt();
        if (ASSERTS)
            assert size <= a.length;
        return true;
    }

    public boolean addAll(final int index, final IntList l) {
        ensureIndex(index);
        final int n = l.size();
        if (n == 0)
            return false;
        grow(size + n);
        if (index != size)
            System.arraycopy(a, index, a, index + n, size - index);
        l.getElements(0, a, index, n);
        size += n;
        if (ASSERTS)
            assert size <= a.length;
        return true;
    }

    @Override
    public boolean removeAll(final IntCollection c) {
        final int[] a = this.a;
        int j = 0;
        for (int i = 0; i < size; i++)
            if (!c.contains(a[i]))
                a[j++] = a[i];
        final boolean modified = size != j;
        size = j;
        return modified;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        final int[] a = this.a;
        int j = 0;
        for (int i = 0; i < size; i++)
            if (!c.contains((Integer.valueOf(a[i]))))
                a[j++] = a[i];
        final boolean modified = size != j;
        size = j;
        return modified;
    }
    public IntListIterator listIterator(final int index) {
        ensureIndex(index);

        return new AbstractIntListIterator() {
            int pos = index, last = -1;

            public boolean hasNext() {
                return pos < size;
            }
            public boolean hasPrevious() {
                return pos > 0;
            }
            public int nextInt() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return a[last = pos++];
            }
            public int previousInt() {
                if (!hasPrevious())
                    throw new NoSuchElementException();
                return a[last = --pos];
            }
            public int nextIndex() {
                return pos;
            }
            public int previousIndex() {
                return pos - 1;
            }
            public void add(int k) {
                IntArrayList.this.add(pos++, k);
                last = -1;
            }
            public void set(int k) {
                if (last == -1)
                    throw new IllegalStateException();
                IntArrayList.this.set(last, k);
            }
            public void remove() {
                if (last == -1)
                    throw new IllegalStateException();
                IntArrayList.this.removeInt(last);
				/*
				 * If the last operation was a next(), we are removing an
				 * element *before* us, and we must decrease pos
				 * correspondingly.
				 */
                if (last < pos)
                    pos--;
                last = -1;
            }
        };
    }

    public IntArrayList clone() {
        IntArrayList c = new IntArrayList(size);
        System.arraycopy(a, 0, c.a, 0, size);
        c.size = size;
        return c;
    }

    /**
     * Compares this type-specific array list to another one.
     *
     * <P>
     * This method exists only for sake of efficiency. The implementation
     * inherited from the abstract implementation would already work.
     *
     * @param l
     *            a type-specific array list.
     * @return true if the argument contains the same elements of this
     *         type-specific array list.
     */
    public boolean equals(final IntArrayList l) {
        if (l == this)
            return true;
        int s = size();
        if (s != l.size())
            return false;
        final int[] a1 = a;
        final int[] a2 = l.a;

        while (s-- != 0)
            if (a1[s] != a2[s])
                return false;

        return true;
    }

    /**
     * Compares this array list to another array list.
     *
     * <P>
     * This method exists only for sake of efficiency. The implementation
     * inherited from the abstract implementation would already work.
     *
     * @param l
     *            an array list.
     * @return a negative integer, zero, or a positive integer as this list is
     *         lexicographically less than, equal to, or greater than the
     *         argument.
     */

    public int compareTo(final IntArrayList l) {
        final int s1 = size(), s2 = l.size();
        final int a1[] = a, a2[] = l.a;
        int e1, e2;
        int r, i;

        for (i = 0; i < s1 && i < s2; i++) {
            e1 = a1[i];
            e2 = a2[i];
            if ((r = (Integer.compare((e1), (e2)))) != 0)
                return r;
        }

        return i < s2 ? -1 : (i < s1 ? 1 : 0);
    }

    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        s.defaultWriteObject();
        for (int i = 0; i < size; i++)
            s.writeInt(a[i]);
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        a = new int[size];
        for (int i = 0; i < size; i++)
            a[i] = s.readInt();
    }


    public static class IntSubList extends AbstractIntCollection
        implements
        java.io.Serializable, IntList {
        private static final long serialVersionUID = -7046029254386353129L;
        /** The list this sublist restricts. */
        protected final IntList l;
        /** Initial (inclusive) index of this sublist. */
        protected final int from;
        /** Final (exclusive) index of this sublist. */
        protected int to;

        private static final boolean ASSERTS = false;

        public IntSubList(final IntList l, final int from, final int to) {
            this.l = l;
            this.from = from;
            this.to = to;
        }

        private void assertRange() {
            if (ASSERTS) {
                assert from <= l.size();
                assert to <= l.size();
                assert to >= from;
            }
        }

        public boolean add(final int k) {
            l.add(to, k);
            to++;
            if (ASSERTS)
                assertRange();
            return true;
        }

        public void add(final int index, final int k) {
            ensureIndex(index);
            l.add(from + index, k);
            to++;
            if (ASSERTS)
                assertRange();
        }

        public boolean addAll(final int index,
            final Collection<? extends Integer> c) {
            ensureIndex(index);
            to += c.size();
            if (ASSERTS) {
                boolean retVal = l.addAll(from + index, c);
                assertRange();
                return retVal;
            }
            return l.addAll(from + index, c);
        }

        public int getInt(int index) {
            ensureRestrictedIndex(index);
            return l.getInt(from + index);
        }

        public int removeInt(int index) {
            ensureRestrictedIndex(index);
            to--;
            return l.removeInt(from + index);
        }

        public int set(int index, int k) {
            ensureRestrictedIndex(index);
            return l.set(from + index, k);
        }

        public void clear() {
            removeElements(0, size());
            if (ASSERTS)
                assertRange();
        }

        public int size() {
            return to - from;
        }

        public void size(final int size) {
            int i = size();
            if (size > i)
                while (i++ < size)
                    add((0));
            else
                while (i-- != size)
                    remove(i);
        }

        public void getElements(final int from, final int[] a,
            final int offset, final int length) {
            ensureIndex(from);
            if (from + length > size())
                throw new IndexOutOfBoundsException("End index (" + from
                    + length + ") is greater than list size (" + size()
                    + ")");
            l.getElements(this.from + from, a, offset, length);
        }

        public void removeElements(final int from, final int to) {
            ensureIndex(from);
            ensureIndex(to);
            l.removeElements(this.from + from, this.from + to);
            this.to -= (to - from);
            if (ASSERTS)
                assertRange();
        }

        public void addElements(int index, final int a[], int offset, int length) {
            ensureIndex(index);
            l.addElements(this.from + index, a, offset, length);
            this.to += length;
            if (ASSERTS)
                assertRange();
        }

        public int lastIndexOf(final int k) {
            IntListIterator i = listIterator(size());
            int e;
            while (i.hasPrevious()) {
                e = i.previousInt();
                if (((k) == (e)))
                    return i.nextIndex();
            }
            return -1;
        }


        public int indexOf(final int k) {
            final IntListIterator i = listIterator();
            int e;
            while (i.hasNext()) {
                e = i.nextInt();
                if (((k) == (e)))
                    return i.previousIndex();
            }
            return -1;
        }

        public IntListIterator listIterator(final int index) {
            ensureIndex(index);

            return new AbstractIntListIterator() {
                int pos = index, last = -1;

                public boolean hasNext() {
                    return pos < size();
                }
                public boolean hasPrevious() {
                    return pos > 0;
                }
                public int nextInt() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    return l.getInt(from + (last = pos++));
                }
                public int previousInt() {
                    if (!hasPrevious())
                        throw new NoSuchElementException();
                    return l.getInt(from + (last = --pos));
                }
                public int nextIndex() {
                    return pos;
                }
                public int previousIndex() {
                    return pos - 1;
                }
                public void add(int k) {
                    if (last == -1)
                        throw new IllegalStateException();
                    IntSubList.this.add(pos++, k);
                    last = -1;
                    if (ASSERTS)
                        assertRange();
                }
                public void set(int k) {
                    if (last == -1)
                        throw new IllegalStateException();
                    IntSubList.this.set(last, k);
                }
                public void remove() {
                    if (last == -1)
                        throw new IllegalStateException();
                    IntSubList.this.removeInt(last);
					/*
					 * If the last operation was a next(), we are removing an
					 * element *before* us, and we must decrease pos
					 * correspondingly.
					 */
                    if (last < pos)
                        pos--;
                    last = -1;
                    if (ASSERTS)
                        assertRange();
                }
            };
        }

        public IntList subList(final int from, final int to) {
            ensureIndex(from);
            ensureIndex(to);
            if (from > to)
                throw new IllegalArgumentException("Start index (" + from
                    + ") is greater than end index (" + to + ")");

            return new IntSubList(this, from, to);
        }

        public boolean rem(int k) {
            int index = indexOf(k);
            if (index == -1)
                return false;
            to--;
            l.removeInt(from + index);
            if (ASSERTS)
                assertRange();
            return true;
        }

        public boolean remove(final Object o) {
            return rem(((((Integer) (o)).intValue())));
        }

        public boolean addAll(final int index, final IntCollection c) {
            ensureIndex(index);
            to += c.size();
            if (ASSERTS) {
                boolean retVal = l.addAll(from + index, c);
                assertRange();
                return retVal;
            }
            return l.addAll(from + index, c);
        }

        public boolean addAll(final int index, final IntList l) {
            ensureIndex(index);
            to += l.size();
            if (ASSERTS) {
                boolean retVal = this.l.addAll(from + index, l);
                assertRange();
                return retVal;
            }
            return this.l.addAll(from + index, l);
        }

        /**
         * Ensures that the given index is nonnegative and not greater than the list
         * size.
         *
         * @param index
         *            an index.
         * @throws IndexOutOfBoundsException
         *             if the given index is negative or greater than the list size.
         */
        protected void ensureIndex(final int index) {
            if (index < 0)
                throw new IndexOutOfBoundsException("Index (" + index
                    + ") is negative");
            if (index > size())
                throw new IndexOutOfBoundsException("Index (" + index
                    + ") is greater than list size (" + (size()) + ")");
        }

        /**
         * Ensures that the given index is nonnegative and smaller than the list
         * size.
         *
         * @param index
         *            an index.
         * @throws IndexOutOfBoundsException
         *             if the given index is negative or not smaller than the list
         *             size.
         */
        protected void ensureRestrictedIndex(final int index) {
            if (index < 0)
                throw new IndexOutOfBoundsException("Index (" + index
                    + ") is negative");
            if (index >= size())
                throw new IndexOutOfBoundsException("Index (" + index
                    + ") is greater than or equal to list size (" + (size())
                    + ")");
        }

        /** Delegates to a more generic method. */
        public boolean addAll(final Collection<? extends Integer> c) {
            return addAll(size(), c);
        }

        /** Delegates to the new covariantly stronger generic method. */

        @Deprecated
        public IntListIterator intListIterator() {
            return listIterator();
        }

        /** Delegates to the new covariantly stronger generic method. */

        @Deprecated
        public IntListIterator intListIterator(final int index) {
            return listIterator(index);
        }

        public IntListIterator iterator() {
            return listIterator();
        }

        public IntListIterator listIterator() {
            return listIterator(0);
        }

        public boolean contains(final int k) {
            return indexOf(k) >= 0;
        }

        /** Delegates to the new covariantly stronger generic method. */

        @Deprecated
        public IntList intSubList(final int from, final int to) {
            return subList(from, to);
        }

        public void addElements(final int index, final int a[]) {
            addElements(index, a, 0, a.length);
        }

        private boolean valEquals(final Object a, final Object b) {
            return a == null ? b == null : a.equals(b);
        }

        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof List))
                return false;

            final List<?> l = (List<?>) o;
            int s = size();
            if (s != l.size())
                return false;

            if (l instanceof IntList) {
                final IntListIterator i1 = listIterator(), i2 = ((IntList) l)
                    .listIterator();
                while (s-- != 0)
                    if (i1.nextInt() != i2.nextInt())
                        return false;
                return true;
            }

            final ListIterator<?> i1 = listIterator(), i2 = l.listIterator();

            while (s-- != 0)
                if (!valEquals(i1.next(), i2.next()))
                    return false;

            return true;
        }

        /**
         * Compares this list to another object. If the argument is a
         * {@link List}, this method performs a lexicographical
         * comparison; otherwise, it throws a <code>ClassCastException</code>.
         *
         * @param l
         *            a list.
         * @return if the argument is a {@link List}, a negative integer,
         *         zero, or a positive integer as this list is lexicographically
         *         less than, equal to, or greater than the argument.
         * @throws ClassCastException
         *             if the argument is not a list.
         */

        public int compareTo(final List<? extends Integer> l) {
            if (l == this)
                return 0;

            if (l instanceof IntList) {

                final IntListIterator i1 = listIterator(), i2 = ((IntList) l)
                    .listIterator();
                int r;
                int e1, e2;

                while (i1.hasNext() && i2.hasNext()) {
                    e1 = i1.nextInt();
                    e2 = i2.nextInt();
                    if ((r = (Integer.compare((e1), (e2)))) != 0)
                        return r;
                }
                return i2.hasNext() ? -1 : (i1.hasNext() ? 1 : 0);
            }

            ListIterator<? extends Integer> i1 = listIterator(), i2 = l
                .listIterator();
            int r;

            while (i1.hasNext() && i2.hasNext()) {
                if ((r = ((Comparable<? super Integer>) i1.next()).compareTo(i2
                    .next())) != 0)
                    return r;
            }
            return i2.hasNext() ? -1 : (i1.hasNext() ? 1 : 0);
        }

        /**
         * Returns the hash code for this list, which is identical to
         * {@link List#hashCode()}.
         *
         * @return the hash code for this list.
         */
        public int hashCode() {
            IntIterator i = iterator();
            int h = 1, s = size();
            while (s-- != 0) {
                int k = i.nextInt();
                h = 31 * h + (k);
            }
            return h;
        }

        public void push(int o) {
            add(o);
        }

        public int popInt() {
            if (isEmpty())
                throw new NoSuchElementException();
            return removeInt(size() - 1);
        }

        public int topInt() {
            if (isEmpty())
                throw new NoSuchElementException();
            return getInt(size() - 1);
        }

        public int peekInt(int i) {
            return getInt(size() - 1 - i);
        }

        public boolean addAll(final IntList l) {
            return addAll(size(), l);
        }

        /** Delegates to the corresponding type-specific method. */
        public void add(final int index, final Integer ok) {
            add(index, ok.intValue());
        }

        /**
         * Delegates to the corresponding type-specific method.
         *
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        public Integer set(final int index, final Integer ok) {
            return (Integer.valueOf(set(index, ok.intValue())));
        }

        /**
         * Delegates to the corresponding type-specific method.
         *
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        public Integer get(final int index) {
            return (Integer.valueOf(getInt(index)));
        }

        /** Delegates to the corresponding type-specific method. */
        public int indexOf(final Object ok) {
            return indexOf(((((Integer) (ok)).intValue())));
        }

        /** Delegates to the corresponding type-specific method. */
        public int lastIndexOf(final Object ok) {
            return lastIndexOf(((((Integer) (ok)).intValue())));
        }

        /**
         * Delegates to the corresponding type-specific method.
         *
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        public Integer remove(final int index) {
            return (Integer.valueOf(removeInt(index)));
        }

        /** Delegates to the corresponding type-specific method. */
        public void push(Integer o) {
            push(o.intValue());
        }

        /**
         * Delegates to the corresponding type-specific method.
         *
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        public Integer pop() {
            return Integer.valueOf(popInt());
        }

        /**
         * Delegates to the corresponding type-specific method.
         *
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        public Integer top() {
            return Integer.valueOf(topInt());
        }

        /**
         * Delegates to the corresponding type-specific method.
         *
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        public Integer peek(int i) {
            return Integer.valueOf(peekInt(i));
        }

        public String toString() {
            final StringBuilder s = new StringBuilder();
            final IntIterator i = iterator();
            int n = size();
            int k;
            boolean first = true;

            s.append("[");

            while (n-- != 0) {
                if (first)
                    first = false;
                else
                    s.append(", ");
                k = i.nextInt();

                s.append(String.valueOf(k));
            }

            s.append("]");
            return s.toString();
        }
    }

}

interface ObjectSet<K> extends Set<K>, Collection<K>, Iterable<K> {
    /**
     * Returns a type-specific iterator on the elements of this set.
     *
     * <p>
     * Note that this specification strengthens the one given in
     * {@link java.lang.Iterable#iterator()}, which was already strengthened in
     * the corresponding type-specific class, but was weakened by the fact that
     * this interface extends {@link Set}.
     *
     * @return a type-specific iterator on the elements of this set.
     */
    ObjectIterator<K> iterator();

    /**
     * Removes an element from this set.
     *
     * <p>
     * Note that the corresponding method of the type-specific collection is
     * <code>rem()</code>. This unfortunate situation is caused by the clash
     * with the similarly named index-based method in the {@link java.util.List}
     * interface.
     *
     * @see java.util.Collection#remove(Object)
     */
    public boolean remove(Object k);

    /**
     * Returns a type-specific iterator on this elements of this collection.
     *
     * @see #iterator()
     * @deprecated As of <code>fastutil</code> 5, replaced by
     *             {@link #iterator()}.
     */
    @Deprecated
    ObjectIterator<K> objectIterator();

    /**
     * Returns an containing the items of this collection; the runtime type of
     * the returned array is that of the specified array.
     *
     * <p>
     * <strong>Warning</strong>: Note that, contrarily to
     * {@link Collection#toArray(Object[])}, this methods just writes all
     * elements of this collection: no special value will be added after the
     * last one.
     *
     * @param a
     *            if this array is big enough, it will be used to store this
     *            collection.
     * @return a primitive type array containing the items of this collection.
     * @see Collection#toArray(Object[])
     */
    <T> T[] toArray(T[] a);
}

interface ObjectIterator<K> extends Iterator<K> {
    /**
     * Skips the given number of elements.
     *
     * <P>
     * The effect of this call is exactly the same as that of calling
     * {@link #next()} for <code>n</code> times (possibly stopping if
     * {@link #hasNext()} becomes false).
     *
     * @param n
     *            the number of elements to skip.
     * @return the number of elements actually skipped.
     * @see Iterator#next()
     */

    int skip(int n);
}

abstract class AbstractIntCollection extends AbstractCollection<Integer>
    implements
    IntCollection {

    protected AbstractIntCollection() {
    }

    public int[] toArray(int a[]) {
        return toIntArray(a);
    }

    public int[] toIntArray() {
        return toIntArray(null);
    }

    public int[] toIntArray(int a[]) {
        if (a == null || a.length < size())
            a = new int[size()];
       unwrap(iterator(), a);
        return a;
    }

    public static int unwrap(final IntIterator i, final int array[]) {
        return unwrap(i, array, 0, array.length);
    }

    public static int unwrap(final IntIterator i, final int array[],
        int offset, final int max) {
        if (max < 0)
            throw new IllegalArgumentException(
                "The maximum number of elements (" + max + ") is negative");
        if (offset < 0 || offset + max > array.length)
            throw new IllegalArgumentException();
        int j = max;
        while (j-- != 0 && i.hasNext())
            array[offset++] = i.nextInt();
        return max - j - 1;
    }

    /**
     * Adds all elements of the given type-specific collection to this
     * collection.
     *
     * @param c
     *            a type-specific collection.
     * @return <code>true</code> if this collection changed as a result of the
     *         call.
     */

    public boolean addAll(IntCollection c) {
        boolean retVal = false;
        final IntIterator i = c.iterator();
        int n = c.size();

        while (n-- != 0)
            if (add(i.nextInt()))
                retVal = true;
        return retVal;
    }

    /**
     * Checks whether this collection contains all elements from the given
     * type-specific collection.
     *
     * @param c
     *            a type-specific collection.
     * @return <code>true</code> if this collection contains all elements of the
     *         argument.
     */

    public boolean containsAll(IntCollection c) {
        final IntIterator i = c.iterator();
        int n = c.size();

        while (n-- != 0)
            if (!contains(i.nextInt()))
                return false;

        return true;
    }

    /**
     * Retains in this collection only elements from the given type-specific
     * collection.
     *
     * @param c
     *            a type-specific collection.
     * @return <code>true</code> if this collection changed as a result of the
     *         call.
     */

    public boolean retainAll(IntCollection c) {
        boolean retVal = false;
        int n = size();

        final IntIterator i = iterator();

        while (n-- != 0) {
            if (!c.contains(i.nextInt())) {
                i.remove();
                retVal = true;
            }
        }

        return retVal;
    }

    /**
     * Remove from this collection all elements in the given type-specific
     * collection.
     *
     * @param c
     *            a type-specific collection.
     * @return <code>true</code> if this collection changed as a result of the
     *         call.
     */

    public boolean removeAll(IntCollection c) {
        boolean retVal = false;
        int n = c.size();

        final IntIterator i = c.iterator();

        while (n-- != 0)
            if (rem(i.nextInt()))
                retVal = true;

        return retVal;
    }

    public Object[] toArray() {
        final Object[] a = new Object[size()];
        unwrap(iterator(), a);
        return a;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final int size = size();
        if (a.length < size)
            a = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                .getComponentType(), size);
        unwrap(iterator(), a);
        if (size < a.length)
            a[size] = null;
        return a;
    }

    /**
     * Unwraps an iterator into an array starting at a given offset for a given
     * number of elements.
     *
     * <P>
     * This method iterates over the given type-specific iterator and stores the
     * elements returned, up to a maximum of <code>length</code>, in the given
     * array starting at <code>offset</code>. The number of actually unwrapped
     * elements is returned (it may be less than <code>max</code> if the
     * iterator emits less than <code>max</code> elements).
     *
     * @param i
     *            a type-specific iterator.
     * @param array
     *            an array to contain the output of the iterator.
     * @param offset
     *            the first element of the array to be returned.
     * @param max
     *            the maximum number of elements to unwrap.
     * @return the number of elements unwrapped.
     */
    public static <K> int unwrap(final Iterator<? extends K> i,
        final K array[], int offset, final int max) {
        if (max < 0)
            throw new IllegalArgumentException(
                "The maximum number of elements (" + max + ") is negative");
        if (offset < 0 || offset + max > array.length)
            throw new IllegalArgumentException();
        int j = max;
        while (j-- != 0 && i.hasNext())
            array[offset++] = i.next();
        return max - j - 1;
    }

    /**
     * Unwraps an iterator into an array.
     *
     * <P>
     * This method iterates over the given type-specific iterator and stores the
     * elements returned in the given array. The iteration will stop when the
     * iterator has no more elements or when the end of the array has been
     * reached.
     *
     * @param i
     *            a type-specific iterator.
     * @param array
     *            an array to contain the output of the iterator.
     * @return the number of elements unwrapped.
     */
    public static <K> int unwrap(final Iterator<? extends K> i, final K array[]) {
        return unwrap(i, array, 0, array.length);
    }

    /**
     * Adds all elements of the given collection to this collection.
     *
     * @param c
     *            a collection.
     * @return <code>true</code> if this collection changed as a result of the
     *         call.
     */

    public boolean addAll(Collection<? extends Integer> c) {
        boolean retVal = false;
        final Iterator<? extends Integer> i = c.iterator();
        int n = c.size();

        while (n-- != 0)
            if (add(i.next()))
                retVal = true;
        return retVal;
    }

    public boolean add(int k) {
        throw new UnsupportedOperationException();
    }

    /** Delegates to the new covariantly stronger generic method. */

    @Deprecated
    public IntIterator intIterator() {
        return iterator();
    }

    public abstract IntIterator iterator();

    /** Delegates to the type-specific <code>rem()</code> method. */
    public boolean remove(Object ok) {
        if (ok == null)
            return false;
        return rem(((((Integer) (ok)).intValue())));
    }

    /** Delegates to the corresponding type-specific method. */
    public boolean add(final Integer o) {
        return add(o.intValue());
    }

    /** Delegates to the corresponding type-specific method. */
    public boolean rem(final Object o) {
        if (o == null)
            return false;
        return rem(((((Integer) (o)).intValue())));
    }

    /** Delegates to the corresponding type-specific method. */
    public boolean contains(final Object o) {
        if (o == null)
            return false;
        return contains(((((Integer) (o)).intValue())));
    }

    public boolean contains(final int k) {
        final IntIterator iterator = iterator();
        while (iterator.hasNext())
            if (k == iterator.nextInt())
                return true;
        return false;
    }

    public boolean rem(final int k) {
        final IntIterator iterator = iterator();
        while (iterator.hasNext())
            if (k == iterator.nextInt()) {
                iterator.remove();
                return true;
            }
        return false;
    }

    /**
     * Checks whether this collection contains all elements from the given
     * collection.
     *
     * @param c
     *            a collection.
     * @return <code>true</code> if this collection contains all elements of the
     *         argument.
     */

    public boolean containsAll(Collection<?> c) {
        int n = c.size();

        final Iterator<?> i = c.iterator();
        while (n-- != 0)
            if (!contains(i.next()))
                return false;

        return true;
    }

    /**
     * Retains in this collection only elements from the given collection.
     *
     * @param c
     *            a collection.
     * @return <code>true</code> if this collection changed as a result of the
     *         call.
     */

    public boolean retainAll(Collection<?> c) {
        boolean retVal = false;
        int n = size();

        final Iterator<?> i = iterator();
        while (n-- != 0) {
            if (!c.contains(i.next())) {
                i.remove();
                retVal = true;
            }
        }

        return retVal;
    }

    /**
     * Remove from this collection all elements in the given collection. If the
     * collection is an instance of this class, it uses faster iterators.
     *
     * @param c
     *            a collection.
     * @return <code>true</code> if this collection changed as a result of the
     *         call.
     */

    public boolean removeAll(Collection<?> c) {
        boolean retVal = false;
        int n = c.size();

        final Iterator<?> i = c.iterator();
        while (n-- != 0)
            if (remove(i.next()))
                retVal = true;

        return retVal;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public String toString() {
        final StringBuilder s = new StringBuilder();
        final IntIterator i = iterator();
        int n = size();
        int k;
        boolean first = true;

        s.append("{");

        while (n-- != 0) {
            if (first)
                first = false;
            else
                s.append(", ");
            k = i.nextInt();

            s.append(String.valueOf(k));
        }

        s.append("}");
        return s.toString();
    }
}

interface IntStack {


    void push(int k);


    int popInt();

    int topInt();


    int peekInt(int i);

    /** Pushes the given object on the stack.
     *
     * @param o the object that will become the new top of the stack.
     */

    void push( Integer o );

    /** Pops the top off the stack.
     *
     * @return the top of the stack.
     * @throws NoSuchElementException if the stack is empty.
     */

    Integer pop();

    /** Checks whether the stack is empty.
     *
     * @return true if the stack is empty.
     */

    boolean isEmpty();

    /** Peeks at the top of the stack (optional operation).
     *
     * @return the top of the stack.
     * @throws NoSuchElementException if the stack is empty.
     */

    Integer top();

    /** Peeks at an element on the stack (optional operation).
     *
     * @param i an index from the stop of the stack (0 represents the top).
     * @return the <code>i</code>-th element on the stack.
     * @throws IndexOutOfBoundsException if the designated element does not exist..
     */

    Integer peek( int i );
}


interface IntListIterator
    extends
    ListIterator<Integer>,
    IntIterator, ObjectIterator<Integer>, Iterator<Integer> {

    void set(int k);
    void add(int k);

    /**
     * Returns the previous element as a primitive type.
     *
     * @return the previous element in the iteration.
     * @see ListIterator#previous()
     */

    int previousInt();

    /**
     * Moves back for the given number of elements.
     *
     * <P>
     * The effect of this call is exactly the same as that of calling
     * {@link #previous()} for <code>n</code> times (possibly stopping if
     * {@link #hasPrevious()} becomes false).
     *
     * @param n
     *            the number of elements to skip back.
     * @return the number of elements actually skipped.
     * @see Iterator#next()
     */

    int back(int n);

    /** Returns the previous element from the collection.
     *
     * @return the previous element from the collection.
     * @see ListIterator#previous()
     */

    Integer previous();

    /** Returns whether there is a previous element.
     *
     * @return whether there is a previous element.
     * @see ListIterator#hasPrevious()
     */

    boolean hasPrevious();
}

interface IntList
    extends
    List<Integer>,
    Comparable<List<? extends Integer>>,
    IntCollection {
    /**
     * Returns a type-specific iterator on the elements of this list (in proper
     * sequence).
     *
     * Note that this specification strengthens the one given in
     * {@link List#iterator()}. It would not be normally necessary, but
     * {@link java.lang.Iterable#iterator()} is bizarrily re-specified in
     * {@link List}.
     *
     * @return an iterator on the elements of this list (in proper sequence).
     */
    IntListIterator iterator();

    /**
     * Returns a type-specific list iterator on the list.
     *
     * @see #listIterator()
     * @deprecated As of <code>fastutil</code> 5, replaced by
     *             {@link #listIterator()}.
     */
    @Deprecated
    IntListIterator intListIterator();

    /**
     * Returns a type-specific list iterator on the list starting at a given
     * index.
     *
     * @see #listIterator(int)
     * @deprecated As of <code>fastutil</code> 5, replaced by
     *             {@link #listIterator(int)}.
     */
    @Deprecated
    IntListIterator intListIterator(int index);

    /**
     * Returns a type-specific list iterator on the list.
     *
     * @see List#listIterator()
     */
    IntListIterator listIterator();

    /**
     * Returns a type-specific list iterator on the list starting at a given
     * index.
     *
     * @see List#listIterator(int)
     */
    IntListIterator listIterator(int index);

    /**
     * Returns a type-specific view of the portion of this list from the index
     * <code>from</code>, inclusive, to the index <code>to</code>, exclusive.
     *
     * @see List#subList(int,int)
     * @deprecated As of <code>fastutil</code> 5, replaced by
     *             {@link #subList(int,int)}.
     */
    @Deprecated
    IntList intSubList(int from, int to);

    /**
     * Returns a type-specific view of the portion of this list from the index
     * <code>from</code>, inclusive, to the index <code>to</code>, exclusive.
     *
     * <P>
     * Note that this specification strengthens the one given in
     * {@link List#subList(int,int)}.
     *
     * @see List#subList(int,int)
     */
    IntList subList(int from, int to);

    /**
     * Sets the size of this list.
     *
     * <P>
     * If the specified size is smaller than the current size, the last elements
     * are discarded. Otherwise, they are filled with 0/<code>null</code>/
     * <code>false</code>.
     *
     * @param size
     *            the new size.
     */

    void size(int size);

    /**
     * Copies (hopefully quickly) elements of this type-specific list into the
     * given array.
     *
     * @param from
     *            the start index (inclusive).
     * @param a
     *            the destination array.
     * @param offset
     *            the offset into the destination array where to store the first
     *            element copied.
     * @param length
     *            the number of elements to be copied.
     */
    void getElements(int from, int a[], int offset, int length);

    /**
     * Removes (hopefully quickly) elements of this type-specific list.
     *
     * @param from
     *            the start index (inclusive).
     * @param to
     *            the end index (exclusive).
     */
    void removeElements(int from, int to);

    /**
     * Add (hopefully quickly) elements to this type-specific list.
     *
     * @param index
     *            the index at which to add elements.
     * @param a
     *            the array containing the elements.
     */
    void addElements(int index, int a[]);

    /**
     * Add (hopefully quickly) elements to this type-specific list.
     *
     * @param index
     *            the index at which to add elements.
     * @param a
     *            the array containing the elements.
     * @param offset
     *            the offset of the first element to add.
     * @param length
     *            the number of elements to add.
     */
    void addElements(int index, int a[], int offset, int length);

    /**
     * @see List#add(Object)
     */
    boolean add(int key);

    /**
     * @see List#add(int,Object)
     */
    void add(int index, int key);

    /**
     * @see List#add(int,Object)
     */
    boolean addAll(int index, IntCollection c);

    /**
     * @see List#add(int,Object)
     */
    boolean addAll(int index, IntList c);

    /**
     * @see List#add(int,Object)
     */
    boolean addAll(IntList c);

    /**
     * @see List#get(int)
     */
    int getInt(int index);

    /**
     * @see List#indexOf(Object)
     */
    int indexOf(int k);

    /**
     * @see List#lastIndexOf(Object)
     */
    int lastIndexOf(int k);

    /**
     * @see List#remove(int)
     */
    int removeInt(int index);

    /**
     * @see List#set(int,Object)
     */
    int set(int index, int k);

}

abstract class AbstractIntListIterator
    implements IntListIterator, IntIterator, ObjectIterator<Integer>, Iterator<Integer> {

    protected AbstractIntListIterator() {
    }

    /** Delegates to the corresponding type-specific method. */
    public void set(Integer ok) {
        set(ok.intValue());
    }
    /** Delegates to the corresponding type-specific method. */
    public void add(Integer ok) {
        add(ok.intValue());
    }

    /** This method just throws an {@link UnsupportedOperationException}. */
    public void set(int k) {
        throw new UnsupportedOperationException();
    }
    /** This method just throws an {@link UnsupportedOperationException}. */
    public void add(int k) {
        throw new UnsupportedOperationException();
    }

    /** Delegates to the corresponding generic method. */
    public int previousInt() {
        return previous().intValue();
    }

    /** Delegates to the corresponding type-specific method. */
    public Integer previous() {
        return Integer.valueOf(previousInt());
    }

    /**
     * This method just iterates the type-specific version of
     * {@link #previous()} for at most <code>n</code> times, stopping if
     * {@link #hasPrevious()} becomes false.
     */
    public int back(final int n) {
        int i = n;
        while (i-- != 0 && hasPrevious())
            previousInt();
        return n - i - 1;
    }

    /**
     * Delegates to the corresponding type-specific method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    public Integer next() {
        return Integer.valueOf(nextInt());
    }

    /**
     * This method just iterates the type-specific version of {@link #next()}
     * for at most <code>n</code> times, stopping if {@link #hasNext()} becomes
     * false.
     */

    public int skip(final int n) {
        int i = n;
        while (i-- != 0 && hasNext())
            nextInt();
        return n - i - 1;
    }

    /** Returns whether there is a previous element.
     *
     * @return whether there is a previous element.
     * @see ListIterator#hasPrevious()
     */

    public abstract boolean hasPrevious();
}

