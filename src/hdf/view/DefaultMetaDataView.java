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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.CellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * DefaultMetadataView is an dialog window used to show data properties. Data
 * properties include attributes and general information such as object type,
 * data type and data space.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DefaultMetaDataView extends JDialog implements ActionListener, MetaDataView {
    private static final long serialVersionUID = 7891048909810508761L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultMetaDataView.class);

    /**
     * The main HDFView.
     */
    private ViewManager       viewer;

    /** The HDF data object */
    private HObject           hObject;

    private JTabbedPane       tabbedPane       = null;
    private JTextArea         attrContentArea;
    private JTable            attrTable;  // table to hold a list of attributes
    private DefaultTableModel attrTableModel;
    private JLabel            attrNumberLabel;
    private int               numAttributes;
    private boolean           isH5, isH4;
    private byte[]            userBlock;
    private JTextArea         userBlockArea;
    private JButton           jamButton;

    private JTextField        linkField        = null;

    private FileFormat        fileFormat;
    private String            LinkTObjName;

    private int[]             libver;

    /**
     * Constructs a DefaultMetadataView with the given HDFView.
     */
    public DefaultMetaDataView(ViewManager theView) {
        super((JFrame) theView, false);
        setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);

        setName("DefaultMetaDataView");
        viewer = theView;
        hObject = viewer.getTreeView().getCurrentObject();
        fileFormat = hObject.getFileFormat();
        numAttributes = 0;
        userBlock = null;
        userBlockArea = null;
        libver = new int[2];

        if (hObject == null) {
            dispose();
        }
        else if (hObject.getPath() == null) {
            setTitle("Properties - " + hObject.getName());
        }
        else {
            setTitle("Properties - " + hObject.getPath() + hObject.getName());
        }

        isH5 = hObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        isH4 = hObject.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4));

        tabbedPane = new JTabbedPane();
        // get the metadata information before add GUI components */
        try {
            log.trace("DefaultMetaDataView: start");
            hObject.getMetadata();
        }
        catch (Exception ex) {
        	log.debug("get the metadata information before add GUI components:", ex);
        }
        tabbedPane.addTab("General", createGeneralPropertyPanel());
        tabbedPane.addTab("Attributes", createAttributePanel());

        boolean isRoot = ((hObject instanceof Group) && ((Group) hObject).isRoot());
        if (isH5 && isRoot) {
            // add panel to display user block
            tabbedPane.addTab("User Block", createUserBlockPanel());
        }
        tabbedPane.setSelectedIndex(0);

        if (isH5) {
            if (hObject.getLinkTargetObjName() != null) {
                LinkTObjName = hObject.getLinkTargetObjName();
            }
        }
        JPanel bPanel = new JPanel();
        bPanel.setName("MetaDataClose");
        JButton b = new JButton("  Close  ");
        b.setName("Close");
        b.setMnemonic(KeyEvent.VK_C);
        b.setActionCommand("Close");
        b.addActionListener(this);
        bPanel.add(b);

        // Add the tabbed pane to this panel.
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setName("MetaDataContent");
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPane.setPreferredSize(new Dimension(620, 400));

        contentPane.add("Center", tabbedPane);
        contentPane.add("South", bPanel);

        // locate the H5Property dialog
        Point l = getParent().getLocation();
        l.x += 250;
        l.y += 80;
        setLocation(l);
        pack();
        setVisible(true);
    }

    @SuppressWarnings("rawtypes")
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String cmd = e.getActionCommand();

        if (cmd.equals("Close")) {
            if (isH5 && linkField != null) checkLinkTargetChanged();

            dispose();
        }
        else if (cmd.equals("Add attribute")) {
            addAttribute(hObject);
        }
        else if (cmd.equals("Delete attribute")) {
            deleteAttribute(hObject);
        }
        else if (cmd.equals("Jam user block")) {
            writeUserBlock();
        }
        else if (cmd.equals("Display user block as")) {
            int type = 0;
            String typeName = (String) ((JComboBox) source).getSelectedItem();
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
            JOptionPane.showMessageDialog(this, "Parent group is null.", getTitle(), JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, ex, getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }

    /** returns the data object displayed in this data viewer */
    public HObject getDataObject() {
        return hObject;
    }

    /** Disposes of this dataobserver. */
    public void dispose() {
        super.dispose();
    }

    /** add an attribute to a data object. */
    public Attribute addAttribute(HObject obj) {
    	/*
        if (obj == null) {
            return null;
        }
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj.getFileFormat().getRootObject();
        NewAttributeDialog dialog = new NewAttributeDialog(this, obj, node.breadthFirstEnumeration());
        dialog.setVisible(true);

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

        attrTableModel.addRow(rowData);
        attrTableModel.fireTableRowsInserted(attrTableModel.getRowCount() - 1, attrTableModel.getRowCount() - 1);
        numAttributes++;
        attrContentArea.setText("");
        attrNumberLabel.setText("Number of attributes = " + numAttributes);

        return attr;
        */
    	
    	return null; // Remove when fixed
    }

    /** delete an attribute from a data object. */
    public Attribute deleteAttribute(HObject obj) {
        if (obj == null) {
            return null;
        }

        int idx = attrTable.getSelectedRow();
        if (idx < 0) {
            JOptionPane.showMessageDialog(getOwner(), "No attribute is selected.", getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        int option = JOptionPane.showConfirmDialog(this, "Do you want to delete the selected attribute?", getTitle(),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.NO_OPTION) {
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

        attrTableModel.removeRow(idx);
        numAttributes--;
        attrTableModel.fireTableRowsDeleted(idx, idx);

        attrContentArea.setText("");
        attrNumberLabel.setText("Number of attributes = " + numAttributes);

        return attr;
    }

    /**
     * Creates a panel used to display general information of HDF object.
     */
    private JPanel createGeneralPropertyPanel() {
    	/*
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        boolean isRoot = ((hObject instanceof Group) && ((Group) hObject).isRoot());
        FileFormat theFile = hObject.getFileFormat();

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        JPanel lp = new JPanel();
        lp.setLayout(new GridLayout(5, 1));

        if (isRoot) {
            lp.add(new JLabel("File Name: "));
            lp.add(new JLabel("File Path: "));
            lp.add(new JLabel("File Type: "));
            if (isH5) {
                try {
                    libver = hObject.getFileFormat().getLibBounds();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (((libver[0] == 0) || (libver[0] == 1)) && (libver[1] == 1))
                    lp.add(new JLabel("Library version: "));
            }
        }
        else {
            lp.add(new JLabel("Name: "));
            if (isH5) {
                if (hObject.getLinkTargetObjName() != null) lp.add(new JLabel("Link To Target: "));
            }
            lp.add(new JLabel("Path: "));
            lp.add(new JLabel("Type: "));

            // bug #926 to remove the OID, put it back on Nov. 20, 2008, --PC
            if (isH4) {
                lp.add(new JLabel("Tag, Ref:        "));
            }
            else {
                lp.add(new JLabel("Object Ref:       "));
            }
        }

        JPanel rp = new JPanel();
        rp.setLayout(new GridLayout(5, 1));

        JLabel nameField = new JLabel(hObject.getName());
        nameField.setName("namefield");
        rp.add(nameField);

        JPanel targetObjPanel = new JPanel();
        JButton ChangeTargetObjButton = new JButton("Change");
        ChangeTargetObjButton.setActionCommand("Change link target");
        ChangeTargetObjButton.addActionListener(this);

        if (isH5) {
            if (hObject.getLinkTargetObjName() != null) {
                linkField = new JTextField(hObject.getLinkTargetObjName());
                linkField.setName("linkField");
                targetObjPanel.setLayout(new BorderLayout());
                targetObjPanel.add(linkField, BorderLayout.CENTER);
                // targetObjPanel.add(ChangeTargetObjButton, BorderLayout.EAST);
                rp.add(targetObjPanel);
            }
        }

        JLabel pathField = new JLabel();
        if (isRoot) {
            pathField.setText((new File(hObject.getFile())).getParent());
        }
        else {
            pathField.setText(hObject.getPath());
        }
        rp.add(pathField);

        String typeStr = "Unknown";
        String fileInfo = "";
        if (isRoot) {
            long size = 0;
            try {
                size = (new File(hObject.getFile())).length();
            }
            catch (Exception ex) {
                size = -1;
            }
            size /= 1024;

            int groupCount = 0, datasetCount = 0;
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) theFile.getRootNode();
            DefaultMutableTreeNode theNode = null;
            Enumeration<?> local_enum = root.depthFirstEnumeration();
            while (local_enum.hasMoreElements()) {
                theNode = (DefaultMutableTreeNode) local_enum.nextElement();
                if (theNode.getUserObject() instanceof Group) {
                    groupCount++;
                }
                else {
                    datasetCount++;
                }
            }
            fileInfo = "size=" + size + "K,  groups=" + groupCount + ",  datasets=" + datasetCount;
        }

        if (isRoot) {
            if (isH5) {
                typeStr = "HDF5,  " + fileInfo;
            }
            else if (isH4) {
                typeStr = "HDF4,  " + fileInfo;
            }
            else {
                typeStr = fileInfo;
            }
        }
        else if (isH5) {
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
        }
        else if (isH4) {
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
        }
        else {
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

        JLabel typeField = new JLabel(typeStr);
        rp.add(typeField);

        if (isRoot && isH5) {
            String libversion = null;
            if ((libver[0] == 0) && (libver[1] == 1))
                libversion = "Earliest and Latest";
            else if ((libver[0] == 1) && (libver[1] == 1)) libversion = "Latest and Latest";
            JLabel libverbound = new JLabel(libversion);
            libverbound.setName("libverbound");
            rp.add(libverbound);
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
            JLabel oidField = new JLabel(oidStr);
            rp.add(oidField);
        }

        JPanel tmpP = new JPanel();
        tmpP.setLayout(new BorderLayout());
        tmpP.add("West", lp);
        tmpP.add("Center", rp);
        tmpP.setBorder(new TitledBorder(""));

        topPanel.add("North", new JLabel(""));
        topPanel.add("Center", tmpP);

        JPanel infoPanel = null;
        if (hObject instanceof Group) {
            infoPanel = createGroupInfoPanel((Group) hObject);
        }
        else if (hObject instanceof Dataset) {
            infoPanel = createDatasetInfoPanel((Dataset) hObject);
        }
        else if (hObject instanceof Datatype) {
            infoPanel = createNamedDatatypeInfoPanel((Datatype) hObject);
        }

        panel.add(topPanel, BorderLayout.NORTH);
        if (infoPanel != null) {
            panel.add(infoPanel, BorderLayout.CENTER);
        }

        return panel;
        */
    	
    	return null; // Remove when fixed
    }

    /**
     * Creates a panel used to display HDF group information.
     */
    private JPanel createGroupInfoPanel(Group g) {
        JPanel panel = new JPanel();

        List<?> mlist = g.getMemberList();
        if (mlist == null) {
            return panel;
        }

        int n = mlist.size();
        if (n <= 0) {
            return panel;
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
        JTable table = new JTable(rowData, columnNames) {
            private static final long serialVersionUID = -834321929059590629L;

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setName("GroupInfo");
        table.setCellSelectionEnabled(false);

        // set cell height for large fonts
        int cellRowHeight = Math.max(16, table.getFontMetrics(table.getFont()).getHeight());
        table.setRowHeight(cellRowHeight);

        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroller = new JScrollPane(table);

        panel.setLayout(new BorderLayout());
        if (g.getNumberOfMembersInFile() < ViewProperties.getMaxMembers()) {
            panel.add(new JLabel("Number of members: " + n), BorderLayout.NORTH);
        }
        else {
            panel.add(new JLabel("Number of members: " + n + " (in memory), " + g.getNumberOfMembersInFile()
                    + " (in file)"), BorderLayout.NORTH);
        }
        panel.add(scroller, BorderLayout.CENTER);
        panel.setBorder(new TitledBorder("Group Members"));

        return panel;
    }

    private JPanel createNamedDatatypeInfoPanel(Datatype t) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JTextArea infoArea = new JTextArea(t.getDatatypeDescription());
        infoArea.setEditable(false);

        panel.add(infoArea, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates a panel used to display HDF dataset information.
     */
    private JPanel createDatasetInfoPanel(Dataset d) {
        JPanel lp = new JPanel();
        lp.setLayout(new GridLayout(4, 1));
        lp.add(new JLabel("No. of Dimension(s): "));
        lp.add(new JLabel("Dimension Size(s): "));
        lp.add(new JLabel("Max Dimension Size(s): "));
        lp.add(new JLabel("Data Type: "));

        JPanel rp = new JPanel();
        rp.setLayout(new GridLayout(4, 1));
        log.trace("createDatasetInfoPanel: start");

        if (d.getRank() <= 0) {
            d.init();
        }
        JTextField txtf = new JTextField("" + d.getRank());
        txtf.setName("dimensions");
        txtf.setEditable(false);
        rp.add(txtf);
        log.trace("createDatasetInfoPanel inited");

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
        txtf = new JTextField(dimStr);
        txtf.setName("dimensionsize");
        txtf.setEditable(false);
        rp.add(txtf);

        txtf = new JTextField(maxDimStr);
        txtf.setEditable(false);
        rp.add(txtf);

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

        txtf = new JTextField(typeStr);
        txtf.setEditable(false);
        rp.add(txtf);

        JPanel infoP = new JPanel();
        infoP.setLayout(new BorderLayout());
        infoP.add(lp, BorderLayout.WEST);
        infoP.add(rp, BorderLayout.CENTER);
        infoP.setBorder(new TitledBorder(""));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(infoP, BorderLayout.NORTH);
        panel.setBorder(new TitledBorder("Dataspace and Datatype"));

        // add compound datatype information
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
                JTable table = new JTable(rowData, columnNames) {
                    private static final long serialVersionUID = -1517773307922536859L;

                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                table.setName("CompoundMetaData");
                table.setCellSelectionEnabled(false);
                table.getTableHeader().setReorderingAllowed(false);
                panel.add(new JScrollPane(table), BorderLayout.CENTER);

                // set cell height for large fonts
                int cellRowHeight = Math.max(16, table.getFontMetrics(table.getFont()).getHeight());
                table.setRowHeight(cellRowHeight);
            } // if (n > 0)
        } // if (d instanceof Compound)

        // // add compression and data layout information
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

        JPanel bPanel = new JPanel();
        bPanel.setBorder(new TitledBorder(""));
        bPanel.setLayout(new BorderLayout());
        lp = new JPanel();
        lp.setName("genmetainfo");
        lp.setLayout(new GridLayout(5, 1));
        lp.add(new JLabel("Chunking: "));
        lp.add(new JLabel("Compression: "));
        lp.add(new JLabel("Filters: "));
        lp.add(new JLabel("Storage: "));
        lp.add(new JLabel("Fill value: "));
        bPanel.add(lp, BorderLayout.WEST);

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

        rp = new JPanel();
        rp.setName("genmetadata");
        rp.setLayout(new GridLayout(5, 1));
        JLabel rpchunk = new JLabel(chunkInfo);
        rpchunk.setName("chunkdata");
        rp.add(rpchunk);
        JLabel rpcomp = new JLabel(d.getCompression());
        rpcomp.setName("compressiondata");
        rp.add(rpcomp);
        JLabel rpfilt = new JLabel(d.getFilters());
        rpfilt.setName("filterdata");
        rp.add(rpfilt);
        JLabel rpstore = new JLabel(d.getStorage());
        rpstore.setName("storagedata");
        rp.add(rpstore);
        JLabel rpfillval = new JLabel(fillValueInfo);
        rpfillval.setName("fillvaluedata");
        rp.add(rpfillval);
        bPanel.add(rp, BorderLayout.CENTER);

        panel.add(bPanel, BorderLayout.SOUTH);
        log.trace("createDatasetInfoPanel: finish");

        return panel;
    }

    /**
     * Creates a panel used to display attribute information.
     */
    private JPanel createAttributePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        // panel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        attrNumberLabel = new JLabel("Number of attributes = 0");
        topPanel.add(attrNumberLabel, BorderLayout.WEST);

        FileFormat theFile = hObject.getFileFormat();
        JPanel bPanel = new JPanel();
        JButton b = new JButton(" Add ");
        b.setName("Add");
        b.setMnemonic('A');
        b.addActionListener(this);
        b.setActionCommand("Add attribute");
        bPanel.add(b);
        b.setEnabled(!theFile.isReadOnly());

        if (isH5) {
            // deleting is not supported by HDF4
            b = new JButton("Delete");
            b.setName("Delete");
            b.setMnemonic('D');
            b.addActionListener(this);
            b.setActionCommand("Delete attribute");
            bPanel.add(b);
            b.setEnabled(!theFile.isReadOnly());
        }
        topPanel.add(bPanel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        List<?> attrList = null;
        log.trace("createAttributePanel: start");

        try {
            attrList = hObject.getMetadata();
        }
        catch (Exception ex) {
            attrList = null;
        }
        if (attrList != null) {
            numAttributes = attrList.size();
        }
        log.trace("createAttributePanel:  isH5={} numAttributes={}", isH5, numAttributes);

        String[] columnNames = { "Name", "Value", "Type", "Array Size" };
        attrTableModel = new DefaultTableModel(columnNames, numAttributes);

        attrTable = new JTable(attrTableModel) {
            private static final long serialVersionUID = 2590244645972259454L;
            int                       lastSelectedRow  = -1;
            int                       lastSelectedCol  = -1;

            public boolean isCellEditable(int row, int column) {
                return ((column == 1) || (isH5 && (column == 0))); 
                // only attribute value and name can be changed
            }

            public void editingStopped(ChangeEvent e) {
                int row = getEditingRow();
                int col = getEditingColumn();
                String oldValue = (String) getValueAt(row, col);

                super.editingStopped(e);

                Object source = e.getSource();

                if (source instanceof CellEditor) {
                    CellEditor editor = (CellEditor) source;
                    String newValue = (String) editor.getCellEditorValue();
                    setValueAt(oldValue, row, col); // set back to what it is
                    updateAttributeValue(newValue, row, col);
                }
            }

            public boolean isCellSelected(int row, int col) {

                if ((getSelectedRow() == row) && (getSelectedColumn() == col)
                        && !((lastSelectedRow == row) && (lastSelectedCol == col))) {
                    // selection is changed
                    Object attrV = getValueAt(row, col);
                    if (attrV != null) {
                        attrContentArea.setText(attrV.toString());
                    }
                    lastSelectedRow = row;
                    lastSelectedCol = col;
                }

                return super.isCellSelected(row, col);
            }
        };

        attrTable.setName("attributes");
        attrTable.setRowSelectionAllowed(false);
        attrTable.setCellSelectionEnabled(true);
        attrTable.getTableHeader().setReorderingAllowed(false);
        attrTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set cell height for large fonts
        int cellRowHeight = Math.max(16, attrTable.getFontMetrics(attrTable.getFont()).getHeight());
        attrTable.setRowHeight(cellRowHeight);

        JScrollPane scroller1 = new JScrollPane(attrTable);
        attrContentArea = new JTextArea();
        attrContentArea.setLineWrap(true);
        attrContentArea.setEditable(false);
        Insets m = new Insets(5, 5, 5, 5);
        attrContentArea.setMargin(m);

        JScrollPane scroller2 = new JScrollPane(attrContentArea);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroller1, scroller2);

        // set the divider location
        int h = Math.min((numAttributes + 2) * attrTable.getRowHeight(), scroller1.getPreferredSize().height - 40);
        splitPane.setDividerLocation(h);
        panel.add(splitPane, BorderLayout.CENTER);

        if (attrList == null) {
            log.trace("createAttributePanel:  attrList == null");
            return panel;
        }

        Attribute attr = null;
        String name, type, size;
        attrNumberLabel.setText("Number of attributes = " + numAttributes);
        for (int i = 0; i < numAttributes; i++) {
            attr = (Attribute) attrList.get(i);
            name = attr.getName();
            type = attr.getType().getDatatypeDescription();
            log.trace("createAttributePanel:  attr[{}] is {} as {}", i, name, type);

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

            if (attr.getProperty("field") !=null) {
            	String fieldInfo = " {Field: "+attr.getProperty("field")+"}";
            	attrTable.setValueAt(name+fieldInfo, i, 0);
            } 
            else
                attrTable.setValueAt(name, i, 0);
            
            attrTable.setValueAt(attr.toString(", "), i, 1);
            attrTable.setValueAt(type, i, 2);
            attrTable.setValueAt(size, i, 3);
        } // for (int i=0; i<n; i++)

        log.trace("createAttributePanel: finish");
        return panel;
    }

    /**
     * Creates a panel used to display HDF5 user block.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private JPanel createUserBlockPanel() {
        JPanel panel = new JPanel();

        userBlock = DefaultFileFilter.getHDF5UserBlock(hObject.getFile());

        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        userBlockArea = new JTextArea();
        userBlockArea.setEditable(true);
        userBlockArea.setLineWrap(true);
        Insets m = new Insets(5, 5, 5, 5);
        userBlockArea.setMargin(m);

        String[] displayChoices = { "Text", "Binary", "Octal", "Hexadecimal", "Decimal" };
        JComboBox userBlockDisplayChoice = new JComboBox(displayChoices);
        userBlockDisplayChoice.setSelectedIndex(0);
        userBlockDisplayChoice.addActionListener(this);
        userBlockDisplayChoice.setEditable(false);
        userBlockDisplayChoice.setActionCommand("Display user block as");

        JLabel sizeLabel = new JLabel("Header Size (Bytes): 0");
        jamButton = new JButton("Save User Block");
        jamButton.setActionCommand("Jam user block");
        jamButton.addActionListener(this);
        JPanel topPane = new JPanel();
        topPane.setLayout(new BorderLayout(10, 5));
        topPane.add(new JLabel("Display As:"), BorderLayout.WEST);
        JPanel topPane0 = new JPanel();
        topPane0.setLayout(new GridLayout(1, 2, 10, 5));
        topPane0.add(userBlockDisplayChoice);
        topPane0.add(sizeLabel);
        topPane.add(topPane0, BorderLayout.CENTER);
        topPane.add(jamButton, BorderLayout.EAST);

        JScrollPane scroller = new JScrollPane(userBlockArea);
        panel.add(topPane, BorderLayout.NORTH);
        panel.add(scroller, BorderLayout.CENTER);

        int headSize = 0;
        if (userBlock != null) {
            headSize = showUserBlockAs(0);
            sizeLabel.setText("Header Size (Bytes): " + headSize);
        }
        else {
            userBlockDisplayChoice.setEnabled(false);
        }

        return panel;
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

        String attrName = (String) attrTable.getValueAt(row, 0);
        List<?> attrList = null;
        try {
            attrList = hObject.getMetadata();
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(getOwner(), ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(getOwner(), "More data value needed: " + newValue, getTitle(),
                        JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(getOwner(), ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (isUnsigned && (d < 0)) {
                    JOptionPane.showMessageDialog(getOwner(), "Negative value for unsigned integer: " + theToken,
                            getTitle(), JOptionPane.ERROR_MESSAGE);
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
                            JOptionPane.showMessageDialog(getOwner(), "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, getTitle(), JOptionPane.ERROR_MESSAGE);
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
                            JOptionPane.showMessageDialog(getOwner(), "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, getTitle(), JOptionPane.ERROR_MESSAGE);
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
                            JOptionPane.showMessageDialog(getOwner(), "Data is out of range[" + min + ", " + max
                                    + "]: " + theToken, getTitle(), JOptionPane.ERROR_MESSAGE);
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
                                    JOptionPane.showMessageDialog(getOwner(), "Data is out of range[" + min + ", " + max
                                            + "]: " + theToken, getTitle(), JOptionPane.ERROR_MESSAGE);
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
                                JOptionPane.showMessageDialog(getOwner(), "Data is out of range[" + min + ", " + max
                                        + "]: " + theToken, getTitle(), JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(getOwner(), ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // update the attribute table
            attrTable.setValueAt(attr.toString(", "), row, 1);
        }

        if ((col == 0) && isH5) { // To change attribute name
            log.trace("updateAttributeValue: change attribute name");
            try {
                hObject.getFileFormat().renameAttribute(hObject, attrName, newValue);
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(getOwner(), ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // update the attribute table
            attrTable.setValueAt(newValue, row, 0);
        }
        if (hObject instanceof ScalarDS) {
            ScalarDS ds = (ScalarDS) hObject;
            try {
                log.trace("updateAttributeValue: ScalarDS:updateMetadata");
                ds.updateMetadata(attr);
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(getOwner(), ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            log.trace("updateAttributeValue: hObject is not instanceof ScalarDS");
        }
    }

    private void writeUserBlock() {
        if (!isH5) {
            return;
        }

        int blkSize0 = 0;
        if (userBlock != null) {
            blkSize0 = userBlock.length;
            // The super block space is allocated by offset 0, 512, 1024, 2048,
            // etc
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
                JOptionPane.showMessageDialog(this, "Can't open output file: " + hObject.getFile(), getTitle(),
                        JOptionPane.ERROR_MESSAGE);
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

            JOptionPane.showMessageDialog(this, "Saving user block is successful.", getTitle(),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            // must rewrite the whole file
            // must rewrite the whole file
            int op = JOptionPane.showConfirmDialog(this,
                    "The user block to write is " + blkSize1 + " (bytes),\n"
                            + "which is larger than the user block space in file " + blkSize0 + " (bytes).\n"
                            + "To expand the user block, the file must be rewriten.\n\n"
                            + "Do you want to replace the current file? Click "
                            + "\n\"Yes\" to replace the current file," + "\n\"No\" to save to a different file, "
                            + "\n\"Cancel\" to quit without saving the change.\n\n ", getTitle(),
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (op == JOptionPane.CANCEL_OPTION) {
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

            if (op == JOptionPane.NO_OPTION) {
                JFileChooser fchooser = new JFileChooser();
                fchooser.setFileFilter(DefaultFileFilter.getFileFilterHDF5());
                fchooser.setSelectedFile(new File(fout));

                int returnVal = fchooser.showSaveDialog(this);
                if (returnVal != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                File choosedFile = fchooser.getSelectedFile();
                if (choosedFile == null) {
                    return;
                }

                outFile = choosedFile;
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
                    JOptionPane.showMessageDialog(this, "Fail to write user block into file. ", getTitle(),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // close the file
            ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Close file");
            //((HDFView) viewer).actionPerformed(e);

            if (DefaultFileFilter.setHDF5UserBlock(fin, fout, buf)) {
                if (op == JOptionPane.NO_OPTION) {
                    fin = fout; // open the new file
                }
                else {
                    File oldFile = new File(fin);
                    boolean status = oldFile.delete();
                    if (status) {
                        outFile.renameTo(oldFile);
                    }
                    else {
                        JOptionPane.showMessageDialog(this,
                                "Cannot replace the current file.\nPlease save to a different file.", getTitle(),
                                JOptionPane.ERROR_MESSAGE);
                        outFile.delete();
                    }
                }
            }
            else {
                JOptionPane.showMessageDialog(this, "Fail to write user block into file. ", getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                outFile.delete();
            }

            // reopen the file
            dispose();
            e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Open file://" + fin);
            //((HDFView) viewer).actionPerformed(e);
        }
    }

}
