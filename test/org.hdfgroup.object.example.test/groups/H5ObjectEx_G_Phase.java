/************************************************************
  This example shows how to set the conditions for
  conversion between compact and dense (indexed) groups.
 ************************************************************/
package groups;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;


public class H5ObjectEx_G_Phase {
    private static String FILE = "H5ObjectEx_G_Phase.h5";
    private static int MAX_GROUPS = 7;
    private static int MAX_COMPACT = 5;
    private static int MIN_DENSE = 3;

    enum H5G_storage {
        H5G_STORAGE_TYPE_UNKNOWN(-1),
        H5G_STORAGE_TYPE_SYMBOL_TABLE(0),
        H5G_STORAGE_TYPE_COMPACT(1),
        H5G_STORAGE_TYPE_DENSE(2);

        private static final Map<Integer, H5G_storage> lookup = new HashMap<>();

        static {
            for (H5G_storage s : EnumSet.allOf(H5G_storage.class))
                lookup.put(s.getCode(), s);
        }

        private int code;

        H5G_storage(int layout_type) {
            this.code = layout_type;
        }

        public int getCode() {
            return this.code;
        }

        public static H5G_storage get(int code) {
            return lookup.get(code);
        }
    }

    private static void CreateGroup() {
        H5File         file = null;
        H5Group        grp = null;
        long            group_id = -1;
        long            fapl_id = -1;
        long            gcpl_id = -1;
        H5G_info_t     ginfo;
        String         name = "G0";    // Name of subgroup_id
        int            i;

        //Set file access property list to allow the latest file format.This will allow the library to create new format groups.
        try {
            fapl_id = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
            if(fapl_id >= 0)
                H5.H5Pset_libver_bounds (fapl_id, HDF5Constants.H5F_LIBVER_LATEST, HDF5Constants.H5F_LIBVER_LATEST);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Create a new file using the default properties.
        try {
            if(fapl_id >= 0) {
                file = new H5File(FILE, FileFormat.CREATE);
                file.open(fapl_id);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Create primary group.
        try {
            //Create group creation property list and set the phase change conditions.
            gcpl_id = file.createGcpl(0, MAX_COMPACT, MIN_DENSE);
            if(gcpl_id >= 0) {
                grp = (H5Group) file.createGroup(name, null, HDF5Constants.H5P_DEFAULT, gcpl_id);
                gcpl_id = -1; //Create closes the group creation property list
                group_id = grp.open();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Add subgroups to "group" one at a time, print the storage type for "group" after each subgroup is created.
        for (i = 1; i <= MAX_GROUPS; i++) {
            //Define the subgroup name and create the subgroup.
            char append = (char) (((char)i) + '0');
            name = name + append; /* G1, G2, G3 etc. */
            try {
                file.createGroup(name, grp);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            //Obtain the group info and print the group storage type
            try {
                if(group_id >= 0) {
                    ginfo = H5.H5Gget_info (group_id);
                    System.out.print(ginfo.nlinks + " Group"+(ginfo.nlinks == 1 ? " " : "s") + ": Storage type is ");
                    switch (H5G_storage.get(ginfo.storage_type)) {
                    case H5G_STORAGE_TYPE_COMPACT:
                        System.out.println("H5G_STORAGE_TYPE_COMPACT"); // New compact format
                        break;
                    case H5G_STORAGE_TYPE_DENSE:
                        System.out.println("H5G_STORAGE_TYPE_DENSE"); //New dense (indexed) format
                        break;
                    case H5G_STORAGE_TYPE_SYMBOL_TABLE:
                        System.out.println("H5G_STORAGE_TYPE_SYMBOL_TABLE"); //Original format
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println();

        //Delete subgroups one at a time, print the storage type for "group" after each subgroup is deleted.
        for (i = MAX_GROUPS; i >= 1; i--) {
            //Define the subgroup name and delete the subgroup.
            try {
                Group subgrp = (Group) file.get("/G0/" + name);
                if(subgrp == null)
                    throw new HDF5Exception(
                            "The subgroup - "+name+" - object could not be found.");
                file.delete(subgrp);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            name = name.substring(0, i+1);

            //Obtain the group info and print the group storage type
            try {
                if(group_id >= 0){
                    ginfo = H5.H5Gget_info(group_id);
                    System.out.print(ginfo.nlinks + " Group"+(ginfo.nlinks == 1 ? " " : "s") + ": Storage type is ");
                    switch (H5G_storage.get(ginfo.storage_type)) {
                    case H5G_STORAGE_TYPE_COMPACT:
                        System.out.println("H5G_STORAGE_TYPE_COMPACT"); // New compact format
                        break;
                    case H5G_STORAGE_TYPE_DENSE:
                        System.out.println("H5G_STORAGE_TYPE_DENSE"); //New dense (indexed) format
                        break;
                    case H5G_STORAGE_TYPE_SYMBOL_TABLE:
                        System.out.println("H5G_STORAGE_TYPE_SYMBOL_TABLE"); //Original format
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        //Close and release resources
        try {
            if(fapl_id >= 0)
                H5.H5Pclose (fapl_id);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        //Close the group
        try {
            if(group_id >= 0)
                grp.close (group_id);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        //Close the file
        try {
            file.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        H5ObjectEx_G_Phase.CreateGroup();
    }
}
