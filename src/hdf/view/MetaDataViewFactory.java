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

import hdf.object.DataFormat;

public class MetaDataViewFactory extends DataViewFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetaDataViewFactory.class);

    @SuppressWarnings("rawtypes")
    @Override
    TableView getTableView(ViewManager viewer, HashMap dataPropertiesMap) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    ImageView getImageView(ViewManager viewer, HashMap dataPropertiesMap) {
        return null;
    }

    @Override
    MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, DataFormat theObj) {
        MetaDataView theView = null;

        log.trace("MetaDataViewFactory: getMetaDataView(): start");

        /* TODO: Currently no support for other modules; return DefaultMetaDataView */
        try {
            theView = (MetaDataView) Tools.newInstance(DefaultMetaDataView.class,
                    new Object[] { parentObj, viewer, theObj });
            log.trace("MetaDataViewFactory: getMetaDataView(): returning DefaultMetaDataView instance");
        }
        catch (Exception ex) {
            log.trace("MetaDataViewFactory: getMetaDataView(): Error instantiating class: {}", ex);
        }

        log.trace("MetaDataViewFactory: getMetaDataView(): finish");

        return theView;
    }

}
