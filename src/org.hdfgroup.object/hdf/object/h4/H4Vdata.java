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
import java.util.Vector;

import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFException;
import hdf.hdflib.HDFLibrary;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.MetaDataContainer;

import hdf.object.h4.H4CompoundAttribute;

/**
 * H4Vdata describes a multi-dimension array of HDF4 vdata, inheriting CompoundDS.
 *
 * A vdata is like a table that consists of a collection of records whose values
 * are stored in fixed-length fields. All records have the same structure and
 * all values in each field have the same data type. Vdatas are uniquely
 * identified by a name, a class, and a series of individual field names.
 *
 * <b>How to Select a Subset</b>
 *
 * Dataset defines APIs for reading, writing and subsetting a dataset. No function is
 * defined to select a subset of a data array. The selection is done in an implicit way.
 * Function calls to dimension information such as getSelectedDims() return an array
 * of dimension values, which is a reference to the array in the dataset object.
 * Changes of the array outside the dataset object directly change the values of
 * the array in the dataset object. It is like pointers in C.
 *
 * The following is an example of how to make a subset. In the example, the dataset
 * is a 4-dimension with size of [200][100][50][10], i.e.
 * dims[0]=200; dims[1]=100; dims[2]=50; dims[3]=10; <br>
 * We want to select every other data point in dims[1] and dims[2]
 * <pre>
     int rank = dataset.getRank();   // number of dimensions of the dataset
     long[] dims = dataset.getDims(); // the dimension sizes of the dataset
     long[] selected = dataset.getSelectedDims(); // the selected size of the dataet
     long[] start = dataset.getStartDims(); // the offset of the selection
     long[] stride = dataset.getStride(); // the stride of the dataset
     int[]  selectedIndex = dataset.getSelectedIndex(); // the selected dimensions for display

     // select dim1 and dim2 as 2D data for display, and slice through dim0
     selectedIndex[0] = 1;
     selectedIndex[1] = 2;
     selectedIndex[1] = 0;

     // reset the selection arrays
     for (int i=0; i&lt;rank; i++) {
         start[i] = 0;
         selected[i] = 1;
         stride[i] = 1;
    }

    // set stride to 2 on dim1 and dim2 so that every other data point is selected.
    stride[1] = 2;
    stride[2] = 2;

    // set the selection size of dim1 and dim2
    selected[1] = dims[1]/stride[1];
    selected[2] = dims[1]/stride[2];

    // when dataset.read() is called, the selection above will be used since
    // the dimension arrays is passed by reference. Changes of these arrays
    // outside the dataset object directly change the values of these array
    // in the dataset object.

 * </pre>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H4Vdata extends CompoundDS implements MetaDataContainer
{
    private static final long serialVersionUID = -5978700886955419959L;

    private static final org.slf4j.Logger       log = org.slf4j.LoggerFactory.getLogger(H4Vdata.class);

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of H4CompoundAttribute.
     */
    @SuppressWarnings("rawtypes")
    private List                                attributeList;

    /**
     * Number of records of this Vdata table.
     */
    private int                                 numberOfRecords;

    /**
     * The data types of the members of the compound dataset.
     */
    private long[]                              memberTIDs;

    /** the number of attributes */
    private int                                 nAttributes = -1;


    /**
     * Creates an H4Vdata object with specific name and path.
     *
     * @param theFile the HDF file.
     * @param name the name of this H4Vdata.
     * @param path the full path of this H4Vdata.
     */
    public H4Vdata(FileFormat theFile, String name, String path) {
        this(theFile, name, path, null);
    }

    /**
     * Creates an H4Vdata object with specific name, path and oid.
     *
     * @param theFile the HDF file.
     * @param name the name of this H4Vdata.
     * @param path the full path of this H4Vdata.
     * @param oid the unique identifier of this data object.
     */
    @SuppressWarnings("deprecation")
    public H4Vdata(FileFormat theFile, String name, String path, long[] oid) {
        super (theFile, name, path, oid);
        numberOfRecords = 0;
        numberOfMembers = 0;
        memberOrders = null;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.DataFormat#hasAttribute()
     */
    @Override
    public boolean hasAttribute() {
        if (nAttributes < 0) {
            long id = open();

            if (id >= 0) {
                try {
                    nAttributes = HDFLibrary.VSnattrs(id);
                }
                catch (Exception ex) {
                    log.debug("hasAttribute() failure: ", ex);
                    nAttributes = 0;
                }

                log.trace("hasAttribute(): nAttributes={}", nAttributes);

                close(id);
            }
        }

        return (nAttributes > 0);
    }

    // implementing Dataset
    /**
     * Returns the datatype of the data object.
     *
     * @return the datatype of the data object.
     */
    @Override
    public Datatype getDatatype() {
        if (!inited)
            init();

        if (datatype == null) {
            try {
                datatype = new H4Datatype(-1);
            }
            catch (Exception ex) {
                log.debug("getDatatype(): failed to create datatype: ", ex);
                datatype = null;
            }
        }

        return datatype;
    }

    /**
     * Returns the fill values for the data object.
     *
     * @return the fill values for the data object.
     */
    @Override
    public Object getFillValue() {
        return null;
    }

    // Implementing Dataset
    @Override
    public byte[] readBytes() throws HDFException {
        byte[] theData = null;

        if (!isInited())
            init();

        if (numberOfMembers <= 0) {
            log.debug("readBytes(): VData contains no members");
            return null; // this Vdata does not have any filed
        }

        long id = open();
        if (id < 0) {
            log.debug("readBytes(): Invalid VData ID");
            return null;
        }

        String allNames = memberNames[0];
        for (int i=0; i<numberOfMembers; i++)
            allNames += ","+memberNames[i];

        try {
            // moves the access pointer to the start position
            HDFLibrary.VSseek(id, (int)startDims[0]);
            // Specify the fields to be accessed
            HDFLibrary.VSsetfields(id, allNames);
            int[] recordSize = {0};
            HDFLibrary.VSQueryvsize(id, recordSize);
            int size =recordSize[0] * (int)selectedDims[0];
            theData = new byte[size];
            HDFLibrary.VSread(id, theData, (int)selectedDims[0], HDFConstants.FULL_INTERLACE);
        }
        catch (Exception ex) {
            log.debug("readBytes(): failure: ", ex);
        }
        finally {
            close(id);
        }

        return theData;
    }

    // Implementing DataFormat
    /**
     * Reads the data from file.
     *
     * read() reads the data from file to a memory buffer and returns the memory
     * buffer. The dataset object does not hold the memory buffer. To store the
     * memory buffer in the dataset object, one must call getData().
     *
     * By default, the whole dataset is read into memory. Users can also select
     * a subset to read. Subsetting is done in an implicit way.
     *
     * @return the data read from file.
     *
     * @see #getData()
     *
     * @throws HDFException
     *             if object can not be read
     * @throws OutOfMemoryError
     *             if memory is exhausted
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
    @Override
    public Object read() throws HDFException {
        List list = null;

        if (!isInited())
            init();

        if (numberOfMembers <= 0) {
            log.debug("read(): VData contains no members");
            return null; // this Vdata does not have any filed
        }

        long id = open();
        if (id < 0) {
            log.debug("read(): Invalid VData ID");
            return null;
        }

        list = new Vector();

        // assume external data files are located in the same directory as the main file.
        HDFLibrary.HXsetdir(getFileFormat().getParent());

        Object member_data = null;
        for (int i=0; i<numberOfMembers; i++) {
            if (!isMemberSelected[i])
                continue;

            try {
                // moves the access pointer to the start position
                HDFLibrary.VSseek(id, (int)startDims[0]);
                // Specify the fields to be accessed
                HDFLibrary.VSsetfields(id, memberNames[i]);
            }
            catch (HDFException ex) {
                log.debug("read(): failure: ", ex);
                isMemberSelected[i] = false;
                continue;
            }

            int n = memberOrders[i]*(int)selectedDims[0];

            member_data = H4Datatype.allocateArray(memberTIDs[i], n);

            log.trace("read(): index={} isMemberSelected[i]={} memberOrders[i]={} array size={}", i, isMemberSelected[i], memberOrders[i], n);
            if (member_data == null) {
                String[] nullValues = new String[n];
                for (int j=0; j<n; j++)
                    nullValues[j] = "*ERROR*";
                list.add(nullValues);
                continue;
            }

            try {
                HDFLibrary.VSread(id, member_data, (int)selectedDims[0], HDFConstants.FULL_INTERLACE);
                if ((memberTIDs[i] == HDFConstants.DFNT_CHAR) ||
                        (memberTIDs[i] ==  HDFConstants.DFNT_UCHAR8)) {
                    // convert characters to string
                    log.trace("read(): convert characters to string");
                    member_data = Dataset.byteToString((byte[])member_data, memberOrders[i]);
                    try {
                        memberTypes[i] = new H4Datatype(Datatype.CLASS_STRING, memberOrders[i], Datatype.NATIVE, Datatype.NATIVE);
                    }
                    catch (Exception ex) {
                        log.debug("read(): failed to create datatype for member[{}]: ", i, ex);
                        memberTypes[i] = null;
                    }
                    memberOrders[i] = 1; //one String
                }
                else if (H4Datatype.isUnsigned(memberTIDs[i])) {
                    // convert unsigned integer to appropriate Java integer
                    log.trace("read(): convert unsigned integer to appropriate Java integer");
                    member_data = Dataset.convertFromUnsignedC(member_data);
                }
            }
            catch (HDFException ex) {
                String[] nullValues = new String[n];
                for (int j=0; j<n; j++)
                    nullValues[j] = "*ERROR*";
                list.add(nullValues);
                continue;
            }

            list.add(member_data);
        } //  (int i=0; i<numberOfMembers; i++)

        close(id);

        return list;
    }

    // Implementing DataFormat
    /**
     * Writes a memory buffer to the object in the file.
     *
     * @param buf
     *            the data to write
     *
     * @throws HDFException
     *             if data can not be written
     */
    @Override
    public void write(Object buf) throws HDFException {
        //For writing to a vdata, VSsetfields can only be called once, to set
        //up the fields in a vdata. Once the vdata fields are set, they may
        //not be changed. Thus, to update some fields of a record after the
        //first write, the user must read all the fields to a buffer, update
        //the buffer, then write the entire record back to the vdata.
        log.trace("write(): disabled");
        /*
        if (buf == null || numberOfMembers <= 0 || !(buf instanceof List))
            return; // no data to write

        List list = (List)buf;
        Object member_data = null;
        String member_name = null;

        int vid = open();
        if (vid < 0) return;

        int idx = 0;
        for (int i=0; i<numberOfMembers; i++) {
            if (!isMemberSelected[i])
                continue;

            HDFLibrary.VSsetfields(vid, memberNames[i]);

            try {
                // Specify the fields to be accessed

                // moves the access pointer to the start position
                HDFLibrary.VSseek(vid, (int)startDims[0]);
            }
            catch (HDFException ex) {
                continue;
            }

            member_data = list.get(idx++);
            if (member_data == null)
                continue;

            if (memberTIDs[i] == HDFConstants.DFNT_CHAR ||
                memberTIDs[i] ==  HDFConstants.DFNT_UCHAR8) {
                member_data = Dataset.stringToByte((String[])member_data, memberOrders[i]);
            }
            else if (H4Datatype.isUnsigned(memberTIDs[i])) {
                // convert unsigned integer to appropriate Java integer
                member_data = Dataset.convertToUnsignedC(member_data);
            }


            int interlace = HDFConstants.NO_INTERLACE;
            try {
                int write_num = HDFLibrary.VSwrite(
                    vid, member_data, (int)selectedDims[0], interlace);
            }
            catch (HDFException ex) {
                log.debug("write():", ex);
            }
        } //  (int i=0; i<numberOfMembers; i++)

        close(vid);
         */
    }

    /**
     * Converts the data values of this data object to appropriate Java integers if
     * they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertFromUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertFromUnsignedC() {
        throw new UnsupportedOperationException("H4Vdata:convertFromUnsignedC Unsupported operation.");
    }

    /**
     * Converts Java integer data values of this data object back to unsigned C-type
     * integer data if they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertToUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertToUnsignedC() {
        throw new UnsupportedOperationException("H4Vdata:convertToUnsignedC Unsupported operation.");
    }

    // Implementing DataFormat
    /**
     * Retrieves the object's metadata, such as attributes, from the file.
     *
     * Metadata, such as attributes, is stored in a List.
     *
     * @return the list of metadata objects.
     *
     * @throws HDFException
     *             if the metadata can not be retrieved
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getMetadata() throws HDFException {
        if (attributeList != null) {
            log.trace("getMetdata(): attributeList != null");
            return attributeList;
        }

        long id = open();

        if (id < 0) {
            log.debug("getMetadata(): Invalid VData ID");
            return attributeList;
        }

        int n = 0;
        try {
            n = HDFLibrary.VSnattrs(id);

            if (n <= 0) {
                log.debug("getMetadata(): VData number of attributes <= 0");
                return attributeList;
            }

            attributeList = new Vector(n, 5);
            boolean b = false;
            String[] attrName = new String[1];
            int[] attrInfo = new int[5];

            // _HDF_VDATA (or -1) to specify the vdata attribute
            int nleft = n;
            for (int j = -1; j < numberOfMembers; j++) {
                for (int i = 0; i < nleft; i++) {
                    attrName[0] = "";

                    try {
                        b = HDFLibrary.VSattrinfo(id, j, i, attrName, attrInfo);
                        // mask off the litend bit
                        attrInfo[0] = attrInfo[0] & (~HDFConstants.DFNT_LITEND);
                    }
                    catch (HDFException ex) {
                        log.debug("getMetadata(): attribute[{}] VSattrinfo failure: ", i, ex);
                        b = false;
                        ex.printStackTrace();
                    }

                    if (!b || attrName[0].length() <= 0)
                        continue;

                    long[] attrDims = {attrInfo[1]};
                    H4CompoundAttribute attr = new H4CompoundAttribute(this, attrName[0], new H4Datatype(attrInfo[0]), attrDims);
                    if (j >= 0)
                        attr.setProperty("field", memberNames[j]);
                    attributeList.add(attr);

                    Object buf = null;
                    try {
                        buf = H4Datatype.allocateArray(attrInfo[0], attrInfo[1]);
                    }
                    catch (OutOfMemoryError e) {
                        log.debug("getMetadata(): out of memory: ", e);
                        buf = null;
                    }

                    try {
                        HDFLibrary.VSgetattr(id, j, i, buf);
                    }
                    catch (HDFException ex) {
                        log.debug("getMetadata(): attribute[{}] VSgetattr failure: ", i, ex);
                        buf = null;
                    }

                    if (buf != null) {
                        if ((attrInfo[0] == HDFConstants.DFNT_CHAR) ||
                                (attrInfo[0] ==  HDFConstants.DFNT_UCHAR8)) {
                            buf = Dataset.byteToString((byte[])buf, attrInfo[1]);
                        }

                        attr.setAttributeData(buf);
                        nleft--;
                    }
                } //  (int i=0; i<n; i++)
            } //  (int j=-1; j<numberOfMembers; j++)
        }
        catch (Exception ex) {
            log.debug("getMetadata(): failure: ", ex);
        }
        finally {
            close(id);
        }

        // todo: We shall also load attributes of fields

        return attributeList;
    }

    // To do: Implementing DataFormat
    /**
     * Writes a specific piece of metadata (such as an attribute) into the file.
     *
     * If an HDF(4&amp;5) attribute exists in the file, this method updates its
     * value. If the attribute does not exist in the file, it creates the
     * attribute in the file and attaches it to the object. It will fail to
     * write a new attribute to the object where an attribute with the same name
     * already exists. To update the value of an existing attribute in the file,
     * one needs to get the instance of the attribute by getMetadata(), change
     * its values, then use writeMetadata() to write the value.
     *
     * @param info
     *            the metadata to write.
     *
     * @throws Exception
     *             if the metadata can not be written
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void writeMetadata(Object info) throws Exception {
        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("writeMetadata(): Object not an H4Attribute");
            return;
        }

        try {
            getFileFormat().writeAttribute(this, (H4ScalarAttribute)info, true);

            if (attributeList == null)
                attributeList = new Vector();

            attributeList.add(info);
            nAttributes = attributeList.size();
        }
        catch (Exception ex) {
            log.trace("writeMetadata(): failure: ", ex);
        }
    }

    /**
     * Deletes an existing piece of metadata from this object.
     *
     * @param info
     *            the metadata to delete.
     *
     * @throws HDFException
     *             if the metadata can not be removed
     */
    @Override
    public void removeMetadata(Object info) throws HDFException {
        log.trace("removeMetadata(): disabled");
    }

    /**
     * Updates an existing piece of metadata attached to this object.
     *
     * @param info
     *            the metadata to update.
     *
     * @throws Exception
     *             if the metadata can not be updated
     */
    @Override
    public void updateMetadata(Object info) throws Exception {
        log.trace("updateMetadata(): disabled");
    }

    // Implementing DataFormat
    @Override
    public long open() {
        // try to open with write permission
        long vsid = -1;
        try {
            vsid = HDFLibrary.VSattach(getFID(), (int)oid[1], "w");
        }
        catch (HDFException ex) {
            log.debug("open(w): VSattach failure: ", ex);
            vsid = -1;
        }

        // try to open with read-only permission
        if (vsid < 0) {
            try {
                vsid = HDFLibrary.VSattach(getFID(), (int)oid[1], "r");
            }
            catch (HDFException ex) {
                log.debug("open(r): VSattach failure: ", ex);
                vsid = -1;
            }
            log.debug("open(r): VSattach vsid: {}", vsid);
        }

        return vsid;
    }

    // Implementing DataFormat
    @Override
    public void close(long vsid) {
        try {
            HDFLibrary.VSdetach(vsid);
        }
        catch (Exception ex) {
            log.debug("close(): VSdetach failure: ", ex);
        }
    }

    /**
     * Initializes the H4Vdata such as dimension sizes of this dataset.
     */
    @Override
    public void init() {
        if (inited) {
            log.trace("init(): Already initialized");
            return; // already called. Initialize only once
        }

        long id = open();
        if (id < 0) {
            log.debug("init(): Invalid VData ID");
            return;
        }

        try {
            numberOfMembers = HDFLibrary.VFnfields(id);
            numberOfRecords = HDFLibrary.VSelts(id);
        }
        catch (HDFException ex) {
            numberOfMembers = 0;
            numberOfRecords = 0;
        }

        //        Still need to get information if there is no record, see bug 1738
        //        if ((numberOfMembers <=0) || (numberOfRecords <= 0)) {
        //            // no table field is defined or no records
        //            close(id);
        //            return;
        //        }

        // a Vdata table is an one dimension array of records.
        // each record has the same fields
        rank = 1;
        dims = new long[1];
        dims[0] = numberOfRecords;
        selectedDims = new long[1];
        selectedDims[0] = numberOfRecords;
        selectedIndex[0] = 0;
        startDims = new long[1];
        startDims[0] = 0;

        memberNames = new String[numberOfMembers];
        memberTIDs = new long[numberOfMembers];
        memberTypes = new Datatype[numberOfMembers];
        memberOrders = new int[numberOfMembers];
        isMemberSelected = new boolean[numberOfMembers];

        try {
            datatype = new H4Datatype(Datatype.CLASS_COMPOUND, -1, Datatype.NATIVE, Datatype.NATIVE);
        }
        catch (Exception ex) {
            log.debug("init(): failed to create compound datatype for VData");
            datatype = null;
        }

        for (int i = 0; i < numberOfMembers; i++) {
            isMemberSelected[i] = true;
            try {
                memberNames[i] = HDFLibrary.VFfieldname(id, i);
                memberTIDs[i] = HDFLibrary.VFfieldtype(id, i);
                try {
                    memberTypes[i] = new H4Datatype(memberTIDs[i]);
                }
                catch (Exception ex) {
                    log.debug("init(): failed to create datatype for member[{}]: ", i, ex);
                    memberTypes[i] = null;
                }
                // mask off the litend bit
                memberTIDs[i] = memberTIDs[i] & (~HDFConstants.DFNT_LITEND);
                memberOrders[i] = HDFLibrary.VFfieldorder(id, i);
                log.trace("init():{}> isMemberSelected[i]={} memberNames[i]={} memberTIDs[i]={} memberOrders[i]={}", i, isMemberSelected[i], memberNames[i], memberTIDs[i], memberOrders[i]);

                /*
                 * NOTE: An ugly workaround to get HDF4 "compound" datatypes to work correctly.
                 */
                if (datatype != null) {
                    datatype.getCompoundMemberNames().add(memberNames[i]);
                    datatype.getCompoundMemberTypes().add(memberTypes[i]);
                }
            }
            catch (HDFException ex) {
                log.debug("init(): member[{}]: ", i, ex);
                continue;
            }
        } //  (int i=0; i<numberOfMembers; i++)

        inited = true;

        close(id);
    }

    /**
     * Returns the number of records.
     *
     * @return the number of records
     */
    public int getRecordCount() {
        return numberOfRecords;
    }

    /**
     * Returns the number of fields.
     *
     * @return the number of fields
     */
    public int getFieldCount() {
        return numberOfMembers;
    }

    /**
     * Returns the orders of fields
     *
     * @return the orders of fields
     */
    public int[] getFieldOrders() {
        return memberOrders;
    }

    //Implementing DataFormat
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

    @Override
    public Dataset copy(Group pgroup, String name, long[] dims, Object data) throws Exception {
        throw new UnsupportedOperationException("Writing a vdata to a new dataset is not implemented.");
    }
}
