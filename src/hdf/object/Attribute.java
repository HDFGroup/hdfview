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
 * Attribute dataRange = new Attribute(name, type, dims);
 * // Set the attribute value
 * dataRange.setValue(value);
 * // See FileFormat.writeAttribute() for how to attach an attribute to an object,
 * &#64;see hdf.object.FileFormat#writeAttribute(HObject, Attribute, boolean)
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
public class Attribute extends Dataset implements DataFormat, CompoundDataFormat {

    private static final long serialVersionUID = 2072473407027648309L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Attribute.class);

    /** The HObject to which this Attribute is attached */
    protected HObject         parentObject;

    /** additional information and properties for the attribute */
    private transient Map<String, Object> properties;

    /**
     * Flag to indicate is the original unsigned C data is converted.
     */
    protected boolean         unsignedConverted;

    /** Flag to indicate if the attribute data is a single scalar point */
    protected final boolean   isScalar;

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
    private List<String> flatNameList;

    /**
     * A list of datatypes of all compound fields including nested fields.
     */
    private List<Datatype> flatTypeList;

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
     * Datatype attrType = null;
     * try {
     *     attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, Datatype.NATIVE, Datatype.NATIVE);
     * }
     * catch (Exception ex) {}
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
        super((parentObj == null) ? null : parentObj.getFileFormat(), attrName,
                (parentObj == null) ? null : parentObj.getFullName(), null);

        this.parentObject = parentObj;

        datatype = attrType;
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

        data = attrValue;
        properties = new HashMap();

        unsignedConverted = false;

        selectedDims = new long[rank];
        startDims = new long[rank];
        selectedStride = new long[rank];

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
        log.trace("open(): start");

        if (parentObject == null) {
            log.debug("open(): attribute's parent object is null");
            log.trace("open(): exit");
            return -1;
        }

        long aid = -1;
        long pObjID = -1;

        try {
            pObjID = parentObject.open();
            if (pObjID >= 0) {
                if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                    log.trace("open(): FILE_TYPE_HDF5");
                    if (H5.H5Aexists(pObjID, getName()))
                        aid = H5.H5Aopen(pObjID, getName(), HDF5Constants.H5P_DEFAULT);
                }
                else if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
                    log.trace("open(): FILE_TYPE_HDF4");
                    /*
                     * TODO: Get type of HDF4 object this is attached to and retrieve attribute info.
                     */
                }
                else if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
                    log.trace("open(): FILE_TYPE_NC3");
                    /*
                     * TODO: Get type of netcdf3 object this is attached to and retrieve attribute info.
                     */
                }
            }

            log.trace("open(): aid={}", aid);
        }
        catch (Exception ex) {
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
            if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                log.trace("close(): FILE_TYPE_HDF5");
                try {
                    H5.H5Aclose(aid);
                }
                catch (HDF5Exception ex) {
                    log.debug("close(): H5Aclose({}) failure: ", aid, ex);
                }
            }
            else if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
                log.trace("close(): FILE_TYPE_HDF4");
                /*
                 * TODO: Get type of HDF4 object this is attached to and close attribute.
                 */
            }
            else if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
                log.trace("close(): FILE_TYPE_NC3");
                /*
                 * TODO: Get type of netcdf3 object this is attached to and close attribute.
                 */
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

        if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
            long aid = -1;
            long tid = -1;
            int tclass = -1;
            flatNameList = new Vector<>();
            flatTypeList = new Vector<>();
            long[] memberTIDs = null;

            log.trace("init(): FILE_TYPE_HDF5");
            aid = open();
            if (aid >= 0) {
                try {
                    tid = H5.H5Aget_type(aid);
                    tclass = H5.H5Tget_class(tid);

                    long tmptid = 0;

                    // Handle ARRAY and VLEN types by getting the base type
                    if (tclass == HDF5Constants.H5T_ARRAY || tclass == HDF5Constants.H5T_VLEN) {
                        try {
                            tmptid = tid;
                            tid = H5.H5Tget_super(tmptid);
                            log.trace("init(): H5T_ARRAY or H5T_VLEN class old={}, new={}", tmptid, tid);
                        }
                        catch (Exception ex) {
                            log.debug("init(): H5T_ARRAY or H5T_VLEN H5Tget_super({}) failure: ", tmptid, ex);
                            tid = -1;
                        }
                        finally {
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (HDF5Exception ex) {
                                log.debug("init(): H5Tclose({}) failure: ", tmptid, ex);
                            }
                        }
                    }

                    if (H5.H5Tget_class(tid) == HDF5Constants.H5T_COMPOUND) {
                        // initialize member information
                        H5Datatype.extractCompoundInfo((H5Datatype) getDatatype(), "", flatNameList, flatTypeList);
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
                            memberTIDs[i] = flatTypeList.get(i).createNative();

                            try {
                                memberTypes[i] = flatTypeList.get(i);
                            }
                            catch (Exception ex) {
                                log.debug("init(): failed to create datatype for member[{}]: ", i, ex);
                                memberTypes[i] = null;
                            }

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
                        } // (int i=0; i<numberOfMembers; i++)
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
        }
        else if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
            log.trace("init(): FILE_TYPE_HDF4");
            /*
             * TODO: If HDF4 attribute object needs to init dependent objects.
             */
            inited = true;
        }
        else if (this.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
            log.trace("init(): FILE_TYPE_NC3");
            /*
             * TODO: If netcdf3 attribute object needs to init dependent objects.
             */
            inited = true;
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
        log.trace("convertFromUnsignedC(): start");

        // Keep a copy of original buffer and the converted buffer
        // so that they can be reused later to save memory
        if ((data != null) && getDatatype().isUnsigned() && !unsignedConverted) {
            log.trace("convertFromUnsignedC(): convert");

            originalBuf = data;
            convertedBuf = convertFromUnsignedC(originalBuf, convertedBuf);
            data = convertedBuf;
            unsignedConverted = true;
        }

        log.trace("convertFromUnsignedC(): finish");

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
        log.trace("convertToUnsignedC(): start");

        // Keep a copy of original buffer and the converted buffer
        // so that they can be reused later to save memory
        if ((data != null) && getDatatype().isUnsigned()) {
            log.trace("convertToUnsignedC(): convert");

            convertedBuf = data;
            originalBuf = convertToUnsignedC(convertedBuf, originalBuf);
            data = originalBuf;
        }

        log.trace("convertToUnsignedC(): finish");

        return data;
    }

    @Override
    public Object getFillValue() {
        /*
         * Currently, Attributes do not support fill values.
         */
        return null;
    }

    @Override
    public void clearData() {
        super.clearData();
        unsignedConverted = false;
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
     * @return true if the data is a single scalar point; otherwise, returns
     *         false.
     */
    public boolean isScalar() {
        return isScalar;
    }

    @Override
    public Object read() throws Exception, OutOfMemoryError {
        log.trace("read(): start");
        if (!inited) init();

        /*
         * TODO: For now, convert a compound Attribute's data (String[]) into a List for
         * convenient processing
         */
        if (getDatatype().isCompound() && !(data instanceof List)) {
            List<String> valueList = Arrays.asList((String[]) data);

            data = valueList;
        }

        log.trace("read(): finish");
        return data;
    }

    @Override
    public void write(Object buf) throws Exception {
        log.trace("write(): start");

        if (!buf.equals(data))
            setData(buf);

        if (!inited) init();

        if (parentObject == null) {
            log.debug("write(): parent object is null; nowhere to write attribute to");
            log.debug("write(): finish");
            return;
        }

        ((MetaDataContainer) getParentObject()).writeMetadata(this);

        log.trace("write(): finish");
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
     * Returns an array of the names of the selected members of the compound dataset.
     *
     * @return an array of the names of the selected members of the compound dataset.
     */
    public final String[] getSelectedMemberNames() {
        if (isMemberSelected == null) {
            log.debug("getSelectedMemberNames(): isMemberSelected array is null");
            log.trace("getSelectedMemberNames(): finish");
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
        log.trace("getSelectedMemberOrders(): start");

        if (isMemberSelected == null) {
            log.debug("getSelectedMemberOrders(): isMemberSelected array is null");
            log.trace("getSelectedMemberOrders(): finish");
            return memberOrders;
        }

        int idx = 0;
        int[] orders = new int[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i]) {
                orders[idx++] = memberOrders[i];
            }
        }

        log.trace("getSelectedMemberOrders(): finish");

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
        log.trace("getSelectedMemberTypes(): start");

        if (isMemberSelected == null) {
            log.debug("getSelectedMemberTypes(): isMemberSelected array is null");
            log.trace("getSelectedMemberTypes(): finish");
            return memberTypes;
        }

        int idx = 0;
        Datatype[] types = new Datatype[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i]) {
                types[idx++] = memberTypes[i];
            }
        }

        log.trace("getSelectedMemberTypes(): finish");

        return types;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public List getMetadata() throws Exception {
        throw new UnsupportedOperationException("Attribute:getMetadata Unsupported operation.");
    }

    @Override
    public void writeMetadata(Object metadata) throws Exception {
        throw new UnsupportedOperationException("Attribute:writeMetadata Unsupported operation.");
    }

    @Override
    public void removeMetadata(Object metadata) throws Exception {
        throw new UnsupportedOperationException("Attribute:removeMetadata Unsupported operation.");
    }

    @Override
    public void updateMetadata(Object metadata) throws Exception {
        throw new UnsupportedOperationException("Attribute:updateMetadata Unsupported operation.");
    }

    @Override
    public boolean hasAttribute() {
        return false;
    }

    @Override
    public final Datatype getDatatype() {
        return datatype;
    }

    @Override
    public byte[] readBytes() throws Exception {
        throw new UnsupportedOperationException("Attribute:readBytes Unsupported operation.");
    }

    @Override
    public Dataset copy(Group pgroup, String name, long[] dims, Object data) throws Exception {
        throw new UnsupportedOperationException("Attribute:copy Unsupported operation.");
    }

    /**
     * Returns whether this Attribute is equal to the specified HObject by comparing
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
        if (obj instanceof Attribute) {
            if (!this.getFullName().equals(((Attribute) obj).getFullName()))
                return false;

            if (!this.getFileFormat().equals(((Attribute) obj).getFileFormat()))
                return false;

            if (!Arrays.equals(this.getDims(), ((DataFormat) obj).getDims()))
                return false;

            return (this.getParentObject().equals(((Attribute) obj).getParentObject()));
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
        log.trace("toString(): start");

        if (data == null) {
            log.debug("toString(): value is null");
            log.trace("toString(): finish");
            return null;
        }

        Class<? extends Object> valClass = data.getClass();

        if (!valClass.isArray()) {
            log.trace("toString(): finish - not array");
            String strValue = data.toString();
            if (maxItems > 0 && strValue.length() > maxItems) {
                // truncate the extra characters
                strValue = strValue.substring(0, maxItems);
            }
            return strValue;
        }

        // attribute value is an array
        StringBuilder sb = new StringBuilder();
        int n = Array.getLength(data);
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
                    byte[] barray = (byte[]) data;
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
                    short[] sarray = (short[]) data;
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
                    int[] iarray = (int[]) data;
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
                    long[] larray = (long[]) data;
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
                    sb.append(Array.get(data, 0));
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        sb.append(Array.get(data, i));
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
                    byte[] barray = (byte[]) data;
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
                    short[] sarray = (short[]) data;
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
                    int[] iarray = (int[]) data;
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
                    long[] larray = (long[]) data;
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
                    String strValue = Array.get(data, 0).toString();
                    if (maxItems > 0 && strValue.length() > maxItems) {
                        // truncate the extra characters
                        strValue = strValue.substring(0, maxItems);
                    }
                    sb.append(strValue);
                    for (int i = 1; i < n; i++) {
                        sb.append(delimiter);
                        strValue = Array.get(data, i).toString();
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
            Object value = Array.get(data, 0);
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
                value = Array.get(data, i);

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

        log.trace("toString: finish");
        return sb.toString();
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
