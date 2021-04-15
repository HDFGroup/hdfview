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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.object.Attribute;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;

/**
 * An attribute is a (name, value) pair of metadata attached to a primary data object such as a
 * dataset, group or named datatype.
 * <p>
 * Like a dataset, an attribute has a name, datatype and dataspace.
 *
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
 * Attribute dataRange = new Attribute(name, type, dims);
 * // Set the attribute value
 * dataRange.setValue(value);
 * // See FileFormat.writeAttribute() for how to attach an attribute to an object,
 * &#64;see hdf.object.FileFormat#writeAttribute(HObject, Attribute, boolean)
 * </pre>
 *
 *
 * For an atomic datatype, the value of an Attribute will be a 1D array of integers, floats and
 * strings. For a compound datatype, it will be a 1D array of strings with field members separated
 * by a comma. For example, "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of {int,
 * float} of three data points.
 *
 * @see hdf.object.Datatype
 *
 * @version 2.0 4/2/2018
 * @author Peter X. Cao, Jordan T. Henderson
 */
public class H5Attribute extends Attribute {

    private static final long serialVersionUID = 2072473407027648309L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5Attribute.class);


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
    public H5Attribute(HObject parentObj, String attrName, Datatype attrType, long[] attrDims) {
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
     * Attribute attr = new Attribute(attrName, attrType, attrDims, classValue);
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
    public H5Attribute(HObject parentObj, String attrName, Datatype attrType, long[] attrDims, Object attrValue) {
        super(parentObj, attrName, attrType, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        long aid = super.open();
        long pObjID = -1;

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

    @Override
    public void init() {
        super.init();
        if (inited) {
            return;
        }

        if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
            long aid = -1;
            long tid = -1;
            int tclass = -1;
            flatNameList = new Vector<>();
            flatTypeList = new Vector<>();
            long[] memberTIDs = null;

            log.trace("init(): FILE_TYPE_HDF5");
            aid = open();
            if (aid >= 0) {
                try {
                    tid = H5.H5Aget_type(aid);
                    tclass = H5.H5Tget_class(tid);

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

                    if (H5.H5Tget_class(tid) == HDF5Constants.H5T_COMPOUND) {
                        // initialize member information
                        H5Datatype.extractCompoundInfo((H5Datatype) getDatatype(), "", flatNameList, flatTypeList);
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
                        log.debug("init(): H5Tclose({}) failure: ", tid, ex2);
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
            }
        }

        resetSelection();
    }

    /**
     * Given an array of bytes representing a compound Datatype and a start index
     * and length, converts len number of bytes into the correct Object type and
     * returns it.
     *
     * @param data
     *            The byte array representing the data of the compound Datatype
     * @param data_type
     *            The type of data to convert the bytes to
     * @param start
     *            The start index of the bytes to get
     * @param len
     *            The number of bytes to convert
     * @return The converted type of the bytes
     */
    @Override
    protected Object convertCompoundByteMember(byte[] data, long data_type, long start, long len) {
        Object currentData = null;

        try {
            long typeClass = H5.H5Tget_class(data_type);
            long size = H5.H5Tget_size(data_type);

            if (typeClass == HDF5Constants.H5T_INTEGER) {
                currentData = HDFNativeData.byteToInt((int) start, (int) (len / size), data);
            }
            else if (typeClass == HDF5Constants.H5T_FLOAT) {
                currentData = HDFNativeData.byteToDouble((int) start, 1, data);
            }
        }
        catch (Exception ex) {
            log.debug("convertCompoundByteMember(): conversion failure: ", ex);
        }

        return currentData;
    }
}
