/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.BitSet;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.StructuralRefreshCommand;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.EditableRule;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.validate.IDataValidator;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.action.MouseEditAction;
import org.eclipse.nebula.widgets.nattable.edit.config.DefaultEditConfiguration;
import org.eclipse.nebula.widgets.nattable.edit.editor.TextCellEditor;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectAllCommand;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.BodyCellEditorMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuAction;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;

import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.ViewProperties;
import hdf.view.ViewProperties.BITMASK_OP;
import hdf.view.ViewProperties.DATA_VIEW_KEY;

/**
 * TableView displays an HDF dataset as a two-dimensional table.
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DefaultTableView implements TableView {

    private final static org.slf4j.Logger log       = org.slf4j.LoggerFactory.getLogger(DefaultTableView.class);

    private final Display display;
    private final Shell shell;

    // The main HDFView
    private final ViewManager viewer;

    private NatTable table; // The NatTable to display data in

    // The Dataset (Scalar or Compound) to be displayed in the Table
    private Dataset dataset;

    /**
     * The value of the dataset.
     */
    private Object                            dataValue;

    private Object                           fillValue               = null;

    private enum ViewType { TABLE, IMAGE, TEXT };
    private    ViewType viewType = ViewType.TABLE;

    // Used for bitmask operations on data
    private BitSet                          bitmask                 = null;
    private BITMASK_OP                      bitmaskOP               = BITMASK_OP.EXTRACT;

    /**
     * Numerical data type. B = byte array, S = short array, I = int array, J = long array, F =
     * float array, and D = double array.
     */
    private char                            NT               = ' ';

    // Changed to use normalized scientific notation (1 <= coefficient < 10).
    // private final DecimalFormat scientificFormat = new DecimalFormat("###.#####E0#");
    private final DecimalFormat             scientificFormat = new DecimalFormat("0.0###E0###");
    private DecimalFormat                   customFormat     = new DecimalFormat("###.#####");
    private final NumberFormat              normalFormat     = null; // NumberFormat.getInstance();
    private NumberFormat                    numberFormat     = normalFormat;


    // Keeps track of which frame of data is being displayed
    private Text frameField;
    private long curFrame                           = 0;
    private long maxFrame                           = 1;

    private static final int                FLOAT_BUFFER_SIZE       = 524288;
    private static final int                INT_BUFFER_SIZE         = 524288;
    private static final int                SHORT_BUFFER_SIZE       = 1048576;
    private static final int                LONG_BUFFER_SIZE        = 262144;
    private static final int                DOUBLE_BUFFER_SIZE      = 262144;
    private static final int                BYTE_BUFFER_SIZE        = 2097152;

    private int                             indexBase = 0;

    private int                             fixedDataLength = -1;

    private int                             binaryOrder;

    private boolean                         isReadOnly = false;

    private boolean                         isValueChanged = false;

    private boolean                         isDisplayTypeChar;

    private boolean                         isDataTransposed;

    private boolean                         isRegRef = false;
    private boolean                         isObjRef = false;

    private boolean                         showAsHex = false, showAsBin = false;

    private final boolean                   startEditing[]   = { false };


    /**
     * Global variables for GUI components
     */

    // Menubar for Table
    private Menu                            menuBar;

    // Popup Menu for region references
    private Menu                            popupMenu = null;

    private MenuItem                        checkFixedDataLength = null;
    private MenuItem                        checkCustomNotation = null;
    private MenuItem                        checkScientificNotation = null;
    private MenuItem                        checkHex = null;
    private MenuItem                        checkBin = null;

    // Labeled Group to display the index base
    private Group                           group;

    // Text field to display the value of the current cell.
    private Text                              cellValueField;

    // Label to indicate the current cell location.
    private Label                             cellLabel;

    // The value of the current cell value in editing.
    private Object                           currentEditingCellValue = null;

    // Keep track of table row selections
    private SelectionLayer                  selectionLayer;

    // Used to get/set column header
    private IDataProvider                   columnHeaderDataProvider;

    /**
     * Constructs a TableView.
     * <p>
     *
     * @param theView
     *             the main HDFView.
     */
    public DefaultTableView(Shell parent, ViewManager theView) {
        this(parent, theView, null);
    }

    /**
     * Constructs a TableView.
     * <p>
     *
     * @param theView
     *             the main HDFView.
     *
     * @param map
     *             the properties on how to show the data. The map is used to allow applications to
     *          pass properties on how to display the data, such as, transposing data, showing
     *          data as character, applying bitmask, and etc. Predefined keys are listed at
     *          ViewProperties.DATA_VIEW_KEY.
     */
    public DefaultTableView(Shell parent, ViewManager theView, HashMap map) {
        log.trace("DefaultTableView start");

        shell = new Shell(parent, SWT.SHELL_TRIM);
        //shell = new Shell(display, SWT.SHELL_TRIM);
        display = shell.getDisplay();
        
        shell.setLayout(new FillLayout());

        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (isValueChanged && !isReadOnly) {
                    MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                    confirm.setText(shell.getText());
                    confirm.setMessage("\"" + dataset.getName() + "\" has changed.\n" + "Do you want to save the changes?");
                    if (confirm.open() == SWT.YES) {
                        updateValueInFile();
                    }
                    else dataset.clearData(); // reload data
                }

                if (dataset instanceof ScalarDS) {
                    ScalarDS sds = (ScalarDS) dataset;
                    // reload the data when it is displayed next time
                    // because the display type (table or image) may be
                    // different.

                    if (sds.isImage()) {
                        sds.clearData();
                    }

                    dataValue = null;
                    table = null;
                }
            }
        });

        viewer = theView;
        HObject hObject = null;

        if (ViewProperties.isIndexBase1()) indexBase = 1;
        log.trace("Index base is {}", indexBase);

        if (map != null) {
            hObject = (HObject) map.get(ViewProperties.DATA_VIEW_KEY.OBJECT);

            bitmask = (BitSet) map.get(ViewProperties.DATA_VIEW_KEY.BITMASK);
            bitmaskOP = (BITMASK_OP) map.get(ViewProperties.DATA_VIEW_KEY.BITMASKOP);

            Boolean b = (Boolean) map.get(ViewProperties.DATA_VIEW_KEY.CHAR);
            if (b != null) isDisplayTypeChar = b.booleanValue();

            b = (Boolean) map.get(ViewProperties.DATA_VIEW_KEY.TRANSPOSED);
            if (b != null) isDataTransposed = b.booleanValue();

            b = (Boolean) map.get(ViewProperties.DATA_VIEW_KEY.INDEXBASE1);
            if (b != null) {
                if (b.booleanValue())
                    indexBase = 1;
                else
                    indexBase = 0;
            }
        }
        log.trace("Index base = {} - Is data transposed = {} - Is display type char = {}", indexBase, isDataTransposed, isDisplayTypeChar);

        if (hObject == null) hObject = viewer.getTreeView().getCurrentObject();

        if ((hObject == null) || !(hObject instanceof Dataset)) {
            return;
        }

        dataset = (Dataset) hObject;
        isReadOnly = dataset.getFileFormat().isReadOnly();
        log.trace("dataset({}) isReadOnly={}", dataset, isReadOnly);

        long[] dims = dataset.getDims();
        long tsize = 1;

        for (int i = 0; i < dims.length; i++)
            tsize *= dims[i];

        log.trace("dataset size={} Height={} Width={}", tsize, dataset.getHeight(), dataset.getWidth());
        if (dataset.getHeight() <= 0 || dataset.getWidth() <= 0 || tsize <= 0) return;

        // cannot edit hdf4 vdata
        if (dataset.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))
                && (dataset instanceof CompoundDS)) {
            isReadOnly = true;
        }

        // disable edit feature for szip compression when encode is not enabled
        if (!isReadOnly) {
            String compression = dataset.getCompression();
            if ((compression != null) && compression.startsWith("SZIP")) {
                if (!compression.endsWith("ENCODE_ENABLED")) {
                    isReadOnly = true;
                }
            }
        }

        Datatype dtype = dataset.getDatatype();
        log.trace("dataset dtype.getDatatypeClass()={}", dtype.getDatatypeClass());
        isDisplayTypeChar = (isDisplayTypeChar && (dtype.getDatatypeSize() == 1 || (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY && dtype
                .getBasetype().getDatatypeClass() == Datatype.CLASS_CHAR)));

        log.trace("dataset isDisplayTypeChar={} isConvertEnum={}", isDisplayTypeChar, ViewProperties.isConvertEnum());
        dataset.setEnumConverted(ViewProperties.isConvertEnum());

        group = new Group(shell, SWT.SHADOW_ETCHED_OUT);
        group.setFont(Display.getCurrent().getSystemFont());
        group.setText(indexBase + "-based");
        group.setLayout(new FillLayout());

        SashForm content = new SashForm(group, SWT.VERTICAL);
        content.setSashWidth(10);

        Composite cellValueComposite = new Composite(content, SWT.BORDER);
        cellValueComposite.setLayout(new FormLayout());

        cellLabel = new Label(cellValueComposite, SWT.NONE);
        cellLabel.setAlignment(SWT.CENTER);
        FormData formData = new FormData();
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(cellValueField, 2);
        formData.top = new FormAttachment(0, 0);
        formData.bottom = new FormAttachment(100, 0);
        cellLabel.setLayoutData(formData);

        cellValueField = new Text(cellValueComposite, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
        //cellValueField.setWrapStyleWord(true);
        cellValueField.setEditable(false);
        cellValueField.setBackground(new Color(display, 255, 255, 240));
        cellValueField.setEnabled(false);
        formData = new FormData();
        formData.top = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        formData.bottom = new FormAttachment(100, 0);
        formData.left = new FormAttachment(10, 0);
        cellValueField.setLayoutData(formData);

        Composite tableComposite = new Composite(content, SWT.BORDER);
        tableComposite.setLayout(new FillLayout());

        // Create the NatTable
        if (dataset instanceof CompoundDS) {
            log.trace("createTable((CompoundDS) dataset): dtype.getDatatypeClass()={}", dtype.getDatatypeClass());

            isDataTransposed = false; // Disable transpose for compound dataset
            shell.setImage(ViewProperties.getTableIcon());
            table = createTable(tableComposite, (CompoundDS) dataset);
        }
        else { /* if (dataset instanceof ScalarDS) */
            log.trace("createTable((ScalarDS) dataset): dtype.getDatatypeClass()={}", dtype.getDatatypeClass());

            shell.setImage(ViewProperties.getDatasetIcon());
            table = createTable(tableComposite, (ScalarDS) dataset);

            if (dtype.getDatatypeClass() == Datatype.CLASS_REFERENCE) {
                if (dtype.getDatatypeSize() > 8) {
                    isReadOnly = true;
                    isRegRef = true;
                }
                else
                    isObjRef = true;
            }
            else if ((dtype.getDatatypeClass() == Datatype.CLASS_BITFIELD) || (dtype.getDatatypeClass() == Datatype.CLASS_OPAQUE)) {
                showAsHex = true;
                checkHex.setSelection(true);
                checkScientificNotation.setSelection(false);
                checkCustomNotation.setSelection(false);
                checkBin.setSelection(false);
                showAsBin = false;
                numberFormat = normalFormat;
            }

            // Create popup menu for reg. ref.
            if (isRegRef || isObjRef) {
                popupMenu = createPopupMenu();
                table.addConfiguration(new RefContextMenu(table));
            }

            log.trace("createTable((ScalarDS) dataset): isRegRef={} isObjRef={} showAsHex={}", isRegRef, isObjRef, showAsHex);
        }
        content.setWeights(new int[] {1, 18});

        if (table == null) {
            viewer.showStatus("Creating table failed - " + dataset.getName());
            dataset = null;
            shell.dispose();
            return;
        }

        StringBuffer sb = new StringBuffer(hObject.getName());
        sb.append("  at  ");
        sb.append(hObject.getPath());
        sb.append("  [");
        sb.append(dataset.getFileFormat().getName());
        sb.append("  in  ");
        sb.append(dataset.getFileFormat().getParent());
        sb.append("]");
        shell.setText(sb.toString());

        // Setup subset information
        log.trace("DefaultTableView: Setup subset information");
        int rank = dataset.getRank();
        int[] selectedIndex = dataset.getSelectedIndex();
        long[] count = dataset.getSelectedDims();
        long[] stride = dataset.getStride();
        long[] start = dataset.getStartDims();
        int n = Math.min(3, rank);
        if (rank > 2) {
            curFrame = start[selectedIndex[2]] + indexBase;
            maxFrame = dims[selectedIndex[2]];
        }

        sb.append(" [ dims");
        sb.append(selectedIndex[0]);
        for (int i = 1; i < n; i++) {
            sb.append("x");
            sb.append(selectedIndex[i]);
        }
        sb.append(", start");
        sb.append(start[selectedIndex[0]]);
        for (int i = 1; i < n; i++) {
            sb.append("x");
            sb.append(start[selectedIndex[i]]);
        }
        sb.append(", count");
        sb.append(count[selectedIndex[0]]);
        for (int i = 1; i < n; i++) {
            sb.append("x");
            sb.append(count[selectedIndex[i]]);
        }
        sb.append(", stride");
        sb.append(stride[selectedIndex[0]]);
        for (int i = 1; i < n; i++) {
            sb.append("x");
            sb.append(stride[selectedIndex[i]]);
        }
        sb.append(" ] ");
        log.trace("DefaultTableView: subset={}", sb.toString());

        shell.setMenuBar(menuBar = createMenuBar());
        viewer.showStatus(sb.toString());

        log.trace("DefaultTableView: finish");

        group.pack();

        shell.pack();

        Composite dataClientArea = ((HDFView) viewer).getDataArea();
        shell.setSize(dataClientArea.getClientArea().width, dataClientArea.getClientArea().height);
        shell.setLocation(dataClientArea.getBounds().x, dataClientArea.getBounds().y);

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    /**
     * Creates a NatTable for a Scalar dataset.
     * @param dataset
     *          The Scalar dataset for the NatTable to display
     * @return The newly created NatTable
     */
    private NatTable createTable(Composite parent, ScalarDS dataset) {
        int rows = 0;
        int cols = 0;

        log.trace("createTable(ScalarDS): start");

        int rank = dataset.getRank();
        if (rank <= 0) {
            try {
                dataset.init();
                log.trace("createTable: dataset inited");
            }
            catch (Exception ex) {
                showError(ex.getMessage(), "createTable:" + shell.getText());
                dataValue = null;
                return null;
            }

            rank = dataset.getRank();
        }
        long[] dims = dataset.getSelectedDims();

        if (rank > 1) {
            rows = (int)dataset.getHeight();
            cols = (int)dataset.getWidth();
        } else {
            rows = (int)dims[0];
            cols = 1;
        }

        log.trace("createTable: rows={} : cols={}", rows, cols);

        dataValue = null;
        try {
            dataValue = dataset.getData();
            if (dataValue == null) {
                showError("No data read", "ScalarDS createTable:" + shell.getText());
                return null;
            }

            log.trace("createTable: dataValue={}", dataValue);

            if (Tools.applyBitmask(dataValue, bitmask, bitmaskOP)) {
                isReadOnly = true;
                String opName = "Bits ";

                if (bitmaskOP == ViewProperties.BITMASK_OP.AND) opName = "Bitwise AND ";

                String title = group.getText();
                title += ", " + opName + bitmask;
                group.setText(title);
            }

            dataset.convertFromUnsignedC();
            dataValue = dataset.getData();

            if (Array.getLength(dataValue) <= rows) cols = 1;
        }
        catch (Throwable ex) {
            showError(ex.getMessage(), "ScalarDS createTable:" + shell.getText());
            dataValue = null;
        }

        if (dataValue == null) {
            return null;
        }

        fillValue = dataset.getFillValue();
        log.trace("createTable: fillValue={}", fillValue);

        String cName = dataValue.getClass().getName();
        int cIndex = cName.lastIndexOf("[");
        if (cIndex >= 0) {
            NT = cName.charAt(cIndex + 1);
        }
        log.trace("createTable: cName={} NT={}", cName, NT);

        // convert numerical data into char
        // only possible cases are byte[] and short[] (converted from unsigned
        // byte)
        if (isDisplayTypeChar && ((NT == 'B') || (NT == 'S'))) {
            int n = Array.getLength(dataValue);
            char[] charData = new char[n];
            for (int i = 0; i < n; i++) {
                if (NT == 'B') {
                    charData[i] = (char) Array.getByte(dataValue, i);
                }
                else if (NT == 'S') {
                    charData[i] = (char) Array.getShort(dataValue, i);
                }
            }

            dataValue = charData;
        }
        else if ((NT == 'B') && dataset.getDatatype().getDatatypeClass() == Datatype.CLASS_ARRAY) {
            Datatype baseType = dataset.getDatatype().getBasetype();
            if (baseType.getDatatypeClass() == Datatype.CLASS_STRING) {
                dataValue = Dataset.byteToString((byte[]) dataValue, (int)baseType.getDatatypeSize());
            }
        }

        final String columnNames[] = new String[cols];
        final long[] startArray = dataset.getStartDims();
        final long[] strideArray = dataset.getStride();
        int[] selectedIndex = dataset.getSelectedIndex();
        int start = 0;
        int stride = 1;

        if (rank > 1) {
            start = (int) startArray[selectedIndex[1]];
            stride = (int) strideArray[selectedIndex[1]];

            for (int i = 0; i < cols; i++) {
                columnNames[i] = String.valueOf(start + indexBase + i * stride);
            }
        }
        else {
            columnNames[0] = "  ";
        }

        // Create body layer
        final IDataProvider bodyDataProvider = new ScalarDSDataProvider();
        final DataLayer dataLayer = new DataLayer(bodyDataProvider);
        selectionLayer = new SelectionLayer(dataLayer);
        ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);
        dataLayer.setDefaultColumnWidth(80);

        // Create the Column Header layer
        columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columnNames);
        ILayer columnHeaderLayer = new ColumnHeaderLayer(new DataLayer(
                columnHeaderDataProvider), viewportLayer, selectionLayer);

        // Create the Row Header layer
        IDataProvider rowHeaderDataProvider = new RowHeader(bodyDataProvider);
        ILayer rowHeaderLayer = new RowHeaderLayer(new DataLayer(
                rowHeaderDataProvider, 40, 20), viewportLayer, selectionLayer);

        // Create the Corner layer
        ILayer cornerLayer = new CornerLayer(new DataLayer(
                new DefaultCornerDataProvider(columnHeaderDataProvider,
                        rowHeaderDataProvider)), rowHeaderLayer,
                columnHeaderLayer);

        // Create the Grid layer
        GridLayer gridLayer = new GridLayer(viewportLayer, columnHeaderLayer,
                rowHeaderLayer, cornerLayer, false);
        gridLayer.addConfiguration(new DefaultEditConfiguration());

        // Change cell editing to be on double click rather than single click
        gridLayer.addConfiguration(new AbstractUiBindingConfiguration() {
            @Override
            public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
                uiBindingRegistry.registerFirstDoubleClickBinding(
                    new BodyCellEditorMouseEventMatcher(TextCellEditor.class), new MouseEditAction());
                //uiBindingRegistry.registerFirstMouseDragMode(mouseEventMatcher, new CellEditDragMode());
            }
        });

        final NatTable natTable = new NatTable(parent, gridLayer, false);
        natTable.addConfiguration(new DefaultNatTableStyleConfiguration());

        // Register cell editing rules with table
        natTable.addConfiguration(new AbstractRegistryConfiguration() {
            @Override
            public void configureRegistry(IConfigRegistry configRegistry) {
                configRegistry.registerConfigAttribute(
                        EditConfigAttributes.CELL_EDITABLE_RULE,
                        getScalarDSEditRule(bodyDataProvider),
                        DisplayMode.EDIT);
            }

            @Override
            public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
                //uiBindingRegistry.registerDoubleClickBinding(mouseEventMatcher, action);
            }
        });

        // Left-align cells
        natTable.addConfiguration(new AbstractRegistryConfiguration() {
            @Override
            public void configureRegistry(IConfigRegistry configRegistry) {
                Style cellStyle = new Style();

                cellStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
                cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR,
                        Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

                configRegistry.registerConfigAttribute(
                        CellConfigAttributes.CELL_STYLE,
                        cellStyle);
            }
        });

        natTable.configure();

        dataLayer.setDefaultRowHeight(2 * natTable.getFont().getFontData()[0].getHeight());

        log.trace("createTable(ScalarDS): finish");

        return natTable;
    }

    /**
     * Creates a NatTable for a Compound dataset
     * @param dataset
     *          The Compound dataset for the NatTable to display
     * @return The newly created NatTable
     */
    private NatTable createTable(Composite parent, CompoundDS dataset) {
        log.trace("createTable: CompoundDS start");

        if (dataset.getRank() <= 0) dataset.init();

        long[] startArray = dataset.getStartDims();
        long[] strideArray = dataset.getStride();
        int[] selectedIndex = dataset.getSelectedIndex();
        final int rowStart = (int) startArray[selectedIndex[0]];
        final int rowStride = (int) strideArray[selectedIndex[0]];

        // use lazy convert for large number of strings
        if (dataset.getHeight() > 10000) {
            dataset.setConvertByteToString(false);
        }

        dataValue = null;
        try {
            dataValue = dataset.getData();
        }
        catch (Throwable ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), "TableView " + shell.getText());
            dataValue = null;
        }

        if ((dataValue == null) || !(dataValue instanceof List)) {
            return null;
        }

        final int rows = (int)dataset.getHeight();
        int cols = dataset.getSelectedMemberCount();
        String[] columnNames = new String[cols];

        int idx = 0;
        String[] columnNamesAll = dataset.getMemberNames();
        for (int i = 0; i < columnNamesAll.length; i++) {
            if (dataset.isMemberSelected(i)) {
                columnNames[idx] = columnNamesAll[i];
                columnNames[idx] = columnNames[idx].replaceAll(CompoundDS.separator, "->");
                idx++;
            }
        }

        String[] subColumnNames = columnNames;
        int columns = (int)dataset.getWidth();
        if (columns > 1) {
            // multi-dimension compound dataset
            subColumnNames = new String[columns * columnNames.length];
            int halfIdx = columnNames.length / 2;
            for (int i = 0; i < columns; i++) {
                for (int j = 0; j < columnNames.length; j++) {
                    // display column index only once, in the middle of the
                    // compound fields
                    if (j == halfIdx) {
                        subColumnNames[i * columnNames.length + j] = (i + indexBase) + "\n " + columnNames[j];
                    }
                    else {
                        subColumnNames[i * columnNames.length + j] = " \n " + columnNames[j];
                    }
                }
            }
        }

        final String[] allColumnNames = subColumnNames;

        // Create body layer
        final IDataProvider bodyDataProvider = new CompoundDSDataProvider();
        final DataLayer dataLayer = new DataLayer(bodyDataProvider);
        final SelectionLayer selectionLayer = new SelectionLayer(dataLayer);
        ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);
        dataLayer.setDefaultColumnWidth(80);

        // Create the Column Header layer
        IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columnNames);
        ILayer columnHeaderLayer = new ColumnHeaderLayer(new DataLayer(
                columnHeaderDataProvider), viewportLayer, selectionLayer);

        // Create the Row Header layer
        IDataProvider rowHeaderDataProvider = new RowHeader(bodyDataProvider);
        ILayer rowHeaderLayer = new RowHeaderLayer(new DataLayer(
                rowHeaderDataProvider, 40, 20), viewportLayer, selectionLayer);

        // Create the Corner layer
        ILayer cornerLayer = new CornerLayer(new DataLayer(
                new DefaultCornerDataProvider(columnHeaderDataProvider,
                        rowHeaderDataProvider)), rowHeaderLayer,
                columnHeaderLayer);

        // Create the Grid layer
        GridLayer gridLayer = new GridLayer(viewportLayer, columnHeaderLayer,
                rowHeaderLayer, cornerLayer);
        gridLayer.addConfiguration(new DefaultEditConfiguration());

        // Change cell editing to be on double click rather than single click
        gridLayer.addConfiguration(new AbstractUiBindingConfiguration() {
            @Override
            public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
                uiBindingRegistry.registerFirstDoubleClickBinding(
                    new BodyCellEditorMouseEventMatcher(TextCellEditor.class), new MouseEditAction());
                //uiBindingRegistry.registerFirstMouseDragMode(mouseEventMatcher, new CellEditDragMode());
            }
        });

        final NatTable natTable = new NatTable(parent, gridLayer);
        natTable.addConfiguration(new DefaultNatTableStyleConfiguration());

        // Register cell editing rules with table
        natTable.addConfiguration(new AbstractRegistryConfiguration() {
            @Override
            public void configureRegistry(IConfigRegistry configRegistry) {
                configRegistry.registerConfigAttribute(
                        EditConfigAttributes.CELL_EDITABLE_RULE,
                        getCompoundDSEditRule(bodyDataProvider),
                        DisplayMode.EDIT);
            }

            @Override
            public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
                //uiBindingRegistry.registerDoubleClickBinding(mouseEventMatcher, action);
            }
        });


        // Left-align cells
        natTable.addConfiguration(new AbstractRegistryConfiguration() {
            @Override
            public void configureRegistry(IConfigRegistry configRegistry) {
                Style cellStyle = new Style();

                cellStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
                cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR,
                        Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

                configRegistry.registerConfigAttribute(
                        CellConfigAttributes.CELL_STYLE,
                        cellStyle);
            }
        });

        natTable.configure();

        dataLayer.setDefaultRowHeight(2 * natTable.getFont().getFontData()[0].getHeight());

        log.trace("createTable(CompoundDS): finish");

        return natTable;
    }

    /**
     * Creates the menubar for the NatTable.
     */
    private Menu createMenuBar() {
        Menu menuBar = new Menu(shell, SWT.BAR);
        boolean isEditable = !isReadOnly;
        boolean is3D = (dataset.getRank() > 2);

        MenuItem tableMenu = new MenuItem(menuBar, SWT.CASCADE);
        tableMenu.setText("&Table");

        Menu menu = new Menu(shell, SWT.DROP_DOWN);
        tableMenu.setMenu(menu);

        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("Export Data to Text File");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    saveAsText();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });

        if(dataset instanceof ScalarDS) {
            MenuItem exportAsBinaryMenuItem = new MenuItem(menu, SWT.CASCADE);
            exportAsBinaryMenuItem.setText("Export Data to Binary File");

            Menu exportAsBinaryMenu = new Menu(menu);

            item = new MenuItem(exportAsBinaryMenu, SWT.PUSH);
            item.setText("Native Order");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    binaryOrder = 1;

                    try {
                        saveAsBinary();
                    }
                    catch (Exception ex) {
                        shell.getDisplay().beep();
                        showError(ex.getMessage(), shell.getText());
                    }
                }
            });

            item = new MenuItem(exportAsBinaryMenu, SWT.PUSH);
            item.setText("Little Endian");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    binaryOrder = 2;

                    try {
                        saveAsBinary();
                    }
                    catch (Exception ex) {
                        shell.getDisplay().beep();
                        showError(ex.getMessage(), shell.getText());
                    }
                }
            });

            item = new MenuItem(exportAsBinaryMenu, SWT.PUSH);
            item.setText("Big Endian");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    binaryOrder = 3;

                    try {
                        saveAsBinary();
                    }
                    catch (Exception ex) {
                        shell.getDisplay().beep();
                        showError(ex.getMessage(), shell.getText());
                    }
                }
            });

            exportAsBinaryMenuItem.setMenu(exportAsBinaryMenu);
        }

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Import Data from Text File");
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String currentDir = dataset.getFileFormat().getParent();

                FileDialog fchooser = new FileDialog(shell, SWT.OPEN);
                fchooser.setFilterPath(currentDir);
                //fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
                fchooser.setFilterExtensions(new String[] {"*.txt", "*.*"});
                fchooser.setFilterNames(new String[] {"Text Documents (*.txt)", "All Files (*.*)"});
                fchooser.setFilterIndex(0);

                if (fchooser.open() == null) return;

                File chosenFile = new File(fchooser.getFilterPath() + File.separator + fchooser.getFileName());
                if (!chosenFile.exists()) {
                    showError("File " + chosenFile.getName() + " does not exist.", "Import Data from Text File");
                    return;
                }

                MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                confirm.setText(shell.getText());
                confirm.setMessage("Do you want to paste selected data?");
                if (confirm.open() == SWT.NO) return;

                importTextData(chosenFile.getAbsolutePath());
            }
        });

        if ((dataset instanceof ScalarDS)) {
            checkFixedDataLength = new MenuItem(menu, SWT.CHECK);
            checkFixedDataLength.setText("Fixed Data Length");
            checkFixedDataLength.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (!checkFixedDataLength.getSelection()) {
                        fixedDataLength = -1;
                        //this.updateUI();
                        return;
                    }

                    String str = new InputDialog(shell, "",
                            "Enter fixed data length when importing text data\n\n"
                            + "For example, for a text string of \"12345678\"\n\t\tenter 2,"
                            + "the data will be 12, 34, 56, 78\n\t\tenter 4, the data will be"
                            + "1234, 5678\n").open();

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
                        return;
                    }
                }
            });

            MenuItem importAsBinaryMenuItem = new MenuItem(menu, SWT.CASCADE);
            importAsBinaryMenuItem.setText("Import Data from Binary File");

            Menu importFromBinaryMenu = new Menu(menu);

            item = new MenuItem(importFromBinaryMenu, SWT.PUSH);
            item.setText("Native Order");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    binaryOrder = 1;

                    try {
                        importBinaryData();
                    }
                    catch (Exception ex) {
                        showError(ex.getMessage(), shell.getText());
                    }
                }
            });

            item = new MenuItem(importFromBinaryMenu, SWT.PUSH);
            item.setText("Little Endian");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    binaryOrder = 2;

                    try {
                        importBinaryData();
                    }
                    catch (Exception ex) {
                        showError(ex.getMessage(), shell.getText());
                    }
                }
            });

            item = new MenuItem(importFromBinaryMenu, SWT.PUSH);
            item.setText("Big Endian");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    binaryOrder = 3;

                    try {
                        importBinaryData();
                    }
                    catch (Exception ex) {
                        showError(ex.getMessage(), shell.getText());
                    }
                }
            });

            importAsBinaryMenuItem.setMenu(importFromBinaryMenu);
        }

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Copy");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                copyData();
            }
        });
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Paste");
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                pasteData();
            }
        });
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Copy to New Dataset");
        item.setEnabled(isEditable && (dataset instanceof ScalarDS));
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if ((selectionLayer.getSelectedColumnPositions().length <= 0) || (selectionLayer.getSelectedRowCount() <= 0)) {
                    MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION);
                    info.setText(shell.getText());
                    info.setMessage("Select table cells to write.");
                    info.open();
                    return;
                }

                /*
                TreeView treeView = viewer.getTreeView();
                TreeNode node = viewer.getTreeView().findTreeNode(dataset);
                Group pGroup = (Group) ((DefaultMutableTreeNode) node.getParent()).getUserObject();
                TreeNode root = dataset.getFileFormat().getRootNode();

                if (root == null) {
                    return;
                }

                Vector<Object> list = new Vector<Object>(dataset.getFileFormat().getNumberOfMembers() + 5);
                DefaultMutableTreeNode theNode = null;
                Enumeration<?> local_enum = ((DefaultMutableTreeNode) root).depthFirstEnumeration();
                while (local_enum.hasMoreElements()) {
                    theNode = (DefaultMutableTreeNode) local_enum.nextElement();
                    list.add(theNode.getUserObject());
                }

                NewDatasetDialog dialog = new NewDatasetDialog((JFrame) viewer, pGroup, list, this);
                dialog.setVisible(true);

                HObject obj = (HObject) dialog.getObject();
                if (obj != null) {
                    Group pgroup = dialog.getParentGroup();
                    try {
                        treeView.addObject(obj, pgroup);
                    }
                    catch (Exception ex) {
                        log.debug("Write selection to dataset:", ex);
                    }
                }

                list.setSize(0);
                 */
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Save Changes to File");
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    updateValueInFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Select All");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    selectAll();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Lineplot");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLineplot();
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Statistics");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    Object theData = getSelectedData();

                    if (dataset instanceof CompoundDS) {
                        int cols = selectionLayer.getFullySelectedColumnPositions().length;
                        if (cols != 1) {
                            showError("Please select one column at a time for compound dataset.", shell.getText());
                            return;
                        }
                    }
                    else if (theData == null) {
                        theData = dataValue;
                    }

                    double[] minmax = new double[2];
                    double[] stat = new double[2];

                    Tools.findMinMax(theData, minmax, fillValue);
                    if (Tools.computeStatistics(theData, stat, fillValue) > 0) {
                        String stats = "Min                      = " + minmax[0] + "\nMax                      = " + minmax[1]
                                     + "\nMean                     = " + stat[0] + "\nStandard deviation = " + stat[1];
                        MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                        info.setText("Statistics");
                        info.setMessage(stats);
                        info.open();
                    }

                    theData = null;
                    System.gc();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Math Conversion");
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    mathConversion();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        if(dataset instanceof ScalarDS) {
            checkScientificNotation = new MenuItem(menu, SWT.CHECK);
            checkScientificNotation.setText("Show Scientific Notation");
            checkScientificNotation.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (checkScientificNotation.getSelection()) {
                        if(checkCustomNotation != null)
                            checkCustomNotation.setSelection(false);
                        if(checkHex != null) checkHex.setSelection(false);
                        if(checkBin != null) checkBin.setSelection(false);

                        numberFormat = scientificFormat;
                        showAsHex = false;
                        showAsBin = false;
                    } else {
                        numberFormat = normalFormat;
                    }

                    table.doCommand(new VisualRefreshCommand());
                }
            });

            checkCustomNotation = new MenuItem(menu, SWT.CHECK);
            checkCustomNotation.setText("Show Custom Notation");
            checkCustomNotation.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (checkCustomNotation.getSelection()) {
                        if(checkScientificNotation != null)
                            checkScientificNotation.setSelection(false);
                        if(checkHex != null) checkHex.setSelection(false);
                        if(checkBin != null) checkBin.setSelection(false);

                        numberFormat = customFormat;
                        showAsHex = false;
                        showAsBin = false;
                    } else {
                        numberFormat = normalFormat;
                    }

                    table.doCommand(new VisualRefreshCommand());
                }
            });
        }

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Create custom notation");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String msg = "Create number format by pattern \nINTEGER . FRACTION E EXPONENT\nusing # for optional digits and 0 for required digits"
                        + "\nwhere, INTEGER: the pattern for the integer part"
                        + "\n       FRACTION: the pattern for the fractional part"
                        + "\n       EXPONENT: the pattern for the exponent part"
                        + "\n\nFor example, "
                        + "\n\t the normalized scientific notation format is \"#.0###E0##\""
                        + "\n\t to make the digits required \"0.00000E000\"\n\n";

                // Add custom HDFLarge icon to dialog
                String str = (new InputDialog(shell, "Create a custom number format", msg)).open();

                if ((str == null) || (str.length() < 1)) {
                    return;
                }

                customFormat.applyPattern(str);
            }
        });

        boolean isInt = (NT == 'B' || NT == 'S' || NT == 'I' || NT == 'J');

        if ((dataset instanceof ScalarDS) && isInt) {
            checkHex = new MenuItem(menu, SWT.CHECK);
            checkHex.setText("Show Hexadecimal");
            checkHex.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    showAsHex = checkHex.getSelection();
                    if (showAsHex) {
                        if(checkScientificNotation != null)
                            checkScientificNotation.setSelection(false);
                        if(checkCustomNotation != null)
                            checkCustomNotation.setSelection(false);
                        if(checkBin != null) checkBin.setSelection(false);

                        showAsBin = false;
                        numberFormat = normalFormat;
                    }

                    table.doCommand(new VisualRefreshCommand());
                }
            });

            checkBin = new MenuItem(menu, SWT.CHECK);
            checkBin.setText("Show Binary");
            checkBin.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    showAsBin = checkBin.getSelection();
                    if (showAsBin) {
                        if(checkScientificNotation != null)
                            checkScientificNotation.setSelection(false);
                        if(checkCustomNotation != null)
                            checkCustomNotation.setSelection(false);
                        if(checkHex != null) checkHex.setSelection(false);

                        showAsHex = false;
                        numberFormat = normalFormat;
                    }

                    table.doCommand(new VisualRefreshCommand());
                }
            });
        }

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Close");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                //viewer.removeDataView(this);

                shell.dispose();
            }
        });

        new MenuItem(menuBar, SWT.SEPARATOR).setText("     ");

        // Add icons to the menubar

        // chart button
        item = new MenuItem(menuBar, SWT.PUSH);
        item.setImage(ViewProperties.getChartIcon());
        //button.setToolTipText("Line Plot");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLineplot();
            }
        });

        if (is3D) {
            new MenuItem(menuBar, SWT.SEPARATOR).setText("     ");

            item = new MenuItem(menuBar, SWT.PUSH);
            item.setImage(ViewProperties.getFirstIcon());
            //button.setToolTipText("First");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    firstPage();
                }
            });

            item = new MenuItem(menuBar, SWT.PUSH);
            item.setImage(ViewProperties.getPreviousIcon());
            //button.setToolTipText("Previous");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    previousPage();
                }
            });

            //frameField = new Text(menuBar, SWT.SINGLE);
            //frameField.setText(String.valueOf(curFrame));
            //frameField.setMaximumSize(new Dimension(50, 30));
            //frameField.setMargin(margin);
            //frameField.addSelectionListener(new SelectionAdapter() {
            //  public void widgetSelected(SelectionEvent e) {
            //      int page = 0;
            //
            //      try {
            //          page = Integer.parseInt(frameField.getText().trim()) - indexBase;
            //      }
            //      catch (Exception ex) {
            //          page = -1;
            //      }
            //
            //      gotoPage(page);
            //  }
            //});

            item = new MenuItem(menuBar, SWT.SEPARATOR_FILL);
            item.setText(String.valueOf(maxFrame));
            item.setEnabled(false);
            //tmpField.setMaximumSize(new Dimension(50, 30));

            item = new MenuItem(menuBar, SWT.PUSH);
            item.setImage(ViewProperties.getNextIcon());
            //button.setToolTipText("Next");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    nextPage();
                }
            });

            item = new MenuItem(menuBar, SWT.PUSH);
            item.setImage(ViewProperties.getLastIcon());
            //button.setToolTipText("Last");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    lastPage();
                }
            });
        }

        return menuBar;
    }

    /** Creates a popup menu for a right mouse click on a data object */
    private Menu createPopupMenu() {
        Menu menu = new Menu(shell, SWT.POP_UP);
        table.setMenu(menu);

        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show As &Table");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                viewType = ViewType.TABLE;

                log.trace("DefaultTableView: Show data as {}: ", viewType);

                Object theData = getSelectedData();
                if (theData == null) {
                    shell.getDisplay().beep();
                    showError("No data selected.", shell.getText());
                    return;
                }

                int[] selectedRows = selectionLayer.getFullySelectedRowPositions();
                int[] selectedCols = selectionLayer.getFullySelectedColumnPositions();
                if (selectedRows == null || selectedRows.length <= 0) {
                    log.trace("DefaultTableView: Show data as {}: selectedRows is empty", viewType);
                    return;
                }

                int len = Array.getLength(selectedRows) * Array.getLength(selectedCols);
                log.trace("DefaultTableView: Show data as {}: len={}", viewType, len);

                for (int i = 0; i < len; i++) {
                    if (isRegRef) {
                        log.trace("DefaultTableView: Show data[{}] as {}: isRegRef={}", i, viewType, isRegRef);
                        showRegRefData((String) Array.get(theData, i));
                    }
                    else if (isObjRef) {
                        log.trace("DefaultTableView: Show data[{}] as {}: isObjRef={}", i, viewType, isObjRef);
                        showObjRefData(Array.getLong(theData, i));
                    }
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show As &Image");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                viewType = ViewType.IMAGE;

                log.trace("DefaultTableView: Show data as {}: ", viewType);

                Object theData = getSelectedData();
                if (theData == null) {
                    shell.getDisplay().beep();
                    showError("No data selected.", shell.getText());
                    return;
                }

                int[] selectedRows = selectionLayer.getFullySelectedRowPositions();
                int[] selectedCols = selectionLayer.getFullySelectedColumnPositions();
                if (selectedRows == null || selectedRows.length <= 0) {
                    log.trace("DefaultTableView: Show data as {}: selectedRows is empty", viewType);
                    return;
                }

                int len = Array.getLength(selectedRows) * Array.getLength(selectedCols);
                log.trace("DefaultTableView: Show data as {}: len={}", viewType, len);

                for (int i = 0; i < len; i++) {
                    if (isRegRef) {
                        log.trace("DefaultTableView: Show data[{}] as {}: isRegRef={}", i, viewType, isRegRef);
                        showRegRefData((String) Array.get(theData, i));
                    }
                    else if (isObjRef) {
                        log.trace("DefaultTableView: Show data[{}] as {}: isObjRef={}", i, viewType, isObjRef);
                        showObjRefData(Array.getLong(theData, i));
                    }
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show As &Text");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                viewType = ViewType.TEXT;

                log.trace("DefaultTableView: Show data as {}: ", viewType);

                Object theData = getSelectedData();
                if (theData == null) {
                    shell.getDisplay().beep();
                    showError("No data selected.", shell.getText());
                    return;
                }

                int[] selectedRows = selectionLayer.getFullySelectedRowPositions();
                int[] selectedCols = selectionLayer.getFullySelectedColumnPositions();
                if (selectedRows == null || selectedRows.length <= 0) {
                    log.trace("DefaultTableView: Show data as {}: selectedRows is empty", viewType);
                    return;
                }

                int len = Array.getLength(selectedRows) * Array.getLength(selectedCols);
                log.trace("DefaultTableView: Show data as {}: len={}", viewType, len);

                for (int i = 0; i < len; i++) {
                    if (isRegRef) {
                        log.trace("DefaultTableView: Show data[{}] as {}: isRegRef={}", i, viewType, isRegRef);
                        showRegRefData((String) Array.get(theData, i));
                    }
                    else if (isObjRef) {
                        log.trace("DefaultTableView: Show data[{}] as {}: isObjRef={}", i, viewType, isObjRef);
                        showObjRefData(Array.getLong(theData, i));
                    }
                }
            }
        });

        return menu;
    }

    // Flip to previous page of Table
    private void previousPage() {
        // Only valid operation if dataset has 3 or more dimensions
        if (dataset.getRank() < 3) return;

        long[] start = dataset.getStartDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        long idx = start[selectedIndex[2]];
        if (idx == 0) {
            return; // current page is the first page
        }

        gotoPage(start[selectedIndex[2]] - 1);
    }

    // Flip to next page of Table
    private void nextPage() {
        // Only valid operation if dataset has 3 or more dimensions
        if (dataset.getRank() < 3) return;

        long[] start = dataset.getStartDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        long[] dims = dataset.getDims();
        long idx = start[selectedIndex[2]];
        if (idx == dims[selectedIndex[2]] - 1) {
            return; // current page is the last page
        }

        gotoPage(start[selectedIndex[2]] + 1);
    }

    // Flip to first page of Table
    private void firstPage() {
        // Only valid operation if dataset has 3 or more dimensions
        if (dataset.getRank() < 3) return;

        long[] start = dataset.getStartDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        long idx = start[selectedIndex[2]];
        if (idx == 0) {
            return; // current page is the first page
        }

        gotoPage(0);
    }

    // Flip to last page of Table
    private void lastPage() {
        // Only valid operation if dataset has 3 or more dimensions
        if (dataset.getRank() < 3) return;

        long[] start = dataset.getStartDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        long[] dims = dataset.getDims();
        long idx = start[selectedIndex[2]];
        if (idx == dims[selectedIndex[2]] - 1) {
            return; // current page is the last page
        }

        gotoPage(dims[selectedIndex[2]] - 1);
    }

    // Flip to specified page of Table
    private void gotoPage (long idx) {
        // Only valid operation if dataset has 3 or more dimensions
        if (dataset.getRank() < 3 || idx == (curFrame - indexBase)) {
            return;
        }

        if (isValueChanged) {
            updateValueInFile();
        }

        long[] start = dataset.getStartDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        long[] dims = dataset.getDims();

        if ((idx < 0) || (idx >= dims[selectedIndex[2]])) {
            shell.getDisplay().beep();
            showError("Frame number must be between " + indexBase + " and " + (dims[selectedIndex[2]] - 1 + indexBase), shell.getText());
            return;
        }

        start[selectedIndex[2]] = idx;
        curFrame = idx + indexBase;
        dataset.clearData();

        shell.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));

        try {
            dataValue = dataset.getData();
            if (dataset instanceof ScalarDS) {
                ((ScalarDS) dataset).convertFromUnsignedC();
                dataValue = dataset.getData();
            }
        }
        catch (Exception ex) {
            shell.setCursor(null);
            dataValue = null;
            showError(ex.getMessage(), shell.getText());
            return;
        }

        shell.setCursor(null);

        //frameField.setText(String.valueOf(curFrame));

        table.doCommand(new VisualRefreshCommand());
    }

    /**
     * Update dataset value in file. The changes will go to the file.
     */
    @Override
    public void updateValueInFile() {
        log.trace("DefaultTableView: updateValueInFile(): begin");

        if (isReadOnly || !isValueChanged || showAsBin || showAsHex) {
            return;
        }

        try {
            log.trace("DefaultTableView: updateValueInFile(): write");
            dataset.write();
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), shell.getText());
            return;
        }

        isValueChanged = false;
        log.trace("DefaultTableView: updateValueInFile(): end");
    }

    /**
     * Update cell value in memory. It does not change the dataset value in file.
     *
     * @param cellValue
     *            the string value of input.
     * @param row
     *            the row of the editing cell.
     * @param col
     *            the column of the editing cell.
     */
    private void updateValueInMemory(String cellValue, int row, int col) throws Exception {
        log.trace("DefaultTableView: updateValueInMemory()");

        if (currentEditingCellValue != null) {
            // Data values are the same, no need to change the data
            if (currentEditingCellValue.toString().equals(cellValue)) return;
        }

        if (dataset instanceof ScalarDS) {
            updateScalarData(cellValue, row, col);
        }
        else if (dataset instanceof CompoundDS) {
            updateCompoundData(cellValue, row, col);
        }
    }

    /**
     * Copy data from the spreadsheet to the system clipboard.
     */
    private void copyData() {
        StringBuffer sb = new StringBuffer();

        int r0 = selectionLayer.getLastSelectedRegion().y; // starting row
        int c0 = selectionLayer.getLastSelectedRegion().x; // starting column

        if ((r0 < 0) || (c0 < 0)) {
            return;
        }

        int nr = selectionLayer.getSelectedRowCount();
        int nc = selectionLayer.getSelectedColumnPositions().length;
        int r1 = r0 + nr; // finish row
        int c1 = c0 + nc; // finishing column

        try {
            for (int i = r0; i < r1; i++) {
                sb.append(selectionLayer.getDataValueByPosition(c0, i).toString());
                for (int j = c0 + 1; j < c1; j++) {
                    sb.append("\t");
                    sb.append(selectionLayer.getDataValueByPosition(j, i).toString());
                }
                sb.append("\n");
            }
        }
        catch (java.lang.OutOfMemoryError err) {
            shell.getDisplay().beep();
            showError("Copying data to system clipboard failed. \nUse \"export/import data\" for copying/pasting large data.", shell.getText());
            return;
        }

        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection contents = new StringSelection(sb.toString());
        cb.setContents(contents, null);
    }

    /**
     * Paste data from the system clipboard to the spreadsheet.
     */
    private void pasteData() {
        MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(shell.getText());
        confirm.setMessage("Do you want to paste selected data?");
        if (confirm.open() == SWT.NO) return;

        int cols = selectionLayer.getPreferredColumnCount();
        int rows = selectionLayer.getPreferredRowCount();
        int r0 = selectionLayer.getLastSelectedRegion().y;
        int c0 = selectionLayer.getLastSelectedRegion().x;

        if (c0 < 0) {
            c0 = 0;
        }
        if (r0 < 0) {
            r0 = 0;
        }
        int r = r0;
        int c = c0;

        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        // Transferable content = cb.getContents(this);
        String line = "";
        try {
            String s = (String) cb.getData(DataFlavor.stringFlavor);

            StringTokenizer st = new StringTokenizer(s, "\n");
            // read line by line
            while (st.hasMoreTokens() && (r < rows)) {
                line = st.nextToken();

                if (fixedDataLength < 1) {
                    // separate by delimiter
                    StringTokenizer lt = new StringTokenizer(line, "\t");
                    while (lt.hasMoreTokens() && (c < cols)) {
                        try {
                            updateValueInMemory(lt.nextToken(), r, c);
                        }
                        catch (Exception ex) {
                            continue;
                        }
                        c++;
                    }
                    r = r + 1;
                    c = c0;
                }
                else {
                    // the data has fixed length
                    int n = line.length();
                    String theVal;
                    for (int i = 0; i < n; i = i + fixedDataLength) {
                        try {
                            theVal = line.substring(i, i + fixedDataLength);
                            updateValueInMemory(theVal, r, c);
                        }
                        catch (Exception ex) {
                            continue;
                        }
                        c++;
                    }
                }
            }
        }
        catch (Throwable ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), shell.getText());
        }
    }

    /**
     * Remove values of NaN, INF from the array.
     *
     * @param data
     *            the data array
     * @param xData
     *            the x-axis data points
     * @param yRange
     *            the range of data values
     * @return number of data points in the plot data if successful; otherwise, returns false.
     */
    private int removeInvalidPlotData (double[][] data, double[] xData, double[] yRange) {
        int idx = 0;
        boolean hasInvalid = false;

        if (data == null || yRange == null) return -1;

        yRange[0] = Double.POSITIVE_INFINITY;
        yRange[1] = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < data[0].length; i++) {
            hasInvalid = false;

            for (int j = 0; j < data.length; j++) {
                hasInvalid = Tools.isNaNINF(data[j][i]);
                if (xData != null) hasInvalid = hasInvalid || Tools.isNaNINF(xData[i]);

                if (hasInvalid)
                    break;
                else {
                    data[j][idx] = data[j][i];
                    if (xData != null) xData[idx] = xData[i];
                    yRange[0] = Math.min(yRange[0], data[j][idx]);
                    yRange[1] = Math.max(yRange[1], data[j][idx]);
                }
            }

            if (!hasInvalid) idx++;
        }

        return idx;
    }

    /**
     * Returns the selected data values.
     */
    @Override
    public Object getSelectedData() {
        if (dataset instanceof CompoundDS) {
            return getSelectedCompoundData();
        }
        else {
            return getSelectedScalarData();
        }
    }

    /**
     * Returns the selected data values of the ScalarDS
     */
    private Object getSelectedScalarData() {
        Object selectedData = null;

        // Since NatTable returns the selected row positions as a Set<Range>, convert this to
        // an Integer[]
        Set<Range> rowPositions = selectionLayer.getSelectedRowPositions();
        Set<Integer> selectedRowPos = new LinkedHashSet<Integer>();
        Iterator<Range> i1 = rowPositions.iterator();
        while(i1.hasNext()) {
            selectedRowPos.addAll(i1.next().getMembers());
        }

        Integer[] selectedRows = selectedRowPos.toArray(new Integer[0]);
        int[] selectedCols = selectionLayer.getSelectedColumnPositions();

        if (selectedRows == null || selectedRows.length <= 0 || selectedCols == null || selectedCols.length <= 0) {
            return null;
        }

        int size = selectedCols.length * selectedRows.length;
        log.trace("DefaultTableView getSelectedScalarData: {}", size);

        // the whole table is selected
        if ((table.getPreferredColumnCount() - 1 == selectedCols.length) && (table.getPreferredRowCount() - 1 == selectedRows.length)) {
            return dataValue;
        }

        selectedData = null;
        if (isRegRef) {
            // reg. ref data are stored in strings
            selectedData = new String[size];
        }
        else {
            switch (NT) {
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
            showError("Unsupported data type.", shell.getText());
            return null;
        }
        log.trace("DefaultTableView getSelectedScalarData: selectedData is type {}", NT);

        int w = table.getPreferredColumnCount() - 1;
        log.trace("DefaultTableView getSelectedScalarData: getColumnCount={}", w);
        int idx_src = 0;
        int idx_dst = 0;
        log.trace("DefaultTableView getSelectedScalarData: Rows.length={} Cols.length={}", selectedRows.length, selectedCols.length);
        for (int i = 0; i < selectedRows.length; i++) {
            for (int j = 0; j < selectedCols.length; j++) {
                idx_src = selectedRows[i] * w + selectedCols[j];
                log.trace("DefaultTableView getSelectedScalarData[{},{}]: dataValue[{}]={} from r{} and c{}", i, j, idx_src, Array.get(dataValue, idx_src), selectedRows[i], selectedCols[j]);
                Array.set(selectedData, idx_dst, Array.get(dataValue, idx_src));
                log.trace("DefaultTableView getSelectedScalarData[{},{}]: selectedData[{}]={}", i, j, idx_dst, Array.get(selectedData, idx_dst));
                idx_dst++;
            }
        }

        // this only works for continuous cells
        // for (int i = 0; i < rows; i++) {
        // idx_src = (r0 + i) * w + c0;
        // System.arraycopy(dataValue, idx_src, selectedData, idx_dst, cols);
        // idx_dst += cols;
        // }

        return selectedData;
    }

    /**
     * Returns the selected data values of the CompoundDS
     */
    private Object getSelectedCompoundData ( ) {
        /*
        Object selectedData = null;

        int cols = table.getSelectedColumnCount();
        int rows = table.getSelectedRowCount();

        if ((cols <= 0) || (rows <= 0)) {
            shell.getDisplay().beep();
            showError("No data is selected.", shell.getText());
            return null;
        }

        Object colData = null;
        try {
            colData = ((List<?>) dataset.getData()).get(table.getSelectedColumn());
        }
        catch (Exception ex) {
            log.debug("colData:", ex);
            return null;
        }

        int size = Array.getLength(colData);
        String cName = colData.getClass().getName();
        int cIndex = cName.lastIndexOf("[");
        char nt = ' ';
        if (cIndex >= 0) {
            nt = cName.charAt(cIndex + 1);
        }
        log.trace("DefaultTableView getSelectedCompoundData: size={} cName={} nt={}", size, cName, nt);

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
            showError("Unsupported data type.", shell.getText());
            return null;
        }
        log.trace("DefaultTableView getSelectedCompoundData: selectedData={}", selectedData);

        System.arraycopy(colData, 0, selectedData, 0, size);

        return selectedData;
        */

        return null; // Remove when fixed
    }

    /**
     * Convert selected data based on predefined math functions.
     */
    private void mathConversion() throws Exception {
        if (isReadOnly) {
            return;
        }

        int cols = selectionLayer.getSelectedColumnPositions().length;
        if ((dataset instanceof CompoundDS) && (cols > 1)) {
            shell.getDisplay().beep();
            showError("Please select one column at a time for math conversion"
                    + "for compound dataset.", shell.getText());
            return;
        }

        Object theData = getSelectedData();
        if (theData == null) {
            shell.getDisplay().beep();
            showError("No data is selected.", shell.getText());
            return;
        }

        MathConversionDialog dialog = new MathConversionDialog(shell, theData);
        dialog.open();

        if (dialog.isConverted()) {
            if (dataset instanceof CompoundDS) {
                Object colData = null;
                try {
                    colData = ((List<?>) dataset.getData()).get(selectionLayer.getSelectedColumnPositions()[0]);
                }
                catch (Exception ex) {
                    log.debug("colData:", ex);
                }

                if (colData != null) {
                    int size = Array.getLength(theData);
                    System.arraycopy(theData, 0, colData, 0, size);
                }
            }
            else {
                int rows = selectionLayer.getSelectedRowCount();

                // Since NatTable returns the selected row positions as a Set<Range>, convert this to
                // an Integer[]
                Set<Range> rowPositions = selectionLayer.getSelectedRowPositions();
                Set<Integer> selectedRowPos = new LinkedHashSet<Integer>();
                Iterator<Range> i1 = rowPositions.iterator();
                while(i1.hasNext()) {
                    selectedRowPos.addAll(i1.next().getMembers());
                }

                int r0 = selectedRowPos.toArray(new Integer[0])[0];
                int c0 = selectionLayer.getSelectedColumnPositions()[0];

                int w = table.getPreferredColumnCount() - 1;
                int idx_src = 0;
                int idx_dst = 0;

                for (int i = 0; i < rows; i++) {
                    idx_dst = (r0 + i) * w + c0;
                    System.arraycopy(theData, idx_src, dataValue, idx_dst, cols);
                    idx_src += cols;
                }
            }

            theData = null;
            System.gc();
            isValueChanged = true;
        }
    }

    // Implementing DataView
    @Override
    public HObject getDataObject() {
        return dataset;
    }

    // Implementing TableView
    @Override
    public NatTable getTable() {
        return table;
    }

    /**
     * Selects all rows, columns, and cells in the table.
     */
    private void selectAll() {
        table.doCommand(new SelectAllCommand());
    }

    // Show an error dialog with the given error message
    private void showError(String errorMsg, String title) {
        MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
        error.setText(title);
        error.setMessage(errorMsg);
        error.open();
    }

    /**
     * Display data pointed by object references. Data of each object is shown in a separate
     * spreadsheet.
     *
     * @param ref
     *            the array of strings that contain the object reference information.
     *
     */
    private void showObjRefData (long ref) {
        long[] oid = { ref };
        log.trace("DefaultTableView showObjRefData: ref={}", ref);

        HObject obj = FileFormat.findObject(dataset.getFileFormat(), oid);
        if (obj == null || !(obj instanceof ScalarDS)) return;

        ScalarDS dset = (ScalarDS) obj;
        ScalarDS dset_copy = null;

        // create an instance of the dataset constructor
        Constructor<? extends ScalarDS> constructor = null;
        Object[] paramObj = null;
        Object data = null;

        try {
            Class[] paramClass = { FileFormat.class, String.class, String.class };
            constructor = dset.getClass().getConstructor(paramClass);
            paramObj = new Object[] { dset.getFileFormat(), dset.getName(), dset.getPath() };
            dset_copy = (ScalarDS) constructor.newInstance(paramObj);
            data = dset_copy.getData();
        }
        catch (Exception ex) {
            showError(ex.getMessage(), "Object Reference:" + shell.getText());
            data = null;
        }

        if (data == null) return;

        DataView dataView = null;
        HashMap map = new HashMap(1);
        map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, dset_copy);
        switch (viewType) {
            case TEXT:
                dataView = new DefaultTextView(shell, viewer, map);
                break;
            case IMAGE:
                dataView = new DefaultImageView(shell, viewer, map);
                break;
            default:
                dataView = new DefaultTableView(shell, viewer, map);
                break;
        }

        if (dataView != null) {
            viewer.addDataView((DataView) dataView);
        }
    }

    /**
     * Display data pointed by region references. Data of each region is shown in a separate
     * spreadsheet. The reg. ref. information is stored in strings of the format below:
     * <p />
     * <ul>
     * <li>For point selections: "file_id:obj_id { <point1> <point2> ...) }", where <point1> is in
     * the form of (location_of_dim0, location_of_dim1, ...). For example, 0:800 { (0,1) (2,11)
     * (1,0) (2,4) }</li>
     * <li>For rectangle selections:
     * "file_id:obj_id { <corner coordinates1> <corner coordinates2> ... }", where <corner
     * coordinates1> is in the form of (start_corner)-(oposite_corner). For example, 0:800 {
     * (0,0)-(0,2) (0,11)-(0,13) (2,0)-(2,2) (2,11)-(2,13) }</li>
     * </ul>
     *
     * @param reg
     *            the array of strings that contain the reg. ref information.
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void showRegRefData (String reg) {
        boolean isPointSelection = false;

        if (reg == null || (reg.length() <= 0) || (reg.compareTo("NULL") == 0)) return;
        log.trace("DefaultTableView showRegRefData: reg={}", reg);

        isPointSelection = (reg.indexOf('-') <= 0);

        // find the object location
        String oidStr = reg.substring(reg.indexOf('/'), reg.indexOf(' '));
        log.trace("DefaultTableView showRegRefData: isPointSelection={} oidStr={}", isPointSelection, oidStr);

        // decode the region selection
        String regStr = reg.substring(reg.indexOf('{') + 1, reg.indexOf('}'));
        if (regStr == null || regStr.length() <= 0) return; // no selection

        reg.substring(reg.indexOf('}') + 1);

        StringTokenizer st = new StringTokenizer(regStr);
        int nSelections = st.countTokens();
        if (nSelections <= 0) return; // no selection
        log.trace("DefaultTableView showRegRefData: nSelections={}", nSelections);

        HObject obj = FileFormat.findObject(dataset.getFileFormat(), oidStr);
        if (obj == null || !(obj instanceof ScalarDS)) return;

        ScalarDS dset = (ScalarDS) obj;
        ScalarDS dset_copy = null;

        // create an instance of the dataset constructor
        Constructor<? extends ScalarDS> constructor = null;
        Object[] paramObj = null;
        try {
            Class[] paramClass = { FileFormat.class, String.class, String.class };
            constructor = dset.getClass().getConstructor(paramClass);
            paramObj = new Object[] { dset.getFileFormat(), dset.getName(), dset.getPath() };
        }
        catch (Exception ex) {
            constructor = null;
        }

        // load each selection into a separate dataset and display it in
        // a separate spreadsheet
        StringBuffer titleSB = new StringBuffer();
        log.trace("DefaultTableView showRegRefData: titleSB created");

        while (st.hasMoreTokens()) {
            log.trace("DefaultTableView showRegRefData: st.hasMoreTokens() begin");
            try {
                dset_copy = (ScalarDS) constructor.newInstance(paramObj);
            }
            catch (Exception ex) {
                continue;
            }

            if (dset_copy == null) continue;

            try {
                dset_copy.init();
            }
            catch (Exception ex) {
                continue;
            }

            dset_copy.getRank();
            long start[] = dset_copy.getStartDims();
            long count[] = dset_copy.getSelectedDims();

            // set the selected dimension sizes based on the region selection
            // info.
            int idx = 0;
            String sizeStr = null;
            String token = st.nextToken();

            titleSB.setLength(0);
            titleSB.append(token);
            titleSB.append(" at ");
            log.trace("DefaultTableView showRegRefData: titleSB={}", titleSB);

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
            log.trace("DefaultTableView showRegRefData: selection inited");

            try {
                dset_copy.getData();
            }
            catch (Exception ex) {
                showError(ex.getMessage(), "Region Reference:" + shell.getText());
            }

            DataView dataView = null;
            HashMap map = new HashMap(1);
            map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, dset_copy);
            switch (viewType) {
                case TEXT:
                    dataView = new DefaultTextView(shell, viewer, map);
                    break;
                case IMAGE:
                    dataView = new DefaultImageView(shell, viewer, map);
                    break;
                default:
                    dataView = new DefaultTableView(shell, viewer, map);
                    break;
            }

            if (dataView != null) {
                viewer.addDataView((DataView) dataView);
                //dataView.setText(dataView.getText() + "; " + titleSB.toString());
            }

            log.trace("DefaultTableView showRegRefData: st.hasMoreTokens() end");
        } // while (st.hasMoreTokens())
    } // private void showRegRefData(String reg)

    /**
     * Update cell value in memory. It does not change the dataset value in file.
     *
     * @param cellValue
     *            the string value of input.
     * @param row
     *            the row of the editing cell.
     * @param col
     *            the column of the editing cell.
     */
    private void updateScalarData (String cellValue, int row, int col) throws Exception {
        if (!(dataset instanceof ScalarDS) || (cellValue == null) || ((cellValue = cellValue.trim()) == null)
                || showAsBin || showAsHex) {
            return;
        }

        int i = 0;
        if (isDataTransposed) {
            i = col * (table.getPreferredRowCount() - 1) + row;
        }
        else {
            i = row * (table.getPreferredColumnCount() - 1) + col;
        }

        log.trace("DefaultTableView: updateScalarData {} NT={}", cellValue, NT);

        ScalarDS sds = (ScalarDS) dataset;
        boolean isUnsigned = sds.isUnsigned();
        String cname = dataset.getOriginalClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        log.trace("updateScalarData: isUnsigned={} cname={} dname={}", isUnsigned, cname, dname);

        // check data range for unsigned datatype converted sizes!
        if (isUnsigned) {
            long lvalue = -1;
            long maxValue = Long.MAX_VALUE;
            if (dname == 'B') {
                maxValue = 255;
                lvalue = Long.parseLong(cellValue);

                if (lvalue < 0) {
                    throw new NumberFormatException("Negative value for unsigned integer: " + lvalue);
                }

                if (lvalue > maxValue) {
                    throw new NumberFormatException("Data value is out of range: " + lvalue);
                }
            }
            else if (dname == 'S') {
                maxValue = 65535;
                lvalue = Long.parseLong(cellValue);

                if (lvalue < 0) {
                    throw new NumberFormatException("Negative value for unsigned integer: " + lvalue);
                }

                if (lvalue > maxValue) {
                    throw new NumberFormatException("Data value is out of range: " + lvalue);
                }
            }
            else if (dname == 'I') {
                maxValue = 4294967295L;
                lvalue = Long.parseLong(cellValue);

                if (lvalue < 0) {
                    throw new NumberFormatException("Negative value for unsigned integer: " + lvalue);
                }

                if (lvalue > maxValue) {
                    throw new NumberFormatException("Data value is out of range: " + lvalue);
                }
            }
            else if (dname == 'J') {
                BigInteger Jmax = new BigInteger("18446744073709551615");
                BigInteger big = new BigInteger(cellValue);
                if (big.compareTo(Jmax) > 0) {
                    throw new NumberFormatException("Negative value for unsigned integer: " + cellValue);
                }
                if (big.compareTo(BigInteger.ZERO) < 0) {
                    throw new NumberFormatException("Data value is out of range: " + cellValue);
                }
            }
        }

        switch (NT) {
            case 'B':
                byte bvalue = 0;
                bvalue = Byte.parseByte(cellValue);
                Array.setByte(dataValue, i, bvalue);
                break;
            case 'S':
                short svalue = 0;
                svalue = Short.parseShort(cellValue);
                Array.setShort(dataValue, i, svalue);
                break;
            case 'I':
                int ivalue = 0;
                ivalue = Integer.parseInt(cellValue);
                Array.setInt(dataValue, i, ivalue);
                break;
            case 'J':
                long lvalue = 0;
                if (dname == 'J') {
                    BigInteger big = new BigInteger(cellValue);
                    lvalue = big.longValue();
                }
                else
                    lvalue = Long.parseLong(cellValue);
                Array.setLong(dataValue, i, lvalue);
                break;
            case 'F':
                float fvalue = 0;
                fvalue = Float.parseFloat(cellValue);
                Array.setFloat(dataValue, i, fvalue);
                break;
            case 'D':
                double dvalue = 0;
                dvalue = Double.parseDouble(cellValue);
                Array.setDouble(dataValue, i, dvalue);
                break;
            default:
                Array.set(dataValue, i, cellValue);
                break;
        }

        isValueChanged = true;
    }

    private void updateCompoundData (String cellValue, int row, int col) throws Exception {
        if (!(dataset instanceof CompoundDS) || (cellValue == null) || ((cellValue = cellValue.trim()) == null)) {
            return;
        }
        log.trace("DefaultTableView: updateCompoundData");

        CompoundDS compDS = (CompoundDS) dataset;
        List<?> cdata = (List<?>) compDS.getData();
        int orders[] = compDS.getSelectedMemberOrders();
        Datatype types[] = compDS.getSelectedMemberTypes();
        int nFields = cdata.size();
        int nSubColumns = (table.getPreferredColumnCount() - 1) / nFields;
        //table.getRowCount();
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
            return;
        }
        else if (types[column].getDatatypeClass() == Datatype.CLASS_STRING) {
            // it is string but not converted, still byte array
            int strlen = (int)types[column].getDatatypeSize();
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
            showError("Number of data points < " + morder + ".", shell.getText());
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

    /**
     * Import data values from text file.
     */
    private void importTextData (String fname) {
        int cols = selectionLayer.getPreferredColumnCount();
        int rows = selectionLayer.getPreferredRowCount();
        int r0;
        int c0;

        Rectangle lastSelection = selectionLayer.getLastSelectedRegion();
        if(lastSelection != null) {
            r0 = lastSelection.y;
            c0 = lastSelection.x;

            if (c0 < 0) {
                c0 = 0;
            }
            if (r0 < 0) {
                r0 = 0;
            }
        } else {
            r0 = 0;
            c0 = 0;
        }

        // Start at the first column for compound datasets
        if (dataset instanceof CompoundDS) c0 = 0;

        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(fname));
        }
        catch (FileNotFoundException ex) {
            log.debug("import data values from text file {}:", fname, ex);
            return;
        }

        String line = null;
        StringTokenizer tokenizer1 = null;

        try {
            line = in.readLine();
        }
        catch (IOException ex) {
            try {
                in.close();
            }
            catch (IOException ex2) {
                log.debug("close text file {}:", fname, ex2);
            }
            log.debug("read text file {}:", fname, ex);
            return;
        }

        String delName = ViewProperties.getDataDelimiter();
        String delimiter = "";

        // delimiter must include a tab to be consistent with copy/paste for
        // compound fields
        if (dataset instanceof CompoundDS)
            delimiter = "\t";
        else {
            if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_TAB)) {
                delimiter = "\t";
            }
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SPACE)) {
                delimiter = " " + delimiter;
            }
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COMMA)) {
                delimiter = ",";
            }
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COLON)) {
                delimiter = ":";
            }
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SEMI_COLON)) {
                delimiter = ";";
            }
        }
        String token = null;
        int r = r0;
        int c = c0;
        while ((line != null) && (r < rows)) {
            if (fixedDataLength > 0) {
                // the data has fixed length
                int n = line.length();
                String theVal;
                for (int i = 0; i < n; i = i + fixedDataLength) {
                    try {
                        theVal = line.substring(i, i + fixedDataLength);
                        updateValueInMemory(theVal, r, c);
                    }
                    catch (Exception ex) {
                        continue;
                    }
                    c++;
                }
            }
            else {
                try {
                    tokenizer1 = new StringTokenizer(line, delimiter);
                    while (tokenizer1.hasMoreTokens() && (c < cols)) {
                        token = tokenizer1.nextToken();
                        if (dataset instanceof ScalarDS) {
                            StringTokenizer tokenizer2 = new StringTokenizer(token);
                            while (tokenizer2.hasMoreTokens() && (c < cols)) {
                                updateValueInMemory(tokenizer2.nextToken(), r, c);
                                c++;
                            }
                        }
                        else {
                            updateValueInMemory(token, r, c);
                            c++;
                        }
                    } // while (tokenizer1.hasMoreTokens() && index < size)
                }
                catch (Exception ex) {
                    showError(ex.getMessage(), shell.getText());

                    try {
                        in.close();
                    }
                    catch (IOException ex2) {
                        log.debug("close text file {}:", fname, ex2);
                    }
                    return;
                }
            }

            try {
                line = in.readLine();
            }
            catch (IOException ex) {
                log.debug("read text file {}:", fname, ex);
                line = null;
            }

            // Start at the first column for compound datasets
            if (dataset instanceof CompoundDS) {
                c = 0;
            } else {
                c = c0;
            }

            //c = 0; // causes a bug where data is imported to the left
            r++;
        } // while ((line != null) && (r < rows))

        try {
            in.close();
        }
        catch (IOException ex) {
            log.debug("close text file {}:", fname, ex);
        }
    }

    /**
     * Import data values from binary file.
     */
    private void importBinaryData() {
        String currentDir = dataset.getFileFormat().getParent();

        FileDialog fchooser = new FileDialog(shell, SWT.OPEN);
        fchooser.setFilterPath(currentDir);
        //fchooser.setFileFilter(DefaultFileFilter.getFileFilterBinary());
        fchooser.setFilterExtensions(new String[] {"*.bin", "*.*"});
        fchooser.setFilterNames(new String[] {"Binary Files (*.bin)", "All Files (*.*)"});
        fchooser.setFilterIndex(0);

        if (fchooser.open() == null) return;

        File chosenFile = new File(fchooser.getFilterPath() + File.separator + fchooser.getFileName());
        if (!chosenFile.exists()) {
            showError("File " + chosenFile.getName() + " does not exist.", "Import Data from Binary File");
            return;
        }

        MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(shell.getText());
        confirm.setMessage("Do you want to paste selected data?");
        if (confirm.open() == SWT.NO) return;

        getBinaryDataFromFile(chosenFile.getAbsolutePath());
    }

    /** Reads data from a binary file into a buffer and updates table. */
    private void getBinaryDataFromFile (String fileName) {
        String fname = fileName;
        FileInputStream inputFile = null;
        BufferedInputStream in = null;
        ByteBuffer byteBuffer = null;

        try {
            inputFile = new FileInputStream(fname);
            long fileSize = inputFile.getChannel().size();
            in = new BufferedInputStream(inputFile);

            Object data = dataset.getData();
            int datasetSize = Array.getLength(data);
            String cname = data.getClass().getName();
            char dname = cname.charAt(cname.lastIndexOf("[") + 1);

            if (dname == 'B') {
                long datasetByteSize = datasetSize;
                byteBuffer = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetByteSize);

                int remainingSize = bufferSize - (BYTE_BUFFER_SIZE);
                int allocValue = 0;
                int iterationNumber = 0;
                byte[] byteArray = new byte[BYTE_BUFFER_SIZE];
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (BYTE_BUFFER_SIZE);
                    }
                    else {
                        allocValue = (BYTE_BUFFER_SIZE);
                    }

                    in.read(byteBuffer.array(), 0, allocValue);

                    byteBuffer.get(byteArray, 0, allocValue);
                    System.arraycopy(byteArray, 0, dataValue, (iterationNumber * BYTE_BUFFER_SIZE), allocValue);
                    byteBuffer.clear();
                    remainingSize = remainingSize - (BYTE_BUFFER_SIZE);
                    iterationNumber++;
                } while (remainingSize > -(BYTE_BUFFER_SIZE));

                isValueChanged = true;
            }
            else if (dname == 'S') {
                long datasetShortSize = datasetSize * 2;
                byteBuffer = ByteBuffer.allocate(SHORT_BUFFER_SIZE * 2);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetShortSize);
                int remainingSize = bufferSize - (SHORT_BUFFER_SIZE * 2);
                int allocValue = 0;
                int iterationNumber = 0;
                ShortBuffer sb = byteBuffer.asShortBuffer();
                short[] shortArray = new short[SHORT_BUFFER_SIZE];

                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (SHORT_BUFFER_SIZE * 2);
                    }
                    else {
                        allocValue = (SHORT_BUFFER_SIZE * 2);
                    }
                    in.read(byteBuffer.array(), 0, allocValue);
                    sb.get(shortArray, 0, allocValue / 2);
                    System.arraycopy(shortArray, 0, dataValue, (iterationNumber * SHORT_BUFFER_SIZE), allocValue / 2);
                    byteBuffer.clear();
                    sb.clear();
                    remainingSize = remainingSize - (SHORT_BUFFER_SIZE * 2);
                    iterationNumber++;
                } while (remainingSize > -(SHORT_BUFFER_SIZE * 2));

                isValueChanged = true;
            }
            else if (dname == 'I') {
                long datasetIntSize = datasetSize * 4;
                byteBuffer = ByteBuffer.allocate(INT_BUFFER_SIZE * 4);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetIntSize);
                int remainingSize = bufferSize - (INT_BUFFER_SIZE * 4);
                int allocValue = 0;
                int iterationNumber = 0;
                int[] intArray = new int[INT_BUFFER_SIZE];
                byte[] tmpBuf = byteBuffer.array();
                IntBuffer ib = byteBuffer.asIntBuffer();

                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (INT_BUFFER_SIZE * 4);
                    }
                    else {
                        allocValue = (INT_BUFFER_SIZE * 4);
                    }
                    in.read(tmpBuf, 0, allocValue);
                    ib.get(intArray, 0, allocValue / 4);
                    System.arraycopy(intArray, 0, dataValue, (iterationNumber * INT_BUFFER_SIZE), allocValue / 4);
                    byteBuffer.clear();
                    ib.clear();
                    remainingSize = remainingSize - (INT_BUFFER_SIZE * 4);
                    iterationNumber++;
                } while (remainingSize > -(INT_BUFFER_SIZE * 4));

                isValueChanged = true;
            }
            else if (dname == 'J') {
                long datasetLongSize = datasetSize * 8;
                byteBuffer = ByteBuffer.allocate(LONG_BUFFER_SIZE * 8);

                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetLongSize);
                int remainingSize = bufferSize - (LONG_BUFFER_SIZE * 8);
                int allocValue = 0;
                int iterationNumber = 0;
                long[] longArray = new long[LONG_BUFFER_SIZE];
                LongBuffer lb = byteBuffer.asLongBuffer();

                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (LONG_BUFFER_SIZE * 8);
                    }
                    else {
                        allocValue = (LONG_BUFFER_SIZE * 8);
                    }

                    in.read(byteBuffer.array(), 0, allocValue);
                    lb.get(longArray, 0, allocValue / 8);
                    System.arraycopy(longArray, 0, dataValue, (iterationNumber * LONG_BUFFER_SIZE), allocValue / 8);
                    byteBuffer.clear();
                    lb.clear();
                    remainingSize = remainingSize - (LONG_BUFFER_SIZE * 8);
                    iterationNumber++;
                } while (remainingSize > -(LONG_BUFFER_SIZE * 8));

                isValueChanged = true;
            }
            else if (dname == 'F') {
                long datasetFloatSize = datasetSize * 4;
                byteBuffer = ByteBuffer.allocate(FLOAT_BUFFER_SIZE * 4);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetFloatSize);
                int remainingSize = bufferSize - (FLOAT_BUFFER_SIZE * 4);
                int allocValue = 0;
                int iterationNumber = 0;
                FloatBuffer fb = byteBuffer.asFloatBuffer();
                float[] floatArray = new float[FLOAT_BUFFER_SIZE];
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (FLOAT_BUFFER_SIZE * 4);
                    }
                    else {
                        allocValue = (FLOAT_BUFFER_SIZE * 4);
                    }

                    in.read(byteBuffer.array(), 0, allocValue);
                    fb.get(floatArray, 0, allocValue / 4);
                    System.arraycopy(floatArray, 0, dataValue, (iterationNumber * FLOAT_BUFFER_SIZE), allocValue / 4);
                    byteBuffer.clear();
                    fb.clear();
                    remainingSize = remainingSize - (FLOAT_BUFFER_SIZE * 4);
                    iterationNumber++;
                } while (remainingSize > -(FLOAT_BUFFER_SIZE * 4));

                isValueChanged = true;
            }
            else if (dname == 'D') {
                long datasetDoubleSize = datasetSize * 8;
                byteBuffer = ByteBuffer.allocate(DOUBLE_BUFFER_SIZE * 8);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetDoubleSize);
                int remainingSize = bufferSize - (DOUBLE_BUFFER_SIZE * 8);
                int allocValue = 0;
                int iterationNumber = 0;
                DoubleBuffer db = byteBuffer.asDoubleBuffer();
                double[] doubleArray = new double[DOUBLE_BUFFER_SIZE];

                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (DOUBLE_BUFFER_SIZE * 8);
                    }
                    else {
                        allocValue = (DOUBLE_BUFFER_SIZE * 8);
                    }

                    in.read(byteBuffer.array(), 0, allocValue);
                    db.get(doubleArray, 0, allocValue / 8);
                    System.arraycopy(doubleArray, 0, dataValue, (iterationNumber * DOUBLE_BUFFER_SIZE), allocValue / 8);
                    byteBuffer.clear();
                    db.clear();
                    remainingSize = remainingSize - (DOUBLE_BUFFER_SIZE * 8);
                    iterationNumber++;
                } while (remainingSize > -(DOUBLE_BUFFER_SIZE * 8));

                isValueChanged = true;
            }
        }
        catch (Exception es) {
            es.printStackTrace();
        }
        finally {
            try {
                in.close();
                inputFile.close();
            }
            catch (IOException ex) {
                log.debug("close binary file {}:", fname, ex);
            }
        }

        table.doCommand(new StructuralRefreshCommand());
    }

    /** Save data as text. */
    private void saveAsText() throws Exception {
        FileDialog fchooser = new FileDialog(shell, SWT.SAVE);
        fchooser.setFilterPath(dataset.getFile());
        //fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
        //fchooser.changeToParentDirectory();
        fchooser.setText("Save Current Data To Text File --- " + dataset.getName());

        //fchooser.setSelectedFile(new File(dataset.getName() + ".txt"));

        if(fchooser.open() == null) return;

        File chosenFile = new File(fchooser.getFilterPath() + File.separator + fchooser.getFileName());
        String fname = chosenFile.getAbsolutePath();

        log.trace("DefaultTableView: saveAsText: file={}", fname);

        // Check if the file is in use and prompt for overwrite
        if(chosenFile.exists()) {
            List<?> fileList = viewer.getTreeView().getCurrentFiles();
            if (fileList != null) {
                FileFormat theFile = null;
                Iterator<?> iterator = fileList.iterator();
                while (iterator.hasNext()) {
                    theFile = (FileFormat) iterator.next();
                    if (theFile.getFilePath().equals(fname)) {
                        shell.getDisplay().beep();
                        showError("Unable to save data to file \"" + fname + "\". \nThe file is being used.", shell.getText());
                        return;
                    }
                }
            }

            MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            confirm.setText(shell.getText());
            confirm.setMessage("File exists. Do you want to replace it?");
            if (confirm.open() == SWT.NO) return;
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(chosenFile)));

        String delName = ViewProperties.getDataDelimiter();
        String delimiter = "";

        // delimiter must include a tab to be consistent with copy/paste for
        // compound fields
        if (dataset instanceof CompoundDS) delimiter = "\t";

        if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_TAB)) {
            delimiter = "\t";
        }
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SPACE)) {
            delimiter = " " + delimiter;
        }
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COMMA)) {
            delimiter = "," + delimiter;
        }
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COLON)) {
            delimiter = ":" + delimiter;
        }
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SEMI_COLON)) {
            delimiter = ";" + delimiter;
        }

        int cols = selectionLayer.getPreferredColumnCount();
        int rows = selectionLayer.getPreferredRowCount();

        for (int i = 0; i < rows; i++) {
            out.print(selectionLayer.getDataValueByPosition(0, i));
            for (int j = 1; j < cols; j++) {
                out.print(delimiter);
                out.print(selectionLayer.getDataValueByPosition(j, i));
            }
            out.println();
        }

        out.flush();
        out.close();

        viewer.showStatus("Data saved to: " + fname);
    }

    /** Save data as binary. */
    private void saveAsBinary() throws Exception {
        FileDialog fchooser = new FileDialog(shell, SWT.SAVE);
        fchooser.setFilterPath(dataset.getFile());
        //fchooser.setFileFilter(DefaultFileFilter.getFileFilterBinary());
        //fchooser.changeToParentDirectory();
        fchooser.setText("Save Current Data To Binary File --- " + dataset.getName());

        //fchooser.setSelectedFile(new File(dataset.getName() + ".bin"));

        if(fchooser.open() == null) return;

        File chosenFile = new File(fchooser.getFilterPath() + File.separator + fchooser.getFileName());
        String fname = chosenFile.getAbsolutePath();

        log.trace("DefaultTableView: saveAsBinary: file={}", fname);

        // Check if the file is in use and prompt for overwrite
        if(chosenFile.exists()) {
            List<?> fileList = viewer.getTreeView().getCurrentFiles();
            if (fileList != null) {
                FileFormat theFile = null;
                Iterator<?> iterator = fileList.iterator();
                while (iterator.hasNext()) {
                    theFile = (FileFormat) iterator.next();
                    if (theFile.getFilePath().equals(fname)) {
                        shell.getDisplay().beep();
                        showError("Unable to save data to file \"" + fname + "\". \nThe file is being used.", shell.getText());
                        return;
                    }
                }
            }

            MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            confirm.setText(shell.getText());
            confirm.setMessage("File exists. Do you want to replace it?");
            if (confirm.open() == SWT.NO) return;
        }

        FileOutputStream outputFile = new FileOutputStream(chosenFile);
        DataOutputStream out = new DataOutputStream(outputFile);

        if (dataset instanceof ScalarDS) {
            ((ScalarDS) dataset).convertToUnsignedC();
            Object data = dataset.getData();
            String cname = data.getClass().getName();
            char dname = cname.charAt(cname.lastIndexOf("[") + 1);
            ByteBuffer bb = null;

            int size = Array.getLength(data);

            if (dname == 'B') {
                byte[] bdata = new byte[size];
                bdata = (byte[]) data;

                bb = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                int remainingSize = size - BYTE_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + BYTE_BUFFER_SIZE;
                    }
                    else {
                        allocValue = BYTE_BUFFER_SIZE;
                    }
                    bb.clear();
                    bb.put(bdata, (iterationNumber * BYTE_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue);
                    remainingSize = remainingSize - BYTE_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -BYTE_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'S') {
                short[] sdata = new short[size];
                sdata = (short[]) data;
                bb = ByteBuffer.allocate(SHORT_BUFFER_SIZE * 2);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                ShortBuffer sb = bb.asShortBuffer();
                int remainingSize = size - SHORT_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + SHORT_BUFFER_SIZE;
                    }
                    else {
                        allocValue = SHORT_BUFFER_SIZE;
                    }
                    bb.clear();
                    sb.clear();
                    sb.put(sdata, (iterationNumber * SHORT_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 2);
                    remainingSize = remainingSize - SHORT_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -SHORT_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'I') {
                int[] idata = new int[size];
                idata = (int[]) data;
                bb = ByteBuffer.allocate(INT_BUFFER_SIZE * 4);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                IntBuffer ib = bb.asIntBuffer();
                int remainingSize = size - INT_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + INT_BUFFER_SIZE;
                    }
                    else {
                        allocValue = INT_BUFFER_SIZE;
                    }
                    bb.clear();
                    ib.clear();
                    ib.put(idata, (iterationNumber * INT_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 4);
                    remainingSize = remainingSize - INT_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -INT_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'J') {
                long[] ldata = new long[size];
                ldata = (long[]) data;

                bb = ByteBuffer.allocate(LONG_BUFFER_SIZE * 8);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                LongBuffer lb = bb.asLongBuffer();
                int remainingSize = size - LONG_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + LONG_BUFFER_SIZE;
                    }
                    else {
                        allocValue = LONG_BUFFER_SIZE;
                    }
                    bb.clear();
                    lb.clear();
                    lb.put(ldata, (iterationNumber * LONG_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 8);
                    remainingSize = remainingSize - LONG_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -LONG_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'F') {
                float[] fdata = new float[size];
                fdata = (float[]) data;

                bb = ByteBuffer.allocate(FLOAT_BUFFER_SIZE * 4);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                FloatBuffer fb = bb.asFloatBuffer();
                int remainingSize = size - FLOAT_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + FLOAT_BUFFER_SIZE;
                    }
                    else {
                        allocValue = FLOAT_BUFFER_SIZE;
                    }
                    bb.clear();
                    fb.clear();
                    fb.put(fdata, (iterationNumber * FLOAT_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 4);
                    remainingSize = remainingSize - FLOAT_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -FLOAT_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'D') {
                double[] ddata = new double[size];
                ddata = (double[]) data;

                bb = ByteBuffer.allocate(DOUBLE_BUFFER_SIZE * 8);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                DoubleBuffer db = bb.asDoubleBuffer();
                int remainingSize = size - DOUBLE_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + DOUBLE_BUFFER_SIZE;
                    }
                    else {
                        allocValue = DOUBLE_BUFFER_SIZE;
                    }
                    bb.clear();
                    db.clear();
                    db.put(ddata, (iterationNumber * DOUBLE_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 8);
                    remainingSize = remainingSize - DOUBLE_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -DOUBLE_BUFFER_SIZE);

                out.flush();
                out.close();
            }
        }

        viewer.showStatus("Data saved to: " + fname);
    }

    private void showLineplot() {
        // Since NatTable returns the selected row positions as a Set<Range>, convert this to
        // an Integer[]
        Set<Range> rowPositions = selectionLayer.getSelectedRowPositions();
        Set<Integer> selectedRowPos = new LinkedHashSet<Integer>();
        Iterator<Range> i1 = rowPositions.iterator();
        while(i1.hasNext()) {
            selectedRowPos.addAll(i1.next().getMembers());
        }

        Integer[] rows = selectedRowPos.toArray(new Integer[0]);
        int[] cols = selectionLayer.getSelectedColumnPositions();

        if ((rows == null) || (cols == null) || (rows.length <= 0) || (cols.length <= 0)) {
            shell.getDisplay().beep();
            showError("Select rows/columns to draw line plot.", shell.getText());
            return;
        }

        int nrow = table.getPreferredRowCount() - 1;
        int ncol = table.getPreferredColumnCount() - 1;

        log.trace("DefaultTableView showLineplot: {} - {}", nrow, ncol);
        LinePlotOption lpo = new LinePlotOption(shell, SWT.NONE, nrow, ncol);
        lpo.open();

        int plotType = lpo.getPlotBy();
        if (plotType == LinePlotOption.NO_PLOT) {
            return;
        }

        boolean isRowPlot = (plotType == LinePlotOption.ROW_PLOT);
        int xIndex = lpo.getXindex();

        // figure out to plot data by row or by column
        // Plot data by rows if all columns are selected and part of
        // rows are selected, otherwise plot data by column
        double[][] data = null;
        int nLines = 0;
        String title = "Lineplot - " + dataset.getPath() + dataset.getName();
        String[] lineLabels = null;
        double[] yRange = { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
        double xData[] = null;

        if (isRowPlot) {
            title += " - by row";
            nLines = rows.length;
            if (nLines > 10) {
                shell.getDisplay().beep();
                nLines = 10;
                MessageBox warning = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                warning.setText(shell.getText());
                warning.setMessage("More than 10 rows are selected.\n" + "The first 10 rows will be displayed.");
                warning.open();
            }
            lineLabels = new String[nLines];
            data = new double[nLines][cols.length];

            double value = 0.0;
            for (int i = 0; i < nLines; i++) {
                lineLabels[i] = String.valueOf(rows[i] + indexBase);
                for (int j = 0; j < cols.length; j++) {
                    data[i][j] = 0;
                    try {
                        value = Double.parseDouble(table.getDataValueByPosition(cols[j], rows[i]).toString());
                        data[i][j] = value;
                        yRange[0] = Math.min(yRange[0], value);
                        yRange[1] = Math.max(yRange[1], value);
                    }
                    catch (NumberFormatException ex) {
                        log.debug("rows[{}]:", i, ex);
                    }
                } // for (int j = 0; j < ncols; j++)
            } // for (int i = 0; i < rows.length; i++)

            if (xIndex >= 0) {
                xData = new double[cols.length];
                for (int j = 0; j < cols.length; j++) {
                    xData[j] = 0;
                    try {
                        value = Double.parseDouble(table.getDataValueByPosition(cols[j], xIndex).toString());
                        xData[j] = value;
                    }
                    catch (NumberFormatException ex) {
                        log.debug("xIndex of {}:", xIndex, ex);
                    }
                }
            }
        } // if (isRowPlot)
        else {
            title += " - by column";
            nLines = cols.length;
            if (nLines > 10) {
                shell.getDisplay().beep();
                nLines = 10;
                MessageBox warning = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                warning.setText(shell.getText());
                warning.setMessage("More than 10 columns are selected.\n" + "The first 10 columns will be displayed.");
                warning.open();
            }
            lineLabels = new String[nLines];
            data = new double[nLines][rows.length];
            double value = 0.0;
            for (int j = 0; j < nLines; j++) {
                lineLabels[j] = columnHeaderDataProvider.getDataValue(cols[j] + indexBase, 0).toString();
                for (int i = 0; i < rows.length; i++) {
                    data[j][i] = 0;
                    try {
                        value = Double.parseDouble(table.getDataValueByPosition(cols[j], rows[i]).toString());
                        data[j][i] = value;
                        yRange[0] = Math.min(yRange[0], value);
                        yRange[1] = Math.max(yRange[1], value);
                    }
                    catch (NumberFormatException ex) {
                        log.debug("cols[{}]:", j, ex);
                    }
                } // for (int j=0; j<ncols; j++)
            } // for (int i=0; i<rows.length; i++)

            if (xIndex >= 0) {
                xData = new double[rows.length];
                for (int j = 0; j < rows.length; j++) {
                    xData[j] = 0;
                    try {
                        value = Double.parseDouble(table.getDataValueByPosition(xIndex, rows[j]).toString());
                        xData[j] = value;
                    }
                    catch (NumberFormatException ex) {
                        log.debug("xIndex of {}:", xIndex, ex);
                    }
                }
            }
        } // else

        int n = removeInvalidPlotData(data, xData, yRange);
        if (n < data[0].length) {
            double[][] dataNew = new double[data.length][n];
            for (int i = 0; i < data.length; i++)
                System.arraycopy(data[i], 0, dataNew[i], 0, n);

            data = dataNew;

            if (xData != null) {
                double[] xDataNew = new double[n];
                System.arraycopy(xData, 0, xDataNew, 0, n);
                xData = xDataNew;
            }
        }

        // allow to draw a flat line: all values are the same
        if (yRange[0] == yRange[1]) {
            yRange[1] += 1;
            yRange[0] -= 1;
        }
        else if (yRange[0] > yRange[1]) {
            shell.getDisplay().beep();
            showError("Cannot show line plot for the selected data. \n" + "Please check the data range: ("
                    + yRange[0] + ", " + yRange[1] + ").", shell.getText());
            data = null;
            return;
        }
        if (xData == null) { // use array index and length for x data range
            xData = new double[2];
            xData[0] = indexBase; // 1- or zero-based
            xData[1] = data[0].length + indexBase - 1; // maximum index
        }

        Chart cv = new Chart(shell, title, Chart.LINEPLOT, data, xData, yRange);
        cv.setLineLabels(lineLabels);

        String cname = dataValue.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        if ((dname == 'B') || (dname == 'S') || (dname == 'I') || (dname == 'J')) {
            cv.setTypeToInteger();
        }

        cv.open();
    }

    // Allow a ScalarDS cell to be edited under specific conditions
    private IEditableRule getScalarDSEditRule(final IDataProvider dataProvider) {
        return new EditableRule() {
            @Override
            public boolean isEditable(int columnIndex, int rowIndex) {
                if (isReadOnly || isDisplayTypeChar || showAsBin || showAsHex
                        || dataset.getDatatype().getDatatypeClass() == Datatype.CLASS_ARRAY) {
                    return false;
                }
                else {
                    return true;
                }
            }
        };
    }

    // Allow a CompoundDS cell to be edited as long as TableView is not in read-only mode
    private IEditableRule getCompoundDSEditRule(final IDataProvider dataProvider) {
        return new EditableRule() {
            @Override
            public boolean isEditable(int columnIndex, int rowIndex) {
                return !isReadOnly;
            }
        };
    }

    private class ScalarDSDataProvider implements IDataProvider {
        private final StringBuffer stringBuffer     = new StringBuffer();
        private final Datatype     dtype            = dataset.getDatatype();
        private final Datatype     btype            = dtype.getBasetype();
        private final long          typeSize         = dtype.getDatatypeSize();
        private final boolean      isArray          = (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY);
        private final boolean      isStr            = (NT == 'L');
        private final boolean      isInt            = (NT == 'B' || NT == 'S' || NT == 'I' || NT == 'J');
        private final boolean      isUINT64         = (dtype.isUnsigned() && (NT == 'J'));
        private Object             theValue;

        boolean                    isNaturalOrder   = (dataset.getRank() == 1 || (dataset.getSelectedIndex()[0] < dataset
                                                            .getSelectedIndex()[1]));

        private long                rowCount         = dataset.getHeight();
        private long                colCount         = dataset.getWidth();

        public ScalarDSDataProvider() {

        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            if (startEditing[0]) return "";
            log.trace("ScalarDS:ScalarDSDataProvider:getValueAt({},{}) start", columnIndex, rowIndex);
            log.trace("ScalarDS:ScalarDSDataProvider:getValueAt isInt={} isArray={} showAsHex={} showAsBin={}", isInt, isArray, showAsHex, showAsBin);

            if (isArray) {
                // ARRAY dataset
                long arraySize = dtype.getDatatypeSize() / btype.getDatatypeSize();
                log.trace("ScalarDS:ScalarDSDataProvider:getValueAt ARRAY dataset size={} isDisplayTypeChar={} isUINT64={}",
                        arraySize, isDisplayTypeChar, isUINT64);

                stringBuffer.setLength(0); // clear the old string
                int i0 = (int)(rowIndex * colCount + columnIndex) * (int)arraySize;
                int i1 = i0 + (int)arraySize;

                if (isDisplayTypeChar) {
                    for (int i = i0; i < i1; i++) {
                        stringBuffer.append(Array.getChar(dataValue, i));
                        if (stringBuffer.length() > 0 && i < (i1 - 1)) stringBuffer.append(", ");
                    }
                }
                else {
                    if (isUINT64) {
                        for (int i = i0; i < i1; i++) {
                            Long l = (Long) Array.get(dataValue, i);
                            if (l < 0) {
                                l = (l << 1) >>> 1;
                                BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                                BigInteger big2 = new BigInteger(l.toString());
                                BigInteger big = big1.add(big2);
                                stringBuffer.append(big.toString());
                            }
                            else
                                stringBuffer.append(Array.get(dataValue, i));
                            if (stringBuffer.length() > 0 && i < (i1 - 1)) stringBuffer.append(", ");
                        }
                    }
                    else {
                        for (int i = i0; i < i1; i++) {
                            stringBuffer.append(Array.get(dataValue, i));
                            if (stringBuffer.length() > 0 && i < (i1 - 1)) stringBuffer.append(", ");
                        }
                    }
                }
                theValue = stringBuffer;
            }
            else {
                long index = columnIndex * rowCount + rowIndex;

                if (dataset.getRank() > 1) {
                    log.trace("ScalarDS:ScalarDSDataProvider:getValueAt rank={} isDataTransposed={} isNaturalOrder={}", dataset.getRank(), isDataTransposed, isNaturalOrder);
                    if ((isDataTransposed && isNaturalOrder) || (!isDataTransposed && !isNaturalOrder))
                        index = columnIndex * rowCount + rowIndex;
                    else
                        index = rowIndex * colCount + columnIndex;
                }
                log.trace("ScalarDS:ScalarDSDataProvider:getValueAt index={} isStr={} isUINT64={}", index, isStr, isUINT64);

                if (isStr) {
                    theValue = Array.get(dataValue, (int)index);
                    return theValue;
                }

                if (isUINT64) {
                    theValue = Array.get(dataValue, (int)index);
                    Long l = (Long) theValue;
                    if (l < 0) {
                        l = (l << 1) >>> 1;
                        BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                        BigInteger big2 = new BigInteger(l.toString());
                        BigInteger big = big1.add(big2);
                        theValue = big.toString();
                    }
                }
                else if (showAsHex && isInt) {
                    // show in Hexadecimal
                    char[] hexArray = "0123456789ABCDEF".toCharArray();
                    theValue = Array.get(dataValue, (int)(index * typeSize));
                    log.trace("ScalarDS:ScalarDSDataProvider:getValueAt() theValue[{}]={}", index, theValue.toString());
                    // show in Hexadecimal
                    char[] hexChars = new char[2];
                    stringBuffer.setLength(0); // clear the old string
                    for (int x = 0; x < typeSize; x++) {
                        if (x > 0)
                            theValue = Array.get(dataValue, (int)(index * typeSize) + x);
                        int v = (int)((Byte)theValue) & 0xFF;
                        hexChars[0] = hexArray[v >>> 4];
                        hexChars[1] = hexArray[v & 0x0F];
                        if (x > 0) stringBuffer.append(":");
                        stringBuffer.append(hexChars);
                        log.trace("ScalarDS:ScalarDSDataProvider:getValueAt() hexChars[{}]={}", x, hexChars);
                    }
                    theValue = stringBuffer;
                }
                else if (showAsBin && isInt) {
                    theValue = Array.get(dataValue, (int)index);
                    theValue = Tools.toBinaryString(Long.valueOf(theValue.toString()), (int)typeSize);
                    // theValue =
                    // Long.toBinaryString(Long.valueOf(theValue.toString()));
                }
                else if (numberFormat != null) {
                    // show in scientific format
                    theValue = Array.get(dataValue, (int)index);
                    theValue = numberFormat.format(theValue);
                }
                else {
                    theValue = Array.get(dataValue, (int)index);
                }
            }

            log.trace("ScalarDS:ScalarDSDataProvider:getValueAt finish");
            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            try {
                updateValueInMemory((String) newValue, rowIndex, columnIndex);
            }
            catch (Exception ex) {
                display.beep();
                showError(ex.getMessage(), shell.getText());
            }
        }

        @Override
        public int getColumnCount() {
            return (int)colCount;
        }

        @Override
        public int getRowCount() {
            return (int)rowCount;
        }
    }

    private class CompoundDSDataProvider implements IDataProvider {
        CompoundDS                compound         = (CompoundDS) dataset;
        int                       orders[]         = compound.getSelectedMemberOrders();
        Datatype                  types[]          = compound.getSelectedMemberTypes();
        StringBuffer              stringBuffer     = new StringBuffer();
        int                       nFields          = ((List<?>) dataValue).size();
        int                       nSubColumns      = (nFields > 0) ? getColumnCount() / nFields : 0;
        int                       nRows            = (int)compound.getHeight();
        int                       nCols            = compound.getSelectedMemberCount();

        public CompoundDSDataProvider() {

        }

        @Override
        public Object getDataValue(int col, int row) {
            if (startEditing[0]) return "";

            int fieldIdx = col;
            int rowIdx = row;
            char CNT = ' ';
            boolean CshowAsHex = false;
            boolean CshowAsBin = false;
            log.trace("CompoundDS:CompoundDSDataProvider:getValueAt({},{}) start", row, col);

            if (nSubColumns > 1) { // multi-dimension compound dataset
                int colIdx = col / nFields;
                fieldIdx = col - colIdx * nFields;
                // BUG 573: rowIdx = row * orders[fieldIdx] + colIdx * nRows
                // * orders[fieldIdx];
                rowIdx = row * orders[fieldIdx] * nSubColumns + colIdx * orders[fieldIdx];
                log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() row={} orders[{}]={} nSubColumns={} colIdx={}", row, fieldIdx, orders[fieldIdx], nSubColumns, colIdx);
            }
            else {
                rowIdx = row * orders[fieldIdx];
                log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() row={} orders[{}]={}", row, fieldIdx, orders[fieldIdx]);
            }
            log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() rowIdx={}", rowIdx);

            Object colValue = ((List<?>) dataValue).get(fieldIdx);
            if (colValue == null) {
                return "Null";
            }

            stringBuffer.setLength(0); // clear the old string
            boolean isString = (types[fieldIdx].getDatatypeClass() == Datatype.CLASS_STRING);
            if (isString && (colValue instanceof byte[])) {
                // strings
                int strlen = (int)types[fieldIdx].getDatatypeSize();
                String str = new String(((byte[]) colValue), rowIdx * strlen, strlen);
                int idx = str.indexOf('\0');
                if (idx > 0) {
                    str = str.substring(0, idx);
                }
                stringBuffer.append(str.trim());
            }
            else {
                // numerical values
                Datatype dtype = types[fieldIdx];
                if (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY) dtype = types[fieldIdx].getBasetype();

                String cName = colValue.getClass().getName();
                int cIndex = cName.lastIndexOf("[");
                if (cIndex >= 0) {
                    CNT = cName.charAt(cIndex + 1);
                }
                log.trace("CompoundDS:CompoundDSDataProvider:getValueAt(): cName={} CNT={}", cName, CNT);

                boolean isUINT64 = false;
                boolean isInt = (CNT == 'B' || CNT == 'S' || CNT == 'I' || CNT == 'J');
                int typeSize = (int)dtype.getDatatypeSize();

                if ((dtype.getDatatypeClass() == Datatype.CLASS_BITFIELD) || (dtype.getDatatypeClass() == Datatype.CLASS_OPAQUE)) {
                    CshowAsHex = true;
                    log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() class={} (BITFIELD or OPAQUE)", dtype.getDatatypeClass());
                }
                if (dtype.isUnsigned()) {
                    if (cIndex >= 0) {
                        isUINT64 = (cName.charAt(cIndex + 1) == 'J');
                    }
                }
                log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() isUINT64={} isInt={} CshowAsHex={} typeSize={}", isUINT64, isInt, CshowAsHex, typeSize);

                for (int i = 0; i < orders[fieldIdx]; i++) {
                    if (isUINT64) {
                        Object theValue = Array.get(colValue, rowIdx + i);
                        log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() theValue[{}]={}", i, theValue.toString());
                        Long l = (Long) theValue;
                        if (l < 0) {
                            l = (l << 1) >>> 1;
                            BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                            BigInteger big2 = new BigInteger(l.toString());
                            BigInteger big = big1.add(big2);
                            theValue = big.toString();
                        }
                        if (i > 0) stringBuffer.append(", ");
                        stringBuffer.append(theValue);
                    }
                    else if (CshowAsHex && isInt) {
                        char[] hexArray = "0123456789ABCDEF".toCharArray();
                        Object theValue = Array.get(colValue, rowIdx * typeSize + typeSize * i);
                        log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() theValue[{}]={}", i, theValue.toString());
                        // show in Hexadecimal
                        char[] hexChars = new char[2];
                        if (i > 0) stringBuffer.append(", ");
                        for (int x = 0; x < typeSize; x++) {
                            if (x > 0)
                                theValue = Array.get(colValue, rowIdx * typeSize + typeSize * i + x);
                            int v = (int)((Byte)theValue) & 0xFF;
                            hexChars[0] = hexArray[v >>> 4];
                            hexChars[1] = hexArray[v & 0x0F];
                            if (x > 0) stringBuffer.append(":");
                            stringBuffer.append(hexChars);
                            log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() hexChars[{}]={}", x, hexChars);
                        }
                    }
                    else if (showAsBin && isInt) {
                        Object theValue = Array.get(colValue, rowIdx + typeSize * i);
                        log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() theValue[{}]={}", i, theValue.toString());
                        theValue = Tools.toBinaryString(Long.valueOf(theValue.toString()), typeSize);
                        if (i > 0) stringBuffer.append(", ");
                        stringBuffer.append(theValue);
                    }
                    else if (numberFormat != null) {
                        // show in scientific format
                        Object theValue = Array.get(colValue, rowIdx + i);
                        log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() theValue[{}]={}", i, theValue.toString());
                        theValue = numberFormat.format(theValue);
                        if (i > 0) stringBuffer.append(", ");
                        stringBuffer.append(theValue);
                    }
                    else {
                        Object theValue = Array.get(colValue, rowIdx + i);
                        log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() theValue[{}]={}", i, theValue.toString());
                        if (i > 0) stringBuffer.append(", ");
                        stringBuffer.append(theValue);
                    }
                }
            } // end of else {

            return stringBuffer;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            try {
                updateValueInMemory((String) newValue, rowIndex, columnIndex);
            }
            catch (Exception ex) {
                shell.getDisplay().beep();
                showError(ex.getMessage(), shell.getText());
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

    // Custom Row Header renderer to set Row Header based on Index Base
    private class RowHeader implements IDataProvider {

        private int rank;
        private long[] dims;

        private int nrows;

        public RowHeader(IDataProvider bodyDataProvider) {
            this.rank = dataset.getRank();

            if (rank <= 0) {
                try {
                    dataset.init();
                    log.trace("createTable: dataset inited");
                }
                catch (Exception ex) {
                    showError(ex.getMessage(), "createTable:" + shell.getText());
                    dataValue = null;
                    return;
                }

                rank = dataset.getRank();
            }

            this.dims = dataset.getSelectedDims();

            if (rank > 1) {
                this.nrows = (int)dataset.getHeight();
            } else {
                this.nrows = (int) dims[0];
            }
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            return nrows;
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
            return String.valueOf(indexBase + rowIndex);
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            // Should not allow user to set row header titles
        }
    }

    // Context-menu for dealing with region and object references
    private class RefContextMenu extends AbstractUiBindingConfiguration {
        private final Menu contextMenu;

        public RefContextMenu(NatTable table) {
            this.contextMenu = new PopupMenuBuilder(table).build();
        }

        @Override
        public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
            uiBindingRegistry.registerMouseDownBinding(
                    new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON),
                                          new PopupMenuAction(this.contextMenu));
        }
    }

    private class LinePlotOption extends Dialog {

        private Shell             linePlotOptionShell;

        private Button            rowButton, colButton;

        private Combo             rowBox, colBox;

        public static final int   NO_PLOT          = -1;
        public static final int   ROW_PLOT         = 0;
        public static final int   COLUMN_PLOT      = 1;

        private int               nrow, ncol;

        private int               idx_xaxis        = -1, plotType = -1;

        public LinePlotOption(Shell parent, int style, int nrow, int ncol) {
            super(parent, style);

            this.nrow = nrow;
            this.ncol = ncol;
        }

        public void open() {
            Shell parent = getParent();
            linePlotOptionShell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
            linePlotOptionShell.setText("Line Plot Options -- " + dataset.getName());
            linePlotOptionShell.setImage(ViewProperties.getHdfIcon());
            linePlotOptionShell.setLayout(new GridLayout(1, true));

            new Label(linePlotOptionShell, SWT.RIGHT).setText("Select Line Plot Options:");

            Composite content = new Composite(linePlotOptionShell, SWT.BORDER);
            content.setLayout(new GridLayout(3, false));
            content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            new Label(content, SWT.RIGHT).setText(" Series in:");

            colButton = new Button(content, SWT.RADIO);
            colButton.setText("Column");
            colButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
            colButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    colBox.setEnabled(true);
                    rowBox.setEnabled(false);
                }
            });

            rowButton = new Button(content, SWT.RADIO);
            rowButton.setText("Row");
            rowButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
            rowButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    rowBox.setEnabled(true);
                    colBox.setEnabled(false);
                }
            });

            new Label(content, SWT.RIGHT).setText(" For abscissa use:");

            long[] startArray = dataset.getStartDims();
            long[] strideArray = dataset.getStride();
            int[] selectedIndex = dataset.getSelectedIndex();
            int start = (int) startArray[selectedIndex[0]];
            int stride = (int) strideArray[selectedIndex[0]];

            colBox = new Combo(content, SWT.SINGLE);
            GridData colBoxData = new GridData(SWT.FILL, SWT.FILL, true, false);
            colBoxData.minimumWidth = 100;
            colBox.setLayoutData(colBoxData);

            colBox.add("array index");

            for (int i = 0; i < ncol; i++) {
                colBox.add("column " + columnHeaderDataProvider.getDataValue(i, 0));
            }

            rowBox = new Combo(content, SWT.SINGLE);
            GridData rowBoxData = new GridData(SWT.FILL, SWT.FILL, true, false);
            rowBoxData.minimumWidth = 100;
            rowBox.setLayoutData(rowBoxData);

            rowBox.add("array index");

            for (int i = 0; i < nrow; i++) {
                rowBox.add("row " + (start + indexBase + i * stride));
            }


            // Create Ok/Cancel button region
            Composite buttonComposite = new Composite(linePlotOptionShell, SWT.NONE);
            buttonComposite.setLayout(new GridLayout(2, true));
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setText("   &Ok   ");
            okButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (colButton.getSelection()) {
                        idx_xaxis = colBox.getSelectionIndex() - 1;
                        plotType = COLUMN_PLOT;
                    }
                    else {
                        idx_xaxis = rowBox.getSelectionIndex() - 1;
                        plotType = ROW_PLOT;
                    }

                    linePlotOptionShell.dispose();
                }
            });
            GridData gridData = new GridData(SWT.END, SWT.FILL, true, false);
            gridData.widthHint = 70;
            okButton.setLayoutData(gridData);

            Button cancelButton = new Button(buttonComposite, SWT.PUSH);
            cancelButton.setText("&Cancel");
            cancelButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    plotType = NO_PLOT;
                    linePlotOptionShell.dispose();
                }
            });

            gridData = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
            gridData.widthHint = 70;
            cancelButton.setLayoutData(gridData);

            colButton.setSelection(true);
            rowButton.setSelection(false);

            colBox.select(0);
            rowBox.select(0);

            colBox.setEnabled(colButton.getSelection());
            rowBox.setEnabled(rowButton.getSelection());

            linePlotOptionShell.pack();

            linePlotOptionShell.setMinimumSize(linePlotOptionShell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

            Rectangle parentBounds = parent.getBounds();
            Point shellSize = linePlotOptionShell.getSize();
            linePlotOptionShell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                              (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            linePlotOptionShell.open();

            Display display = parent.getDisplay();
            while(!linePlotOptionShell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
        }

        int getXindex ( ) {
            return idx_xaxis;
        }

        int getPlotBy ( ) {
            return plotType;
        }
    }
}
