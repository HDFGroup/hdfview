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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.Image;
import java.util.BitSet;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.ScalarDS;

/**
 * DataOptionDialog is an dialog window used to select display options. Display options include
 * selection of subset, display type (image, text, or spreadsheet).
 *
 * @author Jordan T. Henderson
 * @version 2.4 3/26/2016
 */
public class DataOptionDialog extends Dialog {

    private Shell                  shell;
    
    private Font                   curFont;

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

    private BitSet                 bitmask;

    private Button                 spreadsheetButton, imageButton, base1Button, base0Button;
    private Button                 charCheckbox;
    private Button                 bitmaskHelp;
    private Button                 applyBitmaskButton, extractBitButton;
    private Button[]               bitmaskButtons;

    private Combo                  choiceTextView;
    private Combo                  choiceTableView;
    private Combo                  choiceImageView;
    private Combo                  choicePalette;
    private Combo                  transposeChoice;
    private Combo[]                choices;

    private boolean                isSelectionCancelled;

    private boolean                isTrueColorImage;

    private boolean                isText;

    private boolean                isH5;

    private boolean                isImageDisplay = false;
    private boolean                isDisplayTypeChar = false;
    private boolean                isTransposed = false;
    private boolean                isIndexBase1 = false;
    private boolean                isApplyBitmaskOnly = false;

    private String                 dataViewName;

    private Label                  maxLabels[];

    private Text                   startFields[], endFields[], strideFields[], dataRangeField, fillValueField;

    private List                   fieldList;

    private PreviewNavigator       navigator;

    private int                    numberOfPalettes;

    /** the selected dataset/image */
    private Dataset                dataset;


