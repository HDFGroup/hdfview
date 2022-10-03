package misc;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.ScalarDS;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5ScalarDS;

public class TestH5MemoryLeak
{
    /** Name of test file */
    private static final String NAME_FILE_H5="TestH5MemoryLeak.h5";

    private static final boolean DEBUG = true;
    private static final int NLOOPS = 10000;
    private static final int NPRINT = 100;
    private static final int NSTART = 2000;

    /** Name of test groups */
    private static final String NAME_GROUP = "/g0";
    private static final String NAME_GROUP_ATTR = "/g0_attr";
    private static final String NAME_GROUP_SUB = NAME_GROUP+"/g00";

    /** Name of test datasets */
    private static final String DNAMES[] = {
            "/dataset_byte", "/dataset_int",
            "/dataset_float", "/dataset_str",
            "/dataset_enum", "/dataset_image",
            "/dataset_comp", NAME_GROUP + "/dataset_int",
            NAME_GROUP_SUB+ "/dataset_float", NAME_GROUP + "/dataset_comp", "/dataset_str_vlen"};
    private static final String NAME_DATASET_CHAR           = DNAMES[0];
    private static final String NAME_DATASET_INT            = DNAMES[1];
    private static final String NAME_DATASET_FLOAT          = DNAMES[2];
    private static final String NAME_DATASET_STR            = DNAMES[3];
    private static final String NAME_DATASET_ENUM = DNAMES[4];
    private static final String NAME_DATASET_IMAGE          = DNAMES[5];
    private static final String NAME_DATASET_COMPOUND       = DNAMES[6];
    private static final String NAME_DATASET_SUB            = DNAMES[7];
    private static final String NAME_DATASET_SUB_SUB        = DNAMES[8];
    private static final String NAME_DATASET_COMPOUND_SUB   = DNAMES[9];
    private static final String NAME_DATASET_STR_VLEN       = DNAMES[10];

    /** Name of test dataype */
    private static final String NAME_DATATYPE_INT   = NAME_GROUP + "/datatype_int";
    private static final String NAME_DATATYPE_FLOAT = NAME_GROUP + "/datatype_float";
    private static final String NAME_DATATYPE_STR   = NAME_GROUP + "/datatype_str";

    // data space information
    private static final int DATATYPE_SIZE  = 4;
    private static final int RANK           = 2;
    private static final long DIM1          = 10;
    private static final long DIM2          = 5;
    private static final long[] DIMs        = {DIM1, DIM2};
    private static final long[] CHUNKs      = {DIM1/2, DIM2/2};
    private static final int STR_LEN        = 20;
    private static final int DIM_SIZE       = (int)(DIM1*DIM2);;

    /* testing data */
    private static final int[] DATA_INT     = new int[DIM_SIZE];
    private static final long[] DATA_LONG   = new long[DIM_SIZE];
    private static final float[] DATA_FLOAT = new float[DIM_SIZE];
    private static final byte[] DATA_BYTE   = new byte[DIM_SIZE];
    private static final String[] DATA_STR  = new String[DIM_SIZE];
    private static final int[] DATA_ENUM    = new int[DIM_SIZE];
    private static final Vector DATA_COMP   = new Vector(3);

    // compound names and datatypes
    private static final String[] COMPOUND_MEMBER_NAMES = { "int32", "float32", "string", "uint32", "vlstring" };
    private static final H5Datatype[] COMPOUND_MEMBER_DATATYPES = { null, null, null, null, null };

    /**
     * Test memory leak by create file, open dataset, read/write data in an infinite loop
     *
     * @param args
     */
    public static void main(final String[] args) {
        boolean is_userfile = false;
        long retValue = 0;

        if (args.length > 0) {
            File tmpFile = new File(args[0]);
            is_userfile = (tmpFile.exists() && tmpFile.isFile());
        }

        try {
            H5.H5Eclear();
        }
        catch (Exception ex) {}
        System.out.flush();

        System.out.println("\nCheck memory leak (may take 5 to 10 mintues) ...");
        try {
            if (is_userfile)
                retValue = test_user_file(args[0]);
            else
                retValue = test_default_file();
        }
        catch (Exception err) {
            retValue=1;
        }

        if (retValue <= 0)
            System.out.println("PASSED:\tcheck memory leak.\n");
        else
            System.out.println("FAILED***:\tcheck memory leak.\n");
    }

