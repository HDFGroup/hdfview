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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.object.CompoundDataFormat;
import hdf.object.Datatype;

/**
 * A class containing utility functions for the various DataXXXFactory classes,
 * such as DataProviderFactory, DataDisplayConverterFactory and
 * DataValidatorFactory.
 *
 * @author Jordan T. Henderson
 * @version 1.0 2/21/2019
 *
 */
public class DataFactoryUtils
{
    private static final Logger log = LoggerFactory.getLogger(DataFactoryUtils.class);

    /** the error string value */
    public static final String errStr = "*ERROR*";
    /** the null sting value */
    public static final String nullStr = "Null";

    /** the COL_TO_BASE_CLASS_MAP_INDEX value */
    public static final int COL_TO_BASE_CLASS_MAP_INDEX = 0;
    /** the CMPD_START_IDX_MAP_INDEX value */
    public static final int CMPD_START_IDX_MAP_INDEX = 1;

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
    public static List<Datatype> filterNonSelectedMembers(CompoundDataFormat dataFormat, final Datatype compoundType) {
        List<Datatype> allSelectedTypes = Arrays.asList(dataFormat.getSelectedMemberTypes());
        if (allSelectedTypes == null) {
            log.debug("filterNonSelectedMembers(): selected compound member datatype list is null");
            return null;
        }

        /*
         * Make sure to make a copy of the compound datatype's member list, as we will
         * make modifications to the list when members aren't selected.
         */
        List<Datatype> selectedTypes = new ArrayList<>(compoundType.getCompoundMemberTypes());

        /*
         * Among the datatypes within this compound type, only keep the ones that are
         * actually selected in the dataset.
         */
        Iterator<Datatype> localIt = selectedTypes.iterator();
        while (localIt.hasNext()) {
            Datatype curType = localIt.next();

            /*
             * Since the passed in allSelectedMembers list is a flattened out datatype
             * structure, we want to leave the nested compound Datatypes inside our local
             * list of datatypes.
             */
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
    public static HashMap<Integer, Integer>[] buildIndexMaps(CompoundDataFormat dataFormat, List<Datatype> localSelectedTypes) throws Exception {
        HashMap<Integer, Integer>[] maps = new HashMap[2];
        maps[COL_TO_BASE_CLASS_MAP_INDEX] = new HashMap<>();
        maps[CMPD_START_IDX_MAP_INDEX] = new HashMap<>();

        buildColIdxToProviderMap(maps[COL_TO_BASE_CLASS_MAP_INDEX], dataFormat, localSelectedTypes, new int[] { 0 }, new int[] { 0 }, 0);
        buildRelColIdxToStartIdxMap(maps[CMPD_START_IDX_MAP_INDEX], dataFormat, localSelectedTypes, new int[] { 0 }, new int[] { 0 }, 0);

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
    private static void buildColIdxToProviderMap(HashMap<Integer, Integer> outMap, CompoundDataFormat dataFormat,
            List<Datatype> localSelectedTypes, int[] curMapIndex, int[] curProviderIndex, int depth) throws Exception {
        for (int i = 0; i < localSelectedTypes.size(); i++) {
            Datatype curType = localSelectedTypes.get(i);
            log.trace("buildColIdxToStartIdxMap(): curType[{}]={}", i, curType);
            Datatype nestedCompoundType = null;
            int arrSize = 1;

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

            if (nestedCompoundType != null) {
                List<Datatype> cmpdSelectedTypes = filterNonSelectedMembers(dataFormat, nestedCompoundType);

                /*
                 * For Array/Vlen of Compound types, we repeat the compound members n times,
                 * where n is the number of array elements of variable-length elements.
                 * Therefore, we repeat our mapping for these types n times.
                 */
                for (int j = 0; j < arrSize; j++) {
                    buildColIdxToProviderMap(outMap, dataFormat, cmpdSelectedTypes, curMapIndex, curProviderIndex, depth + 1);
                }
            }
            else if (curType.isCompound()) {
                List<Datatype> cmpdSelectedTypes = filterNonSelectedMembers(dataFormat, curType);

                buildColIdxToProviderMap(outMap, dataFormat, cmpdSelectedTypes, curMapIndex, curProviderIndex, depth + 1);
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
    private static void buildRelColIdxToStartIdxMap(HashMap<Integer, Integer> outMap, CompoundDataFormat dataFormat,
            List<Datatype> localSelectedTypes, int[] curMapIndex, int[] curStartIdx, int depth) throws Exception {
        for (int i = 0; i < localSelectedTypes.size(); i++) {
            Datatype curType = localSelectedTypes.get(i);
            log.trace("buildRelColIdxToStartIdxMap(): curType[{}]={}", i, curType);
            Datatype nestedCompoundType = null;
            int arrSize = 1;

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

            if (nestedCompoundType != null) {
                List<Datatype> cmpdSelectedTypes = filterNonSelectedMembers(dataFormat, nestedCompoundType);

                /*
                 * For Array/Vlen of Compound types, we repeat the compound members n times,
                 * where n is the number of array elements of variable-length elements.
                 * Therefore, we repeat our mapping for these types n times.
                 */
                for (int j = 0; j < arrSize; j++) {
                    if (depth == 0)
                        curStartIdx[0] = curMapIndex[0];

                    buildRelColIdxToStartIdxMap(outMap, dataFormat, cmpdSelectedTypes, curMapIndex, curStartIdx, depth + 1);
                }
            }
            else if (curType.isCompound()) {
                if (depth == 0)
                    curStartIdx[0] = curMapIndex[0];

                List<Datatype> cmpdSelectedTypes = filterNonSelectedMembers(dataFormat, curType);

                buildRelColIdxToStartIdxMap(outMap, dataFormat, cmpdSelectedTypes, curMapIndex, curStartIdx, depth + 1);
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

}
