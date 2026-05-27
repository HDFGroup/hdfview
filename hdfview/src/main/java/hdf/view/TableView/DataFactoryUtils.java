/*****************************************************************************
 * Copyright by The HDF Group.                                               *
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

package hdf.view.TableView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import hdf.object.CompoundDataFormat;
import hdf.object.Datatype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class containing utility functions for the various DataXXXFactory classes,
 * such as DataProviderFactory, DataDisplayConverterFactory and
 * DataValidatorFactory.
 *
 * @author Jordan T. Henderson
 * @version 1.0 2/21/2019
 *
 */
public class DataFactoryUtils {
    private static final Logger log = LoggerFactory.getLogger(DataFactoryUtils.class);

    /** the error string value. */
    public static final String errStr = "*ERROR*";
    /** the null sting value. */
    public static final String nullStr = "Null";

    /** the COL_TO_BASE_CLASS_MAP_INDEX value. */
    public static final int COL_TO_BASE_CLASS_MAP_INDEX = 0;
    /** the CMPD_START_IDX_MAP_INDEX value. */
    public static final int CMPD_START_IDX_MAP_INDEX = 1;

    /**
     * Number of flat leaf names a Datatype contributes to the flat-name list
     * produced by H5Datatype.extractCompoundInfo. The rules:
     *   compound        - sum of children's counts (no header entry)
     *   array(compound) - 1 header + sum of inner compound's children's counts
     *   array(atomic)   - 1
     *   vlen(any)       - 1 (header only; vlen recursion is disabled)
     *   atomic, varstr  - 1
     */
    public static int countLeafNames(Datatype t)
    {
        if (t == null)
            return 1;
        if (t.isCompound()) {
            int sum                 = 0;
            List<Datatype> children = t.getCompoundMemberTypes();
            if (children != null)
                for (Datatype child : children)
                    sum += countLeafNames(child);
            return sum;
        }
        if (t.isArray()) {
            Datatype base = t.getDatatypeBase();
            if (base != null && base.isCompound()) {
                int sum = 1;
                for (Datatype child : base.getCompoundMemberTypes())
                    sum += countLeafNames(child);
                return sum;
            }
            return 1;
        }
        return 1;
    }

    /**
     * Column-expansion width for a vlen member, looked up by Datatype identity.
     * Returns 1 for non-vlen types, varstr, or types not present in the map.
     */
    public static int getVlenExpansion(Map<Datatype, Integer> vlenMaxLens, Datatype dtype)
    {
        if (dtype == null || !dtype.isVLEN() || dtype.isVarStr() || vlenMaxLens == null)
            return 1;
        Integer n = vlenMaxLens.get(dtype);
        return (n == null || n.intValue() < 1) ? 1 : n.intValue();
    }

    /**
     * Walk just-read compound data and return the max vlen length per vlen member.
     * Identity-keyed so structurally identical Datatypes at different positions can
     * carry different widths.
     */
    public static IdentityHashMap<Datatype, Integer> computeVlenMaxLens(List<Datatype> selectedTypes,
                                                                        Object dataValue)
    {
        IdentityHashMap<Datatype, Integer> out = new IdentityHashMap<>();
        if (!(dataValue instanceof List) || selectedTypes == null)
            return out;

        List<?> cols = (List<?>)dataValue;
        for (int i = 0; i < selectedTypes.size() && i < cols.size(); i++) {
            Datatype t = selectedTypes.get(i);
            if (t == null || !t.isVLEN() || t.isVarStr())
                continue;

            Object col = cols.get(i);
            int max    = 0;
            if (col instanceof ArrayList[]) {
                for (ArrayList<?> row : (ArrayList[])col) {
                    if (row != null && row.size() > max)
                        max = row.size();
                }
            }
            else if (col != null && col.getClass().isArray()) {
                int len = Array.getLength(col);
                for (int j = 0; j < len; j++) {
                    Object row = Array.get(col, j);
                    if (row instanceof ArrayList<?> al && al.size() > max)
                        max = al.size();
                    else if (row != null && row.getClass().isArray()) {
                        int sz = Array.getLength(row);
                        if (sz > max)
                            max = sz;
                    }
                }
            }
            out.put(t, Integer.valueOf(max));
        }
        return out;
    }

