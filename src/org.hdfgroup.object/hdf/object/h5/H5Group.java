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
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.hdf5lib.structs.H5O_info_t;

import hdf.object.AttributeDataset;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5MetaDataContainer;

/**
 * An H5Group object represents an existing HDF5 group in file.
 * <p>
 * In HDF5, every object has at least one name. An HDF5 group is used to store a
 * set of the names together in one place, i.e. a group. The general structure
 * of a group is similar to that of the UNIX file system in that the group may
 * contain references to other groups or data objects just as the UNIX directory
 * may contain sub-directories or files.
 * <p>
 * For more information on HDF5 Groups,
 *
 * <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5Group extends Group {

    private static final long serialVersionUID = -951164512330444150L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5Group.class);

    /**
     * The metadata object for this data object. Members of the metadata are instances of AttributeDataset.
     */
    private H5MetaDataContainer objMetadata;

    private H5O_info_t        objInfo;

    /**
     * Constructs an HDF5 group with specific name, path, and parent.
     *
     * @param theFile
     *            the file which containing the group.
     * @param name
     *            the name of this group, e.g. "grp01".
     * @param path
     *            the full path of this group, e.g. "/groups/".
     * @param parent
     *            the parent of this group.
     */
    public H5Group(FileFormat theFile, String name, String path, Group parent) {
        this(theFile, name, path, parent, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #H5Group(FileFormat, String, String, Group)}
     *
     * @param theFile
     *            the file which containing the group.
     * @param name
     *            the name of this group, e.g. "grp01".
     * @param path
     *            the full path of this group, e.g. "/groups/".
     * @param parent
     *            the parent of this group.
     * @param oid
     *            the oid of this group.
     */
    @Deprecated
    public H5Group(FileFormat theFile, String name, String path, Group parent, long[] oid) {
        super(theFile, name, path, parent, oid);
        nMembersInFile = -1;
        objInfo = new H5O_info_t(-1L, -1L, 0, 0, -1L, 0L, 0L, 0L, 0L, null, null, null);
        objMetadata = new H5MetaDataContainer(theFile, name, path, this);

        if ((oid == null) && (theFile != null)) {
            // retrieve the object ID
            try {
                byte[] ref_buf = H5.H5Rcreate(theFile.getFID(), this.getFullName(), HDF5Constants.H5R_OBJECT, -1);
                this.oid = new long[1];
                this.oid[0] = HDFNativeData.byteToLong(ref_buf, 0);
            }
            catch (Exception ex) {
                this.oid = new long[1];
                this.oid[0] = 0;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.MetaDataContainer#hasAttribute()
     */
    @Override
    public boolean hasAttribute() {
        objInfo.num_attrs = objMetadata.getObjectAttributeSize();

        if (objInfo.num_attrs < 0) {
            long gid = open();
            if (gid > 0) {
                try {
                    objInfo = H5.H5Oget_info(gid);
                }
                catch (Exception ex) {
                    objInfo.num_attrs = 0;
                }
                finally {
                    close(gid);
                }
                objMetadata.setObjectAttributeSize((int) objInfo.num_attrs);
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
    public int getNumberOfMembersInFile() {
        if (nMembersInFile < 0) {
            long gid = open();
            if (gid > 0) {
                try {
                    H5G_info_t group_info = null;
                    group_info = H5.H5Gget_info(gid);
                    nMembersInFile = (int) group_info.nlinks;
                }
                catch (Exception ex) {
                    nMembersInFile = 0;
                }
                close(gid);
            }
        }
        return nMembersInFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.MetaDataContainer#clear()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void clear() {
        super.clear();
        objMetadata.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.MetaDataContainer#getMetadata()
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List getMetadata() throws HDF5Exception {
        return this.getMetadata(fileFormat.getIndexType(null), fileFormat.getIndexOrder(null));
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.MetaDataContainer#getMetadata(int...)
     */
    @SuppressWarnings("rawtypes")
    public List getMetadata(int... attrPropList) throws HDF5Exception {
        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        List<AttributeDataset> attrlist = null;
        try {
            attrlist = objMetadata.getMetadata(attrPropList);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getMetadata failed: ", ex);
        }
        return attrlist;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.MetaDataContainer#writeMetadata(java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void writeMetadata(Object info) throws Exception {
        try {
            objMetadata.writeMetadata(info);
        }
        catch (Exception ex) {
            log.debug("writeMetadata(): Object not an AttributeDataset");
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.MetaDataContainer#removeMetadata(java.lang.Object)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void removeMetadata(Object info) throws HDF5Exception {
        try {
            objMetadata.removeMetadata(info);
        }
        catch (Exception ex) {
            log.debug("removeMetadata(): Object not an AttributeDataset");
            return;
        }

        AttributeDataset attr = (AttributeDataset) info;
        log.trace("removeMetadata(): {}", attr.getName());
        long gid = open();
        if(gid >= 0) {
            try {
                H5.H5Adelete(gid, attr.getName());
            }
            finally {
                close(gid);
            }
        }
        else {
            log.debug("removeMetadata(): failed to open group");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.MetaDataContainer#updateMetadata(java.lang.Object)
     */
    @Override
    public void updateMetadata(Object info) throws HDF5Exception {
        try {
            objMetadata.updateMetadata(info);
        }
        catch (Exception ex) {
            log.debug("updateMetadata(): Object not an AttributeDataset");
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        long gid = -1;

        try {
            if (isRoot()) {
                gid = H5.H5Gopen(getFID(), SEPARATOR, HDF5Constants.H5P_DEFAULT);
            }
            else {
                gid = H5.H5Gopen(getFID(), getPath() + getName(), HDF5Constants.H5P_DEFAULT);
            }

        }
        catch (HDF5Exception ex) {
            gid = -1;
        }

        return gid;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long gid) {
        try {
            H5.H5Gclose(gid);
        }
        catch (HDF5Exception ex) {
            log.debug("close(): H5Gclose(gid {}): ", gid, ex);
        }
    }

    /**
     * Creates a new group with a name in a group and with the group creation
     * properties specified in gplist.
     * <p>
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
    public static H5Group create(String name, Group pgroup, long... gplist) throws Exception {
        H5Group group = null;
        String fullPath = null;
        long lcpl = HDF5Constants.H5P_DEFAULT;
        long gcpl = HDF5Constants.H5P_DEFAULT;
        long gapl = HDF5Constants.H5P_DEFAULT;

        if (gplist.length > 0) {
            lcpl = gplist[0];
            if (gplist.length > 1) {
                gcpl = gplist[1];
                if (gplist.length > 2) gapl = gplist[2];
            }
        }

        if ((name == null) || (pgroup == null)) {
            log.debug("create(): one or more parameters are null");
            System.err.println("(name == null) || (pgroup == null)");
            return null;
        }

        H5File file = (H5File) pgroup.getFileFormat();

        if (file == null) {
            log.debug("create(): Parent Group FileFormat is null");
            System.err.println("Could not get file that contains object");
            return null;
        }

        String path = HObject.SEPARATOR;
        if (!pgroup.isRoot()) {
            path = pgroup.getPath() + pgroup.getName() + HObject.SEPARATOR;
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            int idx = name.lastIndexOf('/');
            if (idx >= 0) {
                name = name.substring(idx + 1);
            }
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

        byte[] ref_buf = H5.H5Rcreate(file.open(), fullPath, HDF5Constants.H5R_OBJECT, -1);
        long l = HDFNativeData.byteToLong(ref_buf, 0);
        long[] oid = { l };

        group = new H5Group(file, name, path, pgroup, oid);

        if (group != null) {
            pgroup.addToMemberList(group);
        }

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

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#setPath(java.lang.String)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void setPath(String newPath) throws Exception {
        super.setPath(newPath);

        List members = this.getMemberList();
        if (members == null) {
            return;
        }

        int n = members.size();
        HObject obj = null;
        for (int i = 0; i < n; i++) {
            obj = (HObject) members.get(i);
            obj.setPath(getPath() + getName() + HObject.SEPARATOR);
        }
    }
}
