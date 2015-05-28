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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewAttributeDialog displays components for adding new attribute.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class NewAttributeDialog extends JDialog implements ActionListener, ItemListener, HyperlinkListener {
    private static final long serialVersionUID                = 4883237570834215275L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewAttributeDialog.class);

    /** the default length of a string attribute */
    public static final int   DEFAULT_STRING_ATTRIBUTE_LENGTH = 256;

    /** the object which the attribute to be attached to */
    private HObject           hObject;

    private Attribute         newAttribute;

    /** TextField for entering the name of the dataset */
    private JTextField        nameField;

    /** The Choice of the datatypes */
    @SuppressWarnings("rawtypes")
    private JComboBox         classChoice, sizeChoice;

    private JCheckBox         checkUnsigned;

    /** TextField for entering the attribute value. */
    private JTextField        valueField;

    /** The Choice of the object list */
    @SuppressWarnings("rawtypes")
    private JComboBox         objChoice;

    private FileFormat        fileFormat;

    /** TextField for entering the length of the data array or string. */
    private JTextField        lengthField;

    private JLabel            arrayLengthLabel;

    private final boolean     isH5;

    private JDialog           helpDialog;

    private JRadioButton      h4GrAttrRadioButton;

    /**
     * Constructs NewAttributeDialog with specified object (dataset, group, or
     * image) which the new attribute to be attached to.
     * 
     * @param owner
     *            the owner of the input
     * @param obj
     *            the object which the attribute to be attached to.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NewAttributeDialog(Dialog owner, HObject obj, Enumeration<?> objList) {
        super(owner, "New Attribute...", true);

        hObject = obj;
        newAttribute = null;
        isH5 = obj.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        helpDialog = null;
        fileFormat = obj.getFileFormat();
        
        JPanel typeLabelPanel = new JPanel();
        typeLabelPanel.setLayout(new GridLayout(1, 4, 15, 3));
        JPanel typePanel = new JPanel();
        typePanel.setLayout(new GridLayout(1, 4, 15, 3));

        classChoice = new JComboBox();
        classChoice.setName("attrclass");
        sizeChoice = new JComboBox();
        sizeChoice.setName("attrsize");

        classChoice.addItem("INTEGER");
        classChoice.addItem("FLOAT");
        classChoice.addItem("CHAR");

        if (isH5) {
            classChoice.addItem("STRING");
            classChoice.addItem("REFERENCE");
            classChoice.addItem("VLEN_INTEGER");
            classChoice.addItem("VLEN_FLOAT");
            classChoice.addItem("VLEN_STRING");
        }
        sizeChoice.addItem("8");
        sizeChoice.addItem("16");
        sizeChoice.addItem("32");
        sizeChoice.addItem("64");

        typeLabelPanel.add(new JLabel("Datatype class"));
        typeLabelPanel.add(new JLabel("Size (bits)"));
        typeLabelPanel.add(new JLabel(" "));

        typePanel.add(classChoice);
        typePanel.add(sizeChoice);
        checkUnsigned = new JCheckBox("Unsigned");
        checkUnsigned.setName("attrchkunsigned");
        typePanel.add(checkUnsigned);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 10));
        int w = 500 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 220 + (ViewProperties.getFontSize() - 12) * 12;
        contentPane.setPreferredSize(new Dimension(w, h));

        JButton okButton = new JButton("   Ok   ");
        okButton.setName("OK");
        okButton.setActionCommand("Ok");
        okButton.setMnemonic(KeyEvent.VK_O);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setName("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);

        JButton helpButton = new JButton(" Help ");
        helpButton.setName("Help");
        helpButton.setActionCommand("Show help");
        helpButton.setMnemonic(KeyEvent.VK_H);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout(5, 5));
        JPanel p2 = new JPanel();
        p2.setLayout(new GridLayout(6, 1, 3, 3));
        p2.add(new JLabel("Name: "));
        p2.add(new JLabel(" "));
        p2.add(new JLabel("Type: "));
        p2.add(arrayLengthLabel = new JLabel("Array Size: "));
        p2.add(new JLabel("Value: "));
        p2.add(new JLabel("Object List: "));
        p.add("West", p2);

        JPanel typePane = new JPanel();
        typePane.setLayout(new BorderLayout());
        JPanel h4GattrPane = new JPanel();
        h4GattrPane.setLayout(new GridLayout(1, 2, 3, 3));
        ButtonGroup bg = new ButtonGroup();
        JRadioButton grAttr = new JRadioButton("GR");
        JRadioButton sdAttr = new JRadioButton("SD");
        bg.add(sdAttr);
        bg.add(grAttr);
        sdAttr.setSelected(true);
        h4GattrPane.add(sdAttr);
        h4GattrPane.add(grAttr);
        typePane.add(typePanel, BorderLayout.CENTER);
        typePane.add(h4GattrPane, BorderLayout.EAST);
        h4GrAttrRadioButton = grAttr;

        p2 = new JPanel();
        p2.setLayout(new GridLayout(6, 1, 3, 3));
        nameField = new JTextField("", 30);
        nameField.setName("attrname");
        p2.add(nameField);
        if (!isH5 && (obj instanceof Group) && ((Group) obj).isRoot()) {
            p2.add(typePane);
        }
        else {
            p2.add(typeLabelPanel);
            p2.add(typePanel);
        }
        lengthField = new JTextField("1");
        lengthField.setName("attrlength");
        p2.add(lengthField);
        valueField = new JTextField("0");
        valueField.setName("attrvalue");
        p2.add(valueField);
        objChoice = new JComboBox();
        objChoice.setName("attrobjn");
        p2.add(objChoice);
        p.add("Center", p2);

        contentPane.add("Center", p);

        p = new JPanel();
        p.add(okButton);
        p.add(cancelButton);
        p.add(helpButton);
        contentPane.add("South", p);

        classChoice.addItemListener(this);
        sizeChoice.addItemListener(this);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        helpButton.addActionListener(this);
        objChoice.addItemListener(this);
        objChoice.setEnabled(false);

        String str;
        HObject hobj;
        DefaultMutableTreeNode theNode;
        while (objList.hasMoreElements()) {
            theNode = (DefaultMutableTreeNode) objList.nextElement();
            hobj = (HObject) theNode.getUserObject();
            if (hobj instanceof Group) {
                if (((Group) hobj).isRoot()) continue;
            }
            str = hobj.getFullName();
            objChoice.addItem(str);
        }

        Point l = owner.getLocation();
        l.x += 50;
        l.y += 80;
        setLocation(l);
        pack();
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Ok")) {
            if (createAttribute()) {
                dispose();
            }
        }
        else if (cmd.equals("Cancel")) {
            newAttribute = null;
            dispose();
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
    }

    @SuppressWarnings("unchecked")
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();

        if (source.equals(classChoice)) {
            int idx = classChoice.getSelectedIndex();
            sizeChoice.setSelectedIndex(0);
            objChoice.setEnabled(false);
            lengthField.setEnabled(true);

            if ((idx == 0) || (idx == 5)) {
                sizeChoice.setEnabled(true);
                checkUnsigned.setEnabled(true);
                arrayLengthLabel.setText("Array Size: ");

                if (sizeChoice.getItemCount() == 2) {
                    sizeChoice.removeItem("32");
                    sizeChoice.removeItem("64");
                    sizeChoice.addItem("8");
                    sizeChoice.addItem("16");
                    sizeChoice.addItem("32");
                    sizeChoice.addItem("64");
                }
            }
            else if ((idx == 1) || (idx == 6)) {
                sizeChoice.setEnabled(true);
                checkUnsigned.setEnabled(false);
                arrayLengthLabel.setText("Array Size: ");

                if (sizeChoice.getItemCount() == 4) {
                    sizeChoice.removeItem("16");
                    sizeChoice.removeItem("8");
                }
            }
            else if (idx == 2) {
                sizeChoice.setEnabled(false);
                checkUnsigned.setEnabled(true);
                arrayLengthLabel.setText("Array Size: ");
            }
            else if (idx == 3) {
                sizeChoice.setEnabled(false);
                checkUnsigned.setEnabled(false);
                arrayLengthLabel.setText("String Length: ");
            }
            else if (idx == 4) {
                sizeChoice.setEnabled(false);
                checkUnsigned.setEnabled(false);
                lengthField.setText("1");
                lengthField.setEnabled(false);
                arrayLengthLabel.setText("Array Size: ");
                objChoice.setEnabled(true);
                valueField.setText("");
            }
            else if (idx == 7) {
                sizeChoice.setEnabled(false);
                checkUnsigned.setEnabled(false);
                lengthField.setEnabled(false);
            }
        }
        else if (source.equals(sizeChoice)) {
            if (classChoice.getSelectedIndex() == 0) {
                checkUnsigned.setEnabled(true);
            }
        }
        else if (source.equals(objChoice)) {
            String objName = (String) objChoice.getSelectedItem();

            if (e.getStateChange() != ItemEvent.SELECTED) return;

            long ref = -1;
            try {
                HObject obj = fileFormat.get(objName);
                ref = obj.getOID()[0];
            }
            catch (Exception ex) {
            	log.debug("object id:", ex);
            }

            if (ref > 0) {
                if (valueField.getText().length() > 1) {
                    valueField.setText(valueField.getText() + "," + ref);
                    StringTokenizer st = new StringTokenizer(valueField.getText(), ",");
                    lengthField.setText(String.valueOf(st.countTokens()));
                }
                else {
                    valueField.setText(String.valueOf(ref));
                    lengthField.setText("1");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean createAttribute() {
        int string_length = 0;
        int tclass = -1, tsize = -1, torder = -1, tsign = -1;
        boolean isVLen = false;
        log.trace("createAttribute start");

        Object value = null;
        String strValue = valueField.getText();

        String attrName = nameField.getText();
        if (attrName != null) {
            attrName = attrName.trim();
        }

        if ((attrName == null) || (attrName.length() < 1)) {
            JOptionPane.showMessageDialog(this, "No attribute name.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String lengthStr = lengthField.getText();
        log.trace("Name is {} : Length={} and Value={}", attrName, lengthStr, strValue);

        int arraySize = 0;
        if ((lengthStr == null) || (lengthStr.length() <= 0)) {
            arraySize = 1;
        }
        else {
            try {
                arraySize = Integer.parseInt(lengthStr);
            }
            catch (Exception e) {
                arraySize = -1;
            }
        }

        if (arraySize <= 0) {
            JOptionPane.showMessageDialog(this, "Invalid attribute length.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        StringTokenizer st = new StringTokenizer(strValue, ",");
        int count = Math.min(arraySize, st.countTokens());
        String theToken;
        log.trace("Count of Values is {}", count);

        // set datatype class
        int idx = classChoice.getSelectedIndex();
        if (idx == 0) {
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.isSelected()) {
                tsign = Datatype.SIGN_NONE;
            }
            torder = Datatype.NATIVE;
        }
        else if (idx == 1) {
            tclass = Datatype.CLASS_FLOAT;
            torder = Datatype.NATIVE;
        }
        else if (idx == 2) {
            tclass = Datatype.CLASS_CHAR;
            if (checkUnsigned.isSelected()) {
                tsign = Datatype.SIGN_NONE;
            }
            torder = Datatype.NATIVE;
        }
        else if (idx == 3) {
            tclass = Datatype.CLASS_STRING;
        }
        else if (idx == 4) {
            tclass = Datatype.CLASS_REFERENCE;
        }
        else if (idx == 5) {;
            isVLen = true;
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.isSelected()) {
                tsign = Datatype.SIGN_NONE;
            }
            torder = Datatype.NATIVE;
            JOptionPane.showMessageDialog(this, "Multi-dimensional Variable Length Integer Attributes will be created without data", getTitle(), JOptionPane.WARNING_MESSAGE);
        }
        else if (idx == 6) {;
            isVLen = true;
            tclass = Datatype.CLASS_FLOAT;
            torder = Datatype.NATIVE;
            JOptionPane.showMessageDialog(this, "Multi-dimensional Variable Length Float Attributes will be created without data", getTitle(), JOptionPane.WARNING_MESSAGE);
        }
        else if (idx == 7) {
            isVLen = true;
            tclass = Datatype.CLASS_STRING;
        }
        log.trace("Attribute: isVLen={} and tclass={} and torder={} and tsign={}", isVLen, tclass, torder, tsign);

        // set datatype size/order
        idx = sizeChoice.getSelectedIndex();
        if (isVLen) {
            tsize = -1;
            log.trace("Attribute isVLen={} and tsize={}", isVLen, tsize);
            String[] strArray = { strValue };
            value = strArray;
            if (tclass == Datatype.CLASS_INTEGER) {
                switch(idx) {
                        case 0:
                                tsize = 1;
                                break;
                        case 1:
                                tsize = 2;
                                break;
                        case 2:
                                tsize = 4;
                                break;
                        case 3:
                                tsize = 8;
                                break;
                }
                log.trace("Attribute VL-CLASS_INTEGER: tsize={}", tsize);
            }
            else if (tclass == Datatype.CLASS_FLOAT) {
                tsize = (idx + 1) * 4;
                log.trace("Attribute VL-CLASS_FLOAT: tsize={}", tsize);
            }
        }
        else {
            if (tclass == Datatype.CLASS_STRING) {
                int stringLength = 0;
                try {
                    stringLength = Integer.parseInt(lengthField.getText());
                }
                catch (NumberFormatException ex) {
                    stringLength = -1;
                }
    
                if (stringLength <= 0) {
                    stringLength = DEFAULT_STRING_ATTRIBUTE_LENGTH;
                }
                if (strValue.length() > stringLength) {
                    strValue = strValue.substring(0, stringLength);
                }
    
                tsize = stringLength;

                String[] strArray = { strValue };
                value = strArray;

                if (isH5) {
                    arraySize = 1; // support string type
                }
                else {
                    arraySize = stringLength; // array of characters
                }
                log.trace("Attribute CLASS_STRING: isVLen={} and tsize={} and arraySize={}", isVLen, tsize, arraySize);
            }
            else if (tclass == Datatype.CLASS_REFERENCE) {
                tsize = 1;
                arraySize = st.countTokens();
                long[] ref = new long[arraySize];
                for (int j = 0; j < arraySize; j++) {
                    theToken = st.nextToken().trim();
                    try {
                        ref[j] = Long.parseLong(theToken);
                    }
                    catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
    
                value = ref;
                torder = Datatype.NATIVE;
                log.trace("Attribute CLASS_REFERENCE: tsize={} and arraySize={}", tsize, arraySize);
            }
            else if (tclass == Datatype.CLASS_INTEGER) {
                switch(idx) {
                	case 0:
                		tsize = 1;
                		break;
                	case 1:
                		tsize = 2;
                		break;
                	case 2:
                		tsize = 4;
                		break;
                	case 3:
                		tsize = 8;
                		break;
                }
                log.trace("Attribute CLASS_INTEGER: tsize={}", tsize);
            }
            else if (tclass == Datatype.CLASS_FLOAT) {
                tsize = (idx + 1) * 4;
                log.trace("Attribute CLASS_FLOAT: tsize={}", tsize);
            }
            else {
                tsize = 1 << (idx);
                log.trace("Attribute other: tsize={}", tsize);
            }
    
            if ((tsize == 8) && !isH5 && (tclass == Datatype.CLASS_INTEGER)) {
                JOptionPane.showMessageDialog(this,
                        "HDF4 does not support 64-bit integer.", 
                        getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (tclass == Datatype.CLASS_INTEGER) {
                if (tsign == Datatype.SIGN_NONE) {
                    if (tsize == 1) {
                        byte[] b = new byte[arraySize];
                        short sv = 0;
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                sv = Short.parseShort(theToken);
                            }
                            catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                            if (sv < 0) {
                                sv = 0;
                            }
                            else if (sv > 255) {
                                sv = 255;
                            }
                            b[j] = (byte) sv;
                        }
                        value = b;
                    }
                    else if (tsize == 2) {
                        short[] s = new short[arraySize];
                        int iv = 0;
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                iv = Integer.parseInt(theToken);
                            }
                            catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                            if (iv < 0) {
                                iv = 0;
                            }
                            else if (iv > 65535) {
                                iv = 65535;
                            }
                            s[j] = (short) iv;
                        }
                        value = s;
                    }
                    else if (tsize == 4) {
                        int[] i = new int[arraySize];
                        long lv = 0;
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                lv = Long.parseLong(theToken);
                            }
                            catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                            if (lv < 0) {
                                lv = 0;
                            }
                            if (lv > 4294967295L) {
                                lv = 4294967295L;
                            }
                            i[j] = (int) lv;
                        }
                        value = i;
                    }
                    else if (tsize == 8) {
                        long[] i = new long[arraySize];
                        BigInteger lv = BigInteger.valueOf(0);
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                lv = new BigInteger(theToken);
                            }
                            catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                            i[j] = (long) lv.longValue();
                        }
                        value = i;
                    }
                }
                else {
                    if (tsize == 1) {
                        byte[] b = new byte[arraySize];
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                b[j] = Byte.parseByte(theToken);
                            }
                            catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                        }
                        value = b;
                    }
                    else if (tsize == 2) {
                        short[] s = new short[arraySize];
    
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                s[j] = Short.parseShort(theToken);
                            }
                            catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                        }
                        value = s;
                    }
                    else if (tsize == 4) {
                        int[] i = new int[arraySize];
    
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                i[j] = Integer.parseInt(theToken);
                            }
                            catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                        }
                        value = i;
                    }
                    else if (tsize == 8) {
                        long[] l = new long[arraySize];
                        for (int j = 0; j < count; j++) {
                            theToken = st.nextToken().trim();
                            try {
                                l[j] = Long.parseLong(theToken);
                            }
                            catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                        }
                        value = l;
                    }
                }
            }
    
            if (tclass == Datatype.CLASS_FLOAT) {
                if (tsize == 4) {
                    float[] f = new float[arraySize];
                    for (int j = 0; j < count; j++) {
                        theToken = st.nextToken().trim();
                        try {
                            f[j] = Float.parseFloat(theToken);
                        }
                        catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                        if (Float.isInfinite(f[j]) || Float.isNaN(f[j])) {
                            f[j] = 0;
                        }
                    }
                    value = f;
                }
                else if (tsize == 8) {
                    double[] d = new double[arraySize];
                    for (int j = 0; j < count; j++) {
                        theToken = st.nextToken().trim();
                        try {
                            d[j] = Double.parseDouble(theToken);
                        }
                        catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                        if (Double.isInfinite(d[j]) || Double.isNaN(d[j])) {
                            d[j] = 0;
                        }
                    }
                    value = d;
                }
            }
        }        

        Datatype datatype = null;
        try {
            Datatype basedatatype = null;
            if (isVLen) {
                basedatatype = fileFormat.createDatatype(tclass, tsize, torder, tsign);
                tclass = Datatype.CLASS_VLEN;
                log.trace("Attribute CLASS_VLEN");
            }
            datatype = fileFormat.createDatatype(tclass, tsize, torder, tsign, basedatatype);
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        long[] dims = { arraySize };
        Attribute attr = new Attribute(attrName, datatype, dims);
        attr.setValue(value);

        try {
            if (!isH5 && (hObject instanceof Group) && ((Group) hObject).isRoot() && h4GrAttrRadioButton.isSelected()) {
                // don't find a good way to write HDF4 global
                // attribute. Use the isExisted to separate the
                // global attribute is GR or SD
                hObject.getFileFormat().writeAttribute(hObject, attr, false);
                if (hObject.getMetadata() == null) {
                    hObject.getMetadata().add(attr);
                }
            }
            else {
                log.trace("writeMetadata()");
                hObject.writeMetadata(attr);
            }
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        newAttribute = attr;

        log.trace("createAttribute finish");
        return true;
    }

    /** Creates a dialog to show the help information. */
    private void createHelpDialog() {
        helpDialog = new JDialog(this, "Creation New Attribute");

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
            URL u = cl.findResource("ncsa/hdf/view/NewAttrHelp.html");

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
                    log.debug("JEditorPane hyper link:", t);
                }
            }
        }
    }

    /** return the new attribute created. */
    public Attribute getAttribute() {
        return newAttribute;
    }

}
