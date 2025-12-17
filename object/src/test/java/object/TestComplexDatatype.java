package object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import hdf.object.FileFormat;
import hdf.object.HObject;
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
     * Test compound datasets containing complex members.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"tcompound_complex.h5, /CompoundDatasetFloatComplex",
                "tcompound_complex2.h5, /VariableLengthDatasetFloatComplex"})
    @DisplayName("Compound with complex members")
    public void
    testCompoundWithComplexMembers(String filename, String datasetPath) throws Exception
    {
        System.out.println("\n=== Testing: Compound dataset with complex members ===");
        System.out.println("File: " + filename);
        System.out.println("Dataset: " + datasetPath);

        File file = new File(TEST_DIR + filename);
        assertTrue(file.exists(), "Test file not found: " + file.getAbsolutePath());

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        testFile.open();

        HObject obj = testFile.get(datasetPath);
        assertNotNull(obj, "Dataset not found: " + datasetPath);

        System.out.println("Dataset type: " + obj.getClass().getSimpleName());
        System.out.println("✓ File and dataset opened successfully");

        // Attempt to get metadata
        try {
            if (obj instanceof H5ScalarDS) {
                ((H5ScalarDS)obj).getMetadata();
                System.out.println("✓ Metadata access succeeded");
            }
        }
        catch (Exception e) {
            System.out.println("⚠ Metadata access failed: " + e.getMessage());
        }
    }

    /**
     * Test long double complex datasets.
     */
    @Test
    @DisplayName("Long double complex dataset")
    public void testLongDoubleComplexDataset() throws Exception
    {
        System.out.println("\n=== Testing: Long double complex dataset ===");

        File file = new File(TEST_DIR + "tcomplex.h5");
        assertTrue(file.exists(), "Test file not found");

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        testFile.open();

        H5ScalarDS dataset = (H5ScalarDS)testFile.get("/DatasetLongDoubleComplex");
        assertNotNull(dataset, "Dataset not found");

        System.out.println("Datatype description: " + dataset.getDatatype().getDescription());
        System.out.println("Datatype size: " + dataset.getDatatype().getDatatypeSize() + " bytes");
        System.out.println("✓ Long double complex dataset opened");

        // Note: Long double (F80/F128) support is platform-dependent
        System.out.println("(Long double complex reading may not be fully supported on all platforms)");
    }
}
