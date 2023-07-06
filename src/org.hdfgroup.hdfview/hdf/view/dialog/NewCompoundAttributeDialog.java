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

package hdf.view.dialog;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5CompoundAttr;
import hdf.object.h5.H5Datatype;

import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * NewCompoundAttributeDialog shows a message dialog requesting user input for creating
 * a new HDF5 compound attribute.
 *
 * @author Allen Byrne
 * @version 1.0 7/20/2021
 */
public class NewCompoundAttributeDialog extends NewDataObjectDialog {

    private static final Logger log = LoggerFactory.getLogger(NewCompoundAttributeDialog.class);

    private static final String[] DATATYPE_NAMES   = {
        "byte (8-bit)", // 0
        "short (16-bit)", // 1
        "int (32-bit)", // 2
        "unsigned byte (8-bit)", // 3
        "unsigned short (16-bit)", // 4
        "unsigned int (32-bit)", // 5
        "long (64-bit)", // 6
        "float", // 7
        "double", // 8
        "string", // 9
        "enum", // 10
        "unsigned long (64-bit)" // 11
    };

    private Combo                 nFieldBox, templateChoice;

    private Vector<H5CompoundAttr>  compoundAttrList;

    private int                   numberOfMembers;

    private Table                 table;

    private TableEditor[][]       editors;

    private Text                  nameField, currentSizeField;

    private Combo                 rankChoice;

    /**
     * Constructs a NewCompoundAttributeDialog with specified list of possible parent
     * objects.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pObject
     *            the parent object which the new attribute is attached to.
     * @param objs
     *            the list of all objects.
     */
    public NewCompoundAttributeDialog(Shell parent, HObject pObject, List<HObject> objs) {
        super(parent, pObject, objs);

        numberOfMembers = 2;

        compoundAttrList = new Vector<>(objs.size());
    }

    /**
     * Open the NewCompoundAttributeDialog for adding a new compound attribute.
     */
    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Compound Attribute...");
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setLayout(new GridLayout(1, false));


