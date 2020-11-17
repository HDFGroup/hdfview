/************************************************************
  This example shows how to read and write data to a
  dataset.  The program first writes integers to a dataset
  with dataspace dimensions of DIM_XxDIM_Y, then closes the
  file.  Next, it reopens the file, reads back the data, and
  outputs it to the screen.
 ************************************************************/
package datasets;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;

public class H5ObjectEx_D_ReadWrite {
    private static String FILENAME = "H5ObjectEx_D_ReadWrite.h5";
    private static String DATASETNAME = "DS1";
    private static final int DIM_X = 4;
    private static final int DIM_Y = 7;
    private static final int DATATYPE_SIZE = 4;

    private static void WriteDataset() {
        H5File file = null;
        Dataset dset = null;
        long[] dims = { DIM_X, DIM_Y };
        int[][] dset_data = new int[DIM_X][DIM_Y];
        H5Datatype typeInt = null;

        // Initialize data.
        for (int indx = 0; indx < DIM_X; indx++)
            for (int jndx = 0; jndx < DIM_Y; jndx++)
                dset_data[indx][jndx] = indx * jndx - jndx;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the datatype.
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.ORDER_LE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the dataset. We will use all default properties for this example.
        // And write the data to the dataset.
        try {
            dset = file.createScalarDS("/" + DATASETNAME, null, typeInt,
                    dims, null, null, 0,
                    dset_data);
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
        Dataset dset = null;
        int[] dset_data = new int[DIM_X*DIM_Y];

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
        }
        catch (Exception e) {
            e.printStackTrace();
        }

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

        // Close the file.
        try {
            file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        H5ObjectEx_D_ReadWrite.WriteDataset();
        H5ObjectEx_D_ReadWrite.ReadDataset();
    }

}
