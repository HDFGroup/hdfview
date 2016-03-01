/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view;

import hdf.object.HObject;

/**
 * 
 * Defines a list of APIs for the main HDFView windows
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public abstract interface ViewManager {
    /** Data content is displayed, add the dataview to the main windows */
    public abstract void addDataView(DataView dataView);

    /** Data content is closed, remove the dataview from the main window */
    public abstract void removeDataView(DataView dataView);

    /**
     * Returns DataView that contains the specified data object. It is useful to
     * avoid redundant display of data object that is opened already.
     * 
     * @param dataObject
     *            the object whose presence in the main view is to be tested.
     * @return DataView contains the specified data object, null if the data
     *         object is not displayed.
     */
    public abstract DataView getDataView(HObject dataObject);

    /** Display feedback message */
    public abstract void showStatus(String msg);

    /** Returns the current TreeView */
    public abstract TreeView getTreeView();
}
