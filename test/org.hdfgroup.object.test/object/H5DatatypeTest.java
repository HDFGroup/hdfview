package object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Vector;

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
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarAttr;

/**
 * TestCase for H5Datatype.
 *
 * This class tests all the public methods in H5ScalarDS class.
 *
 * The test file contains the following objects.
 *
 * <pre>
 *
 *
 *         /dataset_byte            Dataset {50, 10}
 *         /dataset_comp            Dataset {50, 10}
 *         /dataset_enum            Dataset {50, 10}
 *         /dataset_float           Dataset {50, 10}
 *         /dataset_image           Dataset {50, 10}
 *         /dataset_int             Dataset {50, 10}
 *         /dataset_str             Dataset {50, 10}
 *         /g0                      Group
 *         /g0/dataset_comp         Dataset {50, 10}
 *         /g0/dataset_int          Dataset {50, 10}
 *         /g0/datatype_float       Type
 *         /g0/datatype_int         Type
 *         /g0/datatype_str         Type
 *         /g0/g00                  Group
 *         /g0/g00/dataset_float    Dataset {50, 10}
 *         /g0_attr                 Group
 * </pre>
 *
 * We use the following template to test all the methods:
 *
 * What to test:
 * <ul>
 * <li>Test for boundary conditions
 * <ul>
 * <li>
 * </ul>
 * <li>Test for failure
 * <ul>
 * <li>
 * </ul>
 * <li>Test for success on general functionality
 * <ul>
 * <li>
 * </ul>
 * </ul>
 *
 * @author Peter Cao, The HDF Group
 */
public class H5DatatypeTest
{
    private static final Logger log = LoggerFactory.getLogger(H5DatatypeTest.class);
    private static final H5File H5FILE = new H5File();
    private static final int NLOOPS = 10;
    private static final int TEST_VALUE_INT = Integer.MAX_VALUE;
    private static final float TEST_VALUE_FLOAT = Float.MAX_VALUE;
    private static final String TEST_VALUE_STR = "Test";
    private static final String TNAME = H5TestFile.NAME_DATATYPE_INT;

