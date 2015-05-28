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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewDatasetDialog shows a message dialog requesting user input for creating a
 * new HDF4/5 dataset.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class NewDatatypeDialog extends JDialog 
implements ActionListener, ItemListener {
    private static final long serialVersionUID = -1930736056916611522L;

    private JTextField        nameField, stringLengthField;

    @SuppressWarnings("rawtypes")
    private JComboBox         parentChoice, classChoice, sizeChoice, endianChoice;

    private JCheckBox         checkUnsigned;

    private boolean           isH5;

    /** a list of current groups */
    private List<Object>              groupList;

    private HObject           newObject;

    private FileFormat        fileFormat;

    private final Toolkit     toolkit;

    /**
     * Constructs NewDatatypeDialog with specified list of possible parent
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
    public NewDatatypeDialog(JFrame owner, Group pGroup, List<?> objs) {
        super(owner, "New Datatype...", true);

        newObject = null;

        fileFormat = pGroup.getFileFormat();
        toolkit = Toolkit.getDefaultToolkit();
        isH5 = pGroup.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        parentChoice = new JComboBox();
        groupList = new Vector<Object>(objs.size());
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
        int h = 200 + (ViewProperties.getFontSize() - 12) * 10;
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
        tmpP.add(new JLabel("Datatype name: "));
        tmpP.add(new JLabel("Parent group: "));
        namePanel.add(tmpP, BorderLayout.WEST);
        tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(2, 1));
        nameField = new JTextField();
        nameField.setName("dtname");
        tmpP.add(nameField);
        tmpP.add(parentChoice);
        namePanel.add(tmpP, BorderLayout.CENTER);
        contentPane.add(namePanel, BorderLayout.NORTH);

        // set DATATYPE
        JPanel typePanel = new JPanel();
        typePanel.setLayout(new GridLayout(2, 4, 15, 3));
        TitledBorder border = new TitledBorder("Datatype");
        border.setTitleColor(Color.blue);
        typePanel.setBorder(border);

        stringLengthField = new JTextField("String length");
        stringLengthField.setName("dtstringlen");
        stringLengthField.setEnabled(false);

        endianChoice = new JComboBox();
        endianChoice.setName("dtendian");
        classChoice = new JComboBox();
        classChoice.setName("dtclass");
        sizeChoice = new JComboBox();
        sizeChoice.setName("dtsize");
        endianChoice.setEnabled(isH5);

        classChoice.addItem("INTEGER");
        classChoice.addItem("FLOAT");
        classChoice.addItem("CHAR");

        if (isH5) {
            classChoice.addItem("STRING");
            classChoice.addItem("REFERENCE");
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
            typePanel.add(new JLabel());
        }
        sizeChoice.addItem("8");
        sizeChoice.addItem("16");
        sizeChoice.addItem("32");
        sizeChoice.addItem("64");

        typePanel.add(new JLabel("Datatype class"));
        typePanel.add(new JLabel("Size (bits)"));
        typePanel.add(new JLabel("Byte ordering"));
        checkUnsigned = new JCheckBox("Unsigned");
        checkUnsigned.setName("dtchkunsigned");
        typePanel.add(checkUnsigned);

        typePanel.add(classChoice);
        typePanel.add(sizeChoice);
        typePanel.add(endianChoice);
        typePanel.add(stringLengthField);

        contentPane.add(typePanel, BorderLayout.CENTER);

        classChoice.addItemListener(this);
        sizeChoice.addItemListener(this);

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

        if (cmd.equals("Ok")) {
            newObject = createDatatype();

            if (newObject != null) {
                dispose();
            }
        }
        if (cmd.equals("Cancel")) {
            newObject = null;
            dispose();
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

            if ((idx == 0) || (idx == 5)) {
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
            else if ((idx == 1) || (idx == 6)) {
                sizeChoice.setEnabled(true);
                endianChoice.setEnabled(isH5);
                checkUnsigned.setEnabled(false);

                if (sizeChoice.getItemCount() == 5) {
                    sizeChoice.removeItem("16");
                    sizeChoice.removeItem("8");
                }
            }
            else if (idx == 2) {
                sizeChoice.setEnabled(false);
                endianChoice.setEnabled(isH5);
                checkUnsigned.setEnabled(true);
            }
            else if (idx == 3) {
                sizeChoice.setEnabled(false);
                endianChoice.setEnabled(false);
                checkUnsigned.setEnabled(false);
                stringLengthField.setEnabled(true);
                stringLengthField.setText("String length");
            }
            else if (idx == 4) {
                sizeChoice.setEnabled(false);
                endianChoice.setEnabled(false);
                checkUnsigned.setEnabled(false);
                stringLengthField.setEnabled(false);
            }
            else if (idx == 7) {
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
    }

    private HObject createDatatype() {
        String name = null;
        Group pgroup = null;
        boolean isVLen = false;
        int tclass = -1, tsize = -1, torder = -1, tsign = -1;
        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this,
                    "Datatype name is not specified.", 
                    getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this,
                    "Datatype name cannot contain path.", 
                    getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectedIndex());

        if (pgroup == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, 
                    "Parent group is null.",
                    getTitle(), 
                    JOptionPane.ERROR_MESSAGE);
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
        else if (idx == 5) {;
            isVLen = true;
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.isSelected()) {
                tsign = Datatype.SIGN_NONE;
            }
        }
        else if (idx == 6) {;
            isVLen = true;
            tclass = Datatype.CLASS_FLOAT;
        }
        else if (idx == 7) {
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
                    JOptionPane.showMessageDialog(this, 
                            "Invalid string length: " + stringLengthField.getText(), 
                            getTitle(),
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
    
                tsize = stringLength;
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
            JOptionPane.showMessageDialog(this,
                    "HDF4 does not support 64-bit integer.", 
                    getTitle(),
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

        HObject obj = null;
        try {
            String fullPath = HObject.separator;
            if (pgroup.isRoot()) {
                fullPath += name;
            }
            else {
                fullPath = pgroup.getPath() + HObject.separator + pgroup.getName() + HObject.separator + name;
            }
            Datatype basedatatype = null;
            if (isVLen) {
                basedatatype = fileFormat.createDatatype(tclass, tsize, torder, tsign);
                tclass = Datatype.CLASS_VLEN;
            }
            Datatype datatype = fileFormat.createDatatype(tclass, tsize, torder, tsign, basedatatype, fullPath);
            obj = datatype;
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, 
                    ex,
                    getTitle(),
                    JOptionPane.ERROR_MESSAGE);
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
