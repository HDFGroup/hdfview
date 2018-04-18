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

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * A Factory class to return instances of classes implementing the TableView
 * interface, depending on the "current selected" TableView class in the list
 * maintained by the ViewProperties class.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
public class TableViewFactory extends DataViewFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableViewFactory.class);

    @SuppressWarnings("rawtypes")
    @Override
    public TableView getTableView(ViewManager viewer, HashMap dataPropertiesMap) {
        String dataViewName = ViewProperties.getTableViewList().get(0);
        Object[] initargs = { viewer, dataPropertiesMap };
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

        /* Attempt to load the class by name */
        Class<?> theClass = null;
        try {
            log.trace("TableViewFactory: getTableView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("TableViewFactory: getTableView(): Class.forName({}) failure: {}", dataViewName, ex);

            try {
                log.trace("TableViewFactory: getTableView(): ViewProperties.loadExtClass().loadClass({})",
                        dataViewName);

                /* Attempt to load the class as an external module */
                theClass = ViewProperties.loadExtClass().loadClass(dataViewName);
            }
            catch (Exception ex2) {
                log.debug("TableViewFactory: getTableView(): ViewProperties.loadExtClass().loadClass({}) failure: {}",
                        dataViewName, ex);

                /* No loadable class found; use the default TableView */
                if (dataObject instanceof ScalarDS)
                    dataViewName = "hdf.view.DefaultScalarDSTableView";
                else if (dataObject instanceof CompoundDS)
                    dataViewName = "hdf.view.DefaultCompoundDSTableView";
                else if (dataObject instanceof Attribute)
                    dataViewName = "hdf.view.DefaultAttributeTableView";

                try {
                    log.trace("TableViewFactory: getTableView(): Class.forName({})", dataViewName);

                    theClass = Class.forName(dataViewName);
                }
                catch (Exception ex3) {
                    log.debug("TableViewFactory: getTableView(): Class.forName({}) failure: {}", dataViewName, ex);

                    theClass = null;
                }
            }
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
    PaletteView getPaletteView(Shell parent, ViewManager viewer, ImageView theImageView) {
        return null;
    }

    @Override
    MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, DataFormat theObj) {
        return null;
    }

}
