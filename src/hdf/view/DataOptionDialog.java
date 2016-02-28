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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.BitSet;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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
 * @version 2.4 1/28/2016
 */
public class DataOptionDialog extends Dialog {
    
	private Shell shell;

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

    private Label                  maxLabels[], selLabel;

    private Text                   startFields[], endFields[], strideFields[], dataRangeField, fillValueField;

    //private JList                  fieldList;

    private PreviewNavigator navigator;

    private int                    numberOfPalettes;
	
    /** the selected dataset/image */
	private Dataset dataset;
	
	
	/**
     * Constructs a DataOptionDialog with the given HDFView.
     */
	public DataOptionDialog(Shell parent, Dataset theDataset) {
		super(parent, SWT.APPLICATION_MODAL);
		
		if(theDataset == null) return;
		
		dataset = theDataset;
		
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
        //fieldList = null;

        int h = 1, w = 1;
        h = (int) dims[selectedIndex[0]];
        if (rank > 1) {
            w = (int) dims[selectedIndex[1]];
        }
    	
        navigator = new PreviewNavigator(shell, SWT.DOUBLE_BUFFERED, w, h);
		
		isH5 = dataset.getFileFormat().isThisType(
                FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
	}
	
	public void open() {
		Shell parent = getParent();
    	shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
    	shell.setText("Dataset Selection - " + dataset.getPath() + dataset.getName());
    	shell.setImage(ViewProperties.getHdfIcon());
    	GridLayout layout = new GridLayout(1, true);
    	//layout.verticalSpacing = 0;
    	shell.setLayout(layout);
    	
    	
    	// Create display type region
    	org.eclipse.swt.widgets.Group displayAsGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
		displayAsGroup.setText("Display As");
		displayAsGroup.setLayout(new GridLayout(1, true));
		displayAsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite spreadsheetComposite = new Composite(displayAsGroup, SWT.NONE);
		spreadsheetComposite.setLayout(new GridLayout(2, false));
		spreadsheetComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite imageComposite = new Composite(displayAsGroup, SWT.NONE);
		imageComposite.setLayout(new GridLayout(3, false));
		imageComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		
		
		// Create Index Base region
		org.eclipse.swt.widgets.Group indexBaseGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
		indexBaseGroup.setText("Index Base");
		indexBaseGroup.setLayout(new GridLayout(2, true));
		indexBaseGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		base0Button = new Button(indexBaseGroup, SWT.RADIO);
		base0Button.setText("0-based");
		
		base1Button = new Button(indexBaseGroup, SWT.RADIO);
		base1Button.setText("1-based");
		
		if(ViewProperties.isIndexBase1())
            base1Button.setSelection(true);
        else
            base0Button.setSelection(true);
		
		
		
		// Create Bitmask region
		org.eclipse.swt.widgets.Group bitmaskGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
		bitmaskGroup.setText("Bitmask");
		bitmaskGroup.setLayout(new GridLayout(2, false));
		bitmaskGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		
		
		// Create Dimension/Subset selection region
		org.eclipse.swt.widgets.Group dimSubGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
		dimSubGroup.setText("Dimension and Subset Selection");
		dimSubGroup.setLayout(new GridLayout(5, true));
		dimSubGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		
		
		// Create Ok/Cancel button region
		Composite buttonComposite = new Composite(shell, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, true));
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setText("   &Ok   ");
        okButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		// set palette for image view
                if ((dataset instanceof ScalarDS) && imageButton.getSelection()) {
                    setPalette();
                }

                isSelectionCancelled = !setSelection();

                if (isSelectionCancelled) {
                    return;
                }

                if (dataset instanceof ScalarDS) {
                    ((ScalarDS) dataset).setIsImageDisplay(imageButton.getSelection());
                }
                
                shell.dispose();
        	}
        });
        GridData gridData = new GridData(SWT.END, SWT.FILL, true, false);
        gridData.widthHint = 70;
        okButton.setLayoutData(gridData);
        
        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("&Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
                shell.dispose();
        	}
        });
        
        gridData = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
        gridData.widthHint = 70;
        cancelButton.setLayoutData(gridData);
		
        shell.pack();
        
        //init();
        
        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        
        Rectangle parentBounds = parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));
        
        shell.open();
        
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
        else if (dataset instanceof CompoundDS) {
            imageButton.setEnabled(false);
        }

        choiceTableView.setEnabled(!isImage);
        choiceImageView.setEnabled(isImage);
        imageButton.setSelection(isImage);
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

            //setJComboBoxSelectedIndex(choices[i], idx);
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
            if (isTrueColorImage && imageButton.getSelection()) {
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
            currentIndex[i] = choices[i].getSelectionIndex();
        }

        // reset show char button
        Datatype dtype = dataset.getDatatype();
        int tclass = dtype.getDatatypeClass();
        if (tclass == Datatype.CLASS_CHAR || tclass == Datatype.CLASS_INTEGER) {
            int tsize = dtype.getDatatypeSize();
            charCheckbox.setEnabled((tsize == 1) && spreadsheetButton.getSelection());
            extractBitButton.setEnabled(tsize <= 8);
            applyBitmaskButton.setEnabled(tsize <= 8);
        }
        else {
            charCheckbox.setEnabled(false);
            charCheckbox.setSelection(false);
            extractBitButton.setEnabled(false);
            applyBitmaskButton.setEnabled(false);
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
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                error.setText(shell.getText());
                error.setMessage(ex.getMessage());
                error.open();
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
            /*
             * TODO: switch back
             */
            int[] selectedFieldIndices = {0, 0};//fieldList.getSelectedIndices();
            if ((selectedFieldIndices == null)
                    || (selectedFieldIndices.length < 1)) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                error.setText(shell.getText());
                error.setMessage("No member/field is selected.");
                error.open();
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
            bitmask.set(i, bitmaskButtons[i].getSelection());
        }

        return true;
    }
	
	
	
	

    /** Returns true if the display option is image. */
    public boolean isImageDisplay() {
        return imageButton.getSelection();
    }
    
    public boolean isIndexBase1() {
        if (base1Button == null)
            return false;

        return base1Button.getSelection();
    }
	
	

    /**
     * Return the bitmask.
     */
    public BitSet getBitmask() {
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
    
    /** Return the name of the selected dataview */
    public String getDataViewName() {
        String viewName = null;

        if (isText) {
            viewName = choiceTextView.getItem(choiceTextView.getSelectionIndex());
        }
        else if (isImageDisplay()) {
            viewName = choiceImageView.getItem(choiceImageView.getSelectionIndex());
        }
        else {
            viewName = choiceTableView.getItem(choiceTableView.getSelectionIndex());
        }

        return viewName;
    }
    
    /**
     * 
     * @return true if display the data as characters; otherwise, display as numbers.
     */
    public boolean isDisplayTypeChar ( ) {
        return charCheckbox.getSelection();
    }

    /**
     * Check if it only apply bitmask.
     */
    public boolean isApplyBitmaskOnly ( ) {
        if (getBitmask() == null)
            return false;

        return applyBitmaskButton.getSelection();
    }

    /**
     * 
     * @return true if transpose the data in 2D table; otherwise, do not transpose the data.
     */
    public boolean isTransposed ( ) {
        return (transposeChoice.getSelectionIndex() == 1);
    }
    
    /** Returns true if the data selection is cancelled. */
    public boolean isCancelled ( ) {
        return isSelectionCancelled;
    }
	
    /** PreviewNavigator draws a selection rectangle for selecting a subset
     *  of the data to be displayed. */
    private class PreviewNavigator extends Canvas {
        private static final long serialVersionUID = -4458114008420664965L;
        private final int         NAVIGATOR_SIZE   = 150;
        private int               dimX, dimY, x, y;
        private double            r;
        private Point             startPosition;                           // mouse
                                                                            // clicked
                                                                            // position
        private Rectangle         selectedArea;
        private Image             previewImage     = null;

        private PreviewNavigator(Composite parent, int style, int w, int h) {
        	super(parent, style);
        	
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

            //selectedArea = new Rectangle();
            //setPreferredSize(new Dimension(NAVIGATOR_SIZE, NAVIGATOR_SIZE));
            try {
                previewImage = createPreviewImage();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            //addMouseListener(this);
            //addMouseMotionListener(this);
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
                selectedIndex[0] = choices[0].getSelectionIndex();
                selectedIndex[1] = choices[1].getSelectionIndex();
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

        //@Override
        /*public void paint(Graphics g) {
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
        }*/

        //@Override
        /*public void mousePressed (MouseEvent e) {
            startPosition = e.getPoint();
            selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
        }*/

        //@Override
        /*public void mouseClicked (MouseEvent e) {
            startPosition = e.getPoint();
            selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
            repaint();
        }*/

        //@Override
        /*public void mouseDragged (MouseEvent e) {
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
        }*/

        /*private void updateSelection (int x0, int y0, int w, int h) {
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
        }*/

        /*private void setDimensionSize (int w, int h) {
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
        }*/
    }
}
