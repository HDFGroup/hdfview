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
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * Displays a dialog for viewing and change palettes.
 * 
 * @author Jordan T. Henderson
 * @version 2.4 2/27/16
 */
public class DefaultPaletteView extends Dialog {
	
	private Shell shell;
	
	private ScalarDS dataset;
	
	/** Panel that draws plot of data values. */
    private ChartCanvas chartP;
    private ImageView imageView;
    private PaletteValueTable paletteValueTable;
	
	private Button checkRed, checkGreen, checkBlue;
	
	private Combo choicePalette;
	
    private Image originalImage, currentImage;
	
	private final Color[] lineColors = { Color.red, Color.green, Color.blue };
    private final String lineLabels[] = { "Red", "Green", "Blue" };
	
	
	private static String PALETTE_GRAY = "Gray";
    private static String PALETTE_DEFAULT = "Default";
    private static String PALETTE_REVERSE_GRAY = "Reverse Gray";
    private static String PALETTE_GRAY_WAVE = "GrayWave";
    private static String PALETTE_RAINBOW = "Rainbow";
    private static String PALETTE_NATURE = "Nature";
    private static String PALETTE_WAVE = "Wave";
    
    private int x0, y0; // starting point of mouse drag
    
    byte[][] palette;
    private int numberOfPalettes;
    private int[][] paletteData;
    
    boolean isPaletteChanged = false;
    private boolean startEditing = false;
    private boolean isH5 = false;
    
	
	public DefaultPaletteView(Shell parent, ImageView theImageView) {
        this(parent, null, theImageView);
    }
	
