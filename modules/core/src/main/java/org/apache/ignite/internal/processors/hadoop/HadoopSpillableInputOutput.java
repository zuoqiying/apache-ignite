package org.apache.ignite.internal.processors.hadoop;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import org.apache.ignite.IgniteCheckedException;

/**
 * Contains a couple of {@link HadoopTaskInput} and {@link HadoopTaskOutput}.
 * Usage pattern is following:
 * 1) get HadoopTaskOutput;
 * 2) store values into it;
 * 3) spill() it to a file;
 * 4) read it back from the file with unSpill();
 */
public class HadoopSpillableInputOutput implements AutoCloseable {
    private final HadoopTaskInput in;
    private final HadoopTaskOutput out;

    private final HadoopSerialization keySer;
    private final HadoopSerialization valSer;

    private final String name;

    private DataOutput spillOutput;

    private String composeFileName() {

    }

    public HadoopTaskInput getIn() {
        return in;
    }

    public HadoopTaskOutput getOut() {
        return out;
    }

    /**
     * Blocks until spill is done.
     * Upon the return of this method the output is guaranteed to
     * be able to receive new K-V pairs.
     */
    public void spill() throws IgniteCheckedException {
        DataOutput out = getOutputFile();

        Object key, val;
        Iterator<?> valIt;

        // TODO: see HadoopShuffleMessage : reuse that implementation.
        while (in.next()) {
            key = in.key();

            assert key != null;

            keySer.write(out, key);

            valIt = in.values();

            while (valIt.hasNext()) {
                val = valIt.next();

                valSer.write(out, val);
            }
        }
    }

    private void spillKey(Object key) {
        // format is:
        //   data   |  length :
        //   marker |  1
        //   key length (N) |  4
        //   the key        |  N

        spillOutput.write(HadoopShuffleMesage.MARKER_KEY);
        spillOutput.
    }

    private void spillValue(Object value) {

    }

    private DataOutput getOutputFile() throws IgniteCheckedException {
       try {
           return new DataOutputStream(new FileOutputStream(new File(composeFileName())));
       }
       catch (IOException ioe) {
        throw new IgniteCheckedException(ioe);
       }
    }

    /**
     * Reads back from the disk.
     */
    public void unSpill() {

    }

    @Override public void close() throws IgniteCheckedException {
        in.close();
        out.close();
    }
}
