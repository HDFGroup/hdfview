/************************************************************
  This example shows how to read and write opaque datatypes
  to a dataset.  The program first writes opaque data to a
  dataset with a dataspace of DIM0, then closes the file.
  Next, it reopens the file, reads back the data, and
  outputs it to the screen.
 ************************************************************/

package datatypes;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_T_Opaque {
    private static String FILENAME = "H5ObjectEx_T_Opaque.h5";
    private static String DATASETNAME = "DS1";
    private static final int DIM0 = 4;
    private static final int LEN = 7;
    private static final int RANK = 1;

    private static void CreateDataset() {
        H5File file = null;
        H5ScalarDS dset = null;
        long file_id = -1;
        long dataspace_id = -1;
        long datatype_id = -1;
        long dataset_id = -1;
        long[] dims = { DIM0 };
        byte[] dset_data = new byte[DIM0 * LEN];
        byte[] str_data = { 'O', 'P', 'A', 'Q', 'U', 'E' };

        // Initialize data.
        for (int indx = 0; indx < DIM0; indx++) {
            for (int jndx = 0; jndx < LEN - 1; jndx++)
                dset_data[jndx + indx * LEN] = str_data[jndx];
            dset_data[LEN - 1 + indx * LEN] = (byte) (indx + '0');
        }

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file_id = file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create opaque datatype and set the tag to something appropriate.
        // For this example we will write and view the data as a character
        // array.
        try {
            datatype_id = H5.H5Tcreate(HDF5Constants.H5T_OPAQUE, LEN);
            if (datatype_id >= 0)
                H5.H5Tset_tag(datatype_id, "Character array");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create dataspace. Setting maximum size to NULL sets the maximum
        // size to be the current size.
        try {
            dataspace_id = H5.H5Screate_simple(RANK, dims, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the dataset and write the integer data to it. In this
        // example we will save the data as 64 bit big endian integers,
        // regardless of the native integer type. The HDF5 library
        // automatically converts between different integer types.
        try {
            if ((file_id >= 0) && (datatype_id >= 0) && (dataspace_id >= 0))
                dataset_id = H5.H5Dcreate(file_id, DATASETNAME, datatype_id, dataspace_id, HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            dset = new H5ScalarDS(file, DATASETNAME, "/");
            Group pgroup = (Group) file.get("/");
            pgroup.addToMemberList(dset);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Write the opaque data to the dataset.
        try {
            dset.write(dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // End access to the dataset and release resources used by it.
        try {
            if (dataset_id >= 0)
                dset.close(dataset_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Terminate access to the data space.
        try {
            if (dataspace_id >= 0)
                H5.H5Sclose(dataspace_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (datatype_id >= 0)
                H5.H5Tclose(datatype_id);
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

    private static void ReadDataset() {
        H5File file = null;
        H5ScalarDS dset = null;
        long datatype_id = -1;
        long dataspace_id = -1;
        long dataset_id = -1;
        long type_len = -1;
        long[] dims = { DIM0 };
        byte[] dset_data;
        String tag_name = null;

        // Open an existing file.
        try {
            file = new H5File(FILENAME, FileFormat.READ);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Open an existing dataset.
        try {
            dset = (H5ScalarDS) file.get(DATASETNAME);
            dataset_id = dset.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Get datatype and properties for the datatype.
        try {
            if (dataset_id >= 0)
                datatype_id = H5.H5Dget_type(dataset_id);
            if (datatype_id >= 0) {
                type_len = H5.H5Tget_size(datatype_id);
                tag_name = H5.H5Tget_tag(datatype_id);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Get dataspace and allocate memory for read buffer.
        try {
            if (dataset_id >= 0)
                dataspace_id = H5.H5Dget_space(dataset_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (dataspace_id >= 0)
                H5.H5Sget_simple_extent_dims(dataspace_id, dims, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Allocate buffer.
        dset_data = new byte[(int) (dims[0] * type_len)];

        // Read data.
        try {
            if ((dataset_id >= 0) && (datatype_id >= 0))
                H5.H5Dread(dataset_id, datatype_id, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                        HDF5Constants.H5P_DEFAULT, dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        System.out.println("Datatype tag for " + DATASETNAME + " is: \"" + tag_name + "\"");
        for (int indx = 0; indx < dims[0]; indx++) {
            System.out.print(DATASETNAME + "[" + indx + "]: ");
            for (int jndx = 0; jndx < type_len; jndx++) {
                char temp = (char) dset_data[jndx + indx * (int)type_len];
                System.out.print(temp);
            }
            System.out.println("");
        }
        System.out.println();

        // End access to the dataset and release resources used by it.
        try {
            if (dataset_id >= 0)
                dset.close(dataset_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Terminate access to the data space.
        try {
            if (dataspace_id >= 0)
                H5.H5Sclose(dataspace_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (datatype_id >= 0)
                H5.H5Tclose(datatype_id);
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
        H5ObjectEx_T_Opaque.CreateDataset();
        // Now we begin the read section of this example. Here we assume
        // the dataset and array have the same name and rank, but can have
        // any size. Therefore we must allocate a new array to read in
        // data using malloc().
        H5ObjectEx_T_Opaque.ReadDataset();
    }
}
