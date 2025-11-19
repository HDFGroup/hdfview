/**
 * Base test class for JUnit 5 object module tests
 * Provides common setup, utilities, and test patterns
 */
package test;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import hdf.object.h5.H5File;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all object module JUnit 5 tests
 * Provides common patterns for HDF file management and test utilities
 */
@Tag("unit")
@ExtendWith(TestFileExtension.class)
public abstract class BaseObjectTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseObjectTest.class);

    @TempDir
    protected Path tempDir;

    protected H5File testFile;
    protected String testMethodName;

    @BeforeEach
    void baseSetUp(TestInfo testInfo)
    {
        testMethodName = testInfo.getDisplayName();
        log.debug("Starting test: {}", testMethodName);

        // Log open HDF5 objects for debugging
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0) {
                log.debug("Test setup - HDF5 IDs still open: {}", openID);
            }
        }
        catch (Exception ex) {
            log.warn("Could not check HDF5 open ID count", ex);
        }
    }

    @AfterEach
    void baseTearDown()
    {
        // Ensure test file is closed
        closeTestFile();

        // Log open HDF5 objects for debugging
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0) {
                log.warn("Test cleanup - HDF5 IDs still open: {}", openID);
            }
        }
        catch (Exception ex) {
            log.warn("Could not check HDF5 open ID count during cleanup", ex);
        }

        log.debug("Completed test: {}", testMethodName);
    }

    /**
     * Safely close the test file if it's open
     */
    protected void closeTestFile()
    {
        if (testFile != null) {
            try {
                testFile.close();
            }
            catch (final Exception ex) {
                log.warn("Failed to close test file", ex);
            }
            testFile = null;
        }
    }

    /**
     * Check that only the expected number of HDF5 objects are open
     * @param fileid The file ID to check
     * @param expectedCount Expected number of open objects (usually 1 for file ID only)
     */
    protected void assertHDF5ObjectCount(long fileid, int expectedCount)
    {
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(fileid, HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed: " + ex.getMessage());
        }
        assertEquals(expectedCount, nObjs,
                     String.format("Expected %d HDF5 objects open, but found %d", expectedCount, nObjs));
    }

    /**
     * Assert that a test file exists and is readable
     * @param filePath Path to the test file
     */
    protected void assertTestFileExists(Path filePath)
    {
        assertNotNull(filePath, "Test file path should not be null");
        assertTrue(filePath.toFile().exists(), "Test file should exist: " + filePath);
        assertTrue(filePath.toFile().canRead(), "Test file should be readable: " + filePath);
        assertTrue(filePath.toFile().length() > 0, "Test file should not be empty: " + filePath);
    }

    /**
     * Create a descriptive assertion message with test context
     * @param message The base message
     * @return Formatted message with test context
     */
    protected String withTestContext(String message)
    {
        return String.format("[%s] %s", testMethodName, message);
    }

    /**
     * Template method for test-specific setup
     * Override in subclasses for additional setup
     */
    protected void doTestSetup() throws Exception
    {
        // Override in subclasses
    }

    /**
     * Template method for test-specific cleanup
     * Override in subclasses for additional cleanup
     */
    protected void doTestCleanup() throws Exception
    {
        // Override in subclasses
    }
}