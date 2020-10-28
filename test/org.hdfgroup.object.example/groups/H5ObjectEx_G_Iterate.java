/************************************************************
  This example shows how to iterate over group members using
  H5Gget_obj_info_all.
 ************************************************************/
package groups;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

public class H5ObjectEx_G_Iterate {
    private static String FILENAME = "examples/groups/h5ex_g_iterate.h5";
    private static String DATASETNAME = "/";

    enum H5O_type {
        H5O_TYPE_UNKNOWN(-1), // Unknown object type
        H5O_TYPE_GROUP(0), // Object is a group
        H5O_TYPE_DATASET(1), // Object is a dataset
        H5O_TYPE_NAMED_DATATYPE(2), // Object is a named data type
        H5O_TYPE_NTYPES(3); // Number of different object types
        private static final Map<Integer, H5O_type> lookup = new HashMap<>();

        static {
            for (H5O_type s : EnumSet.allOf(H5O_type.class))
                lookup.put(s.getCode(), s);
        }

        private int code;

        H5O_type(int layout_type) {
            this.code = layout_type;
        }

        public int getCode() {
            return this.code;
        }

        public static H5O_type get(int code) {
            return lookup.get(code);
        }
    }

    private static void do_iterate() {
        H5File      file = null;
        long         o_id = -1;

        // Open a file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.READ);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Begin iteration.
        System.out.println("Objects in root group:");
        try {
            int objType;
            H5O_info_t  info;
            HObject obj = null;
            H5Group grp = (H5Group)file.get(DATASETNAME);
            List<HObject> memberList = grp.getMemberList();
            Iterator<HObject> it = memberList.iterator();
            while (it.hasNext()) {
                obj = it.next();
                o_id = obj.open();
                if (o_id >= 0) {
                    info = H5.H5Oget_info(o_id);
                    objType = info.type;

                    // Get type of the object and display its name and type.
                    switch (H5O_type.get(objType)) {
                    case H5O_TYPE_GROUP:
                        System.out.println("  Group: " + obj.getName());
                        break;
                    case H5O_TYPE_DATASET:
                        System.out.println("  Dataset: " + obj.getName());
                        break;
                    case H5O_TYPE_NAMED_DATATYPE:
                        System.out.println("  Datatype: " + obj.getName());
                        break;
                    default:
                        System.out.println("  Unknown: " + obj.getName());
                    }
                    obj.close(o_id);
                }
            }
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
        H5ObjectEx_G_Iterate.do_iterate();
    }
}
