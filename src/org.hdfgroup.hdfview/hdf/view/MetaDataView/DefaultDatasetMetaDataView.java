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

import java.lang.reflect.Array;

import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.DataView.DataViewManager;
import hdf.view.Tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 *
 * The metadata view interface for displaying dataset metadata information
 */
public class DefaultDatasetMetaDataView extends DefaultLinkMetaDataView implements MetaDataView {

    private static final Logger log = LoggerFactory.getLogger(DefaultDatasetMetaDataView.class);

    /**
     *The metadata view interface for displaying dataset metadata information
     *
     * @param parentComposite
     *        the parent visual object
     * @param viewer
     *        the viewer to use
     * @param theObj
     *        the object to display the metadata info
     */
    public DefaultDatasetMetaDataView(Composite parentComposite, DataViewManager viewer, HObject theObj)
    {
        super(parentComposite, viewer, theObj);
    }

    @Override
    protected void addObjectSpecificContent()
    {

        super.addObjectSpecificContent();

        String labelInfo;
        Label label;
        Text text;

        Dataset d = (Dataset)dataObject;
        if (!d.isInited()) {
            d.init();
        }

        org.eclipse.swt.widgets.Group datasetInfoGroup =
            new org.eclipse.swt.widgets.Group(generalObjectInfoPane, SWT.NONE);
        datasetInfoGroup.setFont(curFont);
        datasetInfoGroup.setText("Dataset Dataspace and Datatype");
        datasetInfoGroup.setLayout(new GridLayout(2, false));
        datasetInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        /* Dataset Rank section */
        label = new Label(datasetInfoGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("No. of Dimension(s): ");

        text = new Text(datasetInfoGroup, SWT.SINGLE | SWT.BORDER);
        text.setEditable(false);
        text.setFont(curFont);
        if (d.isNULL()) {
            labelInfo = "NULL";
        }
        else if (d.isScalar()) {
            labelInfo = "Scalar";
        }
        else {
            labelInfo = "" + d.getRank();
        }
        text.setText(labelInfo);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        if (!d.isScalar() && !d.isNULL()) {
            /* Dataset dimension size section */
            label = new Label(datasetInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Dimension Size(s): ");

            // Set Dimension Size
            String dimStr    = null;
            String maxDimStr = null;
            long dims[]      = d.getDims();
            long maxDims[]   = d.getMaxDims();
            if (dims != null) {
                String[] dimNames   = d.getDimNames();
                boolean hasDimNames = ((dimNames != null) && (dimNames.length == dims.length));
                StringBuilder sb    = new StringBuilder();
                StringBuilder sb2   = new StringBuilder();

                sb.append(dims[0]);
                if (hasDimNames) {
                    sb.append(" (").append(dimNames[0]).append(")");
                }

                if (maxDims[0] < 0)
                    sb2.append("Unlimited");
                else
                    sb2.append(maxDims[0]);

                for (int i = 1; i < dims.length; i++) {
                    sb.append(" x ");
                    sb.append(dims[i]);
                    if (hasDimNames) {
                        sb.append(" (").append(dimNames[i]).append(")");
                    }

                    sb2.append(" x ");
                    if (maxDims[i] < 0)
                        sb2.append("Unlimited");
                    else
                        sb2.append(maxDims[i]);
                }
                dimStr    = sb.toString();
                maxDimStr = sb2.toString();
            }

            text = new Text(datasetInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText((dimStr == null) ? "null" : dimStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            label = new Label(datasetInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Max Dimension Size(s): ");

            text = new Text(datasetInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText((maxDimStr == null) ? "null" : maxDimStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        }

        /* Dataset datatype section */
        label = new Label(datasetInfoGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Data Type: ");

        Datatype t  = d.getDatatype();
        String type = (t == null) ? "null" : t.getDescription();
        if (d instanceof CompoundDS) {
            if (isH4) {
                type = "Vdata";
            }
            else {
                /*
                 * For Compounds, Arrays of Compounds, Vlens of Compounds, etc. we want to show
                 * the fully-qualified type, minus the compound members, since we already show
                 * the Compound datatype's members in a table.
                 */
                int bracketIndex     = type.indexOf('{');
                int lastBracketIndex = type.lastIndexOf('}');
                if (bracketIndex >= 0 && lastBracketIndex >= 0) {
                    type = type.replace(type.substring(bracketIndex, lastBracketIndex + 1), "");
                }
            }
        }

        text = new Text(datasetInfoGroup, SWT.SINGLE | SWT.BORDER);
        text.setEditable(false);
        text.setFont(curFont);
        text.setText(type);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        /* Add a dummy label to take up some vertical space between sections */
        label = new Label(generalObjectInfoPane, SWT.LEFT);
        label.setText("");
        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        /*
         * Dataset storage layout, compression, filters, storage type, fill value, etc.
         * section
         */
        org.eclipse.swt.widgets.Group datasetLayoutGroup =
            new org.eclipse.swt.widgets.Group(generalObjectInfoPane, SWT.NONE);
        datasetLayoutGroup.setFont(curFont);
        datasetLayoutGroup.setText("Miscellaneous Dataset Information");
        datasetLayoutGroup.setLayout(new GridLayout(2, false));
        datasetLayoutGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        /* Dataset Storage Layout section */
        label = new Label(datasetLayoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Storage Layout: ");

        label = new Label(datasetLayoutGroup, SWT.RIGHT);
        label.setFont(curFont);
        labelInfo = d.getStorageLayout();
        if (labelInfo == null)
            labelInfo = "UNKNOWN";
        label.setText(labelInfo);

        /* Dataset Compression section */
        label = new Label(datasetLayoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Compression: ");

        label = new Label(datasetLayoutGroup, SWT.RIGHT);
        label.setFont(curFont);
        labelInfo = d.getCompression();
        if (labelInfo == null)
            labelInfo = "UNKNOWN";
        label.setText(labelInfo);

        /* Dataset filters section */
        label = new Label(datasetLayoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Filters: ");

        label = new Label(datasetLayoutGroup, SWT.RIGHT);
        label.setFont(curFont);
        labelInfo = d.getFilters();
        if (labelInfo == null)
            labelInfo = "UNKNOWN";
        label.setText(labelInfo);

        /* Dataset extra storage information section */
        label = new Label(datasetLayoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Storage: ");

        label = new Label(datasetLayoutGroup, SWT.RIGHT);
        label.setFont(curFont);
        labelInfo = d.getStorage();
        if (labelInfo == null)
            labelInfo = "UNKNOWN";
        label.setText(labelInfo);

        /* Dataset fill value info section */
        label = new Label(datasetLayoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Fill value: ");

        Object fillValue     = null;
        String fillValueInfo = "NONE";
        if (d instanceof ScalarDS)
            fillValue = ((ScalarDS)d).getFillValue();
        if (fillValue != null) {
            if (fillValue.getClass().isArray()) {
                int len       = Array.getLength(fillValue);
                fillValueInfo = Array.get(fillValue, 0).toString();
                for (int i = 1; i < len; i++) {
                    fillValueInfo += ", ";
                    fillValueInfo += Array.get(fillValue, i).toString();
                }
            }
            else
                fillValueInfo = fillValue.toString();
        }

        label = new Label(datasetLayoutGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText(fillValueInfo);

        /* Button to open Data Option dialog */
        Button showDataOptionButton = new Button(datasetInfoGroup, SWT.PUSH);
        showDataOptionButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 2, 1));
        showDataOptionButton.setText("Show Data with Options");
        showDataOptionButton.setEnabled(!d.isNULL());
        showDataOptionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try {
                    viewManager.getTreeView().setDefaultDisplayMode(false);
                    viewManager.getTreeView().showDataContent(dataObject);
                }
                catch (Exception ex) {
                    display.beep();
                    Tools.showError(display.getShells()[0], "Select", ex.getMessage());
                }
            }
        });

        /*
         * If this is a Compound Dataset, add a table which displays all of the members
         * in the Compound Datatype.
         */
        if (d instanceof CompoundDS) {
            log.trace("addObjectSpecificContent(): add member table for Compound Datatype Dataset");

            CompoundDS compound = (CompoundDS)d;

            int n = compound.getMemberCount();
            log.trace("addObjectSpecificContent(): number of compound members={}", n);

            // Add a dummy label to take up some vertical space between sections
            label = new Label(generalObjectInfoPane, SWT.LEFT);
            label.setText("");
            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

            org.eclipse.swt.widgets.Group compoundMembersGroup =
                new org.eclipse.swt.widgets.Group(generalObjectInfoPane, SWT.NONE);
            compoundMembersGroup.setFont(curFont);
            compoundMembersGroup.setText("Compound Dataset Members");
            compoundMembersGroup.setLayout(new FillLayout(SWT.VERTICAL));
            compoundMembersGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

            Table memberTable = new Table(compoundMembersGroup, SWT.BORDER);
            memberTable.setLinesVisible(true);
            memberTable.setHeaderVisible(true);
            memberTable.setFont(curFont);

            String[] columnNames = {"Name", "Type", "Array Size"};

            for (int i = 0; i < columnNames.length; i++) {
                TableColumn column = new TableColumn(memberTable, SWT.NONE);
                column.setText(columnNames[i]);
                column.setMoveable(false);
            }

            if (n > 0) {
                String rowData[][]   = new String[n][3];
                final String names[] = compound.getMemberNames();
                Datatype types[]     = compound.getMemberTypes();
                int orders[]         = compound.getMemberOrders();

                for (int i = 0; i < n; i++) {
                    rowData[i][0] = new String(names[i]);

                    if (rowData[i][0].contains(CompoundDS.SEPARATOR)) {
                        rowData[i][0] = rowData[i][0].replaceAll(CompoundDS.SEPARATOR, "->");
                    }

                    int mDims[] = compound.getMemberDims(i);
                    if (mDims == null) {
                        rowData[i][2] = String.valueOf(orders[i]);

                        if (isH4 && types[i].isString()) {
                            rowData[i][2] = String.valueOf(types[i].getDatatypeSize());
                        }
                    }
                    else {
                        String mStr = String.valueOf(mDims[0]);
                        int m       = mDims.length;
                        for (int j = 1; j < m; j++) {
                            mStr += " x " + mDims[j];
                        }
                        rowData[i][2] = mStr;
                    }
                    rowData[i][1] = (types[i] == null) ? "null" : types[i].getDescription();
                }

                for (int i = 0; i < rowData.length; i++) {
                    TableItem item = new TableItem(memberTable, SWT.NONE);
                    item.setFont(curFont);
                    item.setText(0, rowData[i][0]);
                    item.setText(1, rowData[i][1]);
                    item.setText(2, rowData[i][2]);
                }

                for (int i = 0; i < columnNames.length; i++) {
                    memberTable.getColumn(i).pack();
                }

                // set cell height for large fonts
                // int cellRowHeight = Math.max(16,
                // table.getFontMetrics(table.getFont()).getHeight());
                // table.setRowHeight(cellRowHeight);
            } //  (n > 0)

            // Prevent conflict from equal vertical grabbing
            datasetLayoutGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        }
    }
}
