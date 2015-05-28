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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import hdf.object.DataFormat;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewLinkDialog shows a message dialog requesting user input for creating a
 * new links.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class NewLinkDialog extends JDialog implements ActionListener,DocumentListener, ItemListener {
    private static final long serialVersionUID = 7100424106041533918L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewLinkDialog.class);

    private JTextField nameField;

    @SuppressWarnings("rawtypes")
    private JComboBox parentChoice, targetObject;
    
    private String currentDir;
    
    private JTextField targetFile;
    
    private JButton targetFileButton;
    
    private JRadioButton hardLink, softLink, externalLink;

    private JCheckBox checkUnsigned;

    /** a list of current groups */
    private List<HObject> groupList;

    /** a list of current objects */
    private List<?> objList;
    
    private HObject newObject;

    private FileFormat fileFormat;

    private final Toolkit toolkit;
    
    private ViewManager viewer;
    
    private final List<?> fileList;
      

    /**
     * Constructs NewLinkDialog with specified list of possible parent groups.
     * 
     * @param owner
     *            the owner of the input
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NewLinkDialog(JFrame owner, Group pGroup, List<?> objs) {
        super(owner, "New Link...", true);

        viewer = (ViewManager)owner;
        fileList = viewer.getTreeView().getCurrentFiles();
             
        newObject = null;      
        fileFormat = pGroup.getFileFormat();
        toolkit = Toolkit.getDefaultToolkit();
        objList = objs;
        
        currentDir = ViewProperties.getWorkDir();
        
        parentChoice = new JComboBox();
        parentChoice.setName("linkparent");
        targetObject = new JComboBox();
        targetObject.setEditable(false);
       
        groupList = new Vector<HObject>(objs.size());
        HObject obj = null;
        Iterator<?> iterator = objs.iterator();
        String full_name = null;
        int idx_root = -1, idx = -1;
        while (iterator.hasNext()) {
            obj = (HObject) iterator.next();
            idx++;

            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(obj);
                if (g.isRoot()) {
                    full_name = HObject.separator;
                    idx_root = idx;
                }
                else {
                    full_name = g.getPath() + g.getName() + HObject.separator;
                }
                parentChoice.addItem(full_name);
            }
            else {
                full_name = obj.getPath() + obj.getName();
            }

           targetObject.addItem(full_name);
        }

        targetObject.removeItemAt(idx_root);
       objList.remove(idx_root);

        if (pGroup.isRoot()) {
            parentChoice.setSelectedItem(HObject.separator);
        }
        else {
            parentChoice.setSelectedItem(pGroup.getPath() + pGroup.getName()
                    + HObject.separator);
        }
       

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        int w = 600 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 280 + (ViewProperties.getFontSize() - 12) * 10;
        contentPane.setPreferredSize(new Dimension(w, h));

        JButton okButton = new JButton("   Ok   ");
        okButton.setActionCommand("Ok");
        okButton.setName("makelink");
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);

        // set OK and CANCEL buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // set NAME and PARENT GROUP panel
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout(5, 5));
        JPanel tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(5, 1,5, 5));
        tmpP.add(new JLabel("Link name: "));
        tmpP.add(new JLabel("Parent group: "));
        
        JPanel tmpLinkJPanel = new JPanel();
        tmpLinkJPanel.setLayout(new GridLayout(2, 1));
        tmpLinkJPanel.add(new JLabel("Type of Link: "));
        JButton helpButton = new JButton(ViewProperties.getHelpIcon());
        helpButton.setToolTipText("Help on Links");
        helpButton.setMargin(new Insets(0, 0, 0, 0));
        helpButton.addActionListener(this);
        helpButton.setActionCommand("Help on Links");
        tmpLinkJPanel.add(helpButton);
        tmpP.add(tmpLinkJPanel);
        tmpP.add(new JLabel("Target File: "));
        tmpP.add(new JLabel("Target Object: "));
        namePanel.add(tmpP, BorderLayout.WEST);
        
        tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(5, 1,5,5));
        nameField = new JTextField();
        nameField.setName("linkname");
        tmpP.add(nameField);
        tmpP.add(parentChoice);
              
        JPanel tmpP0 = new JPanel();
        tmpP0.setLayout(new GridLayout(1, 3));
        tmpP0.add(hardLink = new JRadioButton("Hard Link ", true));
        tmpP0.add(softLink = new JRadioButton("Soft Link "));
        tmpP0.add(externalLink = new JRadioButton("External Link "));
        tmpP0.setBorder(new TitledBorder(""));
        tmpP.add(tmpP0);   
        ButtonGroup bgroup = new ButtonGroup();
        bgroup.add(hardLink);
        bgroup.add(softLink);
        bgroup.add(externalLink);
        hardLink.addItemListener(this);
        hardLink.setName("hardlink");
        softLink.addItemListener(this);
        softLink.setName("softlink");
        externalLink.addItemListener(this);
        externalLink.setName("externallink");
        
        JPanel p0 = new JPanel();
        p0.setLayout(new BorderLayout());
        p0.add(targetFile = new JTextField(), BorderLayout.CENTER);
        targetFile.getDocument().addDocumentListener(this);
        targetFile.addActionListener(this);
        targetFile.setActionCommand("Link to File");
        JButton b = new JButton("Browse...");
        targetFileButton = b;
        b.setActionCommand("Browse File");
        b.setName("targetfilebutton");
        b.addActionListener(this);
        p0.add(b, BorderLayout.EAST);
        tmpP.add(p0);
        targetFile.setEnabled(false);
        targetFileButton.setEnabled(false);
        
        tmpP.add(targetObject);
        targetObject.setName("linktarget");
        namePanel.add(tmpP, BorderLayout.CENTER);
        contentPane.add(namePanel, BorderLayout.CENTER);

        // locate the H5Property dialog
        Point l = owner.getLocation();
        l.x += 250;
        l.y += 100;
        setLocation(l);
        validate();
        pack();
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String cmd = e.getActionCommand();
        
        if (cmd.equals("Help on Links")) {
            final String msg = "The Type of Link specifies which type of link the user wants to create. \n"
                + "It could be hard, soft or external links. \n\n"
                + "<html><b>Hard Link</b></html> \n"
                + "Hard Link creates a hard link to a pre-existing object in an HDF5 file. \n"    
                + "The target object must already exist in the file.\n" 
                + "The HDF5 library keeps a count of all hard links pointing to an object. \n\n"
                + "<html><b>Soft Link</b></html> \n"
                + "Soft Link creates a new soft link to an object in an HDF5 file. \n" 
                + "Soft links are only for use only if the target object is in the current file. \n"
                + "Unlike hard links, a soft link in an HDF5 file is allowed to dangle, \n" 
                + "meaning that the target object need not exist at the time that the link is created.\n"
                + "The HDF5 library does not keep a count of soft links  \n\n"
                + "<html><b>External Link</b></html> \n"
                + "External Link creates a new soft link to an external object, which is an object\n" 
                + "in a different HDF5 file from the location of the link. External links are \n"
                + "allowed to dangle like soft links. \n\n"
                + "Soft links and external links are also known as symbolic links as they use \n" 
                + "a name to point to an object; hard links employ an object's address in the file.  \n\n\n";
            JOptionPane.showMessageDialog(this, msg);
        }
   
        if (cmd.equals("Browse File")) {        
            String filename = null;
            filename = openTargetFile();
            
             if (filename == null) {
                 return;
             }
             targetFile.setText(filename);
         }
               
        if (cmd.equals("Ok")) {
            newObject = createLink();

            if (newObject != null) {
                dispose();
            }
        }
        if (cmd.equals("Cancel")) {
            newObject = null;
            dispose();
            ((Vector<HObject>) groupList).setSize(0);
        }
    }

    private String openTargetFile()
    {
    	JFileChooser fchooser = new JFileChooser(currentDir);
    	fchooser.setFileFilter(DefaultFileFilter.getFileFilter());

    	int returnVal = fchooser.showOpenDialog(this);
    	if(returnVal != JFileChooser.APPROVE_OPTION) {
    		return null;
    	}

    	File choosedFile = fchooser.getSelectedFile();

    	if (choosedFile == null) {
    		return null;
    	}

    	if (choosedFile.isDirectory()) {
    		currentDir = choosedFile.getPath();
    	} 
    	else {
    		currentDir = choosedFile.getParent();
    	}

    	return choosedFile.getAbsolutePath();
    }
   
    private final List<Object> breadthFirstUserObjects(TreeNode node)
    {
        if (node == null) {
            return null;
        }

        Vector<Object> list = new Vector<Object>();
        DefaultMutableTreeNode theNode = null;
        Enumeration<?> local_enum = ((DefaultMutableTreeNode)node).breadthFirstEnumeration();
        while(local_enum.hasMoreElements()) {
            theNode = (DefaultMutableTreeNode)local_enum.nextElement();
            list.add(theNode.getUserObject());
        }

        return list;
    }
   
    private HObject createLink() {
        String name = null;
        Group pgroup = null;
        HObject obj = null;

        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this,
                    "Link name is not specified.", getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this,
                    "Link name cannot contain path.", getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectedIndex());

        if (pgroup == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Parent group is null.",
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }    

        if (hardLink.isSelected()) {
            HObject targetObj = (HObject) objList.get(targetObject
                    .getSelectedIndex());

            if (targetObj == null) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Target object is null.",
                        getTitle(), JOptionPane.ERROR_MESSAGE);
                return null;
            }

            if ((targetObj instanceof Group) && ((Group) targetObj).isRoot()) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this,
                        "Cannot make a link to the root group.", getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            
            try {
                obj = fileFormat.createLink(pgroup, name, targetObj);
            }
            catch (Exception ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, ex, getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }          
        }
        else if (softLink.isSelected()){            
            String target_name = targetObject.getEditor().getItem().toString();
            if (target_name.length() < 1)  {
                toolkit.beep();
                JOptionPane.showMessageDialog(this,
                        "Target object name is not specified.", getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }

            HObject targetObj = null;
            try {
                targetObj = fileFormat.get(targetObject.getEditor().getItem().toString());
            } 
            catch (Exception ex) {
            	log.debug("softlink:", ex);
            }
                   
            String tObj = null;
            if(targetObj==null){
                tObj = targetObject.getEditor().getItem().toString();
                
                if (!tObj.startsWith(HObject.separator)) {
                    tObj = HObject.separator + tObj;
                }
            }
            
            if ((targetObj instanceof Group) && ((Group) targetObj).isRoot()) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this,
                        "Cannot make a link to the root group.", getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }

            try {
                if(targetObj !=null)
                    obj = fileFormat.createLink(pgroup, name, targetObj, Group.LINK_TYPE_SOFT);
                else if(tObj!=null)
                    obj = fileFormat.createLink(pgroup, name, tObj, Group.LINK_TYPE_SOFT);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                toolkit.beep();
                JOptionPane.showMessageDialog(this, ex, getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        else if (externalLink.isSelected()){
            String TargetFileName = targetFile.getText();
            FileFormat TargetFileFormat = null;
            int fileAccessID = FileFormat.FILE_CREATE_OPEN;

            File TargetFile = new File(TargetFileName);

            if (!TargetFile.exists()) {               
                return null;
            }
            FileFormat h5format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            try {
                //h5format.close();
                TargetFileFormat = h5format.createInstance(TargetFileName, fileAccessID);
                TargetFileFormat.open(); //open the file
            } 
            catch (Exception ex) {
            	log.debug("external link:", ex);
                return null;
            } 
            
            HObject targetObj = null;
            try{
                targetObj = TargetFileFormat.get(targetObject.getEditor().getItem().toString());
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
                tObj = targetObject.getEditor().getItem().toString();
                if (tObj.length() < 1)  {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this,
                            "Target object name not specified.", getTitle(),
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                tFileObj = TargetFileName + FileFormat.FILE_OBJ_SEP + tObj;
            }
// should allow to link to the root of an external file            
//            if ((targetObj instanceof Group) && ((Group) targetObj).isRoot()) {
//                toolkit.beep();
//                JOptionPane.showMessageDialog(this,
//                        "Cannot make a link to the root group.", getTitle(),
//                        JOptionPane.ERROR_MESSAGE);
//                return null;
//            }

            try {
                if(targetObj !=null)
                	obj = fileFormat.createLink(pgroup, name, targetObj, Group.LINK_TYPE_EXTERNAL);
                else if(tFileObj!=null)
                    obj = fileFormat.createLink(pgroup, name, tFileObj, Group.LINK_TYPE_EXTERNAL);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                toolkit.beep();
                JOptionPane.showMessageDialog(this, ex, getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        
        return obj;
    }

    /** Returns the new dataset created. */
    public DataFormat getObject() {
        return newObject;
    }

    /** Returns the parent group of the new dataset. */
    public Group getParentGroup() {
        return (Group) groupList.get(parentChoice.getSelectedIndex());
    }

    public void changedUpdate(DocumentEvent arg0) {        
    }

    public void insertUpdate(DocumentEvent e) {
        targetObject.setEnabled(true);
        getTargetFileObjs();
    }

    public void removeUpdate(DocumentEvent arg0) {
        targetObject.setEnabled(true);
        getTargetFileObjs();
    }
    
    //Retrieving objects from Target File.
    private void getTargetFileObjs(){
        FileFormat fileFormatC = null;
        int fileAccessID = FileFormat.FILE_CREATE_OPEN;
        String filename = null;
        filename = targetFile.getText();

        if (filename == null || filename.length()<1) {
            return;
        }

        //Check if the target File is not the current file.
        String CurrentFileName = fileFormat.getAbsolutePath();
        if(CurrentFileName.equals(filename))
            targetObject.setEnabled(false);

        //Check if the target File is open in treeView
        if (isFileOpen(filename)) {
            return;
        }

        File choosedFile = new File(filename);

        if (!choosedFile.exists()) {
            targetObject.setEnabled(false);
            return;
        }
        FileFormat h5format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
        try {
            //h5format.close();
            fileFormatC = h5format.createInstance(filename, fileAccessID);
            fileFormatC.open(); //open the file

        } 
        catch (Exception ex) {
            targetObject.setEnabled(false);
            toolkit.beep();
            JOptionPane.showMessageDialog(this,"Invalid File Format", getTitle(),
                    JOptionPane.ERROR_MESSAGE);    
            return;
        } 

        //getting the list of objects from the file:-
        retriveObjects(fileFormatC);

        try {             
            fileFormatC.close();    
        } 
        catch (Exception ex) {
        	log.debug("FileFormat close:", ex);
        }
    }
    
    //Function to check if the target File is open in treeView
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
    			retriveObjects(theFile);
    			break;
    		}
    	} // while(iterator.hasNext())

    	return isOpen;
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();

        if (source instanceof JRadioButton) {
            if (source.equals(hardLink) || source.equals(softLink) || source.equals(externalLink)) {
                if (hardLink.isSelected()) {
                    targetFile.setEnabled(false);
                    targetFileButton.setEnabled(false);
                    targetObject.setEnabled(true);
                    targetObject.setEditable(false);                    
                    retriveObjects(fileFormat);
                }
                else if (softLink.isSelected()) {
                    targetFile.setEnabled(false);
                    targetFileButton.setEnabled(false);
                    targetObject.setEnabled(true);
                    targetObject.setEditable(true);
                    retriveObjects(fileFormat);
                }
                else if (externalLink.isSelected()) {
                    targetFile.setEnabled(true);
                    targetFileButton.setEnabled(true);
                    targetObject.setEnabled(true);
                    targetObject.setEditable(true);
                    targetObject.removeAllItems();
                }
            }
        }
    }
    
    //getting the list of objects from the file:-
    private void retriveObjects(FileFormat file) {        
        List<Object> objsFile =  breadthFirstUserObjects(file.getRootNode());
        List<HObject> groupListFile = new Vector<HObject>(objsFile.size());
        HObject obj = null;
        Iterator<Object> iterator = objsFile.iterator();
        List<Object> objListFile = objsFile;
        String full_name = null;
        int idx_root = -1, idx = -1;
        targetObject.removeAllItems();
        while (iterator.hasNext()) {
            obj = (HObject) iterator.next();
            idx++;

            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupListFile.add(obj);
                if (g.isRoot()) {
                    full_name = HObject.separator;
                    idx_root = idx;
                }
                else {
                    full_name = g.getPath() + g.getName() + HObject.separator;
                }
            }
            else {
                full_name = obj.getPath() + obj.getName();
            }
            targetObject.addItem(full_name);
        }
        targetObject.removeItemAt(idx_root);
        objListFile.remove(idx_root);           
    }
    
}

