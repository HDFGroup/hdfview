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

package hdf.view;

import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import hdf.object.HObject;
import hdf.view.ImageView.ImageView;
import hdf.view.MetaDataView.MetaDataView;
import hdf.view.PaletteView.PaletteView;
import hdf.view.TableView.TableView;

public abstract class DataViewFactory {
    /* Get an instance of TableView given the appropriate constructor parameters */
    @SuppressWarnings("rawtypes")
    public abstract TableView    getTableView(ViewManager viewer, HashMap dataPropertiesMap);

    /* Get an instance of ImageView given the appropriate constructor parameters */
    @SuppressWarnings("rawtypes")
    public abstract ImageView    getImageView(ViewManager viewer, HashMap dataPropertiesMap);

    /*
     * Get an instance of PaletteView given the appropriate constructor parameters
     */
    public abstract PaletteView  getPaletteView(Shell parent, ViewManager viewer, ImageView theImageView);

    /*
     * Get an instance of MetaDataView given the appropriate constructor parameters
     */
    public abstract MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, HObject theObj);
}
