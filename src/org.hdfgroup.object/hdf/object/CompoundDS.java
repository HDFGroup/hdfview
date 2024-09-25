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

package hdf.object;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.Utils;

import hdf.hdf5lib.HDFNativeData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CompoundDS is a dataset with compound datatype.
 *
 * A compound datatype is an aggregation of one or more datatypes. Each member of a compound type has a name
 * which is unique within that type, and a datatype of that member in a compound datum. Compound datatypes can
 * be nested, i.e. members of a compound datatype can be some other compound datatype.
 *
 * For more details on compound datatypes, See <a href=
 * "https://support.hdfgroup.org/releases/hdf5/v1_14/v1_14_5/documentation/doxygen/_h5_t__u_g.html#sec_datatype">HDF5
 * Datatypes in HDF5 User Guide</a>
 *
 * Since Java cannot handle C-structured compound data, data in a compound dataset is loaded in to an Java
 * List. Each element of the list is a data array that corresponds to a compound field. The data is
 * read/written by compound field.
 *
 * For example, if compound dataset "comp" has the following nested structure, and member datatypes
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
 * The data object is a Java list of six arrays: {int[], float[], char[], Stirng[], long[] and double[]}.
 *
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public abstract class CompoundDS extends Dataset implements CompoundDataFormat {
    private static final long serialVersionUID = -4880399929644095662L;

    private static final Logger log = LoggerFactory.getLogger(CompoundDS.class);

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
     *
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
     *
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
     *
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
     * A list of names of all fields including nested fields.
     *
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
    protected List<String> flatNameList;

    /**
     * A list of datatypes of all fields including nested fields.
     */
    protected List<Datatype> flatTypeList;

    /**
     * Constructs a CompoundDS object with the given file, dataset name and path.
     *
     * The dataset object represents an existing dataset in the file. For
     * example, new CompoundDS(file, "dset1", "/g0/") constructs a dataset
     * object that corresponds to the dataset, "dset1", at group "/g0/".
     *
     * This object is usually constructed at FileFormat.open(), which loads the
     * file structure and object information into memory. It is rarely used
     * elsewhere.
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     */
    public CompoundDS(FileFormat theFile, String theName, String thePath)
    {
        this(theFile, theName, thePath, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #CompoundDS(FileFormat, String, String)}
     *
     * @param theFile
     *            the file that contains the data object.
     * @param dsName
     *            the name of the data object, e.g. "dset".
     * @param dsPath
     *            the full path of the data object, e.g. "/arrays/".
     * @param oid
     *            the oid of the data object.
     */
    @Deprecated
    public CompoundDS(FileFormat theFile, String dsName, String dsPath, long[] oid)
    {
        super(theFile, dsName, dsPath, oid);

        numberOfMembers  = 0;
        memberNames      = null;
        isMemberSelected = null;
        memberTypes      = null;
    }

    /**
     * Resets selection of dataspace
     */
    @Override
    protected void resetSelection()
    {
        super.resetSelection();
        setAllMemberSelection(true);
    }

    /**
     * Returns the number of members of the compound dataset.
     *
     * @return the number of members of the compound dataset.
     */
    @Override
    public final int getMemberCount()
    {
        return numberOfMembers;
    }

    /**
     * Returns the number of selected members of the compound dataset.
     *
     * Selected members are the compound fields which are selected for
     * read/write.
     *
     * For example, in a compound datatype of {int A, float B, char[] C},
     * users can choose to retrieve only {A, C} from the dataset. In this
     * case, getSelectedMemberCount() returns two.
     *
     * @return the number of selected members.
     */
    @Override
    public final int getSelectedMemberCount()
    {
        int count = 0;

        if (isMemberSelected != null) {
            for (int i = 0; i < isMemberSelected.length; i++) {
                if (isMemberSelected[i])
                    count++;
            }
        }
        log.trace("count of selected members={}", count);

        return count;
    }

    /**
     * Returns the names of the members of the compound dataset. The names of
     * compound members are stored in an array of Strings.
     *
     * For example, for a compound datatype of {int A, float B, char[] C}
     * getMemberNames() returns ["A", "B", "C"}.
     *
     * @return the names of compound members.
     */
    @Override
    public final String[] getMemberNames()
    {
        return memberNames;
    }

    /**
     * Returns an array of the names of the selected members of the compound dataset.
     *
     * @return an array of the names of the selected members of the compound dataset.
     */
    @Override
    public final String[] getSelectedMemberNames()
    {
        if (isMemberSelected == null) {
            log.debug("getSelectedMemberNames(): isMemberSelected array is null");
            return memberNames;
        }

        int idx        = 0;
        String[] names = new String[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i])
                names[idx++] = memberNames[i];
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
    public final boolean isMemberSelected(int idx)
    {
        if ((isMemberSelected != null) && (isMemberSelected.length > idx))
            return isMemberSelected[idx];
        else
            return false;
    }

    /**
     * Selects the i-th member for read/write.
     *
     * @param idx
     *            the index of compound member.
     */
    @Override
    public final void selectMember(int idx)
    {
        if ((isMemberSelected != null) && (isMemberSelected.length > idx))
            isMemberSelected[idx] = true;
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
    public final void setAllMemberSelection(boolean selectAll)
    {
        if (isMemberSelected == null)
            return;

        for (int i = 0; i < isMemberSelected.length; i++)
            isMemberSelected[i] = selectAll;
    }

    /**
     * Returns array containing the total number of elements of the members of
     * the compound dataset.
     *
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
    public final int[] getMemberOrders()
    {
        return memberOrders;
    }

    /**
     * Returns array containing the total number of elements of the selected
     * members of the compound dataset.
     *
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
    public final int[] getSelectedMemberOrders()
    {
        if (isMemberSelected == null) {
            log.debug("getSelectedMemberOrders(): isMemberSelected array is null");
            return memberOrders;
        }

        int idx      = 0;
        int[] orders = new int[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i])
                orders[idx++] = memberOrders[i];
        }

        return orders;
    }

    /**
     * Returns the dimension sizes of the i-th member.
     *
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
    public final int[] getMemberDims(int i)
    {
        if (memberDims == null) {
            return null;
        }
        return (int[])memberDims[i];
    }

    /**
     * Returns an array of datatype objects of compound members.
     *
     * Each member of a compound dataset has its own datatype. The datatype of a
     * member can be atomic or other compound datatype (nested compound).
     * Sub-classes set up the datatype objects at init().
     *
     * @return the array of datatype objects of the compound members.
     */
    @Override
    public final Datatype[] getMemberTypes()
    {
        return memberTypes;
    }

    /**
     * Returns an array of datatype objects of selected compound members.
     *
     * @return an array of datatype objects of selected compound members.
     */
    @Override
    public final Datatype[] getSelectedMemberTypes()
    {
        if (isMemberSelected == null) {
            log.debug("getSelectedMemberTypes(): isMemberSelected array is null");
            return memberTypes;
        }

        int idx          = 0;
        Datatype[] types = new Datatype[getSelectedMemberCount()];
        for (int i = 0; i < isMemberSelected.length; i++) {
            if (isMemberSelected[i])
                types[idx++] = memberTypes[i];
        }

        return types;
    }

    /**
     * Returns the fill values for the data object.
     *
     * @return the fill values for the data object.
     */
    @Override
    public Object getFillValue()
    {
        return null;
    }

    /**
     * @deprecated Not implemented for compound dataset.
     */
    @Deprecated
    @Override
    public Dataset copy(Group pgroup, String name, long[] dims, Object data) throws Exception
    {
        throw new UnsupportedOperationException(
            "Writing a subset of a compound dataset to a new dataset is not implemented.");
    }

    /**
     * Routine to convert datatypes that are read in as byte arrays to
     * regular types.
     *
     * @param dtype
     *        the datatype to convert to
     * @param byteData
     *        the bytes to convert
     *
     * @return the converted object
     */
    protected Object convertByteMember(final Datatype dtype, byte[] byteData)
    {
        Object theObj = null;
        log.trace("convertByteMember(): byteData={} start", byteData);

        if (dtype.getDatatypeSize() == 1) {
            /*
             * Normal byte[] type, such as an integer datatype of size 1.
             */
            theObj = byteData;
        }
        else if (dtype.isString() && !dtype.isVarStr() && convertByteToString &&
                 (byteData instanceof byte[])) {
            log.trace("convertByteMember(): converting byte array to string array");

            theObj = byteToString(byteData, (int)dtype.getDatatypeSize());
        }
        else if (dtype.isInteger()) {
            log.trace("convertByteMember(): converting byte array to integer array");

            switch ((int)dtype.getDatatypeSize()) {
            case 1:
                /*
                 * Normal byte[] type, such as an integer datatype of size 1.
                 */
                theObj = byteData;
                break;
            case 2:
                theObj = HDFNativeData.byteToShort(byteData);
                break;
            case 4:
                theObj = HDFNativeData.byteToInt(byteData);
                break;
            case 8:
                theObj = HDFNativeData.byteToLong(byteData);
                break;
            default:
                log.debug("convertByteMember(): invalid datatype size");
                theObj = new String("*ERROR*");
                break;
            }
        }
        else if (dtype.isFloat()) {
            log.trace("convertByteMember(): converting byte array to float array");

            if (dtype.getDatatypeSize() == 8)
                theObj = HDFNativeData.byteToDouble(byteData);
            else
                theObj = HDFNativeData.byteToFloat(byteData);
        }
        else if (dtype.isArray()) {
            Datatype baseType = dtype.getDatatypeBase();
            log.trace("convertByteMember(): converting byte array to baseType array");

            /*
             * Retrieve the real base datatype in the case of ARRAY of ARRAY datatypes.
             */
            while (baseType.isArray())
                baseType = baseType.getDatatypeBase();

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

            case Datatype.CLASS_ARRAY: {
                Datatype arrayType = dtype.getDatatypeBase();

                long[] arrayDims = dtype.getArrayDims();
                int arrSize      = 1;
                for (int i = 0; i < arrayDims.length; i++)
                    arrSize *= arrayDims[i];
                log.trace("convertByteMember(): no CLASS_ARRAY arrayType={} arrSize={}", arrayType, arrSize);

                theObj = new Object[arrSize];

                for (int i = 0; i < arrSize; i++) {
                    byte[] indexedBytes = Arrays.copyOfRange(byteData, (int)(i * arrayType.getDatatypeSize()),
                                                             (int)((i + 1) * arrayType.getDatatypeSize()));
                    ((Object[])theObj)[i] = convertByteMember(arrayType, indexedBytes);
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
            log.debug("convertByteMember(): compound datatype class");
            /*
             * TODO: still valid after reading change?
             */
            theObj = convertCompoundByteMembers(dtype, byteData);
        }
        else {
            log.debug("convertByteMember(): byteData={}", byteData);
            theObj = byteData;
        }

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
    protected Object convertCompoundByteMembers(final Datatype dtype, byte[] data)
    {
        List<Object> theData = null;

        List<Datatype> allSelectedTypes = Arrays.asList(this.getSelectedMemberTypes());
        List<Datatype> localTypes       = new ArrayList<>(dtype.getCompoundMemberTypes());
        Iterator<Datatype> localIt      = localTypes.iterator();
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
                theData.add(convertCompoundByteMembers(
                    curType, Arrays.copyOfRange(data, index, index + (int)curType.getDatatypeSize())));
            else
                theData.add(convertByteMember(
                    curType, Arrays.copyOfRange(data, index, index + (int)curType.getDatatypeSize())));

            index += curType.getDatatypeSize();
        }

        return theData;
    }
}
