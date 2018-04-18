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

import hdf.view.ViewProperties.DataViewType;

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
        log.trace("DataViewFactoryProducer: getFactory(): start");

        if (viewType == DataViewType.TABLE) {
            log.trace("DataViewFactoryProducer: getFactory(): returning TableView factory instance");
            log.trace("DataViewFactoryProducer: getFactory(): finish");

            return new TableViewFactory();
        }
        else if (viewType == DataViewType.IMAGE) {
            log.trace("DataViewFactoryProducer: getFactory(): returning ImageView factory instance");
            log.trace("DataViewFactoryProducer: getFactory(): finish");

            return new ImageViewFactory();
        }
        else if (viewType == DataViewType.PALETTE) {
            log.trace("DataViewFactoryProducer: getFactory(): returning PaletteView factory instance");
            log.trace("DataViewFactoryProducer: getFactory(): finish");

            return new PaletteViewFactory();
        }
        else if (viewType == DataViewType.METADATA) {
            log.trace("DataViewFactoryProducer: getFactory(): returning MetaDataView factory instance");
            log.trace("DataViewFactoryProducer: getFactory(): finish");

            return new MetaDataViewFactory();
        }

        log.trace("DataViewFactoryProducer: getFactory(): no suitable factory class found");
        log.trace("DataViewFactoryProducer: getFactory(): finish");

        return null;
    }
}
