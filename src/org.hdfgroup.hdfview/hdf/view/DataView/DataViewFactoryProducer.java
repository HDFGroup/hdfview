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

package hdf.view.DataView;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.ViewProperties.DataViewType;
import hdf.view.ImageView.DefaultImageViewFactory;
import hdf.view.MetaDataView.DefaultMetaDataViewFactory;
import hdf.view.PaletteView.DefaultPaletteViewFactory;
import hdf.view.TableView.DefaultTableViewFactory;
import hdf.view.TreeView.DefaultTreeViewFactory;

/**
 * Following the Abstract Factory Pattern, represents a class to produce
 * different types of DataView factory classes depending on the given
 * DataViewType enum value.
 *
 * @author jhenderson
 * @version 1.0 4/17/2018
 */
public class DataViewFactoryProducer {

    private static final Logger log = LoggerFactory.getLogger(DataViewFactoryProducer.class);

    /**
     * get the requested DataViewFactory
     *
     *
     * @param viewType
     *             The data view type requested
     *
     * @throws Exception
     *             If there is an error getting the class for a data view factory.
     *
     * @return the data view factory.
     */
    public static DataViewFactory getFactory(DataViewType viewType) throws Exception {
        String factoryClassName = null;
        DataViewFactory theFactory = null;
        List<?> moduleList = null;

        /*
         * First determine if we are using the default module for the requested DataViewFactory
         * class. If not, we will attempt to load the given DataViewFactory class.
         */
        switch (viewType) {
            case TABLE:
                /* Retrieve the "currently selected" TableViewFactory class to use */
                moduleList = ViewProperties.getTableViewList();
                if ((moduleList == null) || (moduleList.size() <= 0)) {
                    return null;
                }

                factoryClassName = (String) moduleList.get(0);

                if (factoryClassName.equals(ViewProperties.DEFAULT_MODULE_TEXT)) {
                    log.trace("getFactory(): returning default TableView factory instance");
                    return new DefaultTableViewFactory();
                }

                break;

            case IMAGE:
                /* Retrieve the "currently selected" ImageViewFactory class to use */
                moduleList = ViewProperties.getImageViewList();
                if ((moduleList == null) || (moduleList.size() <= 0)) {
                    return null;
                }

                factoryClassName = (String) moduleList.get(0);

                if (factoryClassName.equals(ViewProperties.DEFAULT_MODULE_TEXT)) {
                    log.trace("getFactory(): returning default ImageView factory instance");
                    return new DefaultImageViewFactory();
                }

                break;

            case PALETTE:
                /* Retrieve the "currently selected" PaletteViewFactory class to use */
                moduleList = ViewProperties.getPaletteViewList();
                if ((moduleList == null) || (moduleList.size() <= 0)) {
                    return null;
                }

                factoryClassName = (String) moduleList.get(0);

                if (factoryClassName.equals(ViewProperties.DEFAULT_MODULE_TEXT)) {
                    log.trace("getFactory(): returning default PaletteView factory instance");
                    return new DefaultPaletteViewFactory();
                }

                break;

            case METADATA:
                /* Retrieve the "currently selected" MetaDataViewFactory class to use */
                moduleList = ViewProperties.getMetaDataViewList();
                if ((moduleList == null) || (moduleList.size() <= 0)) {
                    return null;
                }

                factoryClassName = (String) moduleList.get(0);

                if (factoryClassName.equals(ViewProperties.DEFAULT_MODULE_TEXT)) {
                    log.trace("getFactory(): returning default MetaDataView factory instance");
                    return new DefaultMetaDataViewFactory();
                }

                break;

            case TREEVIEW:
                /* Retrieve the "currently selected" TreeViewFactory class to use */
                moduleList = ViewProperties.getTreeViewList();
                if ((moduleList == null) || (moduleList.size() <= 0)) {
                    return null;
                }

                factoryClassName = (String) moduleList.get(0);

                if (factoryClassName.equals(ViewProperties.DEFAULT_MODULE_TEXT)) {
                    log.trace("getFactory(): returning default TreeView factory instance");
                    return new DefaultTreeViewFactory();
                }

                break;

            default:
                throw new Exception("getFactory(): invalid DataViewType");
        }

        Class<?> theClass = null;
        try {
            log.trace("getFactory(): ViewProperties.loadExtClass().loadClass({})", factoryClassName);

            /* Attempt to load the class as an external module */
            theClass = ViewProperties.loadExtClass().loadClass(factoryClassName);
        }
        catch (Exception ex) {
            log.debug("getFactory(): ViewProperties.loadExtClass().loadClass({}) failure:", factoryClassName, ex);

            try {
                log.trace("getFactory(): Class.forName({})", factoryClassName);

                /* Attempt to load the class directly by the given name */
                theClass = Class.forName(factoryClassName);
            }
            catch (Exception ex2) {
                log.debug("getFactory(): Class.forName({}) failure:", factoryClassName, ex);

                /* At this point, we have no choice but to fall back to the default modules */
                switch (viewType) {
                    case TABLE:
                        log.trace("getFactory(): returning default TableView factory instance");
                        return new DefaultTableViewFactory();
                    case IMAGE:
                        log.trace("getFactory(): returning default ImageView factory instance");
                        return new DefaultImageViewFactory();
                    case PALETTE:
                        log.trace("getFactory(): returning default PaletteView factory instance");
                        return new DefaultPaletteViewFactory();
                    case METADATA:
                        log.trace("getFactory(): returning default MetaDataView factory instance");
                        return new DefaultMetaDataViewFactory();
                    case TREEVIEW:
                        log.trace("getFactory(): returning default TreeView factory instance");
                        return new DefaultTreeViewFactory();
                    default:
                        throw new Exception("getFactory(): invalid DataViewType");
                }
            }
        }

        if (theClass == null) throw new ClassNotFoundException();

        try {
            theFactory = (DataViewFactory) Tools.newInstance(theClass, null);

            log.trace("getFactory(): returning DataViewFactory instance {}", theFactory);
        }
        catch (Exception ex) {
            log.debug("getFactory(): Error instantiating class:", ex);
            theFactory = null;
        }

        return theFactory;
    }
}