    /**
     * Given a CompoundDataFormat, as well as a compound datatype, removes the
     * non-selected datatypes from the List of datatypes inside the compound
     * datatype and returns that as a new List.
     *
     * @param dataFormat
     *        the compound data object
     * @param compoundType
     *        the datatype instance of the compound data object
     *
     * @return the list of datatypes in the compound data object
     */
    public static List<Datatype> filterNonSelectedMembers(CompoundDataFormat dataFormat,
                                                          final Datatype compoundType)
    {
        return filterNonSelectedMembers(dataFormat, compoundType, true);
    }

    /**
     * Variant of {@link #filterNonSelectedMembers(CompoundDataFormat, Datatype)} that
     * skips the selected-member filter for inner (non-top-level) compounds.
     *
     * The dataset's flat selected-member list only enumerates top-level leaves; inner
     * compound members can't be individually deselected. Filtering an inner compound
     * against that list would spuriously remove every non-compound member.
     */
    public static List<Datatype> filterNonSelectedMembers(CompoundDataFormat dataFormat,
                                                          final Datatype compoundType, boolean isTopLevel)
    {
        List<Datatype> selectedTypes = new ArrayList<>(compoundType.getCompoundMemberTypes());
        if (!isTopLevel)
            return selectedTypes;

        List<Datatype> allSelectedTypes = Arrays.asList(dataFormat.getSelectedMemberTypes());
        if (allSelectedTypes == null) {
            log.debug("filterNonSelectedMembers(): selected compound member datatype list is null");
            return null;
        }

        Iterator<Datatype> localIt = selectedTypes.iterator();
        while (localIt.hasNext()) {
            Datatype curType = localIt.next();
            if (curType.isCompound())
                continue;
            if (!allSelectedTypes.contains(curType))
                localIt.remove();
        }

        return selectedTypes;
    }

    /**
     * build the index maps compound types.
     *
     * @param dataFormat
     *        the compound data object
     * @param localSelectedTypes
     *        the list of datatypes of the compound data object
     *
     * @return the map of datatypes in the compound data object
     *
     * @throws Exception if a failure occurred
     */
    @SuppressWarnings("unchecked")
    public static HashMap<Integer, Integer>[] buildIndexMaps(CompoundDataFormat dataFormat,
                                                             List<Datatype> localSelectedTypes)
        throws Exception
    {
        Map<Datatype, Integer> vlenMaxLens;
        try {
            vlenMaxLens = computeVlenMaxLens(localSelectedTypes, dataFormat.getData());
        }
        catch (Exception ex) {
            log.debug("buildIndexMaps(): vlen length scan failed, defaulting to 1 per vlen: ", ex);
            vlenMaxLens = new IdentityHashMap<>();
        }

        HashMap<Integer, Integer>[] maps  = new HashMap[2];
        maps[COL_TO_BASE_CLASS_MAP_INDEX] = new HashMap<>();
        maps[CMPD_START_IDX_MAP_INDEX]    = new HashMap<>();

        buildColIdxToProviderMap(maps[COL_TO_BASE_CLASS_MAP_INDEX], dataFormat, localSelectedTypes,
                                 vlenMaxLens, new int[] {0}, new int[] {0}, 0);
        buildRelColIdxToStartIdxMap(maps[CMPD_START_IDX_MAP_INDEX], dataFormat, localSelectedTypes,
                                    vlenMaxLens, new int[] {0}, new int[] {0}, 0);

        return maps;
    }

