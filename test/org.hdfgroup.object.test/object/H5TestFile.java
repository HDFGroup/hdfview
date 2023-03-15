package object;

import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFArray;

import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;

import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5ScalarDS;

/**
 * Creates an HDF5 file for unit tests.
 *
 * @author xcao
 *
 */
public class H5TestFile
{
    private static final Logger log = LoggerFactory.getLogger(H5TestFile.class);
    public static final String NAME_FILE_H5 = "TestHDF5.h5";
    public static final String NAME_GROUP = "/g0";
    public static final String NAME_GROUP_ATTR = "/g0_attr";
    public static final String NAME_GROUP_SUB = NAME_GROUP + "/g00";
    public static final String NAME_DATASET_INT = "/dataset_int";
    public static final String NAME_DATASET_FLOAT = "/dataset_float";
    public static final String NAME_DATASET_CHAR = "/dataset_byte";
    public static final String NAME_DATASET_STR = "/dataset_str";
    public static final String NAME_DATASET_ENUM = "/dataset_enum";
    public static final String NAME_DATASET_IMAGE = "/dataset_image";
    public static final String NAME_DATASET_IMAGE_PALETTE = "/wave_palete";
    public static final String NAME_DATASET_OBJ_REF = "/dataset_obj_ref";
    public static final String NAME_DATASET_COMPOUND = "/dataset_comp";
    public static final String NAME_DATASET_INT_SUB = NAME_GROUP + "/dataset_int";
    public static final String NAME_DATASET_FLOAT_SUB_SUB = NAME_GROUP_SUB + "/dataset_float";
    public static final String NAME_DATASET_COMPOUND_SUB = NAME_GROUP + "/dataset_comp";
    public static final String NAME_DATATYPE_INT = NAME_GROUP + "/datatype_int";
    public static final String NAME_DATATYPE_UINT = NAME_GROUP + "/datatype_uint";
    public static final String NAME_DATATYPE_FLOAT = NAME_GROUP + "/datatype_float";
    public static final String NAME_DATATYPE_STR = NAME_GROUP + "/datatype_str";
    public static final String NAME_HARD_LINK_TO_IMAGE = "a_link_to_the_image";

    public static final String OBJ_NAMES[] = { NAME_GROUP, NAME_GROUP_ATTR, NAME_GROUP_SUB, NAME_DATASET_INT,
            NAME_DATASET_FLOAT, NAME_DATASET_CHAR, NAME_DATASET_STR, NAME_DATASET_ENUM, NAME_DATASET_IMAGE,
            NAME_DATASET_COMPOUND, NAME_DATASET_INT_SUB, NAME_DATASET_FLOAT_SUB_SUB, NAME_DATASET_COMPOUND_SUB,
            NAME_DATATYPE_INT, NAME_DATATYPE_UINT, NAME_DATATYPE_FLOAT, NAME_DATATYPE_STR, NAME_DATASET_OBJ_REF };

    public static final int OBJ_TYPES[] = { HDF5Constants.H5O_TYPE_GROUP, HDF5Constants.H5O_TYPE_GROUP,
            HDF5Constants.H5O_TYPE_GROUP, HDF5Constants.H5O_TYPE_DATASET, HDF5Constants.H5O_TYPE_DATASET,
            HDF5Constants.H5O_TYPE_DATASET, HDF5Constants.H5O_TYPE_DATASET, HDF5Constants.H5O_TYPE_DATASET,
            HDF5Constants.H5O_TYPE_DATASET, HDF5Constants.H5O_TYPE_DATASET, HDF5Constants.H5O_TYPE_DATASET,
            HDF5Constants.H5O_TYPE_DATASET, HDF5Constants.H5O_TYPE_DATASET, HDF5Constants.H5O_TYPE_NAMED_DATATYPE,
            HDF5Constants.H5O_TYPE_NAMED_DATATYPE, HDF5Constants.H5O_TYPE_NAMED_DATATYPE,
            HDF5Constants.H5O_TYPE_NAMED_DATATYPE, HDF5Constants.H5O_TYPE_DATASET };

    // data space information
    public static final int DATATYPE_SIZE = 4;
    public static final int RANK = 2;
    public static final long DIM1 = 50;
    public static final long DIM2 = 10;
    public static final long DIM3 = 20;
    public static final long[] DIMs = { DIM1, DIM2 };
    public static final long[] CHUNKs = { DIM1 / 2, DIM2 / 2 };
    public static final int STR_LEN = 20;
    public static final int DIM_SIZE = (int) (DIM1 * DIM2);;
    public static final long DIMREF = OBJ_NAMES.length - 1;
    public static final long[] DIMREFs = { DIMREF };
    public static final long[] CHUNKREFs = { DIMREF / 2 };
    public static final int DIMREF_SIZE = (int) (DIMREF);

