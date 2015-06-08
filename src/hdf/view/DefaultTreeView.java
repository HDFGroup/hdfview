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

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;


import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.ViewProperties.DATA_VIEW_KEY;

/**
 * 
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
 * From the TreeView, you can open data content or metadata of selected object.
 * You can select object(s) to delete or add new objects to the file.
 * </p>
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DefaultTreeView implements TreeView {
    private static final long            serialVersionUID    = 4092566164712521186L;
    
    private Shell shell;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultTreeView.class);

    /** The owner of this treeview */
    private ViewManager                   viewer;

    /**
     * The super root of tree: all open files start at this root.
     */
    private final TreeItem				  root;

    /**
     * The tree which holds file structures.
     */
    private final Tree                    tree;

    /**
     * The tree model
     */
    //private final DefaultTreeModel        treeModel;

    /** A list of open files. */
    private final List<FileFormat>        fileList;

    /** Selected file */
    private FileFormat                    selectedFile;

    /** The current selected node. */
    private TreeItem                      selectedItem;

    /** The current selected TreePath. */
    //private TreePath                      selectedTreePath;

    /** The current selected object */
    private HObject                       selectedObject;

    /** Flag to indicate if the dataset is displayed as default */
    private boolean                      isDefaultDisplay;

    /**
     * The popup menu used to display user choice of actions on data object.
     */
    private final Menu             		 popupMenu;

    private MenuItem                     separator;

    /** A list of editing GUI components */
    private List<MenuItem>               editGUIs;

    /** The list of current selected objects */
    private List<Object>                 objectsToCopy;

    private Menu                     	 exportDatasetMenu;
    
    private Menu					 	 newObjectMenu;
    
    private MenuItem                     addTableMenuItem;

    private MenuItem                     addDatasetMenuItem;

    private MenuItem                     addDatatypeMenuItem;

    private MenuItem                     addLinkMenuItem;

    private MenuItem                     setLibVerBoundsItem;

    private MenuItem                     changeIndexItem;

    private String                       currentSearchPhrase = null;

    private boolean                      moveFlag;

    //private TreePath[]                   currentSelectionsForMove;

    private boolean                      isApplyBitmaskOnly  = false;

    private int                          currentIndexType;

    private int                          currentIndexOrder;

    private int                          binaryOrder;
    
    private enum OBJECT_TYPE {GROUP, DATASET, IMAGE, TABLE, DATATYPE, LINK};

    public DefaultTreeView(ViewManager theView, Composite parent) {
        viewer = theView;
        
        // Initialize the tree and root item
        tree = new Tree(parent, SWT.SINGLE | SWT.VIRTUAL);

        root = new TreeItem(tree, SWT.NONE);
        
            //private static final long serialVersionUID = -6829919815424470510L;

            //public boolean isLeaf() {
            //    return false;
            //}

        //treeModel = new DefaultTreeModel(root);
        
        fileList = new Vector<FileFormat>();
        editGUIs = new Vector<MenuItem>();
        objectsToCopy = null;
        isDefaultDisplay = true;
        //selectedTreePath = null;
        selectedItem = null;
        moveFlag = false;
        //currentSelectionsForMove = null;

        //tree.setLargeModel(true);
        //tree.setCellRenderer(new HTreeCellRenderer());
        //tree.addMouseListener(new HTreeMouseAdapter());
        //tree.addKeyListener(new HTreeKeyAdapter());
        //tree.setRootVisible(false);
        // tree.setShowsRootHandles(true);
        //int rowheight = 23 + (int) ((tree.getFont().getSize() - 12) * 0.5);
        //tree.setRowHeight(rowheight);
        
        // Handle tree key events
        tree.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {	
			}

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
         * Handle mouse clicks on data object in the tree view. A right mouse-click
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
        	
        	public void mouseDown(MouseEvent e) {
        		
        	}
        	
        	public void mouseUp(MouseEvent e) {
        		//TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        		//if (selPath == null) return;
        		
        		//TreeItem theItem = (TreeItem) selPath.getLastPathComponent();
        		//if (!theItem.equals(selectedItem)) {
        		//	selectedTreePath = selPath;
        		//	selectedItem = theItem;
        		//	selectedObject = ((HObject) (selectedItem.getData()));
        		//	FileFormat theFile = selectedObject.getFileFormat();
        		//	if ((theFile != null) && !theFile.equals(selectedFile)) {
        				// A different file is selected, handle only one file at a time
        		//		selectedFile = theFile;
        		//		tree.clearSelection();
        		//		tree.setSelectionPath(selPath);
        		//	}
        			
        		//	viewer.mouseEventFired(e);
        		//}
        		
        		// ***************************************************************
                // Different platforms have different ways to show popups
                // if (e.getModifiers() == MouseEvent.BUTTON3_MASK) works for all
                // but mac
                // mouseReleased() and e.isPopupTrigger() work on windows and mac
                // but not unix,
                // mouseClicked() and e.isPopupTrigger() work on unix and mac but
                // not windows,
                // to solve the problem, we use both.
                // 7/25/06 bug 517. e.isPopupTrigger does not work on one mouse Mac.
                // add (MouseEvent.BUTTON1_MASK|MouseEvent.CTRL_MASK) for MAC
                
        		//int eMod = e.getModifiers();
                //if (e.isPopupTrigger()
                //        || (eMod == MouseEvent.BUTTON3_MASK)
                //        || (System.getProperty("os.name").startsWith("Mac") && (eMod == (MouseEvent.BUTTON1_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())))) {
                    //int selRow = tree.getRowForLocation(e.getX(), e.getY());

                    //if (!tree.isRowSelected(selRow)) {
                    //    // reselect the node
                    //    tree.clearSelection();
                    //    tree.setSelectionRow(selRow);
                    //}
                //    showPopupMenu(e);
                //}
        	}
        });

        // Create the popup menu displayed when tree items are right-clicked
        popupMenu = createPopupMenu();

        // reset the scroll increment
        // layout GUI component
        // this.setLayout(new BorderLayout());
    }

    /**
     * Insert an item into the tree.
     * 
     * @param item
     *            the item to insert.
     * @param pitem
     *            the parent item.
     */
    private void insertItem(TreeItem item, TreeItem pitem) {
        if ((item == null) || (pitem == null)) return;

        //item = new TreeItem(tree);
        //tree.insertItemInto((TreeItem) item, (TreeItem) pitem, pitem.getItemCount());
    }

    /**
     * Checks if a file is already open.
     */
    private boolean isFileOpen(String filename) {
        boolean isOpen = false;

        // Find the file by matching its file name and close the file
        FileFormat theFile = null;
        Iterator<FileFormat> iterator = fileList.iterator();
        while (iterator.hasNext()) {
            theFile = iterator.next();
            if (theFile.getFilePath().equals(filename)) {
                isOpen = true;
                break;
            }
        } // while(iterator.hasNext())

        return isOpen;
    }

    /** Creates a popup menu for a right mouse click on a data object */
    private Menu createPopupMenu() {
        final Menu menu = new Menu(tree);
        MenuItem item;
        
        tree.setMenu(menu);

        item = new MenuItem(menu, SWT.NONE);
        item.setText("&Open");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		isDefaultDisplay = true;
        		
        		try {
        			showDataContent(selectedObject);
        		}
        		catch (Throwable err) {
        			shell.getDisplay().beep();
        			showError(err.getMessage());
        			return;
        		}
        	}
        });

        item = new MenuItem(menu, SWT.NONE);
        item.setText("Open &As");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		isDefaultDisplay = false;
        		
        		try {
        			showDataContent(selectedObject);
        		}
        		catch (Throwable err) {
        			shell.getDisplay().beep();
        			showError(err.getMessage());
        			return;
        		}
        	}
        });
        
        MenuItem newObjectMenuItem = new MenuItem(menu, SWT.CASCADE);
        newObjectMenuItem.setText("New");

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.NONE);
        item.setText("Cu&t");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		moveObject();
        	}
        });
        editGUIs.add(item);

        item = new MenuItem(menu, SWT.NONE);
        item.setText("&Copy");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		copyObject();
        	}
        });
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("&Paste");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		pasteObject();
        	}
        });
        editGUIs.add(item);

        item = new MenuItem(menu, SWT.NONE);
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
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("&Save to");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		if (selectedObject == null) return;
        		
        		if ((selectedObject instanceof Group) && ((Group) selectedObject).isRoot()) {
        			shell.getDisplay().beep();
        			showError("Cannot save the root group.\nUse \"Save As\" from file menu to save the whole file");
        			return;
        		}
        		
        		String filetype = FileFormat.FILE_TYPE_HDF4;
        		if (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) 
        			filetype = FileFormat.FILE_TYPE_HDF5;
        		
        		NewFileDialog dialog = new NewFileDialog((JFrame) viewer, selectedObject.getFileFormat().getParent(),
        				filetype, fileList);
        		dialog.setVisible(true);
        		
        		if (!dialog.isFileCreated()) return;
        		
        		String filename = dialog.getFile();
        		FileFormat dstFile = null;
        		
        		try {
        			dstFile = openFile(filename, FileFormat.WRITE);
        		}
        		catch (Exception ex) {
        			shell.getDisplay().beep();
        			showError(ex.getMessage() + "\n" + filename);
        		}
        		
        		List<Object> objList = new Vector<Object>(2);
        		objList.add(selectedObject);
        		pasteObject(objList, dstFile.getRootItem(), dstFile);
        	}
        });
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("&Rename");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		renameObject();
        	}
        });
        editGUIs.add(item);
        
        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("Show Properties");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		isDefaultDisplay = true;
        		
        		try {
        			showMetaData(selectedObject);
        		}
        		catch (Exception ex) {
        			shell.getDisplay().beep();
        			showError(ex.getMessage());
        		}
        	}
        });
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("Show Properties As");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		isDefaultDisplay = false;
        		
        		try {
        			showMetaData(selectedObject);
        		}
        		catch (Exception ex) {
        			shell.getDisplay().beep();
        			showError(ex.getMessage());
        		}
        	}
        });
        
        changeIndexItem = new MenuItem(menu, SWT.NONE);
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
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("&Find");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		String findStr = currentSearchPhrase;
        		if (findStr == null) findStr = "";
        		
        		//findStr = (String) JOptionPane.showInputDialog(this, "Find (e.g. O3Quality, O3*, or *Quality):",
                //        "Find Object by Name", JOptionPane.PLAIN_MESSAGE, null, null, findStr);
        		
        		if (findStr != null && findStr.length() > 0) currentSearchPhrase = findStr;
        		
        		//find(currentSearchPhrase, selectedTreePath, tree);
        	}
        });

        // item = new MenuItem(menu, SWT.NONE);
        // item.setText("Find Next");
        // item.setMnemonic(KeyEvent.VK_N);
        // item.addActionListener(this);
        // item.addSelectionListener(new SelectionAdapter() {
        // 	public void widgetSelected(SelectionEvent e) {
        // 		find(currentSearchPhrase, selectedTreePath, tree);
        // 	}
        // });
        
        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("Expand All");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		//for (int row = 0; row < tree.getRowCount(); row++)
        		//	tree.expandRow(row);
        	}
        });
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("Collapse All");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        	    //for (int row = tree.getRowCount() - 1; row >= 0; row--)
        		//	tree.collapseRow(row);
        	}
        });

        new MenuItem(menu, SWT.SEPARATOR);
        
        item = new MenuItem(menu, SWT.NONE);
        item.setText("Close Fil&e");
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		try {
        			closeFile(getSelectedFile());
        		}
        		catch (Exception ex) {
        			showError(ex.getMessage());
        		}
        	}
        });

        item = new MenuItem(menu, SWT.NONE);
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
        
        item = new MenuItem(newObjectMenu, SWT.NONE);
        item.setText("Group");
        item.setImage(ViewProperties.getFoldercloseIcon());
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		addObject(OBJECT_TYPE.GROUP);
        	}
        });
        
        addDatasetMenuItem = new MenuItem(newObjectMenu, SWT.NONE);
        addDatasetMenuItem.setText("Dataset");
        addDatasetMenuItem.setImage(ViewProperties.getDatasetIcon());
        addDatasetMenuItem.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		addObject(OBJECT_TYPE.DATASET);
        	}
        });
        
        item = new MenuItem(newObjectMenu, SWT.NONE);
        item.setText("Image");
        item.setImage(ViewProperties.getImageIcon());
        item.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		addObject(OBJECT_TYPE.IMAGE);
        	}
        });
        
        addTableMenuItem = new MenuItem(newObjectMenu, SWT.NONE);
        addTableMenuItem.setText("Compound DS");
        addTableMenuItem.setImage(ViewProperties.getTableIcon());
        addTableMenuItem.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		addObject(OBJECT_TYPE.TABLE);
        	}
        });
        
        addDatatypeMenuItem = new MenuItem(newObjectMenu, SWT.NONE);
        addDatatypeMenuItem.setText("Datatype");
        addDatatypeMenuItem.setImage(ViewProperties.getDatatypeIcon());
        addDatatypeMenuItem.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		addObject(OBJECT_TYPE.DATATYPE);
        	}
        });
        
        addLinkMenuItem = new MenuItem(newObjectMenu, SWT.NONE);
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
        
        item = new MenuItem(exportDatasetMenu, SWT.NONE);
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
        
        item = new MenuItem(exportDatasetMenu, SWT.NONE);
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
        
        item = new MenuItem(exportDatasetMenu, SWT.NONE);
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
        
        item = new MenuItem(exportDatasetMenu, SWT.NONE);
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
        	public void menuShown(MouseEvent e) {
        		HObject selectedObject = ((HObject) (selectedItem.getData()));
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

    /** disable/enable GUI components */
    private static void setEnabled(List<MenuItem> list, boolean b) {
        MenuItem item = null;
        Iterator<MenuItem> it = list.iterator();
        while (it.hasNext()) {
            item = it.next();
            item.setEnabled(b);
        }
    }

    /**
     * Save the current file into HDF4. Since HDF4 does not support packing. The
     * source file is copied into the new file with the exact same content.
     */
    private final void saveAsHDF4(FileFormat srcFile) {
        if (srcFile == null) {
            shell.getDisplay().beep();
            showError("Select a file to save.");
            return;
        }

        JFrame owner = (viewer == null) ? new JFrame() : (JFrame) viewer;
        String currentDir = srcFile.getParent();
        NewFileDialog dialog = new NewFileDialog(owner, currentDir, FileFormat.FILE_TYPE_HDF4, getCurrentFiles());
        // dialog.show();

        if (!dialog.isFileCreated()) {
            return;
        }

        String filename = dialog.getFile();

        // Since cannot pack hdf4, simply copy the whole physical file
        int length = 0;
        int bsize = 512;
        byte[] buffer;
        BufferedInputStream bi = null;
        BufferedOutputStream bo = null;

        try {
            bi = new BufferedInputStream(new FileInputStream(srcFile.getFilePath()));
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage() + "\n" + filename);
            return;
        }

        try {
            bo = new BufferedOutputStream(new FileOutputStream(filename));
        }
        catch (Exception ex) {
            try {
                bi.close();
            }
            catch (Exception ex2) {
            	log.debug("Output file force input close:", ex2);
            }
            
            shell.getDisplay().beep();
            showError(ex.getMessage());
            return;
        }

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
        try {
            bi.close();
        }
        catch (Exception ex) {
        	log.debug("Input file:", ex);
        }
        try {
            bo.close();
        }
        catch (Exception ex) {
        	log.debug("Output file:", ex);
        }

        try {
            openFile(filename, FileFormat.WRITE);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage() + "\n" + filename);
        }
    }

    /**
     * Copy the current file into a new file. The new file does not include the
     * inaccessible objects. Values of reference dataset are not updated in the
     * new file.
     */
    private void saveAsHDF5(FileFormat srcFile) {
        if (srcFile == null) {
            shell.getDisplay().beep();
            showError("Select a file to save.");
            return;
        }

        TreeItem root = srcFile.getRootItem();
        if (root == null) {
            shell.getDisplay().beep();
            showError("The file is empty.");
            return;
        }

        JFrame owner = (viewer == null) ? new JFrame() : (JFrame) viewer;
        NewFileDialog dialog = new NewFileDialog(owner, srcFile.getParent(), FileFormat.FILE_TYPE_HDF5,
                getCurrentFiles());

        if (!dialog.isFileCreated()) return;

        String filename = dialog.getFile();

        int n = root.getItemCount();
        Vector<Object> objList = new Vector<Object>(n);
        TreeItem item = null;
        for (int i = 0; i < n; i++) {
            item = root.getItem(i);
            objList.add(item.getData());
        }

        FileFormat newFile = null;
        try {
            newFile = openFile(filename, FileFormat.WRITE);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage() + "\n" + filename);
            return;
        }

        if (newFile == null) return;

        TreeItem pitem = newFile.getRootItem();

        pasteObject(objList, pitem, newFile);
        objList.setSize(0);

        Group srcGroup = (Group) root.getData();
        Group dstGroup = (Group) (newFile.getRootItem()).getData();
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
            showError(ex.getMessage());
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
            showError(ex.getMessage());
        }
    }

    /** Copy selected objects */
    private void copyObject() {
        objectsToCopy = getSelectedObjects();
        moveFlag = false;
    }

    /** Move selected objects */
    private void moveObject() {
        objectsToCopy = getSelectedObjects();
        moveFlag = true;
        //currentSelectionsForMove = tree.getSelectionPaths();
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

        if ((objectsToCopy == null) || (objectsToCopy.size() <= 0) || (pitem == null)) return;

        FileFormat srcFile = ((HObject) objectsToCopy.get(0)).getFileFormat();
        FileFormat dstFile = getSelectedFile();
        FileFormat h5file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
        FileFormat h4file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);

        if (srcFile == null) {
            shell.getDisplay().beep();
            showError("Source file is null.");
            return;
        }
        else if (dstFile == null) {
            shell.getDisplay().beep();
            showError("Destination file is null.");
            return;
        }
        else if (srcFile.isThisType(h4file) && dstFile.isThisType(h5file)) {
            shell.getDisplay().beep();
            showError("Unsupported operation: cannot copy HDF4 object to HDF5 file");
            return;
        }
        else if (srcFile.isThisType(h5file) && dstFile.isThisType(h4file)) {
            shell.getDisplay().beep();
            showError("Unsupported operation: cannot copy HDF5 object to HDF4 file");
            return;
        }

        if (moveFlag == true) {
            if (srcFile != dstFile) {
                shell.getDisplay().beep();
                showError("Cannot move the selected object to different file");
                moveFlag = false;
                //currentSelectionsForMove = null;
                objectsToCopy = null;
                return;
            }
        }

        //if (pitem.isLeaf()) {
        //    pitem = pitem.getParentItem();
        //}
        Group pgroup = (Group) pitem.getData();
        String fullPath = pgroup.getPath() + pgroup.getName();
        if (pgroup.isRoot()) {
            fullPath = HObject.separator;
        }

        String msg = "";
        int msgType = JOptionPane.QUESTION_MESSAGE;
        if (srcFile.isThisType(h4file)) {
            msg = "WARNING: object can not be deleted after it is copied.\n\n";
            msgType = JOptionPane.WARNING_MESSAGE;
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

        // objectsToCopy = null;
        if (moveFlag == true) {
            removeSelectedObjects();
            moveFlag = false;
            //currentSelectionsForMove = null;
            objectsToCopy = null;
        }
        
        log.trace("pasteObject(): finish");
    }

    /** Paste selected objects */
    private void pasteObject(List<Object> objList, TreeItem pitem, FileFormat dstFile) {
        if ((objList == null) || (objList.size() <= 0) || (pitem == null)) return;

        ((HObject) objList.get(0)).getFileFormat();
        Group pgroup = (Group) pitem.getData();
        log.trace("pasteObject(...): start");

        HObject theObj = null;
        TreeItem newItem = null;
        Iterator<Object> iterator = objList.iterator();
        while (iterator.hasNext()) {
            newItem = null;
            theObj = (HObject) iterator.next();

            if ((theObj instanceof Group) && ((Group) theObj).isRoot()) {
                shell.getDisplay().beep();
                showError("Unsupported operation: cannot copy the root group");
                return;
            }

            // Check if it creates infinite loop
            Group pg = pgroup;
            while (!pg.isRoot()) {
                if (theObj.equals(pg)) {
                    shell.getDisplay().beep();
                    showError("Unsupported operation: cannot copy a group to itself.");
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
                showError(ex.getMessage());
                newItem = null;
            }

            // Add the node to the tree
            if (newItem != null) insertItem(newItem, pitem);

        } // while (iterator.hasNext())

        log.trace("pasteObject(...): finish");
    }

    private void removeSelectedObjects() {
        FileFormat theFile = getSelectedFile();
        if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
            shell.getDisplay().beep();
            showError("Unsupported operation: cannot delete HDF4 object.");
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
        //       shell.getDisplay().beep();
        //        showError(ex.getMessage());
        //        continue;
        //    }

        //    if (theObj.equals(selectedObject)) {
        //        selectedObject = null;
        //    }

        //    removeItem(currentItem);
        //} // for (int i=0; i< currentSelections.length; i++) {
    }

    private void removeItem(TreeItem item) {
        if (item == null) return;
        
        TreeItem parentItem = (TreeItem) (item.getParentItem());
        if (parentItem != null) {
            // treeModel.removeNodeFromParent(item);

            // Add the two lines to fix bug in HDFView 1.2. Delete a subgroup
            // and then copy the group to another group, the deleted group still
            // exists.
            Group pgroup = (Group) parentItem.getData();
            pgroup.removeFromMemberList((HObject) item.getData());

            if (item.equals(selectedItem)) {
                selectedItem = null;
                selectedFile = null;
            }
        } // if (parentNode != null) {
    }

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
            if (viewer.getDataView(obj) == null) {
                isOpen = false;
            }
            else {
                isOpen = true;
            }
        }

        return isOpen;
    }

    /**
     * Returns a list of all user objects that traverses the subtree rooted at
     * this node in breadth-first order..
     * 
     * @param node
     *            the node to start with.
     */
    private final List<Object> breadthFirstUserObjects(TreeItem item) {
        if (item == null) return null;

        Vector<Object> list = new Vector<Object>();
        TreeItem theItem = null;
        //Enumeration<?> local_enum = item.breadthFirstEnumeration();
        //while (local_enum.hasMoreElements()) {
        //    theItem = local_enum.nextElement();
        //    list.add(theItem.getData());
        //}

        return list;
    }

    /**
     * Find first object that is matched by name.
     * 
     * @param objName
     *            -- the object name.
     * @return the object if found, otherwise, returns null.
     */
    /*
    private final static HObject find(String objName, TreePath treePath, Tree tree) {
        HObject retObj = null;
        boolean isFound = false, isPrefix = false, isSuffix = false, isContain = false;

        if (objName == null || objName.length() <= 0 || treePath == null) {
            return null;
        }

        if (objName.equals("*")) return null;

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

        if (objName == null || objName.length() <= 0) return null;

        TreeItem item = treePath.getLastPathComponent();
        if (item == null) return null;

        HObject obj = null;
        String theName = null;
        TreeItem theItem = null;
        //Enumeration<?> local_enum = item.breadthFirstEnumeration();
        //while (local_enum.hasMoreElements()) {
        //    theItem = (TreeItem) local_enum.nextElement();
        //    obj = (HObject) theItem.getData();
        //    if (obj != null && (theName = obj.getName()) != null) {
        //        if (isPrefix)
        //            isFound = theName.startsWith(objName);
        //        else if (isSuffix)
        //            isFound = theName.endsWith(objName);
        //        else if (isContain)
        //            isFound = theName.contains(objName);
        //        else
        //            isFound = theName.equals(objName);

        //        if (isFound) {
        //            retObj = obj;
        //            break;
        //        }
        //    }
        //}

        if (retObj != null) {
        //    TreePath dstPath = getTreePath(treePath, theItem, 0);

        //    tree.fireTreeExpanded(dstPath) ;
        //    tree.setSelectionPath(dstPath);
        //    tree.scrollPathToVisible(dstPath);
        }

        return retObj;
    }
    */

    /**
     * Get the TreePath from the parent to the target item.
     * 
     * @param parent
     *            -- the parent TreePath
     * @param item
     *            -- the target item
     * @param depth
     * @return the tree path if target item found, otherwise; returns null;
     */
    /*
    private static TreePath getTreePath(TreePath parent, TreeItem item, int depth) {
        if (item == null || parent == null || depth < 0) return null;

        TreeItem theItem = parent.getLastPathComponent();
        if (item == theItem) return parent;

        //if (theItem.getChildCount() >= 0) {
        //    for (Enumeration<?> e = theItem.children(); e.hasMoreElements();) {
        //        TreeItem n = (TreeItem) e.nextElement();
        //        TreePath path = parent.pathByAddingChild(n);
        //        TreePath result = getTreePath(path, item, depth + 1);

        //        if (result != null) {
        //            return result;
        //        }
        //    }
        //}

        return null;
    }
    */

    private void addObject(OBJECT_TYPE type) {
    	if ((selectedObject == null) || (selectedItem == null)) return;
    	
    	Group pGroup = null;
    	if(selectedObject instanceof Group) {
    		pGroup = (Group) selectedObject;
    	}
    	else {
    		pGroup = (Group) (selectedItem.getParent()).getData();
    	}
    	
    	HObject obj = null;
    	switch(type) {
    		case GROUP:
    			NewGroupDialog groupDialog = new NewGroupDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootItem()));
    			groupDialog.setVisible(true);
    			obj = (HObject) groupDialog.getObject();
    			pGroup = groupDialog.getParentGroup();
    			break;
    		case DATASET:
    			NewDatasetDialog datasetDialog = new NewDatasetDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootItem()));
    			datasetDialog.setVisible(true);
    			obj = (HObject) datasetDialog.getObject();
    			pGroup = datasetDialog.getParentGroup();
    			break;
    		case IMAGE:
    			NewImageDialog imageDialog = new NewImageDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootItem()));
    	        imageDialog.setVisible(true);
    	        obj = (HObject) imageDialog.getObject();
    	        pGroup = imageDialog.getParentGroup();
    	        break;
    		case TABLE:
    			NewTableDataDialog tableDialog = new NewTableDataDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootItem()));
    	        tableDialog.setVisible(true);
    	        obj = (HObject) tableDialog.getObject();
    	        pGroup = tableDialog.getParentGroup();
    	        break;
    		case DATATYPE:
    			NewDatatypeDialog datatypeDialog = new NewDatatypeDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootItem()));
    	        datatypeDialog.setVisible(true);
    	        obj = (HObject) datatypeDialog.getObject();
    	        pGroup = datatypeDialog.getParentGroup();
    	        break;
    		case LINK:
    			NewLinkDialog linkDialog = new NewLinkDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject.getFileFormat().getRootItem()));
    	        linkDialog.setVisible(true);
    	        obj = (HObject) linkDialog.getObject();
    	        pGroup = linkDialog.getParentGroup();
    	        break;
    	}
    	
    	if (obj == null) return;
    	
    	try {
    		this.addObject(obj, pGroup);
    	}
    	catch (Exception ex) {
    		shell.getDisplay().beep();
    		showError(ex.getMessage());
    		return;
    	}
    }

    /** Save data as file. */
    private void saveAsFile() throws Exception {
        if (!(selectedObject instanceof Dataset) || (selectedObject == null) || (selectedItem == null)) return;
        
        Dataset dataset = (Dataset) selectedObject;
        FileDialog fChooser = new FileDialog(shell);
        //fChooser.setFilterPath(dataset.getFile().);
        
        //final JFileChooser fchooser = new JFileChooser(dataset.getFile());
        //fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
        // fchooser.changeToParentDirectory();
        File chosenFile = null;
        
        if(binaryOrder == 99) {
            fChooser.setText("Save Dataset Data To Text File --- " + dataset.getName());
            chosenFile = new File(dataset.getName() + ".txt");
        }
        else {
            fChooser.setText("Save Current Data To Binary File --- " + dataset.getName());
            chosenFile = new File(dataset.getName() + ".bin");
        }

        //fchooser.setSelectedFile(choosedFile);
        //int returnVal = fchooser.showSaveDialog(this);

        if (fChooser.open() == null) return;

        //choosedFile = fchooser.getSelectedFile();
        if (chosenFile == null) return;
        
        String fname = chosenFile.getAbsolutePath();

        // Check if the file is in use
        List<?> fileList = viewer.getTreeView().getCurrentFiles();
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator<?> iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText("Export Dataset");
                    error.setMessage("Unable to save data to file \"" + fname + "\". \nThe file is being used.");
                    error.open();
                    return;
                }
            }
        }

        if (chosenFile.exists()) {
        	MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        	confirm.setText("Export Dataset");
        	confirm.setMessage("File exists. Do you want to replace it?");
            if (confirm.open() == SWT.NO) return;
        }

        boolean isH4 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        if (isH4) {
            shell.getDisplay().beep();
            showError("Cannot export HDF4 object.");
            return;
        }

        try {
            selectedObject.getFileFormat().exportDataset(fname, dataset.getFile(), dataset.getFullName(), binaryOrder);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage());
        }

        viewer.showStatus("Data save to: " + fname);
    }

    private void renameObject() {
        if (selectedObject == null) return;

        if ((selectedObject instanceof Group) && ((Group) selectedObject).isRoot()) {
            shell.getDisplay().beep();
            showError("Cannot rename the root.");
            return;
        }

        boolean isH4 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        if (isH4) {
            shell.getDisplay().beep();
            showError("Cannot rename HDF4 object.");
            return;
        }

        String oldName = selectedObject.getName();
        // String newName = (String) JOptionPane.showInputDialog(this, "Rename \"" + oldName + "\" to:", "Rename...",
        //        JOptionPane.INFORMATION_MESSAGE, null, null, oldName);

        //if (newName == null) return;

        //newName = newName.trim();
        //if ((newName == null) || (newName.length() == 0) || newName.equals(oldName)) {
        //    return;
        //}

        //try {
        //    selectedObject.setName(newName);
        //}
        //catch (Exception ex) {
        //    shell.getDisplay().beep();
        //    showError(ex.getMessage());
        //}
    }

    private void setLibVersionBounds() {
        Object[] lowValues = { "Earliest", "Latest" };
        Object[] highValues = { "Latest" };
        //JComboBox lowComboBox = new JComboBox(lowValues);
        //lowComboBox.setName("earliestversion");
        //JComboBox highComboBox = new JComboBox(highValues);
        //highComboBox.setName("latestversion");

        //Object[] msg = { "Earliest Version:", lowComboBox, "Latest Version:", highComboBox };
        Object[] options = { "Ok", "Cancel" };
        //JOptionPane op = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options);

        //op.setName("libselect");
        //JDialog dialog = op.createDialog(this, "Set the library version bounds: ");
        //dialog.setVisible(true);

        String result = null;
        try {
           // result = (String) op.getValue();
        }
        catch (Exception err) {
            // err.printStackTrace();
        }

        if ((result != null) && (result.equals("Ok"))) {
            int low = -1;
            int high = 1;
            //if ((lowComboBox.getSelectedItem()).equals("Earliest"))
            //    low = 0;
            //else
            //    low = 1;
            try {
                selectedObject.getFileFormat().setLibBounds(low, high);
            }
            catch (Throwable err) {
                shell.getDisplay().beep();
                showError("Error when setting lib version bounds");
                return;
            }
        }
        else
            return;
    }
    
    private void showError(String errorMsg) {
    	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
    	error.setText(((Shell) shell.getParent()).getText());
    	error.setMessage(errorMsg);
    	error.open();
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
        boolean bNewFile = (FileFormat.OPEN_NEW == (accessID & FileFormat.OPEN_NEW));
        if(bNewFile) accessID = accessID - FileFormat.OPEN_NEW;
        
        if (isFileOpen(filename)) {
            viewer.showStatus("File is in use");
            return null;
            // throw new UnsupportedOperationException("File is in use.");
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
                	log.debug("retrieves the file structure of {}:", filename, err);
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
                	log.debug("retrieves the file structure of {}:", filename, err);
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
                	log.debug("retrieves the file structure of {}:", filename, err);
                }
            }
        }

        if (fileFormat == null) throw new java.io.IOException("Unsupported fileformat - " + filename);

        ((JFrame) viewer).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            fileFormat.setMaxMembers(ViewProperties.getMaxMembers());
            fileFormat.setStartMembers(ViewProperties.getStartMembers());
            if (fileFormat.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                if(bNewFile) {
                    currentIndexType = fileFormat.getIndexType(ViewProperties.getIndexType());
                    currentIndexOrder = fileFormat.getIndexOrder(ViewProperties.getIndexOrder());                  
                }
                fileFormat.setIndexType(currentIndexType);
                fileFormat.setIndexOrder(currentIndexOrder);
            }

            fileFormat.open();
        }
        catch (Exception ex) {
        	log.debug("fileformat init and open:", ex);
        	fileFormat = null;
        }
        finally {
            ((JFrame) viewer).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        if (fileFormat == null) {
            throw new java.io.IOException("Failed to open file - " + filename);
        } 
        else  {
            fileRoot = (TreeItem) fileFormat.getRootItem();
            if (fileRoot != null) {
                insertItem(fileRoot, root);

                //int currentRowCount = tree.getRowCount();
                //if (currentRowCount > 0) {
                //    tree.expandRow(tree.getRowCount() - 1);
                //}

                fileList.add(fileFormat);
            }       	
        }

        return fileFormat;
    }

    /**
     * Close a file
     * 
     * @param file
     *            the file to close
     */
    public void closeFile(FileFormat file) throws Exception {
        if (file == null) return;

        // Find the file item in the tree and remove it from the tree first
        FileFormat theFile = null;
        TreeItem theItem = null;
        //Enumeration<?> enumeration = root.getItems();
        //while (enumeration.hasMoreElements()) {
        //    theItem = (TreeItem) enumeration.nextElement();
        //    Group g = (Group) theItem.getData();
        //    theFile = g.getFileFormat();

        //    if (theFile.equals(file)) {
              //treeModel.removeNodeFromParent(theItem);
        //        try {
        //            theFile.close();
        //        }
        //        catch (Exception ex) {
        //        	log.debug("close {}:", theFile.getFilePath(), ex);
        //        }
        //        fileList.remove(theFile);
        //        if (theFile.equals(selectedFile)) {
        //            selectedFile = null;
        //            selectedItem = null;
        //       }
        //        break;
        //    }
        //} // while(enumeration.hasMoreElements())
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
            showError("Select a file to save.");
            return;
        }

        boolean isH4 = file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        boolean isH5 = file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        if (!(isH4 || isH5)) {
            shell.getDisplay().beep();
            showError("Saving file is not supported for this file type");
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
     * Gets the selected file. When multiple files are open, we need to know
     * which file is currently selected.
     * 
     * @return the FileFormat of the selected file.
     */
    public FileFormat getSelectedFile() {
        return selectedFile;
    }

    /**
     * Gets a list of selected objects in the tree. Obtaining a list of current
     * selected objects is necessary for copy/paste/delete objects.
     * 
     * @return a list of selected object in the tree.
     */
    public List<Object> getSelectedObjects() {
        /*
    	TreePath[] paths = tree.getSelectionPaths();
        if ((paths == null) || (paths.length <= 0)) {
            return null;
        }

        List<Object> objs = new Vector<Object>(paths.length);
        HObject theObject = null, parentObject;
        DefaultMutableTreeNode currentNode = null, parentNode = null;
        for (int i = 0; i < paths.length; i++) {
            currentNode = (DefaultMutableTreeNode) (paths[i].getLastPathComponent());
            theObject = (HObject) currentNode.getUserObject();

            if (theObject != null) {
                objs.add(theObject);
                // removed the group from the selected list if some of its
                // members are selected
                // to avoid duplicated copy/paste when a group is pasted.
                parentNode = (DefaultMutableTreeNode) currentNode.getParent();
                parentObject = (HObject) parentNode.getUserObject();
                objs.remove(parentObject);
            }
        }

        return objs;
        */
    	
    	return null; // Remove when finished
    }

    /**
     * @return the current selected object in the tree.
     */
    public HObject getCurrentObject() {
        return selectedObject;
    }

    /**
     * Display the content of a data object.
     * 
     * @param dataObject
     *            the data object
     * @return the DataView that displays the data content
     * @throws Exception
     */
    public DataView showDataContent(HObject dataObject) throws Exception {
        log.trace("showDataContent: start");

        if ((dataObject == null) || !(dataObject instanceof Dataset)) {
            return null; // can only display dataset
        }

        Dataset d = (Dataset) dataObject;

        if (d.getRank() <= 0) d.init();
        
        boolean isText = ((d instanceof ScalarDS) && ((ScalarDS) d).isText());
        boolean isImage = ((d instanceof ScalarDS) && ((ScalarDS) d).isImage());
        boolean isDisplayTypeChar = false;
        boolean isTransposed = false;
        boolean isIndexBase1 = ViewProperties.isIndexBase1();
        BitSet bitmask = null;
        String dataViewName = null;
        
        log.trace("showDataContent: inited");

        Shell theShell = (Shell) viewer.getDataView(d);

        if (isDefaultDisplay) {

            if (theShell != null) {
                theShell.setActive();
                return null;
            }

            if (isText) {
                dataViewName = (String) HDFView.getListOfTextView().get(0);
            }
            else if (isImage) {
                dataViewName = (String) HDFView.getListOfImageView().get(0);
            }
            else {
                dataViewName = (String) HDFView.getListOfTableView().get(0);
            }
        }
        else {
            DataOptionDialog dialog = new DataOptionDialog(viewer, d);

            dialog.setVisible(true);
            if (dialog.isCancelled()) {
                return null;
            }

            isImage = dialog.isImageDisplay();
            isDisplayTypeChar = dialog.isDisplayTypeChar();
            dataViewName = dialog.getDataViewName();
            isTransposed = dialog.isTransposed();
            bitmask = dialog.getBitmask();
            isIndexBase1 = dialog.isIndexBase1();
            isApplyBitmaskOnly = dialog.isApplyBitmaskOnly();
        }
        
        log.trace("showDataContent: {}", dataViewName);

        // Enables use of JHDF5 in JNLP (Web Start) applications, the system
        // class loader with reflection first.
        Class<?> theClass = null;
        try {
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            try {
                theClass = ViewProperties.loadExtClass().loadClass(dataViewName);
            }
            catch (Exception ex2) {
                theClass = null;
            }
        }

        // Use default dataview
        if (theClass == null) {
            log.trace("showDataContent: use default dataview");
            if (isText)
                dataViewName = "hdf.view.DefaultTextView";
            else if (isImage)
                dataViewName = "hdf.view.DefaultImageView";
            else
                dataViewName = "hdf.view.DefaultTableView";
            try {
                theClass = Class.forName(dataViewName);
            }
            catch (Exception ex) {
            	log.debug("Class.forName {} failure: ", dataViewName, ex);
            }
        }
        Object theView = null;
        Object[] initargs = { viewer };
        HashMap<DATA_VIEW_KEY, Serializable> map = new HashMap<DATA_VIEW_KEY, Serializable>(8);
        map.put(ViewProperties.DATA_VIEW_KEY.INDEXBASE1, new Boolean(isIndexBase1));
        if (bitmask != null) {
            map.put(ViewProperties.DATA_VIEW_KEY.BITMASK, bitmask);
            if (isApplyBitmaskOnly) map.put(ViewProperties.DATA_VIEW_KEY.BITMASKOP, ViewProperties.BITMASK_OP.AND);

            // create a copy of dataset
            ScalarDS d_copy = null;
            Constructor<? extends Dataset> constructor = null;
            Object[] paramObj = null;
            try {
                Class<?>[] paramClass = { FileFormat.class, String.class, String.class, long[].class };
                constructor = d.getClass().getConstructor(paramClass);

                paramObj = new Object[] { d.getFileFormat(), d.getName(), d.getPath(), d.getOID() };
            }
            catch (Exception ex) {
                constructor = null;
            }

            try {
                d_copy = (ScalarDS) constructor.newInstance(paramObj);
            }
            catch (Exception ex) {
                d_copy = null;
            }
            if (d_copy != null) {
                try {
                    d_copy.init();
                    log.trace("showDataContent: d_copy inited");
                    int rank = d.getRank();
                    System.arraycopy(d.getDims(), 0, d_copy.getDims(), 0, rank);
                    System.arraycopy(d.getStartDims(), 0, d_copy.getStartDims(), 0, rank);
                    System.arraycopy(d.getSelectedDims(), 0, d_copy.getSelectedDims(), 0, rank);
                    System.arraycopy(d.getStride(), 0, d_copy.getStride(), 0, rank);
                    System.arraycopy(d.getSelectedIndex(), 0, d_copy.getSelectedIndex(), 0, 3);
                }
                catch (Throwable ex) {
                    ex.printStackTrace();
                }

                map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, d_copy);
            }
        }
        if (dataViewName.startsWith("hdf.view.DefaultTableView")) {
            map.put(ViewProperties.DATA_VIEW_KEY.CHAR, new Boolean(isDisplayTypeChar));
            map.put(ViewProperties.DATA_VIEW_KEY.TRANSPOSED, new Boolean(isTransposed));
            Object[] tmpargs = { viewer, map };
            initargs = tmpargs;
        }
        else if (dataViewName.startsWith("hdf.view.DefaultImageView")) {
            map.put(ViewProperties.DATA_VIEW_KEY.CONVERTBYTE, new Boolean((bitmask != null)));
            Object[] tmpargs = { viewer, map };
            initargs = tmpargs;
        }

        ((JFrame) viewer).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            theView = Tools.newInstance(theClass, initargs);
            log.trace("showDataContent: Tools.newInstance");

            viewer.addDataView((DataView) theView);
        }
        finally {
            ((JFrame) viewer).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        log.trace("showDataContent: finish");
        return (DataView) theView;
    }

    /**
     * Displays the meta data of a data object.
     * 
     * @param dataObject
     *            the data object
     * @return the MetaDataView that displays the MetaData of the data object
     * @throws Exception
     */
    public MetaDataView showMetaData(HObject dataObject) throws Exception {
        if (dataObject == null) {
            return null;
        }

        List<?> metaDataViewList = HDFView.getListOfMetaDataView();
        if ((metaDataViewList == null) || (metaDataViewList.size() <= 0)) {
            return null;
        }

        int n = metaDataViewList.size();
        String className = (String) metaDataViewList.get(0);

        if (!isDefaultDisplay && (n > 1)) {
            //className = (String) JOptionPane.showInputDialog(this, "Select MetaDataView", "HDFView",
            //        JOptionPane.INFORMATION_MESSAGE, null, metaDataViewList.toArray(), className);
        }

        // enables use of JHDF5 in JNLP (Web Start) applications, the system
        // class loader with reflection first.
        Class<?> theClass = null;
        try {
            theClass = Class.forName(className);
        }
        catch (Exception ex) {
            theClass = ViewProperties.loadExtClass().loadClass(className);
        }

        Object[] initargs = { viewer };
        MetaDataView dataView = (MetaDataView) Tools.newInstance(theClass, initargs);

        return dataView;
    }

    /**
     * Adds a new data object to the file.
     * 
     * @param newObject
     *            the new object to add.
     * @param parentGroup
     *            the parent group the object is to add to.
     * @throws Exception
     */
    public void addObject(HObject newObject, Group parentGroup) throws Exception {
        if ((newObject == null) || (parentGroup == null)) return;

        TreeItem pitem = findTreeItem(parentGroup);
        TreeItem newitem = new TreeItem(pitem, SWT.NONE);
        newitem.setData(newObject);
        
        //private static final long serialVersionUID = -8852535261445958398L;

        //treeModel.insertNodeInto((DefaultMutableTreeNode) newnode, (DefaultMutableTreeNode) pnode,
        //        pnode.getChildCount());
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
     * Returns the list of current open files..
     */
    public List<FileFormat> getCurrentFiles() {
        return fileList;
    }

    /**
     * Returns the tree item that contains the given data object.
     */
    public TreeItem findTreeItem(HObject obj) {
        if (obj == null) return null;

        TreeItem theFileRoot = obj.getFileFormat().getRootItem();
        if (theFileRoot == null) return null;

        TreeItem theItem = null;
        HObject theObj = null;
        //Enumeration<?> local_enum = ((TreeItem) theFileRoot).breadthFirstEnumeration();
        //while (local_enum.hasMoreElements()) {
        //    theItem = (TreeItem) local_enum.nextElement();
        //    theObj = (HObject) theItem.getData();
        //    if (theObj == null) {
        //        continue;
        //    }
        //    else if (theObj.equals(obj)) {
        //        return theItem;
        //    }
        //}

        return null;
    }
    
    /**
     * This class is used to change the default icons for tree nodes.
     * 
     * @see javax.swing.tree.DefaultTreeCellRenderer
     */
    /*
    private class HTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = -9030708781106435297L;
        private Image              h4Icon, h5Icon, datasetIcon, imageIcon, tableIcon, textIcon, openFolder, closeFolder,
        datasetIconA, imageIconA, tableIconA, textIconA, openFolderA, closeFolderA, datatypeIcon,
        datatypeIconA, questionIcon;

        private HTreeCellRenderer() {
            super();

            openFolder = ViewProperties.getFolderopenIcon();
            closeFolder = ViewProperties.getFoldercloseIcon();
            datasetIcon = ViewProperties.getDatasetIcon();
            imageIcon = ViewProperties.getImageIcon();
            h4Icon = ViewProperties.getH4Icon();
            h5Icon = ViewProperties.getH5Icon();
            tableIcon = ViewProperties.getTableIcon();
            textIcon = ViewProperties.getTextIcon();

            openFolderA = ViewProperties.getFolderopenIconA();
            closeFolderA = ViewProperties.getFoldercloseIconA();
            datasetIconA = ViewProperties.getDatasetIconA();
            imageIconA = ViewProperties.getImageIconA();
            tableIconA = ViewProperties.getTableIconA();
            textIconA = ViewProperties.getTextIconA();
            datatypeIcon = ViewProperties.getDatatypeIcon();
            datatypeIconA = ViewProperties.getDatatypeIconA();

            questionIcon = ViewProperties.getQuestionIcon();

            if (openFolder != null) {
                openIcon = openFolder;
            }
            else {
                openFolder = this.openIcon;
            }

            if (closeFolder != null) {
                closedIcon = closeFolder;
            }
            else {
                closeFolder = closedIcon;
            }

            if (datasetIcon == null) {
                datasetIcon = leafIcon;
            }
            if (imageIcon == null) {
                imageIcon = leafIcon;
            }
            if (tableIcon == null) {
                tableIcon = leafIcon;
            }
            if (textIcon == null) {
                textIcon = leafIcon;
            }
            if (h4Icon == null) {
                h4Icon = leafIcon;
            }
            if (h5Icon == null) {
                h5Icon = leafIcon;
            }
            if (datatypeIcon == null) {
                datatypeIcon = leafIcon;
            }

            if (questionIcon == null) {
                questionIcon = leafIcon;
            }

            if (openFolderA == null) {
                openFolderA = openFolder;
            }
            if (closeFolderA == null) {
                closeFolderA = closeFolder;
            }
            if (datasetIconA == null) {
                datasetIconA = datasetIcon;
            }
            if (imageIconA == null) {
                imageIconA = imageIcon;
            }
            if (tableIconA == null) {
                tableIconA = tableIcon;
            }
            if (textIconA == null) {
                textIconA = textIcon;
            }
            if (datatypeIconA == null) {
                datatypeIconA = datatypeIcon;
            }
        }
        */

        /*
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            HObject theObject = (HObject) ((DefaultMutableTreeNode) value).getUserObject();
            
            if (theObject == null)
            	return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            boolean hasAttribute = theObject.hasAttribute();
            
            if (theObject instanceof Dataset) {
                if (theObject instanceof ScalarDS) {
                    ScalarDS sd = (ScalarDS) theObject;
                    if (sd.isImage()) {
                        if (hasAttribute) {
                            leafIcon = imageIconA;
                        }
                        else {
                            leafIcon = imageIcon;
                        }
                    }
                    else if (sd.isText()) {
                        if (hasAttribute) {
                            leafIcon = textIconA;
                        }
                        else {
                            leafIcon = textIcon;
                        }
                    }
                    else {
                        if (hasAttribute) {
                            leafIcon = datasetIconA;
                        }
                        else {
                            leafIcon = datasetIcon;
                        }

                    }
                }
                else if (theObject instanceof CompoundDS) {
                    if (hasAttribute) {
                        leafIcon = tableIconA;
                    }
                    else {
                        leafIcon = tableIcon;
                    }
                }
            }
            else if (theObject instanceof Group) {
                Group g = (Group) theObject;

                if (hasAttribute) {
                    openIcon = openFolderA;
                    closedIcon = closeFolderA;
                }
                else {
                    openIcon = openFolder;
                    closedIcon = closeFolder;
                }

                if (g.isRoot()) {
                    if (g.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                        openIcon = closedIcon = h5Icon;
                    }
                    else if (g.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
                        openIcon = closedIcon = h4Icon;
                    }
                }
            }
            else if (theObject instanceof Datatype) {
                Datatype t = (Datatype) theObject;

                if (hasAttribute) {
                    leafIcon = datatypeIconA;
                }
                else {
                    leafIcon = datatypeIcon;
                }
            }

            else {
                leafIcon = questionIcon;
            }

            return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    } // private class HTreeCellRenderer
    */

    /**
     * ChangeIndexingDialog displays file index options.
     */
    private class ChangeIndexingDialog extends Dialog {
        private static final long serialVersionUID = 1048114401768228742L;
    
        Object result;
        
        private Button checkIndexType;
        private Button checkIndexOrder;
        private Button checkIndexNative;
    
        private boolean reloadFile;
        
        private FileFormat selectedFile;
        private int indexType;
        private int indexOrder;
    
        /**
         * constructs an UserOptionsDialog.
         * 
         * @param view
         *            The HDFView.
         */
        private ChangeIndexingDialog(Shell parent, int style, FileFormat viewSelectedFile) {
            super(parent, style);
    
            selectedFile = viewSelectedFile;
            indexType = selectedFile.getIndexType(null);
            indexOrder = selectedFile.getIndexOrder(null);
            reloadFile = false;
    
            /*
            Shell contentPane = getParent();
            contentPane.setLayout(new BorderLayout(8, 8));
            contentPane.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
    
            JPanel indexP = new JPanel();
            TitledBorder tborder = new TitledBorder("Index Options");
            tborder.setTitleColor(Color.darkGray);
            indexP.setBorder(tborder);
            indexP.setLayout(new GridLayout(2, 1, 10, 10));
            indexP.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
            contentPane.add(indexP);
    
            JPanel pType = new JPanel();
            tborder = new TitledBorder("Indexing Type");
            tborder.setTitleColor(Color.darkGray);
            pType.setBorder(tborder);
            pType.setLayout(new GridLayout(1, 2, 8, 8));
            //checkIndexType = new JRadioButton("By Name", (indexType) == selectedFile.getIndexType("H5_INDEX_NAME"));
            //checkIndexType.setName("Index by Name");
            //pType.add(checkIndexType);
            //JRadioButton checkIndexCreateOrder = new JRadioButton("By Creation Order", (indexType) == selectedFile.getIndexType("H5_INDEX_CRT_ORDER"));
            //checkIndexCreateOrder.setName("Index by Creation Order");
            //pType.add(checkIndexCreateOrder);
            ButtonGroup bTypegrp = new ButtonGroup();
            //bTypegrp.add(checkIndexType);
            //bTypegrp.add(checkIndexCreateOrder);
            indexP.add(pType);
    
            JPanel pOrder = new JPanel();
            tborder = new TitledBorder("Indexing Order");
            tborder.setTitleColor(Color.darkGray);
            pOrder.setBorder(tborder);
            pOrder.setLayout(new GridLayout(1, 3, 8, 8));
            //checkIndexOrder = new JRadioButton("Increments", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_INC"));
            //checkIndexOrder.setName("Index Increments");
            //pOrder.add(checkIndexOrder);
            //JRadioButton checkIndexDecrement = new JRadioButton("Decrements", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_DEC"));
            //checkIndexDecrement.setName("Index Decrements");
            //pOrder.add(checkIndexDecrement);
            //checkIndexNative = new JRadioButton("Native", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_NATIVE"));
            //checkIndexNative.setName("Index Native");
            //pOrder.add(checkIndexNative);
            ButtonGroup bOrdergrp = new ButtonGroup();
            //bOrdergrp.add(checkIndexOrder);
            //bOrdergrp.add(checkIndexDecrement);
            //bOrdergrp.add(checkIndexNative);
            indexP.add(pOrder);
    
            JPanel buttonP = new JPanel();
            //JButton b = new JButton("Reload File");
            //b.setName("Reload File");
            //b.setActionCommand("Reload File");
            //b.addActionListener(this);
            //buttonP.add(b);
            //b = new JButton("Cancel");
            //b.setName("Cancel");
            //b.setActionCommand("Cancel");
            //b.addActionListener(this);
            //buttonP.add(b);
    
            contentPane.add("Center", indexP);
            contentPane.add("South", buttonP);
    
            // locate the parent dialog
            Point l = getParent().getLocation();
            l.x += 250;
            l.y += 80;
            setLocation(l);
            validate();
            pack();
            */
        }
        
        //public void actionPerformed(ActionEvent e) {
        //    String cmd = e.getActionCommand();
    
        //    if (cmd.equals("Reload File")) {
        //        setIndexOptions();
        //        setVisible(false);
        //    }
        //    else if (cmd.equals("Cancel")) {
        //        reloadFile = false;
        //        setVisible(false);
        //    }
        //}
    
        private void setIndexOptions() {
            //if (checkIndexType.isSelected())
            //    selectedFile.setIndexType(selectedFile.getIndexType("H5_INDEX_NAME"));
            //else
            //    selectedFile.setIndexType(selectedFile.getIndexType("H5_INDEX_CRT_ORDER"));
            //indexType = selectedFile.getIndexType(null);
            
            //if (checkIndexOrder.isSelected())
            //    selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_INC"));
            //else if (checkIndexNative.isSelected())
            //    selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_NATIVE"));
            //else
            //    selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_DEC"));
            //indexOrder = selectedFile.getIndexOrder(null);
            
            reloadFile = true;
        }
    
        public int getIndexType() {
            return indexType;
        }
    
        public int getIndexOrder() {
            return indexOrder;
        }
    
        public boolean isreloadFile() {
            return reloadFile;
        }
        
        public Object open() {
        	Shell parent = getParent();
        	Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        	shell.setText("Indexing options");
        	
        	// Creation code
        	
        	
        	shell.open();
        	Display display = parent.getDisplay();
        	while (!shell.isDisposed()) {
        		if (!display.readAndDispatch()) display.sleep();
        	}
        	return result;
        }
    }
}
