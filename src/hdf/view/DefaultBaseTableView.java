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

package hdf.view;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.BitSet;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.StructuralRefreshCommand;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.ViewProperties.BITMASK_OP;

/**
 *
 *
 * @author jhenderson
 * @version 1.0 4/13/2018
 */
public abstract class DefaultBaseTableView implements TableView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultBaseTableView.class);

    private final Display                 display = Display.getDefault();
    private final Shell                   shell;
    private Font                          curFont;

    // The main HDFView
    private final ViewManager             viewer;

    private NatTable                      dataTable;

    // The data object to be displayed in the Table
    private final DataFormat              dataObject;

    // The data value of the data object
    private Object                        dataValue;

    private Object                        fillValue;

    private enum ViewType { TABLE, IMAGE, TEXT };
    private      ViewType                 viewType = ViewType.TABLE;

    /**
     * Numerical data type. B = byte array, S = short array, I = int array, J = long array, F =
     * float array, and D = double array.
     */
    private char                          NT = ' ';

    private static final int              FLOAT_BUFFER_SIZE       = 524288;
    private static final int              INT_BUFFER_SIZE         = 524288;
    private static final int              SHORT_BUFFER_SIZE       = 1048576;
    private static final int              LONG_BUFFER_SIZE        = 262144;
    private static final int              DOUBLE_BUFFER_SIZE      = 262144;
    private static final int              BYTE_BUFFER_SIZE        = 2097152;

    // Changed to use normalized scientific notation (1 <= coefficient < 10).
    // private final DecimalFormat scientificFormat = new DecimalFormat("###.#####E0#");
    private final DecimalFormat           scientificFormat = new DecimalFormat("0.0###E0###");
    private DecimalFormat                 customFormat     = new DecimalFormat("###.#####");
    private final NumberFormat            normalFormat     = null;
    private NumberFormat                  numberFormat     = normalFormat;

    // Used for bitmask operations on data
    private BitSet                        bitmask = null;
    private BITMASK_OP                    bitmaskOP = BITMASK_OP.EXTRACT;

    // Fields to keep track of which 'frame' of 3 dimensional data is being displayed
    private Text                          frameField;
    private long                          curDataFrame = 0;
    private long                          maxDataFrame = 1;

    // The index base used for display row and column numbers of data
    private int                           indexBase = 0;

    private int                           fixedDataLength = -1;

    private int                           binaryOrder;

    private boolean                       isReadOnly = false;

    private boolean                       isValueChanged = false;

    private boolean                       isEnumConverted = false;

    private boolean                       isDisplayTypeChar, isDataTransposed;

    private boolean                       isRegRef = false, isObjRef = false;
    private boolean                       showAsHex = false, showAsBin = false;

    // Keep references to the selection and data layers for ease of access
    private SelectionLayer                selectionLayer;
    private DataLayer                     dataLayer;

    private IDataProvider                 rowHeaderDataProvider;
    private IDataProvider                 columnHeaderDataProvider;

    private IDisplayConverter             dataDisplayConverter;

    /**
     * Global variables for GUI components
     */

    private MenuItem                      checkFixedDataLength = null;
    private MenuItem                      checkCustomNotation = null;
    private MenuItem                      checkScientificNotation = null;
    private MenuItem                      checkHex = null;
    private MenuItem                      checkBin = null;

    // Labeled Group to display the index base
    private org.eclipse.swt.widgets.Group indexBaseGroup;

    // Text field to display the value of the currently selected table cell
    private Text                          cellValueField;

    // Label to indicate the current cell location
    private Label                         cellLabel;


    /**
     * Constructs a base TableView with no additional data properties.
     *
     * @param theView
     *            the main HDFView.
     */
    public DefaultBaseTableView(ViewManager theView) {
        this(theView, null);
    }

    /**
     * Constructs a base TableView with the specified data properties.
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
    public DefaultBaseTableView(ViewManager theView, HashMap dataPropertiesMap) {

    }

    @Override
    public DataFormat getDataObject() {
        return dataObject;
    }

    @Override
    public Object getTable() {
        return dataTable;
    }

    // Flip to previous 'frame' of Table data
    private void previousFrame() {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3) return;

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long curFrame = start[selectedIndex[2]];

        if (curFrame == 0) return; // Current frame is the first frame

        gotoFrame(curFrame - 1);
    }

    // Flip to next 'frame' of Table data
    private void nextFrame() {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3) return;

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long[] dims = dataObject.getDims();
        long curFrame = start[selectedIndex[2]];

        if (curFrame == dims[selectedIndex[2]] - 1) return; // Current frame is the last frame

        gotoFrame(curFrame + 1);
    }

    // Flip to the first 'frame' of Table data
    private void firstFrame() {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3) return;

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long curFrame = start[selectedIndex[2]];

        if (curFrame == 0) return; // Current frame is the first frame

        gotoFrame(0);
    }

    // Flip to the last 'frame' of Table data
    private void lastFrame() {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3) return;

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long[] dims = dataObject.getDims();
        long curFrame = start[selectedIndex[2]];

        if (curFrame == dims[selectedIndex[2]] - 1) return; // Current page is the last page

        gotoFrame(dims[selectedIndex[2]] - 1);
    }

    // Flip to the specified 'frame' of Table data
    private void gotoFrame(long idx) {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3 || idx == (curDataFrame - indexBase)) {
            return;
        }

        // Make sure to save any changes to this frame of data before changing frames
        if (isValueChanged) {
            updateValueInFile();
        }

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long[] dims = dataObject.getDims();

        // Do a bit of frame index validation
        if ((idx < 0) || (idx >= dims[selectedIndex[2]])) {
            shell.getDisplay().beep();
            Tools.showError(shell,
                    "Frame number must be between " + indexBase + " and " + (dims[selectedIndex[2]] - 1 + indexBase),
                    shell.getText());
            return;
        }

        start[selectedIndex[2]] = idx;
        curDataFrame = idx + indexBase;
        frameField.setText(String.valueOf(curDataFrame));

        dataObject.clearData();

        shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

        try {
            dataValue = dataObject.getData();
            if (dataObject instanceof ScalarDS) {
                ((ScalarDS) dataObject).convertFromUnsignedC();
                dataValue = dataObject.getData();
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

        dataTable.doCommand(new VisualRefreshCommand());
    }

    /**
     * Copy data from the spreadsheet to the system clipboard.
     */
    private void copyData() {
        StringBuffer sb = new StringBuffer();

        Rectangle selection = selectionLayer.getLastSelectedRegion();
        if (selection == null) {
            Tools.showError(shell, "Select data to copy.", shell.getText());
            return;
        }

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
            Tools.showError(shell,
                    "Copying data to system clipboard failed. \nUse \"export/import data\" for copying/pasting large data.",
                    shell.getText());
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
        if (!MessageDialog.openConfirm(shell, "Clipboard Data", "Do you want to paste selected data?")) return;

        int cols = selectionLayer.getPreferredColumnCount();
        int rows = selectionLayer.getPreferredRowCount();
        int r0 = 0;
        int c0 = 0;

        Rectangle selection = selectionLayer.getLastSelectedRegion();
        if (selection != null) {
            r0 = selection.y;
            c0 = selection.x;
        }

        if (c0 < 0) {
            c0 = 0;
        }
        if (r0 < 0) {
            r0 = 0;
        }
        int r = r0;
        int c = c0;

        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
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
     * Import data values from text file.
     *
     * @param fname
     *            the file to import text from
     */
    private void importTextData(String fname) {
        int cols = selectionLayer.getPreferredColumnCount();
        int rows = selectionLayer.getPreferredRowCount();
        int r0;
        int c0;

        Rectangle lastSelection = selectionLayer.getLastSelectedRegion();
        if (lastSelection != null) {
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
        if (dataObject instanceof CompoundDS) c0 = 0;

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
        if (dataObject instanceof CompoundDS)
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
                        if (dataObject instanceof ScalarDS) {
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
            if (dataObject instanceof CompoundDS) {
                c = 0;
            }
            else {
                c = c0;
            }

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
        String currentDir = ((HObject) dataObject).getFileFormat().getParent();

        String filename = null;
        if (((HDFView) viewer).getTestState()) {
            filename = currentDir + File.separator + new InputDialog(shell, "Enter a file name", "").open();
        }
        else {
            FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
            fChooser.setFilterPath(currentDir);

            DefaultFileFilter filter = DefaultFileFilter.getFileFilterBinary();
            fChooser.setFilterExtensions(new String[] { "*.*", filter.getExtensions() });
            fChooser.setFilterNames(new String[] { "All Files", filter.getDescription() });
            fChooser.setFilterIndex(1);

            filename = fChooser.open();
        }

        if (filename == null) return;

        File chosenFile = new File(filename);
        if (!chosenFile.exists()) {
            Tools.showError(shell, "File " + chosenFile.getName() + " does not exist.", "Import Data from Binary File");
            return;
        }

        if (!MessageDialog.openConfirm(shell, "Import Data", "Do you want to paste selected data?")) return;

        Tools.getBinaryDataFromFile(chosenFile.getAbsolutePath());

        dataTable.doCommand(new StructuralRefreshCommand());
    }
}
