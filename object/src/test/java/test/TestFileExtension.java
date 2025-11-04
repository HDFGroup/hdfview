/**
 * JUnit 5 extension for managing HDF test files
 * Handles test file lifecycle and cleanup automatically
 */
package test;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import object.H5TestFile;

/**
 * JUnit 5 extension that manages HDF test file lifecycle
 * Ensures test files are properly created and cleaned up
 */
public class TestFileExtension implements BeforeEachCallback, AfterEachCallback {

    private static final Logger log = LoggerFactory.getLogger(TestFileExtension.class);
    private static boolean testFileCreated = false;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Create test file once per test class execution
        synchronized (TestFileExtension.class) {
            if (!testFileCreated) {
                log.info("Creating HDF5 test file for test execution");
                try {
                    H5TestFile.createTestFile(null);
                    testFileCreated = true;
                    log.info("HDF5 test file created successfully");
                } catch (Exception ex) {
                    log.error("Failed to create HDF5 test file", ex);
                    throw new RuntimeException("Unable to create HDF5 test file: " + ex.getMessage(), ex);
                }
            }
        }

        // Log test method start
        Method testMethod = context.getRequiredTestMethod();
        String testName = context.getDisplayName();
        log.debug("Setting up test: {}.{}", testMethod.getDeclaringClass().getSimpleName(), testName);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Log test method completion
        String testName = context.getDisplayName();
        log.debug("Completed test: {}", testName);

        // Note: We don't delete the test file after each test
        // because multiple tests may use the same file
        // It will be cleaned up by H5TestFile.deleteTestFile() in @AfterAll
    }

    /**
     * Manually trigger test file deletion (called from @AfterAll methods)
     */
    public static void cleanupTestFile() {
        synchronized (TestFileExtension.class) {
            if (testFileCreated) {
                log.info("Cleaning up HDF5 test file");
                try {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(H5TestFile.NAME_FILE_H5));
                    testFileCreated = false;
                    log.info("HDF5 test file cleaned up successfully");
                } catch (Exception ex) {
                    log.warn("Failed to delete HDF5 test file", ex);
                }
            }
        }
    }
}