    private static final long test_user_file(String fname) throws Exception {
        H5File testFile = null;
        MemoryUsage memuse = null;
        DecimalFormat df = new DecimalFormat("000.00#E0#");

        int count = 0;
        String sumStr="-----";
        long KB = 1024, mem0=0, mem1=0, sum=0;

        if (DEBUG) {
            System.out.flush();
            System.out.println("\n\nNo. of loops\tIncrease\tUsed(KB)\tTotal(KB)\tNo. of open IDs\n"+
                    "_______________________________________________________________________________\n");
        }

        while(count<NLOOPS) {
            count ++;
            if (count % NPRINT == 0) {
                memuse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                mem1 = memuse.getCommitted()/KB;
                if (count>NSTART) {
                    sum += (mem1-mem0);
                    sumStr = df.format(sum);
                }

                if (DEBUG) {
                    System.out.println(
                            df.format(count) + "   \t" +
                            sumStr + "    \t" +
                            df.format((mem1)) + "    \t" +
                            df.format(memuse.getMax() / KB) + "   \t" +
                            H5.getOpenIDCount());
                }

                if (sum > 0)
                    break;

                mem0 = mem1;
            }

            testFile = new H5File(fname, FileFormat.READ);
            testFile.open();
            testFile.getRootObject();
            try { Thread.sleep(10); } catch (Exception ex) {;}
            testFile.close();
        }

        return sum;
    }

    @SuppressWarnings("unchecked")
    private static final long test_default_file() {
        long nObjs = 0; // number of object left open
        Dataset dset =null;
        File tmpFile = null;
        MemoryUsage memuse = null;
        DecimalFormat df = new DecimalFormat("000.00#E0#");

        for (int i=0; i<DIM_SIZE; i++) {
            DATA_INT[i] = i;
            DATA_LONG[i] = i;
            DATA_FLOAT[i] = i+i/100.0f;
            DATA_BYTE[i] = (byte)Math.IEEEremainder(i, 127);
            DATA_STR[i] = "str"+i;
            DATA_ENUM[i] = (int)Math.IEEEremainder(i, 2);
        }

        DATA_COMP.add(0, DATA_INT);
        DATA_COMP.add(1, DATA_FLOAT);
        DATA_COMP.add(2, DATA_STR);
        DATA_COMP.add(3, DATA_LONG);

        int count = 0;
        String sumStr="-----";
        long KB = 1024, mem0=0, mem1=0, sum=0, diff=0;

        if (DEBUG) {
            System.out.flush();
            System.out.println("\n\nNo. of loops\tIncrease\tUsed(KB)\tTotal(KB)\tNo. of open IDs\n"+
                    "_______________________________________________________________________________\n");
        }

        int nhigh = 0;
        while(count<NLOOPS) {
            count ++;
            if (count % NPRINT == 0) {
                memuse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                mem1 = memuse.getCommitted()/KB;
                if (count>NSTART) {
                    diff = mem1-mem0;
                    sum += diff;
                    if (diff>0)
                        nhigh++;
                    else
                        nhigh=0;
                    sumStr = df.format(sum);
                }

                if (DEBUG) {
                    System.out.println(
                            df.format(count) + "   \t" +
                            sumStr + "    \t" +
                            df.format((mem1)) + "    \t" +
                            df.format(memuse.getMax() / KB) + "   \t" +
                            H5.getOpenIDCount());
                }

                if ((sum/KB) > 0 || nhigh > 3)
                    break;

                mem0 = mem1;
            }

            tmpFile = null;
            try {
                try {
                    tmpFile = createTestFile();
                }
                catch (Exception ex) {
                    sum = 1;
                    tmpFile = null;
                    break;
                }

                // test two open options: open full tree or open individual object only
                for (int openOption=0; openOption<2; openOption++) {
                    nObjs = 0;
                    H5File file = new H5File(NAME_FILE_H5, FileFormat.WRITE);

                    if (openOption == 0) {
                        try {
                            file.open(); // open the full tree
                        }
                        catch (Exception ex) {
                            System.err.println("file.open(). "+ ex);
                        }
                    }

                    try {
                        Group rootGrp = (Group) file.get("/");

                        // datasets
                        for (int j=0; j<DNAMES.length; j++) {
                            dset = (Dataset)file.get(DNAMES[j]);
                            dset.init();
                            Object data = dset.getData();
                            try {
                                dset.write(data);
                            }
                            catch (Exception ex) {}

                            ((H5ScalarDS)dset).getMetadata();

                            // copy data into a new datast
                            if (dset instanceof ScalarDS) {
                                try {
                                    dset = dset.copy(rootGrp, DNAMES[j]+"_copy"+openOption, DIMs, data);
                                }
                                catch (Exception ex) {}
                            }
                        }

                        // groups
                        file.get(NAME_GROUP);
                        file.get(NAME_GROUP_ATTR);
                        file.get(NAME_GROUP_SUB);

                        // datatypes
                        file.get(NAME_DATATYPE_INT);
                        file.get(NAME_DATATYPE_FLOAT);
                        file.get(NAME_DATATYPE_STR);
                    }
                    catch (Exception ex) {
                        System.err.println("file.get(). "+ ex);
                    }

                    nObjs = 0;
                    try { nObjs = H5.H5Fget_obj_count(file.getFID(), HDF5Constants.H5F_OBJ_ALL); }
                    catch (Exception ex) { ; }
                    if (nObjs > 1)
                        System.err.println("Possible memory leak. Some objects are still open.");

                    try {
                        file.close();
                    }
                    catch (Exception ex) {
                        System.err.println("file.close() failed. "+ ex);
                    }
                } //  (int openOption=0; openOption<2; openOption++)
            }
            finally {
                // delete the testing file
                if (tmpFile != null)
                    tmpFile.delete();
            }
        } // while (true)

        return sum;
    }

