package datatypes;

import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_T_VLString {
    private static String FILENAME = "H5ObjectEx_T_VLString.h5";
    private static String DATASETNAME = "DS1";

    private static void createDataset() {
        H5File file = null;
        String[] str_data = { "Parting", "is such", "sweet", "sorrow." };
        long[] dims = { str_data.length };
        H5Datatype typeVLStr = null;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);

            // Create the base datatype.
            typeVLStr = new H5Datatype(Datatype.CLASS_STRING, -1, Datatype.NATIVE, Datatype.NATIVE);

            // Create the dataset and write the string data to it.
            file.createScalarDS(DATASETNAME, null, typeVLStr, dims, null, null, 0, str_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readDataset() {
        H5File file = null;
        H5ScalarDS dset = null;
        String[] str_data = { "", "", "", "" };

        try {
            file = new H5File(FILENAME, FileFormat.READ);
            dset = (H5ScalarDS) file.get(DATASETNAME);
            str_data = (String[]) dset.read();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        for (int indx = 0; indx < str_data.length; indx++)
            System.out.println(DATASETNAME + " [" + indx + "]: " + str_data[indx]);

    }

    public static void main(String[] args) {
        H5ObjectEx_T_VLString.createDataset();
        H5ObjectEx_T_VLString.readDataset();
    }
}
