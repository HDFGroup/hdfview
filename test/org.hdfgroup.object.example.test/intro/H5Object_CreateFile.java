//
//    Creating and closing a file.

package intro;

import hdf.object.FileFormat;
import hdf.object.h5.H5File;


public class H5Object_CreateFile {
    static final String FILENAME = "H5Object_CreateFile.h5";

    private static void CreateFile() {
        H5File file = null;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
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
        H5Object_CreateFile.CreateFile();
    }

}
