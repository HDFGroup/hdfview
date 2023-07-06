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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.object.Attribute;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;

/**
 * An class that provides general I/O operations for object metadata
 * attached to an object. For example, reading metadata content from the file
 * into memory or writing metadata content from memory into the file.
 *
 * @see hdf.object.HObject
 *
 * @version 2.0 4/2/2018
 * @author Peter X. Cao, Jordan T. Henderson
 */
@SuppressWarnings("rawtypes")
public class H5MetaDataContainer extends HObject implements MetaDataContainer
{

    private static final Logger log = LoggerFactory.getLogger(H5Group.class);

    /** The HObject to which this MetaDataContainer is attached */
    protected HObject         parentObject;

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of Attribute.
     */
    @SuppressWarnings("rawtypes")
    protected List            attributeList;

    /** the number of attributes */
    private int               nAttributes      = -1;

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
    public H5MetaDataContainer(FileFormat theFile, String name, String path, HObject parent) {
        this(theFile, name, path, parent, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
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
    public H5MetaDataContainer(FileFormat theFile, String name, String path, HObject parent, long[] oid) {
        super(theFile, name, path, oid);

        this.parentObject = parent;
    }

    /**
     * Removes all of the elements from metadata list.
     * The list should be empty after this call returns.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void clear() {
        if (attributeList != null)
            ((Vector) attributeList).setSize(0);
    }

    /**
     * Retrieves the object's attributeList.
     *
     * @return the list.
     */
    public List getAttributeList() {
        return attributeList;
    }

    /**
     * Retrieves the object's number of attributes.
     *
     * @return the list size.
     */
    public int getObjectAttributeSize() {
        return nAttributes;
    }

    /**
     * Updates the object's number of attributes.
     *
     * @param objectAttributes
     *            the number of attributes for an object.
     */
    public void setObjectAttributeSize(int objectAttributes) {
        nAttributes = objectAttributes;
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
    public List getMetadata() throws Exception {
        int gmIndexType = 0;
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
     * @throws Exception
     *             if the metadata can not be retrieved
     */
    @SuppressWarnings("rawtypes")
    public List getMetadata(int... attrPropList) throws Exception {
        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this.parentObject);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        if (attributeList != null) {
            log.trace("getMetadata(): attributeList != null");
            return attributeList;
        }

        int indxType = fileFormat.getIndexType(null);
        int order = fileFormat.getIndexOrder(null);

        if (attrPropList.length > 0) {
            indxType = attrPropList[0];
            if (attrPropList.length > 1)
                order = attrPropList[1];
        }
        try {
            attributeList = H5File.getAttribute(this.parentObject, indxType, order);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): H5File.getAttribute failure: ", ex);
        }

        return attributeList;
    }

    /**
     * Writes a specific piece of metadata (such as an attribute) into the file.
     *
     * If an HDF5 attribute exists in the file, this method updates its
     * value. If the attribute does not exist in the file, it creates the
     * attribute in the file and attaches it to the object. It will fail to
     * write a new attribute to the object where an attribute with the same name
     * already exists. To update the value of an existing attribute in the file,
     * one needs to get the instance of the attribute by getMetadata(), change
     * its values, then use writeMetadata() to write the value.
     *
     * @param metadata
     *            the metadata to write.
     *
     * @throws Exception
     *             if the metadata can not be written
     */
    public void writeMetadata(Object metadata) throws Exception {
        // only attribute metadata is supported.
        if (!(metadata instanceof Attribute)) {
            log.debug("writeMetadata(): Object not an Attribute");
            return;
        }

        boolean attrExisted = false;
        Attribute attr = (Attribute) metadata;
        log.trace("writeMetadata(): {}", attr.getAttributeName());

        if (attributeList == null)
            attributeList = ((MetaDataContainer)parentObject).getMetadata();

        if (attributeList != null)
            attrExisted = attributeList.contains(attr);

        getFileFormat().writeAttribute(this.parentObject, attr, attrExisted);
        // add the new attribute into attribute list
        if (!attrExisted) {
            attributeList.add(attr);
            nAttributes = attributeList.size();
        }
    }

    /**
     * Deletes an existing piece of metadata from this object.
     *
     * @param metadata
     *            the metadata to delete.
     *
     * @throws Exception
     *             if the metadata can not be removed
     */
    public void removeMetadata(Object metadata) throws Exception {
        // only attribute metadata is supported.
        if (!(metadata instanceof Attribute))
            throw new IllegalArgumentException("Object not an Attribute");

        Attribute attr = (Attribute) metadata;
        log.trace("removeMetadata(): {}", attr.getAttributeName());
        List attrList = getMetadata();
        attrList.remove(attr);
        nAttributes = attributeList.size();
    }

    /**
     * Updates an existing piece of metadata attached to this object.
     *
     * @param metadata
     *            the metadata to update.
     *
     * @throws Exception
     *             if the metadata can not be updated
     */
    public void updateMetadata(Object metadata) throws Exception {
        // only attribute metadata is supported.
        if (!(metadata instanceof Attribute)) {
            log.debug("updateMetadata(): Object not an Attribute");
            return;
        }

        nAttributes = -1;
    }

    /**
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attributes, false otherwise.
     */
    public boolean hasAttribute() {
        return (nAttributes > 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open(int)
     */
    @Override
    public long open() {
        return -1;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long tid) {
    }
}
