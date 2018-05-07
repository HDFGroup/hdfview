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
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.object.h5.H5Datatype;

/**
 * An attribute is a (name, value) pair of metadata attached to a primary data
 * object such as a dataset, group or named datatype.
 * <p>
 * Like a dataset, an attribute has a name, datatype and dataspace.
 *
 * <p>
 * For more details on attributes, <a href=
 * "https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5
 * User's Guide</a>
 * <p>
 *
 * The following code is an example of an attribute with 1D integer array of two
 * elements.
 *
 * <pre>
 * // Example of creating a new attribute
 * // The name of the new attribute
 * String name = "Data range";
 * // Creating an unsigned 1-byte integer datatype
 * Datatype type = new Datatype(Datatype.CLASS_INTEGER, // class
 *                              1,                      // size in bytes
 *                              Datatype.ORDER_LE,      // byte order
 *                              Datatype.SIGN_NONE);    // signed or unsigned
 * // 1-D array of size two
 * long[] dims = {2};
 * // The value of the attribute
 * int[] value = {0, 255};
 * // Create a new attribute
 * Attribute dataRange = new Attribute(name, type, dims);
 * // Set the attribute value
 * dataRange.setValue(value);
 * // See FileFormat.writeAttribute() for how to attach an attribute to an object,
 * &#64;see hdf.object.FileFormat#writeAttribute(HObject, Attribute, boolean)
 * </pre>
 *
 * @see hdf.object.Datatype
 *
 * @version 2.0 4/2/2018
 * @author Peter X. Cao, Jordan T. Henderson
 */
public class Attribute extends HObject implements DataFormat, CompoundDataFormat {

