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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.hdf5lib.H5;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.view.DefaultFileFilter;
import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * NewLinkDialog shows a message dialog requesting user input for creating
 * new links.
 *
 * @author Jordan T. Henderson
 * @version 2.4 1/1/2016
 */
public class NewLinkDialog extends Dialog {

    private static final Logger log = LoggerFactory.getLogger(NewLinkDialog.class);

    private Shell         shell;

    private Font          curFont;

    private Text          nameField;

    private Combo         parentChoice;

    private CCombo        targetObject;

    private String        currentDir;

    private Text          targetFile;

    private Button        targetFileButton;

    private Button        hardLink, softLink, externalLink;

    /** a list of current groups */
    private List<Group>   groupList;

    /** a list of current objects */
    private List<?>       objList;

    private HObject       newObject;
    private Group         parentGroup;

    private FileFormat    fileFormat;

    private final List<?> fileList;

    /**
     * Constructs a NewLinkDialog with specified list of possible parent groups.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     * @param files
     *            the list of all files open in the TreeView
     */
    public NewLinkDialog(Shell parent, Group pGroup, List<?> objs, List<FileFormat> files) {
        super(parent, SWT.APPLICATION_MODAL);

        try {
            curFont = new Font(
                    Display.getCurrent(),
                    ViewProperties.getFontType(),
                    ViewProperties.getFontSize(),
                    SWT.NORMAL);
        }
        catch (Exception ex) {
            curFont = null;
        }

        newObject = null;
        parentGroup = pGroup;
        objList = objs;

        fileFormat = pGroup.getFileFormat();
        currentDir = ViewProperties.getWorkDir();
        fileList = files;
    }

    /**
     * Open the NewLinkDialog for adding a new link.
     */
    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Link...");
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setLayout(new GridLayout(1, true));

        // Create the main content region
        Composite content = new Composite(shell, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Link name: ");

        nameField = new Text(content, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        GridData fieldData = new GridData(SWT.FILL, SWT.FILL, true, false);
        fieldData.minimumWidth = 300;
        nameField.setLayoutData(fieldData);

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Parent group: ");

        parentChoice = new Combo(content, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        parentChoice.setFont(curFont);
        parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        parentChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                parentGroup = groupList.get(parentChoice.getSelectionIndex());
            }
        });

