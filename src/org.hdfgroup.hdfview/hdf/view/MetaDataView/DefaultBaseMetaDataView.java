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

import java.io.File;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;
import hdf.object.ScalarDS;
import hdf.object.h5.H5ReferenceType;
import hdf.object.h5.H5ReferenceType.H5ReferenceData;

import hdf.view.DefaultFileFilter;
import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;
import hdf.view.TreeView.DefaultTreeView;
import hdf.view.TreeView.TreeView;
import hdf.view.dialog.InputDialog;
import hdf.view.dialog.NewStringAttributeDialog;
import hdf.view.dialog.NewDataObjectDialog;
import hdf.view.dialog.NewScalarAttributeDialog;
//import hdf.view.dialog.NewCompoundAttributeDialog;

/**
 * DefaultBaseMetaDataView is a default implementation of the MetaDataView which
 * is used to show data properties of an object. Data properties include
 * attributes and general object information such as the object type, data type
 * and data space.
 *
 * This base class is responsible for displaying an object's general information
 * and attributes, since these are not object-specific. Subclasses of this class
 * are responsible for displaying any extra object-specific content by
 * overriding the addObjectSpecificContent() method.
 *
 * @author Jordan T. Henderson
 * @version 1.0 4/20/2018
 */
public abstract class DefaultBaseMetaDataView implements MetaDataView {

    private static final Logger log = LoggerFactory.getLogger(DefaultBaseMetaDataView.class);

    /** The default display */
    protected final Display               display = Display.getDefault();

    /** The view manger reference */
    protected final DataViewManager       viewManager;

    private final Composite               parent;

    /** The metadata container */
    protected final TabFolder             contentTabFolder;

    /** The attribute metadata pane */
    protected final Composite             attributeInfoPane;

    /** The general metadata pane */
    protected final Composite             generalObjectInfoPane;

    /** The current font */
    protected Font                        curFont;

    /** The HDF data object */
    protected HObject                     dataObject;

    /* The table to hold the list of attributes attached to the HDF object */
    private Table                         attrTable;

    private Label                         attrNumberLabel;

    private List<?>                       attrList;

    private int                           numAttributes;

    /** The HDF data object is hdf5 type */
    protected boolean                     isH5;
    /** The HDF data object is hdf4 type */
    protected boolean                     isH4;
    /** The HDF data object is netcdf type */
    protected boolean                     isN3;

    private static final String[]         attrTableColNames = { "Name", "Type", "Array Size", "Value[50](...)" };

