/**
 * Custom JUnit 5 assertions for HDF5 testing
 * Provides domain-specific assertions for HDF objects and data validation
 */
package test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import hdf.object.Dataset;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5File;

/**
 * HDF5-specific assertions for JUnit 5 tests
 * Provides readable and descriptive assertion methods for HDF objects
 */
public class HDF5Assertions {

    /**
     * Assert that a dataset exists in the given file
     * @param file The HDF5 file to search
     * @param datasetName The name/path of the dataset
     */
    public static void assertDatasetExists(H5File file, String datasetName)
    {
        assertNotNull(file, "File should not be null");
        assertNotNull(datasetName, "Dataset name should not be null");

        try {
            HObject obj = file.get(datasetName);
            assertNotNull(obj, () -> "Dataset should exist: " + datasetName);
            assertTrue(obj instanceof Dataset,
                       ()
                           -> String.format("Object '%s' should be a Dataset, but was %s", datasetName,
                                            obj.getClass().getSimpleName()));
        }
        catch (Exception ex) {
            fail("Failed to retrieve dataset '" + datasetName + "': " + ex.getMessage());
        }
    }

    /**
     * Assert that a group exists in the given file
     * @param file The HDF5 file to search
     * @param groupName The name/path of the group
     */
    public static void assertGroupExists(H5File file, String groupName)
    {
        assertNotNull(file, "File should not be null");
        assertNotNull(groupName, "Group name should not be null");

        try {
            HObject obj = file.get(groupName);
            assertNotNull(obj, () -> "Group should exist: " + groupName);
            assertTrue(obj instanceof Group,
                       ()
                           -> String.format("Object '%s' should be a Group, but was %s", groupName,
                                            obj.getClass().getSimpleName()));
        }
        catch (Exception ex) {
            fail("Failed to retrieve group '" + groupName + "': " + ex.getMessage());
        }
    }

    /**
     * Assert that a dataset contains the expected data
     * @param dataset The dataset to check
     * @param expectedData The expected data values
     */
    public static void assertDatasetData(Dataset dataset, Object expectedData)
    {
        assertNotNull(dataset, "Dataset should not be null");
        assertNotNull(expectedData, "Expected data should not be null");

        try {
            Object actualData = dataset.getData();
            assertNotNull(actualData, "Dataset data should not be null");

            // Handle different data types
            if (expectedData.getClass().isArray() && actualData.getClass().isArray()) {
                if (expectedData instanceof int[]) {
                    assertArrayEquals((int[])expectedData, (int[])actualData,
                                      "Dataset int array data should match expected values");
                }
                else if (expectedData instanceof float[]) {
                    assertArrayEquals((float[])expectedData, (float[])actualData, 0.001f,
                                      "Dataset float array data should match expected values");
                }
                else if (expectedData instanceof double[]) {
                    assertArrayEquals((double[])expectedData, (double[])actualData, 0.001,
                                      "Dataset double array data should match expected values");
                }
                else if (expectedData instanceof String[]) {
                    assertArrayEquals((String[])expectedData, (String[])actualData,
                                      "Dataset string array data should match expected values");
                }
                else {
                    // Generic array comparison
                    assertTrue(Arrays.deepEquals(new Object[] {expectedData}, new Object[] {actualData}),
                               "Dataset array data should match expected values");
                }
            }
            else {
                assertEquals(expectedData, actualData, "Dataset data should match expected value");
            }
        }
        catch (Exception ex) {
            fail("Failed to compare dataset data: " + ex.getMessage());
        }
    }

    /**
     * Assert that a file contains the expected structure (groups and datasets)
     * @param file The HDF5 file to check
     * @param expectedPaths Array of expected object paths
     */
    public static void assertFileStructure(H5File file, String... expectedPaths)
    {
        assertNotNull(file, "File should not be null");
        assertNotNull(expectedPaths, "Expected paths should not be null");

        for (String path : expectedPaths) {
            try {
                HObject obj = file.get(path);
                assertNotNull(obj, () -> "File should contain object at path: " + path);
            }
            catch (Exception ex) {
                fail("Failed to check file structure for path '" + path + "': " + ex.getMessage());
            }
        }
    }

    /**
     * Assert that a file is valid and can be opened
     * @param file The file to validate
     */
    public static void assertValidHDF5File(FileFormat file)
    {
        assertNotNull(file, "File should not be null");
        assertTrue(file instanceof H5File, "File should be an H5File instance");

        H5File h5File = (H5File)file;
        try {
            assertNotNull(h5File.getAbsolutePath(), "File should have a valid path");
            assertTrue(h5File.getAbsolutePath().length() > 0, "File path should not be empty");
        }
        catch (Exception ex) {
            fail("File validation failed: " + ex.getMessage());
        }
    }

    /**
     * Assert that a dataset has the expected dimensions
     * @param dataset The dataset to check
     * @param expectedDims Expected dimension sizes
     */
    public static void assertDatasetDimensions(Dataset dataset, long... expectedDims)
    {
        assertNotNull(dataset, "Dataset should not be null");
        assertNotNull(expectedDims, "Expected dimensions should not be null");

        long[] actualDims = dataset.getDims();
        assertNotNull(actualDims, "Dataset dimensions should not be null");

        assertArrayEquals(expectedDims, actualDims,
                          ()
                              -> String.format("Dataset dimensions should be %s but were %s",
                                               Arrays.toString(expectedDims), Arrays.toString(actualDims)));
    }

    /**
     * Assert that a dataset has the expected rank (number of dimensions)
     * @param dataset The dataset to check
     * @param expectedRank Expected number of dimensions
     */
    public static void assertDatasetRank(Dataset dataset, int expectedRank)
    {
        assertNotNull(dataset, "Dataset should not be null");

        int actualRank = dataset.getRank();
        assertEquals(expectedRank, actualRank,
                     () -> String.format("Dataset rank should be %d but was %d", expectedRank, actualRank));
    }

    /**
     * Assert that an object has the expected name
     * @param object The HDF object to check
     * @param expectedName Expected name (can be partial for path matching)
     */
    public static void assertObjectName(HObject object, String expectedName)
    {
        assertNotNull(object, "Object should not be null");
        assertNotNull(expectedName, "Expected name should not be null");

        String actualName = object.getName();
        assertNotNull(actualName, "Object name should not be null");
        assertTrue(actualName.endsWith(expectedName),
                   () -> String.format("Object name '%s' should end with '%s'", actualName, expectedName));
    }
}