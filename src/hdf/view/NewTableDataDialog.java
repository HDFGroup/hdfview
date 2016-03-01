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
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewTableDataDialog shows a message dialog requesting user input for creating
 * a new HDF4/5 compound dataset.
 * 
 * @author Jordan T. Henderson
 * @version 2.4 1/7/2015
 */
public class NewTableDataDialog extends Dialog {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewTableDataDialog.class);

	private Shell shell;
    
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

    private FileFormat            fileformat;

    private Combo                 parentChoice, nFieldBox, templateChoice;

    /** A list of current groups */
    private Vector<Group>         groupList;
    private Vector<Object>        compoundDSList;

    private HObject               newObject;
    private Group                 parentGroup;
    
    private List<?>               objList;

    private int                   numberOfMembers;

    private Table                 table;

    private Text                  nameField, currentSizeField, maxSizeField, chunkSizeField;
    
    private Combo                 compressionLevel, rankChoice, memberTypeChoice;
    private Button                checkCompression;
    private Button                checkContiguous, checkChunked;
	
    /**
     * Constructs a NewTableDataDialog with specified list of possible parent
     * groups.
     * 
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
	public NewTableDataDialog(Shell parent, Group pGroup, List<?> objs) {
		super(parent, SWT.APPLICATION_MODAL);
		
		newObject = null;
        numberOfMembers = 2;
        parentGroup = pGroup;
        objList = objs;
        
        groupList = new Vector<Group>(objs.size());
        compoundDSList = new Vector<Object>(objs.size());
        
        fileformat = pGroup.getFileFormat();
	}
	
	public void open() {
		Shell parent = getParent();
    	shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
    	shell.setText("New Compound Dataset...");
    	shell.setImage(ViewProperties.getHdfIcon());
    	shell.setLayout(new GridLayout(1, false));
    	
    	
    	// Create Name/Parent Group/Import field region
    	Composite fieldComposite = new Composite(shell, SWT.NONE);
    	GridLayout layout = new GridLayout(2, false);
    	layout.verticalSpacing = 0;
    	fieldComposite.setLayout(layout);
    	fieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	new Label(fieldComposite, SWT.LEFT).setText("Dataset name: ");
    	
    	nameField = new Text(fieldComposite, SWT.SINGLE | SWT.BORDER);
    	nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	new Label(fieldComposite, SWT.LEFT).setText("Parent group: ");
    	
    	parentChoice = new Combo(fieldComposite, SWT.DROP_DOWN);
    	parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	parentChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			parentGroup = groupList.get(parentChoice.getSelectionIndex());
    		}
    	});
    	
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
            else if (obj instanceof CompoundDS) {
                compoundDSList.add(obj);
            }
        }
        
    	if (parentGroup.isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.separator));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentGroup.getPath() + parentGroup.getName() + HObject.separator));
        }
    	
    	new Label(fieldComposite, SWT.LEFT).setText("Import template: ");
    	
    	templateChoice = new Combo(fieldComposite, SWT.DROP_DOWN);
    	templateChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	templateChoice.setItems(compoundDSList.toArray(new String[0]));
    	templateChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			
    		}
    	});
    	
    	
    	// Create Dataspace region
    	org.eclipse.swt.widgets.Group dataspaceGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
    	dataspaceGroup.setLayout(new GridLayout(3, true));
    	dataspaceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	dataspaceGroup.setText("Dataspace");
    	
    	new Label(dataspaceGroup, SWT.LEFT).setText("No. of dimensions");
    	
    	new Label(dataspaceGroup, SWT.LEFT).setText("Current size");
    	
    	new Label(dataspaceGroup, SWT.LEFT).setText("Max size (-1 for unlimited)");
    	
    	rankChoice = new Combo(dataspaceGroup, SWT.DROP_DOWN);
    	rankChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	rankChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			int rank = (int) rankChoice.getSelectionIndex() + 1;
                String currentSizeStr = "1";
                String maxSizeStr = "0";

                for (int i = 1; i < rank; i++) {
                    currentSizeStr += " x 1";
                    maxSizeStr += " x 0";
                }

                currentSizeField.setText(currentSizeStr);
                maxSizeField.setText(maxSizeStr);

                String currentStr = currentSizeField.getText();
                int idx = currentStr.lastIndexOf("x");
                String chunkStr = "1";

                if (rank <= 1) {
                    chunkStr = currentStr;
                }
                else {
                    for (int i = 1; i < rank - 1; i++) {
                        chunkStr += " x 1";
                    }
                    if (idx > 0) {
                        chunkStr += " x " + currentStr.substring(idx + 1);
                    }
                }

                chunkSizeField.setText(chunkStr);
    		}
    	});
    	
    	for (int i = 1; i < 33; i++) {
            rankChoice.add(String.valueOf(i));
        }
    	
    	currentSizeField = new Text(dataspaceGroup, SWT.SINGLE | SWT.BORDER);
    	currentSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	currentSizeField.setText("1");
    	
    	maxSizeField = new Text(dataspaceGroup, SWT.SINGLE | SWT.BORDER);
    	maxSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	maxSizeField.setText("0");
    	
    	
    	// Create Data Layout/Compression region
    	org.eclipse.swt.widgets.Group layoutGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
    	layoutGroup.setLayout(new GridLayout(7, false));
    	layoutGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	layoutGroup.setText("Data Layout and Compression");
    	
    	new Label(layoutGroup, SWT.LEFT).setText("Storage layout: ");
    	
    	checkContiguous = new Button(layoutGroup, SWT.RADIO);
    	checkContiguous.setText("Contiguous");
    	checkContiguous.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	checkContiguous.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			chunkSizeField.setEnabled(false);
    		}
    	});
    	
    	// Dummy labels
    	new Label(layoutGroup, SWT.LEFT).setText("");
    	new Label(layoutGroup, SWT.LEFT).setText("");
    	
    	checkChunked = new Button(layoutGroup, SWT.RADIO);
    	checkChunked.setText("Chunked");
    	checkChunked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	checkChunked.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			chunkSizeField.setEnabled(true);
                
    			String currentStr = currentSizeField.getText();
                int idx = currentStr.lastIndexOf("x");
                String chunkStr = "1";

                int rank = (int)rankChoice.getSelectionIndex() + 1;
                if (rank <= 1) {
                    chunkStr = currentStr;
                }
                else {
                    for (int i = 1; i < rank - 1; i++) {
                        chunkStr += " x 1";
                    }
                    if (idx > 0) {
                        chunkStr += " x " + currentStr.substring(idx + 1);
                    }
                }

                chunkSizeField.setText(chunkStr);
    		}
    	});
    	
    	new Label(layoutGroup, SWT.LEFT).setText("Size: ");
    	
    	chunkSizeField = new Text(layoutGroup, SWT.SINGLE | SWT.BORDER);
    	chunkSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	chunkSizeField.setText("1");
    	chunkSizeField.setEnabled(false);
    	
    	new Label(layoutGroup, SWT.LEFT).setText("Compression: ");
    	
    	checkCompression = new Button(layoutGroup, SWT.CHECK);
    	checkCompression.setText("gzip");
    	checkCompression.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	checkCompression.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			boolean isCompressed = checkCompression.getSelection();

                if (isCompressed) {
                    if (!checkChunked.getSelection()) {
                        String currentStr = currentSizeField.getText();
                        int idx = currentStr.lastIndexOf("x");
                        String chunkStr = "1";

                        int rank = (int) rankChoice.getSelectionIndex() + 1;
                        if (rank <= 1) {
                            chunkStr = currentStr;
                        }
                        else {
                            for (int i = 1; i < rank - 1; i++) {
                                chunkStr += " x 1";
                            }
                            if (idx > 0) {
                                chunkStr += " x " + currentStr.substring(idx + 1);
                            }
                        }

                        chunkSizeField.setText(chunkStr);
                    }
                    
                    compressionLevel.setEnabled(true);
                    checkContiguous.setEnabled(false);
                    checkContiguous.setSelection(false);
                    checkChunked.setSelection(true);
                    chunkSizeField.setEnabled(true);
                }
                else {
                    compressionLevel.setEnabled(false);
                    checkContiguous.setEnabled(true);
                }
    		}
    	});
    	
    	new Label(layoutGroup, SWT.LEFT).setText("Level: ");
    	
    	compressionLevel = new Combo(layoutGroup, SWT.DROP_DOWN);
    	compressionLevel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	compressionLevel.setEnabled(false);
    	
    	for (int i = 0; i < 10; i++) {
            compressionLevel.add(String.valueOf(i));
        }
    	
    	new Label(layoutGroup, SWT.LEFT).setText("");
    	new Label(layoutGroup, SWT.LEFT).setText("");
    	new Label(layoutGroup, SWT.LEFT).setText("");
    	
    	
    	// Create Properties region
    	org.eclipse.swt.widgets.Group propertiesGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
    	propertiesGroup.setLayout(new GridLayout(2, false));
    	propertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	propertiesGroup.setText("Compound Datatype Properties");
    	
    	new Label(propertiesGroup, SWT.LEFT).setText("Number of Members:");
    	
    	nFieldBox = new Combo(propertiesGroup, SWT.DROP_DOWN);
    	nFieldBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	//nFieldBox.setEditable(true);
    	nFieldBox.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			int n = 0;

                try {
                    n = Integer.valueOf((String) nFieldBox.getItem(nFieldBox.getSelectionIndex())).intValue();
                }
                catch (Exception ex) {
                	log.debug("Change number of members:", ex);
                }

                if (n == numberOfMembers) {
                    return;
                }
                
                if(n > numberOfMembers) {
                	for(int i = 0; i < n - numberOfMembers; i++) {
                		addMemberTableItem(table);
                	}
                } else {
                	for(int i = numberOfMembers - 1; i >= n; i--) {
                		TableItem item = table.getItem(i);
                		TableEditor editor = (TableEditor) item.getData("NameEditor");
                		editor.getEditor().dispose();
                		editor.dispose();
                		
                		editor = (TableEditor) item.getData("DatatypeEditor");
                		editor.getEditor().dispose();
                		editor.dispose();
                		
                		editor = (TableEditor) item.getData("ArraySizeEditor");
                		editor.getEditor().dispose();
                		editor.dispose();
                		
                		table.remove(table.indexOf(item));
                	}
                }
                
                table.setItemCount(n);
                numberOfMembers = n;
    		}
    	});
    	
    	for (int i = 1; i <= 100; i++) {
            nFieldBox.add(String.valueOf(i));
        }
    	
    	table = new Table(propertiesGroup, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    	table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    	table.setLinesVisible(false);
    	table.setHeaderVisible(true);
    	
    	String[] colNames = { "Name", "Datatype", "Array size / String length / Enum names" };
    	
    	TableColumn column = new TableColumn(table, SWT.NONE);
    	column.setText(colNames[0]);
    	
    	column = new TableColumn(table, SWT.NONE);
    	column.setText(colNames[1]);
    	
    	column = new TableColumn(table, SWT.NONE);
    	column.setText(colNames[2]);
    	
    	addMemberTableItem(table);
    	addMemberTableItem(table);
    	
    	for(int i = 0; i < table.getColumnCount(); i++) {
    		table.getColumn(i).pack();
    	}
    	
    	// Create Ok/Cancel button region
    	Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        
        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setText("   &Ok   ");
        GridData gridData = new GridData(SWT.END, SWT.FILL, true, false);
        gridData.widthHint = 70;
        okButton.setLayoutData(gridData);
        okButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		try {
                    newObject = createCompoundDS();
                }
                catch (Exception ex) {
                	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                	error.setText(shell.getText());
                	error.setMessage(ex.getMessage());
                	error.open();
                }

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
                (groupList).setSize(0);
        	}
        });
        
        templateChoice.deselectAll();
        rankChoice.select(0);
        checkContiguous.setSelection(true);
        compressionLevel.select(6);
        nFieldBox.select(nFieldBox.indexOf(String.valueOf(numberOfMembers)));
    	
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
	
	private HObject createCompoundDS() throws Exception {
        HObject obj = null;
        long dims[], maxdims[], chunks[];
        int rank;

        // stop editing the last selected cell
        //int row = table.getSelectedRow();
        //int col = table.getSelectedColumn();
        //if ((row >= 0) && (col > -0)) {
        //    TableCellEditor ed = table.getCellEditor(row, col);
        //    if (ed != null) {
        //        ed.stopCellEditing();
        //    }
        //}

        maxdims = chunks = null;
        String dname = nameField.getText();
        if ((dname == null) || (dname.length() <= 0)) {
            throw new IllegalArgumentException("Dataset name is empty");
        }

        Group pgroup = (Group) groupList.get(parentChoice.getSelectionIndex());
        if (pgroup == null) {
            throw new IllegalArgumentException("Invalid parent group");
        }

        int n = table.getItemCount();
        if (n <= 0) {
            return null;
        }

        String[] mNames = new String[n];
        Datatype[] mDatatypes = new Datatype[n];
        int[] mOrders = new int[n];

        for (int i = 0; i < n; i++) {
            String name = (String) table.getItem(i).getText(0);
            if ((name == null) || (name.length() <= 0)) {
                throw new IllegalArgumentException("Member name is empty");
            }
            mNames[i] = name;

            int order = 1;
            String orderStr = (String) table.getItem(i).getText(2);
            if (orderStr != null) {
                try {
                    order = Integer.parseInt(orderStr);
                }
                catch (Exception ex) {
                	log.debug("compound order:", ex);
                }
            }
            mOrders[i] = order;

            String typeName = (String) table.getItem(i).getText(1);
            Datatype type = null;
            if (DATATYPE_NAMES[0].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_INTEGER, 1, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[1].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_INTEGER, 2, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[2].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[3].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_INTEGER, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
            }
            else if (DATATYPE_NAMES[4].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_INTEGER, 2, Datatype.NATIVE, Datatype.SIGN_NONE);
            }
            else if (DATATYPE_NAMES[5].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.SIGN_NONE);
            }
            else if (DATATYPE_NAMES[6].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_INTEGER, 8, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[7].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[8].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[9].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_STRING, order, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[10].equals(typeName)) { // enum
                type = fileformat.createDatatype(Datatype.CLASS_ENUM, 4, Datatype.NATIVE, Datatype.NATIVE);
                if ((orderStr == null) || (orderStr.length() < 1) || orderStr.endsWith("...")) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText(shell.getText());
                    error.setMessage("Invalid member values: " + orderStr);
                    error.open();
                    return null;
                }
                else {
                    type.setEnumMembers(orderStr);
                }
            }
            else if (DATATYPE_NAMES[11].equals(typeName)) {
                type = fileformat.createDatatype(Datatype.CLASS_INTEGER, 8, Datatype.NATIVE, Datatype.SIGN_NONE);
            }
            else {
                throw new IllegalArgumentException("Invalid data type.");
            }
            mDatatypes[i] = type;
        } // for (int i=0; i<n; i++)

        rank = (int)rankChoice.getSelectionIndex() + 1;
        StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage("Number of values in the current dimension size is less than " + rank);
            error.open();
            return null;
        }

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
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                error.setText(shell.getText());
                error.setMessage("Invalid dimension size: " + currentSizeField.getText());
                error.open();
                return null;
            }

            if (l <= 0) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                error.setText(shell.getText());
                error.setMessage("Dimension size must be greater than zero.");
                error.open();
                return null;
            }

            dims[i] = l;
        }

        st = new StringTokenizer(maxSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage("Number of values in the max dimension size is less than " + rank);
            error.open();
            return null;
        }

        l = 0;
        maxdims = new long[rank];
        for (int i = 0; i < rank; i++) {
            token = st.nextToken().trim();
            try {
                l = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                error.setText(shell.getText());
                error.setMessage("Invalid max dimension size: " + maxSizeField.getText());
                error.open();
                return null;
            }

            if (l < -1) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                error.setText(shell.getText());
                error.setMessage("Dimension size cannot be less than -1.");
                error.open();
                return null;
            }
            else if (l == 0) {
                l = dims[i];
            }

            maxdims[i] = l;
        }

        chunks = null;
        if (checkChunked.getSelection()) {
            st = new StringTokenizer(chunkSizeField.getText(), "x");
            if (st.countTokens() < rank) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                error.setText(shell.getText());
                error.setMessage("Number of values in the chunk size is less than " + rank);
                error.open();
                return null;
            }

            l = 0;
            chunks = new long[rank];
            token = null;
            for (int i = 0; i < rank; i++) {
                token = st.nextToken().trim();
                try {
                    l = Long.parseLong(token);
                }
                catch (NumberFormatException ex) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText(shell.getText());
                    error.setMessage("Invalid chunk dimension size: " + chunkSizeField.getText());
                    error.open();
                    return null;
                }

                if (l < 1) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText(shell.getText());
                    error.setMessage("Chunk size cannot be less than 1.");
                    error.open();
                    return null;
                }

                chunks[i] = l;
            } // for (int i=0; i<rank; i++)

            long tchunksize = 1, tdimsize = 1;
            for (int i = 0; i < rank; i++) {
                tchunksize *= chunks[i];
                tdimsize *= dims[i];
            }

            if (tchunksize >= tdimsize) {
                shell.getDisplay().beep();
                MessageBox confirm = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
                confirm.setText(shell.getText());
                confirm.setMessage("Chunk size is equal/greater than the current size. "
                        + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() + "?");
                if(confirm.open() == SWT.NO) {
                	return null;
                }
            }

            if (tchunksize == 1) {
                shell.getDisplay().beep();
                MessageBox confirm = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
                confirm.setText(shell.getText());
                confirm.setMessage("Chunk size is one, which may cause large memory overhead for large dataset."
                        + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() + "?");
                if(confirm.open() == SWT.NO) {
                	return null;
                }
            }

        } // if (checkChunked.getSelection())

        int gzip = 0;
        if (checkCompression.getSelection()) {
            gzip = compressionLevel.getSelectionIndex();
        }

        if (checkChunked.getSelection()) {
            obj = fileformat.createCompoundDS(dname, pgroup, dims, maxdims, chunks, gzip, mNames, mDatatypes, mOrders,
                    null);
        }
        else {
            obj = fileformat
                    .createCompoundDS(dname, pgroup, dims, maxdims, null, -1, mNames, mDatatypes, mOrders, null);
        }

        return obj;
    }
	
	private TableItem addMemberTableItem(Table table) {
		TableItem item = new TableItem(table, SWT.NONE);
		
		TableEditor editor = new TableEditor(table);
		Text text = new Text(table, SWT.SINGLE | SWT.BORDER);
		editor.grabHorizontal = true;
		editor.grabVertical = true;
		editor.horizontalAlignment = SWT.LEFT;
		editor.verticalAlignment = SWT.TOP;
		editor.setEditor(text, item, 0);
		item.setData("NameEditor", editor);
		
		Combo combo = new Combo(table, SWT.DROP_DOWN);
		combo.setItems(DATATYPE_NAMES);
		editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.grabVertical = true;
		editor.horizontalAlignment = SWT.LEFT;
		editor.verticalAlignment = SWT.TOP;
		editor.setEditor(combo, item, 1);
		item.setData("DatatypeEditor", editor);
		
		text = new Text(table, SWT.SINGLE | SWT.BORDER);
		editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.grabVertical = true;
		editor.horizontalAlignment = SWT.LEFT;
		editor.verticalAlignment = SWT.TOP;
		editor.setEditor(text, item, 2);
		item.setData("ArraySizeEditor", editor);
		
		return item;
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
