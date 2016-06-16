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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Font;

import org.eclipse.swt.dnd.*;

import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.object.h5.H5Link;
import hdf.HDFVersions;

/**
 * HDFView is the main class of this HDF visual tool. It is used to layout the
 * graphical components of the hdfview. The major GUI components of the HDFView
 * include Menubar, Toolbar, TreeView, ContentView, and MessageArea.
 * <p>
 * The HDFView is designed in such a way that it does not have direct access to
 * the HDF library. All the HDF library access is done through HDF objects.
 * Therefore, the HDFView package depends on the object package but not the
 * library package. The source code of the view package (hdf.view) should
 * be compiled with the library package (hdf.hdflib and hdf.hdf5lib).
 *
 * @author Jordan T. Henderson
 * @version 2.4 //2015
 */
public class HDFView implements ViewManager {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HDFView.class);

    private static final Display       display = new Display();
    private static Shell               mainWindow;
    
    /* Determines whether HDFView is being executed for GUI testing */
    private boolean                    isTesting = false;

    /* The directory where HDFView is installed */
    private String                     rootDir;

    /* The current working directory */
    private String                     currentDir;

    /* The current working file */
    private String                     currentFile = null;

    /* The view properties */
    private ViewProperties             props;

    /* A list of tree view implementations. */
    private static List<String>        treeViews;

    /* A list of image view implementations. */
    private static List<String>        imageViews;

    /* A list of tree table implementations. */
    private static List<?>             tableViews;

    /* A list of Text view implementations. */
    private static List<String>        textViews;

    /* A list of metadata view implementations. */
    private static List<?>             metaDataViews;

    /* A list of palette view implementations. */
    private static List<?>             paletteViews;

    /* A list of help view implementations. */
    private static List<?>             helpViews;

    /* The list of GUI components related to HDF4 */
    private final List<MenuItem>       h4GUIs = new Vector<MenuItem>();

    /* The list of GUI components related to HDF5 */
    private final List<MenuItem>       h5GUIs = new Vector<MenuItem>();

    /* The list of GUI components related to editing */
    //private final List<?>            editGUIs;

    /* GUI component: the TreeView */
    private TreeView                   treeView;

    private static final String        HDF4_VERSION = HDFVersions.HDF4_VERSION;
    private static final String        HDF5_VERSION = HDFVersions.HDF5_VERSION;
    private static final String        HDFVIEW_VERSION = HDFVersions.HDFVIEW_VERSION;
    private static final String        HDFVIEW_USERSGUIDE_URL = "http://www.hdfgroup.org/products/java/hdfview/UsersGuide/index.html";
    private static final String        JAVA_COMPILER = "jdk 1.7";
    private static final String        JAVA_VER_INFO = "Compiled at " + JAVA_COMPILER + "\nRunning at " + System.getProperty("java.version");

    private static final String        aboutHDFView = "HDF Viewer, " + "Version " + ViewProperties.VERSION + "\n"
    + "For " + System.getProperty("os.name") + "\n\n"
    + "Copyright " + '\u00a9' + " 2006-2015 The HDF Group.\n"
    + "All rights reserved.";

    /* String buffer holding the status message */
    private StringBuffer               message;

    /* GUI component: The toolbar for open, close, help and hdf4 and hdf5 library information */
    private ToolBar                    toolBar;

    /* GUI component: The text area for showing status messages */
    private Text                       status;

    /* GUI component: The area for quick attribute view */
    private Composite                  attributeArea;

    /* GUI component: To add and display URLs */
    private Combo                      url_bar;
    
    private Button                     recentFilesButton;
    private Button                     clearTextButton;

    /* GUI component: A list of current data windows */
    private Menu                       windowMenu;

    /* GUI component: File menu on the menubar */
    //private final Menu               fileMenu;
    
    /* The font to be used for display text on all Controls */
    private Font                       currentFont;

    private UserOptionsDialog          userOptionDialog;

