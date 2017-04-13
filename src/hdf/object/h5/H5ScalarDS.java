/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.object.h5;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5DataFiltersException;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * H5ScalarDS describes a multi-dimension array of HDF5 scalar or atomic data types, such as byte, int, short, long,
 * float, double and string, and operations performed on the scalar dataset.
 * <p>
 * The library predefines a modest number of datatypes. For details,
 * read <a href="http://hdfgroup.org/HDF5/doc/Datatypes.html">The Datatype Interface (H5T).</a>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5ScalarDS extends ScalarDS {
    private static final long serialVersionUID = 2887517608230611642L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5ScalarDS.class);

    /**
     * The list of attributes of this data object. Members of the list are instance of Attribute.
     */
    private List<Attribute> attributeList;

    private int nAttributes = -1;

    private H5O_info_t obj_info;

    /**
     * The byte array containing references of palettes. Each reference requires eight bytes storage. Therefore, the
     * array length is 8*numberOfPalettes.
     */
    private byte[] paletteRefs;

    /** flag to indicate if the dataset is a variable length */
    private boolean isVLEN = false;

    /** flag to indicate if the dataset is enum */
    private boolean isEnum = false;

    /** flag to indicate if the dataset is an external dataset */
    private boolean isExternal = false;

    /** flag to indicate is the dataset is a virtual dataset */
    private boolean isVirtual = false;
    private List<String> virtualNameList;

    private boolean isArrayOfCompound = false;

    private boolean isArrayOfVLEN = false;
    /**
     * flag to indicate if the datatype in file is the same as dataype in memory
     */
    private boolean isNativeDatatype = false;

    /** flag to indicate is the datatype is reg. ref. */
    private boolean isRegRef = false;

    /**
     * Constructs an instance of a H5 scalar dataset with given file, dataset name and path.
     * <p>
     * For example, in H5ScalarDS(h5file, "dset", "/arrays/"), "dset" is the name of the dataset, "/arrays" is the group
     * path of the dataset.
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     */
    public H5ScalarDS(FileFormat theFile, String theName, String thePath) {
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
    public H5ScalarDS(FileFormat theFile, String theName, String thePath, long[] oid) {
        super(theFile, theName, thePath, oid);
        unsignedConverted = false;
        paletteRefs = null;
        obj_info = new H5O_info_t(-1L, -1L, 0, 0, -1L, 0L, 0L, 0L, 0L, null, null, null);

        if ((oid == null) && (theFile != null)) {
            // retrieve the object ID
            try {
                byte[] ref_buf = H5.H5Rcreate(theFile.getFID(), this.getFullName(), HDF5Constants.H5R_OBJECT, -1);
                this.oid = new long[1];
                this.oid[0] = HDFNativeData.byteToLong(ref_buf, 0);
            }
            catch (Exception ex) {
                log.debug("constructor ID {} for {} failed H5Rcreate", theFile.getFID(), this.getFullName());
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        log.trace("open(): start");
        long did = -1;

        try {
            did = H5.H5Dopen(getFID(), getPath() + getName(), HDF5Constants.H5P_DEFAULT);
        }
        catch (HDF5Exception ex) {
            log.debug("open(): Failed to open dataset {}", getPath() + getName());
            did = -1;
        }

        log.trace("open(): finish");
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

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#init()
     */
    @Override
    public void init() {
        log.trace("init(): start");

        if (rank > 0) {
            resetSelection();
            log.trace("init(): Dataset already intialized");
            log.trace("init(): finish");
            return; // already called. Initialize only once
        }

        long did = -1;
        long tid = -1;
        long sid = -1;
        int tclass = -1;

        did = open();
        if (did >= 0) {
            // check if it is an external or virtual dataset
            long pid = -1;
            try {
                pid = H5.H5Dget_create_plist(did);
                int nfiles = H5.H5Pget_external_count(pid);
                isExternal = (nfiles > 0);
                int layout_type = H5.H5Pget_layout(pid);
                if(isVirtual = (layout_type == HDF5Constants.H5D_VIRTUAL)) {
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
                                catch (Throwable err) {
                                    log.trace("init(): vds[{}] continue", next);
                                    continue;
                                }
                            }
                        }
                    }
                    catch (Throwable err) {
                        log.debug("init(): vds count error: ", err);
                    }
                }
                log.trace("init(): pid={} nfiles={} isExternal={} isVirtual={}", pid, nfiles, isExternal, isVirtual);
            }
            catch (Exception ex) {
                log.debug("init(): check if it is an external or virtual dataset: ", ex);
            }
            finally {
                try {
                    H5.H5Pclose(pid);
                }
                catch (Exception ex) {
                    log.debug("init(): H5Pclose(pid {}) failure: ", pid, ex);
                }
            }

            paletteRefs = getPaletteRefs(did);

            try {
                sid = H5.H5Dget_space(did);
                rank = H5.H5Sget_simple_extent_ndims(sid);
                tid = H5.H5Dget_type(did);
                tclass = H5.H5Tget_class(tid);
                log.debug("init(): H5Tget_class: {} is Array {}", tclass, HDF5Constants.H5T_ARRAY);

                long tmptid = 0;
                if (tclass == HDF5Constants.H5T_ARRAY) {
                    long basetid = -1;
                    try {
                        // use the base datatype to define the array
                        basetid = H5.H5Tget_super(tid);
                        int baseclass = H5.H5Tget_class(basetid);
                        isArrayOfCompound = (baseclass == HDF5Constants.H5T_COMPOUND);
                        isArrayOfVLEN = (baseclass == HDF5Constants.H5T_VLEN);
                        isVLEN = isVLEN || ((baseclass == HDF5Constants.H5T_VLEN) || H5.H5Tis_variable_str(basetid));
                        isVLEN = isVLEN || H5.H5Tdetect_class(basetid, HDF5Constants.H5T_VLEN);
                    }
                    catch (Exception ex) {
                        log.debug("init():  use the base datatype to define the array: ", ex);
                    }
                    finally {
                        try {
                            H5.H5Pclose(basetid);
                        }
                        catch (Exception ex) {
                            log.debug("init(): H5Pclose(basetid {}) failure: ", basetid, ex);
                        }
                    }
                }

                isText = (tclass == HDF5Constants.H5T_STRING);
                isVLEN = isVLEN || ((tclass == HDF5Constants.H5T_VLEN) || H5.H5Tis_variable_str(tid));
                isEnum = (tclass == HDF5Constants.H5T_ENUM);
                isUnsigned = H5Datatype.isUnsigned(tid);
                isRegRef = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_DSETREG);
                log.trace(
                        "init(): tid={} is tclass={} has isText={} : isVLEN={} : isEnum={} : isUnsigned={} : isRegRef={}",
                        tid, tclass, isText, isVLEN, isEnum, isUnsigned, isRegRef);

                // check if datatype in file is native datatype
                try {
                    tmptid = H5.H5Tget_native_type(tid);
                    isNativeDatatype = H5.H5Tequal(tid, tmptid);
                    log.trace("init(): isNativeDatatype={}", isNativeDatatype);

                    /* see if fill value is defined */
                    pid = H5.H5Dget_create_plist(did);
                    int[] fillStatus = { 0 };
                    if (H5.H5Pfill_value_defined(pid, fillStatus) >= 0) {
                        if (fillStatus[0] == HDF5Constants.H5D_FILL_VALUE_USER_DEFINED) {
                            fillValue = H5Datatype.allocateArray(tmptid, 1);
                            log.trace("init(): fillValue={}", fillValue);
                            try {
                                H5.H5Pget_fill_value(pid, tmptid, fillValue);
                                log.trace("init(): H5Pget_fill_value={}", fillValue);
                                if (fillValue != null) {
                                    if (isFillValueConverted)
                                        fillValue = ScalarDS.convertToUnsignedC(fillValue, null);

                                    int n = Array.getLength(fillValue);
                                    for (int i = 0; i < n; i++)
                                        addFilteredImageValue((Number) Array.get(fillValue, i));
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
                    log.debug("init(): check if datatype in file is native datatype: ", ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tmptid);
                    }
                    catch (HDF5Exception ex) {
                        log.debug("init(): H5Tclose(tmptid {}) failure: ", tmptid, ex);
                    }
                    try {
                        H5.H5Pclose(pid);
                    }
                    catch (Exception ex) {
                        log.debug("init(): H5Pclose(pid {}) failure: ", pid, ex);
                    }
                }

                if (rank == 0) {
                    // a scalar data point
                    rank = 1;
                    dims = new long[1];
                    dims[0] = 1;
                    log.trace("init() rank is a scalar data point");
                }
                else {
                    dims = new long[rank];
                    maxDims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, maxDims);
                    log.trace("init() rank={}, dims={}, maxDims={}", rank, dims, maxDims);
                }
            }
            catch (HDF5Exception ex) {
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
            }

            // check for the type of image and interlace mode
            // it is a true color image at one of three cases:
            // 1) IMAGE_SUBCLASS = IMAGE_TRUECOLOR,
            // 2) INTERLACE_MODE = INTERLACE_PIXEL,
            // 3) INTERLACE_MODE = INTERLACE_PLANE
            if ((rank >= 3) && isImage) {
                interlace = -1;
                isTrueColor = isStringAttributeOf(did, "IMAGE_SUBCLASS", "IMAGE_TRUECOLOR");

                if (isTrueColor) {
                    interlace = INTERLACE_PIXEL;
                    if (isStringAttributeOf(did, "INTERLACE_MODE", "INTERLACE_PLANE")) {
                        interlace = INTERLACE_PLANE;
                    }
                }
            }

            close(did);
        }
        else {
            log.debug("init(): failed to open dataset");
        }

        startDims = new long[rank];
        selectedDims = new long[rank];
        resetSelection();
        log.trace("init(): rank={}, startDims={}, selectedDims={}", rank, startDims, selectedDims);
        log.trace("init(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#hasAttribute()
     */
    public boolean hasAttribute() {
        obj_info.num_attrs = nAttributes;

        if (obj_info.num_attrs < 0) {
            long did = open();
            if (did >= 0) {
                long tid = -1;
                obj_info.num_attrs = 0;

                try {
                    obj_info = H5.H5Oget_info(did);
                    nAttributes = (int) obj_info.num_attrs;

                    tid = H5.H5Dget_type(did);

                    int tclass = H5.H5Tget_class(tid);
                    isText = (tclass == HDF5Constants.H5T_STRING);
                    isVLEN = ((tclass == HDF5Constants.H5T_VLEN) || H5.H5Tis_variable_str(tid));
                    isEnum = (tclass == HDF5Constants.H5T_ENUM);
                    log.trace("hasAttribute(): tclass type: isText={},isVLEN={},isEnum={}", isText, isVLEN, isEnum);
                }
                catch (Exception ex) {
                    obj_info.num_attrs = 0;
                    log.debug("hasAttribute(): get object info: ", ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tid);
                    } catch (HDF5Exception ex) {
                        log.debug("hasAttribute(): H5Tclose(tid {}) failure: ", tid, ex);
                    }
                }

                if(nAttributes > 0) {
                    // test if it is an image
                    // check image
                    Object avalue = getAttrValue(did, "CLASS");
                    if (avalue != null) {
                        try {
                            isImageDisplay = isImage = "IMAGE".equalsIgnoreCase(new String((byte[]) avalue).trim());
                            log.trace("hasAttribute(): isImageDisplay dataset: {} with value = {}", isImageDisplay, avalue);
                        }
                        catch (Throwable err) {
                            log.debug("hasAttribute(): check image: ", err);
                        }
                    }

                    // retrieve the IMAGE_MINMAXRANGE
                    avalue = getAttrValue(did, "IMAGE_MINMAXRANGE");
                    if (avalue != null) {
                        double x0 = 0, x1 = 0;
                        try {
                            x0 = Double.valueOf(java.lang.reflect.Array.get(avalue, 0).toString()).doubleValue();
                            x1 = Double.valueOf(java.lang.reflect.Array.get(avalue, 1).toString()).doubleValue();
                        }
                        catch (Exception ex2) {
                            x0 = x1 = 0;
                        }
                        if (x1 > x0) {
                            imageDataRange = new double[2];
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
                close(did);
            }
            else {
                log.debug("hasAttribute(): could not open dataset");
            }
        }

        log.trace("hasAttribute(): nAttributes={}", obj_info.num_attrs);
        return (obj_info.num_attrs > 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getDatatype()
     */
    @Override
    public Datatype getDatatype() {
        log.trace("getDatatype(): start");
        if (datatype == null) {
            log.trace("getDatatype(): datatype == null");
            long did = -1;
            long tid = -1;

            did = open();
            if (did >= 0) {
                try {
                    tid = H5.H5Dget_type(did);

                    log.trace("getDatatype(): isNativeDatatype", isNativeDatatype);
                    if (!isNativeDatatype) {
                        long tmptid = -1;
                        try {
                            tmptid = tid;
                            tid = H5.H5Tget_native_type(tmptid);
                        }
                        finally {
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (Exception ex2) {
                                log.debug("getDatatype(): H5Tclose(tmptid {}) failure: ", tmptid, ex2);
                            }
                        }
                    }
                    datatype = new H5Datatype(tid);
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

        log.trace("getDatatype(): finish");
        return datatype;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#clear()
     */
    @Override
    public void clear() {
        super.clear();

        if (attributeList != null) {
            ((Vector<Attribute>) attributeList).setSize(0);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#readBytes()
     */
    @Override
    public byte[] readBytes() throws HDF5Exception {
        log.trace("readBytes(0: start");

        byte[] theData = null;

        if (rank <= 0) {
            init();
        }

        long did = open();
        if (did >= 0) {
            long fspace = -1;
            long mspace = -1;
            long tid = -1;

            try {
                long[] lsize = { 1 };
                for (int j = 0; j < selectedDims.length; j++) {
                    lsize[0] *= selectedDims[j];
                }

                fspace = H5.H5Dget_space(did);
                mspace = H5.H5Screate_simple(rank, selectedDims, null);

                // set the rectangle selection
                // HDF5 bug: for scalar dataset, H5Sselect_hyperslab gives core dump
                if (rank * dims[0] > 1) {
                    H5.H5Sselect_hyperslab(fspace, HDF5Constants.H5S_SELECT_SET, startDims, selectedStride,
                            selectedDims, null); // set block to 1
                }

                tid = H5.H5Dget_type(did);
                long size = H5.H5Tget_size(tid) * lsize[0];
                log.trace("readBytes(): size = {}", size);

                if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE) throw new Exception("Invalid int size");

                theData = new byte[(int)size];
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

        log.trace("readBytes(): finish");
        return theData;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#read()
     */
    @Override
    public Object read() throws Exception {
        log.trace("read(): start");

        Object theData = null;
        long did = -1;
        long tid = -1;
        long spaceIDs[] = { -1, -1 }; // spaceIDs[0]=mspace, spaceIDs[1]=fspace

        if (rank <= 0) {
            init(); // read data information into memory
        }

        if (isArrayOfCompound) {
            log.debug("read(): Cannot show data of type ARRAY of COMPOUND");
            log.trace("read(): finish");
            throw new HDF5Exception("Cannot show data with datatype of ARRAY of COMPOUND.");
        }
        if (isArrayOfVLEN) {
            log.debug("read(): Cannot show data of type ARRAY of VL");
            log.trace("read(): finish");
            throw new HDF5Exception("Cannot show data with datatype of ARRAY of VL.");
        }

        if (isExternal) {
            String pdir = this.getFileFormat().getAbsoluteFile().getParent();

            if (pdir == null) {
                pdir = ".";
            }
            System.setProperty("user.dir", pdir);//H5.H5Dchdir_ext(pdir);
        }

        boolean isREF = false;
        long[] lsize = { 1 };
        log.trace("read(): open dataset");
        did = open();
        if (did >= 0) {
            try {
                lsize[0] = selectHyperslab(did, spaceIDs);
                log.trace("read(): opened dataset size {} for {}", lsize[0], nPoints);

                if (lsize[0] == 0) {
                    log.debug("read(): No data to read. Dataset or selected subset is empty.");
                    throw new HDF5Exception("No data to read.\nEither the dataset or the selected subset is empty.");
                }

                if (lsize[0] < Integer.MIN_VALUE || lsize[0] > Integer.MAX_VALUE) {
                    log.debug("read(): lsize outside valid Java int range; unsafe cast");
                    throw new HDF5Exception("Dataset too large to read.");
                }

                if (log.isDebugEnabled()) {
                    // check is storage space is allocated
                    try {
                        long ssize = H5.H5Dget_storage_size(did);
                        log.trace("read(): Storage space allocated = {}.", ssize);
                    }
                    catch (Exception ex) {
                        log.debug("read(): check if storage space is allocated:", ex);
                    }
                }

                tid = H5.H5Dget_type(did);
                log.trace("read(): H5Tget_native_type:");
                log.trace("read(): isNativeDatatype={}", isNativeDatatype);
                if (!isNativeDatatype) {
                    long tmptid = -1;
                    try {
                        tmptid = tid;
                        tid = H5.H5Tget_native_type(tmptid);
                    }
                    finally {
                        try {H5.H5Tclose(tmptid);}
                        catch (Exception ex2) {log.debug("read(): H5Tclose(tmptid {}) failure: ", tmptid, ex2);}
                    }
                }

                isREF = (H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_OBJ));

                log.trace("read(): originalBuf={} isText={} isREF={} lsize[0]={} nPoints={}", originalBuf, isText, isREF, lsize[0], nPoints);
                if ((originalBuf == null) || isEnum || isText || isREF || ((originalBuf != null) && (lsize[0] != nPoints))) {
                    try {
                        theData = H5Datatype.allocateArray(tid, (int) lsize[0]);
                    }
                    catch (OutOfMemoryError err) {
                        log.debug("read(): Out of memory.");
                        log.trace("read(): finish");
                        throw new HDF5Exception("Out Of Memory.");
                    }
                }
                else {
                    theData = originalBuf; // reuse the buffer if the size is the same
                }

                if (theData != null) {
                    if (isVLEN) {
                        log.trace("read(): H5DreadVL");
                        boolean is_variable_str = false;
                        try {
                            is_variable_str = H5.H5Tis_variable_str(tid);
                        }
                        catch (Exception ex) {
                            log.debug("allocateArray(): H5Tis_variable_str(tid {}) failure: ", tid, ex);
                        }
                        if (is_variable_str)
                            H5.H5Dread_VLStrings(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, (Object[]) theData);
                        else
                            H5.H5DreadVL(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, (Object[]) theData);
                    }
                    else {
                        log.trace("read(): H5Dread did={} spaceIDs[0]={} spaceIDs[1]={}", did, spaceIDs[0], spaceIDs[1]);
                        H5.H5Dread(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, theData);
                    }
                } // if (theData != null)
            }
            catch (HDF5DataFiltersException exfltr) {
                log.debug("read(): read failure:", exfltr);
                log.trace("read(): finish");
                throw new Exception("Filter not available exception: " + exfltr.getMessage(), exfltr);
            }
            catch (HDF5Exception h5ex) {
                log.debug("read(): read failure", h5ex);
                log.trace("read(): finish");
                throw new HDF5Exception(h5ex.toString());
            }
            finally {
                try {
                    if (HDF5Constants.H5S_ALL != spaceIDs[0])
                        H5.H5Sclose(spaceIDs[0]);
                }
                catch (Exception ex2) {
                    log.debug("read(): H5Sclose(spaceIDs[0] {}) failure: ", spaceIDs[0], ex2);
                }
                try {
                    if (HDF5Constants.H5S_ALL != spaceIDs[1])
                        H5.H5Sclose(spaceIDs[1]);
                }
                catch (Exception ex2) {
                    log.debug("read(): H5Sclose(spaceIDs[1] {}) failure: ", spaceIDs[1], ex2);
                }
                try {
                    if (isText && convertByteToString && theData instanceof byte[]) {
                        log.trace("read(): H5Dread isText convertByteToString");
                        theData = byteToString((byte[]) theData, (int)H5.H5Tget_size(tid));
                    }
                    else if (isREF) {
                        log.trace("read(): H5Dread isREF byteToLong");
                        theData = HDFNativeData.byteToLong((byte[]) theData);
                    }
                }
                catch (Exception ex) {
                    log.debug("read(): convert data: ", ex);
                }
                try {H5.H5Tclose(tid);}
                catch (Exception ex2) {log.debug("read(): H5Tclose(tid {}) failure: ", tid, ex2);}

                close(did);
            }
        }

        log.trace("read(): finish");
        return theData;
    }


    /**
     * Writes the given data buffer into this dataset in a file.
     *
     * @param buf
     *            The buffer that contains the data values.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    @Override
    public void write(Object buf) throws HDF5Exception {
        log.trace("write(): start");
        long did = -1;
        long tid = -1;
        long spaceIDs[] = { -1, -1 }; // spaceIDs[0]=mspace, spaceIDs[1]=fspace
        Object tmpData = null;

        if (buf == null) {
            log.debug("write(): buf is null");
            log.trace("write(): finish");
            return;
        }

        if (isVLEN && !isText) {
            log.trace("write(): VL data={}", buf);
            log.debug("write(): Cannot write non-string variable-length data");
            log.trace("write(): finish");
            throw (new HDF5Exception("Writing non-string variable-length data is not supported"));
        }
        else if (isRegRef) {
            log.debug("write(): Cannot write region reference data");
            log.trace("write(): finish");
            throw (new HDF5Exception("Writing region references data is not supported"));
        }

        long[] lsize = { 1 };
        did = open();
        log.trace("write(): dataset opened");
        if (did >= 0) {
            try {
                lsize[0] = selectHyperslab(did, spaceIDs);
                tid = H5.H5Dget_type(did);

                log.trace("write(): isNativeDatatype={}", isNativeDatatype);
                if (!isNativeDatatype) {
                    long tmptid = -1;
                    try {
                        tmptid = tid;
                        tid = H5.H5Tget_native_type(tmptid);
                    }
                    finally {
                        try {H5.H5Tclose(tmptid);}
                        catch (Exception ex2) {log.debug("write(): H5Tclose(tmptid {}) failure: ", tmptid, ex2);}
                    }
                }

                isText = (H5.H5Tget_class(tid) == HDF5Constants.H5T_STRING);

                // check if need to convert integer data
                int tsize = (int)H5.H5Tget_size(tid);
                String cname = buf.getClass().getName();
                char dname = cname.charAt(cname.lastIndexOf("[") + 1);
                boolean doConversion = (((tsize == 1) && (dname == 'S')) || ((tsize == 2) && (dname == 'I'))
                        || ((tsize == 4) && (dname == 'J')) || (isUnsigned && unsignedConverted));
                log.trace("write(): tsize={} cname={} dname={} doConversion={}", tsize, cname, dname, doConversion);

                tmpData = buf;
                if (doConversion) {
                    tmpData = convertToUnsignedC(buf, null);
                }
                // do not convert v-len strings, regardless of conversion request
                // type
                else if (isText && convertByteToString && !H5.H5Tis_variable_str(tid)) {
                    tmpData = stringToByte((String[]) buf, (int)H5.H5Tget_size(tid));
                }
                else if (isEnum && (Array.get(buf, 0) instanceof String)) {
                    tmpData = H5Datatype.convertEnumNameToValue(tid, (String[]) buf, null);
                }

                H5.H5Dwrite(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, tmpData);
            }
            finally {
                tmpData = null;
                try {
                    if (HDF5Constants.H5S_ALL != spaceIDs[0])
                        H5.H5Sclose(spaceIDs[0]);
                }
                catch (Exception ex2) {
                    log.debug("write(): H5Sclose(spaceIDs[0] {}) failure: ", spaceIDs[0], ex2);
                }
                try {
                    if (HDF5Constants.H5S_ALL != spaceIDs[1])
                        H5.H5Sclose(spaceIDs[1]);
                }
                catch (Exception ex2) {
                    log.debug("write(): H5Sclose(spaceIDs[1] {}) failure: ", spaceIDs[1], ex2);
                }
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex2) {
                    log.debug("write(): H5Tclose(tid {}) failure: ", tid, ex2);
                }
            }
            close(did);
        }
        log.trace("write(): finish");
    }

    /**
     * Set up the selection of hyperslab
     *
     * @param did
     *            IN dataset ID
     * @param spaceIDs
     *            IN/OUT memory and file space IDs -- spaceIDs[0]=mspace, spaceIDs[1]=fspace
     *
     * @return total number of data point selected
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    private long selectHyperslab(long did, long[] spaceIDs) throws HDF5Exception {
        log.trace("selectHyperslab(): start");
        long lsize = 1;

        boolean isAllSelected = true;
        for (int i = 0; i < rank; i++) {
            lsize *= selectedDims[i];
            if (selectedDims[i] < dims[i]) {
                isAllSelected = false;
            }
        }
        log.trace("selectHyperslab(): isAllSelected={}", isAllSelected);

        if (isAllSelected) {
            spaceIDs[0] = HDF5Constants.H5S_ALL;
            spaceIDs[1] = HDF5Constants.H5S_ALL;
        }
        else {
            spaceIDs[1] = H5.H5Dget_space(did);

            // When 1D dataspace is used in chunked dataset, reading is very
            // slow.
            // It is a known problem on HDF5 library for chunked dataset.
            // mspace = H5.H5Screate_simple(1, lsize, null);
            spaceIDs[0] = H5.H5Screate_simple(rank, selectedDims, null);
            H5.H5Sselect_hyperslab(spaceIDs[1], HDF5Constants.H5S_SELECT_SET, startDims, selectedStride, selectedDims,
                    null);
        }

        if ((rank > 1) && (selectedIndex[0] > selectedIndex[1]))
            isDefaultImageOrder = false;
        else
            isDefaultImageOrder = true;

        log.trace("selectHyperslab(): isDefaultImageOrder={}", isDefaultImageOrder);
        log.trace("selectHyperslab(): finish");
        return lsize;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#getMetadata()
     */
    public List<Attribute> getMetadata() throws HDF5Exception {
        return this.getMetadata(fileFormat.getIndexType(null), fileFormat.getIndexOrder(null));
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#getMetadata(int...)
     */
    public List<Attribute> getMetadata(int... attrPropList) throws HDF5Exception {
        log.trace("getMetadata(): start");

        if (rank <= 0) {
            init();
        }

        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        if (attributeList != null) {
            log.trace("getMetadata(): attributeList != null");
            log.trace("getMetadata(): finish");
            return attributeList;
        }

        long did = -1;
        long pcid = -1;
        long paid = -1;
        int indxType = fileFormat.getIndexType(null);
        int order = fileFormat.getIndexOrder(null);

        // load attributes first
        if (attrPropList.length > 0) {
            indxType = attrPropList[0];
            if (attrPropList.length > 1) {
                order = attrPropList[1];
            }
        }
        log.trace("getMetadata(): open dataset");
        did = open();
        if (did >= 0) {
            log.trace("getMetadata(): dataset opened");
            try {
                compression = "";
                attributeList = H5File.getAttribute(did, indxType, order);
                log.trace("getMetadata(): attributeList loaded");

                // get the compression and chunk information
                pcid = H5.H5Dget_create_plist(did);
                paid = H5.H5Dget_access_plist(did);
                long storage_size = H5.H5Dget_storage_size(did);
                int nfilt = H5.H5Pget_nfilters(pcid);
                int layout_type = H5.H5Pget_layout(pcid);
                if (layout_type == HDF5Constants.H5D_CHUNKED) {
                    chunkSize = new long[rank];
                    H5.H5Pget_chunk(pcid, rank, chunkSize);
                    int n = chunkSize.length;
                    storage_layout = "CHUNKED: " + String.valueOf(chunkSize[0]);
                    for (int i = 1; i < n; i++) {
                        storage_layout += " X " + chunkSize[i];
                    }

                    if(nfilt > 0) {
                        long    nelmts = 1;
                        long    uncomp_size;
                        long    datum_size = getDatatype().getDatatypeSize();
                        if (datum_size < 0) {
                            long tmptid = -1;
                            try {
                                tmptid = H5.H5Dget_type(did);
                                datum_size = H5.H5Tget_size(tmptid);
                            }
                            finally {
                                try {H5.H5Tclose(tmptid);}
                                catch (Exception ex2) {log.debug("getMetadata(): H5Tclose(tmptid {}) failure: ", tmptid, ex2);}
                            }
                        }

                        for(int i = 0; i < rank; i++) {
                            nelmts *= dims[i];
                        }
                        uncomp_size = nelmts * datum_size;

                        /* compression ratio = uncompressed size /  compressed size */

                        if(storage_size != 0) {
                            double ratio = (double) uncomp_size / (double) storage_size;
                            DecimalFormat df = new DecimalFormat();
                            df.setMinimumFractionDigits(3);
                            df.setMaximumFractionDigits(3);
                            compression +=  df.format(ratio) + ":1";
                        }
                    }
                }
                else if (layout_type == HDF5Constants.H5D_COMPACT) {
                    storage_layout = "COMPACT";
                }
                else if (layout_type == HDF5Constants.H5D_CONTIGUOUS) {
                    storage_layout = "CONTIGUOUS";
                    if (H5.H5Pget_external_count(pcid) > 0)
                        storage_layout += " - EXTERNAL ";
                }
                else if (layout_type == HDF5Constants.H5D_VIRTUAL) {
                    storage_layout = "VIRTUAL - ";
                    try {
                        long vmaps = H5.H5Pget_virtual_count(pcid);
                        try {
                            int virt_view = H5.H5Pget_virtual_view(paid);
                            long virt_gap = H5.H5Pget_virtual_printf_gap(paid);
                            if (virt_view == HDF5Constants.H5D_VDS_FIRST_MISSING)
                                storage_layout += "First Missing";
                            else
                                storage_layout += "Last Available";
                            storage_layout += "\nGAP : " + String.valueOf(virt_gap);
                        }
                        catch (Throwable err) {
                            log.debug("getMetadata(): vds error: ", err);
                            storage_layout += "ERROR";
                        }
                        storage_layout += "\nMAPS : " + String.valueOf(vmaps);
                        if (vmaps > 0) {
                            for (long next = 0; next < vmaps; next++) {
                                try {
                                    long virtual_vspace = H5.H5Pget_virtual_vspace(pcid, next);
                                    long virtual_srcspace = H5.H5Pget_virtual_srcspace(pcid, next);
                                    String fname = H5.H5Pget_virtual_filename(pcid, next);
                                    String dsetname = H5.H5Pget_virtual_dsetname(pcid, next);
                                    storage_layout += "\n" + fname + " : " + dsetname;
                                }
                                catch (Throwable err) {
                                    log.debug("getMetadata(): vds space[{}] error: ", next, err);
                                    log.trace("getMetadata(): vds[{}] continue", next);
                                    storage_layout += "ERROR";
                                    continue;
                                }
                            }
                        }
                    }
                    catch (Throwable err) {
                        log.debug("getMetadata(): vds count error: ", err);
                        storage_layout += "ERROR";
                    }
                }
                else {
                    chunkSize = null;
                    storage_layout = "NONE";
                }

                int[] flags = { 0, 0 };
                long[] cd_nelmts = { 20 };
                int[] cd_values = new int[(int) cd_nelmts[0]];;
                String[] cd_name = { "", "" };
                log.trace("getMetadata(): {} filters in pipeline", nfilt);
                int filter = -1;
                int[] filter_config = { 1 };
                filters = "";

                for (int i = 0, k = 0; i < nfilt; i++) {
                    log.trace("getMetadata(): filter[{}]", i);
                    if (i > 0) {
                        filters += ", ";
                    }
                    if (k > 0) {
                        compression += ", ";
                    }

                    try {
                        cd_nelmts[0] = 20;
                        cd_values = new int[(int) cd_nelmts[0]];
                        cd_values = new int[(int) cd_nelmts[0]];
                        filter = H5.H5Pget_filter(pcid, i, flags, cd_nelmts, cd_values, 120, cd_name, filter_config);
                        log.trace("getMetadata(): filter[{}] is {} has {} elements ", i, cd_name[0], cd_nelmts[0]);
                        for (int j = 0; j < cd_nelmts[0]; j++) {
                            log.trace("getMetadata(): filter[{}] element {} = {}", i, j, cd_values[j]);
                        }
                    }
                    catch (Throwable err) {
                        log.debug("getMetadata(): filter[{}] error: ", i, err);
                        log.trace("getMetadata(): filter[{}] continue", i);
                        filters += "ERROR";
                        continue;
                    }

                    if (filter == HDF5Constants.H5Z_FILTER_NONE) {
                        filters += "NONE";
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_DEFLATE) {
                        filters += "GZIP";
                        compression += compression_gzip_txt + cd_values[0];
                        k++;
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_FLETCHER32) {
                        filters += "Error detection filter";
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_SHUFFLE) {
                        filters += "SHUFFLE: Nbytes = " + cd_values[0];
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_NBIT) {
                        filters += "NBIT";
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_SCALEOFFSET) {
                        filters += "SCALEOFFSET: MIN BITS = " + cd_values[0];
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_SZIP) {
                        filters += "SZIP";
                        compression += "SZIP: Pixels per block = " + cd_values[1];
                        k++;
                        int flag = -1;
                        try {
                            flag = H5.H5Zget_filter_info(filter);
                        }
                        catch (Exception ex) {
                            flag = -1;
                        }
                        if (flag == HDF5Constants.H5Z_FILTER_CONFIG_DECODE_ENABLED) {
                            compression += ": H5Z_FILTER_CONFIG_DECODE_ENABLED";
                        }
                        else if ((flag == HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED)
                                || (flag >= (HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED + HDF5Constants.H5Z_FILTER_CONFIG_DECODE_ENABLED))) {
                            compression += ": H5Z_FILTER_CONFIG_ENCODE_ENABLED";
                        }
                    }
                    else {
                        filters += "USERDEFINED " + cd_name[0] + "(" + filter + "): ";
                        for (int j = 0; j < cd_nelmts[0]; j++) {
                            if (j > 0)
                                filters += ", ";
                            filters += cd_values[j];
                        }
                        log.debug("getMetadata(): filter[{}] is user defined compression", i);
                    }
                } // for (int i=0; i<nfilt; i++)

                if (compression.length() == 0) {
                    compression = "NONE";
                }
                log.trace("getMetadata(): filter compression={}", compression);

                if (filters.length() == 0) {
                    filters = "NONE";
                }
                log.trace("getMetadata(): filter information={}", filters);

                storage = "SIZE: " + storage_size;
                try {
                    int[] at = { 0 };
                    H5.H5Pget_alloc_time(pcid, at);
                    storage += ", allocation time: ";
                    if (at[0] == HDF5Constants.H5D_ALLOC_TIME_EARLY) {
                        storage += "Early";
                    }
                    else if (at[0] == HDF5Constants.H5D_ALLOC_TIME_INCR) {
                        storage += "Incremental";
                    }
                    else if (at[0] == HDF5Constants.H5D_ALLOC_TIME_LATE) {
                        storage += "Late";
                    }
                }
                catch (Exception ex) {
                    log.debug("getMetadata(): Storage allocation time:", ex);
                }
                if (storage.length() == 0) {
                    storage = "NONE";
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

        log.trace("getMetadata(): finish");
        return attributeList;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#writeMetadata(java.lang.Object)
     */
    public void writeMetadata(Object info) throws Exception {
        log.trace("writeMetadata(): start");
        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("writeMetadata(): Object not an Attribute");
            log.trace("writeMetadata(): finish");
            return;
        }

        boolean attrExisted = false;
        Attribute attr = (Attribute) info;
        log.trace("writeMetadata(): {}", attr.getName());

        if (attributeList == null) {
            this.getMetadata();
        }

        if (attributeList != null)
            attrExisted = attributeList.contains(attr);

        getFileFormat().writeAttribute(this, attr, attrExisted);
        // add the new attribute into attribute list
        if (!attrExisted) {
            attributeList.add(attr);
            nAttributes = attributeList.size();
        }
        log.trace("writeMetadata(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#removeMetadata(java.lang.Object)
     */
    public void removeMetadata(Object info) throws HDF5Exception {
        log.trace("removeMetadata(): start");
        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("removeMetadata(): Object not an Attribute");
            log.trace("removeMetadata(): finish");
            return;
        }

        Attribute attr = (Attribute) info;
        log.trace("removeMetadata(): {}", attr.getName());
        long did = open();
        if (did >= 0) {
            try {
                H5.H5Adelete(did, attr.getName());
                List<Attribute> attrList = getMetadata();
                attrList.remove(attr);
                nAttributes = attrList.size();
            }
            finally {
                close(did);
            }
        }
        log.trace("removeMetadata(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#updateMetadata(java.lang.Object)
     */
    public void updateMetadata(Object info) throws HDF5Exception {
        log.trace("updateMetadata(): start");
        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("updateMetadata(): Object not an Attribute");
            log.trace("updateMetadata(): finish");
            return;
        }

        Attribute attr = (Attribute) info;
        log.trace("updateMetadata(): {}", attr.getName());
        nAttributes = -1;
        log.trace("updateMetadata(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#setName(java.lang.String)
     */
    @Override
    public void setName(String newName) throws Exception {
        H5File.renameObject(this, newName);
        super.setName(newName);
    }

    /**
     * Resets selection of dataspace
     */
    private void resetSelection() {
        log.trace("resetSelection(): start");

        for (int i = 0; i < rank; i++) {
            startDims[i] = 0;
            selectedDims[i] = 1;
            if (selectedStride != null) {
                selectedStride[i] = 1;
            }
        }

        if (interlace == INTERLACE_PIXEL) {
            // 24-bit TRUE color image
            // [height][width][pixel components]
            selectedDims[2] = 3;
            selectedDims[0] = dims[0];
            selectedDims[1] = dims[1];
            selectedIndex[0] = 0; // index for height
            selectedIndex[1] = 1; // index for width
            selectedIndex[2] = 2; // index for depth
        }
        else if (interlace == INTERLACE_PLANE) {
            // 24-bit TRUE color image
            // [pixel components][height][width]
            selectedDims[0] = 3;
            selectedDims[1] = dims[1];
            selectedDims[2] = dims[2];
            selectedIndex[0] = 1; // index for height
            selectedIndex[1] = 2; // index for width
            selectedIndex[2] = 0; // index for depth
        }
        else if (rank == 1) {
            selectedIndex[0] = 0;
            selectedDims[0] = dims[0];
        }
        else if (rank == 2) {
            selectedIndex[0] = 0;
            selectedIndex[1] = 1;
            selectedDims[0] = dims[0];
            selectedDims[1] = dims[1];
        }
        else if (rank > 2) {
            // // hdf-java 2.5 version: 3D dataset is arranged in the order of
            // [frame][height][width] by default
            // selectedIndex[1] = rank-1; // width, the fastest dimension
            // selectedIndex[0] = rank-2; // height
            // selectedIndex[2] = rank-3; // frames

            //
            // (5/4/09) Modified the default dimension order. See bug#1379
            // We change the default order to the following. In most situation,
            // users want to use the natural order of
            // selectedIndex[0] = 0
            // selectedIndex[1] = 1
            // selectedIndex[2] = 2
            // Most of NPOESS data is the the order above.

            if (isImage) {
                // 3D dataset is arranged in the order of [frame][height][width]
                selectedIndex[1] = rank - 1; // width, the fastest dimension
                selectedIndex[0] = rank - 2; // height
                selectedIndex[2] = rank - 3; // frames
            }
            else {
                selectedIndex[0] = 0; // width, the fastest dimension
                selectedIndex[1] = 1; // height
                selectedIndex[2] = 2; // frames
            }

            selectedDims[selectedIndex[0]] = dims[selectedIndex[0]];
            selectedDims[selectedIndex[1]] = dims[selectedIndex[1]];
            selectedDims[selectedIndex[2]] = dims[selectedIndex[2]];
        }

        // by default, only one-D is selected for text data
        if ((rank > 1) && isText) {
            selectedIndex[0] = rank - 1;
            selectedIndex[1] = 0;
            selectedDims[0] = 1;
            selectedDims[selectedIndex[0]] = dims[selectedIndex[0]];
        }

        isDataLoaded = false;
        isDefaultImageOrder = true;
        log.trace("resetSelection(): finish");
    }

    public static Dataset create(String name, Group pgroup, Datatype type, long[] dims, long[] maxdims,
            long[] chunks, int gzip, Object data) throws Exception {
        return create(name, pgroup, type, dims, maxdims, chunks, gzip, null, data);
    }

    /**
     * Creates a scalar dataset in a file with/without chunking and compression.
     * <p>
     * The following example shows how to create a string dataset using this function.
     *
     * <pre>
     * H5File file = new H5File(&quot;test.h5&quot;, H5File.CREATE);
     * int max_str_len = 120;
     * Datatype strType = new H5Datatype(Datatype.CLASS_STRING, max_str_len, -1, -1);
     * int size = 10000;
     * long dims[] = { size };
     * long chunks[] = { 1000 };
     * int gzip = 9;
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
            long[] chunks, int gzip, Object fillValue, Object data) throws Exception {
        log.trace("create(): start");

        H5ScalarDS dataset = null;
        String fullPath = null;
        long did = -1;
        long plist = -1;
        long sid = -1;
        long tid = -1;

        if ((pgroup == null) || (name == null) || (dims == null) || ((gzip > 0) && (chunks == null))) {
            log.debug("create(): one or more parameters are null");
            log.trace("create(): finish");
            return null;
        }

        H5File file = (H5File) pgroup.getFileFormat();
        if (file == null) {
            log.debug("create(): Parent Group FileFormat is null");
            log.trace("create(): finish");
            return null;
        }

        String path = HObject.separator;
        if (!pgroup.isRoot()) {
            path = pgroup.getPath() + pgroup.getName() + HObject.separator;
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            int idx = name.lastIndexOf("/");
            if (idx >= 0) {
                name = name.substring(idx + 1);
            }
        }

        fullPath = path + name;

        // setup chunking and compression
        boolean isExtentable = false;
        if (maxdims != null) {
            for (int i = 0; i < maxdims.length; i++) {
                if (maxdims[i] == 0) {
                    maxdims[i] = dims[i];
                }
                else if (maxdims[i] < 0) {
                    maxdims[i] = HDF5Constants.H5S_UNLIMITED;
                }

                if (maxdims[i] != dims[i]) {
                    isExtentable = true;
                }
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

        if ((tid = type.toNative()) >= 0) {
            try {
                sid = H5.H5Screate_simple(rank, dims, maxdims);

                // figure out creation properties
                plist = HDF5Constants.H5P_DEFAULT;

                byte[] val_fill = null;
                try {
                    val_fill = parseFillValue(type, fillValue);
                }
                catch (Exception ex) {
                    log.debug("create(): parse fill value: ", ex);
                }

                if (chunks != null || val_fill != null) {
                    plist = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);

                    if (chunks != null) {
                        H5.H5Pset_layout(plist, HDF5Constants.H5D_CHUNKED);
                        H5.H5Pset_chunk(plist, rank, chunks);

                        // compression requires chunking
                        if (gzip > 0) {
                            H5.H5Pset_deflate(plist, gzip);
                        }
                    }

                    if (val_fill != null) {
                        H5.H5Pset_fill_value(plist, tid, val_fill);
                    }
                }

                long fid = file.getFID();

                log.trace("create(): create dataset");
                did = H5.H5Dcreate(fid, fullPath, tid, sid, HDF5Constants.H5P_DEFAULT, plist, HDF5Constants.H5P_DEFAULT);
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
                long selected[] = dataset.getSelectedDims();
                for (int i = 0; i < rank; i++) {
                    selected[i] = dims[i];
                }
                dataset.write(data);
            }
        }
        log.trace("create(): finish");
        return dataset;
    }

    // check _FillValue, valid_min, valid_max, and valid_range
    private void checkCFconvention(long oid) throws Exception {
        log.trace("checkCFconvention(): start");

        Object avalue = getAttrValue(oid, "_FillValue");

        if (avalue != null) {
            int n = Array.getLength(avalue);
            for (int i = 0; i < n; i++)
                addFilteredImageValue((Number) Array.get(avalue, i));
        }

        if (imageDataRange == null || imageDataRange[1] <= imageDataRange[0]) {
            double x0 = 0, x1 = 0;
            avalue = getAttrValue(oid, "valid_range");
            if (avalue != null) {
                try {
                    x0 = Double.valueOf(java.lang.reflect.Array.get(avalue, 0).toString()).doubleValue();
                    x1 = Double.valueOf(java.lang.reflect.Array.get(avalue, 1).toString()).doubleValue();
                    imageDataRange = new double[2];
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
                    x0 = Double.valueOf(java.lang.reflect.Array.get(avalue, 0).toString()).doubleValue();
                }
                catch (Exception ex) {
                    log.debug("checkCFconvention(): valid_min: ", ex);
                }
                avalue = getAttrValue(oid, "valid_max");
                if (avalue != null) {
                    try {
                        x1 = Double.valueOf(java.lang.reflect.Array.get(avalue, 0).toString()).doubleValue();
                        imageDataRange = new double[2];
                        imageDataRange[0] = x0;
                        imageDataRange[1] = x1;
                    }
                    catch (Exception ex) {
                        log.debug("checkCFconvention(): valid_max:", ex);
                    }
                }
            }
        } // if (imageDataRange==null || imageDataRange[1]<=imageDataRange[0])
        log.trace("checkCFconvention(): finish");
    }

    private Object getAttrValue(long oid, String aname) {
        log.trace("getAttrValue(): start: name={}", aname);

        long aid = -1;
        long atid = -1;
        long asid = -1;
        Object avalue = null;

        try {
            // try to find attribute name
            aid = H5.H5Aopen_by_name(oid, ".", aname, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        }
        catch (HDF5LibraryException ex5) {
            log.debug("getAttrValue(): Failed to find attribute {} : Expected", aname);
        }
        catch (Exception ex) {
            log.debug("getAttrValue(): try to find attribute {}:", aname, ex);
        }
        if (aid > 0) {
            try {
                atid = H5.H5Aget_type(aid);
                long tmptid = atid;
                atid = H5.H5Tget_native_type(tmptid);
                try {
                    H5.H5Tclose(tmptid);
                }
                catch (Exception ex) {
                    log.debug("getAttrValue(): H5Tclose(tmptid {}) failure: ", tmptid, ex);
                }

                asid = H5.H5Aget_space(aid);
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

                if (lsize < Integer.MIN_VALUE || lsize > Integer.MAX_VALUE) throw new Exception("Invalid int size");

                avalue = H5Datatype.allocateArray(atid, (int) lsize);

                if (avalue != null) {
                    log.trace("getAttrValue(): read attribute id {} of size={}", atid, lsize);
                    H5.H5Aread(aid, atid, avalue);

                    if (H5Datatype.isUnsigned(atid)) {
                        log.trace("getAttrValue(): id {} is unsigned", atid);
                        avalue = convertFromUnsignedC(avalue, null);
                    }
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
        } // if (aid > 0)

        log.trace("getAttrValue(): finish");
        return avalue;
    }

    private boolean isStringAttributeOf(long objID, String name, String value) {
        boolean retValue = false;
        long aid = -1;
        long atid = -1;

        try {
            // try to find out interlace mode
            aid = H5.H5Aopen_by_name(objID, ".", name, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            atid = H5.H5Aget_type(aid);
            int size = (int)H5.H5Tget_size(atid);
            byte[] attrValue = new byte[size];
            H5.H5Aread(aid, atid, attrValue);
            String strValue = new String(attrValue).trim();
            retValue = strValue.equalsIgnoreCase(value);
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
    public Dataset copy(Group pgroup, String dstName, long[] dims, Object buff) throws Exception {
        log.trace("copy(): start");
        // must give a location to copy
        if (pgroup == null) {
            log.debug("copy(): Parent group is null");
            log.trace("copy(): finish");
            return null;
        }

        Dataset dataset = null;
        long srcdid = -1;
        long dstdid = -1;
        long plist = -1;
        long tid = -1;
        long sid = -1;
        String dname = null, path = null;

        if (pgroup.isRoot()) {
            path = HObject.separator;
        }
        else {
            path = pgroup.getPath() + pgroup.getName() + HObject.separator;
        }
        dname = path + dstName;

        srcdid = open();
        if (srcdid >= 0) {
            try {
                tid = H5.H5Dget_type(srcdid);
                sid = H5.H5Screate_simple(dims.length, dims, null);
                plist = H5.H5Dget_create_plist(srcdid);

                long[] chunks = new long[dims.length];
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

        ((ScalarDS) dataset).setIsImage(isImage);

        log.trace("copy(): finish");
        return dataset;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.ScalarDS#getPalette()
     */
    @Override
    public byte[][] getPalette() {
        if (palette == null) {
            palette = readPalette(0);
        }

        return palette;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.ScalarDS#getPaletteName(int)
     */
    public String getPaletteName(int idx) {
        log.trace("getPaletteName(): start");

        byte[] refs = getPaletteRefs();
        long did = -1;
        long pal_id = -1;
        String paletteName = null;

        if (refs == null) {
            log.debug("getPaletteName(): refs is null");
            log.trace("getPaletteName(): finish");
            return null;
        }

        byte[] ref_buf = new byte[8];

        try {
            System.arraycopy(refs, idx * 8, ref_buf, 0, 8);
        }
        catch (Throwable err) {
            log.debug("getPaletteName(): arraycopy failure: ", err);
            log.trace("getPaletteName(): finish");
            return null;
        }

        did = open();
        if (did >= 0) {
            try {
                pal_id = H5.H5Rdereference(getFID(), HDF5Constants.H5P_DEFAULT, HDF5Constants.H5R_OBJECT, ref_buf);
                paletteName = H5.H5Iget_name(pal_id);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            finally {
                close(pal_id);
                close(did);
            }
        }

        log.trace("getPaletteName(): finish");
        return paletteName;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.ScalarDS#readPalette(int)
     */
    @Override
    public byte[][] readPalette(int idx) {
        log.trace("readPalette(): start");

        byte[][] thePalette = null;
        byte[] refs = getPaletteRefs();
        long did = -1;
        long pal_id = -1;
        long tid = -1;

        if (refs == null) {
            log.debug("readPalette(): refs is null");
            log.trace("readPalette(): finish");
            return null;
        }

        byte[] p = null;
        byte[] ref_buf = new byte[8];

        try {
            System.arraycopy(refs, idx * 8, ref_buf, 0, 8);
        }
        catch (Throwable err) {
            log.debug("readPalette(): arraycopy failure: ", err);
            log.trace("readPalette(): failure");
            return null;
        }

        did = open();
        if (did >= 0) {
            try {
                pal_id = H5.H5Rdereference(getFID(), HDF5Constants.H5P_DEFAULT, HDF5Constants.H5R_OBJECT, ref_buf);
                tid = H5.H5Dget_type(pal_id);

                // support only 3*256 byte palette data
                if (H5.H5Dget_storage_size(pal_id) <= 768) {
                    p = new byte[3 * 256];
                    H5.H5Dread(pal_id, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, p);
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
                close(pal_id);
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

        log.trace("readPalette(): finish");
        return thePalette;
    }

    private static byte[] parseFillValue(Datatype type, Object fillValue) throws Exception {
        log.trace("parseFillValue(): start");

        byte[] data = null;

        if (type == null || fillValue == null) {
            log.debug("parseFillValue(): datatype or fill value is null");
            log.trace("parseFillValue(): finish");
            return null;
        }

        int datatypeClass = type.getDatatypeClass();
        int datatypeSize = (int)type.getDatatypeSize();

        double val_dbl = 0;
        String val_str = null;

        if (fillValue instanceof String) {
            val_str = (String) fillValue;
        }
        else if (fillValue.getClass().isArray()) {
            val_str = Array.get(fillValue, 0).toString();
        }

        if (datatypeClass != Datatype.CLASS_STRING) {
            try {
                val_dbl = Double.parseDouble(val_str);
            }
            catch (NumberFormatException ex) {
                log.debug("parseFillValue(): parse error: ", ex);
                log.trace("parseFillValue(): finish");
                return null;
            }
        }

        try {
            switch (datatypeClass) {
            case Datatype.CLASS_INTEGER:
            case Datatype.CLASS_ENUM:
            case Datatype.CLASS_CHAR:
                log.trace("parseFillValue(): class CLASS_INT-ENUM-CHAR");
                if (datatypeSize == 1) {
                    data = new byte[] { (byte) val_dbl };
                }
                else if (datatypeSize == 2) {
                    data = HDFNativeData.shortToByte((short) val_dbl);
                }
                else if (datatypeSize == 8) {
                    data = HDFNativeData.longToByte((long) val_dbl);
                }
                else {
                    data = HDFNativeData.intToByte((int) val_dbl);
                }
                break;
            case Datatype.CLASS_FLOAT:
                log.trace("parseFillValue(): class CLASS_FLOAT");
                if (datatypeSize == 8) {
                    data = HDFNativeData.doubleToByte(val_dbl);
                }
                else {
                    data = HDFNativeData.floatToByte((float) val_dbl);
                    ;
                }
                break;
            case Datatype.CLASS_STRING:
                log.trace("parseFillValue(): class CLASS_STRING");
                data = val_str.getBytes();
                break;
            case Datatype.CLASS_REFERENCE:
                log.trace("parseFillValue(): class CLASS_REFERENCE");
                data = HDFNativeData.longToByte((long) val_dbl);
                break;
            default:
                log.debug("parseFillValue(): datatypeClass unknown");
                break;
            } // switch (tclass)
        }
        catch (Exception ex) {
            log.debug("parseFillValue(): failure: ", ex);
            data = null;
        }

        log.trace("parseFillValue(): finish");
        return data;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.ScalarDS#getPaletteRefs()
     */
    @Override
    public byte[] getPaletteRefs() {
        if (rank <= 0) {
            init(); // init will be called to get refs
        }

        return paletteRefs;
    }

    /**
     * reads references of palettes into a byte array Each reference requires eight bytes storage. Therefore, the array
     * length is 8*numberOfPalettes.
     */
    private byte[] getPaletteRefs(long did) {
        log.trace("getPaletteRefs(): start");

        long aid = -1;
        long sid = -1;
        long atype = -1;
        int size = 0, rank = 0;
        byte[] ref_buf = null;

        try {
            aid = H5.H5Aopen_by_name(did, ".", "PALETTE", HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            sid = H5.H5Aget_space(aid);
            rank = H5.H5Sget_simple_extent_ndims(sid);
            size = 1;
            if (rank > 0) {
                long[] dims = new long[rank];
                H5.H5Sget_simple_extent_dims(sid, dims, null);
                log.trace("getPaletteRefs(): rank={}, dims={}", rank, dims);
                for (int i = 0; i < rank; i++) {
                    size *= (int) dims[i];
                }
            }

            if ((size * 8) < Integer.MIN_VALUE || (size * 8) > Integer.MAX_VALUE) throw new HDF5Exception("Invalid int size");

            ref_buf = new byte[size * 8];
            atype = H5.H5Aget_type(aid);

            H5.H5Aread(aid, atype, ref_buf);
        }
        catch (HDF5Exception ex) {
            log.debug("getPaletteRefs(): Palette attribute search failed: Expected", ex);
            ref_buf = null;
        }
        finally {
            try {
                H5.H5Tclose(atype);
            }
            catch (HDF5Exception ex2) {
                log.debug("getPaletteRefs(): H5Tclose(atype {}) failure: ", atype, ex2);
            }
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

        log.trace("getPaletteRefs(): finish");
        return ref_buf;
    }

    /**
     * H5Dset_extent verifies that the dataset is at least of size size, extending it if necessary. The dimensionality
     * of size is the same as that of the dataspace of the dataset being changed.
     *
     * This function can be applied to the following datasets: 1) Any dataset with unlimited dimensions 2) A dataset
     * with fixed dimensions if the current dimension sizes are less than the maximum sizes set with maxdims (see
     * H5Screate_simple)
     *
     * @param newDims the dimension target size
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    public void extend(long[] newDims) throws HDF5Exception {
        long did = -1;
        long sid = -1;

        did = open();
        if (did >= 0) {
            try {
                H5.H5Dset_extent(did, newDims);
                H5.H5Fflush(did, HDF5Constants.H5F_SCOPE_GLOBAL);
                sid = H5.H5Dget_space(did);
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
}
