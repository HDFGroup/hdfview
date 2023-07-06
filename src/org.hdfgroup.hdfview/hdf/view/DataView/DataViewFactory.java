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

package hdf.view.DataView;

import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import hdf.object.HObject;
import hdf.view.ImageView.ImageView;
import hdf.view.MetaDataView.MetaDataView;
import hdf.view.PaletteView.PaletteView;
import hdf.view.TableView.TableView;
import hdf.view.TreeView.TreeView;

/**
 * The data view factory interface for displaying data objects
 */
public abstract class DataViewFactory {
    /**
     * Get an instance of TableView given the appropriate constructor parameters
     *
     * @param viewer
     *             The data view manager
     * @param dataPropertiesMap
     *             The properties for the table view
     *
     * @throws ClassNotFoundException
     *             If there is an error getting the class for a table view.
     *
     * @return the table view.
     */
    @SuppressWarnings("rawtypes")
    public abstract TableView    getTableView(DataViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException;

    /**
     * Get an instance of ImageView given the appropriate constructor parameters
     *
     * @param viewer
     *             The data view manager
     * @param dataPropertiesMap
     *             The properties for the image view
     *
     * @throws ClassNotFoundException
     *             If there is an error getting the class for a image view.
     *
     * @return the image view.
     */
    @SuppressWarnings("rawtypes")
    public abstract ImageView    getImageView(DataViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException;

    /**
     * Get an instance of PaletteView given the appropriate constructor parameters
     *
     * @param parent
     *             The parent shell for the palette view
     * @param viewer
     *             The data view manager
     * @param theImageView
     *             The image view for the palette view
     *
     * @throws ClassNotFoundException
     *             If there is an error getting the class for a palette view.
     *
     * @return the palette view.
     */
    public abstract PaletteView  getPaletteView(Shell parent, DataViewManager viewer, ImageView theImageView) throws ClassNotFoundException;

    /**
     * Get an instance of MetaDataView given the appropriate constructor parameters
     *
     * @param parent
     *             The parent composite for the maetadata view
     * @param viewer
     *             The data view manager
     * @param theObj
     *             The object for the metadata view
     *
     * @throws ClassNotFoundException
     *             If there is an error getting the class for a metadata view.
     *
     * @return the metadata view.
     */
    public abstract MetaDataView getMetaDataView(Composite parent, DataViewManager viewer, HObject theObj) throws ClassNotFoundException;

    /**
     * Get an instance of TreeView given the appropriate constructor parameters
     *
     * @param parent
     *             The parent composite for the tree view
     * @param viewer
     *             The data view manager
     *
     * @throws ClassNotFoundException
     *             If there is an error getting the class for a tree view.
     *
     * @return the tree view.
     */
    public abstract TreeView getTreeView(Composite parent, DataViewManager viewer) throws ClassNotFoundException;
}
