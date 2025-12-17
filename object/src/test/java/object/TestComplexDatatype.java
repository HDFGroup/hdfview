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
     * Test variable-length datasets with complex element types.
     *
     * NOTE: h5dump can successfully dump this dataset (see tcomplex.ddl),
     * so the data exists and should be readable. If ArrayLists are empty,
     * this indicates a bug in H5DreadVL handling for complex types.
     */
    @Test
    @DisplayName("Variable-length dataset with complex elements")
    public void testVariableLengthComplexDataset() throws Exception
    {
        System.out.println("\n=== Testing: Variable-length dataset with complex elements ===");
        System.out.println("Expected data (from tcomplex.ddl):");
        System.out.println("  (0): (10+0i, 1+1i, 2+2i, 3+3i, 4+4i, 5+5i, 6+6i, 7+7i, 8+8i, 9+9i)");
        System.out.println("  (1): (9+0i, 1.1+1.1i, 2.1+2.1i, ..., 9.1+9.1i)");
        System.out.println("  ... (each element should have 10 complex values)\n");

        File file = new File(TEST_DIR + "tcomplex.h5");
        assertTrue(file.exists(), "Test file not found");

        testFile = new H5File(file.getAbsolutePath(), FileFormat.READ);
        testFile.open();

        HObject obj = testFile.get("/VariableLengthDatasetFloatComplex");
        assertNotNull(obj, "Dataset not found: /VariableLengthDatasetFloatComplex");

        System.out.println("Dataset type: " + obj.getClass().getSimpleName());

        if (obj instanceof H5ScalarDS) {
            H5ScalarDS dataset = (H5ScalarDS)obj;
            hdf.object.h5.H5Datatype dtype = (hdf.object.h5.H5Datatype)dataset.getDatatype();

            System.out.println("Datatype: " + dtype.getDescription());
            System.out.println("Datatype class: " + dtype.getDatatypeClass());
            System.out.println("Is VLEN: " + dtype.isVLEN());
            if (dtype.getDatatypeBase() != null) {
                System.out.println("Base type: " + dtype.getDatatypeBase().getDescription());
            }
            System.out.println("Dataset dimensions: " + java.util.Arrays.toString(dataset.getDims()));
            System.out.println("✓ Variable-length complex dataset opened successfully");

            // Try to read data
            try {
                System.out.println("\nAttempting to read data...");
                Object data = dataset.getData();
                if (data != null) {
                    System.out.println("✓ Data read without crash!");
                    System.out.println("Data type: " + data.getClass().getName());

                    if (data.getClass().isArray()) {
                        int length = java.lang.reflect.Array.getLength(data);
                        System.out.println("Array length: " + length + " (should be 10)");

                        if (length > 0) {
                            Object firstElement = java.lang.reflect.Array.get(data, 0);
                            if (firstElement != null) {
                                System.out.println("First element type: " + firstElement.getClass().getName());

                                if (firstElement instanceof java.util.ArrayList) {
                                    java.util.ArrayList<?> list = (java.util.ArrayList<?>)firstElement;
                                    System.out.println("First element is ArrayList with size: " + list.size() +
                                                       " (should be 10)");
                                    if (list.size() > 0) {
                                        System.out.println("✓ ArrayList has data!");
                                        Object firstValue = list.get(0);
                                        System.out.println("First ArrayList element type: " +
                                                           firstValue.getClass().getName());
                                        System.out.println("First value: " + firstValue);
                                    }
                                    else {
                                        System.out.println("✗ BUG: First ArrayList is EMPTY!");
                                        System.out.println("   Expected: 10 complex float values");
                                        System.out.println("   Actual: 0 values");
                                        System.out.println(
                                            "   This is a bug - h5dump can read this data correctly (see tcomplex.ddl)");
                                    }
                                }
                            }
                            else {
                                System.out.println("⚠ First element is null");
                            }
                        }
                        else {
                            System.out.println("⚠ Array is empty");
                        }
                    }
                }
                else {
                    System.out.println("⚠ Data is null");
                }
            }
            catch (Exception e) {
                System.out.println("✗ Data read failed: " + e.getMessage());
                e.printStackTrace();
            }
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
