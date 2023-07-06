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

package hdf.view.MetaDataView;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5Link;
import hdf.object.nc2.NC2Group;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;

/**
 *
 * The metadata view interface for displaying group metadata information
 */
public class DefaultGroupMetaDataView extends DefaultLinkMetaDataView implements MetaDataView {

    private static final Logger log = LoggerFactory.getLogger(DefaultGroupMetaDataView.class);

    /**
     *The metadata view interface for displaying metadata information
     *
     * @param parentComposite
     *        the parent visual object
     * @param viewer
     *        the viewer to use
     * @param theObj
     *        the object to display the metadata info
     */
    public DefaultGroupMetaDataView(Composite parentComposite, DataViewManager viewer, HObject theObj) {
        super(parentComposite, viewer, theObj);
    }

    @Override
    protected void addObjectSpecificContent() {
        super.addObjectSpecificContent();

        Group g = (Group) dataObject;
        List<?> mlist = g.getMemberList();
        int n = mlist.size();

        log.trace("addObjectSpecificContent(): group object extra info mlist size = {}", n);

        Label label;

        if (isH5) {
            StringBuilder objCreationStr = new StringBuilder("Creation Order NOT Tracked");

            long ocplID = -1;
            try {
                if (g.isRoot()) {
                    ocplID = H5.H5Fget_create_plist(g.getFID());
                }
                else {
                    long objid = -1;
                    try {
                        objid = g.open();
                        if (objid >= 0) {
                            ocplID = H5.H5Gget_create_plist(objid);
                        }
                    }
                    finally {
                        g.close(objid);
                    }
                }
                if (ocplID >= 0) {
                    int creationOrder = H5.H5Pget_link_creation_order(ocplID);
                    log.trace("createGeneralObjectInfoPane(): creationOrder={}", creationOrder);
                    if ((creationOrder & HDF5Constants.H5P_CRT_ORDER_TRACKED) > 0) {
                        objCreationStr.setLength(0);
                        objCreationStr.append("Creation Order Tracked");
                        if ((creationOrder & HDF5Constants.H5P_CRT_ORDER_INDEXED) > 0)
                            objCreationStr.append(" and Indexed");
                    }
                }
            }
            finally {
                H5.H5Pclose(ocplID);
            }

            /* Creation order section */
            label = new Label(generalObjectInfoPane, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Link Creation Order: ");

            Text text = new Text(generalObjectInfoPane, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(objCreationStr.toString());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        }
        if (isN3) {
            StringBuilder objDimensionStr = new StringBuilder("No Dimensions");
            int[] listDimSelector = { 0, 0, 1};
            try {
                List ncDimensions = ((NC2Group)g).getMetadata(listDimSelector);
                if (ncDimensions != null) {
                    int listCnt = ncDimensions.size();
                    log.trace("createGeneralObjectInfoPane(): ncDimensions={}", listCnt);
                    if (listCnt > 0)
                        objDimensionStr.setLength(0);
                    for (int i = 0; i < listCnt; i++) {
                        objDimensionStr.append(((NC2Group)g).netcdfDimensionString(i));
                        if (i < listCnt - 1)
                            objDimensionStr.append("\n");
                    }
                }
            }
            catch (Exception e) {
                log.debug("Error retrieving dimensions of object '" + dataObject.getName() + "':", e);
            }

            /* Dimensions section */
            label = new Label(generalObjectInfoPane, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Dimensions: ");

            ScrolledComposite dimensionScroller = new ScrolledComposite(generalObjectInfoPane, SWT.V_SCROLL | SWT.BORDER);
            dimensionScroller.setExpandHorizontal(true);
            dimensionScroller.setExpandVertical(true);
            dimensionScroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

            Text text = new Text(dimensionScroller, SWT.MULTI | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(objDimensionStr.toString());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            dimensionScroller.setContent(text);

            StringBuilder objEnumTypedefStr = new StringBuilder("No Enums");
            int[] listEnumSelector = { 0, 0, 0, 1};
            try {
                List ncEnums = ((NC2Group)g).getMetadata(listEnumSelector);
                if (ncEnums != null) {
                    int listCnt = ncEnums.size();
                    log.trace("createGeneralObjectInfoPane(): ncEnums={}", listCnt);
                    if (listCnt > 0)
                        objEnumTypedefStr.setLength(0);
                    for (int i = 0; i < listCnt; i++) {
                        objEnumTypedefStr.append(((NC2Group)g).netcdfTypedefString(i));
                        if (i < listCnt - 1)
                            objEnumTypedefStr.append("\n");
                    }
                }
            }
            catch (Exception e) {
                log.debug("Error retrieving enums of object '" + dataObject.getName() + "':", e);
            }

            /* Dimensions section */
            label = new Label(generalObjectInfoPane, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Enums: ");

            ScrolledComposite enumScroller = new ScrolledComposite(generalObjectInfoPane, SWT.V_SCROLL | SWT.BORDER);
            enumScroller.setExpandHorizontal(true);
            enumScroller.setExpandVertical(true);
            enumScroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

            text = new Text(enumScroller, SWT.MULTI | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(objEnumTypedefStr.toString());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            enumScroller.setContent(text);
        }
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
            label.setText("Number of members: " + n + " (in memory)," + "" + g.getNumberOfMembersInFile() + " (in file)");
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

        if (mlist != null && n > 0) {
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

            for (int i = 0; i < rowData.length; i++) {
                TableItem item = new TableItem(memberTable, SWT.NONE);
                item.setFont(curFont);
                item.setText(0, rowData[i][0]);
                item.setText(1, rowData[i][1]);
            }

            // set cell height for large fonts
            // int cellRowHeight = Math.max(16,
            // table.getFontMetrics(table.getFont()).getHeight());
            // table.setRowHeight(cellRowHeight);
        }

        for (int i = 0; i < columnNames.length; i++) {
            memberTable.getColumn(i).pack();
        }
    }

}
