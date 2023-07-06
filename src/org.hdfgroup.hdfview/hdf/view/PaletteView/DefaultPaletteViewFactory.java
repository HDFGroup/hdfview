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

package hdf.view.PaletteView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.widgets.Shell;

import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;
import hdf.view.ImageView.ImageView;

/**
 * A simple Factory class which returns concrete instances of the default
 * PaletteView.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
public class DefaultPaletteViewFactory extends PaletteViewFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultPaletteViewFactory.class);

    @Override
    public PaletteView getPaletteView(Shell parent, DataViewManager viewer, ImageView theImageView) throws ClassNotFoundException {
        String dataViewName = null;
        Object[] initargs;
        PaletteView theView = null;

        dataViewName = ViewProperties.DEFAULT_PALETTEVIEW_NAME;

        Class<?> theClass = null;
        try {
            log.trace("getPaletteView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("getPaletteView(): unable to load default PaletteView class by name({})", dataViewName);
            theClass = null;
        }

        if (theClass == null) throw new ClassNotFoundException();

        try {
            initargs = new Object[] { parent, viewer, theImageView };

            theView = (PaletteView) Tools.newInstance(theClass, initargs);

            log.trace("getPaletteView(): returning PaletteView instance {}", theView);
        }
        catch (Exception ex) {
            log.debug("getPaletteView(): Error instantiating class:", ex);
            theView = null;
        }

        return theView;
    }

}
