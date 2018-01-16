package test.object;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5SymbolTableException;
import hdf.hdf5lib.structs.H5L_info_t;
import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h4.H4File;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;
import hdf.object.h5.H5ScalarDS;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * TestCase for H5File.
 *
 * @author Peter Cao, Jake Smith, The HDF Group
 */
public class H5FileTest {
    private static final H5File H5FILE = new H5File();
    private static final String CLEAN_FILE_NAME = "CleanFile.h5";
    private H5File cleanFile = null;
    private H5Datatype typeInt = null;
    private H5File testFile = null;

    @BeforeClass
    public static void createTestFile() throws Exception {
        assert( H5.getOpenIDCount() < 1 );
        H5TestFile.createTestFile(null);
    }

    @AfterClass
    public static void checkIDsAndExpungeTestFile() {
        assert( H5.getOpenIDCount() < 1 );
        H5File file = new H5File(
                H5TestFile.NAME_FILE_H5, FileFormat.FILE_CREATE_DELETE);
        file.delete();
    }

    @Before
    public void openTestFile() throws Exception {
        assert( H5.getOpenIDCount() < 1 );
        typeInt = new H5Datatype(
                Datatype.CLASS_INTEGER, H5TestFile.DATATYPE_SIZE, -1, -1);

        testFile = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        testFile.open();

        cleanFile = (H5File) H5FILE.createFile(
                CLEAN_FILE_NAME, FileFormat.FILE_CREATE_DELETE);
        cleanFile.open();
    }

    @After
    public void closeTestFile() throws Exception {
        testFile.close();
        cleanFile.close();
        cleanFile.delete();

        assert( H5.getOpenIDCount() < 1 );
    }

    @Test
    public void testOpen() throws Exception {
        testFile.close();

        H5File file = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        long fid = file.open();
        assertTrue("unable to open file", fid >= 0);

        file.close();
        testFile.open();
    }

    @Test(expected=Exception.class)
    public void testOpenFailure() throws Exception {
        H5File file = new H5File
                ("name_of_nonexistent_file", FileFormat.FILE_CREATE_DELETE);
        file.open(); // "file does not exist"
    }

    @Test
    public void testOpenTestFileWithFAPL_ID() throws Exception {
        testFile.close();

        final H5File file =
                new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        final long fapl_id = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
        assertTrue("unable to create default fapl", fapl_id >= 0);
        // H5Pset... is not necessary, but demonstrates fapl manipulation
        H5.H5Pset_fclose_degree(fapl_id, HDF5Constants.H5F_CLOSE_STRONG);
        file.open(fapl_id);

        H5.H5Pclose(fapl_id);
        file.close();
        testFile.open();
    }

    @Test
    public void testCreateFileByNameAndFlag() throws Exception {
        H5File file = (H5File) H5FILE.createFile(
                "testH5File.h5", FileFormat.FILE_CREATE_DELETE);
        file.open();

        file.close();
        file.delete();
    }

    @Test
    public void testGetRootObject() {
        final HObject root = testFile.getRootObject();
        assertNotNull(root);
        assertTrue(((Group) root).breadthFirstMemberList().size() > 0);
    }

