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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.netcdf3.N3header;

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
public class NC2File extends FileFormat {
    private static final long serialVersionUID = 6941235662108358451L;

    private static final Logger   log = LoggerFactory.getLogger(NC2File.class);

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

    /** the netcdf file */
    private NetcdfFile                      ncFile;

    private static boolean isFileOpen;

    /**
     * Constructs an empty NC2File with read-only access.
     */
    public NC2File() {
        this("");
    }

    /**
     * Creates an NC2File object of given file name with read-only access.
     *
     * @param fileName
     *            A valid file name, with a relative or absolute path.
     */
    public NC2File(String fileName) {
        super(fileName);

        isFileOpen = false;
        isReadOnly = true;
        objList = new Vector();
        ncFile = null;

        this.fid = -1;

        if ((fullFileName != null) && (fullFileName.length() > 0)) {
            try {
                log.trace("NetcdfFile:{}", fullFileName);
                ncFile = NetcdfFile.open(fullFileName);
                this.fid = 1;
            }
            catch (Exception ex) {
                log.trace("NC2File:{}", fullFileName, ex);
            }
        }
    }

    /**
     * Checks if the given file format is a NetCDF3 file.
     *
     * @param fileformat
     *            the fileformat to be checked.
     *
     * @return true if the given file is a NetCDF3 file; otherwise returns false.
     */
    @Override
    public boolean isThisType(FileFormat fileformat) {
        return (fileformat instanceof NC2File);
    }

    /**
     * Checks if the given file is a NetCDF file.
     *
     * @param filename
     *            the file to be checked.
     *
     * @return true if the given file is a NetCDF file; otherwise returns false.
     */
    @Override
    public boolean isThisType(String filename) {
        boolean isNetcdf = false;
        ucar.unidata.io.RandomAccessFile raf = null;

        try {
            raf = new ucar.unidata.io.RandomAccessFile(filename, "r");
        }
        catch (Exception ex) {
            log.trace("raf null - exit", ex);
            raf = null;
        }

        if (raf == null) {
            return false;
        }

        try {
            isNetcdf = N3header.isValidFile(raf);
        }
        catch (IOException e) {
            log.trace("raf isValidFile - failure", e);
            return false;
        }

        try {
            raf.close();
        }
        catch (Exception ex) {
            log.trace("raf close:", ex);
        }

        log.trace("{} - isNetcdf:{}", filename, isNetcdf);
        return isNetcdf;
    }

    /**
     * Creates a NC2File instance with specified file name and READ access.
     * Regardless of specified access, the NC2File implementation uses READ.
     *
     * @see hdf.object.FileFormat#createInstance(java.lang.String, int)
     */
    @Override
    public FileFormat createInstance(String filename, int access)
            throws Exception {
        return new NC2File(filename);
    }

    // Implementing FileFormat
    @Override
    public long open() throws Exception {
        log.trace("open(): start isFileOpen={}", isFileOpen);

        if (!isFileOpen) {
            isFileOpen = true;
            rootObject = loadTree();
        }

        return 0;
    }

    private HObject loadTree() {
        long[] oid = { 0 };
        // root object does not have a parent path or a parent node
        NC2Group rootGroup = new NC2Group(this, "/", null, null, oid);

        if (ncFile == null) {
            return rootGroup;
        }

        log.trace("loadTree(): iterate members");
        Iterator it = ncFile.getVariables().iterator();
        Variable ncDataset = null;
        NC2Dataset d = null;
        while (it.hasNext()) {
            ncDataset = (Variable) it.next();
            oid[0] = ncDataset.hashCode();
            d = new NC2Dataset(this, ncDataset, oid);
            rootGroup.addToMemberList(d);
        }

        return rootGroup;
    }

    // Implementing FileFormat
    @Override
    public void close() throws IOException {
        if (ncFile != null) {
            ncFile.close();
        }

        isFileOpen = false;
        fid = -1;
        objList = null;
    }

    // Implementing FileFormat
    @Override
    public HObject getRootObject() {
        return rootObject;
    }

    /**
     * @return the NetCDF file.
     */
    public NetcdfFile getNetcdfFile() {
        return ncFile;
    }

