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

import java.lang.reflect.Constructor;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.FileFormat;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public TableView getTableView(ViewManager viewer, HashMap dataPropertiesMap) {
        String dataViewName = null;
        Object[] initargs = { viewer, dataPropertiesMap };
        TableView theView = null;
        HObject dataObject = null;

        log.trace("getTableView(): start");

        /*
         * If the name of a specific TableView class to use has been passed in via the
         * data options map, retrieve its name now, otherwise grab the
         * "currently selected" TableView class from the ViewProperties-managed list.
         */
        dataViewName = (String) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.VIEW_NAME);
        if (dataViewName == null) {
            List<?> tableViewList = ViewProperties.getTableViewList();
            if ((tableViewList == null) || (tableViewList.size() <= 0)) {
                return null;
            }

            dataViewName = (String) tableViewList.get(0);
        }

        /* Retrieve the data object to be displayed */
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
            log.trace("getTableView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("getTableView(): Class.forName({}) failure: {}", dataViewName, ex);

            try {
                log.trace("getTableView(): ViewProperties.loadExtClass().loadClass({})",
                        dataViewName);

                /* Attempt to load the class as an external module */
                theClass = ViewProperties.loadExtClass().loadClass(dataViewName);
            }
            catch (Exception ex2) {
                log.debug("getTableView(): ViewProperties.loadExtClass().loadClass({}) failure: {}",
                        dataViewName, ex);

                /* No loadable class found; use the default TableView */
                if (dataObject instanceof ScalarDS)
                    dataViewName = "hdf.view.DefaultScalarDSTableView";
                else if (dataObject instanceof CompoundDS)
                    dataViewName = "hdf.view.DefaultCompoundDSTableView";
                else if (dataObject instanceof Attribute)
                    dataViewName = "hdf.view.DefaultAttributeTableView";
                else
                    dataViewName = null;

                try {
                    log.trace("getTableView(): Class.forName({})", dataViewName);

                    theClass = Class.forName(dataViewName);
                }
                catch (Exception ex3) {
                    log.debug("getTableView(): Class.forName({}) failure: {}", dataViewName, ex);

                    theClass = null;
                }
            }
        }

        /* Check to see if there is a bitmask to be applied to the data */
        BitSet bitmask = (BitSet) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.BITMASK);
        if (bitmask != null) {
            /*
             * Create a copy of the data object in order to apply the bitmask
             * non-destructively
             */
            HObject d_copy = null;
            Constructor<? extends HObject> constructor = null;
            Object[] paramObj = null;

            try {
                Class<?>[] paramClass = { FileFormat.class, String.class, String.class, long[].class };
                constructor = dataObject.getClass().getConstructor(paramClass);

                paramObj = new Object[] { dataObject.getFileFormat(), dataObject.getName(), dataObject.getPath(),
                        dataObject.getOID() };
            }
            catch (Exception ex) {
                constructor = null;
            }

            try {
                d_copy = constructor.newInstance(paramObj);
            }
            catch (Exception ex) {
                d_copy = null;
            }

            if (d_copy != null) {
                try {
                    if (d_copy instanceof Dataset) {
                        ((Dataset) d_copy).init();
                        log.trace("getTableView(): d_copy inited");
                    }

                    int rank = ((DataFormat) dataObject).getRank();
                    System.arraycopy(((DataFormat) dataObject).getDims(), 0, ((DataFormat) d_copy).getDims(), 0, rank);
                    System.arraycopy(((DataFormat) dataObject).getStartDims(), 0, ((DataFormat) d_copy).getStartDims(),0, rank);
                    System.arraycopy(((DataFormat) dataObject).getSelectedDims(), 0, ((DataFormat) d_copy).getSelectedDims(), 0, rank);
                    System.arraycopy(((DataFormat) dataObject).getStride(), 0, ((DataFormat) d_copy).getStride(), 0, rank);
                    System.arraycopy(((DataFormat) dataObject).getSelectedIndex(), 0, ((DataFormat) d_copy).getSelectedIndex(), 0, 3);
                }
                catch (Throwable ex) {
                    ex.printStackTrace();
                }

                dataPropertiesMap.put(ViewProperties.DATA_VIEW_KEY.OBJECT, d_copy);
            }
        }

        try {
            theView = (TableView) Tools.newInstance(theClass, initargs);

            log.trace("getTableView(): returning TableView instance {}", theView);
        }
        catch (Exception ex) {
            log.trace("getTableView(): Error instantiating class: {}", ex);
        }

        log.trace("getTableView(): finish");

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
    MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, HObject theObj) {
        return null;
    }

}
