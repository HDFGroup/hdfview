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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import hdf.object.Datatype;
import hdf.object.FileFormat;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.structs.H5O_info_t;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines HDF5 reference characteristics and APIs for a data type of H5T_STD_REF.
 *
 * This class provides convenient functions to access H5T_STD_REF type information.
 */
public class H5ReferenceType extends H5Datatype {
    private static final long serialVersionUID = -3360885430038261178L;

    private static final Logger log = LoggerFactory.getLogger(H5ReferenceType.class);

    /**
     * The memory buffer that holds the raw data array of the reference.
     */
    protected transient ArrayList<H5ReferenceData> refdata;

    /** Flag to indicate if data values are loaded into memory. */
    protected boolean isDataLoaded = false;

    /** Flag to indicate if this dataset has been initialized */
    protected boolean inited = false;

    /** The current array size of the reference. */
    protected long refsize;

    /**
     * The data buffer that contains the raw data directly reading from file
     * (before any data conversion).
     */
    protected transient Object originalRefBuf = null;

    /**
     * Constructs an named HDF5 data type reference for a given file, dataset name and group path.
     *
     * The datatype object represents an existing named datatype in file. For example,
     *
     * <pre>
     * new H5ReferenceType(file, "dset1", "/g0")
     * </pre>
     *
     * constructs a datatype object that corresponds to the dataset,"dset1", at group "/g0".
     *
     * @param theFile
     *            the file that contains the datatype.
     * @param theName
     *            the name of the dataset such as "dset1".
     * @param thePath
     *            the group path to the dataset such as "/g0/".
     */
    public H5ReferenceType(FileFormat theFile, String theName, String thePath)
    {
        this(theFile, theName, thePath, null);
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using {@link #H5ReferenceType(FileFormat, String, String)}
     *
     * @param theFile
     *            the file that contains the datatype.
     * @param theName
     *            the name of the dataset such as "dset1".
     * @param thePath
     *            the group path to the dataset such as "/g0/".
     * @param oid
     *            the oid of the dataset.
     */
    @Deprecated
    public H5ReferenceType(FileFormat theFile, String theName, String thePath, long[] oid)
    {
        super(theFile, theName, thePath, oid);

        log.trace("constructor theName {}", theName);
        refdata = null;
    }

    /**
     * Constructs a H5ReferenceType with specified class, size, byte order and sign.
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size must be multiples H5T_STD_REF.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX,
     *            ORDER_NONE and NATIVE.
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     *
     * @throws Exception
     *            if there is an error
     */
    public H5ReferenceType(int tclass, int tsize, int torder, int tsign) throws Exception
    {
        this(tclass, tsize, torder, tsign, null);
    }

    /**
     * Constructs a H5ReferenceType with specified class, size, byte order and sign.
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size must be multiples H5T_STD_REF.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX,
     *            ORDER_NONE and NATIVE.
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     * @param tbase
     *            the base datatype of the new datatype
     *
     * @throws Exception
     *            if there is an error
     */
    public H5ReferenceType(int tclass, int tsize, int torder, int tsign, Datatype tbase) throws Exception
    {
        this(tclass, tsize, torder, tsign, tbase, null);
    }

    /**
     * Constructs a H5ReferenceType with specified class, size, byte order and sign.
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size must be multiples H5T_STD_REF.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE,
     *            ORDER_BE, ORDER_VAX, ORDER_NONE and NATIVE.
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and
     *            NATIVE.
     * @param tbase
     *            the base datatype of the new datatype
     * @param pbase
     *            the parent datatype of the new datatype
     *
     * @throws Exception
     *            if there is an error
     */
    public H5ReferenceType(int tclass, int tsize, int torder, int tsign, Datatype tbase, Datatype pbase)
        throws Exception
    {
        super(tclass, tsize, torder, tsign, tbase, pbase);

        log.trace("constructor tsize {}", tsize);
        refdata = null;
    }

    /**
     * Constructs a H5ReferenceType with a given native datatype identifier.
     *
     * @see #fromNative(long nativeID)
     *
     * @param theFile
     *            the file that contains the datatype.
     * @param theSize
     *            the size must be multiples H5T_STD_REF.
     * @param nativeID
     *            the native datatype identifier.
     *
     * @throws Exception
     *            if there is an error
     */
    public H5ReferenceType(FileFormat theFile, long theSize, long nativeID) throws Exception
    {
        this(theFile, theSize, nativeID, null);
    }

    /**
     * Constructs a H5ReferenceType with a given native datatype identifier.
     *
     * @see #fromNative(long nativeID)
     *
     * @param theFile
     *            the file that contains the datatype.
     * @param theSize
     *            the size is the number of H5ReferenceData data structs.
     * @param nativeID
     *            the native datatype identifier.
     * @param pbase
     *            the parent datatype of the new datatype
     *
     * @throws Exception
     *            if there is an error
     */
    public H5ReferenceType(FileFormat theFile, long theSize, long nativeID, Datatype pbase) throws Exception
    {
        super(theFile, nativeID, pbase);

        log.trace("constructor theSize {}", theSize);
        refsize = theSize;
        refdata = new ArrayList<>((int)theSize);
    }

    /**
     * Clears memory held by the reference, such as the data buffer.
     */
    @SuppressWarnings("rawtypes")
    public void clear()
    {
        if (refdata != null) {
            if (refdata instanceof List)
                ((List)refdata).clear();
            originalRefBuf = null;
        }
        isDataLoaded = false;
    }

    /**
     * Writes the memory buffer of this reference to file.
     *
     * @throws Exception if buffer can not be written
     */
    public final void write() throws Exception
    {
        log.trace("H5ReferenceType: write enter");
        if (refdata != null) {
            log.trace("H5ReferenceType: write data");
            // write(refdata);
        }
    }

    /**
     * The status of initialization for this object
     *
     * @return true if the data has been initialized
     */
    public final boolean isInited() { return inited; }

    /**
     * setData() loads the reference raw data into the buffer. This
     * buffer will be accessed to get the reference strings and data.
     * Once the references are destroyed, the refdata can only be used
     * to retrieve existing data.
     *
     * @param theData
     *            the data to write.
     */
    public void setData(List theData)
    {
        log.trace("setData(List): refsize={} theData={}", refsize, theData);
        for (int i = 0; i < (int)refsize; i++) {
            H5ReferenceData rf = (H5ReferenceData)theData.get(i);
            refdata.add(rf);
        }
        isDataLoaded = true;
        init();
    }

    /**
     * setData() loads the reference raw data into the buffer. This
     * buffer will be accessed to get the reference strings and data.
     * Once the references are destroyed, the refdata can only be used
     * to retrieve existing data.
     *
     * @param theData
     *            the data to write.
     */
    public void setData(Object theData)
    {
        log.trace("setData(): refsize={} theData={}", refsize, theData);
        originalRefBuf = theData;
        for (int i = 0; i < (int)refsize; i++) {
            byte[] refarr    = new byte[(int)datatypeSize];
            byte[] rElements = null;
            if (theData instanceof ArrayList) {
                rElements = (byte[])((ArrayList)theData).get(i);
                System.arraycopy(rElements, 0, refarr, 0, (int)datatypeSize);
            }
            else {
                rElements    = (byte[])theData;
                int refIndex = (int)datatypeSize * i;
                System.arraycopy(rElements, refIndex, refarr, 0, (int)datatypeSize);
            }
            log.trace("setData(): refarr={}", refarr);
            H5ReferenceData rf = new H5ReferenceData(refarr, datatypeSize);
            refdata.add(rf);
        }
        isDataLoaded = true;
        init();
    }

    /**
     * Returns the data buffer of the reference in memory.
     *
     * If data is already loaded into memory, returns the data; otherwise, calls
     * read() to read data from file into a memory buffer and returns the memory
     * buffer.
     *
     * By default, the whole reference is read into memory.
     *
     * @return the memory buffer of the reference.
     *
     * @throws Exception if object can not be read
     * @throws OutOfMemoryError if memory is exhausted
     */
    public Object getData() throws Exception, OutOfMemoryError
    {
        log.trace("getData(): isDataLoaded={}", isDataLoaded);
        if (!isDataLoaded) {
            // refdata = read(); // load the data
            log.trace("getData(): size={} refdata={}", refdata.size(), refdata);
            if (refdata != null) {
                refsize        = refdata.size();
                originalRefBuf = refdata;
                isDataLoaded   = true;
            }
        }

        return refdata;
    }

    /**
     * Clears the current data buffer in memory and forces the next read() to load
     * the data from file.
     *
     * The function read() loads data from file into memory only if the data is
     * not read. If data is already in memory, read() just returns the memory
     * buffer. Sometimes we want to force read() to re-read data from file. For
     * example, when the selection is changed, we need to re-read the data.
     *
     * @see #getData()
     */
    public void clearData() { isDataLoaded = false; }

    /**
     * Returns the array size of the reference.
     *
     * @return the array size of the reference.
     */
    public final long getRefSize()
    {
        if (!inited)
            init();

        return refsize;
    }

    /**
     * Sets the array size of the reference.
     *
     * @param current_size
     *        the array size of the current reference.
     */
    public final void setRefSize(long current_size) { refsize = current_size; }
    //    public byte[] getOriginalrData() {
    //        if (isDataLoaded)
    //            return originalRefBuf;
    //    }

    /**
     * Retrieves reference information from file into memory.
     */
    public void init()
    {
        if (inited) {
            log.trace("init(): H5ReferenceType already inited");
            return;
        }

        log.trace("init(): refsize={}", refsize);
        for (int i = 0; i < (int)refsize; i++) {
            H5ReferenceData rf = refdata.get(i);
            log.trace("init(): rf.ref_array={}", rf.ref_array);
            byte[] refarr = new byte[(int)datatypeSize];
            System.arraycopy(rf.ref_array, 0, refarr, 0, (int)datatypeSize);

            if (zeroArrayCheck(refarr)) {
                log.trace("init(): refarr is zero");
                rf.file_fullpath = "NULL";
                rf.file_name     = "NULL";
                rf.obj_name      = "NULL";
                rf.attr_name     = "NULL";
                rf.region_type   = "NULL";
                rf.region_desc   = "NULL";
            }
            else {
                log.trace("init(): refarr={}", refarr);
                try {
                    rf.file_fullpath = "NULL";
                    rf.file_name     = "NULL";
                    rf.obj_name      = "NULL";
                    rf.attr_name     = "NULL";
                    if (isStdRef()) {
                        try {
                            rf.file_fullpath = H5.H5Rget_file_name(refarr);
                            log.trace("Reference Full File Path {}", rf.file_fullpath);
                            String[] split = rf.file_fullpath.split(Pattern.quote("/"));
                            rf.file_name   = split[split.length - 1];
                            log.trace("Reference File Name {}", rf.file_name);
                            rf.obj_name = H5.H5Rget_obj_name(refarr, HDF5Constants.H5P_DEFAULT);
                            log.trace("Reference Object Name {}", rf.obj_name);

                            if (H5.H5Rget_type(refarr) == HDF5Constants.H5R_ATTR)
                                rf.attr_name = H5.H5Rget_attr_name(refarr);
                            else
                                rf.attr_name = "NULL";
                            log.trace("Reference Attribute Name {}", rf.attr_name);
                        }
                        catch (Exception ex) {
                            log.debug("Reference H5Rget_*_name", ex);
                        }
                    }
                    else if (isRegRef()) {
                        try {
                            rf.obj_name =
                                H5.H5Rget_name_string(getFID(), HDF5Constants.H5R_DATASET_REGION, refarr);
                        }
                        catch (Exception ex) {
                            log.debug("Reference H5Rget_*_name", ex);
                        }
                    }
                    else {
                        try {
                            rf.obj_name = H5.H5Rget_name_string(getFID(), HDF5Constants.H5R_OBJECT, refarr);
                        }
                        catch (Exception ex) {
                            log.debug("Reference H5Rget_*_name", ex);
                        }
                    }
                    initReferenceRegion(i, refarr, false);
                }
                catch (Exception ex) {
                    log.debug("Reference Init", ex);
                }
            }
        }
        if (isStdRef()) {
            for (int i = 0; i < (int)refsize; i++) {
                H5ReferenceData rf = refdata.get(i);
                log.trace("init(): H5Rdestroy {}", rf.ref_array);
                byte[] refarr = new byte[(int)datatypeSize];
                System.arraycopy(rf.ref_array, 0, refarr, 0, (int)datatypeSize);
                H5.H5Rdestroy(refarr);
            }
        }
        log.trace("init(): finished");
        inited = true;
    }

    private void initReferenceRegion(int refndx, byte[] refarr, boolean showData)
    {
        H5ReferenceData rf = refdata.get(refndx);
        rf.ref_type        = HDF5Constants.H5R_BADTYPE;
        rf.obj_type        = HDF5Constants.H5O_TYPE_UNKNOWN;
        rf.region_type     = "NULL";
        rf.region_desc     = "NULL";
        log.trace("initReferenceRegion start not null");
        if (isStdRef()) {
            try {
                rf.ref_type = (int)H5.H5Rget_type(refarr);
                log.debug("initReferenceRegion ref_type={}", rf.ref_type);
                try {
                    rf.obj_type = H5.H5Rget_obj_type3(refarr, HDF5Constants.H5P_DEFAULT);
                    log.debug("initReferenceRegion obj_type={}", rf.obj_type);
                }
                catch (Exception ex2) {
                    log.debug("initReferenceRegion H5Rget_obj_type3", ex2);
                }
            }
            catch (Exception ex1) {
                log.debug("initReferenceRegion H5Rget_type", ex1);
            }

            if (rf.ref_type > HDF5Constants.H5R_BADTYPE) {
                if (rf.ref_type == HDF5Constants.H5R_OBJECT1) {
                    log.trace("initReferenceRegion H5R_OBJECT1");
                    if (rf.obj_type == HDF5Constants.H5O_TYPE_DATASET) {
                        initRegionDataset(refndx, refarr);
                    } // obj_type == HDF5Constants.H5O_TYPE_DATASET
                    else {
                        /* Object references -- show the type and OID of the referenced object. */
                        rf.region_type = "H5O_TYPE_OBJ_REF";
                        H5O_info_t objInfo;
                        long new_obj_id = HDF5Constants.H5I_INVALID_HID;
                        try {
                            new_obj_id = H5.H5Rdereference(getFID(), HDF5Constants.H5P_DEFAULT,
                                                           HDF5Constants.H5R_OBJECT, refarr);
                            objInfo    = H5.H5Oget_info(new_obj_id);
                            if (objInfo.type == HDF5Constants.H5O_TYPE_GROUP)
                                rf.region_desc = "GROUP";
                            else if (objInfo.type == HDF5Constants.H5O_TYPE_DATASET)
                                rf.region_desc = "DATASET";
                            else if (objInfo.type == HDF5Constants.H5O_TYPE_NAMED_DATATYPE)
                                rf.region_desc = "DATATYPE";
                            else
                                rf.region_desc = "UNKNOWN " + objInfo.type;
                        }
                        catch (Exception ex2) {
                            log.debug("typeObjectRef ", ex2);
                        }
                        finally {
                            H5.H5Dclose(new_obj_id);
                        }
                    }
                }
                else if (rf.ref_type == HDF5Constants.H5R_DATASET_REGION1) {
                    log.trace("initReferenceRegion H5R_DATASET_REGION1");
                    initRegionDataset(refndx, refarr);
                }
                else if (rf.ref_type == HDF5Constants.H5R_OBJECT2) {
                    log.trace("initReferenceRegion H5R_OBJECT2");
                    rf.region_type = "H5O_TYPE_OBJ_REF";
                }
                else if (rf.ref_type == HDF5Constants.H5R_DATASET_REGION2) {
                    log.trace("initReferenceRegion H5R_DATASET_REGION2");
                    initRegionDataset(refndx, refarr);
                }
                else if (rf.ref_type == HDF5Constants.H5R_ATTR) {
                    log.trace("initReferenceRegion H5R_ATTR");
                    rf.region_type = "H5R_ATTR";
                    initRegionAttribute(refndx, refarr);
                }
                else {
                    log.trace("initReferenceRegion OTHER");
                    rf.region_type = "UNKNOWN";
                }
            }
        }
        else {
            if (isRegRef()) {
                rf.ref_type     = HDF5Constants.H5R_DATASET_REGION1;
                rf.obj_type     = HDF5Constants.H5O_TYPE_DATASET;
                int region_type = typeObjectRef(getFID(), HDF5Constants.H5R_DATASET_REGION, refarr);
                if (HDF5Constants.H5S_SEL_POINTS == region_type)
                    rf.region_type = "REGION_TYPE POINT";
                else if (HDF5Constants.H5S_SEL_HYPERSLABS == region_type)
                    rf.region_type = "REGION_TYPE BLOCK";
                else
                    rf.region_type = "REGION_TYPE UNKNOWN";
                rf.region_desc = descRegionDataset(getFID(), refarr);
            }
            else {
                rf.ref_type    = HDF5Constants.H5R_OBJECT1;
                rf.obj_type    = typeObjectRef(getFID(), HDF5Constants.H5R_OBJECT, refarr);
                rf.region_type = "H5O_TYPE_OBJ_REF";
            }
        }
        log.trace("initReferenceRegion finish");
    }

    private void initRegionAttribute(int refndx, byte[] refarr)
    {
        H5ReferenceData rf = refdata.get(refndx);
        long new_obj_id    = HDF5Constants.H5I_INVALID_HID;
        try {
            log.trace("initRegionAttribute refarr2={}:", refarr);
            new_obj_id       = H5.H5Ropen_attr(refarr, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            long new_obj_sid = HDF5Constants.H5I_INVALID_HID;
            try {
                new_obj_sid    = H5.H5Aget_space(new_obj_id);
                long reg_ndims = H5.H5Sget_simple_extent_ndims(new_obj_sid);
                // rf.region_desc = dump_region_attrs(regStr, new_obj_id);
            }
            catch (Exception ex3) {
                log.debug("initRegionAttribute Space Open", ex3);
            }
            finally {
                H5.H5Sclose(new_obj_sid);
            }
            log.trace("initRegionAttribute finish");
        }
        catch (Exception ex2) {
            log.debug("initRegionAttribute ", ex2);
        }
        finally {
            H5.H5Aclose(new_obj_id);
        }
    }

    private void initRegionDataset(int refndx, byte[] refarr)
    {
        H5ReferenceData rf = refdata.get(refndx);
        long new_obj_id    = HDF5Constants.H5I_INVALID_HID;
        try {
            log.trace("initRegionDataset refarr2={}:", refarr);
            new_obj_id = H5.H5Ropen_object(refarr, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            long new_obj_sid = HDF5Constants.H5I_INVALID_HID;
            try {
                log.trace("initRegionDataset refarr3={}:", refarr);
                new_obj_sid = H5.H5Ropen_region(refarr, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                try {
                    int region_type = H5.H5Sget_select_type(new_obj_sid);
                    log.debug("Reference Region Type {}", region_type);
                    long reg_ndims   = H5.H5Sget_simple_extent_ndims(new_obj_sid);
                    StringBuilder sb = new StringBuilder();
                    if (HDF5Constants.H5S_SEL_POINTS == region_type) {
                        rf.region_type   = "REGION_TYPE POINT";
                        long reg_npoints = H5.H5Sget_select_elem_npoints(new_obj_sid);
                        long getcoord[]  = new long[(int)(reg_ndims * reg_npoints)];
                        try {
                            H5.H5Sget_select_elem_pointlist(new_obj_sid, 0, reg_npoints, getcoord);
                        }
                        catch (Exception ex5) {
                            log.debug("initRegionDataset H5.H5Sget_select_elem_pointlist: ", ex5);
                        }
                        sb.append("{ ");
                        for (int i = 0; i < (int)reg_npoints; i++) {
                            if (i > 0)
                                sb.append(" ");
                            sb.append("(");
                            for (int j = 0; j < (int)reg_ndims; j++) {
                                if (j > 0)
                                    sb.append(",");
                                sb.append(getcoord[i * (int)reg_ndims + j]);
                            }
                            sb.append(")");
                        }
                        sb.append(" }");
                        rf.region_desc = sb.toString();
                    }
                    else if (HDF5Constants.H5S_SEL_HYPERSLABS == region_type) {
                        rf.region_type   = "REGION_TYPE BLOCK";
                        long reg_nblocks = H5.H5Sget_select_hyper_nblocks(new_obj_sid);
                        long getblocks[] = new long[(int)(reg_ndims * reg_nblocks) * 2];
                        try {
                            H5.H5Sget_select_hyper_blocklist(new_obj_sid, 0, reg_nblocks, getblocks);
                        }
                        catch (Exception ex5) {
                            log.debug("initRegionDataset H5.H5Sget_select_hyper_blocklist: ", ex5);
                        }
                        sb.append("{ ");
                        for (int i = 0; i < (int)reg_nblocks; i++) {
                            if (i > 0)
                                sb.append(" ");
                            sb.append("(");
                            for (int j = 0; j < (int)reg_ndims; j++) {
                                if (j > 0)
                                    sb.append(",");
                                sb.append(getblocks[i * 2 * (int)reg_ndims + j]);
                            }
                            sb.append(")-(");
                            for (int j = 0; j < (int)reg_ndims; j++) {
                                if (j > 0)
                                    sb.append(",");
                                sb.append(getblocks[i * 2 * (int)reg_ndims + (int)reg_ndims + j]);
                            }
                            sb.append(")");
                        }
                        sb.append(" }");
                        rf.region_desc = sb.toString();
                    }
                    else
                        rf.region_type = "REGION_TYPE UNKNOWN";
                }
                catch (Exception ex4) {
                    log.debug("initRegionDataset Region Type", ex4);
                }
            }
            catch (Exception ex3) {
                log.debug("initRegionDataset Space Open", ex3);
            }
            finally {
                H5.H5Sclose(new_obj_sid);
            }
            log.trace("initRegionDataset finish");
        }
        catch (Exception ex2) {
            log.debug("initRegionDataset ", ex2);
        }
        finally {
            H5.H5Dclose(new_obj_id);
        }
    }

    /**
     * Checks if a reference datatype is all zero.
     *
     * @param refarr
     *            the reference datatype data to be checked.
     *
     * @return true is the reference datatype data is all zero; otherwise returns false.
     */
    public static final boolean zeroArrayCheck(final byte[] refarr)
    {
        for (byte b : refarr) {
            if (b != 0)
                return false;
        }
        return true;
    }

    /**
     * Get the reference datatype reference name.
     *
     * @param refarr
     *            the reference datatype data to be queried.
     *
     * @return the reference datatype name string, null otherwise.
     */
    public final String getObjectReferenceName(byte[] refarr)
    {
        if (!inited)
            init();

        // find the index that matches refarr and ref_array
        H5ReferenceData rf = null;
        for (int i = 0; i < (int)refsize; i++) {
            byte[] theref = refdata.get(i).ref_array;
            if (Arrays.equals(theref, refarr)) {
                rf = refdata.get(i);
                break;
            }
        }
        if (rf == null)
            return null;

        StringBuilder sb = new StringBuilder();
        if (!rf.obj_name.equals("NULL")) {
            sb.append(rf.obj_name);
        }
        if (!rf.attr_name.equals("NULL")) {
            if (sb.length() > 0)
                sb.append("/");
            sb.append(rf.attr_name);
        }
        if (!rf.region_desc.equals("NULL")) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(rf.region_desc);
        }
        log.debug("Reference Object Name {}", sb);
        return sb.toString();
    }

    /**
     * Get the reference datatype reference name.
     *
     * @param refarr
     *            the reference datatype data to be queried.
     *
     * @return the reference datatype name string, null otherwise.
     */
    public final String getFullReferenceName(byte[] refarr)
    {
        if (!inited)
            init();

        // find the index that matches refarr and ref_array
        H5ReferenceData rf = null;
        for (int i = 0; i < (int)refsize; i++) {
            byte[] theref = refdata.get(i).ref_array;
            if (Arrays.equals(theref, refarr)) {
                rf = refdata.get(i);
                break;
            }
        }
        if (rf == null)
            return null;

        StringBuilder sb = new StringBuilder();
        if (!rf.file_name.equals("NULL"))
            sb.append(rf.file_name);
        if (!rf.obj_name.equals("NULL")) {
            if (sb.length() > 0)
                sb.append("/");
            sb.append(rf.obj_name);
        }
        if (!rf.attr_name.equals("NULL")) {
            if (sb.length() > 0)
                sb.append("/");
            sb.append(rf.attr_name);
        }
        if (!rf.region_desc.equals("NULL")) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(rf.region_desc);
        }
        log.debug("Full Reference Name {}", sb);
        return sb.toString();
    }

    /**
     * Get the reference datatype dataset region reference as string.
     *
     * @param refarr
     *            the reference datatype data to be queried.
     *
     * @return the reference datatype name string, null otherwise.
     */
    public final String getRegionDataset(byte[] refarr)
    {
        if (!inited)
            init();

        // find the index that matches refarr and ref_array
        H5ReferenceData rf = null;
        for (int i = 0; i < (int)refsize; i++) {
            byte[] theref = refdata.get(i).ref_array;
            if (Arrays.equals(theref, refarr)) {
                rf = refdata.get(i);
                break;
            }
        }
        if (rf == null)
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append(rf.region_type);
        if (!rf.region_desc.equals("NULL")) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(rf.region_desc);
        }
        log.debug("getRegionDataset Value {}", sb);
        return sb.toString();
    }

    /**
     * Get the reference datatype data.
     *
     * @param refarr
     *            the reference datatype data to be queried.
     *
     * @return the reference datatype data.
     */
    public final H5ReferenceData getReferenceData(byte[] refarr)
    {
        if (!inited)
            init();

        // find the index that matches refarr and ref_array
        H5ReferenceData rf = null;
        for (int i = 0; i < (int)refsize; i++) {
            byte[] theref = refdata.get(i).ref_array;
            if (Arrays.equals(theref, refarr)) {
                rf = refdata.get(i);
                break;
            }
        }
        return rf;
    }

    /**
     * Get the reference datatype region reference as string.
     *
     * @param refarr
     *            the reference datatype data to be queried.
     * @param showData
     *            show the reference region dims
     *
     * @return the reference datatype name string, null otherwise.
     */
    public final String getReferenceRegion(byte[] refarr, boolean showData)
    {
        if (!inited)
            init();

        log.trace("getReferenceRegion refarr {}", refarr);
        // find the index that matches refarr and ref_array
        H5ReferenceData rf = null;
        for (int i = 0; i < (int)refsize; i++) {
            byte[] theref = refdata.get(i).ref_array;
            log.trace("getReferenceRegion theref {}", theref);
            if (Arrays.equals(theref, refarr)) {
                rf = refdata.get(i);
                log.trace("getReferenceRegion rf {}", rf);
                break;
            }
        }
        if (rf == null)
            return null;

        StringBuilder objsb = new StringBuilder();
        if (!rf.file_name.equals("NULL"))
            objsb.append(rf.file_name);
        if (!rf.obj_name.equals("NULL")) {
            objsb.append(rf.obj_name);
        }
        if (!rf.attr_name.equals("NULL")) {
            if (objsb.length() > 0)
                objsb.append("/");
            objsb.append(rf.attr_name);
        }
        log.debug("getReferenceRegion Region Name {}", objsb);

        StringBuilder regsb = new StringBuilder();
        if (!rf.region_type.equals("NULL"))
            regsb.append(rf.region_type);
        if (!rf.region_desc.equals("NULL")) {
            if (regsb.length() > 0)
                regsb.append(" ");
            regsb.append(rf.region_desc);
        }
        log.debug("getReferenceRegion Region Type {}", regsb);
        StringBuilder sb = new StringBuilder(objsb);
        if (regsb.length() > 0) {
            sb.append(" ");
            sb.append(regsb);
        }
        if (sb.length() > 0)
            return sb.toString();
        else
            return "NULL";
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
    public String toString(String delimiter) { return toString(delimiter, -1); }

    /**
     * Returns a string representation of the data value.
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
    public String toString(String delimiter, int maxItems)
    {
        Object theData = originalRefBuf;
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
        log.trace("toString: refsize={} isStdRef={} Array.getLength={}", refsize, isStdRef(),
                  Array.getLength(theData));
        if (isStdRef()) {
            String cname = valClass.getName();
            char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
            log.trace("toString: isStdRef with cname={} dname={}", cname, dname);
            for (int i = 0; i < (int)refsize; i++) {
                int refIndex  = HDF5Constants.H5R_REF_BUF_SIZE * i;
                byte[] refarr = new byte[(int)HDF5Constants.H5R_REF_BUF_SIZE];
                System.arraycopy(theData, refIndex, refarr, 0, (int)HDF5Constants.H5R_REF_BUF_SIZE);
                log.trace("toString: refarr[{}]={}", i, refarr);
                String refarr_str     = getReferenceRegion(refarr, false);
                StringBuilder ref_str = null;
                if (refarr_str != null) {
                    ref_str = new StringBuilder(refarr_str);
                    if ((maxItems > 0) && (ref_str.length() > maxItems)) {
                        ref_str.setLength(maxItems);
                    }
                    log.trace("toString: ref_str[{}]={}", i, ref_str);
                }
                else
                    ref_str = new StringBuilder("NULL");
                if (i > 0)
                    sb.append(", ");
                sb.append(ref_str);
            }
            return sb.toString();
        }
        return toString(delimiter, maxItems);
    }

    /**
     * The individual reference data for a given object.
     */
    public static class H5ReferenceData {
        private static final Logger log = LoggerFactory.getLogger(H5ReferenceData.class);

        /** The reference array raw data */
        public byte[] ref_array = null;

        /** The the full file path referenced */
        public String file_fullpath;

        /** The file name referenced */
        public String file_name;

        /** The object name referenced */
        public String obj_name;

        /** The attribute name referenced */
        public String attr_name;

        /** The type of region referenced */
        public String region_type;

        /** The point/block description of region referenced */
        public String region_desc;

        /** The default type of region referenced */
        public int ref_type = HDF5Constants.H5R_BADTYPE;

        /** The default type of object referenced */
        public int obj_type = HDF5Constants.H5O_TYPE_UNKNOWN;

        /** The type size of object referenced */
        public long typeSize;

        /**
         * Copy the individual reference array for further processing
         *
         * @param theArray    the reference datatype data to be copied.
         * @param theTypeSize the size of the type for the array
         */
        H5ReferenceData(byte[] theArray, long theTypeSize)
        {
            typeSize  = theTypeSize;
            ref_array = new byte[(int)theTypeSize];
            System.arraycopy(theArray, 0, ref_array, 0, (int)theTypeSize);
        }
    }
}
