/**
 *
 */
package object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Dataset;
import hdf.object.FileFormat;
import hdf.object.h5.H5File;

/**
 * @author rsinha
 *
 */
public class DatasetTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetTest.class);
    private static final H5File H5FILE = new H5File();

    private H5File testFile = null;
    String[] dsetNames = {
            H5TestFile.NAME_DATASET_INT, H5TestFile.NAME_DATASET_FLOAT, H5TestFile.NAME_DATASET_CHAR,
            H5TestFile.NAME_DATASET_STR, H5TestFile.NAME_DATASET_ENUM,
            H5TestFile.NAME_DATASET_COMPOUND,
            H5TestFile.NAME_DATASET_IMAGE };
    private Dataset[] dSets = new Dataset[dsetNames.length];

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
                System.out.println("DatasetTest BeforeClass: Number of IDs still open: " + openID);
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
                System.out.println("DatasetTest AfterClass: Number of IDs still open: " + openID);
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
        try {
            testFile = (H5File) H5FILE.open(H5TestFile.NAME_FILE_H5, FileFormat.READ);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotNull(testFile);

        for (int i = 0; i < dSets.length; i++) {
            try {
                dSets[i] = (Dataset) testFile.get(dsetNames[i]);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            assertNotNull(dSets[i]);
        }
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
     * For each dataset in the file we are:
     * <ul>
     * <li>checking the chunk size for the datasets.
     * <li>checking details about compression.
     * <li>cheking whether the byte array is to be converted to string.
     * <li>checking the dimension names.
     * <li>checking the dimension sizes.
     * <li>checking the rank.
     * <li>checking the selected dimensions.
     * <li>checking the selected indexes.
     * <li>checking the startdims.
     * <li>checking the stride.
     * <li>checking the width.
     * </ul>
     */
    @Test
    public void testMetadataAssociatedWithDataset() {
        log.debug("testMetadataAssociatedWithDataset");
        log.debug("loop: Number of IDs still open: " + H5.getOpenIDCount());
        for (int i = 0; i < dsetNames.length; i++) {
            dSets[i].init();
            log.debug("init: Number of IDs still open: " + H5.getOpenIDCount());
            log.debug("testMetadataAssociatedWithDataset current={} is  {}", i, dSets[i].getName());
            log.debug("name: Number of IDs still open: " + H5.getOpenIDCount());
            assertNull(dSets[i].getChunkSize());
            assertTrue(dSets[i].getCompression().equals("NONE"));
            assertTrue(dSets[i].getConvertByteToString()); // by default, strings are converted
            assertNull(dSets[i].getDimNames());
            log.debug("testMetadataAssociatedWithDataset current dims={}", dSets[i].getDims());
            assertTrue(Arrays.equals(dSets[i].getDims(), H5TestFile.DIMs));
            assertEquals(dSets[i].getHeight(), H5TestFile.DIM1);
            assertEquals(dSets[i].getRank(), H5TestFile.RANK);
            long[] array = new long[2];
            array[0] = 50;
            array[1] = 10;
            assertTrue(Arrays.equals(dSets[i].getSelectedDims(), array));
            int[] arrayInt = new int[3];
            arrayInt[0] = 0;
            arrayInt[1] = 1;
            arrayInt[2] = 2;
            assertTrue(Arrays.equals(dSets[i].getSelectedIndex(), arrayInt));
            array[0] = 0;
            array[1] = 0;
            assertTrue(Arrays.equals(dSets[i].getStartDims(), array));
            array[0] = 1;
            array[1] = 1;
            assertTrue(Arrays.equals(dSets[i].getStride(), array));
            assertEquals(dSets[i].getWidth(), H5TestFile.DIM2);
        }
    }

    /**
     * Test converting the following unsigned values to signed values.
     * <ul>
     * <li>byte[] int8 = { - 1, - 128, 127, 0};
     * <li>short[] int16 = { - 1, - 32768, 32767, 0};
     * <li>int[] int32 = { - 1, - 2147483648, 2147483647, 0};
     * </ul>
     * Expected values
     * <ul>
     * <li>short[] uint8 = {255, 128, 127, 0};
     * <li>int[] uint16 = {65535, 32768, 32767, 0};
     * <li>long[] uint32 = {4294967295L, 2147483648L, 2147483647, 0};
     * </ul>
     */
    @Test
    public void testConvertFromUnsignedC() {
        log.debug("testConvertFromUnsignedC");
        byte[] int8 = { -1, -128, 127, 0 };
        short[] int16 = { -1, -32768, 32767, 0 };
        int[] int32 = { -1, -2147483648, 2147483647, 0 };

        short[] uint8 = { 255, 128, 127, 0 };
        int[] uint16 = { 65535, 32768, 32767, 0 };
        long[] uint32 = { 4294967295L, 2147483648L, 2147483647, 0 };

        short[] expected8 = (short[]) Dataset.convertFromUnsignedC(int8, null);
        assertTrue(Arrays.equals(expected8, uint8));

        int[] expected16 = (int[]) Dataset.convertFromUnsignedC(int16, null);
        assertTrue(Arrays.equals(expected16, uint16));

        long[] expected32 = (long[]) Dataset.convertFromUnsignedC(int32, null);
        assertTrue(Arrays.equals(expected32, uint32));
    }
}
