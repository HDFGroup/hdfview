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

import java.io.File;
import java.lang.reflect.Array;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.hdf5lib.structs.H5L_info_t;
import hdf.hdf5lib.structs.H5O_info_t;
import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * H5File is an implementation of the FileFormat class for HDF5 files.
 * <p>
 * The HDF5 file structure is made up of HObjects stored in a tree-like fashion. Each tree node represents an
 * HDF5 object: a Group, Dataset, or Named Datatype. Starting from the root of the tree, <i>rootObject</i>, the
 * tree can be traversed to find a specific object.
 * <p>
 * The following example shows the implementation of finding an object for a given path in FileFormat. User applications
 * can directly call the static method FileFormat.findObject(file, objPath) to get the object.
 *
 * <pre>
 * HObject findObject(FileFormat file, String path) {
 *     if (file == null || path == null)
 *         return null;
 *     if (!path.endsWith(&quot;/&quot;))
 *         path = path + &quot;/&quot;;
 *     HObject theRoot = file.getRootObject();
 *     if (theRoot == null)
 *         return null;
 *     else if (path.equals(&quot;/&quot;))
 *         return theRoot;
 *
 *     Iterator local_it = ((Group) theRoot)
 *             .breadthFirstMemberList().iterator();
 *     HObject theObj = null;
 *     while (local_it.hasNext()) {
 *         theObj = local_it.next();
 *         String fullPath = theObj.getFullName() + &quot;/&quot;;
 *         if (path.equals(fullPath) &amp;&amp;  theObj.getPath() != null ) {
 *             break;
 *     }
 *     return theObj;
 * }
 * </pre>
 *
 * @author Peter X. Cao
 * @version 2.4 9/4/2007
 */
public class H5File extends FileFormat {
    private static final long serialVersionUID = 6247335559471526045L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5File.class);

    /**
     * the file access flag. Valid values are HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5F_ACC_RDWR and
     * HDF5Constants.H5F_ACC_CREAT.
     */
    private int flag;

    /**
     * The index type. Valid values are HDF5Constants.H5_INDEX_NAME, HDF5Constants.H5_INDEX_CRT_ORDER.
     */
    private int indexType = HDF5Constants.H5_INDEX_NAME;

    /**
     * The index order. Valid values are HDF5Constants.H5_ITER_INC, HDF5Constants.H5_ITER_DEC.
     */
    private int indexOrder = HDF5Constants.H5_ITER_INC;

    /**
     * The root object of the file hierarchy.
     */
    private HObject rootObject;

    /**
     * How many characters maximum in an attribute name?
     */
    private static final int attrNameLen = 256;

    /**
     * The library version bounds
     */
    private int[] libver;

    private boolean attrFlag;

    /***************************************************************************
     * Constructor
     **************************************************************************/
    /**
     * Constructs an H5File instance with an empty file name and read-only access.
     */
    public H5File() {
        this("", READ);
    }

    /**
     * Constructs an H5File instance with specified file name and read/write access.
     * <p>
     * This constructor does not open the file for access, nor does it confirm that the file can be opened read/write.
     *
     * @param fileName
     *            A valid file name, with a relative or absolute path.
     *
     * @throws NullPointerException
     *             If the <code>fileName</code> argument is <code>null</code>.
     */
    public H5File(String fileName) {
        this(fileName, WRITE);
    }

    /**
     * Constructs an H5File instance with specified file name and access.
     * <p>
     * The access parameter values and corresponding behaviors:
     * <ul>
     * <li>READ: Read-only access; open() will fail file doesn't exist.</li>
     * <li>WRITE: Read/Write access; open() will fail if file doesn't exist or if file can't be opened with read/write
     * access.</li>
     * <li>CREATE: Read/Write access; create a new file or truncate an existing one; open() will fail if file can't be
     * created or if file exists but can't be opened read/write.</li>
     * </ul>
     * <p>
     * This constructor does not open the file for access, nor does it confirm that the file can later be opened
     * read/write or created.
     * <p>
     * The flag returned by {@link #isReadOnly()} is set to true if the access parameter value is READ, even though the
     * file isn't yet open.
     *
     * @param fileName
     *            A valid file name, with a relative or absolute path.
     * @param access
     *            The file access flag, which determines behavior when file is opened. Acceptable values are
     *            <code> READ, WRITE, </code> and <code>CREATE</code>.
     *
     * @throws NullPointerException
     *             If the <code>fileName</code> argument is <code>null</code>.
     */
    public H5File(String fileName, int access) {
        // Call FileFormat ctor to set absolute path name
        super(fileName);
        libver = new int[2];
        attrFlag = false;

        // set metadata for the instance
        rootObject = null;
        this.fid = -1;
        isReadOnly = (access == READ);

        // At this point we just set up the flags for what happens later.
        // We just pass unexpected access values on... subclasses may have
        // their own values.
        if (access == READ) {
            flag = HDF5Constants.H5F_ACC_RDONLY;
        }
        else if (access == WRITE) {
            flag = HDF5Constants.H5F_ACC_RDWR;
        }
        else if (access == CREATE) {
            flag = HDF5Constants.H5F_ACC_CREAT;
        }
        else {
            flag = access;
        }
    }

    /***************************************************************************
     * Class methods
     **************************************************************************/

    /**
     * Copies the attributes of one object to another object.
     * <p>
     * This method copies all the attributes from one object (source object) to another (destination object). If an
     * attribute already exists in the destination object, the attribute will not be copied. Attribute names exceeding
     * 256 characters will be truncated in the destination object.
     * <p>
     * The object can be an H5Group, an H5Dataset, or a named H5Datatype. This method is in the H5File class because
     * there is no H5Object class and it is specific to HDF5 objects.
     * <p>
     * The copy can fail for a number of reasons, including an invalid source or destination object, but no exceptions
     * are thrown. The actual copy is carried out by the method: {@link #copyAttributes(long, long)}
     *
     * @param src
     *            The source object.
     * @param dst
     *            The destination object.
     *
     * @see #copyAttributes(long, long)
     */
    public static final void copyAttributes(HObject src, HObject dst) {
        if ((src != null) && (dst != null)) {
            long srcID = src.open();
            long dstID = dst.open();

            if ((srcID >= 0) && (dstID >= 0)) {
                copyAttributes(srcID, dstID);
            }

            if (srcID >= 0) {
                src.close(srcID);
            }

            if (dstID >= 0) {
                dst.close(dstID);
            }
        }
    }

    /**
     * Copies the attributes of one object to another object.
     * <p>
     * This method copies all the attributes from one object (source object) to another (destination object). If an
     * attribute already exists in the destination object, the attribute will not be copied. Attribute names exceeding
     * 256 characters will be truncated in the destination object.
     * <p>
     * The object can be an H5Group, an H5Dataset, or a named H5Datatype. This method is in the H5File class because
     * there is no H5Object class and it is specific to HDF5 objects.
     * <p>
     * The copy can fail for a number of reasons, including an invalid source or destination object identifier, but no
     * exceptions are thrown.
     *
     * @param src_id
     *            The identifier of the source object.
     * @param dst_id
     *            The identifier of the destination object.
     */
    public static final void copyAttributes(long src_id, long dst_id) {
        log.trace("copyAttributes(): start: src_id={} dst_id={}", src_id, dst_id);
        long aid_src = -1;
        long aid_dst = -1;
        long asid = -1;
        long atid = -1;
        String aName = null;
        H5O_info_t obj_info = null;

        try {
            obj_info = H5.H5Oget_info(src_id);
        }
        catch (Exception ex) {
            obj_info.num_attrs = -1;
        }

        if (obj_info.num_attrs < 0) {
            log.debug("copyAttributes(): no attributes");
            log.trace("copyAttributes(): finish");
            return;
        }

        for (int i = 0; i < obj_info.num_attrs; i++) {
            try {
                aid_src = H5.H5Aopen_by_idx(src_id, ".", HDF5Constants.H5_INDEX_CRT_ORDER, HDF5Constants.H5_ITER_INC,
                        i, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                aName = H5.H5Aget_name(aid_src);
                atid = H5.H5Aget_type(aid_src);
                asid = H5.H5Aget_space(aid_src);

                aid_dst = H5.H5Acreate(dst_id, aName, atid, asid, HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT);

                // use native data copy
                H5.H5Acopy(aid_src, aid_dst);

            }
            catch (Exception ex) {
                log.debug("copyAttributes(): Attribute[{}] failure: ", i, ex);
            }

            try {
                H5.H5Sclose(asid);
            }
            catch (Exception ex) {
                log.debug("copyAttributes(): Attribute[{}] H5Sclose(asid {}) failure: ", i, asid, ex);
            }
            try {
                H5.H5Tclose(atid);
            }
            catch (Exception ex) {
                log.debug("copyAttributes(): Attribute[{}] H5Tclose(atid {}) failure: ", i, atid, ex);
            }
            try {
                H5.H5Aclose(aid_src);
            }
            catch (Exception ex) {
                log.debug("copyAttributes(): Attribute[{}] H5Aclose(aid_src {}) failure: ", i, aid_src, ex);
            }
            try {
                H5.H5Aclose(aid_dst);
            }
            catch (Exception ex) {
                log.debug("copyAttributes(): Attribute[{}] H5Aclose(aid_dst {}) failure: ", i, aid_dst, ex);
            }

        } // for (int i=0; i<num_attr; i++)
    }

    /**
     * Returns a list of attributes for the specified object.
     * <p>
     * This method returns a list containing the attributes associated with the identified object. If there are no
     * associated attributes, an empty list will be returned.
     * <p>
     * Attribute names exceeding 256 characters will be truncated in the returned list.
     *
     * @param objID
     *            The identifier for the object whose attributes are to be returned.
     *
     * @return The list of the object's attributes.
     *
     * @throws HDF5Exception
     *             If an underlying HDF library routine is unable to perform a step necessary to retrieve the
     *             attributes. A variety of failures throw this exception.
     *
     * @see #getAttribute(long,int,int)
     */
    public static final List<Attribute> getAttribute(long objID) throws HDF5Exception {
        return H5File.getAttribute(objID, HDF5Constants.H5_INDEX_NAME, HDF5Constants.H5_ITER_INC);
    }

    /**
     * Returns a list of attributes for the specified object, in creation or alphabetical order.
     * <p>
     * This method returns a list containing the attributes associated with the identified object. If there are no
     * associated attributes, an empty list will be returned. The list of attributes returned can be in increasing or
     * decreasing, creation or alphabetical order.
     * <p>
     * Attribute names exceeding 256 characters will be truncated in the returned list.
     *
     * @param objID
     *            The identifier for the object whose attributes are to be returned.
     * @param idx_type
     *            The type of index. Valid values are:
     *            <ul>
     *            <li>H5_INDEX_NAME: An alpha-numeric index by attribute name <li>H5_INDEX_CRT_ORDER: An index by
     *            creation order
     *            </ul>
     * @param order
     *            The index traversal order. Valid values are:
     *            <ul>
     *            <li>H5_ITER_INC: A top-down iteration incrementing the index position at each step. <li>H5_ITER_DEC: A
     *            bottom-up iteration decrementing the index position at each step.
     *            </ul>
     *
     * @return The list of the object's attributes.
     *
     * @throws HDF5Exception
     *             If an underlying HDF library routine is unable to perform a step necessary to retrieve the
     *             attributes. A variety of failures throw this exception.
     */

    public static final List<Attribute> getAttribute(long objID, int idx_type, int order) throws HDF5Exception {
        log.trace("getAttribute(): start: objID={} idx_type={} order={}", objID, idx_type, order);
        List<Attribute> attributeList = null;
        long aid = -1;
        long sid = -1;
        long tid = -1;
        H5O_info_t obj_info = null;

        try {
            obj_info = H5.H5Oget_info(objID);
        }
        catch (Exception ex) {
            log.debug("getAttribute(): H5Oget_info(objID {}) failure: ", objID, ex);
        }
        if (obj_info.num_attrs <= 0) {
            log.debug("getAttribute(): no attributes");
            log.trace("getAttribute(): finish");
            return (attributeList = new Vector<Attribute>());
        }

        int n = (int) obj_info.num_attrs;
        attributeList = new Vector<Attribute>(n);
        log.trace("getAttribute(): num_attrs={}", n);

        for (int i = 0; i < n; i++) {
            long lsize = 1;
            log.trace("getAttribute(): attribute[{}]", i);

            try {
                aid = H5.H5Aopen_by_idx(objID, ".", idx_type, order, i, HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT);
                sid = H5.H5Aget_space(aid);

                long dims[] = null;
                int rank = H5.H5Sget_simple_extent_ndims(sid);

                if (rank > 0) {
                    dims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, null);
                    log.trace("getAttribute(): Attribute[{}] rank={}, dims={}", i, rank, dims);
                    for (int j = 0; j < dims.length; j++) {
                        lsize *= dims[j];
                    }
                }
                String nameA = H5.H5Aget_name(aid);
                log.trace("getAttribute(): Attribute[{}] is {}", i, nameA);

                long tmptid = -1;
                try {
                    tmptid = H5.H5Aget_type(aid);
                    tid = H5.H5Tget_native_type(tmptid);
                    log.trace("getAttribute(): Attribute[{}] tid={} native tmptid={} from aid={}", i, tid, tmptid, aid);
                }
                finally {
                    try {
                        H5.H5Tclose(tmptid);
                    }
                    catch (Exception ex) {
                        log.debug("getAttribute(): Attribute[{}] H5Tclose(tmptid {}) failure: ", i, tmptid, ex);
                    }
                }
                Datatype attrType = new H5Datatype(tid);
                Attribute attr = new Attribute(nameA, attrType, dims);
                attributeList.add(attr);
                log.trace("getAttribute(): Attribute[{}] Datatype={}", i, attrType.getDatatypeDescription());

                boolean is_variable_str = false;
                boolean isVLEN = false;
                boolean isCompound = false;
                boolean isScalar = false;
                int tclass = H5.H5Tget_class(tid);

                if (dims == null)
                    isScalar = true;
                try {
                    is_variable_str = H5.H5Tis_variable_str(tid);
                }
                catch (Exception ex) {
                    log.debug("getAttribute(): Attribute[{}] H5Tis_variable_str(tid {}) failure: ", i, tid, ex);
                }
                isVLEN = (tclass == HDF5Constants.H5T_VLEN);
                isCompound = (tclass == HDF5Constants.H5T_COMPOUND);
                log.trace(
                        "getAttribute(): Attribute[{}] has size={} isCompound={} isScalar={} is_variable_str={} isVLEN={}",
                        i, lsize, isCompound, isScalar, is_variable_str, isVLEN);

                // retrieve the attribute value
                if (lsize <= 0) {
                    log.debug("getAttribute(): Attribute[{}] lsize <= 0", i);
                    log.trace("getAttribute(): Attribute[{}] continue", i);
                    continue;
                }

                if (lsize < Integer.MIN_VALUE || lsize > Integer.MAX_VALUE) {
                    log.debug("getAttribute(): Attribute[{}] lsize outside valid Java int range; unsafe cast", i);
                    log.trace("getAttribute(): Attribute[{}] continue", i);
                    continue;
                }

                Object value = null;
                if (is_variable_str) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++) {
                        strs[j] = "";
                    }
                    try {
                        log.trace("getAttribute(): Attribute[{}] H5AreadVL", i);
                        H5.H5AreadVL(aid, tid, strs);
                    }
                    catch (Exception ex) {
                        log.debug("getAttribute(): Attribute[{}] H5AreadVL failure: ", i, ex);
                        ex.printStackTrace();
                    }
                    value = strs;
                }
                else if (isCompound || (isScalar && tclass == HDF5Constants.H5T_ARRAY)) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++) {
                        strs[j] = "";
                    }
                    try {
                        log.trace("getAttribute: attribute[{}] H5AreadComplex", i);
                        H5.H5AreadComplex(aid, tid, strs);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    value = strs;
                }
                else if (isVLEN) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++) {
                        strs[j] = "";
                    }
                    try {
                        log.trace("getAttribute(): Attribute[{}] H5AreadVL", i);
                        H5.H5AreadComplex(aid, tid, strs);
                    }
                    catch (Exception ex) {
                        log.debug("getAttribute(): Attribute[{}] H5AreadVL failure: ", i, ex);
                        ex.printStackTrace();
                    }
                    value = strs;
                }
                else {
                    value = H5Datatype.allocateArray(tid, (int) lsize);
                    if (value == null) {
                        log.debug("getAttribute(): Attribute[{}] allocateArray returned null", i);
                        log.trace("getAttribute(): Attribute[{}] continue", i);
                        continue;
                    }

                    if (tclass == HDF5Constants.H5T_ARRAY) {
                        long tmptid1 = -1, tmptid2 = -1;
                        try {
                            log.trace("getAttribute(): Attribute[{}] H5Aread ARRAY tid={}", i, tid);
                            H5.H5Aread(aid, tid, value);
                        }
                        catch (Exception ex) {
                            log.debug("getAttribute(): Attribute[{}] H5Aread failure: ", i, ex);
                            ex.printStackTrace();
                        }
                        finally {
                            try {
                                H5.H5Tclose(tmptid1);
                            }
                            catch (Exception ex) {
                                log.debug("getAttribute(): Attribute[{}] H5Tclose(tmptid {}) failure: ", i, tmptid1, ex);
                            }
                            try {
                                H5.H5Tclose(tmptid2);
                            }
                            catch (Exception ex) {
                                log.debug("getAttribute(): Attribute[{}] H5Tclose(tmptid {}) failure: ", i, tmptid2, ex);
                            }
                        }
                    }
                    else {
                        log.trace("getAttribute(): Attribute[{}] H5Aread", i);
                        H5.H5Aread(aid, tid, value);
                    }

                    if (tclass == HDF5Constants.H5T_STRING) {
                        log.trace("getAttribute(): Attribute[{}] byteToString", i);
                        value = Dataset.byteToString((byte[]) value, (int)H5.H5Tget_size(tid));
                    }
                    else if (tclass == HDF5Constants.H5T_REFERENCE) {
                        log.trace("getAttribute(): Attribute[{}] byteToLong", i);
                        value = HDFNativeData.byteToLong((byte[]) value);
                    }
                }

                attr.setValue(value);

            }
            catch (HDF5Exception ex) {
                log.debug("getAttribute(): Attribute[{}] inspection failure: ", i, ex);
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("getAttribute(): Attribute[{}] H5Tclose(tid {}) failure: ", i, tid, ex);
                }
                try {
                    H5.H5Sclose(sid);
                }
                catch (Exception ex) {
                    log.debug("getAttribute(): Attribute[{}] H5Sclose(sid {}) failure: ", i, sid, ex);
                }
                try {
                    H5.H5Aclose(aid);
                }
                catch (Exception ex) {
                    log.debug("getAttribute(): Attribute[{}] H5Aclose(aid {}) failure: ", i, aid, ex);
                }
            }
        } // for (int i=0; i<obj_info.num_attrs; i++)

        log.trace("getAttribute(): finish");
        return attributeList;
    }

    /**
     * Creates attributes for an HDF5 image dataset.
     * <p>
     * This method creates attributes for two common types of HDF5 images. It provides a way of adding multiple
     * attributes to an HDF5 image dataset with a single call. The {@link #writeAttribute(HObject, Attribute, boolean)}
     * method may be used to write image attributes that are not handled by this method.
     * <p>
     * For more information about HDF5 image attributes, see the <a
     * href="https://www.hdfgroup.org/HDF5/doc/ADGuide/ImageSpec.html"> HDF5 Image and Palette Specification</a>.
     * <p>
     * This method can be called to create attributes for 24-bit true color and indexed images. The
     * <code>selectionFlag</code> parameter controls whether this will be an indexed or true color image. If
     * <code>selectionFlag</code> is <code>-1</code>, this will be an indexed image. If the value is
     * <code>ScalarDS.INTERLACE_PIXEL</code> or <code>ScalarDS.INTERLACE_PLANE</code>, it will be a 24-bit true color
     * image with the indicated interlace mode.
     * <p>
     * <ul>
     * The created attribute descriptions, names, and values are:
     * <li>The image identifier: name="CLASS", value="IMAGE"
     * <li>The version of image: name="IMAGE_VERSION", value="1.2"
     * <li>The range of data values: name="IMAGE_MINMAXRANGE", value=[0, 255]
     * <li>The type of the image: name="IMAGE_SUBCLASS", value="IMAGE_TRUECOLOR" or "IMAGE_INDEXED"
     * <li>For IMAGE_TRUECOLOR, the interlace mode: name="INTERLACE_MODE", value="INTERLACE_PIXEL" or "INTERLACE_PLANE"
     * <li>For IMAGE_INDEXED, the palettes to use in viewing the image: name="PALETTE", value= 1-d array of references
     * to the palette datasets, with initial value of {-1}
     * </ul>
     * <p>
     * This method is in the H5File class rather than H5ScalarDS because images are typically thought of at the File
     * Format implementation level.
     *
     * @param dataset
     *            The image dataset the attributes are added to.
     * @param selectionFlag
     *            Selects the image type and, for 24-bit true color images, the interlace mode. Valid values are:
     *            <ul>
     *            <li>-1: Indexed Image. <li>ScalarDS.INTERLACE_PIXEL: True Color Image. The component values for a
     *            pixel are stored contiguously. <li>ScalarDS.INTERLACE_PLANE: True Color Image. Each component is
     *            stored in a separate plane.
     *            </ul>
     *
     * @throws Exception
     *             If there is a problem creating the attributes, or if the selectionFlag is invalid.
     */
    private static final void createImageAttributes(Dataset dataset, int selectionFlag) throws Exception {
        log.trace("createImageAttributes(): start: dataset={}", dataset.toString());
        String subclass = null;
        String interlaceMode = null;

        if (selectionFlag == ScalarDS.INTERLACE_PIXEL) {
            log.trace("createImageAttributes(): subclass IMAGE_TRUECOLOR selectionFlag INTERLACE_PIXEL");
            subclass = "IMAGE_TRUECOLOR";
            interlaceMode = "INTERLACE_PIXEL";
        }
        else if (selectionFlag == ScalarDS.INTERLACE_PLANE) {
            log.trace("createImageAttributes(): subclass IMAGE_TRUECOLOR selectionFlag INTERLACE_PLANE");
            subclass = "IMAGE_TRUECOLOR";
            interlaceMode = "INTERLACE_PLANE";
        }
        else if (selectionFlag == -1) {
            log.trace("createImageAttributes(): subclass IMAGE_INDEXED");
            subclass = "IMAGE_INDEXED";
        }
        else {
            log.debug("createImageAttributes(): invalid selectionFlag");
            log.trace("createImageAttributes(): finish");
            throw new HDF5Exception("The selectionFlag is invalid.");
        }

        String attrName = "CLASS";
        String[] classValue = { "IMAGE" };
        Datatype attrType = new H5Datatype(Datatype.CLASS_STRING, classValue[0].length() + 1, -1, -1);
        Attribute attr = new Attribute(attrName, attrType, null);
        attr.setValue(classValue);
        dataset.writeMetadata(attr);

        attrName = "IMAGE_VERSION";
        String[] versionValue = { "1.2" };
        attrType = new H5Datatype(Datatype.CLASS_STRING, versionValue[0].length() + 1, -1, -1);
        attr = new Attribute(attrName, attrType, null);
        attr.setValue(versionValue);
        dataset.writeMetadata(attr);

        long[] attrDims = { 2 };
        attrName = "IMAGE_MINMAXRANGE";
        byte[] attrValueInt = { 0, (byte) 255 };
        attrType = new H5Datatype(Datatype.CLASS_CHAR, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
        attr = new Attribute(attrName, attrType, attrDims);
        attr.setValue(attrValueInt);
        dataset.writeMetadata(attr);

        attrName = "IMAGE_SUBCLASS";
        String[] subclassValue = { subclass };
        attrType = new H5Datatype(Datatype.CLASS_STRING, subclassValue[0].length() + 1, -1, -1);
        attr = new Attribute(attrName, attrType, null);
        attr.setValue(subclassValue);
        dataset.writeMetadata(attr);

        if ((selectionFlag == ScalarDS.INTERLACE_PIXEL) || (selectionFlag == ScalarDS.INTERLACE_PLANE)) {
            attrName = "INTERLACE_MODE";
            String[] interlaceValue = { interlaceMode };
            attrType = new H5Datatype(Datatype.CLASS_STRING, interlaceValue[0].length() + 1, -1, -1);
            attr = new Attribute(attrName, attrType, null);
            attr.setValue(interlaceValue);
            dataset.writeMetadata(attr);
        }
        else {
            attrName = "PALETTE";
            long[] palRef = { 0 }; // set ref to null
            attrType = new H5Datatype(Datatype.CLASS_REFERENCE, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
            attr = new Attribute(attrName, attrType, null);
            attr.setValue(palRef);
            dataset.writeMetadata(attr);
        }
        log.trace("createImageAttributes(): finish");
    }

    /**
     * Updates values of scalar dataset object references in copied file.
     * <p>
     * This method has very specific functionality as documented below, and the user is advised to pay close attention
     * when dealing with files that contain references.
     * <p>
     * When a copy is made from one HDF file to another, object references and dataset region references are copied, but
     * the references in the destination file are not updated by the copy and are therefore invalid.
     * <p>
     * When an entire file is copied, this method updates the values of the object references and dataset region
     * references that are in scalar datasets in the destination file so that they point to the correct object(s) in the
     * destination file. The method does not update references that occur in objects other than scalar datasets.
     * <p>
     * In the current release, the updating of object references is not handled completely as it was not required by the
     * projects that funded development. There is no support for updates when the copy does not include the entire file.
     * Nor is there support for updating objects other than scalar datasets in full-file copies. This functionality will
     * be extended as funding becomes available or, possibly, when the underlying HDF library supports the reference
     * updates itself.
     *
     * @param srcFile
     *            The file that was copied.
     * @param dstFile
     *            The destination file where the object references will be updated.
     *
     * @throws Exception
     *             If there is a problem in the update process.
     */
    public static final void updateReferenceDataset(H5File srcFile, H5File dstFile) throws Exception {
        log.trace("updateReferenceDataset(): start");
        if ((srcFile == null) || (dstFile == null)) {
            log.debug("updateReferenceDataset(): srcFile or dstFile is null");
            log.trace("updateReferenceDataset(): finish");
            return;
        }

        HObject srcRoot = srcFile.getRootObject();
        HObject newRoot = dstFile.getRootObject();

        Iterator<HObject> srcIt = getMembersBreadthFirst(srcRoot).iterator();
        Iterator<HObject> newIt = getMembersBreadthFirst(newRoot).iterator();

        long did = -1;
        // build one-to-one table of between objects in
        // the source file and new file
        long tid = -1;
        HObject srcObj, newObj;
        Hashtable<String, long[]> oidMap = new Hashtable<String, long[]>();
        List<ScalarDS> refDatasets = new Vector<ScalarDS>();
        while (newIt.hasNext() && srcIt.hasNext()) {
            srcObj = srcIt.next();
            newObj = newIt.next();
            oidMap.put(String.valueOf((srcObj.getOID())[0]), newObj.getOID());
            did = -1;
            tid = -1;

            // for Scalar DataSets in destination, if there is an object
            // reference in the dataset, add it to the refDatasets list for
            // later updating.
            if (newObj instanceof ScalarDS) {
                ScalarDS sd = (ScalarDS) newObj;
                did = sd.open();
                if (did >= 0) {
                    try {
                        tid = H5.H5Dget_type(did);
                        if (H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_OBJ)) {
                            refDatasets.add(sd);
                        }
                    }
                    catch (Exception ex) {
                        log.debug("updateReferenceDataset(): ScalarDS reference failure: ", ex);
                    }
                    finally {
                        try {
                            H5.H5Tclose(tid);
                        }
                        catch (Exception ex) {
                            log.debug("updateReferenceDataset(): ScalarDS reference H5Tclose(tid {}) failure: ", tid, ex);
                        }
                    }
                }
                sd.close(did);
            } // if (newObj instanceof ScalarDS)
        }

        // Update the references in the scalar datasets in the dest file.
        H5ScalarDS d = null;
        long sid = -1;
        int size = 0;
        int rank = 0;
        int n = refDatasets.size();
        for (int i = 0; i < n; i++) {
            log.trace("updateReferenceDataset(): Update the references in the scalar datasets in the dest file");
            d = (H5ScalarDS) refDatasets.get(i);
            byte[] buf = null;
            long[] refs = null;

            try {
                did = d.open();
                if (did >= 0) {
                    tid = H5.H5Dget_type(did);
                    sid = H5.H5Dget_space(did);
                    rank = H5.H5Sget_simple_extent_ndims(sid);
                    size = 1;
                    if (rank > 0) {
                        long[] dims = new long[rank];
                        H5.H5Sget_simple_extent_dims(sid, dims, null);
                        log.trace("updateReferenceDataset(): rank={}, dims={}", rank, dims);
                        for (int j = 0; j < rank; j++) {
                            size *= (int) dims[j];
                        }
                        dims = null;
                    }

                    buf = new byte[size * 8];
                    H5.H5Dread(did, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf);

                    // update the ref values
                    refs = HDFNativeData.byteToLong(buf);
                    size = refs.length;
                    for (int j = 0; j < size; j++) {
                        long[] theOID = oidMap.get(String.valueOf(refs[j]));
                        if (theOID != null) {
                            refs[j] = theOID[0];
                        }
                    }

                    // write back to file
                    H5.H5Dwrite(did, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, refs);
                }
                else {
                    log.debug("updateReferenceDataset(): dest file dataset failed to open");
                }
            }
            catch (Exception ex) {
                log.debug("updateReferenceDataset(): Reference[{}] failure: ", i, ex);
                log.trace("updateReferenceDataset(): Reference[{}] continue", i);
                continue;
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("updateReferenceDataset(): H5ScalarDS reference[{}] H5Tclose(tid {}) failure: ", i, tid, ex);
                }
                try {
                    H5.H5Sclose(sid);
                }
                catch (Exception ex) {
                    log.debug("updateReferenceDataset(): H5ScalarDS reference[{}] H5Sclose(sid {}) failure: ", i, sid, ex);
                }
                try {
                    H5.H5Dclose(did);
                }
                catch (Exception ex) {
                    log.debug("updateReferenceDataset(): H5ScalarDS reference[{}] H5Dclose(did {}) failure: ", i, did, ex);
                }
            }

            refs = null;
            buf = null;
        } // for (int i=0; i<n; i++)
    }

    /***************************************************************************
     * Implementation Class methods. These methods are related to the implementing H5File class, but not to a particular
     * instance of the class. Since we can't override class methods (they can only be shadowed in Java), these are
     * instance methods.
     **************************************************************************/

    /**
     * Returns the version of the HDF5 library.
     *
     * @see hdf.object.FileFormat#getLibversion()
     */
    @Override
    public String getLibversion() {
        int[] vers = new int[3];
        String ver = "HDF5 ";

        try {
            H5.H5get_libversion(vers);
        }
        catch (Throwable ex) {
            ex.printStackTrace();
        }

        ver += vers[0] + "." + vers[1] + "." + vers[2];
        log.debug("getLibversion(): libversion is {}", ver);

        return ver;
    }

    /**
     * Checks if the specified FileFormat instance has the HDF5 format.
     *
     * @see hdf.object.FileFormat#isThisType(hdf.object.FileFormat)
     */
    @Override
    public boolean isThisType(FileFormat theFile) {
        return (theFile instanceof H5File);
    }

    /**
     * Checks if the specified file has the HDF5 format.
     *
     * @see hdf.object.FileFormat#isThisType(java.lang.String)
     */
    @Override
    public boolean isThisType(String filename) {
        boolean isH5 = false;

        try {
            isH5 = H5.H5Fis_hdf5(filename);
        }
        catch (HDF5Exception ex) {
            isH5 = false;
        }

        return isH5;
    }

    /**
     * Creates an HDF5 file with the specified name and returns a new H5File instance associated with the file.
     *
     * @throws Exception
     *             If the file cannot be created or if createFlag has unexpected value.
     *
     * @see hdf.object.FileFormat#createFile(java.lang.String, int)
     * @see #H5File(String, int)
     */
    @Override
    public FileFormat createFile(String filename, int createFlag) throws Exception {
        log.trace("createFile(): start: filename={} createFlag={}", filename, createFlag);
        // Flag if we need to create or truncate the file.
        Boolean doCreateFile = true;

        // Won't create or truncate if CREATE_OPEN specified and file exists
        if ((createFlag & FILE_CREATE_OPEN) == FILE_CREATE_OPEN) {
            File f = new File(filename);
            if (f.exists()) {
                doCreateFile = false;
            }
        }
        log.trace("createFile(): doCreateFile={}", doCreateFile);

        if (doCreateFile) {
            long fapl = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);

            if ((createFlag & FILE_CREATE_EARLY_LIB) != FILE_CREATE_EARLY_LIB) {
                H5.H5Pset_libver_bounds(fapl, HDF5Constants.H5F_LIBVER_LATEST, HDF5Constants.H5F_LIBVER_LATEST);
            }

            long fileid = H5.H5Fcreate(filename, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT, fapl);
            try {
                H5.H5Pclose(fapl);
                H5.H5Fclose(fileid);
            }
            catch (HDF5Exception ex) {
                log.debug("H5 file, {} failure: ", filename, ex);
            }
        }

        log.trace("createFile(): finish");
        return new H5File(filename, WRITE);
    }

    /**
     * Creates an H5File instance with specified file name and access.
     *
     * @see hdf.object.FileFormat#createInstance(java.lang.String, int)
     * @see #H5File(String, int)
     *
     * @throws Exception
     *            If there is a failure.
     */
    @Override
    public FileFormat createInstance(String filename, int access) throws Exception {
        return new H5File(filename, access);
    }

    /***************************************************************************
     * Instance Methods
     *
     * These methods are related to the H5File class and to particular instances of objects with this class type.
     **************************************************************************/

    /**
     * Opens file and returns a file identifier.
     *
     * @see hdf.object.FileFormat#open()
     */
    @Override
    public long open() throws Exception {
        return open(true);
    }

    /**
     * Opens file and returns a file identifier.
     *
     * @see hdf.object.FileFormat#open(int...)
     */
    @Override
    public long open(int... indexList) throws Exception {
        setIndexType(indexList[0]);
        setIndexOrder(indexList[1]);
        return open(true);
    }

    /**
     * Sets the bounds of library versions.
     *
     * @param low
     *            The earliest version of the library.
     * @param high
     *            The latest version of the library.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    public void setLibBounds(int low, int high) throws Exception {
        long fapl = HDF5Constants.H5P_DEFAULT;

        if (fid < 0)
            return;

        fapl = H5.H5Fget_access_plist(fid);

        try {
            if (low < 0)
                low = HDF5Constants.H5F_LIBVER_EARLIEST;

            if (high < 0)
                high = HDF5Constants.H5F_LIBVER_LATEST;

            H5.H5Pset_libver_bounds(fapl, low, high);
            H5.H5Pget_libver_bounds(fapl, libver);
        }
        finally {
            try {
                H5.H5Pclose(fapl);
            }
            catch (Exception e) {
                log.debug("setLibBounds(): libver bounds H5Pclose(fapl {}) failure: ", fapl, e);
            }
        }
    }

    /**
     * Gets the bounds of library versions.
     *
     * @return libver The earliest and latest version of the library.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    public int[] getLibBounds() throws Exception {
        return libver;
    }

    /**
     * Closes file associated with this H5File instance.
     *
     * @see hdf.object.FileFormat#close()
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    @Override
    public void close() throws HDF5Exception {
        log.trace("close(): start");
        if (fid < 0) {
            log.debug("close(): file {} is not open", fullFileName);
            log.trace("close(): finish");
            return;
        }
        // The current working directory may be changed at Dataset.read()
        // by System.setProperty("user.dir", newdir) to make it work for external
        // datasets. We need to set it back to the original current working
        // directory (when hdf-java application started) before the file
        // is closed/opened. Otherwise, relative path, e.g. "./test.h5" may
        // not work
        String rootPath = System.getProperty("hdfview.workdir");
        if (rootPath == null) {
            rootPath = System.getProperty("user.dir");
        }
        System.setProperty("user.dir", rootPath);//H5.H5Dchdir_ext(rootPath);

        // clean up unused objects
        if (rootObject != null) {
            HObject theObj = null;
            Iterator<HObject> it = getMembersBreadthFirst(rootObject).iterator();
            while (it.hasNext()) {
                theObj = it.next();

                if (theObj instanceof Dataset) {
                    log.trace("close(): clear Dataset {}", ((Dataset) theObj).toString());
                    ((Dataset) theObj).clear();
                }
                else if (theObj instanceof Group) {
                    log.trace("close(): clear Group {}", ((Group) theObj).toString());
                    ((Group) theObj).clear();
                }
            }
        }

        // Close all open objects associated with this file.
        try {
            int type = -1;
            long[] oids;
            long n = H5.H5Fget_obj_count(fid, HDF5Constants.H5F_OBJ_ALL);
            log.trace("close(): open objects={}", n);

            if (n > 0) {
                if (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE) throw new Exception("Invalid int size");

                oids = new long[(int)n];
                H5.H5Fget_obj_ids(fid, HDF5Constants.H5F_OBJ_ALL, n, oids);

                for (int i = 0; i < (int)n; i++) {
                    log.trace("close(): object[{}] id={}", i, oids[i]);
                    type = H5.H5Iget_type(oids[i]);

                    if (HDF5Constants.H5I_DATASET == type) {
                        try {
                            H5.H5Dclose(oids[i]);
                        }
                        catch (Exception ex2) {
                            log.debug("close(): Object[{}] H5Dclose(oids[{}] {}) failure: ", i, i, oids[i], ex2);
                        }
                    }
                    else if (HDF5Constants.H5I_GROUP == type) {
                        try {
                            H5.H5Gclose(oids[i]);
                        }
                        catch (Exception ex2) {
                            log.debug("close(): Object[{}] H5Gclose(oids[{}] {}) failure: ", i, i, oids[i], ex2);
                        }
                    }
                    else if (HDF5Constants.H5I_DATATYPE == type) {
                        try {
                            H5.H5Tclose(oids[i]);
                        }
                        catch (Exception ex2) {
                            log.debug("close(): Object[{}] H5Tclose(oids[{}] {}) failure: ", i, i, oids[i], ex2);
                        }
                    }
                    else if (HDF5Constants.H5I_ATTR == type) {
                        try {
                            H5.H5Aclose(oids[i]);
                        }
                        catch (Exception ex2) {
                            log.debug("close(): Object[{}] H5Aclose(oids[{}] {}) failure: ", i, i, oids[i], ex2);
                        }
                    }
                } // for (int i=0; i<n; i++)
            } // if ( n>0)
        }
        catch (Exception ex) {
            log.debug("close(): failure: ", ex);
        }

        try {
            H5.H5Fflush(fid, HDF5Constants.H5F_SCOPE_GLOBAL);
        }
        catch (Exception ex) {
            log.debug("close(): H5Fflush(fid {}) failure: ", fid, ex);
        }

        try {
            H5.H5Fclose(fid);
        }
        catch (Exception ex) {
            log.debug("close(): H5Fclose(fid {}) failure: ", fid, ex);
        }

        // Set fid to -1 but don't reset rootObject
        fid = -1;
        log.trace("close(): finish");
    }

    /**
     * Returns the root object of the open HDF5 File.
     *
     * @see hdf.object.FileFormat#getRootObject()
     */
    @Override
    public HObject getRootObject() {
        return rootObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#get(java.lang.String)
     */
    @Override
    public HObject get(String path) throws Exception {
        log.trace("get(): start");
        HObject obj = null;

        if ((path == null) || (path.length() <= 0)) {
            log.debug("get(): path is null or invalid path length");
            System.err.println("(path == null) || (path.length() <= 0)");
            log.trace("get(): finish");
            return null;
        }

        // replace the wrong slash and get rid of "//"
        path = path.replace('\\', '/');
        path = "/" + path;
        path = path.replaceAll("//", "/");

        // the whole file tree is loaded. find the object in the tree
        if (rootObject != null) {
            obj = findObject(this, path);
        }

        // found object in memory
        if (obj != null) {
            log.trace("get(): Found object in memory");
            log.trace("get(): finish");
            return obj;
        }

        // open only the requested object
        String name = null, pPath = null;
        if (path.equals("/")) {
            name = "/"; // the root
        }
        else {
            // separate the parent path and the object name
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
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

        // do not open the full tree structure, only the file handler
        long fid_before_open = fid;
        fid = open(false);
        if (fid < 0) {
            log.debug("get(): Invalid FID");
            log.trace("get(): finish");
            System.err.println("Could not open file handler");
            return null;
        }

        try {
            H5O_info_t info;
            int objType;
            long oid = H5.H5Oopen(fid, path, HDF5Constants.H5P_DEFAULT);

            if (oid >= 0) {
                info = H5.H5Oget_info(oid);
                objType = info.type;
                if (objType == HDF5Constants.H5O_TYPE_DATASET) {
                    long did = -1;
                    try {
                        did = H5.H5Dopen(fid, path, HDF5Constants.H5P_DEFAULT);
                        obj = getDataset(did, name, pPath);
                    }
                    finally {
                        try {
                            H5.H5Dclose(did);
                        }
                        catch (Exception ex) {
                            log.debug("get(): {} H5Dclose(did {}) failure: ", path, did, ex);
                        }
                    }
                }
                else if (objType == HDF5Constants.H5O_TYPE_GROUP) {
                    long gid = -1;
                    try {
                        gid = H5.H5Gopen(fid, path, HDF5Constants.H5P_DEFAULT);
                        H5Group pGroup = null;
                        if (pPath != null) {
                            pGroup = new H5Group(this, null, pPath, null);
                            obj = getGroup(gid, name, pGroup);
                            pGroup.addToMemberList(obj);
                        }
                        else {
                            obj = getGroup(gid, name, pGroup);
                        }
                    }
                    finally {
                        try {
                            H5.H5Gclose(gid);
                        }
                        catch (Exception ex) {
                            log.debug("get(): {} H5Gclose(gid {}) failure: ", path, gid, ex);
                        }
                    }
                }
                else if (objType == HDF5Constants.H5O_TYPE_NAMED_DATATYPE) {
                    obj = new H5Datatype(this, name, pPath);
                }
            }
            try {
                H5.H5Oclose(oid);
            }
            catch (Exception ex) {
                log.debug("get(): H5Oclose(oid {}) failure: ", oid, ex);
                ex.printStackTrace();
            }
        }
        catch (Exception ex) {
            log.debug("get(): Exception finding obj {}", path, ex);
            obj = null;
        }
        finally {
            if ((fid_before_open <= 0) && (obj == null)) {
                // close the fid that is not attached to any object
                try {
                    H5.H5Fclose(fid);
                }
                catch (Exception ex) {
                    log.debug("get(): {} H5Fclose(fid {}) failure: ", path, fid, ex);
                }
                fid = fid_before_open;
            }
        }

        return obj;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createDatatype(int, int, int, int, java.lang.String)
     */
    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign, String name) throws Exception {
        return createDatatype(tclass, tsize, torder, tsign, null, name);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createDatatype(int, int, int, int, Datatype, java.lang.String)
     */
    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign, Datatype tbase, String name)
            throws Exception {
        log.trace("createDatatype(): start: name={} class={} size={} order={} sign={}", name, tclass, tsize, torder, tsign);
        if (tbase != null) log.trace("createDatatype(): baseType is {}", tbase.getDatatypeDescription());

        long tid = -1;
        H5Datatype dtype = null;

        try {
            H5Datatype t = (H5Datatype) createDatatype(tclass, tsize, torder, tsign, tbase);
            if ((tid = t.toNative()) < 0) {
                log.debug("createDatatype(): toNative failure");
                log.trace("createDatatype(): finish");
                throw new Exception("toNative failed");
            }

            H5.H5Tcommit(fid, name, tid, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT,
                    HDF5Constants.H5P_DEFAULT);

            byte[] ref_buf = H5.H5Rcreate(fid, name, HDF5Constants.H5R_OBJECT, -1);
            long l = HDFNativeData.byteToLong(ref_buf, 0);

            long[] oid = new long[1];
            oid[0] = l; // save the object ID

            dtype = new H5Datatype(this, null, name);
        }
        finally {
            H5.H5Tclose(tid);
        }

        log.trace("createDatatype(): finish");
        return dtype;
    }

    /***************************************************************************
     * Methods related to Datatypes and HObjects in HDF5 Files. Strictly speaking, these methods aren't related to
     * H5File and the actions could be carried out through the H5Group, H5Datatype and H5*DS classes. But, in some cases
     * they allow a null input and expect the generated object to be of HDF5 type. So, we put them in the H5File class
     * so that we create the proper type of HObject... H5Group for example.
     *
     * Here again, if there could be Implementation Class methods we'd use those. But, since we can't override class
     * methods (they can only be shadowed in Java), these are instance methods.
     *
     **************************************************************************/

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createDatatype(int, int, int, int)
     */
    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign) throws Exception {
        log.trace("create datatype");
        return new H5Datatype(tclass, tsize, torder, tsign);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createDatatype(int, int, int, int, Datatype)
     */
    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign, Datatype tbase) throws Exception {
        log.trace("create datatype with base");
        return new H5Datatype(tclass, tsize, torder, tsign, tbase);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createScalarDS(java.lang.String, hdf.object.Group, hdf.object.Datatype,
     * long[], long[], long[], int, java.lang.Object)
     */
    @Override
    public Dataset createScalarDS(String name, Group pgroup, Datatype type, long[] dims, long[] maxdims, long[] chunks,
            int gzip, Object fillValue, Object data) throws Exception {
        log.trace("createScalarDS(): name={}", name);
        if (pgroup == null) {
            // create new dataset at the root group by default
            pgroup = (Group) get("/");
        }

        return H5ScalarDS.create(name, pgroup, type, dims, maxdims, chunks, gzip, fillValue, data);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createCompoundDS(java.lang.String, hdf.object.Group, long[], long[], long[],
     * int, java.lang.String[], hdf.object.Datatype[], int[], java.lang.Object)
     */
    @Override
    public Dataset createCompoundDS(String name, Group pgroup, long[] dims, long[] maxdims, long[] chunks, int gzip,
            String[] memberNames, Datatype[] memberDatatypes, int[] memberSizes, Object data) throws Exception {
        log.trace("createCompoundDS(): start: name={}", name);
        int nMembers = memberNames.length;
        int memberRanks[] = new int[nMembers];
        long memberDims[][] = new long[nMembers][1];
        Dataset ds = null;

        for (int i = 0; i < nMembers; i++) {
            memberRanks[i] = 1;
            if (memberSizes == null) {
                memberDims[i][0] = 1;
            }
            else {
                memberDims[i][0] = memberSizes[i];
            }
        }

        if (pgroup == null) {
            // create new dataset at the root group by default
            pgroup = (Group) get("/");
        }
        ds = H5CompoundDS.create(name, pgroup, dims, maxdims, chunks, gzip, memberNames, memberDatatypes, memberRanks,
                memberDims, data);

        log.trace("createCompoundDS(): finish");
        return ds;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createImage(java.lang.String, hdf.object.Group, hdf.object.Datatype,
     * long[], long[], long[], int, int, int, java.lang.Object)
     */
    @Override
    public Dataset createImage(String name, Group pgroup, Datatype type, long[] dims, long[] maxdims, long[] chunks,
            int gzip, int ncomp, int interlace, Object data) throws Exception {
        log.trace("createImage(): start: name={}", name);
        if (pgroup == null) { // create at the root group by default
            pgroup = (Group) get("/");
        }

        H5ScalarDS dataset = (H5ScalarDS)H5ScalarDS.create(name, pgroup, type, dims, maxdims, chunks, gzip, data);

        try {
            H5File.createImageAttributes(dataset, interlace);
            dataset.setIsImage(true);
        }
        catch (Exception ex) {
            log.debug("createImage(): {} createImageAttributtes failure: ", name, ex);
        }

        log.trace("createImage(): finish");
        return dataset;
    }

    /***
     * Creates a new group with specified name in existing group.
     *
     * @see hdf.object.FileFormat#createGroup(java.lang.String, hdf.object.Group)
     */
    @Override
    public Group createGroup(String name, Group pgroup) throws Exception {
        return this.createGroup(name, pgroup, HDF5Constants.H5P_DEFAULT);
    }

    /***
     * Creates a new group with specified name in existing group and with the group creation properties list, gplist.
     *
     * @see hdf.object.h5.H5Group#create(java.lang.String, hdf.object.Group, long...)
     *
     */
    public Group createGroup(String name, Group pgroup, long... gplist) throws Exception {
        // create new group at the root
        if (pgroup == null) {
            pgroup = (Group) this.get("/");
        }

        return H5Group.create(name, pgroup, gplist);
    }

    /***
     * Creates the group creation property list identifier, gcpl. This identifier is used when creating Groups.
     *
     * @see hdf.object.FileFormat#createGcpl(int, int, int)
     *
     */
    public long createGcpl(int creationorder, int maxcompact, int mindense) throws Exception {
        log.trace("createGcpl(): start");
        long gcpl = -1;
        try {
            gcpl = H5.H5Pcreate(HDF5Constants.H5P_GROUP_CREATE);
            if (gcpl >= 0) {
                // Set link creation order.
                if (creationorder == Group.CRT_ORDER_TRACKED) {
                    log.trace("createGcpl(): creation order ORDER_TRACKED");
                    H5.H5Pset_link_creation_order(gcpl, HDF5Constants.H5P_CRT_ORDER_TRACKED);
                }
                else if (creationorder == Group.CRT_ORDER_INDEXED) {
                    log.trace("createGcpl(): creation order ORDER_INDEXED");
                    H5.H5Pset_link_creation_order(gcpl, HDF5Constants.H5P_CRT_ORDER_TRACKED
                            + HDF5Constants.H5P_CRT_ORDER_INDEXED);
                }
                // Set link storage.
                H5.H5Pset_link_phase_change(gcpl, maxcompact, mindense);
            }
        }
        catch (Exception ex) {
            log.debug("createGcpl(): failure: ", ex);
            ex.printStackTrace();
        }

        log.trace("createGcpl(): finish");
        return gcpl;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createLink(hdf.object.Group, java.lang.String, hdf.object.HObject)
     */
    @Override
    public HObject createLink(Group parentGroup, String name, Object currentObj) throws Exception {
        if (currentObj instanceof HObject)
            return this.createLink(parentGroup, name, (HObject) currentObj, Group.LINK_TYPE_HARD);
        else if (currentObj instanceof String)
            return this.createLink(parentGroup, name, (String) currentObj, Group.LINK_TYPE_HARD);

        return null;
    }

    /**
     * Creates a link to an object in the open file.
     * <p>
     * If parentGroup is null, the new link is created in the root group.
     *
     * @param parentGroup
     *            The group where the link is created.
     * @param name
     *            The name of the link.
     * @param currentObj
     *            The existing object the new link will reference.
     * @param lType
     *            The type of link to be created. It can be a hard link, a soft link or an external link.
     *
     * @return The object pointed to by the new link if successful; otherwise returns null.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public HObject createLink(Group parentGroup, String name, HObject currentObj, int lType) throws Exception {
        log.trace("createLink(): start: name={}", name);
        HObject obj = null;
        int type = 0;
        String current_full_name = null, new_full_name = null, parent_path = null;

        if (currentObj == null) {
            log.debug("createLink(): Link target is null");
            log.trace("createLink(): finish");
            throw new HDF5Exception("The object pointed to by the link cannot be null.");
        }
        if ((parentGroup == null) || parentGroup.isRoot()) {
            parent_path = HObject.separator;
        }
        else {
            parent_path = parentGroup.getPath() + HObject.separator + parentGroup.getName() + HObject.separator;
        }

        new_full_name = parent_path + name;

        if (lType == Group.LINK_TYPE_HARD) {
            type = HDF5Constants.H5L_TYPE_HARD;
            log.trace("createLink(): type H5L_TYPE_HARD");
        }
        else if (lType == Group.LINK_TYPE_SOFT) {
            type = HDF5Constants.H5L_TYPE_SOFT;
            log.trace("createLink(): type H5L_TYPE_SOFT");
        }
        else if (lType == Group.LINK_TYPE_EXTERNAL) {
            type = HDF5Constants.H5L_TYPE_EXTERNAL;
            log.trace("createLink(): type H5L_TYPE_EXTERNAL");
        }

        if (H5.H5Lexists(fid, new_full_name, HDF5Constants.H5P_DEFAULT)) {
            H5.H5Ldelete(fid, new_full_name, HDF5Constants.H5P_DEFAULT);
        }

        if (type == HDF5Constants.H5L_TYPE_HARD) {
            if ((currentObj instanceof Group) && ((Group) currentObj).isRoot()) {
                log.debug("createLink(): cannot create link to root group");
                log.trace("createLink(): finish");
                throw new HDF5Exception("Cannot make a link to the root group.");
            }
            current_full_name = currentObj.getPath() + HObject.separator + currentObj.getName();

            H5.H5Lcreate_hard(fid, current_full_name, fid, new_full_name, HDF5Constants.H5P_DEFAULT,
                    HDF5Constants.H5P_DEFAULT);
        }

        else if (type == HDF5Constants.H5L_TYPE_SOFT) {
            H5.H5Lcreate_soft(currentObj.getFullName(), fid, new_full_name, HDF5Constants.H5P_DEFAULT,
                    HDF5Constants.H5P_DEFAULT);
        }

        else if (type == HDF5Constants.H5L_TYPE_EXTERNAL) {
            H5.H5Lcreate_external(currentObj.getFile(), currentObj.getFullName(), fid, new_full_name,
                    HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        }

        if (currentObj instanceof Group) {
            log.trace("createLink(): Link target is type H5Group");
            obj = new H5Group(this, name, parent_path, parentGroup);
        }
        else if (currentObj instanceof H5Datatype) {
            log.trace("createLink(): Link target is type H5Datatype");
            obj = new H5Datatype(this, name, parent_path);
        }
        else if (currentObj instanceof H5CompoundDS) {
            log.trace("createLink(): Link target is type H5CompoundDS");
            obj = new H5CompoundDS(this, name, parent_path);
        }
        else if (currentObj instanceof H5ScalarDS) {
            log.trace("createLink(): Link target is type H5ScalarDS");
            obj = new H5ScalarDS(this, name, parent_path);
        }

        log.trace("createLink(): finish");
        return obj;
    }

    /**
     * Creates a soft or external link to object in a file that does not exist at the time the link is created.
     *
     * @param parentGroup
     *            The group where the link is created.
     * @param name
     *            The name of the link.
     * @param currentObj
     *            The name of the object the new link will reference. The object doesn't have to exist.
     * @param lType
     *            The type of link to be created.
     *
     * @return The H5Link object pointed to by the new link if successful; otherwise returns null.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public HObject createLink(Group parentGroup, String name, String currentObj, int lType) throws Exception {
        log.trace("createLink(): start: name={}", name);
        HObject obj = null;
        int type = 0;
        String new_full_name = null, parent_path = null;

        if (currentObj == null) {
            log.debug("createLink(): Link target is null");
            log.trace("createLink(): finish");
            throw new HDF5Exception("The object pointed to by the link cannot be null.");
        }
        if ((parentGroup == null) || parentGroup.isRoot()) {
            parent_path = HObject.separator;
        }
        else {
            parent_path = parentGroup.getPath() + HObject.separator + parentGroup.getName() + HObject.separator;
        }

        new_full_name = parent_path + name;

        if (lType == Group.LINK_TYPE_HARD) {
            type = HDF5Constants.H5L_TYPE_HARD;
            log.trace("createLink(): type H5L_TYPE_HARD");
        }
        else if (lType == Group.LINK_TYPE_SOFT) {
            type = HDF5Constants.H5L_TYPE_SOFT;
            log.trace("createLink(): type H5L_TYPE_SOFT");
        }
        else if (lType == Group.LINK_TYPE_EXTERNAL) {
            type = HDF5Constants.H5L_TYPE_EXTERNAL;
            log.trace("createLink(): type H5L_TYPE_EXTERNAL");
        }

        if (H5.H5Lexists(fid, new_full_name, HDF5Constants.H5P_DEFAULT)) {
            H5.H5Ldelete(fid, new_full_name, HDF5Constants.H5P_DEFAULT);
        }

        if (type == HDF5Constants.H5L_TYPE_SOFT) {
            H5.H5Lcreate_soft(currentObj, fid, new_full_name, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        }

        else if (type == HDF5Constants.H5L_TYPE_EXTERNAL) {
            String fileName = null;
            String objectName = null;

            // separate the object name and the file name
            fileName = currentObj.substring(0, currentObj.lastIndexOf(FileFormat.FILE_OBJ_SEP));
            objectName = currentObj.substring(currentObj.indexOf(FileFormat.FILE_OBJ_SEP));
            objectName = objectName.substring(3);

            H5.H5Lcreate_external(fileName, objectName, fid, new_full_name, HDF5Constants.H5P_DEFAULT,
                    HDF5Constants.H5P_DEFAULT);
        }

        if (name.startsWith(HObject.separator)) {
            name = name.substring(1);
        }
        obj = new H5Link(this, name, parent_path);

        log.trace("createLink(): finish");
        return obj;
    }

    /**
     * reload the sub-tree structure from file.
     * <p>
     * reloadTree(Group g) is useful when the structure of the group in file is changed while the group structure in
     * memory is not changed.
     *
     * @param g
     *            the group where the structure is to be reloaded in memory
     */
    public void reloadTree(Group g) {
        if (fid < 0 || rootObject == null || g == null) {
            log.debug("reloadTree(): Invalid fid or null object");
            return;
        }

        depth_first(g, Integer.MIN_VALUE);
    }

    /*
     * (non-Javadoc) NOTE: Object references are copied but not updated by this method.
     *
     * @see hdf.object.FileFormat#copy(hdf.object.HObject, hdf.object.Group, java.lang.String)
     */
    @Override
    public HObject copy(HObject srcObj, Group dstGroup, String dstName) throws Exception {
        log.trace("copy(): start: srcObj={} dstGroup={} dstName={}", srcObj, dstGroup, dstName);
        if ((srcObj == null) || (dstGroup == null)) {
            log.debug("copy(): srcObj or dstGroup is null");
            log.trace("copy(): finish");
            return null;
        }

        if (dstName == null) {
            dstName = srcObj.getName();
        }

        List<HObject> members = dstGroup.getMemberList();
        int n = members.size();
        for (int i = 0; i < n; i++) {
            HObject obj = (HObject) members.get(i);
            String name = obj.getName();
            while (name.equals(dstName))
                dstName += "~copy";
        }

        HObject newObj = null;
        if (srcObj instanceof Dataset) {
            log.trace("copy(): srcObj instanceof Dataset");
            newObj = copyDataset((Dataset) srcObj, (H5Group) dstGroup, dstName);
        }
        else if (srcObj instanceof H5Group) {
            log.trace("copy(): srcObj instanceof H5Group");
            newObj = copyGroup((H5Group) srcObj, (H5Group) dstGroup, dstName);
        }
        else if (srcObj instanceof H5Datatype) {
            log.trace("copy(): srcObj instanceof H5Datatype");
            newObj = copyDatatype((H5Datatype) srcObj, (H5Group) dstGroup, dstName);
        }

        log.trace("copy(): finish");
        return newObj;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#delete(hdf.object.HObject)
     */
    @Override
    public void delete(HObject obj) throws Exception {
        if ((obj == null) || (fid < 0)) {
            log.debug("delete(): Invalid FID or object is null");
            return;
        }

        String name = obj.getPath() + obj.getName();

        H5.H5Ldelete(fid, name, HDF5Constants.H5P_DEFAULT);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#writeAttribute(hdf.object.HObject, hdf.object.Attribute, boolean)
     */
    @Override
    public void writeAttribute(HObject obj, Attribute attr, boolean attrExisted) throws HDF5Exception {
        log.trace("writeAttribute(): start");

        String obj_name = obj.getFullName();
        String name = attr.getName();
        long tid = -1;
        long sid = -1;
        long aid = -1;
        log.trace("writeAttribute(): name is {}", name);

        long objID = obj.open();
        if (objID < 0) {
            log.debug("writeAttribute(): Invalid Object ID");
            log.trace("writeAttribute(): finish");
            return;
        }

        if ((tid = attr.getType().toNative()) >= 0) {
            log.trace("writeAttribute(): tid {} from toNative", tid);
            try {
                if (attr.isScalar())
                    sid = H5.H5Screate(HDF5Constants.H5S_SCALAR);
                else
                    sid = H5.H5Screate_simple(attr.getRank(), attr.getDataDims(), null);

                if (attrExisted) {
                    aid = H5.H5Aopen_by_name(objID, obj_name, name, HDF5Constants.H5P_DEFAULT,
                            HDF5Constants.H5P_DEFAULT);
                }
                else {
                    aid = H5.H5Acreate(objID, name, tid, sid, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                }
                log.trace("writeAttribute(): aid {} opened/created", aid);

                // update value of the attribute
                Object attrValue = attr.getValue();
                log.trace("writeAttribute(): getValue");
                if (attrValue != null) {
                    boolean isVlen = (H5.H5Tget_class(tid) == HDF5Constants.H5T_VLEN || H5.H5Tis_variable_str(tid));
                    if (isVlen) {
                        log.trace("writeAttribute(): isvlen={}", isVlen);
                        try {
                            /*
                             * must use native type to write attribute data to file (see bug 1069)
                             */
                            long tmptid = tid;
                            tid = H5.H5Tget_native_type(tmptid);
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (Exception ex) {
                                log.debug("writeAttribute(): H5Tclose(tmptid {}) failure: ", tmptid, ex);
                            }
                            log.trace("writeAttribute(): H5.H5AwriteVL", name);
                            if ((attrValue instanceof String) || (attr.getDataDims().length == 1)) {
                                H5.H5AwriteVL(aid, tid, (String[]) attrValue);
                            }
                            else {
                                log.info("writeAttribute(): Datatype is not a string, unable to write {} data", name);
                            }
                        }
                        catch (Exception ex) {
                            log.debug("writeAttribute(): native type failure: ", name, ex);
                        }
                    }
                    else {
                        if (attr.getType().getDatatypeClass() == Datatype.CLASS_REFERENCE && attrValue instanceof String) {
                            // reference is a path+name to the object
                            attrValue = H5.H5Rcreate(getFID(), (String) attrValue, HDF5Constants.H5R_OBJECT, -1);
                            log.trace("writeAttribute(): Attribute class is CLASS_REFERENCE");
                        }
                        else if (Array.get(attrValue, 0) instanceof String) {
                            long size = H5.H5Tget_size(tid);
                            int len = ((String[]) attrValue).length;
                            byte[] bval = Dataset.stringToByte((String[]) attrValue, (int)size);
                            if (bval != null && bval.length == (int)size * len) {
                                bval[bval.length - 1] = 0;
                                attrValue = bval;
                            }
                            log.trace("writeAttribute(): Array", name);
                        }

                        try {
                            /*
                             * must use native type to write attribute data to file (see bug 1069)
                             */
                            long tmptid = tid;
                            tid = H5.H5Tget_native_type(tmptid);
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (Exception ex) {
                                log.debug("writeAttribute(): H5Tclose(tmptid {}) failure: ", tmptid, ex);
                            }
                            log.trace("writeAttribute(): H5.H5Awrite");
                            H5.H5Awrite(aid, tid, attrValue);
                        }
                        catch (Exception ex) {
                            log.debug("writeAttribute(): native type failure: ", ex);
                        }
                    }
                } // if (attrValue != null) {
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("writeAttribute(): H5Tclose(tid {}) failure: ", tid, ex);
                }
                try {
                    H5.H5Sclose(sid);
                }
                catch (Exception ex) {
                    log.debug("writeAttribute(): H5Sclose(sid {}) failure: ", sid, ex);
                }
                try {
                    H5.H5Aclose(aid);
                }
                catch (Exception ex) {
                    log.debug("writeAttribute(): H5Aclose(aid {}) failure: ", aid, ex);
                }
            }
        }
        else {
            log.debug("writeAttribute(): toNative failure");
        }

        obj.close(objID);
        log.trace("writeAttribute(): finish");
    }

    /***************************************************************************
     * Implementations for methods specific to H5File
     **************************************************************************/

    /**
     * Opens a file with specific file access property list.
     * <p>
     * This function does the same as "long open()" except the you can also pass an HDF5 file access property to file
     * open. For example,
     *
     * <pre>
     * // All open objects remaining in the file are closed then file is closed
     * long plist = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
     * H5.H5Pset_fclose_degree(plist, HDF5Constants.H5F_CLOSE_STRONG);
     * long fid = open(plist);
     * </pre>
     *
     * @param plist
     *            a file access property list identifier.
     *
     * @return the file identifier if successful; otherwise returns negative value.
     *
     * @throws Exception
     *            If there is a failure.
     */
    public long open(long plist) throws Exception {
        return open(true, plist);
    }

    /***************************************************************************
     * Private methods.
     **************************************************************************/

    /**
     * Opens access to this file.
     *
     * @param loadFullHierarchy
     *            if true, load the full hierarchy into memory; otherwise just opens the file identifier.
     *
     * @return the file identifier if successful; otherwise returns negative value.
     *
     * @throws Exception
     *            If there is a failure.
     */
    private long open(boolean loadFullHierarchy) throws Exception {
        long the_fid = -1;

        long plist = HDF5Constants.H5P_DEFAULT;

        /*
         * // BUG: HDF5Constants.H5F_CLOSE_STRONG does not flush cache try { //All open objects remaining in the file
         * are closed // then file is closed plist = H5.H5Pcreate (HDF5Constants.H5P_FILE_ACCESS);
         * H5.H5Pset_fclose_degree ( plist, HDF5Constants.H5F_CLOSE_STRONG); } catch (Exception ex) {;} the_fid =
         * open(loadFullHierarchy, plist); try { H5.H5Pclose(plist); } catch (Exception ex) {}
         */

        log.trace("open(): loadFull={}", loadFullHierarchy);
        the_fid = open(loadFullHierarchy, plist);

        return the_fid;
    }

    /**
     * Opens access to this file.
     *
     * @param loadFullHierarchy
     *            if true, load the full hierarchy into memory; otherwise just opens the file identifier.
     *
     * @return the file identifier if successful; otherwise returns negative value.
     *
     * @throws Exception
     *            If there is a failure.
     */
    private long open(boolean loadFullHierarchy, long plist) throws Exception {
        log.trace("open(loadFullHierarchy = {}, plist = {}): start", loadFullHierarchy, plist);
        if (fid > 0) {
            log.trace("open(): FID already opened");
            log.trace("open(): finish");
            return fid; // file is opened already
        }

        // The cwd may be changed at Dataset.read() by System.setProperty("user.dir", newdir)
        // to make it work for external datasets. We need to set it back
        // before the file is closed/opened.
        String rootPath = System.getProperty("hdfview.workdir");
        if (rootPath == null) {
            rootPath = System.getProperty("user.dir");
        }
        System.setProperty("user.dir", rootPath);//H5.H5Dchdir_ext(rootPath);

        // check for valid file access permission
        if (flag < 0) {
            log.debug("open(): Invalid access identifier -- " + flag);
            log.trace("open(): finish");
            throw new HDF5Exception("Invalid access identifer -- " + flag);
        }
        else if (HDF5Constants.H5F_ACC_CREAT == flag) {
            // create a new file
            log.trace("open(): create file");
            fid = H5.H5Fcreate(fullFileName, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT,
                    HDF5Constants.H5P_DEFAULT);
            H5.H5Fflush(fid, HDF5Constants.H5F_SCOPE_LOCAL);
            H5.H5Fclose(fid);
            flag = HDF5Constants.H5F_ACC_RDWR;
        }
        else if (!exists()) {
            log.debug("open(): File {} does not exist", fullFileName);
            log.trace("open(): finish");
            throw new HDF5Exception("File does not exist -- " + fullFileName);
        }
        else if (((flag == HDF5Constants.H5F_ACC_RDWR) || (flag == HDF5Constants.H5F_ACC_CREAT)) && !canWrite()) {
            log.debug("open(): Cannot write file {}", fullFileName);
            log.trace("open(): finish");
            throw new HDF5Exception("Cannot write file, try opening as read-only -- " + fullFileName);
        }
        else if ((flag == HDF5Constants.H5F_ACC_RDONLY) && !canRead()) {
            log.debug("open(): Cannot read file {}", fullFileName);
            log.trace("open(): finish");
            throw new HDF5Exception("Cannot read file -- " + fullFileName);
        }

        try {
            log.trace("open(): open file");
            fid = H5.H5Fopen(fullFileName, flag, plist);
        }
        catch (Exception ex) {
            try {
                log.debug("open(): open failed, attempting to open file read-only");
                fid = H5.H5Fopen(fullFileName, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);
                isReadOnly = true;
            }
            catch (Exception ex2) {
                // Attemp to open the file as a split file or family file
                try {
                    File tmpf = new File(fullFileName);
                    String tmpname = tmpf.getName();
                    int idx = tmpname.lastIndexOf(".");

                    if (tmpname.contains("-m")) {
                        log.debug("open(): open read-only failed, attempting to open split file");

                        while (idx > 0) {
                            char c = tmpname.charAt(idx - 1);
                            if (!(c == '-'))
                                idx--;
                            else
                                break;
                        }

                        if (idx > 0) {
                            tmpname = tmpname.substring(0, idx - 1);
                            log.trace("open(): attempting to open split file with name {}", tmpname);
                            long pid = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
                            H5.H5Pset_fapl_split(pid, "-m.h5", HDF5Constants.H5P_DEFAULT, "-r.h5", HDF5Constants.H5P_DEFAULT);
                            fid = H5.H5Fopen(tmpf.getParent() + File.separator + tmpname, flag, pid);
                            H5.H5Pclose(pid);
                        }
                    }
                    else {
                        log.debug("open(): open read-only failed, checking for file family");
                        // try to see if it is a file family, always open a family file
                        // from the first one since other files will not be recognized
                        // as an HDF5 file
                        int cnt = idx;
                        while (idx > 0) {
                            char c = tmpname.charAt(idx - 1);
                            if (Character.isDigit(c))
                                idx--;
                            else
                                break;
                        }

                        if (idx > 0) {
                            cnt -= idx;
                            tmpname = tmpname.substring(0, idx) + "%0" + cnt + "d" + tmpname.substring(tmpname.lastIndexOf("."));
                            log.trace("open(): attempting to open file family with name {}", tmpname);
                            long pid = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
                            H5.H5Pset_fapl_family(pid, 0, HDF5Constants.H5P_DEFAULT);
                            fid = H5.H5Fopen(tmpf.getParent() + File.separator + tmpname, flag, pid);
                            H5.H5Pclose(pid);
                        }
                    }
                } catch (Exception ex3) {
                    log.debug("open(): open failed: ", ex3);
                }
            } /* catch (Exception ex) { */
        }

        if ((fid >= 0) && loadFullHierarchy) {
            // load the hierarchy of the file
            loadIntoMemory();
        }

        log.trace("open(loadFullHeirarchy = {}, plist = {}): finish", loadFullHierarchy, plist);
        return fid;
    }

    /**
     * Loads the file structure into memory.
     */
    private void loadIntoMemory() {
        if (fid < 0) {
            log.debug("loadIntoMemory(): Invalid FID");
            return;
        }

        rootObject = new H5Group(this, "/", null, null);
        depth_first(rootObject, 0);
    }

    /**
     * Retrieves the file structure by depth-first order, recursively. The current implementation retrieves groups and
     * datasets only. It does not include named datatypes and soft links.
     * <p>
     * It also detects and stops loops. A loop is detected if there exists an object with the same object ID by tracing
     * a path back up to the root.
     *
     * @param parentObject
     *            the parent object.
     */
    private int depth_first(HObject parentObject, int nTotal) {
        log.trace("depth_first({}): start", parentObject);

        int nelems;
        String fullPath = null;
        String ppath = null;
        long gid = -1;

        H5Group pgroup = (H5Group) parentObject;
        ppath = pgroup.getPath();

        if (ppath == null) {
            fullPath = HObject.separator;
        }
        else {
            fullPath = ppath + pgroup.getName() + HObject.separator;
        }

        nelems = 0;
        try {
            gid = pgroup.open();
            H5G_info_t info = H5.H5Gget_info(gid);
            nelems = (int) info.nlinks;
        }
        catch (HDF5Exception ex) {
            nelems = -1;
            log.debug("depth_first({}): H5Gget_info(gid {}) failure: ", parentObject, gid, ex);
        }

        if (nelems <= 0) {
            pgroup.close(gid);
            log.debug("depth_first({}): nelems <= 0", parentObject);
            log.trace("depth_first({}): finish", parentObject);
            return nTotal;
        }

        // since each call of H5.H5Gget_objname_by_idx() takes about one second.
        // 1,000,000 calls take 12 days. Instead of calling it in a loop,
        // we use only one call to get all the information, which takes about
        // two seconds
        int[] objTypes = new int[nelems];
        long[] fNos = new long[nelems];
        long[] objRefs = new long[nelems];
        String[] objNames = new String[nelems];

        try {
            H5.H5Gget_obj_info_full(fid, fullPath, objNames, objTypes, null, fNos, objRefs, indexType, indexOrder);
        }
        catch (HDF5Exception ex) {
            log.debug("depth_first({}): failure: ", parentObject, ex);
            log.trace("depth_first({}): finish", parentObject);
            ex.printStackTrace();
            return nTotal;
        }

        int nStart = getStartMembers();
        int nMax = getMaxMembers();

        String obj_name;
        int obj_type;

        // Iterate through the file to see members of the group
        for (int i = 0; i < nelems; i++) {
            obj_name = objNames[i];
            obj_type = objTypes[i];
            log.trace("depth_first({}): obj_name={}, obj_type={}", parentObject, obj_name, obj_type);
            long oid[] = { objRefs[i], fNos[i] };

            if (obj_name == null) {
                log.trace("depth_first({}): continue after null obj_name", parentObject);
                continue;
            }

            nTotal++;

            if (nMax > 0) {
                if ((nTotal - nStart) >= nMax)
                    break; // loaded enough objects
            }

            boolean skipLoad = false;
            if ((nTotal > 0) && (nTotal < nStart))
                skipLoad = true;

            // create a new group
            if (obj_type == HDF5Constants.H5O_TYPE_GROUP) {
                //H5Group g = new H5Group(this, obj_name, fullPath, pgroup, oid);
                H5Group g = new H5Group(this, obj_name, fullPath, pgroup);

                pgroup.addToMemberList(g);

                // detect and stop loops
                // a loop is detected if there exists object with the same
                // object ID by tracing path back up to the root.
                boolean hasLoop = false;
                H5Group tmpObj = (H5Group) parentObject;

                while (tmpObj != null) {
                    if (tmpObj.equalsOID(oid) && !(tmpObj.getPath() == null)) {
                        hasLoop = true;
                        break;
                    }
                    else {
                        tmpObj = (H5Group) tmpObj.getParent();
                    }
                }

                // recursively go through the next group
                // stops if it has loop.
                if (!hasLoop) {
                    nTotal = depth_first(g, nTotal);
                }
            }
            else if (skipLoad) {
                continue;
            }
            else if (obj_type == HDF5Constants.H5O_TYPE_DATASET) {
                long did = -1;
                long tid = -1;
                int tclass = -1;
                try {
                    did = H5.H5Dopen(fid, fullPath + obj_name, HDF5Constants.H5P_DEFAULT);
                    if (did >= 0) {
                        tid = H5.H5Dget_type(did);

                        tclass = H5.H5Tget_class(tid);
                        if ((tclass == HDF5Constants.H5T_ARRAY) || (tclass == HDF5Constants.H5T_VLEN)) {
                            // for ARRAY, the type is determined by the base type
                            long btid = H5.H5Tget_super(tid);

                            tclass = H5.H5Tget_class(btid);

                            try {
                                H5.H5Tclose(btid);
                            }
                            catch (Exception ex) {
                                log.debug("depth_first({})[{}] dataset {} H5Tclose(btid {}) failure: ", parentObject, i, obj_name, btid, ex);
                            }
                        }
                    }
                    else {
                        log.debug("depth_first({})[{}] {} dataset open failure", parentObject, i, obj_name);
                    }
                }
                catch (Exception ex) {
                    log.debug("depth_first({})[{}] {} dataset access failure: ", parentObject, i, obj_name, ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tid);
                    }
                    catch (Exception ex) {
                        log.debug("depth_first({})[{}] daatset {} H5Tclose(tid {}) failure: ", parentObject, i, obj_name, tid, ex);
                    }
                    try {
                        H5.H5Dclose(did);
                    }
                    catch (Exception ex) {
                        log.debug("depth_first({})[{}] dataset {} H5Dclose(did {}) failure: ", parentObject, i, obj_name, did, ex);
                    }
                }
                Dataset d = null;
                if (tclass == HDF5Constants.H5T_COMPOUND) {
                    // create a new compound dataset
                    d = new H5CompoundDS(this, obj_name, fullPath, oid); // deprecated!
                }
                else {
                    // create a new scalar dataset
                    d = new H5ScalarDS(this, obj_name, fullPath, oid); // deprecated!
                }

                pgroup.addToMemberList(d);
            }
            else if (obj_type == HDF5Constants.H5O_TYPE_NAMED_DATATYPE) {
                Datatype t = new H5Datatype(this, obj_name, fullPath, oid); // deprecated!

                pgroup.addToMemberList(t);
            }
            else if (obj_type == HDF5Constants.H5O_TYPE_UNKNOWN) {
                H5Link link = new H5Link(this, obj_name, fullPath, oid);

                pgroup.addToMemberList(link);
                continue; // do the next one, if the object is not identified.
            }
        } // for ( i = 0; i < nelems; i++)

        pgroup.close(gid);

        log.trace("depth_first({}): finish", parentObject);
        return nTotal;
    } // private depth_first()

    /**
     * Returns a list of all the members of this H5File in a
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

    private HObject copyDataset(Dataset srcDataset, H5Group pgroup, String dstName) throws Exception {
        log.trace("copyDataset(): start");
        Dataset dataset = null;
        long srcdid = -1, dstdid = -1;
        long ocp_plist_id = -1;
        String dname = null, path = null;

        if (pgroup.isRoot()) {
            path = HObject.separator;
        }
        else {
            path = pgroup.getPath() + pgroup.getName() + HObject.separator;
        }

        if ((dstName == null) || dstName.equals(HObject.separator) || (dstName.length() < 1)) {
            dstName = srcDataset.getName();
        }
        dname = path + dstName;

        try {
            srcdid = srcDataset.open();
            dstdid = pgroup.open();

            try {
                ocp_plist_id = H5.H5Pcreate(HDF5Constants.H5P_OBJECT_COPY);
                H5.H5Pset_copy_object(ocp_plist_id, HDF5Constants.H5O_COPY_EXPAND_REFERENCE_FLAG);
                H5.H5Ocopy(srcdid, ".", dstdid, dstName, ocp_plist_id, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                log.debug("copyDataset(): {} failure: ", dname, ex);
            }
            finally {
                try {
                    H5.H5Pclose(ocp_plist_id);
                }
                catch (Exception ex) {
                    log.debug("copyDataset(): {} H5Pclose(ocp_plist_id {}) failure: ", dname, ocp_plist_id, ex);
                }
            }

            if (srcDataset instanceof H5ScalarDS) {
                dataset = new H5ScalarDS(pgroup.getFileFormat(), dstName, path);
            }
            else {
                dataset = new H5CompoundDS(pgroup.getFileFormat(), dstName, path);
            }

            pgroup.addToMemberList(dataset);
        }
        finally {
            try {
                srcDataset.close(srcdid);
            }
            catch (Exception ex) {
                log.debug("copyDataset(): {} srcDataset.close(srcdid {}) failure: ", dname, srcdid, ex);
            }
            try {
                pgroup.close(dstdid);
            }
            catch (Exception ex) {
                log.debug("copyDataset(): {} pgroup.close(dstdid {}) failure: ", dname, dstdid, ex);
            }
        }

        log.trace("copyDataset(): finish");
        return dataset;
    }

    /**
     * Constructs a dataset for specified dataset identifier.
     *
     * @param did
     *            the dataset identifier
     * @param name
     *            the name of the dataset
     * @param path
     *            the path of the dataset
     *
     * @return the dataset if successful; otherwise return null.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    private Dataset getDataset(long did, String name, String path) throws HDF5Exception {
        log.trace("getDataset(): start");
        Dataset dataset = null;
        if (did >= 0) {
            long tid = -1;
            int tclass = -1;
            try {
                tid = H5.H5Dget_type(did);
                tclass = H5.H5Tget_class(tid);
                if (tclass == HDF5Constants.H5T_ARRAY) {
                    // for ARRAY, the type is determined by the base type
                    long btid = H5.H5Tget_super(tid);
                    tclass = H5.H5Tget_class(btid);
                    try {
                        H5.H5Tclose(btid);
                    }
                    catch (Exception ex) {
                        log.debug("getDataset(): {} H5Tclose(btid {}) failure: ", name, btid, ex);
                    }
                }
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("getDataset(): {} H5Tclose(tid {}) failure: ", name, tid, ex);
                }
            }

            if (tclass == HDF5Constants.H5T_COMPOUND) {
                dataset = new H5CompoundDS(this, name, path);
            }
            else {
                dataset = new H5ScalarDS(this, name, path);
            }
        }
        else {
            log.debug("getDataset(): id failure");
        }

        log.trace("getDataset(): finish");
        return dataset;
    }

    /**
     * Copies a named datatype to another location.
     *
     * @param srcType
     *            the source datatype
     * @param pgroup
     *            the group which the new datatype is copied to
     * @param dstName
     *            the name of the new dataype
     *
     * @throws Exception
     *            If there is a failure.
     */
    private HObject copyDatatype(Datatype srcType, H5Group pgroup, String dstName) throws Exception {
        log.trace("copyDatatype(): start");
        Datatype datatype = null;
        long tid_src = -1;
        long gid_dst = -1;
        String path = null;

        if (pgroup.isRoot()) {
            path = HObject.separator;
        }
        else {
            path = pgroup.getPath() + pgroup.getName() + HObject.separator;
        }

        if ((dstName == null) || dstName.equals(HObject.separator) || (dstName.length() < 1)) {
            dstName = srcType.getName();
        }

        try {
            tid_src = srcType.open();
            gid_dst = pgroup.open();

            try {
                H5.H5Ocopy(tid_src, ".", gid_dst, dstName, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                log.debug("copyDatatype(): {} H5Ocopy(tid_src {}) failure: ", dstName, tid_src, ex);
            }
            datatype = new H5Datatype(pgroup.getFileFormat(), dstName, path);

            pgroup.addToMemberList(datatype);
        }
        finally {
            try {
                srcType.close(tid_src);
            }
            catch (Exception ex) {
                log.debug("copyDatatype(): {} srcType.close(tid_src {}) failure: ", dstName, tid_src, ex);
            }
            try {
                pgroup.close(gid_dst);
            }
            catch (Exception ex) {
                log.debug("copyDatatype(): {} pgroup.close(gid_dst {}) failure: ", dstName, gid_dst, ex);
            }
        }

        log.trace("copyDatatype(): finish");
        return datatype;
    }

    /**
     * Copies a group and its members to a new location.
     *
     * @param srcGroup
     *            the source group
     * @param dstGroup
     *            the location where the new group is located
     * @param dstName
     *            the name of the new group
     *
     * @throws Exception
     *            If there is a failure.
     */
    private HObject copyGroup(H5Group srcGroup, H5Group dstGroup, String dstName) throws Exception {
        log.trace("copyGroup(): start");
        H5Group group = null;
        long srcgid = -1, dstgid = -1;
        String path = null;

        if (dstGroup.isRoot()) {
            path = HObject.separator;
        }
        else {
            path = dstGroup.getPath() + dstGroup.getName() + HObject.separator;
        }

        if ((dstName == null) || dstName.equals(HObject.separator) || (dstName.length() < 1)) {
            dstName = srcGroup.getName();
        }

        try {
            srcgid = srcGroup.open();
            dstgid = dstGroup.open();
            try {
                H5.H5Ocopy(srcgid, ".", dstgid, dstName, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                log.debug("copyGroup(): {} H5Ocopy(srcgid {}) failure: ", dstName, srcgid, ex);
            }

            group = new H5Group(dstGroup.getFileFormat(), dstName, path, dstGroup);
            depth_first(group, Integer.MIN_VALUE); // reload all
            dstGroup.addToMemberList(group);
        }

        finally {
            try {
                srcGroup.close(srcgid);
            }
            catch (Exception ex) {
                log.debug("copyGroup(): {} srcGroup.close(srcgid {}) failure: ", dstName, srcgid, ex);
            }
            try {
                dstGroup.close(dstgid);
            }
            catch (Exception ex) {
                log.debug("copyGroup(): {} pgroup.close(dstgid {}) failure: ", dstName, dstgid, ex);
            }
        }

        log.trace("copyGroup(): finish");
        return group;
    }

    /**
     * Constructs a group for specified group identifier and retrieves members.
     *
     * @param gid
     *            The group identifier.
     * @param name
     *            The group name.
     * @param pGroup
     *            The parent group, or null for the root group.
     *
     * @return The group if successful; otherwise returns false.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    private H5Group getGroup(long gid, String name, Group pGroup) throws HDF5Exception {
        log.trace("getGroup(): start");
        String parentPath = null;
        String thisFullName = null;
        String memberFullName = null;

        if (pGroup == null) {
            thisFullName = name = "/";
        }
        else {
            parentPath = pGroup.getFullName();
            if ((parentPath == null) || parentPath.equals("/")) {
                thisFullName = "/" + name;
            }
            else {
                thisFullName = parentPath + "/" + name;
            }
        }

        // get rid of any extra "/"
        if (parentPath != null) {
            parentPath = parentPath.replaceAll("//", "/");
        }
        if (thisFullName != null) {
            thisFullName = thisFullName.replaceAll("//", "/");
        }

        log.trace("getGroup(): fullName={}", thisFullName);

        H5Group group = new H5Group(this, name, parentPath, pGroup);

        H5G_info_t group_info = null;
        H5O_info_t obj_info = null;
        long oid = -1;
        String link_name = null;
        try {
            group_info = H5.H5Gget_info(gid);
        }
        catch (Exception ex) {
            log.debug("getGroup(): {} H5Gget_info(gid {}) failure: ", name, gid, ex);
        }
        try {
            oid = H5.H5Oopen(gid, thisFullName, HDF5Constants.H5P_DEFAULT);
        }
        catch (Exception ex) {
            log.debug("getGroup(): {} H5Oopen(gid {}) failure: ", name, gid, ex);
        }

        // retrieve only the immediate members of the group, do not follow
        // subgroups
        for (int i = 0; i < group_info.nlinks; i++) {
            try {
                link_name = H5.H5Lget_name_by_idx(gid, thisFullName, indexType, indexOrder, i,
                        HDF5Constants.H5P_DEFAULT);
                obj_info = H5
                        .H5Oget_info_by_idx(oid, thisFullName, indexType, indexOrder, i, HDF5Constants.H5P_DEFAULT);
            }
            catch (HDF5Exception ex) {
                log.debug("getGroup()[{}]: {} name,info failure: ", i, name, ex);
                log.trace("getGroup()[{}]: continue", i);
                // do not stop if accessing one member fails
                continue;
            }
            // create a new group
            if (obj_info.type == HDF5Constants.H5O_TYPE_GROUP) {
                H5Group g = new H5Group(this, link_name, thisFullName, group);
                group.addToMemberList(g);
            }
            else if (obj_info.type == HDF5Constants.H5O_TYPE_DATASET) {
                long did = -1;
                Dataset d = null;

                if ((thisFullName == null) || thisFullName.equals("/")) {
                    memberFullName = "/" + link_name;
                }
                else {
                    memberFullName = thisFullName + "/" + link_name;
                }

                try {
                    did = H5.H5Dopen(fid, memberFullName, HDF5Constants.H5P_DEFAULT);
                    d = getDataset(did, link_name, thisFullName);
                }
                finally {
                    try {
                        H5.H5Dclose(did);
                    }
                    catch (Exception ex) {
                        log.debug("getGroup()[{}]: {} H5Dclose(did {}) failure: ", i, name, did, ex);
                    }
                }
                group.addToMemberList(d);
            }
            else if (obj_info.type == HDF5Constants.H5O_TYPE_NAMED_DATATYPE) {
                Datatype t = new H5Datatype(this, link_name, thisFullName);
                group.addToMemberList(t);
            }
        } // End of for loop.
        try {
            if (oid >= 0)
                H5.H5Oclose(oid);
        }
        catch (Exception ex) {
            log.debug("getGroup(): {} H5Oclose(oid {}) failure: ", name, oid, ex);
        }
        log.trace("getGroup(): finish");
        return group;
    }

    /**
     * Retrieves the name of the target object that is being linked to.
     *
     * @param obj
     *            The current link object.
     *
     * @return The name of the target object.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    public static String getLinkTargetName(HObject obj) throws Exception {
        log.trace("getLinkTargetName(): start");
        String[] link_value = { null, null };
        String targetObjName = null;

        if (obj == null) {
            log.debug("getLinkTargetName(): object is null");
            log.trace("getLinkTargetName(): finish");
            return null;
        }

        if (obj.getFullName().equals("/")) {
            log.debug("getLinkTargetName(): object is root group, links not allowed");
            log.trace("getLinkTargetName(): finish");
            return null;
        }

        H5L_info_t link_info = null;
        try {
            link_info = H5.H5Lget_info(obj.getFID(), obj.getFullName(), HDF5Constants.H5P_DEFAULT);
        }
        catch (Throwable err) {
            log.debug("getLinkTargetName(): H5Lget_info {} failure: ", obj.getFullName(), err);
        }
        if (link_info != null) {
            if ((link_info.type == HDF5Constants.H5L_TYPE_SOFT) || (link_info.type == HDF5Constants.H5L_TYPE_EXTERNAL)) {
                try {
                    H5.H5Lget_value(obj.getFID(), obj.getFullName(), link_value, HDF5Constants.H5P_DEFAULT);
                }
                catch (Exception ex) {
                    log.debug("getLinkTargetName(): H5Lget_value {} failure: ", obj.getFullName(), ex);
                }
                if (link_info.type == HDF5Constants.H5L_TYPE_SOFT)
                    targetObjName = link_value[0];
                else if (link_info.type == HDF5Constants.H5L_TYPE_EXTERNAL) {
                    targetObjName = link_value[1] + FileFormat.FILE_OBJ_SEP + link_value[0];
                }
            }
        }
        log.trace("getLinkTargetName(): finish");
        return targetObjName;
    }

    /**
     * Export dataset.
     *
     * @param file_export_name
     *            The file name to export data into.
     * @param file_name
     *            The name of the HDF5 file containing the dataset.
     * @param object_path
     *            The full path of the dataset to be exported.
     *
     * @throws Exception
     *            If there is a failure.
     */
    public void exportDataset(String file_export_name, String file_name, String object_path, int binary_order)
            throws Exception {
        H5.H5export_dataset(file_export_name, file_name, object_path, binary_order);
    }

    /**
     * Renames an attribute.
     *
     * @param obj
     *            The object whose attribute is to be renamed.
     * @param oldAttrName
     *            The current name of the attribute.
     * @param newAttrName
     *            The new name of the attribute.
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    public void renameAttribute(HObject obj, String oldAttrName, String newAttrName) throws Exception {
        log.trace("renameAttribute(): rename {} to {}", oldAttrName, newAttrName);
        if (!attrFlag) {
            attrFlag = true;
            H5.H5Arename_by_name(obj.getFID(), obj.getName(), oldAttrName, newAttrName, HDF5Constants.H5P_DEFAULT);
        }
    }

    /**
     * Rename the given object
     *
     * @param obj
     *            the object to be renamed.
     * @param newName
     *            the new name of the object.
     *
     * @throws Exception
     *            If there is a failure.
     */
    public static void renameObject(HObject obj, String newName) throws Exception {
        String currentFullPath = obj.getPath() + obj.getName();
        String newFullPath = obj.getPath() + newName;

        currentFullPath = currentFullPath.replaceAll("//", "/");
        newFullPath = newFullPath.replaceAll("//", "/");

        if (currentFullPath.equals("/")) {
            throw new HDF5Exception("Can't rename the root group.");
        }

        if (currentFullPath.equals(newFullPath)) {
            throw new HDF5Exception("The new name is the same as the current name.");
        }

        // Call the library to move things in the file
        H5.H5Lmove(obj.getFID(), currentFullPath, obj.getFID(), newFullPath, HDF5Constants.H5P_DEFAULT,
                HDF5Constants.H5P_DEFAULT);
    }

    public static int getIndexTypeValue(String strtype) {
        if (strtype.compareTo("H5_INDEX_NAME") == 0)
            return HDF5Constants.H5_INDEX_NAME;
        if (strtype.compareTo("H5_INDEX_CRT_ORDER") == 0)
            return HDF5Constants.H5_INDEX_CRT_ORDER;
        if (strtype.compareTo("H5_INDEX_N") == 0)
            return HDF5Constants.H5_INDEX_N;
        return HDF5Constants.H5_INDEX_UNKNOWN;
    }

    public static int getIndexOrderValue(String strorder) {
        if (strorder.compareTo("H5_ITER_INC") == 0)
            return HDF5Constants.H5_ITER_INC;
        if (strorder.compareTo("H5_ITER_DEC") == 0)
            return HDF5Constants.H5_ITER_DEC;
        if (strorder.compareTo("H5_ITER_NATIVE") == 0)
            return HDF5Constants.H5_ITER_NATIVE;
        if (strorder.compareTo("H5_ITER_N") == 0)
            return HDF5Constants.H5_ITER_N;
        return HDF5Constants.H5_ITER_UNKNOWN;
    }

    public int getIndexType(String strtype) {
        if (strtype != null) {
            if (strtype.compareTo("H5_INDEX_NAME") == 0)
                return HDF5Constants.H5_INDEX_NAME;
            if (strtype.compareTo("H5_INDEX_CRT_ORDER") == 0)
                return HDF5Constants.H5_INDEX_CRT_ORDER;
            return HDF5Constants.H5_INDEX_UNKNOWN;
        }
        return getIndexType();
    }

    public int getIndexType() {
        return indexType;
    }

    public void setIndexType(int indexType) {
        this.indexType = indexType;
    }

    public int getIndexOrder(String strorder) {
        if (strorder != null) {
            if (strorder.compareTo("H5_ITER_INC") == 0)
                return HDF5Constants.H5_ITER_INC;
            if (strorder.compareTo("H5_ITER_DEC") == 0)
                return HDF5Constants.H5_ITER_DEC;
            if (strorder.compareTo("H5_ITER_NATIVE") == 0)
                return HDF5Constants.H5_ITER_NATIVE;
            if (strorder.compareTo("H5_ITER_N") == 0)
                return HDF5Constants.H5_ITER_N;
            return HDF5Constants.H5_ITER_UNKNOWN;
        }
        return getIndexOrder();
    }

    public int getIndexOrder() {
        return indexOrder;
    }

    public void setIndexOrder(int indexOrder) {
        this.indexOrder = indexOrder;
    }
}
