public class NewLinkDialogOld extends JDialog implements ActionListener,DocumentListener, ItemListener {
    public NewLinkDialogOld(JFrame owner, Group pGroup, List<?> objs) {
    	targetObject.setEditable(false);    
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
        retrieveObjects(fileFormatC);

        try {             
            fileFormatC.close();    
        } 
        catch (Exception ex) {
        	log.debug("FileFormat close:", ex);
        }
    }
}

