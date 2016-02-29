public class DefaultPaletteViewOld extends JDialog implements PaletteView,
        MouseListener, MouseMotionListener, ActionListener, ItemListener {
    public DefaultPaletteViewOld(ViewManager theViewer, ImageView theImageView) {
        super((JFrame) theViewer, true);

        chartP.addMouseMotionListener(this);
    }

    // implementing MouseMotionListener
    public void mouseDragged(MouseEvent e) {
        int x1 = e.getX() - 40;// takes the vertical gap
        if (x1 < 0) {
            x1 = 0;
        }
        int y1 = e.getY() + 20;

        Dimension d = chartP.getSize();
        double ry = 255 / (double) d.height;
        double rx = 255 / (double) d.width;

        int lineIdx = 0;
        if (checkGreen.isSelected()) {
            lineIdx = 1;
        }
        else if (checkBlue.isSelected()) {
            lineIdx = 2;
        }

        int idx = 0;
        double b = (double) (y1 - y0) / (double) (x1 - x0);
        double a = y0 - b * x0;
        double value = y0 * ry;
        int i0 = Math.min(x0, x1);
        int i1 = Math.max(x0, x1);
        for (int i = i0; i < i1; i++) {
            idx = (int) (rx * i);
            if (idx > 255) {
                continue;
            }
            value = 255 - (a + b * i) * ry;
            if (value < 0) {
                value = 0;
            }
            else if (value > 255) {
                value = 255;
            }
            paletteData[lineIdx][idx] = (int) value;
        }

        chartP.repaint();
        isPaletteChanged = true;
    }

    private final class PaletteValueTable extends JDialog {
        private JTable valueTable;
        private DefaultTableModel valueTableModel;
        int editingRow =-1, editingCol=-1;

        public PaletteValueTable(DefaultPaletteViewOld owner) {
            
            valueTableModel = new DefaultTableModel(columnNames, 256);

            valueTable = new JTable(valueTableModel) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return (col > 0 && col < 4);
                }

                @Override
                public Object getValueAt(int row, int col) {
                    if (startEditing && row==editingRow && col==editingCol)
                        return "";

                    if (col == 0)
                        return String.valueOf(row);
                    else if (col < 4) {
                        return String.valueOf(paletteData[col - 1][row]);
                    }
                    else {
                        return "";
                    }
                }

                @Override
                public boolean editCellAt(int row, int column, java.util.EventObject e) 
                {
                    if (!isCellEditable(row, column)) {
                        return super.editCellAt(row, column, e);
                    }

                    if (e instanceof KeyEvent) {
                        KeyEvent ke = (KeyEvent) e;
                        if (ke.getID() == KeyEvent.KEY_PRESSED) {
                            startEditing = true;
                            editingRow = row;
                            editingCol = column;
                        }
                    }

                    return super.editCellAt(row, column, e);
                }

                @Override
                public void editingStopped(ChangeEvent e) {
                    int row = getEditingRow();
                    int col = getEditingColumn();

                    if (!isCellEditable(row, col)) {
                        return;
                    }

                    String oldValue = (String) getValueAt(row, col);
                    super.editingStopped(e);
                    startEditing = false;
                    editingRow = -1;
                    editingCol = -1;

                    Object source = e.getSource();

                    if (source instanceof CellEditor) {
                        CellEditor editor = (CellEditor) source;
                        String newValue = (String) editor.getCellEditorValue();
                        setValueAt(oldValue, row, col); // set back to what it
                                                        // is
                        updatePaletteValue(newValue, row, col - 1);
                    }
                }
            };

            valueTable.getColumn(rgbName).setCellRenderer(
                    new DefaultTableCellRenderer() {
                        private static final long serialVersionUID = 8390954944015521331L;
                        Color color = Color.white;

                        @Override
                        public java.awt.Component getTableCellRendererComponent(
                                JTable table, Object value, boolean isSelected,
                                boolean hasFocus, int row, int col) {
                            java.awt.Component comp = super
                                    .getTableCellRendererComponent(table,
                                            value, isSelected, hasFocus, row,
                                            col);
                            color = new Color(paletteData[0][row],
                                    paletteData[1][row], paletteData[2][row]);
                            comp.setBackground(color);
                            return comp;
                        }
                    });

            valueTable.getColumn(idxName).setCellRenderer(
                    new DefaultTableCellRenderer() {
                        private static final long serialVersionUID = 2786027382023940417L;

                        @Override
                        public java.awt.Component getTableCellRendererComponent(
                                JTable table, Object value, boolean isSelected,
                                boolean hasFocus, int row, int col) {
                            java.awt.Component comp = super
                                    .getTableCellRendererComponent(table,
                                            value, isSelected, hasFocus, row,
                                            col);
                            comp.setBackground(Color.LIGHT_GRAY);
                            return comp;
                        }
                    });

            valueTable.setRowSelectionAllowed(false);
            valueTable.setCellSelectionEnabled(true);
            valueTable.getTableHeader().setReorderingAllowed(false);
            valueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        private void updatePaletteValue(String strValue, int row, int col) {
            if (strValue == null) {
                return;
            }

            int value = 0;

            try {
                value = Integer.parseInt(strValue);
            }
            catch (Exception ex) {
                return;
            }

            if (value < 0 || value > 255) {
                JOptionPane.showMessageDialog(this,
                        "Value is out of range [0, 255]\n", getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            paletteData[col][row] = value;
            chartP.repaint();
            isPaletteChanged = true;
        }

        public void refresh() {
            valueTable.editingStopped(new ChangeEvent(valueTable));
        }
    }
} // private class PaletteView extends ChartView