        Composite helpComposite = new Composite(content, SWT.NONE);
        helpComposite.setLayout(new GridLayout(1, true));
        helpComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        label = new Label(helpComposite, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Type of Link: ");

        Button helpButton = new Button(helpComposite, SWT.PUSH);
        helpButton.setImage(ViewProperties.getHelpIcon());
        helpButton.setToolTipText("Help on Links");
        helpButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        helpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final String msg = "The Type of Link specifies which type of link the user wants to create. \n"
                        + "It could be hard, soft or external links. \n\n"
                        + "Hard Link: \n"
                        + "Hard Link creates a hard link to a pre-existing object in an HDF5 file. \n"
                        + "The target object must already exist in the file.\n"
                        + "The HDF5 library keeps a count of all hard links pointing to an object. \n\n"
                        + "Soft Link: \n"
                        + "Soft Link creates a new soft link to an object in an HDF5 file. \n"
                        + "Soft links are only for use only if the target object is in the current file. \n"
                        + "Unlike hard links, a soft link in an HDF5 file is allowed to dangle, \n"
                        + "meaning that the target object need not exist at the time that the link is created.\n"
                        + "The HDF5 library does not keep a count of soft links.  \n\n"
                        + "External Link: \n"
                        + "External Link creates a new soft link to an external object, which is an \n"
                        + "object in a different HDF5 file from the location of the link. \n"
                        + "External links are allowed to dangle like soft links. \n\n"
                        + "Soft links and external links are also known as symbolic links as they use "
                        + "a name to point to an object; hard links employ an object's address in the file.  \n\n\n";

                Tools.showInformation(shell, "Help", msg);
            }
        });

        Composite typeComposite = new Composite(content, SWT.BORDER);
        typeComposite.setLayout(new GridLayout(3, true));
        typeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        hardLink = new Button(typeComposite, SWT.RADIO);
        hardLink.setFont(curFont);
        hardLink.setText("Hard Link");
        hardLink.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
        hardLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                targetFile.setEnabled(false);
                targetFileButton.setEnabled(false);
                targetObject.setEnabled(true);
                targetObject.setEditable(false);
                targetObject.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

                targetObject.removeAll();
                retrieveObjects(fileFormat);

                targetObject.select(0);
            }
        });

        softLink = new Button(typeComposite, SWT.RADIO);
        softLink.setFont(curFont);
        softLink.setText("Soft Link");
        softLink.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
        softLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                targetFile.setEnabled(false);
                targetFileButton.setEnabled(false);
                targetObject.setEnabled(true);
                targetObject.setEditable(true);
                targetObject.setBackground(null);

                targetObject.removeAll();
                retrieveObjects(fileFormat);

                targetObject.select(0);
            }
        });

        externalLink = new Button(typeComposite, SWT.RADIO);
        externalLink.setFont(curFont);
        externalLink.setText("External Link");
        externalLink.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
        externalLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                targetFile.setEnabled(true);
                targetFileButton.setEnabled(true);
                targetObject.setEnabled(true);
                targetObject.setEditable(true);
                targetObject.setBackground(null);
                targetObject.removeAll();
            }
        });

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Target File: ");

        Composite fileComposite = new Composite(content, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        fileComposite.setLayout(layout);
        fileComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        targetFile = new Text(fileComposite, SWT.SINGLE | SWT.BORDER);
        targetFile.setFont(curFont);
        targetFile.setEnabled(false);
        targetFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        targetFile.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    String filename = targetFile.getText();

                    if (filename == null || filename.length() <= 0) return;

                    File chosenFile = new File(filename);

                    if (!chosenFile.exists()) {
                        return;
                    }

                    if (chosenFile.isDirectory()) {
                        currentDir = chosenFile.getPath();
                    }
                    else {
                        currentDir = chosenFile.getParent();
                    }

                    //Check if the target File is not the current file.
                    String currentFileName = fileFormat.getAbsolutePath();
                    if(currentFileName.equals(chosenFile.getAbsolutePath())) {
                        Tools.showError(shell, "Traverse",
                                "Please select a file other than the current file for external links.");
                        targetFile.setText("");
                        return;
                    }

                    getTargetFileObjs();

                    if(targetObject.getItemCount() > 0) targetObject.select(0);
                }
            }
        });

        targetFileButton = new Button(fileComposite, SWT.PUSH);
        targetFileButton.setFont(curFont);
        targetFileButton.setText("Browse...");
        targetFileButton.setEnabled(false);
        targetFileButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        targetFileButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String filename = null;
                filename = openTargetFile();

                if (filename == null) {
                    return;
                }

                targetFile.setText(filename);
                getTargetFileObjs();

                if(targetObject.getItemCount() > 0) targetObject.select(0);
            }
        });

        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Target Object: ");

        targetObject = new CCombo(content, SWT.DROP_DOWN | SWT.BORDER);
        targetObject.setFont(curFont);
        targetObject.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        targetObject.setEditable(false);
        targetObject.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        groupList = new ArrayList<>(objList.size());
        Object obj = null;
        Iterator<?> iterator = objList.iterator();
        String fullName = null;
        int idx_root = -1, idx = -1;
        while (iterator.hasNext()) {
            obj = iterator.next();
            idx++;

            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(g);
                if (g.isRoot()) {
                    fullName = HObject.SEPARATOR;
                    idx_root = idx;
                }
                else {
                    fullName = g.getPath() + g.getName() + HObject.SEPARATOR;
                }
                parentChoice.add(fullName);
            }
            else {
                fullName = ((HObject) obj).getPath() + ((HObject) obj).getName();
            }

            targetObject.add(fullName);
        }

        targetObject.remove(idx_root);
        objList.remove(idx_root);

        if (parentGroup.isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.SEPARATOR));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentGroup.getPath() + parentGroup.getName()
                    + HObject.SEPARATOR));
        }

        // Dummy label to take up space as dialog is resized
        label = new Label(content, SWT.LEFT);
        label.setFont(curFont);
        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));


        // Create the Ok/Cancel button region
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
                newObject = createLink();

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

        hardLink.setSelection(true);
        targetObject.select(0);

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

    private HObject createLink() {
        String name = null;
        Group pgroup = null;
        HObject obj = null;

        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Link name is not specified.");
            return null;
        }

        if (name.indexOf(HObject.SEPARATOR) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Link name cannot contain path.");
            return null;
        }

        pgroup = groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Parent group is null.");
            return null;
        }

        if (hardLink.getSelection()) {
            HObject targetObj = (HObject) objList.get(targetObject
                    .getSelectionIndex());

            if (targetObj == null) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Target object is null.");
                return null;
            }

            if ((targetObj instanceof Group) && ((Group) targetObj).isRoot()) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Cannot make a link to the root group.");
                return null;
            }

            try {
                obj = fileFormat.createLink(pgroup, name, targetObj);
            }
            catch (Exception ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", ex.getMessage());
                return null;
            }
        }
        else if (softLink.getSelection()){
            String target_name = targetObject.getText();
            if (target_name.length() < 1)  {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Target object name is not specified.");
                return null;
            }

            /*
             * While checking for the existence of the object that the soft link points to,
             * the following function currently just calls H5Oopen, which will fail and
             * throw an HDF5 error stack for dangling soft links. Due to this, we
             * temporarily suppress the HDF5 error stack.
             */
            HObject targetObj = null;
            try {
                H5.H5error_off();
                targetObj = fileFormat.get(target_name);
            }
            catch (Exception ex) {
                /* It is possible that this is a soft link to a non-existent
                 * object, in which case this exception would be normal.
                 * For this reason, no logging is done here even though there
                 * is the possibility of a real HDF5 exception being thrown
                 * if something went terribly wrong.
                 */
            }
            finally {
                H5.H5error_on();
            }

            String tObj = null;
            if (targetObj == null) {
                tObj = target_name;

                if (!tObj.startsWith(HObject.SEPARATOR)) {
                    tObj = HObject.SEPARATOR + tObj;
                }
            }

            if ((targetObj instanceof Group) && ((Group) targetObj).isRoot()) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Cannot make a link to the root group.");
                return null;
            }

            try {
                if (targetObj != null)
                    obj = fileFormat.createLink(pgroup, name, targetObj, Group.LINK_TYPE_SOFT);
                else if (tObj != null)
                    obj = fileFormat.createLink(pgroup, name, tObj, Group.LINK_TYPE_SOFT);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", ex.getMessage());
                return null;
            }
        }
        else if (externalLink.getSelection()){
            String TargetFileName = targetFile.getText();
            FileFormat TargetFileFormat = null;
            int fileAccessID = FileFormat.FILE_CREATE_OPEN;

            File TargetFile = new File(TargetFileName);

            if (!TargetFile.exists()) {
                return null;
            }
            FileFormat h5format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            try {
                TargetFileFormat = h5format.createInstance(TargetFileName, fileAccessID);
                TargetFileFormat.open(); //open the file
            }
            catch (Exception ex) {
                log.debug("external link:", ex);
                return null;
            }

            HObject targetObj = null;
            try{
                targetObj = TargetFileFormat.get(targetObject.getText());
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                TargetFileFormat.close();
            }
            catch (Exception ex) {
                log.debug("external link:", ex);
            }

            String tFileObj = null;
            if(targetObj==null){
                String tObj = null;
                tObj = targetObject.getText();
                if (tObj.length() < 1)  {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Create", "Target object name not specified.");
                    return null;
                }
                tFileObj = TargetFileName + FileFormat.FILE_OBJ_SEP + tObj;
            }

            try {
                if(targetObj !=null)
                    obj = fileFormat.createLink(pgroup, name, targetObj, Group.LINK_TYPE_EXTERNAL);
                else if(tFileObj!=null)
                    obj = fileFormat.createLink(pgroup, name, tFileObj, Group.LINK_TYPE_EXTERNAL);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", ex.getMessage());
                return null;
            }
        }

        return obj;
    }

    private String openTargetFile() {
        FileDialog fchooser = new FileDialog(shell, SWT.OPEN);
        fchooser.setFilterPath(currentDir);

        DefaultFileFilter filter = DefaultFileFilter.getFileFilter();
        fchooser.setFilterExtensions(new String[] {"*", filter.getExtensions()});
        fchooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
        fchooser.setFilterIndex(1);

        if(fchooser.open() == null) return null;

        File chosenFile = new File(fchooser.getFilterPath() + File.separator + fchooser.getFileName());

        if (!chosenFile.exists()) {
            return null;
        }

        if (chosenFile.isDirectory()) {
            currentDir = chosenFile.getPath();
        }
        else {
            currentDir = chosenFile.getParent();
        }

        //Check if the target File is not the current file.
        String currentFileName = fileFormat.getAbsolutePath();
        if(currentFileName.equals(chosenFile.getAbsolutePath())) {
            Tools.showError(shell, "Open", "Please select a file other than the current file for external links.");
            targetFile.setText("");
            return null;
        }

        return chosenFile.getAbsolutePath();
    }

    //Function to check if the target File is open in TreeView
    private boolean isFileOpen(String filename)
    {
        boolean isOpen = false;
        FileFormat theFile = null;

        Iterator<?> iterator = fileList.iterator();
        while(iterator.hasNext()) {
            theFile = (FileFormat)iterator.next();
            if (theFile.getFilePath().equals(filename)) {
                isOpen = true;
                if(!theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))){
                    targetObject.setEnabled(false);
                }
                retrieveObjects(theFile);
                break;
            }
        }

        return isOpen;
    }

    private List<HObject> getAllUserObjectsBreadthFirst(FileFormat file) {
        if (file == null) return null;

        ArrayList<HObject> breadthFirstObjects = new ArrayList<>();
        Queue<HObject> currentChildren = new LinkedList<>();
        HObject currentObject = file.getRootObject();

        if (currentObject == null) {
            log.debug("getAllUserObjectsBreadthFirst(): file root object is null");
            return null;
        }

        breadthFirstObjects.add(file.getRootObject()); // Add the root object to the list first

        // Add all root object children to a Queue
        currentChildren.addAll(((Group) currentObject).getMemberList());

        while(!currentChildren.isEmpty()) {
            currentObject = currentChildren.remove();
            breadthFirstObjects.add(currentObject);

            if(currentObject instanceof Group) {
                if(((Group) currentObject).getNumberOfMembersInFile() <= 0) continue;

                currentChildren.addAll(((Group) currentObject).getMemberList());
            }
        }

        return breadthFirstObjects;
    }

    // Retrieves the list of objects from the file
    private void retrieveObjects(FileFormat file) {
        HObject obj = null;
        List<HObject> userObjectList = getAllUserObjectsBreadthFirst(file);
        Iterator<HObject> iterator;
        String fullName = null;

        if (userObjectList == null) {
            log.debug("retrieveObjects(): user object list is null");
            return;
        }

        iterator = userObjectList.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();

            if (obj instanceof Group) {
                Group g = (Group) obj;
                if (g.isRoot()) {
                    fullName = HObject.SEPARATOR;
                }
                else {
                    fullName = g.getPath() + g.getName() + HObject.SEPARATOR;
                }
            }
            else {
                fullName = obj.getPath() + obj.getName();
            }

            targetObject.add(fullName);
        }

        // Remove the root group "/" from the target objects
        targetObject.remove(0);
    }

    // Retrieves objects from Target File.
    private void getTargetFileObjs(){
        FileFormat fileFormatC = null;
        int fileAccessID = FileFormat.FILE_CREATE_OPEN;
        String filename = null;
        filename = targetFile.getText();

        if (filename == null || filename.length()<1) {
            return;
        }

        // Check if the target File is open in treeView
        if (isFileOpen(filename)) {
            return;
        }

        File chosenFile = new File(filename);

        if (!chosenFile.exists()) {
            targetObject.setEnabled(false);
            return;
        }

        FileFormat h5format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
        try {
            fileFormatC = h5format.createInstance(filename, fileAccessID);
            fileFormatC.open(); //open the file
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Target", "Invalid File Format");
            targetFile.setText("");
            return;
        }

        // get the list of objects from the file
        retrieveObjects(fileFormatC);

        try {
            fileFormatC.close();
        }
        catch (Exception ex) {
            log.debug("FileFormat close:", ex);
        }
    }

    /** @return the new dataset created. */
    public HObject getObject() {
        return newObject;
    }

    /** @return the parent group of the new dataset. */
    public Group getParentGroup() {
        return parentGroup;
    }
}
