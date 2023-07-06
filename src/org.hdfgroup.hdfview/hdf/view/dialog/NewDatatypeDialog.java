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
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * NewDatatypeDialog shows a message dialog requesting user input for creating a
 * new HDF5 datatype.
 *
 * @author Jordan T. Henderson
 * @version 2.4 1/1/2016
 */
public class NewDatatypeDialog extends NewDataObjectDialog {

    private Combo             parentChoice;

    /** TextField for entering the name of the object */
    protected Text            nameField;

    /** a list of current groups */
    private List<Group>       groupList;

    /**
     * Constructs a NewDatatypeDialog with specified list of possible parent
     * groups.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    public NewDatatypeDialog(Shell parent, Group pGroup, List<?> objs) {
        super(parent, pGroup, objs);
    }

    /**
     * Open the NewDatatypeDialog for adding a new datatype.
     */
    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Datatype...");
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setLayout(new GridLayout(1, false));


        // Create Datatype name / Parent group region
        Composite fieldComposite = new Composite(shell, SWT.NONE);
        fieldComposite.setLayout(new GridLayout(2, false));
        fieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label label = new Label(fieldComposite, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Datatype name: ");

        nameField = new Text(fieldComposite, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        nameField.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                }
            }
        });

        label = new Label(fieldComposite, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Parent Group: ");

        parentChoice = new Combo(fieldComposite, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        parentChoice.setFont(curFont);
        parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        parentChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                parentObj = groupList.get(parentChoice.getSelectionIndex());
            }
        });

        groupList = new Vector<>(objList.size());
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
                    parentChoice.add(g.getPath() + g.getName() + HObject.SEPARATOR);
                }
            }
        }

        if (((Group) parentObj).isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.SEPARATOR));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentObj.getPath() + parentObj.getName() + HObject.SEPARATOR));
        }

        // Create Datatype settings region
        createDatatypeWidget();

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
                newObject = createNewDatatype();

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
            }
        });

        //Control[] controls = new Control[] { okButton, cancelButton };
        //shell.setTabList(controls);
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

    /**
     * Create the datatype specified by the settings.
     *
     * @return the new object created.
     */
    public Datatype createNewDatatype() {
        String name = null;
        Group pgroup = null;

        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Datatype name is not specified.");
            return null;
        }

        if (name.indexOf(HObject.SEPARATOR) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Datatype name cannot contain path.");
            return null;
        }

        pgroup = groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Parent group is null.");
            return null;
        }

        Datatype datatype = null;
        try {
            String fullPath = HObject.SEPARATOR;
            if (pgroup.isRoot()) {
                fullPath += name;
            }
            else {
                fullPath = pgroup.getPath() + HObject.SEPARATOR + pgroup.getName() + HObject.SEPARATOR + name;
            }
            datatype = super.createNewDatatype(fullPath);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", ex.getMessage());
            return null;
        }

        return datatype;
    }
}
