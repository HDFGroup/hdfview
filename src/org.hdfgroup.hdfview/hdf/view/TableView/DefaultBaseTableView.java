/*****************************************************************************
 * Copyright by The HDF Group.                                               *
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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.StructuralRefreshCommand;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.validate.DataValidator;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.action.KeyEditAction;
import org.eclipse.nebula.widgets.nattable.edit.action.MouseEditAction;
import org.eclipse.nebula.widgets.nattable.edit.config.DefaultEditConfiguration;
import org.eclipse.nebula.widgets.nattable.edit.config.DialogErrorHandling;
import org.eclipse.nebula.widgets.nattable.grid.command.ClientAreaResizeCommand;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultColumnHeaderLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultColumnHeaderStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultRowHeaderLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultRowHeaderStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.BeveledBorderDecorator;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.LineBorderDecorator;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectAllCommand;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellEditorMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.LetterOrDigitKeyEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuAction;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.nebula.widgets.nattable.viewport.command.ShowRowInViewportCommand;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import hdf.hdf5lib.HDF5Constants;

import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5ReferenceType;

import hdf.view.Chart;
import hdf.view.DefaultFileFilter;
import hdf.view.HDFView;
import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.ViewProperties.BITMASK_OP;
import hdf.view.DataView.DataViewManager;
import hdf.view.TableView.DataDisplayConverterFactory.HDFDisplayConverter;
import hdf.view.TableView.DataProviderFactory.HDFDataProvider;
import hdf.view.TreeView.TreeView;
import hdf.view.dialog.InputDialog;
import hdf.view.dialog.MathConversionDialog;
import hdf.view.dialog.NewDatasetDialog;

/**
 * DefaultBaseTableView serves as the base class for a DataView that displays
 * HDF data in a tabular format. This class is used for internal bookkeeping and
 * as a place to store higher-level data manipulation functions, whereas its
 * subclasses are responsible for setting up the actual GUI components.
 *
 * @author jhenderson
 * @version 1.0 4/13/2018
 */
public abstract class DefaultBaseTableView implements TableView
{

    private static final Logger   log = LoggerFactory.getLogger(DefaultBaseTableView.class);

    private final Display                   display = Display.getDefault();
    /** The reference to the display shell used */
    protected final Shell                   shell;
    /** The current font */
    protected Font                          curFont;

    /** The main HDFView */
    protected final DataViewManager         viewer;

    /** The reference to the NAT table used */
    protected NatTable                      dataTable;

    /** The data object to be displayed in the Table */
    protected final DataFormat              dataObject;

    /** The data value of the data object */
    protected Object                        dataValue;

    /** The value used for fill */
    protected Object                        fillValue;

    /** the valid types of tableviews */
    protected enum ViewType {
        /** The data view is of type spreadsheet */
        TABLE,
        /** The data view is of type image */
        IMAGE
    };

    /** The type of view */
    protected      ViewType                 viewType = ViewType.TABLE;

    /** Changed to use normalized scientific notation (1 is less than coefficient is less than 10). */
    protected final DecimalFormat           scientificFormat = new DecimalFormat("0.0###E0###");
    /** custom format pattern */
    protected DecimalFormat                 customFormat     = new DecimalFormat("###.#####");
    /** the normal format to be used for numbers */
    protected final NumberFormat            normalFormat     = null;
    /** the format to be used for numbers */
    protected NumberFormat                  numberFormat     = normalFormat;

    /** Used for bitmask operations on data */
    protected BitSet                        bitmask = null;
    /** Used for the type of bitmask operation */
    protected BITMASK_OP                    bitmaskOP = BITMASK_OP.EXTRACT;

    /** Fields to keep track of which 'frame' of 3 dimensional data is being displayed */
    private Text                            frameField;
    private long                            curDataFrame = 0;
    private long                            maxDataFrame = 1;

    /** The index base used for display row and column numbers of data */
    protected int                           indexBase = 0;

    /** size of default data length */
    protected int                           fixedDataLength = -1;

    /** default binary order */
    protected int                           binaryOrder;

    /** status if file is read only */
    protected boolean                       isReadOnly = false;

    /** status if the enums are to display converted */
    protected boolean                       isEnumConverted = false;

    /** status if the display type is a char */
    protected boolean                       isDisplayTypeChar;

    /** status if the data is transposed */
    protected boolean                       isDataTransposed;

    /** reference status */
    protected boolean                       isRegRef = false, isObjRef = false, isStdRef = false;
    /** show data as status */
    protected boolean                       showAsHex = false, showAsBin = false;

    /** Keep references to the selection layers for ease of access */
    protected SelectionLayer                selectionLayer;
    /** Keep references to the data layers for ease of access */
    protected DataLayer                     dataLayer;

    /** reference to the data provider for the row */
    protected IDataProvider                 rowHeaderDataProvider;
    /** reference to the data provider for the column */
    protected IDataProvider                 columnHeaderDataProvider;

    /** reference to the data provider */
    protected HDFDataProvider               dataProvider;
    /** reference to the display converter */
    protected HDFDisplayConverter           dataDisplayConverter;

    /**
     * Global variables for GUI components on the default to show data
     */
    /** Checkbox menu item for Fixed Data Length default*/
    protected MenuItem                      checkFixedDataLength = null;
    /** Checkbox menu item for Custom Notation default*/
    protected MenuItem                      checkCustomNotation = null;
    /** Checkbox menu item for Scientific Notation default */
    protected MenuItem                      checkScientificNotation = null;
    /** Checkbox menu item for hex default */
    protected MenuItem                      checkHex = null;
    /** Checkbox menu item for binary default */
    protected MenuItem                      checkBin = null;
    /** Checkbox menu item for enum default*/
    protected MenuItem                      checkEnum = null;

    /** Labeled Group to display the index base */
    protected org.eclipse.swt.widgets.Group indexBaseGroup;

    /** Text field to display the value of the currently selected table cell */
    protected Text                          cellValueField;

    /** Label to indicate the current cell location */
    protected Label                         cellLabel;


