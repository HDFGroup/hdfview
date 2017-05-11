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

package hdf.object.h5;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
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

/**
 * The H5CompoundDS class defines an HDF5 dataset of compound datatypes.
 * <p>
 * An HDF5 dataset is an object composed of a collection of data elements, or raw data, and metadata that stores a
 * description of the data elements, data layout, and all other information necessary to write, read, and interpret the
 * stored data.
 * <p>
 * A HDF5 compound datatype is similar to a struct in C or a common block in Fortran: it is a collection of one or more
 * atomic types or small arrays of such types. Each member of a compound type has a name which is unique within that
 * type, and a byte offset that determines the first byte (smallest byte address) of that member in a compound datum.
 * <p>
 * For more information on HDF5 datasets and datatypes, read the
 * <a href="https://www.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html#t=HDF5_Users_Guide%2FHDF5_UG_Title%2FHDF5_UG_Title.htm">HDF5 User's Guide</a>.
 * <p>
 * There are two basic types of compound datasets: simple compound data and nested compound data. Members of a simple
 * compound dataset have atomic datatypes. Members of a nested compound dataset are compound or array of compound data.
 * <p>
 * Since Java does not understand C structures, we cannot directly read/write compound data values as in the following C
 * example.
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
 * Values of compound data fields are stored in java.util.Vector object. We read and write compound data by fields
 * instead of compound structure. As for the example above, the java.util.Vector object has three elements: int[LENGTH],
 * float[LENGTH] and double[LENGTH]. Since Java understands the primitive datatypes of int, float and double, we will be
 * able to read/write the compound data by field.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H5CompoundDS extends CompoundDS {
    private static final long serialVersionUID = -5968625125574032736L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5CompoundDS.class);

    /**
     * The list of attributes of this data object. Members of the list are instance of Attribute.
     */
    private List<Attribute> attributeList;

    private int nAttributes = -1;

    private H5O_info_t obj_info;

    /**
     * A list of names of all fields including nested fields.
     * <p>
     * The nested names are separated by CompoundDS.separator. For example, if compound dataset "A" has the following
     * nested structure,
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
     * The flatNameList of compound dataset "A" will be {m01, m02, nest1[m11, nest1[m12, nest1[nest2[m21,
     * nest1[nest2[m22}
     *
     */
    private List<String> flatNameList;

    /**
     * A list of datatypes of all fields including nested fields.
     */
    private List<Long> flatTypeList;

    /** flag to indicate is the dataset is an external dataset */
    private boolean isExternal = false;

    /** flag to indicate is the dataset is a virtual dataset */
    private boolean isVirtual = false;
    private List<String> virtualNameList;

    /**
     * Constructs an instance of a HDF5 compound dataset with given file, dataset name and path.
     * <p>
     * The dataset object represents an existing dataset in the file. For example, new H5CompoundDS(file, "dset1",
     * "/g0/") constructs a dataset object that corresponds to the dataset,"dset1", at group "/g0/".
     * <p>
     * This object is usually constructed at FileFormat.open(), which loads the file structure and object information
     * into memory. It is rarely used elsewhere.
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
        obj_info = new H5O_info_t(-1L, -1L, 0, 0, -1L, 0L, 0L, 0L, 0L, null, null, null);

        if ((oid == null) && (theFile != null)) {
            // retrieve the object ID
            try {
                byte[] ref_buf = H5.H5Rcreate(theFile.getFID(), this.getFullName(), HDF5Constants.H5R_OBJECT, -1);
                this.oid = new long[1];
                this.oid[0] = HDFNativeData.byteToLong(ref_buf, 0);
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
                log.debug("close(): H5Fflush({}, {}) failure: ", did, HDF5Constants.H5F_SCOPE_LOCAL, ex);
            }
            try {
                H5.H5Dclose(did);
            }
            catch (HDF5Exception ex) {
                log.debug("close(): H5Dclose({}) failure: ", did, ex);
            }
        }

        log.trace("close(): finish");
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#init()
     */
    @Override
    public void init() {
        log.trace("init(): start");

        if (rank > 0) {
            resetSelection();
            log.trace("init(): Dataset already inited");
            log.trace("init(): finish");
            return; // already called. Initialize only once
        }

        long did = -1;
        long sid = -1;
        long tid = -1;
        int tclass = -1;
        flatNameList = new Vector<>();
        flatTypeList = new Vector<>();
        long[] memberTIDs = null;

        did = open();
        if (did >= 0) {
            // check if it is an external or virtual dataset
            long pid = -1;
            try {
                pid = H5.H5Dget_create_plist(did);
                int nfiles = H5.H5Pget_external_count(pid);
                isExternal = (nfiles > 0);
                int layout_type = H5.H5Pget_layout(pid);
                if(isVirtual = (layout_type == HDF5Constants.H5D_VIRTUAL)) {
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
                                catch (Throwable err) {
                                    log.trace("init(): vds[{}] continue", next);
                                    continue;
                                }
                            }
                        }
                    }
                    catch (Throwable err) {
                        log.debug("init(): vds count error: ", err);
                    }
                }
                log.trace("init(): pid={} nfiles={} isExternal={} isVirtual={}", pid, nfiles, isExternal, isVirtual);
            }
            catch (Exception ex) {
                log.debug("init(): check if it is an external or virtual dataset:", ex);
            }
            finally {
                try {
                    H5.H5Pclose(pid);
                }
                catch (Exception ex) {
                    log.debug("init(): H5Pclose({}) failure: ", pid, ex);
                }
            }

            try {
                sid = H5.H5Dget_space(did);
                rank = H5.H5Sget_simple_extent_ndims(sid);
                tid = H5.H5Dget_type(did);
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

                if (rank == 0) {
                    // a scalar data point
                    rank = 1;
                    dims = new long[1];
                    dims[0] = 1;
                }
                else {
                    dims = new long[rank];
                    maxDims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, maxDims);
                    log.trace("init(): rank={}, dims={}, maxDims={}", rank, dims, maxDims);
                }

                startDims = new long[rank];
                selectedDims = new long[rank];

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
                    memberTIDs[i] = ((Long) flatTypeList.get(i)).longValue();
                    memberTypes[i] = new H5Datatype(memberTIDs[i]);
                    memberNames[i] = (String) flatNameList.get(i);
                    memberOrders[i] = 1;
                    memberDims[i] = null;
                    log.trace("init()[{}]: memberNames[{}]={}, memberTIDs[{}]={}, memberTypes[{}]={}", i, i, memberNames[i], i, memberTIDs[i], i, memberTypes[i]);

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
                        memberOrders[i] = (int)(H5.H5Tget_size(memberTIDs[i]) / H5.H5Tget_size(tmptid));
                        try {
                            H5.H5Tclose(tmptid);
                        }
                        catch (HDF5Exception ex) {
                            log.debug("init(): memberTIDs[{}] H5Tclose(tmptid {}) failure: ", i, tmptid, ex);
                        }
                    }
                } // for (int i=0; i<numberOfMembers; i++)
            }
            catch (HDF5Exception ex) {
                numberOfMembers = 0;
                memberNames = null;
                memberTypes = null;
                memberOrders = null;
                log.debug("init():", ex);
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("init(): H5Tclose({}) failure: ", tid, ex2);
                }
                try {
                    H5.H5Sclose(sid);
                }
                catch (HDF5Exception ex2) {
                    log.debug("init(): H5Sclose({}) failure: ", sid, ex2);
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
    public boolean hasAttribute() {
        obj_info.num_attrs = nAttributes;

        if (obj_info.num_attrs < 0) {
            long did = open();
            if (did >= 0) {
                try {
                    obj_info = H5.H5Oget_info(did);
                    nAttributes = (int) obj_info.num_attrs;
                }
                catch (Exception ex) {
                    obj_info.num_attrs = 0;
                    log.debug("hasAttribute(): get object info failure: ", ex);
                }
                close(did);
            }
            else {
                log.debug("hasAttribute(): could not open dataset");
            }
        }

        return (obj_info.num_attrs > 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getDatatype()
     */
    @Override
    public Datatype getDatatype() {
        if (datatype == null) {
            log.trace("getDatatype(): datatype == null");
            datatype = new H5Datatype(Datatype.CLASS_COMPOUND, -1, -1, -1);
        }

        return datatype;
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

        if (rank <= 0) {
            init();
        }

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

                if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE) throw new Exception("Dataset too large to read.");

                theData = new byte[(int)size];

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
                    log.debug("readBytes(): H5Tclose({}) failure: ", tid, ex2);
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

        List<Object> list = null;
        Object member_data = null;
        String member_name = null;
        int member_class = -1;
        int member_base_class = -1;
        int member_size = 0;
        long atom_tid = -1;
        long did = -1;
        long tid = -1;
        long[] spaceIDs = { -1, -1 }; // spaceIDs[0]=mspace, spaceIDs[1]=fspace

        if (rank <= 0) {
            init(); // read data information into memory
        }

        if (numberOfMembers <= 0) {
            log.debug("read(): Dataset contains no members");
            log.trace("read(): finish");
            return null; // this compound dataset does not have any member
        }

        if (isExternal) {
            String pdir = this.getFileFormat().getAbsoluteFile().getParent();

            if (pdir == null) {
                pdir = ".";
            }
            System.setProperty("user.dir", pdir);//H5.H5Dchdir_ext(pdir);
            log.trace("read(): External dataset: user.dir={}", pdir);
        }

        long[] lsize = { 1 };
        log.trace("read(): open dataset");
        did = open();
        if (did >= 0) {
            list = new Vector<>(flatNameList.size());
            Vector<Long> atomicList = new Vector<>();
            try {
                lsize[0] = selectHyperslab(did, spaceIDs);
                log.trace("read(): opened dataset size {} for {}", lsize[0], nPoints);

                if (lsize[0] == 0) {
                    log.debug("read(): No data to read");
                    log.trace("read(): finish");
                    throw new HDF5Exception("No data to read.\nEither the dataset or the selected subset is empty.");
                }

                if (lsize[0] < Integer.MIN_VALUE || lsize[0] > Integer.MAX_VALUE) {
                    log.debug("read(): lsize outside valid int range; unsafe cast");
                    log.trace("read(): finish");
                    throw new HDF5Exception("Invalid int size");
                }

                if (log.isDebugEnabled()) {
                    // check is storage space is allocated
                    try {
                        long ssize = H5.H5Dget_storage_size(did);
                        log.trace("read(): Storage space allocated = {}.", ssize);
                    }
                    catch (Exception ex) {
                        log.debug("read(): check if storage space is allocated:", ex);
                    }
                }

                // read each of member data into a byte array, then extract
                // it into its type such, int, long, float, etc.
                int n = flatNameList.size();
                tid = H5.H5Dget_type(did);
                int tclass = H5.H5Tget_class(tid);
                if (tclass == HDF5Constants.H5T_ARRAY) {
                    // array of compound
                    long tmptid = -1;
                    try {
                        tmptid = tid;
                        log.trace("read(): H5Tget_super");
                        tid = H5.H5Tget_super(tmptid);

                        // ARRAY of COMPOUND currently unsupported
                        if (H5.H5Tget_class(tid) == HDF5Constants.H5T_COMPOUND) {
                            log.debug("read(): cannot read dataset of type ARRAY of COMPOUND");
                            log.trace("read(): finish");
                            throw new Exception("Unsupported dataset of type ARRAY of COMPOUND");
                        }
                    }
                    finally {
                        try {H5.H5Tclose(tmptid);}
                        catch (Exception ex2) {log.debug("read(): H5Tclose(tmptid {}) failure: ", tmptid, ex2);}
                    }
                }

                extractCompoundInfo(tid, null, null, atomicList);

                log.trace("read(): foreach nMembers={}", n);
                for (int i = 0; i < n; i++) {
                    boolean isVL = false;
                    member_base_class = -1;

                    if (!isMemberSelected[i]) {
                        log.debug("read(): Member[{}] is not selected", i);
                        continue; // the field is not selected
                    }

                    member_name = new String(memberNames[i]);

                    atom_tid = ((Long) atomicList.get(i)).longValue();
                    try {
                        member_class = H5.H5Tget_class(atom_tid);
                        member_size = (int)H5.H5Tget_size(atom_tid);
                        member_data = H5Datatype.allocateArray(atom_tid, (int) lsize[0]);
                    }
                    catch (OutOfMemoryError err) {
                        member_data = null;
                        throw new HDF5Exception("Out Of Memory.");
                    }
                    catch (Exception ex) {
                        log.debug("read(): Member[{}]: ", i, ex);
                        member_data = null;
                    }
                    log.trace("read(): {} Member[{}] is class {} of size={}", member_name, i, member_class, member_size);

                    if (member_data == null || H5.H5Tequal(atom_tid, HDF5Constants.H5T_STD_REF_DSETREG)) {
                        String[] nullValues = new String[(int) lsize[0]];
                        String errorStr = "*unsupported*";
                        for (int j = 0; j < lsize[0]; j++) {
                            nullValues[j] = errorStr;
                        }
                        list.add(nullValues);

                        log.trace("read(): {} Member[{}] of class {} is unsupported.", member_name, i, member_class);
                        continue;
                    }
                    else if (member_class == HDF5Constants.H5T_ARRAY) {
                        long tmptid = -1;
                        try {
                            tmptid = H5.H5Tget_super(atom_tid);
                            member_base_class = H5.H5Tget_class(tmptid);

                            isVL = isVL || H5.H5Tis_variable_str(tmptid);
                            isVL = isVL || H5.H5Tdetect_class(tmptid, HDF5Constants.H5T_VLEN);

                            if (member_base_class == HDF5Constants.H5T_COMPOUND) {
                                try {
                                    member_data = H5Datatype.allocateArray(tmptid, member_size * (int) lsize[0]);
                                }
                                catch (OutOfMemoryError err) {
                                    member_data = null;
                                    throw new HDF5Exception("Out Of Memory.");
                                }
                                catch (Exception ex) {
                                    log.debug("read(): Member[{}]: Error allocating array for Compound: ", i, ex);
                                    member_data = null;
                                }
                            }

                            log.trace("read(): {} Array Member[{}] is class {} of size={}", member_name, i, member_base_class, member_size);
                        }
                        catch (Exception ex) {
                            log.debug("read(): H5T_ARRAY id or class failure[{}]:", i, ex);
                            continue;
                        }
                        finally {
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (Exception ex) {
                                log.debug("read(): Member[{}]: H5Tclose(tmptid {}) failure:", i, tmptid, ex);
                            }
                        }

                        // cannot deal with ARRAY of ARRAY, support only ARRAY of atomic types
                        if (member_base_class == HDF5Constants.H5T_ARRAY) {
                            String[] nullValues = new String[(int) lsize[0]];
                            String errorStr = "*unsupported*";
                            for (int j = 0; j < lsize[0]; j++) {
                                nullValues[j] = errorStr;
                            }
                            list.add(nullValues);

                            log.trace("read(): {} Member[{}] of type ARRAY of ARRAY is unsupported", member_name, i);
                            continue;
                        }
                    }

                    if (member_data != null) {
                        long comp_tid = -1;
                        int compInfo[] = { member_class, member_size, 0 };
                        try {
                            comp_tid = createCompoundFieldType(atom_tid, member_name, compInfo);
                        }
                        catch (HDF5Exception ex) {
                            String[] nullValues = new String[(int) lsize[0]];
                            for (int j = 0; j < lsize[0]; j++) {
                                nullValues[j] = "*unsupported*";
                            }
                            list.add(nullValues);
                            log.debug("read(): {} Member[{}] createCompoundFieldType failure:", member_name, i, ex);
                            continue;
                        }
                        try {
                            // See BUG#951 isVL = H5.H5Tdetect_class(atom_tid,
                            // HDF5Constants.H5T_VLEN);
                            isVL = isVL || H5.H5Tis_variable_str(atom_tid);
                            isVL = isVL || H5.H5Tdetect_class(atom_tid, HDF5Constants.H5T_VLEN);
                        }
                        catch (Exception ex) {
                            log.debug("read(): detection of varstr:", ex);
                            isVL = false;
                        }
                        try {
                            log.trace("read(): H5Dread({}) did={} spaceIDs[0]={} spaceIDs[1]={}", comp_tid, did, spaceIDs[0], spaceIDs[1]);
                            if (isVL) {
                                H5.H5Dread_VLStrings(did, comp_tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT,
                                        (Object[]) member_data);
                            }
                            else if (member_base_class == HDF5Constants.H5T_COMPOUND) {
                                H5.H5Dread(did, comp_tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT,
                                        (byte[]) member_data, true);
                            }
                            else {
                                H5.H5Dread(did, comp_tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, member_data);
                            }
                        }
                        catch (HDF5DataFiltersException exfltr) {
                            log.debug("read(): {} Member[{}] read failure:", member_name, i, exfltr);
                            log.trace("read(): finish");
                            throw new Exception("Filter not available exception: " + exfltr.getMessage(), exfltr);
                        }
                        catch (HDF5Exception ex2) {
                            String[] nullValues = new String[(int) lsize[0]];
                            for (int j = 0; j < lsize[0]; j++) {
                                nullValues[j] = "*unsupported*";
                            }
                            list.add(nullValues);
                            log.debug("read(): {} Member[{}] read failure:", member_name, i, ex2);
                            continue;
                        }
                        finally {
                            try {H5.H5Tclose(comp_tid);}
                            catch (Exception ex3) {log.debug("read(): H5Tclose(comp_tid {}) failure: ", comp_tid, ex3);}
                        }

                        if (!isVL) {
                            String cname = member_data.getClass().getName();
                            char dname = cname.charAt(cname.lastIndexOf("[") + 1);
                            log.trace("read(!isVL): {} Member[{}] is cname {} of dname={} convert={}", member_name, i, cname, dname, convertByteToString);

                            if ((member_class == HDF5Constants.H5T_STRING) && convertByteToString) {
                                if (dname == 'B') {
                                    member_data = byteToString((byte[]) member_data, member_size / memberOrders[i]);
                                    log.trace("read(!isVL): convertByteToString: {} Member[{}]", member_name, i);
                                }
                            }
                            else if (member_class == HDF5Constants.H5T_REFERENCE) {
                                if (dname == 'B') {
                                    member_data = HDFNativeData.byteToLong((byte[]) member_data);
                                    log.trace("read(!isVL): convertByteToLong: {} Member[{}]", member_name, i);
                                }
                            }
                            else if (compInfo[2] != 0) {
                                member_data = Dataset.convertFromUnsignedC(member_data, null);
                                log.trace("read(!isVL): convertFromUnsignedC: {} Member[{}]", member_name, i);
                            }
                            else if (member_class == HDF5Constants.H5T_ARRAY && member_base_class == HDF5Constants.H5T_COMPOUND) {
                                // Since compounds are read into memory as a byte array, discover each member
                                // type and size and convert the byte array to the correct type before adding
                                // it to the list

                                int numDims = H5.H5Tget_array_ndims(atom_tid);
                                long[] dims = new long[numDims];
                                H5.H5Tget_array_dims(atom_tid, dims);
                                int numberOfCompounds = (int) dims[0] * (int) lsize[0];
                                int compoundSize = (member_size * (int) lsize[0]) / numberOfCompounds;

                                Object current_data = new Object[numberOfCompounds];

                                long base_tid = -1;
                                long memberOffsets[] = null;
                                long memberLengths[] = null;
                                long memberTypes[] = null;
                                int numberOfMembers;

                                try {
                                    base_tid = H5.H5Tget_super(atom_tid);
                                    numberOfMembers = H5.H5Tget_nmembers(base_tid);
                                    memberOffsets = new long[numberOfMembers];
                                    memberLengths = new long[numberOfMembers];
                                    memberTypes = new long[numberOfMembers];

                                    for (int j = 0; j < numberOfMembers; j++) {
                                        memberOffsets[j] = H5.H5Tget_member_offset(base_tid, j);
                                        memberTypes[j] = H5.H5Tget_member_type(base_tid, j);
                                    }

                                    for (int j = 0; j < numberOfMembers; j++) {
                                        if (j < numberOfMembers - 1) {
                                            memberLengths[j] = (memberOffsets[j + 1] - memberOffsets[j]);
                                        }
                                        else {
                                            memberLengths[j] = (compoundSize - memberOffsets[j]);
                                        }
                                    }

                                    for (int j = 0; j < numberOfCompounds; j++) {
                                        Object field_data = new Object[numberOfMembers];

                                        for (int k = 0; k < numberOfMembers; k++) {
                                            Object converted = convertCompoundByteMember((byte[]) member_data, memberTypes[k], memberOffsets[k] + (compoundSize * j), memberLengths[k]);

                                            ((Object[]) field_data)[k] = Array.get(converted, 0);
                                        }

                                        ((Object[]) current_data)[j] = field_data;
                                    }
                                }
                                catch (Exception ex) {
                                    log.debug("read(): Convert Array of Compounds failure: ", ex);
                                    continue;
                                }
                                finally {
                                    for (int j = 0; j < memberTypes.length; j++) {
                                        try {
                                            H5.H5Tclose(memberTypes[j]);
                                        }
                                        catch (Exception ex) {
                                            log.debug("read(): Member[{}]: H5Tclose(memberTypes[{}] {}) failure: ", i, j, memberTypes[j], ex);
                                        }
                                    }
                                    try {
                                        H5.H5Tclose(base_tid);
                                    }
                                    catch (Exception ex) {
                                        log.debug("read(): Member[{}]: H5Tclose(base_tid {}) failure:", i, base_tid, ex);
                                    }
                                }

                                list.add(current_data);
                                continue;
                            }
                        }

                        list.add(member_data);
                    } // if (member_data != null)
                } // end of for (int i=0; i<num_members; i++)

            }
            finally {
                try {
                    if (HDF5Constants.H5S_ALL != spaceIDs[0])
                        H5.H5Sclose(spaceIDs[0]);
                }
                catch (Exception ex2) {
                    log.debug("read(): H5Sclose(spaceIDs[0] {}) failure: ", spaceIDs[0], ex2);
                }
                try {
                    if (HDF5Constants.H5S_ALL != spaceIDs[1])
                        H5.H5Sclose(spaceIDs[1]);
                }
                catch (Exception ex2) {
                    log.debug("read(): H5Sclose(spaceIDs[1] {}) failure: ", spaceIDs[1], ex2);
                }

                // close atomic types
                int ntypes = atomicList.size();
                for (int i = 0; i < ntypes; i++) {
                    atom_tid = ((Long) atomicList.get(i)).longValue();
                    try {
                        H5.H5Tclose(atom_tid);
                    }
                    catch (Exception ex2) {
                        log.debug("read(): H5Tclose(atom_tid {}) failure: ", atom_tid, ex2);
                    }
                }
                try {H5.H5Tclose(tid);}
                catch (Exception ex2) {log.debug("read(): H5Tclose(tid {}) failure: ", tid, ex2);}

                close(did);
            }
        }

        log.trace("read(): finish");
        return list;
    }

    /**
     * Writes the given data buffer into this dataset in a file.
     * <p>
     * The data buffer is a vector that contains the data values of compound fields. The data is written into file field
     * by field.
     *
     * @param buf
     *            The vector that contains the data values of compound fields.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    @Override
    public void write(Object buf) throws HDF5Exception {
        log.trace("write(): start");
        long did = -1;
        long tid = -1;
        long spaceIDs[] = { -1, -1 }; // spaceIDs[0]=mspace, spaceIDs[1]=fspace
        Object member_data = null;
        String member_name = null;
        long atom_tid = -1;
        int member_class = -1, member_size = 0;

        List<?> list = (List<?>) buf;
        if ((buf == null) || (numberOfMembers <= 0) || !(buf instanceof List)) {
            log.debug("write(): buf is null or invalid or contains no members");
            log.trace("write(): finish");
            return;
        }

        long[] lsize = { 1 };
        did = open();
        if (did >= 0) {
            log.trace("write(): dataset opened");
            Vector<Long> atomicList = new Vector<>();
            try {
                lsize[0] = selectHyperslab(did, spaceIDs);
                long tmptid = H5.H5Dget_type(did);

                // read each of member data into a byte array, then extract
                // it into its type such, int, long, float, etc.
                int idx = 0;
                int n = flatNameList.size();
                boolean isEnum = false;

                try {
                    extractCompoundInfo(tmptid, null, null, atomicList);
                }
                finally {
                    try {H5.H5Tclose(tmptid);}
                    catch (Exception ex2) {log.debug("write(): H5Tclose(tmptid {}) failure:", tmptid, ex2);}
                }
                for (int i = 0; i < n; i++) {
                    log.trace("write(): Member[{}] of {}", i, n);
                    if (!isMemberSelected[i]) {
                        log.debug("write(): Member[{}] is not selected", i);
                        continue; // the field is not selected
                    }

                    member_name = new String(memberNames[i]);
                    atom_tid = ((Long) atomicList.get(i)).longValue();
                    member_data = list.get(idx++);

                    if (member_data == null) {
                        log.debug("write(): Member[{}] data is null", i);
                        continue;
                    }

                    boolean isVL = false;
                    try {
                        isVL = (H5.H5Tget_class(atom_tid) == HDF5Constants.H5T_VLEN || H5.H5Tis_variable_str(atom_tid));
                        log.trace("write(): Member[{}] isVL={}", i, isVL);
                    }
                    catch (Exception ex) {
                        log.debug("write(): isVL: ", ex);
                    }

                    try {
                        member_class = H5.H5Tget_class(atom_tid);
                        member_size = (int)H5.H5Tget_size(atom_tid);
                        isEnum = (member_class == HDF5Constants.H5T_ENUM);
                    }
                    catch (Exception ex) {
                        log.debug("write(): retrieve member class or size failure: ", ex);
                    }
                    log.trace("write(): {} Member[{}] is class {} of size={}", member_name, i, member_class, member_size);

                    Object tmpData = member_data;

                    int compInfo[] = { member_class, member_size, 0 };
                    try {
                        tid = createCompoundFieldType(atom_tid, member_name, compInfo);
                        log.trace("write(): {} Member[{}] compInfo[class]={} compInfo[size]={} compInfo[unsigned]={}",
                                member_name, i, compInfo[0], compInfo[1], compInfo[2]);
                        if(isVL) {
                            H5.H5Dwrite_string(did, tid,
                                    spaceIDs[0], spaceIDs[1],
                                    HDF5Constants.H5P_DEFAULT, (String[])tmpData);
                        }
                        else {
                            if (compInfo[2] != 0) {
                                // check if need to convert integer data
                                int tsize = (int)H5.H5Tget_size(tid);
                                String cname = member_data.getClass().getName();
                                char dname = cname.charAt(cname.lastIndexOf("[") + 1);
                                boolean doConversion = (((tsize == 1) && (dname == 'S'))
                                        || ((tsize == 2) && (dname == 'I')) || ((tsize == 4) && (dname == 'J')));

                                tmpData = member_data;
                                if (doConversion) {
                                    log.trace("write(): {} Member[{}] convertToUnsignedC", member_name, i);
                                    tmpData = convertToUnsignedC(member_data, null);
                                }
                            }
                            else if ((member_class == HDF5Constants.H5T_STRING) && (Array.get(member_data, 0) instanceof String)) {
                                log.trace("write(): {} Member[{}] stringToByte", member_name, i);
                                tmpData = stringToByte((String[]) member_data, member_size);
                            }
                            else if (isEnum && (Array.get(member_data, 0) instanceof String)) {
                                log.trace("write(): {} Member[{}] convertEnumNameToValue", member_name, i);
                                tmpData = H5Datatype.convertEnumNameToValue(atom_tid, (String[]) member_data, null);
                            }

                            if (tmpData != null) {
                                // BUG!!! does not write nested compound data and no
                                // exception was caught
                                // need to check if it is a java error or C library
                                // error
                                log.debug("write(): H5Dwrite warning - does not write nested compound data");
                                H5.H5Dwrite(did, tid, spaceIDs[0], spaceIDs[1], HDF5Constants.H5P_DEFAULT, tmpData);
                            }
                        }
                    }
                    catch (Exception ex1) {
                        log.debug("write(): H5Dwrite process failure:", ex1);
                    }
                    finally {
                        try {
                            H5.H5Tclose(tid);
                        }
                        catch (Exception ex2) {
                            log.debug("write(): H5Tclose(tid {}) failure: ", tid, ex2);
                        }
                    }
                } // end of for (int i=0; i<num_members; i++)
            }
            finally {
                try {
                    if (HDF5Constants.H5S_ALL != spaceIDs[0])
                        H5.H5Sclose(spaceIDs[0]);
                }
                catch (Exception ex2) {
                    log.debug("write(): H5Sclose(spaceIDs[0] {}) failure: ", spaceIDs[0], ex2);
                }
                try {
                    if (HDF5Constants.H5S_ALL != spaceIDs[1])
                        H5.H5Sclose(spaceIDs[1]);
                }
                catch (Exception ex2) {
                    log.debug("write(): H5Sclose(spaceIDs[1] {}) failure: ", spaceIDs[1], ex2);
                }

                // close atomic types
                int ntypes = atomicList.size();
                for (int i = 0; i < ntypes; i++) {
                    atom_tid = ((Long) atomicList.get(i)).longValue();
                    try {
                        H5.H5Tclose(atom_tid);
                    }
                    catch (Exception ex2) {
                        log.debug("write(): H5Tclose(atom_tid {}) failure: ", atom_tid, ex2);
                    }
                }
            }
            close(did);
        }
        log.trace("write(): finish");
    }

    /**
     * Set up the selection of hyperslab
     *
     * @param did
     *            IN dataset ID
     * @param spaceIDs
     *            IN/OUT memory and file space IDs -- spaceIDs[0]=mspace, spaceIDs[1]=fspace
     *
     * @return total number of data point selected
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    private long selectHyperslab(long did, long[] spaceIDs) throws HDF5Exception {
        long lsize = 1;

        boolean isAllSelected = true;
        for (int i = 0; i < rank; i++) {
            lsize *= selectedDims[i];
            if (selectedDims[i] < dims[i]) {
                isAllSelected = false;
            }
        }

        log.trace("selectHyperslab(): isAllSelected={}", isAllSelected);

        if (isAllSelected) {
            spaceIDs[0] = HDF5Constants.H5S_ALL;
            spaceIDs[1] = HDF5Constants.H5S_ALL;
        }
        else {
            spaceIDs[1] = H5.H5Dget_space(did);

            // When 1D dataspace is used in chunked dataset, reading is very
            // slow.
            // It is a known problem on HDF5 library for chunked dataset.
            // mspace = H5.H5Screate_simple(1, lsize, null);
            spaceIDs[0] = H5.H5Screate_simple(rank, selectedDims, null);
            H5.H5Sselect_hyperslab(spaceIDs[1], HDF5Constants.H5S_SELECT_SET, startDims, selectedStride, selectedDims,
                    null);
        }

        return lsize;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#getMetadata()
     */
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

        if (rank <= 0) {
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
            log.debug("getMetadata(): attributeList != null");
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
        log.trace("getMetadata(): open dataset");
        did = open();
        if (did >= 0) {
            log.trace("getMetadata(): dataset opened");
            try {
                compression = "";
                attributeList = H5File.getAttribute(did, indxType, order);
                log.trace("getMetadata(): attributeList loaded");

                // get the compression and chunk information
                pcid = H5.H5Dget_create_plist(did);
                paid = H5.H5Dget_access_plist(did);
                long storage_size = H5.H5Dget_storage_size(did);
                int nfilt = H5.H5Pget_nfilters(pcid);
                int layout_type = H5.H5Pget_layout(pcid);
                if (layout_type == HDF5Constants.H5D_CHUNKED) {
                    chunkSize = new long[rank];
                    H5.H5Pget_chunk(pcid, rank, chunkSize);
                    int n = chunkSize.length;
                    storage_layout = "CHUNKED: " + String.valueOf(chunkSize[0]);
                    for (int i = 1; i < n; i++) {
                        storage_layout += " X " + chunkSize[i];
                    }

                    if(nfilt > 0) {
                        long    nelmts = 1;
                        long    uncomp_size;
                        long    datum_size = getDatatype().getDatatypeSize();
                        if (datum_size < 0) {
                            long tmptid = -1;
                            try {
                                tmptid = H5.H5Dget_type(did);
                                datum_size = H5.H5Tget_size(tmptid);
                            }
                            finally {
                                try {H5.H5Tclose(tmptid);}
                                catch (Exception ex2) {log.debug("getMetadata(): H5Tclose(tmptid {}) failure: ", tmptid, ex2);}
                            }
                        }


                        for(int i = 0; i < rank; i++) {
                            nelmts *= dims[i];
                        }
                        uncomp_size = nelmts * datum_size;

                        /* compression ratio = uncompressed size /  compressed size */

                        if(storage_size != 0) {
                            double ratio = (double) uncomp_size / (double) storage_size;
                            DecimalFormat df = new DecimalFormat();
                            df.setMinimumFractionDigits(3);
                            df.setMaximumFractionDigits(3);
                            compression +=  df.format(ratio) + ":1";
                        }
                    }
                }
                else if (layout_type == HDF5Constants.H5D_COMPACT) {
                    storage_layout = "COMPACT";
                }
                else if (layout_type == HDF5Constants.H5D_CONTIGUOUS) {
                    storage_layout = "CONTIGUOUS";
                    if (H5.H5Pget_external_count(pcid) > 0)
                        storage_layout += " - EXTERNAL ";
                }
                else if (layout_type == HDF5Constants.H5D_VIRTUAL) {
                    storage_layout = "VIRTUAL - ";
                    try {
                        long vmaps = H5.H5Pget_virtual_count(pcid);
                        try {
                            int virt_view = H5.H5Pget_virtual_view(paid);
                            long virt_gap = H5.H5Pget_virtual_printf_gap(paid);
                            if (virt_view == HDF5Constants.H5D_VDS_FIRST_MISSING)
                                storage_layout += "First Missing";
                            else
                                storage_layout += "Last Available";
                            storage_layout += "\nGAP : " + String.valueOf(virt_gap);
                        }
                        catch (Throwable err) {
                            log.debug("getMetadata(): vds error: ", err);
                            storage_layout += "ERROR";
                        }
                        storage_layout += "\nMAPS : " + String.valueOf(vmaps);
                        if (vmaps > 0) {
                            for (long next = 0; next < vmaps; next++) {
                                try {
                                    long virtual_vspace = H5.H5Pget_virtual_vspace(pcid, next);
                                    long virtual_srcspace = H5.H5Pget_virtual_srcspace(pcid, next);
                                    String fname = H5.H5Pget_virtual_filename(pcid, next);
                                    String dsetname = H5.H5Pget_virtual_dsetname(pcid, next);
                                    storage_layout += "\n" + fname + " : " + dsetname;
                                }
                                catch (Throwable err) {
                                    log.debug("getMetadata(): vds space[{}] error: ", next, err);
                                    log.trace("getMetadata(): vds[{}] continue", next);
                                    storage_layout += "ERROR";
                                    continue;
                                }
                            }
                        }
                    }
                    catch (Throwable err) {
                        log.debug("getMetadata(): vds count error: ", err);
                        storage_layout += "ERROR";
                    }
                }
                else {
                    chunkSize = null;
                    storage_layout = "NONE";
                }

                int[] flags = { 0, 0 };
                long[] cd_nelmts = { 20 };
                int[] cd_values = new int[(int) cd_nelmts[0]];;
                String[] cd_name = { "", "" };
                log.trace("getMetadata(): {} filters in pipeline", nfilt);
                int filter = -1;
                int[] filter_config = { 1 };
                filters = "";

                for (int i = 0, k = 0; i < nfilt; i++) {
                    log.trace("getMetadata(): filter[{}]", i);
                    if (i > 0) {
                        filters += ", ";
                    }
                    if (k > 0) {
                        compression += ", ";
                    }

                    try {
                        cd_nelmts[0] = 20;
                        cd_values = new int[(int) cd_nelmts[0]];
                        cd_values = new int[(int) cd_nelmts[0]];
                        filter = H5.H5Pget_filter(pcid, i, flags, cd_nelmts, cd_values, 120, cd_name, filter_config);
                        log.trace("getMetadata(): filter[{}] is {} has {} elements ", i, cd_name[0], cd_nelmts[0]);
                        for (int j = 0; j < cd_nelmts[0]; j++) {
                            log.trace("getMetadata(): filter[{}] element {} = {}", i, j, cd_values[j]);
                        }
                    }
                    catch (Throwable err) {
                        filters += "ERROR";
                        continue;
                    }

                    if (filter == HDF5Constants.H5Z_FILTER_NONE) {
                        filters += "NONE";
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_DEFLATE) {
                        filters += "GZIP";
                        compression += compression_gzip_txt + cd_values[0];
                        k++;
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_FLETCHER32) {
                        filters += "Error detection filter";
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_SHUFFLE) {
                        filters += "SHUFFLE: Nbytes = " + cd_values[0];
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_NBIT) {
                        filters += "NBIT";
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_SCALEOFFSET) {
                        filters += "SCALEOFFSET: MIN BITS = " + cd_values[0];
                    }
                    else if (filter == HDF5Constants.H5Z_FILTER_SZIP) {
                        filters += "SZIP";
                        compression += "SZIP: Pixels per block = " + cd_values[1];
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
                            compression += ": H5Z_FILTER_CONFIG_DECODE_ENABLED";
                        }
                        else if ((flag == HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED)
                                || (flag >= (HDF5Constants.H5Z_FILTER_CONFIG_ENCODE_ENABLED + HDF5Constants.H5Z_FILTER_CONFIG_DECODE_ENABLED))) {
                            compression += ": H5Z_FILTER_CONFIG_ENCODE_ENABLED";
                        }
                    }
                    else {
                        filters += "USERDEFINED " + cd_name[0] + "(" + filter + "): ";
                        for (int j = 0; j < cd_nelmts[0]; j++) {
                            if (j > 0)
                                filters += ", ";
                            filters += cd_values[j];
                        }
                        log.debug("getMetadata(): filter[{}] is user defined compression", i);
                    }
                } // for (int i=0; i<nfilt; i++)

                if (compression.length() == 0) {
                    compression = "NONE";
                }
                log.trace("getMetadata(): filter compression={}", compression);

                if (filters.length() == 0) {
                    filters = "NONE";
                }
                log.trace("getMetadata(): filter information={}", filters);

                storage = "SIZE: " + storage_size;
                try {
                    int[] at = { 0 };
                    H5.H5Pget_alloc_time(pcid, at);
                    storage += ", allocation time: ";
                    if (at[0] == HDF5Constants.H5D_ALLOC_TIME_EARLY) {
                        storage += "Early";
                    }
                    else if (at[0] == HDF5Constants.H5D_ALLOC_TIME_INCR) {
                        storage += "Incremental";
                    }
                    else if (at[0] == HDF5Constants.H5D_ALLOC_TIME_LATE) {
                        storage += "Late";
                    }
                }
                catch (Exception ex) {
                    log.debug("getMetadata(): Storage allocation time:", ex);
                }
                if (storage.length() == 0) {
                    storage = "NONE";
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
    public void writeMetadata(Object info) throws Exception {
        log.trace("writeMetadata(): start");

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("writeMetadata(): data not instanceof Attribute");
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
    public void removeMetadata(Object info) throws HDF5Exception {
        log.trace("removeMetadata(): start");

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("removeMetadata(): data not instanceof Attribute");
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
    public void updateMetadata(Object info) throws HDF5Exception {
        log.trace("updateMetadata(): start");

        // only attribute metadata is supported.
        if (!(info instanceof Attribute)) {
            log.debug("updateMetadata(): data not instanceof Attribute");
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
        setMemberSelection(true);
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
     *            list of data arrays written to the new dataset, null if no data is written to the new dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception if there is a failure.
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
     *            list of data arrays written to the new dataset, null if no data is written to the new dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception if the dataset can not be created.
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
     * This function provides an easy way to create a simple compound dataset in file by hiding tedious details of
     * creating a compound dataset from users.
     * <p>
     * This function calls H5.H5Dcreate() to create a simple compound dataset in file. Nested compound dataset is not
     * supported. The required information to create a compound dataset includes the name, the parent group and data
     * space of the dataset, the names, datatypes and data spaces of the compound fields. Other information such as
     * chunks, compression and the data buffer is optional.
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
     *     mdtypes[0] = new H5Datatype(Datatype.CLASS_INTEGER, 4, -1, -1);
     *     mdtypes[1] = new H5Datatype(Datatype.CLASS_FLOAT, 4, -1, -1);
     *     mdtypes[2] = new H5Datatype(Datatype.CLASS_STRING, STR_LEN, -1, -1);
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
     *            list of data arrays written to the new dataset, null if no data is written to the new dataset.
     *
     * @return the new compound dataset if successful; otherwise returns null.
     *
     * @throws Exception if there is a failure.
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
                || (memberNames == null) || (memberDatatypes == null)
                || (memberRanks == null) || (memberDims == null)) {
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

        String path = HObject.separator;
        if (!pgroup.isRoot()) {
            path = pgroup.getPath() + pgroup.getName() + HObject.separator;
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            int idx = name.lastIndexOf("/");
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
            if ((memberSize > 1) && (memberDatatypes[i].getDatatypeClass() != Datatype.CLASS_STRING)) {
                long tmptid = -1;
                if ((tmptid = memberDatatypes[i].toNative()) >= 0) {
                    try {
                        mTypes[i] = H5.H5Tarray_create(tmptid, memberRanks[i], memberDims[i]);
                    }
                    finally {
                        try {H5.H5Tclose(tmptid);}
                        catch (Exception ex) {log.debug("create(): H5Tclose(tmptid {}) failure: ", tmptid, ex);}
                    }
                }
            }
            else {
                mTypes[i] = memberDatatypes[i].toNative();
            }
            try {
                typeSize += H5.H5Tget_size(mTypes[i]);
            }
            catch (Exception ex) {
                log.debug("create(): array create H5Tget_size:", ex);

                while (i > 0) {
                    try {H5.H5Tclose(mTypes[i]);}
                    catch (HDF5Exception ex2) {log.debug("create(): H5Tclose(mTypes[{}] {}) failure: ", i, mTypes[i], ex2);}
                    i--;
                }
                throw ex;
            }
        } // for (int i = 0; i < nMembers; i++) {

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

    /**
     * Extracts compound information into flat structure.
     * <p>
     * For example, compound datatype "nest" has {nest1{a, b, c}, d, e} then extractCompoundInfo() will put the names of
     * nested compound fields into a flat list as
     *
     * <pre>
     * nest.nest1.a
     * nest.nest1.b
     * nest.nest1.c
     * nest.d
     * nest.e
     * </pre>
     *
     * @param tid   the identifier of the compound datatype
     * @param name  the name of the compound datatype
     * @param names  the list to store the member names of the compound datatype
     * @param flatTypeList2  the list to store the nested member names of the compound datatype
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
        if(isVirtual)
            return virtualNameList.get(index);
        else
            return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.Dataset#getVirtualMaps()
     */
    @Override
    public int getVirtualMaps() {
        if(isVirtual)
            return virtualNameList.size();
        else
            return -1;
    }

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
     *            compInfo[0]--IN: class of member datatype; compInfo[1]--IN: size of member datatype; compInfo[2]--OUT:
     *            non-zero if the base type of the compound field is unsigned; zero, otherwise.
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

            log.trace("createCompoundFieldType(): {} Member is class {} of size={} with baseType={}", member_name, member_class, member_size,
                    baseType);
            if (member_class == HDF5Constants.H5T_ARRAY) {
                int mn = H5.H5Tget_array_ndims(atom_tid);
                long[] marray = new long[mn];
                H5.H5Tget_array_dims(atom_tid, marray);
                baseType = H5.H5Tget_super(atom_tid);
                tmp_tid4 = H5.H5Tget_native_type(baseType);
                arrayType = H5.H5Tarray_create(tmp_tid4, mn, marray);
                log.trace("createCompoundFieldType(): H5T_ARRAY {} Member is class {} of size={} with baseType={}", member_name, member_class,
                        member_size, baseType);
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

            member_size = (int)H5.H5Tget_size(atom_tid);
            log.trace("createCompoundFieldType(): member_size={}",member_size);

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
                    log.trace("createCompoundFieldType(sep): H5.H5Tclose:tmp_tid1={}",tmp_tid1);
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
     * Given an array of bytes representing a compound Datatype and a start
     * index and length, converts len number of bytes into the correct
     * Object type and returns it.
     *
     * @param data The byte array representing the data of the compound Datatype
     * @param data_type The type of data to convert the bytes to
     * @param start The start index of the bytes to get
     * @param len The number of bytes to convert
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
