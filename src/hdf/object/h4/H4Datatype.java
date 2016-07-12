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

package hdf.object.h4;

import java.util.List;

import hdf.hdflib.HDFConstants;
import hdf.object.Datatype;

/**
 * This class defines HDF4 data type characteristics and APIs for a data type.
 * <p>
 * This class provides several methods to convert an HDF4 datatype identifier to
 * a datatype object, and vice versa. A datatype object is described by four basic
 * fields: datatype class, size, byte order, and sign, while an HDF5 datatype is
 * presented by a datatype identifier.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H4Datatype extends Datatype
{
    private static final long serialVersionUID = -1342029403385521874L;
    
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H4Datatype.class);

    /**
     * Constructs a H4Datatype with specified class, size, byte order and sign.
     * <p>
     * The following is a list of a few examples of H4Datatype:
     * <ol>
     * <li>to create unsigned native integer<br>
     * H4Datatype type = new H4Dataype(CLASS_INTEGER, NATIVE, NATIVE, SIGN_NONE);
     * <li>to create 16-bit signed integer with big endian<br>
     * H4Datatype type = new H4Dataype(CLASS_INTEGER, 2, ORDER_BE, NATIVE);
     * <li>to create native float<br>
     * H4Datatype type = new H4Dataype(CLASS_FLOAT, NATIVE, NATIVE, -1);
     * <li>to create 64-bit double<br>
     * H4Datatype type = new H4Dataype(CLASS_FLOAT, 8, NATIVE, -1);
     * </ol>
     * @param tclass the class of the datatype, e.g. CLASS_INTEGER, CLASS_FLOAT and etc.
     * @param tsize the size of the datatype in bytes, e.g. for a 32-bit integer, the size is 4.
     * @param torder the byte order of the datatype. Valid values are ORDER_LE, ORDER_BE, ORDER_VAX and ORDER_NONE
     * @param tsign the sign of the datatype. Valid values are SIGN_NONE, SIGN_2 and MSGN
     */
    public H4Datatype(int tclass, int tsize, int torder, int tsign)
    {
        super(tclass, tsize, torder, tsign);
    }

    /**
     * Constructs a H4Datatype with a given native datatype identifier.
     * <p>
     * For example,
     * <pre>
     * Datatype dtype = new H4Datatype(HDFConstants.DFNT_INT32);
     * </pre>
     * will construct a datatype equivalent to
     * new H4Datatype(CLASS_INTEGER, 4, NATIVE, SIGN_NONE);
     *
     * @see #fromNative(long nativeID)
     *
     * @param nativeID the native datatype identifier.
     */
    public H4Datatype(long nativeID)
    {
        super(nativeID);

        fromNative(nativeID);
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.DataFormat#hasAttribute()
     */
    public boolean hasAttribute () { return false; }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#fromNative(long)
     */
    @Override
    public void fromNative(long tid)
    {
        log.trace("fromNative(): start");

        datatypeOrder = NATIVE;
        datatypeSign = NATIVE;

        switch((int)tid)
        {
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
        log.trace("fromNative(): finish");
    }

    /**
     *  Allocate a 1D array large enough to hold a multidimensional
     *  array of 'datasize' elements of 'datatype' numbers.
     *
     *  @param datatype  the data type
     *  @param datasize  the size of the data array
     *
     *  @return an array of 'datasize' numbers of datatype.
     *
     * @throws OutOfMemoryError
     *             if the array cannot be allocated
    */
    public static final Object allocateArray(long datatype, int datasize)
    throws OutOfMemoryError
    {
        log.trace("allocateArray(): start");

        if (datasize <= 0) {
            log.debug("datasize <= 0");
            log.trace("allocateArray(): finish");
            return null;
        }

        Object data = null;

        switch((int)datatype)
        {
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

        log.trace("allocateArray(): finish");
        return data;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#getDatatypeDescription()
     */
    @Override
    public String getDatatypeDescription()
    {
        return getDatatypeDescription(toNative());
    }

    /**
     *  Returns the short description of a given datatype.
     *
     *  @param datatype  the data type
     *
     *  @return  a description String
     */
    public static final String getDatatypeDescription(long datatype)
    {
        log.trace("getDatatypeDescription(): start");

        String description = "Unknown";

        switch((int)datatype)
        {
            case HDFConstants.DFNT_CHAR:
                description = "8-bit character";
                break;
            case HDFConstants.DFNT_UCHAR8:
                description = "8-bit unsigned character";
                break;
            case HDFConstants.DFNT_UINT8:
                description = "8-bit unsigned integer";
                break;
            case HDFConstants.DFNT_INT8:
                description = "8-bit integer";
                break;
            case HDFConstants.DFNT_INT16:
                description = "16-bit integer";
                break;
            case HDFConstants.DFNT_UINT16:
                description = "16-bit unsigned integer";
                break;
            case HDFConstants.DFNT_INT32:
                description = "32-bit integer";
                break;
            case HDFConstants.DFNT_UINT32:
                description = "32-bit unsigned integer";
                break;
            case HDFConstants.DFNT_INT64:
                description = "64-bit integer";
                break;
            case HDFConstants.DFNT_UINT64:
                description = "64-bit unsigned integer";
                break;
            case HDFConstants.DFNT_FLOAT32:
                description = "32-bit floating-point";
                break;
            case HDFConstants.DFNT_FLOAT64:
                description = "64-bit floating-point";
                break;
            default:
                log.debug("getDatatypeDescription(): unknown datatype {}", datatype);
                description = "Unknown";
                break;
        }

        log.trace("getDatatypeDescription(): finish");
        return description;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#isUnsigned()
     */
    @Override
    public boolean isUnsigned()
    {
        return isUnsigned(toNative());
    }

    /**
     *  Checks if the datatype is an unsigned integer.
     *
     *  @param datatype  the data type.
     *
     *  @return True is the datatype is an unsigned integer; otherwise returns false.
     */
    public static final boolean isUnsigned(long datatype)
    {
        log.trace("isUnsigned(): start");

        boolean unsigned = false;;

        switch((int)datatype)
        {
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

        log.trace("isUnsigned(): finish");
        return unsigned;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#toNative()
     */
    @Override
    public long toNative()
    {
        log.trace("toNative(): start");

        long tid = -1;
        int tclass = getDatatypeClass();
        int tsize = (int) getDatatypeSize();

        // figure the datatype
        switch (tclass)
        {
            case Datatype.CLASS_INTEGER:
                int tsign = getDatatypeSign();
                if (tsize == 1)
                {
                    if (tsign == Datatype.SIGN_NONE) {
                        tid = HDFConstants.DFNT_UINT8;
                    } else {
                        tid = HDFConstants.DFNT_INT8;
                    }
                }
                else if (tsize == 2)
                {
                    if (tsign == Datatype.SIGN_NONE) {
                        tid = HDFConstants.DFNT_UINT16;
                    } else {
                        tid = HDFConstants.DFNT_INT16;
                    }
                }
                else if ((tsize == 4) || (tsize == NATIVE))
                {
                    if (tsign == Datatype.SIGN_NONE) {
                        tid = HDFConstants.DFNT_UINT32;
                    } else {
                        tid = HDFConstants.DFNT_INT32;
                    }
                }
                else if (tsize == 8)
                {
                    if (tsign == Datatype.SIGN_NONE) {
                        tid = HDFConstants.DFNT_UINT64;
                    } else {
                        tid = HDFConstants.DFNT_INT64;
                    }
                }
                break;
            case Datatype.CLASS_FLOAT:
                if (tsize == Datatype.NATIVE) {
                    tid = HDFConstants.DFNT_FLOAT;
                } else if (tsize == 4) {
                    tid = HDFConstants.DFNT_FLOAT32;
                } else if (tsize == 8) {
                    tid = HDFConstants.DFNT_FLOAT64;
                }
                break;
            case Datatype.CLASS_CHAR:
                int tsign2 = getDatatypeSign();
                if (tsign2 == Datatype.SIGN_NONE) {
                    tid = HDFConstants.DFNT_UCHAR;
                } else {
                    tid = HDFConstants.DFNT_CHAR;
                }
                break;
            case Datatype.CLASS_STRING:
                    tid = HDFConstants.DFNT_CHAR;
                break;
            default:
                log.debug("toNative(): unknown datatype class {}", tclass);
        }

        log.trace("toNative(): finish");
        return tid;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#close(int)
     */
    @Override
    public void close(long id) {;}

    //Implementing DataFormat
    @SuppressWarnings("rawtypes")
    public List getMetadata(int... attrPropList) throws Exception {
        throw new UnsupportedOperationException("getMetadata(int... attrPropList) is not supported");
    }
}
