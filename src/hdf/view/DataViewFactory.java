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

import hdf.object.HObject;

public abstract class DataViewFactory {
    /* Get an instance of TableView for this HObject */
    abstract TableView    getTableView(HObject dataObject);

    /* Get an instance of ImageView for this HObject */
    abstract ImageView    getImageView(HObject dataObject);

    /* Get an instance of MetaDataView for this HObject */
    abstract MetaDataView getMetaDataView(HObject dataObject);
}
