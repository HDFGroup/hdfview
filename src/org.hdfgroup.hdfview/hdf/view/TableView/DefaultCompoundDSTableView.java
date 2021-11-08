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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel.ColumnGroup;
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

/**
 * A class to construct a CompoundDS TableView.
 */
public class DefaultCompoundDSTableView extends DefaultBaseTableView implements TableView {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultCompoundDSTableView.class);

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

        isDataTransposed = false; // Disable transpose for compound datasets

        if (!shell.isDisposed()) {
            shell.setImage(ViewProperties.getTableIcon());

            viewer.addDataView(this);

            shell.open();
        }
    }

    @Override
    protected void loadData(DataFormat dataObject) throws Exception {
        super.loadData(dataObject);

        if (dataValue == null) {
            log.debug("loadData(): data value is null");
            throw new RuntimeException("data value is null or not a list");
        }
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
        // Create body layer
        final ColumnGroupModel columnGroupModel = new ColumnGroupModel();
        final ColumnGroupModel secondLevelGroupModel = new ColumnGroupModel();

        try {
            dataProvider = DataProviderFactory.getDataProvider(dataObject, dataValue, isDataTransposed);

            log.trace("createTable(): rows={} : cols={}", dataProvider.getRowCount(), dataProvider.getColumnCount());

            dataLayer = new DataLayer(dataProvider);
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

        // Create popup menu for region or object ref.
        //if (isRegRef || isObjRef) {
        //    natTable.addConfiguration(new RefContextMenu(natTable));
        //}

        natTable.configure();

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
        int cIndex = cName.lastIndexOf('[');
        char nt = ' ';
        if (cIndex >= 0) {
            nt = cName.charAt(cIndex + 1);
        }
        log.trace("getSelectedData(): size={} cName={} nt={}", size, cName, nt);

        if (isRegRef) {
            // reg. ref data are stored in strings
            selectedData = new String[size];
        }
        else {
            switch (nt) {
                case 'B':
                    selectedData = new byte[size];
                    break;
                case 'S':
                    selectedData = new short[size];
                    break;
                case 'I':
                    selectedData = new int[size];
                    break;
                case 'J':
                    selectedData = new long[size];
                    break;
                case 'F':
                    selectedData = new float[size];
                    break;
                case 'D':
                    selectedData = new double[size];
                    break;
                default:
                    selectedData = null;
                    break;
            }
        }

        if (selectedData == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select", "Unsupported data type.");
            return null;
        }

        log.trace("getSelectedData(): selectedData is type {}", nt);

        System.arraycopy(colData, 0, selectedData, 0, size);

        return selectedData;
    }


    @Override
    protected void showObjRefData(long ref) {
        // Currently no support for showing Obj. Ref. Data in Compound Datasets
    }

    @Override
    protected void showRegRefData(String reg) {
        // Currently no support for show Reg. Ref. Data in Compound Datasets
    }

    /**
     * Returns an IEditableRule that determines whether cells can be edited.
     *
     * Cells can be edited as long as the dataset is not opened in read-only mode
     * and the data is not currently displayed in hexadecimal, binary, or character
     * mode.
     *
     * @param dataObject
     *            The dataset for editing
     *
     * @return a new IEditableRule for the dataset
     */
    @Override
    protected IEditableRule getDataEditingRule(DataFormat dataObject) {
        if (dataObject == null) return null;

        // Only Allow editing if not in read-only mode
        return new EditableRule() {
            @Override
            public boolean isEditable(int columnIndex, int rowIndex) {
                /*
                 * TODO: Should be able to edit character-displayed types and datasets when
                 * displayed as hex/binary.
                 */
                //return !(isReadOnly || isDisplayTypeChar || showAsBin || showAsHex);
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
                log.trace("ScalarDSCellSelectionListener: CellSelected isRegRef={} isObjRef={}", isRegRef, isObjRef);

                CellSelectionEvent event = (CellSelectionEvent) e;
                Object val = dataTable.getDataValueByPosition(event.getColumnPosition(), event.getRowPosition());

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
            }
        }
    }

    /**
     * Custom Column Header data provider to set column names based on selected
     * members for Compound Datasets.
     */
    private class CompoundDSColumnHeaderDataProvider implements IDataProvider {
        // Column names with CompoundDS SEPARATOR character '->' left intact.
        // Used in CompoundDSNestedColumnHeader to provide correct nesting structure.
        private final String[]          columnNamesFull;

        // Simplified base column names without separator character. Used to
        // actually label the columns.
        private final ArrayList<String> columnNames;

        private final int               ncols;
        private final int               groupSize;

        public CompoundDSColumnHeaderDataProvider(DataFormat dataObject) {
            CompoundDataFormat dataFormat = (CompoundDataFormat) dataObject;

            Datatype cmpdType = dataObject.getDatatype();
            List<Datatype> selectedTypes = DataFactoryUtils.filterNonSelectedMembers(dataFormat, cmpdType);
            final List<String> datasetMemberNames = Arrays.asList(dataFormat.getSelectedMemberNames());

            columnNames = new ArrayList<>(dataFormat.getSelectedMemberCount());

            recursiveColumnHeaderSetup(columnNames, dataFormat, cmpdType, datasetMemberNames, selectedTypes);

            // Make a copy of column names so changes to column names don't affect the full column names,
            // which is used elsewhere.
            columnNamesFull = Arrays.copyOf(columnNames.toArray(new String[0]), columnNames.size());

            // Simplify any nested field column names down to their base names. E.g., a
            // nested field with the full name 'nested_name->a_name' has a simplified column
            // name of 'a_name'
            for (int j = 0; j < columnNames.size(); j++) {
                String nestedName = columnNames.get(j);
                int nestingPosition = nestedName.lastIndexOf("->");

                // If this is a nested field, this column's name is whatever follows the last
                // nesting character '->'
                if (nestingPosition >= 0)
                    columnNames.set(j, nestedName.substring(nestingPosition + 2));
            }

            groupSize = columnNames.size();

            ncols = columnNames.size() * (int) dataFormat.getWidth();
            log.trace("CompoundDSColumnHeaderDataProvider: ncols={}", ncols);
        }

        private void recursiveColumnHeaderSetup(List<String> outColNames, CompoundDataFormat dataFormat,
                Datatype curDtype, List<String> memberNames, List<Datatype> memberTypes) {

            if (curDtype.isArray()) {
                /*
                 * ARRAY of COMPOUND type
                 */
                int arrSize = 1;
                Datatype nestedCompoundType = curDtype;
                while (nestedCompoundType != null) {
                    if (nestedCompoundType.isCompound()) {
                        break;
                    }
                    else if (nestedCompoundType.isArray()) {
                        long[] arrayDims = nestedCompoundType.getArrayDims();
                        for (int i = 0; i < arrayDims.length; i++) {
                            arrSize *= arrayDims[i];
                        }
                    }

                    nestedCompoundType = nestedCompoundType.getDatatypeBase();
                }

                log.trace("recursiveColumnHeaderSetup(): ARRAY size: {}", arrSize);

                /*
                 * TODO: Temporary workaround for top-level array of compound types.
                 */
                if (memberTypes.isEmpty()) {
                    memberTypes = DataFactoryUtils.filterNonSelectedMembers(dataFormat, nestedCompoundType);
                }

                /*
                 * Duplicate member names by the size of the array.
                 *
                 * NOTE: this assumes that the member names of the ARRAY/VLEN of COMPOUND
                 * directly follow the name of the top-level member itself and will break if
                 * that assumption is not true.
                 */
                StringBuilder sBuilder = new StringBuilder();
                ArrayList<String> nestedMemberNames = new ArrayList<>(arrSize * memberNames.size());
                for (int i = 0; i < arrSize; i++) {
                    for (int j = 0; j < memberNames.size(); j++) {
                        sBuilder.setLength(0);

                        // Copy the dataset member name reference, so changes to the column name
                        // don't affect the dataset's internal member names.
                        sBuilder.append(memberNames.get(j).replaceAll(CompoundDS.SEPARATOR, "->"));

                        /*
                         * Add the index number to the member name so we can correctly setup nested
                         * column grouping.
                         */
                        sBuilder.append("[" + i + "]");

                        nestedMemberNames.add(sBuilder.toString());
                    }
                }

                recursiveColumnHeaderSetup(outColNames, dataFormat, nestedCompoundType, nestedMemberNames, memberTypes);
            }
            else if (curDtype.isVLEN() && !curDtype.isVarStr()) {
                /*
                 * TODO: empty until we have true variable-length support.
                 */
            }
            else if (curDtype.isCompound()) {
                ListIterator<String> localIt = memberNames.listIterator();
                while (localIt.hasNext()) {
                    int curIdx = localIt.nextIndex();
                    String curName = localIt.next();
                    Datatype curType = memberTypes.get(curIdx % memberTypes.size());
                    Datatype nestedArrayOfCompoundType = null;
                    boolean nestedArrayOfCompound = false;

                    /*
                     * Recursively detect any nested array/vlen of compound types and deal with them
                     * by creating multiple copies of the member names.
                     */
                    if (curType.isArray() /* || (curType.isVLEN() && !curType.isVarStr()) */ /* TODO: true variable-length support */) {
                        Datatype base = curType.getDatatypeBase();
                        while (base != null) {
                            if (base.isCompound()) {
                                nestedArrayOfCompound = true;
                                nestedArrayOfCompoundType = base;
                                break;
                            }

                            base = base.getDatatypeBase();
                        }
                    }

                    /*
                     * For ARRAY of COMPOUND and VLEN of COMPOUND types, we repeat the compound
                     * members n times, where n is the number of array or vlen elements.
                     */
                    if (nestedArrayOfCompound) {
                        List<Datatype> selTypes = DataFactoryUtils.filterNonSelectedMembers(dataFormat, nestedArrayOfCompoundType);
                        List<String> selMemberNames = new ArrayList<>(selTypes.size());

                        int arrCmpdLen = calcArrayOfCompoundLen(selTypes);
                        for (int i = 0; i < arrCmpdLen; i++) {
                            selMemberNames.add(localIt.next());
                        }

                        recursiveColumnHeaderSetup(outColNames, dataFormat, curType, selMemberNames, selTypes);
                    }
                    else {
                        // Copy the dataset member name reference, so changes to the column name
                        // don't affect the dataset's internal member names.
                        curName = new String(curName.replaceAll(CompoundDS.SEPARATOR, "->"));

                        outColNames.add(curName);
                    }
                }
            }
        }

        private int calcArrayOfCompoundLen(List<Datatype> datatypes) {
            int count = 0;
            Iterator<Datatype> localIt = datatypes.iterator();
            while (localIt.hasNext()) {
                Datatype curType = localIt.next();

                if (curType.isCompound()) {
                    count += calcArrayOfCompoundLen(curType.getCompoundMemberTypes());
                }
                else if (curType.isArray()) {
                    /*
                     * TODO: nested array of compound length calculation
                     */
                }
                else
                    count++;
            }

            return count;
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
            try {
                return columnNames.get(columnIndex % groupSize);
            }
            catch (Exception ex) {
                log.debug("getDataValue({}, {}): ", rowIndex, columnIndex, ex);
                return "*ERROR*";
            }
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            // Disable column header editing
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
            final int groupSize = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).groupSize;
            log.trace("CompoundDSNestedColumnHeaderLayer: groupSize={} -- allColumnNames={}", groupSize, allColumnNames);

            // Set up first-level column grouping
            int[] indices = new int[groupSize];
            for (int i = 0; i < dataObject.getWidth(); i++) {
                for (int j = 0; j < groupSize; j++) {
                    indices[j] = (i * groupSize) + j;
                }

                this.addColumnsIndexesToGroup(String.valueOf(i), indices);
            }

            // Set up any further-nested column groups
            StringBuilder columnHeaderBuilder = new StringBuilder();
            for (int k = 0; k < dataObject.getWidth(); k++) {
                for (int i = 0; i < allColumnNames.length; i++) {
                    int colindex = i + k * allColumnNames.length;
                    int nestingPosition = allColumnNames[i].lastIndexOf("->");

                    columnHeaderBuilder.setLength(0);

                    if (nestingPosition >= 0) {
                        ColumnGroup nestingGroup = columnGroupModel.getColumnGroupByIndex(colindex);
                        if (nestingGroup != null) {
                            String columnGroupName = nestingGroup.getName();
                            int groupTitleStartPosition = allColumnNames[i].lastIndexOf("->", nestingPosition);
                            String nestingName = allColumnNames[i].substring(nestingPosition + 2);
                            String newGroupName;

                            if (groupTitleStartPosition == 0) {
                                /* Singly nested member */
                                newGroupName = allColumnNames[i].substring(groupTitleStartPosition, nestingPosition);
                            }
                            else if (groupTitleStartPosition > 0) {
                                /* Member nested at second level or beyond, skip past leading '->' */
                                newGroupName = allColumnNames[i].substring(0, groupTitleStartPosition);
                            }
                            else {
                                newGroupName = allColumnNames[i].substring(0, nestingPosition);
                            }

                            columnHeaderBuilder.append(newGroupName);
                            columnHeaderBuilder.append("{").append(columnGroupName).append("}");

                            /*
                             * Special case for ARRAY of COMPOUND and VLEN of COMPOUND types.
                             *
                             * NOTE: This is a quick and dirty way of determining array/vlen of compound
                             * members. It will probably cause weird column grouping behavior if a user uses
                             * the "[number]" pattern in one of their member names, but for now we won't
                             * worry about it.
                             */
                            if (nestingName.matches(".*\\[[0-9]*\\]")) {
                                processArrayOfCompound(columnHeaderBuilder, nestingName);
                            }

                            columnGroupHeaderLayer.addColumnsIndexesToGroup(columnHeaderBuilder.toString(), colindex);
                        }
                        else
                            log.debug("CompoundDSNestedColumnHeaderLayer: nesting group was null for index {}", colindex);
                    }
                    else if (allColumnNames[i].matches(".*\\[[0-9]*\\]")) {
                        /*
                         * Top-level ARRAY of COMPOUND types.
                         */
                        columnHeaderBuilder.append("ARRAY");
                        processArrayOfCompound(columnHeaderBuilder, allColumnNames[i]);

                        columnGroupHeaderLayer.addColumnsIndexesToGroup(columnHeaderBuilder.toString(), colindex);
                    }
                }
            }
        }

        private void processArrayOfCompound(StringBuilder curBuilder, String columnName) {
            Pattern indexPattern = Pattern.compile(".*\\[([0-9]*)\\]");
            Matcher indexMatcher = indexPattern.matcher(columnName);

            /*
             * Group array/vlen of compounds members into array-indexed groups.
             */
            if (indexMatcher.matches()) {
                int containerIndex = 0;

                try {
                    containerIndex = Integer.parseInt(indexMatcher.group(1));
                }
                catch (Exception ex) {
                    log.debug("processArrayOfCompound(): error parsing array/vlen of compound index: ", ex);
                    return;
                }

                curBuilder.append("[").append(containerIndex).append("]");
            }
        }
    }
}
