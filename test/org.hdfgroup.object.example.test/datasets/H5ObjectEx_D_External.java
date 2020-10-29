/************************************************************
  This example shows how to read and write data to an
  external dataset.  The program first writes integers to an
  external dataset with dataspace dimensions of DIM_XxDIM_Y,
  then closes the file.  Next, it reopens the file, reads
  back the data, and outputs the name of the external data
  file and the data to the screen.
 ************************************************************/
package datasets;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_D_External {
    private static String FILENAME = "H5ObjectEx_D_External.h5";
    private static String EXTERNALNAME = "H5ObjectEx_D_External.data";
    private static String DATASETNAME = "DS1";
    private static final int DIM_X = 4;
    private static final int DIM_Y = 7;
    private static final int RANK = 2;
    private static final int NAME_BUF_SIZE = 32;
    private static final int DATATYPE_SIZE = 4;

    private static void writeExternal() {
        H5File file = null;
        Dataset dset = null;
        long file_id = -1;
        long dcpl_id = -1;
        long filespace_id = -1;
        long dataset_id = -1;
        long type_id = -1;
        long[] dims = { DIM_X, DIM_Y };
        int[][] dset_data = new int[DIM_X][DIM_Y];
        H5Datatype typeInt = null;

        // Initialize the dataset.
        for (int indx = 0; indx < DIM_X; indx++)
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                dset_data[indx][jndx] = indx * jndx - jndx;

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

        // Create dataspace. Setting maximum size to NULL sets the maximum
        // size to be the current size.
        try {
            filespace_id = H5.H5Screate_simple(RANK, dims, null);
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

        // set the external file.
        try {
            if (dcpl_id >= 0)
                H5.H5Pset_external(dcpl_id, EXTERNALNAME, 0,
                        HDF5Constants.H5F_UNLIMITED);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the HDF5Constants.dataset.
        try {
            if ((file_id >= 0) && (filespace_id >= 0) && (dcpl_id >= 0))
                dataset_id = H5.H5Dcreate(file_id, DATASETNAME,
                        type_id, filespace_id, HDF5Constants.H5P_DEFAULT, dcpl_id, HDF5Constants.H5P_DEFAULT);
            dset = new H5ScalarDS(file, DATASETNAME, "/");
            Group pgroup = (Group) file.get("/");
            pgroup.addToMemberList(dset);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Write the dataset.
        try {
            dset.write(dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

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

        // Terminate access to the data space.
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

    private static void readExternal() {
        H5File file = null;
        Dataset dset = null;
        long dcpl_id = -1;
        long dataset_id = -1;
        int[] dset_data = new int[DIM_X*DIM_Y];
        String[] Xname = new String[1];

        // Open file using the default properties.
        try {
            file = new H5File(FILENAME, FileFormat.READ);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Open dataset using the default properties.
        try {
            dset = (Dataset) file.get(DATASETNAME);
            dataset_id = dset.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Retrieve the dataset creation property list.
        try {
            if (dataset_id >= 0)
                dcpl_id = H5.H5Dget_create_plist(dataset_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Retrieve and print the name of the external file.
        long[] Xsize = new long[NAME_BUF_SIZE];
        try {
            if (dcpl_id >= 0)
                H5.H5Pget_external(dcpl_id, 0, Xsize.length, Xname, Xsize);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(DATASETNAME + " is stored in file: " + Xname[0]);

        // Read the data using the default properties.
        try {
            dset.init();
            dset_data = (int[]) dset.getData();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        System.out.println(DATASETNAME + ":");
        for (int indx = 0; indx < DIM_X; indx++) {
            System.out.print(" [ ");
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                System.out.print(dset_data[indx*DIM_Y+jndx] + " ");
            System.out.println("]");
        }
        System.out.println();

        // Close the dataset.
        try {
            if (dataset_id >= 0)
                dset.close(dataset_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (dcpl_id >= 0)
                H5.H5Pclose(dcpl_id);
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
        H5ObjectEx_D_External.writeExternal();
        H5ObjectEx_D_External.readExternal();
    }

}
