package test.object;

import static org.junit.Assert.*;

import hdf.hdf5lib.H5;
import hdf.object.FileFormat;
import hdf.object.h4.H4File;
import hdf.object.h5.H5File;

import org.junit.*;

import java.util.*;

/**
 * @author rsinha, jacob smith
 * 
 */
public class FileFormatTest {
    private static Map<String, FileFormat> A_PRIORI_FORMATS = new HashMap<>(5);
    private static String A_PRIORI_EXTENSIONS = "";
    private FileFormat testFile = null;

    @BeforeClass
    public static void initializeConstantState() throws Exception {
        assert(0 == H5.getOpenIDCount());
        H5TestFile.createTestFile(null);

        for (String k : FileFormat.getFileFormatKeys())
            A_PRIORI_FORMATS.put(k, FileFormat.getFileFormat(k));

        StringBuilder builder = new StringBuilder();
        for (String s : FileFormat.getFileExtensions())
            builder.append(s);
        A_PRIORI_EXTENSIONS = builder.toString();
    }

    @AfterClass
    public static void afterTestBackout() {
        FileFormat.setFileExtensions(A_PRIORI_EXTENSIONS);
        removeAllFormats();
        repopulateFormats(A_PRIORI_FORMATS);

        H5File file = new H5File(
                H5TestFile.NAME_FILE_H5, FileFormat.FILE_CREATE_DELETE);
        file.delete();
        assert(0 == H5.getOpenIDCount());
    }

    @Before
    public void setup() throws Exception {
        assert(0 == H5.getOpenIDCount());

        testFile = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        testFile.open();

        removeAllFormats();
        FileFormat.clearFileExtensions();
    }

    @After
    public void teardown() throws Exception {
        testFile.close();
        testFile = null;

        removeAllFormats();
        repopulateFormats(A_PRIORI_FORMATS);

        assert(0 == H5.getOpenIDCount());
    }

    private static void removeAllFormats() {
        // funky temporary storage to side-step concurrent modification
        Object[] l = FileFormat.getFileFormatKeys().toArray();
        for (Object o : l)
            FileFormat.removeFileFormat((String) o);
    }

    private static void repopulateFormats(Map<String, FileFormat> map) {
        for (String k : map.keySet())
            FileFormat.addFileFormat(k, map.get(k));
    }

    /**
     * populate FileFormat supported format with subset of pre-loaded
     *
     * re-adds key/format elements into FileFormat's Formats
     * @param keys array of format keys--each key must exist in FileFormat
     *             on startup, prior to tests
     */
    private void withAPrioriFormats(String[] keys) {
        for (String key : keys) {
            assert (A_PRIORI_FORMATS.containsKey(key));
            FileFormat.addFileFormat(key, A_PRIORI_FORMATS.get(key));
        }
    }

    @Test
    public void testGetNumberOfMembers() {
        assertEquals(testFile.getNumberOfMembers(), 21);
    }

    @Test
    public void testGetFID() {
        assertTrue((testFile.getFID() != -1));
    }

    @Test
    public void testModifySupportedFormats() throws Exception {
        final String h5 = "H5_FORMAT_KEY";
        final FileFormat h5Instance = H5File.class.newInstance();

        assertNull("not yet supported", FileFormat.getFileFormat(h5));

        FileFormat.addFileFormat(h5, h5Instance);
        assertTrue(FileFormat.getFileFormatKeys().contains(h5));

        assertNotNull(FileFormat.getFileFormat(h5));

        FileFormat.removeFileFormat(h5);

        assertNull("again unsupported", FileFormat.getFileFormat(h5));
    }

    @Test
    public void testAddFileFormatNullKey() throws Exception {
        assertTrue("sanity check", FileFormat.getFileFormatKeys().isEmpty());
        FileFormat.addFileFormat(null, H5File.class.newInstance());
        assertTrue("should still be empty", FileFormat.getFileFormatKeys().isEmpty());
    }

    @Test
    public void testAddFileFormatNullFormat() throws Exception {
        assertTrue("sanity check", FileFormat.getFileFormatKeys().isEmpty());
        FileFormat.addFileFormat("key", null);
        assertTrue("should still be empty", FileFormat.getFileFormatKeys().isEmpty());
    }

    @Test
    public void testAddFileFormatEmptyKey() throws Exception {
        assertTrue("sanity check", FileFormat.getFileFormatKeys().isEmpty());
        FileFormat.addFileFormat("", H5File.class.newInstance());
        assertFalse("should have an entry", FileFormat.getFileFormatKeys().isEmpty());
        assertNotNull("should get entry with empty key", FileFormat.getFileFormat(""));
    }

    @Test
    public void testAddFileFormatWhiespace() throws Exception {
        FileFormat.addFileFormat("   \t", H5File.class.newInstance());
        assertNotNull("trimmed whitespace is empty string key", FileFormat.getFileFormat(""));
    }

    @Test
    public void testAddFileFormatTrimmed() throws Exception {
        FileFormat.addFileFormat("  key \t", H5File.class.newInstance());
        assertNotNull("trimmed key", FileFormat.getFileFormat("key"));
    }

