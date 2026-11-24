package object;

import hdf.object.FileFormat;

/**
 * The body of the child JVM launched by {@link FileFormatInitOrderTest}.
 *
 * The test must run in a JVM where nothing has touched hdf.object yet, so that
 * hdf.object.h4.H4File is the first class of the hierarchy to be initialized and
 * FileFormat is initialized as its superclass step.
 *
 * Results go to stdout so the parent can tell a registration failure from an
 * environment that has no HDF libraries at all.
 */
public class H4FirstLoadProbe {
    /** The line printed when the probe completes. */
    public static final String RESULT_PREFIX = "HDF4_REGISTERED=";

    /** The line printed when the probe cannot run to completion. */
    public static final String ERROR_PREFIX = "PROBE_ERROR=";

    /** Exit status used when the probe itself failed. */
    public static final int EXIT_PROBE_ERROR = 2;

    /** Default constructor. */
    public H4FirstLoadProbe() {}

    /**
     * Initializes H4File first, then reports whether HDF4 ended up registered.
     *
     * @param args
     *            Ignored.
     */
    public static void main(String[] args)
    {
        try {
            // First touch of the hierarchy. Initializes H4File, which initializes FileFormat.
            Class.forName("hdf.object.h4.H4File");
        }
        catch (Throwable err) {
            System.out.println(ERROR_PREFIX + err);
            System.exit(EXIT_PROBE_ERROR);
        }

        boolean registered = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) != null;
        System.out.println(RESULT_PREFIX + registered);
        System.exit(registered ? 0 : 1);
    }
}
