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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;

/**
 * This class defines HDF5 datatype characteristics and APIs for a data type.
 * <p>
 * This class provides several methods to convert an HDF5 datatype identifier to a datatype object, and vice versa. A
 * datatype object is described by four basic fields: datatype class, size, byte order, and sign, while an HDF5 datatype
 * is presented by a datatype identifier.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5Datatype extends Datatype {
    private static final long serialVersionUID = -750546422258749792L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5Datatype.class);

    /**
     * The list of attributes of this data object.
     */
    private List<Attribute> attributeList;

    /** Flag to indicate if this datatype is a named datatype */
    private boolean isNamed = false;

    private int nAttributes = -1;

    private H5O_info_t obj_info;

    /**
     * Constructs an named HDF5 data type object for a given file, dataset name and group path.
     * <p>
     * The datatype object represents an existing named datatype in file. For example,
     *
     * <pre>
     * new H5Datatype(file, "dtype1", "/g0")
     * </pre>
     *
     * constructs a datatype object that corresponds to the dataset,"dset1", at group "/g0".
     *
     * @param theFile
     *            the file that contains the dataset.
     * @param name
     *            the name of the dataset such as "dset1".
     * @param path
     *            the group path to the dataset such as "/g0/".
     */
    public H5Datatype(FileFormat theFile, String name, String path) {
        this(theFile, name, path, null);
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using {@link #H5Datatype(FileFormat, String, String)}
     *
     * @param theFile
     *            the file that contains the dataset.
     * @param name
     *            the name of the dataset such as "dset1".
     * @param path
     *            the group path to the dataset such as "/g0/".
     * @param oid
     *            the oid of the dataset.
     */
    @Deprecated
    public H5Datatype(FileFormat theFile, String name, String path, long[] oid) {
        super(theFile, name, path, oid);
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

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few example of H5Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, NATIVE, NATIVE, SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, 2, ORDER_BE, NATIVE);
     * <li>to create native float<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, NATIVE, NATIVE, -1);
     * <li>to create 64-bit double<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, 8, NATIVE, -1);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX and ORDER_NONE
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and MSGN
     */
    public H5Datatype(int tclass, int tsize, int torder, int tsign) {
        this(tclass, tsize, torder, tsign, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few example of H5Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, NATIVE, NATIVE, SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, 2, ORDER_BE, NATIVE);
     * <li>to create native float<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, NATIVE, NATIVE, -1);
     * <li>to create 64-bit double<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, 8, NATIVE, -1);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX and ORDER_NONE
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and MSGN
     * @param tbase
     *            the base datatype of the new datatype
     */
    public H5Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase) {
        this(tclass, tsize, torder, tsign, tbase, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few example of H5Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, NATIVE, NATIVE, SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, 2, ORDER_BE, NATIVE);
     * <li>to create native float<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, NATIVE, NATIVE, -1);
     * <li>to create 64-bit double<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, 8, NATIVE, -1);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX and
     *            ORDER_NONE
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and MSGN
     * @param tbase
     *            the base datatype of the new datatype
     * @param pbase
     *            the parent datatype of the new datatype
     */
    public H5Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase, Datatype pbase) {
        super(tclass, tsize, torder, tsign, tbase, pbase);
        datatypeDescription = getDescription();
    }

    /**
     * Constructs a Datatype with a given native datatype identifier.
     * <p>
     * For example, if the datatype identifier is a 32-bit unsigned integer created from HDF5,
     *
     * <pre>
     * int tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * Datatype dtype = new Datatype(tid);
     * </pre>
     *
     * will construct a datatype equivalent to new Datatype(CLASS_INTEGER, 4, NATIVE, SIGN_NONE);
     *
     * @see #fromNative(long nativeID)
     *
     * @param nativeID
     *            the native datatype identifier.
     */
    public H5Datatype(long nativeID) {
        this(nativeID, null);
    }

    /**
     * Constructs a Datatype with a given native datatype identifier.
     * <p>
     * For example, if the datatype identifier is a 32-bit unsigned integer created from HDF5,
     *
     * <pre>
     * int tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * Datatype dtype = new Datatype(tid);
     * </pre>
     *
     * will construct a datatype equivalent to new Datatype(CLASS_INTEGER, 4, NATIVE, SIGN_NONE);
     *
     * @see #fromNative(long nativeID)
     *
     * @param nativeID
     *            the native datatype identifier.
     * @param pbase
     *            the parent datatype of the new datatype
     */
    public H5Datatype(long nativeID, Datatype pbase) {
        super(nativeID, pbase);
        fromNative(nativeID);
        datatypeDescription = getDescription();
    }

    /**
     * Opens access to a named datatype.
     * <p>
     * It calls H5.H5Topen(loc, name).
     *
     * @return the datatype identifier if successful; otherwise returns negative value.
     *
     * @see hdf.hdf5lib.H5#H5Topen(long, String, long)
     */
    @Override
    public long open() {
        log.trace("open(): start");
        long tid = -1;

        try {
            tid = H5.H5Topen(getFID(), getPath() + getName(), HDF5Constants.H5P_DEFAULT);
        }
        catch (HDF5Exception ex) {
            tid = -1;
        }

        log.trace("open(): finish");
        return tid;
    }

    /**
     * Closes a datatype identifier.
     * <p>
     * It calls H5.H5close(tid).
     *
     * @param tid
     *            the datatype ID to close
     */
    @Override
    public void close(long tid) {
        try {
            H5.H5Tclose(tid);
        }
        catch (HDF5Exception ex) {
            log.debug("close(): H5Tclose(tid {}) failure: ", tid, ex);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#hasAttribute()
     */
    @Override
    public boolean hasAttribute() {
        obj_info.num_attrs = nAttributes;

        if (obj_info.num_attrs < 0) {
            long tid = -1;
            try {
                tid = H5.H5Topen(getFID(), getPath() + getName(), HDF5Constants.H5P_DEFAULT);
                fromNative(tid);
                obj_info = H5.H5Oget_info(tid);
                isNamed = true;
            }
            catch (Exception ex) {
                obj_info.num_attrs = 0;
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("hasAttribute(): H5Tclose(tid {}) failure:", tid, ex);
                }
            }
        }

        log.trace("hasAttribute(): nAttributes={}", obj_info.num_attrs);

        return (obj_info.num_attrs > 0);
    }

    /**
     * Converts values in an Enumeration Datatype to names.
     * <p>
     * This method searches the identified enumeration datatype for the values appearing in
     * <code>inValues</code> and returns the names corresponding to those values. If a given value is
     * not found in the enumeration datatype, the name corresponding to that value will be set to
     * <code>"ENUM ERR value"</code> in the string array that is returned.
     * <p>
     * If the method fails in general, null will be returned instead of a String array. An empty
     * <code>inValues</code> parameter or an invalid <code>tid</code> would cause
     * general failure.
     *
     * @param tid
     *            The identifier of the enumeration datatype.
     * @param inValues
     *            The array of enumerations values to be converted.
     *
     * @return The string array of names if successful; otherwise return null.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     *
     */
    public static final String[] convertEnumValueToName(long tid, Object inValues) throws HDF5Exception {
        H5Datatype datatype = new H5Datatype(tid);
        return datatype.convertEnumValueToName(inValues);
    }

    /**
     * Converts values in an Enumeration Datatype to names.
     * <p>
     * This method searches the identified enumeration datatype for the values appearing in
     * <code>inValues</code> and returns the names corresponding to those values. If a given value is
     * not found in the enumeration datatype, the name corresponding to that value will be set to
     * <code>"ENUM ERR value"</code> in the string array that is returned.
     * <p>
     * If the method fails in general, null will be returned instead of a String array. An empty
     * <code>inValues</code> parameter would cause general failure.
     *
     * @param inValues
     *            The array of enumerations values to be converted.
     *
     * @return The string array of names if successful; otherwise return null.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     *
     */
    public String[] convertEnumValueToName(Object inValues) throws HDF5Exception {
        log.trace("convertEnumValueToName() inValues={} start", inValues);
        int inSize = 0;
        String[] outNames = null;
        String cName = inValues.getClass().getName();
        boolean isArray = cName.lastIndexOf("[") >= 0;
        if (isArray) {
            inSize = Array.getLength(inValues);
        }
        else {
            inSize = 1;
        }

        if ((inValues == null) || (inSize <= 0)) {
            log.debug("convertEnumValueToName() failure: in values null or inSize length invalid");
            log.debug("convertEnumValueToName(): inValues={} inSize={}", inValues, inSize);
            log.trace("convertEnumValueToName(): finish");
            return null;
        }

        if (enumMembers == null || enumMembers.size() <= 0) {
            log.debug("convertEnumValueToName(): no members");
            log.trace("convertEnumValueToName(): finish");
            return null;
        }

        log.trace("convertEnumValueToName(): inSize={} nMembers={} enums={}", inSize, enumMembers.size(), enumMembers);
        outNames = new String[inSize];
        for (int i = 0; i < inSize; i++) {
            if (isArray) {
                if (enumMembers.containsKey(String.valueOf(Array.get(inValues, i)))) {
                    outNames[i] = enumMembers.get(String.valueOf(Array.get(inValues, i)));
                }
                else {
                    outNames[i] = "**ENUM ERR " + String.valueOf(Array.get(inValues, i)) + "**";
                }
            }
            else {
                if (enumMembers.containsKey(String.valueOf(inValues))) {
                    outNames[i] = enumMembers.get(String.valueOf(inValues));
                }
                else {
                    outNames[i] = "**ENUM ERR " + String.valueOf(inValues) + "**";
                }
            }
        }

        log.trace("convertEnumValueToName(): finish");
        return outNames;
    }

    /**
     * Converts names in an Enumeration Datatype to values.
     * <p>
     * This method searches the identified enumeration datatype for the names appearing in <code>inValues</code> and
     * returns the values corresponding to those names.
     *
     * @param tid
     *            The identifier of the enumeration datatype.
     * @param in
     *            The array of enumerations names to be converted.
     *
     * @return The int array of values if successful; otherwise return null.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     *
     */
    public static final Object[] convertEnumNameToValue(long tid, String[] in) throws HDF5Exception {
        H5Datatype datatype = new H5Datatype(tid);
        return datatype.convertEnumNameToValue(in);
    }

    /**
     * Converts names in an Enumeration Datatype to values.
     * <p>
     * This method searches the identified enumeration datatype for the names appearing in
     * <code>inValues</code> and returns the values corresponding to those names.
     *
     * @param in
     *            The array of enumerations names to be converted.
     *
     * @return The int array of values if successful; otherwise return null.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     *
     */
    public Object[] convertEnumNameToValue(String[] in) throws HDF5Exception {
        log.trace("convertEnumNameToValue() start");
        int size = 0;

        if ((in == null) || ((size = Array.getLength(in)) <= 0)) {
            log.debug("convertEnumNameToValue() failure: in values null or in size not valid");
            log.debug("convertEnumNameToValue(): in={} inSize={}", in.toString(), in.length);
            log.trace("convertEnumNameToValue(): finish");
            return null;
        }

        if (enumMembers == null || enumMembers.size() <= 0) {
            log.debug("convertEnumNameToValue(): no members");
            log.trace("convertEnumNameToValue(): finish");
            return null;
        }

        Object[] out = null;
        if (datatypeSize == 1) {
            out = new Byte[size];
        }
        else if (datatypeSize == 2) {
            out = new Short[size];
        }
        else if (datatypeSize == 4) {
            out = new Integer[size];
        }
        else if (datatypeSize == 8) {
            out = new Long[size];
        }
        else {
            out = new Object[size];
        }

        for (int i = 0; i < size; i++) {
            if (in[i] == null || in[i].length() <= 0)
                continue;

            for (Entry<String, String> entry : enumMembers.entrySet()) {
                if (Objects.equals(in[i], entry.getValue())) {
                    if (datatypeSize == 1) {
                        log.trace("convertEnumNameToValue(): ENUM is H5T_NATIVE_INT8");
                        out[i] = Byte.parseByte(entry.getKey());
                    }
                    else if (datatypeSize == 2) {
                        log.trace("convertEnumNameToValue(): CLASS_INT-ENUM is H5T_NATIVE_INT16");
                        out[i] = Short.parseShort(entry.getKey());
                    }
                    else if (datatypeSize == 4) {
                        log.trace("convertEnumNameToValue(): CLASS_INT-ENUM is H5T_NATIVE_INT32");
                        out[i] = Integer.parseInt(entry.getKey());
                    }
                    else if (datatypeSize == 8) {
                        log.trace("convertEnumNameToValue(): CLASS_INT-ENUM is H5T_NATIVE_INT64");
                        out[i] = Long.parseLong(entry.getKey());
                    }
                    else {
                        log.debug("convertEnumNameToValue(): enum datatypeSize incorrect");
                        out[i] = -1;
                    }
                    break;
                }
            }
        }

        log.trace("convertEnumNameToValue(): finish");
        return out;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#fromNative(int)
     */
    @Override
    public void fromNative(long tid) {
        log.trace("fromNative(): start: tid={}", tid);
        int tclass = -1;
        long tsize = -1;
        int torder = -1;
        boolean isChar = false, isUchar = false;

        if (tid < 0) {
            datatypeClass = CLASS_NO_CLASS;
        }
        else {
            try {
                tclass = H5.H5Tget_class(tid);
                tsize = H5.H5Tget_size(tid);
                torder = H5.H5Tget_order(tid);
                is_VLEN = (tclass == HDF5Constants.H5T_VLEN) || H5.H5Tis_variable_str(tid);
                log.trace("fromNative(): tclass={}, tsize={}, torder={}, isVLEN={}", tclass, tsize, torder, is_VLEN);
            }
            catch (Exception ex) {
                log.debug("fromNative(): failure: ", ex);
                datatypeClass = CLASS_NO_CLASS;
            }

            if (torder == HDF5Constants.H5T_ORDER_BE)
                datatypeOrder = ORDER_BE;
            else
                datatypeOrder = ORDER_LE;

            try {
                isUchar = H5.H5Tequal(tid, HDF5Constants.H5T_NATIVE_UCHAR);
                isChar = (H5.H5Tequal(tid, HDF5Constants.H5T_NATIVE_CHAR) || isUchar);
            }
            catch (Exception ex) {
                log.debug("fromNative(): native char type failure: ", ex);
            }

            if (tclass == HDF5Constants.H5T_ARRAY) {
                long tmptid = -1;
                datatypeClass = CLASS_ARRAY;
                try {
                    int ndims = H5.H5Tget_array_ndims(tid);
                    arrayDims = new long[ndims];
                    H5.H5Tget_array_dims(tid, arrayDims);
                    tmptid = H5.H5Tget_super(tid);
                    baseType = new H5Datatype(tmptid);
                    is_VLEN = (baseType.getDatatypeClass() == HDF5Constants.H5T_VLEN) || H5.H5Tis_variable_str(tmptid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): array type failure: ", ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tmptid);
                    }
                    catch (Exception ex) {
                        log.debug("fromNative(): array H5Tclose(tmptid {}) failure: ", tmptid, ex);
                    }
                }
            }
            else if (tclass == HDF5Constants.H5T_COMPOUND) {
                datatypeClass = CLASS_COMPOUND;

                try {
                    int nMembers = H5.H5Tget_nmembers(tid);
                    compoundMemberNames = new Vector<>(nMembers);
                    compoundMemberTypes = new Vector<>(nMembers);
                    compoundMemberOffsets = new Vector<>(nMembers);
                    compoundMemberFieldIDs = new Vector<>(nMembers);

                    for (int i = 0; i < nMembers; i++) {
                        String memberName = H5.H5Tget_member_name(tid, i);
                        long memberOffset = H5.H5Tget_member_offset(tid, i);
                        long memberID = -1;
                        H5Datatype t = null;
                        try {
                            memberID = H5.H5Tget_member_type(tid, i);
                            t = new H5Datatype(memberID);
                        }
                        catch (Exception ex1) {
                            log.debug("fromNative(): compound type failure: ", ex1);
                        }
                        finally {
                            try {
                                H5.H5Tclose(memberID);
                            }
                            catch (Exception ex2) {
                                log.debug("fromNative(): compound H5Tclose(memberID {}) failure: ", memberID, ex2);
                            }
                        }

                        compoundMemberNames.add(i, memberName);
                        compoundMemberOffsets.add(i, memberOffset);
                        compoundMemberFieldIDs.add(i, memberID);
                        compoundMemberTypes.add(i, t);
                    }
                }
                catch (HDF5LibraryException ex) {
                    log.debug("fromNative(): compound type failure: ", ex);
                }
            }
            else if (isChar) {
                datatypeClass = CLASS_CHAR;
                if (isUchar)
                    datatypeSign = SIGN_NONE;
                else
                    datatypeSign = SIGN_2;
            }
            else if (tclass == HDF5Constants.H5T_INTEGER) {
                datatypeClass = CLASS_INTEGER;
                try {
                    int tsign = H5.H5Tget_sign(tid);
                    if (tsign == HDF5Constants.H5T_SGN_NONE) {
                        datatypeSign = SIGN_NONE;
                    }
                    else
                        datatypeSign = SIGN_2;

                }
                catch (Exception ex) {
                    log.debug("fromNative(): int type failure: ", ex);
                }
            }
            else if (tclass == HDF5Constants.H5T_FLOAT) {
                datatypeClass = CLASS_FLOAT;
            }
            else if (tclass == HDF5Constants.H5T_STRING) {
                try {
                    is_VLEN = H5.H5Tis_variable_str(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): var str type failure: ", ex);
                }

                datatypeClass = CLASS_STRING;
            }
            else if (tclass == HDF5Constants.H5T_REFERENCE) {
                datatypeClass = CLASS_REFERENCE;
            }
            else if (tclass == HDF5Constants.H5T_ENUM) {
                datatypeClass = CLASS_ENUM;
                long tmptid = -1;
                long basetid = -1;
                try {
                    log.trace("fromNative(): enum type");
                    basetid = H5.H5Tget_super(tid);
                    tmptid = basetid;
                    basetid = H5.H5Tget_native_type(tmptid);
                    log.debug("fromNative(): enum type basetid={}", basetid);
                    if (basetid >= 0) {
                        baseType = new H5Datatype(tmptid, this);
                        datatypeSign = baseType.getDatatypeSign();
                    }
                }
                catch (Exception ex) {
                    log.debug("fromNative(): enum type failure: ", ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tmptid);
                    }
                    catch (Exception ex) {
                        log.debug("fromNative(): enum H5Tclose(tmptid {}) failure: ", tmptid, ex);
                    }
                    try {
                        H5.H5Tclose(basetid);
                    }
                    catch (Exception ex) {
                        log.debug("fromNative(): enum H5Tclose(basetid {}) failure: ", basetid, ex);
                    }
                }
                try {
                    int enumMemberCount = H5.H5Tget_nmembers(tid);
                    String name = null;
                    String enumStr = null;
                    byte[] val = new byte[(int)tsize];
                    enumMembers = new HashMap<>();
                    for (int i = 0; i < enumMemberCount; i++) {
                        name = H5.H5Tget_member_name(tid, i);
                        H5.H5Tget_member_value(tid, i, val);
                        switch ((int)H5.H5Tget_size(tid)) {
                            case 1:
                                enumStr = Byte.toString((HDFNativeData.byteToByte(val[0]))[0]);
                                break;
                            case 2:
                                enumStr = Short.toString((HDFNativeData.byteToShort(val))[0]);
                                break;
                            case 4:
                                enumStr = Integer.toString((HDFNativeData.byteToInt(val))[0]);
                                break;
                            case 8:
                                enumStr = Long.toString((HDFNativeData.byteToLong(val))[0]);
                                break;
                            default:
                                enumStr = "-1";
                                break;
                        }
                        enumMembers.put(enumStr, name);
                    }
                }
                catch (Exception ex) {
                    log.debug("fromNative(): enum type failure: ", ex);
                }
            }
            else if (tclass == HDF5Constants.H5T_VLEN) {
                long tmptid = -1;
                datatypeClass = CLASS_VLEN;
                try {
                    tmptid = H5.H5Tget_super(tid);
                    baseType = new H5Datatype(tmptid);
                }
                catch (Exception ex) {
                }
                finally {
                    try {
                        H5.H5Tclose(tmptid);
                    }
                    catch (Exception ex) {
                        log.debug("fromNative(): vlen H5Tclose(tmptid {}) failure: ", tmptid, ex);
                    }
                }
            }
            else if (tclass == HDF5Constants.H5T_BITFIELD) {
                datatypeClass = CLASS_BITFIELD;
            }
            else if (tclass == HDF5Constants.H5T_OPAQUE) {
                datatypeClass = CLASS_OPAQUE;
            }
            else {
                log.debug("fromNative(): datatypeClass is unknown");
            }

            if (is_VLEN)
                datatypeSize = -1;
            else
                datatypeSize = tsize;
        }
        log.trace("fromNative(): datatypeClass={} baseType={} datatypeSize={}", datatypeClass, baseType, datatypeSize);
        log.trace("fromNative(): finish");
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link hdf.hdf5lib.H5#H5Tget_native_type(long)}
     *             <p>
     *             Return the HDF5 memory datatype identifier based on the HDF5 datatype identifier on disk
     *             <p>
     * @param tid
     *            the datatype identification disk.
     *
     * @return the memory datatype identifier if successful, and negative otherwise.
     */
    @Deprecated
    public static long toNative(long tid) {
        // data type information
        long native_type = -1;

        try {
            native_type = H5.H5Tget_native_type(tid);
        }
        catch (Exception ex) {
            log.debug("toNative(): H5Tget_native_type(tid {}) failure: ", tid, ex);
        }

        try {
            if (H5.H5Tis_variable_str(tid))
                H5.H5Tset_size(native_type, HDF5Constants.H5T_VARIABLE);
        }
        catch (Exception ex) {
            log.debug("toNative(): var str type size failure: ", ex);
        }

        return native_type;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#createNative()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public long createNative() {
        log.trace("createNative(): start");

        long tid = -1;
        long tmptid = -1;

        if (isNamed) {
            try {
                tid = H5.H5Topen(getFID(), getPath() + getName(), HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                log.debug("createNative(): name {} H5Topen failure: ", getPath() + getName(), ex);
            }
        }

        if (tid >= 0) {
            log.trace("createNative(): tid >= 0");
            log.trace("createNative(): finish");
            return tid;
        }

        // figure the datatype
        try {
            log.trace("createNative(): datatypeClass={} baseType={} datatypeSize={}", datatypeClass, baseType, datatypeSize);
            switch (datatypeClass) {
                case CLASS_ARRAY:
                    if (baseType != null) {
                        if ((tmptid = baseType.createNative()) >= 0) {
                            try {
                                tid = H5.H5Tarray_create(tmptid, arrayDims.length, arrayDims);
                            }
                            finally {
                                close(tmptid);
                            }
                        }
                    }
                    else {
                        log.debug("createNative(): CLASS_ARRAY base type is NULL");
                    }
                    break;
                case CLASS_COMPOUND:
                    try {
                        tid = H5.H5Tcreate(CLASS_COMPOUND, datatypeSize);

                        for (int i = 0; i < compoundMemberNames.size(); i++) {
                            String memberName = compoundMemberNames.get(i);
                            long memberOffset = compoundMemberOffsets.get(i);
                            long memberID = compoundMemberFieldIDs.get(i);

                            H5.H5Tinsert(tid, memberName, memberOffset, memberID);
                        }
                    }
                    catch (Exception ex) {
                        log.trace("createNative(): failure: ", ex);
                    }
                    break;
                case CLASS_INTEGER:
                case CLASS_ENUM:
                    if (datatypeSize == 1) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT8");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT8);
                    }
                    else if (datatypeSize == 2) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT16");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT16);
                    }
                    else if (datatypeSize == 4) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT32");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT32);
                    }
                    else if (datatypeSize == 8) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT64");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT64);
                    }
                    else {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT);
                    }

                    if (datatypeOrder == Datatype.ORDER_BE) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_ORDER_BE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_BE);
                    }
                    else if (datatypeOrder == Datatype.ORDER_LE) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_ORDER_LE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_LE);
                    }

                    if (datatypeSign == Datatype.SIGN_NONE) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_SGN_NONE");
                        H5.H5Tset_sign(tid, HDF5Constants.H5T_SGN_NONE);
                    }
                    break;
                case CLASS_FLOAT:
                    if (datatypeSize == 8) {
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_DOUBLE);
                    }
                    else {
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_FLOAT);
                    }

                    if (datatypeOrder == Datatype.ORDER_BE) {
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_BE);
                    }
                    else if (datatypeOrder == Datatype.ORDER_LE) {
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_LE);
                    }
                    break;
                case CLASS_CHAR:
                    if (datatypeSign == Datatype.SIGN_NONE) {
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UCHAR);
                    }
                    else {
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_CHAR);
                    }
                    break;
                case CLASS_STRING:
                    tid = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
                    if (is_VLEN || datatypeSize < 0)
                        H5.H5Tset_size(tid, HDF5Constants.H5T_VARIABLE);
                    else
                        H5.H5Tset_size(tid, datatypeSize);

                    log.trace("createNative(): isVlenStr={}", is_VLEN);
                    // H5.H5Tset_strpad(tid, HDF5Constants.H5T_STR_NULLPAD);
                    break;
                case CLASS_REFERENCE:
                    if (datatypeSize > H5.H5Tget_size(HDF5Constants.H5T_STD_REF_OBJ)) {
                        tid = H5.H5Tcopy(HDF5Constants.H5T_STD_REF_DSETREG);
                    }
                    else {
                        tid = H5.H5Tcopy(HDF5Constants.H5T_STD_REF_OBJ);
                    }
                    break;
                case CLASS_VLEN:
                    if (baseType != null) {
                        if ((tmptid = baseType.createNative()) >= 0) {
                            try {
                                tid = H5.H5Tvlen_create(tmptid);
                            }
                            finally {
                                close(tmptid);
                            }
                        }
                    }
                    else {
                        log.debug("createNative(): CLASS_VLEN base type is NULL");
                    }
                    break;
                case CLASS_BITFIELD:
                case CLASS_OPAQUE:
                    if (datatypeSize == 1) {
                        log.trace("createNative(): CLASS_BITFIELD-OPAQUE is H5T_NATIVE_INT8");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT8);
                    }
                    else if (datatypeSize == 2) {
                        log.trace("createNative(): CLASS_BITFIELD-OPAQUE is H5T_NATIVE_INT16");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT16);
                    }
                    else if (datatypeSize == 4) {
                        log.trace("createNative(): CLASS_BITFIELD-OPAQUE is H5T_NATIVE_INT32");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT32);
                    }
                    else if (datatypeSize == 8) {
                        log.trace("createNative(): CLASS_BITFIELD-OPAQUE is H5T_NATIVE_INT64");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT64);
                    }
                    else {
                        log.trace("createNative(): CLASS_BITFIELD-OPAQUE is H5T_NATIVE_INT");
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT);
                    }

                    if (datatypeOrder == Datatype.ORDER_BE) {
                        log.trace("createNative(): CLASS_BITFIELD-OPAQUE is H5T_ORDER_BE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_BE);
                    }
                    else if (datatypeOrder == Datatype.ORDER_LE) {
                        log.trace("createNative(): CLASS_BITFIELD-OPAQUE is H5T_ORDER_LE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_LE);
                    }
                    break;
                default:
                    log.debug("createNative(): Unknown class");
                    break;
            } // switch (tclass)
        }
        catch (Exception ex) {
            log.debug("createNative(): Error figuring the datatype: ", ex);
            tid = -1;
        }

        // set up enum members
        if (datatypeClass == CLASS_ENUM) {
            long ptid = tid;
            try {
                tid = H5.H5Tenum_create(ptid);
                datatypeSize = H5.H5Tget_size(tid);
            }
            catch (Exception ex) {
                log.debug("createNative(): create members failure: ", ex);
                tid = -1;
            }

            try {
                String memstr, memname;
                byte[] memval = null;
                if (datatypeSize == 1) {
                    memval = HDFNativeData.byteToByte(new Byte((byte) 0));
                }
                else if (datatypeSize == 2) {
                    memval = HDFNativeData.shortToByte(new Short((short) 0));
                }
                else if (datatypeSize == 4) {
                    memval = HDFNativeData.intToByte(new Integer(0));
                }
                else if (datatypeSize == 8) {
                    memval = HDFNativeData.longToByte(new Long(0));
                }

                // using "0" and "1" as default
                if (enumMembers == null) {
                    enumMembers = new HashMap<>();
                    enumMembers.put("1", "0");
                    enumMembers.put("2", "1");
                    log.trace("createNative(): default string");
                }
                Iterator entries = enumMembers.entrySet().iterator();
                while (entries.hasNext()) {
                    Entry thisEntry = (Entry) entries.next();
                    memstr = (String) thisEntry.getKey();
                    memname = (String) thisEntry.getValue();

                    if (datatypeSize == 1) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT8");
                        Byte tval = Byte.parseByte(memstr);
                        memval = HDFNativeData.byteToByte(tval);
                    }
                    else if (datatypeSize == 2) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT16");
                        Short tval = Short.parseShort(memstr);
                        memval = HDFNativeData.shortToByte(tval);
                    }
                    else if (datatypeSize == 4) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT32");
                        Integer tval = Integer.parseInt(memstr);
                        memval = HDFNativeData.intToByte(tval);
                    }
                    else if (datatypeSize == 8) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT64");
                        Long tval = Long.parseLong(memstr);
                        memval = HDFNativeData.longToByte(tval);
                    }
                    else {
                        log.debug("createNative(): enum datatypeSize incorrect");
                    }
                    log.trace("createNative(): H5Tenum_insert {} {}", memname, memval);
                    H5.H5Tenum_insert(tid, memname, memval);
                }
            }
            catch (Exception ex) {
                log.debug("createNative(): set up enum members failure: ", ex);
            }

            try {
                H5.H5Tclose(ptid);
            }
            catch (Exception ex) {
                log.debug("createNative(): H5Tclose(ptid {}) failure: ", ptid, ex);
            }
        } // if (datatypeClass == CLASS_ENUM) {

        return tid;
    }

    /**
     * Allocates a one-dimensional array of byte, short, int, long, float, double, or String to store data in memory.
     *
     * For example,
     *
     * <pre>
     * long tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT32);
     * int[] data = (int[]) allocateArray(tid, 100);
     * </pre>
     *
     * returns a 32-bit integer array of size 100.
     *
     * @param tid
     *            the datatype id.
     * @param size
     *            the total number of data points of the array.
     *
     * @return the array object if successful; otherwise, return null.
     *
     * @throws OutOfMemoryError
     *            If there is a failure.
     */
    public static Object allocateArray(long tid, int size) throws OutOfMemoryError {
        log.trace("allocateArray(): start: tid={} size={}", tid, size);
        Object data = null;
        boolean isVL = false;
        boolean is_variable_str = false;
        boolean is_reg_ref = false;

        if (size < 0) {
            log.debug("allocateArray(): size < 0");
            log.trace("allocateArray(): finish");
            return null;
        }

        // Scalar members have dimensionality zero, i.e. size =0
        // what can we do about it, set the size to 1
        if (size == 0) {
            size = 1;
        }

        // data type information
        int tclass = -1;
        long tsize = -1;

        try {
            tclass = H5.H5Tget_class(tid);
            tsize = H5.H5Tget_size(tid);
            log.trace("allocateArray(): tclass={} : tsize={}", tclass, tsize);
        }
        catch (Exception ex) {
            log.debug("allocateArray(): H5Tget_xxxx data type information failure: ", ex);
        }

        try {
            is_variable_str = H5.H5Tis_variable_str(tid);
        }
        catch (Exception ex) {
            log.debug("allocateArray(): H5Tis_variable_str(tid {}) failure: ", tid, ex);
        }
        isVL = (tclass == HDF5Constants.H5T_VLEN);

        try {
            is_reg_ref = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_DSETREG);
        }
        catch (Exception ex) {
            log.debug("allocateArray(): H5Tequal(tid {}, HDF5Constants.H5T_STD_REF_DSETREG) failure: ", tid, ex);
        }

        if (is_variable_str || isVL || is_reg_ref) {
            log.trace("allocateArray(): is_variable_str={} || isVL={} || is_reg_ref={}", is_variable_str, isVL, is_reg_ref);
            data = new String[size];
            for (int i = 0; i < size; i++) {
                ((String[]) data)[i] = "";
            }
        }
        else if (tclass == HDF5Constants.H5T_INTEGER) {
            log.trace("allocateArray(): class.H5T_INTEGER={}", tclass);
            if (tsize == 1) {
                data = new byte[size];
            }
            else if (tsize == 2) {
                data = new short[size];
            }
            else if (tsize == 4) {
                data = new int[size];
            }
            else if (tsize == 8) {
                data = new long[size];
            }
        }
        else if (tclass == HDF5Constants.H5T_ENUM) {
            log.trace("allocateArray(): class.H5T_ENUM={}", tclass);
            long superTid = -1;
            try {
                superTid = H5.H5Tget_super(tid);
                data = allocateArray(superTid, size);
            }
            catch (Exception ex) {
                log.debug("allocateArray(): H5T_ENUM class enum data type information failure: ", ex);
            }
            finally {
                try {
                    H5.H5Tclose(superTid);
                }
                catch (Exception ex) {
                    log.debug("allocateArray(): H5T_ENUM H5Tclose(superTid {}) failure: ", superTid, ex);
                }
            }
        }
        else if (tclass == HDF5Constants.H5T_COMPOUND) {
            log.trace("allocateArray(): class.H5T_COMPOUND={}", tclass);
            return new byte[(int) (size * tsize)];
        }
        else if (tclass == HDF5Constants.H5T_FLOAT) {
            log.trace("allocateArray(): class.H5T_FLOAT={}", tclass);
            if (tsize == 4) {
                data = new float[size];
            }
            else if (tsize == 8) {
                data = new double[size];
            }
        }
        else if ((tclass == HDF5Constants.H5T_STRING) || (tclass == HDF5Constants.H5T_REFERENCE)) {
            log.trace("allocateArray(): class.H5T_STRING || H5T_REFERENCE={}", tclass);
            data = new byte[(int) (size * tsize)];
        }
        else if (tclass == HDF5Constants.H5T_ARRAY) {
            // use the base datatype to define the array
            long superTid = -1;
            try {
                int mn = H5.H5Tget_array_ndims(tid);
                long[] marray = new long[mn];
                H5.H5Tget_array_dims(tid, marray);
                int asize = 1;
                for (int j = 0; j < mn; j++) {
                    asize *= marray[j];
                }
                log.trace("allocateArray(): class.H5T_ARRAY={} : members={} : asize={}", tclass, mn, asize);

                superTid = H5.H5Tget_super(tid);
                data = allocateArray(superTid, size * asize);
            }
            catch (Exception ex) {
                log.debug("allocateArray(): H5T_ARRAY class failure: ", ex);
            }
            finally {
                try {
                    H5.H5Tclose(superTid);
                }
                catch (Exception ex) {
                    log.debug("allocateArray(): H5T_ARRAY H5Tclose(superTid {}) failure: ", superTid, ex);
                }
            }
        }
        else if ((tclass == HDF5Constants.H5T_OPAQUE) || (tclass == HDF5Constants.H5T_BITFIELD)) {
            log.trace("allocateArray(): class.H5T_OPAQUE || H5T_BITFIELD={}", tclass);
            data = new byte[(int) (size * tsize)];
        }
        else {
            log.debug("allocateArray(): class.????={}", tclass);
            data = null;
        }

        return data;
    }

    /**
     * Returns the size (in bytes) of a given datatype identifier.
     * <p>
     * It basically just calls H5Tget_size(tid).
     *
     * @param tid
     *            The datatype identifier.
     *
     * @return The size of the datatype in bytes.
     *
     * @see hdf.hdf5lib.H5#H5Tget_size(long)
     */
    public static final long getDatatypeSize(long tid) {
        // data type information
        long tsize = -1;

        try {
            tsize = H5.H5Tget_size(tid);
        }
        catch (Exception ex) {
            tsize = -1;
        }

        return tsize;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#getDescription()
     */
    @Override
    public String getDescription() {
        log.trace("getDescription(): start");

        if (datatypeDescription != null) {
            log.trace("getDescription(): finish");
            return datatypeDescription;
        }

        String description = null;
        long tid = -1;

        switch (datatypeClass) {
            case CLASS_CHAR:
                description = "8-bit " + (isUnsigned() ? "unsigned " : "") + "integer";
                break;
            case CLASS_INTEGER:
                description = String.valueOf(datatypeSize * 8) + "-bit " + (isUnsigned() ? "unsigned " : "") + "integer";
                break;
            case CLASS_FLOAT:
                description = String.valueOf(datatypeSize * 8) + "-bit floating-point";
                break;
            case CLASS_STRING:
                description = "String, length = " + (isVarStr() ? "variable" : datatypeSize);

                tid = createNative();
                if (tid >= 0) {
                    try {
                        String strPadType;
                        int strPad = H5.H5Tget_strpad(tid);

                        if (strPad == HDF5Constants.H5T_STR_NULLTERM)
                            strPadType = "H5T_STR_NULLTERM";
                        else if (strPad == HDF5Constants.H5T_STR_NULLPAD)
                            strPadType = "H5T_STR_NULLPAD";
                        else if (strPad == HDF5Constants.H5T_STR_SPACEPAD)
                            strPadType = "H5T_STR_SPACEPAD";
                        else
                            strPadType = null;

                        if (strPadType != null)
                            description += ", string padding = " + strPadType;
                    }
                    catch (Exception ex) {
                        log.debug("H5Tget_strpad failure: ", ex);
                    }
                    finally {
                        close(tid);
                    }
                }
                else {
                    log.debug("createNative() failure");
                }

                break;
            case CLASS_BITFIELD:
                description = String.valueOf(datatypeSize * 8) + "-bit bitfield";
                break;
            case CLASS_OPAQUE:
                description = String.valueOf(datatypeSize) + "-byte Opaque";
                break;
            case CLASS_COMPOUND:
                description = "Compound";

                if (compoundMemberTypes != null) {
                    Iterator<String> member_names = null;
                    Iterator<Datatype> member_types = compoundMemberTypes.iterator();

                    if (compoundMemberNames != null)
                        member_names = compoundMemberNames.iterator();

                    description += " {";

                    while (member_types.hasNext()) {
                        if (member_names.hasNext()) {
                            description += member_names.next() + " = ";
                        }

                        description += member_types.next().getDescription();

                        if (member_types.hasNext())
                            description += ", ";
                    }

                    description += "}";
                }

                break;
            case CLASS_REFERENCE:
                description = "Reference";

                try {
                    boolean is_reg_ref = false;

                    tid = createNative();
                    if (tid >= 0) {
                        is_reg_ref = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_DSETREG);

                        if (is_reg_ref) {
                            description = "Dataset region reference";
                        }
                        else {
                            description = "Object reference";
                        }
                    }
                }
                catch (Exception ex) {
                    log.debug("H5.H5Tequal failure: ", ex);
                }
                finally {
                    close(tid);
                }

                break;
            case CLASS_ENUM:
                description = String.valueOf(datatypeSize * 8) + "-bit enum";

                String members = getEnumMembersAsString();
                if (members != null)
                    description += " (" + members + ")";

                break;
            case CLASS_VLEN:
                description = "Variable-length";

                if (baseType != null) {
                    description += " of " + baseType.getDescription();
                }

                break;
            case CLASS_ARRAY:
                description = "Array";

                if (baseType != null) {
                    description += " of " + baseType.getDescription();
                }

                if (arrayDims != null) {
                    description += " [";
                    for (int i = 0; i < arrayDims.length; i++) {
                        description += arrayDims[i];
                        if (i < arrayDims.length - 1)
                            description += " x ";
                    }
                    description += "]";
                }

                break;
            default:
                description = "Unknown";
                break;
        }

        log.trace("getDescription(): finish");
        return description;
    }

    /**
     * Returns a short description of a given datatype ID.
     *
     * @param tid
     *            the HDF5 datatype identifier
     *
     * @return a string describing the data type.
     */
    public static final String getDatatypeDescription(long tid) {
        log.trace("getDatatypeDescription(): start");
        String description = "Unknown";

        // data type information
        int tclass = -1;
        long tsize = -1;
        int tsign = -1;

        try {
            tclass = H5.H5Tget_class(tid);
            tsize = H5.H5Tget_size(tid);
        }
        catch (Exception ex) {
            log.debug("getDatatypeDescription(): Unknown: ", ex);
        }

        if (tclass == HDF5Constants.H5T_INTEGER) {
            log.trace("getDatatypeDescription(): class H5T_INTEGER");
            try {
                tsign = H5.H5Tget_sign(tid);
            }
            catch (Exception ex) {
                log.debug("getDatatypeDescription(): H5Tget_sign(tid {}) failure:", tid, ex);
            }
            if (tsize == 1) {
                if (tsign == HDF5Constants.H5T_SGN_NONE) {
                    description = "8-bit unsigned integer";
                }
                else {
                    description = "8-bit integer";
                }
            }
            else if (tsize == 2) {
                if (tsign == HDF5Constants.H5T_SGN_NONE) {
                    description = "16-bit unsigned integer";
                }
                else {
                    description = "16-bit integer";
                }
            }
            else if (tsize == 4) {
                if (tsign == HDF5Constants.H5T_SGN_NONE) {
                    description = "32-bit unsigned integer";
                }
                else {
                    description = "32-bit integer";
                }
            }
            else if (tsize == 8) {
                if (tsign == HDF5Constants.H5T_SGN_NONE) {
                    description = "64-bit unsigned integer";
                }
                else {
                    description = "64-bit integer";
                }
            }
        }
        else if (tclass == HDF5Constants.H5T_FLOAT) {
            log.trace("getDatatypeDescription(): class H5T_FLOAT");
            if (tsize == 4) {
                description = "32-bit floating-point";
            }
            else if (tsize == 8) {
                description = "64-bit floating-point";
            }
        }
        else if (tclass == HDF5Constants.H5T_STRING) {
            log.trace("getDatatypeDescription(): class H5T_STRING");
            try {
                if (H5.H5Tis_variable_str(tid)) {
                    description = "String, length = variable";
                }
                else {
                    String strPadType;
                    int strPad = H5.H5Tget_strpad(tid);

                    if (strPad == HDF5Constants.H5T_STR_NULLTERM)
                        strPadType = "H5T_STR_NULLTERM";
                    else if (strPad == HDF5Constants.H5T_STR_NULLPAD)
                        strPadType = "H5T_STR_NULLPAD";
                    else if (strPad == HDF5Constants.H5T_STR_SPACEPAD)
                        strPadType = "H5T_STR_SPACEPAD";
                    else
                        strPadType = null;

                    description = "String, length = " + H5.H5Tget_size(tid);

                    if (strPadType != null) description += ", string padding = " + strPadType;
                }
            }
            catch (Exception ex) {
                description = "String";
            }
        }
        else if (tclass == HDF5Constants.H5T_REFERENCE) {
            log.trace("getDatatypeDescription(): class H5T_REFERENCE");
            boolean is_reg_ref = false;
            try {
                is_reg_ref = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_DSETREG);
            }
            catch (Exception ex) {
                log.debug("getDatatypeDescription(): H5T_STD_REF_DSETREG: ", ex);
            }

            if (is_reg_ref) {
                description = "Dataset region reference";
            }
            else {
                description = "Object reference";
            }
        }
        else if (tclass == HDF5Constants.H5T_BITFIELD) {
            log.trace("getDatatypeDescription(): class H5T_BITFIELD");
            description = "Bitfield";
        }
        else if (tclass == HDF5Constants.H5T_ENUM) {
            log.trace("getDatatypeDescription(): class H5T_ENUM");
            byte[] evalue = new byte[(int) tsize];
            String enames = " ( ";
            try {
                int n = H5.H5Tget_nmembers(tid);
                for (int i = 0; i < n; i++) {
                    H5.H5Tget_member_value(tid, i, evalue);
                    enames += H5.H5Tget_member_name(tid, i);
                    enames += "=";
                    if (tsize == 1) {
                        description = "8-bit enum";
                        enames += (HDFNativeData.byteToByte(evalue[0]))[0];
                    }
                    else if (tsize == 2) {
                        description = "16-bit enum";
                        enames += (HDFNativeData.byteToShort(evalue))[0];
                    }
                    else if (tsize == 4) {
                        description = "32-bit enum";
                        enames += (HDFNativeData.byteToInt(evalue))[0];
                    }
                    else if (tsize == 8) {
                        description = "64-bit enum";
                        enames += (HDFNativeData.byteToLong(evalue))[0];
                    }
                    if(i < n-1)
                        enames += " ";
                }
                enames += ")";
                description += enames;
            }
            catch (Exception ex) {
                log.debug("getDatatypeDescription(): H5T_ENUM: ", ex);
            }
        }
        else if (tclass == HDF5Constants.H5T_ARRAY) {
            log.trace("getDatatypeDescription(): class H5T_ARRAY");
            description = "Array of ";
            // use the base datatype to define the array
            long tmptid = -1;
            try {
                tmptid = H5.H5Tget_super(tid);
                description += getDatatypeDescription(tmptid);
                int ndims = H5.H5Tget_array_ndims(tid);
                long adims[] = new long[ndims];
                try {
                    H5.H5Tget_array_dims(tid, adims);
                }
                catch (Exception ex) {
                    log.debug("getDatatypeDescription(): H5T_ARRAY dims: ", ex);
                }

                description += " (" + adims[0];
                for (int j = 1; j < ndims; j++)
                    description += "x" + adims[j];
                description += ")";
            }
            catch (Exception ex) {
                log.debug("getDatatypeDescription(): H5T_ARRAY: ", ex);
            }
            finally {
                try {
                    H5.H5Tclose(tmptid);
                }
                catch (Exception ex) {
                    log.debug("getDatatypeDescription(): H5T_ARRAY H5Tclose(tmptid {}) failure: ", tmptid, ex);
                }
            }
        }
        else if (tclass == HDF5Constants.H5T_COMPOUND) {
            log.trace("getDatatypeDescription(): class H5T_COMPOUND");
            description = "Compound ";
            try {
                description += "{";
                int n = H5.H5Tget_nmembers(tid);
                long mtid = -1;

                for (int i = 0; i < n; i++) {
                    mtid = H5.H5Tget_member_type(tid, i);
                    if (i > 0)
                        description += ",\n";
                    description += getDatatypeDescription(mtid);
                    try {
                        H5.H5Tclose(mtid);
                    }
                    catch (Exception ex2) {
                        log.debug("getDatatypeDescription(): H5T_COMPOUND H5Tclose(mtid {}) failure: ", mtid, ex2);
                    }
                    mtid = -1;
                }
                description += "}";
            }
            catch (Exception ex) {
                log.debug("getDatatypeDescription(): H5T_COMPOUND: ", ex);
            }
        }
        else if (tclass == HDF5Constants.H5T_VLEN) {
            log.trace("getDatatypeDescription(): class H5T_VLEN");
            long tmptid = -1;
            try {
                tmptid = H5.H5Tget_super(tid);
                description = "Variable-length of " + getDatatypeDescription(tmptid);
            }
            catch (Exception ex) {
                description = "Variable-length";
            }
            finally {
                try {
                    H5.H5Tclose(tmptid);
                }
                catch (Exception ex) {
                    log.debug("getDatatypeDescription(): H5T_VLEN H5Tclose(tmptid {}) failure: ", tmptid, ex);
                }
            }
        }
        else if (tclass == HDF5Constants.H5T_OPAQUE) {
            log.trace("getDatatypeDescription(): class H5T_OPAQUE");
            description = "Opaque";
        }
        else {
            description = "Unknown";
        }

        log.trace("getDatatypeDescription(): finish");
        return description;
    }

    /**
     * Checks if a datatype specified by the identifier is an unsigned integer.
     *
     * @param tid
     *            the datatype ID to be checked.
     *
     * @return true is the datatype is an unsigned integer; otherwise returns false.
     */
    public static final boolean isUnsigned(long tid) {
        boolean unsigned = false;

        if (tid >= 0) {
            try {
                int tclass = H5.H5Tget_class(tid);
                log.trace("isUnsigned(): tclass = {}", tclass);
                if (tclass != HDF5Constants.H5T_FLOAT && tclass != HDF5Constants.H5T_STRING
                        && tclass != HDF5Constants.H5T_REFERENCE && tclass != HDF5Constants.H5T_BITFIELD
                        && tclass != HDF5Constants.H5T_OPAQUE && tclass != HDF5Constants.H5T_VLEN
                        && tclass != HDF5Constants.H5T_COMPOUND && tclass != HDF5Constants.H5T_ARRAY) {
                    int tsign = H5.H5Tget_sign(tid);
                    if (tsign == HDF5Constants.H5T_SGN_NONE) {
                        unsigned = true;
                    }
                    else {
                        log.trace("isUnsigned(): not unsigned");
                    }
                }
                else {
                    log.trace("isUnsigned(): tclass not integer type");
                }
            }
            catch (Exception ex) {
                log.debug("isUnsigned(): Datatype {} failure", tid, ex);
                unsigned = false;
            }
        }
        else {
            log.trace("isUnsigned(): not a valid datatype");
        }

        return unsigned;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#getMetadata()
     */
    @Override
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
        // load attributes first
        if (attributeList == null) {
            int indxType = fileFormat.getIndexType(null);
            int order = fileFormat.getIndexOrder(null);

            if (attrPropList.length > 0) {
                indxType = attrPropList[0];
                if (attrPropList.length > 1) {
                    order = attrPropList[1];
                }
            }

            try {
                attributeList = H5File.getAttribute(this, indxType, order);
            }
            catch (Exception ex) {
                log.debug("getMetadata(): H5File.getAttribute failure: ", ex);
            }
        } // if (attributeList == null)

        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): H5File.linkTargetObjName failure: ", ex);
        }

        log.trace("getMetadata(): finish");
        return attributeList;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#writeMetadata(java.lang.Object)
     */
    @Override
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
     * @see hdf.object.Datatype#removeMetadata(java.lang.Object)
     */
    @Override
    public void removeMetadata(Object info) throws HDF5Exception {
        log.trace("removeMetadata(): start");

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("removeMetadata(): Object not an attribute");
            log.trace("removeMetadata(): finish");
            return;
        }

        Attribute attr = (Attribute) info;
        long tid = open();
        try {
            H5.H5Adelete(tid, attr.getName());
            List<Attribute> attrList = getMetadata();
            attrList.remove(attr);
            nAttributes = attributeList.size();
        }
        catch (Exception ex) {
            log.debug("removeMetadata(): ", ex);
        }
        finally {
            close(tid);
        }
        log.trace("removeMetadata(): finish");
    }

    @Override
    public void setName(String newName) throws Exception {
        H5File.renameObject(this, newName);
        super.setName(newName);
    }

    @Override
    public boolean isText() {
        return (datatypeClass == Datatype.CLASS_STRING);
    }

}
