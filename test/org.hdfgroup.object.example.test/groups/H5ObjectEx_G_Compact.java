package groups;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.object.FileFormat;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

public class H5ObjectEx_G_Compact {

    private static final String FILE1 = "H5ObjectEx_G_Compact1.h5";
    private static final String FILE2 = "H5ObjectEx_G_Compact2.h5";
    private static final String GROUP = "G1";

    enum H5G_storage {
        H5G_STORAGE_TYPE_UNKNOWN(-1),
        H5G_STORAGE_TYPE_SYMBOL_TABLE(0),
        H5G_STORAGE_TYPE_COMPACT(1),
        H5G_STORAGE_TYPE_DENSE(2);

        private static final Map<Integer, H5G_storage> lookup = new HashMap<Integer, H5G_storage>();

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

    public static void CreateGroup() {
        H5File      file = null;
        H5Group     grp = null;
        long         file_id = -1;
        long         group_id = -1;
        long         fapl_id = -1;
        H5G_info_t  ginfo;
        long        size;

        //Create file 1.  This file will use original format groups.
        try {
            file = new H5File(FILE1, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //Create a group in the file1.
        try {
            grp = (H5Group)file.createGroup(GROUP, null);
            group_id = grp.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Obtain the group info and print the group storage type.
        try {
            if(group_id >= 0) {
                ginfo = H5.H5Gget_info(group_id);
                System.out.print("Group storage type for " + FILE1 + " is: ");
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

        // Close the group.
        try {
            if (group_id >= 0)
                grp.close(group_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //close the file 1.
        try {
            file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Re-open file 1.  Need to get the correct file size.
        try {
            file = new H5File(FILE1, FileFormat.READ);
            file_id = file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Obtain and print the file size.
        try {
            if(file_id >= 0) {
                size = H5.H5Fget_filesize (file_id);
                System.out.println ("File size for " + FILE1 + " is: "  +  size + " bytes" );
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        //Close FILE1.
        try {
            file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        //Set file access property list to allow the latest file format.
        // This will allow the library to create new compact format groups.
        try {
            fapl_id = H5.H5Pcreate (HDF5Constants.H5P_FILE_ACCESS);
            if(fapl_id >= 0)
                H5.H5Pset_libver_bounds (fapl_id, HDF5Constants.H5F_LIBVER_LATEST, HDF5Constants.H5F_LIBVER_LATEST);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
        //Create file 2 using the new file access property list.
        try {
            file = new H5File(FILE2, FileFormat.CREATE);
            file.open(fapl_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //Create group in file2.
        try {
            grp = (H5Group)file.createGroup(GROUP, null);
            group_id = grp.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Obtain the group info and print the group storage type.
        try {
            if(group_id >= 0) {
                ginfo = H5.H5Gget_info(group_id);
                System.out.print("Group storage type for " + FILE2 + " is: ");
                switch (H5G_storage.get(ginfo.storage_type)) {
                case H5G_STORAGE_TYPE_COMPACT:
                    System.out.println("H5G_STORAGE_TYPE_COMPACT"); //New compact format
                    break;
                case H5G_STORAGE_TYPE_DENSE:
                    System.out.println("H5G_STORAGE_TYPE_DENSE"); // New dense (indexed) format
                    break;
                case H5G_STORAGE_TYPE_SYMBOL_TABLE:
                    System.out.println("H5G_STORAGE_TYPE_SYMBOL_TABLE"); // Original format
                }
            }
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

        //close the file 2.
        try {
            file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Re-open file 2.  Needed to get the correct file size.
        try {
            file = new H5File(FILE2, FileFormat.READ);
            file_id = file.open(fapl_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Obtain and print the file size.
        try {
            if(file_id >= 0) {
                size = H5.H5Fget_filesize (file_id);
                System.out.println ("File size for " + FILE2 + " is: "  +  size + " bytes");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Close FILE2.
        try {
            file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        H5ObjectEx_G_Compact.CreateGroup();
    }
}
