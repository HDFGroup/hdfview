/**
 *
 */
package object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.MetaDataContainer;
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;

/**
 * @author rsinha
 *
 */
public class DataFormatTest {
    private static final Logger log = LoggerFactory.getLogger(DataFormatTest.class);
    private static final H5File H5FILE = new H5File();

    private H5File testFile = null;
    private MetaDataContainer testGroup = null;

    protected void closeFile() {
        if (testFile != null) {
            try {
                testFile.close();
            }
            catch (final Exception ex) {}
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
                System.out.println("DataFormatTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("DataFormatTest AfterClass: Number of IDs still open: " + openID);
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
            testFile = (H5File) H5FILE.open(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testFile);

        try {
            testGroup = (MetaDataContainer) testFile.get(H5TestFile.NAME_GROUP_ATTR);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testGroup);
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
     * Test method for {@link hdf.object.MetaDataContainer#getMetadata()}.
     * <ul>
     * <li>Reading the attributes
     * <li>Checking the values of attributes
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMetadata() {
        log.debug("testGetMetadata start");
        Attribute strAttr = null;
        Attribute arrayIntAttr = null;
        Attribute imgAttr = null;
        List mdataList = null;
        try {
            mdataList = testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        for (int ndx=0; ndx < mdataList.size(); ndx++){
            Attribute attrobj = (Attribute) mdataList.get(ndx);
            if (attrobj.getAttributeDatatype().isString()) {
                String[] value = null;

                try {
                    value = (String[]) attrobj.getAttributeData();
                }
                catch (Exception ex) {
                    log.trace("testGetMetadata(): getAttributeData() failure:", ex);
                    fail("getAttributeData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testGetMetadata(): Out of memory");
                    fail("Out of memory");
                }

                if (value[0].equals("IMAGE"))
                    imgAttr = attrobj;
                else
                    strAttr = attrobj;
            }
            else
                arrayIntAttr = attrobj;
        }

        String[] value = null;

        try {
            value = (String[]) strAttr.getAttributeData();
        }
        catch (Exception ex) {
            log.trace("testGetMetadata(): getAttributeData() failure:", ex);
            fail("getAttributeData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testGetMetadata(): Out of memory");
            fail("Out of memory");
        }

        log.debug("testGetMetadata-strAttr:{}", value[0]);
        if (!value[0].equals("String attribute.")) {
            fail("getMdata() failed.");
        }

        int[] intValue = null;
        long[] dims = arrayIntAttr.getAttributeDims();

        try {
            intValue = (int[]) arrayIntAttr.getAttributeData();
        }
        catch (Exception ex) {
            log.trace("testGetMetadata(): getAttributeData() failure:", ex);
            fail("getAttributeData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testGetMetadata(): Out of memory");
            fail("Out of memory");
        }

        for (int i = 0; i < dims[0]; i++) {
            if (intValue[i] != i + 1) {
                fail("getValue() failed");
            }
        }
    }

    /**
     * Test method for {@link hdf.object.MetaDataContainer#writeMetadata(java.lang.Object)}.
     * <ul>
     * <li>Writing new attributes
     * <li>Checking that the new attributes are written in file
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testWriteMetadata() {
        log.debug("testWriteMetadata");
        long[] attrDims = { 1 };
        String attrName = "CLASS";
        String[] classValue = { "IMAGE" };

        Datatype attrType = null;
        try {
            attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, Datatype.NATIVE, Datatype.NATIVE);
        }
        catch (Exception ex) {
            fail("new H5datatype failed. " + ex);
        }

        H5ScalarAttr attr = new H5ScalarAttr((Group) testGroup, attrName, attrType, attrDims);
        assertNotNull(testGroup);
        assertNotNull(attr);
        try {
            attr.writeAttribute(classValue);
        }
        catch (Exception ex) {
            fail("writeMetadata() failed " + ex.getMessage());
        }

        List mdataList = null;
        try {
            mdataList = testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }

        assertEquals(3, mdataList.size());

        Attribute strAttr = null;
        Attribute arrayIntAttr = null;
        Attribute imgAttr = null;

        for (int ndx=0; ndx < mdataList.size(); ndx++){
            Attribute attrobj = (Attribute) mdataList.get(ndx);
            if (attrobj.getAttributeDatatype().isString()) {
                String[] value = null;

                try {
                    value = (String[]) attrobj.getAttributeData();
                }
                catch (Exception ex) {
                    log.trace("testWriteMetadata(): getAttributeData() failure:", ex);
                    fail("getAttributeData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }

                if (value[0].equals("IMAGE"))
                    imgAttr = attrobj;
                else
                    strAttr = attrobj;
            }
            else
                arrayIntAttr = attrobj;
        }

        String[] value = null;

        try {
            value = (String[]) strAttr.getAttributeData();
        }
        catch (Exception ex) {
            log.trace("testWriteMetadata(): getAttributeData() failure:", ex);
            fail("getAttributeData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testWriteMetadata(): Out of memory");
            fail("Out of memory");
        }

        log.debug("testWriteMetadata-strAttr:{}",value[0]);

        if (!value[0].equals("String attribute.")) {
            fail("writeMdata() failed.");
        }

        int[] intValue = null;
        long[] dims = arrayIntAttr.getAttributeDims();

        try {
            intValue = (int[]) arrayIntAttr.getAttributeData();
        }
        catch (Exception ex) {
            log.trace("testWriteMetadata(): getAttributeData() failure:", ex);
            fail("getAttributeData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testWriteMetadata(): Out of memory");
            fail("Out of memory");
        }

        for (int i = 0; i < dims[0]; i++) {
            if (intValue[i] != i + 1) {
                fail("writeValue() failed");
            }
        }

        try {
            value = (String[]) imgAttr.getAttributeData();
        }
        catch (Exception ex) {
            log.trace("testWriteMetadata(): getAttributeData() failure:", ex);
            fail("getAttributeData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testWriteMetadata(): Out of memory");
            fail("Out of memory");
        }

        log.debug("testWriteMetadata-imgAttr:{}",value[0]);
        if (!value[0].equals("IMAGE")) {
            fail("writeIMGMetadata() failed.");
        }
    }

    /**
     * Test method for {@link hdf.object.MetaDataContainer#removeMetadata(java.lang.Object)}.
     * <ul>
     * <li>Remove an attribute
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRemoveMetadata() {
        log.debug("testRemoveMetadata");
        List mdataList = null;
        try {
            mdataList = testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex.getMessage());
        }

        Attribute theAttr = (Attribute) mdataList.get(2);
        try {
            testGroup.removeMetadata(theAttr);
        }
        catch (Exception e) {
            fail("removeMetadata() failed " + e.getMessage());
        }
        assertEquals(2, mdataList.size());
    }

}
