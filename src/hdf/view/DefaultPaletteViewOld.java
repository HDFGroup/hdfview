import org.eclipse.swt.graphics.Image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.CellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;


public class DefaultPaletteViewOld extends JDialog implements PaletteView,
        MouseListener, MouseMotionListener, ActionListener, ItemListener {
    public DefaultPaletteViewOld(ViewManager theViewer, ImageView theImageView) {
        super((JFrame) theViewer, true);

        

        chartP = new ChartPanel();
        chartP.setBackground(Color.white);

        paletteData = new int[3][256];
        byte[][] imagePalette = imageView.getPalette();

        int d = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 256; j++) {
                d = imagePalette[i][j];
                if (d < 0) {
                    d += 256;
                }
                paletteData[i][j] = d;
            }
        }

        imageView = theImageView;
        chartP.addMouseListener(this);
        chartP.addMouseMotionListener(this);

        x0 = y0 = 0;
        originalImage = currentImage = imageView.getImage();
        palette = new byte[3][256];

        createUI();
    }

    public void actionPerformed(ActionEvent e) {
        else if (cmd.equals("Hide palette values")) {
            if (paletteValueTable != null) {
                paletteValueTable.setVisible(false);
            }
        }
    }

    @Override
    public void dispose() {
        imageView.setImage(originalImage);
        super.dispose();
    }

    public void mouseClicked(MouseEvent e) {
    } // MouseListener

    public void mouseReleased(MouseEvent e) {
        if ((paletteValueTable != null) && paletteValueTable.isVisible()) {
            paletteValueTable.refresh();
        }
    } // MouseListener

    public void mouseEntered(MouseEvent e) {
    } // MouseListener

    public void mouseExited(MouseEvent e) {
    } // MouseListener

    public void mouseMoved(MouseEvent e) {
    } // MouseMotionListener

    // implementing MouseListener
    public void mousePressed(MouseEvent e) {
        // x0 = e.getX()-40; // takes the horizontal gap
        // if (x0 < 0) x0 = 0;
        // y0 = e.getY()+20;
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

    /** The dialog to show the palette values in spreadsheet. */
    private final class PaletteValueTable extends JDialog {
        private static final long serialVersionUID = 6105012612969555535L;
        private JTable valueTable;
        private DefaultTableModel valueTableModel;
        String rgbName = "Color";
        String idxName = "Index";
        int editingRow =-1, editingCol=-1;

        public PaletteValueTable(DefaultPaletteViewOld owner) {
            super(owner);
            String[] columnNames = { idxName, "Red", "Green", "Blue", rgbName };
            valueTableModel = new DefaultTableModel(columnNames, 256);

            valueTable = new JTable(valueTableModel) {
                private static final long serialVersionUID = -2823793138915014637L;

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

            valueTable.setName("PaletteValue");
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

            // set cell height for large fonts
            int cellRowHeight = Math.max(16, valueTable.getFontMetrics(
                    valueTable.getFont()).getHeight());
            valueTable.setRowHeight(cellRowHeight);

            JScrollPane scroller = new JScrollPane(valueTable);

            JPanel contentPane = (JPanel) getContentPane();
            int w = 300 + (ViewProperties.getFontSize() - 12) * 10;
            int h = 600 + (ViewProperties.getFontSize() - 12) * 15;
            contentPane.setPreferredSize(new Dimension(w, h));
            contentPane.setLayout(new BorderLayout(5, 5));
            contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            contentPane.add(scroller, BorderLayout.CENTER);

            JButton button = new JButton("  Ok  ");
            button.addActionListener(owner);
            button.setActionCommand("Hide palette values");

            JPanel tmpP = new JPanel();
            tmpP.add(button);
            contentPane.add(tmpP, BorderLayout.SOUTH);

            Point l = owner.getLocation();
            l.x += 100;
            l.y += 100;
            setLocation(l);
            pack();
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
            valueTable.updateUI();
        }
    }

    /** The canvas that paints the data lines. */
    private final class ChartPanel extends JComponent {
        private static final long serialVersionUID = -6861041412971944L;

        /**
         * Paints the plot components.
         */
        @Override
        public void paint(Graphics g) {
            Dimension d = getSize();
            int gap = 20;
            int legendSpace = 60;
            int h = d.height - gap;
            int w = d.width - 3 * gap - legendSpace;

            // draw the X axis
            g.drawLine(2 * gap, h, w + 2 * gap, h);

            // draw the Y axis
            g.drawLine(2 * gap, h, 2 * gap, 0);

            // draw X and Y labels: 10 labels for x and y
            int dh = h / 10;
            int dw = w / 10;
            int dx = 25;
            double dy = 25;
            int xp = 2 * gap, yp = 0, x = 0, x0, y0, x1, y1;
            double y = 0;

            // draw X and Y grid labels
            g.drawString(String.valueOf((int) y), 0, h + 8);
            g.drawString(String.valueOf(x), xp - 5, h + gap);
            for (int i = 0; i < 10; i++) {
                xp += dw;
                yp += dh;
                x += dx;
                y += dy;
                g.drawLine(xp, h, xp, h - 5);
                g.drawLine(2 * gap, h - yp, 2 * gap + 5, h - yp);
                g.drawString(String.valueOf((int) y), 0, h - yp + 8);
                g.drawString(String.valueOf(x), xp - 5, h + gap);
            }

            Color c = g.getColor();
            for (int i = 0; i < 3; i++) {
                g.setColor(lineColors[i]);

                // set up the line data for drawing one line a time
                for (int j = 0; j < 255; j++) {
                    x0 = (w * j / 255) + 2 * gap;
                    y0 = (h - h * paletteData[i][j] / 255);
                    x1 = (w * (j + 1) / 255) + 2 * gap;
                    y1 = (h - h * (paletteData[i][j + 1]) / 255);
                    g.drawLine(x0, y0, x1, y1);
                }

                x0 = w + legendSpace;
                y0 = gap + gap * i;
                g.drawLine(x0, y0, x0 + 7, y0);
                g.drawString(lineLabels[i], x0 + 10, y0 + 3);
            }

            g.setColor(c); // set the color back to its default

            // draw a box on the legend
            g.drawRect(w + legendSpace - 10, 10, legendSpace, 10 * gap);
        } // public void paint(Graphics g)

    } // private class ChartPanel extends Canvas

} // private class PaletteView extends ChartView

