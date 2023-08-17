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
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;
import hdf.object.ScalarDS;

import ucar.ma2.DataType;
import ucar.nc2.Variable;

/**
 * NC2Dataset describes an multi-dimension array of HDF5 scalar or atomic data types, such as byte, int, short, long,
 * float, double and string, and operations performed on the scalar dataset
 *
 * The library predefines a modest number of datatypes. For details, read
 * <a href="https://hdfgroup.github.io/hdf5/_h5_t__u_g.html#sec_datatype">HDF5 Datatypes in HDF5 User Guide</a>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class NC2Dataset extends ScalarDS implements MetaDataContainer {
    private static final long serialVersionUID = -6031051694304457461L;

    private static final Logger   log = LoggerFactory.getLogger(NC2Dataset.class);

    /** tag for netCDF datasets.
     *  HDF4 library supports netCDF version 2.3.2. It only supports SDS APIs.
     */
    // magic number for netCDF: "C(67) D(68) F(70) '\001'"
    public static final int                 DFTAG_NDG_NETCDF = 67687001;

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of Attribute.
     */
    @SuppressWarnings("rawtypes")
    private List                            attributeList;

    /** the native dataset */
    private Variable nativeDataset;

    /**
     * Constructs an NC2Dataset object with specific netcdf variable.
     *
     * @param fileFormat
     *            the netcdf file.
     * @param ncDataset
     *            the netcdf variable.
     * @param oid
     *            the unique identifier of this data object.
     */
    public NC2Dataset(FileFormat fileFormat, Variable ncDataset, long[] oid) {
        super(fileFormat, ncDataset.getName(), HObject.SEPARATOR, oid);
        unsignedConverted = false;
        nativeDataset = ncDataset;
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

    // Implementing Dataset
    @Override
    public Dataset copy(Group pgroup, String dstName, long[] dims, Object buff) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
    }

    // implementing Dataset
    @Override
    public byte[] readBytes() throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
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
     * @throws Exception
     *             if object can not be read
     * @throws OutOfMemoryError
     *             if memory is exhausted
     */
    @Override
    public Object read() throws Exception {
        Object theData = null;

        if (nativeDataset == null)
            return null;

        int[] origin = new int[rank];
        int[] shape = new int[rank];

        for (int i = 0; i < rank; i++) {
            origin[i] = (int) startDims[i];
            shape[i] = (int) selectedDims[i];
            log.trace("read(): origin-shape [{}]={}-{}", i, origin[i], shape[i]);
        }

        ucar.ma2.Array ncArray = null;

        try {
            ncArray = nativeDataset.read(origin, shape);
        }
        catch (Exception ex) {
            ncArray = nativeDataset.read();
        }
        Object oneD = ncArray.copyTo1DJavaArray();

        if (oneD == null)
            return null;

        if (oneD.getClass().getName().startsWith("[C")) {
            char[] charA = (char[]) oneD;
            int nCols = (int) getWidth();
            int nRows = (int) getHeight();

            String[] strA = new String[nRows];
            String allStr = new String(charA);

            int indx0 = 0;
            for (int i = 0; i < nRows; i++) {
                indx0 = i * nCols;
                strA[i] = allStr.substring(indx0, indx0 + nCols);
            }
            theData = strA;
        }
        else {
            theData = oneD;
        }

        return theData;
    }

    // Implementing DataFormat
    /**
     * Writes a memory buffer to the object in the file.
     *
     * @param buf
     *            the data to write
     *
     * @throws Exception
     *             if data can not be written
     */
    @Override
    public void write(Object buf) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
    }

    // Implementing DataFormat
    /**
     * Retrieves the object's metadata, such as attributes, from the file.
     *
     * Metadata, such as attributes, is stored in a List.
     *
     * @return the list of metadata objects.
     *
     * @throws Exception
     *             if the metadata can not be retrieved
     */
    @Override
    public List getMetadata() throws Exception {
        if (attributeList != null)
            return attributeList;

        if (nativeDataset == null)
            return (attributeList = null);

        List ncAttrList = nativeDataset.getAttributes();
        if (ncAttrList == null)
            return (attributeList = null);

        int n = ncAttrList.size();
        attributeList = new Vector(n);
        ucar.nc2.Attribute ncAttr = null;
        for (int i = 0; i < n; i++) {
            ncAttr = (ucar.nc2.Attribute) ncAttrList.get(i);
            log.trace("getMetadata(): Attribute[{}]:{}", i, ncAttr.toString());
            attributeList.add(NC2File.convertAttribute(this, ncAttr));
        }

        return attributeList;
    }

    // implementing DataFormat
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
    public void writeMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
    }

    // implementing DataFormat
   /**
    * Deletes an existing piece of metadata from this object.
    *
    * @param info
    *            the metadata to delete.
    *
    * @throws Exception
    *             if the metadata can not be removed
    */
    @Override
    public void removeMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
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
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
    }

    // Implementing HObject
    @Override
    public long open() {
        return -1;
    }

    // Implementing HObject
    @Override
    public void close(long did) {
    }

    /**
     * Retrieve and initialize dimensions and member information.
     */
    @Override
    public void init() {
        if (nativeDataset == null)
            return;

        if (inited)
            return; // already called. Initialize only once

        isText = nativeDataset.getDataType().equals(DataType.STRING);
        boolean isChar = nativeDataset.getDataType().equals(DataType.CHAR);

        rank = nativeDataset.getRank();
        log.trace("init(): rank:{}", rank);

        if (rank == 0) {
            // a scalar data point
            isScalar = true;
            rank = 1;
            dims = new long[] { 1 };
        }
        else {
            isScalar = false;
            dims = new long[rank];
            for (int i = 0; i < rank; i++)
                dims[i] = (nativeDataset.getDimension(i).getLength());
        }

        startDims = new long[rank];
        selectedDims = new long[rank];
        for (int i = 0; i < rank; i++) {
            startDims[i] = 0;
            selectedDims[i] = 1;
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
            selectedIndex[0] = 0;
            selectedIndex[1] = 1;
            selectedIndex[2] = 2;
            selectedDims[0] = dims[0];
            selectedDims[1] = dims[1];
        }

        if ((rank > 1) && isText)
            selectedDims[1] = 1;

        inited = true;
    }

    /**
     * Creates a new dataset.
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            the parent group of the new dataset.
     * @param type
     *            the datatype of the dataset.
     * @param dims
     *            the dimension size of the dataset.
     * @param maxdims
     *            the max dimension size of the dataset.
     * @param chunks
     *            the chunk size of the dataset.
     * @param gzip
     *            the level of the gzip compression.
     * @param data
     *            the array of data values.
     *
     * @return the new dataset if successful. Otherwise returns null.
     *
     * @throws Exception
     *            if there is an error
    */
    public static NC2Dataset create(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks, int gzip, Object data) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
    }

    // implementing ScalarDS
    /**
     * Returns the datatype of the data object.
     *
     * @return the datatype of the data object.
     */
   @Override
    public Datatype getDatatype() {
        if (datatype == null) {
            try {
                datatype = new NC2Datatype(nativeDataset.getDataType());
            }
            catch (Exception ex) {
                datatype = null;
            }
        }

        return datatype;
    }

    /**
     * Sets the name of the data object.
     *
     * @param newName
     *            the new name of the object.
     */
    @Override
    public void setName(String newName) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
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
    public List getMetadata(int... attrPropList) throws Exception {
        throw new UnsupportedOperationException("getMetadata(int... attrPropList) is not supported");
    }
}