//    private Constructor<?>             ctrSrbFileDialog     = null;
//
//    private Dialog                     srbFileDialog         = null;

    /**
     * Constructs HDFView with a given root directory, where the HDFView is
     * installed, and opens the given files in the viewer.
     *
     * @param root
     *            the directory where the HDFView is installed.
     */
    public HDFView(String root) {
        log.debug("Root is {}", root);

        rootDir = root;
        //userOptionsDialog = null;
        //ctrSrbFileDialog = null;

        //editGUIs = new Vector<Object>();

        ViewProperties.loadIcons();
        ViewProperties.loadExtClass();

        props = new ViewProperties(rootDir);
        try {
            props.load();
        }
        catch (Exception ex) {
            log.debug("Failed to load View Properties from {}", rootDir);
        }

        currentDir = ViewProperties.getWorkDir();
        if (currentDir == null) currentDir = System.getProperty("user.home");

        log.info("Current directory is {}", currentDir);
        
        try {
            currentFont = new Font(
                    display,
                    ViewProperties.getFontType(),
                    ViewProperties.getFontSize(),
                    SWT.NORMAL);
        }
        catch (Exception ex) {
            currentFont = null;
        }

        treeViews = ViewProperties.getTreeViewList();
        metaDataViews = ViewProperties.getMetaDataViewList();
        textViews = ViewProperties.getTextViewList();
        tableViews = ViewProperties.getTableViewList();
        imageViews = ViewProperties.getImageViewList();
        paletteViews = ViewProperties.getPaletteViewList();
        helpViews = ViewProperties.getHelpViewList();

        int n = treeViews.size();
        Class<?> theClass = null;
        for (int i = 0; i < n; i++) {
            // Use the first available treeview
            String className = treeViews.get(i);

            // Enables use of JHDF5 in JNLP (Web Start) applications, the system
            // class loader with reflection first.
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

            if (theClass != null) break;
        }

        if (theClass != null) {
            try {
                @SuppressWarnings("rawtypes")
                Class[] paramClass = { Class.forName("hdf.view.ViewManager") };
                Constructor<?> constructor = theClass.getConstructor(paramClass);
                Object[] paramObj = { this };
                treeView = (TreeView) constructor.newInstance(paramObj);
            }
            catch (Exception ex) {
                treeView = null;
            }
        }
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
    public Shell openMainWindow(List<File> flist, int width, int height, int x, int y) {
        log.debug("openMainWindow enter");

        // Initialize all GUI components
        mainWindow = createMainWindow();

        try {
            Font font = null;
            String fType = ViewProperties.getFontType();
            int fSize = ViewProperties.getFontSize();

            try {
                font = new Font(display, fType, fSize, SWT.NORMAL);
            }
            catch (Exception ex) {
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

        int nfiles = flist.size();
        File theFile = null;
        for (int i = 0; i < nfiles; i++) {
            theFile = flist.get(i);

            if (theFile.isFile()) {
                currentDir = theFile.getParentFile().getAbsolutePath();
                currentFile = theFile.getAbsolutePath();

                try {
                    treeView.openFile(currentFile, FileFormat.WRITE);

                    try {
                        url_bar.remove(currentFile);
                    }
                    catch (Exception ex) {}

                    url_bar.add(currentFile, 0);
                    url_bar.select(0);
                }
                catch (Exception ex) {
                    showStatus(ex.toString());
                }
            }
            else {
                currentDir = theFile.getAbsolutePath();
            }

            log.info("CurrentDir is {}", currentDir);
        }

        if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) == null)
            setEnabled(h4GUIs, false);

        if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) == null)
            setEnabled(h5GUIs, false);

        // Set size of main window
        // float inset = 0.17f; // for UG only.
        float inset = 0.04f;
        Point winDim = new Point(width, height);

        // If given height and width are too small, adjust accordingly
        if (height <= 300) {
            winDim.y = (int) ((1 - 2 * inset) * mainWindow.getSize().y);
        }

        if (width <= 300) {
            winDim.x = (int) (0.9 * (double) mainWindow.getSize().y);
        }

        // TEST
        //if (treeView.getClass().getName().startsWith("ext.erdc")) {
        //    topSplitPane.setDividerLocation(500);
        //    winDim.x = (int) (0.9 * mainWindow.getSize().x);
        //    winDim.y = (int) (winDim.x * 0.618);
        //}

        mainWindow.setLocation(x, y);
        mainWindow.setMinimumSize(winDim.x / 2, winDim.y / 2);
        mainWindow.setSize(winDim.x + 200, winDim.y);

        // Display the window
        mainWindow.open();
        log.debug("openMainWindow exit");
        return mainWindow;
    }

    public void runMainWindow() {
        log.debug("runMainWindow enter");

        while(!mainWindow.isDisposed()) {
            // ===================================================
            // Wrap each event dispatch in an exception handler
            // so that if any event causes an exception it does
            // not break the main UI loop
            // ===================================================
            try
            {
               if (!display.readAndDispatch())
               {
                  display.sleep();
               }
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
        }

        //display.dispose();
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
    private Shell createMainWindow() {
        // Create a new display window
        final Shell shell = new Shell(display);
        shell.setImage(ViewProperties.getHdfIcon());
        shell.setFont(currentFont);
        shell.setText("HDFView " + HDFVIEW_VERSION);
        shell.setLayout(new GridLayout(3, false));
        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                ViewProperties.setRecentFiles(new Vector<String>(Arrays.asList(url_bar.getItems())));
                
                try {
                    props.save();
                } catch (Exception ex) {}
                
                closeAllWindows();

                // Close all open files
                try {
                    List<FileFormat> filelist = treeView.getCurrentFiles();

                    if((filelist != null) && (filelist.size() > 0)) {
                        Object[] files = filelist.toArray();

                        for (int i = 0; i < files.length; i++) {
                            try {
                                treeView.closeFile((FileFormat) files[i]);
                            }
                            catch (Throwable ex) {
                                continue;
                            }
                        }
                    }
                }
                catch (Exception ex) {}
            }
        });

        createMenuBar(shell);
        createToolbar(shell);
        createUrlToolbar(shell);
        createContentArea(shell);

        log.info("Main Window created");

        return shell;
    }

    private void createMenuBar(final Shell shell) {
        Menu menu = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menu);

        MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
        menuItem.setText("&File");

        Menu fileMenu = new Menu(menuItem);
        menuItem.setMenu(fileMenu);

        MenuItem item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("&Open \tCtrl-O");
        item.setAccelerator(SWT.MOD1 + 'O');
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                openLocalFile(null, FileFormat.WRITE);
            }
        });

        if(!ViewProperties.isReadOnly()) {
            item = new MenuItem(fileMenu, SWT.PUSH);
            item.setText("Open &Read-Only");
            item.setAccelerator(SWT.MOD1 + 'R');
            item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openLocalFile(null, FileFormat.READ);
                }
            });
        }

        // boolean isSrbSupported = true;
        // try {
        // Class.forName("hdf.srb.H5SRB");
        // Class.forName("hdf.srb.SRBFileDialog");
        // } catch (Throwable ex) {isSrbSupported = false;}
        //
        // if (isSrbSupported) {
        // item = new JMenuItem( "Open from iRODS");
        // item.setMnemonic(KeyEvent.VK_S);
        // item.addActionListener(this);
        // item.setActionCommand("Open from irods");
        // fileMenu.add(item);
        // }

        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem fileNewMenu = new MenuItem(fileMenu, SWT.CASCADE);
        fileNewMenu.setText("New");

        Menu newMenu = new Menu(fileNewMenu);
        fileNewMenu.setMenu(newMenu);

        item = new MenuItem(newMenu, SWT.PUSH);
        item.setText("HDF&4");
        h4GUIs.add(item);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (currentDir != null) {
                    currentDir += File.separator;
                }
                else {
                    currentDir = "";
                }
                
                String filename = null;

                if (!isTesting) {
                    FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
                    fChooser.setFileName(Tools.checkNewFile(currentDir, ".hdf").getName());

                    DefaultFileFilter filter = DefaultFileFilter.getFileFilterHDF4();
                    fChooser.setFilterExtensions(new String[] {filter.getExtensions()});
                    fChooser.setFilterNames(new String[] {filter.getDescription()});
                    fChooser.setFilterIndex(0);

                    filename = fChooser.open();
                } else {
                    // Prepend test file directory to filename
                    filename = currentDir.concat(new InputDialog(mainWindow, "Enter a file name", "").open());
                }

                if(filename == null) return;

                try {
                    FileFormat theFile = Tools.createNewFile(filename, currentDir,
                            FileFormat.FILE_TYPE_HDF4, getTreeView().getCurrentFiles());
                    
                    if (theFile == null) return;

                    currentDir = theFile.getParent();
                }
                catch (Exception ex) {
                    Tools.showError(mainWindow, ex.getMessage(), mainWindow.getText());
                    return;
                }

                try {
                    treeView.openFile(filename, FileFormat.WRITE);
                    currentFile = filename;

                    try {
                        url_bar.remove(filename);
                    }
                    catch (Exception ex) {}

                    url_bar.add(filename, 0);
                    url_bar.select(0);
                }
                catch (Exception ex) {
                    display.beep();
                    Tools.showError(mainWindow, ex.getMessage() + "\n" + filename, null);
                }
            }
        });

        item = new MenuItem(newMenu, SWT.PUSH);
        item.setText("HDF&5");
        h5GUIs.add(item);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (currentDir != null) {
                    currentDir += File.separator;
                }
                else {
                    currentDir = "";
                }
                
                String filename = null;

                if (!isTesting) {
                    FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
                    fChooser.setFileName(Tools.checkNewFile(currentDir, ".h5").getName());

                    DefaultFileFilter filter = DefaultFileFilter.getFileFilterHDF5();
                    fChooser.setFilterExtensions(new String[] {filter.getExtensions()});
                    fChooser.setFilterNames(new String[] {filter.getDescription()});
                    fChooser.setFilterIndex(0);

                    filename = fChooser.open();
                } else {
                    // Prepend test file directory to filename
                    filename = currentDir.concat(new InputDialog(mainWindow, "Enter a file name", "").open());
                }

                if(filename == null) return;

                try {
                    FileFormat theFile = Tools.createNewFile(filename, currentDir,
                            FileFormat.FILE_TYPE_HDF5, getTreeView().getCurrentFiles());
                    
                    if (theFile == null) return;

                    currentDir = theFile.getParent();
                }
                catch (Exception ex) {
                    Tools.showError(mainWindow, ex.getMessage(), mainWindow.getText());
                    return;
                }

                try {
                    treeView.openFile(filename, FileFormat.WRITE);
                    currentFile = filename;

                    try {
                        url_bar.remove(filename);
                    }
                    catch (Exception ex) {}

                    url_bar.add(filename, 0);
                    url_bar.select(0);
                }
                catch (Exception ex) {
                    display.beep();
                    Tools.showError(mainWindow, ex.getMessage() + "\n" + filename, null);
                }
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("&Close");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                closeFile(treeView.getSelectedFile());
            }
        });

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("Close &All");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                closeAllWindows();

                List<FileFormat> files = treeView.getCurrentFiles();
                while (!files.isEmpty()) {
                    try {
                        treeView.closeFile(files.get(0));
                    }
                    catch (Exception ex) {}
                }

                currentFile = null;
                
                for (Control control : attributeArea.getChildren()) control.dispose();
                
                url_bar.setText("");
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("&Save");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (treeView.getCurrentFiles().size() <= 0) {
                    Tools.showError(mainWindow, "No files currently open.", shell.getText());
                    return;
                }

                if (treeView.getSelectedFile() == null) {
                    Tools.showError(mainWindow, "No files currently selected.", shell.getText());
                    return;
                }

                // Save what has been changed in memory into file
                writeDataToFile(treeView.getSelectedFile());
            }
        });

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("S&ave As");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (treeView.getCurrentFiles().size() <= 0) {
                    Tools.showError(mainWindow, "No files currently open.", shell.getText());
                    return;
                }

                if (treeView.getSelectedFile() == null) {
                    Tools.showError(mainWindow, "No files currently selected.", shell.getText());
                    return;
                }

                try {
                    treeView.saveFile(treeView.getSelectedFile());
                }
                catch (Exception ex) {
                    display.beep();
                    Tools.showError(mainWindow, ex.getMessage(), shell.getText());
                }
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("E&xit \tCtrl-Q");
        item.setAccelerator(SWT.MOD1 + 'Q');
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
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
            public void widgetSelected(SelectionEvent e) {
                cascadeWindows();
            }
        });

        item = new MenuItem(windowMenu, SWT.PUSH);
        item.setText("&Tile");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                tileWindows();
            }
        });

        new MenuItem(windowMenu, SWT.SEPARATOR);

        item = new MenuItem(windowMenu, SWT.PUSH);
        item.setText("Close &All");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
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
            public void widgetSelected(SelectionEvent e) {
                convertFile(Tools.FILE_TYPE_IMAGE, FileFormat.FILE_TYPE_HDF4);
            }
        });
        h4GUIs.add(item);

        item = new MenuItem(convertMenu, SWT.PUSH);
        item.setText("HDF5");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                convertFile(Tools.FILE_TYPE_IMAGE, FileFormat.FILE_TYPE_HDF5);
            }
        });
        h5GUIs.add(item);

        new MenuItem(toolsMenu, SWT.SEPARATOR);

        item = new MenuItem(toolsMenu, SWT.PUSH);
        item.setText("User &Options");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                userOptionDialog = new UserOptionsDialog(shell, rootDir);

                userOptionDialog.open();

                if (userOptionDialog.isWorkDirChanged())
                    currentDir = ViewProperties.getWorkDir();

                if (userOptionDialog.isFontChanged()) {
                    Font font = null;

                    try {
                        font = new Font(display, ViewProperties.getFontType(), ViewProperties.getFontSize(), SWT.NORMAL);
                    }
                    catch (Exception ex) {
                        font = null;
                    }

                    updateFont(font);
                }
            }
        });

        new MenuItem(toolsMenu, SWT.SEPARATOR);

        item = new MenuItem(toolsMenu, SWT.PUSH);
        item.setText("&Register File Format");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                registerFileFormat();
            }
        });

        item = new MenuItem(toolsMenu, SWT.PUSH);
        item.setText("&Unregister File Format");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
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
            public void widgetSelected(SelectionEvent e) {
                org.eclipse.swt.program.Program.launch(HDFVIEW_USERSGUIDE_URL);
            }
        });

        if ((helpViews != null) && (helpViews.size() > 0)) {
            int n = helpViews.size();
            for (int i = 0; i < n; i++) {
                HelpView theView = (HelpView) helpViews.get(i);
                item = new MenuItem(helpMenu, SWT.PUSH);
                item.setText(theView.getLabel());
                //item.setActionCommand(theView.getActionCommand());
            }
        }

        new MenuItem(helpMenu, SWT.SEPARATOR);

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("HDF&4 Library Version");
        h4GUIs.add(item);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF4).open();
            }
        });

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("HDF&5 Library Version");
        h5GUIs.add(item);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF5).open();
            }
        });

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("&Java Version");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                new JavaVersionDialog(mainWindow).open();
            }
        });

        new MenuItem(helpMenu, SWT.SEPARATOR);

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("Supported Fi&le Formats");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                new SupportedFileFormatsDialog(mainWindow).open();
            }
        });

        new MenuItem(helpMenu, SWT.SEPARATOR);

        item = new MenuItem(helpMenu, SWT.PUSH);
        item.setText("&About...");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                new AboutDialog(mainWindow).open();
            }
        });

        log.info("Menubar created");
    }

    private void createToolbar(final Shell shell) {
        toolBar = new ToolBar(shell, SWT.HORIZONTAL | SWT.RIGHT);
        toolBar.setFont(Display.getCurrent().getSystemFont());
        toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

        ToolItem openItem = new ToolItem(toolBar, SWT.PUSH);
        openItem.setToolTipText("Open");
        openItem.setImage(ViewProperties.getFileopenIcon());
        openItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                openLocalFile(null, FileFormat.WRITE);
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);

        ToolItem closeItem = new ToolItem(toolBar, SWT.PUSH);
        closeItem.setImage(ViewProperties.getFilecloseIcon());
        closeItem.setToolTipText("Close");
        closeItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                closeFile(treeView.getSelectedFile());
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR).setWidth(20);

        ToolItem helpItem = new ToolItem(toolBar, SWT.PUSH);
        helpItem.setImage(ViewProperties.getHelpIcon());
        helpItem.setToolTipText("Help");
        helpItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String ugPath = ViewProperties.getUsersGuide();

                if(ugPath == null || !ugPath.startsWith("http://")) {
                    String sep = File.separator;
                    File tmpFile = new File(ugPath);

                    if(!(tmpFile.exists())) {
                        ugPath = rootDir + sep + "UsersGuide" + sep + "index.html";
                        tmpFile = new File(ugPath);

                        if(!(tmpFile.exists())) {
                            ugPath = HDFVIEW_USERSGUIDE_URL;
                        }

                        ViewProperties.setUsersGuide(ugPath);
                    }
                }

                try {
                    org.eclipse.swt.program.Program.launch(ugPath);
                }
                catch (Exception ex) {
                    Tools.showError(shell, ex.getMessage(), shell.getText());
                }
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);

        ToolItem hdf4Item = new ToolItem(toolBar, SWT.PUSH);
        hdf4Item.setImage(ViewProperties.getH4Icon());
        hdf4Item.setToolTipText("HDF4 Library Version");
        hdf4Item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF4).open();
            }
        });

        if(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) == null) {
            hdf4Item.setEnabled(false);
        }

        new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);

        ToolItem hdf5Item = new ToolItem(toolBar, SWT.PUSH);
        hdf5Item.setImage(ViewProperties.getH5Icon());
        hdf5Item.setToolTipText("HDF5 Library Version");
        hdf5Item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF5).open();
            }
        });

        if(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) == null) {
            hdf5Item.setEnabled(false);
        }

        // Make the toolbar as wide as the window and as
        // tall as the buttons
        toolBar.setSize(shell.getClientArea().width, openItem.getBounds().height);
        toolBar.setLocation(0, 0);

        log.info("Toolbar created");
    }

    private void createUrlToolbar(final Shell shell) {
        // Recent Files button
        recentFilesButton = new Button(shell, SWT.PUSH);
        recentFilesButton.setFont(currentFont);
        recentFilesButton.setText("Recent Files");
        recentFilesButton.setToolTipText("List of recent files");
        recentFilesButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        recentFilesButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                url_bar.setListVisible(true);
            }
        });

        // Recent files combo box
        url_bar = new Combo(shell, SWT.BORDER | SWT.SINGLE);
        url_bar.setFont(currentFont);
        url_bar.setItems(ViewProperties.getMRF().toArray(new String[0]));
        url_bar.setVisibleItemCount(ViewProperties.MAX_RECENT_FILES);
        url_bar.deselectAll();
        url_bar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        url_bar.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if(e.keyCode == SWT.CR) {
                    String filename = url_bar.getText();
                    if (filename == null || filename.length() < 1 || filename.equals(currentFile)) {
                        return;
                    }

                    if(!(filename.startsWith("http://") || filename.startsWith("https://") || filename.startsWith("ftp://"))) {
                        openLocalFile(filename, FileFormat.WRITE);
                    }
                    else {
                        String remoteFile = openRemoteFile(filename);
                        
                        if (remoteFile != null) openLocalFile(remoteFile, FileFormat.WRITE);
                    }
                }
            }
        });
        url_bar.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String filename = url_bar.getText();

                if(filename.length() <= 0) return;

                if(!(filename.startsWith("http://") || filename.startsWith("ftp://"))) {
                    openLocalFile(filename, FileFormat.WRITE);
                }
                else {
                    openRemoteFile(filename);
                }
            }
        });

        clearTextButton = new Button(shell, SWT.PUSH);
        clearTextButton.setToolTipText("Clear current selection");
        clearTextButton.setFont(currentFont);
        clearTextButton.setText("Clear Text");
        clearTextButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        clearTextButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                url_bar.setText("");
                url_bar.deselectAll();
            }
        });

        log.info("URL Toolbar created");
    }

    private void createContentArea(final Shell shell) {
        SashForm content = new SashForm(shell, SWT.VERTICAL);
        content.setSashWidth(10);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        // Add Data content area and Status Area to main window
        Composite container = new Composite(content, SWT.NONE);
        container.setLayout(new FillLayout());

        Composite statusArea = new Composite(content, SWT.NONE);
        statusArea.setLayout(new FillLayout(SWT.HORIZONTAL));

        SashForm contentArea = new SashForm(container, SWT.HORIZONTAL);
        contentArea.setSashWidth(10);

        // Add TreeView and DataView to content area pane
        ScrolledComposite treeArea = new ScrolledComposite(contentArea, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        treeArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        treeArea.setExpandHorizontal(true);
        treeArea.setExpandVertical(true);

        attributeArea = new Composite(contentArea, SWT.BORDER);
        attributeArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        attributeArea.setLayout(new GridLayout(1, true));

        // Could not load user's treeview, use default treeview.
        if (treeView == null) treeView = new DefaultTreeView(this, treeArea);
        treeArea.setContent(treeView.getTree());
        
        // Add drag and drop support for opening files
        DropTarget target = new DropTarget(treeArea, DND.DROP_COPY);
        final FileTransfer fileTransfer = FileTransfer.getInstance();
        target.setTransfer(new Transfer[] { fileTransfer });
        target.addDropListener(new DropTargetListener() {
            public void dragEnter(DropTargetEvent e) {
                e.detail = DND.DROP_COPY;
            }
            public void dragOver(DropTargetEvent e) {}
            public void dragOperationChanged(DropTargetEvent e) { }
            public void dragLeave(DropTargetEvent e) {}
            public void dropAccept(DropTargetEvent e) {}
            public void drop(DropTargetEvent e) {
                if (fileTransfer.isSupportedType(e.currentDataType)) {
                    String[] files = (String[]) e.data;
                    for (int i = 0; i < files.length; i++) {
                        openLocalFile(files[i], FileFormat.WRITE);
                    }
                }
            }
        });

        // Create status area for displaying messages and metadata
        status = new Text(statusArea, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
        status.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        status.setEditable(false);
        status.setFont(currentFont);
        
        message = new StringBuffer();
        showStatus("HDFView root - " + rootDir);
        showStatus("User property file - " + ViewProperties.getPropertyFile());

        content.setWeights(new int[] {9, 1});
        contentArea.setWeights(new int[] {1, 3});

        log.info("Content Area created");
    }

    /**
     * @return a list of treeview implementations.
     */
    public static final List<String> getListOfTreeViews() {
        return treeViews;
    }

    /**
     * @return a list of imageview implementations.
     */
    public static final List<String> getListOfImageViews() {
        return imageViews;
    }

    /**
     * @return a list of tableview implementations.
     */
    public static final List<?> getListOfTableViews() {
        return tableViews;
    }

    /**
     * @return a list of textview implementations.
     */
    public static final List<?> getListOfTextViews() {
        return textViews;
    }

    /**
     * @return a list of metaDataview implementations.
     */
    public static final List<?> getListOfMetaDataViews() {
        return metaDataViews;
    }

    /**
     * @return a list of paletteview implementations.
     */
    public static final List<?> getListOfPaletteViews() {
        return paletteViews;
    }

    public TreeView getTreeView() {
        return treeView;
    }

    public Combo getUrlBar() {
        return url_bar;
    }

    /**
     * Display feedback message.
     *
     * @param msg
     *            the message to display.
     */
    public void showStatus(String msg) {
        message.append(msg);
        message.append("\n");
        status.setText(message.toString());
    }

    public void showMetaData(HObject obj) {
        if (obj == null || currentFile == null) return;
        
        log.trace("showMetaData: start");
        
        // Get the metadata information before adding GUI components */
        try {
            obj.getMetadata();
        }
        catch (Exception ex) {
            log.debug("Error retrieving metadata of object " + obj.getName() + ":", ex);
        }
        
        for (Control control : attributeArea.getChildren()) control.dispose();
        
        boolean isRoot = ((obj instanceof Group) && ((Group) obj).isRoot());
        boolean isH4 = obj.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        boolean isH5 = obj.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        FileFormat theFile = obj.getFileFormat();
        String typeStr = "Unknown";
        String fileInfo = "";
        
        org.eclipse.swt.widgets.Group generalInfoGroup = new org.eclipse.swt.widgets.Group(attributeArea, SWT.NONE);
        generalInfoGroup.setFont(currentFont);
        generalInfoGroup.setText("General Object Info");
        generalInfoGroup.setLayout(new GridLayout(2, false));
        generalInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        Label label;
        
        if(isRoot) {
            log.trace("showMetaData: isRoot");
            long size = 0;
            try {
                size = (new File(obj.getFile())).length();
            }
            catch (Exception ex) {
                size = -1;
            }
            size /= 1024;
            
            int groupCount = 0, datasetCount = 0;
            
            HObject root = theFile.getRootObject();
            HObject theObj = null;
            Iterator<HObject> it = ((Group) root).depthFirstMemberList().iterator();
            
            while(it.hasNext()) {
                theObj = it.next();
                
                if(theObj instanceof Group) {
                    groupCount++;
                } else {
                    datasetCount++;
                }
            }
            
            fileInfo = "size=" + size + "K,  groups=" + groupCount + ",  datasets=" + datasetCount;
            
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("File Name: ");
            
            label = new Label(generalInfoGroup, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(obj.getName());
            
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("File Path: ");
            
            label = new Label(generalInfoGroup, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText((new File(obj.getFile())).getParent());
            
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("File Type: ");
            
            if (isH5) {
                typeStr = "HDF5,  " + fileInfo;
            }
            else if (isH4) {
                typeStr = "HDF4,  " + fileInfo;
            }
            else {
                typeStr = fileInfo;
            }
            
            label = new Label(generalInfoGroup, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(typeStr);
            
            if (isH5) {
                int[] libver = null;
                
                try {
                    libver = obj.getFileFormat().getLibBounds();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                if (((libver[0] == 0) || (libver[0] == 1)) && (libver[1] == 1)) {
                    label = new Label(generalInfoGroup, SWT.LEFT);
                    label.setFont(currentFont);
                    label.setText("Library version: ");
                }
                
                String libversion = null;
                if ((libver[0] == 0) && (libver[1] == 1))
                    libversion = "Earliest and Latest";
                else if ((libver[0] == 1) && (libver[1] == 1)) libversion = "Latest and Latest";
                else {
                    libversion = "";
                }
                
                label = new Label(generalInfoGroup, SWT.RIGHT);
                label.setFont(currentFont);
                label.setText(libversion);
            }
        }
        else {
            log.trace("showMetaData: is not root");
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Name: ");
            
            label = new Label(generalInfoGroup, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(obj.getName());
            
            if(isH5) {
                if (obj.getLinkTargetObjName() != null) {
                    final HObject theObj = obj;
                    
                    label = new Label(generalInfoGroup, SWT.LEFT);
                    label.setFont(currentFont);
                    label.setText("Link To Target: ");
                    
                    final Text linkTarget = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
                    linkTarget.setFont(currentFont);
                    linkTarget.setText(obj.getLinkTargetObjName());
                    linkTarget.addTraverseListener(new TraverseListener() {
                        public void keyTraversed(TraverseEvent e) {
                            if (e.detail == SWT.TRAVERSE_RETURN) {
                                Group pgroup = null;
                                try {
                                    pgroup = (Group) theObj.getFileFormat().get(theObj.getPath());
                                }
                                catch (Exception ex) {
                                    log.debug("parent group:", ex);
                                }
                                if (pgroup == null) {
                                    Tools.showError(mainWindow, "Parent group is null.", mainWindow.getText());
                                    return;
                                }

                                String target_name = linkTarget.getText();
                                if (target_name != null) target_name = target_name.trim();

                                int linkType = Group.LINK_TYPE_SOFT;
                                if (theObj.getLinkTargetObjName().contains(FileFormat.FILE_OBJ_SEP))
                                    linkType = Group.LINK_TYPE_EXTERNAL;
                                else if (target_name.equals("/")) { // do not allow to link to the root
                                    Tools.showError(mainWindow, "Link to root not allowed.", mainWindow.getText());
                                    return;
                                }

                                // no change
                                if (target_name.equals(theObj.getLinkTargetObjName())) return;

                                // invalid name
                                if (target_name == null || target_name.length() < 1) return;

                                try {
                                    theObj.getFileFormat().createLink(pgroup, theObj.getName(), target_name, linkType);
                                    theObj.setLinkTargetObjName(target_name);
                                }
                                catch (Exception ex) {
                                    Tools.showError(mainWindow, ex.getMessage(), mainWindow.getText());
                                    return;
                                }
                                
                                MessageBox success = new MessageBox(mainWindow, SWT.ICON_INFORMATION | SWT.OK);
                                success.setText(mainWindow.getText());
                                success.setMessage("Link target changed.");
                                success.open();
                            }
                        }
                    });
                }
            }
            
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Path: ");
            
            label = new Label(generalInfoGroup, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(obj.getPath());
            
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Type: ");
            
            if(isH5) {
                if (obj instanceof Group) {
                    typeStr = "HDF5 Group";
                }
                else if (obj instanceof ScalarDS) {
                    typeStr = "HDF5 Scalar Dataset";
                }
                else if (obj instanceof CompoundDS) {
                    typeStr = "HDF5 Compound Dataset";
                }
                else if (obj instanceof Datatype) {
                    typeStr = "HDF5 Named Datatype";
                }
            } else if(isH4) {
                if (obj instanceof Group) {
                    typeStr = "HDF4 Group";
                }
                else if (obj instanceof ScalarDS) {
                    ScalarDS ds = (ScalarDS) obj;
                    if (ds.isImage()) {
                        typeStr = "HDF4 Raster Image";
                    }
                    else {
                        typeStr = "HDF4 SDS";
                    }
                }
                else if (obj instanceof CompoundDS) {
                    typeStr = "HDF4 Vdata";
                }
            } else {
                if (obj instanceof Group) {
                    typeStr = "Group";
                }
                else if (obj instanceof ScalarDS) {
                    typeStr = "Scalar Dataset";
                }
                else if (obj instanceof CompoundDS) {
                    typeStr = "Compound Dataset";
                }
            }
            
            label = new Label(generalInfoGroup, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(typeStr);
        }
        
        // bug #926 to remove the OID, put it back on Nov. 20, 2008, --PC
        if (isH4) {
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Tag, Ref:        ");
        }
        else {
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Object Ref:       ");
        }
        
        // bug #926 to remove the OID, put it back on Nov. 20, 2008, --PC
        String oidStr = null;
        long[] OID = obj.getOID();
        if (OID != null) {
            oidStr = String.valueOf(OID[0]);
            for (int i = 1; i < OID.length; i++) {
                oidStr += ", " + OID[i];
            }
        }
        
        label = new Label(generalInfoGroup, SWT.RIGHT);
        label.setFont(currentFont);
        label.setText(oidStr);

        log.trace("showMetaData: object extra info");
        // Add any extra information depending on object type
        if (obj instanceof Group) {
            log.trace("showMetaData: group object extra info");
            Group g = (Group) obj;
            List<?> mlist = g.getMemberList();
            int n = mlist.size();
            
            if (mlist != null && n > 0) {
                org.eclipse.swt.widgets.Group groupInfoGroup = new org.eclipse.swt.widgets.Group(attributeArea, SWT.NONE);
                groupInfoGroup.setFont(currentFont);
                groupInfoGroup.setText("Group Members");
                groupInfoGroup.setLayout(new GridLayout(1, true));
                groupInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
                
                if (g.getNumberOfMembersInFile() < ViewProperties.getMaxMembers()) {
                    label = new Label(groupInfoGroup, SWT.RIGHT);
                    label.setFont(currentFont);
                    label.setText("Number of members: " + n);
                }
                else {
                    label = new Label(groupInfoGroup, SWT.RIGHT);
                    label.setFont(currentFont);
                    label.setText("Number of members: " + n + " (in memory),"
                            + "" + g.getNumberOfMembersInFile() + " (in file)");
                }
                
                String rowData[][] = new String[n][2];
                for (int i = 0; i < n; i++) {
                    HObject theObj = (HObject) mlist.get(i);
                    rowData[i][0] = theObj.getName();
                    if (theObj instanceof Group) {
                        rowData[i][1] = "Group";
                    }
                    else if (theObj instanceof Dataset) {
                        rowData[i][1] = "Dataset";
                    }
                    else if (theObj instanceof Datatype) {
                        rowData[i][1] = "Datatype";
                    }
                    else if (theObj instanceof H5Link) {
                        rowData[i][1] = "Link";
                    }
                    else
                        rowData[i][1] = "Unknown";
                }
                
                String[] columnNames = { "Name", "Type" };
                
                Table memberTable = new Table(groupInfoGroup, SWT.BORDER);
                memberTable.setLinesVisible(true);
                memberTable.setHeaderVisible(true);
                memberTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
                memberTable.setFont(currentFont);
                
                for(int i = 0; i < columnNames.length; i++) {
                    TableColumn column = new TableColumn(memberTable, SWT.NONE);
                    column.setText(columnNames[i]);
                    column.setMoveable(false);
                }
                
                for(int i = 0; i < rowData.length; i++) {
                    TableItem item = new TableItem(memberTable, SWT.NONE);
                    item.setFont(currentFont);
                    item.setText(0, rowData[i][0]);
                    item.setText(1, rowData[i][1]);
                }
                
                for(int i = 0; i < columnNames.length; i++) {
                    memberTable.getColumn(i).pack();
                }
                
                // set cell height for large fonts
                //int cellRowHeight = Math.max(16, table.getFontMetrics(table.getFont()).getHeight());
                //table.setRowHeight(cellRowHeight);
            }
        }
        else if (obj instanceof Dataset) {
            log.trace("showMetaData: Dataset object extra info");
            Dataset d = (Dataset) obj;
            if (d.getRank() <= 0) {
                d.init();
            }
            
            org.eclipse.swt.widgets.Group datasetInfoGroup = new org.eclipse.swt.widgets.Group(attributeArea, SWT.NONE);
            datasetInfoGroup.setFont(currentFont);
            datasetInfoGroup.setText("Dataspace and Datatype");
            datasetInfoGroup.setLayout(new GridLayout(1, true));
            datasetInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            
            // Create composite for displaying dataset dimensions, dimension size,
            // max dimension size, and data type
            Composite dimensionComposite = new Composite(datasetInfoGroup, SWT.BORDER);
            dimensionComposite.setLayout(new GridLayout(2, false));
            dimensionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            
            label = new Label(dimensionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("No. of Dimension(s): ");
            
            Text text = new Text(dimensionComposite, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(currentFont);
            text.setText("" + d.getRank());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            
            label = new Label(dimensionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Dimension Size(s): ");
            
            // Set Dimension Size
            String dimStr = null;
            String maxDimStr = null;
            long dims[] = d.getDims();
            long maxDims[] = d.getMaxDims();
            if (dims != null) {
                String[] dimNames = d.getDimNames();
                boolean hasDimNames = ((dimNames != null) && (dimNames.length == dims.length));
                StringBuffer sb = new StringBuffer();
                StringBuffer sb2 = new StringBuffer();

                sb.append(dims[0]);
                if (hasDimNames) {
                    sb.append(" (");
                    sb.append(dimNames[0]);
                    sb.append(")");
                }

                if (maxDims[0] < 0)
                    sb2.append("Unlimited");
                else
                    sb2.append(maxDims[0]);

                for (int i = 1; i < dims.length; i++) {
                    sb.append(" x ");
                    sb.append(dims[i]);
                    if (hasDimNames) {
                        sb.append(" (");
                        sb.append(dimNames[i]);
                        sb.append(")");
                    }

                    sb2.append(" x ");
                    if (maxDims[i] < 0)
                        sb2.append("Unlimited");
                    else
                        sb2.append(maxDims[i]);

                }
                dimStr = sb.toString();
                maxDimStr = sb2.toString();
            }

            text = new Text(dimensionComposite, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(currentFont);
            text.setText(dimStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            
            label = new Label(dimensionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Max Dimension Size(s): ");
            
            text = new Text(dimensionComposite, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(currentFont);
            text.setText(maxDimStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            
            label = new Label(dimensionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Data Type: ");
            
            String type = null;
            if (d instanceof ScalarDS) {
                ScalarDS sd = (ScalarDS) d;
                type = sd.getDatatype().getDatatypeDescription();
            }
            else if (d instanceof CompoundDS) {
                if (isH4) {
                    type = "Vdata";
                }
                else {
                    type = "Compound";
                }
            }
            
            text = new Text(dimensionComposite, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(currentFont);
            text.setText(type);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            
            
            // Create composite for possible compound dataset info
            if (d instanceof CompoundDS) {
                log.trace("showMetaData: dataset Compound object extra info");
                CompoundDS compound = (CompoundDS) d;

                int n = compound.getMemberCount();
                if (n > 0) {
                    String rowData[][] = new String[n][3];
                    String names[] = compound.getMemberNames();
                    Datatype types[] = compound.getMemberTypes();
                    int orders[] = compound.getMemberOrders();

                    for (int i = 0; i < n; i++) {
                        if (names[i].contains(CompoundDS.separator)) {
                            names[i] = names[i].replaceAll(CompoundDS.separator, "->");
                        }
                        
                        rowData[i][0] = names[i];
                        
                        int mDims[] = compound.getMemberDims(i);
                        if (mDims == null) {
                            rowData[i][2] = String.valueOf(orders[i]);

                            if (isH4 && types[i].getDatatypeClass() == Datatype.CLASS_STRING) {
                                rowData[i][2] = String.valueOf(types[i].getDatatypeSize());
                            }
                        }
                        else {
                            String mStr = String.valueOf(mDims[0]);
                            int m = mDims.length;
                            for (int j = 1; j < m; j++) {
                                mStr += " x " + mDims[j];
                            }
                            rowData[i][2] = mStr;
                        }
                        rowData[i][1] = types[i].getDatatypeDescription();
                    }

                    String[] columnNames = { "Name", "Type", "Array Size" };
                    
                    Table memberTable = new Table(datasetInfoGroup, SWT.BORDER);
                    memberTable.setLinesVisible(true);
                    memberTable.setHeaderVisible(true);
                    memberTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
                    memberTable.setFont(currentFont);
                    
                    for(int i = 0; i < columnNames.length; i++) {
                        TableColumn column = new TableColumn(memberTable, SWT.NONE);
                        column.setText(columnNames[i]);
                        column.setMoveable(false);
                    }
                    
                    for(int i = 0; i < rowData.length; i++) {
                        TableItem item = new TableItem(memberTable, SWT.NONE);
                        item.setFont(currentFont);
                        item.setText(0, rowData[i][0]);
                        item.setText(1, rowData[i][1]);
                        item.setText(2, rowData[i][2]);
                    }
                    
                    for(int i = 0; i < columnNames.length; i++) {
                        memberTable.getColumn(i).pack();
                    }
                    
                    // set cell height for large fonts
                    //int cellRowHeight = Math.max(16, table.getFontMetrics(table.getFont()).getHeight());
                    //table.setRowHeight(cellRowHeight);
                } // if (n > 0)
            } // if (d instanceof Compound)
            
            
            // Create composite for displaying dataset chunking, compression, filters,
            // storage type, and fill value
            Composite compressionComposite = new Composite(datasetInfoGroup, SWT.BORDER);
            compressionComposite.setLayout(new GridLayout(2, false));
            compressionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            
            // Add compression and data layout information
            label = new Label(compressionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Chunking: ");
            
            // try { d.getMetadata(); } catch (Exception ex) {}
            String chunkInfo = "";
            long[] chunks = d.getChunkSize();
            if (chunks == null) {
                chunkInfo = "NONE";
            }
            else {
                int n = chunks.length;
                chunkInfo = String.valueOf(chunks[0]);
                for (int i = 1; i < n; i++) {
                    chunkInfo += " X " + chunks[i];
                }
            }
            
            label = new Label(compressionComposite, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(chunkInfo);
            
            label = new Label(compressionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Compression: ");
            
            label = new Label(compressionComposite, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(d.getCompression());
            
            label = new Label(compressionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Filters: ");
            
            label = new Label(compressionComposite, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(d.getFilters());
            
            label = new Label(compressionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Storage: ");
            
            label = new Label(compressionComposite, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(d.getStorage());
            
            label = new Label(compressionComposite, SWT.LEFT);
            label.setFont(currentFont);
            label.setText("Fill value: ");
            
            Object fillValue = null;
            String fillValueInfo = "NONE";
            if (d instanceof ScalarDS) fillValue = ((ScalarDS) d).getFillValue();
            if (fillValue != null) {
                if (fillValue.getClass().isArray()) {
                    int len = Array.getLength(fillValue);
                    fillValueInfo = Array.get(fillValue, 0).toString();
                    for (int i = 1; i < len; i++) {
                        fillValueInfo += ", ";
                        fillValueInfo += Array.get(fillValue, i).toString();
                    }
                }
                else
                    fillValueInfo = fillValue.toString();
            }
            
            label = new Label(compressionComposite, SWT.RIGHT);
            label.setFont(currentFont);
            label.setText(fillValueInfo);
        }
        else if (obj instanceof Datatype) {
            log.trace("showMetaData: Datatype object extra info");
            org.eclipse.swt.widgets.Group datatypeInfoGroup = new org.eclipse.swt.widgets.Group(attributeArea, SWT.NONE);
            datatypeInfoGroup.setFont(currentFont);
            datatypeInfoGroup.setText("Type");
            datatypeInfoGroup.setLayout(new FillLayout());
            datatypeInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            
            Text infoArea = new Text(datatypeInfoGroup, SWT.MULTI);
            infoArea.setFont(currentFont);
            infoArea.setText(((Datatype) obj).getDatatypeDescription());
            infoArea.setEditable(false);
        }
        
        attributeArea.layout();
        
        log.trace("showMetaData: finish");
    }
    
    public void closeFile(FileFormat theFile) {
        if (theFile == null) {
            display.beep();
            Tools.showError(mainWindow, "Select a file to close", mainWindow.getText());
            return;
        }

        // Close all the data windows of this file
        Shell[] views = display.getShells();
        if (views != null) {
            for (int i = 0; i < views.length; i++) {
                DataView view = (DataView) views[i].getData();
                
                if (view != null) {
                    HObject obj = view.getDataObject();
                    
                    if (obj == null) continue;
                    
                    if (obj.getFileFormat().equals(theFile)) {
                        views[i].dispose();
                        views[i] = null;
                    }
                }
            }
        }

        int index = url_bar.getSelectionIndex();
        if (index >= 0) {
            String fName = (String) url_bar.getItem(url_bar.getSelectionIndex());
            if (theFile.getFilePath().equals(fName)) {
                currentFile = null;
                url_bar.setText("");
            }
        }

        try {
            treeView.closeFile(theFile);
        }
        catch (Exception ex) {}

        theFile = null;
        
        for (Control control : attributeArea.getChildren()) control.dispose();
        
        System.gc();
    }

    public void reloadFile(FileFormat theFile) {
        int temp_index_type = 0;
        int temp_index_order = 0;

        if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
            temp_index_type = theFile.getIndexType(null);
            temp_index_order = theFile.getIndexOrder(null);
        }
        
        closeFile(theFile);

        if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
            theFile.setIndexType(temp_index_type);
            theFile.setIndexOrder(temp_index_order);
        }

        try {
            FileFormat newFile = treeView.reopenFile(theFile);
            
            if (newFile != null) {
                currentFile = newFile.getAbsolutePath();
            } else {
                currentFile = null;
            }
        }
        catch (Exception ex) {}
    }
    
    /**
     * Write the change of data to the given file.
     * 
     * @param theFile
     *           The file to be updated.
     */
    public void writeDataToFile(FileFormat theFile) {
        try {
            Shell[] openShells = display.getShells();

            if (openShells != null) {
                for (int i = 0; i < openShells.length; i++) {
                    DataView theView = (DataView) openShells[i].getData();

                    if (theView instanceof TableView) {
                        TableView tableView = (TableView) theView;
                        FileFormat file = tableView.getDataObject().getFileFormat();
                        if (file.equals(theFile)) tableView.updateValueInFile();
                    }
                    else if (theView instanceof TextView) {
                        TextView textView = (TextView) theView;
                        FileFormat file = textView.getDataObject().getFileFormat();
                        if (file.equals(theFile)) textView.updateValueInFile();
                    }
                }
            }
        }
        catch (Exception ex) {
            display.beep();
            Tools.showError(mainWindow, ex.getMessage(), mainWindow.getText());
        }
    }

    public void addDataView(DataView dataView) {
        if (dataView == null) {
            return;
        }

        // Check if the data content is already displayed
        Shell[] shellList = display.getShells();
        if (shellList != null) {
            for (int i = 0; i < shellList.length; i++) {
                if (dataView.equals((DataView) shellList[i].getData())
                        && shellList[i].isVisible()) {
                    showWindow(shellList[i]);
                    return;
                }
            }
        }

        HObject obj = dataView.getDataObject();
        String fullPath = obj.getPath() + obj.getName();

        MenuItem item = new MenuItem(windowMenu, SWT.PUSH);
        item.setText(fullPath);
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Shell[] sList = display.getShells();

                for (int i = 0; i < sList.length; i++) {
                    DataView view = (DataView) sList[i].getData();
                    
                    if (view != null) {
                        HObject obj = view.getDataObject();

                        if (obj.getFullName().equals(((MenuItem) e.widget).getText())) {
                            showWindow(sList[i]);
                        }
                    }
                }
            }
        });
    }

    public void removeDataView(DataView dataView) {
        if (mainWindow.isDisposed()) return;
        
        HObject obj = dataView.getDataObject();
        MenuItem[] items = windowMenu.getItems();
        for (int i = 0; i < items.length; i++) {
            if(items[i].getText().equals(obj.getFullName())) {
                items[i].dispose();
            }
        }
    }

    public DataView getDataView(HObject dataObject) {
        Shell[] openShells = display.getShells();
        DataView view = null;
        HObject currentObj = null;
        FileFormat currentFile = null;
        
        for (int i = 0; i < openShells.length; i++) {
            view = (DataView) openShells[i].getData();
            
            if (view != null) {
                currentObj = view.getDataObject();
                currentFile = currentObj.getFileFormat();
                
                if (currentObj.equals(dataObject) && currentFile.equals(dataObject.getFileFormat()))
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
    public void setTestState(boolean testing) {
        isTesting = testing;
    }

    /**
     * Set default UI fonts.
     */
    private void updateFont(Font font) {
        if (currentFont != null) currentFont.dispose();
        
        currentFont = font;
        
        mainWindow.setFont(font);
        recentFilesButton.setFont(font);
        url_bar.setFont(font);
        clearTextButton.setFont(font);
        status.setFont(font);
        
        // On certain platforms the url_bar items don't update their size after
        // a font change. Removing and replacing them fixes this.
        for (String item : url_bar.getItems()) {
            url_bar.remove(item);
            url_bar.add(item);
        }
        
        if (treeView.getSelectedFile() != null) {
            url_bar.select(0);
        }
        
        ((DefaultTreeView) treeView).updateFont(font);
        
        mainWindow.layout();
    }

    /**
     * Bring window to the front.
     * <p>
     *
     * @param name
     *               the name of the window to show.
     */
    private void showWindow(final Shell shell) {
        shell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                shell.forceActive();
            }
        });
    }

    /**
     * Cascade all windows.
     */
    private void cascadeWindows() {
        Shell[] sList = display.getShells();

        // Return if main window (shell) is the only open shell
        if (sList.length <= 1) return;

        int x = 2, y = 2;
        Shell shell = null;

        Rectangle bounds = Display.getCurrent().getPrimaryMonitor().getClientArea();
        int w = Math.max(50, bounds.width - 100);
        int h = Math.max(50, bounds.height - 100);

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
    private void tileWindows() {
        Shell[] sList = display.getShells();

        // Return if main window (shell) is the only open shell
        if (sList.length <= 1) return;

        int x = 0, y = 0, idx = 0;
        Shell shell = null;

        int n = sList.length;
        int cols = (int) Math.sqrt(n);
        int rows = (int) Math.ceil((double) n / (double) cols);

        Rectangle bounds = Display.getCurrent().getPrimaryMonitor().getClientArea();
        int w = bounds.width / cols;
        int h = bounds.height / rows;

        for (int i = 0; i < rows; i++) {
            x = 0;

            for (int j = 0; j < cols; j++) {
                idx = i * cols + j;
                if (idx >= n)
                    return;

                shell = sList[idx];
                shell.setBounds(x, y, w, h);
                x += w;
            }

            y += h;
        }
    }

    /**
     * Closes all windows.
     */
    private void closeAllWindows() {
        Shell[] sList = display.getShells();

        for (int i = 0; i < sList.length; i++) {
            if (sList[i].equals(mainWindow)) continue;
            sList[i].dispose();
        }
    }

    /* Enable and disable GUI components */
    private static void setEnabled(List<MenuItem> list, boolean b) {
        Iterator<MenuItem> it = list.iterator();

        while (it.hasNext())
            it.next().setEnabled(b);
    }

    /** Open local file */
    private void openLocalFile(String filename, int fileAccessID) {
        log.trace("openLocalFile {},{}",filename, fileAccessID);
        int accessMode = fileAccessID;
        if (ViewProperties.isReadOnly()) accessMode = FileFormat.READ;

        String[] selectedFilenames = null;
        File[] chosenFiles = null;

        if (filename != null) {
            File file = new File(filename);
            if(!file.exists()) {
                Tools.showError(mainWindow, "File " + filename + " does not exist.", "Open File");
                return;
            }

            if(file.isDirectory()) {
                currentDir = filename;
                openLocalFile(null, FileFormat.WRITE); // needs to be edited
            }
            else {
                currentFile = filename;
            }

            try {
                url_bar.remove(filename);
            }
            catch (Exception ex) {}

            url_bar.add(filename, 0);
            url_bar.select(0);

            try {
                treeView.openFile(filename, accessMode);
            }
            catch (Throwable ex) {
                try {
                    treeView.openFile(filename, FileFormat.READ);
                }
                catch (Throwable ex2) {
                    display.beep();
                    url_bar.deselectAll();
                    Tools.showError(mainWindow, "Failed to open file " + filename + "\n" + ex2, mainWindow.getText());
                    currentFile = null;
                }
            }
        }
        else {
            if (!isTesting) {
                log.trace("openLocalFile filename is null");
                FileDialog fChooser = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
                fChooser.setFilterPath(null);

                DefaultFileFilter filter = DefaultFileFilter.getFileFilter();
                fChooser.setFilterExtensions(new String[] {"*.*", filter.getExtensions()});
                fChooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
                fChooser.setFilterIndex(1);

                fChooser.open();

                selectedFilenames = fChooser.getFileNames();
                if(selectedFilenames.length <= 0) return;

                chosenFiles = new File[selectedFilenames.length];
                for(int i = 0; i < chosenFiles.length; i++) {
                    log.trace("openLocalFile selectedFilenames[{}]: {}",i,selectedFilenames[i]);
                    chosenFiles[i] = new File(fChooser.getFilterPath() + File.separator + selectedFilenames[i]);

                    if(!chosenFiles[i].exists()) {
                        Tools.showError(mainWindow, "File " + chosenFiles[i].getName() + " does not exist.", "Open File");
                        continue;
                    }

                    if (chosenFiles[i].isDirectory()) {
                        currentDir = chosenFiles[i].getPath();
                    }
                    else {
                        currentDir = chosenFiles[i].getParent();
                    }

                    try {
                        url_bar.remove(chosenFiles[i].getAbsolutePath());
                    }
                    catch (Exception ex) {}

                    url_bar.add(chosenFiles[i].getAbsolutePath(), 0);
                    url_bar.select(0);

                    log.trace("openLocalFile treeView.openFile(chosenFiles[{}]: {}",i,chosenFiles[i].getAbsolutePath());
                    try {
                        treeView.openFile(chosenFiles[i].getAbsolutePath(), accessMode + FileFormat.OPEN_NEW);
                    }
                    catch (Throwable ex) {
                        try {
                            treeView.openFile(chosenFiles[i].getAbsolutePath(), FileFormat.READ);
                        }
                        catch (Throwable ex2) {
                            display.beep();
                            url_bar.deselectAll();
                            Tools.showError(mainWindow, "Failed to open file " + selectedFilenames[i] + "\n" + ex2, mainWindow.getText());
                            currentFile = null;
                        }
                    }
                }

                currentFile = chosenFiles[0].getAbsolutePath();
            } else {
                // Prepend test file directory to filename
                String fName = currentDir + File.separator + new InputDialog(mainWindow, "Enter a file name", "").open();
                
                File chosenFile = new File(fName);
                
                if(!chosenFile.exists()) {
                    Tools.showError(mainWindow, "File " + chosenFile.getName() + " does not exist.", "Open File");
                    return;
                }

                if (chosenFile.isDirectory()) {
                    currentDir = chosenFile.getPath();
                }
                else {
                    currentDir = chosenFile.getParent();
                }

                try {
                    url_bar.remove(chosenFile.getAbsolutePath());
                }
                catch (Exception ex) {}

                url_bar.add(chosenFile.getAbsolutePath(), 0);
                url_bar.select(0);

                log.trace("openLocalFile treeView.openFile(chosenFile[{}]: {}",chosenFile.getAbsolutePath());
                try {
                    treeView.openFile(chosenFile.getAbsolutePath(), accessMode + FileFormat.OPEN_NEW);
                }
                catch (Throwable ex) {
                    try {
                        treeView.openFile(chosenFile.getAbsolutePath(), FileFormat.READ);
                    }
                    catch (Throwable ex2) {
                        display.beep();
                        url_bar.deselectAll();
                        Tools.showError(mainWindow, "Failed to open file " + chosenFile + "\n" + ex2, mainWindow.getText());
                        currentFile = null;
                    }
                }
                
                currentFile = chosenFile.getAbsolutePath();
            }
        }
    }

    /** Load remote file and save it to local temporary directory */
    private String openRemoteFile(String urlStr) {
        if (urlStr == null)
            return null;

        String localFile = null;

        if(urlStr.startsWith("http://")) {
            localFile = urlStr.substring(7);
        }
        else if (urlStr.startsWith("https://")) {
            localFile = urlStr.substring(8);
        }
        else if (urlStr.startsWith("ftp://")) {
            localFile = urlStr.substring(6);
        }
        else {
            return null;
        }

        localFile = localFile.replace('/', '@');
        localFile = localFile.replace('\\', '@');

        // Search the local file cache
        String tmpDir = System.getProperty("java.io.tmpdir");

        File tmpFile = new File(tmpDir);
        if (!tmpFile.canWrite()) tmpDir = System.getProperty("user.home");

        localFile = tmpDir + localFile;

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
            Tools.showError(mainWindow, ex.getMessage(), mainWindow.getText());
            return null;
        }

        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try {
            in = new BufferedInputStream(url.openStream());
            out = new BufferedOutputStream(new FileOutputStream(tmpFile));
        }
        catch (Exception ex) {
            in = null;
            display.beep();
            Tools.showError(mainWindow, ex.getMessage(), mainWindow.getText());

            try {
                out.close();
            }
            catch (Exception ex2) {
                log.debug("Remote file: ", ex2);
            }

            return null;
        }

        mainWindow.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
        byte[] buff = new byte[512]; // set default buffer size to 512
        try {
            int n = 0;
            while ((n = in.read(buff)) > 0) {
                out.write(buff, 0, n);
            }
        }
        catch (Exception ex) {
            log.debug("Remote file: ", ex);
        }

        try {
            in.close();
        }
        catch (Exception ex) {
            log.debug("Remote file: ", ex);
        }

        try {
            out.close();
        }
        catch (Exception ex) {
            log.debug("Remote file: ", ex);
        }

        mainWindow.setCursor(null);

        return localFile;
    }

    /** Open file from SRB server */
    /*private void openFromSRB() throws Exception {
        if (ctrSrbFileDialog == null) {
            Class<?> theClass = null;

            try {
                theClass = Class.forName("hdf.srb.SRBFileDialog");
            }
            catch (Exception ex) {
                theClass = null;
                showStatus(ex.toString());
                throw (new ClassNotFoundException("Cannot find SRBFileDialog"));
            }

            try {
                @SuppressWarnings("rawtypes")
                Class[] paramClass = { Class.forName("java.awt.Frame") };
                ctrSrbFileDialog = theClass.getConstructor(paramClass);
            }
            catch (Exception ex) {
                ctrSrbFileDialog = null;
                throw (new InstantiationException("Cannot construct SRBFileDialog"));
            }
        }

        if (srbFileDialog == null) {
            //try {
            //    Object[] paramObj = { (java.awt.Frame) this };
            //   srbFileDialog = (JDialog) ctrSrbFileDialog.newInstance(paramObj);
            //}
            //catch (Exception ex) {
            //    throw ex;
            //}
        }
        else {
            //srbFileDialog.setVisible(true);
        }

        // currentFile = srbFileDialog.getName();
    }*/

    private void convertFile(String typeFrom, String typeTo) {
        ImageConversionDialog dialog = new ImageConversionDialog(mainWindow, typeFrom, typeTo,
                currentDir, treeView.getCurrentFiles());
        dialog.open();

        if (dialog.isFileConverted()) {
            String filename = dialog.getConvertedFile();
            File theFile = new File(filename);

            if (!theFile.exists()) return;

            currentDir = theFile.getParentFile().getAbsolutePath();
            currentFile = theFile.getAbsolutePath();

            try {
                treeView.openFile(filename, FileFormat.WRITE);

                try {
                    url_bar.remove(filename);
                }
                catch (Exception ex) {}

                url_bar.add(filename, 0);
                url_bar.select(0);
            }
            catch (Exception ex) {
                showStatus(ex.toString());
            }
        }
    }

    private void registerFileFormat() {
        String msg = "Register a new file format by \nKEY:FILE_FORMAT:FILE_EXTENSION\n"
            + "where, KEY: the unique identifier for the file format"
            + "\n           FILE_FORMAT: the full class name of the file format"
            + "\n           FILE_EXTENSION: the file extension for the file format" + "\n\nFor example, "
            + "\n\t to add NetCDF, \"NetCDF:hdf.object.nc2.NC2File:nc\""
            + "\n\t to add FITS, \"FITS:hdf.object.fits.FitsFile:fits\"\n\n";

        // TODO:Add custom HDFLarge icon to dialog
        InputDialog dialog = new InputDialog(mainWindow, SWT.ICON_INFORMATION,
                "Register a file format", msg);
        dialog.setFont(currentFont);
        
        String str = dialog.open();

        if ((str == null) || (str.length() < 1)) return;

        int idx1 = str.indexOf(':');
        int idx2 = str.lastIndexOf(':');

        if ((idx1 < 0) || (idx2 <= idx1)) {
            Tools.showError(mainWindow, "Failed to register " + str
                    + "\n\nMust in the form of KEY:FILE_FORMAT:FILE_EXTENSION",
                    "Register File Format");
            return;
        }

        String key = str.substring(0, idx1);
        String className = str.substring(idx1 + 1, idx2);
        String extension = str.substring(idx2 + 1);

        // Check if the file format has been registered or the key is taken.
        String theKey = null;
        String theClassName = null;
        Enumeration<?> local_enum = FileFormat.getFileFormatKeys();
        while (local_enum.hasMoreElements()) {
            theKey = (String) local_enum.nextElement();
            if (theKey.endsWith(key)) {
                Tools.showError(mainWindow, "Invalid key: " + key + " is taken.", "Register File Format");
                return;
            }

            theClassName = FileFormat.getFileFormat(theKey).getClass().getName();
            if (theClassName.endsWith(className)) {
                Tools.showError(mainWindow, "The file format has already been registered: " + className,
                        "Register File Format");
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

        if (theClass == null) {
            return;
        }

        try {
            Object theObject = theClass.newInstance();
            if (theObject instanceof FileFormat) {
                FileFormat.addFileFormat(key, (FileFormat) theObject);
            }
        }
        catch (Throwable ex) {
            Tools.showError(mainWindow, "Failed to register " + str + "\n\n" + ex, "Register File Format");
            return;
        }

        if ((extension != null) && (extension.length() > 0)) {
            extension = extension.trim();
            String ext = ViewProperties.getFileExtension();
            ext += ", " + extension;
            ViewProperties.setFileExtension(ext);
        }
    }

    private void unregisterFileFormat() {
        Enumeration<?> keys = FileFormat.getFileFormatKeys();
        ArrayList<Object> keyList = new ArrayList<Object>();

        while (keys.hasMoreElements())
            keyList.add((Object) keys.nextElement());

        String theKey = new UnregisterFileFormatDialog(mainWindow, SWT.NONE, keyList).open();

        if (theKey == null) return;

        FileFormat.removeFileFormat(theKey);
    }

    private class LibraryVersionDialog extends Dialog {
        private String message;

        public LibraryVersionDialog(Shell parent, String libType) {
            super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);

            if(libType == FileFormat.FILE_TYPE_HDF4)
                setMessage("HDF " + HDF4_VERSION);
            else if (libType == FileFormat.FILE_TYPE_HDF5)
                setMessage("HDF5 " + HDF5_VERSION);
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void open() {
            Shell dialog = new Shell(getParent(), getStyle());
            dialog.setFont(currentFont);
            dialog.setText("HDF Library Version");
            
            createContents(dialog);

            dialog.pack();

            Point computedSize = dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            dialog.setSize(computedSize.x + 50, computedSize.y + 50);

            // Center the window relative to the main HDFView window
            Point winCenter = new Point(
                    mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
                    mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));

            dialog.setLocation(winCenter.x - (dialog.getSize().x / 2), winCenter.y - (dialog.getSize().y / 2));

            dialog.open();

            Display display = getParent().getDisplay();
            while (!dialog.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        }

        private void createContents(final Shell shell) {
            shell.setLayout(new GridLayout(2, false));

            Image HDFImage = ViewProperties.getLargeHdfIcon();

            Label imageLabel = new Label(shell, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(HDFImage);

            Label versionLabel = new Label(shell, SWT.CENTER);
            versionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            versionLabel.setFont(currentFont);
            versionLabel.setText(message);

            // Draw HDF Icon and Version string
            Composite buttonComposite = new Composite(shell, SWT.NONE);
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            RowLayout buttonLayout = new RowLayout();
            buttonLayout.center = true;
            buttonLayout.justify = true;
            buttonLayout.type = SWT.HORIZONTAL;
            buttonComposite.setLayout(buttonLayout);

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &Ok   ");
            shell.setDefaultButton(okButton);
            okButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    shell.dispose();
                }
            });
        }
    }

    private class JavaVersionDialog extends Dialog {
        public JavaVersionDialog(Shell parent) {
            super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
        }

        public void open() {
            final Shell dialog = new Shell(getParent(), getStyle());
            dialog.setFont(currentFont);
            dialog.setText("HDFView Java Version");
            dialog.setLayout(new GridLayout(2, false));

            Image HDFImage = ViewProperties.getLargeHdfIcon();

            Label imageLabel = new Label(dialog, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(HDFImage);

            Label versionLabel = new Label(dialog, SWT.CENTER);
            versionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            versionLabel.setFont(currentFont);
            versionLabel.setText(JAVA_VER_INFO);

            Composite buttonComposite = new Composite(dialog, SWT.NONE);
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            RowLayout buttonLayout = new RowLayout();
            buttonLayout.center = true;
            buttonLayout.justify = true;
            buttonLayout.type = SWT.HORIZONTAL;
            buttonComposite.setLayout(buttonLayout);

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &Ok   ");
            dialog.setDefaultButton(okButton);
            okButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    dialog.dispose();
                }
            });

            dialog.pack();

            Point computedSize = dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            dialog.setSize(computedSize.x + 50, computedSize.y + 50);

            // Center the window relative to the main HDFView window
            Point winCenter = new Point(
                    mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
                    mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));

            dialog.setLocation(winCenter.x - (dialog.getSize().x / 2), winCenter.y - (dialog.getSize().y / 2));

            dialog.open();

            Display display = getParent().getDisplay();
            while (!dialog.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        }
    }

    private class SupportedFileFormatsDialog extends Dialog {
        public SupportedFileFormatsDialog(Shell parent) {
            super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
        }

        public void open() {
            final Shell dialog = new Shell(getParent(), getStyle());
            dialog.setFont(currentFont);
            dialog.setText("Supported File Formats");
            dialog.setLayout(new GridLayout(2, false));

            Enumeration<?> formatKeys = FileFormat.getFileFormatKeys();

            String formats = "\nSupported File Formats: \n";
            while (formatKeys.hasMoreElements()) {
                formats += "    " + formatKeys.nextElement() + "\n";
            }
            formats += "\n";

            Image HDFImage = ViewProperties.getLargeHdfIcon();

            Label imageLabel = new Label(dialog, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(HDFImage);

            Label formatsLabel = new Label(dialog, SWT.LEFT);
            formatsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            formatsLabel.setFont(currentFont);
            formatsLabel.setText(formats);

            Composite buttonComposite = new Composite(dialog, SWT.NONE);
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            RowLayout buttonLayout = new RowLayout();
            buttonLayout.center = true;
            buttonLayout.justify = true;
            buttonLayout.type = SWT.HORIZONTAL;
            buttonComposite.setLayout(buttonLayout);

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &Ok   ");
            dialog.setDefaultButton(okButton);
            okButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    dialog.dispose();
                }
            });

            dialog.pack();

            Point computedSize = dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            dialog.setSize(computedSize.x + 50, computedSize.y + 50);

            // Center the window relative to the main HDFView window
            Point winCenter = new Point(
                    mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
                    mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));

            dialog.setLocation(winCenter.x - (dialog.getSize().x / 2), winCenter.y - (dialog.getSize().y / 2));

            dialog.open();

            Display display = getParent().getDisplay();
            while (!dialog.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        }
    }

    private class AboutDialog extends Dialog {
        public AboutDialog(Shell parent) {
            super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
        }

        public void open() {
            final Shell dialog = new Shell(getParent(), getStyle());
            dialog.setFont(currentFont);
            dialog.setText("About HDFView");
            dialog.setLayout(new GridLayout(2, false));

            Image HDFImage = ViewProperties.getLargeHdfIcon();

            Label imageLabel = new Label(dialog, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(HDFImage);

            Label aboutLabel = new Label(dialog, SWT.LEFT);
            aboutLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            aboutLabel.setFont(currentFont);
            aboutLabel.setText(aboutHDFView);

            Composite buttonComposite = new Composite(dialog, SWT.NONE);
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            RowLayout buttonLayout = new RowLayout();
            buttonLayout.center = true;
            buttonLayout.justify = true;
            buttonLayout.type = SWT.HORIZONTAL;
            buttonComposite.setLayout(buttonLayout);

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &Ok   ");
            dialog.setDefaultButton(okButton);
            okButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    dialog.dispose();
                }
            });

            dialog.pack();

            Point computedSize = dialog.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            dialog.setSize(computedSize.x + 50, computedSize.y + 50);

            // Center the window relative to the main HDFView window
            Point winCenter = new Point(
                    mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
                    mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));

            dialog.setLocation(winCenter.x - (dialog.getSize().x / 2), winCenter.y - (dialog.getSize().y / 2));

            dialog.open();

            Display display = getParent().getDisplay();
            while (!dialog.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        }
    }

    private class UnregisterFileFormatDialog extends Dialog {

        private List<Object> keyList;
        private String formatChoice = null;

        public UnregisterFileFormatDialog(Shell parent, int style, List<Object> keyList) {
            super(parent, style);

            this.keyList = keyList;
        }

        public String open() {
            Shell parent = getParent();
            final Shell shell = new Shell(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
            shell.setFont(currentFont);
            shell.setText("Unregister a file format");
            shell.setLayout(new GridLayout(2, false));

            Image HDFImage = ViewProperties.getLargeHdfIcon();

            Label imageLabel = new Label(shell, SWT.CENTER);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setImage(HDFImage);


            final Combo formatChoiceCombo = new Combo(shell, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
            formatChoiceCombo.setFont(currentFont);
            formatChoiceCombo.setItems(keyList.toArray(new String[0]));
            formatChoiceCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
            formatChoiceCombo.select(0);
            formatChoiceCombo.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    formatChoice = formatChoiceCombo.getItem(formatChoiceCombo.getSelectionIndex());
                }
            });

            Composite buttonComposite = new Composite(shell, SWT.NONE);
            buttonComposite.setLayout(new GridLayout(2, true));
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(currentFont);
            okButton.setText("   &Ok   ");
            okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    shell.dispose();
                }
            });

            Button cancelButton = new Button(buttonComposite, SWT.PUSH);
            cancelButton.setFont(currentFont);
            cancelButton.setText("&Cancel");
            cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            cancelButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    shell.dispose();
                }
            });

            shell.pack();

            Point computedSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            shell.setSize(computedSize.x + 50, computedSize.y + 50);

            Rectangle parentBounds = parent.getBounds();
            Point shellSize = shell.getSize();
            shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                              (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            shell.open();

            Display display = parent.getDisplay();
            while(!shell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
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
     *        [filename] "the file to open"
     * </pre>
     *
     * @param args  the command line arguments
     */
    public static void main(String[] args) {
        String rootDir = System.getProperty("hdfview.workdir");
        log.trace("main: rootDir = {} ", rootDir);
        if(rootDir == null) rootDir = System.getProperty("user.dir");

        File tmpFile = null;
        Monitor primaryMonitor = display.getPrimaryMonitor();
        Point margin = new Point(primaryMonitor.getBounds().width,
                          primaryMonitor.getBounds().height);


        int j = args.length;
        int W = margin.x / 2,
            H = margin.y,
            X = 0,
            Y = 0;

        for(int i = 0; i < args.length; i++) {
            if ("-root".equalsIgnoreCase(args[i])) {
                j--;
                try {
                    j--;
                    tmpFile = new File(args[++i]);

                    if(tmpFile.isDirectory()) {
                        rootDir = tmpFile.getPath();
                    }
                    else if(tmpFile.isFile()) {
                        rootDir = tmpFile.getParent();
                    }
                }
                catch (Exception ex) {}
            }
            else if("-g".equalsIgnoreCase(args[i]) || "-geometry".equalsIgnoreCase(args[i])) {
                j--;
                // -geometry WIDTHxHEIGHT+XOFF+YOFF
                try {
                    String geom = args[++i];
                    j--;

                    int idx = 0;
                    int idx2 = geom.lastIndexOf('-');
                    int idx3 = geom.lastIndexOf('+');

                    idx = Math.max(idx2, idx3);
                    if(idx > 0) {
                        Y = Integer.parseInt(geom.substring(idx + 1));

                        if(idx == idx2)
                            Y = -Y;

                        geom = geom.substring(0, idx);
                        idx2 = geom.lastIndexOf('-');
                        idx3 = geom.lastIndexOf('+');
                        idx = Math.max(idx2, idx3);

                        if(idx > 0) {
                            X = Integer.parseInt(geom.substring(idx + 1));

                            if(idx == idx2)
                                X = -X;

                            geom = geom.substring(0, idx);
                        }
                    }

                    idx = geom.indexOf('x');

                    if(idx > 0) {
                        W = Integer.parseInt(geom.substring(0, idx));
                        H = Integer.parseInt(geom.substring(idx + 1));
                    }

                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            else if("-java.version".equalsIgnoreCase(args[i])) {
                /* Set icon to ViewProperties.getLargeHdfIcon() */
                MessageBox info = new MessageBox(mainWindow, SWT.ICON_INFORMATION | SWT.OK);
                info.setText(mainWindow.getText());
                info.setMessage(JAVA_VER_INFO);
                info.open();
                System.exit(0);
            }
        }

        Vector<File> fList = new Vector<File>();
        tmpFile = null;

        if(j >= 0) {
            for(int i = args.length - j; i < args.length; i++) {
                tmpFile = new File(args[i]);
                if(tmpFile.exists() && (tmpFile.isFile() || tmpFile.isDirectory()))
                    fList.add(new File(tmpFile.getAbsolutePath()));
            }
        }

        final Vector<File> the_fList = fList;
        final String the_rootDir = rootDir;
        final int the_X = X, the_Y = Y, the_W = W, the_H = H;

        display.syncExec(new Runnable() {
            public void run() {
                HDFView app = new HDFView(the_rootDir);
                
                // TODO: Look for a better solution to native dialog problem
                app.setTestState(false);
                
                app.openMainWindow(the_fList, the_W, the_H, the_X, the_Y);
                app.runMainWindow();
            }
        });
    }
}
