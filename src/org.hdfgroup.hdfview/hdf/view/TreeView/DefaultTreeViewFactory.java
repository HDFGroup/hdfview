/*****************************************************************************
 * Copyright by The HDF Group.                                               *
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

package hdf.view.TreeView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.widgets.Composite;

import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;

/**
 * A simple Factory class which returns concrete instances of the default
 * TreeView.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
public class DefaultTreeViewFactory extends TreeViewFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultTreeViewFactory.class);

    @Override
    public TreeView getTreeView(Composite parent, DataViewManager viewer) throws ClassNotFoundException {
        String dataViewName = null;
        Object[] initargs = { parent, viewer };
        TreeView theView = null;

        dataViewName = ViewProperties.DEFAULT_TREEVIEW_NAME;

        Class<?> theClass = null;
        try {
            log.trace("getTreeView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("getTreeView(): unable to load default TreeView class by name({})", dataViewName);
            theClass = null;
        }

        if (theClass == null) throw new ClassNotFoundException();

        try {
            theView = (TreeView) Tools.newInstance(theClass, initargs);

            log.trace("getTreeView(): returning TreeView instance {}", theView);
        }
        catch (Exception ex) {
            log.debug("getTreeView(): Error instantiating class:", ex);
            theView = null;
        }

        return theView;
    }

}
