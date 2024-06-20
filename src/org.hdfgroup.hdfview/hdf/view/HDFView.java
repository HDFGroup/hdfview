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

package hdf.view;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import hdf.HDFVersions;
import hdf.object.DataFormat;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.view.DataView.DataView;
import hdf.view.DataView.DataViewFactory;
import hdf.view.DataView.DataViewFactoryProducer;
import hdf.view.DataView.DataViewManager;
import hdf.view.HelpView.HelpView;
import hdf.view.MetaDataView.MetaDataView;
import hdf.view.TableView.TableView;
import hdf.view.TreeView.DefaultTreeView;
import hdf.view.TreeView.TreeView;
import hdf.view.ViewProperties.DataViewType;
import hdf.view.dialog.ImageConversionDialog;
import hdf.view.dialog.InputDialog;
import hdf.view.dialog.UserOptionsDialog;
import hdf.view.dialog.UserOptionsGeneralPage;
import hdf.view.dialog.UserOptionsHDFPage;
import hdf.view.dialog.UserOptionsNode;
import hdf.view.dialog.UserOptionsViewModulesPage;

import hdf.hdf5lib.H5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * HDFView is the main class of this HDF visual tool. It is used to layout the graphical components of the
 * hdfview. The major GUI components of the HDFView include Menubar, Toolbar, TreeView, ContentView, and
 * MessageArea.
 *
 * The HDFView is designed in such a way that it does not have direct access to the HDF library. All the HDF
 * library access is done through HDF objects. Therefore, the HDFView package depends on the object package
 * but not the library package. The source code of the view package (hdf.view) should be compiled with the
 * library package (hdf.hdflib and hdf.hdf5lib).
 *
 * @author Jordan T. Henderson
 * @version 2.4 2015
 */
public class HDFView implements DataViewManager {
    private static final Logger log = LoggerFactory.getLogger(HDFView.class);

    private static Display display;
    private static Shell mainWindow;

    /* Determines whether HDFView is being executed for GUI testing */
    private boolean isTesting = false;

    /* The directory where HDFView is installed */
    private String rootDir;

    /* The initial directory where HDFView looks for files */
    private String startDir;

    /* The current working directory */
    private String currentDir;

    /* The current working file */
    private String currentFile = null;

    /* The view properties */
    private ViewProperties props;

    /* A list of tree view implementations. */
    private static List<String> treeViews;

    /* A list of image view implementations. */
    private static List<String> imageViews;

    /* A list of tree table implementations. */
    private static List<?> tableViews;

    /* A list of metadata view implementations. */
    private static List<?> metaDataViews;

    /* A list of palette view implementations. */
    private static List<?> paletteViews;

    /* A list of help view implementations. */
    private static List<?> helpViews;

    /* The list of GUI components related to NetCDF3 */
    private final List<MenuItem> n3GUIs = new ArrayList<>();

    /* The list of GUI components related to HDF4 */
    private final List<MenuItem> h4GUIs = new ArrayList<>();

    /* The list of GUI components related to HDF5 */
    private final List<MenuItem> h5GUIs = new ArrayList<>();

    /* The list of GUI components related to editing */
    // private final List<?>            editGUIs;

    /* GUI component: the TreeView */
    private TreeView treeView = null;

    private static final String JAVA_VERSION    = HDFVersions.getPropertyVersionJava();
    private static final String HDF4_VERSION    = HDFVersions.getPropertyVersionHDF4();
    private static final String HDF5_VERSION    = HDFVersions.getPropertyVersionHDF5();
    private static final String HDFVIEW_VERSION = HDFVersions.getPropertyVersionView();
    private static final String HDFVIEW_USERSGUIDE_URL =
        "https://support.hdfgroup.org/documentation/HDFVIEW/HDFView+3.x+User%27s+Guide";
    private static final String JAVA_COMPILER = "jdk " + JAVA_VERSION;
    private static final String JAVA_VER_INFO =
        "Compiled at " + JAVA_COMPILER + "\nRunning at " + System.getProperty("java.version");

    private static final String ABOUT_HDFVIEW = "HDF Viewer, "
                                                + "Version " + ViewProperties.VERSION + "\n"
                                                + "For " + System.getProperty("os.name") + "\n\n"
                                                + "Copyright " + '\u00a9' + " 2006 The HDF Group.\n"
                                                + "All rights reserved.";

    /* GUI component: The toolbar for open, close, help and hdf4 and hdf5 library information */
    private ToolBar toolBar;

    /* GUI component: The text area for showing status messages */
    private Text status;

    /* GUI component: The area for object view */
    private ScrolledComposite treeArea;

    /* GUI component: The area for quick general view */
    private ScrolledComposite generalArea;

    /* GUI component: To add and display URLs */
    private Combo urlBar;

    private Button recentFilesButton;
    private Button clearTextButton;

    /* GUI component: A list of current data windows */
    private Menu windowMenu;

    /* GUI component: File menu on the menubar */
    // private final Menu               fileMenu;

    /* The font to be used for display text on all Controls */
    private Font currentFont;

    private UserOptionsDialog userOptionDialog;

    /** State of refresh. */
    public boolean viewerState = false;

