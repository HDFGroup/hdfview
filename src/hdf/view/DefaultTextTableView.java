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

import java.util.HashMap;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.validate.DataValidator;
import org.eclipse.swt.widgets.Composite;

import hdf.object.DataFormat;

public class DefaultTextTableView extends DefaultBaseTableView implements TableView {

    /**
     * Constructs a TableView with no additional data properties for displaying text
     * data.
     *
     * @param theView
     *            the main HDFView.
     */
    public DefaultTextTableView(ViewManager theView) {
        this(theView, null);
    }

    /**
     * Constructs a TableView with the specified data properties for displaying text
     * data.
     *
     * @param theView
     *            the main HDFView.
     *
     * @param dataPropertiesMap
     *            the properties on how to show the data. The map is used to allow
     *            applications to pass properties on how to display the data, such
     *            as: transposing data, showing data as characters, applying a
     *            bitmask, and etc. Predefined keys are listed at
     *            ViewProperties.DATA_VIEW_KEY.
     */
    @SuppressWarnings("rawtypes")
    public DefaultTextTableView(ViewManager theView, HashMap dataPropertiesMap) {
        super(theView, dataPropertiesMap);
    }

    @Override
    public Object getSelectedData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateValueInFile() {
        // TODO Auto-generated method stub

    }

    @Override
    protected NatTable createTable(Composite parent, DataFormat dataObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void updateValueInMemory(String cellValue, int row, int coll) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    protected void showObjRefData(long ref) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void showRegRefData(String reg) {
        // TODO Auto-generated method stub

    }

    @Override
    protected DisplayConverter getDataDisplayConverter(DataFormat dataObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected DataValidator getDataValidator(DataFormat dataObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected IEditableRule getDataEditingRule(DataFormat dataObject) {
        // TODO Auto-generated method stub
        return null;
    }

}
