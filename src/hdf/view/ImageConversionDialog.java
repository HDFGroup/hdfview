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
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.FileFormat;

/**
 * ImageConversionDialog shows a message dialog requesting user input for
 * converting files.
 * 
 * @author Jordan T. Henderson
 * @version 2.4 1/28/2016
 */
public class ImageConversionDialog extends Dialog {
	private Shell shell;
	
	private String fileTypeFrom, fileTypeTo;

    private Text srcFileField, dstFileField;

    private boolean isConverted;

    private boolean isConvertedFromImage;

    private String convertedFile;

    private String toFileExtension;

    private List<FileFormat> fileList;

    private String currentDir;

    /**
     * Constructs a FileConversionDialog
     * 
     * @param parent
     *            The parent shell of the dialog.
     * @param typeFrom
     *            source file type
     * @param typeTo
     *            destination file type
     * @param dir
     *            current file directory
     * @param openFiles
     *            The list of currently open files
     */
    public ImageConversionDialog(Shell parent, String typeFrom, String typeTo,
            String dir, List<FileFormat> openFiles) {
    	super(parent, SWT.APPLICATION_MODAL);
    	
    	fileTypeFrom = typeFrom;
        fileTypeTo = typeTo;
        isConverted = false;
        isConvertedFromImage = false;
        fileList = openFiles;
        toFileExtension = "";
        currentDir = dir;
    }
    
    public void open() {
    	Shell parent = getParent();
    	shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
    	shell.setText("New Dataset...");
    	shell.setImage(ViewProperties.getHdfIcon());
    	shell.setLayout(new GridLayout(1, true));
    	
    	if (fileTypeTo.equals(FileFormat.FILE_TYPE_HDF5)) {
            toFileExtension = ".h5";
            shell.setText("Convert Image to HDF5 ...");
            isConvertedFromImage = true;
        }
        else if (fileTypeTo.equals(FileFormat.FILE_TYPE_HDF4)) {
            toFileExtension = ".hdf";
            shell.setText("Convert Image to HDF4 ...");
            isConvertedFromImage = true;
        }
    	
    	
    	// Create content region
    	Composite contentComposite = new Composite(shell, SWT.NONE);
    	contentComposite.setLayout(new GridLayout(3, false));
    	contentComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	Label label = new Label(contentComposite, SWT.RIGHT);
    	label.setText("IMAGE File: ");
    	
    	srcFileField = new Text(contentComposite, SWT.SINGLE | SWT.BORDER);
    	GridData fieldData = new GridData(SWT.FILL, SWT.FILL, true, false);
    	fieldData.minimumWidth = 350;
    	srcFileField.setLayoutData(fieldData);
    	
    	Button browseButton = new Button(contentComposite, SWT.PUSH);
    	browseButton.setText("Browse...");
    	browseButton.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
    			fChooser.setFilterPath(currentDir);
    			
    			if(isConvertedFromImage)
//    			    fChooser.setFileFilter(DefaultFileFilter.getImageFileFilter());
    				
    			if(fChooser.open() == null) {
    				return;
    			}

                File chosenFile = new File(fChooser.getFilterPath() + File.separator + fChooser.getFileName());
                if (chosenFile == null) {
                    return;
                }

                currentDir = chosenFile.getParent();
                srcFileField.setText(chosenFile.getAbsolutePath());
                dstFileField.setText(chosenFile.getAbsolutePath() + toFileExtension);
    		}
    	});
    	
    	label = new Label(contentComposite, SWT.RIGHT);
    	label.setText("HDF File: ");
    	
    	dstFileField = new Text(contentComposite, SWT.SINGLE | SWT.BORDER);
    	dstFileField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	browseButton = new Button(contentComposite, SWT.PUSH);
    	browseButton.setText("Browse...");
    	browseButton.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
    			
    			if(fChooser.open() == null) {
    				return;
    			}
    			
    			File chosenFile = new File(fChooser.getFilterPath() + File.separator + fChooser.getFileName());
                if (chosenFile == null) {
                    return;
                }

                dstFileField.setText(chosenFile.getAbsolutePath());
    		}
    	});
    	
    	// Dummy label to fill space as dialog is resized
    	new Label(shell, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	
    	// Create Ok/Cancel button
    	Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        
        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setText("   &Ok   ");
        okButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		isConverted = convert();

                if (isConverted) {
                    shell.dispose();
                }
        	}
        });
        GridData gridData = new GridData(SWT.END, SWT.FILL, true, false);
        gridData.widthHint = 70;
        okButton.setLayoutData(gridData);
        
        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("&Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		isConverted = false;
                convertedFile = null;
                shell.dispose();
        	}
        });
        
        gridData = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
        gridData.widthHint = 70;
        cancelButton.setLayoutData(gridData);
    	
    	
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
    
    /** Convert file */
    private boolean convert() {
        boolean converted = false;
        String srcFile = srcFileField.getText();
        String dstFile = dstFileField.getText();

        if ((srcFile == null) || (dstFile == null)) {
            return false;
        }

        srcFile = srcFile.trim();
        dstFile = dstFile.trim();
        if ((srcFile == null) || (srcFile.length() <= 0) || (dstFile == null)
                || (dstFile.length() <= 0)) {
            return false;
        }

        // verify the source file
        File f = new File(srcFile);
        if (!f.exists()) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage("Source file does not exist.");
            error.open();
            return false;
        }
        else if (f.isDirectory()) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage("Source file is a directory.");
            error.open();
            return false;
        }

        // verify target file
        String srcPath = f.getParent();
        f = new File(dstFile);
        File pfile = f.getParentFile();
        if (pfile == null) {
            dstFile = srcPath + File.separator + dstFile;
            f = new File(dstFile);
        }
        else if (!pfile.exists()) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage("Destination file path does not exist at\n"
                    + pfile.getPath());
            error.open();
            return false;
        }

        // check if the file is in use
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator<FileFormat> iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(dstFile)) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ERROR | SWT.OK);
                    error.setText(shell.getText());
                    error.setMessage("The destination file is being used.");
                    error.open();
                    return false;
                }
            }
        }

        if (f.exists()) {
        	MessageBox confirm = new MessageBox(shell, SWT.YES | SWT.NO);
        	confirm.setText(shell.getText());
        	confirm.setMessage("Destination file exists. Do you want to replace it ?");
        	if(confirm.open() == SWT.NO) {
        		return false;
        	}
        }

        try {
            Tools.convertImageToHDF(srcFile, dstFile, fileTypeFrom, fileTypeTo);
            convertedFile = dstFile;
            converted = true;
        }
        catch (Exception ex) {
            convertedFile = null;
            converted = false;
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ERROR | SWT.OK);
            error.setText(shell.getText());
            error.setMessage(ex.getMessage());
            error.open();
            return false;
        }

        return converted;
    }

    public boolean isFileConverted() {
        return isConverted;
    }

    public String getConvertedFile() {
        return convertedFile;
    }
}
