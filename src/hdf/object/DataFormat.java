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
     * Retrieves the object's data from the file.
     *
     * @return the object's data.
     *
     * @throws Exception
     *             if the data can not be retrieved
     */
    public abstract Object getData() throws Exception, OutOfMemoryError;

    /**
     *
     *
     * @param data
     *            the data to write.
     */
    public abstract void setData(Object data);

    /**
     * Reads the data from file.
     * <p>
     * read() reads the data from file to a memory buffer and returns the memory
     * buffer. The dataset object does not hold the memory buffer. To store the
     * memory buffer in the dataset object, one must call getData().
     * <p>
     * By default, the whole dataset is read into memory. Users can also select
     * a subset to read. Subsetting is done in an implicit way.
     *
     * @return the data read from file.
     *
     * @see #getData()
     *
     * @throws Exception
     *             if object can not be read
     * @throws OutOfMemoryError
     *             if memory is exhausted
     */
    public abstract Object read() throws Exception, OutOfMemoryError;

    /**
     * Writes a memory buffer to the object in the file.
     *
     * @param buf
     *            the data to write
     *
     * @throws Exception
     *             if data can not be written
     */
    public abstract void write(Object buf) throws Exception;

    /**
     * Returns the datatype of the data object.
     *
     * @return the datatype of the data object.
     */
    public abstract Datatype getDatatype();

    /**
     * Returns the rank (number of dimensions) of the data object. It returns a
     * negative number if it failed to retrieve the dimension information from
     * the file.
     *
     * @return the number of dimensions of the data object.
     */
    public abstract int getRank();

    /**
     * Returns the array that contains the dimension sizes of the data value of
     * the data object. It returns null if it failed to retrieve the dimension
     * information from the file.
     *
     * @return the dimension sizes of the data object.
     */
    public abstract long[] getDims();
}
