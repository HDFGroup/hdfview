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

package hdf.object.nc2;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.object.Attribute;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * An attribute is a (name, value) pair of metadata attached to a primary data object such as a dataset, group or named
 * datatype.
 *
 * Like a dataset, an attribute has a name, datatype and dataspace.
 *
 * For more details on attributes, <a href="https://hdfgroup.github.io/hdf5/_h5_a__u_g.html#sec_attribute">HDF5
 * Attributes in HDF5 User Guide</a>
 *
 * The following code is an example of an attribute with 1D integer array of two elements.
 *
 * <pre>
 * // Example of creating a new attribute
 * // The name of the new attribute
 * String name = "Data range";
 * // Creating an unsigned 1-byte integer datatype
 * Datatype type = new Datatype(Datatype.CLASS_INTEGER, // class
 *                              1,                      // size in bytes
 *                              Datatype.ORDER_LE,      // byte order
 *                              Datatype.SIGN_NONE);    // unsigned
 * // 1-D array of size two
 * long[] dims = {2};
 * // The value of the attribute
 * int[] value = {0, 255};
 * // Create a new attribute
 * Attribute dataRange = new Attribute(name, type, dims);
 * // Set the attribute value
 * dataRange.setValue(value);
 * // See FileFormat.writeAttribute() for how to attach an attribute to an object,
 * &#64;see hdf.object.FileFormat#writeAttribute(HObject, Attribute, boolean)
 * </pre>
 *
 *
 * For an atomic datatype, the value of an Attribute will be a 1D array of integers, floats and strings. For a compound
 * datatype, it will be a 1D array of strings with field members separated by a comma. For example, "{0, 10.5}, {255,
 * 20.0}, {512, 30.0}" is a compound attribute of {int, float} of three data points.
 *
 * @see hdf.object.Datatype
 *
 * @version 2.0 4/2/2018
 * @author Peter X. Cao, Jordan T. Henderson
 */
public class NC2Attribute extends ScalarDS implements Attribute
{
    private static final long serialVersionUID = 2072473407027648309L;

    private static final Logger log = LoggerFactory.getLogger(NC2Attribute.class);

    /** The HObject to which this NC2Attribute is attached, Attribute interface */
    protected HObject         parentObject;

    /** additional information and properties for the attribute, Attribute interface */
    private transient Map<String, Object> properties;

    /**
     * Create an attribute with specified name, data type and dimension sizes.
     *
     * For scalar attribute, the dimension size can be either an array of size one
     * or null, and the rank can be either 1 or zero. Attribute is a general class
     * and is independent of file format, e.g., the implementation of attribute
     * applies to both HDF4 and HDF5.
     *
     * The following example creates a string attribute with the name "CLASS" and
     * value "IMAGE".
     *
     * <pre>
     * long[] attrDims = { 1 };
     * String attrName = &quot;CLASS&quot;;
     * String[] classValue = { &quot;IMAGE&quot; };
     * Datatype attrType = null;
     * try {
     *     attrType = new NC2Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, Datatype.NATIVE, Datatype.NATIVE);
     * }
     * catch (Exception ex) {}
     * Attribute attr = new Attribute(attrName, attrType, attrDims);
     * attr.setValue(classValue);
     * </pre>
     *
     * @param parentObj
     *            the HObject to which this Attribute is attached.
     * @param attrName
     *            the name of the attribute.
     * @param attrType
     *            the datatype of the attribute.
     * @param attrDims
     *            the dimension sizes of the attribute, null for scalar attribute
     *
     * @see hdf.object.Datatype
     */
    public NC2Attribute(HObject parentObj, String attrName, Datatype attrType, long[] attrDims) {
        this(parentObj, attrName, attrType, attrDims, null);
    }

