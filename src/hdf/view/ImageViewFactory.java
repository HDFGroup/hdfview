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

public class ImageViewFactory extends DataViewFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageViewFactory.class);

    @SuppressWarnings("rawtypes")
    @Override
    TableView getTableView(ViewManager viewer, HashMap dataPropertiesMap) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    ImageView getImageView(ViewManager viewer, HashMap dataPropertiesMap) {
        ImageView theView = null;

        log.trace("ImageViewFactory: getImageView(): start");

        /* TODO: Currently no support for other modules; return DefaultImageView */
        try {
            theView = (ImageView) Tools.newInstance(DefaultImageView.class, new Object[] { viewer, dataPropertiesMap });
            log.trace("ImageViewFactory: getImageView(): returning DefaultImageView instance");
        }
        catch (Exception ex) {
            log.trace("ImageViewFactory: getImageView(): Error instantiating class: {}", ex);
        }

        log.trace("ImageViewFactory: getImageView(): finish");

        return theView;
    }

    @Override
    MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, DataFormat theObj) {
        return null;
    }

}
