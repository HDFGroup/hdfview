package test.object;

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
import java.util.Vector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5CompoundDS;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

/**
 * TestCase for H5CompoundDS.
 * <p>
 * This class tests all the public methods in H5CompoundDS class.
 * <p>
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
 *
 * </pre>
 * <p>
 * We use the following template to test all the methods:
 * <p>
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
public class H5CompoundDSTest {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5CompoundDSTest.class);
    private static final H5File H5FILE = new H5File();
    private static final int NLOOPS = 10;
    private static final int TEST_VALUE_INT = Integer.MAX_VALUE;
    private static final float TEST_VALUE_FLOAT = Float.MAX_VALUE;
    private static final String TEST_VALUE_STR = "H5CompoundDSTest";
    private static final String DNAME = H5TestFile.NAME_DATASET_COMPOUND;
    private static final String DNAME_SUB = H5TestFile.NAME_DATASET_COMPOUND_SUB;

    private H5Datatype typeInt = null;
    private H5Datatype typeFloat = null;
    private H5Datatype typeStr = null;
    private H5File testFile = null;
    private H5CompoundDS testDataset = null;

    @BeforeClass
    public static void createFile() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("H5CompoundDSTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("H5CompoundDSTest AfterClass: Number of IDs still open: " + openID);
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
        typeInt = new H5Datatype(Datatype.CLASS_INTEGER, H5TestFile.DATATYPE_SIZE, -1, -1);
        typeFloat = new H5Datatype(Datatype.CLASS_FLOAT, H5TestFile.DATATYPE_SIZE, -1, -1);
        typeStr = new H5Datatype(Datatype.CLASS_STRING, H5TestFile.STR_LEN, -1, -1);

        testFile = (H5File) H5FILE.createInstance(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        assertNotNull(testFile);

        testFile.open();

        testDataset = (H5CompoundDS) testFile.get(DNAME);
        assertNotNull("setUp testFile.get", testDataset);
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#setName(java.lang.String)}.
     * <p>
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
        H5.H5error_off();
        try {
            testDataset.setName(null);
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }
        H5.H5error_on();

        // set an existing name
        H5.H5error_off();
        try {
            testDataset.setName(DNAME_SUB);
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }
        H5.H5error_on();

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
            testDataset = (H5CompoundDS) testFile.get(newName);
        }
        catch (final Exception ex) {
            fail("setName() failed. " + ex);
        }

        // test the old name
        H5CompoundDS tmpDset = null;
        try {
            H5.H5error_off();
            tmpDset = (H5CompoundDS) testFile.get(DNAME);
            H5.H5error_on();
        }
        catch (Exception ex) {
            fail("setName() get(oldname) failed. " + ex);
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
            testDataset = (H5CompoundDS) testFile.get(DNAME);
        }
        catch (final Exception ex) {
            fail("setName() failed. " + ex);
        }
        assertNotNull(testDataset);
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#open()}.
     * <p>
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
            catch (final Exception ex) {
            }
            try {
                H5.H5Sclose(sid);
            }
            catch (final Exception ex) {
            }
            try {
                H5.H5Dclose(did);
            }
            catch (final Exception ex) {
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#close(int)}.
     * <p>
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
            catch (final Exception ex) {
            }
            try {
                H5.H5Sclose(sid);
            }
            catch (final Exception ex) {
            }

            try {
                testDataset.close(did);
            }
            catch (final Exception ex) {
                fail("close() failed. " + ex);
            }

            // dataset is closed, expect to fail
            H5.H5error_off();
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#clear()}.
     * <p>
     * What to test:
     * <ul>
     * <li>Read data/attributes from file
     * <li>clear the dataet
     * <li>make sure that the data is empty
     * <li>make sure that the attribute list is empty
     * </ul>
     */
    @Test
    public void testClear() {
        log.debug("testClear");
        Vector<?> data = null;

        try {
            data = (Vector<?>) testDataset.getData();
        }
        catch (final Exception ex) {
            fail("getData() failed. " + ex);
        }
        assertNotNull(data);
        assertTrue(data.size() > 0);

        Vector<?> attrs = null;
        try {
            attrs = (Vector<?>) testDataset.getMetadata();
        }
        catch (final Exception ex) {
            fail("clear() failed. " + ex);
        }

        // clear up the dataset
        testDataset.clear();
        assertFalse(data.size() > 0);

        // attribute is empty
        try {
            attrs = (Vector<?>) testDataset.getMetadata();
        }
        catch (final Exception ex) {
            fail("clear() failed. " + ex);
        }
        assertTrue(attrs.size() <= 0);
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#init()}.
     * <p>
     * What to test:
     * <ul>
     * <li>call init()
     * <li>make that the dataspace is correct
     * <li>make sure that member selection is correct
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testInit() {
        log.debug("testInit");
        for (int loop = 0; loop < NLOOPS; loop++) {

            try {
                testFile.close();
            }
            catch (final Exception ex) {
            }

            try {
                testFile.open();

                testDataset = (H5CompoundDS) testFile.get(DNAME);
            }
            catch (final Exception ex) {
                fail("setName() failed. " + ex);
            }

            testDataset.init();

            // test the rank
            final int rank = testDataset.getRank();
            assertEquals(H5TestFile.RANK, rank);

            // test the dimesin sizes
            final long[] dims = testDataset.getDims();
            assertNotNull(dims);
            for (int i = 0; i < rank; i++) {
                assertEquals(H5TestFile.DIMs[i], dims[i]);
            }

            // start at 0
            final long[] start = testDataset.getStartDims();
            assertNotNull(start);
            for (int i = 0; i < rank; i++) {
                assertEquals(0, start[i]);
            }

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

            // by default, all members are selected
            final int nmembers = testDataset.getSelectedMemberCount();
            assertTrue(nmembers > 0);
            for (int i = 0; i < nmembers; i++) {
                assertTrue(testDataset.isMemberSelected(i));
            }

            // make some change and do another round of test
            // to make sure that the init() resets the default
            for (int i = 0; i < rank; i++) {
                start[i] = 1;
                selectedDims[0] = 1;
            }
            for (int i = 0; i < nmembers; i++) {
                testDataset.setMemberSelection(false);
            }

            long nObjs = 0;
            try {
                nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
            }
            catch (final Exception ex) {
                fail("H5.H5Fget_obj_count() failed. " + ex);
            }
            assertEquals(1, nObjs); // file id should be the only one left open

            try {
                testFile.close();
            }
            catch (final Exception ex) {
            }

        } // for (int loop=0; loop<NLOOPS; loop++)
    } // public final void testInit() {

    /**
     * Test method for {@link hdf.object.h5.H5CompoundDS#read()}.
     * <p>
     * What to test:
     * <ul>
     * <li>Read the whole dataset
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testRead() {
        log.debug("testRead");
        Vector<?> data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            // read the whole dataset by default
            testDataset.init();

            try {
                data = (Vector<?>) testDataset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);
            assertTrue(data.size() > 0);

            // check the data values
            final int[] ints = (int[]) data.get(0);
            final float[] floats = (float[]) data.get(1);
            final String[] strs = (String[]) data.get(2);
            final long[] longs = (long[]) data.get(3);
            assertNotNull(ints);
            assertNotNull(floats);
            assertNotNull(strs);
            assertNotNull(longs);
            for (int i = 0; i < H5TestFile.DIM_SIZE; i++) {
                assertEquals(H5TestFile.DATA_INT[i], ints[i]);
                assertEquals(H5TestFile.DATA_FLOAT[i], floats[i], Float.MIN_VALUE);
                assertTrue(H5TestFile.DATA_STR[i].equals(strs[i]));
                assertEquals(H5TestFile.DATA_LONG[i], longs[i]);
            }
        } // for (int loop=0; loop<NLOOPS; loop++) {
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#read()}.
     * <p>
     * What to test:
     * <ul>
     * <li>Read data row by row
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testReadByRow() {
        log.debug("testReadByRow");
        Vector<?> data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            testDataset.init();

            // read data row by row
            final long nrows = testDataset.getHeight();
            for (int i = 0; i < nrows; i++) {
                testDataset.clearData();
                testDataset.init();

                final int rank = testDataset.getRank();
                final long[] start = testDataset.getStartDims();
                final long[] count = testDataset.getSelectedDims();

                // select one row only
                for (int j = 0; j < rank; j++) {
                    count[j] = 1;
                }

                // select different rows
                start[0] = i;

                try {
                    data = (Vector<?>) testDataset.getData();
                }
                catch (final Exception ex) {
                    fail("getData() failed. " + ex);
                }

                final int ints[] = (int[]) data.get(0);
                final float floats[] = (float[]) data.get(1);
                final String strs[] = (String[]) data.get(2);
                assertNotNull(ints);
                assertNotNull(floats);
                assertNotNull(strs);
                final int idx = (int) H5TestFile.DIM2 * i;
                assertEquals(H5TestFile.DATA_INT[idx], ints[0]);
                assertEquals(H5TestFile.DATA_FLOAT[idx], floats[0], Float.MIN_VALUE);
                assertTrue(H5TestFile.DATA_STR[idx].equals(strs[0]));
            } // for (int i=0; i<nrows; i++) {
        } // for (int loop=0; loop<NLOOPS; loop++) {
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#read()}.
     * <p>
     * What to test:
     * <ul>
     * <li>Read data field bu field
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testReadByField() {
        log.debug("testReadByField");
        Vector<?> data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            testDataset.init();

            // read field by field
            final int nmembers = testDataset.getMemberCount();
            for (int i = 0; i < nmembers; i++) {
                testDataset.clearData();
                testDataset.init();

                testDataset.setMemberSelection(false);
                testDataset.selectMember(i);

                try {
                    data = (Vector<?>) testDataset.getData();
                }
                catch (final Exception ex) {
                    fail("getData() failed. " + ex);
                }
                assertNotNull(data);
                assertTrue(data.size() == 1);

                switch (i) {
                case 0:
                    final int[] ints = (int[]) data.get(0);
                    assertNotNull(ints);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        assertEquals(H5TestFile.DATA_INT[j], ints[j]);
                    }
                    break;
                case 1:
                    final float[] floats = (float[]) data.get(0);
                    assertNotNull(floats);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        assertEquals(H5TestFile.DATA_FLOAT[j], floats[j], Float.MIN_VALUE);
                    }
                    break;
                case 2:
                    final String[] strs = (String[]) data.get(0);
                    assertNotNull(strs);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        assertTrue(H5TestFile.DATA_STR[j].equals(strs[j]));
                    }
                    break;
                }
            } // for (int i=0; i<nmembers; i++) {
        } // for (int loop=0; loop<NLOOPS; loop++) {
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#readBytes()}.
     * <p>
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
        final int expected = H5TestFile.DIM_SIZE * (4 + 4 + H5TestFile.STR_LEN + 4);

        assertEquals(expected, n);
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#write(java.lang.Object)}.
     * <p>
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
        Vector<?> data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            // read the whole dataset by default
            testDataset.init();

            try {
                data = (Vector<?>) testDataset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);
            assertTrue(data.size() > 0);

            // check the data values
            int[] ints = (int[]) data.get(0);
            float[] floats = (float[]) data.get(1);
            String[] strs = (String[]) data.get(2);
            assertNotNull(ints);
            assertNotNull(floats);
            assertNotNull(strs);
            for (int i = 0; i < H5TestFile.DIM_SIZE; i++) {
                assertEquals(H5TestFile.DATA_INT[i], ints[i]);
                assertEquals(H5TestFile.DATA_FLOAT[i], floats[i], Float.MIN_VALUE);
                assertTrue(H5TestFile.DATA_STR[i].equals(strs[i]));
            }

            // change the data value
            for (int i = 0; i < H5TestFile.DIM_SIZE; i++) {
                ints[i] = TEST_VALUE_INT;
                floats[i] = TEST_VALUE_FLOAT;
                strs[i] = TEST_VALUE_STR;
            }

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
                testDataset = (H5CompoundDS) testFile.get(DNAME);
            }
            catch (final Exception ex) {
                fail("write() failed. " + ex);
            }

            // read the data into memory to make sure the data is correct
            testDataset.init();
            testDataset.clearData();

            try {
                data = (Vector<?>) testDataset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);
            assertTrue(data.size() > 0);

            // check the data values
            ints = (int[]) data.get(0);
            floats = (float[]) data.get(1);
            strs = (String[]) data.get(2);
            for (int i = 0; i < H5TestFile.DIM_SIZE; i++) {
                assertEquals(TEST_VALUE_INT, ints[i]);
                assertEquals(TEST_VALUE_FLOAT, floats[i], Float.MIN_VALUE);
                assertTrue(TEST_VALUE_STR.equals(strs[i]));
            }

            // write the original data into file
            try {
                testDataset.write(H5TestFile.DATA_COMP);
            }
            catch (final Exception ex) {
                fail("write() failed. " + ex);
            }
        } // for (int loop=0; loop<NLOOPS; loop++) {

        // write the original data into file
        testDataset.init();
        testDataset.clearData();
        try {
            testDataset.write(H5TestFile.DATA_COMP);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#write(java.lang.Object)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Read/write data row by row
     * <li>Repeat all above
     * <li>write the original data back to file
     * </ul>
     */
    @Test
    public void testWriteByRow() {
        log.debug("testWriteByRow");
        Vector<?> data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            testDataset.init();

            final int rank = testDataset.getRank();
            final long[] start = testDataset.getStartDims();
            final long[] count = testDataset.getSelectedDims();

            // read data row by row
            for (int i = 0; i < rank; i++) {
                start[i] = 0;
                count[i] = 1;
            }
            final long nrows = testDataset.getHeight();
            for (int i = 0; i < nrows; i++) {
                testDataset.clearData();
                testDataset.init();

                // select one row only
                for (int j = 0; j < rank; j++) {
                    count[j] = 1;
                }

                // select different rows
                start[0] = i;

                try {
                    data = (Vector<?>) testDataset.getData();
                }
                catch (final Exception ex) {
                    fail("getData() failed. " + ex);
                }

                final int[] ints = (int[]) data.get(0);
                final float[] floats = (float[]) data.get(1);
                final String[] strs = (String[]) data.get(2);
                assertNotNull(ints);
                assertNotNull(floats);
                assertNotNull(strs);
                assertEquals(H5TestFile.DATA_INT[i], ints[0]);
                assertEquals(H5TestFile.DATA_FLOAT[i], floats[0], Float.MIN_VALUE);
                assertTrue(H5TestFile.DATA_STR[i].equals(strs[0]));

                // change the data value
                ints[0] = TEST_VALUE_INT;
                floats[0] = TEST_VALUE_FLOAT;
                strs[0] = TEST_VALUE_STR;

                // write data row by row
                try {
                    testDataset.write(data);
                }
                catch (final Exception ex) {
                    fail("write() failed. " + ex);
                }

                // check if data is correct
                testDataset.clearData();
                try {
                    data = (Vector<?>) testDataset.getData();
                }
                catch (final Exception ex) {
                    fail("getData() failed. " + ex);
                }
                assertEquals(TEST_VALUE_INT, ints[0]);
                assertEquals(TEST_VALUE_FLOAT, floats[0], Float.MIN_VALUE);
                assertTrue(TEST_VALUE_STR.equals(strs[0]));
            } // for (int i=0; i<nrows; i++) {

            // write the original data into file
            testDataset.init();
            testDataset.clearData();
            try {
                testDataset.write(H5TestFile.DATA_COMP);
            }
            catch (final Exception ex) {
                fail("write() failed. " + ex);
            }
        } // for (int loop=0; loop<NLOOPS; loop++) {

        // write the original data into file
        testDataset.init();
        testDataset.clearData();
        try {
            testDataset.write(H5TestFile.DATA_COMP);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#write(java.lang.Object)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Read/write data field by field
     * <li>Repeat all above
     * <li>write the original data back to file
     * </ul>
     */
    @Test
    public void testWriteByField() {
        log.debug("testWriteByField");
        Vector<?> data = null;

        for (int loop = 0; loop < NLOOPS; loop++) {
            testDataset.init();

            // read field by field
            final int nmembers = testDataset.getMemberCount();
            for (int i = 0; i < nmembers; i++) {
                testDataset.clearData();
                testDataset.init();

                testDataset.setMemberSelection(false);
                testDataset.selectMember(i);

                try {
                    data = (Vector<?>) testDataset.getData();
                }
                catch (final Exception ex) {
                    fail("getData() failed. " + ex);
                }
                assertNotNull(data);
                assertTrue(data.size() == 1);

                // change the data value
                switch (i) {
                case 0:
                    final int[] ints = (int[]) data.get(0);
                    assertNotNull(ints);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        ints[j] = TEST_VALUE_INT;
                    }
                    break;
                case 1:
                    final float[] floats = (float[]) data.get(0);
                    assertNotNull(floats);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        floats[j] = TEST_VALUE_FLOAT;
                    }
                    break;
                case 2:
                    final String[] strs = (String[]) data.get(0);
                    assertNotNull(strs);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        strs[j] = TEST_VALUE_STR;
                    }
                    break;
                }

                // write data field y field
                try {
                    testDataset.write(data);
                }
                catch (final Exception ex) {
                    fail("write() failed. " + ex);
                }

                // check if data is correct
                testDataset.clearData();
                try {
                    data = (Vector<?>) testDataset.getData();
                }
                catch (final Exception ex) {
                    fail("getData() failed. " + ex);
                }
                switch (i) {
                case 0:
                    final int[] ints = (int[]) data.get(0);
                    assertNotNull(ints);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        assertEquals(TEST_VALUE_INT, ints[j]);
                    }
                    break;
                case 1:
                    final float[] floats = (float[]) data.get(0);
                    assertNotNull(floats);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        assertEquals(TEST_VALUE_FLOAT, floats[j], Float.MIN_VALUE);
                    }
                    break;
                case 2:
                    final String[] strs = (String[]) data.get(0);
                    assertNotNull(strs);
                    for (int j = 0; j < H5TestFile.DIM_SIZE; j++) {
                        assertTrue(TEST_VALUE_STR.equals(strs[j]));
                    }
                    break;
                }

                // write the original data into file
                testDataset.init();
                testDataset.clearData();
                try {
                    testDataset.write(H5TestFile.DATA_COMP);
                }
                catch (final Exception ex) {
                    fail("write() failed. " + ex);
                }
            } // for (int i=0; i<nmembers; i++) {
        } // for (int loop=0; loop<NLOOPS; loop++) {

        // write the original data into file
        testDataset.init();
        testDataset.clearData();
        try {
            testDataset.write(H5TestFile.DATA_COMP);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#getDatatype()}.
     * <p>
     * What to test:
     * <ul>
     * <li>Get the datatype object of the dataset
     * </ul>
     */
    @Test
    public void testGetDatatype() {
        log.debug("testGetDatatype");
        testDataset.init();

        final H5Datatype dtype = (H5Datatype) testDataset.getDatatype();
        assertNotNull(dtype);
        assertEquals(H5Datatype.CLASS_COMPOUND, dtype.getDatatypeClass());
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#isString(int)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Test a string datatype with isString(int tid)
     * <li>Test a non-string datatype with isString(int tid)
     * </ul>
     */
    @Test
    public void testIsString() {
        log.debug("testIsString");
        assertFalse(testDataset.isString(HDF5Constants.H5T_NATIVE_INT));
        assertFalse(testDataset.isString(HDF5Constants.H5T_NATIVE_FLOAT));
        assertTrue(testDataset.isString(HDF5Constants.H5T_C_S1));
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#getSize(int)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Test a sizes of different defined data types
     * </ul>
     */
    @Test
    public void testGetSize() {
        log.debug("testGetSize");
        assertEquals(1, testDataset.getSize(HDF5Constants.H5T_NATIVE_INT8));
        assertEquals(2, testDataset.getSize(HDF5Constants.H5T_NATIVE_INT16));
        assertEquals(4, testDataset.getSize(HDF5Constants.H5T_NATIVE_INT32));
        assertEquals(8, testDataset.getSize(HDF5Constants.H5T_NATIVE_INT64));
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
     * {@link hdf.object.h5.H5CompoundDS#H5CompoundDS(hdf.object.FileFormat, java.lang.String, java.lang.String)}
     * .
     * <p>
     * What to test:
     * <ul>
     * <li>Construct an H5CompoundDS object that exists in file
     * <ul>
     * <li>new H5CompoundDS (file, null, fullpath)
     * <li>new H5CompoundDS (file, fullname, null)
     * <li>new H5CompoundDS (file, name, path)
     * </ul>
     * <li>Construct an H5CompoundDS object that does not exist in file
     * </ul>
     */
    @Test
    public void testH5CompoundDSFileFormatStringString() {
        log.debug("testH5CompoundDSFileFormatStringString");
        Vector<?> data = null;
        final String[] names = { null, DNAME_SUB, DNAME.substring(1) };
        final String[] paths = { DNAME_SUB, null, H5TestFile.NAME_GROUP };

        final H5File file = (H5File) testDataset.getFileFormat();
        assertNotNull(file);

        // test existing dataset in file
        for (int idx = 0; idx < names.length; idx++) {
            H5CompoundDS dset = new H5CompoundDS(file, names[idx], paths[idx]);
            assertNotNull(dset);

            // make sure that the data content is correct
            try {
                data = (Vector<?>) dset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);
            assertTrue(data.size() > 0);
            final int[] ints = (int[]) data.get(0);
            final float[] floats = (float[]) data.get(1);
            final String[] strs = (String[]) data.get(2);
            assertNotNull(ints);
            assertNotNull(floats);
            assertNotNull(strs);
            for (int i = 0; i < H5TestFile.DIM_SIZE; i++) {
                assertEquals(H5TestFile.DATA_INT[i], ints[i]);
                assertEquals(H5TestFile.DATA_FLOAT[i], floats[i], Float.MIN_VALUE);
                assertTrue(H5TestFile.DATA_STR[i].equals(strs[i]));
            }

            // check the name and path
            assertTrue(DNAME_SUB.equals(dset.getFullName()));
            assertTrue(DNAME_SUB.equals(dset.getPath() + dset.getName()));

            dset.clear();
            dset = null;
        }

        // test a non-existing dataset
        H5CompoundDS nodset = null;
        H5.H5error_off();
        try {
            nodset = new H5CompoundDS(file, "NO_SUCH_DATASET", "NO_SUCH_PATH");
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }

        try {
            nodset.init();
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }

        try {
            nodset.clearData();
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }
        data = null;
        try {
            data = (Vector<?>) nodset.getData();
        }
        catch (final Exception ex) {
            data = null; // Expected - intentional
        }
        assertNull(data);
        H5.H5error_on();
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
     * {@link hdf.object.h5.H5CompoundDS#H5CompoundDS(hdf.object.FileFormat, java.lang.String, java.lang.String, long[])}
     * .
     * <p>
     * What to test:
     * <ul>
     * <li>Construct an H5CompoundDS object that exits in file
     * <ul>
     * <li>new H5CompoundDS (file, null, fullpath, oid)
     * <li>new H5CompoundDS (file, fullname, null, oid)
     * <li>new H5CompoundDS (file, name, path, oid)
     * </ul>
     * <li>Construct an H5CompoundDS object that does not exist in file
     * </ul>
     */
    @Test
    public void testH5CompoundDSFileFormatStringStringLongArray() {
        log.debug("testH5CompoundDSFileFormatStringStringLongArray");
        Vector<?> data = null;
        final String[] names = { null, DNAME_SUB, DNAME.substring(1) };
        final String[] paths = { DNAME_SUB, null, H5TestFile.NAME_GROUP };

        final H5File file = (H5File) testDataset.getFileFormat();
        assertNotNull(file);

        // test existing dataset in file
        long[] oid = null;
        for (int idx = 0; idx < names.length; idx++) {

            try {
                final byte[] ref_buf = H5.H5Rcreate(file.getFID(), DNAME_SUB, HDF5Constants.H5R_OBJECT, -1);
                final long l = HDFNativeData.byteToLong(ref_buf, 0);
                oid = new long[1];
                oid[0] = l; // save the object ID
            }
            catch (final HDF5Exception ex) {
                fail("H5.H5Rcreate() failed. " + ex);
            }
            assertNotNull(oid);

            H5CompoundDS dset = new H5CompoundDS(file, names[idx], paths[idx], oid);
            assertNotNull(dset);

            // make sure that the data content is correct
            try {
                data = (Vector<?>) dset.getData();
            }
            catch (final Exception ex) {
                fail("getData() failed. " + ex);
            }
            assertNotNull(data);
            assertTrue(data.size() > 0);
            final int[] ints = (int[]) data.get(0);
            final float[] floats = (float[]) data.get(1);
            final String[] strs = (String[]) data.get(2);
            assertNotNull(ints);
            assertNotNull(floats);
            assertNotNull(strs);
            for (int i = 0; i < H5TestFile.DIM_SIZE; i++) {
                assertEquals(H5TestFile.DATA_INT[i], ints[i]);
                assertEquals(H5TestFile.DATA_FLOAT[i], floats[i], Float.MIN_VALUE);
                assertTrue(H5TestFile.DATA_STR[i].equals(strs[i]));
            }

            // check the name and path
            assertTrue(DNAME_SUB.equals(dset.getFullName()));
            assertTrue(DNAME_SUB.equals(dset.getPath() + dset.getName()));

            dset.clear();
            dset = null;
        }

        // test a non-existing dataset
        H5.H5error_off();
        H5CompoundDS dset = null;
        try {
            dset = new H5CompoundDS(file, "NO_SUCH_DATASET", "NO_SUCH_PATH");
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }
        try {
            dset.init();
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }
        try {
            dset.clearData();
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }
        data = null;
        try {
            data = (Vector<?>) dset.getData();
        }
        catch (final Exception ex) {
            data = null; // Expected - intentional
        }
        assertNull(data);
        H5.H5error_on();
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#getMetadata()}.
     * <p>
     * What to test:
     * <ul>
     * <li>Get all the attributes
     * <li>Check the content of the attributes
     * </ul>
     */
    @Test
    public void testGetMetadata() {
        log.debug("testGetMetadata");
        Vector<?> attrs = null;

        try {
            attrs = (Vector<?>) testDataset.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        final int n = attrs.size();
        for (int i = 0; i < n; i++) {
            final Attribute attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getDatatype();
            if (dtype.getDatatypeClass() == H5Datatype.CLASS_STRING) {
                assertTrue(H5TestFile.ATTRIBUTE_STR.getName().equals(attr.getName()));
                assertTrue(((String[]) H5TestFile.ATTRIBUTE_STR.getData())[0].equals(((String[]) attr.getData())[0]));
            }
            else if (dtype.getDatatypeClass() == H5Datatype.CLASS_INTEGER) {
                assertTrue(H5TestFile.ATTRIBUTE_INT_ARRAY.getName().equals(attr.getName()));
                final int[] expected = (int[]) H5TestFile.ATTRIBUTE_INT_ARRAY.getData();
                assertNotNull(expected);
                final int[] ints = (int[]) attr.getData();
                assertNotNull(ints);
                for (int j = 0; j < expected.length; j++) {
                    assertEquals(expected[j], ints[j]);
                }
            }
        } // for (int i=0; i<n; i++) {
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#writeMetadata(java.lang.Object)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Update the value of an existing attribute
     * <li>Attach a new attribute
     * <li>Close and re-open file to check if the change is made in file
     * <li>Restore to the orginal state
     * </ul>
     */
    @Test
    public void testWriteMetadata() {
        log.debug("testWriteMetadata");
        Vector<?> attrs = null;
        Attribute attr = null;

        try {
            attrs = (Vector<?>) testDataset.getMetadata();
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
            final H5Datatype dtype = (H5Datatype) attr.getDatatype();
            if (dtype.getDatatypeClass() == H5Datatype.CLASS_STRING) {
                final String[] strs = (String[]) attr.getData();
                strs[0] = TEST_VALUE_STR;
            }
            else if (dtype.getDatatypeClass() == H5Datatype.CLASS_INTEGER) {
                final int[] ints = (int[]) attr.getData();
                assertNotNull(ints);
                for (int j = 0; j < ints.length; j++) {
                    ints[j] = TEST_VALUE_INT;
                }
            }
            try {
                testDataset.writeMetadata(attr);
            }
            catch (final Exception ex) {
                fail("writeMetadata() failed. " + ex);
            }
        } // for (int i=0; i<n; i++) {

        // attache a new attribute
        attr = new Attribute("float attribute", typeFloat, new long[] { 1 }, new float[] { TEST_VALUE_FLOAT });
        try {
            testDataset.writeMetadata(attr);
        }
        catch (final Exception ex) {
            fail("writeMetadata() failed. " + ex);
        }

        // close the file and reopen it
        try {
            testDataset.clear();
            testFile.close();
            testFile.open();
            testDataset = (H5CompoundDS) testFile.get(DNAME);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
        }

        // check the change in file
        try {
            attrs = (Vector<?>) testDataset.getMetadata();
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
            final H5Datatype dtype = (H5Datatype) attr.getDatatype();
            if (dtype.getDatatypeClass() == H5Datatype.CLASS_STRING) {
                assertTrue(H5TestFile.ATTRIBUTE_STR.getName().equals(attr.getName()));
                assertTrue(TEST_VALUE_STR.equals(((String[]) attr.getData())[0]));
            }
            else if (dtype.getDatatypeClass() == H5Datatype.CLASS_INTEGER) {
                assertTrue(H5TestFile.ATTRIBUTE_INT_ARRAY.getName().equals(attr.getName()));
                final int[] ints = (int[]) attr.getData();
                assertNotNull(ints);
                for (int j = 0; j < ints.length; j++) {
                    assertEquals(TEST_VALUE_INT, ints[j]);
                }
            }
            else if (dtype.getDatatypeClass() == H5Datatype.CLASS_FLOAT) {
                newAttr = attr;
                final float[] floats = (float[]) attr.getData();
                assertEquals(TEST_VALUE_FLOAT, floats[0], Float.MIN_VALUE);
            }
        } // for (int i=0; i<n; i++) {

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
            final H5Datatype dtype = (H5Datatype) attr.getDatatype();
            if (dtype.getDatatypeClass() == H5Datatype.CLASS_STRING) {
                final String[] strs = (String[]) attr.getData();
                strs[0] = ((String[]) H5TestFile.ATTRIBUTE_STR.getData())[0];
            }
            else if (dtype.getDatatypeClass() == H5Datatype.CLASS_INTEGER) {
                final int[] ints = (int[]) attr.getData();
                assertNotNull(ints);
                for (int j = 0; j < ints.length; j++) {
                    final int[] expected = (int[]) H5TestFile.ATTRIBUTE_INT_ARRAY.getData();
                    ints[j] = expected[j];
                }
            }
            try {
                testDataset.writeMetadata(attr);
            }
            catch (final Exception ex) {
                fail("writeMetadata() failed. " + ex);
            }
        } // for (int i=0; i<n; i++) {
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
     * Test method for {@link hdf.object.h5.H5CompoundDS#removeMetadata(java.lang.Object)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Remove all existing attributes
     * <li>Close and reopen file to check if all attribute are removed from file
     * <li>Restore to the original state
     * </ul>
     */
    @Test
    public void testRemoveMetadata() {
        log.debug("testRemoveMetadata");
        Vector<?> attrs = null;

        try {
            attrs = (Vector<?>) testDataset.getMetadata();
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
            testDataset = (H5CompoundDS) testFile.get(DNAME);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
        }
        attrs = null;

        try {
            attrs = (Vector<?>) testDataset.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertFalse(attrs.size() > 0);

        // restore to the original
        try {
            testDataset.writeMetadata(H5TestFile.ATTRIBUTE_STR);
            testDataset.writeMetadata(H5TestFile.ATTRIBUTE_INT_ARRAY);
        }
        catch (final Exception ex) {
            fail("writeMetadata() failed. " + ex);
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
     * Test method for
     * {@link hdf.object.h5.H5CompoundDS#create(java.lang.String, hdf.object.Group, long[], java.lang.String[], hdf.object.Datatype[], int[], java.lang.Object)}
     * .
     * <p>
     * Create a simple compound dataset, i.e. compound members can be either a scalar data or 1D array.
     *
     * <pre>
     * public static Dataset create(
     *             String name,
     *             Group pgroup,
     *             long[] dims,
     *             String[] memberNames,
     *             Datatype[] memberDatatypes,
     *             int[] memberSizes,
     *             Object data) throws Exception
     * </pre>
     * <p>
     * What to test:
     * <ul>
     * <li>Create new compound datasets
     * <ul>
     * <li>Compound dataset with one field -- an 1D integer array: {int[]}
     * <li>Compound dataset with one field -- an 1D float array: {float[]}
     * <li>Compound dataset with one field -- a string: {string}
     * <li>Compound dataset with three fields {int[], float[], string}
     * </ul>
     * <li>Close and reopen the file
     * <li>Check the content of the new datasets
     * <li>Restore to the orginal file (remove the new datasets)
     * </ul>
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateStringGroupLongArrayStringArrayDatatypeArrayIntArrayObject() {
        log.debug("testCreateStringGroupLongArrayStringArrayDatatypeArrayIntArrayObject");
        H5CompoundDS dset = null;
        H5Group rootGrp = null;
        Vector<Object> compData = new Vector<Object>();
        final String compIntName = "/compoundInt";
        final String compFloatName = "/compoundFloat";
        final String compStrName = "/compoundStr";
        final String compIntFloatStrName = "compoundIntFloatStr";
        final int[] expectedInts = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        final float[] expectedFloats = { .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, .10f };
        final String[] expectedStr = { "Str 1", "Str 2", "Str 3", "Str 4", "Str 5" };
        final long[] dims = { 5 };
        final int[] memberOrders = { 2 };

        try {
            rootGrp = (H5Group) testFile.get("/");
        }
        catch (final Exception ex) {
            fail("testFile.get(\"/\") failed. " + ex);
        }

        // Compound dataset with one field -- an integer array: {int[]}
        compData.setSize(0);
        compData.add(expectedInts);
        try {
            String[] memberNames = new String[] { "int" };
            Datatype[] memberDatatypes = new H5Datatype[] { typeInt };
            int nMembers = memberNames.length;
            int memberRanks[] = new int[nMembers];
            long memberDims[][] = new long[nMembers][1];
            for (int i = 0; i < nMembers; i++) {
                memberRanks[i] = 1;
                memberDims[i][0] = memberOrders[i];
            }
            dset = (H5CompoundDS) H5CompoundDS.create(compIntName, rootGrp, dims, null, null, -1, memberNames,
                    memberDatatypes, memberRanks, memberDims, compData);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);

        // Compound dataset with one field -- a float array: {float[}
        compData.setSize(0);
        compData.add(expectedFloats);
        try {
            String[] memberNames = new String[] { "float" };
            Datatype[] memberDatatypes = new H5Datatype[] { typeFloat };
            int nMembers = memberNames.length;
            int memberRanks[] = new int[nMembers];
            long memberDims[][] = new long[nMembers][1];
            for (int i = 0; i < nMembers; i++) {
                memberRanks[i] = 1;
                memberDims[i][0] = memberOrders[i];
            }
            dset = (H5CompoundDS) H5CompoundDS.create(compFloatName, rootGrp, dims, null, null, -1, memberNames,
                    memberDatatypes, memberRanks, memberDims, compData);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);

        // Compound dataset with one field -- a string: {string}
        compData.setSize(0);
        compData.add(expectedStr);
        try {
            String[] memberNames = new String[] { "Str" };
            Datatype[] memberDatatypes = new H5Datatype[] { typeStr };
            int[] memberStrOrders = { 1 };
            int nMembers = memberNames.length;
            int memberRanks[] = new int[nMembers];
            long memberDims[][] = new long[nMembers][1];
            for (int i = 0; i < nMembers; i++) {
                memberRanks[i] = 1;
                memberDims[i][0] = memberStrOrders[i];
            }
            dset = (H5CompoundDS) H5CompoundDS.create(compStrName, rootGrp, dims, null, null, -1, memberNames,
                    memberDatatypes, memberRanks, memberDims, compData);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);

        // Compound dataset with three fields {int, float, string}
        compData.setSize(0);
        compData.add(expectedInts);
        compData.add(expectedFloats);
        compData.add(expectedStr);
        try {
            String[] memberNames = new String[] { "int", "float", "Str" };
            Datatype[] memberDatatypes = new H5Datatype[] { typeInt, typeFloat, typeStr };
            int[] memberNewOrders = { 2, 2, 1 };
            int nMembers = memberNames.length;
            int memberRanks[] = new int[nMembers];
            long memberDims[][] = new long[nMembers][1];
            for (int i = 0; i < nMembers; i++) {
                memberRanks[i] = 1;
                memberDims[i][0] = memberNewOrders[i];
            }
            dset = (H5CompoundDS) H5CompoundDS.create(compIntFloatStrName, rootGrp, dims, null, null, -1, memberNames,
                    memberDatatypes, memberRanks, memberDims, compData);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);

        // close the file and reopen it
        try {
            testFile.close();
            testFile.open();
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }

        // check the content of the new compIntName
        try {
            dset.clear();
            dset = (H5CompoundDS) testFile.get(compIntName);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);
        compData = null;
        dset.init();
        try {
            compData = (Vector<Object>) dset.getData();
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(compData);
        int[] ints = (int[]) compData.get(0);
        for (int i = 0; i < expectedInts.length; i++) {
            assertEquals(expectedInts[i], ints[i]);
        }
        try {
            testFile.delete(dset); // delete the new datast
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }

        // check the content of the new compFloatName
        try {
            dset.clear();
            dset = (H5CompoundDS) testFile.get(compFloatName);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);
        compData = null;
        dset.init();
        try {
            compData = (Vector<Object>) dset.getData();
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(compData);
        float[] floats = (float[]) compData.get(0);
        for (int i = 0; i < expectedFloats.length; i++) {
            assertEquals(expectedFloats[i], floats[i], Float.MIN_VALUE);
        }
        try {
            testFile.delete(dset); // delete the new datast
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }

        // check the content of the new compStrName
        try {
            dset.clear();
            dset = (H5CompoundDS) testFile.get(compStrName);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);
        compData = null;
        dset.init();
        try {
            compData = (Vector<Object>) dset.getData();
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(compData);
        String[] strs = (String[]) compData.get(0);
        for (int i = 0; i < expectedStr.length; i++) {
            assertTrue(expectedStr[i].equals(strs[i]));
        }
        try {
            testFile.delete(dset); // delete the new datast
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }

        // check the content of the new compIntFloatStrName
        try {
            dset.clear();
            dset = (H5CompoundDS) testFile.get(compIntFloatStrName);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);
        compData = null;
        dset.init();
        try {
            compData = (Vector<Object>) dset.getData();
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(compData);
        assertTrue(compData.size() >= 3);
        ints = (int[]) compData.get(0);
        floats = (float[]) compData.get(1);
        strs = (String[]) compData.get(2);
        for (int i = 0; i < expectedInts.length; i++) {
            assertEquals(expectedInts[i], ints[i]);
        }
        for (int i = 0; i < expectedFloats.length; i++) {
            assertEquals(expectedFloats[i], floats[i], Float.MIN_VALUE);
        }
        for (int i = 0; i < expectedStr.length; i++) {
            assertTrue(expectedStr[i].equals(strs[i]));
        }
        try {
            testFile.delete(dset); // delete the new datast
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
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
     * Test method for
     * {@link hdf.object.h5.H5CompoundDS#create(java.lang.String, hdf.object.Group, long[], java.lang.String[], hdf.object.Datatype[], int[], int[][], java.lang.Object)}
     * .
     * <p>
     * Create a simple compound dataset, i.e. compound members can be multiple-dimensional array.
     *
     * <pre>
     * public static Dataset create(
     *             String name,
     *             Group pgroup,
     *             long[] dims,
     *             String[] memberNames,
     *             Datatype[] memberDatatypes,
     *             int[] memberRanks,
     *             int[][] memberDims,
     *             Object data) throws Exception
     * </pre>
     * <p>
     * What to test:
     * <ul>
     * <li>Create new compound datasets
     * <li>Compound dataset with two fields -- {int[][], float[][]}
     * <li>Close and reopen the file
     * <li>Check the content of the new datasets
     * <li>Restore to the orginal file (remove the new dataset)
     * </ul>
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateStringGroupLongArrayStringArrayDatatypeArrayIntArrayIntArrayArrayObject() {
        log.debug("testCreateStringGroupLongArrayStringArrayDatatypeArrayIntArrayIntArrayArrayObject");
        H5CompoundDS dset = null;
        H5Group rootGrp = null;
        Vector<Object> compData = new Vector<Object>();
        final String compName = "/compound--{int[][], float[][]}";
        final int[] expectedInts = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        final float[] expectedFloats = { .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, .10f, .11f, .12f };
        final long[] dims = { 2 };
        final int[] memberRanks = { 2, 2 };
        final long[][] memberDims = { { 3, 2 }, { 3, 2 } };

        try {
            rootGrp = (H5Group) testFile.get("/");
        }
        catch (final Exception ex) {
            fail("testFile.get(\"/\") failed. " + ex);
        }

        // create new compound dataset
        compData.setSize(0);
        compData.add(expectedInts);
        compData.add(expectedFloats);
        try {
            dset = (H5CompoundDS) H5CompoundDS.create(compName, rootGrp, dims, null, null, 0, new String[] { "int",
            "float" }, new H5Datatype[] { typeInt, typeFloat }, memberRanks, memberDims, compData);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("H5CompoundDS.create()[float] failed. " + ex);
        }
        assertNotNull(dset);

        // close the file and reopen it
        try {
            testFile.close();
            testFile.open();
        }
        catch (final Exception ex) {
            fail("testFile.close-open failed. " + ex);
        }

        // check the content of the new dataset
        try {
            dset.clear();
            dset = (H5CompoundDS) testFile.get(compName);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);
        compData = null;
        dset.init();
        try {
            compData = (Vector<Object>) dset.getData();
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(compData);
        assertTrue(compData.size() >= 2);
        final int[] ints = (int[]) compData.get(0);
        final float[] floats = (float[]) compData.get(1);

        for (int i = 0; i < expectedInts.length; i++) {
            assertEquals(expectedInts[i], ints[i]);
        }
        for (int i = 0; i < expectedFloats.length; i++) {
            assertEquals(expectedFloats[i], floats[i], Float.MIN_VALUE);
        }
        try {
            testFile.delete(dset); // delete the new datast
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
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
     * Test method for
     * {@link hdf.object.h5.H5CompoundDS#create(java.lang.String, hdf.object.Group, long[], long[], long[], int, java.lang.String[], hdf.object.Datatype[], int[], int[][], java.lang.Object)}
     * .
     * <p>
     * Create a simple compound dataset with compression options, i.e. compound members can be multiple-dimensional
     * array.
     *
     * <pre>
     * public static Dataset create(
     *             String name,
     *             Group pgroup,
     *             long[] dims,
     *             long[] maxdims,
     *             long[] chunks,
     *             int gzip,
     *             String[] memberNames,
     *             Datatype[] memberDatatypes,
     *             int[] memberRanks,
     *             int[][] memberDims,
     *             Object data) throws Exception
     * </pre>
     * <p>
     * What to test:
     * <ul>
     * <li>Create new compound datasets with level-9 gzip compression
     * <li>Compound dataset with three fields -- {int, float, string}
     * <li>Close and reopen the file
     * <li>Check the content of the new datasets
     * <li>Restore to the orginal file (remove the new dataset)
     * </ul>
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateStringGroupLongArrayLongArrayLongArrayIntStringArrayDatatypeArrayIntArrayIntArrayArrayObject() {
        log.debug("testCreateStringGroupLongArrayLongArrayLongArrayIntStringArrayDatatypeArrayIntArrayIntArrayArrayObject");
        H5CompoundDS dset = null;
        H5Group rootGrp = null;
        Vector<Object> compData = new Vector<Object>();
        final String compName = "/compound compressed with gzip level 9";
        final long[] maxdims = { H5TestFile.DIMs[0] * 5, H5TestFile.DIMs[1] * 5 };

        try {
            rootGrp = (H5Group) testFile.get("/");
        }
        catch (final Exception ex) {
            fail("testFile.get(\"/\") failed. " + ex);
        }

        // create new compound dataset
        compData.setSize(0);
        compData.add(H5TestFile.DATA_INT);
        compData.add(H5TestFile.DATA_FLOAT);
        compData.add(H5TestFile.DATA_STR);
        try {
            dset = (H5CompoundDS) H5CompoundDS.create(compName, rootGrp, H5TestFile.DIMs, maxdims, H5TestFile.CHUNKs,
                    9, new String[] { "int", "float", "str" }, new H5Datatype[] { typeInt, typeFloat, typeStr },
                    new int[] { 1, 1, 1 }, new long[][] { { 1 }, { 1 }, { 1 } }, compData);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);

        // close the file and reopen it
        try {
            testFile.close();
            testFile.open();
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }

        // check the content of the new dataset
        try {
            dset.clear();
            dset = (H5CompoundDS) testFile.get(compName);
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(dset);
        compData = null;
        dset.init();
        try {
            compData = (Vector<Object>) dset.getData();
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
        }
        assertNotNull(compData);
        assertTrue(compData.size() >= 3);
        final int[] ints = (int[]) compData.get(0);
        final float[] floats = (float[]) compData.get(1);
        final String[] strs = (String[]) compData.get(2);

        for (int i = 0; i < H5TestFile.DATA_INT.length; i++) {
            assertEquals(H5TestFile.DATA_INT[i], ints[i]);
        }
        for (int i = 0; i < H5TestFile.DATA_FLOAT.length; i++) {
            assertEquals(H5TestFile.DATA_FLOAT[i], floats[i], Float.MIN_VALUE);
        }
        for (int i = 0; i < H5TestFile.DATA_STR.length; i++) {
            assertTrue(H5TestFile.DATA_STR[i].equals(strs[i]));
        }

        try {
            testFile.delete(dset); // delete the new datast
        }
        catch (final Exception ex) {
            fail("H5CompoundDS.create() failed. " + ex);
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
     * Test method for {@link hdf.object.h5.H5CompoundDS} IsSerializable.
     */
    @Test
    public void testIsSerializable() {
        log.debug("testIsSerializable");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(testDataset);
            oos.close();
        }
        catch (IOException err) {
            err.printStackTrace();
            fail("ObjectOutputStream failed: " + err);
        }
        assertTrue(out.toByteArray().length > 0);
    }

    /**
     * Test method for {@link hdf.object.h5.H5CompoundDS} SerializeToDisk.
     * <p>
     * What to test:
     * <ul>
     * <li>serialize a dataset identifier
     * <li>deserialize a dataset identifier
     * <li>open a dataset identifier
     * <li>get datatype and dataspace identifier for the dataset
     * </ul>
     */
    @Test
    public void testSerializeToDisk() {
        log.debug("testSerializeToDisk");
        try {
            FileOutputStream fos = new FileOutputStream("temph5cdset.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(testDataset);
            oos.close();
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        H5CompoundDS test = null;
        try {
            FileInputStream fis = new FileInputStream("temph5cdset.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            test = (hdf.object.h5.H5CompoundDS) ois.readObject();
            ois.close();

            // Clean up the file
            new File("temph5cdset.ser").delete();
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        long did = -1, tid = -1, sid = -1;

        for (int loop = 0; loop < NLOOPS; loop++) {
            did = tid = sid = -1;
            try {
                did = test.open();
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
            catch (final Exception ex) {
            }
            try {
                H5.H5Sclose(sid);
            }
            catch (final Exception ex) {
            }
            try {
                H5.H5Dclose(did);
            }
            catch (final Exception ex) {
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

}
