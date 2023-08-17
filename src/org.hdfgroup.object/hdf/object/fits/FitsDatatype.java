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

package hdf.object.fits;

import java.util.List;

import hdf.object.Datatype;
import nom.tam.fits.BasicHDU;

/**
 * Datatype encapsulates information of a datatype.
 * Information includes the class, size, endian of a datatype.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class FitsDatatype extends Datatype
{
    private static final long serialVersionUID = 6545936196104493765L;

    /** the native type */
    private long nativeType;

    /**
     * Create an Datatype with specified class, size, byte order and sign.
     * The following list a few example of how to create a Datatype.
     * <OL>
     * <LI>to create unsigned native integer<br>
     * FitsDatatype type = new H5Dataype(Datatype.CLASS_INTEGER, Datatype.NATIVE, Datatype.NATIVE, Datatype.SIGN_NONE);
     * <LI>to create 16-bit signed integer with big endian<br>
     * FitsDatatype type = new H5Dataype(Datatype.CLASS_INTEGER, 2, Datatype.ORDER_BE, Datatype.NATIVE);
     * <LI>to create native float<br>
     * FitsDatatype type = new H5Dataype(Datatype.CLASS_FLOAT, Datatype.NATIVE, Datatype.NATIVE, Datatype.NATIVE);
     * <LI>to create 64-bit double<br>
     * FitsDatatype type = new H5Dataype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
     * </OL>
     *
     * @param tclass the class of the datatype.
     * @param tsize the size of the datatype in bytes.
     * @param torder the order of the datatype.
     * @param tsign the sign of the datatype.
     *
     * @throws Exception
     *            if there is an error
     */
    public FitsDatatype(int tclass, int tsize, int torder, int tsign) throws Exception {
        super(tclass, tsize, torder, tsign);
        datatypeDescription = getDescription();
    }

    /**
     * Create a Datatype with a given fits native datatype.
     *
     * @param theType the fits native datatype.
     *
     * @throws Exception
     *            if there is an error
     */
    public FitsDatatype(long theType) throws Exception {
        super(null, -1);
        nativeType = theType;
        fromNative(0);
        datatypeDescription = getDescription();
    }

    /**
     * Allocate an one-dimensional array of byte, short, int, long, float, double,
     * or String to store data retrieved from an fits file based on the given
     * fits datatype and dimension sizes.
     *
     * @param dtype the fits datatype.
     * @param size the total size of the array.
     * @return the array object if successful and null otherwise.
     */
    public static Object allocateArray(long dtype, int size) throws OutOfMemoryError {
        Object data = null;

        if (size <= 0 )
            return null;

        switch ((int)dtype) {
            case BasicHDU.BITPIX_BYTE:
                data = new byte[size];
                break;
            case BasicHDU.BITPIX_SHORT:
                data = new short[size];
                break;
            case BasicHDU.BITPIX_INT:
                data = new int[size];
                break;
            case BasicHDU.BITPIX_LONG:
                data = new long[size];
                break;
            case BasicHDU.BITPIX_FLOAT:
                data = new float[size];
                break;
            case BasicHDU.BITPIX_DOUBLE:
                data = new double[size];
                break;
            default:
                break;
        }

        return data;
    }

    /**
     * Translate fits datatype identifier into FitsDatatype.
     */
    public void fromNative() {
        fromNative(nativeType);
    }

    /**
     * Translate fits datatype identifier into FitsDatatype.
     *
     * @param dtype the fits native datatype.
     */
    @Override
    public void fromNative(long dtype) {
        switch ((int)dtype) {
            case BasicHDU.BITPIX_BYTE:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 1;
                break;
            case BasicHDU.BITPIX_SHORT:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 2;
                break;
            case BasicHDU.BITPIX_INT:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 4;
                break;
            case BasicHDU.BITPIX_LONG:
                datatypeClass = CLASS_INTEGER;
                datatypeSize = 8;
                break;
            case BasicHDU.BITPIX_FLOAT:
                datatypeClass = CLASS_FLOAT;
                datatypeSize = 4;
                break;
            case BasicHDU.BITPIX_DOUBLE:
                datatypeClass = CLASS_FLOAT;
                datatypeSize = 8;
                break;
            default:
                break;
        }
    }

    // implementing Datatype
    @Override
    public String getDescription() {
        if (datatypeDescription != null)
            return datatypeDescription;

        String description = null;

        switch ((int)nativeType) {
            case BasicHDU.BITPIX_BYTE:
                description = "8-bit integer";
                break;
            case BasicHDU.BITPIX_SHORT:
                description = "16-bit integer";
                break;
            case BasicHDU.BITPIX_INT:
                description = "32-bit integer";
                break;
            case BasicHDU.BITPIX_LONG:
                description = "64-bit integer";
                break;
            case BasicHDU.BITPIX_FLOAT:
                description = "32-bit float";
                break;
            case BasicHDU.BITPIX_DOUBLE:
                description = "64-bit float";
                break;
            default:
                if (this.isString())
                    description = "String";
                else if (this.isChar())
                    description = "Char";
                else if (this.isInteger())
                    description = "Integer";
                else if (this.isFloat())
                    description = "Float";
                else
                    description = "Unknown data type.";
                break;
        }

        return description;
    }

    // implementing Datatype
    @Override
    public boolean isText() {
        return false;
    }

    // implementing Datatype
    @Override
    public boolean isUnsigned() {
        return false;
    }

    // implementing Datatype
    @Override
    public long createNative() {
        if (datatypeClass == CLASS_INTEGER) {
            if (datatypeSize == 1)
                nativeType = BasicHDU.BITPIX_BYTE;
            else if (datatypeSize == 2)
                nativeType = BasicHDU.BITPIX_SHORT;
            else if (datatypeSize == 4)
                nativeType = BasicHDU.BITPIX_INT;
            else if (datatypeSize == 8)
                nativeType = BasicHDU.BITPIX_LONG;
        }
        else if (datatypeClass == CLASS_FLOAT) {
            if (datatypeSize == 4)
                nativeType = BasicHDU.BITPIX_FLOAT;
            else if (datatypeSize == 8)
                nativeType = BasicHDU.BITPIX_DOUBLE;
        }

        return nativeType;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Datatype#close(int)
     */
    @Override
    public void close(long id) {
        // Nothing to implement
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

    /*
     * (non-Javadoc)
     * @see hdf.object.MetaDataContainer#hasAttribute()
     */
    @Override
    public boolean hasAttribute () {
        return false;
    }
}
