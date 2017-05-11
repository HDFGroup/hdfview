/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.object;

import java.io.Serializable;

/**
 * The HObject class is the root class of all the HDF data objects. Every data
 * class has HObject as a superclass. All objects implement the methods of this
 * class. The following is the inherited structure of HDF Objects.
 *
 * <pre>
 *                                 HObject
 *          __________________________|________________________________
 *          |                         |                               |
 *        Group                    Dataset                        Datatype
 *          |                _________|___________                    |
 *          |                |                   |                    |
 *          |             ScalarDS          CompoundDS                |
 *          |                |                   |                    |
 *    ---------------------Implementing classes such as-------------------------
 *      ____|____       _____|______        _____|_____          _____|_____
 *      |       |       |          |        |         |          |         |
 *   H5Group H4Group H5ScalarDS H4ScalarDS H5CompDS H4CompDS H5Datatype H4Datatype
 *
 * </pre>
 *
 * All HDF4 and HDF5 data objects are inherited from HObject. At the top level
 * of the hierarchy, both HDF4 and HDF5 have the same super-classes, such as
 * Group and Dataset. At the bottom level of the hierarchy, HDF4 and HDF5
 * objects have their own implementation, such as H5Group, H5ScalarDS,
 * H5CompoundDS, and H5Datatype.
 * <p>
 * <b>Warning: HDF4 and HDF5 may have multiple links to the same object. Data
 * objects in this model do not deal with multiple links. Users may create
 * duplicate copies of the same data object with different paths. Applications
 * should check the OID of the data object to avoid duplicate copies of the same
 * object.</b>
 * <p>
 * HDF4 objects are uniquely identified by the OID (tag_id, ref_id) pair.
 * The ref_id is the object reference count. The tag_id is a pre-defined number
 * to identify the type of object. For example, DFTAG_RI is for raster image,
 * DFTAG_SD is for scientific dataset, and DFTAG_VG is for Vgroup.
 * <p>
 * HDF5 objects are uniquely identified by the OID containing just the object
 * reference. The OID is usually obtained by H5Rcreate(). The following example
 * shows how to retrieve an object ID from a file:
 *
 * <pre>
 * // retrieve the object ID
 * try {
 *     byte[] ref_buf = H5.H5Rcreate(h5file.getFID(), this.getFullName(), HDF5Constants.H5R_OBJECT, -1);
 *     long[] oid = new long[1];
 *     oid[0] = HDFNativeData.byteToLong(ref_buf, 0);
 * }
 * catch (Exception ex) {
 * }
 * </pre>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 * @see <a href="DataFormat.html">hdf.object.DataFormat</a>
 */
public abstract class HObject implements Serializable, DataFormat {
    /**
     * The serialVersionUID is a universal version identifier for a Serializable
     * class. Deserialization uses this number to ensure that a loaded class
     * corresponds exactly to a serialized object. For details, see
     * http://java.sun.com/j2se/1.5.0/docs/api/java/io/Serializable.html
     */
    private static final long  serialVersionUID = -1723666708199882519L;

    /**
     * The separator of object path, i.e. "/".
     */
    public final static String separator = "/";

    /**
     * The full path of the file that contains the object.
     */
    private String             filename;

    /**
     * The file which contains the object
     */
    protected final FileFormat fileFormat;

    /**
     * The name of the data object. The root group has its default name, a
     * slash. The name can be changed except the root group.
     */
    private String             name;

    /**
     * The full path of the data object. The full path always starts with the
     * root, a slash. The path cannot be changed. Also, a path must be ended with a
     * slash. For example, /arrays/ints/
     */
    private String             path;

    /** The full name of the data object, i.e. "path + name" */
    private String             fullName;

    /**
     * Array of long integer storing unique identifier for the object.
     * <p>
     * HDF4 objects are uniquely identified by a (tag_id, ref_id) pair. i.e.
     * oid[0] = tag, oid[1] = ref_id.<br>
     * HDF5 objects are uniquely identified by an object reference. i.e.
     * oid[0] = obj_id.
     */
    protected long[]           oid;

    /**
     * The name of the Target Object that is being linked to.
     */
    protected String           linkTargetObjName;

    /**
     * Number of attributes attached to the object.
     */
    // protected int nAttributes = -1;

    /**
     * Constructs an instance of a data object without name and path.
     */
    public HObject() {
        this(null, null, null, null);
    }

    /**
     * Constructs an instance of a data object with specific name and path.
     * <p>
     * For example, in H5ScalarDS(h5file, "dset", "/arrays"), "dset" is the name
     * of the dataset, "/arrays" is the group path of the dataset.
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the group path of the data object, e.g. "/arrays".
     */
    public HObject(FileFormat theFile, String theName, String thePath) {
        this(theFile, theName, thePath, null);
    }

