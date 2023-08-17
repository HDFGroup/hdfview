/*****************************************************************************
 * Copyright by The HDF Group.                                               *
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

package hdf.view.TableView;

import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import hdf.object.HObject;
import hdf.view.DataView.DataViewFactory;
import hdf.view.DataView.DataViewManager;
import hdf.view.ImageView.ImageView;
import hdf.view.MetaDataView.MetaDataView;
import hdf.view.PaletteView.PaletteView;
import hdf.view.TreeView.TreeView;

/**
 * This class extends DataViewFactory so that at runtime it can be determined if a specific DataViewFactory class is a
 * TableViewFactory and can thus be used appropriately where a TableView is needed.
 *
 * @author jhenderson
 * @version 1.0 7/30/2018
 */
public abstract class TableViewFactory extends DataViewFactory {

    @SuppressWarnings("rawtypes")
    @Override
    public final ImageView getImageView(DataViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException("TableViewFactory does not implement getImageView()");
    }

    @Override
    public final PaletteView getPaletteView(Shell parent, DataViewManager viewer, ImageView theImageView) throws ClassNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException("TableViewFactory does not implement getPaletteView()");
    }

    @Override
    public final MetaDataView getMetaDataView(Composite parentObj, DataViewManager viewer, HObject theObj) throws ClassNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException("TableViewFactory does not implement getMetaDataView()");
    }

    @Override
    public final TreeView getTreeView(Composite parent, DataViewManager viewer) throws ClassNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException("TableViewFactory does not implement getTreeView()");
    }

}
