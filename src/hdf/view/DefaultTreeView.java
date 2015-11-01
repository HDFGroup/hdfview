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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.DefaultTreeViewOld.ChangeIndexingDialog;
import hdf.view.ViewProperties.DATA_VIEW_KEY;

/**
 * <p>
 * TreeView defines APIs for opening files and displaying the file structure in
 * a tree structure.
 * </p>
 * 
 * <p>
 * TreeView uses folders and leaf items to represent groups and data objects in
 * the file. You can expand or collapse folders to navigate data objects in the
 * file.
 * </p>
 * 
 * <p>
 * From the TreeView, you can open data content or metadata of the selected object.
 * You can select object(s) to delete or add new objects to the file.
 * </p>
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DefaultTreeView implements TreeView {
    private static final long             serialVersionUID    = 4092566164712521186L;
    
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultTreeViewOld.class);
    
    private Shell                         shell;

    /** The owner of this TreeView */
    private ViewManager                   viewer;
    
    /**
     * The tree which holds file structures.
     */
    private final Tree                    tree;

    /**
     * The super root of the tree: all open files start at this root.
     */
    private final TreeItem                root = null;

    /** The currently selected tree item */
    private TreeItem                      selectedItem = null;
    
    /** The list of current selected objects for copying */
    private TreeItem[]                    objectsToCopy = null;

    /** The currently selected object */
    private HObject                       selectedObject;
    
    /** The currently selected file */
    private FileFormat                    selectedFile;
    
    /** A list of currently open files */
    private final List<FileFormat>        fileList = new Vector<FileFormat>();

    /** A list of editing GUI components */
    private List<MenuItem>                editGUIs = new Vector<MenuItem>();
    
    /**
     * The popup menu used to display user choice of actions on data object.
     */
    private final Menu                    popupMenu;

    private Menu                          newObjectMenu;
    private Menu                          exportDatasetMenu;

    private MenuItem                      separator;
    private MenuItem                      addDatasetMenuItem;
    private MenuItem                      addTableMenuItem;
    private MenuItem                      addDatatypeMenuItem;
    private MenuItem                      addLinkMenuItem;
    private MenuItem                      setLibVerBoundsItem;
    private MenuItem                      changeIndexItem;

    /** Flag to indicate if the dataset is displayed as default */
    private boolean                       isDefaultDisplay = true;

    /** Flag to indicate if TreeItems are being moved */
    private boolean                       moveFlag = false;

    /** Flag to indicate if */
    private boolean                       isApplyBitmaskOnly  = false;

    private int                           currentIndexType;

    private int                           currentIndexOrder;

    private int                           binaryOrder;

    private String                        currentSearchPhrase = null;
    
    private enum OBJECT_TYPE {GROUP, DATASET, IMAGE, TABLE, DATATYPE, LINK};
    
    public DefaultTreeView(ViewManager theView, Composite parent) {
        viewer = theView;
        
        // Initialize the Tree
        tree = new Tree(parent, SWT.MULTI | SWT.VIRTUAL);
        tree.setSize(tree.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        
        // Create the context menu for the Tree
        popupMenu = createPopupMenu();
        
        //treeModel = new DefaultTreeModel(root);
        //tree.setLargeModel(true);
        //tree.setCellRenderer(new HTreeCellRenderer());
        //tree.setRootVisible(false);
        //tree.setShowsRootHandles(true);
        //int rowheight = 23 + (int) ((tree.getFont().getSize() - 12) * 0.5);
        //tree.setRowHeight(rowheight); 
        
        // Handle tree key events
        tree.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int key = e.keyCode;
                
                if (key == SWT.ARROW_LEFT || key == SWT.ARROW_RIGHT || key == SWT.ARROW_DOWN || key == SWT.ARROW_UP ||
                    key == SWT.KEYPAD_4 || key == SWT.KEYPAD_6 || key == SWT.KEYPAD_2 || key == SWT.KEYPAD_8) {
                    
                    //TreePath selPath = ((JTree) e.getComponent()).getSelectionPath();
                    //if (selPath == null) return;
                    
                    //TreeItem theItem = (TreeItem) selPath.getLastPathComponent();

                    //if (!theItem.equals(selectedItem)) {
                    //    selectedTreePath = selPath;
                    //    selectedItem = theItem;
                    //    selectedObject = ((HObject) (selectedItem.getData()));
                    //    FileFormat theFile = selectedObject.getFileFormat();
                    //    if ((theFile != null) && !theFile.equals(selectedFile)) {
                            // A different file is selected, handle only one file at a time
                    //        selectedFile = theFile;
                    //      tree.clearSelection();
                    //        tree.setSelectionPath(selPath);
                    //    }

                    //    ((HDFView) viewer).showMetaData(selectedObject);
                    //}
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
            public void mouseDoubleClick(MouseEvent e) {
                isDefaultDisplay = true;
                
                try {
                    showDataContent(selectedObject);
                }
                catch (Exception ex) {
                }
            }
            
            // When a mouse release is detected, attempt to set the selected item
            // and object to the TreeItem under the pointer
            public void mouseUp(MouseEvent e) {
                // Make sure user clicked on a TreeItem
                TreeItem theItem = tree.getItem(new Point(e.x, e.y));
                
                if (theItem == null) {
                    tree.deselectAll();
                    selectedItem = null;
                    selectedObject = null;
                    selectedFile = null;
                    return;
                }
                
                //TreePath selPath = tree.getPathForLocation(e.x, e.y);
                //if (selPath == null) return;
                
                if (!theItem.equals(selectedItem)) {
                    FileFormat theFile = null;
                    
                    //selectedTreePath = selPath;
                    selectedItem = theItem;
                    
                    try {
                        selectedObject = (HObject) selectedItem.getData();
                    }
                    catch(NullPointerException ex) {
                        System.err.println("TreeItem " + selectedItem.getText() + " had no associated data.");
                        return;
                    }
                    
                    try {
                        theFile = selectedObject.getFileFormat();
                    }
                    catch(NullPointerException ex) {
                        System.err.println("Error retrieving FileFormat of HObject " + selectedObject.getName() + ".");
                        return;
                    }
                    
                    if ((theFile != null) && !theFile.equals(selectedFile)) {
                        // A different file is selected, handle only one file at a time
                        selectedFile = theFile;
                        //tree.deselectAll();
                        //tree.setSelection(selPath);
                    }
                    
                    //viewer.mouseEventFired(e);
                }
            }
        });
        
        // Show context menu only if user has selected a data object
        tree.addListener(SWT.MenuDetect, new Listener() {
            public void handleEvent(Event e) {
                if (selectedItem == null | selectedObject == null | selectedFile == null) return;
                
                Point pt = new Point(e.x, e.y);
                popupMenu.setLocation(pt);
                popupMenu.setVisible(true);
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
            public void widgetSelected(SelectionEvent e) {
                isDefaultDisplay = true;
                
                try {
                    showDataContent(selectedObject);
                }
                catch (Throwable err) {
                    shell.getDisplay().beep();
                    showError(err.getMessage(), null);
                    return;
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Open &As");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                isDefaultDisplay = false;
                
                try {
                    showDataContent(selectedObject);
                }
                catch (Throwable err) {
                    shell.getDisplay().beep();
                    showError(err.getMessage(), null);
                    return;
                }
            }
        });
        
        MenuItem newObjectMenuItem = new MenuItem(menu, SWT.CASCADE);
        newObjectMenuItem.setText("New");

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Cu&t");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                moveObject();
            }
        });
        editGUIs.add(item);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Copy");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                copyObject();
            }
        });
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Paste");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                pasteObject();
            }
        });
        editGUIs.add(item);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Delete");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                removeSelectedObjects();
            }
        });
        editGUIs.add(item);

        MenuItem exportDatasetMenuItem = new MenuItem(menu, SWT.CASCADE);
        exportDatasetMenuItem.setText("Export Dataset");
        
        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Save to");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (selectedObject == null) return;
                
                if ((selectedObject instanceof Group) && ((Group) selectedObject).isRoot()) {
                    shell.getDisplay().beep();
                    showError("Cannot save the root group.\nUse \"Save As\" from file menu to save the whole file", null);
                    return;
                }
                
                String filetype = FileFormat.FILE_TYPE_HDF4;
                if (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) 
                    filetype = FileFormat.FILE_TYPE_HDF5;
                
                NewFileDialog dialog = new NewFileDialog((Shell) viewer, selectedObject.getFileFormat().getParent(),
                        filetype, fileList);
                String filename = dialog.open();
                if (!dialog.isFileCreated()) return;
                
                FileFormat dstFile = null;
                
                try {
                    dstFile = openFile(filename, FileFormat.WRITE);
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage() + "\n" + filename, null);
                }
                
                List<Object> objList = new Vector<Object>(2);
                objList.add(selectedObject);
                //pasteObject(objList, dstFile.getRootObject(), dstFile);
            }
        });
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Rename");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                renameObject();
            }
        });
        editGUIs.add(item);
        
        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Properties");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                isDefaultDisplay = true;
                
                try {
                    showMetaData(selectedObject);
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Properties As");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                isDefaultDisplay = false;
                
                try {
                    showMetaData(selectedObject);
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    showError(ex.getMessage(), shell.getText());
                }
            }
        });
        
        changeIndexItem = new MenuItem(menu, SWT.PUSH);
        changeIndexItem.setText("Change file indexing");
        changeIndexItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ChangeIndexingDialog dialog = new ChangeIndexingDialog(shell, SWT.NONE, selectedFile);
                dialog.open();
                if (dialog.isreloadFile()) {
                    selectedFile.setIndexType(dialog.getIndexType());
                    selectedFile.setIndexOrder(dialog.getIndexOrder());
                    ((HDFView) viewer).reloadFile();
                }
            }
        });
        
        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Find");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String findStr = currentSearchPhrase;
                if (findStr == null) findStr = "";
                
                findStr = (new InputDialog(shell, "Find Object by Name", 
                        "Find (e.g. O3Quality, O3*, or *Quality):", findStr)).open();
                
                if (findStr != null && findStr.length() > 0) currentSearchPhrase = findStr;
                
                //find(currentSearchPhrase, selectedTreePath, tree);
            }
        });

        // item = new MenuItem(menu, SWT.PUSH);
        // item.setText("Find Next");
        // item.setMnemonic(KeyEvent.VK_N);
        // item.addActionListener(this);
        // item.addSelectionListener(new SelectionAdapter() {
        //  public void widgetSelected(SelectionEvent e) {
        //      find(currentSearchPhrase, selectedTreePath, tree);
        //  }
        // });
        
        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Expand All");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                //for (int row = 0; row < tree.getRowCount(); row++)
                //  tree.expandRow(row);
            }
        });
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Collapse All");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                //for (int row = tree.getRowCount() - 1; row >= 0; row--)
                //  tree.collapseRow(row);
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Close Fil&e");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    closeFile(getSelectedFile());
                }
                catch (Exception ex) {
                    showError(ex.getMessage(), null);
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("&Reload File");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ((HDFView) viewer).reloadFile();
            }
        });

        separator = new MenuItem(menu, SWT.SEPARATOR);
        
        setLibVerBoundsItem = new MenuItem(menu, SWT.NONE);
        setLibVerBoundsItem.setText("Set Lib version bounds");
        setLibVerBoundsItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setLibVersionBounds();
            }
        });
        
        
        // Add new object menu
        newObjectMenu = new Menu(menu);
        newObjectMenuItem.setMenu(newObjectMenu);
        
        item = new MenuItem(newObjectMenu, SWT.PUSH);
        item.setText("Group");
        item.setImage(ViewProperties.getFoldercloseIcon());
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                addObject(OBJECT_TYPE.GROUP);
            }
        });
        
        addDatasetMenuItem = new MenuItem(newObjectMenu, SWT.PUSH);
        addDatasetMenuItem.setText("Dataset");
        addDatasetMenuItem.setImage(ViewProperties.getDatasetIcon());
        addDatasetMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                addObject(OBJECT_TYPE.DATASET);
            }
        });
        
        item = new MenuItem(newObjectMenu, SWT.PUSH);
        item.setText("Image");
        item.setImage(ViewProperties.getImageIcon());
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                addObject(OBJECT_TYPE.IMAGE);
            }
        });
        
        addTableMenuItem = new MenuItem(newObjectMenu, SWT.PUSH);
        addTableMenuItem.setText("Compound DS");
        addTableMenuItem.setImage(ViewProperties.getTableIcon());
        addTableMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                addObject(OBJECT_TYPE.TABLE);
            }
        });
        
        addDatatypeMenuItem = new MenuItem(newObjectMenu, SWT.PUSH);
        addDatatypeMenuItem.setText("Datatype");
        addDatatypeMenuItem.setImage(ViewProperties.getDatatypeIcon());
        addDatatypeMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                addObject(OBJECT_TYPE.DATATYPE);
            }
        });
        
        addLinkMenuItem = new MenuItem(newObjectMenu, SWT.PUSH);
        addLinkMenuItem.setText("Link");
        addLinkMenuItem.setImage(ViewProperties.getLinkIcon());
        addLinkMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                addObject(OBJECT_TYPE.LINK);
            }
        });
        
        
        // Add export dataset menu
        exportDatasetMenu = new Menu(menu);
        exportDatasetMenuItem.setMenu(exportDatasetMenu);
        
        item = new MenuItem(exportDatasetMenu, SWT.PUSH);
        item.setText("Export Data to Text File");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 99;
                
                try {
                    saveAsFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText("Export Dataset");
                    error.setMessage(ex.getMessage());
                    error.open();
                }
            }
        });
        
        item = new MenuItem(exportDatasetMenu, SWT.PUSH);
        item.setText("Export Data as Native Order");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 1;
                
                try {
                    saveAsFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText("Export Dataset");
                    error.setMessage(ex.getMessage());
                    error.open();
                }
            }
        });
        
        item = new MenuItem(exportDatasetMenu, SWT.PUSH);
        item.setText("Export Data as Little Endian");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 2;
                
                try {
                    saveAsFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText("Export Dataset");
                    error.setMessage(ex.getMessage());
                    error.open();
                }
            }
        });
        
        item = new MenuItem(exportDatasetMenu, SWT.PUSH);
        item.setText("Export Data as Big Endian");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                binaryOrder = 3;
                
                try {
                    saveAsFile();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText("Export Dataset");
                    error.setMessage(ex.getMessage());
                    error.open();
                }
            }
        });
        
        // Add listener to dynamically enable/disable menu items based
        // on selection in tree
        menu.addMenuListener(new MenuAdapter() {
            public void menuShown(MenuEvent e) {
                if (selectedItem == null || selectedObject == null || selectedFile == null) return;
                
                boolean isReadOnly = selectedObject.getFileFormat().isReadOnly();
                boolean isWritable = !isReadOnly;
                
                setEnabled(editGUIs, !isReadOnly);
                
                // Must be a more convenient and general way to re-write this
                if (selectedObject instanceof Group) {
                    popupMenu.getItem(0).setEnabled(false); // "Open" menuitem
                    popupMenu.getItem(1).setEnabled(false); // "Open as" menuitem

                    boolean state = !(((Group) selectedObject).isRoot());
                    popupMenu.getItem(5).setEnabled(state); // "Copy" menuitem
                    popupMenu.getItem(6).setEnabled(isWritable); // "Paste" menuitem
                    popupMenu.getItem(7).setEnabled(state && isWritable); // "Delete" menuitem
                    popupMenu.getItem(11).setEnabled(state); // "Save to" menuitem
                    popupMenu.getItem(12).setEnabled(state && isWritable); // "Rename" menuitem
                    popupMenu.getItem(8).setEnabled(
                            (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))
                           && state && isWritable); // "Cut" menuitem
                }
                else {
                    popupMenu.getItem(0).setEnabled(true); // "Open" menuitem
                    popupMenu.getItem(1).setEnabled(true); // "Open as" menuitem
                    popupMenu.getItem(5).setEnabled(true); // "Copy" menuitem
                    popupMenu.getItem(6).setEnabled(isWritable); // "Paste" menuitem
                    popupMenu.getItem(7).setEnabled(isWritable); // "Delete" menuitem
                    popupMenu.getItem(11).setEnabled(true); // "Save to" menuitem
                    popupMenu.getItem(12).setEnabled(isWritable); // "Rename" menuitem
                    popupMenu.getItem(8).setEnabled(
                            (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))
                            && isWritable); // "Cut" menuitem
                }
                
                // Adding table is only supported by HDF5
                if ((selectedFile != null) && selectedFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                    addDatasetMenuItem.setText("Dataset");
                    //addTableMenuItem.setVisible(true); // Should be moved to createPopupMenu() since swt doesn't support MenuItem.setVisible
                    //addDatatypeMenuItem.setVisible(true); // Should be moved to createPopupMenu() since swt doesn't support MenuItem.setVisible
                    //addLinkMenuItem.setVisible(true); // Should be moved to createPopupMenu() since swt doesn't support MenuItem.setVisible
                    boolean state = false;
                    if ((selectedObject instanceof Group)) {
                        state = (((Group) selectedObject).isRoot());
                        //separator.setVisible(isWritable && state);
                        //setLibVerBoundsItem.setVisible(isWritable && state); 
                        // added only if it is HDF5format, iswritable & isroot
                    }
                    else {
                        // separator.setVisible(false);
                        //setLibVerBoundsItem.setVisible(false);
                    }
                    //changeIndexItem.setVisible(state);
                }
                else {
                    addDatasetMenuItem.setText("SDS");
                    //addTableMenuItem.setVisible(false);
                    //addDatatypeMenuItem.setVisible(false);
                    //addLinkMenuItem.setVisible(false);
                    //separator.setVisible(false);
                    //setLibVerBoundsItem.setVisible(false);
                    //changeIndexItem.setVisible(false);
                }
            
                // Export table is only supported by HDF5
                if ((selectedObject != null) && selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                    if ((selectedObject instanceof Dataset)) {
                        Dataset dataset = (Dataset) selectedObject;
                        if ((dataset instanceof ScalarDS))
                            exportDatasetMenu.setVisible(true);
                    }
                    else {
                        exportDatasetMenu.setVisible(false);
                    }
                }
                else {
                    exportDatasetMenu.setVisible(false);
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
    private void addObject(OBJECT_TYPE type) {
        if ((selectedObject == null) || (selectedItem == null)) return;
        
        Group pGroup = null;
        if(selectedObject instanceof Group) {
            pGroup = (Group) selectedObject;
        }
        else {
            pGroup = (Group) (selectedItem.getParentItem()).getData();
        }
        
        HObject obj = null;
        /*
        switch(type) {
            case GROUP:
                NewGroupDialog groupDialog = new NewGroupDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootObject()));
                groupDialog.setVisible(true);
                obj = (HObject) groupDialog.getObject();
                pGroup = groupDialog.getParentGroup();
                break;
            case DATASET:
                NewDatasetDialog datasetDialog = new NewDatasetDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootObject()));
                datasetDialog.setVisible(true);
                obj = (HObject) datasetDialog.getObject();
                pGroup = datasetDialog.getParentGroup();
                break;
            case IMAGE:
                NewImageDialog imageDialog = new NewImageDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootObject()));
                imageDialog.setVisible(true);
                obj = (HObject) imageDialog.getObject();
                pGroup = imageDialog.getParentGroup();
                break;
            case TABLE:
                NewTableDataDialog tableDialog = new NewTableDataDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootObject()));
                tableDialog.setVisible(true);
                obj = (HObject) tableDialog.getObject();
                pGroup = tableDialog.getParentGroup();
                break;
            case DATATYPE:
                NewDatatypeDialog datatypeDialog = new NewDatatypeDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootObject()));
                datatypeDialog.setVisible(true);
                obj = (HObject) datatypeDialog.getObject();
                pGroup = datatypeDialog.getParentGroup();
                break;
            case LINK:
                NewLinkDialog linkDialog = new NewLinkDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootObject()));
                linkDialog.setVisible(true);
                obj = (HObject) linkDialog.getObject();
                pGroup = linkDialog.getParentGroup();
                break;
        }
        */
        
        if (obj == null) return;
        
        try {
            this.addObject(obj, pGroup);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), null);
            return;
        }
    }
    
    /**
     * Adds a new data object to the file.
     * 
     * @param newObject
     *            the new object to add.
     * @param parentGroup
     *            the parent group to add the object to.
     * @throws Exception
     */
    public void addObject(HObject newObject, Group parentGroup) throws Exception {
        if (newObject == null) return;

        TreeItem pitem = findTreeItem(parentGroup);
        TreeItem newItem = this.insertObject(newObject, pitem);
        
        // When a TreeItem is disposed, it should be removed from its parent
        // items member list
        newItem.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                TreeItem thisItem = (TreeItem) e.getSource();
                TreeItem parentItem = thisItem.getParentItem();
                
                if (parentItem != null) {
                    Group parentGroup = (Group) parentItem.getData();
                    parentGroup.removeFromMemberList((HObject) thisItem.getData());
                }
                
                if (thisItem.equals(selectedItem)) {
                    selectedItem = null;
                    selectedFile = null;
                }
            }
        });
    }

    /**
     * Insert an object into the tree as the last object 
     * under parent item pobj.
     * 
     * @param obj
     *            the object to insert.
     * @param pobj
     *            the parent TreeItem to insert the new object under.
     * @return the newly created TreeItem
     */
    private TreeItem insertObject(HObject obj, TreeItem pobj) {
        if ((obj == null) || (pobj == null)) return null;

        TreeItem item = new TreeItem(pobj, SWT.NONE, pobj.getItemCount());
        item.setData(obj);
        
        return item;
    }
    
    /** Move selected objects */
    private void moveObject() {
        objectsToCopy = tree.getSelection();
        moveFlag = true;
        //currentSelectionsForMove = tree.getSelectionPaths();
    }
    
    /** Copy selected objects */
    private void copyObject() {
        objectsToCopy = tree.getSelection();
        moveFlag = false;
    }
    
    /** Paste selected objects */
    private void pasteObject() {
        log.trace("pasteObject(): start");
        
        if (moveFlag == true) {
            HObject theObj = null;
            //for (int i = 0; i < currentSelectionsForMove.length; i++) {
            //    TreeItem currentItem = (TreeItem) (currentSelectionsForMove[i]
            //            .getLastPathComponent());
            //    theObj = (HObject) currentItem.getData();

            //    if (isObjectOpen(theObj)) {
            //        shell.getDisplay().beep();
            //        showError("Cannot move the selected object: " + theObj
            //                + "\nThe dataset or dataset in the group is in use."
            //                + "\n\nPlease close the dataset(s) and try again.\n");
            //        
            //        moveFlag = false;
            //        currentSelectionsForMove = null;
            //       objectsToCopy = null;
            //        return;
            //    }
            //}
        }

        TreeItem pitem = selectedItem;

        if ((objectsToCopy == null) || (objectsToCopy.length <= 0) || (pitem == null)) return;

        FileFormat srcFile = ((HObject) objectsToCopy[0].getData()).getFileFormat();
        FileFormat dstFile = getSelectedFile();
        FileFormat h5file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
        FileFormat h4file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);

        if (srcFile == null) {
            shell.getDisplay().beep();
            showError("Source file is null.", null);
            return;
        }
        else if (dstFile == null) {
            shell.getDisplay().beep();
            showError("Destination file is null.", null);
            return;
        }
        else if (srcFile.isThisType(h4file) && dstFile.isThisType(h5file)) {
            shell.getDisplay().beep();
            showError("Unsupported operation: cannot copy HDF4 object to HDF5 file", null);
            return;
        }
        else if (srcFile.isThisType(h5file) && dstFile.isThisType(h4file)) {
            shell.getDisplay().beep();
            showError("Unsupported operation: cannot copy HDF5 object to HDF4 file", null);
            return;
        }

        if (moveFlag == true) {
            if (srcFile != dstFile) {
                shell.getDisplay().beep();
                showError("Cannot move the selected object to different file", null);
                moveFlag = false;
                //currentSelectionsForMove = null;
                objectsToCopy = null;
                return;
            }
        }

        if (pitem.getParentItem() != null) {
            pitem = pitem.getParentItem();
        }
        
        Group pgroup = (Group) pitem.getData();
        String fullPath = pgroup.getPath() + pgroup.getName();
        if (pgroup.isRoot()) {
            fullPath = HObject.separator;
        }

        String msg = "";
        if (srcFile.isThisType(h4file)) {
            msg = "WARNING: object can not be deleted after it is copied.\n\n";
        }

        msg += "Do you want to copy the selected object(s) to \nGroup: " + fullPath + "\nFile: "
                + dstFile.getFilePath();

        int op = -1;
        if (moveFlag == true) {
            String moveMsg = "Do you want to move the selected object(s) to \nGroup: " + fullPath + "\nFile: "
                    + dstFile.getFilePath();
            MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            confirm.setText("Move Object");
            confirm.setMessage(moveMsg);
            op = confirm.open();
        }
        else {
            MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            confirm.setText("Copy object");
            confirm.setMessage(msg);
            op = confirm.open();
        }

        log.trace("pasteObject(): op={}", op);
        if (op == SWT.NO) return;

        pasteObject(objectsToCopy, pitem, dstFile);

        if (moveFlag == true) {
            removeSelectedObjects();
            moveFlag = false;
            //currentSelectionsForMove = null;
            objectsToCopy = null;
        }
        
        log.trace("pasteObject(): finish");
    }

    /** Paste selected objects */
    private void pasteObject(TreeItem[] objList, TreeItem pobj, FileFormat dstFile) {
        if ((objList == null) || (objList.length <= 0) || (pobj == null)) return;

        ((HObject) objList[0].getData()).getFileFormat();
        Group pgroup = (Group) pobj.getData();
        log.trace("pasteObject(...): start");

        HObject theObj = null;
        HObject newItem = null;
        for (int i = 0; i < objList.length; i++) {
            newItem = null;
            theObj = (HObject) objList[i].getData();

            if ((theObj instanceof Group) && ((Group) theObj).isRoot()) {
                shell.getDisplay().beep();
                showError("Unsupported operation: cannot copy the root group", null);
                return;
            }

            // Check if it creates infinite loop
            Group pg = pgroup;
            while (!pg.isRoot()) {
                if (theObj.equals(pg)) {
                    shell.getDisplay().beep();
                    showError("Unsupported operation: cannot copy a group to itself.", null);
                    return;
                }
                pg = pg.getParent();
            }

            try {
                log.trace("pasteObject(...): dstFile.copy(theObj, pgroup, null)");
                newItem = dstFile.copy(theObj, pgroup, null);
            }
            catch (Exception ex) {
                shell.getDisplay().beep();
                showError(ex.getMessage(), null);
                newItem = null;
            }

            // Add the node to the tree
            if (newItem != null) insertObject(newItem, pobj);

        } // for (int i = 0; i < objList.length; i++)

        log.trace("pasteObject(...): finish");
    }

    /**
     * Rename the currently selected object.
     */
    private void renameObject() {
        if (selectedObject == null) return;

        if ((selectedObject instanceof Group) && ((Group) selectedObject).isRoot()) {
            shell.getDisplay().beep();
            showError("Cannot rename the root.", null);
            return;
        }

        boolean isH4 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        if (isH4) {
            shell.getDisplay().beep();
            showError("Cannot rename HDF4 object.", null);
            return;
        }

        String oldName = selectedObject.getName();
        String newName = (new InputDialog(shell, "Rename Object", 
                          "Rename \"" + oldName + "\" to:", oldName)).open();

        if (newName == null) return;

        newName = newName.trim();
        if ((newName == null) || (newName.length() == 0) || newName.equals(oldName)) {
            return;
        }

        try {
            selectedObject.setName(newName);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError("Rename Object", ex.getMessage());
        }
    }
    
    private void removeSelectedObjects() {
        FileFormat theFile = getSelectedFile();
        if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
            shell.getDisplay().beep();
            showError("Unsupported operation: cannot delete HDF4 object.", null);
            return;
        }

        //TreePath[] currentSelections = tree.getSelectionPaths();

        //if (moveFlag == true) {
        //    currentSelections = currentSelectionsForMove;
        //}
        //if ((currentSelections == null) || (currentSelections.length <= 0)) {
        //    return;
        //}
        if (moveFlag != true) {
            MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            confirm.setText("Remove object");
            confirm.setMessage("Do you want to remove all the selected object(s) ?");
            if (confirm.open() == SWT.NO) return;
        }
        HObject theObj = null;
        //for (int i = 0; i < currentSelections.length; i++) {
        //    TreeItem currentItem = (TreeItem) (currentSelections[i].getLastPathComponent());
        //    theObj = (HObject) currentItem.getData();

              // Cannot delete root
        //    if (theObj instanceof Group) {
        //        Group g = (Group) theObj;
        //        if (g.isRoot()) {
        //            shell.getDisplay().beep();
        //            showError("Unsupported operation: cannot delete the file root.");
        //            return;
        //       }
        //    }

        //    if (moveFlag != true) {
        //        if (isObjectOpen(theObj)) {
        //            shell.getDisplay().beep();
        //            showError("Cannot delete the selected object: " + theObj
        //                    + "\nThe dataset or dataset in the group is in use."
        //                    + "\n\nPlease close the dataset(s) and try again.\n");
        //            continue;
        //        }
        //    }

        //    try {
        //        theFile.delete(theObj);
        //    }
        //    catch (Exception ex) {
        //        shell.getDisplay().beep();
        //        showError(ex.getMessage());
        //        continue;
        //    }

        //    if (theObj.equals(selectedObject)) {
        //        selectedObject = null;
        //    }

        //    removeItem(currentItem);
        //} // for (int i=0; i< currentSelections.length; i++) {
    }
    
    /**
     * Checks if a file is already open.
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
            if ((members == null) || (members.size() == 0)) {
                isOpen = false;
            }
            else {
                int n = members.size();
                for (int i = 0; i < n; i++) {
                    HObject theObj = (HObject) members.get(i);
                    isOpen = (viewer.getDataView(theObj) != null);
                    if (isOpen) {
                        break;
                    }
                }
            }
        }
        else {
            return !(viewer.getDataView(obj) == null);
        }

        return isOpen;
    }
    
    /** enable/disable GUI components */
    private static void setEnabled(List<MenuItem> list, boolean b) {
        Iterator<MenuItem> it = list.iterator();
        while (it.hasNext()) {
            it.next().setEnabled(b);
        }
    }
    
    /** Show an error dialog with the given error message */
    private void showError(String errorMsg, String title) {
        MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
        error.setText(title);
        error.setMessage(errorMsg);
        error.open();
    }
    
    /**
     * Opens a file and retrieves the file structure of the file. It also can be
     * used to create a new file by setting the accessID to FileFormat.CREATE.
     * 
     * <p>
     * Subclasses must implement this function to take appropriate steps to open
     * a file.
     * </p>
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
     * @return the FileFormat of this file if successful; otherwise returns
     *         null.
     */
    public FileFormat openFile(String filename, int accessID) throws Exception {
        FileFormat fileFormat = null;
        TreeItem fileRoot = null;
        boolean isNewFile = (FileFormat.OPEN_NEW == (accessID & FileFormat.OPEN_NEW));
        if (isNewFile) accessID = accessID - FileFormat.OPEN_NEW;
        
        if (isFileOpen(filename)) {
            viewer.showStatus("File is in use");
            return null;
        }

        File tmpFile = new File(filename);
        if (!tmpFile.exists()) {
            throw new UnsupportedOperationException("File does not exist.");
        }

        if (!tmpFile.canWrite()) {
            accessID = FileFormat.READ;
        }

        Enumeration<?> keys = FileFormat.getFileFormatKeys();

        String theKey = null;
        while (keys.hasMoreElements()) {
            theKey = (String) keys.nextElement();
            if (theKey.equals(FileFormat.FILE_TYPE_HDF4)) {
                try {
                    FileFormat h4format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
                    if ((h4format != null) && h4format.isThisType(filename)) {
                        fileFormat = h4format.createInstance(filename, accessID);
                        break;
                    }
                }
                catch (Throwable err) {
                    log.debug("Error retrieving the file structure of {}: {}", filename, err);
                }
                continue;
            }
            else if (theKey.equals(FileFormat.FILE_TYPE_HDF5)) {
                try {
                    FileFormat h5format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
                    if ((h5format != null) && h5format.isThisType(filename)) {
                        fileFormat = h5format.createInstance(filename, accessID);
                        break;
                    }
                }
                catch (Throwable err) {
                    log.debug("Error retrieving the file structure of {}: {}", filename, err);
                }
                continue;
            }
            else {
                try {
                    FileFormat theformat = FileFormat.getFileFormat(theKey);
                    if (theformat.isThisType(filename)) {
                        fileFormat = theformat.createInstance(filename, accessID);
                        break;
                    }
                }
                catch (Throwable err) {
                    log.debug("Error retrieving the file structure of {}: {}", filename, err);
                }
            }
        }
        
        if (fileFormat == null) throw new java.io.IOException("Unsupported fileformat - " + filename);

        //shell.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_ARROW));
        
        try {
            fileFormat.setMaxMembers(ViewProperties.getMaxMembers());
            fileFormat.setStartMembers(ViewProperties.getStartMembers());
            if (fileFormat.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                if(isNewFile) {
                    currentIndexType = fileFormat.getIndexType(ViewProperties.getIndexType());
                    currentIndexOrder = fileFormat.getIndexOrder(ViewProperties.getIndexOrder());                  
                }
                fileFormat.setIndexType(currentIndexType);
                fileFormat.setIndexOrder(currentIndexOrder);
            }

            fileFormat.open();
        }
        catch (Exception ex) {
            log.debug("FileFormat init and open: {}", ex);
            fileFormat = null;
        }
        finally {
            //cursor = new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW);
            //shell.setCursor(cursor);
            //cursor.dispose();
        }

        if (fileFormat == null) {
            throw new java.io.IOException("Failed to open file - " + filename);
        } 
        else  {
            fileRoot = new TreeItem(tree, SWT.NONE, 0);
            fileRoot.setData((HObject) fileFormat.getRootObject());
            
            if(fileFormat.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4)))
                fileRoot.setImage(ViewProperties.getH4Icon());
            else
                fileRoot.setImage(ViewProperties.getH5Icon());
            
            String fname = fileFormat.getFilePath();
            fileRoot.setText(fname.substring(fname.lastIndexOf('/') + 1, fname.length()));
            
            if (fileRoot != null) {
                //int currentRowCount = tree.getRowCount();
                //if (currentRowCount > 0) {
                //    tree.expandRow(tree.getRowCount() - 1);
                //}

                fileList.add(fileFormat);
            }
        }

        return fileFormat;
    }
    
    public FileFormat reopenFile(FileFormat fileFormat) throws Exception {
        if (fileFormat.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
            this.currentIndexType = fileFormat.getIndexType(null);
            this.currentIndexOrder = fileFormat.getIndexOrder(null);
        }
        if (fileFormat.isReadOnly())
            return openFile(fileFormat.getAbsolutePath(), FileFormat.READ);
        else
            return openFile(fileFormat.getAbsolutePath(), FileFormat.WRITE);
    }

    /**
     * Close a file
     * 
     * @param file
     *            the file to close
     */
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
                
                try {
                    theFile.close();
                }
                catch (Exception ex) {
                    log.debug("DefaultTreeView: closeFile({}): {}:", theFile.getFilePath(), ex);
                }
                
                fileList.remove(theFile);
                if (theFile.equals(selectedFile)) {
                    selectedFile = null;
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
     */
    public void saveFile(FileFormat file) throws Exception {
        if (file == null) {
            shell.getDisplay().beep();
            showError("Select a file to save.", null);
            return;
        }

        boolean isH4 = file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        boolean isH5 = file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        if (!(isH4 || isH5)) {
            shell.getDisplay().beep();
            showError("Saving file is not supported for this file type", null);
            return;
        }

        // Write the change of the data into the file before saving the file
        List<?> views = ((HDFView) viewer).getDataViews();
        Object theView = null;
        TableView tableView = null;
        TextView textView = null;
        FileFormat theFile = null;
        if (views != null) {
            int n = views.size();
            for (int i = 0; i < n; i++) {
                theView = views.get(i);
                if (theView instanceof TableView) {
                    tableView = (TableView) theView;
                    theFile = tableView.getDataObject().getFileFormat();
                    if (file.equals(theFile)) {
                        tableView.updateValueInFile();
                    }
                }
                else if (theView instanceof TextView) {
                    textView = (TextView) theView;
                    theFile = textView.getDataObject().getFileFormat();
                    if (file.equals(theFile)) {
                        textView.updateValueInFile();
                    }
                }
            }
        }

        if (isH5) {
            saveAsHDF5(file);
        }
        else if (isH4) {
            saveAsHDF4(file);
        }
    }
    
    /**
     * Returns the tree item that contains the given data object.
     */
    public TreeItem findTreeItem(HObject obj) {
        if (obj == null || (obj.getFileFormat().getRootObject() == null)) return null;

        TreeItem theItem = null;
        HObject theObj = null;
        
        Enumeration<?> local_enum = this.getAllItemsBreadthFirst();
        while (local_enum.hasMoreElements()) {
            theItem = (TreeItem) local_enum.nextElement();
            theObj = (HObject) theItem.getData();
            if (theObj == null) {
                continue;
            }
            else if (theObj.equals(obj)) {
                return theItem;
            }
        }

        return null;
    }

    /**
     * Gets the selected file. When multiple files are open, we need to know
     * which file is currently selected.
     * 
     * @return the FileFormat of the currently selected file.
     */
    public FileFormat getSelectedFile() {
        return selectedFile;
    }
    
    /**
     * @return the currently selected object in the tree.
     */
    public HObject getCurrentObject() {
        return selectedObject;
    }
    
    /**
     * Returns the Tree which holds the file structure.
     * 
     * @return the Tree which holds the file structure.
     */
    public Tree getTree() {
        return tree;
    }

    /**
     * Returns the list of currently open files.
     */
    public List<FileFormat> getCurrentFiles() {
        return fileList;
    }

    


}
