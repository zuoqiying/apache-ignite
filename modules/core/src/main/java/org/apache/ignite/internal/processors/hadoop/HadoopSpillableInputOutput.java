//package org.apache.ignite.internal.processors.hadoop;
//
//import java.io.ByteArrayOutputStream;
//import java.io.DataInput;
//import java.io.DataInputStream;
//import java.io.DataOutput;
//import java.io.DataOutputStream;
//import java.io.EOFException;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.util.Iterator;
//import org.apache.ignite.IgniteCheckedException;
//import org.apache.ignite.internal.util.GridUnsafe;
//
///**
// * Contains a couple of {@link HadoopTaskInput} and {@link HadoopTaskOutput}.
// * Usage pattern is following:
// * 1) get HadoopTaskOutput;
// * 2) store values into it;
// * 3) spill() it to a file;
// * 4) read it back from the file with unSpill();
// */
//public class HadoopSpillableInputOutput implements AutoCloseable {
//
//    private final HadoopTaskInput in;
//    private final HadoopTaskOutput out;
//
//    private final HadoopSerialization keySer;
//    private final HadoopSerialization valSer;
//
//    private final String name;
//
//    private String composeFileName() {
//
//    }
//
////    public HadoopTaskInput getIn() {
////        return in;
////    }
////
////    public HadoopTaskOutput getOut() {
////        return out;
////    }
//
//    // TODO: problem: serialoizer should not be used there, write from unsafe or byte[] form.
////    /**
////     * Gets K-V pairs from {@link HadoopTaskInput}, and writes them to the output.
////     * Blocks until spill is done.
////     * Upon the return of this method the output is guaranteed to
////     * be able to receive new K-V pairs.
////     */
////    public void spill() throws IgniteCheckedException, IOException {
////        DataOutput out = getOutputFile();
////
////        Object key, val;
////        Iterator<?> valIt;
////
////        while (in.next()) {
////            key = in.key();
////
////            assert key != null;
////
////            writeWithSize(MARKER_KEY, key, keySer, out);
////
////            valIt = in.values();
////
////            while (valIt.hasNext()) {
////                val = valIt.next();
////
////                writeWithSize(MARKER_VALUE, val, valSer, out);
////            }
////        }
////    }
//
//    /**
//     * Writes an objects using provided serializer in form
//     * |-- marker (1) --|-- size (4) --|-- object --| .
//     *
//     * @param marker
//     * @param kOrV
//     * @param ser
//     * @param out
//     * @return
//     * @throws IOException
//     * @throws IgniteCheckedException
//     */
//    private int writeWithSize(byte marker, Object kOrV, HadoopSerialization ser, DataOutput out)
//            throws IOException, IgniteCheckedException {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_SIZE);
//
//        try (DataOutputStream dOut = new DataOutputStream(baos)) {
//            ser.write(dOut, kOrV);
//        }
//
//        byte[] bb = baos.toByteArray();
//
//        int size = bb.length;
//
//        // Now size is known, so write:
//        // |-- marker (1) --|-- size (4) --|-- object --| :
//        out.write(marker);
//        out.writeInt(size);
//        out.write(bb);
//
//        return size + 5;
//    }
//
//    private DataOutput getOutputFile() throws IgniteCheckedException {
//        try {
//           return new DataOutputStream(new FileOutputStream(new File(composeFileName())));
//        }
//        catch (IOException ioe) {
//           throw new IgniteCheckedException(ioe);
//        }
//    }
//
//    private DataInput getInputFile() throws IgniteCheckedException {
//        try {
//            return new DataInputStream(new FileInputStream(new File(composeFileName())));
//        }
//        catch (IOException ioe) {
//            throw new IgniteCheckedException(ioe);
//        }
//    }
//
//    /**
//     * Reads back from the disk.
//     */
//    public void unSpill() {
//
//    }
//
//    @Override public void close() throws IgniteCheckedException {
//        in.close();
//        out.close();
//    }
//}
