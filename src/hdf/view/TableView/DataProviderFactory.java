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
import java.util.List;

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

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataProviderFactory.class);

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

    private static class HDFDataProvider implements IDataProvider {

        protected final Object  dataBuf;

        protected Object        theValue;

        protected final int     rank;

        protected final boolean isNaturalOrder;
        protected final boolean isDataTransposed;

        protected final long    colCount;
        protected final long    rowCount;

        HDFDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            log.trace("HDFDataProvider: start");

            this.dataBuf = dataBuf;

            char runtimeTypeClass = Utils.getJavaObjectRuntimeClass(dataBuf);
            if (runtimeTypeClass == ' ') {
                log.debug("HDFDataProvider: invalid data value runtime type class: runtimeTypeClass={}", runtimeTypeClass);
                log.trace("HDFDataProvider: finish");
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

            log.trace("HDFDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("HDFDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            try {
                long index = rowIndex * colCount + columnIndex;

                if (rank > 1) {
                    log.trace("HDFDataProvider: getDataValue({}, {}) rank={} isDataTransposed={} isNaturalOrder={}",
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
                log.debug("HDFDataProvider: getDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                theValue = "*ERROR*";
            }

            log.trace("HDFDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        /*
         * When a parent HDFDataProvider (such as an ArrayDataProvider) wants to
         * retrieve a data value by routing the operation through its base HDFDataProvider,
         * the parent HDFDataProvider will generally know the direct index to have the base
         * provider to use. This method is to facilitate this kind of behavior.
         */
        public Object getDataValue(int index) {
            log.trace("HDFDataProvider: getDataValue({}) start", index);

            try {
                theValue = Array.get(dataBuf, index);
            }
            catch (Exception ex) {
                log.debug("HDFDataProvider: getDataValue({}) failure: ", index, ex);
                theValue = "*ERROR*";
            }

            log.trace("HDFDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            /*
             * TODO
             */
            /* try {
                updateValueInMemory((String) newValue, rowIndex, columnIndex);
            }
            catch (Exception ex) {
                log.debug("HDFDataProvider: setDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                Tools.showError(shell, "Select", "Unable to set new value:\n\n " + ex);
            } */
        }

        @Override
        public int getColumnCount() {
            return (int) colCount;
        }

        @Override
        public int getRowCount() {
            return (int) rowCount;
        }

    }

    private static class CompoundDataProvider extends HDFDataProvider {

        private HDFDataProvider baseTypeProviders[];

        private final Datatype  types[];
        private final int       orders[];

        private final int       nFields;
        private final int       nSubColumns;
        private final int       nCols;
        private final int       nRows;

        CompoundDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("CompoundDataProvider: start");

            CompoundDataFormat compoundFormat = (CompoundDataFormat) dataFormatReference;

            types = compoundFormat.getSelectedMemberTypes();
            orders = compoundFormat.getSelectedMemberOrders();

            baseTypeProviders = new HDFDataProvider[types.length];
            for (int i = 0; i < baseTypeProviders.length; i++) {
                baseTypeProviders[i] = getDataProvider(types[i], dataBuf, dataTransposed);
            }

            nFields = ((List<?>) dataBuf).size();

            nCols = (int) (compoundFormat.getWidth() * compoundFormat.getSelectedMemberCount());
            nRows = (int) compoundFormat.getHeight();

            nSubColumns = (nFields > 0) ? nCols / nFields : 0;

            log.trace("CompoundDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("CompoundDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            try {
                int fieldIdx = columnIndex;
                int rowIdx = rowIndex;

                if (nSubColumns > 1) { // multi-dimension compound dataset
                    int colIdx = columnIndex / nFields;
                    fieldIdx %= nFields;
                    rowIdx = rowIndex * orders[fieldIdx] * nSubColumns + colIdx * orders[fieldIdx];
                }
                else {
                    rowIdx = rowIndex * orders[fieldIdx];
                }

                /*
                 * TODO
                 */
                /* log.trace("CompoundDataProvider: getDataValue({}, {}) row={} orders[{}]={} nSubColumns={} colIdx={}",
                        rowIndex, columnIndex, orders[fieldIdx], nSubColumns, colIdx); */

                Object colValue = ((List<?>) dataBuf).get(fieldIdx);
                if (colValue == null) {
                    return "Null";
                }

                Datatype dtype = types[fieldIdx];
                if (dtype == null) {
                    return "Null";
                }

                boolean isUINT64 = false;

                String cName = colValue.getClass().getName();
                int cIndex = cName.lastIndexOf("[");
                if (cIndex >= 0) {
                    if (dtype.isUnsigned())
                        isUINT64 = (cName.charAt(cIndex + 1) == 'J');
                }
            }
            catch (Exception ex) {
                log.debug("CompoundDataProvider: getDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                theValue = "*ERROR*";
            }

            log.trace("CompoundDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
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

        ArrayDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("ArrayDataProvider: start");

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

            log.trace("ArrayDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("ArrayDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            try {
                long index = (rowIndex * colCount + columnIndex) * arraySize;

                if (rank > 1) {
                    log.trace("ArrayDataProvider: getDataValue({}, {}) rank={} isDataTransposed={} isNaturalOrder={}",
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

                for (int i = 0; i < arraySize; i++) {
                    arrayElements[i] = baseTypeDataProvider.getDataValue((int) index + i);
                }

                theValue = arrayElements;
            }
            catch (Exception ex) {
                log.debug("ArrayDataProvider: getDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                theValue = "*ERROR*";
            }

            log.trace("ArrayDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(int index) {
            log.trace("ArrayDataProvider: getDataValue({}) start", index);

            Object[] tempArray = new Object[(int) arraySize];

            try {
                long localIndex = index * arraySize;

                for (int i = 0; i < arraySize; i++) {
                    tempArray[i] = baseTypeDataProvider.getDataValue((int) localIndex + i);
                }

                theValue = tempArray;
            }
            catch (Exception ex) {
                log.debug("ArrayDataProvider: getDataValue({}) failure: ", index, ex);
                theValue = "*ERROR*";
            }

            log.trace("ArrayDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("ArrayDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("ArrayDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

    }

    private static class VlenDataProvider extends HDFDataProvider {

        private final StringBuffer buffer;

        VlenDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("VlenDataProvider: start");

            buffer = new StringBuffer();

            log.trace("VlenDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("VlenDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            buffer.setLength(0);

            super.getDataValue(columnIndex, rowIndex);

            try {
                buffer.append(theValue);

                theValue = buffer.toString();
            }
            catch (Exception ex) {
                log.debug("VlenDataProvider: getDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                theValue = "*ERROR*";
            }

            log.trace("VlenDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(int index) {
            log.trace("VlenDataProvider: getDataValue({}) start", index);

            buffer.setLength(0);

            super.getDataValue(index);

            try {
                buffer.append(theValue);

                theValue = buffer.toString();
            }
            catch (Exception ex) {
                log.debug("VlenDataProvider: getDataValue({}) failure: ", index, ex);
                theValue = "*ERROR*";
            }

            log.trace("VlenDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("VlenDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("VlenDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

    }

    private static class StringDataProvider extends HDFDataProvider {

        StringDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("StringDataProvider: start");
            log.trace("StringDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("StringDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            super.getDataValue(columnIndex, rowIndex);

            log.trace("StringDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(int index) {
            log.trace("StringDataProvider: getDataValue({}) start", index);

            super.getDataValue(index);

            log.trace("StringDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("StringDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("StringDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

    }

    private static class CharDataProvider extends HDFDataProvider {

        CharDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("CharDataProvider: start");
            log.trace("CharDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("CharDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            super.getDataValue(columnIndex, rowIndex);

            log.trace("CharDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(int index) {
            log.trace("CharDataProvider: getDataValue({}) start", index);

            super.getDataValue(index);

            log.trace("CharDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("CharDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("CharDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

    }

    private static class NumericalDataProvider extends HDFDataProvider {

        private final boolean isUINT64;

        private final long    typeSize;

        NumericalDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("NumericalDataProvider: start");

            typeSize = dtype.getDatatypeSize();
            isUINT64 = dtype.isUnsigned() && (typeSize == 8);

            log.trace("NumericalDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("NumericalDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            super.getDataValue(columnIndex, rowIndex);

            try {
                if (isUINT64)
                    theValue = Tools.convertUINT64toBigInt(Long.valueOf((long) theValue));
            }
            catch (Exception ex) {
                log.debug("NumericalDataProvider: getDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                theValue = "*ERROR*";
            }

            log.trace("NumericalDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(int index) {
            log.trace("NumericalDataProvider: getDataValue({}) start", index);

            super.getDataValue(index);

            try {
                if (isUINT64)
                    theValue = Tools.convertUINT64toBigInt(Long.valueOf((long) theValue));
            }
            catch (Exception ex) {
                log.debug("NumericalDataProvider: getDataValue({}) failure: ", index, ex);
                theValue = "*ERROR*";
            }

            log.trace("NumericalDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("NumericalDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("NumericalDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

    }

    private static class EnumDataProvider extends HDFDataProvider {

        EnumDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("EnumDataProvider: start");
            log.trace("EnumDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("EnumDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            super.getDataValue(columnIndex, rowIndex);

            log.trace("EnumDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(int index) {
            log.trace("EnumDataProvider: getDataValue({}) start", index);

            super.getDataValue(index);

            log.trace("EnumDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("EnumDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("EnumDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

    }

    private static class BitfieldDataProvider extends HDFDataProvider {

        private final long typeSize;

        BitfieldDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("BitfieldDataProvider: start");

            typeSize = dtype.getDatatypeSize();

            log.trace("BitfieldDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("BitfieldDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            try {
                long index = rowIndex * colCount + columnIndex;

                if (rank > 1) {
                    log.trace("BitfieldDataProvider: getDataValue({}, {}) rank={} isDataTransposed={} isNaturalOrder={}",
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

                log.trace("BitfieldDataProvider: getDataValue(): datatype size={}", typeSize);

                index *= typeSize;

                log.trace("BitfieldDataProvider: getDataValue(): index={}", index);

                for (int i = 0; i < typeSize; i++) {
                    elements[i] = Array.getByte(dataBuf, (int) index + i);
                }

                theValue = elements;
            }
            catch (Exception ex) {
                log.debug("BitfieldDataProvider: getDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                theValue = "*ERROR*";
            }

            log.trace("BitfieldDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(int index) {
            log.trace("BitfieldDataProvider: getDataValue({}) start", index);

            byte[] elements = new byte[(int) typeSize];

            /* index *= typeSize; */

            for (int i = 0; i < typeSize; i++) {
                elements[i] = Array.getByte(dataBuf, index + i);
            }

            theValue = elements;

            log.trace("BitfieldDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("BitfieldDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("BitfieldDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

    }

    private static class RefDataProvider extends HDFDataProvider {

        RefDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("RefDataProvider: start");
            log.trace("RefDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("RefDataProvider: getDataValue({}, {}) start", rowIndex, columnIndex);

            super.getDataValue(columnIndex, rowIndex);

            log.trace("RefDataProvider: getDataValue({}, {})({}) finish", rowIndex, columnIndex, theValue);

            return theValue;
        }

        @Override
        public Object getDataValue(int index) {
            log.trace("RefDataProvider: getDataValue({}) start", index);

            super.getDataValue(index);

            log.trace("RefDataProvider: getDataValue({})({}) finish", index, theValue);

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            log.trace("RefDataProvider: setDataValue({}, {})({}) start", rowIndex, columnIndex, newValue);

            super.setDataValue(columnIndex, rowIndex, newValue);

            log.trace("RefDataProvider: setDataValue({}, {})({}) finish", rowIndex, columnIndex, newValue);
        }

    }

    /**
     * Since compound type attributes are currently simply retrieved as a 1D array
     * of strings, we use a custom IDataProvider to provide data for the Compound
     * TableView from the array of strings.
     */
    private static class CompoundAttributeDataProvider extends HDFDataProvider {
        private Object             theValue;

        private final StringBuffer stringBuffer;

        private final int          orders[];
        private final int          nFields;
        private final int          nRows;
        private final int          nCols;
        private final int          nSubColumns;

        CompoundAttributeDataProvider(final Datatype dtype, final Object dataBuf, final boolean dataTransposed) throws Exception {
            super(dtype, dataBuf, dataTransposed);

            log.trace("CompoundAttributeDataProvider: start");

            CompoundDataFormat dataFormat = (CompoundDataFormat) dataFormatReference;

            stringBuffer = new StringBuffer();

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

                    theValue = stringBuffer.toString();
                }
                else {
                    theValue = dataValues[fieldIdx];
                }
            }
            catch (Exception ex) {
                log.debug("CompoundAttributeDataProvider:getDataValue() failure: ", ex);
                theValue = "*ERROR*";
            }

            return theValue;
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
