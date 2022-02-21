/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see https://support.hdfgroup.org/products/licenses.html               *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.object.h5;

import java.util.List;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFArray;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.hdf5lib.structs.H5O_token_t;

import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;

/**
 * An H5Link object represents an existing HDF5 object in file.
 *
 * H5Link object is an HDF5 object that is either a soft or an external link to
 * an object in a file that does not exist. The type of the object is unknown.
 * Once the object being linked to is created, and the type is known, then
 * H5link object will change its type.
 *
 * @version 2.7.2 7/6/2010
 * @author Nidhi Gupta
 */

public class H5Link extends HObject implements MetaDataContainer
{
    private static final long serialVersionUID = -8137277460521594367L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5Link.class);

    /** the object properties */
    @SuppressWarnings("unused")
    private H5O_info_t objInfo;

    /**
     * Constructs an HDF5 link with specific name, path, and parent.
     *
     * @param theFile
     *            the file which containing the link.
     * @param name
     *            the name of this link, e.g. "link1".
     * @param path
     *            the full path of this link, e.g. "/groups/".
     */
    public H5Link(FileFormat theFile, String name, String path) {
        this (theFile, name, path, null);
    }

    /**
     * Constructs an HDF5 link with specific name, path, parent and oid.
     *
     * @param theFile
     *            the file which containing the link.
     * @param theName
     *            the name of this link, e.g. "link1".
     * @param thePath
     *            the full path of this link, e.g. "/groups/".
     * @param oid
     *            the oid of this link, e.g. "/groups/".
     */
    @SuppressWarnings("deprecation")
    public H5Link(FileFormat theFile, String theName, String thePath, long[] oid) {
        super(theFile, theName, thePath, oid);

        if ((oid == null) && (theFile != null)) {
            // retrieve the object ID
            try {
                byte[] refBuf = H5.H5Rcreate_object(theFile.getFID(), this.getFullName(), HDF5Constants.H5P_DEFAULT);
                this.oid = HDFNativeData.byteToLong(refBuf);
                log.trace("constructor REF {} to OID {}", refBuf, this.oid);
            }
            catch (Exception ex) {
                log.debug("constructor ID {} for {} failed H5Rcreate_object", theFile.getFID(), this.getFullName());
            }
        }
        log.trace("constructor OID {}", this.oid);
        if (theFile != null) {
            try {
                objInfo = H5.H5Oget_info_by_name(theFile.getFID(), this.getFullName(), HDF5Constants.H5O_INFO_BASIC, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                objInfo = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
            }
        }
        else
            objInfo = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
    }

    protected void finalize() throws Throwable {
        // Invoke the finalizer of our superclass
        super.finalize();
        // Destroy the object reference we were using
        // If the file doesn't exist or tempfile is null, this can throw
        // an exception, but that exception is ignored.
        if (oid != null) {
            HDFArray theArray = new HDFArray(oid);
            byte[] refBuf = theArray.byteify();
            H5.H5Rdestroy(refBuf);
            oid = null;
        }
    }

    @Override
    public void close(long id) {
    }

    @Override
    public long open() {
        return 0;
    }

    /**
     * Get the token for this object.
     *
     * @return true if it has any attributes, false otherwise.
     */
    public long[] getToken() {
        H5O_token_t token = objInfo.token;
        return HDFNativeData.byteToLong(token.data);
    }

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
    @SuppressWarnings("rawtypes")
    public List getMetadata() throws Exception {

        try{
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch(Exception ex){
        }

        return null;
    }

    /**
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attributes, false otherwise.
     */
    public boolean hasAttribute() {
        return false;
    }

    /**
     * Removes all of the elements from metadata list.
     * The list should be empty after this call returns.
     */
    @Override
    public void clear() {
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
    }

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
    @SuppressWarnings("rawtypes")
    public List getMetadata(int... attrPropList) throws Exception {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#setName(java.lang.String)
     */
    @Override
    public void setName(String newName) throws Exception {
        if (newName == null)
            throw new IllegalArgumentException("The new name is NULL");

        H5File.renameObject(this, newName);
        super.setName(newName);
    }
}
