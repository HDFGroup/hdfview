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

import java.util.List;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * 
 * <p>
 * TreeView defines APIs for opening a file and displaying the file structure in 
 * a tree structure.
 * </p>
 * 
 * <p>
 * TreeView uses folders and leaf nodes to represent groups and data objects in
 * the file. You can expand or collapse folders to navigate data objects in the
 * file.
 * </p>
 * 
 * <p>
 * From the TreeView, you can open the data content or metadata of the selected 
 * object. You can select object(s) to delete or add new objects to the file.
 * </p>
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public abstract interface TreeView {
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
    public abstract FileFormat openFile(String filename, int accessID) throws Exception;
    public abstract FileFormat reopenFile(FileFormat theFile) throws Exception;

    /**
     * close a file
     * 
     * @param file
     *            the file to close
     */
    public abstract void closeFile(FileFormat file) throws Exception;

    /**
     * save a file
     * 
     * @param file
     *            the file to save
     */
    public abstract void saveFile(FileFormat file) throws Exception;

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
     * @return the dataview that displays the data content
     * @throws Exception
     */
    public abstract DataView showDataContent(HObject dataObject)
            throws Exception;

    /**
     * Displays the meta data of a data object.
     * 
     * @param dataObject
     *            the data object
     * @return the MetaDataView that displays the MetaData of the data object
     * @throws Exception
     */
    public abstract MetaDataView showMetaData(HObject dataObject)
            throws Exception;

    /**
     * Adds a new data object to the file.
     * 
     * @param newObject
     *            the new object to add.
     * @param parentGroup
     *            the parent group the object is to add to.
     * @throws Exception
     */
    public abstract void addObject(HObject newObject, Group parentGroup)
            throws Exception;

    /**
     * Returns the Tree which holds the file structure.
     * 
     * @return the Tree which holds the file structure.
     */
    public abstract Tree getTree();

    /**
     * Returns the list of current open files..
     */
    public abstract List<FileFormat> getCurrentFiles();

    /**
     * Returns the tree item that contains the given data object.
     */
    public abstract TreeItem findTreeItem(HObject obj);

}
