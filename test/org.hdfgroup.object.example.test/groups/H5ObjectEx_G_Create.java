/************************************************************
  This example shows how to create, open, and close a group.
 ************************************************************/

package groups;

import hdf.object.FileFormat;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;


public class H5ObjectEx_G_Create {
    private static String FILENAME = "H5ObjectEx_G_Create.h5";
    private static String GROUPNAME = "G1";

    private static void CreateGroup() {
        H5File file = null;
        H5Group grp = null;
        long group_id = -1;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create a group in the file.
        try {
            grp = (H5Group)file.createGroup("/" + GROUPNAME, null);
            group_id = grp.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Close the group. The handle "group" can no longer be used.
        try {
            if (group_id >= 0)
                grp.close(group_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Re-open the group, obtaining a new handle.
        try {
            group_id = grp.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Close the group.
        try {
            if (group_id >= 0)
                grp.close(group_id);
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
        H5ObjectEx_G_Create.CreateGroup();
    }

}
