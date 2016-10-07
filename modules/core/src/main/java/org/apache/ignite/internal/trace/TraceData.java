package org.apache.ignite.internal.trace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Collected trace data.
 */
public class TraceData implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Group data. */
    private final Map<String, List<TraceThreadResult>> grps;

    /**
     * Constructor.
     *
     * @param grps Group mapping.
     */
    public TraceData(Map<String, List<TraceThreadResult>> grps) {
        this.grps = grps;
    }

    /**
     * Get available groups.
     *
     * @return Available groups.
     */
    public Collection<String> groups() {
        return grps.keySet();
    }

    /**
     * Get group data.
     *
     * @param grpName Group name.
     * @return Group data.
     */
    @SuppressWarnings("unchecked")
    public List<TraceThreadResult> groupData(String grpName) {
        List<TraceThreadResult> res = grps.get(grpName);

        if (res == null)
            return Collections.emptyList();
        else
            return res;
    }

    /**
     * Save trace data to file.
     *
     * @param file File.
     */
    public void save(File file) {
        try {
//            if (!file.exists()) {
//                if (!file.createNewFile())
//                    throw new RuntimeException("Failed to create file: " + file);
//            }

            try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                os.writeObject(this);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to save trace data to file: " + file, e);
        }
    }

    /**
     * Load trace data from file.
     *
     * @param file File.
     * @return Trace data.
     */
    public static TraceData load(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return (TraceData)ois.readObject();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load trace data from file: " + file, e);
        }
    }
}
