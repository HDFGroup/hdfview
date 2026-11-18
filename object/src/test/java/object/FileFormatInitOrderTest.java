package object;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The default file formats must all register regardless of which class of the FileFormat
 * hierarchy the JVM initializes first.
 *
 * That order is only observable in a JVM that has not yet touched hdf.object, and other
 * tests in this module load FileFormat long before this one runs, so the check is made in
 * a child JVM.
 */
@Tag("unit")
@Tag("fast")
public class FileFormatInitOrderTest {
    private static final long PROBE_TIMEOUT_SECONDS = 120;

    /** Default constructor. */
    public FileFormatInitOrderTest() {}

    /**
     * HDF4 must register even when H4File is the first class of the hierarchy to load.
     *
     * @throws Exception
     *             If the child JVM cannot be launched.
     */
    @Test
    public void testHDF4RegistersWhenH4FileLoadsFirst() throws Exception
    {
        ProbeResult result = runProbe();

        /*
         * Registering HDF4 needs no native library: H4File's static initializer only builds
         * a logger, and its no-argument constructor reads compile-time constants. A probe
         * that cannot answer therefore means the child JVM itself was set up wrong, which is
         * a failure rather than a reason to skip the check.
         */
        if (result.exitCode == H4FirstLoadProbe.EXIT_PROBE_ERROR)
            fail("Probe JVM could not run the check: " + result.output());

        String resultLine = result.resultLine();
        assertNotNull(resultLine, "Probe did not report a result: " + result.output());
        assertEquals(H4FirstLoadProbe.RESULT_PREFIX + "true", resultLine,
                     "HDF4 was not registered when H4File was loaded first: " + result.output());
    }

    private ProbeResult runProbe() throws Exception
    {
        List<String> command = new ArrayList<>();
        command.add(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));

        // Surefire supplies this from build.properties. The child needs it to load the
        // native libraries the format classes bind to.
        String libraryPath = System.getProperty("java.library.path");
        if (libraryPath != null && !libraryPath.isEmpty())
            command.add("-Djava.library.path=" + libraryPath);

        command.add(H4FirstLoadProbe.class.getName());

        // Merged into one stream, so that filling one pipe cannot block the child while
        // this side is reading the other.
        Process probe = new ProcessBuilder(command).redirectErrorStream(true).start();
        probe.getOutputStream().close();

        // Drained by another thread, so that a child that neither exits nor closes its
        // output cannot outlast the timeout below.
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(probe.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
            catch (IOException err) {
                return "<probe output could not be read: " + err + ">";
            }
        });

        if (!probe.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            probe.destroyForcibly();
            throw new IllegalStateException("Probe JVM did not exit within " + PROBE_TIMEOUT_SECONDS +
                                            " seconds");
        }

        return new ProbeResult(probe.exitValue(), output.get(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private static final class ProbeResult {
        private final int exitCode;
        private final String output;

        ProbeResult(int exitCode, String output)
        {
            this.exitCode = exitCode;
            this.output   = output;
        }

        /**
         * @return The line the probe reported its result on, or null if it reported none.
         *         The child's logging shares this stream, so the line is searched for
         *         rather than taken to be the whole of the output.
         */
        String resultLine()
        {
            for (String line : output.split("\\R")) {
                if (line.startsWith(H4FirstLoadProbe.RESULT_PREFIX))
                    return line.trim();
            }
            return null;
        }

        String output() { return "exit=" + exitCode + " output=[" + output + "]"; }
    }
}
