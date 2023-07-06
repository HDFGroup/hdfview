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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5DataFiltersException;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;

import hdf.object.Attribute;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;
import hdf.object.ScalarDS;

import hdf.object.h5.H5Attribute;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5ReferenceType;

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
 * Attribute dataRange = new H5ScalarAttr(name, type, dims);
 * // Set the attribute value
 * dataRange.setValue(value);
 * // See FileFormat.writeAttribute() for how to attach an attribute to an object,
 * &#64;see hdf.object.FileFormat#writeAttribute(HObject, Attribute, boolean)
 * </pre>
 *
 *
 * For an atomic datatype, the value of an Attribute will be a 1D array of integers, floats and strings.
 *
 * @see hdf.object.Datatype
 *
 * @version 1.0 6/15/2021
 * @author Allen Byrne
 */
public class H5ScalarAttr extends ScalarDS implements H5Attribute
{
    private static final long serialVersionUID = 2072473407027648309L;

    private static final Logger log = LoggerFactory.getLogger(H5ScalarAttr.class);

    /** The HObject to which this NC2Attribute is attached, Attribute interface */
    protected HObject         parentObject;

    /** additional information and properties for the attribute, Attribute interface */
    private transient Map<String, Object> properties;

    /**
     * flag to indicate if the datatype in file is the same as dataype in memory
     */
    protected boolean isNativeDatatype = false;

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
     *     attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, Datatype.NATIVE, Datatype.NATIVE);
     * }
     * catch (Exception ex) {}
     * Attribute attr = new H5ScalarAttr(attrName, attrType, attrDims);
     * attr.setValue(classValue);
     * </pre>
     *
     * @param parentObj
     *            the HObject to which this H5ScalarAttr is attached.
     * @param attrName
     *            the name of the attribute.
     * @param attrType
     *            the datatype of the attribute.
     * @param attrDims
     *            the dimension sizes of the attribute, null for scalar attribute
     *
     * @see hdf.object.Datatype
     */
    public H5ScalarAttr(HObject parentObj, String attrName, Datatype attrType, long[] attrDims) {
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
     *     attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, Datatype.NATIVE, Datatype.NATIVE);
     * }
     * catch (Exception ex) {}
     * Attribute attr = new H5ScalarAttr(attrName, attrType, attrDims, classValue);
     * </pre>
     *
     * @param parentObj
     *            the HObject to which this H5ScalarAttr is attached.
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
    public H5ScalarAttr(HObject parentObj, String attrName, Datatype attrType, long[] attrDims, Object attrValue) {
        super((parentObj == null) ? null : parentObj.getFileFormat(), attrName,
                (parentObj == null) ? null : parentObj.getFullName(), null);

        log.trace("H5ScalarAttr: start {}", parentObj);
        this.parentObject = parentObj;

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
            isScalar = true;
        }
        else {
            dims = attrDims;
            rank = dims.length;
            isScalar = false;
        }

        selectedDims = new long[rank];
        startDims = new long[rank];
        selectedStride = new long[rank];

        log.trace("attrName={}, attrType={}, attrValue={}, rank={}, isUnsigned={}",
                attrName, attrType.getDescription(), data, rank, getDatatype().isUnsigned());

        resetSelection();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        if (parentObject == null) {
            log.debug("open(): attribute's parent object is null");
            return HDF5Constants.H5I_INVALID_HID;
        }

        long aid = HDF5Constants.H5I_INVALID_HID;
        long pObjID = HDF5Constants.H5I_INVALID_HID;

        try {
            pObjID = parentObject.open();
            if (pObjID >= 0) {
                if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                    log.trace("open(): FILE_TYPE_HDF5");
                    if (H5.H5Aexists(pObjID, getName()))
                        aid = H5.H5Aopen(pObjID, getName(), HDF5Constants.H5P_DEFAULT);
                }
            }

            log.trace("open(): aid={}", aid);
        }
        catch (Exception ex) {
            log.debug("open(): Failed to open attribute {}: ", getName(), ex);
            aid = HDF5Constants.H5I_INVALID_HID;
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
            if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                log.trace("close(): FILE_TYPE_HDF5");
                try {
                    H5.H5Aclose(aid);
                }
                catch (HDF5Exception ex) {
                    log.debug("close(): H5Aclose({}) failure: ", aid, ex);
                }
            }
        }
    }

    /**
     * Retrieves datatype and dataspace information from file and sets the attribute
     * in memory.
     *
     * The init() is designed to support lazy operation in a attribute object. When a
     * data object is retrieved from file, the datatype, dataspace and raw data are
     * not loaded into memory. When it is asked to read the raw data from file,
     * init() is first called to get the datatype and dataspace information, then
     * load the raw data from file.
     */
    @Override
    public void init() {
        if (inited) {
            // already called. Initialize only once
            resetSelection();
            log.trace("init(): H5ScalarAttr already initialized");
            return;
        }

        long aid = HDF5Constants.H5I_INVALID_HID;
        long tid = HDF5Constants.H5I_INVALID_HID;
        long sid = HDF5Constants.H5I_INVALID_HID;
        long nativeTID = HDF5Constants.H5I_INVALID_HID;

        aid = open();
        if (aid >= 0) {
            try {
                sid = H5.H5Aget_space(aid);
                rank = H5.H5Sget_simple_extent_ndims(sid);
                space_type = H5.H5Sget_simple_extent_type(sid);
                if (space_type == HDF5Constants.H5S_NULL)
                    isNULL = true;
                else
                    isNULL = false;
                tid = H5.H5Aget_type(aid);
                log.trace("init(): tid={} sid={} rank={} space_type={}", tid, sid, rank, space_type);

                if (rank == 0) {
                    // a scalar data point
                    isScalar = true;
                    rank = 1;
                    dims = new long[] { 1 };
                    log.trace("init(): rank is a scalar data point");
                }
                else {
                    isScalar = false;
                    dims = new long[rank];
                    maxDims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, maxDims);
                    log.trace("init(): rank={}, dims={}, maxDims={}", rank, dims, maxDims);
                }

                if (datatype == null) {
                    try {
                        int nativeClass = H5.H5Tget_class(tid);
                        if (nativeClass == HDF5Constants.H5T_REFERENCE) {
                            long lsize = 1;
                            if (rank > 0) {
                                log.trace("init(): rank={}, dims={}", rank, dims);
                                for (int j = 0; j < dims.length; j++) {
                                    lsize *= dims[j];
                                }
                            }
                            datatype = new H5ReferenceType(getFileFormat(), lsize, tid);
                        }
                        else
                            datatype = new H5Datatype(getFileFormat(), tid);

                        log.trace("init(): tid={} is tclass={} has isText={} : isNamed={} :  isVLEN={} : isEnum={} : isUnsigned={} : isRegRef={}",
                                tid, datatype.getDatatypeClass(), ((H5Datatype) datatype).isText(), datatype.isNamed(), datatype.isVLEN(),
                                datatype.isEnum(), datatype.isUnsigned(), ((H5Datatype) datatype).isRegRef());
                    }
                    catch (Exception ex) {
                        log.debug("init(): failed to create datatype for attribute: ", ex);
                        datatype = null;
                    }
                }

                // Check if the datatype in the file is the native datatype
                try {
                    nativeTID = H5.H5Tget_native_type(tid);
                    isNativeDatatype = H5.H5Tequal(tid, nativeTID);
                    log.trace("init(): isNativeDatatype={}", isNativeDatatype);
                }
                catch (Exception ex) {
                    log.debug("init(): check if native type failure: ", ex);
                }

                inited = true;
            }
            catch (HDF5Exception ex) {
                log.debug("init(): ", ex);
            }
            finally {
                try {
                    H5.H5Tclose(nativeTID);
                }
                catch (Exception ex2) {
                    log.debug("init(): H5Tclose(nativeTID {}) failure: ", nativeTID, ex2);
                }
                try {
                    H5.H5Tclose(tid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("init(): H5Tclose(tid {}) failure: ", tid, ex2);
                }
                try {
                    H5.H5Sclose(sid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("init(): H5Sclose(sid {}) failure: ", sid, ex2);
                }

            }

            close(aid);

            startDims = new long[rank];
            selectedDims = new long[rank];

            resetSelection();
        }
        else {
            log.debug("init(): failed to open attribute");
        }
    }

    /**
     * Returns the datatype of the data object.
     *
     * @return the datatype of the data object.
     */
    @Override
    public Datatype getDatatype() {
        if (!inited)
            init();

        if (datatype == null) {
            long aid = HDF5Constants.H5I_INVALID_HID;
            long tid = HDF5Constants.H5I_INVALID_HID;

            aid = open();
            if (aid >= 0) {
                try {
                    tid = H5.H5Aget_type(aid);
                    log.trace("getDatatype(): isNativeDatatype={}", isNativeDatatype);
                    if (!isNativeDatatype) {
                        long tmptid = -1;
                        try {
                            tmptid = H5Datatype.toNative(tid);
                            if (tmptid >= 0) {
                                try {
                                    H5.H5Tclose(tid);
                                }
                                catch (Exception ex2) {
                                    log.debug("getDatatype(): H5Tclose(tid {}) failure: ", tid, ex2);
                                }
                                tid = tmptid;
                            }
                        }
                        catch (Exception ex) {
                            log.debug("getDatatype(): toNative: ", ex);
                        }
                    }
                    int nativeClass = H5.H5Tget_class(tid);
                    if (nativeClass == HDF5Constants.H5T_REFERENCE) {
                        long lsize = 1;
                        long sid = H5.H5Aget_space(aid);
                        int rank = H5.H5Sget_simple_extent_ndims(sid);
                        if (rank > 0) {
                            long dims[] = new long[rank];
                            H5.H5Sget_simple_extent_dims(sid, dims, null);
                            log.trace("getDatatype(): rank={}, dims={}", rank, dims);
                            for (int j = 0; j < dims.length; j++) {
                                lsize *= dims[j];
                            }
                        }
                        datatype = new H5ReferenceType(getFileFormat(), lsize, tid);
                    }
                    else
                        datatype = new H5Datatype(getFileFormat(), tid);
                }
                catch (Exception ex) {
                    log.debug("getDatatype(): ", ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tid);
                    }
                    catch (HDF5Exception ex) {
                        log.debug("getDatatype(): H5Tclose(tid {}) failure: ", tid, ex);
                    }
                    try {
                        H5.H5Aclose(aid);
                    }
                    catch (HDF5Exception ex) {
                        log.debug("getDatatype(): H5Aclose(aid {}) failure: ", aid, ex);
                    }
                }
            }
        }

        return datatype;
    }

    /**
     * Returns the data buffer of the attribute in memory.
     *
     * If data is already loaded into memory, returns the data; otherwise, calls
     * read() to read data from file into a memory buffer and returns the memory
     * buffer.
     *
     * The whole attribute is read into memory. Users can also select
     * a subset from the whole data. Subsetting is done in an implicit way.
     *
     * <b>How to Select a Subset</b>
     *
     * A selection is specified by three arrays: start, stride and count.
     * <ol>
     * <li>start: offset of a selection
     * <li>stride: determines how many elements to move in each dimension
     * <li>count: number of elements to select in each dimension
     * </ol>
     * getStartDims(), getStride() and getSelectedDims() returns the start,
     * stride and count arrays respectively. Applications can make a selection
     * by changing the values of the arrays.
     *
     * The following example shows how to make a subset. In the example, the
     * attribute is a 4-dimensional array of [200][100][50][10], i.e. dims[0]=200;
     * dims[1]=100; dims[2]=50; dims[3]=10; <br>
     * We want to select every other data point in dims[1] and dims[2]
     *
     * <pre>
     * int rank = attribute.getRank(); // number of dimensions of the attribute
     * long[] dims = attribute.getDims(); // the dimension sizes of the attribute
     * long[] selected = attribute.getSelectedDims(); // the selected size of the attribute
     * long[] start = attribute.getStartDims(); // the offset of the selection
     * long[] stride = attribute.getStride(); // the stride of the attribute
     * int[] selectedIndex = attribute.getSelectedIndex(); // the selected dimensions for display
     *
     * // select dim1 and dim2 as 2D data for display,and slice through dim0
     * selectedIndex[0] = 1;
     * selectedIndex[1] = 2;
     * selectedIndex[2] = 0;
     *
     * // reset the selection arrays
     * for (int i = 0; i &lt; rank; i++) {
     *     start[i] = 0;
     *     selected[i] = 1;
     *     stride[i] = 1;
     * }
     *
     * // set stride to 2 on dim1 and dim2 so that every other data point is
     * // selected.
     * stride[1] = 2;
     * stride[2] = 2;
     *
     * // set the selection size of dim1 and dim2
     * selected[1] = dims[1] / stride[1];
     * selected[2] = dims[1] / stride[2];
     *
     * // when H5ScalarAttr.getData() is called, the selection above will be used since
     * // the dimension arrays are passed by reference. Changes of these arrays
     * // outside the attribute object directly change the values of these array
     * // in the attribute object.
     * </pre>
     *
     * For H5ScalarAttr, the memory data buffer is a one-dimensional array of byte,
     * short, int, float, double or String type based on the datatype of the
     * attribute.
     *
     * @return the memory buffer of the attribute.
     *
     * @throws Exception if object can not be read
     * @throws OutOfMemoryError if memory is exhausted
     */
    @Override
    public Object getData() throws Exception, OutOfMemoryError {
        log.trace("getData(): isDataLoaded={}", isDataLoaded);
        if (!isDataLoaded)
            data = read(); // load the data, attributes read all data

        nPoints = 1;
        log.trace("getData(): selectedDims length={}", selectedDims.length);
        int point_len = selectedDims.length;
        //Partial data for 3 or more dimensions
        if (rank > 2)
            point_len = 3;
        for (int j = 0; j < point_len; j++) {
            log.trace("getData(): selectedDims[{}]={}", j, selectedDims[j]);
            nPoints *= selectedDims[j];
        }
        log.trace("getData: read {}", nPoints);

        // apply the selection for 3 or more dimensions
        // selection only expects to use 3 selectedDims
        //     where selectedIndex[0] is the row dimension
        //     where selectedIndex[1] is the col dimension
        //     where selectedIndex[2] is the frame dimension
        if (rank > 2)
            data = AttributeSelection();

        return data;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Dataset#copy(hdf.object.Group, java.lang.String, long[], java.lang.Object)
     */
    @Override
    public Dataset copy(Group pgroup, String dstName, long[] dims, Object buff) throws Exception {
        // not supported
        throw new UnsupportedOperationException("copy operation unsupported for H5.");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Attribute#readBytes()
     */
    @Override
    public byte[] readBytes() throws HDF5Exception {
        byte[] theData = null;

        if (!isInited())
            init();

        long aid = open();
        if (aid >= 0) {
            long tid = HDF5Constants.H5I_INVALID_HID;

            try {
                long[] lsize = { 1 };
                for (int j = 0; j < selectedDims.length; j++)
                    lsize[0] *= selectedDims[j];

                tid = H5.H5Aget_type(aid);
                long size = H5.H5Tget_size(tid) * lsize[0];
                log.trace("readBytes(): size = {}", size);

                if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE)
                    throw new Exception("Invalid int size");

                theData = new byte[(int)size];

                log.trace("readBytes(): read attribute id {} of size={}", tid, lsize);
                H5.H5Aread(aid, tid, theData);
            }
            catch (Exception ex) {
                log.debug("readBytes(): failed to read data: ", ex);
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("readBytes(): H5Tclose(tid {}) failure: ", tid, ex2);
                }
                close(aid);
            }
        }

        return theData;
    }

    /**
     * Reads the data from file.
     *
     * read() reads the data from file to a memory buffer and returns the memory
     * buffer. The attribute object does not hold the memory buffer. To store the
     * memory buffer in the attribute object, one must call getData().
     *
     * By default, the whole attribute is read into memory.
     *
     * For ScalarAttr, the memory data buffer is a one-dimensional array of byte,
     * short, int, float, double or String type based on the datatype of the
     * attribute.
     *
     * @return the data read from file.
     *
     * @see #getData()
     * @see hdf.object.DataFormat#read()
     *
     * @throws Exception
     *             if object can not be read
     */
    @Override
    public Object read() throws Exception {
        Object readData = null;

        if (!isInited())
            init();

        try {
            readData = scalarAttributeCommonIO(H5File.IO_TYPE.READ, null);
        }
        catch (Exception ex) {
            log.debug("read(): failed to read scalar attribute: ", ex);
            throw new Exception("failed to read scalar attribute: " + ex.getMessage(), ex);
        }

        return readData;
    }

    /**
     * Writes the given data buffer into this attribute in a file.
     *
     * @param buf
     *            The buffer that contains the data values.
     *
     * @throws Exception
     *             If there is an error at the HDF5 library level.
     */
    @Override
    public void write(Object buf) throws Exception {
        if (this.getFileFormat().isReadOnly())
            throw new Exception("cannot write to scalar attribute in file opened as read-only");

        if (!buf.equals(data))
            setData(buf);

        if (parentObject == null) {
            log.debug("write(Object): parent object is null; nowhere to write attribute to");
            return;
        }

        ((MetaDataContainer) getParentObject()).writeMetadata(this);

        try {
            scalarAttributeCommonIO(H5File.IO_TYPE.WRITE, buf);
        }
        catch (Exception ex) {
            log.debug("write(Object): failed to write to scalar attribute: ", ex);
            throw new Exception("failed to write to scalar attribute: " + ex.getMessage(), ex);
        }
        resetSelection();
    }

    private Object scalarAttributeCommonIO(H5File.IO_TYPE ioType, Object writeBuf) throws Exception {
        H5Datatype dsDatatype = (H5Datatype)getDatatype();
        Object theData = null;

        /*
         * I/O type-specific pre-initialization.
         */
        if (ioType == H5File.IO_TYPE.WRITE) {
            if (writeBuf == null) {
                log.debug("scalarAttributeCommonIO(): writeBuf is null");
                throw new Exception("write buffer is null");
            }
        }

        long aid = open();
        if (aid >= 0) {
            log.trace("scalarAttributeCommonIO(): isDataLoaded={}", isDataLoaded);
            try {
                theData = AttributeCommonIO(aid, ioType, writeBuf);
            }
            finally {
                close(aid);
            }
        }
        else
            log.debug("scalarAttributeCommonIO(): failed to open attribute");

        return theData;
    }

    /* Implement interface Attribute */

    /**
     * Returns the HObject to which this Attribute is currently "attached".
     *
     * @return the HObject to which this Attribute is currently "attached".
     */
    @Override
    public HObject getParentObject() {
        return parentObject;
    }

    /**
     * Sets the HObject to which this Attribute is "attached".
     *
     * @param pObj
     *            the new HObject to which this Attribute is "attached".
     */
    @Override
    public void setParentObject(HObject pObj) {
        parentObject = pObj;
    }

    /**
     * set a property for the attribute.
     *
     * @param key the attribute Map key
     * @param value the attribute Map value
     */
    @Override
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
    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * get all property keys.
     *
     * @return the Collection of property keys
     */
    @Override
    public Collection<String> getPropertyKeys() {
        return properties.keySet();
    }

    /**
     * Returns the name of the object. For example, "Raster Image #2".
     *
     * @return The name of the object.
     */
    @Override
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
    @Override
    public final Object getAttributeData() throws Exception, OutOfMemoryError {
        return getData();
    }

    /**
     * Returns the datatype of the attribute.
     *
     * @return the datatype of the attribute.
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public final long[] getAttributeDims() {
        return getDims();
    }

    /**
     * @return true if the dataspace is a NULL; otherwise, returns false.
     */
    @Override
    public boolean isAttributeNULL() {
        return isNULL();
    }

    /**
     * @return true if the data is a single scalar point; otherwise, returns false.
     */
    @Override
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
    @Override
    public void setAttributeData(Object d) {
        setData(d);
    }

    /**
     * Writes the memory buffer of this dataset to file.
     *
     * @throws Exception if buffer can not be written
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
    public String toAttributeString(String delimiter, int maxItems) {
        Object theData = originalBuf;
        if (theData == null) {
            log.debug("toAttributeString: value is null");
            return null;
        }

        Class<? extends Object> valClass = theData.getClass();
        if (!valClass.isArray() && !getDatatype().isRef()) {
            log.trace("toAttributeString: finish - not array");
            String strValue = theData.toString();
            if (maxItems > 0 && strValue.length() > maxItems)
                // truncate the extra characters
                strValue = strValue.substring(0, maxItems);
            return strValue;
        }

        int n = 0;
        Datatype dtype = getDatatype();
        // value is an array
        if (valClass.isArray()) {
            n = Array.getLength(theData);
            if (dtype.isRef())
                n /= (int)dtype.getDatatypeSize();
        }
        else
            n = ((ArrayList<Object[]>)theData).size();
        if ((maxItems > 0) && (n > maxItems))
            n = maxItems;

        return toString(theData, dtype, delimiter, n);
    }

    @Override
    protected String toString(Object theData, Datatype theType, String delimiter, int count) {
        log.trace("toString: is_enum={} is_unsigned={} count={}", theType.isEnum(),
                theType.isUnsigned(), count);
        StringBuilder sb = new StringBuilder();
        Class<? extends Object> valClass = theData.getClass();
        log.trace("toString:valClass={}", valClass);

        H5Datatype dtype = (H5Datatype)theType;
        log.trace("toString: count={} isStdRef={}", count, dtype.isStdRef());
        if (dtype.isStdRef()) {
            return ((H5ReferenceType)dtype).toString(delimiter, count);
        }
        else if (dtype.isVLEN() && !dtype.isVarStr()) {
            log.trace("toString: vlen");
            String strValue;

            for (int k = 0; k < count; k++) {
                Object value = Array.get(theData, k);
                if (value == null)
                    strValue = "null";
                else {
                    if (dtype.getDatatypeBase().isRef()) {
                        ArrayList<byte[]> ref_value = (ArrayList<byte[]>)value;
                        log.trace("toString: vlen value={}", ref_value);
                        strValue = "{";
                        for (int m = 0; m < ref_value.size(); m++) {
                            byte[] curBytes = ref_value.get(m);
                            if (m > 0)
                                strValue += ", ";
                            if (H5ReferenceType.zeroArrayCheck(curBytes))
                                strValue += "NULL";
                            else {
                                if (((H5Datatype)dtype.getDatatypeBase()).isStdRef()) {
                                    strValue += H5.H5Rget_obj_name(curBytes, HDF5Constants.H5P_DEFAULT);
                                }
                                else if (dtype.getDatatypeBase().getDatatypeSize() == HDF5Constants.H5R_DSET_REG_REF_BUF_SIZE) {
                                    try {
                                        strValue += H5Datatype.descRegionDataset(parentObject.getFileFormat().getFID(), curBytes);
                                    }
                                    catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                else if (dtype.getDatatypeBase().getDatatypeSize() == HDF5Constants.H5R_OBJ_REF_BUF_SIZE) {
                                    try {
                                        strValue += H5Datatype.descReferenceObject(parentObject.getFileFormat().getFID(), curBytes);
                                    }
                                    catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
                        strValue += "}";
                    }
                    else
                        strValue = value.toString();
                }
                if (k > 0)
                    sb.append(", ");
                sb.append(strValue);
            }
        }
        else if (dtype.isRef()) {
            log.trace("toString: ref");
            String strValue = "NULL";
            byte[] rElements = null;

            for (int k = 0; k < count; k++) {
                // need to iterate if type is ArrayList
                if (theData instanceof ArrayList)
                    rElements = (byte[]) ((ArrayList) theData).get(k);
                else
                    rElements = (byte[]) Array.get(theData, k);

                if (H5ReferenceType.zeroArrayCheck(rElements))
                    strValue = "NULL";
                else {
                    if (dtype.isStdRef()) {
                        strValue = H5.H5Rget_obj_name(rElements, HDF5Constants.H5P_DEFAULT);
                    }
                    else if (dtype.getDatatypeSize() == HDF5Constants.H5R_DSET_REG_REF_BUF_SIZE) {
                        try {
                            strValue = H5Datatype.descRegionDataset(parentObject.getFileFormat().getFID(), rElements);
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    else if (dtype.getDatatypeSize() == HDF5Constants.H5R_OBJ_REF_BUF_SIZE) {
                        try {
                            strValue = H5Datatype.descReferenceObject(parentObject.getFileFormat().getFID(), rElements);
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                if (k > 0)
                    sb.append(", ");
                sb.append(strValue);
            }
        }
        else {
            return super.toString(theData, theType, delimiter, count);
        }

        return sb.toString();
    }

    /* Implement interface H5Attribute */

    /**
     * The general read and write attribute operations for hdf5 object data.
     *
     * @param attr_id
     *        the attribute to access
     * @param ioType
     *        the type of IO operation
     * @param objBuf
     *        the data buffer to use for write operation
     *
     * @return the attribute data
     *
     * @throws Exception
     *             if the data can not be retrieved
     */
    @Override
    public Object AttributeCommonIO(long attr_id, H5File.IO_TYPE ioType, Object objBuf) throws Exception {
        H5Datatype dsDatatype = (H5Datatype) getDatatype();
        Object theData = null;

        long dt_size = dsDatatype.getDatatypeSize();
        log.trace("AttributeCommonIO(): create native");
        long tid = dsDatatype.createNative();

        if (ioType == H5File.IO_TYPE.READ) {
            log.trace("AttributeCommonIO():read ioType isNamed={} isEnum={} isText={} isRefObj={}", dsDatatype.isNamed(), dsDatatype.isEnum(), dsDatatype.isText(), dsDatatype.isRefObj());
            log.trace("AttributeCommonIO():read ioType isVLEN={}", dsDatatype.isVLEN());

            long lsize = 1;
            for (int j = 0; j < dims.length; j++)
                lsize *= dims[j];
            log.trace("AttributeCommonIO():read ioType dt_size={} lsize={}", dt_size, lsize);

            try {
                if (dsDatatype.isVarStr()) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++)
                        strs[j] = "";
                    try {
                        log.trace("AttributeCommonIO():read ioType H5Aread_VLStrings");
                        H5.H5Aread_VLStrings(attr_id, tid, strs);
                    }
                    catch (Exception ex) {
                        log.debug("AttributeCommonIO():read ioType H5Aread_VLStrings failure: ", ex);
                        ex.printStackTrace();
                    }
                    theData = strs;
                }
                else if (dsDatatype.isCompound()) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++)
                        strs[j] = "";
                    try {
                        log.trace("AttributeCommonIO():read ioType H5AreadComplex");
                        H5.H5AreadComplex(attr_id, tid, strs);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    theData = strs;
                }
                else if (dsDatatype.isVLEN()) {
                    log.trace("AttributeCommonIO():read ioType:VLEN-REF H5Aread isArray()={}", dsDatatype.isArray());
                    theData = new ArrayList[(int)lsize];
                    for (int j = 0; j < lsize; j++)
                        ((ArrayList[])theData)[j] = new ArrayList<byte[]>();

                        try {
                            H5.H5AreadVL(attr_id, tid, (Object[])theData);
                        }
                        catch (Exception ex) {
                            log.debug("AttributeCommonIO():read ioType:VLEN-REF H5Aread failure: ", ex);
                            ex.printStackTrace();
                        }
                }
                else {
                    Object attr_data = null;
                    try {
                        attr_data = H5Datatype.allocateArray(dsDatatype, (int) lsize);
                    }
                    catch (OutOfMemoryError e) {
                        log.debug("AttributeCommonIO():read ioType out of memory", e);
                        theData = null;
                    }
                    if (attr_data == null)
                        log.debug("AttributeCommonIO():read ioType allocateArray returned null");

                    log.trace("AttributeCommonIO():read ioType H5Aread isArray()={}", dsDatatype.isArray());
                    try {
                        H5.H5Aread(attr_id, tid, attr_data);
                    }
                    catch (Exception ex) {
                        log.debug("AttributeCommonIO():read ioType H5Aread failure: ", ex);
                        ex.printStackTrace();
                    }

                    /*
                     * Perform any necessary data conversions.
                     */
                    if (dsDatatype.isText() && convertByteToString && (attr_data instanceof byte[])) {
                        log.trace("AttributeCommonIO():read ioType isText: converting byte array to string array");
                        theData = byteToString((byte[]) attr_data, (int) dsDatatype.getDatatypeSize());
                    }
                    else if (dsDatatype.isFloat() && dt_size == 16) {
                        log.trace("AttributeCommonIO():read ioType isFloat: converting byte array to BigDecimal array");
                        theData = dsDatatype.byteToBigDecimal(0, (int)nPoints, (byte[]) attr_data);
                    }
                    else if (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isFloat() && dsDatatype.getDatatypeBase().getDatatypeSize() == 16) {
                        log.trace("AttributeCommonIO():read ioType isArray and isFloat: converting byte array to BigDecimal array");
                        long[] arrayDims = dsDatatype.getArrayDims();
                        int asize = (int)nPoints;
                        for (int j = 0; j < arrayDims.length; j++) {
                            asize *= arrayDims[j];
                        }
                        theData = ((H5Datatype)dsDatatype.getDatatypeBase()).byteToBigDecimal(0, asize, (byte[]) attr_data);
                    }
                    else if (dsDatatype.isRef() && (attr_data instanceof byte[])) {
                        log.trace("AttributeCommonIO():read ioType isRef: converting byte array to List of bytes");
                        theData = new ArrayList<byte[]>((int)lsize);
                        for (int m = 0; m < (int) lsize; m++) {
                            byte[] curBytes = new byte[(int)dsDatatype.getDatatypeSize()];
                            try {
                                System.arraycopy(attr_data, m * (int)dt_size, curBytes, 0, (int)dsDatatype.getDatatypeSize());
                                ((ArrayList<byte[]>)theData).add(curBytes);
                            }
                            catch (Exception err) {
                                log.trace("AttributeCommonIO(): arraycopy failure: ", err);
                            }
                        }
                    }
                    else
                        theData = attr_data;
                }
            }
            catch (HDF5DataFiltersException exfltr) {
                log.debug("AttributeCommonIO():read ioType read failure: ", exfltr);
                throw new Exception("Filter not available exception: " + exfltr.getMessage(), exfltr);
            }
            catch (Exception ex) {
                log.debug("AttributeCommonIO():read ioType read failure: ", ex);
                throw new Exception(ex.getMessage(), ex);
            }
            finally {
                dsDatatype.close(tid);
            }
            log.trace("AttributeCommonIO():read ioType data: {}", theData);
            originalBuf = theData;
            isDataLoaded = true;
        } // H5File.IO_TYPE.READ
        else {
            /*
             * Perform any necessary data conversions before writing the data.
             *
             * Note that v-len strings do not get converted, regardless of
             * conversion request type.
             */
            Object tmpData = objBuf;
            try {
                // Check if we need to convert integer data
                String cname = objBuf.getClass().getName();
                char dname = cname.charAt(cname.lastIndexOf("[") + 1);
                boolean doIntConversion = (((dt_size == 1) && (dname == 'S')) || ((dt_size == 2) && (dname == 'I'))
                        || ((dt_size == 4) && (dname == 'J')) || (dsDatatype.isUnsigned() && unsignedConverted));

                if (doIntConversion) {
                    log.trace("AttributeCommonIO(): converting integer data to unsigned C-type integers");
                    tmpData = convertToUnsignedC(objBuf, null);
                }
                else if (dsDatatype.isText() && !dsDatatype.isVarStr() && convertByteToString && !(objBuf instanceof byte[])) {
                    log.trace("AttributeCommonIO(): converting string array to byte array");
                    tmpData = stringToByte((String[]) objBuf, (int)dt_size);
                }
                else if (dsDatatype.isEnum() && (Array.get(objBuf, 0) instanceof String)) {
                    log.trace("AttributeCommonIO(): converting enum names to values");
                    tmpData = dsDatatype.convertEnumNameToValue((String[]) objBuf);
                }
                else if (dsDatatype.isFloat() && dsDatatype.getDatatypeSize() == 16) {
                    log.trace("AttributeCommonIO(): isFloat: converting BigDecimal array to byte array");
                    throw new Exception("data conversion failure: cannot write BigDecimal values");
                    //tmpData = dsDatatype.bigDecimalToByte(0, (int)nPoints, (BigDecimal[]) objBuf);
                }
            }
            catch (Exception ex) {
                log.debug("AttributeCommonIO(): data conversion failure: ", ex);
                throw new Exception("data conversion failure: " + ex.getMessage());
            }

            /*
             * Actually write the data now that everything has been setup.
             */
            try {
                if (dsDatatype.isVarStr()) {
                    log.trace("AttributeCommonIO(): H5Awrite_VLStrings aid={} tid={}", attr_id, tid);

                    H5.H5Awrite_VLStrings(attr_id, tid, (Object[]) tmpData);
                }
                else if (dsDatatype.isVLEN() || (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isVLEN())) {
                    log.trace("AttributeCommonIO(): H5AwriteVL aid={} tid={}", attr_id, tid);

                    H5.H5AwriteVL(attr_id, tid, (Object[]) tmpData);
                }
                else {
                    log.trace("AttributeCommonIO(): dsDatatype.isRef()={} data is String={}", dsDatatype.isRef(), tmpData instanceof String);
                    if (dsDatatype.isRef() && tmpData instanceof String) {
                        // reference is a path+name to the object
                        log.trace("AttributeCommonIO(): Attribute class is CLASS_REFERENCE");
                        log.trace("AttributeCommonIO(): H5Awrite aid={} tid={}", attr_id, tid);
                        byte[] refBuf = H5.H5Rcreate_object(getFID(), (String) tmpData, HDF5Constants.H5P_DEFAULT);
                        if (refBuf != null) {
                            H5.H5Awrite(attr_id, tid, refBuf);
                            H5.H5Rdestroy(refBuf);
                        }
                    }
                    else if (Array.get(tmpData, 0) instanceof String) {
                        int len = ((String[]) tmpData).length;
                        byte[] bval = Dataset.stringToByte((String[]) tmpData, (int)dt_size);
                        if (bval != null && bval.length == dt_size * len) {
                            bval[bval.length - 1] = 0;
                            tmpData = bval;
                        }
                        log.trace("AttributeCommonIO(): String={}: {}", tmpData);
                        log.trace("AttributeCommonIO(): H5Awrite aid={} tid={}", attr_id, tid);
                        H5.H5Awrite(attr_id, tid, tmpData);
                    }
                    else {
                        log.trace("AttributeCommonIO(): H5Awrite aid={} tid={}", attr_id, tid);
                        H5.H5Awrite(attr_id, tid, tmpData);
                    }
                }
            }
            catch (Exception ex) {
                log.debug("AttributeCommonIO(): write failure: ", ex);
                throw new Exception(ex.getMessage());
            }
            finally {
                dsDatatype.close(tid);
            }
        } // H5File.IO_TYPE.WRITE

        return theData;
    }

    /**
     * Read a subset of an attribute for hdf5 object data.
     *
     * @return the selected attribute data
     *
     * @throws Exception
     *             if the data can not be retrieved
     */
    @Override
    public Object AttributeSelection() throws Exception {
        H5Datatype dsDatatype = (H5Datatype) getDatatype();
        int dsSize = (int)dsDatatype.getDatatypeSize();
        if (dsDatatype.isArray())
            dsSize = (int)dsDatatype.getDatatypeBase().getDatatypeSize();
        Object theData = H5Datatype.allocateArray(dsDatatype, (int)nPoints);
        if (dsDatatype.isText() && convertByteToString && (theData instanceof byte[])) {
            log.trace("scalarAttributeSelection(): isText: converting byte array to string array");
            theData = byteToString((byte[]) theData, dsSize);
        }
        else if (dsDatatype.isFloat() && dsSize == 16) {
            log.trace("scalarAttributeSelection(): isFloat: converting byte array to BigDecimal array");
            theData = dsDatatype.byteToBigDecimal(0, (int)nPoints, (byte[]) theData);
        }
        else if (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isFloat() && dsSize == 16) {
            log.trace("scalarAttributeSelection(): isArray and isFloat: converting byte array to BigDecimal array");
            long[] arrayDims = dsDatatype.getArrayDims();
            int asize = (int)nPoints;
            for (int j = 0; j < arrayDims.length; j++) {
                asize *= arrayDims[j];
            }
            theData = ((H5Datatype)dsDatatype.getDatatypeBase()).byteToBigDecimal(0, asize, (byte[]) theData);
        }
        Object theOrig = originalBuf;
        log.trace("scalarAttributeSelection(): originalBuf={} with datatype size={}", originalBuf, dsSize);

        //Copy the selection from originalBuf to theData
        //Only three dims are involved and selected data is 2 dimensions
        //    getHeight() is the row dimension
        //    getWidth() is the col dimension
        //    getDepth() is the frame dimension
        long[] start = getStartDims();
        long curFrame = start[selectedIndex[2]];
        int k = (int)startDims[selectedIndex[2]] * (int)getDepth();
        for (int col = 0; col < (int)getWidth(); col++) {
            for (int row = 0; row < (int)getHeight(); row++) {
                int index = row * (int)getWidth() + col;
                log.trace("scalarAttributeSelection(): point[{}] row:col:k={}:{}:{}", curFrame, row, col, k);
                int fromIndex = ((int)curFrame * (int)getWidth() * (int)getHeight() +
                        col * (int)getHeight() + row);
                int toIndex = (col * (int)getHeight() + row);
                int objSize = 1;
                if (dsDatatype.isArray()) {
                    long[] arrayDims = dsDatatype.getArrayDims();
                    objSize = arrayDims.length;
                    try {
                        System.arraycopy(theOrig, fromIndex, theData, toIndex, objSize);
                    }
                    catch (Exception err) {
                        log.debug("scalarAttributeSelection(): arraycopy failure: ", err);
                    }
                }
                else if (dsDatatype.isStdRef()) {
                    objSize = (int)HDF5Constants.H5R_REF_BUF_SIZE;
                    fromIndex = fromIndex * HDF5Constants.H5R_REF_BUF_SIZE;
                    toIndex = toIndex * HDF5Constants.H5R_REF_BUF_SIZE;
                    try {
                        System.arraycopy(theOrig, fromIndex, theData, toIndex, objSize);
                    }
                    catch (Exception err) {
                        log.debug("scalarAttributeSelection(): arraycopy failure: ", err);
                    }
                }
                else {
                    if (theOrig instanceof ArrayList) {
                        if (dsDatatype.isRef()) {
                            byte[] rElements = (byte[]) ((ArrayList) theOrig).get(fromIndex);
                            try {
                                System.arraycopy(rElements, 0, theData, toIndex * dsSize, dsSize);
                            }
                            catch (Exception err) {
                                log.trace("scalarAttributeSelection(): refarraycopy failure: ", err);
                            }
                        }
                        else {
                            Object value = Array.get(theOrig, fromIndex);
                            log.trace("scalarAttributeSelection(): value={}", value);
                            ((ArrayList<Object>)theData).add(toIndex, value);
                        }
                    }
                    else
                        theData = theOrig;
                }
            }
        }

        log.trace("scalarAttributeSelection(): theData={}", theData);
        return theData;
    }
}
