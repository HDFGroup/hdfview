//
//   Creating groups using absolute and relative names.

package intro;

import hdf.object.FileFormat;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;


public class H5Object_CreateGroupAbsoluteRelative {
    private static String FILENAME = "H5Object_CreateGroupAbsoluteRelative.h5";
    private static String GROUPNAME = "MyGroup";
    private static String GROUPNAME_A = "GroupA";
    private static String GROUPNAME_B = "GroupB";

    private static void CreateGroupAbsoluteAndRelative() {
        H5File file = null;
        H5Group grp1 = null;
        H5Group grp2 = null;
        H5Group grp3 = null;
        long group1_id = -1;
        long group2_id = -1;
        long group3_id = -1;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create a group named "/MyGroup" in the file.
        try {
            grp1 = (H5Group)file.createGroup("/" + GROUPNAME, null);
            group1_id = grp1.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create group "Group_A" in group "MyGroup" using absolute name.
        try {
            grp2 = (H5Group)file.createGroup("/" + GROUPNAME + "/" + GROUPNAME_A, null);
            group2_id = grp2.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create group "Group_B" in group "MyGroup" using relative name.
        try {
            grp3 = (H5Group)file.createGroup(GROUPNAME_B, grp1);
            group3_id = grp3.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Close the group3.
        try {
            if (group3_id >= 0)
                grp3.close(group3_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Close the group2.
        try {
            if (group2_id >= 0)
                grp2.close(group2_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Close the group1.
        try {
            if (group1_id >= 0)
                grp1.close(group1_id);
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
        H5Object_CreateGroupAbsoluteRelative.CreateGroupAbsoluteAndRelative();
    }

}
