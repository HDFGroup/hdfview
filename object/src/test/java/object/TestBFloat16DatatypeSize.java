package object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;
import hdf.object.h5.H5ScalarAttr;

import java.io.File;
import java.util.List;

/**
 * Test to investigate BFLOAT16 datatype size discrepancy between datasets and attributes.
 *
 * Issue: For BFLOAT16 data:
 * - Attributes report size=4 (correct, uses native type)
 * - Datasets report size=2 (incorrect, uses file type)
 *
 * Both should use the native type size (4 bytes) for proper memory allocation.
 */
@Tag("unit")
@DisplayName("BFLOAT16 Datatype Size Consistency Test")
public class TestBFloat16DatatypeSize {

    private static final String TEST_FILE = "../hdfview/src/test/resources/uitest/tbfloat16.h5";
    private static final String DATASET_NAME = "/DS16BITS";
    private static final String ATTRIBUTE_NAME = "DS16BITS";

    private H5File testFile;

    @BeforeEach
    public void setUp() throws Exception {
        // Verify test file exists
        File file = new File(TEST_FILE);
        assertTrue(file.exists(), "Test file not found: " + TEST_FILE);

        // Open the HDF5 file
        testFile = new H5File(TEST_FILE, FileFormat.READ);
        assertNotNull(testFile, "Failed to open test file");
        testFile.open();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (testFile != null) {
            testFile.close();
        }
    }

    @Test
    @DisplayName("Dataset and attribute should report same datatype size for BFLOAT16")
    public void testBFloat16TypeSizeConsistency() throws Exception {
        // Get the dataset
        H5ScalarDS dataset = (H5ScalarDS) testFile.get(DATASET_NAME);
        assertNotNull(dataset, "Dataset not found: " + DATASET_NAME);

        // Get dataset datatype
        Datatype datasetType = dataset.getDatatype();
        assertNotNull(datasetType, "Dataset datatype is null");
        assertTrue(datasetType.isFloat(), "Dataset should be float type");

        // Get the attribute from the dataset
        List<?> attributes = dataset.getMetadata();
        assertNotNull(attributes, "Attributes list is null");
        assertFalse(attributes.isEmpty(), "No attributes found on dataset");

        H5ScalarAttr attribute = null;
        for (Object obj : attributes) {
            if (obj instanceof H5ScalarAttr) {
                H5ScalarAttr attr = (H5ScalarAttr) obj;
                if (ATTRIBUTE_NAME.equals(attr.getAttributeName())) {
                    attribute = attr;
                    break;
                }
            }
        }
        assertNotNull(attribute, "Attribute not found: " + ATTRIBUTE_NAME);

        // Get attribute datatype
        Datatype attributeType = attribute.getDatatype();
        assertNotNull(attributeType, "Attribute datatype is null");
        assertTrue(attributeType.isFloat(), "Attribute should be float type");

        // Compare sizes
        long datasetSize = datasetType.getDatatypeSize();
        long attributeSize = attributeType.getDatatypeSize();

        System.out.println("=== BFLOAT16 Datatype Size Test ===");
        System.out.println("Dataset  '" + DATASET_NAME + "' datatype size: " + datasetSize + " bytes");
        System.out.println("Attribute '" + ATTRIBUTE_NAME + "' datatype size: " + attributeSize + " bytes");
        System.out.println("Dataset  description: " + datasetType.getDescription());
        System.out.println("Attribute description: " + attributeType.getDescription());

        // The test - they should be equal!
        assertEquals(attributeSize, datasetSize,
            String.format("Dataset and attribute should have same datatype size for BFLOAT16. " +
                         "Dataset reports %d bytes, attribute reports %d bytes. " +
                         "Both should use native type size (4 bytes).",
                         datasetSize, attributeSize));
    }

