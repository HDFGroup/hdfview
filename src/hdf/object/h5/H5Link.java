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

import hdf.hdf5lib.structs.H5O_info_t;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;

/**
 * An H5Link object represents an existing HDF5 object in file.
 * <p>
 * H5Link object is an HDF5 object that is either a soft or an external link to
 * an object in a file that does not exist. The type of the object is unknown.
 * Once the object being linked to is created, and the type is known, then
 * H5link object will change its type.
 *
 * @version 2.7.2 7/6/2010
 * @author Nidhi Gupta
 */

public class H5Link extends HObject implements MetaDataContainer {
    private static final long serialVersionUID = -8137277460521594367L;

    @SuppressWarnings("unused")
    private H5O_info_t obj_info;

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

    @SuppressWarnings("deprecation")
    public H5Link(FileFormat theFile, String theName, String thePath,
            long[] oid) {
        super(theFile, theName, thePath, oid);

        obj_info = new H5O_info_t(-1L, -1L, -1, 0, -1L, 0L, 0L, 0L, 0L, null,null,null);
    }

    @Override
    public void close(long id) {
    }

    @Override
    public long open() {
        return 0;
    }

    @SuppressWarnings("rawtypes")
    public List getMetadata() throws Exception {

        try{
            this.linkTargetObjName= H5File.getLinkTargetName(this);
        }catch(Exception ex){
        }

        return null;
    }

    public boolean hasAttribute() {
        return false;
    }

    public void removeMetadata(Object info) throws Exception {
    }

    public void writeMetadata(Object info) throws Exception {
    }

    public void updateMetadata(Object info) throws Exception {
    }

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
        H5File.renameObject(this, newName);
        super.setName(newName);
    }
}
