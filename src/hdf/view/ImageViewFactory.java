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

import hdf.object.DataFormat;

/**
 * A Factory class to return instances of classes implementing the ImageView
 * interface, depending on the "currently selected" ImageView class in the list
 * maintained by the ViewProperties class.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
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
        String dataViewName = ViewProperties.getImageViewList().get(0);
        Object[] initargs = { viewer, dataPropertiesMap };
        ImageView theView = null;

        log.trace("ImageViewFactory: getImageView(): start");

        /* TODO: Currently no support for other modules; return DefaultImageView */

        /* Attempt to load the class by name */
        Class<?> theClass = null;
        try {
            log.trace("ImageViewFactory: getImageView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("ImageViewFactory: getImageView(): Class.forName({}) failure: {}", dataViewName, ex);

            try {
                log.trace("ImageViewFactory: getImageView(): ViewProperties.loadExtClass().loadClass({})",
                        dataViewName);

                /* Attempt to load the class as an external module */
                theClass = ViewProperties.loadExtClass().loadClass(dataViewName);
            }
            catch (Exception ex2) {
                log.debug("ImageViewFactory: getImageView(): ViewProperties.loadExtClass().loadClass({}) failure: {}",
                        dataViewName, ex);

                /* No loadable class found; use the default ImageView */
                dataViewName = "hdf.view.DefaultImageView";

                try {
                    log.trace("ImageViewFactory: getImageView(): Class.forName({})", dataViewName);

                    theClass = Class.forName(dataViewName);
                }
                catch (Exception ex3) {
                    log.debug("ImageViewFactory: getImageView(): Class.forName({}) failure: {}", dataViewName, ex);

                    theClass = null;
                }
            }
        }

        try {
            theView = (ImageView) Tools.newInstance(theClass, initargs);

            log.trace("ImageViewFactory: getImageView(): returning ImageView instance {}", theView);
        }
        catch (Exception ex) {
            log.trace("ImageViewFactory: getImageView(): Error instantiating class: {}", ex);
        }

        log.trace("ImageViewFactory: getImageView(): finish");

        return theView;
    }

    @Override
    PaletteView getPaletteView(Shell parent, ViewManager viewer, ImageView theImageView) {
        return null;
    }

    @Override
    MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, DataFormat theObj) {
        return null;
    }

}
