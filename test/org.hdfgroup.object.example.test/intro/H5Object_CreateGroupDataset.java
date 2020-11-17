//
//  Create two datasets within groups.

package intro;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

public class H5Object_CreateGroupDataset {
    private static String FILENAME = "H5Object_CreateGroupDataset.h5";
    private static String GROUPNAME = "MyGroup";
    private static String GROUPNAME_A = "GroupA";
    private static String DATASETNAME1 = "dset1";
    private static String DATASETNAME2 = "dset2";
    private static final int DIM1_X = 3;
    private static final int DIM1_Y = 3;
    private static final int DIM2_X = 2;
    private static final int DIM2_Y = 10;
    private static final int DATATYPE_SIZE = 4;

    private static void h5_crtgrpd() {
        H5File file = null;
        H5Group grp = null;
        Dataset dset = null;
        int[][] dset1_data = new int[DIM1_X][DIM1_Y];
        int[][] dset2_data = new int[DIM2_X][DIM2_Y];
        long[] dims1 = { DIM1_X, DIM1_Y };
        long[] dims2 = { DIM2_X, DIM2_Y };
        H5Datatype typeInt = null;

        // Initialize the first dataset.
        for (int indx = 0; indx < DIM1_X; indx++)
            for (int jndx = 0; jndx < DIM1_Y; jndx++)
                dset1_data[indx][jndx] = jndx + 1;

        // Initialize the second dataset.
        for (int indx = 0; indx < DIM2_X; indx++)
            for (int jndx = 0; jndx < DIM2_Y; jndx++)
                dset2_data[indx][jndx] = jndx + 1;

        // Open an existing file or create a new file.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the datatype for the datasets.
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, DATATYPE_SIZE, Datatype.ORDER_BE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the group for the first dataset.
        try {
            grp = (H5Group)file.createGroup("/" + GROUPNAME, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the dataset in group "MyGroup".
        // Write the first dataset.
        try {
            dset = file.createScalarDS("/" + GROUPNAME + "/" + DATASETNAME1, grp, typeInt,
                    dims1, null, null, 0,
                    dset1_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the second dataset in group "Group_A".
        try {
            dset = file.createScalarDS("/" + GROUPNAME + "/" + DATASETNAME2, grp, typeInt,
                    dims2, null, null, 0,
                    dset2_data);
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
        H5Object_CreateGroupDataset.h5_crtgrpd();
    }

}