    /**
     * Create an attribute with specific name and value.
     *
     * For scalar attribute, the dimension size can be either an array of size one
     * or null, and the rank can be either 1 or zero. Attribute is a general class
     * and is independent of file format, e.g., the implementation of attribute
     * applies to both HDF4 and HDF5.
     *
     * The following example creates a string attribute with the name "CLASS" and
     * value "IMAGE".
     *
     * <pre>
     * long[] attrDims = { 1 };
     * String attrName = &quot;CLASS&quot;;
     * String[] classValue = { &quot;IMAGE&quot; };
     * Datatype attrType = null;
     * try {
     *     attrType = new NC2Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, Datatype.NATIVE, Datatype.NATIVE);
     * }
     * catch (Exception ex) {}
     * NC2Attribute attr = new NC2Attribute(attrName, attrType, attrDims, classValue);
     * </pre>
     *
     * @param parentObj
     *            the HObject to which this Attribute is attached.
     * @param attrName
     *            the name of the attribute.
     * @param attrType
     *            the datatype of the attribute.
     * @param attrDims
     *            the dimension sizes of the attribute, null for scalar attribute
     * @param attrValue
     *            the value of the attribute, null if no value
     *
     * @see hdf.object.Datatype
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
    public NC2Attribute(HObject parentObj, String attrName, Datatype attrType, long[] attrDims, Object attrValue) {
        super((parentObj == null) ? null : parentObj.getFileFormat(), attrName,
                (parentObj == null) ? null : parentObj.getFullName(), null);

        log.trace("NC2Attribute: start {}", parentObj);
        this.parentObject = parentObj;

        unsignedConverted = false;

        datatype = attrType;

        if (attrValue != null) {
            data = attrValue;
            originalBuf = attrValue;
            isDataLoaded = true;
        }
        properties = new HashMap();

        if (attrDims == null) {
            rank = 1;
            dims = new long[] { 1 };
        }
        else {
            dims = attrDims;
            rank = dims.length;
        }

        selectedDims = new long[rank];
        startDims = new long[rank];
        selectedStride = new long[rank];

        log.trace("attrName={}, attrType={}, attrValue={}, rank={}, isUnsigned={}",
                attrName, getDatatype().getDescription(), data, rank, getDatatype().isUnsigned());

        resetSelection();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        long aid = -1;
        long pObjID = -1;

        if (parentObject == null) {
            log.debug("open(): attribute's parent object is null");
            return -1;
        }

        try {
            pObjID = parentObject.open();
            if (pObjID >= 0) {
                if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
                    log.trace("open(): FILE_TYPE_NC3");
                    /*
                     * TODO: Get type of netcdf3 object this is attached to and retrieve attribute info.
                     */
                }
            }

            log.trace("open(): aid={}", aid);
        }
        catch (Exception ex) {
            log.debug("open(): Failed to open attribute {}: ", getName(), ex);
            aid = -1;
        }
        finally {
            parentObject.close(pObjID);
        }

        return aid;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long aid) {
        if (aid >= 0) {
            if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
                log.trace("close(): FILE_TYPE_NC3");
                /*
                 * TODO: Get type of netcdf3 object this is attached to and close attribute.
                 */
            }
        }
    }

    @Override
    public void init() {
        if (inited) {
            resetSelection();
            log.trace("init(): NC2Attribute already inited");
            return;
        }

        if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
            log.trace("init(): FILE_TYPE_NC3");
            /*
             * TODO: If netcdf3 attribute object needs to init dependent objects.
             */
            inited = true;
        }

        resetSelection();
    }

    /**
     * Reads the data from file.
     *
     * read() reads the data from file to a memory buffer and returns the memory
     * buffer. The dataset object does not hold the memory buffer. To store the
     * memory buffer in the dataset object, one must call getData().
     *
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
    @Override
    public Object read() throws Exception, OutOfMemoryError {
        if (!inited)
            init();

        return data;
    }

    /* Implement abstract Dataset */

    /**
     * Writes a memory buffer to the object in the file.
     *
     * @param buf
     *            the data to write
     *
     * @throws Exception
     *             if data can not be written
     */
    @Override
    public void write(Object buf) throws Exception {
        log.trace("function of dataset: write(Object) start");
        if (!buf.equals(data))
            setData(buf);

        init();

        if (parentObject == null) {
            log.debug("write(Object): parent object is null; nowhere to write attribute to");
            return;
        }
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Dataset#copy(hdf.object.Group, java.lang.String, long[], java.lang.Object)
     */
    @Override
    public Dataset copy(Group pgroup, String dstName, long[] dims, Object buff) throws Exception {
        // not supported
        throw new UnsupportedOperationException("copy operation unsupported for NC2.");
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Dataset#readBytes()
     */
    @Override
    public byte[] readBytes() throws Exception {
        // not supported
        throw new UnsupportedOperationException("readBytes operation unsupported for NC2.");
    }

    /* Implement interface Attribute */

    /**
     * Returns the HObject to which this Attribute is currently "attached".
     *
     * @return the HObject to which this Attribute is currently "attached".
     */
    public HObject getParentObject() {
        return parentObject;
    }

    /**
     * Sets the HObject to which this Attribute is "attached".
     *
     * @param pObj
     *            the new HObject to which this Attribute is "attached".
     */
    public void setParentObject(HObject pObj) {
        parentObject = pObj;
    }

    /**
     * set a property for the attribute.
     *
     * @param key the attribute Map key
     * @param value the attribute Map value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * get a property for a given key.
     *
     * @param key the attribute Map key
     *
     * @return the property
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * get all property keys.
     *
     * @return the Collection of property keys
     */
    public Collection<String> getPropertyKeys() {
        return properties.keySet();
    }

    /**
     * Returns the name of the object. For example, "Raster Image #2".
     *
     * @return The name of the object.
     */
    public final String getAttributeName() {
        return getName();
    }

    /**
     * Retrieves the attribute data from the file.
     *
     * @return the attribute data.
     *
     * @throws Exception
     *             if the data can not be retrieved
     */
    public final Object getAttributeData() throws Exception, OutOfMemoryError {
        return getData();
    }

    /**
     * Returns the datatype of the attribute.
     *
     * @return the datatype of the attribute.
     */
    public final Datatype getAttributeDatatype() {
        return getDatatype();
    }

    /**
     * Returns the space type for the attribute. It returns a
     * negative number if it failed to retrieve the type information from
     * the file.
     *
     * @return the space type for the attribute.
     */
    public final int getAttributeSpaceType() {
        return getSpaceType();
    }

    /**
     * Returns the rank (number of dimensions) of the attribute. It returns a
     * negative number if it failed to retrieve the dimension information from
     * the file.
     *
     * @return the number of dimensions of the attribute.
     */
    public final int getAttributeRank() {
        return getRank();
    }

    /**
     * Returns the selected size of the rows and columns of the attribute. It returns a
     * negative number if it failed to retrieve the size information from
     * the file.
     *
     * @return the selected size of the rows and colums of the attribute.
     */
    public final int getAttributePlane() {
        return (int)getWidth() * (int)getHeight();
    }

    /**
     * Returns the array that contains the dimension sizes of the data value of
     * the attribute. It returns null if it failed to retrieve the dimension
     * information from the file.
     *
     * @return the dimension sizes of the attribute.
     */
    public final long[] getAttributeDims() {
        return getDims();
    }

    /**
     * @return true if the dataspace is a NULL; otherwise, returns false.
     */
    public boolean isAttributeNULL() {
        return isNULL();
    }

    /**
     * @return true if the data is a single scalar point; otherwise, returns false.
     */
    public boolean isAttributeScalar() {
        return isScalar();
    }

    /**
     * Not for public use in the future.
     *
     * setData() is not safe to use because it changes memory buffer
     * of the dataset object. Dataset operations such as write/read
     * will fail if the buffer type or size is changed.
     *
     * @param d  the object data -must be an array of Objects
     */
    public void setAttributeData(Object d) {
        setData(d);
    }

    /**
     * Writes the memory buffer of this dataset to file.
     *
     * @throws Exception if buffer can not be written
     */
    public void writeAttribute() throws Exception {
        write();
    }

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
    public void writeAttribute(Object buf) throws Exception {
        write(buf);
    }

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
    public String toAttributeString(String delimiter) {
        return toString(delimiter, -1);
    }

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
    public String toAttributeString(String delimiter, int maxItems) {
        return toString(delimiter, maxItems);
    }
}
