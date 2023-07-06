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

package hdf.object;

import java.util.List;

/**
 * An interface that provides general I/O operations for object metadata
 * attached to an object. For example, reading metadata content from the file
 * into memory or writing metadata content from memory into the file.
 *
 * @see hdf.object.HObject
 *
 * @version 2.0 4/2/2018
 * @author Peter X. Cao, Jordan T. Henderson
 */
@SuppressWarnings("rawtypes")
public interface MetaDataContainer
{
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
    List getMetadata() throws Exception;

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
     * @param metadata
     *            the metadata to write.
     *
     * @throws Exception
     *             if the metadata can not be written
     */
    void writeMetadata(Object metadata) throws Exception;

    /**
     * Deletes an existing piece of metadata from this object.
     *
     * @param metadata
     *            the metadata to delete.
     *
     * @throws Exception
     *             if the metadata can not be removed
     */
    void removeMetadata(Object metadata) throws Exception;

    /**
     * Updates an existing piece of metadata attached to this object.
     *
     * @param metadata
     *            the metadata to update.
     *
     * @throws Exception
     *             if the metadata can not be updated
     */
    void updateMetadata(Object metadata) throws Exception;

    /**
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attributes, false otherwise.
     */
    boolean hasAttribute();

    /**
     * Removes all of the elements from metadata list.
     * The list should be empty after this call returns.
     */
    void clear();
}
