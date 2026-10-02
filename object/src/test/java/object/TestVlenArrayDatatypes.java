package object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.h5.H5File;

import hdf.hdf5lib.H5;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Read and write tests for datatypes holding variable-length data underneath an
 * ARRAY or a COMPOUND, using the shared UI test files.
 *
 * Assertions are on element values rather than only on buffer shape, since a
 * correctly shaped buffer can still be filled with the wrong contents.
 */
@Tag("unit")
@DisplayName("Variable-length Array and Compound Datatype Tests")
public class TestVlenArrayDatatypes {

    private static final String TEST_DIR = "../hdfview/src/test/resources/uitest/";

    /** The three strings stored in every element of tvlenstr_array.h5. */
    private static final String[] EXPECTED_VLEN_STRINGS = {
        "This is a variable-length test string.", "This test string is also variable-length.",
        "A final test of variable-length strings. This string is longer than the others."};

    private H5File testFile;

    private static int openIDsAtStart;

    @BeforeAll
    public static void recordOpenIDs() throws Exception
    {
        openIDsAtStart = H5.getOpenIDCount();
    }

    @AfterAll
    public static void checkIDs() throws Exception
    {
        assertEquals(openIDsAtStart, H5.getOpenIDCount(), "HDF5 identifiers leaked by this test class");
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        if (testFile != null) {
            try {
                testFile.close();
            }
            catch (Exception e) {
                // Ignore close errors
            }
        }
    }

    private H5File openTestFile(String filename) throws Exception
    {
        File file = new File(TEST_DIR + filename);
        assertTrue(file.exists(), "Test file not found: " + file.getAbsolutePath());

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        assertNotNull(testFile, "Failed to open test file");
        testFile.open();

        return testFile;
    }

    private Dataset getDataset(String filename, String datasetPath) throws Exception
    {
        HObject obj = openTestFile(filename).get(datasetPath);
        assertNotNull(obj, "Dataset not found: " + datasetPath);
        assertInstanceOf(Dataset.class, obj, "Object is not a dataset: " + datasetPath);

        Dataset dataset = (Dataset)obj;
        dataset.init();

        return dataset;
    }

    /** Collects every String in a nested read buffer, in order. */
    private static void collectStrings(Object data, List<String> out)
    {
        if (data instanceof String str)
            out.add(str);
        else if (data instanceof List<?> list) {
            for (Object element : list)
                collectStrings(element, out);
        }
        else if (data != null && data.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(data);
            for (int i = 0; i < length; i++)
                collectStrings(java.lang.reflect.Array.get(data, i), out);
        }
    }

    private static List<String> allStrings(Object data)
    {
        List<String> out = new ArrayList<>();
        collectStrings(data, out);
        return out;
    }

    @Test
    @DisplayName("Array of variable-length string")
    public void testArrayOfVlenString() throws Exception
    {
        Dataset dataset = getDataset("tvlenstr_array.h5", "/ScalarArrayOfVlenStr");

        Datatype dtype = dataset.getDatatype();
        assertTrue(dtype.isArray(), "Expected an ARRAY datatype");
        assertTrue(dtype.getDatatypeBase().isVarStr(), "Expected a variable-length string base type");

        Object data = dataset.getData();
        assertNotNull(data, "Data read returned null");

        assertInstanceOf(Object[].class, data, "Expected one buffer slot per selected point");
        Object[] points = (Object[])data;
        assertEquals(5, points.length, "Expected 5 points");

        for (int i = 0; i < points.length; i++) {
            assertInstanceOf(List.class, points[i], "Point " + i + " should hold a List of elements");

            List<?> elements = (List<?>)points[i];
            assertEquals(EXPECTED_VLEN_STRINGS.length, elements.size(), "Point " + i + " element count");

            for (int j = 0; j < EXPECTED_VLEN_STRINGS.length; j++)
                assertEquals(EXPECTED_VLEN_STRINGS[j], elements.get(j), "Point " + i + " element " + j);
        }
    }

    @Test
    @DisplayName("Compound with an array of variable-length string member")
    public void testCompoundWithArrayOfVlenStringMember() throws Exception
    {
        Dataset dataset = getDataset("tvlenstr_array.h5", "/CompoundArrayOfVlenStr");

        Object data = dataset.getData();
        assertNotNull(data, "Data read returned null");

        // One entry per member, one slot per point, each slot a List of elements.
        assertInstanceOf(List.class, data, "Expected one entry per compound member");
        List<?> members = (List<?>)data;
        assertEquals(1, members.size(), "Expected the single member vlen_str_array");

        assertInstanceOf(Object[].class, members.get(0), "Expected one slot per selected point");
        Object[] points = (Object[])members.get(0);
        assertEquals(5, points.length, "Expected 5 points");

        for (int i = 0; i < points.length; i++) {
            assertInstanceOf(List.class, points[i], "Point " + i + " should hold a List of elements");

            List<?> elements = (List<?>)points[i];
            assertEquals(EXPECTED_VLEN_STRINGS.length, elements.size(), "Point " + i + " element count");

            for (int j = 0; j < EXPECTED_VLEN_STRINGS.length; j++)
                assertEquals(EXPECTED_VLEN_STRINGS[j], elements.get(j), "Point " + i + " element " + j);
        }
    }

