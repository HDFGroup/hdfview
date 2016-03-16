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

public class DefaultMetaDataViewOld extends JDialog implements ActionListener, MetaDataView {
    /**
     * Creates a panel used to display general information of HDF object.
     */
    private JPanel createGeneralPropertyPanel() {
        /*
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
        */
    }

    /**
     * Creates a panel used to display attribute information.
     */
    private JPanel createAttributePanel() {
        attrTableModel = new DefaultTableModel(columnNames, numAttributes);

        attrTable = new JTable(attrTableModel) {
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

        // set the divider location
        int h = Math.min((numAttributes + 2) * attrTable.getRowHeight(), scroller1.getPreferredSize().height - 40);
    }
}
