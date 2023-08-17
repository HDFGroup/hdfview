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

package hdf.object.h4;

import java.util.List;

import hdf.hdflib.HDFConstants;
import hdf.object.Datatype;

/**
 * This class defines HDF4 data type characteristics and APIs for a data type.
 *
 * This class provides several methods to convert an HDF4 datatype identifier to a datatype object,
 * and vice versa. A datatype object is described by four basic fields: datatype class, size, byte
 * order, and sign, while an HDF5 datatype is presented by a datatype identifier.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H4Datatype extends Datatype {
    private static final long serialVersionUID = -1342029403385521874L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H4Datatype.class);

    /**
     * Constructs a H4Datatype with specified class, size, byte order and sign.
     *
     * The following is a list of a few examples of H4Datatype:
     * <ol>
     * <li>to create unsigned native integer<br>
     * H4Datatype type = new H4Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H4Datatype type = new H4Dataype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);
     * <li>to create native float<br>
     * H4Datatype type = new H4Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
     * <li>to create 64-bit double<br>
     * H4Datatype type = new H4Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
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
    public H4Datatype(int tclass, int tsize, int torder, int tsign) throws Exception {
        super(tclass, tsize, torder, tsign);
        datatypeDescription = getDescription();
    }

    /**
     * Constructs a H4Datatype with a given native datatype identifier.
     *
     * For example,
     *
     * <pre>
     * Datatype dtype = new H4Datatype(HDFConstants.DFNT_INT32);
     * </pre>
     *
     * will construct a datatype equivalent to
     *
     * <pre>
     * new H4Datatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.SIGN_NONE);
     * </pre>
     *
     * @see #fromNative(long nativeID)
     *
     * @param nativeID
     *            the native datatype identifier.
     *
     * @throws Exception
     *            if there is an error
     */
    public H4Datatype(long nativeID) throws Exception {
        super(null, nativeID);

        fromNative(nativeID);
        datatypeDescription = getDescription();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#fromNative(long)
     */
    @Override
    public void fromNative(long tid) {
        datatypeOrder = NATIVE;
        datatypeSign = NATIVE;

        switch ((int) tid) {
            case HDFConstants.DFNT_CHAR:
                datatypeClass = CLASS_CHAR;
                datatypeSize = 1;
                break;
            case HDFConstants.DFNT_UCHAR8:
                datatypeClass = CLASS_CHAR;
                datatypeSize = 1;
                datatypeSign = SIGN_NONE;
                break;
            case HDFConstants.DFNT_INT8:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 1;
                break;
            case HDFConstants.DFNT_UINT8:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 1;
                datatypeSign = SIGN_NONE;
                break;
            case HDFConstants.DFNT_INT16:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 2;
                break;
            case HDFConstants.DFNT_UINT16:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 2;
                datatypeSign = SIGN_NONE;
                break;
            case HDFConstants.DFNT_INT32:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 4;
                break;
            case HDFConstants.DFNT_UINT32:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 4;
                datatypeSign = SIGN_NONE;
                break;
            case HDFConstants.DFNT_INT64:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 8;
                break;
            case HDFConstants.DFNT_UINT64:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 8;
                datatypeSign = SIGN_NONE;
                break;
            case HDFConstants.DFNT_FLOAT32:
                datatypeClass = CLASS_FLOAT;
                datatypeSize = 4;
                break;
            case HDFConstants.DFNT_FLOAT64:
                datatypeClass = CLASS_FLOAT;
                datatypeSize = 8;
                break;
            default:
                datatypeClass = CLASS_NO_CLASS;
                break;
        }

        log.trace("Datatype class={} size={}", datatypeClass, datatypeSize);
    }

    /**
     * Allocate a 1D array large enough to hold a multidimensional array of 'datasize' elements of
     * 'datatype' numbers.
     *
     * @param datatype
     *            the data type
     * @param datasize
     *            the size of the data array
     *
     * @return an array of 'datasize' numbers of datatype.
     *
     * @throws OutOfMemoryError
     *             if the array cannot be allocated
     */
    public static final Object allocateArray(long datatype, int datasize) throws OutOfMemoryError {
        if (datasize <= 0) {
            log.debug("datasize <= 0");
            return null;
        }

        Object data = null;

        switch ((int) datatype) {
            case HDFConstants.DFNT_CHAR:
            case HDFConstants.DFNT_UCHAR8:
            case HDFConstants.DFNT_UINT8:
            case HDFConstants.DFNT_INT8:
                log.trace("allocateArray(): allocating byte array of size {}", datasize);
                data = new byte[datasize];
                break;
            case HDFConstants.DFNT_INT16:
            case HDFConstants.DFNT_UINT16:
                log.trace("allocateArray(): allocating short array of size {}", datasize);
                data = new short[datasize];
                break;
            case HDFConstants.DFNT_INT32:
            case HDFConstants.DFNT_UINT32:
                log.trace("allocateArray(): allocating int array of size {}", datasize);
                if (datasize == NATIVE)
                    datasize = 4;
                data = new int[datasize];
                break;
            case HDFConstants.DFNT_INT64:
            case HDFConstants.DFNT_UINT64:
                log.trace("allocateArray(): allocating long array of size {}", datasize);
                data = new long[datasize];
                break;
            case HDFConstants.DFNT_FLOAT32:
                log.trace("allocateArray(): allocating float array of size {}", datasize);
                data = new float[datasize];
                break;
            case HDFConstants.DFNT_FLOAT64:
                log.trace("allocateArray(): allocating double array of size {}", datasize);
                data = new double[datasize];
                break;
            default:
                log.debug("allocateArray(): unknown datatype {}", datatype);
                data = null;
                break;
        }

        return data;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#getDatatypeDescription()
     */
    @Override
    public String getDescription() {
        if (datatypeDescription != null)
            return datatypeDescription;

        String description = null;

        switch (datatypeClass) {
            case CLASS_CHAR:
                description = "8-bit " + (isUnsigned() ? "unsigned " : "") + "character";
                break;
            case CLASS_INTEGER:
                description = String.valueOf(datatypeSize * 8) + "-bit " + (isUnsigned() ? "unsigned " : "") + "integer";
                break;
            case CLASS_FLOAT:
                description = String.valueOf(datatypeSize * 8) + "-bit floating-point";
                break;
            default:
                description = "Unknown";
                break;
        }

        return description;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#isUnsigned()
     */
    @Override
    public boolean isUnsigned() {
        return (Datatype.SIGN_NONE == getDatatypeSign());
    }

    /**
     * Checks if the datatype is an unsigned integer.
     *
     * @param datatype
     *            the data type.
     *
     * @return True is the datatype is an unsigned integer; otherwise returns false.
     */
    public static final boolean isUnsigned(long datatype) {
        boolean unsigned = false;

        switch((int) datatype) {
            case HDFConstants.DFNT_UCHAR8:
            case HDFConstants.DFNT_UINT8:
            case HDFConstants.DFNT_UINT16:
            case HDFConstants.DFNT_UINT32:
            case HDFConstants.DFNT_UINT64:
                unsigned = true;
                break;
            default:
                log.debug("isUnsigned(): unknown datatype {}", datatype);
                unsigned = false;
                break;
        }

        return unsigned;
    }

    @Override
    public boolean isText() {
        return (Datatype.CLASS_STRING == getDatatypeClass());
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#createNative()
     */
    @Override
    public long createNative() {
        long tid = -1;
        int tclass = getDatatypeClass();
        int tsize = (int) getDatatypeSize();

        // figure the datatype
        switch (tclass) {
            case Datatype.CLASS_INTEGER:
                if (tsize == 1) {
                    if (isUnsigned()) {
                        tid = HDFConstants.DFNT_UINT8;
                    }
                    else {
                        tid = HDFConstants.DFNT_INT8;
                    }
                }
                else if (tsize == 2) {
                    if (isUnsigned()) {
                        tid = HDFConstants.DFNT_UINT16;
                    }
                    else {
                        tid = HDFConstants.DFNT_INT16;
                    }
                }
                else if ((tsize == 4) || (tsize == NATIVE)) {
                    if (isUnsigned()) {
                        tid = HDFConstants.DFNT_UINT32;
                    }
                    else {
                        tid = HDFConstants.DFNT_INT32;
                    }
                }
                else if (tsize == 8) {
                    if (isUnsigned()) {
                        tid = HDFConstants.DFNT_UINT64;
                    }
                    else {
                        tid = HDFConstants.DFNT_INT64;
                    }
                }
                break;
            case Datatype.CLASS_FLOAT:
                if (tsize == Datatype.NATIVE) {
                    tid = HDFConstants.DFNT_FLOAT;
                }
                else if (tsize == 4) {
                    tid = HDFConstants.DFNT_FLOAT32;
                }
                else if (tsize == 8) {
                    tid = HDFConstants.DFNT_FLOAT64;
                }
                break;
            case Datatype.CLASS_CHAR:
                if (isUnsigned()) {
                    tid = HDFConstants.DFNT_UCHAR;
                }
                else {
                    tid = HDFConstants.DFNT_CHAR;
                }
                break;
            case Datatype.CLASS_STRING:
                tid = HDFConstants.DFNT_CHAR;
                break;
            default:
                log.debug("createNative(): unknown datatype class {}", tclass);
        }

        return tid;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#close(int)
     */
    @Override
    public void close(long id) {
        // No implementation
    }

    // Implementing MetaDataContainer
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
     * @throws Exception
     *             if the metadata can not be retrieved
     */
    @SuppressWarnings("rawtypes")
    public List getMetadata(int... attrPropList) throws Exception {
        throw new UnsupportedOperationException("getMetadata(int... attrPropList) is not supported");
    }

    /**
     * Check if the object has any attributes attached.
     *
     * @return true if it has any attributes, false otherwise.
     */
    @Override
    public boolean hasAttribute() {
        return false;
    }
}
