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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFArray;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5DataFiltersException;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.hdf5lib.structs.H5O_token_t;

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
import hdf.object.h5.H5MetaDataContainer;
import hdf.object.h5.H5ReferenceType;

/**
 * The H5CompoundDS class defines an HDF5 dataset of compound datatypes.
 *
 * An HDF5 dataset is an object composed of a collection of data elements, or raw data, and metadata that stores a
 * description of the data elements, data layout, and all other information necessary to write, read, and interpret the
 * stored data.
 *
 * A HDF5 compound datatype is similar to a struct in C or a common block in Fortran: it is a collection of one or more
 * atomic types or small arrays of such types. Each member of a compound type has a name which is unique within that
 * type, and a byte offset that determines the first byte (smallest byte address) of that member in a compound datum.
 *
 * For more information on HDF5 datasets and datatypes, read
 * <a href="https://hdfgroup.github.io/hdf5/_h5_d__u_g.html#sec_dataset">HDF5 Datasets in HDF5 User Guide</a>
 * <a href="https://hdfgroup.github.io/hdf5/_h5_t__u_g.html#sec_datatype">HDF5 Datatypes in HDF5 User Guide</a>
 *
 * There are two basic types of compound datasets: simple compound data and nested compound data. Members of a simple
 * compound dataset have atomic datatypes. Members of a nested compound dataset are compound or array of compound data.
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
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5CompoundDS extends CompoundDS implements MetaDataContainer
{
    private static final long serialVersionUID = -5968625125574032736L;

    private static final Logger log = LoggerFactory.getLogger(H5CompoundDS.class);

    /**
     * The metadata object for this data object. Members of the metadata are instances of Attribute.
     */
    private H5MetaDataContainer objMetadata;

    /** the object properties */
    private H5O_info_t objInfo;

    /** flag to indicate if the dataset is an external dataset */
    private boolean isExternal = false;

    /** flag to indicate if the dataset is a virtual dataset */
    private boolean isVirtual = false;
    /** the list of virtual names */
    private List<String> virtualNameList;

    /**
     * Constructs an instance of a HDF5 compound dataset with given file, dataset name and path.
     *
     * The dataset object represents an existing dataset in the file. For example, new
     * H5CompoundDS(file, "dset1", "/g0/") constructs a dataset object that corresponds to the
     * dataset,"dset1", at group "/g0/".
     *
     * This object is usually constructed at FileFormat.open(), which loads the file structure and
     * object information into memory. It is rarely used elsewhere.
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     */
    public H5CompoundDS(FileFormat theFile, String theName, String thePath) {
        this(theFile, theName, thePath, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #H5CompoundDS(FileFormat, String, String)}
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     * @param oid
     *            the oid of the data object.
     */
    @Deprecated
    public H5CompoundDS(FileFormat theFile, String theName, String thePath, long[] oid) {
        super(theFile, theName, thePath, oid);
        objMetadata = new H5MetaDataContainer(theFile, theName, thePath, this);

        if (theFile != null) {
            if (oid == null) {
                // retrieve the object ID
                byte[] refBuf = null;
                try {
                    refBuf = H5.H5Rcreate_object(theFile.getFID(), this.getFullName(), HDF5Constants.H5P_DEFAULT);
                    this.oid = HDFNativeData.byteToLong(refBuf);
                    log.trace("constructor REF {} to OID {}", refBuf, this.oid);
                }
                catch (Exception ex) {
                    log.debug("constructor ID {} for {} failed H5Rcreate_object", theFile.getFID(), this.getFullName());
                }
                finally {
                    if (refBuf != null)
                        H5.H5Rdestroy(refBuf);
                }
            }
            log.trace("constructor OID {}", this.oid);
            try {
                objInfo = H5.H5Oget_info_by_name(theFile.getFID(), this.getFullName(), HDF5Constants.H5O_INFO_BASIC, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                objInfo = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
            }
        }
        else {
            this.oid = null;
            objInfo = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        long did = HDF5Constants.H5I_INVALID_HID;

        if (getFID() < 0)
            log.trace("open(): file id for:{} is invalid", getPath() + getName());
        else {
            try {
                did = H5.H5Dopen(getFID(), getPath() + getName(), HDF5Constants.H5P_DEFAULT);
                log.trace("open(): did={}", did);
            }
            catch (HDF5Exception ex) {
                log.debug("open(): Failed to open dataset {}", getPath() + getName(), ex);
                did = HDF5Constants.H5I_INVALID_HID;
            }
        }

        return did;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long did) {
        if (did >= 0) {
            try {
                H5.H5Fflush(did, HDF5Constants.H5F_SCOPE_LOCAL);
            }
            catch (Exception ex) {
                log.debug("close(): H5Fflush(did {}) failure: ", did, ex);
            }
            try {
                H5.H5Dclose(did);
            }
            catch (HDF5Exception ex) {
                log.debug("close(): H5Dclose(did {}) failure: ", did, ex);
            }
        }
    }

    /**
     * Retrieves datatype and dataspace information from file and sets the dataset
     * in memory.
     *
     * The init() is designed to support lazy operation in a dataset object. When a
     * data object is retrieved from file, the datatype, dataspace and raw data are
     * not loaded into memory. When it is asked to read the raw data from file,
     * init() is first called to get the datatype and dataspace information, then
     * load the raw data from file.
     *
     * init() is also used to reset the selection of a dataset (start, stride and
     * count) to the default, which is the entire dataset for 1D or 2D datasets. In
     * the following example, init() at step 1) retrieves datatype and dataspace
     * information from file. getData() at step 3) reads only one data point. init()
     * at step 4) resets the selection to the whole dataset. getData() at step 4)
     * reads the values of whole dataset into memory.
     *
     * <pre>
     * dset = (Dataset) file.get(NAME_DATASET);
     *
     * // 1) get datatype and dataspace information from file
     * dset.init();
     * rank = dset.getRank(); // rank = 2, a 2D dataset
     * count = dset.getSelectedDims();
     * start = dset.getStartDims();
     * dims = dset.getDims();
     *
     * // 2) select only one data point
     * for (int i = 0; i &lt; rank; i++) {
     *     start[0] = 0;
     *     count[i] = 1;
     * }
     *
     * // 3) read one data point
     * data = dset.getData();
     *
     * // 4) reset selection to the whole dataset
     * dset.init();
     *
     * // 5) clean the memory data buffer
     * dset.clearData();
     *
     * // 6) Read the whole dataset
     * data = dset.getData();
     * </pre>
     */
    @Override
    public void init() {
        if (inited) {
            resetSelection();
            log.trace("init(): Dataset already initialized");
            return; // already called. Initialize only once
        }

        long did = HDF5Constants.H5I_INVALID_HID;
        long tid = HDF5Constants.H5I_INVALID_HID;
        long sid = HDF5Constants.H5I_INVALID_HID;
        flatNameList = new Vector<>();
        flatTypeList = new Vector<>();

        did = open();
        if (did >= 0) {
            // check if it is an external or virtual dataset
            long pid = HDF5Constants.H5I_INVALID_HID;
            try {
                pid = H5.H5Dget_create_plist(did);
                try {
                    int nfiles = H5.H5Pget_external_count(pid);
                    isExternal = (nfiles > 0);
                    int layoutType = H5.H5Pget_layout(pid);
                    if (isVirtual = (layoutType == HDF5Constants.H5D_VIRTUAL)) {
                        try {
                            long vmaps = H5.H5Pget_virtual_count(pid);
                            if (vmaps > 0) {
                                virtualNameList = new Vector<>();
                                for (long next = 0; next < vmaps; next++) {
                                    try {
                                        String fname = H5.H5Pget_virtual_filename(pid, next);
                                        virtualNameList.add(fname);
                                        log.trace("init(): virtualNameList[{}]={}", next, fname);
                                    }
                                    catch (Exception err) {
                                        log.trace("init(): vds[{}] continue", next);
                                    }
                                }
                            }
                        }
                        catch (Exception err) {
                            log.debug("init(): vds count error: ", err);
                        }
                    }
                    log.trace("init(): pid={} nfiles={} isExternal={} isVirtual={}", pid, nfiles, isExternal, isVirtual);
                }
                catch (Exception ex) {
                    log.debug("init(): check if it is an external or virtual dataset: ", ex);
                }
            }
            catch (Exception ex) {
                log.debug("init(): H5Dget_create_plist() failure: ", ex);
            }
            finally {
                try {
                    H5.H5Pclose(pid);
                }
                catch (Exception ex) {
                    log.debug("init(): H5Pclose(pid {}) failure: ", pid, ex);
                }
            }

            try {
                sid = H5.H5Dget_space(did);
                rank = H5.H5Sget_simple_extent_ndims(sid);
                space_type = H5.H5Sget_simple_extent_type(sid);
                if (space_type == HDF5Constants.H5S_NULL)
                    isNULL = true;
                else
                    isNULL = false;
                tid = H5.H5Dget_type(did);
                log.trace("init(): tid={} sid={} rank={}", tid, sid, rank);

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

                try {
                    int nativeClass = H5.H5Tget_class(tid);
                    if (nativeClass == HDF5Constants.H5T_REFERENCE) {
                        long lsize = 1;
                        if (rank > 0) {
                            log.trace("init():rank={}, dims={}", rank, dims);
                            for (int j = 0; j < dims.length; j++) {
                                lsize *= dims[j];
                            }
                        }
                        datatype = new H5ReferenceType(getFileFormat(), lsize, tid);
                    }
                    else
                        datatype = new H5Datatype(getFileFormat(), tid);

                    log.trace("init(): tid={} has isText={} : isVLEN={} : isEnum={} : isUnsigned={} : isStdRef={} : isRegRef={}", tid,
                            datatype.isText(), datatype.isVLEN(), ((H5Datatype) datatype).isEnum(), datatype.isUnsigned(),
                            ((H5Datatype) datatype).isStdRef(), ((H5Datatype) datatype).isRegRef());

                    H5Datatype.extractCompoundInfo((H5Datatype) datatype, "", flatNameList, flatTypeList);
                }
                catch (Exception ex) {
                    log.debug("init(): failed to create datatype for dataset: ", ex);
                    datatype = null;
                }

                // initialize member information
                numberOfMembers = flatNameList.size();
                log.trace("init(): numberOfMembers={}", numberOfMembers);

                memberNames = new String[numberOfMembers];
                memberTypes = new Datatype[numberOfMembers];
                memberOrders = new int[numberOfMembers];
                isMemberSelected = new boolean[numberOfMembers];
                memberDims = new Object[numberOfMembers];

                for (int i = 0; i < numberOfMembers; i++) {
                    isMemberSelected[i] = true;
                    memberOrders[i] = 1;
                    memberDims[i] = null;

                    try {
                        memberTypes[i] = flatTypeList.get(i);
                        log.trace("init()[{}]: memberTypes[{}]={}", i, i, memberTypes[i].getDescription());

                        if (memberTypes[i].isArray()) {
                            long mdim[] = memberTypes[i].getArrayDims();
                            int idim[] = new int[mdim.length];
                            int arrayNpoints = 1;

                            for (int j = 0; j < idim.length; j++) {
                                idim[j] = (int) mdim[j];
                                arrayNpoints *= idim[j];
                            }

                            memberDims[i] = idim;
                            memberOrders[i] = arrayNpoints;
                        }
                    }
                    catch (Exception ex) {
                        log.debug("init()[{}]: memberTypes[{}] get failure: ", i, i, ex);
                        memberTypes[i] = null;
                    }

                    try {
                        memberNames[i] = flatNameList.get(i);
                        log.trace("init()[{}]: memberNames[{}]={}", i, i, memberNames[i]);
                    }
                    catch (Exception ex) {
                        log.debug("init()[{}]: memberNames[{}] get failure: ", i, i, ex);
                        memberNames[i] = "null";
                    }
                } //  (int i=0; i<numberOfMembers; i++)

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
                if (datatype != null)
                    datatype.close(tid);

                try {
                    H5.H5Sclose(sid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("init(): H5Sclose(sid {}) failure: ", sid, ex2);
                }
            }

            close(did);

            startDims = new long[rank];
            selectedDims = new long[rank];

            resetSelection();
        }
        else {
            log.debug("init(): failed to open dataset");
        }
    }

    /**
     * Get the token for this object.
     *
     * @return true if it has any attributes, false otherwise.
     */
    public long[] getToken() {
        H5O_token_t token = objInfo.token;
        return HDFNativeData.byteToLong(token.data);
    }

    /**
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attributes, false otherwise.
     */
    @Override
    public boolean hasAttribute() {
        objInfo.num_attrs = objMetadata.getObjectAttributeSize();

        if (objInfo.num_attrs < 0) {
            long did = open();
            if (did >= 0) {
                objInfo.num_attrs = 0;

                try {
                    objInfo = H5.H5Oget_info(did);
                }
                catch (Exception ex) {
                    objInfo.num_attrs = 0;
                    log.debug("hasAttribute(): get object info failure: ", ex);
                }
                finally {
                    close(did);
                }
                objMetadata.setObjectAttributeSize((int) objInfo.num_attrs);
            }
            else {
                log.debug("hasAttribute(): could not open dataset");
            }
        }

        log.trace("hasAttribute(): nAttributes={}", objInfo.num_attrs);
        return (objInfo.num_attrs > 0);
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
            long did = HDF5Constants.H5I_INVALID_HID;
            long tid = HDF5Constants.H5I_INVALID_HID;

            did = open();
            if (did >= 0) {
                try {
                    tid = H5.H5Dget_type(did);
                    int nativeClass = H5.H5Tget_class(tid);
                    if (nativeClass == HDF5Constants.H5T_REFERENCE) {
                        long lsize = 1;
                        if (rank > 0) {
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
                        H5.H5Dclose(did);
                    }
                    catch (HDF5Exception ex) {
                        log.debug("getDatatype(): H5Dclose(did {}) failure: ", did, ex);
                    }
                }
            }
        }

        if (isExternal) {
            String pdir = this.getFileFormat().getAbsoluteFile().getParent();

            if (pdir == null) {
                pdir = ".";
            }
            System.setProperty("user.dir", pdir);
            log.trace("getDatatype(): External dataset: user.dir={}", pdir);
        }

        return datatype;
    }

    /**
     * Removes all of the elements from metadata list.
     * The list should be empty after this call returns.
     */
    @Override
    public void clear() {
        super.clear();
        objMetadata.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#readBytes()
     */
    @Override
    public byte[] readBytes() throws HDF5Exception {
        byte[] theData = null;

        if (!isInited())
            init();

        long did = open();
        if (did >= 0) {
            long fspace = HDF5Constants.H5I_INVALID_HID;
            long mspace = HDF5Constants.H5I_INVALID_HID;
            long tid = HDF5Constants.H5I_INVALID_HID;

            try {
                long[] lsize = { 1 };
                for (int j = 0; j < selectedDims.length; j++)
                    lsize[0] *= selectedDims[j];

                fspace = H5.H5Dget_space(did);
                mspace = H5.H5Screate_simple(rank, selectedDims, null);

                // set the rectangle selection
                // HDF5 bug: for scalar dataset, H5Sselect_hyperslab gives core dump
                if (rank * dims[0] > 1)
                    H5.H5Sselect_hyperslab(fspace, HDF5Constants.H5S_SELECT_SET, startDims, selectedStride, selectedDims, null); // set block to 1

                tid = H5.H5Dget_type(did);
                long size = H5.H5Tget_size(tid) * lsize[0];
                log.trace("readBytes(): size = {}", size);

                if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE)
                    throw new Exception("Invalid int size");

                theData = new byte[(int)size];

                log.trace("readBytes(): H5Dread: did={} tid={} fspace={} mspace={}", did, tid, fspace, mspace);
                H5.H5Dread(did, tid, mspace, fspace, HDF5Constants.H5P_DEFAULT, theData);
            }
            catch (Exception ex) {
                log.debug("readBytes(): failed to read data: ", ex);
            }
            finally {
                try {
                    H5.H5Sclose(fspace);
                }
                catch (Exception ex2) {
                    log.debug("readBytes(): H5Sclose(fspace {}) failure: ", fspace, ex2);
                }
                try {
                    H5.H5Sclose(mspace);
                }
                catch (Exception ex2) {
                    log.debug("readBytes(): H5Sclose(mspace {}) failure: ", mspace, ex2);
                }
                try {
                    H5.H5Tclose(tid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("readBytes(): H5Tclose(tid {}) failure: ", tid, ex2);
                }
                close(did);
            }
        }

        return theData;
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
     * dataset is a 4-dimensional array of [200][100][50][10], i.e. dims[0]=200;
     * dims[1]=100; dims[2]=50; dims[3]=10; <br>
     * We want to select every other data point in dims[1] and dims[2]
     *
     * <pre>
     * int rank = dataset.getRank(); // number of dimensions of the dataset
     * long[] dims = dataset.getDims(); // the dimension sizes of the dataset
     * long[] selected = dataset.getSelectedDims(); // the selected size of the
     *                                              // dataset
     * long[] start = dataset.getStartDims(); // the offset of the selection
     * long[] stride = dataset.getStride(); // the stride of the dataset
     * int[] selectedIndex = dataset.getSelectedIndex(); // the selected
     *                                                   // dimensions for
     *                                                   // display
     *
     * // select dim1 and dim2 as 2D data for display, and slice through dim0
     * selectedIndex[0] = 1;
     * selectedIndex[1] = 2;
     * selectedIndex[1] = 0;
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
     * // when dataset.getData() is called, the selection above will be used
     * // since
     * // the dimension arrays are passed by reference. Changes of these arrays
     * // outside the dataset object directly change the values of these array
     * // in the dataset object.
     * </pre>
     *
     * For CompoundDS, the memory data object is an java.util.List object. Each
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
            readData = compoundDatasetCommonIO(H5File.IO_TYPE.READ, null);
        }
        catch (Exception ex) {
            log.debug("read(): failed to read compound dataset: ", ex);
            throw new Exception("failed to read compound dataset: " + ex.getMessage(), ex);
        }

        return readData;
    }

    /**
     * Writes the given data buffer into this dataset in a file.
     *
     * The data buffer is a vector that contains the data values of compound fields. The data is written
     * into file field by field.
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
            throw new Exception("cannot write to compound dataset in file opened as read-only");

        if (!isInited())
            init();

        try {
            compoundDatasetCommonIO(H5File.IO_TYPE.WRITE, buf);
        }
        catch (Exception ex) {
            log.debug("write(Object): failed to write compound dataset: ", ex);
            throw new Exception("failed to write compound dataset: " + ex.getMessage(), ex);
        }
    }

    /*
     * Routine to convert datatypes that are read in as byte arrays to
     * regular types.
     */
    @Override
    protected Object convertByteMember(final Datatype dtype, byte[] byteData) {
        Object theObj = null;
        log.debug("convertByteMember(): dtype={} byteData={}", dtype, byteData);

        if (dtype.isFloat() && dtype.getDatatypeSize() == 16)
            theObj = ((H5Datatype)dtype).byteToBigDecimal(byteData, 0);
        else
            theObj = super.convertByteMember(dtype, byteData);

        return theObj;
    }

    private Object compoundDatasetCommonIO(H5File.IO_TYPE ioType, Object writeBuf) throws Exception {
        H5Datatype dsDatatype = (H5Datatype) getDatatype();
        Object theData = null;

        if (numberOfMembers <= 0) {
            log.debug("compoundDatasetCommonIO(): Dataset contains no members");
            throw new Exception("dataset contains no members");
        }

        /*
         * I/O type-specific pre-initialization.
         */
        if (ioType == H5File.IO_TYPE.WRITE) {
            if ((writeBuf == null) || !(writeBuf instanceof List)) {
                log.debug("compoundDatasetCommonIO(): writeBuf is null or invalid");
                throw new Exception("write buffer is null or invalid");
            }

            /*
             * Check for any unsupported datatypes and fail early before
             * attempting to write to the dataset.
             */
            if (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isCompound()) {
                log.debug("compoundDatasetCommonIO(): cannot write dataset of type ARRAY of COMPOUND");
                throw new HDF5Exception("Unsupported dataset of type ARRAY of COMPOUND");
            }

            if (dsDatatype.isVLEN() && !dsDatatype.isVarStr() && dsDatatype.getDatatypeBase().isCompound()) {
                log.debug("compoundDatasetCommonIO(): cannot write dataset of type VLEN of COMPOUND");
                throw new HDF5Exception("Unsupported dataset of type VLEN of COMPOUND");
            }
        }

        long did = open();
        if (did >= 0) {
            long[] spaceIDs = { HDF5Constants.H5I_INVALID_HID, HDF5Constants.H5I_INVALID_HID }; // spaceIDs[0]=mspace, spaceIDs[1]=fspace

            try {
                /*
                 * NOTE: this call sets up a hyperslab selection in the file according to the
                 * current selection in the dataset object.
                 */
                long totalSelectedSpacePoints = H5Utils.getTotalSelectedSpacePoints(did, dims, startDims,
                        selectedStride, selectedDims, spaceIDs);

                theData = compoundTypeIO(ioType, did, spaceIDs, (int) totalSelectedSpacePoints, dsDatatype, writeBuf, new int[]{0});
            }
            finally {
                if (HDF5Constants.H5S_ALL != spaceIDs[0]) {
                    try {
                        H5.H5Sclose(spaceIDs[0]);
                    }
                    catch (Exception ex) {
                        log.debug("compoundDatasetCommonIO(): H5Sclose(spaceIDs[0] {}) failure: ", spaceIDs[0], ex);
                    }
                }

                if (HDF5Constants.H5S_ALL != spaceIDs[1]) {
                    try {
                        H5.H5Sclose(spaceIDs[1]);
                    }
                    catch (Exception ex) {
                        log.debug("compoundDatasetCommonIO(): H5Sclose(spaceIDs[1] {}) failure: ", spaceIDs[1], ex);
                    }
                }

                close(did);
            }
        }
        else
            log.debug("compoundDatasetCommonIO(): failed to open dataset");

        return theData;
    }

    /*
     * Private recursive routine to read/write an entire compound datatype field by
     * field. This routine is called recursively for ARRAY of COMPOUND and VLEN of
     * COMPOUND datatypes.
     *
     * NOTE: the globalMemberIndex hack is ugly, but we need to keep track of a
     * running counter so that we can index properly into the flattened name list
     * generated from H5Datatype.extractCompoundInfo() at dataset init time.
     */
    private Object compoundTypeIO(H5File.IO_TYPE ioType, long did, long[] spaceIDs, int nSelPoints,
            final H5Datatype cmpdType, Object writeBuf, int[] globalMemberIndex) {
        Object theData = null;

        if (cmpdType.isArray()) {
            log.trace("compoundTypeIO(): ARRAY type");

            long[] arrayDims = cmpdType.getArrayDims();
            int arrSize = nSelPoints;
            for (int i = 0; i < arrayDims.length; i++)
                arrSize *= arrayDims[i];
            theData = compoundTypeIO(ioType, did, spaceIDs, arrSize, (H5Datatype) cmpdType.getDatatypeBase(), writeBuf, globalMemberIndex);
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
            List<Object> memberDataList = null;
            List<Datatype> typeList = cmpdType.getCompoundMemberTypes();

            log.trace("compoundTypeIO(): {} {} members:", (ioType == H5File.IO_TYPE.READ) ? "read" : "write", typeList.size());

            if (ioType == H5File.IO_TYPE.READ)
                memberDataList = (List<Object>) H5Datatype.allocateArray(cmpdType, nSelPoints);

            try {
                for (int i = 0, writeListIndex = 0; i < typeList.size(); i++) {
                    H5Datatype memberType = null;
                    String memberName = null;
                    Object memberData = null;

                    try {
                        memberType = (H5Datatype) typeList.get(i);
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

                    log.trace("compoundTypeIO(): member[{}]({}) is type {}", i, memberName, memberType.getDescription());

                    if (ioType == H5File.IO_TYPE.READ) {
                        try {
                            if (memberType.isCompound())
                                memberData = compoundTypeIO(ioType, did, spaceIDs, nSelPoints, memberType, writeBuf, globalMemberIndex);
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

                                    memberData = compoundTypeIO(ioType, did, spaceIDs, nSelPoints, memberType, writeBuf, globalMemberIndex);
                                }
                                else {
                                    memberData = readSingleCompoundMember(did, spaceIDs, nSelPoints, memberType, memberName);
                                    globalMemberIndex[0]++;
                                }
                            }
                            else {
                                memberData = readSingleCompoundMember(did, spaceIDs, nSelPoints, memberType, memberName);
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
                    }
                    else {
                        try {
                            /*
                             * TODO: currently doesn't correctly handle non-selected compound members.
                             */
                            memberData = ((List<?>) writeBuf).get(writeListIndex++);
                        }
                        catch (Exception ex) {
                            log.debug("compoundTypeIO(): get member[{}] data failure: ", i, ex);
                            globalMemberIndex[0]++;
                            continue;
                        }

                        if (memberData == null) {
                            log.debug("compoundTypeIO(): member[{}] data is null", i);
                            globalMemberIndex[0]++;
                            continue;
                        }

                        try {
                            if (memberType.isCompound()) {
                                List<?> nestedList = (List<?>) ((List<?>) writeBuf).get(writeListIndex++);
                                compoundTypeIO(ioType, did, spaceIDs, nSelPoints, memberType, nestedList, globalMemberIndex);
                            }
                            else {
                                writeSingleCompoundMember(did, spaceIDs, nSelPoints, memberType, memberName, memberData);
                                globalMemberIndex[0]++;
                            }
                        }
                        catch (Exception ex) {
                            log.debug("compoundTypeIO(): failed to write member[{}]: ", i, ex);
                            globalMemberIndex[0]++;
                        }
                    }
                } //  (i = 0, writeListIndex = 0; i < atomicTypeList.size(); i++)
            }
            catch (Exception ex) {
                log.debug("compoundTypeIO(): failure: ", ex);
                memberDataList = null;
            }

            theData = memberDataList;
        }

        return theData;
    }

    /*
     * Private routine to read a single field of a compound datatype by creating a
     * compound datatype and inserting the single field into that datatype.
     */
    private Object readSingleCompoundMember(long dsetID, long[] spaceIDs, int nSelPoints,
            final H5Datatype memberType, String memberName) throws Exception {
        H5Datatype dsDatatype = (H5Datatype) this.getDatatype();
        Object memberData = null;

        try {
            memberData = H5Datatype.allocateArray(memberType, nSelPoints);
            log.trace("readSingleCompoundMember(): allocateArray {} points ", nSelPoints);
        }
        catch (OutOfMemoryError err) {
            memberData = null;
            throw new Exception("Out of memory");
        }
        catch (Exception ex) {
            log.debug("readSingleCompoundMember(): ", ex);
            memberData = null;
        }

        if (memberData != null) {
            /*
             * Create a compound datatype containing just a single field (the one which we
             * want to read).
             */
            long compTid = -1;
            try {
                compTid = dsDatatype.createCompoundFieldType(memberName);
            }
            catch (HDF5Exception ex) {
                log.debug("readSingleCompoundMember(): unable to create compound field type for member of type {}: ",
                        memberType.getDescription(), ex);
                memberData = null;
            }

            /*
             * Actually read the data for this member now that everything has been setup.
             */
            try {
                if (memberType.isVarStr()) {
                    log.trace("readSingleCompoundMember(): H5DreadVL did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                            dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                    (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                    H5.H5Dread_VLStrings(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, (Object[]) memberData);
                }
                else if (memberType.isVLEN() || (memberType.isArray() && memberType.getDatatypeBase().isVLEN())) {
                    log.trace("readSingleCompoundMember(): H5DreadVL did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                            dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                    (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                    H5.H5DreadVL(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, (Object[]) memberData);
                }
                else {
                    log.trace("readSingleCompoundMember(): H5Dread did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                            dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                    (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                    H5.H5Dread(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, memberData);
                }
            }
            catch (HDF5DataFiltersException exfltr) {
                log.debug("readSingleCompoundMember(): read failure: ", exfltr);
                throw new Exception("Filter not available exception: " + exfltr.getMessage(), exfltr);
            }
            catch (Exception ex) {
                log.debug("readSingleCompoundMember(): read failure: ", ex);
                throw new Exception("failed to read compound member: " + ex.getMessage(), ex);
            }
            finally {
                dsDatatype.close(compTid);
            }

            /*
             * Perform any necessary data conversions.
             */
            if (memberType.isUnsigned()) {
                log.trace("readSingleCompoundMember(): converting from unsigned C-type integers");
                memberData = Dataset.convertFromUnsignedC(memberData, null);
            }
            else if (Utils.getJavaObjectRuntimeClass(memberData) == 'B') {
                log.trace("readSingleCompoundMember(): converting byte array member into Object");

                /*
                 * For all other types that get read into memory as a byte[] (such as nested
                 * compounds and arrays of compounds), we must manually convert the byte[] into
                 * something usable.
                 */
                memberData = convertByteMember(memberType, (byte[]) memberData);
            }
        }

        return memberData;
    }

    /*
     * Private routine to write a single field of a compound datatype by creating a
     * compound datatype and inserting the single field into that datatype.
     */
    private void writeSingleCompoundMember(long dsetID, long[] spaceIDs, int nSelPoints,
            final H5Datatype memberType, String memberName, Object theData) throws Exception {
        H5Datatype dsDatatype = (H5Datatype) this.getDatatype();

        /*
         * Check for any unsupported datatypes before attempting to write this compound
         * member.
         */
        if (memberType.isVLEN() && !memberType.isVarStr()) {
            log.debug("writeSingleCompoundMember(): writing of VL non-strings is not currently supported");
            throw new Exception("writing of VL non-strings is not currently supported");
        }

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
            return;
        }

        /*
         * Create a compound datatype containing just a single field (the one which we
         * want to write).
         */
        long compTid = -1;
        try {
            compTid = dsDatatype.createCompoundFieldType(memberName);
        }
        catch (HDF5Exception ex) {
            log.debug("writeSingleCompoundMember(): unable to create compound field type for member of type {}: ",
                    memberType.getDescription(), ex);
        }

        /*
         * Actually write the data now that everything has been setup.
         */
        try {
            if (memberType.isVarStr()) {
                log.trace("writeSingleCompoundMember(): H5Dwrite_string did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                        dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                H5.H5Dwrite_string(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, (String[]) tmpData);
            }
            else {
                log.trace("writeSingleCompoundMember(): H5Dwrite did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                        dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                // BUG!!! does not write nested compound data and no
                // exception was caught. Need to check if it is a java
                // error or C library error.
                H5.H5Dwrite(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, tmpData);
            }
        }
        catch (Exception ex) {
            log.debug("writeSingleCompoundMember(): write failure: ", ex);
            throw new Exception("failed to write compound member: " + ex.getMessage(), ex);
        }
        finally {
            dsDatatype.close(compTid);
        }
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

    /**
     * Retrieves the object's metadata, such as attributes, from the file.
     *
     * Metadata, such as attributes, is stored in a List.
     *
     * @return the list of metadata objects.
     *
     * @throws HDF5Exception
     *             if the metadata can not be retrieved
     */
    @Override
    public List<Attribute> getMetadata() throws HDF5Exception {
        int gmIndexType = 0;
        int gmIndexOrder = 0;

        try {
            gmIndexType = fileFormat.getIndexType(null);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getIndexType failed: ", ex);
        }
        try {
            gmIndexOrder = fileFormat.getIndexOrder(null);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getIndexOrder failed: ", ex);
        }
        return this.getMetadata(gmIndexType, gmIndexOrder);
    }

    /**
     * Retrieves the object's metadata, such as attributes, from the file.
     *
     * Metadata, such as attributes, is stored in a List.
     *
     * @param attrPropList
     *             the list of properties to get
     *
     * @return the list of metadata objects.
     *
     * @throws HDF5Exception
     *             if the metadata can not be retrieved
     */
    public List<Attribute> getMetadata(int... attrPropList) throws HDF5Exception {
        if (!isInited())
            init();

        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        if (objMetadata.getAttributeList() == null) {
            long did = HDF5Constants.H5I_INVALID_HID;
            long pcid = HDF5Constants.H5I_INVALID_HID;
            long paid = HDF5Constants.H5I_INVALID_HID;

            did = open();
            if (did >= 0) {
                try {
                    // get the compression and chunk information
                    pcid = H5.H5Dget_create_plist(did);
                    paid = H5.H5Dget_access_plist(did);
                    long storageSize = H5.H5Dget_storage_size(did);
                    int nfilt = H5.H5Pget_nfilters(pcid);
                    int layoutType = H5.H5Pget_layout(pcid);

                    storageLayout.setLength(0);
                    compression.setLength(0);

                    if (layoutType == HDF5Constants.H5D_CHUNKED) {
                        chunkSize = new long[rank];
                        H5.H5Pget_chunk(pcid, rank, chunkSize);
                        int n = chunkSize.length;
                        storageLayout.append("CHUNKED: ").append(chunkSize[0]);
                        for (int i = 1; i < n; i++)
                            storageLayout.append(" X ").append(chunkSize[i]);

                        if (nfilt > 0) {
                            long nelmts = 1;
                            long uncompSize;
                            long datumSize = getDatatype().getDatatypeSize();

                            if (datumSize < 0) {
                                long tmptid = HDF5Constants.H5I_INVALID_HID;
                                try {
                                    tmptid = H5.H5Dget_type(did);
                                    datumSize = H5.H5Tget_size(tmptid);
                                }
                                finally {
                                    try {
                                        H5.H5Tclose(tmptid);
                                    }
                                    catch (Exception ex2) {
                                        log.debug("getMetadata(): H5Tclose(tmptid {}) failure: ", tmptid, ex2);
                                    }
                                }
                            }

                            for (int i = 0; i < rank; i++)
                                nelmts *= dims[i];
                            uncompSize = nelmts * datumSize;

                            /* compression ratio = uncompressed size / compressed size */

                            if (storageSize != 0) {
                                double ratio = (double) uncompSize / (double) storageSize;
                                DecimalFormat df = new DecimalFormat();
                                df.setMinimumFractionDigits(3);
                                df.setMaximumFractionDigits(3);
                                compression.append(df.format(ratio)).append(":1");
                            }
                        }
                    }
                    else if (layoutType == HDF5Constants.H5D_COMPACT) {
                        storageLayout.append("COMPACT");
                    }
                    else if (layoutType == HDF5Constants.H5D_CONTIGUOUS) {
                        storageLayout.append("CONTIGUOUS");
                        if (H5.H5Pget_external_count(pcid) > 0)
                            storageLayout.append(" - EXTERNAL ");
                    }
                    else if (layoutType == HDF5Constants.H5D_VIRTUAL) {
                        storageLayout.append("VIRTUAL - ");
                        try {
                            long vmaps = H5.H5Pget_virtual_count(pcid);
                            try {
                                int virtView = H5.H5Pget_virtual_view(paid);
                                long virtGap = H5.H5Pget_virtual_printf_gap(paid);
                                if (virtView == HDF5Constants.H5D_VDS_FIRST_MISSING)
                                    storageLayout.append("First Missing");
                                else
                                    storageLayout.append("Last Available");
                                storageLayout.append("\nGAP : ").append(virtGap);
                            }
                            catch (Exception err) {
                                log.debug("getMetadata(): vds error: ", err);
                                storageLayout.append("ERROR");
                            }
                            storageLayout.append("\nMAPS : ").append(vmaps);
                            if (vmaps > 0) {
                                for (long next = 0; next < vmaps; next++) {
                                    try {
                                        H5.H5Pget_virtual_vspace(pcid, next);
                                        H5.H5Pget_virtual_srcspace(pcid, next);
                                        String fname = H5.H5Pget_virtual_filename(pcid, next);
                                        String dsetname = H5.H5Pget_virtual_dsetname(pcid, next);
                                        storageLayout.append("\n").append(fname).append(" : ").append(dsetname);
                                    }
                                    catch (Exception err) {
                                        log.debug("getMetadata(): vds space[{}] error: ", next, err);
                                        storageLayout.append("ERROR");
                                    }
                                }
                            }
                        }
                        catch (Exception err) {
                            log.debug("getMetadata(): vds count error: ", err);
                            storageLayout.append("ERROR");
                        }
                    }
                    else {
                        chunkSize = null;
                        storageLayout.append("NONE");
                    }

                    int[] flags = { 0, 0 };
                    long[] cdNelmts = { 20 };
                    int[] cdValues = new int[(int) cdNelmts[0]];
                    String[] cdName = { "", "" };
                    log.trace("getMetadata(): {} filters in pipeline", nfilt);
                    int filter = -1;
                    int[] filterConfig = { 1 };

                    filters.setLength(0);

                    if (nfilt == 0) {
                        filters.append("NONE");
                    }
                    else {
                        for (int i = 0, k = 0; i < nfilt; i++) {
                            log.trace("getMetadata(): filter[{}]", i);
                            if (i > 0)
                                filters.append(", ");
                            if (k > 0)
                                compression.append(", ");

                            try {
                                cdNelmts[0] = 20;
                                cdValues = new int[(int) cdNelmts[0]];
                                cdValues = new int[(int) cdNelmts[0]];
                                filter = H5.H5Pget_filter(pcid, i, flags, cdNelmts, cdValues, 120, cdName, filterConfig);
                                log.trace("getMetadata(): filter[{}] is {} has {} elements ", i, cdName[0], cdNelmts[0]);
                                for (int j = 0; j < cdNelmts[0]; j++)
                                    log.trace("getMetadata(): filter[{}] element {} = {}", i, j, cdValues[j]);
                            }
                            catch (Exception err) {
                                log.debug("getMetadata(): filter[{}] error: ", i, err);
                                filters.append("ERROR");
                                continue;
                            }

                            if (filter == HDF5Constants.H5Z_FILTER_NONE) {
                                filters.append("NONE");
                            }
                            else if (filter == HDF5Constants.H5Z_FILTER_DEFLATE) {
                                filters.append("GZIP");
                                compression.append(COMPRESSION_GZIP_TXT).append(cdValues[0]);
                                k++;
                            }
                            else if (filter == HDF5Constants.H5Z_FILTER_FLETCHER32) {
                                filters.append("Error detection filter");
                            }
                            else if (filter == HDF5Constants.H5Z_FILTER_SHUFFLE) {
                                filters.append("SHUFFLE: Nbytes = ").append(cdValues[0]);
                            }
                            else if (filter == HDF5Constants.H5Z_FILTER_NBIT) {
                                filters.append("NBIT");
                            }
                            else if (filter == HDF5Constants.H5Z_FILTER_SCALEOFFSET) {
                                filters.append("SCALEOFFSET: MIN BITS = ").append(cdValues[0]);
                            }
                            else if (filter == HDF5Constants.H5Z_FILTER_SZIP) {
                                filters.append("SZIP");
                                compression.append("SZIP: Pixels per block = ").append(cdValues[1]);
                                k++;
                                int flag = -1;
                                try {
                                    flag = H5.H5Zget_filter_info(filter);
                                }
                                catch (Exception ex) {
                                    log.debug("getMetadata(): H5Zget_filter_info failure: ", ex);
                                    flag = -1;
                                }
                                if (flag == HDF5Constants.H5Z_FILTER_CONFIG_DECODE_ENABLED)
                                    compression.append(": H5Z_FILTER_CONFIG_DECODE_ENABLED");
                                else if ((flag == HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED)
                                        || (flag >= (HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED
                                                + HDF5Constants.H5Z_FILTER_CONFIG_DECODE_ENABLED)))
                                    compression.append(": H5Z_FILTER_CONFIG_ENCODE_ENABLED");
                            }
                            else {
                                filters.append("USERDEFINED ").append(cdName[0]).append("(").append(filter).append("): ");
                                for (int j = 0; j < cdNelmts[0]; j++) {
                                    if (j > 0)
                                        filters.append(", ");
                                    filters.append(cdValues[j]);
                                }
                                log.debug("getMetadata(): filter[{}] is user defined compression", i);
                            }
                        } //  (int i=0; i<nfilt; i++)
                    }

                    if (compression.length() == 0)
                        compression.append("NONE");
                    log.trace("getMetadata(): filter compression={}", compression);
                    log.trace("getMetadata(): filter information={}", filters);

                    storage.setLength(0);
                    storage.append("SIZE: ").append(storageSize);

                    try {
                        int[] at = { 0 };
                        H5.H5Pget_alloc_time(pcid, at);
                        storage.append(", allocation time: ");
                        if (at[0] == HDF5Constants.H5D_ALLOC_TIME_EARLY)
                            storage.append("Early");
                        else if (at[0] == HDF5Constants.H5D_ALLOC_TIME_INCR)
                            storage.append("Incremental");
                        else if (at[0] == HDF5Constants.H5D_ALLOC_TIME_LATE)
                            storage.append("Late");
                        else
                            storage.append("Default");
                    }
                    catch (Exception ex) {
                        log.debug("getMetadata(): Storage allocation time:", ex);
                    }
                    log.trace("getMetadata(): storage={}", storage);
                }
                finally {
                    try {
                        H5.H5Pclose(paid);
                    }
                    catch (Exception ex) {
                        log.debug("getMetadata(): H5Pclose(paid {}) failure: ", paid, ex);
                    }
                    try {
                        H5.H5Pclose(pcid);
                    }
                    catch (Exception ex) {
                        log.debug("getMetadata(): H5Pclose(pcid {}) failure: ", pcid, ex);
                    }
                    close(did);
                }
            }
        }

        List<Attribute> attrlist = null;
        try {
            attrlist = objMetadata.getMetadata(attrPropList);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getMetadata failed: ", ex);
        }
        return attrlist;
    }

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
     * @param info
     *            the metadata to write.
     *
     * @throws Exception
     *             if the metadata can not be written
     */
    @Override
    public void writeMetadata(Object info) throws Exception {
        try {
            objMetadata.writeMetadata(info);
        }
        catch (Exception ex) {
            log.debug("writeMetadata(): Object not an Attribute");
            return;
        }
    }

    /**
     * Deletes an existing piece of metadata from this object.
     *
     * @param info
     *            the metadata to delete.
     *
     * @throws HDF5Exception
     *             if the metadata can not be removed
     */
    @Override
    public void removeMetadata(Object info) throws HDF5Exception {
        try {
            objMetadata.removeMetadata(info);
        }
        catch (Exception ex) {
            log.debug("removeMetadata(): Object not an Attribute");
            return;
        }

        Attribute attr = (Attribute) info;
        log.trace("removeMetadata(): {}", attr.getAttributeName());
        long did = open();
        if (did >= 0) {
            try {
                H5.H5Adelete(did, attr.getAttributeName());
            }
            finally {
                close(did);
            }
        }
        else {
            log.debug("removeMetadata(): failed to open compound dataset");
        }
    }

    /**
     * Updates an existing piece of metadata attached to this object.
     *
     * @param info
     *            the metadata to update.
     *
     * @throws HDF5Exception
     *             if the metadata can not be updated
     */
    @Override
    public void updateMetadata(Object info) throws HDF5Exception {
        try {
            objMetadata.updateMetadata(info);
        }
        catch (Exception ex) {
            log.debug("updateMetadata(): Object not an Attribute");
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#setName(java.lang.String)
     */
    @Override
    public void setName(String newName) throws Exception {
        if (newName == null)
            throw new IllegalArgumentException("The new name is NULL");

        H5File.renameObject(this, newName);
        super.setName(newName);
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using
     *             {@link #create(String, Group, long[], long[], long[], int, String[], Datatype[], int[], long[][], Object)}
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param dims
     *            the dimension size of the dataset.
     * @param memberNames
     *            the names of compound datatype
     * @param memberDatatypes
     *            the datatypes of the compound datatype
     * @param memberSizes
     *            the dim sizes of the members
     * @param data
     *            list of data arrays written to the new dataset, null if no data is written to the new
     *            dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception
     *             if there is a failure.
     */
    @Deprecated
    public static Dataset create(String name, Group pgroup, long[] dims, String[] memberNames,
            Datatype[] memberDatatypes, int[] memberSizes, Object data) throws Exception {
        if ((pgroup == null) || (name == null) || (dims == null) || (memberNames == null)
                || (memberDatatypes == null) || (memberSizes == null)) {
            return null;
        }

        int nMembers = memberNames.length;
        int memberRanks[] = new int[nMembers];
        long memberDims[][] = new long[nMembers][1];
        for (int i = 0; i < nMembers; i++) {
            memberRanks[i] = 1;
            memberDims[i][0] = memberSizes[i];
        }

        return H5CompoundDS.create(name, pgroup, dims, memberNames, memberDatatypes, memberRanks, memberDims, data);
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using
     *             {@link #create(String, Group, long[], long[], long[], int, String[], Datatype[], int[], long[][], Object)}
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param dims
     *            the dimension size of the dataset.
     * @param memberNames
     *            the names of compound datatype
     * @param memberDatatypes
     *            the datatypes of the compound datatype
     * @param memberRanks
     *            the ranks of the members
     * @param memberDims
     *            the dim sizes of the members
     * @param data
     *            list of data arrays written to the new dataset, null if no data is written to the new
     *            dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception
     *             if the dataset can not be created.
     */
    @Deprecated
    public static Dataset create(String name, Group pgroup, long[] dims, String[] memberNames,
            Datatype[] memberDatatypes, int[] memberRanks, long[][] memberDims, Object data) throws Exception {
        return H5CompoundDS.create(name, pgroup, dims, null, null, -1, memberNames, memberDatatypes, memberRanks,
                memberDims, data);
    }

    /**
     * Creates a simple compound dataset in a file with/without chunking and compression.
     *
     * This function provides an easy way to create a simple compound dataset in file by hiding tedious
     * details of creating a compound dataset from users.
     *
     * This function calls H5.H5Dcreate() to create a simple compound dataset in file. Nested compound
     * dataset is not supported. The required information to create a compound dataset includes the
     * name, the parent group and data space of the dataset, the names, datatypes and data spaces of the
     * compound fields. Other information such as chunks, compression and the data buffer is optional.
     *
     * The following example shows how to use this function to create a compound dataset in file.
     *
     * <pre>
     * H5File file = null;
     * String message = &quot;&quot;;
     * Group pgroup = null;
     * int[] DATA_INT = new int[DIM_SIZE];
     * float[] DATA_FLOAT = new float[DIM_SIZE];
     * String[] DATA_STR = new String[DIM_SIZE];
     * long[] DIMs = { 50, 10 };
     * long[] CHUNKs = { 25, 5 };
     *
     * try {
     *     file = (H5File) H5FILE.open(fname, H5File.CREATE);
     *     file.open();
     *     pgroup = (Group) file.get(&quot;/&quot;);
     * }
     * catch (Exception ex) {
     * }
     *
     * Vector data = new Vector();
     * data.add(0, DATA_INT);
     * data.add(1, DATA_FLOAT);
     * data.add(2, DATA_STR);
     *
     * // create groups
     * Datatype[] mdtypes = new H5Datatype[3];
     * String[] mnames = { &quot;int&quot;, &quot;float&quot;, &quot;string&quot; };
     * Dataset dset = null;
     * try {
     *     mdtypes[0] = new H5Datatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE);
     *     mdtypes[1] = new H5Datatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, Datatype.NATIVE);
     *     mdtypes[2] = new H5Datatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
     *     dset = file.createCompoundDS(&quot;/CompoundDS&quot;, pgroup, DIMs, null, CHUNKs, 9, mnames, mdtypes, null, data);
     * }
     * catch (Exception ex) {
     *     failed(message, ex, file);
     *     return 1;
     * }
     * </pre>
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param dims
     *            the dimension size of the dataset.
     * @param maxdims
     *            the max dimension size of the dataset. maxdims is set to dims if maxdims = null.
     * @param chunks
     *            the chunk size of the dataset. No chunking if chunk = null.
     * @param gzip
     *            GZIP compression level (1 to 9). 0 or negative values if no compression.
     * @param memberNames
     *            the names of compound datatype
     * @param memberDatatypes
     *            the datatypes of the compound datatype
     * @param memberRanks
     *            the ranks of the members
     * @param memberDims
     *            the dim sizes of the members
     * @param data
     *            list of data arrays written to the new dataset, null if no data is written to the new
     *            dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception
     *             if there is a failure.
     */
    public static Dataset create(String name, Group pgroup, long[] dims, long[] maxdims, long[] chunks, int gzip,
            String[] memberNames, Datatype[] memberDatatypes, int[] memberRanks, long[][] memberDims, Object data) throws Exception {
        H5CompoundDS dataset = null;
        String fullPath = null;
        long did = HDF5Constants.H5I_INVALID_HID;
        long plist = HDF5Constants.H5I_INVALID_HID;
        long sid = HDF5Constants.H5I_INVALID_HID;
        long tid = HDF5Constants.H5I_INVALID_HID;

        if ((pgroup == null) || (name == null) || (dims == null) || ((gzip > 0) && (chunks == null))
                || (memberNames == null) || (memberDatatypes == null) || (memberRanks == null)
                || (memberDims == null)) {
            log.debug("create(): one or more parameters are null");
            return null;
        }

        H5File file = (H5File) pgroup.getFileFormat();
        if (file == null) {
            log.debug("create(): parent group FileFormat is null");
            return null;
        }

        String path = HObject.SEPARATOR;
        if (!pgroup.isRoot()) {
            path = pgroup.getPath() + pgroup.getName() + HObject.SEPARATOR;
            if (name.endsWith("/"))
                name = name.substring(0, name.length() - 1);
            int idx = name.lastIndexOf('/');
            if (idx >= 0)
                name = name.substring(idx + 1);
        }

        fullPath = path + name;

        int typeSize = 0;
        int nMembers = memberNames.length;
        long[] mTypes = new long[nMembers];
        int memberSize = 1;
        for (int i = 0; i < nMembers; i++) {
            memberSize = 1;
            for (int j = 0; j < memberRanks[i]; j++)
                memberSize *= memberDims[i][j];

            mTypes[i] = -1;
            // the member is an array
            if ((memberSize > 1) && (!memberDatatypes[i].isString())) {
                long tmptid = -1;
                if ((tmptid = memberDatatypes[i].createNative()) >= 0) {
                    try {
                        mTypes[i] = H5.H5Tarray_create(tmptid, memberRanks[i], memberDims[i]);
                    }
                    finally {
                        try {
                            H5.H5Tclose(tmptid);
                        }
                        catch (Exception ex) {
                            log.debug("create(): H5Tclose(tmptid {}) failure: ", tmptid, ex);
                        }
                    }
                }
            }
            else {
                mTypes[i] = memberDatatypes[i].createNative();
            }
            try {
                typeSize += H5.H5Tget_size(mTypes[i]);
            }
            catch (Exception ex) {
                log.debug("create(): array create H5Tget_size:", ex);

                while (i > 0) {
                    try {
                        H5.H5Tclose(mTypes[i]);
                    }
                    catch (HDF5Exception ex2) {
                        log.debug("create(): H5Tclose(mTypes[{}] {}) failure: ", i, mTypes[i], ex2);
                    }
                    i--;
                }
                throw ex;
            }
        } //  (int i = 0; i < nMembers; i++) {

        // setup chunking and compression
        boolean isExtentable = false;
        if (maxdims != null) {
            for (int i = 0; i < maxdims.length; i++) {
                if (maxdims[i] == 0)
                    maxdims[i] = dims[i];
                else if (maxdims[i] < 0)
                    maxdims[i] = HDF5Constants.H5S_UNLIMITED;

                if (maxdims[i] != dims[i])
                    isExtentable = true;
            }
        }

        // HDF5 requires you to use chunking in order to define extendible
        // datasets. Chunking makes it possible to extend datasets efficiently,
        // without having to reorganize storage excessively. Using default size
        // of 64x...which has good performance
        if ((chunks == null) && isExtentable) {
            chunks = new long[dims.length];
            for (int i = 0; i < dims.length; i++)
                chunks[i] = Math.min(dims[i], 64);
        }

        // prepare the dataspace and datatype
        int rank = dims.length;

        try {
            sid = H5.H5Screate_simple(rank, dims, maxdims);

            // figure out creation properties
            plist = HDF5Constants.H5P_DEFAULT;

            tid = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, typeSize);
            int offset = 0;
            for (int i = 0; i < nMembers; i++) {
                H5.H5Tinsert(tid, memberNames[i], offset, mTypes[i]);
                offset += H5.H5Tget_size(mTypes[i]);
            }

            if (chunks != null) {
                plist = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);

                H5.H5Pset_layout(plist, HDF5Constants.H5D_CHUNKED);
                H5.H5Pset_chunk(plist, rank, chunks);

                // compression requires chunking
                if (gzip > 0) {
                    H5.H5Pset_deflate(plist, gzip);
                }
            }

            long fid = file.getFID();

            did = H5.H5Dcreate(fid, fullPath, tid, sid, HDF5Constants.H5P_DEFAULT, plist, HDF5Constants.H5P_DEFAULT);
            dataset = new H5CompoundDS(file, name, path);
        }
        finally {
            try {
                H5.H5Pclose(plist);
            }
            catch (HDF5Exception ex) {
                log.debug("create(): H5Pclose(plist {}) failure: ", plist, ex);
            }
            try {
                H5.H5Sclose(sid);
            }
            catch (HDF5Exception ex) {
                log.debug("create(): H5Sclose(sid {}) failure: ", sid, ex);
            }
            try {
                H5.H5Tclose(tid);
            }
            catch (HDF5Exception ex) {
                log.debug("create(): H5Tclose(tid {}) failure: ", tid, ex);
            }
            try {
                H5.H5Dclose(did);
            }
            catch (HDF5Exception ex) {
                log.debug("create(): H5Dclose(did {}) failure: ", did, ex);
            }

            for (int i = 0; i < nMembers; i++) {
                try {
                    H5.H5Tclose(mTypes[i]);
                }
                catch (HDF5Exception ex) {
                    log.debug("create(): H5Tclose(mTypes[{}] {}) failure: ", i, mTypes[i], ex);
                }
            }
        }

        if (dataset != null) {
            pgroup.addToMemberList(dataset);
            if (data != null) {
                dataset.init();
                long selected[] = dataset.getSelectedDims();
                for (int i = 0; i < rank; i++)
                    selected[i] = dims[i];
                dataset.write(data);
            }
        }

        return dataset;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#isString(long)
     */
    @Override
    public boolean isString(long tid) {
        boolean b = false;
        try {
            b = (HDF5Constants.H5T_STRING == H5.H5Tget_class(tid));
        }
        catch (Exception ex) {
            b = false;
        }

        return b;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getSize(long)
     */
    @Override
    public long getSize(long tid) {
        return H5Datatype.getDatatypeSize(tid);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#isVirtual()
     */
    @Override
    public boolean isVirtual() {
        return isVirtual;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getVirtualFilename(int)
     */
    @Override
    public String getVirtualFilename(int index) {
        if(isVirtual)
            return virtualNameList.get(index);
        else
            return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getVirtualMaps()
     */
    @Override
    public int getVirtualMaps() {
        if(isVirtual)
            return virtualNameList.size();
        else
            return -1;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#toString(String delimiter, int maxItems)
     */
    @Override
    public String toString(String delimiter, int maxItems) {
        Object theData = originalBuf;
        if (theData == null) {
            log.debug("toString: value is null");
            return null;
        }

        if (theData instanceof List<?>) {
            log.trace("toString: value is list");
            return null;
        }

        Class<? extends Object> valClass = theData.getClass();

        if (!valClass.isArray()) {
            log.trace("toString: finish - not array");
            String strValue = theData.toString();
            if (maxItems > 0 && strValue.length() > maxItems)
                // truncate the extra characters
                strValue = strValue.substring(0, maxItems);
            return strValue;
        }

        // value is an array
        StringBuilder sb = new StringBuilder();
        int n = Array.getLength(theData);
        if ((maxItems > 0) && (n > maxItems))
            n = maxItems;

        log.trace("toString: isStdRef={} Array.getLength={}", ((H5Datatype) getDatatype()).isStdRef(), n);
        if (((H5Datatype) getDatatype()).isStdRef()) {
            String cname = valClass.getName();
            char dname = cname.charAt(cname.lastIndexOf('[') + 1);
            log.trace("toString: isStdRef with cname={} dname={}", cname, dname);
            String ref_str = ((H5ReferenceType) getDatatype()).getObjectReferenceName((byte[])theData);
            log.trace("toString: ref_str={}", ref_str);
            return ref_str;
        }
        else {
            return super.toString(delimiter, maxItems);
        }
    }

}
