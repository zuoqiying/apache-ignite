package org.apache.ignite.examples.indexing;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.util.BitSet;
import java.util.concurrent.locks.Lock;

/**
 * <p>
 * The <code>EntityManager</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class EntityManager<V> {
    /** Segment capacity. */
    public static final int CAPACITY = 16_000; // Compressed size fits in 2k page.

    /** */
    private ThreadLocal<StringBuilder> builder = new ThreadLocal<StringBuilder>() {
        @Override protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };

    /** */
    protected final String name;

    /** */
    private String seqName;

    /** */
    protected final IgniteBiTuple<String, IgniteClosure<Object, String>>[] fields;

    /** */
    private Ignite ignite;

    /**
     * @param name   Name.
     * @param fields Fields.
     */
    @SafeVarargs
    public EntityManager(String name, IgniteBiTuple<String, IgniteClosure<Object, String>>... fields) {
        this.name = name;

        this.seqName = name + "_seq";

        this.fields = fields;
    }

    /**
     * Returns cache configurations.
     */
    public CacheConfiguration[] cacheConfigurations() {
        CacheConfiguration[] ccfgs = new CacheConfiguration[(fields == null ? 0 : fields.length) + 1];

        int c = 0;

        ccfgs[c] = new CacheConfiguration(entityCacheName());
        ccfgs[c].setCacheMode(CacheMode.REPLICATED);
        ccfgs[c].setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        c++;

        if (fields != null)
            for (IgniteBiTuple<String, IgniteClosure<Object, String>> field : fields) {
                String field1 = field.get1();
                ccfgs[c] = new CacheConfiguration(segmentCacheName(field1));
                ccfgs[c].setCacheMode(CacheMode.REPLICATED);
                ccfgs[c].setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

                c++;
            }

        return ccfgs;
    }

    /**
     * Attaches ignite instance to a manager.
     *
     * @param ignite Ignite.
     */
    public void attach(Ignite ignite) {
        this.ignite = ignite;
    }

    public IndexedFields indexedFields(long key, V t) {
        return null;
    }

    public V get(long key) {
        return (V) entityCache().get(key);
    }

    /**
     * Insert value with generated id, creating necessary indices.
     *
     * @param val Value.
     *
     *  @return Assigned id.
     */
    public long create(V val) {
        try(Transaction tx = ignite.transactions().txStart(TransactionConcurrency.PESSIMISTIC, TransactionIsolation.READ_COMMITTED)) {
            IgniteAtomicSequence seq = ignite.atomicSequence(seqName, 0, true);

            long key = seq.getAndIncrement();

            entityCache().put(key, val);

            IndexedFields indexedFields = indexedFields(key, val);

            addEntry(indexedFields);

            tx.commit();

            return key;
        }
    }

    public void update(long key, V val) {
        // TODO implement indices merging.
    }

    public boolean delete(long key) {
        V v = get(key);

        if (v == null)
            return false;

        IndexedFields indexedFields = indexedFields(key, v);

        if (indexedFields != null)
            removeEntry(indexedFields);

        entityCache().remove(key);

        return true;
    }

    protected void addEntry(IndexedFields indexedFields) {
        long seg = indexedFields.id() / CAPACITY;

        int off = (int) (indexedFields.id() % CAPACITY);

        for (Field field : indexedFields.fields()) {
            String k = segmentKey(field.value(), seg);

            Lock lock = segmentCache(field.name()).lock(k);
            try {
                lock.lock();

                BitSet set = segmentCache(field.name()).get(k);

                if (set == null)
                    set = new BitSet();

                set.set(off);

                segmentCache(field.name()).put(k, set);
            } finally {
                lock.unlock();
            }
        }
    }

    protected void removeEntry(IndexedFields indexedFields) {
        long seg = indexedFields.id() / CAPACITY;

        int off = (int) (indexedFields.id() % CAPACITY);

        for (Field field : indexedFields.fields()) {
            String k = segmentKey(field.value(), seg);

            BitSet set = segmentCache(field.name()).get(k);

            if (set == null)
                continue;

            if (set.cardinality() == 0)
                segmentCache(field.name()).remove(k);
            else {
                set.clear(off);

                segmentCache(field.name()).put(k, set);
            }
        }
    }

    /**
     * @param val Value.
     * @param seg Seg.
     */
    protected String segmentKey(String val, long seg) {
        StringBuilder builder = builder();

        builder.append(val);
        builder.append('\0');
        builder.append(seg);

        return builder.toString();
    }

    /**
     * @param field Field.
     * @param val Value.
     * @param id Id.
     */
    public boolean contains(String field, String val, long id) {
        long seg = id / CAPACITY;

        int off = (int) (id % CAPACITY);

        BitSet set = segmentCache(field).get(segmentKey(val, seg));

        return set != null && set.get(off);
    }

    /**
     * Returns all entity ids for indexed field.
     *
     * @param field Field.
     * @param val Value.
     */
    public Iterable<Long> ids(String field, String val) {
        // TODO FIXME can be efficiently implemented.
        return null;
    }

    /**
     * @param field Field.
     */
    private IgniteCache<String, BitSet> segmentCache(String field) {
        return ignite.getOrCreateCache(segmentCacheName(field));
    }

    /**
     * @param field Field.
     */
    private String segmentCacheName(String field) {
        return name + "_" + field;
    }

    /** */
    private IgniteCache<Long, V> entityCache() {
        return ignite.getOrCreateCache(entityCacheName());
    }

    /** */
    private String entityCacheName() {
        return name + "_entity";
    }

    /** */
    private StringBuilder builder() {
        StringBuilder builder = this.builder.get();

        builder.setLength(0);

        return builder;
    }

    /**
     * @param field Field.
     */
    public int indexSize(String field) {
        return segmentCache(field).size();
    }
}