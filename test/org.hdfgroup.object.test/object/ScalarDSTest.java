package object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import hdf.object.FileFormat;
import hdf.object.ScalarDS;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;
import hdf.object.h5.H5ScalarDS;

/**
 * @author rsinha
 *
 */
public class ScalarDSTest {
    private static final Logger log = LoggerFactory.getLogger(ScalarDSTest.class);
    private static final H5File H5FILE = new H5File();

    private H5File testFile = null;
    private H5Group testGroup = null;
    private ScalarDS intDset = null;
    private ScalarDS floatDset = null;
    private ScalarDS charDset = null;
    private ScalarDS strDset = null;
    private ScalarDS enumDset = null;
    private ScalarDS imageDset = null;
    private ScalarDS imagePalete = null;
    private ScalarDS ORDset = null;

    protected void closeFile() {
        if (testFile != null) {
            try {
                testFile.close();
            }
            catch (final Exception ex) {
            }
            testFile = null;
        }
    }

    protected void checkObjCount(long fileid) {
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(fileid, HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    @BeforeClass
    public static void createFile() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("ScalarDSTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("ScalarDSTest AfterClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
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
        try {
            testFile = (H5File) H5FILE.open(H5TestFile.NAME_FILE_H5, FileFormat.READ);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testFile);
        testGroup = (H5Group) testFile.get(H5TestFile.NAME_GROUP_ATTR);
        assertNotNull(testGroup);

        intDset = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_INT);
        assertNotNull(intDset);
        floatDset = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_FLOAT);
        assertNotNull(floatDset);
        charDset = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_CHAR);
        assertNotNull(charDset);
        strDset = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_STR);
        assertNotNull(strDset);
        enumDset = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_ENUM);
        assertNotNull(enumDset);
        imageDset = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_IMAGE);
        assertNotNull(imageDset);
        ORDset = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_OBJ_REF);
        assertNotNull(ORDset);
        imagePalete = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_IMAGE_PALETTE);
        assertNotNull(imagePalete);
    }

    @After
    public void removeFiles() throws Exception {
        if (testFile != null) {
            closeFile();
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
     *
     * What to test:
     * <ul>
     * <li>Test for general functionality
     * <ul>
     * <li>Check for all datasets whether it is an image or not (only image should return true).
     * <li>Check for all datasets whether imageDisplay is set (only image should return true).
     * <li>Get Image Data Range from image dataset and check if it is valid.
     * <li>Check the interlace value for the image dataset.
     * <li>Check if the image is unsigned byte.
     * <li>Check setting the image to unsigned byte.
     * <li>Check true color for the image dataset.
     * </ul>
     * </ul>
     */
    @Test
    public void testImageFunctionality() {
        log.debug("testImageFunctionality");
        assertTrue(((H5ScalarDS)imageDset).hasAttribute());
        assertTrue(imageDset.isImage());
        assertTrue(imageDset.isImageDisplay());

        assertEquals(imageDset.getInterlace(), -1);

        assertFalse(imageDset.isTrueColor());

        assertFalse(intDset.isImage());
        assertFalse(floatDset.isImage());
        assertFalse(charDset.isImage());
        assertFalse(enumDset.isImage());
        assertFalse(imagePalete.isImage());
        assertFalse(ORDset.isImage());

        assertFalse(intDset.isImageDisplay());
        assertFalse(floatDset.isImageDisplay());
        assertFalse(charDset.isImageDisplay());
        assertFalse(enumDset.isImageDisplay());
        assertFalse(imagePalete.isImageDisplay());
        assertFalse(ORDset.isImageDisplay());

        intDset.setIsImageDisplay(true);
        assertTrue(intDset.isImageDisplay());
        intDset.setIsImageDisplay(false);
        assertFalse(intDset.isImageDisplay());
    }

    /**
     * What to test:
     * <ul>
     * <li>Test for general functionality
     * <ul>
     * <li>For all datasets in the file check if it is text. Only string dataset should return true.
     * </ul>
     * </ul>
     */
    @Test
    public void testIsText() {
        log.debug("testIsText");
        assertTrue(strDset.getDatatype().isText());
        assertFalse(imageDset.getDatatype().isText());
        assertFalse(intDset.getDatatype().isText());
        assertFalse(floatDset.getDatatype().isText());
        assertFalse(charDset.getDatatype().isText());
        assertFalse(enumDset.getDatatype().isText());
        assertFalse(ORDset.getDatatype().isText());
        assertFalse(imagePalete.getDatatype().isText());
    }
}
