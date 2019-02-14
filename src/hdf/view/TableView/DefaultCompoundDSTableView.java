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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.EditableRule;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupExpandCollapseLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupGroupHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.event.CellSelectionEvent;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;

import hdf.object.CompoundDS;
import hdf.object.CompoundDataFormat;
import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;

public class DefaultCompoundDSTableView extends DefaultBaseTableView implements TableView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultCompoundDSTableView.class);

    /**
     * Constructs a CompoundDS TableView with no additional data properties.
     *
     * @param theView
     *            the main HDFView.
     */
    public DefaultCompoundDSTableView(DataViewManager theView) {
        this(theView, null);
    }

    /**
     * Constructs a CompoundDS TableView with the specified data properties.
     *
     * @param theView
     *            the main HDFView.
     *
     * @param dataPropertiesMap
     *            the properties on how to show the data. The map is used to allow
     *            applications to pass properties on how to display the data, such
     *            as: transposing data, showing data as characters, applying a
     *            bitmask, and etc. Predefined keys are listed at
     *            ViewProperties.DATA_VIEW_KEY.
     */
    @SuppressWarnings("rawtypes")
    public DefaultCompoundDSTableView(DataViewManager theView, HashMap dataPropertiesMap) {
        super(theView, dataPropertiesMap);

        log.trace("DefaultCompoundDSTableView: start");

        isDataTransposed = false; // Disable transpose for compound datasets

        if (!shell.isDisposed()) {
            shell.setImage(ViewProperties.getTableIcon());

            viewer.addDataView(this);

            log.trace("DefaultCompoundDSTableView: viewer add");

            shell.open();
        }

        log.trace("DefaultCompoundDSTableView: finish");
    }

    @Override
    protected void loadData(DataFormat dataObject) throws Exception {
        log.trace("loadData(): start");

        super.loadData(dataObject);

        if ((dataValue == null) || !(dataValue instanceof List)) {
            log.debug("loadData(): data value is null or data not a list");
            log.trace("loadData(): finish");
            throw new RuntimeException("data value is null or not a list");
        }

        log.trace("loadData(): finish");
    }

    /**
     * Creates a NatTable for a Compound dataset
     *
     * @param parent
     *            The parent for the NatTable
     * @param dataObject
     *            The Compound dataset for the NatTable to display
     *
     * @return The newly created NatTable
     */
    @Override
    protected NatTable createTable(Composite parent, DataFormat dataObject) {
        log.trace("createTable(): start");

        // Create body layer
        final ColumnGroupModel columnGroupModel = new ColumnGroupModel();
        final ColumnGroupModel secondLevelGroupModel = new ColumnGroupModel();

        try {
            final IDataProvider bodyDataProvider = DataProviderFactory.getDataProvider(dataObject, dataValue, isDataTransposed);

            dataLayer = new DataLayer(bodyDataProvider);
        }
        catch (Exception ex) {
            log.debug("createTable(): failed to retrieve DataProvider for table: ", ex);
            return null;
        }

        final ColumnGroupExpandCollapseLayer expandCollapseLayer = new ColumnGroupExpandCollapseLayer(dataLayer,
                secondLevelGroupModel, columnGroupModel);
        selectionLayer = new SelectionLayer(expandCollapseLayer);
        final ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);

        dataLayer.setDefaultColumnWidth(80);

        // Create the Column Header layer
        columnHeaderDataProvider = new CompoundDSColumnHeaderDataProvider(dataObject);
        ColumnHeaderLayer columnHeaderLayer = new ColumnHeader(new DataLayer(columnHeaderDataProvider), viewportLayer,
                selectionLayer);

        // Set up column grouping
        ColumnGroupHeaderLayer columnGroupHeaderLayer = new ColumnGroupHeaderLayer(columnHeaderLayer, selectionLayer,
                columnGroupModel);
        CompoundDSNestedColumnHeaderLayer nestedColumnGroupHeaderLayer = new CompoundDSNestedColumnHeaderLayer(
                columnGroupHeaderLayer, selectionLayer, secondLevelGroupModel);

        // Create the Row Header layer
        rowHeaderDataProvider = new RowHeaderDataProvider(dataObject);

        // Try to adapt row height to current font
        int defaultRowHeight = curFont == null ? 20 : (2 * curFont.getFontData()[0].getHeight());

        DataLayer baseLayer = new DataLayer(rowHeaderDataProvider, 40, defaultRowHeight);
        RowHeaderLayer rowHeaderLayer = new RowHeader(baseLayer, viewportLayer, selectionLayer);

        // Create the Corner Layer
        ILayer cornerLayer = new CornerLayer(
                new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
                rowHeaderLayer, nestedColumnGroupHeaderLayer);

        // Create the Grid Layer
        GridLayer gridLayer = new EditingGridLayer(viewportLayer, nestedColumnGroupHeaderLayer, rowHeaderLayer,
                cornerLayer);

        final NatTable natTable = new NatTable(parent, gridLayer, false);
        natTable.addConfiguration(new DefaultNatTableStyleConfiguration());
        natTable.addLayerListener(new CompoundDSCellSelectionListener());

        natTable.configure();

        log.trace("createTable(): finish");

        return natTable;
    }

    @Override
    public Object getSelectedData() {
        Object selectedData = null;

        int cols = this.getSelectedColumnCount();
        int rows = this.getSelectedRowCount();

        if ((cols <= 0) || (rows <= 0)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select", "No data is selected.");
            return null;
        }

        Object colData = null;
        try {
            colData = ((List<?>) dataObject.getData()).get(selectionLayer.getSelectedColumnPositions()[0]);
        }
        catch (Exception ex) {
            log.debug("getSelectedData(): ", ex);
            return null;
        }

        int size = Array.getLength(colData);
        String cName = colData.getClass().getName();
        int cIndex = cName.lastIndexOf("[");
        char nt = ' ';
        if (cIndex >= 0) {
            nt = cName.charAt(cIndex + 1);
        }
        log.trace("getSelectedData(): size={} cName={} nt={}", size, cName, nt);

        if (nt == 'B') {
            selectedData = new byte[size];
        }
        else if (nt == 'S') {
            selectedData = new short[size];
        }
        else if (nt == 'I') {
            selectedData = new int[size];
        }
        else if (nt == 'J') {
            selectedData = new long[size];
        }
        else if (nt == 'F') {
            selectedData = new float[size];
        }
        else if (nt == 'D') {
            selectedData = new double[size];
        }
        else {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select", "Unsupported data type.");
            return null;
        }
        log.trace("getSelectedData(): selectedData={}", selectedData);

        System.arraycopy(colData, 0, selectedData, 0, size);

        return selectedData;
    }


    @Override
    protected void showObjRefData(long ref) {
        // Currently no support for showing Obj. Ref. Data in Compound Datasets
        return;
    }

    @Override
    protected void showRegRefData(String reg) {
        // Currently no support for show Reg. Ref. Data in Compound Datasets
        return;
    }

    /**
     * Update cell value in memory. It does not change the dataset's value in the
     * file.
     *
     * @param cellValue
     *            the string value of input.
     * @param row
     *            the row of the editing cell.
     * @param col
     *            the column of the editing cell.
     *
     * @throws Exception
     *             if a failure occurred
     */
    @Override
    protected void updateValueInMemory(String cellValue, int row, int col) throws Exception {
        log.trace("updateValueInMemory({}, {}): start", row, col);

        if ((cellValue == null) || ((cellValue = cellValue.trim()) == null)) {
            log.debug("updateValueInMemory({}, {}): cell value not updated; new value is null", row, col);
            log.trace("updateValueInMemory({}, {}): finish", row, col);
            return;
        }

        // No need to update if values are the same
        Object oldVal = dataLayer.getDataValue(col, row);
        if ((oldVal != null) && cellValue.equals(oldVal.toString())) {
            log.debug("updateValueInMemory({}, {}): cell value not updated; new value same as old value", row, col);
            log.trace("updateValueInMemory({}, {}): finish", row, col);
            return;
        }

        try {
            List<?> cdata = (List<?>) dataObject.getData();
            int orders[] = ((CompoundDataFormat) dataObject).getSelectedMemberOrders();
            Datatype types[] = ((CompoundDataFormat) dataObject).getSelectedMemberTypes();
            int nFields = cdata.size();
            int nSubColumns = (dataTable.getPreferredColumnCount() - 1) / nFields;
            int column = col;
            int offset = 0;
            int morder = 1;

            if (nSubColumns > 1) { // multi-dimension compound dataset
                int colIdx = col / nFields;
                column = col - colIdx * nFields;
                // //BUG 573: offset = row * orders[column] + colIdx * nRows *
                // orders[column];
                offset = row * orders[column] * nSubColumns + colIdx * orders[column];
            }
            else {
                offset = row * orders[column];
            }
            morder = orders[column];

            Object mdata = cdata.get(column);

            // strings
            if (Array.get(mdata, 0) instanceof String) {
                Array.set(mdata, offset, cellValue);
                isValueChanged = true;

                log.trace("updateValueInMemory({}, {}): finish", row, col);
                return;
            }
            else if (types[column].isString()) {
                // it is string but not converted, still byte array
                int strlen = (int) types[column].getDatatypeSize();
                offset *= strlen;
                byte[] bytes = cellValue.getBytes();
                byte[] bData = (byte[]) mdata;
                int n = Math.min(strlen, bytes.length);
                System.arraycopy(bytes, 0, bData, offset, n);
                offset += n;
                n = strlen - bytes.length;
                // space padding
                for (int i = 0; i < n; i++) {
                    bData[offset + i] = ' ';
                }
                isValueChanged = true;

                log.trace("updateValueInMemory({}, {}): finish", row, col);
                return;
            }

            // Numeric data
            char mNT = ' ';
            String cName = mdata.getClass().getName();
            int cIndex = cName.lastIndexOf("[");
            if (cIndex >= 0) {
                mNT = cName.charAt(cIndex + 1);
            }

            StringTokenizer st = new StringTokenizer(cellValue, ",");
            if (st.countTokens() < morder) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Select", "Number of data points < " + morder + ".");
                log.debug("updateValueInMemory({}, {}): number of data points < {}", row, col, morder);
                log.trace("updateValueInMemory({}, {}): finish", row, col);
                return;
            }

            String token = "";
            isValueChanged = true;
            switch (mNT) {
                case 'B':
                    byte bvalue = 0;
                    for (int i = 0; i < morder; i++) {
                        token = st.nextToken().trim();
                        bvalue = Byte.parseByte(token);
                        Array.setByte(mdata, offset + i, bvalue);
                    }
                    break;
                case 'S':
                    short svalue = 0;
                    for (int i = 0; i < morder; i++) {
                        token = st.nextToken().trim();
                        svalue = Short.parseShort(token);
                        Array.setShort(mdata, offset + i, svalue);
                    }
                    break;
                case 'I':
                    int ivalue = 0;
                    for (int i = 0; i < morder; i++) {
                        token = st.nextToken().trim();
                        ivalue = Integer.parseInt(token);
                        Array.setInt(mdata, offset + i, ivalue);
                    }
                    break;
                case 'J':
                    long lvalue = 0;
                    for (int i = 0; i < morder; i++) {
                        token = st.nextToken().trim();
                        BigInteger big = new BigInteger(token);
                        lvalue = big.longValue();
                        // lvalue = Long.parseLong(token);
                        Array.setLong(mdata, offset + i, lvalue);
                    }
                    break;
                case 'F':
                    float fvalue = 0;
                    for (int i = 0; i < morder; i++) {
                        token = st.nextToken().trim();
                        fvalue = Float.parseFloat(token);
                        Array.setFloat(mdata, offset + i, fvalue);
                    }
                    break;
                case 'D':
                    double dvalue = 0;
                    for (int i = 0; i < morder; i++) {
                        token = st.nextToken().trim();
                        dvalue = Double.parseDouble(token);
                        Array.setDouble(mdata, offset + i, dvalue);
                    }
                    break;
                default:
                    isValueChanged = false;
            }
        }
        catch (Exception ex) {
            log.debug("updateValueInMemory({}, {}):", row, col, ex);
        }

        log.trace("updateValueInMemory({}, {}): finish", row, col);
    }

    /**
     * Update dataset's value in file. The changes will go to the file.
     */
    @Override
    public void updateValueInFile() {
        log.trace("updateValueInFile(): start");

        if (isReadOnly || !isValueChanged || showAsBin || showAsHex) {
            log.debug("updateValueInFile(): file not updated; read-only or unchanged data or displayed as hex or binary");
            log.trace("updateValueInFile(): finish");
            return;
        }

        try {
            log.trace("updateValueInFile(): write");
            dataObject.write();
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Update", ex.getMessage());
            log.debug("updateValueInFile(): ", ex);
            log.trace("updateValueInFile(): finish");
            return;
        }

        isValueChanged = false;
        log.trace("updateValueInFile(): finish");
    }

    /**
     * Returns an IEditableRule that determines whether cells can be edited.
     *
     * Cells can be edited as long as the dataset is not opened in read-only mode.
     *
     * @param dataObject
     *            The dataset for editing
     *
     * @return a new IEditableRule for the dataset
     */
    @Override
    protected IEditableRule getDataEditingRule(DataFormat dataObject) {
        if (dataObject == null) return null;

        // Only Allow editing of CompoundDS if not in read-only mode
        return new EditableRule() {
            @Override
            public boolean isEditable(int columnIndex, int rowIndex) {
                return !isReadOnly;
            }
        };
    }

    /**
     * Update cell value label and cell value field when a cell is selected
     */
    private class CompoundDSCellSelectionListener implements ILayerListener {
        @Override
        public void handleLayerEvent(ILayerEvent e) {
            if (e instanceof CellSelectionEvent) {
                CellSelectionEvent event = (CellSelectionEvent) e;
                Object val = dataTable.getDataValueByPosition(event.getColumnPosition(), event.getRowPosition());

                log.trace("NATTable CellSelected isRegRef={} isObjRef={}", isRegRef, isObjRef);

                int rowStart = ((RowHeaderDataProvider) rowHeaderDataProvider).start;
                int rowStride = ((RowHeaderDataProvider) rowHeaderDataProvider).stride;

                int rowIndex = rowStart + indexBase + dataTable.getRowIndexByPosition(event.getRowPosition()) * rowStride;
                Object fieldName = columnHeaderDataProvider.getDataValue(dataTable.getColumnIndexByPosition(event.getColumnPosition()), 0);

                String colIndex = "";

                if (dataObject.getWidth() > 1) {
                    int groupSize = ((CompoundDataFormat) dataObject).getSelectedMemberCount();
                    colIndex = "[" + String.valueOf((dataTable.getColumnIndexByPosition(event.getColumnPosition())) / groupSize) + "]";
                }

                cellLabel.setText(String.valueOf(rowIndex) + ", " + fieldName + colIndex + " =  ");

                if (val == null) {
                    cellValueField.setText("Null");
                    return;
                }

                ILayerCell cell = dataTable.getCellByPosition(((CellSelectionEvent) e).getColumnPosition(), ((CellSelectionEvent) e).getRowPosition());
                cellValueField.setText(dataDisplayConverter.canonicalToDisplayValue(cell, dataTable.getConfigRegistry(), val).toString());
                ((ScrolledComposite) cellValueField.getParent()).setMinSize(cellValueField.computeSize(SWT.DEFAULT, SWT.DEFAULT));

                log.trace("NATTable CellSelected finish");
            }
        }
    }

    /**
     * Custom Column Header data provider to set column names based on selected
     * members for Compound Datasets.
     */
    private class CompoundDSColumnHeaderDataProvider implements IDataProvider {
        // Column names with CompoundDS separator character '->' left intact.
        // Used in CompoundDSNestedColumnHeader to provide correct nesting structure
        private final String[] columnNamesFull;

        // Simplified base column names without separator character. Used to
        // actually label the columns
        private String[]       columnNames;

        private int            ncols;
        private final int      groupSize;

        public CompoundDSColumnHeaderDataProvider(DataFormat dataObject) {
            CompoundDataFormat dataFormat = (CompoundDataFormat) dataObject;

            int datasetWidth = (int) dataFormat.getWidth();
            Datatype[] types = dataFormat.getSelectedMemberTypes();
            groupSize = dataFormat.getSelectedMemberCount();
            ncols = groupSize * datasetWidth;
            final String[] datasetMemberNames = dataFormat.getMemberNames();
            columnNames = new String[groupSize];
            log.trace("CompoundDSColumnHeaderDataProvider: ncols={}", ncols);

            // Copy selected dataset member names
            int idx = 0;
            for (int i = 0; i < datasetMemberNames.length; i++) {
                if (dataFormat.isMemberSelected(i)) {
                    // Copy the dataset member name reference, so changes to the column name
                    // don't affect the dataset's internal member names
                    columnNames[idx] = new String(datasetMemberNames[i]);
                    columnNames[idx] = columnNames[idx].replaceAll(CompoundDS.separator, "->");

                    if ((types[idx] != null) && (types[idx].isArray())) {
                        Datatype baseType = types[idx].getDatatypeBase();

                        if (baseType.isCompound()) {
                            // If member is type array of compound, list member names in column header
                            List<String> memberNames = baseType.getCompoundMemberNames();

                            columnNames[idx] += "\n\n[ ";

                            for (int j = 0; j < memberNames.size(); j++) {
                                columnNames[idx] += memberNames.get(j);
                                if (j < memberNames.size() - 1) columnNames[idx] += ", ";
                            }

                            columnNames[idx] += " ]";
                        }
                    }

                    idx++;
                }
            }

            // Make a copy of column names so changes to column names don't affect the full column names
            columnNamesFull = Arrays.copyOf(columnNames, columnNames.length);

            // Simplify any nested field column names down to their base names. E.g., a
            // nested field with the full name 'nested_name->a_name' has a simplified column
            // name of 'a_name'
            for (int j = 0; j < columnNames.length; j++) {
                int nestingPosition = columnNames[j].lastIndexOf("->");

                // If this is a nested field, this column's name is whatever follows the last
                // nesting character '->'
                if (nestingPosition >= 0)
                    columnNames[j] = columnNames[j].substring(nestingPosition + 2);
            }
        }

        @Override
        public int getColumnCount() {
            return ncols;
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            return columnNames[columnIndex % groupSize];
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            return;
        }
    }

    /**
     * Implementation of Column Grouping for Compound Datasets with nested members.
     */
    private class CompoundDSNestedColumnHeaderLayer extends ColumnGroupGroupHeaderLayer {
        public CompoundDSNestedColumnHeaderLayer(ColumnGroupHeaderLayer columnGroupHeaderLayer,
                SelectionLayer selectionLayer, ColumnGroupModel columnGroupModel) {
            super(columnGroupHeaderLayer, selectionLayer, columnGroupModel);

            if (curFont != null) {
                this.setRowHeight(2 * curFont.getFontData()[0].getHeight());
                columnGroupHeaderLayer.setRowHeight(2 * curFont.getFontData()[0].getHeight());
            }

            final String[] allColumnNames = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).columnNamesFull;
            final int groupSize = ((CompoundDataFormat) dataObject).getSelectedMemberCount();
            log.trace("CompoundDSNestedColumnHeaderLayer: groupSize={} -- allColumnNames={}", groupSize, allColumnNames);

            // Set up first-level column grouping
            for (int i = 0; i < dataObject.getWidth(); i++) {
                for (int j = 0; j < groupSize; j++) {
                    this.addColumnsIndexesToGroup("" + i, (i * groupSize) + j);
                }
            }

            // Set up any further-nested column groups
            for (int k = 0; k < dataObject.getWidth(); k++) {
                for (int i = 0; i < allColumnNames.length; i++) {
                    int colindex = i + k * allColumnNames.length;
                    int nestingPosition = allColumnNames[i].lastIndexOf("->");

                    if (nestingPosition >= 0) {
                        String columnGroupName = columnGroupModel.getColumnGroupByIndex(colindex).getName();
                        int groupTitleStartPosition = allColumnNames[i].lastIndexOf("->", nestingPosition);

                        if (groupTitleStartPosition == 0) {
                            /* Singly nested member */
                            columnGroupHeaderLayer.addColumnsIndexesToGroup("" + allColumnNames[i].substring(groupTitleStartPosition, nestingPosition) + "{" + columnGroupName + "}", colindex);
                        }
                        else if (groupTitleStartPosition > 0) {
                            /* Member nested at second level or beyond, skip past leading '->' */
                            columnGroupHeaderLayer.addColumnsIndexesToGroup("" + allColumnNames[i].substring(0, groupTitleStartPosition) + "{" + columnGroupName + "}", colindex);
                        }
                        else {
                            columnGroupHeaderLayer.addColumnsIndexesToGroup("" + allColumnNames[i].substring(0, nestingPosition) + "{" + columnGroupName + "}", colindex);
                        }
                    }
                }
            }
        }
    }
}
