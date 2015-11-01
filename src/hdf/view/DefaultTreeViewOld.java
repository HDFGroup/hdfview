public class DefaultTreeViewOld implements TreeView {
    /**
     * Save the current file into HDF4. Since HDF4 does not support packing, the
     * source file is copied into the new file with the exact same content.
     */
    private final void saveAsHDF4(FileFormat srcFile) {
        if (srcFile == null) {
            shell.getDisplay().beep();
            showError("Select a file to save.", null);
            return;
        }

        Shell owner = (viewer == null) ? new Shell(Display.getCurrent()) : (Shell) viewer;
        String currentDir = srcFile.getParent();
        
        NewFileDialog dialog = new NewFileDialog(owner, currentDir, FileFormat.FILE_TYPE_HDF4, getCurrentFiles());
        String filename = dialog.open();
        if (!dialog.isFileCreated()) return;

        // Since cannot pack hdf4, simply copy the whole physical file
        int length = 0;
        int bsize = 512;
        byte[] buffer;
        BufferedInputStream bi = null;
        BufferedOutputStream bo = null;

        try {
            bi = new BufferedInputStream(new FileInputStream(srcFile.getFilePath()));
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage() + "\n" + filename, null);
            return;
        }

        try {
            bo = new BufferedOutputStream(new FileOutputStream(filename));
        }
        catch (Exception ex) {
            try {
                bi.close();
            }
            catch (Exception ex2) {
            	log.debug("Output file force input close:", ex2);
            }
            
            shell.getDisplay().beep();
            showError(ex.getMessage(), null);
            return;
        }

        buffer = new byte[bsize];
        try {
            length = bi.read(buffer, 0, bsize);
        }
        catch (Exception ex) {
            length = 0;
        }
        while (length > 0) {
            try {
                bo.write(buffer, 0, length);
                length = bi.read(buffer, 0, bsize);
            }
            catch (Exception ex) {
                length = 0;
            }
        }

        try {
            bo.flush();
        }
        catch (Exception ex) {
        	log.debug("Output file:", ex);
        }
        try {
            bi.close();
        }
        catch (Exception ex) {
        	log.debug("Input file:", ex);
        }
        try {
            bo.close();
        }
        catch (Exception ex) {
        	log.debug("Output file:", ex);
        }

        try {
            openFile(filename, FileFormat.WRITE);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage() + "\n" + filename, null);
        }
    }

    /**
     * Copy the current file into a new file. The new file does not include the
     * inaccessible objects. Values of reference dataset are not updated in the
     * new file.
     */
    private void saveAsHDF5(FileFormat srcFile) {
        if (srcFile == null) {
            shell.getDisplay().beep();
            showError("Select a file to save.", null);
            return;
        }

        HObject root = srcFile.getRootObject();
        if (root == null) {
            shell.getDisplay().beep();
            showError("The file is empty.", null);
            return;
        }

        Shell owner = (viewer == null) ? new Shell(Display.getCurrent()) : (Shell) viewer;
        NewFileDialog dialog = new NewFileDialog(owner, srcFile.getParent(), FileFormat.FILE_TYPE_HDF5,
                getCurrentFiles());
        String filename = dialog.open();
        
        if (!dialog.isFileCreated()) return;

        //int n = root.getItemCount();
        //Vector<Object> objList = new Vector<Object>(n);
        TreeItem item = null;
        //for (int i = 0; i < n; i++) {
        //    item = root.getItem(i);
        //    objList.add(item.getData());
        //}

        FileFormat newFile = null;
        try {
            newFile = openFile(filename, FileFormat.WRITE);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage() + "\n" + filename, null);
            return;
        }

        if (newFile == null) return;

        HObject pitem = newFile.getRootObject();

        //pasteObject(objList, pitem, newFile);
        //objList.setSize(0);

        Group srcGroup = (Group) root;
        Group dstGroup = (Group) newFile.getRootObject();
        Object[] parameter = new Object[2];
        Class<?> classHOjbect = null;
        Class<?>[] parameterClass = new Class[2];
        Method method = null;

        // Copy attributes of the root group
        try {
            parameter[0] = srcGroup;
            parameter[1] = dstGroup;
            classHOjbect = Class.forName("hdf.object.HObject");
            parameterClass[0] = parameterClass[1] = classHOjbect;
            method = newFile.getClass().getMethod("copyAttributes", parameterClass);
            method.invoke(newFile, parameter);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), null);
        }

        // Update reference datasets
        parameter[0] = srcGroup.getFileFormat();
        parameter[1] = newFile;
        parameterClass[0] = parameterClass[1] = parameter[0].getClass();
        try {
            method = newFile.getClass().getMethod("updateReferenceDataset", parameterClass);
            method.invoke(newFile, parameter);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), null);
        }
    }

    /**
     * Returns a list of all user objects that traverses the subtree rooted at
     * this item in breadth-first order..
     * 
     * @param item
     *            the item to start with.
     */
    private final List<Object> breadthFirstUserObjects(TreeItem item) {
        if (item == null) return null;

        Vector<Object> list = new Vector<Object>();
        TreeItem theItem = null;
        //Enumeration<?> local_enum = item.breadthFirstEnumeration();
        //while (local_enum.hasMoreElements()) {
        //    theItem = local_enum.nextElement();
        //    list.add(theItem.getData());
        //}

        return list;
    }

    /**
     * Find first object that is matched by name.
     * 
     * @param objName
     *            -- the object name.
     * @return the object if found, otherwise, returns null.
     */
    /*
    private final static HObject find(String objName, TreePath treePath, Tree tree) {
        HObject retObj = null;
        boolean isFound = false, isPrefix = false, isSuffix = false, isContain = false;

        if (objName == null || objName.length() <= 0 || treePath == null) {
            return null;
        }

        if (objName.equals("*")) return null;

        if (objName.startsWith("*")) {
            isSuffix = true;
            objName = objName.substring(1, objName.length());
        }

        if (objName.endsWith("*")) {
            isPrefix = true;
            objName = objName.substring(0, objName.length() - 1);
        }

        if (isPrefix && isSuffix) {
            isContain = true;
            isPrefix = isSuffix = false;
        }

        if (objName == null || objName.length() <= 0) return null;

        TreeItem item = treePath.getLastPathComponent();
        if (item == null) return null;

        HObject obj = null;
        String theName = null;
        TreeItem theItem = null;
        //Enumeration<?> local_enum = item.breadthFirstEnumeration();
        //while (local_enum.hasMoreElements()) {
        //    theItem = (TreeItem) local_enum.nextElement();
        //    obj = (HObject) theItem.getData();
        //    if (obj != null && (theName = obj.getName()) != null) {
        //        if (isPrefix)
        //            isFound = theName.startsWith(objName);
        //        else if (isSuffix)
        //            isFound = theName.endsWith(objName);
        //        else if (isContain)
        //            isFound = theName.contains(objName);
        //        else
        //            isFound = theName.equals(objName);

        //        if (isFound) {
        //            retObj = obj;
        //            break;
        //        }
        //    }
        //}

        if (retObj != null) {
        //    TreePath dstPath = getTreePath(treePath, theItem, 0);

        //    tree.fireTreeExpanded(dstPath) ;
        //    tree.setSelectionPath(dstPath);
        //    tree.scrollPathToVisible(dstPath);
        }

        return retObj;
    }
    */

    /**
     * Get the TreePath from the parent to the target item.
     * 
     * @param parent
     *            -- the parent TreePath
     * @param item
     *            -- the target item
     * @param depth
     * @return the tree path if target item found, otherwise; returns null;
     */
    /*
    private static TreePath getTreePath(TreePath parent, TreeItem item, int depth) {
        if (item == null || parent == null || depth < 0) return null;

        TreeItem theItem = parent.getLastPathComponent();
        if (item == theItem) return parent;

        //if (theItem.getChildCount() >= 0) {
        //    for (Enumeration<?> e = theItem.children(); e.hasMoreElements();) {
        //        TreeItem n = (TreeItem) e.nextElement();
        //        TreePath path = parent.pathByAddingChild(n);
        //        TreePath result = getTreePath(path, item, depth + 1);

        //        if (result != null) {
        //            return result;
        //        }
        //    }
        //}

        return null;
    }
    */

    /** Save data as file. */
    private void saveAsFile() throws Exception {
        if (!(selectedObject instanceof Dataset) || (selectedObject == null) || (selectedItem == null)) return;
        
        Dataset dataset = (Dataset) selectedObject;
        FileDialog fChooser = new FileDialog(shell);
        //fChooser.setFilterPath(dataset.getFile().);
        
        //final JFileChooser fchooser = new JFileChooser(dataset.getFile());
        //fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
        // fchooser.changeToParentDirectory();
        File chosenFile = null;
        
        if(binaryOrder == 99) {
            fChooser.setText("Save Dataset Data To Text File --- " + dataset.getName());
            chosenFile = new File(dataset.getName() + ".txt");
        }
        else {
            fChooser.setText("Save Current Data To Binary File --- " + dataset.getName());
            chosenFile = new File(dataset.getName() + ".bin");
        }

        //fchooser.setSelectedFile(choosedFile);
        //int returnVal = fchooser.showSaveDialog(this);

        if (fChooser.open() == null) return;

        //choosedFile = fchooser.getSelectedFile();
        if (chosenFile == null) return;
        
        String fname = chosenFile.getAbsolutePath();

        // Check if the file is in use
        List<?> fileList = viewer.getTreeView().getCurrentFiles();
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator<?> iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                    error.setText("Export Dataset");
                    error.setMessage("Unable to save data to file \"" + fname + "\". \nThe file is being used.");
                    error.open();
                    return;
                }
            }
        }

        if (chosenFile.exists()) {
        	MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        	confirm.setText("Export Dataset");
        	confirm.setMessage("File exists. Do you want to replace it?");
            if (confirm.open() == SWT.NO) return;
        }

        boolean isH4 = selectedObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        if (isH4) {
            shell.getDisplay().beep();
            showError("Cannot export HDF4 object.", null);
            return;
        }

        try {
            selectedObject.getFileFormat().exportDataset(fname, dataset.getFile(), dataset.getFullName(), binaryOrder);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            showError(ex.getMessage(), null);
        }

        viewer.showStatus("Data save to: " + fname);
    }
    
    private void setLibVersionBounds() {
        Object[] lowValues = { "Earliest", "Latest" };
        Object[] highValues = { "Latest" };
        //JComboBox lowComboBox = new JComboBox(lowValues);
        //lowComboBox.setName("earliestversion");
        //JComboBox highComboBox = new JComboBox(highValues);
        //highComboBox.setName("latestversion");

        //Object[] msg = { "Earliest Version:", lowComboBox, "Latest Version:", highComboBox };
        Object[] options = { "Ok", "Cancel" };
        //JOptionPane op = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options);

        //op.setName("libselect");
        //JDialog dialog = op.createDialog(this, "Set the library version bounds: ");
        //dialog.setVisible(true);

        String result = null;
        try {
           // result = (String) op.getValue();
        }
        catch (Exception err) {
            // err.printStackTrace();
        }

        if ((result != null) && (result.equals("Ok"))) {
            int low = -1;
            int high = 1;
            //if ((lowComboBox.getSelectedItem()).equals("Earliest"))
            //    low = 0;
            //else
            //    low = 1;
            try {
                selectedObject.getFileFormat().setLibBounds(low, high);
            }
            catch (Throwable err) {
                shell.getDisplay().beep();
                showError("Error when setting lib version bounds", null);
                return;
            }
        }
        else
            return;
    }

    /**
     * Display the content of a data object.
     * 
     * @param dataObject
     *            the data object
     * @return the DataView that displays the data content
     * @throws Exception
     */
    public DataView showDataContent(HObject dataObject) throws Exception {
        log.trace("showDataContent: start");

        if ((dataObject == null) || !(dataObject instanceof Dataset)) {
        	return null; // can only display dataset
        }

        Dataset d = (Dataset) dataObject;

        if (d.getRank() <= 0) d.init();
        
        boolean isText = ((d instanceof ScalarDS) && ((ScalarDS) d).isText());
        boolean isImage = ((d instanceof ScalarDS) && ((ScalarDS) d).isImage());
        boolean isDisplayTypeChar = false;
        boolean isTransposed = false;
        boolean isIndexBase1 = ViewProperties.isIndexBase1();
        BitSet bitmask = null;
        String dataViewName = null;
        
        log.trace("showDataContent: inited");

        Shell theShell = (Shell) viewer.getDataView(d);

        if (isDefaultDisplay) {
            if (theShell != null) {
                theShell.setActive();
                return null;
            }

            if (isText) {
                dataViewName = (String) HDFView.getListOfTextView().get(0);
            }
            else if (isImage) {
                dataViewName = (String) HDFView.getListOfImageView().get(0);
            }
            else {
                dataViewName = (String) HDFView.getListOfTableView().get(0);
            }
        }
        else {
            DataOptionDialog dialog = new DataOptionDialog(viewer, d);

            dialog.setVisible(true);
            if (dialog.isCancelled()) {
                return null;
            }

            isImage = dialog.isImageDisplay();
            isDisplayTypeChar = dialog.isDisplayTypeChar();
            dataViewName = dialog.getDataViewName();
            isTransposed = dialog.isTransposed();
            bitmask = dialog.getBitmask();
            isIndexBase1 = dialog.isIndexBase1();
            isApplyBitmaskOnly = dialog.isApplyBitmaskOnly();
        }
        
        log.trace("showDataContent: {}", dataViewName);

        // Enables use of JHDF5 in JNLP (Web Start) applications, the system
        // class loader with reflection first.
        Class<?> theClass = null;
        try {
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            try {
                theClass = ViewProperties.loadExtClass().loadClass(dataViewName);
            }
            catch (Exception ex2) {
                theClass = null;
            }
        }

        // Use default dataview
        if (theClass == null) {
            log.trace("showDataContent: use default dataview");
            if (isText)
                dataViewName = "hdf.view.DefaultTextView";
            else if (isImage)
                dataViewName = "hdf.view.DefaultImageView";
            else
                dataViewName = "hdf.view.DefaultTableView";
            try {
                theClass = Class.forName(dataViewName);
            }
            catch (Exception ex) {
            	log.debug("Class.forName {} failure: ", dataViewName, ex);
            }
        }
        Object theView = null;
        Object[] initargs = { viewer };
        HashMap<DATA_VIEW_KEY, Serializable> map = new HashMap<DATA_VIEW_KEY, Serializable>(8);
        map.put(ViewProperties.DATA_VIEW_KEY.INDEXBASE1, new Boolean(isIndexBase1));
        if (bitmask != null) {
            map.put(ViewProperties.DATA_VIEW_KEY.BITMASK, bitmask);
            if (isApplyBitmaskOnly) map.put(ViewProperties.DATA_VIEW_KEY.BITMASKOP, ViewProperties.BITMASK_OP.AND);

            // create a copy of dataset
            ScalarDS d_copy = null;
            Constructor<? extends Dataset> constructor = null;
            Object[] paramObj = null;
            try {
                Class<?>[] paramClass = { FileFormat.class, String.class, String.class, long[].class };
                constructor = d.getClass().getConstructor(paramClass);

                paramObj = new Object[] { d.getFileFormat(), d.getName(), d.getPath(), d.getOID() };
            }
            catch (Exception ex) {
                constructor = null;
            }

            try {
                d_copy = (ScalarDS) constructor.newInstance(paramObj);
            }
            catch (Exception ex) {
                d_copy = null;
            }
            if (d_copy != null) {
                try {
                    d_copy.init();
                    log.trace("showDataContent: d_copy inited");
                    int rank = d.getRank();
                    System.arraycopy(d.getDims(), 0, d_copy.getDims(), 0, rank);
                    System.arraycopy(d.getStartDims(), 0, d_copy.getStartDims(), 0, rank);
                    System.arraycopy(d.getSelectedDims(), 0, d_copy.getSelectedDims(), 0, rank);
                    System.arraycopy(d.getStride(), 0, d_copy.getStride(), 0, rank);
                    System.arraycopy(d.getSelectedIndex(), 0, d_copy.getSelectedIndex(), 0, 3);
                }
                catch (Throwable ex) {
                    ex.printStackTrace();
                }

                map.put(ViewProperties.DATA_VIEW_KEY.OBJECT, d_copy);
            }
        }
        if (dataViewName.startsWith("hdf.view.DefaultTableView")) {
            map.put(ViewProperties.DATA_VIEW_KEY.CHAR, new Boolean(isDisplayTypeChar));
            map.put(ViewProperties.DATA_VIEW_KEY.TRANSPOSED, new Boolean(isTransposed));
            Object[] tmpargs = { viewer, map };
            initargs = tmpargs;
        }
        else if (dataViewName.startsWith("hdf.view.DefaultImageView")) {
            map.put(ViewProperties.DATA_VIEW_KEY.CONVERTBYTE, new Boolean((bitmask != null)));
            Object[] tmpargs = { viewer, map };
            initargs = tmpargs;
        }

        //Cursor cursor = new Cursor(Display.getCurrent(), SWT.CURSOR_WAIT);
        //shell.setCursor(cursor);
        //cursor.dispose();
        
        try {
            theView = Tools.newInstance(theClass, initargs);
            log.trace("showDataContent: Tools.newInstance");

            viewer.addDataView((DataView) theView);
        }
        finally {
        	//cursor = new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW);
        	//shell.setCursor(cursor);
        	//cursor.dispose();
        }

        log.trace("showDataContent: finish");
        return (DataView) theView;
    }

    /**
     * Displays the meta data of a data object.
     * 
     * @param dataObject
     *            the data object
     * @return the MetaDataView that displays the MetaData of the data object
     * @throws Exception
     */
    public MetaDataView showMetaData(HObject dataObject) throws Exception {
        if (dataObject == null) {
            return null;
        }

        List<?> metaDataViewList = HDFView.getListOfMetaDataView();
        if ((metaDataViewList == null) || (metaDataViewList.size() <= 0)) {
            return null;
        }

        int n = metaDataViewList.size();
        String className = (String) metaDataViewList.get(0);

        if (!isDefaultDisplay && (n > 1)) {
            //className = (String) JOptionPane.showInputDialog(this, "Select MetaDataView", "HDFView",
            //        JOptionPane.INFORMATION_MESSAGE, null, metaDataViewList.toArray(), className);
        }

        // enables use of JHDF5 in JNLP (Web Start) applications, the system
        // class loader with reflection first.
        Class<?> theClass = null;
        try {
            theClass = Class.forName(className);
        }
        catch (Exception ex) {
            theClass = ViewProperties.loadExtClass().loadClass(className);
        }

        Object[] initargs = { viewer };
        MetaDataView dataView = (MetaDataView) Tools.newInstance(theClass, initargs);

        return dataView;
    }
    
    /**
     * This class is used to change the default icons for tree nodes.
     * 
     * @see javax.swing.tree.DefaultTreeCellRenderer
     */
    /*
    private class HTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = -9030708781106435297L;
        private Image              h4Icon, h5Icon, datasetIcon, imageIcon, tableIcon, textIcon, openFolder, closeFolder,
        datasetIconA, imageIconA, tableIconA, textIconA, openFolderA, closeFolderA, datatypeIcon,
        datatypeIconA, questionIcon;

        private HTreeCellRenderer() {
            super();

            openFolder = ViewProperties.getFolderopenIcon();
            closeFolder = ViewProperties.getFoldercloseIcon();
            datasetIcon = ViewProperties.getDatasetIcon();
            imageIcon = ViewProperties.getImageIcon();
            h4Icon = ViewProperties.getH4Icon();
            h5Icon = ViewProperties.getH5Icon();
            tableIcon = ViewProperties.getTableIcon();
            textIcon = ViewProperties.getTextIcon();

            openFolderA = ViewProperties.getFolderopenIconA();
            closeFolderA = ViewProperties.getFoldercloseIconA();
            datasetIconA = ViewProperties.getDatasetIconA();
            imageIconA = ViewProperties.getImageIconA();
            tableIconA = ViewProperties.getTableIconA();
            textIconA = ViewProperties.getTextIconA();
            datatypeIcon = ViewProperties.getDatatypeIcon();
            datatypeIconA = ViewProperties.getDatatypeIconA();

            questionIcon = ViewProperties.getQuestionIcon();

            if (openFolder != null) {
                openIcon = openFolder;
            }
            else {
                openFolder = this.openIcon;
            }

            if (closeFolder != null) {
                closedIcon = closeFolder;
            }
            else {
                closeFolder = closedIcon;
            }

            if (datasetIcon == null) {
                datasetIcon = leafIcon;
            }
            if (imageIcon == null) {
                imageIcon = leafIcon;
            }
            if (tableIcon == null) {
                tableIcon = leafIcon;
            }
            if (textIcon == null) {
                textIcon = leafIcon;
            }
            if (h4Icon == null) {
                h4Icon = leafIcon;
            }
            if (h5Icon == null) {
                h5Icon = leafIcon;
            }
            if (datatypeIcon == null) {
                datatypeIcon = leafIcon;
            }

            if (questionIcon == null) {
                questionIcon = leafIcon;
            }

            if (openFolderA == null) {
                openFolderA = openFolder;
            }
            if (closeFolderA == null) {
                closeFolderA = closeFolder;
            }
            if (datasetIconA == null) {
                datasetIconA = datasetIcon;
            }
            if (imageIconA == null) {
                imageIconA = imageIcon;
            }
            if (tableIconA == null) {
                tableIconA = tableIcon;
            }
            if (textIconA == null) {
                textIconA = textIcon;
            }
            if (datatypeIconA == null) {
                datatypeIconA = datatypeIcon;
            }
        }
        */

        /*
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            HObject theObject = (HObject) ((DefaultMutableTreeNode) value).getUserObject();
            
            if (theObject == null)
            	return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            boolean hasAttribute = theObject.hasAttribute();
            
            if (theObject instanceof Dataset) {
                if (theObject instanceof ScalarDS) {
                    ScalarDS sd = (ScalarDS) theObject;
                    if (sd.isImage()) {
                        if (hasAttribute) {
                            leafIcon = imageIconA;
                        }
                        else {
                            leafIcon = imageIcon;
                        }
                    }
                    else if (sd.isText()) {
                        if (hasAttribute) {
                            leafIcon = textIconA;
                        }
                        else {
                            leafIcon = textIcon;
                        }
                    }
                    else {
                        if (hasAttribute) {
                            leafIcon = datasetIconA;
                        }
                        else {
                            leafIcon = datasetIcon;
                        }

                    }
                }
                else if (theObject instanceof CompoundDS) {
                    if (hasAttribute) {
                        leafIcon = tableIconA;
                    }
                    else {
                        leafIcon = tableIcon;
                    }
                }
            }
            else if (theObject instanceof Group) {
                Group g = (Group) theObject;

                if (hasAttribute) {
                    openIcon = openFolderA;
                    closedIcon = closeFolderA;
                }
                else {
                    openIcon = openFolder;
                    closedIcon = closeFolder;
                }

                if (g.isRoot()) {
                    if (g.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
                        openIcon = closedIcon = h5Icon;
                    }
                    else if (g.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4))) {
                        openIcon = closedIcon = h4Icon;
                    }
                }
            }
            else if (theObject instanceof Datatype) {
                Datatype t = (Datatype) theObject;

                if (hasAttribute) {
                    leafIcon = datatypeIconA;
                }
                else {
                    leafIcon = datatypeIcon;
                }
            }

            else {
                leafIcon = questionIcon;
            }

            return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    } // private class HTreeCellRenderer
    */

    /**
     * ChangeIndexingDialog displays file index options.
     */
    private class ChangeIndexingDialog extends Dialog {
        private static final long serialVersionUID = 1048114401768228742L;
    
        Object result;
        
        private Button checkIndexType;
        private Button checkIndexOrder;
        private Button checkIndexNative;
        
        private boolean reloadFile;
        
        private FileFormat selectedFile;
        private int indexType;
        private int indexOrder;
        
        /**
         * constructs an UserOptionsDialog.
         * 
         * @param view
         *            The HDFView.
         */
        private ChangeIndexingDialog(Shell parent, int style, FileFormat viewSelectedFile) {
            super(parent, style);
    
            selectedFile = viewSelectedFile;
            indexType = selectedFile.getIndexType(null);
            indexOrder = selectedFile.getIndexOrder(null);
            reloadFile = false;
    
            /*
            Shell contentPane = getParent();
            contentPane.setLayout(new BorderLayout(8, 8));
            contentPane.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
    
            JPanel indexP = new JPanel();
            TitledBorder tborder = new TitledBorder("Index Options");
            tborder.setTitleColor(Color.darkGray);
            indexP.setBorder(tborder);
            indexP.setLayout(new GridLayout(2, 1, 10, 10));
            indexP.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
            contentPane.add(indexP);
    
            JPanel pType = new JPanel();
            tborder = new TitledBorder("Indexing Type");
            tborder.setTitleColor(Color.darkGray);
            pType.setBorder(tborder);
            pType.setLayout(new GridLayout(1, 2, 8, 8));
            //checkIndexType = new JRadioButton("By Name", (indexType) == selectedFile.getIndexType("H5_INDEX_NAME"));
            //checkIndexType.setName("Index by Name");
            //pType.add(checkIndexType);
            //JRadioButton checkIndexCreateOrder = new JRadioButton("By Creation Order", (indexType) == selectedFile.getIndexType("H5_INDEX_CRT_ORDER"));
            //checkIndexCreateOrder.setName("Index by Creation Order");
            //pType.add(checkIndexCreateOrder);
            ButtonGroup bTypegrp = new ButtonGroup();
            //bTypegrp.add(checkIndexType);
            //bTypegrp.add(checkIndexCreateOrder);
            indexP.add(pType);
    
            JPanel pOrder = new JPanel();
            tborder = new TitledBorder("Indexing Order");
            tborder.setTitleColor(Color.darkGray);
            pOrder.setBorder(tborder);
            pOrder.setLayout(new GridLayout(1, 3, 8, 8));
            //checkIndexOrder = new JRadioButton("Increments", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_INC"));
            //checkIndexOrder.setName("Index Increments");
            //pOrder.add(checkIndexOrder);
            //JRadioButton checkIndexDecrement = new JRadioButton("Decrements", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_DEC"));
            //checkIndexDecrement.setName("Index Decrements");
            //pOrder.add(checkIndexDecrement);
            //checkIndexNative = new JRadioButton("Native", (indexOrder) == selectedFile.getIndexOrder("H5_ITER_NATIVE"));
            //checkIndexNative.setName("Index Native");
            //pOrder.add(checkIndexNative);
            ButtonGroup bOrdergrp = new ButtonGroup();
            //bOrdergrp.add(checkIndexOrder);
            //bOrdergrp.add(checkIndexDecrement);
            //bOrdergrp.add(checkIndexNative);
            indexP.add(pOrder);
    
            JPanel buttonP = new JPanel();
            //JButton b = new JButton("Reload File");
            //b.setName("Reload File");
            //b.setActionCommand("Reload File");
            //b.addActionListener(this);
            //buttonP.add(b);
            //b = new JButton("Cancel");
            //b.setName("Cancel");
            //b.setActionCommand("Cancel");
            //b.addActionListener(this);
            //buttonP.add(b);
    
            contentPane.add("Center", indexP);
            contentPane.add("South", buttonP);
    
            // locate the parent dialog
            Point l = getParent().getLocation();
            l.x += 250;
            l.y += 80;
            setLocation(l);
            validate();
            pack();
            */
        }
        
        //public void actionPerformed(ActionEvent e) {
        //    String cmd = e.getActionCommand();
    
        //    if (cmd.equals("Reload File")) {
        //        setIndexOptions();
        //        setVisible(false);
        //    }
        //    else if (cmd.equals("Cancel")) {
        //        reloadFile = false;
        //        setVisible(false);
        //    }
        //}
    
        private void setIndexOptions() {
            //if (checkIndexType.isSelected())
            //    selectedFile.setIndexType(selectedFile.getIndexType("H5_INDEX_NAME"));
            //else
            //    selectedFile.setIndexType(selectedFile.getIndexType("H5_INDEX_CRT_ORDER"));
            //indexType = selectedFile.getIndexType(null);
            
            //if (checkIndexOrder.isSelected())
            //    selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_INC"));
            //else if (checkIndexNative.isSelected())
            //    selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_NATIVE"));
            //else
            //    selectedFile.setIndexOrder(selectedFile.getIndexOrder("H5_ITER_DEC"));
            //indexOrder = selectedFile.getIndexOrder(null);
            
            reloadFile = true;
        }
    
        public int getIndexType() {
            return indexType;
        }
    
        public int getIndexOrder() {
            return indexOrder;
        }
    
        public boolean isreloadFile() {
            return reloadFile;
        }
        
        public Object open() {
        	Shell parent = getParent();
        	Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        	shell.setText("Indexing options");
        	
        	// Creation code
        	
        	
        	shell.open();
        	Display display = parent.getDisplay();
        	while (!shell.isDisposed()) {
        		if (!display.readAndDispatch()) display.sleep();
        	}
        	return result;
        }
    }
}
