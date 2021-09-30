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
public class Attribute extends AttributeDataset implements DataFormat, CompoundDataFormat {

    private static final long serialVersionUID = 2072473407027648309L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Attribute.class);

    /** Fields for Compound datatype attributes */

    /**
     * A list of names of all compound fields including nested fields.
     * <p>
     * The nested names are separated by CompoundDS.SEPARATOR. For example, if
     * compound attribute "A" has the following nested structure,
     *
     * <pre>
     * A --&gt; m01
     * A --&gt; m02
     * A --&gt; nest1 --&gt; m11
     * A --&gt; nest1 --&gt; m12
     * A --&gt; nest1 --&gt; nest2 --&gt; m21
     * A --&gt; nest1 --&gt; nest2 --&gt; m22
     * i.e.
     * A = { m01, m02, nest1{m11, m12, nest2{ m21, m22}}}
     * </pre>
     *
     * The flatNameList of compound attribute "A" will be {m01, m02, nest1[m11,
     * nest1[m12, nest1[nest2[m21, nest1[nest2[m22}
     *
     */
    protected List<String> flatNameList;

    /**
     * A list of datatypes of all compound fields including nested fields.
     */
    protected List<Datatype> flatTypeList;

    /**
     * The number of members of the compound attribute.
     */
    protected int numberOfMembers = 0;

    /**
     * The names of the members of the compound attribute.
     */
    protected String[] memberNames = null;

    /**
     * Array containing the total number of elements of the members of this compound
     * attribute.
     * <p>
     * For example, a compound attribute COMP has members of A, B and C as
     *
     * <pre>
     *     COMP {
     *         int A;
     *         float B[5];
     *         double C[2][3];
     *     }
     * </pre>
     *
     * memberOrders is an integer array of {1, 5, 6} to indicate that member A has
     * one element, member B has 5 elements, and member C has 6 elements.
     */
    protected int[] memberOrders = null;

    /**
     * The dimension sizes of each member.
     * <p>
     * The i-th element of the Object[] is an integer array (int[]) that contains
     * the dimension sizes of the i-th member.
     */
    protected transient Object[] memberDims = null;

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
     * AttributeDataset attr = new Attribute(attrName, attrType, attrDims);
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
    public Attribute(HObject parentObj, String attrName, Datatype attrType, long[] attrDims) {
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
     * Attribute attr = new Attribute(attrName, attrType, attrDims, classValue);
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
    public Attribute(HObject parentObj, String attrName, Datatype attrType, long[] attrDims, Object attrValue) {
        super(parentObj, attrName, attrType, attrDims, attrValue);

        log.trace("Attribute: start {}", parentObj);

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
            log.trace("init(): Attribute already inited");
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

        /*
         * TODO: For now, convert a compound Attribute's data (String[]) into a List for
         * convenient processing
         */
        if (getDatatype().isCompound() && !(data instanceof List)) {
            List<String> valueList = Arrays.asList((String[]) data);

            data = valueList;
        }

        return data;
    }

    /**
     * Returns the number of members of the compound attribute.
     *
     * @return the number of members of the compound attribute.
     */
    @Override
    public int getMemberCount() {
        return numberOfMembers;
    }

    /**
     * Returns the names of the members of the compound attribute. The names of
     * compound members are stored in an array of Strings.
     * <p>
     * For example, for a compound datatype of {int A, float B, char[] C}
     * getMemberNames() returns ["A", "B", "C"}.
     *
     * @return the names of compound members.
     */
    @Override
    public String[] getMemberNames() {
        return memberNames;
    }

    /**
     * Returns an array of the names of the selected members of the compound dataset.
     *
     * @return an array of the names of the selected members of the compound dataset.
     */
    public final String[] getSelectedMemberNames() {
        if (isMemberSelected == null) {
            log.debug("getSelectedMemberNames(): isMemberSelected array is null");
            return memberNames;
        }

        int idx = 0;
        String[] names = new String[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i]) {
                names[idx++] = memberNames[i];
            }
        }

        return names;
    }

    /**
     * Checks if a member of the compound attribute is selected for read/write.
     *
     * @param idx
     *            the index of compound member.
     *
     * @return true if the i-th memeber is selected; otherwise returns false.
     */
    @Override
    public boolean isMemberSelected(int idx) {
        if ((isMemberSelected != null) && (isMemberSelected.length > idx)) {
            return isMemberSelected[idx];
        }

        return false;
    }

    /**
     * Selects the i-th member for read/write.
     *
     * @param idx
     *            the index of compound member.
     */
    @Override
    public void selectMember(int idx) {
        if ((isMemberSelected != null) && (isMemberSelected.length > idx)) {
            isMemberSelected[idx] = true;
        }
    }

    /**
     * Selects/deselects all members.
     *
     * @param selectAll
     *            The indicator to select or deselect all members. If true, all
     *            members are selected for read/write. If false, no member is
     *            selected for read/write.
     */
    @Override
    public void setAllMemberSelection(boolean selectAll) {
        if (isMemberSelected == null) {
            return;
        }

        for (int i = 0; i < isMemberSelected.length; i++) {
            isMemberSelected[i] = selectAll;
        }
    }

    /**
     * Returns array containing the total number of elements of the members of the
     * compound attribute.
     * <p>
     * For example, a compound attribute COMP has members of A, B and C as
     *
     * <pre>
     *     COMP {
     *         int A;
     *         float B[5];
     *         double C[2][3];
     *     }
     * </pre>
     *
     * getMemberOrders() will return an integer array of {1, 5, 6} to indicate that
     * member A has one element, member B has 5 elements, and member C has 6
     * elements.
     *
     * @return the array containing the total number of elements of the members of
     *         the compound attribute.
     */
    @Override
    public int[] getMemberOrders() {
        return memberOrders;
    }

    /**
     * Returns array containing the total number of elements of the selected members
     * of the compound attribute.
     *
     * <p>
     * For example, a compound attribute COMP has members of A, B and C as
     *
     * <pre>
     *     COMP {
     *         int A;
     *         float B[5];
     *         double C[2][3];
     *     }
     * </pre>
     *
     * If A and B are selected, getSelectedMemberOrders() returns an array of {1, 5}
     *
     * @return array containing the total number of elements of the selected members
     *         of the compound attribute.
     */
    @Override
    public int[] getSelectedMemberOrders() {
        if (isMemberSelected == null) {
            log.debug("getSelectedMemberOrders(): isMemberSelected array is null");
            return memberOrders;
        }

        int idx = 0;
        int[] orders = new int[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i]) {
                orders[idx++] = memberOrders[i];
            }
        }

        return orders;
    }

    /**
     * Returns the dimension sizes of the i-th member.
     * <p>
     * For example, a compound attribute COMP has members of A, B and C as
     *
     * <pre>
     *     COMP {
     *         int A;
     *         float B[5];
     *         double C[2][3];
     *     }
     * </pre>
     *
     * getMemberDims(2) returns an array of {2, 3}, while getMemberDims(1) returns
     * an array of {5}, and getMemberDims(0) returns null.
     *
     * @param i
     *            the i-th member
     *
     * @return the dimension sizes of the i-th member, null if the compound member
     *         is not an array.
     */
    @Override
    public int[] getMemberDims(int i) {
        if (memberDims == null) {
            return null;
        }

        return (int[]) memberDims[i];
    }

    /**
     * Returns an array of datatype objects of compound members.
     * <p>
     * Each member of a compound attribute has its own datatype. The datatype of a
     * member can be atomic or other compound datatype (nested compound). The
     * datatype objects are setup at init().
     * <p>
     *
     * @return the array of datatype objects of the compound members.
     */
    @Override
    public Datatype[] getMemberTypes() {
        return memberTypes;
    }

    /**
     * Given an array of bytes representing a compound Datatype and a start index
     * and length, converts len number of bytes into the correct Object type and
     * returns it.
     *
     * @param data
     *            The byte array representing the data of the compound Datatype
     * @param data_type
     *            The type of data to convert the bytes to
     * @param start
     *            The start index of the bytes to get
     * @param len
     *            The number of bytes to convert
     * @return The converted type of the bytes
     */
    protected Object convertCompoundByteMember(byte[] data, long data_type, long start, long len) {
        return null;
    }
}