    /**
     * Constructs a DataOptionDialog with the given HDFView.
     *
     * @param parent
     *           the parent of this dialog
     * @param theDataset
     *           the dataset associated with this dialog
     */
    public DataOptionDialog(Shell parent, Dataset theDataset) {
        super(parent, SWT.APPLICATION_MODAL);

        if(theDataset == null) return;

        dataset = theDataset;
        
        try {
            curFont = new Font(
                    Display.getCurrent(),
                    ViewProperties.getFontType(),
                    ViewProperties.getFontSize(),
                    SWT.NORMAL);
        }
        catch (Exception ex) {
            curFont = null;
        }

        isSelectionCancelled = true;
        isTrueColorImage = false;
        isText = false;
        bitmask = null;
        numberOfPalettes = 1;

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
        currentIndex = new int[Math.min(3, rank)];

        maxLabels = new Label[3];
        startFields = new Text[3];
        endFields = new Text[3];
        strideFields = new Text[3];
        choices = new Combo[3];

        isH5 = dataset.getFileFormat().isThisType(
                FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
    }

    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("Dataset Selection - " + dataset.getPath() + dataset.getName());
        shell.setImage(ViewProperties.getHdfIcon());
        shell.setLayout(new GridLayout(1, true));

        if (dataset instanceof ScalarDS) {
            ScalarDS ds = (ScalarDS) dataset;
            isText = ds.isText();

            if(isText) {
                createTextContents();
            } else {
                createScalarDSContents();
            }
        }
        else if (dataset instanceof CompoundDS) {
            createCompoundDSContents();
        }

        // Create Ok/Cancel button region
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("  &Ok  ");
        okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        okButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                // set palette for image view
                if(imageButton != null) {
                    if ((dataset instanceof ScalarDS) && imageButton.getSelection()) {
                        setPalette();
                    }
                }

                isSelectionCancelled = !setSelection();

                if (isSelectionCancelled) {
                    return;
                }

                if(imageButton != null) {
                    if (dataset instanceof ScalarDS) {
                        ((ScalarDS) dataset).setIsImageDisplay(imageButton.getSelection());
                    }
                }

                shell.notifyListeners(SWT.Close, null);

                shell.dispose();
            }
        });

        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setFont(curFont);
        cancelButton.setText("&Cancel");
        cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                shell.notifyListeners(SWT.Close, null);

                shell.dispose();
            }
        });

        shell.pack();

        init();

        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        org.eclipse.swt.graphics.Rectangle parentBounds = parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

        shell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event e) {
                if (imageButton == null) {
                    isImageDisplay = false;
                } else {
                    isImageDisplay = imageButton.getSelection();
                }

                if (charCheckbox == null) {
                    isDisplayTypeChar = false;
                } else {
                    isDisplayTypeChar = charCheckbox.getSelection();
                }

                if (transposeChoice == null) {
                    isTransposed = false;
                } else {
                    isTransposed = transposeChoice.getSelectionIndex() == 1;
                }

                if (base1Button == null) {
                    isIndexBase1 = false;
                } else {
                    isIndexBase1 = base1Button.getSelection();
                }

                if (applyBitmaskButton == null) {
                    isApplyBitmaskOnly = false;
                } else {
                    isApplyBitmaskOnly = applyBitmaskButton.getSelection();
                }

                if (isText) {
                    dataViewName = choiceTextView.getItem(choiceTextView.getSelectionIndex());
                }
                else if (isImageDisplay()) {
                    dataViewName = choiceImageView.getItem(choiceImageView.getSelectionIndex());
                }
                else {
                    dataViewName = choiceTableView.getItem(choiceTableView.getSelectionIndex());
                }
            }
        });
        
        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null) curFont.dispose();
            }
        });

        shell.open();
        
        // Prevent SWT from selecting TableView button by
        // default if dataset is an image
        if (dataset instanceof ScalarDS) {
            if (((ScalarDS) dataset).isImageDisplay()) {
                spreadsheetButton.setSelection(false);
            }
        }

        Display display = parent.getDisplay();
        while(!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    /**
     * Set the initial state of all the variables
     */
    private void init() {
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

        if(choiceTableView != null) {
            choiceTableView.setEnabled(!isImage);
        }

        if(choiceImageView != null) {
            choiceImageView.setEnabled(isImage);
        }

        if(imageButton != null) {
            imageButton.setSelection(isImage);
        }

        if(choicePalette != null) {
            choicePalette.setEnabled(isImage && !isTrueColorImage);
        }

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

            choices[i].select(idx);
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
            transposeChoice.setEnabled((choices[0].getSelectionIndex() > choices[1]
                           .getSelectionIndex()));

            if (isText) {
                endFields[1].setEnabled(false);
                endFields[1].setText(startFields[1].getText());
            }
        }

        if (rank > 2) {
            endFields[2].setEnabled(false);
            strideFields[2].setEnabled(false);

            if (isTrueColorImage && imageButton != null) {
                if(imageButton.getSelection()) {
                    choices[0].setEnabled(false);
                    choices[1].setEnabled(false);
                    choices[2].setEnabled(false);
                    startFields[2].setEnabled(false);
                    startFields[2].setText("0");
                    endFields[2].setText("0");
                }
            }
            else {
                choices[0].setEnabled(true);
                choices[1].setEnabled(true);
                choices[2].setEnabled(true);
                startFields[2].setEnabled(true);
                startFields[2].setText(String.valueOf(start[selectedIndex[2]]));
                endFields[2].setText(startFields[2].getText());
            }
        }

        for (int i = 0; i < n; i++) {
            currentIndex[i] = choices[i].getSelectionIndex();
        }

        // reset show char button
        Datatype dtype = dataset.getDatatype();
        int tclass = dtype.getDatatypeClass();
        if (tclass == Datatype.CLASS_CHAR || tclass == Datatype.CLASS_INTEGER) {
            int tsize = dtype.getDatatypeSize();

            if(charCheckbox != null) {
                charCheckbox.setEnabled((tsize == 1) && spreadsheetButton.getSelection());
            }

            if(extractBitButton != null) {
                extractBitButton.setEnabled(tsize <= 8);
            }

            if(applyBitmaskButton != null) {
                applyBitmaskButton.setEnabled(tsize <= 8);
            }
        }
        else {
            if(charCheckbox != null) {
                charCheckbox.setEnabled(false);
                charCheckbox.setSelection(false);
            }

            if(extractBitButton != null) {
                extractBitButton.setEnabled(false);
            }

            if(applyBitmaskButton != null) {
                applyBitmaskButton.setEnabled(false);
            }
        }
    }

    private void setPalette() {
        if (!(dataset instanceof ScalarDS)) {
            return;
        }

        byte[][] pal = null;
        int palChoice = choicePalette.getSelectionIndex();

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

    private boolean setSelection() {
        long[] n0 = { 0, 0, 0 }; // start
        long[] n1 = { 0, 0, 0 }; // end
        long[] n2 = { 1, 1, 1 }; // stride
        int[] sIndex = { 0, 1, 2 };
        boolean retVal = true;

        int n = Math.min(3, rank);
        for (int i = 0; i < n; i++) {
            sIndex[i] = choices[i].getSelectionIndex();

            try {
                n0[i] = Long.parseLong(startFields[i].getText());
                if (i < 2) {
                    n1[i] = Long.parseLong(endFields[i].getText());
                    n2[i] = Long.parseLong(strideFields[i].getText());
                }
            }
            catch (NumberFormatException ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, ex.getMessage(), shell.getText());
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
            int[] selectedFieldIndices = fieldList.getSelectionIndices();
            if ((selectedFieldIndices == null)
                    || (selectedFieldIndices.length < 1)) {
                shell.getDisplay().beep();
                Tools.showError(shell, "No member/field is selected.", shell.getText());
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
        else if ((rank > 2) && isTrueColorImage && imageButton.getSelection()) {
            start[selectedIndex[2]] = 0;
            selected[selectedIndex[2]] = 3;
        }

        // clear the old data
        dataset.clearData();

        retVal = setBitmask();

        return retVal;
    }

    private boolean setBitmask() {
        boolean isAll = false, isNothing = false;

        if (bitmaskButtons == null) {
            bitmask = null;
            return true;
        }

        if (!(applyBitmaskButton.getSelection() || extractBitButton.getSelection())) {
            bitmask = null;
            return true;
        }

        int len = bitmaskButtons.length;
        for (int i = 0; i < len; i++) {
            isAll = (isAll && bitmaskButtons[i].getSelection());
            isNothing = (isNothing && !bitmaskButtons[i].getSelection());
        }

        if (isAll || isNothing) {
            bitmask = null;
            return true;
        }

        if (bitmask == null)
            bitmask = new BitSet(len);

        for (int i = 0; i < len; i++) {
            /* Bitmask buttons are indexed from highest to lowest */
            bitmask.set(i, bitmaskButtons[len - i - 1].getSelection());
        }

        return true;
    }

    /** @return true if the display option is image. */
    public boolean isImageDisplay() {
        return isImageDisplay;
    }

    /** @return true if the index starts with 0. */
    public boolean isIndexBase1() {
        return isIndexBase1;
    }

    /**
     * @return the bitmask.
     */
    public BitSet getBitmask() {
        if (bitmask == null)
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

    /** @return the name of the selected dataview */
    public String getDataViewName() {
        return dataViewName;
    }

    /**
     *
     * @return true if display the data as characters; otherwise, display as numbers.
     */
    public boolean isDisplayTypeChar() {
        return isDisplayTypeChar;
    }

    /**
     * @return if it only apply bitmask.
     */
    public boolean isApplyBitmaskOnly() {
        return isApplyBitmaskOnly;
    }

    /**
     *
     * @return true if transpose the data in 2D table; otherwise, do not transpose the data.
     */
    public boolean isTransposed() {
        return isTransposed;
    }

    /** @return true if the data selection is cancelled. */
    public boolean isCancelled() {
        return isSelectionCancelled;
    }

    private void createScalarDSContents() {
        // Create display type region
        org.eclipse.swt.widgets.Group displayAsGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        displayAsGroup.setFont(curFont);
        displayAsGroup.setText("Display As");
        displayAsGroup.setLayout(new GridLayout(1, true));
        displayAsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        int tclass = dataset.getDatatype().getDatatypeClass();

        Composite spreadsheetComposite = new Composite(displayAsGroup, SWT.BORDER);
        spreadsheetComposite.setLayout(new GridLayout(2, false));
        spreadsheetComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        spreadsheetButton = new Button(spreadsheetComposite, SWT.RADIO);
        spreadsheetButton.setFont(curFont);
        spreadsheetButton.setText("&Spreadsheet");
        spreadsheetButton.setSelection(true);
        spreadsheetButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        spreadsheetButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                imageButton.setSelection(!spreadsheetButton.getSelection());
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
        });

        charCheckbox = new Button(spreadsheetComposite, SWT.CHECK);
        charCheckbox.setFont(curFont);
        charCheckbox.setText("Show As &Char");
        charCheckbox.setSelection(false);
        charCheckbox.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
        charCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

            }
        });

        if (tclass == Datatype.CLASS_CHAR
                || (tclass == Datatype.CLASS_INTEGER && dataset
                        .getDatatype().getDatatypeSize() == 1)) {
            charCheckbox.setEnabled(false);
        }

        Label label = new Label(spreadsheetComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("TableView: ");

        choiceTableView = new Combo(spreadsheetComposite, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
        choiceTableView.setFont(curFont);
        choiceTableView.setItems(HDFView.getListOfTableViews().toArray(new String[0]));
        choiceTableView.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        choiceTableView.select(0);


        Composite imageComposite = new Composite(displayAsGroup, SWT.BORDER);
        imageComposite.setLayout(new GridLayout(4, false));
        imageComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        imageButton = new Button(imageComposite, SWT.RADIO);
        imageButton.setFont(curFont);
        imageButton.setText("&Image");
        imageButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        imageButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                spreadsheetButton.setSelection(!imageButton.getSelection());
                choicePalette.setEnabled(!isTrueColorImage);
                dataRangeField.setEnabled(true);
                fillValueField.setEnabled(true);
                choiceImageView.setEnabled(true);
                choiceTableView.setEnabled(false);
                charCheckbox.setSelection(false);
                charCheckbox.setEnabled(false);
            }
        });

        choicePalette = new Combo(imageComposite, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
        choicePalette.setFont(curFont);
        choicePalette.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        choicePalette.add("Select palette");

        if (dataset instanceof ScalarDS) {
            String paletteName = ((ScalarDS) dataset).getPaletteName(0);
            if (paletteName == null) {
                paletteName = "Default";
            }
            choicePalette.add(paletteName);
            for (int i = 2; i <= numberOfPalettes; i++) {
                paletteName = ((ScalarDS) dataset).getPaletteName(i - 1);
                choicePalette.add(paletteName);
            }
        }
        choicePalette.add("Gray");
        choicePalette.add("ReverseGray");
        choicePalette.add("GrayWave");
        choicePalette.add("Rainbow");
        choicePalette.add("Nature");
        choicePalette.add("Wave");

        choicePalette.select(0);

        label = new Label(imageComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Valid Range: ");

        dataRangeField = new Text(imageComposite, SWT.SINGLE | SWT.BORDER);
        dataRangeField.setFont(curFont);
        dataRangeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        String minmaxStr = "min, max";

        double minmax[] = ((ScalarDS) dataset).getImageDataRange();
        if (minmax != null) {
            if (dataset.getDatatype().getDatatypeClass() == Datatype.CLASS_FLOAT)
                minmaxStr = minmax[0] + "," + minmax[1];
            else
                minmaxStr = ((long) minmax[0]) + "," + ((long) minmax[1]);
        }

        dataRangeField.setText(minmaxStr);

        label = new Label(imageComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("ImageView: ");

        choiceImageView = new Combo(imageComposite, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
        choiceImageView.setFont(curFont);
        choiceImageView.setItems(HDFView.getListOfImageViews().toArray(new String[0]));
        choiceImageView.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        choiceImageView.select(0);

        label = new Label(imageComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Invalid Values: ");

        fillValueField = new Text(imageComposite, SWT.SINGLE | SWT.BORDER);
        fillValueField.setFont(curFont);
        fillValueField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        String fillStr = "val1, val2, ...";

        java.util.List<Number> fillValue = ((ScalarDS) dataset).getFilteredImageValues();
        int n = fillValue.size();
        if (n > 0) {
            fillStr = fillValue.get(0).toString();
            for (int i = 1; i < n; i++) {
                fillStr += ", " + fillValue.get(i);
            }
        }

        fillValueField.setText(fillStr);

        // Create Index Base region
        org.eclipse.swt.widgets.Group indexBaseGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        indexBaseGroup.setFont(curFont);
        indexBaseGroup.setText("Index Base");
        indexBaseGroup.setLayout(new GridLayout(2, true));
        indexBaseGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        base0Button = new Button(indexBaseGroup, SWT.RADIO);
        base0Button.setFont(curFont);
        base0Button.setText("0-based");
        base0Button.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));

        base1Button = new Button(indexBaseGroup, SWT.RADIO);
        base1Button.setFont(curFont);
        base1Button.setText("1-based");
        base1Button.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));

        if(ViewProperties.isIndexBase1()) {
            base0Button.setSelection(false);
            base1Button.setSelection(true);
        }
        else {
            base0Button.setSelection(true);
            base1Button.setSelection(false);
        }


        // Create Bitmask region
        org.eclipse.swt.widgets.Group bitmaskGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        bitmaskGroup.setFont(curFont);
        bitmaskGroup.setText("Bitmask");
        bitmaskGroup.setLayout(new GridLayout(2, false));
        bitmaskGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        extractBitButton = new Button(bitmaskGroup, SWT.CHECK);
        extractBitButton.setFont(curFont);
        extractBitButton.setText("Show &Value of Selected Bits");
        extractBitButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));
        extractBitButton.setSelection(false);
        extractBitButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = extractBitButton.getSelection();

                if(selected) {
                    if(applyBitmaskButton.getSelection()) {
                        applyBitmaskButton.setSelection(false);
                    }

                    if(!bitmaskButtons[0].getEnabled()) {
                        for(int i = 0; i < bitmaskButtons.length; i++) {
                            bitmaskButtons[i].setEnabled(true);
                        }
                    }

                    int n = 0;

                    if(bitmaskButtons[0].getSelection()) n = 1;

                    for(int i = 1; i < bitmaskButtons.length; i++) {
                        if(bitmaskButtons[i].getSelection() && !bitmaskButtons[i - 1].getSelection()) {
                            n++;
                        }
                    }

                    if(n > 1) {
                        extractBitButton.setSelection(false);
                        applyBitmaskButton.setSelection(true);

                        Tools.showError(shell,
                                "Selecting non-adjacent bits is only allowed \nfor the \"Apply Bitmask\" option.",
                                "Select Bitmask");
                        return;
                    }
                }
                else {
                    for(int i = 0; i < bitmaskButtons.length; i++) {
                        bitmaskButtons[i].setEnabled(false);
                    }
                }
            }
        });

        bitmaskHelp = new Button(bitmaskGroup, SWT.PUSH);
        bitmaskHelp.setImage(ViewProperties.getHelpIcon());
        bitmaskHelp.setToolTipText("Help on how to set bitmask");
        bitmaskHelp.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));
        bitmaskHelp.setEnabled(false);
        bitmaskHelp.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
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

                MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                info.setText(shell.getText());
                info.setMessage(msg);
                info.open();
            }
        });

        applyBitmaskButton = new Button(bitmaskGroup, SWT.CHECK);
        applyBitmaskButton.setFont(curFont);
        applyBitmaskButton.setText("&Apply Bitmask");
        applyBitmaskButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));
        applyBitmaskButton.setSelection(false);
        applyBitmaskButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if(extractBitButton.getSelection()) {
                    extractBitButton.setSelection(false);
                }
                else if (!applyBitmaskButton.getSelection()) {
                    for(int i = 0; i < bitmaskButtons.length; i++) {
                        bitmaskButtons[i].setEnabled(false);
                    }
                }
                else {
                    if(!bitmaskButtons[0].getEnabled()) {
                        for(int i = 0; i < bitmaskButtons.length; i++) {
                            bitmaskButtons[i].setEnabled(true);
                        }
                    }
                }
            }
        });

        new Label(bitmaskGroup, SWT.RIGHT).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Composite buttonComposite = new Composite(bitmaskGroup, SWT.NO_RADIO_GROUP);
        buttonComposite.setLayout(new GridLayout(16, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        int tsize = dataset.getDatatype().getDatatypeSize();
        bitmaskButtons = new Button[8 * tsize];

        for (int i = 0; i < bitmaskButtons.length; i++) {
            bitmaskButtons[i] = new Button(buttonComposite, SWT.RADIO);
            bitmaskButtons[i].setFont(curFont);
            bitmaskButtons[i].setText(String.valueOf(bitmaskButtons.length - i - 1));
            bitmaskButtons[i].setEnabled(false);
            bitmaskButtons[i].addSelectionListener(
                    new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            if(extractBitButton.getSelection()) {
                                Button source = (Button) e.widget;

                                // Don't allow non-adjacent selection of bits when extracting bits
                                int n = 0;

                                if(bitmaskButtons[0].getSelection()) n = 1;

                                for(int i = 1; i < bitmaskButtons.length; i++) {
                                    if(bitmaskButtons[i].getSelection() && !bitmaskButtons[i - 1].getSelection()) {
                                        n++;
                                    }
                                }

                                if(n > 1) {
                                    Tools.showError(shell,
                                            "Please select contiguous bits \nwhen the \"Show Value of Selected Bits\" option is checked.",
                                            "Select Bitmask");
                                    
                                    source.setSelection(false);
                                    return;
                                }
                            }
                        }
                    }
            );
        }

        if (tclass == Datatype.CLASS_CHAR
                || (tclass == Datatype.CLASS_INTEGER && tsize <= 8)) {
            extractBitButton.setEnabled(true);
            applyBitmaskButton.setEnabled(true);
            bitmaskHelp.setEnabled(true);
        }


        // Create Dimension/Subset selection region
        org.eclipse.swt.widgets.Group dimSubGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        dimSubGroup.setFont(curFont);
        dimSubGroup.setText("Dimension and Subset Selection");
        dimSubGroup.setLayout(new GridLayout(2, false));
        dimSubGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        int h = 1, w = 1;
        h = (int) dims[selectedIndex[0]];
        if (rank > 1) {
            w = (int) dims[selectedIndex[1]];
        }

        if (rank > 1) {
            navigator = new PreviewNavigator(dimSubGroup, SWT.DOUBLE_BUFFERED | SWT.BORDER, w, h);
        }
        else {
            dimSubGroup.setLayout(new GridLayout(1, true));
        }

        createDimSubSelectionComposite(dimSubGroup);
    }

    private void createCompoundDSContents() {
        Composite content = new Composite(shell, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        org.eclipse.swt.widgets.Group membersGroup = new org.eclipse.swt.widgets.Group(content, SWT.NONE);
        membersGroup.setFont(curFont);
        membersGroup.setText("Select Members");
        membersGroup.setLayout(new GridLayout(1, true));
        membersGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

        String[] names = ((CompoundDS) dataset).getMemberNames();
        fieldList = new List(membersGroup, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        fieldList.setFont(curFont);
        fieldList.setItems(names);
        fieldList.selectAll();

        GridData data = new GridData(SWT.FILL, SWT.FILL, false, true);
        data.heightHint = fieldList.getItemHeight() * 10;
        data.widthHint = fieldList.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + 10;
        fieldList.setLayoutData(data);

        org.eclipse.swt.widgets.Group dimSubGroup = new org.eclipse.swt.widgets.Group(content, SWT.NONE);
        dimSubGroup.setFont(curFont);
        dimSubGroup.setText("Dimension and Subset Selection");
        dimSubGroup.setLayout(new GridLayout());
        dimSubGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createDimSubSelectionComposite(dimSubGroup);

        Composite tableViewComposite = new Composite(dimSubGroup, SWT.BORDER);
        tableViewComposite.setLayout(new GridLayout(2, false));
        tableViewComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label label = new Label(tableViewComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("TableView: ");

        choiceTableView = new Combo(tableViewComposite, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
        choiceTableView.setFont(curFont);
        choiceTableView.setItems(HDFView.getListOfTableViews().toArray(new String[0]));
        choiceTableView.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        choiceTableView.select(0);
    }

    private void createTextContents() {
        org.eclipse.swt.widgets.Group dimSubGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        dimSubGroup.setFont(curFont);
        dimSubGroup.setText("Dimension and Subset Selection");
        dimSubGroup.setLayout(new GridLayout(1, true));
        dimSubGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createDimSubSelectionComposite(dimSubGroup);

        Composite textViewComposite = new Composite(dimSubGroup, SWT.BORDER);
        textViewComposite.setLayout(new GridLayout(2, false));
        textViewComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label label = new Label(textViewComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("TextView: ");

        choiceTextView = new Combo(textViewComposite, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
        choiceTextView.setFont(curFont);
        choiceTextView.setItems(HDFView.getListOfTextViews().toArray(new String[0]));
        choiceTextView.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        choiceTextView.select(0);
    }

    private void createDimSubSelectionComposite(Composite parent) {
        String[] dimNames = dataset.getDimNames();

        Composite selectionComposite = new Composite(parent, SWT.BORDER);
        selectionComposite.setLayout(new GridLayout(6, true));
        selectionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        new Label(selectionComposite, SWT.RIGHT).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        if (rank > 1) {
            transposeChoice = new Combo(selectionComposite, SWT.SINGLE | SWT.READ_ONLY);
            transposeChoice.setFont(curFont);
            transposeChoice.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));

            transposeChoice.add("Reshape");
            transposeChoice.add("Transpose");

            transposeChoice.select(0);
        }
        else {
            new Label(selectionComposite, SWT.RIGHT).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        }

        Label label = new Label(selectionComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Start: ");
        label.setLayoutData(new GridData(SWT.CENTER, SWT.END, true, true));

        label = new Label(selectionComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("End: ");
        label.setLayoutData(new GridData(SWT.CENTER, SWT.END, true, true));

        label = new Label(selectionComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Stride: ");
        label.setLayoutData(new GridData(SWT.CENTER, SWT.END, true, true));

        label = new Label(selectionComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Max Size");
        label.setLayoutData(new GridData(SWT.CENTER, SWT.END, true, true));


        // Create Height selection row
        label = new Label(selectionComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Height: ");
        label.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true));

        choices[0] = new Combo(selectionComposite, SWT.SINGLE | SWT.READ_ONLY);
        choices[0].setFont(curFont);
        choices[0].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        choices[0].addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Combo source = (Combo) e.widget;

                processComboBoxEvent(source);
            }
        });

        for (int j = 0; j < rank; j++) {
            if (dimNames == null) {
                choices[0].add("dim " + j);
            }
            else {
                choices[0].add(dimNames[j]);
            }
        }

        choices[0].select(0);

        startFields[0] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        startFields[0].setFont(curFont);
        startFields[0].setText("0");
        startFields[0].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        endFields[0] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        endFields[0].setFont(curFont);
        endFields[0].setText("0");
        endFields[0].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        strideFields[0] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        strideFields[0].setFont(curFont);
        strideFields[0].setText("1");
        strideFields[0].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        maxLabels[0] = new Label(selectionComposite, SWT.NONE);
        maxLabels[0].setFont(curFont);
        maxLabels[0].setText("1");
        maxLabels[0].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


        // Create Width selection row
        label = new Label(selectionComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Width: ");
        label.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true));

        choices[1] = new Combo(selectionComposite, SWT.SINGLE | SWT.READ_ONLY);
        choices[1].setFont(curFont);
        choices[1].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        choices[1].addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Combo source = (Combo) e.widget;

                processComboBoxEvent(source);
            }
        });

        for (int j = 0; j < rank; j++) {
            if (dimNames == null) {
                choices[1].add("dim " + j);
            }
            else {
                choices[1].add(dimNames[j]);
            }
        }

        choices[1].select(0);

        startFields[1] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        startFields[1].setFont(curFont);
        startFields[1].setText("0");
        startFields[1].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        endFields[1] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        endFields[1].setFont(curFont);
        endFields[1].setText("0");
        endFields[1].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        strideFields[1] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        strideFields[1].setFont(curFont);
        strideFields[1].setText("1");
        strideFields[1].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        maxLabels[1] = new Label(selectionComposite, SWT.NONE);
        maxLabels[1].setFont(curFont);
        maxLabels[1].setText("1");
        maxLabels[1].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


        // Create Depth selection row
        label = new Label(selectionComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Depth: ");
        label.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true));

        choices[2] = new Combo(selectionComposite, SWT.SINGLE | SWT.READ_ONLY);
        choices[2].setFont(curFont);
        choices[2].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        choices[2].addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Combo source = (Combo) e.widget;

                processComboBoxEvent(source);
            }
        });

        for (int j = 0; j < rank; j++) {
            if (dimNames == null) {
                choices[2].add("dim " + j);
            }
            else {
                choices[2].add(dimNames[j]);
            }
        }

        choices[2].select(0);

        startFields[2] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        startFields[2].setFont(curFont);
        startFields[2].setText("0");
        startFields[2].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        endFields[2] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        endFields[2].setFont(curFont);
        endFields[2].setText("0");
        endFields[2].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        strideFields[2] = new Text(selectionComposite, SWT.SINGLE | SWT.BORDER);
        strideFields[2].setFont(curFont);
        strideFields[2].setText("1");
        strideFields[2].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        maxLabels[2] = new Label(selectionComposite, SWT.NONE);
        maxLabels[2].setFont(curFont);
        maxLabels[2].setText("1");
        maxLabels[2].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


        // Create row for Dims and reset button
        new Label(selectionComposite, SWT.RIGHT).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Button dimsButton = new Button(selectionComposite, SWT.PUSH);
        dimsButton.setFont(curFont);
        dimsButton.setText("Dims...");
        dimsButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        dimsButton.setEnabled((rank > 3));
        dimsButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (rank < 4) {
                    return;
                }

                int idx = 0;
                Vector<Object> choice4 = new Vector<Object>(rank);
                int[] choice4Index = new int[rank - 3];
                for (int i = 0; i < rank; i++) {
                    if ((i != currentIndex[0]) && (i != currentIndex[1])
                            && (i != currentIndex[2])) {
                        choice4.add(choices[0].getItem(i));
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

                String result = new InputDialog(shell, shell.getText(), msg, initValue).open();
                if ((result == null) || ((result = result.trim()) == null)
                        || (result.length() < 1)) {
                    return;
                }

                StringTokenizer st = new StringTokenizer(result, "x");
                if (st.countTokens() < n) {
                    Tools.showError(shell, "Number of dimension(s) is less than " + n + "\n" + result, "Select Slice Location");
                    return;
                }

                long[] start4 = new long[n];
                for (int i = 0; i < n; i++) {
                    try {
                        start4[i] = Long.parseLong(st.nextToken().trim());
                    }
                    catch (Exception ex) {
                        Tools.showError(shell, ex.getMessage(), "Select Slice Location");
                        return;
                    }

                    if ((start4[i] < 0) || (start4[i] >= dims[choice4Index[i]])) {
                        Tools.showError(shell,
                                "Slice location is out of range.\n" + start4[i]
                                + " >= " + dims[choice4Index[i]],
                                "Select Slice Location");
                        
                        return;
                    }

                }

                for (int i = 0; i < n; i++) {
                    start[choice4Index[i]] = start4[i];
                }
            }
        });

        new Label(selectionComposite, SWT.RIGHT).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        new Label(selectionComposite, SWT.RIGHT).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Button resetButton = new Button(selectionComposite, SWT.PUSH);
        resetButton.setFont(curFont);
        resetButton.setText("Reset");
        resetButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        resetButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int n = startFields.length;

                for (int i = 0; i < n; i++) {
                    startFields[i].setText("0");
                    strideFields[i].setText("1");
                    long l = Long.valueOf(maxLabels[i].getText()) - 1;
                    endFields[i].setText(String.valueOf(l));
                }
            }
        });

        new Label(selectionComposite, SWT.RIGHT).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));


        for (int i = 0; i < 3; i++) {
            // Disable the selection components
            // init() will set them appropriately
            choices[i].setEnabled(false);
            startFields[i].setEnabled(false);
            endFields[i].setEnabled(false);
            strideFields[i].setEnabled(false);
            maxLabels[i].setEnabled(false);
        }
    }

    private void processComboBoxEvent(Combo source) {
        int theSelectedChoice = -1;

        int n = Math.min(3, rank);
        for (int i = 0; i < n; i++) {
            if (source.equals(choices[i])) {
                theSelectedChoice = i;
            }
        }

        if (theSelectedChoice < 0) {
            return; // the selected JComboBox is not a dimension choice
        }

        int theIndex = source.getSelectionIndex();
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
            else if (theIndex == choices[i].getSelectionIndex()) {
                choices[i].select(currentIndex[theSelectedChoice]);
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
            currentIndex[i] = choices[i].getSelectionIndex();
        }

        // update the navigator
        if (rank > 1) {
            if (isText) {
                endFields[1].setText(startFields[1].getText());
            }
            else {
                int hIdx = choices[0].getSelectionIndex();
                int wIdx = choices[1].getSelectionIndex();
                transposeChoice.select(0);

                // Use transpose option only if the dims are not in original
                // order
                if (hIdx < wIdx)
                    transposeChoice.setEnabled(false);
                else
                    transposeChoice.setEnabled(true);

                long dims[] = dataset.getDims();
                int w = (int) dims[wIdx];
                int h = (int) dims[hIdx];

                if(navigator != null) {
                    navigator.setDimensionSize(w, h);
                    navigator.redraw();
                }
            }
        }

        if (rank > 2) {
            endFields[2].setText(startFields[2].getText());
        }
    }

    /** PreviewNavigator draws a selection rectangle for selecting a subset
     *  of the data to be displayed. */
    private class PreviewNavigator extends Canvas {
        private final int         NAVIGATOR_SIZE   = 150;
        private int               dimX, dimY, x, y;
        private double            r;
        private Point             startPosition; // mouse clicked position
        private Rectangle         selectedArea;
        private Image             previewImage     = null;
        private String            selStr;

        private PreviewNavigator(Composite parent, int style, int w, int h) {
            super(parent, style);

            GridData data = new GridData(SWT.FILL, SWT.FILL, false, true);
            data.widthHint = NAVIGATOR_SIZE;
            data.heightHint = NAVIGATOR_SIZE;
            this.setLayoutData(data);

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

            selStr = "";

            try {
                previewImage = createPreviewImage();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            this.addMouseListener(new MouseListener() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {

                }

                @Override
                public void mouseDown(MouseEvent e) {
                    startPosition = new Point(e.x, e.y);
                    selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
                }

                @Override
                public void mouseUp(MouseEvent e) {
                    if(startPosition.x == e.x && startPosition.y == e.y) {
                        selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
                        redraw();
                    }
                }
            });

            this.addMouseMoveListener(new MouseMoveListener() {
                @Override
                public void mouseMove(MouseEvent e) {
                    if(e.stateMask == SWT.BUTTON1 || e.stateMask == SWT.BUTTON3) {
                        Point p0 = startPosition;
                        Point p1 = new Point(e.x, e.y);

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

                        redraw();
                    }
                }
            });

            this.addPaintListener(new PaintListener() {
                public void paintControl(PaintEvent e) {
                    GC gc = e.gc;

                    gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

                    if (previewImage != null) {
                        gc.drawImage(convertBufferedImageToSWTImage((BufferedImage) previewImage), 0, 0);
                    }
                    else {
                        gc.fillRectangle(0, 0, x, y);
                    }

                    int w = selectedArea.width;
                    int h = selectedArea.height;
                    if ((w > 0) && (h > 0)) {
                        gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
                        gc.drawRectangle(selectedArea.x, selectedArea.y, w, h);
                    }

                    org.eclipse.swt.graphics.Rectangle bounds = ((Canvas) e.widget).getBounds();
                    Point textSize = gc.stringExtent(selStr);
                    gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                    gc.drawString(selStr, (bounds.width / 2) - (textSize.x / 2), bounds.height - textSize.y - 4);
                }
            });
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
                try {
                    selectedIndex[0] = choices[0].getSelectionIndex();
                    selectedIndex[1] = choices[1].getSelectionIndex();
                } catch (Exception ex) {
                }
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

            // update the ratio of preview image size to the real dataset
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

        private void updateSelection (int x0, int y0, int w, int h) {
            int i0 = 0, i1 = 0;

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

            selectedArea.setSize(0, 0);

            try {
                previewImage = createPreviewImage();
            }
            catch (Exception ex) {
            }

            this.redraw();
        }

        /**
         * Converts a given BufferedImage to ImageData for a SWT-readable Image
         *
         * @param image The BufferedImage to be converted
         *
         * @return the Image
         */
        private org.eclipse.swt.graphics.Image convertBufferedImageToSWTImage(BufferedImage image) {
            Display display = this.getDisplay();

            if (image.getColorModel() instanceof DirectColorModel) {
                DirectColorModel colorModel = (DirectColorModel) image
                        .getColorModel();
                PaletteData palette = new PaletteData(colorModel.getRedMask(),
                        colorModel.getGreenMask(), colorModel.getBlueMask());
                ImageData data = new ImageData(image.getWidth(),
                        image.getHeight(), colorModel.getPixelSize(),
                        palette);

                for (int y = 0; y < data.height; y++) {
                    for (int x = 0; x < data.width; x++) {
                        int rgb = image.getRGB(x, y);
                        int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                        data.setPixel(x, y, pixel);
                        if (colorModel.hasAlpha()) {
                            data.setAlpha(x, y, (rgb >> 24) & 0xFF);
                        }
                    }
                }

                return new org.eclipse.swt.graphics.Image(display, data);
            }
            else if (image.getColorModel() instanceof IndexColorModel) {
                IndexColorModel colorModel = (IndexColorModel) image
                        .getColorModel();
                int size = colorModel.getMapSize();
                byte[] reds = new byte[size];
                byte[] greens = new byte[size];
                byte[] blues = new byte[size];
                colorModel.getReds(reds);
                colorModel.getGreens(greens);
                colorModel.getBlues(blues);
                RGB[] rgbs = new RGB[size];
                for (int i = 0; i < rgbs.length; i++) {
                    rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
                            blues[i] & 0xFF);
                }
                PaletteData palette = new PaletteData(rgbs);
                ImageData data = new ImageData(image.getWidth(),
                        image.getHeight(), colorModel.getPixelSize(),
                        palette);
                data.transparentPixel = colorModel.getTransparentPixel();
                WritableRaster raster = image.getRaster();
                int[] pixelArray = new int[1];
                for (int y = 0; y < data.height; y++) {
                    for (int x = 0; x < data.width; x++) {
                        raster.getPixel(x, y, pixelArray);
                        data.setPixel(x, y, pixelArray[0]);
                    }
                }

                return new org.eclipse.swt.graphics.Image(display, data);
            }
            else if (image.getColorModel() instanceof ComponentColorModel) {
                ComponentColorModel colorModel = (ComponentColorModel) image.getColorModel();
                //ASSUMES: 3 BYTE BGR IMAGE TYPE
                PaletteData palette = new PaletteData(0x0000FF, 0x00FF00,0xFF0000);
                ImageData data = new ImageData(image.getWidth(), image.getHeight(),
                        colorModel.getPixelSize(), palette);
                //This is valid because we are using a 3-byte Data model with no transparent pixels
                data.transparentPixel = -1;
                WritableRaster raster = image.getRaster();
                int[] pixelArray = new int[3];
                for (int y = 0; y < data.height; y++) {
                    for (int x = 0; x < data.width; x++) {
                        raster.getPixel(x, y, pixelArray);
                        int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
                        data.setPixel(x, y, pixel);
                    }
                }
                return new org.eclipse.swt.graphics.Image(display, data);
            }

            return null;
        }
    }
}
