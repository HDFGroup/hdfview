//
//   Creating a dataset attribute.

package intro;

import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5ScalarDS;


public class H5Object_CreateAttribute {
    private static String FILENAME = "H5Object_CreateAttribute.h5";
    private static String DATASETNAME = "dset";
    private static final int DIM_X = 4;
    private static final int DIM_Y = 6;
    private static String DATASETATTRIBUTE = "Units";
    private static final int DATATYPE_SIZE = 4;

    private static void CreateDatasetAttribute() {
        H5File file = null;
        H5ScalarDS dset = null;
        H5ScalarAttr attr = null;
        int[][] dset_data = new int[DIM_X][DIM_Y];
        long[] dims1 = { DIM_X, DIM_Y };
        long[] dims = { 2 };
        int[] attr_data = { 100, 200 };
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
            final H5Group rootGrp = (H5Group)file.get("/");
            dset = (H5ScalarDS) H5ScalarDS.create("/" + DATASETNAME, rootGrp, typeInt,
                    dims1, null, null, 0,
                    dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create a dataset attribute.
        try {
            attr = new H5ScalarAttr(dset, DATASETATTRIBUTE, typeInt, dims, attr_data);
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
        H5Object_CreateAttribute.CreateDatasetAttribute();
    }

}