    /**
     * Constructs an instance of a data object with specific name and path.
     * <p>
     * For example, in H5ScalarDS(h5file, "dset", "/arrays"), "dset" is the name
     * of the dataset, "/arrays" is the group path of the dataset.
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the group path of the data object, e.g. "/arrays".
     * @param oid
     *            the ids of the data object.
     */
    @Deprecated
    public HObject(FileFormat theFile, String theName, String thePath, long[] oid) {
        this.fileFormat = theFile;
        this.oid = oid;

        if (fileFormat != null) {
            this.filename = fileFormat.getFilePath();
        }
        else {
            this.filename = null;
        }

        // file name is packed in the full path
        if ((theName == null) && (thePath != null)) {
            if (thePath.equals(separator)) {
                theName = separator;
                thePath = null;
            }
            else {
                // the path must starts with "/"
                if (!thePath.startsWith(HObject.separator)) {
                    thePath = HObject.separator + thePath;
                }

                // get rid of the last "/"
                if (thePath.endsWith(HObject.separator)) {
                    thePath = thePath.substring(0, thePath.length() - 1);
                }

                // separate the name and the path
                theName = thePath.substring(thePath.lastIndexOf(separator) + 1);
                thePath = thePath.substring(0, thePath.lastIndexOf(separator));
            }
        }
        else if ((theName != null) && (thePath == null) && (theName.indexOf(separator) >= 0)) {
            if (theName.equals(separator)) {
                theName = separator;
                thePath = null;
            }
            else {
                // the full name must starts with "/"
                if (!theName.startsWith(separator)) {
                    theName = separator + theName;
                }

                // the fullname must not end with "/"
                int n = theName.length();
                if (theName.endsWith(separator)) {
                    theName = theName.substring(0, n - 1);
                }

                int idx = theName.lastIndexOf(separator);
                if (idx < 0) {
                    thePath = separator;
                }
                else {
                    thePath = theName.substring(0, idx);
                    theName = theName.substring(idx + 1);
                }
            }
        }

        // the path must start and end with "/"
        if (thePath != null) {
            thePath = thePath.replaceAll("//", "/");
            if (!thePath.endsWith(separator)) {
                thePath += separator;
            }
        }

        this.name = theName;
        this.path = thePath;

        if (thePath != null) {
            this.fullName = thePath + theName;
        }
        else {
            if (theName == null) {
                this.fullName = "/";
            }
            else if (theName.startsWith("/")) {
                this.fullName = theName;
            }
            else {
                this.fullName = "/" + theName;
            }
        }
    }

    /**
     * Print out debug information
     * <p>
     *
     * @param msg
     *            the debug message to print
     */
    protected final void debug(Object msg) {
        System.out.println("*** " + this.getClass().getName() + ": " + msg);
    }

    /**
     * Returns the name of the file that contains this data object.
     * <p>
     * The file name is necessary because the file of this data object is
     * uniquely identified when multiple files are opened by an application at
     * the same time.
     *
     * @return The full path (path + name) of the file.
     */
    public final String getFile() {
        return filename;
    }

    /**
     * Returns the name of the object. For example, "Raster Image #2".
     *
     * @return The name of the object.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the name of the target object that is linked to.
     *
     * @return The name of the object that is linked to.
     */
    public final String getLinkTargetObjName() {
        return linkTargetObjName;
    }

    /**
     * Sets the name of the target object that is linked to.
     *
     * @param targetObjName
     *            The new name of the object.
     */
    public final void setLinkTargetObjName(String targetObjName) {
        linkTargetObjName = targetObjName;
    }

    /**
     * Returns the full name (group path + object name) of the object. For
     * example, "/Images/Raster Image #2"
     *
     * @return The full name (group path + object name) of the object.
     */
    public final String getFullName() {
        return fullName;
    }

    /**
     * Returns the group path of the object. For example, "/Images".
     *
     * @return The group path of the object.
     */
    public final String getPath() {
        return path;
    }

    /**
     * Sets the name of the object.
     *
     * setName (String newName) changes the name of the object in the file.
     *
     * @param newName
     *            The new name of the object.
     *
     * @throws Exception if name is root or contains separator
     */
    public void setName(String newName) throws Exception {
        if (newName != null) {
            if (newName.equals(HObject.separator)) {
                throw new IllegalArgumentException("The new name cannot be the root");
            }

            if (newName.startsWith(HObject.separator)) {
                newName = newName.substring(1);
            }

            if (newName.endsWith(HObject.separator)) {
                newName = newName.substring(0, newName.length() - 2);
            }

            if (newName.contains(HObject.separator)) {
                throw new IllegalArgumentException("The new name contains the separator character: "
                        + HObject.separator);
            }
        }

        name = newName;
    }