    @Override
    public Group createGroup(String name, Group pgroup) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation - create group.");
    }

    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign)
            throws Exception {
        throw new UnsupportedOperationException("Unsupported operation - create datatype.");
    }

    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder,
            int tsign, Datatype tbase) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation - create datatype.");
    }

    @Override
    public Datatype createNamedDatatype(Datatype tnative, String name) throws Exception {
        throw new UnsupportedOperationException("netcdf3 does not support named datatype.");
    }

    @Override
    public Dataset createScalarDS(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks,
            int gzip, Object fillValue, Object data) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation create dataset.");
    }

    @Override
    public Dataset createImage(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks,
            int gzip, int ncomp, int intelace, Object data) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation create image.");
    }

    @Override
    public void delete(HObject obj) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation.");
    }

    @Override
    public HObject copy(HObject srcObj, Group dstGroup, String dstName)
            throws Exception {
        throw new UnsupportedOperationException("Unsupported operation - copy.");
    }

    @Override
    public void writeAttribute(HObject obj, hdf.object.Attribute attr, boolean attrExisted) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation - write attribute.");
    }

    private HObject copyGroup(NC2Group srcGroup, NC2Group pgroup)
            throws Exception {
        throw new UnsupportedOperationException("Unsupported operation - copy group.");
    }

    private void copyDataset(Dataset srcDataset, NC2Group pgroup)
            throws Exception {
        throw new UnsupportedOperationException("Unsupported operation - copy dataset.");
    }

    /**
     * Copies the attributes of one object to another object.
     *
     * NC3 does not support attribute copy
     *
     * @param src
     *            The source object.
     * @param dst
     *            The destination object.
     */
    public void copyAttributes(HObject src, HObject dst) {
        throw new UnsupportedOperationException("Unsupported operation copy attributes with HObject.");
    }

    /**
     * Copies the attributes of one object to another object.
     *
     * NC3 does not support attribute copy
     *
     * @param srcID
     *            The source identifier.
     * @param dstID
     *            The destination identifier.
     */
    public void copyAttributes(int srcID, int dstID) {
        throw new UnsupportedOperationException("Unsupported operation - copy attributes.");
    }

    /**
     * converts a ucar.nc2.Attribute into an hdf.object.nc2.NC2Attribute
     *
     * @param parent
     *            the parent object.
     * @param netcdfAttr
     *            the ucar.nc2.Attribute object.
     *
     * @return the hdf.object.nc2.NC2Attribute if successful
     */
    public static hdf.object.nc2.NC2Attribute convertAttribute(HObject parent, ucar.nc2.Attribute netcdfAttr) {
        hdf.object.nc2.NC2Attribute ncsaAttr = null;

        if (netcdfAttr == null) {
            return null;
        }

        String attrName = netcdfAttr.getShortName();
        long[] attrDims = { netcdfAttr.getLength() };
        log.trace("convertAttribute(): attrName={} len={}", attrName, netcdfAttr.getLength());
        Datatype attrType = null;
        try {
            attrType = new NC2Datatype(netcdfAttr.getDataType());
        }
        catch (Exception ex) {
            attrType = null;
        }
        ncsaAttr = new hdf.object.nc2.NC2Attribute(parent, attrName, attrType, attrDims);
        Object[] attrValues = { netcdfAttr.getValue(0) };
        ncsaAttr.setData(attrValues);

        log.trace("convertAttribute(): finish data={}", netcdfAttr.getValue(0));
        return ncsaAttr;
    }

    /**
     * Retrieves the file structure from disk and returns the root object.
     *
     * First gets the top level objects or objects that do not belong to any
     * groups. If a top level object is a group, call the depth_first() to
     * retrieve the sub-tree of that group, recursively.
     */
    private void loadIntoMemory() {
        if (fid < 0) {
            log.debug("loadIntoMemory(): Invalid File Id");
            return;
        }
    }

    /**
     * Retrieves the tree structure of the file by depth-first order. The
     * current implementation only retrieves groups and datasets.
     *
     * @param parentObject
     *            the parent object.
     */
    private void depth_first(HObject parentObj) {
        log.trace("depth_first(pobj = {})", parentObj);

        if (parentObj == null) {
            log.debug("depth_first(): Parent object is null");
            return;
        }
    } // private depth_first()

    /**
     * Returns a list of all the members of this NetCDF3 in a
     * breadth-first ordering that are rooted at the specified
     * object.
     */
    private static List<HObject> getMembersBreadthFirst(HObject obj) {
        List<HObject> allMembers = new ArrayList<>();
        Queue<HObject> queue = new LinkedList<>();
        HObject currentObject = obj;

        queue.add(currentObject);

        while(!queue.isEmpty()) {
            currentObject = queue.remove();
            allMembers.add(currentObject);

            if(currentObject instanceof Group) {
                queue.addAll(((Group) currentObject).getMemberList());
            }
        }

        return allMembers;
    }

    /**
     * Returns the version of the library.
     */
    @Override
    public String getLibversion() {
        return "NetCDF Java (version 4.3)";
    }

    // implementing FileFormat
    @Override
    public HObject get(String path) throws Exception {
        throw new UnsupportedOperationException("get() is not supported");
    }
}
