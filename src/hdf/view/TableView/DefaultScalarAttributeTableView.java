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

package hdf.view.TableView;

import java.lang.reflect.Array;
import java.util.HashMap;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;

import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.view.Tools;
import hdf.view.ViewManager;

public class DefaultScalarAttributeTableView extends DefaultScalarDSTableView implements TableView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultScalarAttributeTableView.class);

    public DefaultScalarAttributeTableView(ViewManager theView) {
        this(theView, null);
    }

    @SuppressWarnings("rawtypes")
    public DefaultScalarAttributeTableView(ViewManager theView, HashMap dataPropertiesMap) {
        super(theView, dataPropertiesMap);
    }

    /**
     * Since attributes are currently simply retrieved as a 1D array of data
     * entries, we use a custom IDataProvider to provide data for the Scalar
     * TableView from the array.
     */
    @Override
    protected IDataProvider getDataProvider(final DataFormat dataObject) {
        if (dataObject == null) return null;

        return new ScalarAttributeDataProvider(dataObject);
    }

    private class ScalarAttributeDataProvider implements IDataProvider {
        private Object             theValue;

        private final Object[]     arrayElements;

        private final StringBuffer buffer;

        private final Datatype     dtype;
        private final Datatype     btype;

        private final long         arraySize;

        private final long[]       dims;

        private final int          rank;

        private final boolean      isArray;
        private final boolean      isInt;
        private final boolean      isUINT64;
        private final boolean      isBitfieldOrOpaque;

        private boolean            isVLStr;

        private final boolean      isNaturalOrder;

        private final long         rowCount;
        private final long         colCount;

        public ScalarAttributeDataProvider(DataFormat dataObject) {
            log.trace("ScalarAttributeDataProvider: start");

            buffer = new StringBuffer();

            dtype = dataObject.getDatatype();
            btype = dtype.getDatatypeBase();

            dims = dataObject.getSelectedDims();

            rank = dataObject.getRank();

            char runtimeTypeClass = Tools.getJavaObjectRuntimeClass(dataValue);
            if (runtimeTypeClass == ' ') {
                log.debug("ScalarAttributeDataProvider: invalid data value runtime type class: runtimeTypeClass={}",
                        runtimeTypeClass);
                log.trace("ScalarAttributeDataProvider: finish");
                throw new IllegalStateException("Invalid data value runtime type class: " + runtimeTypeClass);
            }

            isInt = (runtimeTypeClass == 'B' || runtimeTypeClass == 'S' || runtimeTypeClass == 'I'
                    || runtimeTypeClass == 'J');
            log.trace("ScalarAttributeDataProvider:runtimeTypeClass={}", runtimeTypeClass);

            isArray = dtype.isArray();
            log.trace("ScalarAttributeDataProvider:isArray={} start", isArray);
            if (isArray)
                isUINT64 = (btype.isUnsigned() && (runtimeTypeClass == 'J'));
            else
                isUINT64 = (dtype.isUnsigned() && (runtimeTypeClass == 'J'));
            isBitfieldOrOpaque = (dtype.isOpaque() || dtype.isBitField());

            isNaturalOrder = (dataObject.getRank() == 1
                    || (dataObject.getSelectedIndex()[0] < dataObject.getSelectedIndex()[1]));

            if (isArray) {
                if (dtype.isVLEN() && btype.isString()) {
                    isVLStr = true;

                    // Variable-length string arrays don't have a defined array size
                    arraySize = dtype.getArrayDims()[0];
                }
                else if (btype.isArray()) {
                    // Array of Array
                    long[] dims = btype.getArrayDims();

                    long size = 1;
                    for (int i = 0; i < dims.length; i++) {
                        size *= dims[i];
                    }

                    arraySize = size * (dtype.getDatatypeSize() / btype.getDatatypeSize());
                }
                else if (isBitfieldOrOpaque) {
                    arraySize = dtype.getDatatypeSize();
                }
                else {
                    arraySize = dtype.getDatatypeSize() / btype.getDatatypeSize();
                }

                arrayElements = new Object[(int) arraySize];
            }
            else {
                if (dtype.isVLEN() && btype.isString())
                    isVLStr = true;

                arraySize = 0;
                arrayElements = null;
            }

            if (dataObject.getRank() > 1) {
                rowCount = dataObject.getHeight();
                colCount = dataObject.getWidth();
            }
            else {
                rowCount = (int) dims[0];
                colCount = 1;
            }

            log.trace("ScalarAttributeDataProvider: finish");
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            log.trace("ScalarAttributeDataProvider:getValueAt({},{}) start", rowIndex, columnIndex);
            log.trace("ScalarAttributeDataProvider:getValueAt isInt={} isArray={} showAsHex={} showAsBin={}", isInt,
                    isArray, showAsHex, showAsBin);

            long index = columnIndex * rowCount + rowIndex;

            if (rank > 1) {
                log.trace("ScalarAttributeDataProvider:getValueAt rank={} isDataTransposed={} isNaturalOrder={}", rank,
                        isDataTransposed, isNaturalOrder);
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

            // if (isArray) {
            // log.trace(
            // "ScalarAttributeDataProvider:getValueAt ARRAY dataset size={}
            // isDisplayTypeChar={} isUINT64={}",
            // arraySize, isDisplayTypeChar, isUINT64);
            //
            // // int index = (int) (rowIndex * colCount + columnIndex) * (int) arraySize;
            //
            // if (isDisplayTypeChar) {
            // for (int i = 0; i < arraySize; i++) {
            // arrayElements[i] = Array.getChar(dataValue, (int) index++);
            // }
            //
            // theValue = arrayElements;
            // }
            // else if (isVLStr) {
            // buffer.setLength(0);
            //
            // for (int i = 0; i < dtype.getArrayDims()[0]; i++) {
            // if (i > 0) buffer.append(", ");
            // buffer.append(Array.get(dataValue, (int) index++));
            // }
            //
            // theValue = buffer.toString();
            // }
            // else if (isBitfieldOrOpaque) {
            // for (int i = 0; i < arraySize; i++) {
            // arrayElements[i] = Array.getByte(dataValue, (int) index++);
            // }
            //
            // theValue = arrayElements;
            // }
            // else {
            // if (isUINT64) {
            // for (int i = 0; i < arraySize; i++) {
            // arrayElements[i] = Tools.convertUINT64toBigInt(Array.getLong(dataValue, (int)
            // index++));
            // }
            // }
            // else {
            // for (int i = 0; i < arraySize; i++) {
            // arrayElements[i] = Array.get(dataValue, (int) index++);
            // }
            // }
            //
            // theValue = arrayElements;
            // }
            // }
            // else {
            // long index = columnIndex * rowCount + rowIndex;
            //
            // if (rank > 1) {
            // log.trace("ScalarAttributeDataProvider:getValueAt rank={} isDataTransposed={}
            // isNaturalOrder={}",
            // rank, isDataTransposed, isNaturalOrder);
            // if (isDataTransposed && isNaturalOrder)
            // index = columnIndex * rowCount + rowIndex;
            // else if (!isDataTransposed && !isNaturalOrder)
            // // Reshape Data
            // index = rowIndex * colCount + columnIndex;
            // else if (isDataTransposed && !isNaturalOrder)
            // // Transpose Data
            // index = columnIndex * rowCount + rowIndex;
            // else
            // index = rowIndex * colCount + columnIndex;
            // }

            if (isBitfieldOrOpaque) {
                int len = (int) dtype.getDatatypeSize();
                byte[] elements = new byte[len];

                index *= len;

                for (int i = 0; i < len; i++) {
                    elements[i] = Array.getByte(dataValue, (int) index + i);
                }

                theValue = elements;
            }
            else {
                if (isUINT64) {
                    theValue = Tools.convertUINT64toBigInt(Array.getLong(dataValue, (int) index));
                }
                else {
                    theValue = Array.get(dataValue, (int) index);
                }
            }
            // }

            log.trace("ScalarAttributeDataProvider:getValueAt {} finish", theValue);
            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            try {
                updateValueInMemory((String) newValue, rowIndex, columnIndex);
            }
            catch (Exception ex) {
                log.debug("ScalarAttributeDataProvider:setDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
                Tools.showError(shell, "Select", "Unable to set new value:\n\n " + ex);
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
    }
}
