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

package hdf.view.DataView;

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

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataViewFactoryProducer.class);

    public static DataViewFactory getFactory(DataViewType viewType) {
        log.trace("getFactory(): start");

        switch (viewType) {
            case TABLE:
                log.trace("getFactory(): returning TableView factory instance");
                log.trace("getFactory(): finish");
                return new DefaultTableViewFactory();
            case IMAGE:
                log.trace("getFactory(): returning ImageView factory instance");
                log.trace("getFactory(): finish");
                return new DefaultImageViewFactory();
            case PALETTE:
                log.trace("getFactory(): returning PaletteView factory instance");
                log.trace("getFactory(): finish");
                return new DefaultPaletteViewFactory();
            case METADATA:
                log.trace("getFactory(): returning MetaDataView factory instance");
                log.trace("getFactory(): finish");
                return new DefaultMetaDataViewFactory();
            case TREEVIEW:
                log.trace("getFactory(): returning TreeView factory instance");
                log.trace("getFactory(): finish");
                return new DefaultTreeViewFactory();
            default:
                log.trace("getFactory(): no suitable factory class found");
                log.trace("getFactory(): finish");
                return null;
        }
    }
}
