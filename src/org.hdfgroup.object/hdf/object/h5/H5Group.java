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
import java.util.Vector;

import hdf.object.Attribute;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5MetaDataContainer;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFArray;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.hdf5lib.structs.H5O_token_t;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An H5Group object represents an existing HDF5 group in file.
 *
 * In HDF5, every object has at least one name. An HDF5 group is used to store a set of the names together in
 * one place, i.e. a group. The general structure of a group is similar to that of the UNIX file system in
 * that the group may contain references to other groups or data objects just as the UNIX directory may
 * contain sub-directories or files.
 *
 * For more information on HDF5 Groups,
 *
 * <a href=
 * "https://support.hdfgroup.org/releases/hdf5/v1_14/v1_14_5/documentation/doxygen/_h5_g__u_g.html#sec_group">HDF5
 * Groups in HDF5 User Guide</a>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5Group extends Group {
    private static final long serialVersionUID = -951164512330444150L;

    private static final Logger log = LoggerFactory.getLogger(H5Group.class);

    /**
     * The metadata object for this data object. Members of the metadata are instances of Attribute.
     */
    private H5MetaDataContainer objMetadata;

    /** the object properties */
    private H5O_info_t objInfo;

    /**
     * Constructs an HDF5 group with specific name, path, and parent.
     *
     * @param theFile
     *            the file that contains the group.
     * @param theName
     *            the name of this group, e.g. "grp01".
     * @param thePath
     *            the full path of this group, e.g. "/groups/".
     * @param theParent
     *            the parent of this group.
     */
    public H5Group(FileFormat theFile, String theName, String thePath, Group theParent)
    {
        this(theFile, theName, thePath, theParent, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #H5Group(FileFormat, String, String, Group)}
     *
     * @param theFile
     *            the file that contains the group.
     * @param theName
     *            the name of this group, e.g. "grp01".
     * @param thePath
     *            the full path of this group, e.g. "/groups/".
     * @param theParent
     *            the parent of this group.
     * @param oid
     *            the oid of this group.
     */
    @Deprecated
    public H5Group(FileFormat theFile, String theName, String thePath, Group theParent, long[] oid)
    {
        super(theFile, theName, thePath, theParent, oid);
        nMembersInFile = -1;
        objMetadata    = new H5MetaDataContainer(theFile, theName, thePath, this);

        if (theFile != null) {
            if (oid == null) {
                // retrieve the object ID
                byte[] refBuf = null;
                try {
                    refBuf =
                        H5.H5Rcreate_object(theFile.getFID(), this.getFullName(), HDF5Constants.H5P_DEFAULT);
                    this.oid = HDFNativeData.byteToLong(refBuf);
                    log.trace("constructor REF {} to OID {}", refBuf, this.oid);
                }
                catch (Exception ex) {
                    log.debug("constructor ID {} for {} failed H5Rcreate_object", theFile.getFID(),
                              this.getFullName());
                }
                finally {
                    if (refBuf != null)
                        H5.H5Rdestroy(refBuf);
                }
            }
            log.trace("constructor OID {}", this.oid);
            try {
                objInfo = H5.H5Oget_info_by_name(theFile.getFID(), this.getFullName(),
                                                 HDF5Constants.H5O_INFO_BASIC, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                objInfo = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
            }
        }
        else {
            this.oid = null;
            objInfo  = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open()
    {
        long gid = HDF5Constants.H5I_INVALID_HID;

        try {
            if (isRoot()) {
                gid = H5.H5Gopen(getFID(), SEPARATOR, HDF5Constants.H5P_DEFAULT);
            }
            else {
                gid = H5.H5Gopen(getFID(), getPath() + getName(), HDF5Constants.H5P_DEFAULT);
            }
            log.trace("open(): gid={}", gid);
        }
        catch (HDF5Exception ex) {
            log.debug("open(): Failed to open group {}", getPath() + getName(), ex);
            gid = HDF5Constants.H5I_INVALID_HID;
        }

        return gid;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long gid)
    {
        if (gid >= 0) {
            try {
                H5.H5Gclose(gid);
            }
            catch (HDF5Exception ex) {
                log.debug("close(): H5Gclose(gid {}): ", gid, ex);
            }
        }
    }

    /**
     * Get the token for this object.
     *
     * @return true if it has any attributes, false otherwise.
     */
    public long[] getToken()
    {
        H5O_token_t token = objInfo.token;
        return HDFNativeData.byteToLong(token.data);
    }

    /**
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attributes, false otherwise.
     */
    @Override
    public boolean hasAttribute()
    {
        objInfo.num_attrs = objMetadata.getObjectAttributeSize();

        if (objInfo.num_attrs < 0) {
            long gid = open();
            if (gid > 0) {
                try {
                    objInfo = H5.H5Oget_info(gid);
                }
                catch (Exception ex) {
                    objInfo.num_attrs = 0;
                    log.debug("hasAttribute(): get object info failure: ", ex);
                }
                finally {
                    close(gid);
                }
                objMetadata.setObjectAttributeSize((int)objInfo.num_attrs);
            }
            else {
                log.debug("hasAttribute(): could not open group");
            }
        }

        log.trace("hasAttribute(): nAttributes={}", objInfo.num_attrs);
        return (objInfo.num_attrs > 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Group#getNumberOfMembersInFile()
     */
    @Override
    public int getNumberOfMembersInFile()
    {
        if (nMembersInFile < 0) {
            long gid = open();
            if (gid > 0) {
                try {
                    H5G_info_t group_info = null;
                    group_info            = H5.H5Gget_info(gid);
                    nMembersInFile        = (int)group_info.nlinks;
                }
                catch (Exception ex) {
                    nMembersInFile = 0;
                }
                close(gid);
            }
        }
        return nMembersInFile;
    }

    /**
     * Removes all of the elements from metadata list.
     * The list should be empty after this call returns.
     */
    @Override
    public void clear()
    {
        super.clear();
        objMetadata.clear();
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
    public List<Attribute> getMetadata() throws HDF5Exception
    {
        int gmIndexType  = 0;
        int gmIndexOrder = 0;

        try {
            gmIndexType = fileFormat.getIndexType(null);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getIndexType failed: ", ex);
        }
        try {
            gmIndexOrder = fileFormat.getIndexOrder(null);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getIndexOrder failed: ", ex);
        }
        return this.getMetadata(gmIndexType, gmIndexOrder);
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
    public List<Attribute> getMetadata(int... attrPropList) throws HDF5Exception
    {
        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        List<Attribute> attrlist = null;
        try {
            attrlist = objMetadata.getMetadata(attrPropList);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getMetadata failed: ", ex);
        }
        return attrlist;
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
    public void writeMetadata(Object info) throws Exception
    {
        try {
            objMetadata.writeMetadata(info);
        }
        catch (Exception ex) {
            log.debug("writeMetadata(): Object not an Attribute");
        }
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
    public void removeMetadata(Object info) throws HDF5Exception
    {
        try {
            objMetadata.removeMetadata(info);
        }
        catch (Exception ex) {
            log.debug("removeMetadata(): Object not an Attribute");
            return;
        }

        Attribute attr = (Attribute)info;
        log.trace("removeMetadata(): {}", attr.getAttributeName());
        long gid = open();
        if (gid >= 0) {
            try {
                H5.H5Adelete(gid, attr.getAttributeName());
            }
            catch (Exception ex) {
                log.debug("removeMetadata(): ", ex);
            }
            finally {
                close(gid);
            }
        }
        else {
            log.debug("removeMetadata(): failed to open group");
        }
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
    public void updateMetadata(Object info) throws HDF5Exception
    {
        try {
            objMetadata.updateMetadata(info);
        }
        catch (Exception ex) {
            log.debug("updateMetadata(): Object not an Attribute");
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#setName(java.lang.String)
     */
    @Override
    public void setName(String newName) throws Exception
    {
        if (newName == null)
            throw new IllegalArgumentException("The new name is NULL");

        H5File.renameObject(this, newName);
        super.setName(newName);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#setPath(java.lang.String)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void setPath(String newPath) throws Exception
    {
        super.setPath(newPath);

        List members = this.getMemberList();
        if (members == null)
            return;

        int n       = members.size();
        HObject obj = null;
        for (int i = 0; i < n; i++) {
            obj = (HObject)members.get(i);
            obj.setPath(getPath() + getName() + HObject.SEPARATOR);
        }
    }

    /**
     * Creates a new group with a name in a group and with the group creation
     * properties specified in gplist.
     *
     * The gplist contains a sequence of group creation property list
     * identifiers, lcpl, gcpl, gapl. It allows the user to create a group with
     * group creation properties. It will close the group creation properties
     * specified in gplist.
     *
     * @see hdf.hdf5lib.H5#H5Gcreate(long, String, long, long, long) for the
     *      order of property list identifiers.
     *
     * @param name
     *            The name of a new group.
     * @param pgroup
     *            The parent group object.
     * @param gplist
     *            The group creation properties, in which the order of the
     *            properties conforms the HDF5 library API, H5Gcreate(), i.e.
     *            lcpl, gcpl and gapl, where
     *            <ul>
     *            <li>lcpl : Property list for link creation <li>gcpl : Property
     *            list for group creation <li>gapl : Property list for group
     *            access
     *            </ul>
     *
     * @return The new group if successful; otherwise returns null.
     *
     * @throws Exception if there is a failure.
     */
    public static H5Group create(String name, Group pgroup, long... gplist) throws Exception
    {
        H5Group group   = null;
        String fullPath = null;
        long lcpl       = HDF5Constants.H5P_DEFAULT;
        long gcpl       = HDF5Constants.H5P_DEFAULT;
        long gapl       = HDF5Constants.H5P_DEFAULT;

        if (gplist.length > 0) {
            lcpl = gplist[0];
            if (gplist.length > 1) {
                gcpl = gplist[1];
                if (gplist.length > 2)
                    gapl = gplist[2];
            }
        }

        if ((name == null) || (pgroup == null)) {
            log.debug("create(): one or more parameters are null");
            return null;
        }

        H5File file = (H5File)pgroup.getFileFormat();
        if (file == null) {
            log.debug("create(): parent group FileFormat is null");
            return null;
        }

        String path = HObject.SEPARATOR;
        if (!pgroup.isRoot()) {
            path = pgroup.getPath() + pgroup.getName() + HObject.SEPARATOR;
            if (name.endsWith("/"))
                name = name.substring(0, name.length() - 1);
            int idx = name.lastIndexOf('/');
            if (idx >= 0)
                name = name.substring(idx + 1);
        }

        fullPath = path + name;

        // create a new group and add it to the parent node
        long gid = H5.H5Gcreate(file.open(), fullPath, lcpl, gcpl, gapl);
        try {
            H5.H5Gclose(gid);
        }
        catch (Exception ex) {
            log.debug("create(): H5Gcreate {} H5Gclose(gid {}) failure: ", fullPath, gid, ex);
        }

        group = new H5Group(file, name, path, pgroup);

        if (group != null)
            pgroup.addToMemberList(group);

        if (gcpl > 0) {
            try {
                H5.H5Pclose(gcpl);
            }
            catch (final Exception ex) {
                log.debug("create(): create prop H5Pclose(gcpl {}) failure: ", gcpl, ex);
            }
        }

        return group;
    }
}
