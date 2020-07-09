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
import java.util.ArrayList;
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
import hdf.object.CompoundDS;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5Datatype.class);

    /**
     * The list of attributes of this data object.
     */
    private List<Attribute> attributeList;

    private boolean isRefObj = false;

    private boolean isRegRef = false;

    private int nAttributes = -1;

    private H5O_info_t objInfo;

    /**
     * The native class of the datatype.
     */
    private int nativeClass = -1;

    /**
     * The native properties of the number datatype.
     */
    private long nativePrecision = 0;
    private int nativeOffset = -1;
    private int nativePadLSB = -1;
    private int nativePadMSB = -1;

    /**
     * The native properties of the float datatype.
     */
    private long nativeFPebias = 0;
    private long nativeFPspos = -1;
    private long nativeFPepos = -1;
    private long nativeFPesize = -1;
    private long nativeFPmpos = -1;
    private long nativeFPmsize = -1;
    private int nativeFPnorm = -1;
    private int nativeFPinpad = -1;

    /**
     * The native properties of the string datatype.
     */
    private int nativeStrPad = -1;
    private int nativeStrCSET = -1;

    /**
     * The tag for an opaque datatype.
     */
    private String opaqueTag = null;

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
     *            the file that contains the datatype.
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
     *            the file that contains the datatype.
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
        objInfo = new H5O_info_t(-1L, -1L, 0, 0, -1L, 0L, 0L, 0L, 0L, null, null, null);

        if (theFile != null) {
            if (oid == null) {
                // retrieve the object ID
                try {
                    byte[] refBuf = H5.H5Rcreate(theFile.getFID(), this.getFullName(), HDF5Constants.H5R_OBJECT, -1);
                    this.oid = new long[1];
                    this.oid[0] = HDFNativeData.byteToLong(refBuf, 0);
                }
                catch (Exception ex) {
                    log.debug("constructor ID {} for {} failed H5Rcreate", theFile.getFID(), this.getFullName());
                }
            }

            long tid = -1;
            try {
                tid = H5.H5Topen(theFile.getFID(), this.getFullName(), HDF5Constants.H5P_DEFAULT);
                fromNative(tid);
            }
            catch (Exception ex) {
                log.debug("constructor H5Topen() failure");
            }
            finally {
                close(tid);
            }
        }
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few examples of H5Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);
     * <li>to create native float<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
     * <li>to create 64-bit double<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4.
     *            Valid values are NATIVE or a positive value. For string datatypes, -1 is also
     *            a valid value (to create a variable-length string).
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX,
     *            ORDER_NONE and NATIVE.
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     *
     * @throws Exception
     *            if there is an error
     */
    public H5Datatype(int tclass, int tsize, int torder, int tsign) throws Exception {
        this(tclass, tsize, torder, tsign, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few examples of H5Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);
     * <li>to create native float<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
     * <li>to create 64-bit double<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4.
     *            Valid values are NATIVE or a positive value. For string datatypes, -1 is also
     *            a valid value (to create a variable-length string).
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
    public H5Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase) throws Exception {
        this(tclass, tsize, torder, tsign, tbase, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few examples of H5Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);
     * <li>to create native float<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
     * <li>to create 64-bit double<br>
     * H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer, the
     *            size is 4. Valid values are NATIVE or a positive value. For string
     *            datatypes, -1 is also a valid value (to create a variable-length
     *            string).
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
    public H5Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase, Datatype pbase) throws Exception {
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
     * will construct a datatype equivalent to new Datatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.SIGN_NONE);
     *
     * @see #fromNative(long nativeID)
     *
     * @param theFile
     *            the file that contains the datatype.
     * @param nativeID
     *            the native datatype identifier.
     *
     * @throws Exception
     *            if there is an error
     */
    public H5Datatype(FileFormat theFile, long nativeID) throws Exception {
        this(theFile, nativeID, null);
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
     * will construct a datatype equivalent to new Datatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.SIGN_NONE);
     *
     * @see #fromNative(long nativeID)
     *
     * @param theFile
     *            the file that contains the datatype.
     * @param nativeID
     *            the native datatype identifier.
     * @param pbase
     *            the parent datatype of the new datatype
     *
     * @throws Exception
     *            if there is an error
     */
    public H5Datatype(FileFormat theFile, long nativeID, Datatype pbase) throws Exception {
        super(theFile, nativeID, pbase);
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
        long tid = -1;

        if (fileFormat != null) {
            try {
                tid = H5.H5Topen(getFID(), getFullName(), HDF5Constants.H5P_DEFAULT);
            }
            catch (HDF5Exception ex) {
                tid = -1;
            }
        }

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
        if (tid >= 0) {
            try {
                H5.H5Tclose(tid);
            }
            catch (HDF5Exception ex) {
                log.debug("close(): H5Tclose(tid {}) failure: ", tid, ex);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#hasAttribute()
     */
    @Override
    public boolean hasAttribute() {
        log.trace("hasAttribute(): nAttributes={}", nAttributes);
        objInfo.num_attrs = nAttributes;

        if ((objInfo.num_attrs < 0) && (fileFormat != null)) {
            long tid = -1;
            try {
                tid = H5.H5Topen(getFID(), getFullName(), HDF5Constants.H5P_DEFAULT);
                fromNative(tid);
                objInfo = H5.H5Oget_info(tid);
            }
            catch (Exception ex) {
                objInfo.num_attrs = 0;
            }
            finally {
                close(tid);
            }
            nAttributes = (int) objInfo.num_attrs;
        }

        log.trace("hasAttribute(): objInfo.num_attrs={}", objInfo.num_attrs);

        return (objInfo.num_attrs > 0);
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

        if (inValues == null) {
            log.debug("convertEnumValueToName() failure: in values null ");
            return null;
        }

        int inSize = 0;
        String[] outNames = null;
        String cName = inValues.getClass().getName();
        boolean isArray = cName.lastIndexOf('[') >= 0;
        if (isArray) {
            inSize = Array.getLength(inValues);
        }
        else {
            inSize = 1;
        }

        if (inSize <= 0) {
            log.debug("convertEnumValueToName() failure: inSize length invalid");
            log.debug("convertEnumValueToName(): inValues={} inSize={}", inValues, inSize);
            return null;
        }

        if (enumMembers == null || enumMembers.size() <= 0) {
            log.debug("convertEnumValueToName(): no members");
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
                    outNames[i] = "**ENUM ERR " + Array.get(inValues, i) + "**";
                }
            }
            else {
                if (enumMembers.containsKey(String.valueOf(inValues))) {
                    outNames[i] = enumMembers.get(String.valueOf(inValues));
                }
                else {
                    outNames[i] = "**ENUM ERR " + inValues + "**";
                }
            }
        }

        return outNames;
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
        int size = 0;

        if (in == null) {
            log.debug("convertEnumNameToValue() failure: in values null");
            return null;
        }

        if ((size = Array.getLength(in)) <= 0) {
            log.debug("convertEnumNameToValue() failure: in size not valid");
            return null;
        }

        if (enumMembers == null || enumMembers.size() <= 0) {
            log.debug("convertEnumNameToValue(): no members");
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
        long tsize = -1;
        int torder = -1;
        boolean isChar = false;
        boolean isUchar = false;

        if (tid < 0) {
            datatypeClass = CLASS_NO_CLASS;
        }
        else {
            try {
                nativeClass = H5.H5Tget_class(tid);
                tsize = H5.H5Tget_size(tid);
                isVariableStr = H5.H5Tis_variable_str(tid);
                isVLEN = false;
                log.trace("fromNative(): tclass={}, tsize={}, torder={}, isVLEN={}", nativeClass, tsize, torder, isVLEN);
                if (H5.H5Tcommitted(tid)) {
                    isNamed = true;
                    try {
                        setFullname(null, H5.H5Iget_name(tid));
                    }
                    catch (Exception nex) {
                        log.debug("fromNative(): setName failure: {}", nex.getMessage());
                    }
                    log.trace("fromNative(): path={} name={}", this.getPath(), this.getName());
                }
                log.trace("fromNative(): isNamed={}", isNamed());
            }
            catch (Exception ex) {
                log.debug("fromNative(): failure: ", ex);
                datatypeClass = CLASS_NO_CLASS;
            }

            try {
                isUchar = H5.H5Tequal(tid, HDF5Constants.H5T_NATIVE_UCHAR);
                isChar = (H5.H5Tequal(tid, HDF5Constants.H5T_NATIVE_CHAR) || isUchar);
                log.trace("fromNative(): tclass={}, tsize={}, torder={}, isUchar={}, isChar={}", nativeClass, tsize, torder, isUchar, isChar);
            }
            catch (Exception ex) {
                log.debug("fromNative(): native char type failure: ", ex);
            }

            datatypeOrder = HDF5Constants.H5T_ORDER_NONE;
            if (datatypeIsAtomic(tid) || (nativeClass == HDF5Constants.H5T_COMPOUND)) {
                try {
                    torder = H5.H5Tget_order(tid);
                    datatypeOrder = (torder == HDF5Constants.H5T_ORDER_BE) ? ORDER_BE : ORDER_LE;
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_order failure: ", ex);
                }
            }

            if (datatypeIsAtomic(tid)) {
                try {
                    nativePrecision = H5.H5Tget_precision_long(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_precision failure: ", ex);
                }

                try {
                    nativeOffset = H5.H5Tget_offset(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_offset failure: ", ex);
                }

                try {
                    int[] pads = new int[2];
                    H5.H5Tget_pad(tid, pads);
                    nativePadLSB = pads[0];
                    nativePadMSB = pads[1];
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_pad failure: ", ex);
                }
            }

            log.trace("fromNative(): isUchar={}, nativePrecision={}, nativeOffset={}, nativePadLSB={}, nativePadMSB={}", isUchar, nativePrecision, nativeOffset, nativePadLSB,
                    nativePadMSB);

            datatypeSign = NATIVE; // default
            if (nativeClass == HDF5Constants.H5T_ARRAY) {
                long tmptid = -1;
                datatypeClass = CLASS_ARRAY;
                try {
                    int ndims = H5.H5Tget_array_ndims(tid);
                    arrayDims = new long[ndims];
                    H5.H5Tget_array_dims(tid, arrayDims);

                    tmptid = H5.H5Tget_super(tid);
                    baseType = new H5Datatype(this.fileFormat, tmptid, this);
                    if (baseType == null) {
                        log.debug("fromNative(): ARRAY datatype has null base type");
                        throw new Exception("Datatype (ARRAY) has no base datatype");
                    }

                    datatypeSign = baseType.getDatatypeSign();
                }
                catch (Exception ex) {
                    log.debug("fromNative(): array type failure: ", ex);
                }
                finally {
                    close(tmptid);
                }
            }
            else if (nativeClass == HDF5Constants.H5T_COMPOUND) {
                datatypeClass = CLASS_COMPOUND;

                try {
                    int nMembers = H5.H5Tget_nmembers(tid);
                    compoundMemberNames = new Vector<>(nMembers);
                    compoundMemberTypes = new Vector<>(nMembers);
                    compoundMemberOffsets = new Vector<>(nMembers);
                    log.trace("fromNative(): compound type nMembers={} start", nMembers);

                    for (int i = 0; i < nMembers; i++) {
                        String memberName = H5.H5Tget_member_name(tid, i);
                        log.trace("fromNative(): compound type [{}] name={} start", i, memberName);
                        long memberOffset = H5.H5Tget_member_offset(tid, i);
                        long memberID = -1;
                        H5Datatype membertype = null;
                        try {
                            memberID = H5.H5Tget_member_type(tid, i);
                            membertype = new H5Datatype(this.fileFormat, memberID, this);
                        }
                        catch (Exception ex1) {
                            log.debug("fromNative(): compound type failure: ", ex1);
                        }
                        finally {
                            close(memberID);
                        }

                        compoundMemberNames.add(i, memberName);
                        compoundMemberOffsets.add(i, memberOffset);
                        compoundMemberTypes.add(i, membertype);
                    }
                }
                catch (HDF5LibraryException ex) {
                    log.debug("fromNative(): compound type failure: ", ex);
                }
            }
            else if (nativeClass == HDF5Constants.H5T_INTEGER) {
                datatypeClass = CLASS_INTEGER;
                try {
                    log.trace("fromNative(): integer type");
                    int tsign = H5.H5Tget_sign(tid);
                    datatypeSign = (tsign == HDF5Constants.H5T_SGN_NONE) ? SIGN_NONE : SIGN_2;
                }
                catch (Exception ex) {
                    log.debug("fromNative(): int type failure: ", ex);
                }
            }
            else if (nativeClass == HDF5Constants.H5T_FLOAT) {
                datatypeClass = CLASS_FLOAT;
                try {
                    nativeFPebias = H5.H5Tget_ebias_long(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_ebias failure: ", ex);
                }
                try {
                    long[] fields = new long[5];
                    H5.H5Tget_fields(tid, fields);
                    nativeFPspos = fields[0];
                    nativeFPepos = fields[1];
                    nativeFPesize = fields[2];
                    nativeFPmpos = fields[3];
                    nativeFPmsize = fields[4];
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_fields failure: ", ex);
                }
                try {
                    nativeFPnorm = H5.H5Tget_norm(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_norm failure: ", ex);
                }
                try {
                    nativeFPinpad = H5.H5Tget_inpad(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_inpad failure: ", ex);
                }
            }
            else if (isChar) {
                datatypeClass = CLASS_CHAR;
                datatypeSign = (isUchar) ? SIGN_NONE : SIGN_2;
                log.trace("fromNative(): CLASS_CHAR:datatypeSign={}", datatypeSign);
            }
            else if (nativeClass == HDF5Constants.H5T_STRING) {
                datatypeClass = CLASS_STRING;
                try {
                    isVLEN = H5.H5Tdetect_class(tid, HDF5Constants.H5T_VLEN) || isVariableStr;
                    log.trace("fromNative(): H5T_STRING:var str type={}", isVLEN);
                    nativeStrPad = H5.H5Tget_strpad(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): var str type failure: ", ex);
                }
                try {
                    nativeStrCSET = H5.H5Tget_cset(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): H5T_STRING:get_cset failure: ", ex);
                }
                log.trace("fromNative(): H5T_STRING:nativeStrPad={}, nativeStrCSET={}", nativeStrPad, nativeStrCSET);
            }
            else if (nativeClass == HDF5Constants.H5T_REFERENCE) {
                datatypeClass = CLASS_REFERENCE;
                log.trace("fromNative(): reference type");
                try {
                    isRegRef = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_DSETREG);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): H5T_STD_REF_DSETREG: ", ex);
                }
                try {
                    isRefObj = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_OBJ);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): H5T_STD_REF_OBJ: ", ex);
                }
            }
            else if (nativeClass == HDF5Constants.H5T_ENUM) {
                datatypeClass = CLASS_ENUM;
                long tmptid = -1;
                long basetid = -1;
                try {
                    log.trace("fromNative(): enum type");
                    basetid = H5.H5Tget_super(tid);
                    tmptid = basetid;
                    basetid = H5.H5Tget_native_type(tmptid);
                    log.trace("fromNative(): enum type basetid={}", basetid);
                    if (basetid >= 0) {
                        baseType = new H5Datatype(this.fileFormat, tmptid, this);
                        datatypeSign = baseType.getDatatypeSign();
                    }
                }
                catch (Exception ex) {
                    log.debug("fromNative(): enum type failure: ", ex);
                }
                finally {
                    close(tmptid);
                    close(basetid);
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
            else if (nativeClass == HDF5Constants.H5T_VLEN) {
                long tmptid = -1;
                datatypeClass = CLASS_VLEN;
                isVLEN = true;
                try {
                    log.trace("fromNative(): vlen type");
                    tmptid = H5.H5Tget_super(tid);
                    baseType = new H5Datatype(this.fileFormat, tmptid, this);
                    if (baseType == null) {
                        log.debug("fromNative(): VLEN datatype has null base type");
                        throw new Exception("Datatype (VLEN) has no base datatype");
                    }

                    datatypeSign = baseType.getDatatypeSign();
                }
                catch (Exception ex) {
                    log.debug("fromNative(): vlen type failure: ", ex);
                }
                finally {
                    close(tmptid);
                }
            }
            else if (nativeClass == HDF5Constants.H5T_BITFIELD) {
                datatypeClass = CLASS_BITFIELD;
            }
            else if (nativeClass == HDF5Constants.H5T_OPAQUE) {
                datatypeClass = CLASS_OPAQUE;

                try {
                    opaqueTag = H5.H5Tget_tag(tid);
                }
                catch (Exception ex) {
                    log.debug("fromNative(): opaque type tag retrieval failed: ", ex);
                    opaqueTag = null;
                }
            }
            else {
                log.debug("fromNative(): datatypeClass is unknown");
            }

            datatypeSize = (isVLEN && !isVariableStr) ? HDF5Constants.H5T_VL_T : tsize;
        }
        log.trace("fromNative(): datatypeClass={} baseType={} datatypeSize={}", datatypeClass, baseType, datatypeSize);
    }

    /**
     * @param tid
     *            the datatype identification disk.
     *
     * @return the memory datatype identifier if successful, and negative otherwise.
     */
    public static long toNative(long tid) {
        // data type information
        log.trace("toNative(): tid={} start", tid);
        long nativeID = -1;

        try {
            nativeID = H5.H5Tget_native_type(tid);
        }
        catch (Exception ex) {
            log.debug("toNative(): H5Tget_native_type(tid {}) failure: ", tid, ex);
        }

        try {
            if (H5.H5Tis_variable_str(tid))
                H5.H5Tset_size(nativeID, HDF5Constants.H5T_VARIABLE);
        }
        catch (Exception ex) {
            log.debug("toNative(): var str type size failure: ", ex);
        }

        return nativeID;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#createNative()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public long createNative() {
        long tid = -1;
        long tmptid = -1;

        String the_path = getFullName();
        // isNamed == true should have non-null fileFormat
        if (isNamed()) {
            try {
                tid = H5.H5Topen(getFID(), the_path, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                log.debug("createNative(): name {} H5Topen failure: ", the_path, ex);
            }
        }
        else
            log.debug("createNative(): isNamed but named path={}", the_path);

        if (tid >= 0) {
            return tid;
        }

        log.trace("createNative(): datatypeClass={} datatypeSize={} baseType={}", datatypeClass, datatypeSize, baseType);

        switch (datatypeClass) {
            case CLASS_ARRAY:
                try {
                    if (baseType == null) {
                        log.debug("createNative(): CLASS_ARRAY base type is NULL");
                        break;
                    }

                    if ((tmptid = baseType.createNative()) < 0) {
                        log.debug("createNative(): failed to create native datatype for ARRAY base datatype");
                        break;
                    }

                    tid = H5.H5Tarray_create(tmptid, arrayDims.length, arrayDims);
                }
                catch (Exception ex) {
                    log.debug("createNative(): native array datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }
                finally {
                    close(tmptid);
                }

                break;
            case CLASS_COMPOUND:
                try {
                    tid = H5.H5Tcreate(CLASS_COMPOUND, datatypeSize);

                    for (int i = 0; i < compoundMemberTypes.size(); i++) {
                        H5Datatype memberType = null;
                        String memberName = null;
                        long memberOffset = -1;

                        try {
                            memberType = (H5Datatype) compoundMemberTypes.get(i);
                        }
                        catch (Exception ex) {
                            log.debug("createNative(): get compound member[{}] type failure: ", i, ex);
                            memberType = null;
                        }

                        try {
                            memberName = compoundMemberNames.get(i);
                        }
                        catch (Exception ex) {
                            log.debug("createNative(): get compound member[{}] name failure: ", i, ex);
                            memberName = null;
                        }

                        try {
                            memberOffset = compoundMemberOffsets.get(i);
                        }
                        catch (Exception ex) {
                            log.debug("createNative(): get compound member[{}] offset failure: ", i, ex);
                            memberOffset = -1;
                        }

                        long memberID = -1;
                        try {
                            memberID = memberType.createNative();
                            log.trace("createNative(): {} member[{}] with offset={} ID={}: ", memberName, i,
                                    memberOffset, memberID);

                            H5.H5Tinsert(tid, memberName, memberOffset, memberID);
                        }
                        catch (Exception ex) {
                            log.debug("createNative(): compound type member[{}] insertion failure: ", i, ex);
                        }
                        finally {
                            close(memberID);
                        }
                    }
                }
                catch (Exception ex) {
                    log.debug("createNative(): native compound datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }
                break;
            case CLASS_INTEGER:
                log.trace("createNative(): CLASS_INT of size {}", datatypeSize);

                try {
                    switch ((int) datatypeSize) {
                        case 1:
                            log.trace("createNative(): CLASS_INT is H5T_NATIVE_INT8");
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT8);
                            break;
                        case 2:
                            log.trace("createNative(): CLASS_INT is H5T_NATIVE_INT16");
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT16);
                            break;
                        case 4:
                            log.trace("createNative(): CLASS_INT is H5T_NATIVE_INT32");
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT32);
                            break;
                        case 8:
                            log.trace("createNative(): CLASS_INT is H5T_NATIVE_INT64");
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT64);
                            break;
                        default:
                            if (datatypeSize == NATIVE) {
                                log.trace("createNative(): CLASS_INT is H5T_NATIVE_INT");
                                tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT);
                            }
                            else {
                                /* Custom sized integer */
                                tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT8);
                                H5.H5Tset_size(tid, datatypeSize);
                                H5.H5Tset_precision(tid, 8 * datatypeSize);
                            }
                            break;
                    }

                    if (datatypeOrder == Datatype.ORDER_BE) {
                        log.trace("createNative(): CLASS_INT order is H5T_ORDER_BE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_BE);
                    }
                    else if (datatypeOrder == Datatype.ORDER_LE) {
                        log.trace("createNative(): CLASS_INT order is H5T_ORDER_LE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_LE);
                    }

                    if (datatypeSign == Datatype.SIGN_NONE) {
                        log.trace("createNative(): CLASS_INT sign is H5T_SGN_NONE");
                        H5.H5Tset_sign(tid, HDF5Constants.H5T_SGN_NONE);
                    }
                }
                catch (Exception ex) {
                    log.debug("createNative(): native integer datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }

                break;
            case CLASS_ENUM:
                log.trace("createNative(): CLASS_ENUM");
                try {
                    if (baseType != null) {
                        if ((tmptid = baseType.createNative()) < 0) {
                            log.debug("createNative(): failed to create native type for ENUM base datatype");
                            break;
                        }

                        tid = H5.H5Tenum_create(tmptid);
                    }
                    else {
                        if (datatypeSize == NATIVE)
                            datatypeSize = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);

                        tid = H5.H5Tcreate(HDF5Constants.H5T_ENUM, datatypeSize);
                    }

                    if (datatypeOrder == Datatype.ORDER_BE) {
                        log.trace("createNative(): CLASS_ENUM order is H5T_ORDER_BE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_BE);
                    }
                    else if (datatypeOrder == Datatype.ORDER_LE) {
                        log.trace("createNative(): CLASS_ENUM order is H5T_ORDER_LE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_LE);
                    }

                    if (datatypeSign == Datatype.SIGN_NONE) {
                        log.trace("createNative(): CLASS_ENUM sign is H5T_SGN_NONE");
                        H5.H5Tset_sign(tid, HDF5Constants.H5T_SGN_NONE);
                    }
                }
                catch (Exception ex) {
                    log.debug("createNative(): native enum datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }
                finally {
                    close(tmptid);
                }

                break;
            case CLASS_FLOAT:
                try {
                    tid = H5.H5Tcopy((datatypeSize == 8) ? HDF5Constants.H5T_NATIVE_DOUBLE : HDF5Constants.H5T_NATIVE_FLOAT);

                    if (datatypeOrder == Datatype.ORDER_BE) {
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_BE);
                    }
                    else if (datatypeOrder == Datatype.ORDER_LE) {
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_LE);
                    }

                    if (nativeFPebias > 0) {
                        H5.H5Tset_ebias(tid, nativeFPebias);
                    }

                    if (nativeFPnorm >= 0) {
                        H5.H5Tset_norm(tid, nativeFPnorm);
                    }

                    if (nativeFPinpad >= 0) {
                        H5.H5Tset_inpad(tid, nativeFPinpad);
                    }

                    if ((nativeFPesize >= 0) && (nativeFPmsize >= 0)) {
                        H5.H5Tset_fields(tid, nativeFPspos, nativeFPmpos, nativeFPesize, nativeFPmpos, nativeFPmsize);
                    }
                }
                catch (Exception ex) {
                    log.debug("createNative(): native floating-point datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }

                break;
            case CLASS_CHAR:
                try {
                    tid = H5.H5Tcopy((datatypeSign == Datatype.SIGN_NONE) ? HDF5Constants.H5T_NATIVE_UCHAR
                            : HDF5Constants.H5T_NATIVE_CHAR);
                }
                catch (Exception ex) {
                    log.debug("createNative(): native character datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }

                break;
            case CLASS_STRING:
                try {
                    tid = H5.H5Tcopy(HDF5Constants.H5T_C_S1);

                    H5.H5Tset_size(tid, (isVLEN || datatypeSize < 0) ? HDF5Constants.H5T_VARIABLE : datatypeSize);

                    log.trace("createNative(): isVlenStr={} nativeStrPad={} nativeStrCSET={}", isVLEN, nativeStrPad,
                            nativeStrCSET);

                    H5.H5Tset_strpad(tid, (nativeStrPad >= 0) ? nativeStrPad : HDF5Constants.H5T_STR_NULLTERM);

                    if (nativeStrCSET >= 0) {
                        H5.H5Tset_cset(tid, nativeStrCSET);
                    }
                }
                catch (Exception ex) {
                    log.debug("createNative(): native string datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }

                break;
            case CLASS_REFERENCE:
                try {
                    long objRefTypeSize = H5.H5Tget_size(HDF5Constants.H5T_STD_REF_OBJ);

                    tid = H5.H5Tcopy((datatypeSize > objRefTypeSize) ? HDF5Constants.H5T_STD_REF_DSETREG
                            : HDF5Constants.H5T_STD_REF_OBJ);
                }
                catch (Exception ex) {
                    log.debug("createNative(): native reference datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }

                break;
            case CLASS_VLEN:
                try {
                    if (baseType == null) {
                        log.debug("createNative(): CLASS_VLEN base type is NULL");
                        break;
                    }

                    if ((tmptid = baseType.createNative()) < 0) {
                        log.debug("createNative(): failed to create native datatype for VLEN base datatype");
                        break;
                    }

                    tid = H5.H5Tvlen_create(tmptid);
                }
                catch (Exception ex) {
                    log.debug("createNative(): native variable-length datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }
                finally {
                    close(tmptid);
                }

                break;
            case CLASS_BITFIELD:
                log.trace("createNative(): CLASS_BITFIELD size is {}", datatypeSize);

                try {
                    switch ((int) datatypeSize) {
                        case 1:
                            log.trace("createNative(): CLASS_BITFIELD is H5T_NATIVE_B8");
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_B8);
                            break;
                        case 2:
                            log.trace("createNative(): CLASS_BITFIELD is H5T_NATIVE_B16");
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_B16);
                            break;
                        case 4:
                            log.trace("createNative(): CLASS_BITFIELD is H5T_NATIVE_B32");
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_B32);
                            break;
                        case 8:
                            log.trace("createNative(): CLASS_BITFIELD is H5T_NATIVE_B64");
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_B64);
                            break;
                        default:
                            if (datatypeSize == NATIVE)
                                datatypeSize = 1;

                            /* Custom sized bitfield */
                            tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_B8);
                            H5.H5Tset_size(tid, datatypeSize);
                            H5.H5Tset_precision(tid, 8 * datatypeSize);

                            break;
                    }

                    if (datatypeOrder == Datatype.ORDER_BE) {
                        log.trace("createNative(): CLASS_BITFIELD order is H5T_ORDER_BE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_BE);
                    }
                    else if (datatypeOrder == Datatype.ORDER_LE) {
                        log.trace("createNative(): CLASS_BITFIELD order is H5T_ORDER_LE");
                        H5.H5Tset_order(tid, HDF5Constants.H5T_ORDER_LE);
                    }
                }
                catch (Exception ex) {
                    log.debug("createNative(): native bitfield datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }

                break;
            case CLASS_OPAQUE:
                log.trace("createNative(): CLASS_OPAQUE is {}-byte H5T_OPAQUE", datatypeSize);

                try {
                    if (datatypeSize == NATIVE)
                        tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_OPAQUE);
                    else
                        tid = H5.H5Tcreate(HDF5Constants.H5T_OPAQUE, datatypeSize);

                    if (opaqueTag != null) {
                        H5.H5Tset_tag(tid, opaqueTag);
                    }
                }
                catch (Exception ex) {
                    log.debug("createNative(): native opaque datatype creation failed: ", ex);
                    if (tid >= 0) close(tid);
                    tid = -1;
                }

                break;
            default:
                log.debug("createNative(): Unknown class");
                break;
        } // (tclass)

        // set up enum members
        if ((datatypeClass == CLASS_ENUM) && (enumMembers != null)) {
            log.trace("createNative(): set up enum members");
            try {
                String memstr;
                String memname;
                byte[] memval = null;

                Iterator entries = enumMembers.entrySet().iterator();
                while (entries.hasNext()) {
                    Entry thisEntry = (Entry) entries.next();
                    memstr = (String) thisEntry.getKey();
                    memname = (String) thisEntry.getValue();

                    if (datatypeSize == 1) {
                        log.trace("createNative(): CLASS_ENUM is H5T_NATIVE_INT8");
                        Byte tval = Byte.parseByte(memstr);
                        memval = HDFNativeData.byteToByte(tval);
                    }
                    else if (datatypeSize == 2) {
                        log.trace("createNative(): CLASS_ENUM is H5T_NATIVE_INT16");
                        Short tval = Short.parseShort(memstr);
                        memval = HDFNativeData.shortToByte(tval);
                    }
                    else if (datatypeSize == 4) {
                        log.trace("createNative(): CLASS_ENUM is H5T_NATIVE_INT32");
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
        } // (datatypeClass == CLASS_ENUM)

        try {
            tmptid = tid;
            tid = H5.H5Tget_native_type(tmptid);
        }
        catch (HDF5Exception ex) {
            log.debug("createNative(): H5Tget_native_type({}) failure: ", tmptid, ex);
        }
        finally {
            close(tmptid);
        }

        return tid;
    }

    /**
     * Allocates a one-dimensional array of byte, short, int, long, float, double,
     * or String to store data in memory.
     *
     * For example,
     *
     * <pre>
     * long tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT32);
     * int[] data = (int[]) H5Datatype.allocateArray(datatype, 100);
     * </pre>
     *
     * returns a 32-bit integer array of size 100.
     *
     * @param dtype
     *            the type.
     * @param nPoints
     *            the total number of data points of the array.
     *
     * @return the array object if successful; otherwise, return null.
     *
     * @throws OutOfMemoryError
     *             If there is a failure.
     */
    public static final Object allocateArray(final H5Datatype dtype, int nPoints) throws OutOfMemoryError {
        log.trace("allocateArray(): start: nPoints={}", nPoints);

        Object data = null;
        H5Datatype baseType = (H5Datatype) dtype.getDatatypeBase();
        int typeClass = dtype.getDatatypeClass();
        long typeSize = dtype.getDatatypeSize();

        if (nPoints < 0) {
            log.debug("allocateArray(): nPoints < 0");
            return null;
        }

        // Scalar members have dimensionality zero, i.e. size =0
        // what can we do about it, set the size to 1
        if (nPoints == 0) {
            nPoints = 1;
        }

        log.trace("allocateArray(): tclass={} : tsize={}", typeClass, typeSize);

        if (dtype.isVarStr() || dtype.isVLEN() || dtype.isRegRef()) {
            log.trace("allocateArray(): is_variable_str={} || isVL={} || is_reg_ref={}", dtype.isVarStr(), dtype.isVLEN(), dtype.isRegRef());

            data = new String[nPoints];
            for (int i = 0; i < nPoints; i++) {
                ((String[]) data)[i] = "";
            }
        }
        else if (typeClass == HDF5Constants.H5T_INTEGER) {
            log.trace("allocateArray(): class H5T_INTEGER");

            switch ((int) typeSize) {
                case 1:
                    data = new byte[nPoints];
                    break;
                case 2:
                    data = new short[nPoints];
                    break;
                case 4:
                    data = new int[nPoints];
                    break;
                case 8:
                    data = new long[nPoints];
                    break;
                default:
                    break;
            }
        }
        else if (typeClass == HDF5Constants.H5T_ENUM) {
            log.trace("allocateArray(): class H5T_ENUM");

            if (baseType != null)
                data = H5Datatype.allocateArray(baseType, nPoints);
            else
                data = new byte[(int) (nPoints * typeSize)];
        }
        else if (typeClass == HDF5Constants.H5T_COMPOUND) {
            log.trace("allocateArray(): class H5T_COMPOUND");

            data = new ArrayList<>(dtype.getCompoundMemberTypes().size());
        }
        else if (typeClass == HDF5Constants.H5T_FLOAT) {
            log.trace("allocateArray(): class H5T_FLOAT");

            switch ((int) typeSize) {
                case 4:
                    data = new float[nPoints];
                    break;
                case 8:
                    data = new double[nPoints];
                    break;
                default:
                    break;
            }
        }
        else if ((typeClass == HDF5Constants.H5T_STRING) || (typeClass == HDF5Constants.H5T_REFERENCE)) {
            log.trace("allocateArray(): class H5T_STRING || H5T_REFERENCE");

            data = new byte[(int) (nPoints * typeSize)];
        }
        else if (typeClass == HDF5Constants.H5T_ARRAY) {
            log.trace("allocateArray(): class H5T_ARRAY");

            try {
                log.trace("allocateArray(): ArrayRank={}", dtype.getArrayDims().length);

                // Use the base datatype to define the array
                long[] arrayDims = dtype.getArrayDims();
                int asize = nPoints;
                for (int j = 0; j < arrayDims.length; j++) {
                    log.trace("allocateArray(): Array dims[{}]={}", j, arrayDims[j]);

                    asize *= arrayDims[j];
                }

                if (baseType != null) {
                    data = H5Datatype.allocateArray(baseType, asize);
                }
            }
            catch (Exception ex) {
                log.debug("allocateArray(): H5T_ARRAY class failure: ", ex);
            }
        }
        else if ((typeClass == HDF5Constants.H5T_OPAQUE) || (typeClass == HDF5Constants.H5T_BITFIELD)) {
            log.trace("allocateArray(): class H5T_OPAQUE || H5T_BITFIELD");

            data = new byte[(int) (nPoints * typeSize)];
        }
        else {
            log.debug("allocateArray(): class ???? ({})", typeClass);

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
        log.trace("getDescription(): start - isNamed={}", isNamed());

        if (datatypeDescription != null) {
            return datatypeDescription;
        }

        StringBuilder description = new StringBuilder();
        long tid = -1;

        switch (datatypeClass) {
            case CLASS_CHAR:
                log.trace("getDescription(): Char");
                description.append("8-bit ").append(isUnsigned() ? "unsigned " : "").append("integer");
                break;
            case CLASS_INTEGER:
                log.trace("getDescription(): Int");
                if (datatypeSize == NATIVE)
                    description.append("native ").append(isUnsigned() ? "unsigned " : "").append("integer");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit ").append(isUnsigned() ? "unsigned " : "").append("integer");
                break;
            case CLASS_FLOAT:
                log.trace("getDescription(): Float");
                if (datatypeSize == NATIVE)
                    description.append("native floating-point");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit floating-point");
                break;
            case CLASS_STRING:
                log.trace("getDescription(): String");
                description.append("String, length = ").append(isVarStr() ? "variable" : datatypeSize);

                try {
                    tid = createNative();
                    if (tid >= 0) {
                        String strPadType;
                        String strCSETType;
                        int strPad = H5.H5Tget_strpad(tid);
                        int strCSET = H5.H5Tget_cset(tid);

                        if (strPad == HDF5Constants.H5T_STR_NULLTERM)
                            strPadType = "H5T_STR_NULLTERM";
                        else if (strPad == HDF5Constants.H5T_STR_NULLPAD)
                            strPadType = "H5T_STR_NULLPAD";
                        else if (strPad == HDF5Constants.H5T_STR_SPACEPAD)
                            strPadType = "H5T_STR_SPACEPAD";
                        else
                            strPadType = null;

                        if (strPadType != null)
                            description.append(", padding = ").append(strPadType);

                        if (strCSET == HDF5Constants.H5T_CSET_ASCII)
                            strCSETType = "H5T_CSET_ASCII";
                        else if (strCSET == HDF5Constants.H5T_CSET_UTF8)
                            strCSETType = "H5T_CSET_UTF8";
                        else
                            strCSETType = null;

                        if (strCSETType != null)
                            description.append(", cset = ").append(strCSETType);
                    }
                    else {
                        log.debug("createNative() failure");
                    }
                }
                catch (Exception ex) {
                    log.debug("H5Tget_strpad failure: ", ex);
                }
                finally {
                    close(tid);
                }
                break;
            case CLASS_BITFIELD:
                log.trace("getDescription(): Bit");
                if (datatypeSize == NATIVE)
                    description.append("native bitfield");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit bitfield");
                break;
            case CLASS_OPAQUE:
                log.trace("getDescription(): Opaque");
                if (datatypeSize == NATIVE)
                    description.append("native Opaque");
                else
                    description.append(String.valueOf(datatypeSize)).append("-byte Opaque");

                if (opaqueTag != null) {
                    description.append(", tag = ").append(opaqueTag);
                }

                break;
            case CLASS_COMPOUND:
                log.trace("getDescription(): Compound");
                description.append("Compound");

                if ((compoundMemberTypes != null) && !compoundMemberTypes.isEmpty()) {
                    Iterator<String> memberNames = null;
                    Iterator<Datatype> memberTypes = compoundMemberTypes.iterator();

                    if (compoundMemberNames != null)
                        memberNames = compoundMemberNames.iterator();

                    description.append(" {");

                    while (memberTypes.hasNext()) {
                        if (memberNames != null && memberNames.hasNext()) {
                            description.append(memberNames.next()).append(" = ");
                        }

                        description.append(memberTypes.next().getDescription());

                        if (memberTypes.hasNext())
                            description.append(", ");
                    }

                    description.append("}");
                }

                break;
            case CLASS_REFERENCE:
                log.trace("getDescription(): Ref");
                description.append("Reference");

                try {
                    boolean isRegionType = false;

                    tid = createNative();
                    if (tid >= 0) {
                        isRegionType = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_DSETREG);

                        description.setLength(0);
                        if (isRegionType) {
                            description.append("Dataset region reference");
                        }
                        else {
                            description.append("Object reference");
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
                log.trace("getDescription(): Enum");
                if (datatypeSize == NATIVE)
                    description.append("native enum");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit enum");

                String members = getEnumMembersAsString();
                if (members != null)
                    description.append(" (").append(members).append(")");

                break;
            case CLASS_VLEN:
                log.trace("getDescription(): Var Len");
                description.append("Variable-length");

                if (baseType != null) {
                    description.append(" of ").append(baseType.getDescription());
                }

                break;
            case CLASS_ARRAY:
                log.trace("getDescription(): Array");
                description.append("Array");

                if (arrayDims != null) {
                    description.append(" [");
                    for (int i = 0; i < arrayDims.length; i++) {
                        description.append(arrayDims[i]);
                        if (i < arrayDims.length - 1)
                            description.append(" x ");
                    }
                    description.append("]");
                }

                if (baseType != null) {
                    description.append(" of ").append(baseType.getDescription());
                }

                break;
            default:
                description.append("Unknown");
                break;
        }
        if (isNamed())
            description.append("->").append(getFullName());

        return description.toString();
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
        } //  (attributeList == null)

        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): H5File.linkTargetObjName failure: ", ex);
        }

        return attributeList;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#writeMetadata(java.lang.Object)
     */
    @Override
    public void writeMetadata(Object info) throws Exception {

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("writeMetadata(): Object not an Attribute");
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
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#removeMetadata(java.lang.Object)
     */
    @Override
    public void removeMetadata(Object info) throws HDF5Exception {
        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("removeMetadata(): Object not an attribute");
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
    }

    @Override
    public void setName(String newName) throws Exception {
        if (newName == null)
            throw new IllegalArgumentException("The new name is NULL");

        H5File.renameObject(this, newName);
        super.setName(newName);
    }
    @Override
    public void setFullname(String newPath, String newName) throws Exception {
        H5File.renameObject(this, newPath, newName);
        super.setFullname(newPath, newName);
    }

    @Override
    public boolean isText() {
        return (datatypeClass == Datatype.CLASS_STRING);
    }

    public boolean isRefObj() {
        return isRefObj;
    }

    public boolean isRegRef() {
        return isRegRef;
    }

    public int getNativeStrPad() {
        return nativeStrPad;
    }

    /**
     * Extracts compound information into flat structure.
     * <p>
     * For example, compound datatype "nest" has {nest1{a, b, c}, d, e} then extractCompoundInfo() will
     * put the names of nested compound fields into a flat list as
     *
     * <pre>
     * nest.nest1.a
     * nest.nest1.b
     * nest.nest1.c
     * nest.d
     * nest.e
     * </pre>
     *
     *@param dtype
     *            the datatype to extract compound info from
     * @param name
     *            the name of the compound datatype
     * @param names
     *            the list to store the member names of the compound datatype
     * @param flatListTypes
     *            the list to store the nested member names of the compound datatype
     */
    public static void extractCompoundInfo(final H5Datatype dtype, String name, List<String> names, List<Datatype> flatListTypes) {
        log.trace("extractCompoundInfo(): start: name={}", name);

        if (dtype.isArray()) {
            log.trace("extractCompoundInfo(): array type - extracting compound info from base datatype");
            H5Datatype.extractCompoundInfo((H5Datatype) dtype.getDatatypeBase(), name, names, flatListTypes);
        }
        else if (dtype.isVLEN() && !dtype.isVarStr()) {
            log.trace("extractCompoundInfo(): variable-length type - extracting compound info from base datatype");
            H5Datatype.extractCompoundInfo((H5Datatype) dtype.getDatatypeBase(), name, names, flatListTypes);
        }
        else if (dtype.isCompound()) {
            List<String> compoundMemberNames = dtype.getCompoundMemberNames();
            List<Datatype> compoundMemberTypes = dtype.getCompoundMemberTypes();
            Datatype mtype = null;
            String mname = null;

            if (compoundMemberNames == null) {
                log.debug("extractCompoundInfo(): compoundMemberNames is null");
                return;
            }

            if (compoundMemberNames.isEmpty()) {
                log.debug("extractCompoundInfo(): compound datatype has no members");
                return;
            }

            log.trace("extractCompoundInfo(): nMembers={}", compoundMemberNames.size());

            for (int i = 0; i < compoundMemberNames.size(); i++) {
                log.trace("extractCompoundInfo(): member[{}]:", i);

                mtype = compoundMemberTypes.get(i);

                log.trace("extractCompoundInfo(): type={} with size={}", mtype.getDescription(), mtype.getDatatypeSize());

                if (names != null) {
                    mname = name + compoundMemberNames.get(i);
                    log.trace("extractCompoundInfo(): mname={}, name={}", mname, name);
                }

                if (mtype.isCompound()) {
                    H5Datatype.extractCompoundInfo((H5Datatype) mtype, mname + CompoundDS.SEPARATOR, names, flatListTypes);
                    log.trace("extractCompoundInfo(): continue after recursive compound");
                    continue;
                }

                if (names != null) {
                    names.add(mname);
                }

                flatListTypes.add(mtype);

                /*
                 * For ARRAY of COMPOUND and VLEN of COMPOUND types, we first add the top-level
                 * array or vlen type to the list of datatypes, and then follow that with a
                 * listing of the datatypes inside the nested compound.
                 */
                /*
                 * TODO: Don't flatten variable-length types until true variable-length support
                 * is implemented.
                 */
                if (mtype.isArray() /* || (mtype.isVLEN() && !mtype.isVarStr()) */) {
                    H5Datatype.extractCompoundInfo((H5Datatype) mtype, mname + CompoundDS.SEPARATOR, names, flatListTypes);
                }
            }
        }
    }

    /**
     * Creates a datatype of a compound with one field.
     * <p>
     * This function is needed to read/write data field by field.
     *
     * @param memberName
     *            The name of the datatype
     *
     * @return the identifier of the compound datatype.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    public long createCompoundFieldType(String memberName) throws HDF5Exception {
        log.trace("createCompoundFieldType(): start member_name={}", memberName);

        long topTID = -1;
        long tmpTID1 = -1;

        try {
            if (this.isArray()) {
                log.trace("createCompoundFieldType(): array datatype");

                if (baseType != null) {
                    log.trace("createCompoundFieldType(): creating compound field type from base datatype");
                    tmpTID1 = ((H5Datatype) baseType).createCompoundFieldType(memberName);
                }

                log.trace("createCompoundFieldType(): creating container array datatype");
                topTID = H5.H5Tarray_create(tmpTID1, arrayDims.length, arrayDims);
            }
            else if (this.isVLEN()) {
                log.trace("createCompoundFieldType(): variable-length datatype");

                if (baseType != null) {
                    log.trace("createCompoundFieldType(): creating compound field type from base datatype");
                    tmpTID1 = ((H5Datatype) baseType).createCompoundFieldType(memberName);
                }

                log.trace("createCompoundFieldType(): creating container variable-length datatype");
                topTID = H5.H5Tvlen_create(tmpTID1);
            }
            else if (this.isCompound()) {
                log.trace("createCompoundFieldType(): compound datatype");

                String insertedName = memberName;

                int sep = memberName.indexOf(CompoundDS.SEPARATOR);
                if (sep >= 0) {
                    /*
                     * If a compound separator character is present in the supplied string, then
                     * there is an additional level of compound nesting. We will create a compound
                     * type to hold the nested compound type.
                     */
                    insertedName = memberName.substring(0, sep);

                    log.trace("createCompoundFieldType(): member with name {} is nested inside compound", insertedName);
                }

                /*
                 * Retrieve the index of the compound member by its name.
                 */
                int memberIndex = this.compoundMemberNames.indexOf(insertedName);
                if (memberIndex >= 0) {
                    H5Datatype memberType = (H5Datatype) this.compoundMemberTypes.get(memberIndex);

                    log.trace("createCompoundFieldType(): Member {} is type {} of size={} with baseType={}", insertedName,
                            memberType.getDescription(), memberType.getDatatypeSize(), memberType.getDatatypeBase());

                    if (sep >= 0)
                        /*
                         * Additional compound nesting; create the nested compound type.
                         */
                        tmpTID1 = memberType.createCompoundFieldType(memberName.substring(sep + 1));
                    else
                        tmpTID1 = memberType.createNative();

                    log.trace("createCompoundFieldType(): creating container compound datatype");
                    topTID = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, datatypeSize);

                    log.trace("createCompoundFieldType(): inserting member {} into compound datatype", insertedName);
                    H5.H5Tinsert(topTID, insertedName, 0, tmpTID1);

                    /*
                     * WARNING!!! This step is crucial. Without it, the compound type created might be larger than
                     * the size of the single datatype field we are inserting. Performing a read with a compound
                     * datatype of an incorrect size will corrupt JVM memory and cause strange behavior and crashes.
                     */
                    H5.H5Tpack(topTID);
                }
                else {
                    log.debug("createCompoundFieldType(): member name {} not found in compound datatype's member name list", memberName);
                }
            }
        }
        catch (Exception ex) {
            log.debug("createCompoundFieldType(): creation of compound field type failed: ", ex);
            topTID = -1;
        }
        finally {
            close(tmpTID1);
        }

        return topTID;
    }

    private boolean datatypeIsComplex(long tid) {
        long tclass = HDF5Constants.H5T_NO_CLASS;

        try {
            tclass = H5.H5Tget_class(tid);
        }
        catch (Exception ex) {
            log.debug("datatypeIsComplex():", ex);
        }

        return (tclass == HDF5Constants.H5T_COMPOUND || tclass == HDF5Constants.H5T_ENUM || tclass == HDF5Constants.H5T_VLEN || tclass == HDF5Constants.H5T_ARRAY);
    }

    private boolean datatypeIsAtomic(long tid) {
        return !datatypeIsComplex(tid) || isOpaque() || isBitField();
    }
}
