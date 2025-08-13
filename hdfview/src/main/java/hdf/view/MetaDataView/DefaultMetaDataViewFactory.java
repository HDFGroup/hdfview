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

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5Link;
import hdf.view.DataView.DataViewManager;
import hdf.view.Tools;
import hdf.view.ViewProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.widgets.Composite;

/**
 * A simple Factory class which returns concrete instances of the default
 * MetaDataView, based on whether the data object is a Group, Dataset, Datatype
 * or other form of object.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
public class DefaultMetaDataViewFactory extends MetaDataViewFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultMetaDataViewFactory.class);

    @Override
    public MetaDataView getMetaDataView(Composite parentObj, DataViewManager viewer, HObject theObj)
        throws ClassNotFoundException
    {
        String dataViewName  = null;
        Object[] initargs    = {parentObj, viewer, theObj};
        MetaDataView theView = null;

        if (theObj instanceof Group)
            dataViewName = ViewProperties.DEFAULT_GROUP_METADATAVIEW_NAME;
        else if (theObj instanceof Dataset)
            dataViewName = ViewProperties.DEFAULT_DATASET_METADATAVIEW_NAME;
        else if (theObj instanceof Datatype)
            dataViewName = ViewProperties.DEFAULT_DATATYPE_METADATAVIEW_NAME;
        else if (theObj instanceof H5Link)
            dataViewName = ViewProperties.DEFAULT_LINK_METADATAVIEW_NAME;
        else
            dataViewName = null;

        Class<?> theClass = null;
        try {
            log.trace("getMetaDataView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("getMetaDataView(): unable to load default MetaDataView class by name({})",
                      dataViewName);
            theClass = null;
        }

        if (theClass == null)
            throw new ClassNotFoundException();

        try {
            theView = (MetaDataView)Tools.newInstance(theClass, initargs);

            log.trace("getMetaDataView(): returning MetaDataView instance {}", theView);
        }
        catch (Exception ex) {
            log.debug("getMetaDataView(): Error instantiating class:", ex);
            theView = null;
        }

        return theView;
    }
}
