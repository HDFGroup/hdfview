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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
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
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.action.MouseEditAction;
import org.eclipse.nebula.widgets.nattable.edit.config.DefaultEditConfiguration;
import org.eclipse.nebula.widgets.nattable.edit.editor.TextCellEditor;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupExpandCollapseLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupGroupHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultColumnHeaderLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultColumnHeaderStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultRowHeaderLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultRowHeaderStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.BeveledBorderDecorator;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.LineBorderDecorator;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectAllCommand;
import org.eclipse.nebula.widgets.nattable.selection.event.CellSelectionEvent;
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

import hdf.hdf5lib.H5;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.object.h5.H5Datatype;
import hdf.view.ViewProperties;
import hdf.view.ViewProperties.BITMASK_OP;
import hdf.view.ViewProperties.DATA_VIEW_KEY;

/**
 * TableView displays an HDF dataset as a two-dimensional table.
 *
 * @author Jordan T. Henderson
 * @version 2.4 //
 */
public class DefaultTableView implements TableView {

    private final static org.slf4j.Logger   log       = org.slf4j.LoggerFactory.getLogger(DefaultTableView.class);

    private final Display                   display = Display.getDefault();
    private final Shell                     shell;
    private Font                            curFont;

    // The main HDFView
    private final ViewManager               viewer;

    private NatTable                        table; // The NatTable to display data in

    // The Dataset (Scalar or Compound) to be displayed in the Table
    private Dataset                         dataset;

    /**
     * The value of the dataset.
     */
    private Object                          dataValue;

    private Object                          fillValue               = null;

    private enum ViewType { TABLE, IMAGE, TEXT };
    private    ViewType viewType = ViewType.TABLE;

    /**
     * Numerical data type. B = byte array, S = short array, I = int array, J = long array, F =
     * float array, and D = double array.
     */
    private char                            NT               = ' ';

    private static final int                FLOAT_BUFFER_SIZE       = 524288;
    private static final int                INT_BUFFER_SIZE         = 524288;
    private static final int                SHORT_BUFFER_SIZE       = 1048576;
    private static final int                LONG_BUFFER_SIZE        = 262144;
    private static final int                DOUBLE_BUFFER_SIZE      = 262144;
    private static final int                BYTE_BUFFER_SIZE        = 2097152;

    // Changed to use normalized scientific notation (1 <= coefficient < 10).
    // private final DecimalFormat scientificFormat = new DecimalFormat("###.#####E0#");
    private final DecimalFormat             scientificFormat = new DecimalFormat("0.0###E0###");
    private DecimalFormat                   customFormat     = new DecimalFormat("###.#####");
    private final NumberFormat              normalFormat     = null; // NumberFormat.getInstance();
    private NumberFormat                    numberFormat     = normalFormat;

    // Used for bitmask operations on data
    private BitSet                          bitmask                 = null;
    private BITMASK_OP                      bitmaskOP               = BITMASK_OP.EXTRACT;

    // Keeps track of which frame of data is being displayed
    private Text                            frameField;
    private long                            curFrame = 0;
    private long                            maxFrame = 1;

    private int                             indexBase = 0;

    private int                             fixedDataLength = -1;

    private int                             binaryOrder;

    private boolean                         isReadOnly = false;

    private boolean                         isValueChanged = false;

    private boolean                         isDisplayTypeChar, isDataTransposed;

    private boolean                         isRegRef = false, isObjRef = false;
    private boolean                         showAsHex = false, showAsBin = false;

    // Keep references to the selection and data layers
    private SelectionLayer                  selectionLayer;
    private DataLayer                       dataLayer;

    private IDataProvider                   rowHeaderDataProvider;
    private IDataProvider                   columnHeaderDataProvider;


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
    private Text                            cellValueField;

    // Label to indicate the current cell location.
    private Label                           cellLabel;


    /**
     * Constructs a TableView.
     *
     * @param theView
     *             the main HDFView.
     */
    public DefaultTableView(ViewManager theView) {
        this(theView, null);
    }

