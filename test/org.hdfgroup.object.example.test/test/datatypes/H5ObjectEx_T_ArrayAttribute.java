/************************************************************
  This example shows how to read and write array datatypes
  to an attribute.  The program first writes integers arrays
  of dimension ADIM0xADIM1 to an attribute with a dataspace
  of DIM0, then closes the  file.  Next, it reopens the
  file, reads back the data, and outputs it to the screen.
 ************************************************************/

package datatypes;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_T_ArrayAttribute {
    private static String FILENAME = "H5ObjectEx_T_ArrayAttribute.h5";
    private static String DATASETNAME = "DS1";
    private static String ATTRIBUTENAME = "A1";
    private static final int DIM0 = 4;
    private static final int ADIM0 = 3;
    private static final int ADIM1 = 5;
    private static final int NDIMS = 2;

    private static void CreateDataset() {
        H5File file = null;
        H5ScalarDS dset = null;
        long filetype_id = -1;
        long memtype_id = -1;
        long dataset_id = -1;
        long attribute_id = -1;
        long[] dims = { DIM0 };
        long[] adims = { ADIM0, ADIM1 };
        int[][][] dset_data = new int[DIM0][ADIM0][ADIM1];
        H5Datatype typeIntArray = null;
        H5Datatype typeInt = null;

        // Initialize data. indx is the element in the dataspace, jndx and kndx the
        // elements within the array datatype.
        for (int indx = 0; indx < DIM0; indx++)
            for (int jndx = 0; jndx < ADIM0; jndx++)
                for (int kndx = 0; kndx < ADIM1; kndx++)
                    dset_data[indx][jndx][kndx] = indx * jndx - jndx * kndx + indx * kndx;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create base datatypes.
        try {
            typeIntArray = new H5Datatype(Datatype.CLASS_ARRAY, 8, Datatype.ORDER_LE, Datatype.NATIVE);
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, 4, Datatype.ORDER_LE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create array datatypes for file.
        try {
            filetype_id = H5.H5Tarray_create(HDF5Constants.H5T_STD_I64LE, NDIMS, adims);
            typeIntArray.fromNative(filetype_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create array datatypes for memory.
        try {
            memtype_id = H5.H5Tarray_create(HDF5Constants.H5T_NATIVE_INT, NDIMS, adims);
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

        // Create the attribute and write the array data to it.
        try {
            H5ScalarAttr dataArray = new H5ScalarAttr(dset, ATTRIBUTENAME, typeIntArray, dims);
            dataArray.write();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Write the dataset.
        try {
            if (dataset_id >= 0)
                attribute_id = H5.H5Aopen_by_name(dataset_id, ".", ATTRIBUTENAME, HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT);
            if ((attribute_id >= 0) && (memtype_id >= 0))
                H5.H5Awrite(attribute_id, memtype_id, dset_data);
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

        // Terminate access to the file type.
        try {
            if (filetype_id >= 0)
                H5.H5Tclose(filetype_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Terminate access to the mem type.
        try {
            if (memtype_id >= 0)
                H5.H5Tclose(memtype_id);
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
        long filetype_id = -1;
        long memtype_id = -1;
        long dataset_id = -1;
        long attribute_id = -1;
        long[] dims = { DIM0 };
        long[] adims = { ADIM0, ADIM1 };
        int[][][] dset_data;

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

        // Get the datatype.
        try {
            if (attribute_id >= 0)
                filetype_id = H5.H5Aget_type(attribute_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Get the datatype's dimensions.
        try {
            if (filetype_id >= 0)
                H5.H5Tget_array_dims(filetype_id, adims);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Allocate array of pointers to two-dimensional arrays (the
        // elements of the dataset.
        dset_data = new int[(int) dims[0]][(int) (adims[0])][(int) (adims[1])];

        // Create array datatypes for memory.
        try {
            memtype_id = H5.H5Tarray_create(HDF5Constants.H5T_NATIVE_INT, 2, adims);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Read data.
        try {
            if ((attribute_id >= 0) && (memtype_id >= 0))
                H5.H5Aread(attribute_id, memtype_id, dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        for (int indx = 0; indx < dims[0]; indx++) {
            System.out.println(ATTRIBUTENAME + " [" + indx + "]:");
            for (int jndx = 0; jndx < adims[0]; jndx++) {
                System.out.print(" [");
                for (int kndx = 0; kndx < adims[1]; kndx++)
                    System.out.print(dset_data[indx][jndx][kndx] + " ");
                System.out.println("]");
            }
            System.out.println();
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

        // Terminate access to the file type.
        try {
            if (filetype_id >= 0)
                H5.H5Tclose(filetype_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Terminate access to the mem type.
        try {
            if (memtype_id >= 0)
                H5.H5Tclose(memtype_id);
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
        H5ObjectEx_T_ArrayAttribute.CreateDataset();
        // Now we begin the read section of this example. Here we assume
        // the dataset and array have the same name and rank, but can have
        // any size. Therefore we must allocate a new array to read in
        // data using malloc().
        H5ObjectEx_T_ArrayAttribute.ReadDataset();
    }

}