	public DefaultPaletteView(Shell parent, ViewManager theViewer, ImageView theImageView) {
		super(parent, SWT.APPLICATION_MODAL);
		
		imageView = theImageView;
        dataset = (ScalarDS) imageView.getDataObject();
        
        numberOfPalettes = 1;
        
        isH5 = dataset.getFileFormat().isThisType(
                FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
	}
	
	public void open() {
		Shell parent = getParent();
    	shell = new Shell(parent, SWT.TITLE | SWT.CLOSE |
    			SWT.BORDER | SWT.APPLICATION_MODAL);
    	shell.setText("Image Palette for - " + dataset.getPath() + dataset.getName());
    	shell.setImage(ViewProperties.getHdfIcon());
    	shell.setLayout(new GridLayout(1, true));
    	
    	
    	// Create the content composite for the Canvas
    	Composite content = new Composite(shell, SWT.BORDER);
    	content.setLayout(new FillLayout());
    	content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	
    	
    	
    	// Create the toolbar composite
    	Composite tools = new Composite(shell, SWT.NONE);
    	tools.setLayout(new RowLayout(SWT.HORIZONTAL));
    	tools.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	// Add buttons for changing line colors
    	Composite rgbComposite = new Composite(tools, SWT.BORDER);
    	rgbComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
    	rgbComposite.setLayoutData(new RowData());
    	
    	checkRed = new Button(rgbComposite, SWT.RADIO);
    	checkRed.setText("Red");
    	checkRed.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
    	
    	checkGreen = new Button(rgbComposite, SWT.RADIO);
    	checkGreen.setText("Green");
    	checkGreen.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
    	
    	checkBlue = new Button(rgbComposite, SWT.RADIO);
    	checkBlue.setText("Blue");
    	checkBlue.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
    	
    	checkRed.setSelection(true);
    	checkGreen.setSelection(false);
    	checkBlue.setSelection(false);
    	
    	// Add controls for selecting palettes and showing values
    	Composite paletteComposite = new Composite(tools, SWT.BORDER);
    	paletteComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
    	paletteComposite.setLayoutData(new RowData());
    	
    	choicePalette = new Combo(paletteComposite, SWT.SINGLE);
    	choicePalette.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			int idx = choicePalette.getSelectionIndex();
    	        if (idx <= 0) {
    	            return;
    	        }

    	        byte[][] imagePalette = null;
    	        Object item = choicePalette.getItem(idx);

    	        if (item.equals(PALETTE_DEFAULT)) {
    	            imagePalette = dataset.getPalette();
    	        }
    	        else if (item.equals(PALETTE_GRAY)) {
    	            imagePalette = Tools.createGrayPalette();
    	        }
    	        else if (item.equals(PALETTE_REVERSE_GRAY)) {
    	            imagePalette = Tools.createReverseGrayPalette();
    	        }
    	        else if (item.equals(PALETTE_GRAY_WAVE)) {
    	            imagePalette = Tools.createGrayWavePalette();
    	        }
    	        else if (item.equals(PALETTE_RAINBOW)) {
    	            imagePalette = Tools.createRainbowPalette();
    	        }
    	        else if (item.equals(PALETTE_NATURE)) {
    	            imagePalette = Tools.createNaturePalette();
    	        }
    	        else if (item.equals(PALETTE_WAVE)) {
    	            imagePalette = Tools.createWavePalette();
    	        }
    	        else if (idx > 0 && idx <= numberOfPalettes) {
    	            imagePalette = ((ScalarDS) dataset).readPalette(idx - 1);
    	        }
    	        else {
    	            imagePalette = Tools.readPalette((String)item);
    	        }

    	        if (imagePalette == null) {
    	            return;
    	        }

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

    	        //chartP.repaint();
    	        isPaletteChanged = true;
    		}
    	});
    	
    	choicePalette.add("Select palette");
    	
        String paletteName = ((ScalarDS) dataset).getPaletteName(0);
        
        if (paletteName!= null)
            paletteName = paletteName.trim();
        
        if (paletteName!= null && paletteName.length()>0)
            choicePalette.add(paletteName);
        
        if (isH5 && (dataset instanceof ScalarDS)) {
            byte[] palRefs = ((ScalarDS) dataset).getPaletteRefs();
            if ((palRefs != null) && (palRefs.length > 8)) {
              numberOfPalettes = palRefs.length / 8;
            }
        }
        for (int i = 1; i < numberOfPalettes; i++) {
            paletteName = ((ScalarDS) dataset).getPaletteName(i);
            choicePalette.add(paletteName);
        }
        choicePalette.add(PALETTE_GRAY);
        choicePalette.add(PALETTE_GRAY_WAVE);
        choicePalette.add(PALETTE_RAINBOW);
        choicePalette.add(PALETTE_NATURE);
        choicePalette.add(PALETTE_WAVE);
        Vector<?> plist = ViewProperties.getPaletteList();
        int n = plist.size();
        for (int i = 0; i < n; i++)
            choicePalette.add((String) plist.get(i));
        
        choicePalette.select(0);
        
        
        Button showValueButton = new Button(paletteComposite, SWT.PUSH);
        showValueButton.setText("Show Values");
        showValueButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		if (paletteValueTable == null) {
                    paletteValueTable = new PaletteValueTable(shell, SWT.NONE);
                }
                //paletteValueTable.refresh();
                //paletteValueTable.setVisible(true);
        	}
        });
        
    	
    	// Add Ok/Cancel/Preview buttons
    	Composite buttonComposite = new Composite(tools, SWT.BORDER);
    	buttonComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
    	buttonComposite.setLayoutData(new RowData());
    	
    	Button okButton = new Button(buttonComposite, SWT.PUSH);
    	okButton.setText("   &Ok   ");
        okButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		if (isPaletteChanged) {
                    //updatePalette();
                    isPaletteChanged = false;
                    imageView.setPalette(palette);
                    imageView.setImage(currentImage);
                }
                
        		shell.dispose();
        	}
        });
        
        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("&Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		imageView.setImage(originalImage);
                shell.dispose();
        	}
        });
        
        Button previewButton = new Button(buttonComposite, SWT.PUSH);
        previewButton.setText("&Preview");
        previewButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		//updatePalette();
                imageView.setImage(currentImage);
        	}
        });
    	

		shell.pack();
		
		int w = 700 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 500 + (ViewProperties.getFontSize() - 12) * 10;

		//Point computedSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		shell.setMinimumSize(w, h);

		Rectangle parentBounds = parent.getBounds();
		Point shellSize = shell.getSize();
		shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
				(parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

		shell.open();

		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
	
    /** Returns the data object displayed in this data viewer */
    public HObject getDataObject() {
        return dataset;
    }
    
    private void updatePalette() {
        for (int i = 0; i < 256; i++) {
            palette[0][i] = (byte) paletteData[0][i];
            palette[1][i] = (byte) paletteData[1][i];
            palette[2][i] = (byte) paletteData[2][i];
        }

        IndexColorModel colorModel = new IndexColorModel(8, // bits - the number
                                                            // of bits each
                                                            // pixel occupies
                256, // size - the size of the color component arrays
                palette[0], // r - the array of red color components
                palette[1], // g - the array of green color components
                palette[2]); // b - the array of blue color components

        int w = dataset.getWidth();
        int h = dataset.getHeight();
        MemoryImageSource memoryImageSource = null;

        try {
            //memoryImageSource = (MemoryImageSource) originalImage.getSource();
        }
        catch (Throwable err) {
            memoryImageSource = null;
        }

        if (memoryImageSource == null) {
            memoryImageSource = new MemoryImageSource(w, h, colorModel,
                    imageView.getImageByteData(), 0, w);
        }
        else {
            memoryImageSource.newPixels(imageView.getImageByteData(),
                    colorModel, 0, w);
        }

        //currentImage = Toolkit.getDefaultToolkit().createImage(
        //        memoryImageSource);
    }
    
    private class ChartCanvas extends Canvas {
    	public ChartCanvas(Composite parent, int style) {
    		super(parent, style);
    	}
    }
    
    /** The dialog to show the palette values in spreadsheet. */
    private class PaletteValueTable extends Dialog {
    	public PaletteValueTable(Shell parent, int style) {
    		super(parent, style);
    	}
    }
}
