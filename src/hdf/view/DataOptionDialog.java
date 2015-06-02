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

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.ScalarDS;

/**
 * DataOptionDialog is an dialog window used to select display options. Display options include
 * selection of subset, display type (image, text, or spreadsheet).
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DataOptionDialog extends JDialog implements ActionListener, ItemListener
{
    /**
     * 
     */
    private static final long      serialVersionUID      = -1078411885690696784L;

    /**
     * The main HDFView.
     */
    private final ViewManager      viewer;

    /** the selected dataset/image */
    private Dataset                dataset;

    /** the rank of the dataset/image */
    private int                    rank;

    /** the starting point of selected subset */
    private long                   start[];

    /** the sizes of all dimensions */
    private long                   dims[];

    /** the selected sizes of all dimensions */
    private long                   selected[];

    /** the stride */
    private long                   stride[];

    /** the indices of the selected dimensions. */
    private int                    selectedIndex[];

    private int                    currentIndex[];

    private JRadioButton           spreadsheetButton, imageButton, base1Button, base0Button;

    private JRadioButton[]         bitmaskButtons;
    private JCheckBox              applyBitmaskButton, extractBitButton;

    private JCheckBox              charCheckbox;

    private BitSet                 bitmask;

    private JButton                bitmaskHelp;

    @SuppressWarnings("rawtypes")
    private JComboBox              choiceTextView;
    @SuppressWarnings("rawtypes")
    private JComboBox              choiceTableView;
    @SuppressWarnings("rawtypes")
    private JComboBox              choiceImageView;
    @SuppressWarnings("rawtypes")
    private JComboBox              choicePalette;
    @SuppressWarnings("rawtypes")
    private JComboBox              choices[];
    @SuppressWarnings("rawtypes")
    private JComboBox              transposeChoice;

    private boolean                isSelectionCancelled;

    private boolean                isTrueColorImage;

    private boolean                isText;

    private boolean                isH5;

    private JLabel                 maxLabels[], selLabel;

    private JTextField             startFields[], endFields[], strideFields[], dataRangeField, fillValueField;

    @SuppressWarnings("rawtypes")
    private JList                  fieldList;

    private final Toolkit          toolkit;

    private final PreviewNavigator navigator;

    private int                    numberOfPalettes;

    /**
     * JComboBox.setSelectedItem() or setSelectedIndex() always fires action event. If you call
     * setSelectedItem() or setSelectedIndex() at itemStateChanged() or actionPerformed(), the
     * setSelectedItem() or setSelectedIndex() will make loop calls of itemStateChanged() or
     * actionPerformed(). This is not what we want. We want the setSelectedItem() or
     * setSelectedIndex() behavior like java.awt.Choice. This flag is used to serve this purpose.
     */
    private boolean                performJComboBoxEvent = false;

    /**
     * Constructs a DataOptionDialog with the given HDFView.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public DataOptionDialog(ViewManager theview, Dataset theDataset) {
        super((JFrame) theview, true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        viewer = theview;
        dataset = theDataset;
        isSelectionCancelled = true;
        isTrueColorImage = false;
        isText = false;
        bitmask = null;
        numberOfPalettes = 1;
        toolkit = Toolkit.getDefaultToolkit();

        if (dataset == null) {
            dispose();
        }
        else {
            setTitle("Dataset Selection - " + dataset.getPath()
                    + dataset.getName());
        }

        isH5 = dataset.getFileFormat().isThisType(
                FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        rank = dataset.getRank();
        if (rank <= 0) {
            dataset.init();
        }
        if (isH5 && (dataset instanceof ScalarDS)) {
            byte[] palRefs = ((ScalarDS) dataset).getPaletteRefs();
            if ((palRefs != null) && (palRefs.length > 8)) {
                numberOfPalettes = palRefs.length / 8;
            }
        }
        rank = dataset.getRank();
        dims = dataset.getDims();
        selected = dataset.getSelectedDims();
        start = dataset.getStartDims();
        selectedIndex = dataset.getSelectedIndex();
        stride = dataset.getStride();
        fieldList = null;

        int h = 1, w = 1;
        h = (int) dims[selectedIndex[0]];
        if (rank > 1) {
            w = (int) dims[selectedIndex[1]];
        }

        transposeChoice = new JComboBox();
        transposeChoice.addItem("Reshape");
        transposeChoice.addItem("Transpose");

        selLabel = new JLabel("", SwingConstants.CENTER);
        navigator = new PreviewNavigator(w, h);

        currentIndex = new int[Math.min(3, rank)];

        choicePalette = new JComboBox();
        choicePalette.setName("modulepalette");
        choiceTextView = new JComboBox((Vector<?>) HDFView.getListOfTextView());
        choiceTextView.setName("moduletext");
        choiceImageView = new JComboBox((Vector<?>) HDFView.getListOfImageView());
        choiceImageView.setName("moduleimage");
        choiceTableView = new JComboBox((Vector<?>) HDFView.getListOfTableView());
        choiceTableView.setName("moduletable");

        choicePalette.addItem("Select palette");
        if (dataset instanceof ScalarDS) {
            String paletteName = ((ScalarDS) dataset).getPaletteName(0);
            if (paletteName == null) {
                paletteName = "Default";
            }
            choicePalette.addItem(paletteName);
            for (int i = 2; i <= numberOfPalettes; i++) {
                paletteName = ((ScalarDS) dataset).getPaletteName(i - 1);
                choicePalette.addItem(paletteName);
            }
        }
        choicePalette.addItem("Gray");
        choicePalette.addItem("ReverseGray");
        choicePalette.addItem("GrayWave");
        choicePalette.addItem("Rainbow");
        choicePalette.addItem("Nature");
        choicePalette.addItem("Wave");

        spreadsheetButton = new JRadioButton("Spreadsheet ", true);
        spreadsheetButton.setMnemonic(KeyEvent.VK_S);
        spreadsheetButton.setName("spreadsheetbutton");
        imageButton = new JRadioButton("Image ");
        imageButton.setMnemonic(KeyEvent.VK_I);
        imageButton.setName("imagebutton");

        charCheckbox = new JCheckBox("Show As Char", false);
        charCheckbox.setMnemonic(KeyEvent.VK_C);
        charCheckbox.setEnabled(false);
        charCheckbox.addItemListener(this);

        extractBitButton = new JCheckBox("Show Value of Selected Bits", false);
        extractBitButton.setMnemonic(KeyEvent.VK_V);
        extractBitButton.setEnabled(false);
        extractBitButton.addItemListener(this);

        applyBitmaskButton = new JCheckBox("Apply Bitmask", false);
        applyBitmaskButton.setMnemonic(KeyEvent.VK_A);
        applyBitmaskButton.setEnabled(false);
        applyBitmaskButton.addItemListener(this);
        applyBitmaskButton.setName("applybitmask");

        //bitmaskHelp = new JButton(ViewProperties.getHelpIcon());
        //bitmaskHelp.setEnabled(false);
        //bitmaskHelp.setToolTipText("Help on how to set bitmask");
        //bitmaskHelp.setMargin(new Insets(0, 0, 0, 0));
        //bitmaskHelp.addActionListener(this);
        //bitmaskHelp.setActionCommand("Help on how to set bitmask");

        // layout the components
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        int w1 = 700 + (ViewProperties.getFontSize() - 12) * 15;
        int h1 = 350 + (ViewProperties.getFontSize() - 12) * 10;
        contentPane.setPreferredSize(new Dimension(w1, h1));

        JPanel centerP = new JPanel();
        centerP.setLayout(new BorderLayout());
        TitledBorder tborder = new TitledBorder("Dimension and Subset Selection");
        tborder.setTitleColor(Color.gray);
        centerP.setBorder(tborder);

        JPanel navigatorP = new JPanel();
        navigatorP.setLayout(new BorderLayout());
        navigatorP.add(navigator, BorderLayout.CENTER);
        navigatorP.add(selLabel, BorderLayout.SOUTH);
        navigatorP.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        navigatorP.setName("navigator");
        performJComboBoxEvent = true;

        // create and initialize these buttons here so the isIndexBase1 method
        // functions properly
        base0Button = new JRadioButton("0-based ");
        base1Button = new JRadioButton("1-based ");
        if (ViewProperties.isIndexBase1())
            base1Button.setSelected(true);
        else
            base0Button.setSelected(true);

        if (dataset instanceof CompoundDS) {
            // setup GUI components for the field selection
            CompoundDS d = (CompoundDS) dataset;
            String[] names = d.getMemberNames();
            fieldList = new JList(names);
            fieldList.addSelectionInterval(0, names.length - 1);
            JPanel fieldP = new JPanel();
            fieldP.setLayout(new BorderLayout());
            w1 = 150 + (ViewProperties.getFontSize() - 12) * 10;
            h1 = 250 + (ViewProperties.getFontSize() - 12) * 15;
            fieldP.setPreferredSize(new Dimension(w1, h1));
            JScrollPane scrollP = new JScrollPane(fieldList);
            fieldP.add(scrollP);
            tborder = new TitledBorder("Select Members");
            tborder.setTitleColor(Color.gray);
            fieldP.setBorder(tborder);
            contentPane.add(fieldP, BorderLayout.WEST);

            JPanel tviewP = new JPanel();
            tviewP.setLayout(new BorderLayout());
            tviewP.add(new JLabel("        TableView:  "), BorderLayout.WEST);
            tviewP.add(choiceTableView, BorderLayout.CENTER);
            tviewP.setBorder(new LineBorder(Color.LIGHT_GRAY));

            centerP.add(tviewP, BorderLayout.SOUTH);
        }
        else if (dataset instanceof ScalarDS) {
            ScalarDS sd = (ScalarDS) dataset;
            isText = sd.isText();

            if (isText) {
                w1 = 700 + (ViewProperties.getFontSize() - 12) * 15;
                h1 = 280 + (ViewProperties.getFontSize() - 12) * 10;
                contentPane.setPreferredSize(new Dimension(w1, h1));
                // add textview selection
                JPanel txtviewP = new JPanel();
                txtviewP.setLayout(new BorderLayout());
                txtviewP.add(new JLabel("          TextView:  "),
                        BorderLayout.WEST);
                txtviewP.add(choiceTextView, BorderLayout.CENTER);
                txtviewP.setBorder(new LineBorder(Color.LIGHT_GRAY));

                centerP.add(txtviewP, BorderLayout.SOUTH);
            }
            else {
                w1 = 800 + (ViewProperties.getFontSize() - 12) * 15;
                h1 = 550 + (ViewProperties.getFontSize() - 12) * 10;
                contentPane.setPreferredSize(new Dimension(w1, h1));
                if (rank > 1) {
                    centerP.add(navigatorP, BorderLayout.WEST);
                }

                // setup GUI components for the display options: table or image
                imageButton.addItemListener(this);
                spreadsheetButton.addItemListener(this);
                ButtonGroup rgroup = new ButtonGroup();
                rgroup.add(spreadsheetButton);
                rgroup.add(imageButton);
                JPanel viewP = new JPanel();
                viewP.setLayout(new GridLayout(2, 1, 5, 5));
                tborder = new TitledBorder("Display As");
                tborder.setTitleColor(Color.gray);
                viewP.setBorder(tborder);

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

        // setup GUI for dimension and subset selection
        JPanel selectionP = new JPanel();
        selectionP.setLayout(new GridLayout(5, 6, 10, 3));
        selectionP.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        centerP.add(selectionP, BorderLayout.CENTER);
        contentPane.add(centerP, BorderLayout.CENTER);

        selectionP.add(new JLabel(" "));
        if (rank > 1)
            selectionP.add(transposeChoice);
        else
            selectionP.add(new JLabel(" "));

        JLabel label = new JLabel("Start:");
        selectionP.add(label);
        label = new JLabel("End: ");
        selectionP.add(label);
        label = new JLabel("Stride:");
        selectionP.add(label);
        label = new JLabel("Max Size");
        selectionP.add(label);

        choices = new JComboBox[3];
        maxLabels = new JLabel[3];
        startFields = new JTextField[3];
        endFields = new JTextField[3];
        strideFields = new JTextField[3];
        JLabel dimLabels[] = { new JLabel("Height", SwingConstants.RIGHT),
                new JLabel("Width", SwingConstants.RIGHT),
                new JLabel("Depth", SwingConstants.RIGHT), };

        String[] dimNames = dataset.getDimNames();
        for (int i = 0; i < 3; i++) {
            choices[i] = new JComboBox();
            choices[i].addItemListener(this);
            for (int j = 0; j < rank; j++) {
                if (dimNames == null) {
                    choices[i].addItem("dim " + j);
                }
                else {
                    choices[i].addItem(dimNames[j]);
                }
            }
            maxLabels[i] = new JLabel("1");
            startFields[i] = new JTextField("0");
            endFields[i] = new JTextField("0");
            strideFields[i] = new JTextField("1");
            selectionP.add(dimLabels[i]);
            selectionP.add(choices[i]);
            selectionP.add(startFields[i]);
            selectionP.add(endFields[i]);
            selectionP.add(strideFields[i]);
            selectionP.add(maxLabels[i]);

            // disable the selection components
            // init() will set them appropriate
            choices[i].setEnabled(false);
            startFields[i].setEnabled(false);
            endFields[i].setEnabled(false);
            strideFields[i].setEnabled(false);
            maxLabels[i].setEnabled(false);
            
            // Provide fields with names for access
            startFields[i].setName("startField"+i);
            endFields[i].setName("endField"+i);
            strideFields[i].setName("strideField"+i);
            choices[i].setName("dimensionBox"+i);
        }

        // add button dimension selection when dimension size >= 4
        JButton button = new JButton("dims...");
        selectionP.add(new JLabel("", SwingConstants.RIGHT));
        selectionP.add(button);

        button.setActionCommand("Select more dimensions");
        button.addActionListener(this);
        button.setEnabled((rank > 3));
        selectionP.add(new JLabel(" "));
        selectionP.add(new JLabel(" "));
        button = new JButton("Reset");
        button.setName("Reset");
        button.setActionCommand("Reset data range");
        button.addActionListener(this);
        selectionP.add(button);
        selectionP.add(new JLabel(" "));

        // add OK and CANCEL buttons
        JPanel confirmP = new JPanel();
        contentPane.add(confirmP, BorderLayout.SOUTH);
        button = new JButton("   Ok   ");
        button.setName("OK");
        button.setMnemonic(KeyEvent.VK_O);
        button.setActionCommand("Ok");
        button.addActionListener(this);
        confirmP.add(button);
        button = new JButton("Cancel");
        button.setName("Cancel");
        button.setMnemonic(KeyEvent.VK_C);
        button.setActionCommand("Cancel");
        button.addActionListener(this);
        confirmP.add(button);

        init();

        // locate the H5Property dialog
        Point l = getParent().getLocation();
        l.x += 250;
        l.y += 80;
        setLocation(l);
        pack();
    }

    @Override
    public void actionPerformed (ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Ok")) {
            // set palette for image view
            if ((dataset instanceof ScalarDS) && imageButton.isSelected()) {
                setPalette();
            }

            isSelectionCancelled = !setSelection();

            if (isSelectionCancelled) {
                return;
            }

            if (dataset instanceof ScalarDS) {
                ((ScalarDS) dataset).setIsImageDisplay(imageButton.isSelected());
            }

            dispose();
        }
        else if (cmd.equals("Cancel")) {
            dispose();
        }
        else if (cmd.equals("Reset data range")) {
            int n = startFields.length;

            for (int i = 0; i < n; i++) {
                startFields[i].setText("0");
                strideFields[i].setText("1");
                long l = Long.valueOf(maxLabels[i].getText()) - 1;
                endFields[i].setText(String.valueOf(l));
            }
        }
        else if (cmd.equals("Select more dimensions")) {
            if (rank < 4) {
                return;
            }

            int idx = 0;
            Vector<Object> choice4 = new Vector<Object>(rank);
            int[] choice4Index = new int[rank - 3];
            for (int i = 0; i < rank; i++) {
                if ((i != currentIndex[0]) && (i != currentIndex[1])
                        && (i != currentIndex[2])) {
                    choice4.add(choices[0].getItemAt(i));
                    choice4Index[idx++] = i;
                }
            }

            String msg = "Select slice location for dimension(s):\n\""
                    + choice4.get(0) + " [0 .. " + (dims[choice4Index[0]] - 1)
                    + "]\"";
            String initValue = String.valueOf(start[choice4Index[0]]);
            int n = choice4.size();
            for (int i = 1; i < n; i++) {
                msg += " x \"" + choice4.get(i) + " [0 .. "
                        + (dims[choice4Index[i]] - 1) + "]\"";
                initValue += " x " + String.valueOf(start[choice4Index[i]]);
            }

            String result = JOptionPane.showInputDialog(this, msg, initValue);
            if ((result == null) || ((result = result.trim()) == null)
                    || (result.length() < 1)) {
                return;
            }

            StringTokenizer st = new StringTokenizer(result, "x");
            if (st.countTokens() < n) {
                JOptionPane.showMessageDialog(this,
                        "Number of dimension(s) is less than " + n + "\n"
                                + result, "Select Slice Location",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            long[] start4 = new long[n];
            for (int i = 0; i < n; i++) {
                try {
                    start4[i] = Long.parseLong(st.nextToken().trim());
                }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(),
                            "Select Slice Location", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if ((start4[i] < 0) || (start4[i] >= dims[choice4Index[i]])) {
                    JOptionPane.showMessageDialog(this,
                            "Slice location is out of range.\n" + start4[i]
                                    + " >= " + dims[choice4Index[i]],
                            "Select Slice Location", JOptionPane.ERROR_MESSAGE);
                    return;
                }

            }

            for (int i = 0; i < n; i++) {
                start[choice4Index[i]] = start4[i];
            }
        } // else if (cmd.equals("Select more dimensions"))
        else if (cmd.equals("Help on how to set bitmask")) {
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

    @Override
    public void itemStateChanged (ItemEvent e) {
        Object source = e.getSource();

        if (source.equals(imageButton)) {
            choicePalette.setEnabled(!isTrueColorImage);
            dataRangeField.setEnabled(true);
            fillValueField.setEnabled(true);
            choiceImageView.setEnabled(true);
            choiceTableView.setEnabled(false);
            charCheckbox.setSelected(false);
            charCheckbox.setEnabled(false);
        }
        else if (source.equals(spreadsheetButton)) {
            choicePalette.setEnabled(false);
            choiceImageView.setEnabled(false);
            choiceTableView.setEnabled(true);
            dataRangeField.setEnabled(false);
            fillValueField.setEnabled(false);
            Datatype dtype = dataset.getDatatype();
            int tclass = dtype.getDatatypeClass();
            charCheckbox.setEnabled((tclass == Datatype.CLASS_CHAR ||
                    tclass == Datatype.CLASS_INTEGER) &&
                    (dtype.getDatatypeSize() == 1));
        }
        else if (source instanceof JToggleButton) {
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

    /** Returns true if the data selection is cancelled. */
    public boolean isCancelled ( ) {
        return isSelectionCancelled;
    }

    /** Returns true if the display option is image. */
    public boolean isImageDisplay ( ) {
        return imageButton.isSelected();
    }

    public boolean isIndexBase1 ( ) {
        if (base1Button == null)
            return false;

        return base1Button.isSelected();
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

    /**
     * Set the initial state of all the variables
     */
    private void init ( ) {
        // set the imagebutton state
        boolean isImage = false;

        if (dataset instanceof ScalarDS) {
        	if(!((ScalarDS) dataset).isText()) {
        		ScalarDS sd = (ScalarDS) dataset;
        		isImage = sd.isImageDisplay();
        		isTrueColorImage = sd.isTrueColor();
            	// compound datasets don't have data range or fill values
            	// (JAVA-1825)
            	dataRangeField.setEnabled(isImage);
            	fillValueField.setEnabled(isImage);
            }
        }
        else if (dataset instanceof CompoundDS) {
            imageButton.setEnabled(false);
        }

        choiceTableView.setEnabled(!isImage);
        choiceImageView.setEnabled(isImage);
        imageButton.setSelected(isImage);
        choicePalette.setEnabled(isImage && !isTrueColorImage);

        int n = Math.min(3, rank);
        long endIdx = 0;
        for (int i = 0; i < n; i++) {
            choices[i].setEnabled(true);
            startFields[i].setEnabled(true);
            endFields[i].setEnabled(true);
            strideFields[i].setEnabled(true);
            maxLabels[i].setEnabled(true);

            int idx = selectedIndex[i];
            endIdx = start[idx] + selected[idx] * stride[idx];
            if (endIdx >= dims[idx]) {
                endIdx = dims[idx];
            }

            setJComboBoxSelectedIndex(choices[i], idx);
            maxLabels[i].setText(String.valueOf(dims[idx]));
            startFields[i].setText(String.valueOf(start[idx]));
            endFields[i].setText(String.valueOf(endIdx - 1));

            if (!isH5 && (dataset instanceof CompoundDS)) {
                strideFields[i].setEnabled(false);
            }
            else {
                strideFields[i].setText(String.valueOf(stride[idx]));
            }
        }

        if (rank > 1) {
            transposeChoice
                    .setEnabled((choices[0].getSelectedIndex() > choices[1]
                            .getSelectedIndex()));

            if (isText) {
                endFields[1].setEnabled(false);
                endFields[1].setText(startFields[1].getText());
            }
        }

        if (rank > 2) {
            endFields[2].setEnabled(false);
            strideFields[2].setEnabled(false);
            if (isTrueColorImage && imageButton.isSelected()) {
                choices[0].setEnabled(false);
                choices[1].setEnabled(false);
                choices[2].setEnabled(false);
                startFields[2].setEnabled(false);
                startFields[2].setText("0");
                endFields[2].setText("0");
            }
            else {
                choices[0].setEnabled(true);
                choices[1].setEnabled(true);
                choices[2].setEnabled(true);
                startFields[2].setEnabled(true);
                startFields[2].setText(String.valueOf(start[selectedIndex[2]]));
                // endFields[2].setEnabled(!isText);
                endFields[2].setText(startFields[2].getText());
            }
        }

        for (int i = 0; i < n; i++) {
            currentIndex[i] = choices[i].getSelectedIndex();
        }

        // reset show char button
        Datatype dtype = dataset.getDatatype();
        int tclass = dtype.getDatatypeClass();
        if (tclass == Datatype.CLASS_CHAR || tclass == Datatype.CLASS_INTEGER) {
            int tsize = dtype.getDatatypeSize();
            charCheckbox.setEnabled((tsize == 1) && spreadsheetButton.isSelected());
            extractBitButton.setEnabled(tsize <= 8);
            applyBitmaskButton.setEnabled(tsize <= 8);
        }
        else {
            charCheckbox.setEnabled(false);
            charCheckbox.setSelected(false);
            extractBitButton.setEnabled(false);
            applyBitmaskButton.setEnabled(false);
        }
    }

    /**
     * JComboBox.setSelectedItem() or setSelectedIndex() always fires action event. If you call
     * setSelectedItem() or setSelectedIndex() at itemStateChanged() or actionPerformed(), the
     * setSelectedItem() or setSelectedIndex() will make loop calls of itemStateChanged() or
     * actionPerformed(). This is not what we want. We want the setSelectedItem() or
     * setSelectedIndex() behavior like java.awt.Choice. This flag is used to serve this purpose.
     */
    @SuppressWarnings("rawtypes")
    private void setJComboBoxSelectedIndex (JComboBox box, int idx) {
        performJComboBoxEvent = false;
        box.setSelectedIndex(idx);
        performJComboBoxEvent = true;
    }

    private void setPalette ( ) {
        if (!(dataset instanceof ScalarDS)) {
            return;
        }

        byte[][] pal = null;
        int palChoice = choicePalette.getSelectedIndex();

        if (palChoice == 0) {
            return; /* using default palette */
        }

        if (palChoice == numberOfPalettes + 1) {
            pal = Tools.createGrayPalette();
        }
        else if (palChoice == numberOfPalettes + 2) {
            pal = Tools.createReverseGrayPalette();
        }
        else if (palChoice == numberOfPalettes + 3) {
            pal = Tools.createGrayWavePalette();
        }
        else if (palChoice == numberOfPalettes + 4) {
            pal = Tools.createRainbowPalette();
        }
        else if (palChoice == numberOfPalettes + 5) {
            pal = Tools.createNaturePalette();
        }
        else if (palChoice == numberOfPalettes + 6) {
            pal = Tools.createWavePalette();
        }
        else if ((palChoice > 0) && (palChoice <= numberOfPalettes)) {
            // multiple palettes attached
            pal = ((ScalarDS) dataset).readPalette(palChoice - 1);
        }

        ((ScalarDS) dataset).setPalette(pal);
    }

    private boolean setSelection ( ) {
        long[] n0 = { 0, 0, 0 }; // start
        long[] n1 = { 0, 0, 0 }; // end
        long[] n2 = { 1, 1, 1 }; // stride
        int[] sIndex = { 0, 1, 2 };
        boolean retVal = true;

        int n = Math.min(3, rank);
        for (int i = 0; i < n; i++) {
            sIndex[i] = choices[i].getSelectedIndex();

            try {
                n0[i] = Long.parseLong(startFields[i].getText());
                if (i < 2) {
                    n1[i] = Long.parseLong(endFields[i].getText());
                    n2[i] = Long.parseLong(strideFields[i].getText());
                }
            }
            catch (NumberFormatException ex) {
                toolkit.beep();
                JOptionPane.showMessageDialog((JFrame) viewer, ex.getMessage(),
                        getTitle(), JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // silently correct errors
            if (n0[i] < 0) {
                n0[i] = 0; // start
            }
            if (n0[i] >= dims[sIndex[i]]) {
                n0[i] = dims[sIndex[i]] - 1;
            }
            if (n1[i] < 0) {
                n1[i] = 0; // end
            }
            if (n1[i] >= dims[sIndex[i]]) {
                n1[i] = dims[sIndex[i]] - 1;
            }
            if (n0[i] > n1[i]) {
                n1[i] = n0[i]; // end <= start
            }
            if (n2[i] > dims[sIndex[i]]) {
                n2[i] = dims[sIndex[i]];
            }
            if (n2[i] <= 0) {
                n2[i] = 1; // stride cannot be zero
            }
        } // for (int i=0; i<n; i++)

        if (dataset instanceof CompoundDS) {
            CompoundDS d = (CompoundDS) dataset;
            int[] selectedFieldIndices = fieldList.getSelectedIndices();
            if ((selectedFieldIndices == null)
                    || (selectedFieldIndices.length < 1)) {
                toolkit.beep();
                JOptionPane.showMessageDialog((JFrame) viewer,
                        "No member/field is selected.", getTitle(),
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            d.setMemberSelection(false); // deselect all members
            for (int i = 0; i < selectedFieldIndices.length; i++) {
                d.selectMember(selectedFieldIndices[i]);
            }
        }
        else {
            ScalarDS ds = (ScalarDS) dataset;
            
            if(!ds.isText()) {
            	StringTokenizer st = new StringTokenizer(dataRangeField.getText(), ",");
            	if (st.countTokens() == 2) {
            		double min = 0, max = 0;
            		try {
            			min = Double.valueOf(st.nextToken());
            			max = Double.valueOf(st.nextToken());
            		}
            		catch (Throwable ex) {
            		}
            		if (max > min)
            			ds.setImageDataRange(min, max);
            	}
            	st = new StringTokenizer(fillValueField.getText(), ",");
            	while (st.hasMoreTokens()) {
            		double x = 0;
            		try {
            			x = Double.valueOf(st.nextToken());
            			ds.addFilteredImageValue(x);
            		}
            		catch (Throwable ex) {
            		}
            	}
            }
        }

        // reset selected size
        for (int i = 0; i < rank; i++) {
            selected[i] = 1;
            stride[i] = 1;
        }

        // find no error, set selection the the dataset object
        for (int i = 0; i < n; i++) {
            selectedIndex[i] = sIndex[i];
            start[selectedIndex[i]] = n0[i];
            if (i < 2) {
                selected[selectedIndex[i]] = (int) ((n1[i] - n0[i]) / n2[i]) + 1;
                stride[selectedIndex[i]] = n2[i];
            }
        }

        if ((rank > 1) && isText) {
            selected[selectedIndex[1]] = 1;
            stride[selectedIndex[1]] = 1;
        }
        else if ((rank > 2) && isTrueColorImage && imageButton.isSelected()) {
            start[selectedIndex[2]] = 0;
            selected[selectedIndex[2]] = 3;
        }

        // clear the old data
        dataset.clearData();

        retVal = setBitmask();

        return retVal;
    }

    private boolean setBitmask ( )
    {
        boolean isAll = false, isNothing = false;

        if (bitmaskButtons == null) {
            bitmask = null;
            return true;
        }

        if (!(applyBitmaskButton.isSelected() || extractBitButton.isSelected())) {
            bitmask = null;
            return true;
        }

        int len = bitmaskButtons.length;
        for (int i = 0; i < len; i++) {
            isAll = (isAll && bitmaskButtons[i].isSelected());
            isNothing = (isNothing && !bitmaskButtons[i].isSelected());
        }

        if (isAll || isNothing) {
            bitmask = null;
            return true;
        }

        if (bitmask == null)
            bitmask = new BitSet(len);

        for (int i = 0; i < len; i++) {
            bitmask.set(i, bitmaskButtons[i].isSelected());
        }

        return true;
    }

    /** SubsetNavigator draws selection rectangle of subset. */
    private class PreviewNavigator extends JComponent implements MouseListener,
            MouseMotionListener {
        private static final long serialVersionUID = -4458114008420664965L;
        private final int         NAVIGATOR_SIZE   = 150;
        private int               dimX, dimY, x, y;
        private double            r;
        private Point             startPosition;                           // mouse
                                                                            // clicked
                                                                            // position
        private Rectangle         selectedArea;
        private Image             previewImage     = null;

        private PreviewNavigator(int w, int h) {
            dimX = w;
            dimY = h;
            if (dimX > dimY) {
                x = NAVIGATOR_SIZE;
                r = dimX / (double) x;
                y = (int) (dimY / r);
            }
            else {
                y = NAVIGATOR_SIZE;
                r = dimY / (double) y;
                x = (int) (dimX / r);
            }

            selectedArea = new Rectangle();
            setPreferredSize(new Dimension(NAVIGATOR_SIZE, NAVIGATOR_SIZE));
            try {
                previewImage = createPreviewImage();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            addMouseListener(this);
            addMouseMotionListener(this);
        }

        private Image createPreviewImage ( ) throws Exception {
            if ((rank <= 1) || !(dataset instanceof ScalarDS)) {
                return null;
            }

            Image preImage = null;
            ScalarDS sd = (ScalarDS) dataset;

            if (sd.isText()) {
                return null;
            }

            // backup the selection
            long[] strideBackup = new long[rank];
            long[] selectedBackup = new long[rank];
            long[] startBackup = new long[rank];
            int[] selectedIndexBackup = new int[3];
            System.arraycopy(stride, 0, strideBackup, 0, rank);
            System.arraycopy(selected, 0, selectedBackup, 0, rank);
            System.arraycopy(start, 0, startBackup, 0, rank);
            System.arraycopy(selectedIndex, 0, selectedIndexBackup, 0, 3);

            // set the selection for preview
            for (int i = 0; i < rank; i++) {
                start[i] = 0;
                stride[i] = 1;
                selected[i] = 1;
            }

            if (choices != null) {
                selectedIndex[0] = choices[0].getSelectedIndex();
                selectedIndex[1] = choices[1].getSelectedIndex();
            }
            long steps = (long) Math.ceil(r);
            selected[selectedIndex[0]] = (dims[selectedIndex[0]] / steps);
            selected[selectedIndex[1]] = (dims[selectedIndex[1]] / steps);
            stride[selectedIndex[0]] = stride[selectedIndex[1]] = steps;

            if (selected[selectedIndex[0]] == 0) {
                selected[selectedIndex[0]] = 1;
            }
            if (selected[selectedIndex[1]] == 0) {
                selected[selectedIndex[1]] = 1;
            }

            if (isTrueColorImage && (start.length > 2)) {
                start[selectedIndex[2]] = 0;
                selected[selectedIndex[2]] = 3;
                stride[selectedIndex[2]] = 1;
            }

            // update the ration of preview image size to the real dataset
            y = (int) selected[selectedIndex[0]];
            x = (int) selected[selectedIndex[1]];
            r = Math.min((double) dims[selectedIndex[0]]
                    / (double) selected[selectedIndex[0]],
                    (double) dims[selectedIndex[1]]
                            / (double) selected[selectedIndex[1]]);

            try {
                Object data = sd.read();
                int h = sd.getHeight();
                int w = sd.getWidth();

                byte[] bData = Tools.getBytes(data, sd.getImageDataRange(), w, h, false, sd.getFilteredImageValues(), null);

                if (isTrueColorImage) {
                    boolean isPlaneInterlace = (sd.getInterlace() == ScalarDS.INTERLACE_PLANE);
                    preImage = Tools.createTrueColorImage(bData,
                            isPlaneInterlace, w, h);
                }
                else {
                    byte[][] imagePalette = sd.getPalette();
                    if (imagePalette == null) {
                        imagePalette = Tools.createGrayPalette();
                    }

                    if ((isH5 || (rank > 2))
                            && (selectedIndex[0] > selectedIndex[1])) {
                        // transpose data
                        int n = bData.length;
                        byte[] bData2 = new byte[n];
                        for (int i = 0; i < h; i++) {
                            for (int j = 0; j < w; j++) {
                                bData[i * w + j] = bData2[j * h + i];
                            }
                        }
                    }
                    if (!isH5 && !sd.isDefaultImageOrder() && (selectedIndex[1] > selectedIndex[0])) {
                        // transpose data for hdf4 images where selectedIndex[1]
                        // > selectedIndex[0]
                        int n = bData.length;
                        byte[] bData2 = new byte[n];
                        for (int i = 0; i < h; i++) {
                            for (int j = 0; j < w; j++) {
                                bData[i * w + j] = bData2[j * h + i];
                            }
                        }
                    }
                    preImage = Tools.createIndexedImage(null, bData, imagePalette, w, h);
                }
            }
            finally {
                // set back the original selection
                System.arraycopy(strideBackup, 0, stride, 0, rank);
                System.arraycopy(selectedBackup, 0, selected, 0, rank);
                System.arraycopy(startBackup, 0, start, 0, rank);
                System.arraycopy(selectedIndexBackup, 0, selectedIndex, 0, 3);
            }

            return preImage;
        }

        @Override
        public void paint (Graphics g) {
            g.setColor(Color.blue);

            if (previewImage != null) {
                g.drawImage(previewImage, 0, 0, this);
            }
            else {
                g.fillRect(0, 0, x, y);
            }

            int w = selectedArea.width;
            int h = selectedArea.height;
            if ((w > 0) && (h > 0)) {
                g.setColor(Color.red);
                g.drawRect(selectedArea.x, selectedArea.y, w, h);
            }
        }

        @Override
        public void mousePressed (MouseEvent e) {
            startPosition = e.getPoint();
            selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
        }

        @Override
        public void mouseClicked (MouseEvent e) {
            startPosition = e.getPoint();
            selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
            repaint();
        }

        @Override
        public void mouseDragged (MouseEvent e) {
            Point p0 = startPosition;
            Point p1 = e.getPoint();

            int x0 = Math.max(0, Math.min(p0.x, p1.x));
            int y0 = Math.max(0, Math.min(p0.y, p1.y));
            int x1 = Math.min(x, Math.max(p0.x, p1.x));
            int y1 = Math.min(y, Math.max(p0.y, p1.y));

            int w = x1 - x0;
            int h = y1 - y0;
            selectedArea.setBounds(x0, y0, w, h);

            try {
                updateSelection(x0, y0, w, h);
            }
            catch (Exception ex) {
            }

            repaint();
        }

        private void updateSelection (int x0, int y0, int w, int h) {
            int i0 = 0, i1 = 0;
            String selStr;

            i0 = (int) (y0 * r);
            if (i0 > dims[currentIndex[0]]) {
                i0 = (int) dims[currentIndex[0]];
            }
            startFields[0].setText(String.valueOf(i0));

            i1 = (int) ((y0 + h) * r);

            if (i1 < i0) {
                i1 = i0;
            }
            endFields[0].setText(String.valueOf(i1));

            selStr = String.valueOf((int) (h * r));

            if (rank > 1) {
                i0 = (int) (x0 * r);
                if (i0 > dims[currentIndex[1]]) {
                    i0 = (int) dims[currentIndex[1]];
                }
                startFields[1].setText(String.valueOf(i0));

                i1 = (int) ((x0 + w) * r);
                if (i1 < i0) {
                    i1 = i0;
                }
                endFields[1].setText(String.valueOf(i1));

                selStr += " x " + ((int) (w * r));
            }

            selLabel.setText(selStr);
        }

        @Override
        public void mouseReleased (MouseEvent e) {
        }

        @Override
        public void mouseEntered (MouseEvent e) {
        }

        @Override
        public void mouseExited (MouseEvent e) {
        }

        @Override
        public void mouseMoved (MouseEvent e) {
        }

        private void setDimensionSize (int w, int h) {
            dimX = w;
            dimY = h;
            if (dimX > dimY) {
                x = NAVIGATOR_SIZE;
                r = dimX / (double) x;
                y = (int) (dimY / r);
            }
            else {
                y = NAVIGATOR_SIZE;
                r = dimY / (double) y;
                x = (int) (dimX / r);
            }
            setPreferredSize(new Dimension(NAVIGATOR_SIZE, NAVIGATOR_SIZE));
            selectedArea.setSize(0, 0);
            try {
                previewImage = createPreviewImage();
            }
            catch (Exception ex) {
            }

            repaint();
        }
    } // private class SubsetNavigator extends JComponent

    /**
     * 
     * @return true if display the data as characters; otherwise, display as numbers.
     */
    public boolean isDisplayTypeChar ( ) {
        return charCheckbox.isSelected();
    }

    /**
     * Return the bitmask.
     */
    public BitSet getBitmask ( ) {
        if (bitmask == null)
            return null;

        if (!extractBitButton.isEnabled())
            return null;

        // do not use bitmask if it is empty (all bits are zero)
        if (bitmask.isEmpty())
            return null;

        boolean isAllSelected = true;
        int size = bitmask.size();
        for (int i = 0; i < size; i++)
            isAllSelected = (bitmask.get(i) && isAllSelected);

        // do not use bitmask if it is full (all bits are one)
        if (isAllSelected)
            return null;

        return bitmask;
    }

    /**
     * Check if it only apply bitmask.
     */
    public boolean isApplyBitmaskOnly ( )
    {
        if (getBitmask() == null)
            return false;

        return applyBitmaskButton.isSelected();
    }

    /**
     * 
     * @return true if transpose the data in 2D table; otherwise, do not transpose the data.
     */
    public boolean isTransposed ( ) {
        return (transposeChoice.getSelectedIndex() == 1);
    }

    /** return the name of selected dataview */
    public String getDataViewName ( ) {
        String viewName = null;

        if (isText) {
            viewName = (String) choiceTextView.getSelectedItem();
        }
        else if (isImageDisplay()) {
            viewName = (String) choiceImageView.getSelectedItem();
        }
        else {
            viewName = (String) choiceTableView.getSelectedItem();
        }

        return viewName;
    }
}