    /* testing data */
    public static final int[] DATA_INT = new int[DIM_SIZE];
    public static final long[] DATA_LONG = new long[DIM_SIZE];
    public static final float[] DATA_FLOAT = new float[DIM_SIZE];
    public static final byte[] DATA_BYTE = new byte[DIM_SIZE];
    public static final String[] DATA_STR = new String[DIM_SIZE];
    public static final int[] DATA_ENUM = new int[DIM_SIZE];
    public static final Vector DATA_COMP = new Vector(3);
    public static final byte[] DATA_PALETTE = createWavePalette();

    // compound names and datatypes
    public static final String[] COMPOUND_MEMBER_NAMES = { "int32", "float32", "string", "uint32" };
    public static final H5Datatype[] COMPOUND_MEMBER_DATATYPES = { null, null, null, null };

    public static H5ScalarAttr ATTRIBUTE_STR = null;
    public static H5ScalarAttr ATTRIBUTE_INT_ARRAY = null;

    /**
     * Creates an HDF5 test file.
     *
     * The test file contains the following objects:
     *
     * <pre>
     * /dataset_byte            Dataset {50, 10}
     * /dataset_comp            Dataset {50, 10}
     * /dataset_enum            Dataset {50, 10}
     * /dataset_float           Dataset {50, 10}
     * /dataset_int             Dataset {50, 10}
     * /dataset_image           Dataset {50, 10}
     * /dataset_str             Dataset {50, 10}
     * /g0                      Group
     * /g0/dataset_int          Dataset {50, 10}
     * /g0/g00                  Group
     * /g0/g00/dataset_float    Dataset {50, 10}
     * /g0_attr                 Group
     * </pre>
     *
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final H5File createTestFile(String fileName) throws Exception {
        log.debug("createTestFile {}", fileName);
        H5File file = null;
        Group g0, g1, g00;
        Dataset[] dsets = new Dataset[11];

        if ((fileName == null) || (fileName.length() < 1))
            fileName = NAME_FILE_H5;

        final H5Datatype typeInt = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeByte = new H5Datatype(Datatype.CLASS_INTEGER, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
        final H5Datatype typeFloat = new H5Datatype(Datatype.CLASS_FLOAT, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeStr = new H5Datatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeChar = new H5Datatype(Datatype.CLASS_CHAR, 1, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeEnum = new H5Datatype(Datatype.CLASS_ENUM, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        typeEnum.setEnumMembers("0=1,1=2");
        log.trace("create reference type");
        final H5Datatype typeRef = new H5Datatype(Datatype.CLASS_REFERENCE, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);

        log.trace("create compound types");
        COMPOUND_MEMBER_DATATYPES[0] = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        COMPOUND_MEMBER_DATATYPES[1] = new H5Datatype(Datatype.CLASS_FLOAT, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        COMPOUND_MEMBER_DATATYPES[2] = new H5Datatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
        COMPOUND_MEMBER_DATATYPES[3] = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.SIGN_NONE);

        for (int i = 0; i < DIM_SIZE; i++) {
            DATA_INT[i] = i;
            DATA_LONG[i] = i;
            DATA_FLOAT[i] = i + i / 100.0f;
            DATA_BYTE[i] = (byte) Math.IEEEremainder(i, 127);
            DATA_STR[i] = "str" + i;
            DATA_ENUM[i] = (int) Math.IEEEremainder(i, 2);
        }

        DATA_COMP.add(0, DATA_INT);
        DATA_COMP.add(1, DATA_FLOAT);
        DATA_COMP.add(2, DATA_STR);
        DATA_COMP.add(3, DATA_LONG);

        log.trace("filename: " + fileName);
        file = new H5File(fileName, FileFormat.CREATE);
        file.open();

        log.trace("create groups");
        g0 = file.createGroup(NAME_GROUP, null);
        g1 = file.createGroup(NAME_GROUP_ATTR, null);
        g00 = file.createGroup(NAME_GROUP_SUB, null);

        log.trace("create attributes");
        // attributes
        ATTRIBUTE_STR = new H5ScalarAttr(g1, "strAttr", typeStr, new long[] { 1 }, new String[] { "String attribute." });
        ATTRIBUTE_INT_ARRAY = new H5ScalarAttr(g1, "arrayInt", typeInt, new long[] { 10 },
                new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });

        ATTRIBUTE_STR.write();
        ATTRIBUTE_INT_ARRAY.write();

        log.trace("create datasets");
        dsets[0] = file.createScalarDS(NAME_DATASET_INT, null, typeInt, DIMs, null, CHUNKs, 9, DATA_INT);
        dsets[1] = file.createScalarDS(NAME_DATASET_FLOAT, null, typeFloat, DIMs, null, CHUNKs, 9, DATA_FLOAT);
        dsets[2] = file.createScalarDS(NAME_DATASET_CHAR, null, typeChar, DIMs, null, CHUNKs, 9, DATA_BYTE);
        dsets[3] = file.createScalarDS(NAME_DATASET_STR, null, typeStr, DIMs, null, CHUNKs, 9, DATA_STR);
        dsets[4] = file.createScalarDS(NAME_DATASET_ENUM, null, typeEnum, DIMs, null, CHUNKs, 9, DATA_ENUM);
        dsets[5] = file.createScalarDS(NAME_DATASET_INT_SUB, g0, typeInt, DIMs, null, CHUNKs, 9, DATA_INT);
        dsets[6] = file.createScalarDS(NAME_DATASET_FLOAT_SUB_SUB, g00, typeFloat, DIMs, null, CHUNKs, 9, DATA_FLOAT);
        dsets[7] = file.createImage(NAME_DATASET_IMAGE, null, typeByte, DIMs, null, CHUNKs, 9, 1, -1, DATA_BYTE);
        log.trace("create compound datasets");
        dsets[8] = file.createCompoundDS(NAME_DATASET_COMPOUND, null, DIMs, null, CHUNKs, 9, COMPOUND_MEMBER_NAMES,
                COMPOUND_MEMBER_DATATYPES, null, DATA_COMP);
        dsets[9] = file.createCompoundDS(NAME_DATASET_COMPOUND_SUB, null, DIMs, null, CHUNKs, 9, COMPOUND_MEMBER_NAMES,
                COMPOUND_MEMBER_DATATYPES, null, DATA_COMP);
        dsets[10] = file.createScalarDS(NAME_DATASET_OBJ_REF, null, typeRef, DIMREFs, null, CHUNKREFs, 9, null);

        // attach attributes to all datasets
        log.trace("attach attributes");
        for (int i = 0; i < dsets.length; i++) {
            ATTRIBUTE_STR = new H5ScalarAttr(dsets[i], "strAttr", typeStr, new long[] { 1 }, new String[] { "String attribute." });
            ATTRIBUTE_INT_ARRAY = new H5ScalarAttr(dsets[i], "arrayInt", typeInt, new long[] { 10 },
                    new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
            ATTRIBUTE_STR.write();
            ATTRIBUTE_INT_ARRAY.write();
        }

        log.trace("create a wave palette and attach it to the image");
        final Dataset pal = file.createScalarDS(NAME_DATASET_IMAGE_PALETTE, null, typeByte, new long[] { 256, 3 },
                null, null, -1, DATA_PALETTE);
        final Vector attrs = (Vector) ((H5ScalarDS)dsets[7]).getMetadata();
        final int n = attrs.size();
        log.trace("wave palette has {} attributes", n);
        for (int i = 0; i < n; i++) {
            H5ScalarAttr attr = (H5ScalarAttr) attrs.get(i);
            log.trace("wave palette attribute[{}] is {}", i, attr.getAttributeName());
            if ("PALETTE".equals(attr.getAttributeName())) {
                log.trace("wave palette data = {}", NAME_DATASET_IMAGE_PALETTE);
                attr.writeAttribute(NAME_DATASET_IMAGE_PALETTE);
            }
        }

        log.trace("create committed");
        Datatype dnative = file.createDatatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        Datatype dtype = file.createNamedDatatype(dnative, NAME_DATATYPE_INT);
        ATTRIBUTE_STR = new H5ScalarAttr(dtype, "strAttr", typeStr, new long[] { 1 }, new String[] { "String attribute." });
        ATTRIBUTE_INT_ARRAY = new H5ScalarAttr(dtype, "arrayInt", typeInt, new long[] { 10 },
                new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        ATTRIBUTE_STR.write();
        ATTRIBUTE_INT_ARRAY.write();

        dnative = file.createDatatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.SIGN_NONE);
        dtype = file.createNamedDatatype(dnative, NAME_DATATYPE_UINT);
        ATTRIBUTE_STR = new H5ScalarAttr(dtype, "strAttr", typeStr, new long[] { 1 }, new String[] { "String attribute." });
        ATTRIBUTE_INT_ARRAY = new H5ScalarAttr(dtype, "arrayInt", typeInt, new long[] { 10 },
                new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        ATTRIBUTE_STR.write();
        ATTRIBUTE_INT_ARRAY.write();

        dnative = file.createDatatype(Datatype.CLASS_FLOAT, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        dtype = file.createNamedDatatype(dnative, NAME_DATATYPE_FLOAT);
        ATTRIBUTE_STR = new H5ScalarAttr(dtype, "strAttr", typeStr, new long[] { 1 }, new String[] { "String attribute." });
        ATTRIBUTE_INT_ARRAY = new H5ScalarAttr(dtype, "arrayInt", typeInt, new long[] { 10 },
                new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        ATTRIBUTE_STR.write();
        ATTRIBUTE_INT_ARRAY.write();

        dnative = file.createDatatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
        dtype = file.createNamedDatatype(dnative, NAME_DATATYPE_STR);
        ATTRIBUTE_STR = new H5ScalarAttr(dtype, "strAttr", typeStr, new long[] { 1 }, new String[] { "String attribute." });
        ATTRIBUTE_INT_ARRAY = new H5ScalarAttr(dtype, "arrayInt", typeInt, new long[] { 10 },
                new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        ATTRIBUTE_STR.write();
        ATTRIBUTE_INT_ARRAY.write();

        file.createLink(g0, NAME_HARD_LINK_TO_IMAGE, dsets[7]);

        log.trace("create file close");
        try {
            file.close();
        }
        catch (Exception ex) {}

        log.trace("create file open to write refs");
        file.setNewLibBounds("Latest", "Latest");
        file.setLibBounds("Latest", "Latest");
        file.open();
        log.trace("create file opened");
        byte[][] brefs = new byte[DIMREF_SIZE][HDF5Constants.H5R_REF_BUF_SIZE];
        //  (int i = 0; i < OBJ_NAMES.length; i++) { --//This gives CORE DUMP when OBJ_NAMES = NAME_DATASET_OBJ_REF,
        // as it enters an infinite loop.
        try {
            for (int i = 0; i < DIMREF_SIZE; i++) {
                log.trace("get object[{}]={}", i, OBJ_NAMES[i]);
                try {
                    brefs[i] = H5.H5Rcreate_object(file.getFID(), OBJ_NAMES[i], HDF5Constants.H5P_DEFAULT);
                }
                catch (Throwable err) {
                    err.printStackTrace();
                    log.trace("H5Rcreate_object OBJ_NAMES[{}]",OBJ_NAMES[i]);
                }
                log.trace("create brefs[{}]={}", i, brefs[i]);
                String objName = H5.H5Rget_obj_name(brefs[i], HDF5Constants.H5P_DEFAULT);
                int ref_type = H5.H5Rget_type(brefs[i]);
                int obj_type = H5.H5Rget_obj_type3(brefs[i], HDF5Constants.H5P_DEFAULT);
                log.trace("create refs[{}] name={} reftype={} objtype={}", i, objName, ref_type, obj_type);
            }
            log.trace("write object refs to the ref dataset");
            dsets[10].write(brefs);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                for (int i = 0; i < DIMREF_SIZE; i++)
                    H5.H5Rdestroy(brefs[i]);
            }
            catch (Exception ex) {}
        }

        log.trace("create file close after refs");
        try {
            file.close();
        }
        catch (Exception ex) {}

        log.debug("create file finished");
        return file;
    }

    /**
     * Creates the wave palette of the indexed 256-color table.
     *
     * The palette values are stored in a two-dimensional byte array and arrange by color components of red, green and
     * blue. palette[][] = byte[3][256], where, palette[0][], palette[1][] and palette[2][] are the red, green and blue
     * components respectively.
     *
     * @return the wave palette in the form of byte[3][256]
     */
    private static final byte[] createWavePalette() {
        byte[] p = new byte[768]; // 256*3

        for (int i = 1; i < 255; i++) {
            p[3 * i] = (byte) ((Math.sin(((double) i / 40 - 3.2)) + 1) * 128);
            p[3 * i + 1] = (byte) ((1 - Math.sin((i / 2.55 - 3.1))) * 70 + 30);
            p[3 * i + 2] = (byte) ((1 - Math.sin(((double) i / 40 - 3.1))) * 128);
        }

        p[0] = p[1] = p[2] = 0;
        p[765] = p[766] = p[767] = (byte) 255;

        return p;
    }

}
