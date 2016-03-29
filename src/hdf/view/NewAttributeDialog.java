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

import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewAttributeDialog displays components for adding a new attribute.
 *
 * @author Jordan T. Henderson
 * @version 2.4 1/7/2016
 */
public class NewAttributeDialog extends Dialog {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewAttributeDialog.class);

    private Shell             shell;

    /** the default length of a string attribute */
    public static final int   DEFAULT_STRING_ATTRIBUTE_LENGTH = 256;

    /** the object which the attribute to be attached to */
    private HObject           hObject;

    private Attribute         newAttribute;

    /** TextField for entering the name of the dataset */
    private Text              nameField;

    /** TextField for entering the attribute value. */
    private Text              valueField;

    /** TextField for entering the length of the data array or string. */
    private Text              lengthField;

    /** The Choice of the datatypes */
    private Combo             classChoice, sizeChoice;

    /** The Choice of the object list */
    private Combo             objChoice;

    private Button            checkUnsigned;

    private Button            h4GrAttrRadioButton;

    private FileFormat        fileFormat;

    private List<HObject>     objList;

    private Label             arrayLengthLabel;

    private final boolean     isH5;

    /**
     * Constructs a NewAttributeDialog with specified object (dataset, group, or
     * image) for the new attribute to be attached to.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param obj
     *            the object for the attribute to be attached to.
     * @param objs
     *            the specified objects.
     */
    public NewAttributeDialog(Shell parent, HObject obj, List<HObject> objs) {
        super(parent, SWT.APPLICATION_MODAL);

        hObject = obj;
        newAttribute = null;
        objList = objs;
        isH5 = obj.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        fileFormat = obj.getFileFormat();
    }

    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("New Attribute...");
        shell.setImage(ViewProperties.getHdfIcon());
        shell.setLayout(new GridLayout(1, true));


        // Create content region
        Composite content = new Composite(shell, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = new Label(content, SWT.LEFT);
        label.setText("Name: ");

        nameField = new Text(content, SWT.SINGLE | SWT.BORDER);
        nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        label = new Label(content, SWT.LEFT);
        label.setText("Type: ");

        Composite optionsComposite = new Composite(content, SWT.NONE);
        optionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        optionsComposite.setLayout(new GridLayout(
                (!isH5 && (hObject instanceof Group) && ((Group) hObject).isRoot()) ? 5 : 3,
                false)
                );

        label = new Label(optionsComposite, SWT.LEFT);
        label.setText("Datatype class");

        label = new Label(optionsComposite, SWT.LEFT);
        label.setText("Size (bits) ");

        // Dummy label
        label = new Label(optionsComposite, SWT.LEFT);
        label.setText("");

        if (!isH5 && (hObject instanceof Group) && ((Group) hObject).isRoot()) {
            label = new Label(optionsComposite, SWT.LEFT);
            label.setText("");

            label = new Label(optionsComposite, SWT.LEFT);
            label.setText("");
        }

        classChoice = new Combo(optionsComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        classChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        classChoice.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int idx = classChoice.getSelectionIndex();
                sizeChoice.select(0);
                objChoice.setEnabled(false);
                lengthField.setEnabled(true);

                if ((idx == 0) || (idx == 5)) {
                    sizeChoice.setEnabled(true);
                    checkUnsigned.setEnabled(true);
                    arrayLengthLabel.setText("Array Size: ");

                    if (sizeChoice.getItemCount() == 2) {
                        sizeChoice.remove("32");
                        sizeChoice.remove("64");
                        sizeChoice.add("8");
                        sizeChoice.add("16");
                        sizeChoice.add("32");
                        sizeChoice.add("64");
                    }
                }
                else if ((idx == 1) || (idx == 6)) {
                    sizeChoice.setEnabled(true);
                    checkUnsigned.setEnabled(false);
                    arrayLengthLabel.setText("Array Size: ");

                    if (sizeChoice.getItemCount() == 4) {
                        sizeChoice.remove("16");
                        sizeChoice.remove("8");
                    }
                }
                else if (idx == 2) {
                    sizeChoice.setEnabled(false);
                    checkUnsigned.setEnabled(true);
                    arrayLengthLabel.setText("Array Size: ");
                }
                else if (idx == 3) {
                    sizeChoice.setEnabled(false);
                    checkUnsigned.setEnabled(false);
                    arrayLengthLabel.setText("String Length: ");
                }
                else if (idx == 4) {
                    sizeChoice.setEnabled(false);
                    checkUnsigned.setEnabled(false);
                    lengthField.setText("1");
                    lengthField.setEnabled(false);
                    arrayLengthLabel.setText("Array Size: ");
                    objChoice.setEnabled(true);
                    valueField.setText("");
                }
                else if (idx == 7) {
                    sizeChoice.setEnabled(false);
                    checkUnsigned.setEnabled(false);
                    lengthField.setEnabled(false);
                }
            }
        });

        classChoice.add("INTEGER");
        classChoice.add("FLOAT");
        classChoice.add("CHAR");

        if (isH5) {
            classChoice.add("STRING");
            classChoice.add("REFERENCE");
            classChoice.add("VLEN_INTEGER");
            classChoice.add("VLEN_FLOAT");
            classChoice.add("VLEN_STRING");
        }

        sizeChoice = new Combo(optionsComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        sizeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        sizeChoice.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (classChoice.getSelectionIndex() == 0) {
                    checkUnsigned.setEnabled(true);
                }
            }
        });

        sizeChoice.add("8");
        sizeChoice.add("16");
        sizeChoice.add("32");
        sizeChoice.add("64");

        checkUnsigned = new Button(optionsComposite, SWT.CHECK);
        checkUnsigned.setText("Unsigned");
        checkUnsigned.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        if (!isH5 && (hObject instanceof Group) && ((Group) hObject).isRoot()) {
            Button h4SdAttrRadioButton = new Button(optionsComposite, SWT.RADIO);
            h4SdAttrRadioButton.setText("SD");
            h4SdAttrRadioButton.setSelection(true);

            h4GrAttrRadioButton = new Button(optionsComposite, SWT.RADIO);
            h4GrAttrRadioButton.setText("GR");
        }

        arrayLengthLabel = new Label(content, SWT.LEFT);
        arrayLengthLabel.setText("Array Size: ");

        lengthField = new Text(content, SWT.SINGLE | SWT.BORDER);
        lengthField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        lengthField.setTextLimit(30);
        lengthField.setText("1");

        label = new Label(content, SWT.LEFT);
        label.setText("Value: ");

        valueField = new Text(content, SWT.SINGLE | SWT.BORDER);
        valueField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        valueField.setText("0");

        label = new Label(content, SWT.LEFT);
        label.setText("Object List: ");

        objChoice = new Combo(content, SWT.DROP_DOWN | SWT.READ_ONLY);
        objChoice.setEnabled(false);
        objChoice.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String objName = objChoice.getItem(objChoice.getSelectionIndex());

                long ref = -1;
                try {
                    HObject obj = fileFormat.get(objName);
                    ref = obj.getOID()[0];
                }
                catch (Exception ex) {
                    log.debug("object id:", ex);
                }

                if (ref > 0) {
                    if (valueField.getText().length() > 1) {
                        valueField.setText(valueField.getText() + "," + ref);
                        StringTokenizer st = new StringTokenizer(valueField.getText(), ",");
                        lengthField.setText(String.valueOf(st.countTokens()));
                    }
                    else {
                        valueField.setText(String.valueOf(ref));
                        lengthField.setText("1");
                    }
                }
            }
        });

        Iterator<HObject> it = objList.iterator();
        HObject hobj;
        while (it.hasNext()) {
            hobj = it.next();

            if (hobj instanceof Group) {
                if (((Group) hobj).isRoot()) continue;
            }

            objChoice.add(hobj.getFullName());
        }


        // Create Ok/Cancel/Help button region
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(3, false));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setText("   &Ok   ");
        GridData gridData = new GridData(SWT.END, SWT.FILL, true, false);
        gridData.widthHint = 70;
        okButton.setLayoutData(gridData);
        okButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (createAttribute()) {
                    shell.dispose();
                }
            }
        });

        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("&Cancel");
        gridData = new GridData(SWT.CENTER, SWT.FILL, false, false);
        gridData.widthHint = 70;
        cancelButton.setLayoutData(gridData);
        cancelButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                newAttribute = null;
                shell.dispose();
            }
        });

        Button helpButton = new Button(buttonComposite, SWT.PUSH);
        helpButton.setText("&Help");
        gridData = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
        gridData.widthHint = 70;
        helpButton.setLayoutData(gridData);
        helpButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                new HelpDialog(shell).open();
            }
        });

        classChoice.select(0);
        sizeChoice.select(0);
        objChoice.select(0);

        shell.pack();

        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Rectangle parentBounds = parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

        shell.open();

        Display display = parent.getDisplay();
        while(!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean createAttribute() {
        int tclass = -1, tsize = -1, torder = -1, tsign = -1;
        boolean isVLen = false;
        log.trace("createAttribute start");

        Object value = null;
        String strValue = valueField.getText();

        String attrName = nameField.getText();
        if (attrName != null) {
            attrName = attrName.trim();
        }

        if ((attrName == null) || (attrName.length() < 1)) {
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage("No attribute name specified.");
            error.open();
            return false;
        }

        String lengthStr = lengthField.getText();
        log.trace("Name is {} : Length={} and Value={}", attrName, lengthStr, strValue);

        int arraySize = 0;
        if ((lengthStr == null) || (lengthStr.length() <= 0)) {
            arraySize = 1;
        }
        else {
            try {
                arraySize = Integer.parseInt(lengthStr);
            }
            catch (Exception e) {
                arraySize = -1;
            }
        }

        if (arraySize <= 0) {
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage("Invalid attribute length.");
            error.open();
            return false;
        }

        StringTokenizer st = new StringTokenizer(strValue, ",");
        int count = Math.min(arraySize, st.countTokens());
        String theToken;
        log.trace("Count of Values is {}", count);

        // set datatype class
        int idx = classChoice.getSelectionIndex();
        if (idx == 0) {
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.getSelection()) {
                tsign = Datatype.SIGN_NONE;
            }
            torder = Datatype.NATIVE;
        }
        else if (idx == 1) {
            tclass = Datatype.CLASS_FLOAT;
            torder = Datatype.NATIVE;
        }
        else if (idx == 2) {
            tclass = Datatype.CLASS_CHAR;
            if (checkUnsigned.getSelection()) {
                tsign = Datatype.SIGN_NONE;
            }
            torder = Datatype.NATIVE;
        }
        else if (idx == 3) {
            tclass = Datatype.CLASS_STRING;
        }
        else if (idx == 4) {
            tclass = Datatype.CLASS_REFERENCE;
        }
        else if (idx == 5) {;
            isVLen = true;
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.getSelection()) {
                tsign = Datatype.SIGN_NONE;
            }
            torder = Datatype.NATIVE;

            MessageBox warn = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
            warn.setText(shell.getText());
            warn.setMessage("Multi-dimensional Variable Length Integer Attributes will be created without data.");
            warn.open();
        }
        else if (idx == 6) {;
            isVLen = true;
            tclass = Datatype.CLASS_FLOAT;
            torder = Datatype.NATIVE;

            MessageBox warn = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
            warn.setText(shell.getText());
            warn.setMessage("Multi-dimensional Variable Length Float Attributes will be created without data.");
            warn.open();
        }
        else if (idx == 7) {
            isVLen = true;
            tclass = Datatype.CLASS_STRING;
        }
        log.trace("Attribute: isVLen={} and tclass={} and torder={} and tsign={}", isVLen, tclass, torder, tsign);

        // set datatype size/order
        idx = sizeChoice.getSelectionIndex();
        if (isVLen) {
            tsize = -1;
            log.trace("Attribute isVLen={} and tsize={}", isVLen, tsize);
            String[] strArray = { strValue };
            value = strArray;
            if (tclass == Datatype.CLASS_INTEGER) {
                switch(idx) {
                        case 0:
                                tsize = 1;
                                break;
                        case 1:
                                tsize = 2;
                                break;
                        case 2:
                                tsize = 4;
                                break;
                        case 3:
                                tsize = 8;
                                break;
                }
                log.trace("Attribute VL-CLASS_INTEGER: tsize={}", tsize);
            }
            else if (tclass == Datatype.CLASS_FLOAT) {
                tsize = (idx + 1) * 4;
                log.trace("Attribute VL-CLASS_FLOAT: tsize={}", tsize);
            }
        }
        else {
            if (tclass == Datatype.CLASS_STRING) {
                int stringLength = 0;
                try {
                    stringLength = Integer.parseInt(lengthField.getText());
                }
                catch (NumberFormatException ex) {
                    stringLength = -1;
                }

                if (stringLength <= 0) {
                    stringLength = DEFAULT_STRING_ATTRIBUTE_LENGTH;
                }
                if (strValue.length() > stringLength) {
                    strValue = strValue.substring(0, stringLength);
                }

                tsize = stringLength;

                String[] strArray = { strValue };
                value = strArray;

                if (isH5) {
                    arraySize = 1; // support string type
                }
                else {
                    arraySize = stringLength; // array of characters
                }
                log.trace("Attribute CLASS_STRING: isVLen={} and tsize={} and arraySize={}", isVLen, tsize, arraySize);
            }
            else if (tclass == Datatype.CLASS_REFERENCE) {
                tsize = 1;
                arraySize = st.countTokens();
                long[] ref = new long[arraySize];
                for (int j = 0; j < arraySize; j++) {
                    theToken = st.nextToken().trim();
                    try {
                        ref[j] = Long.parseLong(theToken);
                    }
                    catch (NumberFormatException ex) {
                        MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                        error.setText(shell.getText());
                        error.setMessage(ex.getMessage());
                        error.open();
                        return false;
                    }
                }

                value = ref;
                torder = Datatype.NATIVE;
                log.trace("Attribute CLASS_REFERENCE: tsize={} and arraySize={}", tsize, arraySize);
            }
            else if (tclass == Datatype.CLASS_INTEGER) {
                switch(idx) {
                    case 0:
                        tsize = 1;
                        break;
                    case 1:
                        tsize = 2;
                        break;
                    case 2:
                        tsize = 4;
                        break;
                    case 3:
                        tsize = 8;
                        break;
                }
                log.trace("Attribute CLASS_INTEGER: tsize={}", tsize);
            }
            else if (tclass == Datatype.CLASS_FLOAT) {
                tsize = (idx + 1) * 4;
                log.trace("Attribute CLASS_FLOAT: tsize={}", tsize);
            }
            else {
                tsize = 1 << (idx);
                log.trace("Attribute other: tsize={}", tsize);
            }

            if ((tsize == 8) && !isH5 && (tclass == Datatype.CLASS_INTEGER)) {
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                error.setText(shell.getText());
                error.setMessage("HDF4 does not support 64-bit integer.");
                error.open();
                return false;
            }

            if (tclass == Datatype.CLASS_INTEGER) {
                if (tsign == Datatype.SIGN_NONE) {
                    if (tsize == 1) {
                        byte[] b = new byte[arraySize];
                        short sv = 0;
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                sv = Short.parseShort(theToken);
                            }
                            catch (NumberFormatException ex) {
                                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                                error.setText(shell.getText());
                                error.setMessage(ex.getMessage());
                                error.open();
                                return false;
                            }
                            if (sv < 0) {
                                sv = 0;
                            }
                            else if (sv > 255) {
                                sv = 255;
                            }
                            b[j] = (byte) sv;
                        }
                        value = b;
                    }
                    else if (tsize == 2) {
                        short[] s = new short[arraySize];
                        int iv = 0;
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                iv = Integer.parseInt(theToken);
                            }
                            catch (NumberFormatException ex) {
                                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                                error.setText(shell.getText());
                                error.setMessage(ex.getMessage());
                                error.open();
                                return false;
                            }
                            if (iv < 0) {
                                iv = 0;
                            }
                            else if (iv > 65535) {
                                iv = 65535;
                            }
                            s[j] = (short) iv;
                        }
                        value = s;
                    }
                    else if (tsize == 4) {
                        int[] i = new int[arraySize];
                        long lv = 0;
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                lv = Long.parseLong(theToken);
                            }
                            catch (NumberFormatException ex) {
                                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                                error.setText(shell.getText());
                                error.setMessage(ex.getMessage());
                                error.open();
                                return false;
                            }
                            if (lv < 0) {
                                lv = 0;
                            }
                            if (lv > 4294967295L) {
                                lv = 4294967295L;
                            }
                            i[j] = (int) lv;
                        }
                        value = i;
                    }
                    else if (tsize == 8) {
                        long[] i = new long[arraySize];
                        BigInteger lv = BigInteger.valueOf(0);
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                lv = new BigInteger(theToken);
                            }
                            catch (NumberFormatException ex) {
                                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                                error.setText(shell.getText());
                                error.setMessage(ex.getMessage());
                                error.open();
                                return false;
                            }
                            i[j] = (long) lv.longValue();
                        }
                        value = i;
                    }
                }
                else {
                    if (tsize == 1) {
                        byte[] b = new byte[arraySize];
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                b[j] = Byte.parseByte(theToken);
                            }
                            catch (NumberFormatException ex) {
                                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                                error.setText(shell.getText());
                                error.setMessage(ex.getMessage());
                                error.open();
                                return false;
                            }
                        }
                        value = b;
                    }
                    else if (tsize == 2) {
                        short[] s = new short[arraySize];

                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                s[j] = Short.parseShort(theToken);
                            }
                            catch (NumberFormatException ex) {
                                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                                error.setText(shell.getText());
                                error.setMessage(ex.getMessage());
                                error.open();
                                return false;
                            }
                        }
                        value = s;
                    }
                    else if (tsize == 4) {
                        int[] i = new int[arraySize];

                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                i[j] = Integer.parseInt(theToken);
                            }
                            catch (NumberFormatException ex) {
                                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                                error.setText(shell.getText());
                                error.setMessage(ex.getMessage());
                                error.open();
                                return false;
                            }
                        }
                        value = i;
                    }
                    else if (tsize == 8) {
                        long[] l = new long[arraySize];
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                l[j] = Long.parseLong(theToken);
                            }
                            catch (NumberFormatException ex) {
                                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                                error.setText(shell.getText());
                                error.setMessage(ex.getMessage());
                                error.open();
                                return false;
                            }
                        }
                        value = l;
                    }
                }
            }

            if (tclass == Datatype.CLASS_FLOAT) {
                if (tsize == 4) {
                    float[] f = new float[arraySize];
                    for (int j = 0; j < count; j++) {
                        theToken = st.nextToken().trim();
                        try {
                            f[j] = Float.parseFloat(theToken);
                        }
                        catch (NumberFormatException ex) {
                            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                            error.setText(shell.getText());
                            error.setMessage(ex.getMessage());
                            error.open();
                            return false;
                        }
                        if (Float.isInfinite(f[j]) || Float.isNaN(f[j])) {
                            f[j] = 0;
                        }
                    }
                    value = f;
                }
                else if (tsize == 8) {
                    double[] d = new double[arraySize];
                    for (int j = 0; j < count; j++) {
                        theToken = st.nextToken().trim();
                        try {
                            d[j] = Double.parseDouble(theToken);
                        }
                        catch (NumberFormatException ex) {
                            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                            error.setText(shell.getText());
                            error.setMessage(ex.getMessage());
                            error.open();
                            return false;
                        }
                        if (Double.isInfinite(d[j]) || Double.isNaN(d[j])) {
                            d[j] = 0;
                        }
                    }
                    value = d;
                }
            }
        }

        Datatype datatype = null;
        try {
            Datatype basedatatype = null;
            if (isVLen) {
                basedatatype = fileFormat.createDatatype(tclass, tsize, torder, tsign);
                tclass = Datatype.CLASS_VLEN;
                log.trace("Attribute CLASS_VLEN");
            }
            datatype = fileFormat.createDatatype(tclass, tsize, torder, tsign, basedatatype);
        }
        catch (Exception ex) {
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage(ex.getMessage());
            error.open();
            return false;
        }

        long[] dims = { arraySize };
        Attribute attr = new Attribute(attrName, datatype, dims);
        attr.setValue(value);

        try {
            if (!isH5 && (hObject instanceof Group) && ((Group) hObject).isRoot() && h4GrAttrRadioButton.getSelection()) {
                // don't find a good way to write HDF4 global
                // attribute. Use the isExisted to separate the
                // global attribute is GR or SD
                hObject.getFileFormat().writeAttribute(hObject, attr, false);
                if (hObject.getMetadata() == null) {
                    hObject.getMetadata().add(attr);
                }
            }
            else {
                log.trace("writeMetadata()");
                hObject.writeMetadata(attr);
            }
        }
        catch (Exception ex) {
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage(ex.getMessage());
            error.open();
            return false;
        }

        newAttribute = attr;

        log.trace("createAttribute finish");
        return true;
    }

    private class HelpDialog extends Dialog {
        private Shell helpShell;

        public HelpDialog(Shell parent) {
            super(parent, SWT.APPLICATION_MODAL);
        }

        public void open() {
            Shell parent = getParent();
            helpShell = new Shell(parent, SWT.TITLE | SWT.CLOSE |
                    SWT.RESIZE | SWT.BORDER | SWT.APPLICATION_MODAL);
            helpShell.setText("Create New Attribute");
            helpShell.setImage(ViewProperties.getHdfIcon());
            helpShell.setLayout(new GridLayout(1, true));

            // Try to create a Browser on platforms that support it
            Browser browser;
            try {
                browser = new Browser(helpShell, SWT.NONE);
                browser.setBounds(0, 0, 500, 500);
                browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

                //TODO: Fix URLClassLoader URLs to load local html file from .jar
                try {
                    URL url = null, url2 = null, url3 = null;
                    String rootPath = ViewProperties.getViewRoot();

                    try {
                        url = new URL("file://" + rootPath + "/HDFView.jar");
                    }
                    catch (java.net.MalformedURLException mfu) {
                        log.debug("help information:", mfu);
                    }

                    try {
                        url2 = new URL("file://" + rootPath + "/");
                    }
                    catch (java.net.MalformedURLException mfu) {
                        log.debug("help information:", mfu);
                    }

                    try {
                        url3 = new URL("file://" + rootPath + "/src/");
                    }
                    catch (java.net.MalformedURLException mfu) {
                        log.debug("help information:", mfu);
                    }

                    URL uu[] = { url, url2, url3 };
                    URLClassLoader cl = new URLClassLoader(uu);
                    URL u = cl.findResource("hdf/view/NewAttrHelp.html");

                    browser.setUrl(u.toString());

                    cl.close();
                }
                catch (Exception e) {
                    StringBuffer buff = new StringBuffer();
                    buff.append("<html>");
                    buff.append("<body>");
                    buff.append("ERROR: cannot load help information.");
                    buff.append("</body>");
                    buff.append("</html>");
                    browser.setText(buff.toString(), true);
                }

                Button okButton = new Button(helpShell, SWT.PUSH);
                okButton.setText("   Ok   ");
                okButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
                okButton.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        helpShell.dispose();
                    }
                });

                helpShell.pack();

                helpShell.setSize(helpShell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

                Rectangle parentBounds = parent.getBounds();
                Point shellSize = helpShell.getSize();
                helpShell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                                (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

                helpShell.open();

                Display display = parent.getDisplay();
                while(!helpShell.isDisposed()) {
                    if (!display.readAndDispatch())
                        display.sleep();
                }
            } catch (Exception ex) {
                // Try opening help link in external browser if platform
                // doesn't support SWT browser
                browser = null;
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                error.setMessage("Platform doesn't support Browser. Opening external link in web browser...");
                error.setText("Browser support");
                error.open();
                helpShell.dispose();

                //TODO: Add support for launching in external browser
            }
        }
    }

    /** @return the new attribute created. */
    public Attribute getAttribute() {
        return newAttribute;
    }
}
