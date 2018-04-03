package test.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

/**
 * @author Rishi R. Sinha
 *
 */
public class AttributeTest {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttributeTest.class);
    private static final H5File H5FILE = new H5File();

    private H5File testFile = null;
    private H5Group testGroup = null;
    private Attribute strAttr = null;
    private Attribute arrayIntAttr = null;

    @BeforeClass
    public static void createFile() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("AttributTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("AttributTest AfterClass: Number of IDs still open: " + openID);
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
        testGroup = (H5Group) testFile.get(H5TestFile.NAME_GROUP_ATTR);
        assertNotNull(testGroup);
        List testAttrs = testGroup.getMetadata();
        assertNotNull(testAttrs);
        strAttr = (Attribute) testAttrs.get(1);
        assertNotNull(strAttr);
        arrayIntAttr = (Attribute) testAttrs.get(0);
        assertNotNull(arrayIntAttr);
    }

    @After
    public void removeFiles() throws Exception {
        if (testFile != null) {
            try {
                testFile.close();
            }
            catch (final Exception ex) {
                log.debug("testfile close failure: ", ex);
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
     * Test method for {@link hdf.object.Attribute#Attribute(java.lang.String, hdf.object.Datatype, long[])} .
     * <p>
     * Here we test:
     * <ul>
     * <li>Creating a new attribute with no value.
     * <li>Setting the attribute value.
     * </ul>
     *
     */
    @Test
    public void testAttributeStringDatatypeLongArray() {
        log.debug("testAttributeStringDatatypeLongArray");
        long[] attrDims = { 1 };
        String attrName = "CLASS";
        String[] classValue = { "IMAGE" };
        Datatype attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, -1, -1);
        Attribute attr = new Attribute(attrName, attrType, attrDims);
        attr.setData(classValue);
        assertNotNull(attr);
        assertEquals(classValue[0], attr.toString("|"));
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
     * Test method for
     * {@link hdf.object.Attribute#Attribute(java.lang.String, hdf.object.Datatype, long[], java.lang.Object)}
     * .
     * <p>
     * Here we test:
     * <ul>
     * <li>Creating a new attribute with a value.
     * </ul>
     */
    @Test
    public void testAttributeStringDatatypeLongArrayObject() {
        log.debug("testAttributeStringDatatypeLongArrayObject");
        long[] attrDims = { 1 };
        String attrName = "CLASS";
        String[] classValue = { "IMAGE" };
        Datatype attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, -1, -1);
        Attribute attr = new Attribute(attrName, attrType, attrDims, classValue);
        assertNotNull(attr);
        assertEquals(classValue[0], attr.toString("|"));
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
     * Test method for {@link hdf.object.Attribute#getValue()}.
     *
     * Here we test:
     * <ul>
     * <li>Getting the value for the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testGetValue() {
        log.debug("testGetValue");
        assertEquals(((String[]) strAttr.getData())[0], "String attribute.");
        assertTrue(Arrays.equals((int[]) arrayIntAttr.getData(), new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
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
     * Test method for {@link hdf.object.Attribute#setValue(java.lang.Object)}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Setting new value for the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testSetValue() {
        log.debug("testSetValue");
        String[] prevValue = (String[]) strAttr.getData();
        strAttr.setData("Temp String Value");
        assertEquals((strAttr.getData()), "Temp String Value");
        strAttr.setData(prevValue);

        int[] intPrevValue = (int[]) arrayIntAttr.getData();
        arrayIntAttr.setData(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        assertTrue(Arrays.equals((int[]) arrayIntAttr.getData(), new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
        arrayIntAttr.setData(intPrevValue);
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
     * Test method for {@link hdf.object.Attribute#getName()}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Getting the names of the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testGetName() {
        log.debug("testGetName");
        assertTrue(strAttr.getName().equals("strAttr"));
        assertTrue(arrayIntAttr.getName().equals("arrayInt"));
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
     * Test method for {@link hdf.object.Attribute#getRank()}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Getting the rank for the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testGetRank() {
        log.debug("testGetRank");
        assertEquals(strAttr.getRank(), 1);
        assertEquals(arrayIntAttr.getRank(), 1);
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
     * Test method for {@link hdf.object.Attribute#getDataDims()}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Getting the dimensionalities for the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testGetDataDims() {
        log.debug("testGetDataDims");
        assertEquals(strAttr.getDataDims()[0], 1);
        assertEquals(arrayIntAttr.getDataDims()[0], 10);
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
     * Test method for {@link hdf.object.Attribute#getType()}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Getting the value for the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testGetType() {
        log.debug("testGetType");
        assertTrue(strAttr.getDatatype().getDatatypeDescription().equals("String, length = 20"));
        assertTrue(arrayIntAttr.getDatatype().getDatatypeDescription().equals("32-bit integer"));
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
     * Test method for {@link hdf.object.Attribute#isUnsigned()}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Check if the two attributes (the string attribute and the int array attribute) are unsigned.
     * </ul>
     */
    @Test
    public void testIsUnsigned() {
        log.debug("testIsUnsigned");
        assertFalse(strAttr.isUnsigned());
        assertFalse(arrayIntAttr.isUnsigned());
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
     * Test method for {@link hdf.object.Attribute#toString(java.lang.String)}.
     * <p>
     * Here we test:
     * <ul>
     * <li>the toString method for the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testToStringString() {
        log.debug("testToStringString");
        assertTrue(strAttr.toString(",").equals("String attribute."));
        assertTrue(arrayIntAttr.toString(",").equals("1,2,3,4,5,6,7,8,9,10"));
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
