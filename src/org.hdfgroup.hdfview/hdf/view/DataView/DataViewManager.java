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

import hdf.object.HObject;
import hdf.view.TreeView.TreeView;

/**
 *
 * Defines a list of APIs for the main HDFView windows
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public abstract interface DataViewManager {
    /**
     * Data content is displayed, add the dataview to the main windows
     * @param dataView
     *            the dataView whose presence in the main view is to be added.
     */
    public abstract void addDataView(DataView dataView);

    /**
     * Data content is closed, remove the dataview from the main window
     * @param dataView
     *            the dataView whose presence in the main view is to be removed.
     */
    public abstract void removeDataView(DataView dataView);

    /**
     * Returns DataView that contains the specified data object. It is useful to
     * avoid redundant display of data object that is opened already.
     *
     * @param dataObject
     *            the object whose presence in the main view is to be tested.
     *
     * @return DataView contains the specified data object, null if the data
     *         object is not displayed.
     */
    public abstract DataView getDataView(HObject dataObject);

    /**
     * Display feedback message
     *
     * @param msg
     *            the status message to display
     */
    public abstract void showStatus(String msg);

    /**
     * Display error message
     *
     * @param errMsg
     *            the error message to display
     */
    public abstract void showError(String errMsg);

    /**
     * Get the current TreeView
     *
     * @return the current TreeView
     */
    public abstract TreeView getTreeView();

    /**
     * Start stop a timer.
     *
     * @param toggleTimer
     *            -- true: start timer, false stop timer.
     */
    public abstract void executeTimer(boolean toggleTimer);
}
