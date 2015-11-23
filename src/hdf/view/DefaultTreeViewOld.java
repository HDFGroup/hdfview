public class DefaultTreeViewOld implements TreeView {
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
