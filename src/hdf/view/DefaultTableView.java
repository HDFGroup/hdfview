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

import java.awt.event.MouseEvent;

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
//import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;

import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
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
    private static final long serialVersionUID      = -7452459299532863847L;

    private final static org.slf4j.Logger log       = org.slf4j.LoggerFactory.getLogger(DefaultTableView.class);

    private final Display display                   = Display.getCurrent();
    private final Shell shell;
    
    // The main HDFView
    private final ViewManager viewer;

    private NatTable table; // The NatTable to display data in
    
    // The Dataset (Scalar or Compound) to be displayed in the Table
    private Dataset dataset;
    
    /**
     * The value of the dataset.
     */
    private Object                        	dataValue;
    
    private Object           				fillValue               = null;

    private enum ViewType { TABLE, IMAGE, TEXT };
    private	ViewType viewType = ViewType.TABLE;
    
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
    
    // Text field to display the value of the current cell.
    private Text                          	cellValueField;
    
    // Label to indicate the current cell location.
    private Label                         	cellLabel;

    // The value of the current cell value in editing.
    private Object           				currentEditingCellValue = null;

    /**
     * Constructs a TableView.
     * <p>
     * 
     * @param theView
     * 			the main HDFView.
     */
    public DefaultTableView(Shell parent, ViewManager theView) {
        this(parent, theView, null);
    }

    /**
     * Constructs a TableView.
     * <p>
     * 
     * @param theView
     * 			the main HDFView.
     * 
     * @param map
     * 			the properties on how to show the data. The map is used to allow applications to
     *          pass properties on how to display the data, such as, transposing data, showing
     *          data as character, applying bitmask, and etc. Predefined keys are listed at
     *          ViewProperties.DATA_VIEW_KEY.
     */
    public DefaultTableView(Shell parent, ViewManager theView, HashMap map) {
        log.trace("DefaultTableView start");

        shell = new Shell(parent, SWT.TITLE | SWT.MAX | SWT.CLOSE);
        shell.setLayout(new RowLayout());
        
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

        // Create border around Table with Index base indicator
        //TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray, 1), indexBase + "-based",
        //        TitledBorder.RIGHT, TitledBorder.TOP, this.getFont(), Color.black);
        //((JPanel) getContentPane()).setBorder(border);
        
        org.eclipse.swt.widgets.Group group = 
        		new org.eclipse.swt.widgets.Group(shell, SWT.SHADOW_ETCHED_OUT);
        group.setFont(Display.getCurrent().getSystemFont());
        group.setText(indexBase + "-based");
        group.setLayout(new FillLayout());
        
        SashForm content = new SashForm(group, SWT.VERTICAL);
        content.setSashWidth(10);
        
        Composite cellValueComposite = new Composite(content, SWT.NONE);
        RowLayout layout = new RowLayout();
        layout.justify = true;
        layout.marginHeight = 2;
        layout.marginWidth = 2;
        layout.type = SWT.HORIZONTAL;
        cellValueComposite.setLayout(new RowLayout());
        
        cellLabel = new Label(cellValueComposite, SWT.BORDER);
        cellLabel.setSize(75, cellLabel.getSize().y);
        cellLabel.setAlignment(SWT.CENTER);
        
        cellValueField = new Text(cellValueComposite, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
        //cellValueField.setWrapStyleWord(true);
        cellValueField.setEditable(false);
        cellValueField.setBackground(new Color(display, 255, 255, 240));
        cellValueField.setEnabled(false);
        
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
                //table.addMouseListener(this);

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
            log.trace("createTable((ScalarDS) dataset): isRegRef={} isObjRef={} showAsHex={}", isRegRef, isObjRef, showAsHex);
        }
        content.setWeights(new int[] {1, 19});

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
        
        // Create popup menu for reg. ref.
        if (isRegRef || isObjRef) popupMenu = createPopupMenu();
        
        log.trace("DefaultTableView: finish");
        
        Composite dataClientArea = ((HDFView) viewer).getDataArea();
        shell.setSize(dataClientArea.getClientArea().width, dataClientArea.getClientArea().height);
        shell.setLocation(dataClientArea.getBounds().x, dataClientArea.getBounds().y);
        
        group.pack();
        group.setLayoutData(new RowData(shell.getSize().x, shell.getSize().y));
        
        shell.pack();
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
            rows = dataset.getHeight();
            cols = dataset.getWidth();
        } else {
        	rows = (int) dims[0];
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

                //JPanel contentpane = (JPanel) getContentPane();
                //Border border = contentpane.getBorder();
                
               // String btitle = ((TitledBorder) border).getTitle();
               //btitle += ", " + opName + bitmask;
               //((TitledBorder) border).setTitle(btitle);
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
                dataValue = Dataset.byteToString((byte[]) dataValue, baseType.getDatatypeSize());
            }
        }
        
        final String columnNames[] = new String[cols];
        final int rowCount = rows;
        final int colCount = cols;
        final long[] startArray = dataset.getStartDims();
        final long[] strideArray = dataset.getStride();
        int[] selectedIndex = dataset.getSelectedIndex();
        final int rowStart = (int) startArray[selectedIndex[0]];
        final int rowStride = (int) strideArray[selectedIndex[0]];
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
        IDataProvider bodyDataProvider = new ScalarDSDataProvider();
        final DataLayer dataLayer = new DataLayer(bodyDataProvider);
        final SelectionLayer selectionLayer = new SelectionLayer(dataLayer);
        ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);
        dataLayer.setDefaultColumnWidth(80);
        
        // Create the Column Header layer
        IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columnNames);
        ILayer columnHeaderLayer = new ColumnHeaderLayer(new DataLayer(
                columnHeaderDataProvider), viewportLayer, selectionLayer);
        
        // Create the Row Header layer
        IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(
                bodyDataProvider);
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
        
        final NatTable natTable = new NatTable(parent, gridLayer);
        
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
        
        return null; // Remove when implemented
    }
    
    /**
     * Creates the menubar for the NatTable.
     */
    private Menu createMenuBar() {
        Menu menuBar = new Menu(shell, SWT.BAR);
        Button button;
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
                    //saveAsText();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });

        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Export Data to Binary File");
        item.setEnabled(dataset instanceof ScalarDS); // Disable export menu if this isn't a Scalar Dataset

        Menu exportAsBinaryMenu = new Menu(menu);
        item.setMenu(exportAsBinaryMenu);

        //if (!(dataset instanceof ScalarDS)) {
        //exportAsBinaryMenu.setVisible(false);
        //}

        item = new MenuItem(exportAsBinaryMenu, SWT.PUSH);
        item.setText("Native Order");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 1;

                try {
                    //saveAsBinary();
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
                    //saveAsBinary();
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
                    //saveAsBinary();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Import Data from Text File");
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                /*
                String currentDir = dataset.getFileFormat().getParent();
                JFileChooser fchooser = new JFileChooser(currentDir);
                fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
                int returnVal = fchooser.showOpenDialog(this);

                if (returnVal != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                File choosedFile = fchooser.getSelectedFile();
                if (choosedFile == null) {
                    return;
                }

                String txtFile = choosedFile.getAbsolutePath();
                importTextData(txtFile);
                 */
            }
        });

        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Import Data from Binary File");
        item.setEnabled(dataset instanceof ScalarDS); // Disable import menu if this isn't a Scalar Dataset

        Menu importFromBinaryMenu = new Menu(menu);
        item.setMenu(importFromBinaryMenu);

        //if ((dataset instanceof ScalarDS)) {
        //    menu.add(importFromBinaryMenu);
        //}

        item = new MenuItem(importFromBinaryMenu, SWT.PUSH);
        item.setText("Native Order");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 1;

                try {
                    //importBinaryData();
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
                    //importBinaryData();
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
                    //importBinaryData();
                }
                catch (Exception ex) {
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });
        
        if(dataset instanceof ScalarDS) {
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
        //item.setActionCommand("Write selection to dataset");
        item.setEnabled(isEditable && (dataset instanceof ScalarDS));
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                /*
                JTable jtable = getTable();
                if ((jtable.getSelectedColumnCount() <= 0) || (jtable.getSelectedRowCount() <= 0)) {
                    JOptionPane.showMessageDialog(this, "Select table cells to write.", "HDFView", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

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
                //showLineplot();
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Statistics");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                /*
                try {
                    Object theData = getSelectedData();

                    if (dataset instanceof CompoundDS) {
                        int cols = table.getSelectedColumnCount();
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
                 */
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Math Conversion");
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    //mathConversion();
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
                        checkCustomNotation.setSelection(false);
                        numberFormat = scientificFormat;
                        checkHex.setSelection(false);
                        checkBin.setSelection(false);
                        showAsHex = false;
                        showAsBin = false;
                    }
                    else
                        numberFormat = normalFormat;
                    //this.updateUI();  //redraw menu
                }
            });
            
            checkCustomNotation = new MenuItem(menu, SWT.CHECK);
            checkCustomNotation.setText("Show Custom Notation");
            checkCustomNotation.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (checkCustomNotation.getSelection()) {
                        numberFormat = customFormat;
                        checkScientificNotation.setSelection(false);
                        checkHex.setSelection(false);
                        checkBin.setSelection(false);
                        showAsHex = false;
                        showAsBin = false;
                    }
                    else
                        numberFormat = normalFormat;
                    //this.updateUI(); // redraw menu
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

                //customFormat.applyPattern(str);
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
                        checkScientificNotation.setSelection(false);
                        checkCustomNotation.setSelection(false);
                        checkBin.setSelection(false);
                        showAsBin = false;
                        numberFormat = normalFormat;
                    }
                    //this.updateUI();
                }
            });
            
            checkBin = new MenuItem(menu, SWT.CHECK);
            checkBin.setText("Show Binary");
            checkBin.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    showAsBin = checkBin.getSelection();
                    if (showAsBin) {
                        checkScientificNotation.setSelection(false);
                        checkCustomNotation.setSelection(false);
                        checkHex.setSelection(false);
                        showAsHex = false;
                        numberFormat = normalFormat;
                    }
                    //this.updateUI();
                }
            });

        }
        
        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Close");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
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

                //viewer.removeDataView(this);

                shell.dispose();
            }
        });

        //new Label(menuBar, SWT.NONE).setText("     ");

        // add icons to the menubar

        //Insets margin = new Insets(0, 2, 0, 2);

        // chart button
        /*
        button = new JButton(ViewProperties.getChartIcon());
        bar.add(button);
        button.setToolTipText("Line Plot");
        button.setMargin(margin);
        button.addActionListener(this);
        button.setActionCommand("Show chart");
         */

        if (is3D) {
            //bar.add(new JLabel("     "));

            // first button
            //button = new Button(menuBar, SWT.PUSH);
            //button.setImage(ViewProperties.getFirstIcon());
            //button.setToolTipText("First");
            //button.setMargin(margin);
            //button.addSelectionListener(new SelectionAdapter() {
            //  public void widgetSelected(SelectionEvent e) {
            //      firstPage();
            //  }
            //});

            // previous button
            //button = new Button(menuBar, SWT.PUSH);
            //button.setImage(ViewProperties.getPreviousIcon());
            //button.setToolTipText("Previous");
            //button.setMargin(margin);
            //button.addSelectionListener(new SelectionAdapter() {
            //  public void widgetSelected(SelectionEvent e) {
            //      previousPage();
            //  }
            //});

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

            //JLabel tmpField = new JLabel(String.valueOf(maxFrame), SwingConstants.CENTER);
            //tmpField.setMaximumSize(new Dimension(50, 30));
            //bar.add(tmpField);

            // next button
            //button = new Button(menuBar, SWT.PUSH);
            //button.setImage(ViewProperties.getNextIcon());
            //button.setToolTipText("Next");
            //button.setMargin(margin);
            //button.addSelectionListener(new SelectionAdapter() {
            //  public void widgetSelected(SelectionEvent e) {
            //      nextPage();
            //  }
            //});

            // last button
            //button = new Button(menuBar, SWT.PUSH);
            //button.setImage(ViewProperties.getLastIcon());
            //button.setToolTipText("Last");
            //button.setMargin(margin);
            //button.addSelectionListener(new SelectionAdapter() {
            //  public void widgetSelected(SelectionEvent e) {
            //      lastPage();
            //  }
            //});
        }

        return menuBar;
    }
    
    /** Creates a popup menu for a right mouse click on a data object */
    private Menu createPopupMenu() {
        Menu menu = new Menu(shell, SWT.POP_UP);
        table.setMenu(menu);
        
        /*
        MenuItem item = new MenuItem(menu);
        item.setText("Show As &Table");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                viewType = ViewType.TABLE;
                
                log.trace("DefaultTableView: Show data as {}: ", viewType);
                
                Object theData = getSelectedData();
                if (theData == null) {
                    shell.getDisplay.beep();
                    showError("No data selected.", shell.getText());
                    return;
                }
                
                int[] selectedRows = table.getSelectedRows();
                int[] selectedCols = table.getSelectedColumns();
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

        item = new MenuItem(menu);
        item.setText("Show As &Image");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                viewType = ViewType.IMAGE;
                
                log.trace("DefaultTableView: Show data as {}: ", viewType);
                
                Object theData = getSelectedData();
                if (theData == null) {
                    shell.getDisplay.beep();
                    showError("No data selected.", shell.getText());
                    return;
                }
                
                int[] selectedRows = table.getSelectedRows();
                int[] selectedCols = table.getSelectedColumns();
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

        item = new MenuItem(menu);
        item.setText("Show As &Text");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                viewType = ViewType.TEXT;
                
                log.trace("DefaultTableView: Show data as {}: ", viewType);
                
                Object theData = getSelectedData();
                if (theData == null) {
                    shell.getDisplay.beep();
                    showError("No data selected.", shell.getText());
                    return;
                }
                
                int[] selectedRows = table.getSelectedRows();
                int[] selectedCols = table.getSelectedColumns();
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
        
        */

        return menu;
    }
    
    // Flip to previous page of Table
    private void previousPage() {
        // Only valid operation if dataset has 3 or more dimensions
        if (dataset.getRank() < 3) return;

        long[] start = dataset.getStartDims();
        //dataset.getDims();
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
        //dataset.getDims();
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

        //setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            dataValue = dataset.getData();
            if (dataset instanceof ScalarDS) {
                ((ScalarDS) dataset).convertFromUnsignedC();
                dataValue = dataset.getData();
            }
        }
        catch (Exception ex) {
            //setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            dataValue = null;
            showError(ex.getMessage(), shell.getText());
            return;
        }

        //setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        frameField.setText(String.valueOf(curFrame));
        
        table.refresh();
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
        
        //if (currentEditingCellValue != null) {
            // Data values are the same, no need to change the data
        //    if (currentEditingCellValue.toString().equals(cellValue)) return;
        //}

        if (dataset instanceof ScalarDS) {
            //updateScalarData(cellValue, row, col);
        }
        else if (dataset instanceof CompoundDS) {
            //updateCompoundData(cellValue, row, col);
        }
    }
    
    /**
     * Copy data from the spreadsheet to the system clipboard.
     */
    private void copyData() {
        /*
        StringBuffer sb = new StringBuffer();

        int r0 = table.getSelectedRow(); // starting row
        int c0 = table.getSelectedColumn(); // starting column

        if ((r0 < 0) || (c0 < 0)) {
            return;
        }

        int nr = table.getSelectedRowCount();
        int nc = table.getSelectedColumnCount();
        int r1 = r0 + nr; // finish row
        int c1 = c0 + nc; // finishing column

        try {
            for (int i = r0; i < r1; i++) {
                sb.append(table.getValueAt(i, c0).toString());
                for (int j = c0 + 1; j < c1; j++) {
                    sb.append("\t");
                    sb.append(table.getValueAt(i, j).toString());
                }
                sb.append("\n");
            }
        }
        catch (java.lang.OutOfMemoryError err) {
            shell.getDisplay().beep();
            showError("Copying data to system clipboard failed. \nUsing \"export/import data\" for copying/pasting large data.", shell.getText());
            return;
        }

        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection contents = new StringSelection(sb.toString());
        cb.setContents(contents, null);
        */
    }
    
    /**
     * Paste data from the system clipboard to the spreadsheet.
     */
    private void pasteData() {
        /*
        MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(shell.getText());
        confirm.setMessage("Do you want to paste selected data?");
        if (confirm.open() == SWT.NO) return;

        int cols = table.getColumnCount();
        int rows = table.getRowCount();
        int r0 = table.getSelectedRow();
        int c0 = table.getSelectedColumn();

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

        table.updateUI();
        */
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
        /*
        Object selectedData = null;

        int[] selectedRows = table.getSelectedRows();
        int[] selectedCols = table.getSelectedColumns();
        if (selectedRows == null || selectedRows.length <= 0 || selectedCols == null || selectedCols.length <= 0) {
            return null;
        }

        int size = selectedCols.length * selectedRows.length;
        log.trace("DefaultTableView getSelectedScalarData: {}", size);

        // the whole table is selected
        if ((table.getColumnCount() == selectedCols.length) && (table.getRowCount() == selectedRows.length)) {
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

        table.getSelectedRow();
        table.getSelectedColumn();
        int w = table.getColumnCount();
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
        */
        
        return null; // Remove when fixed
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
        
    }
    
    // Show an error dialog with the given error message
    private void showError(String errorMsg, String title) {
        MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
        error.setText(title);
        error.setMessage(errorMsg);
        error.open();
    }
    
    private class ScalarDSDataProvider implements IDataProvider {
    	private static final long  serialVersionUID = 254175303655079056L;
    	private final StringBuffer stringBuffer     = new StringBuffer();
        private final Datatype     dtype            = dataset.getDatatype();
        private final Datatype     btype            = dtype.getBasetype();
        private final int          typeSize         = dtype.getDatatypeSize();
        private final boolean      isArray          = (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY);
        private final boolean      isStr            = (NT == 'L');
        private final boolean      isInt            = (NT == 'B' || NT == 'S' || NT == 'I' || NT == 'J');
        private final boolean      isUINT64         = (dtype.isUnsigned() && (NT == 'J'));
        private Object             theValue;

        boolean                    isNaturalOrder   = (dataset.getRank() == 1 || (dataset.getSelectedIndex()[0] < dataset
                                                            .getSelectedIndex()[1]));
        
        private int                rowCount         = dataset.getHeight();
        private int                colCount         = dataset.getWidth();
    	
    	public ScalarDSDataProvider() {
    		
    	}
    	
    	@Override
        public Object getDataValue(int columnIndex, int rowIndex) {
    		if (startEditing[0]) return "";
            log.trace("ScalarDS:ScalarDSDataProvider:getValueAt({},{}) start", columnIndex, rowIndex);
            log.trace("ScalarDS:ScalarDSDataProvider:getValueAt isInt={} isArray={} showAsHex={} showAsBin={}", isInt, isArray, showAsHex, showAsBin);

            if (isArray) {
                // ARRAY dataset
                int arraySize = dtype.getDatatypeSize() / btype.getDatatypeSize();
                log.trace("ScalarDS:ScalarDSDataProvider:getValueAt ARRAY dataset size={} isDisplayTypeChar={} isUINT64={}",
                        arraySize, isDisplayTypeChar, isUINT64);

                stringBuffer.setLength(0); // clear the old string
                int i0 = (rowIndex * colCount + columnIndex) * arraySize;
                int i1 = i0 + arraySize;

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
                int index = columnIndex * rowCount + rowIndex;

                if (dataset.getRank() > 1) {
                    log.trace("ScalarDS:ScalarDSDataProvider:getValueAt rank={} isDataTransposed={} isNaturalOrder={}", dataset.getRank(), isDataTransposed, isNaturalOrder);
                    if ((isDataTransposed && isNaturalOrder) || (!isDataTransposed && !isNaturalOrder))
                        index = columnIndex * rowCount + rowIndex;
                    else
                        index = rowIndex * colCount + columnIndex;
                }
                log.trace("ScalarDS:ScalarDSDataProvider:getValueAt index={} isStr={} isUINT64={}", index, isStr, isUINT64);

                if (isStr) {
                    theValue = Array.get(dataValue, index);
                    return theValue;
                }

                if (isUINT64) {
                    theValue = Array.get(dataValue, index);
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
                    theValue = Array.get(dataValue, index * typeSize);
                    log.trace("ScalarDS:ScalarDSDataProvider:getValueAt() theValue[{}]={}", index, theValue.toString());
                    // show in Hexadecimal
                    char[] hexChars = new char[2];
                    stringBuffer.setLength(0); // clear the old string
                    for (int x = 0; x < typeSize; x++) {
                        if (x > 0)
                            theValue = Array.get(dataValue, index * typeSize + x);
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
                    theValue = Array.get(dataValue, index);
                    theValue = Tools.toBinaryString(Long.valueOf(theValue.toString()), typeSize);
                    // theValue =
                    // Long.toBinaryString(Long.valueOf(theValue.toString()));
                }
                else if (numberFormat != null) {
                    // show in scientific format
                    theValue = Array.get(dataValue, index);
                    theValue = numberFormat.format(theValue);
                }
                else {
                    theValue = Array.get(dataValue, index);
                }
            }

            log.trace("ScalarDS:ScalarDSDataProvider:getValueAt finish");
            return theValue;
        }

        @Override
        public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
        	
        }

        @Override
        public int getColumnCount() {
            return dataset.getWidth();
        }

        @Override
        public int getRowCount() {
            return dataset.getHeight();
        }
    }
    
    //private class CompoundDSDataProvider implements IDataProvider {
    	
    //}
}
