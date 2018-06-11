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
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.validate.DataValidator;
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
import org.eclipse.swt.widgets.Composite;

import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.object.CompoundDS;
import hdf.object.CompoundDataFormat;
import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.object.h5.H5Datatype;
import hdf.view.Tools;
import hdf.view.ViewManager;
import hdf.view.ViewProperties;

public class DefaultCompoundDSTableView extends DefaultBaseTableView implements TableView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultCompoundDSTableView.class);

    /**
     * Constructs a CompoundDS TableView with no additional data properties.
     *
     * @param theView
     *            the main HDFView.
     */
    public DefaultCompoundDSTableView(ViewManager theView) {
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
    public DefaultCompoundDSTableView(ViewManager theView, HashMap dataPropertiesMap) {
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

        if (!dataObject.isInited()) {
            dataObject.init();
            log.trace("loadData(): inited");
        }

        // use lazy convert for large number of strings
        if (dataObject.getHeight() > 10000 && dataObject instanceof CompoundDS) {
            ((CompoundDS) dataObject).setConvertByteToString(false);
        }

        // Make sure entire dataset is not loaded when looking at 3D
        // datasets using the default display mode (double clicking the
        // data object)
        if (dataObject.getRank() > 2) {
            dataObject.getSelectedDims()[dataObject.getSelectedIndex()[2]] = 1;
        }

        dataValue = null;
        try {
            dataValue = dataObject.getData();
        }
        catch (Throwable ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Load", "CompoundDS loadData: " + ex.getMessage());
            log.debug("loadData(): ", ex);
            dataValue = null;
        }

        if ((dataValue == null) || !(dataValue instanceof List)) {
            log.debug("loadData(): data value is null or data not a list");
            log.trace("loadData(): finish");
            throw new RuntimeException("data value is null");
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

        final IDataProvider bodyDataProvider = getDataProvider(dataObject);
        dataLayer = new DataLayer(bodyDataProvider);
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
            log.debug(
                    "updateValueInMemory({}, {}): cell value not updated; new value is null", row, col);
            log.trace("updateValueInMemory({}, {}): finish", row, col);
            return;
        }

        // No need to update if values are the same
        if (cellValue.equals(dataLayer.getDataValue(col, row).toString())) {
            log.debug(
                    "updateValueInMemory({}, {}): cell value not updated; new value same as old value", row, col);
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
            log.debug(
                    "updateValueInFile(): file not updated; read-only or unchanged data or displayed as hex or binary");
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

    protected IDataProvider getDataProvider(DataFormat dataObject) {
        if (dataObject == null) return null;

        return new CompoundDSDataProvider(dataObject);
    }

    /**
     * Returns an appropriate DisplayConverter to convert data values into
     * human-readable forms in the table. Also converts the human-readable form back
     * into real data when writing the data object back to the file.
     *
     * @param dataObject
     *            The data object whose values are to be converted.
     *
     * @return A new DisplayConverter if the data object is valid, or null
     *         otherwise.
     */
    @Override
    protected DisplayConverter getDataDisplayConverter(final DataFormat dataObject) {
        if (dataObject == null) return null;

        return new CompoundDSDataDisplayConverter(dataObject);
    }

    // TODO: implement DataValidator to validate based upon current column index
    @Override
    protected DataValidator getDataValidator(final DataFormat dataObject) {
        return new DataValidator() {
            @Override
            public boolean validate(int colIndex, int rowIndex, Object newValue) {
                return true;
            }
        };
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
     * Provides the NatTable with data from a Compound Dataset for each cell.
     */
    private class CompoundDSDataProvider implements IDataProvider {
        private Object             theValue;

        // Used to store any data fields of type ARRAY
        private Object[]           arrayElements;

        // StringBuffer used for variable-length types
        private final StringBuffer stringBuffer;

        private final Datatype     types[];

        private final int          orders[];
        private final int          nFields;
        private final int          nRows;
        private final int          nCols;
        private final int          nSubColumns;

        public CompoundDSDataProvider(DataFormat theDataset) {
            log.trace("CompoundDSDataProvider: start");

            CompoundDataFormat dataFormat = (CompoundDataFormat) theDataset;

            stringBuffer = new StringBuffer();

            types = dataFormat.getSelectedMemberTypes();

            orders = dataFormat.getSelectedMemberOrders();
            nFields = ((List<?>) dataValue).size();
            nRows = (int) dataFormat.getHeight();
            nCols = (int) (dataFormat.getWidth() * dataFormat.getSelectedMemberCount());
            nSubColumns = (nFields > 0) ? getColumnCount() / nFields : 0;

            log.trace("CompoundDSDataProvider: finish");
        }

        @Override
        public Object getDataValue(int col, int row) {
            int fieldIdx = col;
            int rowIdx = row;
            log.trace("CompoundDSDataProvider:getDataValue({},{}): start", row, col);

            if (nSubColumns > 1) { // multi-dimension compound dataset
                int colIdx = col / nFields;
                fieldIdx %= nFields;
                rowIdx = row * orders[fieldIdx] * nSubColumns + colIdx * orders[fieldIdx];
                log.trace("CompoundDSDataProvider:getDataValue(): row={} orders[{}]={} nSubColumns={} colIdx={}", row,
                        fieldIdx, orders[fieldIdx], nSubColumns, colIdx);
            }
            else {
                rowIdx = row * orders[fieldIdx];
                log.trace("CompoundDSDataProvider:getDataValue(): row={} orders[{}]={}", row, fieldIdx,
                        orders[fieldIdx]);
            }
            log.trace("CompoundDSDataProvider:getDataValue(): rowIdx={}", rowIdx);

            Object colValue = ((List<?>) dataValue).get(fieldIdx);
            if (colValue == null) {
                return "Null";
            }

            Datatype dtype = types[fieldIdx];

            boolean isUINT64 = false;

            String cName = colValue.getClass().getName();
            int cIndex = cName.lastIndexOf("[");
            if (cIndex >= 0) {
                if (dtype.isUnsigned())
                    isUINT64 = (cName.charAt(cIndex + 1) == 'J');
            }

            if (dtype.isArray()) {
                Datatype btype = dtype.getDatatypeBase();
                if (cIndex >= 0) {
                    if (btype.isUnsigned())
                        isUINT64 = (cName.charAt(cIndex + 1) == 'J');
                }

                log.trace("CompoundDSDataProvider:getDataValue(): Array - isArray={} isString={} isUINT64={}", btype.isArray(),
                        btype.isString(), isUINT64);

                if (btype.isCompound()) {
                    int numberOfMembers = btype.getCompoundMemberNames().size();
                    arrayElements = new Object[orders[fieldIdx] * numberOfMembers];

                    log.trace("CompoundDSDataProvider:getDataValue(): Array - Datatype.CLASS_COMPOUND with {} members",
                            numberOfMembers);
                    for (int i = 0; i < orders[fieldIdx]; i++) {
                        try {
                            Object field_data = null;

                            try {
                                field_data = Array.get(colValue, rowIdx + i);
                            }
                            catch (Exception ex) {
                                log.debug("CompoundDSDataProvider:getDataValue(): Array - could not retrieve field_data: ", ex);
                            }

                            log.trace("CompoundDSDataProvider:getDataValue(): Array - fieldIdx = {}", i);
                            for (int j = 0; j < numberOfMembers; j++) {
                                Object theValue = null;

                                try {
                                    theValue = Array.get(field_data, j);
                                    log.trace("CompoundDSDataProvider:getDataValue(): Array - theValue[{}]={}",
                                            (i * numberOfMembers) + j, theValue.toString());
                                }
                                catch (Exception ex) {
                                    theValue = "*unsupported*";
                                }

                                arrayElements[(i * numberOfMembers) + j] = theValue;
                            }
                        }
                        catch (Exception ex) {
                            log.trace("CompoundDSDataProvider:getDataValue(): Array - ", ex);
                        }
                    }

                    theValue = arrayElements;
                }
                else if (btype.isVLEN()) {
                    stringBuffer.setLength(0); // clear the old string

                    log.trace("CompoundDSDataProvider:getDataValue(): Array - Datatype is VLEN");
                    if (btype.isString()) {
                        for (int i = 0; i < orders[fieldIdx]; i++) {
                            if (i > 0) stringBuffer.append(", ");
                            stringBuffer.append(((String[]) colValue)[rowIdx + i]);
                            log.trace("CompoundDSDataProvider:getDataValue(): Array - theValue[{}]={}", i,
                                    ((String[]) colValue)[rowIdx + i]);
                        }
                    }
                    else {
                        // Only support variable length strings
                        log.debug("**CompoundDSDataProvider:getDataValue(): Array - Unsupported Variable-length of {}",
                                btype.getDatatypeDescription());
                        stringBuffer.append("*unsupported*");
                    }

                    theValue = stringBuffer.toString();
                }
                else if (btype.isString()) {
                    // ARRAY of strings
                    int strlen = (int) btype.getDatatypeSize();
                    int arraylen = (int) types[fieldIdx].getDatatypeSize();
                    arrayElements = new Object[arraylen];

                    log.trace("**CompoundDSDataProvider:getDataValue(): Array - size {}: isString={} of size {}",
                            arraylen, btype.isString(), strlen);
                    int arraycnt = arraylen / strlen;
                    for (int i = 0; i < arraycnt; i++) {
                        String str = new String(((byte[]) colValue), rowIdx * strlen, strlen);
                        int idx = str.indexOf('\0');
                        if (idx > 0) {
                            str = str.substring(0, idx);
                        }
                        log.trace("**CompoundDSDataProvider:getDataValue(): Array - theValue[{}]={}", i, str);

                        arrayElements[i] = str.trim();
                    }

                    theValue = arrayElements;
                }
                else if (btype.isEnum()) {
                    arrayElements = new Object[orders[fieldIdx]];

                    log.trace("**CompoundDSDataProvider:getDataValue(): Array - ENUM");
                    for (int i = 0; i < orders[fieldIdx]; i++) {
                        arrayElements[i] = Array.get(colValue, rowIdx + i);
                    }

                    theValue = arrayElements;
                }
                else if (btype.isOpaque() || btype.isBitField()) {
                    arrayElements = new Object[orders[fieldIdx]];

                    log.trace("**CompoundDSDataProvider:getDataValue(): Array - OPAQUE or BITFILED");
                    int len = (int) btype.getDatatypeSize();
                    byte[] elements = new byte[len];

                    for (int i = 0; i < orders[fieldIdx]; i++) {
                        rowIdx *= len;

                        for (int j = 0; i < len; i++) {
                            elements[j] = Array.getByte(colValue, rowIdx + j);
                        }

                        arrayElements[i] = elements;
                    }

                    theValue = arrayElements;
                }
                else {
                    arrayElements = new Object[orders[fieldIdx]];

                    log.trace("**CompoundDSDataProvider:getDataValue(): Array - OTHER");
                    if (isUINT64) {
                        for (int i = 0; i < orders[fieldIdx]; i++) {
                            arrayElements[i] = Tools.convertUINT64toBigInt(Array.getLong(colValue, rowIdx + i));
                        }
                    }
                    else {
                        for (int i = 0; i < orders[fieldIdx]; i++) {
                            arrayElements[i] = Array.get(colValue, rowIdx + i);
                        }
                    }

                    theValue = arrayElements;
                }
            }
            else if (dtype.isString() && colValue instanceof byte[]) {
                // strings
                int strlen = (int) dtype.getDatatypeSize();
                log.trace("**CompoundDSDataProvider:getDataValue(): isString={} of size {}", strlen);

                String str = new String(((byte[]) colValue), rowIdx * strlen, strlen);
                int idx = str.indexOf('\0');
                if (idx > 0) {
                    str = str.substring(0, idx);
                }

                theValue = str.trim();
            }
            else if (dtype.isOpaque() || dtype.isBitField()) {
                int len = (int) dtype.getDatatypeSize();
                byte[] elements = new byte[len];

                rowIdx *= len;

                for (int i = 0; i < len; i++) {
                    elements[i] = Array.getByte(colValue, rowIdx + i);
                }

                theValue = elements;
            }
            else {
                // Flat numerical types
                if (isUINT64) {
                    theValue = Tools.convertUINT64toBigInt(Array.getLong(colValue, rowIdx));
                }
                else {
                    theValue = Array.get(colValue, rowIdx);
                }
            }

            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            try {
                updateValueInMemory((String) newValue, rowIndex, columnIndex);
            }
            catch (Exception ex) {
                log.debug("CompoundDSDataProvider:setDataValue({}, {}): failure: ", rowIndex, columnIndex, ex);
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

    private class CompoundDSDataDisplayConverter extends DisplayConverter {
        private final StringBuffer buffer;

        private final Datatype[]   types;

        private final int[]        orders;
        private final int          nFields;
        private int                fieldIndex;
        private final int          nSubColumns;

        public CompoundDSDataDisplayConverter(final DataFormat dataObject) {
            log.trace("CompoundDSDataDisplayConverter: start");

            CompoundDataFormat dataFormat = (CompoundDataFormat) dataObject;

            buffer = new StringBuffer();

            types = dataFormat.getSelectedMemberTypes();

            orders = dataFormat.getSelectedMemberOrders();
            nFields = ((List<?>) dataValue).size();
            nSubColumns = (nFields > 0) ? (int) (dataFormat.getWidth() * dataFormat.getSelectedMemberCount()) / nFields : 0;

            log.trace("CompoundDSDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object value) {
            fieldIndex = cell.getColumnIndex();
            if (nSubColumns > 1) fieldIndex %= nFields;
            return canonicalToDisplayValue(value);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            if (value instanceof String) return value;
            log.trace("CompoundDSDataDisplayConverter:canonicalToDisplayValue {} start", value);

            Datatype dtype = types[fieldIndex];

            buffer.setLength(0);

            if (dtype.isArray()) {
                Datatype btype = dtype.getDatatypeBase();
                log.trace("CompoundDSDataDisplayConverter:canonicalToDisplayValue():Array - isArray={} isEnum={} isStr={}", btype.isArray(), btype.isEnum(), btype.isString());

                if (btype.isCompound()) {
                    int numberOfMembers = dtype.getCompoundMemberNames().size();

                    for (int i = 0; i < orders[fieldIndex]; i++) {
                        if (i > 0) buffer.append(", ");

                        buffer.append("[");

                        for (int j = 0; j < numberOfMembers; j++) {
                            if (j > 0) buffer.append(", ");
                            buffer.append(Array.get(value, (i * numberOfMembers) + j));
                        }

                        buffer.append("]");
                    }
                }
                else if (btype.isEnum()) {
                    int len = Array.getLength(value);

                    if (isEnumConverted) {
                        String[] retValues = null;

                        try {
                            retValues = ((H5Datatype) btype).convertEnumValueToName(value);
                        }
                        catch (HDF5Exception ex) {
                            log.trace(
                                    "CompoundDSDataDisplayConverter:canonicalToDisplayValue(): Could not convert enum values to names: ex");
                            retValues = null;
                        }

                        if (retValues != null) {
                            for (int i = 0; i < retValues.length; i++) {
                                if (i > 0) buffer.append(", ");
                                buffer.append(retValues[i]);
                            }
                        }
                    }
                    else {
                        for (int i = 0; i < len; i++) {
                            if (i > 0) buffer.append(", ");
                            buffer.append(Array.get(value, i));
                        }
                    }
                }
                else {
                    for (int i = 0; i < orders[fieldIndex]; i++) {
                        if (i > 0) buffer.append(", ");
                        buffer.append(((Object[]) value)[i]);
                    }
                }
            }
            else if (dtype.isEnum()) {
                if (isEnumConverted) {
                    String[] retValues = null;

                    try {
                        retValues = ((H5Datatype) dtype).convertEnumValueToName(value);
                    }
                    catch (HDF5Exception ex) {
                        log.trace(
                                "CompoundDSDataDisplayConverter:canonicalToDisplayValue(): Could not convert enum values to names: ex");
                        retValues = null;
                    }

                    if (retValues != null)
                        buffer.append(retValues[0]);
                }
                else
                    buffer.append(value);
            }
            else if (dtype.isOpaque() || dtype.isBitField()) {
                for (int i = 0; i < ((byte[]) value).length; i++) {
                    if (i > 0) {
                        buffer.append(dtype.isBitField() ? ":" : " ");
                    }
                    buffer.append(Tools.toHexString(Long.valueOf(((byte[]) value)[i]), 1));
                }
            }
            else if (numberFormat != null) {
                // show in scientific or custom notation
                buffer.append(numberFormat.format(value));
            }
            else {
                buffer.append(value);
            }
            log.trace("CompoundDSDataDisplayConverter:canonicalToDisplayValue {} finish", buffer);

            return buffer;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }
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

                if (val == null) return;

                log.trace("NATTable CellSelected isRegRef={} isObjRef={}", isRegRef, isObjRef);

                int rowStart = ((RowHeaderDataProvider) rowHeaderDataProvider).start;
                int rowStride = ((RowHeaderDataProvider) rowHeaderDataProvider).stride;

                int rowIndex = rowStart + indexBase
                        + dataTable.getRowIndexByPosition(event.getRowPosition()) * rowStride;
                Object fieldName = columnHeaderDataProvider
                        .getDataValue(dataTable.getColumnIndexByPosition(event.getColumnPosition()), 0);

                String colIndex = "";
                int numGroups = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).numGroups;

                if (numGroups > 1) {
                    int groupSize = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).groupSize;
                    colIndex = "[" + String
                            .valueOf((dataTable.getColumnIndexByPosition(event.getColumnPosition())) / groupSize) + "]";
                }

                cellLabel.setText(String.valueOf(rowIndex) + ", " + fieldName + colIndex + " =  ");

                ILayerCell cell = dataTable.getCellByPosition(((CellSelectionEvent) e).getColumnPosition(),
                        ((CellSelectionEvent) e).getRowPosition());
                cellValueField.setText(
                        dataDisplayConverter.canonicalToDisplayValue(cell, dataTable.getConfigRegistry(), val)
                        .toString());

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
        private final int      numGroups;
        private final int      groupSize;

        public CompoundDSColumnHeaderDataProvider(DataFormat dataObject) {
            CompoundDataFormat dataFormat = (CompoundDataFormat) dataObject;

            int datasetWidth = (int) dataFormat.getWidth();
            Datatype[] types = dataFormat.getSelectedMemberTypes();
            groupSize = dataFormat.getSelectedMemberCount();
            numGroups = (datasetWidth * groupSize) / groupSize;
            ncols = groupSize * numGroups;

            String[] datasetMemberNames = dataFormat.getMemberNames();
            columnNames = new String[groupSize];

            // Copy selected dataset member names
            int idx = 0;
            for (int i = 0; i < datasetMemberNames.length; i++) {
                if (dataFormat.isMemberSelected(i)) {
                    // Copy the dataset member name reference, so changes to the column name
                    // don't affect the dataset's internal member names
                    columnNames[idx] = new String(datasetMemberNames[i]);
                    columnNames[idx] = columnNames[idx].replaceAll(CompoundDS.separator, "->");

                    if (types[i].isArray()) {
                        Datatype baseType = types[i].getDatatypeBase();

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

            if (datasetWidth > 1) {
                // Multi-dimension compound dataset, copy column names into new arrays
                // of size (dataset width * count of selected dataset members)
                String[] newColumnNames = new String[datasetWidth * columnNames.length];
                String[] newColumnNamesFull = new String[datasetWidth * columnNames.length];
                for (int i = 0; i < datasetWidth; i++) {
                    for (int j = 0; j < columnNames.length; j++) {
                        newColumnNames[i * columnNames.length + j] = columnNames[j];
                        newColumnNamesFull[i * columnNames.length + j] = columnNames[j];
                    }
                }

                columnNames = newColumnNames;
                columnNamesFull = newColumnNamesFull;
            }
            else {
                // Make a copy of column names so changes to column names don't affect the full
                // column names
                columnNamesFull = Arrays.copyOf(columnNames, columnNames.length);
            }

            // Simplify any nested field column names down to their base names. E.g., a
            // nested field
            // with the full name 'nested_name->a_name' has a simplified column name of
            // 'a_name'
            for (int j = 0; j < columnNames.length; j++) {
                int nestingPosition = columnNames[j].lastIndexOf("->");

                // If this is a nested field, this column's name is whatever follows the last
                // nesting character '->'
                if (nestingPosition >= 0) columnNames[j] = columnNames[j].substring(nestingPosition + 2);
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
            return columnNames[columnIndex];
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
            final int numGroups = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).numGroups;
            final int groupSize = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).groupSize;

            // Set up first-level column grouping
            for (int i = 0; i < numGroups; i++) {
                for (int j = 0; j < groupSize; j++) {
                    this.addColumnsIndexesToGroup("" + i, (i * groupSize) + j);
                }
            }

            // Set up any further-nested column groups
            for (int i = 0; i < allColumnNames.length; i++) {
                int nestingPosition = allColumnNames[i].lastIndexOf("->");

                if (nestingPosition >= 0) {
                    String columnGroupName = columnGroupModel.getColumnGroupByIndex(i).getName();
                    int groupTitleStartPosition = allColumnNames[i].lastIndexOf("->", nestingPosition - 1);

                    if (groupTitleStartPosition == 0) {
                        /* Singly nested member */
                        columnGroupHeaderLayer.addColumnsIndexesToGroup(
                                "" + allColumnNames[i].substring(groupTitleStartPosition, nestingPosition) + "{"
                                        + columnGroupName + "}",
                                        i);
                    }
                    else if (groupTitleStartPosition > 0) {
                        /* Member nested at second level or beyond, skip past leading '->' */
                        columnGroupHeaderLayer.addColumnsIndexesToGroup(
                                "" + allColumnNames[i].substring(groupTitleStartPosition + 2, nestingPosition) + "{"
                                        + columnGroupName + "}",
                                        i);
                    }
                    else {
                        columnGroupHeaderLayer.addColumnsIndexesToGroup(
                                "" + allColumnNames[i].substring(0, nestingPosition) + "{" + columnGroupName + "}", i);
                    }
                }
            }
        }
    }
}
