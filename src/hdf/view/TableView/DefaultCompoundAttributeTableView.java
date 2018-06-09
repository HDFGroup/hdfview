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

import java.util.HashMap;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;

import hdf.object.CompoundDataFormat;
import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.view.ViewManager;

public class DefaultCompoundAttributeTableView extends DefaultCompoundDSTableView implements TableView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultCompoundAttributeTableView.class);

    public DefaultCompoundAttributeTableView(ViewManager theView) {
        this(theView, null);
    }

    @SuppressWarnings("rawtypes")
    public DefaultCompoundAttributeTableView(ViewManager theView, HashMap dataPropertiesMap) {
        super(theView, dataPropertiesMap);
    }

    /**
     * Since compound type attributes are currently simply retrieved as a 1D array
     * of strings, we use a custom IDataProvider to provide data for the Compound
     * TableView from the array of strings.
     */
    @Override
    protected IDataProvider getDataProvider(final DataFormat dataObject) {
        if (dataObject == null) return null;

        return new CompoundAttributeDataProvider(dataObject);
    }

    private class CompoundAttributeDataProvider implements IDataProvider {
        private Object             theValue;

        private final StringBuffer stringBuffer;

        private final Datatype     types[];

        private final int          orders[];
        private final int          nFields;
        private final int          nRows;
        private final int          nCols;
        private final int          nSubColumns;

        public CompoundAttributeDataProvider(DataFormat dataObject) {
            log.trace("CompoundAttributeDataProvider: start");

            CompoundDataFormat dataFormat = (CompoundDataFormat) dataObject;

            stringBuffer = new StringBuffer();

            types = dataFormat.getSelectedMemberTypes();

            orders = dataFormat.getSelectedMemberOrders();
            nFields = dataFormat.getSelectedMemberCount();
            nRows = (int) dataFormat.getHeight();
            nCols = (int) (dataFormat.getWidth() * dataFormat.getSelectedMemberCount());
            nSubColumns = (nFields > 0) ? getColumnCount() / nFields : 0;

            log.trace("CompoundAttributeDataProvider: finish");
        }

        @Override
        public Object getDataValue(int col, int row) {
            int fieldIdx = col;
            int rowIdx = row;

            log.trace("CompoundAttributeDataProvider:getDataValue({},{}) start", row, col);

            if (nSubColumns > 1) { // multi-dimension compound dataset
                int colIdx = col / nFields;
                fieldIdx %= nFields;
                rowIdx = row * orders[fieldIdx] * nSubColumns + colIdx * orders[fieldIdx];
                log.trace("CompoundAttributeDataProvider:getDataValue() row={} orders[{}]={} nSubColumns={} colIdx={}",
                        row, fieldIdx, orders[fieldIdx], nSubColumns, colIdx);
            }
            else {
                rowIdx = row * orders[fieldIdx];
                log.trace("CompoundAttributeDataProvider:getDataValue() row={} orders[{}]={}", row, fieldIdx,
                        orders[fieldIdx]);
            }

            log.trace("CompoundAttributeDataProvider:getDataValue() rowIdx={}", rowIdx);

            String colValue = (String) ((List<?>) dataValue).get(rowIdx);
            if (colValue == null) {
                return "Null";
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

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            try {
                updateValueInMemory((String) newValue, rowIndex, columnIndex);
            }
            catch (Exception ex) {
                log.debug("CompoundAttributeDataProvider:setDataValue({}, {}) failure: ", rowIndex, columnIndex, ex);
            }
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
