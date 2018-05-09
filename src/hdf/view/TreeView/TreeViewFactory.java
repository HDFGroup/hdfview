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

import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import hdf.object.HObject;
import hdf.view.DataViewFactory;
import hdf.view.Tools;
import hdf.view.ViewManager;
import hdf.view.ViewProperties;
import hdf.view.ImageView.ImageView;
import hdf.view.MetaDataView.MetaDataView;
import hdf.view.PaletteView.PaletteView;
import hdf.view.TableView.TableView;

/**
 * A Factory class to return instances of classes implementing the TreeView
 * interface, depending on the "current selected" TreeView class in the list
 * maintained by the ViewProperties class.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
public class TreeViewFactory extends DataViewFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TreeViewFactory.class);

    @SuppressWarnings("rawtypes")
    @Override
    public TableView getTableView(ViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ImageView getImageView(ViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException {
        return null;
    }

    @Override
    public PaletteView getPaletteView(Shell parent, ViewManager viewer, ImageView theImageView) throws ClassNotFoundException {
        return null;
    }

    @Override
    public MetaDataView getMetaDataView(Composite parentObj, ViewManager viewer, HObject theObj) throws ClassNotFoundException {
        return null;
    }

    @Override
    public TreeView getTreeView(Composite parent, ViewManager viewer) throws ClassNotFoundException {
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
        }

        log.trace("getTreeView(): finish");

        return theView;
    }

}
