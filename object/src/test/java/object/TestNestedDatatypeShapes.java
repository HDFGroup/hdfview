package object;

import static org.junit.jupiter.api.Assertions.*;

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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Read tests for nested datatype shapes.
 *
 * Fixtures are built at test time, written with raw H5Dwrite/H5DwriteVL calls
 * against the JNI's buffer data model.
 */
@Tag("unit")
@DisplayName("Nested Datatype Shape Read Tests")
public class TestNestedDatatypeShapes {

    private static Path workDir;

    private static final int WIDE_POINTS   = 50;
    private static final int WIDE_ELEMENTS = 230;

    private static H5File testFile;
    private static int openIDsAtStart;

    @BeforeAll
    static void createFile() throws Exception
    {
        openIDsAtStart = H5.getOpenIDCount();

        workDir = Path.of("target", "nested-datatype-shapes");
        Files.createDirectories(workDir);

        String path = workDir.resolve("nested_shapes.h5").toString();
        long fid    = H5.H5Fcreate(path, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT,
                                   HDF5Constants.H5P_DEFAULT);
        try {
            writeVlenOfCompound(fid);
            writeVlenOfVarStr(fid);
            writeArrayOfVlenInt(fid);
            writeArrayOfArrayVarStr(fid);
            writeArrayOfArrayInt(fid);
            writeArrayOfCompoundVarStr(fid);
            writeCompoundOfVlenCompound(fid);
            writeVlenOfFixedString(fid);
            writeArrayOfFixedString(fid);
            writeWideVlenOfCompound(fid);
            writeCompoundWithReference(fid);
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

        assertEquals(openIDsAtStart, H5.getOpenIDCount(), "HDF5 identifiers leaked by this test class");
    }

    /** Variable-length string type, which the caller closes. */
    private static long varStrType() throws Exception
    {
        long tid = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
        H5.H5Tset_size(tid, HDF5Constants.H5T_VARIABLE);
        return tid;
    }

    /** Compound {p:int, q:int}, which the caller closes. */
    private static long pqCompoundType() throws Exception
    {
        long tid = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8);
        H5.H5Tinsert(tid, "p", 0, HDF5Constants.H5T_NATIVE_INT);
        H5.H5Tinsert(tid, "q", 4, HDF5Constants.H5T_NATIVE_INT);
        return tid;
    }

    private static void writeDataset(long fid, String name, long tid, int nPoints, Object buf, boolean vlen)
        throws Exception
    {
        long sid = H5.H5Screate_simple(1, new long[] {nPoints}, null);
        long did = H5.H5Dcreate(fid, name, tid, sid, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT,
                                HDF5Constants.H5P_DEFAULT);
        try {
            if (vlen)
                H5.H5DwriteVL(did, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                              HDF5Constants.H5P_DEFAULT, (Object[])buf);
            else
                H5.H5Dwrite(did, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT,
                            buf);
        }
        finally {
            H5.H5Dclose(did);
            H5.H5Sclose(sid);
        }
    }

    private static ArrayList<Object> list(Object... items)
    {
        ArrayList<Object> out = new ArrayList<>();
        for (Object item : items)
            out.add(item);
        return out;
    }

    // ---- fixtures -------------------------------------------------------------