    @Test
    public void testIsReadOnly() throws Exception {
        assertFalse(testFile.isReadOnly());

        H5File file = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.READ);
        file.open();
        assertTrue(file.isReadOnly());
        assertFalse(testFile.isReadOnly());
        file.close();
    }

    @Test
    public void testCreateGroup() throws Exception {
        final Group group = testFile.createGroup("new group", null);
        assertNotNull(group);
        final long gid = group.open();
        assertTrue(gid > 0);
        group.close(gid);
    }

    @Test
    public void testCreateGroupInvalidPropertyListOrder_GCPL_LCPL()
    throws Exception {
        final long lcpl = H5.H5Pcreate(HDF5Constants.H5P_LINK_CREATE);
        assertTrue(lcpl >= 0);
        H5.H5Pset_create_intermediate_group(lcpl, true);
        final long gcpl = H5.H5Pcreate(HDF5Constants.H5P_GROUP_CREATE);
        assertTrue(gcpl >= 0);
        H5.H5Pset_link_creation_order(gcpl, HDF5Constants.H5P_CRT_ORDER_TRACKED
                + HDF5Constants.H5P_CRT_ORDER_INDEXED);// Set link creation order

        boolean creationFailed = false;
        try {
            Group group = testFile.createGroup(
                    "Group1/Group2/Group3", null, gcpl, lcpl);
        }
        catch (final Exception ex) {
            creationFailed = true;
        }
        assertTrue(creationFailed);

        H5.H5Pclose(lcpl);
        H5.H5Pclose(gcpl);
    }

    @Test
    public void testCreateGroupWithPropertyList() throws Exception {
        final long lcpl = H5.H5Pcreate(HDF5Constants.H5P_LINK_CREATE);
        assertTrue(lcpl >= 0);
        H5.H5Pset_create_intermediate_group(lcpl, true);
        final long gcpl = H5.H5Pcreate(HDF5Constants.H5P_GROUP_CREATE);
        assertTrue(gcpl >= 0);
        H5.H5Pset_link_creation_order(gcpl, HDF5Constants.H5P_CRT_ORDER_TRACKED
                + HDF5Constants.H5P_CRT_ORDER_INDEXED);// Set link creation order

        Group group = testFile.createGroup(
                "Group1/Group2/Group3", null, lcpl, gcpl);
        assertNotNull(group);

        final long group_id = group.open();

        // create "children" subgroups
        Group grp2 = testFile.createGroup("G4", group);
        assertNotNull(grp2);
        Group grp3 = testFile.createGroup("G3", group);
        assertNotNull(grp3);

        assertEquals(
                "G3",
                H5.H5Lget_name_by_idx(group_id,
                        ".",
                        HDF5Constants.H5_INDEX_CRT_ORDER,
                        HDF5Constants.H5_ITER_INC,
                        1,
                        HDF5Constants.H5P_DEFAULT));

        group.close(group_id);
        H5.H5Pclose(lcpl);
        try {
            H5.H5Pclose(gcpl);
        }
        catch (final hdf.hdf5lib.exceptions.HDF5AtomException ex) {
            //TODO: what causes this exception?
            //   ...Error is irrelevant as pertaining to H5File testing, however
            //   ...it would be good to track down and resolve.
        }
    }

    @Test
    public void testCreateGcplWithArbitraryValues() throws Exception {
        final long gcpl_id =
                testFile.createGcpl(Group.CRT_ORDER_INDEXED, 5, 3);
        assertTrue(gcpl_id >= 0);
        H5.H5Pclose(gcpl_id);
    }

    @Test
    public void testIsThisType() {
        assertTrue(H5FILE.isThisType(H5TestFile.NAME_FILE_H5));
        assertTrue(H5FILE.isThisType(testFile));
        assertFalse(H5FILE.isThisType("No such file"));
        assertFalse(H5FILE.isThisType(new H4File("somefile.h4")));
    }

    @Test(expected=NullPointerException.class)
    public void testIsThisTypeInvalidInput() throws NullPointerException {
        H5FILE.isThisType((String) null); // String cast to resolve op overload
    }

    @Test
    public void testCopy() throws Exception {
        Group root = (Group) testFile.get("/");
        final List<HObject> members = root.getMemberList();
        assert( members.size() > 0 );

        root = (Group) cleanFile.get("/");
        assertNotNull(root);

        // copy all the objects to the new file
        for (HObject sourceObject : members) {
            HObject destinationObject =
                    testFile.copy(
                            sourceObject, root, sourceObject.getFullName());
            String destinationName = destinationObject.getFullName();

            assertNotNull(cleanFile.get(destinationName));
        }
    }

    // TODO: H5file.copy() invalid copies

    @Test
    public void testDelete() throws Exception {
        H5File file = H5TestFile.createTestFile("h5FileToDeleteObjs.h5");
        file.open();
        final Group root = (Group) file.get("/");
        final List<HObject> members = root.getMemberList();
        final int n = members.size();
        assertTrue("file has no objects to delete", n > 0 );

        final Object[] memberArray = members.toArray();
        for (int i = 0; i < n; i++) {
            HObject obj = (HObject) memberArray[i];
            file.delete(obj);

            // re-open the file to make sure the object is written to file
            // TODO: file.flush() ?
            file.close();
            file.open();

            assertNull(file.get(obj.getFullName()));
        }

        file.close();
        file.delete();
    }

    @Test(expected=HDF5SymbolTableException.class)
    public void testDeleteAbsentHObject() throws Exception {
        testFile.delete(new H5ScalarDS(testFile, "thing", "place"));
    }

    @Test(expected=Exception.class)
    public void testDeleteDatasetOnReadOnlyFile() throws Exception {
        Dataset s = null;
        try {
            testFile.close();
            testFile = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.READ);
            assertTrue(testFile.isReadOnly());
            s = (Dataset) testFile.get(H5TestFile.NAME_DATASET_FLOAT);
        }
        catch (Exception e) {
            fail("unable to prepare for read-only delete test " + e);
        }
        assertNotNull(s);
        testFile.delete(s); // throws expected Exception
    }

    @Test
    public void testGet() throws Exception {
        assert( H5TestFile.OBJ_NAMES.length > 0 );
        for (int i = 0; i < H5TestFile.OBJ_NAMES.length; i++) {
            assertNotNull(testFile.get(H5TestFile.OBJ_NAMES[i]));
        }
    }

    @Test
    public void testGetAbsentObject() throws Exception {
        assertNull(testFile.get("/_INVALID_OBJECT_PATH_SHOULD_RETURN_NULL_"));
    }

    @Test
    public void testGetWithoutOpen() throws Exception {
        final String nameNewFile = "someNewFile.h5";
        H5File file = H5TestFile.createTestFile(nameNewFile);

        assert( H5TestFile.OBJ_NAMES.length > 0 ); // at least one object to get
        for (int i = 0; i < H5TestFile.OBJ_NAMES.length; i++) {
                assertNotNull(file.get(H5TestFile.OBJ_NAMES[i]));
        }

        file.close(); // file opened in the process of get?
        file.delete();
    }

    @Test
    public void testGetAbsentObjectWithoutOpen () throws Exception {
        H5File file = H5TestFile.createTestFile("someNewFile.h5");

        assertNull(file.get("/_INVALID_OBJECT_PATH_SHOULD_RETURN_NULL_"));

        file.delete();
    }

    @Test(expected=Exception.class)
    public void testWriteDatasetOnReadOnlyFile() throws Exception {
        Dataset s = null;
        try {
            testFile.close();
            testFile = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.READ);
            assertTrue(testFile.isReadOnly());
            s = (Dataset) testFile.get(H5TestFile.NAME_DATASET_FLOAT);
            s.getData(); // TODO: unclear function name!
            // what exactly is getData() doing here??
            // it is REQUIRED for the test to succeed, but why?
        }
        catch (Exception e) {
            fail("unable to prepare for read-only write test " + e);
        }
        assertNotNull(s);
        s.write(); // throws expected Exception
        // TODO: ^ yuck! the dataset is the thing doing writing in the file??
        // alternatives:
        // file.write(s);
        // s.write(file);
    }

    @Test
    public void testWriteDatasetOnWriteFile() throws Exception {
        Dataset dataset = (Dataset) testFile.get(H5TestFile.NAME_DATASET_FLOAT);
        dataset.getData();
        testFile.close();
        testFile = new H5File(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        testFile.open();

        dataset.write();
    }

    @Test(expected=HDF5Exception.class)
    public void testCannotOpenNonExistentFileReadOnly() throws Exception {
        H5File file = new H5File("nonexistent.h5", FileFormat.READ);
        file.open(); // error expected: "File does not exist"
    }

    @Test(expected=HDF5Exception.class)
    public void testCannotOpenNonExistentFileInWriteMode() throws Exception {
        H5File file = new H5File("nonexistent.h5", FileFormat.WRITE);
        file.open(); // error expected: "File does not exist"
    }

    @Test
    public void testH5FileCREATE() throws Exception {
        H5File file = new H5File("fileToCreate.h5", FileFormat.CREATE);
        file.open();
        assertNotNull(file);
        assertNull(file.get(H5TestFile.NAME_DATASET_FLOAT));
        file.close();
        file.delete();
    }

    @Test
    public void testUpdateReferenceDataset() throws Exception {
        Group root = (Group) testFile.get("/");
        final List<HObject> members = root.getMemberList();
        assert( members.size() > 0 );

        root = (Group) cleanFile.get("/");

        // copy all the objects to the new file
        for (HObject sourceObject : members ) {
            HObject destinationObject = testFile.copy(sourceObject, root);
            String destinationName = destinationObject.getFullName();
            assertNotNull(cleanFile.get(destinationName));
        }

        // TODO: check before updateReferenceDataset()

        H5File.updateReferenceDataset(testFile, cleanFile);

        // TODO: this is a very low-level check for a very high-level op

        byte[] read_data = new byte[3920];

        // Check if the copied dataset containing references,
        // point to correct object type.
        long dataset_id = H5.H5Dopen(
                cleanFile.getFID(),
                cleanFile.get(H5TestFile.NAME_DATASET_OBJ_REF).getName(),
                HDF5Constants.H5P_DEFAULT);
        H5.H5Dread(
                dataset_id,
                HDF5Constants.H5T_STD_REF_OBJ,
                HDF5Constants.H5S_ALL,
                HDF5Constants.H5S_ALL,
                HDF5Constants.H5P_DEFAULT,
                read_data);

        byte rbuf0[] = new byte[8];
        int srcPos = 0;

        //assertEquals(17, H5TestFile.OBJ_TYPES.length - 1);
        for (int i = 0; i < (H5TestFile.OBJ_TYPES.length - 1); i++) {
            int expected_object_type = H5TestFile.OBJ_TYPES[i];
            System.arraycopy(read_data, srcPos, rbuf0, 0, 8);
            srcPos = srcPos + 8;
            int obj_type = H5.H5Rget_obj_type(
                    cleanFile.getFID(), HDF5Constants.H5R_OBJECT, rbuf0);
            assertEquals(expected_object_type, obj_type);
        }
        H5.H5Dclose(dataset_id);
    }

    @Test
    public void testCanGetH5ScalarDS() throws Exception {
        // TODO: I guess?
        // Formerly "testCreateImageAttributes"
        // It isn't clear why this test is here; after peeling away all the
        // cruft, one can actually read what is going on
        H5ScalarDS img = (H5ScalarDS) testFile.get(
                H5TestFile.NAME_DATASET_IMAGE);
        assertTrue(img.hasAttribute());
        assertTrue(img.isImage());
    }

    @Test
    public void testLibBounds() throws Exception {
        int low = HDF5Constants.H5F_LIBVER_LATEST;
        int high = HDF5Constants.H5F_LIBVER_LATEST;

        final H5File file = new H5File(
                H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        file.open();

        int[] expected = {0, 0};
        assertArrayEquals(expected, file.getLibBounds());

        file.setLibBounds(low, high);

        expected[0] = low;
        expected[1] = high;
        assertArrayEquals(expected, file.getLibBounds());

        file.close();
    }

    @Test
    public void testCreateLinkHard() throws Exception {
        Group g1 = cleanFile.createGroup("Group1", null);
        Group g2 = cleanFile.createGroup("Group2", null);

        assertNotNull(
                cleanFile.createLink(
                        g2, "NAME_HARD_LINK", g1, Group.LINK_TYPE_HARD));
        // TODO: test this behavior further?
        // this non-null check is all that was provided in tests as found
    }

    @Test
    public void testCreateLinkSoft() throws Exception {
        Group g1 = cleanFile.createGroup("Group1", null);
        final long[] H5dims = { 4, 6 };
        // create dataset in Group1
        Dataset dataset = cleanFile.createScalarDS(
                "DS1", g1, typeInt, H5dims, null, null, 0, null);

        HObject link = cleanFile.createLink(
                g1, "NAME_SOFT_LINK", dataset, Group.LINK_TYPE_SOFT);
        assertNotNull(link);
        assertEquals(H5File.getLinkTargetName(link), dataset.getFullName());
    }

    @Test
    public void testCreateDanglingLinkToFloatingGroup() throws Exception {
        Group group1 = cleanFile.createGroup("Group1", null);
        Group floatingGroup = new H5Group(null, "DGroup", "/Group1", null);
        HObject link = cleanFile.createLink(
                group1, "SOFT_DANGLE", floatingGroup, Group.LINK_TYPE_SOFT);
        assertNotNull(link);

        //TODO: do something with the floating group?
        //TODO: check link destination/resolving?
        final long group_id = group1.open();
        H5L_info_t link_info = H5.H5Lget_info(
                group_id, "SOFT_DANGLE", HDF5Constants.H5P_DEFAULT);
        assertNotNull(link_info);
        assertEquals(HDF5Constants.H5L_TYPE_SOFT, link_info.type);
        assertTrue(link_info.address_val_size > 0);
    }

    @Test
    public void testCreateDanglingLink() throws Exception {
        Group group1 = cleanFile.createGroup("Group1", null);
        HObject link = cleanFile.createLink(
                group1, "DANGLER", "MISSING", Group.LINK_TYPE_SOFT);
        assertNotNull(link);
        // TODO: make sure that link does not resolve
    }

    @Test
    public void testCreateLinkExternal() throws Exception {
        // TODO: break down by category, as with above?
        // TODO: major clean-up
        Group grp1 = cleanFile.createGroup("Group1", null);
        Dataset datasetFile1Group1 = cleanFile.createScalarDS(
                "DS1", grp1, typeInt, new long[] {4,6}, null, null, 0, null);

        // Create File2
        H5File file2 = (H5File) H5FILE.createFile(
                "TESTExternal.h5", FileFormat.FILE_CREATE_DELETE);
        file2.open();
        Group fgrp1 = file2.createGroup("Group1", null);

        // Create External Links from file2 to dataset in File1.
        HObject obj = file2.createLink(
                fgrp1,
                "NAME_EXTERNAL_LINK",
                datasetFile1Group1,
                Group.LINK_TYPE_EXTERNAL);

        // Check the name of the target object the link points to is correct.
        String d1fullName = datasetFile1Group1.getFile() +
                FileFormat.FILE_OBJ_SEP +
                datasetFile1Group1.getFullName();
        assertEquals(d1fullName, H5File.getLinkTargetName(obj));

        // Create a Dangling Link to object.
        Group grplink = new H5Group(cleanFile, "DGroup", null, null);
        assertNotNull(grplink);
        obj = file2.createLink(
                fgrp1,
                "GROUP_HARD_LINK_DANGLE",
                grplink, Group.LINK_TYPE_EXTERNAL);

        // Create the object to which a dangling link is created
        grplink = cleanFile.createGroup("DGroup", null);

        // Retrieve Link information
        final long gid = fgrp1.open();
        H5L_info_t link_info = H5.H5Lget_info(
                gid,
                "GROUP_HARD_LINK_DANGLE",
                HDF5Constants.H5P_DEFAULT);
        assertNotNull(link_info);
        assertEquals(HDF5Constants.H5L_TYPE_EXTERNAL, link_info.type);
        assertTrue(link_info.address_val_size > 0);

        fgrp1.close(gid);
        file2.close();
        file2.delete();
    }

    @Test
    public void testGetAttributeOrder() throws Exception {
        Group g1 = cleanFile.createGroup("Group1", null);
        final String attributeNames[] = {"intAttr", "strAttr", "floatAttr"};

        Attribute attr1 = new Attribute(
                attributeNames[0],
                new H5Datatype(Datatype.CLASS_INTEGER, 4, -1, -1),
                new long[] { 10 },
                new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        Attribute attr2 = new Attribute(
                attributeNames[1],
                new H5Datatype(Datatype.CLASS_STRING, 20, -1, -1),
                new long[] { 1 },
                new String[] { "String attribute." });
        Attribute attr3 = new Attribute(attributeNames[2],
                new H5Datatype(Datatype.CLASS_FLOAT, 4, -1, -1),
                new long[] { 2 },
                new float[] { 2, 4 });

        g1.writeMetadata(attr1);
        g1.writeMetadata(attr2);
        g1.writeMetadata(attr3);

        final long gid = g1.open();

        // Attributes in order of creation
        List<Attribute> attributeList = H5File.getAttribute(
                gid,
                HDF5Constants.H5_INDEX_CRT_ORDER,
                HDF5Constants.H5_ITER_INC);
        for (int i = 0; i < attributeNames.length; i++) {
            assertEquals(attributeNames[i], attributeList.get(i).toString());
        }

        // Attributes in alphabetical order
        attributeList = H5File.getAttribute(gid);
        assertEquals("floatAttr", attributeList.get(0).toString());
        assertEquals("intAttr",   attributeList.get(1).toString());
        assertEquals("strAttr",   attributeList.get(2).toString());

        g1.close(gid);
    }

    @Test
    public void testCreateDatatype() throws Exception {
        final String datatypeName = "NATIVE_INT";
        assertNull(cleanFile.get(datatypeName));
        Datatype d1 = cleanFile.createDatatype(
                Datatype.CLASS_INTEGER,
                4,
                Datatype.ORDER_LE,
                Datatype.SIGN_NONE,
                datatypeName);
        HObject fetched = cleanFile.get(datatypeName);
        assertNotNull("unable to get the just-created datatype", fetched);
        // TODO: additional checking on the returned datatype's values?
    }

    @Ignore("Attribute Renaming should be deferred to specific HObjects?")
    public void testRenameAttribute() throws Exception {
        Group group = cleanFile.createGroup("G1", null);
        Datatype datatype = cleanFile.createDatatype(
                Datatype.CLASS_INTEGER,
                4,
                Datatype.ORDER_LE,
                Datatype.SIGN_NONE,
                "NATIVE_INT");

        Attribute attribute = new Attribute(
                "strAttr",
                new H5Datatype(Datatype.CLASS_STRING, 20, -1, -1),
                new long[] { 1 },
                new String[] { "String attribute." });

        assertNull(cleanFile.get("GroupAttribute"));
        assertNull(cleanFile.get("DatatypeAttribute"));
        assertNotNull(cleanFile.get("NATIVE_INT"));
        assertNotNull(cleanFile.get("strAttr"));

        group.writeMetadata(attribute);
        datatype.writeMetadata(attribute);
        cleanFile.renameAttribute(group, "strAttr", "GroupAttribute");
        cleanFile.renameAttribute(
                datatype, attribute.getName(), "DatatypeAttribute");
    }

    @Test
    public void testIsSerializable() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream writer = new ObjectOutputStream(out);
            writer.writeObject(testFile);
            writer.close();
        }
        catch (IOException err) {
            err.printStackTrace();
            fail("ObjectOutputStream failed: " + err);
        }
        assertTrue("no bytes were written", out.toByteArray().length > 0);
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
    public void testSerializeToDisk() throws Exception {
        try {

            FileOutputStream fos = new FileOutputStream("temph5file.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(testFile);
            oos.close();
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        testFile.close();
        testFile = null;

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