    private static final int DATATYPE_SIZE = 4;
    private static final int STR_LEN = 20;
    private static final String ATTRIBUTE_STR_NAME = "strAttr";
    private static final String ATTRIBUTE_INT_ARRAY_NAME = "arrayInt";
    private static final String[] ATTRIBUTE_STR = { "String attribute." };
    private static final int[] ATTRIBUTE_INT_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    private H5Datatype typeInt = null;
    private H5Datatype typeUInt = null;
    private H5Datatype typeFloat = null;
    private H5Datatype typeStr = null;
    private H5File testFile = null;
    private H5Datatype testDatatype = null;

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
                System.out.println("H5DatatypeTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("H5DatatypeTest AfterClass: Number of IDs still open: " + openID);
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
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, H5TestFile.DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
            typeUInt = new H5Datatype(Datatype.CLASS_INTEGER, H5TestFile.DATATYPE_SIZE, Datatype.NATIVE, Datatype.SIGN_NONE);
            typeFloat = new H5Datatype(Datatype.CLASS_FLOAT, H5TestFile.DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
            typeStr = new H5Datatype(Datatype.CLASS_STRING, H5TestFile.STR_LEN, Datatype.NATIVE, Datatype.NATIVE);

            testFile = (H5File) H5FILE.createInstance(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testFile);

        try {
            testFile.open();

            testDatatype = (H5Datatype) testFile.get(TNAME);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testDatatype);
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
     * Test method for {@link hdf.object.h5.H5Datatype#open()}.
     *
     * What to test:
     * <ul>
     * <li>Open a datatype identifier
     * <li>Check the class and size of the datatype
     * <li>Close the datatype
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testOpen() {
        log.debug("testOpen");
        long tid = -1, tclass = -1, tsize = -1;

        for (int loop = 0; loop < NLOOPS; loop++) {
            tid = tclass = tsize = -1;

            try {
                tid = testDatatype.open();
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }
            assertTrue(tid > 0);

            try {
                tclass = H5.H5Tget_class(tid);
                tsize = H5.H5Tget_size(tid);
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }

            assertEquals(4, tsize);
            assertEquals(HDF5Constants.H5T_INTEGER, tclass);

            try {
                H5.H5Tclose(tid);
            }
            catch (final Exception ex) {}
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype#close(int)}.
     *
     * What to test:
     * <ul>
     * <li>Open a datatype identifier
     * <li>Check the class and size of the datatype
     * <li>Close the datatype
     * <li>Access the closed datatype (expect to fail)
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testClose() {
        log.debug("testClose");
        long tid = -1, tclass = -1, tsize = -1;

        for (int loop = 0; loop < NLOOPS; loop++) {
            tid = tclass = tsize = -1;

            try {
                tid = testDatatype.open();
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }
            assertTrue(tid > 0);

            try {
                tclass = H5.H5Tget_class(tid);
                tsize = H5.H5Tget_size(tid);
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }

            assertEquals(4, tsize);
            assertEquals(HDF5Constants.H5T_INTEGER, tclass);

            H5.H5error_off();
            try {
                H5.H5Tclose(tid);
            }
            catch (final Exception ex) {}

            try {
                tclass = H5.H5Tget_class(tid);
            }
            // Expected - intentional
            catch (final Exception ex) {}
            H5.H5error_on();
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype#toNative()}.
     *
     * What to test:
     * <ul>
     * <li>Create integer, float and string datatypes in memory
     * <li>Call toNative() to get datatype identifiers
     * <li>Check the classes and sizes of the datatypes from toNative()
     * </ul>
     */
    @Test
    public void testToNative() {
        log.debug("testToNative");
        long tid = -1, tclass = -1, tsize = -1;

        // test integer datatype
        try {
            tid = typeInt.createNative();
        }
        catch (final Exception ex) {
            fail("testToNative() failed. " + ex);
        }
        try {
            tclass = H5.H5Tget_class(tid);
            tsize = H5.H5Tget_size(tid);
        }
        catch (final Exception ex) {
            fail("open() failed. " + ex);
        }
        assertEquals(4, tsize);
        assertEquals(HDF5Constants.H5T_INTEGER, tclass);
        try {
            H5.H5Tclose(tid);
        }
        catch (final Exception ex) {}

        // test float datatype
        try {
            tid = typeFloat.createNative();
        }
        catch (final Exception ex) {
            fail("testToNative() failed. " + ex);
        }
        try {
            tclass = H5.H5Tget_class(tid);
            tsize = H5.H5Tget_size(tid);
        }
        catch (final Exception ex) {
            fail("open() failed. " + ex);
        }
        assertEquals(4, tsize);
        assertEquals(HDF5Constants.H5T_FLOAT, tclass);
        try {
            H5.H5Tclose(tid);
        }
        catch (final Exception ex) {}

        // test String datatype
        try {
            tid = typeStr.createNative();
        }
        catch (final Exception ex) {
            fail("testToNative() failed. " + ex);
        }
        try {
            tclass = H5.H5Tget_class(tid);
            tsize = H5.H5Tget_size(tid);
        }
        catch (final Exception ex) {
            fail("open() failed. " + ex);
        }
        assertEquals(H5TestFile.STR_LEN, tsize);
        assertEquals(HDF5Constants.H5T_STRING, tclass);

        try {
            H5.H5Tclose(tid);
        }
        catch (final Exception ex) {}
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype#fromNative(int)}.
     *
     * What to test:
     * <ul>
     * <li>Create empty datatype object in memory
     * <li>Test faillure on the class and size of the datatype
     * <li>Open datatype identifier from file
     * <li>Call fromNative(int tid) to fill the datatype object
     * <li>Check the class and size of the datatype
     * </ul>
     */
    @Test
    public void testFromNative() {
        log.debug("testFromNative");
        long tid = -1;
        H5.H5error_off();

        H5Datatype type = null;
        try {
            type = new H5Datatype(null, -1);
        }
        catch (Exception ex) {}

        H5.H5error_on();

        assertFalse(Datatype.CLASS_INTEGER == type.getDatatypeClass());
        assertFalse(type.getDatatypeSize() == 4);

        try {
            tid = H5.H5Topen(testFile.getFID(), TNAME, HDF5Constants.H5P_DEFAULT);
        }
        catch (final Exception ex) {
            fail("H5Topen() failed. " + ex);
        }
        assertTrue(tid > 0);

        type.fromNative(tid);

        assertTrue(Datatype.CLASS_INTEGER == type.getDatatypeClass());
        assertTrue(type.getDatatypeSize() == 4);

        try {
            H5.H5Tclose(tid);
        }
        catch (final Exception ex) {}

        long tids[] = { HDF5Constants.H5T_STD_I32LE, HDF5Constants.H5T_STD_U16LE, HDF5Constants.H5T_STD_I32BE };
        int sizes[] = { 4, 2, 4 };
        int signs[] = { Datatype.SIGN_2, Datatype.SIGN_NONE, Datatype.SIGN_2 };
        int orders[] = { Datatype.ORDER_LE, Datatype.ORDER_LE, Datatype.ORDER_BE };
        for (int i = 0; i < tids.length; i++) {
            try {
                type = new H5Datatype(null, tids[i]);
            }
            catch (Exception ex) {
                fail("new H5Datatype failed. " + ex);
            }

            assertEquals("sizes#" + i, sizes[i], type.getDatatypeSize());
            assertEquals("signs#" + i, signs[i], type.getDatatypeSign());
            assertEquals("orders#" + i, orders[i], type.getDatatypeOrder());
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype#isUnsigned()}.
     *
     * What to test:
     * <ul>
     * <li>Check unsigned integer datatype
     * <li>check signed integer datatype
     * <li>Check non-integer datatype
     * </ul>
     */
    @Test
    public void testIsUnsigned() {
        log.debug("testIsUnsigned");
        assertFalse(typeInt.isUnsigned());
        assertFalse(typeFloat.isUnsigned());
        assertFalse(typeStr.isUnsigned());
        assertTrue(typeUInt.isUnsigned());
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype#getMetadata()}.
     *
     * What to test:
     * <ul>
     * <li>Get all the attributes
     * <li>Check the content of the attributes
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMetadata() {
        log.debug("testGetMetadata");
        List attrs = null;

        try {
            attrs = (List) testDatatype.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        final int n = attrs.size();
        for (int i = 0; i < n; i++) {
            final Attribute attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getAttributeDatatype();
            if (dtype.isString()) {
                log.debug("testGetMetadata[{}] - ATTRIBUTE_STR:{} = attr:{}", i, ATTRIBUTE_STR_NAME, attr.getAttributeName());
                assertTrue(ATTRIBUTE_STR_NAME.equals(attr.getAttributeName()));

                try {
                    assertTrue(
                            ((String[]) ATTRIBUTE_STR)[0].equals(((String[]) attr.getAttributeData())[0]));
                }
                catch (Exception ex) {
                    log.trace("testGetMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testGetMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_INTEGER) {
                try {
                    log.debug("testGetMetadata[{}] - ATTRIBUTE_INT_ARRAY:{} = attr:{}", i, ATTRIBUTE_INT_ARRAY_NAME, attr.getAttributeName());
                    assertTrue(ATTRIBUTE_INT_ARRAY_NAME.equals(attr.getAttributeName()));
                    final int[] expected = (int[]) ATTRIBUTE_INT_ARRAY;
                    assertNotNull(expected);
                    log.debug("testGetMetadata - expected.length:{}", expected.length);
                    final int[] ints = (int[]) attr.getAttributeData();
                    assertNotNull(ints);
                    log.debug("testGetMetadata - ints.length:{}", ints.length);
                    for (int j = 0; j < expected.length; j++)
                        assertEquals(expected[j], ints[j]);
                }
                catch (Exception ex) {
                    log.trace("testGetMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testGetMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
        } //  (int i=0; i<n; i++) {
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype#writeMetadata(java.lang.Object)}.
     *
     * What to test:
     * <ul>
     * <li>Update the value of an existing attribute
     * <li>Attach a new attribute
     * <li>Close and re-open file to check if the change is made in file
     * <li>Restore to the orginal state
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testWriteMetadata() {
        log.debug("testWriteMetadata");
        List attrs = null;
        Attribute attr = null;

        try {
            attrs = (List) testDatatype.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        // update existing attribute
        int n = attrs.size();
        for (int i = 0; i < n; i++) {
            attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getAttributeDatatype();
            if (dtype.isString()) {
                try {
                    final String[] strs = (String[]) attr.getAttributeData();
                    strs[0] = TEST_VALUE_STR;
                }
                catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_INTEGER) {
                try {
                    final int[] ints = (int[]) attr.getAttributeData();
                    assertNotNull(ints);
                    for (int j = 0; j < ints.length; j++) {
                        ints[j] = TEST_VALUE_INT;
                    }
                }
                catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            try {
                attr.writeAttribute();
            }
            catch (final Exception ex) {
                fail("writeMetadata() failed. " + ex);
            }
        } //  (int i=0; i<n; i++)

        // attache a new attribute
        attr = (Attribute)new H5ScalarAttr(testDatatype, "float attribute", typeFloat, new long[] { 1 },
                new float[] { TEST_VALUE_FLOAT });
        try {
            attr.writeAttribute();
        }
        catch (final Exception ex) {
            fail("writeMetadata() failed. " + ex);
        }

        // close the file and reopen it
        try {
            testFile.close();
            testFile.open();
            testDatatype = (H5Datatype) testFile.get(TNAME);
        }
        catch (final Exception ex) {
            fail("testFile.get(TNAME) failed. " + ex);
        }

        // check the change in file
        try {
            attrs = (List) testDatatype.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        n = attrs.size();
        Attribute newAttr = null;
        for (int i = 0; i < n; i++) {
            attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getAttributeDatatype();
            if (dtype.isString()) {
                try {
                    assertTrue(ATTRIBUTE_STR_NAME.equals(attr.getAttributeName()));
                    assertTrue(TEST_VALUE_STR.equals(((String[]) attr.getAttributeData())[0]));
                }
                catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_INTEGER) {
                try {
                    assertTrue(ATTRIBUTE_INT_ARRAY_NAME.equals(attr.getAttributeName()));
                    final int[] ints = (int[]) attr.getAttributeData();
                    assertNotNull(ints);
                    for (int j = 0; j < ints.length; j++) {
                        assertEquals(TEST_VALUE_INT, ints[j]);
                    }
                }
                catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_FLOAT) {
                try {
                    newAttr = attr;
                    final float[] floats = (float[]) attr.getAttributeData();
                    assertEquals(TEST_VALUE_FLOAT, floats[0], Float.MIN_VALUE);
                }
                catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
        } //  (int i=0; i<n; i++) {

        // remove the new attribute
        try {
            testDatatype.removeMetadata(newAttr);
        }
        catch (final Exception ex) {
            fail("removeMetadata() failed. " + ex);
        }

        // set the value to original
        n = attrs.size();
        for (int i = 0; i < n; i++) {
            attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getAttributeDatatype();
            if (dtype.isString()) {
                try {
                    final String[] strs = (String[]) attr.getAttributeData();
                    strs[0] = ((String[]) ATTRIBUTE_STR)[0];
                }
                catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_INTEGER) {
                try {
                    final int[] ints = (int[]) attr.getAttributeData();
                    assertNotNull(ints);
                    for (int j = 0; j < ints.length; j++) {
                        final int[] expected = (int[]) ATTRIBUTE_INT_ARRAY;
                        ints[j] = expected[j];
                    }
                }
                catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                }
                catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            try {
                attr.writeAttribute();
            }
            catch (final Exception ex) {
                fail("writeMetadata() failed. " + ex);
            }
        } //  (int i=0; i<n; i++) {
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype#removeMetadata(java.lang.Object)}.
     *
     * What to test:
     * <ul>
     * <li>Remove all existing attributes
     * <li>Close and reopen file to check if all attribute are removed from file
     * <li>Restore to the orginal state
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRemoveMetadata() {
        log.debug("testRemoveMetadata");
        List attrs = null;
        try {
            attrs = (List) testDatatype.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        // remove all attributes
        final int n = attrs.size();
        final Object[] arrayAttr = attrs.toArray();
        for (int i = 0; i < n; i++) {
            try {
                testDatatype.removeMetadata(arrayAttr[i]);
            }
            catch (final Exception ex) {
                fail("removeMetadata() failed. " + ex);
            }
        }

        // close the file and reopen it
        try {
            testFile.close();
            testFile.open();
            testDatatype = (H5Datatype) testFile.get(TNAME);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
        }
        attrs = null;

        try {
            attrs = (List) testDatatype.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertFalse(attrs.size() > 0);

        // restore to the original
        try {
            H5TestFile.ATTRIBUTE_STR.setParentObject(testDatatype);
            H5TestFile.ATTRIBUTE_INT_ARRAY.setParentObject(testDatatype);
            H5TestFile.ATTRIBUTE_STR.write();
            H5TestFile.ATTRIBUTE_INT_ARRAY.write();
        }
        catch (final Exception ex) {
            fail("writeMetadata() failed. " + ex);
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype} IsSerializable.
     */
    @Test
    public void testIsSerializable() {
        log.debug("testIsSerializable");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(testDatatype);
            oos.close();
        }
        catch (IOException err) {
            err.printStackTrace();
            fail("ObjectOutputStream failed: " + err);
        }
        assertTrue(out.toByteArray().length > 0);
    }

    /**
     * Test method for {@link hdf.object.h5.H5Datatype} SerializeToDisk.
     *
     * What to test:
     * <ul>
     * <li>serialize a dataset identifier
     * <li>deserialize a dataset identifier
     * <li>Open a datatype identifier
     * <li>Check the class and size of the datatype
     * <li>Close the datatype
     * </ul>
     */
    @Test
    public void testSerializeToDisk() {
        log.debug("testSerializeToDisk");
        try {
            FileOutputStream fos = new FileOutputStream("temph5dtype.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(testDatatype);
            oos.close();
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        H5Datatype test = null;
        try {
            FileInputStream fis = new FileInputStream("temph5dtype.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            test = (hdf.object.h5.H5Datatype) ois.readObject();
            ois.close();

            // Clean up the file
            new File("temph5dtype.ser").delete();
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        long tid = -1, tclass = -1, tsize = -1;

        for (int loop = 0; loop < NLOOPS; loop++) {
            tid = tclass = tsize = -1;

            try {
                tid = test.open();
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }
            assertTrue(tid > 0);

            try {
                tclass = H5.H5Tget_class(tid);
                tsize = H5.H5Tget_size(tid);
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }

            assertEquals(4, tsize);
            assertEquals(HDF5Constants.H5T_INTEGER, tclass);

            try {
                H5.H5Tclose(tid);
            }
            catch (final Exception ex) {}
        }
    }
}
