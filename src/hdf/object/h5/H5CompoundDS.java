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

package hdf.object.h5;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5DataFiltersException;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.Utils;

/**
 * The H5CompoundDS class defines an HDF5 dataset of compound datatypes.
 * <p>
 * An HDF5 dataset is an object composed of a collection of data elements, or raw data, and metadata
 * that stores a description of the data elements, data layout, and all other information necessary
 * to write, read, and interpret the stored data.
 * <p>
 * A HDF5 compound datatype is similar to a struct in C or a common block in Fortran: it is a
 * collection of one or more atomic types or small arrays of such types. Each member of a compound
 * type has a name which is unique within that type, and a byte offset that determines the first
 * byte (smallest byte address) of that member in a compound datum.
 * <p>
 * For more information on HDF5 datasets and datatypes, read the <a href=
 * "https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5
 * User's Guide</a>.
 * <p>
 * There are two basic types of compound datasets: simple compound data and nested compound data.
 * Members of a simple compound dataset have atomic datatypes. Members of a nested compound dataset
 * are compound or array of compound data.
 * <p>
 * Since Java does not understand C structures, we cannot directly read/write compound data values
 * as in the following C example.
 *
 * <pre>
 * typedef struct s1_t {
 *         int    a;
 *         float  b;
 *         double c;
 *         } s1_t;
 *     s1_t       s1[LENGTH];
 *     ...
 *     H5Dwrite(..., s1);
 *     H5Dread(..., s1);
 * </pre>
 *
 * Values of compound data fields are stored in java.util.Vector object. We read and write compound
 * data by fields instead of compound structure. As for the example above, the java.util.Vector
 * object has three elements: int[LENGTH], float[LENGTH] and double[LENGTH]. Since Java understands
 * the primitive datatypes of int, float and double, we will be able to read/write the compound data
 * by field.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5CompoundDS extends CompoundDS {
    private static final long serialVersionUID = -5968625125574032736L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5CompoundDS.class);

    /**
     * The list of attributes of this data object. Members of the list are instance of Attribute.
     */
    private List<Attribute> attributeList;

    private int nAttributes = -1;

    private H5O_info_t objInfo;

    /**
     * A list of names of all fields including nested fields.
     * <p>
     * The nested names are separated by CompoundDS.SEPARATOR. For example, if compound dataset "A" has
     * the following nested structure,
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
     * The flatNameList of compound dataset "A" will be {m01, m02, nest1[m11, nest1[m12,
     * nest1[nest2[m21, nest1[nest2[m22}
     *
     */
    private List<String> flatNameList;

    /**
     * A list of datatypes of all fields including nested fields.
     */
    private List<Datatype> flatTypeList;

    /** flag to indicate if the dataset is an external dataset */
    private boolean isExternal = false;

    /** flag to indicate if the dataset is a virtual dataset */
    private boolean isVirtual = false;
    private List<String> virtualNameList;

    /*
     * Enum to indicate the type of I/O to perform inside of the common I/O
     * function.
     */
    protected static enum IO_TYPE {
        READ, WRITE
    };

    /**
     * Constructs an instance of a HDF5 compound dataset with given file, dataset name and path.
     * <p>
     * The dataset object represents an existing dataset in the file. For example, new
     * H5CompoundDS(file, "dset1", "/g0/") constructs a dataset object that corresponds to the
     * dataset,"dset1", at group "/g0/".
     * <p>
     * This object is usually constructed at FileFormat.open(), which loads the file structure and
     * object information into memory. It is rarely used elsewhere.
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     */
    public H5CompoundDS(FileFormat theFile, String theName, String thePath) {
        this(theFile, theName, thePath, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #H5CompoundDS(FileFormat, String, String)}
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     * @param oid
     *            the oid of the data object.
     */
    @Deprecated
    public H5CompoundDS(FileFormat theFile, String theName, String thePath, long[] oid) {
        super(theFile, theName, thePath, oid);
        objInfo = new H5O_info_t(-1L, -1L, 0, 0, -1L, 0L, 0L, 0L, 0L, null, null, null);

        if ((oid == null) && (theFile != null)) {
            // retrieve the object ID
            try {
                byte[] refBuf = H5.H5Rcreate(theFile.getFID(), this.getFullName(), HDF5Constants.H5R_OBJECT, -1);
                this.oid = new long[1];
                this.oid[0] = HDFNativeData.byteToLong(refBuf, 0);
            }
            catch (Exception ex) {
                log.debug("constructor ID {} for {} failed H5Rcreate", theFile.getFID(), this.getFullName());
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#open()
     */
    @Override
    public long open() {
        log.trace("open(): start");

        long did = -1;

        try {
            did = H5.H5Dopen(getFID(), getPath() + getName(), HDF5Constants.H5P_DEFAULT);
            log.trace("open(): did={}", did);
        }
        catch (HDF5Exception ex) {
            log.debug("open(): Failed to open dataset {}: ", getPath() + getName(), ex);
            did = -1;
        }

        log.trace("open(): finish");
        return did;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#close(int)
     */
    @Override
    public void close(long did) {
        log.trace("close(): start");

        if (did >= 0) {
            try {
                H5.H5Fflush(did, HDF5Constants.H5F_SCOPE_LOCAL);
            }
            catch (Exception ex) {
                log.debug("close(): H5Fflush(did {}) failure: ", did, ex);
            }
            try {
                H5.H5Dclose(did);
            }
            catch (HDF5Exception ex) {
                log.debug("close(): H5Dclose(did {}) failure: ", did, ex);
            }
        }

        log.trace("close(): finish");
    }

    /**
     * Retrieves datatype and dataspace information from file and sets the dataset
     * in memory.
     * <p>
     * The init() is designed to support lazy operation in a dataset object. When a
     * data object is retrieved from file, the datatype, dataspace and raw data are
     * not loaded into memory. When it is asked to read the raw data from file,
     * init() is first called to get the datatype and dataspace information, then
     * load the raw data from file.
     * <p>
     * init() is also used to reset the selection of a dataset (start, stride and
     * count) to the default, which is the entire dataset for 1D or 2D datasets. In
     * the following example, init() at step 1) retrieves datatype and dataspace
     * information from file. getData() at step 3) reads only one data point. init()
     * at step 4) resets the selection to the whole dataset. getData() at step 4)
     * reads the values of whole dataset into memory.
     *
     * <pre>
     * dset = (Dataset) file.get(NAME_DATASET);
     *
     * // 1) get datatype and dataspace information from file
     * dset.init();
     * rank = dset.getRank(); // rank = 2, a 2D dataset
     * count = dset.getSelectedDims();
     * start = dset.getStartDims();
     * dims = dset.getDims();
     *
     * // 2) select only one data point
     * for (int i = 0; i &lt; rank; i++) {
     *     start[0] = 0;
     *     count[i] = 1;
     * }
     *
     * // 3) read one data point
     * data = dset.getData();
     *
     * // 4) reset selection to the whole dataset
     * dset.init();
     *
     * // 5) clean the memory data buffer
     * dset.clearData();
     *
     * // 6) Read the whole dataset
     * data = dset.getData();
     * </pre>
     */
    @Override
    public void init() {
        log.trace("init(): start");

        if (inited) {
            resetSelection();
            log.trace("init(): Dataset already initialized");
            log.trace("init(): finish");
            return; // already called. Initialize only once
        }

        long did = -1;
        long tid = -1;
        long sid = -1;
        flatNameList = new Vector<>();
        flatTypeList = new Vector<>();

        did = open();
        if (did >= 0) {
            // check if it is an external or virtual dataset
            long pid = -1;
            try {
                pid = H5.H5Dget_create_plist(did);
                try {
                    int nfiles = H5.H5Pget_external_count(pid);
                    isExternal = (nfiles > 0);
                    int layoutType = H5.H5Pget_layout(pid);
                    if (isVirtual = (layoutType == HDF5Constants.H5D_VIRTUAL)) {
                        try {
                            long vmaps = H5.H5Pget_virtual_count(pid);
                            if (vmaps > 0) {
                                virtualNameList = new Vector<>();
                                for (long next = 0; next < vmaps; next++) {
                                    try {
                                        String fname = H5.H5Pget_virtual_filename(pid, next);
                                        virtualNameList.add(fname);
                                        log.trace("init(): virtualNameList[{}]={}", next, fname);
                                    }
                                    catch (Exception err) {
                                        log.trace("init(): vds[{}] continue", next);
                                    }
                                }
                            }
                        }
                        catch (Exception err) {
                            log.debug("init(): vds count error: ", err);
                        }
                    }
                    log.trace("init(): pid={} nfiles={} isExternal={} isVirtual={}", pid, nfiles, isExternal, isVirtual);
                }
                catch (Exception ex) {
                    log.debug("init(): check if it is an external or virtual dataset:", ex);
                }
            }
            catch (Exception ex) {
                log.debug("init(): H5Dget_create_plist() failure: ", ex);
            }
            finally {
                try {
                    H5.H5Pclose(pid);
                }
                catch (Exception ex) {
                    log.debug("init(): H5Pclose(pid {}) failure: ", pid, ex);
                }
            }

            try {
                sid = H5.H5Dget_space(did);
                rank = H5.H5Sget_simple_extent_ndims(sid);
                tid = H5.H5Dget_type(did);
                log.trace("init(): tid={} sid={} rank={}", tid, sid, rank);

                if (rank == 0) {
                    // a scalar data point
                    rank = 1;
                    dims = new long[1];
                    dims[0] = 1;
                    log.trace("init(): rank is a scalar data point");
                }
                else {
                    dims = new long[rank];
                    maxDims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, maxDims);
                    log.trace("init(): rank={}, dims={}, maxDims={}", rank, dims, maxDims);
                }

                startDims = new long[rank];
                selectedDims = new long[rank];

                try {
                    datatype = new H5Datatype(tid);

                    log.trace("init(): tid={} has isText={} : isVLEN={} : isEnum={} : isUnsigned={} : isRegRef={}", tid,
                            datatype.isText(), datatype.isVLEN(), ((H5Datatype) datatype).isEnum(), datatype.isUnsigned(), ((H5Datatype) datatype).isRegRef());

                    H5Datatype.extractCompoundInfo((H5Datatype) datatype, "", flatNameList, flatTypeList);
                }
                catch (Exception ex) {
                    log.debug("init(): failed to create datatype for dataset: ", ex);
                    datatype = null;
                }

                // initialize member information
                numberOfMembers = flatNameList.size();
                log.trace("init(): numberOfMembers={}", numberOfMembers);

                memberNames = new String[numberOfMembers];
                memberTypes = new Datatype[numberOfMembers];
                memberOrders = new int[numberOfMembers];
                isMemberSelected = new boolean[numberOfMembers];
                memberDims = new Object[numberOfMembers];

                for (int i = 0; i < numberOfMembers; i++) {
                    isMemberSelected[i] = true;
                    memberOrders[i] = 1;
                    memberDims[i] = null;

                    try {
                        memberTypes[i] = flatTypeList.get(i);
                        log.trace("init()[{}]: memberTypes[{}]={}", i, i, memberTypes[i].getDescription());

                        if (memberTypes[i].isArray()) {
                            long mdim[] = memberTypes[i].getArrayDims();
                            int idim[] = new int[mdim.length];
                            int arrayNpoints = 1;

                            for (int j = 0; j < idim.length; j++) {
                                idim[j] = (int) mdim[j];
                                arrayNpoints *= idim[j];
                            }

                            memberDims[i] = idim;
                            memberOrders[i] = arrayNpoints;
                        }
                    }
                    catch (Exception ex) {
                        log.debug("init()[{}]: memberTypes[{}] get failure: ", i, i, ex);
                        memberTypes[i] = null;
                    }

                    try {
                        memberNames[i] = flatNameList.get(i);
                        log.trace("init()[{}]: memberNames[{}]={}", i, i, memberNames[i]);
                    }
                    catch (Exception ex) {
                        log.debug("init()[{}]: memberNames[{}] get failure: ", i, i, ex);
                        memberNames[i] = "null";
                    }
                } //  (int i=0; i<numberOfMembers; i++)

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
                if (datatype != null)
                    datatype.close(tid);

                try {
                    H5.H5Sclose(sid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("init(): H5Sclose(sid {}) failure: ", sid, ex2);
                }
            }

            log.trace("init(): close dataset");
            close(did);
        }
        else {
            log.debug("init(): failed to open dataset");
        }

        resetSelection();
        log.trace("init(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#hasAttribute()
     */
    @Override
    public boolean hasAttribute() {
        objInfo.num_attrs = nAttributes;

        if (objInfo.num_attrs < 0) {
            long did = open();
            if (did >= 0) {
                try {
                    objInfo = H5.H5Oget_info(did);
                    nAttributes = (int) objInfo.num_attrs;
                }
                catch (Exception ex) {
                    objInfo.num_attrs = 0;
                    log.debug("hasAttribute(): get object info failure: ", ex);
                }
                close(did);
            }
            else {
                log.debug("hasAttribute(): could not open dataset");
            }
        }

        log.trace("hasAttribute(): nAttributes={}", objInfo.num_attrs);
        return (objInfo.num_attrs > 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getDatatype()
     */
    @Override
    public Datatype getDatatype() {
        log.trace("getDatatype(): start");

        if (!inited)
            init();

        if (datatype == null) {
            long did = -1;
            long tid = -1;

            log.trace("getDatatype(): datatype == null");

            did = open();
            if (did >= 0) {
                try {
                    tid = H5.H5Dget_type(did);
                    datatype = new H5Datatype(tid);
                }
                catch (Exception ex) {
                    log.debug("getDatatype(): ", ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tid);
                    }
                    catch (HDF5Exception ex) {
                        log.debug("getDatatype(): H5Tclose(tid {}) failure: ", tid, ex);
                    }
                    try {
                        H5.H5Dclose(did);
                    }
                    catch (HDF5Exception ex) {
                        log.debug("getDatatype(): H5Dclose(did {}) failure: ", did, ex);
                    }
                }
            }
        }

        if (isExternal) {
            String pdir = this.getFileFormat().getAbsoluteFile().getParent();

            if (pdir == null) {
                pdir = ".";
            }
            System.setProperty("user.dir", pdir);
            log.trace("getDatatype(): External dataset: user.dir={}", pdir);
        }

        log.trace("getDatatype(): finish");
        return datatype;
    }

    @Override
    public Object getFillValue() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#clear()
     */
    @Override
    public void clear() {
        super.clear();

        if (attributeList != null) {
            ((Vector<Attribute>) attributeList).setSize(0);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#readBytes()
     */
    @Override
    public byte[] readBytes() throws HDF5Exception {
        log.trace("readBytes(): start");

        byte[] theData = null;

        if (!isInited())
            init();

        long did = open();
        if (did >= 0) {
            long fspace = -1;
            long mspace = -1;
            long tid = -1;

            try {
                long[] lsize = { 1 };
                for (int j = 0; j < selectedDims.length; j++) {
                    lsize[0] *= selectedDims[j];
                }

                fspace = H5.H5Dget_space(did);
                mspace = H5.H5Screate_simple(rank, selectedDims, null);

                // set the rectangle selection
                // HDF5 bug: for scalar dataset, H5Sselect_hyperslab gives core dump
                if (rank * dims[0] > 1) {
                    H5.H5Sselect_hyperslab(fspace, HDF5Constants.H5S_SELECT_SET, startDims, selectedStride,
                            selectedDims, null); // set block to 1
                }

                tid = H5.H5Dget_type(did);
                long size = H5.H5Tget_size(tid) * lsize[0];
                log.trace("readBytes(): size = {}", size);

                if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE) throw new Exception("Invalid int size");

                theData = new byte[(int) size];

                log.trace("readBytes(): H5Dread: did={} tid={} fspace={} mspace={}", did, tid, fspace, mspace);
                H5.H5Dread(did, tid, mspace, fspace, HDF5Constants.H5P_DEFAULT, theData);
            }
            catch (Exception ex) {
                log.debug("readBytes(): failed to read data: ", ex);
            }
            finally {
                try {
                    H5.H5Sclose(fspace);
                }
                catch (Exception ex2) {
                    log.debug("readBytes(): H5Sclose(fspace {}) failure: ", fspace, ex2);
                }
                try {
                    H5.H5Sclose(mspace);
                }
                catch (Exception ex2) {
                    log.debug("readBytes(): H5Sclose(mspace {}) failure: ", mspace, ex2);
                }
                try {
                    H5.H5Tclose(tid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("readBytes(): H5Tclose(tid {}) failure: ", tid, ex2);
                }
                close(did);
            }
        }

        log.trace("readBytes(): finish");
        return theData;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#read()
     */
    @Override
    public Object read() throws Exception {
        log.trace("read(): start");

        Object readData = null;

        if (!isInited())
            init();

        try {
            readData = compoundDatasetCommonIO(IO_TYPE.READ, null);
        }
        catch (Exception ex) {
            log.debug("read(): failed to read compound dataset: ", ex);
            throw new Exception("failed to read compound dataset: " + ex.getMessage(), ex);
        }

        log.trace("read(): finish");

        return readData;
    }

    /**
     * Writes the given data buffer into this dataset in a file.
     * <p>
     * The data buffer is a vector that contains the data values of compound fields. The data is written
     * into file field by field.
     *
     * @param buf
     *            The vector that contains the data values of compound fields.
     *
     * @throws Exception
     *             If there is an error at the HDF5 library level.
     */
    @Override
    public void write(Object buf) throws Exception {
        log.trace("write(): start");

        if (this.getFileFormat().isReadOnly())
            throw new Exception("cannot write to compound dataset in file opened as read-only");

        if (!isInited())
            init();

        try {
            compoundDatasetCommonIO(IO_TYPE.WRITE, buf);
        }
        catch (Exception ex) {
            log.debug("write(): failed to write compound dataset: ", ex);
            throw new Exception("failed to write compound dataset: " + ex.getMessage(), ex);
        }

        log.trace("write(): finish");
    }

    private Object compoundDatasetCommonIO(IO_TYPE ioType, Object writeBuf) throws Exception {
        log.trace("compoundDatasetCommonIO(): start");

        H5Datatype dsDatatype = (H5Datatype) getDatatype();
        Object data = null;

        if (numberOfMembers <= 0) {
            log.debug("compoundDatasetCommonIO(): Dataset contains no members");
            log.trace("compoundDatasetCommonIO(): exit");
            throw new Exception("dataset contains no members");
        }

        /*
         * I/O type-specific pre-initialization.
         */
        if (ioType == IO_TYPE.WRITE) {
            if ((writeBuf == null) || !(writeBuf instanceof List)) {
                log.debug("compoundDatasetCommonIO(): writeBuf is null or invalid");
                log.trace("compoundDatasetCommonIO(): exit");
                throw new Exception("write buffer is null or invalid");
            }

            /*
             * Check for any unsupported datatypes and fail early before
             * attempting to write to the dataset.
             */
            if (dsDatatype.isArray() && dsDatatype.getDatatypeBase().isCompound()) {
                log.debug("compoundDatasetCommonIO(): cannot write dataset of type ARRAY of COMPOUND");
                log.trace("compoundDatasetCommonIO(): finish");
                throw new HDF5Exception("Unsupported dataset of type ARRAY of COMPOUND");
            }

            if (dsDatatype.isVLEN() && dsDatatype.getDatatypeBase().isCompound()) {
                log.debug("compoundDatasetCommonIO(): cannot write dataset of type VLEN of COMPOUND");
                log.trace("compoundDatasetCommonIO(): finish");
                throw new HDF5Exception("Unsupported dataset of type VLEN of COMPOUND");
            }
        }

        log.trace("compoundDatasetCommonIO(): open dataset");

        long did = open();
        if (did >= 0) {
            long[] spaceIDs = { -1, -1 }; // spaceIDs[0]=mspace, spaceIDs[1]=fspace

            try {
                /*
                 * NOTE: this call sets up a hyperslab selection in the file according to the
                 * current selection in the dataset object.
                 */
                long totalSelectedSpacePoints = H5Utils.getTotalSelectedSpacePoints(did, dims, startDims,
                        selectedStride, selectedDims, spaceIDs);

                data = compoundTypeIO(ioType, did, spaceIDs, (int) totalSelectedSpacePoints, dsDatatype, writeBuf, new int[]{0});
            }
            finally {
                if (HDF5Constants.H5S_ALL != spaceIDs[0]) {
                    try {
                        H5.H5Sclose(spaceIDs[0]);
                    }
                    catch (Exception ex) {
                        log.debug("compoundDatasetCommonIO(): H5Sclose(spaceIDs[0] {}) failure: ", spaceIDs[0], ex);
                    }
                }

                if (HDF5Constants.H5S_ALL != spaceIDs[1]) {
                    try {
                        H5.H5Sclose(spaceIDs[1]);
                    }
                    catch (Exception ex) {
                        log.debug("compoundDatasetCommonIO(): H5Sclose(spaceIDs[1] {}) failure: ", spaceIDs[1], ex);
                    }
                }

                close(did);
            }
        }
        else
            log.debug("compoundDatasetCommonIO(): failed to open dataset");

        log.trace("compoundDatasetCommonIO(): finish");

        return data;
    }

    /*
     * Private recursive routine to read/write an entire compound datatype field by
     * field. This routine is called recursively for ARRAY of COMPOUND and VLEN of
     * COMPOUND datatypes.
     *
     * NOTE: the globalMemberIndex hack is ugly, but we need to keep track of a
     * running counter so that we can index properly into the flattened name list
     * generated from H5Datatype.extractCompoundInfo() at dataset init time.
     */
    private Object compoundTypeIO(IO_TYPE ioType, long did, long[] spaceIDs, int nSelPoints, final H5Datatype cmpdType,
            Object writeBuf, int[] globalMemberIndex) {
        log.trace("compoundTypeIO(): start");

        Object theData = null;

        if (cmpdType.isArray()) {
            log.trace("compoundTypeIO(): ARRAY type");

            long[] arrayDims = cmpdType.getArrayDims();
            int arrSize = nSelPoints;
            for (int i = 0; i < arrayDims.length; i++) {
                arrSize *= arrayDims[i];
            }

            theData = compoundTypeIO(ioType, did, spaceIDs, arrSize, (H5Datatype) cmpdType.getDatatypeBase(), writeBuf, globalMemberIndex);
        }
        else if (cmpdType.isVLEN() && !cmpdType.isVarStr()) {
            /*
             * TODO: true variable-length support.
             */
            String[] errVal = new String[nSelPoints];
            String errStr = "*UNSUPPORTED*";

            for (int j = 0; j < nSelPoints; j++)
                errVal[j] = errStr;

            /*
             * Setup a fake data list.
             */
            Datatype baseType = cmpdType.getDatatypeBase();
            while (baseType != null && !baseType.isCompound()) {
                baseType = baseType.getDatatypeBase();
            }

            List<Object> fakeVlenData = (List<Object>) H5Datatype.allocateArray((H5Datatype) baseType, nSelPoints);
            fakeVlenData.add(errVal);

            theData = fakeVlenData;
        }
        else if (cmpdType.isCompound()) {
            List<Object> memberDataList = null;
            List<Datatype> typeList = cmpdType.getCompoundMemberTypes();

            log.trace("compoundTypeIO(): {} {} members:", (ioType == IO_TYPE.READ) ? "read" : "write",
                    typeList.size());

            if (ioType == IO_TYPE.READ) {
                memberDataList = (List<Object>) H5Datatype.allocateArray(cmpdType, nSelPoints);
            }

            try {
                for (int i = 0, writeListIndex = 0; i < typeList.size(); i++) {
                    H5Datatype memberType = null;
                    String memberName = null;
                    Object memberData = null;

                    try {
                        memberType = (H5Datatype) typeList.get(i);
                    }
                    catch (Exception ex) {
                        log.debug("compoundTypeIO(): get member {} failure: ", i, ex);
                        globalMemberIndex[0]++;
                        continue;
                    }

                    /*
                     * Since the type list used here is not a flattened structure, we need to skip
                     * the member selection check for compound types, as otherwise having a single
                     * member not selected would skip the reading/writing for the entire compound
                     * type. The member selection check will be deferred to the recursive compound
                     * read/write below.
                     */
                    if (!memberType.isCompound()) {
                        if (!isMemberSelected[globalMemberIndex[0] % this.getMemberCount()]) {
                            log.debug("compoundTypeIO(): member[{}] is not selected", i);
                            globalMemberIndex[0]++;
                            continue; // the field is not selected
                        }
                    }

                    if (!memberType.isCompound()) {
                        try {
                            memberName = new String(flatNameList.get(globalMemberIndex[0]));
                        }
                        catch (Exception ex) {
                            log.debug("compoundTypeIO(): get member {} name failure: ", i, ex);
                            memberName = "null";
                        }
                    }

                    log.trace("compoundTypeIO(): member[{}]({}) is type {}", i, memberName, memberType.getDescription());

                    if (ioType == IO_TYPE.READ) {
                        try {
                            if (memberType.isCompound())
                                memberData = compoundTypeIO(ioType, did, spaceIDs, nSelPoints, memberType, writeBuf, globalMemberIndex);
                            else if (memberType.isArray() /* || (memberType.isVLEN() && !memberType.isVarStr()) */) {
                                /*
                                 * Recursively detect any nested array/vlen of compound types.
                                 */
                                boolean compoundFound = false;

                                Datatype base = memberType.getDatatypeBase();
                                while (base != null) {
                                    if (base.isCompound())
                                        compoundFound = true;

                                    base = base.getDatatypeBase();
                                }

                                if (compoundFound) {
                                    /*
                                     * Skip the top-level array/vlen type.
                                     */
                                    globalMemberIndex[0]++;

                                    memberData = compoundTypeIO(ioType, did, spaceIDs, nSelPoints, memberType, writeBuf, globalMemberIndex);
                                }
                                else {
                                    memberData = readSingleCompoundMember(did, spaceIDs, nSelPoints, memberType, memberName);
                                    globalMemberIndex[0]++;
                                }
                            }
                            else {
                                memberData = readSingleCompoundMember(did, spaceIDs, nSelPoints, memberType, memberName);
                                globalMemberIndex[0]++;
                            }
                        }
                        catch (Exception ex) {
                            log.debug("compoundTypeIO(): failed to read member {}: ", i, ex);
                            globalMemberIndex[0]++;
                            memberData = null;
                        }

                        if (memberData == null) {
                            String[] errVal = new String[nSelPoints];
                            String errStr = "*ERROR*";

                            for (int j = 0; j < nSelPoints; j++)
                                errVal[j] = errStr;

                            memberData = errVal;
                        }

                        memberDataList.add(memberData);
                    }
                    else {
                        try {
                            /*
                             * TODO: currently doesn't correctly handle non-selected compound members.
                             */
                            memberData = ((List<?>) writeBuf).get(writeListIndex++);
                        }
                        catch (Exception ex) {
                            log.debug("compoundTypeIO(): get member[{}] data failure: ", i, ex);
                            globalMemberIndex[0]++;
                            continue;
                        }

                        if (memberData == null) {
                            log.debug("compoundTypeIO(): member[{}] data is null", i);
                            globalMemberIndex[0]++;
                            continue;
                        }

                        try {
                            if (memberType.isCompound()) {
                                List<?> nestedList = (List<?>) ((List<?>) writeBuf).get(writeListIndex++);
                                compoundTypeIO(ioType, did, spaceIDs, nSelPoints, memberType, nestedList, globalMemberIndex);
                            }
                            else {
                                writeSingleCompoundMember(did, spaceIDs, nSelPoints, memberType, memberName, memberData);
                                globalMemberIndex[0]++;
                            }
                        }
                        catch (Exception ex) {
                            log.debug("compoundTypeIO(): failed to write member[{}]: ", i, ex);
                            globalMemberIndex[0]++;
                        }
                    }
                } //  (i = 0, writeListIndex = 0; i < atomicTypeList.size(); i++)
            }
            catch (Exception ex) {
                log.debug("compoundTypeIO(): failure: ", ex);
                memberDataList = null;
            }

            theData = memberDataList;
        }

        log.trace("compoundTypeIO(): finish");

        return theData;
    }

    /*
     * Private routine to read a single field of a compound datatype by creating a
     * compound datatype and inserting the single field into that datatype.
     */
    private Object readSingleCompoundMember(long dsetID, long[] spaceIDs, int nSelPoints, final H5Datatype memberType,
            String memberName) throws Exception {
        log.trace("readSingleCompoundMember(): start");

        H5Datatype dsDatatype = (H5Datatype) this.getDatatype();
        Object memberData = null;

        try {
            memberData = H5Datatype.allocateArray(memberType, nSelPoints);
            log.trace("readSingleCompoundMember(): allocateArray {} points ", nSelPoints);
        }
        catch (OutOfMemoryError err) {
            memberData = null;
            throw new Exception("Out of memory");
        }
        catch (Exception ex) {
            log.debug("readSingleCompoundMember(): ", ex);
            memberData = null;
        }

        if (memberData != null) {
            /*
             * Create a compound datatype containing just a single field (the one which we
             * want to read).
             */
            long compTid = -1;
            try {
                compTid = dsDatatype.createCompoundFieldType(memberName);
            }
            catch (HDF5Exception ex) {
                log.debug("readSingleCompoundMember(): unable to create compound field type for member of type {}: ",
                        memberType.getDescription(), ex);
                memberData = null;
            }

            /*
             * Actually read the data for this member now that everything has been setup.
             */
            try {
                if (memberType.isVLEN() || (memberType.isArray() && memberType.getDatatypeBase().isVLEN())) {
                    log.trace("readSingleCompoundMember(): H5DreadVL did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                            dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                            (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                    H5.H5DreadVL(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, (Object[]) memberData);
                }
                else {
                    log.trace("readSingleCompoundMember(): H5Dread did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                            dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                            (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                    H5.H5Dread(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, memberData);
                }
            }
            catch (HDF5DataFiltersException exfltr) {
                log.debug("readSingleCompoundMember(): read failure: ", exfltr);
                log.trace("readSingleCompoundMember(): exit");
                throw new Exception("Filter not available exception: " + exfltr.getMessage(), exfltr);
            }
            catch (Exception ex) {
                log.debug("readSingleCompoundMember(): read failure: ", ex);
                log.trace("readSingleCompoundMember(): exit");
                throw new Exception("failed to read compound member: " + ex.getMessage(), ex);
            }
            finally {
                dsDatatype.close(compTid);
            }

            /*
             * Perform any necessary data conversions.
             */
            if (memberType.isUnsigned()) {
                log.trace("readSingleCompoundMember(): converting from unsigned C-type integers");
                memberData = Dataset.convertFromUnsignedC(memberData, null);
            }
            else if (Utils.getJavaObjectRuntimeClass(memberData) == 'B') {
                log.trace("readSingleCompoundMember(): converting byte array member into Object");

                /*
                 * For all other types that get read into memory as a byte[] (such as nested
                 * compounds and arrays of compounds), we must manually convert the byte[] into
                 * something usable.
                 */
                memberData = convertByteMember(memberType, (byte[]) memberData);
            }
        }

        log.trace("readSingleCompoundMember(): finish");

        return memberData;
    }

    /*
     * Private routine to write a single field of a compound datatype by creating a
     * compound datatype and inserting the single field into that datatype.
     */
    private void writeSingleCompoundMember(long dsetID, long[] spaceIDs, int nSelPoints, final H5Datatype memberType,
            String memberName, Object theData) throws Exception {
        log.trace("writeSingleCompoundMember(): start");

        H5Datatype dsDatatype = (H5Datatype) this.getDatatype();

        /*
         * Check for any unsupported datatypes before attempting to write this compound
         * member.
         */
        if (memberType.isVLEN() && !memberType.isVarStr()) {
            log.debug("writeSingleCompoundMember(): writing of VL non-strings is not currently supported");
            log.trace("writeSingleCompoundMember(): exit");
            throw new Exception("writing of VL non-strings is not currently supported");
        }

        /*
         * Perform any necessary data conversions before writing the data.
         */
        Object tmpData = theData;
        try {
            if (memberType.isUnsigned()) {
                // Check if we need to convert unsigned integer data from Java-style
                // to C-style integers
                long tsize = memberType.getDatatypeSize();
                String cname = theData.getClass().getName();
                char dname = cname.charAt(cname.lastIndexOf('[') + 1);
                boolean doIntConversion = (((tsize == 1) && (dname == 'S'))
                        || ((tsize == 2) && (dname == 'I')) || ((tsize == 4) && (dname == 'J')));

                if (doIntConversion) {
                    log.trace("writeSingleCompoundMember(): converting integer data to unsigned C-type integers");
                    tmpData = convertToUnsignedC(theData, null);
                }
            }
            else if (memberType.isString() && (Array.get(theData, 0) instanceof String)) {
                log.trace("writeSingleCompoundMember(): converting string array to byte array");
                tmpData = stringToByte((String[]) theData, (int) memberType.getDatatypeSize());
            }
            else if (memberType.isEnum() && (Array.get(theData, 0) instanceof String)) {
                log.trace("writeSingleCompoundMember(): converting enum names to values");
                tmpData = memberType.convertEnumNameToValue((String[]) theData);
            }
        }
        catch (Exception ex) {
            log.debug("writeSingleCompoundMember(): data conversion failure: ", ex);
            tmpData = null;
        }

        if (tmpData == null) {
            log.debug("writeSingleCompoundMember(): data is null");
            log.trace("writeSingleCompoundMember(): finish");
            return;
        }

        /*
         * Create a compound datatype containing just a single field (the one which we
         * want to write).
         */
        long compTid = -1;
        try {
            compTid = dsDatatype.createCompoundFieldType(memberName);
        }
        catch (HDF5Exception ex) {
            log.debug("writeSingleCompoundMember(): unable to create compound field type for member of type {}: ",
                    memberType.getDescription(), ex);
        }

        /*
         * Actually write the data now that everything has been setup.
         */
        try {
            if (memberType.isVarStr()) {
                log.trace("writeSingleCompoundMember(): H5Dwrite_string did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                        dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                        (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                H5.H5Dwrite_string(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, (String[]) tmpData);
            }
            else {
                log.trace("writeSingleCompoundMember(): H5Dwrite did={} compTid={} spaceIDs[0]={} spaceIDs[1]={}",
                        dsetID, compTid, (spaceIDs[0] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[0],
                        (spaceIDs[1] == HDF5Constants.H5P_DEFAULT) ? "H5P_DEFAULT" : spaceIDs[1]);

                // BUG!!! does not write nested compound data and no
                // exception was caught. Need to check if it is a java
                // error or C library error.
                H5.H5Dwrite(dsetID, compTid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, tmpData);
            }
        }
        catch (Exception ex) {
            log.debug("writeSingleCompoundMember(): write failure: ", ex);
            log.trace("writeSingleCompoundMember(): finish");
            throw new Exception("failed to write compound member: " + ex.getMessage(), ex);
        }
        finally {
            dsDatatype.close(compTid);
        }

        log.trace("writeSingleCompoundMember(): finish");
    }

    /*
     * Private routine to convert datatypes that are read in as byte arrays to
     * regular types.
     */
    private Object convertByteMember(final H5Datatype dtype, byte[] byteData) {
        log.trace("convertByteMember(): start");

        Object theObj = null;

        if (dtype.getDatatypeSize() == 1) {
            /*
             * Normal byte[] type, such as an integer datatype of size 1.
             */
            theObj = byteData;
        }
        else if (dtype.isString() && !dtype.isVarStr() && convertByteToString) {
            log.trace("convertByteMember(): converting byte array to string array");

            theObj = byteToString(byteData, (int) dtype.getDatatypeSize());
        }
        else if (dtype.isInteger()) {
            log.trace("convertByteMember(): converting byte array to integer array");

            theObj = HDFNativeData.byteToInt(byteData);
        }
        else if (dtype.isFloat()) {
            log.trace("convertByteMember(): converting byte array to float array");

            theObj = HDFNativeData.byteToFloat(byteData);
        }
        else if (dtype.isRef()) {
            log.trace("convertByteMember(): reference type - converting byte array to long array");

            theObj = HDFNativeData.byteToLong(byteData);
        }
        else if (dtype.isArray()) {
            H5Datatype baseType = (H5Datatype) dtype.getDatatypeBase();

            /*
             * Retrieve the real base datatype in the case of ARRAY of ARRAY datatypes.
             */
            while (baseType.isArray()) baseType = (H5Datatype) baseType.getDatatypeBase();

            /*
             * Optimize for the common cases of Arrays.
             */
            switch (baseType.getDatatypeClass()) {
                case Datatype.CLASS_INTEGER:
                case Datatype.CLASS_FLOAT:
                case Datatype.CLASS_CHAR:
                case Datatype.CLASS_STRING:
                case Datatype.CLASS_BITFIELD:
                case Datatype.CLASS_OPAQUE:
                case Datatype.CLASS_COMPOUND:
                case Datatype.CLASS_REFERENCE:
                case Datatype.CLASS_ENUM:
                case Datatype.CLASS_VLEN:
                case Datatype.CLASS_TIME:
                    theObj = convertByteMember(baseType, byteData);
                    break;

                case Datatype.CLASS_ARRAY:
                {
                    H5Datatype arrayType = (H5Datatype) dtype.getDatatypeBase();

                    long[] arrayDims = dtype.getArrayDims();
                    int arrSize = 1;
                    for (int i = 0; i < arrayDims.length; i++) {
                        arrSize *= arrayDims[i];
                    }

                    theObj = new Object[arrSize];

                    for (int i = 0; i < arrSize; i++) {
                        byte[] indexedBytes = Arrays.copyOfRange(byteData, (int) (i * arrayType.getDatatypeSize()),
                                (int) ((i + 1) * arrayType.getDatatypeSize()));
                        ((Object[]) theObj)[i] = convertByteMember(arrayType, indexedBytes);
                    }

                    break;
                }

                case Datatype.CLASS_NO_CLASS:
                default:
                    log.debug("convertByteMember(): invalid datatype class");
                    theObj = new String("*ERROR*");
            }
        }
        else if (dtype.isCompound()) {
            /*
             * TODO: still valid after reading change?
             */
            theObj = convertCompoundByteMembers(dtype, byteData);
        }
        else {
            theObj = byteData;
        }

        log.trace("convertByteMember(): finish");

        return theObj;
    }

    /**
     * Given an array of bytes representing a compound Datatype, converts each of
     * its members into Objects and returns the results.
     *
     * @param dtype
     *            The compound datatype to convert
     * @param data
     *            The byte array representing the data of the compound Datatype
     * @return The converted types of the bytes
     */
    private Object convertCompoundByteMembers(final H5Datatype dtype, byte[] data) {
        List<Object> theData = null;

        List<Datatype> allSelectedTypes = Arrays.asList(this.getSelectedMemberTypes());
        List<Datatype> localTypes = new ArrayList<>(dtype.getCompoundMemberTypes());
        Iterator<Datatype> localIt = localTypes.iterator();
        while (localIt.hasNext()) {
            Datatype curType = localIt.next();

            if (curType.isCompound())
                continue;

            if (!allSelectedTypes.contains(curType))
                localIt.remove();
        }

        theData = new ArrayList<>(localTypes.size());
        for (int i = 0, index = 0; i < localTypes.size(); i++) {
            Datatype curType = localTypes.get(i);

            if (curType.isCompound())
                theData.add(convertCompoundByteMembers((H5Datatype) curType,
                        Arrays.copyOfRange(data, index, index + (int) curType.getDatatypeSize())));
            else
                theData.add(convertByteMember((H5Datatype) curType,
                        Arrays.copyOfRange(data, index, index + (int) curType.getDatatypeSize())));

            index += curType.getDatatypeSize();
        }

        return theData;
    }

    @Override
    public Object convertFromUnsignedC() {
        throw new UnsupportedOperationException("H5CompoundDS:convertFromUnsignedC Unsupported operation.");
    }

    @Override
    public Object convertToUnsignedC() {
        throw new UnsupportedOperationException("H5CompoundDS:convertToUnsignedC Unsupported operation.");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#getMetadata()
     */
    @Override
    public List<Attribute> getMetadata() throws HDF5Exception {
        return this.getMetadata(fileFormat.getIndexType(null), fileFormat.getIndexOrder(null));
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#getMetadata(int...)
     */
    public List<Attribute> getMetadata(int... attrPropList) throws HDF5Exception {
        log.trace("getMetadata(): start");

        if (!isInited()) {
            init();
            log.trace("getMetadata(): inited");
        }

        try {
            this.linkTargetObjName = H5File.getLinkTargetName(this);
        }
        catch (Exception ex) {
            log.debug("getMetadata(): getLinkTargetName failed: ", ex);
        }

        if (attributeList != null) {
            log.trace("getMetadata(): attributeList != null");
            log.trace("getMetadata(): finish");
            return attributeList;
        }

        long did = -1;
        long pcid = -1;
        long paid = -1;
        int indxType = fileFormat.getIndexType(null);
        int order = fileFormat.getIndexOrder(null);

        // load attributes first
        if (attrPropList.length > 0) {
            indxType = attrPropList[0];
            if (attrPropList.length > 1) {
                order = attrPropList[1];
            }
        }

        attributeList = H5File.getAttribute(this, indxType, order);
        log.trace("getMetadata(): attributeList loaded");

        log.trace("getMetadata(): open dataset");
        did = open();
        if (did >= 0) {
            log.trace("getMetadata(): dataset opened");
            try {
                // get the compression and chunk information
                pcid = H5.H5Dget_create_plist(did);
                paid = H5.H5Dget_access_plist(did);
                long storageSize = H5.H5Dget_storage_size(did);
                int nfilt = H5.H5Pget_nfilters(pcid);
                int layoutType = H5.H5Pget_layout(pcid);

                storageLayout.setLength(0);
                compression.setLength(0);

                if (layoutType == HDF5Constants.H5D_CHUNKED) {
                    chunkSize = new long[rank];
                    H5.H5Pget_chunk(pcid, rank, chunkSize);
                    int n = chunkSize.length;

                    storageLayout.append("CHUNKED: ").append(chunkSize[0]);
                    for (int i = 1; i < n; i++) {
                        storageLayout.append(" X ").append(chunkSize[i]);
                    }

                    if (nfilt > 0) {
                        long nelmts = 1;
                        long uncompSize;
                        long datumSize = getDatatype().getDatatypeSize();
                        if (datumSize < 0) {
                            long tmptid = -1;
                            try {
                                tmptid = H5.H5Dget_type(did);
                                datumSize = H5.H5Tget_size(tmptid);
                            }
                            finally {
                                try {
                                    H5.H5Tclose(tmptid);
                                }
                                catch (Exception ex2) {
                                    log.debug("getMetadata(): H5Tclose(tmptid {}) failure: ", tmptid, ex2);
                                }
                            }
                        }

                        for (int i = 0; i < rank; i++) {
                            nelmts *= dims[i];
                        }
                        uncompSize = nelmts * datumSize;

                        /* compression ratio = uncompressed size / compressed size */

                        if (storageSize != 0) {
                            double ratio = (double) uncompSize / (double) storageSize;
                            DecimalFormat df = new DecimalFormat();
                            df.setMinimumFractionDigits(3);
                            df.setMaximumFractionDigits(3);
                            compression.append(df.format(ratio)).append(":1");
                        }
                    }
                }
                else if (layoutType == HDF5Constants.H5D_COMPACT) {
                    storageLayout.append("COMPACT");
                }
                else if (layoutType == HDF5Constants.H5D_CONTIGUOUS) {
                    storageLayout.append("CONTIGUOUS");
                    if (H5.H5Pget_external_count(pcid) > 0)
                        storageLayout.append(" - EXTERNAL ");
                }
                else if (layoutType == HDF5Constants.H5D_VIRTUAL) {
                    storageLayout.append("VIRTUAL - ");
                    try {
                        long vmaps = H5.H5Pget_virtual_count(pcid);
                        try {
                            int virtView = H5.H5Pget_virtual_view(paid);
                            long virtGap = H5.H5Pget_virtual_printf_gap(paid);
                            if (virtView == HDF5Constants.H5D_VDS_FIRST_MISSING)
                                storageLayout.append("First Missing");
                            else
                                storageLayout.append("Last Available");
                            storageLayout.append("\nGAP : ").append(virtGap);
                        }
                        catch (Exception err) {
                            log.debug("getMetadata(): vds error: ", err);
                            storageLayout.append("ERROR");
                        }
                        storageLayout.append("\nMAPS : ").append(vmaps);
                        if (vmaps > 0) {
                            for (long next = 0; next < vmaps; next++) {
                                try {
                                    H5.H5Pget_virtual_vspace(pcid, next);
                                    H5.H5Pget_virtual_srcspace(pcid, next);
                                    String fname = H5.H5Pget_virtual_filename(pcid, next);
                                    String dsetname = H5.H5Pget_virtual_dsetname(pcid, next);
                                    storageLayout.append("\n").append(fname).append(" : ").append(dsetname);
                                }
                                catch (Exception err) {
                                    log.debug("getMetadata(): vds space[{}] error: ", next, err);
                                    log.trace("getMetadata(): vds[{}] continue", next);
                                    storageLayout.append("ERROR");
                                }
                            }
                        }
                    }
                    catch (Exception err) {
                        log.debug("getMetadata(): vds count error: ", err);
                        storageLayout.append("ERROR");
                    }
                }
                else {
                    chunkSize = null;
                    storageLayout.append("NONE");
                }

                int[] flags = { 0, 0 };
                long[] cdNelmts = { 20 };
                int[] cdValues = new int[(int) cdNelmts[0]];
                String[] cdName = { "", "" };
                log.trace("getMetadata(): {} filters in pipeline", nfilt);
                int filter = -1;
                int[] filterConfig = { 1 };

                filters.setLength(0);

                if (nfilt == 0) {
                    filters.append("NONE");
                }
                else {
                    for (int i = 0, k = 0; i < nfilt; i++) {
                        log.trace("getMetadata(): filter[{}]", i);
                        if (i > 0) {
                            filters.append(", ");
                        }
                        if (k > 0) {
                            compression.append(", ");
                        }

                        try {
                            cdNelmts[0] = 20;
                            cdValues = new int[(int) cdNelmts[0]];
                            cdValues = new int[(int) cdNelmts[0]];
                            filter = H5.H5Pget_filter(pcid, i, flags, cdNelmts, cdValues, 120, cdName, filterConfig);
                            log.trace("getMetadata(): filter[{}] is {} has {} elements ", i, cdName[0], cdNelmts[0]);
                            for (int j = 0; j < cdNelmts[0]; j++) {
                                log.trace("getMetadata(): filter[{}] element {} = {}", i, j, cdValues[j]);
                            }
                        }
                        catch (Exception err) {
                            log.debug("getMetadata(): filter[{}] error: ", i, err);
                            log.trace("getMetadata(): filter[{}] continue", i);
                            filters.append("ERROR");
                            continue;
                        }

                        if (filter == HDF5Constants.H5Z_FILTER_NONE) {
                            filters.append("NONE");
                        }
                        else if (filter == HDF5Constants.H5Z_FILTER_DEFLATE) {
                            filters.append("GZIP");
                            compression.append(COMPRESSION_GZIP_TXT).append(cdValues[0]);
                            k++;
                        }
                        else if (filter == HDF5Constants.H5Z_FILTER_FLETCHER32) {
                            filters.append("Error detection filter");
                        }
                        else if (filter == HDF5Constants.H5Z_FILTER_SHUFFLE) {
                            filters.append("SHUFFLE: Nbytes = ").append(cdValues[0]);
                        }
                        else if (filter == HDF5Constants.H5Z_FILTER_NBIT) {
                            filters.append("NBIT");
                        }
                        else if (filter == HDF5Constants.H5Z_FILTER_SCALEOFFSET) {
                            filters.append("SCALEOFFSET: MIN BITS = ").append(cdValues[0]);
                        }
                        else if (filter == HDF5Constants.H5Z_FILTER_SZIP) {
                            filters.append("SZIP");
                            compression.append("SZIP: Pixels per block = ").append(cdValues[1]);
                            k++;
                            int flag = -1;
                            try {
                                flag = H5.H5Zget_filter_info(filter);
                            }
                            catch (Exception ex) {
                                log.debug("getMetadata(): H5Zget_filter_info failure: ", ex);
                                flag = -1;
                            }
                            if (flag == HDF5Constants.H5Z_FILTER_CONFIG_DECODE_ENABLED) {
                                compression.append(": H5Z_FILTER_CONFIG_DECODE_ENABLED");
                            }
                            else if ((flag == HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED)
                                    || (flag >= (HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED
                                            + HDF5Constants.H5Z_FILTER_CONFIG_DECODE_ENABLED))) {
                                compression.append(": H5Z_FILTER_CONFIG_ENCODE_ENABLED");
                            }
                        }
                        else {
                            filters.append("USERDEFINED ").append(cdName[0]).append("(").append(filter).append("): ");
                            for (int j = 0; j < cdNelmts[0]; j++) {
                                if (j > 0)
                                    filters.append(", ");
                                filters.append(cdValues[j]);
                            }
                            log.debug("getMetadata(): filter[{}] is user defined compression", i);
                        }
                    } //  (int i=0; i<nfilt; i++)
                }

                if (compression.length() == 0) {
                    compression.append("NONE");
                }
                log.trace("getMetadata(): filter compression={}", compression);

                log.trace("getMetadata(): filter information={}", filters);

                storage.setLength(0);
                storage.append("SIZE: ").append(storageSize);

                try {
                    int[] at = { 0 };
                    H5.H5Pget_alloc_time(pcid, at);
                    storage.append(", allocation time: ");
                    if (at[0] == HDF5Constants.H5D_ALLOC_TIME_EARLY) {
                        storage.append("Early");
                    }
                    else if (at[0] == HDF5Constants.H5D_ALLOC_TIME_INCR) {
                        storage.append("Incremental");
                    }
                    else if (at[0] == HDF5Constants.H5D_ALLOC_TIME_LATE) {
                        storage.append("Late");
                    }
                    else
                        storage.append("Default");
                }
                catch (Exception ex) {
                    log.debug("getMetadata(): Storage allocation time:", ex);
                }
                log.trace("getMetadata(): storage={}", storage);
            }
            finally {
                try {
                    H5.H5Pclose(paid);
                }
                catch (Exception ex) {
                    log.debug("getMetadata(): H5Pclose(paid {}) failure: ", paid, ex);
                }
                try {
                    H5.H5Pclose(pcid);
                }
                catch (Exception ex) {
                    log.debug("getMetadata(): H5Pclose(pcid {}) failure: ", pcid, ex);
                }
                close(did);
            }
        }

        log.trace("getMetadata(): finish");
        return attributeList;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#writeMetadata(java.lang.Object)
     */
    @Override
    public void writeMetadata(Object info) throws Exception {
        log.trace("writeMetadata(): start");

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("writeMetadata(): Object not an Attribute");
            log.trace("writeMetadata(): finish");
            return;
        }

        boolean attrExisted = false;
        Attribute attr = (Attribute) info;
        log.trace("writeMetadata(): {}", attr.getName());

        if (attributeList == null) {
            this.getMetadata();
        }

        if (attributeList != null)
            attrExisted = attributeList.contains(attr);

        getFileFormat().writeAttribute(this, attr, attrExisted);
        // add the new attribute into attribute list
        if (!attrExisted) {
            attributeList.add(attr);
            nAttributes = attributeList.size();
        }

        log.trace("writeMetadata(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#removeMetadata(java.lang.Object)
     */
    @Override
    public void removeMetadata(Object info) throws HDF5Exception {
        log.trace("removeMetadata(): start");

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("removeMetadata(): Object not an Attribute");
            log.trace("removeMetadata(): finish");
            return;
        }

        Attribute attr = (Attribute) info;
        log.trace("removeMetadata(): {}", attr.getName());
        long did = open();
        if (did >= 0) {
            try {
                H5.H5Adelete(did, attr.getName());
                List<Attribute> attrList = getMetadata();
                attrList.remove(attr);
                nAttributes = attrList.size();
            }
            finally {
                close(did);
            }
        }

        log.trace("removeMetadata(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#updateMetadata(java.lang.Object)
     */
    @Override
    public void updateMetadata(Object info) throws HDF5Exception {
        log.trace("updateMetadata(): start");

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("updateMetadata(): Object not an Attribute");
            log.trace("updateMetadata(): finish");
            return;
        }

        nAttributes = -1;

        log.trace("updateMetadata(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.HObject#setName(java.lang.String)
     */
    @Override
    public void setName(String newName) throws Exception {
        H5File.renameObject(this, newName);
        super.setName(newName);
    }

    /**
     * Resets selection of dataspace
     */
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
            // selectedIndex[0] = rank - 2; // columns
            // selectedIndex[1] = rank - 1; // rows
            // selectedIndex[2] = rank - 3;
            selectedIndex[0] = 0; // width, the fastest dimension
            selectedIndex[1] = 1; // height
            selectedIndex[2] = 2; // frames
            // selectedDims[rank - 1] = dims[rank - 1];
            // selectedDims[rank - 2] = dims[rank - 2];
            selectedDims[selectedIndex[0]] = dims[selectedIndex[0]];
            selectedDims[selectedIndex[1]] = dims[selectedIndex[1]];
        }

        isDataLoaded = false;
        setAllMemberSelection(true);
        log.trace("resetSelection(): finish");
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using
     *             {@link #create(String, Group, long[], long[], long[], int, String[], Datatype[], int[], long[][], Object)}
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param dims
     *            the dimension size of the dataset.
     * @param memberNames
     *            the names of compound datatype
     * @param memberDatatypes
     *            the datatypes of the compound datatype
     * @param memberSizes
     *            the dim sizes of the members
     * @param data
     *            list of data arrays written to the new dataset, null if no data is written to the new
     *            dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception
     *             if there is a failure.
     */
    @Deprecated
    public static Dataset create(String name, Group pgroup, long[] dims, String[] memberNames,
            Datatype[] memberDatatypes, int[] memberSizes, Object data) throws Exception {
        if ((pgroup == null) || (name == null) || (dims == null) || (memberNames == null) || (memberDatatypes == null)
                || (memberSizes == null)) {
            return null;
        }

        int nMembers = memberNames.length;
        int memberRanks[] = new int[nMembers];
        long memberDims[][] = new long[nMembers][1];
        for (int i = 0; i < nMembers; i++) {
            memberRanks[i] = 1;
            memberDims[i][0] = memberSizes[i];
        }

        return H5CompoundDS.create(name, pgroup, dims, memberNames, memberDatatypes, memberRanks, memberDims, data);
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using
     *             {@link #create(String, Group, long[], long[], long[], int, String[], Datatype[], int[], long[][], Object)}
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param dims
     *            the dimension size of the dataset.
     * @param memberNames
     *            the names of compound datatype
     * @param memberDatatypes
     *            the datatypes of the compound datatype
     * @param memberRanks
     *            the ranks of the members
     * @param memberDims
     *            the dim sizes of the members
     * @param data
     *            list of data arrays written to the new dataset, null if no data is written to the new
     *            dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception
     *             if the dataset can not be created.
     */
    @Deprecated
    public static Dataset create(String name, Group pgroup, long[] dims, String[] memberNames,
            Datatype[] memberDatatypes, int[] memberRanks, long[][] memberDims, Object data) throws Exception {
        return H5CompoundDS.create(name, pgroup, dims, null, null, -1, memberNames, memberDatatypes, memberRanks,
                memberDims, data);
    }

    /**
     * Creates a simple compound dataset in a file with/without chunking and compression.
     * <p>
     * This function provides an easy way to create a simple compound dataset in file by hiding tedious
     * details of creating a compound dataset from users.
     * <p>
     * This function calls H5.H5Dcreate() to create a simple compound dataset in file. Nested compound
     * dataset is not supported. The required information to create a compound dataset includes the
     * name, the parent group and data space of the dataset, the names, datatypes and data spaces of the
     * compound fields. Other information such as chunks, compression and the data buffer is optional.
     * <p>
     * The following example shows how to use this function to create a compound dataset in file.
     *
     * <pre>
     * H5File file = null;
     * String message = &quot;&quot;;
     * Group pgroup = null;
     * int[] DATA_INT = new int[DIM_SIZE];
     * float[] DATA_FLOAT = new float[DIM_SIZE];
     * String[] DATA_STR = new String[DIM_SIZE];
     * long[] DIMs = { 50, 10 };
     * long[] CHUNKs = { 25, 5 };
     *
     * try {
     *     file = (H5File) H5FILE.open(fname, H5File.CREATE);
     *     file.open();
     *     pgroup = (Group) file.get(&quot;/&quot;);
     * }
     * catch (Exception ex) {
     * }
     *
     * Vector data = new Vector();
     * data.add(0, DATA_INT);
     * data.add(1, DATA_FLOAT);
     * data.add(2, DATA_STR);
     *
     * // create groups
     * Datatype[] mdtypes = new H5Datatype[3];
     * String[] mnames = { &quot;int&quot;, &quot;float&quot;, &quot;string&quot; };
     * Dataset dset = null;
     * try {
     *     mdtypes[0] = new H5Datatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE);
     *     mdtypes[1] = new H5Datatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, Datatype.NATIVE);
     *     mdtypes[2] = new H5Datatype(Datatype.CLASS_STRING, STR_LEN, Datatype.NATIVE, Datatype.NATIVE);
     *     dset = file.createCompoundDS(&quot;/CompoundDS&quot;, pgroup, DIMs, null, CHUNKs, 9, mnames, mdtypes, null, data);
     * }
     * catch (Exception ex) {
     *     failed(message, ex, file);
     *     return 1;
     * }
     * </pre>
     *
     * @param name
     *            the name of the dataset to create.
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param dims
     *            the dimension size of the dataset.
     * @param maxdims
     *            the max dimension size of the dataset. maxdims is set to dims if maxdims = null.
     * @param chunks
     *            the chunk size of the dataset. No chunking if chunk = null.
     * @param gzip
     *            GZIP compression level (1 to 9). 0 or negative values if no compression.
     * @param memberNames
     *            the names of compound datatype
     * @param memberDatatypes
     *            the datatypes of the compound datatype
     * @param memberRanks
     *            the ranks of the members
     * @param memberDims
     *            the dim sizes of the members
     * @param data
     *            list of data arrays written to the new dataset, null if no data is written to the new
     *            dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception
     *             if there is a failure.
     */
    public static Dataset create(String name, Group pgroup, long[] dims, long[] maxdims, long[] chunks, int gzip,
            String[] memberNames, Datatype[] memberDatatypes, int[] memberRanks, long[][] memberDims, Object data)
                    throws Exception {
        log.trace("create(): start");

        H5CompoundDS dataset = null;
        String fullPath = null;
        long did = -1;
        long tid = -1;
        long plist = -1;
        long sid = -1;

        if ((pgroup == null) || (name == null) || (dims == null) || ((gzip > 0) && (chunks == null))
                || (memberNames == null) || (memberDatatypes == null) || (memberRanks == null)
                || (memberDims == null)) {
            log.debug("create(): one or more parameters are null");
            log.trace("create(): finish");
            return null;
        }

        H5File file = (H5File) pgroup.getFileFormat();
        if (file == null) {
            log.debug("create(): parent group FileFormat is null");
            log.trace("create(): finish");
            return null;
        }

        String path = HObject.SEPARATOR;
        if (!pgroup.isRoot()) {
            path = pgroup.getPath() + pgroup.getName() + HObject.SEPARATOR;
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            int idx = name.lastIndexOf('/');
            if (idx >= 0) {
                name = name.substring(idx + 1);
            }
        }

        fullPath = path + name;

        int typeSize = 0;
        int nMembers = memberNames.length;
        long[] mTypes = new long[nMembers];
        int memberSize = 1;
        for (int i = 0; i < nMembers; i++) {
            memberSize = 1;
            for (int j = 0; j < memberRanks[i]; j++) {
                memberSize *= memberDims[i][j];
            }

            mTypes[i] = -1;
            // the member is an array
            if ((memberSize > 1) && (!memberDatatypes[i].isString())) {
                long tmptid = -1;
                if ((tmptid = memberDatatypes[i].createNative()) >= 0) {
                    try {
                        mTypes[i] = H5.H5Tarray_create(tmptid, memberRanks[i], memberDims[i]);
                    }
                    finally {
                        try {
                            H5.H5Tclose(tmptid);
                        }
                        catch (Exception ex) {
                            log.debug("create(): H5Tclose(tmptid {}) failure: ", tmptid, ex);
                        }
                    }
                }
            }
            else {
                mTypes[i] = memberDatatypes[i].createNative();
            }
            try {
                typeSize += H5.H5Tget_size(mTypes[i]);
            }
            catch (Exception ex) {
                log.debug("create(): array create H5Tget_size:", ex);

                while (i > 0) {
                    try {
                        H5.H5Tclose(mTypes[i]);
                    }
                    catch (HDF5Exception ex2) {
                        log.debug("create(): H5Tclose(mTypes[{}] {}) failure: ", i, mTypes[i], ex2);
                    }
                    i--;
                }
                throw ex;
            }
        } //  (int i = 0; i < nMembers; i++) {

        // setup chunking and compression
        boolean isExtentable = false;
        if (maxdims != null) {
            for (int i = 0; i < maxdims.length; i++) {
                if (maxdims[i] == 0) {
                    maxdims[i] = dims[i];
                }
                else if (maxdims[i] < 0) {
                    maxdims[i] = HDF5Constants.H5S_UNLIMITED;
                }

                if (maxdims[i] != dims[i]) {
                    isExtentable = true;
                }
            }
        }

        // HDF5 requires you to use chunking in order to define extendible
        // datasets. Chunking makes it possible to extend datasets efficiently,
        // without having to reorganize storage excessively. Using default size
        // of 64x...which has good performance
        if ((chunks == null) && isExtentable) {
            chunks = new long[dims.length];
            for (int i = 0; i < dims.length; i++)
                chunks[i] = Math.min(dims[i], 64);
        }

        // prepare the dataspace and datatype
        int rank = dims.length;

        try {
            sid = H5.H5Screate_simple(rank, dims, maxdims);

            // figure out creation properties
            plist = HDF5Constants.H5P_DEFAULT;

            tid = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, typeSize);
            int offset = 0;
            for (int i = 0; i < nMembers; i++) {
                H5.H5Tinsert(tid, memberNames[i], offset, mTypes[i]);
                offset += H5.H5Tget_size(mTypes[i]);
            }

            if (chunks != null) {
                plist = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);

                H5.H5Pset_layout(plist, HDF5Constants.H5D_CHUNKED);
                H5.H5Pset_chunk(plist, rank, chunks);

                // compression requires chunking
                if (gzip > 0) {
                    H5.H5Pset_deflate(plist, gzip);
                }
            }

            long fid = file.getFID();

            log.trace("create(): create dataset");
            did = H5.H5Dcreate(fid, fullPath, tid, sid, HDF5Constants.H5P_DEFAULT, plist, HDF5Constants.H5P_DEFAULT);
            log.trace("create(): new H5CompoundDS");
            dataset = new H5CompoundDS(file, name, path);
        }
        finally {
            try {
                H5.H5Pclose(plist);
            }
            catch (HDF5Exception ex) {
                log.debug("create(): H5Pclose(plist {}) failure: ", plist, ex);
            }
            try {
                H5.H5Sclose(sid);
            }
            catch (HDF5Exception ex) {
                log.debug("create(): H5Sclose(sid {}) failure: ", sid, ex);
            }
            try {
                H5.H5Tclose(tid);
            }
            catch (HDF5Exception ex) {
                log.debug("create(): H5Tclose(tid {}) failure: ", tid, ex);
            }
            try {
                H5.H5Dclose(did);
            }
            catch (HDF5Exception ex) {
                log.debug("create(): H5Dclose(did {}) failure: ", did, ex);
            }

            for (int i = 0; i < nMembers; i++) {
                try {
                    H5.H5Tclose(mTypes[i]);
                }
                catch (HDF5Exception ex) {
                    log.debug("create(): H5Tclose(mTypes[{}] {}) failure: ", i, mTypes[i], ex);
                }
            }
        }

        if (dataset != null) {
            pgroup.addToMemberList(dataset);
            if (data != null) {
                dataset.init();
                long selected[] = dataset.getSelectedDims();
                for (int i = 0; i < rank; i++) {
                    selected[i] = dims[i];
                }
                dataset.write(data);
            }
        }

        log.trace("create(): finish");
        return dataset;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#isString(long)
     */
    @Override
    public boolean isString(long tid) {
        boolean b = false;
        try {
            b = (HDF5Constants.H5T_STRING == H5.H5Tget_class(tid));
        }
        catch (Exception ex) {
            b = false;
        }

        return b;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getSize(long)
     */
    @Override
    public long getSize(long tid) {
        long tsize = -1;

        try {
            tsize = H5.H5Tget_size(tid);
        }
        catch (Exception ex) {
            tsize = -1;
        }

        return tsize;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#isVirtual()
     */
    @Override
    public boolean isVirtual() {
        return isVirtual;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getVirtualFilename(int)
     */
    @Override
    public String getVirtualFilename(int index) {
        return (isVirtual) ? virtualNameList.get(index) : null;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getVirtualMaps()
     */
    @Override
    public int getVirtualMaps() {
        return (isVirtual) ? virtualNameList.size() : -1;
    }

}
