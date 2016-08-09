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

package hdf.object;

import java.util.List;

/**
 * Datatype is an abstract class that defines datatype characteristics and APIs
 * for a data type.
 * <p>
 * A datatype has four basic characteristics: class, size, byte order and sign.
 * These characteristics are defined in the
 * <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>.
 * <p>
 * These characteristics apply to all the sub-classes. The sub-classes may have
 * different ways to describe a datatype. We here define the <strong> native
 * datatype</strong> to the datatype used by the sub-class. For example,
 * H5Datatype uses a datatype identifier (hid_t) to specify a datatype.
 * NC2Datatype uses ucar.nc2.DataType object to describe its datatype. "Native"
 * here is different from the "native" definition in the HDF5 library.
 * <p>
 * Two functions, toNative() and fromNative(), are defined to convert the
 * general characteristics to/from the native datatype. Sub-classes must implement
 * these functions so that the conversion will be done correctly.
 * The values of the CLASS member are not identical to HDF5 values for a datatype class.
 * <p>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public abstract class Datatype extends HObject {
    private static final long serialVersionUID = -581324710549963177L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Datatype.class);

    /**
     * The default definition for datatype size, order, and sign.
     */
    public static final int NATIVE = -1;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_NO_CLASS = -1;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_INTEGER = 0;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_FLOAT = 1;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_CHAR = 2;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_STRING = 3;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_BITFIELD = 4;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_OPAQUE = 5;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_COMPOUND = 6;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_REFERENCE = 7;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_ENUM = 8;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_VLEN = 9;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_ARRAY = 10;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int CLASS_TIME = 11;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int ORDER_LE = 0;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int ORDER_BE = 1;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int ORDER_VAX = 2;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int ORDER_NONE = 3;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int SIGN_NONE = 0;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int SIGN_2 = 1;

    /**
     * See <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a>
     */
    public static final int NSGN = 2;

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
     * The (name, value) pairs of enum members.
     */
    protected String enumMembers;

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
     * The list of field IDs of members of a compound Datatype.
     */
    protected List<Long> compoundMemberFieldIDs;

    /**
     * The base datatype of every element of the array (for CLASS_ARRAY
     * datatype).
     */
    protected Datatype baseType;

    /**
     * The dimensions of the ARRAY element of an ARRAY datatype.
     */
    protected long[] dims;

    /**
     * Determines whether this datatype is a variable-length type.
     */
    protected boolean isVLEN = false;


    /**
     * Constructs a named datatype with a given file, name and path.
     *
     * @param theFile
     *            the HDF file.
     * @param name
     *            the name of the datatype, e.g "12-bit Integer".
     * @param path
     *            the full group path of the datatype, e.g. "/datatypes/".
     */
    public Datatype(FileFormat theFile, String name, String path) {
        this(theFile, name, path, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #Datatype(FileFormat, String, String)}
     *
     * @param theFile
     *            the HDF file.
     * @param name
     *            the name of the datatype, e.g "12-bit Integer".
     * @param path
     *            the full group path of the datatype, e.g. "/datatypes/".
     * @param oid
     *            the oidof the datatype.
     */
    @Deprecated
    public Datatype(FileFormat theFile, String name, String path, long[] oid) {
        super(theFile, name, path, oid);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few example of H5Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, NATIVE, NATIVE,
     * SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, 2, ORDER_BE, NATIVE);
     * <li>to create native float<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, NATIVE, NATIVE, -1);
     * <li>to create 64-bit double<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, 8, NATIVE, -1);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and
     *            etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer,
     *            the size is 4.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE,
     *            ORDER_BE, ORDER_VAX and ORDER_NONE
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2
     *            and MSGN
     */
    public Datatype(int tclass, int tsize, int torder, int tsign) {
        this(tclass, tsize, torder, tsign, null);
    }

    /**
     * Constructs a Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few example of H5Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, NATIVE, NATIVE,
     * SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H5Datatype type = new H5Dataype(CLASS_INTEGER, 2, ORDER_BE, NATIVE);
     * <li>to create native float<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, NATIVE, NATIVE, -1);
     * <li>to create 64-bit double<br>
     * H5Datatype type = new H5Dataype(CLASS_FLOAT, 8, NATIVE, -1);
     * </ol>
     *
     * @param tclass
     *            the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and
     *            etc.
     * @param tsize
     *            the size of the datatype in bytes, e.g. for a 32-bit integer,
     *            the size is 4.
     * @param torder
     *            the byte order of the datatype. Valid values are ORDER_LE,
     *            ORDER_BE, ORDER_VAX and ORDER_NONE
     * @param tsign
     *            the sign of the datatype. Valid values are SIGN_NONE, SIGN_2
     *            and MSGN
     * @param tbase
     *            the base datatype of the new datatype
     */
    public Datatype(int tclass, int tsize, int torder, int tsign, Datatype tbase) {
        datatypeClass = tclass;
        datatypeSize = tsize;
        datatypeOrder = torder;
        datatypeSign = tsign;
        enumMembers = null;
        baseType = tbase;
        dims = null;
        log.trace("datatypeClass={} datatypeSize={} datatypeOrder={} datatypeSign={} baseType={}", datatypeClass, datatypeSize, datatypeOrder, datatypeSign, baseType);
    }

    /**
     * Constructs a Datatype with a given native datatype identifier.
     * <p>
     * For example, if the datatype identifier is a 32-bit unsigned integer
     * created from HDF5,
     *
     * <pre>
     * int tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * Datatype dtype = new Datatype(tid);
     * </pre>
     *
     * will construct a datatype equivalent to new Datatype(CLASS_INTEGER, 4,
     * NATIVE, SIGN_NONE);
     *
     * @see #fromNative(long tid)
     * @param tid
     *            the native datatype identifier.
     */
    public Datatype(long tid) {
        this(CLASS_NO_CLASS, NATIVE, NATIVE, NATIVE);
    }

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
     * Returns the sign (SIGN_NONE, SIGN_2 or NSGN) of an integer datatype.
     *
     * @return the sign of the datatype.
     */
    public int getDatatypeSign() {
        return datatypeSign;
    }

    /**
     * Returns the datatype of array elements for an ARRAY datatype.
     * <p>
     * For example, in a dataset of type ARRAY of integer, the datatype of the
     * dataset is ARRAY. The datatype of the base type is integer.
     *
     * @return the datatype of array elements for an ARRAY datatype.
     */
    public Datatype getBasetype() {
        return baseType;
    }

    /**
     * Sets the (name, value) pairs of enum members for enum datatype.
     * <p>
     * For Example,
     * <dl>
     * <dt>setEnumMembers("lowTemp=-40, highTemp=90")</dt>
     * <dd>sets the value of enum member lowTemp to -40 and highTemp to 90.</dd>
     * <dt>setEnumMembers("lowTemp, highTemp")</dt>
     * <dd>sets enum members to defaults, i.e. lowTemp=0 and highTemp=1</dd>
     * <dt>setEnumMembers("lowTemp=10, highTemp")</dt>
     * <dd>sets enum member lowTemp to 10 and highTemp to 11.</dd>
     * </dl>
     *
     * @param enumStr
     *            the (name, value) pairs of enum members
     */
    public final void setEnumMembers(String enumStr) {
        enumMembers = enumStr;
    }

    /**
     * Returns the "name=value" pairs of enum members for enum datatype.
     * <p>
     * For Example,
     * <dl>
     * <dt>setEnumMembers("lowTemp=-40, highTemp=90")</dt>
     * <dd>sets the value of enum member lowTemp to -40 and highTemp to 90.</dd>
     * <dt>setEnumMembers("lowTemp, highTemp")</dt>
     * <dd>sets enum members to defaults, i.e. lowTemp=0 and highTemp=1</dd>
     * <dt>setEnumMembers("lowTemp=10, highTemp")</dt>
     * <dd>sets enum member lowTemp to 10 and highTemp to 11.</dd>
     * </dl>
     *
     * @return enumStr the (name, value) pairs of enum members
     */
    public final String getEnumMembers() {
        return enumMembers;
    }

    /**
     * Returns the dimensions of an Array Datatype.
     *
     * @return dims the dimensions of the Array Datatype
     */
    public final long[] getArrayDims() {
        return dims;
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
     * Subclasses must implement it so that this datatype will be converted
     * accordingly. Use close() to close the native identifier; otherwise, the
     * datatype will be left open.
     * <p>
     * For example, a HDF5 datatype created from<br>
     *
     * <pre>
     * H5Dataype dtype = new H5Datatype(CLASS_INTEGER, 4, NATIVE, SIGN_NONE);
     * int tid = dtype.toNative();
     * </pre>
     *
     * There "tid" will be the HDF5 datatype id of a 32-bit unsigned integer,
     * which is equivalent to
     *
     * <pre>
     * int tid = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_UNINT32);
     * </pre>
     *
     * @return the identifier of the native datatype.
     */
    public abstract long toNative();

    /**
     * Set datatype characteristics (class, size, byte order and sign) from a
     * given datatype identifier.
     * <p>
     * Sub-classes must implement it so that this datatype will be converted
     * accordingly.
     * <p>
     * For example, if the type identifier is a 32-bit unsigned integer created
     * from HDF5,
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
    public String getDatatypeDescription() {
        log.trace("getDatatypeDescription(): start");

        String description = "Unknown";

        switch (datatypeClass) {
        case CLASS_INTEGER:
            if (datatypeSign == SIGN_NONE) {
                description = String.valueOf(datatypeSize * 8)
                        + "-bit unsigned integer";
            }
            else {
                description = String.valueOf(datatypeSize * 8) + "-bit integer";
            }
            break;
        case CLASS_FLOAT:
            description = String.valueOf(datatypeSize * 8)
                    + "-bit floating-point";
            break;
        case CLASS_STRING:
            description = "String";
            break;
        case CLASS_REFERENCE:
            description = "Object reference";
            break;
        case CLASS_BITFIELD:
            description = "Bitfield";
            break;
        case CLASS_ENUM:
            description = String.valueOf(datatypeSize * 8) + "-bit enum";
            break;
        case CLASS_ARRAY:
            description = "Array";
            break;
        case CLASS_COMPOUND:
            description = "Compound ";
            break;
        case CLASS_VLEN:
            description = "Variable-length";
            break;
        default:
            description = "Unknown";
            break;
        }

        log.trace("description={}", description);
        log.trace("getDatatypeDescription(): finish");
        return description;
    }

    /**
     * Checks if this datatype is an unsigned integer.
     *
     * @return true if the datatype is an unsigned integer; otherwise, returns
     *         false.
     */
    public abstract boolean isUnsigned();

    /**
     * Checks if this datatype is a variable-length type.
     *
     * @return true if the datatype is variable-length; false otherwise
     */
    public boolean isVLEN() {
        return isVLEN;
    }

    /**
     * Opens access to this named datatype. Sub-classes must replace this default
     * implementation. For example, in H5Datatype, open() function
     * H5.H5Topen(loc_id, name) to get the datatype identifier.
     *
     * @return the datatype identifier if successful; otherwise returns negative
     *         value.
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

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#getMetadata()
     */
    @SuppressWarnings("rawtypes")
    public List getMetadata() throws Exception {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#writeMetadata(java.lang.Object)
     */
    public void writeMetadata(Object info) throws Exception {
        log.trace("writeMetadata(): disabled");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#removeMetadata(java.lang.Object)
     */
    public void removeMetadata(Object info) throws Exception {
        log.trace("removeMetadata(): disabled");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#updateMetadata(java.lang.Object)
     */
    public void updateMetadata(Object info) throws Exception {
        log.trace("updateMetadata(): disabled");
    }
}
