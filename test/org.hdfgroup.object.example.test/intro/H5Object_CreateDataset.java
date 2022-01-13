//
//   Creating and closing a dataset.

package intro;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;


public class H5Object_CreateDataset {
    private static String FILENAME = "H5Object_CreateDataset.h5";
    private static String DATASETNAME = "dset";
    private static final int DIM_X = 4;
    private static final int DIM_Y = 6;
    private static final int DATATYPE_SIZE = 4;

    private static void CreateDataset() {
        H5File file = null;
        Dataset dset = null;
        int[][] dset_data = new int[DIM_X][DIM_Y];
        long[] dims = { DIM_X, DIM_Y };
        H5Datatype typeInt = null;

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
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.ORDER_BE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the dataset.
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

    public static void main(String[] args) {
        H5Object_CreateDataset.CreateDataset();
    }

}
