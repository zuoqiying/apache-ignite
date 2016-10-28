package org.apache.ignite.internal.processors.hadoop.shuffle.collections;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import org.apache.hadoop.io.IntWritable;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.hadoop.HadoopJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInput;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskOutput;
import org.apache.ignite.internal.processors.hadoop.shuffle.HadoopShuffleJob;
import org.apache.ignite.internal.processors.hadoop.shuffle.HadoopShuffleMessage;
import org.apache.ignite.internal.util.GridUnsafe;
import org.apache.ignite.internal.util.IgniteUtils;
import org.apache.ignite.internal.util.io.GridUnsafeDataInput;
import org.apache.ignite.internal.util.offheap.unsafe.GridUnsafeMemory;

/**
 *
 */
public class HadoopSpillableMultimap implements HadoopSpillable, HadoopMultimap {
    /** Should correspond to maximum length of key or value. */
    private static final int INITIAL_BUF_SIZE = 1024;

    /** */
    static final byte MARKER_KEY = (byte)17;

    /** */
    static final byte MARKER_VALUE = (byte)31;

    /** */
    private byte[] buf = new byte[INITIAL_BUF_SIZE];

    /** */
    private HadoopMultimap delegateMulimap;

    /** */
    private final HadoopTaskContext ctx;

    /** */
    private final HadoopJobInfo info;

    /** */
    private final GridUnsafeMemory mem;

    /** */
    public HadoopSpillableMultimap(HadoopTaskContext ctx, HadoopJobInfo info, GridUnsafeMemory mem) {
        this.ctx = ctx;
        this.info = info;
        this.mem = mem;

        delegateMulimap = createNew();
    }

    /** {@inheritDoc} */
    @Override public boolean accept(boolean ignoreLastVisited, Visitor v) throws IgniteCheckedException {
        return delegateMulimap.accept(true, v);
    }

    /** {@inheritDoc} */
    @Override public HadoopTaskOutput startAdding(HadoopTaskContext ctx) throws IgniteCheckedException {
        return delegateMulimap.startAdding(ctx);
    }

    /** {@inheritDoc} */
    @Override public HadoopTaskInput input(HadoopTaskContext taskCtx) throws IgniteCheckedException {
        return delegateMulimap.input(taskCtx);
    }

    /** {@inheritDoc} */
    @Override public HadoopTaskInput rawInput() throws IgniteCheckedException {
        return delegateMulimap.rawInput();
    }

    /** {@inheritDoc} */
    @Override public void close() {
        buf = null;

        delegateMulimap.close();
    }

    /** {@inheritDoc} */
    @Override public long spill(final DataOutput dout) throws IgniteCheckedException {
        // TODO: since now we have method HadoopMultimap.rawInput(), we can read
        // TODO: the multimap content with #rawInput(), without the visitor.
//        try {
            final long[] cnt = new long[] { 0 };

            HadoopMultimap.Visitor visitor = new HadoopMultimap.Visitor() {
                /** */
                private boolean keyLast;

                /** {@inheritDoc} */
                @Override public void visitKey(long keyPtr, int keySize) throws IgniteCheckedException {
                    assert !keyLast;

                    cnt[0] += addKey(dout, keyPtr, keySize);

                    keyLast = true;
                }

                /** {@inheritDoc} */
                @Override public void visitValue(long valPtr, int valSize) throws IgniteCheckedException {
                    cnt[0] += addValue(dout, valPtr, valSize);

                    keyLast = false;
                }
            };

            delegateMulimap.accept(true, visitor);

            delegateMulimap.close();

            delegateMulimap = null;

            return cnt[0];
//        }
//        finally {
//            IgniteUtils.closeQuiet((Closeable)dout);
//        }
    }

    /** */
    protected HadoopMultimap createNew() {
        return new HadoopSkipList(info, mem);
    }

    /** {@inheritDoc} */
    @Override public long unSpill(DataInput din) throws IgniteCheckedException {
        if (delegateMulimap != null)
            delegateMulimap.close();

        delegateMulimap = createNew();

        try {
            // Read the values to the delegate multimap
            try (HadoopMultimap.Adder adder = (HadoopMultimap.Adder)delegateMulimap.startAdding(ctx)) {
                try {
                    long r = acceptToRead(din, new HadoopShuffleMessage.Visitor() {
                        /** */
                        private final GridUnsafeDataInput keyInput = new GridUnsafeDataInput();

                        /** */
                        private final HadoopShuffleJob.UnsafeValue val = new HadoopShuffleJob.UnsafeValue();

                        /** */
                        private HadoopMultimap.Key key;

                        @Override public void visitKey(byte[] buf, int off, int len) throws IgniteCheckedException {
                            keyInput.bytes(buf, off, len); // TODO: changed (buf, off, off + len) -> (buf, off, len) ? make sure this is correct.

                            // TODO: debug only:
                            try {
                                IntWritable iw = new IntWritable();
                                iw.readFields(new DataInputStream(new ByteArrayInputStream(Arrays.copyOfRange(buf,
                                    off, len))));
                                System.out.println("UnSpill:   Key: " + iw.get());
                            }
                            catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                            key = adder.addKey(keyInput, key);
                        }

                        @Override public void visitValue(byte[] buf, int off, int len) {
                            val.setBuf(buf);
                            val.setOff(off);
                            val.setSize(len);

                            // TODO: debug only:
                            try {
                                IntWritable iw = new IntWritable();
                                iw.readFields(new DataInputStream(new ByteArrayInputStream(Arrays.copyOfRange(buf,
                                    off, len))));

                                System.out.println("UnSpill: val: " + iw.get());
                            }
                            catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                            key.add(val);
                        }
                    });

                    return r;
                }
                finally {
                    IgniteUtils.closeQuiet((Closeable)din);
                }
            }
        }
        catch (IOException ioe) {
            throw new IgniteCheckedException(ioe);
        }
    }

