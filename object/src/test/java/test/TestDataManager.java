/**
 * Test data management utilities for JUnit 5 tests
 * Handles test file creation, data generation, and resource management
 */
package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import hdf.object.FileFormat;
import hdf.object.h5.H5File;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import object.H5TestFile;

/**
 * Utility class for managing test data and files in JUnit 5 tests
 * Provides methods for file copying, data generation, and cleanup
 */
public class TestDataManager {

    private static final Logger log = LoggerFactory.getLogger(TestDataManager.class);

    /**
     * Copy a test file from resources to a temporary directory
     * @param resourceName Name of the resource file (e.g., "test.h5")
     * @param destination Target path where the file should be copied
     * @throws IOException If file copying fails
     */
    public static void copyTestFileFromResources(String resourceName, Path destination) throws IOException
    {
        // For now, we'll use the existing H5TestFile mechanism
        // In Phase 2B, we can enhance this to load from actual resources
        throw new UnsupportedOperationException(
            "Resource-based test files not yet implemented. Use createTestFile() instead.");
    }

    /**
     * Create a temporary HDF5 test file using the existing test file generator
     * @param tempDir Temporary directory for the file
     * @param fileName Name for the test file
     * @return Path to the created test file
     * @throws Exception If file creation fails
     */
    public static Path createTestFile(Path tempDir, String fileName) throws Exception
    {
        // Use existing H5TestFile creation mechanism
        H5TestFile.createTestFile(null);

        // Return the path to the created file
        Path testFilePath = tempDir.resolve(fileName != null ? fileName : H5TestFile.NAME_FILE_H5);

        // Copy the created test file to our temp directory
        Path originalFile = Path.of(H5TestFile.NAME_FILE_H5);
        if (Files.exists(originalFile)) {
            Files.copy(originalFile, testFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return testFilePath;
    }

    /**
     * Create a programmatic HDF5 file with simple test data
     * @param filePath Path where the file should be created
     * @param fileName Name for the file
     * @return H5File instance for the created file
     * @throws Exception If file creation fails
     */
    public static H5File createSimpleTestFile(Path filePath, String fileName) throws Exception
    {
        Path fullPath = filePath.resolve(fileName);

        // Create a simple HDF5 file programmatically
        long fileId = H5.H5Fcreate(fullPath.toString(), HDF5Constants.H5F_ACC_TRUNC,
                                   HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

        try {
            // Create a simple dataset
            long[] dims  = {10, 10};
            long spaceId = H5.H5Screate_simple(2, dims, null);
            long datasetId =
                H5.H5Dcreate(fileId, "/simple_dataset", HDF5Constants.H5T_NATIVE_INT, spaceId,
                             HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

            // Write some test data
            int[] data = new int[100];
            for (int i = 0; i < 100; i++) {
                data[i] = i;
            }
            H5.H5Dwrite(datasetId, HDF5Constants.H5T_NATIVE_INT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                        HDF5Constants.H5P_DEFAULT, data);

            // Cleanup HDF5 resources
            H5.H5Dclose(datasetId);
            H5.H5Sclose(spaceId);
        }
        finally {
            H5.H5Fclose(fileId);
        }

        // Return H5File instance
        return new H5File(fullPath.toString(), FileFormat.READ);
    }

    /**
     * Generate test data array of specified type and size
     * @param dataType Type of data ("int", "float", "double", "string")
     * @param size Size of the array
     * @return Generated test data array
     */
    public static Object generateTestData(String dataType, int size)
    {
        switch (dataType.toLowerCase()) {
        case "int":
            int[] intData = new int[size];
            for (int i = 0; i < size; i++) {
                intData[i] = i * 10;
            }
            return intData;

        case "float":
            float[] floatData = new float[size];
            for (int i = 0; i < size; i++) {
                floatData[i] = i * 0.5f;
            }
            return floatData;

        case "double":
            double[] doubleData = new double[size];
            for (int i = 0; i < size; i++) {
                doubleData[i] = i * 0.25;
            }
            return doubleData;

        case "string":
            String[] stringData = new String[size];
            for (int i = 0; i < size; i++) {
                stringData[i] = "TestString_" + i;
            }
            return stringData;

        default:
            throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }
    }

    /**
     * Clean up test files and resources
     * @param filePath Path to the file to delete
     */
    public static void cleanupTestFile(Path filePath)
    {
        if (filePath != null && Files.exists(filePath)) {
            try {
                Files.delete(filePath);
                log.debug("Deleted test file: {}", filePath);
            }
            catch (IOException ex) {
                log.warn("Failed to delete test file: {}", filePath, ex);
            }
        }
    }

    /**
     * Validate that a test file is properly formed and accessible
     * @param filePath Path to the test file
     * @return true if the file is valid, false otherwise
     */
    public static boolean validateTestFile(Path filePath)
    {
        if (filePath == null || !Files.exists(filePath)) {
            return false;
        }

        H5File testFile = null;
        try {
            testFile = new H5File(filePath.toString(), FileFormat.READ);
            // Try to open and get basic file info
            testFile.open();
            return testFile.getFID() > 0;
        }
        catch (Exception ex) {
            log.warn("Test file validation failed for: {}", filePath, ex);
            return false;
        }
        finally {
            if (testFile != null) {
                try {
                    testFile.close();
                }
                catch (Exception e) {
                    // Ignore close errors
                }
            }
        }
    }

    /**
     * Get the standard test dataset names used in H5TestFile
     * @return Array of standard test dataset names
     */
    public static String[] getStandardTestDatasetNames()
    {
        return new String[] {H5TestFile.NAME_DATASET_INT,  H5TestFile.NAME_DATASET_FLOAT,
                             H5TestFile.NAME_DATASET_CHAR, H5TestFile.NAME_DATASET_STR,
                             H5TestFile.NAME_DATASET_ENUM, H5TestFile.NAME_DATASET_COMPOUND,
                             H5TestFile.NAME_DATASET_IMAGE};
    }
}