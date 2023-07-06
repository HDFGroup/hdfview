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

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;
import hdf.object.Utils;

import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5ReferenceType;

/**
 * The H5CompoundAttr class defines an HDF5 attribute of compound datatypes.
 *
 * An attribute is a (name, value) pair of metadata attached to a primary data object such as a dataset, group or named
 * datatype.
 *
 * Like a dataset, an attribute has a name, datatype and dataspace.
 *
 * A HDF5 compound datatype is similar to a struct in C or a common block in Fortran: it is a collection of one or more
 * atomic types or small arrays of such types. Each member of a compound type has a name which is unique within that
 * type, and a byte offset that determines the first byte (smallest byte address) of that member in a compound datum.
 *
 * For more information on HDF5 attributes and datatypes, read the
 * <a href="https://hdfgroup.github.io/hdf5/_h5_a__u_g.html#sec_attribute">HDF5 Attributes in HDF5 User Guide</a>
 *
 * There are two basic types of compound attributes: simple compound data and nested compound data. Members of a simple
 * compound attribute have atomic datatypes. Members of a nested compound attribute are compound or array of compound
 * data.
 *
 * Since Java does not understand C structures, we cannot directly read/write compound data values as in the following C
 * example.
 *
 * <pre>
 * typedef struct s1_t {
 *         int    a;
 *         float  b;
 *         double c;
 *         } s1_t;
 *     s1_t       s1[LENGTH];
 *     ...
 *     H5Dwrite(..., s1);
 *     H5Dread(..., s1);
 * </pre>
 *
 * Values of compound data fields are stored in java.util.Vector object. We read and write compound data by fields
 * instead of compound structure. As for the example above, the java.util.Vector object has three elements: int[LENGTH],
 * float[LENGTH] and double[LENGTH]. Since Java understands the primitive datatypes of int, float and double, we will be
 * able to read/write the compound data by field.
 *
 * @version 1.0 6/15/2021
 * @author Allen Byrne
 */
public class H5CompoundAttr extends CompoundDS implements H5Attribute
{
    private static final long serialVersionUID = 2072473407027648309L;

    private static final Logger log = LoggerFactory.getLogger(H5CompoundAttr.class);

    /** The HObject to which this NC2Attribute is attached, Attribute interface */
    protected HObject         parentObject;

    /** additional information and properties for the attribute, Attribute interface */
    private transient Map<String, Object> properties;

    /**
     * Create an attribute with specified name, data type and dimension sizes.
     *
     * @param parentObj
     *            the HObject to which this H5CompoundAttr is attached.
     * @param attrName
     *            the name of the attribute.
     * @param attrType
     *            the datatype of the attribute.
     * @param attrDims
     *            the dimension sizes of the attribute, null for scalar attribute
     *
     * @see hdf.object.Datatype
     */
    public H5CompoundAttr(HObject parentObj, String attrName, Datatype attrType, long[] attrDims) {
        this(parentObj, attrName, attrType, attrDims, null);
    }

