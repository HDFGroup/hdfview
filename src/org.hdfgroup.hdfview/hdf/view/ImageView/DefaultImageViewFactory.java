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

package hdf.view.ImageView;

import java.util.BitSet;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;

/**
 * A simple Factory class which returns concrete instances of the default
 * ImageView.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
public class DefaultImageViewFactory extends ImageViewFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultImageViewFactory.class);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public ImageView getImageView(DataViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException {
        String dataViewName = null;
        Object[] initargs = { viewer, dataPropertiesMap };
        ImageView theView = null;

        /*
         * If the name of a specific ImageView class to use has been passed in via the
         * data options map, retrieve its name now, otherwise use the default ImageView
         * class.
         */
        dataViewName = (String) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.VIEW_NAME);
        if (dataViewName == null || dataViewName.equals(ViewProperties.DEFAULT_MODULE_TEXT)) {
            dataViewName = ViewProperties.DEFAULT_IMAGEVIEW_NAME;
        }

        Class<?> theClass = null;
        try {
            log.trace("getImageView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("getImageView(): unable to load default ImageView class by name({})", dataViewName);
            theClass = null;
        }

        if (theClass == null) throw new ClassNotFoundException();

        /* Add some data display properties if using the default ImageView */
        if (dataViewName.startsWith(ViewProperties.DEFAULT_IMAGEVIEW_NAME)) {
            BitSet bitmask = (BitSet) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.BITMASK);
            dataPropertiesMap.put(ViewProperties.DATA_VIEW_KEY.CONVERTBYTE, Boolean.valueOf((bitmask != null)));
        }

        try {
            theView = (ImageView) Tools.newInstance(theClass, initargs);

            log.trace("getImageView(): returning ImageView instance {}", theView);
        }
        catch (Exception ex) {
            log.debug("getImageView(): Error instantiating class:", ex);
            theView = null;
        }

        return theView;
    }

}