    /**
     * Constructs a base TableView with no additional data properties.
     *
     * @param theView
     *            the main HDFView.
     */
    public DefaultBaseTableView(DataViewManager theView) {
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
    @SuppressWarnings("rawtypes")
    public DefaultBaseTableView(DataViewManager theView, HashMap dataPropertiesMap) {
        shell = new Shell(display, SWT.SHELL_TRIM);

        shell.setData(this);

        shell.setLayout(new GridLayout(1, true));

        /*
         * When the table is closed, make sure to prompt the user about saving their
         * changes, then do any pending cleanup work.
         */
        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (dataProvider != null) {
                    if (dataProvider.getIsValueChanged() && !isReadOnly) {
                        if (Tools.showConfirm(shell, "Changes Detected", "\"" + ((HObject) dataObject).getName()
                                + "\" has changed.\nDo you want to save the changes?"))
                            updateValueInFile();
                        else
                            dataObject.clearData();
                    }
                }

                dataValue = null;
                dataTable = null;

                if (curFont != null)
                    curFont.dispose();

                viewer.removeDataView(DefaultBaseTableView.this);
            }
        });

        /* Grab the current font to be used for all GUI components */
        try {
            curFont = new Font(display, ViewProperties.getFontType(), ViewProperties.getFontSize(), SWT.NORMAL);
        }
        catch (Exception ex) {
            curFont = null;
        }

        viewer = theView;

        /* Retrieve any display properties passed in via the HashMap parameter */
        HObject hObject = null;

        if (ViewProperties.isIndexBase1())
            indexBase = 1;

        if (dataPropertiesMap != null) {
            hObject = (HObject) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.OBJECT);

            bitmask = (BitSet) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.BITMASK);
            bitmaskOP = (BITMASK_OP) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.BITMASKOP);

            Boolean b = (Boolean) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.CHAR);
            if (b != null) isDisplayTypeChar = b.booleanValue();

            b = (Boolean) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.TRANSPOSED);
            if (b != null) isDataTransposed = b.booleanValue();

            b = (Boolean) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.INDEXBASE1);
            if (b != null) {
                if (b.booleanValue())
                    indexBase = 1;
                else
                    indexBase = 0;
            }
        }

        if (hObject == null)
            hObject = viewer.getTreeView().getCurrentObject();

        /* Only edit objects which actually contain editable data */
        if ((hObject == null) || !(hObject instanceof DataFormat)) {
            log.debug("data object is null or not an instanceof DataFormat");
            dataObject = null;
            shell.dispose();
            return;
        }

        dataObject = (DataFormat) hObject;
        if (((HObject) dataObject).getFileFormat() == null) {
            log.debug("DataFormat object cannot access FileFormat");
            shell.dispose();
            return;
        }

        isReadOnly = ((HObject) dataObject).getFileFormat().isReadOnly();

        if (((HObject) dataObject).getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))
                && (dataObject instanceof CompoundDS)) {
            /* Cannot edit HDF4 VData */
            isReadOnly = true;
        }

        /* Disable edit feature for SZIP compression when encode is not enabled */
        if (!isReadOnly) {
            String compression = dataObject.getCompression();
            if ((compression != null) && compression.startsWith("SZIP")) {
                if (!compression.endsWith("ENCODE_ENABLED"))
                    isReadOnly = true;
            }
        }

        log.trace("dataObject({}) isReadOnly={}", dataObject, isReadOnly);

        long[] dims = dataObject.getDims();
        long tsize = 1;

        if (dims == null) {
            log.debug("data object has null dimensions");
            viewer.showError("Error: Data object '" + ((HObject) dataObject).getName() + "' has null dimensions.");
            shell.dispose();
            Tools.showError(display.getActiveShell(), "Error", "Could not open data object '" + ((HObject) dataObject).getName()
                    + "'. Data object has null dimensions.");
            return;
        }

        for (int i = 0; i < dims.length; i++)
            tsize *= dims[i];

        log.trace("Data object Size={} Height={} Width={}", tsize, dataObject.getHeight(), dataObject.getWidth());

        if (dataObject.getHeight() <= 0 || dataObject.getWidth() <= 0 || tsize <= 0) {
            log.debug("data object has dimension of size 0");
            viewer.showError("Error: Data object '" + ((HObject) dataObject).getName() + "' has dimension of size 0.");
            shell.dispose();
            Tools.showError(display.getActiveShell(), "Error", "Could not open data object '" + ((HObject) dataObject).getName()
                    + "'. Data object has dimension of size 0.");
            return;
        }

        /*
         * Determine whether the data is to be displayed as characters and whether or
         * not enum data is to be converted.
         */
        Datatype dtype = dataObject.getDatatype();

        log.trace("Data object getDatatypeClass()={}", dtype.getDatatypeClass());
        isDisplayTypeChar = (isDisplayTypeChar
                && (dtype.getDatatypeSize() == 1 || (dtype.isArray() && dtype.getDatatypeBase().isChar())));

        isEnumConverted = ViewProperties.isConvertEnum();

        log.trace("Data object isDisplayTypeChar={} isEnumConverted={}", isDisplayTypeChar, isEnumConverted);

        if (dtype.isRef()) {
            if (((HObject) dataObject).getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                isStdRef = ((H5Datatype)dtype).isStdRef();
                isRegRef = ((H5Datatype)dtype).isRegRef();
                isObjRef = ((H5Datatype)dtype).isRefObj();
            }
        }


        // Setup subset information
        int space_type = dataObject.getSpaceType();
        int rank = dataObject.getRank();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long[] count = dataObject.getSelectedDims();
        long[] stride = dataObject.getStride();
        long[] start = dataObject.getStartDims();
        int n = Math.min(3, rank);

        if (rank > 2) {
            curDataFrame = start[selectedIndex[2]] + indexBase;
            maxDataFrame = (indexBase == 1) ? dims[selectedIndex[2]] : dims[selectedIndex[2]] - 1;
        }

        /* Create the toolbar area that contains useful shortcuts */
        ToolBar toolBar = createToolbar(shell);
        toolBar.setSize(shell.getSize().x, 30);
        toolBar.setLocation(0, 0);

        /*
         * Create the group that contains the text fields for displaying the value and
         * location of the current cell, as well as the index base.
         */
        indexBaseGroup = new org.eclipse.swt.widgets.Group(shell, SWT.SHADOW_ETCHED_OUT);
        indexBaseGroup.setFont(curFont);
        indexBaseGroup.setText(indexBase + "-based");
        indexBaseGroup.setLayout(new GridLayout(1, true));
        indexBaseGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        SashForm content = new SashForm(indexBaseGroup, SWT.VERTICAL);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        content.setSashWidth(10);

        SashForm cellValueComposite = new SashForm(content, SWT.HORIZONTAL);
        cellValueComposite.setSashWidth(8);

        cellLabel = new Label(cellValueComposite, SWT.RIGHT | SWT.BORDER);
        cellLabel.setAlignment(SWT.CENTER);
        cellLabel.setFont(curFont);

        final ScrolledComposite cellValueFieldScroller = new ScrolledComposite(cellValueComposite, SWT.V_SCROLL | SWT.H_SCROLL);
        cellValueFieldScroller.setLayout(new FillLayout());

        cellValueField = new Text(cellValueFieldScroller, SWT.MULTI | SWT.BORDER | SWT.WRAP);
        cellValueField.setEditable(false);
        cellValueField.setBackground(new Color(display, 255, 255, 240));
        cellValueField.setEnabled(false);
        cellValueField.setFont(curFont);

        cellValueFieldScroller.setContent(cellValueField);
        cellValueFieldScroller.setExpandHorizontal(true);
        cellValueFieldScroller.setExpandVertical(true);
        cellValueFieldScroller.setMinSize(cellValueField.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        cellValueComposite.setWeights(new int[] { 1, 5 });

        /* Make sure that the Dataset's data value is accessible for conditionally adding GUI components */
        try {
            loadData(dataObject);
            if (isStdRef) {
                if (dataObject.getRank() > 2)
                    ((H5ReferenceType)dtype).setRefSize((int)dataObject.getWidth() * (int)dataObject.getWidth());
                ((H5ReferenceType)dtype).setData(dataValue);
            }
        }
        catch (Exception ex) {
            log.debug("loadData(): data not loaded: ", ex);
            viewer.showError("Error: unable to load table data");
            shell.dispose();
            Tools.showError(display.getActiveShell(), "Open", "An error occurred while loading data for the table:\n\n" + ex.getMessage());
            return;
        }

        /* Create the Shell's MenuBar */
        shell.setMenuBar(createMenuBar(shell));

        /*
         * Set the default selection on the "Show Hexadecimal/Show Binary", etc. MenuItems.
         * This step must be done after the menu bar has actually been created.
         */
        if (dataObject.getDatatype().isBitField() || dataObject.getDatatype().isOpaque()) {
            showAsHex = true;
            checkHex.setSelection(true);
            checkScientificNotation.setSelection(false);
            checkCustomNotation.setSelection(false);
            checkBin.setSelection(false);
            showAsBin = false;
            numberFormat = normalFormat;
        }

        /*
         * Set the default selection on the "Show Enum", etc. MenuItems.
         * This step must be done after the menu bar has actually been created.
         */
        if (dataObject.getDatatype().isEnum()) {
            checkEnum.setSelection(isEnumConverted);
            checkScientificNotation.setSelection(false);
            checkCustomNotation.setSelection(false);
            checkBin.setSelection(false);
            checkHex.setSelection(false);
            showAsBin = false;
            showAsHex = false;
            numberFormat = normalFormat;
        }

        /* Create the actual NatTable */
        log.debug("table creation {}", ((HObject) dataObject).getName());
        try {
            dataTable = createTable(content, dataObject);
            if (dataTable == null) {
                log.debug("table creation for object {} failed", ((HObject) dataObject).getName());
                viewer.showError("Creating table for object '" + ((HObject) dataObject).getName() + "' failed.");
                shell.dispose();
                Tools.showError(display.getActiveShell(), "Open", "Failed to create Table object");
                return;
            }
        }
        catch (UnsupportedOperationException ex) {
            log.debug("Subclass does not implement createTable()");
            shell.dispose();
            return;
        }

        /*
         * Set the default data display conversion settings.
         */
        updateDataConversionSettings();

        dataTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        /*
         * Set the Shell's title using the object path and name
         */
        StringBuilder sb = new StringBuilder(hObject.getName());

        if (((HObject) dataObject).getFileFormat() != null) {
            sb.append("  at  ")
            .append(hObject.getPath())
            .append("  [")
            .append(((HObject) dataObject).getFileFormat().getName())
            .append("  in  ")
            .append(((HObject) dataObject).getFileFormat().getParent())
            .append("]");
        }

        shell.setText(sb.toString());

        /*
         * Append subsetting information and show this as a status message in the
         * HDFView main window
         */
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

        if (log.isTraceEnabled())
            log.trace("subset={}", sb);

        viewer.showStatus(sb.toString());

        indexBaseGroup.pack();

        content.setWeights(new int[] { 1, 12 });

        shell.pack();

        int width = 700 + (ViewProperties.getFontSize() - 12) * 15;
        int height = 500 + (ViewProperties.getFontSize() - 12) * 10;
        shell.setSize(width, height);
    }

    /**
     * Creates the toolbar for the Shell.
     */
    private ToolBar createToolbar(final Shell shell) {
        ToolBar toolbar = new ToolBar(shell, SWT.HORIZONTAL | SWT.RIGHT | SWT.BORDER);
        toolbar.setFont(curFont);
        toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // Chart button
        ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        item.setImage(ViewProperties.getChartIcon());
        item.setToolTipText("Line Plot");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showLineplot();
            }
        });

        if (dataObject.getRank() > 2) {
            new ToolItem(toolbar, SWT.SEPARATOR).setWidth(20);

            // First frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getFirstIcon());
            item.setToolTipText("First Frame");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    firstFrame();
                }
            });

            // Previous frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getPreviousIcon());
            item.setToolTipText("Previous Frame");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    previousFrame();
                }
            });

            ToolItem separator = new ToolItem(toolbar, SWT.SEPARATOR);

            frameField = new Text(toolbar, SWT.SINGLE | SWT.BORDER | SWT.CENTER);
            frameField.setFont(curFont);
            frameField.setText(String.valueOf(curDataFrame));
            frameField.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(TraverseEvent e) {
                    if (e.detail == SWT.TRAVERSE_RETURN) {
                        try {
                            int frame = 0;

                            try {
                                frame = Integer.parseInt(frameField.getText().trim()) - indexBase;
                            }
                            catch (Exception ex) {
                                frame = -1;
                            }

                            gotoFrame(frame);
                        }
                        catch (Exception ex) {
                            log.debug("Frame change failure: ", ex);
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
            maxFrameText.setText(String.valueOf(maxDataFrame));
            maxFrameText.setEditable(false);
            maxFrameText.setEnabled(false);

            maxFrameText.pack();

            separator.setWidth(maxFrameText.getSize().x + 30);
            separator.setControl(maxFrameText);

            new ToolItem(toolbar, SWT.SEPARATOR).setWidth(10);

            // Next frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getNextIcon());
            item.setToolTipText("Next Frame");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    nextFrame();
                }
            });

            // Last frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getLastIcon());
            item.setToolTipText("Last Frame");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    lastFrame();
                }
            });
        }

        return toolbar;
    }

    /**
     * Creates the menubar for the Shell.
     *
     * @param theShell
     *    the reference to the display shell
     *
     * @return the newly created menu
     */
    protected Menu createMenuBar(final Shell theShell) {
        Menu menuBar = new Menu(theShell, SWT.BAR);
        boolean isEditable = !isReadOnly;

        MenuItem tableMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        tableMenuItem.setText("&Table");

        Menu tableMenu = new Menu(theShell, SWT.DROP_DOWN);
        tableMenuItem.setMenu(tableMenu);

        MenuItem item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Select All");
        item.setAccelerator(SWT.CTRL | 'A');
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    dataTable.doCommand(new SelectAllCommand());
                }
                catch (Exception ex) {
                    theShell.getDisplay().beep();
                    Tools.showError(theShell, "Select", ex.getMessage());
                }
            }
        });

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Copy");
        item.setAccelerator(SWT.CTRL | 'C');
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copyData();
            }
        });

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Paste");
        item.setAccelerator(SWT.CTRL | 'V');
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                pasteData();
            }
        });

        new MenuItem(tableMenu, SWT.SEPARATOR);

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Copy to New Dataset");
        item.setEnabled(isEditable && (dataObject instanceof ScalarDS));
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if ((selectionLayer.getSelectedColumnPositions().length <= 0)
                        || (selectionLayer.getSelectedRowCount() <= 0)) {
                    Tools.showInformation(theShell, "Copy", "Select table cells to write.");
                    return;
                }

                TreeView treeView = viewer.getTreeView();
                Group pGroup = (Group) (treeView.findTreeItem((HObject) dataObject).getParentItem().getData());
                HObject root = ((HObject) dataObject).getFileFormat().getRootObject();

                if (root == null) return;

                ArrayList<HObject> list = new ArrayList<>(((HObject) dataObject).getFileFormat().getNumberOfMembers() + 5);
                Iterator<HObject> it = ((Group) root).depthFirstMemberList().iterator();

                while (it.hasNext())
                    list.add(it.next());
                list.add(root);

                NewDatasetDialog dialog = new NewDatasetDialog(theShell, pGroup, list, DefaultBaseTableView.this);
                dialog.open();

                HObject obj = dialog.getObject();
                if (obj != null) {
                    Group pgroup = dialog.getParentGroup();
                    try {
                        treeView.addObject(obj, pgroup);
                    }
                    catch (Exception ex) {
                        log.debug("Write selection to dataset:", ex);
                    }
                }

                list.clear();
            }
        });

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Save Changes to File");
        item.setAccelerator(SWT.CTRL | 'U');
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    updateValueInFile();
                }
                catch (Exception ex) {
                    theShell.getDisplay().beep();
                    Tools.showError(theShell, "Save", ex.getMessage());
                }
            }
        });

        new MenuItem(tableMenu, SWT.SEPARATOR);

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Show Lineplot");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showLineplot();
            }
        });

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Show Statistics");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    Object theData = getSelectedData();

                    if (dataObject instanceof CompoundDS) {
                        int cols = selectionLayer.getFullySelectedColumnPositions().length;
                        if (cols != 1) {
                            Tools.showError(theShell, "Statistics", "Please select one column at a time for compound dataset.");
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
                        String stats = "Min                      = " + minmax[0] + "\nMax                      = "
                                + minmax[1] + "\nMean                     = " + stat[0] + "\nStandard deviation = "
                                + stat[1];
                        Tools.showInformation(theShell, "Statistics", stats);
                    }

                    System.gc();
                }
                catch (Exception ex) {
                    theShell.getDisplay().beep();
                    Tools.showError(shell, "Statistics", ex.getMessage());
                }
            }
        });

        new MenuItem(tableMenu, SWT.SEPARATOR);

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Math Conversion");
        item.setEnabled(isEditable);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    mathConversion();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(theShell, "Convert", ex.getMessage());
                }
            }
        });

        new MenuItem(tableMenu, SWT.SEPARATOR);

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Close");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                theShell.dispose();
            }
        });

        /********************************************************************
         *                                                                  *
         * Set up MenuItems for refreshing the TableView                    *
         *                                                                  *
         ********************************************************************/
        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Start Timer");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                viewer.executeTimer(true);
            }
        });

        item = new MenuItem(tableMenu, SWT.PUSH);
        item.setText("Stop Timer");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                viewer.executeTimer(false);
            }
        });


        /********************************************************************
         *                                                                  *
         * Set up MenuItems for Importing/Exporting Data from the TableView *
         *                                                                  *
         ********************************************************************/
        MenuItem importExportMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        importExportMenuItem.setText("&Import/Export Data");

        Menu importExportMenu = new Menu(theShell, SWT.DROP_DOWN);
        importExportMenuItem.setMenu(importExportMenu);

        item = new MenuItem(importExportMenu, SWT.CASCADE);
        item.setText("Export Data to");

        Menu exportMenu = new Menu(item);
        item.setMenu(exportMenu);

        item = new MenuItem(exportMenu, SWT.PUSH);
        item.setText("Text File");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    saveAsText();
                }
                catch (Exception ex) {
                    theShell.getDisplay().beep();
                    Tools.showError(theShell, "Save", ex.getMessage());
                }
            }
        });

        item = new MenuItem(importExportMenu, SWT.CASCADE);
        item.setText("Import Data from");

        Menu importMenu = new Menu(item);
        item.setMenu(importMenu);

        item = new MenuItem(importMenu, SWT.PUSH);
        item.setText("Text File");
        item.setEnabled(!isReadOnly);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String currentDir = ((HObject) dataObject).getFileFormat().getParent();

                String filename = null;
                if (((HDFView) viewer).getTestState()) {
                    filename = currentDir + File.separator + new InputDialog(theShell, "Enter a file name", "").open();
                }
                else {
                    FileDialog fChooser = new FileDialog(theShell, SWT.OPEN);
                    fChooser.setFilterPath(currentDir);

                    DefaultFileFilter filter = DefaultFileFilter.getFileFilterText();
                    fChooser.setFilterExtensions(new String[] { "*", filter.getExtensions() });
                    fChooser.setFilterNames(new String[] { "All Files", filter.getDescription() });
                    fChooser.setFilterIndex(1);

                    filename = fChooser.open();
                }

                if (filename == null)
                    return;

                File chosenFile = new File(filename);
                if (!chosenFile.exists()) {
                    Tools.showError(theShell, "Import Data From Text File", "Data import error: " + filename + " does not exist.");
                    return;
                }

                if (!Tools.showConfirm(theShell, "Import Data From Text File", "Do you want to paste selected data?"))
                    return;

                importTextData(chosenFile.getAbsolutePath());
            }
        });

        return menuBar;
    }

    /**
     * Loads the data buffer of an object.
     *
     * @param dataObject
     *        the object that has the buffer for the data.
     *
     * @throws Exception if a failure occurred
     */
    protected void loadData(DataFormat dataObject) throws Exception {
        if (!dataObject.isInited()) {
            try {
                dataObject.init();
            }
            catch (Exception ex) {
                dataValue = null;
                log.debug("loadData(): ", ex);
                throw ex;
            }
        }

        // use lazy convert for large number of strings
        if (dataObject.getHeight() > 10000 && dataObject instanceof CompoundDS) {
            ((CompoundDS) dataObject).setConvertByteToString(false);
        }

        // Make sure entire dataset is not loaded when looking at 3D
        // datasets using the default display mode (double clicking the
        // data object)
        if (dataObject.getRank() > 2)
            dataObject.getSelectedDims()[dataObject.getSelectedIndex()[2]] = 1;

        dataValue = null;
        try {
            log.trace("loadData(): call getData()");
            dataValue = dataObject.getData();
        }
        catch (Exception ex) {
            dataValue = null;
            log.debug("loadData(): ", ex);
            throw ex;
        }
    }

    /**
     * Create a data table for a data object.
     *
     * @param parent
     *            the parent object this table will be associated with.
     * @param dataObject
     *            the data object this table will be associated with.
     *
     * @return the newly created data table
     */
    protected abstract NatTable createTable(Composite parent, DataFormat dataObject);

    /**
     * Show the object reference data.
     *
     * @param ref
     *            the identifer for the object reference.
     */
    protected abstract void showObjRefData(byte[] ref);

    /**
     * Show the region reference data.
     *
     * @param reg
     *            the identifier for the region reference.
     */
    protected abstract void showRegRefData(byte[] reg);

    /**
     * Show the standard reference data.
     *
     * @param reg
     *            the identifier for the standard reference.
     */
    protected abstract void showStdRefData(byte[] reg);

    /**
     * Get the data editing rule for the object.
     *
     * @param dataObject
     *        the data object
     *
     * @return the rule
     */
    protected abstract IEditableRule getDataEditingRule(DataFormat dataObject);

    /**
     * Update the display converters.
     */
    protected void updateDataConversionSettings() {
        if (dataDisplayConverter != null) {
            dataDisplayConverter.setShowAsHex(showAsHex);
            dataDisplayConverter.setShowAsBin(showAsBin);
            dataDisplayConverter.setNumberFormat(numberFormat);
            dataDisplayConverter.setConvertEnum(isEnumConverted);
        }
    }

    /**
     * Update dataset's value in file. The changes will go to the file.
     */
    @Override
    public void updateValueInFile() {

        if (isReadOnly || !dataProvider.getIsValueChanged() || showAsBin || showAsHex) {
            log.debug("updateValueInFile(): file not updated; read-only or unchanged data or displayed as hex or binary");
            return;
        }

        try {
            dataObject.write();
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Update", ex.getMessage());
            log.debug("updateValueInFile(): ", ex);
            return;
        }

        dataProvider.setIsValueChanged(false);
    }

    @Override
    public HObject getDataObject() {
        return (HObject) dataObject;
    }

    @Override
    public Object getTable() {
        return dataTable;
    }

    @Override
    public int getSelectedRowCount() {
        return selectionLayer.getSelectedRowCount();
    }

    @Override
    public int getSelectedColumnCount() {
        return selectionLayer.getSelectedColumnPositions().length;
    }

    /** @return the selection layer */
    public SelectionLayer getSelectionLayer() {
        return selectionLayer;
    }

    /** @return the data layer */
    public DataLayer getDataLayer() {
        return dataLayer;
    }

    /** refresh the data table */
    @Override
    public void refreshDataTable() {
        log.trace("refreshDataTable()");

        shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
        dataValue = dataObject.refreshData();
        shell.setCursor(null);

        long[] dims = dataObject.getDims();
        log.trace("refreshDataTable() dims:{}", dims);
        dataProvider.updateDataBuffer(dataValue);
        ((RowHeaderDataProvider)rowHeaderDataProvider).updateRows(dataObject);
        log.trace("refreshDataTable(): rows={} : cols={}", dataProvider.getRowCount(), dataProvider.getColumnCount());

        dataTable.doCommand(new StructuralRefreshCommand());
        final ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);
        dataTable.doCommand(new ShowRowInViewportCommand(dataProvider.getRowCount()-1));
        log.trace("refreshDataTable() finish");
    }

    // Flip to previous 'frame' of Table data
    private void previousFrame() {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3)
            return;

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long curFrame = start[selectedIndex[2]];

        if (curFrame == 0)
            return; // Current frame is the first frame

        gotoFrame(curFrame - 1);
    }

    // Flip to next 'frame' of Table data
    private void nextFrame() {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3)
            return;

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long[] dims = dataObject.getDims();
        long curFrame = start[selectedIndex[2]];

        if (curFrame == dims[selectedIndex[2]] - 1)
            return; // Current frame is the last frame

        gotoFrame(curFrame + 1);
    }

    // Flip to the first 'frame' of Table data
    private void firstFrame() {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3)
            return;

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long curFrame = start[selectedIndex[2]];

        if (curFrame == 0)
            return; // Current frame is the first frame

        gotoFrame(0);
    }

    // Flip to the last 'frame' of Table data
    private void lastFrame() {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3)
            return;

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long[] dims = dataObject.getDims();
        long curFrame = start[selectedIndex[2]];

        if (curFrame == dims[selectedIndex[2]] - 1)
            return; // Current page is the last page

        gotoFrame(dims[selectedIndex[2]] - 1);
    }

    // Flip to the specified 'frame' of Table data
    private void gotoFrame(long idx) {
        // Only valid operation if data object has 3 or more dimensions
        if (dataObject.getRank() < 3 || idx == (curDataFrame - indexBase))
            return;

        // Make sure to save any changes to this frame of data before changing frames
        if (dataProvider.getIsValueChanged())
            updateValueInFile();

        long[] start = dataObject.getStartDims();
        int[] selectedIndex = dataObject.getSelectedIndex();
        long[] dims = dataObject.getDims();

        // Do a bit of frame index validation
        if ((idx < 0) || (idx >= dims[selectedIndex[2]])) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select",
                    "Frame number must be between " + indexBase + " and " + (dims[selectedIndex[2]] - 1 + indexBase));
            return;
        }

        start[selectedIndex[2]] = idx;
        curDataFrame = idx + indexBase;
        frameField.setText(String.valueOf(curDataFrame));

        dataObject.clearData();

        shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

        try {
            dataValue = dataObject.getData();

            /*
             * TODO: Converting data from unsigned C integers to Java integers
             *       is currently unsupported for Compound Datasets.
             */
            if (!(dataObject instanceof CompoundDS))
                dataObject.convertFromUnsignedC();

            dataValue = dataObject.getData();
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Error loading data", "Dataset getData: " + ex.getMessage());
            log.debug("gotoFrame(): ", ex);
            dataValue = null;
        }
        finally {
            shell.setCursor(null);
        }

        dataProvider.updateDataBuffer(dataValue);

        dataTable.doCommand(new VisualRefreshCommand());
    }

    /**
     * Copy data from the spreadsheet to the system clipboard.
     */
    private void copyData() {
        StringBuilder sb = new StringBuilder();

        Rectangle selection = selectionLayer.getLastSelectedRegion();
        if (selection == null) {
            Tools.showError(shell, "Copy", "Select data to copy.");
            return;
        }

        int r0 = selectionLayer.getLastSelectedRegion().y; // starting row
        int c0 = selectionLayer.getLastSelectedRegion().x; // starting column

        if ((r0 < 0) || (c0 < 0))
            return;

        int nr = selectionLayer.getSelectedRowCount();
        int nc = selectionLayer.getSelectedColumnPositions().length;
        int r1 = r0 + nr; // finish row
        int c1 = c0 + nc; // finishing column

        try {
            for (int i = r0; i < r1; i++) {
                sb.append(selectionLayer.getDataValueByPosition(c0, i).toString());
                for (int j = c0 + 1; j < c1; j++)
                    sb.append("\t").append(selectionLayer.getDataValueByPosition(j, i).toString());
                sb.append("\n");
            }
        }
        catch (java.lang.OutOfMemoryError err) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Copy",
                    "Copying data to system clipboard failed. \nUse \"export/import data\" for copying/pasting large data.");
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
        if (!Tools.showConfirm(shell, "Clipboard Data", "Do you want to paste selected data?"))
            return;

        int cols = selectionLayer.getPreferredColumnCount();
        int rows = selectionLayer.getPreferredRowCount();
        int r0 = 0;
        int c0 = 0;

        Rectangle selection = selectionLayer.getLastSelectedRegion();
        if (selection != null) {
            r0 = selection.y;
            c0 = selection.x;
        }

        if (c0 < 0)
            c0 = 0;
        if (r0 < 0)
            r0 = 0;
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
                            dataProvider.setDataValue(c, r, lt.nextToken());
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
                            dataProvider.setDataValue(c, r, theVal);
                        }
                        catch (Exception ex) {
                            continue;
                        }
                        c++;
                    }
                }
            }
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Paste", ex.getMessage());
        }
    }

    /**
     * Save data as text.
     *
     * @throws Exception
     *             if a failure occurred
     */
    protected void saveAsText() throws Exception {
        String currentDir = ((HObject) dataObject).getFileFormat().getParent();

        String filename = null;
        if (((HDFView) viewer).getTestState()) {
            filename = currentDir + File.separator + new InputDialog(shell, "Enter a file name", "").open();
        }
        else {
            FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
            fChooser.setFilterPath(currentDir);

            DefaultFileFilter filter = DefaultFileFilter.getFileFilterText();
            fChooser.setFilterExtensions(new String[] { "*", filter.getExtensions() });
            fChooser.setFilterNames(new String[] { "All Files", filter.getDescription() });
            fChooser.setFilterIndex(1);
            fChooser.setText("Save Current Data To Text File --- " + ((HObject) dataObject).getName());

            filename = fChooser.open();
        }
        if (filename == null)
            return;

        File chosenFile = new File(filename);
        String fname = chosenFile.getAbsolutePath();

        log.trace("saveAsText: file={}", fname);

        // Check if the file is in use and prompt for overwrite
        if (chosenFile.exists()) {
            List<?> fileList = viewer.getTreeView().getCurrentFiles();
            if (fileList != null) {
                FileFormat theFile = null;
                Iterator<?> iterator = fileList.iterator();
                while (iterator.hasNext()) {
                    theFile = (FileFormat) iterator.next();
                    if (theFile.getFilePath().equals(fname)) {
                        shell.getDisplay().beep();
                        Tools.showError(shell, "Save",
                                "Unable to save data to file \"" + fname + "\". \nThe file is being used.");
                        return;
                    }
                }
            }

            if (!Tools.showConfirm(shell, "Save", "File exists. Do you want to replace it?"))
                return;
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(chosenFile)));

        String delName = ViewProperties.getDataDelimiter();
        String delimiter = "";

        // delimiter must include a tab to be consistent with copy/paste for
        // compound fields
        if (dataObject instanceof CompoundDS)
            delimiter = "\t";

        if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_TAB))
            delimiter = "\t";
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SPACE))
            delimiter = " " + delimiter;
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COMMA))
            delimiter = "," + delimiter;
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COLON))
            delimiter = ":" + delimiter;
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SEMI_COLON))
            delimiter = ";" + delimiter;

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

    /**
     * Save data as binary.
     *
     * @throws Exception
     *             if a failure occurred
     */
    protected void saveAsBinary() throws Exception {
        String currentDir = ((HObject) dataObject).getFileFormat().getParent();

        String filename = null;
        if (((HDFView) viewer).getTestState()) {
            filename = currentDir + File.separator + new InputDialog(shell, "Enter a file name", "").open();
        }
        else {
            FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
            fChooser.setFilterPath(currentDir);

            DefaultFileFilter filter = DefaultFileFilter.getFileFilterBinary();
            fChooser.setFilterExtensions(new String[] { "*", filter.getExtensions() });
            fChooser.setFilterNames(new String[] { "All Files", filter.getDescription() });
            fChooser.setFilterIndex(1);
            fChooser.setText("Save Current Data To Binary File --- " + ((HObject) dataObject).getName());

            filename = fChooser.open();
        }
        if (filename == null)
            return;

        File chosenFile = new File(filename);
        String fname = chosenFile.getAbsolutePath();

        log.trace("saveAsBinary: file={}", fname);

        // Check if the file is in use and prompt for overwrite
        if (chosenFile.exists()) {
            List<?> fileList = viewer.getTreeView().getCurrentFiles();
            if (fileList != null) {
                FileFormat theFile = null;
                Iterator<?> iterator = fileList.iterator();
                while (iterator.hasNext()) {
                    theFile = (FileFormat) iterator.next();
                    if (theFile.getFilePath().equals(fname)) {
                        shell.getDisplay().beep();
                        Tools.showError(shell, "Save",
                                "Unable to save data to file \"" + fname + "\". \nThe file is being used.");
                        return;
                    }
                }
            }

            if (!Tools.showConfirm(shell, "Save", "File exists. Do you want to replace it?"))
                return;
        }

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(chosenFile))) {
            if (dataObject instanceof ScalarDS) {
                ((ScalarDS) dataObject).convertToUnsignedC();
                Object data = dataObject.getData();
                ByteOrder bo = ByteOrder.nativeOrder();

                if (binaryOrder == 1)
                    bo = ByteOrder.nativeOrder();
                else if (binaryOrder == 2)
                    bo = ByteOrder.LITTLE_ENDIAN;
                else if (binaryOrder == 3)
                    bo = ByteOrder.BIG_ENDIAN;

                Tools.saveAsBinary(out, data, bo);

                viewer.showStatus("Data saved to: " + fname);
            }
            else
                viewer.showError("Data not saved - not a ScalarDS");
        }
    }

    /**
     * Import data values from text file.
     *
     * @param fname
     *            the file to import text from
     */
    protected void importTextData(String fname) {
        int cols = selectionLayer.getPreferredColumnCount();
        int rows = selectionLayer.getPreferredRowCount();
        int r0;
        int c0;

        Rectangle lastSelection = selectionLayer.getLastSelectedRegion();
        if (lastSelection != null) {
            r0 = lastSelection.y;
            c0 = lastSelection.x;

            if (c0 < 0)
                c0 = 0;
            if (r0 < 0)
                r0 = 0;
        }
        else {
            r0 = 0;
            c0 = 0;
        }

        // Start at the first column for compound datasets
        if (dataObject instanceof CompoundDS)
            c0 = 0;

        String importLine = null;
        StringTokenizer tokenizer1 = null;
        try (BufferedReader in = new BufferedReader(new FileReader(fname))) {
            try {
                importLine = in.readLine();
            }
            catch (FileNotFoundException ex) {
                log.debug("import data values from text file {}:", fname, ex);
                return;
            }
            catch (IOException ex) {
                log.debug("read text file {}:", fname, ex);
                return;
            }

            String delName = ViewProperties.getDataDelimiter();
            String delimiter = "";

            if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_TAB))
                delimiter = "\t";
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SPACE))
                delimiter = " " + delimiter;
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COMMA))
                delimiter = ",";
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COLON))
                delimiter = ":";
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SEMI_COLON))
                delimiter = ";";
            String token = null;
            int r = r0;
            int c = c0;
            while ((importLine != null) && (r < rows)) {
                if (fixedDataLength > 0) {
                    // the data has fixed length
                    int n = importLine.length();
                    String theVal;
                    for (int i = 0; i < n; i = i + fixedDataLength) {
                        try {
                            theVal = importLine.substring(i, i + fixedDataLength);
                            dataProvider.setDataValue(c, r, theVal);
                        }
                        catch (Exception ex) {
                            continue;
                        }
                        c++;
                    }
                }
                else {
                    try {
                        tokenizer1 = new StringTokenizer(importLine, delimiter);
                        while (tokenizer1.hasMoreTokens() && (c < cols)) {
                            token = tokenizer1.nextToken();
                            StringTokenizer tokenizer2 = new StringTokenizer(token);
                            if (tokenizer2.hasMoreTokens()) {
                                while (tokenizer2.hasMoreTokens() && (c < cols)) {
                                    dataProvider.setDataValue(c, r, tokenizer2.nextToken());
                                    c++;
                                }
                            }
                            else
                                c++;
                        }
                    }
                    catch (Exception ex) {
                        Tools.showError(shell, "Import", ex.getMessage());
                        return;
                    }
                }

                try {
                    importLine = in.readLine();
                }
                catch (IOException ex) {
                    log.debug("read text file {}:", fname, ex);
                    importLine = null;
                }

                // Start at the first column for compound datasets
                if (dataObject instanceof CompoundDS)
                    c = 0;
                else
                    c = c0;

                r++;
            } // ((line != null) && (r < rows))
        }
        catch (IOException ex) {
            log.debug("import text file {}:", fname, ex);
        }
    }

    /**
     * Import data values from binary file.
     */
    protected void importBinaryData() {
        String currentDir = ((HObject) dataObject).getFileFormat().getParent();

        String filename = null;
        if (((HDFView) viewer).getTestState()) {
            filename = currentDir + File.separator + new InputDialog(shell, "Enter a file name", "").open();
        }
        else {
            FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
            fChooser.setFilterPath(currentDir);

            DefaultFileFilter filter = DefaultFileFilter.getFileFilterBinary();
            fChooser.setFilterExtensions(new String[] { "*", filter.getExtensions() });
            fChooser.setFilterNames(new String[] { "All Files", filter.getDescription() });
            fChooser.setFilterIndex(1);

            filename = fChooser.open();
        }

        if (filename == null)
            return;

        File chosenFile = new File(filename);
        if (!chosenFile.exists()) {
            Tools.showError(shell, "Import Data from Binary File", "Data import error: " + chosenFile.getName() + " does not exist.");
            return;
        }

        if (!Tools.showConfirm(shell, "Import Data from Binary File", "Do you want to paste selected data?"))
            return;

        ByteOrder bo = ByteOrder.nativeOrder();
        if (binaryOrder == 1)
            bo = ByteOrder.nativeOrder();
        else if (binaryOrder == 2)
            bo = ByteOrder.LITTLE_ENDIAN;
        else if (binaryOrder == 3)
            bo = ByteOrder.BIG_ENDIAN;

        try {
            if (Tools.getBinaryDataFromFile(dataValue, chosenFile.getAbsolutePath(), bo))
                dataProvider.setIsValueChanged(true);

            dataTable.doCommand(new StructuralRefreshCommand());
        }
        catch (Exception ex) {
            log.debug("importBinaryData():", ex);
        }
        catch (OutOfMemoryError e) {
            log.debug("importBinaryData(): Out of memory");
        }
    }

    /**
     * Convert selected data based on predefined math functions.
     */
    private void mathConversion() throws Exception {
        if (isReadOnly) {
            log.debug("mathConversion(): can't convert read-only data");
            return;
        }

        int cols = selectionLayer.getSelectedColumnPositions().length;
        if ((dataObject instanceof CompoundDS) && (cols > 1)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Convert", "Please select one column at a time for math conversion" + "for compound dataset.");
            log.debug("mathConversion(): more than one column selected for CompoundDS");
            return;
        }

        Object theData = getSelectedData();
        if (theData == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Convert", "No data is selected.");
            log.debug("mathConversion(): no data selected");
            return;
        }

        MathConversionDialog dialog = new MathConversionDialog(shell, theData);
        dialog.open();

        if (dialog.isConverted()) {
            if (dataObject instanceof CompoundDS) {
                Object colData = null;
                try {
                    colData = ((List<?>) dataObject.getData()).get(selectionLayer.getSelectedColumnPositions()[0]);
                }
                catch (Exception ex) {
                    log.debug("mathConversion(): ", ex);
                }

                if (colData != null) {
                    int size = Array.getLength(theData);
                    System.arraycopy(theData, 0, colData, 0, size);
                }
            }
            else {
                int rows = selectionLayer.getSelectedRowCount();

                // Since NatTable returns the selected row positions as a Set<Range>, convert
                // this to
                // an Integer[]
                Set<Range> rowPositions = selectionLayer.getSelectedRowPositions();
                Set<Integer> selectedRowPos = new LinkedHashSet<>();
                Iterator<Range> i1 = rowPositions.iterator();
                while (i1.hasNext())
                    selectedRowPos.addAll(i1.next().getMembers());

                int r0 = selectedRowPos.toArray(new Integer[0])[0];
                int c0 = selectionLayer.getSelectedColumnPositions()[0];

                int w = dataTable.getPreferredColumnCount() - 1;
                int idxSrc = 0;
                int idxDst = 0;

                for (int i = 0; i < rows; i++) {
                    idxDst = (r0 + i) * w + c0;
                    System.arraycopy(theData, idxSrc, dataValue, idxDst, cols);
                    idxSrc += cols;
                }
            }

            System.gc();

            dataProvider.setIsValueChanged(true);
        }
    }

    private void showLineplot() {
        // Since NatTable returns the selected row positions as a Set<Range>, convert
        // this to
        // an Integer[]
        Set<Range> rowPositions = selectionLayer.getSelectedRowPositions();
        Set<Integer> selectedRowPos = new LinkedHashSet<>();
        Iterator<Range> i1 = rowPositions.iterator();
        while (i1.hasNext()) {
            selectedRowPos.addAll(i1.next().getMembers());
        }

        Integer[] rows = selectedRowPos.toArray(new Integer[0]);
        int[] cols = selectionLayer.getSelectedColumnPositions();

        if ((rows == null) || (cols == null) || (rows.length <= 0) || (cols.length <= 0)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select", "Select rows/columns to draw line plot.");
            return;
        }

        int nrow = dataTable.getPreferredRowCount() - 1;
        int ncol = dataTable.getPreferredColumnCount() - 1;

        log.trace("DefaultTableView showLineplot: {} - {}", nrow, ncol);
        LinePlotOption lpo = new LinePlotOption(shell, SWT.NONE, nrow, ncol);
        lpo.open();

        int plotType = lpo.getPlotBy();
        if (plotType == LinePlotOption.NO_PLOT)
            return;

        boolean isRowPlot = (plotType == LinePlotOption.ROW_PLOT);
        int xIndex = lpo.getXindex();

        // figure out to plot data by row or by column
        // Plot data by rows if all columns are selected and part of
        // rows are selected, otherwise plot data by column
        double[][] data = null;
        int nLines = 0;
        String title = "Lineplot - " + ((HObject) dataObject).getPath() + ((HObject) dataObject).getName();
        String[] lineLabels = null;
        double[] yRange = { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
        double[] xData = null;

        if (isRowPlot) {
            title += " - by row";
            nLines = rows.length;
            if (nLines > 10) {
                shell.getDisplay().beep();
                nLines = 10;
                Tools.showWarning(shell, "Select",
                        "More than 10 rows are selected.\n" + "The first 10 rows will be displayed.");
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
                }
            }

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
        }
        else {
            title += " - by column";
            nLines = cols.length;
            if (nLines > 10) {
                shell.getDisplay().beep();
                nLines = 10;
                Tools.showWarning(shell, "Select",
                        "More than 10 columns are selected.\n" + "The first 10 columns will be displayed.");
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
                }
            }

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
        }

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
            Tools.showError(shell, "Select", "Cannot show line plot for the selected data. \n" + "Please check the data range: ("
                    + yRange[0] + ", " + yRange[1] + ").");
            return;
        }
        if (xData == null) { // use array index and length for x data range
            xData = new double[2];
            xData[0] = indexBase; // 1- or zero-based
            xData[1] = data[0].length + (double) indexBase - 1; // maximum index
        }

        Chart cv = new Chart(shell, title, Chart.LINEPLOT, data, xData, yRange);
        cv.setLineLabels(lineLabels);

        String cname = dataValue.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf('[') + 1);
        if ((dname == 'B') || (dname == 'S') || (dname == 'I') || (dname == 'J'))
            cv.setTypeToInteger();

        cv.open();
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
     * @return number of data points in the plot data if successful; otherwise,
     *         returns false.
     */
    private int removeInvalidPlotData(double[][] data, double[] xData, double[] yRange) {
        int idx = 0;
        boolean hasInvalid = false;

        if (data == null || yRange == null)
            return -1;

        yRange[0] = Double.POSITIVE_INFINITY;
        yRange[1] = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < data[0].length; i++) {
            hasInvalid = false;

            for (int j = 0; j < data.length; j++) {
                hasInvalid = Tools.isNaNINF(data[j][i]);
                if (xData != null)
                    hasInvalid = hasInvalid || Tools.isNaNINF(xData[i]);

                if (hasInvalid)
                    break;
                else {
                    data[j][idx] = data[j][i];
                    if (xData != null)
                        xData[idx] = xData[i];
                    yRange[0] = Math.min(yRange[0], data[j][idx]);
                    yRange[1] = Math.max(yRange[1], data[j][idx]);
                }
            }

            if (!hasInvalid)
                idx++;
        }

        return idx;
    }

    /**
     * An implementation of a GridLayer with support for column grouping and with
     * editing triggered by a double click instead of a single click.
     */
    protected class EditingGridLayer extends GridLayer
    {
        /** Create the Grid Layer with editing triggered by a
         *  double click instead of a single click.
         *
         * @param bodyLayer
         *        the body layer
         * @param columnHeaderLayer
         *        the Column Header layer
         * @param rowHeaderLayer
         *        the Row Header layer
         * @param cornerLayer
         *        the Corner Layer
         */
        public EditingGridLayer(ILayer bodyLayer, ILayer columnHeaderLayer, ILayer rowHeaderLayer, ILayer cornerLayer) {
            super(bodyLayer, columnHeaderLayer, rowHeaderLayer, cornerLayer, false);

            // Left-align cells, change font for rendering cell text
            // and add cell data display converter for displaying as
            // Hexadecimal, Binary, etc.
            this.addConfiguration(new AbstractRegistryConfiguration() {
                @Override
                public void configureRegistry(IConfigRegistry configRegistry) {
                    Style cellStyle = new Style();

                    cellStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
                    cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR,
                            Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

                    if (curFont != null)
                        cellStyle.setAttributeValue(CellStyleAttributes.FONT, curFont);
                    else
                        cellStyle.setAttributeValue(CellStyleAttributes.FONT, Display.getDefault().getSystemFont());

                    configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle,
                            DisplayMode.NORMAL, GridRegion.BODY);

                    configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle,
                            DisplayMode.SELECT, GridRegion.BODY);

                    // Add data display conversion capability
                    try {
                        dataDisplayConverter = DataDisplayConverterFactory.getDataDisplayConverter(dataObject);

                        configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
                                dataDisplayConverter, DisplayMode.NORMAL, GridRegion.BODY);
                    }
                    catch (Exception ex) {
                        log.debug("EditingGridLayer: failed to retrieve a DataDisplayConverter: ", ex);
                        dataDisplayConverter = null;
                    }
                }
            });

            if (isStdRef || isRegRef || isObjRef) {
                // Show data pointed to by reference on double click
                this.addConfiguration(new AbstractUiBindingConfiguration() {
                    @Override
                    public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
                        uiBindingRegistry.registerDoubleClickBinding(new MouseEventMatcher(), new IMouseAction() {
                            @Override
                            public void run(NatTable table, MouseEvent event) {
                                if (!(isStdRef || isRegRef || isObjRef))
                                    return;

                                viewType = ViewType.TABLE;

                                Object theData = null;
                                try {
                                    theData = ((Dataset) getDataObject()).getData();
                                }
                                catch (Exception ex) {
                                    log.debug("show reference data: ", ex);
                                    theData = null;
                                    Tools.showError(shell, "Select", ex.getMessage());
                                }

                                if (theData == null) {
                                    shell.getDisplay().beep();
                                    Tools.showError(shell, "Select", "No data selected.");
                                    return;
                                }

                                // Since NatTable returns the selected row positions as a Set<Range>, convert
                                // this to an Integer[]
                                Set<Range> rowPositions = selectionLayer.getSelectedRowPositions();
                                Set<Integer> selectedRowPos = new LinkedHashSet<>();
                                Iterator<Range> i1 = rowPositions.iterator();
                                while (i1.hasNext()) {
                                    selectedRowPos.addAll(i1.next().getMembers());
                                }

                                Integer[] selectedRows = selectedRowPos.toArray(new Integer[0]);
                                if (selectedRows == null || selectedRows.length <= 0) {
                                    log.debug("show reference data: no data selected");
                                    Tools.showError(shell, "Select", "No data selected.");
                                    return;
                                }
                                int len = Array.getLength(selectedRows);
                                for (int i = 0; i < len; i++) {
                                    byte[] rElements = null;
                                    if (theData instanceof ArrayList)
                                        rElements = (byte[]) ((ArrayList) theData).get(selectedRows[i]);
                                    else
                                        rElements = (byte[]) theData;

                                    if (isStdRef)
                                        showStdRefData(rElements);
                                    else if (isRegRef)
                                        showRegRefData(rElements);
                                    else if (isObjRef)
                                        showObjRefData(rElements);
                                }
                            }
                        });
                    }
                });
            }
            else {
                // Add default bindings for editing
                this.addConfiguration(new DefaultEditConfiguration());

                // Register cell editing rules with the table and add
                // data validation
                this.addConfiguration(new AbstractRegistryConfiguration() {
                    @Override
                    public void configureRegistry(IConfigRegistry configRegistry) {
                        IEditableRule editingRule = getDataEditingRule(dataObject);
                        if (editingRule != null) {
                            // Register cell editing rules with table
                            configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
                                    editingRule, DisplayMode.EDIT);
                        }

                        // Add data validator and validation error handler
                        DataValidator validator = null;
                        try {
                            validator = DataValidatorFactory.getDataValidator(dataObject);
                        }
                        catch (Exception ex) {
                            log.debug("EditingGridLayer: no DataValidator retrieved, data editing will be disabled");
                        }

                        if (validator != null) {
                            configRegistry.registerConfigAttribute(EditConfigAttributes.DATA_VALIDATOR, validator,
                                    DisplayMode.EDIT, GridRegion.BODY);
                        }

                        configRegistry.registerConfigAttribute(EditConfigAttributes.VALIDATION_ERROR_HANDLER,
                                new DialogErrorHandling(), DisplayMode.EDIT, GridRegion.BODY);
                    }
                });

                // Change cell editing to be on double click rather than single click
                // and allow editing of cells by pressing keys as well
                this.addConfiguration(new AbstractUiBindingConfiguration() {
                    @Override
                    public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
                        uiBindingRegistry.registerFirstKeyBinding(new LetterOrDigitKeyEventMatcher(), new KeyEditAction());
                        uiBindingRegistry.registerFirstDoubleClickBinding(
                                new CellEditorMouseEventMatcher(), new MouseEditAction());
                    }
                });
            }
        }
    }

    /**
     * An implementation of the table's Row Header which adapts to the current font.
     */
    protected class RowHeader extends RowHeaderLayer
    {
        /** Create the RowHeader which adapts to the current font.
         *
         * @param baseLayer
         *        the base layer
         * @param verticalLayerDependency
         *        the vertical layer dependency
         * @param selectionLayer
         *        the selection layer
         */
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
     * Custom Row Header data provider to set row indices based on Index Base for
     * both Scalar Datasets and Compound Datasets.
     */
    protected class RowHeaderDataProvider implements IDataProvider
    {
        private int    rank;
        private int    space_type;
        private long[] dims;
        private long[] startArray;
        private long[] strideArray;
        private int[]  selectedIndex;

        /** the start value. */
        protected int  start;
        /** the stride value. */
        protected int  stride;

        private int    nrows;

        /** Create the Row Header data provider to set row indices based on Index Base for
         *  both Scalar Datasets and Compound Datasets.
         *
         * @param theDataObject
         *        the data object
         */
        public RowHeaderDataProvider(DataFormat theDataObject) {
            this.space_type = theDataObject.getSpaceType();
            this.rank = theDataObject.getRank();
            this.dims = theDataObject.getSelectedDims();
            this.startArray = theDataObject.getStartDims();
            this.strideArray = theDataObject.getStride();
            this.selectedIndex = theDataObject.getSelectedIndex();

            if (rank > 1)
                this.nrows = (int) theDataObject.getHeight();
            else
                this.nrows = (int) dims[0];

            start = (int) startArray[selectedIndex[0]];
            stride = (int) strideArray[selectedIndex[0]];
        }


        /** Update the Row Header data provider to set row indices based on Index Base for
         *  both Scalar Datasets and Compound Datasets.
         *
         * @param theDataObject
         *        the data object
         */
        public void updateRows(DataFormat theDataObject) {
            this.rank = theDataObject.getRank();
            this.dims = theDataObject.getSelectedDims();
            this.selectedIndex = theDataObject.getSelectedIndex();

            if (rank > 1)
                this.nrows = (int) theDataObject.getHeight();
            else
                this.nrows = (int) dims[0];
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
            // Intentional
        }
    }

    /**
     * An implementation of the table's Column Header which adapts to the current
     * font.
     */
    protected class ColumnHeader extends ColumnHeaderLayer
    {
        /** Create the ColumnHeader which adapts to the current font.
         *
         * @param baseLayer
         *        the base layer
         * @param horizontalLayerDependency
         *        the horizontal layer dependency
         * @param selectionLayer
         *        the selection layer
         */
        public ColumnHeader(IUniqueIndexLayer baseLayer, ILayer horizontalLayerDependency,
                SelectionLayer selectionLayer) {
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

    /** Context-menu for dealing with region and object references */
    protected class RefContextMenu extends AbstractUiBindingConfiguration
    {
        private final Menu contextMenu;

        /** Create the Context-menu for dealing with region and object references.
         *
         * @param table
         *        the NatTable object
         */
        public RefContextMenu(NatTable table) {
            this.contextMenu = createMenu(table).build();
        }

        private void showRefTable() {
            log.trace("show reference data: Show data as {}", viewType);

            Object theData = getSelectedData();
            if (theData == null) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Select", "No data selected.");
                return;
            }
            if (!(theData instanceof byte[]) && !(theData instanceof ArrayList)) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Select", "Data selected is not a reference.");
                return;
            }
            log.trace("show reference data: Data is {}", theData);

            // Since NatTable returns the selected row positions as a Set<Range>, convert
            // this to an Integer[]
            Set<Range> rowPositions = selectionLayer.getSelectedRowPositions();
            Set<Integer> selectedRowPos = new LinkedHashSet<>();
            Iterator<Range> i1 = rowPositions.iterator();
            while (i1.hasNext())
                selectedRowPos.addAll(i1.next().getMembers());

            Integer[] selectedRows = selectedRowPos.toArray(new Integer[0]);
            int[] selectedCols = selectionLayer.getSelectedColumnPositions();
            if (selectedRows == null || selectedRows.length <= 0) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Select", "No data selected.");
                log.trace("show reference data: Show data as {}: selectedRows is empty", viewType);
                return;
            }

            int len = Array.getLength(selectedRows) * Array.getLength(selectedCols);
            log.trace("show reference data: Show data as {}: len={}", viewType, len);
            if (len > 1) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Select", "Reference selection must be one cell.");
                log.trace("show reference data: Show data as {}: Too much data", viewType);
                return;
            }

            for (int i = 0; i < len; i++) {
                byte[] rElements = null;
                if (theData instanceof ArrayList)
                    rElements = (byte[]) ((ArrayList) theData).get(i);
                else
                    rElements = (byte[]) theData;

                if (rElements.length == HDF5Constants.H5R_DSET_REG_REF_BUF_SIZE) {
                    showRegRefData(rElements);
                }
                else if (rElements.length == HDF5Constants.H5R_OBJ_REF_BUF_SIZE) {
                    showObjRefData(rElements);
                }
                else {
                    showStdRefData(rElements);
                }
            }
        }

        private PopupMenuBuilder createMenu(NatTable table) {
            Menu menu = new Menu(table);

            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText("Show As &Table");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    viewType = ViewType.TABLE;
                    showRefTable();
                }
            });

            item = new MenuItem(menu, SWT.PUSH);
            item.setText("Show As &Image");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    viewType = ViewType.IMAGE;
                    showRefTable();
                }
            });

            return new PopupMenuBuilder(table, menu);
        }

        @Override
        public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
            uiBindingRegistry.registerMouseDownBinding(
                    new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON),
                    new PopupMenuAction(this.contextMenu));
        }
    }

    private class LinePlotOption extends Dialog
    {
        private Shell linePlotOptionShell;

        private Button rowButton, colButton;

        private Combo rowBox, colBox;

        public static final int NO_PLOT = -1;
        public static final int ROW_PLOT = 0;
        public static final int COLUMN_PLOT = 1;

        private int nrow, ncol;

        private int idx_xaxis = -1;
        private int plotType = -1;

        public LinePlotOption(Shell parent, int style, int nrow, int ncol) {
            super(parent, style);

            this.nrow = nrow;
            this.ncol = ncol;
        }

        public void open() {
            Shell parent = getParent();
            linePlotOptionShell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
            linePlotOptionShell.setFont(curFont);
            linePlotOptionShell.setText("Line Plot Options -- " + ((HObject) dataObject).getName());
            linePlotOptionShell.setImages(ViewProperties.getHdfIcons());
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
                @Override
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
                @Override
                public void widgetSelected(SelectionEvent e) {
                    rowBox.setEnabled(true);
                    colBox.setEnabled(false);
                }
            });

            label = new Label(content, SWT.RIGHT);
            label.setFont(curFont);
            label.setText(" For abscissa use:");

            long[] startArray = dataObject.getStartDims();
            long[] strideArray = dataObject.getStride();
            int[] selectedIndex = dataObject.getSelectedIndex();
            int start = (int) startArray[selectedIndex[0]];
            int stride = (int) strideArray[selectedIndex[0]];

            colBox = new Combo(content, SWT.SINGLE | SWT.READ_ONLY);
            colBox.setFont(curFont);
            GridData colBoxData = new GridData(SWT.FILL, SWT.FILL, true, false);
            colBoxData.minimumWidth = 100;
            colBox.setLayoutData(colBoxData);

            colBox.add("array index");

            for (int i = 0; i < ncol; i++)
                colBox.add("column " + columnHeaderDataProvider.getDataValue(i, 0));

            rowBox = new Combo(content, SWT.SINGLE | SWT.READ_ONLY);
            rowBox.setFont(curFont);
            GridData rowBoxData = new GridData(SWT.FILL, SWT.FILL, true, false);
            rowBoxData.minimumWidth = 100;
            rowBox.setLayoutData(rowBoxData);

            rowBox.add("array index");

            for (int i = 0; i < nrow; i++)
                rowBox.add("row " + (start + indexBase + i * stride));

            // Create Ok/Cancel button region
            Composite buttonComposite = new Composite(linePlotOptionShell, SWT.NONE);
            buttonComposite.setLayout(new GridLayout(2, true));
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
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
                @Override
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
            while (!linePlotOptionShell.isDisposed())
                if (!display.readAndDispatch()) display.sleep();
        }

        int getXindex() {
            return idx_xaxis;
        }

        int getPlotBy() {
            return plotType;
        }
    }
}
