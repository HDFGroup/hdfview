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
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import hdf.object.DataFormat;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewGroupDialog shows a message dialog requesting user input for creating a new HDF4/5 group.
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class NewGroupDialog extends JDialog implements ActionListener, ItemListener, KeyListener {
    private static final long serialVersionUID = 7340860373483987075L;

    private JTextField nameField;

    private JTextField compactField;

    private JTextField indexedField;

    @SuppressWarnings("rawtypes")
    private JComboBox parentChoice;

    private JCheckBox useCreationOrder;

    private JCheckBox setLinkStorage;

    @SuppressWarnings("rawtypes")
    private JComboBox orderFlags;

    /** a list of current groups */
    private List<Group> groupList;

    private HObject newObject;

    private FileFormat fileFormat;

    private final Toolkit toolkit;

    private int creationOrder;

    private JPanel useCreationOrderJPanel;

    private JPanel setLinkStorageJPanel;

    private JButton moreButton;

    private JPanel labelPanel;

    private JPanel textPanel;

    private JPanel contentPane;

    private JButton creationOrderHelpButton;

    private JButton storageTypeHelpButton;

    private boolean isH5;

    /**
     * Constructs NewGroupDialog with specified list of possible parent groups.
     * 
     * @param owner
     *            the owner of the input
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NewGroupDialog(Frame owner, Group pGroup, List<?> objs) {
        super(owner, "New Group...", true);

        newObject = null;

        fileFormat = pGroup.getFileFormat();
        isH5 = pGroup.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        toolkit = Toolkit.getDefaultToolkit();

        parentChoice = new JComboBox();
        groupList = new Vector<Group>();
        Object obj = null;
        Iterator<?> iterator = objs.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                groupList.add((Group) obj);
                Group g = (Group) obj;
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

        orderFlags = new JComboBox();
        orderFlags.addItem("Tracked");
        orderFlags.addItem("Tracked+Indexed");

        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        int w = 400 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 150 + (ViewProperties.getFontSize() - 12) * 10;
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

        moreButton = new JButton("More");
        moreButton.setName("More");
        moreButton.addActionListener(this);

        // set OK and CANCEL buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // set NAME and PARENT GROUP panel
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout(5, 5));

        labelPanel = new JPanel();
        textPanel = new JPanel();

        if (!isH5) {
            labelPanel.setLayout(new GridLayout(2, 1));
            labelPanel.add(new JLabel("Group name: "));
            labelPanel.add(new JLabel("Parent group: "));
            textPanel.setLayout(new GridLayout(2, 1));
            textPanel.add(nameField = new JTextField());
            textPanel.add(parentChoice);
        }
        else {
            labelPanel.setLayout(new GridLayout(3, 1));
            labelPanel.add(new JLabel("Group name: "));
            labelPanel.add(new JLabel("Parent group: "));
            labelPanel.add(moreButton); // if h5 format then add more button
            textPanel.setLayout(new GridLayout(3, 1));
            textPanel.add(nameField = new JTextField());
            textPanel.add(parentChoice);
            textPanel.add(new JLabel("")); // for more button
        }
        nameField.setName("groupname");
        parentChoice.setName("groupparent");

        //creationOrderHelpButton = new JButton(ViewProperties.getHelpIcon());
        creationOrderHelpButton.setToolTipText("Help on Creation Order");
        creationOrderHelpButton.setMargin(new Insets(0, 0, 0, 0));
        creationOrderHelpButton.addActionListener(this);
        creationOrderHelpButton.setActionCommand("Help on Creation Order");

        //storageTypeHelpButton = new JButton(ViewProperties.getHelpIcon());
        storageTypeHelpButton.setToolTipText("Help on set Link Storage");
        storageTypeHelpButton.setMargin(new Insets(0, 0, 0, 0));
        storageTypeHelpButton.addActionListener(this);
        storageTypeHelpButton.setActionCommand("Help on set Link Storage");

        namePanel.add(labelPanel, BorderLayout.WEST);

        useCreationOrderJPanel = new JPanel();
        useCreationOrderJPanel.setLayout(new GridLayout(1, 2));
        useCreationOrderJPanel.setBorder(new TitledBorder(""));
        useCreationOrderJPanel.add(useCreationOrder = new JCheckBox("Use Creation Order"));
        useCreationOrder.addItemListener(this);
        JPanel orderFlagsJPanel = new JPanel();
        orderFlagsJPanel.setLayout(new GridLayout(1, 2));
        orderFlagsJPanel.add(new JLabel("Order Flags: "));
        orderFlagsJPanel.add(orderFlags);
        orderFlags.setEnabled(false);
        useCreationOrderJPanel.add(orderFlagsJPanel);

        setLinkStorageJPanel = new JPanel();
        setLinkStorageJPanel.setLayout(new GridLayout(1, 2));
        setLinkStorageJPanel.setBorder(new TitledBorder(""));
        setLinkStorageJPanel.add(setLinkStorage = new JCheckBox("Set Link Storage"));
        setLinkStorage.addItemListener(this);
        JPanel storageTypeJPanel = new JPanel();
        storageTypeJPanel.setLayout(new GridLayout(2, 2));
        storageTypeJPanel.add(new JLabel("Min Indexed: "));
        storageTypeJPanel.add(new JLabel("Max Compact: "));
        indexedField = new JTextField();
        indexedField.addKeyListener(this);
        storageTypeJPanel.add(indexedField);
        indexedField.setDocument(new JTextFieldLimit(5));
        indexedField.setText("6");
        indexedField.setEnabled(false);
        compactField = new JTextField();
        storageTypeJPanel.add(compactField);
        compactField.addKeyListener(this);
        compactField.setDocument(new JTextFieldLimit(5));
        compactField.setText("8");
        compactField.setEnabled(false);
        setLinkStorageJPanel.add(storageTypeJPanel);

        namePanel.add(textPanel, BorderLayout.CENTER);
        contentPane.add(namePanel, BorderLayout.CENTER);

        // locate the H5Property dialog
        Point l = owner.getLocation();
        l.x += 250;
        l.y += 80;
        setLocation(l);
        validate();
        pack();
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("More")) {
            moreButton.setText("Less");
            int w = 500 + (ViewProperties.getFontSize() - 12) * 15;
            int h = 280 + (ViewProperties.getFontSize() - 12) * 10;
            contentPane.setPreferredSize(new Dimension(w, h));
            labelPanel.setLayout(new GridLayout(5, 1));
            labelPanel.add(creationOrderHelpButton);
            labelPanel.add(storageTypeHelpButton);
            textPanel.setLayout(new GridLayout(5, 1));
            textPanel.add(useCreationOrderJPanel);
            textPanel.add(setLinkStorageJPanel);
            validate();
            pack();
        }

        if (cmd.equals("Less")) {
            moreButton.setText("More");
            int w = 400 + (ViewProperties.getFontSize() - 12) * 15;
            int h = 150 + (ViewProperties.getFontSize() - 12) * 10;
            contentPane.setPreferredSize(new Dimension(w, h));
            labelPanel.setLayout(new GridLayout(3, 1));
            labelPanel.remove(creationOrderHelpButton);
            labelPanel.remove(storageTypeHelpButton);
            textPanel.setLayout(new GridLayout(3, 1));
            textPanel.remove(useCreationOrderJPanel);
            textPanel.remove(setLinkStorageJPanel);
            useCreationOrder.setSelected(false);
            setLinkStorage.setSelected(false);
            validate();
            pack();
        }

        if (cmd.equals("Help on Creation Order")) {
            final String msg = "Use Creation Order allows the user to set the creation order \n"
                    + "of links in a group, so that tracking, indexing, and iterating over links\n"
                    + "in groups can be possible. \n\n"
                    + "If the order flag Tracked is selected, links in a group can now \n"
                    + "be explicitly tracked by the order that they were created. \n\n"
                    + "If the order flag Tracked+Indexed is selected, links in a group can \n"
                    + "now be explicitly tracked and indexed in the order that they were created. \n\n"
                    + "The default order in which links in a group are listed is alphanumeric-by-name. \n\n\n";
            JOptionPane.showMessageDialog(this, msg);
        }

        if (cmd.equals("Help on set Link Storage")) {
            final String msg = "Set Link Storage allows the users to explicitly set the storage  \n"
                    + "type of a group to be Compact or Indexed. \n\n"
                    + "Compact Storage: For groups with only a few links, compact link storage\n"
                    + "allows groups containing only a few links to take up much less space \n" + "in the file. \n\n"
                    + "Indexed Storage: For groups with large number of links, indexed link storage  \n"
                    + "provides a faster and more scalable method for storing and working with  \n"
                    + "large groups containing many links. \n\n"
                    + "The threshold for switching between the compact and indexed storage   \n"
                    + "formats is either set to default values or can be set by the user. \n\n"
                    + "<html><b>Max Compact</b></html> \n"
                    + "Max Compact is the maximum number of links to store in the group in a  \n"
                    + "compact format, before converting the group to the Indexed format. Groups \n"
                    + "that are in compact format and in which the number of links rises above \n"
                    + " this threshold are automatically converted to indexed format. \n\n"
                    + "<html><b>Min Indexed</b></html> \n"
                    + "Min Indexed is the minimum number of links to store in the Indexed format.   \n"
                    + "Groups which are in indexed format and in which the number of links falls    \n"
                    + "below this threshold are automatically converted to compact format. \n\n\n";
            JOptionPane.showMessageDialog(this, msg);
        }

        if (cmd.equals("Ok")) {
            newObject = create();
            if (newObject != null) {
                dispose();
            }
        }
        if (cmd.equals("Cancel")) {
            newObject = null;
            dispose();
        }
    }

    private HObject create() {
        String name = null;
        Group pgroup = null;
        int gcpl = 0;

        name = nameField.getText();
        if (name == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Group name is not specified.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Group name cannot contain path.", getTitle(),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        pgroup = groupList.get(parentChoice.getSelectedIndex());

        if (pgroup == null) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, "Parent group is null.", getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Group obj = null;

        if (orderFlags.isEnabled()) {
            String order = (String) orderFlags.getSelectedItem();
            if (order.equals("Tracked"))
                creationOrder = Group.CRT_ORDER_TRACKED;
            else if (order.equals("Tracked+Indexed"))
                creationOrder = Group.CRT_ORDER_INDEXED;
        }
        else
            creationOrder = 0;

        if ((orderFlags.isEnabled()) || (setLinkStorage.isSelected())) {
            int maxCompact = Integer.parseInt(compactField.getText());
            int minDense = Integer.parseInt(indexedField.getText());

            if ((maxCompact <= 0) || (maxCompact > 65536) || (minDense > 65536)) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Max Compact and Min Indexed should be > 0 and < 65536.",
                        getTitle(), JOptionPane.ERROR_MESSAGE);
                return null;
            }

            if (maxCompact < minDense) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "Min Indexed should be <= Max Compact", getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }

            try {
                gcpl = fileFormat.createGcpl(creationOrder, maxCompact, minDense);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            if (isH5)
                obj = fileFormat.createGroup(name, pgroup, 0, gcpl);
            else
                obj = fileFormat.createGroup(name, pgroup);
        }
        catch (Exception ex) {
            toolkit.beep();
            JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return obj;
    }

    /** Returns the new group created. */
    public DataFormat getObject() {
        return newObject;
    }

    /** Returns the parent group of the new group. */
    public Group getParentGroup() {
        return groupList.get(parentChoice.getSelectedIndex());
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();

        if (source.equals(useCreationOrder)) {
            boolean isOrder = useCreationOrder.isSelected();

            if (isOrder)
                orderFlags.setEnabled(true);
            else
                orderFlags.setEnabled(false);
        }

        if (source.equals(setLinkStorage)) {
            boolean setStorage = setLinkStorage.isSelected();

            if (setStorage) {
                compactField.setEnabled(true);
                indexedField.setEnabled(true);
            }
            else {
                compactField.setText("8");
                compactField.setEnabled(false);
                indexedField.setText("6");
                indexedField.setEnabled(false);
            }
        }
    }

    // Setting the length of the text fields.
    class JTextFieldLimit extends PlainDocument {
        private static final long serialVersionUID = -5131438789797052658L;
        private int limit;

        JTextFieldLimit(int limit) {
            super();
            this.limit = limit;
        }

        JTextFieldLimit(int limit, boolean upper) {
            super();
            this.limit = limit;
        }

        public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
            if (str == null)
                return;

            if ((getLength() + str.length()) <= limit) {
                super.insertString(offset, str, attr);
            }
        }
    }

    public void keyPressed(java.awt.event.KeyEvent arg0) {
    }

    public void keyReleased(java.awt.event.KeyEvent arg0) {
    }

    public void keyTyped(java.awt.event.KeyEvent arg0) {
        char c = arg0.getKeyChar();
        if (!Character.isDigit(c))
            arg0.consume(); // prevent event propagation
    }

}
