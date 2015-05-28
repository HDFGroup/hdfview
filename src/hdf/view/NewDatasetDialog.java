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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * NewDatasetDialog shows a message dialog requesting user input for creating a
 * new HDF4/5 dataset.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class NewDatasetDialog extends JDialog implements ActionListener, ItemListener, HyperlinkListener {
    private static final long serialVersionUID = 5381164938654184532L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewDatasetDialog.class);

    private JTextField        nameField, currentSizeField, maxSizeField, chunkSizeField, stringLengthField,
    fillValueField;

    @SuppressWarnings("rawtypes")
    private JComboBox         parentChoice, classChoice, sizeChoice, endianChoice, rankChoice, compressionLevel;

    private JCheckBox         checkUnsigned, checkCompression, checkFillValue;

    private JRadioButton      checkContinguous, checkChunked;

    private JDialog           helpDialog;

    private boolean           isH5;

    /** a list of current groups */
    private List<Object>      groupList;

    private HObject           newObject;

    private FileFormat        fileFormat;

    private final Toolkit     toolkit;

    private final DataView    dataView;

    /**
     * Constructs NewDatasetDialog with specified list of possible parent
     * groups.
     * 
     * @param owner
     *            the owner of the input
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NewDatasetDialog(JFrame owner, Group pGroup, List<?> objs) {
        super(owner, "New Dataset...", true);

        helpDialog = null;
        newObject = null;
        dataView = null;

        fileFormat = pGroup.getFileFormat();
        toolkit = Toolkit.getDefaultToolkit();
        isH5 = pGroup.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        parentChoice = new JComboBox();
        groupList = new Vector<Object>();
        Object obj = null;
        Iterator<?> iterator = objs.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(obj);
                if (g.isRoot()) {
                    parentChoice.addItem(HObject.separator);
                }
                else {
                    parentChoice.addItem(g.getPath() + g.getName() + HObject.separator);
                }
            }
        }

        if (pGroup.isRoot()) {
            parentChoice.setSelectedItem(HObject.separator);
        }
        else {
            parentChoice.setSelectedItem(pGroup.getPath() + pGroup.getName() + HObject.separator);
        }

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        int w = 600 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 350 + (ViewProperties.getFontSize() - 12) * 10;
        contentPane.setPreferredSize(new Dimension(w, h));

        JButton okButton = new JButton("   Ok   ");
        okButton.setName("OK");
        okButton.setActionCommand("Ok");
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setName("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);

        JButton helplButton = new JButton("Help");
        helplButton.setName("Help");
        helplButton.setMnemonic(KeyEvent.VK_H);
        helplButton.setActionCommand("Show help");
        helplButton.addActionListener(this);

        // set OK and CANCEL buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(helplButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // set NAME and PARENT GROUP panel
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout(5, 5));
        JPanel tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(2, 1));
        tmpP.add(new JLabel("Dataset name: "));
        tmpP.add(new JLabel("Parent group: "));
        namePanel.add(tmpP, BorderLayout.WEST);
        tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(2, 1));
        tmpP.add(nameField = new JTextField());
        nameField.setName("datasetname");
        tmpP.add(parentChoice);
        namePanel.add(tmpP, BorderLayout.CENTER);
        contentPane.add(namePanel, BorderLayout.NORTH);

        // set DATATYPE
        JPanel typePanel = new JPanel();
        typePanel.setLayout(new GridLayout(2, 4, 15, 3));
        TitledBorder border = new TitledBorder("Datatype");
        border.setTitleColor(Color.gray);
        typePanel.setBorder(border);

        stringLengthField = new JTextField("String length");
        stringLengthField.setName("datasetstringlen");
        stringLengthField.setEnabled(false);

        endianChoice = new JComboBox();
        endianChoice.setName("datasetendian");
        classChoice = new JComboBox();
        classChoice.setName("datasetclass");
        sizeChoice = new JComboBox();
        sizeChoice.setName("datasetsize");
        endianChoice.setEnabled(isH5);

        classChoice.addItem("INTEGER");
        classChoice.addItem("FLOAT");
        classChoice.addItem("CHAR");

        if (isH5) {
            classChoice.addItem("STRING");
            classChoice.addItem("REFERENCE");
            classChoice.addItem("ENUM");
            classChoice.addItem("VLEN_INTEGER");
            classChoice.addItem("VLEN_FLOAT");
            classChoice.addItem("VLEN_STRING");
            sizeChoice.addItem("NATIVE");
            endianChoice.addItem("NATIVE");
            endianChoice.addItem("LITTLE ENDIAN");
            endianChoice.addItem("BIG ENDIAN");
        }
        else {
            sizeChoice.addItem("DEFAULT");
            endianChoice.addItem("DEFAULT");
        }
        sizeChoice.addItem("8");
        sizeChoice.addItem("16");
        sizeChoice.addItem("32");
        sizeChoice.addItem("64");

        typePanel.add(new JLabel("Datatype class"));
        typePanel.add(new JLabel("Size (bits)"));
        typePanel.add(new JLabel("Byte ordering"));
        checkUnsigned = new JCheckBox("Unsigned");
        checkUnsigned.setName("datasetchkunsigned");
        typePanel.add(checkUnsigned);

        typePanel.add(classChoice);
        typePanel.add(sizeChoice);
        typePanel.add(endianChoice);
        typePanel.add(stringLengthField);

        // set DATATSPACE
        JPanel spacePanel = new JPanel();
        spacePanel.setLayout(new GridLayout(2, 3, 15, 3));
        border = new TitledBorder("Dataspace");
        border.setTitleColor(Color.gray);
        spacePanel.setBorder(border);

        rankChoice = new JComboBox();
        rankChoice.setName("datasetrank");
        for (int i = 1; i < 33; i++) {
            rankChoice.addItem(String.valueOf(i));
        }
        rankChoice.setSelectedIndex(1);

        currentSizeField = new JTextField("1 x 1");
        currentSizeField.setName("currentsize");
        maxSizeField = new JTextField("");
        spacePanel.add(new JLabel("No. of dimensions"));
        spacePanel.add(new JLabel("Current size"));
        spacePanel.add(new JLabel(""));
        spacePanel.add(rankChoice);
        spacePanel.add(currentSizeField);
        JButton jb = new JButton("Set Max Size");
        jb.setActionCommand("Set max size");
        jb.addActionListener(this);
        spacePanel.add(jb);
        // spacePanel.add(maxSizeField);

        // set storage layout and data compression
        JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BorderLayout());
        border = new TitledBorder("Storage Properties");
        border.setTitleColor(Color.gray);
        layoutPanel.setBorder(border);

        checkContinguous = new JRadioButton("Contiguous");
        checkContinguous.setName("datasetcontinguous");
        checkContinguous.setSelected(true);
        checkChunked = new JRadioButton("Chunked (size) ");
        checkChunked.setName("datasetchunk");
        ButtonGroup bgroup = new ButtonGroup();
        bgroup.add(checkChunked);
        bgroup.add(checkContinguous);
        chunkSizeField = new JTextField("1 x 1");
        chunkSizeField.setName("datasetchunksize");
        chunkSizeField.setEnabled(false);
        checkCompression = new JCheckBox("gzip (level) ");
        checkCompression.setName("datasetgzip");

        compressionLevel = new JComboBox();
        compressionLevel.setName("datasetlevel");
        for (int i = 0; i < 10; i++) {
            compressionLevel.addItem(String.valueOf(i));
        }
        compressionLevel.setSelectedIndex(6);
        compressionLevel.setEnabled(false);

        tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(2, 1));
        tmpP.add(new JLabel("Storage layout:  "));
        tmpP.add(new JLabel("Compression:  "));
        layoutPanel.add(tmpP, BorderLayout.WEST);

        tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(2, 1));

        // storage layout
        JPanel tmpP0 = new JPanel();
        tmpP0.setLayout(new GridLayout(1, 3, 0, 5));
        tmpP0.add(checkContinguous);
        JPanel tmpP00 = new JPanel();
        tmpP00.setLayout(new BorderLayout());
        tmpP00.add(checkChunked, BorderLayout.WEST);
        tmpP00.add(chunkSizeField, BorderLayout.CENTER);
        tmpP0.add(tmpP00);
        tmpP0.add(new JLabel(""));
        tmpP.add(tmpP0);

        tmpP0 = new JPanel();
        tmpP0.setLayout(new GridLayout(1, 2, 30, 5));

        // compression
        tmpP00 = new JPanel();
        tmpP00.setLayout(new BorderLayout());
        tmpP00.add(checkCompression, BorderLayout.WEST);
        tmpP00.add(compressionLevel, BorderLayout.CENTER);
        tmpP0.add(tmpP00);

        // fill values
        checkFillValue = new JCheckBox("Fill Value ");
        checkFillValue.setName("datasetchkfill");
        fillValueField = new JTextField("0");
        fillValueField.setName("datasetfillval");
        fillValueField.setEnabled(false);
        checkFillValue.setSelected(false);
        tmpP00 = new JPanel();
        tmpP00.setLayout(new BorderLayout());
        tmpP00.add(checkFillValue, BorderLayout.WEST);
        tmpP00.add(fillValueField, BorderLayout.CENTER);

        if (isH5)
            tmpP0.add(tmpP00);
        else
            tmpP0.add(new JLabel(""));

        tmpP.add(tmpP0);

        layoutPanel.add(tmpP, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new GridLayout(3, 1, 5, 10));
        infoPanel.add(typePanel);
        infoPanel.add(spacePanel);
        infoPanel.add(layoutPanel);
        contentPane.add(infoPanel, BorderLayout.CENTER);

        classChoice.addItemListener(this);
        sizeChoice.addItemListener(this);
        rankChoice.addItemListener(this);
        checkCompression.addItemListener(this);
        checkFillValue.addItemListener(this);
        checkContinguous.addItemListener(this);
        checkChunked.addItemListener(this);

        // locate the H5Property dialog
        Point l = owner.getLocation();
        l.x += 250;
        l.y += 80;
        setLocation(l);
        validate();
        pack();
    }

    /**
     * Constructs NewDatasetDialog with specified list of possible parent
     * groups.
     * 
     * @param owner
     *            the owner of the input
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NewDatasetDialog(JFrame owner, Group pGroup, List<?> objs, DataView observer) {
        super(owner, "New Dataset...", true);

        helpDialog = null;
        newObject = null;
        dataView = observer;

        fileFormat = pGroup.getFileFormat();
        toolkit = Toolkit.getDefaultToolkit();
        isH5 = pGroup.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        parentChoice = new JComboBox();
        groupList = new Vector<Object>();
        Object obj = null;
        Iterator<?> iterator = objs.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(obj);
                if (g.isRoot()) {
                    parentChoice.addItem(HObject.separator);
                }
                else {
                    parentChoice.addItem(g.getPath() + g.getName() + HObject.separator);
                }
            }
        }

        if (pGroup.isRoot()) {
            parentChoice.setSelectedItem(HObject.separator);
        }
        else {
            parentChoice.setSelectedItem(pGroup.getPath() + pGroup.getName() + HObject.separator);
        }

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        int w = 400 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 120 + (ViewProperties.getFontSize() - 12) * 10;
        contentPane.setPreferredSize(new Dimension(w, h));

        JButton okButton = new JButton("   Ok   ");
        okButton.setActionCommand("Ok");
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
        tmpP.setLayout(new GridLayout(2, 1));
        tmpP.add(new JLabel("Dataset name: "));
        tmpP.add(new JLabel("Parent group: "));
        namePanel.add(tmpP, BorderLayout.WEST);
        tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(2, 1));
        tmpP.add(nameField = new JTextField(((HObject) observer.getDataObject()).getName() + "~copy", 40));
        tmpP.add(parentChoice);
        namePanel.add(tmpP, BorderLayout.CENTER);
        contentPane.add(namePanel, BorderLayout.CENTER);

        // locate the H5Property dialog
        Point l = owner.getLocation();
        l.x += 250;
        l.y += 80;
        setLocation(l);
        pack();
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Ok")) {
            if (dataView instanceof TableView) {
                newObject = createFromTable();
            }
            else if (dataView instanceof ImageView) {
                newObject = createFromImage();
            }
            else if (dataView == null) {
                newObject = createFromScratch();
            }

            if (newObject != null) {
                dispose();
            }
        }
        if (cmd.equals("Cancel")) {
            newObject = null;
            dispose();
            ((Vector<Object>) groupList).setSize(0);
        }
        else if (cmd.equals("Show help")) {
            if (helpDialog == null) {
                createHelpDialog();
            }
            helpDialog.setVisible(true);
        }
        else if (cmd.equals("Hide help")) {
            if (helpDialog != null) {
                helpDialog.setVisible(false);
            }
        }
        else if (cmd.equals("Set max size")) {
            String strMax = maxSizeField.getText();
            if (strMax == null || strMax.length() < 1) strMax = currentSizeField.getText();

            String msg = JOptionPane.showInputDialog(this, "Enter max dimension sizes. \n"
                    + "Use \"unlimited\" for unlimited dimension size.\n\n" + "For example,\n" + "    200 x 100\n"
                    + "    100 x unlimited\n\n", strMax);

            if (msg == null || msg.length() < 1)
                maxSizeField.setText(currentSizeField.getText());
            else
                maxSizeField.setText(msg);

            checkMaxSize();
        }
    }

    @SuppressWarnings("unchecked")
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();

        if (source.equals(classChoice)) {
            int idx = classChoice.getSelectedIndex();
            sizeChoice.setSelectedIndex(0);
            endianChoice.setSelectedIndex(0);
            stringLengthField.setEnabled(false);

            if ((idx == 0) || (idx == 6)) { // INTEGER
                sizeChoice.setEnabled(true);
                endianChoice.setEnabled(isH5);
                checkUnsigned.setEnabled(true);

                if (sizeChoice.getItemCount() == 3) {
                    sizeChoice.removeItem("32");
                    sizeChoice.removeItem("64");
                    sizeChoice.addItem("8");
                    sizeChoice.addItem("16");
                    sizeChoice.addItem("32");
                    sizeChoice.addItem("64");
                }
            }
            else if ((idx == 1) || (idx == 7)) { // FLOAT
                sizeChoice.setEnabled(true);
                endianChoice.setEnabled(isH5);
                checkUnsigned.setEnabled(false);

                if (sizeChoice.getItemCount() == 5) {
                    sizeChoice.removeItem("16");
                    sizeChoice.removeItem("8");
                }
            }
            else if (idx == 2) { // CHAR
                sizeChoice.setEnabled(false);
                endianChoice.setEnabled(isH5);
                checkUnsigned.setEnabled(true);
            }
            else if (idx == 3) { // STRING
                sizeChoice.setEnabled(false);
                endianChoice.setEnabled(false);
                checkUnsigned.setEnabled(false);
                stringLengthField.setEnabled(true);
                stringLengthField.setText("String length");
            }
            else if (idx == 4) { // REFERENCE
                sizeChoice.setEnabled(false);
                endianChoice.setEnabled(false);
                checkUnsigned.setEnabled(false);
                stringLengthField.setEnabled(false);
            }
            else if (idx == 5) { // ENUM
                sizeChoice.setEnabled(true);
                checkUnsigned.setEnabled(true);
                stringLengthField.setEnabled(true);
                stringLengthField.setText("R=0,G=1,B=2,...");
            }
            else if (idx == 8) {
                sizeChoice.setEnabled(false);
                endianChoice.setEnabled(false);
                checkUnsigned.setEnabled(false);
                stringLengthField.setEnabled(false);
            }
        }
        else if (source.equals(sizeChoice)) {
            if (classChoice.getSelectedIndex() == 0) {
                checkUnsigned.setEnabled(true);
            }
        }
        else if (source.equals(rankChoice)) {
            int rank = rankChoice.getSelectedIndex() + 1;
            String currentSizeStr = "1";
            String maxSizeStr = "0";

            for (int i = 1; i < rank; i++) {
                currentSizeStr += " x 1";
                maxSizeStr += " x 0";
            }

            currentSizeField.setText(currentSizeStr);
            maxSizeField.setText(maxSizeStr);

            String currentStr = currentSizeField.getText();
            int idx = currentStr.lastIndexOf("x");
            String chunkStr = "1";

            if (rank <= 1) {
                chunkStr = currentStr;
            }
            else {
                for (int i = 1; i < rank - 1; i++) {
                    chunkStr += " x 1";
                }
                if (idx > 0) {
                    chunkStr += " x " + currentStr.substring(idx + 1);
                }
            }

            chunkSizeField.setText(chunkStr);
        }
        else if (source.equals(checkContinguous)) {
            chunkSizeField.setEnabled(false);
        }
        else if (source.equals(checkChunked)) {
            chunkSizeField.setEnabled(true);
            String chunkStr = "";
            StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
            int rank = rankChoice.getSelectedIndex() + 1;
            while (st.hasMoreTokens()) {
                long l = Math.max(1, Long.valueOf(st.nextToken().trim()) / (2 * rank));
                chunkStr += String.valueOf(l) + "x";
            }
            chunkStr = chunkStr.substring(0, chunkStr.lastIndexOf('x'));
            chunkSizeField.setText(chunkStr);
        }
        else if (source.equals(checkCompression)) {
            boolean isCompressed = checkCompression.isSelected();

            if (isCompressed && isH5) {
                if (!checkChunked.isSelected()) {
                    String currentStr = currentSizeField.getText();
                    int idx = currentStr.lastIndexOf("x");
                    String chunkStr = "1";

                    int rank = rankChoice.getSelectedIndex() + 1;
                    if (rank <= 1) {
                        chunkStr = currentStr;
                    }
                    else {
                        for (int i = 1; i < rank - 1; i++) {
                            chunkStr += " x 1";
                        }
                        if (idx > 0) {
                            chunkStr += " x " + currentStr.substring(idx + 1);
                        }
                    }

                    chunkSizeField.setText(chunkStr);
                }
                compressionLevel.setEnabled(true);
                checkContinguous.setEnabled(false);
                checkChunked.setSelected(true);
                chunkSizeField.setEnabled(true);
            }
            else {
                compressionLevel.setEnabled(isCompressed);
                checkContinguous.setEnabled(true);
            }
        }
        else if (source.equals(checkFillValue)) {
            fillValueField.setEnabled(checkFillValue.isSelected());
        }
    }

    /** check is the max size is valid */
    private void checkMaxSize() {
        boolean isChunkNeeded = false;
        String dimStr = currentSizeField.getText();
        String maxStr = maxSizeField.getText();
        StringTokenizer stMax = new StringTokenizer(maxStr, "x");
        StringTokenizer stDim = new StringTokenizer(dimStr, "x");

        if (stMax.countTokens() != stDim.countTokens()) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Wrong number of values in the max dimension size " + maxStr,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            maxSizeField.setText(null);
            return;
        }

        int rank = stDim.countTokens();
        long max = 0, dim = 0;
        long[] maxdims = new long[rank];
        for (int i = 0; i < rank; i++) {
            String token = stMax.nextToken().trim();

            token = token.toLowerCase();
            if (token.startsWith("u")) {
                max = -1;
                isChunkNeeded = true;
            }
            else {
                try {
                    max = Long.parseLong(token);
                }
                catch (NumberFormatException ex) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Invalid max dimension size: " + maxStr, getTitle(),
                            JOptionPane.ERROR_MESSAGE);
                    maxSizeField.setText(null);
                    return;
                }
            }

            token = stDim.nextToken().trim();
            try {
                dim = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Invalid dimension size: " + dimStr, getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (max != -1 && max < dim) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Invalid max dimension size: " + maxStr, getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                maxSizeField.setText(null);
                return;
            }
            else if (max > dim) {
                isChunkNeeded = true;
            }

            maxdims[i] = max;
        } // for (int i = 0; i < rank; i++)

        if (isH5) {
            if (isChunkNeeded && !checkChunked.isSelected()) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Chunking is required for the max dimensions of " + maxStr,
                        getTitle(), JOptionPane.ERROR_MESSAGE);
                checkChunked.setSelected(true);
            }
        }
        else {
            for (int i = 1; i < rank; i++) {
                if (maxdims[i] <= 0) {
                    maxSizeField.setText(currentSizeField.getText());
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Only dim[0] can be unlimited." + maxStr, getTitle(),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
    }

    /** Creates a dialog to show the help information. */
    private void createHelpDialog() {
        helpDialog = new JDialog(this, "Create New Dataset");

        JPanel contentPane = (JPanel) helpDialog.getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        int w = 500 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 400 + (ViewProperties.getFontSize() - 12) * 10;
        contentPane.setPreferredSize(new Dimension(w, h));

        JButton b = new JButton("  Ok  ");
        b.addActionListener(this);
        b.setActionCommand("Hide help");
        JPanel tmpP = new JPanel();
        tmpP.add(b);
        contentPane.add(tmpP, BorderLayout.SOUTH);

        JEditorPane infoPane = new JEditorPane();
        infoPane.setEditable(false);
        JScrollPane editorScrollPane = new JScrollPane(infoPane);
        contentPane.add(editorScrollPane, BorderLayout.CENTER);

        try {
            URL url = null, url2 = null, url3 = null;
            String rootPath = ViewProperties.getViewRoot();

            try {
                url = new URL("file:" + rootPath + "/lib/jhdfview.jar");
            }
            catch (java.net.MalformedURLException mfu) {
            	log.debug("help information:", mfu);
            }

            try {
                url2 = new URL("file:" + rootPath + "/");
            }
            catch (java.net.MalformedURLException mfu) {
            	log.debug("help information:", mfu);
            }

            try {
                url3 = new URL("file:" + rootPath + "/src/");
            }
            catch (java.net.MalformedURLException mfu) {
            	log.debug("help information:", mfu);
            }

            URL uu[] = { url, url2, url3 };
            URLClassLoader cl = new URLClassLoader(uu);
            URL u = cl.findResource("ncsa/hdf/view/NewDatasetHelp.html");

            infoPane.setPage(u);
            infoPane.addHyperlinkListener(this);
        }
        catch (Exception e) {
            infoPane.setContentType("text/html");
            StringBuffer buff = new StringBuffer();
            buff.append("<html>");
            buff.append("<body>");
            buff.append("ERROR: cannot load help information.");
            buff.append("</body>");
            buff.append("</html>");
            infoPane.setText(buff.toString());
        }

        Point l = helpDialog.getOwner().getLocation();
        l.x += 50;
        l.y += 80;
        helpDialog.setLocation(l);
        helpDialog.validate();
        helpDialog.pack();
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane) e.getSource();

            if (e instanceof HTMLFrameHyperlinkEvent) {
                HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
                HTMLDocument doc = (HTMLDocument) pane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(evt);
            }
            else {
                try {
                    pane.setPage(e.getURL());
                }
                catch (Throwable t) {
                    log.debug("JEditorPane hyperlink:", t);
                }
            }
        }
    }

    private HObject createFromScratch() {
        String name = null;
        Group pgroup = null;
        boolean isVLen = false;
        int rank = -1, gzip = -1, tclass = -1, tsize = -1, torder = -1, tsign = -1;
        long dims[], maxdims[] = null, chunks[] = null;

        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            toolkit.beep();
            JOptionPane
            .showMessageDialog(this, "Dataset name is not specified.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Dataset name cannot contain path.", getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectedIndex());

        if (pgroup == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Parent group is null.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // set datatype class
        int idx = classChoice.getSelectedIndex();
        if (idx == 0) {
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.isSelected()) {
                tsign = Datatype.SIGN_NONE;
            }
        }
        else if (idx == 1) {
            tclass = Datatype.CLASS_FLOAT;
        }
        else if (idx == 2) {
            tclass = Datatype.CLASS_CHAR;
            if (checkUnsigned.isSelected()) {
                tsign = Datatype.SIGN_NONE;
            }
        }
        else if (idx == 3) {
            tclass = Datatype.CLASS_STRING;
        }
        else if (idx == 4) {
            tclass = Datatype.CLASS_REFERENCE;
        }
        else if (idx == 5) {
            tclass = Datatype.CLASS_ENUM;
        }
        else if (idx == 6) {
            isVLen = true;
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.isSelected()) {
                tsign = Datatype.SIGN_NONE;
            }
        }
        else if (idx == 7) {
            isVLen = true;
            tclass = Datatype.CLASS_FLOAT;
        }
        else if (idx == 8) {
            isVLen = true;
            tclass = Datatype.CLASS_STRING;
        }

        // set datatype size/order
        idx = sizeChoice.getSelectedIndex();
        if (tclass == Datatype.CLASS_STRING) {
            if (isVLen) {
                tsize = -1;
            }
            else {
                int stringLength = 0;
                try {
                    stringLength = Integer.parseInt(stringLengthField.getText());
                }
                catch (NumberFormatException ex) {
                    stringLength = -1;
                }
    
                if (stringLength <= 0) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Invalid string length: " + stringLengthField.getText(),
                            getTitle(), JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                tsize = stringLength;
            }
        }
        else if (tclass == Datatype.CLASS_ENUM) {
            String enumStr = stringLengthField.getText();
            if ((enumStr == null) || (enumStr.length() < 1) || enumStr.endsWith("...")) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Invalid member values: " + stringLengthField.getText(),
                        getTitle(), JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        else if (tclass == Datatype.CLASS_REFERENCE) {
            tsize = 1;
        }
        else if (idx == 0) {
            tsize = Datatype.NATIVE;
        }
        else if (tclass == Datatype.CLASS_FLOAT) {
            tsize = idx * 4;
        }
        else {
            tsize = 1 << (idx - 1);
        }

        if ((tsize == 8) && !isH5 && (tclass == Datatype.CLASS_INTEGER)) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "HDF4 does not support 64-bit integer.", getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // set order
        idx = endianChoice.getSelectedIndex();
        if (idx == 0) {
            torder = Datatype.NATIVE;
        }
        else if (idx == 1) {
            torder = Datatype.ORDER_LE;
        }
        else {
            torder = Datatype.ORDER_BE;
        }

        rank = rankChoice.getSelectedIndex() + 1;
        StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Number of values in the current dimension size is less than " + rank,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        long l = 0;
        dims = new long[rank];
        String token = null;
        for (int i = 0; i < rank; i++) {
            token = st.nextToken().trim();
            try {
                l = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Invalid dimension size: " + currentSizeField.getText(),
                        getTitle(), JOptionPane.ERROR_MESSAGE);
                return null;
            }

            if (l <= 0) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Dimension size must be greater than zero.", getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }

            dims[i] = l;
        }

        String maxFieldStr = maxSizeField.getText();
        if (maxFieldStr != null && maxFieldStr.length() > 1) {
            st = new StringTokenizer(maxFieldStr, "x");
            if (st.countTokens() < rank) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Number of values in the max dimension size is less than " + rank,
                        getTitle(), JOptionPane.ERROR_MESSAGE);
                return null;
            }

            l = 0;
            maxdims = new long[rank];
            for (int i = 0; i < rank; i++) {
                token = st.nextToken().trim();

                token = token.toLowerCase();
                if (token.startsWith("u"))
                    l = -1;
                else {
                    try {
                        l = Long.parseLong(token);
                    }
                    catch (NumberFormatException ex) {
                        toolkit.beep();
                        JOptionPane.showMessageDialog(this, "Invalid max dimension size: " + maxSizeField.getText(),
                                getTitle(), JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                }

                if (l < -1) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Dimension size cannot be less than -1.", getTitle(),
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                else if (l == 0) {
                    l = dims[i];
                }

                maxdims[i] = l;
            }
        }

        chunks = null;
        if (checkChunked.isSelected()) {
            st = new StringTokenizer(chunkSizeField.getText(), "x");
            if (st.countTokens() < rank) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Number of values in the chunk size is less than " + rank,
                        getTitle(), JOptionPane.ERROR_MESSAGE);
                return null;
            }

            l = 0;
            chunks = new long[rank];
            for (int i = 0; i < rank; i++) {
                token = st.nextToken().trim();
                try {
                    l = Long.parseLong(token);
                }
                catch (NumberFormatException ex) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Invalid chunk dimension size: " + chunkSizeField.getText(),
                            getTitle(), JOptionPane.ERROR_MESSAGE);
                    return null;
                }

                if (l < 1) {
                    toolkit.beep();
                    JOptionPane.showMessageDialog(this, "Chunk size cannot be less than 1.", getTitle(),
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }

                chunks[i] = l;
            } // for (int i=0; i<rank; i++)

            long tchunksize = 1, tdimsize = 1;
            for (int i = 0; i < rank; i++) {
                tchunksize *= chunks[i];
                tdimsize *= dims[i];
            }

            if (tchunksize >= tdimsize) {
                toolkit.beep();
                int status = JOptionPane.showConfirmDialog(this, "Chunk size is equal/greater than the current size. "
                        + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() + "?", getTitle(),
                        JOptionPane.YES_NO_OPTION);
                if (status == JOptionPane.NO_OPTION) {
                    return null;
                }
            }

            if (tchunksize == 1) {
                toolkit.beep();
                int status = JOptionPane.showConfirmDialog(this,
                        "Chunk size is one, which may cause large memory overhead for large dataset."
                                + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() + "?",
                                getTitle(), JOptionPane.YES_NO_OPTION);
                if (status == JOptionPane.NO_OPTION) {
                    return null;
                }
            }

        } // if (checkChunked.isSelected())

        if (checkCompression.isSelected()) {
            gzip = compressionLevel.getSelectedIndex();
        }
        else {
            gzip = 0;
        }

        HObject obj = null;
        try {
            Datatype basedatatype = null;
            if (isVLen) {
                basedatatype = fileFormat.createDatatype(tclass, tsize, torder, tsign);
                tclass = Datatype.CLASS_VLEN;
            }
            Datatype datatype = fileFormat.createDatatype(tclass, tsize, torder, tsign, basedatatype);
            if (tclass == Datatype.CLASS_ENUM) {
                datatype.setEnumMembers(stringLengthField.getText());
            }
            String fillValue = null;
            if (fillValueField.isEnabled()) fillValue = fillValueField.getText();

            obj = fileFormat.createScalarDS(name, pgroup, datatype, dims, maxdims, chunks, gzip, fillValue, null);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex, getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return obj;
    }

    private HObject createFromTable() {
        HObject obj = null;

        String name = null;
        Group pgroup = null;

        name = nameField.getText();
        if (name == null) {
            toolkit.beep();
            JOptionPane
            .showMessageDialog(this, "Dataset name is not specified.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Dataset name cannot contain path.", getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectedIndex());
        if (pgroup == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Parent group is null.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        TableView tableView = (TableView) dataView;
        Object theData = tableView.getSelectedData();
        if (theData == null) {
            return null;
        }

        int w = tableView.getTable().getSelectedColumnCount();
        int h = tableView.getTable().getSelectedRowCount();
        Dataset dataset = (Dataset) tableView.getDataObject();
        if (dataset instanceof ScalarDS) {
            ScalarDS sd = (ScalarDS) dataset;
            if (sd.isUnsigned()) {
                theData = Dataset.convertToUnsignedC(theData, null);
            }
        }

        try {
            long[] dims = { h, w };
            obj = dataset.copy(pgroup, name, dims, theData);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return obj;
    }

    private HObject createFromImage() {
        HObject obj = null;
        String name = null;
        Group pgroup = null;

        name = nameField.getText();
        if (name == null) {
            toolkit.beep();
            JOptionPane
            .showMessageDialog(this, "Dataset name is not specified.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Dataset name cannot contain path.", getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectedIndex());
        if (pgroup == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Parent group is null.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        ImageView imageView = (ImageView) dataView;
        ScalarDS dataset = (ScalarDS) imageView.getDataObject();
        Object theData = imageView.getSelectedData();

        if (theData == null) {
            return null;
        }

        // in version 2.4, unsigned image data is converted to signed data
        // to write data, the data needs to converted back to unsigned.
        if (dataset.isUnsigned()) {
            theData = Dataset.convertToUnsignedC(theData, null);
        }

        int w = imageView.getSelectedArea().width;
        int h = imageView.getSelectedArea().height;

        try {
            long[] dims = null;
            if (isH5) {
                if (imageView.isTrueColor()) {
                    dims = new long[3];
                    if (imageView.isPlaneInterlace()) {
                        dims[0] = 3;
                        dims[1] = h;
                        dims[2] = w;
                    }
                    else {
                        dims[0] = h;
                        dims[1] = w;
                        dims[2] = 3;
                    }
                }
                else {
                    dims = new long[2];
                    dims[0] = h;
                    dims[1] = w;
                }
            }
            else {
                dims = new long[2];
                dims[0] = w;
                dims[1] = h;
            }

            obj = dataset.copy(pgroup, name, dims, theData);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
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

}