    /**
     * Create an attribute with specific name and value.
     *
     * @param parentObj
     *            the HObject to which this H5CompoundAttr is attached.
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
    public H5CompoundAttr(HObject parentObj, String attrName, Datatype attrType, long[] attrDims, Object attrValue) {
        super((parentObj == null) ? null : parentObj.getFileFormat(), attrName,
                (parentObj == null) ? null : parentObj.getFullName(), null);

        log.trace("H5CompoundAttr: start {}", parentObj);
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

        numberOfMembers = 0;
        memberNames = null;
        isMemberSelected = null;
        memberTypes = null;

        log.trace("attrName={}, attrType={}, attrValue={}, rank={}",
                attrName, attrType.getDescription(), data, rank);

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
     *
     * init() is also used to reset the selection of a attribute (start, stride and
     * count) to the default, which is the entire attribute for 1D or 2D datasets. In
     * the following example, init() at step 1) retrieves datatype and dataspace
     * information from file. getData() at step 3) reads only one data point. init()
     * at step 4) resets the selection to the whole attribute. getData() at step 4)
     * reads the values of whole attribute into memory.
     *
     * <pre>
     * dset = (Dataset) file.get(NAME_DATASET);
     *
     * // 1) get datatype and dataspace information from file
     * attr.init();
     * rank = attr.getAttributeRank(); // rank = 2, a 2D attribute
     * count = attr.getSelectedDims();
     * start = attr.getStartDims();
     * dims = attr.getAttributeDims();
     *
     * // 2) select only one data point
     * for (int i = 0; i &lt; rank; i++) {
     *     start[0] = 0;
     *     count[i] = 1;
     * }
     *
     * // 3) read one data point
     * data = attr.getAttributeData();
     *
     * // 4) reset selection to the whole attribute
     * attr.init();
     *
     * // 5) clean the memory data buffer
     * attr.clearData();
     *
     * // 6) Read the whole attribute
     * data = attr.getAttributeData();
     * </pre>
     */
    @Override
    public void init() {
        if (inited) {
            resetSelection();
            log.trace("init(): H5CompoundAttr already inited");
            return;
        }

        long aid = HDF5Constants.H5I_INVALID_HID;
        long tid = HDF5Constants.H5I_INVALID_HID;
        long sid = HDF5Constants.H5I_INVALID_HID;
        int tclass = HDF5Constants.H5I_INVALID_HID;
        flatNameList = new Vector<>();
        flatTypeList = new Vector<>();
        long[] memberTIDs = null;

        log.trace("init(): FILE_TYPE_HDF5");
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
                tclass = H5.H5Tget_class(tid);
                log.trace("init(): tid={} sid={} rank={} space_type={}", tid, sid, rank, space_type);

                long tmptid = 0;

                // Handle ARRAY and VLEN types by getting the base type
                if (tclass == HDF5Constants.H5T_ARRAY || tclass == HDF5Constants.H5T_VLEN) {
                    try {
                        tmptid = tid;
                        tid = H5.H5Tget_super(tmptid);
                        log.trace("init(): H5T_ARRAY or H5T_VLEN class old={}, new={}", tmptid, tid);
                    }
                    catch (Exception ex) {
                        log.debug("init(): H5T_ARRAY or H5T_VLEN H5Tget_super({}) failure: ", tmptid, ex);
                        tid = -1;
                    }
                    finally {
                        try {
                            H5.H5Tclose(tmptid);
                        }
                        catch (HDF5Exception ex) {
                            log.debug("init(): H5Tclose({}) failure: ", tmptid, ex);
                        }
                    }
                }

                if (tclass == HDF5Constants.H5T_COMPOUND) {
                    // initialize member information
                    H5Datatype.extractCompoundInfo((H5Datatype)datatype, "", flatNameList, flatTypeList);
                    numberOfMembers = flatNameList.size();
                    log.trace("init(): numberOfMembers={}", numberOfMembers);

                    memberNames = new String[numberOfMembers];
                    memberTIDs = new long[numberOfMembers];
                    memberTypes = new Datatype[numberOfMembers];
                    memberOrders = new int[numberOfMembers];
                    isMemberSelected = new boolean[numberOfMembers];
                    memberDims = new Object[numberOfMembers];

                    for (int i = 0; i < numberOfMembers; i++) {
                        isMemberSelected[i] = true;
                        memberTIDs[i] = flatTypeList.get(i).createNative();

                        try {
                            memberTypes[i] = flatTypeList.get(i);
                        }
                        catch (Exception ex) {
                            log.debug("init(): failed to create datatype for member[{}]: ", i, ex);
                            memberTypes[i] = null;
                        }

                        memberNames[i] = flatNameList.get(i);
                        memberOrders[i] = 1;
                        memberDims[i] = null;
                        log.trace("init()[{}]: memberNames[{}]={}, memberTIDs[{}]={}, memberTypes[{}]={}", i, i,
                                memberNames[i], i, memberTIDs[i], i, memberTypes[i]);

                        try {
                            tclass = H5.H5Tget_class(memberTIDs[i]);
                        }
                        catch (HDF5Exception ex) {
                            log.debug("init(): H5Tget_class({}) failure: ", memberTIDs[i], ex);
                        }

                        if (tclass == HDF5Constants.H5T_ARRAY) {
                            int n = H5.H5Tget_array_ndims(memberTIDs[i]);
                            long mdim[] = new long[n];
                            H5.H5Tget_array_dims(memberTIDs[i], mdim);
                            int idim[] = new int[n];
                            for (int j = 0; j < n; j++)
                                idim[j] = (int) mdim[j];
                            memberDims[i] = idim;
                            tmptid = H5.H5Tget_super(memberTIDs[i]);
                            memberOrders[i] = (int) (H5.H5Tget_size(memberTIDs[i]) / H5.H5Tget_size(tmptid));
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (HDF5Exception ex) {
                                log.debug("init(): memberTIDs[{}] H5Tclose(tmptid {}) failure: ", i, tmptid, ex);
                            }
                        }
                    } // (int i=0; i<numberOfMembers; i++)
                }

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

                inited = true;
            }
            catch (HDF5Exception ex) {
                numberOfMembers = 0;
                memberNames = null;
                memberTypes = null;
                memberOrders = null;
                log.debug("init(): ", ex);
            }
            finally {
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

                if (memberTIDs != null) {
                    for (int i = 0; i < memberTIDs.length; i++) {
                        try {
                            H5.H5Tclose(memberTIDs[i]);
                        }
                        catch (Exception ex) {
                            log.debug("init(): H5Tclose(memberTIDs[{}] {}) failure: ", i, memberTIDs[i], ex);
                        }
                    }
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
     * // when H5CompoundAttr.getData() is called, the selection above will be used since
     * // the dimension arrays are passed by reference. Changes of these arrays
     * // outside the attribute object directly change the values of these array
     * // in the attribute object.
     * </pre>
     *
     * For H5CompoundAttr, the memory data object is an java.util.List object. Each
     * element of the list is a data array that corresponds to a compound field.
     *
     * For example, if compound attribute "comp" has the following nested
     * structure, and member datatypes
     *
     * <pre>
     * comp --&gt; m01 (int)
     * comp --&gt; m02 (float)
     * comp --&gt; nest1 --&gt; m11 (char)
     * comp --&gt; nest1 --&gt; m12 (String)
     * comp --&gt; nest1 --&gt; nest2 --&gt; m21 (long)
     * comp --&gt; nest1 --&gt; nest2 --&gt; m22 (double)
     * </pre>
     *
     * getData() returns a list of six arrays: {int[], float[], char[],
     * String[], long[] and double[]}.
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
                log.trace("readBytes(): size={}", size);

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
     * For CompoundAttr, the memory data object is an java.util.List object. Each
     * element of the list is a data array that corresponds to a compound field.
     *
     * For example, if compound dataset "comp" has the following nested
     * structure, and member datatypes
     *
     * <pre>
     * comp --&gt; m01 (int)
     * comp --&gt; m02 (float)
     * comp --&gt; nest1 --&gt; m11 (char)
     * comp --&gt; nest1 --&gt; m12 (String)
     * comp --&gt; nest1 --&gt; nest2 --&gt; m21 (long)
     * comp --&gt; nest1 --&gt; nest2 --&gt; m22 (double)
     * </pre>
     *
     * getData() returns a list of six arrays: {int[], float[], char[],
     * String[], long[] and double[]}.
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
            readData = compoundAttributeCommonIO(H5File.IO_TYPE.READ, null);
        }
        catch (Exception ex) {
            log.debug("read(): failed to read compound attribute: ", ex);
            throw new Exception("failed to read compound attribute: " + ex.getMessage(), ex);
        }

        return readData;
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
     *             If there is an error at the HDF5 library level.
     */
    @Override
    public void write(Object buf) throws Exception {
        if (this.getFileFormat().isReadOnly())
            throw new Exception("cannot write to compound attribute in file opened as read-only");

        if (!buf.equals(data))
            setData(buf);

        init();

        if (parentObject == null) {
            log.debug("write(Object): parent object is null; nowhere to write attribute to");
            return;
        }

        ((MetaDataContainer) getParentObject()).writeMetadata(this);

        try {
            compoundAttributeCommonIO(H5File.IO_TYPE.WRITE, buf);
        }
        catch (Exception ex) {
            log.debug("write(Object): failed to write compound attribute: ", ex);
            throw new Exception("failed to write compound attribute: " + ex.getMessage(), ex);
        }
        resetSelection();
    }

    /*
     * Routine to convert datatypes that are read in as byte arrays to
     * regular types.
     */
    @Override
    protected Object convertByteMember(final Datatype dtype, byte[] byteData) {
        Object theObj = null;

        if (dtype.isFloat() && dtype.getDatatypeSize() == 16)
            theObj = ((H5Datatype)dtype).byteToBigDecimal(byteData, 0);
        else
            theObj = super.convertByteMember(dtype, byteData);

        return theObj;
    }

    private Object compoundAttributeCommonIO(H5File.IO_TYPE ioType, Object writeBuf) throws Exception {
        H5Datatype dsDatatype = (H5Datatype)getDatatype();
        Object theData = null;

        if (numberOfMembers <= 0) {
            log.debug("compoundAttributeCommonIO(): attribute contains no members");
            throw new Exception("dataset contains no members");
        }

        /*
         * I/O type-specific pre-initialization.
         */
        if (ioType == H5File.IO_TYPE.WRITE) {
            if ((writeBuf == null) || !(writeBuf instanceof List)) {
                log.debug("compoundAttributeCommonIO(): writeBuf is null or invalid");
                throw new Exception("write buffer is null or invalid");
            }

            /*
             * Check for any unsupported datatypes and fail early before
             * attempting to write to the attribute.
             */
            if (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isCompound()) {
                log.debug("compoundAttributeCommonIO(): cannot write attribute of type ARRAY of COMPOUND");
                throw new HDF5Exception("Unsupported attribute of type ARRAY of COMPOUND");
            }

            if (dsDatatype.isVLEN() && !dsDatatype.isVarStr() && dsDatatype.getDatatypeBase().isCompound()) {
                log.debug("compoundAttributeCommonIO(): cannot write attribute of type VLEN of COMPOUND");
                throw new HDF5Exception("Unsupported attribute of type VLEN of COMPOUND");
            }
        }

        long aid = open();
        if (aid >= 0) {
            log.trace("compoundAttributeCommonIO(): isDataLoaded={}", isDataLoaded);
            try {
                theData = AttributeCommonIO(aid, ioType, writeBuf);
            }
            finally {
                close(aid);
            }
        }
        else
            log.debug("compoundAttributeCommonIO(): failed to open attribute");

        return theData;
    }

    /*
     * Private recursive routine to read/write an entire compound datatype field by
     * field. This routine is called recursively for ARRAY of COMPOUND and VLEN of
     * COMPOUND datatypes.
     *
     * NOTE: the globalMemberIndex hack is ugly, but we need to keep track of a
     * running counter so that we can index properly into the flattened name list
     * generated from H5Datatype.extractCompoundInfo() at attribute init time.
     */
    private Object compoundTypeIO(H5Datatype parentType, int nSelPoints, final H5Datatype cmpdType,
            Object dataBuf, int[] globalMemberIndex) {
        Object theData = null;

        if (cmpdType.isArray()) {
            log.trace("compoundTypeIO(): ARRAY type");

            long[] arrayDims = cmpdType.getArrayDims();
            int arrSize = nSelPoints;
            for (int i = 0; i < arrayDims.length; i++) {
                arrSize *= arrayDims[i];
            }
            theData = compoundTypeIO(cmpdType, arrSize, (H5Datatype) cmpdType.getDatatypeBase(), dataBuf, globalMemberIndex);
        }
        else if (cmpdType.isVLEN() && !cmpdType.isVarStr()) {
            /*
             * TODO: true variable-length support.
             */
            String[] errVal = new String[nSelPoints];
            String errStr = "*UNSUPPORTED*";

            for (int j = 0; j < nSelPoints; j++)
                errVal[j] = errStr;

            /*
             * Setup a fake data list.
             */
            Datatype baseType = cmpdType.getDatatypeBase();
            while (baseType != null && !baseType.isCompound()) {
                baseType = baseType.getDatatypeBase();
            }

            List<Object> fakeVlenData = (List<Object>) H5Datatype.allocateArray((H5Datatype) baseType, nSelPoints);
            fakeVlenData.add(errVal);

            theData = fakeVlenData;
        }
        else if (cmpdType.isCompound()) {
            long parentLength = parentType.getDatatypeSize();
            List<Object> memberDataList = null;
            List<Datatype> typeList = cmpdType.getCompoundMemberTypes();
            List<Long> offsetList = cmpdType.getCompoundMemberOffsets();

            log.trace("compoundTypeIO(): read {} members: parentLength={}", typeList.size(), parentLength);

            memberDataList = (List<Object>) H5Datatype.allocateArray(cmpdType, nSelPoints);

            try {
                for (int i = 0; i < typeList.size(); i++) {
                    long memberOffset = 0;  //offset into dataBuf
                    H5Datatype memberType = null;
                    String memberName = null;
                    Object memberData = null;

                    try {
                        memberType = (H5Datatype) typeList.get(i);
                        memberOffset = offsetList.get(i);
                    }
                    catch (Exception ex) {
                        log.debug("compoundTypeIO(): get member {} failure: ", i, ex);
                        globalMemberIndex[0]++;
                        continue;
                    }

                    /*
                     * Since the type list used here is not a flattened structure, we need to skip
                     * the member selection check for compound types, as otherwise having a single
                     * member not selected would skip the reading/writing for the entire compound
                     * type. The member selection check will be deferred to the recursive compound
                     * read/write below.
                     */
                    if (!memberType.isCompound()) {
                        if (!isMemberSelected[globalMemberIndex[0] % this.getMemberCount()]) {
                            log.debug("compoundTypeIO(): member[{}] is not selected", i);
                            globalMemberIndex[0]++;
                            continue; // the field is not selected
                        }
                    }

                    if (!memberType.isCompound()) {
                        try {
                            memberName = new String(flatNameList.get(globalMemberIndex[0]));
                        }
                        catch (Exception ex) {
                            log.debug("compoundTypeIO(): get member {} name failure: ", i, ex);
                            memberName = "null";
                        }
                    }

                    log.trace("compoundTypeIO(): member[{}]({}) is type {} offset {}", i, memberName,
                            memberType.getDescription(), memberOffset);

                    try {
                        int mt_typesize = (int)memberType.getDatatypeSize();
                        log.trace("compoundTypeIO(): member[{}] mt_typesize={}", i, mt_typesize);
                        byte[] memberbuf = new byte[nSelPoints * mt_typesize];
                        for (int dimindx = 0; dimindx < nSelPoints; dimindx++)
                            try {
                                System.arraycopy(dataBuf, (int)memberOffset + dimindx * (int)parentLength, memberbuf, dimindx * mt_typesize, mt_typesize);
                            }
                        catch (Exception err) {
                            log.trace("compoundTypeIO(): arraycopy failure: ", err);
                        }

                        if (memberType.isCompound()) {
                            memberData = compoundTypeIO(cmpdType, nSelPoints, memberType, memberbuf,
                                    globalMemberIndex);
                        }
                        else if (memberType.isArray() /* || (memberType.isVLEN() && !memberType.isVarStr()) */) {
                            /*
                             * Recursively detect any nested array/vlen of compound types.
                             */
                            boolean compoundFound = false;

                            Datatype base = memberType.getDatatypeBase();
                            while (base != null) {
                                if (base.isCompound())
                                    compoundFound = true;

                                base = base.getDatatypeBase();
                            }

                            if (compoundFound) {
                                /*
                                 * Skip the top-level array/vlen type.
                                 */
                                globalMemberIndex[0]++;

                                memberData = compoundTypeIO(cmpdType, nSelPoints, memberType, memberbuf,
                                        globalMemberIndex);
                            }
                            else {
                                memberData = convertByteMember(memberType, memberbuf);
                                globalMemberIndex[0]++;
                            }
                        }
                        else {
                            memberData = convertByteMember(memberType, memberbuf);
                            globalMemberIndex[0]++;
                        }
                    }
                    catch (Exception ex) {
                        log.debug("compoundTypeIO(): failed to read member {}: ", i, ex);
                        globalMemberIndex[0]++;
                        memberData = null;
                    }

                    if (memberData == null) {
                        String[] errVal = new String[nSelPoints];
                        String errStr = "*ERROR*";

                        for (int j = 0; j < nSelPoints; j++)
                            errVal[j] = errStr;

                        memberData = errVal;
                    }

                    memberDataList.add(memberData);
                } // (i = 0; i < atomicTypeList.size(); i++)
            }
            catch (Exception ex) {
                log.debug("compoundTypeIO(): failure: ", ex);
                memberDataList = null;
            }

            theData = memberDataList;
        }

        return theData;
    }

    private Object compoundTypeWriteIO(H5Datatype parentType, final H5Datatype cmpdType,
            Object dataBuf, int[] globalMemberIndex) {
        Object theData = null;
        if (cmpdType.isArray()) {
            Object memberData = null;
            log.trace("compoundTypeWriteIO(): ARRAY type");

            theData = compoundTypeWriteIO(cmpdType, (H5Datatype) cmpdType.getDatatypeBase(), dataBuf, globalMemberIndex);
        }
        else if (cmpdType.isVLEN() && !cmpdType.isVarStr()) {
            /*
             * TODO: true variable-length support.
             */
            String errVal = new String("*UNSUPPORTED*");

            /*
             * Setup a fake data bytes.
             */
            Datatype baseType = cmpdType.getDatatypeBase();
            while (baseType != null && !baseType.isCompound()) {
                baseType = baseType.getDatatypeBase();
            }

            List<Object> fakeVlenData = (List<Object>) H5Datatype.allocateArray((H5Datatype) baseType, 1);
            fakeVlenData.add(errVal);

            theData = convertMemberByte(baseType, fakeVlenData);
        }
        else if (cmpdType.isCompound()) {
            long parentLength = parentType.getDatatypeSize();
            List<Object> memberDataList = null;
            List<Datatype> typeList = cmpdType.getCompoundMemberTypes();
            List<Long> offsetList = cmpdType.getCompoundMemberOffsets();

            log.trace("compoundTypeWriteIO(): write {} members", typeList.size());

            theData = new byte[(int)cmpdType.getDatatypeSize()];
            try {
                for (int i = 0, writeListIndex = 0; i < typeList.size(); i++) {
                    long memberOffset = 0;  //offset into dataBuf
                    H5Datatype memberType = null;
                    String memberName = null;
                    Object memberData = null;

                    try {
                        memberType = (H5Datatype) typeList.get(i);
                        memberOffset = offsetList.get(i);
                    }
                    catch (Exception ex) {
                        log.debug("compoundTypeWriteIO(): get member {} failure: ", i, ex);
                        globalMemberIndex[0]++;
                        continue;
                    }
                    long memberLength = memberType.getDatatypeSize();

                    /*
                     * Since the type list used here is not a flattened structure, we need to skip the member selection
                     * check for compound types, as otherwise having a single member not selected would skip the
                     * reading/writing for the entire compound type. The member selection check will be deferred to the
                     * recursive compound read/write below.
                     */
                    if (!memberType.isCompound()) {
                        if (!isMemberSelected[globalMemberIndex[0] % this.getMemberCount()]) {
                            log.debug("compoundTypeWriteIO(): member[{}] is not selected", i);
                            globalMemberIndex[0]++;
                            continue; // the field is not selected
                        }
                    }

                    if (!memberType.isCompound()) {
                        try {
                            memberName = new String(flatNameList.get(globalMemberIndex[0]));
                        }
                        catch (Exception ex) {
                            log.debug("compoundTypeWriteIO(): get member {} name failure: ", i, ex);
                            memberName = "null";
                        }
                    }

                    log.trace("compoundTypeWriteIO(): member[{}]({}) is type {} offset {}", i, memberName,
                            memberType.getDescription(), memberOffset);

                    try {
                        /*
                         * TODO: currently doesn't correctly handle non-selected compound members.
                         */
                        memberData = ((List<?>) dataBuf).get(i);
                    }
                    catch (Exception ex) {
                        log.debug("compoundTypeWriteIO(): get member[{}] data failure: ", i, ex);
                        globalMemberIndex[0]++;
                        continue;
                    }

                    if (memberData == null) {
                        log.debug("compoundTypeWriteIO(): member[{}] data is null", i);
                        globalMemberIndex[0]++;
                        continue;
                    }

                    try {
                        if (memberType.isCompound()) {
                            List<?> nestedList = (List<?>) ((List<?>) dataBuf).get(i);
                            memberData = compoundTypeWriteIO(cmpdType, memberType, nestedList, globalMemberIndex);
                        }
                        else {
                            memberData = writeSingleCompoundMember(memberType, memberData);
                            globalMemberIndex[0]++;
                        }
                    }
                    catch (Exception ex) {
                        log.debug("compoundTypeWriteIO(): failed to write member[{}]: ", i, ex);
                        globalMemberIndex[0]++;
                    }

                    byte[] indexedBytes = convertMemberByte(memberType, memberData);
                    try {
                        System.arraycopy(indexedBytes, 0, theData, writeListIndex, (int)memberLength);
                    }
                    catch (Exception err) {
                        log.trace("compoundTypeWriteIO(): arraycopy failure: ", err);
                    }
                    writeListIndex += memberLength;
                } // (i = 0, writeListIndex = 0; i < atomicTypeList.size(); i++)
            }
            catch (Exception ex) {
                log.debug("compoundTypeWriteIO(): failure: ", ex);
                theData = null;
            }
        }

        return theData;
    }

    /*
     * Routine to convert datatypes that are in object arrays to
     * bytes.
     */
    private byte[] convertMemberByte(final Datatype dtype, Object theObj) {
        byte[] byteData = null;

        if (dtype.getDatatypeSize() == 1) {
            /*
             * Normal byte[] type, such as an integer datatype of size 1.
             */
            byteData = (byte[])theObj;
        }
        else if (dtype.isString() && !dtype.isVarStr() && convertByteToString && !(theObj instanceof byte[])) {
            log.trace("convertMemberByte(): converting string array to byte array");

            byteData = stringToByte((String[])theObj, (int) dtype.getDatatypeSize());
        }
        else if (dtype.isInteger()) {
            log.trace("convertMemberByte(): converting integer array to byte array");

            switch ((int)dtype.getDatatypeSize()) {
            case 1:
                /*
                 * Normal byte[] type, such as an integer datatype of size 1.
                 */
                byteData = (byte[])theObj;
                break;
            case 2:
                byteData = HDFNativeData.shortToByte(0, 1, (short[])theObj);
                break;
            case 4:
                byteData = HDFNativeData.intToByte(0, 1, (int[])theObj);
                break;
            case 8:
                byteData = HDFNativeData.longToByte(0, 1, (long[])theObj);
                break;
            default:
                log.debug("convertMemberByte(): invalid datatype size");
                byteData = null;
                break;
            }
        }
        else if (dtype.isFloat()) {
            log.trace("convertMemberByte(): converting float array to byte array");

            if (dtype.getDatatypeSize() == 16)
                byteData = ((H5Datatype)dtype).bigDecimalToByte((BigDecimal[])theObj, 0);
            else if (dtype.getDatatypeSize() == 8)
                byteData = HDFNativeData.doubleToByte(0, 1, (double[])theObj);
            else
                byteData = HDFNativeData.floatToByte(0, 1, (float[])theObj);
        }
        else if (((H5Datatype)dtype).isRegRef() || ((H5Datatype)dtype).isRefObj()) {
            log.trace("convertMemberByte(): reference type - converting long array to byte array");

            byteData = HDFNativeData.longToByte(0, 1, (long[])theObj);
        }
        else if (dtype.isArray()) {
            Datatype baseType = dtype.getDatatypeBase();

            /*
             * Retrieve the real base datatype in the case of ARRAY of ARRAY datatypes.
             */
            while (baseType.isArray())
                baseType = baseType.getDatatypeBase();

            /*
             * Optimize for the common cases of Arrays.
             */
            switch (baseType.getDatatypeClass()) {
            case Datatype.CLASS_INTEGER:
            case Datatype.CLASS_FLOAT:
            case Datatype.CLASS_CHAR:
            case Datatype.CLASS_STRING:
            case Datatype.CLASS_BITFIELD:
            case Datatype.CLASS_OPAQUE:
            case Datatype.CLASS_COMPOUND:
            case Datatype.CLASS_REFERENCE:
            case Datatype.CLASS_ENUM:
            case Datatype.CLASS_VLEN:
            case Datatype.CLASS_TIME:
                byteData = convertMemberByte(baseType, theObj);
                break;

            case Datatype.CLASS_ARRAY:
            {
                Datatype arrayType = dtype.getDatatypeBase();

                long[] arrayDims = dtype.getArrayDims();
                int arrSize = 1;
                for (int i = 0; i < arrayDims.length; i++) {
                    arrSize *= arrayDims[i];
                }

                byteData = new byte[arrSize * (int)arrayType.getDatatypeSize()];

                for (int i = 0; i < arrSize; i++) {
                    byte[] indexedBytes = convertMemberByte(arrayType, ((Object[]) theObj)[i]);
                    try {
                        System.arraycopy(indexedBytes, 0, byteData, (int)(i * arrayType.getDatatypeSize()), (int)arrayType.getDatatypeSize());
                    }
                    catch (Exception err) {
                        log.trace("convertMemberByte(): arraycopy failure: ", err);
                    }
                }

                break;
            }

            case Datatype.CLASS_NO_CLASS:
            default:
                log.debug("convertMemberByte(): invalid datatype class");
                byteData = null;
            }
        }
        else if (dtype.isCompound()) {
            /*
             * TODO: still valid after reading change?
             */
            byteData = convertCompoundMemberBytes(dtype, (List<Object>)theObj);
        }
        else {
            log.debug("convertMemberByte(): no change as byte[]");
            byteData = (byte[])theObj;
        }

        return byteData;
    }

    /**
     * Given an array of objects representing a compound Datatype, converts each of
     * its members into bytes and returns the results.
     *
     * @param dtype
     *            The compound datatype to convert
     * @param theObj
     *            The object array representing the data of the compound Datatype
     * @return The converted bytes of the objects
     */
    private byte[] convertCompoundMemberBytes(final Datatype dtype, List<Object> theObj) {
        List<Datatype> allSelectedTypes = Arrays.asList(this.getSelectedMemberTypes());
        List<Datatype> localTypes = new ArrayList<>(dtype.getCompoundMemberTypes());
        Iterator<Datatype> localIt = localTypes.iterator();
        while (localIt.hasNext()) {
            Datatype curType = localIt.next();

            if (curType.isCompound())
                continue;

            if (!allSelectedTypes.contains(curType))
                localIt.remove();
        }

        byte[] byteData = new byte[(int)dtype.getDatatypeSize()];
        for (int i = 0, index = 0; i < localTypes.size(); i++) {
            Datatype curType = localTypes.get(i);
            byte[] indexedBytes = null;
            if (curType.isCompound())
                indexedBytes = convertCompoundMemberBytes(curType, (List<Object>)theObj.get(i));
            else
                indexedBytes = convertMemberByte(curType, theObj.get(i));

            try {
                System.arraycopy(indexedBytes, 0, byteData, index + (int)curType.getDatatypeSize(), (int)curType.getDatatypeSize());
            }
            catch (Exception err) {
                log.trace("convertCompoundMemberBytes(): arraycopy failure: ", err);
            }
            index += curType.getDatatypeSize();
        }

        return byteData;
    }

    /*
     * Private routine to convert a single field of a compound datatype.
     */
    private Object writeSingleCompoundMember(final H5Datatype memberType, Object theData) throws Exception {
        /*
         * Perform any necessary data conversions before writing the data.
         */
        Object tmpData = theData;
        try {
            if (memberType.isUnsigned()) {
                // Check if we need to convert unsigned integer data from Java-style
                // to C-style integers
                long tsize = memberType.getDatatypeSize();
                String cname = theData.getClass().getName();
                char dname = cname.charAt(cname.lastIndexOf('[') + 1);
                boolean doIntConversion = (((tsize == 1) && (dname == 'S'))
                        || ((tsize == 2) && (dname == 'I')) || ((tsize == 4) && (dname == 'J')));

                if (doIntConversion) {
                    log.trace("writeSingleCompoundMember(): converting integer data to unsigned C-type integers");
                    tmpData = convertToUnsignedC(theData, null);
                }
            }
            else if (memberType.isString() && (Array.get(theData, 0) instanceof String)) {
                log.trace("writeSingleCompoundMember(): converting string array to byte array");
                tmpData = stringToByte((String[]) theData, (int) memberType.getDatatypeSize());
            }
            else if (memberType.isEnum() && (Array.get(theData, 0) instanceof String)) {
                log.trace("writeSingleCompoundMember(): converting enum names to values");
                tmpData = memberType.convertEnumNameToValue((String[]) theData);
            }
        }
        catch (Exception ex) {
            log.debug("writeSingleCompoundMember(): data conversion failure: ", ex);
            tmpData = null;
        }

        if (tmpData == null) {
            log.debug("writeSingleCompoundMember(): data is null");
        }

        return tmpData;
    }

    /**
     * Converts the data values of this data object to appropriate Java integers if
     * they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertFromUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertFromUnsignedC() {
        throw new UnsupportedOperationException("H5CompoundDS:convertFromUnsignedC Unsupported operation.");
    }

    /**
     * Converts Java integer data values of this data object back to unsigned C-type
     * integer data if they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertToUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertToUnsignedC() {
        throw new UnsupportedOperationException("H5CompoundDS:convertToUnsignedC Unsupported operation.");
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

        // attribute value is an array
        StringBuilder sb = new StringBuilder();
        int numberTypes = ((ArrayList<Object[]>)theData).size();
        log.trace("toAttributeString: numberTypes={}", numberTypes);
        List<Datatype> cmpdTypes =  getDatatype().getCompoundMemberTypes();

        int loopcnt = 0;
        while (loopcnt < maxItems) {
            if (loopcnt > 0)
                sb.append(delimiter);
            sb.append("{");
            for (int dv = 0; dv < numberTypes; dv++) {
                if (dv > 0)
                    sb.append(delimiter);

                Object theobj = ((ArrayList<Object[]>)theData).get(dv);
                Class<? extends Object> valClass = theobj.getClass();
                log.trace("toAttributeString:valClass={}", valClass);
                int n = 0;
                Datatype dtype = cmpdTypes.get(dv);
                // value is an array
                if (valClass.isArray()) {
                    n = Array.getLength(theobj);
                    if (dtype.isRef())
                        n /= (int)dtype.getDatatypeSize();
                }
                else
                    n = ((ArrayList<Object[]>)theobj).size();
                //if ((maxItems > 0) && (n + loopcnt > maxItems))
                //    n = maxItems - loopcnt;
                log.trace("toAttributeString:[{}] theobj={} size={}", dv, theobj, n);
                String sobj = toString(theobj, dtype, delimiter, n);
                sb.append(sobj);
                loopcnt += n;
                if (loopcnt >= maxItems)
                    break;
            }  // end for (int dv = 0; dv < numberTypes; dv++)
            sb.append("}");
            break;
        }  // end for (int i = 1; i < n; i++)

        return sb.toString();
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
            int dtypesize = (int)dtype.getDatatypeSize();
            String strValue = "NULL";
            byte[] rElements = null;

            for (int k = 0; k < count; k++) {
                // need to iterate if type is ArrayList
                if (theData instanceof ArrayList)
                    rElements = (byte[]) ((ArrayList) theData).get(k);
                else
                    rElements = (byte[])theData;

                if (H5ReferenceType.zeroArrayCheck(rElements))
                    strValue = "NULL";
                else {
                    if (dtype.isStdRef()) {
                        strValue += H5.H5Rget_obj_name(rElements, HDF5Constants.H5P_DEFAULT);
                    }
                    else if (dtypesize == HDF5Constants.H5R_DSET_REG_REF_BUF_SIZE) {
                        try {
                            strValue = H5Datatype.descRegionDataset(parentObject.getFileFormat().getFID(), rElements);
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    else if (dtypesize == HDF5Constants.H5R_OBJ_REF_BUF_SIZE) {
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

            long lsize = 1;
            for (int j = 0; j < dims.length; j++)
                lsize *= dims[j];
            log.trace("AttributeCommonIO():read ioType dt_size={} lsize={}", dt_size, lsize);

            try {
                // Read data.
                Object attr_data = new byte[(int)(dt_size * lsize)];

                try {
                    H5.H5Aread(attr_id, tid, attr_data);
                }
                catch (Exception ex) {
                    log.debug("AttributeCommonIO(): H5Aread failure: ", ex);
                }
                theData = compoundTypeIO(dsDatatype, (int)lsize, dsDatatype, attr_data, new int[]{0});
            }
            catch (Exception ex) {
                log.debug("AttributeCommonIO():read ioType read failure: ", ex);
                throw new Exception(ex.getMessage(), ex);
            }
            finally {
                dsDatatype.close(tid);
            }
            for (int i = 0; i < ((ArrayList<Object[]>)theData).size(); i++) {
                Object theobj = ((ArrayList<Object[]>)theData).get(i);
                log.trace("AttributeCommonIO():read ioType data: {}", theobj);
            }
            originalBuf = theData;
            isDataLoaded = true;
        } // H5File.IO_TYPE.READ
        else {
            theData = compoundTypeWriteIO(dsDatatype, dsDatatype, objBuf, new int[]{0});
            try {
                H5.H5Awrite(attr_id, tid, theData);
            }
            catch (Exception ex) {
                log.debug("AttributeCommonIO(): H5Awrite failure: ", ex);
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
        return originalBuf;
    }
}
