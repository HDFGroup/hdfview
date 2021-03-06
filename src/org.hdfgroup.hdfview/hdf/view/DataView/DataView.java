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

import hdf.object.HObject;

/**
 * The data view interface for displaying data objects
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public abstract interface DataView {
    /** The unknown view type */
    public static final int DATAVIEW_UNKNOWN = -1;

    /** The table view type */
    public static final int DATAVIEW_TABLE = 1;

    /** The image view type */
    public static final int DATAVIEW_IMAGE = 2;

    /** @return the data object displayed in this data viewer */
    public abstract HObject getDataObject();
}
