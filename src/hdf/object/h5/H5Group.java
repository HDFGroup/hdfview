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

package hdf.object.h5;

import java.util.List;
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.object.Attribute;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

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
 * @see <a href="http://hdfgroup.org/HDF5/doc/UG/">HDF5 User's Guide</a>
 *      <p>
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5Group extends Group {
    /**
     * 
     */
    private static final long serialVersionUID = -951164512330444150L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5Group.class);

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of Attribute.
     */
    protected List            attributeList;

    private int               nAttributes      = -1;

    private H5O_info_t        obj_info;

    /**
     * Constructs an HDF5 group with specific name, path, and parent.
     * <p>
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
     */
    @Deprecated
    public H5Group(FileFormat theFile, String name, String path, Group parent, long[] oid) {
        super(theFile, name, path, parent, oid);
        nMembersInFile = -1;
        obj_info = new H5O_info_t(-1L, -1L, 0, 0, -1L, 0L, 0L, 0L, 0L, null, null, null);

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
     * @see hdf.object.DataFormat#hasAttribute()
     */
    public boolean hasAttribute() {
        obj_info.num_attrs = nAttributes;

        if (obj_info.num_attrs < 0) {
            int gid = open();
            if (gid > 0) {
                try {
                    obj_info = H5.H5Oget_info(gid);

                }
                catch (Exception ex) {
                    obj_info.num_attrs = 0;
                }
                close(gid);
            }
        }

        return (obj_info.num_attrs > 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hdf.object.Group#getNumberOfMembersInFile()
     */
    @Override
    public int getNumberOfMembersInFile() {
        if (nMembersInFile < 0) {
            int gid = open();
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
     * @see hdf.object.Group#clear()
     */
    @Override
    public void clear() {
        super.clear();

        if (attributeList != null) {
            ((Vector) attributeList).setSize(0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see hdf.object.DataFormat#getMetadata()
     */
    public List getMetadata() throws HDF5Exception {
        return this.getMetadata(fileFormat.getIndexType(null), fileFormat.getIndexOrder(null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see hdf.object.DataFormat#getMetadata(int...)
     */
    public List getMetadata(int... attrPropList) throws HDF5Exception {
        if (attributeList == null) {
            int gid = open();
            if(gid >= 0) {
            	int indxType = fileFormat.getIndexType(null);
            	int order = fileFormat.getIndexOrder(null);

            	if (attrPropList.length > 0) {
            		indxType = attrPropList[0];
            		if (attrPropList.length > 1) {
            			order = attrPropList[1];
            		}
            	}
            	try {
            		attributeList = H5File.getAttribute(gid, indxType, order);
            	}
            	finally {
            		close(gid);
            	}
            }
            else {
            	log.debug("failed to open group");
            }
        }

        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
        	log.debug("getLinkTargetName:", ex);
        }

        return attributeList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hdf.object.DataFormat#writeMetadata(java.lang.Object)
     */
    public void writeMetadata(Object info) throws Exception {
        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            return;
        }

        boolean attrExisted = false;
        Attribute attr = (Attribute) info;
        log.trace("writeMetadata: {}", attr.getName());

        if (attributeList == null) {
            this.getMetadata();
        }

        if (attributeList != null) attrExisted = attributeList.contains(attr);

        getFileFormat().writeAttribute(this, attr, attrExisted);
        // add the new attribute into attribute list
        if (!attrExisted) {
            attributeList.add(attr);
            nAttributes = attributeList.size();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see hdf.object.DataFormat#removeMetadata(java.lang.Object)
     */
    public void removeMetadata(Object info) throws HDF5Exception {
        // only attribute metadata is supported.
    	if (!(info instanceof Attribute)) {
    		return;
    	}

    	Attribute attr = (Attribute) info;
        log.trace("removeMetadata: {}", attr.getName());
    	int gid = open();
    	if(gid >= 0) {
    	    try {
    	        H5.H5Adelete(gid, attr.getName());
    	        List attrList = getMetadata();
    	        attrList.remove(attr);
    	        nAttributes = attributeList.size();
    	    }
    	    finally {
    	        close(gid);
    	    }
    	}
    	else {
    	    log.debug("failed to open group");
    	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see hdf.object.DataFormat#updateMetadata(java.lang.Object)
     */
    public void updateMetadata(Object info) throws HDF5Exception {
        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            return;
        }
        log.trace("updateMetadata");

        nAttributes = -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hdf.object.HObject#open()
     */
    @Override
    public int open() {
        int gid = -1;

        try {
            if (isRoot()) {
                gid = H5.H5Gopen(getFID(), separator, HDF5Constants.H5P_DEFAULT);
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
    public void close(int gid) {
        try {
            H5.H5Gclose(gid);
        }
        catch (HDF5Exception ex) {
        	log.debug("H5Gclose:", ex);
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
     * @see hdf.hdf5lib.H5#H5Gcreate(int, String, int, int, int) for the
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
     */
    public static H5Group create(String name, Group pgroup, int... gplist) throws Exception {
        H5Group group = null;
        String fullPath = null;
        int lcpl = HDF5Constants.H5P_DEFAULT;
        int gcpl = HDF5Constants.H5P_DEFAULT;
        int gapl = HDF5Constants.H5P_DEFAULT;

        if (gplist.length > 0) {
            lcpl = gplist[0];
            if (gplist.length > 1) {
                gcpl = gplist[1];
                if (gplist.length > 2) gapl = gplist[2];
            }
        }

        if ((name == null) || (pgroup == null)) {
            System.err.println("(name == null) || (pgroup == null)");
            return null;
        }

        H5File file = (H5File) pgroup.getFileFormat();

        if (file == null) {
            System.err.println("Could not get file that contains object");
            return null;
        }

        String path = HObject.separator;
        if (!pgroup.isRoot()) {
            path = pgroup.getPath() + pgroup.getName() + HObject.separator;
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            int idx = name.lastIndexOf("/");
            if (idx >= 0) {
                name = name.substring(idx + 1);
            }
        }

        fullPath = path + name;

        // create a new group and add it to the parent node
        int gid = H5.H5Gcreate(file.open(), fullPath, lcpl, gcpl, gapl);
        try {
            H5.H5Gclose(gid);
        }
        catch (Exception ex) {
        	log.debug("H5Gcreate {} H5Gclose:", fullPath, ex);
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
            	log.debug("create prop H5Pclose:", ex);
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
        H5File.renameObject(this, newName);
        super.setName(newName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hdf.object.HObject#setPath(java.lang.String)
     */
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
            obj.setPath(getPath() + getName() + HObject.separator);
        }
    }
}
