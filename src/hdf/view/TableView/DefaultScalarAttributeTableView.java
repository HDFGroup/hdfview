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

package hdf.view.TableView;

import java.util.HashMap;

import hdf.view.ViewManager;

public class DefaultScalarAttributeTableView extends DefaultScalarDSTableView implements TableView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultScalarAttributeTableView.class);

    public DefaultScalarAttributeTableView(ViewManager theView) {
        this(theView, null);
    }

    @SuppressWarnings("rawtypes")
    public DefaultScalarAttributeTableView(ViewManager theView, HashMap dataPropertiesMap) {
        super(theView, dataPropertiesMap);
    }
}
