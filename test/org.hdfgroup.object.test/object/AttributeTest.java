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
import hdf.object.AttributeDataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;
import hdf.object.h5.H5ScalarAttr;

/**
 * @author Rishi R. Sinha
 *
 */
public class AttributeTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttributeTest.class);
    private static final H5File H5FILE = new H5File();

    private H5File testFile = null;
    private H5Group testGroup = null;
    private AttributeDataset strAttr = null;
    private AttributeDataset arrayIntAttr = null;

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
                System.out.println("AttributeTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("AttributeTest AfterClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings({ "deprecation", "rawtypes" })
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
            testFile = (H5File) H5FILE.createInstance(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testFile);

        try {
            testGroup = (H5Group) testFile.get(H5TestFile.NAME_GROUP_ATTR);
            assertNotNull(testGroup);
            List testAttrs = testGroup.getMetadata();
            assertNotNull(testAttrs);
            strAttr = (AttributeDataset) testAttrs.get(1);
            assertNotNull(strAttr);
            arrayIntAttr = (AttributeDataset) testAttrs.get(0);
            assertNotNull(arrayIntAttr);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @After
    public void removeFiles() throws Exception {
        if (testFile != null) {
            checkObjCount(testFile.getFID());
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
     * Test method for {@link hdf.object.h5.H5ScalarAttr#H5ScalarAttr(java.lang.String, hdf.object.Datatype, long[])} .
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

        Datatype attrType = null;
        try {
            attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, Datatype.NATIVE, Datatype.NATIVE);
        }
        catch (Exception ex) {
            fail("new H5Datatype failed. " + ex);
        }

        AttributeDataset attr = new H5ScalarAttr(testGroup, attrName, attrType, attrDims);
        attr.setData(classValue);
        assertNotNull(attr);
        assertEquals(classValue[0], attr.toString("|"));
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5ScalarAttr#H5ScalarAttr(java.lang.String, hdf.object.Datatype, long[], java.lang.Object)}
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

        Datatype attrType = null;
        try {
            attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, Datatype.NATIVE, Datatype.NATIVE);
        }
        catch (Exception ex) {
            fail("new H5Datatype failed. " + ex);
        }

        AttributeDataset attr = new H5ScalarAttr(testGroup, attrName, attrType, attrDims, classValue);
        assertNotNull(attr);
        assertEquals(classValue[0], attr.toString("|"));
    }

    /**
     * Test method for {@link hdf.object.Attribute#getData()}.
     *
     * Here we test:
     * <ul>
     * <li>Getting the value for the two attributes (the string attribute and
     * the int array attribute).
     * </ul>
     */
    @Test
    public void testGetData() {
        log.debug("testGetData");

        try {
            assertEquals(((String[]) strAttr.getData())[0], "String attribute.");
        }
        catch (Exception ex) {
            log.trace("testGetData(): getData() failure:", ex);
            fail("getData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testGetData(): Out of memory");
            fail("Out of memory");
        }

        try {
            assertTrue(Arrays.equals((int[]) arrayIntAttr.getData(), new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
        }
        catch (Exception ex) {
            log.trace("testGetData(): getData() failure:", ex);
            fail("getData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testGetData(): Out of memory");
            fail("Out of memory");
        }
    }

    /**
     * Test method for {@link hdf.object.AttributeDataset#setData(java.lang.Object)}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Setting new value for the two attributes (the string attribute and
     * the int array attribute).
     * </ul>
     */
    @Test
    public void testSetData() {
        String[] prevValue = null;

        log.debug("testSetData");

        try {
            prevValue = (String[]) strAttr.getData();
        }
        catch (Exception ex) {
            log.trace("testSetData(): getData() failure:", ex);
            fail("getData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testSetData(): Out of memory");
            fail("Out of memory");
        }

        strAttr.setData("Temp String Value");

        try {
            assertEquals((strAttr.getData()), "Temp String Value");
        }
        catch (Exception ex) {
            log.trace("testSetData(): getData() failure:", ex);
            fail("getData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testSetData(): Out of memory");
            fail("Out of memory");
        }

        strAttr.setData(prevValue);

        int[] intPrevValue = null;

        try {
            intPrevValue = (int[]) arrayIntAttr.getData();
        }
        catch (Exception ex) {
            log.trace("testSetData(): getData() failure:", ex);
            fail("getData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testSetData(): Out of memory");
            fail("Out of memory");
        }

        arrayIntAttr.setData(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });

        try {
            assertTrue(Arrays.equals((int[]) arrayIntAttr.getData(), new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
        }
        catch (Exception ex) {
            log.trace("testSetData(): getData() failure:", ex);
            fail("getData() failure " + ex);
        }
        catch (OutOfMemoryError e) {
            log.trace("testSetData(): Out of memory");
            fail("Out of memory");
        }

        arrayIntAttr.setData(intPrevValue);
    }

    /**
     * Test method for {@link hdf.object.AttributeDataset#getName()}.
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
    }

    /**
     * Test method for {@link hdf.object.AttributeDataset#getRank()}.
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
    }

    /**
     * Test method for {@link hdf.object.AttributeDataset#getDataDims()}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Getting the dimensionalities for the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testGetDataDims() {
        log.debug("testGetDataDims");
        assertEquals(strAttr.getDims()[0], 1);
        assertEquals(arrayIntAttr.getDims()[0], 10);
    }

    /**
     * Test method for {@link hdf.object.AttributeDataset#getType()}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Getting the value for the two attributes (the string attribute and the int array attribute).
     * </ul>
     */
    @Test
    public void testGetType() {
        log.debug("testGetType");
        assertTrue(strAttr.getDatatype().getDescription()
                .equals("String, length = 20, padding = H5T_STR_NULLTERM, cset = H5T_CSET_ASCII"));
        assertTrue(arrayIntAttr.getDatatype().getDescription().equals("32-bit integer"));
    }

    /**
     * Test method for {@link hdf.object.AttributeDataset#isUnsigned()}.
     * <p>
     * Here we test:
     * <ul>
     * <li>Check if the two attributes (the string attribute and the int array attribute) are unsigned.
     * </ul>
     */
    @Test
    public void testIsUnsigned() {
        log.debug("testIsUnsigned");
        assertFalse(strAttr.getDatatype().isUnsigned());
        assertFalse(arrayIntAttr.getDatatype().isUnsigned());
    }

    /**
     * Test method for {@link hdf.object.AttributeDataset#toString(java.lang.String)}.
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
    }

}
