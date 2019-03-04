/*****************************************************************************
 * Copyright by The HDF Group.                                               *
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

package hdf.view.TableView;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;

import hdf.object.Attribute;
import hdf.object.CompoundDataFormat;
import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.object.Utils;
import hdf.view.Tools;

/**
 * A Factory class to return a concrete class implementing the IDataProvider
 * interface in order to provide data for a NatTable.
 *
 * @author Jordan T. Henderson
 * @version 1.0 2/9/2019
 *
 */
public class DataProviderFactory {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataProviderFactory.class);

    /*
     * To keep things clean from an API perspective, keep a static reference to the last
     * DataFormat that was passed in. This keeps us from needing to pass the DataFormat
     * object as a parameter to every DataProvider class, since it's really only needed
     * during the HDFDataProvider constructor.
     */
    private static DataFormat dataFormatReference = null;

    public static HDFDataProvider getDataProvider(final DataFormat dataObject, final Object dataBuf, final boolean dataTransposed) throws Exception {
        log.trace("getDataProvider(DataFormat): start");

        if (dataObject == null) {
            log.debug("getDataProvider(DataFormat): data object is null");
            log.trace("getDataProvider(DataFormat): finish");
            return null;
        }

        dataFormatReference = dataObject;

        HDFDataProvider dataProvider = getDataProvider(dataObject.getDatatype(), dataBuf, dataTransposed);

        log.trace("getDataProvider(DataFormat): finish");

        return dataProvider;
    }

    private static final HDFDataProvider getDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
        log.trace("getDataProvider(Datatype): start");

        HDFDataProvider dataProvider = null;

        try {
            if (dtype.isCompound()) {
                if (dataFormatReference instanceof Attribute)
                    dataProvider = new CompoundAttributeDataProvider(dtype, dataBuf, dataTransposed);
                else
                    dataProvider = new CompoundDataProvider(dtype, dataBuf, dataTransposed);
            }
            else if (dtype.isArray())
                dataProvider = new ArrayDataProvider(dtype, dataBuf, dataTransposed);
            else if (dtype.isVLEN() && !dtype.isVarStr())
                dataProvider = new VlenDataProvider(dtype, dataBuf, dataTransposed);
            else if (dtype.isString() || dtype.isVarStr())
                dataProvider = new StringDataProvider(dtype, dataBuf, dataTransposed);
            else if (dtype.isChar())
                dataProvider = new CharDataProvider(dtype, dataBuf, dataTransposed);
            else if (dtype.isInteger() || dtype.isFloat())
                dataProvider = new NumericalDataProvider(dtype, dataBuf, dataTransposed);
            else if (dtype.isEnum())
                dataProvider = new EnumDataProvider(dtype, dataBuf, dataTransposed);
            else if (dtype.isOpaque() || dtype.isBitField())
                dataProvider = new BitfieldDataProvider(dtype, dataBuf, dataTransposed);
            else if (dtype.isRef())
                dataProvider = new RefDataProvider(dtype, dataBuf, dataTransposed);
        }
        catch (Exception ex) {
            log.debug("getDataProvider(): error occurred in retrieving a DataProvider: ", ex);
            dataProvider = null;
        }

        /*
         * Try to use a default DataProvider.
         */
        if (dataProvider == null) {
            log.debug("getDataProvider(): using a default data provider");

            dataProvider = new HDFDataProvider(dtype, dataBuf, dataTransposed);
        }

        log.trace("getDataProvider(Datatype): finish");

        return dataProvider;
    }

    /*
     * The base DataProvider which pulls data from a given Array object using direct
     * indices.
     */
    public static class HDFDataProvider implements IDataProvider {

        protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HDFDataProvider.class);

        protected final Object     dataBuf;

        protected Object           theValue;

        protected boolean          isValueChanged;

        protected final boolean    isContainerType;

        protected final int        rank;

        protected final boolean    isNaturalOrder;
        protected final boolean    isDataTransposed;

        protected final long       colCount;
        protected final long       rowCount;

        HDFDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            log.trace("constructor: start");

            this.dataBuf = dataBuf;

            char runtimeTypeClass = Utils.getJavaObjectRuntimeClass(dataBuf);
            if (runtimeTypeClass == ' ') {
                log.debug("invalid data value runtime type class: runtimeTypeClass={}", runtimeTypeClass);
                log.trace("finish");
                throw new IllegalStateException("Invalid data value runtime type class: " + runtimeTypeClass);
            }

            rank = dataFormatReference.getRank();

            isNaturalOrder = ((rank == 1) || (dataFormatReference.getSelectedIndex()[0] < dataFormatReference.getSelectedIndex()[1]));
            isDataTransposed = dataTransposed;

            if (rank > 1) {
                rowCount = dataFormatReference.getHeight();
                colCount = dataFormatReference.getWidth();
            }
            else {
                rowCount = (int) dataFormatReference.getSelectedDims()[0];
                colCount = 1;
            }

            theValue = null;
            isValueChanged = false;

            if (   this instanceof CompoundDataProvider
                || this instanceof ArrayDataProvider
                || this instanceof VlenDataProvider)
                isContainerType = true;
            else
                isContainerType = false;

            log.trace("constructor: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}): start", rowIndex, columnIndex);

            try {
                long index = rowIndex * colCount + columnIndex;

                if (rank > 1) {
                    log.trace("getDataValue({}, {}): rank={} isDataTransposed={} isNaturalOrder={}",
                            rowIndex, columnIndex, rank, isDataTransposed, isNaturalOrder);

                    if (isDataTransposed && isNaturalOrder)
                        index = columnIndex * rowCount + rowIndex;
                    else if (!isDataTransposed && !isNaturalOrder)
                        // Reshape Data
                        index = rowIndex * colCount + columnIndex;
                    else if (isDataTransposed && !isNaturalOrder)
                        // Transpose Data
                        index = columnIndex * rowCount + rowIndex;
                    else
                        index = rowIndex * colCount + columnIndex;
                }

                theValue = Array.get(dataBuf, (int) index);
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {})({}): finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        /*
         * When a CompoundDataProvider wants to pass a List of data down to a nested
         * CompoundDataProvider, or when a top-level container DataProvider (such as an
         * ArrayDataProvider) wants to hand data down to a base CompoundDataProvider, we
         * need to pass down a List of data, plus a field and row index. This method is
         * for facilitating this behavior.
         *
         * In general, all "container" DataProviders that have a "container" base
         * DataProvider should call down into their base DataProvider(s) using this
         * method, in order to ensure that buried CompoundDataProviders get handled
         * correctly. When their base DataProvider is not a "container" type, the method
         * getDataValue(Object, index) should be used instead.
         *
         * For atomic type DataProviders, we treat this method as directly calling into
         * getDataValue(Object, index) using the passed rowIndex. However, this method
         * should, in general, not be called by atomic type DataProviders.
         */
        public Object getDataValue(Object obj, int columnIndex, int rowIndex) {
            return getDataValue(obj, rowIndex);
        }

        /*
         * When a parent HDFDataProvider (such as an ArrayDataProvider) wants to
         * retrieve a data value by routing the operation through its base
         * HDFDataProvider, the parent HDFDataProvider will generally know the direct
         * index to have the base provider use. This method is to facilitate this kind
         * of behavior.
         *
         * Note that this method takes an Object parameter, which is the object that the
         * method should pull its data from. This is to be able to nicely support nested
         * compound DataProviders.
         */
        public Object getDataValue(Object obj, int index) {
            log.trace("getDataValue({}, {}): start", obj, index);

            try {
                theValue = Array.get(obj, index);
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", obj, index, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {})({}): finish", obj, index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("setDataValue({}, {}, {}): start", rowIndex, columnIndex, newValue);

            if ((newValue == null) || ((newValue = ((String) newValue).trim()) == null)) {
                log.debug("setDataValue({}, {}, {}): cell value not updated; new value is null", rowIndex, columnIndex, newValue);
                log.trace("setDataValue({}, {}, {}): exit", rowIndex, columnIndex, newValue);
                return;
            }

            // No need to update if values are the same
            Object oldVal = this.getDataValue(columnIndex, rowIndex);
            if ((oldVal != null) && newValue.equals(oldVal.toString())) {
                log.debug("setDataValue({}, {}, {}): cell value not updated; new value same as old value", rowIndex, columnIndex, newValue);
                log.trace("setDataValue({}, {}, {}): exit", rowIndex, columnIndex, newValue);
                return;
            }

            try {
                long index = rowIndex * colCount + columnIndex;

                if (rank > 1) {
                    log.trace("setDataValue({}, {}, {}): rank={} isDataTransposed={} isNaturalOrder={}", rowIndex,
                            columnIndex, newValue, rank, isDataTransposed, isNaturalOrder);

                    if (isDataTransposed && isNaturalOrder)
                        index = columnIndex * rowCount + rowIndex;
                    else if (!isDataTransposed && !isNaturalOrder)
                        // Reshape Data
                        index = rowIndex * colCount + columnIndex;
                    else if (isDataTransposed && !isNaturalOrder)
                        // Transpose Data
                        index = columnIndex * rowCount + rowIndex;
                    else
                        index = rowIndex * colCount + columnIndex;
                }

                char runtimeTypeClass = Utils.getJavaObjectRuntimeClass(dataBuf);

                log.trace("setDataValue({}, {}, {}): runtimeTypeClass={}", rowIndex, columnIndex, newValue, runtimeTypeClass);

                switch (runtimeTypeClass) {
                    case 'B':
                        byte bvalue = 0;
                        bvalue = Byte.parseByte((String) newValue);
                        Array.setByte(dataBuf, (int) index, bvalue);
                        break;
                    case 'S':
                        short svalue = 0;
                        svalue = Short.parseShort((String) newValue);
                        Array.setShort(dataBuf, (int) index, svalue);
                        break;
                    case 'I':
                        int ivalue = 0;
                        ivalue = Integer.parseInt((String) newValue);
                        Array.setInt(dataBuf, (int) index, ivalue);
                        break;
                    case 'J':
                        long lvalue = 0;
                        /*
                         * TODO:
                         */
                        /* if (dname == 'J') {
                            BigInteger big = new BigInteger((String) newValue);
                            lvalue = big.longValue();
                        }
                        else */
                            lvalue = Long.parseLong((String) newValue);
                        Array.setLong(dataBuf, (int) index, lvalue);
                        break;
                    case 'F':
                        float fvalue = 0;
                        fvalue = Float.parseFloat((String) newValue);
                        Array.setFloat(dataBuf, (int) index, fvalue);
                        break;
                    case 'D':
                        double dvalue = 0;
                        dvalue = Double.parseDouble((String) newValue);
                        Array.setDouble(dataBuf, (int) index, dvalue);
                        break;
                    default:
                        Array.set(dataBuf, (int) index, newValue);
                        break;
                }

                isValueChanged = true;
            }
            catch (Exception ex) {
                log.debug("setDataValue({}, {}, {}): cell value update failure: ", rowIndex, columnIndex, newValue, ex);
            }
            finally {
                log.trace("setDataValue({}, {}, {}): finish", rowIndex, columnIndex, newValue);
            }

            /*
             * TODO: throwing error dialogs when something fails?
             *
             * Tools.showError(shell, "Select", "Unable to set new value:\n\n " + ex);
             */
        }

        /*
         * When a CompoundDataProvider wants to pass a List of data down to a nested
         * CompoundDataProvider, or when a top-level container DataProvider (such as an
         * ArrayDataProvider) wants to hand data down to a base CompoundDataProvider, we
         * need to pass down a List of data and the new value, plus a field and row
         * index. This method is for facilitating this behavior.
         *
         * In general, all "container" DataProviders that have a "container" base
         * DataProvider should call down into their base DataProvider(s) using this
         * method, in order to ensure that buried CompoundDataProviders get handled
         * correctly. When their base DataProvider is not a "container" type, the method
         * setDataValue(index, Object, Object) should be used instead.
         *
         * For atomic type DataProviders, we treat this method as directly calling into
         * setDataValue(index, Object, Object) using the passed rowIndex. However, this
         * method should, in general, not be called by atomic type DataProviders.
         */
        public void setDataValue(int columnIndex, int rowIndex, Object bufObject, Object newValue) {
            setDataValue(rowIndex, bufObject, newValue);
        }

        /*
         * When a parent HDFDataProvider (such as an ArrayDataProvider) wants to set a
         * data value by routing the operation through its base HDFDataProvider, the
         * parent HDFDataProvider will generally know the direct index to have the base
         * provider use. This method is to facilitate this kind of behavior.
         *
         * Note that this method takes two Object parameters, one which is the object
         * that the method should set its data inside of and one which is the new value
         * to set. This is to be able to nicely support nested compound DataProviders.
         */
        public void setDataValue(int index, Object bufObject, Object newValue) {
            log.trace("setDataValue({}, {}, {}): start", index, bufObject, newValue);

            if ((newValue == null) || ((newValue = ((String) newValue).trim()) == null)) {
                log.debug("setDataValue({}, {}, {}): cell value not updated; new value is null", index, bufObject, newValue);
                log.trace("setDataValue({}, {}, {}): finish", index, bufObject, newValue);
                return;
            }

            // No need to update if values are the same
            Object oldVal = this.getDataValue(bufObject, index);
            if ((oldVal != null) && newValue.equals(oldVal.toString())) {
                log.debug("setDataValue({}, {}, {}): cell value not updated; new value same as old value", index, bufObject, newValue);
                log.trace("setDataValue({}, {}, {}): finish", index, bufObject, newValue);
                return;
            }

            try {
                char runtimeTypeClass = Utils.getJavaObjectRuntimeClass(bufObject);

                log.trace("setDataValue({}, {}, {}): runtimeTypeClass={}", index, bufObject, newValue, runtimeTypeClass);

                switch (runtimeTypeClass) {
                    case 'B':
                        byte bvalue = 0;
                        bvalue = Byte.parseByte((String) newValue);
                        Array.setByte(bufObject, index, bvalue);
                        break;
                    case 'S':
                        short svalue = 0;
                        svalue = Short.parseShort((String) newValue);
                        Array.setShort(bufObject, index, svalue);
                        break;
                    case 'I':
                        int ivalue = 0;
                        ivalue = Integer.parseInt((String) newValue);
                        Array.setInt(bufObject, index, ivalue);
                        break;
                    case 'J':
                        long lvalue = 0;
                        /*
                         * TODO:
                         */
                        /* if (dname == 'J') {
                            BigInteger big = new BigInteger((String) newValue);
                            lvalue = big.longValue();
                        }
                        else */
                            lvalue = Long.parseLong((String) newValue);
                        Array.setLong(bufObject, index, lvalue);
                        break;
                    case 'F':
                        float fvalue = 0;
                        fvalue = Float.parseFloat((String) newValue);
                        Array.setFloat(bufObject, index, fvalue);
                        break;
                    case 'D':
                        double dvalue = 0;
                        dvalue = Double.parseDouble((String) newValue);
                        Array.setDouble(bufObject, index, dvalue);
                        break;
                    default:
                        Array.set(bufObject, index, newValue);
                        break;
                }
            }
            catch (Exception ex) {
                log.debug("setDataValue({}, {}, {}): failure: ", index, bufObject, newValue, ex);
            }
            finally {
                log.trace("setDataValue({}, {}, {}): finish", index, bufObject, newValue);
            }
        }

        @Override
        public int getColumnCount() {
            return (int) colCount;
        }

        @Override
        public int getRowCount() {
            return (int) rowCount;
        }

        public final void setIsValueChanged(boolean isChanged) {
            isValueChanged = isChanged;
        }

        public final boolean getIsValueChanged() {
            return isValueChanged;
        }

    }

    /*
     * A DataProvider for Compound datatype datasets which is a composite of
     * DataProviders, one for each selected member of the Compound datatype.
     */
    private static class CompoundDataProvider extends HDFDataProvider {

        private final HashMap<Integer, Integer> baseProviderIndexMap;
        private final HashMap<Integer, Integer> relCmpdStartIndexMap;

        private final HDFDataProvider[]         baseTypeProviders;

        private final Datatype[]                selectedMemberTypes;

        private final int[]                     selectedMemberOrders;

        private final int                       nSubColumns;
        private final int                       nCols;
        private final int                       nRows;

        CompoundDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(CompoundDataProvider.class);

            log.trace("constructor: start");

            CompoundDataFormat compoundFormat = (CompoundDataFormat) dataFormatReference;
            selectedMemberTypes = compoundFormat.getSelectedMemberTypes();
            selectedMemberOrders = compoundFormat.getSelectedMemberOrders();

            List<Datatype> localSelectedTypes = DataFactoryUtils.filterNonSelectedMembers(compoundFormat, dtype);

            log.trace("setting up {} base HDFDataProviders", localSelectedTypes.size());

            baseTypeProviders = new HDFDataProvider[localSelectedTypes.size()];
            for (int i = 0; i < baseTypeProviders.length; i++) {
                log.trace("retrieving DataProvider for member {}", i);

                try {
                    baseTypeProviders[i] = getDataProvider(localSelectedTypes.get(i), dataBuf, dataTransposed);
                }
                catch (Exception ex) {
                    log.debug("failed to retrieve DataProvider for member {}: ", i, ex);
                    baseTypeProviders[i] = null;
                }
            }

            /*
             * Build necessary index maps.
             */
            HashMap<Integer, Integer>[] maps = DataFactoryUtils.buildIndexMaps(compoundFormat, localSelectedTypes);
            baseProviderIndexMap = maps[DataFactoryUtils.COL_TO_BASE_CLASS_MAP_INDEX];
            relCmpdStartIndexMap = maps[DataFactoryUtils.CMPD_START_IDX_MAP_INDEX];

            log.trace("index maps built: baseProviderIndexMap = {}, relColIdxMap = {}",
                    baseProviderIndexMap.toString(), relCmpdStartIndexMap.toString());

            if (baseProviderIndexMap.size() == 0) {
                log.debug("base DataProvider index mapping is invalid - size 0");
                log.trace("constructor: finish");
                throw new Exception("CompoundDataProvider: invalid DataProvider mapping of size 0 built");
            }

            if (relCmpdStartIndexMap.size() == 0) {
                log.debug("compound field start index mapping is invalid - size 0");
                log.trace("constructor: finish");
                throw new Exception("CompoundDataProvider: invalid compound field start index mapping of size 0 built");
            }

            /*
             * nCols should represent the number of columns covered by this CompoundDataProvider
             * only. For top-level CompoundDataProviders, this should be the entire width of the
             * dataset. For nested CompoundDataProviders, nCols will be a subset of these columns.
             */
            nCols = (int) compoundFormat.getWidth() * baseProviderIndexMap.size();
            nRows = (int) compoundFormat.getHeight();

            nSubColumns = (int) compoundFormat.getWidth();

            log.trace("constructor: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}): start", rowIndex, columnIndex);

            try {
                int fieldIdx = columnIndex;
                int rowIdx = rowIndex;

                if (nSubColumns > 1) { // multi-dimension compound dataset
                    /*
                     * Make sure fieldIdx is within a valid range, since even for multi-dimensional
                     * compound datasets there will only be as many lists of data as there are
                     * members in a single compound type.
                     */
                    fieldIdx %= selectedMemberTypes.length;

                    int realColIdx = columnIndex / selectedMemberTypes.length;
                    rowIdx = rowIndex * nSubColumns + realColIdx;
                }

                int providerIndex = baseProviderIndexMap.get(fieldIdx);
                Object colValue = ((List<?>) dataBuf).get(providerIndex);
                if (colValue == null) {
                    return DataFactoryUtils.nullStr;
                }

                /*
                 * Delegate data retrieval to one of the base DataProviders according to the
                 * index of the relevant compound field.
                 */
                HDFDataProvider base = baseTypeProviders[providerIndex];
                if (base.isContainerType)
                    /*
                     * Adjust the compound field index by subtracting the starting index of the
                     * nested compound that we are delegating to. When the nested compound's index
                     * map is setup correctly, this adjusted index should map to the correct field
                     * among the nested compound's members.
                     */
                    theValue = base.getDataValue(colValue, fieldIdx - relCmpdStartIndexMap.get(fieldIdx), rowIdx);
                else
                    theValue = base.getDataValue(colValue, rowIdx);
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {}): finish", rowIndex, columnIndex);

            return theValue;
        }

        @Override
        public Object getDataValue(Object obj, int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}, {}): start", obj, rowIndex, columnIndex);

            try {
                int providerIndex = baseProviderIndexMap.get(columnIndex);
                Object colValue = ((List<?>) obj).get(providerIndex);
                if (colValue == null) {
                    return DataFactoryUtils.nullStr;
                }

                /*
                 * Delegate data retrieval to one of the base DataProviders according to the
                 * index of the relevant compound field.
                 */
                HDFDataProvider base = baseTypeProviders[providerIndex];
                if (base.isContainerType)
                    /*
                     * Adjust the compound field index by subtracting the starting index of the
                     * nested compound that we are delegating to. When the nested compound's index
                     * map is setup correctly, this adjusted index should map to the correct field
                     * among the nested compound's members.
                     */
                    theValue = base.getDataValue(colValue, columnIndex - relCmpdStartIndexMap.get(columnIndex), rowIndex);
                else
                    theValue = base.getDataValue(colValue, rowIndex);
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}, {}): failure: ", obj, rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }
            finally {
                log.trace("getDataValue({}, {}, {}): finish", obj, rowIndex, columnIndex);
            }

            return theValue;
        }

        @Override
        public Object getDataValue(Object obj, int index) {
            throw new UnsupportedOperationException("getDataValue(Object, int) should not be called for CompoundDataProviders");
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("setDataValue({}, {}, {}): start", rowIndex, columnIndex, newValue);

            if ((newValue == null) || ((newValue = ((String) newValue).trim()) == null)) {
                log.debug("setDataValue({}, {}, {}): cell value not updated; new value is null", rowIndex, columnIndex, newValue);
                log.trace("setDataValue({}, {}, {}): exit", rowIndex, columnIndex, newValue);
                return;
            }

            // No need to update if values are the same
            Object oldVal = this.getDataValue(columnIndex, rowIndex);
            if ((oldVal != null) && newValue.equals(oldVal.toString())) {
                log.debug("setDataValue({}, {}, {}): cell value not updated; new value same as old value", rowIndex, columnIndex, newValue);
                log.trace("setDataValue({}, {}, {}): exit", rowIndex, columnIndex, newValue);
                return;
            }

            try {
                int fieldIdx = columnIndex;
                int rowIdx = rowIndex;

                if (nSubColumns > 1) { // multi-dimension compound dataset
                    /*
                     * Make sure fieldIdx is within a valid range, since even for multi-dimensional
                     * compound datasets there will only be as many lists of data as there are
                     * members in a single compound type.
                     */
                    fieldIdx %= selectedMemberTypes.length;

                    int realColIdx = columnIndex / selectedMemberTypes.length;
                    rowIdx = rowIndex * nSubColumns + realColIdx;
                }

                int providerIndex = baseProviderIndexMap.get(fieldIdx);
                Object colValue = ((List<?>) dataBuf).get(providerIndex);
                if (colValue == null) {
                    log.debug("setDataValue({}, {}, {}): colValue is null", rowIndex, columnIndex, newValue);
                    log.trace("setDataValue({}, {}, {}): finish", rowIndex, columnIndex, newValue);
                    return;
                }

                /*
                 * Delegate data setting to one of the base DataProviders according to the index
                 * of the relevant compound field.
                 */
                HDFDataProvider base = baseTypeProviders[providerIndex];
                if (base.isContainerType)
                    /*
                     * Adjust the compound field index by subtracting the starting index of the
                     * nested compound that we are delegating to. When the nested compound's index
                     * map is setup correctly, this adjusted index should map to the correct field
                     * among the nested compound's members.
                     */
                    base.setDataValue(fieldIdx - relCmpdStartIndexMap.get(fieldIdx), rowIdx, colValue, newValue);
                else
                    base.setDataValue(rowIdx, colValue, newValue);

                isValueChanged = true;
            }
            catch (Exception ex) {
                log.debug("setDataValue({}, {}, {}): cell value update failure: ", rowIndex, columnIndex, newValue);
            }
            finally {
                log.trace("setDataValue({}, {}, {}): finish", rowIndex, columnIndex, newValue);
            }

            /*
             * TODO: throwing error dialogs when something fails?
             *
             * Tools.showError(shell, "Select", "Unable to set new value:\n\n " + ex);
             */
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object bufObject, Object newValue) {
            log.trace("setDataValue({}, {}, {}, {}): start", rowIndex, columnIndex, bufObject, newValue);

            try {
                int providerIndex = baseProviderIndexMap.get(columnIndex);
                Object colValue = ((List<?>) bufObject).get(providerIndex);
                if (colValue == null) {
                    log.debug("setDataValue({}, {}, {}, {}): colValue is null", rowIndex, columnIndex, bufObject, newValue);
                    log.trace("setDataValue({}, {}, {}, {}): finish", rowIndex, columnIndex, bufObject, newValue);
                    return;
                }

                /*
                 * Delegate data setting to one of the base DataProviders according to the index
                 * of the relevant compound field.
                 */
                HDFDataProvider base = baseTypeProviders[providerIndex];
                if (base.isContainerType)
                    /*
                     * Adjust the compound field index by subtracting the starting index of the
                     * nested compound that we are delegating to. When the nested compound's index
                     * map is setup correctly, this adjusted index should map to the correct field
                     * among the nested compound's members.
                     */
                    base.setDataValue(columnIndex - relCmpdStartIndexMap.get(columnIndex), rowIndex, colValue, newValue);
                else
                    base.setDataValue(rowIndex, colValue, newValue);

                isValueChanged = true;
            }
            catch (Exception ex) {
                log.debug("setDataValue({}, {}, {}, {}): cell value update failure: ", rowIndex, columnIndex, bufObject, newValue, ex);
            }
            finally {
                log.trace("setDataValue({}, {}, {}, {}): finish", rowIndex, columnIndex, bufObject, newValue);
            }
        }

        @Override
        public void setDataValue(int index, Object bufObject, Object newValue) {
            throw new UnsupportedOperationException("setDataValue(int, Object, Object) should not be called for CompoundDataProviders");
        }

        @Override
        public int getColumnCount() {
            return nCols;
        }

        @Override
        public int getRowCount() {
            return nRows;
        }

    }

    private static class ArrayDataProvider extends HDFDataProvider {

        private final HDFDataProvider baseTypeDataProvider;

        private final Object[]        arrayElements;
        private final long            arraySize;

        private final int             nCols;

        ArrayDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(ArrayDataProvider.class);

            log.trace("constructor: start");

            Datatype baseType = dtype.getDatatypeBase();

            baseTypeDataProvider = getDataProvider(baseType, dataBuf, dataTransposed);

            if (baseType.isVarStr()) {
                arraySize = dtype.getArrayDims()[0];
            }
            else if (baseType.isBitField() || baseType.isOpaque()) {
                arraySize = dtype.getDatatypeSize();
            }
            else {
                arraySize = dtype.getDatatypeSize() / baseType.getDatatypeSize();
            }

            arrayElements = new Object[(int) arraySize];

            if (baseTypeDataProvider instanceof CompoundDataProvider)
                nCols = (int) arraySize * ((CompoundDataProvider) baseTypeDataProvider).nCols;
            else
                nCols = super.getColumnCount();

            log.trace("constructor: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}): start", rowIndex, columnIndex);

            try {
                long index = (rowIndex * colCount + columnIndex) * arraySize;

                if (rank > 1) {
                    log.trace("getDataValue({}, {}): rank={} isDataTransposed={} isNaturalOrder={}",
                            rowIndex, columnIndex, rank, isDataTransposed, isNaturalOrder);

                    if (isDataTransposed && isNaturalOrder)
                        index = (columnIndex * rowCount + rowIndex) * arraySize;
                    else if (!isDataTransposed && !isNaturalOrder)
                        // Reshape Data
                        index = (rowIndex * colCount + columnIndex) * arraySize;
                    else if (isDataTransposed && !isNaturalOrder)
                        // Transpose Data
                        index = (columnIndex * rowCount + rowIndex) * arraySize;
                    else
                        index = (rowIndex * colCount + columnIndex) * arraySize;
                }

                if (baseTypeDataProvider instanceof CompoundDataProvider) {
                    for (int i = 0; i < arraySize; i++) {
                        /*
                         * TODO: still valid?
                         */
                        /* arrayElements[i] = ((Object[]) dataBuf)[i]; */
                        arrayElements[i] = baseTypeDataProvider.getDataValue(dataBuf, columnIndex, (int) index + i);
                    }
                }
                else if (baseTypeDataProvider instanceof ArrayDataProvider) {
                    for (int i = 0; i < arraySize; i++) {
                        arrayElements[i] = baseTypeDataProvider.getDataValue(dataBuf, columnIndex, (int) index + i);
                    }
                }
                else {
                    for (int i = 0; i < arraySize; i++) {
                        arrayElements[i] = baseTypeDataProvider.getDataValue(dataBuf, (int) index + i);
                    }
                }

                theValue = arrayElements;
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {})({}): finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(Object obj, int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}, {}): start", obj, rowIndex, columnIndex);

            /*
             * TODO: can we get away without temp. array? Overwriting problem.
             */
            Object[] tempArray = new Object[(int) arraySize];

            try {
                long index = rowIndex * arraySize;

                if (baseTypeDataProvider instanceof CompoundDataProvider) {
                    /*
                     * Since we flatten array of compound types, we only need to return a single
                     * value.
                     */
                    theValue = new Object[] { baseTypeDataProvider.getDataValue(obj, columnIndex, (int) index) };
                }
                else if (baseTypeDataProvider instanceof ArrayDataProvider) {
                    for (int i = 0; i < arraySize; i++) {
                        tempArray[i] = baseTypeDataProvider.getDataValue(obj, columnIndex, (int) index + i);
                    }

                    theValue = tempArray;
                }
                else {
                    for (int i = 0; i < arraySize; i++) {
                        tempArray[i] = baseTypeDataProvider.getDataValue(obj, (int) index + i);
                    }

                    theValue = tempArray;
                }
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}, {}): failure: ", obj, rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {}, {}): finish", obj, rowIndex, columnIndex);

            return theValue;
        }

        @Override
        public Object getDataValue(Object obj, int index) {
            throw new UnsupportedOperationException("getDataValue(Object, int) should not be called for ArrayDataProviders");
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("setDataValue({}, {}, {}): start", rowIndex, columnIndex, newValue);

            try {
                long index = (rowIndex * colCount + columnIndex) * arraySize;

                if (rank > 1) {
                    log.trace("setDataValue({}, {}, {}): rank={} isDataTransposed={} isNaturalOrder={}",
                            rowIndex, columnIndex, newValue, rank, isDataTransposed, isNaturalOrder);

                    if (isDataTransposed && isNaturalOrder)
                        index = (columnIndex * rowCount + rowIndex) * arraySize;
                    else if (!isDataTransposed && !isNaturalOrder)
                        // Reshape Data
                        index = (rowIndex * colCount + columnIndex) * arraySize;
                    else if (isDataTransposed && !isNaturalOrder)
                        // Transpose Data
                        index = (columnIndex * rowCount + rowIndex) * arraySize;
                    else
                        index = (rowIndex * colCount + columnIndex) * arraySize;
                }

                StringTokenizer st = new StringTokenizer((String) newValue, ",[]");
                if (st.countTokens() < arraySize) {
                    /*
                     * TODO:
                     */
                    /* Tools.showError(shell, "Select", "Number of data points < " + morder + "."); */
                    log.debug("setDataValue({}, {}, {}): number of data points < array size {}", rowIndex, columnIndex, newValue, arraySize);
                    log.trace("setDataValue({}, {}, {}): finish", rowIndex, columnIndex, newValue);
                    return;
                }

                String token = "";

                /*
                 * TODO: should maybe be modified to multiply the index by the arraySize before
                 * handing down?
                 */
                if (baseTypeDataProvider instanceof CompoundDataProvider) {
                    for (int i = 0; i < arraySize; i++) {
                        List<?> cmpdDataList = (List<?>) ((Object[]) dataBuf)[i];
                        token = st.nextToken().trim();
                        baseTypeDataProvider.setDataValue(columnIndex, (int) index + i, cmpdDataList, token);
                    }
                }
                else if (baseTypeDataProvider instanceof ArrayDataProvider) {
                    for (int i = 0; i < arraySize; i++) {
                        token = st.nextToken().trim();
                        baseTypeDataProvider.setDataValue(columnIndex, (int) index + i, dataBuf, token);
                    }
                }
                else {
                    for (int i = 0; i < arraySize; i++) {
                        token = st.nextToken().trim();
                        baseTypeDataProvider.setDataValue((int) index + i, dataBuf, token);
                    }
                }

                isValueChanged = true;
            }
            catch (Exception ex) {
                log.debug("setDataValue({}, {}, {}): cell value update failure: ", rowIndex, columnIndex, newValue, ex);
            }
            finally {
                log.trace("setDataValue({}, {}, {}): finish", rowIndex, columnIndex, newValue);
            }
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object bufObject, Object newValue) {
            log.trace("setDataValue({}, {}, {}, {}): start", rowIndex, columnIndex, bufObject, newValue);

            try {
                long index = rowIndex * arraySize;

                StringTokenizer st = new StringTokenizer((String) newValue, ",[]");
                if (st.countTokens() < arraySize) {
                    /*
                     * TODO:
                     */
                    /* Tools.showError(shell, "Select", "Number of data points < " + morder + "."); */
                    log.debug("setDataValue({}, {}, {}, {}): number of data points < array size {}", rowIndex, columnIndex, bufObject, newValue, arraySize);
                    log.trace("setDataValue({}, {}, {}, {}): finish", rowIndex, columnIndex, bufObject, newValue);
                    return;
                }

                String token = "";

                if (baseTypeDataProvider instanceof CompoundDataProvider) {
                    for (int i = 0; i < arraySize; i++) {
                        List<?> cmpdDataList = (List<?>) ((Object[]) bufObject)[i];
                        token = st.nextToken().trim();
                        baseTypeDataProvider.setDataValue(columnIndex, (int) index + i, cmpdDataList, token);
                    }
                }
                else if (baseTypeDataProvider instanceof ArrayDataProvider) {
                    for (int i = 0; i < arraySize; i++) {
                        token = st.nextToken().trim();
                        baseTypeDataProvider.setDataValue(columnIndex, (int) index + i, bufObject, token);
                    }
                }
                else {
                    for (int i = 0; i < arraySize; i++) {
                        token = st.nextToken().trim();
                        baseTypeDataProvider.setDataValue((int) index + i, bufObject, token);
                    }
                }

                isValueChanged = true;
            }
            catch (Exception ex) {
                log.debug("setDataValue({}, {}, {}, {}): cell value update failure: ", rowIndex, columnIndex, bufObject, newValue, ex);
            }
            finally {
                log.trace("setDataValue({}, {}, {}, {}): finish", rowIndex, columnIndex, bufObject, newValue);
            }
        }

        @Override
        public void setDataValue(int index, Object bufObject, Object newValue) {
            throw new UnsupportedOperationException("setDataValue(int, Object, Object) should not be called for ArrayDataProviders");
        }

        @Override
        public int getColumnCount() {
            return nCols;
        }

    }

    private static class VlenDataProvider extends HDFDataProvider {

        private final HDFDataProvider baseTypeDataProvider;

        private final StringBuilder   buffer;

        VlenDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(VlenDataProvider.class);

            log.trace("constructor: start");

            Datatype baseType = dtype.getDatatypeBase();

            baseTypeDataProvider = getDataProvider(baseType, dataBuf, dataTransposed);

            buffer = new StringBuilder();

            log.trace("constructor: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}): start", rowIndex, columnIndex);

            buffer.setLength(0);

            try {
                long index = (rowIndex * colCount + columnIndex);

                if (rank > 1) {
                    log.trace("getDataValue({}, {}): rank={} isDataTransposed={} isNaturalOrder={}",
                            rowIndex, columnIndex, rank, isDataTransposed, isNaturalOrder);

                    if (isDataTransposed && isNaturalOrder)
                        index = (columnIndex * rowCount + rowIndex);
                    else if (!isDataTransposed && !isNaturalOrder)
                        // Reshape Data
                        index = (rowIndex * colCount + columnIndex);
                    else if (isDataTransposed && !isNaturalOrder)
                        // Transpose Data
                        index = (columnIndex * rowCount + rowIndex);
                    else
                        index = (rowIndex * colCount + columnIndex);
                }

                if (baseTypeDataProvider instanceof CompoundDataProvider) {
                    /*
                     * TODO:
                     */
                    /*
                     * buffer.append(baseTypeDataProvider.getDataValue(dataBuf, columnIndex, (int) index));
                     */
                    if (dataBuf instanceof String[])
                        buffer.append(Array.get(dataBuf, (int) index));
                    else
                        buffer.append("*UNSUPPORTED*");
                }
                else if (baseTypeDataProvider instanceof ArrayDataProvider) {
                    buffer.append(baseTypeDataProvider.getDataValue(dataBuf, columnIndex, (int) index));
                }
                else {
                    buffer.append(baseTypeDataProvider.getDataValue(dataBuf, (int) index));
                }

                theValue = buffer.toString();
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {})({}): finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(Object obj, int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}, {}): start", obj, rowIndex, columnIndex);

            buffer.setLength(0);

            try {
                if (baseTypeDataProvider instanceof CompoundDataProvider) {
                    /*
                     * TODO:
                     */
                    /*
                     * buffer.append(baseTypeDataProvider.getDataValue(obj, columnIndex, rowIndex));
                     */
                    if (obj instanceof String[])
                        buffer.append(Array.get(obj, rowIndex));
                    else
                        buffer.append("*UNSUPPORTED*");
                }
                else if (baseTypeDataProvider instanceof ArrayDataProvider) {
                    buffer.append(baseTypeDataProvider.getDataValue(obj, columnIndex, rowIndex));
                }
                else {
                    buffer.append(baseTypeDataProvider.getDataValue(obj, rowIndex));
                }

                theValue = buffer.toString();
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {}, {})({}): finish", obj, rowIndex, columnIndex, theValue);

            return theValue;
        }

        /* @Override
        public Object getDataValue(Object obj, int index) {
            throw new UnsupportedOperationException("getDataValue(Object, int) should not be called for VlenDataProviders");
        } */

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            /*
             * TODO:
             */
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object bufObject, Object newValue) {
            /*
             * TODO:
             */
        }

        @Override
        public void setDataValue(int index, Object bufObject, Object newValue) {
            throw new UnsupportedOperationException("setDataValue(int, Object, Object) should not be called for VlenDataProviders");
        }

    }

    private static class StringDataProvider extends HDFDataProvider {

        private final long typeSize;

        StringDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(StringDataProvider.class);

            log.trace("constructor: start");

            typeSize = dtype.getDatatypeSize();

            log.trace("constructor: finish");
        }

        @Override
        public Object getDataValue(Object obj, int index) {
            log.trace("getDataValue({}, {}): start", obj, index);

            if (obj instanceof byte[]) {
                int strlen = (int) typeSize;

                log.trace("getDataValue({}, {}): converting byte[] to String", obj, index);

                String str = new String((byte[]) obj, index * strlen, strlen);
                int idx = str.indexOf('\0');
                if (idx > 0) {
                    str = str.substring(0, idx);
                }

                theValue = str.trim();
            }
            else
                super.getDataValue(obj, index);

            log.trace("getDataValue({}, {})({}): finish", obj, index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("setDataValue({}, {}, {}): start", rowIndex, columnIndex, newValue);

            try {
                long index = (rowIndex * colCount + columnIndex);

                if (rank > 1) {
                    log.trace("setDataValue({}, {}, {}): rank={} isDataTransposed={} isNaturalOrder={}",
                            rowIndex, columnIndex, newValue, rank, isDataTransposed, isNaturalOrder);

                    if (isDataTransposed && isNaturalOrder)
                        index = (columnIndex * rowCount + rowIndex);
                    else if (!isDataTransposed && !isNaturalOrder)
                        // Reshape Data
                        index = (rowIndex * colCount + columnIndex);
                    else if (isDataTransposed && !isNaturalOrder)
                        // Transpose Data
                        index = (columnIndex * rowCount + rowIndex);
                    else
                        index = (rowIndex * colCount + columnIndex);
                }

                // String data represented as a byte[]
                int strlen = (int) typeSize;
                byte[] bytes = ((String) newValue).getBytes();
                byte[] bData = (byte[]) dataBuf;
                int n = Math.min(strlen, bytes.length);

                index *= strlen;

                System.arraycopy(bytes, 0, bData, (int) index, n);

                index += n;
                n = strlen - bytes.length;

                // space padding
                for (int i = 0; i < n; i++) {
                    bData[(int) index + i] = ' ';
                }

                isValueChanged = true;
            }
            catch (Exception ex) {
                log.debug("setDataValue({}, {}, {}): cell value update failure: ", rowIndex, columnIndex, newValue, ex);
            }
            finally {
                log.trace("setDataValue({}, {}, {}): finish", rowIndex, columnIndex, newValue);
            }
        }

        @Override
        public void setDataValue(int index, Object bufObject, Object newValue) {
            log.trace("setDataValue({}, {}, {}): start", index, bufObject, newValue);

            try {
                // String data represented as a byte[]
                int strlen = (int) typeSize;
                byte[] bytes = ((String) newValue).getBytes();
                byte[] bData = (byte[]) bufObject;
                int n = Math.min(strlen, bytes.length);

                index *= strlen;

                System.arraycopy(bytes, 0, bData, index, n);

                index += n;
                n = strlen - bytes.length;

                // space padding
                for (int i = 0; i < n; i++) {
                    bData[index + i] = ' ';
                }

                isValueChanged = true;
            }
            catch (Exception ex) {
                log.debug("setDataValue({}, {}, {}): cell value update failure: ", index, bufObject, newValue, ex);
            }
            finally {
                log.trace("setDataValue({}, {}, {}): finish", index, bufObject, newValue);
            }
        }

    }

    private static class CharDataProvider extends HDFDataProvider {

        CharDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(CharDataProvider.class);

            log.trace("constructor: start");
            log.trace("constructor: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}): start", rowIndex, columnIndex);

            /*
             * Compatibility with HDF4 8-bit character types that get converted to a String
             * ahead of time.
             */
            if (dataBuf instanceof String) {
                log.trace("getDataValue({}, {})({}): finish", rowIndex, columnIndex, dataBuf);
                return dataBuf;
            }

            return super.getDataValue(columnIndex, rowIndex);
        }

    }

    private static class NumericalDataProvider extends HDFDataProvider {

        private final boolean isUINT64;

        private final long    typeSize;

        NumericalDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(NumericalDataProvider.class);

            log.trace("constructor: start");

            typeSize = dtype.getDatatypeSize();
            isUINT64 = dtype.isUnsigned() && (typeSize == 8);

            log.trace("constructor: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}): start", rowIndex, columnIndex);

            super.getDataValue(columnIndex, rowIndex);

            try {
                if (isUINT64)
                    theValue = Tools.convertUINT64toBigInt(Long.valueOf((long) theValue));
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {})({}): finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(Object obj, int index) {
            log.trace("getDataValue({}, {}): start", obj, index);

            super.getDataValue(obj, index);

            try {
                if (isUINT64)
                    theValue = Tools.convertUINT64toBigInt(Long.valueOf((long) theValue));
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", obj, index, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {})({}): finish", obj, index, theValue);

            return theValue;
        }

    }

    private static class EnumDataProvider extends HDFDataProvider {

        EnumDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(EnumDataProvider.class);

            log.trace("constructor: start");
            log.trace("constructor: finish");
        }

    }

    private static class BitfieldDataProvider extends HDFDataProvider {

        private final long typeSize;

        BitfieldDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(BitfieldDataProvider.class);

            log.trace("constructor: start");

            typeSize = dtype.getDatatypeSize();

            log.trace("constructor: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("getDataValue({}, {}): start", rowIndex, columnIndex);

            try {
                long index = rowIndex * colCount + columnIndex;

                if (rank > 1) {
                    log.trace("getDataValue({}, {}): rank={} isDataTransposed={} isNaturalOrder={}",
                            rowIndex, columnIndex, rank, isDataTransposed, isNaturalOrder);

                    if (isDataTransposed && isNaturalOrder)
                        index = columnIndex * rowCount + rowIndex;
                    else if (!isDataTransposed && !isNaturalOrder)
                        // Reshape Data
                        index = rowIndex * colCount + columnIndex;
                    else if (isDataTransposed && !isNaturalOrder)
                        // Transpose Data
                        index = columnIndex * rowCount + rowIndex;
                    else
                        index = rowIndex * colCount + columnIndex;
                }

                byte[] elements = new byte[(int) typeSize];

                log.trace("getDataValue({}, {}): datatype size={}", rowIndex, columnIndex, typeSize);

                index *= typeSize;

                log.trace("getDataValue({}, {}): index={}", rowIndex, columnIndex, index);

                for (int i = 0; i < typeSize; i++) {
                    elements[i] = Array.getByte(dataBuf, (int) index + i);
                }

                theValue = elements;
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): failure: ", rowIndex, columnIndex, ex);
                theValue = DataFactoryUtils.errStr;
            }

            log.trace("getDataValue({}, {})({}): finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(Object obj, int index) {
            log.trace("getDataValue({}, {}): start", obj, index);

            byte[] elements = new byte[(int) typeSize];

            index *= typeSize;

            for (int i = 0; i < typeSize; i++) {
                elements[i] = Array.getByte(obj, index + i);
            }

            theValue = elements;

            log.trace("getDataValue({}, {})({}): finish", obj, index, theValue);

            return theValue;
        }

    }

    private static class RefDataProvider extends HDFDataProvider {

        RefDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log = org.slf4j.LoggerFactory.getLogger(RefDataProvider.class);

            log.trace("constructor: start");
            log.trace("constructor: finish");
        }

    }

    /**
     * Since compound type attributes are currently simply retrieved as a 1D array
     * of strings, we use a custom IDataProvider to provide data for the Compound
     * TableView from the array of strings.
     */
    private static class CompoundAttributeDataProvider extends HDFDataProvider {
        private Object              theAttrValue;

        private final StringBuilder stringBuffer;

        private final int           orders[];
        private final int           nFields;
        private final int           nRows;
        private final int           nCols;
        private final int           nSubColumns;

        CompoundAttributeDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("CompoundAttributeDataProvider: start");

            CompoundDataFormat dataFormat = (CompoundDataFormat) dataFormatReference;

            stringBuffer = new StringBuilder();

            orders = dataFormat.getSelectedMemberOrders();
            nFields = dataFormat.getSelectedMemberCount();
            nRows = (int) dataFormat.getHeight();
            nCols = (int) (dataFormat.getWidth() * dataFormat.getSelectedMemberCount());
            nSubColumns = (nFields > 0) ? getColumnCount() / nFields : 0;

            log.trace("CompoundAttributeDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            try {
                int fieldIdx = columnIndex;
                int rowIdx = rowIndex;

                log.trace("CompoundAttributeDataProvider:getDataValue({},{}) start", rowIndex, columnIndex);

                if (nSubColumns > 1) { // multi-dimension compound dataset
                    int colIdx = columnIndex / nFields;
                    fieldIdx %= nFields;
                    rowIdx = rowIndex * orders[fieldIdx] * nSubColumns + colIdx * orders[fieldIdx];
                    log.trace(
                            "CompoundAttributeDataProvider:getDataValue() row={} orders[{}]={} nSubColumns={} colIdx={}",
                            rowIndex, fieldIdx, orders[fieldIdx], nSubColumns, colIdx);
                }
                else {
                    rowIdx = rowIndex * orders[fieldIdx];
                    log.trace("CompoundAttributeDataProvider:getDataValue() row={} orders[{}]={}", rowIndex, fieldIdx,
                            orders[fieldIdx]);
                }

                rowIdx = rowIndex;

                log.trace("CompoundAttributeDataProvider:getDataValue() rowIdx={}", rowIdx);

                int listIndex = ((columnIndex + (rowIndex * nCols)) / nFields);
                String colValue = (String) ((List<?>) dataBuf).get(listIndex);
                if (colValue == null) {
                    return "null";
                }

                colValue = colValue.replace("{", "").replace("}", "");
                colValue = colValue.replace("[", "").replace("]", "");

                String[] dataValues = colValue.split(",");
                if (orders[fieldIdx] > 1) {
                    stringBuffer.setLength(0);

                    stringBuffer.append("[");

                    int startIdx = 0;
                    for (int i = 0; i < fieldIdx; i++) {
                        startIdx += orders[i];
                    }

                    for (int i = 0; i < orders[fieldIdx]; i++) {
                        if (i > 0) stringBuffer.append(", ");

                        stringBuffer.append(dataValues[startIdx + i]);
                    }

                    stringBuffer.append("]");

                    theAttrValue = stringBuffer.toString();
                }
                else {
                    theAttrValue = dataValues[fieldIdx];
                }
            }
            catch (Exception ex) {
                log.debug("CompoundAttributeDataProvider:getDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                theAttrValue = DataFactoryUtils.errStr;
            }

            return theAttrValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("CompoundAttributeDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("CompoundAttributeDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

        @Override
        public int getColumnCount() {
            return nCols;
        }

        @Override
        public int getRowCount() {
            return nRows;
        }
    }

}