    private static final long serialVersionUID = 2072473407027648309L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Attribute.class);

    private boolean           inited = false;

    /** The HObject to which this Attribute is attached */
    protected HObject         parentObject;

    /** The datatype of the attribute. */
    private final Datatype    type;

    /** The rank of the data value of the attribute. */
    private int               rank;

    /** The dimension sizes of the attribute. */
    private long[]            dims;

    private long[]            selectedDims;

    private long[]            startDims;

    private long[]            selectedStride;

    private final int[]       selectedIndex;

    /** The value of the attribute. */
    private Object            value;

    /** additional information and properties for the attribute */
    private Map<String, Object>  properties;

    /** Flag to indicate if the datatype is an unsigned integer. */
    private final boolean     isUnsigned;

    /** Flag to indicate if the data is text */
    protected final boolean   isTextData;

    /** Flag to indicate if the attribute data is a single scalar point */
    protected final boolean   isScalar;

    /** Flag to indicate if the attribute has a compound datatype */
    protected final boolean   isCompound;

    /** Fields for Compound datatype attributes */

    /**
     * A list of names of all compound fields including nested fields.
     * <p>
     * The nested names are separated by CompoundDS.separator. For example, if
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
    private List<String> flatNameList;

    /**
     * A list of datatypes of all compound fields including nested fields.
     */
    private List<Long> flatTypeList;

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
    protected Object[] memberDims = null;

    /**
     * The datatypes of the compound attribute's members.
     */
    protected Datatype[] memberTypes = null;

    /**
     * The array to store flags to indicate if a member of this compound attribute
     * is selected for read/write.
     * <p>
     * If a member is selected, the read/write will perform on the member.
     * Applications such as HDFView will only display the selected members of the
     * compound attribute.
     *
     * <pre>
     * For example, if a compound attribute has four members
     *     String[] memberNames = {"X", "Y", "Z", "TIME"};
     * and
     *     boolean[] isMemberSelected = {true, false, false, true};
     * members "X" and "TIME" are selected for read and write.
     * </pre>
     */
    protected boolean[] isMemberSelected = null;

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
     * Datatype attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, -1, -1);
     * Attribute attr = new Attribute(attrName, attrType, attrDims);
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
     * Datatype attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, -1, -1);
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
        super((parentObj == null) ? null : parentObj.getFileFormat(), attrName,
                (parentObj == null) ? null : parentObj.getFullName(), null);

        log.trace("Attribute: start");

        this.parentObject = parentObj;

        type = attrType;
        dims = attrDims;
        value = attrValue;
        properties = new HashMap();
        rank = -1;

        isScalar = (dims == null);
        isUnsigned = (type.getDatatypeSign() == Datatype.SIGN_NONE);
        isTextData = (type.getDatatypeClass() == Datatype.CLASS_STRING);
        isCompound = (type.getDatatypeClass() == Datatype.CLASS_COMPOUND);

        if (dims == null) {
            rank = 1;
            dims = new long[] { 1 };
        }
        else {
            rank = dims.length;
        }

        selectedDims = new long[rank];
        startDims = new long[rank];
        selectedStride = new long[rank];

        selectedIndex = new int[3];
        selectedIndex[0] = 0;
        selectedIndex[1] = 1;
        selectedIndex[2] = 2;

        log.trace("Attribute: {}, attrType={}, attrValue={}, rank={}, isUnsigned={}, isScalar={}", attrName, type,
                value, rank, isUnsigned, isScalar);

        resetSelection();

        log.trace("Attribute: finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        log.trace("open(): start");

        if (parentObject == null) {
            log.debug("open(): attribute's parent object is null");
            log.trace("open(): finish");
            return -1;
        }

        long aid = -1;
        long pObjID = -1;

        try {
            pObjID = parentObject.open();
            if (pObjID >= 0) {
                if (H5.H5Aexists(pObjID, getName()))
                    aid = H5.H5Aopen(pObjID, getName(), HDF5Constants.H5P_DEFAULT);
            }

            log.trace("open(): aid={}", aid);
        }
        catch (HDF5Exception ex) {
            log.debug("open(): Failed to open attribute {}: ", getName(), ex);
            aid = -1;
        }
        finally {
            parentObject.close(pObjID);
        }

        log.trace("open(): finish");

        return aid;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long aid) {
        log.trace("close(): start");

        if (aid >= 0) {
            try {
                H5.H5Aclose(aid);
            }
            catch (HDF5Exception ex) {
                log.debug("close(): H5Aclose({}) failure: ", aid, ex);
            }
        }

        log.trace("close(): finish");
    }

    @Override
    public void init() {
        log.trace("init(): start");

        if (inited) {
            resetSelection();
            log.trace("init(): Attribute already inited");
            log.trace("init(): finish");
            return;
        }

        long aid = -1;
        long tid = -1;
        int tclass = -1;
        flatNameList = new Vector<>();
        flatTypeList = new Vector<>();
        long[] memberTIDs = null;

        aid = open();
        if (aid >= 0) {
            try {
                tid = H5.H5Aget_type(aid);
                tclass = H5.H5Tget_class(tid);

                long tmptid = 0;
                if (tclass == HDF5Constants.H5T_ARRAY) {
                    // array of compound
                    tmptid = tid;
                    tid = H5.H5Tget_super(tmptid);
                    try {
                        H5.H5Tclose(tmptid);
                    }
                    catch (HDF5Exception ex) {
                        log.debug("init(): H5Tclose({}) failure: ", tmptid, ex);
                    }
                }

                if (H5.H5Tget_class(tid) == HDF5Constants.H5T_COMPOUND) {
                    // initialize member information
                    extractCompoundInfo(tid, "", flatNameList, flatTypeList);
                    numberOfMembers = flatNameList.size();
                    log.trace("init(): numberOfMembers={}", numberOfMembers);

                    memberNames = new String[numberOfMembers];
                    memberTIDs = new long[numberOfMembers];
                    memberTypes = new Datatype[numberOfMembers];
                    memberOrders = new int[numberOfMembers];
                    isMemberSelected = new boolean[numberOfMembers];
                    memberDims = new Object[numberOfMembers];

                    for (int i = 0; i < numberOfMembers; i++) {
                        isMemberSelected[i] = true;
                        memberTIDs[i] = flatTypeList.get(i).longValue();
                        memberTypes[i] = new H5Datatype(memberTIDs[i]);
                        memberNames[i] = flatNameList.get(i);
                        memberOrders[i] = 1;
                        memberDims[i] = null;
                        log.trace("init()[{}]: memberNames[{}]={}, memberTIDs[{}]={}, memberTypes[{}]={}", i, i,
                                memberNames[i], i, memberTIDs[i], i, memberTypes[i]);

                        try {
                            tclass = H5.H5Tget_class(memberTIDs[i]);
                        }
                        catch (HDF5Exception ex) {
                            log.debug("init(): H5Tget_class({}) failure: ", memberTIDs[i], ex);
                        }

                        if (tclass == HDF5Constants.H5T_ARRAY) {
                            int n = H5.H5Tget_array_ndims(memberTIDs[i]);
                            long mdim[] = new long[n];
                            H5.H5Tget_array_dims(memberTIDs[i], mdim);
                            int idim[] = new int[n];
                            for (int j = 0; j < n; j++)
                                idim[j] = (int) mdim[j];
                            memberDims[i] = idim;
                            tmptid = H5.H5Tget_super(memberTIDs[i]);
                            memberOrders[i] = (int) (H5.H5Tget_size(memberTIDs[i]) / H5.H5Tget_size(tmptid));
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (HDF5Exception ex) {
                                log.debug("init(): memberTIDs[{}] H5Tclose(tmptid {}) failure: ", i, tmptid, ex);
                            }
                        }
                    } // for (int i=0; i<numberOfMembers; i++)
                }

                inited = true;
            }
            catch (HDF5Exception ex) {
                numberOfMembers = 0;
                memberNames = null;
                memberTypes = null;
                memberOrders = null;
                log.debug("init(): ", ex);
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("init(): H5Tclose({}) failure: ", tid, ex2);
                }

                if (memberTIDs != null) {
                    for (int i = 0; i < memberTIDs.length; i++) {
                        try {
                            H5.H5Tclose(memberTIDs[i]);
                        }
                        catch (Exception ex) {
                            log.debug("init(): H5Tclose(memberTIDs[{}] {}) failure: ", i, memberTIDs[i], ex);
                        }
                    }
                }
            }

            close(aid);
        }

        resetSelection();

        log.trace("init(): finish");
    }

    /**
     * Returns the HObject to which this Attribute is currently "attached".
     *
     * @return the HObject to which this Attribute is currently "attached".
     */
    public HObject getParentObject() {
        return parentObject;
    }

    /**
     * Sets the HObject to which this Attribute is "attached".
     *
     * @param pObj
     *            the new HObject to which this Attribute is "attached".
     */
    public void setParentObject(HObject pObj) {
        parentObject = pObj;
    }

    /**
     * Returns the value of the attribute. For an atomic datatype, this will be
     * a 1D array of integers, floats and strings. For a compound datatype, it
     * will be a 1D array of strings with field members separated by a comma. For
     * example, "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of
     * {int, float} of three data points.
     *
     * @return the value of the attribute, or null if failed to retrieve data
     *         from file.
     */
    @Override
    public Object getData() throws Exception, OutOfMemoryError {
        if (!inited) init();

        /*
         * TODO: For now, convert a compound Attribute's data (String[]) into a List for
         * convenient processing
         */
        if (isCompound) {
            List<String> valueList = Arrays.asList((String[]) value);

            return valueList;
        }

        return value;
    }

    @Override
    public Object getFillValue() {
        /*
         * Currently, Attributes do not support fill values.
         */
        return null;
    }

    /**
     * Sets the value of the attribute. It returns null if it failed to retrieve
     * the name from file.
     *
     * @param data
     *            The value of the attribute to set
     */
    @Override
    public void setData(Object data) {
        value = data;
    }

    @Override
    public void clearData() {
        /* Currently not implemented for Attributes */
        return;
    }

    private void resetSelection() {
        log.trace("resetSelection(): start");

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
            // // hdf-java 2.5 version: 3D dataset is arranged in the order of
            // [frame][height][width] by default
            // selectedIndex[1] = rank-1; // width, the fastest dimension
            // selectedIndex[0] = rank-2; // height
            // selectedIndex[2] = rank-3; // frames

            //
            // (5/4/09) Modified the default dimension order. See bug#1379
            // We change the default order to the following. In most situation,
            // users want to use the natural order of
            // selectedIndex[0] = 0
            // selectedIndex[1] = 1
            // selectedIndex[2] = 2
            // Most of NPOESS data is the the order above.

            selectedIndex[0] = 0; // width, the fastest dimension
            selectedIndex[1] = 1; // height
            selectedIndex[2] = 2; // frames

            selectedDims[selectedIndex[0]] = dims[selectedIndex[0]];
            selectedDims[selectedIndex[1]] = dims[selectedIndex[1]];
            selectedDims[selectedIndex[2]] = dims[selectedIndex[2]];
        }

        log.trace("resetSelection(): finish");
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

    /**
     * Returns the rank (number of dimensions) of the attribute. It returns a
     * negative number if it failed to retrieve the dimension information from
     * file.
     *
     * @return the number of dimensions of the attribute.
     */
    @Override
    public final int getRank() {
        if (!inited) init();

        return rank;
    }

    /**
     * Returns the dimension sizes of the data value of the attribute. It
     * returns null if it failed to retrieve the dimension information from file.
     *
     * @return the dimension sizes of the attribute.
     */
    @Override
    public final long[] getDims() {
        if (!inited) init();

        return dims;
    }

    /**
     * Returns the datatype of the attribute. It returns null if it failed to
     * retrieve the datatype information from file.
     *
     * @return the datatype of the attribute.
     */
    @Override
    public Datatype getDatatype() {
        return type;
    }

    /**
     * At the current time, Attributes do not support compression.
     *
     * @return null
     */
    @Override
    public final String getCompression() {
        return null;
    }

    /**
     * Get Class of the original data buffer if converted.
     *
     * @return the Class of originalBuf
     */
    @Override
    @SuppressWarnings("rawtypes")
    public final Class getOriginalClass() {
        return value.getClass();
    }

    /**
     * @return true if the data is a single scalar point; otherwise, returns
     *         false.
     */
    public boolean isScalar() {
        return isScalar;
    }

    /**
     * Checks if the data type of this attribute is an unsigned integer.
     *
     * @return true if the data type of the attribute is an unsigned integer;
     *         otherwise returns false.
     */
    @Override
    public boolean isUnsigned() {
        return isUnsigned;
    }

    @Override
    public boolean isTextData() {
        return isTextData;
    }

    @Override
    public Object read() throws Exception, OutOfMemoryError {
        if (!inited) init();

        throw new UnsupportedOperationException("Unsupported operation.");
    }

    @Override
    public void write(Object buf) throws Exception {
        setData(buf);
        write();
    }

    @Override
    public void write() throws Exception {
        log.trace("write(): start");

        if (!inited) init();

        if (parentObject == null) {
            log.debug("write(): parent object is null; nowhere to write attribute to");
            log.debug("write(): finish");
            return;
        }

        ((MetaDataContainer) getParentObject()).writeMetadata(this);

        log.trace("write(): finish");
    }

    @Override
    public long[] getSelectedDims() {
        if (!inited) init();

        /*
         * Currently, Attributes do not support subsetting.
         */
        return dims;
    }

    @Override
    public long[] getStartDims() {
        if (!inited) init();

        /*
         * Currently, Attributes do not support subsetting.
         */

        if (startDims == null) {
            startDims = new long[rank];
            for (int i = 0; i < rank; i++) {
                startDims[i] = 0;
            }
        }

        return startDims;
    }

    @Override
    public long[] getStride() {
        if (!inited) init();

        /*
         * Currently, Attributes do not support subsetting.
         */

        if (selectedStride == null) {
            selectedStride = new long[rank];
            for (int i = 0; i < rank; i++) {
                selectedStride[i] = 1;
            }
        }

        return selectedStride;
    }

    @Override
    public int[] getSelectedIndex() {
        if (!inited) init();

        return selectedIndex;
    }

    @Override
    public long getHeight() {
        if (!inited) init();

        if ((selectedDims == null) || (selectedIndex == null)) return 0;

        return selectedDims[selectedIndex[0]];
    }

    @Override
    public long getWidth() {
        if (!inited) init();

        if ((selectedDims == null) || (selectedIndex == null)) return 0;

        if ((selectedDims.length < 2) || (selectedIndex.length < 2)) return 1;

        return selectedDims[selectedIndex[1]];
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
     * Returns the number of selected members of the compound attribute.
     *
     * Selected members are the compound fields which are selected for read/write.
     * <p>
     * For example, in a compound datatype of {int A, float B, char[] C}, users can
     * choose to retrieve only {A, C} from the attribute. In this case,
     * getSelectedMemberCount() returns two.
     *
     * @return the number of selected members.
     */
    @Override
    public int getSelectedMemberCount() {
        int count = 0;

        if (isMemberSelected != null) {
            for (int i = 0; i < isMemberSelected.length; i++) {
                if (isMemberSelected[i]) {
                    count++;
                }
            }
        }

        log.trace("getSelectedMemberCount(): count of selected members={}", count);

        return count;
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
     * Returns an array of datatype objects of selected compound members.
     *
     * @return an array of datatype objects of selected compound members.
     */
    @Override
    public Datatype[] getSelectedMemberTypes() {
        if (isMemberSelected == null) {
            return memberTypes;
        }

        int idx = 0;
        Datatype[] types = new Datatype[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i]) {
                types[idx++] = memberTypes[i];
            }
        }

        return types;
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
        log.trace("toString(): start");

        if (value == null) {
            log.debug("toString(): value is null");
            log.trace("toString(): finish");
            return null;
        }

        Class<? extends Object> valClass = value.getClass();

        if (!valClass.isArray()) {
            log.trace("toString(): finish - not array");
            String strValue = value.toString();
            if (maxItems > 0 && strValue.length() > maxItems) {
                // truncate the extra characters
                strValue = strValue.substring(0, maxItems);
            }
            return strValue;
        }

        // attribute value is an array
        StringBuffer sb = new StringBuffer();
        int n = Array.getLength(value);
        if (maxItems > 0)
            if (n > maxItems)
                n = maxItems;

        boolean is_unsigned = (this.getDatatype().getDatatypeSign() == Datatype.SIGN_NONE);
        boolean is_enum = (this.getDatatype().getDatatypeClass() == Datatype.CLASS_ENUM);
        log.trace("toString: is_enum={} is_unsigned={} Array.getLength={}", is_enum, is_unsigned, n);
        if(is_enum) {
            String cname = valClass.getName();
            char dname = cname.charAt(cname.lastIndexOf("[") + 1);
            log.trace("toString: is_enum with cname={} dname={}", cname, dname);

            String enum_members = this.getDatatype().getEnumMembers();
            log.trace("toString: is_enum enum_members={}", enum_members);
            Map<String,String> map = new HashMap<String,String>();
            String[] entries = enum_members.split(",");
            for (String entry : entries) {
                String[] keyValue = entry.split("=");
                map.put(keyValue[1],keyValue[0]);
                log.trace("toString: is_enum value={} name={}", keyValue[1],keyValue[0]);
            }
            String theValue = null;
            switch (dname) {
                case 'B':
                    byte[] barray = (byte[]) value;
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
                    short[] sarray = (short[]) value;
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
                    int[] iarray = (int[]) value;
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
                    long[] larray = (long[]) value;
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
                    sb.append(Array.get(value, 0));
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        sb.append(Array.get(value, i));
                    }
                    break;
            }
        }
        else if (is_unsigned) {
            String cname = valClass.getName();
            char dname = cname.charAt(cname.lastIndexOf("[") + 1);
            log.trace("toString: is_unsigned with cname={} dname={}", cname, dname);

            switch (dname) {
                case 'B':
                    byte[] barray = (byte[]) value;
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
                    short[] sarray = (short[]) value;
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
                    int[] iarray = (int[]) value;
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
                    long[] larray = (long[]) value;
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
                    String strValue = Array.get(value, 0).toString();
                    if (maxItems > 0 && strValue.length() > maxItems) {
                        // truncate the extra characters
                        strValue = strValue.substring(0, maxItems);
                    }
                    sb.append(strValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        strValue = Array.get(value, i).toString();
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
            String strValue = Array.get(value, 0).toString();
            if (maxItems > 0 && strValue.length() > maxItems) {
                // truncate the extra characters
                strValue = strValue.substring(0, maxItems);
            }
            sb.append(strValue);
            for (int i = 1; i < n; i++) {
                sb.append(delimiter);
                strValue = Array.get(value, i).toString();
                if (maxItems > 0 && strValue.length() > maxItems) {
                    // truncate the extra characters
                    strValue = strValue.substring(0, maxItems);
                }
                sb.append(strValue);
            }
        }

        log.trace("toString: finish");
        return sb.toString();
    }

    /**
     * Extracts compound information into flat structure.
     * <p>
     * For example, compound datatype "nest" has {nest1{a, b, c}, d, e} then
     * extractCompoundInfo() will put the names of nested compound fields into a
     * flat list as
     *
     * <pre>
     * nest.nest1.a
     * nest.nest1.b
     * nest.nest1.c
     * nest.d
     * nest.e
     * </pre>
     *
     * @param tid
     *            the identifier of the compound datatype
     * @param name
     *            the name of the compound datatype
     * @param names
     *            the list to store the member names of the compound datatype
     * @param flatTypeList2
     *            the list to store the nested member names of the compound datatype
     */
    private void extractCompoundInfo(long tid, String name, List<String> names, List<Long> flatTypeList2) {
        log.trace("extractCompoundInfo(): start: tid={}, name={}", tid, name);

        int nMembers = 0, mclass = -1;
        long mtype = -1;
        String mname = null;

        try {
            nMembers = H5.H5Tget_nmembers(tid);
        }
        catch (Exception ex) {
            log.debug("extractCompoundInfo(): H5Tget_nmembers(tid {}) failure", tid, ex);
            nMembers = 0;
        }
        log.trace("extractCompoundInfo(): nMembers={}", nMembers);

        if (nMembers <= 0) {
            log.debug("extractCompoundInfo(): datatype has no members");
            log.trace("extractCompoundInfo(): finish");
            return;
        }

        long tmptid = -1;
        for (int i = 0; i < nMembers; i++) {
            log.trace("extractCompoundInfo(): nMembers[{}]", i);
            try {
                mtype = H5.H5Tget_member_type(tid, i);
            }
            catch (Exception ex) {
                log.debug("extractCompoundInfo(): continue after H5Tget_member_type[{}] failure: ", i, ex);
                continue;
            }

            try {
                tmptid = mtype;
                mtype = H5.H5Tget_native_type(tmptid);
            }
            catch (HDF5Exception ex) {
                log.debug("extractCompoundInfo(): continue after H5Tget_native_type[{}] failure: ", i, ex);
                continue;
            }
            finally {
                try {
                    H5.H5Tclose(tmptid);
                }
                catch (HDF5Exception ex) {
                    log.debug("extractCompoundInfo(): H5Tclose(tmptid {}) failure: ", tmptid, ex);
                }
            }

            try {
                mclass = H5.H5Tget_class(mtype);
            }
            catch (HDF5Exception ex) {
                log.debug("extractCompoundInfo(): continue after H5Tget_class[{}] failure: ", i, ex);
                continue;
            }

            if (names != null) {
                mname = name + H5.H5Tget_member_name(tid, i);
                log.trace("extractCompoundInfo():[{}] mname={}, name={}", i, mname, name);
            }

            if (mclass == HDF5Constants.H5T_COMPOUND) {
                extractCompoundInfo(mtype, mname + CompoundDS.separator, names, flatTypeList2);
                log.debug("extractCompoundInfo(): continue after recursive H5T_COMPOUND[{}]:", i);
                continue;
            }
            else if (mclass == HDF5Constants.H5T_ARRAY) {
                try {
                    tmptid = H5.H5Tget_super(mtype);
                    int tmpclass = H5.H5Tget_class(tmptid);

                    // cannot deal with ARRAY of ARRAY, support only ARRAY of atomic types
                    if ((tmpclass == HDF5Constants.H5T_ARRAY)) {
                        log.debug("extractCompoundInfo():[{}] unsupported ARRAY of ARRAY", i);
                        continue;
                    }
                }
                catch (Exception ex) {
                    log.debug("extractCompoundInfo():[{}] continue after H5T_ARRAY id or class failure: ", i, ex);
                    continue;
                }
                finally {
                    try {
                        H5.H5Tclose(tmptid);
                    }
                    catch (Exception ex) {
                        log.debug("extractCompoundInfo():[{}] H5Tclose(tmptid {}) failure: ", i, tmptid, ex);
                    }
                }
            }

            if (names != null) {
                names.add(mname);
            }
            flatTypeList2.add(new Long(mtype));

        } // for (int i=0; i<nMembers; i++)
        log.trace("extractCompoundInfo(): finish");
    } // extractNestedCompoundInfo

    /**
     * Creates a datatype of a compound with one field.
     * <p>
     * This function is needed to read/write data field by field.
     *
     * @param atom_tid
     *            The datatype identifier of the compound to create
     * @param member_name
     *            The name of the datatype
     * @param compInfo
     *            compInfo[0]--IN: class of member datatype; compInfo[1]--IN: size
     *            of member datatype; compInfo[2]--OUT: non-zero if the base type of
     *            the compound field is unsigned; zero, otherwise.
     *
     * @return the identifier of the compound datatype.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    private final long createCompoundFieldType(long atom_tid, String member_name, int[] compInfo) throws HDF5Exception {
        log.trace("createCompoundFieldType(): start");

        long nested_tid = -1;

        long arrayType = -1;
        long baseType = -1;
        long tmp_tid1 = -1;
        long tmp_tid4 = -1;

        try {
            int member_class = compInfo[0];
            int member_size = compInfo[1];

            log.trace("createCompoundFieldType(): {} Member is class {} of size={} with baseType={}", member_name,
                    member_class, member_size, baseType);
            if (member_class == HDF5Constants.H5T_ARRAY) {
                int mn = H5.H5Tget_array_ndims(atom_tid);
                long[] marray = new long[mn];
                H5.H5Tget_array_dims(atom_tid, marray);
                baseType = H5.H5Tget_super(atom_tid);
                tmp_tid4 = H5.H5Tget_native_type(baseType);
                arrayType = H5.H5Tarray_create(tmp_tid4, mn, marray);
                log.trace("createCompoundFieldType(): H5T_ARRAY {} Member is class {} of size={} with baseType={}",
                        member_name, member_class, member_size, baseType);
            }

            try {
                if (baseType < 0) {
                    if (H5Datatype.isUnsigned(atom_tid)) {
                        compInfo[2] = 1;
                    }
                }
                else {
                    if (H5Datatype.isUnsigned(baseType)) {
                        compInfo[2] = 1;
                    }
                }
            }
            catch (Exception ex2) {
                log.debug("createCompoundFieldType(): baseType isUnsigned: ", ex2);
            }
            try {
                H5.H5Tclose(baseType);
                baseType = -1;
            }
            catch (HDF5Exception ex4) {
                log.debug("createCompoundFieldType(): H5Tclose(baseType {}) failure: ", baseType, ex4);
            }

            member_size = (int) H5.H5Tget_size(atom_tid);
            log.trace("createCompoundFieldType(): member_size={}", member_size);

            // construct nested compound structure with a single field
            String theName = member_name;
            if (arrayType < 0) {
                tmp_tid1 = H5.H5Tcopy(atom_tid);
            }
            else {
                tmp_tid1 = H5.H5Tcopy(arrayType);
            }
            try {
                H5.H5Tclose(arrayType);
                arrayType = -1;
            }
            catch (HDF5Exception ex4) {
                log.debug("createCompoundFieldType(): H5Tclose(arrayType {}) failure: ", arrayType, ex4);
            }
            int sep = member_name.lastIndexOf(CompoundDS.separator);
            log.trace("createCompoundFieldType(): sep={}", sep);

            while (sep > 0) {
                theName = member_name.substring(sep + 1);
                log.trace("createCompoundFieldType(): sep={} with name={}", sep, theName);
                nested_tid = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, member_size);
                H5.H5Tinsert(nested_tid, theName, 0, tmp_tid1);
                try {
                    log.trace("createCompoundFieldType(sep): H5.H5Tclose:tmp_tid1={}", tmp_tid1);
                    H5.H5Tclose(tmp_tid1);
                }
                catch (Exception ex) {
                    log.debug("createCompoundFieldType(): H5Tclose(tmp_tid {}) failure: ", tmp_tid1, ex);
                }
                tmp_tid1 = nested_tid;
                member_name = member_name.substring(0, sep);
                sep = member_name.lastIndexOf(CompoundDS.separator);
            }

            nested_tid = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, member_size);

            H5.H5Tinsert(nested_tid, member_name, 0, tmp_tid1);
        }
        finally {
            try {
                log.trace("createCompoundFieldType(): finally H5.H5Tclose:tmp_tid1={}", tmp_tid1);
                H5.H5Tclose(tmp_tid1);
            }
            catch (HDF5Exception ex3) {
                log.debug("createCompoundFieldType(): H5Tclose(tmp_tid {}) failure: ", tmp_tid1, ex3);
            }
            try {
                log.trace("createCompoundFieldType(): finally H5.H5Tclose:tmp_tid4={}", tmp_tid4);
                H5.H5Tclose(tmp_tid4);
            }
            catch (HDF5Exception ex3) {
                log.debug("createCompoundFieldType(): H5Tclose(tmp_tid {}) failure: ", tmp_tid4, ex3);
            }
            try {
                log.trace("createCompoundFieldType(): finally H5.H5Tclose:baseType={}", baseType);
                H5.H5Tclose(baseType);
            }
            catch (HDF5Exception ex4) {
                log.debug("createCompoundFieldType(): H5Tclose(baseType {}) failure: ", baseType, ex4);
            }
            try {
                log.trace("createCompoundFieldType(): finally H5.H5Tclose:arrayType={}", arrayType);
                H5.H5Tclose(arrayType);
            }
            catch (HDF5Exception ex4) {
                log.debug("createCompoundFieldType(): H5Tclose(arrayType {}) failure: ", arrayType, ex4);
            }
        }

        log.trace("createCompoundFieldType(): finish");
        return nested_tid;
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
    private Object convertCompoundByteMember(byte[] data, long data_type, long start, long len) {
        Object currentData = null;

        try {
            long typeClass = H5.H5Tget_class(data_type);

            if (typeClass == HDF5Constants.H5T_INTEGER) {
                long size = H5.H5Tget_size(data_type);

                currentData = HDFNativeData.byteToInt((int) start, (int) (len / size), data);
            }
            else if (typeClass == HDF5Constants.H5T_FLOAT) {
                currentData = HDFNativeData.byteToDouble((int) start, 1, data);
            }
        }
        catch (Exception ex) {
            log.debug("convertCompoundByteMember(): conversion failure: ", ex);
        }

        return currentData;
    }
}
