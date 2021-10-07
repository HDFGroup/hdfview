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

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public abstract class AttributeDataset extends Dataset {

    private static final long serialVersionUID = 2072473407027648309L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttributeDataset.class);

    /** The HObject to which this AttributeDataset is attached */
    protected HObject         parentObject;

    /** additional information and properties for the attribute */
    private transient Map<String, Object> properties;

    /** Flag to indicate if the attribute data is a single scalar point */
    protected final boolean   isScalar;

    /**
     * Flag to indicate is the original unsigned C data is converted.
     */
    protected boolean         unsignedConverted;

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
    public AttributeDataset(HObject parentObj, String attrName, Datatype attrType, long[] attrDims) {
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
    public AttributeDataset(HObject parentObj, String attrName, Datatype attrType, long[] attrDims, Object attrValue) {
        super((parentObj == null) ? null : parentObj.getFileFormat(), attrName,
                (parentObj == null) ? null : parentObj.getFullName(), null);

        log.trace("AttributeDataset: start {}", parentObj);
        this.parentObject = parentObj;

        datatype = attrType;

        if (attrValue != null) {
            data = attrValue;
            originalBuf = attrValue;
            isDataLoaded = true;
        }
        properties = new HashMap();

        if (attrDims == null) {
            rank = 1;
            dims = new long[] { 1 };
            isScalar = true;
        }
        else {
            dims = attrDims;
            rank = dims.length;
            isScalar = false;
        }

        selectedDims = new long[rank];
        startDims = new long[rank];
        selectedStride = new long[rank];

        log.trace("attrName={}, attrType={}, attrValue={}",
                attrName, attrType.getDescription(), data);
    }

    /**
     * Returns the HObject to which this AttributeDataset is currently "attached".
     *
     * @return the HObject to which this AttributeDataset is currently "attached".
     */
    public HObject getParentObject() {
        return parentObject;
    }

    /**
     * Sets the HObject to which this AttributeDataset is "attached".
     *
     * @param pObj
     *            the new HObject to which this AttributeDataset is "attached".
     */
    public void setParentObject(HObject pObj) {
        parentObject = pObj;
    }

    /**
     * Converts the data values of this Attribute to appropriate Java integers if
     * they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertFromUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertFromUnsignedC() {
        // Keep a copy of original buffer and the converted buffer
        // so that they can be reused later to save memory
        if ((data != null) && getDatatype().isUnsigned() && !unsignedConverted) {
            originalBuf = data;
            convertedBuf = convertFromUnsignedC(originalBuf, convertedBuf);
            data = convertedBuf;
            unsignedConverted = true;
        }

        return data;
    }

    /**
     * Converts Java integer data values of this Attribute back to unsigned C-type
     * integer data if they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertToUnsignedC(Object, Object)
     * @see #convertFromUnsignedC(Object data_in)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertToUnsignedC() {
        // Keep a copy of original buffer and the converted buffer
        // so that they can be reused later to save memory
        if ((data != null) && getDatatype().isUnsigned()) {
            convertedBuf = data;
            originalBuf = convertToUnsignedC(convertedBuf, originalBuf);
            data = originalBuf;
        }

        return data;
    }

    @Override
    public Object getFillValue() {
        return null;
    }

    @Override
    public void clearData() {
        super.clearData();
    }

    protected void resetSelection() {
        for (int i = 0; i < rank; i++) {
            startDims[i] = 0;
            selectedDims[i] = 1;
            if (selectedStride != null) {
                selectedStride[i] = 1;
            }
        }

        if (rank == 1) {
            selectedIndex[0] = 0;
            selectedDims[0] = dims[0];
        }
        else if (rank == 2) {
            selectedIndex[0] = 0;
            selectedIndex[1] = 1;
            selectedDims[0] = dims[0];
            selectedDims[1] = dims[1];
        }
        else if (rank > 2) {
            selectedIndex[0] = 0; // width, the fastest dimension
            selectedIndex[1] = 1; // height
            selectedIndex[2] = 2; // frames

            selectedDims[selectedIndex[0]] = dims[selectedIndex[0]];
            selectedDims[selectedIndex[1]] = dims[selectedIndex[1]];
            selectedDims[selectedIndex[2]] = dims[selectedIndex[2]];
        }
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

    /**
     * @return true if the data is a single scalar point; otherwise, returns
     *         false.
     */
    public boolean isScalar() {
        return isScalar;
    }

    /**
     * set a property for the attribute.
     *
     * @param key the attribute Map key
     * @param value the attribute Map value
     */
    public void setProperty(String key, Object value)
    {
        properties.put(key, value);
    }

    /**
     * get a property for a given key.
     *
     * @param key the attribute Map key
     *
     * @return the property
     */
    public Object getProperty(String key)
    {
        return properties.get(key);
    }

    /**
     * get all property keys.
     *
     * @return the Collection of property keys
     */
    public Collection<String> getPropertyKeys()
    {
        return properties.keySet();
    }

    @Override
    public Datatype getDatatype() {
        return datatype;
    }

    @Override
    public Object read() throws Exception, OutOfMemoryError {
        if (!inited) init();

        return data;
    }

    @Override
    public void write(Object buf) throws Exception {
        log.trace("AttributeDataset: write start");
        if (!buf.equals(data))
            setData(buf);

        init();

        if (parentObject == null) {
            log.debug("write(Object): parent object is null; nowhere to write attribute to");
            return;
        }

        ((MetaDataContainer) getParentObject()).writeMetadata(this);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List getMetadata() throws Exception {
        throw new UnsupportedOperationException("AttributeDataset:getMetadata Unsupported operation.");
    }

    @Override
    public void writeMetadata(Object metadata) throws Exception {
        throw new UnsupportedOperationException("AttributeDataset:writeMetadata Unsupported operation.");
    }

    @Override
    public void removeMetadata(Object metadata) throws Exception {
        throw new UnsupportedOperationException("AttributeDataset:removeMetadata Unsupported operation.");
    }

    @Override
    public void updateMetadata(Object metadata) throws Exception {
        throw new UnsupportedOperationException("AttributeDataset:updateMetadata Unsupported operation.");
    }

    @Override
    public boolean hasAttribute() {
        return false;
    }

    @Override
    public byte[] readBytes() throws Exception {
        throw new UnsupportedOperationException("AttributeDataset:readBytes Unsupported operation.");
    }

    @Override
    public Dataset copy(Group pgroup, String name, long[] dims, Object data) throws Exception {
        throw new UnsupportedOperationException("AttributeDataset:copy Unsupported operation.");
    }

    /**
     * Returns whether this AttributeDataset is equal to the specified HObject by comparing
     * various properties.
     *
     * @param obj
     *            The object
     *
     * @return true if the object is equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        // checking if both the object references are
        // referring to the same object.
        if (this == obj)
            return true;
        if (obj instanceof AttributeDataset) {
            if (!this.getFullName().equals(((AttributeDataset) obj).getFullName()))
                return false;

            if (!this.getFileFormat().equals(((AttributeDataset) obj).getFileFormat()))
                return false;

            if (!Arrays.equals(this.getDims(), ((DataFormat) obj).getDims()))
                return false;

            return (this.getParentObject().equals(((AttributeDataset) obj).getParentObject()));
        }
        return false;
    }

    @Override
    public int hashCode() {

        // We are returning the OID as a hashcode value.
        return super.hashCode();
    }

    /**
     * Returns a string representation of the data value of the attribute. For
     * example, "0, 255".
     * <p>
     * For a compound datatype, it will be a 1D array of strings with field
     * members separated by the delimiter. For example,
     * "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of {int,
     * float} of three data points.
     * <p>
     *
     * @param delimiter
     *            The delimiter used to separate individual data points. It
     *            can be a comma, semicolon, tab or space. For example,
     *            toString(",") will separate data by commas.
     *
     * @return the string representation of the data values.
     */
    public String toString(String delimiter) {
        return toString(delimiter, -1);
    }

    /**
     * Returns a string representation of the data value of the attribute. For
     * example, "0, 255".
     * <p>
     * For a compound datatype, it will be a 1D array of strings with field
     * members separated by the delimiter. For example,
     * "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of {int,
     * float} of three data points.
     * <p>
     *
     * @param delimiter
     *            The delimiter used to separate individual data points. It
     *            can be a comma, semicolon, tab or space. For example,
     *            toString(",") will separate data by commas.
     * @param maxItems
     *            The maximum number of Array values to return
     *
     * @return the string representation of the data values.
     */
    public String toString(String delimiter, int maxItems) {
        Object theData = originalBuf;
        if (theData == null) {
            log.debug("toString: value is null");
            return null;
        }

        if (theData instanceof List<?>) {
            log.trace("toString: value is list");
            return null;
//            for (int i = 0; i < ((ArrayList<Object[]>)theData).size(); i++) {
//                Object theobj = ((ArrayList<Object[]>)theData).get(i);
//                return theobj.toString();
//            }
        }

        Class<? extends Object> valClass = theData.getClass();

        if (!valClass.isArray()) {
            log.trace("toString: finish - not array");
            String strValue = theData.toString();
            if (maxItems > 0 && strValue.length() > maxItems) {
                // truncate the extra characters
                strValue = strValue.substring(0, maxItems);
            }
            return strValue;
        }

        // attribute value is an array
        StringBuilder sb = new StringBuilder();
        int n = Array.getLength(theData);
        if ((maxItems > 0) && (n > maxItems))
            n = maxItems;

        log.trace("toString: is_enum={} is_unsigned={} Array.getLength={}", getDatatype().isEnum(),
                getDatatype().isUnsigned(), n);

        if (getDatatype().isEnum()) {
            String cname = valClass.getName();
            char dname = cname.charAt(cname.lastIndexOf('[') + 1);
            log.trace("toString: is_enum with cname={} dname={}", cname, dname);

            Map<String, String> map = this.getDatatype().getEnumMembers();
            String theValue = null;
            switch (dname) {
                case 'B':
                    byte[] barray = (byte[]) theData;
                    short sValue = barray[0];
                    theValue = String.valueOf(sValue);
                    if (map.containsKey(theValue)) {
                        sb.append(map.get(theValue));
                    }
                    else
                        sb.append(sValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        sValue = barray[i];
                        theValue = String.valueOf(sValue);
                        if (map.containsKey(theValue)) {
                            sb.append(map.get(theValue));
                        }
                        else
                            sb.append(sValue);
                    }
                    break;
                case 'S':
                    short[] sarray = (short[]) theData;
                    int iValue = sarray[0];
                    theValue = String.valueOf(iValue);
                    if (map.containsKey(theValue)) {
                        sb.append(map.get(theValue));
                    }
                    else
                        sb.append(iValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        iValue = sarray[i];
                        theValue = String.valueOf(iValue);
                        if (map.containsKey(theValue)) {
                            sb.append(map.get(theValue));
                        }
                        else
                            sb.append(iValue);
                    }
                    break;
                case 'I':
                    int[] iarray = (int[]) theData;
                    long lValue = iarray[0];
                    theValue = String.valueOf(lValue);
                    if (map.containsKey(theValue)) {
                        sb.append(map.get(theValue));
                    }
                    else
                        sb.append(lValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        lValue = iarray[i];
                        theValue = String.valueOf(lValue);
                        if (map.containsKey(theValue)) {
                            sb.append(map.get(theValue));
                        }
                        else
                            sb.append(lValue);
                    }
                    break;
                case 'J':
                    long[] larray = (long[]) theData;
                    Long l = larray[0];
                    theValue = Long.toString(l);
                    if (map.containsKey(theValue)) {
                        sb.append(map.get(theValue));
                    }
                    else
                        sb.append(theValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        l = larray[i];
                        theValue = Long.toString(l);
                        if (map.containsKey(theValue)) {
                            sb.append(map.get(theValue));
                        }
                        else
                            sb.append(theValue);
                    }
                    break;
                default:
                    sb.append(Array.get(theData, 0));
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        sb.append(Array.get(theData, i));
                    }
                    break;
            }
        }
        else if (getDatatype().isUnsigned()) {
            String cname = valClass.getName();
            char dname = cname.charAt(cname.lastIndexOf('[') + 1);
            log.trace("toString: is_unsigned with cname={} dname={}", cname, dname);

            switch (dname) {
                case 'B':
                    byte[] barray = (byte[]) theData;
                    short sValue = barray[0];
                    if (sValue < 0) {
                        sValue += 256;
                    }
                    sb.append(sValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        sValue = barray[i];
                        if (sValue < 0) {
                            sValue += 256;
                        }
                        sb.append(sValue);
                    }
                    break;
                case 'S':
                    short[] sarray = (short[]) theData;
                    int iValue = sarray[0];
                    if (iValue < 0) {
                        iValue += 65536;
                    }
                    sb.append(iValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        iValue = sarray[i];
                        if (iValue < 0) {
                            iValue += 65536;
                        }
                        sb.append(iValue);
                    }
                    break;
                case 'I':
                    int[] iarray = (int[]) theData;
                    long lValue = iarray[0];
                    if (lValue < 0) {
                        lValue += 4294967296L;
                    }
                    sb.append(lValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        lValue = iarray[i];
                        if (lValue < 0) {
                            lValue += 4294967296L;
                        }
                        sb.append(lValue);
                    }
                    break;
                case 'J':
                    long[] larray = (long[]) theData;
                    Long l = larray[0];
                    String theValue = Long.toString(l);
                    if (l < 0) {
                        l = (l << 1) >>> 1;
                        BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                        BigInteger big2 = new BigInteger(l.toString());
                        BigInteger big = big1.add(big2);
                        theValue = big.toString();
                    }
                    sb.append(theValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        l = larray[i];
                        theValue = Long.toString(l);
                        if (l < 0) {
                            l = (l << 1) >>> 1;
                            BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                            BigInteger big2 = new BigInteger(l.toString());
                            BigInteger big = big1.add(big2);
                            theValue = big.toString();
                        }
                        sb.append(theValue);
                    }
                    break;
                default:
                    String strValue = Array.get(theData, 0).toString();
                    if (maxItems > 0 && strValue.length() > maxItems) {
                        // truncate the extra characters
                        strValue = strValue.substring(0, maxItems);
                    }
                    sb.append(strValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        strValue = Array.get(theData, i).toString();
                        if (maxItems > 0 && strValue.length() > maxItems) {
                            // truncate the extra characters
                            strValue = strValue.substring(0, maxItems);
                        }
                        sb.append(strValue);
                    }
                    break;
            }
        }
        else {
            log.trace("toString: not enum or unsigned");
            Object value = Array.get(theData, 0);
            String strValue;

            if (value == null) {
                strValue = "null";
            }
            else {
                strValue = value.toString();
            }

            if (maxItems > 0 && strValue.length() > maxItems) {
                // truncate the extra characters
                strValue = strValue.substring(0, maxItems);
            }
            sb.append(strValue);

            for (int i = 1; i < n; i++) {
                sb.append(delimiter);
                value = Array.get(theData, i);

                if (value == null) {
                    strValue = "null";
                }
                else {
                    strValue = value.toString();
                }

                if (maxItems > 0 && strValue.length() > maxItems) {
                    // truncate the extra characters
                    strValue = strValue.substring(0, maxItems);
                }
                sb.append(strValue);
            }
        }

        return sb.toString();
    }
}
