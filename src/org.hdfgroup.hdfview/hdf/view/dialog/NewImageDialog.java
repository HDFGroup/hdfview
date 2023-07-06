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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * NewImageDialog shows a message dialog requesting user input for creating a
 * new HDF4/5 Image.
 *
 * @author Jordan T. Henderson
 * @version 2.4 1/1/2016
 */
public class NewImageDialog extends NewDataObjectDialog {

    private Text        nameField, widthField, heightField;

    private Combo       parentChoice;

    private Button      checkIndex, checkTrueColor, checkInterlacePixel,
                        checkInterlacePlane;

    /** A list of current groups */
    private List<Group> groupList;

    /**
     * Constructs a NewImageDialog with specified list of possible parent groups.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    public NewImageDialog(Shell parent, Group pGroup, List<?> objs) {
        super(parent, pGroup, objs);
    }

    /**
     * Open the NewImageDialog for adding a new image.
     */
    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New HDF Image...");
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setLayout(new GridLayout(1, true));


        // Create main content region
        Composite content = new Composite(shell, SWT.BORDER);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Image name: ");

        nameField = new Text(content, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        GridData fieldData = new GridData(SWT.FILL, SWT.FILL, true, false);
        fieldData.minimumWidth = 300;
        nameField.setLayoutData(fieldData);

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Parent Group: ");

        parentChoice = new Combo(content, SWT.DROP_DOWN | SWT.READ_ONLY);
        parentChoice.setFont(curFont);
        parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        parentChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                parentObj = groupList.get(parentChoice.getSelectionIndex());
            }
        });

        groupList = new ArrayList<>();
        Object obj = null;
        Iterator<?> iterator = objList.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(g);
                if (g.isRoot()) {
                    parentChoice.add(HObject.SEPARATOR);
                }
                else {
                    parentChoice.add(g.getPath() + g.getName()
                            + HObject.SEPARATOR);
                }
            }
        }

        if (((Group) parentObj).isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.SEPARATOR));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentObj.getPath() + parentObj.getName()
                    + HObject.SEPARATOR));
        }

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Height: ");

        heightField = new Text(content, SWT.SINGLE | SWT.BORDER);
        heightField.setFont(curFont);
        heightField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Width: ");

        widthField = new Text(content, SWT.SINGLE | SWT.BORDER);
        widthField.setFont(curFont);
        widthField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Image type: ");

        Composite typeComposite = new Composite(content, SWT.BORDER);
        typeComposite.setLayout(new GridLayout(2, true));
        typeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        checkIndex = new Button(typeComposite, SWT.RADIO);
        checkIndex.setFont(curFont);
        checkIndex.setText("Indexed colormap");
        checkIndex.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
        checkIndex.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkInterlacePixel.setSelection(true);
                checkInterlacePlane.setSelection(false);
                checkInterlacePixel.setEnabled(false);
                checkInterlacePlane.setEnabled(false);
            }
        });

        checkTrueColor = new Button(typeComposite, SWT.RADIO);
        checkTrueColor.setFont(curFont);
        checkTrueColor.setText("24-bit truecolor");
        checkTrueColor.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
        checkTrueColor.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkInterlacePixel.setEnabled(true);
                checkInterlacePlane.setEnabled(true);
            }
        });

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Data layout: ");

        Composite layoutComposite = new Composite(content, SWT.BORDER);
        layoutComposite.setLayout(new GridLayout(2, true));
        layoutComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        checkInterlacePixel = new Button(layoutComposite, SWT.RADIO);
        checkInterlacePixel.setFont(curFont);
        checkInterlacePixel.setText("Pixel interlace");
        checkInterlacePixel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));

        checkInterlacePlane = new Button(layoutComposite, SWT.RADIO);
        checkInterlacePlane.setFont(curFont);
        checkInterlacePlane.setText("Plane interlace");
        checkInterlacePlane.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));


        // Create Ok/Cancel button region
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("   &OK   ");
        okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newObject = createHDFimage();
                if (newObject != null) {
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
                ((Vector<Group>) groupList).setSize(0);
            }
        });

        checkIndex.setSelection(true);
        checkInterlacePixel.setSelection(true);
        checkInterlacePixel.setEnabled(false);
        checkInterlacePlane.setEnabled(false);

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

    private Dataset createHDFimage() {
        Dataset dataset = null;

        String name = nameField.getText();
        if (name != null) {
            name = name.trim();
        }
        if ((name == null) || (name.length() <= 0)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Image name is not specified.");
            return null;
        }

        if (name.indexOf(HObject.SEPARATOR) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Image name cannot contain path.");
            return null;
        }

        Group pgroup = groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Select a parent group.");
            return null;
        }

        int w = 0, h = 0;
        try {
            w = Integer.parseInt(widthField.getText());
            h = Integer.parseInt(heightField.getText());
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", ex.getMessage());
            return null;
        }

        long[] dims = null;
        int tclass = Datatype.CLASS_CHAR;
        int tsign = Datatype.SIGN_NONE;
        int tsize = 1;
        int torder = Datatype.NATIVE;
        int interlace = ScalarDS.INTERLACE_PIXEL;
        int ncomp = 2;

        if (checkIndex.getSelection()) {
            // indexed colormap
            if (isH5) {
                long[] tmpdims = { h, w };
                dims = tmpdims;
            }
            else {
                long[] tmpdims = { w, h };
                dims = tmpdims;
            }
        }
        else {
            // true color image
            if (isH5) {
                // HDF5 true color image
                if (checkInterlacePixel.getSelection()) {
                    long[] tmpdims = { h, w, 3 };
                    dims = tmpdims;
                }
                else {
                    interlace = ScalarDS.INTERLACE_PLANE;
                    long[] tmpdims = { 3, h, w };
                    dims = tmpdims;
                }
            }
            else {
                // HDF4 true color image
                ncomp = 3;
                long[] tmpdims = { w, h };
                dims = tmpdims;
                if (checkInterlacePlane.getSelection()) {
                    interlace = ScalarDS.INTERLACE_PLANE;
                }
            }
        }

        try {
            Datatype datatype = fileFormat.createDatatype(tclass, tsize, torder, tsign);
            dataset = fileFormat.createImage(name, pgroup, datatype, dims, dims, null, -1, ncomp, interlace, null);
            dataset.init();
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", ex.getMessage());
            return null;
        }

        return dataset;
    }
}