    /**
     * Constructs a TableView.
     *
     * @param theView
     *             the main HDFView.
     * @param map
     *             the properties on how to show the data. The map is used to allow applications to
     *          pass properties on how to display the data, such as, transposing data, showing
     *          data as character, applying bitmask, and etc. Predefined keys are listed at
     *          ViewProperties.DATA_VIEW_KEY.
     */
    public DefaultTableView(ViewManager theView, HashMap map) {
        log.trace("DefaultTableView start");
        
        shell = new Shell(display, SWT.SHELL_TRIM);

        shell.setData(this);

        shell.setLayout(new GridLayout(1, true));

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
                
                if (curFont != null) curFont.dispose();
                
                viewer.removeDataView(DefaultTableView.this);
            }
        });
        
        try {
            curFont = new Font(
                    display,
                    ViewProperties.getFontType(),
                    ViewProperties.getFontSize(),
                    SWT.NORMAL);
        }
        catch (Exception ex) {
            curFont = null;
        }
        
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
        if (dataset.getHeight() <= 0 || dataset.getWidth() <= 0 || tsize <= 0) {
            Tools.showError(shell, "Could not open dataset '" + dataset.getName() + "'. Dataset has dimension of size 0.", shell.getText());
            return;
        }

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

        ToolBar bar = createToolbar(shell);
        bar.setSize(shell.getSize().x, 30);
        bar.setLocation(0, 0);

        group = new Group(shell, SWT.SHADOW_ETCHED_OUT);
        group.setFont(curFont);
        group.setText(indexBase + "-based");
        group.setLayout(new GridLayout(1, true));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        SashForm content = new SashForm(group, SWT.VERTICAL);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        content.setSashWidth(10);

        SashForm cellValueComposite = new SashForm(content, SWT.HORIZONTAL);
        cellValueComposite.setSashWidth(8);

        cellLabel = new Label(cellValueComposite, SWT.RIGHT | SWT.BORDER);
        cellLabel.setAlignment(SWT.CENTER);
        cellLabel.setFont(curFont);

        cellValueField = new Text(cellValueComposite, SWT.MULTI | SWT.BORDER | SWT.WRAP);
        cellValueField.setEditable(false);
        cellValueField.setBackground(new Color(display, 255, 255, 240));
        cellValueField.setEnabled(false);
        cellValueField.setFont(curFont);
        
        cellValueComposite.setWeights(new int[] {1, 5});
        
        // Create the NatTable
        if (dataset instanceof CompoundDS) {
            log.trace("createTable((CompoundDS) dataset): dtype.getDatatypeClass()={}", dtype.getDatatypeClass());

            isDataTransposed = false; // Disable transpose for compound dataset
            shell.setImage(ViewProperties.getTableIcon());
            table = createTable(content, (CompoundDS) dataset);
        }
        else { /* if (dataset instanceof ScalarDS) */
            log.trace("createTable((ScalarDS) dataset): dtype.getDatatypeClass()={}", dtype.getDatatypeClass());

            shell.setImage(ViewProperties.getDatasetIcon());
            table = createTable(content, (ScalarDS) dataset);

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

        if (table == null) {
            viewer.showStatus("Creating table failed - " + dataset.getName());
            dataset = null;
            shell.dispose();
            return;
        }

        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        StringBuffer sb = new StringBuffer(hObject.getName());
        sb.append("  at  ");
        sb.append(hObject.getPath());
        sb.append("  [");
        sb.append(dataset.getFileFormat().getName());
        sb.append("  in  ");
        sb.append(dataset.getFileFormat().getParent());
        sb.append("]");
        shell.setText(sb.toString());

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

        content.setWeights(new int[] {1, 12});

        shell.pack();

        int width = 700 + (ViewProperties.getFontSize() - 12) * 15;
        int height = 500 + (ViewProperties.getFontSize() - 12) * 10;
        shell.setSize(width, height);

        viewer.addDataView(this);
        
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }
    
    // Implementing TableView
    @Override
    public NatTable getTable() {
        return table;
    }
    
    // Implementing DataView
    @Override
    public HObject getDataObject() {
        return dataset;
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
    
    @Override
    public int getSelectedRowCount() {
        return selectionLayer.getSelectedRowCount();
    }

    @Override
    public int getSelectedColumnCount() {
        return selectionLayer.getSelectedColumnPositions().length;
    }
    
    /**
     * Creates a NatTable for a Scalar dataset.
     *
     * @param parent
     *          The parent for the NatTable
     * @param theDataset
     *          The Scalar dataset for the NatTable to display
     *
     * @return The newly created NatTable
     */
    private NatTable createTable(Composite parent, ScalarDS theDataset) {
        log.trace("createTable(ScalarDS): start");

        if (theDataset.getRank() <= 0) {
            try {
                theDataset.init();
                log.trace("createTable: dataset inited");
            }
            catch (Exception ex) {
                Tools.showError(shell, ex.getMessage(), "createTable:" + shell.getText());
                dataValue = null;
                return null;
            }
        }

        dataValue = null;
        try {
            dataValue = theDataset.getData();
            if (dataValue == null) {
                Tools.showError(shell, "No data read", "ScalarDS createTable:" + shell.getText());
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

            theDataset.convertFromUnsignedC();
            dataValue = theDataset.getData();

            //TODO: Revise if (Array.getLength(dataValue) <= rows) cols = 1;
        }
        catch (Throwable ex) {
            Tools.showError(shell, ex.getMessage(), "ScalarDS createTable:" + shell.getText());
            dataValue = null;
        }

        if (dataValue == null) {
            return null;
        }

        fillValue = theDataset.getFillValue();
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
        else if ((NT == 'B') && theDataset.getDatatype().getDatatypeClass() == Datatype.CLASS_ARRAY) {
            Datatype baseType = theDataset.getDatatype().getBasetype();
            if (baseType.getDatatypeClass() == Datatype.CLASS_STRING) {
                dataValue = Dataset.byteToString((byte[]) dataValue, (int) baseType.getDatatypeSize());
            }
        }


        // Create body layer
        final ScalarDSDataProvider bodyDataProvider = new ScalarDSDataProvider(theDataset);
        dataLayer = new DataLayer(bodyDataProvider);
        selectionLayer = new SelectionLayer(dataLayer);
        final ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);

        dataLayer.setDefaultColumnWidth(80);
        
        log.trace("createTable: rows={} : cols={}", bodyDataProvider.getRowCount(), bodyDataProvider.getColumnCount());


        // Create the Column Header layer
        columnHeaderDataProvider = new ScalarDSColumnHeaderDataProvider(theDataset);
        ColumnHeaderLayer columnHeaderLayer = new ColumnHeader(new DataLayer(columnHeaderDataProvider), viewportLayer, selectionLayer);


        // Create the Row Header layer
        rowHeaderDataProvider = new RowHeaderDataProvider(theDataset);
        
        // Try to adapt row height to current font
        int defaultRowHeight = curFont == null ? 20 : (2 * curFont.getFontData()[0].getHeight());
        
        DataLayer baseLayer = new DataLayer(rowHeaderDataProvider, 40, defaultRowHeight);
        RowHeaderLayer rowHeaderLayer = new RowHeader(baseLayer, viewportLayer, selectionLayer);


        // Create the Corner layer
        ILayer cornerLayer = new CornerLayer(new DataLayer(
                new DefaultCornerDataProvider(columnHeaderDataProvider,
                        rowHeaderDataProvider)), rowHeaderLayer,
                columnHeaderLayer);


        // Create the Grid layer
        GridLayer gridLayer = new EditingGridLayer(viewportLayer, columnHeaderLayer, rowHeaderLayer, cornerLayer);


        final NatTable natTable = new NatTable(parent, gridLayer, false);
        natTable.addConfiguration(new DefaultNatTableStyleConfiguration());
        natTable.addConfiguration(new CellConfiguration(theDataset));
        natTable.addLayerListener(new ScalarDSCellSelectionListener());

        natTable.configure();

        log.trace("createTable(ScalarDS): finish");

        return natTable;
    }
    
    /**
     * Creates a NatTable for a Compound dataset
     *
     * @param parent
     *          The parent for the NatTable
     * @param theDataset
     *          The Compound dataset for the NatTable to display
     *
     * @return The newly created NatTable
     */
    private NatTable createTable(Composite parent, CompoundDS theDataset) {
        log.trace("createTable: CompoundDS start");

        if (theDataset.getRank() <= 0) theDataset.init();

        // use lazy convert for large number of strings
        if (theDataset.getHeight() > 10000) {
            theDataset.setConvertByteToString(false);
        }

        dataValue = null;
        try {
            dataValue = theDataset.getData();
        }
        catch (Throwable ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, ex.getMessage(), "TableView " + shell.getText());
            dataValue = null;
        }

        if ((dataValue == null) || !(dataValue instanceof List)) {
            return null;
        }


        // Create body layer
        final ColumnGroupModel columnGroupModel = new ColumnGroupModel();
        final ColumnGroupModel secondLevelGroupModel = new ColumnGroupModel();
        
        final IDataProvider bodyDataProvider = new CompoundDSDataProvider(theDataset);
        dataLayer = new DataLayer(bodyDataProvider);
        final ColumnGroupExpandCollapseLayer expandCollapseLayer =
            new ColumnGroupExpandCollapseLayer(dataLayer, secondLevelGroupModel, columnGroupModel);
        selectionLayer = new SelectionLayer(expandCollapseLayer);
        final ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);

        dataLayer.setDefaultColumnWidth(80);


        // Create the Column Header layer
        columnHeaderDataProvider = new CompoundDSColumnHeaderDataProvider(theDataset);
        ColumnHeaderLayer columnHeaderLayer = new ColumnHeader(new DataLayer(
                columnHeaderDataProvider), viewportLayer, selectionLayer);


        // Set up column grouping
        ColumnGroupHeaderLayer columnGroupHeaderLayer = new ColumnGroupHeaderLayer(columnHeaderLayer, selectionLayer, columnGroupModel);
        CompoundDSNestedColumnHeaderLayer nestedColumnGroupHeaderLayer =
                new CompoundDSNestedColumnHeaderLayer(columnGroupHeaderLayer, selectionLayer, secondLevelGroupModel);


        // Create the Row Header layer
        rowHeaderDataProvider = new RowHeaderDataProvider(theDataset);
        
        // Try to adapt row height to current font
        int defaultRowHeight = curFont == null ? 20 : (2 * curFont.getFontData()[0].getHeight());
        
        DataLayer baseLayer = new DataLayer(rowHeaderDataProvider, 40, defaultRowHeight);
        RowHeaderLayer rowHeaderLayer = new RowHeader(baseLayer, viewportLayer, selectionLayer);


        // Create the Corner Layer
        ILayer cornerLayer = new CornerLayer(new DataLayer(
                new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
                rowHeaderLayer,
                nestedColumnGroupHeaderLayer);


        // Create the Grid Layer
        GridLayer gridLayer = new EditingGridLayer(viewportLayer, nestedColumnGroupHeaderLayer, rowHeaderLayer, cornerLayer);


        final NatTable natTable = new NatTable(parent, gridLayer, false);
        natTable.addConfiguration(new DefaultNatTableStyleConfiguration());
        natTable.addConfiguration(new CellConfiguration(theDataset));
        natTable.addLayerListener(new CompoundDSCellSelectionListener());

        natTable.configure();

        log.trace("createTable(CompoundDS): finish");

        return natTable;
    }
    
    /**
     * Creates the menubar for the NatTable.
     */
    private Menu createMenuBar() {
        Menu menuBar = new Menu(shell, SWT.BAR);
        boolean isEditable = !isReadOnly;

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
                    Tools.showError(shell, ex.getMessage(), shell.getText());
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
                        Tools.showError(shell, ex.getMessage(), shell.getText());
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
                        Tools.showError(shell, ex.getMessage(), shell.getText());
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
                        Tools.showError(shell, ex.getMessage(), shell.getText());
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

                DefaultFileFilter filter = DefaultFileFilter.getFileFilterText();
                fchooser.setFilterExtensions(new String[] {"*.*", filter.getExtensions()});
                fchooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
                fchooser.setFilterIndex(1);

                if (fchooser.open() == null) return;

                File chosenFile = new File(fchooser.getFilterPath() + File.separator + fchooser.getFileName());
                if (!chosenFile.exists()) {
                    Tools.showError(shell, "File " + chosenFile.getName() + " does not exist.", "Import Data from Text File");
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
                        Tools.showError(shell, ex.getMessage(), shell.getText());
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
                        Tools.showError(shell, ex.getMessage(), shell.getText());
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
                        Tools.showError(shell, ex.getMessage(), shell.getText());
                    }
                }
            });

            importAsBinaryMenuItem.setMenu(importFromBinaryMenu);
        }

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Copy");
        item.setAccelerator(SWT.CTRL | 'C');
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                copyData();
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Paste");
        item.setAccelerator(SWT.CTRL | 'V');
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                pasteData();
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Copy to New Dataset");
        item.setEnabled(isEditable && (dataset instanceof ScalarDS));
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if ((selectionLayer.getSelectedColumnPositions().length <= 0) || (selectionLayer.getSelectedRowCount() <= 0)) {
                    MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
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
        item.setAccelerator(SWT.CTRL | 'U');
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    updateValueInFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, ex.getMessage(), shell.getText());
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Select All");
        item.setAccelerator(SWT.CTRL | 'A');
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    selectAll();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, ex.getMessage(), shell.getText());
                }
            }
        });

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
                            Tools.showError(shell, "Please select one column at a time for compound dataset.", shell.getText());
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
                    Tools.showError(shell, ex.getMessage(), shell.getText());
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
                    Tools.showError(shell, ex.getMessage(), shell.getText());
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
                    }
                    else {
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
                    }
                    else {
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
                shell.dispose();
            }
        });

        return menuBar;
    }

    private ToolBar createToolbar(final Shell shell) {
        ToolBar toolbar = new ToolBar(shell, SWT.HORIZONTAL | SWT.RIGHT | SWT.BORDER);
        toolbar.setFont(curFont);
        toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // Chart button
        ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        item.setImage(ViewProperties.getChartIcon());
        item.setToolTipText("Line Plot");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLineplot();
            }
        });

        if (dataset.getRank() > 2) {
            new ToolItem(toolbar, SWT.SEPARATOR).setWidth(20);

            // First frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getFirstIcon());
            item.setToolTipText("First Page");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    firstPage();
                }
            });

            // Previous frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getPreviousIcon());
            item.setToolTipText("Previous Page");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    previousPage();
                }
            });

            ToolItem separator = new ToolItem(toolbar, SWT.SEPARATOR);

            frameField = new Text(toolbar, SWT.SINGLE | SWT.BORDER | SWT.CENTER);
            frameField.setFont(curFont);
            frameField.setText(String.valueOf(curFrame));
            frameField.addTraverseListener(new TraverseListener() {
                public void keyTraversed(TraverseEvent e) {
                    if (e.detail == SWT.TRAVERSE_RETURN) {
                        try {
                            int page = 0;

                            try {
                                page = Integer.parseInt(frameField.getText()
                                        .trim()) - indexBase;
                            } catch (Exception ex) {
                                page = -1;
                            }

                            gotoPage(page);
                        }
                        catch (Exception ex) {
                            log.debug("Page change failure: ", ex);
                        }
                    }
                }
            });

            frameField.pack();

            separator.setWidth(frameField.getSize().x + 30);
            separator.setControl(frameField);

            separator = new ToolItem(toolbar, SWT.SEPARATOR);

            Text maxFrameText = new Text(toolbar, SWT.SINGLE | SWT.BORDER | SWT.CENTER);
            maxFrameText.setFont(curFont);
            maxFrameText.setText(String.valueOf(maxFrame - 1));
            maxFrameText.setEditable(false);
            maxFrameText.setEnabled(false);

            maxFrameText.pack();

            separator.setWidth(maxFrameText.getSize().x + 30);
            separator.setControl(maxFrameText);

            new ToolItem(toolbar, SWT.SEPARATOR).setWidth(10);

            // Next frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getNextIcon());
            item.setToolTipText("Next Page");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    nextPage();
                }
            });

            // Last frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getLastIcon());
            item.setToolTipText("Last Page");
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    lastPage();
                }
            });
        }

        return toolbar;
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
                    Tools.showError(shell, "No data selected.", shell.getText());
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
                    Tools.showError(shell, "No data selected.", shell.getText());
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
                    Tools.showError(shell, "No data selected.", shell.getText());
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
            Tools.showError(shell, "Frame number must be between " + indexBase + " and " + (dims[selectedIndex[2]] - 1 + indexBase), shell.getText());
            return;
        }

        start[selectedIndex[2]] = idx;
        curFrame = idx + indexBase;
        frameField.setText(String.valueOf(curFrame));

        dataset.clearData();

        shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

        try {
            dataValue = dataset.getData();
            if (dataset instanceof ScalarDS) {
                ((ScalarDS) dataset).convertFromUnsignedC();
                dataValue = dataset.getData();
            }
        }
        catch (Exception ex) {
            dataValue = null;
            Tools.showError(shell, ex.getMessage(), shell.getText());
            return;
        }
        finally {
            shell.setCursor(null);
        }

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
            Tools.showError(shell, ex.getMessage(), shell.getText());
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
     *
     * @throws Exception if a failure occurred
     */
    private void updateValueInMemory(String cellValue, int row, int col) throws Exception {
        if (cellValue == null) return;
        
        log.trace("DefaultTableView: updateValueInMemory()");
        
        // No need to update if values are the same
        if (cellValue.equals((String) dataLayer.getDataValue(col, row).toString())) return;

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
            Tools.showError(shell, "Copying data to system clipboard failed. \nUse \"export/import data\" for copying/pasting large data.", shell.getText());
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
            Tools.showError(shell, ex.getMessage(), shell.getText());
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
     *
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
            Tools.showError(shell, "Unsupported data type.", shell.getText());
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

        return selectedData;
    }

    /**
     * Returns the selected data values of the CompoundDS
     */
    private Object getSelectedCompoundData ( ) {
        Object selectedData = null;

        int cols = this.getSelectedColumnCount();
        int rows = this.getSelectedRowCount();

        if ((cols <= 0) || (rows <= 0)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "No data is selected.", shell.getText());
            return null;
        }
        
        Object colData = null;
        try {
            colData = ((List<?>) dataset.getData()).get(selectionLayer.getSelectedColumnPositions()[0]);
        }
        catch (Exception ex) {
            log.debug("getSelectedCompoundData(): ", ex);
            return null;
        }

        int size = Array.getLength(colData);
        String cName = colData.getClass().getName();
        int cIndex = cName.lastIndexOf("[");
        char nt = ' ';
        if (cIndex >= 0) {
            nt = cName.charAt(cIndex + 1);
        }
        log.trace("getSelectedCompoundData(): size={} cName={} nt={}", size, cName, nt);

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
            Tools.showError(shell, "Unsupported data type.", shell.getText());
            return null;
        }
        log.trace("getSelectedCompoundData(): selectedData={}", selectedData);

        System.arraycopy(colData, 0, selectedData, 0, size);

        return selectedData;
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
            Tools.showError(shell, "Please select one column at a time for math conversion"
                    + "for compound dataset.", shell.getText());
            return;
        }

        Object theData = getSelectedData();
        if (theData == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "No data is selected.", shell.getText());
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

    /**
     * Selects all rows, columns, and cells in the table.
     */
    private void selectAll() {
        table.doCommand(new SelectAllCommand());
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
            Tools.showError(shell, ex.getMessage(), "Object Reference:" + shell.getText());
            data = null;
        }

        if (data == null) return;

        DataView dataView = null;
        HashMap map = new HashMap(1);
        map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, dset_copy);
        switch (viewType) {
            case TEXT:
                dataView = new DefaultTextView(viewer, map);
                break;
            case IMAGE:
                dataView = new DefaultImageView(viewer, map);
                break;
            default:
                dataView = new DefaultTableView(viewer, map);
                break;
        }

        if (dataView != null) {
            viewer.addDataView((DataView) dataView);
        }
    }

    /**
     * Display data pointed by region references. Data of each region is shown in a separate
     * spreadsheet. The reg. ref. information is stored in strings of the format below:
     * <p>
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
                Tools.showError(shell, ex.getMessage(), "Region Reference:" + shell.getText());
            }

            DataView dataView = null;
            HashMap map = new HashMap(1);
            map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, dset_copy);
            switch (viewType) {
                case TEXT:
                    dataView = new DefaultTextView(viewer, map);
                    break;
                case IMAGE:
                    dataView = new DefaultImageView(viewer, map);
                    break;
                default:
                    dataView = new DefaultTableView(viewer, map);
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
     *
     * @throws Exception if a failure occurred
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
            Tools.showError(shell, "Number of data points < " + morder + ".", shell.getText());
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
     *
     * @param fname  the file to import text from
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
        }
        else {
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
                    Tools.showError(shell, ex.getMessage(), shell.getText());

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
            }
            else {
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

        DefaultFileFilter filter = DefaultFileFilter.getFileFilterBinary();
        fchooser.setFilterExtensions(new String[] {"*.*", filter.getExtensions()});
        fchooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
        fchooser.setFilterIndex(1);

        if (fchooser.open() == null) return;

        File chosenFile = new File(fchooser.getFilterPath() + File.separator + fchooser.getFileName());
        if (!chosenFile.exists()) {
            Tools.showError(shell, "File " + chosenFile.getName() + " does not exist.", "Import Data from Binary File");
            return;
        }

        MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(shell.getText());
        confirm.setMessage("Do you want to paste selected data?");
        if (confirm.open() == SWT.NO) return;

        getBinaryDataFromFile(chosenFile.getAbsolutePath());
    }

    /** Reads data from a binary file into a buffer and updates table.
     *
     * @param filename the file to read binary data from
     */
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

    /** Save data as text.
     *
     * @throws Exception if a failure occurred
     */
    private void saveAsText() throws Exception {
        FileDialog fchooser = new FileDialog(shell, SWT.SAVE);
        fchooser.setFilterPath(dataset.getFile());

        DefaultFileFilter filter = DefaultFileFilter.getFileFilterText();
        fchooser.setFilterExtensions(new String[] {"*.*", filter.getExtensions()});
        fchooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
        fchooser.setFilterIndex(1);

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
                        Tools.showError(shell, "Unable to save data to file \"" + fname + "\". \nThe file is being used.", shell.getText());
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

    /** Save data as binary.
     *
     * @throws Exception if a failure occurred
     */
    private void saveAsBinary() throws Exception {
        FileDialog fchooser = new FileDialog(shell, SWT.SAVE);
        fchooser.setFilterPath(dataset.getFile());

        DefaultFileFilter filter = DefaultFileFilter.getFileFilterBinary();
        fchooser.setFilterExtensions(new String[] {"*.*", filter.getExtensions()});
        fchooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
        fchooser.setFilterIndex(1);

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
                        Tools.showError(shell, "Unable to save data to file \"" + fname + "\". \nThe file is being used.", shell.getText());
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
            Tools.showError(shell, "Select rows/columns to draw line plot.", shell.getText());
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
                MessageBox warning = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
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
                        value = Double.parseDouble(selectionLayer.getDataValueByPosition(cols[j], rows[i]).toString());
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
                        value = Double.parseDouble(selectionLayer.getDataValueByPosition(cols[j], xIndex).toString());
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
                MessageBox warning = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
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
                        value = Double.parseDouble(selectionLayer.getDataValueByPosition(cols[j], rows[i]).toString());
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
                        value = Double.parseDouble(selectionLayer.getDataValueByPosition(xIndex, rows[j]).toString());
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
            Tools.showError(shell, "Cannot show line plot for the selected data. \n" + "Please check the data range: ("
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
    
    // Allow editing under specific conditions
    private IEditableRule getScalarDSEditingRule(final ScalarDS theDataset) {
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
    
    // Only Allow editing of CompoundDS if not in read-only mode
    private IEditableRule getCompoundDSEditingRule(final CompoundDS theDataset) {
        return new EditableRule() {
            @Override
            public boolean isEditable(int columnIndex, int rowIndex) {
                return !isReadOnly;
            }
        };
    }
    
    /**
     * Provides the NatTable with data from a Scalar Dataset for each cell.
     */
    private class ScalarDSDataProvider implements IDataProvider {
        
        private final StringBuffer stringBuffer;
        
        private final Datatype     dtype;
        private final Datatype     btype;
        
        private final long         typeSize;
        
        private final long[]       dims;
        
        private final boolean      isArray;
        private final boolean      isStr;
        private final boolean      isInt;
        private final boolean      isUINT64;
        
        private Object             theValue;

        private final boolean      isNaturalOrder;

        private final long         rowCount;
        private final long         colCount;

        public ScalarDSDataProvider(ScalarDS theDataset) {
            stringBuffer = new StringBuffer();
            
            dtype = theDataset.getDatatype();
            btype = dtype.getBasetype();
            
            dims = theDataset.getSelectedDims();
            
            typeSize = dtype.getDatatypeSize();
            
            isArray = (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY);
            isStr = (NT == 'L');
            isInt = (NT == 'B' || NT == 'S' || NT == 'I' || NT == 'J');
            isUINT64 = (dtype.isUnsigned() && (NT == 'J'));
            
            isNaturalOrder = (theDataset.getRank() == 1 || (theDataset.getSelectedIndex()[0] < theDataset
                    .getSelectedIndex()[1]));
            
            if (theDataset.getRank() > 1) {
                rowCount = theDataset.getHeight();
                colCount = theDataset.getWidth();
            }
            else {
                rowCount = (int) dims[0];
                colCount = 1;
            }
        }

        @Override
        public Object getDataValue(int columnIndex, int rowIndex) {
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
                    theValue = Array.get(dataValue, (int) index);
                    return theValue;
                }

                if (isUINT64) {
                    theValue = Array.get(dataValue, (int) index);
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
                    theValue = Array.get(dataValue, (int) index);
                    theValue = Tools.toHexString(Long.valueOf(theValue.toString()), (int) typeSize);
                }
                else if (showAsBin && isInt) {
                    theValue = Array.get(dataValue, (int) index);
                    theValue = Tools.toBinaryString(Long.valueOf(theValue.toString()), (int) typeSize);
                    // theValue =
                    // Long.toBinaryString(Long.valueOf(theValue.toString()));
                }
                else if (numberFormat != null) {
                    // show in scientific format
                    theValue = Array.get(dataValue, (int) index);
                    theValue = numberFormat.format(theValue);
                }
                else {
                    theValue = Array.get(dataValue, (int) index);
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
                Tools.showError(shell, "Failed to update value at "
                        + "(" + rowIndex + ", " + columnIndex + ") to '" + newValue.toString() + "'", shell.getText());
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
    
    /**
     * Provides the NatTable with data from a Compound Dataset for each cell.
     */
    private class CompoundDSDataProvider implements IDataProvider {
        
        private final StringBuffer      stringBuffer;
        
        private final Datatype          types[];
        
        private final int               orders[];
        private final int               nFields;
        private final int               nRows;
        private final int               nCols;
        private final int               nSubColumns;

        public CompoundDSDataProvider(CompoundDS theDataset) {
            stringBuffer = new StringBuffer();
            
            types = theDataset.getSelectedMemberTypes();
            
            orders = theDataset.getSelectedMemberOrders();
            nFields = ((List<?>) dataValue).size();
            nRows = (int) theDataset.getHeight();
            nCols = (int) (theDataset.getWidth() * theDataset.getSelectedMemberCount());
            nSubColumns = (nFields > 0) ? getColumnCount() / nFields : 0;
        }

        @Override
        public Object getDataValue(int col, int row) {
            int fieldIdx = col;
            int rowIdx = row;
            char CNT = ' ';
            boolean CshowAsHex = false;
            boolean CshowAsBin = false;
            log.trace("CompoundDS:CompoundDSDataProvider:getDataValue({},{}) start", row, col);

            if (nSubColumns > 1) { // multi-dimension compound dataset
                int colIdx = col / nFields;
                fieldIdx = col - colIdx * nFields;
                // BUG 573: rowIdx = row * orders[fieldIdx] + colIdx * nRows
                // * orders[fieldIdx];
                rowIdx = row * orders[fieldIdx] * nSubColumns + colIdx * orders[fieldIdx];
                log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() row={} orders[{}]={} nSubColumns={} colIdx={}", row, fieldIdx, orders[fieldIdx], nSubColumns, colIdx);
            }
            else {
                rowIdx = row * orders[fieldIdx];
                log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() row={} orders[{}]={}", row, fieldIdx, orders[fieldIdx]);
            }
            log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() rowIdx={}", rowIdx);

            Object colValue = ((List<?>) dataValue).get(fieldIdx);
            if (colValue == null) {
                return "Null";
            }

            stringBuffer.setLength(0); // clear the old string
            Datatype dtype = types[fieldIdx];
            boolean isString = (dtype.getDatatypeClass() == Datatype.CLASS_STRING);
            boolean isArray = (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY);
            if (isArray) {
                dtype = types[fieldIdx].getBasetype();
                isString = (dtype.getDatatypeClass() == Datatype.CLASS_STRING);
                log.trace("**CompoundDS:CompoundDSDataProvider:getDataValue(): isArray={} isString={}", isArray, isString);
                
                if (dtype.getDatatypeClass() == Datatype.CLASS_VLEN) {
                    // Only support variable length strings
                    if (!(dtype.getDatatypeClass() == Datatype.CLASS_STRING)) {
                        int arraylen = (int) types[fieldIdx].getDatatypeSize();
                        log.trace("**CompoundDS:CompoundDSDataProvider:getDataValue(): isArray={} of {} istype={}", isArray, arraylen, dtype);
                        String str = new String( "*unsupported*");
                        stringBuffer.append(str.trim());
                        return stringBuffer;
                    }
                }
            }
            log.trace("CompoundDS:CompoundDSDataProvider:getDataValue(): isString={} getBasetype()={}", isString, types[fieldIdx].getDatatypeClass());
            if (isString && ((colValue instanceof byte[]) || isArray)) {
                // strings
                int strlen = (int) dtype.getDatatypeSize();
                int arraylen = strlen;
                if(isArray) {
                    arraylen = (int) types[fieldIdx].getDatatypeSize();
                }
                log.trace("**CompoundDS:CompoundDSDataProvider:getDataValue(): isArray={} of {} isString={} of {}", isArray, arraylen, isString, strlen);
                int arraycnt = arraylen / strlen;
                for (int loopidx = 0; loopidx < arraycnt; loopidx++) {
                    if(isArray && loopidx > 0) {
                        stringBuffer.append(", ");
                    }
                    String str = new String(((byte[]) colValue), rowIdx * strlen, strlen);
                    int idx = str.indexOf('\0');
                    if (idx > 0) {
                        str = str.substring(0, idx);
                    }
                    stringBuffer.append(str.trim());
                }
            }
            else if (isArray && dtype.getDatatypeClass() == Datatype.CLASS_COMPOUND) {
                for (int i = 0; i < orders[fieldIdx]; i++) {
                    try {
                        int numberOfMembers = dtype.getCompoundMemberNames().size();
                        Object field_data = null;

                        try {
                            field_data = Array.get(colValue, rowIdx + i);
                        }
                        catch (Exception ex) {
                            log.debug("CompoundDS:CompoundDSDataProvider:getDataValue(): could not retrieve field_data: ", ex);
                        }

                        if (i > 0) stringBuffer.append(", ");
                        stringBuffer.append("[ ");

                        for (int j = 0; j < numberOfMembers; j++) {
                            Object theValue = null;
                            
                            try {
                                theValue = Array.get(field_data, j);
                                log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() theValue[{}]={}", j, theValue.toString());
                            }
                            catch (Exception ex) {
                                theValue = "*unsupported*";
                            }
                            
                            if (j > 0) stringBuffer.append(", ");
                            stringBuffer.append(theValue);
                        }

                        stringBuffer.append(" ]");
                    }
                    catch (Exception ex) {
                        log.trace("CompoundDS:CompoundDSDataProvider:getDataValue(): ", ex);
                    }
                }

                return stringBuffer;
            }
            else {
                // numerical values
                String cName = colValue.getClass().getName();
                int cIndex = cName.lastIndexOf("[");
                if (cIndex >= 0) {
                    CNT = cName.charAt(cIndex + 1);
                }
                log.trace("CompoundDS:CompoundDSDataProvider:getDataValue(): cName={} CNT={}", cName, CNT);

                boolean isUINT64 = false;
                boolean isInt = (CNT == 'B' || CNT == 'S' || CNT == 'I' || CNT == 'J');
                boolean isEnum = dtype.getDatatypeClass() == Datatype.CLASS_ENUM;
                int typeSize = (int) dtype.getDatatypeSize();
                int classType = dtype.getDatatypeClass();

                if ((classType == Datatype.CLASS_BITFIELD) || (classType == Datatype.CLASS_OPAQUE)) {
                    CshowAsHex = true;
                    log.trace("CompoundDS:CompoundDSDataProvider:getValueAt() class={} (BITFIELD or OPAQUE)", classType);
                }
                if (dtype.isUnsigned()) {
                    if (cIndex >= 0) {
                        isUINT64 = (cName.charAt(cIndex + 1) == 'J');
                    }
                }
                log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() isUINT64={} isInt={} CshowAsHex={} typeSize={}", isUINT64, isInt, CshowAsHex, typeSize);

                if (isEnum) {
                    String[] outValues = new String[orders[fieldIdx]];
                    
                    try {
                        H5Datatype.convertEnumValueToName(dtype.toNative(), colValue, outValues);
                    } catch (HDF5Exception ex) {
                        log.trace("CompoundDS:CompoundDSDataProvider:getDataValue(): Could not convert enum values to names: ex");
                        return stringBuffer;
                    }
                    
                    for (int i = 0; i < orders[fieldIdx]; i++) {
                        if (i > 0) stringBuffer.append(", ");
                        stringBuffer.append(outValues[i]);
                    }
                }
                else {
                    for (int i = 0; i < orders[fieldIdx]; i++) {
                        if (isUINT64) {
                            Object theValue = Array.get(colValue, rowIdx + i);
                            log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() theValue[{}]={}", i, theValue.toString());
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
                            log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() theValue[{}]={}", i, theValue.toString());
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
                                log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() hexChars[{}]={}", x, hexChars);
                            }
                        }
                        else if (showAsBin && isInt) {
                            Object theValue = Array.get(colValue, rowIdx + typeSize * i);
                            log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() theValue[{}]={}", i, theValue.toString());
                            theValue = Tools.toBinaryString(Long.valueOf(theValue.toString()), typeSize);
                            if (i > 0) stringBuffer.append(", ");
                            stringBuffer.append(theValue);
                        }
                        else if (numberFormat != null) {
                            // show in scientific format
                            Object theValue = Array.get(colValue, rowIdx + i);
                            log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() theValue[{}]={}", i, theValue.toString());
                            theValue = numberFormat.format(theValue);
                            if (i > 0) stringBuffer.append(", ");
                            stringBuffer.append(theValue);
                        }
                        else {
                            Object theValue = Array.get(colValue, rowIdx + i);
                            log.trace("CompoundDS:CompoundDSDataProvider:getDataValue() theValue[{}]={}", i, theValue.toString());
                            if (i > 0) stringBuffer.append(", ");
                            stringBuffer.append(theValue);
                        }
                    }
                } // end of else {
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
                Tools.showError(shell, "Failed to update value at "
                        + "(" + rowIndex + ", " + columnIndex + ") to '" + newValue.toString() + "'", shell.getText());
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
    
    /**
     * Custom Row Header data provider to set row indices based on Index Base
     * for both Scalar Datasets and Compound Datasets.
     */
    private class RowHeaderDataProvider implements IDataProvider {
        
        private final int         rank;
        private final long[]      dims;
        private final long[]      startArray;
        private final long[]      strideArray;
        private final int[]       selectedIndex;
        
        private final int         start;
        private final int         stride;

        private final int         nrows;

        public RowHeaderDataProvider(Dataset theDataset) {
            this.rank = theDataset.getRank();
            this.dims = theDataset.getSelectedDims();
            this.startArray = theDataset.getStartDims();
            this.strideArray = theDataset.getStride();
            this.selectedIndex = theDataset.getSelectedIndex();

            if (rank > 1) {
                this.nrows = (int) theDataset.getHeight();
            }
            else {
                this.nrows = (int) dims[0];
            }
            
            start = (int) startArray[selectedIndex[0]];
            stride = (int) strideArray[selectedIndex[0]];
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
            return String.valueOf(start + indexBase + (rowIndex * stride));
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
            return;
        }
    }
    
    /**
     * Custom Column Header data provider to set column indices based on Index Base
     * for Scalar Datasets.
     */
    private class ScalarDSColumnHeaderDataProvider implements IDataProvider {
        
        private final String    columnNames[];
        
        private final int       rank;
        
        private final long[]    startArray;
        private final long[]    strideArray;
        private final int[]     selectedIndex;
        
        private final int       ncols;
        
        public ScalarDSColumnHeaderDataProvider(ScalarDS theDataset) {
            rank = theDataset.getRank();
            
            startArray = theDataset.getStartDims();
            strideArray = theDataset.getStride();
            selectedIndex = theDataset.getSelectedIndex();
            
            if (rank > 1) {
                ncols = (int) theDataset.getWidth();
                
                int start = (int) startArray[selectedIndex[1]];
                int stride = (int) strideArray[selectedIndex[1]];
                
                columnNames = new String[ncols];
                
                for (int i = 0; i < ncols; i++) {
                    columnNames[i] = String.valueOf(start + indexBase + i * stride);
                }
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
            return;
        }
    }
    
    /**
     * Custom Column Header data provider to set column names based on
     * selected members for Compound Datasets.
     */
    private class CompoundDSColumnHeaderDataProvider implements IDataProvider {
        
        private final String[]  allColumnNames;
        private String[]        columnLabels;
        
        private int             ncols;
        private final int       numGroups;
        private final int       groupSize;
        
        public CompoundDSColumnHeaderDataProvider(CompoundDS theDataset) {
            int datasetWidth = (int) theDataset.getWidth();
            Datatype[] types = theDataset.getSelectedMemberTypes();
            groupSize = theDataset.getSelectedMemberCount();
            numGroups = (datasetWidth * groupSize) / groupSize;
            ncols = groupSize * numGroups;
            
            final String[] columnNames = new String[groupSize];
            
            int idx = 0;
            String[] columnNamesAll = theDataset.getMemberNames();
            for (int i = 0; i < columnNamesAll.length; i++) {
                if (theDataset.isMemberSelected(i)) {
                    if (types[i].getDatatypeClass() == Datatype.CLASS_ARRAY) {
                        Datatype baseType = types[i].getBasetype();
                        
                        if (baseType.getDatatypeClass() == Datatype.CLASS_COMPOUND) {
                            List<String> memberNames = baseType.getCompoundMemberNames();
                            
                            columnNames[idx] = columnNamesAll[i];
                            columnNames[idx] = columnNames[idx].replaceAll(CompoundDS.separator, "->");
                            
                            columnNames[idx] += "\n\n[ ";
                            
                            for (int j = 0; j < memberNames.size(); j++) {
                                columnNames[idx] += memberNames.get(j);
                                if (j < memberNames.size() - 1) columnNames[idx] += ", ";
                            }
                            
                            columnNames[idx] += " ]";
                            
                            idx++;
                            continue;
                        }
                    }
                    
                    columnNames[idx] = columnNamesAll[i];
                    columnNames[idx] = columnNames[idx].replaceAll(CompoundDS.separator, "->");
                    idx++;
                }
            }
            
            String[] subColumnNames = columnNames;
            columnLabels = columnNames;
            if (datasetWidth > 1) {
                // multi-dimension compound dataset
                subColumnNames = new String[datasetWidth * columnNames.length];
                columnLabels = new String[datasetWidth * columnNames.length];
                int halfIdx = columnNames.length / 2;
                for (int i = 0; i < datasetWidth; i++) {
                    for (int j = 0; j < columnNames.length; j++) {
                        // display column index only once, in the middle of the
                        // compound fields
                        if (j == halfIdx) {
                            subColumnNames[i * columnNames.length + j] = (i + indexBase) + "\n " + columnNames[j];
                        }
                        else {
                            subColumnNames[i * columnNames.length + j] = " \n " + columnNames[j];
                        }

                        // This column's name is whatever follows the last nesting character '->'
                        int nestingPosition = columnNames[j].lastIndexOf("->");
                        if (nestingPosition != -1) {
                            columnLabels[i * columnNames.length + j] = " \n " + columnNames[j].substring(nestingPosition + 2);
                        }
                        else {
                            columnLabels[i * columnNames.length + j] = " \n " + columnNames[j];
                        }
                    }
                }
            }
            
            allColumnNames = subColumnNames;
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
            return columnLabels[columnIndex];
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
        public CompoundDSNestedColumnHeaderLayer(ColumnGroupHeaderLayer columnGroupHeaderLayer, SelectionLayer selectionLayer, ColumnGroupModel columnGroupModel) {
            super(columnGroupHeaderLayer, selectionLayer, columnGroupModel);
            
            final String[] allColumnNames = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).allColumnNames;
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

                    if(groupTitleStartPosition >= 0) {
                        columnGroupHeaderLayer.addColumnsIndexesToGroup("" +
                                allColumnNames[i].substring(groupTitleStartPosition, nestingPosition) +
                                "{" + columnGroupName + "}", i);
                    }
                    else {
                        columnGroupHeaderLayer.addColumnsIndexesToGroup("" +
                                allColumnNames[i].substring(0, nestingPosition) + "{" + columnGroupName + "}", i);
                    }
                }
            }
        }
    }
    
    /**
     * An implementation of the table's Column Header which adapts to the current font.
     */
    private class ColumnHeader extends ColumnHeaderLayer {
        public ColumnHeader(IUniqueIndexLayer baseLayer, ILayer horizontalLayerDependency, SelectionLayer selectionLayer) {
            super(baseLayer, horizontalLayerDependency, selectionLayer);
            
            this.addConfiguration(new DefaultColumnHeaderLayerConfiguration() {
                @Override
                public void addColumnHeaderStyleConfig() {
                    this.addConfiguration(new DefaultColumnHeaderStyleConfiguration() {
                        {
                            this.cellPainter = new BeveledBorderDecorator(new TextPainter(false, true, 2, true));
                            this.bgColor = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
                            this.font = (curFont == null) ? Display.getDefault().getSystemFont() : curFont;
                        }
                    });
                }
            });
        }
    }
    
    /**
     * An implementation of the table's Row Header which adapts to the current font.
     */
    private class RowHeader extends RowHeaderLayer {
        public RowHeader(IUniqueIndexLayer baseLayer, ILayer verticalLayerDependency, SelectionLayer selectionLayer) {
            super(baseLayer, verticalLayerDependency, selectionLayer);
            
            this.addConfiguration(new DefaultRowHeaderLayerConfiguration() {
                @Override
                public void addRowHeaderStyleConfig() {
                    this.addConfiguration(new DefaultRowHeaderStyleConfiguration() {
                        {
                            this.cellPainter = new LineBorderDecorator(new TextPainter(false, true, 2, true));
                            this.bgColor = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
                            this.font = (curFont == null) ? Display.getDefault().getSystemFont() : curFont;
                        }
                    });
                }
            });
        }
    }
    
    /**
     * An implementation of a GridLayer with support for column grouping and with
     * editing triggered by a double click instead of a single click.
     */
    private class EditingGridLayer extends GridLayer {
        public EditingGridLayer(ILayer bodyLayer, ILayer columnHeaderLayer, ILayer rowHeaderLayer, ILayer cornerLayer) {
            super(bodyLayer, columnHeaderLayer, rowHeaderLayer, cornerLayer, false);
            
            // Add default bindings for editing
            this.addConfiguration(new DefaultEditConfiguration());
            
            // Change cell editing to be on double click rather than single click
            this.addConfiguration(new AbstractUiBindingConfiguration() {
                @Override
                public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
                    uiBindingRegistry.registerFirstDoubleClickBinding(
                        new BodyCellEditorMouseEventMatcher(TextCellEditor.class), new MouseEditAction());
                }
            });
        }
    }
    
    /**
     * Custom configuration for editing and rendering cells in the table.
     */
    private class CellConfiguration extends AbstractRegistryConfiguration {
        private final Dataset theDataset;
        
        public CellConfiguration(Dataset theDataset) {
            this.theDataset = theDataset;
        }
        
        @Override
        public void configureRegistry(IConfigRegistry configRegistry) {
            // Register cell editing rules with table
            if (theDataset instanceof ScalarDS) {
                configRegistry.registerConfigAttribute(
                        EditConfigAttributes.CELL_EDITABLE_RULE,
                        getScalarDSEditingRule((ScalarDS) theDataset),
                        DisplayMode.EDIT);
            } else {
                configRegistry.registerConfigAttribute(
                        EditConfigAttributes.CELL_EDITABLE_RULE,
                        getCompoundDSEditingRule((CompoundDS) theDataset),
                        DisplayMode.EDIT);
            }
            
            // Left-align cells and change font for rendering cell text
            Style cellStyle = new Style();

            cellStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
            cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR,
                    Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            
            if (curFont != null) {
                cellStyle.setAttributeValue(CellStyleAttributes.FONT, curFont);
            }
            else {
                cellStyle.setAttributeValue(CellStyleAttributes.FONT, Display.getDefault().getSystemFont());
            }

            configRegistry.registerConfigAttribute(
                    CellConfigAttributes.CELL_STYLE,
                    cellStyle);
        }
    }
    
    /**
     * Update cell value label and cell value field when a cell is selected
     */
    private class ScalarDSCellSelectionListener implements ILayerListener {
        @Override
        public void handleLayerEvent(ILayerEvent e) {
            if (e instanceof CellSelectionEvent) {
                CellSelectionEvent event = (CellSelectionEvent) e;
                Object val = table.getDataValueByPosition(event.getColumnPosition(), event.getRowPosition());
                String strVal = null;
                
                log.trace("NATTable CellSelected isRegRef={} isObjRef={}", isRegRef, isObjRef);
                
                String[] columnNames = ((ScalarDSColumnHeaderDataProvider) columnHeaderDataProvider).columnNames;
                int rowStart = ((RowHeaderDataProvider) rowHeaderDataProvider).start;
                int rowStride = ((RowHeaderDataProvider) rowHeaderDataProvider).stride;
                
                cellLabel.setText(String.valueOf(rowStart + indexBase
                        + table.getRowIndexByPosition(event.getRowPosition()) * rowStride)
                        + ", " + columnNames[table.getColumnIndexByPosition(event.getColumnPosition())] + "  =  ");
                
                if (isRegRef) {
                    boolean displayValues = ViewProperties.showRegRefValues();
                    log.trace("NATTable CellSelected displayValues={}", displayValues);
                    if (displayValues && val != null && ((String) val).compareTo("NULL") != 0) {
                        String reg = (String) val;
                        boolean isPointSelection = (reg.indexOf('-') <= 0);

                        // find the object location
                        String oidStr = reg.substring(reg.indexOf('/'), reg.indexOf(' '));
                        log.trace("NATTable CellSelected: isPointSelection={} oidStr={}", isPointSelection, oidStr);

                        // decode the region selection
                        String regStr = reg.substring(reg.indexOf('{') + 1, reg.indexOf('}'));
                        if (regStr == null || regStr.length() <= 0) { // no
                                                                      // selection
                            strVal = null;
                        }
                        else {
                            reg.substring(reg.indexOf('}') + 1);

                            StringTokenizer st = new StringTokenizer(regStr);
                            int nSelections = st.countTokens();
                            if (nSelections <= 0) { // no selection
                                strVal = null;
                            }
                            else {
                                log.trace("NATTable CellSelected: nSelections={}", nSelections);

                                HObject obj = FileFormat.findObject(dataset.getFileFormat(), oidStr);
                                if (obj == null || !(obj instanceof ScalarDS)) { // no
                                                                                 // selection
                                    strVal = null;
                                }
                                else {
                                    ScalarDS dset = (ScalarDS) obj;
                                    try {
                                        dset.init();
                                    }
                                    catch (Exception ex) {
                                        log.debug("reference dset did not init()", ex);
                                    }
                                    StringBuffer selectionSB = new StringBuffer();
                                    StringBuffer strvalSB = new StringBuffer();

                                    int idx = 0;
                                    while (st.hasMoreTokens()) {
                                        log.trace("NATTable CellSelected: st.hasMoreTokens() begin");

                                        int rank = dset.getRank();
                                        long start[] = dset.getStartDims();
                                        long count[] = dset.getSelectedDims();
                                        // long count[] = new long[rank];

                                        // set the selected dimension sizes
                                        // based on the region selection
                                        // info.
                                        String sizeStr = null;
                                        String token = st.nextToken();

                                        selectionSB.setLength(0);
                                        selectionSB.append(token);
                                        log.trace("NATTable CellSelected: selectionSB={}", selectionSB);

                                        token = token.replace('(', ' ');
                                        token = token.replace(')', ' ');
                                        if (isPointSelection) {
                                            // point selection
                                            String[] tmp = token.split(",");
                                            for (int x = 0; x < tmp.length; x++) {
                                                count[x] = 1;
                                                sizeStr = tmp[x].trim();
                                                start[x] = Long.valueOf(sizeStr);
                                                log.trace("NATTable CellSelected: point sel={}", tmp[x]);
                                            }
                                        }
                                        else {
                                            // rectangle selection
                                            String startStr = token.substring(0, token.indexOf('-'));
                                            String endStr = token.substring(token.indexOf('-') + 1);
                                            log.trace("NATTable CellSelected: rect sel with startStr={} endStr={}",
                                                    startStr, endStr);
                                            String[] tmp = startStr.split(",");
                                            log.trace("NATTable CellSelected: tmp with length={} rank={}", tmp.length,
                                                    rank);
                                            for (int x = 0; x < tmp.length; x++) {
                                                sizeStr = tmp[x].trim();
                                                start[x] = Long.valueOf(sizeStr);
                                                log.trace("NATTable CellSelected: rect start={}", tmp[x]);
                                            }
                                            tmp = endStr.split(",");
                                            for (int x = 0; x < tmp.length; x++) {
                                                sizeStr = tmp[x].trim();
                                                count[x] = Long.valueOf(sizeStr) - start[x] + 1;
                                                log.trace("NATTable CellSelected: rect end={} count={}", tmp[x],
                                                        count[x]);
                                            }
                                        }
                                        log.trace("NATTable CellSelected: selection inited");

                                        Object dbuf = null;
                                        try {
                                            dbuf = dset.getData();
                                        }
                                        catch (Exception ex) {
                                            Tools.showError(shell, ex.getMessage(), "Region Reference:" + shell.getText());
                                        }

                                        // Convert dbuf to a displayable
                                        // string
                                        String cName = dbuf.getClass().getName();
                                        int cIndex = cName.lastIndexOf("[");
                                        if (cIndex >= 0) {
                                            NT = cName.charAt(cIndex + 1);
                                        }
                                        log.trace("NATTable CellSelected: cName={} NT={}", cName, NT);

                                        if (idx > 0) strvalSB.append(',');

                                        // convert numerical data into char
                                        // only possible cases are byte[]
                                        // and short[] (converted from
                                        // unsigned
                                        // byte)
                                        Datatype dtype = dset.getDatatype();
                                        Datatype baseType = dtype.getBasetype();
                                        log.trace("NATTable CellSelected: dtype={} baseType={}",
                                                dtype.getDatatypeDescription(), baseType);
                                        if (baseType == null) baseType = dtype;
                                        if ((dtype.getDatatypeClass() == Datatype.CLASS_ARRAY && baseType.getDatatypeClass() == Datatype.CLASS_CHAR)
                                                && ((NT == 'B') || (NT == 'S'))) {
                                            int n = Array.getLength(dbuf);
                                            log.trace("NATTable CellSelected charData length = {}", n);
                                            char[] charData = new char[n];
                                            for (int i = 0; i < n; i++) {
                                                if (NT == 'B') {
                                                    charData[i] = (char) Array.getByte(dbuf, i);
                                                }
                                                else if (NT == 'S') {
                                                    charData[i] = (char) Array.getShort(dbuf, i);
                                                }
                                            }

                                            strvalSB.append(charData);
                                            log.trace("NATTable CellSelected charData");
                                        }
                                        else {
                                            // numerical values
                                            if (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY) dtype = baseType;
                                            boolean is_unsigned = dtype.isUnsigned();
                                            int n = Array.getLength(dbuf);
                                            if (is_unsigned) {
                                                switch (NT) {
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
                                                        Long l = (Long) larray[0];
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
                                                            l = (Long) larray[i];
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
                                            log.trace("NATTable CellSelected byteString");
                                        }
                                        idx++;
                                        dset.clearData();
                                        log.trace("NATTable CellSelected: st.hasMoreTokens() end");
                                    } // while (st.hasMoreTokens())
                                    strVal = strvalSB.toString();
                                    log.trace("NATTable CellSelected: st.hasMoreTokens() end");
                                }
                            }
                        }
                    }
                    else {
                        strVal = null;
                    }
                }
                else if (isObjRef) {
                    Long ref = (Long) val;
                    long oid[] = { ref.longValue() };

                    // decode object ID
                    try {
                        HObject obj = FileFormat.findObject(dataset.getFileFormat(), oid);
                        strVal = obj.getFullName();
                    }
                    catch (Exception ex) {
                        strVal = null;
                    }
                }

                if (strVal == null && val != null) strVal = val.toString();

                log.trace("NATTable CellSelected finish");
                
                cellValueField.setText(strVal);
            }
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
                Object val = table.getDataValueByPosition(event.getColumnPosition(), event.getRowPosition());
                
                if (val == null) return;
                
                log.trace("NATTable CellSelected isRegRef={} isObjRef={}", isRegRef, isObjRef);
                
                int rowStart = ((RowHeaderDataProvider) rowHeaderDataProvider).start;
                int rowStride = ((RowHeaderDataProvider) rowHeaderDataProvider).stride;
                
                int rowIndex = rowStart + indexBase + table.getRowIndexByPosition(event.getRowPosition()) * rowStride;
                Object fieldName = columnHeaderDataProvider.getDataValue(table.getColumnIndexByPosition(event.getColumnPosition()), 0);
                
                String colIndex = "";
                int numGroups = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).numGroups;
                
                if (numGroups > 1) {
                    int groupSize = ((CompoundDSColumnHeaderDataProvider) columnHeaderDataProvider).groupSize;
                    colIndex = "[" + String.valueOf((table.getColumnIndexByPosition(event.getColumnPosition())) / groupSize) + "]";
                }
                
                cellLabel.setText(String.valueOf(rowIndex) + ", " + fieldName + colIndex + " =  ");
                
                cellValueField.setText(val.toString());
                
                log.trace("NATTable CellSelected finish");
            }
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
            linePlotOptionShell.setFont(curFont);
            linePlotOptionShell.setText("Line Plot Options -- " + dataset.getName());
            linePlotOptionShell.setImage(ViewProperties.getHdfIcon());
            linePlotOptionShell.setLayout(new GridLayout(1, true));

            Label label = new Label(linePlotOptionShell, SWT.RIGHT);
            label.setFont(curFont);
            label.setText("Select Line Plot Options:");

            Composite content = new Composite(linePlotOptionShell, SWT.BORDER);
            content.setLayout(new GridLayout(3, false));
            content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            label = new Label(content, SWT.RIGHT);
            label.setFont(curFont);
            label.setText(" Series in:");

            colButton = new Button(content, SWT.RADIO);
            colButton.setFont(curFont);
            colButton.setText("Column");
            colButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
            colButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    colBox.setEnabled(true);
                    rowBox.setEnabled(false);
                }
            });

            rowButton = new Button(content, SWT.RADIO);
            rowButton.setFont(curFont);
            rowButton.setText("Row");
            rowButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
            rowButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    rowBox.setEnabled(true);
                    colBox.setEnabled(false);
                }
            });

            label = new Label(content, SWT.RIGHT);
            label.setFont(curFont);
            label.setText(" For abscissa use:");

            long[] startArray = dataset.getStartDims();
            long[] strideArray = dataset.getStride();
            int[] selectedIndex = dataset.getSelectedIndex();
            int start = (int) startArray[selectedIndex[0]];
            int stride = (int) strideArray[selectedIndex[0]];

            colBox = new Combo(content, SWT.SINGLE | SWT.READ_ONLY);
            colBox.setFont(curFont);
            GridData colBoxData = new GridData(SWT.FILL, SWT.FILL, true, false);
            colBoxData.minimumWidth = 100;
            colBox.setLayoutData(colBoxData);

            colBox.add("array index");

            for (int i = 0; i < ncol; i++) {
                colBox.add("column " + columnHeaderDataProvider.getDataValue(i, 0));
            }

            rowBox = new Combo(content, SWT.SINGLE | SWT.READ_ONLY);
            rowBox.setFont(curFont);
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
            okButton.setFont(curFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
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

            Button cancelButton = new Button(buttonComposite, SWT.PUSH);
            cancelButton.setFont(curFont);
            cancelButton.setText(" &Cancel ");
            cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            cancelButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    plotType = NO_PLOT;
                    linePlotOptionShell.dispose();
                }
            });

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
}
