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

import java.io.File;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFException;
import hdf.hdflib.HDFLibrary;
import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * This class provides file level APIs. File access APIs include retrieving the
 * file hierarchy, opening and closing file, and writing file content to disk.
 *
 * @version 2.4 9/4/2007
 * @author Peter X. Cao
 */
public class H4File extends FileFormat {
    private static final long serialVersionUID = 8985533001471224030L;

    private final static org.slf4j.Logger   log = org.slf4j.LoggerFactory.getLogger(H4File.class);

    /**
     * the file access flag.
     */
    private int                             flag;

    /**
     * The root object of this file.
     */
    private HObject                         rootObject;

    /**
     * The list of unique (tag, ref) pairs. It is used to avoid duplicate
     * objects in memory.
     */
    @SuppressWarnings("rawtypes")
    private List                            objList;

    /**
     * The GR interface identifier. The identifier is returned by GRstart(fid),
     * which initializes the GR interface for the file specified by the
     * parameter. GRstart(fid) is an expensive call. It should be called only
     * once. Calling GRstart(fid) in a loop should be avoided.
     */
    private long                            grid;

    private boolean                         isNetCDF = false;

    /**
     * The SDS interface identifier. The identifier is returned by
     * SDstart(fname, flag), which initializes the SD interface for the file
     * specified by the parameter. SDstart(fname, flag) is an expensive call. It
     * should be called only once. Calling SDstart(fname, flag) in a loop should
     * be avoided.
     */
    private long                            sdid;

    /**
     * secret flag: show CDF0.0, etc., to help debug
     */
    private boolean                         showAll = false;

    /**
     * Creates an H4File with read only access.
     */
    public H4File() {
        this("", WRITE);
    }

    /**
     * Creates an H4File with read only access.
     *
     * @param pathname
     *        The file path string.
     */
    public H4File(String pathname) {
        this(pathname, WRITE);
    }

    /**
     * Creates an H4File instance with specified file name and access.
     * <p>
     * The access parameter values and corresponding behaviors:
     * <ul>
     * <li>READ: Read-only access; open() will fail if file doesn't exist.
     * <li>WRITE: Read/Write access; if file doesn't exist, open() will create
     * it; open() will fail if read/write access not allowed.
     * <li>CREATE: Read/Write access; create a new file or truncate an existing
     * one; open() will fail if file can't be created or if file exists but
     * can't be opened read/write.
     * </ul>
     * <p>
     * This constructor does not open the file for access, nor does it confirm
     * that the file can later be opened read/write or created.
     * <p>
     * The flag returned by {@link #isReadOnly()} is set to true if the access
     * parameter value is READ, even though the file isn't yet open.
     *
     * @param fileName
     *            A valid file name, with a relative or absolute path.
     * @param access
     *            The file access flag, which determines behavior when file is
     *            opened. Acceptable values are <code> READ, WRITE, </code> and
     *            <code>CREATE</code>.
     *
     * @throws NullPointerException
     *             If the <code>fileName</code> argument is <code>null</code>.
     */
    @SuppressWarnings("rawtypes")
    public H4File(String fileName, int access) {
        super(fileName);
        isReadOnly = (access == READ);
        objList = new Vector();

        this.fid = -1;

        if (access == READ) {
            flag = HDFConstants.DFACC_READ;
        }
        else if (access == WRITE) {
            flag = HDFConstants.DFACC_WRITE;
        }
        else if (access == CREATE) {
            flag = HDFConstants.DFACC_CREATE;
        }
        else {
            flag = access;
        }

        log.trace("File: {} isReadOnly={} accessType={}", isReadOnly, flag);

        String shwAll = System.getProperty("h4showall");
        if (shwAll != null) {
            showAll = true;
            log.debug("show all is on");
        }
        else {
            log.debug("show all is off");
        }
    }

    /**
     * Checks if the given file format is an HDF4 file.
     *
     * @param fileformat
     *            the fileformat to be checked.
     *
     * @return true if the given file is an HDF4 file; otherwise returns false.
     */
    @Override
    public boolean isThisType(FileFormat fileformat) {
        return (fileformat instanceof H4File);
    }

    /**
     * Checks if the given file is an HDF4 file or netCDF. HDF4 library supports
     * netCDF version 2.3.2. It only supports SDS APIs.
     *
     * @param filename
     *            the file to be checked.
     *
     * @return true if the given file is an HDF4 file; otherwise returns false.
     */
    @Override
    public boolean isThisType(String filename) {
        boolean isH4 = false;

        try {
            isH4 = HDFLibrary.Hishdf(filename);
        }
        catch (HDFException ex) {
            isH4 = false;
        }

        if (!isH4) {
            isH4 = isNetCDF(filename);
        }

        log.trace("isThisType(): isH4={}", isH4);
        return isH4;
    }

    /**
     * Creates an HDF4 file with the specified name and returns a new H4File
     * instance associated with the file.
     *
     * @throws HDFException
     *             If the file cannot be created or if createFlag has unexpected
     *             value.
     *
     * @see hdf.object.FileFormat#createFile(java.lang.String, int)
     * @see #H4File(String, int)
     */
    @Override
    public FileFormat createFile(String filename, int createFlag)
            throws Exception {
        log.trace("createFile(): start");

        // Flag if we need to create or truncate the file.
        Boolean doCreateFile = true;

        // Won't create or truncate if CREATE_OPEN specified and file exists
        if (createFlag == FILE_CREATE_OPEN) {
            File f = new File(filename);
            if (f.exists()) {
                doCreateFile = false;
            }
        }

        log.trace("createFile(): doCreateFile={}", doCreateFile);

        if (doCreateFile) {
            long fileid = HDFLibrary.Hopen(filename, HDFConstants.DFACC_CREATE);
            try {
                HDFLibrary.Hclose(fileid);
            }
            catch (HDFException ex) {
                log.debug("Hclose failure: ", ex);
            }
        }

        log.trace("createFile(): finish");
        return new H4File(filename, WRITE);
    }

    /**
     * Creates an H4File instance with specified file name and access.
     *
     * @see hdf.object.FileFormat#createInstance(java.lang.String, int)
     * @see #H4File(String, int)
     */
    @Override
    public FileFormat createInstance(String filename, int access)
            throws Exception {
        return new H4File(filename, access);
    }

    // Implementing FileFormat
    @Override
    public long open() throws Exception {
        log.trace("open(): start");

        if (fid >= 0) {
            log.trace("open(): File " + fid + " already open");
            log.trace("open(): finish");
            return fid; // file is opened already
        }

        // check for valid file access permission
        if (flag < 0) { // invalid access id
            throw new HDFException("Invalid access identifer -- " + flag);
        }
        else if (flag == HDFConstants.DFACC_READ) {
            if (!exists()) {
                log.debug("File {} does not exist", fullFileName);
                log.trace("open(): finish");
                throw new HDFException("File does not exist -- " + fullFileName);
            }
            else if (exists() && !canRead()) {
                log.debug("Cannot read file {}", fullFileName);
                log.trace("open(): finish");
                throw new HDFException("Cannot read file -- " + fullFileName);
            }
        }
        else if ((flag == HDFConstants.DFACC_WRITE)
                || (flag == HDFConstants.DFACC_CREATE)) {
            if (exists() && !canWrite()) {
                log.debug("Cannot write file {}, try opening as read-only", fullFileName);
                log.trace("open(): finish");
                throw new HDFException(
                        "Cannot write file, try opening as read-only -- "
                                + fullFileName);
            }
        }

        // Only check for NetCDF if the file exists, else isNetCDF() throws an exception
        if (exists()) isNetCDF = isNetCDF(fullFileName);
        if (isNetCDF) {
            isReadOnly = true; // read only for netCDF
        }

        log.trace("open(): isNetCDF={}", isNetCDF);

        // only support SDS APIs for netCDF
        if (isNetCDF) {
            fid = 0;
        }
        else {
            log.trace("HDFLibrary - open({},{})", fullFileName, flag);
            fid = HDFLibrary.Hopen(fullFileName, flag);
            HDFLibrary.Vstart(fid);
            grid = HDFLibrary.GRstart(fid);
            log.trace("open(): fid:{} grid:{}", fid, grid);
        }
        sdid = HDFLibrary.SDstart(fullFileName, flag);
        log.trace("open(): sdid:{}", sdid);

        // load the file hierarchy
        loadIntoMemory();

        log.trace("open(): finish");
        return fid;
    }

