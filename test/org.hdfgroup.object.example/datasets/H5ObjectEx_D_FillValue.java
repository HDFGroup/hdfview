/************************************************************
  This example shows how to set the fill value for a
  dataset.  The program first sets the fill value to
  FILLVAL, creates a dataset with dimensions of DIM_XxDIM_Y,
  reads from the uninitialized dataset, and outputs the
  contents to the screen.  Next, it writes integers to the
  dataset, reads the data back, and outputs it to the
  screen.  Finally it extends the dataset, reads from it,
  and outputs the result to the screen.
 ************************************************************/
package datasets;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_D_FillValue {
    private static String FILENAME = "H5ObjectEx_D_FillValue.h5";
    private static String DATASETNAME = "ExtendibleArray";
    private static final int DIM_X = 4;
    private static final int DIM_Y = 7;
    private static final int EDIM_X = 6;
    private static final int EDIM_Y = 10;
    private static final int CHUNK_X = 4;
    private static final int CHUNK_Y = 4;
    private static final int RANK = 2;
    private static final int NDIMS = 2;
    private static final int FILLVAL = 99;
    private static final int DATATYPE_SIZE = 4;

    private static void fillValue() {
        H5File file = null;
        H5ScalarDS dset = null;
        long file_id = -1;
        long dcpl_id = -1;
        long dataspace_id = -1;
        long dataset_id = -1;
        long type_id = -1;
        long[] dims = { DIM_X, DIM_Y };
        long[] extdims = { EDIM_X, EDIM_Y };
        long[] chunk_dims = { CHUNK_X, CHUNK_Y };
        long[] maxdims = { HDF5Constants.H5S_UNLIMITED, HDF5Constants.H5S_UNLIMITED };
        int[][] write_dset_data = new int[DIM_X][DIM_Y];
        int[] read_dset_data = new int[DIM_X*DIM_Y];
        int[] extend_dset_data = new int[EDIM_X*EDIM_Y];
        H5Datatype typeInt = null;

        // Initialize the dataset.
        for (int indx = 0; indx < DIM_X; indx++)
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                write_dset_data[indx][jndx] = indx * jndx - jndx;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file_id = file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create datatype.
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.ORDER_LE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create dataspace with unlimited dimensions.
        try {
            dataspace_id = H5.H5Screate_simple(RANK, dims, maxdims);
            type_id = typeInt.createNative();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the dataset creation property list.
        try {
            dcpl_id = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Set the chunk size.
        try {
            if (dcpl_id >= 0)
                H5.H5Pset_chunk(dcpl_id, NDIMS, chunk_dims);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Set the fill value for the dataset
        try {
            int[] fill_value = { FILLVAL };
            if (dcpl_id >= 0)
                H5.H5Pset_fill_value(dcpl_id, HDF5Constants.H5T_NATIVE_INT, fill_value);
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
            if ((file_id >= 0) && (dataspace_id >= 0) && (dcpl_id >= 0))
                dataset_id = H5.H5Dcreate(file_id, DATASETNAME,
                        type_id, dataspace_id, HDF5Constants.H5P_DEFAULT, dcpl_id, HDF5Constants.H5P_DEFAULT);
            dset = new H5ScalarDS(file, DATASETNAME, "/");
            Group pgroup = (Group) file.get("/");
            pgroup.addToMemberList(dset);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Read values from the dataset, which has not been written to yet.
        try {
            dset.init();
            read_dset_data = (int[]) dset.getData();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        System.out.println("Dataset before being written to:");
        for (int indx = 0; indx < DIM_X; indx++) {
            System.out.print(" [ ");
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                System.out.print(read_dset_data[indx*DIM_Y+jndx] + " ");
            System.out.println("]");
        }
        System.out.println();

        // Write the data to the dataset.
        try {
            dset.write(write_dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Read the data back.
        try {
            dset.init();
            read_dset_data = (int[]) dset.getData();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        System.out.println("Dataset after being written to:");
        for (int indx = 0; indx < DIM_X; indx++) {
            System.out.print(" [ ");
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                System.out.print(read_dset_data[indx*DIM_Y+jndx] + " ");
            System.out.println("]");
        }
        System.out.println();

        // Extend the dataset.
        try {
            dset.extend(extdims);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Read from the extended dataset.
        try {
            dset.init();
            extend_dset_data = (int[]) dset.getData();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        System.out.println("Dataset after extension:");
        for (int indx = 0; indx < EDIM_X; indx++) {
            System.out.print(" [ ");
            for (int jndx = 0; jndx < EDIM_Y; jndx++)
                System.out.print(extend_dset_data[indx*EDIM_Y+jndx] + " ");
            System.out.println("]");
        }
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
            if (dataset_id >= 0)
                dset.close(dataset_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (dataspace_id >= 0)
                H5.H5Sclose(dataspace_id);
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
        H5ObjectEx_D_FillValue.fillValue();
    }

}
