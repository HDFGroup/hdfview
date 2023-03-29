package object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Vector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;

import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.ScalarDS;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5ScalarDS;

/**
 * TestCase for H5ScalarDS.
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
public class H5ScalarDSTest
{
    private static final Logger log = LoggerFactory.getLogger(H5ScalarDSTest.class);
    private static final H5File H5FILE = new H5File();
    private static final int NLOOPS = 10;
    private static final int TEST_VALUE_INT = Integer.MAX_VALUE;
    private static final float TEST_VALUE_FLOAT = Float.MAX_VALUE;
    private static final String TEST_VALUE_STR = "H5ScalarDSTest";
    private static final String DNAME = H5TestFile.NAME_DATASET_INT;
    private static final String DNAME_SUB = H5TestFile.NAME_DATASET_INT_SUB;

    private static final int DATATYPE_SIZE = 4;
    private static final int STR_LEN = 20;
    private static final String ATTRIBUTE_STR_NAME = "strAttr";
    private static final String ATTRIBUTE_INT_ARRAY_NAME = "arrayInt";
    private static final String[] ATTRIBUTE_STR = { "String attribute." };
    private static final int[] ATTRIBUTE_INT_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    private H5Datatype typeInt = null;
    private H5Datatype typeFloat = null;
    private H5Datatype typeStr = null;
    private H5File testFile = null;
    private H5ScalarDS testDataset = null;

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
                System.out.println("H5ScalarDSTest BeforeClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            H5File file = H5TestFile.createTestFile(null);
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
                System.out.println("H5ScalarDSTest AfterClass: Number of IDs still open: " + openID);
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

            testDataset = (H5ScalarDS) testFile.get(DNAME);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testDataset);
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
     * Test method for {@link hdf.object.h5.H5ScalarDS#setName(java.lang.String)}.
     *
     * What to test:
     * <ul>
     * <li>Test for boundary conditions
     * <ul>
     * <li>Set name to null
     * </ul>
     * <li>Test for failure
     * <ul>
     * <li>Set a name that already exists in file.
     * </ul>
     * <li>Test for general functionality
     * <ul>
     * <li>change the dataset name
     * <li>close/re-open the file
     * <li>get the dataset with the new name
     * <li>failure test: get the dataset with the original name
     * <li>set the name back to the original name
     * </ul>
     * </ul>
     */
    @Test
    public void testSetName() {
        log.debug("testSetName");
        final String newName = "tmpName";

        // test set name to null
        try {
            H5.H5error_off();
            testDataset.setName(null);
            H5.H5error_on();
        }
        catch (final Exception ex) {} // Expected - intentional

        // set to an existing name
        try {
            H5.H5error_off();
            testDataset.setName(H5TestFile.NAME_DATASET_FLOAT);
            H5.H5error_on();
        }
        catch (final Exception ex) {} // Expected - intentional

        try {
            testDataset.setName(newName);
        }
        catch (final Exception ex) {
            fail("setName() failed. " + ex);
        }

        // close the file and reopen it
        try {
            testFile.close();
            testFile.open();
            testDataset = (H5ScalarDS) testFile.get(newName);
        }
        catch (final Exception ex) {
            fail("get(newName) failed. " + ex);
        }

        // test the old name
        H5ScalarDS tmpDset = null;
        try {
            H5.H5error_off();
            tmpDset = (H5ScalarDS) testFile.get(DNAME);
            H5.H5error_on();
        }
        catch (final Exception ex) {
            fail("get(DNAME) get(oldname) failed. " + ex);
        }
        assertNull("The dataset should be null because it has been renamed", tmpDset);

        // set back the original name
        try {
            testDataset.setName(DNAME);
        }
        catch (final Exception ex) {
            fail("setName() failed. " + ex);
        }

        // make sure the dataset is OK
        try {
            testDataset = (H5ScalarDS) testFile.get(DNAME);
        }
        catch (final Exception ex) {
            fail("get(DNAME) failed. " + ex);
        }
        assertNotNull(testDataset);
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#open()}.
     *
     * What to test:
     * <ul>
     * <li>open a dataset identifier
     * <li>get datatype and dataspace identifier for the dataset
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testOpen() {
        log.debug("testOpen");
        long did = -1, tid = -1, sid = -1;

        for (int loop = 0; loop < NLOOPS; loop++) {
            did = tid = sid = -1;
            try {
                did = testDataset.open();
                if (did >= 0) {
                    tid = H5.H5Dget_type(did);
                    sid = H5.H5Dget_space(did);
                }
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }

            assertTrue(did > 0);
            assertTrue(tid > 0);
            assertTrue(sid > 0);

            try {
                H5.H5Tclose(tid);
            }
            catch (final Exception ex) {}
            try {
                H5.H5Sclose(sid);
            }
            catch (final Exception ex) {}
            try {
                H5.H5Dclose(did);
            }
            catch (final Exception ex) {}
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#close(int)}.
     *
     * What to test:
     * <ul>
     * <li>open a dataset identifier
     * <li>get datatype and dataspace identifier for the dataset
     * <li>close dataset
     * <li>failure test for the closed did
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testClose() {
        log.debug("testClose");
        long did = -1, tid = -1, sid = -1;

        for (int loop = 0; loop < NLOOPS; loop++) {
            did = tid = sid = -1;
            try {
                did = testDataset.open();
                if (did >= 0) {
                    tid = H5.H5Dget_type(did);
                    sid = H5.H5Dget_space(did);
                }
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }

            assertTrue(did > 0);
            assertTrue(tid > 0);
            assertTrue(sid > 0);

            try {
                H5.H5Tclose(tid);
            }
            catch (final Exception ex) {}
            try {
                H5.H5Sclose(sid);
            }
            catch (final Exception ex) {}

            try {
                testDataset.close(did);
            }
            catch (final Exception ex) {
                fail("close() failed. " + ex);
            }

            H5.H5error_off();
            // dataset is closed, expect to fail
            try {
                tid = H5.H5Dget_type(did);
            }
            catch (final Exception ex) {
                tid = -1; // Expected - intentional
            }
            assertTrue(tid < 0);

            try {
                sid = H5.H5Dget_space(did);
            }
            catch (final Exception ex) {
                sid = -1; // Expected - intentional
            }
            assertTrue(sid < 0);
            H5.H5error_on();
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#clear()}.
     *
     * What to test:
     * <ul>
     * <li>Read data/attributes from file
     * <li>clear the dataet
     * <li>make sure that the data is empty
     * <li>make sure that the attribute list is empty
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testClear() {
        log.debug("testClear");
        Object data = null;

        try {
            data = testDataset.getData();
        }
        catch (final Exception ex) {
            fail("getData() failed. " + ex);
        }
        assertNotNull(data);
        assertTrue(Array.getLength(data) > 0);

        List attrs = null;
        try {
            attrs = (List) testDataset.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }

        // clear up the dataset
        testDataset.clear();

        // attribute is empty
        try {
            attrs = (List) testDataset.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertTrue(attrs.size() <= 0);
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#init()}.
     *
     * What to test:
     * <ul>
     * <li>call init()
     * <li>Select a subset
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testInit() {
        log.debug("testInit");

        // Close default testFile
        try {
            testFile.close();
            log.trace("testInit close testfile");
        }
        catch (final Exception ex) {}

        for (int loop = 0; loop < NLOOPS; loop++) {
            // Reopen default testFile
            try {
                testFile = (H5File) H5FILE.createInstance(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
                log.trace("testInit createInstance testfile loop={}", loop);

                testDataset = (H5ScalarDS) testFile.get(DNAME);
                log.trace("testInit get testfile testDataset");
            }
            catch (final Exception ex) {
                fail("get(DNAME) failed. " + ex);
            }

            testDataset.init();
            log.trace("testInit testfile testDataset.init");

            // test the rank
            final int rank = testDataset.getRank();
            assertEquals(H5TestFile.RANK, rank);

            // test the dimension sizes
            final long[] dims = testDataset.getDims();
            assertNotNull(dims);
            for (int i = 0; i < rank; i++)
                assertEquals(H5TestFile.DIMs[i], dims[i]);

            // start at 0
            final long[] start = testDataset.getStartDims();
            assertNotNull(start);
            for (int i = 0; i < rank; i++)
                assertEquals(0, start[i]);

            // test selection
            final long[] selectedDims = testDataset.getSelectedDims();
            final int[] selectedIndex = testDataset.getSelectedIndex();
            assertNotNull(selectedDims);
            assertNotNull(selectedIndex);
            if (rank == 1) {
                assertEquals(0, selectedIndex[0]);
                assertEquals(dims[0], selectedDims[0]);
            }
            else if (rank == 2) {
                assertEquals(0, selectedIndex[0]);
                assertEquals(1, selectedIndex[1]);
                assertEquals(dims[0], selectedDims[0]);
                assertEquals(dims[1], selectedDims[1]);
            }
            else if (rank > 2) {
                assertEquals(rank - 2, selectedIndex[0]); // columns
                assertEquals(rank - 1, selectedIndex[1]); // rows
                assertEquals(rank - 3, selectedIndex[2]);
                assertEquals(dims[rank - 1], selectedDims[rank - 1]);
                assertEquals(dims[rank - 2], selectedDims[rank - 2]);
            }

            try {
                testFile.close();
                log.trace("testInit close testfile loop={}", loop);
            }
            catch (final Exception ex) {
                System.err.println("testFile.close() failed. " + ex);
            }
        } //  (int loop=0; loop<NLOOPS; loop++)
        // clear the testFile handle now
        if (testFile != null)
            testFile = null;
    } // public final void testInit() {

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#read()}.
     *
     * What to test:
     * <ul>
     * <li>Read the whole dataset of the test dataset
     * <li>read a subset of the test dataset
     * <li>Repeat all above
     * <li>Read all types scalar datasets
     *
     * </ul>
     */
    @Test
    public void testRead() {
        log.debug("testRead");
        for (int loop = 0; loop < NLOOPS; loop++) {
            testDataset.init();
            int[] ints = null;

            // read the whole dataset
            try {
                ints = (int[]) testDataset.getData();
            }
            catch (final Exception ex) {
                fail("testDataset.getData() failed. " + ex);
            }
            assertNotNull(ints);

            // check the data content
            for (int i = 0; i < ints.length; i++)
                assertEquals(H5TestFile.DATA_INT[i], ints[i]);
        } //  (int loop=0; loop<NLOOPS; loop++)

        // Close default testFile
        closeFile();

        // read all types of scalar datasets
        Dataset dset = null;
        final String dnames[] = { H5TestFile.NAME_DATASET_CHAR, H5TestFile.NAME_DATASET_ENUM,
                H5TestFile.NAME_DATASET_FLOAT, H5TestFile.NAME_DATASET_IMAGE, H5TestFile.NAME_DATASET_INT,
                H5TestFile.NAME_DATASET_STR, H5TestFile.NAME_DATASET_INT_SUB, H5TestFile.NAME_DATASET_FLOAT_SUB_SUB };

        for (int i = 0; i < NLOOPS; i++) {
            final H5File file = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.READ);

            try {
                // datasets
                for (int j = 0; j < dnames.length; j++) {
                    dset = (Dataset) file.get(dnames[j]);
                    dset.getData();
                }
            }
            catch (final Exception ex) {
                fail("file.get() failed. " + ex);
            }

            try {
                file.close();
            }
            catch (final Exception ex) {
                fail("file.close() failed. " + ex);
            }
        } //  (int i=0; i<NLOOPS; i++)
        // clear the testFile handle now
        if (testFile != null)
            testFile = null;
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#read()}.
     *
     * What to test:
     * <ul>
     * <li>Read an external dataset
     * </ul>
     */
    @Ignore
    public void testReadExt() {
        log.debug("testReadExt");

        Dataset dset = null;
        H5File file = null;

        try {
            file = new H5File("test/object/h5ex_d_extern.hdf5");
            try {
                dset = (Dataset) file.get("/DS1");
            }
            catch (Exception ex) {
                fail("Failed to get datset.");
            }

            assertNotNull(dset);

            try {
                dset.read();
            }
            catch (Exception ex) {
                fail("Failed to read data from an external dataset.");
            }
        }
        catch (Exception ex) {
            fail("Failed to open an external file.");
        }
        finally {
            if (file != null)
                try {
                    file.close();
                }
                catch(Exception ex){
                    ex.printStackTrace();
                }
        }
        log.debug("testReadExt finish");
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#read()}.
     *
     * What to test:
     * <ul>
     * <li>Read data row by row
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testReadByRow() {
        log.debug("testReadByRow");
        int[] data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            log.trace("testReadByRow testDataset loop={}", loop);
            testDataset.init();

            // read data row by row
            final long nrows = testDataset.getHeight();
            log.trace("testReadByRow testDataset nrows={}", nrows);
            for (int i = 0; i < nrows; i++) {
                log.trace("testReadByRow testDataset loop={} row={}", loop, i);
                testDataset.clearData();

                final int rank = testDataset.getRank();
                final long[] start = testDataset.getStartDims();
                final long[] count = testDataset.getSelectedDims();

                // select one row only
                for (int j = 0; j < rank; j++)
                    count[j] = 1;

                // select different rows
                start[0] = i;

                try {
                    data = (int[]) testDataset.getData();
                    log.trace("testReadByRow testDataset.getData loop={} row={}", loop, i);
                }
                catch (final Exception ex) {
                    fail("getData() failed. " + ex);
                }
                assertNotNull(data);

                final int idx = (int) H5TestFile.DIM2 * i;
                log.trace("testReadByRow testDataset.DATA_INT[{}]={} loop={} row={}", idx, H5TestFile.DATA_INT[idx], loop, i);
                log.trace("testReadByRow testDataset.data[0]={} loop={} row={}", data[0], loop, i);
                assertEquals(H5TestFile.DATA_INT[idx], data[0]);
            } //  (int i=0; i<nrows; i++) {
        } //  (int loop=0; loop<NLOOPS; loop++) {
        log.trace("testReadByRow testDataset finished");
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#readBytes()}.
     *
     * What to test:
     * <ul>
     * <li>Read the whole dataset in a byte buffer
     * <li>check the data size
     * </ul>
     */
    @Test
    public void testReadBytes() {
        log.debug("testReadBytes");
        byte[] data = null;

        try {
            data = testDataset.readBytes();
        }
        catch (final Exception ex) {
            fail("readBytes() failed. " + ex);
        }
        assertNotNull(data);

        final int n = Array.getLength(data);
        final int expected = H5TestFile.DIM_SIZE * 4;

        assertEquals(expected, n);
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#write(java.lang.Object)}.
     *
     * What to test:
     * <ul>
     * <li>Read/write the whole dataset
     * <li>Repeat all above
     * <li>write the original data back to file
     * </ul>
     */
    @Test
    public void testWriteObject() {
        log.debug("testWriteObject");
        int[] data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            // read the whole dataset by default
            testDataset.init();

            try {
                data = (int[]) testDataset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);
            assertEquals(H5TestFile.DIM_SIZE, Array.getLength(data));

            // change the data value
            for (int i = 0; i < H5TestFile.DIM_SIZE; i++)
                data[i] = TEST_VALUE_INT;

            // write the data to file
            try {
                testDataset.write(data);
            }
            catch (final Exception ex) {
                fail("write() failed. " + ex);
            }

            // close the file and reopen it
            try {
                testFile.close();
                testFile.open();
                testDataset = (H5ScalarDS) testFile.get(DNAME);
            }
            catch (final Exception ex) {
                fail("get(DNAME) failed. " + ex);
            }

            // read the data into memory to make sure the data is correct
            testDataset.init();
            testDataset.clearData();

            try {
                data = (int[]) testDataset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);
            assertEquals(H5TestFile.DIM_SIZE, Array.getLength(data));

            // check the data values
            for (int i = 0; i < H5TestFile.DIM_SIZE; i++) {
                assertEquals(TEST_VALUE_INT, data[i]);
            }

            // write the original data into file
            try {
                testDataset.write(H5TestFile.DATA_INT);
            }
            catch (final Exception ex) {
                fail("write() failed. " + ex);
            }
        } //  (int loop=0; loop<NLOOPS; loop++) {
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#write(java.lang.Object)}.
     *
     * What to test:
     * <ul>
     * <li>Read/write a subset of dataset
     * <li>Repeat all above
     * <li>write the original data back to file
     * </ul>
     */
    @Test
    public void testWriteSubset() {
        log.debug("testWriteSubset");
        int[] data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            // read the whole dataset by default
            testDataset.init();

            // write a subset: the first half of the dataset
            final int rank = testDataset.getRank();
            final long[] dims = testDataset.getDims();
            long[] count = testDataset.getSelectedDims();

            // select the first 1/2 of the datast
            long size = 1;
            for (int j = 0; j < rank; j++) {
                count[j] = dims[j] / 2;
                size *= count[j];
            }

            data = new int[(int) size];
            for (int j = 0; j < size; j++)
                data[j] = TEST_VALUE_INT;

            // write the data to file
            try {
                testDataset.write(data);
            }
            catch (final Exception ex) {
                fail("write() failed. " + ex);
            }

            // close the file and reopen it
            try {
                testFile.close();
                testFile.open();
                testDataset = (H5ScalarDS) testFile.get(DNAME);
            }
            catch (final Exception ex) {
                fail("get(DNAME) failed. " + ex);
            }

            // read the data into memory to make sure the data is correct
            testDataset.init();
            testDataset.clearData();

            // select the first 1/2 of the datast
            count = testDataset.getSelectedDims();
            for (int j = 0; j < rank; j++)
                count[j] = dims[j] / 2;

            try {
                data = (int[]) testDataset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);
            assertEquals(size, Array.getLength(data));

            // check the data values
            for (int i = 0; i < size; i++)
                assertEquals(TEST_VALUE_INT, data[i]);

            // write the original data into file
            for (int j = 0; j < rank; j++)
                count[j] = dims[j];
            try {
                testDataset.write(H5TestFile.DATA_INT);
            }
            catch (final Exception ex) {
                fail("write() failed. " + ex);
            }
        } //  (int loop=0; loop<NLOOPS; loop++) {
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#write(java.lang.Object)}.
     *
     * What to test:
     * <ul>
     * <li>Read/write a subset of null strings
     * <li>Repeat all above
     * <li>write the original data back to file
     * </ul>
     */
    @Test
    public void testReadWriteNullStr() {
        log.debug("testReadWriteNullStr");
        String[] data = null;
        String[] nullStrs = null;
        H5ScalarDS dset = null;

        try {
            dset = (H5ScalarDS) testFile.get(H5TestFile.NAME_DATASET_STR);
        }
        catch (Exception ex) {
            dset = null;
        }
        assertNotNull(dset);

        try {
            data = (String[]) dset.getData();
        }
        catch (Exception ex) {
            data = null;
        }
        assertNotNull(data);
        assertTrue(data.length > 0);

        nullStrs = new String[data.length];

        for (int i = 0; i < data.length; i++)
            nullStrs[i] = null;

        // write null strings
        try {
            dset.write(nullStrs);
        }
        catch (Exception ex) {
            fail("Write null strings failed. " + ex);
        }

        // read null strings
        try {
            dset.clearData();
            dset.init();
            nullStrs = (String[]) dset.read();
        }
        catch (Exception ex) {
            fail("Read null strings failed. " + ex);
            nullStrs = null;
        }
        assertNotNull(nullStrs);

        // make sure all the strings are empty
        for (int i = 0; i < data.length; i++) {
            assertNotNull(nullStrs[i]);
            assertTrue(nullStrs[i].length() == 0);
        }

        // restore to the original state
        //dset.init();
        try {
            dset.write(data);
        }
        catch (Exception ex) {
            fail("Write null strings failed. " + ex);
        }

        // read data back and check it is to the original state
        try {
            dset.clearData();
            dset.init();
            nullStrs = (String[]) dset.read();
        }
        catch (Exception ex) {
            fail("Read null strings failed. " + ex);
            nullStrs = null;
        }
        assertNotNull(nullStrs);
        for (int i = 0; i < data.length; i++)
            assertTrue(data[i].equals(nullStrs[i]));
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5ScalarDS#copy(hdf.object.Group, java.lang.String, long[], java.lang.Object)} .
     *
     * What to test:
     * <ul>
     * <li>Copy all scalar datasets to a new file
     * <li>Check the content of new datasts
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testCopy() {
        log.debug("testCopy");
        Dataset dset = null, dsetNew = null;
        H5File tmpFile = null;

        final String DNAMES[] = { H5TestFile.NAME_DATASET_CHAR, H5TestFile.NAME_DATASET_ENUM,
                H5TestFile.NAME_DATASET_FLOAT, H5TestFile.NAME_DATASET_IMAGE, H5TestFile.NAME_DATASET_INT,
                H5TestFile.NAME_DATASET_STR };

        try {
            testFile.close();
            log.trace("testCopy close testfile");
        }
        catch (final Exception ex) {}

        for (int loop = 0; loop < NLOOPS; loop++) {
            tmpFile = new H5File("H5ScalarDS_testCopy.h5", FileFormat.CREATE);
            log.trace("testCopy new H5File loop={}", loop);

            try {
                // test two open options: open full tree or open individual
                // object only
                for (int openOption = 0; openOption < 2; openOption++) {
                    log.trace("testCopy loop={} openOption={}", loop, openOption);
                    if (openOption == 0) {
                        try {
                            testFile.open(); // open the full tree
                            log.trace("testCopy testFile open loop={} openOption={}", loop, openOption);
                        }
                        catch (final Exception ex) {
                            System.err.println("file.open(). " + ex);
                        }
                    }

                    try {
                        final Group rootGrp = (Group) tmpFile.get("/");
                        log.trace("testCopy get tmpFile rootGrp");

                        // datasets
                        for (int j = 0; j < DNAMES.length; j++) {
                            dset = (Dataset) testFile.get(DNAMES[j]);
                            final Object data = dset.getData();

                            log.trace("testCopy copy data into a new dataset[{}]={}", j, DNAMES[j]);
                            if (dset instanceof ScalarDS) {
                                dsetNew = dset.copy(rootGrp, DNAMES[j] + "_copy" + openOption, H5TestFile.DIMs, data);
                                assertNotNull(dsetNew);
                                final Object dataCopy = dsetNew.getData();
                                final int size = Array.getLength(data);
                                for (int k = 0; k < size; k++)
                                    assertEquals(Array.get(data, k), Array.get(dataCopy, k));
                                log.trace("testCopy copy data into a new dataset size={}", size);
                            }
                        }
                    }
                    catch (final Exception ex) {
                        fail("file.get(). " + ex);
                    }

                    try {
                        testFile.close();
                    }
                    catch (final Exception ex) {
                        System.err.println("testFile.close() failed. " + ex);
                    }

                    log.trace("testCopy check obj count");
                    checkObjCount(tmpFile.getFID());
                } //  (int openOption=0; openOption<2; openOption++)
            }
            finally {
                try {
                    tmpFile.close();
                    log.trace("testCopy close obj tmpFile");
                }
                catch (final Exception ex) {
                    System.err.println("tmpFile.close() failed. " + ex);
                }
                // delete the testing file
                if (tmpFile != null)
                    tmpFile.delete();
            }
        } //  (int loop=0; loop<NLOOPS; loop++) {
        // clear the testFile handle now
        if (testFile != null)
            testFile = null;
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#getDatatype()}.
     *
     * What to test:
     * <ul>
     * <li>Get datatype
     * <li>Check the class and size of the datatype
     * </ul>
     */
    @Test
    public void testGetDatatype() {
        log.debug("testGetDatatype");
        H5Datatype dtype = null;

        try {
            dtype = (H5Datatype) testDataset.getDatatype();
        }
        catch (final Exception ex) {
            fail("testDataset.getDatatype() failed. " + ex);
        }

        assertNotNull(dtype);
        assertEquals(Datatype.CLASS_INTEGER, dtype.getDatatypeClass());
        assertEquals(H5TestFile.DATATYPE_SIZE, dtype.getDatatypeSize());
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#getPalette()}.
     *
     * What to test:
     * <ul>
     * <li>Get the palette from an image
     * <li>Check the content of the palette
     * </ul>
     */
    @Test
    public void testGetPalette() {
        log.debug("testGetPalette");
        ScalarDS img = null;

        try {
            img = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_IMAGE);
        }
        catch (final Exception ex) {
            fail("testFile.get failed. " + ex);
        }
        assertNotNull(img);
        img.init();

        log.debug("testGetPalette pal");
        final byte[][] pal = img.getPalette();
        assertNotNull(pal);

        for (int i = 0; i < 256; i++) {
            assertEquals(H5TestFile.DATA_PALETTE[i * 3], pal[0][i]);
            assertEquals(H5TestFile.DATA_PALETTE[i * 3 + 1], pal[1][i]);
            assertEquals(H5TestFile.DATA_PALETTE[i * 3 + 2], pal[2][i]);
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#readPalette(int)}.
     *
     * What to test:
     * <ul>
     * <li>Get the palette from an image
     * <li>Check the content of the palette
     * </ul>
     */
    @Test
    public void testReadPalette() {
        log.debug("testReadPalette");
        ScalarDS img = null;

        try {
            img = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_IMAGE);
        }
        catch (final Exception ex) {
            fail("testFile.get failed. " + ex);
        }
        assertNotNull(img);

        log.debug("testReadPalette pal");
        final byte[][] pal = img.readPalette(0);
        assertNotNull(pal);

        for (int i = 0; i < 256; i++) {
            assertEquals(H5TestFile.DATA_PALETTE[i * 3], pal[0][i]);
            assertEquals(H5TestFile.DATA_PALETTE[i * 3 + 1], pal[1][i]);
            assertEquals(H5TestFile.DATA_PALETTE[i * 3 + 2], pal[2][i]);
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#getNumberOfPalettes()}.
     *
     * What to test:
     * <ul>
     * <li>Check the number of palette references
     * </ul>
     */
    @Test
    public void testGetNumberOfPalettes() {
        log.debug("testNumberOfPalettes");
        ScalarDS img = null;

        try {
            img = (ScalarDS) testFile.get(H5TestFile.NAME_DATASET_IMAGE);
        }
        catch (final Exception ex) {
            fail("testFile.get failed. " + ex);
        }
        assertNotNull(img);
        img.init();

        log.debug("testGetNumberOfPalettes count");
        final int count = img.getNumberOfPalettes();
        assertTrue(count > 0);
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5ScalarDS#H5ScalarDS(hdf.object.FileFormat, java.lang.String, java.lang.String)}
     *
     * What to test:
     * <ul>
     * <li>Construct an H5ScalarDS object that exists in file
     * <ul>
     * <li>new H5ScalarDS (file, null, fullpath)
     * <li>new H5ScalarDS (file, fullname, null)
     * <li>new H5ScalarDS (file, name, path)
     * </ul>
     * <li>Construct an H5ScalarDS object that does not exist in file
     * </ul>
     */
    @Test
    public void testH5ScalarDSFileFormatStringString() {
        log.debug("testH5ScalarDSFileFormatStringString");
        int[] data = null;
        final String[] names = { null, DNAME_SUB, DNAME.substring(1) };
        final String[] paths = { DNAME_SUB, null, H5TestFile.NAME_GROUP };

        final H5File file = (H5File) testDataset.getFileFormat();
        assertNotNull(file);

        // test existing dataset in file
        for (int idx = 0; idx < names.length; idx++) {
            H5ScalarDS dset = new H5ScalarDS(file, names[idx], paths[idx]);
            assertNotNull(dset);

            // make sure that the data content is correct
            try {
                data = (int[]) dset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);

            for (int i = 0; i < H5TestFile.DIM_SIZE; i++)
                assertEquals(H5TestFile.DATA_INT[i], data[i]);

            // check the name and path
            assertTrue(DNAME_SUB.equals(dset.getFullName()));
            assertTrue(DNAME_SUB.equals(dset.getPath() + dset.getName()));

            dset.clear();
            dset = null;
        }

        // test a non-existing dataset
        H5.H5error_off();
        final H5ScalarDS dset = new H5ScalarDS(file, "NO_SUCH_DATASET", "NO_SUCH_PATH");
        dset.clearData();
        data = null;
        try {
            data = (int[]) dset.getData();
        }
        catch (final Exception ex) {
            data = null; // Expected - intentional
        }
        H5.H5error_on();
        assertNull(data);
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5ScalarDS#H5ScalarDS(hdf.object.FileFormat, java.lang.String, java.lang.String, long[])}
     *
     * What to test:
     * <ul>
     * <li>Construct an H5ScalarDS object that exits in file
     * <ul>
     * <li>new H5ScalarDS (file, null, fullpath, oid)
     * <li>new H5ScalarDS (file, fullname, null, oid)
     * <li>new H5ScalarDS (file, name, path, oid)
     * </ul>
     * <li>Construct an H5ScalarDS object that does not exist in file
     * </ul>
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testH5ScalarDSFileFormatStringStringLongArray() {
        log.debug("testH5ScalarDSFileFormatStringStringLongArray");
        int[] data = null;
        final String[] names = { null, DNAME_SUB, DNAME.substring(1) };
        final String[] paths = { DNAME_SUB, null, H5TestFile.NAME_GROUP };

        final H5File file = (H5File) testDataset.getFileFormat();
        assertNotNull(file);

        // test existing dataset in file
        for (int idx = 0; idx < names.length; idx++) {
            H5ScalarDS dset = new H5ScalarDS(file, names[idx], paths[idx]);
            assertNotNull(dset);

            // make sure that the data content is correct
            try {
                data = (int[]) dset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);

            for (int i = 0; i < H5TestFile.DIM_SIZE; i++)
                assertEquals(H5TestFile.DATA_INT[i], data[i]);

            // check the name and path
            assertTrue(DNAME_SUB.equals(dset.getFullName()));
            assertTrue(DNAME_SUB.equals(dset.getPath() + dset.getName()));

            dset.clear();
            dset = null;
        }

        // test a non-existing dataset
        H5.H5error_off();
        final H5ScalarDS dset = new H5ScalarDS(file, "NO_SUCH_DATASET", "NO_SUCH_PATH", null);
        dset.init();
        dset.clearData();
        data = null;
        try {
            data = (int[]) dset.getData();
        }
        catch (final Exception ex) {
            data = null; // Expected - intentional
        }
        H5.H5error_on();
        assertNull(data);
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS#getMetadata()}.
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
            attrs = (List) testDataset.getMetadata();
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
     * Test method for {@link hdf.object.h5.H5ScalarDS#writeMetadata(java.lang.Object)}.
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
            attrs = (List) testDataset.getMetadata();
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
                    for (int j = 0; j < ints.length; j++)
                        ints[j] = TEST_VALUE_INT;
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
        attr = (Attribute)new H5ScalarAttr(testDataset, "float attribute", typeFloat, new long[] { 1 },
                new float[] { TEST_VALUE_FLOAT });
        try {
            attr.writeAttribute();
        }
        catch (final Exception ex) {
            fail("writeMetadata() failed. " + ex);
        }

        // close the file and reopen it
        try {
            testDataset.clear();
            testFile.close();
            testFile.open();
            testDataset = (H5ScalarDS) testFile.get(DNAME);
        }
        catch (final Exception ex) {
            fail("testFile.get(DNAME) failed. " + ex);
        }

        // check the change in file
        try {
            attrs = (List) testDataset.getMetadata();
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
                    for (int j = 0; j < ints.length; j++)
                        assertEquals(TEST_VALUE_INT, ints[j]);
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
            testDataset.removeMetadata(newAttr);
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
     * Test method for {@link hdf.object.h5.H5ScalarDS#removeMetadata(java.lang.Object)}.
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
            attrs = (List) testDataset.getMetadata();
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
                testDataset.removeMetadata(arrayAttr[i]);
            }
            catch (final Exception ex) {
                fail("removeMetadata() failed. " + ex);
            }
        }

        // close the file and reopen it
        try {
            testDataset.clear();
            testFile.close();
            testFile.open();
            testDataset = (H5ScalarDS) testFile.get(DNAME);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
        }
        attrs = null;

        try {
            attrs = (List) testDataset.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertFalse(attrs.size() > 0);

        // restore to the original
        try {
            H5TestFile.ATTRIBUTE_STR.setParentObject(testDataset);
            H5TestFile.ATTRIBUTE_INT_ARRAY.setParentObject(testDataset);
            H5TestFile.ATTRIBUTE_STR.write();
            H5TestFile.ATTRIBUTE_INT_ARRAY.write();
        }
        catch (final Exception ex) {
            fail("writeMetadata() failed. " + ex);
        }
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5ScalarDS#create(java.lang.String, hdf.object.Group, hdf.object.Datatype, long[], long[], long[], int, java.lang.Object)}
     *
     * What to test:
     * <ul>
     * <li>Create a new dataset of 32-bit float with level-9 gzip compression
     * <li>Close and reopen the file
     * <li>Check the content of the new dataset
     * <li>Restore to the orginal file (remove the new dataset)
     * </ul>
     */
    @Test
    public void testCreate() {
        log.debug("testCreate");
        ScalarDS dset = null;
        final String nameNew = "/tmpH5ScalarDS";
        float[] data = null;

        H5Datatype typeFloat = null;
        try {
            typeFloat = new H5Datatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, Datatype.NATIVE);
        }
        catch (Exception ex) {
            fail("new H5Datatype failed. " + ex);
        }

        try {
            final Group rootGrp = (Group) testFile.get("/");
            dset = (ScalarDS) H5ScalarDS.create(nameNew, rootGrp, typeFloat, H5TestFile.DIMs, null, H5TestFile.CHUNKs,
                    9, H5TestFile.DATA_FLOAT);
        }
        catch (final Exception ex) {
            fail("H5ScalarDS.create() failed. " + ex);
        }

        // check the data content
        try {
            data = (float[]) dset.getData();
        }
        catch (final Exception ex) {
            fail("dset.getData() failed. " + ex);
        }
        assertNotNull(data);
        for (int i = 0; i < H5TestFile.DIM_SIZE; i++)
            assertEquals(H5TestFile.DATA_FLOAT[i], data[i], Float.MIN_VALUE);

        try {
            testFile.delete(dset); // delete the new datast
        }
        catch (final Exception ex) {
            fail("testFile.delete failed. " + ex);
        }
    }

}
