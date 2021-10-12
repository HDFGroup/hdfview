/************************************************************
  This example shows how to read and write bitfield
  datatypes to an attribute.  The program first writes bit
  fields to an attribute with a dataspace of DIM0xDIM1, then
  closes the file.  Next, it reopens the file, reads back
  the data, and outputs it to the screen.
 ************************************************************/

package datatypes;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_T_BitAttribute {
    private static String FILENAME = "H5ObjectEx_T_BitAttribute.h5";
    private static String DATASETNAME = "DS1";
    private static String ATTRIBUTENAME = "A1";
    private static final int DIM0 = 4;
    private static final int DIM1 = 7;
    private static final int RANK = 2;

    private static void CreateDataset() {
        H5File file = null;
        H5ScalarDS dset = null;
        long dataspace_id = -1;
        long dataset_id = -1;
        long attribute_id = -1;
        long[] dims = { DIM0, DIM1 };
        int[][] dset_data = new int[DIM0][DIM1];
        H5Datatype typeInt = null;

        // Initialize data.
        for (int indx = 0; indx < DIM0; indx++)
            for (int jndx = 0; jndx < DIM1; jndx++) {
                dset_data[indx][jndx] = 0;
                dset_data[indx][jndx] |= (indx * jndx - jndx) & 0x03; /* Field "A" */
                dset_data[indx][jndx] |= (indx & 0x03) << 2; /* Field "B" */
                dset_data[indx][jndx] |= (jndx & 0x03) << 4; /* Field "C" */
                dset_data[indx][jndx] |= ((indx + jndx) & 0x03) << 6; /* Field "D" */
            }

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the base datatype.
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, 4, Datatype.ORDER_LE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create dataset with a scalar dataspace.
        try {
            dset = (H5ScalarDS) file.createScalarDS(DATASETNAME, null, typeInt, dims, null, null, 0, null);
            dataset_id = dset.open();
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

        // Create the attribute and write the array data to it.
        try {
            if ((dataset_id >= 0) && (dataspace_id >= 0))
                attribute_id = H5.H5Acreate(dataset_id, ATTRIBUTENAME, HDF5Constants.H5T_STD_B8BE, dataspace_id,
                        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Write the dataset.
        try {
            if (attribute_id >= 0)
                H5.H5Awrite(attribute_id, HDF5Constants.H5T_NATIVE_B8, dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // End access to the dataset and release resources used by it.
        try {
            if (attribute_id >= 0)
                H5.H5Aclose(attribute_id);
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

    private static void ReadDataset() {
        H5File file = null;
        H5ScalarDS dset = null;
        long dataspace_id = -1;
        long dataset_id = -1;
        long attribute_id = -1;
        long[] dims = { DIM0, DIM1 };
        int[][] dset_data;

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

        try {
            if (dataset_id >= 0)
                attribute_id = H5.H5Aopen_by_name(dataset_id, ".", ATTRIBUTENAME, HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Get dataspace and allocate memory for read buffer.
        try {
            if (attribute_id >= 0)
                dataspace_id = H5.H5Aget_space(attribute_id);
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

        // Allocate array of pointers to two-dimensional arrays (the
        // elements of the dataset.
        dset_data = new int[(int) dims[0]][(int) (dims[1])];

        // Read data.
        try {
            if (attribute_id >= 0)
                H5.H5Aread(attribute_id, HDF5Constants.H5T_NATIVE_B8, dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        System.out.println(ATTRIBUTENAME + ":");
        for (int indx = 0; indx < dims[0]; indx++) {
            System.out.print(" [");
            for (int jndx = 0; jndx < dims[1]; jndx++) {
                System.out.print("{" + (dset_data[indx][jndx] & 0x03) + ", ");
                System.out.print(((dset_data[indx][jndx] >> 2) & 0x03) + ", ");
                System.out.print(((dset_data[indx][jndx] >> 4) & 0x03) + ", ");
                System.out.print(((dset_data[indx][jndx] >> 6) & 0x03) + "}");
            }
            System.out.println("]");
        }
        System.out.println();

        // End access to the dataset and release resources used by it.
        try {
            if (attribute_id >= 0)
                H5.H5Aclose(attribute_id);
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
        H5ObjectEx_T_BitAttribute.CreateDataset();
        // Now we begin the read section of this example. Here we assume
        // the dataset and array have the same name and rank, but can have
        // any size. Therefore we must allocate a new array to read in
        // data using malloc().
        H5ObjectEx_T_BitAttribute.ReadDataset();
    }

}
