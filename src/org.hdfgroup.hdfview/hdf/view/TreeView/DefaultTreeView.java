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

package hdf.view.TreeView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import hdf.hdf5lib.HDF5Constants;

import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;
import hdf.object.ScalarDS;

import hdf.view.DefaultFileFilter;
import hdf.view.HDFView;
import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.ViewProperties.DATA_VIEW_KEY;
import hdf.view.ViewProperties.DataViewType;
import hdf.view.DataView.DataView;
import hdf.view.DataView.DataViewFactory;
import hdf.view.DataView.DataViewFactoryProducer;
import hdf.view.DataView.DataViewManager;
import hdf.view.MetaDataView.MetaDataView;
import hdf.view.dialog.DataOptionDialog;
import hdf.view.dialog.InputDialog;
import hdf.view.dialog.NewCompoundDatasetDialog;
import hdf.view.dialog.NewDatasetDialog;
import hdf.view.dialog.NewDatatypeDialog;
import hdf.view.dialog.NewGroupDialog;
import hdf.view.dialog.NewImageDialog;
import hdf.view.dialog.NewLinkDialog;

/**
 * TreeView defines APIs for opening files and displaying the file structure in
 * a tree structure.
 *
 * TreeView uses folders and leaf items to represent groups and data objects in
 * the file. You can expand or collapse folders to navigate data objects in the
 * file.
 *
 * From the TreeView, you can open data content or metadata of the selected object.
 * You can select object(s) to delete or add new objects to the file.
 *
 * @author Jordan T. Henderson
 * @version 2.4 12//2015
 */
public class DefaultTreeView implements TreeView {

    private static final Logger log = LoggerFactory.getLogger(DefaultTreeView.class);

    private Shell                         shell;

    private Font                          curFont;

    /** The owner of this TreeView */
    private DataViewManager               viewer;

    /** Thread to load TableView Data in the background */
    private LoadDataThread                loadDataThread;

    /**
     * The tree which holds file structures.
     */
    private final Tree                    tree;

    /** The currently selected tree item */
    private TreeItem                      selectedItem = null;

    /** The list of current selected objects for copying */
    private TreeItem[]                    objectsToCopy = null;

    private TreeItem[]                    currentSelectionsForMove = null;

    /** The currently selected object */
    private HObject                       selectedObject;

    /** The currently selected file */
    private FileFormat                    selectedFile;

    /** Maintains a list of TreeItems in the tree in breadth-first order
     * to prevent many calls of getAllItemsBreadthFirst.
     */
    //private ArrayList<TreeItem>         breadthFirstItems = null;

    /** A list of currently open files */
    private final List<FileFormat>        fileList = new ArrayList<>();

    /** A list of editing GUI components */
    private List<MenuItem>                editGUIs = new ArrayList<>();

    /**
     * The popup menu used to display user choice of actions on data object.
     */
    private final Menu                    popupMenu;

    private Menu                          newObjectMenu;
    private Menu                          exportDatasetMenu;

    private MenuItem                      openVirtualFilesMenuItem;
    private MenuItem                      addDatasetMenuItem;
    private MenuItem                      exportDatasetMenuItem;
    private MenuItem                      addTableMenuItem;
    private MenuItem                      addDatatypeMenuItem;
    private MenuItem                      addLinkMenuItem;
    private MenuItem                      setLibVerBoundsItem;
    private MenuItem                      changeIndexItem;

    /** Keep Image instances to prevent many calls to ViewProperties.getTypeIcon() */
    private Image h4Icon = ViewProperties.getH4Icon();
    private Image h4IconR = ViewProperties.getH4IconR();
    private Image h5Icon = ViewProperties.getH5Icon();
    private Image h5IconR = ViewProperties.getH5IconR();
    private Image nc3Icon = ViewProperties.getNC3Icon();
    private Image nc3IconR = ViewProperties.getNC3IconR();
    private Image imageIcon = ViewProperties.getImageIcon();
    private Image imageIconA = ViewProperties.getImageIconA();
    private Image textIcon = ViewProperties.getTextIcon();
    private Image textIconA = ViewProperties.getTextIconA();
    private Image datasetIcon = ViewProperties.getDatasetIcon();
    private Image datasetIconA = ViewProperties.getDatasetIconA();
    private Image tableIcon = ViewProperties.getTableIcon();
    private Image tableIconA = ViewProperties.getTableIconA();
    private Image datatypeIcon = ViewProperties.getDatatypeIcon();
    private Image datatypeIconA = ViewProperties.getDatatypeIconA();
    private Image folderCloseIcon = ViewProperties.getFoldercloseIcon();
    private Image folderCloseIconA = ViewProperties.getFoldercloseIconA();
    private Image folderOpenIcon = ViewProperties.getFolderopenIcon();
    private Image folderOpenIconA = ViewProperties.getFolderopenIconA();
    private Image questionIcon = ViewProperties.getQuestionIcon();

    /** Flag to indicate if the dataset is displayed as default */
    private boolean                       isDefaultDisplay = true;

    /** Flag to indicate if TreeItems are being moved */
    private boolean                       moveFlag = false;

    private boolean                       isApplyBitmaskOnly  = false;

    private int                           binaryOrder;

    private String                        currentSearchPhrase = null;

    /** Used to open a File using a temporary indexing type and order */
    private int                           tempIdxType = -1;
    private int                           tempIdxOrder = -1;

    private enum OBJECT_TYPE {GROUP, DATASET, IMAGE, TABLE, DATATYPE, LINK};

