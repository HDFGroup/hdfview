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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

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
 * TreeView defines APIs for open a file and display the file structure in tree
 * structure.
 * </p>
 * 
 * <p>
 * TreeView uses folders and leaf nodes to represent groups and data objects in
 * the file. You can expand or collapse folders to navigate data objects in the
 * file.
 * </p>
 * 
 * <p>
 * From the TreeView, you can open data content or metadata of selected object.
 * You can selet object(s) to delete or add new object to the file.
 * </p>
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DefaultTreeView extends JPanel implements TreeView, ActionListener {
    private static final long            serialVersionUID    = 4092566164712521186L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultTreeView.class);

    /** the owner of this treeview */
    private ViewManager                  viewer;

    /**
     * The super root of tree: all open files start at this root.
     */
    private final DefaultMutableTreeNode root;

    /**
     * The tree which holds file structures.
     */
    private final JTree                  tree;

    /**
     * The tree model
     */
    private final DefaultTreeModel       treeModel;

    /** A list open files. */
    private final List<FileFormat>       fileList;

    private final Toolkit                toolkit;

    /** Selected file */
    private FileFormat                   selectedFile;

    /** The current selected node. */
    private DefaultMutableTreeNode       selectedNode;

    /** The current selected TreePath. */
    private TreePath                     selectedTreePath;

    /** the current selected object */
    private HObject                      selectedObject;

    /** flag to indicate if the dataset is displayed as default */
    private boolean                      isDefaultDisplay;

    /**
     * The popup menu used to display user choice of actions on data object.
     */
    private final JPopupMenu             popupMenu;

    private JSeparator                   separator;

    /** a list of editing GUI components */
    private List<JMenuItem>              editGUIs;

    /** the list of current selected objects */
    private List<Object>                 objectsToCopy;

    private JMenu                        exportDatasetMenu;

    private JMenuItem                    addTableMenuItem;

    private JMenuItem                    addDatasetMenuItem;

    private JMenuItem                    addDatatypeMenuItem;

    private JMenuItem                    addLinkMenuItem;

    private JMenuItem                    setLibVerBoundsItem;

    private JMenuItem                    changeIndexItem;

    private String                       currentSearchPhrase = null;

    private boolean                      moveFlag;

    private TreePath[]                   currentSelectionsForMove;

    private boolean                      isApplyBitmaskOnly  = false;

    private int                          currentIndexType;

    private int                          currentIndexOrder;

    private int                          binaryOrder;

    public DefaultTreeView(ViewManager theView) {
        viewer = theView;

        root = new DefaultMutableTreeNode() {
            private static final long serialVersionUID = -6829919815424470510L;

            public boolean isLeaf() {
                return false;
            }
        };

        fileList = new Vector<FileFormat>();
        toolkit = Toolkit.getDefaultToolkit();
        editGUIs = new Vector<JMenuItem>();
        objectsToCopy = null;
        isDefaultDisplay = true;
        selectedTreePath = null;
        selectedNode = null;
        moveFlag = false;
        currentSelectionsForMove = null;

        //addDatasetMenuItem = new JMenuItem("Dataset", ViewProperties.getDatasetIcon());
        addDatasetMenuItem.addActionListener(this);
        addDatasetMenuItem.setActionCommand("Add dataset");

        //addTableMenuItem = new JMenuItem("Compound DS", ViewProperties.getTableIcon());
        addTableMenuItem.addActionListener(this);
        addTableMenuItem.setActionCommand("Add table");

        //addDatatypeMenuItem = new JMenuItem("Datatype", ViewProperties.getDatatypeIcon());
        addDatatypeMenuItem.addActionListener(this);
        addDatatypeMenuItem.setActionCommand("Add datatype");

        //addLinkMenuItem = new JMenuItem("Link", ViewProperties.getLinkIcon());
        addLinkMenuItem.addActionListener(this);
        addLinkMenuItem.setActionCommand("Add link");

        setLibVerBoundsItem = new JMenuItem("Set Lib version bounds");
        setLibVerBoundsItem.addActionListener(this);
        setLibVerBoundsItem.setActionCommand("Set Lib version bounds");

        changeIndexItem = new JMenuItem("Change file indexing");
        changeIndexItem.addActionListener(this);
        changeIndexItem.setActionCommand("Change file indexing");

        // initialize the tree and root
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);

        tree.setLargeModel(true);
        tree.setCellRenderer(new HTreeCellRenderer());
        tree.addMouseListener(new HTreeMouseAdapter());
        tree.addKeyListener(new HTreeKeyAdapter());
        tree.setRootVisible(false);
        // tree.setShowsRootHandles(true);
        int rowheight = 23 + (int) ((tree.getFont().getSize() - 12) * 0.5);
        tree.setRowHeight(rowheight);

        // create the separator
        separator = new JPopupMenu.Separator();

        // create the popupmenu
        popupMenu = createPopupMenu();

        // reset the scroll increament
        // layout GUI component
        this.setLayout(new BorderLayout());
        this.add(tree, BorderLayout.CENTER);
    }

    /**
     * Insert a node into the tree.
     * 
     * @param node
     *            the node to insert.
     * @param pnode
     *            the parent node.
     */
    private void insertNode(TreeNode node, TreeNode pnode) {
        if ((node == null) || (pnode == null)) {
            return;
        }

        treeModel.insertNodeInto((DefaultMutableTreeNode) node, (DefaultMutableTreeNode) pnode, pnode.getChildCount());
    }

    /**
     * Checks if a file is already open.
     */
    private boolean isFileOpen(String filename) {
        boolean isOpen = false;

        // find the file by matching its file name and close the file
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

    /** creates a popup menu for a right mouse click on a data object */
    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item;

        item = new JMenuItem("Open");
        item.setMnemonic(KeyEvent.VK_O);
        item.addActionListener(this);
        item.setActionCommand("Open data");
        menu.add(item);

        item = new JMenuItem("Open As");
        item.setMnemonic(KeyEvent.VK_A);
        item.addActionListener(this);
        item.setActionCommand("Open data as");
        menu.add(item);

        menu.addSeparator();

        JMenu newOjbectMenu = new JMenu("New");
        menu.add(newOjbectMenu);
        editGUIs.add(newOjbectMenu);

        //item = new JMenuItem("Group", ViewProperties.getFoldercloseIcon());
        item.addActionListener(this);
        item.setActionCommand("Add group");
        newOjbectMenu.add(item);

        newOjbectMenu.add(addDatasetMenuItem);

        //item = new JMenuItem("Image", ViewProperties.getImageIcon());
        item.addActionListener(this);
        item.setActionCommand("Add image");
        newOjbectMenu.add(item);

        newOjbectMenu.add(addTableMenuItem);
        newOjbectMenu.add(addDatatypeMenuItem);
        newOjbectMenu.add(addLinkMenuItem);

        menu.addSeparator();

        item = new JMenuItem("Copy");
        item.setMnemonic(KeyEvent.VK_C);
        item.addActionListener(this);
        item.setActionCommand("Copy object");
        menu.add(item);

        item = new JMenuItem("Paste");
        item.setMnemonic(KeyEvent.VK_P);
        item.addActionListener(this);
        item.setActionCommand("Paste object");
        menu.add(item);
        editGUIs.add(item);

        item = new JMenuItem("Delete");
        item.setMnemonic(KeyEvent.VK_D);
        item.addActionListener(this);
        item.setActionCommand("Cut object");
        menu.add(item);
        editGUIs.add(item);

        item = new JMenuItem("Cut");
        item.setMnemonic(KeyEvent.VK_T);
        item.addActionListener(this);
        item.setActionCommand("Move object");
        menu.add(item);
        editGUIs.add(item);
        
        exportDatasetMenu = new JMenu("Export Dataset");
        menu.add(exportDatasetMenu);
        item = new JMenuItem("Export Data to Text File");
        item.addActionListener(this);
        item.setActionCommand("Save table as text");
        exportDatasetMenu.add(item);
    
        item = new JMenuItem("Export Data as Native Order");
        item.addActionListener(this);
        item.setActionCommand("Save table as binary Native Order");
        exportDatasetMenu.add(item);
        item = new JMenuItem("Export Data as Little Endian");
        item.addActionListener(this);
        item.setActionCommand("Save table as binary Little Endian");
        exportDatasetMenu.add(item);
        item = new JMenuItem("Export Data as Big Endian");
        item.addActionListener(this);
        item.setActionCommand("Save table as binary Big Endian");
        exportDatasetMenu.add(item);

        menu.addSeparator();

        item = new JMenuItem("Save to");
        item.setMnemonic(KeyEvent.VK_S);
        item.addActionListener(this);
        item.setActionCommand("Save object to file");
        menu.add(item);

        item = new JMenuItem("Rename");
        item.setMnemonic(KeyEvent.VK_R);
        item.addActionListener(this);
        item.setActionCommand("Rename object");
        menu.add(item);
        editGUIs.add(item);

        menu.addSeparator();

        item = new JMenuItem("Show Properties");
        item.addActionListener(this);
        item.setActionCommand("Show object properties");
        menu.add(item);

        item = new JMenuItem("Show Properties As");
        item.addActionListener(this);
        item.setActionCommand("Show object properties as");
        menu.add(item);

        menu.add(changeIndexItem);

        menu.addSeparator();

        item = new JMenuItem("Find");
        item.setMnemonic(KeyEvent.VK_F);
        item.addActionListener(this);
        item.setActionCommand("Find");
        menu.add(item);

        // item = new JMenuItem( "Find Next");
        // item.setMnemonic(KeyEvent.VK_N);
        // item.addActionListener(this);
        // item.setActionCommand("Find next");
        // menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("Expand All");
        item.addActionListener(this);
        item.setActionCommand("Expand all");
        menu.add(item);
        item = new JMenuItem("Collapse All");
        item.addActionListener(this);
        item.setActionCommand("Collapse all");
        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("Close File");
        item.setMnemonic(KeyEvent.VK_E);
        item.addActionListener(this);
        item.setActionCommand("Close file");
        menu.add(item);

        item = new JMenuItem("Reload File");
        // item.setMnemonic(KeyEvent.VK_R);
        item.addActionListener(this);
        item.setActionCommand("Reload file");
        menu.add(item);

        menu.add(separator);
        menu.add(setLibVerBoundsItem);

        return menu;
    }

    /** display the popupmenu of data properties */
    private void showPopupMenu(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        HObject selectedObject = ((HObject) (selectedNode.getUserObject()));
        boolean isReadOnly = selectedObject.getFileFormat().isReadOnly();

        setEnabled(editGUIs, !isReadOnly);

        boolean isWritable = !selectedObject.getFileFormat().isReadOnly();
        if (selectedObject instanceof Group) {
            popupMenu.getComponent(0).setEnabled(false); // "open" menuitem
            popupMenu.getComponent(1).setEnabled(false); // "open as" menuitem

            boolean state = !(((Group) selectedObject).isRoot());
            popupMenu.getComponent(5).setEnabled(state); // "Copy" menuitem
            popupMenu.getComponent(6).setEnabled(isWritable); // "Paste"
            // menuitem
            popupMenu.getComponent(7).setEnabled(state && isWritable); // "Delete"
            // menuitem
            popupMenu.getComponent(11).setEnabled(state); // "save to" menuitem
            popupMenu.getComponent(12).setEnabled(state && isWritable); // "rename"
            // menuitem
            popupMenu.getComponent(8).setEnabled(
                    (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))
                    && state && isWritable); // "cut" menuitem
        }
        else {
            popupMenu.getComponent(0).setEnabled(true);
            popupMenu.getComponent(1).setEnabled(true);
            popupMenu.getComponent(5).setEnabled(true); // "Copy" menuitem
            popupMenu.getComponent(6).setEnabled(isWritable); // "Paste"
            // menuitem
            popupMenu.getComponent(7).setEnabled(isWritable); // "Delete"
            // menuitem
            popupMenu.getComponent(11).setEnabled(true); // "save to" menuitem
            popupMenu.getComponent(12).setEnabled(isWritable); // "rename"
            // menuitem
            popupMenu.getComponent(8).setEnabled(
                    (selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)))
                    && isWritable); // "cut" menuitem
        }

        // adding table is only supported by HDF5
        if ((selectedFile != null) && selectedFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
            addDatasetMenuItem.setText("Dataset");
            addTableMenuItem.setVisible(true);
            addDatatypeMenuItem.setVisible(true);
            addLinkMenuItem.setVisible(true);
            boolean state = false;
            if ((selectedObject instanceof Group)) {
                state = (((Group) selectedObject).isRoot());
                separator.setVisible(isWritable && state);
                setLibVerBoundsItem.setVisible(isWritable && state); 
                // added only if it is HDF5format, iswritable & isroot
            }
            else {
                separator.setVisible(false);
                setLibVerBoundsItem.setVisible(false);
            }
            changeIndexItem.setVisible(state);
        }
        else {
            addDatasetMenuItem.setText("SDS");
            addTableMenuItem.setVisible(false);
            addDatatypeMenuItem.setVisible(false);
            addLinkMenuItem.setVisible(false);
            separator.setVisible(false);
            setLibVerBoundsItem.setVisible(false);
            changeIndexItem.setVisible(false);
        }
    
        // export table is only supported by HDF5
        if ((selectedObject != null) && selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
            if ((selectedObject instanceof Dataset)) {
                Dataset dataset = (Dataset) selectedObject;
                if ((dataset instanceof ScalarDS)) {
                    exportDatasetMenu.setVisible(true);
                }
            }
            else {
                exportDatasetMenu.setVisible(false);
            }
        }
        else {
            exportDatasetMenu.setVisible(false);
        }
    
        popupMenu.show((JComponent) e.getSource(), x, y);
    }

    /** disable/enable GUI components */
    private static void setEnabled(List<JMenuItem> list, boolean b) {
        Component item = null;
        Iterator<JMenuItem> it = list.iterator();
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
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Select a file to save.", "HDFView", JOptionPane.ERROR_MESSAGE);
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

        // since cannot pack hdf4, simple copy the whole phyisical file
        int length = 0;
        int bsize = 512;
        byte[] buffer;
        BufferedInputStream bi = null;
        BufferedOutputStream bo = null;

        try {
            bi = new BufferedInputStream(new FileInputStream(srcFile.getFilePath()));
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex.getMessage() + "\n" + filename, "HDFView", JOptionPane.ERROR_MESSAGE);
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
            	log.debug("output file force input close:", ex2);
            }
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
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
        	log.debug("output file:", ex);
        }
        try {
            bi.close();
        }
        catch (Exception ex) {
        	log.debug("input file:", ex);
        }
        try {
            bo.close();
        }
        catch (Exception ex) {
        	log.debug("output file:", ex);
        }

        try {
            openFile(filename, FileFormat.WRITE);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane .showMessageDialog(this, ex.getMessage() + "\n" + filename, "HDFView", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Copy the current file into a new file. The new file does not include the
     * inaccessible objects. Values of reference dataset are not updated in the
     * new file.
     */
    private void saveAsHDF5(FileFormat srcFile) {
        if (srcFile == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Select a file to save.", "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        TreeNode root = srcFile.getRootNode();
        if (root == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "The file is empty.", "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFrame owner = (viewer == null) ? new JFrame() : (JFrame) viewer;
        NewFileDialog dialog = new NewFileDialog(owner, srcFile.getParent(), FileFormat.FILE_TYPE_HDF5,
                getCurrentFiles());
        // dialog.show();

        if (!dialog.isFileCreated()) {
            return;
        }

        String filename = dialog.getFile();

        int n = root.getChildCount();
        Vector<Object> objList = new Vector<Object>(n);
        DefaultMutableTreeNode node = null;
        for (int i = 0; i < n; i++) {
            node = (DefaultMutableTreeNode) root.getChildAt(i);
            objList.add(node.getUserObject());
        }

        FileFormat newFile = null;
        try {
            newFile = openFile(filename, FileFormat.WRITE);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane .showMessageDialog(this, ex.getMessage() + "\n" + filename, "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (newFile == null) {
            return;
        }

        TreeNode pnode = newFile.getRootNode();

        pasteObject(objList, pnode, newFile);
        objList.setSize(0);

        Group srcGroup = (Group) ((DefaultMutableTreeNode) root).getUserObject();
        Group dstGroup = (Group) ((DefaultMutableTreeNode) newFile.getRootNode()).getUserObject();
        Object[] parameter = new Object[2];
        Class<?> classHOjbect = null;
        Class<?>[] parameterClass = new Class[2];
        Method method = null;

        // copy attributes of the root group
        try {
            parameter[0] = srcGroup;
            parameter[1] = dstGroup;
            classHOjbect = Class.forName("hdf.object.HObject");
            parameterClass[0] = parameterClass[1] = classHOjbect;
            method = newFile.getClass().getMethod("copyAttributes", parameterClass);
            method.invoke(newFile, parameter);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
        }

        // update reference datasets
        parameter[0] = srcGroup.getFileFormat();
        parameter[1] = newFile;
        parameterClass[0] = parameterClass[1] = parameter[0].getClass();
        try {
            method = newFile.getClass().getMethod("updateReferenceDataset", parameterClass);
            method.invoke(newFile, parameter);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** copy selected objects */
    private void copyObject() {
        objectsToCopy = getSelectedObjects();
        moveFlag = false;
    }

    /** move selected objects */
    private void moveObject() {
        objectsToCopy = getSelectedObjects();
        moveFlag = true;
        currentSelectionsForMove = tree.getSelectionPaths();
    }

    /** paste selected objects */
    private void pasteObject() {
        log.trace("pasteObject(): start");
        if (moveFlag == true) {
            HObject theObj = null;
            for (int i = 0; i < currentSelectionsForMove.length; i++) {
                DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelectionsForMove[i]
                        .getLastPathComponent());
                theObj = (HObject) currentNode.getUserObject();

                if (isObjectOpen(theObj)) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Cannot move the selected object: " + theObj
                            + "\nThe dataset or dataset in the group is in use."
                            + "\n\nPlease close the dataset(s) and try again.\n", "HDFView", JOptionPane.ERROR_MESSAGE);
                    moveFlag = false;
                    currentSelectionsForMove = null;
                    objectsToCopy = null;
                    return;
                }
            }
        }

        TreeNode pnode = selectedNode;

        if ((objectsToCopy == null) || (objectsToCopy.size() <= 0) || (pnode == null)) {
            return;
        }

        FileFormat srcFile = ((HObject) objectsToCopy.get(0)).getFileFormat();
        FileFormat dstFile = getSelectedFile();
        FileFormat h5file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
        FileFormat h4file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);

        if (srcFile == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Source file is null.", "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if (dstFile == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Destination file is null.", "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if (srcFile.isThisType(h4file) && dstFile.isThisType(h5file)) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Unsupported operation: cannot copy HDF4 object to HDF5 file",
                    "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if (srcFile.isThisType(h5file) && dstFile.isThisType(h4file)) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Unsupported operation: cannot copy HDF5 object to HDF4 file",
                    "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (moveFlag == true) {
            if (srcFile != dstFile) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Cannot move the selected object to different file", "HDFView",
                        JOptionPane.ERROR_MESSAGE);
                moveFlag = false;
                currentSelectionsForMove = null;
                objectsToCopy = null;
                return;
            }
        }

        if (pnode.isLeaf()) {
            pnode = pnode.getParent();
        }
        Group pgroup = (Group) ((DefaultMutableTreeNode) pnode).getUserObject();
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
            op = JOptionPane.showConfirmDialog(this, moveMsg, "Move object", JOptionPane.YES_NO_OPTION, msgType);
        }
        else {
            op = JOptionPane.showConfirmDialog(this, msg, "Copy object", JOptionPane.YES_NO_OPTION, msgType);
        }

        log.trace("pasteObject(): op={}", op);
        if (op == JOptionPane.NO_OPTION) {
            return;
        }

        pasteObject(objectsToCopy, pnode, dstFile);

        // objectsToCopy = null;
        if (moveFlag == true) {
            removeSelectedObjects();
            moveFlag = false;
            currentSelectionsForMove = null;
            objectsToCopy = null;
        }
        log.trace("pasteObject(): finish");
    }

    /** paste selected objects */
    private void pasteObject(List<Object> objList, TreeNode pnode, FileFormat dstFile) {
        if ((objList == null) || (objList.size() <= 0) || (pnode == null)) {
            return;
        }

        ((HObject) objList.get(0)).getFileFormat();
        Group pgroup = (Group) ((DefaultMutableTreeNode) pnode).getUserObject();
        log.trace("pasteObject(...): start");

        HObject theObj = null;
        TreeNode newNode = null;
        Iterator<Object> iterator = objList.iterator();
        while (iterator.hasNext()) {
            newNode = null;
            theObj = (HObject) iterator.next();

            if ((theObj instanceof Group) && ((Group) theObj).isRoot()) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Unsupported operation: cannot copy the root group", "HDFView",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // check if it creates infinite loop
            Group pg = pgroup;
            while (!pg.isRoot()) {
                if (theObj.equals(pg)) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Unsupported operation: cannot copy a group to itself.",
                            "HDFView", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                pg = pg.getParent();
            }

            try {
                log.trace("pasteObject(...): dstFile.copy(theObj, pgroup, null)");
                newNode = dstFile.copy(theObj, pgroup, null);
            }
            catch (Exception ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
                // newNode = null;
            }

            // add the node to the tree
            if (newNode != null) {
                insertNode(newNode, pnode);
            }

        } // while (iterator.hasNext())
        log.trace("pasteObject(...): finish");
    }

    private void removeSelectedObjects() {
        FileFormat theFile = getSelectedFile();
        if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Unsupported operation: cannot delete HDF4 object.", "HDFView",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        TreePath[] currentSelections = tree.getSelectionPaths();

        if (moveFlag == true) {
            currentSelections = currentSelectionsForMove;
        }
        if ((currentSelections == null) || (currentSelections.length <= 0)) {
            return;
        }
        if (moveFlag != true) {
            int op = JOptionPane.showConfirmDialog(this, "Do you want to remove all the selected object(s) ?",
                    "Remove object", JOptionPane.YES_NO_OPTION);

            if (op == JOptionPane.NO_OPTION) {
                return;
            }
        }
        HObject theObj = null;
        for (int i = 0; i < currentSelections.length; i++) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelections[i].getLastPathComponent());
            theObj = (HObject) currentNode.getUserObject();

            // cannot delete root
            if (theObj instanceof Group) {
                Group g = (Group) theObj;
                if (g.isRoot()) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Unsupported operation: cannot delete the file root.",
                            "HDFView", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            if (moveFlag != true) {
                if (isObjectOpen(theObj)) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Cannot delete the selected object: " + theObj
                            + "\nThe dataset or dataset in the group is in use."
                            + "\n\nPlease close the dataset(s) and try again.\n", "HDFView", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
            }

            try {
                theFile.delete(theObj);
            }
            catch (Exception ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            if (theObj.equals(selectedObject)) {
                selectedObject = null;
            }

            removeNode(currentNode);
        } // for (int i=0; i< currentSelections.length; i++) {
    }

    private void removeNode(DefaultMutableTreeNode node) {
        if (node == null) {
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) (node.getParent());
        if (parentNode != null) {
            treeModel.removeNodeFromParent(node);

            // add the two lines to fix bug in HDFView 1.2. Delete a subgroup
            // and
            // then copy the group to another group, the deleted group still
            // exists.
            Group pgroup = (Group) parentNode.getUserObject();
            pgroup.removeFromMemberList((HObject) node.getUserObject());

            if (node.equals(selectedNode)) {
                selectedNode = null;
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
    private final List<Object> breadthFirstUserObjects(TreeNode node) {
        if (node == null) {
            return null;
        }

        Vector<Object> list = new Vector<Object>();
        DefaultMutableTreeNode theNode = null;
        Enumeration<?> local_enum = ((DefaultMutableTreeNode) node).breadthFirstEnumeration();
        while (local_enum.hasMoreElements()) {
            theNode = (DefaultMutableTreeNode) local_enum.nextElement();
            list.add(theNode.getUserObject());
        }

        return list;
    }

    /**
     * Find first object that is matched by name.
     * 
     * @param objName
     *            -- the object name.
     * @return the object if found, otherwise, returns null.
     */
    private final static HObject find(String objName, TreePath treePath, JTree tree) {
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

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if (node == null) return null;

        HObject obj = null;
        String theName = null;
        DefaultMutableTreeNode theNode = null;
        Enumeration<?> local_enum = node.breadthFirstEnumeration();
        while (local_enum.hasMoreElements()) {
            theNode = (DefaultMutableTreeNode) local_enum.nextElement();
            obj = (HObject) theNode.getUserObject();
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
            TreePath dstPath = getTreePath(treePath, theNode, 0);

            // tree.fireTreeExpanded(dstPath) ;
            tree.setSelectionPath(dstPath);
            tree.scrollPathToVisible(dstPath);
        }

        return retObj;
    }

    /**
     * Get the TreePath from the parent to the target node.
     * 
     * @param parent
     *            -- the parent TreePath
     * @param node
     *            -- the target node
     * @param depth
     * @return the tree path if target node found, otherwise; returns null;
     */
    private static TreePath getTreePath(TreePath parent, TreeNode node, int depth) {
        if (node == null || parent == null || depth < 0) return null;

        TreeNode theNode = (TreeNode) parent.getLastPathComponent();
        if (node == theNode) return parent;

        if (theNode.getChildCount() >= 0) {
            for (Enumeration<?> e = theNode.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                TreePath result = getTreePath(path, node, depth + 1);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private void addGroup() {
        if ((selectedObject == null) || (selectedNode == null)) {
            return;
        }

        Group pGroup = null;
        if (selectedObject instanceof Group) {
            pGroup = (Group) selectedObject;
        }
        else {
            pGroup = (Group) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
        }

        NewGroupDialog dialog = new NewGroupDialog((JFrame) viewer, pGroup, 
                breadthFirstUserObjects(selectedObject.getFileFormat().getRootNode()));
        dialog.setVisible(true);

        HObject obj = (HObject) dialog.getObject();
        if (obj == null) {
            return;
        }

        Group pgroup = dialog.getParentGroup();
        try {
            this.addObject(obj, pgroup);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void addDataset() {
        if ((selectedObject == null) || (selectedNode == null)) {
            return;
        }

        Group pGroup = null;
        if (selectedObject instanceof Group) {
            pGroup = (Group) selectedObject;
        }
        else {
            pGroup = (Group) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
        }

        NewDatasetDialog dialog = new NewDatasetDialog((JFrame) viewer, pGroup, 
                breadthFirstUserObjects(selectedObject.getFileFormat().getRootNode()));
        dialog.setVisible(true);

        HObject obj = (HObject) dialog.getObject();
        if (obj == null) {
            return;
        }

        Group pgroup = dialog.getParentGroup();
        try {
            addObject(obj, pgroup);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void addImage() {
        if ((selectedObject == null) || (selectedNode == null)) {
            return;
        }

        Group pGroup = null;
        if (selectedObject instanceof Group) {
            pGroup = (Group) selectedObject;
        }
        else {
            pGroup = (Group) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
        }

        NewImageDialog dialog = new NewImageDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject
                .getFileFormat().getRootNode()));
        dialog.setVisible(true);

        HObject obj = (HObject) dialog.getObject();
        if (obj == null) {
            return;
        }

        Group pgroup = dialog.getParentGroup();
        try {
            this.addObject(obj, pgroup);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void addTable() {
        if ((selectedObject == null) || (selectedNode == null)) {
            return;
        }

        Group pGroup = null;
        if (selectedObject instanceof Group) {
            pGroup = (Group) selectedObject;
        }
        else {
            pGroup = (Group) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
        }

        NewTableDataDialog dialog = new NewTableDataDialog((JFrame) viewer, pGroup,
                breadthFirstUserObjects(selectedObject.getFileFormat().getRootNode()));
        dialog.setVisible(true);

        HObject obj = (HObject) dialog.getObject();
        if (obj == null) {
            return;
        }

        Group pgroup = dialog.getParentGroup();
        try {
            addObject(obj, pgroup);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void addDatatype() {
        if ((selectedObject == null) || (selectedNode == null)) {
            return;
        }

        Group pGroup = null;
        if (selectedObject instanceof Group) {
            pGroup = (Group) selectedObject;
        }
        else {
            pGroup = (Group) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
        }

        NewDatatypeDialog dialog = new NewDatatypeDialog((JFrame) viewer, pGroup,
                breadthFirstUserObjects(selectedObject.getFileFormat().getRootNode()));
        dialog.setVisible(true);

        HObject obj = (HObject) dialog.getObject();
        if (obj == null) {
            return;
        }

        Group pgroup = dialog.getParentGroup();
        try {
            addObject(obj, pgroup);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void addLink() {
        if ((selectedObject == null) || (selectedNode == null)) {
            return;
        }

        Group pGroup = null;
        if (selectedObject instanceof Group) {
            pGroup = (Group) selectedObject;
        }
        else {
            pGroup = (Group) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
        }

        NewLinkDialog dialog = new NewLinkDialog((JFrame) viewer, pGroup, breadthFirstUserObjects(selectedObject
                .getFileFormat().getRootNode()));
        dialog.setVisible(true);

        HObject obj = (HObject) dialog.getObject();
        if (obj == null) {
            return;
        }

        Group pgroup = dialog.getParentGroup();
        try {
            addObject(obj, pgroup);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    /** Save data as file. */
    private void saveAsFile() throws Exception {
        if (!(selectedObject instanceof Dataset) || (selectedObject == null) || (selectedNode == null)) {
            return;
        }
        Dataset dataset = (Dataset) selectedObject;
        final JFileChooser fchooser = new JFileChooser(dataset.getFile());
        fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
        // fchooser.changeToParentDirectory();
        File choosedFile = null;
        
        if(binaryOrder == 99) {
            fchooser.setDialogTitle("Save Dataset Data To Text File --- " + dataset.getName());
    
            choosedFile = new File(dataset.getName() + ".txt");
        }
        else {
            fchooser.setDialogTitle("Save Current Data To Binary File --- " + dataset.getName());

            choosedFile = new File(dataset.getName() + ".bin");
        }

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
        List<?> fileList = viewer.getTreeView().getCurrentFiles();
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator<?> iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, 
                            "Unable to save data to file \"" + fname + "\". \nThe file is being used.", 
                            "Export Dataset", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        if (choosedFile.exists()) {
            int newFileFlag = JOptionPane.showConfirmDialog(this, 
                    "File exists. Do you want to replace it ?",
                    "Export Dataset", JOptionPane.YES_NO_OPTION);
            if (newFileFlag == JOptionPane.NO_OPTION) {
                return;
            }
        }

        boolean isH4 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        if (isH4) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Cannot export HDF4 object.", "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            selectedObject.getFileFormat().exportDataset(fname, dataset.getFile(), dataset.getFullName(), binaryOrder);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "HDFView", JOptionPane.ERROR_MESSAGE);
        }

        viewer.showStatus("Data save to: " + fname);
    }

    private void renameObject() {
        if (selectedObject == null) {
            return;
        }

        if ((selectedObject instanceof Group) && ((Group) selectedObject).isRoot()) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Cannot rename the root.", "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isH4 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        if (isH4) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Cannot rename HDF4 object.", "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String oldName = selectedObject.getName();
        String newName = (String) JOptionPane.showInputDialog(this, "Rename \"" + oldName + "\" to:", "Rename...",
                JOptionPane.INFORMATION_MESSAGE, null, null, oldName);

        if (newName == null) {
            return;
        }

        newName = newName.trim();
        if ((newName == null) || (newName.length() == 0) || newName.equals(oldName)) {
            return;
        }

        try {
            selectedObject.setName(newName);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "HDFView", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("rawtypes")
    private void setLibVersionBounds() {
        Object[] lowValues = { "Earliest", "Latest" };
        Object[] highValues = { "Latest" };
        JComboBox lowComboBox = new JComboBox(lowValues);
        lowComboBox.setName("earliestversion");
        JComboBox highComboBox = new JComboBox(highValues);
        highComboBox.setName("latestversion");

        Object[] msg = { "Earliest Version:", lowComboBox, "Latest Version:", highComboBox };
        Object[] options = { "Ok", "Cancel" };
        JOptionPane op = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options);

        op.setName("libselect");
        JDialog dialog = op.createDialog(this, "Set the library version bounds: ");
        dialog.setVisible(true);

        String result = null;
        try {
            result = (String) op.getValue();
        }
        catch (Exception err) {
            // err.printStackTrace();
        }

        if ((result != null) && (result.equals("Ok"))) {
            int low = -1;
            int high = 1;
            if ((lowComboBox.getSelectedItem()).equals("Earliest"))
                low = 0;
            else
                low = 1;
            try {
                selectedObject.getFileFormat().setLibBounds(low, high);
            }
            catch (Throwable err) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, err, "Error when setting lib version bounds",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        else
            return;
    }

    // Implementing java.io.ActionListener
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Close file")) {
            //((HDFView) viewer).actionPerformed(e);
        }
        else if (cmd.equals("Reload file")) {
            //((HDFView) viewer).actionPerformed(e);
        }
        else if (cmd.equals("Add group")) {
            addGroup();
        }
        else if (cmd.equals("Add dataset")) {
            addDataset();
        }
        else if (cmd.equals("Add image")) {
            addImage();
        }
        else if (cmd.equals("Add table")) {
            addTable();
        }
        else if (cmd.equals("Add datatype")) {
            addDatatype();
        }
        else if (cmd.equals("Add link")) {
            addLink();
        }
        else if (cmd.equals("Save table as text")) {
            binaryOrder = 99;
            try {
                saveAsFile();
            }
            catch (Exception ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog((JFrame) viewer, ex, "Export Dataset", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (cmd.startsWith("Save table as binary")) {
            if (cmd.equals("Save table as binary Native Order")) binaryOrder = 1;
            if (cmd.equals("Save table as binary Little Endian")) binaryOrder = 2;
            if (cmd.equals("Save table as binary Big Endian")) binaryOrder = 3;
            try {
                saveAsFile();
            }
            catch (Exception ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog((JFrame) viewer, ex, "Export Dataset", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (cmd.startsWith("Open data")) {
            if (cmd.equals("Open data")) {
                isDefaultDisplay = true;
            }
            else {
                isDefaultDisplay = false;
            }

            try {
                showDataContent(selectedObject);
            }
            catch (Throwable err) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, err, "HDFView", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        else if (cmd.equals("Copy object")) {
            copyObject();
        }
        else if (cmd.equals("Paste object")) {
            pasteObject();
        }
        else if (cmd.equals("Cut object")) {
            removeSelectedObjects();
        }
        else if (cmd.equals("Move object")) {
            moveObject();
        }
        else if (cmd.equals("Save object to file")) {
            if (selectedObject == null) {
                return;
            }

            if ((selectedObject instanceof Group) && ((Group) selectedObject).isRoot()) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this,
                        "Cannot save the root group.\nUse \"Save As\" from file menu to save the whole file",
                        "HDFView", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String filetype = FileFormat.FILE_TYPE_HDF4;
            boolean isH5 = selectedObject.getFileFormat().isThisType(
                    FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
            if (isH5) {
                filetype = FileFormat.FILE_TYPE_HDF5;
            }

            NewFileDialog dialog = new NewFileDialog((JFrame) viewer, selectedObject.getFileFormat().getParent(),
                    filetype, fileList);
            // dialog.show();

            if (!dialog.isFileCreated()) {
                return;
            }

            String filename = dialog.getFile();
            FileFormat dstFile = null;
            try {
                dstFile = openFile(filename, FileFormat.WRITE);
            }
            catch (Exception ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, ex.getMessage() + "\n" + filename, "HDFView",
                        JOptionPane.ERROR_MESSAGE);
            }
            List<Object> objList = new Vector<Object>(2);
            objList.add(selectedObject);
            pasteObject(objList, dstFile.getRootNode(), dstFile);
        }
        else if (cmd.equals("Rename object")) {
            renameObject();
        }
        else if (cmd.startsWith("Show object properties")) {
            if (cmd.equals("Show object properties")) {
                isDefaultDisplay = true;
            }
            else {
                isDefaultDisplay = false;
            }

            try {
                showMetaData(selectedObject);
            }
            catch (Exception ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, ex, "HDFView", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (cmd.startsWith("Find")) {
            if (cmd.equals("Find")) {
                String findStr = currentSearchPhrase;
                if (findStr == null) findStr = "";

                findStr = (String) JOptionPane.showInputDialog(this, "Find (e.g. O3Quality, O3*, or *Quality):",
                        "Find Object by Name", JOptionPane.PLAIN_MESSAGE, null, null, findStr);

                if (findStr != null && findStr.length() > 0) currentSearchPhrase = findStr;
            }

            find(currentSearchPhrase, selectedTreePath, tree);
        }
        else if (cmd.startsWith("Expand all")) {
            int row = 0;
            while (row < tree.getRowCount()) {
                tree.expandRow(row);
                row++;
            }
        }
        else if (cmd.startsWith("Collapse all")) {
            int row = tree.getRowCount() - 1;
            while (row >= 0) {
                tree.collapseRow(row);
                row--;
            }
        }
        else if (cmd.startsWith("Set Lib version bounds")) {
            setLibVersionBounds();
        }
        else if (cmd.startsWith("Change file indexing")) {
            ChangeIndexingDialog dialog = new ChangeIndexingDialog((JFrame) viewer, selectedFile);
            dialog.setVisible(true);
            if (dialog.isreloadFile()) {
                selectedFile.setIndexType(dialog.getIndexType());
                selectedFile.setIndexOrder(dialog.getIndexOrder());
                ((HDFView) viewer).reloadFile();
            }
        }
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
        MutableTreeNode fileRoot = null;
        boolean bNewFile = (FileFormat.OPEN_NEW == (accessID & FileFormat.OPEN_NEW));
        if(bNewFile)
            accessID = accessID - FileFormat.OPEN_NEW;
        
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

        if (fileFormat == null) {
            throw new java.io.IOException("Unsupported fileformat - " + filename);
        }

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
            fileRoot = (MutableTreeNode) fileFormat.getRootNode();
            if (fileRoot != null) {
                insertNode(fileRoot, root);

                int currentRowCount = tree.getRowCount();
                if (currentRowCount > 0) {
                    tree.expandRow(tree.getRowCount() - 1);
                }

                fileList.add(fileFormat);
            }       	
        }

        return fileFormat;
    }

    /**
     * close a file
     * 
     * @param file
     *            the file to close
     */
    public void closeFile(FileFormat file) throws Exception {
        if (file == null) {
            return;
        }

        // find the file node in the tree and removed it from the tree first
        FileFormat theFile = null;
        DefaultMutableTreeNode theNode = null;
        Enumeration<?> enumeration = root.children();
        while (enumeration.hasMoreElements()) {
            theNode = (DefaultMutableTreeNode) enumeration.nextElement();
            Group g = (Group) theNode.getUserObject();
            theFile = g.getFileFormat();

            if (theFile.equals(file)) {
                treeModel.removeNodeFromParent(theNode);
                try {
                    theFile.close();
                }
                catch (Exception ex) {
                	log.debug("close {}:", theFile.getFilePath(), ex);
                }
                fileList.remove(theFile);
                if (theFile.equals(selectedFile)) {
                    selectedFile = null;
                    selectedNode = null;
                }
                break;
            }
        } // while(enumeration.hasMoreElements())
    }

    /**
     * save a file
     * 
     * @param file
     *            the file to save
     */
    public void saveFile(FileFormat file) throws Exception {
        if (file == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Select a file to save.", "HDFView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isH4 = file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        boolean isH5 = file.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        if (!(isH4 || isH5)) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Saving file is not supported for this file type", "HDFView",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // write the change of the data into file before save the file
        /*List<?> views = ((HDFView) viewer).getDataViews();
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
        }*/

        if (isH5) {
            saveAsHDF5(file);
        }
        else if (isH4) {
            saveAsHDF4(file);
        }
    }

    /**
     * Gets the selected the file. When multiple files are open, we need to know
     * which file is currently selected.
     * 
     * @return the FileFormat of the selected file.
     */
    public FileFormat getSelectedFile() {
        return selectedFile;
    }

    /**
     * Gets a list of selected object in the tree. Obtaining a list of current
     * selected objects is necessary for copy/paste/delete objects.
     * 
     * @return a list of selected object in the tree.
     */
    public List<Object> getSelectedObjects() {
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

        if (d.getRank() <= 0) {
            d.init();
        }
        boolean isText = ((d instanceof ScalarDS) && ((ScalarDS) d).isText());
        boolean isImage = ((d instanceof ScalarDS) && ((ScalarDS) d).isImage());
        boolean isDisplayTypeChar = false;
        boolean isTransposed = false;
        boolean isIndexBase1 = ViewProperties.isIndexBase1();
        BitSet bitmask = null;
        String dataViewName = null;
        log.trace("showDataContent: inited");

        JInternalFrame theFrame = (JInternalFrame) viewer.getDataView(d);

        if (isDefaultDisplay) {

            if (theFrame != null) {
                theFrame.toFront();
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

        // enables use of JHDF5 in JNLP (Web Start) applications, the system
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

        // use default dataview
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
            className = (String) JOptionPane.showInputDialog(this, "Select MetaDataView", "HDFView",
                    JOptionPane.INFORMATION_MESSAGE, null, metaDataViewList.toArray(), className);
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
        if ((newObject == null) || (parentGroup == null)) {
            return;
        }

        TreeNode pnode = findTreeNode(parentGroup);
        TreeNode newnode = null;
        if (newObject instanceof Group) {
            newnode = new DefaultMutableTreeNode(newObject) {
                private static final long serialVersionUID = -8852535261445958398L;

                public boolean isLeaf() {
                    return false;
                }
            };
        }
        else {
            newnode = new DefaultMutableTreeNode(newObject);
        }

        treeModel.insertNodeInto((DefaultMutableTreeNode) newnode, (DefaultMutableTreeNode) pnode,
                pnode.getChildCount());
    }

    /**
     * Returns the JTree which holds the file structure.
     * 
     * @return the JTree which holds the file structure.
     */
    public JTree getTree() {
        return tree;
    }

    /**
     * Returns the list of current open files..
     */
    public List<FileFormat> getCurrentFiles() {
        return fileList;
    }

    /**
     * Returns the tree node that contains the given data object.
     */
    public TreeNode findTreeNode(HObject obj) {
        if (obj == null) {
            return null;
        }

        TreeNode theFileRoot = obj.getFileFormat().getRootNode();
        if (theFileRoot == null) {
            return null;
        }

        DefaultMutableTreeNode theNode = null;
        HObject theObj = null;
        Enumeration<?> local_enum = ((DefaultMutableTreeNode) theFileRoot).breadthFirstEnumeration();
        while (local_enum.hasMoreElements()) {
            theNode = (DefaultMutableTreeNode) local_enum.nextElement();
            theObj = (HObject) theNode.getUserObject();
            if (theObj == null) {
                continue;
            }
            else if (theObj.equals(obj)) {
                return theNode;
            }
        }

        return null;
    }

    /**
     * This class is used to change the default icons for tree nodes.
     * 
     * @see javax.swing.tree.DefaultTreeCellRenderer
     */
    private class HTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = -9030708781106435297L;
        private Icon              h4Icon, h5Icon, datasetIcon, imageIcon, tableIcon, textIcon, openFolder, closeFolder,
        datasetIconA, imageIconA, tableIconA, textIconA, openFolderA, closeFolderA, datatypeIcon,
        datatypeIconA, questionIcon;

        private HTreeCellRenderer() {
            super();

            /*
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
			*/

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

    /**
     * Handle mouse clicks on data object in the tree view. A right mouse-click
     * to show the popup menu for user choice. A double left-mouse-click to
     * display the data content. A single left-mouse-click to select the current
     * data object.
     */
    private class HTreeMouseAdapter extends MouseAdapter {
        // public void mousePressed(MouseEvent e)
        public void mouseReleased(MouseEvent e) {
            TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
            if (selPath == null) {
                return;
            }

            DefaultMutableTreeNode theNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
            if (!theNode.equals(selectedNode)) {
                selectedTreePath = selPath;
                selectedNode = theNode;
                selectedObject = ((HObject) (selectedNode.getUserObject()));
                FileFormat theFile = selectedObject.getFileFormat();
                if ((theFile != null) && !theFile.equals(selectedFile)) {
                    // a different file is selected, handle only one file a time
                    selectedFile = theFile;
                    tree.clearSelection();
                    tree.setSelectionPath(selPath);
                }

                viewer.mouseEventFired(e);
            }

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
            int eMod = e.getModifiers();
            if (e.isPopupTrigger()
                    || (eMod == MouseEvent.BUTTON3_MASK)
                    || (System.getProperty("os.name").startsWith("Mac") && (eMod == (MouseEvent.BUTTON1_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())))) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());

                if (!tree.isRowSelected(selRow)) {
                    // reselect the node
                    tree.clearSelection();
                    tree.setSelectionRow(selRow);
                }
                showPopupMenu(e);
            }
            // double click to open data content
            else if (e.getClickCount() == 2) {
                isDefaultDisplay = true;
                try {
                    showDataContent(selectedObject);
                }
                catch (Exception ex) {
                }
            }
        } // public void mousePressed(MouseEvent e)
    } // private class HTreeMouseAdapter extends MouseAdapter

    /**
     * Handle key pressed event.
     */
    private class HTreeKeyAdapter extends KeyAdapter {

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_KP_LEFT || key == KeyEvent.VK_KP_RIGHT || key == KeyEvent.VK_KP_UP
                    || key == KeyEvent.VK_KP_DOWN || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT
                    || key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {

                TreePath selPath = ((JTree) e.getComponent()).getSelectionPath();
                if (selPath == null) {
                    return;
                }

                DefaultMutableTreeNode theNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();

                if (!theNode.equals(selectedNode)) {
                    selectedTreePath = selPath;
                    selectedNode = theNode;
                    selectedObject = ((HObject) (selectedNode.getUserObject()));
                    FileFormat theFile = selectedObject.getFileFormat();
                    if ((theFile != null) && !theFile.equals(selectedFile)) {
                        // a different file is selected, handle only one file a
                        // time
                        selectedFile = theFile;
                        tree.clearSelection();
                        tree.setSelectionPath(selPath);
                    }

                    //((HDFView) viewer).showMetaData(selectedObject);
                }
            }
        }
    }

    /**
     * ChangeIndexingDialog displays file index options.
     */
    private class ChangeIndexingDialog extends JDialog implements ActionListener {
        private static final long serialVersionUID = 1048114401768228742L;
    
        private JRadioButton checkIndexType;
        private JRadioButton checkIndexOrder;
        private JRadioButton checkIndexNative;
    
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
        private ChangeIndexingDialog(JFrame view, FileFormat viewSelectedFile) {
            super(view, "Index Options", true);
    
            selectedFile = viewSelectedFile;
            indexType = selectedFile.getIndexType(null);
            indexOrder = selectedFile.getIndexOrder(null);
            reloadFile = false;
    
            JPanel contentPane = (JPanel) getContentPane();
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
            checkIndexType = new JRadioButton("By Name", (indexType) == selectedFile.getIndexType("H5_INDEX_NAME"));
            checkIndexType.setName("Index by Name");
            pType.add(checkIndexType);
            JRadioButton checkIndexCreateOrder = new JRadioButton("By Creation Order", (indexType) == selectedFile.getIndexType("H5_INDEX_CRT_ORDER"));
            checkIndexCreateOrder.setName("Index by Creation Order");
            pType.add(checkIndexCreateOrder);
            ButtonGroup bTypegrp = new ButtonGroup();
            bTypegrp.add(checkIndexType);
            bTypegrp.add(checkIndexCreateOrder);
            indexP.add(pType);
    
            JPanel pOrder = new JPanel();
            tborder = new TitledBorder("Indexing Order");
            tborder.setTitleColor(Color.darkGray);
            pOrder.setBorder(tborder);
            pOrder.setLayout(new GridLayout(1, 3, 8, 8));
            checkIndexOrder = new JRadioButton("Increments", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_INC"));
            checkIndexOrder.setName("Index Increments");
            pOrder.add(checkIndexOrder);
            JRadioButton checkIndexDecrement = new JRadioButton("Decrements", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_DEC"));
            checkIndexDecrement.setName("Index Decrements");
            pOrder.add(checkIndexDecrement);
            checkIndexNative = new JRadioButton("Native", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_NATIVE"));
            checkIndexNative.setName("Index Native");
            pOrder.add(checkIndexNative);
            ButtonGroup bOrdergrp = new ButtonGroup();
            bOrdergrp.add(checkIndexOrder);
            bOrdergrp.add(checkIndexDecrement);
            bOrdergrp.add(checkIndexNative);
            indexP.add(pOrder);
    
            JPanel buttonP = new JPanel();
            JButton b = new JButton("Reload File");
            b.setName("Reload File");
            b.setActionCommand("Reload File");
            b.addActionListener(this);
            buttonP.add(b);
            b = new JButton("Cancel");
            b.setName("Cancel");
            b.setActionCommand("Cancel");
            b.addActionListener(this);
            buttonP.add(b);
    
            contentPane.add("Center", indexP);
            contentPane.add("South", buttonP);
    
            // locate the parent dialog
            Point l = getParent().getLocation();
            l.x += 250;
            l.y += 80;
            setLocation(l);
            validate();
            pack();
        }
    
        public void setVisible(boolean b) {
            super.setVisible(b);
        }
    
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
    
            if (cmd.equals("Reload File")) {
                setIndexOptions();
                setVisible(false);
            }
            else if (cmd.equals("Cancel")) {
                reloadFile = false;
                setVisible(false);
            }
        }
    
        private void setIndexOptions() {
            if (checkIndexType.isSelected())
                selectedFile.setIndexType(selectedFile.getIndexType("H5_INDEX_NAME"));
            else
                selectedFile.setIndexType(selectedFile.getIndexType("H5_INDEX_CRT_ORDER"));
            indexType = selectedFile.getIndexType(null);
            
            if (checkIndexOrder.isSelected())
                selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_INC"));
            else if (checkIndexNative.isSelected())
                selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_NATIVE"));
            else
                selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_DEC"));
            indexOrder = selectedFile.getIndexOrder(null);
            
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
    }
}
