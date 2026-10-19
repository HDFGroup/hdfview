package object;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hdf.object.Dataset;
import hdf.object.FileFormat;
import hdf.object.h5.H5File;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Since 1.14, a compound holding a variable-length string member whose stored value is NULL
 * comes back from the HDF5 library one value short, with every member after the NULL sitting
 * a place too early. Those records should be marked rather than displayed against the wrong
 * members.
 *
 * Once the underlying issue is resolved in the JNI these tests should assert data
 * correctness again.
 *
 * Fixtures are built at test time with raw H5DwriteVL calls, since writing a NULL member is
 * the whole point and the object layer has no way to ask for one.
 *
 * See HDFGroup/hdfview issue 503.
 */
@Tag("unit")
@DisplayName("Compound With A NULL Variable-Length String Member")
public class TestCompoundNullVarStrMember {

    private static H5File testFile;
    private static int openIDsAtStart;

    @BeforeAll
    static void createFile() throws Exception
    {
        openIDsAtStart = H5.getOpenIDCount();

        Path workDir = Path.of("target", "compound-null-varstr-member");
        Files.createDirectories(workDir);

        String path = workDir.resolve("compound_null_varstr_member.h5").toString();
        long fid    = H5.H5Fcreate(path, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT,
                                   HDF5Constants.H5P_DEFAULT);
        try {
            /* Second element of the first sequence has a NULL label. */
            writeVlenOfCompound(fid, "with_null_label", null);
            /* Same shape with every label present, to show the check leaves good data alone. */
            writeVlenOfCompound(fid, "without_null_label", "two");
            /* Middle element of the array is NULL, which the library drops the same way. */
            writeArrayOfVarStr(fid, "array_with_null");
        }
        finally {
            H5.H5Fclose(fid);
        }

        testFile = (H5File)(new H5File()).createInstance(path, FileFormat.READ);
        testFile.open();
    }

    @AfterAll
    static void closeFile() throws Exception
    {
        if (testFile != null)
            testFile.close();

        assertEquals(openIDsAtStart, H5.getOpenIDCount(), "Leaked HDF5 ids");
    }

    /*
     * VLEN of COMPOUND{varstr label, int count}. The first sequence holds two records and
     * the second label is whatever secondLabel says, so passing null writes a NULL member.
     */
    private static void writeVlenOfCompound(long fid, String name, String secondLabel) throws Exception
    {
        long vstr = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
        H5.H5Tset_size(vstr, HDF5Constants.H5T_VARIABLE);

        long cmpd = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 16);
        H5.H5Tinsert(cmpd, "label", 0, vstr);
        H5.H5Tinsert(cmpd, "count", 8, HDF5Constants.H5T_NATIVE_INT);