    /*
     * Recursive routine to build a mapping between physical column indices and
     * indices into the base HDFDataProvider array. For example, consider the
     * following compound datatype:
     *
     *  ___________________________________
     * |             Compound              |
     * |___________________________________|
     * |     |     |    Compound     |     |
     * | int | int |_________________| int |
     * |     |     | int | int | int |     |
     * |_____|_____|_____|_____|_____|_____|
     *
     * The CompoundDataProvider would have 4 base HDFDataProviders:
     *
     * [NumericalDataProvider, NumericalDataProvider, CompoundDataProvider, NumericalDataProvider]
     *
     * and the mapping between physical column indices and this array would be:
     *
     * (0=0, 1=1, 2=2, 3=2, 4=2, 5=3)
     *
     * For the nested CompoundDataProvider, the mapping would simply be:
     *
     * (0=0, 1=1, 2=2)
     */
    private static void buildColIdxToProviderMap(HashMap<Integer, Integer> outMap,
                                                 CompoundDataFormat dataFormat,
                                                 List<Datatype> localSelectedTypes,
                                                 Map<Datatype, Integer> vlenMaxLens, int[] curMapIndex,
                                                 int[] curProviderIndex, int depth) throws Exception
    {
        for (int i = 0; i < localSelectedTypes.size(); i++) {
            Datatype curType = localSelectedTypes.get(i);
            log.trace("buildColIdxToStartIdxMap(): curType[{}]={}", i, curType);
            Datatype nestedCompoundType = null;
            int arrSize                 = 1;

            if (curType.isArray()) {
                long[] arrayDims = curType.getArrayDims();
                for (int j = 0; j < arrayDims.length; j++) {
                    arrSize *= arrayDims[j];
                }
                log.trace("buildColIdxToStartIdxMap(): arrSize={}", arrSize);

                /*
                 * Recursively detect any nested array/vlen of compound types.
                 */
                Datatype base = curType.getDatatypeBase();
                while (base != null) {
                    if (base.isCompound()) {
                        nestedCompoundType = base;
                        break;
                    }
                    else if (base.isArray()) {
                        arrayDims = base.getArrayDims();
                        for (int j = 0; j < arrayDims.length; j++) {
                            arrSize *= arrayDims[j];
                        }
                    }

                    base = base.getDatatypeBase();
                }
                log.trace("buildColIdxToStartIdxMap(): arrSize after base={}", arrSize);
            }
            else if (curType.isVLEN() && !curType.isVarStr()) {
                // Only a top-level vlen-of-compound expands into its inner compound's
                // leaves; inner vlens contribute a single column.
                arrSize       = getVlenExpansion(vlenMaxLens, curType);
                Datatype base = curType.getDatatypeBase();
                if (depth == 0 && base != null && base.isCompound())
                    nestedCompoundType = base;
            }

            if (nestedCompoundType != null) {
                List<Datatype> cmpdSelectedTypes =
                    filterNonSelectedMembers(dataFormat, nestedCompoundType, false);

                /*
                 * For Array/Vlen of Compound types, we repeat the compound members n times,
                 * where n is the number of array elements of variable-length elements.
                 * Therefore, we repeat our mapping for these types n times.
                 */
                for (int j = 0; j < arrSize; j++) {
                    buildColIdxToProviderMap(outMap, dataFormat, cmpdSelectedTypes, vlenMaxLens, curMapIndex,
                                             curProviderIndex, depth + 1);
                }
            }
            else if (curType.isCompound()) {
                List<Datatype> cmpdSelectedTypes = filterNonSelectedMembers(dataFormat, curType, false);

                buildColIdxToProviderMap(outMap, dataFormat, cmpdSelectedTypes, vlenMaxLens, curMapIndex,
                                         curProviderIndex, depth + 1);
            }
            else if (curType.isVLEN() && !curType.isVarStr()) {
                for (int j = 0; j < arrSize; j++)
                    outMap.put(curMapIndex[0]++, curProviderIndex[0]);
            }
            else
                outMap.put(curMapIndex[0]++, curProviderIndex[0]);

            if (depth == 0)
                curProviderIndex[0]++;
        }
    }

