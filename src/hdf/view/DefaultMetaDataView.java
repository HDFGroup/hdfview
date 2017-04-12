/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.object.Attribute;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * DefaultMetadataView is used to show data properties. Data properties include
 * attributes and general information such as object type, data type and data
 * space.
 *
 * @author Jordan T. Henderson
 * @version 2.4 2/21/2016
 */
public class DefaultMetaDataView implements MetaDataView {
    private final Display              display = Display.getDefault();
    private Shell                      shell;

    private Font                       curFont;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultMetaDataView.class);

    private final ViewManager          viewer;

    /** The HDF data object */
    private HObject                    hObject;

    private StyledText                 attrContentArea;
    private Table                      attrTable; // table to hold a list of attributes
    private Label                      attrNumberLabel;
    private int                        numAttributes;
    private boolean                    isH5, isH4;

    public DefaultMetaDataView(ViewManager theView, HObject theObj) {
        log.trace("DefaultMetaDataView: start");

        shell = new Shell(display, SWT.SHELL_TRIM);

        shell.setData(this);

        shell.setImage(ViewProperties.getHdfIcon());
        shell.setLayout(new GridLayout(1, true));

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

        shell.setFont(curFont);

        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null) curFont.dispose();

                viewer.removeDataView(DefaultMetaDataView.this);
            }
        });

        viewer = theView;
        hObject = theObj;
        numAttributes = 0;

        isH5 = hObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        isH4 = hObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        if (hObject == null) {
            shell.dispose();
        }
        StringBuffer sb = new StringBuffer("Properties");
        if (hObject.getPath() != null) {
            sb.append("  at  ");
            sb.append(hObject.getPath());
        }
        sb.append("  [");
        sb.append(hObject.getName());
        sb.append("  in  ");
        sb.append(hObject.getFileFormat().getParent());
        sb.append("]");
        shell.setText(sb.toString());

        // Get the metadata information before adding GUI components */
        try {
            hObject.getMetadata();
        }
        catch (Exception ex) {
            log.debug("Error retrieving metadata of object " + hObject.getName() + ":", ex);
        }

        // Create main content
        Composite content = createAttributesComposite(shell);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        shell.setMenuBar(createMenuBar());

        // Create Close button region
        Button closeButton = new Button(shell, SWT.PUSH);
        closeButton.setFont(curFont);
        closeButton.setText("   &Close   ");
        closeButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
        closeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });

        viewer.addDataView(this);

        log.trace("DefaultMetaDataView: finish");

        shell.open();
    }

    /** Add an attribute to a data object. */
    public Attribute addAttribute(HObject obj) {
        if (obj == null) {
            return null;
        }

        HObject node = obj.getFileFormat().getRootObject();
        NewAttributeDialog dialog = new NewAttributeDialog(shell, obj, ((Group) node).breadthFirstMemberList());
        dialog.open();

        Attribute attr = dialog.getAttribute();
        if (attr == null) {
            return null;
        }

        String rowData[] = new String[4]; // name, value, type, size

        rowData[0] = attr.getName();
        rowData[2] = attr.getType().getDatatypeDescription();

        rowData[1] = attr.toString(", ");

        long dims[] = attr.getDataDims();

        rowData[3] = String.valueOf(dims[0]);
        for (int j = 1; j < dims.length; j++) {
            rowData[3] += " x " + dims[j];
        }

        TableItem item = new TableItem(attrTable, SWT.NONE);
        item.setFont(curFont);
        item.setText(rowData);

        numAttributes++;
        attrContentArea.setText("");
        attrNumberLabel.setText("Number of attributes = " + numAttributes);

        return attr;
    }

    /** Delete an attribute from a data object. */
    public Attribute deleteAttribute(HObject obj) {
        if (obj == null) {
            return null;
        }

        int idx = attrTable.getSelectionIndex();
        if (idx < 0) {
            Tools.showError(shell, "No attribute is selected.", shell.getText());
            return null;
        }

        int answer = SWT.NO;
        if (((HDFView) viewer).getTestState()) {
            if(MessageDialog.openConfirm(shell,
                    shell.getText(), "Do you want to delete the selected attribute?"))
                answer = SWT.YES;
        }
        else {
            MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            confirm.setText(shell.getText());
            confirm.setMessage("Do you want to delete the selected attribute?");
            answer = confirm.open();
        }
        if (answer == SWT.NO) return null;

        List<?> attrList = null;
        try {
            attrList = obj.getMetadata();
        }
        catch (Exception ex) {
            attrList = null;
        }

        if (attrList == null) {
            return null;
        }

        Attribute attr = (Attribute) attrList.get(idx);
        try {
            obj.removeMetadata(attr);
        }
        catch (Exception ex) {
            log.debug("delete an attribute from a data object:", ex);
        }

        attrTable.remove(idx);
        numAttributes--;

        attrContentArea.setText("");
        attrNumberLabel.setText("Number of attributes = " + numAttributes);

        return attr;
    }

    /** Returns the data object displayed in this data viewer */
    public HObject getDataObject() {
        return hObject;
    }

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

        String attrName = (String) attrTable.getItem(row).getText(0);
        List<?> attrList = null;
        try {
            attrList = hObject.getMetadata();
        }
        catch (Exception ex) {
            Tools.showError(shell, ex.getMessage(), shell.getText());
            return;
        }

        Attribute attr = (Attribute) attrList.get(row);

        if (col == 1) { // To change attribute value
            log.trace("updateAttributeValue: change attribute value");
            Object data = attr.getValue();
            if (data == null) {
                return;
            }

            int array_length = Array.getLength(data);
            StringTokenizer st = new StringTokenizer(newValue, ",");
            if (st.countTokens() < array_length) {
                Tools.showError(shell, "More data values needed: " + newValue, shell.getText());
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
                    Tools.showError(shell, ex.getMessage(), shell.getText());
                    return;
                }

                if (isUnsigned && (d < 0)) {
                    Tools.showError(shell, "Negative value for unsigned integer: " + theToken, shell.getText());
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
                            Tools.showError(shell, "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, shell.getText());
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
                            Tools.showError(shell, "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, shell.getText());
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
                            Tools.showError(shell, "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, shell.getText());
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
                                    Tools.showError(shell, "Data is out of range[" + min + ", " + max
                                            + "]: " + theToken, shell.getText());
                                }
                                lvalue = big.longValue();
                                log.trace("updateAttributeValue: big.longValue={}", lvalue);
                                Array.setLong(data, i, lvalue);
                            }
                            else
                                Array.set(data, i, (Object)theToken);
                        }
                        else {
                            min = Long.MIN_VALUE;
                            max = Long.MAX_VALUE;
                            if ((d > max) || (d < min)) {
                                Tools.showError(shell, "Data is out of range[" + min + ", " + max
                                        + "]: " + theToken, shell.getText());
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
                        Array.set(data, i, (Object)theToken);
                        break;
                }
            }

            try {
                hObject.getFileFormat().writeAttribute(hObject, attr, true);
            }
            catch (Exception ex) {
                Tools.showError(shell, ex.getMessage(), shell.getText());
                return;
            }

            // update the attribute table
            attrTable.getItem(row).setText(1, attr.toString(", "));
        }

        if ((col == 0) && isH5) { // To change attribute name
            log.trace("updateAttributeValue: change attribute name");
            try {
                hObject.getFileFormat().renameAttribute(hObject, attrName, newValue);
            }
            catch (Exception ex) {
                Tools.showError(shell, ex.getMessage(), shell.getText());
                return;
            }

            // update the attribute table
            attrTable.getItem(row).setText(0, newValue);
        }
        if (hObject instanceof ScalarDS) {
            ScalarDS ds = (ScalarDS) hObject;
            try {
                log.trace("updateAttributeValue: ScalarDS:updateMetadata");
                ds.updateMetadata(attr);
            }
            catch (Exception ex) {
                Tools.showError(shell, ex.getMessage(), shell.getText());
            }
        }
        else {
            log.trace("updateAttributeValue: hObject is not instanceof ScalarDS");
        }
    }

    private Composite createAttributesComposite(Shell parent) {
        log.trace("createAttributesComposite: start");

        List<?> attrList = null;
        FileFormat theFile = hObject.getFileFormat();

        try {
            attrList = hObject.getMetadata();
        }
        catch (Exception ex) {
            attrList = null;
        }
        if (attrList != null) {
            numAttributes = attrList.size();
        }

        log.trace("createAttributesComposite:  isH5={} numAttributes={}", isH5, numAttributes);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(4, false));

        attrNumberLabel = new Label(composite, SWT.RIGHT);
        attrNumberLabel.setFont(curFont);
        attrNumberLabel.setText("Number of attributes = 0");

        // Add dummy labels
        Label dummyLabel = new Label(composite, SWT.RIGHT);
        dummyLabel.setFont(curFont);
        dummyLabel.setText("");

        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        if (isH4) data.horizontalSpan = 2; // Delete button doesn't show for HDF4 files
        dummyLabel.setLayoutData(data);

        Button addButton = new Button(composite, SWT.PUSH);
        addButton.setFont(curFont);
        addButton.setText("  &Add  ");
        addButton.setEnabled(!theFile.isReadOnly());
        addButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        addButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                addAttribute(hObject);
            }
        });

        // deleting is not supported by HDF4
        if(isH5) {
            Button delButton = new Button(composite, SWT.PUSH);
            delButton.setFont(curFont);
            delButton.setText(" &Delete ");
            delButton.setEnabled(!theFile.isReadOnly());
            delButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
            delButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    deleteAttribute(hObject);
                }
            });
        }


        SashForm sashForm = new SashForm(composite, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

        String[] columnNames = { "Name", "Value", "Type", "Array Size" };

        attrTable = new Table(sashForm, SWT.FULL_SELECTION | SWT.BORDER);
        attrTable.setLinesVisible(true);
        attrTable.setHeaderVisible(true);
        attrTable.setFont(curFont);

        // Only allow editing of attribute name and value
        attrTable.addListener(SWT.MouseDoubleClick, attrTableCellEditor);

        attrTable.addListener(SWT.MouseDown, new Listener() {
            public void handleEvent(Event e) {
                Point location = new Point(e.x, e.y);
                TableItem item = attrTable.getItem(location);

                if(item == null) return;

                for(int i = 0; i < attrTable.getColumnCount(); i++) {
                    Rectangle rect = item.getBounds(i);

                    if(rect.contains(location)) {
                        attrContentArea.setText(item.getText(i));
                    }
                }
            }
        });

        for(int i = 0; i < columnNames.length; i++) {
            TableColumn column = new TableColumn(attrTable, SWT.NONE);
            column.setText(columnNames[i]);
            column.setMoveable(false);
        }

        if (attrList == null) {
            log.trace("createAttributesComposite:  attrList == null");
        }
        else {
            attrNumberLabel.setText("Number of attributes = " + numAttributes);

            Attribute attr = null;
            String name, type, size;
            for (int i = 0; i < numAttributes; i++) {
                attr = (Attribute) attrList.get(i);
                name = attr.getName();
                type = attr.getType().getDatatypeDescription();
                log.trace("createAttributesComposite:  attr[{}] is {} as {}", i, name, type);

                if (name == null) name = "null";
                if (type == null) type = "null";

                if (attr.isScalar()) {
                    size = "Scalar";
                }
                else {
                    long dims[] = attr.getDataDims();
                    size = String.valueOf(dims[0]);
                    for (int j = 1; j < dims.length; j++) {
                        size += " x " + dims[j];
                    }

                    if (size == null) size = "null";
                }

                TableItem item = new TableItem(attrTable, SWT.NONE);
                item.setFont(curFont);

                if (attr.getProperty("field") != null) {
                    String fieldInfo = " {Field: "+attr.getProperty("field")+"}";
                    item.setText(0, (name + fieldInfo == null) ? "null" : name + fieldInfo);
                } else {
                    item.setText(0, (name == null) ? "null" : name);
                }

                String value = attr.toString(", ");
                if (value == null) value = "null";

                item.setText(1, value);
                item.setText(2, type);
                item.setText(3, size);
            } // for (int i=0; i<n; i++)
        }

        for(int i = 0; i < columnNames.length; i++) {
            attrTable.getColumn(i).pack();
        }

        // Prevent attributes with many values, such as array types, from making
        // the window too wide
        attrTable.getColumn(1).setWidth(400);

        // set cell height for large fonts
        //int cellRowHeight = Math.max(16, attrTable.getFontMetrics(attrTable.getFont()).getHeight());
        //attrTable.setRowHeight(cellRowHeight);

        attrContentArea = new StyledText(sashForm, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        attrContentArea.setEditable(false);
        attrContentArea.setAlwaysShowScrollBars(false);
        attrContentArea.setFont(curFont);

        sashForm.setWeights(new int[] {1, 2});

        log.trace("createAttributesComposite: finish");

        return composite;
    }

    private Menu createMenuBar() {
        Menu menuBar = new Menu(shell, SWT.BAR);

        MenuItem item = new MenuItem(menuBar, SWT.PUSH);
        item.setText("Close");
        item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });

        return menuBar;
    }

    // Listener to allow user to only change attribute name or value
    private Listener attrTableCellEditor = new Listener() {
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
                        if (!(i == 1 || (isH5 && (i == 0)))) {
                            // Only attribute value and name can be changed
                            return;
                        }

                        final int column = i;
                        final int row = index;

                        final Text text = new Text(attrTable, SWT.NONE);
                        text.setFont(curFont);

                        Listener textListener = new Listener() {
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

                if (!visible) return;

                index++;
            }
        }
    };
}
