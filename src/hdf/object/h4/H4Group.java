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

import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFException;
import hdf.hdflib.HDFLibrary;
import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * An H4Group is a vgroup in HDF4, inheriting from Group.
 * A vgroup is a structure designed to associate related data objects. The
 * general structure of a vgroup is similar to that of the UNIX file system in
 * that the vgroup may contain references to other vgroups or HDF data objects
 * just as the UNIX directory may contain subdirectories or files.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class H4Group extends Group
{
    private static final long               serialVersionUID = 3785240955078867900L;

    private final static org.slf4j.Logger   log = org.slf4j.LoggerFactory.getLogger(H4Group.class);

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of Attribute.
     */
    @SuppressWarnings("rawtypes")
    private List                            attributeList;

    private int                             nAttributes = -1;

    /** The default object ID for HDF4 objects */
    private final static long[]             DEFAULT_OID = {0, 0};

    public H4Group(FileFormat theFile, String name, String path, Group parent)
    {
        this(theFile, name, path, parent, null);
    }

    /**
     * Creates a group object with specific name, path, and parent.
     *
     * @param theFile the HDF file.
     * @param name the name of this group.
     * @param path the full path of this group.
     * @param parent the parent of this group.
     * @param oid the unique identifier of this data object.
     */
    public H4Group(
        FileFormat theFile,
        String name,
        String path,
        Group parent,
        long[] oid)
    {
        super (theFile, name, path, parent, ((oid == null) ? DEFAULT_OID : oid));
    }

    /*
     * (non-Javadoc)
     * @see hdf.object.DataFormat#hasAttribute()
     */
    public boolean hasAttribute ()
    {
        if (nAttributes < 0) {
            long vgid = open();
            
            if (vgid > 0) {
                try {
                    nAttributes = HDFLibrary.Vnattrs(vgid);
                    nMembersInFile = HDFLibrary.Vntagrefs(vgid);
                }
                catch (Exception ex) {
                    log.debug("hasAttribute(): failure: ", ex);
                    nAttributes = 0;
                }
                
                log.trace("hasAttribute(): nAttributes={}", nAttributes);

                close(vgid);
            }
        }

        return (nAttributes > 0);
    }

    // Implementing DataFormat
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getMetadata() throws HDFException
    {
        log.trace("getMetadata(): start");
        
        if (attributeList != null) {
            log.trace("getMetadata(): attributeList != null");
            log.trace("getMetadata(): finish");
            return attributeList;
        }
        else {
            attributeList = new Vector();
        }
        
        // Library methods cannot be called on HDF4 dummy root group since it has a ref of 0
        if (oid[1] > 0) {
            long vgid = open();
            log.trace("getMetadata(): open: id={}", vgid);
            if (vgid < 0) {
                log.debug("getMetadata(): Invalid VG ID");
                log.trace("getMetadata(): finish");
                return attributeList;
            }

            int n = -1;

            try {
                n = HDFLibrary.Vnattrs(vgid);
                log.trace("getMetadata(): Vnattrs: n={}", n);

                boolean b = false;
                String[] attrName = new String[1];
                int[] attrInfo = new int[5];
                for (int i=0; i<n; i++) {
                    attrName[0] = "";
                    try {
                        b = HDFLibrary.Vattrinfo(vgid, i, attrName, attrInfo);
                        // mask off the litend bit
                        attrInfo[0] = attrInfo[0] & (~HDFConstants.DFNT_LITEND);
                    }
                    catch (HDFException ex) {
                        log.trace("getMetadata(): attribute[{}] Vattrinfo failure: ", i, ex);
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
                        HDFLibrary.Vgetattr(vgid, i, buf);
                    }
                    catch (HDFException ex) {
                        log.trace("getMetadata(): attribute[{}] Vgetattr failure: ", i, ex);
                        buf = null;
                    }

                    if (buf != null) {
                        if ((attrInfo[0] == HDFConstants.DFNT_CHAR) ||
                                (attrInfo[0] ==  HDFConstants.DFNT_UCHAR8)) {
                            buf = Dataset.byteToString((byte[])buf, attrInfo[1]);
                        }

                        attr.setValue(buf);
                    }
                }
            }
            catch (Exception ex) {
                log.trace("getMetadata(): failure: ", ex);
            }
            finally {
                close(vgid);
            }
        }

        log.trace("getMetadata(): finish");
        return attributeList;
    }

    // To do: implementing DataFormat
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


    // To do: implementing DataFormat
    public void removeMetadata(Object info) throws HDFException {
        log.trace("removeMetadata(): disabled");
    }

    // implementing DataFormat
    public void updateMetadata(Object info) throws Exception {
        log.trace("updateMetadata(): disabled");
    }

    // Implementing HObject
    @Override
    public long open()
    {
        log.trace("open(): start: for file={} with ref={}", getFID(), oid[1]);
        
        if (oid[1] <= 0) {
            log.debug("open(): oid[1] <= 0");
            log.trace("open(): finish");
            return -1; // Library methods cannot be called on HDF4 dummy group with ref 0
        }
        
        long vgid = -1;

        // try to open with write permission
        try {
            vgid = HDFLibrary.Vattach(getFID(), (int)oid[1], "w");
            log.trace("open(): Vattach write id={}", vgid);
        }
        catch (HDFException ex) {
            log.debug("open(): Vattach failure: ", ex);
            vgid = -1;
        }

        // try to open with read-only permission
        if (vgid < 0) {
            try {
                vgid = HDFLibrary.Vattach(getFID(), (int)oid[1], "r");
                log.trace("open(): Vattach readonly id={}", vgid);
            }
            catch (HDFException ex) {
                log.debug("open(): Vattach failure: ", ex);
                vgid = -1;
            }
        }

        log.trace("open(): finish");
        return vgid;
    }

    /** close group access. */
    @Override
    public void close(long vgid)
    {
        log.trace("close(): id={}", vgid);

        if (vgid >= 0) {
            try {
                HDFLibrary.Vdetach(vgid);
            }
            catch (Exception ex) {
                log.debug("close(): Vdetach failure: ", ex);
            }
        }
    }

    /**
     * Creates a new group.
     *
     * @param name the name of the group to create.
     * @param pgroup the parent group of the new group.
     *
     * @return the new group if successful. Otherwise returns null.
     *
     * @throws Exception if the group can not be created
     */
    public static H4Group create(String name, Group pgroup)
        throws Exception
    {
        log.trace("create(): start: name={} parentGroup={}", name, pgroup);
        
        H4Group group = null;
        if ((pgroup == null) ||
            (name == null)) {
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
        long fileid = file.open();
        if (fileid < 0) {
            log.debug("create(): Invalid File ID");
            log.trace("create(): finish");
            return null;
        }

        long gid = HDFLibrary.Vattach(fileid, -1, "w");
        if (gid < 0) {
            log.debug("create(): Invalid Group ID");
            log.trace("create(): finish");
            return null;
        }

        HDFLibrary.Vsetname(gid, name);
        int ref = HDFLibrary.VQueryref(gid);
        int tag = HDFLibrary.VQuerytag(gid);

        if (!pgroup.isRoot()) {
            // add the dataset to the parent group
            long pid = pgroup.open();
            if (pid < 0) {
                log.debug("create(): Invalid Parent Group ID");
                log.trace("create(): finish");
                throw (new HDFException("Unable to open the parent group."));
            }

            HDFLibrary.Vinsert(pid, gid);

            pgroup.close(pid);
        }

        try {
            HDFLibrary.Vdetach(gid);
        }
        catch (Exception ex) {
            log.debug("create(): Vdetach failure: ", ex);
        }

        long[] oid = {tag, ref};
        group = new H4Group(file, name, path, pgroup, oid);

        if (group != null) {
            pgroup.addToMemberList(group);
        }

        log.trace("create(): finish");
        return group;
    }

    //Implementing DataFormat
    @SuppressWarnings("rawtypes")
    public List getMetadata(int... attrPropList) throws Exception {
        throw new UnsupportedOperationException("getMetadata(int... attrPropList) is not supported");
    }
}
