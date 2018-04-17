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

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.HObject;
import hdf.object.ScalarDS;

public class TableViewFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableViewFactory.class);

    public TableView getTableView(HObject dataObject) {
        if (dataObject == null) return null;

        log.trace("TableViewFactory: getTableView(): start");

        if (dataObject instanceof ScalarDS) {

        }
        else if (dataObject instanceof CompoundDS) {

        }
        else if (dataObject instanceof Attribute) {

        }

        log.trace("TableViewFactory: getTableView(): finish");

        return null;
    }
}
