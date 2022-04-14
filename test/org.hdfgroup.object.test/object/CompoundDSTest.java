package object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.CompoundDS;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5File;

/**
 * @author rsinha
 *
 */
public class CompoundDSTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompoundDSTest.class);
    private static final H5File H5FILE = new H5File();

    private H5File testFile = null;
    private CompoundDS testDS = null;

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
                System.out.println("CompoundDSTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("CompoundDSTest AfterClass: Number of IDs still open: " + openID);
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
            testFile = (H5File) H5FILE.createInstance(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testFile);

        try {
            testDS = (CompoundDS) testFile.get(H5TestFile.NAME_DATASET_COMPOUND);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testDS);
        testDS.init();
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
     * For the Compund Dataset in the Test File, we are checking
     * <ul>
     * <li>geting the memberCount.
     * <li>the names of each member in the dataset.
     * <li>the types of each member in the dataset.
     * <li>the orders of each member in the dataset.
     * <li>the dims of each member in the dataset.
     * </ul>
     */
    @Test
    public void testFieldsHaveCorrectNameTypeOrderAndDims() {
        log.debug("testFieldsHaveCorrectNameTypeOrderAndDims");
        int correctMemberCount = H5TestFile.COMPOUND_MEMBER_NAMES.length;
        assertEquals(testDS.getMemberCount(), correctMemberCount);
        String[] names = testDS.getMemberNames();
        for (int i = 0; i < correctMemberCount; i++) {
            if (!names[i].equals(H5TestFile.COMPOUND_MEMBER_NAMES[i])) {
                fail("Member Name at position " + i + " should be " + H5TestFile.COMPOUND_MEMBER_NAMES[i]
                        + ", while getMemberNames returns " + names[i]);
            }
        }
        Datatype[] types = testDS.getMemberTypes();
        for (int i = 0; i < correctMemberCount; i++) {
            if (!types[i].getDescription().startsWith(H5TestFile.COMPOUND_MEMBER_DATATYPES[i].getDescription())) {
                fail("Member Type at position " + i + " should be "
                        + H5TestFile.COMPOUND_MEMBER_DATATYPES[i].getDescription() + ", while getMemberTypes returns " + types[i].getDescription());
            }
        }
        int[] orders = testDS.getMemberOrders();
        for (int i = 0; i < correctMemberCount; i++) {
            if (orders[i] != 1) {
                fail("Member Order at position " + i + " should be " + 1 + ", while getMemberOrders returns "
                        + orders[i]);
            }
        }
        for (int i = 0; i < correctMemberCount; i++) {
            assertNull(testDS.getMemberDims(i)); // all scalar data
        }
    }

    /**
     * For the Compund Dataset in the Test File, we are checking
     * <ul>
     * <li>Geting the selectMemberCount method on the default selection.
     * <li>setting ths member selection so that no member is selected.
     * <li>setting the member selection so that all members are exlplicitly selected.
     * <li>Adding one member at a time and checking if the addition is working properly.
     * </ul>
     */
    @Test
    public void testSelectionDeselectionCountWorks() {
        log.debug("testSelectionDeselectionCountWorks");
        if (testDS.getSelectedMemberCount() != H5TestFile.COMPOUND_MEMBER_NAMES.length) {
            fail("Right after init getSelectedMemberCount returns" + testDS.getSelectedMemberCount()
            + ", when it should return " + H5TestFile.COMPOUND_MEMBER_NAMES.length);
        }

        testDS.setAllMemberSelection(false);
        assertEquals(testDS.getSelectedMemberCount(), 0);
        testDS.setAllMemberSelection(true);
        assertEquals(testDS.getSelectedMemberCount(), H5TestFile.COMPOUND_MEMBER_NAMES.length);
        testDS.setAllMemberSelection(false);
        assertEquals(testDS.getSelectedMemberCount(), 0);

        for (int i = 0; i < testDS.getMemberCount(); i++) {
            testDS.selectMember(i);
            int[] orders = testDS.getSelectedMemberOrders();
            Datatype[] types = testDS.getMemberTypes();
            for (int j = 0; j <= i; j++) {
                if (!testDS.isMemberSelected(j)) {
                    fail("Member " + j + " was selected while isMemberSelected says it wasnt.");
                }
                if (orders[j] != 1) {
                    fail("Member Order at position " + j + " should be " + 1 + ", while getMemberOrders returns " + orders[j]);
                }
                if (!types[j].getDescription().startsWith(H5TestFile.COMPOUND_MEMBER_DATATYPES[j].getDescription())) {
                    fail("Member Type at position " + i + " should be " + H5TestFile.COMPOUND_MEMBER_DATATYPES[j].getDescription() + ", while getMemberTypes returns "
                            + types[j].getDescription());
                }
            }
        }
    }
}
