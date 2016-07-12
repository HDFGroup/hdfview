/**
 * 
 */
package test.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Enumeration;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.FileFormat;
import hdf.object.h5.H5File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author rsinha
 * 
 */
public class FileFormatTest {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileFormatTest.class);
    private static final H5File H5FILE = new H5File();

    private FileFormat testFile = null;

    @BeforeClass
    public static void createFile() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("FileFormatTest BeforeClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            H5TestFile.createTestFile(null);
        }
        catch (final Exception ex) {
            System.out.println("*** Unable to create HDF5 test file. " + ex);
            System.exit(-1);
        }
    }

    @AfterClass
    public static void checkIDs() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("FileFormatTest AfterClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Before
    public void openFiles() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                log.debug("Before: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        testFile = H5FILE.open(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        assertNotNull(testFile);
        testFile.open();
    }

    @After
    public void removeFiles() throws Exception {
        if (testFile != null) {
            try {
                testFile.close();
            }
            catch (final Exception ex) {
            }
            testFile = null;
        }
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                log.debug("After: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Test method for {@link hdf.object.FileFormat#create(java.lang.String, int)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Create a file that is already created with option FILE_CREATE_OPEN.
     * <li>Create a file that is already created and opened with option FILE_CREATE_DELETE.
     * <li>Create a file that is already created and not opened with FILE_CREATE_DELETE.
     * <li>Create a file that is new with FILE_CREATE_DELETE.
     * <li>Create a file that is new with FILE_CREATE_OPEN.
     * </ul>
     * 
     */
    /*
     * RUTH - come back and update this with new method, createInstance public final void testCreateStringInt() {
     * FileFormat f = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
     * 
     * try { f.create(H5TestFile.NAME_FILE_H5, FileFormat.FILE_CREATE_OPEN); } catch (Exception ex) {
     * fail("Create Failed " + ex.getMessage()); } try { f.create(H5TestFile.NAME_FILE_H5,
     * FileFormat.FILE_CREATE_DELETE); } catch (Exception ex) { ; //Expected to fail. } try { f.create("simpleFile",
     * FileFormat.FILE_CREATE_DELETE); } catch (Exception ex) { fail("Create failed " + ex.getMessage()); } try {
     * f.create("testFile", FileFormat.FILE_CREATE_DELETE); } catch (Exception ex) { fail("Create failed " +
     * ex.getMessage()); } try { f.create("testFile", FileFormat.FILE_CREATE_OPEN); } catch (Exception ex) {
     * fail("Create failed " + ex.getMessage()); } }
     */
    /**
     * Test method for {@link hdf.object.FileFormat#getNumberOfMembers()}.
     * <p>
     * <ul>
     * <li>Test the number of compements.
     * </ul>
     */
    @Test
    public void testGetNumberOfMembers() {
        log.debug("testGetNumberOfMembers");
        assertEquals(testFile.getNumberOfMembers(), 21);
        int nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.FileFormat#getFileFormat(java.lang.String)}.
     * <p>
     * <ul>
     * <li>Test for HDF5.
     * </ul>
     */
    @Test
    public void testGetFileFormat() {
        log.debug("testGetFileFormat");
        FileFormat f = FileFormat.getFileFormat("HDF5");
        assertNotNull(f);
        int nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.FileFormat#getFileFormatKeys()}.
     * <p>
     * <ul>
     * <li>current file formats are HDF5, HDF.
     * </ul>
     */
    @Test
    public void testGetFileFormatKeys() {
        log.debug("testGetFileFormatKeys");

        Enumeration<String> e = FileFormat.getFileFormatKeys();

        while (e.hasMoreElements())
            assertNotNull(FileFormat.getFileFormat((String) e.nextElement()));

        int nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.FileFormat#getFID()}.
     * <p>
     * <ul>
     * <li>Make sure the fid is not -1.
     * </ul>
     */
    @Test
    public void testGetFID() {
        log.debug("testGetFID");
        assertTrue((testFile.getFID() != -1));
        int nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.FileFormat#getInstance(java.lang.String)}.
     * <p>
     * <ul>
     * <li>Open an non existing file.
     * <li>Open an exisiting file.
     * </ul>
     */
    @Test
    public void testGetInstance() {
        log.debug("testGetInstance");
        H5File f = null;
        try {
            f = (H5File) FileFormat.getInstance("test_hdf5.h5");
        }
        catch (Exception ex) {
            ;
        }
        assertNull(f);

        try {
            f = (H5File) FileFormat.getInstance(H5TestFile.NAME_FILE_H5);
        }
        catch (Exception ex) {
            fail("getInstance() failed" + ex.getMessage());
        }
        assertNotNull(f);
        int nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.FileFormat#getFileFormats()}.
     * <p>
     * <ul>
     * <li>Test that the FileFormat object is formed for HDF5.
     * </ul>
     */
    @Test
    public void testGetFileFormats() {
        log.debug("testGetFileFormats");
        FileFormat f = FileFormat.getFileFormat("HDF5");
        assertNotNull(f);
        FileFormat f1 = FileFormat.getFileFormat("ALL");
        assertNull(f1);
        int nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

}
