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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.object.MetaDataContainer;

import hdf.object.fits.FitsAttribute;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

/**
 * FitsDataset describes an multi-dimension array of HDF5 scalar or atomic data types, such as byte, int, short, long,
 * float, double and string, and operations performed on the scalar dataset
 *
 * The library predefines a modest number of datatypes. For details, read
 * <a href="https://hdfgroup.github.io/hdf5/_h5_t__u_g.html#sec_datatype">HDF5 Datatypes in HDF5 User Guide</a>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class FitsDataset extends ScalarDS implements MetaDataContainer
{
    private static final long serialVersionUID = 3944770379558335171L;

    private static final Logger log = LoggerFactory.getLogger(FitsDataset.class);

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of Attribute.
     */
    private List attributeList;

    /** the native dataset */
    private BasicHDU nativeDataset;

    /**
     * Constructs an FitsDataset object with specific netcdf variable.
     *
     * @param fileFormat the netcdf file.
     * @param hdu the BasicHDU.
     * @param dName the name for this dataset.
     * @param oid the unique identifier for this dataset.
     */
    public FitsDataset(FileFormat fileFormat, BasicHDU hdu, String dName, long[] oid) {
        super(fileFormat, dName, HObject.SEPARATOR, oid);
        unsignedConverted = false;
        nativeDataset = hdu;
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
        throw new UnsupportedOperationException("copy operation unsupported for FITS.");
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Dataset#readBytes()
     */
    @Override
    public byte[] readBytes() throws Exception {
        // not supported
        throw new UnsupportedOperationException("readBytes operation unsupported for FITS.");
    }

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
        Object fitsData = null;

        if (nativeDataset == null)
            return null;

        try {
            fitsData = nativeDataset.getData().getData();
        }
        catch (Exception ex) {
            throw new UnsupportedOperationException("This implementation only supports integer and float dataset. " +
                    "It may not work for other datatypes. \n"+ex);
        }

        int n = get1DLength(fitsData);

        theData = FitsDatatype.allocateArray(nativeDataset.getBitPix(), n);

        to1Darray(fitsData, theData, 0);

        return theData;
    }

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
        throw new UnsupportedOperationException("write operation unsupported for FITS.");
    }

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
    @SuppressWarnings("rawtypes")
    public List getMetadata() throws Exception {
        if (attributeList != null)
            return attributeList;

        if (nativeDataset == null)
            return null;

        Header header = nativeDataset.getHeader();
        if (header == null)
            return null;

        attributeList = new Vector();
        HeaderCard hc = null;
        Iterator it = header.iterator();
        FitsAttribute attr = null;
        Datatype dtype = new FitsDatatype(Datatype.CLASS_STRING, 80, 0, 0);
        long[] dims = {1};
        String value = null;
        while (it.hasNext()) {
            value = "";
            hc = (HeaderCard)it.next();
            attr = new FitsAttribute(this, hc.getKey(), dtype, dims);
            String tvalue = hc.getValue();
            if (tvalue != null)
                value += tvalue;
            tvalue = hc.getComment();
            if (tvalue != null)
                value += " / " + tvalue;
            attr.setAttributeData(value);
            attributeList.add(attr);
        }

        return attributeList;
    }

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
    public void writeMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("writeMetadata operation unsupported for FITS.");
    }

    /**
     * Deletes an existing piece of metadata from this object.
     *
     * @param info
     *            the metadata to delete.
     *
     * @throws Exception
     *             if the metadata can not be removed
     */
    public void removeMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("removeMetadata operation unsupported for FITS.");
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
    public void updateMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("updateMetadata operation unsupported for FITS.");
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        return -1;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long did) {
        // Nothing to implement
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.Dataset#init()
     */
    @Override
    public void init() {
        if (nativeDataset == null)
            return;

        if (inited)
            return; // already called. Initialize only once

        int[] axes= null;
        try {
            axes = nativeDataset.getAxes();
        }
        catch (Exception ex) {
            log.debug("nativeDataset.getAxes():", ex);
        }

        if (axes == null)
            return;


        rank = axes.length;
        if (rank == 0) {
            // a scalar data point
            isScalar = true;
            rank = 1;
            dims = new long[] { 1 };
        }
        else {
            isScalar = false;
            dims = new long[rank];
            for (int i=0; i<rank; i++)
                dims[i] = axes[i];
        }

        startDims = new long[rank];
        selectedDims = new long[rank];
        for (int i=0; i<rank; i++) {
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

    /* Implement abstart ScalarDS */

    /**
     * Creates a new dataset.
     *
     * @param name the name of the dataset to create.
     * @param pgroup the parent group of the new dataset.
     * @param type the datatype of the dataset.
     * @param dims the dimension size of the dataset.
     * @param maxdims the max dimension size of the dataset.
     * @param chunks the chunk size of the dataset.
     * @param gzip the level of the gzip compression.
     * @param data the array of data values.
     *
     * @return the new dataset if successful. Otherwise returns null.
     *
     * @throws Exception
     *            if there is an error
     */
    public static FitsDataset create(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks, int gzip, Object data) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for FITS.");
    }

    /**
     * Returns the datatype of the data object.
     *
     * @return the datatype of the data object.
     */
    @Override
    public Datatype getDatatype() {
        if (datatype == null) {
            try {
                datatype = new FitsDatatype(nativeDataset.getBitPix());
            }
            catch (Exception ex) {
                log.debug("getDatatype(): failed to create datatype: ", ex);
                datatype = null;
            }
        }

        return datatype;
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.HObject#setName(java.lang.String)
     */
    @Override
    public void setName (String newName) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for FITS.");
    }

    private int get1DLength(Object data) throws Exception {
        if (!data.getClass().isArray())
            return 1;

        int len = Array.getLength(data);

        int total = 0;
        for (int i = 0; i < len; i++)
            total += get1DLength(Array.get(data, i));

        return total;
    }

    /** copy multi-dimension array of fits data into 1D array */
    private int to1Darray(Object dataIn, Object dataOut, int offset) throws Exception {
        Class component = dataIn.getClass().getComponentType();
        if (component == null)
            return offset;

        int size = Array.getLength(dataIn);
        if (!component.isArray()) {
            System.arraycopy(dataIn, 0, dataOut, offset, size);
            return offset+size;
        }

        for (int i = size - 1; i >= 0; i--)
            offset = to1Darray(Array.get(dataIn, i), dataOut, offset);

        return offset;
    }

    //Implementing DataFormat
    /* FITS does not support metadata */
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