    @Test
    @DisplayName("Test native type handling for BFLOAT16 dataset")
    public void testBFloat16NativeTypeHandling() throws Exception {
        // Get the dataset
        H5ScalarDS dataset = (H5ScalarDS) testFile.get(DATASET_NAME);
        assertNotNull(dataset, "Dataset not found");

        // Open dataset and get type IDs directly
        long did = dataset.open();
        assertTrue(did >= 0, "Failed to open dataset");

        try {
            // Get file datatype
            long fileTid = H5.H5Dget_type(did);
            assertTrue(fileTid >= 0, "Failed to get dataset type");

            try {
                long fileSize = H5.H5Tget_size(fileTid);
                int fileClass = H5.H5Tget_class(fileTid);

                // Get native datatype
                long nativeTid = H5.H5Tget_native_type(fileTid);
                assertTrue(nativeTid >= 0, "Failed to get native type");

                try {
                    long nativeSize = H5.H5Tget_size(nativeTid);
                    boolean isEqual = H5.H5Tequal(fileTid, nativeTid);

                    System.out.println("=== Native Type Investigation ===");
                    System.out.println("File type ID: " + fileTid);
                    System.out.println("File type size: " + fileSize + " bytes");
                    System.out.println("File type class: " + fileClass);
                    System.out.println("Native type ID: " + nativeTid);
                    System.out.println("Native type size: " + nativeSize + " bytes");
                    System.out.println("File == Native?: " + isEqual);

                    // For BFLOAT16, file size is 2 bytes (storage) but native size is 4 bytes (memory)
                    assertEquals(HDF5Constants.H5T_FLOAT, fileClass, "Should be float class");
                    assertEquals(2, fileSize, "BFLOAT16 file storage size should be 2 bytes");
                    assertEquals(4, nativeSize, "BFLOAT16 native memory size should be 4 bytes");
                    assertFalse(isEqual, "File type should NOT equal native type for BFLOAT16");

                    // The key point: HDFView should use nativeSize (4), not fileSize (2)
                    System.out.println("\n✓ CORRECT: HDFView should use native size (" + nativeSize + " bytes)");
                    System.out.println("✗ BUG: HDFView datasets currently use file size (" + fileSize + " bytes)");
                }
                finally {
                    H5.H5Tclose(nativeTid);
                }
            }
            finally {
                H5.H5Tclose(fileTid);
            }
        }
        finally {
            dataset.close(did);
        }
    }

    @Test
    @DisplayName("Test H5Datatype constructor behavior with BFLOAT16")
    public void testH5DatatypeConstructorWithBFloat16() throws Exception {
        // Get the dataset
        H5ScalarDS dataset = (H5ScalarDS) testFile.get(DATASET_NAME);
        long did = dataset.open();

        try {
            // Get file type ID
            long fileTid = H5.H5Dget_type(did);

            try {
                // Create H5Datatype with file type ID (what init() does)
                H5Datatype datatypeFromFile = new H5Datatype(testFile, fileTid);
                long sizeFromFile = datatypeFromFile.getDatatypeSize();

                // Get native type ID
                long nativeTid = H5.H5Tget_native_type(fileTid);

                try {
                    // Create H5Datatype with native type ID (what should happen?)
                    H5Datatype datatypeFromNative = new H5Datatype(testFile, nativeTid);
                    long sizeFromNative = datatypeFromNative.getDatatypeSize();

                    System.out.println("=== H5Datatype Constructor Test ===");
                    System.out.println("H5Datatype(file type)   size: " + sizeFromFile + " bytes");
                    System.out.println("H5Datatype(native type) size: " + sizeFromNative + " bytes");
                    System.out.println("Description (file):   " + datatypeFromFile.getDescription());
                    System.out.println("Description (native): " + datatypeFromNative.getDescription());

                    // This shows the issue: creating H5Datatype with file type gives wrong size
                    assertEquals(2, sizeFromFile, "File type creates 2-byte datatype");
                    assertEquals(4, sizeFromNative, "Native type creates 4-byte datatype");

                    System.out.println("\n→ FINDING: H5Datatype constructor uses whatever size the type ID reports");
                    System.out.println("→ FIX: HDFView should create H5Datatype with native type ID, not file type ID");
                }
                finally {
                    H5.H5Tclose(nativeTid);
                }
            }
            finally {
                H5.H5Tclose(fileTid);
            }
        }
        finally {
            dataset.close(did);
        }
    }
}
