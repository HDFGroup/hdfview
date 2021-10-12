/************************************************************
  This example shows how to read and write data to a dataset
  using a data transform expression.  The program first
  writes integers to a dataset using the transform
  expression TRANSFORM, then closes the file.  Next, it
  reopens the file, reads back the data without a transform,
  and outputs the data to the screen.  Finally it reads the
  data using the transform expression RTRANSFORM and outputs
  the results to the screen.
 ************************************************************/

package datasets;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;

public class H5ObjectEx_D_Transform {

    private static String FILENAME = "H5ObjectEx_D_Transform.h5";
    private static String DATASETNAME = "DS1";
    private static final int DIM_X = 4;
    private static final int DIM_Y = 7;
    private static String TRANSFORM = "x+1";
    private static String RTRANSFORM = "x-1";

    private static void writeData() {
        H5File file = null;
        Dataset dset = null;
        long dataset_id = -1;
        long dxpl_id = -1;
        long[] dims = { DIM_X, DIM_Y };
        int[][] dset_data = new int[DIM_X][DIM_Y];
        H5Datatype typeInt = null;

        // Initialize data.
        for (int indx = 0; indx < DIM_X; indx++)
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                dset_data[indx][jndx] = indx * jndx - jndx;

        //Output the data to the screen.
        System.out.println("Original Data:");
        for (int indx = 0; indx < DIM_X; indx++) {
            System.out.print(" [");
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                System.out.print(" " + dset_data[indx][jndx]+ " ");
            System.out.println("]");
        }

        //Create a new file using the default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the dataset transfer property list and define the transform expression.
        try {
            dxpl_id = H5.H5Pcreate (HDF5Constants.H5P_DATASET_XFER);
            if (dxpl_id >= 0)
                H5.H5Pset_data_transform(dxpl_id, TRANSFORM);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the datatype.
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Create the dataset using the default properties.  Unfortunately we must save as
        //a native type or the transform operation will fail.
        try {
            dset = file.createScalarDS("/" + DATASETNAME, null, typeInt,
                    dims, null, null, 0,
                    null);
            dataset_id = dset.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Write the data to the dataset using the dataset transfer property list.
        try {
            if ((dataset_id >= 0) && (dxpl_id >= 0))
                H5.H5Dwrite(dataset_id, HDF5Constants.H5T_NATIVE_INT,
                        HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, dxpl_id, dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //End access to the dataset and release resources used by it.
        try {
            if (dxpl_id >= 0)
                H5.H5Pclose(dxpl_id);
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

        //Close the file.
        try {
            file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readData() {
        H5File file = null;
        Dataset dset = null;
        long dataset_id = -1;
        long dxpl_id = -1;
        int[] dset_data = new int[DIM_X*DIM_Y];

        //Open an existing file using the default properties.
        try {
            file = new H5File(FILENAME, FileFormat.READ);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Open an existing dataset using the default properties.
        try {
            dset = (Dataset) file.get(DATASETNAME);
            dataset_id = dset.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Read the data using the default properties.
        try {
            dset.init();
            dset_data = (int[]) dset.getData();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Output the data to the screen.
        System.out.println("Data as written with transform '" + TRANSFORM + "'");
        for (int indx = 0; indx < DIM_X; indx++) {
            System.out.print(" [");
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                System.out.print(" " + dset_data[indx*DIM_Y+jndx] + " ");
            System.out.println("]");
        }

        //Create the dataset transfer property list and define the  transform expression.
        try {
            dxpl_id = H5.H5Pcreate(HDF5Constants.H5P_DATASET_XFER);
            if (dxpl_id >= 0)
                H5.H5Pset_data_transform(dxpl_id, RTRANSFORM);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Read the data using the dataset transfer property list.
        try {
            if ((dataset_id >= 0) && (dxpl_id >= 0))
                H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_INT,
                        HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, dxpl_id, dset_data);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        //Output the data to the screen.

        System.out.println("Data as written with transform  '" + TRANSFORM + "' and read with transform  '" +
                RTRANSFORM + "'");
        for (int indx = 0; indx < DIM_X; indx++) {
            System.out.print(" [");
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                System.out.print(" " + dset_data[indx*DIM_Y+jndx] + " ");
            System.out.println("]");
        }

        //Close and release resources.
        try {
            if (dxpl_id >= 0)
                H5.H5Pclose(dxpl_id);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        try {
            if (dataset_id >= 0)
                dset.close(dataset_id);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        try {
            file.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        H5ObjectEx_D_Transform.writeData();
        H5ObjectEx_D_Transform.readData();
    }


}
