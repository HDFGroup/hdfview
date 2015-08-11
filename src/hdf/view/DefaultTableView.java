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
//import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;
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
public class DefaultTableView extends Shell implements TableView {
    private static final long             	serialVersionUID = -7452459299532863847L;

    private final static org.slf4j.Logger 	log              = org.slf4j.LoggerFactory.getLogger(DefaultTableView.class);

    private final Display display =			Display.getCurrent();
    
    /**
     * The shell to display the TableView in.
     */
    private final Shell 				  	shell;
    
    /**
     * The main HDFView.
     */
    private final ViewManager             	viewer;

    /**
     * Numerical data type. B = byte array, S = short array, I = int array, J = long array, F =
     * float array, and D = double array.
     */
    private char                          	NT               = ' ';

    /**
     * The Scalar Dataset.
     */
    private Dataset                       	dataset;

    /**
     * The value of the dataset.
     */
    private Object                        	dataValue;

    /**
     * The table used to hold the table data.
     */
    private Table                         	table;

    /** Label to indicate the current cell location. */
    private Label                         	cellLabel;

    /** Text field to display the value of the current cell. */
    private Text                          	cellValueField;

    private boolean                       	isValueChanged;

    private boolean                       	isReadOnly;

    private boolean                       	isDisplayTypeChar;

    private boolean                       	isDataTransposed;

    private boolean                       	isRegRef;
    private boolean                       	isObjRef;

    private final Button                  	checkFixedDataLength;
    private int                           	fixedDataLength;
    private final Button                  	checkCustomNotation;
    private final Button                  	checkScientificNotation;
    private final Button                  	checkHex;
    private final Button                  	checkBin;

    // changed to use normalized scientific notation (1 <= coefficient < 10).
    // private final DecimalFormat scientificFormat = new DecimalFormat("###.#####E0#");
    private final DecimalFormat           	scientificFormat = new DecimalFormat("0.0###E0###");
    private DecimalFormat                 	customFormat     = new DecimalFormat("###.#####");
    private final NumberFormat            	normalFormat     = null;                           // NumberFormat.getInstance();
    private NumberFormat                  	numberFormat     = normalFormat;
    private boolean                       	showAsHex        = false, showAsBin = false;
    private final boolean                 	startEditing[]   = { false };
    private Menu                          	popupMenu;

    private enum ViewType {
        TABLE, IMAGE, TEXT
    }

    private ViewType         				viewType;

    private Text             				frameField;

    private long             				curFrame                = 0;
    private long             				maxFrame                = 1;

    private Object           				fillValue               = null;

    //private BitSet           				bitmask;

    private BITMASK_OP       				bitmaskOP               = BITMASK_OP.EXTRACT;

    private int              				binaryOrder;

    private int              				indexBase               = 0;

    private static final int 				FLOAT_BUFFER_SIZE       = 524288;

    private static final int 				INT_BUFFER_SIZE         = 524288;

    private static final int 				SHORT_BUFFER_SIZE       = 1048576;

    private static final int 				LONG_BUFFER_SIZE        = 262144;

    private static final int 				DOUBLE_BUFFER_SIZE      = 262144;

    private static final int 				BYTE_BUFFER_SIZE        = 2097152;

    /* The value of the current cell value in editing. */
    private Object           				currentEditingCellValue = null;

    /**
     * Constructs an TableView.
     * <p>
     * 
     * @param theView
     *            the main HDFView.
     */
    public DefaultTableView(ViewManager theView) {
        this(theView, null);
    }

    /**
     * Constructs an TableView.
     * <p>
     * 
     * @param theView
     *            the main HDFView.
     * @param map
     *            the properties on how to show the data. The map is used to allow applications to
     *            pass properties on how to display the data, such as, transposing data, showing
     *            data as character, applying bitmask, and etc. Predefined keys are listed at
     *            ViewProperties.DATA_VIEW_KEY.
     */
    public DefaultTableView(ViewManager theView, HashMap map) {
        shell = new Shell(display);

        log.trace("DefaultTableView start");

        viewer = theView;
        isValueChanged = false;
        isReadOnly = false;
        isRegRef = false;
        isObjRef = false;
        viewType = ViewType.TABLE;
        fixedDataLength = -1;
        HObject hobject = null;
        popupMenu = null;
        //bitmask = null;

        if (ViewProperties.isIndexBase1()) indexBase = 1;
        log.trace("isIndexBase1() is {}", indexBase);

        checkFixedDataLength = new Button(shell, SWT.CHECK);
        checkFixedDataLength.setText("Fixed Data Length");
        checkCustomNotation = new Button(shell, SWT.CHECK);
        checkCustomNotation.setText("Show Custom Notation");
        checkScientificNotation = new Button(shell, SWT.CHECK);
        checkScientificNotation.setText("Show Scientific Notation");
        checkHex = new Button(shell, SWT.CHECK);
        checkHex.setText("Show Hexadecimal");
        checkBin = new Button(shell, SWT.CHECK);
        checkBin.setText("Show Binary");

        if (map != null) {
        	/*
            hobject = (HObject) map.get(ViewProperties.DATA_VIEW_KEY.OBJECT);

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
            */
        }
        log.trace("isIndexBase={} - isDataTransposed={} - isDisplayTypeChar={}", indexBase, isDataTransposed, isDisplayTypeChar);

        if (hobject == null) hobject = viewer.getTreeView().getCurrentObject();

        if ((hobject == null) || !(hobject instanceof Dataset)) {
            return;
        }

        dataset = (Dataset) hobject;
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
        
        // set title & border
        //TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray, 1), indexBase + "-based",
        //        TitledBorder.RIGHT, TitledBorder.TOP, this.getFont(), Color.black);
        //((JPanel) getContentPane()).setBorder(border);

        // create the table and its columnHeader
        if (dataset instanceof CompoundDS) {
            isDataTransposed = false; // disable transpose for compound dataset
            //this.setFrameIcon(ViewProperties.getTableIcon());
            table = createTable((CompoundDS) dataset);
        }
        else { /* if (dataset instanceof ScalarDS) */
            this.setImage(ViewProperties.getDatasetIcon());
            table = createTable((ScalarDS) dataset);
            log.trace("createTable((ScalarDS) dataset) dtype.getDatatypeClass()={}", dtype.getDatatypeClass());

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
            log.trace("createTable((ScalarDS) dataset) isRegRef={} isObjRef={} showAsHex={}", isRegRef, isObjRef, showAsHex);
        }

        if (table == null) {
            viewer.showStatus("Creating table failed - " + dataset.getName());
            dataset = null;
            this.dispose();
            return;
        }

        log.trace("DefaultTableView create ColumnHeader");
        //ColumnHeader columnHeaders = new ColumnHeader(table);
        //table.setTableHeader(columnHeaders);
        //table.setCellSelectionEnabled(true);
        //table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        //table.setGridColor(Color.gray);

        // Add the table to a scroller
        ScrolledComposite scrollingTable = new ScrolledComposite(shell, SWT.H_SCROLL | SWT.V_SCROLL);
        scrollingTable.setContent(table);
        scrollingTable.getVerticalBar().setIncrement(100);
        scrollingTable.getHorizontalBar().setIncrement(100);

        // Create row headers and add it to the scroller
        log.trace("DefaultTableView create RowHeader");
        //RowHeader rowHeaders = new RowHeader(table, dataset);

        //JViewport viewp = new JViewport();
        //viewp.add(rowHeaders);
        //viewp.setPreferredSize(rowHeaders.getPreferredSize());
        //scrollingTable.setRowHeader(viewp);

        //cellLabel = new Label();
        //cellLabel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        //Dimension dim = cellLabel.getPreferredSize();
        //dim.width = 75;
        //cellLabel.setPreferredSize(dim);
        //cellLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        //cellValueField = new Text(table, SWT.SINGLE);
        //cellValueField.setLineWrap(true);
        //cellValueField.setWrapStyleWord(true);
        //cellValueField.setEditable(false);
        //cellValueField.setBackground(new Color(255, 255, 240));

        ScrolledComposite scrollingcellValue = new ScrolledComposite(table, SWT.H_SCROLL | SWT.V_SCROLL);
        scrollingcellValue.setContent(cellValueField);
        scrollingcellValue.getVerticalBar().setIncrement(50);
        scrollingcellValue.getHorizontalBar().setIncrement(50);

        //JPanel valuePane = new JPanel();
        //valuePane.setLayout(new BorderLayout());
        //valuePane.add(cellLabel, BorderLayout.WEST);
        //valuePane.add(scrollingcellValue, BorderLayout.CENTER);

        //JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, valuePane, scrollingTable);
        //splitPane.setDividerLocation(25);
        //JPanel contentPane = (JPanel) getContentPane();
        //contentPane.add(splitPane);

        StringBuffer sb = new StringBuffer(hobject.getName());
        sb.append("  at  ");
        sb.append(hobject.getPath());
        sb.append("  [");
        sb.append(dataset.getFileFormat().getName());
        sb.append("  in  ");
        sb.append(dataset.getFileFormat().getParent());
        sb.append("]");
        shell.setText(sb.toString());

        // Setup subset information
        log.trace("DefaultTableView setup subset information");
        int rank = dataset.getRank();
        int[] selectedIndex = dataset.getSelectedIndex();
        long[] count = dataset.getSelectedDims();
        long[] stride = dataset.getStride();
        // long[] dims = dataset.getDims();
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
        log.trace("DefaultTableView subset={}", sb.toString());

        shell.setMenuBar(createMenuBar());
        viewer.showStatus(sb.toString());

        // set cell height for large fonts
        //int cellRowHeight = table.getFontMetrics(table.getFont()).getHeight();
        //rowHeaders.setRowHeight(cellRowHeight);
        //table.setRowHeight(cellRowHeight);

        // create popup menu for reg. ref.
        if (isRegRef || isObjRef) popupMenu = createPopupMenu();
        log.trace("DefaultTableView finish");
        
        shell.open();
        
        while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
   }

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
                    saveAsText();
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
        
