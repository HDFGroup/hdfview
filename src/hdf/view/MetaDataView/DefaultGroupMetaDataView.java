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

package hdf.view.MetaDataView;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5Link;
import hdf.view.ViewManager;
import hdf.view.ViewProperties;

public class DefaultGroupMetaDataView extends DefaultLinkMetaDataView implements MetaDataView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultGroupMetaDataView.class);

    public DefaultGroupMetaDataView(Composite parentComposite, ViewManager viewer, HObject theObj) {
        super(parentComposite, viewer, theObj);
    }

    @Override
    protected void addObjectSpecificContent() {
        log.trace("addObjectSpecificContent(): start");

        super.addObjectSpecificContent();

        Group g = (Group) dataObject;
        List<?> mlist = g.getMemberList();
        int n = mlist.size();

        log.trace("addObjectSpecificContent(): group object extra info mlist size = {}", n);

        if (mlist != null && n > 0) {
            Label label;

            org.eclipse.swt.widgets.Group groupInfoGroup = new org.eclipse.swt.widgets.Group(generalObjectInfoPane, SWT.NONE);
            groupInfoGroup.setFont(curFont);
            groupInfoGroup.setText("Group Members");
            groupInfoGroup.setLayout(new GridLayout(1, true));
            groupInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

            if (g.getNumberOfMembersInFile() < ViewProperties.getMaxMembers()) {
                label = new Label(groupInfoGroup, SWT.RIGHT);
                label.setFont(curFont);
                label.setText("Number of members: " + n);
            }
            else {
                label = new Label(groupInfoGroup, SWT.RIGHT);
                label.setFont(curFont);
                label.setText(
                        "Number of members: " + n + " (in memory)," + "" + g.getNumberOfMembersInFile() + " (in file)");
            }

            String rowData[][] = new String[n][2];
            for (int i = 0; i < n; i++) {
                HObject theObj = (HObject) mlist.get(i);
                rowData[i][0] = theObj.getName();
                if (theObj instanceof Group) {
                    rowData[i][1] = "Group";
                }
                else if (theObj instanceof Dataset) {
                    rowData[i][1] = "Dataset";
                }
                else if (theObj instanceof Datatype) {
                    rowData[i][1] = "Datatype";
                }
                else if (theObj instanceof H5Link) {
                    rowData[i][1] = "Link";
                }
                else
                    rowData[i][1] = "Unknown";
            }

            String[] columnNames = { "Name", "Type" };

            Table memberTable = new Table(groupInfoGroup, SWT.BORDER);
            memberTable.setLinesVisible(true);
            memberTable.setHeaderVisible(true);
            memberTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            memberTable.setFont(curFont);

            for (int i = 0; i < columnNames.length; i++) {
                TableColumn column = new TableColumn(memberTable, SWT.NONE);
                column.setText(columnNames[i]);
                column.setMoveable(false);
            }

            for (int i = 0; i < rowData.length; i++) {
                TableItem item = new TableItem(memberTable, SWT.NONE);
                item.setFont(curFont);
                item.setText(0, rowData[i][0]);
                item.setText(1, rowData[i][1]);
            }

            for (int i = 0; i < columnNames.length; i++) {
                memberTable.getColumn(i).pack();
            }

            // set cell height for large fonts
            // int cellRowHeight = Math.max(16,
            // table.getFontMetrics(table.getFont()).getHeight());
            // table.setRowHeight(cellRowHeight);
        }

        log.trace("addObjectSpecificContent(): finish");
    }

}
