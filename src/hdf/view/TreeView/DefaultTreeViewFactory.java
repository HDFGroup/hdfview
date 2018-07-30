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

package hdf.view.TreeView;

import java.util.List;

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

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultTreeViewFactory.class);

    @Override
    public TreeView getTreeView(Composite parent, DataViewManager viewer) throws ClassNotFoundException {
        String dataViewName = null;
        Object[] initargs = { parent, viewer };
        TreeView theView = null;

        log.trace("getTreeView(): start");

        /* Retrieve the "currently selected" TreeView class to use */
        List<?> treeViewList = ViewProperties.getTreeViewList();
        if ((treeViewList == null) || (treeViewList.size() <= 0)) {
            return null;
        }

        dataViewName = (String) treeViewList.get(0);

        /* Attempt to load the class by name */
        Class<?> theClass = null;
        try {
            log.trace("getTreeView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("getTreeView(): Class.forName({}) failure:", dataViewName, ex);

            try {
                log.trace("getTreeView(): ViewProperties.loadExtClass().loadClass({})", dataViewName);

                /* Attempt to load the class as an external module */
                theClass = ViewProperties.loadExtClass().loadClass(dataViewName);
            }
            catch (Exception ex2) {
                log.debug("getTreeView(): ViewProperties.loadExtClass().loadClass({}) failure:", dataViewName, ex);

                /* No loadable class found; use the default PaletteView */
                dataViewName = ViewProperties.DEFAULT_TREEVIEW_NAME;

                try {
                    log.trace("getTreeView(): Class.forName({})", dataViewName);

                    theClass = Class.forName(dataViewName);
                }
                catch (Exception ex3) {
                    log.debug("getTreeView(): Class.forName({}) failure:", dataViewName, ex);

                    theClass = null;
                }
            }
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

        log.trace("getTreeView(): finish");

        return theView;
    }

}
