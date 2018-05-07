package test.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.hdf5lib.structs.H5L_info_t;
import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;
import hdf.object.h5.H5ScalarDS;

/**
 * TestCase for H5File.
 * <p>
 * This class tests all the public methods in H5ScalarDS class.
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
public class H5FileTest {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5FileTest.class);
    private static final H5File H5FILE = new H5File();
    private static final int NLOOPS = 10;
    private static final int TEST_VALUE_INT = Integer.MAX_VALUE;
    private static final float TEST_VALUE_FLOAT = Float.MAX_VALUE;
    private static final String TEST_VALUE_STR = "H5ScalarDSTest";
    private static final String DNAME = H5TestFile.NAME_DATASET_INT;
    private static final String DNAME_SUB = H5TestFile.NAME_DATASET_INT_SUB;

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
            catch (final Exception ex) {
            }
            testFile = null;
        }
    }

    @BeforeClass
    public static void createFile() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("H5FileTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("H5FileTest AfterClass: Number of IDs still open: " + openID);
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

        testFile = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        assertNotNull(testFile);

        testFile.open();

        testDataset = (H5ScalarDS) testFile.get(DNAME);
        assertNotNull(testDataset);
    }

    @After
    public void removeFiles() throws Exception {
        closeFile();
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
     * Test method for {@link hdf.object.h5.H5File#open()}.
     * <p>
     * What to test:
     * <ul>
     * <li>open a file identifier
     * <li>check the file content
     * <li>close the file
     * </ul>
     */
    @Test
    public void testOpen() {
        log.debug("testOpen");
        try {
            testFile.close();
        }
        catch (final Exception ex) {
        }

        for (int i = 0; i < NLOOPS; i++) {
            long nObjs = 0;
            long fid = -1;
            final H5File file = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);

            try {
                fid = file.open(); // open the full tree
            }
            catch (final Exception ex) {
                fail("file.open() failed. " + ex);
            }
            assertTrue(fid > 0);

            // try to get all object in the file
            try {
                for (int j = 0; j < H5TestFile.OBJ_NAMES.length; j++) {
                    assertNotNull(file.get(H5TestFile.OBJ_NAMES[j]));
                }
            }
            catch (final Exception ex) {
                fail("file.get() failed. " + ex);
            }

            try {
                nObjs = H5.H5Fget_obj_count(file.getFID(), HDF5Constants.H5F_OBJ_ALL);
            }
            catch (final Exception ex) {
                fail("H5.H5Fget_obj_count() failed. " + ex);
            }
            assertTrue(nObjs <= 1); // file id should be the only this left
            // open. IS THIS BECAUSE THE ONLY THING WE
            // HAVE DONE IS OPEN THE FILE?

            try {
                file.close();
            }
            catch (final Exception ex) {
                fail("file.close() failed. " + ex);
            }
        } // for (int i=0; i<NLOOPS; i++)

        try {
            testFile.open();
        }
        catch (final Exception ex) {
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
     * Test method for {@link hdf.object.h5.H5File#create(java.lang.String)}.
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>access the file
     * <li>close/delete the file
     * </ul>
     */
    @Test
    public void testCreateString() {
        log.debug("testCreateString");
        final String nameNew = "testH5File.h5";
        H5File file = null;

        try {
            file = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE);
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }

        long fid = -1;
        try {
            fid = file.open();
        }
        catch (final Exception ex) {
            fail("file.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        try {
            file.close();
        }
        catch (final Exception ex) {
        }
        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#getRootNode()}.
     * <p>
     * What to test:
     * <ul>
     * <li>get the root node
     * <li>check the content of the root node
     * </ul>
     */
    @Test
    public void testGetRootObject() {
        log.debug("testGetRootObject");
        final HObject root = testFile.getRootObject();
        assertNotNull(root);
        assertTrue(((Group) root).breadthFirstMemberList().size() > 0);
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
     * Test method for {@link hdf.object.h5.H5File#isReadOnly()}.
     */
    @Test
    public void testIsReadOnly() {
        log.debug("testIsReadOnly");
        assertFalse(testFile.isReadOnly());
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
     * Test method for {@link hdf.object.h5.H5File#createGroup(java.lang.String, hdf.object.Group)} .
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>create a group
     * <li>access the group
     * <li>close/delete the file
     * </ul>
     */
    @Test
    public void testCreateGroup() {
        log.debug("testCreateGroup");
        final String nameNew = "testH5File.h5";
        H5File file = null;

        try {
            file = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE);
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }

        long fid = -1;
        try {
            fid = file.open();
        }
        catch (final Exception ex) {
            fail("file.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        Group grp = null;
        try {
            grp = file.createGroup("new group", null);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grp);

        long gid = -1;
        try {
            gid = grp.open();
        }
        catch (final Exception ex) {
            fail("fgrp.open() failed. " + ex);
        }
        assertTrue(gid > 0);
        grp.close(gid);

        try {
            file.close();
        }
        catch (final Exception ex) {
        }
        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#createGroup(java.lang.String, hdf.object.Group, int)} .
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>Sets link creation property list identifier
     * <li>Sets group creation property list identifier
     * <li>Check that group is not created when the order of group property list is incorrect.
     * <li>create a group
     * <li>create subgroups
     * <li>Check the new group and subgroup
     * <li>Check name of ith link in group by creation order.
     * <li>close/delete the file
     * </ul>
     */
    @Test
    public void testCreateGroupWithGroupplist() {
        log.debug("testCreateGroupWithGroupplist");
        final String nameNew = "testH5File2.h5";
        H5File file = null;
        long fid = -1;
        long gcpl = -1;
        long gid = -1;
        long lcpl = -1;
        Group grp = null;
        Group grp2 = null, grp3 = null;
        H5G_info_t ginfo;

        try {
            file = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE);
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }
        try {
            fid = file.open();
        }
        catch (final Exception ex) {
            fail("file.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        try {
            lcpl = H5.H5Pcreate(HDF5Constants.H5P_LINK_CREATE); // create lcpl
            if (lcpl >= 0)
                H5.H5Pset_create_intermediate_group(lcpl, true);
        }
        catch (final Exception ex) {
            fail("H5.H5Pcreate() failed. " + ex);
        }

        try {
            gcpl = H5.H5Pcreate(HDF5Constants.H5P_GROUP_CREATE); // create gcpl
            if (gcpl >= 0) {
                H5.H5Pset_link_creation_order(gcpl, HDF5Constants.H5P_CRT_ORDER_TRACKED
                        + HDF5Constants.H5P_CRT_ORDER_INDEXED);// Set link creation order
            }
        }
        catch (final Exception ex) {
            fail("H5.H5Pcreate() failed. " + ex);
        }
        H5.H5error_off();
        try {
            grp = file.createGroup("Group1/Group2/Group3", null, gcpl, lcpl);
        }
        catch (final Exception ex) {
            ; // Expected -intentional as the order of gplist is invalid.
        }
        H5.H5error_on();
        assertNull(grp);

        try {
            grp = file.createGroup("Group1/Group2/Group3", null, lcpl, gcpl);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grp);

        try {
            gid = grp.open();
        }
        catch (final Exception ex) {
            fail("grp.open() failed. " + ex);
        }
        assertTrue(gid > 0);

        try {
            grp2 = file.createGroup("G4", grp); // create subgroups in /Group3
            grp3 = file.createGroup("G3", grp);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grp2);
        assertNotNull(grp3);

        try {
            String name = H5.H5Lget_name_by_idx(gid, ".", HDF5Constants.H5_INDEX_CRT_ORDER, HDF5Constants.H5_ITER_INC,
                    1, HDF5Constants.H5P_DEFAULT); // Get name of 2nd link
            assertEquals("G3", name);
        }
        catch (final Exception ex) {
            fail("H5.H5Lget_name_by_idx() failed. " + ex);
        }

        grp.close(gid);
        H5.H5error_off();
        try {
            H5.H5Pclose(lcpl);
            H5.H5Pclose(gcpl);
        }
        catch (final Exception ex) {
        }
        H5.H5error_on();
        try {
            file.close();
        }
        catch (final Exception ex) {
        }
        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#createGcpl(int, int, int)} .
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>set and create gcpl
     * <li>Check if gcpl has been created
     * <li>create group, using the gcpl created.
     * <li>create subgroups
     * <li>Check the new group and subgroups
     * <li>Check name of ith link in group by creation order.
     * <li>close/delete the file
     * </ul>
     */
    @Test
    public void testcreateGcpl() {
        log.debug("testcreateGcpl");
        final String nameNew = "test8.h5";
        H5File file = null;
        long fid = -1;
        long gcpl = -1;
        long gid = -1;

        Group grp = null;
        Group grp2 = null, grp3 = null;
        H5G_info_t ginfo;

        try {
            file = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE);
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }
        try {
            fid = file.open();
        }
        catch (final Exception ex) {
            fail("file.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        try {
            gcpl = file.createGcpl(Group.CRT_ORDER_INDEXED, 5, 3);
        }
        catch (final Exception ex) {
            fail("file.createGcpl() failed. " + ex);
        }

        try {
            grp = file.createGroup("/Group1", null, HDF5Constants.H5P_DEFAULT, gcpl);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grp);

        try {
            gid = grp.open();
        }
        catch (final Exception ex) {
            fail("grp.open() failed. " + ex);
        }
        assertTrue(gid > 0);

        try {
            grp2 = file.createGroup("G4", grp); // create subgroups in /Group1
            grp3 = file.createGroup("G3", grp);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grp2);
        assertNotNull(grp3);

        try {
            String name = H5.H5Lget_name_by_idx(gid, ".", HDF5Constants.H5_INDEX_CRT_ORDER, HDF5Constants.H5_ITER_INC,
                    1, HDF5Constants.H5P_DEFAULT); // Get name of 2nd link
            assertEquals("G3", name);
        }
        catch (final Exception ex) {
            fail("H5.H5Lget_name_by_idx() failed. " + ex);
        }

        grp.close(gid);

        try {
            file.close();
        }
        catch (final Exception ex) {
        }
        file.delete();
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5File#createScalarDS(java.lang.String, hdf.object.Group, hdf.object.Datatype, long[], long[], long[], int, java.lang.Object)}
     * , <br>
     * {@link hdf.object.h5.H5File#createCompoundDS(java.lang.String, hdf.object.Group, long[], java.lang.String[], hdf.object.Datatype[], int[], java.lang.Object)}
     * , <br>
     * {@link hdf.object.h5.H5File#createCompoundDS(java.lang.String, hdf.object.Group, long[], long[], long[], int, java.lang.String[], hdf.object.Datatype[], int[], java.lang.Object)}
     * , <br>
     * {@link hdf.object.h5.H5File#createImage(java.lang.String, hdf.object.Group, hdf.object.Datatype, long[], long[], long[], int, int, int, java.lang.Object)}
     * , <br>
     * {@link hdf.object.h5.H5File#createDatatype(int, int, int, int)}, <br>
     * {@link hdf.object.h5.H5File#createDatatype(int, int, int, int, java.lang.String)} , <br>
     * {@link hdf.object.h5.H5File#createLink(hdf.object.Group, java.lang.String, hdf.object.HObject)} , <br>
     * {@link hdf.object.h5.H5File#get(java.lang.String)}, <br>
     * {@link hdf.object.h5.H5File#getAttribute(int)}, <br>
     * {@link hdf.object.h5.H5File#writeAttribute(hdf.object.HObject, hdf.object.Attribute, boolean)} .
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>create a different types of objects
     * <li>access the objects
     * <li>close/delete the new file
     * </ul>
     */
    @Test
    public void testCreateObjects() {
        log.debug("testCreateObjects");
        final String nameNew = "testH5File.h5";
        H5File file = null;

        try {
            file = H5TestFile.createTestFile(nameNew);
            file.open();
        }
        catch (final Exception ex) {
            fail("H5TestFile.createTestFile() failed. " + ex);
        }
        assertNotNull(file);

        // try to get all object in the file
        try {
            for (int j = 0; j < H5TestFile.OBJ_NAMES.length; j++) {
                assertNotNull(file.get(H5TestFile.OBJ_NAMES[j]));
            }
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }

        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(file.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertTrue(nObjs <= 1); // file id should be the only this left open

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#isThisType(java.lang.String)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Check an HDF5 file
     * <li>Check a non HDF5 file
     * </ul>
     */
    @Test
    public void testIsThisTypeString() {
        log.debug("testIsThisTypeString");
        assertTrue(H5FILE.isThisType(H5TestFile.NAME_FILE_H5));
        H5.H5error_off();
        assertFalse(H5FILE.isThisType("No such file"));
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
     * Test method for {@link hdf.object.h5.H5File#isThisType(hdf.object.FileFormat)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Check an HDF5 file
     * <li>Check a non HDF5 file
     * </ul>
     */
    @Test
    public void testIsThisTypeFileFormat() {
        log.debug("testIsThisTypeFileFormat");
        assertTrue(H5FILE.isThisType(testFile));
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
     * Test method for {@link hdf.object.h5.H5File#copy(hdf.object.HObject, hdf.object.Group)} .
     * <p>
     * What to test:
     * <ul>
     * <li>create a new file
     * <li>copy all the objects (datasts, groups and datatypes) from test file to the new file
     * <li>check the content of the new file
     * <li>close/delete the new file
     * </ul>
     */
    @SuppressWarnings({ "rawtypes", "deprecation" })
    @Test
    public void testCopyHObjectGroup() {
        log.debug("testCopyHObjectGroup");
        Group root = null;
        HObject srcObj = null, dstObj = null;
        final String nameNewFile = "testH5File.h5";
        String dstName = null;
        H5File file = null;

        try {
            root = (Group) testFile.get("/");
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }
        assertNotNull(root);

        final List members = root.getMemberList();
        final int n = members.size();
        assertTrue(n > 0);

        try {
            file = (H5File) H5FILE.createFile(nameNewFile, FileFormat.FILE_CREATE_DELETE);
            file.open();
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }
        assertNotNull(file);

        try {
            root = (Group) file.get("/");
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }
        assertNotNull(root);

        // copy all the objects to the new file
        for (int i = 0; i < n; i++) {
            dstName = null;
            dstObj = null;
            srcObj = (HObject) members.get(i);

            try {
                dstObj = testFile.copy(srcObj, root);
            }
            catch (final Exception ex) {
                // image palette probably is copied already
                if (H5TestFile.NAME_DATASET_IMAGE_PALETTE.equals(srcObj.getFullName())) {
                    continue;
                }

                fail("file.copy() failed on " + srcObj.getFullName() + ". " + ex);
            }
            assertNotNull(dstObj);
            dstName = dstObj.getFullName();

            // re-open the file to make sure the object is writen to file
            try {
                file.close();
                file.open();
            }
            catch (final Exception ex) {
                fail("file.close() failed. " + ex);
            }

            try {
                dstObj = file.get(dstName);
            }
            catch (final Exception ex) {
                fail("file.get() failed on " + dstObj.getFullName() + ". " + ex);
            }
            assertNotNull(dstObj);
        }

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#delete(hdf.object.HObject)}.
     * <p>
     * What to test:
     * <ul>
     * <li>create a new file with all types of objects (datasts, groups and datatypes)
     * <li>check the content of the new file
     * <li>delete all objects
     * <li>close/re-open the file to check the content of the file
     * <li>close/delete the new file
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testDeleteHObject() {
        log.debug("testDeleteHObject");
        Group root = null;
        HObject obj = null;
        final String nameNewFile = "testH5File.h5";
        H5File file = null;

        try {
            file = H5TestFile.createTestFile(nameNewFile);
            file.open();
        }
        catch (final Exception ex) {
            fail("H5TestFile.createTestFile() failed. " + ex);
        }
        assertNotNull(file);

        try {
            root = (Group) file.get("/");
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }
        assertNotNull(root);

        final List members = root.getMemberList();
        final int n = members.size();
        assertTrue(n > 0);

        final Object[] objs = members.toArray();
        for (int i = 0; i < n; i++) {
            obj = (HObject) objs[i];

            try {
                file.delete(obj);
            }
            catch (final Exception ex) {
                fail("file.copy() failed on " + obj.getFullName() + ". " + ex);
            }

            // re-open the file to make sure the object is writen to file
            try {
                file.close();
                file.open();
            }
            catch (final Exception ex) {
                fail("file.close() failed. " + ex);
            }

            try {
                H5.H5error_off();
                obj = file.get(obj.getFullName());
                H5.H5error_on();
            }
            catch (final Exception ex) {
                fail("file.get(getFullName) failed. " + ex);
            }
            assertNull(obj);
        }

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#get(java.lang.String)}.
     * <p>
     * What to test:
     * <ul>
     * <li>ceate a test file
     * <li>do not call file.open() (without the full tree in memory)
     * <li>get all types of objects (datasts, groups and datatypes)
     * <li>get object that does not exitst in file
     * <li>close and delete the test file
     * </ul>
     */
    @Test
    public void testGet() {
        log.debug("testGet");
        long nObjs = 0; // number of object left open
        HObject obj = null;

        final String nameNewFile = "testH5File.h5";
        H5File file = null;

        try {
            H5TestFile.createTestFile(nameNewFile);
        }
        catch (final Exception ex) {
            fail("H5TestFile.createTestFile() failed. " + ex);
        }

        file = new H5File(nameNewFile);

        // get object that does not exist in file
        try {
            H5.H5error_off();
            obj = file.get("/_INVALID_OBJECT_PATH_SHOULD_RETURN_NULL_");
            H5.H5error_on();
        }
        catch (final Exception ex) {
            fail("file.get() failed on invalid path. " + ex);
        }
        assertNull(obj);

        // get all object in file
        for (int i = 0; i < H5TestFile.OBJ_NAMES.length; i++) {
            try {
                obj = file.get(H5TestFile.OBJ_NAMES[i]);
            }
            catch (final Exception ex) {
                fail("file.get(\"" + H5TestFile.OBJ_NAMES[i] + "\" failed. " + ex);
            }
            assertNotNull(obj);
        }

        try {
            nObjs = H5.H5Fget_obj_count(file.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#get(java.lang.String)}.
     * <p>
     * What to test:
     * <ul>
     * <li>ceate a test file
     * <li>call file.open() (with the full tree in memory)
     * <li>get all types of objects (datasts, groups and datatypes)
     * <li>get object that does not exitst in file
     * <li>close and delete the test file
     * </ul>
     */
    @Test
    public void testGetFromOpen() {
        log.debug("testGetFromOpen");
        long nObjs = 0; // number of object left open
        HObject obj = null;

        final String nameNewFile = "testH5File.h5";
        H5File file = null;

        try {
            H5TestFile.createTestFile(nameNewFile);
        }
        catch (final Exception ex) {
            fail("H5TestFile.createTestFile() failed. " + ex);
        }

        file = new H5File(nameNewFile);

        try {
            file.open();
        }
        catch (final Exception ex) {
            fail("file.open failed. " + ex);
        }

        // get object that does not exist in file
        try {
            H5.H5error_off();
            obj = file.get("/_INVALID_OBJECT_PATH_SHOULD_RETURN_NULL_");
            H5.H5error_on();
        }
        catch (final Exception ex) {
            fail("file.get() failed on invalid path. " + ex);
        }
        assertNull(obj);

        // get all object in file
        for (int i = 0; i < H5TestFile.OBJ_NAMES.length; i++) {
            try {
                obj = file.get(H5TestFile.OBJ_NAMES[i]);
            }
            catch (final Exception ex) {
                fail("file.get(\"" + H5TestFile.OBJ_NAMES[i] + "\" failed. " + ex);
            }
            assertNotNull(obj);
        }

        try {
            nObjs = H5.H5Fget_obj_count(file.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#H5File(java.lang.String, int)}.
     * <p>
     * What to test:
     * <ul>
     * <li>create files with READ, WRITE and CREATE opttions
     * <li>check access permision of the files
     * <li>close/delete the new file
     * </ul>
     */
    @Test
    public void testH5FileStringInt() {
        log.debug("testH5FileStringInt");
        Dataset dset = null;
        final String nameNewFile = "testH5File.h5";
        H5File file = null;

        try {
            file = H5TestFile.createTestFile(nameNewFile);
            file.open();
        }
        catch (final Exception ex) {
            fail("H5TestFile.createTestFile() failed. " + ex);
        }
        assertNotNull(file);
        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        // make sure the file is read only
        try {
            file = new H5File(nameNewFile, FileFormat.READ);
            file.open();
        }
        catch (final Exception ex) {
            fail("new H5File(nameNewFile, H5File.READ) failed. " + ex);
        }
        assertTrue(file.isReadOnly());

        try {
            dset = (Dataset) file.get(H5TestFile.NAME_DATASET_FLOAT);
            dset.getData();
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }
        assertNotNull(dset);

        boolean isWrittenFailed = false;
        H5.H5error_off();
        try {
            dset.write();
        }
        catch (final Exception ex) {
            isWrittenFailed = true; // Expected.
        }
        H5.H5error_on();
        assertTrue(isWrittenFailed);

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        // make sure the file is read/write
        try {
            file = new H5File(nameNewFile, FileFormat.WRITE);
            file.open();
        }
        catch (final Exception ex) {
            fail("new H5File(nameNewFile, H5File.READ) failed. " + ex);
        }

        try {
            dset = (Dataset) file.get(H5TestFile.NAME_DATASET_FLOAT);
            dset.getData();
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }
        assertNotNull(dset);

        try {
            dset.write();
        }
        catch (final Exception ex) {
            fail("file.write() failed. " + ex);
        }

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        // create a new file
        try {
            file = new H5File(nameNewFile, FileFormat.CREATE);
            file.open();
        }
        catch (final Exception ex) {
            fail("new H5File(nameNewFile, H5File.READ) failed. " + ex);
        }

        H5.H5error_off();
        try {
            dset = (Dataset) file.get(H5TestFile.NAME_DATASET_FLOAT);
            dset.getData();
        }
        catch (final Exception ex) {
            ; // Expected. The file is empty.
        }
        H5.H5error_on();

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#open(int)}.
     * <p>
     * What to test:
     * <ul>
     * <li>open a file with H5F_CLOSE_STRONG file access
     * <li>check the file content
     * <li>close the file
     * </ul>
     */
    @Test
    public void testOpenInt() {
        log.debug("testOpenInt");
        try {
            testFile.close();
        }
        catch (final Exception ex) {
        }

        long nObjs = 0;
        long plist = -1;

        final H5File file = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);

        try {
            plist = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
            H5.H5Pset_fclose_degree(plist, HDF5Constants.H5F_CLOSE_STRONG);
        }
        catch (final Exception ex) {
            fail("H5.H5Pcreate() failed. " + ex);
        }

        try {
            file.open(plist); // open the full tree
        }
        catch (final Exception ex) {
            fail("file.open() failed. " + ex);
        }
        try {
            H5.H5Pclose(plist);
        }
        catch (final Exception ex) {
        }

        // try to get all object in the file
        try {
            for (int j = 0; j < H5TestFile.OBJ_NAMES.length; j++) {
                assertNotNull(file.get(H5TestFile.OBJ_NAMES[j]));
            }
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }

        try {
            nObjs = H5.H5Fget_obj_count(file.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        assertTrue(nObjs <= 1); // file id should be the only this left open

        try {
            testFile.open();
        }
        catch (final Exception ex) {
        }
        nObjs = 0;
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
     * {@link hdf.object.h5.H5File#updateReferenceDataset(hdf.object.h5.H5File, hdf.object.h5.H5File)} .
     * <p>
     * What to test:
     * <ul>
     * <li>create a new file
     * <li>copy a reference dataset from the test file to the new file
     * <li>check the content of the dataset in the new file
     * <li>close/delete the new file
     * </ul>
     */
    @SuppressWarnings({ "rawtypes", "deprecation" })
    @Test
    public void testUpdateReferenceDataset() {
        log.debug("testUpdateReferenceDataset");
        Group root = null;
        HObject srcObj = null, dstObj = null;
        final String nameNewFile = "testH5File.h5";
        String dstName = null;
        H5File file = null;

        try {
            root = (Group) testFile.get("/");
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }
        assertNotNull(root);

        final List members = root.getMemberList();
        final int n = members.size();
        assertTrue(n > 0);

        try {
            file = (H5File) H5FILE.createFile(nameNewFile, FileFormat.FILE_CREATE_DELETE);
            file.open();
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }
        assertNotNull(file);

        try {
            root = (Group) file.get("/");
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }
        assertNotNull(root);

        // copy all the objects to the new file
        for (int i = 0; i < n; i++) {
            dstName = null;
            dstObj = null;
            srcObj = (HObject) members.get(i);

            try {
                dstObj = testFile.copy(srcObj, root);
            }
            catch (final Exception ex) {
                // image palette probably is copied already
                if (H5TestFile.NAME_DATASET_IMAGE_PALETTE.equals(srcObj.getFullName())) {
                    continue;
                }

                fail("file.copy() failed on " + srcObj.getFullName() + ". " + ex);
            }
            assertNotNull(dstObj);
            dstName = dstObj.getFullName();

            // re-open the file to make sure the object is writen to file
            try {
                file.close();
                file.open();
            }
            catch (final Exception ex) {
                fail("file.close() failed. " + ex);
            }

            try {
                dstObj = file.get(dstName);
            }
            catch (final Exception ex) {
                fail("file.get() failed on " + dstObj.getFullName() + ". " + ex);
            }
            assertNotNull(dstObj);
        }

        try {
            H5File.updateReferenceDataset(testFile, file);
        }
        catch (final Exception ex) {
            fail("H5File.updateReferenceDataset() failed. " + ex);
        }

        long obj_type = -1;
        long did = -1;
        byte[] read_data = new byte[3920];
        HObject obj = null;

        // Check if the copied dataset containing references, point to correct object type.
        try {
            obj = file.get(H5TestFile.OBJ_NAMES[17]);
            did = H5.H5Dopen(file.getFID(), obj.getName(), HDF5Constants.H5P_DEFAULT);
            H5.H5Dread(did, HDF5Constants.H5T_STD_REF_OBJ, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                    HDF5Constants.H5P_DEFAULT, read_data);

            byte rbuf0[] = new byte[8];
            int srcPos = 0;

            for (int i = 0; i < 17; i++) {
                System.arraycopy(read_data, srcPos, rbuf0, 0, 8);
                srcPos = srcPos + 8;
                obj_type = H5.H5Rget_obj_type(file.getFID(), HDF5Constants.H5R_OBJECT, rbuf0);
                assertTrue(obj_type == H5TestFile.OBJ_TYPES[i]);
            }
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.get() failed. " + ex);
        }

        try {
            H5.H5Dclose(did);
        }
        catch (final Exception ex) {
        }

        try {
            file.close();
        }
        catch (final Exception ex) {
            fail("file.close() failed. " + ex);
        }

        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#createImageAttributes(hdf.object.Dataset, int)} .
     */
    @Test
    public void testCreateImageAttributes() {
        log.debug("testCreateImageAttributes");
        H5ScalarDS img = null;

        try {
            img = (H5ScalarDS) testFile.get(H5TestFile.NAME_DATASET_IMAGE);
        }
        catch (final Exception ex) {
            fail("file.get() failed. " + ex);
        }
        assertNotNull(img);
        assertTrue(img.hasAttribute());
        assertTrue(img.isImage());
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
     * Test method for {@link hdf.object.h5.H5File#setLibBounds(int , int )}
     * {@link hdf.object.h5.H5File#getLibBounds()}
     */
    @Test
    public void testSetLibBounds() {
        log.debug("testSetLibBounds");
        String low = "Latest";
        String high = "Latest";

        final H5File file = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);

        try {
            file.open();
            file.setLibBounds(low, high);
        }
        catch (final Exception ex) {
            fail("testFile.setLibBounds() failed. " + ex);
        }

        int[] libver = new int[2];
        try {
            libver = file.getLibBounds();
        }
        catch (final Exception ex) {
            fail("testFile.getLibBounds() failed. " + ex);
        }

        assertEquals(file.LIBVER_LATEST, libver[0]);
        assertEquals(file.LIBVER_LATEST, libver[1]);

        try {
            file.close();
        }
        catch (Exception ex) {
        }
        try {
            testFile.setLibBounds(null, null);
        }
        catch (final Exception ex) {
            fail("testFile.setLibBounds() failed. " + ex);
        }
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5File#createLink(hdf.object.Group, java.lang.String, hdf.object.HObject, int)}
     * .
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>create groups
     * <li>create subgroups and dataset
     * <li>Create soft link and hard link
     * <li>Checks the soft link and hard link
     * <li>Create a soft dangling link
     * <li>Check the soft dangling link
     * <li>Retrieve Link information.
     * <li>Check the link type.
     * <li>close/delete the file
     * </ul>
     */
    @Test
    public void testCreateLink() {
        log.debug("testCreateLink");
        final String nameNew = "testH5FileLinks1.h5";
        H5File file = null;
        long fid = -1;
        Group grp1 = null, grp2 = null;
        Group subgrp1 = null;
        Dataset d1 = null;

        try {
            file = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE);
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }

        try {
            fid = file.open();
        }
        catch (final Exception ex) {
            fail("file.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        try {
            grp1 = file.createGroup("Group1", null);
            grp2 = file.createGroup("Group2", null);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grp1);
        assertNotNull(grp2);

        try {
            subgrp1 = file.createGroup("G2", grp1); // create subgroup in Group1
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(subgrp1);

        long[] H5dims = { 4, 6 };
        try {
            d1 = file.createScalarDS("DS1", grp1, typeInt, H5dims, null, null, 0, null); // create dataset in Group1
        }
        catch (final Exception ex) {
            fail("file.createScalarDS() failed. " + ex);
        }
        assertNotNull(d1);

        // Create Soft and hard Links
        HObject obj = null;
        try {
            obj = file.createLink(grp1, "NAME_SOFT_LINK", d1, Group.LINK_TYPE_SOFT);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.createLink() failed. " + ex);
        }
        assertNotNull(obj);

        // Check the name of the target object the link points to is correct.
        String linkTargetObjName = null;
        try {
            linkTargetObjName = H5File.getLinkTargetName(obj);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.getLinkInfo() failed. " + ex);
        }
        assertEquals(linkTargetObjName, d1.getFullName());

        try {
            obj = file.createLink(grp2, "NAME_HARD_LINK", grp1, Group.LINK_TYPE_HARD);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.createLink() failed. " + ex);
        }
        assertNotNull(obj);

        // Create a Dangling Link to object.
        Group grplink = new H5Group(null, "DGroup", "/Group1", null);
        assertNotNull(grplink);
        H5.H5error_off();
        try {
            obj = file.createLink(grp1, "NAME_SOFT_LINK_DANGLE", grplink, Group.LINK_TYPE_SOFT);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.createLink() failed. " + ex);
        }
        H5.H5error_on();
        assertNotNull(obj);

        // Create the object to which a dangling link is created
        try {
            grplink = file.createGroup("DGroup", grp1);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grplink);

        // Create a soft dangling Link to object.
        String a = "D5";
        try {
            obj = file.createLink(grp1, "SD2", a, Group.LINK_TYPE_SOFT);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.createLink() failed. " + ex);
        }
        assertNotNull(obj);

        long gid = -1;
        try {
            gid = grp1.open();
        }
        catch (Exception ex) {
            fail("grp1.open()failed. " + ex);
        }

        H5L_info_t link_info = null;
        try {
            link_info = H5.H5Lget_info(gid, "NAME_SOFT_LINK_DANGLE", HDF5Constants.H5P_DEFAULT);
        }
        catch (Exception ex) {
            fail("H5.H5Lget_info: " + ex);
        }
        assertFalse("H5Lget_info ", link_info == null);
        assertTrue("H5Lget_info link type", link_info.type == HDF5Constants.H5L_TYPE_SOFT);
        assertTrue("Link Address ", link_info.address_val_size > 0);

        try {
            grp1.close(gid);
        }
        catch (final Exception ex) {
        }

        try {
            file.close();
        }
        catch (final Exception ex) {
        }
        file.delete();
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5File#createLink(hdf.object.Group, java.lang.String, hdf.object.HObject, int)}
     * .
     * <p>
     * What to test:
     * <ul>
     * <li>create a file, file1
     * <li>create group
     * <li>create subgroup and dataset
     * <li>create a second file, file2
     * <li>create group in file2
     * <li>Create External Links from file2 to dataset in File1
     * <li>Checks the external link
     * <li>Create a dangling external link from file2 to object in file1
     * <li>Checks the dangling external link
     * <li>Retrieve Link information.
     * <li>Check the link type.
     * <li>close/delete the files
     * </ul>
     */
    @Test
    public void testCreateLinkExternal() {
        log.debug("testCreateLinkExternal");
        final String nameNew = "TESTFILE1.h5";
        H5File file1 = null;
        H5File file2 = null;
        long fid = -1;
        Group grp1 = null;
        Group fgrp1 = null;
        Group subgrp1 = null;
        Dataset d1 = null;

        // Create File1.
        try {
            file1 = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE);
        }
        catch (final Exception ex) {
            fail("file1.create() failed. " + ex);
        }
        try {
            fid = file1.open();
        }
        catch (final Exception ex) {
            fail("file1.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        try {
            grp1 = file1.createGroup("Group1", null);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grp1);
        try {
            subgrp1 = file1.createGroup("G2", grp1); // create subgroups in Group1
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(subgrp1);
        long[] H5dims = { 4, 6 };
        try {
            d1 = file1.createScalarDS("DS1", grp1, typeInt, H5dims, null, null, 0, null); // create dataset in Group1
        }
        catch (final Exception ex) {
            fail("file.createScalarDS() failed. " + ex);
        }
        assertNotNull(d1);

        // Create File2
        try {
            file2 = (H5File) H5FILE.createFile("TESTExternal.h5", FileFormat.FILE_CREATE_DELETE);
        }
        catch (final Exception ex) {
            fail("file2.create() failed. " + ex);
        }
        try {
            fid = file2.open();
        }
        catch (final Exception ex) {
            fail("file2.open() failed. " + ex);
        }
        assertTrue(fid > 0);
        try {
            fgrp1 = file2.createGroup("Group1", null);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(fgrp1);

        // Create External Links from file2 to dataset in File1.
        HObject obj = null;
        try {
            obj = file2.createLink(fgrp1, "NAME_EXTERNAL_LINK", d1, Group.LINK_TYPE_EXTERNAL);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.createLink() failed. " + ex);
        }
        assertNotNull(obj);

        // Check the name of the target object the link points to is correct.
        String linkTargetObjName = null;
        try {
            linkTargetObjName = H5File.getLinkTargetName(obj);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.getLinkInfo() failed. " + ex);
        }
        String d1fullName = d1.getFile() + FileFormat.FILE_OBJ_SEP + d1.getFullName();
        assertEquals(d1fullName, linkTargetObjName);

        H5.H5error_off();
        // Create a Dangling Link to object.
        Group grplink = new H5Group(file1, "DGroup", null, null);
        assertNotNull(grplink);
        try {
            obj = file2.createLink(fgrp1, "GROUP_HARD_LINK_DANGLE", grplink, Group.LINK_TYPE_EXTERNAL);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.createLink() failed. " + ex);
        }
        assertNotNull(obj);

        // Create the object to which a dangling link is created
        try {
            grplink = file1.createGroup("DGroup", null);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(grplink);
        H5.H5error_on();

        // Retrieve Link information
        long gid = -1;
        try {
            gid = fgrp1.open();
        }
        catch (Exception ex) {
            fail("fgrp1.open()failed. " + ex);
        }

        H5L_info_t link_info = null;
        try {
            link_info = H5.H5Lget_info(gid, "GROUP_HARD_LINK_DANGLE", HDF5Constants.H5P_DEFAULT);
        }
        catch (Exception ex) {
            fail("H5.H5Lget_info: " + ex);
        }
        assertFalse("H5Lget_info ", link_info == null);
        assertTrue("H5Lget_info link type", link_info.type == HDF5Constants.H5L_TYPE_EXTERNAL);
        assertTrue("Link Address ", link_info.address_val_size > 0);

        try {
            fgrp1.close(gid);
        }
        catch (final Exception ex) {
        }

        // Close file.
        try {
            file1.close();
        }
        catch (final Exception ex) {
        }

        try {
            file2.close();
        }
        catch (final Exception ex) {
        }
        file1.delete();
        file2.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#getAttribute(int, int, int)}.
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>create group
     * <li>create 3 attributes in a group
     * <li>open group
     * <li>retrieves attributes in alphabetical or creation order
     * <li>check the attribute name equals the attribute name retrieved from list
     * <li>close group
     * <li>close/delete the files
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testCreateAttribute() {
        log.debug("testCreateAttribute");
        final String nameNew = "TESTFILEAttr1.h5";
        H5File file = null;
        long fid = -1;
        Group g1 = null;
        Dataset d1 = null;

        try {
            file = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE); // Create File1.
        }
        catch (final Exception ex) {
            fail("file1.create() failed. " + ex);
        }
        try {
            fid = file.open();
        }
        catch (final Exception ex) {
            fail("file1.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        try {
            g1 = file.createGroup("Group1", null);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(g1);

        Attribute attr1 = new Attribute(g1, "intAttr", new H5Datatype(Datatype.CLASS_INTEGER, 4, -1, -1),
                new long[] { 10 }, new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        Attribute attr2 = new Attribute(g1, "strAttr", new H5Datatype(Datatype.CLASS_STRING, 20, -1, -1),
                new long[] { 1 },
                new String[] { "String attribute." });

        Attribute attr3 = new Attribute(g1, "floatAttr", new H5Datatype(Datatype.CLASS_FLOAT, 4, -1, -1),
                new long[] { 2 },
                new float[] { 2, 4 });

        try {
            attr1.write();
            attr2.write();
            attr3.write();
        }
        catch (final Exception ex) {
            fail("g1.writeMetadata() failed. " + ex);
        }

        long gid = -1;
        try {
            gid = g1.open();
        }
        catch (Exception ex) {
            fail("g1.open()failed. " + ex);
        }
        List attributeList = null;
        try {
            attributeList = H5File.getAttribute(g1, HDF5Constants.H5_INDEX_CRT_ORDER, HDF5Constants.H5_ITER_INC);
            // Retrieve attributes in increasing creation order.
            assertEquals(attr2.getName(), attributeList.get(1).toString());
        }
        catch (final Exception ex) {
            fail("file.getAttribute() failed. " + ex);
        }

        try {
            attributeList = H5File.getAttribute(g1);
            // Retrieve attributes in increasing alphabetical order.
            assertEquals(attr2.getName(), attributeList.get(2).toString());
        }
        catch (final Exception ex) {
            fail("file.getAttribute() failed. " + ex);
        }

        try {
            g1.close(gid);
        }
        catch (final Exception ex) {
        }

        try {
            file.close(); // Close file.
        }
        catch (final Exception ex) {
        }

        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#createDatatype(int, int, int, int, java.lang.String)}.
     * {@link hdf.object.h5.H5Datatype#hasAttribute()}.
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>open file
     * <li>create Datatype
     * <li>create attribute in Datatype.
     * <li>check for attribute in datatype.
     * <li>close/delete the file
     * </ul>
     */
    @Test
    public void testDatatypehasAttribute() {
        log.debug("testDatatypehasAttribute");
        final String nameNew = "testH5FileDatatype.h5";
        H5File file = null;
        long fid = -1;
        Datatype d1 = null;

        try {
            file = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE); // Create File
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }

        try {
            fid = file.open();
        }
        catch (final Exception ex) {
            fail("file.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        try {
            d1 = file.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.ORDER_LE, Datatype.SIGN_NONE, "NATIVE_INT");
            // create Datatype.
        }
        catch (final Exception ex) {
            fail("file.createDatatype() failed. " + ex);
        }
        assertNotNull(d1);

        Attribute attr1 = new Attribute(d1, "strAttr", new H5Datatype(Datatype.CLASS_STRING, 20, -1, -1),
                new long[] { 1 },
                new String[] { "String attribute." });

        try {
            attr1.write();
        }
        catch (final Exception ex) {
            fail("d1.writeMetadata() failed. " + ex);
        }

        assertEquals(true, d1.hasAttribute());

        try {
            file.close();
        }
        catch (final Exception ex) {
        }
        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File#renameAttribute(HObject, java.lang.String, java.lang.String)}.
     * <p>
     * What to test:
     * <ul>
     * <li>create a file
     * <li>open file
     * <li>create group and datatype
     * <li>create attribute in the group and datatype.
     * <li>rename the attribute.
     * <li>close/delete the file
     * </ul>
     */
    @Test
    public void testrenameAttribute() {
        log.debug("testrenameAttribute");
        final String nameNew = "testAttrName.h5";
        H5File file = null;
        long fid = -1;
        Group g1 = null;
        Datatype t1 = null;
        Dataset d1 = null;

        try {
            file = (H5File) H5FILE.createFile(nameNew, FileFormat.FILE_CREATE_DELETE); // Create File
        }
        catch (final Exception ex) {
            fail("file.create() failed. " + ex);
        }

        try {
            fid = file.open();
        }
        catch (final Exception ex) {
            fail("file.open() failed. " + ex);
        }
        assertTrue(fid > 0);

        try {
            g1 = file.createGroup("G1", null);
        }
        catch (final Exception ex) {
            fail("file.createGroup() failed. " + ex);
        }
        assertNotNull(g1);

        try {
            t1 = file.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.ORDER_LE, Datatype.SIGN_NONE, "NATIVE_INT");
        }
        catch (final Exception ex) {
            fail("file.createDatatype() failed. " + ex);
        }
        assertNotNull(t1);

        try {
            d1 = file.createScalarDS("TEST_DATASET", g1, t1, new long[1], null, null, 0, null);
        }
        catch (final Exception ex) {
            fail("file.createScalarDS() failed. " + ex);
        }
        assertNotNull(d1);

        Attribute attr1 = new Attribute(g1, "strAttr", new H5Datatype(Datatype.CLASS_STRING, 20, -1, -1),
                new long[] { 1 },
                new String[] { "String attribute." });

        try {
            attr1.write();
        }
        catch (final Exception ex) {
            fail("g1.writeMetadata() failed. " + ex);
        }
        try {
            attr1.setParentObject(t1);
            attr1.write();
        }
        catch (final Exception ex) {
            fail("d1.writeMetadata() failed. " + ex);
        }
        try {
            attr1.setParentObject(d1);
            attr1.write();
        }
        catch (final Exception ex) {
            fail("d1.writeMetadata() failed. " + ex);
        }

        try {
            file.renameAttribute(g1, "strAttr", "GroupAttribute");
            file.renameAttribute(t1, attr1.getName(), "DatatypeAttribute");
            file.renameAttribute(d1, attr1.getName(), "DatasetAttribute");
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("file.changeAttrName() failed. " + ex);
        }

        try {
            file.close();
        }
        catch (final Exception ex) {
        }
        file.delete();
    }

    /**
     * Test method for {@link hdf.object.h5.H5File} IsSerializable.
     */
    @Test
    public void testIsSerializable() {
        log.debug("testIsSerializable");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(testFile);
            oos.close();
        }
        catch (IOException err) {
            err.printStackTrace();
            fail("ObjectOutputStream failed: " + err);
        }
        assertTrue(out.toByteArray().length > 0);

    }

    /**
     * Test method for {@link hdf.object.h5.H5File} SerializeToDisk.
     * <p>
     * What to test:
     * <ul>
     * <li>serialize a dataset identifier
     * <li>deserialize a dataset identifier
     * <li>open a file identifier
     * <li>check the file content
     * <li>close the file
     * </ul>
     */
    @Ignore
    public void testSerializeToDisk() {
        log.debug("testSerializeToDisk");
        try {

            FileOutputStream fos = new FileOutputStream("temph5file.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(testFile);
            oos.close();
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        closeFile();

        try {
            FileInputStream fis = new FileInputStream("temph5file.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            testFile = (hdf.object.h5.H5File) ois.readObject();
            ois.close();

            // Clean up the file
            //new File("temph5file.ser").delete();

            /*
             * xcao: no need to test lib version. it is tested at the hdf5lib level String tver = testFile.getLibversion();
             * String H5ver = "HDF5 " + H5.LIB_VERSION[0] + "." + H5.LIB_VERSION[1] + "." + H5.LIB_VERSION[2];
             * assertEquals("H5.LIB_VERSION", tver, H5ver);
             */
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        // try to get all object in the file
        try {
            for (int j = 0; j < H5TestFile.OBJ_NAMES.length; j++) {
                assertNotNull(testFile.get(H5TestFile.OBJ_NAMES[j]));
            }
        }
        catch (final Exception ex) {
            fail("test.get() failed. " + ex);
        }
        long nObjs = 0;

        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertTrue(nObjs <= 1); // only file id should be open.
    }
}
