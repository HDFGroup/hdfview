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

package hdf.view.TableView;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.EditableRule;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectCellCommand;
import org.eclipse.nebula.widgets.nattable.selection.event.CellSelectionEvent;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import hdf.object.Attribute;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.object.Utils;

import hdf.hdf5lib.HDF5Constants;

import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5ReferenceType.H5ReferenceData;
import hdf.object.h5.H5ReferenceType;

import hdf.view.HDFView;
import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;
import hdf.view.dialog.InputDialog;

/**
 * A class to construct a ScalarDS TableView.
 */
public class DefaultScalarDSTableView extends DefaultBaseTableView implements TableView
{
    private static final Logger log = LoggerFactory.getLogger(DefaultScalarDSTableView.class);

    /**
     * Constructs a ScalarDS TableView with no additional data properties.
     *
     * @param theView
     *            the main HDFView.
     */
    public DefaultScalarDSTableView(DataViewManager theView) {
        this(theView, null);
    }

    /**
     * Constructs a ScalarDS TableView with the specified data properties.
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
    public DefaultScalarDSTableView(DataViewManager theView, HashMap dataPropertiesMap) {
        super(theView, dataPropertiesMap);

        if (!shell.isDisposed()) {
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                shell.setImages(ViewProperties.getHdfIcons());
            }
            else {
                shell.setImage(dataObject.getDatatype().isText() ? ViewProperties.getTextIcon() : ViewProperties.getDatasetIcon());
            }

            shell.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    if (dataObject instanceof ScalarDS) {
                        ScalarDS ds = (ScalarDS) dataObject;

                        /*
                         * Reload the data when it is displayed next time because the display type
                         * (table or image) may be different.
                         */
                        if (ds.isImage()) ds.clearData();
                    }
                }
            });

            viewer.addDataView(this);

            shell.open();
        }
    }

    @Override
    protected void loadData(DataFormat dataObject) throws Exception {
        super.loadData(dataObject);

        try {
            if (Tools.applyBitmask(dataValue, bitmask, bitmaskOP)) {
                isReadOnly = true;
                String opName = "Bits ";

                if (bitmaskOP == ViewProperties.BITMASK_OP.AND)
                    opName = "Bitwise AND ";

                String title = indexBaseGroup.getText();
                title += ", " + opName + bitmask;
                indexBaseGroup.setText(title);
            }

            dataObject.convertFromUnsignedC();

            dataValue = dataObject.getData();
        }
        catch (Exception ex) {
            log.debug("loadData(): ", ex);
            dataValue = null;
            throw ex;
        }

        if (dataValue == null) {
            log.debug("loadData(): data value is null");
            throw new RuntimeException("data value is null");
        }

        fillValue = dataObject.getFillValue();
        log.trace("loadData(): fillValue={}", fillValue);

        char runtimeTypeClass = Utils.getJavaObjectRuntimeClass(dataValue);
        log.trace("loadData(): cName={} runtimeTypeClass={}", dataValue.getClass().getName(), runtimeTypeClass);

        /*
         * Convert numerical data into character data; only possible cases are byte[]
         * and short[] (converted from unsigned byte)
         */
        if (isDisplayTypeChar && ((runtimeTypeClass == 'B') || (runtimeTypeClass == 'S'))) {
            int n = Array.getLength(dataValue);
            char[] charData = new char[n];
            for (int i = 0; i < n; i++) {
                if (runtimeTypeClass == 'B')
                    charData[i] = (char) Array.getByte(dataValue, i);
                else if (runtimeTypeClass == 'S')
                    charData[i] = (char) Array.getShort(dataValue, i);
            }

            dataValue = charData;
        }
        else if ((runtimeTypeClass == 'B') && dataObject.getDatatype().isArray()) {
            Datatype baseType = dataObject.getDatatype().getDatatypeBase();
            if (baseType.isString())
                dataValue = Dataset.byteToString((byte[]) dataValue, (int) baseType.getDatatypeSize());
        }
    }

    /**
     * Creates the menubar for the Shell.
     */
    @Override
    protected Menu createMenuBar(final Shell theShell) {
        Menu baseMenu = super.createMenuBar(theShell);
        MenuItem[] baseMenuItems = baseMenu.getItems();
        MenuItem item = null;

        /*****************************************************************************
         *                                                                           *
         * Add in a few MenuItems for importing/exporting data from/to binary files. *
         *                                                                           *
         *****************************************************************************/

        MenuItem importExportMenuItem = null;
        for (int i = 0; i < baseMenuItems.length; i++)
            if (baseMenuItems[i].getText().equals("&Import/Export Data"))
                importExportMenuItem = baseMenuItems[i];

        if (importExportMenuItem != null) {
            Menu importExportMenu = importExportMenuItem.getMenu();
            MenuItem[] importExportMenuItems = importExportMenu.getItems();

            for (int i = 0; i < importExportMenuItems.length; i++)
                if (importExportMenuItems[i].getText().equals("Export Data to"))
                    item = importExportMenuItems[i];

            if (item != null) {
                Menu exportMenu = item.getMenu();

                MenuItem exportAsBinaryMenuItem = new MenuItem(exportMenu, SWT.CASCADE);
                exportAsBinaryMenuItem.setText("Binary File");

                Menu exportAsBinaryMenu = new Menu(exportAsBinaryMenuItem);
                exportAsBinaryMenuItem.setMenu(exportAsBinaryMenu);

                item = new MenuItem(exportAsBinaryMenu, SWT.PUSH);
                item.setText("Native Order");
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        binaryOrder = 1;

                        try {
                            saveAsBinary();
                        }
                        catch (Exception ex) {
                            theShell.getDisplay().beep();
                            Tools.showError(theShell, "Export", ex.getMessage());
                        }
                    }
                });

                item = new MenuItem(exportAsBinaryMenu, SWT.PUSH);
                item.setText("Little Endian");
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        binaryOrder = 2;

                        try {
                            saveAsBinary();
                        }
                        catch (Exception ex) {
                            theShell.getDisplay().beep();
                            Tools.showError(theShell, "Export", ex.getMessage());
                        }
                    }
                });

                item = new MenuItem(exportAsBinaryMenu, SWT.PUSH);
                item.setText("Big Endian");
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        binaryOrder = 3;

                        try {
                            saveAsBinary();
                        }
                        catch (Exception ex) {
                            theShell.getDisplay().beep();
                            Tools.showError(theShell, "Export", ex.getMessage());
                        }
                    }
                });
            }

            item = null;
            for (int i = 0; i < importExportMenuItems.length; i++)
                if (importExportMenuItems[i].getText().equals("Import Data from"))
                    item = importExportMenuItems[i];

            if (item != null) {
                Menu importMenu = item.getMenu();

                MenuItem importAsBinaryMenuItem = new MenuItem(importMenu, SWT.CASCADE);
                importAsBinaryMenuItem.setText("Binary File");

                Menu importAsBinaryMenu = new Menu(importAsBinaryMenuItem);
                importAsBinaryMenuItem.setMenu(importAsBinaryMenu);

                item = new MenuItem(importAsBinaryMenu, SWT.PUSH);
                item.setText("Native Order");
                item.setEnabled(!isReadOnly);
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        binaryOrder = 1;

                        try {
                            importBinaryData();
                        }
                        catch (Exception ex) {
                            Tools.showError(theShell, "Import", ex.getMessage());
                        }
                    }
                });

                item = new MenuItem(importAsBinaryMenu, SWT.PUSH);
                item.setText("Little Endian");
                item.setEnabled(!isReadOnly);
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        binaryOrder = 2;

                        try {
                            importBinaryData();
                        }
                        catch (Exception ex) {
                            Tools.showError(theShell, "Import", ex.getMessage());
                        }
                    }
                });

                item = new MenuItem(importAsBinaryMenu, SWT.PUSH);
                item.setText("Big Endian");
                item.setEnabled(!isReadOnly);
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        binaryOrder = 3;

                        try {
                            importBinaryData();
                        }
                        catch (Exception ex) {
                            Tools.showError(theShell, "Import", ex.getMessage());
                        }
                    }
                });
            }

            new MenuItem(importExportMenu, SWT.SEPARATOR);

            checkFixedDataLength = new MenuItem(importExportMenu, SWT.CHECK);
            checkFixedDataLength.setText("Fixed Data Length");
            checkFixedDataLength.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (!checkFixedDataLength.getSelection()) {
                        fixedDataLength = -1;
                        return;
                    }

                    String str = new InputDialog(theShell, "",
                            "Enter fixed data length when importing text data\n\n"
                                    + "For example, for a text string of \"12345678\"\n\t\tenter 2,"
                                    + "the data will be 12, 34, 56, 78\n\t\tenter 4, the data will be" + "1234, 5678\n")
                            .open();

                    if ((str == null) || (str.length() < 1)) {
                        checkFixedDataLength.setSelection(false);
                        return;
                    }

                    try {
                        fixedDataLength = Integer.parseInt(str);
                    }
                    catch (Exception ex) {
                        fixedDataLength = -1;
                    }

                    if (fixedDataLength < 1) {
                        checkFixedDataLength.setSelection(false);
                    }
                }
            });
        }

        /*****************************************************************************************
         *                                                                                       *
         * Add a section for changing the way that data is displayed, e.g. as hexadecimal values *
         *                                                                                       *
         *****************************************************************************************/

        MenuItem dataDisplayMenuItem = new MenuItem(baseMenu, SWT.CASCADE);
        dataDisplayMenuItem.setText("Data Display");

        Menu dataDisplayMenu = new Menu(theShell, SWT.DROP_DOWN);
        dataDisplayMenuItem.setMenu(dataDisplayMenu);

        checkScientificNotation = new MenuItem(dataDisplayMenu, SWT.CHECK);
        checkScientificNotation.setText("Show Scientific Notation");
        checkScientificNotation.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (checkScientificNotation.getSelection()) {
                    if (checkCustomNotation != null)
                        checkCustomNotation.setSelection(false);
                    if (checkEnum != null)
                        checkEnum.setSelection(false);
                    if (checkHex != null)
                        checkHex.setSelection(false);
                    if (checkBin != null)
                        checkBin.setSelection(false);

                    numberFormat = scientificFormat;
                    showAsHex = false;
                    showAsBin = false;
                }
                else {
                    numberFormat = normalFormat;
                }

                updateDataConversionSettings();

                dataTable.doCommand(new VisualRefreshCommand());

                PositionCoordinate lastSelectedCell = getSelectionLayer().getLastSelectedCellPosition();
                if (lastSelectedCell != null) {
                    /*
                     * Send down a cell selection event for the current cell to update the cell
                     * value labels
                     */
                    dataTable.doCommand(new SelectCellCommand(getSelectionLayer(), lastSelectedCell.columnPosition,
                            lastSelectedCell.rowPosition, false, false));
                }
            }
        });

        checkCustomNotation = new MenuItem(dataDisplayMenu, SWT.CHECK);
        checkCustomNotation.setText("Show Custom Notation");
        checkCustomNotation.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (checkCustomNotation.getSelection()) {
                    if (checkScientificNotation != null)
                        checkScientificNotation.setSelection(false);
                    if (checkEnum != null)
                        checkEnum.setSelection(false);
                    if (checkHex != null)
                        checkHex.setSelection(false);
                    if (checkBin != null)
                        checkBin.setSelection(false);

                    numberFormat = customFormat;
                    showAsHex = false;
                    showAsBin = false;
                }
                else {
                    numberFormat = normalFormat;
                }

                updateDataConversionSettings();

                dataTable.doCommand(new VisualRefreshCommand());

                PositionCoordinate lastSelectedCell = getSelectionLayer().getLastSelectedCellPosition();
                if (lastSelectedCell != null) {
                    /*
                     * Send down a cell selection event for the current cell to update the cell
                     * value labels
                     */
                    dataTable.doCommand(new SelectCellCommand(getSelectionLayer(), lastSelectedCell.columnPosition,
                            lastSelectedCell.rowPosition, false, false));
                }
            }
        });

        item = new MenuItem(dataDisplayMenu, SWT.PUSH);
        item.setText("Create custom notation");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String msg = "Create number format by pattern \nINTEGER . FRACTION E EXPONENT\nusing # for optional digits and 0 for required digits"
                        + "\nwhere, INTEGER: the pattern for the integer part"
                        + "\n       FRACTION: the pattern for the fractional part"
                        + "\n       EXPONENT: the pattern for the exponent part" + "\n\nFor example, "
                        + "\n\t the normalized scientific notation format is \"#.0###E0##\""
                        + "\n\t to make the digits required \"0.00000E000\"\n\n";

                String str = (new InputDialog(theShell, "Create a custom number format", msg, customFormat.toPattern())).open();

                if ((str == null) || (str.length() < 1))
                    return;

                try {
                    customFormat.applyPattern(str);
                }
                catch (Exception ex) {
                    log.debug("Invalid custom number notation format: {}:", str, ex);
                    Tools.showError(shell, "Create", "Invalid custom notation format " + str);
                }
            }
        });

        char runtimeTypeClass = Utils.getJavaObjectRuntimeClass(dataValue);
        boolean isInt = (runtimeTypeClass == 'B' || runtimeTypeClass == 'S' || runtimeTypeClass == 'I'
                || runtimeTypeClass == 'J');

        if (isInt || dataObject.getDatatype().isBitField() || dataObject.getDatatype().isOpaque()) {
            checkHex = new MenuItem(dataDisplayMenu, SWT.CHECK);
            checkHex.setText("Show Hexadecimal");
            checkHex.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showAsHex = checkHex.getSelection();
                    if (showAsHex) {
                        if (checkScientificNotation != null)
                            checkScientificNotation.setSelection(false);
                        if (checkCustomNotation != null)
                            checkCustomNotation.setSelection(false);
                        if (checkEnum != null)
                            checkEnum.setSelection(false);
                        if (checkBin != null)
                            checkBin.setSelection(false);

                        showAsBin = false;
                        numberFormat = normalFormat;
                    }

                    updateDataConversionSettings();

                    dataTable.doCommand(new VisualRefreshCommand());

                    PositionCoordinate lastSelectedCell = getSelectionLayer().getLastSelectedCellPosition();
                    if (lastSelectedCell != null) {
                        /*
                         * Send down a cell selection event for the current cell to update the cell
                         * value labels
                         */
                        dataTable.doCommand(new SelectCellCommand(getSelectionLayer(), lastSelectedCell.columnPosition,
                                lastSelectedCell.rowPosition, false, false));
                    }
                }
            });

            checkBin = new MenuItem(dataDisplayMenu, SWT.CHECK);
            checkBin.setText("Show Binary");
            checkBin.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showAsBin = checkBin.getSelection();
                    if (showAsBin) {
                        if (checkScientificNotation != null)
                            checkScientificNotation.setSelection(false);
                        if (checkCustomNotation != null)
                            checkCustomNotation.setSelection(false);
                        if (checkEnum != null)
                            checkEnum.setSelection(false);
                        if (checkHex != null)
                            checkHex.setSelection(false);

                        showAsHex = false;
                        numberFormat = normalFormat;
                    }

                    updateDataConversionSettings();

                    dataTable.doCommand(new VisualRefreshCommand());

                    PositionCoordinate lastSelectedCell = getSelectionLayer().getLastSelectedCellPosition();
                    if (lastSelectedCell != null) {
                        /*
                         * Send down a cell selection event for the current cell to update the cell
                         * value labels
                         */
                        dataTable.doCommand(new SelectCellCommand(getSelectionLayer(), lastSelectedCell.columnPosition,
                                lastSelectedCell.rowPosition, false, false));
                    }
                }
            });

            checkEnum = new MenuItem(dataDisplayMenu, SWT.CHECK);
            checkEnum.setText("Show Enum Values");
            checkEnum.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    isEnumConverted = checkEnum.getSelection();
                    if (isEnumConverted) {
                        if (checkScientificNotation != null)
                            checkScientificNotation.setSelection(false);
                        if (checkCustomNotation != null)
                            checkCustomNotation.setSelection(false);
                        if (checkHex != null)
                            checkHex.setSelection(false);
                        if (checkBin != null)
                            checkBin.setSelection(false);

                        showAsBin = false;
                        showAsHex = false;
                        numberFormat = normalFormat;
                    }

                    updateDataConversionSettings();

                    dataTable.doCommand(new VisualRefreshCommand());

                    PositionCoordinate lastSelectedCell = getSelectionLayer().getLastSelectedCellPosition();
                    if (lastSelectedCell != null) {
                        /*
                         * Send down a cell selection event for the current cell to update the cell
                         * value labels
                         */
                        dataTable.doCommand(new SelectCellCommand(getSelectionLayer(), lastSelectedCell.columnPosition,
                                lastSelectedCell.rowPosition, false, false));
                    }
                }
            });
        }

        return baseMenu;
    }

    /**
     * Creates a NatTable for a Scalar dataset.
     *
     * @param parent
     *            The parent for the NatTable
     * @param dataObject
     *            The Scalar dataset for the NatTable to display
     *
     * @return The newly created NatTable
     */
    @Override
    protected NatTable createTable(Composite parent, DataFormat dataObject) {
        // Create body layer
        try {
            dataProvider = DataProviderFactory.getDataProvider(dataObject, dataValue, isDataTransposed);

            log.trace("createTable(): rows={} : cols={}", dataProvider.getRowCount(), dataProvider.getColumnCount());

            dataLayer = new DataLayer(dataProvider);
        }
        catch (Exception ex) {
            log.debug("createTable(): failed to retrieve DataProvider for table: ", ex);
            return null;
        }

        selectionLayer = new SelectionLayer(dataLayer);
        final ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);

        dataLayer.setDefaultColumnWidth(80);

        // Create the Column Header layer
        columnHeaderDataProvider = new ScalarDSColumnHeaderDataProvider(dataObject);
        ColumnHeaderLayer columnHeaderLayer = new ColumnHeader(new DataLayer(columnHeaderDataProvider), viewportLayer,
                selectionLayer);

        // Create the Row Header layer
        rowHeaderDataProvider = new RowHeaderDataProvider(dataObject);

        // Try to adapt row height to current font
        int defaultRowHeight = curFont == null ? 20 : (2 * curFont.getFontData()[0].getHeight());

        DataLayer baseLayer = new DataLayer(rowHeaderDataProvider, 40, defaultRowHeight);
        RowHeaderLayer rowHeaderLayer = new RowHeader(baseLayer, viewportLayer, selectionLayer);

        // Create the Corner Layer
        ILayer cornerLayer = new CornerLayer(
                new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
                rowHeaderLayer, columnHeaderLayer);

        // Create the Grid Layer
        GridLayer gridLayer = new EditingGridLayer(viewportLayer, columnHeaderLayer, rowHeaderLayer, cornerLayer);

        final NatTable natTable = new NatTable(parent, gridLayer, false);
        natTable.addConfiguration(new DefaultNatTableStyleConfiguration());
        natTable.addLayerListener(new ScalarDSCellSelectionListener());

        // Create popup menu for region or object ref.
        if (isStdRef || isRegRef || isObjRef)
            natTable.addConfiguration(new RefContextMenu(natTable));

        natTable.configure();

        return natTable;
    }

    /**
     * Returns the selected data values of the ScalarDS
     */
    @Override
    public Object getSelectedData() {
        Object selectedData = null;

        // Since NatTable returns the selected row positions as a Set<Range>, convert
        // this to an Integer[]
        Set<Range> rowPositions = selectionLayer.getSelectedRowPositions();
        log.trace("getSelectedData() rowPositions: {}", rowPositions);
        Set<Integer> selectedRowPos = new LinkedHashSet<>();
        Iterator<Range> i1 = rowPositions.iterator();
        while (i1.hasNext())
            selectedRowPos.addAll(i1.next().getMembers());

        Integer[] selectedRows = selectedRowPos.toArray(new Integer[0]);
        int[] selectedCols = selectionLayer.getSelectedColumnPositions();

        if (selectedRows == null || selectedRows.length <= 0 || selectedCols == null || selectedCols.length <= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select", "No data is selected.");
            return null;
        }

        int size = selectedCols.length * selectedRows.length;
        log.trace("getSelectedData() data size: {}", size);

        // the whole table is selected
        if ((dataTable.getPreferredColumnCount() - 1 == selectedCols.length)
                && (dataTable.getPreferredRowCount() - 1 == selectedRows.length))
            return dataValue;

        if (dataObject.getDatatype().isRef()) {
            // ref data are stored in bytes
            selectedData = new byte[size * (int)dataObject.getDatatype().getDatatypeSize()];
        }
        else {
            switch (Utils.getJavaObjectRuntimeClass(dataValue)) {
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

        log.trace("getSelectedData(): selectedData is type {}", Utils.getJavaObjectRuntimeClass(dataValue));

        int w = dataTable.getPreferredColumnCount() - 1;
        log.trace("getSelectedData(): getColumnCount={}", w);
        int idxSrc = 0;
        int idxDst = 0;
        log.trace("getSelectedData(): Rows.length={} Cols.length={}", selectedRows.length,
                selectedCols.length);
        for (int i = 0; i < selectedRows.length; i++) {
            for (int j = 0; j < selectedCols.length; j++) {
                idxSrc = selectedRows[i] * w + selectedCols[j];
                Object dataArrayValue = null;
                if (dataValue instanceof ArrayList) {
                    dataArrayValue = ((ArrayList)dataValue).get(idxSrc);
                    System.arraycopy(dataArrayValue, 0, selectedData, idxDst, (int)dataObject.getDatatype().getDatatypeSize());
                }
                else {
                    dataArrayValue = Array.get(dataValue, idxSrc);
                    Array.set(selectedData, idxDst, dataArrayValue);
                }
                log.trace("getSelectedData()[{},{}]: dataValue[{}]={} from r{} and c{}", i, j,
                        idxSrc, dataArrayValue, selectedRows[i], selectedCols[j]);
                idxDst++;
            }
        }

        return selectedData;
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
    protected IEditableRule getDataEditingRule(final DataFormat dataObject) {
        if (dataObject == null)
            return null;

        // Only Allow editing if not in read-only mode
        return new EditableRule() {
            @Override
            public boolean isEditable(int columnIndex, int rowIndex) {
                /*
                 * TODO: Should be able to edit character-displayed types and datasets when
                 * displayed as hex/binary.
                 */
                return !(isReadOnly || isDisplayTypeChar || showAsBin || showAsHex);
            }
        };
    }

    /**
     * Display data pointed to by object references. Data of each object is shown in
     * a separate spreadsheet.
     *
     * @param refarr
     *            the array of bytes that contain the object reference information.
     *
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void showObjRefData(byte[] refarr) {
        log.trace("showObjRefData(): start: refarr={}", refarr);

        if (refarr == null || (refarr.length <= 0) || H5Datatype.zeroArrayCheck(refarr)) {
            Tools.showError(shell, "Select", "Could not show object reference data: invalid or null data");
            log.debug("showObjRefData(): refarr is null or invalid");
            return;
        }

        String objref = H5Datatype.descReferenceObject(((HObject) dataObject).getFileFormat().getFID(), refarr);
        log.trace("showObjRefData(): start: objref={}", objref);

        // find the object location
        String oidStr = objref.substring(objref.indexOf('/'), objref.indexOf("H5O_TYPE_OBJ_REF")-1);
        HObject obj = FileFormat.findObject(((HObject) dataObject).getFileFormat(), oidStr);
        if (obj == null || !(obj instanceof ScalarDS)) {
            Tools.showError(shell, "Select", "Could not show object reference data: invalid or null data");
            log.debug("showObjRefData(): obj is null or not a Scalar Dataset");
            return;
        }

        ScalarDS dset = (ScalarDS) obj;
        ScalarDS dsetCopy = null;

        // create an instance of the dataset constructor
        Constructor<? extends ScalarDS> constructor = null;
        Object[] paramObj = null;
        Object data = null;

        try {
            Class[] paramClass = { FileFormat.class, String.class, String.class };
            constructor = dset.getClass().getConstructor(paramClass);
            paramObj = new Object[] { dset.getFileFormat(), dset.getName(), dset.getPath() };
            dsetCopy = constructor.newInstance(paramObj);
            data = dsetCopy.getData();
        }
        catch (Exception ex) {
            log.debug("showObjRefData(): couldn't show data: ", ex);
            Tools.showError(shell, "Select", "Object Reference: " + ex.getMessage());
            data = null;
        }

        if (data == null)
            return;

        Class<?> theClass = null;
        String viewName = null;

        switch (viewType) {
        case IMAGE:
            viewName = HDFView.getListOfImageViews().get(0);
            break;
        case TABLE:
            viewName = (String) HDFView.getListOfTableViews().get(0);
            break;
        default:
            viewName = null;
        }

        try {
            theClass = Class.forName(viewName);
        }
        catch (Exception ex) {
            try {
                theClass = ViewProperties.loadExtClass().loadClass(viewName);
            }
            catch (Exception ex2) {
                theClass = null;
            }
        }

        // Use default dataview
        if (theClass == null) {
            switch (viewType) {
            case IMAGE:
                viewName = ViewProperties.DEFAULT_IMAGEVIEW_NAME;
                break;
            case TABLE:
                viewName = ViewProperties.DEFAULT_SCALAR_DATASET_TABLEVIEW_NAME;
                break;
            default:
                viewName = null;
            }

            try {
                theClass = Class.forName(viewName);
            }
            catch (Exception ex) {
                log.debug("showObjRefData(): no suitable display class found");
                Tools.showError(shell, "Select", "Could not show reference data: no suitable display class found");
                return;
            }
        }

        HashMap map = new HashMap(1);
        map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, dsetCopy);
        Object[] args = { viewer, map };

        try {
            Tools.newInstance(theClass, args);
        }
        catch (Exception ex) {
            log.debug("showObjRefData(): Could not show reference data: ", ex);
            Tools.showError(shell, "Select", "Could not show reference data: " + ex.toString());
        }
    }

    /**
     * Display data pointed to by region references. Data of each region is shown in
     * a separate spreadsheet.
     *
     * @param refarr
     *            the array of bytes that contain the reg. ref information.
     *
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void showRegRefData(byte[] refarr) {
        if (refarr == null || (refarr.length <= 0) || H5Datatype.zeroArrayCheck(refarr)) {
            Tools.showError(shell, "Select", "Could not show region reference data: invalid or null data");
            log.debug("showRegRefData(): refarr is null or invalid");
            return;
        }

        String reg = null;
        if (refarr.length == HDF5Constants.H5R_DSET_REG_REF_BUF_SIZE)
            reg = H5Datatype.descRegionDataset(((HObject) dataObject).getFileFormat().getFID(), refarr);
        else
            reg = ((H5ReferenceType)dataObject.getDatatype()).getReferenceRegion(refarr, false);

        boolean isPointSelection = (reg.indexOf('-') <= 0);

        // find the object location
        String oidStr = reg.substring(reg.indexOf('/'), reg.indexOf("REGION_TYPE")-1);

        // decode the region selection
        String regStr = reg.substring(reg.indexOf('{') + 1, reg.indexOf('}'));
        if (regStr == null || regStr.length() <= 0) {
            Tools.showError(shell, "Select", "Could not show region reference data: no region selection made.");
            log.debug("showRegRefData(): no region selection made");
            return; // no selection
        }

        // TODO: do we need to do something with what's past the closing bracket
        // regStr = reg.substring(reg.indexOf('}') + 1);

        StringTokenizer st = new StringTokenizer(regStr);
        int nSelections = st.countTokens();
        if (nSelections <= 0) {
            Tools.showError(shell, "Select", "Could not show region reference data: no region selection made.");
            log.debug("showRegRefData(): no region selection made");
            return; // no selection
        }

        HObject obj = FileFormat.findObject(((HObject) dataObject).getFileFormat(), oidStr);
        if (obj == null || !(obj instanceof ScalarDS)) {
            Tools.showError(shell, "Select", "Could not show object reference data: invalid or null data");
            log.debug("showRegRefData(): obj is null or not a Scalar Dataset");
            return;
        }

        ScalarDS dset = (ScalarDS) obj;
        ScalarDS dsetCopy = null;

        // create an instance of the dataset constructor
        Constructor<? extends ScalarDS> constructor = null;
        Object[] paramObj = null;
        try {
            Class[] paramClass = { FileFormat.class, String.class, String.class };
            constructor = dset.getClass().getConstructor(paramClass);
            paramObj = new Object[] { dset.getFileFormat(), dset.getName(), dset.getPath() };
        }
        catch (Exception ex) {
            log.debug("showRegRefData(): constructor failure: ", ex);
            constructor = null;
        }

        // load each selection into a separate dataset and display it in
        // a separate spreadsheet

        while (st.hasMoreTokens()) {
            try {
                dsetCopy = constructor.newInstance(paramObj);
            }
            catch (Exception ex) {
                log.debug("showRegRefData(): constructor newInstance failure: ", ex);
                continue;
            }

            if (dsetCopy == null) {
                log.debug("showRegRefData(): continue after null dataset copy");
                continue;
            }

            try {
                dsetCopy.init();
            }
            catch (Exception ex) {
                log.debug("showRegRefData(): continue after copied dataset init failure: ", ex);
                continue;
            }

            dsetCopy.getRank();
            long[] start = dsetCopy.getStartDims();
            long[] count = dsetCopy.getSelectedDims();

            // set the selected dimension sizes based on the region selection
            // info.
            int idx = 0;
            String sizeStr = null;
            String token = st.nextToken();

            token = token.replace('(', ' ');
            token = token.replace(')', ' ');
            if (isPointSelection) {
                // point selection
                StringTokenizer tmp = new StringTokenizer(token, ",");
                while (tmp.hasMoreTokens()) {
                    count[idx] = 1;
                    sizeStr = tmp.nextToken().trim();
                    start[idx] = Long.valueOf(sizeStr);
                    idx++;
                }
            }
            else {
                // rectangle selection
                String startStr = token.substring(0, token.indexOf('-'));
                String endStr = token.substring(token.indexOf('-') + 1);
                StringTokenizer tmp = new StringTokenizer(startStr, ",");
                while (tmp.hasMoreTokens()) {
                    sizeStr = tmp.nextToken().trim();
                    start[idx] = Long.valueOf(sizeStr);
                    idx++;
                }

                idx = 0;
                tmp = new StringTokenizer(endStr, ",");
                while (tmp.hasMoreTokens()) {
                    sizeStr = tmp.nextToken().trim();
                    count[idx] = Long.valueOf(sizeStr) - start[idx] + 1;
                    idx++;
                }
            }

            try {
                dsetCopy.getData();
            }
            catch (Exception ex) {
                log.debug("showRegRefData(): getData failure: ", ex);
                Tools.showError(shell, "Select", "Region Reference: " + ex.getMessage());
            }

            Class<?> theClass = null;
            String viewName = null;

            switch (viewType) {
            case IMAGE:
                viewName = HDFView.getListOfImageViews().get(0);
                break;
            case TABLE:
                viewName = (String) HDFView.getListOfTableViews().get(0);
                break;
            default:
                viewName = null;
            }

            try {
                theClass = Class.forName(viewName);
            }
            catch (Exception ex) {
                try {
                    theClass = ViewProperties.loadExtClass().loadClass(viewName);
                }
                catch (Exception ex2) {
                    theClass = null;
                }
            }

            // Use default dataview
            if (theClass == null) {
                switch (viewType) {
                case IMAGE:
                    viewName = ViewProperties.DEFAULT_IMAGEVIEW_NAME;
                    break;
                case TABLE:
                    viewName = ViewProperties.DEFAULT_SCALAR_DATASET_TABLEVIEW_NAME;
                    break;
                default:
                    viewName = null;
                }

                try {
                    theClass = Class.forName(viewName);
                }
                catch (Exception ex) {
                    log.debug("showRegRefData(): no suitable display class found");
                    Tools.showError(shell, "Select", "Could not show reference data: no suitable display class found");
                    return;
                }
            }

            HashMap map = new HashMap(1);
            map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, dsetCopy);
            Object[] args = { viewer, map };

            try {
                Tools.newInstance(theClass, args);
            }
            catch (Exception ex) {
                log.debug("showRegRefData(): Could not show reference data: ", ex);
                Tools.showError(shell, "Select", "Could not show reference data: " + ex.toString());
            }
        } // (st.hasMoreTokens())
    } // end of showRegRefData()

    /**
     * Display data pointed to by references. Data of each reference is shown in
     * a separate spreadsheet. The std. ref. information is stored in bytes
     *
     * @param refarr
     *            the array of bytes that contain the std. ref information.
     *
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void showStdRefData(byte[] refarr) {
        if (refarr == null || H5ReferenceType.zeroArrayCheck(refarr)) {
            Tools.showError(shell, "Select", "Could not show region reference data: invalid or null data");
            log.debug("showStdRefData(): ref is null or invalid");
            return;
        }

        H5ReferenceType refType = ((H5ReferenceType) dataObject.getDatatype());
        H5ReferenceData refdata = refType.getReferenceData(refarr);
        /* get the filename associated with the reference */
        String reffile = refdata.file_name;
        if ((refdata.ref_type == HDF5Constants.H5R_DATASET_REGION1) ||
                (refdata.ref_type == HDF5Constants.H5R_DATASET_REGION2)) {
            String ref_ptr = refType.getReferenceRegion(refarr, false);
            if ("REGION_TYPE UNKNOWN".equals(refdata.region_type)) {
                String msg = "Reference to " + ref_ptr + " cannot be displayed in a table";
                Tools.showInformation(shell, "Reference", msg);
            }
            else {
                showRegRefData(refarr);
            }
        }
        if (((refdata.ref_type == HDF5Constants.H5R_OBJECT1) && (refdata.obj_type == HDF5Constants.H5O_TYPE_DATASET)) ||
                ((refdata.ref_type == HDF5Constants.H5R_OBJECT2) && (refdata.obj_type == HDF5Constants.H5O_TYPE_DATASET))) {
            String ref_obj = refdata.obj_name;
            showObjStdRefData(ref_obj);
        }
        else if (refdata.ref_type == HDF5Constants.H5R_ATTR) {
            String ref_attr_name = refdata.attr_name;
            String ref_obj_name = refdata.obj_name;
            showAttrStdRefData(ref_obj_name, ref_attr_name);
        }
        else if ("H5O_TYPE_OBJ_REF".equals(refdata.region_type)) {
            String msg = "Reference to " + refdata.obj_name + " cannot be displayed in a table";
            //String ref_ptr = refType.getObjectReferenceName(refarr);
            Tools.showInformation(shell, "Reference", msg);
        }
        else {
            // Other types
        }
    } // end of showStdRefData(byte[] refarr)

    /**
     * Display data pointed to by object references. Data of each object is shown in
     * a separate spreadsheet.
     *
     * @param ref
     *            the string that contain the object reference information.
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void showObjStdRefData(String ref) {
        if (ref == null || (ref.length() <= 0) || (ref.compareTo("NULL") == 0)) {
            Tools.showError(shell, "Select", "Could not show object reference data: invalid or null data");
            log.debug("showObjStdRefData(): ref is null or invalid");
            return;
        }

        HObject obj = FileFormat.findObject(((HObject) dataObject).getFileFormat(), ref);
        if (obj == null || !(obj instanceof ScalarDS)) {
            Tools.showError(shell, "Select", "Could not show object reference data: invalid or null data");
            log.debug("showObjStdRefData(): obj is null or not a Scalar Dataset");
            return;
        }

        ScalarDS dset = (ScalarDS) obj;
        ScalarDS dsetCopy = null;

        // create an instance of the dataset constructor
        Constructor<? extends ScalarDS> constructor = null;
        Object[] paramObj = null;
        Object data = null;

        try {
            Class[] paramClass = { FileFormat.class, String.class, String.class };
            constructor = dset.getClass().getConstructor(paramClass);
            paramObj = new Object[] { dset.getFileFormat(), dset.getName(), dset.getPath() };
            dsetCopy = constructor.newInstance(paramObj);
            data = dsetCopy.getData();
        }
        catch (Exception ex) {
            log.debug("showObjStdRefData(): couldn't show data: ", ex);
            Tools.showError(shell, "Select", "Object Reference: " + ex.getMessage());
            data = null;
        }

        if (data == null)
            return;

        Class<?> theClass = null;
        String viewName = null;

        switch (viewType) {
        case IMAGE:
            viewName = HDFView.getListOfImageViews().get(0);
            break;
        case TABLE:
            viewName = (String) HDFView.getListOfTableViews().get(0);
            break;
        default:
            viewName = null;
        }

        try {
            theClass = Class.forName(viewName);
        }
        catch (Exception ex) {
            try {
                theClass = ViewProperties.loadExtClass().loadClass(viewName);
            }
            catch (Exception ex2) {
                theClass = null;
            }
        }

        // Use default dataview
        if (theClass == null) {
            switch (viewType) {
            case IMAGE:
                viewName = ViewProperties.DEFAULT_IMAGEVIEW_NAME;
                break;
            case TABLE:
                viewName = ViewProperties.DEFAULT_SCALAR_DATASET_TABLEVIEW_NAME;
                break;
            default:
                viewName = null;
            }

            try {
                theClass = Class.forName(viewName);
            }
            catch (Exception ex) {
                log.debug("showObjStdRefData(): no suitable display class found");
                Tools.showError(shell, "Select", "Could not show reference data: no suitable display class found");
                return;
            }
        }

        HashMap map = new HashMap(1);
        map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, dsetCopy);
        Object[] args = { viewer, map };

        try {
            Tools.newInstance(theClass, args);
        }
        catch (Exception ex) {
            log.debug("showObjStdRefData(): Could not show reference data: ", ex);
            Tools.showError(shell, "Select", "Could not show reference data: " + ex.toString());
        }
    }

    /**
     * Display data pointed to by attribute references. Data of each object is shown in
     * a separate spreadsheet.
     *
     * @param ref_obj_name
     *            the string that contain the attribute reference information.
     * @param ref_attr_name
     *            the string that contain the attribute reference information.
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void showAttrStdRefData(String ref_obj_name, String ref_attr_name) {
        if (ref_obj_name == null || (ref_obj_name.length() <= 0) || (ref_obj_name.compareTo("NULL") == 0)) {
            log.debug("showAttrStdRefData(): ref_obj_name is null or invalid");
            Tools.showError(shell, "Select", "Could not show attribute reference data: invalid or null object name");
            return;
        }

        if (ref_attr_name == null || (ref_attr_name.length() <= 0) || (ref_attr_name.compareTo("NULL") == 0)) {
            log.debug("showAttrStdRefData(): ref_attr_name is null or invalid");
            Tools.showError(shell, "Select", "Could not show attribute reference data: invalid or null attribute name");
            return;
        }

        // find the parent object first
        HObject obj = FileFormat.findObject(((HObject) dataObject).getFileFormat(), ref_obj_name);
        if (obj == null) {
            log.debug("showAttrStdRefData(): obj is null");
            Tools.showError(shell, "Select", "Could not show attribute reference data: invalid or null data");
            return;
        }
        List<Attribute> attrs = H5File.getAttribute(obj);
        if ((attrs == null) || (attrs.size() < 1)) {
            log.debug("showAttrStdRefData(): attrs is null");
            Tools.showError(shell, "Select", "Could not show attribute reference data: no attributes found");
            return;
        }
        H5ScalarAttr attr = null;
        H5ScalarAttr attrCopy = null;
        int n = attrs.size();
        for (int i = 0; i < n; i++) {
            attr = (H5ScalarAttr)attrs.get(i);
            if (attr.getAttributeName().equals(ref_attr_name))
                break;
            else
                attr = null;
        }

        // create an instance of the Attribute constructor
        Constructor<? extends H5ScalarAttr> constructor = null;
        Object[] paramObj = null;
        Object data = null;

        try {
            Class[] paramClass = { HObject.class, String.class, Datatype.class, long[].class };
            constructor = attr.getClass().getConstructor(paramClass);
            paramObj = new Object[] { obj, attr.getName(), attr.getDatatype(), null };
            attrCopy = constructor.newInstance(paramObj);
            data = attrCopy.getData();
        }
        catch (Exception ex) {
            log.debug("showAttrStdRefData(): couldn't show data: ", ex);
            Tools.showError(shell, "Select", "Attribute Reference: " + ex.getMessage());
            data = null;
        }

        if (data == null)
            return;

        Class<?> theClass = null;
        String viewName = null;

        switch (viewType) {
        case IMAGE:
            viewName = HDFView.getListOfImageViews().get(0);
            break;
        case TABLE:
            viewName = (String) HDFView.getListOfTableViews().get(0);
            break;
        default:
            viewName = null;
        }

        try {
            theClass = Class.forName(viewName);
        }
        catch (Exception ex) {
            try {
                theClass = ViewProperties.loadExtClass().loadClass(viewName);
            }
            catch (Exception ex2) {
                theClass = null;
            }
        }

        // Use default dataview
        if (theClass == null) {
            switch (viewType) {
            case IMAGE:
                viewName = ViewProperties.DEFAULT_IMAGEVIEW_NAME;
                break;
            case TABLE:
                viewName = ViewProperties.DEFAULT_SCALAR_DATASET_TABLEVIEW_NAME;
                break;
            default:
                viewName = null;
            }

            try {
                theClass = Class.forName(viewName);
            }
            catch (Exception ex) {
                log.debug("showAttrStdRefData(): no suitable display class found");
                Tools.showError(shell, "Select", "Could not show reference data: no suitable display class found");
                return;
            }
        }

        HashMap map = new HashMap(1);
        map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, attrCopy);
        Object[] args = { viewer, map };

        try {
            Tools.newInstance(theClass, args);
        }
        catch (Exception ex) {
            log.debug("showAttrStdRefData(): Could not show reference data: ", ex);
            Tools.showError(shell, "Select", "Could not show reference data: " + ex.toString());
        }
    }

    /**
     * Update cell value label and cell value field when a cell is selected
     */
    private class ScalarDSCellSelectionListener implements ILayerListener
    {
        @Override
        public void handleLayerEvent(ILayerEvent e) {
            if (e instanceof CellSelectionEvent) {
                CellSelectionEvent event = (CellSelectionEvent) e;
                Object val = dataTable.getDataValueByPosition(event.getColumnPosition(), event.getRowPosition());
                String strVal = null;

                String[] columnNames = ((ScalarDSColumnHeaderDataProvider) columnHeaderDataProvider).columnNames;
                int rowStart = ((RowHeaderDataProvider) rowHeaderDataProvider).start;
                int rowStride = ((RowHeaderDataProvider) rowHeaderDataProvider).stride;

                cellLabel.setText(String.valueOf(
                        rowStart + indexBase + dataTable.getRowIndexByPosition(event.getRowPosition()) * rowStride)
                        + ", " + columnNames[dataTable.getColumnIndexByPosition(event.getColumnPosition())] + "  =  ");

                if (val == null) {
                    cellValueField.setText("Null");
                    ((ScrolledComposite) cellValueField.getParent()).setMinSize(cellValueField.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                    return;
                }

                if (isStdRef) {
                    boolean displayValues = ViewProperties.showRegRefValues();

                    if (displayValues && val != null && !(val instanceof String)) {//((String) val).compareTo("NULL") != 0) {
                        strVal = ((H5ReferenceType) dataObject.getDatatype()).getReferenceRegion((byte[])val, true);
                    }
                }
                else if (isRegRef) {
                    boolean displayValues = ViewProperties.showRegRefValues();

                    if (displayValues && val != null && ((String) val).compareTo("NULL") != 0) {
                        String reg = (String) val;
                        boolean isPointSelection = (reg.indexOf('-') <= 0);

                        // find the object location
                        String oidStr = reg.substring(reg.indexOf('/'), reg.indexOf(' '));

                        // decode the region selection
                        String regStr = reg.substring(reg.indexOf('{') + 1, reg.indexOf('}'));

                        // no selection
                        if (regStr == null || regStr.length() <= 0) {
                            log.debug("ScalarDSCellSelectionListener:RegRef CellSelected: no selection made");
                            strVal = null;
                        }
                        else {
                            // TODO: do we need to do something with what's past the closing bracket
                            // regStr = reg.substring(reg.indexOf('}') + 1);

                            StringTokenizer st = new StringTokenizer(regStr);
                            int nSelections = st.countTokens();
                            if (nSelections <= 0) { // no selection
                                strVal = null;
                            }
                            else {
                                HObject obj = FileFormat.findObject(((HObject) dataObject).getFileFormat(), oidStr);
                                if (obj == null || !(obj instanceof ScalarDS)) { // no selection
                                    strVal = null;
                                }
                                else {
                                    ScalarDS dset = (ScalarDS) obj;
                                    try {
                                        dset.init();
                                    }
                                    catch (Exception ex) {
                                        log.debug("ScalarDSCellSelectionListener:RegRef CellSelected: reference dset did not init()", ex);
                                    }
                                    StringBuilder strvalSB = new StringBuilder();

                                    int idx = 0;
                                    while (st.hasMoreTokens()) {
                                        int space_type = dset.getSpaceType();
                                        int rank = dset.getRank();
                                        long[] start = dset.getStartDims();
                                        long[] count = dset.getSelectedDims();
                                        // long count[] = new long[rank];

                                        // set the selected dimension sizes
                                        // based on the region selection
                                        // info.
                                        String sizeStr = null;
                                        String token = st.nextToken();

                                        token = token.replace('(', ' ');
                                        token = token.replace(')', ' ');
                                        if (isPointSelection) {
                                            // point selection
                                            String[] tmp = token.split(",");
                                            for (int x = 0; x < tmp.length; x++) {
                                                count[x] = 1;
                                                sizeStr = tmp[x].trim();
                                                start[x] = Long.valueOf(sizeStr);
                                                log.trace("ScalarDSCellSelectionListener:RegRef CellSelected: point sel={}", tmp[x]);
                                            }
                                        }
                                        else {
                                            // rectangle selection
                                            String startStr = token.substring(0, token.indexOf('-'));
                                            String endStr = token.substring(token.indexOf('-') + 1);
                                            log.trace("ScalarDSCellSelectionListener:RegRef CellSelected: rect sel with startStr={} endStr={}",
                                                    startStr, endStr);
                                            String[] tmp = startStr.split(",");
                                            log.trace("ScalarDSCellSelectionListener:RegRef CellSelected: tmp with length={} rank={}",
                                                    tmp.length, rank);
                                            for (int x = 0; x < tmp.length; x++) {
                                                sizeStr = tmp[x].trim();
                                                start[x] = Long.valueOf(sizeStr);
                                                log.trace("ScalarDSCellSelectionListener:RegRef CellSelected: rect start={}",
                                                        tmp[x]);
                                            }
                                            tmp = endStr.split(",");
                                            for (int x = 0; x < tmp.length; x++) {
                                                sizeStr = tmp[x].trim();
                                                count[x] = Long.valueOf(sizeStr) - start[x] + 1;
                                                log.trace("ScalarDSCellSelectionListener:RegRef CellSelected: rect end={} count={}",
                                                        tmp[x], count[x]);
                                            }
                                        }

                                        Object dbuf = null;
                                        try {
                                            dbuf = dset.getData();
                                        }
                                        catch (Exception ex) {
                                            Tools.showError(shell, "Select", "Region Reference:" +ex.getMessage());
                                        }

                                        /* Convert dbuf to a displayable string */
                                        char runtimeTypeClass = Utils.getJavaObjectRuntimeClass(dbuf);
                                        log.trace("ScalarDSCellSelectionListener:RegRef CellSelected: cName={} runtimeTypeClass={}",
                                                dbuf.getClass().getName(), runtimeTypeClass);

                                        if (idx > 0) strvalSB.append(',');

                                        // convert numerical data into char
                                        // only possible cases are byte[]
                                        // and short[] (converted from
                                        // unsigned byte)
                                        Datatype dtype = dset.getDatatype();
                                        Datatype baseType = dtype.getDatatypeBase();
                                        log.trace("ScalarDSCellSelectionListener:RegRef CellSelected: dtype={} baseType={}",
                                                dtype.getDescription(), baseType);
                                        if (baseType == null)
                                            baseType = dtype;
                                        if ((dtype.isArray() && baseType.isChar())
                                                && ((runtimeTypeClass == 'B') || (runtimeTypeClass == 'S'))) {
                                            int n = Array.getLength(dbuf);
                                            log.trace("ScalarDSCellSelectionListener:RegRef CellSelected charData length = {}", n);
                                            char[] charData = new char[n];
                                            for (int i = 0; i < n; i++) {
                                                if (runtimeTypeClass == 'B') {
                                                    charData[i] = (char) Array.getByte(dbuf, i);
                                                }
                                                else if (runtimeTypeClass == 'S') {
                                                    charData[i] = (char) Array.getShort(dbuf, i);
                                                }
                                            }

                                            strvalSB.append(charData);
                                        }
                                        else {
                                            // numerical values
                                            boolean isUnsigned = dtype.isUnsigned();
                                            if (dtype.isArray())
                                                isUnsigned = baseType.isUnsigned();
                                            int n = Array.getLength(dbuf);
                                            if (isUnsigned) {
                                                switch (runtimeTypeClass) {
                                                case 'B':
                                                    byte[] barray = (byte[]) dbuf;
                                                    short sValue = barray[0];
                                                    if (sValue < 0) {
                                                        sValue += 256;
                                                    }
                                                    strvalSB.append(sValue);
                                                    for (int i = 1; i < n; i++) {
                                                        strvalSB.append(',');
                                                        sValue = barray[i];
                                                        if (sValue < 0) {
                                                            sValue += 256;
                                                        }
                                                        strvalSB.append(sValue);
                                                    }
                                                    break;
                                                case 'S':
                                                    short[] sarray = (short[]) dbuf;
                                                    int iValue = sarray[0];
                                                    if (iValue < 0) {
                                                        iValue += 65536;
                                                    }
                                                    strvalSB.append(iValue);
                                                    for (int i = 1; i < n; i++) {
                                                        strvalSB.append(',');
                                                        iValue = sarray[i];
                                                        if (iValue < 0) {
                                                            iValue += 65536;
                                                        }
                                                        strvalSB.append(iValue);
                                                    }
                                                    break;
                                                case 'I':
                                                    int[] iarray = (int[]) dbuf;
                                                    long lValue = iarray[0];
                                                    if (lValue < 0) {
                                                        lValue += 4294967296L;
                                                    }
                                                    strvalSB.append(lValue);
                                                    for (int i = 1; i < n; i++) {
                                                        strvalSB.append(',');
                                                        lValue = iarray[i];
                                                        if (lValue < 0) {
                                                            lValue += 4294967296L;
                                                        }
                                                        strvalSB.append(lValue);
                                                    }
                                                    break;
                                                case 'J':
                                                    long[] larray = (long[]) dbuf;
                                                    Long l = larray[0];
                                                    String theValue = Long.toString(l);
                                                    if (l < 0) {
                                                        l = (l << 1) >>> 1;
                                                        BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                                                        BigInteger big2 = new BigInteger(l.toString());
                                                        BigInteger big = big1.add(big2);
                                                        theValue = big.toString();
                                                    }
                                                    strvalSB.append(theValue);
                                                    for (int i = 1; i < n; i++) {
                                                        strvalSB.append(',');
                                                        l = larray[i];
                                                        theValue = Long.toString(l);
                                                        if (l < 0) {
                                                            l = (l << 1) >>> 1;
                                                            BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                                                            BigInteger big2 = new BigInteger(l.toString());
                                                            BigInteger big = big1.add(big2);
                                                            theValue = big.toString();
                                                        }
                                                        strvalSB.append(theValue);
                                                    }
                                                    break;
                                                default:
                                                    strvalSB.append(Array.get(dbuf, 0));
                                                    for (int i = 1; i < n; i++) {
                                                        strvalSB.append(',');
                                                        strvalSB.append(Array.get(dbuf, i));
                                                    }
                                                    break;
                                                }
                                            }
                                            else {
                                                for (int x = 0; x < n; x++) {
                                                    Object theValue = Array.get(dbuf, x);
                                                    if (x > 0) strvalSB.append(',');
                                                    strvalSB.append(theValue);
                                                }
                                            }
                                        }
                                        idx++;
                                        dset.clearData();
                                    } // (st.hasMoreTokens())
                                    strVal = strvalSB.toString();
                                }
                            }
                        }
                    }
                    else {
                        strVal = null;
                    }
                }
                else if (isObjRef) {
                    if (val != null && ((String) val).compareTo("NULL") != 0) {
                        strVal = (String) val;
                    }
                    else {
                        strVal = null;
                    }
                }

                if (strVal == null && val != null)
                    strVal = dataDisplayConverter.canonicalToDisplayValue(val).toString();

                cellValueField.setText(strVal);
                ((ScrolledComposite) cellValueField.getParent()).setMinSize(cellValueField.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        }
    }

    /**
     * Custom Column Header data provider to set column indices based on Index Base
     * for Scalar Datasets.
     */
    private class ScalarDSColumnHeaderDataProvider implements IDataProvider
    {
        private final String columnNames[];

        private final int    space_type;
        private final int    rank;

        private final long[] startArray;
        private final long[] strideArray;
        private final int[]  selectedIndex;

        private final int    ncols;

        public ScalarDSColumnHeaderDataProvider(DataFormat theDataObject) {
            space_type = theDataObject.getSpaceType();
            rank = theDataObject.getRank();

            startArray = theDataObject.getStartDims();
            strideArray = theDataObject.getStride();
            selectedIndex = theDataObject.getSelectedIndex();

            if (rank > 1) {
                ncols = (int) theDataObject.getWidth();

                int start = (int) startArray[selectedIndex[1]];
                int stride = (int) strideArray[selectedIndex[1]];

                columnNames = new String[ncols];

                for (int i = 0; i < ncols; i++)
                    columnNames[i] = String.valueOf(start + indexBase + i * stride);
            }
            else {
                ncols = 1;

                columnNames = new String[] { "  " };
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
            // intentional
        }
    }
}
