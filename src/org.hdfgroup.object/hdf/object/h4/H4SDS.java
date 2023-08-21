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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.hdflib.HDFChunkInfo;
import hdf.hdflib.HDFCompInfo;
import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFDeflateCompInfo;
import hdf.hdflib.HDFException;
import hdf.hdflib.HDFJPEGCompInfo;
import hdf.hdflib.HDFLibrary;
import hdf.hdflib.HDFNBITCompInfo;
import hdf.hdflib.HDFSKPHUFFCompInfo;
import hdf.hdflib.HDFSZIPCompInfo;

import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.object.MetaDataContainer;

import hdf.object.h4.H4ScalarAttribute;

/**
 * H4SDS describes HDF4 Scientific Data Sets (SDS) and operations performed on
 * the SDS. A SDS is a group of data structures used to store and describe
 * multidimensional arrays of scientific data.
 *
 * The data contained in an SDS array has a data type associated with it. The
 * standard data types supported by the SD interface include 32- and 64-bit
 * floating-point numbers, 8-, 16- and 32-bit signed integers, 8-, 16- and
 * 32-bit unsigned integers, and 8-bit characters.
 *
 * <b>How to Select a Subset</b>
 *
 * Dataset defines APIs for reading, writing and subsetting a dataset. No function
 * is defined to select a subset of a data array. The selection is done in an implicit
 * way. Function calls to dimension information such as getSelectedDims() return an array
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

     // select dim1 and dim2 as 2D data for display,and slice through dim0
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

    // when dataset.read() is called, the slection above will be used since
    // the dimension arrays is passed by reference. Changes of these arrays
    // outside the dataset object directly change the values of these array
    // in the dataset object.

 * </pre>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H4SDS extends ScalarDS implements MetaDataContainer
{
    private static final long serialVersionUID = 2557157923292438696L;

    private static final Logger   log = LoggerFactory.getLogger(H4SDS.class);

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

    /**
     * The SDS interface identifier obtained from SDstart(filename, access)
     */
    private long                            sdid;

    /** the datatype identifier */
    private long                            datatypeID = -1;

    /** the number of attributes */
    private int                             nAttributes = -1;

    /**
     * Creates an H4SDS object with specific name and path.
     *
     * @param theFile
     *            the HDF file.
     * @param name
     *            the name of this H4SDS.
     * @param path
     *            the full path of this H4SDS.
     */
    public H4SDS(FileFormat theFile, String name, String path) {
        this(theFile, name, path, null);
    }

    /**
     * Creates an H4SDS object with specific name, path and oid.
     *
     * @param theFile
     *            the HDF file.
     * @param name
     *            the name of this H4SDS.
     * @param path
     *            the full path of this H4SDS.
     * @param oid
     *            the unique identifier of this data object.
     */
    @SuppressWarnings("deprecation")
    public H4SDS(FileFormat theFile, String name, String path, long[] oid) {
        super(theFile, name, path, oid);
        unsignedConverted = false;
        sdid = ((H4File)getFileFormat()).getSDAccessID();
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.DataFormat#hasAttribute()
     */
    @Override
    public boolean hasAttribute() {
        if (nAttributes < 0) {
            sdid = ((H4File)getFileFormat()).getSDAccessID();

            long id = open();

            if (id >= 0) {
                try { // retrieve attributes of the dataset
                    String[] objName = {""};
                    int[] sdInfo = {0, 0, 0};
                    int[] tmpDim = new int[HDFConstants.MAX_VAR_DIMS];
                    HDFLibrary.SDgetinfo(id, objName, tmpDim, sdInfo);
                    nAttributes = sdInfo[2];
                }
                catch (Exception ex) {
                    log.debug("hasAttribute(): failure: ", ex);
                    nAttributes=0;
                }

                log.trace("hasAttribute(): nAttributes={}", nAttributes);

                close(id);
            }
        }

        return (nAttributes>0);
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
                datatype = new H4Datatype(datatypeID);
            }
            catch (Exception ex) {
                log.debug("getDatatype(): failed to create datatype: ", ex);
                datatype = null;
            }
        }

        return datatype;
    }

    // To do: Implementing Dataset
    @Override
    public Dataset copy(Group pgroup, String dname, long[] dims, Object buff) throws Exception {
        log.trace("copy(): start: parentGroup={} datasetName={}", pgroup, dname);

        Dataset dataset = null;
        long srcdid = -1;
        long dstdid = -1;
        long tid = -1;
        int size = 1;
        int theRank = 2;
        String path = null;
        int[] count = null;
        int[] start = null;

        if (pgroup == null) {
            log.debug("copy(): Parent group is null");
            return null;
        }

        if (dname == null)
            dname = getName();

        if (pgroup.isRoot())
            path = HObject.SEPARATOR;
        else
            path = pgroup.getPath()+pgroup.getName()+HObject.SEPARATOR;
        log.trace("copy(): path={}", path);

        srcdid = open();
        if (srcdid < 0) {
            log.debug("copy(): Invalid source SDID");
            return null;
        }

        if (dims == null) {
            if (!isInited())
                init();

            theRank = getRank();

            dims = getDims();
        }
        else {
            theRank = dims.length;
        }

        start = new int[theRank];
        count = new int[theRank];
        for (int i=0; i<theRank; i++) {
            start[i] = 0;
            count[i] = (int)dims[i];
            size *= count[i];
        }
        log.trace("copy(): theRank={} with size={}", theRank, size);

        // create the new dataset and attach it to the parent group
        tid = datatypeID;
        dstdid = HDFLibrary.SDcreate(((H4File)pgroup.getFileFormat()).getSDAccessID(), dname, tid, theRank, count);
        if (dstdid < 0) {
            log.debug("copy(): Invalid dest SDID");
            return null;
        }

        int ref = HDFLibrary.SDidtoref(dstdid);
        if (!pgroup.isRoot()) {
            long vgid = pgroup.open();
            HDFLibrary.Vaddtagref(vgid, HDFConstants.DFTAG_NDG, ref);
            pgroup.close(vgid);
        }

        // copy attributes from one object to the new object
        log.trace("copy(): copy attributes");
        copyAttribute(srcdid, dstdid);

        // read data from the source dataset
        log.trace("copy(): read data from the source dataset");
        if (buff == null) {
            buff = new byte[size * HDFLibrary.DFKNTsize(tid)];
            HDFLibrary.SDreaddata(srcdid, start, null, count, buff);
        }

        // write the data into the destination dataset
        log.trace("copy(): write the data into the destination dataset");
        HDFLibrary.SDwritedata(dstdid, start, null, count, buff);

        long[] oid = {HDFConstants.DFTAG_NDG, ref};
        dataset = new H4SDS(pgroup.getFileFormat(), dname, path, oid);

        pgroup.addToMemberList(dataset);

        close(srcdid);

        try {
            HDFLibrary.SDendaccess(dstdid);
        }
        catch (HDFException ex) {
            log.debug("copy(): SDendaccess failure: ", ex);
        }

        return dataset;
    }

    // Implementing Dataset
    @Override
    public byte[] readBytes() throws HDFException {
        byte[] theData = null;

        if (!isInited())
            init();

        long id = open();
        if (id < 0) {
            log.debug("readBytes(): Invalid SDID");
            return null;
        }

        int datasize = 1;
        int[] select = new int[rank];
        int[] start = new int[rank];
        for (int i=0; i<rank; i++) {
            datasize *= (int)selectedDims[i];
            select[i] = (int)selectedDims[i];
            start[i] = (int)startDims[i];
        }

        int[] stride = null;
        if (selectedStride != null) {
            stride = new int[rank];
            for (int i=0; i<rank; i++) {
                stride[i] = (int)selectedStride[i];
            }
        }

        try {
            int size = HDFLibrary.DFKNTsize(datatypeID)*datasize;
            theData = new byte[size];
            HDFLibrary.SDreaddata(id, start, stride, select, theData);
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
    @Override
    public Object read() throws HDFException, OutOfMemoryError {
        Object theData = null;

        if (!isInited())
            init();

        long id = open();
        if (id < 0) {
            log.debug("read(): Invalid SDID");
            return null;
        }

        int datasize = 1;
        int[] select = new int[rank];
        int[] start = new int[rank];
        for (int i=0; i<rank; i++) {
            datasize *= (int)selectedDims[i];
            select[i] = (int)selectedDims[i];
            start[i] = (int)startDims[i];
        }

        int[] stride = null;
        if (selectedStride != null) {
            stride = new int[rank];
            for (int i=0; i<rank; i++) {
                stride[i] = (int)selectedStride[i];
            }
        }

        try {
            theData = H4Datatype.allocateArray(datatypeID, datasize);

            if (theData != null) {
                // assume external data files are located in the same directory as the main file.
                HDFLibrary.HXsetdir(getFileFormat().getParent());

                HDFLibrary.SDreaddata(id, start, stride, select, theData);

                if (isText)
                    theData = byteToString((byte[])theData, select[0]);
            }
        }
        catch (Exception ex) {
            log.debug("read(): failure: ", ex);
        }
        finally {
            close(id);
        }

        if (fillValue==null && isImageDisplay) {
            try {
                getMetadata();
            } // need to set fillValue for images
            catch (Exception ex) {
                log.debug("read(): getMetadata failure: ", ex);
            }
        }

        if ((rank > 1) && (selectedIndex[0] > selectedIndex[1]))
            isDefaultImageOrder = false;
        else
            isDefaultImageOrder = true;

        log.trace("read(): isDefaultImageOrder={}", isDefaultImageOrder);
        return theData;
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
    @SuppressWarnings("deprecation")
    @Override
    public void write(Object buf) throws HDFException {
        if (buf == null) {
            log.debug("write(): Object is null");
            return;
        }

        long id = open();
        if (id < 0) {
            log.debug("write(): Invalid SDID");
            return;
        }

        int[] select = new int[rank];
        int[] start = new int[rank];
        for (int i=0; i<rank; i++) {
            select[i] = (int)selectedDims[i];
            start[i] = (int)startDims[i];
        }

        int[] stride = null;
        if (selectedStride != null) {
            stride = new int[rank];
            for (int i=0; i<rank; i++) {
                stride[i] = (int)selectedStride[i];
            }
        }

        Object tmpData = buf;
        try {
            if (getDatatype().isUnsigned() && unsignedConverted)
                tmpData = convertToUnsignedC(buf);
            // assume external data files are located in the same directory as the main file.
            HDFLibrary.HXsetdir(getFileFormat().getParent());

            HDFLibrary.SDwritedata(id, start, stride, select, tmpData);
        }
        catch (Exception ex) {
            log.debug("write(): failure: ", ex);
        }
        finally {
            tmpData = null;
            close(id);
        }
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
        String[] objName = {""};
        int[] sdInfo = {0, 0, 0};
        try {
            // retrieve attributes of the dataset
            int[] tmpDim = new int[HDFConstants.MAX_VAR_DIMS];
            HDFLibrary.SDgetinfo(id, objName, tmpDim, sdInfo);
            int n = sdInfo[2];

            if ((attributeList == null) && (n>0))
                attributeList = new Vector(n, 5);

            boolean b = false;
            String[] attrName = new String[1];
            int[] attrInfo = {0, 0};
            for (int i=0; i<n; i++) {
                attrName[0] = "";
                try {
                    b = HDFLibrary.SDattrinfo(id, i, attrName, attrInfo);
                    // mask off the litend bit
                    attrInfo[0] = attrInfo[0] & (~HDFConstants.DFNT_LITEND);
                }
                catch (HDFException ex) {
                    log.debug("getMetadata(): attribute[{}] SDattrinfo failure: ", i, ex);
                    b = false;
                }

                if (!b)
                    continue;

                long[] attrDims = {attrInfo[1]};
                H4ScalarAttribute attr = new H4ScalarAttribute(this, attrName[0], new H4Datatype(attrInfo[0]), attrDims);
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
                    HDFLibrary.SDreadattr(id, i, buf);
                }
                catch (HDFException ex) {
                    log.debug("getMetadata(): attribute[{}] SDreadattr failure: ", i, ex);
                    buf = null;
                }

                if (buf != null) {
                    if ((attrInfo[0] == HDFConstants.DFNT_CHAR) ||
                        (attrInfo[0] ==  HDFConstants.DFNT_UCHAR8)) {
                        buf = Dataset.byteToString((byte[])buf, attrInfo[1]);
                    }
                    else if (attrName[0].equalsIgnoreCase("fillValue") ||
                            attrName[0].equalsIgnoreCase("_fillValue")) {
                        fillValue = buf;
                    }

                    attr.setAttributeData(buf);
                }

            } // (int i=0; i<n; i++)

            // retrieve attribute of dimension
            // BUG !! HDFLibrary.SDgetdimstrs(dimID, argv, 80) does not return anything
            /**
             * for (int i=0; i< rank; i++) { int dimID = HDFLibrary.SDgetdimid(id, i); String[] argv = {" ", "
             * ", " "}; HDFLibrary.SDgetdimstrs(dimID, argv, 80); }
             */
        }
        catch (Exception ex) {
            log.debug("getMetadata(): failure: ", ex);
        }
        finally {
            close(id);
        }

        return attributeList;
    }

    // To do: implementing DataFormat
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
            log.debug("writeMetadata(): Object not an H4ScalarAttribute");
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

    // Implementing HObject
    @Override
    public long open() {
        long id=-1;

        try {
            int index = 0;
            int tag = (int)oid[0];

            log.trace("open(): tag={}", tag);
            if (tag == H4SDS.DFTAG_NDG_NETCDF)
                index = (int)oid[1]; //HDFLibrary.SDidtoref(id) fails for netCDF
            else
                index = HDFLibrary.SDreftoindex(sdid, (int)oid[1]);

            id = HDFLibrary.SDselect(sdid,index);
        }
        catch (HDFException ex) {
            log.debug("open(): failure: ", ex);
            id = -1;
        }

        return id;
    }

    // Implementing HObject
    @Override
    public void close(long id) {
        try {
            HDFLibrary.SDendaccess(id);
        }
        catch (HDFException ex) {
            log.debug("close(): failure: ", ex);
        }
    }

    /**
     * Initializes the H4SDS such as dimension size of this dataset.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void init() {
        if (inited) {
            log.trace("init(): Already initialized");
            return; // already called. Initialize only once
        }

        long id = open();
        String[] objName = {""};
        String[] dimName = {""};
        int[] dimInfo = {0, 0, 0};
        int[] sdInfo = {0, 0, 0};
        boolean isUnlimited = false;

        int[] idims = new int[HDFConstants.MAX_VAR_DIMS];
        try {
            HDFLibrary.SDgetinfo(id, objName, idims, sdInfo);
            // mask off the litend bit
            sdInfo[1] = sdInfo[1] & (~HDFConstants.DFNT_LITEND);
            nAttributes = sdInfo[2];
            rank = sdInfo[0];

            if (rank <= 0) {
                rank = 1;
                idims[0] = 1;
            }

            isUnlimited = HDFLibrary.SDisrecord(id);
            log.trace("init(): isUnlimited={}", isUnlimited);

            datatypeID = sdInfo[1];
            isText = ((datatypeID == HDFConstants.DFNT_CHAR) || (datatypeID == HDFConstants.DFNT_UCHAR8));

            // get the dimension names
            try {
                dimNames = new String[rank];
                for (int i=0; i<rank; i++) {
                    long dimid = HDFLibrary.SDgetdimid(id, i);
                    HDFLibrary.SDdiminfo(dimid, dimName, dimInfo);
                    dimNames[i] = dimName[0];
                }
            }
            catch (Exception ex) {
                log.debug("init(): get the dimension names: ", ex);
            }

            // get compression information
            try {
                HDFCompInfo compInfo = new HDFCompInfo();
                HDFLibrary.SDgetcompinfo(id, compInfo);

                compression.setLength(0);

                if (compInfo.ctype == HDFConstants.COMP_CODE_DEFLATE) {
                    HDFDeflateCompInfo comp = new HDFDeflateCompInfo();
                    HDFLibrary.SDgetcompinfo(id, comp);
                    compression.append("GZIP(level=").append(comp.level).append(")");
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_SZIP) {
                    HDFSZIPCompInfo comp = new HDFSZIPCompInfo();
                    HDFLibrary.SDgetcompinfo(id, comp);
                    compression.append("SZIP(bits_per_pixel=").append(comp.bits_per_pixel).append(",options_mask=")
                            .append(comp.options_mask).append(",pixels=").append(comp.pixels).append(",pixels_per_block=")
                            .append(comp.pixels_per_block).append(",pixels_per_scanline=").append(comp.pixels_per_scanline).append(")");
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_JPEG) {
                    HDFJPEGCompInfo comp = new HDFJPEGCompInfo();
                    HDFLibrary.SDgetcompinfo(id, comp);
                    compression.append("JPEG(quality=").append(comp.quality).append(",options_mask=")
                            .append(",force_baseline=").append(comp.force_baseline).append(")");
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_SKPHUFF) {
                    HDFSKPHUFFCompInfo comp = new HDFSKPHUFFCompInfo();
                    HDFLibrary.SDgetcompinfo(id, comp);
                    compression.append("SKPHUFF(skp_size=").append(comp.skp_size).append(")");
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_RLE) {
                    compression.append("RLE");
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_NBIT) {
                    HDFNBITCompInfo comp = new HDFNBITCompInfo();
                    HDFLibrary.SDgetcompinfo(id, comp);
                    compression.append("NBIT(nt=").append(comp.nt).append(",bit_len=").append(comp.bit_len)
                            .append(",ctype=").append(comp.ctype).append(",fill_one=").append(comp.fill_one)
                            .append(",sign_ext=").append(comp.sign_ext).append(",start_bit=").append(comp.start_bit).append(")");
                }

                if (compression.length() == 0)
                    compression.append("NONE");
            }
            catch (Exception ex) {
                log.debug("init(): get compression information failure: ", ex);
            }

            // get chunk information
            try {
                HDFChunkInfo chunkInfo = new HDFChunkInfo();
                int[] cflag = {HDFConstants.HDF_NONE};

                try {
                    HDFLibrary.SDgetchunkinfo(id, chunkInfo, cflag);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }

                storageLayout.setLength(0);

                if (cflag[0] == HDFConstants.HDF_NONE) {
                    chunkSize = null;
                    storageLayout.append("NONE");
                }
                else {
                    chunkSize = new long[rank];
                    for (int i=0; i<rank; i++)
                        chunkSize[i] = chunkInfo.chunk_lengths[i];
                    storageLayout.append("CHUNKED: ").append(chunkSize[0]);
                    for (int i = 1; i < rank; i++)
                        storageLayout.append(" X ").append(chunkSize[i]);
                }
            }
            catch (Exception ex) {
                log.debug("init(): get chunk information failure: ", ex);
            }

            inited = true;
        }
        catch (HDFException ex) {
            log.debug("init(): failure: ", ex);
        }
        finally {
            close(id);
        }

        dims = new long[rank];
        maxDims = new long[rank];
        startDims = new long[rank];
        selectedDims = new long[rank];

        for (int i=0; i<rank; i++) {
            startDims[i] = 0;
            selectedDims[i] = 1;
            dims[i] = maxDims[i] = idims[i];
        }

        if (isUnlimited)
            maxDims[0] = -1;

        selectedIndex[0] = 0;
        selectedIndex[1] = 1;
        selectedIndex[2] = 2;

        // select only two dimension a time,
        if (rank == 1)
            selectedDims[0] = dims[0];

        if (rank > 1) {
            selectedDims[0] = dims[0];
            if (isText)
                selectedDims[1] = 1;
            else
                selectedDims[1] = dims[1];
        }
    }

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
     * @param fillValue the default value.
     * @param data the array of data values.
     *
     * @return the new dataset if successful. Otherwise returns null.
     *
     * @throws Exception if the dataset can not be created
     */
    public static H4SDS create(String name, Group pgroup, Datatype type, long[] dims, long[] maxdims,
            long[] chunks, int gzip, Object fillValue, Object data) throws Exception {
        H4SDS dataset = null;
        if ((pgroup == null) || (name == null)|| (dims == null)) {
            log.trace("create(): Parent group, name or dims is null");
            return null;
        }

        H4File file = (H4File)pgroup.getFileFormat();

        if (file == null) {
            log.trace("create(): Parent group FileFormat is null");
            return null;
        }

        String path = HObject.SEPARATOR;
        if (!pgroup.isRoot())
            path = pgroup.getPath()+pgroup.getName()+HObject.SEPARATOR;
        // prepare the dataspace
        int rank = dims.length;
        int[] idims = new int[rank];
        int[] start = new int[rank];
        for (int i=0; i<rank; i++) {
            idims[i] = (int)dims[i];
            start[i] = 0;
        }

        // only the first element of the SDcreate parameter dim_sizes (i.e.,
        // the dimension of the lowest rank or the slowest-changing dimension)
        // can be assigned the value SD_UNLIMITED (or 0) to make the first
        // dimension unlimited.
        if ((maxdims != null) && (maxdims[0]<=0))
            idims[0] = 0; // set to unlimited dimension.

        int[] ichunks = null;
        if (chunks != null) {
            ichunks = new int[rank];
            for (int i=0; i<rank; i++)
                ichunks[i] = (int)chunks[i];
        }

        // unlimited cannot be used with chunking or compression for HDF 4.2.6 or earlier.
        if (idims[0] == 0 && (ichunks != null || gzip>0)) {
            log.debug("create(): Unlimited cannot be used with chunking or compression");
            throw new HDFException("Unlimited cannot be used with chunking or compression");
        }

        long sdid = (file).getSDAccessID();
        long sdsid = -1;
        long vgid = -1;
        long tid = type.createNative();

        if(tid >= 0) {
            try {
                sdsid = HDFLibrary.SDcreate(sdid, name, tid, rank, idims);
                // set fill value to zero.
                int vsize = HDFLibrary.DFKNTsize(tid);
                byte[] fill = new byte[vsize];
                for (int i=0; i<vsize; i++)
                    fill[i] = 0;
                HDFLibrary.SDsetfillvalue(sdsid, fill);

                // when we create a new dataset with unlimited dimension,
                // we have to write some data into the dataset or otherwise
                // the current dataset has zero dimensin size.
            }
            catch (Exception ex) {
                log.debug("create(): failure: ", ex);
                throw (ex);
            }
        }

        if (sdsid < 0) {
            log.debug("create(): Dataset creation failed");
            throw (new HDFException("Unable to create the new dataset."));
        }

        HDFDeflateCompInfo compInfo = null;
        if (gzip > 0) {
            // set compression
            compInfo = new HDFDeflateCompInfo();
            compInfo.level = gzip;
            if (chunks == null)
                HDFLibrary.SDsetcompress(sdsid, HDFConstants.COMP_CODE_DEFLATE, compInfo);
        }

        if (chunks != null) {
            // set chunk
            HDFChunkInfo chunkInfo = new HDFChunkInfo(ichunks);
            int flag = HDFConstants.HDF_CHUNK;

            if (gzip > 0) {
                flag = HDFConstants.HDF_CHUNK | HDFConstants.HDF_COMP;
                chunkInfo = new HDFChunkInfo(ichunks, HDFConstants.COMP_CODE_DEFLATE, compInfo);
            }

            try  {
                HDFLibrary.SDsetchunk (sdsid, chunkInfo, flag);
            }
            catch (Exception err) {
                log.debug("create(): SDsetchunk failure: ", err);
                err.printStackTrace();
                throw new HDFException("SDsetchunk failed.");
            }
        }

        if ((sdsid > 0) && (data != null))
            HDFLibrary.SDwritedata(sdsid, start, null, idims, data);

        int ref = HDFLibrary.SDidtoref(sdsid);

        if (!pgroup.isRoot()) {
            // add the dataset to the parent group
            vgid = pgroup.open();
            if (vgid < 0) {
                if (sdsid > 0)
                    HDFLibrary.SDendaccess(sdsid);
                log.debug("create(): Invalid Parent Group ID");
                throw (new HDFException("Unable to open the parent group."));
            }

            HDFLibrary.Vaddtagref(vgid, HDFConstants.DFTAG_NDG, ref);

            pgroup.close(vgid);
        }

        try {
            if (sdsid > 0)
                HDFLibrary.SDendaccess(sdsid);
        }
        catch (Exception ex) {
            log.debug("create(): SDendaccess failure: ", ex);
        }

        long[] oid = {HDFConstants.DFTAG_NDG, ref};
        dataset = new H4SDS(file, name, path, oid);

        if (dataset != null)
            pgroup.addToMemberList(dataset);

        return dataset;
    }

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
     * @throws Exception if the dataset can not be created
     */
    public static H4SDS create(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks, int gzip, Object data) throws Exception {
        return create(name, pgroup, type, dims, maxdims, chunks, gzip, null, data);
    }

    /**
     * copy attributes from one SDS to another SDS
     */
    private void copyAttribute(long srcdid, long dstdid) {
        log.trace("copyAttribute(): start: srcdid={} dstdid={}", srcdid, dstdid);
        try {
            String[] objName = {""};
            int[] sdInfo = {0, 0, 0};
            int[] tmpDim = new int[HDFConstants.MAX_VAR_DIMS];
            HDFLibrary.SDgetinfo(srcdid, objName, tmpDim, sdInfo);
            int numberOfAttributes = sdInfo[2];
            log.trace("copyAttribute(): numberOfAttributes={}", numberOfAttributes);

            boolean b = false;
            String[] attrName = new String[1];
            int[] attrInfo = {0, 0};
            for (int i=0; i<numberOfAttributes; i++) {
                attrName[0] = "";
                try {
                    b = HDFLibrary.SDattrinfo(srcdid, i, attrName, attrInfo);
                }
                catch (HDFException ex) {
                    log.debug("copyAttribute(): attribute[{}] SDattrinfo failure: ", i, ex);
                    b = false;
                }

                if (!b)
                    continue;

                // read attribute data from source dataset
                byte[] attrBuff = new byte[attrInfo[1] * HDFLibrary.DFKNTsize(attrInfo[0])];
                try {
                    HDFLibrary.SDreadattr(srcdid, i, attrBuff);
                }
                catch (HDFException ex) {
                    log.debug("copyAttribute(): attribute[{}] SDreadattr failure: ", i, ex);
                    attrBuff = null;
                }

                if (attrBuff == null) {
                    log.debug("copyAttribute(): attrBuff[{}] is null", i);
                    continue;
                }

                // attach attribute to the destination dataset
                HDFLibrary.SDsetattr(dstdid, attrName[0], attrInfo[0], attrInfo[1], attrBuff);
            } // (int i=0; i<numberOfAttributes; i++)
        }
        catch (Exception ex) {
            log.debug("copyAttribute(): failure: ", ex);
        }
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
}
