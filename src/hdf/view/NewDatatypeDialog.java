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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWT;
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

import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewDatatypeDialog shows a message dialog requesting user input for creating a
 * new HDF5 datatype.
 * 
 * @author Jordan T. Henderson
 * @version 2.4 1/1/2016
 */
public class NewDatatypeDialog extends Dialog {
	
	private Shell             shell;

	private Text              nameField, stringLengthField;

	private Combo             parentChoice, classChoice, sizeChoice, endianChoice;

	private Button            checkUnsigned;

	private boolean           isH5;

	/** a list of current groups */
	private List<Group>      groupList;
	
	private List<?>           objList;

	private HObject           newObject;
	private Group             parentGroup;

	private FileFormat        fileFormat;
	
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
		super(parent, SWT.APPLICATION_MODAL);
		
		newObject = null;
		parentGroup = pGroup;
		objList = objs;

        fileFormat = pGroup.getFileFormat();
        isH5 = pGroup.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
	}
	
	public void open() {
		Shell parent = getParent();
    	shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
    	shell.setText("New Datatype...");
    	shell.setImage(ViewProperties.getHdfIcon());
    	shell.setLayout(new GridLayout(1, false));
    	
    	
    	// Create Datatype name / Parent group region
    	Composite fieldComposite = new Composite(shell, SWT.NONE);
    	fieldComposite.setLayout(new GridLayout(2, false));
    	fieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	Label label = new Label(fieldComposite, SWT.LEFT);
    	label.setText("Datatype name: ");
    	
    	nameField = new Text(fieldComposite, SWT.SINGLE | SWT.BORDER);
    	nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	label = new Label(fieldComposite, SWT.LEFT);
    	label.setText("Parent Group: ");
    	
    	parentChoice = new Combo(fieldComposite, SWT.DROP_DOWN | SWT.BORDER);
    	parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	parentChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			parentGroup = groupList.get(parentChoice.getSelectionIndex());
    		}
    	});
    	
    	groupList = new Vector<Group>(objList.size());
        Object obj = null;
        Iterator<?> iterator = objList.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(g);
                if (g.isRoot()) {
                    parentChoice.add(HObject.separator);
                }
                else {
                    parentChoice.add(g.getPath() + g.getName() + HObject.separator);
                }
            }
        }
        
        if (parentGroup.isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.separator));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentGroup.getPath() + parentGroup.getName() + HObject.separator));
        }
    	
    	
    	// Create Datatype settings region
    	org.eclipse.swt.widgets.Group datatypeGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
    	datatypeGroup.setText("Datatype");
    	datatypeGroup.setLayout(new GridLayout(4, true));
    	datatypeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	label = new Label(datatypeGroup, SWT.LEFT);
    	label.setText("Datatype class");
    	
    	label = new Label(datatypeGroup, SWT.LEFT);
    	label.setText("Size (bits) ");
    	
    	label = new Label(datatypeGroup, SWT.LEFT);
    	label.setText("Byte ordering");
    	
    	checkUnsigned = new Button(datatypeGroup, SWT.CHECK);
    	checkUnsigned.setText("Unsigned");
    	checkUnsigned.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	classChoice = new Combo(datatypeGroup, SWT.DROP_DOWN);
    	classChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	classChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			int idx = classChoice.getSelectionIndex();
                sizeChoice.select(0);
                endianChoice.select(0);
                stringLengthField.setEnabled(false);

                if ((idx == 0) || (idx == 5)) {
                    sizeChoice.setEnabled(true);
                    endianChoice.setEnabled(isH5);
                    checkUnsigned.setEnabled(true);

                    if (sizeChoice.getItemCount() == 3) {
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
                    endianChoice.setEnabled(isH5);
                    checkUnsigned.setEnabled(false);

                    if (sizeChoice.getItemCount() == 5) {
                        sizeChoice.remove("16");
                        sizeChoice.remove("8");
                    }
                }
                else if (idx == 2) {
                    sizeChoice.setEnabled(false);
                    endianChoice.setEnabled(isH5);
                    checkUnsigned.setEnabled(true);
                }
                else if (idx == 3) {
                    sizeChoice.setEnabled(false);
                    endianChoice.setEnabled(false);
                    checkUnsigned.setEnabled(false);
                    stringLengthField.setEnabled(true);
                    stringLengthField.setText("String length");
                }
                else if (idx == 4) {
                    sizeChoice.setEnabled(false);
                    endianChoice.setEnabled(false);
                    checkUnsigned.setEnabled(false);
                    stringLengthField.setEnabled(false);
                }
                else if (idx == 7) {
                    sizeChoice.setEnabled(false);
                    endianChoice.setEnabled(false);
                    checkUnsigned.setEnabled(false);
                    stringLengthField.setEnabled(false);
                }
    		}
    	});
    	
    	classChoice.add("INTEGER");
        classChoice.add("FLOAT");
        classChoice.add("CHAR");
    	
    	if(isH5) {
    		classChoice.add("STRING");
            classChoice.add("REFERENCE");
            classChoice.add("VLEN_INTEGER");
            classChoice.add("VLEN_FLOAT");
            classChoice.add("VLEN_STRING");
    	}
    	
    	sizeChoice = new Combo(datatypeGroup, SWT.DROP_DOWN);
    	sizeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	sizeChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			if (classChoice.getSelectionIndex() == 0) {
                    checkUnsigned.setEnabled(true);
                }
    		}
    	});
    	
    	if(isH5) {
    		sizeChoice.add("NATIVE");
    	} else {
    		sizeChoice.add("DEFAULT");
    	}
    	
    	sizeChoice.add("8");
        sizeChoice.add("16");
        sizeChoice.add("32");
        sizeChoice.add("64");
    	
    	endianChoice = new Combo(datatypeGroup, SWT.DROP_DOWN);
    	endianChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	endianChoice.setEnabled(isH5);
    	
        if (isH5) {   
            endianChoice.add("NATIVE");
            endianChoice.add("LITTLE ENDIAN");
            endianChoice.add("BIG ENDIAN");
        }
        else {
            endianChoice.add("DEFAULT");
        }
        
        stringLengthField = new Text(datatypeGroup, SWT.SINGLE | SWT.BORDER);
        stringLengthField.setText("String length");
        stringLengthField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        stringLengthField.setEnabled(false);
    	
    	
    	// Create Ok/Cancel button region
    	Composite buttonComposite = new Composite(shell, SWT.NONE);
    	buttonComposite.setLayout(new GridLayout(2, true));
    	buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	Button okButton = new Button(buttonComposite, SWT.PUSH);
    	okButton.setText("   &Ok   ");
    	GridData gridData = new GridData(SWT.END, SWT.FILL, true, false);
    	gridData.widthHint = 70;
    	okButton.setLayoutData(gridData);
    	okButton.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			newObject = createDatatype();

                if (newObject != null) {
                    shell.dispose();
                }
    		}
    	});
        
        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("&Cancel");
        gridData = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
        gridData.widthHint = 70;
        cancelButton.setLayoutData(gridData);
        cancelButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		newObject = null;
                shell.dispose();
        	}
        });
        
        classChoice.select(0);
        sizeChoice.select(0);
        endianChoice.select(0);
    	
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
	
	private HObject createDatatype() {
        String name = null;
        Group pgroup = null;
        boolean isVLen = false;
        int tclass = -1, tsize = -1, torder = -1, tsign = -1;
        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Datatype name is not specified.");
            error.open();
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Datatype name cannot contain path.");
            error.open();
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Parent group is null.");
            error.open();
            return null;
        }

        // set datatype class
        int idx = classChoice.getSelectionIndex();
        if (idx == 0) {
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.getSelection()) {
                tsign = Datatype.SIGN_NONE;
            }
        }
        else if (idx == 1) {
            tclass = Datatype.CLASS_FLOAT;
        }
        else if (idx == 2) {
            tclass = Datatype.CLASS_CHAR;
            if (checkUnsigned.getSelection()) {
                tsign = Datatype.SIGN_NONE;
            }
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
        }
        else if (idx == 6) {;
            isVLen = true;
            tclass = Datatype.CLASS_FLOAT;
        }
        else if (idx == 7) {
            isVLen = true;
            tclass = Datatype.CLASS_STRING;
        }

        // set datatype size/order
        idx = sizeChoice.getSelectionIndex();
        if (tclass == Datatype.CLASS_STRING) {
            if (isVLen) {
                tsize = -1;
            }
            else {
                int stringLength = 0;
                try {
                    stringLength = Integer.parseInt(stringLengthField.getText());
                }
                catch (NumberFormatException ex) {
                    stringLength = -1;
                }
    
                if (stringLength <= 0) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                    error.setText(shell.getText());
                    error.setMessage("Invalid string length: " + stringLengthField.getText());
                    error.open();
                    return null;
                }
    
                tsize = stringLength;
            }        
        }
        else if (tclass == Datatype.CLASS_REFERENCE) {
            tsize = 1;
        }
        else if (idx == 0) {
            tsize = Datatype.NATIVE;
        }
        else if (tclass == Datatype.CLASS_FLOAT) {
            tsize = idx * 4;
        }
        else {
            tsize = 1 << (idx - 1);
        }

        if ((tsize == 8) && !isH5 && (tclass == Datatype.CLASS_INTEGER)) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("HDF4 does not support 64-bit integer.");
            error.open();
            return null;
        }

        // set order
        idx = endianChoice.getSelectionIndex();
        if (idx == 0) {
            torder = Datatype.NATIVE;
        }
        else if (idx == 1) {
            torder = Datatype.ORDER_LE;
        }
        else {
            torder = Datatype.ORDER_BE;
        }

        HObject obj = null;
        try {
            String fullPath = HObject.separator;
            if (pgroup.isRoot()) {
                fullPath += name;
            }
            else {
                fullPath = pgroup.getPath() + HObject.separator + pgroup.getName() + HObject.separator + name;
            }
            Datatype basedatatype = null;
            if (isVLen) {
                basedatatype = fileFormat.createDatatype(tclass, tsize, torder, tsign);
                tclass = Datatype.CLASS_VLEN;
            }
            Datatype datatype = fileFormat.createDatatype(tclass, tsize, torder, tsign, basedatatype, fullPath);
            obj = datatype;
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage(ex.getMessage());
            error.open();
            return null;
        }

        return obj;
    }

    /** Returns the new dataset created. */
    public DataFormat getObject() {
        return newObject;
    }

    /** Returns the parent group of the new dataset. */
    public Group getParentGroup() {
        return parentGroup;
    }
}