    /**
     * Create a visual component for opening files and displaying the file
     * structure in a tree structure.
     *
     * @param parent
     *        the parent component
     * @param theView
     *        the associated data view manager
     */
    public DefaultTreeView(Composite parent, DataViewManager theView) {
        viewer = theView;
        shell = parent.getShell();

        try {
            curFont = new Font(
                    Display.getCurrent(),
                    ViewProperties.getFontType(),
                    ViewProperties.getFontSize(),
                    SWT.NORMAL);
        }
        catch (Exception ex) {
            curFont = null;
        }

        // Initialize the Tree
        tree = new Tree(parent, SWT.MULTI | SWT.VIRTUAL);
        tree.setSize(tree.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        tree.setFont(curFont);

        // Create the context menu for the Tree
        popupMenu = createPopupMenu();
        tree.setMenu(popupMenu);

        // Handle tree key events
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.keyCode;

                if (key == SWT.ARROW_DOWN || key == SWT.ARROW_UP || key == SWT.KEYPAD_2 || key == SWT.KEYPAD_8) {
                    TreeItem[] selectedItems = tree.getSelection();
                    TreeItem theItem = selectedItems[0];

                    if(theItem.equals(selectedItem)) return;

                    selectedItem = theItem;
                    selectedObject = ((HObject) (selectedItem.getData()));
                    FileFormat theFile = selectedObject.getFileFormat();
                    if ((theFile != null) && !theFile.equals(selectedFile)) {
                        // A different file is selected, handle only one file at a time
                        selectedFile = theFile;
                        tree.deselectAll();
                    }

                    ((HDFView) viewer).showMetaData(selectedObject);
                }
                else if (key == SWT.ARROW_LEFT || key == SWT.KEYPAD_4) {
                    if(selectedObject instanceof Group) {
                        selectedItem.setExpanded(false);

                        Event collapse = new Event();
                        collapse.item = selectedItem;

                        tree.notifyListeners(SWT.Collapse, collapse);
                    }
                }
                else if (key == SWT.ARROW_RIGHT || key == SWT.KEYPAD_6) {
                    if(selectedObject instanceof Group) {
                        selectedItem.setExpanded(true);

                        Event expand = new Event();
                        expand.item = selectedItem;

                        tree.notifyListeners(SWT.Expand, expand);
                    }
                }
            }
        });

        /**
         * If user presses Enter on a TreeItem, expand/collapse the item if it
         * is a group, or try to show the data content if it is a data object.
         */
        tree.addListener(SWT.Traverse, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.detail != SWT.TRAVERSE_RETURN)
                    return;

                TreeItem item = selectedItem;
                if(item == null)
                    return;

                final HObject obj = (HObject) item.getData();
                if(obj == null)
                    return;

                if(obj instanceof Group) {
                    boolean isExpanded = item.getExpanded();

                    item.setExpanded(!isExpanded);

                    Event expand = new Event();
                    expand.item = item;

                    if(isExpanded)
                        tree.notifyListeners(SWT.Collapse, expand);
                    else
                        tree.notifyListeners(SWT.Expand, expand);
                }
                else {
                    try {
                        loadDataThread = new LoadDataThread();
                        loadDataThread.start();
                    }
                    catch (Exception err) {
                        shell.getDisplay().beep();
                        Tools.showError(shell, "Select", err.getMessage());
                    }
                }
            }
        });

        /**
         * Handle mouse clicks on data objects in the tree view. A right mouse-click
         * to show the popup menu for user choice. A double left-mouse-click to
         * display the data content. A single left-mouse-click to select the current
         * data object.
         */
        tree.addMouseListener(new MouseAdapter() {
            // Double click opens data content of selected data object
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                isDefaultDisplay = true;

                try {
                    if(!(selectedObject instanceof Group)) {
                        loadDataThread = new LoadDataThread();
                        loadDataThread.start();
                    }
                    else {
                        boolean isExpanded = selectedItem.getExpanded();

                        selectedItem.setExpanded(!isExpanded);

                        Event expand = new Event();
                        expand.item = selectedItem;

                        if(isExpanded)
                            tree.notifyListeners(SWT.Collapse, expand);
                        else
                            tree.notifyListeners(SWT.Expand, expand);
                    }
                }
                catch (Exception ex) {
                    log.trace("defaultDisplay showDataContent failed: {}", ex.getMessage());
                    ex.printStackTrace();
                }
            }

            // When a mouse release is detected, attempt to set the selected item
            // and object to the TreeItem under the pointer
            @Override
            public void mouseUp(MouseEvent e) {
                // Make sure user clicked on a TreeItem
                TreeItem theItem = tree.getItem(new Point(e.x, e.y));

                if (theItem == null) {
                    tree.deselectAll();
                    selectedItem = null;
                    selectedObject = null;
                    selectedFile = null;

                    // Clear any information shown in the object info panel
                    ((HDFView) viewer).showMetaData(null);

                    return;
                }

                if (theItem.equals(selectedItem))
                    return;

                FileFormat theFile = null;

                selectedItem = theItem;

                try {
                    selectedObject = (HObject) selectedItem.getData();
                }
                catch(NullPointerException ex) {
                    viewer.showError("Object " + selectedItem.getText() + " had no associated data.");
                    return;
                }

                try {
                    theFile = selectedObject.getFileFormat();
                }
                catch(NullPointerException ex) {
                    viewer.showError("Error retrieving FileFormat of HObject " + selectedObject.getName() + ".");
                    return;
                }

                if ((theFile != null) && !theFile.equals(selectedFile)) {
                    // A different file is selected, handle only one file at a time
                    selectedFile = theFile;
                }

                // Set this file to the most recently selected file in the recent files bar
                Combo recentFilesCombo = ((HDFView) viewer).getUrlBar();
                String filename = selectedFile.getAbsolutePath();

                try {
                    recentFilesCombo.remove(filename);
                }
                catch (Exception ex) {}

                // first entry is always the workdir
                recentFilesCombo.add(filename, 1);
                recentFilesCombo.select(1);

                ((HDFView) viewer).showMetaData(selectedObject);
            }
        });

        // Show context menu only if user has selected a data object
        tree.addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent e) {
                Display display = Display.getDefault();

                Point pt = display.map(null, tree, new Point(e.x, e.y));
                TreeItem item = tree.getItem(pt);
                if(item == null) { e.doit = false; return; }

                FileFormat theFile = null;

                selectedItem = item;

                log.trace("tree.addMenuDetectListener(): selectedItem={}", selectedItem.getText());
                try {
                    selectedObject = (HObject) selectedItem.getData();
                }
                catch(NullPointerException ex) {
                    viewer.showError("Object " + selectedItem.getText() + " had no associated data.");
                    return;
                }

                try {
                    theFile = selectedObject.getFileFormat();
                }
                catch(NullPointerException ex) {
                    viewer.showError("Error retrieving FileFormat of HObject " + selectedObject.getName() + ".");
                    return;
                }

                if ((theFile != null) && !theFile.equals(selectedFile)) {
                    // A different file is selected, handle only one file at a time
                    selectedFile = theFile;
                    //tree.deselectAll();
                    //tree.setSelection(selPath);
                    log.trace("tree.addMenuDetectListener(): selectedFile={}", selectedFile.getAbsolutePath());
                }

                ((HDFView) viewer).showMetaData(selectedObject);

                popupMenu.setLocation(display.map(tree, null, pt));
                popupMenu.setVisible(true);
            }
        });

        tree.addListener(SWT.Expand, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TreeItem item = (TreeItem) event.item;
                Object obj = item.getData();

                if (!(obj instanceof Group))
                    return;

                Group theGroup = (Group) item.getData();

                if(theGroup.isRoot())
                    return;

                // Prevent graphical issues from happening by stopping
                // tree from redrawing until all the items are created
                tree.setRedraw(false);

                if(item.getItemCount() > 0)
                    item.setImage(theGroup.hasAttribute() ? folderOpenIconA : folderOpenIcon);

                // Process any remaining SetData events and then allow
                // the tree to redraw once all are finished
                //                while(tree.getDisplay().readAndDispatch());

                tree.setRedraw(true);
            }
        });

        tree.addListener(SWT.Collapse, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TreeItem item = (TreeItem) event.item;
                Object obj = item.getData();

                if (!(obj instanceof Group))
                    return;

                Group theGroup = (Group) item.getData();

                if(theGroup.isRoot())
                    return;

                item.setImage(theGroup.hasAttribute() ? folderCloseIconA : folderCloseIcon);
            }
        });

        // When groups are expanded, populate TreeItems corresponding to file objects
        // on demand.
        tree.addListener(SWT.SetData, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TreeItem item = (TreeItem) event.item;
                TreeItem parentItem = item.getParentItem();

                int position = parentItem.indexOf(item);
                HObject obj = ((Group) parentItem.getData()).getMember(position);

                item.setData(obj);
                item.setFont(curFont);
                item.setText(obj.getName());
                item.setImage(getObjectTypeImage(obj));

                if(obj instanceof Group)
                    item.setItemCount(((Group) obj).getMemberList().size());
            }
        });

        tree.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null)
                    curFont.dispose();
            }
        });
    }

    /** Creates a popup menu for a right mouse click on a data object */
    private Menu createPopupMenu() {
        Menu menu = new Menu(tree);
        MenuItem item;

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Open");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                isDefaultDisplay = true;

                try {
                    loadDataThread = new LoadDataThread();
                    loadDataThread.start();
                }
                catch (Exception err) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Open", err.getMessage());
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Open &As");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                isDefaultDisplay = false;

                try {
                    loadDataThread = new LoadDataThread();
                    loadDataThread.start();
                }
                catch (Exception err) {
                    shell.getDisplay().beep();
                    err.printStackTrace();
                    Tools.showError(shell, "Open", err.getMessage());
                }
            }
        });

        openVirtualFilesMenuItem = new MenuItem(menu, SWT.PUSH);
        openVirtualFilesMenuItem.setText("Open Source Fi&les");
        openVirtualFilesMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                isDefaultDisplay = false;

                log.trace("createPopupMenu(): selectedObject={}", selectedObject);
                // If dataset is virtual - open source files. Only for HDF5
                if (selectedObject != null) {
                    log.trace("createPopupMenu(): selectedObject={} is dataset instance-{} of type H5 {}", selectedObject, selectedObject instanceof Dataset, selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)));
                    if (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)) &&
                            (selectedObject instanceof Dataset)) {
                        Dataset dataset = (Dataset) selectedObject;
                        boolean isVirtual = dataset.isVirtual();
                        log.trace("createPopupMenu(): isVirtual={}", isVirtual);
                        if(isVirtual) {
                            for(int ndx=0; ndx<dataset.getVirtualMaps(); ndx++) {
                                try {
                                    String theFile = selectedFile.getParentFile().getAbsolutePath() + File.separator + dataset.getVirtualFilename(ndx);
                                    openFile(theFile, FileFormat.WRITE);
                                }
                                catch (Exception ex) {
                                    shell.getDisplay().beep();
                                    ex.printStackTrace();
                                    Tools.showError(shell, "Open", ex.getMessage() + "\n" + dataset.getVirtualFilename(ndx));
                                }
                                log.trace("createPopupMenu(): virtualNameList[{}]={}", ndx, dataset.getVirtualFilename(ndx));
                            }
                        }
                    }
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        MenuItem newObjectMenuItem = new MenuItem(menu, SWT.CASCADE);
        newObjectMenuItem.setText("New");
        editGUIs.add(newObjectMenuItem);

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Cu&t");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                moveObject();
            }
        });
        editGUIs.add(item);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Copy");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copyObject();
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Paste");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                pasteObject();
            }
        });
        editGUIs.add(item);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Delete");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cutObject();
            }
        });
        editGUIs.add(item);

        exportDatasetMenuItem = new MenuItem(menu, SWT.CASCADE);
        exportDatasetMenuItem.setText("Export Dataset");

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Save to");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] selectedItems = tree.getSelection();

                if (selectedItems.length <= 0) return;

                for (int i = 0; i < selectedItems.length; i++) {
                    if (((HObject) selectedItems[i].getData() instanceof Group)
                            && ((Group) selectedItems[i].getData()).isRoot()) {
                        shell.getDisplay().beep();
                        Tools.showError(shell, "Save", "Cannot save the root group.\nUse \"Save As\" from file menu to save the whole file");
                        return;
                    }
                }

                String filetype = FileFormat.FILE_TYPE_HDF4;
                if (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))
                    filetype = FileFormat.FILE_TYPE_HDF5;
                else if (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3)))
                    filetype = FileFormat.FILE_TYPE_NC3;

                String currentDir = selectedObject.getFileFormat().getParent();

                if (currentDir != null)
                    currentDir += File.separator;
                else
                    currentDir = "";

                FileDialog fChooser = new FileDialog(shell, SWT.SAVE);

                DefaultFileFilter filter = null;

                if (filetype.equals(FileFormat.FILE_TYPE_HDF4)) {
                    fChooser.setFileName(Tools.checkNewFile(currentDir, ".hdf").getName());
                    filter = DefaultFileFilter.getFileFilterHDF4();
                }
                else if (filetype.equals(FileFormat.FILE_TYPE_NC3)) {
                    fChooser.setFileName(Tools.checkNewFile(currentDir, ".nc").getName());
                    filter = DefaultFileFilter.getFileFilterNetCDF3();
                }
                else {
                    fChooser.setFileName(Tools.checkNewFile(currentDir, ".h5").getName());
                    filter = DefaultFileFilter.getFileFilterHDF5();
                }

                fChooser.setFilterExtensions(new String[] {filter.getExtensions()});
                fChooser.setFilterNames(new String[] {filter.getDescription()});
                fChooser.setFilterIndex(0);

                String filename = fChooser.open();

                if(filename == null)
                    return;

                try {
                    Tools.createNewFile(filename, currentDir, filetype, fileList);
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Save", ex.getMessage());
                }

                FileFormat dstFile = null;

                try {
                    dstFile = openFile(filename, FileFormat.WRITE);
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Save", ex.getMessage() + "\n" + filename);
                }
                if (dstFile != null)
                    pasteObject(selectedItems, findTreeItem(dstFile.getRootObject()), dstFile);
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Rename");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                renameObject();
            }
        });
        editGUIs.add(item);

        new MenuItem(menu, SWT.SEPARATOR);

        changeIndexItem = new MenuItem(menu, SWT.PUSH);
        changeIndexItem.setText("Change file indexing");
        changeIndexItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ChangeIndexingDialog dialog = new ChangeIndexingDialog(shell, SWT.NONE, selectedFile);
                dialog.open();
                if (dialog.isReloadFile()) {
                    try {
                        selectedFile.setIndexType(dialog.getIndexType());
                    }
                    catch (Exception ex) {
                        log.debug("ChangeIndexingDialog(): setIndexType failed: ", ex);
                    }
                    try {
                        selectedFile.setIndexOrder(dialog.getIndexOrder());
                    }
                    catch (Exception ex) {
                        log.debug("ChangeIndexingDialog(): setIndexOrder failed: ", ex);
                    }

                    try {
                        reopenFile(selectedFile, -1);
                    }
                    catch (Exception ex) {
                        log.debug("reload file {} failure after indexing change: ", selectedFile.getAbsolutePath(), ex);
                        Tools.showError(shell, "File reload error", "Error reloading file " + selectedFile.getAbsolutePath() + " after changing indexing: " + ex.getMessage());
                    }
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Find");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String findStr = currentSearchPhrase;
                if (findStr == null)
                    findStr = "";

                findStr = (new InputDialog(shell, "Find Object by Name",
                        "Find (e.g. O3Quality, O3*, or *Quality):", findStr)).open();

                if (findStr != null && findStr.length() > 0)
                    currentSearchPhrase = findStr;

                find(currentSearchPhrase, selectedItem);
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Expand All");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if(selectedItem != null)
                    recursiveExpand(selectedItem, true);
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Collapse All");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if(selectedItem != null)
                    recursiveExpand(selectedItem, false);
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Close Fil&e");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    ((HDFView) viewer).closeFile(selectedFile);
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Close", ex.getMessage());
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Reload File");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    reopenFile(selectedFile, -1);
                }
                catch (Exception ex) {
                    log.debug("reload file {} failure: ", selectedFile.getAbsolutePath(), ex);
                    Tools.showError(shell, "File reload error", "Error reloading file " + selectedFile.getAbsolutePath() + ": " + ex.getMessage());
                }
            }
        });

        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Reload File As");

        Menu reloadFileMenu = new Menu(item);
        item.setMenu(reloadFileMenu);

        item = new MenuItem(reloadFileMenu, SWT.PUSH);
        item.setText("Read-Only");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    reopenFile(selectedFile, FileFormat.READ);
                }
                catch (Exception ex) {
                    log.debug("reload file {} as read-only failure: ", selectedFile.getAbsolutePath(), ex);
                    Tools.showError(shell, "File reload error", "Error reloading file " + selectedFile.getAbsolutePath() + " read-only: " + ex.getMessage());
                }
            }
        });

        item = new MenuItem(reloadFileMenu, SWT.PUSH);
        item.setText("SWMR Read-Only");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    reopenFile(selectedFile, FileFormat.READ | FileFormat.MULTIREAD);
                }
                catch (Exception ex) {
                    log.debug("reload file {} as SWMR read-only failure: ", selectedFile.getAbsolutePath(), ex);
                    Tools.showError(shell, "File reload error", "Error reloading file " + selectedFile.getAbsolutePath() + " SWMR read-only: " + ex.getMessage());
                }
            }
        });

        item = new MenuItem(reloadFileMenu, SWT.PUSH);
        item.setText("Read/Write");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    reopenFile(selectedFile, FileFormat.WRITE);
                }
                catch (Exception ex) {
                    log.debug("reload file {} as read/write failure: ", selectedFile.getAbsolutePath(), ex);
                    Tools.showError(shell, "File reload error", "Error reloading file " + selectedFile.getAbsolutePath() + " read/write: " + ex.getMessage());
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        setLibVerBoundsItem = new MenuItem(menu, SWT.NONE);
        setLibVerBoundsItem.setText("Set Lib version bounds");
        setLibVerBoundsItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new ChangeLibVersionDialog(shell, SWT.NONE).open();
            }
        });


        // Add new object menu
        newObjectMenu = new Menu(menu);
        newObjectMenuItem.setMenu(newObjectMenu);

        item = new MenuItem(newObjectMenu, SWT.PUSH);
        item.setText("Group");
        item.setImage(ViewProperties.getFoldercloseIcon());
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addNewObject(OBJECT_TYPE.GROUP);
            }
        });
        editGUIs.add(item);

        addDatasetMenuItem = new MenuItem(newObjectMenu, SWT.PUSH);
        addDatasetMenuItem.setText("Dataset");
        addDatasetMenuItem.setImage(ViewProperties.getDatasetIcon());
        addDatasetMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addNewObject(OBJECT_TYPE.DATASET);
            }
        });
        editGUIs.add(addDatasetMenuItem);

        item = new MenuItem(newObjectMenu, SWT.PUSH);
        item.setText("Image");
        item.setImage(ViewProperties.getImageIcon());
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addNewObject(OBJECT_TYPE.IMAGE);
            }
        });
        editGUIs.add(item);

        addTableMenuItem = new MenuItem(newObjectMenu, SWT.PUSH);
        addTableMenuItem.setText("Compound DS");
        addTableMenuItem.setImage(ViewProperties.getTableIcon());
        addTableMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addNewObject(OBJECT_TYPE.TABLE);
            }
        });
        editGUIs.add(addTableMenuItem);

        addDatatypeMenuItem = new MenuItem(newObjectMenu, SWT.PUSH);
        addDatatypeMenuItem.setText("Datatype");
        addDatatypeMenuItem.setImage(ViewProperties.getDatatypeIcon());
        addDatatypeMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addNewObject(OBJECT_TYPE.DATATYPE);
            }
        });
        editGUIs.add(addDatatypeMenuItem);

        addLinkMenuItem = new MenuItem(newObjectMenu, SWT.PUSH);
        addLinkMenuItem.setText("Link");
        addLinkMenuItem.setImage(ViewProperties.getLinkIcon());
        addLinkMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addNewObject(OBJECT_TYPE.LINK);
            }
        });
        editGUIs.add(addLinkMenuItem);


        // Add export dataset menu
        exportDatasetMenu = new Menu(menu);
        exportDatasetMenuItem.setMenu(exportDatasetMenu);

        item = new MenuItem(exportDatasetMenu, SWT.PUSH);
        item.setText("Export Data to Text File");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 99;

                try {
                    saveDataAsFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Export Dataset", ex.getMessage());
                }
            }
        });

        item = new MenuItem(exportDatasetMenu, SWT.PUSH);
        item.setText("Export Data as Native Order");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 1;

                try {
                    saveDataAsFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Export Dataset", ex.getMessage());
                }
            }
        });

        item = new MenuItem(exportDatasetMenu, SWT.PUSH);
        item.setText("Export Data as Little Endian");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 2;

                try {
                    saveDataAsFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Export Dataset", ex.getMessage());
                }
            }
        });

        item = new MenuItem(exportDatasetMenu, SWT.PUSH);
        item.setText("Export Data as Big Endian");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 3;

                try {
                    saveDataAsFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Export Dataset", ex.getMessage());
                }
            }
        });

        // Add listener to dynamically enable/disable menu items based
        // on selection in tree
        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(MenuEvent e) {
                if (selectedItem == null || selectedObject == null || selectedFile == null) return;

                boolean isReadOnly = selectedObject.getFileFormat().isReadOnly();
                boolean isWritable = !isReadOnly;

                setEnabled(editGUIs, isWritable);

                if (selectedObject instanceof Group) {
                    boolean state = !(((Group) selectedObject).isRoot());

                    popupMenu.getItem(0).setEnabled(false); // "Open" menuitem
                    popupMenu.getItem(1).setEnabled(false); // "Open as" menuitem
                    popupMenu.getItem(2).setEnabled(false); // "Open Source Files" menuitem
                    popupMenu.getItem(6).setEnabled(
                            (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))
                            && state && isWritable); // "Cut" menuitem
                    popupMenu.getItem(7).setEnabled(state); // "Copy" menuitem
                    popupMenu.getItem(9).setEnabled(state && isWritable); // "Delete" menuitem
                    popupMenu.getItem(10).setEnabled(false); // "Export Dataset" menuitem
                    popupMenu.getItem(12).setEnabled(state && isWritable); // "Save to" menuitem
                    popupMenu.getItem(13).setEnabled(state && isWritable); // "Rename" menuitem
                }
                else {
                    popupMenu.getItem(0).setEnabled(true); // "Open" menuitem
                    popupMenu.getItem(1).setEnabled(true); // "Open as" menuitem
                    popupMenu.getItem(2).setEnabled(
                            (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))); // "Open Source Files" menuitem
                    popupMenu.getItem(6).setEnabled(
                            (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))
                            && isWritable); // "Cut" menuitem
                    popupMenu.getItem(7).setEnabled(true); // "Copy" menuitem
                    popupMenu.getItem(9).setEnabled(isWritable); // "Delete" menuitem
                    popupMenu.getItem(10).setEnabled(true); // "Export Dataset" menuitem
                    popupMenu.getItem(12).setEnabled(true); // "Save to" menuitem
                    popupMenu.getItem(13).setEnabled(isWritable); // "Rename" menuitem
                }

                // Adding table is only supported by HDF5
                if ((selectedFile != null) && selectedFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                    //openVirtualFilesMenuItem.setEnabled(true); // Should be moved to createPopupMenu() since swt doesn't support MenuItem.setVisible

                    boolean state = false;
                    if ((selectedObject instanceof Group)) {
                        state = (((Group) selectedObject).isRoot());
                        setLibVerBoundsItem.setEnabled(isWritable && state);
                    }
                    else {
                        setLibVerBoundsItem.setEnabled(false);
                    }

                    changeIndexItem.setEnabled(state);
                }
                else {
                    addTableMenuItem.setEnabled(false);
                    addDatatypeMenuItem.setEnabled(false);
                    addLinkMenuItem.setEnabled(false);
                    //openVirtualFilesMenuItem.setEnabled(false);
                    setLibVerBoundsItem.setEnabled(false);
                    changeIndexItem.setEnabled(false);
                }

                // Export table is only supported by HDF5
                if ((selectedObject != null) && selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                    if ((selectedObject instanceof Dataset)) {
                        Dataset dataset = (Dataset) selectedObject;
                        if ((dataset instanceof ScalarDS))
                            exportDatasetMenuItem.setEnabled(true);
                        openVirtualFilesMenuItem.setEnabled(true);
                    }
                    else {
                        exportDatasetMenuItem.setEnabled(false);
                        openVirtualFilesMenuItem.setEnabled(false);
                    }
                }
                else {
                    exportDatasetMenuItem.setEnabled(false);
                    openVirtualFilesMenuItem.setEnabled(false);
                }
            }
        });

        return menu;
    }

    /**
     * Creates a dialog for the user to select a type of new
     * object to be added to the TreeView, then passes the
     * result of the dialog on to addObject(HObject newObject, Group parentGroup)
     *
     * @param type
     *          The type (GROUP, DATASET, IMAGE, TABLE, DATATYPE, LINK) of object to add.
     */
    private void addNewObject(OBJECT_TYPE type) {
        if ((selectedObject == null) || (selectedItem == null)) return;

        TreeItem parentItem = null;
        if(selectedObject instanceof Group)
            parentItem = selectedItem;
        else
            parentItem = selectedItem.getParentItem();

        // Find the root item of the selected file
        TreeItem rootItem = selectedItem;
        while(rootItem.getParentItem() != null)
            rootItem = rootItem.getParentItem();

        HObject obj = null;

        switch(type) {
        case GROUP:
            NewGroupDialog groupDialog = new NewGroupDialog(shell, (Group) parentItem.getData(),
                    breadthFirstUserObjects(rootItem));
            groupDialog.open();
            obj = groupDialog.getObject();
            parentItem = findTreeItem(groupDialog.getParentGroup());
            break;
        case DATASET:
            NewDatasetDialog datasetDialog = new NewDatasetDialog(shell, (Group) parentItem.getData(), breadthFirstUserObjects(rootItem));
            datasetDialog.open();
            obj = datasetDialog.getObject();
            parentItem = findTreeItem(datasetDialog.getParentGroup());
            break;
        case IMAGE:
            NewImageDialog imageDialog = new NewImageDialog(shell, (Group) parentItem.getData(), breadthFirstUserObjects(rootItem));
            imageDialog.open();
            obj = imageDialog.getObject();
            parentItem = findTreeItem(imageDialog.getParentGroup());
            break;
        case TABLE:
            NewCompoundDatasetDialog tableDialog = new NewCompoundDatasetDialog(shell, (Group) parentItem.getData(), breadthFirstUserObjects(rootItem));
            tableDialog.open();
            obj = tableDialog.getObject();
            parentItem = findTreeItem(tableDialog.getParentGroup());
            break;
        case DATATYPE:
            NewDatatypeDialog datatypeDialog = new NewDatatypeDialog(shell, (Group) parentItem.getData(), breadthFirstUserObjects(rootItem));
            datatypeDialog.open();
            obj = datatypeDialog.getObject();
            parentItem = findTreeItem(datatypeDialog.getParentGroup());
            break;
        case LINK:
            NewLinkDialog linkDialog = new NewLinkDialog(shell, (Group) parentItem.getData(), breadthFirstUserObjects(rootItem), getCurrentFiles());
            linkDialog.open();
            obj = linkDialog.getObject();
            parentItem = findTreeItem(linkDialog.getParentGroup());
            break;
        }

        if (obj == null)
            return;

        try {
            this.insertObject(obj, parentItem);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", ex.getMessage());
        }
    }

    /**
     * Adds an already created HObject to the tree under the
     * TreeItem containing the specified parent group.
     *
     * @param obj
     *            the object to add.
     * @param parentGroup
     *            the parent group to add the object to.
     */
    @Override
    public TreeItem addObject(HObject obj, Group parentGroup) {
        if ((obj == null) || (parentGroup == null))
            return null;

        return insertObject(obj, findTreeItem(parentGroup));
    }

    /**
     * Insert an object into the tree as the last object
     * under parent item pobj.
     *
     * @param obj
     *            the object to insert.
     * @param pobj
     *            the parent TreeItem to insert the new object under.
     *            If null, inserts the object at the end of the Tree.
     *
     * @return the newly created TreeItem
     */
    private TreeItem insertObject(HObject obj, TreeItem pobj) {
        if ((obj == null))
            return null;

        TreeItem item;

        if(pobj != null) {
            item = new TreeItem(pobj, SWT.NONE, pobj.getItemCount());
            item.setFont(curFont);
            item.setText(obj.getName());
        }
        else {
            // Parent object was null, insert at end of tree as root object
            item = new TreeItem(tree, SWT.NONE, tree.getItemCount());
            item.setFont(curFont);
            item.setText(obj.getFileFormat().getName());
        }

        item.setData(obj);
        item.setImage(getObjectTypeImage(obj));

        return item;
    }

    /** Move selected objects */
    private void moveObject() {
        objectsToCopy = tree.getSelection();
        moveFlag = true;
        currentSelectionsForMove = tree.getSelection();
    }

    /** Copy selected objects */
    private void copyObject() {
        if (moveFlag)
            if(!Tools.showConfirm(shell, "Copy object", "Do you want to copy all the selected object(s) instead of move?"))
                return;
        moveFlag = false;
        currentSelectionsForMove = null;
        objectsToCopy = tree.getSelection();
    }

    /** Delete selected objects */
    private void cutObject() {
        if (moveFlag)
            if(!Tools.showConfirm(shell, "Delete object", "Do you want to delete all the selected object(s) instead of move?"))
                return;
        moveFlag = false;
        currentSelectionsForMove = null;
        objectsToCopy = tree.getSelection();
        removeSelectedObjects();
    }

    /** Paste selected objects */
    private void pasteObject() {
        if (moveFlag) {
            HObject theObj = null;
            for (int i = 0; i < currentSelectionsForMove.length; i++) {
                TreeItem currentItem = currentSelectionsForMove[i];
                theObj = (HObject) currentItem.getData();

                if (isObjectOpen(theObj)) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Move Objects", "Cannot move the selected object: " + theObj
                            + "\nThe dataset or dataset in the group is in use."
                            + "\n\nPlease close the dataset(s) and try again.\n");

                    moveFlag = false;
                    currentSelectionsForMove = null;
                    objectsToCopy = null;
                    return;
                }
            }
        }

        TreeItem pitem = selectedItem;

        if ((objectsToCopy == null) || (objectsToCopy.length <= 0) || (pitem == null))
            return;

        FileFormat srcFile = ((HObject) objectsToCopy[0].getData()).getFileFormat();
        FileFormat dstFile = getSelectedFile();
        FileFormat h5file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
        FileFormat h4file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
        FileFormat ncfile = FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3);

        if (srcFile == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Copy", "Source file is null.");
            return;
        }
        else if (dstFile == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Copy", "Destination file is null.");
            return;
        }
        else if (srcFile.isThisType(h4file) && dstFile.isThisType(h5file)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Copy", "Unsupported operation: cannot copy HDF4 object to HDF5 file");
            return;
        }
        else if (srcFile.isThisType(h5file) && dstFile.isThisType(h4file)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Copy", "Unsupported operation: cannot copy HDF5 object to HDF4 file");
            return;
        }
        else if (srcFile.isThisType(ncfile) && dstFile.isThisType(ncfile)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Copy", "Unsupported operation: cannot copy NetCDF3 objects");
            return;
        }

        if (moveFlag) {
            if (srcFile != dstFile) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Move", "Cannot move the selected object to different file");
                moveFlag = false;
                currentSelectionsForMove = null;
                objectsToCopy = null;
                return;
            }
        }

        /*
        if (pitem.getParentItem() != null) {
            pitem = pitem.getParentItem();
        }*/

        Group pgroup = (Group) pitem.getData();
        String fullPath = pgroup.getPath() + pgroup.getName();
        if (pgroup.isRoot()) {
            fullPath = HObject.SEPARATOR;
        }

        String msg = "";
        if (srcFile.isThisType(h4file))
            msg = "WARNING: object can not be deleted after it is copied.\n\n";

        msg += "Do you want to copy the selected object(s) to \nGroup: " + fullPath + "\nFile: "
                + dstFile.getFilePath();

        if (moveFlag) {
            String moveMsg = "Do you want to move the selected object(s) to \nGroup: " + fullPath + "\nFile: "
                    + dstFile.getFilePath();
            if(!Tools.showConfirm(shell, "Move Object", moveMsg))
                return;
        }
        else {
            if(!Tools.showConfirm(shell, "Copy object", msg))
                return;
        }

        pasteObject(objectsToCopy, pitem, dstFile);

        if (moveFlag) {
            removeSelectedObjects();
            moveFlag = false;
            currentSelectionsForMove = null;
            objectsToCopy = null;
        }
    }

    /** Paste selected objects */
    private void pasteObject(TreeItem[] objList, TreeItem pobj, FileFormat dstFile) {
        if ((objList == null) || (objList.length <= 0) || (pobj == null)) return;

        FileFormat srcFile = ((HObject) objList[0].getData()).getFileFormat();
        Group pgroup = (Group) pobj.getData();

        HObject theObj = null;
        for (int i = 0; i < objList.length; i++) {
            theObj = (HObject) objList[i].getData();

            if ((theObj instanceof Group) && ((Group) theObj).isRoot()) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Paste", "Unsupported operation: cannot copy the root group");
                return;
            }

            // Check if it creates infinite loop
            Group pg = pgroup;
            while (!pg.isRoot()) {
                if (theObj.equals(pg)) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Paste", "Unsupported operation: cannot copy a group to itself.");
                    return;
                }
                pg = pg.getParent();
            }

            try {
                log.trace("pasteObject(...): dstFile.copy({}, {}, null)", theObj, pgroup);

                HObject newObj = null;
                if((newObj = srcFile.copy(theObj, pgroup, null)) != null) {
                    // Add the node to the tree
                    TreeItem newItem = insertObject(newObj, pobj);

                    // If this is a group, add its first level child items
                    if(newObj instanceof Group) {
                        Iterator<HObject> children = ((Group) newObj).getMemberList().iterator();
                        while(children.hasNext())
                            insertObject(children.next(), newItem);
                    }
                }
            }
            catch (Exception ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Paste", ex.getMessage());
            }
        } // (int i = 0; i < objList.length; i++)
    }

    /**
     * Rename the currently selected object.
     */
    private void renameObject() {
        if (moveFlag) {
            if(!Tools.showConfirm(shell,  "Rename object", "Do you want to rename all the selected object(s) instead of move?"))
                return;
        }
        moveFlag = false;
        currentSelectionsForMove = null;

        if (selectedObject == null)
            return;

        if ((selectedObject instanceof Group) && ((Group) selectedObject).isRoot()) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Rename", "Cannot rename the root.");
            return;
        }

        boolean isH4 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        if (isH4) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Rename", "Cannot rename HDF4 object.");
            return;
        }

        boolean isN3 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3));
        if (isN3) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Rename", "Cannot rename NetCDF3 object.");
            return;
        }

        String oldName = selectedObject.getName();
        String newName = (new InputDialog(shell, "Rename Object",
                "Rename \"" + oldName + "\" to:", oldName)).open();

        if (newName == null)
            return;

        newName = newName.trim();
        if ((newName == null) || (newName.length() == 0) || newName.equals(oldName))
            return;

        try {
            selectedObject.setName(newName);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Rename Object", ex.getMessage());
        }

        selectedItem.setText(newName);
    }

    private void removeSelectedObjects() {
        FileFormat theFile = getSelectedFile();
        if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Remove object", "Unsupported operation: cannot delete HDF4 object.");
            return;
        }
        if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Remove object", "Unsupported operation: cannot delete NetCDF3 object.");
            return;
        }

        TreeItem[] currentSelections = tree.getSelection();

        if (moveFlag)
            currentSelections = currentSelectionsForMove;
        if ((currentSelections == null) || (currentSelections.length <= 0))
            return;

        if (!moveFlag) {
            if(!Tools.showConfirm(shell, "Remove object", "Do you want to remove all the selected object(s) ?"))
                return;
        }

        HObject theObj = null;
        for (int i = 0; i < currentSelections.length; i++) {
            TreeItem currentItem = currentSelections[i];
            theObj = (HObject) currentItem.getData();

            // Cannot delete a root object
            if (theObj instanceof Group && ((Group) theObj).isRoot()) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Delete Objects", "Unsupported operation: cannot delete the file root.");
                return;
            }

            if (!moveFlag) {
                if (isObjectOpen(theObj)) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Delete Objects", "Cannot delete the selected object: " + theObj
                            + "\nThe dataset or dataset in the group is in use."
                            + "\n\nPlease close the dataset(s) and try again.\n");
                    continue;
                }
            }

            try {
                theFile.delete(theObj);
            }
            catch (Exception ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Delete Objects", ex.getMessage());
                continue;
            }

            // When a TreeItem is disposed, it should be removed from its parent
            // items member list to prevent a bug when copying and deleting
            // groups/datasets
            ((Group) currentItem.getParentItem().getData()).removeFromMemberList(theObj);

            if (currentItem.equals(selectedItem)) {
                selectedItem = null;
                selectedObject = null;
                selectedFile = null;
            }

            currentItem.dispose();
        } // (int i=0; i < currentSelections.length; i++)
    }

    /**
     * Populates the TreeView with TreeItems corresponding to
     * the top-level user objects in the specified file. The rest
     * of the user objects in the file are populated as TreeItems
     * on demand when the user expands groups.
     *
     * @return the root TreeItem created in the Tree corresponding
     * to the file object.
     */
    private TreeItem populateTree(FileFormat theFile) {
        if (theFile == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Open File", "Error opening file");
            log.debug("Error populating tree, File object was null.");
            return null;
        }
        else if ((theFile.getFID() < 0) || (theFile.getRootObject() == null)) {
            if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
                log.trace("populateTree(): FileID={} Null Root={}", theFile.getFID(), (theFile.getRootObject() == null));
            }
            //TODO: Update FitsFile and NC2File to have a fid other than -1
            // so this check isn't needed
            if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4)) ||
                    //theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3)) ||
                    theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Open File", "Error opening file " + theFile.getName());
                log.debug("Error populating tree for {}, File ID was wrong or File root object was null.", theFile.getFilePath());
                return null;
            }
        }

        TreeItem rootItem = null;

        try {
            rootItem = insertObject(theFile.getRootObject(), null);
            if (rootItem != null) {
                Iterator<HObject> it = ((Group) rootItem.getData()).getMemberList().iterator();
                while (it.hasNext()) {
                    TreeItem newItem = null;
                    HObject obj = it.next();

                    newItem = insertObject(obj, rootItem);

                    // Tell SWT how many members this group has so they can
                    // be populated when the group is expanded
                    if (obj instanceof Group) {
                        newItem.setItemCount(((Group) obj).getMemberList().size());
                        log.debug("populateTree(): group members size {}:", ((Group) obj).getMemberList().size());
                    }
                }
            }
        }
        catch (Exception ex) {
            log.debug("populateTree(): Error populating Tree with members of file {}:", theFile.getFilePath(), ex);
            if (rootItem != null)
                rootItem.dispose();
            shell.getDisplay().beep();
            Tools.showError(shell, "Open File", "Error opening file " + theFile.getName() + "\n\n" + ex.getMessage());
            return null;
        }

        return rootItem;
    }

    /**
     * Recursively expand/collapse a given selected TreeItem.
     *
     * @param item the selected tree item
     * @param expand
     *            Expands the TreeItem and its children if true.
     *            Collapse the TreeItem and its children if false.
     */
    //TODO: some groups dont get expanded right, likely due to SetData not being
    // able to catch up with a large number of expanding
    private void recursiveExpand(TreeItem item, boolean expand) {
        if(item == null || !(item.getData() instanceof Group))
            return;

        TreeItem[] toExpand = item.getItems();

        item.setExpanded(expand);

        // Make sure the TreeItem's icon gets set appropriately by
        // notifying its Expand or Collapse listener
        Event event = new Event();
        event.item = item;
        tree.notifyListeners(expand ? SWT.Expand : SWT.Collapse, event);

        // All SetData events for this group must be processed before any
        // child groups can be expanded, otherwise their data will be
        // null
        while(tree.getDisplay().readAndDispatch());

        for(int i = 0; i < toExpand.length; i++)
            recursiveExpand(toExpand[i], expand);
    }

    /**
     * Gets the Image to set on the TreeItem for the specified HObject,
     * based on the type of HObject it is.
     *
     * @param obj
     *
     * @return the image for the specified HObject
     */
    private Image getObjectTypeImage(HObject obj) {
        if (obj == null)
            return null;

        // Should be safe to cast to a MetaDataContainer here because the
        // TreeView should never be able to select an object that does
        // not implement the MetaDataContainer interface
        boolean hasAttribute = ((MetaDataContainer) obj).hasAttribute();

        if(obj instanceof Dataset) {
            if (obj instanceof ScalarDS) {
                ScalarDS sd = (ScalarDS) obj;
                Datatype dt = sd.getDatatype();

                if (sd.isImage()) {
                    if (hasAttribute)
                        return imageIconA;
                    else
                        return imageIcon;
                }
                else if ((dt != null) && dt.isText()) {
                    if (hasAttribute)
                        return textIconA;
                    else
                        return textIcon;
                }
                else {
                    if (hasAttribute)
                        return datasetIconA;
                    else
                        return datasetIcon;
                }
            }
            else if (obj instanceof CompoundDS) {
                if (hasAttribute)
                    return tableIconA;
                else
                    return tableIcon;
            }
        }
        else if(obj instanceof Group) {
            if(((Group) obj).isRoot()) {
                FileFormat theFile = obj.getFileFormat();

                if(theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3))) {
                    if(theFile.isReadOnly())
                        return nc3IconR;
                    else
                        return nc3Icon;
                }
                else if(theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
                    if(theFile.isReadOnly())
                        return h4IconR;
                    else
                        return h4Icon;
                }
                else {
                    if(theFile.isReadOnly())
                        return h5IconR;
                    else
                        return h5Icon;
                }
            }
            else {
                if(hasAttribute)
                    return folderCloseIconA;
                else
                    return folderCloseIcon;
            }
        }
        else if(obj instanceof Datatype) {
            if(hasAttribute)
                return datatypeIconA;
            else
                return datatypeIcon;
        }

        return questionIcon;
    }

    /**
     * Checks if a file is already open.
     *
     * @param filename the file to query
     *
     * @return true if the file is open
     */
    private boolean isFileOpen(String filename) {
        boolean isOpen = false;

        // Find the file by matching its file name from the list of open files
        FileFormat theFile = null;
        Iterator<FileFormat> iterator = fileList.iterator();
        while (iterator.hasNext()) {
            theFile = iterator.next();
            if (theFile.getFilePath().equals(filename)) {
                isOpen = true;
                break;
            }
        }

        return isOpen;
    }

    /**
     * Checks if an object is already open.
     */
    private boolean isObjectOpen(HObject obj) {
        boolean isOpen = false;

        if (obj instanceof Group) {
            Group g = (Group) obj;
            List<?> members = g.getMemberList();
            if ((members == null) || members.isEmpty()) {
                return false;
            }
            else {
                int n = members.size();
                for (int i = 0; i < n; i++) {
                    HObject theObj = (HObject) members.get(i);
                    isOpen = (viewer.getDataView(theObj) != null);
                    if (isOpen)
                        break;
                }
            }
        }
        else {
            return (viewer.getDataView(obj) != null);
        }

        return isOpen;
    }

    /**
     * Returns a list that lists all TreeItems in the
     * current Tree that are children of the specified
     * TreeItem in a breadth-first manner.
     *
     * @param the current Tree item
     *
     * @return list of TreeItems
     */
    private ArrayList<TreeItem> getItemsBreadthFirst(TreeItem item) {
        if (item == null)
            return null;

        ArrayList<TreeItem> allItems = new ArrayList<>();
        Queue<TreeItem> currentChildren = new LinkedList<>();
        TreeItem currentItem = item;

        // Add all root items in the Tree to a Queue
        currentChildren.addAll(Arrays.asList(currentItem.getItems()));

        // For every item in the queue, remove it from the head of the queue,
        // add it to the list of all items, then add all of its possible children
        // TreeItems to the end of the queue. This produces a breadth-first
        // ordering of the Tree's TreeItems.
        while(!currentChildren.isEmpty()) {
            currentItem = currentChildren.remove();
            allItems.add(currentItem);

            if(currentItem.getItemCount() <= 0)
                continue;

            currentChildren.addAll(Arrays.asList(currentItem.getItems()));
        }

        return allItems;
    }

    /**
     * Returns a list of all user objects that traverses the subtree rooted at
     * this item in breadth-first order..
     *
     * @param item
     *            the item to start with.
     */
    private final List<Object> breadthFirstUserObjects(TreeItem item) {
        if (item == null) return null;

        ArrayList<Object> list = new ArrayList<>();
        list.add(item.getData()); // Add this item to the list first

        Iterator<TreeItem> it = getItemsBreadthFirst(item).iterator();
        TreeItem theItem = null;

        while (it.hasNext()) {
            theItem = it.next();
            list.add(theItem.getData());
        }

        return list;
    }

    /**
     * Find first object that is matched by name under the specified
     * TreeItem.
     *
     * @param objName
     *            -- the object name.
     * @return the object if found, otherwise, returns null.
     */
    private final HObject find(String objName, TreeItem parentItem) {
        if (objName == null || objName.length() <= 0 || parentItem == null) return null;

        HObject retObj = null;
        boolean isFound = false;
        boolean isPrefix = false;
        boolean isSuffix = false;
        boolean isContain = false;

        if (objName.equals("*"))
            return null;

        if (objName.startsWith("*")) {
            isSuffix = true;
            objName = objName.substring(1, objName.length());
        }

        if (objName.endsWith("*")) {
            isPrefix = true;
            objName = objName.substring(0, objName.length() - 1);
        }

        if (isPrefix && isSuffix) {
            isContain = true;
            isPrefix = isSuffix = false;
        }

        if (objName == null || objName.length() <= 0)
            return null;

        HObject obj = null;
        String theName = null;
        TreeItem theItem = null;
        Iterator<TreeItem> it = getItemsBreadthFirst(parentItem).iterator();
        while (it.hasNext()) {
            theItem = it.next();
            obj = (HObject) theItem.getData();
            if (obj != null && (theName = obj.getName()) != null) {
                if (isPrefix)
                    isFound = theName.startsWith(objName);
                else if (isSuffix)
                    isFound = theName.endsWith(objName);
                else if (isContain)
                    isFound = theName.contains(objName);
                else
                    isFound = theName.equals(objName);

                if (isFound) {
                    retObj = obj;
                    break;
                }
            }
        }

        if (retObj != null) {
            tree.deselectAll();
            tree.setSelection(theItem);
            tree.showItem(theItem);
        }

        return retObj;
    }

    /**
     * Save the current file into a new HDF4 file. Since HDF4 does not
     * support packing, the source file is copied into the new file with
     * the exact same content.
     */
    private final void saveAsHDF4(FileFormat srcFile) {
        if (srcFile == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "Select a file to save.");
            return;
        }

        HObject root = srcFile.getRootObject();
        if (root == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "The file is empty.");
            return;
        }

        String currentDir = srcFile.getParent();

        if (currentDir != null)
            currentDir += File.separator;
        else
            currentDir = "";

        String filename = null;
        if (((HDFView) viewer).getTestState()) {
            filename = currentDir + File.separator + new InputDialog(shell, "Enter a file name", "").open();
        }
        else {
            FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
            fChooser.setFileName(Tools.checkNewFile(currentDir, ".hdf").getName());

            DefaultFileFilter filter = DefaultFileFilter.getFileFilterHDF4();
            fChooser.setFilterExtensions(new String[] {filter.getExtensions()});
            fChooser.setFilterNames(new String[] {filter.getDescription()});
            fChooser.setFilterIndex(0);

            filename = fChooser.open();
        }
        if(filename == null)
            return;

        try {
            Tools.createNewFile(filename, currentDir, FileFormat.FILE_TYPE_HDF4, fileList);
        }
        catch (Exception ex) {
            Tools.showError(shell, "Save", ex.getMessage());
        }

        // Since cannot pack hdf4, simply copy the whole physical file
        int length = 0;
        int bsize = 512;
        byte[] buffer;

        try (BufferedInputStream bi = new BufferedInputStream(new FileInputStream(srcFile.getFilePath()))) {
            try (BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(filename))) {
                buffer = new byte[bsize];
                try {
                    length = bi.read(buffer, 0, bsize);
                }
                catch (Exception ex) {
                    length = 0;
                }
                while (length > 0) {
                    try {
                        bo.write(buffer, 0, length);
                        length = bi.read(buffer, 0, bsize);
                    }
                    catch (Exception ex) {
                        length = 0;
                    }
                }

                try {
                    bo.flush();
                }
                catch (Exception ex) {
                    log.debug("Output file:", ex);
                }
            }
            catch (Exception ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Save", ex.getMessage());
                return;
            }
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", ex.getMessage() + "\n" + filename);
            return;
        }

        try {
            openFile(filename, FileFormat.WRITE);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", ex.getMessage() + "\n" + filename);
        }
    }

    /**
     * Copy the current file into a new HDF5 file. The new file does not include the
     * inaccessible objects. Values of reference dataset are not updated in the
     * new file.
     */
    private void saveAsHDF5(FileFormat srcFile) {
        if (srcFile == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "Select a file to save.");
            return;
        }

        HObject root = srcFile.getRootObject();
        if (root == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "The file is empty.");
            return;
        }

        String currentDir = srcFile.getParent();

        if (currentDir != null)
            currentDir += File.separator;
        else
            currentDir = "";

        String filename = null;
        if (((HDFView) viewer).getTestState()) {
            filename = currentDir + File.separator + new InputDialog(shell, "Enter a file name", "").open();
        }
        else {
            FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
            fChooser.setFileName(Tools.checkNewFile(currentDir, ".h5").getName());

            DefaultFileFilter filter = DefaultFileFilter.getFileFilterHDF5();
            fChooser.setFilterExtensions(new String[] {filter.getExtensions()});
            fChooser.setFilterNames(new String[] {filter.getDescription()});
            fChooser.setFilterIndex(0);

            filename = fChooser.open();
        }
        if(filename == null)
            return;

        try {
            Tools.createNewFile(filename, currentDir, FileFormat.FILE_TYPE_HDF5, fileList);
        }
        catch (Exception ex) {
            Tools.showError(shell, "Save", ex.getMessage());
        }

        TreeItem rootItem = findTreeItem(root);
        int n = rootItem.getItemCount();
        ArrayList<TreeItem> objList = new ArrayList<>(n);

        try {
            for (int i = 0; i < n; i++) objList.add(rootItem.getItem(i));
        }
        catch (Exception ex) {
            log.debug("saveAsHDF5() objList add failure: ", ex);
        }

        FileFormat newFile = null;
        try {
            newFile = openFile(filename, FileFormat.WRITE);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", ex.getMessage() + "\n" + filename);
            return;
        }

        if (newFile == null)
            return;

        HObject pitem = newFile.getRootObject();

        pasteObject(objList.toArray(new TreeItem[0]), findTreeItem(pitem), newFile);
        objList.clear();

        Group srcGroup = (Group) root;
        Group dstGroup = (Group) newFile.getRootObject();
        Object[] parameter = new Object[2];
        Class<?> classHOjbect = null;
        Class<?>[] parameterClass = new Class[2];
        Method method = null;

        // Copy attributes of the root group
        try {
            parameter[0] = srcGroup;
            parameter[1] = dstGroup;
            classHOjbect = Class.forName("hdf.object.HObject");
            parameterClass[0] = parameterClass[1] = classHOjbect;
            method = newFile.getClass().getMethod("copyAttributes", parameterClass);
            method.invoke(newFile, parameter);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", ex.getMessage());
        }

        // Update reference datasets
        parameter[0] = srcGroup.getFileFormat();
        parameter[1] = newFile;
        parameterClass[0] = parameterClass[1] = parameter[0].getClass();
        try {
            method = newFile.getClass().getMethod("updateReferenceDataset", parameterClass);
            method.invoke(newFile, parameter);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", ex.getMessage());
        }
    }

    /** Save data as file.
     *
     * @throws Exception if a failure occurred
     */
    private void saveDataAsFile() throws Exception {
        if (!(selectedObject instanceof Dataset) || (selectedObject == null) || (selectedItem == null))
            return;

        File chosenFile = null;
        String filename = null;
        Dataset dataset = (Dataset) selectedObject;
        String currentDir = dataset.getFile().substring(0, dataset.getFile().lastIndexOf(File.separator));
        String msgtext = null;
        if(binaryOrder == 99)
            msgtext = "Save Dataset Data To Text File --- " + dataset.getName();
        else
            msgtext = "Save Current Data To Binary File --- " + dataset.getName();
        if (((HDFView) viewer).getTestState()) {
            filename = currentDir + File.separator + new InputDialog(shell, msgtext, "").open();
        }
        else {
            FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
            fChooser.setFilterPath(currentDir);

            DefaultFileFilter filter = null;

            if (binaryOrder == 99) {
                fChooser.setText(msgtext);
                fChooser.setFileName(dataset.getName() + ".txt");
                filter = DefaultFileFilter.getFileFilterText();
            }
            else {
                fChooser.setText(msgtext);
                fChooser.setFileName(dataset.getName() + ".bin");
                filter = DefaultFileFilter.getFileFilterBinary();
            }

            fChooser.setFilterExtensions(new String[] {"*", filter.getExtensions()});
            fChooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
            fChooser.setFilterIndex(1);

            filename = fChooser.open();
        }
        if(filename == null)
            return;

        // Check if the file is in use
        List<?> saveFileList = viewer.getTreeView().getCurrentFiles();
        if (saveFileList != null) {
            FileFormat theFile = null;
            Iterator<?> iterator = saveFileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(filename)) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Export Dataset", "Unable to save data to file \"" + filename + "\". \nThe file is being used.");
                    return;
                }
            }
        }

        chosenFile = new File(filename);

        if (chosenFile.exists()) {
            if(!Tools.showConfirm(shell, "Export Dataset", "File exists. Do you want to replace it?"))
                return;
        }

        boolean isH4 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        if (isH4) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "Cannot export HDF4 object.");
            return;
        }

        boolean isN3 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3));
        if (isN3) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "Cannot export netCDF3 object.");
            return;
        }

        try {
            selectedObject.getFileFormat().exportDataset(filename, dataset, binaryOrder);
            viewer.showStatus("Data saved to: " + filename);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "Unable to export dataset: " + ex.getMessage());
        }
    }

    /** enable/disable GUI components */
    private static void setEnabled(List<MenuItem> list, boolean b) {
        if (list == null)
            return;

        Iterator<MenuItem> it = list.iterator();
        while (it.hasNext())
            it.next().setEnabled(b);
    }

    /**
     * Opens a file and retrieves the file structure of the file. It also can be
     * used to create a new file by setting the accessID to FileFormat.CREATE.
     *
     * Subclasses must implement this function to take appropriate steps to open
     * a file.
     *
     * @param filename
     *            the name of the file to open.
     * @param accessID
     *            identifier for the file access. Valid value of accessID is:
     *            <ul>
     *            <li>FileFormat.READ --- allow read-only access to file.</li>
     *            <li>FileFormat.WRITE --- allow read and write access to file.</li>
     *            <li>FileFormat.CREATE --- create a new file.</li>
     *            </ul>
     *
     * @return the FileFormat of this file if successful; otherwise returns null.
     *
     * @throws Exception if a failure occurred
     */
    @Override
    public FileFormat openFile(String filename, int accessID) throws Exception {
        log.trace("openFile: {},{}", filename, accessID);
        FileFormat fileFormat = null;
        boolean isSWMRFile = (FileFormat.MULTIREAD == (accessID & FileFormat.MULTIREAD));
        log.trace("openFile: isSWMRFile={}", isSWMRFile);
        boolean isNewFile = (FileFormat.OPEN_NEW == (accessID & FileFormat.OPEN_NEW));
        if (isNewFile)
            accessID = accessID - FileFormat.OPEN_NEW; //strip OPEN_NEW

        if (isFileOpen(filename)) {
            viewer.showStatus("File is in use.");
            return null;
        }

        File tmpFile = new File(filename);
        if (!tmpFile.exists())
            throw new FileNotFoundException("File does not exist.");

        if (!tmpFile.canWrite() && !isSWMRFile)
            accessID = FileFormat.READ;

        Enumeration<?> keys = FileFormat.getFileFormatKeys();

        String theKey = null;
        while (keys.hasMoreElements()) {
            theKey = (String) keys.nextElement();
            if (theKey.equals(FileFormat.FILE_TYPE_HDF4)) {
                log.trace("openFile: {} FILE_TYPE_HDF4", filename);
                try {
                    FileFormat h4format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
                    if ((h4format != null) && h4format.isThisType(filename)) {
                        fileFormat = h4format.createInstance(filename, accessID);
                        break;
                    }
                }
                catch (UnsatisfiedLinkError e) {
                    log.debug("openFile({}): HDF4 library link error:", filename, e);
                    viewer.showError("Unable to open file '" + filename + "': HDF4 library linking error");
                }
                catch (Exception err) {
                    log.debug("openFile: Error retrieving the file structure of {}:", filename, err);
                }
                continue;
            }
            else if (theKey.equals(FileFormat.FILE_TYPE_HDF5)) {
                log.trace("openFile: {} FILE_TYPE_HDF5", filename);
                try {
                    FileFormat h5format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
                    if ((h5format != null) && h5format.isThisType(filename)) {
                        fileFormat = h5format.createInstance(filename, accessID);
                        break;
                    }
                }
                catch (UnsatisfiedLinkError e) {
                    log.debug("openFile({}): HDF5 library link error:", filename, e);
                    viewer.showError("Unable to open file '" + filename + "': HDF5 library linking error");
                }
                catch (Exception err) {
                    log.debug("openFile: Error retrieving the file structure of {}:", filename, err);
                }
                continue;
            }
            else if (theKey.equals(FileFormat.FILE_TYPE_NC3)) {
                log.trace("openFile: {} FILE_TYPE_NC3", filename);
                try {
                    FileFormat nc3format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3);
                    if ((nc3format != null) && nc3format.isThisType(filename)) {
                        fileFormat = nc3format.createInstance(filename, accessID);
                        break;
                    }
                }
                catch (UnsatisfiedLinkError e) {
                    log.debug("openFile({}): NetCDF3 library link error:", filename, e);
                    viewer.showError("Unable to open file '" + filename + "': NetCDF3 library linking error");
                }
                catch (Exception err) {
                    log.debug("openFile: Error retrieving the file structure of {}:", filename, err);
                }
                continue;
            }
            else {
                log.trace("openFile: {} Other", filename);
                try {
                    FileFormat theformat = FileFormat.getFileFormat(theKey);
                    if (theformat.isThisType(filename)) {
                        fileFormat = theformat.createInstance(filename, accessID);
                        break;
                    }
                }
                catch (Exception err) {
                    log.debug("openFile: Error retrieving the file structure of {}:", filename, err);
                }
            }
        }

        if (fileFormat == null)
            throw new java.io.IOException("Unsupported fileformat - " + filename);

        if (fileFormat.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
            if (tempIdxType >= 0) {
                fileFormat.setIndexType(tempIdxType);

                // Reset the temporary index type
                tempIdxType = -1;
            }
            else
                fileFormat.setIndexType(fileFormat.getIndexType(ViewProperties.getIndexType()));

            if (tempIdxOrder >= 0) {
                fileFormat.setIndexOrder(tempIdxOrder);

                // Reset the temporary index order
                tempIdxOrder = -1;
            }
            else
                fileFormat.setIndexOrder(fileFormat.getIndexOrder(ViewProperties.getIndexOrder()));
        }

        return initFile(fileFormat);
    }

    /**
     * Initializes a FileFormat object by opening it and populating the file tree structure.
     *
     * @param fileFormat
     *            the file to open with an existing FileFormat instance.
     *
     * @return the initialized FileFormat of this file if successful; otherwise returns null.
     *
     * @throws Exception
     *             if a failure occurred
     */
    private FileFormat initFile(FileFormat fileFormat) throws Exception {
        log.trace("initFile[{}] - start", fileFormat.getAbsolutePath());

        TreeItem fileRoot = null;

        shell.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));

        try {
            fileFormat.setMaxMembers(ViewProperties.getMaxMembers());
            fileFormat.setStartMembers(ViewProperties.getStartMembers());

            fileFormat.open();

            fileRoot = populateTree(fileFormat);

            if (fileRoot != null) {
                /* Expand top level items of root object */
                int currentRowCount = tree.getItemCount();
                if (currentRowCount > 0)
                    tree.getItem(currentRowCount - 1).setExpanded(true);

                fileList.add(fileFormat);
            }

            tree.setItemCount(fileList.size());

            log.trace("initFile[{}] - fileList items={}", fileFormat.getAbsolutePath(), fileList.size());
        }
        catch (Exception ex) {
            log.debug("initFile: FileFormat init error:", ex);
            fileFormat = null;
        }
        finally {
            shell.setCursor(null);
        }

        return fileFormat;
    }

    @Override
    public FileFormat reopenFile(FileFormat fileFormat, int newFileAccessMode) throws Exception {
        String fileFormatName = fileFormat.getAbsolutePath();

        // Make sure to reload the file using the file's current indexing options
        tempIdxType = fileFormat.getIndexType(null);
        tempIdxOrder = fileFormat.getIndexOrder(null);

        closeFile(fileFormat);
        ((HDFView) viewer).showMetaData(null);

        if (newFileAccessMode < 0) {
            if (ViewProperties.isReadOnly())
                return openFile(fileFormatName, FileFormat.READ);
            else if (ViewProperties.isReadSWMR())
                return openFile(fileFormatName, FileFormat.READ | FileFormat.MULTIREAD);
            else
                return openFile(fileFormatName, FileFormat.WRITE);
        }
        else
            return openFile(fileFormatName, newFileAccessMode);
    }

    /**
     * Close a file
     *
     * @param file
     *            the file to close
     *
     * @throws Exception if a failure occurred
     */
    @Override
    public void closeFile(FileFormat file) throws Exception {
        if (file == null) return;

        // Find the file item in the tree and remove it
        FileFormat theFile = null;
        TreeItem[] openFiles = tree.getItems(); // Returns the top-level items of the tree

        for (int i = 0; i < openFiles.length; i++) {
            theFile = ((Group) openFiles[i].getData()).getFileFormat();

            if (theFile.equals(file)) {
                // Remove TreeItem from the view
                openFiles[i].dispose();
                log.trace("dispose({}):", theFile.getFilePath());

                try {
                    theFile.close();
                }
                catch (Exception ex) {
                    log.debug("closeFile({}):", theFile.getFilePath(), ex);
                }

                fileList.remove(theFile);
                if (theFile.equals(selectedFile)) {
                    selectedFile = null;
                    selectedObject = null;
                    selectedItem = null;
                }

                break;
            }
        }
    }

    /**
     * Save a file
     *
     * @param file
     *            the file to save
     *
     * @throws Exception if a failure occurred
     */
    @Override
    public void saveFile(FileFormat file) throws Exception {
        if (file == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "Select a file to save.");
            return;
        }

        boolean isH4 = file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        boolean isH5 = file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        if (!(isH4 || isH5)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", "Saving file is not supported for this file type");
            return;
        }

        // Write the change of the data into the file before saving the file
        ((HDFView) viewer).writeDataToFile(file);

        if (isH5)
            saveAsHDF5(file);
        else if (isH4)
            saveAsHDF4(file);
    }

    /**
     * Returns the tree item that contains the given data object.
     */
    @Override
    public TreeItem findTreeItem(HObject obj) {
        if (obj == null)
            return null;

        if (obj.getFileFormat().getRootObject() == null)
            return null;

        // Locate the item's file root to save on search time in large files
        TreeItem[] fileRoots = tree.getItems();
        TreeItem rootItem = null;
        HObject rootObject = null;
        for (int i = 0; i < fileRoots.length; i++) {
            rootItem = fileRoots[i];
            rootObject = (HObject) rootItem.getData();

            if (rootObject == null)
                continue;

            if (rootObject.getFileFormat().equals(obj.getFileFormat())) {
                // If the object being looked for is a file root, return
                // this found TreeItem
                if (obj instanceof Group && ((Group) obj).isRoot())
                    return rootItem;

                // Else the file root for the object being looked for has
                // been found, continue the search through only this TreeItem's
                // members
                break;
            }
        }

        TreeItem theItem = null;
        HObject theObj = null;
        List<TreeItem> breadthFirstItems = getItemsBreadthFirst(rootItem);

        if (breadthFirstItems != null) {
            Iterator<TreeItem> it = getItemsBreadthFirst(rootItem).iterator();

            while (it.hasNext()) {
                theItem = it.next();
                theObj = (HObject) theItem.getData();

                if (theObj == null)
                    continue;

                if (theObj.equals(obj))
                    return theItem;
            }
        }

        return null;
    }

    /**
     * change the display option.
     */
    @Override
    public void setDefaultDisplayMode(boolean displaymode) {
        isDefaultDisplay = displaymode;
    }

    /**
     * Gets the selected file. When multiple files are open, we need to know
     * which file is currently selected.
     *
     * @return the FileFormat of the currently selected file.
     */
    @Override
    public FileFormat getSelectedFile() {
        return selectedFile;
    }

    /**
     * @return the currently selected object in the tree.
     */
    @Override
    public HObject getCurrentObject() {
        return selectedObject;
    }

    /**
     * @return the Tree which holds the file structure.
     */
    @Override
    public Tree getTree() {
        return tree;
    }

    /**
     * @return the list of currently open files.
     */
    @Override
    public List<FileFormat> getCurrentFiles() {
        return fileList;
    }

    /**
     * Display the content of a data object.
     *
     * @param dataObject
     *            the data object
     *
     * @return the DataView that displays the data content
     *
     * @throws Exception if a failure occurred
     */
    @Override
    public DataView showDataContent(HObject dataObject) throws Exception {
        /* Can only display objects with data */
        if ((dataObject == null) || !(dataObject instanceof DataFormat))
            return null;

        log.trace("showDataContent({}): start", dataObject.getName());

        /* Set up the default display properties passed to the DataView instance */
        DataView theView = null;
        DataFormat d = (DataFormat) dataObject;
        HashMap<DATA_VIEW_KEY, Serializable> map = new HashMap<>(8);

        if (!d.isInited())
            d.init();

        boolean isImage = ((d instanceof ScalarDS) && ((ScalarDS) d).isImage());
        boolean isDisplayTypeChar = false;
        boolean isTransposed = false;
        boolean isIndexBase1 = ViewProperties.isIndexBase1();
        BitSet bitmask = null;
        String dataViewName = null;

        if (isDefaultDisplay) { /* Displaying a data object using the default display options */
            DataView existingView = viewer.getDataView((HObject) d);

            /*
             * Check to make sure this data object isn't already opened in an existing
             * DataView. If it is, just bring that DataView to focus.
             */
            if (existingView != null) {
                Shell[] shells = Display.getDefault().getShells();

                if (shells.length >= 1) {
                    for (int i = 0; i < shells.length; i++) {
                        DataView view = (DataView) shells[i].getData();

                        if (view != null) {
                            if (view.equals(existingView)) {
                                shells[i].forceActive();

                                log.trace("showDataContent(): found existing DataView for data object {}", dataObject.getName());

                                return view;
                            }
                        }
                    }
                }
            }
        }
        else { /* Open Dialog to allow user to choose different data display options */
            DataOptionDialog dialog = new DataOptionDialog(shell, d);
            dialog.open();

            if (dialog.isCancelled())
                return null;

            isImage = dialog.isImageDisplay();
            isDisplayTypeChar = dialog.isDisplayTypeChar();
            dataViewName = dialog.getDataViewName();
            isTransposed = dialog.isTransposed();
            bitmask = dialog.getBitmask();
            isIndexBase1 = dialog.isIndexBase1();
            isApplyBitmaskOnly = dialog.isApplyBitmaskOnly();
        }

        map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, dataObject);
        map.put(ViewProperties.DATA_VIEW_KEY.VIEW_NAME, dataViewName);
        map.put(ViewProperties.DATA_VIEW_KEY.CHAR, isDisplayTypeChar);
        map.put(ViewProperties.DATA_VIEW_KEY.TRANSPOSED, isTransposed);
        map.put(ViewProperties.DATA_VIEW_KEY.INDEXBASE1, isIndexBase1);
        map.put(ViewProperties.DATA_VIEW_KEY.BITMASK, bitmask);
        if (isApplyBitmaskOnly)
            map.put(ViewProperties.DATA_VIEW_KEY.BITMASKOP, ViewProperties.BITMASK_OP.AND);

        log.trace(
                "showDataContent(): object={} dataViewName={} isDisplayTypeChar={} isTransposed={} isIndexBase1={} bitmask={}",
                dataObject, dataViewName, isDisplayTypeChar, isTransposed, isIndexBase1, bitmask);

        shell.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));

        if (isImage) {
            DataViewFactory imageViewFactory = null;
            try {
                imageViewFactory = DataViewFactoryProducer.getFactory(DataViewType.IMAGE);
            }
            catch (Exception ex) {
                log.debug("showDataContent(): error occurred while instantiating ImageView factory class", ex);
                viewer.showError("Error occurred while instantiating ImageView factory class");
                return null;
            }

            if (imageViewFactory == null) {
                log.debug("showDataContent(): ImageView factory is null");
                return null;
            }

            try {
                theView = imageViewFactory.getImageView(viewer, map);

                if (theView == null) {
                    log.debug("showDataContent(): error occurred while instantiating ImageView class");
                    viewer.showError("Error occurred while instantiating ImageView class");
                    Tools.showError(shell, "Show Data", "Error occurred while instantiating ImageView class");
                }
            }
            catch (ClassNotFoundException ex) {
                log.debug("showDataContent(): no suitable ImageView class found");
                viewer.showError("Unable to find suitable ImageView class for object '" + dataObject.getName() + "'");
                Tools.showError(shell, "Show Data", "Unable to find suitable ImageView class for object '" + dataObject.getName() + "'");
                theView = null;
            }
        }
        else {
            DataViewFactory tableViewFactory = null;
            try {
                tableViewFactory = DataViewFactoryProducer.getFactory(DataViewType.TABLE);
            }
            catch (Exception ex) {
                log.debug("showDataContent(): error occurred while instantiating TableView factory class", ex);
                viewer.showError("Error occurred while instantiating TableView factory class");
                return null;
            }

            if (tableViewFactory == null) {
                log.debug("showDataContent(): TableView factory is null");
                return null;
            }

            try {
                theView = tableViewFactory.getTableView(viewer, map);

                if (theView == null) {
                    log.debug("showDataContent(): error occurred while instantiating TableView class");
                    viewer.showError("Error occurred while instantiating TableView class");
                    Tools.showError(shell, "Show Data", "Error occurred while instantiating TableView class");
                }
            }
            catch (ClassNotFoundException ex) {
                log.debug("showDataContent(): no suitable TableView class found");
                viewer.showError("Unable to find suitable TableView class for object '" + dataObject.getName() + "'");
                Tools.showError(shell, "Show Data", "Unable to find suitable TableView class for object '" + dataObject.getName() + "'");
                theView = null;
            }
        }

        if (!shell.isDisposed())
            shell.setCursor(null);

        return theView;
    }

    /**
     * Updates the current font.
     *
     * @param font
     *           the new font
     */
    public void updateFont(Font font) {
        if (curFont != null)
            curFont.dispose();

        log.trace("updateFont():");
        curFont = font;

        tree.setFont(font);
        tree.pack();
        tree.requestLayout();
    }

    /**
     * Updates the icon for the TreeItem representing the given HObject. Used
     * to change the icon after a status update, such as adding an attribute to
     * an object.
     *
     * @param obj
     *           the object to update the icon for
     */
    public void updateItemIcon(HObject obj) {
        if (obj == null) {
            log.debug("updateItemIcon(): object is null");
            return;
        }

        TreeItem theItem = findTreeItem(obj);

        if (theItem == null) {
            log.debug("updateItemIcon(): could not find TreeItem for HObject");
        }
        else {
            if (obj instanceof Group && !(((Group) obj).isRoot())) {
                if (theItem.getExpanded()) {
                    if (((MetaDataContainer) obj).hasAttribute())
                        theItem.setImage(folderOpenIconA);
                    else
                        theItem.setImage(folderOpenIcon);
                }
                else {
                    if (((MetaDataContainer) obj).hasAttribute())
                        theItem.setImage(folderCloseIconA);
                    else
                        theItem.setImage(folderCloseIcon);
                }
            }
            else {
                theItem.setImage(getObjectTypeImage(obj));
            }
        }
    }

    /**
     * ChangeIndexingDialog displays file index options.
     */
    private class ChangeIndexingDialog extends Dialog
    {
        private Button checkIndexByName;
        private Button checkIndexIncrements;
        private Button checkIndexNative;

        private boolean reloadFile;

        private FileFormat selectedFile;
        private int indexType;
        private int indexOrder;

        private ChangeIndexingDialog(Shell parent, int style, FileFormat viewSelectedFile) {
            super(parent, style);

            selectedFile = viewSelectedFile;
            try {
                indexType = selectedFile.getIndexType(null);
            }
            catch (Exception ex) {
                log.debug("ChangeIndexingDialog(): getIndexType failed: ", ex);
            }
            try {
                indexOrder = selectedFile.getIndexOrder(null);
            }
            catch (Exception ex) {
                log.debug("ChangeIndexingDialog(): getIndexOrder failed: ", ex);
            }
            reloadFile = false;
        }

        private void setIndexOptions() {
            try {
                if (checkIndexByName.getSelection())
                    indexType = selectedFile.getIndexType("H5_INDEX_NAME");
                else
                    indexType = selectedFile.getIndexType("H5_INDEX_CRT_ORDER");
            }
            catch (Exception ex) {
                log.debug("setIndexOptions(): getIndexType failed: ", ex);
            }

            try {
                if (checkIndexIncrements.getSelection())
                    indexOrder = selectedFile.getIndexOrder("H5_ITER_INC");
                else if (checkIndexNative.getSelection())
                    indexOrder = selectedFile.getIndexOrder("H5_ITER_NATIVE");
                else
                    indexOrder = selectedFile.getIndexOrder("H5_ITER_DEC");
            }
            catch (Exception ex) {
                log.debug("setIndexOptions(): getIndexOrder failed: ", ex);
            }

            reloadFile = true;
        }

        /** @return the current value of the index type. */
        public int getIndexType() {
            return indexType;
        }

        /** @return the current value of the index order. */
        public int getIndexOrder() {
            return indexOrder;
        }

        /** @return the current value of the reloadFile. */
        public boolean isReloadFile() {
            return reloadFile;
        }

        /** open the ChangeIndexingDialog for setting the indexing values. */
        public void open() {
            Shell parent = getParent();
            final Shell openShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
            openShell.setFont(curFont);
            openShell.setText("Indexing options");
            openShell.setImages(ViewProperties.getHdfIcons());
            openShell.setLayout(new GridLayout(1, true));

            // Create main content region
            Composite content = new Composite(openShell, SWT.NONE);
            content.setLayout(new GridLayout(1, true));
            content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            org.eclipse.swt.widgets.Group indexingTypeGroup = new org.eclipse.swt.widgets.Group(content, SWT.NONE);
            indexingTypeGroup.setFont(curFont);
            indexingTypeGroup.setText("Indexing Type");
            indexingTypeGroup.setLayout(new GridLayout(2, true));
            indexingTypeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            int initIndexType = 0;
            try {
                initIndexType = selectedFile.getIndexType("H5_INDEX_NAME");
            }
            catch (Exception ex) {
                log.debug("open(): getIndexType failed: ", ex);
            }
            checkIndexByName = new Button(indexingTypeGroup, SWT.RADIO);
            checkIndexByName.setFont(curFont);
            checkIndexByName.setText("By Name");
            checkIndexByName.setSelection((indexType) == initIndexType);
            checkIndexByName.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));

            try {
                initIndexType = selectedFile.getIndexType("H5_INDEX_CRT_ORDER");
            }
            catch (Exception ex) {
                log.debug("open(): getIndexType failed: ", ex);
            }
            Button byOrder = new Button(indexingTypeGroup, SWT.RADIO);
            byOrder.setFont(curFont);
            byOrder.setText("By Creation Order");
            byOrder.setSelection((indexType) == initIndexType);
            byOrder.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));

            org.eclipse.swt.widgets.Group indexingOrderGroup = new org.eclipse.swt.widgets.Group(content, SWT.NONE);
            indexingOrderGroup.setFont(curFont);
            indexingOrderGroup.setText("Indexing Order");
            indexingOrderGroup.setLayout(new GridLayout(3, true));
            indexingOrderGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            int initIndexOrder = 0;
            try {
                initIndexOrder = selectedFile.getIndexOrder("H5_ITER_INC");
            }
            catch (Exception ex) {
                log.debug("open(): getIndexOrder failed: ", ex);
            }
            checkIndexIncrements = new Button(indexingOrderGroup, SWT.RADIO);
            checkIndexIncrements.setFont(curFont);
            checkIndexIncrements.setText("Increments");
            checkIndexIncrements.setSelection((indexOrder) == initIndexOrder);
            checkIndexIncrements.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));

            try {
                initIndexOrder = selectedFile.getIndexOrder("H5_ITER_DEC");
            }
            catch (Exception ex) {
                log.debug("open(): getIndexOrder failed: ", ex);
            }
            Button decrements = new Button(indexingOrderGroup, SWT.RADIO);
            decrements.setFont(curFont);
            decrements.setText("Decrements");
            decrements.setSelection((indexOrder) == initIndexOrder);
            decrements.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));

            try {
                initIndexOrder = selectedFile.getIndexOrder("H5_ITER_NATIVE");
            }
            catch (Exception ex) {
                log.debug("open(): getIndexOrder failed: ", ex);
            }
            checkIndexNative = new Button(indexingOrderGroup, SWT.RADIO);
            checkIndexNative.setFont(curFont);
            checkIndexNative.setText("Native");
            checkIndexNative.setSelection((indexOrder) == initIndexOrder);
            checkIndexNative.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));


            // Create Ok/Cancel button region
            Composite buttonComposite = new Composite(openShell, SWT.NONE);
            buttonComposite.setLayout(new GridLayout(2, true));
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &Reload File   ");
            okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setIndexOptions();
                    openShell.dispose();
                }
            });

            Button cancelButton = new Button(buttonComposite, SWT.PUSH);
            cancelButton.setFont(curFont);
            cancelButton.setText("     &Cancel     ");
            cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            cancelButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    reloadFile = false;
                    openShell.dispose();
                }
            });

            openShell.pack();

            Point minimumSize = openShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

            openShell.setMinimumSize(minimumSize);

            Rectangle parentBounds = parent.getBounds();
            Point shellSize = openShell.getSize();
            openShell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                    (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            openShell.open();

            Display display = parent.getDisplay();
            while (!openShell.isDisposed())
                if (!display.readAndDispatch())
                    display.sleep();
        }
    }

    private class ChangeLibVersionDialog extends Dialog
    {
        public ChangeLibVersionDialog(Shell parent, int style) {
            super(parent, style);
        }

        public void open() {
            Shell parent = getParent();
            final Shell openShell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
            openShell.setFont(curFont);
            openShell.setText("Set the library version bounds: ");
            openShell.setImages(ViewProperties.getHdfIcons());
            openShell.setLayout(new GridLayout(1, true));

            String[] lowValues = { "Earliest", "V18", "V110", "V112", "V114", "V116", "Latest" };
            int[] lowConstants = { HDF5Constants.H5F_LIBVER_EARLIEST, HDF5Constants.H5F_LIBVER_V18,
                    HDF5Constants.H5F_LIBVER_V110, HDF5Constants.H5F_LIBVER_V112, HDF5Constants.H5F_LIBVER_V114,
                    HDF5Constants.H5F_LIBVER_V116, HDF5Constants.H5F_LIBVER_LATEST };
            String[] highValues = { "V18", "V110", "V112", "V114", "V116", "Latest" };
            int[] highConstants = { HDF5Constants.H5F_LIBVER_V18, HDF5Constants.H5F_LIBVER_V110,
                    HDF5Constants.H5F_LIBVER_V112, HDF5Constants.H5F_LIBVER_V114, HDF5Constants.H5F_LIBVER_V116,
                    HDF5Constants.H5F_LIBVER_LATEST };

            // Try to retrieve the existing version bounds
            int[] current = null;
            try {
                current = selectedObject.getFileFormat().getLibBounds();
            }
            catch (Exception err) {
                openShell.getDisplay().beep();
                Tools.showError(openShell, "Version bounds", "Error when getting lib version bounds, using default");
                current = new int[]{HDF5Constants.H5F_LIBVER_EARLIEST, HDF5Constants.H5F_LIBVER_LATEST};
            }
            int lowidx = 0;
            for(int i = 0; i < lowConstants.length; i++) {
                if(lowConstants[i] == current[0]){
                    lowidx = i;
                    break;
                }
            }
            int highidx = 0;
            for(int i = 0; i < highConstants.length; i++) {
                if(highConstants[i] == current[1]){
                    highidx = i;
                    break;
                }
            }

            // Dummy label
            new Label(openShell, SWT.LEFT);

            Label label = new Label(openShell, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Earliest Version: ");

            final Combo earliestCombo = new Combo(openShell, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
            earliestCombo.setFont(curFont);
            earliestCombo.setItems(lowValues);
            earliestCombo.select(lowidx);
            earliestCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            label = new Label(openShell, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Latest Version: ");

            final Combo latestCombo = new Combo(openShell, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
            latestCombo.setFont(curFont);
            latestCombo.setItems(highValues);
            latestCombo.select(highidx);
            latestCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            // Dummy label to consume remain space after resizing
            new Label(openShell, SWT.LEFT).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            // Create Ok/Cancel button region
            Composite buttonComposite = new Composite(openShell, SWT.NONE);
            buttonComposite.setLayout(new GridLayout(2, true));
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        selectedObject.getFileFormat().setLibBounds(earliestCombo.getItem(earliestCombo.getSelectionIndex()), latestCombo.getItem(latestCombo.getSelectionIndex()));
                    }
                    catch (Exception err) {
                        openShell.getDisplay().beep();
                        Tools.showError(openShell, "Version bounds", "Error when setting lib version bounds");
                        return;
                    }

                    openShell.dispose();

                    ((HDFView) viewer).showMetaData(selectedObject);
                }
            });

            Button cancelButton = new Button(buttonComposite, SWT.PUSH);
            cancelButton.setFont(curFont);
            cancelButton.setText(" &Cancel ");
            cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            cancelButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    openShell.dispose();
                }
            });

            openShell.pack();

            Point minimumSize = openShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

            openShell.setMinimumSize(minimumSize);
            openShell.setSize(minimumSize.x + 50, minimumSize.y);

            Rectangle parentBounds = parent.getBounds();
            Point shellSize = openShell.getSize();
            openShell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                    (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            openShell.open();

            Display display = parent.getDisplay();
            while (!openShell.isDisposed())
                if (!display.readAndDispatch())
                    display.sleep();
        }
    }

    private class LoadDataThread extends Thread
    {
        LoadDataThread() {
            super();
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            showDataContent(selectedObject);
                        }
                        catch (Exception ex) {
                            log.debug("showDataContent failure: ", ex);
                        }
                    }
                });
            }
            catch (Exception e) {
                log.debug("showDataContent loading manually interrupted");
            }
        }
    }
}
