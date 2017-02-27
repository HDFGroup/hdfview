/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.object.h4;

import java.util.List;
import java.util.Vector;

import hdf.hdflib.HDFChunkInfo;
import hdf.hdflib.HDFCompInfo;
import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFDeflateCompInfo;
import hdf.hdflib.HDFException;
import hdf.hdflib.HDFLibrary;
import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * H4GRImage describes an HDF4 general raster(GR) image and operations performed on
 * the GR image. An HDF4 raster image is a two-dimension array of pixel values.
 * <p>
 * Every GR data set must contain the following components: image array, name,
 * pixel type, and dimensions. The name, dimensions, and pixel type must be
 * supplied by the user at the time the GR data set is defined.
 * <p>
 * An image array is a two-dimensional array of pixels. Each element in an image
 * array corresponds to one pixel and each pixel can consist of a number of
 * color component values or pixel components, e.g., Red-Green-Blue or RGB,
 * Cyan-Magenta-Yellow-Black or CMYK, etc. Pixel components can be represented
 * by different methods (8-bit lookup table or 24-bit direct representation) and
 * may have different data types. The data type of pixel components and the number
 * of components in each pixel are collectively known as the pixel type.
 * <p>
 * <b>How to Select a Subset</b>
 * <p>
 * Dataset defines APIs for reading, writing and subsetting a dataset. No function is
 * defined to select a subset of a data array. The selection is done in an implicit way.
 * Function calls to dimension information such as getSelectedDims() return an array
 * of dimension values, which is a reference to the array in the dataset object.
 * Changes of the array outside the dataset object directly change the values of
 * the array in the dataset object. It is like pointers in C.
 * <p>
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

    // when dataset.read() is called, the slection above will be used since
    // the dimension arrays are passed by reference. Changes of these arrays
    // outside the dataset object directly change the values of these array
    // in the dataset object.

 * </pre>
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H4GRImage extends ScalarDS
{
    private static final long serialVersionUID = 1029672744963360976L;

    private final static org.slf4j.Logger   log = org.slf4j.LoggerFactory.getLogger(H4GRImage.class);

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of Attribute.
     */
    @SuppressWarnings("rawtypes")
    private List                            attributeList;

    /**
     * The GR interface identifier obtained from GRstart(fid)
     */
    private long                            grid;

    /**
     * The number of components in the raster image
     */
    private int                             ncomp;

    /** the datatype identifier */
    private long                            datatypeID = -1;

    private int                             nAttributes = -1;


    public H4GRImage(FileFormat theFile, String name, String path)
    {
        this(theFile, name, path, null);
    }

    /**
     * Creates a H4GRImage object with specific name, path, and object ID.
     *
     * @param theFile the HDF file.
     * @param name the name of this H4GRImage.
     * @param path the full path of this H4GRImage.
     * @param oid the unique identifier of this data object.
     */
    public H4GRImage(
        FileFormat theFile,
        String name,
        String path,
        long[] oid)
    {
        super (theFile, name, path, oid);
        palette = null;
        isImage = isImageDisplay = true;
        unsignedConverted = false;
        grid = ((H4File)getFileFormat()).getGRAccessID();
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.DataFormat#hasAttribute()
     */
    public boolean hasAttribute ()
    {
        if (nAttributes < 0) {
            grid = ((H4File)getFileFormat()).getGRAccessID();

            long id = open();

            if (id >= 0) {
                String[] objName = {""};
                int[] grInfo = new int[4]; //ncomp, data_type, interlace, and num_attrs
                int[] idims = new int[2];
                try {
                    HDFLibrary.GRgetiminfo(id, objName, grInfo, idims);
                    nAttributes = grInfo[3];
                }
                catch (Exception ex) {
                    log.trace("hasAttribute() failure: ", ex);
                    nAttributes = 0;
                }

                log.trace("hasAttribute(): nAttributes={}", nAttributes);

                close(id);
            }
        }

        return (nAttributes > 0);
    }

    // To do: Implementing Dataset
    @Override
    public Dataset copy(Group pgroup, String dname, long[] dims, Object buff) throws Exception
    {
        log.trace("copy(): start: parentGroup={} datasetName={}", pgroup, dname);

        Dataset dataset = null;
        long srcdid=-1, dstdid=-1;
        String path=null;
        int[] count=null;

        if (pgroup == null) {
            log.debug("copy(): Parent group is null");
            log.trace("copy(): finish");
            return null;
        }

        if (pgroup.isRoot()) {
            path = HObject.separator;
        }
        else {
            path = pgroup.getPath()+pgroup.getName()+HObject.separator;
        }

        srcdid = open();
        if (srcdid < 0) {
            log.debug("copy(): Invalid source dataset ID");
            log.trace("copy(): finish");
            return null;
        }

        if (dims != null) {
            count = new int[2];
            count[0] = (int)dims[0];
            count[1] = (int)dims[1];
        }

        int[] grInfo = new int[4]; //ncomp, data_type, interlace and num_attrs
        try {
            String[] tmpName = {""};
            int[] tmpDims = new int[2];
            HDFLibrary.GRgetiminfo(srcdid, tmpName, grInfo, tmpDims);
            if (count == null) {
                count = tmpDims;
            }
        }
        catch (HDFException ex) {
            log.debug("copy(): GRgetiminfo failure: ", ex);
        }

        int ncomp = grInfo[0];
        long tid = (long)grInfo[1];
        int interlace = grInfo[2];
        int numberOfAttributes = grInfo[3];
        dstdid = HDFLibrary.GRcreate(
            ((H4File)pgroup.getFileFormat()).getGRAccessID(),
            dname, ncomp, tid, interlace, count);
        if (dstdid < 0) {
            log.debug("copy(): Invalid dest dataset ID");
            log.trace("copy(): finish");
            return null;
        }

        int ref = HDFLibrary.GRidtoref(dstdid);
        if (!pgroup.isRoot()) {
            long vgid = pgroup.open();
            HDFLibrary.Vaddtagref(vgid, HDFConstants.DFTAG_RIG, ref);
            pgroup.close(vgid);
        }

        // read data from the source dataset
        int[] start = {0, 0};
        if (buff == null) {
            buff = new byte[count[0]*count[1] * HDFLibrary.DFKNTsize(tid)];
            HDFLibrary.GRreadimage(srcdid, start, null, count, buff);
        }

        // write the data into the destination dataset
        HDFLibrary.GRwriteimage(dstdid, start, null, count, buff);

        // copy palette
        long pid = HDFLibrary.GRgetlutid(srcdid, 0);
        int[] palInfo = new int[4];

        HDFLibrary.GRgetlutinfo(pid, palInfo);
        palInfo[1] = HDFConstants.DFNT_UINT8; // support unsigned byte only. Other type does not work
        int palSize = palInfo[0]*palInfo[3];
        byte[] palBuff = new byte[palSize];
        HDFLibrary.GRreadlut(pid, palBuff);
        pid = HDFLibrary.GRgetlutid(dstdid, 0);
        HDFLibrary.GRwritelut(pid, palInfo[0], palInfo[1], palInfo[2], palInfo[3], palBuff);

        // copy attributes from one object to the new object
        log.trace("copy(): copyAttributes: numAttributes={}", numberOfAttributes);
        copyAttribute(srcdid, dstdid, numberOfAttributes);

        long[] oid = {HDFConstants.DFTAG_RIG, ref};
        dataset = new H4GRImage(pgroup.getFileFormat(), dname, path, oid);

        pgroup.addToMemberList(dataset);

        close(srcdid);

        try {
            HDFLibrary.GRendaccess(dstdid);
        }
        catch (HDFException ex) {
            log.debug("copy(): GRendaccess failure: ", ex);
        }

        log.trace("copy(): finish");
        return dataset;
    }

    // ***** need to implement from ScalarDS *****
    @Override
    public byte[][] readPalette(int idx) { return null;}

    // ***** need to implement from ScalarDS *****
    @Override
    public byte[] getPaletteRefs() { return null;}

    // implementing ScalarDS
    @Override
    public Datatype getDatatype()
    {
        if (datatype == null) {
            datatype = new H4Datatype(datatypeID);
        }

        return datatype;
    }

    // Implementing Dataset
    @Override
    public byte[] readBytes() throws HDFException
    {
        log.trace("readBytes(): start");

        byte[] theData = null;

        if (rank <= 0) {
            init();
        }

        long id = open();
        if (id < 0) {
            log.debug("readBytes(): Invalid ID");
            log.trace("readBytes(): finish");
            return null;
        }

        try {
            // set the interlacing scheme for reading image data
            HDFLibrary.GRreqimageil(id, interlace);
            int datasize = (int)(getWidth()*getHeight()*ncomp);
            int size = HDFLibrary.DFKNTsize(datatypeID)*datasize;
            theData = new byte[size];
            int[] start = {(int)startDims[0], (int)startDims[1]};
            int[] select = {(int)selectedDims[0], (int)selectedDims[1]};

            int[] stride = null;
            if (selectedStride != null) {
                stride = new int[rank];
                for (int i=0; i<rank; i++) {
                    stride[i] = (int)selectedStride[i];
                }
            }

            HDFLibrary.GRreadimage(id, start, stride, select, theData);
        }
        catch (Exception ex) {
            log.debug("readBytes(): failure: ", ex);
        }
        finally {
            close(id);
        }

        log.trace("readBytes(): finish");
        return theData;
    }

    // ***** need to implement from DataFormat *****
    @Override
    public Object read() throws HDFException
    {
        log.trace("read(): start");

        Object theData = null;

        if (rank <=0 ) {
            init();
        }

        long id = open();
        if (id < 0) {
            log.debug("read(): Invalid ID");
            log.trace("read(): finish");
            return null;
        }

        try {
            // set the interlacing scheme for reading image data
            HDFLibrary.GRreqimageil(id, interlace);
            int datasize = (int)(getWidth()*getHeight()*ncomp);

            theData = H4Datatype.allocateArray(datatypeID, datasize);

            if (theData != null) {
                // assume external data files are located in the same directory as the main file.
                HDFLibrary.HXsetdir(getFileFormat().getParent());

                int[] start = {(int)startDims[0], (int)startDims[1]};
                int[] select = {(int)selectedDims[0], (int)selectedDims[1]};

                int[] stride = null;
                if (selectedStride != null) {
                    stride = new int[rank];
                    for (int i=0; i<rank; i++) {
                        stride[i] = (int)selectedStride[i];
                    }
                }

                HDFLibrary.GRreadimage(id, start, stride, select, theData);
            }
        }
        catch (Exception ex) {
            log.debug("read(): failure: ", ex);
        }
        finally {
            close(id);
        }

        if ( (rank >1) && (selectedIndex[1]>selectedIndex[0]))
            isDefaultImageOrder = false;
        else
            isDefaultImageOrder = true;

        log.trace("read(): isDefaultImageOrder={}", isDefaultImageOrder);
        log.trace("read(): finish");
        return theData;
    }

    // Implementing DataFormat
    @Override
    public void write(Object buf) throws HDFException
    {
        log.trace("write(): start");

        if (buf == null) {
            log.debug("write(): buf is null");
            log.trace("write(): finish");
            return;
        }

        long id = open();
        if (id < 0) {
            log.debug("write(): Invalid ID");
            log.trace("write(): finish");
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
            if ( isUnsigned && unsignedConverted) {
                tmpData = convertToUnsignedC(buf);
            }
            // assume external data files are located in the same directory as the main file.
            HDFLibrary.HXsetdir(getFileFormat().getParent());

            HDFLibrary.GRwriteimage(id, start, stride, select, tmpData);
        }
        catch (Exception ex) {
            log.debug("write(): failure: ", ex);
        }
        finally {
            tmpData = null;
            close(id);
        }

        log.trace("write(): finish");
    }

    // ***** need to implement from DataFormat *****
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getMetadata() throws HDFException
    {
        log.trace("getMetadata(): start");

        if (attributeList != null) {
            log.trace("getMetadata(): attributeList != null");
            log.trace("getMetadata(): finish");
            return attributeList;
        }

        long id = open();
        String[] objName = {""};
        int[] grInfo = new int[4]; //ncomp, data_type, interlace, and num_attrs
        int[] idims = new int[2];
        try {
            HDFLibrary.GRgetiminfo(id, objName, grInfo, idims);
            // mask off the litend bit
            grInfo[1] = grInfo[1] & (~HDFConstants.DFNT_LITEND);
            int n = grInfo[3];

            if ((attributeList == null) && (n>0)) {
                attributeList = new Vector(n, 5);
            }

            boolean b = false;
            String[] attrName = new String[1];
            int[] attrInfo = {0, 0}; // data_type, length
            for (int i=0; i<n; i++) {
                attrName[0] = "";
                try {
                    b = HDFLibrary.GRattrinfo(id, i, attrName, attrInfo);
                    // mask off the litend bit
                    attrInfo[0] = attrInfo[0] & (~HDFConstants.DFNT_LITEND);
                }
                catch (HDFException ex) {
                    log.debug("getMetadata(): GRattrinfo failure: ", ex);
                    b = false;
                }

                if (!b) {
                    continue;
                }

                long[] attrDims = {attrInfo[1]};
                Attribute attr = new Attribute(attrName[0], new H4Datatype(attrInfo[0]), attrDims);
                attributeList.add(attr);

                Object buf = H4Datatype.allocateArray(attrInfo[0], attrInfo[1]);
                try {
                    HDFLibrary.GRgetattr(id, i, buf);
                }
                catch (HDFException ex) {
                    log.debug("getMetadata(): GRgetattr failure: ", ex);
                    buf = null;
                }

                if (buf != null) {
                    if ((attrInfo[0] == HDFConstants.DFNT_CHAR) ||
                        (attrInfo[0] ==  HDFConstants.DFNT_UCHAR8)) {
                        buf = Dataset.byteToString((byte[])buf, attrInfo[1]);
                    }

                    attr.setValue(buf);
                }
            } // for (int i=0; i<n; i++)
        }
        catch (Exception ex) {
            log.debug("getMetadata(): failure: ", ex);
        }
        finally {
            close(id);
        }

        log.trace("getMetadata(): finish");
        return attributeList;
    }

    // ***** need to implement from DataFormat *****
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void writeMetadata(Object info) throws Exception
    {
        log.trace("writeMetadata(): start");

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("writeMetadata(): Object not an Attribute");
            log.trace("writeMetadata(): finish");
            return;
        }

        try {
            getFileFormat().writeAttribute(this, (Attribute)info, true);

            if (attributeList == null) {
                attributeList = new Vector();
            }

            attributeList.add(info);
            nAttributes = attributeList.size();
        }
        catch (Exception ex) {
            log.debug("writeMetadata(): failure: ", ex);
        }

        log.trace("writeMetadata(): finish");
    }

    // ***** need to implement from DataFormat *****
    public void removeMetadata(Object info) throws HDFException {
        log.trace("removeMetadata(): disabled");
    }

    // implementing DataFormat
    public void updateMetadata(Object info) throws Exception {
        log.trace("updateMetadata(): disabled");
    }

    // Implementing HObject.
    @Override
    public long open()
    {
        log.trace("open(): start: for file={} with ref={}", getFID(), (short) oid[1]);

        long id = -1;
        try {
            int index = HDFLibrary.GRreftoindex(grid, (short)oid[1]);
            id = HDFLibrary.GRselect(grid, index);
        }
        catch (HDFException ex) {
            log.debug("open(): failure: ", ex);
            id = -1;
        }

        log.trace("open(): finish");
        return id;
    }

    // Implementing HObject.
    @Override
    public void close(long grid)
    {
        try { HDFLibrary.GRendaccess(grid); }
        catch (HDFException ex) {log.debug("close(): failure: ", ex);}
    }

    // Implementing Dataset.
    @Override
    public void init()
    {
        log.trace("init(): start");

        if (rank>0) {
            log.trace("init(): Already initialized");
            log.trace("init(): finish");
            return; // already called. Initialize only once
        }

        long id = open();
        String[] objName = {""};
        int[] grInfo = new int[4]; //ncomp, data_type, interlace and num_attrs
        int[] idims = new int[2];
        try {
            HDFLibrary.GRgetiminfo(id, objName, grInfo, idims);
            // mask off the litend bit
            grInfo[1] = grInfo[1] & (~HDFConstants.DFNT_LITEND);
            datatypeID = grInfo[1];

            // get compression information
            try {
                HDFCompInfo compInfo = new HDFCompInfo();
                HDFLibrary.GRgetcompinfo(id, compInfo);
                if (compInfo.ctype == HDFConstants.COMP_CODE_DEFLATE) {
                    compression = "GZIP";
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_SZIP) {
                    compression = "SZIP";
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_JPEG) {
                    compression = "JPEG";
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_SKPHUFF) {
                    compression = "SKPHUFF";
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_RLE) {
                    compression = "RLE";
                }
                else if (compInfo.ctype == HDFConstants.COMP_CODE_NBIT) {
                    compression = "NBIT";
                }
            }
            catch (Exception ex) {
                log.debug("init(): get compression information failure: ", ex);
            }

            // get chunk information
            try {
                HDFChunkInfo chunkInfo = new HDFChunkInfo();
                int[] cflag = {HDFConstants.HDF_NONE};
                HDFLibrary.GRgetchunkinfo(id, chunkInfo, cflag);
                if (cflag[0] == HDFConstants.HDF_NONE) {
                    chunkSize = null;
                    storage_layout = "NONE";
                }
                else {
                    chunkSize = new long[rank];
                    for (int i=0; i<rank; i++) {
                        chunkSize[i] = chunkInfo.chunk_lengths[i];
                    }
                    storage_layout = "CHUNKED: " + String.valueOf(chunkSize[0]);
                    for (int i = 1; i < rank; i++) {
                        storage_layout += " X " + chunkSize[i];
                    }
                }
            }
            catch (Exception ex) {
                log.debug("init(): get chunk information failure: ", ex);
            }

        }
        catch (HDFException ex) {
            log.debug("init(): failure: ", ex);
        }
        finally {
            close(id);
        }

        isUnsigned = H4Datatype.isUnsigned(datatypeID);

        if (idims == null) {
            log.debug("init(): idims is null");
            log.trace("init(): finish");
            return;
        }

        ncomp = grInfo[0];
        isTrueColor = (ncomp >= 3);
        interlace = grInfo[2];
        rank = 2; // support only two dimensional raster image

        // data in HDF4 GR image is arranged as dim[0]=width, dim[1]=height.
        // other image data is arranged as dim[0]=height, dim[1]=width.
        selectedIndex[0] = 1;
        selectedIndex[1] = 0;

        dims = new long[rank];
        startDims = new long[rank];
        selectedDims = new long[rank];
        for (int i=0; i<rank; i++) {
            startDims[i] = 0;
            selectedDims[i] = idims[i];
            dims[i] = idims[i];
        }

        log.trace("init(): finish");
    }

    // ***** need to implement from ScalarDS *****
    @Override
    public byte[][] getPalette()
    {
        log.trace("getPalette(): start");

        if (palette != null) {
            log.trace("getPalette(): palette != null");
            log.trace("getPalette(): finish");
            return palette;
        }

        long id = open();
        if (id < 0) {
            log.debug("getPalette(): Invalid ID");
            log.trace("getPalette(): finish");
            return null;
        }

        // get palette info.
        long lutid  = -1;
        int[] lutInfo = new int[4]; //ncomp, datatype, interlace, num_entries
        try {
            // find the first palette.
            // Todo: get all the palettes
            lutid = HDFLibrary.GRgetlutid(id, 0);
            HDFLibrary.GRgetlutinfo(lutid, lutInfo);
        }
        catch (HDFException ex) {
            log.debug("getPalette(): failure: ", ex);
            close(id);
            log.trace("getPalette(): finish");
            return null;
        }

        // check if there is palette data. HDFLibrary.GRgetlutinfo() sometimes
        // return true even if there is no palette data, and check if it is a
        // RGB with 256 colors
        if ((lutInfo[0] != 3) || (lutInfo[2] < 0) | (lutInfo[3] != 256)) {
            close(id);
            log.debug("getPalette(): no palette data");
            log.trace("getPalette(): finish");
            return null;
        }

        // read palette data
        boolean b = false;
        byte[] pal = new byte[3*256];
        try
        {
            HDFLibrary.GRreqlutil(id, lutInfo[2]);
            b = HDFLibrary.GRreadlut(lutid, pal);
        }
        catch (HDFException ex) {
            log.debug("getPalette(): failure: ", ex);
            b = false;
        }

        if (!b) {
            close(id);
            log.debug("getPalette(): no palette data");
            log.trace("getPalette(): finish");
            return null;
        }

        palette = new byte[3][256];
        if (lutInfo[2] == HDFConstants.MFGR_INTERLACE_PIXEL) {
            // color conponents are arranged in RGB, RGB, RGB, ...
            for (int i=0; i<256; i++) {
                palette[0][i] = pal[i*3];
                palette[1][i] = pal[i*3+1];
                palette[2][i] = pal[i*3+2];
            }
        }
        else {
            for (int i=0; i<256; i++) {
                palette[0][i] = pal[i];
                palette[1][i] = pal[256+i];
                palette[2][i] = pal[512+i];
            }
        }

        close(id);

        log.trace("getPalette(): finish");
        return palette;
    }

    /**
     * Returns the number of components of this image data.
     *
     * @return the number of components
     */
    public int getComponentCount()
    {
        return ncomp;
    }

    /**
     * Creates a new image.
     *
     * @param name the name of the dataset to create.
     * @param pgroup the parent group of the new dataset.
     * @param type the datatype of the dataset.
     * @param dims the dimension size of the dataset.
     * @param maxdims the max dimension size of the dataset.
     * @param chunks the chunk size of the dataset.
     * @param gzip the level of the gzip compression.
     * @param ncomp number of components of the image data.
     * @param interlace the interlace mode.
     * @param data the array of data values.
     *
     * @return the new image if successful. Otherwise returns null.
     *
     * @throws Exception if the image can not be created
     */
    public static H4GRImage create(
        String name,
        Group pgroup,
        Datatype type,
        long[] dims,
        long[] maxdims,
        long[] chunks,
        int gzip,
        int ncomp,
        int interlace,
        Object data) throws Exception
    {
        log.trace("create(): start: name={} parentGroup={} type={} gzip={} ncomp={} interlace={}", name, pgroup, type, gzip, ncomp, interlace);

        H4GRImage dataset = null;
        if ((name == null) ||
            (pgroup == null) ||
            (dims == null) ||
            ((gzip>0) && (chunks==null))) {
            log.debug("create(): one or more parameters are null");
            log.trace("create(): finish");
            return null;
        }

        H4File file = (H4File)pgroup.getFileFormat();
        if (file == null) {
            log.debug("create(): Parent group FileFormat is null");
            log.trace("create(): finish");
            return null;
        }

        String path = HObject.separator;
        if (!pgroup.isRoot()) {
            path = pgroup.getPath()+pgroup.getName()+HObject.separator;
        }
        if (interlace == ScalarDS.INTERLACE_PLANE) {
            interlace = HDFConstants.MFGR_INTERLACE_COMPONENT;
        }
        else {
            interlace = HDFConstants.MFGR_INTERLACE_PIXEL;
        }

        int rank = 2;
        int idims[] = new int[rank];
        int imaxdims[] = new int[rank];
        int start[] = new int [rank];
        for (int i=0; i<rank; i++) {
            idims[i] = (int)dims[i];
            if (maxdims != null) {
                imaxdims[i] = (int)maxdims[i];
            }
            else {
                imaxdims[i] = idims[i];
            }
            start[i] = 0;
        }

        int ichunks[] = null;
        if (chunks != null) {
            ichunks = new int[rank];
            for (int i=0; i<rank; i++) {
                ichunks[i] = (int)chunks[i];
            }
        }

        long grid = -1;
        long vgid = -1;
        long gid = (file).getGRAccessID();
        long tid = type.toNative();

        if(tid >= 0) {
            try {
                grid = HDFLibrary.GRcreate(gid, name, ncomp, tid, interlace, idims);
            }
            catch (Exception ex) {
                log.debug("create(): failure: ", ex);
                log.trace("create(): finish");
                throw (ex);
            }
        }

        if (grid < 0) {
            log.debug("create(): Invalid GR ID");
            log.trace("create(): finish");
            throw (new HDFException("Unable to create the new dataset."));
        }

        if ((grid > 0) && (data != null)) {
            HDFLibrary.GRwriteimage(grid, start, null, idims, data);
        }

        if (chunks != null) {
            // set chunk
            HDFChunkInfo chunkInfo = new HDFChunkInfo(ichunks);
            HDFLibrary.GRsetchunk(grid, chunkInfo, HDFConstants.HDF_CHUNK);
        }

        if (gzip > 0) {
            // set compression
            int compType = HDFConstants.COMP_CODE_DEFLATE;
            HDFDeflateCompInfo compInfo = new HDFDeflateCompInfo();
            compInfo.level = gzip;
            HDFLibrary.GRsetcompress(grid, compType, compInfo);
        }

        int ref = HDFLibrary.GRidtoref(grid);

        if (!pgroup.isRoot()) {
            // add the dataset to the parent group
            vgid = pgroup.open();
            if (vgid < 0) {
                if (grid > 0) {
                    HDFLibrary.GRendaccess(grid);
                }
                log.debug("create(): Invalid VG ID");
                log.trace("create(): finish");
                throw (new HDFException("Unable to open the parent group."));
            }

            HDFLibrary.Vaddtagref(vgid, HDFConstants.DFTAG_RI, ref);

            pgroup.close(vgid);
        }

        try {
            if (grid > 0) {
                HDFLibrary.GRendaccess(grid);
            }
        }
        catch (Exception ex) {
            log.debug("create(): GRendaccess failure: ", ex);
        }

        long[] oid = {HDFConstants.DFTAG_NDG, ref};
        dataset = new H4GRImage(file, name, path, oid);

        if (dataset != null) {
            pgroup.addToMemberList(dataset);
        }

        log.trace("create(): finish");
        return dataset;
    }

    /**
     * copy attributes from one GR image to another GR image
     */
    private void copyAttribute(long srcdid, long dstdid, int numberOfAttributes)
    {
        log.trace("copyAttribute(): start: srcdid={} dstdid={} numAttributes={}", srcdid, dstdid, numberOfAttributes);

        if (numberOfAttributes <= 0) {
            log.debug("copyAttribute(): numberOfAttributes={}", numberOfAttributes);
            log.trace("copyAttribute(): finish");
            return;
        }

        try {
            boolean b = false;
            String[] attrName = new String[1];
            int[] attrInfo = {0, 0};
            for (int i=0; i<numberOfAttributes; i++) {
                attrName[0] = "";
                try {
                    b = HDFLibrary.GRattrinfo(srcdid, i, attrName, attrInfo);
                }
                catch (HDFException ex) {
                    log.trace("copyAttribute(): attribute[{}] GRattrinfo failure: ", i, ex);
                    b = false;
                }

                if (!b) {
                    continue;
                }

                // read attribute data from source dataset
                byte[] attrBuff = new byte[attrInfo[1] * HDFLibrary.DFKNTsize(attrInfo[0])];
                try {
                    HDFLibrary.GRgetattr(srcdid, i, attrBuff);
                }
                catch (Exception ex) {
                    log.trace("copyAttribute(): attribute[{}] GRgetattr failure: ", i, ex);
                    attrBuff = null;
                }

                if (attrBuff == null) {
                    log.debug("copyAttribute(): attrBuff[{}] is null", i);
                    log.trace("copyAttribute(): continue");
                    continue;
                }

                // attach attribute to the destination dataset
                HDFLibrary.GRsetattr(dstdid, attrName[0], attrInfo[0], attrInfo[1], attrBuff);
            } // for (int i=0; i<numberOfAttributes; i++)
        }
        catch (Exception ex) {
            log.debug("copyAttribute(): failure: ", ex);
        }
    }

    //Implementing DataFormat
    @SuppressWarnings("rawtypes")
    public List getMetadata(int... attrPropList) throws Exception {
        throw new UnsupportedOperationException("getMetadata(int... attrPropList) is not supported");
    }
}
