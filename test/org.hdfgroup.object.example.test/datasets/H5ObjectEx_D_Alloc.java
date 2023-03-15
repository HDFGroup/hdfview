/************************************************************
  This example shows how to set the space allocation time
  for a dataset.  The program first creates two datasets,
  one with the default allocation time (late) and one with
  early allocation time, and displays whether each has been
  allocated and their allocation size.  Next, it writes data
  to the datasets, and again displays whether each has been
  allocated and their allocation size.
 ************************************************************/
package datasets;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_D_Alloc {
    private static String FILENAME = "H5ObjectEx_D_Alloc.h5";
    private static String DATASETNAME1 = "DS1";
    private static String DATASETNAME2 = "DS2";
    private static final int DIM_X = 4;
    private static final int DIM_Y = 7;
    private static final int FILLVAL = 99;
    private static final int RANK = 2;
    private static final int DATATYPE_SIZE = 4;

    // Values for the status of space allocation
    enum H5D_space_status {
        H5D_SPACE_STATUS_ERROR(-1), H5D_SPACE_STATUS_NOT_ALLOCATED(0), H5D_SPACE_STATUS_PART_ALLOCATED(
                1), H5D_SPACE_STATUS_ALLOCATED(2);
        private static final Map<Integer, H5D_space_status> lookup = new HashMap<Integer, H5D_space_status>();

        static {
            for (H5D_space_status s : EnumSet.allOf(H5D_space_status.class))
                lookup.put(s.getCode(), s);
        }

        private int code;

        H5D_space_status(int space_status) {
            this.code = space_status;
        }

        public int getCode() {
            return this.code;
        }

        public static H5D_space_status get(int code) {
            return lookup.get(code);
        }
    }

    private static void allocation() {
        H5File file = null;
        Dataset dset1 = null;
        Dataset dset2 = null;
        long file_id = -1;
        long filespace_id = -1;
        long dataset_id1 = -1;
        long dataset_id2 = -1;
        long dcpl_id = -1;
        long type_id = -1;
        long[] dims = { DIM_X, DIM_Y };
        int[][] dset_data = new int[DIM_X][DIM_Y];
        int space_status = -1;
        long storage_size = 0;
        H5Datatype typeInt = null;

        // Initialize the dataset.
        for (int indx = 0; indx < DIM_X; indx++)
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                dset_data[indx][jndx] = FILLVAL;

        // Create a file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file_id = file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Creating datasets...");
        System.out.println(DATASETNAME1
                + " has allocation time H5D_ALLOC_TIME_LATE");

        // Create the datatype for the datasets.
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.ORDER_BE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the dataset using the dataset default creation property list.
        try {
            dset1 = file.createScalarDS(DATASETNAME1, null, typeInt,
                    dims, null, null, 0,
                    null);
            dataset_id1 = dset1.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(DATASETNAME2
                + " has allocation time H5D_ALLOC_TIME_EARLY");
        System.out.println();

        // Create the dataset creation property list, and set the chunk size.
        try {
            dcpl_id = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Set the allocation time to "early". This way we can be sure
        // that reading from the dataset immediately after creation will
        // return the fill value.
        try {
            if (dcpl_id >= 0)
                H5.H5Pset_alloc_time(dcpl_id, HDF5Constants.H5D_ALLOC_TIME_EARLY);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the dataset using the dataset creation property list.
        try {
            filespace_id = H5.H5Screate_simple(RANK, dims, null);
            type_id = typeInt.createNative();
            if ((file_id >= 0) && (filespace_id >= 0) && (type_id >= 0) && (dcpl_id >= 0)) {
                dataset_id2 = H5.H5Dcreate(file_id, DATASETNAME2, type_id, filespace_id,
                        HDF5Constants.H5P_DEFAULT, dcpl_id, HDF5Constants.H5P_DEFAULT);
                dset2 = new H5ScalarDS(file, DATASETNAME2, "/");
                Group pgroup = (Group) file.get("/");
                pgroup.addToMemberList(dset2);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Retrieve and print space status and storage size for dset1.
        try {
            if (dataset_id1 >= 0)
                space_status = H5.H5Dget_space_status(dataset_id1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (dataset_id1 >= 0)
                storage_size = H5.H5Dget_storage_size(dataset_id1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        String the_space = " ";
        if (H5D_space_status.get(space_status) != H5D_space_status.H5D_SPACE_STATUS_ALLOCATED)
            the_space += "not ";
        System.out.println("Space for " + DATASETNAME1 + " has" + the_space
                + "been allocated.");
        System.out.println("Storage size for " + DATASETNAME1 + " is: "
                + storage_size + " bytes.");

        // Retrieve and print space status and storage size for dset2.
        try {
            if (dataset_id2 >= 0)
                space_status = H5.H5Dget_space_status(dataset_id2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (dataset_id2 >= 0)
                storage_size = H5.H5Dget_storage_size(dataset_id2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        the_space = " ";
        if (H5D_space_status.get(space_status) != H5D_space_status.H5D_SPACE_STATUS_ALLOCATED)
            the_space += "not ";
        System.out.println("Space for " + DATASETNAME2 + " has" + the_space
                + "been allocated.");
        System.out.println("Storage size for " + DATASETNAME2 + " is: "
                + storage_size + " bytes.");
        System.out.println();

        System.out.println("Writing data...");
        System.out.println();

        // Write the data to the datasets.
        try {
            if (dset1 != null)
                dset1.write(dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (dset2 != null)
                dset2.write(dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Retrieve and print space status and storage size for dset1.
        try {
            if (dataset_id1 >= 0)
                space_status = H5.H5Dget_space_status(dataset_id1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (dataset_id1 >= 0)
                storage_size = H5.H5Dget_storage_size(dataset_id1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        the_space = " ";
        if (H5D_space_status.get(space_status) != H5D_space_status.H5D_SPACE_STATUS_ALLOCATED)
            the_space += "not ";
        System.out.println("Space for " + DATASETNAME1 + " has" + the_space
                + "been allocated.");
        System.out.println("Storage size for " + DATASETNAME1 + " is: "
                + storage_size + " bytes.");

        // Retrieve and print space status and storage size for dset2.
        try {
            if (dataset_id2 >= 0)
                space_status = H5.H5Dget_space_status(dataset_id2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (dataset_id2 >= 0)
                storage_size = H5.H5Dget_storage_size(dataset_id2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        the_space = " ";
        if (H5D_space_status.get(space_status) != H5D_space_status.H5D_SPACE_STATUS_ALLOCATED)
            the_space += "not ";
        System.out.println("Space for " + DATASETNAME2 + " has" + the_space
                + "been allocated.");
        System.out.println("Storage size for " + DATASETNAME2 + " is: "
                + storage_size + " bytes.");
        System.out.println();

        // End access to the dataset and release resources used by it.
        try {
            if (dcpl_id >= 0)
                H5.H5Pclose(dcpl_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (type_id >= 0)
                H5.H5Tclose(type_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (dataset_id1 >= 0)
                dset1.close(dataset_id1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (dataset_id2 >= 0)
                dset2.close(dataset_id2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (filespace_id >= 0)
                H5.H5Sclose(filespace_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Close the file.
        try {
            file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        H5ObjectEx_D_Alloc.allocation();
    }

}
