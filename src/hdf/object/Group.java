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

package hdf.object;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;

/**
 * Group is an abstract class. Current implementing classes are the H4Group and
 * H5Group. This class includes general information of a group object such as
 * members of a group and common operations on groups.
 * <p>
 * Members of a group may include other groups, datasets or links.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public abstract class Group extends HObject implements MetaDataContainer {

    private static final long serialVersionUID = 3913174542591568052L;

    /**
     * The list of members (Groups and Datasets) of this group in memory.
     */
    private List<HObject> memberList;

    /**
     * The parent group where this group is located. The parent of the root
     * group is null.
     */
    protected Group parent;

    /**
     * Total number of members of this group in file.
     */
    protected int nMembersInFile;

    public static final int LINK_TYPE_HARD = 0;

    public static final int LINK_TYPE_SOFT = 1;

    public static final int LINK_TYPE_EXTERNAL = 64;

    public static final int CRT_ORDER_TRACKED = 1;

    public static final int CRT_ORDER_INDEXED = 2;


    /**
     * Constructs an instance of the group with specific name, path and parent
     * group. An HDF data object must have a name. The path is the group path
     * starting from the root. The parent group is the group where this group is
     * located.
     * <p>
     * For example, in H5Group(h5file, "grp", "/groups/", pgroup), "grp" is the
     * name of the group, "/groups/" is the group path of the group, and pgroup
     * is the group where "grp" is located.
     *
     * @param theFile
     *            the file containing the group.
     * @param grpName
     *            the name of this group, e.g. "grp01".
     * @param grpPath
     *            the full path of this group, e.g. "/groups/".
     * @param grpParent
     *            the parent of this group.
     */
    public Group(FileFormat theFile, String grpName, String grpPath, Group grpParent) {
        this(theFile, grpName, grpPath, grpParent, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #Group(FileFormat, String, String, Group)}
     *
     * @param theFile
     *            the file containing the group.
     * @param grpName
     *            the name of this group, e.g. "grp01".
     * @param grpPath
     *            the full path of this group, e.g. "/groups/".
     * @param grpParent
     *            the parent of this group.
     * @param oid
     *            the oid of this group.
     */
    @Deprecated
    public Group(FileFormat theFile, String grpName, String grpPath, Group grpParent, long[] oid) {
        super(theFile, grpName, grpPath, oid);

        this.parent = grpParent;
    }

    /**
     * Clears up member list and other resources in memory for the group. Since
     * the destructor will clear memory space, the function is usually not
     * needed.
     */
    public void clear() {
        if (memberList != null) {
            ((Vector<HObject>) memberList).setSize(0);
        }
    }

    /**
     * Adds an object to the member list of this group in memory.
     *
     * @param object
     *            the HObject to be added to the member list.
     */
    public void addToMemberList(HObject object) {
        if (memberList == null) {
            int size = Math.min(getNumberOfMembersInFile(), this
                    .getFileFormat().getMaxMembers());
            memberList = new Vector<>(size + 5);
        }

        if ((object != null) && !memberList.contains(object)) {
            memberList.add(object);
        }
    }

    /**
     * Removes an object from the member list of this group in memory.
     *
     * @param object
     *            the HObject (Group or Dataset) to be removed from the member
     *            list.
     */
    public void removeFromMemberList(HObject object) {
        if (memberList != null) {
            memberList.remove(object);
        }
    }

    /**
     * Returns the list of members of this group. The list is an java.util.List
     * containing HObjects.
     *
     * @return the list of members of this group.
     */
    public List<HObject> getMemberList() {
        FileFormat theFile = this.getFileFormat();

        if ((memberList == null) && (theFile != null)) {
            int size = Math.min(getNumberOfMembersInFile(), this.getFileFormat().getMaxMembers());
            memberList = new Vector<>(size + 5); // avoid infinite loop search for groups without members

            // find the memberList from the file by checking the group path and
            // name. group may be created out of the structure tree
            // (H4/5File.loadTree()).
            if (theFile.getFID() < 0) {
                try {
                    theFile.open();
                } // load the file structure;
                catch (Exception ex) {
                    ;
                }
            }

            HObject root = theFile.getRootObject();
            if (root == null) return memberList;

            Iterator<HObject> it = ((Group) root).depthFirstMemberList().iterator();
            Group g = null;
            Object uObj = null;
            while (it.hasNext()) {
                uObj = it.next();

                if (uObj instanceof Group) {
                    g = (Group) uObj;
                    if (g.getPath() != null) // add this check to get rid of null exception
                    {
                        if ((this.isRoot() && g.isRoot())
                                || (this.getPath().equals(g.getPath()) &&
                                        g.getName().endsWith(this.getName()))) {
                            memberList = g.getMemberList();
                            break;
                        }
                    }
                }
            }
        }

        return memberList;
    }

    /**
     * @return the members of this Group in breadth-first order.
     */
    public List<HObject> breadthFirstMemberList() {
        Vector<HObject> members = new Vector<>();
        Queue<HObject> queue = new LinkedList<>();
        HObject currentObj = this;

        queue.addAll(((Group) currentObj).getMemberList());

        while(!queue.isEmpty()) {
            currentObj = queue.remove();
            members.add(currentObj);

            if(currentObj instanceof Group && ((Group) currentObj).getNumberOfMembersInFile() > 0) {
                queue.addAll(((Group) currentObj).getMemberList());
            }
        }

        return members;
    }

    /**
     * @return the members of this Group in depth-first order.
     */
    public List<HObject> depthFirstMemberList() {
        Vector<HObject> members = new Vector<>();
        Stack<HObject> stack = new Stack<>();
        HObject currentObj = this;

        // Push elements onto the stack in reverse order
        List<HObject> list = ((Group) currentObj).getMemberList();
        for(int i = list.size() - 1; i >= 0; i--) {
            stack.push(list.get(i));
        }

        while(!stack.empty()) {
            currentObj = stack.pop();
            members.add(currentObj);

            if(currentObj instanceof Group && ((Group) currentObj).getNumberOfMembersInFile() > 0) {
                list = ((Group) currentObj).getMemberList();
                for(int i = list.size() - 1; i >= 0; i--) {
                    stack.push(list.get(i));
                }
            }
        }

        return members;
    }

    /**
     * Sets the name of the group.
     * <p>
     * setName (String newName) changes the name of the group in memory and
     * file.
     * <p>
     * setName() updates the path in memory for all the objects that are under
     * the group with the new name.
     *
     * @param newName
     *            The new name of the group.
     *
     * @throws Exception if the name can not be set
     */
    @Override
    public void setName(String newName) throws Exception {
        super.setName(newName);

        if (memberList != null) {
            int n = memberList.size();
            HObject theObj = null;
            for (int i = 0; i < n; i++) {
                theObj = memberList.get(i);
                theObj.setPath(this.getPath() + newName + HObject.SEPARATOR);
            }
        }
    }

    /** @return the parent group. */
    public final Group getParent() {
        return parent;
    }

    /**
     * Checks if it is a root group.
     *
     * @return true if the group is a root group; otherwise, returns false.
     */
    public final boolean isRoot() {
        return (parent == null);
    }

    /**
     * Returns the total number of members of this group in file.
     *
     * Current Java applications such as HDFView cannot handle files with large
     * numbers of objects (1,000,000 or more objects) due to JVM memory
     * limitation. The max_members is used so that applications such as HDFView
     * will load up to <i>max_members</i> number of objects. If the number of
     * objects in file is larger than <i>max_members</i>, only
     * <i>max_members</i> are loaded in memory.
     * <p>
     * getNumberOfMembersInFile() returns the number of objects in this group.
     * The number of objects in memory is obtained by getMemberList().size().
     *
     * @return Total number of members of this group in the file.
     */
    public int getNumberOfMembersInFile() {
        return nMembersInFile;
    }

    /**
     * Get the HObject at the specified index in this Group's member list.
     * @param idx The index of the HObject to get.
     * @return The HObject at the specified index.
     */
    public HObject getMember(int idx) {
        if(memberList.size() <= 0 || idx >= memberList.size()) return null;

        return memberList.get(idx);
    }
}
