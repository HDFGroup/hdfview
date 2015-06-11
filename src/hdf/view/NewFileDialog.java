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
import org.eclipse.swt.widgets.*;

import hdf.object.FileFormat;

/**
 * NewFileDialog shows a message dialog requesting user input for creating a new
 * HDF4/5 file.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class NewFileDialog extends FileDialog
{
    private static final long serialVersionUID = 4796246032789504234L;

    /** Flag if the new file is an HDF5 */
    private String fileType;

    /** The current working directory */
    private String currentDir;

    /** The view working directory */
    private String viewDir;

    private boolean fileCreated;

    private List fileList;

    private final Shell viewer;

    private boolean isH5 = false;

    private boolean isH4 = false;

    /**
     * constructs an NewFileDialog.
     * 
     * @param owner
     *            The owner of the dialog.
     * @param dir
     *            The default directory of the new file.
     * @param type
     *            The type of file format.
     * @param openFiles
     *            The list of current open files. It is used to make sure the
     *            new file cannot be any file in use.
     */
    public NewFileDialog(Shell owner, String dir, String type, List openFiles) {
        super(owner, SWT.SAVE);

        currentDir = dir;
        viewer = owner;
        viewDir = dir;
        fileType = type;
        fileCreated = false;
        fileList = openFiles;
        
        if (currentDir != null) {
            currentDir += File.separator;
        }
        else {
            currentDir = "";
        }

        if (fileType == FileFormat.FILE_TYPE_HDF4) {
            isH4 = true;
            setFileName(Tools.checkNewFile(currentDir, ".hdf").getName());
            setFileFilter(DefaultFileFilter.getFileFilterHDF4());
        }
        else if (fileType == FileFormat.FILE_TYPE_HDF5) {
            isH5 = true;
            setFileName(Tools.checkNewFile(currentDir, ".h5").getName());
            setFileFilter(DefaultFileFilter.getFileFilterHDF5());
        }

        this.setAcceptAllFileFilterUsed(false);
        
        String filename = open();
        if (filename != null) {
        	fileCreated = createNewFile();
        } else {
        	fileCreated = false;
        }
    }

    /** create a new HDF file with default file creation properties */
    private boolean createNewFile() {
        File f = new File(getFileName());
        if (f == null) return false;

        String fname = f.getAbsolutePath();
        if (fname == null) return false;

        fname = fname.trim();
        if ((fname == null) || (fname.length() == 0)) {
            Display.getCurrent().beep();
            MessageBox error = new MessageBox(viewer, SWT.ICON_ERROR);
            error.setText(viewer.getText());
            error.setMessage("Invalid file name.");
            error.open();
            return false;
        }

        String extensions = FileFormat.getFileExtensions();
        boolean noExtension = true;
        if ((extensions != null) && (extensions.length() > 0)) {
            java.util.StringTokenizer currentExt = new java.util.StringTokenizer(extensions, ",");
            String extension = "";
            String tmpFilename = fname.toLowerCase();
            while (currentExt.hasMoreTokens() && noExtension) {
                extension = currentExt.nextToken().trim().toLowerCase();
                noExtension = !tmpFilename.endsWith("." + extension);
            }
        }

        if (noExtension) {
            if (isH4) {
                fname += ".hdf";
                f = new File(fname);
                //setSelectedFile(f);
            }
            else if (isH5) {
                fname += ".h5";
                f = new File(fname);
                //setSelectedFile(f);
            }
        }

        if (f.exists() && f.isDirectory()) {
            Display.getCurrent().beep();
            MessageBox error = new MessageBox(viewer, SWT.ICON_ERROR);
            error.setText(viewer.getText());
            error.setMessage("File is a directory.");
            error.open();
            return false;
        }

        File pfile = f.getParentFile();
        if (pfile == null) {
            fname = viewDir + File.separator + fname;
            f = new File(fname);
        }
        else if (!pfile.exists()) {
            Display.getCurrent().beep();
            MessageBox error = new MessageBox(viewer, SWT.ICON_ERROR);
            error.setText(viewer.getText());
            error.setMessage("File path does not exist at\n" + pfile.getPath());
            error.open();
            return false;
        }

        // check if the file is in use
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                    Display.getCurrent().beep();
                    MessageBox error = new MessageBox(viewer, SWT.ICON_ERROR);
                    error.setText(viewer.getText());
                    error.setMessage("Unable to create the new file. \nThe file is being used.");
                    error.open();
                    return false;
                }
            }
        }

        int newFileFlag = -1;
        if (f.exists()) {
        	MessageBox confirm = new MessageBox(viewer, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        	confirm.setText(viewer.getText());
        	confirm.setMessage("File exists. Do you want to replace it?");
        	newFileFlag = confirm.open();
            if (newFileFlag == SWT.NO) return false;
        }

        currentDir = f.getParent();
        try {
        	int aFlag = FileFormat.FILE_CREATE_DELETE;
        	if (ViewProperties.isEarlyLib())
        		aFlag = FileFormat.FILE_CREATE_DELETE | FileFormat.FILE_CREATE_EARLY_LIB;
            FileFormat.getFileFormat(fileType).createFile(fname, aFlag);
        }
        catch (Exception ex) {
            Display.getCurrent().beep();
            MessageBox error = new MessageBox(viewer, SWT.ICON_ERROR);
            error.setText(viewer.getText());
            error.setMessage(ex.getMessage());
            error.open();
            return false;
        }

        return true;
    }

    public boolean isFileCreated() {
        return fileCreated;
    }

    public String getAbsoluteFilePath() {
        String fname = null;
        File f = new File(getFileName());
        if (f != null) {
            fname = f.getAbsolutePath();
        }

        return fname;
    }
}
