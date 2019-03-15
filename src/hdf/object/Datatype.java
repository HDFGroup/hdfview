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

package hdf.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Datatype is an abstract class that defines datatype characteristics and APIs for a data type.
 * <p>
 * A datatype has four basic characteristics: class, size, byte order and sign. These
 * characteristics are defined in the
 * <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>.
 * <p>
 * These characteristics apply to all the sub-classes. The sub-classes may have different ways to
 * describe a datatype. We here define the <strong> native datatype</strong> to the datatype used by
 * the sub-class. For example, H5Datatype uses a datatype identifier (hid_t) to specify a datatype.
 * NC2Datatype uses ucar.nc2.DataType object to describe its datatype. "Native" here is different
 * from the "native" definition in the HDF5 library.
 * <p>
 * Two functions, createNative() and fromNative(), are defined to convert the general
 * characteristics to/from the native datatype. Sub-classes must implement these functions so that
 * the conversion will be done correctly. The values of the CLASS member are not identical to HDF5
 * values for a datatype class.
 * <p>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public abstract class Datatype extends HObject implements MetaDataContainer {

    private static final long serialVersionUID = -581324710549963177L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Datatype.class);

    /**
     * The default definition for datatype size, order, and sign.
     */
    public static final int NATIVE = -1;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_NO_CLASS = -1;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_INTEGER = 0;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_FLOAT = 1;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_CHAR = 2;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_STRING = 3;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_BITFIELD = 4;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_OPAQUE = 5;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_COMPOUND = 6;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_REFERENCE = 7;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_ENUM = 8;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_VLEN = 9;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_ARRAY = 10;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_TIME = 11;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int ORDER_LE = 0;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int ORDER_BE = 1;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int ORDER_VAX = 2;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int ORDER_NONE = 3;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int SIGN_NONE = 0;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int SIGN_2 = 1;

    /**
     * See <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int NSGN = 2;

    protected String datatypeDescription = null;

    /**
     * The class of the datatype.
     */
    protected int datatypeClass;

    /**
     * The size (in bytes) of the datatype.
     */
    protected long datatypeSize;

    /**
     * The byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, and
     * ORDER_VAX.
     */
    protected int datatypeOrder;

    /**
     * The sign of the datatype.
     */
    protected int datatypeSign;

    /**
     * The base datatype of this datatype (null if this datatype is atomic).
     */
    protected Datatype baseType;

    /**
     * The dimensions of the ARRAY element of an ARRAY datatype.
     */
    protected long[] arrayDims;

    /**
     * Determines whether this datatype is a variable-length type.
     */
    protected boolean isVLEN = false;
    protected boolean isVariableStr = false;

    /**
     * The (name, value) pairs of enum members.
     */
    protected Map<String, String> enumMembers;

    /**
     * The list of names of members of a compound Datatype.
     */
    protected List<String> compoundMemberNames;

    /**
     * The list of types of members of a compound Datatype.
     */
    protected List<Datatype> compoundMemberTypes;

    /**
     * The list of offsets of members of a compound Datatype.
     */
    protected List<Long> compoundMemberOffsets;

    /**
     * Constructs a named datatype with a given file, name and path.
     *
     * @param theFile
     *            the HDF file.
     * @param typeName
     *            the name of the datatype, e.g "12-bit Integer".
     * @param typePath
     *            the full group path of the datatype, e.g. "/datatypes/".
     */
    public Datatype(FileFormat theFile, String typeName, String typePath) {
        this(theFile, typeName, typePath, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #Datatype(FileFormat, String, String)}
     *
     * @param theFile
     *            the HDF file.
     * @param typeName
     *            the name of the datatype, e.g "12-bit Integer".
     * @param typePath
     *            the full group path of the datatype, e.g. "/datatypes/".
     * @param oid
     *            the oidof the datatype.
     */
    @Deprecated
    public Datatype(FileFormat theFile, String typeName, String typePath, long[] oid) {
        super(theFile, typeName, typePath, oid);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few examples of Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * Datatype type = new Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * Datatype type = new Dataype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);
     * <li>to create native float<br>
     * Datatype type = new Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
     * <li>to create 64-bit double<br>
     * Datatype type = new Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4.
     *            Valid values are NATIVE or a positive value.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX,
     *            ORDER_NONE and NATIVE.
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     *
     * @throws Exception
     *            if there is an error
     */
    public Datatype(int tclass, int tsize, int torder, int tsign) throws Exception {
        this(tclass, tsize, torder, tsign, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few examples of Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * Datatype type = new Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * Datatype type = new Dataype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);
     * <li>to create native float<br>
     * Datatype type = new Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
     * <li>to create 64-bit double<br>
     * Datatype type = new Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and
     *            etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer,
     *            the size is 4.
     *            Valid values are NATIVE or a positive value.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE,
     *            ORDER_BE, ORDER_VAX, ORDER_NONE and NATIVE.
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     * @param tbase
     *            the base datatype of the new datatype
     *
* @throws Exception
     *            if there is an error
     */
    public Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase) throws Exception {
        this(tclass, tsize, torder, tsign, tbase, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few examples of Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * Datatype type = new Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * Datatype type = new Dataype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);
     * <li>to create native float<br>
     * Datatype type = new Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
     * <li>to create 64-bit double<br>
     * Datatype type = new Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4.
     *            Valid values are NATIVE or a positive value.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX,
     *            ORDER_NONE and NATIVE.
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and NATIVE.
     * @param tbase
     *            the base datatype of the new datatype
     * @param pbase
     *            the parent datatype of the new datatype
     *
* @throws Exception
     *            if there is an error
     */
    public Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase, Datatype pbase) throws Exception {
        if ((tsize == 0) || (tsize < 0 && tsize != NATIVE))
            throw new Exception("invalid datatype size - " + tsize);
        if ((torder != ORDER_LE) && (torder != ORDER_BE) && (torder != ORDER_VAX)
                && (torder != ORDER_NONE) && (torder != NATIVE))
            throw new Exception("invalid datatype order - " + torder);
        if ((tsign != SIGN_NONE) && (tsign != SIGN_2) && (tsign != NATIVE))
            throw new Exception("invalid datatype sign - " + tsign);

        datatypeClass = tclass;
        datatypeSize = tsize;
        datatypeOrder = torder;
        datatypeSign = tsign;
        enumMembers = null;
        baseType = tbase;
        arrayDims = null;
        isVariableStr = (datatypeClass == Datatype.CLASS_STRING) && (tsize < 0);
        isVLEN = (datatypeClass == Datatype.CLASS_VLEN) || isVariableStr;

        compoundMemberNames = new ArrayList<>();
        compoundMemberTypes = new ArrayList<>();
        compoundMemberOffsets = new ArrayList<>();

        log.trace("datatypeClass={} datatypeSize={} datatypeOrder={} datatypeSign={} baseType={}",
                datatypeClass, datatypeSize, datatypeOrder, datatypeSign, baseType);
    }

    /**
     * Constructs a Datatype with a given native datatype identifier.
     * <p>
     * For example, if the datatype identifier is a 32-bit unsigned integer created from HDF5,
     *
     * <pre>
     * long tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * Datatype dtype = new Datatype(tid);
     * </pre>
     *
     * will construct a datatype equivalent to new Datatype(CLASS_INTEGER, 4, NATIVE, SIGN_NONE);
     *
     * @see #fromNative(long tid)
     * @param tid
     *            the native datatype identifier.
     *
* @throws Exception
     *            if there is an error
     */
    public Datatype(long tid) throws Exception {
        this(tid, null);
    }

    /**
     * Constructs a Datatype with a given native datatype identifier.
     * <p>
     * For example, if the datatype identifier is a 32-bit unsigned integer created from HDF5,
     *
     * <pre>
     * long tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * Datatype dtype = new Datatype(tid);
     * </pre>
     *
     * will construct a datatype equivalent to new Datatype(CLASS_INTEGER, 4, NATIVE, SIGN_NONE);
     *
     * @see #fromNative(long tid)
     * @param tid
     *            the native datatype identifier.
     * @param pbase
     *            the parent datatype of the new datatype
     *
* @throws Exception
     *            if there is an error
     */
    public Datatype(long tid, Datatype pbase) throws Exception {
        this(CLASS_NO_CLASS, NATIVE, NATIVE, NATIVE, null, pbase);
    }

    /**
     * Opens access to this named datatype. Sub-classes must replace this default implementation. For
     * example, in H5Datatype, open() function H5.H5Topen(loc_id, name) to get the datatype identifier.
     *
     * @return the datatype identifier if successful; otherwise returns negative value.
     */
    @Override
    public long open() {
        return -1;
    }

    /**
     * Closes a datatype identifier.
     * <p>
     * Sub-classes must replace this default implementation.
     *
     * @param id
     *            the datatype identifier to close.
     */
    @Override
    public abstract void close(long id);

    /**
     * Returns the class of the datatype. Valid values are:
     * <ul>
     * <li>CLASS_NO_CLASS
     * <li>CLASS_INTEGER
     * <li>CLASS_FLOAT
     * <li>CLASS_CHAR
     * <li>CLASS_STRING
     * <li>CLASS_BITFIELD
     * <li>CLASS_OPAQUE
     * <li>CLASS_COMPOUND
     * <li>CLASS_REFERENCE
     * <li>CLASS_ENUM
     * <li>CLASS_VLEN
     * <li>CLASS_ARRAY
     * </ul>
     *
     * @return the class of the datatype.
     */
    public int getDatatypeClass() {
        return datatypeClass;
    }

    /**
     * Returns the size of the datatype in bytes. For example, for a 32-bit
     * integer, the size is 4 (bytes).
     *
     * @return the size of the datatype.
     */
    public long getDatatypeSize() {
        return datatypeSize;
    }

    /**
     * Returns the byte order of the datatype. Valid values are
     * <ul>
     * <li>ORDER_LE
     * <li>ORDER_BE
     * <li>ORDER_VAX
     * <li>ORDER_NONE
     * </ul>
     *
     * @return the byte order of the datatype.
     */
    public int getDatatypeOrder() {
        return datatypeOrder;
    }

    /**
     * Returns the sign (SIGN_NONE, SIGN_2) of an integer datatype.
     *
     * @return the sign of the datatype.
     */
    public int getDatatypeSign() {
        return datatypeSign;
    }

    /**
     * Returns the base datatype for this datatype.
     * <p>
     * For example, in a dataset of type ARRAY of integer, the datatype of the dataset is ARRAY. The
     * datatype of the base type is integer.
     *
     * @return the datatype of the contained basetype.
     */
    public Datatype getDatatypeBase() {
        return baseType;
    }

    /**
     * Sets the (key, value) pairs of enum members for enum datatype.
     * <p>
     * For Example,
     * <dl>
     * <dt>setEnumMembers("-40=lowTemp, 90=highTemp")</dt>
     * <dd>sets the key of enum member lowTemp to -40 and highTemp to 90.</dd>
     * <dt>setEnumMembers("lowTemp, highTemp")</dt>
     * <dd>sets enum members to defaults, i.e. 0=lowTemp and 1=highTemp</dd>
     * <dt>setEnumMembers("10=lowTemp, highTemp")</dt>
     * <dd>sets enum member lowTemp to 10 and highTemp to 11.</dd>
     * </dl>
     *
     * @param enumStr
     *            the (key, value) pairs of enum members
     */
    public final void setEnumMembers(String enumStr) {
        log.trace("setEnumMembers: is_enum enum_members={}", enumStr);
        enumMembers = new HashMap<>();
        String[] entries = enumStr.split(",");
        for (String entry : entries) {
            String[] keyValue = entry.split("=");
            enumMembers.put(keyValue[0].trim(), keyValue[1].trim());
            if (log.isTraceEnabled())
                log.trace("setEnumMembers: is_enum value={} name={}", keyValue[0].trim(), keyValue[1].trim());
        }
    }

    /**
     * Returns the Map&lt;String,String&gt; pairs of enum members for enum datatype.
     *
     * @return enumStr Map&lt;String,String%gt; pairs of enum members
     */
    public final Map<String, String> getEnumMembers() {
        if (enumMembers == null) {
            enumMembers = new HashMap<>();
            enumMembers.put("1", "0");
            enumMembers.put("2", "1");
        }

        return enumMembers;
    }

    /**
     * Returns the HashMap pairs of enum members for enum datatype.
     * <p>
     * For Example,
     * <dl>
     * <dt>getEnumMembersAsString()</dt>
     * <dd>returns "10=lowTemp, 40=highTemp"</dd>
     * </dl>
     *
     * @return enumStr the (key, value) pairs of enum members
     */
    @SuppressWarnings("rawtypes")
    public final String getEnumMembersAsString() {
        if (enumMembers == null) {
            enumMembers = new HashMap<>();
            enumMembers.put("1", "0");
            enumMembers.put("2", "1");
        }

        StringBuilder enumStr = new StringBuilder();
        Iterator<Entry<String, String>> entries = enumMembers.entrySet().iterator();
        int i = enumMembers.size();
        while (entries.hasNext()) {
            Entry thisEntry = entries.next();
            enumStr.append((String) thisEntry.getKey())
                   .append("=")
                   .append((String) thisEntry.getValue());

            i--;
            if (i > 0)
                enumStr.append(", ");
        }
        return enumStr.toString();
    }

    /**
     * Returns the dimensions of an Array Datatype.
     *
     * @return dims the dimensions of the Array Datatype
     */
    public final long[] getArrayDims() {
        return arrayDims;
    }

    public final List<String> getCompoundMemberNames() {
        return compoundMemberNames;
    }

    public final List<Datatype> getCompoundMemberTypes() {
        return compoundMemberTypes;
    }

    /**
     * Converts the datatype object to a native datatype.
     *
     * Subclasses must implement it so that this datatype will be converted accordingly. Use close() to
     * close the native identifier; otherwise, the datatype will be left open.
     * <p>
     * For example, a HDF5 datatype created from<br>
     *
     * <pre>
     * H5Dataype dtype = new H5Datatype(CLASS_INTEGER, 4, NATIVE, SIGN_NONE);
     * int tid = dtype.createNative();
     * </pre>
     *
     * The "tid" will be the HDF5 datatype id of a 64-bit unsigned integer, which is equivalent to
     *
     * <pre>
     * int tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * </pre>
     *
     * @return the identifier of the native datatype.
     */
    public abstract long createNative();

    /**
     * Set datatype characteristics (class, size, byte order and sign) from a given datatype identifier.
     * <p>
     * Sub-classes must implement it so that this datatype will be converted accordingly.
     * <p>
     * For example, if the type identifier is a 64-bit unsigned integer created from HDF5,
     *
     * <pre>
     * H5Datatype dtype = new H5Datatype();
     * dtype.fromNative(HDF5Constants.H5T_NATIVE_UNINT32);
     * </pre>
     *
     * Where dtype is equivalent to <br>
     * new H5Datatype(CLASS_INTEGER, 4, NATIVE, SIGN_NONE);
     *
     * @param nativeID
     *            the datatype identifier.
     */
    public abstract void fromNative(long nativeID);

    /**
     * Returns a short text description of this datatype.
     *
     * @return a short text description of this datatype
     */
    public String getDescription() {
        log.trace("getDescription(): start");

        if (datatypeDescription != null) {
            log.trace("getDescription(): finish");
            return datatypeDescription;
        }

        StringBuilder description = new StringBuilder();

        switch (datatypeClass) {
            case CLASS_CHAR:
                description.append("8-bit ").append((isUnsigned() ? "unsigned " : "")).append("integer");
                break;
            case CLASS_INTEGER:
                if (datatypeSize == NATIVE)
                    description.append("native ").append((isUnsigned() ? "unsigned " : "")).append("integer");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit ")
                            .append((isUnsigned() ? "unsigned " : "")).append("integer");
                break;
            case CLASS_FLOAT:
                if (datatypeSize == NATIVE)
                    description.append("native floating-point");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit floating-point");
                break;
            case CLASS_STRING:
                description.append("String");
                break;
            case CLASS_REFERENCE:
                description.append("Object reference");
                break;
            case CLASS_OPAQUE:
                if (datatypeSize == NATIVE)
                    description.append("native opaque");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit opaque");
                break;
            case CLASS_BITFIELD:
                if (datatypeSize == NATIVE)
                    description.append("native bitfield");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit bitfield");
                break;
            case CLASS_ENUM:
                if (datatypeSize == NATIVE)
                    description.append("native enum");
                else
                    description.append(String.valueOf(datatypeSize * 8)).append("-bit enum");
                break;
            case CLASS_ARRAY:
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

                break;
            case CLASS_COMPOUND:
                description.append("Compound");
                break;
            case CLASS_VLEN:
                description.append("Variable-length");
                break;
            default:
                description.append("Unknown");
                break;
        }

        if (baseType != null) {
            description.append(" of " + baseType.getDescription());
        }

        log.trace("getDescription(): finish");
        return description.toString();
    }

    /**
     * Checks if this datatype is unsigned.
     *
     * @return true if the datatype is unsigned;
     *         otherwise, returns false.
     */
    public boolean isUnsigned() {
        if (baseType != null)
            return baseType.isUnsigned();
        else {
            if (isCompound()) {
                if ((compoundMemberTypes != null) && !compoundMemberTypes.isEmpty()) {
                    boolean allMembersUnsigned = true;

                    Iterator<Datatype> cmpdTypeListIT = compoundMemberTypes.iterator();
                    while (cmpdTypeListIT.hasNext()) {
                        Datatype next = cmpdTypeListIT.next();

                        allMembersUnsigned = allMembersUnsigned && next.isUnsigned();
                    }

                    return allMembersUnsigned;
                }
                else {
                    log.debug("isUnsigned(): compoundMemberTypes is null");
                    return false;
                }
            }
            else {
                return (datatypeSign == Datatype.SIGN_NONE);
            }
        }
    }

    public abstract boolean isText();

    /**
     * Checks if this datatype is an integer type.
     *
     * @return true if the datatype is integer; false otherwise
     */
    public boolean isInteger() {
        return (datatypeClass == Datatype.CLASS_INTEGER);
    }

    /**
     * Checks if this datatype is a floating-point type.
     *
     * @return true if the datatype is floating-point; false otherwise
     */
    public boolean isFloat() {
        return (datatypeClass == Datatype.CLASS_FLOAT);
    }

    /**
     * Checks if this datatype is a variable-length string type.
     *
     * @return true if the datatype is variable-length string; false otherwise
     */
    public boolean isVarStr() {
        return isVariableStr;
    }

    /**
     * Checks if this datatype is a variable-length type.
     *
     * @return true if the datatype is variable-length; false otherwise
     */
    public boolean isVLEN() {
        return isVLEN;
    }

    /**
     * Checks if this datatype is an compound type.
     *
     * @return true if the datatype is compound; false otherwise
     */
    public boolean isCompound() {
        return (datatypeClass == Datatype.CLASS_COMPOUND);
    }

    /**
     * Checks if this datatype is an array type.
     *
     * @return true if the datatype is array; false otherwise
     */
    public boolean isArray() {
        return (datatypeClass == Datatype.CLASS_ARRAY);
    }

    /**
     * Checks if this datatype is a string type.
     *
     * @return true if the datatype is string; false otherwise
     */
    public boolean isString() {
        return (datatypeClass == Datatype.CLASS_STRING);
    }

    /**
     * Checks if this datatype is a character type.
     *
     * @return true if the datatype is character; false otherwise
     */
    public boolean isChar() {
        return (datatypeClass == Datatype.CLASS_CHAR);
    }

    /**
     * Checks if this datatype is a reference type.
     *
     * @return true if the datatype is reference; false otherwise
     */
    public boolean isRef() {
        return (datatypeClass == Datatype.CLASS_REFERENCE);
    }

    /**
     * Checks if this datatype is a enum type.
     *
     * @return true if the datatype is enum; false otherwise
     */
    public boolean isEnum() {
        return (datatypeClass == Datatype.CLASS_ENUM);
    }

    /**
     * Checks if this datatype is a opaque type.
     *
     * @return true if the datatype is opaque; false otherwise
     */
    public boolean isOpaque() {
        return (datatypeClass == Datatype.CLASS_OPAQUE);
    }

    /**
     * Checks if this datatype is a bitfield type.
     *
     * @return true if the datatype is bitfield; false otherwise
     */
    public boolean isBitField() {
        return (datatypeClass == Datatype.CLASS_BITFIELD);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#getMetadata()
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List getMetadata() throws Exception {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#writeMetadata(java.lang.Object)
     */
    @Override
    public void writeMetadata(Object info) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement Datatype:writeMetadata.");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#removeMetadata(java.lang.Object)
     */
    @Override
    public void removeMetadata(Object info) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement Datatype:removeMetadata.");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#updateMetadata(java.lang.Object)
     */
    @Override
    public void updateMetadata(Object info) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement Datatype:updateMetadata.");
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