    /** Timer for refresh functions. */
    private final Runnable timer = new Runnable() {
        public void run()
        {
            // refresh each table displaying data
            Shell[] shellList = display.getShells();
            if (shellList != null) {
                for (int i = 0; i < shellList.length; i++) {
                    if (shellList[i].equals(mainWindow))
                        showMetaData(treeView.getCurrentObject());
                    else {
                        DataView view = (DataView)shellList[i].getData();
                        if ((view != null) && (view instanceof TableView)) {
                            HObject obj = view.getDataObject();
                            if (obj == null || obj.getFileFormat() == null || !(obj instanceof DataFormat))
                                continue;

                            FileFormat file = obj.getFileFormat();
                            if (file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))
                                ((TableView)view).refreshDataTable();
                        }
                    }
                }
            }
            log.trace("viewerState = {}", viewerState);
            if (viewerState)
                display.timerExec(ViewProperties.getTimerRefresh(), timer);
            else
                display.timerExec(-1, timer);
        }
    };

    /**
     * Constructs HDFView with a given root directory, where the HDFView is
     * installed, and opens the given files in the viewer.
     *
     * @param root
     *            the directory where the HDFView is installed.
     * @param start_dir
     *            the starting directory for file searches
     */
    public HDFView(String root, String start_dir)
    {
        log.debug("Root is {}", root);

        if (display == null || display.isDisposed())
            display = new Display();

        rootDir  = root;
        startDir = start_dir;

        // editGUIs = new Vector<Object>();

        props = new ViewProperties(rootDir, startDir);
        try {
            props.load();
        }
        catch (Exception ex) {
            log.debug("Failed to load View Properties from {}", rootDir);
        }

        ViewProperties.loadIcons();

        String workDir = System.getProperty("hdfview.workdir");
        if (workDir != null)
            currentDir = workDir;
        else
            currentDir = ViewProperties.getWorkDir();

        if (currentDir == null)
            currentDir = System.getProperty("user.dir");

        log.info("Current directory is {}", currentDir);

        try {
            currentFont =
                new Font(display, ViewProperties.getFontType(), ViewProperties.getFontSize(), SWT.NORMAL);
        }
        catch (Exception ex) {
            currentFont = null;
        }

        if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) != null)
            ViewProperties.loadPluginPaths();

        treeViews     = ViewProperties.getTreeViewList();
        metaDataViews = ViewProperties.getMetaDataViewList();
        tableViews    = ViewProperties.getTableViewList();
        imageViews    = ViewProperties.getImageViewList();
        paletteViews  = ViewProperties.getPaletteViewList();
        helpViews     = ViewProperties.getHelpViewList();

        log.debug("Constructor exit");
    }

    /**
     * Creates HDFView with a given size, and opens the given files in the viewer.
     *
     * @param flist
     *            a list of files to open.
     * @param width
     *            the width of the app in pixels
     * @param height
     *            the height of the app in pixels
     * @param x
     *            the coord x of the app in pixels
     * @param y
     *            the coord y of the app in pixels
     *
     * @return
     *            the newly-created HDFView Shell
     */
    public Shell openMainWindow(List<File> flist, int width, int height, int x, int y)
    {
        log.debug("openMainWindow enter current directory is {}", currentDir);

        // Initialize all GUI components
        mainWindow = createMainWindow();

        try {
            Font font    = null;
            String fType = ViewProperties.getFontType();
            int fSize    = ViewProperties.getFontSize();

            try {
                font = new Font(display, fType, fSize, SWT.NORMAL);
            }
            catch (Exception ex) {
                log.debug("Failed to load font");
                font = null;
            }

            if (font != null)
                updateFont(font);
        }
        catch (Exception ex) {
            log.debug("Failed to load Font properties");
        }

        // Make sure all GUI components are in place before
        // opening any files
        mainWindow.pack();

        int nfiles   = flist.size();
        File theFile = null;
        for (int i = 0; i < nfiles; i++) {
            theFile = flist.get(i);

            if (theFile.isFile()) {
                currentDir  = theFile.getParentFile().getAbsolutePath();
                currentFile = theFile.getAbsolutePath();

                try {
                    int access_mode = FileFormat.WRITE;
                    if (ViewProperties.isReadOnly())
                        access_mode = FileFormat.READ;
                    else if (ViewProperties.isReadSWMR())
                        access_mode = FileFormat.READ | FileFormat.MULTIREAD;
                    treeView.openFile(currentFile, access_mode);

                    try {
                        urlBar.remove(currentFile);
                    }
                    catch (Exception ex) {
                    }

                    // first entry is always the workdir
                    urlBar.add(currentFile, 1);
                    urlBar.select(1);
                }
                catch (Exception ex) {
                    showError(ex.toString());
                }
            }
            else {
                currentDir = theFile.getAbsolutePath();
            }

            log.info("CurrentDir is {}", currentDir);
        }

        if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3) == null)
            setEnabled(n3GUIs, false);

        if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) == null)
            setEnabled(h4GUIs, false);

        if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) == null)
            setEnabled(h5GUIs, false);

        // Set size of main window
        // float inset = 0.17f; // for UG only.
        float inset  = 0.04f;
        Point winDim = new Point(width, height);

        // If given height and width are too small, adjust accordingly
        if (height <= 300)
            winDim.y = (int)((1 - 2 * inset) * mainWindow.getSize().y);

        if (width <= 300)
            winDim.x = (int)(0.9 * mainWindow.getSize().y);

        mainWindow.setLocation(x, y);
        mainWindow.setSize(winDim.x + 200, winDim.y);

        // Display the window
        mainWindow.open();
        log.debug("openMainWindow exit");
        return mainWindow;
    }

    /** switch processing to the main application window */
    public void runMainWindow()
    {
        log.debug("runMainWindow enter");

        while (!mainWindow.isDisposed()) {
            // ===================================================
            // Wrap each event dispatch in an exception handler
            // so that if any event causes an exception it does
            // not break the main UI loop
            // ===================================================
            try {
                if (!display.readAndDispatch())
                    display.sleep();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!isTesting)
            display.dispose();
        log.debug("runMainWindow exit");
    }

    /**
     * Creates and lays out GUI components.
     *
     * <pre>
     * ||=========||=============================||
     * ||         ||                             ||
     * ||         ||                             ||
     * || TreeView||       ContentPane           ||
     * ||         ||                             ||
     * ||=========||=============================||
     * ||            Message Area                ||
     * ||========================================||
     * </pre>
     */
    private Shell createMainWindow()
    {
        // Create a new display window
        final Shell shell = new Shell(display);
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setFont(currentFont);
        shell.setText("HDFView " + HDFVIEW_VERSION);
        shell.setLayout(new GridLayout(3, false));
        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                ViewProperties.setRecentFiles(new ArrayList<>(Arrays.asList(urlBar.getItems())));

                try {
                    props.save();
                }
                catch (Exception ex) {
                }

                closeAllWindows();

                // Close all open files
                try {
                    List<FileFormat> filelist = treeView.getCurrentFiles();

                    if ((filelist != null) && !filelist.isEmpty()) {
                        Object[] files = filelist.toArray();

                        for (int i = 0; i < files.length; i++) {
                            try {
                                treeView.closeFile((FileFormat)files[i]);
                            }
                            catch (Exception ex) {
                                continue;
                            }
                        }
                    }
                }
                catch (Exception ex) {
                }

                if (currentFont != null)
                    currentFont.dispose();
            }
        });

        createMenuBar(shell);
        createToolbar(shell);
        createUrlToolbar(shell);
        createContentArea(shell);

        log.info("Main Window created");

        return shell;
    }

    private void createMenuBar(final Shell shell)
    {
        Menu menu = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menu);

        MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
        menuItem.setText("&File");

        Menu fileMenu = new Menu(menuItem);
        menuItem.setMenu(fileMenu);

        MenuItem item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("&Open\tCtrl-O");
        item.setAccelerator(SWT.MOD1 + 'O');
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                openLocalFile(null, -1);
            }
        });

        item = new MenuItem(fileMenu, SWT.CASCADE);
        item.setText("Open As");

        Menu openAsMenu = new Menu(item);
        item.setMenu(openAsMenu);

        item = new MenuItem(openAsMenu, SWT.PUSH);
        item.setText("Read-Only");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                openLocalFile(null, FileFormat.READ);
            }
        });

        item = new MenuItem(openAsMenu, SWT.PUSH);
        item.setText("SWMR Read-Only");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                openLocalFile(null, FileFormat.READ | FileFormat.MULTIREAD);
            }
        });

        item = new MenuItem(openAsMenu, SWT.PUSH);
        item.setText("Read/Write");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                openLocalFile(null, FileFormat.WRITE);
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem fileNewMenu = new MenuItem(fileMenu, SWT.CASCADE);
        fileNewMenu.setText("New");

        Menu newMenu = new Menu(fileNewMenu);
        fileNewMenu.setMenu(newMenu);

        item = new MenuItem(newMenu, SWT.PUSH);
        item.setText("HDF&4");
        h4GUIs.add(item);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (currentDir != null)
                    currentDir += File.separator;
                else
                    currentDir = "";

                String filename = null;

                if (!isTesting) {
                    FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
                    fChooser.setFileName(Tools.checkNewFile(currentDir, ".hdf").getName());

                    DefaultFileFilter filter = DefaultFileFilter.getFileFilterHDF4();
                    fChooser.setFilterExtensions(new String[] {filter.getExtensions()});
                    fChooser.setFilterNames(new String[] {filter.getDescription()});
                    fChooser.setFilterIndex(0);

                    filename = fChooser.open();
                }
                else {
                    // Prepend test file directory to filename
                    filename = currentDir.concat(new InputDialog(mainWindow, "Enter a file name", "").open());
                }

                if (filename == null)
                    return;

                try {
                    log.trace("HDFView create hdf4 file");
                    FileFormat theFile = Tools.createNewFile(filename, currentDir, FileFormat.FILE_TYPE_HDF4,
                                                             getTreeView().getCurrentFiles());

                    if (theFile == null)
                        return;

                    currentDir = theFile.getParent();
                }
                catch (Exception ex) {
                    Tools.showError(mainWindow, "New", ex.getMessage());
                    return;
                }

                try {
                    treeView.openFile(filename, FileFormat.WRITE);
                    currentFile = filename;

                    try {
                        urlBar.remove(filename);
                    }
                    catch (Exception ex) {
                        log.debug("unable to remove {} from urlBar", filename);
                    }

                    // first entry is always the workdir
                    urlBar.add(filename, 1);
                    urlBar.select(1);
                }
                catch (Exception ex) {
                    display.beep();
                    Tools.showError(mainWindow, "New", ex.getMessage() + "\n" + filename);
                }
            }
        });

        item = new MenuItem(newMenu, SWT.PUSH);
        item.setText("HDF&5");
        h5GUIs.add(item);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (currentDir != null)
                    currentDir += File.separator;
                else
                    currentDir = "";

                String filename = null;

                if (!isTesting) {
                    FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
                    fChooser.setFileName(Tools.checkNewFile(currentDir, ".h5").getName());

                    DefaultFileFilter filter = DefaultFileFilter.getFileFilterHDF5();
                    fChooser.setFilterExtensions(new String[] {filter.getExtensions()});
                    fChooser.setFilterNames(new String[] {filter.getDescription()});
                    fChooser.setFilterIndex(0);

                    filename = fChooser.open();
                }
                else {
                    // Prepend test file directory to filename
                    filename = currentDir.concat(new InputDialog(mainWindow, "Enter a file name", "").open());
                }

                if (filename == null)
                    return;

                try {
                    log.trace("HDFView create hdf5 file");
                    FileFormat theFile = Tools.createNewFile(filename, currentDir, FileFormat.FILE_TYPE_HDF5,
                                                             getTreeView().getCurrentFiles());

                    if (theFile == null)
                        return;

                    currentDir = theFile.getParent();
                }
                catch (Exception ex) {
                    Tools.showError(mainWindow, "New", ex.getMessage());
                    return;
                }

                try {
                    treeView.openFile(filename, FileFormat.WRITE);
                    currentFile = filename;

                    try {
                        urlBar.remove(filename);
                    }
                    catch (Exception ex) {
                        log.debug("unable to remove {} from urlBar", filename);
                    }

                    // first entry is always the workdir
                    urlBar.add(filename, 1);
                    urlBar.select(1);
                }
                catch (Exception ex) {
                    display.beep();
                    Tools.showError(mainWindow, "New", ex.getMessage() + "\n" + filename);
                }
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("&Close");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                closeFile(treeView.getSelectedFile());
            }
        });

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("Close &All");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                closeAllWindows();

                List<FileFormat> files = treeView.getCurrentFiles();
                while (!files.isEmpty()) {
                    try {
                        treeView.closeFile(files.get(0));
                    }
                    catch (Exception ex) {
                        log.trace("unable to close {} in treeView", files.get(0));
                    }
                }

                currentFile = null;

                for (Control control : generalArea.getChildren())
                    control.dispose();
                generalArea.setContent(null);

                urlBar.setText("");
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("&Save");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (treeView.getCurrentFiles().isEmpty()) {
                    Tools.showError(mainWindow, "Save", "No files currently open.");
                    return;
                }

                if (treeView.getSelectedFile() == null) {
                    Tools.showError(mainWindow, "Save", "No files currently selected.");
                    return;
                }

                // Save what has been changed in memory into file
                writeDataToFile(treeView.getSelectedFile());
            }
        });

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("S&ave As");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (treeView.getCurrentFiles().isEmpty()) {
                    Tools.showError(mainWindow, "Save", "No files currently open.");
                    return;
                }

                if (treeView.getSelectedFile() == null) {
                    Tools.showError(mainWindow, "Save", "No files currently selected.");
                    return;
                }

                try {
                    treeView.saveFile(treeView.getSelectedFile());
                }
                catch (Exception ex) {
                    display.beep();
                    Tools.showError(mainWindow, "Save", ex.getMessage());
                }
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("E&xit\tCtrl-Q");
        item.setAccelerator(SWT.MOD1 + 'Q');
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                mainWindow.dispose();
            }
        });

        menuItem = new MenuItem(menu, SWT.CASCADE);
        menuItem.setText("&Window");

        windowMenu = new Menu(menuItem);
        menuItem.setMenu(windowMenu);

        item = new MenuItem(windowMenu, SWT.PUSH);
        item.setText("&Cascade");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                cascadeWindows();
            }
        });

        item = new MenuItem(windowMenu, SWT.PUSH);
        item.setText("&Tile");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                tileWindows();
            }
        });

        new MenuItem(windowMenu, SWT.SEPARATOR);

        item = new MenuItem(windowMenu, SWT.PUSH);
        item.setText("Close &All");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                closeAllWindows();
            }
        });

        new MenuItem(windowMenu, SWT.SEPARATOR);

        menuItem = new MenuItem(menu, SWT.CASCADE);
        menuItem.setText("&Tools");

        Menu toolsMenu = new Menu(menuItem);
        menuItem.setMenu(toolsMenu);

        MenuItem convertMenuItem = new MenuItem(toolsMenu, SWT.CASCADE);
        convertMenuItem.setText("Convert Image To");

        Menu convertMenu = new Menu(convertMenuItem);
        convertMenuItem.setMenu(convertMenu);

        item = new MenuItem(convertMenu, SWT.PUSH);
        item.setText("HDF4");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                convertFile(Tools.FILE_TYPE_IMAGE, FileFormat.FILE_TYPE_HDF4);
            }
        });
        h4GUIs.add(item);

        item = new MenuItem(convertMenu, SWT.PUSH);
        item.setText("HDF5");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                convertFile(Tools.FILE_TYPE_IMAGE, FileFormat.FILE_TYPE_HDF5);
            }
        });
        h5GUIs.add(item);

        new MenuItem(toolsMenu, SWT.SEPARATOR);

        item = new MenuItem(toolsMenu, SWT.PUSH);
        item.setText("User &Options");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                // Create the preference manager
                PreferenceManager mgr = new PreferenceManager();

                // Create the nodes
                UserOptionsNode one   = new UserOptionsNode("general", new UserOptionsGeneralPage());
                UserOptionsNode two   = new UserOptionsNode("hdf", new UserOptionsHDFPage());
                UserOptionsNode three = new UserOptionsNode("modules", new UserOptionsViewModulesPage());

                // Add the nodes
                mgr.addToRoot(one);
                mgr.addToRoot(two);
                mgr.addToRoot(three);

                // Create the preferences dialog
                userOptionDialog = new UserOptionsDialog(shell, mgr, rootDir);

                // Set the preference store
                userOptionDialog.setPreferenceStore(props);
                userOptionDialog.create();

                // Open the dialog
                userOptionDialog.open();

                // TODO: this functionality is currently broken because isWorkDirChanged() is not exposed
                // correctly. if (userOptionDialog.isWorkDirChanged()) this will always overwrite the
                // currentDir until isWorkDirChanged() is fixed
                currentDir = ViewProperties.getWorkDir();

                // if (userOptionDialog.isFontChanged()) {
                Font font = null;

                try {
                    font = new Font(display, ViewProperties.getFontType(), ViewProperties.getFontSize(),
                                    SWT.NORMAL);
                }
                catch (Exception ex) {
                    font = null;
                }

                log.trace("update fonts");
                updateFont(font);
            }
        });

        new MenuItem(toolsMenu, SWT.SEPARATOR);

        item = new MenuItem(toolsMenu, SWT.PUSH);
        item.setText("&Register File Format");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                registerFileFormat();
            }
        });

        item = new MenuItem(toolsMenu, SWT.PUSH);
        item.setText("&Unregister File Format");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                unregisterFileFormat();
            }
        });

        menuItem = new MenuItem(menu, SWT.CASCADE);
        menuItem.setText("&Help");

        Menu helpMenu = new Menu(menuItem);
        menuItem.setMenu(helpMenu);

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("&User's Guide");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String usersGuideURL = ViewProperties.getUsersGuide();
                log.trace("usersGuideURL: {}", usersGuideURL);
                URL urlObject;
                try {
                    if (usersGuideURL != null)
                        urlObject = new URI(usersGuideURL).toURL();
                    else
                        urlObject = new URI(HDFVIEW_USERSGUIDE_URL).toURL();
                    org.eclipse.swt.program.Program.launch(urlObject.toString());
                }
                catch (Exception ex) {
                    log.debug("Could not instantiate Browser: {}", ex);
                }
            }
        });

        if ((helpViews != null) && !helpViews.isEmpty()) {
            int n = helpViews.size();
            for (int i = 0; i < n; i++) {
                HelpView theView = (HelpView)helpViews.get(i);
                item             = new MenuItem(helpMenu, SWT.PUSH);
                item.setText(theView.getLabel());
                // item.setActionCommand(theView.getActionCommand());
            }
        }

        new MenuItem(helpMenu, SWT.SEPARATOR);

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("HDF&4 Library Version");
        h4GUIs.add(item);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF4).open();
            }
        });

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("HDF&5 Library Version");
        h5GUIs.add(item);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF5).open();
            }
        });

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("&Java Version");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new JavaVersionDialog(mainWindow).open();
            }
        });

        new MenuItem(helpMenu, SWT.SEPARATOR);

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("Supported Fi&le Formats");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new SupportedFileFormatsDialog(mainWindow).open();
            }
        });

        new MenuItem(helpMenu, SWT.SEPARATOR);

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("&About...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new AboutDialog(mainWindow).open();
            }
        });

        setEnabled(Arrays.asList(windowMenu.getItems()), false);

        log.info("Menubar created");
    }

    private void createToolbar(final Shell shell)
    {
        toolBar = new ToolBar(shell, SWT.HORIZONTAL | SWT.RIGHT);
        toolBar.setFont(Display.getCurrent().getSystemFont());
        toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

        ToolItem openItem = new ToolItem(toolBar, SWT.PUSH);
        openItem.setToolTipText("Open");
        openItem.setImage(ViewProperties.getFileopenIcon());
        openItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                openLocalFile(null, -1);
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);

        ToolItem closeItem = new ToolItem(toolBar, SWT.PUSH);
        closeItem.setImage(ViewProperties.getFilecloseIcon());
        closeItem.setToolTipText("Close");
        closeItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                closeFile(treeView.getSelectedFile());
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR).setWidth(20);

        ToolItem helpItem = new ToolItem(toolBar, SWT.PUSH);
        helpItem.setImage(ViewProperties.getHelpIcon());
        helpItem.setToolTipText("Help");
        helpItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String ugPath = ViewProperties.getUsersGuide();

                if (ugPath == null || !ugPath.startsWith("http://")) {
                    String sep   = File.separator;
                    File tmpFile = new File(ugPath);

                    if (!(tmpFile.exists())) {
                        ugPath  = rootDir + sep + "UsersGuide" + sep + "index.html";
                        tmpFile = new File(ugPath);

                        if (!(tmpFile.exists()))
                            ugPath = HDFVIEW_USERSGUIDE_URL;

                        ViewProperties.setUsersGuide(ugPath);
                    }
                }

                try {
                    org.eclipse.swt.program.Program.launch(ugPath);
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Help", ex.getMessage());
                }
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);

        ToolItem hdf4Item = new ToolItem(toolBar, SWT.PUSH);
        hdf4Item.setImage(ViewProperties.getH4Icon());
        hdf4Item.setToolTipText("HDF4 Library Version");
        hdf4Item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF4).open();
            }
        });

        if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) == null)
            hdf4Item.setEnabled(false);

        new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);

        ToolItem hdf5Item = new ToolItem(toolBar, SWT.PUSH);
        hdf5Item.setImage(ViewProperties.getH5Icon());
        hdf5Item.setToolTipText("HDF5 Library Version");
        hdf5Item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF5).open();
            }
        });

        if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) == null)
            hdf5Item.setEnabled(false);

        // Make the toolbar as wide as the window and as
        // tall as the buttons
        toolBar.setSize(shell.getClientArea().width, openItem.getBounds().height);
        toolBar.setLocation(0, 0);

        log.info("Toolbar created");
    }

    private void createUrlToolbar(final Shell shell)
    {
        // Recent Files button
        recentFilesButton = new Button(shell, SWT.PUSH);
        recentFilesButton.setFont(currentFont);
        recentFilesButton.setText("Recent Files");
        recentFilesButton.setToolTipText("List of recent files");
        recentFilesButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        recentFilesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                urlBar.setListVisible(true);
            }
        });

        // Recent files combo box
        urlBar = new Combo(shell, SWT.BORDER | SWT.SINGLE);
        urlBar.setFont(currentFont);
        urlBar.setItems(ViewProperties.getMRF().toArray(new String[0]));
        urlBar.setVisibleItemCount(ViewProperties.MAX_RECENT_FILES);
        urlBar.deselectAll();
        urlBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        urlBar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR) {
                    String filename = urlBar.getText();
                    if (filename == null || filename.length() < 1 || filename.equals(currentFile))
                        return;

                    if (!(filename.startsWith("http://") || filename.startsWith("https://") ||
                          filename.startsWith("ftp://"))) {
                        openLocalFile(filename, -1);
                    }
                    else {
                        String remoteFile = openRemoteFile(filename);

                        if (remoteFile != null)
                            openLocalFile(remoteFile, -1);
                    }
                }
            }
        });
        urlBar.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String filename = urlBar.getText();
                if (filename == null || filename.length() < 1 || filename.equals(currentFile)) {
                    return;
                }

                if (!(filename.startsWith("http://") || filename.startsWith("https://") ||
                      filename.startsWith("ftp://"))) {
                    openLocalFile(filename, -1);
                }
                else {
                    String remoteFile = openRemoteFile(filename);

                    if (remoteFile != null)
                        openLocalFile(remoteFile, -1);
                }
            }
        });

        clearTextButton = new Button(shell, SWT.PUSH);
        clearTextButton.setToolTipText("Clear current selection");
        clearTextButton.setFont(currentFont);
        clearTextButton.setText("Clear Text");
        clearTextButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        clearTextButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                urlBar.setText("");
                urlBar.deselectAll();
            }
        });

        log.info("URL Toolbar created");
    }

    private void createContentArea(final Shell shell)
    {
        SashForm content = new SashForm(shell, SWT.VERTICAL);
        content.setSashWidth(10);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        // Add Data content area and Status Area to main window
        Composite container = new Composite(content, SWT.NONE);
        container.setLayout(new FillLayout());

        Composite statusArea = new Composite(content, SWT.NONE);
        statusArea.setLayout(new FillLayout(SWT.HORIZONTAL));

        final SashForm contentArea = new SashForm(container, SWT.HORIZONTAL);
        contentArea.setSashWidth(10);

        // Add TreeView and DataView to content area pane
        treeArea = new ScrolledComposite(contentArea, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        treeArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        treeArea.setExpandHorizontal(true);
        treeArea.setExpandVertical(true);

        generalArea = new ScrolledComposite(contentArea, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        generalArea.setExpandHorizontal(true);
        generalArea.setExpandVertical(true);
        generalArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        generalArea.setMinHeight(contentArea.getSize().y - 2);

        // Create status area for displaying messages and metadata
        status = new Text(statusArea, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
        status.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        status.setEditable(false);
        status.setFont(currentFont);

        contentArea.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event arg0)
            {
                generalArea.setMinHeight(contentArea.getSize().y - 2);
            }
        });

        // Add drag and drop support for opening files
        DropTarget target               = new DropTarget(treeArea, DND.DROP_COPY);
        final FileTransfer fileTransfer = FileTransfer.getInstance();
        target.setTransfer(new Transfer[] {fileTransfer});
        target.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetEvent e)
            {
                e.detail = DND.DROP_COPY;
            }
            @Override
            public void dragOver(DropTargetEvent e)
            {
                // Intentional
            }
            @Override
            public void dragOperationChanged(DropTargetEvent e)
            {
                // Intentional
            }
            @Override
            public void dragLeave(DropTargetEvent e)
            {
                // Intentional
            }
            @Override
            public void dropAccept(DropTargetEvent e)
            {
                // Intentional
            }
            @Override
            public void drop(DropTargetEvent e)
            {
                if (fileTransfer.isSupportedType(e.currentDataType)) {
                    String[] files = (String[])e.data;
                    for (int i = 0; i < files.length; i++)
                        openLocalFile(files[i], -1);
                }
            }
        });

        showStatus("HDFView root - " + rootDir);
        showStatus("User property file - " + ViewProperties.getPropertyFile());

        content.setWeights(new int[] {9, 1});
        contentArea.setWeights(new int[] {1, 3});

        DataViewFactory treeViewFactory = null;
        try {
            treeViewFactory = DataViewFactoryProducer.getFactory(DataViewType.TREEVIEW);
        }
        catch (Exception ex) {
            log.debug("createContentArea(): error occurred while instantiating TreeView factory class", ex);
            this.showError("Error occurred while instantiating TreeView factory class");
            return;
        }

        if (treeViewFactory == null) {
            log.debug("createContentArea(): TreeView factory is null");
            return;
        }

        try {
            treeView = treeViewFactory.getTreeView(treeArea, this);

            if (treeView == null) {
                log.debug("createContentArea(): error occurred while instantiating TreeView class");
                this.showError("Error occurred while instantiating TreeView class");
                return;
            }
        }
        catch (ClassNotFoundException ex) {
            log.debug("createContentArea(): no suitable TreeView class found");
            this.showError("Unable to find suitable TreeView class");
            return;
        }

        treeArea.setContent(treeView.getTree());

        log.info("Content Area created");
    }

    /**
     * Get a list of treeview implementations.
     *
     * @return a list of treeview implementations.
     */
    public static final List<String> getListOfTreeViews() { return treeViews; }

    /**
     * Get a list of imageview implementations.
     *
     * @return a list of imageview implementations.
     */
    public static final List<String> getListOfImageViews() { return imageViews; }

    /**
     * Get a list of tableview implementations.
     *
     * @return a list of tableview implementations.
     */
    public static final List<?> getListOfTableViews() { return tableViews; }

    /**
     * Get a list of metaDataview implementations.
     *
     * @return a list of metaDataview implementations.
     */
    public static final List<?> getListOfMetaDataViews() { return metaDataViews; }

    /**
     * Get a list of paletteview implementations.
     *
     * @return a list of paletteview implementations.
     */
    public static final List<?> getListOfPaletteViews() { return paletteViews; }

    @Override
    public TreeView getTreeView()
    {
        return treeView;
    }

    /**
     * Get the combobox associated with a URL entry.
     *
     * @return the combobox associated with a URL entry.
     */
    public Combo getUrlBar() { return urlBar; }

    /**
     * Start stop a timer.
     *
     * @param toggleTimer
     *            -- true: start timer, false stop timer.
     */
    @Override
    public final void executeTimer(boolean toggleTimer)
    {
        showStatus("toggleTimer: " + toggleTimer);
        viewerState = toggleTimer;
        if (viewerState)
            display.timerExec(ViewProperties.getTimerRefresh(), timer);
        else
            display.timerExec(-1, timer);
    }

    /**
     * Display feedback message.
     *
     * @param msg
     *            the message to display.
     */
    @Override
    public void showStatus(String msg)
    {
        if (status == null) {
            log.debug("showStatus(): status area is null");
            return;
        }

        status.append(msg);
        status.append("\n");
    }

    /**
     * Display error message
     *
     * @param errMsg
     *            the error message to display
     */
    @Override
    public void showError(String errMsg)
    {
        if (status == null) {
            log.debug("showError(): status area is null");
            return;
        }

        status.append(" *** ");
        status.append(errMsg);
        if (log.isDebugEnabled())
            status.append(" - see log for more info");
        status.append(" *** ");
        status.append("\n");
    }

    /**
     * Display the metadata view for an object
     *
     * @param obj
     *            the object containing the metadata to show
     */
    public void showMetaData(final HObject obj)
    {
        for (Control control : generalArea.getChildren())
            control.dispose();
        generalArea.setContent(null);

        if (obj == null)
            return;

        DataViewFactory metaDataViewFactory = null;
        try {
            metaDataViewFactory = DataViewFactoryProducer.getFactory(DataViewType.METADATA);
        }
        catch (Exception ex) {
            log.debug("showMetaData(): error occurred while instantiating MetaDataView factory class", ex);
            this.showError("Error occurred while instantiating MetaDataView factory class");
            return;
        }

        if (metaDataViewFactory == null) {
            log.debug("showMetaData(): MetaDataView factory is null");
            return;
        }

        MetaDataView theView;
        try {
            theView = metaDataViewFactory.getMetaDataView(generalArea, this, obj);

            if (theView == null) {
                log.debug("showMetaData(): error occurred while instantiating MetaDataView class");
                this.showError("Error occurred while instantiating MetaDataView class");
                return;
            }
        }
        catch (ClassNotFoundException ex) {
            log.debug("showMetaData(): no suitable MetaDataView class found");
            this.showError("Unable to find suitable MetaDataView class");
            return;
        }
    }

    /**
     * close the file currently selected in the application
     *
     * @param theFile
     *        the file selected or specified
     */
    public void closeFile(FileFormat theFile)
    {
        if (theFile == null) {
            display.beep();
            Tools.showError(mainWindow, "Close", "Select a file to close");
            return;
        }

        // Close all the data windows of this file
        Shell[] views = display.getShells();
        if (views != null) {
            for (int i = 0; i < views.length; i++) {
                Object shellData = views[i].getData();

                if (!(shellData instanceof DataView))
                    continue;

                if ((DataView)shellData != null) {
                    HObject obj = ((DataView)shellData).getDataObject();

                    if (obj == null || obj.getFileFormat() == null)
                        continue;

                    if (obj.getFileFormat().equals(theFile)) {
                        views[i].dispose();
                        views[i] = null;
                    }
                }
            }
        }

        int index = urlBar.getSelectionIndex();
        if (index >= 0) {
            String fName = urlBar.getItem(urlBar.getSelectionIndex());
            if (theFile.getFilePath().equals(fName)) {
                currentFile = null;
                urlBar.setText("");
            }
        }

        try {
            treeView.closeFile(theFile);
        }
        catch (Exception ex) {
            // Intentional
        }

        for (Control control : generalArea.getChildren())
            control.dispose();
        generalArea.setContent(null);

        System.gc();
    }

    /**
     * Write the change of data to the given file.
     *
     * @param theFile
     *           The file to be updated.
     */
    public void writeDataToFile(FileFormat theFile)
    {
        try {
            Shell[] openShells = display.getShells();

            if (openShells != null) {
                for (int i = 0; i < openShells.length; i++) {
                    DataView theView = (DataView)openShells[i].getData();

                    if (theView instanceof TableView) {
                        TableView tableView = (TableView)theView;
                        FileFormat file     = tableView.getDataObject().getFileFormat();
                        if (file.equals(theFile))
                            tableView.updateValueInFile();
                    }
                }
            }
        }
        catch (Exception ex) {
            display.beep();
            Tools.showError(mainWindow, "Save", ex.getMessage());
        }
    }

    @Override
    public void addDataView(DataView dataView)
    {
        if (dataView == null || dataView instanceof MetaDataView)
            return;

        // Check if the data content is already displayed
        Shell[] shellList = display.getShells();
        if (shellList != null) {
            for (int i = 0; i < shellList.length; i++) {
                if (dataView.equals(shellList[i].getData()) && shellList[i].isVisible()) {
                    showWindow(shellList[i]);
                    return;
                }
            }
        }

        // First window being added
        if (shellList != null && shellList.length == 2)
            setEnabled(Arrays.asList(windowMenu.getItems()), true);

        HObject obj = dataView.getDataObject();
        String fullPath =
            ((obj.getPath() == null) ? "" : obj.getPath()) + ((obj.getName() == null) ? "" : obj.getName());

        MenuItem item = new MenuItem(windowMenu, SWT.PUSH);
        item.setText(fullPath);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Shell[] sList = display.getShells();

                for (int i = 0; i < sList.length; i++) {
                    DataView view = (DataView)sList[i].getData();

                    if (view != null) {
                        HObject obj = view.getDataObject();

                        if (obj.getFullName().equals(((MenuItem)e.widget).getText()))
                            showWindow(sList[i]);
                    }
                }
            }
        });

        mainWindow.setCursor(null);
    }

    @Override
    public void removeDataView(DataView dataView)
    {
        if (mainWindow.isDisposed())
            return;

        HObject obj = dataView.getDataObject();
        if (obj == null)
            return;

        MenuItem[] items = windowMenu.getItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i].getText().equals(obj.getFullName()))
                items[i].dispose();
        }

        // Last window being closed
        if (display.getShells().length == 2)
            for (MenuItem item : windowMenu.getItems())
                item.setEnabled(false);
    }

    @Override
    public DataView getDataView(HObject dataObject)
    {
        Shell[] openShells             = display.getShells();
        DataView view                  = null;
        HObject currentObj             = null;
        FileFormat currentDataViewFile = null;

        for (int i = 0; i < openShells.length; i++) {
            view = (DataView)openShells[i].getData();

            if (view != null) {
                currentObj = view.getDataObject();
                if (currentObj == null)
                    continue;

                currentDataViewFile = currentObj.getFileFormat();

                if (currentObj.equals(dataObject) && currentDataViewFile.equals(dataObject.getFileFormat()))
                    return view;
            }
        }

        return null;
    }

    /**
     * Set the testing state that determines if HDFView
     * is being executed for GUI testing.
     *
     * @param testing
     *           Provides SWTBot native dialog compatibility
     *           workarounds if set to true.
     */
    public void setTestState(boolean testing) { isTesting = testing; }

    /**
     * Get the testing state that determines if HDFView
     * is being executed for GUI testing.
     *
     * @return true if HDFView is being executed for GUI testing.
     */
    public boolean getTestState() { return isTesting; }

    /**
     * Set default UI fonts.
     */
    private void updateFont(Font font)
    {
        if (currentFont != null)
            currentFont.dispose();

        log.trace("updateFont():");
        currentFont = font;

        mainWindow.setFont(font);
        recentFilesButton.setFont(font);
        recentFilesButton.requestLayout();
        urlBar.setFont(font);
        urlBar.requestLayout();
        clearTextButton.setFont(font);
        clearTextButton.requestLayout();
        status.setFont(font);

        // On certain platforms the url_bar items don't update their size after
        // a font change. Removing and replacing them fixes this.
        for (String item : urlBar.getItems()) {
            urlBar.remove(item);
            urlBar.add(item);
        }

        treeArea.setFont(font);
        treeArea.requestLayout();
        for (Control control : treeArea.getChildren()) {
            control.setFont(font);
            control.requestLayout();
        }

        generalArea.setFont(font);
        generalArea.requestLayout();
        for (Control control : generalArea.getChildren()) {
            control.setFont(font);
            control.requestLayout();
        }

        if (treeView.getSelectedFile() != null)
            urlBar.select(0);

        if (treeView instanceof DefaultTreeView)
            ((DefaultTreeView)treeView).updateFont(font);

        Shell[] shellList = display.getShells();
        if (shellList != null) {
            for (int i = 0; i < shellList.length; i++) {
                shellList[i].setFont(font);
                shellList[i].requestLayout();
            }
        }

        mainWindow.requestLayout();
    }

    /**
     * Bring window to the front.
     *
     * @param name
     *               the name of the window to show.
     */
    private void showWindow(final Shell shell)
    {
        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run()
            {
                shell.forceActive();
            }
        });
    }

    /**
     * Cascade all windows.
     */
    private void cascadeWindows()
    {
        Shell[] sList = display.getShells();

        // Return if main window (shell) is the only open shell
        if (sList.length <= 1)
            return;

        Shell shell = null;

        Rectangle bounds = Display.getCurrent().getPrimaryMonitor().getClientArea();
        int w            = Math.max(50, bounds.width - 100);
        int h            = Math.max(50, bounds.height - 100);

        int x = bounds.x;
        int y = bounds.y;

        for (int i = 0; i < sList.length; i++) {
            shell = sList[i];
            shell.setBounds(x, y, w, h);
            shell.setActive();
            x += 20;
            y += 20;
        }
    }

    /**
     * Tile all windows.
     */
    private void tileWindows()
    {
        Shell[] sList = display.getShells();

        // Return if main window (shell) is the only open shell
        if (sList.length <= 1)
            return;

        int x       = 0;
        int y       = 0;
        int idx     = 0;
        Shell shell = null;

        int n    = sList.length;
        int cols = (int)Math.sqrt(n);
        int rows = (int)Math.ceil((double)n / (double)cols);

        Rectangle bounds = Display.getCurrent().getPrimaryMonitor().getClientArea();
        int w            = bounds.width / cols;
        int h            = bounds.height / rows;

        y = bounds.y;
        for (int i = 0; i < rows; i++) {
            x = bounds.x;

            for (int j = 0; j < cols; j++) {
                idx = i * cols + j;
                if (idx >= n)
                    return;

                shell = sList[idx];
                shell.setBounds(x, y, w, h);
                shell.setActive();
                x += w;
            }

            y += h;
        }
    }

    /**
     * Closes all windows.
     */
    private void closeAllWindows()
    {
        Shell[] sList = display.getShells();

        for (int i = 0; i < sList.length; i++) {
            if (sList[i].equals(mainWindow))
                continue;
            sList[i].dispose();
        }
    }

    /* Enable and disable GUI components */
    private static void setEnabled(List<MenuItem> list, boolean b)
    {
        Iterator<MenuItem> it = list.iterator();

        while (it.hasNext())
            it.next().setEnabled(b);
    }

    /** Open local file */
    private void openLocalFile(String filename, int fileAccessID)
    {
        log.trace("openLocalFile {},{}", filename, fileAccessID);

        /*
         * If given a specific access mode, use it without changing it. If not given a
         * specific access mode, check the current status of the "is read only" property
         * to determine how to open the file. This is to allow one time overrides of the
         * default file access mode when opening a file.
         */
        int accessMode = fileAccessID;
        if (accessMode < 0) {
            if (ViewProperties.isReadOnly())
                accessMode = FileFormat.READ;
            else if (ViewProperties.isReadSWMR())
                accessMode = FileFormat.READ | FileFormat.MULTIREAD;
            else
                accessMode = FileFormat.WRITE;
        }

        String[] selectedFilenames = null;
        File[] chosenFiles         = null;

        if (filename != null) {
            File file = new File(filename);
            if (!file.exists()) {
                Tools.showError(mainWindow, "Open", "File " + filename + " does not exist.");
                return;
            }

            if (file.isDirectory()) {
                currentDir = filename;
                openLocalFile(null, -1);
            }
            else {
                currentFile = filename;

                try {
                    treeView.openFile(filename, accessMode);
                }
                catch (Exception ex) {
                    try {
                        treeView.openFile(filename, FileFormat.READ);
                    }
                    catch (Exception ex2) {
                        display.beep();
                        urlBar.deselectAll();
                        Tools.showError(mainWindow, "Open", "Failed to open file " + filename + "\n" + ex2);
                        currentFile = null;
                    }
                }
            }

            try {
                urlBar.remove(filename);
            }
            catch (Exception ex) {
                log.trace("unable to remove {} from urlBar", filename);
            }

            // first entry is always the workdir
            urlBar.add(filename, 1);
            urlBar.select(1);
        }
        else {
            if (!isTesting) {
                log.trace("openLocalFile filename is null");
                FileDialog fChooser = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
                String modeStr      = "Read/Write";
                boolean isSWMRFile  = (FileFormat.MULTIREAD == (accessMode & FileFormat.MULTIREAD));
                if (isSWMRFile)
                    modeStr = "SWMR Read-only";
                else if (accessMode == FileFormat.READ)
                    modeStr = "Read-only";
                fChooser.setText(mainWindow.getText() + " - Open File " + modeStr);
                fChooser.setFilterPath(currentDir);

                DefaultFileFilter filter = DefaultFileFilter.getFileFilter();
                fChooser.setFilterExtensions(new String[] {"*", filter.getExtensions()});
                fChooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
                fChooser.setFilterIndex(1);

                fChooser.open();

                selectedFilenames = fChooser.getFileNames();
                if (selectedFilenames.length <= 0)
                    return;

                chosenFiles = new File[selectedFilenames.length];
                for (int i = 0; i < chosenFiles.length; i++) {
                    log.trace("openLocalFile selectedFilenames[{}]: {}", i, selectedFilenames[i]);
                    chosenFiles[i] =
                        new File(fChooser.getFilterPath() + File.separator + selectedFilenames[i]);

                    if (!chosenFiles[i].exists()) {
                        Tools.showError(mainWindow, "Open",
                                        "File " + chosenFiles[i].getName() + " does not exist.");
                        continue;
                    }

                    if (chosenFiles[i].isDirectory())
                        currentDir = chosenFiles[i].getPath();
                    else
                        currentDir = chosenFiles[i].getParent();

                    try {
                        urlBar.remove(chosenFiles[i].getAbsolutePath());
                    }
                    catch (Exception ex) {
                        log.trace("unable to remove {} from urlBar", chosenFiles[i].getAbsolutePath());
                    }

                    // first entry is always the workdir
                    urlBar.add(chosenFiles[i].getAbsolutePath(), 1);
                    urlBar.select(1);

                    log.trace("openLocalFile treeView.openFile(accessMode={} chosenFiles[{}]: {}", accessMode,
                              i, chosenFiles[i].getAbsolutePath());
                    try {
                        treeView.openFile(chosenFiles[i].getAbsolutePath(), accessMode + FileFormat.OPEN_NEW);
                    }
                    catch (Exception ex) {
                        try {
                            treeView.openFile(chosenFiles[i].getAbsolutePath(), FileFormat.READ);
                        }
                        catch (Exception ex2) {
                            display.beep();
                            urlBar.deselectAll();
                            Tools.showError(mainWindow, "Open",
                                            "Failed to open file " + selectedFilenames[i] + "\n" + ex2);
                            currentFile = null;
                        }
                    }
                }

                currentFile = chosenFiles[0].getAbsolutePath();
            }
            else {
                // Prepend test file directory to filename
                String fName =
                    currentDir + File.separator + new InputDialog(mainWindow, "Enter a file name", "").open();

                File chosenFile = new File(fName);

                if (!chosenFile.exists()) {
                    Tools.showError(mainWindow, "Open", "File " + chosenFile.getName() + " does not exist.");
                    return;
                }

                if (chosenFile.isDirectory())
                    currentDir = chosenFile.getPath();
                else
                    currentDir = chosenFile.getParent();

                try {
                    urlBar.remove(chosenFile.getAbsolutePath());
                }
                catch (Exception ex) {
                    log.trace("unable to remove {} from urlBar", chosenFile.getAbsolutePath());
                }

                // first entry is always the workdir
                urlBar.add(chosenFile.getAbsolutePath(), 1);
                urlBar.select(1);

                log.trace("openLocalFile treeView.openFile(chosenFile[{}]: {}", chosenFile.getAbsolutePath(),
                          accessMode + FileFormat.OPEN_NEW);
                try {
                    treeView.openFile(chosenFile.getAbsolutePath(), accessMode + FileFormat.OPEN_NEW);
                }
                catch (Exception ex) {
                    try {
                        treeView.openFile(chosenFile.getAbsolutePath(), FileFormat.READ);
                    }
                    catch (Exception ex2) {
                        display.beep();
                        urlBar.deselectAll();
                        Tools.showError(mainWindow, "Open", "Failed to open file " + chosenFile + "\n" + ex2);
                        currentFile = null;
                    }
                }

                currentFile = chosenFile.getAbsolutePath();
            }
        }
    }

    /** Load remote file and save it to local temporary directory */
    private String openRemoteFile(String urlStr)
    {
        if (urlStr == null)
            return null;

        String localFile = null;

        if (urlStr.startsWith("http://"))
            localFile = urlStr.substring(7);
        else if (urlStr.startsWith("https://"))
            localFile = urlStr.substring(8);
        else if (urlStr.startsWith("ftp://"))
            localFile = urlStr.substring(6);
        else
            return null;

        localFile = localFile.replace('/', '@');
        localFile = localFile.replace('\\', '@');

        // Search the local file cache
        String tmpDir = System.getProperty("java.io.tmpdir");

        File tmpFile = new File(tmpDir);
        if (!tmpFile.canWrite())
            tmpDir = System.getProperty("user.home");

        localFile = tmpDir + File.separator + localFile;

        tmpFile = new File(localFile);
        if (tmpFile.exists())
            return localFile;

        URL url = null;

        try {
            url = new URL(urlStr);
        }
        catch (Exception ex) {
            url = null;
            display.beep();
            Tools.showError(mainWindow, "Open", ex.getMessage());
            return null;
        }

        try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
                mainWindow.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
                byte[] buff = new byte[512]; // set default buffer size to 512
                int n       = 0;
                while ((n = in.read(buff)) > 0)
                    out.write(buff, 0, n);
            }
            catch (Exception ex) {
                log.debug("Remote file: ", ex);
                throw ex;
            }
        }
        catch (Exception ex) {
            display.beep();
            Tools.showError(mainWindow, "Open", ex.getMessage());
            // Want to call setCursor always
            localFile = null;
        }

        mainWindow.setCursor(null);

        return localFile;
    }

    private void convertFile(String typeFrom, String typeTo)
    {
        ImageConversionDialog dialog =
            new ImageConversionDialog(mainWindow, typeFrom, typeTo, currentDir, treeView.getCurrentFiles());
        dialog.open();

        if (dialog.isFileConverted()) {
            String filename = dialog.getConvertedFile();
            File theFile    = new File(filename);

            if (!theFile.exists())
                return;

            currentDir  = theFile.getParentFile().getAbsolutePath();
            currentFile = theFile.getAbsolutePath();

            try {
                treeView.openFile(filename, FileFormat.WRITE);

                try {
                    urlBar.remove(filename);
                }
                catch (Exception ex) {
                    log.trace("unable to remove {} from urlBar", filename);
                }

                // first entry is always the workdir
                urlBar.add(filename, 1);
                urlBar.select(1);
            }
            catch (Exception ex) {
                showError(ex.toString());
            }
        }
    }

    private void registerFileFormat()
    {
        String msg = "Register a new file format by \nKEY:FILE_FORMAT:FILE_EXTENSION\n"
                     + "where, KEY: the unique identifier for the file format"
                     + "\n           FILE_FORMAT: the full class name of the file format"
                     + "\n           FILE_EXTENSION: the file extension for the file format"
                     + "\n\nFor example, "
                     + "\n\t to add NetCDF, \"NetCDF:hdf.object.nc2.NC2File:nc\""
                     + "\n\t to add FITS, \"FITS:hdf.object.fits.FitsFile:fits\"\n\n";

        // TODO:Add custom HDFLarge icon to dialog
        InputDialog dialog = new InputDialog(mainWindow, "Register a file format", msg, SWT.ICON_INFORMATION);

        String str = dialog.open();

        if ((str == null) || (str.length() < 1))
            return;

        int idx1 = str.indexOf(':');
        int idx2 = str.lastIndexOf(':');

        if ((idx1 < 0) || (idx2 <= idx1)) {
            Tools.showError(mainWindow, "Register File Format",
                            "Failed to register " + str +
                                "\n\nMust in the form of KEY:FILE_FORMAT:FILE_EXTENSION");
            return;
        }

        String key       = str.substring(0, idx1);
        String className = str.substring(idx1 + 1, idx2);
        String extension = str.substring(idx2 + 1);

        // Check if the file format has been registered or the key is taken.
        String theKey            = null;
        String theClassName      = null;
        Enumeration<?> localEnum = FileFormat.getFileFormatKeys();
        while (localEnum.hasMoreElements()) {
            theKey = (String)localEnum.nextElement();
            if (theKey.endsWith(key)) {
                Tools.showError(mainWindow, "Register File Format", "Invalid key: " + key + " is taken.");
                return;
            }

            theClassName = FileFormat.getFileFormat(theKey).getClass().getName();
            if (theClassName.endsWith(className)) {
                Tools.showError(mainWindow, "Register File Format",
                                "The file format has already been registered: " + className);
                return;
            }
        }

        // Enables use of JHDF5 in JNLP (Web Start) applications, the system
        // class loader with reflection first.
        Class<?> theClass = null;
        try {
            theClass = Class.forName(className);
        }
        catch (Exception ex) {
            try {
                theClass = ViewProperties.loadExtClass().loadClass(className);
            }
            catch (Exception ex2) {
                theClass = null;
            }
        }

        if (theClass == null)
            return;

        try {
            Object theObject = theClass.newInstance();
            if (theObject instanceof FileFormat)
                FileFormat.addFileFormat(key, (FileFormat)theObject);
        }
        catch (Exception ex) {
            Tools.showError(mainWindow, "Register File Format", "Failed to register " + str + "\n\n" + ex);
            return;
        }

        if ((extension != null) && (extension.length() > 0)) {
            extension  = extension.trim();
            String ext = ViewProperties.getFileExtension();
            ext += ", " + extension;
            ViewProperties.setFileExtension(ext);
        }
    }

    private void unregisterFileFormat()
    {
        Enumeration<?> keys       = FileFormat.getFileFormatKeys();
        ArrayList<Object> keyList = new ArrayList<>();

        while (keys.hasMoreElements())
            keyList.add(keys.nextElement());

        String theKey = new UnregisterFileFormatDialog(mainWindow, SWT.NONE, keyList).open();

        if (theKey == null)
            return;

        FileFormat.removeFileFormat(theKey);
    }

    private class LibraryVersionDialog extends Dialog {
        private String message;

        public LibraryVersionDialog(Shell parent, String libType)
        {
            super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);

            if (libType.equals(FileFormat.FILE_TYPE_HDF4))
                setMessage("HDF " + HDF4_VERSION);
            else if (libType.equals(FileFormat.FILE_TYPE_HDF5))
                setMessage("HDF5 " + HDF5_VERSION);
        }

        public void setMessage(String message) { this.message = message; }

        public void open()
        {
            Shell dialog = new Shell(getParent(), getStyle());
            dialog.setFont(currentFont);
            dialog.setText("HDF Library Version");

            createContents(dialog);

            dialog.pack();

            Point computedSize = dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            dialog.setSize(computedSize.x + 50, computedSize.y + 50);

            // Center the window relative to the main HDFView window
            Point winCenter = new Point(mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
                                        mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));

            dialog.setLocation(winCenter.x - (dialog.getSize().x / 2),
                               winCenter.y - (dialog.getSize().y / 2));

            dialog.open();

            Display display = getParent().getDisplay();
            while (!dialog.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
        }

        private void createContents(final Shell shell)
        {
            shell.setLayout(new GridLayout(2, false));

            Image hdfImage = ViewProperties.getHDFViewIcon();

            Label imageLabel = new Label(shell, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(hdfImage);

            Label versionLabel = new Label(shell, SWT.CENTER);
            versionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            versionLabel.setFont(currentFont);
            versionLabel.setText(message);

            // Draw HDF Icon and Version string
            Composite buttonComposite = new Composite(shell, SWT.NONE);
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            RowLayout buttonLayout = new RowLayout();
            buttonLayout.center    = true;
            buttonLayout.justify   = true;
            buttonLayout.type      = SWT.HORIZONTAL;
            buttonComposite.setLayout(buttonLayout);

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &OK   ");
            shell.setDefaultButton(okButton);
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    shell.dispose();
                }
            });
        }
    }

    private class JavaVersionDialog extends Dialog {
        public JavaVersionDialog(Shell parent) { super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM); }

        public void open()
        {
            final Shell dialog = new Shell(getParent(), getStyle());
            dialog.setFont(currentFont);
            dialog.setText("HDFView Java Version");
            dialog.setLayout(new GridLayout(2, false));

            Image hdfImage = ViewProperties.getHDFViewIcon();

            Label imageLabel = new Label(dialog, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(hdfImage);

            Label versionLabel = new Label(dialog, SWT.CENTER);
            versionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            versionLabel.setFont(currentFont);
            versionLabel.setText(JAVA_VER_INFO);

            Composite buttonComposite = new Composite(dialog, SWT.NONE);
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            RowLayout buttonLayout = new RowLayout();
            buttonLayout.center    = true;
            buttonLayout.justify   = true;
            buttonLayout.type      = SWT.HORIZONTAL;
            buttonComposite.setLayout(buttonLayout);

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &OK   ");
            dialog.setDefaultButton(okButton);
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    dialog.dispose();
                }
            });

            dialog.pack();

            Point computedSize = dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            dialog.setSize(computedSize.x + 50, computedSize.y + 50);

            // Center the window relative to the main HDFView window
            Point winCenter = new Point(mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
                                        mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));

            dialog.setLocation(winCenter.x - (dialog.getSize().x / 2),
                               winCenter.y - (dialog.getSize().y / 2));

            dialog.open();

            Display openDisplay = getParent().getDisplay();
            while (!dialog.isDisposed()) {
                if (!openDisplay.readAndDispatch())
                    openDisplay.sleep();
            }
        }
    }

    private class SupportedFileFormatsDialog extends Dialog {
        public SupportedFileFormatsDialog(Shell parent)
        {
            super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
        }

        public void open()
        {
            final Shell dialog = new Shell(getParent(), getStyle());
            dialog.setFont(currentFont);
            dialog.setText("Supported File Formats");
            dialog.setLayout(new GridLayout(2, false));

            Image hdfImage = ViewProperties.getHDFViewIcon();

            Label imageLabel = new Label(dialog, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(hdfImage);

            Enumeration<?> formatKeys = FileFormat.getFileFormatKeys();

            StringBuilder formats = new StringBuilder("\nSupported File Formats: \n");
            while (formatKeys.hasMoreElements())
                formats.append("    ").append(formatKeys.nextElement()).append("\n");
            formats.append("\n");

            Label formatsLabel = new Label(dialog, SWT.LEFT);
            formatsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            formatsLabel.setFont(currentFont);
            formatsLabel.setText(formats.toString());

            Composite buttonComposite = new Composite(dialog, SWT.NONE);
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            RowLayout buttonLayout = new RowLayout();
            buttonLayout.center    = true;
            buttonLayout.justify   = true;
            buttonLayout.type      = SWT.HORIZONTAL;
            buttonComposite.setLayout(buttonLayout);

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &OK   ");
            dialog.setDefaultButton(okButton);
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    dialog.dispose();
                }
            });

            dialog.pack();

            Point computedSize = dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            dialog.setSize(computedSize.x + 50, computedSize.y + 50);

            // Center the window relative to the main HDFView window
            Point winCenter = new Point(mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
                                        mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));

            dialog.setLocation(winCenter.x - (dialog.getSize().x / 2),
                               winCenter.y - (dialog.getSize().y / 2));

            dialog.open();

            Display openDisplay = getParent().getDisplay();
            while (!dialog.isDisposed()) {
                if (!openDisplay.readAndDispatch())
                    openDisplay.sleep();
            }
        }
    }

    private class AboutDialog extends Dialog {
        public AboutDialog(Shell parent) { super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM); }

        public void open()
        {
            final Shell dialog = new Shell(getParent(), getStyle());
            dialog.setFont(currentFont);
            dialog.setText("About HDFView");
            dialog.setLayout(new GridLayout(2, false));

            Image hdfImage = ViewProperties.getHDFViewIcon();

            Label imageLabel = new Label(dialog, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(hdfImage);

            Label aboutLabel = new Label(dialog, SWT.LEFT);
            aboutLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            aboutLabel.setFont(currentFont);
            aboutLabel.setText(ABOUT_HDFVIEW);

            Composite buttonComposite = new Composite(dialog, SWT.NONE);
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            RowLayout buttonLayout = new RowLayout();
            buttonLayout.center    = true;
            buttonLayout.justify   = true;
            buttonLayout.type      = SWT.HORIZONTAL;
            buttonComposite.setLayout(buttonLayout);

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &OK   ");
            dialog.setDefaultButton(okButton);
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    dialog.dispose();
                }
            });

            dialog.pack();

            Point computedSize = dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            dialog.setSize(computedSize.x + 50, computedSize.y + 50);

            // Center the window relative to the main HDFView window
            Point winCenter = new Point(mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
                                        mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));

            dialog.setLocation(winCenter.x - (dialog.getSize().x / 2),
                               winCenter.y - (dialog.getSize().y / 2));

            dialog.open();

            Display openDisplay = getParent().getDisplay();
            while (!dialog.isDisposed()) {
                if (!openDisplay.readAndDispatch())
                    openDisplay.sleep();
            }
        }
    }

    private class UnregisterFileFormatDialog extends Dialog {
        private List<Object> keyList;
        private String formatChoice = null;

        public UnregisterFileFormatDialog(Shell parent, int style, List<Object> keyList)
        {
            super(parent, style);

            this.keyList = keyList;
        }

        public String open()
        {
            Shell parent      = getParent();
            final Shell shell = new Shell(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
            shell.setFont(currentFont);
            shell.setText("Unregister a file format");
            shell.setLayout(new GridLayout(2, false));

            Image hdfImage = ViewProperties.getHDFViewIcon();

            Label imageLabel = new Label(shell, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(hdfImage);

            final Combo formatChoiceCombo = new Combo(shell, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
            formatChoiceCombo.setFont(currentFont);
            formatChoiceCombo.setItems(keyList.toArray(new String[0]));
            formatChoiceCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
            formatChoiceCombo.select(0);
            formatChoiceCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    formatChoice = formatChoiceCombo.getItem(formatChoiceCombo.getSelectionIndex());
                }
            });

            Composite buttonComposite = new Composite(shell, SWT.NONE);
            buttonComposite.setLayout(new GridLayout(2, true));
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    shell.dispose();
                }
            });

            Button cancelButton = new Button(buttonComposite, SWT.PUSH);
            cancelButton.setFont(currentFont);
            cancelButton.setText(" &Cancel ");
            cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            cancelButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    shell.dispose();
                }
            });

            shell.pack();

            Point computedSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            shell.setSize(computedSize.x + 50, computedSize.y + 50);

            Rectangle parentBounds = parent.getBounds();
            Point shellSize        = shell.getSize();
            shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                              (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            shell.open();

            Display openDisplay = parent.getDisplay();
            while (!shell.isDisposed()) {
                if (!openDisplay.readAndDispatch())
                    openDisplay.sleep();
            }

            return formatChoice;
        }
    }

    /**
     * The starting point of this application.
     *
     * <pre>
     * Usage: java(w)
     *        -Dhdf.hdf5lib.H5.hdf5lib="your HDF5 library path"
     *        -Dhdf.hdflib.HDFLibrary.hdflib="your HDF4 library path"
     *        -root "the directory where the HDFView is installed"
     *        -start "the directory HDFView searches for files"
     *        -geometry or -g "the preferred window size as WIDTHxHEIGHT+XOFF+YOFF"
     *        -java.version "show the version of jave used to build the HDFView and exit"
     *        [filename] "the file to open"
     * </pre>
     *
     * @param args  the command line arguments
     */
    public static void main(String[] args)
    {
        if (display == null || display.isDisposed())
            display = new Display();

        String rootDir = System.getProperty("hdfview.root");
        if (rootDir == null)
            rootDir = System.getProperty("user.dir");
        String startDir = System.getProperty("user.dir");
        log.trace("main: rootDir = {}  startDir = {}", rootDir, startDir);

        File tmpFile           = null;
        Monitor primaryMonitor = display.getPrimaryMonitor();
        Point margin = new Point(primaryMonitor.getBounds().width, primaryMonitor.getBounds().height);

        int j = args.length;
        int W = margin.x / 2;
        int H = margin.y;
        int X = 0;
        int Y = 0;

        for (int i = 0; i < args.length; i++) {
            if ("-root".equalsIgnoreCase(args[i])) {
                j--;
                try {
                    j--;
                    tmpFile = new File(args[++i]);

                    if (tmpFile.isDirectory())
                        rootDir = tmpFile.getPath();
                    else if (tmpFile.isFile())
                        rootDir = tmpFile.getParent();
                }
                catch (Exception ex) {
                }
            }
            else if ("-start".equalsIgnoreCase(args[i])) {
                j--;
                try {
                    j--;
                    tmpFile = new File(args[++i]);

                    if (tmpFile.isDirectory())
                        startDir = tmpFile.getPath();
                    else if (tmpFile.isFile())
                        startDir = tmpFile.getParent();
                }
                catch (Exception ex) {
                }
            }
            else if ("-g".equalsIgnoreCase(args[i]) || "-geometry".equalsIgnoreCase(args[i])) {
                j--;
                // -geometry WIDTHxHEIGHT+XOFF+YOFF
                try {
                    String geom = args[++i];
                    j--;

                    int idx  = 0;
                    int idx2 = geom.lastIndexOf('-');
                    int idx3 = geom.lastIndexOf('+');

                    idx = Math.max(idx2, idx3);
                    if (idx > 0) {
                        Y = Integer.parseInt(geom.substring(idx + 1));

                        if (idx == idx2)
                            Y = -Y;

                        geom = geom.substring(0, idx);
                        idx2 = geom.lastIndexOf('-');
                        idx3 = geom.lastIndexOf('+');
                        idx  = Math.max(idx2, idx3);

                        if (idx > 0) {
                            X = Integer.parseInt(geom.substring(idx + 1));

                            if (idx == idx2)
                                X = -X;

                            geom = geom.substring(0, idx);
                        }
                    }

                    idx = geom.indexOf('x');

                    if (idx > 0) {
                        W = Integer.parseInt(geom.substring(0, idx));
                        H = Integer.parseInt(geom.substring(idx + 1));
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            else if ("-java.version".equalsIgnoreCase(args[i])) {
                /* Set icon to ViewProperties.getLargeHdfIcon() */
                Tools.showInformation(mainWindow, "Java Version", JAVA_VER_INFO);
                System.exit(0);
            }
        }

        ArrayList<File> fList = new ArrayList<>();

        if (j >= 0) {
            for (int i = args.length - j; i < args.length; i++) {
                tmpFile = new File(args[i]);
                if (!tmpFile.isAbsolute())
                    tmpFile = new File(rootDir, args[i]);
                log.trace("main: filelist - file = {} ", tmpFile.getAbsolutePath());
                log.trace("main: filelist - add file = {} exists={} isFile={} isDir={}", tmpFile,
                          tmpFile.exists(), tmpFile.isFile(), tmpFile.isDirectory());
                if (tmpFile.exists() && (tmpFile.isFile() || tmpFile.isDirectory())) {
                    log.trace("main: flist - add file = {}", tmpFile.getAbsolutePath());
                    fList.add(new File(tmpFile.getAbsolutePath()));
                }
            }
        }

        final ArrayList<File> theFileList = fList;
        final String the_rootDir          = rootDir;
        final String the_startDir         = startDir;
        final int the_X = X, the_Y = Y, the_W = W, the_H = H;

        display.syncExec(new Runnable() {
            @Override
            public void run()
            {
                HDFView app = new HDFView(the_rootDir, the_startDir);

                // TODO: Look for a better solution to native dialog problem
                app.setTestState(false);

                app.openMainWindow(theFileList, the_W, the_H, the_X, the_Y);
                app.runMainWindow();
            }
        });
    }
}