        long vlen = H5.H5Tvlen_create(cmpd);
        long sid  = H5.H5Screate_simple(1, new long[] {2}, null);
        long did  = H5.H5Dcreate(fid, name, vlen, sid, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT,
                                 HDF5Constants.H5P_DEFAULT);
        try {
            ArrayList<Object> first  = record("one", 11);
            ArrayList<Object> second = record(secondLabel, 22);
            ArrayList<Object> third  = record("three", 33);

            ArrayList<Object> row0 = new ArrayList<>();
            row0.add(first);
            row0.add(second);
            ArrayList<Object> row1 = new ArrayList<>();
            row1.add(third);

            H5.H5DwriteVL(did, vlen, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT,
                          new Object[] {row0, row1});
        }
        finally {
            H5.H5Dclose(did);
            H5.H5Sclose(sid);
            H5.H5Tclose(vlen);
            H5.H5Tclose(cmpd);
            H5.H5Tclose(vstr);
        }
    }

    /* ARRAY[3] of variable-length string whose middle element is written as NULL. */
    private static void writeArrayOfVarStr(long fid, String name) throws Exception
    {
        long vstr = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
        H5.H5Tset_size(vstr, HDF5Constants.H5T_VARIABLE);

        long arr = H5.H5Tarray_create(vstr, 1, new long[] {3});
        long sid = H5.H5Screate_simple(1, new long[] {1}, null);
        long did = H5.H5Dcreate(fid, name, arr, sid, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT,
                                HDF5Constants.H5P_DEFAULT);
        try {
            ArrayList<Object> elements = new ArrayList<>();
            elements.add("a");
            elements.add(null);
            elements.add("c");

            H5.H5DwriteVL(did, arr, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT,
                          new Object[] {elements});
        }
        finally {
            H5.H5Dclose(did);
            H5.H5Sclose(sid);
            H5.H5Tclose(arr);
            H5.H5Tclose(vstr);
        }
    }

    private static ArrayList<Object> record(String label, int count)
    {
        ArrayList<Object> rec = new ArrayList<>();
        rec.add(label);
        rec.add(count);
        return rec;
    }

    private static Dataset open(String name) throws Exception
    {
        Dataset dataset = (Dataset)testFile.get("/" + name);
        assertNotNull(dataset, "Dataset not found: " + name);
        dataset.init();
        return dataset;
    }

    @Test
    @DisplayName("Compound with a NULL varstr member - HDFView#503 detection")
    public void testShortRecordIsMarked() throws Exception
    {
        Object data = open("with_null_label").getData();

        List<?> records = sequenceAt(data, 0);
        assertEquals(2, records.size(), "Expected both records of the first sequence");

        /* The record whose label was written survives untouched. */
        List<?> whole = (List<?>)records.get(0);
        assertEquals(2, whole.size(), "A whole record keeps both members");
        assertEquals("one", whole.get(0));
        assertEquals(Integer.valueOf(11), whole.get(1));

        /*
         * The record whose label was NULL came back one value short, so every value of it is
         * marked. Filling it back out to two also puts the member count right.
         */
        List<?> marked = (List<?>)records.get(1);
        assertEquals(2, marked.size(), "A marked record is filled back out to its member count");
        assertEquals("*ERR:HDFView#503*", marked.get(0), "The dropped member's place is marked");
        assertEquals("*ERR:HDFView#503*", marked.get(1),
                     "The member that would have shifted into its place is marked too");

        /* The rest of the dataset still reads. */
        List<?> second = sequenceAt(data, 1);
        assertEquals(1, second.size(), "Expected the second sequence's one record");
        assertEquals("three", ((List<?>)second.get(0)).get(0), "An unaffected point is untouched");
    }

    @Test
    @DisplayName("The same shape with no NULL member is left alone")
    public void testWholeRecordsAreLeftAlone() throws Exception
    {
        Object data = open("without_null_label").getData();

        List<?> records = sequenceAt(data, 0);
        assertEquals(2, records.size(), "Expected both records of the first sequence");

        for (int i = 0; i < records.size(); i++) {
            List<?> record = (List<?>)records.get(i);
            assertEquals(2, record.size(), "Record " + i + " should keep both members");
            assertNotEquals("*ERR:HDFView#503*", record.get(0), "Record " + i + " should not be marked");
        }

        assertEquals("one", ((List<?>)records.get(0)).get(0));
        assertEquals("two", ((List<?>)records.get(1)).get(0));
    }

    @Test
    @DisplayName("Array with a NULL element - HDFView#503 detection")
    public void testShortArrayIsMarked() throws Exception
    {
        List<?> elements = sequenceAt(open("array_with_null").getData(), 0);

        assertEquals(3, elements.size(), "A marked array is filled back out to its element count");
        for (int i = 0; i < elements.size(); i++)
            assertEquals("*ERR:HDFView#503*", elements.get(i), "Element " + i + " should be marked");
    }

    /* One point of a VLEN dataset, which reads back as a list of records. */
    private static List<?> sequenceAt(Object data, int point)
    {
        assertNotNull(data, "Data read returned null");
        assertInstanceOf(Object[].class, data, "Expected one entry per selected point");
        Object sequence = ((Object[])data)[point];
        assertInstanceOf(List.class, sequence, "Expected point " + point + " to be a sequence");
        return (List<?>)sequence;
    }
}
