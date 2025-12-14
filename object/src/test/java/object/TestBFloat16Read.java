package object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import hdf.object.FileFormat;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test to verify BFLOAT16 dataset can be read.
 */
@Tag("unit")
@DisplayName("BFLOAT16 Dataset Read Test")
@Disabled("Temporarily disabled - requires BFLOAT16 support from fix-float8-float16-bfloat16-support branch")
public class TestBFloat16Read {

    private static final String TEST_FILE    = "../hdfview/src/test/resources/uitest/tbfloat16.h5";
    private static final String DATASET_NAME = "/DS16BITS";

    private H5File testFile;

    @BeforeEach
    public void setUp() throws Exception
    {
        // Verify test file exists
        File file = new File(TEST_FILE);
        assertTrue(file.exists(), "Test file not found: " + TEST_FILE);

        // Open the HDF5 file
        testFile = new H5File(TEST_FILE, FileFormat.READ);
        assertNotNull(testFile, "Failed to open test file");
        testFile.open();
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        if (testFile != null) {
            testFile.close();
        }
    }

    @Test
    @DisplayName("BFLOAT16 dataset should read")
    public void testBFloat16DatasetRead() throws Exception
    {
        // Get the dataset
        H5ScalarDS dataset = (H5ScalarDS)testFile.get(DATASET_NAME);
        assertNotNull(dataset, "Dataset not found: " + DATASET_NAME);

        System.out.println("=== BFLOAT16 Dataset Read Test ===");
        System.out.println("Dataset: " + DATASET_NAME);
        System.out.println("Datatype description: " + dataset.getDatatype().getDescription());
        System.out.println("Datatype size reported: " + dataset.getDatatype().getDatatypeSize() + " bytes");
        System.out.println("Attempting to read data...");

        // This should NOT crash if allocateArray() is using native size
        Object data = dataset.getData();

        assertNotNull(data, "Data read returned null");
        System.out.println("✓ SUCCESS: Data read without crash!");
        System.out.println("Data type: " + data.getClass().getName());

        // For BFLOAT16, buffer should be float[] (4-byte elements)
        assertTrue(data instanceof float[], "Expected float[] buffer for BFLOAT16");
        float[] floatData = (float[])data;
        System.out.println("Data length: " + floatData.length);

        // Print first few values
        int printCount = Math.min(10, floatData.length);
        System.out.print("First values: ");
        for (int i = 0; i < printCount; i++) {
            System.out.print(floatData[i] + " ");
        }
        System.out.println();
    }

    @Test
    @DisplayName("BFLOAT16 attribute should read")
    public void testBFloat16AttributeRead() throws Exception
    {
        // Get the dataset (attributes are on the dataset)
        H5ScalarDS dataset = (H5ScalarDS)testFile.get(DATASET_NAME);
        assertNotNull(dataset, "Dataset not found");

        // Get attribute metadata
        dataset.getMetadata();

        System.out.println("=== BFLOAT16 Attribute Read Test ===");
        System.out.println("✓ SUCCESS: Attribute read without crash!");
        System.out.println("(Attributes already work, this confirms no regression)");
    }
}