    @Ignore("format types are equivalent!")
    @Test
    public void testAddFileFormatNoOverwrite() throws Exception {
        final FileFormat h5Type = H5File.class.newInstance();
        final FileFormat h4Type = H4File.class.newInstance();
        assert(!Objects.equals(h5Type, h4Type)); // assertion error -- whoops?
        final String key = "KEY";
        FileFormat.addFileFormat(key, h4Type);
        assertEquals("should be H4", h4Type, FileFormat.getFileFormat(key));
        FileFormat.addFileFormat(key, h5Type);
        assertEquals("H4 not overwritten", h4Type, FileFormat.getFileFormat(key));
        assertNotEquals("H5 not set", h5Type, FileFormat.getFileFormat(key));
    }

    @Test
    public void testGetFileFormatKeysEmpty() {
        Set<String> keyset = FileFormat.getFileFormatKeys();
        assertNotNull(keyset);
        assertTrue("FileFormat should not have keys", keyset.isEmpty());
    }

    @Test
    public void testGetFileFormatKeys() {
        String[] format_keys = new String[] {"HDF5", "HDF4", "Fits"};
        withAPrioriFormats(format_keys);

        Set<String> keyset = FileFormat.getFileFormatKeys();
        assertNotNull(keyset);
        assertFalse("FileFormat should have keys", keyset.isEmpty());
        for (String key : format_keys) {
            assertTrue("should have  key " + key, keyset.contains(key));
        }
    }

    @Test
    public void testGetInstance() throws Exception {
        withAPrioriFormats(new String[] {"HDF5"});

        H5File file = (H5File) FileFormat.getInstance(H5TestFile.NAME_FILE_H5);
        assertNotNull(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstanceMissingFile() throws Exception {
        FileFormat.getInstance("missing_file.h5"); // no such file
    }

    @Test
    public void testGetInstanceUnsupported() throws Exception {
        assertNull(
                "unsupported format returns null",
                FileFormat.getInstance(H5TestFile.NAME_FILE_H5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstanceNull() throws Exception {
        FileFormat.getInstance(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstanceEmpty() throws Exception {
        FileFormat.getInstance("");
    }

    @Test
    public void testGetFileExtensionsEmpty() {
        verifyExtensionsMembership(null);
    }

    @Test
    public void testSetFileExtensions() {
        FileFormat.setFileExtensions("h5, hdf4");
        verifyExtensionsMembership(new String[] {".h5", ".hdf4"});
    }

    @Test
    public void testSetFileExtensionsDuplicateIgnored() {
        FileFormat.setFileExtensions("h5, h5");
        verifyExtensionsMembership(new String[] {".h5"});
    }

    @Test
    public void testSetFileExtensionsPeriodStripped() {
        FileFormat.setFileExtensions(".h5,fits");
        verifyExtensionsMembership(new String[] {".fits", ".h5"});
    }

    @Test
    public void testAddFileExtensions() {
        FileFormat.addFileExtensions("h5, lulu");
        verifyExtensionsMembership(new String[] {".lulu", ".h5"});
    }

    @Test
    public void testAddFileExtensionsAppend() {
        FileFormat.setFileExtensions("h5, hdf5, h4");
        assertFalse(FileFormat.getFileFormatKeys().contains(".hdf4"));
        FileFormat.addFileExtensions("hdf4");
        verifyExtensionsMembership(
                new String[] {".h4", ".h5", ".hdf5", ".hdf4"});
    }

    @Test
    public void testAddFileExtensionsNoEffect() {
        FileFormat.setFileExtensions("h5, hdf5, h4");
        FileFormat.addFileExtensions(null);
        FileFormat.addFileExtensions("");
        verifyExtensionsMembership(new String[] {".h4", ".h5", ".hdf5"});
    }

    @Test
    public void testSetFileExtensionsNullClears() {
        FileFormat.setFileExtensions("h5, hdf5, h4");
        verifyExtensionsMembership(new String[] {".h4", ".h5", ".hdf5"});

        FileFormat.setFileExtensions(null);
        verifyExtensionsMembership(null);
    }

    @Test
    public void testSetFileExtensionsEmptyClears() {
        FileFormat.setFileExtensions("h5, hdf5, h4");
        verifyExtensionsMembership(new String[] {".h4", ".h5", ".hdf5"});

        FileFormat.setFileExtensions("");
        verifyExtensionsMembership(null);
    }

    @Test
    public void testSetFileExtensionsSingle() {
        FileFormat.setFileExtensions("h5");
        verifyExtensionsMembership(new String[] {".h5"});
    }

    @Test
    public void testSetFileExtensionsSemicolonSep() {
        //TODO: sanitize string inputs?
        FileFormat.setFileExtensions("h5; uluru");
        verifyExtensionsMembership(new String[] {".h5; uluru"});
    }

    @Test
    public void testAddFileExtensionsSemicolonSep() {
        //TODO: sanitize string inputs?
        FileFormat.addFileExtensions("h5; uluru");
        verifyExtensionsMembership(new String[] {".h5; uluru"});
    }

    /**
     * compare the set returned by FileFormat.getFileExtensions() against an
     * arrays of strings that should comprise the set
     * @param contains array of formats to require.
     *                 If null, set should be empty.
     */
    private void verifyExtensionsMembership(String[] contains) {
        Set<String> extensions = FileFormat.getFileExtensions();
        assertNotNull("set must exist", extensions);
        if (contains == null || contains.length == 0) {
            assertTrue("set should be empty", extensions.isEmpty());
            return;
        }
        else {
            assertFalse("set should not be empty", extensions.isEmpty());
        }
        assertEquals(contains.length, extensions.size());
        for (String s : contains)
            assertTrue("should contain " + s,
                    extensions.contains(s));
    }

}
