/**
 * 
 */
package test.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Attribute;
import hdf.object.MetaDataContainer;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
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
public class DataFormatTest {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataFormatTest.class);
    private static final H5File H5FILE = new H5File();

    private H5File testFile = null;
    private MetaDataContainer testGroup = null;

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
        testFile = (H5File) H5FILE.open(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        assertNotNull(testFile);
        testGroup = testFile.get(H5TestFile.NAME_GROUP_ATTR);
        assertNotNull(testGroup);
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
     * Test method for {@link hdf.object.MetaDataContainer#getFile()}.
     * <ul>
     * <li>Test if the file name is correct
     * </ul>
     */
    @Test
    public void testGetFile() {
        log.debug("testGetFile: {}",testGroup.getFile());
        if (!testGroup.getFile().endsWith(H5TestFile.NAME_FILE_H5)) {
            fail("getFile() fails.");
        }
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.MetaDataContainer#getMetadata()}.
     * <ul>
     * <li>Reading the attributes
     * <li>Checking the values of attributes
     * </ul>
     */
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
            if(attrobj.getType().getDatatypeClass()==Datatype.CLASS_STRING) {
                String[] value = (String[]) attrobj.getValue();
                if (value[0].equals("IMAGE"))
                    imgAttr = attrobj;
                else
                    strAttr = attrobj;
            }
            else
                arrayIntAttr = attrobj;
        }
        String[] value = (String[]) strAttr.getValue();
        log.debug("testGetMetadata-strAttr:{}", value[0]);
        if (!value[0].equals("String attribute.")) {
            fail("getMdata() failed.");
        }

        int[] intValue = (int[]) arrayIntAttr.getValue();
        long[] dims = arrayIntAttr.getDataDims();

        for (int i = 0; i < dims[0]; i++) {
            if (intValue[i] != i + 1) {
                fail("getValue() failed");
            }
        }
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.MetaDataContainer#writeMetadata(java.lang.Object)}.
     * <ul>
     * <li>Writing new attributes
     * <li>Checking that the new attributes are written in file
     * </ul>
     */
    @Test
    public void testWriteMetadata() {
        log.debug("testWriteMetadata");
        long[] attrDims = { 1 };
        String attrName = "CLASS";
        String[] classValue = { "IMAGE" };
        Datatype attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, -1, -1);
        Attribute attr = new Attribute(attrName, attrType, attrDims);
        assertNotNull(testGroup);
        assertNotNull(attr);
        attr.setValue(classValue);
        try {
            testGroup.writeMetadata(attr);
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
            if(attrobj.getType().getDatatypeClass()==Datatype.CLASS_STRING) {
                String[] value = (String[]) attrobj.getValue();
                if (value[0].equals("IMAGE"))
                    imgAttr = attrobj;
                else
                    strAttr = attrobj;
            }
            else
                arrayIntAttr = attrobj;
        }
        String[] value = (String[]) strAttr.getValue();
        log.debug("testWriteMetadata-strAttr:{}",value[0]);

        if (!value[0].equals("String attribute.")) {
            fail("writeMdata() failed.");
        }

        int[] intValue = (int[]) arrayIntAttr.getValue();
        long[] dims = arrayIntAttr.getDataDims();

        for (int i = 0; i < dims[0]; i++) {
            if (intValue[i] != i + 1) {
                fail("writeValue() failed");
            }
        }

        value = (String[]) imgAttr.getValue();
        log.debug("testWriteMetadata-imgAttr:{}",value[0]);
        if (!value[0].equals("IMAGE")) {
            fail("writeIMGMetadata() failed.");
        }
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.MetaDataContainer#removeMetadata(java.lang.Object)}.
     * <ul>
     * <li>Remove an attribute
     * </ul>
     */
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
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

}
