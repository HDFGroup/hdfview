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
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.MetaDataContainer;
import hdf.object.h5.H5CompoundAttr;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5ScalarAttr;

import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * NewScalarAttributeDialog displays components for adding a new attribute.
 *
 * @author Jordan T. Henderson
 * @version 2.4 1/7/2016
 */
public class NewScalarAttributeDialog extends NewDataObjectDialog {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewScalarAttributeDialog.class);

    /** the default length of a string attribute */
    public static final int   DEFAULT_STRING_ATTRIBUTE_LENGTH = 256;

    private Text              currentSizeField;

    private Combo             rankChoice;

    /** TextField for entering the name of the attribute */
    protected Text            nameField;

    /**
     * Constructs a NewScalarAttributeDialog with specified object (dataset, group, or
     * image) for the new attribute to be attached to.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pObject
     *            the parent object which the new attribute is attached to.
     * @param objs
     *            the list of all objects.
     */
    public NewScalarAttributeDialog(Shell parent, HObject pObject, List<HObject> objs) {
        super(parent, pObject, objs);
    }

    /**
     * Open the NewScalarAttributeDialog for adding a new attribute.
     */
    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Attribute...");
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setLayout(new GridLayout(1, true));


        // Create Attribute name / Parent Object region
        Composite fieldComposite = new Composite(shell, SWT.NONE);
        fieldComposite.setLayout(new GridLayout(2, false));
        fieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label attributeNameLabel = new Label(fieldComposite, SWT.LEFT);
        attributeNameLabel.setFont(curFont);
        attributeNameLabel.setText("Attribute name: ");

        nameField = new Text(fieldComposite, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);

        data.minimumWidth = 250;
        nameField.setLayoutData(data);

        Label parentObjectLabel = new Label(fieldComposite, SWT.LEFT);
        parentObjectLabel.setFont(curFont);
        parentObjectLabel.setText("Parent Object: ");

        // Create Datatype region
        createDatatypeWidget();

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

    /** Check if the dim size is valid */
    private void checkDimSize() {
        String dimStr = currentSizeField.getText();
        StringTokenizer stDim = new StringTokenizer(dimStr, "x");

        int rank = stDim.countTokens();
        long dim = 0;
        for (int i = 0; i < rank; i++) {
            String token = stDim.nextToken().trim();

            token = token.toLowerCase();
            try {
                dim = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Check", "Invalid dimension size: " + dimStr);
                return;
            }
        } //  (int i = 0; i < rank; i++)
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

        rank = rankChoice.getSelectionIndex() + 1;
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

            if (datatype.isCompound())
                attr = (Attribute)new H5CompoundAttr(parentObj, attrName, datatype, dims);
            else
                attr = (Attribute)new H5ScalarAttr(parentObj, attrName, datatype, dims);
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
