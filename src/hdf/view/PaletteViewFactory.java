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
 * A Factory class to return instances of classes implementing the PaletteView
 * interface, depending on the "current selected" PaletteView class in the list
 * maintained by the ViewProperties class.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
public class PaletteViewFactory extends DataViewFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaletteViewFactory.class);

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
    PaletteView getPaletteView(Shell parent, ViewManager viewer, ImageView theImageView) {
        String dataViewName = ViewProperties.getPaletteViewList().get(0);
        Object[] initargs = { parent, viewer, theImageView };
        PaletteView theView = null;

        log.trace("PaletteViewFactory: getPaletteView(): start");

        /* TODO: Currently no support for other modules; return DefaultImageView */

        /* Attempt to load the class by name */
        Class<?> theClass = null;
        try {
            log.trace("PaletteViewFactory: getPaletteView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("PaletteViewFactory: getPaletteView(): Class.forName({}) failure: {}", dataViewName, ex);

            try {
                log.trace("PaletteViewFactory: getPaletteView(): ViewProperties.loadExtClass().loadClass({})",
                        dataViewName);

                /* Attempt to load the class as an external module */
                theClass = ViewProperties.loadExtClass().loadClass(dataViewName);
            }
            catch (Exception ex2) {
                log.debug(
                        "PaletteViewFactory: getPaletteView(): ViewProperties.loadExtClass().loadClass({}) failure: {}",
                        dataViewName, ex);

                /* No loadable class found; use the default PaletteView */
                dataViewName = "hdf.view.DefaultPaletteView";

                try {
                    log.trace("PaletteViewFactory: getPaletteView(): Class.forName({})", dataViewName);

                    theClass = Class.forName(dataViewName);
                }
                catch (Exception ex3) {
                    log.debug("PaletteViewFactory: getPaletteView(): Class.forName({}) failure: {}", dataViewName, ex);

                    theClass = null;
                }
            }
        }

        try {
            theView = (PaletteView) Tools.newInstance(theClass, initargs);

            log.trace("PaletteViewFactory: getPaletteView(): returning PaletteView instance {}", theView);
        }
        catch (Exception ex) {
            log.trace("PaletteViewFactory: getPaletteView(): Error instantiating class: {}", ex);
        }

        log.trace("PaletteViewFactory: getPaletteView(): finish");

        return theView;
    }

    @Override
    MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, DataFormat theObj) {
        return null;
    }

}
