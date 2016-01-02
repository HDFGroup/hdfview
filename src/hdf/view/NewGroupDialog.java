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
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
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
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewGroupDialog shows a message dialog requesting user input for creating a new HDF4/5 group.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class NewGroupDialog extends Dialog {
	private static final long serialVersionUID = 7340860373483987075L;
	
	private Shell shell;

	private Text nameField;
	private Text compactField;
	private Text indexedField;
	
	private Combo parentChoice;
	private Combo orderFlags;
	
    private Button useCreationOrder;
    private Button setLinkStorage;
    private Button creationOrderHelpButton;
    private Button storageTypeHelpButton;
    private Button okButton;
    private Button cancelButton;
    private Button moreButton;
    
    private Composite moreOptionsComposite;
    private Composite creationOrderComposite;
    private Composite storageTypeComposite;
    
    private List<Group> groupList;
    private List<?> objList;

    private HObject newObject;
    private Group parentGroup;

    private FileFormat fileFormat;
    
    private int creationOrder;
    
    private boolean isH5;
    private boolean moreOptionsEnabled;
    
    /**
     * Constructs NewGroupDialog with specified list of possible parent groups.
     * 
     * @param owner
     *            the owner of the input
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    public NewGroupDialog(Shell parent, Group pGroup, List<?> objs) {
    	super(parent, SWT.APPLICATION_MODAL);
    	
    	newObject = null;
    	parentGroup = pGroup;
    	objList = objs;
    	
    	moreOptionsEnabled = false;
    	
    	fileFormat = pGroup.getFileFormat();
        isH5 = pGroup.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
    }
    
    public void open() {
    	Shell parent = getParent();
    	shell = new Shell(parent, SWT.TITLE | SWT.CLOSE |
    			SWT.RESIZE | SWT.BORDER | SWT.APPLICATION_MODAL);
    	shell.setText("New Group...");
    	GridLayout layout = new GridLayout(2, false);
    	layout.marginWidth = 5;
    	layout.marginHeight = 5;
    	shell.setLayout(layout);
    	
    	CLabel groupNameLabel = new CLabel(shell, SWT.LEFT);
    	groupNameLabel.setText("Group name:");
    	
    	nameField = new Text(shell, SWT.SINGLE | SWT.BORDER);
    	nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	CLabel parentGroupLabel = new CLabel(shell, SWT.LEFT);
    	parentGroupLabel.setText("Parent group:");
    	parentGroupLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    	
    	parentChoice = new Combo(shell, SWT.DROP_DOWN | SWT.BORDER);
    	parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	parentChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			parentGroup = groupList.get(parentChoice.getSelectionIndex());
    		}
    	});
    	
    	groupList = new Vector<Group>();
        Object obj = null;
        Iterator<?> iterator = objList.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                groupList.add((Group) obj);
                Group g = (Group) obj;
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
        	parentChoice.select(parentChoice.indexOf(parentGroup.getPath() + 
        			parentGroup.getName() + HObject.separator));
        }
        
        // Only add "More" button if file is H5 type
        if(isH5) {
        	moreOptionsComposite = new Composite(shell, SWT.NONE);
        	moreOptionsComposite.setLayout(new GridLayout(2, false));
        	moreOptionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        	
        	moreButton = new Button(moreOptionsComposite, SWT.PUSH);
        	moreButton.setText("More");
        	moreButton.addSelectionListener(new SelectionAdapter() {
        		public void widgetSelected(SelectionEvent e) {
        			moreOptionsEnabled = !moreOptionsEnabled;
        			
        			if(moreOptionsEnabled) {
        				addMoreOptions();
        			} else {
        				removeMoreOptions();
        			}
        		}
        	});
        	moreButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
        	
        	new Composite(moreOptionsComposite, SWT.NONE);
        }
        
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
        rowLayout.fill = true;
        rowLayout.justify = true;
        buttonComposite.setLayout(rowLayout);
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        buttonComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        
        okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setText("Ok");
        okButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		newObject = create();
                if (newObject != null) {
                    shell.dispose();
                }
        	}
        });
        //okButton.setMnemonic(KeyEvent.VK_O);
        okButton.setLayoutData(new RowData(70, SWT.DEFAULT));
        
        cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		newObject = null;
                shell.dispose();
        	}
        });
        //cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.setLayoutData(new RowData(70, SWT.DEFAULT));

        shell.pack();
        
        int w = 400 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 150 + (ViewProperties.getFontSize() - 12) * 10;
        shell.setMinimumSize(w, h);
        
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
    
    private HObject create() {
    	String name = null;
        Group pgroup = null;
        int gcpl = 0;

        name = nameField.getText();
        if (name == null) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ERROR);
            error.setText(shell.getText());
            error.setMessage("Group name is not specified.");
            error.open();
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ERROR);
            error.setText(shell.getText());
            error.setMessage("Group name cannot contain path.");
            error.open();
            return null;
        }
        
        pgroup = groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ERROR);
            error.setText(shell.getText());
            error.setMessage("Parent group is null.");
            error.open();
            return null;
        }

        Group obj = null;
        
        if (orderFlags != null && orderFlags.isEnabled()) {
            String order = (String) orderFlags.getItem(orderFlags.getSelectionIndex());
            if (order.equals("Tracked"))
                creationOrder = Group.CRT_ORDER_TRACKED;
            else if (order.equals("Tracked+Indexed"))
                creationOrder = Group.CRT_ORDER_INDEXED;
        }
        else
            creationOrder = 0;

        if ((orderFlags != null) && ((orderFlags.isEnabled()) || (setLinkStorage.getSelection()))) {
            int maxCompact = Integer.parseInt(compactField.getText());
            int minDense = Integer.parseInt(indexedField.getText());

            if ((maxCompact <= 0) || (maxCompact > 65536) || (minDense > 65536)) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ERROR);
                error.setText(shell.getText());
                error.setMessage("Max Compact and Min Indexed should be > 0 and < 65536.");
                error.open();
                return null;
            }

            if (maxCompact < minDense) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ERROR);
                error.setText(shell.getText());
                error.setMessage("Min Indexed should be <= Max Compact");
                error.open();
                return null;
            }

            try {
                gcpl = fileFormat.createGcpl(creationOrder, maxCompact, minDense);
            }
            catch (Exception ex) {
            	ex.printStackTrace();
            }
        }

        try {
            if (isH5)
                obj = fileFormat.createGroup(name, pgroup, 0, gcpl);
            else
                obj = fileFormat.createGroup(name, pgroup);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ERROR);
            error.setText(shell.getText());
            error.setMessage(ex.getMessage());
            error.open();
            return null;
        }

        return obj;
    }
    
    private void addMoreOptions() {
    	moreButton.setText("Less");
        
        creationOrderHelpButton = new Button(moreOptionsComposite, SWT.PUSH);
        creationOrderHelpButton.setImage(ViewProperties.getHelpIcon());
        creationOrderHelpButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
        creationOrderHelpButton.setToolTipText("Help on Creation Order");
        creationOrderHelpButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		final String msg = "Use Creation Order allows the user to set the creation order \n"
                        + "of links in a group, so that tracking, indexing, and iterating over links\n"
                        + "in groups can be possible. \n\n"
                        + "If the order flag Tracked is selected, links in a group can now \n"
                        + "be explicitly tracked by the order that they were created. \n\n"
                        + "If the order flag Tracked+Indexed is selected, links in a group can \n"
                        + "now be explicitly tracked and indexed in the order that they were created. \n\n"
                        + "The default order in which links in a group are listed is alphanumeric-by-name. \n\n\n";
                
        		MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION);
        		info.setText(shell.getText());
        		info.setMessage(msg);
        		info.open();
        	}
        });
        
        creationOrderComposite = new Composite(moreOptionsComposite, SWT.BORDER);
        creationOrderComposite.setLayout(new GridLayout(3, true));
        creationOrderComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        useCreationOrder = new Button(creationOrderComposite, SWT.CHECK);
        useCreationOrder.setText("Use Creation Order");
        useCreationOrder.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		boolean isOrder = useCreationOrder.getSelection();

                if (isOrder)
                    orderFlags.setEnabled(true);
                else
                    orderFlags.setEnabled(false);
        	}
        });
        
        Label label = new Label(creationOrderComposite, SWT.RIGHT);
        label.setText("Order Flags: ");
        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        orderFlags = new Combo(creationOrderComposite, SWT.DROP_DOWN);
        orderFlags.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        orderFlags.add("Tracked");
        orderFlags.add("Tracked+Indexed");
        orderFlags.select(orderFlags.indexOf("Tracked"));
        orderFlags.setEnabled(false);
        
        
        storageTypeHelpButton = new Button(moreOptionsComposite, SWT.PUSH);
        storageTypeHelpButton.setImage(ViewProperties.getHelpIcon());
        storageTypeHelpButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
        storageTypeHelpButton.setToolTipText("Help on set Link Storage");
        storageTypeHelpButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		final String msg = "Set Link Storage allows the users to explicitly set the storage  \n"
                        + "type of a group to be Compact or Indexed. \n\n"
                        + "Compact Storage: For groups with only a few links, compact link storage\n"
                        + "allows groups containing only a few links to take up much less space \n" + "in the file. \n\n"
                        + "Indexed Storage: For groups with large number of links, indexed link storage  \n"
                        + "provides a faster and more scalable method for storing and working with  \n"
                        + "large groups containing many links. \n\n"
                        + "The threshold for switching between the compact and indexed storage   \n"
                        + "formats is either set to default values or can be set by the user. \n\n"
                        + "<html><b>Max Compact</b></html> \n"
                        + "Max Compact is the maximum number of links to store in the group in a  \n"
                        + "compact format, before converting the group to the Indexed format. Groups \n"
                        + "that are in compact format and in which the number of links rises above \n"
                        + " this threshold are automatically converted to indexed format. \n\n"
                        + "<html><b>Min Indexed</b></html> \n"
                        + "Min Indexed is the minimum number of links to store in the Indexed format.   \n"
                        + "Groups which are in indexed format and in which the number of links falls    \n"
                        + "below this threshold are automatically converted to compact format. \n\n\n";
                
        		MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION);
        		info.setText(shell.getText());
        		info.setMessage(msg);
        		info.open();
        	}
        });
        
        storageTypeComposite = new Composite(moreOptionsComposite, SWT.BORDER);
        storageTypeComposite.setLayout(new GridLayout(2, true));
        storageTypeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        setLinkStorage = new Button(storageTypeComposite, SWT.CHECK);
        setLinkStorage.setText("Set Link Storage");
        setLinkStorage.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
                if (setLinkStorage.getSelection()) {
                    compactField.setEnabled(true);
                    indexedField.setEnabled(true);
                }
                else {
                    compactField.setText("8");
                    compactField.setEnabled(false);
                    indexedField.setText("6");
                    indexedField.setEnabled(false);
                }
        	}
        });
        
        Composite indexedComposite = new Composite(storageTypeComposite, SWT.NONE);
        indexedComposite.setLayout(new GridLayout(2, true));
        indexedComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        Label minLabel = new Label(indexedComposite, SWT.LEFT);
        minLabel.setText("Min Indexed: ");
        
        Label maxLabel = new Label(indexedComposite, SWT.LEFT);
        maxLabel.setText("Max Compact: ");
        
        indexedField = new Text(indexedComposite, SWT.SINGLE | SWT.BORDER);
        indexedField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        indexedField.setText("6");
        indexedField.setTextLimit(5);
        indexedField.setEnabled(false);
        indexedField.addVerifyListener(new VerifyListener() {
        	public void verifyText(VerifyEvent e) {
        		String input = e.text;
        		
        		char[] chars = new char[input.length()];
        	    input.getChars(0, chars.length, chars, 0);
        	    for (int i = 0; i < chars.length; i++) {
        	       if (!('0' <= chars[i] && chars[i] <= '9')) {
        	          e.doit = false;
        	          return;
        	       }
        	    }
        	}
        });
        
        compactField = new Text(indexedComposite, SWT.SINGLE | SWT.BORDER);
        compactField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        compactField.setText("8");
        compactField.setTextLimit(5);
        compactField.setEnabled(false);
        compactField.addVerifyListener(new VerifyListener() {
        	public void verifyText(VerifyEvent e) {
        		String input = e.text;
        		
        		char[] chars = new char[input.length()];
        	    input.getChars(0, chars.length, chars, 0);
        	    for (int i = 0; i < chars.length; i++) {
        	       if (!('0' <= chars[i] && chars[i] <= '9')) {
        	          e.doit = false;
        	          return;
        	       }
        	    }
        	}
        });
        
        shell.pack();

        int w = 500 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 280 + (ViewProperties.getFontSize() - 12) * 10;
        shell.setMinimumSize(w, h);
    }
    
    private void removeMoreOptions() {
    	moreButton.setText("More");
    	
    	creationOrderHelpButton.dispose();
    	storageTypeHelpButton.dispose();
    	
    	creationOrderComposite.dispose();
    	storageTypeComposite.dispose();
    	
    	shell.pack();
    	
    	int w = 400 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 150 + (ViewProperties.getFontSize() - 12) * 10;
    	shell.setMinimumSize(w, h);
    }
    
    /** Returns the new group created. */
    public DataFormat getObject() {
    	return newObject;
    }
    
    /** Returns the parent group of the new group. */
    public Group getParentGroup() {
        return parentGroup;
    }
}
