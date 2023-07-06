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

package hdf.view.HelpView;

/**
 *
 * The helpview interface for displaying user help information
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public abstract interface HelpView {
    /** Display help information */
    public abstract void show();

    /**
     * @return the HelpView's label, which is displayed in the HDFView
     * help menu.
     */
    public abstract String getLabel();

    /** @return the action command for this HelpView. */
    public abstract String getActionCommand();
}
