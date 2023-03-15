package object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Iterator;
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
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

/**
 * @author Rishi R Sinha
 *
 */
public class GroupTest
{
    private static final Logger log = LoggerFactory.getLogger(GroupTest.class);

    private H5File testFile = null;
    private Group testGroup = null;

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
                System.out.println("GroupTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("GroupTest AfterClass: Number of IDs still open: " + openID);
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
        H5File H5FILE = new H5File();
        testFile = (H5File) H5FILE.open(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        assertNotNull(testFile);
        testGroup = (Group) testFile.get(H5TestFile.NAME_GROUP);
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
     * Test method for {@link hdf.object.Group#clear()}.
     *
     * What to test:
     * <ul>
     * <li>For the root group clear the list.
     * </ul>
     * </ul>
     */
    @Test
    public void testClear() {
        log.debug("testClear");
        testGroup.clear();
        assertEquals(testGroup.getMemberList().size(), 0);
    }

    /**
     * Test method for {@link hdf.object.Group#addToMemberList(hdf.object.HObject)}.
     *
     * What to test:
     * <ul>
     * <li>Test for boundary conditions
     * <ul>
     * <li>Add null to the member list.
     * </ul>
     * <li>Test for failure
     * <ul>
     * <li>Add an already existing object to the list.
     * </ul>
     * <li>Test for general functionality
     * <ul>
     * <li>add a group to it.
     * </ul>
     * </ul>
     */
    @Test
    public void testAddToMemberList() {
        log.debug("testAddToMemberList");
        int previous_size = testGroup.getMemberList().size();
        testGroup.addToMemberList(null);
        assertEquals(testGroup.getMemberList().size(), previous_size);

        H5.H5error_off();
        Group tmp = new H5Group(testFile, "tmp", "/grp0/", testGroup);
        H5.H5error_on();
        testGroup.addToMemberList(testGroup.getMemberList().get(0));

        if (testGroup.getMemberList().size() != previous_size)
            fail("addToMemberList adds an existing member to the member list.");

        testGroup.addToMemberList(tmp);
        if (!testGroup.getMemberList().get(previous_size).equals(tmp))
            fail("Add to member list does not add to the end.");
        if (testGroup.getMemberList().size() != previous_size + 1)
            fail("Add to member list not working.");
    }

    /**
     * Test method for {@link hdf.object.Group#removeFromMemberList(hdf.object.HObject)} .
     *
     * What to test:
     * <ul>
     * <li>Test for boundary conditions
     * <ul>
     * <li>Remove a null from the member list.
     * </ul>
     * <li>Test for failure
     * <ul>
     * <li>Remove a non existing object to the list.
     * </ul>
     * <li>Test for general functionality
     * <ul>
     * <li>Remove a group from it.
     * </ul>
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRemoveFromMemberList() {
        log.debug("testRemoveFromMemberList");
        int previous_size = testGroup.getMemberList().size();
        List memberList = testGroup.getMemberList();

        H5.H5error_off();
        testGroup.removeFromMemberList(null);
        if (testGroup.getMemberList().size() != previous_size)
            fail("removeFromMemberList removes a null from the member list.");

        Group tmp = new H5Group(testFile, "tmp", "/grp0/", testGroup);
        testGroup.removeFromMemberList(tmp);
        if (testGroup.getMemberList().size() != previous_size)
            fail("removeFromMemberList removes a non existing member from the member list.");
        H5.H5error_on();

        Iterator it = memberList.iterator();
        HObject obj = (HObject) it.next();
        testGroup.removeFromMemberList(obj);

        if (memberList.size() != previous_size - 1)
            fail("The Number of members in list should be " + (previous_size - 1));
    }

    /**
     * Test method for {@link hdf.object.Group#getMemberList()}.
     *
     * <ul>
     * <li>testing the member list for the root group.
     * <ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMemberList() {
        log.debug("testGetMemberList");
        String objs[] = { "a_link_to_the_image", "dataset_comp", "dataset_int", "datatype_float", "datatype_int",
                "datatype_str", "datatype_uint", "g00" };
        List memberList = testGroup.getMemberList();
        Iterator it = memberList.iterator();
        int position = 0;
        while (it.hasNext()) {
            HObject obj = (HObject) it.next();
            assertEquals(objs[position++], obj.getName());
        }
    }

    /**
     * Test method for {@link hdf.object.Group#getParent()}.
     *
     * <ul>
     * <li>Test to get the parent of group g0.
     * </ul>
     */
    @Test
    public void testGetParent() {
        log.debug("testGetParent");
        assertEquals(testGroup.getParent().getName(), "/");
    }

    /**
     * Test method for {@link hdf.object.Group#isRoot()}.
     *
     * <ul>
     * <li>Test for not root.
     * </ul>
     */
    @Test
    public void testIsRoot() {
        log.debug("testIsRoot");
        assertFalse(testGroup.isRoot());
    }

    /**
     * Test method for {@link hdf.object.Group#getNumberOfMembersInFile()}.
     *
     * <ul>
     * <li>Test for the number of members in the file.
     * <ul>
     */
    @Test
    public void testGetNumberOfMembersInFile() {
        log.debug("testGetNumberOfMembersInFile");
        assertEquals(testGroup.getNumberOfMembersInFile(), 8);
    }

}
