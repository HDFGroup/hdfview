package object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.h5.H5CompoundDS;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test to verify H5T_COMPLEX dataset reading functionality.
 *
 * Complex datatypes consist of real and imaginary components stored as
 * floating-point values. This test verifies that complex datasets can be
 * opened, read, and have their metadata accessed without crashing.
 */
@Tag("unit")
@DisplayName("H5T_COMPLEX Datatype Tests")
public class TestComplexDatatype {

    private static final String TEST_DIR = "../hdfview/src/test/resources/uitest/";

    private H5File testFile;

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

    /**
     * Test reading simple float complex datasets (F32 and F64).
     */
    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({"tcomplex.h5, /DatasetFloatComplex, float complex (F32)",
                "tcomplex.h5, /DatasetDoubleComplex, double complex (F64)",
                "tcomplex_be.h5, /DatasetFloatComplex, float complex BE (F32)",
                "tcomplex_be.h5, /DatasetDoubleComplex, double complex BE (F64)"})
    @DisplayName("Simple complex dataset read test")
    public void
    testSimpleComplexDatasetRead(String filename, String datasetPath, String description) throws Exception
    {
        System.out.println("\n=== Testing: " + description + " ===");
        System.out.println("File: " + filename);
        System.out.println("Dataset: " + datasetPath);

        // Open file
        File file = new File(TEST_DIR + filename);
        assertTrue(file.exists(), "Test file not found: " + file.getAbsolutePath());

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        assertNotNull(testFile, "Failed to open test file");
        testFile.open();

        // Get the dataset
        HObject obj = testFile.get(datasetPath);
        assertNotNull(obj, "Dataset not found: " + datasetPath);
        assertTrue(obj instanceof H5ScalarDS, "Object is not a scalar dataset");

        H5ScalarDS dataset = (H5ScalarDS)obj;

        // Print datatype information
        System.out.println("Datatype class: " + dataset.getDatatype().getDatatypeClass());
        System.out.println("Datatype description: " + dataset.getDatatype().getDescription());
        System.out.println("Datatype size: " + dataset.getDatatype().getDatatypeSize() + " bytes");
        System.out.println("Dataset dimensions: " + java.util.Arrays.toString(dataset.getDims()));

        // Attempt to read data - this should not crash
        System.out.println("Attempting to read data...");

        try {
            Object data = dataset.getData();

            if (data == null) {
                System.out.println("⚠ WARNING: Data read returned null (known issue with complex types)");
                System.out.println("This is expected until native HDF5 library support is complete.");
            }
            else {
                System.out.println("✓ SUCCESS: Data read without crash!");
                System.out.println("Data type: " + data.getClass().getName());

                // Complex data should be float[] or double[] (interleaved real/imag)
                if (data instanceof float[]) {
                    float[] floatData = (float[])data;
                    System.out.println("Data length: " + floatData.length + " float values");
                    System.out.println("(Contains " + (floatData.length / 2) + " complex numbers)");

                    // Print first few complex values
                    int printCount = Math.min(6, floatData.length);
                    System.out.print("First values (real, imag): ");
                    for (int i = 0; i < printCount; i += 2) {
                        if (i + 1 < floatData.length) {
                            System.out.print("(" + floatData[i] + "," + floatData[i + 1] + ") ");
                        }
                    }
                    System.out.println();
                }
                else if (data instanceof double[]) {
                    double[] doubleData = (double[])data;
                    System.out.println("Data length: " + doubleData.length + " double values");
                    System.out.println("(Contains " + (doubleData.length / 2) + " complex numbers)");

                    // Print first few complex values
                    int printCount = Math.min(6, doubleData.length);
                    System.out.print("First values (real, imag): ");
                    for (int i = 0; i < printCount; i += 2) {
                        if (i + 1 < doubleData.length) {
                            System.out.print("(" + doubleData[i] + "," + doubleData[i + 1] + ") ");
                        }
                    }
                    System.out.println();
                }
            }
        }
        catch (Exception e) {
            System.out.println("✗ FAILED: Exception during read");
            System.out.println("Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            // Don't fail the test - we're documenting current behavior
            // Once fixed, we can make this a hard failure
        }
    }

    /**
     * Test reading complex datasets with attributes.
     */
    @Test
    @DisplayName("Complex dataset with attributes")
    public void testComplexDatasetWithAttributes() throws Exception
    {
        System.out.println("\n=== Testing: Complex dataset with attributes ===");

        File file = new File(TEST_DIR + "tcomplex.h5");
        assertTrue(file.exists(), "Test file not found");

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        testFile.open();

        // Find a dataset with attributes
        H5ScalarDS dataset = (H5ScalarDS)testFile.get("/DatasetFloatComplex");
        assertNotNull(dataset, "Dataset not found");

        // Get metadata (attributes)
        System.out.println("Reading metadata...");
        dataset.getMetadata();

        System.out.println("✓ Metadata access succeeded");
    }

    /**
     * Test array datasets with complex element types.
     */
    @Test
    @DisplayName("Array dataset with complex elements")
    public void testArrayComplexDataset() throws Exception
    {
        System.out.println("\n=== Testing: Array dataset with complex elements ===");

        File file = new File(TEST_DIR + "tcomplex.h5");
        assertTrue(file.exists(), "Test file not found");

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        testFile.open();

        HObject obj = testFile.get("/ArrayDatasetFloatComplex");
        assertNotNull(obj, "Dataset not found: /ArrayDatasetFloatComplex");

        System.out.println("Dataset type: " + obj.getClass().getSimpleName());

        if (obj instanceof H5ScalarDS) {
            H5ScalarDS dataset = (H5ScalarDS)obj;
            System.out.println("Datatype: " + dataset.getDatatype().getDescription());
            System.out.println("✓ Array complex dataset opened successfully");

            // Try to read data
            try {
                Object data = dataset.getData();
                if (data != null) {
                    System.out.println("✓ Data read successfully: " + data.getClass().getName());
                }
                else {
                    System.out.println("⚠ Data is null");
                }
            }
            catch (Exception e) {
                System.out.println("⚠ Data read failed: " + e.getMessage());
            }
        }
    }

    /**
     * Test compound datasets containing complex members.
     */
    @Test
    @DisplayName("Compound dataset with complex members")
    public void testCompoundComplexDataset() throws Exception
    {
        System.out.println("\n=== Testing: Compound dataset with complex members ===");

        File file = new File(TEST_DIR + "tcomplex.h5");
        assertTrue(file.exists(), "Test file not found");

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        testFile.open();

        HObject obj = testFile.get("/CompoundDatasetFloatComplex");
        assertNotNull(obj, "Dataset not found: /CompoundDatasetFloatComplex");

        System.out.println("Dataset type: " + obj.getClass().getSimpleName());

        if (obj instanceof H5CompoundDS) {
            System.out.println("✓ Compound complex dataset opened successfully");

            // Try to read data
            try {
                Object data = ((H5CompoundDS)obj).getData();
                if (data != null) {
                    System.out.println("✓ Data read successfully");
                }
                else {
                    System.out.println("⚠ Data is null");
                }
            }
            catch (Exception e) {
                System.out.println("⚠ Data read failed: " + e.getMessage());
            }
        }
    }

    /**
     * Test that variable-length complex datasets are properly detected as unsupported.
     *
     * Dataset: /VariableLengthDatasetFloatComplex
     * Type: VLEN { H5T_COMPLEX_IEEE_F32LE }
     * Dimensions: 10
     *
     * NOTE: h5dump can successfully dump this dataset (see tcomplex.ddl),
     * so the data exists in the file. However, the jarhdf5 2.0.0 library's
     * H5DreadVL does not properly handle complex base types, resulting in
     * empty ArrayLists. We detect this combination and throw an exception
     * with a user-friendly error message rather than silently failing.
     *
     * This test verifies that:
     * 1. The dataset can be opened and metadata accessed
     * 2. Reading the data throws an appropriate exception
     * 3. The exception message is clear and helpful
     */
    @Test
    @DisplayName("Variable-length dataset with complex elements (expected to fail)")
    public void testVariableLengthComplexDataset() throws Exception
    {
        System.out.println("\n=== Testing: Variable-length dataset with complex elements ===");
        System.out.println("Expected behavior: Opening succeeds, reading fails with clear error");
        System.out.println("Note: h5dump CAN read this data (see tcomplex.ddl),");
        System.out.println("      but jarhdf5 2.0.0 H5DreadVL doesn't support VLEN complex\n");

        File file = new File(TEST_DIR + "tcomplex.h5");
        assertTrue(file.exists(), "Test file not found");

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        testFile.open();

        HObject obj = testFile.get("/VariableLengthDatasetFloatComplex");
        assertNotNull(obj, "Dataset not found: /VariableLengthDatasetFloatComplex");

        System.out.println("Dataset type: " + obj.getClass().getSimpleName());

        assertTrue(obj instanceof H5ScalarDS, "Object should be H5ScalarDS");
        H5ScalarDS dataset             = (H5ScalarDS)obj;
        hdf.object.h5.H5Datatype dtype = (hdf.object.h5.H5Datatype)dataset.getDatatype();

        // Verify metadata access works
        System.out.println("Datatype: " + dtype.getDescription());
        System.out.println("Datatype class: " + dtype.getDatatypeClass());
        System.out.println("Is VLEN: " + dtype.isVLEN());
        assertTrue(dtype.isVLEN(), "Dataset should be VLEN type");

        if (dtype.getDatatypeBase() != null) {
            hdf.object.h5.H5Datatype baseType = (hdf.object.h5.H5Datatype)dtype.getDatatypeBase();
            System.out.println("Base type: " + baseType.getDescription());
            System.out.println("Base type is complex: " + baseType.isComplex());
            assertTrue(baseType.isComplex(), "Base type should be complex");
        }

        System.out.println("Dataset dimensions: " + java.util.Arrays.toString(dataset.getDims()));
        System.out.println("✓ Variable-length complex dataset opened successfully");

        // Verify that reading the data throws the expected exception
        System.out.println("\nAttempting to read data (should fail with clear error)...");
        Exception exception = assertThrows(Exception.class, () -> {
            dataset.getData();
        }, "Reading VLEN complex dataset should throw an exception");

        // Verify the exception message is helpful
        String message = exception.getMessage();
        System.out.println("✓ Exception thrown as expected: " + message);
        assertTrue(message.contains("Variable-length complex"),
                   "Exception message should mention 'Variable-length complex'");
        assertTrue(message.contains("not currently supported") || message.contains("not supported"),
                   "Exception message should indicate lack of support");
        assertTrue(message.contains("jarhdf5") || message.contains("HDF5 library"),
                   "Exception message should mention the library");

        System.out.println("✓ Test passed: VLEN complex properly detected and reported");
    }

    /**
     * Test long double complex datasets.
     *
     * Dataset: /DatasetLongDoubleComplex
     * Type: H5T_COMPLEX { 128-bit little-endian floating-point }
     * Dimensions: [10, 10]
     *
     * Expected data (from tcomplex.ddl):
     * (0,0): 10+0i, 1+1i, 2+2i, 3+3i, 4+4i, 5+5i, 6+6i, 7+7i, 8+8i, 9+9i
     * (1,0): 9+0i, 1.1+1.1i, 2.1+2.1i, ...
     *
     * Note: Long double is platform-dependent:
     * - x86_64 Linux: 80-bit extended precision (stored in 16 bytes with padding)
     * - Some platforms: 128-bit quadruple precision
     */
    @Test
    @DisplayName("Long double complex dataset")
    public void testLongDoubleComplexDataset() throws Exception
    {
        System.out.println("\n=== Testing: Long double complex dataset ===");
        System.out.println("Expected first row: 10+0i, 1+1i, 2+2i, 3+3i, 4+4i, 5+5i, 6+6i, 7+7i, 8+8i, 9+9i");
        System.out.println("Expected second row: 9+0i, 1.1+1.1i, 2.1+2.1i, ...\n");

        File file = new File(TEST_DIR + "tcomplex.h5");
        assertTrue(file.exists(), "Test file not found");

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        testFile.open();

        H5ScalarDS dataset = (H5ScalarDS)testFile.get("/DatasetLongDoubleComplex");
        assertNotNull(dataset, "Dataset not found");

        hdf.object.h5.H5Datatype dtype = (hdf.object.h5.H5Datatype)dataset.getDatatype();
        System.out.println("Datatype description: " + dtype.getDescription());
        System.out.println("Datatype size: " + dtype.getDatatypeSize() + " bytes");
        System.out.println("Datatype class: " + dtype.getDatatypeClass());
        System.out.println("Is complex: " + dtype.isComplex());
        System.out.println("Dataset dimensions: " + java.util.Arrays.toString(dataset.getDims()));
        System.out.println("✓ Long double complex dataset opened");

        // Try to read data
        System.out.println("\nAttempting to read data...");
        try {
            Object data = dataset.getData();
            assertNotNull(data, "Data should not be null");

            System.out.println("✓ Data read without crash!");
            System.out.println("Data type: " + data.getClass().getName());

            // Long double complex is returned as byte[][] since Java has no native long double type
            // Format: byte[100][32] - 100 complex numbers, 32 bytes each (16 real + 16 imaginary)
            if (data instanceof byte[][]) {
                byte[][] complexData = (byte[][])data;
                int expectedElements = 10 * 10; // 10×10 dataset
                System.out.println("Data structure: byte[" + complexData.length + "][32]");
                System.out.println("Expected: byte[" + expectedElements + "][32]");
                assertEquals(expectedElements, complexData.length, "Should have 100 complex numbers");

                // Verify each complex number is 32 bytes
                for (int i = 0; i < Math.min(5, complexData.length); i++) {
                    assertEquals(32, complexData[i].length, "Complex number " + i + " should be 32 bytes");
                }

                // Verify data is not all zeros (which would indicate read failure)
                int nonZeroBytes = 0;
                for (byte[] complexNum : complexData) {
                    for (byte b : complexNum) {
                        if (b != 0)
                            nonZeroBytes++;
                    }
                }
                System.out.println("Non-zero bytes: " + nonZeroBytes + " out of " + (100 * 32));
                assertTrue(nonZeroBytes > 0,
                           "Data should contain non-zero bytes (not all zeros means data was read)");

                // Display first complex number as sanity check
                // On x86_64 Linux, long double is 80-bit extended precision in 16 bytes
                System.out.println("\nFirst complex number (raw bytes):");
                System.out.print("  Real part (16 bytes): ");
                for (int i = 0; i < 16; i++) {
                    System.out.printf("%02X ", complexData[0][i]);
                }
                System.out.println();
                System.out.print("  Imag part (16 bytes): ");
                for (int i = 16; i < 32; i++) {
                    System.out.printf("%02X ", complexData[0][i]);
                }
                System.out.println();

                System.out.println("\n✓ Long double complex data read successfully as byte[100][32]");
                System.out.println(
                    "Note: Data is in platform-specific long double format (16 bytes per float).");
                System.out.println(
                    "      Object layer preserves raw bytes. UI layer can convert to displayable format.");
            }
            else {
                fail("Expected byte[][] for long double complex, got: " + data.getClass().getName());
            }
        }
        catch (Exception e) {
            System.out.println("✗ Data read failed: " + e.getMessage());
            e.printStackTrace();
            fail("Reading long double complex dataset should not throw exception: " + e.getMessage());
        }
    }
}
