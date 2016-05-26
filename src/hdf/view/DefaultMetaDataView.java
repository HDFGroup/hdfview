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

import java.io.File;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * DefaultMetadataView is a dialog window used to show data properties. Data
 * properties include attributes and general information such as object type,
 * data type and data space.
 * 
 * @author Jordan T. Henderson
 * @version 2.4 2/21/2016
 */
public class DefaultMetaDataView implements MetaDataView {
	private final Display display;
	private Shell shell;

	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultMetaDataView.class);

	private ViewManager viewer;
	
	/** The HDF data object */
	private HObject hObject;

	private Text attrContentArea;
	private Table attrTable; // table to hold a list of attributes
	private Label attrNumberLabel;
	private int numAttributes;
	private boolean isH5, isH4;
	private byte[] userBlock;
	private Text userBlockArea;
	private Button jamButton;

	private Text linkField = null;

	private FileFormat fileFormat;
	private String LinkTObjName;

	private int[] libver;

	public DefaultMetaDataView(Shell parent, ViewManager theView, HObject theObj) {
		log.trace("DefaultMetaDataView: start");

		shell = new Shell(parent, SWT.SHELL_TRIM);
		display = shell.getDisplay();
		
		shell.setData(this);
		
		shell.setImage(ViewProperties.getHdfIcon());
		shell.setLayout(new GridLayout(1, true));
		
		viewer = theView;

		hObject = theObj;
		fileFormat = hObject.getFileFormat();
		numAttributes = 0;
		userBlock = null;
		userBlockArea = null;
		libver = new int[2];

		isH5 = hObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
		isH4 = hObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

		if (hObject == null) {
			shell.dispose();
		} else if (hObject.getPath() == null) {
			shell.setText("Properties - " + hObject.getName());
		} else {
			shell.setText("Properties - " + hObject.getPath() + hObject.getName());
		}
		
		// Get the metadata information before adding GUI components */
        try {
            hObject.getMetadata();
        }
        catch (Exception ex) {
        	log.debug("Error retrieving metadata of object " + hObject.getName() + ":", ex);
        }
        
        if (isH5) {
            if (hObject.getLinkTargetObjName() != null) {
                LinkTObjName = hObject.getLinkTargetObjName();
            }
        }

		// Create main content
		TabFolder folder = new TabFolder(shell, SWT.TOP);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.setSelection(0);

		TabItem generalItem = new TabItem(folder, SWT.NONE);
		generalItem.setText("General");
		generalItem.setControl(createGeneralComposite(folder));

		TabItem attributeItem = new TabItem(folder, SWT.NONE);
		attributeItem.setText("Attributes");
		attributeItem.setControl(createAttributesComposite(folder));
		
		boolean isRoot = ((hObject instanceof Group) && ((Group) hObject).isRoot());
        if (isH5 && isRoot) {
            // Add panel to display user block
            TabItem userBlock = new TabItem(folder, SWT.NONE);
            userBlock.setText("User Block");
            userBlock.setControl(createUserBlockComposite(folder));
        }

		// Create Close button region
		Button closeButton = new Button(shell, SWT.PUSH);
		closeButton.setText("   &Close   ");
		closeButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		closeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (isH5 && linkField != null) checkLinkTargetChanged();
				
				shell.dispose();
			}
		});


		log.trace("DefaultMetaDataView: finish");

		shell.pack();
		
		Point minimumSize = shell.getParent().getSize();

		shell.setMinimumSize(minimumSize.x / 2, minimumSize.y / 2);
		shell.setSize(minimumSize.x / 2, minimumSize.y / 2);

		Rectangle parentBounds = parent.getBounds();
		Point shellSize = shell.getSize();
		shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
				(parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));


		viewer.addDataView(this);
		
		shell.open();
		
        // Workaround to prevent parent shell cursor from staying in "wait"
        // mode while TableView is open
        parent.setCursor(null);

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		
		viewer.removeDataView(this);
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
        	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
        	error.setText(shell.getText());
        	error.setMessage("No attribute is selected.");
        	error.open();
        	return null;
        }
        
        MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(shell.getText());
        confirm.setMessage("Do you want to delete the selected attribute?");
        if(confirm.open() == SWT.NO) {
        	return null;
        }

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
	
	private final void checkLinkTargetChanged() {
        Group pgroup = null;
        try {
            pgroup = (Group) hObject.getFileFormat().get(hObject.getPath());
        }
        catch (Exception ex) {
        	log.debug("parent group:", ex);
        }
        if (pgroup == null) {
        	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
        	error.setText(shell.getText());
        	error.setMessage("Parent group is null.");
        	error.open();
        	return;
        }

        String target_name = linkField.getText();
        if (target_name != null) target_name = target_name.trim();

        int linkType = Group.LINK_TYPE_SOFT;
        if (LinkTObjName.contains(FileFormat.FILE_OBJ_SEP))
            linkType = Group.LINK_TYPE_EXTERNAL;
        else if (target_name.equals("/")) // do not allow to link to the root
            return;

        // no change
        if (target_name.equals(hObject.getLinkTargetObjName())) return;

        // invalid name
        if (target_name == null || target_name.length() < 1) return;

        try {
            fileFormat.createLink(pgroup, hObject.getName(), target_name, linkType);
            hObject.setLinkTargetObjName(target_name);
        }
        catch (Exception ex) {
        	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
        	error.setText(shell.getText());
        	error.setMessage(ex.getMessage());
        	error.open();
        }
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
        	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
        	error.setText(shell.getText());
        	error.setMessage(ex.getMessage());
        	error.open();
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
            	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            	error.setText(shell.getText());
            	error.setMessage("More data values needed: " + newValue);
            	error.open();
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
                	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                	error.setText(shell.getText());
                	error.setMessage(ex.getMessage());
                	error.open();
                    return;
                }

                if (isUnsigned && (d < 0)) {
                	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                	error.setText(shell.getText());
                	error.setMessage("Negative value for unsigned integer: " + theToken);
                	error.open();
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
                        	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                        	error.setText(shell.getText());
                        	error.setMessage("Data is out of range[" + min + ", " + max
                                    + "]: " + theToken);
                        	error.open();
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
                        	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                        	error.setText(shell.getText());
                        	error.setMessage("Data is out of range[" + min + ", " + max
                                    + "]: " + theToken);
                        	error.open();
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
                        	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                        	error.setText(shell.getText());
                        	error.setMessage("Data is out of range[" + min + ", " + max
                                    + "]: " + theToken);
                        	error.open();
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
                                	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                                	error.setText(shell.getText());
                                	error.setMessage("Data is out of range[" + min + ", " + max
                                            + "]: " + theToken);
                                	error.open();
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
                            	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                            	error.setText(shell.getText());
                            	error.setMessage("Data is out of range[" + min + ", " + max
                                        + "]: " + theToken);
                            	error.open();
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
            	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            	error.setText(shell.getText());
            	error.setMessage(ex.getMessage());
            	error.open();
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
            	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            	error.setText(shell.getText());
            	error.setMessage(ex.getMessage());
            	error.open();
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
            	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            	error.setText(shell.getText());
            	error.setMessage(ex.getMessage());
            	error.open();
            }
        }
        else {
            log.trace("updateAttributeValue: hObject is not instanceof ScalarDS");
        }
    }
    
    private int showUserBlockAs(int radix) {
        int headerSize = 0;

        if (userBlock == null) {
            return 0;
        }

        String userBlockInfo = null;
        if ((radix == 2) || (radix == 8) || (radix == 16) || (radix == 10)) {
            StringBuffer sb = new StringBuffer();
            for (headerSize = 0; headerSize < userBlock.length; headerSize++) {
                int intValue = (int) userBlock[headerSize];
                if (intValue < 0) {
                    intValue += 256;
                }
                else if (intValue == 0) {
                    break; // null end
                }
                sb.append(Integer.toString(intValue, radix));
                sb.append(" ");
            }
            userBlockInfo = sb.toString();
        }
        else {
            userBlockInfo = new String(userBlock).trim();
            if (userBlockInfo != null) {
                headerSize = userBlockInfo.length();
            }
        }

        userBlockArea.setText(userBlockInfo);

        return headerSize;
    }
    
    private void writeUserBlock() {
        if (!isH5) {
            return;
        }

        int blkSize0 = 0;
        if (userBlock != null) {
            blkSize0 = userBlock.length;
            // The super block space is allocated by offset 0, 512, 1024, 2048, etc
            if (blkSize0 > 0) {
                int offset = 512;
                while (offset < blkSize0) {
                    offset *= 2;
                }
                blkSize0 = offset;
            }
        }

        int blkSize1 = 0;
        String userBlockStr = userBlockArea.getText();
        if (userBlockStr == null) {
            if (blkSize0 <= 0) {
                return; // nothing to write
            }
            else {
                userBlockStr = " "; // want to wipe out old userblock content
            }
        }
        byte buf[] = null;
        buf = userBlockStr.getBytes();

        blkSize1 = buf.length;
        if (blkSize1 <= blkSize0) {
            java.io.RandomAccessFile raf = null;
            try {
                raf = new java.io.RandomAccessFile(hObject.getFile(), "rw");
            }
            catch (Exception ex) {
            	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            	error.setText(shell.getText());
            	error.setMessage("Can't open output file: " + hObject.getFile());
            	error.open();
                return;
            }

            try {
                raf.seek(0);
                raf.write(buf, 0, buf.length);
                raf.seek(buf.length);
                if (blkSize0 > buf.length) {
                    byte[] padBuf = new byte[blkSize0 - buf.length];
                    raf.write(padBuf, 0, padBuf.length);
                }
            }
            catch (Exception ex) {
            	log.debug("raf write:", ex);
            }
            try {
                raf.close();
            }
            catch (Exception ex) {
            	log.debug("raf close:", ex);
            }

            MessageBox success = new MessageBox(shell, SWT.ICON_INFORMATION);
        	success.setText(shell.getText());
        	success.setMessage("Saving user block is successful.");
        	success.open();
        }
        else {
            // must rewrite the whole file
        	MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
        	confirm.setText(shell.getText());
        	confirm.setMessage("The user block to write is " + blkSize1 + " (bytes),\n"
                    + "which is larger than the user block space in file " + blkSize0 + " (bytes).\n"
                    + "To expand the user block, the file must be rewriten.\n\n"
                    + "Do you want to replace the current file? Click "
                    + "\n\"Yes\" to replace the current file," + "\n\"No\" to save to a different file, "
                    + "\n\"Cancel\" to quit without saving the change.\n\n ");

        	int op = confirm.open();
        	
        	if(op == SWT.CANCEL) {
        		return;
        	}

            String fin = hObject.getFile();

            String fout = fin + "~copy.h5";
            if (fin.endsWith(".h5")) {
                fout = fin.substring(0, fin.length() - 3) + "~copy.h5";
            }
            else if (fin.endsWith(".hdf5")) {
                fout = fin.substring(0, fin.length() - 5) + "~copy.h5";
            }

            File outFile = null;

            if (op == SWT.NO) {
            	FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
            	fChooser.setFileName(fout);
            	
            	DefaultFileFilter filter = DefaultFileFilter.getFileFilterHDF5();
            	fChooser.setFilterExtensions(new String[] {"*.*", filter.getExtensions()});
            	fChooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
            	fChooser.setFilterIndex(1);

            	if(fChooser.open() == null) {
            		return;
            	}

                File chosenFile = new File(fChooser.getFileName());

                outFile = chosenFile;
                fout = outFile.getAbsolutePath();
            }
            else {
                outFile = new File(fout);
            }

            if (!outFile.exists()) {
                try {
                    outFile.createNewFile();
                }
                catch (Exception ex) {
                	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                	error.setText(shell.getText());
                	error.setMessage("Failed to write user block into file.");
                	error.open();
                    return;
                }
            }

            // close the file
            TreeView view = ((HDFView) viewer).getTreeView();
            
            try {
                view.closeFile((view.getSelectedFile()));
            } catch (Exception ex) {
            	log.debug("Error closing file {}", fin);
            }

            if (Tools.setHDF5UserBlock(fin, fout, buf)) {
                if (op == SWT.NO) {
                    fin = fout; // open the new file
                }
                else {
                    File oldFile = new File(fin);
                    boolean status = oldFile.delete();
                    if (status) {
                        outFile.renameTo(oldFile);
                    }
                    else {
                    	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                    	error.setText(shell.getText());
                    	error.setMessage("Cannot replace the current file.\nPlease save to a different file.");
                    	error.open();
                        outFile.delete();
                    }
                }
            }
            else {
            	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            	error.setText(shell.getText());
            	error.setMessage("Failed to write user block into file.");
            	error.open();
                outFile.delete();
            }

            // reopen the file
            shell.dispose();
            
            try {
                view.openFile(fin, ViewProperties.isReadOnly() ? FileFormat.READ : FileFormat.WRITE);
            } catch (Exception ex) {
            	log.debug("Error opening file {}", fin);
            }
        }
    }
	
	private Composite createGeneralComposite(TabFolder folder) {
		boolean isRoot = ((hObject instanceof Group) && ((Group) hObject).isRoot());
		FileFormat theFile = hObject.getFileFormat();
		
		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		
		Composite detailComposite = new Composite(composite, SWT.BORDER);
		detailComposite.setLayout(new GridLayout(2, false));
		detailComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		String typeStr = "Unknown";
        String fileInfo = "";
		
		if(isRoot) {
			long size = 0;
            try {
                size = (new File(hObject.getFile())).length();
            }
            catch (Exception ex) {
                size = -1;
            }
            size /= 1024;
            
            int groupCount = 0, datasetCount = 0;
            
            HObject root = theFile.getRootObject();
            HObject theObj = null;
            Iterator<HObject> it = ((Group) root).depthFirstMemberList().iterator();
            
            while(it.hasNext()) {
            	theObj = it.next();
            	
            	if(theObj instanceof Group) {
            		groupCount++;
            	} else {
            		datasetCount++;
            	}
            }
            
            fileInfo = "size=" + size + "K,  groups=" + groupCount + ",  datasets=" + datasetCount;
			
			new Label(detailComposite, SWT.RIGHT).setText("File Name: ");
			
			new Label(detailComposite, SWT.RIGHT).setText(hObject.getName());
			
			new Label(detailComposite, SWT.RIGHT).setText("File Path: ");
			
			new Label(detailComposite, SWT.RIGHT).setText((new File(hObject.getFile())).getParent());
			
			new Label(detailComposite, SWT.RIGHT).setText("File Type: ");
			
			if (isH5) {
                typeStr = "HDF5,  " + fileInfo;
            }
            else if (isH4) {
                typeStr = "HDF4,  " + fileInfo;
            }
            else {
                typeStr = fileInfo;
            }
			
			new Label(detailComposite, SWT.RIGHT).setText(typeStr);
			
			if (isH5) {
                try {
                    libver = hObject.getFileFormat().getLibBounds();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                if (((libver[0] == 0) || (libver[0] == 1)) && (libver[1] == 1)) {
                    new Label(detailComposite, SWT.RIGHT).setText("Library version: ");
                }
                
                String libversion = null;
                if ((libver[0] == 0) && (libver[1] == 1))
                    libversion = "Earliest and Latest";
                else if ((libver[0] == 1) && (libver[1] == 1)) libversion = "Latest and Latest";
                else {
                	libversion = "";
                }
                
                new Label(detailComposite, SWT.RIGHT).setText(libversion);
            }
		}
		else {
			new Label(detailComposite, SWT.RIGHT).setText("Name: ");
			
			new Label(detailComposite, SWT.RIGHT).setText(hObject.getName());
			
			if(isH5) {
				if (hObject.getLinkTargetObjName() != null) {
					new Label(detailComposite, SWT.RIGHT).setText("Link To Target: ");
				}
			}
			
			new Label(detailComposite, SWT.RIGHT).setText("Path: ");
			
			new Label(detailComposite, SWT.RIGHT).setText(hObject.getPath());
			
			new Label(detailComposite, SWT.RIGHT).setText("Type: ");
			
			if(isH5) {
				if (hObject instanceof Group) {
	                typeStr = "HDF5 Group";
	            }
	            else if (hObject instanceof ScalarDS) {
	                typeStr = "HDF5 Scalar Dataset";
	            }
	            else if (hObject instanceof CompoundDS) {
	                typeStr = "HDF5 Compound Dataset";
	            }
	            else if (hObject instanceof Datatype) {
	                typeStr = "HDF5 Named Datatype";
	            }
			} else if(isH4) {
				if (hObject instanceof Group) {
	                typeStr = "HDF4 Group";
	            }
	            else if (hObject instanceof ScalarDS) {
	                ScalarDS ds = (ScalarDS) hObject;
	                if (ds.isImage()) {
	                    typeStr = "HDF4 Raster Image";
	                }
	                else {
	                    typeStr = "HDF4 SDS";
	                }
	            }
	            else if (hObject instanceof CompoundDS) {
	                typeStr = "HDF4 Vdata";
	            }
			} else {
				if (hObject instanceof Group) {
	                typeStr = "Group";
	            }
	            else if (hObject instanceof ScalarDS) {
	                typeStr = "Scalar Dataset";
	            }
	            else if (hObject instanceof CompoundDS) {
	                typeStr = "Compound Dataset";
	            }
			}
			
			new Label(detailComposite, SWT.RIGHT).setText(typeStr);
			
			// bug #926 to remove the OID, put it back on Nov. 20, 2008, --PC
            if (isH4) {
                new Label(detailComposite, SWT.RIGHT).setText("Tag, Ref:        ");
            }
            else {
                new Label(detailComposite, SWT.RIGHT).setText("Object Ref:       ");
            }
            
            // bug #926 to remove the OID, put it back on Nov. 20, 2008, --PC
	        String oidStr = null;
	        long[] OID = hObject.getOID();
	        if (OID != null) {
	            oidStr = String.valueOf(OID[0]);
	            for (int i = 1; i < OID.length; i++) {
	                oidStr += ", " + OID[i];
	            }
	        }
			
			if (!isRoot) {
	            new Label(detailComposite, SWT.RIGHT).setText(oidStr);
	        }
		}
		
		
		if (hObject instanceof Group) {
            createGroupInfoComposite(composite, (Group) hObject);
        }
        else if (hObject instanceof Dataset) {
            createDatasetInfoComposite(composite, (Dataset) hObject);
        }
        else if (hObject instanceof Datatype) {
            createNamedDatatypeInfoComposite(composite, (Datatype) hObject);
        }
		
		return composite;
	}
	
	/**
     * Creates a composite used to display HDF group information.
     */
	private Composite createGroupInfoComposite(Composite parent, Group g) {
		log.trace("createGroupInfoComposite: start");
		
		List<?> mlist = g.getMemberList();
        if (mlist == null) {
            return null;
        }

        int n = mlist.size();
        if (n <= 0) {
            return null;
        }
		
		org.eclipse.swt.widgets.Group groupInfoGroup = new org.eclipse.swt.widgets.Group(parent, SWT.NONE);
		groupInfoGroup.setText("Group Members");
		GridLayout layout = new GridLayout(1, true);
		layout.verticalSpacing = 10;
		groupInfoGroup.setLayout(layout);
		groupInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (g.getNumberOfMembersInFile() < ViewProperties.getMaxMembers()) {
            new Label(groupInfoGroup, SWT.RIGHT).setText("Number of members: " + n);
        }
        else {
            new Label(groupInfoGroup, SWT.RIGHT).setText("Number of members: " + n + " (in memory),"
            		+ "" + g.getNumberOfMembersInFile() + " (in file)");
        }
		
		String rowData[][] = new String[n][2];
        for (int i = 0; i < n; i++) {
            HObject theObj = (HObject) mlist.get(i);
            rowData[i][0] = theObj.getName();
            if (theObj instanceof Group) {
                rowData[i][1] = "Group";
            }
            else if (theObj instanceof Dataset) {
                rowData[i][1] = "Dataset";
            }
        }
        
        String[] columnNames = { "Name", "Type" };
        
        Table memberTable = new Table(groupInfoGroup, SWT.BORDER);
        memberTable.setLinesVisible(true);
        memberTable.setHeaderVisible(true);
        memberTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        for(int i = 0; i < columnNames.length; i++) {
        	TableColumn column = new TableColumn(memberTable, SWT.NONE);
        	column.setText(columnNames[i]);
        	column.setMoveable(false);
        }
        
        for(int i = 0; i < rowData.length; i++) {
        	TableItem item = new TableItem(memberTable, SWT.NONE);
        	item.setText(0, rowData[i][0]);
        	item.setText(1, rowData[i][1]);
        }
        
        for(int i = 0; i < columnNames.length; i++) {
        	memberTable.getColumn(i).pack();
        }
        
        // set cell height for large fonts
        //int cellRowHeight = Math.max(16, table.getFontMetrics(table.getFont()).getHeight());
        //table.setRowHeight(cellRowHeight);
		
		log.trace("createGroupInfoComposite: finish");
		
		return groupInfoGroup;
	}
	
	/**
     * Creates a composite used to display HDF dataset information.
     */
	private Composite createDatasetInfoComposite(Composite parent, Dataset d) {
		log.trace("createDatasetInfoComposite: start");
		
		if (d.getRank() <= 0) {
            d.init();
        }
		
		org.eclipse.swt.widgets.Group datasetInfoGroup = new org.eclipse.swt.widgets.Group(parent, SWT.NONE);
		datasetInfoGroup.setText("Dataspace and Datatype");
		GridLayout layout = new GridLayout(1, true);
		layout.verticalSpacing = 40;
		datasetInfoGroup.setLayout(layout);
		datasetInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		// Create composite for displaying dataset dimensions, dimension size,
		// max dimension size, and data type
		Composite dimensionComposite = new Composite(datasetInfoGroup, SWT.BORDER);
		dimensionComposite.setLayout(new GridLayout(2, false));
		dimensionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		new Label(dimensionComposite, SWT.RIGHT).setText("No. of Dimension(s): ");
		
		Text text = new Text(dimensionComposite, SWT.SINGLE | SWT.BORDER);
		text.setEditable(false);
		text.setText("" + d.getRank());
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		new Label(dimensionComposite, SWT.RIGHT).setText("Dimension Size(s): ");
		
		// Set Dimension Size
		String dimStr = null;
		String maxDimStr = null;
		long dims[] = d.getDims();
		long maxDims[] = d.getMaxDims();
		if (dims != null) {
			String[] dimNames = d.getDimNames();
			boolean hasDimNames = ((dimNames != null) && (dimNames.length == dims.length));
			StringBuffer sb = new StringBuffer();
			StringBuffer sb2 = new StringBuffer();

			sb.append(dims[0]);
			if (hasDimNames) {
				sb.append(" (");
				sb.append(dimNames[0]);
				sb.append(")");
			}

			if (maxDims[0] < 0)
				sb2.append("Unlimited");
			else
				sb2.append(maxDims[0]);

			for (int i = 1; i < dims.length; i++) {
				sb.append(" x ");
				sb.append(dims[i]);
				if (hasDimNames) {
					sb.append(" (");
					sb.append(dimNames[i]);
					sb.append(")");
				}

				sb2.append(" x ");
				if (maxDims[i] < 0)
					sb2.append("Unlimited");
				else
					sb2.append(maxDims[i]);

			}
			dimStr = sb.toString();
			maxDimStr = sb2.toString();
		}

		text = new Text(dimensionComposite, SWT.SINGLE | SWT.BORDER);
		text.setEditable(false);
		text.setText(dimStr);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		new Label(dimensionComposite, SWT.RIGHT).setText("Max Dimension Size(s): ");
		
		text = new Text(dimensionComposite, SWT.SINGLE | SWT.BORDER);
		text.setEditable(false);
		text.setText(maxDimStr);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		new Label(dimensionComposite, SWT.RIGHT).setText("Data Type: ");
		
		String typeStr = null;
        if (d instanceof ScalarDS) {
            ScalarDS sd = (ScalarDS) d;
            typeStr = sd.getDatatype().getDatatypeDescription();
        }
        else if (d instanceof CompoundDS) {
            if (isH4) {
                typeStr = "Vdata";
            }
            else {
                typeStr = "Compound";
            }
        }
        
        text = new Text(dimensionComposite, SWT.SINGLE | SWT.BORDER);
        text.setEditable(false);
        text.setText(typeStr);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        
        // Create composite for possible compound dataset info
        if (d instanceof CompoundDS) {
            CompoundDS compound = (CompoundDS) d;

            int n = compound.getMemberCount();
            if (n > 0) {
                String rowData[][] = new String[n][3];
                String names[] = compound.getMemberNames();
                Datatype types[] = compound.getMemberTypes();
                int orders[] = compound.getMemberOrders();

                for (int i = 0; i < n; i++) {
                    rowData[i][0] = names[i];
                    int mDims[] = compound.getMemberDims(i);
                    if (mDims == null) {
                        rowData[i][2] = String.valueOf(orders[i]);

                        if (isH4 && types[i].getDatatypeClass() == Datatype.CLASS_STRING) {
                            rowData[i][2] = String.valueOf(types[i].getDatatypeSize());
                        }
                    }
                    else {
                        String mStr = String.valueOf(mDims[0]);
                        int m = mDims.length;
                        for (int j = 1; j < m; j++) {
                            mStr += " x " + mDims[j];
                        }
                        rowData[i][2] = mStr;
                    }
                    rowData[i][1] = types[i].getDatatypeDescription();
                }

                String[] columnNames = { "Name", "Type", "Array Size" };
                
                Table memberTable = new Table(datasetInfoGroup, SWT.BORDER);
                memberTable.setLinesVisible(true);
                memberTable.setHeaderVisible(true);
                memberTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
                
                for(int i = 0; i < columnNames.length; i++) {
                	TableColumn column = new TableColumn(memberTable, SWT.NONE);
                	column.setText(columnNames[i]);
                	column.setMoveable(false);
                }
                
                for(int i = 0; i < rowData.length; i++) {
                	TableItem item = new TableItem(memberTable, SWT.NONE);
                	item.setText(0, rowData[i][0]);
                	item.setText(1, rowData[i][1]);
                	item.setText(2, rowData[i][2]);
                }
                
                for(int i = 0; i < columnNames.length; i++) {
                	memberTable.getColumn(i).pack();
                }
                
                // set cell height for large fonts
                //int cellRowHeight = Math.max(16, table.getFontMetrics(table.getFont()).getHeight());
                //table.setRowHeight(cellRowHeight);
            } // if (n > 0)
        } // if (d instanceof Compound)
		
		
		// Create composite for displaying dataset chunking, compression, filters,
		// storage type, and fill value
		Composite compressionComposite = new Composite(datasetInfoGroup, SWT.BORDER);
		compressionComposite.setLayout(new GridLayout(2, false));
		compressionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// Add compression and data layout information
        new Label(compressionComposite, SWT.RIGHT).setText("Chunking: ");
        
        // try { d.getMetadata(); } catch (Exception ex) {}
        String chunkInfo = "";
        long[] chunks = d.getChunkSize();
        if (chunks == null) {
            chunkInfo = "NONE";
        }
        else {
            int n = chunks.length;
            chunkInfo = String.valueOf(chunks[0]);
            for (int i = 1; i < n; i++) {
                chunkInfo += " X " + chunks[i];
            }
        }
        
        new Label(compressionComposite, SWT.RIGHT).setText(chunkInfo);
        
        new Label(compressionComposite, SWT.RIGHT).setText("Compression: ");
        
        new Label(compressionComposite, SWT.RIGHT).setText(d.getCompression());
        
        new Label(compressionComposite, SWT.RIGHT).setText("Filters: ");
        
        new Label(compressionComposite, SWT.RIGHT).setText(d.getFilters());
        
        new Label(compressionComposite, SWT.RIGHT).setText("Storage: ");
        
        new Label(compressionComposite, SWT.RIGHT).setText(d.getStorage());
        
        new Label(compressionComposite, SWT.RIGHT).setText("Fill value: ");
        
        Object fillValue = null;
        String fillValueInfo = "NONE";
        if (d instanceof ScalarDS) fillValue = ((ScalarDS) d).getFillValue();
        if (fillValue != null) {
            if (fillValue.getClass().isArray()) {
                int len = Array.getLength(fillValue);
                fillValueInfo = Array.get(fillValue, 0).toString();
                for (int i = 1; i < len; i++) {
                    fillValueInfo += ", ";
                    fillValueInfo += Array.get(fillValue, i).toString();
                }
            }
            else
                fillValueInfo = fillValue.toString();
        }
        
        new Label(compressionComposite, SWT.RIGHT).setText(fillValueInfo);
		
		log.trace("createDatasetInfoComposite: finish");
		
		return datasetInfoGroup;
	}
	
	private Composite createNamedDatatypeInfoComposite(Composite parent, Datatype t) {
		log.trace("createNamedDatatypeInfoComposite: start");
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Text infoArea = new Text(composite, SWT.MULTI);
		
		infoArea.setText(t.getDatatypeDescription());
		infoArea.setEditable(false);
		
		log.trace("createNamedDatatypeInfoComposite: finish");
		
		return composite;
	}
	
	private Composite createAttributesComposite(TabFolder folder) {
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
		
		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(5, false));
		
		attrNumberLabel = new Label(composite, SWT.RIGHT);
		attrNumberLabel.setText("Number of attributes = 0");
		
		// Add dummy labels
		Label dummyLabel = new Label(composite, SWT.RIGHT);
		dummyLabel.setText("");
		dummyLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button addButton = new Button(composite, SWT.PUSH);
		addButton.setText(" &Add ");
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
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 2));
        
        String[] columnNames = { "Name", "Value", "Type", "Array Size" };
        
        attrTable = new Table(sashForm, SWT.FULL_SELECTION | SWT.BORDER);
        attrTable.setLinesVisible(true);
        attrTable.setHeaderVisible(true);
        attrTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
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
        } else {
        	attrNumberLabel.setText("Number of attributes = " + numAttributes);
        	
        	Attribute attr = null;
            String name, type, size;
            for (int i = 0; i < numAttributes; i++) {
                attr = (Attribute) attrList.get(i);
                name = attr.getName();
                type = attr.getType().getDatatypeDescription();
                log.trace("createAttributesComposite:  attr[{}] is {} as {}", i, name, type);

                if (attr.isScalar()) {
                    size = "Scalar";
                }
                else {
                    long dims[] = attr.getDataDims();
                    size = String.valueOf(dims[0]);
                    for (int j = 1; j < dims.length; j++) {
                        size += " x " + dims[j];
                    }
                }
                
                TableItem item = new TableItem(attrTable, SWT.NONE);

                if (attr.getProperty("field") != null) {
                	String fieldInfo = " {Field: "+attr.getProperty("field")+"}";
                	item.setText(0, name + fieldInfo);
                } else {
                    item.setText(0, name);
                }
                
                item.setText(1, attr.toString(", "));
                item.setText(2, type);
                item.setText(3, size);
            } // for (int i=0; i<n; i++)
        }
        
        for(int i = 0; i < columnNames.length; i++) {
        	attrTable.getColumn(i).pack();
        }
        
        // set cell height for large fonts
        //int cellRowHeight = Math.max(16, attrTable.getFontMetrics(attrTable.getFont()).getHeight());
        //attrTable.setRowHeight(cellRowHeight);
        
        attrContentArea = new Text(sashForm, SWT.MULTI | SWT.BORDER);
        attrContentArea.setEditable(false);
        
        
        sashForm.setWeights(new int[] {1, 3});
        
        log.trace("createAttributesComposite: finish");
		
		return composite;
	}
	
	/**
     * Creates a panel used to display HDF5 user block.
     */
	private Composite createUserBlockComposite(TabFolder folder) {
		log.trace("createUserBlockComposite: start");
		
		userBlock = Tools.getHDF5UserBlock(hObject.getFile());
		
		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(5, false));
		
		new Label(composite, SWT.RIGHT).setText("Display As: ");
		
		String[] displayChoices = { "Text", "Binary", "Octal", "Hexadecimal", "Decimal" };
		
		Combo userBlockDisplayChoice = new Combo(composite, SWT.SINGLE | SWT.READ_ONLY);
		userBlockDisplayChoice.setItems(displayChoices);
		userBlockDisplayChoice.select(0);
		userBlockDisplayChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Combo source = (Combo) e.widget;
				int type = 0;
				
				String typeName = (String) source.getItem(source.getSelectionIndex());
				
	            jamButton.setEnabled(false);
	            userBlockArea.setEditable(false);

	            if (typeName.equalsIgnoreCase("Text")) {
	                type = 0;
	                jamButton.setEnabled(true);
	                userBlockArea.setEditable(true);
	            }
	            else if (typeName.equalsIgnoreCase("Binary")) {
	                type = 2;
	            }
	            else if (typeName.equalsIgnoreCase("Octal")) {
	                type = 8;
	            }
	            else if (typeName.equalsIgnoreCase("Hexadecimal")) {
	                type = 16;
	            }
	            else if (typeName.equalsIgnoreCase("Decimal")) {
	                type = 10;
	            }

	            showUserBlockAs(type);
			}
		});
		
		Label dummyLabel = new Label(composite, SWT.RIGHT);
		dummyLabel.setText("");
		dummyLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label sizeLabel = new Label(composite, SWT.RIGHT);
		sizeLabel.setText("Header Size (Bytes): 0");
        
        jamButton = new Button(composite, SWT.PUSH);
        jamButton.setText("Save User Block");
        jamButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        jamButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		writeUserBlock();
        	}
        });
        
        ScrolledComposite userBlockScroller = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.BORDER);
        userBlockScroller.setExpandHorizontal(true);
        userBlockScroller.setExpandVertical(true);
        userBlockScroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));
        
		userBlockArea = new Text(userBlockScroller, SWT.MULTI | SWT.WRAP);
		userBlockArea.setEditable(true);
		userBlockScroller.setContent(userBlockArea);
		
		int headSize = 0;
        if (userBlock != null) {
            headSize = showUserBlockAs(0);
            sizeLabel.setText("Header Size (Bytes): " + headSize);
        }
        else {
            userBlockDisplayChoice.setEnabled(false);
        }
		
		log.trace("createUserBlockComposite: finish");
		
		return composite;
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
