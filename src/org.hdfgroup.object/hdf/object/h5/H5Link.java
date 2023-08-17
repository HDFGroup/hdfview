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

package hdf.object.h5;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFArray;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.hdf5lib.structs.H5O_token_t;

import hdf.object.Attribute;
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

    private static final Logger log = LoggerFactory.getLogger(H5Link.class);

    /** the object properties */
    private H5O_info_t objInfo;

    /**
     * Constructs an HDF5 link with specific name, path, and parent.
     *
     * @param theFile
     *            the file which containing the link.
     * @param theName
     *            the name of this link, e.g. "link1".
     * @param thePath
     *            the full path of this link, e.g. "/groups/".
     */
    public H5Link(FileFormat theFile, String theName, String thePath) {
        this(theFile, theName, thePath, null);
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

        if (theFile != null) {
            if (oid == null) {
                // retrieve the object ID
                byte[] refBuf = null;
                try {
                    refBuf = H5.H5Rcreate_object(theFile.getFID(), this.getFullName(), HDF5Constants.H5P_DEFAULT);
                    this.oid = HDFNativeData.byteToLong(refBuf);
                    log.trace("constructor REF {} to OID {}", refBuf, this.oid);
                }
                catch (Exception ex) {
                    log.debug("constructor ID {} for {} failed H5Rcreate_object", theFile.getFID(), this.getFullName());
                }
                finally {
                    if (refBuf != null)
                        H5.H5Rdestroy(refBuf);
                }
            }
            log.trace("constructor OID {}", this.oid);
            try {
                objInfo = H5.H5Oget_info_by_name(theFile.getFID(), this.getFullName(), HDF5Constants.H5O_INFO_BASIC, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                objInfo = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
            }
        }
        else {
            this.oid = null;
            objInfo = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long id) {
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
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attributes, false otherwise.
     */
    @Override
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
     * Retrieves the object's metadata, such as attributes, from the file.
     *
     * Metadata, such as attributes, is stored in a List.
     *
     * @return the list of metadata objects.
     *
     * @throws HDF5Exception
     *             if the metadata can not be retrieved
     */
    @Override
    public List<Attribute> getMetadata() throws HDF5Exception {
        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        return null;
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
     * @throws HDF5Exception
     *             if the metadata can not be retrieved
     */
    public List<Attribute> getMetadata(int... attrPropList) throws HDF5Exception {
        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        return null;
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
    @Override
    public void writeMetadata(Object info) throws Exception {
    }

    /**
     * Deletes an existing piece of metadata from this object.
     *
     * @param info
     *            the metadata to delete.
     *
     * @throws HDF5Exception
     *             if the metadata can not be removed
     */
    @Override
    public void removeMetadata(Object info) throws HDF5Exception {
    }

    /**
     * Updates an existing piece of metadata attached to this object.
     *
     * @param info
     *            the metadata to update.
     *
     * @throws HDF5Exception
     *             if the metadata can not be updated
     */
    @Override
    public void updateMetadata(Object info) throws HDF5Exception {
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
