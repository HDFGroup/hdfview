/************************************************************
  This example shows how to read and write string datatypes
  to an attribute.  The program first writes strings to an
  attribute with a dataspace of DIM0, then closes the file.
  Next, it reopens the file, reads back the data, and
  outputs it to the screen.
 ************************************************************/

package datatypes;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_T_StringAttribute {
    private static String FILENAME = "H5ObjectEx_T_StringAttribute.h5";
    private static String DATASETNAME = "DS1";
    private static String ATTRIBUTENAME = "A1";
    private static final int DIM0 = 4;
    private static final int SDIM = 8;
    private static final int RANK = 1;

    private static void CreateDataset() {
        H5File file = null;
        H5ScalarDS dset = null;
        long memtype_id = -1;
        long filetype_id = -1;
        long dataspace_id = -1;
        long dataset_id = -1;
        long attribute_id = -1;
        long[] dims = { DIM0 };
        byte[][] dset_data = new byte[DIM0][SDIM];
        H5Datatype typeInt = null;
        StringBuilder[] str_data = { new StringBuilder("Parting"), new StringBuilder("is such"),
                new StringBuilder("sweet"), new StringBuilder("sorrow.") };

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create file and memory datatypes. For this example we will save
        // the strings as FORTRAN strings, therefore they do not need space
        // for the null terminator in the file.
        try {
            filetype_id = H5.H5Tcopy(HDF5Constants.H5T_FORTRAN_S1);
            if (filetype_id >= 0)
                H5.H5Tset_size(filetype_id, SDIM - 1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            memtype_id = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
            if (memtype_id >= 0)
                H5.H5Tset_size(memtype_id, SDIM);
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

        // Create the attribute.
        try {
            if ((dataset_id >= 0) && (dataspace_id >= 0) && (filetype_id >= 0))
                attribute_id = H5.H5Acreate(dataset_id, ATTRIBUTENAME, filetype_id, dataspace_id,
                        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Write the data to the dataset.
        try {
            for (int indx = 0; indx < DIM0; indx++) {
                for (int jndx = 0; jndx < SDIM; jndx++) {
                    if (jndx < str_data[indx].length())
                        dset_data[indx][jndx] = (byte) str_data[indx].charAt(jndx);
                    else
                        dset_data[indx][jndx] = 0;
                }
            }
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

        // Terminate access to the data space.
        try {
            if (dataspace_id >= 0)
                H5.H5Sclose(dataspace_id);
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
        long dataspace_id = -1;
        long dataset_id = -1;
        long attribute_id = -1;
        long sdim = 0;
        long[] dims = { DIM0 };
        byte[][] dset_data;
        StringBuilder[] str_data;

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

        // Get the datatype and its size.
        try {
            if (attribute_id >= 0)
                filetype_id = H5.H5Aget_type(attribute_id);
            if (filetype_id >= 0) {
                sdim = H5.H5Tget_size(filetype_id);
                sdim++; // Make room for null terminator
            }
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

        // Allocate space for data.
        dset_data = new byte[(int) dims[0]][(int)sdim];
        str_data = new StringBuilder[(int) dims[0]];

        // Create the memory datatype.
        try {
            memtype_id = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
            if (memtype_id >= 0)
                H5.H5Tset_size(memtype_id, sdim);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Read data.
        try {
            if ((attribute_id >= 0) && (memtype_id >= 0))
                H5.H5Aread(attribute_id, memtype_id, dset_data);
            byte[] tempbuf = new byte[(int)sdim];
            for (int indx = 0; indx < (int) dims[0]; indx++) {
                for (int jndx = 0; jndx < sdim; jndx++) {
                    tempbuf[jndx] = dset_data[indx][jndx];
                }
                str_data[indx] = new StringBuilder(new String(tempbuf).trim());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        for (int indx = 0; indx < dims[0]; indx++) {
            System.out.println(DATASETNAME + " [" + indx + "]: " + str_data[indx]);
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
        H5ObjectEx_T_StringAttribute.CreateDataset();
        // Now we begin the read section of this example. Here we assume
        // the dataset and array have the same name and rank, but can have
        // any size. Therefore we must allocate a new array to read in
        // data using malloc().
        H5ObjectEx_T_StringAttribute.ReadDataset();
    }

}
