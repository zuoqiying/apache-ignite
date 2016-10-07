package org.apache.ignite.internal.trace.atomic;

import org.apache.ignite.internal.util.typedef.internal.U;

import java.io.File;

/**
 * Atomic trace utility methods.
 */
public class AtomicTraceUtils {
    /** Trace directory. */
    //private static final String TRACE_DIR = System.getProperty("TRACE_DIR");
    private static final String TRACE_DIR = "C:\\Personal\\atomic_trace";

    /**
     * Get trace directory.
     *
     * @return Trace directory.
     */
    @SuppressWarnings("ConstantConditions")
    public static File traceDir() {
        if (TRACE_DIR == null || TRACE_DIR.isEmpty())
            throw new RuntimeException("TRACE_DIR is not defined.");

        return new File(TRACE_DIR);
    }

    /**
     * Get trace file with the given index.
     *
     * @param idx Index.
     * @return Trace file.
     */
    public static File traceFile(int idx) {
        return new File(traceDir(), "trace-" + idx + ".log");
    }

    /**
     * Clear trace directory.
     *
     * @return Directory.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File prepareTraceDir() {
        File dir = traceDir();

        if (dir.exists()) {
            if (!U.delete(dir))
                throw new RuntimeException("Failed to clear trace dir: " + dir.getAbsolutePath());
        }

        dir.mkdirs();

        return dir;
    }

    /**
     * Private constructor.
     */
    private AtomicTraceUtils() {
        // No-op.
    }
}
