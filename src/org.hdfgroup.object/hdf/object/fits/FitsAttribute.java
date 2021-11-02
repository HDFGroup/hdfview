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

package hdf.object.fits;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import hdf.object.AttributeDataset;
import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.HObject;

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
 * FitsAttribute dataRange = new FitsAttribute(name, type, dims);
 * // Set the attribute value
 * dataRange.setValue(value);
 * // See FileFormat.writeAttribute() for how to attach an attribute to an object,
 * &#64;see hdf.object.FileFormat#writeAttribute(HObject, AttributeDataset, boolean)
 * </pre>
 *
 *
 * For an atomic datatype, the value of an FitsAttribute will be a 1D array of integers, floats and
 * strings. For a compound datatype, it will be a 1D array of strings with field members separated
 * by a comma. For example, "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of {int,
 * float} of three data points.
 *
 * @see hdf.object.Datatype
 *
 * @version 2.0 4/2/2018
 * @author Peter X. Cao, Jordan T. Henderson
 */
public class FitsAttribute extends AttributeDataset implements DataFormat {

    private static final long serialVersionUID = 2072473407027648309L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FitsAttribute.class);

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
     * FitsAttribute attr = new FitsAttribute(attrName, attrType, attrDims);
     * attr.setValue(classValue);
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
     *
     * @see hdf.object.Datatype
     */
    public FitsAttribute(HObject parentObj, String attrName, Datatype attrType, long[] attrDims) {
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
     * FitsAttribute attr = new FitsAttribute(attrName, attrType, attrDims, classValue);
     * </pre>
     *
     * @param parentObj
     *            the HObject to which this FitsAttribute is attached.
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
    public FitsAttribute(HObject parentObj, String attrName, Datatype attrType, long[] attrDims, Object attrValue) {
        super(parentObj, attrName, attrType, attrDims, attrValue);

        log.trace("FitsAttribute: start {}", parentObj);

        unsignedConverted = false;

        log.trace("attrName={}, attrType={}, attrValue={}, rank={}, isUnsigned={}, isScalar={}",
                attrName, getDatatype().getDescription(), data, rank, getDatatype().isUnsigned(), isScalar);

        resetSelection();
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        if (parentObject == null) {
            log.debug("open(): attribute's parent object is null");
            return -1;
        }

        return -1;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long aid) {
    }

    @Override
    public void init() {
        if (inited) {
            resetSelection();
            log.trace("init(): FitsAttribute already inited");
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#clearData()
     */
    @Override
    public void clearData() {
        super.clearData();
        unsignedConverted = false;
    }

    @Override
    public Object read() throws Exception, OutOfMemoryError {
        if (!inited) init();

        return data;
    }
}
