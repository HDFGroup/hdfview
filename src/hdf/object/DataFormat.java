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

import java.util.List;

/**
 * An interface that provides general I/O operations for read/write object data.
 * For example, reading data content or data attribute from file into memory or
 * writing data content or data attribute from memory into file.
 * <p>
 *
 * @see hdf.object.HObject
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
@SuppressWarnings("rawtypes")
public interface DataFormat {
    /**
     * Returns the full path of the file that contains this data object.
     * <p>
     * The file name is necessary because data objects are uniquely identified
     * by object reference and file name when mutilple files are opened at the
     * same time.
     *
     * @return the full path of the file.
     */
    public abstract String getFile();

    /**
     * Retrieves the metadata such as attributes from file.
     * <p>
     * Metadata such as attributes are stored in a List.
     *
     * @return the list of metadata objects.
     *
     * @throws Exception if the metadata can not be retrieved
     */
    public abstract List getMetadata() throws Exception;

    /**
     * Writes a specific metadata (such as attribute) into file.
     *
     * If an HDF(4&amp;5) attribute exists in file, the method updates its value.
     * If the attribute does not exists in file, it creates the attribute in
     * file and attaches it to the object.
     * It will fail to write a new attribute to the object where an attribute
     * with the same name already exists.
     * To update the value of an existing attribute in file, one needs to get
     * the instance of the attribute by getMetadata(), change its values,
     * and use writeMetadata() to write the value.
     *
     * @param info
     *            the metadata to write.
     *
     * @throws Exception if the metadata can not be written
     */
    public abstract void writeMetadata(Object info) throws Exception;

    /**
     * Deletes an existing metadata from this data object.
     *
     * @param info
     *            the metadata to delete.
     *
     * @throws Exception if the metadata can not be removed
     */
    public abstract void removeMetadata(Object info) throws Exception;

    /**
     * Updates an existing metadata from this data object.
     *
     * @param info
     *            the metadata to update.
     *
     * @throws Exception if the metadata can not be updated
     */
    public abstract void updateMetadata(Object info) throws Exception;

    /**
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attribute(s), false otherwise.
     */
    public abstract boolean hasAttribute();

}
