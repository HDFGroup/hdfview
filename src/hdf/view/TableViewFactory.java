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

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;

public class TableViewFactory extends DataViewFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableViewFactory.class);

    @SuppressWarnings("rawtypes")
    @Override
    public TableView getTableView(ViewManager viewer, HashMap dataPropertiesMap) {
        TableView theView = null;
        HObject dataObject = null;

        log.trace("TableViewFactory: getTableView(): start");

        if (dataPropertiesMap != null)
            dataObject = (HObject) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.OBJECT);

        if (dataObject == null) dataObject = viewer.getTreeView().getCurrentObject();

        if (dataObject == null) return null;

        /*
         * TODO: Currently no support for other modules; return DefaultBaseTableView
         * subclasses
         */
        if (dataObject instanceof ScalarDS) {

        }
        else if (dataObject instanceof CompoundDS) {

        }
        else if (dataObject instanceof Attribute) {

        }

        log.trace("TableViewFactory: getTableView(): finish");

        return theView;
    }

    @SuppressWarnings("rawtypes")
    @Override
    ImageView getImageView(ViewManager viewer, HashMap dataPropertiesMap) {
        return null;
    }

    @Override
    MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, DataFormat theObj) {
        return null;
    }

}
