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

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

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

    private Shell               shell;
    
    private Font                curFont;

    private ScalarDS            dataset;

    /** Panel that draws plot of data values. */
    private ChartCanvas         chartP;
    private ImageView           imageView;
    private PaletteValueTable   paletteValueTable;

    private Button              checkRed, checkGreen, checkBlue;

    private Combo               choicePalette;

    private Image               originalImage, currentImage;

    private final int[]         lineColors = { SWT.COLOR_RED, SWT.COLOR_GREEN, SWT.COLOR_BLUE };
    private final String        lineLabels[] = { "Red", "Green", "Blue" };

    private static String       PALETTE_GRAY = "Gray";
    private static String       PALETTE_DEFAULT = "Default";
    private static String       PALETTE_REVERSE_GRAY = "Reverse Gray";
    private static String       PALETTE_GRAY_WAVE = "GrayWave";
    private static String       PALETTE_RAINBOW = "Rainbow";
    private static String       PALETTE_NATURE = "Nature";
    private static String       PALETTE_WAVE = "Wave";

    private int                 x0, y0; // starting point of mouse drag

    byte[][]                    palette;
    private int                 numberOfPalettes;
    private int[][]             paletteData;

    boolean                     isPaletteChanged = false;
    private boolean             isH5 = false;


    public DefaultPaletteView(Shell parent, ImageView theImageView) {
        this(parent, null, theImageView);
    }

    public DefaultPaletteView(Shell parent, ViewManager theViewer, ImageView theImageView) {
        super(parent, SWT.APPLICATION_MODAL);
        
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

        imageView = theImageView;
        dataset = (ScalarDS) imageView.getDataObject();

        numberOfPalettes = 1;

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

        x0 = y0 = 0;
        originalImage = currentImage = imageView.getImage();
        palette = new byte[3][256];

        isH5 = dataset.getFileFormat().isThisType(
                FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        
        createUI();
    }

    public void createUI() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("Image Palette for - " + dataset.getPath() + dataset.getName());
        shell.setImage(ViewProperties.getHdfIcon());
        shell.setLayout(new GridLayout(1, true));

        shell.setData(this);

        // Create the content composite for the Canvas
        Composite content = new Composite(shell, SWT.BORDER);
        content.setLayout(new GridLayout(1, true));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        chartP = new ChartCanvas(content, SWT.DOUBLE_BUFFERED);

        // Create the toolbar composite
        Composite tools = new Composite(shell, SWT.NONE);
        tools.setLayout(new GridLayout(3, false));
        tools.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // Add buttons for changing line colors
        Composite rgbComposite = new Composite(tools, SWT.BORDER);
        rgbComposite.setLayout(new GridLayout(3, true));
        rgbComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        checkRed = new Button(rgbComposite, SWT.RADIO);
        checkRed.setFont(curFont);
        checkRed.setText("Red");
        checkRed.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        checkRed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        
        checkGreen = new Button(rgbComposite, SWT.RADIO);
        checkGreen.setFont(curFont);
        checkGreen.setText("Green");
        checkGreen.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
        checkGreen.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        checkBlue = new Button(rgbComposite, SWT.RADIO);
        checkBlue.setFont(curFont);
        checkBlue.setText("Blue");
        checkBlue.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
        checkBlue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        checkRed.setSelection(true);
        checkGreen.setSelection(false);
        checkBlue.setSelection(false);

        // Add controls for selecting palettes and showing values
        Composite paletteComposite = new Composite(tools, SWT.BORDER);
        paletteComposite.setLayout(new GridLayout(2, false));
        paletteComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        choicePalette = new Combo(paletteComposite, SWT.SINGLE | SWT.READ_ONLY);
        choicePalette.setFont(curFont);
        choicePalette.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
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

                chartP.refresh();
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
        showValueButton.setFont(curFont);
        showValueButton.setText("Show Values");
        showValueButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        showValueButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (paletteValueTable == null) {
                    paletteValueTable = new PaletteValueTable(shell, SWT.NONE);
                }
                
                paletteValueTable.open();
            }
        });

        // Add Ok/Cancel/Preview buttons
        Composite buttonComposite = new Composite(tools, SWT.BORDER);
        buttonComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("  &Ok  ");
        okButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (isPaletteChanged) {
                    updatePalette();
                    isPaletteChanged = false;
                    imageView.setPalette(palette);
                    imageView.setImage(currentImage);
                }

                shell.dispose();
            }
        });

        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setFont(curFont);
        cancelButton.setText("&Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                imageView.setImage(originalImage);
                shell.dispose();
            }
        });

        Button previewButton = new Button(buttonComposite, SWT.PUSH);
        previewButton.setFont(curFont);
        previewButton.setText("&Preview");
        previewButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updatePalette();
                imageView.setImage(currentImage);
            }
        });

        shell.pack();
        
        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null) curFont.dispose();
            }
        });

        shell.setSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

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

    /** @return the data object displayed in this data viewer */
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

        long w = dataset.getWidth();
        long h = dataset.getHeight();
        MemoryImageSource memoryImageSource = null;

        try {
            memoryImageSource = (MemoryImageSource) originalImage.getSource();
        }
        catch (Throwable err) {
            memoryImageSource = null;
        }

        if (memoryImageSource == null) {
            memoryImageSource = new MemoryImageSource((int)w, (int)h, colorModel,
                    imageView.getImageByteData(), 0, (int)w);
        }
        else {
            memoryImageSource.newPixels(imageView.getImageByteData(),
                    colorModel, 0, (int)w);
        }

        currentImage = Tools.toBufferedImage(Toolkit.getDefaultToolkit().createImage(memoryImageSource));
    }

    /** The canvas that paints the data lines. */
    private class ChartCanvas {
        private Canvas canvas;

        public ChartCanvas(Composite parent, int style) {
            canvas = new Canvas(parent, style);
            
            GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
            data.widthHint = 700 + (ViewProperties.getFontSize() - 12) * 15;
            data.heightHint = 500 + (ViewProperties.getFontSize() - 12) * 10;
            canvas.setLayoutData(data);
            
            canvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

            canvas.addPaintListener(new PaintListener() {
                public void paintControl(PaintEvent e) {
                    // Get the graphics context for this paint event
                    GC g = e.gc;

                    Point p = canvas.getSize();
                    int gap = 20;
                    int legendSpace = 60;
                    int h = p.y - gap;
                    int w = p.x - 3 * gap - legendSpace;

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

                    org.eclipse.swt.graphics.Color c = g.getForeground();
                    for (int i = 0; i < 3; i++) {
                        g.setForeground(Display.getCurrent().getSystemColor(lineColors[i]));

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

                    g.setForeground(c); // set the color back to its default

                    // draw a box on the legend
                    g.drawRectangle(w + legendSpace - 10, 10, legendSpace, 10 * gap);
                }
            });

            canvas.addMouseListener(new MouseAdapter() {
                public void mouseUp(MouseEvent e) {
                    if ((paletteValueTable != null)) {
                        paletteValueTable.refresh();
                    }
                }
            });

        }

        public void refresh() {
            canvas.redraw();
        }
    }

    /** The dialog to show the palette values in spreadsheet. */
    private class PaletteValueTable extends Dialog {
    	
    	private Display display;
    	private Shell tableShell;

    	Table valueTable;

        String rgbName = "Color";
        String idxName = "Index";

        public PaletteValueTable(Shell parent, int style) {
            super(parent, style);
        }

        public void open() {
            Shell parent = getParent();
            display = parent.getDisplay();
            
            tableShell = new Shell(parent, SWT.SHELL_TRIM);
            tableShell.setFont(curFont);
            tableShell.setText("");
            tableShell.setImage(ViewProperties.getHdfIcon());
            tableShell.setLayout(new GridLayout(1, true));

            Composite content = new Composite(tableShell, SWT.NONE);
            content.setLayout(new GridLayout(1, true));
            content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            
            GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
            data.heightHint = 200;
            content.setLayoutData(data);

            String[] columnNames = { idxName, "Red", "Green", "Blue", rgbName };

            valueTable = new Table(content, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.NO_SCROLL);
            valueTable.setHeaderVisible(true);
            valueTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            valueTable.setFont(curFont);

            // Add cell editor for changing cell values in-place
            valueTable.addListener(SWT.MouseDoubleClick, valueTableCellEditor);
            
            valueTable.addListener(SWT.Resize, new Listener() {
            	public void handleEvent(Event e) {
            		int numColumns = valueTable.getColumnCount();
            		
            		for (int i = 0; i < numColumns; i++) {
            			valueTable.getColumn(i).setWidth(valueTable.getClientArea().width / numColumns);
            		}
            	}
            });
            
            valueTable.addPaintListener(new PaintListener() {
            	public void paintControl(PaintEvent e) {
            		for (int i = 0; i < valueTable.getItemCount(); i++) {
            			Color cellColor = new Color(display, paletteData[0][i],
                                paletteData[1][i], paletteData[2][i]);
            			
            			valueTable.getItem(i).setBackground(4, cellColor);
            			
            			cellColor.dispose();
            		}
            	}
            });
            
            for (int i = 0; i < columnNames.length; i++) {
            	TableColumn column = new TableColumn(valueTable, SWT.NONE);
            	column.setText(columnNames[i]);
            	column.setMoveable(false);
            	column.pack();
            }
            
            for (int i = 0; i < 256; i++) {
            	TableItem item = new TableItem(valueTable, SWT.NONE);
            	item.setFont(curFont);
            	
            	item.setText(new String[] {String.valueOf(i), String.valueOf(paletteData[0][i]),
            			String.valueOf(paletteData[1][i]), String.valueOf(paletteData[2][i]),
            			null});
            	
            	item.setBackground(0, Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            }

            // set cell height for large fonts
            //int cellRowHeight = Math.max(16, valueTable.getFontMetrics(
            //        valueTable.getFont()).getHeight());
            //valueTable.setRowHeight(cellRowHeight);

            Button okButton = new Button(tableShell, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &Ok   ");
            okButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    tableShell.dispose();
                }
            });

            tableShell.pack();
            
            int w = 300 + (ViewProperties.getFontSize() - 12) * 10;
            int h = 600 + (ViewProperties.getFontSize() - 12) * 15;

            tableShell.setSize(w, h);

            Rectangle parentBounds = parent.getBounds();
            Point shellSize = tableShell.getSize();
            tableShell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                              (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            tableShell.open();

            Display display = parent.getDisplay();
            while(!tableShell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
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
                Tools.showError(tableShell, "Value is out of range [0, 255]\n", tableShell.getText());
            	return;
            }

            paletteData[col][row] = value;
            chartP.refresh();
            isPaletteChanged = true;
        }

        public void refresh() {
            valueTable.redraw();
        }

        private Listener valueTableCellEditor = new Listener() {
            public void handleEvent(Event event) {
                final TableEditor editor = new TableEditor(valueTable);
                editor.horizontalAlignment = SWT.LEFT;
                editor.grabHorizontal = true;

                Rectangle clientArea = valueTable.getClientArea();
                Point pt = new Point(event.x, event.y);
                int index = valueTable.getTopIndex();

                while (index < valueTable.getItemCount()) {
                    boolean visible = false;
                    final TableItem item = valueTable.getItem(index);

                    // Only allow editing of RGB values
                    for (int i = 1; i < valueTable.getColumnCount() - 1; i++) {
                        Rectangle rect = item.getBounds(i);

                        if (rect.contains(pt)) {
                            final int column = i;
                            final int row = index;
                            
                            final Text text = new Text(valueTable, SWT.NONE);
                            text.setFont(curFont);

                            Listener textListener = new Listener() {
                                public void handleEvent(final Event e) {
                                	int newValue = Integer.parseInt(text.getText());
                                	
                                    switch (e.type) {
                                    case SWT.FocusOut:
                                    	if (newValue >= 0 && newValue <= 255) {
                                    		item.setText(column, text.getText());
                                    		updatePaletteValue(item.getText(column), row, column - 1);
                                    	} else {
                                    	    Tools.showError(tableShell, "Value is out of range [0, 255]\n", tableShell.getText());
                                    	}
                                        
                                        text.dispose();
                                        break;
                                    case SWT.Traverse:
                                        switch (e.detail) {
                                        case SWT.TRAVERSE_RETURN:
                                        	if (newValue >= 0 && newValue <= 255) {
                                        		item.setText(column, text.getText());
                                        		updatePaletteValue(item.getText(column), row, column - 1);
                                        	} else {
                                        	    Tools.showError(tableShell, "Value is out of range [0, 255]\n", tableShell.getText());
                                        	}
                                        case SWT.TRAVERSE_ESCAPE:
                                            text.dispose();
                                            e.doit = false;
                                        }

                                        break;
                                    }
                                }
                            };
                            
                            text.addListener(SWT.FocusOut, textListener);
                            text.addListener(SWT.Traverse, textListener);
                            editor.setEditor(text, item, i);
                            text.setText(item.getText(i));
                            text.selectAll();
                            text.setFocus();
                            return;
                        }
                        
                        if (!visible && rect.intersects(clientArea)) {
                            visible = true;
                        }
                    }
                    
                    if (!visible) return;
                    
                    index++;
                }
            }
        };
    }
}