        //item = checkFixedDataLength;
        //if (dataset instanceof ScalarDS) {
        //    menu.add(item);
        //}
        //item.addSelectionListener(new SelectionAdapter() {
        //	public void widgetSelected(SelectionEvent e) {
        //		if (!checkFixedDataLength.isSelected()) {
        //            fixedDataLength = -1;
        //            this.updateUI();
        //            return;
        //        }
        //
        //        String str = JOptionPane
        //                .showInputDialog(
        //                        this,
        //                        "Enter fixed data length when importing text data\n\n"
        //                                + "For example, for a text string of \"12345678\"\n\t\tenter 2, the data will be 12, 34, 56, 78\n\t\tenter 4, the data will be 1234, 5678\n",
        //                        "");
        //
        //        if ((str == null) || (str.length() < 1)) {
        //            checkFixedDataLength.setSelected(false);
        //            return;
        //        }
        //
        //        try {
        //            fixedDataLength = Integer.parseInt(str);
        //        }
        //        catch (Exception ex) {
        //            fixedDataLength = -1;
        //        }
        //
        //        if (fixedDataLength < 1) {
        //            checkFixedDataLength.setSelected(false);
        //            return;
        //        }
        //	}
        //});
        
        
        //if ((dataset instanceof ScalarDS)) {
        //    menu.add(importFromBinaryMenu);
        //}
        
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
        		showLineplot();
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
        			mathConversion();
        		}
        		catch (Exception ex) {
        			shell.getDisplay().beep();
        			showError(ex.getMessage(), shell.getText());
        		}
        	}
        });

        new MenuItem(menu, SWT.SEPARATOR);

        //item = checkScientificNotation;
        //if (dataset instanceof ScalarDS) {
        //    menu.add(item);
        //}
        //item.addSelectionListener(new SelectionAdapter() {
        //	public void widgetSelected(SelectionEvent e) {
        //		if (checkScientificNotation.isSelected()) {
        //            checkCustomNotation.setSelected(false);
        //            numberFormat = scientificFormat;
        //            checkHex.setSelected(false);
        //            checkBin.setSelected(false);
        //            showAsHex = false;
        //            showAsBin = false;
        //        }
        //        else
        //            numberFormat = normalFormat;
        //        this.updateUI();
        //	}
        //});

        //item = checkCustomNotation;
        //if (dataset instanceof ScalarDS) {
        //    menu.add(item);
        //}
        //item.addSelectionListener(new SelectionAdapter() {
        //	public void widgetSelected(SelectionEvent e) {
        //		if (checkCustomNotation.isSelected()) {
        //            numberFormat = customFormat;
        //            checkScientificNotation.setSelected(false);
        //            checkHex.setSelected(false);
        //            checkBin.setSelected(false);
        //            showAsHex = false;
        //            showAsBin = false;
        //        }
        //        else
        //            numberFormat = normalFormat;
        //        this.updateUI();
        //	}
        //});

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
        		// String str = (String) JOptionPane.showInputDialog(this, msg, "Create a custom number format",
        		//        JOptionPane.PLAIN_MESSAGE, ViewProperties.getLargeHdfIcon(), null, null);
        		//if ((str == null) || (str.length() < 1)) {
        		//    return;
        		//}

        		//customFormat.applyPattern(str);
        	}
        });

        boolean isInt = (NT == 'B' || NT == 'S' || NT == 'I' || NT == 'J');
        
        //item = checkHex;
        //if ((dataset instanceof ScalarDS) && isInt) {
        //    menu.add(item);
        //}
        //item.addSelectionListener(new SelectionAdapter() {
        //	public void widgetSelected(SelectionEvent e) {
        //		showAsHex = checkHex.isSelected();
        //        if (showAsHex) {
        //            checkScientificNotation.setSelected(false);
        //            checkCustomNotation.setSelected(false);
        //            checkBin.setSelected(false);
        //            showAsBin = false;
        //            numberFormat = normalFormat;
        //        }
        //        this.updateUI();
        //	}
        //});

        //item = checkBin;
        //if ((dataset instanceof ScalarDS) && isInt) {
        //    menu.add(item);
        //}
        //item.addSelectionListener(new SelectionAdapter() {
        //	public void widgetSelected(SelectionEvent e) {
        //		showAsBin = checkBin.isSelected();
        //        if (showAsBin) {
        //            checkScientificNotation.setSelected(false);
        //            checkCustomNotation.setSelected(false);
        //            checkHex.setSelected(false);
        //            showAsHex = false;
        //            numberFormat = normalFormat;
        //        }
        //        this.updateUI();
        //	}
        //});

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
            //	public void widgetSelected(SelectionEvent e) {
            //		firstPage();
            //	}
            //});

            // previous button
            //button = new Button(menuBar, SWT.PUSH);
            //button.setImage(ViewProperties.getPreviousIcon());
            //button.setToolTipText("Previous");
            //button.setMargin(margin);
            //button.addSelectionListener(new SelectionAdapter() {
            //	public void widgetSelected(SelectionEvent e) {
            //		previousPage();
            //	}
            //});

            //frameField = new Text(menuBar, SWT.SINGLE);
            //frameField.setText(String.valueOf(curFrame));
            //frameField.setMaximumSize(new Dimension(50, 30));
            //frameField.setMargin(margin);
            //frameField.addSelectionListener(new SelectionAdapter() {
            //	public void widgetSelected(SelectionEvent e) {
            //		int page = 0;
            //		
            //		try {
            //			page = Integer.parseInt(frameField.getText().trim()) - indexBase;
            //		}
            //		catch (Exception ex) {
            //			page = -1;
            //		}
            //		
            //		gotoPage(page);
            //	}
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
            //	public void widgetSelected(SelectionEvent e) {
            //		nextPage();
            //	}
            //});

            // last button
            //button = new Button(menuBar, SWT.PUSH);
            //button.setImage(ViewProperties.getLastIcon());
            //button.setToolTipText("Last");
            //button.setMargin(margin);
            //button.addSelectionListener(new SelectionAdapter() {
            //	public void widgetSelected(SelectionEvent e) {
            //		lastPage();
            //	}
            //});
        }

        return menuBar;
    }

    // Implementing DataView.
    public HObject getDataObject() {
        return dataset;
    }

    // Implementing DataObserver.
    private void previousPage() {
        if (dataset.getRank() < 3) return;

        long[] start = dataset.getStartDims();
        dataset.getDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        long idx = start[selectedIndex[2]];
        if (idx == 0) {
            return; // current page is the first page
        }

        gotoPage(start[selectedIndex[2]] - 1);
    }

    // Implementing DataObserver.
    private void nextPage() {
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

    // Implementing DataObserver.
    private void firstPage() {
    	if (dataset.getRank() < 3) return;

        long[] start = dataset.getStartDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        dataset.getDims();
        long idx = start[selectedIndex[2]];
        if (idx == 0) {
            return; // current page is the first page
        }

        gotoPage(0);
    }

    // Implementing DataObserver.
    private void lastPage() {
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

    // Implementing TableObserver.
    public Table getTable() {
        return table;
    }

    // Implementing TableObserver.
    private void showLineplot() {
    	/*
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();

        if ((rows == null) || (cols == null) || (rows.length <= 0) || (cols.length <= 0)) {
            shell.getDisplay().beep();
            showError("Select rows/columns to draw line plot.", shell.getText());
            return;
        }

        int nrow = table.getRowCount();
        int ncol = table.getColumnCount();

        log.trace("DefaultTableView showLineplot: {} - {}", nrow, ncol);
        LineplotOption lpo = new LineplotOption((JFrame) viewer, "Line Plot Options -- " + dataset.getName(), nrow, ncol);
        lpo.setVisible(true);

        int plotType = lpo.getPlotBy();
        if (plotType == LineplotOption.NO_PLOT) {
            return;
        }

        boolean isRowPlot = (plotType == LineplotOption.ROW_PLOT);
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
                JOptionPane.showMessageDialog(this, "More than 10 rows are selected.\n" + "The first 10 rows will be displayed.",
                        getTitle(), JOptionPane.WARNING_MESSAGE);
            }
            lineLabels = new String[nLines];
            data = new double[nLines][cols.length];

            double value = 0.0;
            for (int i = 0; i < nLines; i++) {
                lineLabels[i] = String.valueOf(rows[i] + indexBase);
                for (int j = 0; j < cols.length; j++) {
                    data[i][j] = 0;
                    try {
                        value = Double.parseDouble(table.getValueAt(rows[i], cols[j]).toString());
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
                        value = Double.parseDouble(table.getValueAt(xIndex, cols[j]).toString());
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
                lineLabels[j] = table.getColumnName(cols[j] + indexBase);
                for (int i = 0; i < rows.length; i++) {
                    data[j][i] = 0;
                    try {
                        value = Double.parseDouble(table.getValueAt(rows[i], cols[j]).toString());
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
                        value = Double.parseDouble(table.getValueAt(rows[j], xIndex).toString());
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

        Chart cv = new Chart((JFrame) viewer, title, Chart.LINEPLOT, data, xData, yRange);
        cv.setLineLabels(lineLabels);

        String cname = dataValue.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        if ((dname == 'B') || (dname == 'S') || (dname == 'I') || (dname == 'J')) {
            cv.setTypeToInteger();
        }

        cv.setVisible(true);
        */
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
    //@Override
    public Object getSelectedData() {
        if (dataset instanceof CompoundDS) {
            return getSelectedCompoundData();
        }
        else {
            return getSelectedScalarData();
        }
    }

    /**
     * Returns the selected data values.
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
     * Returns the selected data values.
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
     * Creates a JTable to hold a scalar dataset.
     */
    private Table createTable (ScalarDS d) {
        Table theTable = null;
        int rows = 0;
        int cols = 0;

        log.trace("createTable: ScalarDS start");
        int rank = d.getRank();
        if (rank <= 0) {
            try {
                d.init();
                log.trace("createTable: d.inited");
            }
            catch (Exception ex) {
            	showError(ex.getMessage(), "createTable:" + shell.getText());
            	dataValue = null;
                return null;
            }

            rank = d.getRank();
        }
        long[] dims = d.getSelectedDims();

        rows = (int) dims[0];
        cols = 1;
        if (rank > 1) {
            rows = d.getHeight();
            cols = d.getWidth();
        }

        log.trace("createTable: rows={} : cols={}", rows, cols);
        dataValue = null;
        try {
            dataValue = d.getData();
            if (dataValue == null) {
            	showError("No data read", "ScalarDS createTable:" + shell.getText());
                return null;
            }

            log.trace("createTable: dataValue={}", dataValue);
            /*
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
            */

            d.convertFromUnsignedC();
            dataValue = d.getData();

            if (Array.getLength(dataValue) <= rows) cols = 1;
        }
        catch (Throwable ex) {
        	showError(ex.getMessage(), "ScalarDS createTable:" + shell.getText());
            dataValue = null;
        }

        if (dataValue == null) {
            return null;
        }

        fillValue = d.getFillValue();
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

        /*
        AbstractTableModel tm = new AbstractTableModel() {
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

            //@Override
            public int getColumnCount ( ) {
                return columnNames.length;
            }

            //@Override
            public int getRowCount ( ) {
                return rowCount;
            }

            @Override
            public String getColumnName (int col) {
                return columnNames[col];
            }

            //@Override
            public Object getValueAt (int row, int column) {
                if (startEditing[0]) return "";
                log.trace("ScalarDS:createTable:AbstractTableModel:getValueAt({},{}) start", row, column);
                log.trace("ScalarDS:createTable:AbstractTableModel:getValueAt isInt={} isArray={} showAsHex={} showAsBin={}", isInt, isArray, showAsHex, showAsBin);

                if (isArray) {
                    // ARRAY dataset
                    int arraySize = dtype.getDatatypeSize() / btype.getDatatypeSize();
                    log.trace("ScalarDS:createTable:AbstractTableModel:getValueAt ARRAY dataset size={} isDisplayTypeChar={} isUINT64={}",
                            arraySize, isDisplayTypeChar, isUINT64);

                    stringBuffer.setLength(0); // clear the old string
                    int i0 = (row * colCount + column) * arraySize;
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
                    int index = column * rowCount + row;

                    if (dataset.getRank() > 1) {
                        log.trace("ScalarDS:createTable:AbstractTableModel:getValueAt rank={} isDataTransposed={} isNaturalOrder={}", dataset.getRank(), isDataTransposed, isNaturalOrder);
                        if ((isDataTransposed && isNaturalOrder) || (!isDataTransposed && !isNaturalOrder))
                            index = column * rowCount + row;
                        else
                            index = row * colCount + column;
                    }
                    log.trace("ScalarDS:createTable:AbstractTableModel:getValueAt index={} isStr={} isUINT64={}", index, isStr, isUINT64);
 
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
                        log.trace("ScalarDS:createTable:AbstractTableModel:getValueAt() theValue[{}]={}", index, theValue.toString());
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
                            log.trace("ScalarDS::createTable:AbstractTableModel:getValueAt() hexChars[{}]={}", x, hexChars);
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

                log.trace("ScalarDS:createTable:AbstractTableModel:getValueAt finish");
                return theValue;
            } // getValueAt(int row, int column)
        };
        */

        /*
        theTable = new JTable(tm) {
            private static final long serialVersionUID = -145476220959400488L;
            private final Datatype    dtype            = dataset.getDatatype();
            private final boolean     isArray          = (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY);

            @Override
            public boolean isCellEditable (int row, int col) {
                if (isReadOnly || isDisplayTypeChar || isArray || showAsBin || showAsHex) {
                    return false;
                }
                else {
                    return true;
                }
            }

            @Override
            public boolean editCellAt (int row, int column, java.util.EventObject e) {
                if (!isCellEditable(row, column)) {
                    return super.editCellAt(row, column, e);
                }

                if (e instanceof KeyEvent) {
                    KeyEvent ke = (KeyEvent) e;
                    if (ke.getID() == KeyEvent.KEY_PRESSED) {
                        startEditing[0] = true;
                    }
                }
                else if (e instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) e;
                    int mc = me.getClickCount();
                    if (mc > 1) {
                        currentEditingCellValue = getValueAt(row, column);
                    }
                }

                return super.editCellAt(row, column, e);
            }

            @Override
            public void editingStopped (ChangeEvent e) {
                int row = getEditingRow();
                int col = getEditingColumn();
                super.editingStopped(e);
                startEditing[0] = false;

                Object source = e.getSource();

                if (source instanceof CellEditor) {
                    CellEditor editor = (CellEditor) source;
                    String cellValue = (String) editor.getCellEditorValue();

                    try {
                        updateValueInMemory(cellValue, row, col);
                    }
                    catch (Exception ex) {
                        toolkit.beep();
                        JOptionPane.showMessageDialog(this, ex, getTitle(), JOptionPane.ERROR_MESSAGE);
                    }
                } // if (source instanceof CellEditor)
            }

            @Override
            public boolean isCellSelected (int row, int column) {
                if ((getSelectedRow() == row) && (getSelectedColumn() == column)) {
                    cellLabel.setText(String.valueOf(rowStart + indexBase + row * rowStride) + ", " + table.getColumnName(column)
                            + "  =  ");

                    log.trace("JTable.ScalarDS isCellSelected isRegRef={} isObjRef={}", isRegRef, isObjRef);
                    Object val = getValueAt(row, column);
                    String strVal = null;

                    if (isRegRef) {
                        boolean displayValues = ViewProperties.showRegRefValues();
                        log.trace("JTable.ScalarDS isCellSelected displayValues={}", displayValues);
                        if (displayValues && val != null && ((String) val).compareTo("NULL") != 0) {
                            String reg = (String) val;
                            boolean isPointSelection = (reg.indexOf('-') <= 0);

                            // find the object location
                            String oidStr = reg.substring(reg.indexOf('/'), reg.indexOf(' '));
                            log.trace("JTable.ScalarDS isCellSelected: isPointSelection={} oidStr={}", isPointSelection, oidStr);

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
                                    log.trace("JTable.ScalarDS isCellSelected: nSelections={}", nSelections);

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
                                            log.trace("JTable.ScalarDS isCellSelected: st.hasMoreTokens() begin");

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
                                            log.trace("JTable.ScalarDS isCellSelected: selectionSB={}", selectionSB);

                                            token = token.replace('(', ' ');
                                            token = token.replace(')', ' ');
                                            if (isPointSelection) {
                                                // point selection
                                                String[] tmp = token.split(",");
                                                for (int x = 0; x < tmp.length; x++) {
                                                    count[x] = 1;
                                                    sizeStr = tmp[x].trim();
                                                    start[x] = Long.valueOf(sizeStr);
                                                    log.trace("JTable.ScalarDS isCellSelected: point sel={}", tmp[x]);
                                                }
                                            }
                                            else {
                                                // rectangle selection
                                                String startStr = token.substring(0, token.indexOf('-'));
                                                String endStr = token.substring(token.indexOf('-') + 1);
                                                log.trace("JTable.ScalarDS isCellSelected: rect sel with startStr={} endStr={}",
                                                        startStr, endStr);
                                                String[] tmp = startStr.split(",");
                                                log.trace("JTable.ScalarDS isCellSelected: tmp with length={} rank={}", tmp.length,
                                                        rank);
                                                for (int x = 0; x < tmp.length; x++) {
                                                    sizeStr = tmp[x].trim();
                                                    start[x] = Long.valueOf(sizeStr);
                                                    log.trace("JTable.ScalarDS isCellSelected: rect start={}", tmp[x]);
                                                }
                                                tmp = endStr.split(",");
                                                for (int x = 0; x < tmp.length; x++) {
                                                    sizeStr = tmp[x].trim();
                                                    count[x] = Long.valueOf(sizeStr) - start[x] + 1;
                                                    log.trace("JTable.ScalarDS isCellSelected: rect end={} count={}", tmp[x],
                                                            count[x]);
                                                }
                                            }
                                            log.trace("JTable.ScalarDS isCellSelected: selection inited");

                                            Object dbuf = null;
                                            try {
                                                dbuf = dset.getData();
                                            }
                                            catch (Exception ex) {
                                                JOptionPane.showMessageDialog(this, ex, "Region Reference:" + getTitle(),
                                                        JOptionPane.ERROR_MESSAGE);
                                            }

                                            // Convert dbuf to a displayable
                                            // string
                                            String cName = dbuf.getClass().getName();
                                            int cIndex = cName.lastIndexOf("[");
                                            if (cIndex >= 0) {
                                                NT = cName.charAt(cIndex + 1);
                                            }
                                            log.trace("JTable.ScalarDS isCellSelected: cName={} NT={}", cName, NT);

                                            if (idx > 0) strvalSB.append(',');

                                            // convert numerical data into char
                                            // only possible cases are byte[]
                                            // and short[] (converted from
                                            // unsigned
                                            // byte)
                                            Datatype dtype = dset.getDatatype();
                                            Datatype baseType = dtype.getBasetype();
                                            log.trace("JTable.ScalarDS isCellSelected: dtype={} baseType={}",
                                                    dtype.getDatatypeDescription(), baseType);
                                            if (baseType == null) baseType = dtype;
                                            if ((dtype.getDatatypeClass() == Datatype.CLASS_ARRAY && baseType.getDatatypeClass() == Datatype.CLASS_CHAR)
                                                    && ((NT == 'B') || (NT == 'S'))) {
                                                int n = Array.getLength(dbuf);
                                                log.trace("JTable.ScalarDS isCellSelected charData length = {}", n);
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
                                                log.trace("JTable.ScalarDS isCellSelected charData");// =
                                                                                                     // {}",
                                                                                                     // strvalSB);
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
                                                log.trace("JTable.ScalarDS isCellSelected byteString");// =
                                                                                                       // {}",
                                                                                                       // strvalSB);
                                            }
                                            idx++;
                                            dset.clearData();
                                            log.trace("JTable.ScalarDS isCellSelected: st.hasMoreTokens() end");// strvalSB
                                                                                                                // =
                                                                                                                // {}",
                                                                                                                // strvalSB);
                                        } // while (st.hasMoreTokens())
                                        strVal = strvalSB.toString();
                                        log.trace("JTable.ScalarDS isCellSelected: st.hasMoreTokens() end");// value
                                                                                                            // =
                                                                                                            // {}",
                                                                                                            // strVal);
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

                    log.trace("JTable.ScalarDS isCellSelected finish");// value
                                                                       // =
                                                                       // {}",strVal);
                    cellValueField.setText(strVal);
                }

                return super.isCellSelected(row, column);
            }
        };
        */
        
        //theTable.setName("ScalarDS");

        //log.trace("createTable: ScalarDS finish");
        //return theTable;
        
        return null; // Remove when fixed
    }

    /**
     * Creates a Table to hold a compound dataset.
     */
    private Table createTable (CompoundDS d) {
        Table theTable = null;
        log.trace("createTable: CompoundDS start");

        if (d.getRank() <= 0) d.init();

        long[] startArray = d.getStartDims();
        long[] strideArray = d.getStride();
        int[] selectedIndex = d.getSelectedIndex();
        final int rowStart = (int) startArray[selectedIndex[0]];
        final int rowStride = (int) strideArray[selectedIndex[0]];

        // use lazy convert for large number of strings
        if (d.getHeight() > 10000) {
            d.setConvertByteToString(false);
        }

        dataValue = null;
        try {
            dataValue = d.getData();
        }
        catch (Throwable ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), "TableView " + shell.getText());
            dataValue = null;
        }

        if ((dataValue == null) || !(dataValue instanceof List)) {
            return null;
        }

        final int rows = d.getHeight();
        int cols = d.getSelectedMemberCount();
        String[] columnNames = new String[cols];

        int idx = 0;
        String[] columnNamesAll = d.getMemberNames();
        for (int i = 0; i < columnNamesAll.length; i++) {
            if (d.isMemberSelected(i)) {
                columnNames[idx] = columnNamesAll[i];
                columnNames[idx] = columnNames[idx].replaceAll(CompoundDS.separator, "->");
                idx++;
            }
        }

        String[] subColumnNames = columnNames;
        int columns = d.getWidth();
        if (columns > 1) {
            // multi-dimension compound dataset
            subColumnNames = new String[columns * columnNames.length];
            int halfIdx = columnNames.length / 2;
            for (int i = 0; i < columns; i++) {
                for (int j = 0; j < columnNames.length; j++) {
                    // display column index only once, in the middle of the
                    // compound fields
                    if (j == halfIdx) {
                        // subColumnNames[i * columnNames.length + j] = (i + 1)
                        // + "\n " + columnNames[j];
                        subColumnNames[i * columnNames.length + j] = (i + indexBase) + "\n " + columnNames[j];
                    }
                    else {
                        subColumnNames[i * columnNames.length + j] = " \n " + columnNames[j];
                    }
                }
            }
        }

        final String[] allColumnNames = subColumnNames;
        /*
        AbstractTableModel tm = new AbstractTableModel() {
            private static final long serialVersionUID = -2176296469630678304L;
            CompoundDS                compound         = (CompoundDS) dataset;
            int                       orders[]         = compound.getSelectedMemberOrders();
            Datatype                  types[]          = compound.getSelectedMemberTypes();
            StringBuffer              stringBuffer     = new StringBuffer();
            int                       nFields          = ((List<?>) dataValue).size();
            int                       nRows            = getRowCount();
            int                       nSubColumns      = (nFields > 0) ? getColumnCount() / nFields : 0;

            //@Override
            public int getColumnCount ( ) {
                return allColumnNames.length;
            }

            //@Override
            public int getRowCount ( ) {
                return rows;
            }

            @Override
            public String getColumnName (int col) {
                return allColumnNames[col];
            }

            //@Override
            public Object getValueAt (int row, int col) {
                if (startEditing[0]) return "";

                int fieldIdx = col;
                int rowIdx = row;
                char CNT = ' ';
                boolean CshowAsHex = false;
                boolean CshowAsBin = false;
                log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt({},{}) start", row, col);

                if (nSubColumns > 1) { // multi-dimension compound dataset
                    int colIdx = col / nFields;
                    fieldIdx = col - colIdx * nFields;
                    // BUG 573: rowIdx = row * orders[fieldIdx] + colIdx * nRows
                    // * orders[fieldIdx];
                    rowIdx = row * orders[fieldIdx] * nSubColumns + colIdx * orders[fieldIdx];
                    log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() row={} orders[{}]={} nSubColumns={} colIdx={}", row, fieldIdx, orders[fieldIdx], nSubColumns, colIdx);
                }
                else {
                    rowIdx = row * orders[fieldIdx];
                    log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() row={} orders[{}]={}", row, fieldIdx, orders[fieldIdx]);
                }
                log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() rowIdx={}", rowIdx);

                Object colValue = ((List<?>) dataValue).get(fieldIdx);
                if (colValue == null) {
                    return "Null";
                }

                stringBuffer.setLength(0); // clear the old string
                boolean isString = (types[fieldIdx].getDatatypeClass() == Datatype.CLASS_STRING);
                if (isString && (colValue instanceof byte[])) {
                    // strings
                    int strlen = types[fieldIdx].getDatatypeSize();
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
                    log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt(): cName={} CNT={}", cName, CNT);

                    boolean isUINT64 = false;
                    boolean isInt = (CNT == 'B' || CNT == 'S' || CNT == 'I' || CNT == 'J');
                    int typeSize = dtype.getDatatypeSize();
                    
                    if ((dtype.getDatatypeClass() == Datatype.CLASS_BITFIELD) || (dtype.getDatatypeClass() == Datatype.CLASS_OPAQUE)) {
                        CshowAsHex = true;
                        log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() class={} (BITFIELD or OPAQUE)", dtype.getDatatypeClass());
                    }
                    if (dtype.isUnsigned()) {
                        if (cIndex >= 0) {
                            isUINT64 = (cName.charAt(cIndex + 1) == 'J');
                        }
                    }
                    log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() isUINT64={} isInt={} CshowAsHex={} typeSize={}", isUINT64, isInt, CshowAsHex, typeSize);

                    for (int i = 0; i < orders[fieldIdx]; i++) {
                        if (isUINT64) {
                            Object theValue = Array.get(colValue, rowIdx + i);
                            log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() theValue[{}]={}", i, theValue.toString());
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
                            log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() theValue[{}]={}", i, theValue.toString());
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
                                log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() hexChars[{}]={}", x, hexChars);
                            }
                        }
                        else if (showAsBin && isInt) {
                            Object theValue = Array.get(colValue, rowIdx + typeSize * i);
                            log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() theValue[{}]={}", i, theValue.toString());
                            theValue = Tools.toBinaryString(Long.valueOf(theValue.toString()), typeSize);
                            if (i > 0) stringBuffer.append(", ");
                            stringBuffer.append(theValue);
                        }
                        else if (numberFormat != null) {
                            // show in scientific format
                            Object theValue = Array.get(colValue, rowIdx + i);
                            log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() theValue[{}]={}", i, theValue.toString());
                            theValue = numberFormat.format(theValue);
                            if (i > 0) stringBuffer.append(", ");
                            stringBuffer.append(theValue);
                        }
                        else {
                            Object theValue = Array.get(colValue, rowIdx + i);
                            log.trace("CompoundDS:createTable:AbstractTableModel:getValueAt() theValue[{}]={}", i, theValue.toString());
                            if (i > 0) stringBuffer.append(", ");
                            stringBuffer.append(theValue);
                        }
                    }
                } // end of else {

                return stringBuffer;
            }
        };
        */

        /*
        theTable = new JTable(tm) {
            private static final long serialVersionUID   = 3221288637329958074L;
            int                       lastSelectedRow    = -1;
            int                       lastSelectedColumn = -1;

            @Override
            public boolean isCellEditable (int row, int column) {
                return !isReadOnly;
            }

            @Override
            public boolean editCellAt (int row, int column, java.util.EventObject e) {
                if (!isCellEditable(row, column)) {
                    return super.editCellAt(row, column, e);
                }

                if (e instanceof KeyEvent) {
                    KeyEvent ke = (KeyEvent) e;
                    if (ke.getID() == KeyEvent.KEY_PRESSED) startEditing[0] = true;
                }
                else if (e instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) e;
                    int mc = me.getClickCount();
                    if (mc > 1) {
                        currentEditingCellValue = getValueAt(row, column);
                    }
                }

                return super.editCellAt(row, column, e);
            }

            @Override
            public void editingStopped (ChangeEvent e) {
                int row = getEditingRow();
                int col = getEditingColumn();
                super.editingStopped(e);
                startEditing[0] = false;

                Object source = e.getSource();

                if (source instanceof CellEditor) {
                    CellEditor editor = (CellEditor) source;
                    String cellValue = (String) editor.getCellEditorValue();

                    try {
                        updateValueInMemory(cellValue, row, col);
                    }
                    catch (Exception ex) {
                        toolkit.beep();
                        JOptionPane.showMessageDialog(this, ex, getTitle(), JOptionPane.ERROR_MESSAGE);
                    }
                } // if (source instanceof CellEditor)
            }

            @Override
            public boolean isCellSelected (int row, int column) {
                if ((lastSelectedRow == row) && (lastSelectedColumn == column)) {
                    return super.isCellSelected(row, column);
                }
                log.trace("JTable.CompoundDS isCellSelected row={} column={}", row, column);

                lastSelectedRow = row;
                lastSelectedColumn = column;
                if ((getSelectedRow() == row) && (getSelectedColumn() == column)) {
                    cellLabel.setText(String.valueOf(rowStart + indexBase + row * rowStride) + ", " + table.getColumnName(column)
                            + "  =  ");
                    cellValueField.setText(getValueAt(row, column).toString());
                }

                return super.isCellSelected(row, column);
            }
        };
        */

        /*
        if (columns > 1) {
            // multi-dimension compound dataset
            MultiLineHeaderRenderer renderer = new MultiLineHeaderRenderer(columns, columnNames.length);
            Enumeration<?> local_enum = theTable.getColumnModel().getColumns();
            while (local_enum.hasMoreElements()) {
                ((TableColumn) local_enum.nextElement()).setHeaderRenderer(renderer);
            }
        }
        */
        
        //theTable.setName("CompoundDS");

        //log.trace("createTable: CompoundDS finish");
        //return theTable;
        
        return null; // Remove when fixed
    } /* createTable */

    private void gotoPage (long idx) {
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
            showError("Frame number must be between" + indexBase + " and " + (dims[selectedIndex[2]] - 1 + indexBase), shell.getText());
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
        //updateUI();
    }

    /** Copy data from the spreadsheet to the system clipboard. */
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

    /** Paste data from the system clipboard to the spreadsheet. */
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
     * Import data values from text file.
     */
    private void importTextData (String fname) {
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

        // start at the first column for compound datasets
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
            c = 0;
            r++;
        } // while ((line != null) && (r < rows))

        try {
            in.close();
        }
        catch (IOException ex) {
            log.debug("close text file {}:", fname, ex);
        }

        table.updateUI();
        */
    }

    /**
     * Import data values from binary file.
     */
    private void importBinaryData() {
    	/*
        String currentDir = dataset.getFileFormat().getParent();
        JFileChooser fchooser = new JFileChooser(currentDir);
        fchooser.setFileFilter(DefaultFileFilter.getFileFilterBinary());
        int returnVal = fchooser.showOpenDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File choosedFile = fchooser.getSelectedFile();
        if (choosedFile == null) {
            return;
        }
        String fname = choosedFile.getAbsolutePath();

        
        MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(shell.getText());
        confirm.setMessage("Do you want to paste selected data?");
        if (confirm.open() == SWT.NO) return;

        getBinaryDatafromFile(fname);
        */
    }

    /** Reads data from a binary file into a buffer and updates table. */
    private void getBinaryDatafromFile (String fileName) {
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
        
        //table.updateUI();
    }

    /** Save data as text. */
    private void saveAsText() throws Exception {
    	/*
        final JFileChooser fchooser = new JFileChooser(dataset.getFile());
        fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
        // fchooser.changeToParentDirectory();
        fchooser.setDialogTitle("Save Current Data To Text File --- " + dataset.getName());

        File choosedFile = new File(dataset.getName() + ".txt");

        fchooser.setSelectedFile(choosedFile);
        int returnVal = fchooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        choosedFile = fchooser.getSelectedFile();
        if (choosedFile == null) {
            return;
        }
        String fname = choosedFile.getAbsolutePath();
        log.trace("DefaultTableView saveAsText: file={}", fname);

        // check if the file is in use
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

        if (choosedFile.exists()) {
        	MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        	confirm.setText(shell.getText());
        	confirm.setMessage("File exists. Do you want to replace it?");
        	if (confirm.open() == SWT.NO) return;
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(choosedFile)));

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

        int cols = table.getColumnCount();
        int rows = table.getRowCount();

        for (int i = 0; i < rows; i++) {
            out.print(table.getValueAt(i, 0));
            for (int j = 1; j < cols; j++) {
                out.print(delimiter);
                out.print(table.getValueAt(i, j));
            }
            out.println();
        }

        out.flush();
        out.close();

        viewer.showStatus("Data save to: " + fname);
        */
    }

    /** Save data as binary. */
    private void saveAsBinary() throws Exception {
    	/*
        final JFileChooser fchooser = new JFileChooser(dataset.getFile());
        fchooser.setFileFilter(DefaultFileFilter.getFileFilterBinary());
        // fchooser.changeToParentDirectory();
        fchooser.setDialogTitle("Save Current Data To Binary File --- " + dataset.getName());

        File choosedFile = new File(dataset.getName() + ".bin");
        fchooser.setSelectedFile(choosedFile);
        int returnVal = fchooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        choosedFile = fchooser.getSelectedFile();
        if (choosedFile == null) {
            return;
        }
        String fname = choosedFile.getAbsolutePath();
        log.trace("DefaultTableView saveAsBinary: file={}", fname);

        // check if the file is in use
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

        // check if the file exists
        if (choosedFile.exists()) {
        	MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        	confirm.setText(shell.getText());
        	confirm.setMessage("File exists. Do you want to replace it?");
        	if (confirm.open() == SWT.NO) return;
        }

        FileOutputStream outputFile = new FileOutputStream(choosedFile);
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

        viewer.showStatus("Data save to: " + fname);
        */
    }

    /**
     * update dataset value in file. The change will go to file.
     */
    //@Override
    public void updateValueInFile() {
        log.trace("DefaultTableView updateValueInFile enter");
        if (isReadOnly || showAsBin || showAsHex) {
            return;
        }

        if (!isValueChanged) {
            return;
        }

        try {
            log.trace("DefaultTableView updateValueInFile write");
            dataset.write();
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), shell.getText());
            return;
        }

        isValueChanged = false;
        log.trace("DefaultTableView updateValueInFile exit");
    }

    /**
     * Selects all rows, columns, and cells in the table.
     */
    private void selectAll ( ) throws Exception {
        table.selectAll();
    }

    /**
     * Converting selected data based on predefined math functions.
     */
    private void mathConversion ( ) throws Exception {
    	/*
        if (isReadOnly) {
            return;
        }

        int cols = table.getSelectedColumnCount();
        // if (!(dataset instanceof ScalarDS)) return;
        if ((dataset instanceof CompoundDS) && (cols > 1)) {
            shell.getDisplay().beep();
            showError("Please select one colunm a time for math conversion for compound dataset.", shell.getText());
            return;
        }

        Object theData = getSelectedData();
        if (theData == null) {
            shell.getDisplay().beep();
            showError("No data is selected.", shell.getText());
            return;
        }

        MathConversionDialog dialog = new MathConversionDialog((JFrame) viewer, theData);
        dialog.setVisible(true);

        if (dialog.isConverted()) {
            if (dataset instanceof CompoundDS) {
                Object colData = null;
                try {
                    colData = ((List<?>) dataset.getData()).get(table.getSelectedColumn());
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
                int rows = table.getSelectedRowCount();
                int r0 = table.getSelectedRow();
                int c0 = table.getSelectedColumn();
                int w = table.getColumnCount();
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
            //table.updateUI();
            isValueChanged = true;
        }
		*/
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
    private void updateValueInMemory (String cellValue, int row, int col) throws Exception {
        log.trace("DefaultTableView updateValueInMemory");
        if (currentEditingCellValue != null) {
            // data values are the same, no need to change the data
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
    	/*
        if (!(dataset instanceof ScalarDS) || (cellValue == null) || ((cellValue = cellValue.trim()) == null) || showAsBin
                || showAsHex) {
            return;
        }

        int i = 0;
        if (isDataTransposed) {
            i = col * table.getRowCount() + row;
        }
        else {
            i = row * table.getColumnCount() + col;
        }
        log.trace("DefaultTableView updateScalarData {} NT={}", cellValue, NT);

        ScalarDS sds = (ScalarDS) dataset;
        boolean isUnsigned = sds.isUnsigned();
        String cname = dataset.getOriginalClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        log.trace("updateScalarData isUnsigned={} cname={} dname={}", isUnsigned, cname, dname);

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
        */
    }

    private void updateCompoundData (String cellValue, int row, int col) throws Exception {
    	/*
        if (!(dataset instanceof CompoundDS) || (cellValue == null) || ((cellValue = cellValue.trim()) == null)) {
            return;
        }
        log.trace("DefaultTableView updateCompoundData");

        CompoundDS compDS = (CompoundDS) dataset;
        List<?> cdata = (List<?>) compDS.getData();
        int orders[] = compDS.getSelectedMemberOrders();
        Datatype types[] = compDS.getSelectedMemberTypes();
        int nFields = cdata.size();
        int nSubColumns = table.getColumnCount() / nFields;
        table.getRowCount();
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
            int strlen = types[column].getDatatypeSize();
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
            showError("Number of data point < " + morder + ".", shell.getText());
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
        */
    }

    private class LineplotOption
    //extends JDialog implements ActionListener, ItemListener 
    {
    	/*
        private static final long serialVersionUID = -3457035832213978906L;
        public static final int   NO_PLOT          = -1;
        public static final int   ROW_PLOT         = 0;
        public static final int   COLUMN_PLOT      = 1;

        private int               idx_xaxis        = -1, plotType = -1;
        private JRadioButton      rowButton, colButton;
        @SuppressWarnings("rawtypes")
        private JComboBox         rowBox, colBox;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public LineplotOption(JFrame owner, String title, int nrow, int ncol) {
            super(owner, title, true);

            rowBox = new JComboBox();
            rowBox.setEditable(false);
            colBox = new JComboBox();
            colBox.setEditable(false);

            JPanel contentPane = (JPanel) this.getContentPane();
            contentPane.setPreferredSize(new Dimension(400, 150));
            contentPane.setLayout(new BorderLayout(10, 10));

            long[] startArray = dataset.getStartDims();
            long[] strideArray = dataset.getStride();
            int[] selectedIndex = dataset.getSelectedIndex();
            int start = (int) startArray[selectedIndex[0]];
            int stride = (int) strideArray[selectedIndex[0]];

            rowBox.addItem("array index");
            for (int i = 0; i < nrow; i++) {
                rowBox.addItem("row " + (start + indexBase + i * stride));
            }

            colBox.addItem("array index");
            for (int i = 0; i < ncol; i++) {
                colBox.addItem("column " + table.getColumnName(i));
            }

            rowButton = new JRadioButton("Row");
            colButton = new JRadioButton("Column", true);
            rowButton.addItemListener(this);
            colButton.addItemListener(this);
            ButtonGroup rgroup = new ButtonGroup();
            rgroup.add(rowButton);
            rgroup.add(colButton);

            JPanel p1 = new JPanel();
            p1.setLayout(new GridLayout(2, 1, 5, 5));
            p1.add(new JLabel(" Series in:", SwingConstants.RIGHT));
            p1.add(new JLabel(" For abscissa use:", SwingConstants.RIGHT));

            JPanel p2 = new JPanel();
            p2.setLayout(new GridLayout(2, 1, 5, 5));
            // p2.setBorder(new LineBorder(Color.lightGray));
            p2.add(colButton);
            p2.add(colBox);

            JPanel p3 = new JPanel();
            p3.setLayout(new GridLayout(2, 1, 5, 5));
            // p3.setBorder(new LineBorder(Color.lightGray));
            p3.add(rowButton);
            p3.add(rowBox);

            JPanel p = new JPanel();
            p.setBorder(new LineBorder(Color.lightGray));
            p.setLayout(new GridLayout(1, 3, 20, 5));
            p.add(p1);
            p.add(p2);
            p.add(p3);

            JPanel bp = new JPanel();

            JButton okButton = new JButton("Ok");
            okButton.addActionListener(this);
            okButton.setActionCommand("Ok");
            bp.add(okButton);

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(this);
            cancelButton.setActionCommand("Cancel");
            bp.add(cancelButton);

            contentPane.add(new JLabel(" Select plot options:"), BorderLayout.NORTH);
            contentPane.add(p, BorderLayout.CENTER);
            contentPane.add(bp, BorderLayout.SOUTH);

            colBox.setEnabled(colButton.isSelected());
            rowBox.setEnabled(rowButton.isSelected());

            Point l = getParent().getLocation();
            l.x += 450;
            l.y += 200;
            setLocation(l);
            pack();
        }

        int getXindex ( ) {
            return idx_xaxis;
        }

        int getPlotBy ( ) {
            return plotType;
        }

        //@Override
        public void actionPerformed (ActionEvent e) {
            e.getSource();
            String cmd = e.getActionCommand();

            if (cmd.equals("Cancel")) {
                plotType = NO_PLOT;
                this.dispose(); // terminate the application
            }
            else if (cmd.equals("Ok")) {
                if (colButton.isSelected()) {
                    idx_xaxis = colBox.getSelectedIndex() - 1;
                    plotType = COLUMN_PLOT;
                }
                else {
                    idx_xaxis = rowBox.getSelectedIndex() - 1;
                    plotType = ROW_PLOT;
                }

                this.dispose(); // terminate the application
            }
        }

        //@Override
        public void itemStateChanged (ItemEvent e) {
            Object source = e.getSource();

            if (source.equals(colButton) || source.equals(rowButton)) {
                colBox.setEnabled(colButton.isSelected());
                rowBox.setEnabled(rowButton.isSelected());
            }
        }
        */
    }

    private class ColumnHeader
    //extends JTableHeader 
    {
    	/*
        private static final long serialVersionUID   = -3179653809792147055L;
        private int               currentColumnIndex = -1;
        private int               lastColumnIndex    = -1;
        private JTable            parentTable;

        public ColumnHeader(JTable theTable) {
            super(theTable.getColumnModel());

            parentTable = theTable;
            setReorderingAllowed(false);
        }

        @Override
        protected void processMouseMotionEvent (MouseEvent e) {
            super.processMouseMotionEvent(e);

            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                // do not do anything, just resize the column
                if (getResizingColumn() != null) return;

                int colEnd = columnAtPoint(e.getPoint());

                if (colEnd < 0) {
                    colEnd = 0;
                }
                if (currentColumnIndex < 0) {
                    currentColumnIndex = 0;
                }

                parentTable.clearSelection();

                if (colEnd > currentColumnIndex) {
                    parentTable.setColumnSelectionInterval(currentColumnIndex, colEnd);
                }
                else {
                    parentTable.setColumnSelectionInterval(colEnd, currentColumnIndex);
                }

                parentTable.setRowSelectionInterval(0, parentTable.getRowCount() - 1);
            }
        }

        @Override
        protected void processMouseEvent (MouseEvent e) {
            super.processMouseEvent(e);

            int mouseID = e.getID();

            if (mouseID == MouseEvent.MOUSE_CLICKED) {
                if (currentColumnIndex < 0) {
                    return;
                }

                if (e.isControlDown()) {
                    // select discontinuous columns
                    parentTable.addColumnSelectionInterval(currentColumnIndex, currentColumnIndex);
                }
                else if (e.isShiftDown()) {
                    // select continuous columns
                    if (lastColumnIndex < 0) {
                        parentTable.addColumnSelectionInterval(0, currentColumnIndex);
                    }
                    else if (lastColumnIndex < currentColumnIndex) {
                        parentTable.addColumnSelectionInterval(lastColumnIndex, currentColumnIndex);
                    }
                    else {
                        parentTable.addColumnSelectionInterval(currentColumnIndex, lastColumnIndex);
                    }
                }
                else {
                    // clear old selection and set new column selection
                    parentTable.clearSelection();
                    parentTable.setColumnSelectionInterval(currentColumnIndex, currentColumnIndex);
                }

                lastColumnIndex = currentColumnIndex;
                parentTable.setRowSelectionInterval(0, parentTable.getRowCount() - 1);
            }
            else if (mouseID == MouseEvent.MOUSE_PRESSED) {
                currentColumnIndex = columnAtPoint(e.getPoint());
            }
        }
        */
    } // private class ColumnHeader

    /** RowHeader defines the row header component of the Spreadsheet. */
    private class RowHeader 
    //extends JTable 
    {
    	/*
        private static final long serialVersionUID = -1548007702499873626L;
        private int               currentRowIndex  = -1;
        private int               lastRowIndex     = -1;
        private JTable            parentTable;
        */

    	/*
        public RowHeader(JTable pTable, Dataset dset) {
            // Create a JTable with the same number of rows as
            // the parent table and one column.
            // super( pTable.getRowCount(), 1 );

            final long[] startArray = dset.getStartDims();
            final long[] strideArray = dset.getStride();
            final int[] selectedIndex = dset.getSelectedIndex();
            final int start = (int) startArray[selectedIndex[0]];
            final int stride = (int) strideArray[selectedIndex[0]];
            final int rowCount = pTable.getRowCount();
            parentTable = pTable;

            AbstractTableModel tm = new AbstractTableModel() {
                private static final long serialVersionUID = -8117073107569884677L;

                //@Override
                public int getColumnCount ( ) {
                    return 1;
                }

                //@Override
                public int getRowCount ( ) {
                    return rowCount;
                }

                @Override
                public String getColumnName (int col) {
                    return " ";
                }

                //@Override
                public Object getValueAt (int row, int column) {
                    log.trace("RowHeader:AbstractTableModel:getValueAt");
                    return String.valueOf(start + indexBase + row * stride);
                }
            };

            this.setModel(tm);

            // Get the only table column.
            TableColumn col = getColumnModel().getColumn(0);

            // Use the cell renderer in the column.
            col.setCellRenderer(new RowHeaderRenderer());
        }
        */

        /** Overridden to return false since the headers are not editable. */
        /*
    	@Override
        public boolean isCellEditable (int row, int col) {
            return false;
        }
        */

        /** This is called when the selection changes in the row headers. */
        /*
    	@Override
        public void valueChanged (ListSelectionEvent e) {
            if (parentTable == null) {
                return;
            }

            int rows[] = getSelectedRows();
            if ((rows == null) || (rows.length == 0)) {
                return;
            }

            parentTable.clearSelection();
            parentTable.setRowSelectionInterval(rows[0], rows[rows.length - 1]);
            parentTable.setColumnSelectionInterval(0, parentTable.getColumnCount() - 1);
        }
        */

    	/*
        @Override
        protected void processMouseMotionEvent (MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                int colEnd = rowAtPoint(e.getPoint());

                if (colEnd < 0) {
                    colEnd = 0;
                }
                if (currentRowIndex < 0) {
                    currentRowIndex = 0;
                }

                parentTable.clearSelection();

                if (colEnd > currentRowIndex) {
                    parentTable.setRowSelectionInterval(currentRowIndex, colEnd);
                }
                else {
                    parentTable.setRowSelectionInterval(colEnd, currentRowIndex);
                }

                parentTable.setColumnSelectionInterval(0, parentTable.getColumnCount() - 1);
            }
        }
        */

		/*
        @Override
        protected void processMouseEvent (MouseEvent e) {
            int mouseID = e.getID();

            if (mouseID == MouseEvent.MOUSE_CLICKED) {
                if (currentRowIndex < 0) {
                    return;
                }

                if (e.isControlDown()) {
                    // select discontinuous rows
                    parentTable.addRowSelectionInterval(currentRowIndex, currentRowIndex);
                }
                else if (e.isShiftDown()) {
                    // select contiguous columns
                    if (lastRowIndex < 0) {
                        parentTable.addRowSelectionInterval(0, currentRowIndex);
                    }
                    else if (lastRowIndex < currentRowIndex) {
                        parentTable.addRowSelectionInterval(lastRowIndex, currentRowIndex);
                    }
                    else {
                        parentTable.addRowSelectionInterval(currentRowIndex, lastRowIndex);
                    }
                }
                else {
                    // clear old selection and set new column selection
                    parentTable.clearSelection();
                    parentTable.setRowSelectionInterval(currentRowIndex, currentRowIndex);
                }

                lastRowIndex = currentRowIndex;

                parentTable.setColumnSelectionInterval(0, parentTable.getColumnCount() - 1);
            }
            else if (mouseID == MouseEvent.MOUSE_PRESSED) {
                currentRowIndex = rowAtPoint(e.getPoint());
            }
        }
        */
    } // private class RowHeader extends JTable

    /**
     * RowHeaderRenderer is a custom cell renderer that displays cells as buttons.
     */
    private class RowHeaderRenderer
    //extends JLabel implements TableCellRenderer 
    {
        private static final long serialVersionUID = -8963879626159783226L;

        /*
        public RowHeaderRenderer( ) {
            super();
            setHorizontalAlignment(SwingConstants.CENTER);

            setOpaque(true);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setBackground(Color.lightGray);
        }
        */

        /** Configures the button for the current cell, and returns it. */
        /*
        @Override
        public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            setFont(table.getFont());

            if (value != null) {
                setText(value.toString());
            }

            return this;
        }
        */
    } // private class RowHeaderRenderer extends JLabel implements
      // TableCellRenderer

    @SuppressWarnings("rawtypes")
    private class MultiLineHeaderRenderer
    //extends JList implements TableCellRenderer 
    {
        /*
    	private static final long    serialVersionUID = -3697496960833719169L;
        private final CompoundBorder subBorder        = new CompoundBorder(new MatteBorder(1, 0, 1, 0, java.awt.Color.darkGray),
                                                              new MatteBorder(1, 0, 1, 0, java.awt.Color.white));
        private final CompoundBorder majorBorder      = new CompoundBorder(new MatteBorder(1, 1, 1, 0, java.awt.Color.darkGray),
                                                              new MatteBorder(1, 2, 1, 0, java.awt.Color.white));
        Vector<String>                       lines            = new Vector<String>();
        int                          nSubcolumns      = 1;

        public MultiLineHeaderRenderer(int majorColumns, int subColumns) {
            nSubcolumns = subColumns;
            setOpaque(true);
            setForeground(UIManager.getColor("TableHeader.foreground"));
            setBackground(UIManager.getColor("TableHeader.background"));
        }

        //@Override
        public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            setFont(table.getFont());
            String str = (value == null) ? "" : value.toString();
            BufferedReader br = new BufferedReader(new StringReader(str));
            String line;

            lines.clear();
            try {
                while ((line = br.readLine()) != null) {
                    lines.addElement(line);
                }
            }
            catch (IOException ex) {
                log.debug("string read:", ex);
            }

            if ((column / nSubcolumns) * nSubcolumns == column) {
                setBorder(majorBorder);
            }
            else {
                setBorder(subBorder);
            }
            setListData(lines);

            return this;
        }
        */
    }

    // ////////////////////////////////////////////////////////////////////////
    // //
    // The code below was added to deal with region references //
    // Peter Cao, 4/30/2009 //
    // //
    // ////////////////////////////////////////////////////////////////////////

    //@Override
    public void mouseClicked (MouseEvent e) {
        /*
    	// only deal with reg. ref
        if (!(isRegRef || isObjRef)) return;

        int eMod = e.getModifiers();

        // provide two options here: double click to show data in table, or
        // right mouse to choose to show data in table or in image

        // right mouse click
        if (e.isPopupTrigger()
                || (eMod == InputEvent.BUTTON3_MASK)
                || (System.getProperty("os.name").startsWith("Mac")
                && (eMod == (InputEvent.BUTTON1_MASK
                | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())))) {
            if (popupMenu != null) {
                popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
            }
        }
        else if (e.getClickCount() == 2) {
            // double click
            viewType = ViewType.TABLE;
            Object theData = null;
            try {
                theData = ((Dataset) getDataObject()).getData();
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
            }

            if (theData == null) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "No data selected.", getTitle(), JOptionPane.ERROR_MESSAGE);
                return;

            }

            int[] selectedRows = table.getSelectedRows();
            if (selectedRows == null || selectedRows.length <= 0) {
                return;
            }
            int len = Array.getLength(selectedRows);
            for (int i = 0; i < len; i++) {
                if (isRegRef)
                    showRegRefData((String) Array.get(theData, selectedRows[i]));
                else if (isObjRef) showObjRefData(Array.getLong(theData, selectedRows[i]));
            }
        }
        */
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

        Shell dataView = null;
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
            @SuppressWarnings("rawtypes")
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

            Shell dataView = null;
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
                dataView.setText(dataView.getText() + "; " + titleSB.toString());
            }
            log.trace("DefaultTableView showRegRefData: st.hasMoreTokens() end");
        } // while (st.hasMoreTokens())
    } // private void showRegRefData(String reg)
    
    private void showError(String errorMsg, String title) {
    	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
    	error.setText(title);
    	error.setMessage(errorMsg);
    	error.open();
    }
}