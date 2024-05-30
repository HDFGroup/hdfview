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
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Vector;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5MetaDataContainer;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFArray;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.hdf5lib.structs.H5O_token_t;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines HDF5 datatype characteristics and APIs for a data type. This class provides several
 * methods to convert an HDF5 datatype identifier to a datatype object, and vice versa. A datatype object is
 * described by four basic fields: datatype class, size, byte order, and sign, while an HDF5 datatype is
 * presented by a datatype identifier.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5Datatype extends Datatype {
    private static final long serialVersionUID = -750546422258749792L;

    private static final Logger log = LoggerFactory.getLogger(H5Datatype.class);

    /**
     * The metadata object for this data object. Members of the metadata are instances of Attribute.
     */
    private H5MetaDataContainer objMetadata;

    /**
     * The dimension sizes of the reference object
     */
    protected long[] refdims;

    /** the datatype is an object reference */
    private boolean isRefObj = false;

    /** the datatype is a region reference */
    private boolean isRegRef = false;

    /** the datatype is a standard reference */
    private boolean isStdRef = false;

    /** the object properties */
    private H5O_info_t objInfo;

    /**
     * The native class of the datatype.
     */
    private int nativeClass = -1;

    /** The native Precision properties of the number datatype. */
    private long nativePrecision = 0;
    /** The native Offset properties of the number datatype. */
    private int nativeOffset = -1;
    /** The native PadLSB properties of the number datatype. */
    private int nativePadLSB = -1;
    /** The native PadMSB properties of the number datatype. */
    private int nativePadMSB = -1;

    /** The native ebias properties of the float datatype. */
    private long nativeFPebias = 0;
    /** The native spos properties of the float datatype. */
    private long nativeFPspos = -1;
    /** The native epos properties of the float datatype. */
    private long nativeFPepos = -1;
    /** The native esize properties of the float datatype. */
    private long nativeFPesize = -1;
    /** The native mpos properties of the float datatype. */
    private long nativeFPmpos = -1;
    /** The native msize properties of the float datatype. */
    private long nativeFPmsize = -1;
    /** The native norm properties of the float datatype. */
    private int nativeFPnorm = -1;
    /** The native inpad properties of the float datatype. */
    private int nativeFPinpad = -1;

    /** The native padding properties of the string datatype. */
    private int nativeStrPad = -1;
    /** The native CSET properties of the string datatype. */
    private int nativeStrCSET = -1;

    /**
     * The tag for an opaque datatype.
     */
    private String opaqueTag = null;

    /**
     * Constructs an named HDF5 data type object for a given file, dataset name and group path. The datatype
     * object represents an existing named datatype in file. For example,
     *
     * <pre>
     * new H5Datatype(file, "dtype1", "/g0")
     * </pre>
     *
     * constructs a datatype object that corresponds to the dataset,"dset1", at group "/g0".
     *
     * @param theFile
     *                the file that contains the datatype.
     * @param theName
     *                the name of the dataset such as "dset1".
     * @param thePath
     *                the group path to the dataset such as "/g0/".
     */
    public H5Datatype(FileFormat theFile, String theName, String thePath)
    {
        this(theFile, theName, thePath, null);
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using {@link #H5Datatype(FileFormat, String, String)}
     * @param theFile
     *                the file that contains the datatype.
     * @param theName
     *                the name of the dataset such as "dset1".
     * @param thePath
     *                the group path to the dataset such as "/g0/".
     * @param oid
     *                the oid of the dataset.
     */
    @Deprecated
    public H5Datatype(FileFormat theFile, String theName, String thePath, long[] oid)
    {
        super(theFile, theName, thePath, oid);
        objMetadata = new H5MetaDataContainer(theFile, theName, thePath, this);

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

            long tid = HDF5Constants.H5I_INVALID_HID;
            try {
                tid = open();
            }
            catch (Exception ex) {
                log.debug("constructor H5Topen() failure");
            }
            finally {
                close(tid);
            }
        }
        else {
            this.oid = null;
            objInfo  = new H5O_info_t(-1L, null, 0, 0, 0L, 0L, 0L, 0L, 0L);
        }
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign. The following is a list of a few
     * examples of H5Datatype. <ol> <li>to create unsigned native integer<br> H5Datatype type = new
     * H5Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE); <li>to create
     * 16-bit signed integer with big endian<br> H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, 2,
     * Datatype.ORDER_BE, Datatype.NATIVE); <li>to create native float<br> H5Datatype type = new
     * H5Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE); <li>to create
     * 64-bit double<br> H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE,
     * Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *               the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *               the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4. Valid values
     * are NATIVE or a positive value. For string datatypes, -1 is also a valid value (to create a
     *               variable-length string).
     * @param torder
     *               the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX,
     * ORDER_NONE and NATIVE.
     * @param tsign
     *               the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     * @throws Exception
     *                   if there is an error
     */
    public H5Datatype(int tclass, int tsize, int torder, int tsign) throws Exception
    {
        this(tclass, tsize, torder, tsign, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign. The following is a list of a few
     * examples of H5Datatype. <ol> <li>to create unsigned native integer<br> H5Datatype type = new
     * H5Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE); <li>to create
     * 16-bit signed integer with big endian<br> H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, 2,
     * Datatype.ORDER_BE, Datatype.NATIVE); <li>to create native float<br> H5Datatype type = new
     * H5Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE); <li>to create
     * 64-bit double<br> H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE,
     * Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *               the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *               the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4. Valid values
     * are NATIVE or a positive value. For string datatypes, -1 is also a valid value (to create a
     *               variable-length string).
     * @param torder
     *               the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX,
     * ORDER_NONE and NATIVE.
     * @param tsign
     *               the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     * @param tbase
     *               the base datatype of the new datatype
     * @throws Exception
     *                   if there is an error
     */
    public H5Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase) throws Exception
    {
        this(tclass, tsize, torder, tsign, tbase, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign. The following is a list of a few
     * examples of H5Datatype. <ol> <li>to create unsigned native integer<br> H5Datatype type = new
     * H5Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE); <li>to create
     * 16-bit signed integer with big endian<br> H5Datatype type = new H5Dataype(Datatype.CLASS_INTEGER, 2,
     * Datatype.ORDER_BE, Datatype.NATIVE); <li>to create native float<br> H5Datatype type = new
     * H5Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE); <li>to create
     * 64-bit double<br> H5Datatype type = new H5Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE,
     * Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *               the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *               the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4. Valid values
     * are NATIVE or a positive value. For string datatypes, -1 is also a valid value (to create a
     *               variable-length string).
     * @param torder
     *               the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX,
     * ORDER_NONE and NATIVE.
     * @param tsign
     *               the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     * @param tbase
     *               the base datatype of the new datatype
     * @param pbase
     *               the parent datatype of the new datatype
     * @throws Exception
     *                   if there is an error
     */
    public H5Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase, Datatype pbase)
        throws Exception
    {
        super(tclass, tsize, torder, tsign, tbase, pbase);
        datatypeDescription = getDescription();
    }

    /**
     * Constructs a Datatype with a given native datatype identifier. For example, if the datatype identifier
     * is a 32-bit unsigned integer created from HDF5,
     *
     * <pre>
     * int tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * Datatype dtype = new Datatype(tid);
     * </pre>
     *
     * will construct a datatype equivalent to new Datatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE,
     * Datatype.SIGN_NONE);
     *
     * @see #fromNative(long nativeID)
     * @param theFile
     *                 the file that contains the datatype.
     * @param nativeID
     *                 the native datatype identifier.
     * @throws Exception
     *                   if there is an error
     */
    public H5Datatype(FileFormat theFile, long nativeID) throws Exception { this(theFile, nativeID, null); }

    /**
     * Constructs a Datatype with a given native datatype identifier. For example, if the datatype identifier
     * is a 32-bit unsigned integer created from HDF5,
     *
     * <pre>
     * int tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * Datatype dtype = new Datatype(tid);
     * </pre>
     *
     * will construct a datatype equivalent to new Datatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE,
     * Datatype.SIGN_NONE);
     *
     * @see #fromNative(long nativeID)
     * @param theFile
     *                 the file that contains the datatype.
     * @param nativeID
     *                 the native datatype identifier.
     * @param pbase
     *                 the parent datatype of the new datatype
     * @throws Exception
     *                   if there is an error
     */
    public H5Datatype(FileFormat theFile, long nativeID, Datatype pbase) throws Exception
    {
        super(theFile, nativeID, pbase);
        fromNative(nativeID);
        datatypeDescription = getDescription();
    }

    /**
     * Opens access to a named datatype. It calls H5.H5Topen(loc, name).
     *
     * @return the datatype identifier if successful; otherwise returns negative value.
     * @see hdf.hdf5lib.H5#H5Topen(long, String, long)
     */
    @Override
    public long open()
    {
        long tid = HDF5Constants.H5I_INVALID_HID;

        if (fileFormat != null) {
            try {
                tid = H5.H5Topen(getFID(), getFullName(), HDF5Constants.H5P_DEFAULT);
                fromNative(tid);
                log.trace("open(): tid={}", tid);
            }
            catch (HDF5Exception ex) {
                log.debug("open(): Failed to open datatype {}", getFullName(), ex);
                tid = HDF5Constants.H5I_INVALID_HID;
            }
        }

        return tid;
    }

    /**
     * Closes a datatype identifier. It calls H5.H5close(tid).
     *
     * @param tid
     *            the datatype ID to close
     */
    @Override
    public void close(long tid)
    {
        if (tid >= 0) {
            try {
                H5.H5Tclose(tid);
            }
            catch (HDF5Exception ex) {
                log.debug("close(): H5Tclose(tid {}) failure: ", tid, ex);
            }
        }
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
            long tid = open();
            if (tid > 0) {
                try {
                    objInfo = H5.H5Oget_info(tid);
                }
                catch (Exception ex) {
                    objInfo.num_attrs = 0;
                    log.debug("hasAttribute(): get object info failure: ", ex);
                }
                finally {
                    close(tid);
                }
                objMetadata.setObjectAttributeSize((int)objInfo.num_attrs);
            }
            else {
                log.debug("hasAttribute(): could not open group");
            }
        }

        log.trace("hasAttribute(): nAttributes={}", objInfo.num_attrs);
        return (objInfo.num_attrs > 0);
    }

    /**
     * Converts values in an Enumeration Datatype to names. This method searches the identified enumeration
     * datatype for the values appearing in <code>inValues</code> and returns the names corresponding to those
     * values. If a given value is not found in the enumeration datatype, the name corresponding to that value
     * will be set to <code>"ENUM ERR value"</code> in the string array that is returned. If the method fails
     * in general, null will be returned instead of a String array. An empty <code>inValues</code> parameter
     * would cause general failure.
     *
     * @param inValues
     *                 The array of enumerations values to be converted.
     * @return The string array of names if successful; otherwise return null.
     * @throws HDF5Exception
     *                       If there is an error at the HDF5 library level.
     */
    public String[] convertEnumValueToName(Object inValues) throws HDF5Exception
    {
        log.trace("convertEnumValueToName() inValues={} start", inValues);

        if (inValues == null) {
            log.debug("convertEnumValueToName() failure: in values null ");
            return null;
        }

        int inSize        = 0;
        String[] outNames = null;
        String cName      = inValues.getClass().getName();
        boolean isArray   = cName.lastIndexOf('[') >= 0;
        if (isArray)
            inSize = Array.getLength(inValues);
        else
            inSize = 1;

        if (inSize <= 0) {
            log.debug("convertEnumValueToName() failure: inSize length invalid");
            log.debug("convertEnumValueToName(): inValues={} inSize={}", inValues, inSize);
            return null;
        }

        if (enumMembers == null || enumMembers.size() <= 0) {
            log.debug("convertEnumValueToName(): no members");
            return null;
        }

        log.trace("convertEnumValueToName(): inSize={} nMembers={} enums={}", inSize, enumMembers.size(),
                  enumMembers);
        outNames = new String[inSize];
        for (int i = 0; i < inSize; i++) {
            if (isArray) {
                if (enumMembers.containsKey(String.valueOf(Array.get(inValues, i))))
                    outNames[i] = enumMembers.get(String.valueOf(Array.get(inValues, i)));
                else
                    outNames[i] = "**ENUM ERR " + Array.get(inValues, i) + "**";
            }
            else {
                if (enumMembers.containsKey(String.valueOf(inValues)))
                    outNames[i] = enumMembers.get(String.valueOf(inValues));
                else
                    outNames[i] = "**ENUM ERR " + inValues + "**";
            }
        }

        return outNames;
    }

    /**
     * Converts names in an Enumeration Datatype to values. This method searches the identified enumeration
     * datatype for the names appearing in <code>inValues</code> and returns the values corresponding to those
     * names.
     *
     * @param in
     *           The array of enumerations names to be converted.
     * @return The int array of values if successful; otherwise return null.
     * @throws HDF5Exception
     *                       If there is an error at the HDF5 library level.
     */
    public Object[] convertEnumNameToValue(String[] in) throws HDF5Exception
    {
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
        if (datatypeSize == 1)
            out = new Byte[size];
        else if (datatypeSize == 2)
            out = new Short[size];
        else if (datatypeSize == 4)
            out = new Integer[size];
        else if (datatypeSize == 8)
            out = new Long[size];
        else
            out = new Object[size];

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

    /**
     * Convert from an array of BigDecimal into an array of bytes
     *
     * @param start
     *              The position in the input array of BigDecimal to start
     * @param len
     *              The number of 'BigDecimal' to convert
     * @param data
     *              The input array of BigDecimal
     * @return an array of bytes
     */
    public byte[] bigDecimalToByte(int start, int len, BigDecimal[] data)
    {
        int ii;
        byte[] bd      = new byte[(int)datatypeSize];
        byte[] bdconv  = new byte[(int)datatypeSize];
        byte[] bdbytes = new byte[(int)datatypeSize * len];

        for (ii = 0; ii < len; ii++) {
            BigDecimal entry = data[start + ii];
            bdconv           = convertBigDecimalToByte(entry);
            /* bitsets operate assuming LE order, BigInteger/BigDecimal expect BE */
            if (datatypeOrder == ORDER_BE) {
                int k = 0;
                for (int j = (int)datatypeSize - 1; j >= 0; j--)
                    bd[k++] = bdconv[j];
            }
            else {
                try {
                    System.arraycopy(bdconv, 0, bd, 0, (int)datatypeSize);
                }
                catch (Exception err) {
                    log.trace("bigDecimalToByte(): arraycopy failure: ", err);
                }
            }
            try {
                System.arraycopy(bd, 0, bdbytes, ii * 16, 16);
            }
            catch (Exception err) {
                log.trace("bigDecimalToByte(): arraycopy failure: ", err);
            }
        }
        return bdbytes;
    }

    /**
     * Convert from a single BigDecimal object from an array of BigDecimal into an array of bytes
     *
     * @param start
     *              The position in the input array of BigDecimal to start
     * @param data
     *              The input Float
     * @return an array of bytes
     */
    public byte[] bigDecimalToByte(BigDecimal[] data, int start)
    {
        byte[] bdbytes = new byte[(int)datatypeSize];
        bdbytes        = bigDecimalToByte(start, 1, data);
        return bdbytes;
    }

    /**
     * Convert a BigDecimal to a byte array .
     *
     * @param num
     *            The BigDecimal number to convert
     * @return A byte array representing the BigDecimal.
     */
    public byte[] convertBigDecimalToByte(BigDecimal num)
    {
        BigInteger sig = new BigInteger(num.unscaledValue().toString());
        byte[] bsig    = sig.toByteArray();
        int scale      = num.scale();
        byte[] bscale =
            new byte[] {(byte)(scale >>> 24), (byte)(scale >>> 16), (byte)(scale >>> 8), (byte)(scale)};
        byte[] both = new byte[bscale.length + bsig.length];
        try {
            System.arraycopy(bscale, 0, both, 0, bscale.length);
            System.arraycopy(bsig, 0, both, bscale.length, bsig.length);
        }
        catch (Exception err) {
            log.trace("convertBigDecimalToByte(): arraycopy failure: ", err);
        }
        return both;
    }

    /**
     * Convert a range from an array of bytes into an array of BigDecimal
     *
     * @param start
     *              The position in the input array of bytes to start
     * @param len
     *              The number of 'BigDecimal' to convert
     * @param data
     *              The input array of bytes
     * @return an array of 'len' BigDecimal
     */
    public BigDecimal[] byteToBigDecimal(int start, int len, byte[] data)
    {
        int ii;
        byte[] bd            = new byte[(int)datatypeSize];
        BigDecimal[] BDarray = new BigDecimal[len];

        for (ii = 0; ii < len; ii++) {
            int rawpos = (start + ii) * (int)datatypeSize;
            /* bitsets operate assuming LE order, BigInteger/BigDecimal expect BE */
            if (datatypeOrder == ORDER_BE) {
                int k = 0;
                for (int j = (int)datatypeSize - 1; j >= 0; j--)
                    bd[k++] = data[rawpos + j];
            }
            else {
                try {
                    System.arraycopy(data, rawpos, bd, 0, (int)datatypeSize);
                }
                catch (Exception err) {
                    log.trace("byteToBigDecimal(): arraycopy failure: ", err);
                }
            }
            BDarray[ii] = convertByteToBigDecimal(bd);
        }
        return BDarray;
    }

    /**
     * Convert 4 bytes from an array of bytes into a single BigDecimal
     *
     * @param start
     *              The position in the input array of bytes to start
     * @param data
     *              The input array of bytes
     * @return The BigDecimal value of the bytes.
     */
    public BigDecimal byteToBigDecimal(byte[] data, int start)
    {
        BigDecimal[] bdval = new BigDecimal[1];
        bdval              = byteToBigDecimal(start, 1, data);
        return (bdval[0]);
    }

    /**
     * Convert byte array data to a BigDecimal.
     *
     * @param raw
     *            The byte array to convert to a BigDecimal
     * @return A BigDecimal representing the byte array.
     */
    public BigDecimal convertByteToBigDecimal(byte[] raw)
    {
        BitSet rawset = BitSet.valueOf(raw);

        boolean sign       = rawset.get(nativeOffset + (int)nativeFPspos);
        BitSet mantissaset = rawset.get(nativeOffset + (int)nativeFPmpos,
                                        nativeOffset + (int)nativeFPmpos + (int)nativeFPmsize);
        BitSet exponentset = rawset.get(nativeOffset + (int)nativeFPepos,
                                        nativeOffset + (int)nativeFPepos + (int)nativeFPesize);
        byte[] expraw      = Arrays.copyOf(exponentset.toByteArray(), (int)(nativeFPesize + 7) / 8);
        byte[] bexp        = new byte[expraw.length];
        /* bitsets operate assuming LE order, BigInteger/BigDecimal expect BE */
        if (datatypeOrder == ORDER_LE) {
            int k = 0;
            for (int j = expraw.length - 1; j >= 0; j--)
                bexp[k++] = expraw[j];
        }
        else {
            try {
                System.arraycopy(expraw, 0, bexp, 0, expraw.length);
            }
            catch (Exception err) {
                log.trace("convertByteToBigDecimal(): arraycopy failure: ", err);
            }
        }
        BigInteger bscale = new BigInteger(bexp);
        long scale        = bscale.longValue();
        scale -= nativeFPebias;
        double powscale = Math.pow(2, scale);

        byte[] manraw = Arrays.copyOf(mantissaset.toByteArray(), (int)(nativeFPmsize + 7) / 8);
        byte[] bman   = new byte[manraw.length];
        /* bitsets operate assuming LE order, BigInteger/BigDecimal expect BE */
        if (datatypeOrder == ORDER_BE) {
            int k = 0;
            for (int j = manraw.length - 1; j >= 0; j--)
                bman[k++] = manraw[j];
        }
        else {
            try {
                System.arraycopy(manraw, 0, bman, 0, manraw.length);
            }
            catch (Exception err) {
                log.trace("convertByteToBigDecimal(): arraycopy failure: ", err);
            }
        }
        BitSet manset = BitSet.valueOf(bman);

        // calculate mantissa value
        double val = 0.0;
        for (int i = 0; i < (int)nativeFPmsize; i++) {
            if (manset.get((int)nativeFPmsize - 1 - i))
                val += Math.pow(2, -(i));
        }
        if (nativeFPnorm == HDF5Constants.H5T_NORM_IMPLIED || nativeFPnorm == HDF5Constants.H5T_NORM_MSBSET)
            val += 1;
        BigDecimal sig = BigDecimal.valueOf(val);
        if (sign)
            sig.negate(MathContext.DECIMAL128);
        return sig.multiply(new BigDecimal(powscale, MathContext.DECIMAL128));
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#fromNative(int)
     */
    @Override
    public void fromNative(long tid)
    {
        log.trace("fromNative(): start: tid={}", tid);
        long tsize      = -1;
        int torder      = -1;
        boolean isChar  = false;
        boolean isUchar = false;

        if (tid < 0) {
            datatypeClass = CLASS_NO_CLASS;
        }
        else {
            try {
                nativeClass   = H5.H5Tget_class(tid);
                tsize         = H5.H5Tget_size(tid);
                isVariableStr = H5.H5Tis_variable_str(tid);
                isVLEN        = false;
                log.trace("fromNative(): tclass={}, tsize={}, torder={}, isVLEN={}", nativeClass, tsize,
                          torder, isVLEN);
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
                isChar  = (H5.H5Tequal(tid, HDF5Constants.H5T_NATIVE_CHAR) || isUchar);
                log.trace("fromNative(): tclass={}, tsize={}, torder={}, isUchar={}, isChar={}", nativeClass,
                          tsize, torder, isUchar, isChar);
            }
            catch (Exception ex) {
                log.debug("fromNative(): native char type failure: ", ex);
            }

            datatypeOrder    = HDF5Constants.H5T_ORDER_NONE;
            boolean IsAtomic = datatypeClassIsAtomic(nativeClass);
            if (IsAtomic || (nativeClass == HDF5Constants.H5T_COMPOUND)) {
                try {
                    torder        = H5.H5Tget_order(tid);
                    datatypeOrder = (torder == HDF5Constants.H5T_ORDER_BE) ? ORDER_BE : ORDER_LE;
                }
                catch (Exception ex) {
                    log.debug("fromNative(): get_order failure: ", ex);
                }
            }

            if (IsAtomic && !datatypeClassIsOpaque(nativeClass)) {
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

            log.trace(
                "fromNative(): isUchar={}, nativePrecision={}, nativeOffset={}, nativePadLSB={}, nativePadMSB={}",
                isUchar, nativePrecision, nativeOffset, nativePadLSB, nativePadMSB);

            datatypeSign = NATIVE; // default
            if (nativeClass == HDF5Constants.H5T_ARRAY) {
                long tmptid   = HDF5Constants.H5I_INVALID_HID;
                datatypeClass = CLASS_ARRAY;
                try {
                    int ndims = H5.H5Tget_array_ndims(tid);
                    arrayDims = new long[ndims];
                    H5.H5Tget_array_dims(tid, arrayDims);

                    tmptid              = H5.H5Tget_super(tid);
                    int nativeBaseClass = H5.H5Tget_class(tmptid);
                    if (nativeBaseClass == HDF5Constants.H5T_REFERENCE)
                        baseType = new H5ReferenceType(this.fileFormat, 1, tmptid);
                    else
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
                    int nMembers          = H5.H5Tget_nmembers(tid);
                    compoundMemberNames   = new Vector<>(nMembers);
                    compoundMemberTypes   = new Vector<>(nMembers);
                    compoundMemberOffsets = new Vector<>(nMembers);
                    log.trace("fromNative(): compound type nMembers={} start", nMembers);

                    for (int i = 0; i < nMembers; i++) {
                        String memberName = H5.H5Tget_member_name(tid, i);
                        log.trace("fromNative(): compound type [{}] name={} start", i, memberName);
                        long memberOffset     = H5.H5Tget_member_offset(tid, i);
                        long memberID         = HDF5Constants.H5I_INVALID_HID;
                        H5Datatype membertype = null;
                        try {
                            memberID              = H5.H5Tget_member_type(tid, i);
                            int nativeMemberClass = H5.H5Tget_class(memberID);
                            if (nativeMemberClass == HDF5Constants.H5T_REFERENCE)
                                membertype = new H5ReferenceType(this.fileFormat, 1, memberID);
                            else
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
                    int tsign    = H5.H5Tget_sign(tid);
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
                    nativeFPspos  = fields[0];
                    nativeFPepos  = fields[1];
                    nativeFPesize = fields[2];
                    nativeFPmpos  = fields[3];
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
                datatypeSign  = (isUchar) ? SIGN_NONE : SIGN_2;
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
                log.trace("fromNative(): H5T_STRING:nativeStrPad={}, nativeStrCSET={}", nativeStrPad,
                          nativeStrCSET);
            }
            else if (nativeClass == HDF5Constants.H5T_REFERENCE) {
                datatypeClass = CLASS_REFERENCE;
                log.trace("fromNative(): reference type");
                try {
                    isStdRef = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF);
                    log.trace("fromNative(): reference type is orig StdRef:{}", isStdRef);
                    if (H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF))
                        tsize = HDF5Constants.H5R_REF_BUF_SIZE;
                }
                catch (Exception ex) {
                    log.debug("fromNative(): H5T_STD_REF: ", ex);
                }
                try {
                    isRegRef = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_DSETREG);
                    log.trace("fromNative(): reference type isRegRef:{}", isRegRef);
                    if (isRegRef)
                        tsize = HDF5Constants.H5R_DSET_REG_REF_BUF_SIZE;
                }
                catch (Exception ex) {
                    log.debug("fromNative(): H5T_STD_REF_DSETREG: ", ex);
                }
                try {
                    isRefObj = H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_OBJ);
                    log.trace("fromNative(): reference type isRefObj:{}", isRefObj);
                    if (isRefObj)
                        tsize = HDF5Constants.H5R_OBJ_REF_BUF_SIZE;
                }
                catch (Exception ex) {
                    log.debug("fromNative(): H5T_STD_REF_OBJ: ", ex);
                }
            }
            else if (nativeClass == HDF5Constants.H5T_ENUM) {
                datatypeClass = CLASS_ENUM;
                long tmptid   = HDF5Constants.H5I_INVALID_HID;
                long basetid  = HDF5Constants.H5I_INVALID_HID;
                try {
                    log.trace("fromNative(): enum type");
                    basetid = H5.H5Tget_super(tid);
                    tmptid  = basetid;
                    basetid = H5.H5Tget_native_type(tmptid);
                    log.trace("fromNative(): enum type basetid={}", basetid);
                    if (basetid >= 0) {
                        baseType     = new H5Datatype(this.fileFormat, tmptid, this);
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
                    String name         = null;
                    String enumStr      = null;
                    byte[] val          = new byte[(int)tsize];
                    enumMembers         = new HashMap<>();
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
                long tmptid   = HDF5Constants.H5I_INVALID_HID;
                datatypeClass = CLASS_VLEN;
                isVLEN        = true;
                try {
                    log.trace("fromNative(): vlen type");
                    tmptid              = H5.H5Tget_super(tid);
                    int nativeBaseClass = H5.H5Tget_class(tmptid);
                    if (nativeBaseClass == HDF5Constants.H5T_REFERENCE)
                        baseType = new H5ReferenceType(this.fileFormat, 1, tmptid);
                    else
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
        if (datatypeSize == NATIVE)
            datatypeNATIVE = true;
        else
            datatypeNATIVE = false;
        log.trace("fromNative(): datatypeClass={} baseType={} datatypeSize={}", datatypeClass, baseType,
                  datatypeSize);
    }

    /**
     * Get the memory datatype identifier from the datatype file identifier.
     *
     * @param tid the datatype file identification.
     *
     * @return the memory datatype identifier if successful, and negative otherwise.
     */
    public static long toNative(long tid)
    {
        // data type information
        log.trace("toNative(): tid={} start", tid);
        long nativeID = HDF5Constants.H5I_INVALID_HID;

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
     * @see hdf.object.Datatype#createNative()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public long createNative()
    {
        long tid    = HDF5Constants.H5I_INVALID_HID;
        long tmptid = HDF5Constants.H5I_INVALID_HID;

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
            log.debug("createNative(): isNamed={} and named path={}", isNamed(), the_path);

        if (tid >= 0)
            return tid;

        log.trace("createNative(): datatypeClass={} datatypeSize={} baseType={}", datatypeClass, datatypeSize,
                  baseType);

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
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
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
                    String memberName     = null;
                    long memberOffset     = -1;

                    try {
                        memberType = (H5Datatype)compoundMemberTypes.get(i);
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

                    long memberID = HDF5Constants.H5I_INVALID_HID;
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
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
            }
            break;
        case CLASS_INTEGER:
            log.trace("createNative(): CLASS_INT of size {}", datatypeSize);

            try {
                switch ((int)datatypeSize) {
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
                        datatypeNATIVE = true;
                        log.trace("createNative(): CLASS_INT is H5T_NATIVE_INT");
                        tid          = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT);
                        datatypeSize = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);
                    }
                    else {
                        datatypeNATIVE = false;
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
                if (tid >= 0)
                    close(tid);
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
                    if (datatypeSize == NATIVE) {
                        datatypeNATIVE = true;
                        datatypeSize   = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);
                    }
                    else
                        datatypeNATIVE = false;

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
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
            }
            finally {
                close(tmptid);
            }

            break;
        case CLASS_FLOAT:
            try {
                if (datatypeSize > 8)
                    tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_LDOUBLE);
                else if (datatypeSize == 8)
                    tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_DOUBLE);
                else
                    tid = H5.H5Tcopy((datatypeSize == 4) ? HDF5Constants.H5T_NATIVE_FLOAT
                                                         : HDF5Constants.H5T_NATIVE_FLOAT16);

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
                    H5.H5Tset_fields(tid, nativeFPspos, nativeFPmpos, nativeFPesize, nativeFPmpos,
                                     nativeFPmsize);
                }
            }
            catch (Exception ex) {
                log.debug("createNative(): native floating-point datatype creation failed: ", ex);
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
            }

            break;
        case CLASS_CHAR:
            try {
                tid = H5.H5Tcopy((datatypeSign == Datatype.SIGN_NONE) ? HDF5Constants.H5T_NATIVE_UCHAR
                                                                      : HDF5Constants.H5T_NATIVE_CHAR);
            }
            catch (Exception ex) {
                log.debug("createNative(): native character datatype creation failed: ", ex);
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
            }

            break;
        case CLASS_STRING:
            try {
                tid = H5.H5Tcopy(HDF5Constants.H5T_C_S1);

                H5.H5Tset_size(tid, (isVLEN || datatypeSize < 0) ? HDF5Constants.H5T_VARIABLE : datatypeSize);

                log.trace("createNative(): isVlenStr={} nativeStrPad={} nativeStrCSET={}", isVLEN,
                          nativeStrPad, nativeStrCSET);

                H5.H5Tset_strpad(tid, (nativeStrPad >= 0) ? nativeStrPad : HDF5Constants.H5T_STR_NULLTERM);

                if (nativeStrCSET >= 0) {
                    H5.H5Tset_cset(tid, nativeStrCSET);
                }
            }
            catch (Exception ex) {
                log.debug("createNative(): native string datatype creation failed: ", ex);
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
            }

            break;
        case CLASS_REFERENCE:
            try {
                long objRefTypeSize  = H5.H5Tget_size(HDF5Constants.H5T_STD_REF_OBJ);
                long dsetRefTypeSize = H5.H5Tget_size(HDF5Constants.H5T_STD_REF_DSETREG);
                // use datatypeSize as which type to copy
                log.debug("createNative(): datatypeSize:{} ", datatypeSize);
                if (datatypeSize < 0 || datatypeSize > dsetRefTypeSize) {
                    tid = H5.H5Tcopy(HDF5Constants.H5T_STD_REF);
                    log.debug("createNative(): HDF5Constants.H5T_STD_REF");
                }
                else if (datatypeSize > objRefTypeSize) {
                    tid = H5.H5Tcopy(HDF5Constants.H5T_STD_REF_DSETREG);
                    log.debug("createNative(): HDF5Constants.H5T_STD_REF_DSETREG");
                }
                else {
                    tid = H5.H5Tcopy(HDF5Constants.H5T_STD_REF_OBJ);
                    log.debug("createNative(): HDF5Constants.H5T_STD_REF_OBJ");
                }
            }
            catch (Exception ex) {
                log.debug("createNative(): native reference datatype creation failed: ", ex);
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
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
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
            }
            finally {
                close(tmptid);
            }

            break;
        case CLASS_BITFIELD:
            log.trace("createNative(): CLASS_BITFIELD size is {}", datatypeSize);

            try {
                switch ((int)datatypeSize) {
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
                    if (datatypeSize == NATIVE) {
                        datatypeNATIVE = true;
                        datatypeSize   = 1;
                    }
                    else
                        datatypeNATIVE = false;

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
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
            }

            break;
        case CLASS_OPAQUE:
            log.trace("createNative(): CLASS_OPAQUE is {}-byte H5T_OPAQUE", datatypeSize);

            try {
                if (datatypeSize == NATIVE) {
                    datatypeNATIVE = true;
                    tid            = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_OPAQUE);
                    datatypeSize   = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_OPAQUE);
                }
                else {
                    datatypeNATIVE = false;
                    tid            = H5.H5Tcreate(HDF5Constants.H5T_OPAQUE, datatypeSize);
                }

                if (opaqueTag != null) {
                    H5.H5Tset_tag(tid, opaqueTag);
                }
            }
            catch (Exception ex) {
                log.debug("createNative(): native opaque datatype creation failed: ", ex);
                if (tid >= 0)
                    close(tid);
                tid = HDF5Constants.H5I_INVALID_HID;
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
                    Entry thisEntry = (Entry)entries.next();
                    memstr          = (String)thisEntry.getKey();
                    memname         = (String)thisEntry.getValue();

                    if (datatypeSize == 1) {
                        log.trace("createNative(): CLASS_ENUM is H5T_NATIVE_INT8");
                        Byte tval = Byte.parseByte(memstr);
                        memval    = HDFNativeData.byteToByte(tval);
                    }
                    else if (datatypeSize == 2) {
                        log.trace("createNative(): CLASS_ENUM is H5T_NATIVE_INT16");
                        Short tval = Short.parseShort(memstr);
                        memval     = HDFNativeData.shortToByte(tval);
                    }
                    else if (datatypeSize == 4) {
                        log.trace("createNative(): CLASS_ENUM is H5T_NATIVE_INT32");
                        Integer tval = Integer.parseInt(memstr);
                        memval       = HDFNativeData.intToByte(tval);
                    }
                    else if (datatypeSize == 8) {
                        log.trace("createNative(): CLASS_INT-ENUM is H5T_NATIVE_INT64");
                        Long tval = Long.parseLong(memstr);
                        memval    = HDFNativeData.longToByte(tval);
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
            tid    = H5.H5Tget_native_type(tmptid);
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
     * Allocates a one-dimensional array of byte, short, int, long, float, double, or String to store data in
     * memory. For example,
     *
     * <pre>
     * long tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT32);
     * int[] data = (int[]) H5Datatype.allocateArray(datatype, 100);
     * </pre>
     *
     * returns a 32-bit integer array of size 100.
     *
     * @param dtype
     *                  the type.
     * @param numPoints
     *                  the total number of data points of the array.
     * @return the array object if successful; otherwise, return null.
     * @throws OutOfMemoryError
     *                          If there is a failure.
     */
    public static final Object allocateArray(final H5Datatype dtype, int numPoints) throws OutOfMemoryError
    {
        log.trace("allocateArray(): start: numPoints={}", numPoints);

        Object data         = null;
        H5Datatype baseType = (H5Datatype)dtype.getDatatypeBase();
        int typeClass       = dtype.getDatatypeClass();
        long typeSize       = dtype.getDatatypeSize();

        if (numPoints < 0) {
            log.debug("allocateArray(): numPoints < 0");
            return null;
        }

        // Scalar members have dimensionality zero, i.e. size =0
        // what can we do about it, set the size to 1
        if (numPoints == 0)
            numPoints = 1;

        log.trace("allocateArray(): tclass={} : tsize={}", typeClass, typeSize);

        if (dtype.isVarStr()) {
            log.trace("allocateArray(): is_variable_str={}", dtype.isVarStr());

            data = new String[numPoints];
            for (int i = 0; i < numPoints; i++)
                ((String[])data)[i] = "";
        }
        else if (typeClass == HDF5Constants.H5T_INTEGER) {
            log.trace("allocateArray(): class H5T_INTEGER");
            if (typeSize == NATIVE)
                typeSize = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);

            switch ((int)typeSize) {
            case 1:
                data = new byte[numPoints];
                break;
            case 2:
                data = new short[numPoints];
                break;
            case 4:
                data = new int[numPoints];
                break;
            case 8:
                data = new long[numPoints];
                break;
            default:
                break;
            }
        }
        else if (typeClass == HDF5Constants.H5T_ENUM) {
            log.trace("allocateArray(): class H5T_ENUM");

            if (baseType != null)
                data = H5Datatype.allocateArray(baseType, numPoints);
            else {
                if (typeSize == NATIVE)
                    typeSize = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);
                data = new byte[(int)(numPoints * typeSize)];
            }
        }
        else if (typeClass == HDF5Constants.H5T_COMPOUND) {
            log.trace("allocateArray(): class H5T_COMPOUND");

            data = new ArrayList<>(dtype.getCompoundMemberTypes().size());
        }
        else if (typeClass == HDF5Constants.H5T_FLOAT) {
            log.trace("allocateArray(): class H5T_FLOAT");
            if (typeSize == NATIVE)
                typeSize = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_FLOAT);

            switch ((int)typeSize) {
            case 2:
                data = new short[numPoints];
                break;
            case 4:
                data = new float[numPoints];
                break;
            case 8:
                data = new double[numPoints];
                break;
            case 16:
                data = new byte[numPoints * 16];
                break;
            default:
                break;
            }
        }
        else if ((typeClass == HDF5Constants.H5T_STRING) || (typeClass == HDF5Constants.H5T_REFERENCE)) {
            log.trace("allocateArray(): class H5T_STRING || H5T_REFERENCE");

            data = new byte[(int)(numPoints * typeSize)];
        }
        else if (dtype.isVLEN()) {
            log.trace("allocateArray(): isVLEN");

            data = new ArrayList[numPoints];
            for (int j = 0; j < numPoints; j++)
                ((ArrayList[])data)[j] = new ArrayList<byte[]>();
            // if (baseType != null)
            // ((ArrayList<>)data).add(H5Datatype.allocateArray(baseType, numPoints));
        }
        else if (typeClass == HDF5Constants.H5T_ARRAY) {
            log.trace("allocateArray(): class H5T_ARRAY");

            try {
                log.trace("allocateArray(): ArrayRank={}", dtype.getArrayDims().length);

                // Use the base datatype to define the array
                long[] arrayDims = dtype.getArrayDims();
                int asize        = numPoints;
                for (int j = 0; j < arrayDims.length; j++) {
                    log.trace("allocateArray(): Array dims[{}]={}", j, arrayDims[j]);

                    asize *= arrayDims[j];
                }

                if (baseType != null)
                    data = H5Datatype.allocateArray(baseType, asize);
            }
            catch (Exception ex) {
                log.debug("allocateArray(): H5T_ARRAY class failure: ", ex);
            }
        }
        else if ((typeClass == HDF5Constants.H5T_OPAQUE) || (typeClass == HDF5Constants.H5T_BITFIELD)) {
            log.trace("allocateArray(): class H5T_OPAQUE || H5T_BITFIELD");
            if (typeSize == NATIVE)
                typeSize = H5.H5Tget_size(typeClass);

            data = new byte[(int)(numPoints * typeSize)];
        }
        else {
            log.debug("allocateArray(): class ???? ({})", typeClass);

            data = null;
        }

        return data;
    }

    /**
     * Returns the size (in bytes) of a given datatype identifier. It basically just calls H5Tget_size(tid).
     *
     * @param tid
     *            The datatype identifier.
     * @return The size of the datatype in bytes.
     * @see hdf.hdf5lib.H5#H5Tget_size(long)
     */
    public static final long getDatatypeSize(long tid)
    {
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
     * @see hdf.object.Datatype#getDescription()
     */
    @Override
    public String getDescription()
    {
        log.trace("getDescription(): start - isNamed={}", isNamed());

        if (datatypeDescription != null)
            return datatypeDescription;

        StringBuilder description = new StringBuilder();
        long tid                  = HDF5Constants.H5I_INVALID_HID;

        switch (datatypeClass) {
        case CLASS_CHAR:
            log.trace("getDescription(): Char");
            description.append("8-bit ").append(isUnsigned() ? "unsigned " : "").append("integer");
            break;
        case CLASS_INTEGER:
            log.trace("getDescription(): Int [{}]", datatypeNATIVE);
            if (datatypeNATIVE)
                description.append("native ").append(isUnsigned() ? "unsigned " : "").append("integer");
            else
                description.append(String.valueOf(datatypeSize * 8))
                    .append("-bit ")
                    .append(isUnsigned() ? "unsigned " : "")
                    .append("integer");
            break;
        case CLASS_FLOAT:
            log.trace("getDescription(): Float");
            if (datatypeNATIVE)
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
                    int strPad  = H5.H5Tget_strpad(tid);
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
            if (datatypeNATIVE)
                description.append("native bitfield");
            else
                description.append(String.valueOf(datatypeSize * 8)).append("-bit bitfield");
            break;
        case CLASS_OPAQUE:
            log.trace("getDescription(): Opaque");
            if (datatypeNATIVE)
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
                Iterator<String> memberNames   = null;
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
                    if (!H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF)) {
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
            if (datatypeNATIVE)
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

            if (baseType != null)
                description.append(" of ").append(baseType.getDescription());

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

            if (baseType != null)
                description.append(" of ").append(baseType.getDescription());

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
     * @return true is the datatype is an unsigned integer; otherwise returns false.
     */
    public static final boolean isUnsigned(long tid)
    {
        boolean unsigned = false;

        if (tid >= 0) {
            try {
                int tclass = H5.H5Tget_class(tid);
                log.trace("isUnsigned(): tclass = {}", tclass);
                if (tclass != HDF5Constants.H5T_FLOAT && tclass != HDF5Constants.H5T_STRING &&
                    tclass != HDF5Constants.H5T_REFERENCE && tclass != HDF5Constants.H5T_BITFIELD &&
                    tclass != HDF5Constants.H5T_OPAQUE && tclass != HDF5Constants.H5T_VLEN &&
                    tclass != HDF5Constants.H5T_COMPOUND && tclass != HDF5Constants.H5T_ARRAY) {
                    int tsign = H5.H5Tget_sign(tid);
                    if (tsign == HDF5Constants.H5T_SGN_NONE)
                        unsigned = true;
                    else
                        log.trace("isUnsigned(): not unsigned");
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

    /**
     * Removes all of the elements from metadata list. The list should be empty after this call returns.
     */
    @Override
    public void clear()
    {
        super.clear();
        objMetadata.clear();
    }

    /**
     * Retrieves the object's metadata, such as attributes, from the file. Metadata, such as attributes, is
     * stored in a List.
     *
     * @return the list of metadata objects.
     * @throws HDF5Exception
     *                       if the metadata can not be retrieved
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
     * Retrieves the object's metadata, such as attributes, from the file. Metadata, such as attributes, is
     * stored in a List.
     *
     * @param attrPropList
     *                     the list of properties to get
     * @return the list of metadata objects.
     * @throws HDF5Exception
     *                       if the metadata can not be retrieved
     */
    public List<Attribute> getMetadata(int... attrPropList) throws HDF5Exception
    {
        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
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
     * Writes a specific piece of metadata (such as an attribute) into the file. If an HDF(4&amp;5) attribute
     * exists in the file, this method updates its value. If the attribute does not exist in the file, it
     * creates the attribute in the file and attaches it to the object. It will fail to write a new attribute
     * to the object where an attribute with the same name already exists. To update the value of an existing
     * attribute in the file, one needs to get the instance of the attribute by getMetadata(), change its
     * values, then use writeMetadata() to write the value.
     *
     * @param info
     *             the metadata to write.
     * @throws Exception
     *                   if the metadata can not be written
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
     *             the metadata to delete.
     * @throws HDF5Exception
     *                       if the metadata can not be removed
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
        long tid = open();
        if (tid >= 0) {
            try {
                H5.H5Adelete(tid, attr.getAttributeName());
            }
            catch (Exception ex) {
                log.debug("removeMetadata(): ", ex);
            }
            finally {
                close(tid);
            }
        }
        else {
            log.debug("removeMetadata(): failed to open datatype");
        }
    }

    /**
     * Updates an existing piece of metadata attached to this object.
     *
     * @param info
     *             the metadata to update.
     * @throws HDF5Exception
     *                       if the metadata can not be updated
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

    @Override
    public void setFullname(String newPath, String newName) throws Exception
    {
        H5File.renameObject(this, newPath, newName);
        super.setFullname(newPath, newName);
    }

    @Override
    public boolean isText()
    {
        return (datatypeClass == Datatype.CLASS_STRING);
    }

    /**
     * Checks if this datatype is an object reference type.
     *
     * @return true if the datatype is an object reference; false otherwise
     */
    public boolean isRefObj() { return isRefObj; }

    /**
     * Checks if this datatype is a region reference type.
     *
     * @return true if the datatype is a region reference; false otherwise
     */
    public boolean isRegRef() { return isRegRef; }

    /**
     * Checks if this datatype is a standard reference type.
     *
     * @return true if the datatype is a standard reference; false otherwise
     */
    public boolean isStdRef() { return isStdRef; }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#getReferenceType()
     */
    @Override
    public long getReferenceType() throws HDF5Exception
    {
        if (isRegRef)
            return HDF5Constants.H5T_STD_REF_DSETREG;
        if (isRefObj)
            return HDF5Constants.H5T_STD_REF_OBJ;
        if (isStdRef)
            return HDF5Constants.H5T_STD_REF;
        return -1;
    }

    /**
     * Describes the dataset object description for a 1.10 reference.
     *
     * @param container
     *                  the dataset/attribute with the reference
     * @param refarr
     *                  the reference datatype data to be checked.
     *
     * @return the dataset reference object description.
     */
    public static String descReferenceObject(long container, byte[] refarr)
    {
        String region_desc = H5.H5Rget_name_string(container, HDF5Constants.H5R_OBJECT, refarr);
        region_desc += " H5O_TYPE_OBJ_REF";
        log.trace("descReferenceObject region_desc={}:", region_desc);
        return region_desc;
    }

    /**
     * Describes the dataset region description for a 1.10 reference.
     *
     * @param container
     *                  the dataset/attribute with the reference
     * @param refarr
     *                  the reference datatype data to be checked.
     *
     * @return the dataset region description.
     */
    public static String descRegionDataset(long container, byte[] refarr)
    {
        String region_desc = H5.H5Rget_name_string(container, HDF5Constants.H5R_DATASET_REGION, refarr);
        log.trace("descRegionDataset region_desc={}:", region_desc);
        long new_obj_id = HDF5Constants.H5I_INVALID_HID;
        try {
            log.trace("descRegionDataset refarr2={}:", refarr);
            new_obj_id       = H5.H5Rdereference(container, HDF5Constants.H5P_DEFAULT,
                                                 HDF5Constants.H5R_DATASET_REGION, refarr);
            long new_obj_sid = HDF5Constants.H5I_INVALID_HID;
            try {
                log.trace("descRegionDataset refarr3={}:", refarr);
                new_obj_sid = H5.H5Rget_region(container, HDF5Constants.H5R_DATASET_REGION, refarr);
                try {
                    int region_type = H5.H5Sget_select_type(new_obj_sid);
                    log.debug("descRegionDataset Reference Region Type {}", region_type);
                    long reg_ndims   = H5.H5Sget_simple_extent_ndims(new_obj_sid);
                    StringBuilder sb = new StringBuilder();
                    if (HDF5Constants.H5S_SEL_POINTS == region_type) {
                        sb.append(" REGION_TYPE POINT ");
                        long reg_npoints = H5.H5Sget_select_elem_npoints(new_obj_sid);
                        long getcoord[]  = new long[(int)(reg_ndims * reg_npoints)];
                        try {
                            H5.H5Sget_select_elem_pointlist(new_obj_sid, 0, reg_npoints, getcoord);
                        }
                        catch (Exception ex5) {
                            log.debug("descRegionDataset H5.H5Sget_select_elem_pointlist: ", ex5);
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
                        region_desc += sb.toString();
                    }
                    else if (HDF5Constants.H5S_SEL_HYPERSLABS == region_type) {
                        sb.append(" REGION_TYPE BLOCK ");
                        long reg_nblocks = H5.H5Sget_select_hyper_nblocks(new_obj_sid);
                        long getblocks[] = new long[(int)(reg_ndims * reg_nblocks) * 2];
                        try {
                            H5.H5Sget_select_hyper_blocklist(new_obj_sid, 0, reg_nblocks, getblocks);
                        }
                        catch (Exception ex5) {
                            log.debug("descRegionDataset H5.H5Sget_select_hyper_blocklist: ", ex5);
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
                        region_desc += sb.toString();
                    }
                    else
                        region_desc += " REGION_TYPE UNKNOWN";
                }
                catch (Exception ex4) {
                    log.debug("descRegionDataset Region Type", ex4);
                }
            }
            catch (Exception ex3) {
                log.debug("descRegionDataset Space Open", ex3);
            }
            finally {
                H5.H5Sclose(new_obj_sid);
            }
            log.trace("descRegionDataset finish");
        }
        catch (Exception ex2) {
            log.debug("descRegionDataset ", ex2);
        }
        finally {
            H5.H5Dclose(new_obj_id);
        }
        return region_desc;
    }

    /**
     * Gets the dataset reference type for a 1.10 reference.
     *
     * @param container the dataset/attribute with the reference
     * @param obj_type  the dataset/attribute object type
     * @param refarr    the reference datatype data to be checked.
     *
     * @return the dataset reference type.
     */
    public static int typeObjectRef(long container, int obj_type, byte[] refarr)
    {
        int ref_type    = -1;
        long new_obj_id = HDF5Constants.H5I_INVALID_HID;
        try {
            log.trace("typeObjectRef refarr2={}:", refarr);
            new_obj_id = H5.H5Rdereference(container, HDF5Constants.H5P_DEFAULT, obj_type, refarr);
            if (HDF5Constants.H5R_DATASET_REGION == obj_type) {
                long new_obj_sid = HDF5Constants.H5I_INVALID_HID;
                try {
                    log.trace("typeObjectRef refarr3={}:", refarr);
                    new_obj_sid = H5.H5Rget_region(container, HDF5Constants.H5R_DATASET_REGION, refarr);
                    try {
                        ref_type = H5.H5Sget_select_type(new_obj_sid);
                        log.debug("typeObjectRef Reference Region Type {}", ref_type);
                    }
                    catch (Exception ex4) {
                        log.debug("typeObjectRef Region Type", ex4);
                    }
                }
                catch (Exception ex3) {
                    log.debug("typeObjectRef Space Open", ex3);
                }
                finally {
                    H5.H5Sclose(new_obj_sid);
                }
            }
            else {
                H5O_info_t objInfo;

                objInfo  = H5.H5Oget_info(new_obj_id);
                ref_type = objInfo.type;
            }
            log.trace("typeObjectRef finish");
        }
        catch (Exception ex2) {
            log.debug("typeObjectRef ", ex2);
        }
        finally {
            H5.H5Dclose(new_obj_id);
        }
        return ref_type;
    }

    /**
     * Checks if a reference datatype is all zero.
     *
     * @param refarr
     *               the reference datatype data to be checked.
     * @return true is the reference datatype data is all zero; otherwise returns false.
     */
    public static boolean zeroArrayCheck(final byte[] refarr)
    {
        for (byte b : refarr) {
            if (b != 0)
                return false;
        }
        return true;
    }

    /**
     * Gets the string padding.
     *
     * @return the string padding value
     */
    public int getNativeStrPad() { return nativeStrPad; }

    /**
     * Extracts compound information into flat structure. For example, compound datatype "nest" has {nest1{a,
     * b, c}, d, e} then extractCompoundInfo() will put the names of nested compound fields into a flat list
     * as
     *
     * <pre>
     * nest.nest1.a
     * nest.nest1.b
     * nest.nest1.c
     * nest.d
     * nest.e
     * </pre>
     *
     * @param dtype
     *                      the datatype to extract compound info from
     * @param name
     *                      the name of the compound datatype
     * @param names
     *                      the list to store the member names of the compound datatype
     * @param flatListTypes
     *                      the list to store the nested member names of the compound datatype
     */
    public static void extractCompoundInfo(final H5Datatype dtype, String name, List<String> names,
                                           List<Datatype> flatListTypes)
    {
        log.trace("extractCompoundInfo(): start: name={}", name);

        if (dtype.isArray()) {
            log.trace("extractCompoundInfo(): array type - extracting compound info from base datatype");
            H5Datatype.extractCompoundInfo((H5Datatype)dtype.getDatatypeBase(), name, names, flatListTypes);
        }
        else if (dtype.isVLEN() && !dtype.isVarStr()) {
            log.trace(
                "extractCompoundInfo(): variable-length type - extracting compound info from base datatype");
            H5Datatype.extractCompoundInfo((H5Datatype)dtype.getDatatypeBase(), name, names, flatListTypes);
        }
        else if (dtype.isCompound()) {
            List<String> compoundMemberNames   = dtype.getCompoundMemberNames();
            List<Datatype> compoundMemberTypes = dtype.getCompoundMemberTypes();
            Datatype mtype                     = null;
            String mname                       = null;

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

                log.trace("extractCompoundInfo(): type={} with size={}", mtype.getDescription(),
                          mtype.getDatatypeSize());

                if (names != null) {
                    mname = name + compoundMemberNames.get(i);
                    log.trace("extractCompoundInfo(): mname={}, name={}", mname, name);
                }

                if (mtype.isCompound()) {
                    H5Datatype.extractCompoundInfo((H5Datatype)mtype, mname + CompoundDS.SEPARATOR, names,
                                                   flatListTypes);
                    log.trace("extractCompoundInfo(): continue after recursive compound");
                    continue;
                }

                if (names != null) {
                    names.add(mname);
                }

                flatListTypes.add(mtype);

                /*
                 * For ARRAY of COMPOUND and VLEN of COMPOUND types, we first add the top-level array or vlen
                 * type to the list of datatypes, and then follow that with a listing of the datatypes inside
                 * the nested compound.
                 */
                /*
                 * TODO: Don't flatten variable-length types until true variable-length support is
                 * implemented.
                 */
                if (mtype.isArray() /* || (mtype.isVLEN() && !mtype.isVarStr()) */) {
                    H5Datatype.extractCompoundInfo((H5Datatype)mtype, mname + CompoundDS.SEPARATOR, names,
                                                   flatListTypes);
                }
            }
        }
    }

    /**
     * Creates a datatype of a compound with one field. This function is needed to read/write data field by
     * field.
     *
     * @param memberName
     *                   The name of the datatype
     * @return the identifier of the compound datatype.
     * @throws HDF5Exception
     *                       If there is an error at the HDF5 library level.
     */
    public long createCompoundFieldType(String memberName) throws HDF5Exception
    {
        log.trace("createCompoundFieldType(): start member_name={}", memberName);

        long topTID  = HDF5Constants.H5I_INVALID_HID;
        long tmpTID1 = HDF5Constants.H5I_INVALID_HID;

        try {
            if (this.isArray()) {
                log.trace("createCompoundFieldType(): array datatype");

                if (baseType != null) {
                    log.trace("createCompoundFieldType(): creating compound field type from base datatype");
                    tmpTID1 = ((H5Datatype)baseType).createCompoundFieldType(memberName);
                }

                log.trace("createCompoundFieldType(): creating container array datatype");
                topTID = H5.H5Tarray_create(tmpTID1, arrayDims.length, arrayDims);
            }
            else if (this.isVLEN()) {
                log.trace("createCompoundFieldType(): variable-length datatype");

                if (baseType != null) {
                    log.trace("createCompoundFieldType(): creating compound field type from base datatype");
                    tmpTID1 = ((H5Datatype)baseType).createCompoundFieldType(memberName);
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
                     * If a compound separator character is present in the supplied string, then there is an
                     * additional level of compound nesting. We will create a compound type to hold the nested
                     * compound type.
                     */
                    insertedName = memberName.substring(0, sep);

                    log.trace("createCompoundFieldType(): member with name {} is nested inside compound",
                              insertedName);
                }

                /*
                 * Retrieve the index of the compound member by its name.
                 */
                int memberIndex = this.compoundMemberNames.indexOf(insertedName);
                if (memberIndex >= 0) {
                    H5Datatype memberType = (H5Datatype)this.compoundMemberTypes.get(memberIndex);

                    log.trace("createCompoundFieldType(): Member {} is type {} of size={} with baseType={}",
                              insertedName, memberType.getDescription(), memberType.getDatatypeSize(),
                              memberType.getDatatypeBase());

                    if (sep >= 0)
                        /*
                         * Additional compound nesting; create the nested compound type.
                         */
                        tmpTID1 = memberType.createCompoundFieldType(memberName.substring(sep + 1));
                    else
                        tmpTID1 = memberType.createNative();

                    log.trace("createCompoundFieldType(): creating container compound datatype");
                    topTID = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, datatypeSize);

                    log.trace("createCompoundFieldType(): inserting member {} into compound datatype",
                              insertedName);
                    H5.H5Tinsert(topTID, insertedName, 0, tmpTID1);

                    /*
                     * WARNING!!! This step is crucial. Without it, the compound type created might be larger
                     * than the size of the single datatype field we are inserting. Performing a read with a
                     * compound datatype of an incorrect size will corrupt JVM memory and cause strange
                     * behavior and crashes.
                     */
                    H5.H5Tpack(topTID);
                }
                else {
                    log.debug(
                        "createCompoundFieldType(): member name {} not found in compound datatype's member name list",
                        memberName);
                }
            }
        }
        catch (Exception ex) {
            log.debug("createCompoundFieldType(): creation of compound field type failed: ", ex);
            topTID = HDF5Constants.H5I_INVALID_HID;
        }
        finally {
            close(tmpTID1);
        }

        return topTID;
    }

    private boolean datatypeIsComplex(long tid)
    {
        long tclass = HDF5Constants.H5T_NO_CLASS;

        try {
            tclass = H5.H5Tget_class(tid);
            log.trace("datatypeIsComplex():{}", tclass);
        }
        catch (Exception ex) {
            log.debug("datatypeIsComplex():", ex);
        }

        boolean retVal = (tclass == HDF5Constants.H5T_COMPOUND);
        retVal |= (tclass == HDF5Constants.H5T_ENUM);
        retVal |= (tclass == HDF5Constants.H5T_VLEN);
        retVal |= (tclass == HDF5Constants.H5T_ARRAY);

        return retVal;
    }

    private boolean datatypeIsReference(long tid)
    {
        long tclass = HDF5Constants.H5T_NO_CLASS;

        try {
            tclass = H5.H5Tget_class(tid);
            log.trace("datatypeIsReference():{}", tclass);
        }
        catch (Exception ex) {
            log.debug("datatypeIsReference():", ex);
        }

        return (tclass == HDF5Constants.H5T_REFERENCE);
    }

    private boolean datatypeIsAtomic(long tid)
    {
        boolean retVal = !(datatypeIsComplex(tid) | datatypeIsReference(tid) | isRef());
        retVal |= isOpaque();
        retVal |= isBitField();

        return retVal;
    }

    private boolean datatypeClassIsComplex(long tclass)
    {
        boolean retVal = (tclass == HDF5Constants.H5T_COMPOUND);
        retVal |= (tclass == HDF5Constants.H5T_ENUM);
        retVal |= (tclass == HDF5Constants.H5T_VLEN);
        retVal |= (tclass == HDF5Constants.H5T_ARRAY);

        return retVal;
    }

    private boolean datatypeClassIsReference(long tclass) { return (tclass == HDF5Constants.H5T_REFERENCE); }

    private boolean datatypeClassIsOpaque(long tclass) { return (tclass == Datatype.CLASS_OPAQUE); }

    private boolean datatypeClassIsAtomic(long tclass)
    {
        boolean retVal = !(datatypeClassIsComplex(tclass) | datatypeClassIsReference(tclass));
        retVal |= (tclass == Datatype.CLASS_OPAQUE);
        retVal |= (tclass == Datatype.CLASS_BITFIELD);

        return retVal;
    }
}
