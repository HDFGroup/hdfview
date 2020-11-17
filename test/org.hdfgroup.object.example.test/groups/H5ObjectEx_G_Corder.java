package groups;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.object.FileFormat;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

public class H5ObjectEx_G_Corder {
    private static String FILE = "H5ObjectEx_G_Corder.h5";

    private static void CreateGroup() throws Exception {
        H5File      file = null;
        H5Group     grp = null;
        long      group_id = -1;
        long      gcpl_id = -1;
        long      status;
        H5G_info_t  ginfo;
        int      i;
        String   name;

        try {
            // Create a new file using default properties.
            file = new H5File(FILE, FileFormat.CREATE);
            file.open();

            // Create group creation property list and enable link creation order tracking.
            gcpl_id = H5.H5Pcreate (HDF5Constants.H5P_GROUP_CREATE);
            status = H5.H5Pset_link_creation_order(gcpl_id, HDF5Constants.H5P_CRT_ORDER_TRACKED + HDF5Constants.H5P_CRT_ORDER_INDEXED);

            // Create primary group using the property list.
            if (status >= 0) {
                grp = (H5Group) file.createGroup("index_group", null, HDF5Constants.H5P_DEFAULT, gcpl_id);
                group_id = grp.open();
            }

            try {
                /*
                 * Create subgroups in the primary group.  These will be tracked
                 * by creation order.  Note that these groups do not have to have
                 * the creation order tracking property set.
                 */
                file.createGroup("H", grp);
                file.createGroup("D", grp);
                file.createGroup("F", grp);
                file.createGroup("5", grp);

                // Get group info.
                ginfo = H5.H5Gget_info(group_id);

                //Traverse links in the primary group using alphabetical indices (H5_INDEX_NAME).
                System.out.println("Traversing group using alphabetical indices:");
                for (i = 0; i < ginfo.nlinks; i++) {
                    //Retrieve the name of the ith link in a group
                    name = H5.H5Lget_name_by_idx(group_id, ".", HDF5Constants.H5_INDEX_NAME, HDF5Constants.H5_ITER_INC, i, HDF5Constants.H5P_DEFAULT);
                    System.out.println("Index " + i + ": " + name);
                }

                //Traverse links in the primary group by creation order (H5_INDEX_CRT_ORDER).
                System.out.println("Traversing group using creation order indices:");
                for (i = 0; i < ginfo.nlinks; i++) {
                    //Retrieve the name of the ith link in a group
                    name = H5.H5Lget_name_by_idx(group_id, ".", HDF5Constants.H5_INDEX_CRT_ORDER, HDF5Constants.H5_ITER_INC, i, HDF5Constants.H5P_DEFAULT);
                    System.out.println("Index " + i + ": " + name);
                }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Close and release resources.
            // Group property lists closed during createGroup
            if (group_id >= 0)
                grp.close (group_id);
            file.close();
        }
    }

    public static void main(String[] args) {
        try {
            H5ObjectEx_G_Corder.CreateGroup();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}

