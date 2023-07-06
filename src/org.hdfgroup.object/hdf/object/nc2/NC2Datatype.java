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

package hdf.object.nc2;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.object.Datatype;

import ucar.ma2.DataType;

/**
 * Datatype encapsulates information of a datatype. Information includes the
 * class, size, endian of a datatype.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class NC2Datatype extends Datatype
{
    private static final long serialVersionUID = 5399364372073889764L;

    private static final Logger log = LoggerFactory.getLogger(NC2Datatype.class);

    /** the native datatype */
    private DataType nativeType = null;

    /**
     * Create an Datatype with specified class, size, byte order and sign. The
     * following list a few example of how to create a Datatype.
     * <ol>
     * <li>to create unsigned native integer<br>
     * NC2Datatype type = new NC2Datatype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);</li>
     * <li>to create 16-bit signed integer with big endian<br>
     * NC2Datatype type = new NC2Datatype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);</li>
     * <li>to create native float<br>
     * NC2Datatype type = new NC2Datatype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);</li>
     * <li>to create 64-bit double<br>
     * NC2Datatype type = new NC2Datatype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);</li>
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
    public NC2Datatype(int tclass, int tsize, int torder, int tsign) throws Exception {
        super(tclass, tsize, torder, tsign);
        datatypeDescription = getDescription();
    }

    /**
     * Constructs a NC2Datatype with a given NetCDF3 native datatype object.
     *
     * @param theType
     *            the netcdf native datatype.
     *
     * @throws Exception
     *            if there is an error
     */
    public NC2Datatype(DataType theType) throws Exception {
        super(null, -1);
        log.trace("NC2Datatype: start nc2 type = {}", theType);
        nativeType = theType;
        fromNative(0);
        datatypeDescription = getDescription();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#fromNative(long)
     */
    /**
     * Translate NetCDF3 datatype object into NC2Datatype.
     *
     * @param tid the native ID
     *            UNUSED.
     */
    @Override
    public void fromNative(long tid) {
        if (nativeType == null)
            return;

        datatypeOrder = NATIVE;
        if (nativeType.equals(DataType.CHAR)) {
            datatypeClass = CLASS_CHAR;
            datatypeSize = 1;
        }
        else if (nativeType.equals(DataType.BYTE)) {
            datatypeClass = CLASS_INTEGER;
            datatypeSize = 1;
        }
        else if (nativeType.equals(DataType.SHORT)) {
            datatypeClass = CLASS_INTEGER;
            datatypeSize = 2;
        }
        else if (nativeType.equals(DataType.INT)) {
            datatypeClass = CLASS_INTEGER;
            datatypeSize = 4;
        }
        else if (nativeType.equals(DataType.LONG)) {
            datatypeClass = CLASS_INTEGER;
            datatypeSize = 8;
        }
        else if (nativeType.equals(DataType.FLOAT)) {
            datatypeClass = CLASS_FLOAT;
            datatypeSize = 4;
        }
        else if (nativeType.equals(DataType.DOUBLE)) {
            datatypeClass = CLASS_FLOAT;
            datatypeSize = 8;
        }
        else if (nativeType.equals(DataType.STRING)) {
            datatypeClass = CLASS_STRING;
            datatypeSize = 80; // default length. need to figure out the actual length
        }
        else if (nativeType.equals(DataType.OPAQUE)) {
            datatypeClass = CLASS_OPAQUE;
            datatypeSize = 1;
        }

        log.trace("Datatype class={} size={}", datatypeClass, datatypeSize);
    }

    /**
     * Allocate an one-dimensional array of byte, short, int, long, float,
     * double, or String to store data retrieved from an NetCDF3 file based on
     * the given NetCDF3 datatype and dimension sizes.
     *
     * @param dtype
     *            the NetCDF3 datatype object.
     * @param datasize
     *            the size of the data array
     *
     * @return an array of 'datasize' numbers of datatype.
     *
     * @throws OutOfMemoryError
     *             if the array cannot be allocated
     */
    public static final Object allocateArray(DataType dtype, int datasize) throws OutOfMemoryError {
        if ((datasize <= 0) || (dtype == null)) {
            log.debug("datasize <= 0");
            return null;
        }

        Object data = null;

        if (dtype.equals(DataType.BYTE))
            data = new byte[datasize];
        else if (dtype.equals(DataType.SHORT))
            data = new short[datasize];
        else if (dtype.equals(DataType.INT))
            data = new int[datasize];
        else if (dtype.equals(DataType.LONG))
            data = new long[datasize];
        else if (dtype.equals(DataType.FLOAT))
            data = new float[datasize];
        else if (dtype.equals(DataType.DOUBLE))
            data = new double[datasize];
        else if (dtype.equals(DataType.STRING))
            data = new String[datasize];

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

        if (nativeType == null)
            description = "Unknown data type.";

        description = nativeType.toString();

        return description;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Datatype#isUnsigned()
     */
    @Override
    public boolean isUnsigned() {
        if (nativeType.isNumeric())
            return false;
        else
            return false;
    }

    @Override
    public boolean isText() {
        return (nativeType == DataType.CHAR);
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#createNative()
     */
    @Override
    public long createNative() {
        if (datatypeClass == CLASS_INTEGER) {
            if (datatypeSize == 1)
                nativeType = DataType.BYTE;
            else if (datatypeSize == 2)
                nativeType = DataType.SHORT;
            else if (datatypeSize == 4)
                nativeType = DataType.INT;
            else if (datatypeSize == 8)
                nativeType = DataType.LONG;
        }
        else if (datatypeClass == CLASS_FLOAT) {
            if (datatypeSize == 4)
                nativeType = DataType.FLOAT;
            else if (datatypeSize == 8)
                nativeType = DataType.DOUBLE;
        }
        else if (datatypeClass == CLASS_STRING) {
            nativeType = DataType.STRING;
        }
        else {
            log.debug("createNative(): unknown datatype class {}", datatypeClass);
        }

        return -1;
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
