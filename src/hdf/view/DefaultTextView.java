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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.StreamPrintServiceFactory;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;

import hdf.object.Dataset;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * TextView displays an HDF string dataset in text.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DefaultTextView implements TextView {
    private static final long serialVersionUID = 3892752752951438428L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultTextView.class);

    /**
     * The main HDFView.
     */
    private final ViewManager viewer;
    
    private final Display display = Display.getCurrent();
    
    private final Shell shell;

    /**
     * The Scalar Dataset.
     */
    private ScalarDS dataset;

    /**
     * The string text.
     */
    private String[] text;

    /** The table to display the text content */
    private Table table;

    // Text areas to hold the text.
    private Text[] textAreas;

    private boolean isReadOnly = false;

    private boolean isTextChanged = false;

    private TextAreaEditor textEditor = null;

    private RowHeader rowHeaders = null;
    
    private int indexBase = 0;
    
    public DefaultTextView(Shell parent, ViewManager theView) {
    	this(parent, theView, null);
    }

    /**
     * Constructs an TextView.
     * <p>
     * 
     * @param theView
     *            the main HDFView.
     * @param map
     *            the properties on how to show the data. The map is used to
     *            allow applications to pass properties on how to display the
     *            data, such as, transposing data, showing data as character,
     *            applying bitmask, and etc. Predefined keys are listed at
     *            ViewProperties.DATA_VIEW_KEY.
     */
    public DefaultTextView(Shell parent, ViewManager theView, HashMap map) {
        shell = new Shell(display);
    	
    	viewer = theView;
        text = null;
        table = null;
        dataset = null;
        //textEditor = new TextAreaEditor(this);
        
        if (ViewProperties.isIndexBase1())
            indexBase = 1;

        HObject hobject = null;
        if (map != null)
            hobject = (HObject) map.get(ViewProperties.DATA_VIEW_KEY.OBJECT);
        else
            hobject = theView.getTreeView().getCurrentObject();

        if (!(hobject instanceof ScalarDS)) {
            return;
        }

        dataset = (ScalarDS) hobject;

        if (!dataset.isText()) {
            viewer.showStatus("Cannot display non-text dataset in text view.");
            dataset = null;
            return;
        }

        isReadOnly = dataset.getFileFormat().isReadOnly();

        try {
            text = (String[]) dataset.getData();
        } 
        catch (Exception ex) {
        	showError(ex.getMessage(), "TextView" + shell.getText());
            text = null; 
        }

        if (text == null) {
            viewer.showStatus("Loading text dataset failed - "
                    + dataset.getName());
            dataset = null;
            return;
        }
        
        String fname = new java.io.File(dataset.getFile()).getName();
        shell.setText("TextView  -  " + dataset.getName() + "  -  "
                + dataset.getPath() + "  -  " + fname);
        shell.setImage(ViewProperties.getTextIcon());
        
        int rank = dataset.getRank();
        long start[] = dataset.getStartDims();
        long count[] = dataset.getSelectedDims();
        
        String colName = "Data selection:   ["+start[0];
        for (int i=1; i<rank; i++) {
            colName += ", "+start[i];
        }
        colName += "] ~ ["+(start[0]+count[0]-1);
        for (int i=1; i<rank; i++) {
            colName += ", "+(start[i]+count[i]-1);
        }  
        colName += "]";

        table = createTable(colName);
        
        //JTableHeader colHeader = table.getTableHeader();
        //colHeader.setReorderingAllowed(false);
        //colHeader.setBackground(Color.black);
        
        //rowHeaders = new RowHeader(table, dataset);

        // add the table to a scroller
        ScrolledComposite scrollingTable = new ScrolledComposite(shell, SWT.H_SCROLL | SWT.V_SCROLL);
        scrollingTable.setContent(table);
        scrollingTable.getVerticalBar().setIncrement(100);
        scrollingTable.getHorizontalBar().setIncrement(100);

        //JViewport viewp = new JViewport();
        //viewp.add(rowHeaders);
        //viewp.setPreferredSize(rowHeaders.getPreferredSize());
        //scrollingTable.setRowHeader(viewp);

        //TableColumnModel cmodel = table.getColumnModel();
        TextAreaRenderer textAreaRenderer = new TextAreaRenderer();

        //cmodel.getColumn(0).setCellRenderer(textAreaRenderer);
        //cmodel.getColumn(0).setCellEditor(textEditor);

        shell.setMenuBar(createMenuBar());
        
        shell.addDisposeListener(new DisposeListener() {
        	public void widgetDisposed(DisposeEvent e) {
        		if (isTextChanged && !isReadOnly) {
                	MessageBox confirm = new MessageBox(shell, SWT.YES | SWT.NO);
                	confirm.setMessage("\""
                            + dataset.getName() + "\" has changed.\n"
                            + "Do you want to save the changes?");
                	confirm.setText(shell.getText());

                    if (confirm.open() == SWT.YES) {
                        updateValueInFile();
                    }
                }

                //viewer.removeDataView();
        	}
        });
        
        shell.open();
        
        while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
    }

    /**
     * Creates a Table to hold a compound dataset.
     */
    private Table createTable(final String colName) {
        Table theTable = null;
        
        /*
        AbstractTableModel tm =  new AbstractTableModel()
        {
            public int getColumnCount() {
                return 1;
            }

            public int getRowCount() {
                return text.length;
            }

            public String getColumnName(int col) {
                return colName;
            }

            public Object getValueAt(int row, int column)
            {
                return text[row];
            }
        };
        */
        
        /*
        theTable = new Table(tm) {
            private static final long serialVersionUID = -6571266777012522255L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return !isReadOnly;
            }

            @Override
            public void editingStopped(ChangeEvent e) {
                int row = getEditingRow();
                int col = getEditingColumn();
                super.editingStopped(e);

                Object source = e.getSource();

                if (source instanceof CellEditor) {
                    CellEditor editor = (CellEditor) source;
                    String cellValue = (String) editor.getCellEditorValue();
                    text[row] = cellValue;
                } // if (source instanceof CellEditor)
            }
        };
        */
        //theTable.setName("TextView");

        return theTable;
    }

    /*
    public void keyTyped(KeyEvent e) {
        isTextChanged = true;
    }
    */

    private Menu createMenuBar() {
        Menu menuBar = new Menu(shell, SWT.BAR);
        
        MenuItem textMenu = new MenuItem(menuBar, SWT.CASCADE);
        textMenu.setText("&Text");
        
        Menu menu = new Menu(shell, SWT.DROP_DOWN);
        textMenu.setMenu(menu);

        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("Save To &Text File");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		try {
        			saveAsText();
        		}
        		catch (Exception ex) {
        			showError(ex.getMessage(), shell.getText());
        		}
        	}
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Save Changes");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		updateValueInFile();
        	}
        });
        
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

    /**
     * Update dataset value in file. The change will go to file.
     */
    public void updateValueInFile() {
        if (isReadOnly) {
            return;
        }

        if (!(dataset instanceof ScalarDS)) {
            return;
        }

        if (!isTextChanged) {
            return;
        }

        //int row = table.getEditingRow();
        //if (row >= 0) {
            // make sure to update the current row
        //    String cellValue = (String) textEditor.getCellEditorValue();
        //    text[row] = cellValue;
        //}

        try {
            dataset.write();
        }
        catch (Exception ex) {
        	showError(ex.getMessage(), shell.getText());
            return;
        }
        
        isTextChanged = false;
    }

    /** Save data as text. */
    private void saveAsText() throws Exception {
    	/*
        final JFileChooser fchooser = new JFileChooser(dataset.getFile());
        fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
        fchooser.changeToParentDirectory();
        fchooser.setDialogTitle("Save Current Data To Text File --- "
                + dataset.getName());

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

        // check if the file is in use
        List fileList = viewer.getTreeView().getCurrentFiles();
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                	showError("Unable to save data to file \"" + fname
                            + "\". \nThe file is being used.", shell.getText());
                    return;
                }
            }
        }

        if (choosedFile.exists()) {
            int newFileFlag = JOptionPane.showConfirmDialog(this,
                    "File exists. Do you want to replace it ?",
                    this.getTitle(), JOptionPane.YES_NO_OPTION);
            if (newFileFlag == JOptionPane.NO_OPTION) {
                return;
            }
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
                choosedFile)));

        int rows = text.length;
        for (int i = 0; i < rows; i++) {
            out.print(text[i].trim());
            out.println();
            out.println();
        }

        out.flush();
        out.close();

        viewer.showStatus("Data save to: " + fname);

        try {
            RandomAccessFile rf = new RandomAccessFile(choosedFile, "r");
            long size = rf.length();
            rf.close();
            viewer.showStatus("File size (bytes): " + size);
        }
        catch (Exception ex) {
        	log.debug("raf file size:", ex);
        }
        */
    }

    // Implementing DataView.
    public HObject getDataObject() {
        return dataset;
    }

    // Implementing TextView.
    public String[] getContents() {
        return text;
    }

    // print the table
    private void print() {
        StreamPrintServiceFactory[] spsf = StreamPrintServiceFactory
                .lookupStreamPrintServiceFactories(null, null);
        for (int i = 0; i < spsf.length; i++) {
            System.out.println(spsf[i]);
        }
        DocFlavor[] docFlavors = spsf[0].getSupportedDocFlavors();
        for (int i = 0; i < docFlavors.length; i++) {
            System.out.println(docFlavors[i]);
        }

        // TODO: windows url
        // Get a text DocFlavor
        InputStream is = null;
        try {
            is = new BufferedInputStream(new java.io.FileInputStream(
                    "e:\\temp\\t.html"));
        }
        catch (Exception ex) {
        	log.debug("Get a text DocFlavor:", ex);
        }
        DocFlavor flavor = DocFlavor.STRING.TEXT_HTML;

        // Get all available print services
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null,
                null);

        // Print this job on the first print server
        DocPrintJob job = services[0].createPrintJob();
        Doc doc = new SimpleDoc(is, flavor, null);

        // Print it
        try {
            job.print(doc, null);
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }
    
    private void showError(String errorMsg, String title) {
    	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
    	error.setText(title);
    	error.setMessage(errorMsg);
    	error.open();
    }

    private class TextAreaRenderer
    //extends JTextArea implements TableCellRenderer 
    {
        private static final long serialVersionUID = -5869975162678521978L;

        //private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();

        /** map from table to map of rows to map of column heights */
        private final Map cellSizes = new HashMap();

        public TextAreaRenderer() {
            //setLineWrap(true);
            //setWrapStyleWord(true);
        }

        /*
        public Component getTableCellRendererComponent(
                //
                JTable table, Object obj, boolean isSelected, boolean hasFocus,
                int row, int column) {
            // set the colours, etc. using the standard for that platform
            adaptee.getTableCellRendererComponent(table, obj, isSelected,
                    hasFocus, row, column);
            setForeground(adaptee.getForeground());
            setBackground(adaptee.getBackground());
            setBorder(adaptee.getBorder());
            setFont(adaptee.getFont());
            setText(adaptee.getText());

            // This line was very important to get it working with JDK1.4
            TableColumnModel columnModel = table.getColumnModel();
            setSize(columnModel.getColumn(column).getWidth(), 100000);
            int height_wanted = (int) getPreferredSize().getHeight();
            addSize(table, row, column, height_wanted);
            height_wanted = findTotalMaximumRowSize(table, row);
            if (height_wanted != table.getRowHeight(row)) {
                table.setRowHeight(row, height_wanted);
                rowHeaders.setRowHeight(row, height_wanted);

            }
            return this;
        }
        */

        /*
        private void addSize(JTable table, int row, int column, int height) {
            Map rows = (Map) cellSizes.get(table);
            if (rows == null) {
                cellSizes.put(table, rows = new HashMap());
            }
            Map rowheights = (Map) rows.get(new Integer(row));
            if (rowheights == null) {
                rows.put(new Integer(row), rowheights = new HashMap());
            }
            rowheights.put(new Integer(column), new Integer(height));
        }
        */

        /**
         * Look through all columns and get the renderer. If it is also a
         * TextAreaRenderer, we look at the maximum height in its hash table for
         * this row.
         */
        /*
        private int findTotalMaximumRowSize(JTable table, int row) {
            int maximum_height = 0;
            Enumeration columns = table.getColumnModel().getColumns();
            while (columns.hasMoreElements()) {
                TableColumn tc = (TableColumn) columns.nextElement();
                TableCellRenderer cellRenderer = tc.getCellRenderer();
                if (cellRenderer instanceof TextAreaRenderer) {
                    TextAreaRenderer tar = (TextAreaRenderer) cellRenderer;
                    maximum_height = Math.max(maximum_height, tar
                            .findMaximumRowSize(table, row));
                }
            }
            return maximum_height;
        }
        */

        /*
        private int findMaximumRowSize(JTable table, int row) {
            Map rows = (Map) cellSizes.get(table);
            if (rows == null) {
                return 0;
            }
            Map rowheights = (Map) rows.get(new Integer(row));
            if (rowheights == null) {
                return 0;
            }
            int maximum_height = 0;
            for (Iterator it = rowheights.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                int cellHeight = ((Integer) entry.getValue()).intValue();
                maximum_height = Math.max(maximum_height, cellHeight);
            }
            return maximum_height;
        }
        */
    }

    private class TextAreaEditor
    //extends DefaultCellEditor 
    {
    	/*
        private static final long serialVersionUID = 1721646779892184957L;

        public TextAreaEditor(KeyListener keyListener) {
            super(new JTextField());

            final JTextArea textArea = new JTextArea();

            textArea.addKeyListener(keyListener);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(null);
            editorComponent = scrollPane;
            delegate = new DefaultCellEditor.EditorDelegate() {
                private static final long serialVersionUID = 7662356579385373160L;

                @Override
                public void setValue(Object value) {
                    textArea.setText((value != null) ? value.toString() : "");
                }

                @Override
                public Object getCellEditorValue() {
                    return textArea.getText();
                }
            };
        }
        */
    }

    /** RowHeader defines the row header component of the Spreadsheet. */
    private class RowHeader extends Table {
		private static final long serialVersionUID = 2572539746584274419L;
        private int currentRowIndex = -1;
        private int lastRowIndex = -1;
        private Table parentTable;
        
        // Only here for compile reasons
        public RowHeader(Composite parent, int style) {
			super(parent, style);
			// TODO Auto-generated constructor stub
		}
        
        /*
        public RowHeader(Table pTable, Dataset dset) {
            // Create a JTable with the same number of rows as
            // the parent table and one column.
            super(pTable.getRowCount(), 1);

            long[] startArray = dset.getStartDims();
            long[] strideArray = dset.getStride();
            int[] selectedIndex = dset.getSelectedIndex();
            int start = (int) startArray[selectedIndex[0]];
            int stride = (int) strideArray[selectedIndex[0]];

            // Store the parent table.
            parentTable = pTable;

            // Set the values of the row headers starting at 0.
            int n = parentTable.getRowCount();
            for (int i = 0; i < n; i++) {
                setValueAt(new Integer(start + indexBase+ i * stride), i, 0);
            }

            // Get the only table column.
            TableColumn col = getColumnModel().getColumn(0);

            // Use the cell renderer in the column.
            col.setCellRenderer(new RowHeaderRenderer());
        }
        */

        /** Overridden to return false since the headers are not editable. */
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        /** This is called when the selection changes in the row headers. */
        /*
        public void valueChanged(ListSelectionEvent e) {
            if (parentTable == null) {
                return;
            }

            int rows[] = getSelectedRows();
            if ((rows == null) || (rows.length == 0)) {
                return;
            }

            parentTable.clearSelection();
            parentTable.setRowSelectionInterval(rows[0], rows[rows.length - 1]);
            parentTable.setColumnSelectionInterval(0, parentTable
                    .getColumnCount() - 1);
        }
        */

        /*
        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
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
                    parentTable
                            .setRowSelectionInterval(currentRowIndex, colEnd);
                }
                else {
                    parentTable
                            .setRowSelectionInterval(colEnd, currentRowIndex);
                }

                parentTable.setColumnSelectionInterval(0, parentTable
                        .getColumnCount() - 1);
            }
        }
        */

        /*
        @Override
        protected void processMouseEvent(MouseEvent e) {
            int mouseID = e.getID();

            if (mouseID == MouseEvent.MOUSE_CLICKED) {
                if (currentRowIndex < 0) {
                    return;
                }

                if (e.isControlDown()) {
                    // select discontinguous rows
                    parentTable.addRowSelectionInterval(currentRowIndex,
                            currentRowIndex);
                }
                else if (e.isShiftDown()) {
                    // select continguous columns
                    if (lastRowIndex < 0) {
                        parentTable.addRowSelectionInterval(0, currentRowIndex);
                    }
                    else if (lastRowIndex < currentRowIndex) {
                        parentTable.addRowSelectionInterval(lastRowIndex,
                                currentRowIndex);
                    }
                    else {
                        parentTable.addRowSelectionInterval(currentRowIndex,
                                lastRowIndex);
                    }
                }
                else {
                    // clear old selection and set new column selection
                    parentTable.clearSelection();
                    parentTable.setRowSelectionInterval(currentRowIndex,
                            currentRowIndex);
                }

                lastRowIndex = currentRowIndex;

                parentTable.setColumnSelectionInterval(0, parentTable
                        .getColumnCount() - 1);
            }
            else if (mouseID == MouseEvent.MOUSE_PRESSED) {
                currentRowIndex = rowAtPoint(e.getPoint());
            }
        }
        */
    } // private class RowHeader extends JTable

    /**
     * RowHeaderRenderer is a custom cell renderer that displays cells as
     * buttons.
     */
    private class RowHeaderRenderer extends Label
    //implements TableCellRenderer
    {
        private static final long serialVersionUID = 3081275694689434654L;

        public RowHeaderRenderer() {
            super(shell, SWT.CENTER);

            //setOpaque(true);
            //setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setBackground(display.getSystemColor(SWT.COLOR_GRAY));
        }

        /** Configures the button for the current cell, and returns it. */
        /*
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
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
}
