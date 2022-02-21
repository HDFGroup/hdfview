/************************************************************
  This example shows how to read and write object references
  to a dataset.  The program first creates objects in the
  file and writes references to those objects to a dataset
  with a dataspace of DIM0, then closes the file.  Next, it
  reopens the file, dereferences the references, and outputs
  the names of their targets to the screen.
 ************************************************************/

package datatypes;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;
import hdf.object.h5.H5ReferenceType;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_T_ObjectReference {
    private static String FILENAME = "H5ObjectEx_T_ObjectReference.h5";
    private static String DATASETNAME = "DS1";
    private static String DATASETNAME2 = "DS2";
    private static String GROUPNAME = "G1";
    private static final int DIM0 = 2;
    private static final int RANK = 1;

    // Values for the status of object type
    enum H5O_TYPE_obj {
        H5O_TYPE_UNKNOWN(HDF5Constants.H5O_TYPE_UNKNOWN), /* Unknown object type */
        H5O_TYPE_GROUP(HDF5Constants.H5O_TYPE_GROUP), /* Object is a group */
        H5O_TYPE_DATASET(HDF5Constants.H5O_TYPE_DATASET), /* Object is a dataset */
        H5O_TYPE_NAMED_DATATYPE(HDF5Constants.H5O_TYPE_NAMED_DATATYPE); /* Object is a named data type */
        private static final Map<Integer, H5O_TYPE_obj> lookup = new HashMap<>();

        static {
            for (H5O_TYPE_obj s : EnumSet.allOf(H5O_TYPE_obj.class))
                lookup.put(s.getCode(), s);
        }

        private int code;

        H5O_TYPE_obj(int layout_type) {
            this.code = layout_type;
        }

        public int getCode() {
            return this.code;
        }

        public static H5O_TYPE_obj get(int code) {
            return lookup.get(code);
        }
    }

    private static void writeObjRef() {
        H5File file = null;
        long file_id = HDF5Constants.H5I_INVALID_HID;
        H5ScalarDS dset = null;
        H5ScalarDS dset2 = null;
        H5Group grp = null;
        long[] dims = { DIM0 };
        byte[][] dset_data = new byte[DIM0][HDF5Constants.H5R_REF_BUF_SIZE];
        H5Datatype typeInt = null;
        H5Datatype typeRef = null;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file_id = file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the base datatypes.
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, 8, Datatype.ORDER_BE, Datatype.NATIVE);
            typeRef = new H5ReferenceType(Datatype.CLASS_REFERENCE, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create dataset with a scalar dataspace.
        try {
            dset2 = (H5ScalarDS) file.createScalarDS(DATASETNAME2, null, typeInt, dims, null, null, 0, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create a group in the file.
        try {
            grp = (H5Group) file.createGroup("/" + GROUPNAME, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            try {
                dset_data[0] = H5.H5Rcreate_object(file_id, "/" + GROUPNAME, HDF5Constants.H5P_DEFAULT);
            }
            catch (Throwable err) {
                err.printStackTrace();
            }

            try {
                dset_data[1] = H5.H5Rcreate_object(file_id, DATASETNAME2, HDF5Constants.H5P_DEFAULT);
            }
            catch (Throwable err) {
                err.printStackTrace();
            }

            // Create the dataset.
            try {
                dset = (H5ScalarDS) file.createScalarDS(DATASETNAME, null, typeRef, dims, null, null, 0, dset_data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {H5.H5Rdestroy(dset_data[1]);} catch (Exception ex) {}
            try {H5.H5Rdestroy(dset_data[0]);} catch (Exception ex) {}
        }

        // Close the file.
        try {
            file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readObjRef() {
        H5File file = null;
        H5ScalarDS dset = null;
        long dataspace_id = HDF5Constants.H5I_INVALID_HID;
        long dataset_id = HDF5Constants.H5I_INVALID_HID;
        int object_type = -1;
        long object_id = HDF5Constants.H5I_INVALID_HID;
        long[] dims = { DIM0 };
        byte[][] dset_data = new byte[DIM0][HDF5Constants.H5R_REF_BUF_SIZE];

        // Open an existing file.
        try {
            file = new H5File(FILENAME, FileFormat.READ);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Open an existing dataset.
        try {
            dset = (H5ScalarDS) file.get(DATASETNAME);
            dataset_id = dset.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Get dataspace and allocate memory for read buffer.
        try {
            if (dataset_id >= 0)
                dataspace_id = H5.H5Dget_space(dataset_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (dataspace_id >= 0)
                H5.H5Sget_simple_extent_dims(dataspace_id, dims, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Read data.
        try {
            if (dataset_id >= 0) {
                H5.H5Dread(dataset_id, HDF5Constants.H5T_STD_REF, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                        HDF5Constants.H5P_DEFAULT, dset_data);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        for (int indx = 0; indx < dims[0]; indx++) {
            System.out.println(DATASETNAME + "[" + indx + "]:");
            System.out.print("  ->");
            // Open the referenced object, get its name and type.
            try {
                if (dataset_id >= 0) {
                    object_id = H5.H5Ropen_object(dset_data[indx], HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                    object_type = H5.H5Rget_obj_type(dataset_id, HDF5Constants.H5R_OBJECT, dset_data[indx]);
                }
                String obj_name = null;
                if (object_type >= 0) {
                    // Get the name.
                    obj_name = H5.H5Iget_name(object_id);
                }
                if ((object_id >= 0) && (object_type >= -1)) {
                    switch (H5O_TYPE_obj.get(object_type)) {
                    case H5O_TYPE_GROUP:
                        System.out.print("H5O_TYPE_GROUP");
                        break;
                    case H5O_TYPE_DATASET:
                        System.out.print("H5O_TYPE_DATASET");
                        break;
                    case H5O_TYPE_NAMED_DATATYPE:
                        System.out.print("H5O_TYPE_NAMED_DATATYPE");
                        break;
                    default:
                        System.out.print("UNHANDLED");
                    }
                }
                // Print the name.
                System.out.println(": " + obj_name);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                try {H5.H5Oclose(object_id);} catch (Exception ex) {}
            }
        }

        // End access to the dataset and release resources used by it.
        try {
            if (dataspace_id >= 0)
                H5.H5Sclose(dataspace_id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (dataset_id >= 0)
                dset.close(dataset_id);
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
        H5ObjectEx_T_ObjectReference.writeObjRef();
        H5ObjectEx_T_ObjectReference.readObjRef();
    }

}
