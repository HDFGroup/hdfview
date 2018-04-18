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

import java.io.File;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;
import hdf.object.ScalarDS;
import hdf.object.h5.H5Link;
import hdf.view.dialog.NewAttributeDialog;

/**
 * DefaultMetaDataView is a default implementation of the MetaDataView which
 * is used to show data properties of an object. Data properties include
 * attributes and general object information such as the object type, data type
 * and data space.
 *
 * @author Jordan T. Henderson
 * @version 2.4 3/12/2018
 */
public class DefaultMetaDataView implements MetaDataView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultMetaDataView.class);

    private final Display                 display = Display.getDefault();

    private final Composite               parent;

    private Font                          curFont;

    private final ViewManager             viewManager;

    /** The HDF data object */
    private DataFormat                    dataObject;

    /* The table to hold the list of attributes attached to the HDF object */
    private Table                         attrTable;

    private Label                         attrNumberLabel;

    private List<?>                       attrList;

    private int                           numAttributes;

    private boolean                       isH5, isH4;

    public DefaultMetaDataView(Composite parentObj, ViewManager theView, DataFormat theObj) {
        log.trace("start");

        this.parent = parentObj;
        this.viewManager = theView;
        this.dataObject = theObj;

        numAttributes = 0;

        isH5 = ((HObject) dataObject).getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        isH4 = ((HObject) dataObject).getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        try {
            curFont = new Font(
                    display,
                    ViewProperties.getFontType(),
                    ViewProperties.getFontSize(),
                    SWT.NORMAL);
        }
        catch (Exception ex) {
            curFont = null;
        }

        /* Get the metadata information before adding GUI components */
        try {
            attrList = ((MetaDataContainer) dataObject).getMetadata();
            if (attrList != null)
                numAttributes = attrList.size();
        }
        catch (Exception ex) {
            attrList = null;
            log.debug("Error retrieving metadata of object " + ((HObject) dataObject).getName() + ":", ex);
        }

        log.trace("isH5={} numAttributes={}", isH5, numAttributes);


        Composite generalObjectInfoPane = null;
        Composite attributeInfoPane = null;
        TabFolder tabFolder = new TabFolder(parent, SWT.NONE);

        attributeInfoPane = createAttributeInfoPane(tabFolder, dataObject);
        if (attributeInfoPane != null) {
            TabItem attributeInfoItem = new TabItem(tabFolder, SWT.None);
            attributeInfoItem.setText("Object Attribute Info");
            attributeInfoItem.setControl(attributeInfoPane);
        }

        generalObjectInfoPane = createGeneralObjectInfoPane(tabFolder, dataObject);
        if (generalObjectInfoPane != null) {
            TabItem generalInfoItem = new TabItem(tabFolder, SWT.None);
            generalInfoItem.setText("General Object Info");
            generalInfoItem.setControl(generalObjectInfoPane);
        }

        if (parent instanceof ScrolledComposite)
            ((ScrolledComposite) parent).setContent(tabFolder);

        log.trace("finish");
    }

    @Override
    public DataFormat getDataObject() {
        return dataObject;
    }

    @Override
    public Attribute addAttribute(HObject obj) {
        if (obj == null) return null;

        HObject root = obj.getFileFormat().getRootObject();
        NewAttributeDialog dialog = new NewAttributeDialog(display.getShells()[0], obj, ((Group) root).breadthFirstMemberList());
        dialog.open();

        Attribute attr = dialog.getAttribute();
        if (attr == null) {
            return null;
        }

        String rowData[] = new String[4]; // name, value, type, size

        rowData[0] = attr.getName();
        rowData[2] = attr.getDatatype().getDatatypeDescription();

        rowData[1] = attr.toString(", ");

        long dims[] = attr.getDims();

        rowData[3] = String.valueOf(dims[0]);
        for (int j = 1; j < dims.length; j++) {
            rowData[3] += " x " + dims[j];
        }

        TableItem item = new TableItem(attrTable, SWT.NONE);
        item.setFont(curFont);
        item.setText(rowData);

        numAttributes++;
        attrNumberLabel.setText("Number of attributes = " + numAttributes);

        return attr;
    }

    @Override
    public Attribute deleteAttribute(HObject obj) {
        if (obj == null) return null;

        int idx = attrTable.getSelectionIndex();
        if (idx < 0) {
            Tools.showError(display.getShells()[0], "No attribute is selected.", display.getShells()[0].getText());
            return null;
        }

        int answer = SWT.NO;
        if(MessageDialog.openConfirm(display.getShells()[0],
                display.getShells()[0].getText(), "Do you want to delete the selected attribute?"))
            answer = SWT.YES;
        if (answer == SWT.NO) return null;

        if (attrList == null) {
            return null;
        }

        Attribute attr = (Attribute) attrList.get(idx);
        try {
            ((MetaDataContainer) obj).removeMetadata(attr);
        }
        catch (Exception ex) {
            log.debug("delete an attribute from a data object:", ex);
        }

        attrTable.remove(idx);
        numAttributes--;

        attrNumberLabel.setText("Number of attributes = " + numAttributes);

        return attr;
    }

    private Composite createAttributeInfoPane(Composite parent, final DataFormat dataObject) {
        if (parent == null || dataObject == null || attrList == null) return null;

        log.trace("createAttributeInfoPane: start");

        org.eclipse.swt.widgets.Group attributeInfoGroup = null;

        attributeInfoGroup = new org.eclipse.swt.widgets.Group(parent, SWT.NONE);
        attributeInfoGroup.setFont(curFont);
        attributeInfoGroup.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        attributeInfoGroup.setLayout(new GridLayout(3, false));
        attributeInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        log.trace("createAttributeInfoPane:  numAttributes={}", numAttributes);

        attrNumberLabel = new Label(attributeInfoGroup, SWT.RIGHT);
        attrNumberLabel.setFont(curFont);
        attrNumberLabel.setText("Number of attributes = 0");
        attrNumberLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));

        Button addButton = new Button(attributeInfoGroup, SWT.PUSH);
        addButton.setFont(curFont);
        addButton.setText("Add Attribute");
        addButton.setEnabled(!(((HObject) dataObject).getFileFormat().isReadOnly()));
        addButton.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addAttribute((HObject) dataObject);
            }
        });

        /* Deleting attributes is not supported by HDF4 */
        if (isH5) {
            Button delButton = new Button(attributeInfoGroup, SWT.PUSH);
            delButton.setFont(curFont);
            delButton.setText("Delete Attribute");
            delButton.setEnabled(!(((HObject) dataObject).getFileFormat().isReadOnly()));
            delButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
            delButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    deleteAttribute((HObject) dataObject);
                }
            });
        }
        else {
            addButton.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false, 2, 1));
        }

        String[] columnNames = { "Name", "Type", "Array Size", "Value" };

        attrTable = new Table(attributeInfoGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        attrTable.setLinesVisible(true);
        attrTable.setHeaderVisible(true);
        attrTable.setFont(curFont);
        attrTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        attrTable.addListener(SWT.MouseDoubleClick, attrTableCellEditor);

        for(int i = 0; i < columnNames.length; i++) {
            TableColumn column = new TableColumn(attrTable, SWT.NONE);
            column.setText(columnNames[i]);
            column.setMoveable(false);

            /* Make sure all columns show even when the object in question has no attributes */
            if (i == columnNames.length -1)
                column.setWidth(200);
            else
                column.setWidth(50);
        }

        if (attrList == null) {
            log.trace("createAttributeInfoPane:  attrList == null");
        }
        else {
            attrNumberLabel.setText("Number of attributes = " + numAttributes);

            Attribute attr = null;
            String name, type, size;
            for (int i = 0; i < numAttributes; i++) {
                attr = (Attribute) attrList.get(i);
                name = attr.getName();
                type = attr.getDatatype().getDatatypeDescription();
                log.trace("createAttributeInfoPane:  attr[{}] is {} as {}", i, name, type);

                if (name == null) name = "null";
                if (type == null) type = "null";

                if (attr.isScalar()) {
                    size = "Scalar";
                }
                else {
                    long dims[] = attr.getDims();
                    size = String.valueOf(dims[0]);
                    for (int j = 1; j < dims.length; j++) {
                        size += " x " + dims[j];
                    }

                    if (size == null) size = "null";
                }

                TableItem item = new TableItem(attrTable, SWT.NONE);
                item.setFont(curFont);
                item.setData(attr);

                if (attr.getProperty("field") != null) {
                    String fieldInfo = " {Field: "+attr.getProperty("field")+"}";
                    item.setText(0, (name + fieldInfo == null) ? "null" : name + fieldInfo);
                }
                else {
                    item.setText(0, (name == null) ? "null" : name);
                }

                String value = attr.toString(", ", 50);
                if (value == null) value = "null";

                item.setText(1, type);
                item.setText(2, size);
                item.setText(3, value);
            } // for (int i=0; i<n; i++)

            for(int i = 0; i < columnNames.length; i++) {
                attrTable.getColumn(i).pack();
            }

            // Prevent attributes with many values, such as array types, from making
            // the window too wide
            attrTable.getColumn(3).setWidth(200);
        }

        log.trace("createAttributeInfoPane: finish");

        return attributeInfoGroup;
    }

    private Composite createGeneralObjectInfoPane(Composite parent, final DataFormat dataObject) {
        if (parent == null || dataObject == null) return null;

        log.trace("createGeneralObjectInfoPane: start");

        FileFormat theFile = ((HObject) dataObject).getFileFormat();
        boolean isRoot = ((dataObject instanceof Group) && ((Group) dataObject).isRoot());
        String     typeStr = "Unknown";
        Label      label;
        Text       text;

        /* Add an SWT Group to encompass all of the GUI components */
        org.eclipse.swt.widgets.Group generalInfoGroup = new org.eclipse.swt.widgets.Group(parent, SWT.NONE);
        generalInfoGroup.setFont(curFont);
        generalInfoGroup.setLayout(new GridLayout(2, false));
        generalInfoGroup.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        /* If this is the root group, add some special info, like the Library version bounds */
        if (isRoot) {
            log.trace("createGeneralObjectInfoPane: isRoot");

            long fileSize = 0;
            try {
                fileSize = (new File(((HObject) dataObject).getFile())).length();
            }
            catch (Exception ex) {
                fileSize = -1;
            }
            fileSize /= 1024;

            // Retrieve the number of subgroups and datasets in the root group
            HObject root = theFile.getRootObject();
            HObject theObj = null;
            Iterator<HObject> it = ((Group) root).depthFirstMemberList().iterator();
            int groupCount = 0, datasetCount = 0;

            while(it.hasNext()) {
                theObj = it.next();

                if(theObj instanceof Group) {
                    groupCount++;
                }
                else {
                    datasetCount++;
                }
            }

            log.trace("createGeneralObjectInfoPane: isRoot get fileinfo");

            // Append all of the file's information to the general object info pane
            String fileInfo = "";

            fileInfo = "size=" + fileSize + "K,  groups=" + groupCount + ",  datasets=" + datasetCount;

            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("File Name: ");

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(((HObject) dataObject).getName());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("File Path: ");

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText((new File(((HObject) dataObject).getFile())).getParent());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("File Type: ");

            if (isH5) {
                typeStr = "HDF5,  " + fileInfo;
            }
            else if (isH4) {
                typeStr = "HDF4,  " + fileInfo;
            }
            else {
                typeStr = fileInfo;
            }

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(typeStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            log.trace("createGeneralObjectInfoPane: isRoot get version bounds");

            // Append the Library Version bounds information
            if (isH5) {
                String libversion = ((HObject) dataObject).getFileFormat().getLibBoundsDescription();
                if (libversion.length() > 0) {
                    label = new Label(generalInfoGroup, SWT.LEFT);
                    label.setFont(curFont);
                    label.setText("Library version bounds: ");

                    text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
                    text.setEditable(false);
                    text.setFont(curFont);
                    text.setText(libversion);
                    text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                }

                Button userBlockButton = new Button(generalInfoGroup, SWT.PUSH);
                userBlockButton.setText("Show User Block");
                userBlockButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        new UserBlockDialog(display.getShells()[0], SWT.NONE, (HObject) dataObject).open();
                    }
                });
            }
        }
        else {
            log.trace("createGeneralObjectInfoPane: isNotRoot");

            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Name: ");

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(((HObject) dataObject).getName());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            // For HDF5 links, add a box to allow changing of the link target
            if (isH5) {
                if (((HObject) dataObject).getLinkTargetObjName() != null) {
                    final HObject theObj = (HObject) dataObject;

                    log.trace("createGeneralObjectInfoPane: isNotRoot H5 get link target");
                    label = new Label(generalInfoGroup, SWT.LEFT);
                    label.setFont(curFont);
                    label.setText("Link To Target: ");

                    final Text linkTarget = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
                    linkTarget.setFont(curFont);
                    linkTarget.setText(((HObject) dataObject).getLinkTargetObjName());
                    linkTarget.addTraverseListener(new TraverseListener() {
                        @Override
                        public void keyTraversed(TraverseEvent e) {
                            if (e.detail == SWT.TRAVERSE_RETURN) {
                                Group pgroup = null;
                                try {
                                    pgroup = (Group) theObj.getFileFormat().get(theObj.getPath());
                                }
                                catch (Exception ex) {
                                    log.debug("parent group:", ex);
                                }
                                if (pgroup == null) {
                                    display.beep();
                                    Tools.showError(display.getShells()[0], "Parent group is null.", display.getShells()[0].getText());
                                    return;
                                }

                                String target_name = linkTarget.getText();
                                if (target_name != null) target_name = target_name.trim();

                                int linkType = Group.LINK_TYPE_SOFT;
                                if (theObj.getLinkTargetObjName().contains(FileFormat.FILE_OBJ_SEP))
                                    linkType = Group.LINK_TYPE_EXTERNAL;
                                else if (target_name.equals("/")) { // do not allow to link to the root
                                    display.beep();
                                    Tools.showError(display.getShells()[0], "Link to root not allowed.", display.getShells()[0].getText());
                                    return;
                                }

                                // no change
                                if (target_name.equals(theObj.getLinkTargetObjName())) return;

                                // invalid name
                                if (target_name == null || target_name.length() < 1) return;

                                try {
                                    theObj.getFileFormat().createLink(pgroup, theObj.getName(), target_name, linkType);
                                    theObj.setLinkTargetObjName(target_name);
                                }
                                catch (Exception ex) {
                                    display.beep();
                                    Tools.showError(display.getShells()[0], ex.getMessage(), display.getShells()[0].getText());
                                    return;
                                }

                                MessageDialog.openInformation(display.getShells()[0], display.getShells()[0].getText(), "Link target changed.");
                            }
                        }
                    });
                }
            }

            log.trace("createGeneralObjectInfoPane: isNotRoot get type of object");

            // Append information about the type of the object
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Path: ");

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(((HObject) dataObject).getPath());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Type: ");

            if(isH5) {
                if (dataObject instanceof Group) {
                    typeStr = "HDF5 Group";
                }
                else if (dataObject instanceof ScalarDS) {
                    typeStr = "HDF5 Dataset";
                }
                else if (dataObject instanceof CompoundDS) {
                    typeStr = "HDF5 Dataset";
                }
                else if (dataObject instanceof Datatype) {
                    typeStr = "HDF5 Named Datatype";
                }
            }
            else if(isH4) {
                if (dataObject instanceof Group) {
                    typeStr = "HDF4 Group";
                }
                else if (dataObject instanceof ScalarDS) {
                    ScalarDS ds = (ScalarDS) dataObject;
                    if (ds.isImage()) {
                        typeStr = "HDF4 Raster Image";
                    }
                    else {
                        typeStr = "HDF4 SDS";
                    }
                }
                else if (dataObject instanceof CompoundDS) {
                    typeStr = "HDF4 Vdata";
                }
            }
            else {
                if (dataObject instanceof Group) {
                    typeStr = "Group";
                }
                else if (dataObject instanceof ScalarDS) {
                    typeStr = "Dataset";
                }
                else if (dataObject instanceof CompoundDS) {
                    typeStr = "Dataset";
                }
            }

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(typeStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));


            // Append object ID information about the object
            if (isH5) {
                label = new Label(generalInfoGroup, SWT.LEFT);
                label.setFont(curFont);
                label.setText("Object Ref:       ");
            }
            else {
                label = new Label(generalInfoGroup, SWT.LEFT);
                label.setFont(curFont);
                label.setText("Tag, Ref:        ");
            }

            // bug #926 to remove the OID, put it back on Nov. 20, 2008, --PC
            String oidStr = null;
            long[] OID = ((HObject) dataObject).getOID();
            if (OID != null) {
                oidStr = String.valueOf(OID[0]);
                if (isH4) oidStr += ", " + OID[1];
            }

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(oidStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        }

        // Add a dummy label to take up some vertical space between sections
        label = new Label(generalInfoGroup, SWT.LEFT);
        label.setText("");
        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));


        // Add any extra information depending on object type
        log.trace("createGeneralObjectInfoPane: show extra object info");

        if (dataObject instanceof Group) {
            log.trace("showMetaData: group object extra info");
            Group g = (Group) dataObject;
            List<?> mlist = g.getMemberList();
            int n = mlist.size();

            log.trace("showMetaData: group object extra info mlist = {}",n);
            if (mlist != null && n > 0) {
                org.eclipse.swt.widgets.Group groupInfoGroup = new org.eclipse.swt.widgets.Group(generalInfoGroup, SWT.NONE);
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
                    label.setText("Number of members: " + n + " (in memory),"
                            + "" + g.getNumberOfMembersInFile() + " (in file)");
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

                for(int i = 0; i < columnNames.length; i++) {
                    TableColumn column = new TableColumn(memberTable, SWT.NONE);
                    column.setText(columnNames[i]);
                    column.setMoveable(false);
                }

                for(int i = 0; i < rowData.length; i++) {
                    TableItem item = new TableItem(memberTable, SWT.NONE);
                    item.setFont(curFont);
                    item.setText(0, rowData[i][0]);
                    item.setText(1, rowData[i][1]);
                }

                for(int i = 0; i < columnNames.length; i++) {
                    memberTable.getColumn(i).pack();
                }

                // set cell height for large fonts
                //int cellRowHeight = Math.max(16, table.getFontMetrics(table.getFont()).getHeight());
                //table.setRowHeight(cellRowHeight);
            }
        }
        else if (dataObject instanceof Dataset) {
            log.trace("showMetaData: Dataset object extra info");
            Dataset d = (Dataset) dataObject;
            if (d.getRank() <= 0) {
                d.init();
            }

            org.eclipse.swt.widgets.Group datasetInfoGroup = new org.eclipse.swt.widgets.Group(generalInfoGroup, SWT.NONE);
            datasetInfoGroup.setFont(curFont);
            datasetInfoGroup.setText("Dataset Dataspace and Datatype");
            datasetInfoGroup.setLayout(new GridLayout(2, false));
            datasetInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            String labelinfo;

            log.trace("showMetaData: Dataset object extra info - dimesions");
            label = new Label(datasetInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("No. of Dimension(s): ");

            text = new Text(datasetInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            labelinfo = "" + d.getRank();
            text.setText(labelinfo);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            label = new Label(datasetInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Dimension Size(s): ");

            // Set Dimension Size
            String dimStr = null;
            String maxDimStr = null;
            long dims[] = d.getDims();
            long maxDims[] = d.getMaxDims();
            if (dims != null) {
                String[] dimNames = d.getDimNames();
                boolean hasDimNames = ((dimNames != null) && (dimNames.length == dims.length));
                StringBuffer sb = new StringBuffer();
                StringBuffer sb2 = new StringBuffer();

                sb.append(dims[0]);
                if (hasDimNames) {
                    sb.append(" (");
                    sb.append(dimNames[0]);
                    sb.append(")");
                }

                if (maxDims[0] < 0)
                    sb2.append("Unlimited");
                else
                    sb2.append(maxDims[0]);

                for (int i = 1; i < dims.length; i++) {
                    sb.append(" x ");
                    sb.append(dims[i]);
                    if (hasDimNames) {
                        sb.append(" (");
                        sb.append(dimNames[i]);
                        sb.append(")");
                    }

                    sb2.append(" x ");
                    if (maxDims[i] < 0)
                        sb2.append("Unlimited");
                    else
                        sb2.append(maxDims[i]);

                }
                dimStr = sb.toString();
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

            log.trace("showMetaData: Dataset object extra info - datatype");
            label = new Label(datasetInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Data Type: ");

            String type = null;
            if (d instanceof ScalarDS) {
                ScalarDS sd = (ScalarDS) d;
                Datatype t = sd.getDatatype();
                type = (t == null) ? "null" : t.getDatatypeDescription();
            }
            else if (d instanceof CompoundDS) {
                if (isH4) {
                    type = "Vdata";
                }
                else {
                    type = "Compound";
                }
            }

            text = new Text(datasetInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(type);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            // Add a dummy label to take up some vertical space between sections
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setText("");
            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

            log.trace("showMetaData: Dataset object extra info - storage");
            // Display dataset storage layout, compression, filters,
            // storage type, and fill value
            org.eclipse.swt.widgets.Group datasetLayoutGroup = new org.eclipse.swt.widgets.Group(generalInfoGroup, SWT.NONE);
            datasetLayoutGroup.setFont(curFont);
            datasetLayoutGroup.setText("Miscellaneous Dataset Information");
            datasetLayoutGroup.setLayout(new GridLayout(2, false));
            datasetLayoutGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

            // Add compression and data layout information
            label = new Label(datasetLayoutGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Storage Layout: ");

            label = new Label(datasetLayoutGroup, SWT.RIGHT);
            label.setFont(curFont);
            labelinfo = d.getStorageLayout();
            if (labelinfo == null)
                labelinfo = "UNKNOWN";
            label.setText(labelinfo);

            label = new Label(datasetLayoutGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Compression: ");

            label = new Label(datasetLayoutGroup, SWT.RIGHT);
            label.setFont(curFont);
            labelinfo = d.getCompression();
            if (labelinfo == null)
                labelinfo = "UNKNOWN";
            label.setText(labelinfo);

            log.trace("showMetaData: Dataset object extra info - filters");
            label = new Label(datasetLayoutGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Filters: ");

            label = new Label(datasetLayoutGroup, SWT.RIGHT);
            label.setFont(curFont);
            labelinfo = d.getFilters();
            if (labelinfo == null)
                labelinfo = "UNKNOWN";
            label.setText(labelinfo);

            label = new Label(datasetLayoutGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Storage: ");

            label = new Label(datasetLayoutGroup, SWT.RIGHT);
            label.setFont(curFont);
            labelinfo = d.getStorage();
            if (labelinfo == null)
                labelinfo = "UNKNOWN";
            label.setText(labelinfo);

            log.trace("showMetaData: Dataset object extra info fill value");
            label = new Label(datasetLayoutGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Fill value: ");

            Object fillValue = null;
            String fillValueInfo = "NONE";
            if (d instanceof ScalarDS) fillValue = ((ScalarDS) d).getFillValue();
            if (fillValue != null) {
                if (fillValue.getClass().isArray()) {
                    int len = Array.getLength(fillValue);
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

            // button to open Data Option Dialog
            Button showDataOptionButton = new Button(datasetInfoGroup, SWT.PUSH);
            showDataOptionButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 2, 1));
            showDataOptionButton.setText("Show Data with Options");
            showDataOptionButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        viewManager.getTreeView().setDefaultDisplayMode(false);
                        viewManager.getTreeView().showDataContent((HObject) dataObject);
                    }
                    catch (Exception ex) {
                        display.beep();
                        Tools.showError(display.getShells()[0], ex.getMessage(), display.getShells()[0].getText());
                    }
                }
            });

            // Create composite for possible compound dataset info
            if (d instanceof CompoundDS) {
                log.trace("showMetaData: dataset Compound object extra info");
                CompoundDS compound = (CompoundDS) d;

                int n = compound.getMemberCount();
                if (n > 0) {
                    log.trace("showMetaData: dataset Compound object extra info members={}",n);
                    String rowData[][] = new String[n][3];
                    String names[] = compound.getMemberNames();
                    Datatype types[] = compound.getMemberTypes();
                    int orders[] = compound.getMemberOrders();

                    // Add a dummy label to take up some vertical space between sections
                    label = new Label(generalInfoGroup, SWT.LEFT);
                    label.setText("");
                    label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

                    org.eclipse.swt.widgets.Group compoundMembersGroup = new org.eclipse.swt.widgets.Group(generalInfoGroup, SWT.NONE);
                    compoundMembersGroup.setFont(curFont);
                    compoundMembersGroup.setText("Compound Dataset Members");
                    compoundMembersGroup.setLayout(new FillLayout(SWT.VERTICAL));
                    compoundMembersGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

                    for (int i = 0; i < n; i++) {
                        rowData[i][0] = new String(names[i]);

                        if (rowData[i][0].contains(CompoundDS.separator)) {
                            rowData[i][0] = rowData[i][0].replaceAll(CompoundDS.separator, "->");
                        }

                        int mDims[] = compound.getMemberDims(i);
                        if (mDims == null) {
                            rowData[i][2] = String.valueOf(orders[i]);

                            if (isH4 && types[i].getDatatypeClass() == Datatype.CLASS_STRING) {
                                rowData[i][2] = String.valueOf(types[i].getDatatypeSize());
                            }
                        }
                        else {
                            String mStr = String.valueOf(mDims[0]);
                            int m = mDims.length;
                            for (int j = 1; j < m; j++) {
                                mStr += " x " + mDims[j];
                            }
                            rowData[i][2] = mStr;
                        }
                        rowData[i][1] = (types[i] == null) ? "null" : types[i].getDatatypeDescription();
                    }

                    String[] columnNames = { "Name", "Type", "Array Size" };

                    Table memberTable = new Table(compoundMembersGroup, SWT.BORDER);
                    memberTable.setLinesVisible(true);
                    memberTable.setHeaderVisible(true);
                    memberTable.setFont(curFont);

                    for(int i = 0; i < columnNames.length; i++) {
                        TableColumn column = new TableColumn(memberTable, SWT.NONE);
                        column.setText(columnNames[i]);
                        column.setMoveable(false);
                    }

                    for(int i = 0; i < rowData.length; i++) {
                        TableItem item = new TableItem(memberTable, SWT.NONE);
                        item.setFont(curFont);
                        item.setText(0, rowData[i][0]);
                        item.setText(1, rowData[i][1]);
                        item.setText(2, rowData[i][2]);
                    }

                    for(int i = 0; i < columnNames.length; i++) {
                        memberTable.getColumn(i).pack();
                    }

                    // set cell height for large fonts
                    //int cellRowHeight = Math.max(16, table.getFontMetrics(table.getFont()).getHeight());
                    //table.setRowHeight(cellRowHeight);

                    // Prevent conflict from equal vertical grabbing
                    datasetLayoutGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
                } // if (n > 0)
            } // if (d instanceof Compound)
        }
        else if (dataObject instanceof Datatype) {
            log.trace("showMetaData: Datatype object extra info");
            org.eclipse.swt.widgets.Group datatypeInfoGroup = new org.eclipse.swt.widgets.Group(generalInfoGroup, SWT.NONE);
            datatypeInfoGroup.setFont(curFont);
            datatypeInfoGroup.setText("Type");
            datatypeInfoGroup.setLayout(new FillLayout());
            datatypeInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            Text infoArea = new Text(datatypeInfoGroup, SWT.MULTI);
            infoArea.setFont(curFont);
            infoArea.setText(((Datatype) dataObject).getDatatypeDescription());
            infoArea.setEditable(false);
        }

        log.trace("createGeneralObjectInfoPane: finish");

        return generalInfoGroup;
    }

    /* Listener to allow user to only change attribute name or value in attribute table */
    private Listener attrTableCellEditor = new Listener() {
        @Override
        public void handleEvent(Event event) {
            final TableEditor editor = new TableEditor(attrTable);
            editor.horizontalAlignment = SWT.LEFT;
            editor.grabHorizontal = true;

            Rectangle clientArea = attrTable.getClientArea();
            Point pt = new Point(event.x, event.y);

            int index = attrTable.getTopIndex();

            while (index < attrTable.getItemCount()) {
                boolean visible = false;
                final TableItem item = attrTable.getItem(index);

                for (int i = 0; i < attrTable.getColumnCount(); i++) {
                    Rectangle rect = item.getBounds(i);

                    if (rect.contains(pt)) {
                        if (!(i == 3 || (isH5 && (i == 0)))) {
                            // Only attribute value and name can be changed
                            return;
                        }

                        final int column = i;
                        final int row = index;

                        final Text text = new Text(attrTable, SWT.NONE);
                        text.setFont(curFont);

                        /* XXX: TODO try {
                            viewManager.getTreeView().showMetaData(obj);
                        }
                        catch (Exception ex) {
                            display.beep();
                            Tools.showError(display.getShells()[0], ex.getMessage(), display.getShells()[0].getText());
                        } */

                        Listener textListener = new Listener() {
                            @Override
                            public void handleEvent(final Event e) {
                                switch (e.type) {
                                    case SWT.FocusOut:
                                        item.setText(column, text.getText());
                                        updateAttributeValue(text.getText(), row, column);
                                        text.dispose();
                                        break;
                                    case SWT.Traverse:
                                        switch (e.detail) {
                                            case SWT.TRAVERSE_RETURN:
                                                item.setText(column, text.getText());
                                                updateAttributeValue(text.getText(), row, column);
                                            case SWT.TRAVERSE_ESCAPE:
                                                text.dispose();
                                                e.doit = false;
                                        }

                                        break;
                                }
                            }
                        };

                        text.addListener(SWT.FocusOut, textListener);
                        text.addListener(SWT.Traverse, textListener);
                        editor.setEditor(text, item, i);
                        text.setText(item.getText(i));
                        text.selectAll();
                        text.setFocus();
                        return;

                    }

                    if (!visible && rect.intersects(clientArea)) {
                        visible = true;
                    }
                }

                if (!visible)
                    return;

                index++;
            }
        }
    };

    /**
     * update attribute value. Currently can only update single data point.
     *
     * @param newValue
     *            the string of the new value.
     * @param row
     *            the row number of the selected cell.
     * @param col
     *            the column number of the selected cell.
     */
    private void updateAttributeValue(String newValue, int row, int col) {
        log.trace("updateAttributeValue:start value={}[{},{}]", newValue, row, col);

        String attrName = attrTable.getItem(row).getText(0);
        List<?> attrList = null;
        try {
            attrList = ((MetaDataContainer) dataObject).getMetadata();
        }
        catch (Exception ex) {
            Tools.showError(display.getShells()[0], ex.getMessage(), display.getShells()[0].getText());
            return;
        }

        Attribute attr = (Attribute) attrList.get(row);

        if (col == 1) { // To change attribute value
            Object data;

            log.trace("updateAttributeValue: change attribute value");

            try {
                data = attr.getData();
            } catch (Exception ex) {
                log.trace("updateAttributeValue(): getData() failure: {}", ex);
                return;
            }

            if (data == null) {
                return;
            }

            int array_length = Array.getLength(data);
            StringTokenizer st = new StringTokenizer(newValue, ",");
            if (st.countTokens() < array_length) {
                Tools.showError(display.getShells()[0], "More data values needed: " + newValue, display.getShells()[0].getText());
                return;
            }

            char NT = ' ';
            String cName = data.getClass().getName();
            int cIndex = cName.lastIndexOf("[");
            if (cIndex >= 0) {
                NT = cName.charAt(cIndex + 1);
            }
            boolean isUnsigned = attr.isUnsigned();
            log.trace("updateAttributeValue:start array_length={} cName={} NT={} isUnsigned={}", array_length, cName, NT, isUnsigned);

            double d = 0;
            String theToken = null;
            long max = 0, min = 0;
            for (int i = 0; i < array_length; i++) {
                max = min = 0;
                theToken = st.nextToken().trim();
                try {
                    if (!(Array.get(data, i) instanceof String)) {
                        d = Double.parseDouble(theToken);
                    }
                }
                catch (NumberFormatException ex) {
                    Tools.showError(display.getShells()[0], ex.getMessage(), display.getShells()[0].getText());
                    return;
                }

                if (isUnsigned && (d < 0)) {
                    Tools.showError(display.getShells()[0], "Negative value for unsigned integer: " + theToken, display.getShells()[0].getText());
                    return;
                }

                switch (NT) {
                    case 'B': {
                        if (isUnsigned) {
                            min = 0;
                            max = 255;
                        }
                        else {
                            min = Byte.MIN_VALUE;
                            max = Byte.MAX_VALUE;
                        }

                        if ((d > max) || (d < min)) {
                            Tools.showError(display.getShells()[0], "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, display.getShells()[0].getText());
                        }
                        else {
                            Array.setByte(data, i, (byte) d);
                        }
                        break;
                    }
                    case 'S': {
                        if (isUnsigned) {
                            min = 0;
                            max = 65535;
                        }
                        else {
                            min = Short.MIN_VALUE;
                            max = Short.MAX_VALUE;
                        }

                        if ((d > max) || (d < min)) {
                            Tools.showError(display.getShells()[0], "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, display.getShells()[0].getText());
                        }
                        else {
                            Array.setShort(data, i, (short) d);
                        }
                        break;
                    }
                    case 'I': {
                        if (isUnsigned) {
                            min = 0;
                            max = 4294967295L;
                        }
                        else {
                            min = Integer.MIN_VALUE;
                            max = Integer.MAX_VALUE;
                        }

                        if ((d > max) || (d < min)) {
                            Tools.showError(display.getShells()[0], "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, display.getShells()[0].getText());
                        }
                        else {
                            Array.setInt(data, i, (int) d);
                        }
                        break;
                    }
                    case 'J':
                        long lvalue = 0;
                        if (isUnsigned) {
                            if (theToken != null) {
                                String theValue = theToken;
                                BigInteger Jmax = new BigInteger("18446744073709551615");
                                BigInteger big = new BigInteger(theValue);
                                if ((big.compareTo(Jmax)>0) || (big.compareTo(BigInteger.ZERO)<0)) {
                                    Tools.showError(display.getShells()[0], "Data is out of range[" + min + ", " + max
                                            + "]: " + theToken, display.getShells()[0].getText());
                                }
                                lvalue = big.longValue();
                                log.trace("updateAttributeValue: big.longValue={}", lvalue);
                                Array.setLong(data, i, lvalue);
                            }
                            else
                                Array.set(data, i, theToken);
                        }
                        else {
                            min = Long.MIN_VALUE;
                            max = Long.MAX_VALUE;
                            if ((d > max) || (d < min)) {
                                Tools.showError(display.getShells()[0], "Data is out of range[" + min + ", " + max
                                        + "]: " + theToken, display.getShells()[0].getText());
                            }
                            lvalue = (long)d;
                            log.trace("updateAttributeValue: longValue={}", lvalue);
                            Array.setLong(data, i, lvalue);
                        }
                        break;
                    case 'F':
                        Array.setFloat(data, i, (float) d);
                        break;
                    case 'D':
                        Array.setDouble(data, i, d);
                        break;
                    default:
                        Array.set(data, i, theToken);
                        break;
                }
            }

            try {
                ((HObject) dataObject).getFileFormat().writeAttribute((HObject) dataObject, attr, true);
            }
            catch (Exception ex) {
                Tools.showError(display.getShells()[0], ex.getMessage(), display.getShells()[0].getText());
                return;
            }

            // update the attribute table
            attrTable.getItem(row).setText(1, attr.toString(", "));
        }

        if ((col == 0) && isH5) { // To change attribute name
            log.trace("updateAttributeValue: change attribute name");
            try {
                ((HObject) dataObject).getFileFormat().renameAttribute((HObject) dataObject, attrName, newValue);
            }
            catch (Exception ex) {
                Tools.showError(display.getShells()[0], ex.getMessage(), display.getShells()[0].getText());
                return;
            }

            // update the attribute table
            attrTable.getItem(row).setText(0, newValue);
        }
        if (dataObject instanceof ScalarDS) {
            ScalarDS ds = (ScalarDS) dataObject;
            try {
                log.trace("updateAttributeValue: ScalarDS:updateMetadata");
                ds.updateMetadata(attr);
            }
            catch (Exception ex) {
                Tools.showError(display.getShells()[0], ex.getMessage(), display.getShells()[0].getText());
            }
        }
        else {
            log.trace("updateAttributeValue: hObject is not instanceof ScalarDS");
        }
        log.trace("updateAttributeValue:exit");
    }

    private class UserBlockDialog extends Dialog {
        private Shell          shell;

        private final HObject  obj;

        private byte[]         userBlock;

        private final String[] displayChoices = { "Text", "Binary", "Octal", "Hexadecimal", "Decimal" };

        private Button         jamButton;
        private Text           userBlockArea;

        public UserBlockDialog(Shell parent, int style, HObject obj) {
            super(parent, style);

            this.obj = obj;

            userBlock = Tools.getHDF5UserBlock(obj.getFile());
        }

        public void open() {
            Shell parent = getParent();
            shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE);
            shell.setFont(curFont);
            shell.setText("User Block - " + obj);
            shell.setLayout(new GridLayout(5, false));


            Label label = new Label(shell, SWT.RIGHT);
            label.setFont(curFont);
            label.setText("Display As: ");

            Combo userBlockDisplayChoice = new Combo(shell, SWT.SINGLE | SWT.READ_ONLY);
            userBlockDisplayChoice.setFont(curFont);
            userBlockDisplayChoice.setItems(displayChoices);
            userBlockDisplayChoice.select(0);
            userBlockDisplayChoice.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Combo source = (Combo) e.widget;
                    int type = 0;

                    String typeName = source.getItem(source.getSelectionIndex());

                    jamButton.setEnabled(false);
                    userBlockArea.setEditable(false);

                    if (typeName.equalsIgnoreCase("Text")) {
                        type = 0;
                        jamButton.setEnabled(true);
                        userBlockArea.setEditable(true);
                    }
                    else if (typeName.equalsIgnoreCase("Binary")) {
                        type = 2;
                    }
                    else if (typeName.equalsIgnoreCase("Octal")) {
                        type = 8;
                    }
                    else if (typeName.equalsIgnoreCase("Hexadecimal")) {
                        type = 16;
                    }
                    else if (typeName.equalsIgnoreCase("Decimal")) {
                        type = 10;
                    }

                    showUserBlockAs(type);
                }
            });

            Label dummyLabel = new Label(shell, SWT.RIGHT);
            dummyLabel.setFont(curFont);
            dummyLabel.setText("");
            dummyLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            Label sizeLabel = new Label(shell, SWT.RIGHT);
            sizeLabel.setFont(curFont);
            sizeLabel.setText("Header Size (Bytes): 0");

            jamButton = new Button(shell, SWT.PUSH);
            jamButton.setFont(curFont);
            jamButton.setText("Save User Block");
            jamButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
            jamButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    writeUserBlock();
                }
            });

            ScrolledComposite userBlockScroller = new ScrolledComposite(shell, SWT.V_SCROLL | SWT.BORDER);
            userBlockScroller.setExpandHorizontal(true);
            userBlockScroller.setExpandVertical(true);
            userBlockScroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

            userBlockArea = new Text(userBlockScroller, SWT.MULTI | SWT.WRAP);
            userBlockArea.setEditable(true);
            userBlockArea.setFont(curFont);
            userBlockScroller.setContent(userBlockArea);

            Button closeButton = new Button(shell, SWT.CENTER);
            closeButton.setFont(curFont);
            closeButton.setText(" &Close ");
            closeButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 5, 1));
            closeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    shell.dispose();
                }
            });

            if (userBlock != null) {
                int headSize = showUserBlockAs(0);
                sizeLabel.setText("Header Size (Bytes): " + headSize);
            }
            else {
                userBlockDisplayChoice.setEnabled(false);
            }


            shell.pack();

            Rectangle parentBounds = parent.getBounds();

            Point shellSize = new Point((int) (0.5 * parentBounds.width), (int) (0.5 * parentBounds.height));
            shell.setSize(shellSize);

            shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                    (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            shell.open();

            Display display = parent.getDisplay();
            while(!shell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
        }

        private int showUserBlockAs(int radix) {
            if (userBlock == null) return 0;

            int headerSize = 0;

            String userBlockInfo = null;
            if ((radix == 2) || (radix == 8) || (radix == 16) || (radix == 10)) {
                StringBuffer sb = new StringBuffer();
                for (headerSize = 0; headerSize < userBlock.length; headerSize++) {
                    int intValue = userBlock[headerSize];
                    if (intValue < 0) {
                        intValue += 256;
                    }
                    else if (intValue == 0) {
                        break; // null end
                    }
                    sb.append(Integer.toString(intValue, radix));
                    sb.append(" ");
                }
                userBlockInfo = sb.toString();
            }
            else {
                userBlockInfo = new String(userBlock).trim();
                if (userBlockInfo != null) {
                    headerSize = userBlockInfo.length();
                }
            }

            userBlockArea.setText(userBlockInfo);

            return headerSize;
        }

        private void writeUserBlock() {
            if (!obj.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                return;
            }

            int blkSize0 = 0;
            if (userBlock != null) {
                blkSize0 = userBlock.length;
                // The super block space is allocated by offset 0, 512, 1024, 2048, etc
                if (blkSize0 > 0) {
                    int offset = 512;
                    while (offset < blkSize0) {
                        offset *= 2;
                    }
                    blkSize0 = offset;
                }
            }

            int blkSize1 = 0;
            String userBlockStr = userBlockArea.getText();
            if (userBlockStr == null) {
                if (blkSize0 <= 0) {
                    return; // nothing to write
                }
                else {
                    userBlockStr = " "; // want to wipe out old userblock content
                }
            }
            byte buf[] = null;
            buf = userBlockStr.getBytes();

            blkSize1 = buf.length;
            if (blkSize1 <= blkSize0) {
                java.io.RandomAccessFile raf = null;
                try {
                    raf = new java.io.RandomAccessFile(obj.getFile(), "rw");
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Can't open output file: " + obj.getFile(), shell.getText());
                    return;
                }

                try {
                    raf.seek(0);
                    raf.write(buf, 0, buf.length);
                    raf.seek(buf.length);
                    if (blkSize0 > buf.length) {
                        byte[] padBuf = new byte[blkSize0 - buf.length];
                        raf.write(padBuf, 0, padBuf.length);
                    }
                }
                catch (Exception ex) {
                    log.debug("raf write:", ex);
                }
                try {
                    raf.close();
                }
                catch (Exception ex) {
                    log.debug("raf close:", ex);
                }

                MessageDialog.openInformation(shell, shell.getText(), "Saving user block is successful.");
            }
            else {
                // must rewrite the whole file
                MessageDialog confirm = new MessageDialog(shell, shell.getText(), null,
                        "The user block to write is " + blkSize1 + " (bytes),\n"
                                + "which is larger than the user block space in file " + blkSize0 + " (bytes).\n"
                                + "To expand the user block, the file must be rewriten.\n\n"
                                + "Do you want to replace the current file? Click "
                                + "\n\"Yes\" to replace the current file," + "\n\"No\" to save to a different file, "
                                + "\n\"Cancel\" to quit without saving the change.\n\n ",
                                MessageDialog.QUESTION_WITH_CANCEL, new String[] { "Yes", "No", "Cancel" }, 0);
                int op = confirm.open();

                if(op == 2) return;

                String fin = obj.getFile();

                String fout = fin + "~copy.h5";
                if (fin.endsWith(".h5")) {
                    fout = fin.substring(0, fin.length() - 3) + "~copy.h5";
                }
                else if (fin.endsWith(".hdf5")) {
                    fout = fin.substring(0, fin.length() - 5) + "~copy.h5";
                }

                File outFile = null;

                if (op == 1) {
                    FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
                    fChooser.setFileName(fout);

                    DefaultFileFilter filter = DefaultFileFilter.getFileFilterHDF5();
                    fChooser.setFilterExtensions(new String[] {"*.*", filter.getExtensions()});
                    fChooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
                    fChooser.setFilterIndex(1);

                    if(fChooser.open() == null) return;

                    File chosenFile = new File(fChooser.getFileName());

                    outFile = chosenFile;
                    fout = outFile.getAbsolutePath();
                }
                else {
                    outFile = new File(fout);
                }

                if (!outFile.exists()) {
                    try {
                        outFile.createNewFile();
                    }
                    catch (Exception ex) {
                        Tools.showError(shell, "Failed to write user block into file.", shell.getText());
                        return;
                    }
                }

                // close the file
                TreeView view = viewManager.getTreeView();

                try {
                    view.closeFile(view.getSelectedFile());
                }
                catch (Exception ex) {
                    log.debug("Error closing file {}", fin);
                }

                if (Tools.setHDF5UserBlock(fin, fout, buf)) {
                    if (op == 1) {
                        fin = fout; // open the new file
                    }
                    else {
                        File oldFile = new File(fin);
                        boolean status = oldFile.delete();
                        if (status) {
                            outFile.renameTo(oldFile);
                        }
                        else {
                            Tools.showError(shell, "Cannot replace the current file.\nPlease save to a different file.", shell.getText());
                            outFile.delete();
                        }
                    }
                }
                else {
                    Tools.showError(shell, "Failed to write user block into file.", shell.getText());
                    outFile.delete();
                }

                // reopen the file
                shell.dispose();

                try {
                    view.openFile(fin, ViewProperties.isReadOnly() ? FileFormat.READ : FileFormat.WRITE);
                }
                catch (Exception ex) {
                    log.debug("Error opening file {}", fin);
                }
            }
        }
    }
}