    /*
     * Recursive routine to build a mapping between relative indices in a compound
     * type and the relative index of the first member of the nested compound that
     * index belongs to. For example, consider the following compound datatype:
     *
     *  ___________________________________________________________
     * |                         Compound                          |
     * |___________________________________________________________|
     * |   Compound   |   Compound   |   Compound   |   Compound   |
     * |______________|______________|______________|______________|
     * | int |  float | int |  float | int |  float | int |  float |
     * |_____|________|_____|________|_____|________|_____|________|
     *
     * The top-level mapping between relative compound offsets and the relative
     * index of the first member of the nested compound would look like:
     *
     * (0=0, 1=0, 2=2, 3=2, 4=4, 5=4, 6=6, 7=6)
     *
     * Each of the nested Compound types would have the same mapping of:
     *
     * (0=0, 1=1)
     *
     * As the above mapping for the nested Compound types shows, when the member
     * in question is not part of a further nested compound, its index is simply its
     * offset, as in the following compound type:
     *
     *  ____________________________
     * |          Compound          |
     * |____________________________|
     * |     |       |   Compound   |
     * | int | float |______________|
     * |     |       | int | float  |
     * |_____|_______|_____|________|
     *
     * The top-level mapping would be:
     *
     * (0=0, 1=1, 2=2, 3=2)
     *
     * and the mapping for the nested compound would be:
     *
     * (0=0, 1=1)
     */
    private static void buildRelColIdxToStartIdxMap(HashMap<Integer, Integer> outMap,
                                                    CompoundDataFormat dataFormat,
                                                    List<Datatype> localSelectedTypes,
                                                    Map<Datatype, Integer> vlenMaxLens, int[] curMapIndex,
                                                    int[] curStartIdx, int depth) throws Exception
    {
        for (int i = 0; i < localSelectedTypes.size(); i++) {
            Datatype curType = localSelectedTypes.get(i);
            log.trace("buildRelColIdxToStartIdxMap(): curType[{}]={}", i, curType);
            Datatype nestedCompoundType = null;
            int arrSize                 = 1;

            if (curType.isArray()) {
                long[] arrayDims = curType.getArrayDims();
                for (int j = 0; j < arrayDims.length; j++) {
                    arrSize *= arrayDims[j];
                }
                log.trace("buildRelColIdxToStartIdxMap(): arrSize={}", arrSize);

                /*
                 * Recursively detect any nested array/vlen of compound types.
                 */
                Datatype base = curType.getDatatypeBase();
                while (base != null) {
                    if (base.isCompound()) {
                        nestedCompoundType = base;
                        break;
                    }
                    else if (base.isArray()) {
                        arrayDims = base.getArrayDims();
                        for (int j = 0; j < arrayDims.length; j++) {
                            arrSize *= arrayDims[j];
                        }
                    }

                    base = base.getDatatypeBase();
                }
                log.trace("buildRelColIdxToStartIdxMap(): arrSize after base={}", arrSize);
            }
            else if (curType.isVLEN() && !curType.isVarStr()) {
                arrSize       = getVlenExpansion(vlenMaxLens, curType);
                Datatype base = curType.getDatatypeBase();
                if (depth == 0 && base != null && base.isCompound())
                    nestedCompoundType = base;
            }

            if (nestedCompoundType != null) {
                List<Datatype> cmpdSelectedTypes =
                    filterNonSelectedMembers(dataFormat, nestedCompoundType, false);

                /*
                 * For Array/Vlen of Compound types, we repeat the compound members n times,
                 * where n is the number of array elements of variable-length elements.
                 * Therefore, we repeat our mapping for these types n times.
                 */
                for (int j = 0; j < arrSize; j++) {
                    if (depth == 0)
                        curStartIdx[0] = curMapIndex[0];

                    buildRelColIdxToStartIdxMap(outMap, dataFormat, cmpdSelectedTypes, vlenMaxLens,
                                                curMapIndex, curStartIdx, depth + 1);
                }
            }
            else if (curType.isCompound()) {
                if (depth == 0)
                    curStartIdx[0] = curMapIndex[0];

                List<Datatype> cmpdSelectedTypes = filterNonSelectedMembers(dataFormat, curType, false);

                buildRelColIdxToStartIdxMap(outMap, dataFormat, cmpdSelectedTypes, vlenMaxLens, curMapIndex,
                                            curStartIdx, depth + 1);
            }
            else if (curType.isVLEN() && !curType.isVarStr()) {
                for (int j = 0; j < arrSize; j++) {
                    if (depth == 0) {
                        outMap.put(curMapIndex[0], curMapIndex[0]);
                        curMapIndex[0]++;
                    }
                    else
                        outMap.put(curMapIndex[0]++, curStartIdx[0]);
                }
            }
            else {
                if (depth == 0) {
                    outMap.put(curMapIndex[0], curMapIndex[0]);
                    curMapIndex[0]++;
                }
                else
                    outMap.put(curMapIndex[0]++, curStartIdx[0]);
            }
        }
    }

    /**
     * Return true when the datatype tree contains a construct whose table-view
     * write path is not symmetric with the (recently expanded) display path,
     * so a single-cell edit cannot be reliably mapped back to storage.
     */
    public static boolean isUnsafeForWrite(Datatype dtype)
    {
        return isUnsafe(dtype, false);
    }

    private static boolean isUnsafe(Datatype dtype, boolean insideCompound)
    {
        if (dtype == null)
            return false;

        if (dtype.isVLEN() && !dtype.isVarStr())
            return true;

        if (dtype.isArray()) {
            Datatype base = dtype.getDatatypeBase();
            if (base != null && (base.isCompound() || base.isArray()
                                 || (base.isVLEN() && !base.isVarStr())))
                return true;
            return insideCompound;
        }

        if (dtype.isCompound()) {
            if (insideCompound)
                return true;
            List<Datatype> members = dtype.getCompoundMemberTypes();
            if (members != null) {
                for (Datatype m : members) {
                    if (isUnsafe(m, true))
                        return true;
                }
            }
            return false;
        }

        return false;
    }
}
