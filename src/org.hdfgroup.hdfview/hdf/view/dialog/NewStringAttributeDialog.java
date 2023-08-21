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

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;

import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * NewStringAttributeDialog displays components for adding a new attribute.
 *
 * @author Jordan T. Henderson
 * @version 2.4 1/7/2016
 */
public class NewStringAttributeDialog extends NewDataObjectDialog {

    private static final Logger log = LoggerFactory.getLogger(NewStringAttributeDialog.class);

    /** the default length of a string attribute */
    public static final int   DEFAULT_STRING_ATTRIBUTE_LENGTH = 256;

    /** TextField for entering the name of the dataset */
    private Text              nameField;

    /** TextField for entering the attribute value. */
    private Text              valueField;

    /** The Choice of the object list */
    private Combo             objChoice;

    private Button            h4GrAttrRadioButton;

    private Label             arrayLengthLabel;

    /** If the attribute should be attached to a hdf4 object */
    protected boolean isH4;
    /** If the attribute should be attached to a netcdf object */
    protected boolean isN3;

    /**
     * Constructs a NewStringAttributeDialog with specified object (dataset, group, or
     * image) for the new attribute to be attached to.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pObject
     *            the parent object which the new attribute is attached to.
     * @param objs
     *            the list of all objects.
     */
    public NewStringAttributeDialog(Shell parent, HObject pObject, List<HObject> objs) {
        super(parent, pObject, objs);
        isH4 = pObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));
        isN3 = pObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3));
    }

    /**
     * Open the NewStringAttributeDialog for adding a new attribute.
     */
    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Attribute...");
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setLayout(new GridLayout(1, true));


        // Create content region
        Composite content = new Composite(shell, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Name: ");

        nameField = new Text(content, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Type: ");

        Composite optionsComposite = new Composite(content, SWT.NONE);
        optionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        optionsComposite.setLayout(new GridLayout(
                (!isH5 && (parentObj instanceof Group) && ((Group) parentObj).isRoot()) ? 5 : 3,
                        false)
                );

        // Dummy label
        label = new Label(optionsComposite, SWT.LEFT);
        label.setFont(curFont);
        label.setText("");

        if (!isH5 && (parentObj instanceof Group) && ((Group) parentObj).isRoot()) {
            label = new Label(optionsComposite, SWT.LEFT);
            label.setFont(curFont);
            label.setText("");

            label = new Label(optionsComposite, SWT.LEFT);
            label.setFont(curFont);
            label.setText("");
        }

        createDatatypeWidget();

        if (!isH5 && (parentObj instanceof Group) && ((Group) parentObj).isRoot()) {
            Button h4SdAttrRadioButton = new Button(optionsComposite, SWT.RADIO);
            h4SdAttrRadioButton.setFont(curFont);
            h4SdAttrRadioButton.setText("SD");
            h4SdAttrRadioButton.setSelection(true);

            h4GrAttrRadioButton = new Button(optionsComposite, SWT.RADIO);
            h4GrAttrRadioButton.setFont(curFont);
            h4GrAttrRadioButton.setText("GR");
        }

        arrayLengthLabel = new Label(content, SWT.LEFT);
        arrayLengthLabel.setFont(curFont);
        arrayLengthLabel.setText("Array Size: ");

        lengthField = new Text(content, SWT.SINGLE | SWT.BORDER);
        lengthField.setFont(curFont);
        lengthField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        lengthField.setTextLimit(30);
        lengthField.setText("1");

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Value: ");

        valueField = new Text(content, SWT.SINGLE | SWT.BORDER);
        valueField.setFont(curFont);
        valueField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        valueField.setText("0");

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Object List: ");

        objChoice = new Combo(content, SWT.DROP_DOWN | SWT.READ_ONLY);
        objChoice.setFont(curFont);
        objChoice.setEnabled(true);
        objChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        objChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String objName = objChoice.getItem(objChoice.getSelectionIndex());

                long[] ref = null;
                try {
                    HObject obj = fileFormat.get(objName);
                    ref = obj.getOID();
                }
                catch (Exception ex) {
                    log.debug("object id:", ex);
                }

                if (ref != null) {
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

        Iterator<?> it = objList.iterator();
        HObject hobj;
        while (it.hasNext()) {
            hobj = (HObject) it.next();

            if (hobj instanceof Group) {
                if (((Group) hobj).isRoot()) continue;
            }

            objChoice.add(hobj.getFullName());
        }

        // Add label to take up extra space when resizing dialog
        label = new Label(content, SWT.LEFT);
        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        // Create Ok/Cancel/Help button region
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(3, false));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

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
        cancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newObject = null;
                shell.dispose();
            }
        });

        Button helpButton = new Button(buttonComposite, SWT.PUSH);
        helpButton.setFont(curFont);
        helpButton.setText(" &Help ");
        helpButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
        helpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new HelpDialog(shell).open();
            }
        });

        shell.pack();

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
        Object value = null;
        String strValue = valueField.getText();

        String attrName = nameField.getText();
        if (attrName != null) {
            attrName = attrName.trim();
        }

        if ((attrName == null) || (attrName.length() < 1)) {
            Tools.showError(shell, "Create", "No attribute name specified.");
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
            Tools.showError(shell, "Create", "Invalid attribute length.");
            return false;
        }

        StringTokenizer st = new StringTokenizer(strValue, ",");
        int count = Math.min(arraySize, st.countTokens());
        String theToken;
        log.trace("Count of Values is {}", count);

        // set datatype class
        Datatype datatype = super.createNewDatatype(null);
        if (isVLen) {
            log.trace("Attribute isVLen={} and tsize={}", isVLen, tsize);
            String[] strArray = { strValue };
            value = strArray;
        }
        else {
            if (tclass == Datatype.CLASS_STRING) {
                if (!isVlenStr) {
                    if (strValue.length() > tsize) {
                        strValue = strValue.substring(0, tsize);
                    }
                }

                String[] strArray = { strValue };
                value = strArray;

                if (isH5) {
                    arraySize = 1; // support string type
                }
                else {
                    arraySize = tsize; // array of characters
                }
                log.trace("Attribute CLASS_STRING: isVlenStr={} and tsize={} and arraySize={}", isVlenStr, tsize, arraySize);
            }
            else if (tclass == Datatype.CLASS_REFERENCE) {
                arraySize = st.countTokens();
                long[] ref = new long[arraySize];
                for (int j = 0; j < arraySize; j++) {
                    theToken = st.nextToken().trim();
                    try {
                        ref[j] = Long.parseLong(theToken);
                    }
                    catch (NumberFormatException ex) {
                        Tools.showError(shell, "Create", ex.getMessage());
                        return false;
                    }
                }

                value = ref;
                log.trace("Attribute CLASS_REFERENCE: tsize={} and arraySize={}", tsize, arraySize);
            }
            else if (tclass == Datatype.CLASS_INTEGER) {
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
                                Tools.showError(shell, "Create", ex.getMessage());
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
                                Tools.showError(shell, "Create", ex.getMessage());
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
                    else if ((tsize == 4) || (tsize == -1)) {
                        int[] i = new int[arraySize];
                        long lv = 0;
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                lv = Long.parseLong(theToken);
                            }
                            catch (NumberFormatException ex) {
                                Tools.showError(shell, "Create", ex.getMessage());
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
                                Tools.showError(shell, "Create", ex.getMessage());
                                return false;
                            }
                            i[j] = lv.longValue();
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
                                Tools.showError(shell, "Create", ex.getMessage());
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
                                Tools.showError(shell, "Create", ex.getMessage());
                                return false;
                            }
                        }
                        value = s;
                    }
                    else if ((tsize == 4) || (tsize == -1)) {
                        int[] i = new int[arraySize];

                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                i[j] = Integer.parseInt(theToken);
                            }
                            catch (NumberFormatException ex) {
                                Tools.showError(shell, "Create", ex.getMessage());
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
                                Tools.showError(shell, "Create", ex.getMessage());
                                return false;
                            }
                        }
                        value = l;
                    }
                }
            }

            if (tclass == Datatype.CLASS_FLOAT) {
                if ((tsize == 4) || (tsize == -1)) {
                    float[] f = new float[arraySize];
                    for (int j = 0; j < count; j++) {
                        theToken = st.nextToken().trim();
                        try {
                            f[j] = Float.parseFloat(theToken);
                        }
                        catch (NumberFormatException ex) {
                            Tools.showError(shell, "Create", ex.getMessage());
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
                            Tools.showError(shell, "Create", ex.getMessage());
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

        long[] dims = { arraySize };
        Attribute attr = null;
        try {
            if (isH4)
                attr = new hdf.object.h4.H4ScalarAttribute(parentObj, attrName, datatype, dims);
            else
                attr = new hdf.object.nc2.NC2Attribute(parentObj, attrName, datatype, dims);
        }
        catch (Exception ex) {
            Tools.showError(shell, "Create", ex.getMessage());
            log.debug("createAttribute(): ", ex);
            return false;
        }
        if (attr ==null) {
            Tools.showError(shell, "Create", "Attribute could not be created");
            log.debug("createAttribute(): failed");
            return false;
        }
        attr.setAttributeData(value);

        try {
            if (!isH5 && (parentObj instanceof Group) && ((Group) parentObj).isRoot() && h4GrAttrRadioButton.getSelection()) {
                parentObj.getFileFormat().writeAttribute(parentObj, attr, false);
                // don't find a good way to write HDF4 global
                // attribute. Use the isExisted to separate the
                // global attribute is GR or SD

                if (((MetaDataContainer) parentObj).getMetadata() == null) {
                    ((MetaDataContainer) parentObj).getMetadata().add(attr);
                }
            }
            else {
                log.trace("writeMetadata() via write()");
                attr.writeAttribute();
            }
        }
        catch (Exception ex) {
            Tools.showError(shell, "Create", ex.getMessage());
            log.debug("createAttribute(): ", ex);
            return false;
        }

        newObject = (HObject)attr;

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
            helpShell.setFont(curFont);
            helpShell.setText("Create New Attribute");
            helpShell.setImages(ViewProperties.getHdfIcons());
            helpShell.setLayout(new GridLayout(1, true));

            // Try to create a Browser on platforms that support it
            try {
                Browser browser = new Browser(helpShell, SWT.NONE);
                browser.setFont(curFont);
                browser.setBounds(0, 0, 500, 500);
                browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

                if (ClassLoader.getSystemResource("hdf/view/HDFView.class").toString().startsWith("jar")) {
                    // Attempt to load HTML help file from jar
                    try (InputStream in = getClass().getClassLoader().getResourceAsStream("hdf/view/NewAttrHelp.html")) {
                        Scanner scan = new Scanner(in);
                        StringBuilder buffer = new StringBuilder();
                        while(scan.hasNextLine()) {
                            buffer.append(scan.nextLine());
                        }

                        browser.setText(buffer.toString());

                        scan.close();
                    }
                    catch (Exception e) {
                        StringBuilder buff = new StringBuilder();
                        buff.append("<html>")
                            .append("<body>")
                            .append("ERROR: cannot load help information.")
                            .append("</body>")
                            .append("</html>");
                        browser.setText(buff.toString(), true);
                    }
                }
                else {
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
                        try (URLClassLoader cl = new URLClassLoader(uu)) {
                            URL u = cl.findResource("hdf/view/NewAttrHelp.html");

                            browser.setUrl(u.toString());
                        }
                        catch (Exception ex) {
                            log.trace("URLClassLoader failed:", ex);
                        }
                    }
                    catch (Exception e) {
                        StringBuilder buff = new StringBuilder();
                        buff.append("<html>")
                            .append("<body>")
                            .append("ERROR: cannot load help information.")
                            .append("</body>")
                            .append("</html>");
                        browser.setText(buff.toString(), true);
                    }
                }

                Button okButton = new Button(helpShell, SWT.PUSH);
                okButton.setFont(curFont);
                okButton.setText("   &OK   ");
                okButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
                okButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        helpShell.dispose();
                    }
                });

                helpShell.pack();

                helpShell.setSize(new Point(500, 500));

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
            }
            catch (Error er) {
                // Try opening help link in external browser if platform
                // doesn't support SWT browser
                Tools.showError(shell, "Browser support",
                        "Platform doesn't support Browser. Opening external link in web browser...");

                //TODO: Add support for launching in external browser
            }
            catch (Exception ex) {
                log.debug("Open New Attribute Help failure: ", ex);
            }
        }
    }

    /** @return the new attribute created. */
    public Attribute getAttribute() {
        return (Attribute)newObject;
    }
}