    /**
     * Calls garbage collector
     */
    private static void collectGarbage() {
        try {
            System.gc();
            Thread.sleep(100);
        }
        catch (final Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * Creates an HDF5 test file.
     *
     * The test file contains the following objects:
     *
     * <pre>
          /dataset_byte            Dataset {50, 10}
          /dataset_comp            Dataset {50, 10}
          /dataset_enum            Dataset {50, 10}
          /dataset_float           Dataset {50, 10}
          /dataset_int             Dataset {50, 10}
          /dataset_image           Dataset {50, 10}
          /dataset_str             Dataset {50, 10}
          /g0                      Group
          /g0/dataset_int          Dataset {50, 10}
          /g0/g00                  Group
          /g0/g00/dataset_float    Dataset {50, 10}
          /g0_attr                 Group
     * </pre>
     *
     * @throws Exception
     */
    private static final File createTestFile()  throws Exception {
        H5File file=null;
        Group g0, g1, g00;

        final H5Datatype typeInt = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeFloat = new H5Datatype(Datatype.CLASS_FLOAT, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeStr = new H5Datatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeStrVlen = new H5Datatype(Datatype.CLASS_STRING, -1, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeChar = new H5Datatype(Datatype.CLASS_CHAR, 1, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype typeEnum = new H5Datatype(Datatype.CLASS_ENUM, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);

        COMPOUND_MEMBER_DATATYPES[0] = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        COMPOUND_MEMBER_DATATYPES[1] = new H5Datatype(Datatype.CLASS_FLOAT, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        COMPOUND_MEMBER_DATATYPES[2] = new H5Datatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
        COMPOUND_MEMBER_DATATYPES[3] = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.SIGN_NONE);
        COMPOUND_MEMBER_DATATYPES[4] = new H5Datatype(Datatype.CLASS_STRING, -1, Datatype.NATIVE, Datatype.NATIVE);

        final H5Datatype strAttrType = new H5Datatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
        final H5Datatype intAttrType = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);

        final H5ScalarAttr ATTRIBUTE_STR = new H5ScalarAttr(null, "attrName", strAttrType, new long[] { 1 }, new String[] {"attrValue"});
        final H5ScalarAttr ATTRIBUTE_INT_ARRAY = new H5ScalarAttr(null, "arrayInt", intAttrType, new long[] { 10 }, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        file = new H5File(NAME_FILE_H5, FileFormat.CREATE);
        file.open();

        g0 = file.createGroup(NAME_GROUP, null);
        g1 = file.createGroup(NAME_GROUP_ATTR, null);
        g00 = file.createGroup(NAME_GROUP_SUB, null);

        ATTRIBUTE_STR.setParentObject(g1);
        ATTRIBUTE_INT_ARRAY.setParentObject(g1);
        ATTRIBUTE_STR.write();
        ATTRIBUTE_INT_ARRAY.write();

        final Dataset[] dsets = new Dataset[11];
        dsets[0] = file.createScalarDS  (NAME_DATASET_INT, null, typeInt, DIMs, null, CHUNKs, 9, DATA_INT);
        dsets[1] = file.createScalarDS  (NAME_DATASET_FLOAT, null, typeFloat, DIMs, null, CHUNKs, 9, DATA_FLOAT);
        dsets[2] = file.createScalarDS  (NAME_DATASET_CHAR, null, typeChar, DIMs, null, CHUNKs, 9, DATA_BYTE);
        dsets[3] = file.createScalarDS  (NAME_DATASET_STR, null, typeStr, DIMs, null, CHUNKs, 9, DATA_STR);
        dsets[4] = file.createScalarDS  (NAME_DATASET_ENUM, null, typeEnum, DIMs, null, CHUNKs, 9, DATA_ENUM);
        dsets[5] = file.createScalarDS  (NAME_DATASET_SUB, g0, typeInt, DIMs, null, CHUNKs, 9, DATA_INT);
        dsets[6] = file.createScalarDS  (NAME_DATASET_SUB_SUB, g00, typeFloat, DIMs, null, CHUNKs, 9, DATA_FLOAT);
        dsets[7] = file.createImage     (NAME_DATASET_IMAGE, null, typeInt, DIMs, null, CHUNKs, 9, 1, -1, DATA_BYTE);
        dsets[8] = file.createCompoundDS(NAME_DATASET_COMPOUND, null, DIMs, null, CHUNKs, 9,
                COMPOUND_MEMBER_NAMES, COMPOUND_MEMBER_DATATYPES, null, DATA_COMP);
        dsets[9] = file.createCompoundDS(NAME_DATASET_COMPOUND_SUB, null, DIMs, null, CHUNKs, 9,
                COMPOUND_MEMBER_NAMES, COMPOUND_MEMBER_DATATYPES, null, DATA_COMP);
        dsets[10] = file.createScalarDS  (NAME_DATASET_STR_VLEN, null, typeStrVlen, DIMs, null, CHUNKs, 9, DATA_STR);
        for (int i=0; i<dsets.length; i++) {
            ATTRIBUTE_STR.setParentObject(dsets[i]);
            ATTRIBUTE_INT_ARRAY.setParentObject(dsets[i]);
            ATTRIBUTE_STR.write();
            ATTRIBUTE_INT_ARRAY.write();
        }

        Datatype dnative = file.createDatatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        file.createNamedDatatype(dnative, NAME_DATATYPE_INT);
        dnative = file.createDatatype(Datatype.CLASS_FLOAT, DATATYPE_SIZE, Datatype.NATIVE, Datatype.NATIVE);
        file.createNamedDatatype(dnative, NAME_DATATYPE_FLOAT);
        dnative = file.createDatatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
        file.createNamedDatatype(dnative, NAME_DATATYPE_STR);

        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(file.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {}
        if (nObjs > 1)
            System.err.println("Possible memory leak. Some objects are still open." +nObjs);

        try {
            file.close();
        }
        catch (final Exception ex) {}

        return file;
    }
}

