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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * TextView displays an HDF string dataset in text.
 *
 * @author Jordan T. Henderson
 * @version 2.4 3/27/2016
 */
public class DefaultTextView implements TextView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultTextView.class);

    /**
     * The main HDFView.
     */
    private final ViewManager       viewer;

    private final Display           display = Display.getDefault();

    private final Shell             shell;

    private Font                    curFont;

    /**
     * The Scalar Dataset.
     */
    private ScalarDS                dataset;

    /**
     * The string text.
     */
    private String[]                text;

    /** The table to display the text content */
    private Table                   table;

    private boolean                 isReadOnly = false;

    private boolean                 isTextChanged = false;

    private RowHeader               rowHeaders = null;

    private int                     indexBase = 0;

    public DefaultTextView(ViewManager theView) {
        this(theView, null);
    }

    /**
     * Constructs a TextView.
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
    public DefaultTextView(ViewManager theView, HashMap map) {
        shell = new Shell(display, SWT.SHELL_TRIM);

        shell.setData(this);

        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (isTextChanged && !isReadOnly) {
                    if(MessageDialog.openConfirm(shell,
                                shell.getText(), "\"" + dataset.getName() + "\" has changed.\n" + "Do you want to save the changes?"))
                        updateValueInFile();
                }

                viewer.removeDataView(DefaultTextView.this);
            }
        });

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = layout.marginHeight = layout.horizontalSpacing = 0;
        shell.setLayout(layout);

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

        shell.setFont(curFont);

        viewer = theView;

        text = null;
        table = null;
        dataset = null;

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

        StringBuffer sb = new StringBuffer(dataset.getName());
        sb.append("  at  ");
        sb.append(dataset.getPath());
        sb.append("  [");
        sb.append(dataset.getFileFormat().getName());
        sb.append("  in  ");
        sb.append(dataset.getFileFormat().getParent());
        sb.append("]");
        shell.setText(sb.toString());

        shell.setImage(ViewProperties.getTextIcon());

        isReadOnly = dataset.getFileFormat().isReadOnly();

        try {
            text = (String[]) dataset.getData();
        }
        catch (Exception ex) {
            Tools.showError(shell, ex.getMessage(), "TextView" + shell.getText());
            text = null;
        }

        if (text == null) {
            viewer.showStatus("Loading text dataset failed - "
                    + dataset.getName());
            dataset = null;
            return;
        }

        ScrolledComposite tableScroller = new ScrolledComposite(shell, SWT.V_SCROLL);
        tableScroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tableScroller.setExpandHorizontal(true);
        tableScroller.setExpandVertical(true);
        tableScroller.setAlwaysShowScrollBars(false);

        table = createTable(tableScroller);
        tableScroller.setContent(table);

        shell.setMenuBar(createMenuBar());

        shell.pack();

        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null) curFont.dispose();
            }
        });

        int width = 700 + (ViewProperties.getFontSize() - 12) * 15;
        int height = 500 + (ViewProperties.getFontSize() - 12) * 10;
        shell.setSize(width, height);

        viewer.addDataView(this);

        shell.open();
    }

    /**
     * Creates a Table to hold text data.
     */
    private Table createTable(Composite parent) {
        int rank = dataset.getRank();
        long start[] = dataset.getStartDims();
        long count[] = dataset.getSelectedDims();

        long[] startArray = dataset.getStartDims();
        long[] strideArray = dataset.getStride();
        int[] selectedIndex = dataset.getSelectedIndex();
        int startIndex = (int) startArray[selectedIndex[0]];
        int stride = (int) strideArray[selectedIndex[0]];

        String colName = "Data selection:   ["+start[0];
        for (int i=1; i<rank; i++) {
            colName += ", "+start[i];
        }
        colName += "] ~ ["+(start[0]+count[0]-1);
        for (int i=1; i<rank; i++) {
            colName += ", "+(start[i]+count[i]-1);
        }
        colName += "]";

        final Table theTable = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        theTable.setHeaderVisible(true);
        theTable.setLinesVisible(true);
        theTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
        theTable.addListener(SWT.MouseDoubleClick, CellEditor);
        theTable.setFont(curFont);

        TableColumn indexColumn = new TableColumn(theTable, SWT.NONE);
        indexColumn.setAlignment(SWT.CENTER);
        indexColumn.setWidth(70);
        indexColumn.setMoveable(false);
        indexColumn.setResizable(true);

        TableColumn column = new TableColumn(theTable, SWT.NONE);
        column.setText(colName);
        column.setMoveable(false);
        column.setResizable(true);
        column.setWidth(400);

        for (int i = 0; i < startIndex + count[selectedIndex[0]]; i++) {
            TableItem item = new TableItem(theTable, SWT.BORDER);
            item.setFont(curFont);
            item.setText(0, String.valueOf(startIndex + indexBase + i * stride));
            item.setText(1, (text[i] == null) ? "" : text[i]);
            item.setBackground(0, display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        }

        theTable.getColumn(1).pack();

        return theTable;
    }

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
                    Tools.showError(shell, ex.getMessage(), shell.getText());
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

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Print");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                print();
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
        if (!(dataset instanceof ScalarDS) || isReadOnly || !isTextChanged) return;

        for (int i = 0; i < table.getItemCount(); i++) {
            text[i] = table.getItem(i).getText(1);
        }

        try {
            dataset.write();
        }
        catch (Exception ex) {
            Tools.showError(shell, ex.getMessage(), shell.getText());
            return;
        }

        isTextChanged = false;
    }

    /** Save data as text. */
    private void saveAsText() throws Exception {
        FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
        fChooser.setText("Save Current Data To Text File --- " + dataset.getName());
        fChooser.setFilterPath(dataset.getFileFormat().getParent());

        DefaultFileFilter filter = DefaultFileFilter.getFileFilterText();
        fChooser.setFilterExtensions(new String[] {"*.*", filter.getExtensions()});
        fChooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
        fChooser.setFilterIndex(1);

        // fchooser.changeToParentDirectory();
        fChooser.setFileName(dataset.getName() + ".txt");
        fChooser.setOverwrite(true);

        String filename = fChooser.open();

        if (filename == null) return;

        File chosenFile = new File(filename);

        // check if the file is in use
        String fname = chosenFile.getAbsolutePath();
        List<FileFormat> fileList = viewer.getTreeView().getCurrentFiles();
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator<FileFormat> iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                    Tools.showError(shell, "Unable to save data to file \"" + fname
                            + "\". \nThe file is being used.", shell.getText());
                    return;
                }
            }
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(chosenFile)));

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
            RandomAccessFile rf = new RandomAccessFile(chosenFile, "r");
            long size = rf.length();
            rf.close();
            viewer.showStatus("File size (bytes): " + size);
        }
        catch (Exception ex) {
            log.debug("raf file size:", ex);
        }
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
        // StreamPrintServiceFactory[] spsf = StreamPrintServiceFactory
        //         .lookupStreamPrintServiceFactories(null, null);
        // for (int i = 0; i < spsf.length; i++) {
        //     System.out.println(spsf[i]);
        // }
        // DocFlavor[] docFlavors = spsf[0].getSupportedDocFlavors();
        // for (int i = 0; i < docFlavors.length; i++) {
        //     System.out.println(docFlavors[i]);
        // }

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

        // Print it
        try {
            // Print this job on the first print server
            DocPrintJob job = services[0].createPrintJob();
            Doc doc = new SimpleDoc(is, flavor, null);

            job.print(doc, null);
        }
        catch (Exception ex) {
            log.debug("print(): failure: ", ex);
        }
    }

    // Listener to allow in-place editing of Text area cells
    private Listener CellEditor = new Listener() {
        public void handleEvent(Event event) {
            if (isReadOnly) return;

            final TableEditor editor = new TableEditor(table);
            editor.horizontalAlignment = SWT.LEFT;
            editor.grabHorizontal = true;

            Rectangle clientArea = table.getClientArea();
            Point pt = new Point(event.x, event.y);

            int index = table.getTopIndex();

            while (index < table.getItemCount()) {
                boolean visible = false;
                final TableItem item = table.getItem(index);

                for (int i = 0; i < table.getColumnCount(); i++) {
                    Rectangle rect = item.getBounds(i);

                    if (rect.contains(pt)) {
                        final int column = i;

                        if (column == 0) return;

                        final Text text = new Text(table, SWT.WRAP);
                        text.setFont(curFont);

                        Listener textListener = new Listener() {
                            public void handleEvent(final Event e) {
                                switch (e.type) {
                                case SWT.FocusOut:
                                    if (!item.getText(column).equals(text.getText())) isTextChanged=true;
                                    item.setText(column, text.getText());
                                    text.dispose();
                                    break;
                                case SWT.Traverse:
                                    switch (e.detail) {
                                    case SWT.TRAVERSE_RETURN:
                                        if (!item.getText(column).equals(text.getText())) isTextChanged=true;
                                        item.setText(column, text.getText());
                                    case SWT.TRAVERSE_ESCAPE:
                                        text.dispose();
                                        e.doit = false;
                                    }

                                    break;
                                }
                            }
                        };

                        text.addListener(SWT.FocusOut, textListener);
                        text.addListener(SWT.Traverse, textListener);
                        editor.setEditor(text, item, i);
                        text.setText(item.getText(i));
                        text.selectAll();
                        text.setFocus();
                        return;
                    }

                    if (!visible && rect.intersects(clientArea)) {
                        visible = true;
                    }
                }

                if (!visible) return;

                index++;
            }
        }
    };

    /** RowHeader defines the row header component of the Spreadsheet. */
    private class RowHeader extends Table {
        private int currentRowIndex = -1;
        private int lastRowIndex = -1;
        private Table parentTable;

        // Only here for compile reasons
        public RowHeader(Composite parent, int style) {
            super(parent, style);
            // TODO Auto-generated constructor stub
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
}