    /**
     * Reading from file functionality.
     * @param v Visitor.
     */
    public long acceptToRead(DataInput din, HadoopShuffleMessage.Visitor v) throws IgniteCheckedException, IOException {
        long read = 0;

        byte[] bb = new byte[INITIAL_BUF_SIZE];

        try {
            byte marker;
            int size;

            while (true) {
                marker = din.readByte(); // read marker byte (key or value);

                size = din.readInt(); // read or just skip these bytes;

                bb = ensureLength(bb, size);

                din.readFully(bb, 0, size);

                read += size + 5;

                if (marker == MARKER_VALUE)
                    v.visitValue(bb, 0, size);
                else if (marker == MARKER_KEY)
                    v.visitKey(bb, 0, size);
                else
                    throw new IllegalStateException("Unknown marker: " + marker);
            }
        }
        catch (EOFException eofe) {
            // noop
        }

        return read;
    }

    public static byte[] ensureLength(byte[] buf, long requiredSize) {
        // NB: 0 is possible value.
        assert requiredSize >= 0;

        if (requiredSize > Integer.MAX_VALUE)
            throw new IgniteException("Required size out of int size: " + requiredSize);

        if (buf == null)
            buf = new byte[(int)requiredSize];
        else if (requiredSize > buf.length) {
            int newSize = Math.max((int)requiredSize, buf.length * 2);

            buf = new byte[newSize]; // increase buffer if needed.
        }

        return buf;
    }

    /** */
    private int addKey(DataOutput dout, long ptr, int size) throws IgniteCheckedException {
        // TODO: debug only:
        System.out.println("Spill: Key: " + mem.readInt(ptr) );

        return add(dout, MARKER_KEY, ptr, size);
    }

    /** */
    private int addValue(DataOutput dout, long ptr, int size) throws IgniteCheckedException {
        // TODO: debug only:
        System.out.println("Spill: Val: " + mem.readInt(ptr) );

        return add(dout, MARKER_VALUE, ptr, size);
    }

    /**
     * @param marker Marker.
     * @param ptr Pointer.
     * @param size Size.
     * @return how many bytes written.
     */
    private int add(DataOutput dout, byte marker, long ptr, int size) throws IgniteCheckedException {
//        int off = 0;
//
//        buf = ensureLength(buf, size + 1 + 4);
//
//        buf[off++] = marker;
//
//        GridUnsafe.putInt(buf, GridUnsafe.BYTE_ARR_OFF + off, size);
//
//        off += 4;
//
//        GridUnsafe.copyMemory(null, ptr, buf, GridUnsafe.BYTE_ARR_OFF + off, size);
//
//        off += size;
//
//        try {
//            dout.write(buf, 0, off);
//
//            return off;
//        }
//        catch (IOException ioe) {
//            throw new IgniteCheckedException(ioe);
//        }
        try {
            dout.writeByte(marker);

            dout.writeInt(size);

            buf = ensureLength(buf, size);

            GridUnsafe.copyMemory(null, ptr, buf, GridUnsafe.BYTE_ARR_OFF, size);

            dout.write(buf, 0, size);

            return size + 5;
        }
        catch (IOException ioe) {
            throw new IgniteCheckedException(ioe);
        }
    }

//    private DataOutput createOutputFile() throws IgniteCheckedException {
//        try {
//            return new DataOutputStream(new FileOutputStream(new File(composeFileName())));
//        }
//        catch (IOException ioe) {
//            throw new IgniteCheckedException(ioe);
//        }
//    }
//
//    private DataInput createInputFile() throws IgniteCheckedException {
//        try {
//            return new DataInputStream(new FileInputStream(new File(composeFileName())));
//        }
//        catch (IOException ioe) {
//            throw new IgniteCheckedException(ioe);
//        }
//    }
//
//    private String composeFileName() {
//        return "/tmp/foo";
//    }
}
