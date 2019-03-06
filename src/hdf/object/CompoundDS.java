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

/**
 * A CompoundDS is a dataset with compound datatype.
 * <p>
 * A compound datatype is an aggregation of one or more datatypes. Each member
 * of a compound type has a name which is unique within that type, and a
 * datatype of that member in a compound datum. Compound datatypes can be nested,
 * i.e. members of a compound datatype can be some other compound datatype.
 * <p>
 * For more details on compound datatypes,
 * see <b> <a href="https://support.hdfgroup.org/HDF5/doc/UG/HDF5_Users_Guide-Responsive%20HTML5/index.html">HDF5 User's Guide</a> </b>
 * <p>
 * Since Java cannot handle C-structured compound data, data in a compound dataset
 * is loaded in to an Java List. Each element of the list is a data array that
 * corresponds to a compound field. The data is read/written by compound field.
 * <p>
 * For example, if compound dataset "comp" has the following nested structure,
 * and member datatypes
 *
 * <pre>
 * comp --&gt; m01 (int)
 * comp --&gt; m02 (float)
 * comp --&gt; nest1 --&gt; m11 (char)
 * comp --&gt; nest1 --&gt; m12 (String)
 * comp --&gt; nest1 --&gt; nest2 --&gt; m21 (long)
 * comp --&gt; nest1 --&gt; nest2 --&gt; m22 (double)
 * </pre>
 *
 * The data object is a Java list of six arrays: {int[], float[], char[],
 * Stirng[], long[] and double[]}.
 *
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public abstract class CompoundDS extends Dataset implements CompoundDataFormat {
    private static final long serialVersionUID = -4880399929644095662L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompoundDS.class);

    /**
     * A single character to separate the names of nested compound fields. An
     * extended ASCII character, 0x95, is used to avoid common characters in
     * compound names.
     */
    public static final String SEPARATOR = "\u0095";

    /**
     * The number of members of the compound dataset.
     */
    protected int numberOfMembers;

    /**
     * The names of members of the compound dataset.
     */
    protected String[] memberNames;

    /**
     * Returns array containing the total number of elements of the members of
     * this compound dataset.
     * <p>
     * For example, a compound dataset COMP has members of A, B and C as
     *
     * <pre>
     *     COMP {
     *         int A;
     *         float B[5];
     *         double C[2][3];
     *     }
     * </pre>
     *
     * memberOrders is an integer array of {1, 5, 6} to indicate that member A
     * has one element, member B has 5 elements, and member C has 6 elements.
     */
    protected int[] memberOrders;

    /**
     * The dimension sizes of each member.
     * <p>
     * The i-th element of the Object[] is an integer array (int[]) that
     * contains the dimension sizes of the i-th member.
     */
    protected transient Object[] memberDims;

    /**
     * The datatypes of compound members.
     */
    protected Datatype[] memberTypes;

    /**
     * The array to store flags to indicate if a member of this compound
     * dataset is selected for read/write.
     * <p>
     * If a member is selected, the read/write will perform on the member.
     * Applications such as HDFView will only display the selected members of
     * the compound dataset.
     *
     * <pre>
     * For example, if a compound dataset has four members
     *     String[] memberNames = {"X", "Y", "Z", "TIME"};
     * and
     *     boolean[] isMemberSelected = {true, false, false, true};
     * members "X" and "TIME" are selected for read and write.
     * </pre>
     */
    protected boolean[] isMemberSelected;

    /**
     * Constructs a CompoundDS object with the given file, dataset name and path.
     * <p>
     * The dataset object represents an existing dataset in the file. For
     * example, new H5CompoundDS(file, "dset1", "/g0/") constructs a dataset
     * object that corresponds to the dataset, "dset1", at group "/g0/".
     * <p>
     * This object is usually constructed at FileFormat.open(), which loads the
     * file structure and object information into memory. It is rarely used
     * elsewhere.
     *
     * @param theFile
     *            the file that contains the dataset.
     * @param dsName
     *            the name of the CompoundDS, e.g. "compDS".
     * @param dsPath
     *            the full path of the CompoundDS, e.g. "/g1".
     */
    public CompoundDS(FileFormat theFile, String dsName, String dsPath) {
        this(theFile, dsName, dsPath, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #CompoundDS(FileFormat, String, String)}
     *
     * @param theFile
     *            the file that contains the dataset.
     * @param dsName
     *            the name of the CompoundDS, e.g. "compDS".
     * @param dsPath
     *            the full path of the CompoundDS, e.g. "/g1".
     * @param oid
     *            the oid of the CompoundDS.
     */
    @Deprecated
    public CompoundDS(FileFormat theFile, String dsName, String dsPath, long[] oid) {
        super(theFile, dsName, dsPath, oid);

        numberOfMembers = 0;
        memberNames = null;
        isMemberSelected = null;
        memberTypes = null;
    }

    /**
     * Returns the number of members of the compound dataset.
     *
     * @return the number of members of the compound dataset.
     */
    @Override
    public final int getMemberCount() {
        return numberOfMembers;
    }

    /**
     * Returns the number of selected members of the compound dataset.
     *
     * Selected members are the compound fields which are selected for
     * read/write.
     * <p>
     * For example, in a compound datatype of {int A, float B, char[] C},
     * users can choose to retrieve only {A, C} from the dataset. In this
     * case, getSelectedMemberCount() returns two.
     *
     * @return the number of selected members.
     */
    @Override
    public final int getSelectedMemberCount() {
        int count = 0;

        if (isMemberSelected != null) {
            for (int i = 0; i < isMemberSelected.length; i++) {
                if (isMemberSelected[i]) {
                    count++;
                }
            }
        }
        log.trace("count of selected members={}", count);

        return count;
    }

    /**
     * Returns the names of the members of the compound dataset. The names of
     * compound members are stored in an array of Strings.
     * <p>
     * For example, for a compound datatype of {int A, float B, char[] C}
     * getMemberNames() returns ["A", "B", "C"}.
     *
     * @return the names of compound members.
     */
    @Override
    public final String[] getMemberNames() {
        return memberNames;
    }

    /**
     * Returns an array of the names of the selected members of the compound dataset.
     *
     * @return an array of the names of the selected members of the compound dataset.
     */
    public final String[] getSelectedMemberNames() {
        if (isMemberSelected == null) {
            log.debug("getSelectedMemberNames(): isMemberSelected array is null");
            log.trace("getSelectedMemberNames(): finish");
            return memberNames;
        }

        int idx = 0;
        String[] names = new String[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i]) {
                names[idx++] = memberNames[i];
            }
        }

        return names;
    }

    /**
     * Checks if a member of the compound dataset is selected for read/write.
     *
     * @param idx
     *            the index of compound member.
     *
     * @return true if the i-th memeber is selected; otherwise returns false.
     */
    @Override
    public final boolean isMemberSelected(int idx) {
        if ((isMemberSelected != null) && (isMemberSelected.length > idx)) {
            return isMemberSelected[idx];
        }
        else {
            return false;
        }
    }

    /**
     * Selects the i-th member for read/write.
     *
     * @param idx
     *            the index of compound member.
     */
    @Override
    public final void selectMember(int idx) {
        if ((isMemberSelected != null) && (isMemberSelected.length > idx)) {
            isMemberSelected[idx] = true;
        }
    }

    /**
     * Selects/deselects all members.
     *
     * @param selectAll
     *            The indicator to select or deselect all members. If true, all
     *            members are selected for read/write. If false, no member is
     *            selected for read/write.
     */
    @Override
    public final void setAllMemberSelection(boolean selectAll) {
        if (isMemberSelected == null) {
            return;
        }

        for (int i = 0; i < isMemberSelected.length; i++) {
            isMemberSelected[i] = selectAll;
        }
    }

    /**
     * Returns array containing the total number of elements of the members of
     * the compound dataset.
     * <p>
     * For example, a compound dataset COMP has members of A, B and C as
     *
     * <pre>
     *     COMP {
     *         int A;
     *         float B[5];
     *         double C[2][3];
     *     }
     * </pre>
     *
     * getMemberOrders() will return an integer array of {1, 5, 6} to indicate
     * that member A has one element, member B has 5 elements, and member C has
     * 6 elements.
     *
     * @return the array containing the total number of elements of the members
     *         of compound.
     */
    @Override
    public final int[] getMemberOrders() {
        return memberOrders;
    }

    /**
     * Returns array containing the total number of elements of the selected
     * members of the compound dataset.
     *
     * <p>
     * For example, a compound dataset COMP has members of A, B and C as
     *
     * <pre>
     *     COMP {
     *         int A;
     *         float B[5];
     *         double C[2][3];
     *     }
     * </pre>
     *
     * If A and B are selected, getSelectedMemberOrders() returns an array of
     * {1, 5}
     *
     * @return array containing the total number of elements of the selected
     *         members of compound.
     */
    @Override
    public final int[] getSelectedMemberOrders() {
        log.trace("getSelectedMemberOrders(): start");

        if (isMemberSelected == null) {
            log.debug("getSelectedMemberOrders(): isMemberSelected array is null");
            log.trace("getSelectedMemberOrders(): finish");
            return memberOrders;
        }

        int idx = 0;
        int[] orders = new int[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i]) {
                orders[idx++] = memberOrders[i];
            }
        }

        log.trace("getSelectedMemberOrders(): finish");

        return orders;
    }

    /**
     * Returns the dimension sizes of the i-th member.
     * <p>
     * For example, a compound dataset COMP has members of A, B and C as
     *
     * <pre>
     *     COMP {
     *         int A;
     *         float B[5];
     *         double C[2][3];
     *     }
     * </pre>
     *
     * getMemberDims(2) returns an array of {2, 3}, while getMemberDims(1)
     * returns an array of {5}, and getMemberDims(0) returns null.
     *
     * @param i  the i-th member
     *
     * @return the dimension sizes of the i-th member, null if the compound
     *         member is not an array.
     */
    @Override
    public final int[] getMemberDims(int i) {
        if (memberDims == null) {
            return null;
        }
        return (int[]) memberDims[i];
    }

    /**
     * Returns an array of datatype objects of compound members.
     * <p>
     * Each member of a compound dataset has its own datatype. The datatype of a
     * member can be atomic or other compound datatype (nested compound).
     * Sub-classes set up the datatype objects at init().
     * <p>
     *
     * @return the array of datatype objects of the compound members.
     */
    @Override
    public final Datatype[] getMemberTypes() {
        return memberTypes;
    }

    /**
     * Returns an array of datatype objects of selected compound members.
     *
     * @return an array of datatype objects of selected compound members.
     */
    @Override
    public final Datatype[] getSelectedMemberTypes() {
        log.trace("getSelectedMemberTypes(): start");

        if (isMemberSelected == null) {
            log.debug("getSelectedMemberTypes(): isMemberSelected array is null");
            log.trace("getSelectedMemberTypes(): finish");
            return memberTypes;
        }

        int idx = 0;
        Datatype[] types = new Datatype[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i]) {
                types[idx++] = memberTypes[i];
            }
        }

        log.trace("getSelectedMemberTypes(): finish");

        return types;
    }

    /**
     * @deprecated Not implemented for compound dataset.
     */
    @Deprecated
    @Override
    public Dataset copy(Group pgroup, String name, long[] dims, Object data)
            throws Exception {
        throw new UnsupportedOperationException(
                "Writing a subset of a compound dataset to a new dataset is not implemented.");
    }
}
