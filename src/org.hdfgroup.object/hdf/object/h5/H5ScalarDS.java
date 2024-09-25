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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;
import hdf.object.ScalarDS;
import hdf.object.h5.H5Attribute;
import hdf.object.h5.H5MetaDataContainer;
import hdf.object.h5.H5ReferenceType;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFArray;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5DataFiltersException;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.hdf5lib.structs.H5O_token_t;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * H5ScalarDS describes a multi-dimension array of HDF5 scalar or atomic data types, such as byte, int, short,
 * long, float, double and string, and operations performed on the scalar dataset.
 *
 * The library predefines a modest number of datatypes. For details, read <a href=
 * "https://support.hdfgroup.org/releases/hdf5/v1_14/v1_14_5/documentation/doxygen/_h5_t__u_g.html#sec_datatype">HDF5
 * Datatypes in HDF5 User Guide</a>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5ScalarDS extends ScalarDS implements MetaDataContainer {
    private static final long serialVersionUID = 2887517608230611642L;

    private static final Logger log = LoggerFactory.getLogger(H5ScalarDS.class);

    /**
     * The metadata object for this data object. Members of the metadata are instances of Attribute.
     */
    private H5MetaDataContainer objMetadata;

    /** the object properties */
    private H5O_info_t objInfo;

    /** the number of palettes */
    private int NumberOfPalettes;

    /** flag to indicate if the dataset is an external dataset */
    private boolean isExternal = false;

    /** flag to indicate if the dataset is a virtual dataset */
    private boolean isVirtual = false;
    /** the list of virtual names */
    private List<String> virtualNameList;

    /**
     * flag to indicate if the dataset buffers should be refreshed.
     */
    protected boolean refresh = false;

    /**
     * flag to indicate if the datatype in file is the same as dataype in memory
     */
    protected boolean isNativeDatatype = false;

    /**
     * Constructs an instance of a H5 scalar dataset with given file, dataset name and path.
     *
     * For example, in H5ScalarDS(h5file, "dset", "/arrays/"), "dset" is the name of the dataset, "/arrays" is
     * the group path of the dataset.
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     */
    public H5ScalarDS(FileFormat theFile, String theName, String thePath)
    {
        this(theFile, theName, thePath, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #H5ScalarDS(FileFormat, String, String)}
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
    public H5ScalarDS(FileFormat theFile, String theName, String thePath, long[] oid)
    {
        super(theFile, theName, thePath, oid);
        unsignedConverted = false;
        NumberOfPalettes  = 0;
        objMetadata       = new H5MetaDataContainer(theFile, theName, thePath, this);

        if (theFile != null) {
            if (oid == null) {
                // retrieve the object ID
                byte[] refBuf = null;
                try {
                    refBuf =
                        H5.H5Rcreate_object(theFile.getFID(), this.getFullName(), HDF5Constants.H5P_DEFAULT);
                    this.oid = HDFNativeData.byteToLong(refBuf);
                    log.trace("constructor REF {} to OID {}", refBuf, this.oid);
                }
                catch (Exception ex) {
                    log.debug("constructor ID {} for {} failed H5Rcreate_object", theFile.getFID(),
                              this.getFullName());
                }
                finally {
                    if (refBuf != null)
                        H5.H5Rdestroy(refBuf);
                }
            }
            log.trace("constructor OID {}", this.oid);
            try {
                objInfo = H5.H5Oget_info_by_name(theFile.getFID(), this.getFullName(),
                                                 HDF5Constants.H5O_INFO_BASIC, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                objInfo = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
            }
        }
        else {
            this.oid = null;
            objInfo  = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open()
    {
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
    public void close(long did)
    {
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
    public void init()
    {
        if (inited) {
            // already called. Initialize only once
            resetSelection();
            log.trace("init(): H5ScalarDS already initialized");
            return;
        }

        long did       = HDF5Constants.H5I_INVALID_HID;
        long tid       = HDF5Constants.H5I_INVALID_HID;
        long sid       = HDF5Constants.H5I_INVALID_HID;
        long nativeTID = HDF5Constants.H5I_INVALID_HID;

        did = open();
        if (did >= 0) {
            try {
                H5.H5Drefresh(did);
            }
            catch (Exception ex) {
                log.debug("H5Drefresh(): ", ex);
            }
            // check if it is an external or virtual dataset
            long pid = HDF5Constants.H5I_INVALID_HID;
            try {
                pid = H5.H5Dget_create_plist(did);
                try {
                    int nfiles     = H5.H5Pget_external_count(pid);
                    isExternal     = (nfiles > 0);
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
                    log.trace("init(): pid={} nfiles={} isExternal={} isVirtual={}", pid, nfiles, isExternal,
                              isVirtual);
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

            NumberOfPalettes = readNumberOfPalette(did);

            try {
                sid        = H5.H5Dget_space(did);
                rank       = H5.H5Sget_simple_extent_ndims(sid);
                space_type = H5.H5Sget_simple_extent_type(sid);
                if (space_type == HDF5Constants.H5S_NULL)
                    isNULL = true;
                else
                    isNULL = false;
                tid = H5.H5Dget_type(did);
                log.trace("init(): tid={} sid={} rank={} space_type={} ", tid, sid, rank, space_type);

                if (rank == 0) {
                    // a scalar data point
                    isScalar = true;
                    rank     = 1;
                    dims     = new long[] {1};
                    log.trace("init(): rank is a scalar data point");
                }
                else {
                    isScalar = false;
                    dims     = new long[rank];
                    maxDims  = new long[rank];
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

                    log.trace(
                        "init(): tid={} is tclass={} has isText={} : isNamed={} :  isVLEN={} : isEnum={} : isUnsigned={} : isStdRef={} : isRegRef={}",
                        tid, datatype.getDatatypeClass(), ((H5Datatype)datatype).isText(), datatype.isNamed(),
                        datatype.isVLEN(), datatype.isEnum(), datatype.isUnsigned(),
                        ((H5Datatype)datatype).isStdRef(), ((H5Datatype)datatype).isRegRef());
                }
                catch (Exception ex) {
                    log.debug("init(): failed to create datatype for dataset: ", ex);
                    datatype = null;
                }

                // Check if the datatype in the file is the native datatype
                try {
                    nativeTID        = H5.H5Tget_native_type(tid);
                    isNativeDatatype = H5.H5Tequal(tid, nativeTID);
                    log.trace("init(): isNativeDatatype={}", isNativeDatatype);
                }
                catch (Exception ex) {
                    log.debug("init(): check if native type failure: ", ex);
                }

                try {
                    pid              = H5.H5Dget_create_plist(did);
                    int[] fillStatus = {0};
                    if (H5.H5Pfill_value_defined(pid, fillStatus) >= 0) {
                        // Check if fill value is user-defined before retrieving it.
                        if (fillStatus[0] == HDF5Constants.H5D_FILL_VALUE_USER_DEFINED) {
                            try {
                                fillValue = H5Datatype.allocateArray((H5Datatype)datatype, 1);
                            }
                            catch (OutOfMemoryError e) {
                                log.debug("init(): out of memory: ", e);
                                fillValue = null;
                            }
                            catch (Exception ex) {
                                log.debug("init(): allocate fill value buffer failed: ", ex);
                                fillValue = null;
                            }

                            log.trace("init(): fillValue={}", fillValue);
                            try {
                                H5.H5Pget_fill_value(pid, nativeTID, fillValue);
                                log.trace("init(): H5Pget_fill_value={}", fillValue);
                                if (fillValue != null) {
                                    if (datatype.isUnsigned() && !isFillValueConverted) {
                                        fillValue            = ScalarDS.convertFromUnsignedC(fillValue, null);
                                        isFillValueConverted = true;
                                    }

                                    int n = Array.getLength(fillValue);
                                    for (int i = 0; i < n; i++)
                                        addFilteredImageValue((Number)Array.get(fillValue, i));
                                }
                            }
                            catch (Exception ex2) {
                                log.debug("init(): fill value was defined: ", ex2);
                                fillValue = null;
                            }
                        }
                    }
                }
                catch (HDF5Exception ex) {
                    log.debug("init(): check if fill value is defined failure: ", ex);
                }
                finally {
                    try {
                        H5.H5Pclose(pid);
                    }
                    catch (Exception ex) {
                        log.debug("init(): H5Pclose(pid {}) failure: ", pid, ex);
                    }
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

            // check for the type of image and interlace mode
            // it is a true color image at one of three cases:
            // 1) IMAGE_SUBCLASS = IMAGE_TRUECOLOR,
            // 2) INTERLACE_MODE = INTERLACE_PIXEL,
            // 3) INTERLACE_MODE = INTERLACE_PLANE
            if ((rank >= 3) && isImage) {
                interlace   = -1;
                isTrueColor = isStringAttributeOf(did, "IMAGE_SUBCLASS", "IMAGE_TRUECOLOR");

                if (isTrueColor) {
                    interlace = INTERLACE_PIXEL;
                    if (isStringAttributeOf(did, "INTERLACE_MODE", "INTERLACE_PLANE")) {
                        interlace = INTERLACE_PLANE;
                    }
                }
            }

            close(did);

            startDims    = new long[rank];
            selectedDims = new long[rank];

            resetSelection();
        }
        else {
            log.debug("init(): failed to open dataset");
        }
        refresh = false;
    }

    /**
     * Get the token for this object.
     *
     * @return true if it has any attributes, false otherwise.
     */
    public long[] getToken()
    {
        H5O_token_t token = objInfo.token;
        return HDFNativeData.byteToLong(token.data);
    }

    /**
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attributes, false otherwise.
     */
    @Override
    public boolean hasAttribute()
    {
        objInfo.num_attrs = objMetadata.getObjectAttributeSize();

        if (objInfo.num_attrs < 0) {
            long did = open();
            if (did >= 0) {
                objInfo.num_attrs = 0;

                try {
                    objInfo = H5.H5Oget_info(did);

                    if (objInfo.num_attrs > 0) {
                        // test if it is an image
                        // check image
                        Object avalue = getAttrValue(did, "CLASS");
                        if (avalue != null) {
                            try {
                                isImageDisplay = isImage =
                                    "IMAGE".equalsIgnoreCase(new String((byte[])avalue).trim());
                                log.trace("hasAttribute(): isImageDisplay dataset: {} with value = {}",
                                          isImageDisplay, avalue);
                            }
                            catch (Exception err) {
                                log.debug("hasAttribute(): check image: ", err);
                            }
                        }

                        // retrieve the IMAGE_MINMAXRANGE
                        avalue = getAttrValue(did, "IMAGE_MINMAXRANGE");
                        if (avalue != null) {
                            double x0 = 0;
                            double x1 = 0;
                            try {
                                x0 = Double.parseDouble(java.lang.reflect.Array.get(avalue, 0).toString());
                                x1 = Double.parseDouble(java.lang.reflect.Array.get(avalue, 1).toString());
                            }
                            catch (Exception ex2) {
                                x0 = x1 = 0;
                            }
                            if (x1 > x0) {
                                imageDataRange    = new double[2];
                                imageDataRange[0] = x0;
                                imageDataRange[1] = x1;
                            }
                        }

                        try {
                            checkCFconvention(did);
                        }
                        catch (Exception ex) {
                            log.debug("hasAttribute(): checkCFconvention(did {}):", did, ex);
                        }
                    }
                }
                catch (Exception ex) {
                    objInfo.num_attrs = 0;
                    log.debug("hasAttribute(): get object info failure: ", ex);
                }
                finally {
                    close(did);
                }
                objMetadata.setObjectAttributeSize((int)objInfo.num_attrs);
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
    public Datatype getDatatype()
    {
        if (!inited)
            init();

        if (datatype == null) {
            long did = HDF5Constants.H5I_INVALID_HID;
            long tid = HDF5Constants.H5I_INVALID_HID;

            did = open();
            if (did >= 0) {
                try {
                    tid = H5.H5Dget_type(did);
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
     * Refreshes the dataset before re-read of data.
     */
    @Override
    public Object refreshData()
    {
        inited  = false;
        refresh = true;

        init();
        return super.refreshData();
    }

    /**
     * Removes all of the elements from metadata list.
     * The list should be empty after this call returns.
     */
    @Override
    public void clear()
    {
        super.clear();
        objMetadata.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#readBytes()
     */
    @Override
    public byte[] readBytes() throws HDF5Exception
    {
        byte[] theData = null;

        if (!isInited())
            init();

        long did = open();
        if (did >= 0) {
            long fspace = HDF5Constants.H5I_INVALID_HID;
            long mspace = HDF5Constants.H5I_INVALID_HID;
            long tid    = HDF5Constants.H5I_INVALID_HID;

            try {
                long[] lsize = {1};
                for (int j = 0; j < selectedDims.length; j++)
                    lsize[0] *= selectedDims[j];

                fspace = H5.H5Dget_space(did);
                mspace = H5.H5Screate_simple(rank, selectedDims, null);

                // set the rectangle selection
                // HDF5 bug: for scalar dataset, H5Sselect_hyperslab gives core dump
                if (rank * dims[0] > 1)
                    H5.H5Sselect_hyperslab(fspace, HDF5Constants.H5S_SELECT_SET, startDims, selectedStride,
                                           selectedDims, null); // set block to 1

                tid       = H5.H5Dget_type(did);
                long size = H5.H5Tget_size(tid) * lsize[0];
                log.trace("readBytes(): size = {}", size);

                if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE)
                    throw new Exception("Invalid int size");

                theData = new byte[(int)size];

                log.trace("readBytes(): H5Dread: did={} tid={} fspace={} mspace={}", did, tid, fspace,
                          mspace);
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
     * For ScalarDS, the memory data buffer is a one-dimensional array of byte,
     * short, int, float, double or String type based on the datatype of the
     * dataset.
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
    public Object read() throws Exception
    {
        Object readData = null;

        if (!isInited())
            init();

        try {
            readData = scalarDatasetCommonIO(H5File.IO_TYPE.READ, null);
        }
        catch (Exception ex) {
            log.debug("read(): failed to read scalar dataset: ", ex);
            throw new Exception("failed to read scalar dataset: " + ex.getMessage(), ex);
        }

        return readData;
    }

    /**
     * Writes the given data buffer into this dataset in a file.
     *
     * @param buf
     *            The buffer that contains the data values.
     *
     * @throws Exception
     *             If there is an error at the HDF5 library level.
     */
    @Override
    public void write(Object buf) throws Exception
    {
        if (this.getFileFormat().isReadOnly())
            throw new Exception("cannot write to scalar dataset in file opened as read-only");

        if (!isInited())
            init();

        try {
            scalarDatasetCommonIO(H5File.IO_TYPE.WRITE, buf);
        }
        catch (Exception ex) {
            log.debug("write(Object): failed to write to scalar dataset: ", ex);
            throw new Exception("failed to write to scalar dataset: " + ex.getMessage(), ex);
        }
    }

    private Object scalarDatasetCommonIO(H5File.IO_TYPE ioType, Object writeBuf) throws Exception
    {
        H5Datatype dsDatatype     = (H5Datatype)getDatatype();
        H5Datatype dsBaseDatatype = (H5Datatype)getDatatype().getDatatypeBase();
        boolean BDTisRef          = false;
        if (dsBaseDatatype != null)
            BDTisRef = dsBaseDatatype.isStdRef();
        Object theData = null;

        /*
         * I/O type-specific pre-initialization.
         */
        if (ioType == H5File.IO_TYPE.WRITE) {
            if (writeBuf == null) {
                log.debug("scalarDatasetCommonIO(): writeBuf is null");
                throw new Exception("write buffer is null");
            }
        }

        long did = open();
        if (did >= 0) {
            long[] spaceIDs = {HDF5Constants.H5I_INVALID_HID,
                               HDF5Constants.H5I_INVALID_HID}; // spaceIDs[0]=mspace, spaceIDs[1]=fspace

            try {
                /*
                 * NOTE: this call sets up a hyperslab selection in the file according to the
                 * current selection in the dataset object.
                 */
                long totalSelectedSpacePoints = H5Utils.getTotalSelectedSpacePoints(
                    did, dims, startDims, selectedStride, selectedDims, spaceIDs);

                if (ioType == H5File.IO_TYPE.READ) {
                    log.trace(
                        "scalarDatasetCommonIO():read ioType isNamed={} isEnum={} isText={} isRefObj={}",
                        dsDatatype.isNamed(), dsDatatype.isEnum(), dsDatatype.isText(),
                        dsDatatype.isRefObj());
                    if (dsDatatype.isVarStr()) {
                        try {
                            theData = H5Datatype.allocateArray(dsDatatype, (int)totalSelectedSpacePoints);
                        }
                        catch (OutOfMemoryError err) {
                            log.debug("scalarDatasetCommonIO(): Out of memory");
                            throw new HDF5Exception("Out Of Memory");
                        }
                    }
                    else if (dsDatatype.isVLEN()) {
                        theData = new ArrayList[(int)totalSelectedSpacePoints];
                        for (int j = 0; j < (int)totalSelectedSpacePoints; j++)
                            ((ArrayList[])theData)[j] = new ArrayList<byte[]>();
                    }
                    else if ((originalBuf == null) || dsDatatype.isEnum() || dsDatatype.isText() ||
                             dsDatatype.isRefObj() ||
                             ((originalBuf != null) && (totalSelectedSpacePoints != nPoints))) {
                        try {
                            theData = H5Datatype.allocateArray(dsDatatype, (int)totalSelectedSpacePoints);
                        }
                        catch (OutOfMemoryError err) {
                            log.debug("scalarDatasetCommonIO(): Out of memory");
                            throw new HDF5Exception("Out Of Memory");
                        }
                    }
                    else {
                        // reuse the buffer if the size is the same
                        log.trace(
                            "scalarDatasetCommonIO():read ioType reuse the buffer if the size is the same");
                        theData = originalBuf;
                    }

                    if (theData != null) {
                        /*
                         * Actually read the data now that everything has been setup.
                         */
                        long tid = HDF5Constants.H5I_INVALID_HID;
                        try {
                            log.trace("scalarDatasetCommonIO():read ioType create native");
                            tid = dsDatatype.createNative();

                            if (dsDatatype.isVarStr()) {
                                log.trace(
                                    "scalarDatasetCommonIO(): H5Dread_VLStrings did={} tid={} spaceIDs[0]={} spaceIDs[1]={}",
                                    did, tid,
                                    (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                    (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                                H5.H5Dread_VLStrings(did, tid, spaceIDs[0], spaceIDs[1],
                                                     HDF5Constants.H5P_DEFAULT, (Object[])theData);
                            }
                            else if (dsDatatype.isVLEN() ||
                                     (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isVLEN())) {
                                log.trace(
                                    "scalarDatasetCommonIO(): H5DreadVL did={} tid={} spaceIDs[0]={} spaceIDs[1]={}",
                                    did, tid,
                                    (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                    (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                                H5.H5DreadVL(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT,
                                             (Object[])theData);
                            }
                            else {
                                log.trace(
                                    "scalarDatasetCommonIO(): H5Dread did={} tid={} spaceIDs[0]={} spaceIDs[1]={}",
                                    did, tid,
                                    (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                    (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                                H5.H5Dread(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT,
                                           theData);
                            }
                        }
                        catch (HDF5DataFiltersException exfltr) {
                            log.debug("scalarDatasetCommonIO(): read failure: ", exfltr);
                            throw new Exception("Filter not available exception: " + exfltr.getMessage(),
                                                exfltr);
                        }
                        catch (Exception ex) {
                            log.debug("scalarDatasetCommonIO(): read failure: ", ex);
                            throw new Exception(ex.getMessage(), ex);
                        }
                        finally {
                            dsDatatype.close(tid);
                        }

                        /*
                         * Perform any necessary data conversions.
                         */
                        if (dsDatatype.isText() && convertByteToString && (theData instanceof byte[])) {
                            log.trace(
                                "scalarDatasetCommonIO(): isText: converting byte array to string array");
                            theData = byteToString((byte[])theData, (int)dsDatatype.getDatatypeSize());
                        }
                        else if (dsDatatype.isFloat() && dsDatatype.getDatatypeSize() == 16) {
                            log.trace(
                                "scalarDatasetCommonIO(): isFloat: converting byte array to BigDecimal array");
                            theData = dsDatatype.byteToBigDecimal(0, (int)totalSelectedSpacePoints,
                                                                  (byte[])theData);
                        }
                        else if (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isFloat() &&
                                 dsDatatype.getDatatypeBase().getDatatypeSize() == 16) {
                            log.trace(
                                "scalarDatasetCommonIO(): isArray and isFloat: converting byte array to BigDecimal array");
                            long[] arrayDims = dsDatatype.getArrayDims();
                            int asize        = (int)totalSelectedSpacePoints;
                            for (int j = 0; j < arrayDims.length; j++) {
                                asize *= arrayDims[j];
                            }
                            theData = ((H5Datatype)dsDatatype.getDatatypeBase())
                                          .byteToBigDecimal(0, asize, (byte[])theData);
                        }
                        else if (dsDatatype.isRef() && (theData instanceof byte[])) {
                            log.trace(
                                "scalarDatasetCommonIO():read ioType isRef: converting byte array to List of bytes");
                            ArrayList<byte[]> theListData = new ArrayList<>((int)totalSelectedSpacePoints);
                            for (int m = 0; m < (int)totalSelectedSpacePoints; m++) {
                                byte[] curBytes = new byte[(int)dsDatatype.getDatatypeSize()];
                                try {
                                    System.arraycopy(theData, m * (int)dsDatatype.getDatatypeSize(), curBytes,
                                                     0, (int)dsDatatype.getDatatypeSize());
                                    theListData.add(curBytes);
                                }
                                catch (Exception err) {
                                    log.trace("scalarDatasetCommonIO(): arraycopy failure: ", err);
                                }
                            }
                            theData = theListData;
                        }
                    }
                } // H5File.IO_TYPE.READ
                else {
                    /*
                     * Perform any necessary data conversions before writing the data.
                     *
                     * Note that v-len strings do not get converted, regardless of
                     * conversion request type.
                     */
                    Object tmpData = writeBuf;
                    try {
                        // Check if we need to convert integer data
                        int tsize    = (int)dsDatatype.getDatatypeSize();
                        String cname = writeBuf.getClass().getName();
                        log.trace("scalarDatasetCommonIO(): cname={} of datatype size={}", cname, tsize);
                        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
                        boolean doIntConversion =
                            (((tsize == 1) && (dname == 'S')) || ((tsize == 2) && (dname == 'I')) ||
                             ((tsize == 4) && (dname == 'J')) ||
                             (dsDatatype.isUnsigned() && unsignedConverted));

                        if (doIntConversion) {
                            log.trace(
                                "scalarDatasetCommonIO(): converting integer data to unsigned C-type integers");
                            tmpData = convertToUnsignedC(writeBuf, null);
                        }
                        else if (dsDatatype.isText() && !dsDatatype.isVarStr() && convertByteToString &&
                                 !(writeBuf instanceof byte[])) {
                            log.trace("scalarDatasetCommonIO(): converting string array to byte array");
                            tmpData = stringToByte((String[])writeBuf, tsize);
                        }
                        else if (dsDatatype.isEnum() && (Array.get(writeBuf, 0) instanceof String)) {
                            log.trace("scalarDatasetCommonIO(): converting enum names to values");
                            tmpData = dsDatatype.convertEnumNameToValue((String[])writeBuf);
                        }
                        else if (dsDatatype.isFloat() && dsDatatype.getDatatypeSize() == 16) {
                            log.trace(
                                "scalarDatasetCommonIO(): isFloat: converting BigDecimal array to byte array");
                            throw new Exception("data conversion failure: cannot write BigDecimal values");
                            // tmpData = dsDatatype.bigDecimalToByte(0, (int)totalSelectedSpacePoints,
                            // (BigDecimal[]) writeBuf);
                        }
                    }
                    catch (Exception ex) {
                        log.debug("scalarDatasetCommonIO(): data conversion failure: ", ex);
                        throw new Exception("data conversion failure: " + ex.getMessage());
                    }

                    /*
                     * Actually write the data now that everything has been setup.
                     */
                    long tid = HDF5Constants.H5I_INVALID_HID;
                    try {
                        tid = dsDatatype.createNative();

                        if (dsDatatype.isVarStr()) {
                            log.trace(
                                "scalarDatasetCommonIO(): H5Dwrite_VLStrings did={} tid={} spaceIDs[0]={} spaceIDs[1]={}",
                                did, tid,
                                (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                            H5.H5Dwrite_VLStrings(did, tid, spaceIDs[0], spaceIDs[1],
                                                  HDF5Constants.H5P_DEFAULT, (Object[])tmpData);
                        }
                        else if (dsDatatype.isVLEN() ||
                                 (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isVLEN())) {
                            log.trace(
                                "scalarDatasetCommonIO(): H5DwriteVL did={} tid={} spaceIDs[0]={} spaceIDs[1]={}",
                                did, tid,
                                (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                            H5.H5DwriteVL(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT,
                                          (Object[])tmpData);
                        }
                        else {
                            log.trace(
                                "scalarDatasetCommonIO(): H5Dwrite did={} tid={} spaceIDs[0]={} spaceIDs[1]={}",
                                did, tid,
                                (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                                (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                            H5.H5Dwrite(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT,
                                        tmpData);
                        }
                    }
                    catch (Exception ex) {
                        log.debug("scalarDatasetCommonIO(): write failure: ", ex);
                        throw new Exception(ex.getMessage());
                    }
                    finally {
                        dsDatatype.close(tid);
                    }
                } // H5File.IO_TYPE.WRITE
            }
            finally {
                if (HDF5Constants.H5S_ALL != spaceIDs[0]) {
                    try {
                        H5.H5Sclose(spaceIDs[0]);
                    }
                    catch (Exception ex) {
                        log.debug("scalarDatasetCommonIO(): H5Sclose(spaceIDs[0] {}) failure: ", spaceIDs[0],
                                  ex);
                    }
                }

                if (HDF5Constants.H5S_ALL != spaceIDs[1]) {
                    try {
                        H5.H5Sclose(spaceIDs[1]);
                    }
                    catch (Exception ex) {
                        log.debug("scalarDatasetCommonIO(): H5Sclose(spaceIDs[1] {}) failure: ", spaceIDs[1],
                                  ex);
                    }
                }

                close(did);
            }
        }
        else
            log.debug("scalarDatasetCommonIO(): failed to open dataset");

        return theData;
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
    public List<Attribute> getMetadata() throws HDF5Exception
    {
        int gmIndexType  = 0;
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
    public List<Attribute> getMetadata(int... attrPropList) throws HDF5Exception
    {
        if (!isInited())
            init();

        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        if (objMetadata.getAttributeList() == null) {
            long did  = HDF5Constants.H5I_INVALID_HID;
            long pcid = HDF5Constants.H5I_INVALID_HID;
            long paid = HDF5Constants.H5I_INVALID_HID;

            did = open();
            if (did >= 0) {
                try {
                    // get the compression and chunk information
                    pcid             = H5.H5Dget_create_plist(did);
                    paid             = H5.H5Dget_access_plist(did);
                    long storageSize = H5.H5Dget_storage_size(did);
                    int nfilt        = H5.H5Pget_nfilters(pcid);
                    int layoutType   = H5.H5Pget_layout(pcid);

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
                                    tmptid    = H5.H5Dget_type(did);
                                    datumSize = H5.H5Tget_size(tmptid);
                                }
                                finally {
                                    try {
                                        H5.H5Tclose(tmptid);
                                    }
                                    catch (Exception ex2) {
                                        log.debug("getMetadata(): H5Tclose(tmptid {}) failure: ", tmptid,
                                                  ex2);
                                    }
                                }
                            }

                            for (int i = 0; i < rank; i++)
                                nelmts *= dims[i];
                            uncompSize = nelmts * datumSize;

                            /* compression ratio = uncompressed size / compressed size */

                            if (storageSize != 0) {
                                double ratio     = (double)uncompSize / (double)storageSize;
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
                                        String fname    = H5.H5Pget_virtual_filename(pcid, next);
                                        String dsetname = H5.H5Pget_virtual_dsetname(pcid, next);
                                        storageLayout.append("\n").append(fname).append(" : ").append(
                                            dsetname);
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

                    int[] flags     = {0, 0};
                    long[] cdNelmts = {20};
                    int[] cdValues  = new int[(int)cdNelmts[0]];
                    String[] cdName = {"", ""};
                    log.trace("getMetadata(): {} filters in pipeline", nfilt);
                    int filter         = -1;
                    int[] filterConfig = {1};

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
                                cdValues    = new int[(int)cdNelmts[0]];
                                cdValues    = new int[(int)cdNelmts[0]];
                                filter = H5.H5Pget_filter(pcid, i, flags, cdNelmts, cdValues, 120, cdName,
                                                          filterConfig);
                                log.trace("getMetadata(): filter[{}] is {} has {} elements ", i, cdName[0],
                                          cdNelmts[0]);
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
                                else if ((flag == HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED) ||
                                         (flag >= (HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED +
                                                   HDF5Constants.H5Z_FILTER_CONFIG_DECODE_ENABLED)))
                                    compression.append(": H5Z_FILTER_CONFIG_ENCODE_ENABLED");
                            }
                            else {
                                filters.append("USERDEFINED ")
                                    .append(cdName[0])
                                    .append("(")
                                    .append(filter)
                                    .append("): ");
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
                        int[] at = {0};
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
    public void writeMetadata(Object info) throws Exception
    {
        try {
            objMetadata.writeMetadata(info);
        }
        catch (Exception ex) {
            log.debug("writeMetadata(): Object not an Attribute");
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
    public void removeMetadata(Object info) throws HDF5Exception
    {
        try {
            objMetadata.removeMetadata(info);
        }
        catch (Exception ex) {
            log.debug("removeMetadata(): Object not an Attribute");
            return;
        }

        Attribute attr = (Attribute)info;
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
            log.debug("removeMetadata(): failed to open scalar dataset");
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
    public void updateMetadata(Object info) throws HDF5Exception
    {
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
    public void setName(String newName) throws Exception
    {
        if (newName == null)
            throw new IllegalArgumentException("The new name is NULL");

        H5File.renameObject(this, newName);
        super.setName(newName);
    }

    /**
     * Resets selection of dataspace
     */
    @Override
    protected void resetSelection()
    {
        super.resetSelection();

        if (interlace == INTERLACE_PIXEL) {
            // 24-bit TRUE color image
            // [height][width][pixel components]
            selectedDims[2]  = 3;
            selectedDims[0]  = dims[0];
            selectedDims[1]  = dims[1];
            selectedIndex[0] = 0; // index for height
            selectedIndex[1] = 1; // index for width
            selectedIndex[2] = 2; // index for depth
        }
        else if (interlace == INTERLACE_PLANE) {
            // 24-bit TRUE color image
            // [pixel components][height][width]
            selectedDims[0]  = 3;
            selectedDims[1]  = dims[1];
            selectedDims[2]  = dims[2];
            selectedIndex[0] = 1; // index for height
            selectedIndex[1] = 2; // index for width
            selectedIndex[2] = 0; // index for depth
        }

        if ((rank > 1) && (selectedIndex[0] > selectedIndex[1]))
            isDefaultImageOrder = false;
        else
            isDefaultImageOrder = true;
    }

    /**
     * Creates a scalar dataset in a file with/without chunking and compression.
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param type
     *            the datatype of the dataset.
     * @param dims
     *            the dimension size of the dataset.
     * @param maxdims
     *            the max dimension size of the dataset. maxdims is set to dims if maxdims = null.
     * @param chunks
     *            the chunk size of the dataset. No chunking if chunk = null.
     * @param gzip
     *            GZIP compression level (1 to 9). No compression if gzip&lt;=0.
     * @param data
     *            the array of data values.
     *
     * @return the new scalar dataset if successful; otherwise returns null.
     *
     * @throws Exception if there is a failure.
     */
    public static Dataset create(String name, Group pgroup, Datatype type, long[] dims, long[] maxdims,
                                 long[] chunks, int gzip, Object data) throws Exception
    {
        return create(name, pgroup, type, dims, maxdims, chunks, gzip, null, data);
    }

    /**
     * Creates a scalar dataset in a file with/without chunking and compression.
     *
     * The following example shows how to create a string dataset using this function.
     *
     * <pre>
     * H5File file = new H5File(&quot;test.h5&quot;, H5File.CREATE);
     * int max_str_len = 120;
     * Datatype strType = new H5Datatype(Datatype.CLASS_STRING, max_str_len, Datatype.NATIVE,
     * Datatype.NATIVE); int size = 10000; long dims[] = { size }; long chunks[] = { 1000 }; int gzip = 9;
     * String strs[] = new String[size];
     *
     * for (int i = 0; i &lt; size; i++)
     *     strs[i] = String.valueOf(i);
     *
     * file.open();
     * file.createScalarDS(&quot;/1D scalar strings&quot;, null, strType, dims, null, chunks, gzip, strs);
     *
     * try {
     *     file.close();
     * }
     * catch (Exception ex) {
     * }
     * </pre>
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param type
     *            the datatype of the dataset.
     * @param dims
     *            the dimension size of the dataset.
     * @param maxdims
     *            the max dimension size of the dataset. maxdims is set to dims if maxdims = null.
     * @param chunks
     *            the chunk size of the dataset. No chunking if chunk = null.
     * @param gzip
     *            GZIP compression level (1 to 9). No compression if gzip&lt;=0.
     * @param fillValue
     *            the default data value.
     * @param data
     *            the array of data values.
     *
     * @return the new scalar dataset if successful; otherwise returns null.
     *
     * @throws Exception if there is a failure.
     */
    public static Dataset create(String name, Group pgroup, Datatype type, long[] dims, long[] maxdims,
                                 long[] chunks, int gzip, Object fillValue, Object data) throws Exception
    {
        H5ScalarDS dataset = null;
        String fullPath    = null;
        long did           = HDF5Constants.H5I_INVALID_HID;
        long plist         = HDF5Constants.H5I_INVALID_HID;
        long sid           = HDF5Constants.H5I_INVALID_HID;
        long tid           = HDF5Constants.H5I_INVALID_HID;

        if ((pgroup == null) || (name == null) || (dims == null) || ((gzip > 0) && (chunks == null))) {
            log.debug("create(): one or more parameters are null");
            return null;
        }

        H5File file = (H5File)pgroup.getFileFormat();
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

        tid = type.createNative();
        if (tid >= 0) {
            try {
                sid = H5.H5Screate_simple(rank, dims, maxdims);

                // figure out creation properties
                plist = HDF5Constants.H5P_DEFAULT;

                byte[] valFill = null;
                try {
                    valFill = parseFillValue(type, fillValue);
                }
                catch (Exception ex) {
                    log.debug("create(): parse fill value: ", ex);
                }
                log.trace("create(): parseFillValue={}", valFill);

                if (chunks != null || valFill != null) {
                    plist = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);

                    if (chunks != null) {
                        H5.H5Pset_layout(plist, HDF5Constants.H5D_CHUNKED);
                        H5.H5Pset_chunk(plist, rank, chunks);

                        // compression requires chunking
                        if (gzip > 0) {
                            H5.H5Pset_deflate(plist, gzip);
                        }
                    }

                    if (valFill != null)
                        H5.H5Pset_fill_value(plist, tid, valFill);
                }

                long fid = file.getFID();

                log.trace("create(): create dataset fid={}", fid);
                did = H5.H5Dcreate(fid, fullPath, tid, sid, HDF5Constants.H5P_DEFAULT, plist,
                                   HDF5Constants.H5P_DEFAULT);
                log.trace("create(): create dataset did={}", did);
                dataset = new H5ScalarDS(file, name, path);
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
            }
        }

        if (dataset != null) {
            pgroup.addToMemberList(dataset);
            if (data != null) {
                dataset.init();
                long[] selected = dataset.getSelectedDims();
                for (int i = 0; i < rank; i++)
                    selected[i] = dims[i];
                dataset.write(data);
            }
        }

        return dataset;
    }

    // check _FillValue, valid_min, valid_max, and valid_range
    private void checkCFconvention(long oid) throws Exception
    {
        Object avalue = getAttrValue(oid, "_FillValue");

        if (avalue != null) {
            int n = Array.getLength(avalue);
            for (int i = 0; i < n; i++)
                addFilteredImageValue((Number)Array.get(avalue, i));
        }

        if (imageDataRange == null || imageDataRange[1] <= imageDataRange[0]) {
            double x0 = 0;
            double x1 = 0;
            avalue    = getAttrValue(oid, "valid_range");
            if (avalue != null) {
                try {
                    x0                = Double.parseDouble(java.lang.reflect.Array.get(avalue, 0).toString());
                    x1                = Double.parseDouble(java.lang.reflect.Array.get(avalue, 1).toString());
                    imageDataRange    = new double[2];
                    imageDataRange[0] = x0;
                    imageDataRange[1] = x1;
                    return;
                }
                catch (Exception ex) {
                    log.debug("checkCFconvention(): valid_range: ", ex);
                }
            }

            avalue = getAttrValue(oid, "valid_min");
            if (avalue != null) {
                try {
                    x0 = Double.parseDouble(java.lang.reflect.Array.get(avalue, 0).toString());
                }
                catch (Exception ex) {
                    log.debug("checkCFconvention(): valid_min: ", ex);
                }
                avalue = getAttrValue(oid, "valid_max");
                if (avalue != null) {
                    try {
                        x1 = Double.parseDouble(java.lang.reflect.Array.get(avalue, 0).toString());
                        imageDataRange    = new double[2];
                        imageDataRange[0] = x0;
                        imageDataRange[1] = x1;
                    }
                    catch (Exception ex) {
                        log.debug("checkCFconvention(): valid_max:", ex);
                    }
                }
            }
        } // (imageDataRange==null || imageDataRange[1]<=imageDataRange[0])
    }

    private Object getAttrValue(long oid, String aname)
    {
        log.trace("getAttrValue(): start: name={}", aname);

        long aid      = HDF5Constants.H5I_INVALID_HID;
        long atid     = HDF5Constants.H5I_INVALID_HID;
        long asid     = HDF5Constants.H5I_INVALID_HID;
        Object avalue = null;

        try {
            // try to find attribute name
            if (H5.H5Aexists_by_name(oid, ".", aname, HDF5Constants.H5P_DEFAULT))
                aid =
                    H5.H5Aopen_by_name(oid, ".", aname, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        }
        catch (HDF5LibraryException ex5) {
            log.debug("getAttrValue(): Failed to find attribute {} : Expected", aname);
        }
        catch (Exception ex) {
            log.debug("getAttrValue(): try to find attribute {}:", aname, ex);
        }
        if (aid > 0) {
            try {
                atid        = H5.H5Aget_type(aid);
                long tmptid = atid;
                atid        = H5.H5Tget_native_type(tmptid);
                try {
                    H5.H5Tclose(tmptid);
                }
                catch (Exception ex) {
                    log.debug("getAttrValue(): H5Tclose(tmptid {}) failure: ", tmptid, ex);
                }

                asid         = H5.H5Aget_space(aid);
                long adims[] = null;

                int arank = H5.H5Sget_simple_extent_ndims(asid);
                if (arank > 0) {
                    adims = new long[arank];
                    H5.H5Sget_simple_extent_dims(asid, adims, null);
                }
                log.trace("getAttrValue(): adims={}", adims);

                // retrieve the attribute value
                long lsize = 1;
                if (adims != null) {
                    for (int j = 0; j < adims.length; j++) {
                        lsize *= adims[j];
                    }
                }
                log.trace("getAttrValue(): lsize={}", lsize);

                if (lsize < Integer.MIN_VALUE || lsize > Integer.MAX_VALUE)
                    throw new Exception("Invalid int size");

                H5Datatype dsDatatype = null;
                int nativeClass       = H5.H5Tget_class(atid);
                if (nativeClass == HDF5Constants.H5T_REFERENCE)
                    dsDatatype = new H5ReferenceType(getFileFormat(), lsize, atid);
                else
                    dsDatatype = new H5Datatype(getFileFormat(), atid);

                try {
                    avalue = H5Datatype.allocateArray(dsDatatype, (int)lsize);
                }
                catch (OutOfMemoryError e) {
                    log.debug("getAttrValue(): out of memory: ", e);
                    avalue = null;
                }

                if (avalue != null) {
                    log.trace("getAttrValue(): read attribute id {} of size={}", atid, lsize);
                    H5.H5Aread(aid, atid, avalue);

                    if (dsDatatype.isUnsigned()) {
                        log.trace("getAttrValue(): id {} is unsigned", atid);
                        avalue = convertFromUnsignedC(avalue, null);
                    }
                    if (dsDatatype.isRef() && (avalue instanceof byte[]))
                        ((H5ReferenceType)dsDatatype).setData((ArrayList<byte[]>)avalue);
                    else if (dsDatatype.isRef())
                        ((H5ReferenceType)dsDatatype).setData(avalue);
                }
            }
            catch (Exception ex) {
                log.debug("getAttrValue(): try to get value for attribute {}: ", aname, ex);
            }
            finally {
                try {
                    H5.H5Tclose(atid);
                }
                catch (HDF5Exception ex) {
                    log.debug("getAttrValue(): H5Tclose(atid {}) failure: ", atid, ex);
                }
                try {
                    H5.H5Sclose(asid);
                }
                catch (HDF5Exception ex) {
                    log.debug("getAttrValue(): H5Sclose(asid {}) failure: ", asid, ex);
                }
                try {
                    H5.H5Aclose(aid);
                }
                catch (HDF5Exception ex) {
                    log.debug("getAttrValue(): H5Aclose(aid {}) failure: ", aid, ex);
                }
            }
        } // (aid > 0)

        return avalue;
    }

    private boolean isStringAttributeOf(long objID, String name, String value)
    {
        boolean retValue = false;
        long aid         = HDF5Constants.H5I_INVALID_HID;
        long atid        = HDF5Constants.H5I_INVALID_HID;

        try {
            if (H5.H5Aexists_by_name(objID, ".", name, HDF5Constants.H5P_DEFAULT)) {
                aid              = H5.H5Aopen_by_name(objID, ".", name, HDF5Constants.H5P_DEFAULT,
                                                      HDF5Constants.H5P_DEFAULT);
                atid             = H5.H5Aget_type(aid);
                int size         = (int)H5.H5Tget_size(atid);
                byte[] attrValue = new byte[size];
                H5.H5Aread(aid, atid, attrValue);
                String strValue = new String(attrValue).trim();
                retValue        = strValue.equalsIgnoreCase(value);
            }
        }
        catch (Exception ex) {
            log.debug("isStringAttributeOf(): try to find out interlace mode:", ex);
        }
        finally {
            try {
                H5.H5Tclose(atid);
            }
            catch (HDF5Exception ex) {
                log.debug("isStringAttributeOf(): H5Tclose(atid {}) failure: ", atid, ex);
            }
            try {
                H5.H5Aclose(aid);
            }
            catch (HDF5Exception ex) {
                log.debug("isStringAttributeOf(): H5Aclose(aid {}) failure: ", aid, ex);
            }
        }

        return retValue;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#copy(hdf.object.Group, java.lang.String, long[], java.lang.Object)
     */
    @Override
    public Dataset copy(Group pgroup, String dstName, long[] dims, Object buff) throws Exception
    {
        // must give a location to copy
        if (pgroup == null) {
            log.debug("copy(): Parent group is null");
            return null;
        }

        Dataset dataset = null;
        long srcdid     = HDF5Constants.H5I_INVALID_HID;
        long dstdid     = HDF5Constants.H5I_INVALID_HID;
        long plist      = HDF5Constants.H5I_INVALID_HID;
        long tid        = HDF5Constants.H5I_INVALID_HID;
        long sid        = HDF5Constants.H5I_INVALID_HID;
        String dname    = null;
        String path     = null;

        if (pgroup.isRoot())
            path = HObject.SEPARATOR;
        else
            path = pgroup.getPath() + pgroup.getName() + HObject.SEPARATOR;
        dname = path + dstName;

        srcdid = open();
        if (srcdid >= 0) {
            try {
                tid   = H5.H5Dget_type(srcdid);
                sid   = H5.H5Screate_simple(dims.length, dims, null);
                plist = H5.H5Dget_create_plist(srcdid);

                long[] chunks        = new long[dims.length];
                boolean setChunkFlag = false;
                try {
                    H5.H5Pget_chunk(plist, dims.length, chunks);
                    for (int i = 0; i < dims.length; i++) {
                        if (dims[i] < chunks[i]) {
                            setChunkFlag = true;
                            if (dims[i] == 1)
                                chunks[i] = 1;
                            else
                                chunks[i] = dims[i] / 2;
                        }
                    }
                }
                catch (Exception ex) {
                    log.debug("copy(): chunk: ", ex);
                }

                if (setChunkFlag)
                    H5.H5Pset_chunk(plist, dims.length, chunks);

                try {
                    dstdid = H5.H5Dcreate(pgroup.getFID(), dname, tid, sid, HDF5Constants.H5P_DEFAULT, plist,
                                          HDF5Constants.H5P_DEFAULT);
                }
                catch (Exception e) {
                    log.debug("copy(): H5Dcreate: ", e);
                }
                finally {
                    try {
                        H5.H5Dclose(dstdid);
                    }
                    catch (Exception ex2) {
                        log.debug("copy(): H5Dclose(dstdid {}) failure: ", dstdid, ex2);
                    }
                }

                dataset = new H5ScalarDS(pgroup.getFileFormat(), dstName, path);
                if (buff != null) {
                    dataset.init();
                    dataset.write(buff);
                }

                dstdid = dataset.open();
                if (dstdid >= 0) {
                    try {
                        H5File.copyAttributes(srcdid, dstdid);
                    }
                    finally {
                        try {
                            H5.H5Dclose(dstdid);
                        }
                        catch (Exception ex) {
                            log.debug("copy(): H5Dclose(dstdid {}) failure: ", dstdid, ex);
                        }
                    }
                }
            }
            finally {
                try {
                    H5.H5Pclose(plist);
                }
                catch (Exception ex) {
                    log.debug("copy(): H5Pclose(plist {}) failure: ", plist, ex);
                }
                try {
                    H5.H5Sclose(sid);
                }
                catch (Exception ex) {
                    log.debug("copy(): H5Sclose(sid {}) failure: ", sid, ex);
                }
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("copy(): H5Tclose(tid {}) failure: ", tid, ex);
                }
                try {
                    H5.H5Dclose(srcdid);
                }
                catch (Exception ex) {
                    log.debug("copy(): H5Dclose(srcdid {}) failure: ", srcdid, ex);
                }
            }
        }

        pgroup.addToMemberList(dataset);

        if (dataset != null)
            ((ScalarDS)dataset).setIsImage(isImage);

        return dataset;
    }

    /**
     * Get the number of pallettes for this object.
     *
     * @return the number of palettes if it has any, 0 otherwise.
     */
    public int getNumberOfPalettes() { return NumberOfPalettes; }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.ScalarDS#getPalette()
     */
    @Override
    public byte[][] getPalette()
    {
        log.trace("getPalette(): NumberOfPalettes={}", NumberOfPalettes);
        if (NumberOfPalettes > 0)
            if (palette == null)
                palette = readPalette(0);

        return palette;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.ScalarDS#getPaletteName(int)
     */
    @Override
    public String getPaletteName(int idx)
    {
        int count          = readNumberOfPalettes();
        long did           = HDF5Constants.H5I_INVALID_HID;
        long palID         = HDF5Constants.H5I_INVALID_HID;
        String paletteName = null;

        if (count < 1) {
            log.debug("getPaletteName(): no palettes are attached");
            return null;
        }

        byte[][] refBuf = null;

        did = open();
        if (did >= 0) {
            try {
                refBuf = getPaletteRefs(did);
                palID  = H5.H5Ropen_object(refBuf[idx], HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                paletteName = H5.H5Iget_name(palID);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            finally {
                close(palID);
                for (int i = 0; i < count; i++)
                    H5.H5Rdestroy(refBuf[i]);
                close(did);
            }
        }

        return paletteName;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.ScalarDS#readPalette(int)
     */
    @Override
    public byte[][] readPalette(int idx)
    {
        byte[][] thePalette = null;
        int count           = readNumberOfPalettes();
        long did            = HDF5Constants.H5I_INVALID_HID;
        long palID          = HDF5Constants.H5I_INVALID_HID;
        long tid            = HDF5Constants.H5I_INVALID_HID;
        log.trace("readPalette(): palette count={}", count);

        if (count < 1) {
            log.debug("readPalette(): no palettes are attached");
            return null;
        }

        byte[] p        = null;
        byte[][] refBuf = null;

        did = open();
        if (did >= 0) {
            try {
                refBuf = getPaletteRefs(did);
                palID  = H5.H5Ropen_object(refBuf[idx], HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                log.trace("readPalette(): H5Ropen_object: {}", palID);
                tid = H5.H5Dget_type(palID);

                // support only 3*256 byte palette data
                if (H5.H5Dget_storage_size(palID) <= 768) {
                    p = new byte[3 * 256];
                    H5.H5Dread(palID, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                               HDF5Constants.H5P_DEFAULT, p);
                }
            }
            catch (HDF5Exception ex) {
                log.debug("readPalette(): failure: ", ex);
                p = null;
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("readPalette(): H5Tclose(tid {}) failure: ", tid, ex2);
                }
                close(palID);
                for (int i = 0; i < count; i++)
                    H5.H5Rdestroy(refBuf[i]);
                close(did);
            }
        }

        if (p != null) {
            thePalette = new byte[3][256];
            for (int i = 0; i < 256; i++) {
                thePalette[0][i] = p[i * 3];
                thePalette[1][i] = p[i * 3 + 1];
                thePalette[2][i] = p[i * 3 + 2];
            }
        }

        return thePalette;
    }

    private static byte[] parseFillValue(Datatype type, Object fillValue) throws Exception
    {
        byte[] data = null;

        if (type == null || fillValue == null) {
            log.debug("parseFillValue(): datatype or fill value is null");
            return null;
        }

        int datatypeClass = type.getDatatypeClass();
        int datatypeSize  = (int)type.getDatatypeSize();

        double valDbl = 0;
        String valStr = null;

        if (fillValue instanceof String)
            valStr = (String)fillValue;
        else if (fillValue.getClass().isArray())
            valStr = Array.get(fillValue, 0).toString();

        if (!type.isString()) {
            try {
                valDbl = Double.parseDouble(valStr);
            }
            catch (NumberFormatException ex) {
                log.debug("parseFillValue(): parse error: ", ex);
                return null;
            }
        }

        try {
            switch (datatypeClass) {
            case Datatype.CLASS_INTEGER:
            case Datatype.CLASS_ENUM:
            case Datatype.CLASS_CHAR:
                log.trace("parseFillValue(): class CLASS_INT-ENUM-CHAR");
                if (datatypeSize == 1)
                    data = new byte[] {(byte)valDbl};
                else if (datatypeSize == 2)
                    data = HDFNativeData.shortToByte((short)valDbl);
                else if (datatypeSize == 8)
                    data = HDFNativeData.longToByte((long)valDbl);
                else
                    data = HDFNativeData.intToByte((int)valDbl);
                break;
            case Datatype.CLASS_FLOAT:
                log.trace("parseFillValue(): class CLASS_FLOAT");
                if (datatypeSize > 8)
                    data = valStr.getBytes();
                else if (datatypeSize == 8)
                    data = HDFNativeData.doubleToByte(valDbl);
                else if (datatypeSize == 4)
                    data = HDFNativeData.floatToByte((float)valDbl);
                else
                    data = HDFNativeData.shortToByte((short)Float.floatToFloat16((float)valDbl));
                break;
            case Datatype.CLASS_STRING:
                log.trace("parseFillValue(): class CLASS_STRING");
                if (valStr != null)
                    data = valStr.getBytes();
                break;
            case Datatype.CLASS_REFERENCE:
                log.trace("parseFillValue(): class CLASS_REFERENCE");
                data = HDFNativeData.longToByte((long)valDbl);
                break;
            default:
                log.debug("parseFillValue(): datatypeClass unknown");
                break;
            } // (datatypeClass)
        }
        catch (Exception ex) {
            log.debug("parseFillValue(): failure: ", ex);
            data = null;
        }

        return data;
    }

    /**
     * reads references of palettes to count the numberOfPalettes.
     *
     * @return the number of palettes referenced.
     */
    public int readNumberOfPalettes()
    {
        log.trace("readNumberOfPalettes(): isInited={}", isInited());
        if (!isInited())
            init(); // init will be called to get refs

        return NumberOfPalettes;
    }

    /**
     * reads references of palettes to calculate the numberOfPalettes.
     */
    private int readNumberOfPalette(long did)
    {
        long aid      = HDF5Constants.H5I_INVALID_HID;
        long sid      = HDF5Constants.H5I_INVALID_HID;
        long atype    = HDF5Constants.H5I_INVALID_HID;
        int size      = 0;
        int rank      = 0;
        byte[] refbuf = null;
        log.trace("readNumberOfPalette(): did={}", did);

        try {
            if (H5.H5Aexists_by_name(did, ".", "PALETTE", HDF5Constants.H5P_DEFAULT)) {
                aid  = H5.H5Aopen_by_name(did, ".", "PALETTE", HDF5Constants.H5P_DEFAULT,
                                          HDF5Constants.H5P_DEFAULT);
                sid  = H5.H5Aget_space(aid);
                rank = H5.H5Sget_simple_extent_ndims(sid);
                size = 1;
                if (rank > 0) {
                    long[] dims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, null);
                    log.trace("readNumberOfPalette(): rank={}, dims={}", rank, dims);
                    for (int i = 0; i < rank; i++)
                        size *= (int)dims[i];
                }
                log.trace("readNumberOfPalette(): size={}", size);

                if ((size * HDF5Constants.H5R_REF_BUF_SIZE) < Integer.MIN_VALUE ||
                    (size * HDF5Constants.H5R_REF_BUF_SIZE) > Integer.MAX_VALUE)
                    throw new HDF5Exception("Invalid int size");
            }
        }
        catch (HDF5Exception ex) {
            log.debug("readNumberOfPalette(): Palette attribute search failed: Expected", ex);
            refbuf = null;
        }
        finally {
            try {
                H5.H5Tclose(atype);
            }
            catch (HDF5Exception ex2) {
                log.debug("readNumberOfPalette(): H5Tclose(atype {}) failure: ", atype, ex2);
            }
            try {
                H5.H5Sclose(sid);
            }
            catch (HDF5Exception ex2) {
                log.debug("readNumberOfPalette(): H5Sclose(sid {}) failure: ", sid, ex2);
            }
            try {
                H5.H5Aclose(aid);
            }
            catch (HDF5Exception ex2) {
                log.debug("readNumberOfPalette(): H5Aclose(aid {}) failure: ", aid, ex2);
            }
        }

        return size;
    }

    /**
     * reads references of palettes into a byte array Each reference requires eight bytes storage. Therefore,
     * the array length is 8*numberOfPalettes.
     */
    private byte[][] getPaletteRefs(long did)
    {
        long aid        = HDF5Constants.H5I_INVALID_HID;
        long sid        = HDF5Constants.H5I_INVALID_HID;
        long atype      = HDF5Constants.H5I_INVALID_HID;
        int size        = 0;
        int rank        = 0;
        byte[][] refBuf = null;
        log.trace("getPaletteRefs(): did={}", did);

        try {
            if (H5.H5Aexists_by_name(did, ".", "PALETTE", HDF5Constants.H5P_DEFAULT)) {
                aid  = H5.H5Aopen_by_name(did, ".", "PALETTE", HDF5Constants.H5P_DEFAULT,
                                          HDF5Constants.H5P_DEFAULT);
                sid  = H5.H5Aget_space(aid);
                rank = H5.H5Sget_simple_extent_ndims(sid);
                size = 1;
                if (rank > 0) {
                    long[] dims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, null);
                    log.trace("getPaletteRefs(): rank={}, dims={}", rank, dims);
                    for (int i = 0; i < rank; i++)
                        size *= (int)dims[i];
                }
                log.trace("getPaletteRefs(): size={}", size);

                if ((size * HDF5Constants.H5R_REF_BUF_SIZE) < Integer.MIN_VALUE ||
                    (size * HDF5Constants.H5R_REF_BUF_SIZE) > Integer.MAX_VALUE)
                    throw new HDF5Exception("Invalid int size");
                refBuf = new byte[size][HDF5Constants.H5R_REF_BUF_SIZE];

                H5.H5Aread(aid, HDF5Constants.H5T_STD_REF, refBuf);
            }
        }
        catch (HDF5Exception ex) {
            log.debug("getPaletteRefs(): Palette attribute search failed: Expected", ex);
            refBuf = null;
        }
        finally {
            try {
                H5.H5Sclose(sid);
            }
            catch (HDF5Exception ex2) {
                log.debug("getPaletteRefs(): H5Sclose(sid {}) failure: ", sid, ex2);
            }
            try {
                H5.H5Aclose(aid);
            }
            catch (HDF5Exception ex2) {
                log.debug("getPaletteRefs(): H5Aclose(aid {}) failure: ", aid, ex2);
            }
        }

        return refBuf;
    }

    /**
     * H5Dset_extent verifies that the dataset is at least of size size, extending it if necessary. The
     * dimensionality of size is the same as that of the dataspace of the dataset being changed.
     *
     * This function can be applied to the following datasets: 1) Any dataset with unlimited dimensions 2) A
     * dataset with fixed dimensions if the current dimension sizes are less than the maximum sizes set with
     * maxdims (see H5Screate_simple)
     *
     * @param newDims the dimension target size
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    public void extend(long[] newDims) throws HDF5Exception
    {
        long did = HDF5Constants.H5I_INVALID_HID;
        long sid = HDF5Constants.H5I_INVALID_HID;

        did = open();
        if (did >= 0) {
            try {
                H5.H5Dset_extent(did, newDims);
                H5.H5Fflush(did, HDF5Constants.H5F_SCOPE_GLOBAL);
                sid              = H5.H5Dget_space(did);
                long[] checkDims = new long[rank];
                H5.H5Sget_simple_extent_dims(sid, checkDims, null);
                log.trace("extend(): rank={}, checkDims={}", rank, checkDims);
                for (int i = 0; i < rank; i++) {
                    if (checkDims[i] != newDims[i]) {
                        log.debug("extend(): error extending dataset");
                        throw new HDF5Exception("error extending dataset " + getName());
                    }
                }
                dims = checkDims;
            }
            catch (Exception e) {
                log.debug("extend(): failure: ", e);
                throw new HDF5Exception(e.getMessage());
            }
            finally {
                if (sid > 0)
                    H5.H5Sclose(sid);

                close(did);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#isVirtual()
     */
    @Override
    public boolean isVirtual()
    {
        return isVirtual;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getVirtualFilename(int)
     */
    @Override
    public String getVirtualFilename(int index)
    {
        if (isVirtual)
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
    public int getVirtualMaps()
    {
        if (isVirtual)
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
    public String toString(String delimiter, int maxItems)
    {
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
        long lsize       = 1;
        for (int j = 0; j < dims.length; j++)
            lsize *= dims[j];

        log.trace("toString: isStdRef={} Array.getLength={}", ((H5Datatype)getDatatype()).isStdRef(),
                  Array.getLength(theData));
        if (((H5Datatype)getDatatype()).isStdRef()) {
            String cname = valClass.getName();
            char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
            log.trace("toString: isStdRef with cname={} dname={}", cname, dname);
            for (int i = 0; i < (int)lsize; i++) {
                int refIndex  = HDF5Constants.H5R_REF_BUF_SIZE * i;
                byte[] refarr = new byte[(int)HDF5Constants.H5R_REF_BUF_SIZE];
                System.arraycopy(theData, refIndex, refarr, 0, (int)HDF5Constants.H5R_REF_BUF_SIZE);
                String ref_str = ((H5ReferenceType)getDatatype()).getReferenceRegion(refarr, false);
                log.trace("toString: ref_str[{}]={}", i, ref_str);
                if (i > 0)
                    sb.append(", ");
                sb.append(ref_str);

                //                int n = ref_str.length();
                //                if (maxItems > 0) {
                //                    if (n > maxItems)
                //                        break;
                //                    else
                //                        maxItems -= n;
                //                }
            }
            return sb.toString();
        }
        return super.toString(delimiter, maxItems);
    }
}
