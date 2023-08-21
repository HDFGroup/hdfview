/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the COPYING file, which can be found  *
 * at the root of the source code distribution tree,                         *
 * or in https://www.hdfgroup.org/licenses.                                  *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view.PaletteView;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
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
import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;
import hdf.view.ImageView.ImageView;

/**
 * Displays a dialog for viewing and change palettes.
 *
 * @author Jordan T. Henderson
 * @version 2.4 2/27/16
 */
public class DefaultPaletteView extends Dialog implements PaletteView
{
    private Shell shell;

    private Font curFont;

    private ScalarDS dataset;

    /** Panel that draws plot of data values. */
    private ChartCanvas chartP;
    private ImageView imageView;
    private PaletteValueTable paletteValueTable;

    private Button checkRed;
    private Button checkGreen;
    private Button checkBlue;

    private Combo choicePalette;

    private Image originalImage;
    private Image currentImage;

    private static final int[] lineColors = { SWT.COLOR_RED, SWT.COLOR_GREEN, SWT.COLOR_BLUE };
    private static final String[] lineLabels = { "Red", "Green", "Blue" };

    private static final String PALETTE_GRAY = "Gray";
    private static final String PALETTE_DEFAULT = "Default";
    private static final String PALETTE_REVERSE_GRAY = "Reverse Gray";
    private static final String PALETTE_GRAY_WAVE = "GrayWave";
    private static final String PALETTE_RAINBOW = "Rainbow";
    private static final String PALETTE_NATURE = "Nature";
    private static final String PALETTE_WAVE = "Wave";

    private byte[][] palette;
    private int numberOfPalettes;
    private int[][] paletteData;

    private boolean isPaletteChanged = false;
    private boolean isH5 = false;

    /**
     * Create a dialog for viewing and changing palettes.
     *
     * @param parent
     *        the parent component
     * @param theImageView
     *        the associated ImageView
     */
    public DefaultPaletteView(Shell parent, ImageView theImageView) {
        this(parent, null, theImageView);
    }

