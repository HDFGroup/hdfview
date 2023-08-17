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

package hdf.view.TreeView;

import java.util.List;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.view.DataView.DataView;
import hdf.view.MetaDataView.MetaDataView;

/**
 * TreeView defines APIs for opening a file and displaying the file structure in
 * a tree structure.
 *
 * TreeView uses folders and leaf nodes to represent groups and data objects in
 * the file. You can expand or collapse folders to navigate data objects in the
 * file.
 *
 * From the TreeView, you can open the data content or metadata of the selected
 * object. You can select object(s) to delete or add new objects to the file.
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public abstract interface TreeView {
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
     * @return the FileFormat of this file if successful; otherwise returns
     *         null.
     *
     * @throws Exception if a failure occurred
     */
    public abstract FileFormat openFile(String filename, int accessID) throws Exception;
    /**
     * Reopens a file and retrieves the file structure of the file.
     *
     * Subclasses must implement this function to take appropriate steps to re-open
     * a file.
     *
     * @param theFile
     *            the file to re-open.
     * @param newFileAccessMode
     *            identifier for the new file access. Valid value of newFileAccessMode is:
     *            <ul>
     *            <li>FileFormat.READ --- allow read-only access to file.</li>
     *            <li>FileFormat.WRITE --- allow read and write access to file.</li>
     *            <li>FileFormat.CREATE --- create a new file.</li>
     *            </ul>
     *
     * @return the FileFormat of this file if successful; otherwise returns
     *         null.
     *
     * @throws Exception if a failure occurred
     */
    public abstract FileFormat reopenFile(FileFormat theFile, int newFileAccessMode) throws Exception;

    /**
     * close a file
     *
     * @param file
     *            the file to close
     *
     * @throws Exception if a failure occurred
     */
    public abstract void closeFile(FileFormat file) throws Exception;

    /**
     * save a file
     *
     * @param file
     *            the file to save
     *
     * @throws Exception if a failure occurred
     */
    public abstract void saveFile(FileFormat file) throws Exception;

    /**
     * change the display option.
     *
     * @param displaymode
     *            the default displaymode
     */
    public abstract void setDefaultDisplayMode(boolean displaymode);

    /**
     * Gets the selected the file. When multiple files are open, we need to know
     * which file is currently selected.
     *
     * @return the FileFormat of the selected file.
     */
    public abstract FileFormat getSelectedFile();

    /**
     * @return the current selected object in the tree.
     */
    public abstract HObject getCurrentObject();

    /**
     * Display the content of a data object.
     *
     * @param dataObject
     *            the data object
     *
     * @return the dataview that displays the data content
     *
     * @throws Exception if a failure occurred
     */
    public abstract DataView showDataContent(HObject dataObject)
            throws Exception;

    /**
     * Adds an already created HObject to the tree under the
     * TreeItem containing the specified parent group.
     *
     * @param newObject
     *            the object to add.
     * @param parentGroup
     *            the parent group to add the object to.
     *
     * @return the TreeItem object
     */
    public abstract TreeItem addObject(HObject newObject, Group parentGroup);

    /**
     * @return the Tree which holds the file structure.
     */
    public abstract Tree getTree();

    /**
     * @return the list of currently open files.
     */
    public abstract List<FileFormat> getCurrentFiles();

    /**
     * @param obj the object to find
     *
     * @return the tree item that contains the given data object.
     */
    public abstract TreeItem findTreeItem(HObject obj);

}
