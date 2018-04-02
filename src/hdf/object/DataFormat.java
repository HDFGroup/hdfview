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

package hdf.object;

/* TODO: Update comments */

/**
 * An interface that provides general I/O operations for object data. For
 * example, reading data content from the file into memory or writing data
 * content from memory into the file.
 * <p>
 *
 * @see hdf.object.HObject
 *
 * @version 1.0 4/2/2018
 * @author Jordan T. Henderson
 */
public interface DataFormat {
    /**
     * Retrieves the data from the file.
     * <p>
     * Metadata, such as attributes, is stored in a List.
     *
     * @return the list of metadata objects.
     *
     * @throws Exception
     *             if the metadata can not be retrieved
     */
    public abstract Object getData() throws Exception, OutOfMemoryError;

    /**
     *
     *
     * @param metadata
     *            the metadata to write.
     *
     * @throws Exception
     *             if the metadata can not be written
     */
    public abstract void setData(Object data);
}