    @Test
    @DisplayName("Compound mixing an array of variable-length string with fixed members")
    public void testCompoundComplexVlenStringMember() throws Exception
    {
        Dataset dataset = getDataset("tcompound_complex.h5", "/CompoundComplex");

        Object data = dataset.getData();
        assertNotNull(data, "Data read returned null");

        // b_name is an ARRAY[4] of varstr and c_name a fixed-length string, and both
        // are collected, so assert on known values rather than a count.
        List<String> strings = allStrings(data);
        assertFalse(strings.isEmpty(), "Expected string members to be read");
        assertTrue(strings.contains("A fight is a contract that takes two people to honor."),
                   "Expected the first variable-length string of b_name");
        assertTrue(strings.contains("  --  Professor Cheng Man-ch'ing"),
                   "Expected the last variable-length string of b_name");
    }

    @Test
    @DisplayName("Array of compound")
    public void testArrayOfCompound() throws Exception
    {
        Dataset dataset = getDataset("tarray4.h5", "/Dataset1");

        Datatype dtype = dataset.getDatatype();
        assertTrue(dtype.isArray(), "Expected an ARRAY datatype");
        assertTrue(dtype.getDatatypeBase().isCompound(), "Expected a compound base type");

        Object data = dataset.getData();
        assertNotNull(data, "Data read returned null");

        // No variable-length data, so the layout is one flat primitive array per member.
        assertInstanceOf(List.class, data, "Expected one entry per compound member");
        List<?> members = (List<?>)data;
        assertEquals(2, members.size(), "Expected members i and f");

        assertInstanceOf(int[].class, members.get(0), "Member i should be a flat int[]");
        assertInstanceOf(float[].class, members.get(1), "Member f should be a flat float[]");
        assertEquals(16, ((int[])members.get(0)).length, "Expected 4 points x 4 array elements");
    }

    @ParameterizedTest(name = "{0}{1}")
    @CsvSource({"tvlenstr_array.h5, /ScalarArrayOfVlenStr", "tvlenstr_array.h5, /CompoundArrayOfVlenStr",
                "tarray4.h5, /Dataset1", "tcompound_complex.h5, /CompoundComplex",
                "tcompound_complex2.h5, /CompoundComplex1D", "tstr.h5, /comp1"})
    @DisplayName("Nested datatype read")
    public void
    testNestedDatatypeRead(String filename, String datasetPath) throws Exception
    {
        Dataset dataset = getDataset(filename, datasetPath);

        Object data =
            assertDoesNotThrow(() -> dataset.getData(), "Reading " + filename + datasetPath + " threw");
        assertNotNull(data, "Data read returned null for " + filename + datasetPath);
    }

    @Test
    @DisplayName("Array of variable-length string write")
    public void testArrayOfVlenStringWriteRoundTrip() throws Exception
    {
        Path source = new File(TEST_DIR + "tvlenstr_array.h5").toPath();
        assertTrue(Files.exists(source), "Test file not found: " + source);

        /*
         * Written under target/ rather than a JUnit temporary directory: HDF5 can still
         * hold a file open when the temporary directory is torn down, which Windows
         * refuses to delete and JUnit then reports as a failure.
         */
        Path workDir = Path.of("target", "vlen-array-datatypes");
        Files.createDirectories(workDir);

        Path target = workDir.resolve("tvlenstr_array_rw.h5");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        final String replacement = "A replacement variable-length string.";

        testFile = new H5File(target.toString(), FileFormat.WRITE);
        testFile.open();
        try {
            Dataset dataset = (Dataset)testFile.get("/ScalarArrayOfVlenStr");
            dataset.init();

            Object data = dataset.getData();
            @SuppressWarnings("unchecked")
            List<Object> firstPoint = (List<Object>)((Object[])data)[0];
            firstPoint.set(1, replacement);

            dataset.write(data);
        }
        finally {
            testFile.close();
            testFile = null;
        }

        testFile = new H5File(target.toString(), FileFormat.READ);
        testFile.open();

        Dataset reopened = (Dataset)testFile.get("/ScalarArrayOfVlenStr");
        reopened.init();

        Object[] points    = (Object[])reopened.getData();
        List<?> firstPoint = (List<?>)points[0];

        assertEquals(EXPECTED_VLEN_STRINGS[0], firstPoint.get(0), "Untouched element changed");
        assertEquals(replacement, firstPoint.get(1), "Edited element did not round-trip");
        assertEquals(EXPECTED_VLEN_STRINGS[2], firstPoint.get(2), "Untouched element changed");

        List<?> secondPoint = (List<?>)points[1];
        assertEquals(EXPECTED_VLEN_STRINGS[1], secondPoint.get(1), "A different point was modified");
    }
}
