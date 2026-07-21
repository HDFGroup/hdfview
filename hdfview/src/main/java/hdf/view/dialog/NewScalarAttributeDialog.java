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

import java.util.List;
import java.util.StringTokenizer;

import hdf.object.Attribute;
import hdf.object.HObject;
import hdf.object.h5.H5CompoundAttr;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5ScalarAttr;
import hdf.view.Tools;
import hdf.view.ViewProperties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
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

/**
 * NewScalarAttributeDialog displays components for adding a new attribute.
 *
 * @author Jordan T. Henderson
 * @version 2.4 1/7/2016
 */
public class NewScalarAttributeDialog extends NewDataObjectDialog {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(NewScalarAttributeDialog.class);

    /** the default length of a string attribute. */
    public static final int DEFAULT_STRING_ATTRIBUTE_LENGTH = 256;

    private Text currentSizeField;

    private Combo rankChoice;

    /** TextField for entering the name of the attribute. */
    protected Text nameField;

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
    public NewScalarAttributeDialog(Shell parent, HObject pObject, List<HObject> objs)
    {
        super(parent, pObject, objs);
    }

    /**
     * Open the NewScalarAttributeDialog for adding a new attribute.
     */
    public void open()
    {
        Shell parent = getParent();
        shell        = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
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
            public void widgetSelected(SelectionEvent e)
            {
                int rank                     = rankChoice.getSelectionIndex() + 1;
                StringBuilder currentSizeStr = new StringBuilder("1");

                for (int i = 1; i < rank; i++) {
                    currentSizeStr.append(" x 1");
                }

                currentSizeField.setText(currentSizeStr.toString());

                String currentStr = currentSizeField.getText();
                int idx           = currentStr.lastIndexOf('x');
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
            public void widgetSelected(SelectionEvent e)
            {
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
            public void widgetSelected(SelectionEvent e)
            {
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
            public void widgetSelected(SelectionEvent e)
            {
                new HelpDialog(shell).open();
            }
        });

        shell.pack();

        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (curFont != null)
                    curFont.dispose();
            }
        });

        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Rectangle parentBounds = parent.getBounds();
        Point shellSize        = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

        shell.open();

        Display display = shell.getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
    }

    /** Check if the dim size is valid. */
    private void checkDimSize()
    {
        String dimStr         = currentSizeField.getText();
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
    private boolean createAttribute()
    {
        String attrName = null;
        int rank        = -1;
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

        rank               = rankChoice.getSelectionIndex() + 1;
        StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create",
                            "Number of values in the current dimension size is less than " + rank);
            return false;
        }

        long lsize   = 1; // The total size
        long l       = 0;
        dims         = new long[rank];
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
                attr = (Attribute) new H5CompoundAttr(parentObj, attrName, datatype, dims);
            else
                attr = (Attribute) new H5ScalarAttr(parentObj, attrName, datatype, dims);
            Object value = H5Datatype.allocateArray(datatype, (int)lsize);
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

        private static final String HELP_INFORMATION = """
                How to Create a New Attribute

                The following instructions explain how to create a new
                attribute. This dialog allows the creation of attributes that
                are numbers or strings, and 1D arrays of numbers.

                To create an attribute, it is necessary to define the name,
                type, and number of values or length of string. Then the
                value(s) can be entered.

                At each step, be sure to press <return> to make sure the value
                is accepted by the dialog.

                1) Attribute name
                The name of the attribute is a string. HDF accepts almost any
                characters in an attribute name.

                2) Datatype
                A list of predefined datatypes is given. These are the data
                types that can be created with this tool; you can only select a
                datatype from the list.

                The size specifies the size of a single data point in bits.

                3) Array length
                The length field is used to specify the length of the array or
                string. For numeric data, you can create an attribute with a
                single value or a one-dimension array of length values.

                For a string attribute, the length is the maximum length of the
                string.

                As a practical matter, attributes must be relatively small,
                perhaps a few kilobytes.

                4) Attribute value
                The value field is used to enter the initial value of the
                attribute. Be sure to press <return> after entering the
                value(s).

                Numeric data is interpreted according to the Datatype, using the
                number formats supported by Java. If the attribute is an array,
                values of the array must be separated by a comma, for example,
                12, 3, 4, 5.

                In the case of a string attribute, the entered text is stored as
                the string.

                Note that the dialog will accept more values than will fit. In
                this case, the excess values will not be written to the file,
                although they may remain visible in the dialog.
                """;

        HelpDialog(Shell parent) { super(parent, SWT.APPLICATION_MODAL); }

        public void open()
        {
            Shell parent = getParent();
            helpShell =
                new Shell(parent, SWT.TITLE | SWT.CLOSE | SWT.RESIZE | SWT.BORDER | SWT.APPLICATION_MODAL);
            helpShell.setFont(curFont);
            helpShell.setText("Create New Attribute");
            helpShell.setImages(ViewProperties.getHdfIcons());
            helpShell.setLayout(new GridLayout(1, true));

            // Render the help text with a native StyledText widget
            StyledText helpText = new StyledText(
                helpShell, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
            helpText.setFont(curFont);
            helpText.setText(HELP_INFORMATION);
            helpText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            Button okButton = new Button(helpShell, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    helpShell.dispose();
                }
            });

            helpShell.pack();

            helpShell.setSize(new Point(500, 500));

            Rectangle parentBounds = parent.getBounds();
            Point shellSize        = helpShell.getSize();
            helpShell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                                  (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            helpShell.open();

            Display display = parent.getDisplay();
            while (!helpShell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
        }
    }

    /**
     * Get the new attribute created.
     *
     * @return the new attribute created.
     */
    public Attribute getAttribute() { return (Attribute)newObject; }
}
