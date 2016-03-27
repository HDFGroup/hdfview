import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.BitSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

public class DataOptionDialogOld extends JDialog implements ActionListener, ItemListener
{
    /**
     * JComboBox.setSelectedItem() or setSelectedIndex() always fires action event. If you call
     * setSelectedItem() or setSelectedIndex() at itemStateChanged() or actionPerformed(), the
     * setSelectedItem() or setSelectedIndex() will make loop calls of itemStateChanged() or
     * actionPerformed(). This is not what we want. We want the setSelectedItem() or
     * setSelectedIndex() behavior like java.awt.Choice. This flag is used to serve this purpose.
     */
    private boolean                performJComboBoxEvent = false;

    public DataOptionDialogOld(ViewManager theview, Dataset theDataset) {
        selLabel = new JLabel("", SwingConstants.CENTER);

        choiceImageView = new JComboBox((Vector<?>) HDFView.getListOfImageView());
        choiceImageView.setName("moduleimage");

        extractBitButton = new JCheckBox("Show Value of Selected Bits", false);
        extractBitButton.setMnemonic(KeyEvent.VK_V);
        extractBitButton.setEnabled(false);
        extractBitButton.addItemListener(this);

        applyBitmaskButton = new JCheckBox("Apply Bitmask", false);
        applyBitmaskButton.setMnemonic(KeyEvent.VK_A);
        applyBitmaskButton.setEnabled(false);
        applyBitmaskButton.addItemListener(this);
        applyBitmaskButton.setName("applybitmask");

        bitmaskHelp = new JButton(ViewProperties.getHelpIcon());
        bitmaskHelp.setEnabled(false);
        bitmaskHelp.setToolTipText("Help on how to set bitmask");
        bitmaskHelp.setMargin(new Insets(0, 0, 0, 0));
        bitmaskHelp.addActionListener(this);
        bitmaskHelp.setActionCommand("Help on how to set bitmask");

        // layout the components
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        int w1 = 700 + (ViewProperties.getFontSize() - 12) * 15;
        int h1 = 350 + (ViewProperties.getFontSize() - 12) * 10;
        contentPane.setPreferredSize(new Dimension(w1, h1));

        JPanel navigatorP = new JPanel();
        navigatorP.setLayout(new BorderLayout());
        navigatorP.add(navigator, BorderLayout.CENTER);
        navigatorP.add(selLabel, BorderLayout.SOUTH);
        navigatorP.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        performJComboBoxEvent = true;


        if (dataset instanceof CompoundDS) {
            w1 = 150 + (ViewProperties.getFontSize() - 12) * 10;
            h1 = 250 + (ViewProperties.getFontSize() - 12) * 15;
            fieldP.setPreferredSize(new Dimension(w1, h1));
        }
        else if (dataset instanceof ScalarDS) {
            ScalarDS sd = (ScalarDS) dataset;
            
            if (isText) {
                w1 = 700 + (ViewProperties.getFontSize() - 12) * 15;
                h1 = 280 + (ViewProperties.getFontSize() - 12) * 10;
                contentPane.setPreferredSize(new Dimension(w1, h1));
            }
            else {
                w1 = 800 + (ViewProperties.getFontSize() - 12) * 15;
                h1 = 550 + (ViewProperties.getFontSize() - 12) * 10;
                contentPane.setPreferredSize(new Dimension(w1, h1));
                if (rank > 1) {
                    centerP.add(navigatorP, BorderLayout.WEST);
                }

                JPanel sheetP = new JPanel();
                sheetP.setLayout(new GridLayout(1, 2, 25, 5));
                sheetP.add(spreadsheetButton);
                int tclass = sd.getDatatype().getDatatypeClass();
                sheetP.add(charCheckbox);
                if (tclass == Datatype.CLASS_CHAR
                        || (tclass == Datatype.CLASS_INTEGER && sd
                                .getDatatype().getDatatypeSize() == 1)) {
                    charCheckbox.setEnabled(false);
                }

                // add tableview selection
                JPanel tviewP = new JPanel();
                tviewP.setLayout(new BorderLayout());
                tviewP.add(new JLabel("TableView:   "), BorderLayout.WEST);
                tviewP.add(choiceTableView, BorderLayout.CENTER);

                JPanel leftP = new JPanel();
                leftP.setBorder(BorderFactory
                        .createLineBorder(Color.LIGHT_GRAY));
                leftP.setLayout(new GridLayout(2, 1, 5, 5));
                leftP.add(sheetP);
                leftP.add(tviewP);

                viewP.add(leftP);

                // add imageview selection
                JPanel rightP = new JPanel();
                rightP.setLayout(new BorderLayout(5, 5));
                rightP.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                JPanel imageP1 = new JPanel();
                JPanel imageP2 = new JPanel();
                rightP.add(imageP1, BorderLayout.CENTER);
                rightP.add(imageP2, BorderLayout.EAST);
                viewP.add(rightP);
                imageP1.setLayout(new BorderLayout(5, 5));
                JPanel tmpP = new JPanel();
                tmpP.setLayout(new GridLayout(2, 1, 5, 5));
                tmpP.add(imageButton);
                tmpP.add(new JLabel("ImageView: "));
                imageP1.add(tmpP, BorderLayout.WEST);
                tmpP = new JPanel();
                tmpP.setLayout(new GridLayout(2, 1, 5, 5));
                tmpP.add(choicePalette);
                tmpP.add(choiceImageView);
                imageP1.add(tmpP, BorderLayout.CENTER);

                imageP2.setLayout(new GridLayout(1, 2, 5, 5));
                tmpP = new JPanel();
                tmpP.setLayout(new GridLayout(2, 1, 5, 5));
                tmpP.add(new JLabel("  Valid Range: "));
                tmpP.add(new JLabel("  Invalid Values:  "));
                imageP2.add(tmpP);
                tmpP = new JPanel();
                tmpP.setLayout(new GridLayout(2, 1, 5, 5));
                String minmaxStr = "min, max", fillStr = "val1, val2, ...";
                double minmax[] = ((ScalarDS) dataset).getImageDataRange();
                if (minmax != null) {
                    if (dataset.getDatatype().getDatatypeClass() == Datatype.CLASS_FLOAT)
                        minmaxStr = minmax[0] + "," + minmax[1];
                    else
                        minmaxStr = ((long) minmax[0]) + "," + ((long) minmax[1]);
                }
                List<Number> fillValue = ((ScalarDS) dataset).getFilteredImageValues();
                int n = fillValue.size();
                if (n > 0) {
                    fillStr = fillValue.get(0).toString();
                    for (int i = 1; i < n; i++) {
                        fillStr += ", " + fillValue.get(i);
                    }
                }
                tmpP.add(dataRangeField = new JTextField(minmaxStr));
                tmpP.add(fillValueField = new JTextField(fillStr));
                imageP2.add(tmpP);

                JPanel northP = new JPanel();
                northP.setLayout(new BorderLayout(5, 5));
                northP.add(viewP, BorderLayout.CENTER);

                // index base and bit mask
                viewP = new JPanel();
                viewP.setLayout(new BorderLayout());
                northP.add(viewP, BorderLayout.SOUTH);

                JPanel baseIndexP = new JPanel();
                viewP.add(baseIndexP, BorderLayout.NORTH);
                tborder = new TitledBorder("Index Base");
                tborder.setTitleColor(Color.gray);
                baseIndexP.setBorder(tborder);
                baseIndexP.setLayout(new GridLayout(1, 2, 5, 5));

                ButtonGroup bgrp = new ButtonGroup();
                bgrp.add(base0Button);
                bgrp.add(base1Button);

                baseIndexP.add(base0Button);
                baseIndexP.add(base1Button);

                int tsize = sd.getDatatype().getDatatypeSize();
                bitmaskButtons = new JRadioButton[8 * tsize];
                for (int i = 0; i < bitmaskButtons.length; i++) {
                    bitmaskButtons[i] = new JRadioButton(String.valueOf(i));
                    bitmaskButtons[i].setEnabled(false);
                    bitmaskButtons[i].addItemListener(this);
                    bitmaskButtons[i].setName("bitmaskButton"+i);
                }

                JPanel sheetP2 = new JPanel();
                viewP.add(sheetP2, BorderLayout.CENTER);
                tborder = new TitledBorder("Bitmask");
                tborder.setTitleColor(Color.gray);
                sheetP2.setBorder(tborder);

                tmpP = new JPanel();
                if (bitmaskButtons.length <= 16) {
                	tmpP.setLayout(new GridLayout(1, bitmaskButtons.length));
                    for (int i = bitmaskButtons.length; i > 0; i--)
                        tmpP.add(bitmaskButtons[i - 1]);
                } else {
                	tmpP.setLayout(new GridLayout(tsize/2, 16));
                    for (int i = bitmaskButtons.length; i > 0; i--)
                        tmpP.add(bitmaskButtons[i - 1]);
                }
                
                sheetP2.setLayout(new BorderLayout(10, 10));
                if (tsize <= 8) sheetP2.add(tmpP, BorderLayout.CENTER);
                sheetP2.add(new JLabel(), BorderLayout.NORTH);

                JPanel tmpP2 = new JPanel();
                tmpP2.setLayout(new GridLayout(2, 1));
                tmpP2.add(extractBitButton);
                tmpP2.add(applyBitmaskButton);
                tmpP = new JPanel();
                tmpP.setLayout(new BorderLayout());
                tmpP.add(tmpP2, BorderLayout.WEST);
                tmpP2 = new JPanel();
                tmpP2.add(bitmaskHelp);
                tmpP.add(tmpP2, BorderLayout.EAST);
                sheetP2.add(tmpP, BorderLayout.NORTH);
                contentPane.add(northP, BorderLayout.NORTH);

                if (tclass == Datatype.CLASS_CHAR
                        || (tclass == Datatype.CLASS_INTEGER && tsize <= 8)) {
                    extractBitButton.setEnabled(true);
                    applyBitmaskButton.setEnabled(true);
                    bitmaskHelp.setEnabled(true);
                }
            }
        }
    }