    // Implementing FileFormat
    @Override
    public void close() throws HDFException {
        log.trace("close(): start");

        // clean unused objects
        if (rootObject != null) {
            HObject theObj = null;
            Iterator<HObject> it = getMembersBreadthFirst(rootObject).iterator();
            while (it.hasNext()) {
                theObj = it.next();

                if (theObj instanceof Dataset) {
                    ((Dataset) theObj).clearData();
                }
                else if (theObj instanceof Group) {
                    ((Group) theObj).clear();
                }
            }
        }

        try {
            HDFLibrary.GRend(grid);
        }
        catch (HDFException ex) {
            log.debug("close(): GRend failure: ", ex);
        }
        try {
            HDFLibrary.SDend(sdid);
        }
        catch (HDFException ex) {
            log.debug("close(): SDend failure: ", ex);
        }
        try {
            HDFLibrary.Vend(fid);
        }
        catch (HDFException ex) {
            log.debug("close(): Vend failure: ", ex);
        }

        HDFLibrary.Hclose(fid);

        fid = -1;
        objList = null;
    }

    // Implementing FileFormat
    @Override
    public HObject getRootObject() {
        return rootObject;
    }

    @Override
    public Group createGroup(String name, Group pgroup) throws Exception {
        return H4Group.create(name, pgroup);
    }

    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign)
            throws Exception {
        return new H4Datatype(tclass, tsize, torder, tsign);
    }

    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder,
            int tsign, Datatype tbase) throws Exception {
        return new H4Datatype(tclass, tsize, torder, tsign);
    }

    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder,
            int tsign, String name) throws Exception {
        throw new UnsupportedOperationException(
                "HDF4 does not support named datatype.");
    }

    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder,
            int tsign, Datatype tbase, String name) throws Exception {
        throw new UnsupportedOperationException(
                "HDF4 does not support named datatype.");
    }

    @Override
    public Dataset createScalarDS(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks, int gzip,
            Object fillValue, Object data) throws Exception {
        return H4SDS.create(name, pgroup, type, dims, maxdims, chunks, gzip,
                fillValue, data);
    }

    @Override
    public Dataset createImage(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks, int gzip, int ncomp,
            int interlace, Object data) throws Exception {
        H4GRImage dataset = H4GRImage.create(name, pgroup, type, dims, maxdims,
                chunks, gzip, ncomp, interlace, data);

        return dataset;
    }

    /**
     * Delete an object from the file.
     *
     * @param obj
     *            the data object to delete.
     *
     * @throws Exception if the object can not be deleted
     */
    @Override
    public void delete(HObject obj) throws Exception {
        throw (new UnsupportedOperationException("Cannot delete HDF4 object."));
    }

    /**
     * Copy an object to a group.
     *
     * @param srcObj
     *            the object to copy.
     * @param dstGroup
     *            the destination group.
     *
     * @return the destination group, if the copy was successful, or
     *            null otherwise.
     *
     * @throws Exception if the object can not be copied
     */
    @Override
    public HObject copy(HObject srcObj, Group dstGroup, String dstName)
            throws Exception {
        log.trace("copy(): start: srcObj={} dstGroup={} dstName={}", srcObj, dstGroup, dstName);

        if ((srcObj == null) || (dstGroup == null)) {
            log.debug("copy(): source or destination is null");
            log.trace("copy(): finish");
            return null;
        }

        if (dstName == null) {
            dstName = srcObj.getName();
            log.trace("copy(): dstName is null, using dstName={}", dstName);
        }

        HObject newObj = null;
        if (srcObj instanceof H4SDS) {
            log.trace("copy(): srcObj instanceof H4SDS");
            newObj = ((H4SDS) srcObj).copy(dstGroup, dstName, null, null);
        }
        else if (srcObj instanceof H4GRImage) {
            log.trace("copy(): srcObj instanceof H4GRImage");
            newObj = ((H4GRImage) srcObj).copy(dstGroup, dstName, null, null);
        }
        else if (srcObj instanceof H4Vdata) {
            log.trace("copy(): srcObj instanceof H4Vdata");
            newObj = ((H4Vdata) srcObj).copy(dstGroup, dstName, null, null);
        }
        else if (srcObj instanceof H4Group) {
            log.trace("copy(): srcObj instanceof H4Group");
            newObj = copyGroup((H4Group) srcObj, (H4Group) dstGroup);
        }

        log.trace("copy(): finish");
        return newObj;
    }

    /**
     * Creates a new attribute and attaches it to the object if the
     * attribute does not exist. Otherwise, just update the value of
     * the attribute.
     *
     * @param obj
     *            the object which the attribute is to be attached to.
     * @param attr
     *            the attribute to attach.
     * @param isSDglobalAttr
     *            The indicator if the given attribute exists.
     *
     * @throws HDFException if the attribute can not be written
     */
    @Override
    public void writeAttribute(HObject obj, Attribute attr,
            boolean isSDglobalAttr) throws HDFException {
        log.trace("writeAttribute(): start: obj={} attribute={} isSDglobalAttr={}", obj, attr, isSDglobalAttr);

        String attrName = attr.getName();
        long attrType = attr.getType().toNative();
        long[] dims = attr.getDataDims();
        int count = 1;
        if (dims != null) {
            for (int i = 0; i < dims.length; i++) {
                count *= (int) dims[i];
            }
        }

        log.trace("writeAttribute(): count={}", count);
        Object attrValue = attr.getValue();
        if (Array.get(attrValue, 0) instanceof String) {
            String strValue = (String) Array.get(attrValue, 0);

            if (strValue.length() > count) {
                // truncate the extra characters
                strValue = strValue.substring(0, count);
                Array.set(attrValue, 0, strValue);
            }
            else {
                // pad space to the unused space
                for (int i = strValue.length(); i < count; i++) {
                    strValue += " ";
                }
            }

            byte[] bval = strValue.getBytes();
            // add null to the end to get rid of the junks
            bval[(strValue.length() - 1)] = 0;
            attrValue = bval;
        }

        if ((obj instanceof H4Group) && ((H4Group) obj).isRoot()) {
            if (isSDglobalAttr) {
                HDFLibrary.SDsetattr(sdid, attrName, attrType, count, attrValue);
            }
            else {
                HDFLibrary.GRsetattr(grid, attrName, attrType, count, attrValue);
            }
            log.trace("writeAttribute(): wrote attribute to root H4Group");
            log.trace("writeAttribute(): finish");
            return;
        }

        long id = obj.open();

        if (id >= 0) {
            if (obj instanceof H4Group) {
                HDFLibrary.Vsetattr(id, attrName, attrType, count, attrValue);
                log.trace("writeAttribute(): wrote attribute to H4Group");
            }
            else if (obj instanceof H4SDS) {
                HDFLibrary.SDsetattr(id, attrName, attrType, count, attrValue);
                log.trace("writeAttribute(): wrote attribute to H4SDS");
            }
            else if (obj instanceof H4GRImage) {
                HDFLibrary.GRsetattr(id, attrName, attrType, count, attrValue);
                log.trace("writeAttribute(): wrote attribute to H4GRImage");
            }
            else if (obj instanceof H4Vdata) {
                HDFLibrary.VSsetattr(id, -1, attrName, attrType, count, attrValue);
                log.trace("writeAttribute(): wrote attribute to H4Vdata");
            }

            obj.close(id);
        }

        log.trace("writeAttribute(): finish");
    }

    private HObject copyGroup(H4Group srcGroup, H4Group pgroup)
            throws Exception {
        log.trace("copyGroup(): start: srcGroup={} parentGroup={}", srcGroup, pgroup);

        H4Group group = null;
        long srcgid, dstgid;
        String gname = null, path = null;

        dstgid = HDFLibrary.Vattach(fid, -1, "w");
        if (dstgid < 0) {
            log.trace("copyGroup(): Invalid dst Group Id");
            log.trace("copyGroup(): finish");
            return null;
        }

        gname = srcGroup.getName();
        srcgid = srcGroup.open();

        HDFLibrary.Vsetname(dstgid, gname);
        int ref = HDFLibrary.VQueryref(dstgid);
        int tag = HDFLibrary.VQuerytag(dstgid);

        if (pgroup.isRoot()) {
            path = HObject.separator;
        }
        else {
            // add the dataset to the parent group
            path = pgroup.getPath() + pgroup.getName() + HObject.separator;
            long pid = pgroup.open();
            HDFLibrary.Vinsert(pid, dstgid);
            pgroup.close(pid);
        }

        // copy attributes
        int numberOfAttributes = 0;
        try {
            numberOfAttributes = HDFLibrary.Vnattrs(srcgid);
        }
        catch (Exception ex) {
            log.debug("copyGroup(): Vnattrs failure: ", ex);
            numberOfAttributes = 0;
        }

        String[] attrName = new String[1];
        byte[] attrBuff = null;
        int[] attrInfo = new int[3]; // data_type, count, size
        for (int i = 0; i < numberOfAttributes; i++) {
            try {
                attrName[0] = "";
                HDFLibrary.Vattrinfo(srcgid, i, attrName, attrInfo);
                attrBuff = new byte[attrInfo[2]];
                HDFLibrary.Vgetattr(srcgid, i, attrBuff);
                HDFLibrary.Vsetattr(dstgid, attrName[0], attrInfo[0],
                        attrInfo[2], attrBuff);
            }
            catch (Exception ex) {
                log.trace("copyGroup(): failure: ", ex);
                continue;
            }
        }

        long[] oid = { tag, ref };
        group = new H4Group(this, gname, path, pgroup, oid);

        pgroup.addToMemberList(group);

        // copy members of the source group to the new group
        List<HObject> members = srcGroup.getMemberList();
        if ((members != null) && (members.size() > 0)) {
            Iterator<HObject> iterator = members.iterator();
            while (iterator.hasNext()) {
                HObject mObj = (HObject) iterator.next();
                try {
                    copy(mObj, group, mObj.getName());
                }
                catch (Exception ex) {
                    log.debug("copy(): failure: ", ex);
                }
            }
        }

        srcGroup.close(srcgid);

        if (dstgid >= 0) {
            try {
                HDFLibrary.Vdetach(dstgid);
            }
            catch (Exception ex) {
                log.debug("copyGroup(): Vdetach failure: ", ex);
            }
        }

        log.trace("copyGroup(): finish");
        return group;
    }

    /**
     * Retrieves the file structure from disk and returns the root object.
     * <p>
     * First gets the top level objects or objects that do not belong to any
     * groups. If a top level object is a group, call the depth_first() to
     * retrieve the sub-tree of that group, recursively.
     */
    private void loadIntoMemory() {
        log.trace("loadIntoMemory(): start");

        if (fid < 0) {
            log.debug("loadIntoMemory(): Invalid File Id");
            log.trace("loadIntoMemory(): finish");
            return;
        }

        int n = 0, ref = -1;
        int[] argv = null;

        // get top level VGroup
        int[] tmpN = new int[1];
        int[] refs = null;

        try {
            // first call to get the number of lone Vgroups
            log.trace("loadIntoMemory(): first call to Vlone: get number of lone Vgroups");
            n = HDFLibrary.Vlone(fid, tmpN, 0);
            log.trace("loadIntoMemory(): number of lone Vgroups={}", n);
            refs = new int[n];

            // second call to get the references of all lone Vgroups
            log.trace("loadIntoMemory(): second call to Vlone: get references of lone Vgroups");
            n = HDFLibrary.Vlone(fid, refs, n);
        }
        catch (HDFException ex) {
            log.trace("loadIntoMemory(): get Vlone failure: ", ex);
            n = 0;
        }

        long[] oid = { 0, 0 };
        rootObject = new H4Group(this, "/",
                null, // root object does not have a parent path
                null, // root object does not have a parent object
                oid);

        int i0 = Math.max(0, getStartMembers());
        int i1 = getMaxMembers();
        if (i1 >= n) {
            i1 = n;
            i0 = 0; // load all members
        }
        i1 += i0;
        i1 = Math.min(i1, n);

        // Iterate through the file to see members of the group
        log.trace("loadIntoMemory(): start={} to last={}", i0, i1);
        for (int i = i0; i < i1; i++) {
            ref = refs[i];
            log.trace("loadIntoMemory(): Iterate[{}] members of the group ref={}",i,ref);
            H4Group g = getVGroup(HDFConstants.DFTAG_VG, ref,
                    HObject.separator, (H4Group) rootObject, false);

            if (g != null) {
                ((H4Group) rootObject).addToMemberList(g);

                // recursively get the sub-tree
                depth_first(g);
            }
        } // for (int i=0; i<n; i++)

        // get the top level GR images
        argv = new int[2];
        boolean b = false;
        try {
            b = HDFLibrary.GRfileinfo(grid, argv);
        }
        catch (HDFException ex) {
            log.debug("loadIntoMemory(): GRfileinfo failure: ",ex);
            b = false;
        }

        if (b) {
            n = argv[0];

            for (int i = 0; i < n; i++) {
                // no duplicate object at top level
                H4GRImage gr = getGRImage(HDFConstants.DFTAG_RIG, i,
                        HObject.separator, false);
                if (gr != null) {
                    ((H4Group) rootObject).addToMemberList(gr);
                }
            } // for (int i=0; i<n; i++)
        } // if ( grid!=HDFConstants.FAIL && HDFLibrary.GRfileinfo(grid,argv) )

        // get top level SDS
        try {
            b = HDFLibrary.SDfileinfo(sdid, argv);
        }
        catch (HDFException ex) {
            log.debug("loadIntoMemory(): SDfileinfo failure: ",ex);
            b = false;
        }

        if (b) {
            n = argv[0];
            for (int i = 0; i < n; i++) {
                // no duplicate object at top level
                H4SDS sds = getSDS(HDFConstants.DFTAG_NDG, i,
                        HObject.separator, false);
                if (sds != null) {
                    ((H4Group) rootObject).addToMemberList(sds);
                }
            } // for (int i=0; i<n; i++)
        } // if (sdid != HDFConstants.FAIL && HDFLibrary.SDfileinfo(sdid, argv))

        // get top level VData
        try {
            n = HDFLibrary.VSlone(fid, tmpN, 0);
            log.trace("loadIntoMemory(): number of lone Vdatas={}", n);
            refs = new int[n];
            n = HDFLibrary.VSlone(fid, refs, n);
        }
        catch (HDFException ex) {
            log.debug("loadIntoMemory(): VSlone failure: ",ex);
            n = 0;
        }

        for (int i = 0; i < n; i++) {
            ref = refs[i];
            log.trace("loadIntoMemory(): references of Vdata[{}]={}", i, ref);

            // no duplicate object at top level
            H4Vdata vdata = getVdata(HDFConstants.DFTAG_VS, ref,
                    HObject.separator, false);

            if (vdata != null) {
                ((H4Group) rootObject).addToMemberList(vdata);
            }
        } // for (int i=0; i<n; i++)

        if (rootObject != null) {
            // retrieve file annotation, GR and SDS global attributes
            @SuppressWarnings("rawtypes")
            List attributeList = null;
            try {
                attributeList = ((H4Group) rootObject).getMetadata();
            }
            catch (HDFException ex) {
                log.debug("loadIntoMemory(): getMetadata failure: ", ex);
            }

            try {
                getFileAnnotation(fid, attributeList);
            }
            catch (HDFException ex) {
                log.debug("loadIntoMemory(): getFileAnnotation failure: ", ex);
            }
            try {
                getGRglobalAttribute(grid, attributeList);
            }
            catch (HDFException ex) {
                log.debug("loadIntoMemory(): getGRglobalAttribute failure: ", ex);
            }
            try {
                getSDSglobalAttribute(sdid, attributeList);
            }
            catch (HDFException ex) {
                log.debug("loadIntoMemory(): getSDglobalAttribute failure: ", ex);
            }
        }

        log.trace("loadIntoMemory(): finish");
    }

    /**
     * Retrieves the tree structure of the file by depth-first order. The
     * current implementation only retrieves groups and datasets. It does not
     * include named datatypes and soft links.
     *
     * @param parentObject
     *            the parent object.
     */
    private void depth_first(HObject parentObj) {
        log.trace("depth_first(pobj = {})", parentObj);

        if (parentObj == null) {
            log.debug("depth_first(): Parent object is null");
            log.trace("depth_first(): finish");
            return;
        }

        int nelems = 0, ref = -1, tag = -1, index = -1;
        int[] tags = null;
        int[] refs = null;

        H4Group parentGroup = (H4Group) parentObj;

        String fullPath = parentGroup.getPath() + parentGroup.getName() + HObject.separator;
        long gid = parentGroup.open();
        if (gid == HDFConstants.FAIL) {
            log.debug("depth_first(): Invalid Parent group ID");
            log.trace("depth_first(): finish");
            return;
        }

        try {
            nelems = HDFLibrary.Vntagrefs(gid);
            tags = new int[nelems];
            refs = new int[nelems];
            nelems = HDFLibrary.Vgettagrefs(gid, tags, refs, nelems);
        }
        catch (HDFException ex) {
            log.debug("depth_first(): failure: ", ex);
            nelems = 0;
        }
        finally {
            parentGroup.close(gid);
        }

        int i0 = Math.max(0, getStartMembers());
        int i1 = getMaxMembers();
        if (i1 >= nelems) {
            i1 = nelems;
            i0 = 0; // load all members
        }
        i1 += i0;
        i1 = Math.min(i1, nelems);

        // Iterate through the file to see members of the group
        for (int i = i0; i < i1; i++) {
            tag = tags[i];
            ref = refs[i];

            switch (tag) {
            case HDFConstants.DFTAG_RIG:
            case HDFConstants.DFTAG_RI:
            case HDFConstants.DFTAG_RI8:
                try {
                    index = HDFLibrary.GRreftoindex(grid, (short) ref);
                }
                catch (HDFException ex) {
                    index = HDFConstants.FAIL;
                }
                if (index != HDFConstants.FAIL) {
                    H4GRImage gr = getGRImage(tag, index, fullPath, true);
                    parentGroup.addToMemberList(gr);
                }
                break;
            case HDFConstants.DFTAG_SD:
            case HDFConstants.DFTAG_SDG:
            case HDFConstants.DFTAG_NDG:
                try {
                    index = HDFLibrary.SDreftoindex(sdid, ref);
                }
                catch (HDFException ex) {
                    index = HDFConstants.FAIL;
                }
                if (index != HDFConstants.FAIL) {
                    H4SDS sds = getSDS(tag, index, fullPath, true);
                    parentGroup.addToMemberList(sds);
                }
                break;
            case HDFConstants.DFTAG_VH:
            case HDFConstants.DFTAG_VS:
                H4Vdata vdata = getVdata(tag, ref, fullPath, true);
                parentGroup.addToMemberList(vdata);
                break;
            case HDFConstants.DFTAG_VG:
                H4Group vgroup = getVGroup(tag, ref, fullPath, parentGroup, true);
                parentGroup.addToMemberList(vgroup);
                if ((vgroup != null) && (parentGroup != null)) {
                    // check for loops
                    boolean looped = false;
                    H4Group theGroup = (H4Group) parentGroup;
                    while ((theGroup != null) && !looped) {
                        long[] oid = { tag, ref };
                        if (theGroup.equalsOID(oid)) {
                            looped = true;
                        }
                        else {
                            theGroup = (H4Group) theGroup.getParent();
                        }
                    }
                    if (!looped) {
                        depth_first(vgroup);
                    }
                }
                break;
            default:
                break;
            } // switch (tag)

        } // for (int i=0; i<nelms; i++)

        log.trace("depth_first(): finish");
    } // private depth_first()

    /**
     * Returns a list of all the members of this H4File in a
     * breadth-first ordering that are rooted at the specified
     * object.
     */
    private static List<HObject> getMembersBreadthFirst(HObject obj) {
        List<HObject> allMembers = new Vector<HObject>();
        Queue<HObject> queue = new LinkedList<HObject>();
        HObject currentObject = obj;

        queue.add(currentObject);

        while(!queue.isEmpty()) {
            currentObject = queue.remove();
            allMembers.add(currentObject);

            if(currentObject instanceof Group) {
                queue.addAll(((Group) currentObject).getMemberList());
            } else {
                continue;
            }
        }

        return allMembers;
    }

    /**
     * Retrieve a GR image for the given GR image identifier and index.
     *
     * @param tag
     *            the reference tag of the GR image.
     * @param index
     *            the index of the image.
     * @param path
     *            the path of the image.
     * @param copyAllowed
     *            The indicator if multiple copies of an object is allowed.
     *
     * @return the new H5GRImage if successful; otherwise returns null.
     */
    @SuppressWarnings("unchecked")
    private final H4GRImage getGRImage(int tag, int index, String path,
            boolean copyAllowed) {
        log.trace("getGRImage(): start: tag={} index={} path={} copyAllowed={}", tag, index, path, copyAllowed);

        long id = -1;
        int ref = -1;
        H4GRImage gr = null;
        String[] objName = { "" };
        int[] imgInfo = new int[4];
        int[] dim_sizes = { 0, 0 };
        // int tag = HDFConstants.DFTAG_RIG;

        try {
            id = HDFLibrary.GRselect(grid, index);
            ref = HDFLibrary.GRidtoref(id);
            log.trace("getGRImage(): GRselect:{} GRidtoref:{}",id,ref);
            HDFLibrary.GRgetiminfo(id, objName, imgInfo, dim_sizes);
        }
        catch (HDFException ex) {
            log.debug("getGRImage(): failure: ", ex);
            id = HDFConstants.FAIL;
        }
        finally {
            if (id >= 0) {
                try {
                    HDFLibrary.GRendaccess(id);
                }
                catch (HDFException ex) {
                    log.debug("getGRImage(): GRendaccess failure: ", ex);
                }
            }
        }

        if (id != HDFConstants.FAIL) {
            long oid[] = { tag, ref };

            if (copyAllowed) {
                objList.add(oid);
            }
            else if (find(oid)) {
                log.trace("getGRImage(): Image found in memory with OID:({}, {})", oid[0], oid[1]);
                log.trace("getGRImage(): finish");
                return null;
            }

            gr = new H4GRImage(this, objName[0], path, oid);
        }

        log.trace("getGRImage(): finish");
        return gr;
    }

    /**
     * Retrieve a SDS for the given sds identifier and index.
     *
     * @param tag
     *            the reference tag of the group (DFTAG_SD, DFTAG_SDG, DFTAG_NDG).
     * @param index
     *            the index of the SDS.
     * @param path
     *            the path of the SDS.
     * @param copyAllowed
     *            The indicator if multiple copies of an object is allowed.
     *
     * @return the new H4SDS if successful; otherwise returns null.
     */
    @SuppressWarnings("unchecked")
    private final H4SDS getSDS(int tag, int index, String path,
            boolean copyAllowed) {
        log.trace("getSDS(): start: tag={} index={} path={} copyAllowed={}", tag, index, path, copyAllowed);

        long id = -1;
        int ref = -1;
        H4SDS sds = null;
        String[] objName = { "" };
        int[] tmpInfo = new int[HDFConstants.MAX_VAR_DIMS];
        int[] sdInfo = { 0, 0, 0 };
        // int tag = HDFConstants.DFTAG_NDG;

        boolean isCoordvar = false;
        try {
            id = HDFLibrary.SDselect(sdid, index);
            if (isNetCDF) {
                ref = index; // HDFLibrary.SDidtoref(id) fails for netCDF
                tag = H4SDS.DFTAG_NDG_NETCDF;
            }
            else {
                ref = HDFLibrary.SDidtoref(id);
            }
            log.trace("getSDS(): SDselect id={} with ref={} isNetCDF={}", id, ref, isNetCDF);

            HDFLibrary.SDgetinfo(id, objName, tmpInfo, sdInfo);
            log.trace("getSDS(): SDselect id={} with objName={}: rank={}, numberType={}, nAttributes={}", id, objName, sdInfo[0], sdInfo[1], sdInfo[2]);

            try {
                isCoordvar = HDFLibrary.SDiscoordvar(id);
            }
            catch (Exception ex) {
                log.debug("getSDS(): SDiscoordvar failure: ", ex);
                isCoordvar = false;
            }
        }
        catch (HDFException ex) {
            log.debug("getSDS(): failure: ", ex);
            id = HDFConstants.FAIL;
        }
        finally {
            if (id >= 0) {
                try {
                    HDFLibrary.SDendaccess(id);
                }
                catch (HDFException ex) {
                    log.debug("getSDS(): SDendaccess failure: ", ex);
                }
            }
        }

        // check if the given SDS has dimension metadata
        // Coordinate variables are not displayed. They are created to store
        // metadata associated with dimensions. To ensure compatibility with
        // netCDF, coordinate variables are implemented as data sets

        if (isCoordvar) {
            objName[0] += " (dimension)";
        }

        if (id != HDFConstants.FAIL) { // && !isCoordvar)
            long oid[] = { tag, ref };

            if (copyAllowed) {
                objList.add(oid);
            }
            else if (find(oid)) {
                log.trace("getSDS(): SDS found in memory with OID:({}, {})", oid[0], oid[1]);
                log.trace("getSDS(): finish");
                return null;
            }

            sds = new H4SDS(this, objName[0], path, oid);
        }

        log.trace("getSDS(): finish");
        return sds;
    }

    /**
     * Retrieve a Vdata for the given Vdata identifier and index.
     *
     * @param tag
     *            the reference tag of the Vdata.
     * @param ref
     *            the reference identifier of the Vdata.
     * @param path
     *            the path of the Vdata.
     * @param copyAllowed
     *            The indicator if multiple copies of an object is allowed.
     *
     * @return the new H4Vdata if successful; otherwise returns null.
     */
    @SuppressWarnings("unchecked")
    private final H4Vdata getVdata(int tag, int ref, String path,
            boolean copyAllowed) {
        log.trace("getVdata(): start: tag={} ref={} path={} copyAllowed={}", tag, ref, path, copyAllowed);

        long id = -1;
        H4Vdata vdata = null;
        String[] objName = { "" };
        String[] vClass = { "" };
        // int tag = HDFConstants.DFTAG_VS;
        long oid[] = { tag, ref };

        if (copyAllowed) {
            objList.add(oid);
        }
        else if (find(oid)) {
            log.trace("getVdata(): VData found in memory with OID:({}, {})", oid[0], oid[1]);
            log.trace("getVdata(): finish");
            return null;
        }

        try {
            id = HDFLibrary.VSattach(fid, ref, "r");
            HDFLibrary.VSgetclass(id, vClass);
            vClass[0] = vClass[0].trim();
            HDFLibrary.VSgetname(id, objName);
        }
        catch (HDFException ex) {
            log.trace("getVData(): failure: ", ex);
            id = HDFConstants.FAIL;
        }
        finally {
            if (id >= 0) {
                try {
                    HDFLibrary.VSdetach(id);
                }
                catch (HDFException ex) {
                    log.debug("getVData(): VSdetach failure: ", ex);
                }
            }
        }

        if (showAll ||
                ((id != HDFConstants.FAIL)
                        && !vClass[0].equalsIgnoreCase(HDFConstants.HDF_ATTRIBUTE) // do not display Vdata named "Attr0.0" // commented out for bug 1737
                        && !vClass[0].startsWith(HDFConstants.HDF_CHK_TBL)         // do not display internal Vdata, "_HDF_CHK_TBL_"
                        && !vClass[0].startsWith(HDFConstants.HDF_SDSVAR)          // do not display attributes
                        && !vClass[0].startsWith(HDFConstants.HDF_CRDVAR)
                        && !vClass[0].startsWith(HDFConstants.DIM_VALS)
                        && !vClass[0].startsWith(HDFConstants.DIM_VALS01)
                        && !vClass[0].startsWith(HDFConstants.RIGATTRCLASS)
                        && !vClass[0].startsWith(HDFConstants.RIGATTRNAME)
                        && !vClass[0].equalsIgnoreCase(HDFConstants.HDF_CDF)))     // do not display internal vdata for CDF, "CDF0.0"
        {
            vdata = new H4Vdata(this, objName[0], path, oid);
        }

        log.trace("getVdata(): finish");
        return vdata;
    }

    /**
     * Retrieve a VGroup for the given VGroup identifier and index.
     *
     * @param tag
     *            the reference tag of the VGroup.
     * @param ref
     *            the reference identifier of the VGroup.
     * @param path
     *            the path of the VGroup.
     * @param pgroup
     *            the parent group.
     * @param copyAllowed
     *            The indicator if multiple copies of an object is allowed.
     *
     * @return the new H4VGroup if successful; otherwise returns null.
     */
    @SuppressWarnings("unchecked")
    private final H4Group getVGroup(int tag, int ref, String path,
            H4Group pgroup, boolean copyAllowed) {
        log.trace("getVGroup(): start: tag={}, ref={} path={} pgroup={} copyAllowed={}", tag, ref, path, pgroup, copyAllowed);

        long id = -1;
        H4Group vgroup = null;
        String[] objName = { "" };
        String[] vClass = { "" };
        // int tag = HDFConstants.DFTAG_VG;
        long oid[] = { tag, ref };

        if (ref <= 0) {
            log.trace("getVGroup(): Skipping dummy root group with ref={}", ref);
            log.trace("getVGroup(): finish");
            return null;
        }

        if (copyAllowed) {
            objList.add(oid);
        }
        else if (find(oid)) {
            log.trace("getVGroup(): VGroup found in memory with OID:({}, {})", oid[0], oid[1]);
            log.trace("getVGroup(): finish");
            return null;
        }

        try {
            id = HDFLibrary.Vattach(fid, ref, "r");
            log.trace("getVGroup(): Vattach fid={} id={}", fid, id);
            HDFLibrary.Vgetclass(id, vClass);
            vClass[0] = vClass[0].trim();
            HDFLibrary.Vgetname(id, objName);
        }
        catch (HDFException ex) {
            log.debug("getVGroup(): failure: ",ex);
            id = HDFConstants.FAIL;
        }
        finally {
            if (id >= 0) {
                try {
                    HDFLibrary.Vdetach(id);
                }
                catch (HDFException ex) {
                    log.debug("getVGroup(): Vdetach failure: ", ex);
                }
            }
        }

        // ignore the Vgroups created by the GR interface
        if (showAll || ((id != HDFConstants.FAIL)
                && !vClass[0].equalsIgnoreCase(HDFConstants.GR_NAME) // do not display Vdata named "Attr0.0"
                && !vClass[0].equalsIgnoreCase(HDFConstants.RI_NAME)
                && !vClass[0].equalsIgnoreCase(HDFConstants.RIGATTRNAME)
                && !vClass[0].equalsIgnoreCase(HDFConstants.RIGATTRCLASS)
                && !vClass[0].equalsIgnoreCase(HDFConstants.HDF_CDF)))
        {
            vgroup = new H4Group(this, objName[0], path, pgroup, oid);
        }

        log.trace("getVGroup(): finish");
        return vgroup;
    }

    /**
     * Check if object already exists in memory by matching the (tag, ref) pairs.
     */
    @SuppressWarnings("unchecked")
    private final boolean find(long[] oid) {
        log.trace("find(): start: oid({}, {})", oid[0], oid[1]);

        boolean existed = false;

        if (objList == null) {
            log.debug("find(): objList is null");
            log.trace("find(): finish");
            return false;
        }

        int n = objList.size();
        long[] theOID = null;

        for (int i = 0; i < n; i++) {
            theOID = (long[]) objList.get(i);
            if ((theOID[0] == oid[0]) && (theOID[1] == oid[1])) {
                log.trace("find(): matched object in objList");
                existed = true;
                break;
            }
        }

        if (!existed) {
            objList.add(oid);
        }

        log.trace("find(): finish");
        return existed;
    }

    /**
     * Returns the GR identifier, which is returned from GRstart(fid).
     *
     * @return the identifier.
     */
    long getGRAccessID() {
        return grid;
    }

    /**
     * Returns the SDS identifier, which is returned from SDstart(fname, flag).
     *
     * @return the identifier.
     */
    long getSDAccessID() {
        return sdid;
    }

    /**
     * Reads HDF file annotation (file labels and descriptions) into memory.
     * The file annotation is stored as an attribute of the root group.
     *
     * @param fid
     *            the file identifier.
     * @param attrList
     *            the list of attributes.
     *
     * @return the updated attribute list.
     *
     * @throws Exception if the annotation can not be read
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List getFileAnnotation(long fid, List attrList) throws HDFException {
        log.trace("getFileAnnotation(): start: FID={}", fid);

        if (fid < 0) {
            log.debug("getFileAnnotation(): Invalid FID");
            log.trace("getFileAnnotation(): finish");
            return attrList;
        }

        long anid = HDFConstants.FAIL;
        try {
            anid = HDFLibrary.ANstart(fid);
            // fileInfo[0] = n_file_label, fileInfo[1] = n_file_desc,
            // fileInfo[2] = n_data_label, fileInfo[3] = n_data_desc
            int[] fileInfo = new int[4];
            HDFLibrary.ANfileinfo(anid, fileInfo);

            if (fileInfo[0] + fileInfo[1] <= 0) {
                try {
                    HDFLibrary.ANend(anid);
                }
                catch (HDFException ex) {
                    log.debug("getFileAnnotation(): ANend failure: ", ex);
                }

                log.debug("getFileAnnotation(): n_file_labels + n_file_descriptions <= 0");
                log.trace("getFileAnnotation(): finish");
                return attrList;
            }

            if (attrList == null) {
                attrList = new Vector(fileInfo[0] + fileInfo[1], 5);
            }

            // load file labels and descriptions
            long id = -1;
            int[] annTypes = { HDFConstants.AN_FILE_LABEL,
                    HDFConstants.AN_FILE_DESC };
            for (int j = 0; j < 2; j++) {
                String annName = null;
                if (j == 0) {
                    annName = "File Label";
                }
                else {
                    annName = "File Description";
                }

                for (int i = 0; i < fileInfo[j]; i++) {
                    try {
                        id = HDFLibrary.ANselect(anid, i, annTypes[j]);
                    }
                    catch (HDFException ex) {
                        log.debug("getFileAnnotation(): ANselect failure: ", ex);
                        id = HDFConstants.FAIL;
                    }

                    if (id == HDFConstants.FAIL) {
                        log.trace("getFileAnnotation(): ANselect({}, {}, {}) failure", anid, i, annTypes[j]);
                        try {
                            HDFLibrary.ANendaccess(id);
                        }
                        catch (HDFException ex) {
                            log.debug("getFileAnnotation(): ANendaccess failure: ", ex);
                        }
                        continue;
                    }

                    int length = 0;
                    try {
                        length = HDFLibrary.ANannlen(id) + 1;
                    }
                    catch (HDFException ex) {
                        log.debug("getFileAnnotation(): ANannlen failure: ", ex);
                        length = 0;
                    }

                    if (length > 0) {
                        boolean b = false;
                        String str[] = { "" };
                        try {
                            b = HDFLibrary.ANreadann(id, str, length);
                        }
                        catch (HDFException ex) {
                            log.debug("getFileAnnotation(): ANreadann failure: ", ex);
                            b = false;
                        }

                        if (b && (str[0].length() > 0)) {
                            long attrDims[] = { str[0].length() };
                            Attribute newAttr = new Attribute(annName + " #" + i,
                                    new H4Datatype(HDFConstants.DFNT_CHAR), attrDims);
                            attrList.add(newAttr);
                            newAttr.setValue(str[0]);
                        }
                    }

                    try {
                        HDFLibrary.ANendaccess(id);
                    }
                    catch (HDFException ex) {
                        log.debug("getFileAnnotation(): ANendaccess failure: ", ex);
                    }
                } // for (int i=0; i < fileInfo[annTYpe]; i++)
            } // for (int annType=0; annType<2; annType++)
        }
        finally {
            if (anid >= 0) {
                try {
                    HDFLibrary.ANend(anid);
                }
                catch (HDFException ex) {
                    log.debug("getFileAnnotation(): ANend failure: ", ex);
                }
            }
        }

        log.trace("getFileAnnotation(): finish");
        return attrList;
    }

    /**
     * Reads GR global attributes into memory. The attributes are stored as
     * attributes of the root group.
     *
     * @param grid
     *            the GR identifier.
     * @param attrList
     *            the list of attributes.
     *
     * @return the updated attribute list.
     *
     * @throws HDFException if the GR attributes can not be read
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List getGRglobalAttribute(long grid, List attrList)
            throws HDFException {
        log.trace("getGRglobalAttribute(): start: GRID={}", grid);

        if (grid == HDFConstants.FAIL) {
            log.debug("getGRglobalAttribute(): Invalid GRID");
            log.trace("getGRglobalAttribute(): finish");
            return attrList;
        }

        int[] attrInfo = { 0, 0 };
        HDFLibrary.GRfileinfo(grid, attrInfo);
        int numberOfAttributes = attrInfo[1];

        if (numberOfAttributes > 0) {
            if (attrList == null) {
                attrList = new Vector(numberOfAttributes, 5);
            }

            String[] attrName = new String[1];
            for (int i = 0; i < numberOfAttributes; i++) {
                attrName[0] = "";
                boolean b = false;
                try {
                    b = HDFLibrary.GRattrinfo(grid, i, attrName, attrInfo);
                    // mask off the litend bit
                    attrInfo[0] = attrInfo[0] & (~HDFConstants.DFNT_LITEND);
                }
                catch (HDFException ex) {
                    log.debug("getGRglobalAttribute(): GRattrinfo failure: ", ex);
                    b = false;
                }

                if (!b) {
                    continue;
                }

                long[] attrDims = { attrInfo[1] };
                Attribute attr = new Attribute(attrName[0], new H4Datatype(attrInfo[0]), attrDims);
                attrList.add(attr);

                Object buf = H4Datatype.allocateArray(attrInfo[0], attrInfo[1]);
                try {
                    HDFLibrary.GRgetattr(grid, i, buf);
                }
                catch (HDFException ex) {
                    log.debug("getGRglobalAttribute(): GRgetattr failure: ", ex);
                    buf = null;
                }

                if (buf != null) {
                    if ((attrInfo[0] == HDFConstants.DFNT_CHAR)
                            || (attrInfo[0] == HDFConstants.DFNT_UCHAR8)) {
                        buf = Dataset.byteToString((byte[]) buf, attrInfo[1]);
                    }

                    attr.setValue(buf);
                }

            } // for (int i=0; i<numberOfAttributes; i++)
        } // if (b && numberOfAttributes>0)

        log.trace("getGRglobalAttribute(): finish");
        return attrList;
    }

    /**
     * Reads SDS global attributes into memory. The attributes are stored as
     * attributes of the root group.
     *
     * @param sdid
     *            the SD identifier.
     * @param attrList
     *            the list of attributes.
     *
     * @return the updated attribute list.
     *
     * @throws HDFException if the SDS attributes can not be read
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List getSDSglobalAttribute(long sdid, List attrList)
            throws HDFException {
        log.trace("getSDSglobalAttribute(): start: SDID:{}", sdid);

        if (sdid == HDFConstants.FAIL) {
            log.debug("getSDSglobalAttribute(): Invalid SDID");
            log.trace("getSDSglobalAttribute(): finish");
            return attrList;
        }

        int[] attrInfo = { 0, 0 };
        HDFLibrary.SDfileinfo(sdid, attrInfo);

        int numberOfAttributes = attrInfo[1];
        if (numberOfAttributes > 0) {
            if (attrList == null) {
                attrList = new Vector(numberOfAttributes, 5);
            }

            String[] attrName = new String[1];
            for (int i = 0; i < numberOfAttributes; i++) {
                attrName[0] = "";
                boolean b = false;
                try {
                    b = HDFLibrary.SDattrinfo(sdid, i, attrName, attrInfo);
                    // mask off the litend bit
                    attrInfo[0] = attrInfo[0] & (~HDFConstants.DFNT_LITEND);
                }
                catch (HDFException ex) {
                    log.debug("getSDSglobalAttribute(): SDattrinfo failure: ", ex);
                    b = false;
                }

                if (!b) {
                    continue;
                }

                long[] attrDims = { attrInfo[1] };
                Attribute attr = new Attribute(attrName[0], new H4Datatype(attrInfo[0]), attrDims);
                attrList.add(attr);

                Object buf = H4Datatype.allocateArray(attrInfo[0], attrInfo[1]);
                try {
                    HDFLibrary.SDreadattr(sdid, i, buf);
                }
                catch (HDFException ex) {
                    log.debug("getSDSglobalAttribute(): SDreadattr failure: ", ex);
                    buf = null;
                }

                if (buf != null) {
                    if ((attrInfo[0] == HDFConstants.DFNT_CHAR)
                            || (attrInfo[0] == HDFConstants.DFNT_UCHAR8)) {
                        buf = Dataset.byteToString((byte[]) buf, attrInfo[1]);
                    }

                    attr.setValue(buf);
                }

            } // for (int i=0; i<numberOfAttributes; i++)
        } // if (b && numberOfAttributes>0)

        log.trace("getSDSglobalAttribute(): finish");
        return attrList;
    }

    /**
     * Returns the version of the HDF4 library.
     */
    @Override
    public String getLibversion() {
        int[] vers = new int[3];
        String ver = "HDF ";
        String[] verStr = { "" };

        try {
            HDFLibrary.Hgetlibversion(vers, verStr);
        }
        catch (HDFException ex) {
            log.debug("getLibVersion(): Hgetlibversion failure: ", ex);
        }

        ver += vers[0] + "." + vers[1] + "." + vers[2];
        log.debug("getLibversion(): libversion is {}", ver);

        return ver;
    }

    /** HDF4 library supports netCDF version 2.3.2. It only supports SDS APIs. */
    private boolean isNetCDF(String filename) {
        log.trace("isNetCDF(): start: filename={}", filename);

        boolean isnetcdf = false;
        java.io.RandomAccessFile raf = null;

        try {
            raf = new java.io.RandomAccessFile(filename, "r");
        }
        catch (Exception ex) {
            log.debug("RandomAccessFile {}", filename, ex);
            try {
                raf.close();
            }
            catch (Exception ex2) {
                log.debug("RAF.close failure: ", ex2);
            }
            raf = null;
        }

        if (raf == null) {
            log.debug("isNetCDF(): raf is null");
            log.trace("isNetCDF(): finish");
            return false;
        }

        byte[] header = new byte[4];
        try {
            raf.read(header);
        }
        catch (Exception ex) {
            header = null;
        }

        if (header != null) {
            if (
                    // netCDF
                    ((header[0] == 67) && (header[1] == 68) && (header[2] == 70) && (header[3] == 1))) {
                isnetcdf = true;
            }
            else {
                isnetcdf = false;
            }
        }

        try {
            raf.close();
        }
        catch (Exception ex) {
            log.debug("RAF.close failure: ", ex);
        }

        log.trace("isNetCDF(): finish");
        return isnetcdf;
    }

    /**
     * Get an individual HObject with a given path. It does not load the whole
     * file structure.
     *
     * @param path the path of the object
     *
     * @throws Exception if the object cannot be found
     */
    @Override
    @SuppressWarnings("rawtypes")
    public HObject get(String path) throws Exception {
        log.trace("get(): start: path={}", path);

        if (objList == null) {
            objList = new Vector();
        }

        if ((path == null) || (path.length() <= 0)) {
            log.debug("get(): path is null or invalid path length");
            log.trace("get(): finish");
            return null;
        }

        path = path.replace('\\', '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String name = null, pPath = null;
        boolean isRoot = false;

        if (path.equals("/")) {
            name = "/"; // the root
            isRoot = true;
        }
        else {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 2);
            }
            int idx = path.lastIndexOf('/');
            name = path.substring(idx + 1);
            if (idx == 0) {
                pPath = "/";
            }
            else {
                pPath = path.substring(0, idx);
            }
        }

        log.trace("get(): isRoot={}", isRoot);

        HObject obj = null;
        isReadOnly = false;

        if (fid < 0) {
            fid = HDFLibrary.Hopen(fullFileName, HDFConstants.DFACC_WRITE);
            if (fid < 0) {
                isReadOnly = true;
                fid = HDFLibrary.Hopen(fullFileName, HDFConstants.DFACC_READ);
            }
            HDFLibrary.Vstart(fid);
            grid = HDFLibrary.GRstart(fid);
            sdid = HDFLibrary.SDstart(fullFileName, flag);
        }

        if (isRoot) {
            obj = getRootGroup();
        }
        else {
            obj = getAttachedObject(pPath, name);
        }

        log.trace("get(): finish");
        return obj;
    }

    /** Get the root group and all the alone objects */
    private H4Group getRootGroup() {
        log.trace("getRootGroup(): start");

        H4Group rootGroup = null;

        long[] oid = { 0, 0 };
        int n = 0, ref = -1;
        int[] argv = null;

        rootGroup = new H4Group(this, "/", null, null, oid);

        // get top level VGroup
        int[] tmpN = new int[1];
        int[] refs = null;
        try {
            // first call to get the number of lone Vgroups
            log.trace("getRootGroup(): first call to Vlone, get number of lone Vgroups");
            n = HDFLibrary.Vlone(fid, tmpN, 0);
            log.trace("getRootGroup(): number of lone Vgroups={}", n);
            refs = new int[n];
            // second call to get the references of all lone Vgroups
            log.trace("getRootGroup(): second call to Vlone, get references of lone Vgroups");
            n = HDFLibrary.Vlone(fid, refs, n);
        }
        catch (HDFException ex) {
            log.debug("getRootGroup(): Vlone failure: ", ex);
            n = 0;
        }

        // Iterate through the file to see members of the group
        for (int i = 0; i < n; i++) {
            ref = refs[i];
            H4Group g = getVGroup(HDFConstants.DFTAG_VG, ref,
                    HObject.separator, rootGroup, false);
            if (g != null) {
                rootGroup.addToMemberList(g);
            }
        } // for (int i=0; i<n; i++)

        // get the top level GR images
        argv = new int[2];
        boolean b = false;
        try {
            b = HDFLibrary.GRfileinfo(grid, argv);
        }
        catch (HDFException ex) {
            log.debug("getRootGroup(): GRfileinfo failure: ", ex);
            b = false;
        }

        if (b) {
            n = argv[0];
            for (int i = 0; i < n; i++) {
                // no duplicate object at top level
                H4GRImage gr = getGRImage(HDFConstants.DFTAG_RIG, i,
                        HObject.separator, false);
                if (gr != null) {
                    rootGroup.addToMemberList(gr);
                }
            } // for (int i=0; i<n; i++)
        } // if ( grid!=HDFConstants.FAIL && HDFLibrary.GRfileinfo(grid,argv) )

        // get top level SDS
        try {
            b = HDFLibrary.SDfileinfo(sdid, argv);
        }
        catch (HDFException ex) {
            log.debug("getRootGroup(): SDfileinfo failure: ", ex);
            b = false;
        }

        if (b) {
            n = argv[0];

            for (int i = 0; i < n; i++) {
                // no duplicate object at top level
                H4SDS sds = getSDS(HDFConstants.DFTAG_NDG, i,
                        HObject.separator, false);
                if (sds != null) {
                    rootGroup.addToMemberList(sds);
                }
            } // for (int i=0; i<n; i++)
        } // if (sdid != HDFConstants.FAIL && HDFLibrary.SDfileinfo(sdid, argv))

        // get top level VData
        try {
            log.trace("getRootGroup(): first call to VSlone, get number of lone VDatas");
            n = HDFLibrary.VSlone(fid, tmpN, 0);
            log.trace("getRootGroup(): number of lone Vdatas={}", n);
            refs = new int[n];
            log.trace("getRootGroup(): second call to VSlone, get references of lone VDatas");
            n = HDFLibrary.VSlone(fid, refs, n);
        }
        catch (HDFException ex) {
            log.debug("getRootGroup(): VSlone failure: ex");
            n = 0;
        }

        for (int i = 0; i < n; i++) {
            ref = refs[i];

            // no duplicate object at top level
            H4Vdata vdata = getVdata(HDFConstants.DFTAG_VS, ref,
                    HObject.separator, false);

            if (vdata != null) {
                rootGroup.addToMemberList(vdata);
            }
        } // for (int i=0; i<n; i++)

        if (rootGroup != null) {
            // retrieve file annotation, GR and SDS globle attributes
            @SuppressWarnings("rawtypes")
            List attributeList = null;
            try {
                attributeList = rootGroup.getMetadata();
            }
            catch (HDFException ex) {
                log.debug("getRootGroup(): getMetadata() failure: ", ex);
            }

            if (attributeList != null) {
                try {
                    getFileAnnotation(fid, attributeList);
                }
                catch (HDFException ex) {
                    log.debug("getRootGroup(): getFileAnnotation() failure: ", ex);
                }
                try {
                    getGRglobalAttribute(grid, attributeList);
                }
                catch (HDFException ex) {
                    log.debug("getRootGroup(): getGRglobalAttribute() failure: ", ex);
                }
                try {
                    getSDSglobalAttribute(sdid, attributeList);
                }
                catch (HDFException ex) {
                    log.debug("getRootGroup(): getSDSglobalAttribute() failure: ", ex);
                }
            }
        }

        log.trace("getRootGroup(): finish");
        return rootGroup;
    }

    /** Get the object attached to a vgroup */
    private HObject getAttachedObject(String path, String name) {
        log.trace("getAttachedObject(): start");

        if ((name == null) || (name.length() <= 0)) {
            log.debug("getAttachedObject(): name is null or invalid name length");
            log.trace("getAttachedObject(): finish");
            return null;
        }

        // get top level VGroup
        String[] objName = { "" };
        // check if it is an image
        int idx = -1;
        try {
            idx = HDFLibrary.GRnametoindex(grid, name);
        }
        catch (HDFException ex) {
            log.debug("getAttachedObject(): GRnametoindex failure: ", ex);
            idx = -1;
        }

        if (idx >= 0) {
            H4GRImage img = getGRImage(HDFConstants.DFTAG_RIG, idx, HObject.separator, false);
            log.trace("getAttachedObject(): finish");
            return img;
        }

        // get top level SDS
        try {
            idx = HDFLibrary.SDnametoindex(sdid, name);
        }
        catch (HDFException ex) {
            log.debug("getAttachedObject(): SDnametoindex failure: ", ex);
            idx = -1;
        }

        if (idx >= 0) {
            H4SDS sds = getSDS(HDFConstants.DFTAG_NDG, idx, HObject.separator, false);
            log.trace("getAttachedObject(): finish");
            return sds;
        } // if (sdid != HDFConstants.FAIL && HDFLibrary.SDfileinfo(sdid, argv))

        int ref = 0;
        try {
            ref = HDFLibrary.Vfind(fid, name);
        }
        catch (HDFException ex) {
            log.debug("getAttachedObject(): Vfind failure: ", ex);
            ref = -1;
        }

        if (ref > 0) {
            long oid[] = { HDFConstants.DFTAG_VG, ref };
            H4Group g = new H4Group(this, objName[0], path, null, oid);
            depth_first(g);
            log.trace("getAttachedObject(): finish");
            return g;
        }

        // get top level VData
        try {
            ref = HDFLibrary.VSfind(fid, name);
        }
        catch (HDFException ex) {
            log.debug("getAttachedObject(): VSfind failure: ", ex);
            ref = -1;
        }

        if (ref > 0) {
            H4Vdata vdata = getVdata(HDFConstants.DFTAG_VS, ref, HObject.separator, false);
            log.trace("getAttachedObject(): finish");
            return vdata;
        } // for (int i=0; i<n; i++)

        log.debug("getAttachedObject(): Object not found");
        log.trace("getAttachedObject(): finish");
        return null;
    }
}
