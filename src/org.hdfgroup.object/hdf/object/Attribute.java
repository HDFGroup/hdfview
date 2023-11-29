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

import java.util.Collection;

/**
 * An interface that provides general attribute operations for object data. For
 * example, reference to a parent object.
 *
 * @see hdf.object.HObject
 */
public interface Attribute {
    /**
     * Returns the HObject to which this Attribute is currently "attached".
     *
     * @return the HObject to which this Attribute is currently "attached".
     */
    HObject getParentObject();

    /**
     * Sets the HObject to which this Attribute is "attached".
     *
     * @param pObj
     *            the new HObject to which this Attributet is "attached".
     */
    void setParentObject(HObject pObj);

    /**
     * set a property for the attribute.
     *
     * @param key the attribute Map key
     * @param value the attribute Map value
     */
    void setProperty(String key, Object value);

    /**
     * get a property for a given key.
     *
     * @param key the attribute Map key
     *
     * @return the property
     */
    Object getProperty(String key);

    /**
     * get all property keys.
     *
     * @return the Collection of property keys
     */
    Collection<String> getPropertyKeys();

    /**
     * Returns the name of the attribute.
     *
     * @return The name of the attribute.
     */
    String getAttributeName();

    /**
     * Retrieves the attribute data from the file.
     *
     * @return the attribute data.
     *
     * @throws Exception
     *             if the data can not be retrieved
     */
    Object getAttributeData() throws Exception, OutOfMemoryError;

    /**
     * Returns the datatype of the attribute.
     *
     * @return the datatype of the attribute.
     */
    Datatype getAttributeDatatype();

    /**
     * Returns the space type for the attribute. It returns a
     * negative number if it failed to retrieve the type information from
     * the file.
     *
     * @return the space type for the attribute.
     */
    int getAttributeSpaceType();

    /**
     * Returns the rank (number of dimensions) of the attribute. It returns a
     * negative number if it failed to retrieve the dimension information from
     * the file.
     *
     * @return the number of dimensions of the attribute.
     */
    int getAttributeRank();

    /**
     * Returns the array that contains the dimension sizes of the data value of
     * the attribute. It returns null if it failed to retrieve the dimension
     * information from the file.
     *
     * @return the dimension sizes of the attribute.
     */
    long[] getAttributeDims();

    /**
     * Returns the selected size of the rows and columns of the attribute. It returns a
     * negative number if it failed to retrieve the size information from
     * the file.
     *
     * @return the selected size of the rows and colums of the attribute.
     */
    int getAttributePlane();

    /**
     * Check if attribute's dataspace is a NULL
     *
     * @return true if the dataspace is a NULL; otherwise, returns false.
     */
    boolean isAttributeNULL();

    /**
     * Check if attribute is a single scalar point
     *
     * @return true if the data is a single scalar point; otherwise, returns false.
     */
    boolean isAttributeScalar();

    /**
     * Not for public use in the future.
     *
     * setAttributeData() is not safe to use because it changes memory buffer of the dataset object. Dataset
     * operations such as write/read will fail if the buffer type or size is changed.
     *
     * @param d the object data -must be an array of Objects
     */
    void setAttributeData(Object d);

    /**
     * Writes the memory buffer of this dataset to file.
     *
     * @throws Exception if buffer can not be written
     */
    void writeAttribute() throws Exception;

    /**
     * Writes the given data buffer into this attribute in a file.
     *
     * The data buffer is a vector that contains the data values of compound fields. The data is written
     * into file as one data blob.
     *
     * @param buf
     *            The vector that contains the data values of compound fields.
     *
     * @throws Exception
     *             If there is an error at the library level.
     */
    void writeAttribute(Object buf) throws Exception;

    /**
     * Returns a string representation of the data value. For
     * example, "0, 255".
     *
     * For a compound datatype, it will be a 1D array of strings with field
     * members separated by the delimiter. For example,
     * "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of {int,
     * float} of three data points.
     *
     * @param delimiter
     *            The delimiter used to separate individual data points. It
     *            can be a comma, semicolon, tab or space. For example,
     *            toString(",") will separate data by commas.
     *
     * @return the string representation of the data values.
     */
    String toAttributeString(String delimiter);

    /**
     * Returns a string representation of the data value. For
     * example, "0, 255".
     *
     * For a compound datatype, it will be a 1D array of strings with field
     * members separated by the delimiter. For example,
     * "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of {int,
     * float} of three data points.
     *
     * @param delimiter
     *            The delimiter used to separate individual data points. It
     *            can be a comma, semicolon, tab or space. For example,
     *            toString(",") will separate data by commas.
     * @param maxItems
     *            The maximum number of Array values to return
     *
     * @return the string representation of the data values.
     */
    String toAttributeString(String delimiter, int maxItems);
}
