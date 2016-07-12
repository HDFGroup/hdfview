package test.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Metadata;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Rishi R. Sinha This has to be removed because both the methods tested here are actually abstract methods and
 *         should be tested elsewhere.
 *
 */
public class MetadataTest {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataTest.class);
    private static final H5File H5FILE = new H5File();

    private H5File testFile = null;
    private H5Group testGroup = null;
    private Metadata strAttr = null;
    private Metadata arrayIntAttr = null;

    @BeforeClass
    public static void createFile() throws Exception {
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
                System.out.println("Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Before
    public void openFiles() throws Exception {
        testFile = (H5File) H5FILE.open(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        assertNotNull(testFile);
        testGroup = (H5Group) testFile.get(H5TestFile.NAME_GROUP_ATTR);
        assertNotNull(testGroup);
        List testAttrs = testGroup.getMetadata();
        assertNotNull(testAttrs);
        Attribute attrobj = (Attribute) testAttrs.get(0);
        if(attrobj.getType().getDatatypeClass()==Datatype.CLASS_STRING)
            strAttr = attrobj;
        else
            arrayIntAttr = attrobj;
        attrobj = (Attribute) testAttrs.get(1);
        if(attrobj.getType().getDatatypeClass()==Datatype.CLASS_STRING)
            strAttr = attrobj;
        else
            arrayIntAttr = attrobj;
        assertNotNull(strAttr);
        assertNotNull(arrayIntAttr);
    }

    @After
    public void removeFiles() throws Exception {
        // make sure all objects are closed
        final long fid = testFile.getFID();
        if (fid > 0) {
            long nObjs = 0;
            try {
                nObjs = H5.H5Fget_obj_count(fid, HDF5Constants.H5F_OBJ_ALL);
            }
            catch (final Exception ex) {
                fail("H5.H5Fget_obj_count() failed. " + ex);
            }
            assertEquals(1, nObjs); // file id should be the only one left open
        }

        if (testFile != null) {
            try {
                testFile.close();
            }
            catch (final Exception ex) {
            }
            testFile = null;
        }
    }

    /**
     * Test method for {@link hdf.object.Metadata#getValue()}.
     */
    @Test
    public void testGetValue() {
        log.debug("testGetValue");
        String[] value = (String[]) strAttr.getValue();
        if (!value[0].equals("String attribute.")) {
            fail("getValue() fails.");
        }

        int[] intValue = (int[]) arrayIntAttr.getValue();

        for (int i = 0; i < 10; i++) {
            if (intValue[i] != i + 1) {
                fail("getValue() fails");
            }
        }
    }

    /**
     * Test method for {@link hdf.object.Metadata#setValue(java.lang.Object)}.
     */
    @Test
    public void testSetValue() {
        log.debug("testSetValue");
        String[] tempValue = { "Temp String Value" };
        String[] prevValue = (String[]) strAttr.getValue();
        strAttr.setValue(tempValue);
        String[] value = (String[]) strAttr.getValue();
        if (!value[0].equals("Temp String Value")) {
            fail("setValue() fails.");
        }
        strAttr.setValue(prevValue);

        int[] tempIntArray = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        int[] intPrevValue = (int[]) arrayIntAttr.getValue();
        arrayIntAttr.setValue(tempIntArray);

        int[] intValue = (int[]) arrayIntAttr.getValue();

        for (int i = 0; i < 10; i++) {
            if (intValue[i] != i) {
                fail("getValue() fails");
            }
        }
        arrayIntAttr.setValue(intPrevValue);
    }

}