    /** VLEN of COMPOUND{p,q}. */
    private static void writeVlenOfCompound(long fid) throws Exception
    {
        long inner = pqCompoundType();
        long tid   = H5.H5Tvlen_create(inner);
        try {
            Object[] buf = {list(list(10, 11), list(20, 21)), list(list(30, 31))};
            writeDataset(fid, "vlen_of_compound", tid, 2, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(inner);
        }
    }

    /** VLEN of variable-length string. */
    private static void writeVlenOfVarStr(long fid) throws Exception
    {
        long vs  = varStrType();
        long tid = H5.H5Tvlen_create(vs);
        try {
            Object[] buf = {list("a0", "a1"), list("b0")};
            writeDataset(fid, "vlen_of_varstr", tid, 2, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(vs);
        }
    }

    /** ARRAY[2] of VLEN of int. */
    private static void writeArrayOfVlenInt(long fid) throws Exception
    {
        long vt  = H5.H5Tvlen_create(HDF5Constants.H5T_NATIVE_INT);
        long tid = H5.H5Tarray_create(vt, 1, new long[] {2});
        try {
            Object[] buf = {list(list(1, 2), list(3)), list(list(4), list(5, 6))};
            writeDataset(fid, "array_of_vlen_int", tid, 2, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(vt);
        }
    }

    /** ARRAY[2] of ARRAY[3] of variable-length string. */
    private static void writeArrayOfArrayVarStr(long fid) throws Exception
    {
        long vs    = varStrType();
        long inner = H5.H5Tarray_create(vs, 1, new long[] {3});
        long tid   = H5.H5Tarray_create(inner, 1, new long[] {2});
        try {
            Object[] buf = {list(list("n000", "n001", "n002"), list("n010", "n011", "n012")),
                            list(list("n100", "n101", "n102"), list("n110", "n111", "n112"))};
            writeDataset(fid, "array_of_array_varstr", tid, 2, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(inner);
            H5.H5Tclose(vs);
        }
    }

    /** ARRAY[2] of ARRAY[3] of int. */
    private static void writeArrayOfArrayInt(long fid) throws Exception
    {
        long inner = H5.H5Tarray_create(HDF5Constants.H5T_NATIVE_INT, 1, new long[] {3});
        long tid   = H5.H5Tarray_create(inner, 1, new long[] {2});
        try {
            int[] buf = {0, 1, 2, 10, 11, 12, 100, 101, 102, 110, 111, 112};
            writeDataset(fid, "array_of_array_int", tid, 2, buf, false);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(inner);
        }
    }

    /** ARRAY[2] of COMPOUND{n:int, s:varstr}. */
    private static void writeArrayOfCompoundVarStr(long fid) throws Exception
    {
        long vs    = varStrType();
        long strSz = H5.H5Tget_size(vs);
        long cmpd  = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8 + strSz);
        H5.H5Tinsert(cmpd, "n", 0, HDF5Constants.H5T_NATIVE_INT);
        H5.H5Tinsert(cmpd, "s", 8, vs);
        long tid = H5.H5Tarray_create(cmpd, 1, new long[] {2});
        try {
            Object[] buf = {list(list(0, "x0"), list(1, "x1")), list(list(2, "y0"), list(3, "y1"))};
            writeDataset(fid, "array_of_compound_varstr", tid, 2, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(cmpd);
            H5.H5Tclose(vs);
        }
    }

    /** COMPOUND{id:int, nested:VLEN of COMPOUND{p,q}}. */
    private static void writeCompoundOfVlenCompound(long fid) throws Exception
    {
        long inner  = pqCompoundType();
        long vt     = H5.H5Tvlen_create(inner);
        long vtSize = H5.H5Tget_size(vt);
        long tid    = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8 + vtSize);
        H5.H5Tinsert(tid, "id", 0, HDF5Constants.H5T_NATIVE_INT);
        H5.H5Tinsert(tid, "nested", 8, vt);
        try {
            Object[] buf = {list(7, list(list(1, 2))), list(8, list(list(3, 4), list(5, 6)))};
            writeDataset(fid, "compound_of_vlen_compound", tid, 2, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(vt);
            H5.H5Tclose(inner);
        }
    }

    /** VLEN of a one-character fixed-length string, as a string stored as a sequence. */
    private static void writeVlenOfFixedString(long fid) throws Exception
    {
        long st  = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
        long tid = H5.H5Tvlen_create(st);
        try {
            Object[] buf = {list("a", "b", "c", "d"), list("x", "y")};
            writeDataset(fid, "vlen_of_fixed_string", tid, 2, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(st);
        }
    }

    /** ARRAY[3] of fixed-length string, so no variable-length data anywhere. */
    private static void writeArrayOfFixedString(long fid) throws Exception
    {
        long st = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
        H5.H5Tset_size(st, 8);
        long tid = H5.H5Tarray_create(st, 1, new long[] {3});
        try {
            byte[] buf      = new byte[2 * 3 * 8];
            String[] values = {"alpha", "beta", "gamma", "delta", "epsilon", "zeta"};
            for (int i = 0; i < values.length; i++)
                System.arraycopy(values[i].getBytes("US-ASCII"), 0, buf, i * 8, values[i].length());
            writeDataset(fid, "array_of_fixed_string", tid, 2, buf, false);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(st);
        }
    }

    /** VLEN of COMPOUND wide enough to catch anything that scales with element count. */
    private static void writeWideVlenOfCompound(long fid) throws Exception
    {
        long inner = pqCompoundType();
        long tid   = H5.H5Tvlen_create(inner);
        try {
            Object[] buf = new Object[WIDE_POINTS];
            for (int i = 0; i < WIDE_POINTS; i++) {
                ArrayList<Object> seq = new ArrayList<>();
                for (int j = 0; j < WIDE_ELEMENTS; j++)
                    seq.add(list(i, j));
                buf[i] = seq;
            }
            writeDataset(fid, "wide_vlen_of_compound", tid, WIDE_POINTS, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(inner);
        }
    }

    /** COMPOUND mixing a VLEN of compound, a variable-length string and an object reference. */
    private static void writeCompoundWithReference(long fid) throws Exception
    {
        long gid = H5.H5Gcreate(fid, "referenced_group", HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT,
                                HDF5Constants.H5P_DEFAULT);
        H5.H5Gclose(gid);
        byte[] ref = H5.H5Rcreate_object(fid, "referenced_group", HDF5Constants.H5P_DEFAULT);

        long inner  = pqCompoundType();
        long vt     = H5.H5Tvlen_create(inner);
        long vs     = varStrType();
        long rt     = H5.H5Tcopy(HDF5Constants.H5T_STD_REF_OBJ);
        long vtSize = H5.H5Tget_size(vt);
        long vsSize = H5.H5Tget_size(vs);
        long rtSize = H5.H5Tget_size(rt);
        long tid    = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, vtSize + vsSize + rtSize);
        H5.H5Tinsert(tid, "seq", 0, vt);
        H5.H5Tinsert(tid, "label", vtSize, vs);
        H5.H5Tinsert(tid, "target", vtSize + vsSize, rt);
        try {
            byte[] shortRef = new byte[(int)rtSize];
            System.arraycopy(ref, 0, shortRef, 0, Math.min(ref.length, shortRef.length));
            Object[] buf = {list(list(list(1, 2), list(3, 4)), "first", shortRef),
                            list(list(list(5, 6)), "second", shortRef)};
            writeDataset(fid, "compound_with_reference", tid, 2, buf, true);
        }
        finally {
            H5.H5Tclose(tid);
            H5.H5Tclose(rt);
            H5.H5Tclose(vs);
            H5.H5Tclose(vt);
            H5.H5Tclose(inner);
        }
    }

    // ---- helpers --------------------------------------------------------------

    private static Dataset open(String name) throws Exception
    {
        Dataset dataset = (Dataset)testFile.get("/" + name);
        assertNotNull(dataset, "Dataset not found: " + name);
        dataset.init();
        return dataset;
    }

    /** Renders a read buffer to a stable string. */
    private static String render(Object data)
    {
        if (data == null)
            return "null";
        if (data instanceof String str)
            return str;
        if (data instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(render(list.get(i)));
            }
            return sb.append("]").toString();
        }
        if (data.getClass().isArray()) {
            int n            = java.lang.reflect.Array.getLength(data);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < n; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(render(java.lang.reflect.Array.get(data, i)));
            }
            return sb.append("]").toString();
        }
        return String.valueOf(data);
    }

    // ---- tests ----------------------------------------------------------------

    @Test
    @DisplayName("VLEN of compound")
    public void testVlenOfCompound() throws Exception
    {
        assertEquals("[[[10, 11], [20, 21]], [[30, 31]]]", render(open("vlen_of_compound").getData()));
    }

    @Test
    @DisplayName("VLEN of variable-length string")
    public void testVlenOfVarStr() throws Exception
    {
        assertEquals("[[a0, a1], [b0]]", render(open("vlen_of_varstr").getData()));
    }

    @Test
    @DisplayName("Array of VLEN of int")
    public void testArrayOfVlenInt() throws Exception
    {
        assertEquals("[[[1, 2], [3]], [[4], [5, 6]]]", render(open("array_of_vlen_int").getData()));
    }

    @Test
    @DisplayName("Array of array of variable-length string")
    public void testArrayOfArrayVarStr() throws Exception
    {
        assertEquals("[[[n000, n001, n002], [n010, n011, n012]], "
                         + "[[n100, n101, n102], [n110, n111, n112]]]",
                     render(open("array_of_array_varstr").getData()));
    }

    @Test
    @DisplayName("Array of array of int")
    public void testArrayOfArrayInt() throws Exception
    {
        Object data = open("array_of_array_int").getData();
        assertInstanceOf(int[].class, data, "No variable-length data, so the layout is flat");
        assertEquals("[0, 1, 2, 10, 11, 12, 100, 101, 102, 110, 111, 112]", render(data));
    }

    @Test
    @DisplayName("Array of compound with a variable-length string member")
    public void testArrayOfCompoundVarStr() throws Exception
    {
        // An array of compound is presented per member, not per element.
        assertEquals("[[0, 1, 2, 3], [x0, x1, y0, y1]]", render(open("array_of_compound_varstr").getData()));
    }

    @Test
    @DisplayName("Compound with a VLEN of compound member")
    public void testCompoundOfVlenCompound() throws Exception
    {
        String rendered = render(open("compound_of_vlen_compound").getData());
        assertTrue(rendered.contains("[1, 2]"), "Expected first record's nested element: " + rendered);
        assertTrue(rendered.contains("[3, 4]"), "Expected second record's first element: " + rendered);
        assertTrue(rendered.contains("[5, 6]"), "Expected second record's second element: " + rendered);
    }

    @Test
    @Disabled // depends on the JNI reading fixed-length strings past their element
    @DisplayName("VLEN of fixed-length string")
    public void testVlenOfFixedString() throws Exception
    {
        assertEquals("[[a, b, c, d], [x, y]]", render(open("vlen_of_fixed_string").getData()));
    }

    @Test
    @DisplayName("Array of fixed-length string")
    public void testArrayOfFixedString() throws Exception
    {
        Object data = open("array_of_fixed_string").getData();

        /*
         * A fixed-length string is not variable-length data, so this takes the flat path
         * and comes back as the raw bytes of every element rather than one list per
         * point. Asserted because it is the boundary the variable-length detection has
         * to get right: a string array that must not be treated as an object buffer.
         */
        assertInstanceOf(byte[].class, data, "Expected a flat byte[]");
        assertEquals(2 * 3 * 8, ((byte[])data).length, "2 points x 3 elements x 8 bytes");

        String text = new String((byte[])data, "US-ASCII");
        for (String value : new String[] {"alpha", "beta", "gamma", "delta", "epsilon", "zeta"})
            assertTrue(text.contains(value), "Expected element " + value);
    }

    @Test
    @DisplayName("VLEN of compound at width")
    public void testWideVlenOfCompound() throws Exception
    {
        Object data = open("wide_vlen_of_compound").getData();

        assertInstanceOf(Object[].class, data);
        Object[] points = (Object[])data;
        assertEquals(WIDE_POINTS, points.length, "point count");

        for (int i = 0; i < points.length; i++) {
            List<?> seq = (List<?>)points[i];
            assertEquals(WIDE_ELEMENTS, seq.size(), "element count at point " + i);
            assertEquals(List.of(i, WIDE_ELEMENTS - 1), seq.get(WIDE_ELEMENTS - 1),
                         "last element of point " + i);
        }
    }

    @Test
    @DisplayName("Compound holding a VLEN, a variable-length string and a reference")
    public void testCompoundWithReference() throws Exception
    {
        Object data = open("compound_with_reference").getData();
        assertNotNull(data);

        List<?> members = (List<?>)data;
        assertEquals(3, members.size(), "Expected members seq, label and target");

        assertEquals("[[[1, 2], [3, 4]], [[5, 6]]]", render(members.get(0)));
        assertEquals("[first, second]", render(members.get(1)));
        assertNotNull(members.get(2), "reference member should be read");
    }

    @Test
    @DisplayName("Writing a VLEN of compound is refused")
    public void testVlenOfCompoundWriteRefused() throws Exception
    {
        /*
         * Built and closed here rather than copied from the shared fixture, which stays
         * open for the whole class: Windows will not copy a file another handle holds.
         */
        Path target = workDir.resolve("refused.h5");
        long fid    = H5.H5Fcreate(target.toString(), HDF5Constants.H5F_ACC_TRUNC,
                                   HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        try {
            writeVlenOfCompound(fid);
        }
        finally {
            H5.H5Fclose(fid);
        }

        H5File rw = (H5File)(new H5File()).createInstance(target.toString(), FileFormat.WRITE);
        rw.open();
        try {
            Dataset dataset = (Dataset)rw.get("/vlen_of_compound");
            dataset.init();
            Object data = dataset.getData();

            /*
             * There is no way to map an edited cell of this shape back to storage, so the
             * write must fail rather than report success having stored nothing.
             */
            assertThrows(Exception.class,
                         () -> dataset.write(data), "Writing a VLEN of compound should be refused");
        }
        finally {
            rw.close();
        }

        // The refusal must also leave the data as it was.
        H5File check = (H5File)(new H5File()).createInstance(target.toString(), FileFormat.READ);
        check.open();
        try {
            Dataset dataset = (Dataset)check.get("/vlen_of_compound");
            dataset.init();
            assertEquals("[[[10, 11], [20, 21]], [[30, 31]]]", render(dataset.getData()));
        }
        finally {
            check.close();
        }
    }
}