    /**
     * Create a dialog for viewing and change palettes.
     *
     * @param parent
     *        the parent component
     * @param theViewer
     *        the data view manager
     * @param theImageView
     *        the associated ImageView
     */
    public DefaultPaletteView(Shell parent, DataViewManager theViewer, ImageView theImageView) {
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
                if (d < 0)
                    d += 256;
                paletteData[i][j] = d;
            }
        }

        originalImage = currentImage = imageView.getImage();
        palette = new byte[3][256];

        isH5 = dataset.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));

        createUI();
    }

    /** Create the visual components */
    public void createUI() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("Image Palette for - " + dataset.getPath() + dataset.getName());
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setLayout(new GridLayout(1, true));

        shell.setData(this);

        chartP = new ChartCanvas(shell, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 700 + (ViewProperties.getFontSize() - 12) * 15;
        data.heightHint = 500 + (ViewProperties.getFontSize() - 12) * 10;
        chartP.setLayoutData(data);

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
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = choicePalette.getSelectionIndex();
                if (idx <= 0)
                    return;

                byte[][] imagePalette = null;
                Object item = choicePalette.getItem(idx);

                if (item.equals(PALETTE_DEFAULT))
                    imagePalette = dataset.getPalette();
                else if (item.equals(PALETTE_GRAY))
                    imagePalette = Tools.createGrayPalette();
                else if (item.equals(PALETTE_REVERSE_GRAY))
                    imagePalette = Tools.createReverseGrayPalette();
                else if (item.equals(PALETTE_GRAY_WAVE))
                    imagePalette = Tools.createGrayWavePalette();
                else if (item.equals(PALETTE_RAINBOW))
                    imagePalette = Tools.createRainbowPalette();
                else if (item.equals(PALETTE_NATURE))
                    imagePalette = Tools.createNaturePalette();
                else if (item.equals(PALETTE_WAVE))
                    imagePalette = Tools.createWavePalette();
                else if (idx > 0 && idx <= numberOfPalettes)
                    imagePalette = dataset.readPalette(idx - 1);
                else
                    imagePalette = Tools.readPalette((String) item);

                if (imagePalette == null)
                    return;

                int d = 0;
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 256; j++) {
                        d = imagePalette[i][j];
                        if (d < 0)
                            d += 256;
                        paletteData[i][j] = d;
                    }
                }

                chartP.redraw();
                isPaletteChanged = true;
            }
        });

        choicePalette.add("Select palette");

        String paletteName = dataset.getPaletteName(0);

        if (paletteName != null)
            paletteName = paletteName.trim();

        if (paletteName != null && paletteName.length() > 0)
            choicePalette.add(paletteName);

        if (isH5 && (dataset instanceof ScalarDS))
            numberOfPalettes = dataset.getNumberOfPalettes();
        for (int i = 1; i < numberOfPalettes; i++) {
            paletteName = dataset.getPaletteName(i);
            choicePalette.add(paletteName);
        }
        choicePalette.add(PALETTE_GRAY);
        choicePalette.add(PALETTE_GRAY_WAVE);
        choicePalette.add(PALETTE_RAINBOW);
        choicePalette.add(PALETTE_NATURE);
        choicePalette.add(PALETTE_WAVE);
        ArrayList<?> plist = (ArrayList<?>) ViewProperties.getPaletteList();
        int n = plist.size();
        for (int i = 0; i < n; i++)
            choicePalette.add((String) plist.get(i));

        choicePalette.select(0);

        Button showValueButton = new Button(paletteComposite, SWT.PUSH);
        showValueButton.setFont(curFont);
        showValueButton.setText("Show Values");
        showValueButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        showValueButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (paletteValueTable == null)
                    paletteValueTable = new PaletteValueTable(shell, SWT.NONE);

                paletteValueTable.open();
            }
        });

        // Add Ok/Cancel/Preview buttons
        Composite buttonComposite = new Composite(tools, SWT.BORDER);
        buttonComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("   &OK   ");
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
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
        cancelButton.setText(" &Cancel ");
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                imageView.setImage(originalImage);
                shell.dispose();
            }
        });

        Button previewButton = new Button(buttonComposite, SWT.PUSH);
        previewButton.setFont(curFont);
        previewButton.setText("&Preview");
        previewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePalette();
                imageView.setImage(currentImage);
            }
        });

        shell.pack();

        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null)
                    curFont.dispose();
            }
        });

        shell.setSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Rectangle parentBounds = parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

        shell.open();
    }

    /** @return the data object displayed in this data viewer */
    @Override
    public HObject getDataObject() {
        return dataset;
    }

    private void updatePalette() {
        for (int i = 0; i < 256; i++) {
            palette[0][i] = (byte) paletteData[0][i];
            palette[1][i] = (byte) paletteData[1][i];
            palette[2][i] = (byte) paletteData[2][i];
        }

        IndexColorModel colorModel = new IndexColorModel(
                8,   // bits - the number of bits each pixel occupies
                256, // size - the size of the color component arrays
                palette[0],  // r - the array of red color components
                palette[1],  // g - the array of green color components
                palette[2]); // b - the array of blue color components

        long w = dataset.getWidth();
        long h = dataset.getHeight();
        MemoryImageSource memoryImageSource = null;

        try {
            memoryImageSource = (MemoryImageSource) originalImage.getSource();
        }
        catch (Exception err) {
            memoryImageSource = null;
        }

        if (memoryImageSource == null)
            memoryImageSource = new MemoryImageSource((int) w, (int) h, colorModel, imageView.getImageByteData(), 0, (int) w);
        else
            memoryImageSource.newPixels(imageView.getImageByteData(), colorModel, 0, (int) w);

        currentImage = Tools.toBufferedImage(Toolkit.getDefaultToolkit().createImage(memoryImageSource));
    }

    /** The canvas that paints the data lines. */
    private class ChartCanvas extends Canvas
    {
        // Value controlling gap between the sides of the canvas
        // and the drawn elements
        private final int gap = 20;

        private int xgap = 0;
        private int ygap = 0;

        private int plotWidth = 0;
        private int plotHeight = 0;

        private final int LEGEND_LINE_WIDTH = 10;
        private final int LEGEND_LINE_GAP = 30;

        // Values controlling the dimensions of the legend,
        // as well as the gap in between each
        // element displayed in the legend
        private int LEGEND_WIDTH = 60;
        private final int LEGEND_HEIGHT = (5 * LEGEND_LINE_GAP);

        private final int PALETTE_MAX = 255;

        private int dragX0, dragY0; // starting point of mouse drag

        public ChartCanvas(Composite parent, int style) {
            super(parent, style);

            this.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

            this.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e) {
                    // Get the graphics context for this paint event
                    GC g = e.gc;

                    g.setFont(curFont);

                    Rectangle canvasBounds = getClientArea();
                    org.eclipse.swt.graphics.Color c = g.getForeground();

                    // Make sure legend width scales with font size
                    for (int i = 0; i < lineLabels.length; i++) {
                        int width = g.stringExtent(lineLabels[i]).x;
                        if (width > (2 * LEGEND_WIDTH / 3) - 10)
                            LEGEND_WIDTH += width;
                    }

                    // Calculate maximum width needed to draw the y-axis labels
                    final int maxYLabelWidth = g.stringExtent(String.valueOf(PALETTE_MAX)).x;

                    // Calculate maximum height needed to draw the x-axis labels
                    final int maxXLabelHeight = g.stringExtent(String.valueOf(PALETTE_MAX)).y;

                    xgap = maxYLabelWidth + gap;
                    ygap = getSize().y - maxXLabelHeight - gap - 1;
                    plotHeight = ygap - gap;
                    plotWidth = canvasBounds.width - LEGEND_WIDTH - (2 * gap) - xgap;

                    // draw the X axis
                    g.drawLine(xgap, ygap, xgap + plotWidth, ygap);

                    // draw the Y axis
                    g.drawLine(xgap, ygap, xgap, gap);

                    // draw X and Y labels: 10 labels for x and y
                    int dh = plotHeight / 10;
                    int dw = plotWidth / 10;
                    int dx = 25;
                    double dy = 25;
                    int xp = 2 * gap;
                    int yp = 0;
                    int x = 0;
                    int x0;
                    int y0;
                    int x1;
                    int y1;
                    double y = 0;

                    // draw X and Y grid labels
                    String xVal = String.valueOf(x);
                    String yVal = String.valueOf((int) y);
                    Point xLabelSize = g.stringExtent(xVal);
                    Point yLabelSize = g.stringExtent(yVal);
                    g.drawString(yVal, 0, ygap - yLabelSize.y / 2);
                    g.drawString(xVal, xgap - xLabelSize.x / 2, canvasBounds.height - xLabelSize.y);
                    for (int i = 0; i < 10; i++) {
                        xp += dw;
                        yp += dh;
                        x += dx;
                        y += dy;

                        xVal = String.valueOf(x);
                        yVal = String.valueOf((int) y);
                        xLabelSize = g.stringExtent(xVal);
                        yLabelSize = g.stringExtent(yVal);

                        // Draw tick marks
                        g.drawLine(xp, ygap, xp, ygap - 5);
                        g.drawLine(xgap, ygap - yp, xgap + 5, ygap - yp);

                        g.drawString(xVal, xp - (xLabelSize.x / 2), canvasBounds.height - xLabelSize.y);
                        g.drawString(yVal, 0, ygap - yp - (yLabelSize.y / 2));
                    }

                    for (int i = 0; i < 3; i++) {
                        g.setForeground(Display.getCurrent().getSystemColor(lineColors[i]));

                        // set up the line data for drawing one line a time
                        for (int j = 0; j < 255; j++) {
                            x0 = xgap + (plotWidth * j / 255);
                            y0 = ygap - (plotHeight * paletteData[i][j] / 255);
                            x1 = xgap + (plotWidth * (j + 1) / 255);
                            y1 = ygap - (plotHeight * (paletteData[i][j + 1]) / 255);
                            g.drawLine(x0, y0, x1, y1);
                        }

                        // Draw lines and labels in the legend
                        x0 = (canvasBounds.width - gap - LEGEND_WIDTH) + (LEGEND_WIDTH / 3);
                        y0 = gap + LEGEND_LINE_GAP * (i + 1);
                        g.drawLine(x0, y0, x0 + LEGEND_LINE_WIDTH, y0);
                        g.drawString(lineLabels[i], x0 + 10, y0 + 3);
                    }

                    // draw a box on the legend
                    g.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                    g.drawRectangle(canvasBounds.width - LEGEND_WIDTH - gap, gap, LEGEND_WIDTH, LEGEND_HEIGHT);

                    g.setForeground(c); // set the color back to its default
                }
            });

            //TODO: editing behavior not quite correct yet
            this.addMouseMoveListener(new MouseMoveListener() {
                @Override
                public void mouseMove(MouseEvent e) {
                    if ((e.stateMask & SWT.BUTTON1) != 0) {
                        int x1 = e.x - 40;
                        if (x1 < 0)
                            x1 = 0;
                        int y1 = e.y + 20;

                        Point size = chartP.getSize();
                        double ry = 255 / (double) size.y;
                        double rx = 255 / (double) size.x;

                        int lineIdx = 0;
                        if (checkGreen.getSelection())
                            lineIdx = 1;
                        else if (checkBlue.getSelection())
                            lineIdx = 2;

                        int idx = 0;
                        double b = (double) (y1 - dragY0) / (double) (x1 - dragX0);
                        double a = dragY0 - b * dragX0;
                        int i0 = Math.min(dragX0, x1);
                        int i1 = Math.max(dragX0, x1);
                        for (int i = i0; i < i1; i++) {
                            idx = (int) (rx * i);
                            if (idx > 255)
                                continue;
                            double value = 255 - (a + b * i) * ry;
                            if (value < 0)
                                value = 0;
                            else if (value > 255)
                                value = 255;

                            paletteData[lineIdx][idx] = (int) value;
                        }

                        chartP.redraw();
                        isPaletteChanged = true;
                    }
                }
            });

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    // dragX0 = e.x - xgap;
                    // dragY0 = e.y + gap;
                    //
                    //  (dragX0 < 0)
                    // dragX0 = 0;
                    //  (dragX0 > xgap + plotWidth)
                    // dragX0 = xgap + plotWidth;
                    //  (dragY0 < 0)
                    // dragY0 = 0;
                    //  (dragY0 > plotHeight + gap)
                    // dragY0 = plotHeight + gap;
                }

                @Override
                public void mouseUp(MouseEvent e) {
                    if (paletteValueTable != null)
                        paletteValueTable.refresh();
                }
            });
        }
    }

    /** The dialog to show the palette values in spreadsheet. */
    private class PaletteValueTable extends Dialog
    {
        private Display display;
        private Shell tableShell;

        private Table valueTable;

        private static final String RGBNAME = "Color";
        private static final String IDXNAME = "Index";

        public PaletteValueTable(Shell parent, int style) {
            super(parent, style);
        }

        public void open() {
            Shell parent = getParent();
            display = parent.getDisplay();

            tableShell = new Shell(parent, SWT.SHELL_TRIM);
            tableShell.setFont(curFont);
            tableShell.setText("");
            tableShell.setImages(ViewProperties.getHdfIcons());
            tableShell.setLayout(new GridLayout(1, true));

            Composite content = new Composite(tableShell, SWT.NONE);
            content.setLayout(new GridLayout(1, true));
            content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
            data.heightHint = 200;
            content.setLayoutData(data);

            String[] columnNames = { IDXNAME, "Red", "Green", "Blue", RGBNAME };

            valueTable = new Table(content, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.NO_SCROLL);
            valueTable.setHeaderVisible(true);
            valueTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            valueTable.setFont(curFont);

            // Add cell editor for changing cell values in-place
            valueTable.addListener(SWT.MouseDoubleClick, valueTableCellEditor);

            valueTable.addListener(SWT.Resize, new Listener() {
                @Override
                public void handleEvent(Event e) {
                    int numColumns = valueTable.getColumnCount();

                    for (int i = 0; i < numColumns; i++)
                        valueTable.getColumn(i).setWidth(valueTable.getClientArea().width / numColumns);
                }
            });

            valueTable.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e) {
                    for (int i = 0; i < valueTable.getItemCount(); i++) {
                        Color cellColor = new Color(display, paletteData[0][i], paletteData[1][i], paletteData[2][i]);

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

                item.setText(new String[] {
                        String.valueOf(i),
                        String.valueOf(paletteData[0][i]),
                        String.valueOf(paletteData[1][i]),
                        String.valueOf(paletteData[2][i]), null });

                item.setBackground(0, Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            }

            // set cell height for large fonts
            // int cellRowHeight = Math.max(16, valueTable.getFontMetrics(
            // valueTable.getFont()).getHeight());
            // valueTable.setRowHeight(cellRowHeight);

            Button okButton = new Button(tableShell, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
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

            while (!tableShell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
        }

        private void updatePaletteValue(String strValue, int row, int col) {
            if (strValue == null)
                return;

            int value = 0;

            try {
                value = Integer.parseInt(strValue);
            }
            catch (Exception ex) {
                return;
            }

            if (value < 0 || value > 255) {
                Tools.showError(tableShell, "Update", "Value is out of range [0, 255]");
                return;
            }

            paletteData[col][row] = value;
            chartP.redraw();
            isPaletteChanged = true;
        }

        public void refresh() {
            if (valueTable != null && !valueTable.isDisposed())
                valueTable.redraw();
        }

        private Listener valueTableCellEditor = new Listener() {
            @Override
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
                                @Override
                                public void handleEvent(final Event e) {
                                    switch (e.type) {
                                        case SWT.FocusOut:
                                            item.setText(column, text.getText());
                                            updatePaletteValue(item.getText(column), row, column - 1);
                                            text.dispose();
                                            break;
                                        case SWT.Traverse:
                                            switch (e.detail) {
                                                case SWT.TRAVERSE_RETURN:
                                                    item.setText(column, text.getText());
                                                    updatePaletteValue(item.getText(column), row, column - 1);
                                                    break;
                                                case SWT.TRAVERSE_ESCAPE:
                                                    text.dispose();
                                                    e.doit = false;
                                                    break;
                                                default:
                                                    break;
                                            }
                                            break;
                                        default:
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

                        if (!visible && rect.intersects(clientArea))
                            visible = true;
                    }

                    if (!visible)
                        return;

                    index++;
                }
            }
        };
    }
}
