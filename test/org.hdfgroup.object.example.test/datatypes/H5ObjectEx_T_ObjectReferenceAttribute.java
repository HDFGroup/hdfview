/************************************************************
  This example shows how to read and write object references
  to an attribute.  The program first creates objects in the
  file and writes references to those objects to an
  attribute with a dataspace of DIM0, then closes the file.
  Next, it reopens the file, dereferences the references,
  and outputs the names of their targets to the screen.
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
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5ScalarDS;

public class H5ObjectEx_T_ObjectReferenceAttribute {
    private static String FILENAME = "H5ObjectEx_T_ObjectReferenceAttribute.h5";
    private static String DATASETNAME = "DS1";
    private static String ATTRIBUTENAME = "A1";
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
        H5ScalarDS dset = null;
        H5ScalarDS dset2 = null;
        H5Group grp = null;
        long[] dims = { DIM0 };
        long[] dset_data = new long[DIM0];
        H5Datatype typeInt = null;
        H5Datatype typeRef = null;

        // Create a new file using default properties.
        try {
            file = new H5File(FILENAME, FileFormat.CREATE);
            file.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the base datatypes.
        try {
            typeInt = new H5Datatype(Datatype.CLASS_INTEGER, 8, Datatype.ORDER_BE, Datatype.NATIVE);
            typeRef = new H5Datatype(Datatype.CLASS_REFERENCE, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
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

        // Create references to the previously created objects. Passing -1
        // as space_id causes this parameter to be ignored. Other values
        // besides valid dataspaces result in an error.
        try {
            dset_data[0] = grp.getOID()[0];
            dset_data[1] = dset2.getOID()[0];
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create dataset with a scalar dataspace to serve as the parent
        // for the attribute.
        try {
            dset = (H5ScalarDS) file.createScalarDS(DATASETNAME, null, typeInt, dims, null, null, 0, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Create the attribute and write the array data to it.
        try {
            H5ScalarAttr attr = new H5ScalarAttr(dset, ATTRIBUTENAME, typeRef, dims);
            attr.setAttributeData(dset_data);
            file.writeAttribute(dset, attr, false);
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

    private static void readObjRef() {
        H5File file = null;
        H5ScalarDS dset = null;
        long dataspace_id = -1;
        long dataset_id = -1;
        long attribute_id = -1;
        int object_type = -1;
        long object_id = -1;
        long[] dims = { DIM0 };
        byte[][] dset_data;

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

        try {
            if (dataset_id >= 0)
                attribute_id = H5.H5Aopen_by_name(dataset_id, ".", ATTRIBUTENAME, HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Get dataspace and allocate memory for read buffer.
        try {
            if (attribute_id >= 0)
                dataspace_id = H5.H5Aget_space(attribute_id);
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

        // Allocate array of pointers to two-dimensional arrays (the
        // elements of the dataset.
        dset_data = new byte[(int) dims[0]][8];

        // Read data.
        try {
            if (attribute_id >= 0)
                H5.H5Aread(attribute_id, HDF5Constants.H5T_STD_REF_OBJ, dset_data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Output the data to the screen.
        for (int indx = 0; indx < dims[0]; indx++) {
            System.out.println(ATTRIBUTENAME + "[" + indx + "]:");
            System.out.print("  ->");
            // Open the referenced object, get its name and type.
            try {
                if (dataset_id >= 0) {
                    object_id = H5.H5Rdereference(dataset_id, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5R_OBJECT, dset_data[indx]);
                    object_type = H5.H5Rget_obj_type(dataset_id, HDF5Constants.H5R_OBJECT, dset_data[indx]);
                }
                String obj_name = null;
                if (object_type >= 0) {
                    // Get the length of the name and retrieve the name.
                    obj_name = H5.H5Iget_name(object_id);
                }
                if ((object_id >= 0) && (object_type >= -1)) {
                    switch (H5O_TYPE_obj.get(object_type)) {
                        case H5O_TYPE_GROUP:
                            System.out.print("H5O_TYPE_GROUP");
                            try {
                                if (object_id >= 0)
                                    H5.H5Gclose(object_id);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case H5O_TYPE_DATASET:
                            System.out.print("H5O_TYPE_DATASET");
                            try {
                                if (object_id >= 0)
                                    H5.H5Dclose(object_id);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case H5O_TYPE_NAMED_DATATYPE:
                            System.out.print("H5O_TYPE_NAMED_DATATYPE");
                            try {
                                if (object_id >= 0)
                                    H5.H5Tclose(object_id);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
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
        }

        // End access to the dataset and release resources used by it.
        try {
            if (attribute_id >= 0)
                H5.H5Aclose(attribute_id);
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

        // Terminate access to the data space.
        try {
            if (dataspace_id >= 0)
                H5.H5Sclose(dataspace_id);
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
        H5ObjectEx_T_ObjectReferenceAttribute.writeObjRef();
        H5ObjectEx_T_ObjectReferenceAttribute.readObjRef();
    }

}
