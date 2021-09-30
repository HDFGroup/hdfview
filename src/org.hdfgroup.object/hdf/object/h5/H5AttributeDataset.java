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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5DataFiltersException;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;

import hdf.object.AttributeDataset;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.HObject;

import hdf.object.h5.H5Datatype;

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
 * AttributeDataset dataRange = new Attribute(name, type, dims);
 * // Set the attribute value
 * dataRange.setValue(value);
 * // See FileFormat.writeAttribute() for how to attach an attribute to an object,
 * &#64;see hdf.object.FileFormat#writeAttribute(HObject, AttributeDataset, boolean)
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
public abstract class H5AttributeDataset extends AttributeDataset {

    private static final long serialVersionUID = 2072473407027648309L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5AttributeDataset.class);

    /**
     * Flag to indicate is the original unsigned C data is converted.
     */
    protected boolean unsignedConverted;

    /*
     * Enum to indicate the type of I/O to perform inside of the common I/O
     * function.
     */
    public static enum IO_TYPE {
        READ, WRITE
    };

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
     * AttributeDataset attr = new AttributeDataset(attrName, attrType, attrDims);
     * attr.setValue(classValue);
     * </pre>
     *
     * @param parentObj
     *            the HObject to which this AttributeDataset is attached.
     * @param attrName
     *            the name of the attribute.
     * @param attrType
     *            the datatype of the attribute.
     * @param attrDims
     *            the dimension sizes of the attribute, null for scalar attribute
     *
     * @see hdf.object.Datatype
     */
    public H5AttributeDataset(HObject parentObj, String attrName, Datatype attrType, long[] attrDims) {
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
     * AttributeDataset attr = new AttributeDataset(attrName, attrType, attrDims, classValue);
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
    public H5AttributeDataset(HObject parentObj, String attrName, Datatype attrType, long[] attrDims, Object attrValue) {
        super(parentObj, attrName, attrType, attrDims, attrValue);
    }

    /*
     * Routine to convert datatypes that are read in as byte arrays to
     * regular types.
     */
    protected Object convertByteMember(final H5Datatype dtype, byte[] byteData) {
        Object theObj = null;

        if (dtype.getDatatypeSize() == 1) {
            /*
             * Normal byte[] type, such as an integer datatype of size 1.
             */
            theObj = byteData;
        }
        else if (dtype.isString() && !dtype.isVarStr() && convertByteToString) {
            log.trace("convertByteMember(): converting byte array to string array");

            theObj = byteToString(byteData, (int) dtype.getDatatypeSize());
        }
        else if (dtype.isInteger()) {
            log.trace("convertByteMember(): converting byte array to integer array");

            switch ((int)dtype.getDatatypeSize()) {
            case 1:
                /*
                 * Normal byte[] type, such as an integer datatype of size 1.
                 */
                theObj = byteData;
                break;
            case 2:
                theObj = HDFNativeData.byteToShort(byteData);
                break;
            case 4:
                theObj = HDFNativeData.byteToInt(byteData);
                break;
            case 8:
                theObj = HDFNativeData.byteToLong(byteData);
                break;
            default:
                log.debug("convertByteMember(): invalid datatype size");
                theObj = new String("*ERROR*");
                break;
            }
        }
        else if (dtype.isFloat()) {
            log.trace("convertByteMember(): converting byte array to float array");

            if (dtype.getDatatypeSize() == 16)
                theObj = dtype.byteToBigDecimal(byteData, 0);
            else if (dtype.getDatatypeSize() == 8)
                theObj = HDFNativeData.byteToDouble(byteData);
            else
                theObj = HDFNativeData.byteToFloat(byteData);
        }
        else if (dtype.isRef()) {
            log.trace("convertByteMember(): reference type - converting byte array to long array");

            theObj = HDFNativeData.byteToLong(byteData);
        }
        else if (dtype.isArray()) {
            H5Datatype baseType = (H5Datatype) dtype.getDatatypeBase();

            /*
             * Retrieve the real base datatype in the case of ARRAY of ARRAY datatypes.
             */
            while (baseType.isArray()) baseType = (H5Datatype) baseType.getDatatypeBase();

            /*
             * Optimize for the common cases of Arrays.
             */
            switch (baseType.getDatatypeClass()) {
                case Datatype.CLASS_INTEGER:
                case Datatype.CLASS_FLOAT:
                case Datatype.CLASS_CHAR:
                case Datatype.CLASS_STRING:
                case Datatype.CLASS_BITFIELD:
                case Datatype.CLASS_OPAQUE:
                case Datatype.CLASS_COMPOUND:
                case Datatype.CLASS_REFERENCE:
                case Datatype.CLASS_ENUM:
                case Datatype.CLASS_VLEN:
                case Datatype.CLASS_TIME:
                    theObj = convertByteMember(baseType, byteData);
                    break;

                case Datatype.CLASS_ARRAY:
                {
                    H5Datatype arrayType = (H5Datatype) dtype.getDatatypeBase();

                    long[] arrayDims = dtype.getArrayDims();
                    int arrSize = 1;
                    for (int i = 0; i < arrayDims.length; i++) {
                        arrSize *= arrayDims[i];
                    }

                    theObj = new Object[arrSize];

                    for (int i = 0; i < arrSize; i++) {
                        byte[] indexedBytes = Arrays.copyOfRange(byteData, (int) (i * arrayType.getDatatypeSize()),
                                (int) ((i + 1) * arrayType.getDatatypeSize()));
                        ((Object[]) theObj)[i] = convertByteMember(arrayType, indexedBytes);
                    }

                    break;
                }

                case Datatype.CLASS_NO_CLASS:
                default:
                    log.debug("convertByteMember(): invalid datatype class");
                    theObj = new String("*ERROR*");
            }
        }
        else if (dtype.isCompound()) {
            /*
             * TODO: still valid after reading change?
             */
            theObj = convertCompoundByteMembers(dtype, byteData);
        }
        else {
            theObj = byteData;
        }

        return theObj;
    }

    /**
     * Given an array of bytes representing a compound Datatype, converts each of
     * its members into Objects and returns the results.
     *
     * @param dtype
     *            The compound datatype to convert
     * @param data
     *            The byte array representing the data of the compound Datatype
     * @return The converted types of the bytes
     */
    private Object convertCompoundByteMembers(final H5Datatype dtype, byte[] data) {
        List<Object> theData = null;

        List<Datatype> allSelectedTypes = Arrays.asList(getSelectedMemberTypes());
        List<Datatype> localTypes = new ArrayList<>(dtype.getCompoundMemberTypes());
        Iterator<Datatype> localIt = localTypes.iterator();
        while (localIt.hasNext()) {
            Datatype curType = localIt.next();

            if (curType.isCompound())
                continue;

            if (!allSelectedTypes.contains(curType))
                localIt.remove();
        }

        theData = new ArrayList<>(localTypes.size());
        for (int i = 0, index = 0; i < localTypes.size(); i++) {
            Datatype curType = localTypes.get(i);

            if (curType.isCompound())
                theData.add(convertCompoundByteMembers((H5Datatype) curType,
                        Arrays.copyOfRange(data, index, index + (int) curType.getDatatypeSize())));
            else
                theData.add(convertByteMember((H5Datatype) curType,
                        Arrays.copyOfRange(data, index, index + (int) curType.getDatatypeSize())));

            index += curType.getDatatypeSize();
        }

        return theData;
    }

    /*
     * Routine to convert datatypes that are in object arrays to
     * bytes.
     */
    protected byte[] convertMemberByte(final H5Datatype dtype, Object theObj) {
        byte[] byteData = null;

        if (dtype.getDatatypeSize() == 1) {
            /*
             * Normal byte[] type, such as an integer datatype of size 1.
             */
            byteData = (byte[])theObj;
        }
        else if (dtype.isString() && !dtype.isVarStr() && convertByteToString) {
            log.trace("convertMemberByte(): converting string array to byte array");

            byteData = stringToByte((String[])theObj, (int) dtype.getDatatypeSize());
        }
        else if (dtype.isInteger()) {
            log.trace("convertMemberByte(): converting integer array to byte array");

            switch ((int)dtype.getDatatypeSize()) {
            case 1:
                /*
                 * Normal byte[] type, such as an integer datatype of size 1.
                 */
                byteData = (byte[])theObj;
                break;
            case 2:
                byteData = HDFNativeData.shortToByte(0, 1, (short[])theObj);
                break;
            case 4:
                byteData = HDFNativeData.intToByte(0, 1, (int[])theObj);
                break;
            case 8:
                byteData = HDFNativeData.longToByte(0, 1, (long[])theObj);
                break;
            default:
                log.debug("convertByteMember(): invalid datatype size");
                byteData = null;
                break;
            }
        }
        else if (dtype.isFloat()) {
            log.trace("convertByteMember(): converting float array to byte array");

            if (dtype.getDatatypeSize() == 16)
                byteData = dtype.bigDecimalToByte((BigDecimal[])theObj, 0);
            else if (dtype.getDatatypeSize() == 8)
                byteData = HDFNativeData.doubleToByte(0, 1, (double[])theObj);
            else
                byteData = HDFNativeData.floatToByte(0, 1, (float[])theObj);
        }
        else if (dtype.isRef()) {
            log.trace("convertByteMember(): reference type - converting long array to byte array");

            byteData = HDFNativeData.longToByte(0, 1, (long[])theObj);
        }
        else if (dtype.isArray()) {
            H5Datatype baseType = (H5Datatype) dtype.getDatatypeBase();

            /*
             * Retrieve the real base datatype in the case of ARRAY of ARRAY datatypes.
             */
            while (baseType.isArray())
                baseType = (H5Datatype) baseType.getDatatypeBase();

            /*
             * Optimize for the common cases of Arrays.
             */
            switch (baseType.getDatatypeClass()) {
                case Datatype.CLASS_INTEGER:
                case Datatype.CLASS_FLOAT:
                case Datatype.CLASS_CHAR:
                case Datatype.CLASS_STRING:
                case Datatype.CLASS_BITFIELD:
                case Datatype.CLASS_OPAQUE:
                case Datatype.CLASS_COMPOUND:
                case Datatype.CLASS_REFERENCE:
                case Datatype.CLASS_ENUM:
                case Datatype.CLASS_VLEN:
                case Datatype.CLASS_TIME:
                    byteData = convertMemberByte(baseType, theObj);
                    break;

                case Datatype.CLASS_ARRAY:
                {
                    H5Datatype arrayType = (H5Datatype) dtype.getDatatypeBase();

                    long[] arrayDims = dtype.getArrayDims();
                    int arrSize = 1;
                    for (int i = 0; i < arrayDims.length; i++) {
                        arrSize *= arrayDims[i];
                    }

                    byteData = new byte[arrSize * (int)arrayType.getDatatypeSize()];

                    for (int i = 0; i < arrSize; i++) {
                        byte[] indexedBytes = convertMemberByte(arrayType, ((Object[]) theObj)[i]);
                        System.arraycopy(indexedBytes, 0, byteData, (int)(i * arrayType.getDatatypeSize()), (int)arrayType.getDatatypeSize());
                    }

                    break;
                }

                case Datatype.CLASS_NO_CLASS:
                default:
                    log.debug("convertByteMember(): invalid datatype class");
                    byteData = null;
            }
        }
        else if (dtype.isCompound()) {
            /*
             * TODO: still valid after reading change?
             */
            byteData = (byte[])convertCompoundMemberBytes(dtype, (List<Object>)theObj);
        }
        else {
            byteData = (byte[])theObj;
        }

        return byteData;
    }

    /**
     * Given an array of objects representing a compound Datatype, converts each of
     * its members into bytes and returns the results.
     *
     * @param dtype
     *            The compound datatype to convert
     * @param theObj
     *            The object array representing the data of the compound Datatype
     * @return The converted bytes of the objects
     */
    private byte[] convertCompoundMemberBytes(final H5Datatype dtype, List<Object> theObj) {
        List<Datatype> allSelectedTypes = Arrays.asList(this.getSelectedMemberTypes());
        List<Datatype> localTypes = new ArrayList<>(dtype.getCompoundMemberTypes());
        Iterator<Datatype> localIt = localTypes.iterator();
        while (localIt.hasNext()) {
            Datatype curType = localIt.next();

            if (curType.isCompound())
                continue;

            if (!allSelectedTypes.contains(curType))
                localIt.remove();
        }

        byte[] byteData = new byte[(int)dtype.getDatatypeSize()];
        for (int i = 0, index = 0; i < localTypes.size(); i++) {
            Datatype curType = localTypes.get(i);
            byte[] indexedBytes = null;
            if (curType.isCompound())
                indexedBytes = convertCompoundMemberBytes((H5Datatype) curType, (List<Object>)theObj.get(i));
            else
                indexedBytes = convertMemberByte((H5Datatype) curType, theObj.get(i));

            System.arraycopy(indexedBytes, 0, byteData, index + (int)curType.getDatatypeSize(), (int)curType.getDatatypeSize());
            index += curType.getDatatypeSize();
        }

        return byteData;
    }

    public static long getAttributeSize(long attr_id) throws Exception {
        long lsize = 1;
        long sid = H5.H5Aget_space(attr_id);
        log.trace("getAttributeSize(): aid={} sid={}", attr_id, sid);

        long dims[] = null;
        int rank = H5.H5Sget_simple_extent_ndims(sid);
        int space_type = H5.H5Sget_simple_extent_type(sid);

        log.trace("getAttributeSize(): isScalar={} isNull={}", (space_type == HDF5Constants.H5S_SCALAR), (space_type == HDF5Constants.H5S_NULL));

        if (rank > 0 && space_type == HDF5Constants.H5S_SIMPLE) {
            dims = new long[rank];
            H5.H5Sget_simple_extent_dims(sid, dims, null);
            log.trace("getAttributeSize(): rank={}, dims={}", rank, dims);
            for (int j = 0; j < dims.length; j++) {
                lsize *= dims[j];
            }
        }
        try {
            H5.H5Sclose(sid);
        }
        catch (Exception ex) {
            log.debug("getAttributeSize(): H5Sclose(sid {}) failure: ", sid, ex);
        }
        return lsize;
    }

    protected Object AttributeCommonIO(long attr_id, IO_TYPE ioType, Object objBuf) throws Exception {
        H5Datatype dsDatatype = (H5Datatype) getDatatype();
        Object theData = null;

        long dt_size = dsDatatype.getDatatypeSize();
        log.trace("AttributeCommonIO(): create native");
        long tid = dsDatatype.createNative();

        if (ioType == IO_TYPE.READ) {
            log.trace("AttributeCommonIO():read ioType isNamed={} isEnum={} isText={} isRefObj={}", dsDatatype.isNamed(), dsDatatype.isEnum(), dsDatatype.isText(), dsDatatype.isRefObj());

            long lsize = 1;
            for (int j = 0; j < dims.length; j++) {
                lsize *= dims[j];
            }
            log.trace("AttributeCommonIO():read ioType dt_size={} lsize={}", dt_size, lsize);

            try {
                if (dsDatatype.isVarStr()) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++) {
                        strs[j] = "";
                    }
                    try {
                        log.trace("AttributeCommonIO():read ioType H5AreadVL");
                        H5.H5AreadVL(attr_id, tid, strs);
                    }
                    catch (Exception ex) {
                        log.debug("AttributeCommonIO():read ioType H5AreadVL failure: ", ex);
                        ex.printStackTrace();
                    }
                    theData = strs;
                }
                else if (dsDatatype.isCompound()) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++) {
                        strs[j] = "";
                    }
                    try {
                        log.trace("AttributeCommonIO():read ioType H5AreadComplex");
                        H5.H5AreadComplex(attr_id, tid, strs);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    theData = strs;
                }
                else if (dsDatatype.isVLEN()) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++) {
                        strs[j] = "";
                    }
                    try {
                        log.trace("AttributeCommonIO():read ioType H5AreadVL");
                        H5.H5AreadVL(attr_id, tid, strs);
                    }
                    catch (Exception ex) {
                        log.debug("AttributeCommonIO():read ioType H5AreadVL failure: ", ex);
                        ex.printStackTrace();
                    }
                    theData = strs;
                }
                else {
                    Object attr_data = null;
                    try {
                        attr_data = H5Datatype.allocateArray(((H5Datatype)dsDatatype), (int) lsize);
                    }
                    catch (OutOfMemoryError e) {
                        log.debug("AttributeCommonIO():read ioType out of memory", e);
                        theData = null;
                    }
                    if (attr_data == null) {
                        log.debug("AttributeCommonIO():read ioType allocateArray returned null");
                    }

                    log.trace("AttributeCommonIO():read ioType H5Aread isArray()={}", dsDatatype.isArray());
                    try {
                        H5.H5Aread(attr_id, tid, attr_data);
                    }
                    catch (Exception ex) {
                        log.debug("AttributeCommonIO():read ioType H5Aread failure: ", ex);
                        ex.printStackTrace();
                    }

                    /*
                     * Perform any necessary data conversions.
                     */
                    if (dsDatatype.isText() && convertByteToString && (attr_data instanceof byte[])) {
                        log.trace("AttributeCommonIO():read ioType isText: converting byte array to string array");
                        theData = byteToString((byte[]) attr_data, (int) dsDatatype.getDatatypeSize());
                    }
                    else if (dsDatatype.isFloat() && dsDatatype.getDatatypeSize() == 16) {
                        log.trace("AttributeCommonIO():read ioType isFloat: converting byte array to BigDecimal array");
                        theData = dsDatatype.byteToBigDecimal(0, (int)nPoints, (byte[]) attr_data);
                    }
                    else if (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isFloat() && dsDatatype.getDatatypeBase().getDatatypeSize() == 16) {
                        log.trace("AttributeCommonIO():read ioType isArray and isFloat: converting byte array to BigDecimal array");
                        long[] arrayDims = dsDatatype.getArrayDims();
                        int asize = (int)nPoints;
                        for (int j = 0; j < arrayDims.length; j++) {
                            asize *= arrayDims[j];
                        }
                        theData = ((H5Datatype)dsDatatype.getDatatypeBase()).byteToBigDecimal(0, asize, (byte[]) attr_data);
                    }
                    else if (dsDatatype.isRefObj()) {
                        log.trace("AttributeCommonIO():read ioType isREF: converting byte array to long array");
                        theData = HDFNativeData.byteToLong((byte[]) attr_data);
                    }
                    else
                        theData = attr_data;
                }
            }
            catch (HDF5DataFiltersException exfltr) {
                log.debug("AttributeCommonIO():read ioType read failure: ", exfltr);
                throw new Exception("Filter not available exception: " + exfltr.getMessage(), exfltr);
            }
            catch (Exception ex) {
                log.debug("AttributeCommonIO():read ioType read failure: ", ex);
                throw new Exception(ex.getMessage(), ex);
            }
            finally {
                dsDatatype.close(tid);
            }
            log.trace("AttributeCommonIO():read ioType data: {}", theData);
            originalBuf = theData;
            isDataLoaded = true;
        } // IO_TYPE.READ
        else {
            /*
             * Perform any necessary data conversions before writing the data.
             *
             * Note that v-len strings do not get converted, regardless of
             * conversion request type.
             */
            Object tmpData = objBuf;
            try {
                // Check if we need to convert integer data
                String cname = objBuf.getClass().getName();
                char dname = cname.charAt(cname.lastIndexOf("[") + 1);
                boolean doIntConversion = (((dt_size == 1) && (dname == 'S')) || ((dt_size == 2) && (dname == 'I'))
                        || ((dt_size == 4) && (dname == 'J')) || (dsDatatype.isUnsigned() && unsignedConverted));

                if (doIntConversion) {
                    log.trace("AttributeCommonIO(): converting integer data to unsigned C-type integers");
                    tmpData = convertToUnsignedC(objBuf, null);
                }
                else if (dsDatatype.isText() && !dsDatatype.isVarStr() && convertByteToString) {
                    log.trace("AttributeCommonIO(): converting string array to byte array");
                    tmpData = stringToByte((String[]) objBuf, (int)dt_size);
                }
                else if (dsDatatype.isEnum() && (Array.get(objBuf, 0) instanceof String)) {
                    log.trace("AttributeCommonIO(): converting enum names to values");
                    tmpData = dsDatatype.convertEnumNameToValue((String[]) objBuf);
                }
                else if (dsDatatype.isFloat() && dsDatatype.getDatatypeSize() == 16) {
                    log.trace("AttributeCommonIO(): isFloat: converting BigDecimal array to byte array");
                    throw new Exception("data conversion failure: cannot write BigDecimal values");
                    //tmpData = dsDatatype.bigDecimalToByte(0, (int)nPoints, (BigDecimal[]) objBuf);
                }
            }
            catch (Exception ex) {
                log.debug("AttributeCommonIO(): data conversion failure: ", ex);
                throw new Exception("data conversion failure: " + ex.getMessage());
            }

            /*
             * Actually write the data now that everything has been setup.
             */
            try {
                if (dsDatatype.isVLEN() || (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isVLEN())) {
                    log.trace("AttributeCommonIO(): H5AwriteVL aid={} tid={}", attr_id, tid);

                    H5.H5AwriteVL(attr_id, tid, (Object[]) tmpData);
                }
                else {
                    if (dsDatatype.isRef() && tmpData instanceof String) {
                        // reference is a path+name to the object
                        tmpData = H5.H5Rcreate(getFID(), (String) tmpData, HDF5Constants.H5R_OBJECT, -1);
                        log.trace("AttributeCommonIO(): Attribute class is CLASS_REFERENCE");
                    }
                    else if (Array.get(tmpData, 0) instanceof String) {
                        int len = ((String[]) tmpData).length;
                        byte[] bval = Dataset.stringToByte((String[]) tmpData, (int)dt_size);
                        if (bval != null && bval.length == dt_size * len) {
                            bval[bval.length - 1] = 0;
                            tmpData = bval;
                        }
                        log.trace("AttributeCommonIO(): String={}: {}", tmpData);
                    }

                    log.trace("AttributeCommonIO(): H5Awrite aid={} tid={}", attr_id, tid);
                    H5.H5Awrite(attr_id, tid, tmpData);
                }
            }
            catch (Exception ex) {
                log.debug("AttributeCommonIO(): write failure: ", ex);
                throw new Exception(ex.getMessage());
            }
            finally {
                dsDatatype.close(tid);
            }
        } // IO_TYPE.WRITE

        return theData;
    }

    protected Object AttributeSelection() throws Exception {
        H5Datatype dsDatatype = (H5Datatype) getDatatype();
        Object theData = H5Datatype.allocateArray(dsDatatype, (int)nPoints);
        if (dsDatatype.isText() && convertByteToString && (theData instanceof byte[])) {
            log.trace("AttributeSelection(): isText: converting byte array to string array");
            theData = byteToString((byte[]) theData, (int) dsDatatype.getDatatypeSize());
        }
        else if (dsDatatype.isFloat() && dsDatatype.getDatatypeSize() == 16) {
            log.trace("AttributeSelection(): isFloat: converting byte array to BigDecimal array");
            theData = dsDatatype.byteToBigDecimal(0, (int)nPoints, (byte[]) theData);
        }
        else if (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isFloat() && dsDatatype.getDatatypeBase().getDatatypeSize() == 16) {
            log.trace("AttributeSelection(): isArray and isFloat: converting byte array to BigDecimal array");
            long[] arrayDims = dsDatatype.getArrayDims();
            int asize = (int)nPoints;
            for (int j = 0; j < arrayDims.length; j++) {
                asize *= arrayDims[j];
            }
            theData = ((H5Datatype)dsDatatype.getDatatypeBase()).byteToBigDecimal(0, asize, (byte[]) theData);
        }
        else if (dsDatatype.isRefObj()) {
            log.trace("AttributeSelection(): isREF: converting byte array to long array");
            theData = HDFNativeData.byteToLong((byte[]) theData);
        }
        Object theOrig = originalBuf;
        log.trace("scalarAttributeSelection(): originalBuf={}", originalBuf);

        //Copy the selection from originalBuf to theData
        //Only three dims are involved and selected data is 2 dimensions
        //    selectedDims[selectedIndex[0]] is the row dimension
        //    selectedDims[selectedIndex[1]] is the col dimension
        //    selectedDims[selectedIndex[2]] is the frame dimension
        long[] start = getStartDims();
        long curFrame = start[selectedIndex[2]];
        for (int col = 0; col < (int)selectedDims[selectedIndex[1]]; col++) {
            for (int row = 0; row < (int)selectedDims[selectedIndex[0]]; row++) {

                int k = (int)startDims[selectedIndex[2]] * (int)selectedDims[selectedIndex[2]];
                int index = row * (int)selectedDims[selectedIndex[1]] + col;
                log.trace("scalarAttributeSelection(): point{} row:col:k={}:{}:{}", curFrame, row, col, k);
                int fromIndex = ((int)curFrame * (int)selectedDims[selectedIndex[1]] * (int)selectedDims[selectedIndex[0]] +
                                        col * (int)selectedDims[selectedIndex[0]] +
                                        row);// * (int) dsDatatype.getDatatypeSize();
                int toIndex = (col * (int)selectedDims[selectedIndex[0]] +
                        row);// * (int) dsDatatype.getDatatypeSize();
                int objSize = 1;
                if (dsDatatype.isArray()) {
                    long[] arrayDims = dsDatatype.getArrayDims();
                    objSize = (int)arrayDims.length;
                }
                System.arraycopy(theOrig, fromIndex, theData, toIndex, objSize);
            }
        }

        log.trace("scalarAttributeSelection(): theData={}", theData);
        return theData;
    }
}
