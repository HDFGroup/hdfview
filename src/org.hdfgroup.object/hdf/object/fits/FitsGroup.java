/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the COPYING file, which can be found  *
 * at the root of the source code distribution tree,                         *
 * or in https://www.hdfgroup.org/licenses.                                  *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.object.fits;

import java.util.List;

import hdf.object.FileFormat;
import hdf.object.Group;

/**
 * An H5Group represents HDF5 group, inheriting from Group.
 * Every HDF5 object has at least one name. An HDF5 group is used to store
 * a set of the names together in one place, i.e. a group. The general
 * structure of a group is similar to that of the UNIX file system in
 * that the group may contain references to other groups or data objects
 * just as the UNIX directory may contain subdirectories or files.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class FitsGroup extends Group
{
    private static final long serialVersionUID = 4993155577664991838L;

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of AttributeDatset.
     */
    private List attributeList;

    /** The default object ID for HDF5 objects */
    private static final long[] DEFAULT_OID = {0};

    /**
     * Constructs an HDF5 group with specific name, path, and parent.
     *
     * @param fileFormat the file which containing the group.
     * @param name the name of this group.
     * @param path the full path of this group.
     * @param parent the parent of this group.
     * @param theID the unique identifier of this data object.
     */
    public FitsGroup(FileFormat fileFormat, String name, String path, Group parent, long[] theID) {
        super (fileFormat, name, path, parent, ((theID == null) ? DEFAULT_OID : theID));
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.DataFormat#hasAttribute()
     */
    public boolean hasAttribute () {
        return false;
    }

    // Implementing DataFormat
    /**
     * Retrieves the object's metadata, such as attributes, from the file.
     *
     * Metadata, such as attributes, is stored in a List.
     *
     * @return the list of metadata objects.
     *
     * @throws Exception
     *             if the metadata can not be retrieved
     */
    public List getMetadata() throws Exception {
        if (!isRoot())
            return null; // there is only one group in the file: the root

        if (attributeList != null)
            return attributeList;

        return attributeList;
    }

    /**
     * Writes a specific piece of metadata (such as an attribute) into the file.
     *
     * If an HDF(4&amp;5) attribute exists in the file, this method updates its
     * value. If the attribute does not exist in the file, it creates the
     * attribute in the file and attaches it to the object. It will fail to
     * write a new attribute to the object where an attribute with the same name
     * already exists. To update the value of an existing attribute in the file,
     * one needs to get the instance of the attribute by getMetadata(), change
     * its values, then use writeMetadata() to write the value.
     *
     * @param info
     *            the metadata to write.
     *
     * @throws Exception
     *             if the metadata can not be written
     */
    public void writeMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for FITS.");
    }

    /**
     * Deletes an existing piece of metadata from this object.
     *
     * @param info
     *            the metadata to delete.
     *
     * @throws Exception
     *             if the metadata can not be removed
     */
    public void removeMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for FITS.");
    }

    /**
     * Updates an existing piece of metadata attached to this object.
     *
     * @param info
     *            the metadata to update.
     *
     * @throws Exception
     *             if the metadata can not be updated
     */
    public void updateMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for FITS.");
    }

    // Implementing DataFormat
    @Override
    public long open() {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for FITS.");
    }

    /**
     *  close group access
     *
     * @param gid
     *        the group identifier
     */
    @Override
    public void close(long gid) {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for FITS.");
    }

    /**
     * Creates a new group.
     *
     * @param name
     *        the name of the group to create.
     * @param pgroup
     *        the parent group of the new group.
     *
     * @return the new group if successful. Otherwise returns null.
     *
     * @throws Exception
     *            if there is an error
     */
    public static FitsGroup create(String name, Group pgroup) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for FITS.");
    }

    //Implementing DataFormat
    /**
     * Retrieves the object's metadata, such as attributes, from the file.
     *
     * Metadata, such as attributes, is stored in a List.
     *
     * @param attrPropList
     *             the list of properties to get
     *
     * @return the list of metadata objects.
     *
     * @throws Exception
     *             if the metadata can not be retrieved
     */
    public List getMetadata(int... attrPropList) throws Exception {
        throw new UnsupportedOperationException("getMetadata(int... attrPropList) is not supported");
    }

}