        // Create Name/Parent Object/Import field region
        Composite fieldComposite = new Composite(shell, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.verticalSpacing = 0;
        fieldComposite.setLayout(layout);
        fieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label attributeNameLabel = new Label(fieldComposite, SWT.LEFT);
        attributeNameLabel.setFont(curFont);
        attributeNameLabel.setText("Attribute name: ");

        nameField = new Text(fieldComposite, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // Create Dataspace region
        org.eclipse.swt.widgets.Group dataspaceGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        dataspaceGroup.setFont(curFont);
        dataspaceGroup.setText("Dataspace");
        dataspaceGroup.setLayout(new GridLayout(3, true));
        dataspaceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = new Label(dataspaceGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("No. of dimensions");

        label = new Label(dataspaceGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Current size");

        // Dummy label
        label = new Label(dataspaceGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("");

        rankChoice = new Combo(dataspaceGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        rankChoice.setFont(curFont);
        rankChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        rankChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int rank = rankChoice.getSelectionIndex() + 1;
                StringBuilder currentSizeStr = new StringBuilder("1");

                for (int i = 1; i < rank; i++) {
                    currentSizeStr.append(" x 1");
                }

                currentSizeField.setText(currentSizeStr.toString());

                String currentStr = currentSizeField.getText();
                int idx = currentStr.lastIndexOf('x');
            }
        });

        for (int i = 1; i < 33; i++) {
            rankChoice.add(String.valueOf(i));
        }
        rankChoice.select(1);

        currentSizeField = new Text(dataspaceGroup, SWT.SINGLE | SWT.BORDER);
        currentSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        currentSizeField.setFont(curFont);
        currentSizeField.setText("1 x 1");

        // Create Properties region
        org.eclipse.swt.widgets.Group propertiesGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        propertiesGroup.setLayout(new GridLayout(2, false));
        propertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        propertiesGroup.setFont(curFont);
        propertiesGroup.setText("Compound Datatype Properties");

        label = new Label(propertiesGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Number of Members:");

        nFieldBox = new Combo(propertiesGroup, SWT.DROP_DOWN);
        nFieldBox.setFont(curFont);
        nFieldBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        nFieldBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateMemberTableItems();
            }
        });
        nFieldBox.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) updateMemberTableItems();
            }
        });

        for (int i = 1; i <= 100; i++) {
            nFieldBox.add(String.valueOf(i));
        }

        table = new Table(propertiesGroup, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        table.setLinesVisible(false);
        table.setHeaderVisible(true);
        table.setFont(curFont);

        editors = new TableEditor[nFieldBox.getItemCount()][3];

        String[] colNames = { "Name", "Datatype", "Array size / String length / Enum names" };

        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(colNames[0]);

        column = new TableColumn(table, SWT.NONE);
        column.setText(colNames[1]);

        column = new TableColumn(table, SWT.NONE);
        column.setText(colNames[2]);

        for (int i = 0; i < 2; i++) {
            TableEditor[] editor = addMemberTableItem(table);
            editors[i][0] = editor[0];
            editors[i][1] = editor[1];
            editors[i][2] = editor[2];
        }

        for(int i = 0; i < table.getColumnCount(); i++) {
            table.getColumn(i).pack();
        }

        // Last table column always expands to fill remaining table size
        table.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event e) {
                Table table = (Table) e.widget;
                Rectangle area = table.getClientArea();
                int columnCount = table.getColumnCount();
                int totalGridLineWidth = (columnCount - 1) * table.getGridLineWidth();

                int totalColumnWidth = 0;
                for (TableColumn column : table.getColumns()) {
                    totalColumnWidth += column.getWidth();
                }

                int diff = area.width - (totalColumnWidth + totalGridLineWidth);

                TableColumn col = table.getColumns()[columnCount - 1];
                col.setWidth(diff + col.getWidth());
            }
        });

        // Disable table selection highlighting
        table.addListener(SWT.EraseItem, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if ((e.detail & SWT.SELECTED) != 0) {
                    e.detail &= ~SWT.SELECTED;
                }
            }
        });

        // Create Ok/Cancel button region
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("   &OK   ");
        okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (createAttribute()) {
                    shell.dispose();
                }
            }
        });

        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setFont(curFont);
        cancelButton.setText(" &Cancel ");
        cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newObject = null;
                shell.dispose();
            }
        });

        rankChoice.select(0);
        nFieldBox.select(nFieldBox.indexOf(String.valueOf(numberOfMembers)));

        shell.pack();

        table.getColumn(0).setWidth(table.getClientArea().width / 3);
        table.getColumn(1).setWidth(table.getClientArea().width / 3);

        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null) curFont.dispose();
            }
        });

        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Rectangle parentBounds = parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

        shell.open();

        Display display = shell.getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
    }

    @SuppressWarnings("unchecked")
    private boolean createAttribute() {
        String attrName = null;
        int rank = -1;
        long[] dims;

        attrName = nameField.getText();
        if (attrName != null) {
            attrName = attrName.trim();
        }

        if ((attrName == null) || (attrName.length() < 1)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Attribute name is not specified.");
            return false;
        }

        int n = table.getItemCount();
        if (n <= 0) {
            return false;
        }

        String[] mNames = new String[n];
        Datatype[] mDatatypes = new Datatype[n];
        int[] mOrders = new int[n];

        for (int i = 0; i < n; i++) {
            String name = (String) table.getItem(i).getData("MemberName");
            if ((name == null) || (name.length() <= 0)) {
                throw new IllegalArgumentException("Member name is empty");
            }
            mNames[i] = name;
            log.trace("createCompoundAttribute member[{}] name = {}", i, mNames[i]);

            int order = 1;
            String orderStr = (String) table.getItem(i).getData("MemberSize");
            if (orderStr != null) {
                try {
                    order = Integer.parseInt(orderStr);
                }
                catch (Exception ex) {
                    log.debug("compound order:", ex);
                }
            }
            mOrders[i] = order;

            String typeName = (String) table.getItem(i).getData("MemberType");
            log.trace("createCompoundAttribute type[{}] name = {}", i, typeName);
            Datatype type = null;
            try {
                if (DATATYPE_NAMES[0].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 1, Datatype.NATIVE, Datatype.NATIVE);
                }
                else if (DATATYPE_NAMES[1].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 2, Datatype.NATIVE, Datatype.NATIVE);
                }
                else if (DATATYPE_NAMES[2].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE);
                }
                else if (DATATYPE_NAMES[3].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
                }
                else if (DATATYPE_NAMES[4].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 2, Datatype.NATIVE, Datatype.SIGN_NONE);
                }
                else if (DATATYPE_NAMES[5].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.SIGN_NONE);
                }
                else if (DATATYPE_NAMES[6].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 8, Datatype.NATIVE, Datatype.NATIVE);
                }
                else if (DATATYPE_NAMES[7].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, Datatype.NATIVE);
                }
                else if (DATATYPE_NAMES[8].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
                }
                else if (DATATYPE_NAMES[9].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_STRING, order, Datatype.NATIVE, Datatype.NATIVE);
                }
                else if (DATATYPE_NAMES[10].equals(typeName)) { // enum
                    type = fileFormat.createDatatype(Datatype.CLASS_ENUM, 4, Datatype.NATIVE, Datatype.NATIVE);
                    if ((orderStr == null) || (orderStr.length() < 1) || orderStr.endsWith("...")) {
                        shell.getDisplay().beep();
                        Tools.showError(shell, "Create", "Invalid member values: " + orderStr);
                        return false;
                    }
                    else {
                        type.setEnumMembers(orderStr);
                    }
                }
                else if (DATATYPE_NAMES[11].equals(typeName)) {
                    type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 8, Datatype.NATIVE, Datatype.SIGN_NONE);
                }
                else {
                    throw new IllegalArgumentException("Invalid data type.");
                }
                mDatatypes[i] = type;
            }
            catch (Exception ex) {
                Tools.showError(shell, "Create", ex.getMessage());
                log.debug("createAttribute(): ", ex);
                return false;
            }
        } //  (int i=0; i<n; i++)

        rank = rankChoice.getSelectionIndex() + 1;
        log.trace("createCompoundAttribute rank={}", rank);
        StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Number of values in the current dimension size is less than " + rank);
            return false;
        }

        long lsize = 1; // The total size
        long l = 0;
        dims = new long[rank];
        String token = null;
        for (int i = 0; i < rank; i++) {
            token = st.nextToken().trim();
            try {
                l = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Invalid dimension size: " + currentSizeField.getText());
                return false;
            }

            if (l <= 0) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Dimension size must be greater than zero.");
                return false;
            }

            dims[i] = l;
            lsize *= l;
        }
        log.trace("Create: lsize={}", lsize);

        Attribute attr = null;
        try {
            H5Datatype datatype = (H5Datatype)createNewDatatype(null);

            attr = (Attribute)new H5CompoundAttr(parentObj, attrName, datatype, dims);
            Object value = H5Datatype.allocateArray(datatype, (int) lsize);
            attr.setAttributeData(value);

            log.trace("writeMetadata() via write()");
            attr.writeAttribute();
        }
        catch (Exception ex) {
            Tools.showError(shell, "Create", ex.getMessage());
            log.debug("createAttribute(): ", ex);
            return false;
        }

        newObject = (HObject)attr;

        return true;
    }

    private void updateMemberTableItems() {
        int n = 0;

        try {
            n = Integer.valueOf(nFieldBox.getItem(nFieldBox.getSelectionIndex())).intValue();
        }
        catch (Exception ex) {
            log.debug("Change number of members:", ex);
            return;
        }

        if (n == numberOfMembers) {
            return;
        }

        if(n > numberOfMembers) {
            try {
                for (int i = numberOfMembers; i < n; i++) {
                    TableEditor[] editor = addMemberTableItem(table);
                    editors[i][0] = editor[0];
                    editors[i][1] = editor[1];
                    editors[i][2] = editor[2];
                }
            }
            catch (Exception ex) {
                log.debug("Error adding member table items: ", ex);
                return;
            }
        }
        else {
            try {
                for(int i = numberOfMembers - 1; i >= n; i--) {
                    table.remove(i);
                }
            }
            catch (Exception ex) {
                log.debug("Error removing member table items: ", ex);
                return;
            }
        }

        table.setItemCount(n);
        numberOfMembers = n;
    }

    private TableEditor[] addMemberTableItem(Table table) {
        final TableItem item = new TableItem(table, SWT.NONE);
        final TableEditor[] editor = new TableEditor[3];

        for (int i = 0; i < editor.length; i++) editor[i] = new TableEditor(table);

        final Text nameText = new Text(table, SWT.SINGLE | SWT.BORDER);
        nameText.setFont(curFont);

        editor[0].grabHorizontal = true;
        editor[0].grabVertical = true;
        editor[0].horizontalAlignment = SWT.LEFT;
        editor[0].verticalAlignment = SWT.TOP;
        editor[0].setEditor(nameText, item, 0);

        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Text text = (Text) e.widget;
                item.setData("MemberName", text.getText());
            }
        });

        final CCombo typeCombo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setFont(curFont);
        typeCombo.setItems(DATATYPE_NAMES);

        editor[1].grabHorizontal = true;
        editor[1].grabVertical = true;
        editor[1].horizontalAlignment = SWT.LEFT;
        editor[1].verticalAlignment = SWT.TOP;
        editor[1].setEditor(typeCombo, item, 1);

        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                CCombo combo = (CCombo) e.widget;
                item.setData("MemberType", combo.getItem(combo.getSelectionIndex()));
            }
        });

        final Text sizeText = new Text(table, SWT.SINGLE | SWT.BORDER);
        sizeText.setFont(curFont);

        editor[2].grabHorizontal = true;
        editor[2].grabVertical = true;
        editor[2].horizontalAlignment = SWT.LEFT;
        editor[2].verticalAlignment = SWT.TOP;
        editor[2].setEditor(sizeText, item, 2);

        sizeText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Text text = (Text) e.widget;
                item.setData("MemberSize", text.getText());
            }
        });

        item.setData("MemberName", "");
        item.setData("MemberType", "");
        item.setData("MemberSize", "");

        item.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                editor[0].dispose();
                editor[1].dispose();
                editor[2].dispose();
                nameText.dispose();
                typeCombo.dispose();
                sizeText.dispose();
            }
        });

        return editor;
    }
}