    /**
     * Sets the path of the object.
     * <p>
     * setPath() is needed to change the path for an object when the name of a
     * group containing the object is changed by setName(). The path of the
     * object in memory under this group should be updated to the new path to
     * the group. Unlike setName(), setPath() does not change anything in file.
     *
     * @param newPath
     *            The new path of the object.
     *
     * @throws Exception if a failure occurred
     */
    public void setPath(String newPath) throws Exception {
        if (newPath == null) {
            newPath = "/";
        }

        path = newPath;
    }

    /**
     * Opens an existing object such as a dataset or group for access.
     *
     * The return value is an object identifier obtained by implementing classes
     * such as H5.H5Dopen(). This function is needed to allow other objects to
     * be able to access the object. For instance, H5File class uses the open()
     * function to obtain object identifier for copyAttributes(long src_id, long
     * dst_id) and other purposes. The open() function should be used in pair
     * with close(long) function.
     *
     * @see hdf.object.HObject#close(long)
     *
     * @return the object identifier if successful; otherwise returns a negative
     *         value.
     */
    public abstract long open();

    /**
     * Closes access to the object.
     * <p>
     * Sub-classes must implement this interface because different data objects
     * have their own ways of how the data resources are closed.
     * <p>
     * For example, H5Group.close() calls the hdf.hdf5lib.H5.H5Gclose()
     * method and closes the group resource specified by the group id.
     *
     * @param id
     *            The object identifier.
     */
    public abstract void close(long id);

    /**
     * Returns the file identifier of of the file containing the object.
     *
     * @return the file identifier of of the file containing the object.
     */
    public final long getFID() {
        if (fileFormat != null) {
            return fileFormat.getFID();
        }
        else {
            return -1;
        }
    }

    /**
     * Checks if the OID of the object is the same as the given object
     * identifier within the same file.
     * <p>
     * HDF4 and HDF5 data objects are identified by their unique OIDs. A data
     * object in a file may have multiple logical names , which are represented
     * in a graph structure as separate objects.
     * <p>
     * The HObject.equalsOID(long[] theID) can be used to check if two data
     * objects with different names are pointed to the same object within the
     * same file.
     *
     * @param theID
     *            The list object identifiers.
     *
     * @return true if the ID of the object equals the given OID; otherwise,
     *         returns false.
     */
    public final boolean equalsOID(long[] theID) {
        if ((theID == null) || (oid == null)) {
            return false;
        }

        int n1 = theID.length;
        int n2 = oid.length;

        if (n1 == 0 || n2 == 0) {
            return false;
        }

        int n = Math.min(n1, n2);
        boolean isMatched = (theID[0] == oid[0]);

        for (int i = 1; isMatched && (i < n); i++) {
            isMatched = (theID[i] == oid[i]);
        }

        return isMatched;
    }

    /**
     * Returns the file that contains the object.
     *
     * @return The file that contains the object.
     */
    public final FileFormat getFileFormat() {
        return fileFormat;
    }

    /**
     * Returns a cloned copy of the object identifier.
     * <p>
     * The object OID cannot be modified once it is created. getOID() clones the
     * object OID to ensure the object OID cannot be modified outside of this
     * class.
     *
     * @return the cloned copy of the object OID.
     */
    public final long[] getOID() {
        if (oid == null) {
            return null;
        }

        return oid.clone();
    }

    /**
     * Returns whether this HObject is equal to the specified HObject
     * by comparing their OIDs.
     *
     * @param obj
     *            The object
     *
     * @return true if the object is equal by OID
     */
    public boolean equals(HObject obj) {
        return this.equalsOID(obj.getOID());
    }

    /**
     * Returns the name of the object.
     * <p>
     * This method overwrites the toString() method in the Java Object class
     * (the root class of all Java objects) so that it returns the name of the
     * HObject instead of the name of the class.
     * <p>
     * For example, toString() returns "Raster Image #2" instead of
     * "hdf.object.h4.H4SDS".
     *
     * @return The name of the object.
     */
    @Override
    public String toString() {
        if (this instanceof Group) {
            if (((Group) this).isRoot() && this.getFileFormat() != null) return this.getFileFormat().getName();
        }

        if (name != null) return name;

        return super.toString();
    }
}
