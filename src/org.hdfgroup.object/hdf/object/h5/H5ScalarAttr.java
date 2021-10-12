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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5DataFiltersException;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;

import hdf.object.h5.H5AttributeDataset;
import hdf.object.h5.H5Datatype;

/**
 * An attribute is a (name, value) pair of metadata attached to a primary data object such as a
 * dataset, group or named datatype.
 * <p>
 * Like a dataset, an attribute has a name, datatype and dataspace.
 * <p>
 * For more details on attributes, <a href=
 * "https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5
 * User's Guide</a>
 * <p>
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
 * AttributeDataset dataRange = new H5ScalarAttr(name, type, dims);
 * // Set the attribute value
 * dataRange.setValue(value);
 * // See FileFormat.writeAttribute() for how to attach an attribute to an object,
 * &#64;see hdf.object.FileFormat#writeAttribute(HObject, AttributeDataset, boolean)
 * </pre>
 *
 *
 * For an atomic datatype, the value of an Attribute will be a 1D array of integers, floats and
 * strings.
 *
 * @see hdf.object.Datatype
 *
 * @version 1.0 6/15/2021
 * @author Allen Byrne
 */
public class H5ScalarAttr extends H5AttributeDataset implements DataFormat {

    private static final long serialVersionUID = 2072473407027648309L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5ScalarAttr.class);

    /**
     * True if this dataset is ASCII text.
     */
    protected boolean isText;

    /**
     * flag to indicate if the datatype in file is the same as dataype in memory
     */
    private boolean isNativeDatatype = false;

    /** The fill value of the dataset. */
    protected Object fillValue = null;

    /**
     * Flag to indicate if the FillValue is converted from unsigned C.
     */
    public boolean isFillValueConverted;

    /**
     * Create an attribute with specified name, data type and dimension sizes.
     *
     * For scalar attribute, the dimension size can be either an array of size one
     * or null, and the rank can be either 1 or zero. Attribute is a general class
     * and is independent of file format, e.g., the implementation of attribute
     * applies to both HDF4 and HDF5.
     * <p>
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
     * AttributeDataset attr = new H5ScalarAttr(attrName, attrType, attrDims);
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
     * <p>
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
     * AttributeDataset attr = new H5ScalarAttr(attrName, attrType, attrDims, classValue);
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
        super(parentObj, attrName, attrType, attrDims, attrValue);

        log.trace("ScalarAttr: start {}", parentObj);

        isFillValueConverted = false;
        unsignedConverted = false;

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

        long aid = super.open();
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
     * <p>
     * The init() is designed to support lazy operation in a attribute object. When a
     * data object is retrieved from file, the datatype, dataspace and raw data are
     * not loaded into memory. When it is asked to read the raw data from file,
     * init() is first called to get the datatype and dataspace information, then
     * load the raw data from file.
     */
    @Override
    public void init() {
        if (inited) {
            resetSelection();
            log.trace("init(): ScalarAttr already inited");
            return;
        }

        long aid = HDF5Constants.H5I_INVALID_HID;
        long tid = HDF5Constants.H5I_INVALID_HID;
        long sid = HDF5Constants.H5I_INVALID_HID;
        long nativeTID = HDF5Constants.H5I_INVALID_HID;

        log.trace("init(): FILE_TYPE_HDF5");
        aid = open();
        if (aid >= 0) {
            try {
                sid = H5.H5Aget_space(aid);
                rank = H5.H5Sget_simple_extent_ndims(sid);
                space_type = H5.H5Sget_simple_extent_type(sid);
                tid = H5.H5Aget_type(aid);
                log.trace("init(): tid={} sid={} rank={} space_type={}", tid, sid, rank, space_type);

                try {
                    datatype = new H5Datatype(getFileFormat(), tid);

                    log.trace("init(): tid={} is tclass={} has isText={} : isNamed={} :  isVLEN={} : isEnum={} : isUnsigned={} : isRegRef={}",
                            tid, datatype.getDatatypeClass(), ((H5Datatype) datatype).isText(), datatype.isNamed(), datatype.isVLEN(),
                            datatype.isEnum(), datatype.isUnsigned(), ((H5Datatype) datatype).isRegRef());
                }
                catch (Exception ex) {
                    log.debug("init(): failed to create datatype for attribute: ", ex);
                    datatype = null;
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

                if (rank == 0) {
                    // a scalar data point
                    rank = 1;
                    dims = new long[1];
                    dims[0] = 1;
                    log.trace("init(): rank is a scalar data point");
                }
                else {
                    dims = new long[rank];
                    maxDims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, maxDims);
                    log.trace("init(): rank={}, dims={}, maxDims={}", rank, dims, maxDims);
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
     * Resets selection of dataspace
     */
    protected void resetSelection() {
        super.resetSelection();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#clearData()
     */
    @Override
    public void clearData() {
        super.clearData();
        unsignedConverted = false;
    }

    /**
     * Converts the data values of this Attribute to appropriate Java integers if
     * they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertFromUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertFromUnsignedC() {
        // Keep a copy of original buffer and the converted buffer
        // so that they can be reused later to save memory
        if ((data != null) && getDatatype().isUnsigned() && !unsignedConverted) {
            originalBuf = data;
            convertedBuf = convertFromUnsignedC(originalBuf, convertedBuf);
            data = convertedBuf;
            unsignedConverted = true;

            if (fillValue != null) {
                if (!isFillValueConverted) {
                    fillValue = convertFromUnsignedC(fillValue, null);
                    isFillValueConverted = true;
                }
            }
        }

        return data;
    }

    /**
     * Converts Java integer data values of this Attribute back to unsigned C-type
     * integer data if they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertToUnsignedC(Object, Object)
     * @see #convertFromUnsignedC(Object data_in)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertToUnsignedC() {
        // Keep a copy of original buffer and the converted buffer
        // so that they can be reused later to save memory
        if ((data != null) && getDatatype().isUnsigned()) {
            convertedBuf = data;
            originalBuf = convertToUnsignedC(convertedBuf, originalBuf);
            data = originalBuf;
        }

        return data;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getDatatype()
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
     * <p>
     * If data is already loaded into memory, returns the data; otherwise, calls
     * read() to read data from file into a memory buffer and returns the memory
     * buffer.
     * <p>
     * The whole attribute is read into memory. Users can also select
     * a subset from the whole data. Subsetting is done in an implicit way.
     * <p>
     * <b>How to Select a Subset</b>
     * <p>
     * A selection is specified by three arrays: start, stride and count.
     * <ol>
     * <li>start: offset of a selection
     * <li>stride: determines how many elements to move in each dimension
     * <li>count: number of elements to select in each dimension
     * </ol>
     * getStartDims(), getStride() and getSelectedDims() returns the start,
     * stride and count arrays respectively. Applications can make a selection
     * by changing the values of the arrays.
     * <p>
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
     * // when AttributeDataset.getData() is called, the selection above will be used since
     * // the dimension arrays are passed by reference. Changes of these arrays
     * // outside the attribute object directly change the values of these array
     * // in the attribute object.
     * </pre>
     * <p>
     * For ScalarAttr, the memory data buffer is a one-dimensional array of byte,
     * short, int, float, double or String type based on the datatype of the
     * attribute.
     * <p>
     *
     * @return the memory buffer of the attribute.
     *
     * @throws Exception if object can not be read
     * @throws OutOfMemoryError if memory is exhausted
     */
    @Override
    public Object getData() throws Exception, OutOfMemoryError {
        log.trace("getData(): isDataLoaded={}", isDataLoaded);
        if (!isDataLoaded) {
            data = read(); // load the data, attributes read all data
        }

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
        if (rank > 2) {
            data = AttributeSelection();
        }

        return data;
    }

    /**
     * Returns the fill values for the dataset.
     *
     * @return the fill values for the dataset.
     */
    @Override
    public final Object getFillValue() {
        return fillValue;
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
                for (int j = 0; j < selectedDims.length; j++) {
                    lsize[0] *= selectedDims[j];
                }

                tid = H5.H5Aget_type(aid);
                long size = H5.H5Tget_size(tid) * lsize[0];
                log.trace("readBytes(): size={}", size);

                if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE) throw new Exception("Invalid int size");

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
     * <p>
     * read() reads the data from file to a memory buffer and returns the memory
     * buffer. The attribute object does not hold the memory buffer. To store the
     * memory buffer in the attribute object, one must call getData().
     * <p>
     * By default, the whole attribute is read into memory.
     * <p>
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
            readData = scalarAttributeCommonIO(IO_TYPE.READ, null);
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
        super.write(buf);
        if (this.getFileFormat().isReadOnly())
            throw new Exception("cannot write to scalar attribute in file opened as read-only");

        if (!isInited())
            init();

        try {
            scalarAttributeCommonIO(IO_TYPE.WRITE, buf);
        }
        catch (Exception ex) {
            log.debug("write(Object): failed to write to scalar attribute: ", ex);
            throw new Exception("failed to write to scalar attribute: " + ex.getMessage(), ex);
        }
        resetSelection();
    }

    private Object scalarAttributeCommonIO(IO_TYPE ioType, Object writeBuf) throws Exception {
        H5Datatype dsDatatype = (H5Datatype)getDatatype();
        Object theData = null;

        /*
         * I/O type-specific pre-initialization.
         */
        if (ioType == IO_TYPE.WRITE) {
            if (writeBuf == null) {
                log.debug("scalarAttributeCommonIO(): writeBuf is null");
                throw new Exception("write buffer is null");
            }

            /*
             * Check for any unsupported datatypes and fail early before
             * attempting to write to the attribute.
             */
            if (dsDatatype.isVLEN() && !dsDatatype.isText()) {
                log.debug("scalarAttributeCommonIO(): Cannot write non-string variable-length data");
                throw new HDF5Exception("Writing non-string variable-length data is not supported");
            }

            if (dsDatatype.isRegRef()) {
                log.debug("scalarAttributeCommonIO(): Cannot write region reference data");
                throw new HDF5Exception("Writing region reference data is not supported");
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
}
