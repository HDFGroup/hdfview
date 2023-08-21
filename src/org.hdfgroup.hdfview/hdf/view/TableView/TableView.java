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

package hdf.view.TableView;

import hdf.view.DataView.DataView;

/**
 *
 * The table view interface for displaying data in table form
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public abstract interface TableView extends DataView {
    /** @return the table */
    public abstract Object getTable();

    /** @return array of selected data */
    public abstract Object getSelectedData();

    /** @return array of selected column count */
    public abstract int getSelectedColumnCount();

    /** @return array of selected row count */
    public abstract int getSelectedRowCount();

    /** Write the change of a dataset into file. */
    public abstract void updateValueInFile();

    /** refresh the data table. */
    public abstract void refreshDataTable();
}