    private static final int              ATTR_TAB_INDEX = 0;
    private static final int              GENERAL_TAB_INDEX = 1;

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
    public DefaultBaseMetaDataView(Composite parentComposite, DataViewManager viewer, HObject theObj) {
        this.parent = parentComposite;
        this.viewManager = viewer;
        this.dataObject = theObj;

        numAttributes = 0;

        isH5 = dataObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        isH4 = dataObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        isN3 = dataObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3));

        try {
            curFont = new Font(display, ViewProperties.getFontType(), ViewProperties.getFontSize(), SWT.NORMAL);
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
            log.debug("Error retrieving metadata of object '" + dataObject.getName() + "':", ex);
        }
        for (int i = 0; i < numAttributes; i++) {
            Attribute attr = (Attribute) attrList.get(i);
            Datatype atype = attr.getAttributeDatatype();
            if (isH5 && atype.isRef()) {
                H5ReferenceType rtype = (H5ReferenceType)atype;
                try {
                    List<H5ReferenceData> refdata = (List)rtype.getData();
                    for (int r = 0; r < (int)rtype.getRefSize(); r++) {
                        H5ReferenceData rf = refdata.get(r);
                        log.trace("constructor: refdata {}", rf.ref_array);
                    }
                }
                catch (Exception ex) {
                    log.trace("Error retrieving H5ReferenceData of object ", ex);
                }
            }
        }

        log.trace("dataObject={} isN3={} isH4={} isH5={} numAttributes={}", dataObject, isN3, isH4, isH5, numAttributes);

        contentTabFolder = new TabFolder(parent, SWT.NONE);
        contentTabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                switch (contentTabFolder.getSelectionIndex()) {
                    case ATTR_TAB_INDEX:
                        parent.setData("MetaDataView.LastTabIndex", ATTR_TAB_INDEX);
                        break;
                    case GENERAL_TAB_INDEX:
                    default:
                        parent.setData("MetaDataView.LastTabIndex", GENERAL_TAB_INDEX);
                        break;
                }
            }
        });

        attributeInfoPane = createAttributeInfoPane(contentTabFolder, dataObject);
        if (attributeInfoPane != null) {
            TabItem attributeInfoItem = new TabItem(contentTabFolder, SWT.None, ATTR_TAB_INDEX);
            attributeInfoItem.setText("Object Attribute Info");
            attributeInfoItem.setControl(attributeInfoPane);
        }

        generalObjectInfoPane = createGeneralObjectInfoPane(contentTabFolder, dataObject);
        if (generalObjectInfoPane != null) {
            TabItem generalInfoItem = new TabItem(contentTabFolder, SWT.None, GENERAL_TAB_INDEX);
            generalInfoItem.setText("General Object Info");
            generalInfoItem.setControl(generalObjectInfoPane);
        }

        /* Add any extra information depending on the object type */
        try {
            addObjectSpecificContent();
        }
        catch (UnsupportedOperationException ex) {
        }

        if (parent instanceof ScrolledComposite)
            ((ScrolledComposite) parent).setContent(contentTabFolder);

        /*
         * If the MetaDataView.LastTabIndex key data exists in the parent
         * composite, retrieve its value to determine which remembered
         * tab to select.
         */
        Object lastTabObject = parent.getData("MetaDataView.LastTabIndex");
        if (lastTabObject != null) {
            contentTabFolder.setSelection((int) lastTabObject);
        }
    }

    /**
     * Additional metadata to display
     */
    protected abstract void addObjectSpecificContent();

    private Composite createAttributeInfoPane(Composite parent, final HObject dataObject) {
        if (parent == null || dataObject == null) return null;

        org.eclipse.swt.widgets.Group attributeInfoGroup = null;

        attributeInfoGroup = new org.eclipse.swt.widgets.Group(parent, SWT.NONE);
        attributeInfoGroup.setFont(curFont);
        attributeInfoGroup.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        attributeInfoGroup.setLayout(new GridLayout(3, false));
        attributeInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        if (isH5) {
            StringBuilder objCreationStr = new StringBuilder("Creation Order NOT Tracked");
            long ocplID = -1;
            long objid = -1;
            int creationOrder = 0;
            try {
                objid = dataObject.open();
                if (objid >= 0) {
                    if (dataObject instanceof Group) {
                        ocplID = H5.H5Gget_create_plist(objid);
                    }
                    else if (dataObject instanceof Dataset) {
                        ocplID = H5.H5Dget_create_plist(objid);
                    }
                    if (ocplID >= 0) {
                        creationOrder = H5.H5Pget_attr_creation_order(ocplID);
                        log.trace("createAttributeInfoPane(): creationOrder={}", creationOrder);
                        if ((creationOrder & HDF5Constants.H5P_CRT_ORDER_TRACKED) > 0) {
                            objCreationStr.setLength(0);
                            objCreationStr.append("Creation Order Tracked");
                            if ((creationOrder & HDF5Constants.H5P_CRT_ORDER_INDEXED) > 0)
                                objCreationStr.append(" and Indexed");
                        }
                    }
                }
            }
            finally {
                H5.H5Pclose(ocplID);
                dataObject.close(objid);
            }

            /* Creation order section */
            Label label;
            label = new Label(attributeInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Attribute Creation Order: ");
            label.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

            Text text;
            text = new Text(attributeInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(objCreationStr.toString());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        }

        log.trace("createAttributeInfoPane(): numAttributes={}", numAttributes);

        attrNumberLabel = new Label(attributeInfoGroup, SWT.RIGHT);
        attrNumberLabel.setFont(curFont);
        attrNumberLabel.setText("Number of attributes = 0");
        attrNumberLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

        Button addButton = new Button(attributeInfoGroup, SWT.PUSH);
        addButton.setFont(curFont);
        addButton.setText("Add Attribute");
        addButton.setEnabled(!(dataObject.getFileFormat().isReadOnly()));
        addButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addAttribute(dataObject);
            }
        });

        /* Deleting attributes is not supported by HDF4 */
        Button delButton = new Button(attributeInfoGroup, SWT.PUSH);
        delButton.setFont(curFont);
        delButton.setText("Delete Attribute");
        delButton.setEnabled(isH5 && !(dataObject.getFileFormat().isReadOnly()));
        delButton.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));
        delButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteAttribute(dataObject);
            }
        });

        attrTable = new Table(attributeInfoGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        attrTable.setLinesVisible(true);
        attrTable.setHeaderVisible(true);
        attrTable.setFont(curFont);
        attrTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        Menu attrPopupMenu = createAttributePopupMenu(attrTable);
        attrTable.setMenu(attrPopupMenu);

        /*
         * Add a double-click listener for editing attribute values in a separate
         * TableView
         */
        attrTable.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                int selectionIndex = attrTable.getSelectionIndex();
                if (selectionIndex < 0) {
                    Tools.showError(Display.getDefault().getShells()[0], "Select", "No Attribute selected");
                    return;
                }

                final TableItem item = attrTable.getItem(selectionIndex);

                viewManager.getTreeView().setDefaultDisplayMode(true);

                try {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                HObject selectedObject = (HObject) item.getData();
                                if ((selectedObject instanceof Dataset) && !((Dataset) selectedObject).isNULL()) {
                                    viewManager.getTreeView().showDataContent(selectedObject);
                                }
                                else {
                                    Tools.showInformation(Display.getDefault().getShells()[0], "Open",
                                            "No data to display in an object with a NULL dataspace.");
                                }
                            }
                            catch (Exception ex) {
                                log.debug("Attribute showDataContent failure: ", ex);
                            }
                        }
                    });
                }
                catch (Exception e) {
                    log.debug("Attribute showDataContent loading manually interrupted");
                }
            }
        });

        /*
         * Add a right-click listener for showing a menu that has options for renaming
         * an attribute, editing an attribute, or deleting an attribute
         */
        attrTable.addListener(SWT.MenuDetect, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                int index = attrTable.getSelectionIndex();
                if (index < 0) return;

                attrTable.getMenu().setVisible(true);
            }
        });

        for (int i = 0; i < attrTableColNames.length; i++) {
            TableColumn column = new TableColumn(attrTable, SWT.NONE);
            column.setText(attrTableColNames[i]);
            column.setMoveable(false);

            /*
             * Make sure all columns show even when the object in question has no attributes
             */
            if (i == attrTableColNames.length - 1)
                column.setWidth(200);
            else
                column.setWidth(50);
        }

        if (attrList != null) {
            attrNumberLabel.setText("Number of attributes = " + numAttributes);

            Attribute attr = null;
            for (int i = 0; i < numAttributes; i++) {
                attr = (Attribute) attrList.get(i);

                log.trace("createAttributeInfoPane(): attr[{}] is {} of type {}", i, attr.getAttributeName(),
                        attr.getAttributeDatatype().getDescription());

                addAttributeTableItem(attrTable, attr);
            }
        }

        for (int i = 0; i < attrTableColNames.length; i++) {
            attrTable.getColumn(i).pack();
        }

        // Prevent attributes with many values, such as array types, from making
        // the window too wide
        attrTable.getColumn(3).setWidth(200);

        return attributeInfoGroup;
    }

    private Composite createGeneralObjectInfoPane(Composite parent, final HObject dataObject) {
        if (parent == null || dataObject == null) return null;

        FileFormat theFile = dataObject.getFileFormat();
        boolean isRoot = ((dataObject instanceof Group) && ((Group) dataObject).isRoot());
        String objTypeStr = "Unknown";
        Label label;
        Text text;

        /* Add an SWT Group to encompass all of the GUI components */
        org.eclipse.swt.widgets.Group generalInfoGroup = new org.eclipse.swt.widgets.Group(parent, SWT.NONE);
        generalInfoGroup.setFont(curFont);
        generalInfoGroup.setLayout(new GridLayout(2, false));
        generalInfoGroup.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        /* Object name section */
        label = new Label(generalInfoGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Name: ");

        text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
        text.setEditable(false);
        text.setFont(curFont);
        text.setText(dataObject.getName());
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        /* Object Path section */
        label = new Label(generalInfoGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Path: ");

        text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
        text.setEditable(false);
        text.setFont(curFont);
        text.setText(dataObject.getPath() == null ? "/"
                : dataObject.getPath()); /* TODO: temporary workaround until Object Library is fixed */
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        /* Object Type section */
        label = new Label(generalInfoGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Type: ");

        if (isH5) {
            if (dataObject instanceof Group) {
                objTypeStr = "HDF5 Group";
            }
            else if (dataObject instanceof ScalarDS) {
                objTypeStr = "HDF5 Dataset";
            }
            else if (dataObject instanceof CompoundDS) {
                objTypeStr = "HDF5 Dataset";
            }
            else if (dataObject instanceof Datatype) {
                objTypeStr = "HDF5 Named Datatype";
            }
            else {
                log.debug("createGeneralObjectInfoPane(): unknown HDF5 dataObject");
            }
        }
        else if (isH4) {
            if (dataObject instanceof Group) {
                objTypeStr = "HDF4 Group";
            }
            else if (dataObject instanceof ScalarDS) {
                ScalarDS ds = (ScalarDS) dataObject;
                if (ds.isImage()) {
                    objTypeStr = "HDF4 Raster Image";
                }
                else {
                    objTypeStr = "HDF4 SDS";
                }
            }
            else if (dataObject instanceof CompoundDS) {
                objTypeStr = "HDF4 Vdata";
            }
            else {
                log.debug("createGeneralObjectInfoPane(): unknown HDF4 dataObject");
            }
        }
        else if (isN3) {
            if (dataObject instanceof Group) {
                objTypeStr = "netCDF3 Group";
            }
            else if (dataObject instanceof ScalarDS) {
                objTypeStr = "netCDF3 Dataset";
            }
            else {
                log.debug("createGeneralObjectInfoPane(): unknown netCDF3 dataObject");
            }
        }
        else {
            if (dataObject instanceof Group) {
                objTypeStr = "Group";
            }
            else if (dataObject instanceof ScalarDS) {
                objTypeStr = "Dataset";
            }
            else if (dataObject instanceof CompoundDS) {
                objTypeStr = "Dataset";
            }
            else {
                log.debug("createGeneralObjectInfoPane(): unknown dataObject");
            }
        }

        text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
        text.setEditable(false);
        text.setFont(curFont);
        text.setText(objTypeStr);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        /* Object ID section */

        // bug #926 to remove the OID, put it back on Nov. 20, 2008, --PC
        String oidStr = null;
        long[] oID = dataObject.getOID();
        if (oID != null) {
            oidStr = String.valueOf(oID[0]);
            if (isH4)
                oidStr += ", " + oID[1];

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

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(oidStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        }

        /*
         * If this is the root group, add some special extra info, such as the Library
         * Version bounds set for the file.
         */
        if (isRoot) {
            /* Get the file's size */
            long fileSize = 0;
            try {
                fileSize = (new File(dataObject.getFile())).length();
            }
            catch (Exception ex) {
                fileSize = -1;
            }
            fileSize /= 1024;

            /* Retrieve the number of subgroups and datasets in the root group */
            HObject root = theFile.getRootObject();
            HObject theObj = null;
            Iterator<HObject> it = ((Group) root).depthFirstMemberList().iterator();
            int groupCount = 0;
            int datasetCount = 0;

            while (it.hasNext()) {
                theObj = it.next();

                if (theObj instanceof Group)
                    groupCount++;
                else
                    datasetCount++;
            }

            /* Append all of the file's information to the general object info pane */
            String fileInfo = "";

            fileInfo = "size=" + fileSize + "K,  groups=" + groupCount + ",  datasets=" + datasetCount;

            /* File name section */
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("File Name: ");

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(dataObject.getFileFormat().getName());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            /* File Path section */
            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("File Path: ");

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText((new File(dataObject.getFile())).getParent());
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            label = new Label(generalInfoGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("File Type: ");

            if (isH5)
                objTypeStr = "HDF5,  " + fileInfo;
            else if (isH4)
                objTypeStr = "HDF4,  " + fileInfo;
            else if (isN3)
                objTypeStr = "netCDF3,  " + fileInfo;
            else
                objTypeStr = fileInfo;

            text = new Text(generalInfoGroup, SWT.SINGLE | SWT.BORDER);
            text.setEditable(false);
            text.setFont(curFont);
            text.setText(objTypeStr);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            if (isH5) {
                log.trace("createGeneralObjectInfoPane(): get Library Version bounds info");
                String libversion = "";
                try {
                    libversion = dataObject.getFileFormat().getLibBoundsDescription();
                }
                catch (Exception ex) {
                    log.debug("Get Library Bounds Description failure: ", ex);
                }

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
                        new UserBlockDialog(display.getShells()[0], SWT.NONE, dataObject).open();
                    }
                });
            }
        }

        /* Add a dummy label to take up some vertical space between sections */
        label = new Label(generalInfoGroup, SWT.LEFT);
        label.setText("");
        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        return generalInfoGroup;
    }

    @Override
    public HObject getDataObject() {
        return dataObject;
    }

    private Menu createAttributePopupMenu(final Table table) {
        final Menu menu = new Menu(table);
        MenuItem item;

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Rename Attribute");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = table.getSelectionIndex();
                if (selectionIndex < 0) {
                    Tools.showError(Display.getDefault().getShells()[0], "Select", "No Attribute selected");
                    return;
                }

                HObject itemObj = (HObject) table.getItem(selectionIndex).getData();
                String result = new InputDialog(Display.getDefault().getShells()[0],
                        Display.getDefault().getShells()[0].getText() + " - Rename Attribute", "New Attribute Name",
                        itemObj.getName()).open();

                if ((result == null) || ((result = result.trim()) == null) || (result.length() < 1)) {
                    return;
                }

                Attribute attr = (Attribute) attrTable.getItem(selectionIndex).getData();
                renameAttribute(attr, result);
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("View/Edit Attribute Value");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = attrTable.getSelectionIndex();
                if (selectionIndex < 0) {
                    Tools.showError(Display.getDefault().getShells()[0], "Select", "No Attribute selected");
                    return;
                }

                final TableItem item = attrTable.getItem(selectionIndex);

                viewManager.getTreeView().setDefaultDisplayMode(true);

                try {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                HObject selectedObject = (HObject) item.getData();
                                if ((selectedObject instanceof Dataset) && !((Dataset) selectedObject).isNULL()) {
                                    viewManager.getTreeView().showDataContent(selectedObject);
                                }
                                else {
                                    Tools.showInformation(Display.getDefault().getShells()[0], "Open",
                                            "No data to display in an object with a NULL dataspace.");
                                }
                            }
                            catch (Exception ex) {
                                log.debug("Attribute showDataContent failure: ", ex);
                            }
                        }
                    });
                }
                catch (Exception ex) {
                    log.debug("Attribute showDataContent loading manually interrupted");
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Delete Attribute");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = attrTable.getSelectionIndex();
                if (selectionIndex < 0) {
                    Tools.showError(Display.getDefault().getShells()[0], "Select", "No Attribute selected");
                    return;
                }

                deleteAttribute(dataObject);
            }
        });

        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(MenuEvent e) {
                /* 'Rename Attribute' MenuItem */
                menu.getItem(0).setEnabled(!dataObject.getFileFormat().isReadOnly() && !isH4);

                /* 'Delete Attribute' MenuItem */
                menu.getItem(2).setEnabled(!dataObject.getFileFormat().isReadOnly() && !isH4);
            }
        });

        return menu;
    }

    @Override
    public Attribute addAttribute(HObject obj) {
        if (obj == null) return null;

        HObject root = obj.getFileFormat().getRootObject();
        Attribute attr = null;
        if (isH5) {
            NewScalarAttributeDialog dialog = new NewScalarAttributeDialog(display.getShells()[0], obj,
                    ((Group) root).breadthFirstMemberList());
            dialog.open();
            attr = dialog.getAttribute();
        }
        else {
            NewStringAttributeDialog dialog = new NewStringAttributeDialog(display.getShells()[0], obj,
                    ((Group) root).breadthFirstMemberList());
            dialog.open();
            attr = dialog.getAttribute();
        }

        if (attr == null) {
            log.debug("addAttribute(): attr is null");
            return null;
        }

        addAttributeTableItem(attrTable, attr);

        numAttributes++;
        attrNumberLabel.setText("Number of attributes = " + numAttributes);

        if (viewManager.getTreeView() instanceof DefaultTreeView)
            ((DefaultTreeView) viewManager.getTreeView()).updateItemIcon(obj);

        return attr;
    }

    @Override
    public Attribute deleteAttribute(HObject obj) {
        if (obj == null) return null;

        int idx = attrTable.getSelectionIndex();
        if (idx < 0) {
            log.debug("deleteAttribute(): no attribute is selected");
            Tools.showError(display.getShells()[0], "Delete", "No attribute is selected.");
            return null;
        }

        int answer = SWT.NO;
        if (Tools.showConfirm(display.getShells()[0], "Delete",
                "Do you want to delete the selected attribute?"))
            answer = SWT.YES;
        if (answer == SWT.NO) {
            log.trace("deleteAttribute(): attribute deletion cancelled");
            return null;
        }

        if (attrList == null) {
            log.debug("deleteAttribute(): Attribute list was null; can't delete an attribute from it");
            return null;
        }

        Attribute attr = (Attribute) attrList.get(idx);

        log.trace("deleteAttribute(): Attribute selected for deletion: {}", attr.getAttributeName());

        try {
            ((MetaDataContainer) obj).removeMetadata(attr);
        }
        catch (Exception ex) {
            log.debug("deleteAttribute(): attribute deletion failed for object '{}': ", obj.getName(), ex);
        }

        attrTable.remove(idx);
        numAttributes--;

        attrNumberLabel.setText("Number of attributes = " + numAttributes);

        if (viewManager.getTreeView() instanceof DefaultTreeView)
            ((DefaultTreeView) viewManager.getTreeView()).updateItemIcon(obj);

        return attr;
    }

    private void renameAttribute(Attribute attr, String newName) {
        if ((attr == null) || (newName == null) || (newName = newName.trim()) == null || (newName.length() < 1)) {
            log.debug("renameAttribute(): Attribute is null or Attribute's new name is null");
            return;
        }

        String attrName = attr.getAttributeName();

        log.trace("renameAttribute(): oldName={} newName={}", attrName, newName);

        if (isH5) {
            try {
                dataObject.getFileFormat().renameAttribute(dataObject, attrName, newName);
            }
            catch (Exception ex) {
                log.debug("renameAttribute(): renaming failure:", ex);
                Tools.showError(display.getShells()[0], "Delete", ex.getMessage());
            }

            /* Update the attribute table */
            int selectionIndex = attrTable.getSelectionIndex();
            if (selectionIndex < 0) {
                Tools.showError(Display.getDefault().getShells()[0], "Delete", "No Attribute selected");
                return;
            }

            attrTable.getItem(selectionIndex).setText(0, newName);
        }
        else {
            log.debug("renameAttribute(): renaming attributes is only allowed for HDF5 files");
        }

        if (dataObject instanceof MetaDataContainer) {
            try {
                ((MetaDataContainer) dataObject).updateMetadata(attr);
            }
            catch (Exception ex) {
                log.debug("renameAttribute(): updateMetadata() failure:", ex);
                Tools.showError(display.getShells()[0], "Delete", ex.getMessage());
            }
        }
    }

    /**
     * Update an attribute's value. Currently can only update a single data point.
     *
     * @param attr
     *            the selected attribute.
     * @param newValue
     *            the string of the new value.
     */
    private void updateAttributeValue(Attribute attr, String newValue) {
        if ((attr == null) || (newValue == null) || (newValue = newValue.trim()) == null || (newValue.length() < 1)) {
            log.debug("updateAttributeValue(): Attribute is null or Attribute's new value is null");
            return;
        }

        String attrName = attr.getAttributeName();
        Object data;

        log.trace("updateAttributeValue(): changing value of attribute '{}'", attrName);

        try {
            data = attr.getAttributeData();
        }
        catch (Exception ex) {
            log.debug("updateAttributeValue(): getData() failure:", ex);
            return;
        }

        if (data == null) {
            log.debug("updateAttributeValue(): attribute's data was null");
            return;
        }

        int arrayLength = Array.getLength(data);
        StringTokenizer st = new StringTokenizer(newValue, ",");
        if (st.countTokens() < arrayLength) {
            log.debug("updateAttributeValue(): More data values needed: {}", newValue);
            Tools.showError(display.getShells()[0], "Update", "More data values needed: " + newValue);
            return;
        }

        char cNT = ' ';
        String cName = data.getClass().getName();
        int cIndex = cName.lastIndexOf('[');
        if (cIndex >= 0) {
            cNT = cName.charAt(cIndex + 1);
        }
        boolean isUnsigned = attr.getAttributeDatatype().isUnsigned();

        log.trace("updateAttributeValue(): array_length={} cName={} NT={} isUnsigned={}", arrayLength, cName,
                cNT, isUnsigned);

        double d = 0;
        String theToken = null;
        long max = 0;
        long min = 0;
        for (int i = 0; i < arrayLength; i++) {
            max = min = 0;
            theToken = st.nextToken().trim();
            try {
                if (!(Array.get(data, i) instanceof String)) {
                    d = Double.parseDouble(theToken);
                }
            }
            catch (NumberFormatException ex) {
                log.debug("updateAttributeValue(): NumberFormatException: ", ex);
                Tools.showError(display.getShells()[0], "Update", ex.getMessage());
                return;
            }

            if (isUnsigned && (d < 0)) {
                log.debug("updateAttributeValue(): Negative value for unsigned integer: {}", theToken);
                Tools.showError(display.getShells()[0], "Update", "Negative value for unsigned integer: " + theToken);
                return;
            }

            switch (cNT) {
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
                        Tools.showError(display.getShells()[0], "Update",
                                "Data is out of range[" + min + ", " + max + "]: " + theToken);
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
                        Tools.showError(display.getShells()[0], "Update",
                                "Data is out of range[" + min + ", " + max + "]: " + theToken);
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
                        Tools.showError(display.getShells()[0], "Update",
                                "Data is out of range[" + min + ", " + max + "]: " + theToken);
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
                            BigInteger maxJ = new BigInteger("18446744073709551615");
                            BigInteger big = new BigInteger(theValue);
                            if ((big.compareTo(maxJ) > 0) || (big.compareTo(BigInteger.ZERO) < 0)) {
                                Tools.showError(display.getShells()[0], "Update",
                                        "Data is out of range[" + min + ", " + max + "]: " + theToken);
                            }
                            lvalue = big.longValue();
                            log.trace("updateAttributeValue(): big.longValue={}", lvalue);
                            Array.setLong(data, i, lvalue);
                        }
                        else
                            Array.set(data, i, theToken);
                    }
                    else {
                        min = Long.MIN_VALUE;
                        max = Long.MAX_VALUE;
                        if ((d > max) || (d < min)) {
                            Tools.showError(display.getShells()[0], "Update",
                                    "Data is out of range[" + min + ", " + max + "]: " + theToken);
                        }
                        lvalue = (long) d;
                        log.trace("updateAttributeValue(): longValue={}", lvalue);
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
            dataObject.getFileFormat().writeAttribute(dataObject, attr, true);
        }
        catch (Exception ex) {
            log.debug("updateAttributeValue(): writeAttribute failure: ", ex);
            Tools.showError(display.getShells()[0], "Update", ex.getMessage());
            return;
        }

        /* Update the attribute table */
        int selectionIndex = attrTable.getSelectionIndex();
        if (selectionIndex < 0) {
            Tools.showError(Display.getDefault().getShells()[0], "Update", "No Attribute selected");
            return;
        }

        attrTable.getItem(selectionIndex).setText(3, attr.toAttributeString(", "));

        if (dataObject instanceof MetaDataContainer) {
            try {
                ((MetaDataContainer) dataObject).updateMetadata(attr);
            }
            catch (Exception ex) {
                log.debug("updateAttributeValue(): updateMetadata() failure:", ex);
                Tools.showError(display.getShells()[0], "Update", ex.getMessage());
            }
        }
    }

    private void addAttributeTableItem(Table table, Attribute attr) {
        if (table == null || attr == null) {
            log.debug("addAttributeTableItem(): table or attribute is null");
            return;
        }

        String attrName = attr.getAttributeName();
        String attrType = attr.getAttributeDatatype().getDescription();
        StringBuilder attrSize = new StringBuilder();
        String attrValue = attr.toAttributeString(", ", 50);
        String[] rowData = new String[attrTableColNames.length];

        if (attrName == null) attrName = "null";
        if (attrType == null) attrType = "null";
        if (attrValue == null) attrValue = "null";

        TableItem item = new TableItem(attrTable, SWT.NONE);
        item.setFont(curFont);
        item.setData(attr);

        if (attr.getProperty("field") != null) {
            rowData[0] = attrName + " {Field: " + attr.getProperty("field") + "}";
        }
        else {
            rowData[0] = attrName;
        }

        if (attr.isAttributeNULL()) {
            attrSize.append("NULL");
        }
        else if (attr.isAttributeScalar()) {
            attrSize.append("Scalar");
        }
        else {
            long[] dims = attr.getAttributeDims();
            attrSize.append(String.valueOf(dims[0]));
            for (int j = 1; j < dims.length; j++) {
                attrSize.append(" x ").append(dims[j]);
            }
        }

        rowData[1] = attrType;
        rowData[2] = attrSize.toString();
        if (attr.isAttributeNULL())
            rowData[3] = "NULL";
        else
            rowData[3] = attrValue;

        item.setText(rowData);
    }

    /**
     * Updates the current font.
     *
     * @param font the new font
     */
    public void updateFont(Font font) {
        if (curFont != null)
            curFont.dispose();

        log.trace("updateFont():");
        curFont = font;

        attributeInfoPane.setFont(font);
        attributeInfoPane.pack();
        attributeInfoPane.requestLayout();

        generalObjectInfoPane.setFont(font);
        generalObjectInfoPane.pack();
        generalObjectInfoPane.requestLayout();
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
            Shell openParent = getParent();
            shell = new Shell(openParent, SWT.DIALOG_TRIM | SWT.RESIZE);
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

            Rectangle parentBounds = openParent.getBounds();

            Point shellSize = new Point((int) (0.5 * parentBounds.width), (int) (0.5 * parentBounds.height));
            shell.setSize(shellSize);

            shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                    (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            shell.open();

            Display openDisplay = openParent.getDisplay();
            while (!shell.isDisposed()) {
                if (!openDisplay.readAndDispatch()) openDisplay.sleep();
            }
        }

        private int showUserBlockAs(int radix) {
            if (userBlock == null) return 0;

            int headerSize = 0;

            String userBlockInfo = null;
            if ((radix == 2) || (radix == 8) || (radix == 16) || (radix == 10)) {
                StringBuilder sb = new StringBuilder();
                for (headerSize = 0; headerSize < userBlock.length; headerSize++) {
                    int intValue = userBlock[headerSize];
                    if (intValue < 0) {
                        intValue += 256;
                    }
                    else if (intValue == 0) {
                        break; // null end
                    }

                    sb.append(Integer.toString(intValue, radix)).append(" ");
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
            byte[] buf = null;
            buf = userBlockStr.getBytes();

            blkSize1 = buf.length;
            if (blkSize1 <= blkSize0) {
                java.io.RandomAccessFile raf = null;
                try {
                    raf = new java.io.RandomAccessFile(obj.getFile(), "rw");
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Save", "Can't open output file: " + obj.getFile());
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

                Tools.showInformation(shell, "Save", "Saving user block is successful.");
            }
            else {
                // must rewrite the whole file
                MessageDialog confirm = new MessageDialog(shell, "Save", null,
                        "The user block to write is " + blkSize1 + " (bytes),\n"
                                + "which is larger than the user block space in file " + blkSize0 + " (bytes).\n"
                                + "To expand the user block, the file must be rewritten.\n\n"
                                + "Do you want to replace the current file? Click "
                                + "\n\"Yes\" to replace the current file," + "\n\"No\" to save to a different file, "
                                + "\n\"Cancel\" to quit without saving the change.\n\n ",
                                MessageDialog.QUESTION_WITH_CANCEL, new String[] { "Yes", "No", "Cancel" }, 0);
                int op = confirm.open();

                if (op == 2) return;

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
                    fChooser.setFilterExtensions(new String[] { "*", filter.getExtensions() });
                    fChooser.setFilterNames(new String[] { "All Files", filter.getDescription() });
                    fChooser.setFilterIndex(1);

                    if (fChooser.open() == null) return;

                    File chosenFile = new File(fChooser.getFileName());

                    outFile = chosenFile;
                    fout = outFile.getAbsolutePath();
                }
                else {
                    outFile = new File(fout);
                }

                if (!outFile.exists()) {
                    try {
                        if (!outFile.createNewFile())
                            log.debug("Error creating file {}", fout);
                    }
                    catch (Exception ex) {
                        Tools.showError(shell, "Save", "Failed to write user block into file.");
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
                            if (!outFile.renameTo(oldFile))
                                log.debug("Error renaming file {}", fout);
                        }
                        else {
                            Tools.showError(shell, "Save", "Cannot replace the current file.\nPlease save to a different file.");
                            outFile.delete();
                        }
                    }
                }
                else {
                    Tools.showError(shell, "Save", "Failed to write user block into file.");
                    outFile.delete();
                }

                // reopen the file
                shell.dispose();

                try {
                    int access_mode = FileFormat.WRITE;
                    if (ViewProperties.isReadOnly())
                        access_mode = FileFormat.READ;
                    else if (ViewProperties.isReadSWMR())
                        access_mode = FileFormat.READ | FileFormat.MULTIREAD;
                    view.openFile(fin, access_mode);
                }
                catch (Exception ex) {
                    log.debug("Error opening file {}", fin);
                }
            }
        }
    }
}