    //@Override
    public void actionPerformed (ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Help on how to set bitmask")) {
            String msg = ""
                    + "\"Apply Bitmask\" applies bitwise \"AND\" to the original data.\n"
                    + "For example, bits 2, 3, and 4 are selected for the bitmask\n"
                    + "         10010101 (data)\n"
                    + "AND 00011100 (mask)  \n"
                    + "  =     00010100 (result) ==> the decimal value is 20. \n"
                    + "\n"
                    + "\"Extract Bit(s)\" removes all the bits from the result above where\n"
                    + "their corresponding bits in the bitmask are 0. \nFor the same example above, "
                    + "the result is \n101 ==> the decimal value is 5.\n\n";

            JOptionPane.showMessageDialog((JFrame) viewer, msg);
        }
    }

    //@Override
    public void itemStateChanged (ItemEvent e) {
        Object source = e.getSource();

        if (source instanceof JToggleButton) {
            checkBitmaskButtons((JToggleButton) source);
        }
        else if (source instanceof JComboBox) {
            if (!performJComboBoxEvent) {
                return;
            }

            if (e.getStateChange() == ItemEvent.DESELECTED) {
                return; // don't care about the deselect
            }

            @SuppressWarnings("rawtypes")
            JComboBox theChoice = (JComboBox) source;

            int theSelectedChoice = -1;

            int n = Math.min(3, rank);
            for (int i = 0; i < n; i++) {
                if (theChoice.equals(choices[i])) {
                    theSelectedChoice = i;
                }
            }

            if (theSelectedChoice < 0) {
                return; // the selected JComboBox is not a dimension choice
            }

            int theIndex = theChoice.getSelectedIndex();
            if (theIndex == currentIndex[theSelectedChoice]) {
                return; // select the same item, no change
            }

            start[currentIndex[theSelectedChoice]] = 0;

            // reset the selected dimension choice
            startFields[theSelectedChoice].setText("0");
            endFields[theSelectedChoice].setText(String
                    .valueOf(dims[theIndex] - 1));
            strideFields[theSelectedChoice].setText("1");
            maxLabels[theSelectedChoice]
                    .setText(String.valueOf(dims[theIndex]));

            // if the selected choice selects the dimension that is selected by
            // other dimension choice, exchange the dimensions
            for (int i = 0; i < n; i++) {
                if (i == theSelectedChoice) {
                    continue; // don't exchange itself
                }
                else if (theIndex == choices[i].getSelectedIndex()) {
                    setJComboBoxSelectedIndex(choices[i],
                            currentIndex[theSelectedChoice]);
                    startFields[i].setText("0");
                    endFields[i]
                            .setText(String
                                    .valueOf(dims[currentIndex[theSelectedChoice]] - 1));
                    strideFields[i].setText("1");
                    maxLabels[i].setText(String
                            .valueOf(dims[currentIndex[theSelectedChoice]]));
                }
            }

            for (int i = 0; i < n; i++) {
                currentIndex[i] = choices[i].getSelectedIndex();
            }

            // update the navigator
            if (rank > 1) {
                if (isText) {
                    endFields[1].setText(startFields[1].getText());
                }
                else {
                    int hIdx = choices[0].getSelectedIndex();
                    int wIdx = choices[1].getSelectedIndex();
                    transposeChoice.setSelectedIndex(0);

                    // Use transpose option only if the dims are not in original
                    // order
                    if (hIdx < wIdx)
                        transposeChoice.setEnabled(false);
                    else
                        transposeChoice.setEnabled(true);

                    long dims[] = dataset.getDims();
                    int w = (int) dims[wIdx];
                    int h = (int) dims[hIdx];
                    navigator.setDimensionSize(w, h);
                    navigator.updateUI();
                }
            }

            if (rank > 2) {
                endFields[2].setText(startFields[2].getText());
            }
        } // else if (source instanceof JComboBox)
    }

    /** for deal with bit masks only */
    private void checkBitmaskButtons (JToggleButton source) {
        boolean b = false;
        int n = 0;

        if (source.equals(applyBitmaskButton)) {
            if (applyBitmaskButton.isSelected())
                extractBitButton.setSelected(false);
        }
        else if (source.equals(extractBitButton)) {
            if (extractBitButton.isSelected())
                applyBitmaskButton.setSelected(false);
        }

        b = (applyBitmaskButton.isSelected() || extractBitButton.isSelected());
        bitmaskButtons[0].setEnabled(b);
        if (bitmaskButtons[0].isSelected())
            n = 1;

        for (int i = 1; i < bitmaskButtons.length; i++) {
            bitmaskButtons[i].setEnabled(b);
            if (bitmaskButtons[i].isSelected() && !bitmaskButtons[i - 1].isSelected())
                n++;
        }

        // do not allow non-adjacent selection for extracting bits
        if (extractBitButton.isSelected() && n > 1) {
            if (source.equals(extractBitButton) && extractBitButton.isSelected()) {
                applyBitmaskButton.setSelected(true);
                JOptionPane.showMessageDialog(this,
                        "Selecting non-adjacent bits is only allowed \nfor the \"Apply Bitmask\" option.",
                        "Select Bitmask",
                        JOptionPane.ERROR_MESSAGE);
            }
            else if (source instanceof JRadioButton) {
                JOptionPane.showMessageDialog(this,
                        "Please select contiguous bits \nwhen the \"Show Value of Selected Bits\" option is checked.",
                        "Select Bitmask",
                        JOptionPane.ERROR_MESSAGE);
                source.setSelected(false);
            }
        } // if (extractBitButton.isSelected() && n>1) {
    }
}
