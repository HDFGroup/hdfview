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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
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
 * The HDF5 file structure is stored in a tree that is made up of Java TreeNode objects. Each tree node represents an
 * HDF5 object: a Group, Dataset, or Named Datatype. Starting from the root of the tree, <i>rootNode</i>, the tree can
 * be traversed to find a specific object.
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
 *     DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) file
 *             .getRootNode();
 *     if (theRoot == null)
 *         return null;
 *     else if (path.equals(&quot;/&quot;))
 *         return (HObject) theRoot.getUserObject();
 *
 *     Enumeration local_enum = ((DefaultMutableTreeNode) theRoot)
 *             .breadthFirstEnumeration();
 *     DefaultMutableTreeNode theNode = null;
 *     HObject theObj = null;
 *     while (local_enum.hasMoreElements()) {
 *         theNode = (DefaultMutableTreeNode) local_enum.nextElement();
 *         theObj = (HObject) theNode.getUserObject();
 *         String fullPath = theObj.getFullName() + &quot;/&quot;;
 *         if (path.equals(fullPath) &&  theObj.getPath() != null ) {
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
     * The root node of the file hierarchy.
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
     * <li>READ: Read-only access; open() will fail file doesn't exist.
     * <li>WRITE: Read/Write access; open() will fail if file doesn't exist or if file can't be opened with read/write
     * access.
     * <li>CREATE: Read/Write access; create a new file or truncate an existing one; open() will fail if file can't be
     * created or if file exists but can't be opened read/write.
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
     * are thrown. The actual copy is carried out by the method: {@link #copyAttributes(int, int)}
     *
     * @param src
     *            The source object.
     * @param dst
     *            The destination object.
     * @see #copyAttributes(int, int)
     */
    public static final void copyAttributes(HObject src, HObject dst) {
        if ((src != null) && (dst != null)) {
            int srcID = src.open();
            int dstID = dst.open();

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
    public static final void copyAttributes(int src_id, int dst_id) {
        int aid_src = -1, aid_dst = -1, atid = -1, asid = -1;
        String[] aName = { "" };
        H5O_info_t obj_info = null;

        try {
            obj_info = H5.H5Oget_info(src_id);
        }
        catch (Exception ex) {
            obj_info.num_attrs = -1;
        }

        if (obj_info.num_attrs < 0) {
            return;
        }

        for (int i = 0; i < obj_info.num_attrs; i++) {
            aName[0] = new String("");

            try {
                aid_src = H5.H5Aopen_by_idx(src_id, ".", HDF5Constants.H5_INDEX_CRT_ORDER, HDF5Constants.H5_ITER_INC,
                        i, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                H5.H5Aget_name(aid_src, H5File.attrNameLen, aName);
                atid = H5.H5Aget_type(aid_src);
                asid = H5.H5Aget_space(aid_src);

                aid_dst = H5.H5Acreate(dst_id, aName[0], atid, asid, HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT);

                // use native data copy
                H5.H5Acopy(aid_src, aid_dst);

            }
            catch (Exception ex) {
                log.debug("Attribute[{}] failure: ", i, ex);
            }

            try {
                H5.H5Sclose(asid);
            }
            catch (Exception ex) {
                log.debug("H5Sclose failure: ", ex);
            }
            try {
                H5.H5Tclose(atid);
            }
            catch (Exception ex) {
                log.debug("H5Tclose failure: ", ex);
            }
            try {
                H5.H5Aclose(aid_src);
            }
            catch (Exception ex) {
                log.debug("src H5Aclose failure: ", ex);
            }
            try {
                H5.H5Aclose(aid_dst);
            }
            catch (Exception ex) {
                log.debug("dst H5Aclose failure: ", ex);
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
     * @return The list of the object's attributes.
     * @throws HDF5Exception
     *             If an underlying HDF library routine is unable to perform a step necessary to retrieve the
     *             attributes. A variety of failures throw this exception.
     * @see #getAttribute(int,int,int)
     */
    public static final List<Attribute> getAttribute(int objID) throws HDF5Exception {
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
     * @return The list of the object's attributes.
     * @throws HDF5Exception
     *             If an underlying HDF library routine is unable to perform a step necessary to retrieve the
     *             attributes. A variety of failures throw this exception.
     */

    public static final List<Attribute> getAttribute(int objID, int idx_type, int order) throws HDF5Exception {
        List<Attribute> attributeList = null;
        int aid = -1, sid = -1, tid = -1;
        H5O_info_t obj_info = null;
        log.trace("getAttribute: start");

        try {
            obj_info = H5.H5Oget_info(objID);
        }
        catch (Exception ex) {
            log.debug("H5Oget_info failure: ", ex);
        }
        if (obj_info.num_attrs <= 0) {
            return (attributeList = new Vector<Attribute>());
        }

        int n = (int) obj_info.num_attrs;
        attributeList = new Vector<Attribute>(n);
        log.trace("getAttribute: num_attrs={}", n);

        for (int i = 0; i < n; i++) {
            long lsize = 1;
            log.trace("getAttribute: attribute[{}]", i);

            try {
                aid = H5.H5Aopen_by_idx(objID, ".", idx_type, order, i, HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT);
                sid = H5.H5Aget_space(aid);

                long dims[] = null;
                int rank = H5.H5Sget_simple_extent_ndims(sid);

                if (rank > 0) {
                    dims = new long[rank];
                    H5.H5Sget_simple_extent_dims(sid, dims, null);
                    for (int j = 0; j < dims.length; j++) {
                        lsize *= dims[j];
                    }
                }
                String[] nameA = { "" };
                H5.H5Aget_name(aid, H5File.attrNameLen, nameA);
                log.trace("getAttribute: attribute[{}] is {}", i, nameA);

                int tmptid = -1;
                try {
                    tmptid = H5.H5Aget_type(aid);
                    tid = H5.H5Tget_native_type(tmptid);
                    log.trace("getAttribute: attribute[{}] tid={} native tmptid={} from aid={}", i, tid, tmptid, aid);
                }
                finally {
                    try {
                        H5.H5Tclose(tmptid);
                    }
                    catch (Exception ex) {
                        log.debug("H5Tclose failure: ", ex);
                    }
                }
                Datatype attrType = new H5Datatype(tid);
                Attribute attr = new Attribute(nameA[0], attrType, dims);
                attributeList.add(attr);
                log.trace("getAttribute: attribute[{}] Datatype={}", i, attrType.getDatatypeDescription());

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
                    log.debug("H5Tis_variable_str failure: ", ex);
                }
                isVLEN = (tclass == HDF5Constants.H5T_VLEN);
                isCompound = (tclass == HDF5Constants.H5T_COMPOUND);
                log.trace(
                        "getAttribute: attribute[{}] has size={} isCompound={} isScalar={} is_variable_str={} isVLEN={}",
                        i, lsize, isCompound, isScalar, is_variable_str, isVLEN);

                // retrieve the attribute value
                if (lsize <= 0) {
                    continue;
                }

                Object value = null;
                if (isVLEN || is_variable_str || isCompound || (isScalar && tclass == HDF5Constants.H5T_ARRAY)) {
                    String[] strs = new String[(int) lsize];
                    for (int j = 0; j < lsize; j++) {
                        strs[j] = "";
                    }
                    try {
                        log.trace("getAttribute: attribute[{}] H5AreadVL", i);
                        H5.H5AreadVL(aid, tid, strs);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    value = strs;
                }
                else {
                    value = H5Datatype.allocateArray(tid, (int) lsize);
                    if (value == null) {
                        continue;
                    }

                    if (tclass == HDF5Constants.H5T_ARRAY) {
                        int tmptid1 = -1, tmptid2 = -1;
                        try {
                            log.trace("getAttribute: attribute[{}] H5Aread ARRAY tid={}", i, tid);
                            H5.H5Aread(aid, tid, value);
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        finally {
                            try {
                                H5.H5Tclose(tmptid1);
                            }
                            catch (Exception ex) {
                                log.debug("tid1 H5Tclose failure: ", ex);
                            }
                            try {
                                H5.H5Tclose(tmptid2);
                            }
                            catch (Exception ex) {
                                log.debug("tid2 H5Tclose failure: ", ex);
                            }
                        }
                    }
                    else {
                        log.trace("getAttribute: attribute[{}] H5Aread", i);
                        H5.H5Aread(aid, tid, value);
                    }

                    if (tclass == HDF5Constants.H5T_STRING) {
                        log.trace("getAttribute: attribute[{}] byteToString", i);
                        value = Dataset.byteToString((byte[]) value, H5.H5Tget_size(tid));
                    }
                    else if (tclass == HDF5Constants.H5T_REFERENCE) {
                        log.trace("getAttribute: attribute[{}] byteToLong", i);
                        value = HDFNativeData.byteToLong((byte[]) value);
                    }
                }

                attr.setValue(value);

            }
            catch (HDF5Exception ex) {
                log.debug("Attribute[{}] inspection failure: ", i, ex);
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("H5Tclose[{}] failure: ", i, ex);
                }
                try {
                    H5.H5Sclose(sid);
                }
                catch (Exception ex) {
                    log.debug("H5Sclose[{}] failure: ", i, ex);
                }
                try {
                    H5.H5Aclose(aid);
                }
                catch (Exception ex) {
                    log.debug("H5Aclose[{}] failure: ", i, ex);
                }
            }
        } // for (int i=0; i<obj_info.num_attrs; i++)

        log.trace("getAttribute: finish");
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
     * href="http://hdfgroup.org/HDF5/doc/ADGuide/ImageSpec.html"> HDF5 Image and Palette Specification</a>.
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
     * @throws Exception
     *             If there is a problem creating the attributes, or if the selectionFlag is invalid.
     */
    private static final void createImageAttributes(Dataset dataset, int selectionFlag) throws Exception {
        String subclass = null;
        String interlaceMode = null;

        if (selectionFlag == ScalarDS.INTERLACE_PIXEL) {
            subclass = "IMAGE_TRUECOLOR";
            interlaceMode = "INTERLACE_PIXEL";
        }
        else if (selectionFlag == ScalarDS.INTERLACE_PLANE) {
            subclass = "IMAGE_TRUECOLOR";
            interlaceMode = "INTERLACE_PLANE";
        }
        else if (selectionFlag == -1) {
            subclass = "IMAGE_INDEXED";
        }
        else {
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
     * @throws Exception
     *             If there is a problem in the update process.
     */
    public static final void updateReferenceDataset(H5File srcFile, H5File dstFile) throws Exception {
        if ((srcFile == null) || (dstFile == null)) {
            return;
        }

        HObject srcRoot = srcFile.getRootObject();
        HObject newRoot = dstFile.getRootObject();

        //Enumeration<?> srcEnum = srcRoot.breadthFirstEnumeration();
        //Enumeration<?> newEnum = newRoot.breadthFirstEnumeration();

        // build one-to-one table of between objects in
        // the source file and new file
        int did = -1, tid = -1;
        HObject srcObj, newObj;
        Hashtable<String, long[]> oidMap = new Hashtable<String, long[]>();
        List<ScalarDS> refDatasets = new Vector<ScalarDS>();
        //while (newEnum.hasMoreElements() && srcEnum.hasMoreElements()) {
        //    srcObj = (HObject) ((DefaultMutableTreeNode) srcEnum.nextElement()).getUserObject();
        //    newObj = (HObject) ((DefaultMutableTreeNode) newEnum.nextElement()).getUserObject();
        //    oidMap.put(String.valueOf((srcObj.getOID())[0]), newObj.getOID());
        //    did = -1;
        //    tid = -1;

            // for Scalar DataSets in destination, if there is an object
            // reference in the dataset, add it to the refDatasets list for
            // later updating.
        //    if (newObj instanceof ScalarDS) {
        //        ScalarDS sd = (ScalarDS) newObj;
        //        did = sd.open();
        //        if (did >= 0) {
        //            try {
        //                tid = H5.H5Dget_type(did);
        //                if (H5.H5Tequal(tid, HDF5Constants.H5T_STD_REF_OBJ)) {
        //                    refDatasets.add(sd);
        //                }
        //            }
        //            catch (Exception ex) {
        //                log.debug("ScalarDS reference  failure: ", ex);
        //            }
        //            finally {
        //                try {
        //                    H5.H5Tclose(tid);
        //                }
        //                catch (Exception ex) {
        //                    log.debug("ScalarDS reference H5Tclose failure: ", ex);
        //                }
        //            }
        //        }
        //        sd.close(did);
        //    } // if (newObj instanceof ScalarDS)
        //}

        // Update the references in the scalar datasets in the dest file.
        H5ScalarDS d = null;
        int sid = -1, size = 0, rank = 0;
        //int n = refDatasets.size();
        //for (int i = 0; i < n; i++) {
        //    log.trace("Update the references in the scalar datasets in the dest file");
        //    d = (H5ScalarDS) refDatasets.get(i);
        //    byte[] buf = null;
        //    long[] refs = null;

        //    try {
        //        did = d.open();
        //        if (did >= 0) {
        //            tid = H5.H5Dget_type(did);
        //            sid = H5.H5Dget_space(did);
        //            rank = H5.H5Sget_simple_extent_ndims(sid);
        //            size = 1;
        //            if (rank > 0) {
        //                long[] dims = new long[rank];
        //                H5.H5Sget_simple_extent_dims(sid, dims, null);
        //                for (int j = 0; j < rank; j++) {
        //                    size *= (int) dims[j];
        //                }
        //                dims = null;
        //            }

        //            buf = new byte[size * 8];
        //            H5.H5Dread(did, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf);

                    // update the ref values
        //            refs = HDFNativeData.byteToLong(buf);
        //            size = refs.length;
        //            for (int j = 0; j < size; j++) {
        //                long[] theOID = oidMap.get(String.valueOf(refs[j]));
        //                if (theOID != null) {
        //                    refs[j] = theOID[0];
        //                }
        //            }

                    // write back to file
        //            H5.H5Dwrite(did, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, refs);
        //        }
        //        else {
        //            log.debug("dest file dataset failed to open");
        //        }
        //    }
        //    catch (Exception ex) {
        //        continue;
        //    }
        //    finally {
        //        try {
        //            H5.H5Tclose(tid);
        //        }
        //        catch (Exception ex) {
        //            log.debug("H5ScalarDS reference[{}] H5Tclose failure: ", i, ex);
        //        }
        //        try {
        //            H5.H5Sclose(sid);
        //        }
        //        catch (Exception ex) {
        //            log.debug("H5ScalarDS reference[{}] H5Sclose failure: ", i, ex);
        //        }
        //        try {
        //            H5.H5Dclose(did);
        //        }
        //        catch (Exception ex) {
        //            log.debug("H5ScalarDS reference[{}] H5Dclose failure: ", i, ex);
        //        }
        //    }

        //    refs = null;
        //    buf = null;
        //} // for (int i=0; i<n; i++)
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
        log.debug("libversion is {}", ver);

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
     * @throws HDF5Exception
     *             If the file cannot be created or if createFlag has unexpected value.
     * @see hdf.object.FileFormat#createFile(java.lang.String, int)
     * @see #H5File(String, int)
     */
    @Override
    public FileFormat createFile(String filename, int createFlag) throws Exception {
        // Flag if we need to create or truncate the file.
        Boolean doCreateFile = true;

        // Won't create or truncate if CREATE_OPEN specified and file exists
        if ((createFlag & FILE_CREATE_OPEN) == FILE_CREATE_OPEN) {
            File f = new File(filename);
            if (f.exists()) {
                doCreateFile = false;
            }
        }

        if (doCreateFile) {

            int fapl = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
            ;

            if ((createFlag & FILE_CREATE_EARLY_LIB) != FILE_CREATE_EARLY_LIB) {
                H5.H5Pset_libver_bounds(fapl, HDF5Constants.H5F_LIBVER_LATEST, HDF5Constants.H5F_LIBVER_LATEST);
            }

            int fileid = H5.H5Fcreate(filename, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT, fapl);
            try {
                H5.H5Pclose(fapl);
                H5.H5Fclose(fileid);
            }
            catch (HDF5Exception ex) {
                log.debug("H5 file, {} failure: ", filename, ex);
            }
        }

        return new H5File(filename, WRITE);
    }

    /**
     * Creates an H5File instance with specified file name and access.
     * <p>
     *
     * @see hdf.object.FileFormat#createInstance(java.lang.String, int)
     * @see #H5File(String, int)
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
    public int open() throws Exception {
        return open(true);
    }

    /**
     * Opens file and returns a file identifier.
     *
     * @see hdf.object.FileFormat#open(int...)
     */
    @Override
    public int open(int... propList) throws Exception {
        setIndexType(propList[0]);
        return open(true);
    }

    /**
     * Sets the bounds of library versions.
     *
     * @param low
     *            The earliest version of the library.
     * @param high
     *            The latest version of the library.
     * @throws HDF5Exception
     */
    public void setLibBounds(int low, int high) throws Exception {
        int fapl = HDF5Constants.H5P_DEFAULT;

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
                log.debug("libver bounds H5Pclose failure: ", e);
            }
        }
    }

    /**
     * Gets the bounds of library versions.
     *
     * @return libver The earliest and latest version of the library.
     * @throws HDF5Exception
     */
    public int[] getLibBounds() throws Exception {
        return libver;
    }

    /**
     * Closes file associated with this H5File instance.
     *
     * @see hdf.object.FileFormat#close()
     * @throws HDF5Exception
     */
    @Override
    public void close() throws HDF5Exception {
        if (fid < 0) {
            log.debug("file {} is not open", fullFileName);
            return;
        }
        // The current working directory may be changed at Dataset.read()
        // by H5Dchdir_ext()by this file to make it work for external
        // datasets. We need to set it back to the original current working
        // directory (when hdf-java application started) before the file
        // is closed/opened. Otherwise, relative path, e.g. "./test.h5" may
        // not work
        String rootPath = System.getProperty("hdfview.workdir");
        if (rootPath == null) {
            rootPath = System.getProperty("user.dir");
        }
        H5.H5Dchdir_ext(rootPath);

        // clean up unused objects
        if (rootObject != null) {
            //DefaultMutableTreeNode theNode = null;
            //HObject theObj = null;
            //Enumeration<?> local_enum = (rootNode).breadthFirstEnumeration();
            //while (local_enum.hasMoreElements()) {
            //    theNode = (DefaultMutableTreeNode) local_enum.nextElement();
            //    theObj = (HObject) theNode.getUserObject();

            //    if (theObj instanceof Dataset) {
            //        ((Dataset) theObj).clear();
            //    }
            //    else if (theObj instanceof Group) {
            //        ((Group) theObj).clear();
            //    }
            //}
        }

        // Close all open objects associated with this file.
        try {
            int n = 0, type = -1, oids[];
            n = H5.H5Fget_obj_count(fid, HDF5Constants.H5F_OBJ_ALL);

            if (n > 0) {
                oids = new int[n];
                H5.H5Fget_obj_ids(fid, HDF5Constants.H5F_OBJ_ALL, n, oids);

                for (int i = 0; i < n; i++) {
                    type = H5.H5Iget_type(oids[i]);

                    if (HDF5Constants.H5I_DATASET == type) {
                        try {
                            H5.H5Dclose(oids[i]);
                        }
                        catch (Exception ex2) {
                            log.debug("Object[{}] H5Dclose failure: ", i, ex2);
                        }
                    }
                    else if (HDF5Constants.H5I_GROUP == type) {
                        try {
                            H5.H5Gclose(oids[i]);
                        }
                        catch (Exception ex2) {
                            log.debug("Object[{}] H5Gclose failure: ", i, ex2);
                        }
                    }
                    else if (HDF5Constants.H5I_DATATYPE == type) {
                        try {
                            H5.H5Tclose(oids[i]);
                        }
                        catch (Exception ex2) {
                            log.debug("Object[{}] H5Tclose failure: ", i, ex2);
                        }
                    }
                    else if (HDF5Constants.H5I_ATTR == type) {
                        try {
                            H5.H5Aclose(oids[i]);
                        }
                        catch (Exception ex2) {
                            log.debug("Object[{}] H5Aclose failure: ", i, ex2);
                        }
                    }
                } // for (int i=0; i<n; i++)
            } // if ( n>0)
        }
        catch (Exception ex) {
            log.debug("close open objects failure: ", ex);
        }

        try {
            H5.H5Fflush(fid, HDF5Constants.H5F_SCOPE_GLOBAL);
        }
        catch (Exception ex) {
            log.debug("H5Fflush failure: ", ex);
        }

        try {
            H5.H5Fclose(fid);
        }
        catch (Exception ex) {
            log.debug("H5Fclose failure: ", ex);
        }

        // Set fid to -1 but don't reset rootNode
        fid = -1;
    }

    /**
     * Returns the root node of the open HDF5 File.
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
        HObject obj = null;

        if ((path == null) || (path.length() <= 0)) {
            System.err.println("(path == null) || (path.length() <= 0)");
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
        int fid_before_open = fid;
        fid = open(false);
        if (fid < 0) {
            System.err.println("Could not open file handler");
            return null;
        }

        try {
            H5O_info_t info;
            int objType;
            int oid = H5.H5Oopen(fid, path, HDF5Constants.H5P_DEFAULT);

            if (oid >= 0) {
                info = H5.H5Oget_info(oid);
                objType = info.type;
                if (objType == HDF5Constants.H5O_TYPE_DATASET) {
                    int did = -1;
                    try {
                        did = H5.H5Dopen(fid, path, HDF5Constants.H5P_DEFAULT);
                        obj = getDataset(did, name, pPath);
                    }
                    finally {
                        try {
                            H5.H5Dclose(did);
                        }
                        catch (Exception ex) {
                            log.debug("{} H5Dclose failure: ", path, ex);
                        }
                    }
                }
                else if (objType == HDF5Constants.H5O_TYPE_GROUP) {
                    int gid = -1;
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
                            log.debug("{} H5Gclose failure: ", path, ex);
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
                ex.printStackTrace();
            }
        }
        catch (Exception ex) {
            log.debug("Exception finding obj {}", path);
            obj = null;
        }
        finally {
            if ((fid_before_open <= 0) && (obj == null)) {
                // close the fid that is not attached to any object
                try {
                    H5.H5Fclose(fid);
                }
                catch (Exception ex) {
                    log.debug("[] H5Fclose failure: ", path, ex);
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
        int tid = -1;
        H5Datatype dtype = null;

        log.trace("createDatatype with name={} start", name);
        try {
            H5Datatype t = (H5Datatype) createDatatype(tclass, tsize, torder, tsign, tbase);
            if ((tid = t.toNative()) < 0)
                throw new Exception("toNative failed");

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

        log.trace("createDatatype with name={} finish", name);
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
        log.trace("createDatatype");
        return new H5Datatype(tclass, tsize, torder, tsign);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#createDatatype(int, int, int, int, Datatype)
     */
    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign, Datatype tbase) throws Exception {
        log.trace("createDatatype with base");
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
        if (pgroup == null) {
            // create new dataset at the root group by default
            pgroup = (Group) get("/");
        }

        log.trace("createScalarDS name={}", name);
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
        log.trace("createCompoundDS name={}", name);
        ds = H5CompoundDS.create(name, pgroup, dims, maxdims, chunks, gzip, memberNames, memberDatatypes, memberRanks,
                memberDims, data);

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
        if (pgroup == null) { // create at the root group by default
            pgroup = (Group) get("/");
        }

        H5ScalarDS dataset = (H5ScalarDS)H5ScalarDS.create(name, pgroup, type, dims, maxdims, chunks, gzip, data);

        try {
            H5File.createImageAttributes(dataset, interlace);
            dataset.setIsImage(true);
        }
        catch (Exception ex) {
            log.debug("{} createImageAttributtes failure: ", name, ex);
        }

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
     * @see hdf.object.h5.H5Group#create(java.lang.String, hdf.object.Group, int...)
     *
     */
    public Group createGroup(String name, Group pgroup, int... gplist) throws Exception {
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
    public int createGcpl(int creationorder, int maxcompact, int mindense) throws Exception {
        int gcpl = -1;
        try {
            gcpl = H5.H5Pcreate(HDF5Constants.H5P_GROUP_CREATE);
            if (gcpl >= 0) {
                // Set link creation order.
                if (creationorder == Group.CRT_ORDER_TRACKED) {
                    H5.H5Pset_link_creation_order(gcpl, HDF5Constants.H5P_CRT_ORDER_TRACKED);
                }
                else if (creationorder == Group.CRT_ORDER_INDEXED) {
                    H5.H5Pset_link_creation_order(gcpl, HDF5Constants.H5P_CRT_ORDER_TRACKED
                            + HDF5Constants.H5P_CRT_ORDER_INDEXED);
                }
                // Set link storage.
                H5.H5Pset_link_phase_change(gcpl, maxcompact, mindense);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

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
     * @return The object pointed to by the new link if successful; otherwise returns null.
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public HObject createLink(Group parentGroup, String name, HObject currentObj, int lType) throws Exception {
        HObject obj = null;
        int type = 0;
        String current_full_name = null, new_full_name = null, parent_path = null;

        if (currentObj == null) {
            throw new HDF5Exception("The object pointed by the link cannot be null.");
        }
        if ((parentGroup == null) || parentGroup.isRoot()) {
            parent_path = HObject.separator;
        }
        else {
            parent_path = parentGroup.getPath() + HObject.separator + parentGroup.getName() + HObject.separator;
        }

        new_full_name = parent_path + name;

        if (lType == Group.LINK_TYPE_HARD)
            type = HDF5Constants.H5L_TYPE_HARD;

        else if (lType == Group.LINK_TYPE_SOFT)
            type = HDF5Constants.H5L_TYPE_SOFT;

        else if (lType == Group.LINK_TYPE_EXTERNAL)
            type = HDF5Constants.H5L_TYPE_EXTERNAL;

        if (H5.H5Lexists(fid, new_full_name, HDF5Constants.H5P_DEFAULT)) {
            H5.H5Ldelete(fid, new_full_name, HDF5Constants.H5P_DEFAULT);
        }

        if (type == HDF5Constants.H5L_TYPE_HARD) {
            if ((currentObj instanceof Group) && ((Group) currentObj).isRoot()) {
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
            obj = new H5Group(this, name, parent_path, parentGroup);
        }
        else if (currentObj instanceof H5Datatype) {
            obj = new H5Datatype(this, name, parent_path);
        }
        else if (currentObj instanceof H5CompoundDS) {
            obj = new H5CompoundDS(this, name, parent_path);
        }
        else if (currentObj instanceof H5ScalarDS) {
            obj = new H5ScalarDS(this, name, parent_path);
        }
        return obj;
    }

    /**
     * Creates a soft or external links to objects in a file that do not exist at the time the link is created.
     *
     * @param parentGroup
     *            The group where the link is created.
     * @param name
     *            The name of the link.
     * @param currentObj
     *            The name of the object the new link will reference. The object doesn't have to exist.
     * @param lType
     *            The type of link to be created.
     * @return The H5Link object pointed to by the new link if successful; otherwise returns null.
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public HObject createLink(Group parentGroup, String name, String currentObj, int lType) throws Exception {
        HObject obj = null;
        int type = 0;
        String new_full_name = null, parent_path = null;

        if (currentObj == null) {
            throw new HDF5Exception("The object pointed by the link cannot be null.");
        }
        if ((parentGroup == null) || parentGroup.isRoot()) {
            parent_path = HObject.separator;
        }
        else {
            parent_path = parentGroup.getPath() + HObject.separator + parentGroup.getName() + HObject.separator;
        }

        new_full_name = parent_path + name;

        if (lType == Group.LINK_TYPE_HARD)
            type = HDF5Constants.H5L_TYPE_HARD;

        else if (lType == Group.LINK_TYPE_SOFT)
            type = HDF5Constants.H5L_TYPE_SOFT;

        else if (lType == Group.LINK_TYPE_EXTERNAL)
            type = HDF5Constants.H5L_TYPE_EXTERNAL;

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
    // Consider changing to call SWT Tree reload
    //public void reloadTree(Group g) {
    //    if (fid < 0 || rootNode == null || g == null)
    //        return;

    //    HObject theObj = null;
    //    DefaultMutableTreeNode theNode = null;

    //    if (g.equals(rootNode.getUserObject()))
    //        theNode = rootNode;
    //    else {
    //        Enumeration<?> local_enum = rootNode.breadthFirstEnumeration();
    //        while (local_enum.hasMoreElements()) {
    //            theNode = (DefaultMutableTreeNode) local_enum.nextElement();
    //            theObj = (HObject) theNode.getUserObject();
    //            if (g.equals(theObj))
    //                break;
    //        }
    //    }

    //    theNode.removeAllChildren();
    //    depth_first(theNode, Integer.MIN_VALUE);
    //}

    /*
     * (non-Javadoc) NOTE: Object references are copied but not updated by this method.
     *
     * @see hdf.object.FileFormat#copy(hdf.object.HObject, hdf.object.Group, java.lang.String)
     */
    @Override
    public H5Group copy(HObject srcObj, Group dstGroup, String dstName) throws Exception {
        if ((srcObj == null) || (dstGroup == null)) {
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

        if (srcObj instanceof Dataset) {
            copyDataset((Dataset) srcObj, (H5Group) dstGroup, dstName);
        }
        else if (srcObj instanceof H5Group) {
            copyGroup((H5Group) srcObj, (H5Group) dstGroup, dstName);
        }
        else if (srcObj instanceof H5Datatype) {
            copyDatatype((H5Datatype) srcObj, (H5Group) dstGroup, dstName);
        }

        return (H5Group) dstGroup;
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.FileFormat#delete(hdf.object.HObject)
     */
    @Override
    public void delete(HObject obj) throws Exception {
        if ((obj == null) || (fid < 0)) {
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
        String obj_name = obj.getFullName();
        String name = attr.getName();
        int tid = -1, sid = -1, aid = -1;
        log.trace("{} writeAttribute start", name);

        int objID = obj.open();
        if (objID < 0) {
            return;
        }

        if ((tid = attr.getType().toNative()) >= 0) {
            log.trace("{} writeAttribute tid from native", name);
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
                log.trace("{} writeAttribute aid opened/created", name);

                // update value of the attribute
                Object attrValue = attr.getValue();
                log.trace("{} writeAttribute getvalue", name);
                if (attrValue != null) {
                    boolean isVlen = (H5.H5Tget_class(tid) == HDF5Constants.H5T_VLEN || H5.H5Tis_variable_str(tid));
                    if (isVlen) {
                        log.trace("{} writeAttribute isvlen", name);
                        try {
                            /*
                             * must use native type to write attribute data to file (see bug 1069)
                             */
                            int tmptid = tid;
                            tid = H5.H5Tget_native_type(tmptid);
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (Exception ex) {
                                log.debug("{} writeAttribute H5Tclose failure: ", name, ex);
                            }
                            log.trace("{} writeAttribute H5.H5AwriteVL", name);
                            if ((attrValue instanceof String) || (attr.getDataDims().length == 1)) {
                                H5.H5AwriteVL(aid, tid, (String[]) attrValue);
                            }
                            else {
                                log.info("Datatype is not a string, unable to write {} data", name);
                            }
                        }
                        catch (Exception ex) {
                            log.debug("{} writeAttribute native type failure: ", name, ex);
                        }
                    }
                    else {
                        if (attr.getType().getDatatypeClass() == Datatype.CLASS_REFERENCE && attrValue instanceof String) {
                            // reference is a path+name to the object
                            attrValue = H5.H5Rcreate(getFID(), (String) attrValue, HDF5Constants.H5R_OBJECT, -1);
                            log.trace("{} writeAttribute CLASS_REFERENCE", name);
                        }
                        else if (Array.get(attrValue, 0) instanceof String) {
                            int size = H5.H5Tget_size(tid);
                            int len = ((String[]) attrValue).length;
                            byte[] bval = Dataset.stringToByte((String[]) attrValue, size);
                            if (bval != null && bval.length == size * len) {
                                bval[bval.length - 1] = 0;
                                attrValue = bval;
                            }
                            log.trace("{} writeAttribute Array", name);
                        }

                        try {
                            /*
                             * must use native type to write attribute data to file (see bug 1069)
                             */
                            int tmptid = tid;
                            tid = H5.H5Tget_native_type(tmptid);
                            try {
                                H5.H5Tclose(tmptid);
                            }
                            catch (Exception ex) {
                                log.debug("{} writeAttribute H5Tclose failure: ", name, ex);
                            }
                            log.trace("{} writeAttribute H5.H5Awrite", name);
                            H5.H5Awrite(aid, tid, attrValue);
                        }
                        catch (Exception ex) {
                            log.debug("{} writeAttribute native type failure: ", name, ex);
                        }
                    }
                } // if (attrValue != null) {
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("{} writeAttribute H5Tclose failure: ", name, ex);
                }
                try {
                    H5.H5Sclose(sid);
                }
                catch (Exception ex) {
                    log.debug("{} writeAttribute H5Sclose failure: ", name, ex);
                }
                try {
                    H5.H5Aclose(aid);
                }
                catch (Exception ex) {
                    log.debug("{} writeAttribute H5Aclose failure: ", name, ex);
                }
            }
        }
        else {
            log.debug("{} writeAttribute toNative failure: ", name);
        }

        obj.close(objID);
        log.trace("{} writeAttribute finish", name);
    }

    /***************************************************************************
     * Implementations for methods specific to H5File
     **************************************************************************/

    /**
     * Opens a file with specific file access property list.
     * <p>
     * This function does the same as "int open()" except the you can also pass an HDF5 file access property to file
     * open. For example,
     *
     * <pre>
     * // All open objects remaining in the file are closed then file is closed
     * int plist = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
     * H5.H5Pset_fclose_degree(plist, HDF5Constants.H5F_CLOSE_STRONG);
     * int fid = open(plist);
     * </pre>
     *
     * @param plist
     *            a file access property list identifier.
     * @return the file identifier if successful; otherwise returns negative value.
     */
    public int open(int plist) throws Exception {
        return open(true, plist);
    }

    /***************************************************************************
     * Private methods.
     **************************************************************************/

    /**
     * Opens access to this file.
     *
     * @param loadFullHierarchy
     *            if true, load the full hierarchy into memory; otherwise just opens the file idenfitier.
     * @return the file identifier if successful; otherwise returns negative value.
     */
    private int open(boolean loadFullHierarchy) throws Exception {
        int the_fid = -1;

        int plist = HDF5Constants.H5P_DEFAULT;

        /*
         * // BUG: HDF5Constants.H5F_CLOSE_STRONG does not flush cache try { //All open objects remaining in the file
         * are closed // then file is closed plist = H5.H5Pcreate (HDF5Constants.H5P_FILE_ACCESS);
         * H5.H5Pset_fclose_degree ( plist, HDF5Constants.H5F_CLOSE_STRONG); } catch (Exception ex) {;} the_fid =
         * open(loadFullHierarchy, plist); try { H5.H5Pclose(plist); } catch (Exception ex) {}
         */

        the_fid = open(loadFullHierarchy, plist);

        return the_fid;
    }

    /**
     * Opens access to this file.
     *
     * @param loadFullHierarchy
     *            if true, load the full hierarchy into memory; otherwise just opens the file identifier.
     * @return the file identifier if successful; otherwise returns negative value.
     */
    private int open(boolean loadFullHierarchy, int plist) throws Exception {
        if (fid > 0) {
            return fid; // file is opened already
        }
        log.trace("open: loadFullHierarchy={} start", loadFullHierarchy);

        // The cwd may be changed at Dataset.read() by H5Dchdir_ext()
        // to make it work for external datasets. We need to set it back
        // before the file is closed/opened.
        String rootPath = System.getProperty("hdfview.workdir");
        if (rootPath == null) {
            rootPath = System.getProperty("user.dir");
        }
        H5.H5Dchdir_ext(rootPath);

        // check for valid file access permission
        if (flag < 0) {
            throw new HDF5Exception("Invalid access identifer -- " + flag);
        }
        else if (HDF5Constants.H5F_ACC_CREAT == flag) {
            // create a new file
            log.trace("open: create file");
            fid = H5.H5Fcreate(fullFileName, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT,
                    HDF5Constants.H5P_DEFAULT);
            H5.H5Fflush(fid, HDF5Constants.H5F_SCOPE_LOCAL);
            H5.H5Fclose(fid);
            flag = HDF5Constants.H5F_ACC_RDWR;
        }
        else if (!exists()) {
            throw new HDF5Exception("File does not exist -- " + fullFileName);
        }
        else if (((flag == HDF5Constants.H5F_ACC_RDWR) || (flag == HDF5Constants.H5F_ACC_CREAT)) && !canWrite()) {
            throw new HDF5Exception("Cannot write file, try open as read-only -- " + fullFileName);
        }
        else if ((flag == HDF5Constants.H5F_ACC_RDONLY) && !canRead()) {
            throw new HDF5Exception("Cannot read file -- " + fullFileName);
        }

        try {
            log.trace("open: open file");
            fid = H5.H5Fopen(fullFileName, flag, plist);
        }
        catch (Exception ex) {
            try {
                fid = H5.H5Fopen(fullFileName, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);
                isReadOnly = true;
            }
            catch (Exception ex2) {
                // try to see if it is a file family, always open a family file
                // from the first one since other files will not be recongized
                // as
                // an HDF5 file
                File tmpf = new File(fullFileName);
                String tmpname = tmpf.getName();
                int idx = tmpname.lastIndexOf(".");
                while (idx > 0) {
                    char c = tmpname.charAt(idx);
                    if (c >= '0')
                        idx--;
                    else
                        break;
                }

                if (idx > 0) {
                    tmpname = tmpname.substring(0, idx - 1) + "%d" + tmpname.substring(tmpname.lastIndexOf("."));
                    int pid = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
                    H5.H5Pset_fapl_family(pid, 0, HDF5Constants.H5P_DEFAULT);
                    fid = H5.H5Fopen(tmpf.getParent() + File.separator + tmpname, flag, pid);
                    H5.H5Pclose(pid);
                }
            } /* catch (Exception ex) { */
        }

        if ((fid >= 0) && loadFullHierarchy) {
            // load the hierarchy of the file
            rootObject = loadTree();
        }

        log.trace("open: finish");
        return fid;
    }

    /**
     * Reads the file structure into memory and returns the root object.
     *
     * @return the root object of the file structure.
     */
    private HObject loadTree() {
        if (fid < 0) {
            return null;
        }

        //DefaultMutableTreeNode root = null;

        long[] rootOID = { 0 };
        H5Group rootGroup = new H5Group(this, "/", null, // root node does not
                // have a parent path
                null); // root node does not have a parent node

        //root = new DefaultMutableTreeNode(rootGroup) {
        //    private static final long serialVersionUID = 991382067363411723L;

        //    @Override
        //    public boolean isLeaf() {
        //        return false;
        //    }
        //};

        //depth_first(root, 0); // reload all

        return rootGroup;
    }

    /**
     * Retrieves the file structure by depth-first order, recursively. The current implementation retrieves group and
     * dataset only. It does not include named datatype and soft links.
     * <p>
     * It also detects and stops loops. A loop is detected if there exists object with the same object ID by tracing
     * path back up to the root.
     * <p>
     *
     * @param parentNode
     *            the parent node.
     */
    private int depth_first(HObject parentObject, int nTotal) {
        int nelems;
        //MutableTreeNode node = null;
        String fullPath = null;
        String ppath = null;
        int gid = -1;
        log.trace("depth_first: start");

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
            log.debug("H5Gget_info: ", ex);
        }

        if (nelems <= 0) {
            pgroup.close(gid);
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
            log.trace("depth_first: obj_name={}, obj_type={}", obj_name, obj_type);
            long oid[] = { objRefs[i], fNos[i] };

            if (obj_name == null) {
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
                H5Group g = new H5Group(this, obj_name, fullPath, pgroup, oid); // deprecated!
                //node = new DefaultMutableTreeNode(g) {
                //    private static final long serialVersionUID = 5139629211215794015L;

                //    @Override
                //    public boolean isLeaf() {
                //        return false;
                //    }
                //};
                //pnode.add(node);
                pgroup.addToMemberList(g);

                // detect and stop loops
                // a loop is detected if there exists object with the same
                // object ID by tracing path back up to the root.
                boolean hasLoop = false;
                HObject tmpObj = null;
                //DefaultMutableTreeNode tmpNode = pnode;

                //while (tmpNode != null) {
                //    tmpObj = (HObject) tmpNode.getUserObject();
                //    
                //    if (tmpObj.equalsOID(oid) && !(tmpObj.getPath() == null)) {
                //        hasLoop = true;
                //        break;
                //    }
                //    else {
                //        tmpNode = (DefaultMutableTreeNode) tmpNode.getParent();
                //    }
                //}

                // recursively go through the next group
                // stops if it has loop.
                if (!hasLoop) {
                    //nTotal = depth_first(node, nTotal);
                }
            }
            else if (skipLoad) {
                continue;
            }
            else if (obj_type == HDF5Constants.H5O_TYPE_DATASET) {
                int did = -1, tid = -1, tclass = -1;
                try {
                    did = H5.H5Dopen(fid, fullPath + obj_name, HDF5Constants.H5P_DEFAULT);
                    if (did >= 0) {
                        tid = H5.H5Dget_type(did);

                        tclass = H5.H5Tget_class(tid);
                        if ((tclass == HDF5Constants.H5T_ARRAY) || (tclass == HDF5Constants.H5T_VLEN)) {
                            // for ARRAY, the type is determined by the base type
                            int btid = H5.H5Tget_super(tid);
                            int tmpclass = H5.H5Tget_class(btid);

                            // cannot deal with ARRAY of COMPOUND in compound table
                            // viewer
                            if (tmpclass != HDF5Constants.H5T_COMPOUND)
                                tclass = H5.H5Tget_class(btid);

                            try {
                                H5.H5Tclose(btid);
                            }
                            catch (Exception ex) {
                                log.debug("depth_first[{}] {} dataset access H5Tclose failure: ", i, obj_name, ex);
                            }
                        }
                    }
                    else {
                        log.debug("depth_first[{}] {} dataset open failure", i, obj_name);
                    }
                }
                catch (Exception ex) {
                    log.debug("depth_first[{}] {} dataset access failure: ", i, obj_name, ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tid);
                    }
                    catch (Exception ex) {
                        log.debug("depth_first[{}] {} dataset access H5Tclose failure: ", i, obj_name, ex);
                    }
                    try {
                        H5.H5Dclose(did);
                    }
                    catch (Exception ex) {
                        log.debug("depth_first[{}] {} dataset access H5Dclose failure: ", i, obj_name, ex);
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

                //node = new DefaultMutableTreeNode(d);
                //pnode.add(node);
                pgroup.addToMemberList(d);
            }
            else if (obj_type == HDF5Constants.H5O_TYPE_NAMED_DATATYPE) {
                Datatype t = new H5Datatype(this, obj_name, fullPath, oid); // deprecated!

                //node = new DefaultMutableTreeNode(t);
                //pnode.add(node);
                pgroup.addToMemberList(t);
            }
            else if (obj_type == HDF5Constants.H5O_TYPE_UNKNOWN) {
                H5Link link = new H5Link(this, obj_name, fullPath, oid);

                //node = new DefaultMutableTreeNode(link);
                //pnode.add(node);
                pgroup.addToMemberList(link);
                continue; // do the next one, if the object is not identified.
            }
        } // for ( i = 0; i < nelems; i++)

        pgroup.close(gid);

        log.trace("depth_first: finish");
        return nTotal;
    } // private depth_first()

    /*
    private void depth_first_old(MutableTreeNode parentNode) {
        int nelems;
        MutableTreeNode node = null;
        String fullPath = null;
        String ppath = null;
        DefaultMutableTreeNode pnode = (DefaultMutableTreeNode) parentNode;
        int gid = -1;
        log.trace("depth_first_old: start");

        H5Group pgroup = (H5Group) (pnode.getUserObject());
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
        }

        if (nelems <= 0) {
            pgroup.close(gid);
            return;
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
            ex.printStackTrace();
            return;
        }

        int startIndex = Math.max(0, getStartMembers());
        int endIndex = getMaxMembers();
        if (endIndex >= nelems) {
            endIndex = nelems;
            startIndex = 0; // load all members
        }
        endIndex += startIndex;
        endIndex = Math.min(endIndex, nelems);

        String obj_name;
        int obj_type;
        // int lnk_type;

        // Iterate through the file to see members of the group
        for (int i = startIndex; i < endIndex; i++) {
            obj_name = objNames[i];
            obj_type = objTypes[i];
            log.trace("depth_first_old: obj_name={}, obj_type={}", obj_name, obj_type);
            long oid[] = { objRefs[i], fNos[i] };

            if (obj_name == null) {
                continue;
            }

            // we need to use the OID for this release. we will rewrite this so
            // that we do not use the deprecated constructor
            if (obj_type == HDF5Constants.H5O_TYPE_UNKNOWN) {
                H5Link link = new H5Link(this, obj_name, fullPath, oid);

                node = new DefaultMutableTreeNode(link);
                pnode.add(node);
                pgroup.addToMemberList(link);
                continue; // do the next one, if the object is not identified.
            }

            // create a new group
            if (obj_type == HDF5Constants.H5O_TYPE_GROUP) {
                H5Group g = new H5Group(this, obj_name, fullPath, pgroup, oid); // deprecated!
                node = new DefaultMutableTreeNode(g) {
                    private static final long serialVersionUID = 5139629211215794015L;

                    @Override
                    public boolean isLeaf() {
                        return false;
                    }
                };
                pnode.add(node);
                pgroup.addToMemberList(g);

                // detect and stop loops
                // a loop is detected if there exists object with the same
                // object ID by tracing path back up to the root.
                boolean hasLoop = false;
                HObject tmpObj = null;
                DefaultMutableTreeNode tmpNode = pnode;

                while (tmpNode != null) {
                    tmpObj = (HObject) tmpNode.getUserObject();

                    if (tmpObj.equalsOID(oid)) {
                        hasLoop = true;
                        break;
                    }
                    else {
                        tmpNode = (DefaultMutableTreeNode) tmpNode.getParent();
                    }
                }

                // recursively go through the next group
                // stops if it has loop.
                if (!hasLoop) {
                    depth_first_old(node);
                }
            }
            else if (obj_type == HDF5Constants.H5O_TYPE_DATASET) {
                int did = -1, tid = -1, tclass = -1;
                try {
                    did = H5.H5Dopen(fid, fullPath + obj_name, HDF5Constants.H5P_DEFAULT);
                    if (did >= 0) {
                        tid = H5.H5Dget_type(did);

                        tclass = H5.H5Tget_class(tid);
                        if ((tclass == HDF5Constants.H5T_ARRAY) || (tclass == HDF5Constants.H5T_VLEN)) {
                            // for ARRAY, the type is determined by the base type
                            int btid = H5.H5Tget_super(tid);
                            int tmpclass = H5.H5Tget_class(btid);

                            // cannot deal with ARRAY of COMPOUND in compound table
                            // viewer
                            if (tmpclass != HDF5Constants.H5T_COMPOUND)
                                tclass = H5.H5Tget_class(btid);

                            try {
                                H5.H5Tclose(btid);
                            }
                            catch (Exception ex) {
                                log.debug("depth_first_old[{}] {} dataset access H5Tclose failure: ", i, obj_name, ex);
                            }
                        }
                    }
                    else {
                        log.debug("depth_first_old[{}] {} dataset open failure", i, obj_name);
                    }
                }
                catch (HDF5Exception ex) {
                    log.debug("depth_first_old[{}] {} dataset access failure: ", i, obj_name, ex);
                }
                finally {
                    try {
                        H5.H5Tclose(tid);
                    }
                    catch (Exception ex) {
                        log.debug("depth_first_old[{}] {} dataset access H5Tclose failure: ", i, obj_name, ex);
                    }
                    try {
                        H5.H5Dclose(did);
                    }
                    catch (Exception ex) {
                        log.debug("depth_first_old[{}] {} dataset access H5Tclose failure: ", i, obj_name, ex);
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

                node = new DefaultMutableTreeNode(d);
                pnode.add(node);
                pgroup.addToMemberList(d);
            }
            else if (obj_type == HDF5Constants.H5O_TYPE_NAMED_DATATYPE) {
                Datatype t = new H5Datatype(this, obj_name, fullPath, oid); // deprecated!

                node = new DefaultMutableTreeNode(t);
                pnode.add(node);
                pgroup.addToMemberList(t);
            }
        } // for ( i = 0; i < nelems; i++)

        pgroup.close(gid);
        log.trace("depth_first_old: finish");
    } // private depth_first()
    */

    private void copyDataset(Dataset srcDataset, H5Group pgroup, String dstName) throws Exception {
        Dataset dataset = null;
        int srcdid = -1, dstdid = -1;
        int ocp_plist_id = -1;
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
                log.debug("copyDataset {} failure: ", dname, ex);
            }
            finally {
                try {
                    H5.H5Pclose(ocp_plist_id);
                }
                catch (Exception ex) {
                    log.debug("copyDataset {} H5Pclose failure: ", dname, ex);
                }
            }

            if (srcDataset instanceof H5ScalarDS) {
                dataset = new H5ScalarDS(pgroup.getFileFormat(), dstName, path);
            }
            else {
                dataset = new H5CompoundDS(pgroup.getFileFormat(), dstName, path);
            }

            pgroup.addToMemberList(dataset);
            //newNode = new DefaultMutableTreeNode(dataset);
        }
        finally {
            try {
                srcDataset.close(srcdid);
            }
            catch (Exception ex) {
                log.debug("copyDataset {} srcDataset.close failure: ", dname, ex);
            }
            try {
                pgroup.close(dstdid);
            }
            catch (Exception ex) {
                log.debug("copyDataset {} pgroup.close failure: ", dname, ex);
            }
        }
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
     * @return the dataset if successful; otherwise return null.
     * @throws HDF5Exception
     */
    private Dataset getDataset(int did, String name, String path) throws HDF5Exception {
        Dataset dataset = null;
        if (did >= 0) {
            int tid = -1, tclass = -1;
            try {
                tid = H5.H5Dget_type(did);
                tclass = H5.H5Tget_class(tid);
                if (tclass == HDF5Constants.H5T_ARRAY) {
                    // for ARRAY, the type is determined by the base type
                    int btid = H5.H5Tget_super(tid);
                    tclass = H5.H5Tget_class(btid);
                    try {
                        H5.H5Tclose(btid);
                    }
                    catch (Exception ex) {
                        log.debug("getDataset {} H5Tclose failure: ", name, ex);
                    }
                }
            }
            finally {
                try {
                    H5.H5Tclose(tid);
                }
                catch (Exception ex) {
                    log.debug("getDataset {} H5Tclose failure: ", name, ex);
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
            log.debug("getDataset id failure");
        }

        return dataset;
    }

    /**
     * Copies a named datatype to another location
     *
     * @param srcType
     *            the source datatype
     * @param pgroup
     *            the group which the new datatype is copied to
     * @param dstName
     *            the name of the new dataype
     * @return the tree node containing the new datatype.
     * @throws Exception
     */
    private void copyDatatype(Datatype srcType, H5Group pgroup, String dstName) throws Exception {
        Datatype datatype = null;
        int tid_src = -1, gid_dst = -1;
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
                log.debug("copyDatatype {} H5Ocopy failure: ", dstName, ex);
            }
            datatype = new H5Datatype(pgroup.getFileFormat(), dstName, path);

            pgroup.addToMemberList(datatype);
            //newNode = new DefaultMutableTreeNode(datatype);
        }
        finally {
            try {
                srcType.close(tid_src);
            }
            catch (Exception ex) {
                log.debug("copyDatatype {} srcType.close failure: ", dstName, ex);
            }
            try {
                pgroup.close(gid_dst);
            }
            catch (Exception ex) {
                log.debug("copyDatatype {} pgroup.close failure: ", dstName, ex);
            }
        }
    }

    /**
     * Copies a group and its members to a new location
     *
     * @param srcGroup
     *            the source group
     * @param dstGroup
     *            the location where the new group is located
     * @param dstName
     *            the name of the new group
     * @return the tree node containing the new group;
     */
    private void copyGroup(H5Group srcGroup, H5Group dstGroup, String dstName) throws Exception {
        H5Group group = null;
        int srcgid = -1, dstgid = -1;
        String gname = null, path = null;

        if (dstGroup.isRoot()) {
            path = HObject.separator;
        }
        else {
            path = dstGroup.getPath() + dstGroup.getName() + HObject.separator;
        }

        if ((dstName == null) || dstName.equals(HObject.separator) || (dstName.length() < 1)) {
            dstName = srcGroup.getName();
        }

        gname = path + dstName;

        try {
            srcgid = srcGroup.open();
            dstgid = dstGroup.open();
            try {
                H5.H5Ocopy(srcgid, ".", dstgid, dstName, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            }
            catch (Exception ex) {
                log.debug("copyGroup {} H5Ocopy failure: ", dstName, ex);
            }

            group = new H5Group(dstGroup.getFileFormat(), dstName, path, dstGroup);
            //newNode = new DefaultMutableTreeNode(group) {
            //    private static final long serialVersionUID = -4981107816640372359L;

            //    @Override
            //    public boolean isLeaf() {
            //        return false;
            //    }
            //};
            //depth_first(newNode, Integer.MIN_VALUE); // reload all
            dstGroup.addToMemberList(group);
        }

        finally {
            try {
                srcGroup.close(srcgid);
            }
            catch (Exception ex) {
                log.debug("copyGroup {} srcGroup.close failure: ", dstName, ex);
            }
            try {
                dstGroup.close(dstgid);
            }
            catch (Exception ex) {
                log.debug("copyGroup {} pgroup.close failure: ", dstName, ex);
            }
        }
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
     * @return The group if successful; otherwise returns false.
     * @throws HDF5Exception
     */
    private H5Group getGroup(int gid, String name, Group pGroup) throws HDF5Exception {
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

        H5Group group = new H5Group(this, name, parentPath, pGroup);

        H5G_info_t group_info = null;
        H5O_info_t obj_info = null;
        int oid = -1;
        String link_name = null;
        try {
            group_info = H5.H5Gget_info(gid);
        }
        catch (Exception ex) {
            log.debug("getGroup {} H5Gget_info failure: ", name, ex);
        }
        try {
            oid = H5.H5Oopen(gid, thisFullName, HDF5Constants.H5P_DEFAULT);
        }
        catch (Exception ex) {
            log.debug("getGroup {} H5Oopen failure: ", name, ex);
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
                log.debug("getGroup[{}] {} name,info failure: ", i, name, ex);
                // do not stop if accessing one member fails
                continue;
            }
            // create a new group
            if (obj_info.type == HDF5Constants.H5O_TYPE_GROUP) {
                H5Group g = new H5Group(this, link_name, thisFullName, group);
                group.addToMemberList(g);
            }
            else if (obj_info.type == HDF5Constants.H5O_TYPE_DATASET) {
                int did = -1;
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
                        log.debug("getGroup[{}] {} H5Dclose failure: ", i, name, ex);
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
            log.debug("getGroup {} H5Oclose failure: ", name, ex);
        }
        return group;
    }

    /**
     * Retrieves the name of the target object that is being linked to.
     *
     * @param obj
     *            The current link object.
     * @return The name of the target object.
     * @throws HDF5Exception
     */
    public static String getLinkTargetName(HObject obj) throws Exception {
        String[] link_value = { null, null };
        String targetObjName = null;

        if (obj == null) {
            return null;
        }

        if (obj.getFullName().equals("/")) {
            return null;
        }

        H5L_info_t link_info = null;
        try {
            link_info = H5.H5Lget_info(obj.getFID(), obj.getFullName(), HDF5Constants.H5P_DEFAULT);
        }
        catch (Throwable err) {
            log.debug("H5Lget_info {} failure: ", obj.getFullName());
            log.trace("H5Lget_info {} failure: ", obj.getFullName(), err);
        }
        if (link_info != null) {
            if ((link_info.type == HDF5Constants.H5L_TYPE_SOFT) || (link_info.type == HDF5Constants.H5L_TYPE_EXTERNAL)) {
                try {
                    H5.H5Lget_val(obj.getFID(), obj.getFullName(), link_value, HDF5Constants.H5P_DEFAULT);
                }
                catch (Exception ex) {
                    log.debug("H5Lget_val {} failure: ", obj.getFullName(), ex);
                }
                if (link_info.type == HDF5Constants.H5L_TYPE_SOFT)
                    targetObjName = link_value[0];
                else if (link_info.type == HDF5Constants.H5L_TYPE_EXTERNAL) {
                    targetObjName = link_value[1] + FileFormat.FILE_OBJ_SEP + link_value[0];
                }
            }
        }
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
     * @throws Exception
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
     * @throws HDF5Exception
     */
    public void renameAttribute(HObject obj, String oldAttrName, String newAttrName) throws Exception {
        log.trace("renameAttribute {} to {}", oldAttrName, newAttrName);
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
     * @throws Exception
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
