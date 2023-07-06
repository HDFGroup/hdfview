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

package hdf.view.MetaDataView;

import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import hdf.view.DataView.DataViewFactory;
import hdf.view.DataView.DataViewManager;
import hdf.view.ImageView.ImageView;
import hdf.view.PaletteView.PaletteView;
import hdf.view.TableView.TableView;
import hdf.view.TreeView.TreeView;

/**
 * This class extends DataViewFactory so that at runtime it can be determined
 * if a specific DataViewFactory class is a MetaDataViewFactory and can thus
 * be used appropriately where a MetaDataView is needed.
 *
 * @author jhenderson
 * @version 1.0 7/30/3018
 */
public abstract class MetaDataViewFactory extends DataViewFactory {

    @SuppressWarnings("rawtypes")
    @Override
    public final TableView getTableView(DataViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException("MetaDataViewFactory does not implement getTableView()");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public final ImageView getImageView(DataViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException("MetaDataViewFactory does not implement getImageView()");
    }

    @Override
    public final PaletteView getPaletteView(Shell parent, DataViewManager viewer, ImageView theImageView) throws ClassNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException("MetaDataViewFactory does not implement getPaletteView()");
    }

    @Override
    public final TreeView getTreeView(Composite parent, DataViewManager viewer) throws ClassNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException("MetaDataViewFactory does not implement getTreeView()");
    }

}
