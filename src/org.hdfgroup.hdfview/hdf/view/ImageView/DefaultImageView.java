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

package hdf.view.ImageView;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.Chart;
import hdf.view.DataView.DataViewFactory;
import hdf.view.DataView.DataViewFactoryProducer;
import hdf.view.DataView.DataViewManager;
import hdf.view.DefaultFileFilter;
import hdf.view.PaletteView.PaletteView;
import hdf.view.Tools;
import hdf.view.TreeView.TreeView;
import hdf.view.ViewProperties;
import hdf.view.ViewProperties.BITMASK_OP;
import hdf.view.ViewProperties.DataViewType;
import hdf.view.dialog.NewDatasetDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeItem;

/**
 * ImageView displays an HDF dataset as an image.
 *
 * A scalar dataset in HDF can be displayed in image or table. By default, an HDF4 GR image and HDF5 image is
 * displayed as an image. Other scalar datasets are display in a two-dimensional table.
 *
 * Users can also choose to display a scalar dataset as image. Currently this version of the ImageView only
 * supports 8-bit raster image with indexed RGB color model of 256 colors or 24-bit true color raster image.
 * Data of other type will be converted to 8-bit integer. The simple linear conversion is used for this
 * purpose:
 *
 * <pre>
 * y = f * (x - min),
 *       where y   = the value of 8-bit integer,
 *             x   = the value of original data,
 *             f   = 255/(max-min), conversion factor,
 *             max = the maximum of the original data,
 *             min = the minimum of the original data.
 * </pre>
 *
 * A default color table is provided for images without palette attached to it. Current choice of default
 * palettes include Gray, Rainbow, Nature and Wave. For more infomation on palette, read <a
 * href="https://hdfgroup.github.io/hdf5/_i_m_g.html">HDF5 Image and Palette Specification</a>
 *
 * @author Jordan T. Henderson
 * @version 2.4 2//2016
 */
public class DefaultImageView implements ImageView {
    private static final Logger log = LoggerFactory.getLogger(DefaultImageView.class);

    private final Display display = Display.getDefault();
    private final Shell shell;
    private Font curFont;

    /** Horizontal direction to flip an image. */
    public static final int FLIP_HORIZONTAL = 0;

    /** Vertical direction to flip an image. */
    public static final int FLIP_VERTICAL = 1;

    /** ROTATE IMAGE 90 DEGREE CLOCKWISE. */
    public static final int ROTATE_CW_90 = 10;

    /** ROTATE IMAGE COUNTER CLOCKWISE 90 DEGREE. */
    public static final int ROTATE_CCW_90 = 11;

    /**
     * The main HDFView.
     */
    private final DataViewManager viewer;

    private Toolkit toolkit;

    /**
     * The Scalar Dataset.
     */
    private ScalarDS dataset;

    /**
     * The Component containing the image.
     */
    private ImageComponent imageComponent;

    /**
     * The Label for the image origin.
     */
    private Label imageOriginLabel;

    /**
     * The image contained in the ImageView.
     */
    private Image image;

    /**
     * The zooming factor of this image.
     */
    private float zoomFactor;

    /**
     * The byte data array of the image.
     */
    private byte[] imageByteData;

    /**
     * The color table of the image.
     */
    private byte[][] imagePalette;

    /**
     * The title of this imageview.
     */
    private String frameTitle;

    /** TextField to show the image value. */
    private Text valueField;

    /** Flag to indicate if the image is a true color image */
    private boolean isTrueColor;

    /** Flag to indicate if the image is a 3D */
    private boolean is3D;

    /** Flag to indicate whether to show pixel values in ImageComponent */
    private boolean showValues = false;

    /** Flag to indicate if the image is plane interleaved */
    private boolean isPlaneInterlace;

    private boolean isHorizontalFlipped = false;

    private boolean isVerticalFlipped = false;

    private int rotateCount = 0;

    /** the number type of the image data */
    private char NT;

    /** the raw data of the image */
    private Object data;

    private boolean isUnsignedConverted = false;

    private double[] dataRange;
    private double[] originalRange = {0, 0};

    private PaletteComponent paletteComponent;

    private int animationSpeed = 2;

    private List rotateRelatedItems;

    private ScrolledComposite imageScroller;

    private Text frameField;

    private long curFrame = 0;
    private long maxFrame = 1;

    private BufferedImage bufferedImage;

    private ContrastSlider contrastSlider;

    private int indexBase  = 0;
    private int[] dataDist = null;

    /**
     * equates to brightness
     */
    private boolean doAutoGainContrast = false;
    private double[] gainBias;
    private double[] gainBiasCurrent;

    /**
     * int array to hold unsigned short or signed int data from applying the
     * autogain
     */
    private Object autoGainData;

    private BitSet bitmask;
    private boolean convertByteData = false;
    private BITMASK_OP bitmaskOP    = BITMASK_OP.EXTRACT;

    private enum Origin { UPPER_LEFT, LOWER_LEFT, UPPER_RIGHT, LOWER_RIGHT }

    private Origin imageOrigin = null;

    private List<Integer> invalidValueIndex;

    /**
     * Constructs an ImageView.
     *
     * @param theView
     *            the main HDFView.
     */
    public DefaultImageView(DataViewManager theView) { this(theView, null); }

    /**
     * Constructs an ImageView.
     *
     * @param theView
     *            the main HDFView.
     * @param map
     *            the properties on how to show the data. The map is used to
     *            allow applications to pass properties on how to display the
     *            data, such as, transposing data, showing data as character,
     *            applying bitmask, and etc. Predefined keys are listed at
     *            ViewProperties.DATA_VIEW_KEY.
     */
    @SuppressWarnings("rawtypes")
    public DefaultImageView(DataViewManager theView, HashMap map)
    {
        shell = new Shell(display, SWT.SHELL_TRIM);

        shell.setData(this);

        shell.setImage(ViewProperties.getImageIcon());
        shell.setLayout(new GridLayout(1, true));

        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                // reload the data when it is displayed next time
                // because the display type (table or image) may be
                // different.
                if ((dataset != null) && !dataset.isImage()) {
                    dataset.clearData();
                }

                if (curFont != null)
                    curFont.dispose();

                data           = null;
                image          = null;
                imageByteData  = null;
                imageComponent = null;
                autoGainData   = null;
                ((Vector)rotateRelatedItems).setSize(0);

                viewer.removeDataView(DefaultImageView.this);

                System.gc();
            }
        });

        try {
            curFont =
                new Font(display, ViewProperties.getFontType(), ViewProperties.getFontSize(), SWT.NORMAL);
        }
        catch (Exception ex) {
            curFont = null;
        }

        shell.setFont(curFont);

        viewer             = theView;
        zoomFactor         = 1.0f;
        imageByteData      = null;
        imagePalette       = null;
        paletteComponent   = null;
        isTrueColor        = false;
        is3D               = false;
        isPlaneInterlace   = false;
        data               = null;
        NT                 = 0;
        showValues         = ViewProperties.showImageValues();
        rotateRelatedItems = new Vector(10);
        imageScroller      = null;
        gainBias           = null;
        gainBiasCurrent    = null;
        autoGainData       = null;
        contrastSlider     = null;
        bitmask            = null;
        invalidValueIndex  = new ArrayList<>();

        toolkit = Toolkit.getDefaultToolkit();

        String origStr = ViewProperties.getImageOrigin();
        if (ViewProperties.ORIGIN_LL.equalsIgnoreCase(origStr))
            imageOrigin = Origin.LOWER_LEFT;
        else if (ViewProperties.ORIGIN_UR.equalsIgnoreCase(origStr))
            imageOrigin = Origin.UPPER_RIGHT;
        else if (ViewProperties.ORIGIN_LR.equalsIgnoreCase(origStr))
            imageOrigin = Origin.LOWER_RIGHT;
        else
            imageOrigin = Origin.UPPER_LEFT;

        if (ViewProperties.isIndexBase1())
            indexBase = 1;

        HObject hobject = null;

        if (map != null) {
            hobject   = (HObject)map.get(ViewProperties.DATA_VIEW_KEY.OBJECT);
            bitmask   = (BitSet)map.get(ViewProperties.DATA_VIEW_KEY.BITMASK);
            bitmaskOP = (BITMASK_OP)map.get(ViewProperties.DATA_VIEW_KEY.BITMASKOP);

            Boolean b = (Boolean)map.get(ViewProperties.DATA_VIEW_KEY.CONVERTBYTE);
            if (b != null)
                convertByteData = b.booleanValue();

            b = (Boolean)map.get(ViewProperties.DATA_VIEW_KEY.INDEXBASE1);
            if (b != null) {
                if (b.booleanValue())
                    indexBase = 1;
                else
                    indexBase = 0;
            }
        }

        if (hobject == null) {
            hobject = theView.getTreeView().getCurrentObject();
        }

        if ((hobject == null) || !(hobject instanceof ScalarDS)) {
            viewer.showError("Display data in image failed for - " + hobject);
            return;
        }

        dataset   = (ScalarDS)hobject;
        dataRange = dataset.getImageDataRange();
        if (dataRange == null) {
            dataRange    = new double[2];
            dataRange[0] = dataRange[1] = 0;
            if (dataset.getDatatype().getDatatypeSize() == 1 && !convertByteData) {
                dataRange[1] = 255; // byte image data rang = [0, 255]
            }
        }
        else {
            if (dataRange[0] < dataRange[1])
                convertByteData = true;
        }

        if (image == null) {
            image = getImage();
        }

        if (image == null) {
            viewer.showError("Loading image failed - " + dataset.getName());
            dataset = null;
            return;
        }

        originalRange[0] = dataRange[0];
        originalRange[1] = dataRange[1];

        // set title
        StringBuilder sb = new StringBuilder(hobject.getName());
        sb.append("  at  ")
            .append(hobject.getPath())
            .append("  [")
            .append(dataset.getFileFormat().getName())
            .append("  in  ")
            .append(dataset.getFileFormat().getParent())
            .append("]");

        frameTitle = sb.toString();
        shell.setText(sb.toString());

        // setup subset information
        int rank            = dataset.getRank();
        int[] selectedIndex = dataset.getSelectedIndex();
        long[] count        = dataset.getSelectedDims();
        long[] stride       = dataset.getStride();
        long[] dims         = dataset.getDims();
        long[] start        = dataset.getStartDims();
        int n               = Math.min(3, rank);

        if (rank > 2) {
            curFrame = start[selectedIndex[2]] + indexBase;
            maxFrame = (indexBase == 1) ? dims[selectedIndex[2]] : dims[selectedIndex[2]] - 1;
        }

        sb.append(" [ dims");
        sb.append(selectedIndex[0]);
        for (int i = 1; i < n; i++) {
            sb.append("x");
            sb.append(selectedIndex[i]);
        }
        sb.append(", start");
        sb.append(start[selectedIndex[0]]);
        for (int i = 1; i < n; i++) {
            sb.append("x");
            sb.append(start[selectedIndex[i]]);
        }
        sb.append(", count");
        sb.append(count[selectedIndex[0]]);
        for (int i = 1; i < n; i++) {
            sb.append("x");
            sb.append(count[selectedIndex[i]]);
        }
        sb.append(", stride");
        sb.append(stride[selectedIndex[0]]);
        for (int i = 1; i < n; i++) {
            sb.append("x");
            sb.append(stride[selectedIndex[i]]);
        }
        sb.append(" ] ");

        viewer.showStatus(sb.toString());

        shell.setMenuBar(createMenuBar());

        // Add toolbar for Histogram, Frame selection, etc.
        ToolBar bar = createToolbar(shell);
        bar.setSize(shell.getSize().x, 30);
        bar.setLocation(0, 0);

        String originTag = "(0,0)";
        if (ViewProperties.isIndexBase1())
            originTag = "(1,1)";

        // Create main component region
        org.eclipse.swt.widgets.Group group = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        group.setFont(curFont);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        if (imageOrigin == Origin.UPPER_LEFT || imageOrigin == Origin.UPPER_RIGHT) {
            imageOriginLabel = new Label(group, SWT.NONE);
            imageOriginLabel.setText(originTag);
            imageOriginLabel.setLayoutData(new GridData(
                (imageOrigin == Origin.UPPER_LEFT || imageOrigin == Origin.LOWER_LEFT) ? SWT.BEGINNING
                                                                                       : SWT.END,
                SWT.FILL, true, false, (imagePalette == null) ? 2 : 1, 1));

            /* Dummy label to fill space in second column */
            if (imagePalette != null)
                new Label(group, SWT.NONE);
        }

        imageScroller = new ScrolledComposite(group, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        imageScroller.getHorizontalBar().setIncrement(50);
        imageScroller.getVerticalBar().setIncrement(50);
        imageScroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        imageScroller.setFont(curFont);

        imageComponent = new ImageComponent(imageScroller, SWT.DOUBLE_BUFFERED, image);
        imageScroller.setContent(imageComponent);

        if (imageOrigin == Origin.LOWER_LEFT)
            flip(FLIP_VERTICAL);
        else if (imageOrigin == Origin.UPPER_RIGHT)
            flip(FLIP_HORIZONTAL);
        if (imageOrigin == Origin.LOWER_RIGHT) {
            rotate(ROTATE_CW_90);
            rotate(ROTATE_CW_90);
        }

        // add palette canvas to show the palette
        if (imagePalette != null) {
            paletteComponent = new PaletteComponent(group, SWT.DOUBLE_BUFFERED, imagePalette, dataRange);
        }
        else {
            // Make ImageComponent take entire width
            imageScroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        }

        if (imageOrigin == Origin.LOWER_LEFT || imageOrigin == Origin.LOWER_RIGHT) {
            imageOriginLabel = new Label(group, SWT.NONE);
            imageOriginLabel.setText(originTag);
            imageOriginLabel.setLayoutData(new GridData(
                (imageOrigin == Origin.UPPER_LEFT || imageOrigin == Origin.LOWER_LEFT) ? SWT.BEGINNING
                                                                                       : SWT.END,
                SWT.FILL, true, false, (imagePalette == null) ? 2 : 1, 1));

            /* Dummy label to fill space in second column */
            if (imagePalette != null)
                new Label(group, SWT.NONE);
        }

        // Add the text field to display pixel data
        valueField = new Text(group, SWT.BORDER | SWT.SINGLE);
        valueField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        valueField.setEditable(false);
        valueField.setFont(curFont);
        setValueVisible(showValues);

        shell.pack();

        int width  = 700 + (ViewProperties.getFontSize() - 12) * 15;
        int height = 500 + (ViewProperties.getFontSize() - 12) * 10;
        shell.setSize(width, height);

        viewer.addDataView(this);

        shell.open();
    }

    private Menu createMenuBar()
    {
        Menu menuBar = new Menu(shell, SWT.BAR);

        MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
        item.setText("Image");

        Menu menu = new Menu(item);
        item.setMenu(menu);

        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Save Image As");

        Menu saveAsMenu = new Menu(item);
        item.setMenu(saveAsMenu);

        item = new MenuItem(saveAsMenu, SWT.PUSH);
        item.setText("JPEG");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String filetype = Tools.FILE_TYPE_JPEG;

                try {
                    saveImageAs(filetype);
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Save", ex.getMessage());
                }
            }
        });

        /*
         * ImageIO does not support tiff by default
         */
        /**
         * item = new MenuItem(saveAsMenu, SWT.PUSH); item.setText("TIFF"); item.addSelectionListener(new
         * SelectionAdapter() { public void widgetSelected(SelectionEvent e) { String filetype =
         * Tools.FILE_TYPE_TIFF;
         *
         * try { saveImageAs(filetype); } catch (Exception ex) { shell.getDisplay().beep();
         * Tools.showError(shell, "Save", ex.getMessage()); } } });
         */

        item = new MenuItem(saveAsMenu, SWT.PUSH);
        item.setText("PNG");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String filetype = Tools.FILE_TYPE_PNG;

                try {
                    saveImageAs(filetype);
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Save", ex.getMessage());
                }
            }
        });

        item = new MenuItem(saveAsMenu, SWT.PUSH);
        item.setText("GIF");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String filetype = Tools.FILE_TYPE_GIF;

                try {
                    saveImageAs(filetype);
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Save", ex.getMessage());
                }
            }
        });

        item = new MenuItem(saveAsMenu, SWT.PUSH);
        item.setText("BMP");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String filetype = Tools.FILE_TYPE_BMP;

                try {
                    saveImageAs(filetype);
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Save", ex.getMessage());
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Write Selection to Image");
        item.setEnabled(!dataset.getFileFormat().isReadOnly());
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                writeSelectionToImage();
            }
        });

        rotateRelatedItems.add(item);

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Change Palette");
        item.setEnabled(!isTrueColor);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showColorTable();
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Import Palette");
        item.setEnabled(!isTrueColor);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
                fChooser.setFilterPath(ViewProperties.getWorkDir());

                fChooser.setFilterExtensions(new String[] {"*"});
                fChooser.setFilterNames(new String[] {"All Files"});
                fChooser.setFilterIndex(0);

                if (fChooser.open() == null)
                    return;

                File chosenFile =
                    new File(fChooser.getFilterPath() + File.separator + fChooser.getFileName());
                if (chosenFile == null || !chosenFile.exists() || chosenFile.isDirectory())
                    return;

                ArrayList<String> palList = (ArrayList<String>)ViewProperties.getPaletteList();
                String palPath            = chosenFile.getAbsolutePath();
                if (!palList.contains(palPath))
                    palList.add(palPath);
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Export Palette");
        item.setEnabled(!isTrueColor);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (imagePalette == null)
                    return;

                String workDir      = ViewProperties.getWorkDir() + File.separator;
                FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
                fChooser.setFilterPath(workDir);

                fChooser.setFilterExtensions(new String[] {"*", "*.lut"});
                fChooser.setFilterNames(new String[] {"All Files", "Color Lookup Table"});
                fChooser.setFilterIndex(1);

                File pfile = Tools.checkNewFile(workDir, ".lut");

                fChooser.setFileName(pfile.getName());

                if (fChooser.open() == null)
                    return;

                File chosenFile =
                    new File(fChooser.getFilterPath() + File.separator + fChooser.getFileName());
                if (chosenFile == null || chosenFile.isDirectory())
                    return;

                if (chosenFile.exists()) {
                    int answer = SWT.NO;
                    if (Tools.showConfirm(shell, "Export", "File exists. Do you want to replace it ?"))
                        answer = SWT.YES;

                    if (answer == SWT.NO)
                        return;
                }

                PrintWriter out = null;

                try {
                    out = new PrintWriter(new BufferedWriter(new FileWriter(chosenFile)));
                }
                catch (Exception ex) {
                    out = null;
                }

                if (out == null)
                    return;

                int cols = 3;
                int rows = 256;
                int rgb  = 0;
                for (int i = 0; i < rows; i++) {
                    out.print(i);
                    for (int j = 0; j < cols; j++) {
                        out.print(' ');
                        rgb = imagePalette[j][i];
                        if (rgb < 0)
                            rgb += 256;
                        out.print(rgb);
                    }
                    out.println();
                }

                out.flush();
                out.close();
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Set Value Range");
        item.setEnabled(!isTrueColor);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (originalRange == null || originalRange[0] == originalRange[1])
                    return;

                // Call only once
                if (dataDist == null) {
                    dataDist = new int[256];
                    Tools.findDataDist(data, dataDist, originalRange);
                }

                DataRangeDialog drd =
                    new DataRangeDialog(shell, SWT.NONE, dataRange, originalRange, dataDist);
                drd.open();

                double[] drange = drd.getRange();

                if ((drange == null) || (drange[0] == drange[1]) ||
                    ((drange[0] == dataRange[0]) && (drange[1] == dataRange[1]))) {
                    return;
                }

                applyDataRange(drange);
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Histogram");
        item.setEnabled(!isTrueColor);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showHistogram();
            }
        });
        rotateRelatedItems.add(item);

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Zoom In");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                zoomIn();
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Zoom Out");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                zoomOut();
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Flip Image");

        Menu flipMenu = new Menu(item);
        item.setMenu(flipMenu);

        item = new MenuItem(flipMenu, SWT.PUSH);
        item.setText("Horizontal");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                flip(FLIP_HORIZONTAL);
            }
        });

        item = new MenuItem(flipMenu, SWT.PUSH);
        item.setText("Vertical");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                flip(FLIP_VERTICAL);
            }
        });

        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Rotate Image");

        Menu rotateMenu = new Menu(item);
        item.setMenu(rotateMenu);

        char t = 186;

        item = new MenuItem(rotateMenu, SWT.PUSH);
        item.setText("90" + t + " CW");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                rotate(ROTATE_CW_90);

                int n = rotateRelatedItems.size();
                for (int i = 0; i < n; i++) {
                    boolean itemState = (rotateCount == 0);
                    ((MenuItem)rotateRelatedItems.get(i)).setEnabled(itemState);
                }
            }
        });

        item = new MenuItem(rotateMenu, SWT.PUSH);
        item.setText("90" + t + " CCW");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                rotate(ROTATE_CCW_90);

                int n = rotateRelatedItems.size();
                for (int i = 0; i < n; i++) {
                    boolean itemState = (rotateCount == 0);
                    ((MenuItem)rotateRelatedItems.get(i)).setEnabled(itemState);
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Brightness/Contrast");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (contrastSlider == null)
                    contrastSlider = new ContrastSlider(shell, SWT.NONE, image.getSource());
                contrastSlider.open();
            }
        });

        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Contour");

        Menu contourMenu = new Menu(item);
        item.setMenu(contourMenu);

        for (int i = 3; i < 10; i += 2) {
            item = new MenuItem(contourMenu, SWT.PUSH);
            item.setText(String.valueOf(i));
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    MenuItem item = (MenuItem)e.widget;
                    contour(Integer.parseInt(item.getText()));
                }
            });
        }

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Animation");
        item.setEnabled(is3D);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

                new Animation(shell, SWT.DOUBLE_BUFFERED, dataset).open();

                shell.setCursor(null);
            }
        });

        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Animation Speed (frames/second)");
        item.setEnabled(is3D);

        Menu animationSpeedMenu = new Menu(item);
        item.setMenu(animationSpeedMenu);

        for (int i = 2; i < 12; i += 2) {
            item = new MenuItem(animationSpeedMenu, SWT.PUSH);
            item.setText(String.valueOf(i));
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    MenuItem item  = (MenuItem)e.widget;
                    animationSpeed = Integer.parseInt(item.getText());
                }
            });
        }

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.CHECK);
        item.setText("Show Values");
        item.setSelection(showValues);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showValues = !showValues;
                setValueVisible(showValues);
            }
        });
        rotateRelatedItems.add(item);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Statistics");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try {
                    double[] minmax = new double[2];
                    double[] stat   = new double[2];

                    Object theData = null;
                    theData        = getSelectedData();

                    if (theData == null)
                        theData = data;

                    Tools.findMinMax(theData, minmax, dataset.getFillValue());
                    if (Tools.computeStatistics(theData, stat, dataset.getFillValue()) > 0) {
                        String statistics = "Min                      = " + minmax[0] +
                                            "\nMax                      = " + minmax[1] +
                                            "\nMean                     = " + stat[0] +
                                            "\nStandard deviation = " + stat[1];

                        Tools.showInformation(shell, "Statistics", statistics);
                    }
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Statistics", ex.getMessage());
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Select All");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try {
                    selectAll();
                }
                catch (Exception ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Select", ex.getMessage());
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Close");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                shell.dispose();
            }
        });

        return menuBar;
    }

    private ToolBar createToolbar(final Shell shell)
    {
        ToolBar toolbar = new ToolBar(shell, SWT.HORIZONTAL | SWT.RIGHT | SWT.BORDER);
        toolbar.setFont(curFont);
        toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // Chart button
        ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        item.setImage(ViewProperties.getChartIcon());
        item.setToolTipText("Histogram");
        item.setEnabled(!isTrueColor);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showHistogram();
            }
        });

        // Palette button
        item = new ToolItem(toolbar, SWT.PUSH);
        item.setImage(ViewProperties.getPaletteIcon());
        item.setToolTipText("Palette");
        item.setEnabled(!isTrueColor);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showColorTable();
            }
        });

        // Brightness button
        item = new ToolItem(toolbar, SWT.PUSH);
        item.setImage(ViewProperties.getBrightIcon());
        item.setToolTipText("Brightness");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (contrastSlider == null)
                    contrastSlider = new ContrastSlider(shell, SWT.NONE, image.getSource());
                contrastSlider.open();
            }
        });

        // Zoom in button
        item = new ToolItem(toolbar, SWT.PUSH);
        item.setImage(ViewProperties.getZoominIcon());
        item.setToolTipText("Zoom In");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                zoomIn();
            }
        });

        // Zoom out button
        item = new ToolItem(toolbar, SWT.PUSH);
        item.setImage(ViewProperties.getZoomoutIcon());
        item.setToolTipText("Zoom Out");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                zoomOut();
            }
        });

        if (is3D) {
            new ToolItem(toolbar, SWT.SEPARATOR).setWidth(20);

            // First frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getFirstIcon());
            item.setToolTipText("First Page");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    try {
                        shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

                        firstPage();
                    }
                    finally {
                        shell.setCursor(null);
                    }
                }
            });

            // Previous frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getPreviousIcon());
            item.setToolTipText("Previous Page");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    try {
                        shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

                        previousPage();
                    }
                    finally {
                        shell.setCursor(null);
                    }
                }
            });

            ToolItem separator = new ToolItem(toolbar, SWT.SEPARATOR);

            frameField = new Text(toolbar, SWT.SINGLE | SWT.BORDER | SWT.CENTER);
            frameField.setFont(curFont);
            frameField.setText(String.valueOf(curFrame));
            frameField.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(TraverseEvent e)
                {
                    if (e.detail == SWT.TRAVERSE_RETURN) {
                        try {
                            shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

                            int page = 0;

                            try {
                                page = Integer.parseInt(frameField.getText().trim()) - indexBase;
                            }
                            catch (Exception ex) {
                                page = -1;
                            }

                            gotoPage(page);
                        }
                        finally {
                            shell.setCursor(null);
                        }
                    }
                }
            });

            frameField.pack();

            separator.setWidth(frameField.getSize().x + 30);
            separator.setControl(frameField);

            separator = new ToolItem(toolbar, SWT.SEPARATOR);

            Text maxFrameText = new Text(toolbar, SWT.SINGLE | SWT.BORDER | SWT.CENTER);
            maxFrameText.setFont(curFont);
            maxFrameText.setText(String.valueOf(maxFrame));
            maxFrameText.setEditable(false);
            maxFrameText.setEnabled(false);

            maxFrameText.pack();

            separator.setWidth(maxFrameText.getSize().x + 30);
            separator.setControl(maxFrameText);

            new ToolItem(toolbar, SWT.SEPARATOR).setWidth(10);

            // Next frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getNextIcon());
            item.setToolTipText("Next Page");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    try {
                        shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

                        nextPage();
                    }
                    finally {
                        shell.setCursor(null);
                    }
                }
            });

            // Last frame button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getLastIcon());
            item.setToolTipText("Last Page");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    try {
                        shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));

                        lastPage();
                    }
                    finally {
                        shell.setCursor(null);
                    }
                }
            });

            // Animation button
            item = new ToolItem(toolbar, SWT.PUSH);
            item.setImage(ViewProperties.getAnimationIcon());
            item.setToolTipText("View Animation");
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
                    new Animation(shell, SWT.DOUBLE_BUFFERED, dataset).open();
                    shell.setCursor(null);
                }
            });
        }

        return toolbar;
    }

    // Implementing DataObserver.
    private void previousPage()
    {
        int rank = dataset.getRank();

        if (rank < 3)
            return;

        int[] selectedIndex = dataset.getSelectedIndex();
        long[] selectedDims = dataset.getSelectedDims();

        if (selectedDims[selectedIndex[2]] > 1)
            return; // it is a true color image with three color components

        long[] start = dataset.getStartDims();
        long idx     = start[selectedIndex[2]];
        if (idx == 0)
            return; // current page is the first page

        gotoPage(start[selectedIndex[2]] - 1);
    }

    // Implementing DataObserver.
    private void nextPage()
    {
        int rank = dataset.getRank();

        if (rank < 3)
            return;

        int[] selectedIndex = dataset.getSelectedIndex();
        long[] selectedDims = dataset.getSelectedDims();

        if (selectedDims[selectedIndex[2]] > 1)
            return; // it is a true color image with three color components

        long[] start = dataset.getStartDims();
        long[] dims  = dataset.getDims();
        long idx     = start[selectedIndex[2]];
        if (idx == dims[selectedIndex[2]] - 1)
            return; // current page is the last page

        gotoPage(start[selectedIndex[2]] + 1);
    }

    // Implementing DataObserver.
    private void firstPage()
    {
        int rank = dataset.getRank();

        if (rank < 3)
            return;

        int[] selectedIndex = dataset.getSelectedIndex();
        long[] selectedDims = dataset.getSelectedDims();

        if (selectedDims[selectedIndex[2]] > 1)
            return; // it is a true color image with three color components

        long[] start = dataset.getStartDims();
        long idx     = start[selectedIndex[2]];
        if (idx == 0)
            return; // current page is the first page

        gotoPage(0);
    }

    // Implementing DataObserver.
    private void lastPage()
    {
        int rank = dataset.getRank();

        if (rank < 3)
            return;

        int[] selectedIndex = dataset.getSelectedIndex();
        long[] selectedDims = dataset.getSelectedDims();

        if (selectedDims[selectedIndex[2]] > 1)
            return; // it is a true color image with three color components

        long[] start = dataset.getStartDims();
        long[] dims  = dataset.getDims();
        long idx     = start[selectedIndex[2]];
        if (idx == dims[selectedIndex[2]] - 1)
            return; // current page is the last page

        gotoPage(dims[selectedIndex[2]] - 1);
    }

    @Override
    public Image getImage()
    {
        if (image != null) {
            return image;
        }

        if (!dataset.isInited())
            dataset.init();

        isTrueColor = dataset.isTrueColor();
        is3D        = (dataset.getRank() > 2) && !dataset.isTrueColor();

        try {
            if (isTrueColor)
                getTrueColorImage();
            else
                getIndexedImage();
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select", "ImageView: " + shell.getText());
            return null;
        }

        // set number type, ...
        if (data != null) {
            String cname = data.getClass().getName();
            NT           = cname.charAt(cname.lastIndexOf('[') + 1);
        }

        return image;
    }

    /**
     * @throws Exception if a failure occurred
     * @throws OutOfMemoryError if memory is exhausted
     */
    private void getIndexedImage() throws Exception, OutOfMemoryError
    {
        if (imagePalette == null)
            imagePalette = dataset.getPalette();

        boolean noPalette   = false;
        boolean isLocalFile = dataset.getFileFormat().exists();

        if (imagePalette == null) {
            noPalette    = true;
            imagePalette = Tools.createGrayPalette();
            viewer.showStatus("\nNo attached palette found, default grey palette is used to display image");
        }

        // Make sure entire dataset is not loaded when looking at 3D
        // datasets using the default display mode (double clicking the
        // data object)
        if (dataset.getRank() > 2)
            dataset.getSelectedDims()[dataset.getSelectedIndex()[2]] = 1;

        data = dataset.getData();
        if ((bitmask != null) && Tools.applyBitmask(data, bitmask, bitmaskOP))
            doAutoGainContrast = false;

        if (dataset.getDatatype().isInteger() || dataset.getDatatype().isChar()) {
            data                = dataset.convertFromUnsignedC();
            isUnsignedConverted = true;
            doAutoGainContrast =
                doAutoGainContrast || (ViewProperties.isAutoContrast() && noPalette && isLocalFile);
        }
        else
            doAutoGainContrast = false;

        boolean isAutoContrastFailed = true;
        if (doAutoGainContrast) {
            isAutoContrastFailed = (!computeAutoGainImageData(gainBias, null));
        }

        long w = dataset.getWidth();
        long h = dataset.getHeight();

        if (isAutoContrastFailed) {
            doAutoGainContrast = false;
            imageByteData      = Tools.getBytes(data, dataRange, w, h, !dataset.isDefaultImageOrder(),
                                                dataset.getFilteredImageValues(), convertByteData, imageByteData,
                                                invalidValueIndex);
        }
        else if (dataRange != null && dataRange[0] == dataRange[1]) {
            Tools.findMinMax(data, dataRange, null);
        }

        image = createIndexedImage(imageByteData, imagePalette, w, h);
    }

    /**
     * @throws Exception
     * @throws OutOfMemoryError
     */
    private void getTrueColorImage() throws Exception, OutOfMemoryError
    {
        isPlaneInterlace = (dataset.getInterlace() == ScalarDS.INTERLACE_PLANE);

        long[] selected     = dataset.getSelectedDims();
        long[] start        = dataset.getStartDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        long[] stride       = dataset.getStride();

        if (start.length > 2) {
            start[selectedIndex[2]]    = 0;
            selected[selectedIndex[2]] = 3;
            stride[selectedIndex[2]]   = 1;
        }

        // reload data
        dataset.clearData();
        data = dataset.getData();

        long w = dataset.getWidth();
        long h = dataset.getHeight();

        // converts raw data to image data
        imageByteData =
            Tools.getBytes(data, dataRange, w, h, false, dataset.getFilteredImageValues(), imageByteData);

        image = createTrueColorImage(imageByteData, isPlaneInterlace, (int)w, (int)h);
    }

    /**
     * Compute image data from autogain
     *
     * @param gb the gain bias
     * @param range the contrast range to apply
     *
     * @return true if the image buffer is converted
     */
    private boolean computeAutoGainImageData(double[] gb, double[] range)
    {
        boolean retValue = true;

        // data is unsigned short. Convert image byte data using auto-contrast
        // image algorithm

        if (gainBias == null) { // calculate auto_gain only once
            gainBias = new double[2];
            Tools.autoContrastCompute(data, gainBias, dataset.getDatatype().isUnsigned());
        }

        if (gb == null)
            gb = gainBias;

        autoGainData =
            Tools.autoContrastApply(data, autoGainData, gb, range, dataset.getDatatype().isUnsigned());

        if (autoGainData != null) {
            if ((imageByteData == null) || (imageByteData.length != Array.getLength(data))) {
                imageByteData = new byte[Array.getLength(data)];
            }
            retValue = (Tools.autoContrastConvertImageBuffer(autoGainData, imageByteData, true) >= 0);
        }
        else
            retValue = false;

        if (gainBiasCurrent == null)
            gainBiasCurrent = new double[2];

        gainBiasCurrent[0] = gb[0];
        gainBiasCurrent[1] = gb[1];

        return retValue;
    }

    // implementing ImageObserver
    private void zoomIn()
    {
        if (zoomFactor >= 1)
            zoomTo(zoomFactor + 1.0f);
        else
            zoomTo(zoomFactor + 0.125f);
    }

    // implementing ImageObserver
    private void zoomOut()
    {
        if (zoomFactor > 1)
            zoomTo(zoomFactor - 1.0f);
        else
            zoomTo(zoomFactor - 0.125f);
    }

    // implementing ImageObserver
    private void zoomTo(float zf)
    {
        if (zf > 8)
            zf = 8;
        else if (zf < 0.125)
            zf = 0.125f;

        if (zoomFactor == zf)
            return; // no change in zooming

        zoomFactor = zf;

        Dimension imageSize = new Dimension((int)(imageComponent.originalSize.width * zoomFactor),
                                            (int)(imageComponent.originalSize.height * zoomFactor));

        imageComponent.setImageSize(imageSize);
        imageComponent.redraw();

        if ((zoomFactor > 0.99) && (zoomFactor < 1.01))
            shell.setText(frameTitle);
        else
            shell.setText(frameTitle + " - " + 100 * zoomFactor + "%");
    }

    // implementing ImageObserver
    private void showColorTable()
    {
        if (imagePalette == null)
            return;

        DataViewFactory paletteViewFactory = null;
        try {
            paletteViewFactory = DataViewFactoryProducer.getFactory(DataViewType.PALETTE);
        }
        catch (Exception ex) {
            log.debug("showColorTable(): error occurred while instantiating PaletteView factory class", ex);
            viewer.showError("Error occurred while instantiating PaletteView factory class");
            return;
        }

        if (paletteViewFactory == null) {
            log.debug("showColorTable(): PaletteView factory is null");
            return;
        }

        PaletteView theView;
        try {
            theView = paletteViewFactory.getPaletteView(shell, viewer, this);

            if (theView == null) {
                log.debug("showColorTable(): error occurred while instantiating PaletteView class");
                viewer.showError("Error occurred while instantiating PaletteView class");
                Tools.showError(shell, "Show Palette",
                                "Error occurred while instantiating PaletteView class");
            }
        }
        catch (ClassNotFoundException ex) {
            log.debug("showColorTable(): no suitable PaletteView class found");
            viewer.showError("Unable to find suitable PaletteView class for object '" + dataset.getName() +
                             "'");
            Tools.showError(shell, "Show Palette",
                            "Unable to find suitable PaletteView class for object '" + dataset.getName() +
                                "'");
        }
    }

    private void showHistogram()
    {
        Rectangle rec = imageComponent.selectedArea;

        if (isTrueColor) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select",
                            "Unsupported operation: unable to draw histogram for true color image.");
            return;
        }

        if ((rec == null) || (rec.width <= 0) || (rec.height <= 0)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select",
                            "No data for histogram.\nUse Shift+Mouse_drag to select an image area.");
            return;
        }

        double[][] chartData = new double[1][256];
        for (int i = 0; i < 256; i++) {
            chartData[0][i] = 0.0;
        }

        // Java only allows ints for array indices, may cause an issue with a dataset of width
        // larger than an int
        int w          = (int)dataset.getWidth();
        int x0         = (int)(rec.x / zoomFactor);
        int y0         = (int)(rec.y / zoomFactor);
        int x          = x0 + (int)(rec.width / zoomFactor);
        int y          = y0 + (int)(rec.height / zoomFactor);
        int arrayIndex = 0;
        for (int i = y0; i < y; i++) {
            for (int j = x0; j < x; j++) {
                arrayIndex = imageByteData[i * w + j];
                if (arrayIndex < 0)
                    arrayIndex += 256;
                chartData[0][arrayIndex] += 1.0;
            }
        }

        // Use original data range
        double[] xRange = originalRange;
        if (xRange == null || xRange[0] == xRange[1]) {
            xRange = new double[2];
            Tools.findMinMax(data, xRange, null);
        }

        Chart cv =
            new Chart(shell, "Histogram - " + dataset.getPath() + dataset.getName() + " - by pixel index",
                      Chart.HISTOGRAM, chartData, xRange, null);
        cv.open();
    }

    /**
     * Selects the whole image.
     *
     * @throws Exception if a failure occurred
     */
    private void selectAll() throws Exception { imageComponent.selectAll(); }

    // implementing ImageObserver
    private void flip(int direction)
    {
        ImageFilter filter = new FlipFilter(direction);

        if (applyImageFilter(filter)) {
            // toggle flip flag
            if (direction == FLIP_HORIZONTAL)
                isHorizontalFlipped = !isHorizontalFlipped;
            else
                isVerticalFlipped = !isVerticalFlipped;
        }
    }

    // implementing ImageObserver
    private void rotate(int direction)
    {
        if (!(direction == ROTATE_CW_90 || direction == ROTATE_CCW_90))
            return;

        Rotate90Filter filter = new Rotate90Filter(direction);
        applyImageFilter(filter);

        if (direction == ROTATE_CW_90) {
            rotateCount++;
            if (rotateCount == 4)
                rotateCount = 0;
        }
        else {
            rotateCount--;
            if (rotateCount == -4)
                rotateCount = 0;
        }
    }

    // implementing ImageObserver
    private void contour(int level) { applyImageFilter(new ContourFilter(level)); }

    /**
     * Apply contrast/brightness to unsigned short integer
     *
     * @param gb the gain bias
     * @param range the contrast range to apply
     */
    private void applyAutoGain(double[] gb, double[] range)
    {
        if (computeAutoGainImageData(gb, range)) {
            long w = dataset.getWidth();
            long h = dataset.getHeight();
            image  = createIndexedImage(imageByteData, imagePalette, w, h);
            imageComponent.setImage(image);
            zoomTo(zoomFactor);
        }
    }

    private void setValueVisible(boolean b)
    {
        valueField.setVisible(b);

        GridData gridData = (GridData)valueField.getLayoutData();

        if (!b)
            gridData.exclude = true;
        else
            gridData.exclude = false;

        valueField.setLayoutData(gridData);

        valueField.getParent().pack();

        shell.pack();
    }

    /**
     * change alpha value for a given list of pixel locations
     *
     * @param img the image to adjust
     * @param alpha the alpha value
     * @param idx the list of indices to adjust
     *
     */
    private void adjustAlpha(BufferedImage img, int alpha, List<Integer> idx)
    {
        if (img == null || idx.isEmpty())
            return;

        final int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
        int len            = pixels.length;

        alpha = alpha << 24;
        for (Integer i : idx) {
            if (i < len)
                pixels[i] = alpha | (pixels[i] & 0x00ffffff);
        }
    }

    /**
     * Save the image to an image file.
     *
     * @param type
     *            the image type.
     *
     * @throws Exception
     *             if a failure occurred
     */
    private void saveImageAs(String type) throws Exception
    {
        if (image == null) {
            return;
        }

        FileDialog fChooser = new FileDialog(shell, SWT.SAVE);
        fChooser.setFilterPath(dataset.getFileFormat().getParent());
        fChooser.setOverwrite(true);

        DefaultFileFilter filter = null;

        if (type.equals(Tools.FILE_TYPE_JPEG)) {
            filter = DefaultFileFilter.getFileFilterJPEG();
        } /**
           * else if (type.equals(Tools.FILE_TYPE_TIFF)) { filter = DefaultFileFilter.getFileFilterTIFF(); }
           */
        else if (type.equals(Tools.FILE_TYPE_PNG)) {
            filter = DefaultFileFilter.getFileFilterPNG();
        }
        else if (type.equals(Tools.FILE_TYPE_GIF)) {
            filter = DefaultFileFilter.getFileFilterGIF();
        }
        else if (type.equals(Tools.FILE_TYPE_BMP)) {
            filter = DefaultFileFilter.getFileFilterBMP();
        }

        if (filter == null) {
            fChooser.setFilterExtensions(new String[] {"*"});
            fChooser.setFilterNames(new String[] {"All Files"});
            fChooser.setFilterIndex(0);
        }
        else {
            fChooser.setFilterExtensions(new String[] {"*", filter.getExtensions()});
            fChooser.setFilterNames(new String[] {"All Files", filter.getDescription()});
            fChooser.setFilterIndex(1);
        }

        fChooser.setText("Save Current Image To " + type + " File --- " + dataset.getName());

        File chosenFile = new File(dataset.getName() + "." + type.toLowerCase());
        fChooser.setFileName(chosenFile.getName());

        String filename = fChooser.open();

        if (filename == null) {
            return;
        }

        chosenFile = new File(filename);

        BufferedImage bi = null;
        try {
            bi = Tools.toBufferedImage(image);

            // Convert JPG and BMP images to TYPE_INT_RGB so ImageIO.write succeeds
            if (bi.getType() == BufferedImage.TYPE_INT_ARGB && (type.equals("JPEG") || type.equals("BMP"))) {
                BufferedImage newImage =
                    new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics g = newImage.createGraphics();
                g.drawImage(bi, 0, 0, Color.BLACK, null);
                g.dispose();
                bi = newImage;
            }
        }
        catch (OutOfMemoryError err) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Save", err.getMessage());
            return;
        }

        Tools.saveImageAs(bi, chosenFile, type);

        viewer.showStatus("Current image saved to: " + chosenFile.getAbsolutePath());

        try (RandomAccessFile rf = new RandomAccessFile(chosenFile, "r")) {
            long size = rf.length();
            viewer.showStatus("File size (bytes): " + size);
        }
        catch (Exception ex) {
            log.debug("File {} size:", chosenFile.getName(), ex);
        }
    }

    // Implementing DataView.
    @Override
    public HObject getDataObject()
    {
        return dataset;
    }

    @Override
    public byte[] getImageByteData()
    {
        return imageByteData;
    }

    /**
     * Returns the selected data values.
     *
     * @return the selected data object.
     */
    @Override
    public Object getSelectedData()
    {
        Object selectedData = null;

        int cols = imageComponent.originalSelectedArea.width;
        int rows = imageComponent.originalSelectedArea.height;

        if ((cols <= 0) || (rows <= 0)) {
            return null; // no data is selected
        }

        int size = cols * rows;
        if (isTrueColor) {
            size *= 3;
        }

        if (NT == 'B') {
            selectedData = new byte[size];
        }
        else if (NT == 'S') {
            selectedData = new short[size];
        }
        else if (NT == 'I') {
            selectedData = new int[size];
        }
        else if (NT == 'J') {
            selectedData = new long[size];
        }
        else if (NT == 'F') {
            selectedData = new float[size];
        }
        else if (NT == 'D') {
            selectedData = new double[size];
        }
        else {
            return null;
        }

        int r0 = imageComponent.originalSelectedArea.y;
        int c0 = imageComponent.originalSelectedArea.x;
        int w  = imageComponent.originalSize.width;
        int h  = imageComponent.originalSize.height;

        // transfer location to the original coordinator
        if (isHorizontalFlipped) {
            c0 = w - 1 - c0 - cols;
        }

        if (isVerticalFlipped) {
            r0 = h - 1 - r0 - rows;
        }

        int idxSrc = 0;
        int idxDst = 0;
        if (isTrueColor) {
            int imageSize = w * h;
            if (isPlaneInterlace) {
                for (int j = 0; j < 3; j++) {
                    int plane = imageSize * j;
                    for (int i = 0; i < rows; i++) {
                        idxSrc = plane + (r0 + i) * w + c0;
                        System.arraycopy(data, idxSrc, selectedData, idxDst, cols);
                        idxDst += cols;
                    }
                }
            }
            else {
                int numberOfDataPoints = cols * 3;
                for (int i = 0; i < rows; i++) {
                    idxSrc = (r0 + i) * w + c0;
                    System.arraycopy(data, idxSrc * 3, selectedData, idxDst, numberOfDataPoints);
                    idxDst += numberOfDataPoints;
                }
            }
        }
        else { // indexed image
            for (int i = 0; i < rows; i++) {
                idxSrc = (r0 + i) * w + c0;
                System.arraycopy(data, idxSrc, selectedData, idxDst, cols);
                idxDst += cols;
            }
        }

        return selectedData;
    }

    /**
     * returns the selected area of the image
     *
     * @return the rectangle of the selected image area.
     */
    @Override
    public Rectangle getSelectedArea()
    {
        return imageComponent.originalSelectedArea;
    }

    /** @return true if the image is a truecolor image. */
    @Override
    public boolean isTrueColor()
    {
        return isTrueColor;
    }

    /** @return true if the image interlace is plance interlace. */
    @Override
    public boolean isPlaneInterlace()
    {
        return isPlaneInterlace;
    }

    @Override
    public void setImage(Image img)
    {
        image = img;
        imageComponent.setImage(img);

        setImageDirection();
    }

    private void setImageDirection()
    {
        boolean isHF = isHorizontalFlipped;
        boolean isVF = isVerticalFlipped;
        int rc       = rotateCount;

        if (isHF || isVF || rc != 0) {
            isHorizontalFlipped = false;
            isVerticalFlipped   = false;
            rotateCount         = 0;

            if (isHF)
                flip(FLIP_HORIZONTAL);

            if (isVF)
                flip(FLIP_VERTICAL);

            while (rc > 0) {
                rotate(ROTATE_CW_90);
                rc--;
            }

            while (rc < 0) {
                rotate(ROTATE_CCW_90);
                rc++;
            }
        }
        else {
            if (imageOrigin == Origin.LOWER_LEFT)
                flip(FLIP_VERTICAL);
            else if (imageOrigin == Origin.UPPER_RIGHT)
                flip(FLIP_HORIZONTAL);
            if (imageOrigin == Origin.LOWER_RIGHT) {
                rotate(ROTATE_CW_90);
                rotate(ROTATE_CW_90);
            }
        }

        zoomTo(zoomFactor);
    }

    @Override
    public byte[][] getPalette()
    {
        return imagePalette;
    }

    @Override
    public void setPalette(byte[][] pal)
    {
        imagePalette = pal;
        paletteComponent.updatePalette(pal);
    }

    private void gotoPage(long idx)
    {
        if (dataset.getRank() < 3 || idx == (curFrame - indexBase)) {
            return;
        }

        long[] start        = dataset.getStartDims();
        int[] selectedIndex = dataset.getSelectedIndex();
        long[] dims         = dataset.getDims();

        if ((idx < 0) || (idx >= dims[selectedIndex[2]])) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Select",
                            "Frame number must be between " + indexBase + " and " +
                                (dims[selectedIndex[2]] - 1 + indexBase));
            return;
        }

        start[selectedIndex[2]] = idx;
        curFrame                = idx + indexBase;
        dataset.clearData();
        image    = null;
        gainBias = null;
        imageComponent.setImage(getImage());
        frameField.setText(String.valueOf(curFrame));

        isHorizontalFlipped = false;
        isVerticalFlipped   = false;
        rotateCount         = 0;

        if (imageOrigin == Origin.LOWER_LEFT)
            flip(FLIP_VERTICAL);
        else if (imageOrigin == Origin.UPPER_RIGHT)
            flip(FLIP_HORIZONTAL);
        if (imageOrigin == Origin.LOWER_RIGHT) {
            rotate(ROTATE_CW_90);
            rotate(ROTATE_CW_90);
        }
    }

    /**
     * Converts a given BufferedImage to ImageData for a SWT-readable Image
     *
     * @param image The BufferedImage to be converted
     *
     * @return the image object
     */
    private org.eclipse.swt.graphics.Image convertBufferedImageToSWTImage(BufferedImage image)
    {
        if (image.getColorModel() instanceof DirectColorModel) {
            DirectColorModel colorModel = (DirectColorModel)image.getColorModel();
            PaletteData palette =
                new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
            ImageData imgData =
                new ImageData(image.getWidth(), image.getHeight(), colorModel.getPixelSize(), palette);

            for (int y = 0; y < imgData.height; y++) {
                for (int x = 0; x < imgData.width; x++) {
                    int rgb   = image.getRGB(x, y);
                    int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                    imgData.setPixel(x, y, pixel);
                    if (colorModel.hasAlpha()) {
                        imgData.setAlpha(x, y, (rgb >> 24) & 0xFF);
                    }
                }
            }

            return new org.eclipse.swt.graphics.Image(display, imgData);
        }
        else if (image.getColorModel() instanceof IndexColorModel) {
            IndexColorModel colorModel = (IndexColorModel)image.getColorModel();
            int size                   = colorModel.getMapSize();
            byte[] reds                = new byte[size];
            byte[] greens              = new byte[size];
            byte[] blues               = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
            }
            PaletteData palette = new PaletteData(rgbs);
            ImageData imgData =
                new ImageData(image.getWidth(), image.getHeight(), colorModel.getPixelSize(), palette);
            imgData.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster    = image.getRaster();
            int[] pixelArray         = new int[1];
            for (int y = 0; y < imgData.height; y++) {
                for (int x = 0; x < imgData.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    imgData.setPixel(x, y, pixelArray[0]);
                }
            }

            return new org.eclipse.swt.graphics.Image(display, imgData);
        }
        else if (image.getColorModel() instanceof ComponentColorModel) {
            ComponentColorModel colorModel = (ComponentColorModel)image.getColorModel();
            // ASSUMES: 3 BYTE BGR IMAGE TYPE
            PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
            ImageData imgData =
                new ImageData(image.getWidth(), image.getHeight(), colorModel.getPixelSize(), palette);
            // This is valid because we are using a 3-byte Data model with no transparent pixels
            imgData.transparentPixel = -1;
            WritableRaster raster    = image.getRaster();
            int[] pixelArray         = new int[3];
            for (int y = 0; y < imgData.height; y++) {
                for (int x = 0; x < imgData.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
                    imgData.setPixel(x, y, pixel);
                }
            }
            return new org.eclipse.swt.graphics.Image(display, imgData);
        }

        return null;
    }

    /**
     * Creates a RGB indexed image of 256 colors.
     *
     * @param imageData
     *            the byte array of the image data.
     * @param palette
     *            the color lookup table.
     * @param w
     *            the width of the image.
     * @param h
     *            the height of the image.
     *
     * @return the image.
     */
    private Image createIndexedImage(byte[] imageData, byte[][] palette, long w, long h)
    {
        bufferedImage = (BufferedImage)Tools.createIndexedImage(bufferedImage, imageData, palette, w, h);
        adjustAlpha(bufferedImage, 0, invalidValueIndex);

        return bufferedImage;
    }

    /**
     * Creates a true color image.
     *
     * The data may be arranged in one of two ways: by pixel or by plane. In
     * both cases, the dataset will have a dataspace with three dimensions,
     * height, width, and components.
     *
     * For HDF4, the interlace modes specify orders for the dimensions as:
     *
     * <pre>
     * INTERLACE_PIXEL = [width][height][pixel components]
     * INTERLACE_PLANE = [pixel components][width][height]
     * </pre>
     *
     * For HDF5, the interlace modes specify orders for the dimensions as:
     *
     * <pre>
     * INTERLACE_PIXEL = [height][width][pixel components]
     * INTERLACE_PLANE = [pixel components][height][width]
     * </pre>
     *
     * @param imageData
     *            the byte array of the image data.
     * @param planeInterlace
     *            flag if the image is plane intelace.
     * @param w
     *            the width of the image.
     * @param h
     *            the height of the image.
     *
     * @return the image.
     */
    private Image createTrueColorImage(byte[] imageData, boolean planeInterlace, int w, int h)
    {
        if (bufferedImage == null)
            bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        final int[] pixels = ((DataBufferInt)bufferedImage.getRaster().getDataBuffer()).getData();
        int len            = pixels.length;

        int idx = 0, r = 0, g = 0, b = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if (planeInterlace) {
                    r = (imageData[idx] & 0xff) << 16;
                    g = (imageData[len + idx] & 0xff) << 8;
                    b = (imageData[len * 2 + idx] & 0xff);
                }
                else {
                    r = (imageData[idx * 3] & 0xff) << 16;
                    g = (imageData[idx * 3 + 1] & 0xff) << 8;
                    b = (imageData[idx * 3 + 2] & 0xff);
                }
                pixels[idx++] = 0xff000000 | r | g | b;
            }
        }

        adjustAlpha(bufferedImage, 0, invalidValueIndex);

        return bufferedImage;
    }

    private boolean applyImageFilter(ImageFilter filter)
    {
        boolean status              = true;
        ImageProducer imageProducer = image.getSource();

        try {
            image =
                Tools.toBufferedImage(toolkit.createImage(new FilteredImageSource(imageProducer, filter)));
            imageComponent.setImage(image);
            zoomTo(zoomFactor);
        }
        catch (Exception err) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Apply Image Filter", err.getMessage());
            status = false;
        }

        return status;
    }

    private void applyDataRange(double[] newRange)
    {
        if (doAutoGainContrast && gainBias != null) {
            applyAutoGain(gainBiasCurrent, newRange);
        }
        else {
            long w = dataset.getWidth();
            long h = dataset.getHeight();

            invalidValueIndex.clear(); // data range changed. need to reset

            // invalid values
            imageByteData = Tools.getBytes(data, newRange, w, h, !dataset.isDefaultImageOrder(),
                                           dataset.getFilteredImageValues(), true, null, invalidValueIndex);

            image = createIndexedImage(imageByteData, imagePalette, w, h);
            setImage(image);
            zoomTo(zoomFactor);
            paletteComponent.updateRange(newRange);
        }

        dataRange[0] = newRange[0];
        dataRange[1] = newRange[1];
    }

    private void writeSelectionToImage()
    {
        if ((getSelectedArea().width <= 0) || (getSelectedArea().height <= 0)) {
            Tools.showError(shell, "Select",
                            "No data to write.\nUse Shift+Mouse_drag to select an image area.");
            return;
        }

        TreeView treeView = viewer.getTreeView();
        TreeItem item     = treeView.findTreeItem(dataset);
        Group pGroup      = (Group)item.getParentItem().getData();
        HObject root      = dataset.getFileFormat().getRootObject();

        if (root == null)
            return;

        ArrayList<HObject> list = new ArrayList<>(dataset.getFileFormat().getNumberOfMembers() + 5);
        Iterator<HObject> it    = ((Group)root).depthFirstMemberList().iterator();

        list.add(dataset.getFileFormat().getRootObject());

        while (it.hasNext()) {
            list.add(it.next());
        }

        NewDatasetDialog dialog = new NewDatasetDialog(shell, pGroup, list, this);
        dialog.open();

        HObject obj = dialog.getObject();
        if (obj != null) {
            Group pgroup = dialog.getParentGroup();
            try {
                treeView.addObject(obj, pgroup);
            }
            catch (Exception ex) {
                log.debug("Write selection to image: ", ex);
            }
        }

        list.clear();
    }

    /** PaletteComponent draws the palette on the side of the image. */
    private class PaletteComponent extends Canvas {

        private org.eclipse.swt.graphics.Color[] colors = null;
        private double[] pixelData                      = null;
        private Dimension paintSize                     = null;
        DecimalFormat format;
        double[] dRange = null;

        private PaletteComponent(Composite parent, int style, byte[][] palette, double[] range)
        {
            super(parent, style);

            paintSize = new Dimension(25, 2);
            format    = new DecimalFormat("0.00E0");
            dRange    = range;

            if ((palette != null) && (range != null)) {
                double ratio = (dRange[1] - dRange[0]) / 255;

                pixelData = new double[256];
                for (int i = 0; i < 256; i++) {
                    pixelData[i] = (dRange[0] + ratio * i);
                }
            }

            updatePalette(palette);

            this.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    // Dispose all created colors to prevent memory leak
                    for (int i = 0; i < colors.length; i++) {
                        if (colors[i] != null)
                            colors[i].dispose();
                    }
                }
            });

            this.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e)
                {
                    if ((colors == null) && (pixelData == null)) {
                        return;
                    }

                    GC gc                                        = e.gc;
                    org.eclipse.swt.graphics.Color oldBackground = gc.getBackground();

                    for (int i = 0; i < 256; i++) {
                        if ((colors != null) && (colors[i] != null))
                            gc.setBackground(colors[i]);
                        gc.fillRectangle(0, paintSize.height * i, paintSize.width, paintSize.height);
                    }

                    FontData[] fontData;
                    int fontHeight = 10;

                    if (curFont != null) {
                        fontData = curFont.getFontData();
                    }
                    else {
                        fontData = Display.getDefault().getSystemFont().getFontData();
                    }

                    Font newFont =
                        new Font(display, fontData[0].getName(), fontHeight, fontData[0].getStyle());
                    gc.setFont(newFont);

                    gc.setBackground(oldBackground);
                    gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));

                    int trueHeight;
                    int i = 0;
                    while (i < 25) {
                        String str = format.format(pixelData[i * 10]);
                        trueHeight = gc.textExtent(str).y;

                        gc.drawString(str, paintSize.width + 5,
                                      (trueHeight + paintSize.height + 1) * i -
                                          ((trueHeight - fontHeight) / 2));

                        i++;
                    }

                    String str = format.format(pixelData[255]);
                    trueHeight = gc.textExtent(str).y;

                    gc.drawString(str, paintSize.width + 5,
                                  (trueHeight + paintSize.height + 1) * i - ((trueHeight - fontHeight) / 2));

                    newFont.dispose();
                }
            });

            GridData gridData  = new GridData(SWT.FILL, SWT.FILL, false, true);
            gridData.widthHint = paintSize.width + 60;
            this.setLayoutData(gridData);
        }

        private void updatePalette(byte[][] palette)
        {
            if ((palette != null) && (dRange != null)) {
                colors = new org.eclipse.swt.graphics.Color[256];

                int r, g, b;
                for (int i = 0; i < 256; i++) {
                    r = palette[0][i];
                    if (r < 0) {
                        r += 256;
                    }
                    g = palette[1][i];
                    if (g < 0) {
                        g += 256;
                    }
                    b = palette[2][i];
                    if (b < 0) {
                        b += 256;
                    }

                    colors[i] = new org.eclipse.swt.graphics.Color(display, r, g, b);
                }
            }

            redraw();
        }

        private void updateRange(double[] newRange)
        {
            if (newRange == null) {
                return;
            }

            dRange       = newRange;
            double ratio = (dRange[1] - dRange[0]) / 255;
            for (int i = 0; i < 256; i++) {
                pixelData[i] = (dRange[0] + ratio * i);
            }

            redraw();
        }
    }

    /** ImageComponent draws the image. */
    private class ImageComponent extends Canvas implements ImageObserver {
        /* The BufferedImage is converted to an SWT Image for dislay */
        private org.eclipse.swt.graphics.Image convertedImage;

        private Dimension originalSize;
        private Dimension imageSize;
        private Point scrollDim = null;
        private Point startPosition;   // mouse clicked position
        private Point currentPosition; // mouse clicked position
        private Rectangle selectedArea;
        private Rectangle originalSelectedArea;
        private StringBuilder strBuff;  // to hold display value
        private int yMousePosition = 0; // the vertical position of the current mouse
        private ScrollBar hbar     = null;
        private ScrollBar vbar     = null;

        public ImageComponent(Composite parent, int style, Image img)
        {
            super(parent, style);

            convertedImage = convertBufferedImageToSWTImage((BufferedImage)img);
            if (convertedImage != null)
                imageSize =
                    new Dimension(convertedImage.getBounds().width, convertedImage.getBounds().height);

            originalSize         = imageSize;
            selectedArea         = new Rectangle();
            originalSelectedArea = new Rectangle();
            setSize(imageSize.width, imageSize.height);
            strBuff = new StringBuilder();

            this.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent arg0)
                {
                    if (convertedImage != null && !convertedImage.isDisposed())
                        convertedImage.dispose();
                }
            });

            this.addMouseMoveListener(new MouseMoveListener() {
                @Override
                public void mouseMove(MouseEvent e)
                {
                    currentPosition = new Point(e.x, e.y);

                    if ((e.stateMask & SWT.BUTTON1) != 0) {
                        // If a drag event has occurred, draw a selection Rectangle
                        if ((e.stateMask & SWT.SHIFT) != 0) {
                            int x0 = Math.max(0, Math.min(startPosition.x, currentPosition.x));
                            int y0 = Math.max(0, Math.min(startPosition.y, currentPosition.y));
                            int x1 = Math.min(imageSize.width, Math.max(startPosition.x, currentPosition.x));
                            int y1 = Math.min(imageSize.height, Math.max(startPosition.y, currentPosition.y));

                            int w = x1 - x0;
                            int h = y1 - y0;

                            selectedArea.setBounds(x0, y0, w, h);
                            double ratio = 1.0 / zoomFactor;

                            originalSelectedArea.setBounds((int)(x0 * ratio), (int)(y0 * ratio),
                                                           (int)(w * ratio), (int)(h * ratio));
                        }
                        else {
                            if ((hbar != null) && hbar.isVisible()) {
                                int dx = startPosition.x - currentPosition.x;
                                hbar.setSelection(hbar.getSelection() + dx);
                            }

                            if ((vbar != null) && vbar.isVisible()) {
                                int dy = startPosition.y - currentPosition.y;
                                vbar.setSelection(vbar.getSelection() + dy);
                            }
                        }

                        redraw();
                    }

                    if (showValues) {
                        yMousePosition = e.y;
                        showPixelValue(e.x, yMousePosition);
                    }
                }
            });

            this.addMouseListener(new MouseListener() {
                @Override
                public void mouseDoubleClick(MouseEvent e)
                {
                    // Intentional
                }

                @Override
                public void mouseDown(MouseEvent e)
                {
                    startPosition = new Point(e.x, e.y);

                    selectedArea.x      = startPosition.x;
                    selectedArea.y      = startPosition.y;
                    selectedArea.width  = 0;
                    selectedArea.height = 0;

                    scrollDim = imageScroller.getSize();
                    hbar      = imageScroller.getHorizontalBar();
                    vbar      = imageScroller.getVerticalBar();

                    if ((e.stateMask & SWT.SHIFT) != 0) {
                        shell.setCursor(display.getSystemCursor(SWT.CURSOR_CROSS));
                    }
                    else {
                        shell.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
                    }
                }

                @Override
                public void mouseUp(MouseEvent e)
                {
                    shell.setCursor(null);

                    // Single mouse click
                    if (e.count == 1) {
                        if (startPosition.x == e.x && startPosition.y == e.y) {
                            selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
                            originalSelectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
                        }

                        startPosition = new Point(e.x, e.y);

                        if (hbar.isVisible()) {
                            hbar.setSelection(startPosition.x - scrollDim.x / 2);
                        }

                        if (vbar.isVisible()) {
                            vbar.setSelection(startPosition.y - scrollDim.y / 2);
                        }

                        redraw();
                    }
                }
            });

            this.addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseScrolled(MouseEvent e)
                {
                    ScrollBar jb = imageScroller.getVerticalBar();

                    jb.getSelection();
                    showPixelValue(e.x, yMousePosition);
                }
            });

            this.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e)
                {
                    GC gc = e.gc;

                    org.eclipse.swt.graphics.Rectangle sourceBounds = convertedImage.getBounds();

                    gc.drawImage(convertedImage, 0, 0, sourceBounds.width, sourceBounds.height, 0, 0,
                                 imageSize.width, imageSize.height);

                    if ((selectedArea.width > 0) && (selectedArea.height > 0)) {
                        gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
                        gc.drawRectangle(selectedArea.x, selectedArea.y, selectedArea.width,
                                         selectedArea.height);
                    }
                }
            });
        }

        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
        {
            return false;
        }

        /**
         * Create an image using multiple step bilinear, see details at
         * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
         *
         * @param img          the original image to be scaled
         * @param targetWidth  the desired width of the scaled instance
         * @param targetHeight the desired height of the scaled instance
         * @param highquality  the quality desired
         *
         * @return a scaled version of the original
         */
        private Image multiBilinear(Image img, int targetWidth, int targetHeight, boolean highquality)
        {
            Image ret = img;
            int w     = img.getWidth(null) / 2;
            int h     = img.getHeight(null) / 2;

            // only do multiple step bilinear for down scale more than two times
            if (!highquality || w <= targetWidth || h <= targetHeight)
                return ret;

            int type = BufferedImage.TYPE_INT_RGB;
            if (image instanceof BufferedImage) {
                BufferedImage tmp = (BufferedImage)image;
                if (tmp.getColorModel().hasAlpha())
                    type = BufferedImage.TYPE_INT_ARGB;
            }
            else {
                PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
                ColorModel cm   = pg.getColorModel();
                if (cm != null && cm.hasAlpha())
                    type = BufferedImage.TYPE_INT_ARGB;
            }

            do {
                BufferedImage tmp = new BufferedImage(w, h, type);
                Graphics2D g2     = tmp.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(ret, 0, 0, w, h, null);
                g2.dispose();
                ret = tmp;

                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }

                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }

            } while (w != targetWidth || h != targetHeight);

            return ret;
        }

        private void showPixelValue(int x, int y)
        {
            if (!valueField.isVisible() || rotateCount != 0) {
                return;
            }

            if (data == null) {
                return;
            }

            x     = (int)(x / zoomFactor);
            int w = originalSize.width;

            if ((x < 0) || (x >= w)) {
                return; // out of image bound
            }

            y     = (int)(y / zoomFactor);
            int h = originalSize.height;
            if ((y < 0) || (y >= h)) {
                return; // out of image bound
            }

            // transfer location to the original coordinator
            if (isHorizontalFlipped) {
                x = w - 1 - x;
            }

            if (isVerticalFlipped) {
                y = h - 1 - y;
            }

            strBuff.setLength(0); // reset the string buffer
            strBuff.append("x=")
                .append(x + indexBase)
                .append(",   y=")
                .append(y + indexBase)
                .append(",   value=");

            if (isTrueColor) {
                int i0, i1, i2;
                String r, g, b;

                if (isPlaneInterlace) {
                    i0 = y * w + x;      // index for the first plane
                    i1 = i0 + w * h;     // index for the second plane
                    i2 = i0 + 2 * w * h; // index for the third plane
                }
                else {
                    i0 = 3 * (y * w + x); // index for the first pixel
                    i1 = i0 + 1;          // index for the second pixel
                    i2 = i0 + 2;          // index for the third pixel
                }

                if (dataset.getDatatype().isUnsigned() && !isUnsignedConverted) {
                    r = String.valueOf(convertUnsignedPoint(i0));
                    g = String.valueOf(convertUnsignedPoint(i1));
                    b = String.valueOf(convertUnsignedPoint(i2));
                }
                else {
                    r = String.valueOf(Array.get(data, i0));
                    g = String.valueOf(Array.get(data, i1));
                    b = String.valueOf(Array.get(data, i2));
                }

                strBuff.append("(").append(r + ", " + g + ", " + b).append(")");
            } // (isTrueColor)
            else {
                int idx;

                if (!dataset.isDefaultImageOrder())
                    idx = x * h + y;
                else
                    idx = y * w + x;

                if (dataset.getDatatype().isUnsigned() && !isUnsignedConverted) {
                    strBuff.append(convertUnsignedPoint(idx));
                }
                else {
                    strBuff.append(Array.get(data, idx));
                }
            }

            valueField.setText(strBuff.toString());
        } // private void showPixelValue

        private long convertUnsignedPoint(int idx)
        {
            long l = 0;

            if (NT == 'B') {
                byte b = Array.getByte(data, idx);

                if (b < 0) {
                    l = (long)b + 256;
                }
                else {
                    l = b;
                }
            }
            else if (NT == 'S') {
                short s = Array.getShort(data, idx);
                if (s < 0) {
                    l = (long)s + 65536;
                }
                else {
                    l = s;
                }
            }
            else if (NT == 'I') {
                int i = Array.getInt(data, idx);
                if (i < 0) {
                    l = i + 4294967296L;
                }
                else {
                    l = i;
                }
            }

            return l;
        }

        private void selectAll()
        {
            selectedArea.setBounds(0, 0, imageSize.width, imageSize.height);
            originalSelectedArea.setBounds(0, 0, originalSize.width, originalSize.height);

            redraw();
        }

        private void setImageSize(Dimension size)
        {
            imageSize = size;
            setSize(imageSize.width, imageSize.height);

            int w = selectedArea.width;
            int h = selectedArea.height;
            if ((w > 0) && (h > 0)) {
                // use fixed selected area to reduce the rounding error
                selectedArea.setBounds((int)(originalSelectedArea.x * zoomFactor),
                                       (int)(originalSelectedArea.y * zoomFactor),
                                       (int)(originalSelectedArea.width * zoomFactor),
                                       (int)(originalSelectedArea.height * zoomFactor));
            }

            redraw();
        }

        private void setImage(Image img)
        {
            /* Make sure to dispose the old image first so resources aren't leaked */
            if (convertedImage != null && !convertedImage.isDisposed())
                convertedImage.dispose();

            convertedImage = convertBufferedImageToSWTImage((BufferedImage)img);
            if (convertedImage != null)
                imageSize =
                    new Dimension(convertedImage.getBounds().width, convertedImage.getBounds().height);
            originalSize        = imageSize;
            selectedArea.width  = 0;
            selectedArea.height = 0;
            setSize(imageSize.width, imageSize.height);

            setImageSize(new Dimension((int)(originalSize.width * zoomFactor),
                                       (int)(originalSize.height * zoomFactor)));

            redraw();
        }
    }

    /**
     * FlipFilter creates image filter to flip image horizontally or vertically.
     */
    public static class FlipFilter extends ImageFilter {
        /** flip direction */
        private int direction;

        /** pixel value */
        private int[] raster = null;

        /** width & height */
        private int imageWidth;
        private int imageHeight;

        /**
         * Constructs an image filter to flip horizontally or vertically.
         *
         * @param d
         *            the flip direction.
         */
        public FlipFilter(int d)
        {
            if (d < FLIP_HORIZONTAL)
                d = FLIP_HORIZONTAL;
            else if (d > FLIP_VERTICAL)
                d = FLIP_VERTICAL;

            direction = d;
        }

        @Override
        public void setDimensions(int w, int h)
        {
            imageWidth  = w;
            imageHeight = h;

            // specify the raster
            if (raster == null)
                raster = new int[imageWidth * imageHeight];

            consumer.setDimensions(imageWidth, imageHeight);
        }

        @Override
        public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
                              int scansize)
        {
            int srcoff = off;
            int dstoff = y * imageWidth + x;
            for (int yc = 0; yc < h; yc++) {
                for (int xc = 0; xc < w; xc++)
                    raster[dstoff++] = model.getRGB(pixels[srcoff++] & 0xff);

                srcoff += (scansize - w);
                dstoff += (imageWidth - w);
            }
        }

        @Override
        public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
                              int scansize)
        {
            int srcoff = off;
            int dstoff = y * imageWidth + x;

            for (int yc = 0; yc < h; yc++) {
                for (int xc = 0; xc < w; xc++)
                    raster[dstoff++] = model.getRGB(pixels[srcoff++]);
                srcoff += (scansize - w);
                dstoff += (imageWidth - w);
            }
        }

        @Override
        public void imageComplete(int status)
        {
            if ((status == IMAGEERROR) || (status == IMAGEABORTED)) {
                consumer.imageComplete(status);
                return;
            }

            int[] pixels = new int[imageWidth];
            for (int y = 0; y < imageHeight; y++) {
                if (direction == FLIP_VERTICAL) {
                    // grab pixel values of the target line ...
                    int pos = (imageHeight - 1 - y) * imageWidth;
                    for (int kk = 0; kk < imageWidth; kk++)
                        pixels[kk] = raster[pos + kk];
                }
                else {
                    int pos = y * imageWidth;
                    for (int kk = 0; kk < imageWidth; kk++)
                        pixels[kk] = raster[pos + kk];

                    // swap the pixel values of the target line
                    int hw = imageWidth / 2;
                    for (int kk = 0; kk < hw; kk++) {
                        int tmp                     = pixels[kk];
                        pixels[kk]                  = pixels[imageWidth - kk - 1];
                        pixels[imageWidth - kk - 1] = tmp;
                    }
                }

                // consumer it ....
                consumer.setPixels(0, y, imageWidth, 1, ColorModel.getRGBdefault(), pixels, 0, imageWidth);
            } // (int y = 0; y < imageHeight; y++)

            // complete ?
            consumer.imageComplete(status);
        }
    } // private class FlipFilter extends ImageFilter

    /**
     * Apply general brightness/contrast algorithm. For details, visit
     * http://www.developerfusion.co.uk/
     *
     * The general algorithm is represented by: If Brighten = True New_Value =
     * Old_Value + Adjustment_Amount Else New_Value = Old_Value -
     * Adjustment_Amount If New_Value < Value_Minimum New_Value = Value_Minimum
     * If New_Value > Value_Maximum New_Value = Value_Maximum
     *
     * Contrast is a complicated operation. It is hard to formulate a
     * "general algorithm". Here is the closest representation
     * (Contrast_Value=[0, 2]):
     *
     * //Converts to a percent //[0, 1] New_Value = Old_Value / 255
     *
     * //Centers on 0 instead of .5 //[-.5, .5] New_Value -= 0.5
     *
     * //Adjusts by Contrast_Value //[-127.5, 127.5], usually [-1, 1] New_Value
     * *= Contrast_Value
     *
     * //Re-add .5 (un-center over 0) //[-127, 128] New_Value += 0.5
     *
     * //Re-multiply by 255 (un-convert to percent) //[-32385, 32640], usually
     * [0, 255] New_Value *= 255 //Clamp [0, 255] If(New_Value > 255) New_Value
     * = 255 If(New_Value < 0) New_Value = 0
     */
    private class BrightnessFilter extends RGBImageFilter {
        // brightness level = [-200, 200]
        int brightLevel = 0;

        // contrast level [0, 4]
        float contrastLevel = 0;

        public BrightnessFilter(int blevel, int clevel)
        {
            if (blevel < -100)
                brightLevel = -100;
            else if (blevel > 100)
                brightLevel = 100;
            else
                brightLevel = blevel;
            brightLevel *= 2;

            if (clevel < -100)
                clevel = -100;
            else if (clevel > 100)
                clevel = 100;

            if (clevel > 0)
                contrastLevel = (clevel / 100f + 1) * 2;
            else if (clevel < 0)
                contrastLevel = (clevel / 100f + 1) / 2;
            else
                contrastLevel = 0;

            canFilterIndexColorModel = true;
        }

        @Override
        public int filterRGB(int x, int y, int rgb)
        {
            // adjust brightness first, then adjust contrast
            // it gives more color depth

            if (brightLevel != 0) {
                int r = (rgb & 0x00ff0000) >> 16;
                int g = (rgb & 0x0000ff00) >> 8;
                int b = (rgb & 0x000000ff);

                r += brightLevel;
                g += brightLevel;
                b += brightLevel;

                if (r < 0)
                    r = 0;
                if (r > 255)
                    r = 255;
                if (g < 0)
                    g = 0;
                if (g > 255)
                    g = 255;
                if (b < 0)
                    b = 0;
                if (b > 255)
                    b = 255;

                r = (r << 16) & 0x00ff0000;
                g = (g << 8) & 0x0000ff00;
                b = b & 0x000000ff;

                rgb = ((rgb & 0xff000000) | r | g | b);
            }

            // do not compare float using !=0 or ==0
            if (contrastLevel > 0.000001) {
                int r = (rgb & 0x00ff0000) >> 16;
                int g = (rgb & 0x0000ff00) >> 8;
                int b = (rgb & 0x000000ff);

                float f = r / 255f;
                f -= 0.5;
                f *= contrastLevel;
                f += 0.5;
                f *= 255f;
                if (f < 0)
                    f = 0;
                if (f > 255)
                    f = 255;
                r = (int)f;

                f = g / 255f;
                f -= 0.5;
                f *= contrastLevel;
                f += 0.5;
                f *= 255f;
                if (f < 0)
                    f = 0;
                if (f > 255)
                    f = 255;
                g = (int)f;

                f = b / 255f;
                f -= 0.5;
                f *= contrastLevel;
                f += 0.5;
                f *= 255f;
                if (f < 0)
                    f = 0;
                if (f > 255)
                    f = 255;
                b = (int)f;

                r = (r << 16) & 0x00ff0000;
                g = (g << 8) & 0x0000ff00;
                b = b & 0x000000ff;

                rgb = ((rgb & 0xff000000) | r | g | b);
            }

            return rgb;
        }
    }

    /**
     * Makes an image filter for contour.
     */
    private class ContourFilter extends ImageFilter {
        // default color model
        private ColorModel defaultRGB;

        // contour level
        int level;

        // the table of the contour levels
        int[] levels;

        // colors for drawable contour line
        int[] levelColors;

        // default RGB

        // pixel value
        private int[] raster = null;

        // width & height
        private int imageWidth;
        private int imageHeight;

        /**
         * Create an contour filter for a given level contouring.
         *
         * @param theLevel
         *            the contour level.
         */
        private ContourFilter(int theLevel)
        {
            defaultRGB = ColorModel.getRGBdefault();

            levelColors    = new int[9];
            levelColors[0] = Color.red.getRGB();
            levelColors[1] = Color.green.getRGB();
            levelColors[2] = Color.blue.getRGB();
            levelColors[3] = Color.magenta.getRGB();
            levelColors[4] = Color.orange.getRGB();
            levelColors[5] = Color.cyan.getRGB();
            levelColors[6] = Color.black.getRGB();
            levelColors[7] = Color.pink.getRGB();
            levelColors[8] = Color.yellow.getRGB();

            if (theLevel < 1)
                theLevel = 1;
            else if (theLevel > 9)
                theLevel = 9;

            level  = theLevel;
            levels = new int[level];

            int dx = 128 / level;
            for (int i = 0; i < level; i++)
                levels[i] = (i + 1) * dx;
        }

        @Override
        public void setDimensions(int width, int height)
        {
            this.imageWidth  = width;
            this.imageHeight = height;

            // specify the raster
            if (raster == null)
                raster = new int[imageWidth * imageHeight];

            consumer.setDimensions(width, height);
        }

        @Override
        public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
                              int scansize)
        {
            int rgb    = 0;
            int srcoff = off;
            int dstoff = y * imageWidth + x;

            for (int yc = 0; yc < h; yc++) {
                for (int xc = 0; xc < w; xc++) {
                    rgb              = model.getRGB(pixels[srcoff++] & 0xff);
                    raster[dstoff++] = (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
                }
                srcoff += (scansize - w);
                dstoff += (imageWidth - w);
            }
        }

        @Override
        public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
                              int scansize)
        {
            int rgb    = 0;
            int srcoff = off;
            int dstoff = y * imageWidth + x;

            for (int yc = 0; yc < h; yc++) {
                for (int xc = 0; xc < w; xc++) {
                    rgb              = model.getRGB(pixels[srcoff++] & 0xff);
                    raster[dstoff++] = (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
                }

                srcoff += (scansize - w);
                dstoff += (imageWidth - w);
            }
        }

        @Override
        public void imageComplete(int status)
        {
            if ((status == IMAGEERROR) || (status == IMAGEABORTED)) {
                consumer.imageComplete(status);
                return;
            }

            int[] pixels = new int[imageWidth * imageHeight];
            for (int z = 0; z < levels.length; z++) {
                int currentLevel = levels[z];
                int color        = levelColors[z];

                setContourLine(raster, pixels, currentLevel, color, imageWidth, imageHeight);
            }

            int[] line = new int[imageWidth];
            for (int y = 0; y < imageHeight; y++) {
                for (int x = 0; x < imageWidth; x++)
                    line[x] = pixels[y * imageWidth + x];

                consumer.setPixels(0, y, imageWidth, 1, defaultRGB, line, 0, imageWidth);
            } // (int y = 0; y < imageHeight; y++)

            // complete ?
            consumer.imageComplete(status);
        }

        /**
         * draw a contour line based on the current parameter---level, color
         *
         * @param raster
         *            the data of the raster image.
         * @param pixels
         *            the pixel value of the image.
         * @param level
         *            the contour level.
         * @param color
         *            the color of the contour line.
         * @param w
         *            the width of the image.
         * @param h
         *            the height of the image.
         */
        private void setContourLine(int[] raster, int[] pixels, int level, int color, int w, int h)
        {
            int p = 0;               // entrance point
            int q = p + (w * h - 1); // bottom right point
            int u = 0 + (w - 1);     // top right point

            // first round
            while (true) {
                while (p < u) {
                    int rgb = raster[p];
                    if (rgb < level) {
                        while ((raster[p] < level) && (p < u))
                            p++;
                        if (raster[p] >= level)
                            pixels[p] = color;
                    }
                    else if (rgb == level) {
                        while ((raster[p] == level) && (p < u))
                            p++;
                        if ((raster[p] < level) || (raster[p] > level))
                            pixels[p] = color;
                    }
                    else {
                        while ((raster[p] > level) && (p < u))
                            p++;
                        if ((raster[p] <= level))
                            pixels[p] = color;
                    }
                }

                if (u == q)
                    break;
                else {
                    u += w;
                    p++;
                }
            }
        }

    } // private class ContourFilter extends ImageFilter

    /**
     * Makes an image filter for rotating image by 90 degrees.
     */
    public static class Rotate90Filter extends ImageFilter {
        private ColorModel defaultRGB = ColorModel.getRGBdefault();

        private double[] coord = new double[2];

        private int[] raster;
        private int xoffset;
        private int yoffset;
        private int srcW;
        private int srcH;
        private int dstW;
        private int dstH;
        private int direction;

        /**
         * Image filter for rotating image by 90 degrees.
         *
         * @param dir
         *        the direction to rotate the image
         *        ROTATE_CW_90 or ROTATE_CCW_90
         */
        public Rotate90Filter(int dir) { direction = dir; }

        /**
         * Transform when rotating image by 90 degrees.
         *
         * @param x
         *        the x coordinate to transform
         * @param y
         *        the y coordinate to transform
         * @param retcoord
         *        the x.y coordinate transformed
         */
        public void transform(double x, double y, double[] retcoord)
        {
            if (direction == ROTATE_CW_90) {
                retcoord[0] = -y;
                retcoord[1] = x;
            }
            else {
                retcoord[0] = y;
                retcoord[1] = -x;
            }
        }

        /**
         * Transform when rotating image by 90 degrees.
         *
         * @param x
         *        the x coordinate to transform
         * @param y
         *        the y coordinate to transform
         * @param retcoord
         *        the x.y coordinate transformed
         */
        public void itransform(double x, double y, double[] retcoord)
        {
            if (direction == ROTATE_CCW_90) {
                retcoord[0] = -y;
                retcoord[1] = x;
            }
            else {
                retcoord[0] = y;
                retcoord[1] = -x;
            }
        }

        /**
         * Transform the image specified by a rectangle.
         *
         * @param rect
         *        the rectangle coordinates transformed
         */
        public void transformBBox(Rectangle rect)
        {
            double minx = Double.POSITIVE_INFINITY;
            double miny = Double.POSITIVE_INFINITY;
            double maxx = Double.NEGATIVE_INFINITY;
            double maxy = Double.NEGATIVE_INFINITY;
            for (int y = 0; y <= 1; y++) {
                for (int x = 0; x <= 1; x++) {
                    transform((double)rect.x + x * rect.width, (double)rect.y + y * rect.height, coord);
                    minx = Math.min(minx, coord[0]);
                    miny = Math.min(miny, coord[1]);
                    maxx = Math.max(maxx, coord[0]);
                    maxy = Math.max(maxy, coord[1]);
                }
            }
            rect.x      = (int)Math.floor(minx);
            rect.y      = (int)Math.floor(miny);
            rect.width  = (int)Math.ceil(maxx) - rect.x;
            rect.height = (int)Math.ceil(maxy) - rect.y;
        }

        @Override
        public void setDimensions(int width, int height)
        {
            Rectangle rect = new Rectangle(0, 0, width, height);
            transformBBox(rect);
            xoffset = -rect.x;
            yoffset = -rect.y;
            srcW    = width;
            srcH    = height;
            dstW    = rect.width;
            dstH    = rect.height;
            raster  = new int[srcW * srcH];
            consumer.setDimensions(dstW, dstH);
        }

        @Override
        public void setProperties(Hashtable props)
        {
            props    = (Hashtable)props.clone();
            Object o = props.get("filters");
            if (o == null)
                props.put("filters", toString());
            else if (o instanceof String)
                props.put("filters", ((String)o) + toString());
            consumer.setProperties(props);
        }

        @Override
        public void setColorModel(ColorModel model)
        {
            consumer.setColorModel(defaultRGB);
        }

        @Override
        public void setHints(int hintflags)
        {
            consumer.setHints(TOPDOWNLEFTRIGHT | COMPLETESCANLINES | SINGLEPASS | (hintflags & SINGLEFRAME));
        }

        @Override
        public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
                              int scansize)
        {
            int srcoff = off;
            int dstoff = y * srcW + x;
            for (int yc = 0; yc < h; yc++) {
                for (int xc = 0; xc < w; xc++)
                    raster[dstoff++] = model.getRGB(pixels[srcoff++] & 0xff);
                srcoff += (scansize - w);
                dstoff += (srcW - w);
            }
        }

        @Override
        public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
                              int scansize)
        {
            int srcoff = off;
            int dstoff = y * srcW + x;
            if (model == defaultRGB) {
                for (int yc = 0; yc < h; yc++) {
                    System.arraycopy(pixels, srcoff, raster, dstoff, w);
                    srcoff += scansize;
                    dstoff += srcW;
                }
            }
            else {
                for (int yc = 0; yc < h; yc++) {
                    for (int xc = 0; xc < w; xc++)
                        raster[dstoff++] = model.getRGB(pixels[srcoff++]);
                    srcoff += (scansize - w);
                    dstoff += (srcW - w);
                }
            }
        }

        @Override
        public void imageComplete(int status)
        {
            if ((status == IMAGEERROR) || (status == IMAGEABORTED)) {
                consumer.imageComplete(status);
                return;
            }
            int[] pixels = new int[dstW];
            for (int dy = 0; dy < dstH; dy++) {
                itransform(0 - (double)xoffset, dy - (double)yoffset, coord);
                double x1 = coord[0];
                double y1 = coord[1];
                itransform(dstW - (double)xoffset, dy - (double)yoffset, coord);
                double x2   = coord[0];
                double y2   = coord[1];
                double xinc = (x2 - x1) / dstW;
                double yinc = (y2 - y1) / dstW;
                for (int dx = 0; dx < dstW; dx++) {
                    int sx = (int)Math.round(x1);
                    int sy = (int)Math.round(y1);
                    if ((sx < 0) || (sy < 0) || (sx >= srcW) || (sy >= srcH))
                        pixels[dx] = 0;
                    else
                        pixels[dx] = raster[sy * srcW + sx];
                    x1 += xinc;
                    y1 += yinc;
                }
                consumer.setPixels(0, dy, dstW, 1, defaultRGB, pixels, 0, dstW);
            }
            consumer.imageComplete(status);
        }
    } // private class RotateFilter

    /**
     * Makes animation for 3D images.
     */
    private class Animation extends Dialog {
        private static final int MAX_ANIMATION_IMAGE_SIZE = 300;

        /* A list of frames to display for animation */
        private org.eclipse.swt.graphics.Image[] frames = null;

        private Shell shell;
        private Canvas canvas; // Canvas to draw the image
        private int numberOfImages = 0;
        private int currentFrame   = 0;
        private int sleepTime      = 200;

        public Animation(Shell parent, int style, ScalarDS dataset)
        {
            super(parent, style);

            long[] dims         = dataset.getDims();
            long[] stride       = dataset.getStride();
            long[] start        = dataset.getStartDims();
            long[] selected     = dataset.getSelectedDims();
            int[] selectedIndex = dataset.getSelectedIndex();
            int rank            = dataset.getRank();
            if (animationSpeed != 0)
                sleepTime = 1000 / animationSpeed;

            // back up the start and selected size
            long[] tstart    = new long[rank];
            long[] tselected = new long[rank];
            long[] tstride   = new long[rank];
            System.arraycopy(start, 0, tstart, 0, rank);
            System.arraycopy(selected, 0, tselected, 0, rank);
            System.arraycopy(stride, 0, tstride, 0, rank);

            int strideN = 1;
            int maxSize = (int)Math.max(selected[selectedIndex[0]], selected[selectedIndex[1]]);
            if (maxSize > MAX_ANIMATION_IMAGE_SIZE)
                strideN = (int)((double)maxSize / (double)MAX_ANIMATION_IMAGE_SIZE + 0.5);

            start[selectedIndex[0]]    = 0;
            start[selectedIndex[1]]    = 0;
            start[selectedIndex[2]]    = 0;
            selected[selectedIndex[0]] = dims[selectedIndex[0]] / strideN;
            selected[selectedIndex[1]] = dims[selectedIndex[1]] / strideN;
            selected[selectedIndex[2]] = 1;
            stride[selectedIndex[0]]   = strideN;
            stride[selectedIndex[1]]   = strideN;
            stride[selectedIndex[2]]   = 1;

            Object data3d   = null;
            byte[] byteData = null;
            int h           = (int)selected[selectedIndex[0]];
            int w           = (int)selected[selectedIndex[1]];
            int size        = w * h;

            numberOfImages = (int)dims[selectedIndex[2]];
            frames         = new org.eclipse.swt.graphics.Image[numberOfImages];

            BufferedImage frameImage;
            try {
                for (int i = 0; i < numberOfImages; i++) {
                    start[selectedIndex[2]] = i;

                    dataset.clearData();
                    try {
                        data3d = dataset.read();
                    }
                    catch (Exception err) {
                        continue;
                    }

                    byteData = new byte[size];

                    byteData = Tools.getBytes(data3d, dataRange, w, h, false,
                                              dataset.getFilteredImageValues(), true, byteData);

                    frameImage = (BufferedImage)createIndexedImage(byteData, imagePalette, w, h);
                    frames[i]  = convertBufferedImageToSWTImage(frameImage);
                }
            }
            finally {
                // set back to original state
                System.arraycopy(tstart, 0, start, 0, rank);
                System.arraycopy(tselected, 0, selected, 0, rank);
                System.arraycopy(tstride, 0, stride, 0, rank);
            }
        }

        public void open()
        {
            Shell parent = getParent();
            shell        = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
            shell.setFont(curFont);
            shell.setText("Animation - " + dataset.getName());
            shell.setImages(ViewProperties.getHdfIcons());
            shell.setLayout(new GridLayout(1, true));

            canvas = new Canvas(shell, SWT.DOUBLE_BUFFERED);
            canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            canvas.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e)
                {
                    GC gc = e.gc;

                    if (frames == null)
                        return;

                    org.eclipse.swt.graphics.Rectangle canvasBounds = canvas.getBounds();
                    int x = ((canvasBounds.width / 2) - (frames[currentFrame].getBounds().width / 2));
                    int y = ((canvasBounds.height / 2) - (frames[currentFrame].getBounds().height / 2));
                    gc.drawImage(frames[currentFrame], x, y);

                    gc.dispose();
                }
            });

            canvas.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent arg0)
                {
                    /* Make sure to dispose of all generated images */
                    for (int i = 0; i < frames.length; i++) {
                        if (frames[i] != null && !frames[i].isDisposed())
                            frames[i].dispose();
                    }
                }
            });

            Button closeButton = new Button(shell, SWT.PUSH);
            closeButton.setFont(curFont);
            closeButton.setText("&Close");
            closeButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
            closeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    shell.dispose();
                }
            });

            shell.pack();

            shell.setSize(MAX_ANIMATION_IMAGE_SIZE, MAX_ANIMATION_IMAGE_SIZE);

            org.eclipse.swt.graphics.Rectangle parentBounds = parent.getBounds();
            Point shellSize                                 = shell.getSize();
            shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                              (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            shell.open();

            Runnable runnable = new AnimationThread();

            /**
             * Run the animation. This method is called by class Thread.
             *
             * @see java.lang.Thread
             */
            Display.getDefault().timerExec(sleepTime, runnable);

            Display openDisplay = parent.getDisplay();
            while (!shell.isDisposed()) {
                if (!openDisplay.readAndDispatch())
                    openDisplay.sleep();
            }

            openDisplay.timerExec(-1, runnable);
        }

        private class AnimationThread implements Runnable {
            @Override
            public void run()
            {
                if ((frames == null) || (canvas == null))
                    return;

                if (++currentFrame >= numberOfImages)
                    currentFrame = 0;

                canvas.redraw();

                Display.getCurrent().timerExec(sleepTime, this);
            }
        }
    }

    private class DataRangeDialog extends Dialog {
        private Shell shell;
        private Slider minSlider;
        private Slider maxSlider;
        private Text minField;
        private Text maxField;
        final int nTICKS            = 10;
        double tickRatio            = 1;
        final int rangeW            = 500;
        final int rangeH            = 400;
        double[] rangeMinMaxCurrent = {0, 0};
        double min;
        double max;
        double minOrg;
        double maxOrg;
        final double[] minmaxPrevious = {0, 0};
        final double[] minmaxDist     = {0, 0};

        final DecimalFormat numberFormat = new DecimalFormat("#.##E0");

        public DataRangeDialog(Shell parent, int style, double[] minmaxCurrent, double[] minmaxOriginal,
                               final int[] dataDist)
        {

            super(parent, style);

            Tools.findMinMax(dataDist, minmaxDist, null);

            if ((minmaxOriginal == null) || (minmaxOriginal.length <= 1)) {
                minmaxCurrent[0] = 0;
                minmaxCurrent[1] = 255;
            }
            else {
                if (minmaxOriginal[0] == minmaxOriginal[1])
                    Tools.findMinMax(data, minmaxOriginal, dataset.getFillValue());

                minmaxCurrent[0] = minmaxOriginal[0];
                minmaxCurrent[1] = minmaxOriginal[1];
            }

            minmaxPrevious[0] = min = minmaxCurrent[0];
            minmaxPrevious[1] = max = minmaxCurrent[1];
            minOrg                  = originalRange[0];
            maxOrg                  = originalRange[1];

            tickRatio = (maxOrg - minOrg) / nTICKS;
        }

        public void open()
        {
            Shell parent = getParent();
            shell        = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
            shell.setFont(curFont);
            shell.setText("Image Value Range");
            shell.setImages(ViewProperties.getHdfIcons());
            shell.setLayout(new GridLayout(1, true));

            Canvas chartCanvas  = new Canvas(shell, SWT.DOUBLE_BUFFERED);
            GridData gridData   = new GridData(SWT.FILL, SWT.FILL, true, true);
            gridData.widthHint  = 400;
            gridData.heightHint = 150;
            chartCanvas.setLayoutData(gridData);

            final int numberOfPoints = dataDist.length;
            int gap                  = 5;
            final int xgap           = 2 * gap;
            final double xmin        = originalRange[0];
            final double xmax        = originalRange[1];

            chartCanvas.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e)
                {
                    GC gc = e.gc;

                    gc.setFont(curFont);

                    int h        = rangeH / 3 - 50;
                    int w        = rangeW;
                    int xnpoints = Math.min(10, numberOfPoints - 1);

                    // draw the X axis
                    gc.drawLine(xgap, h, w + xgap, h);

                    // draw x labels
                    double xp = 0;
                    double x;
                    double dw = (double)w / (double)xnpoints;
                    double dx = (xmax - xmin) / xnpoints;
                    for (int i = 0; i <= xnpoints; i++) {
                        x  = xmin + i * dx;
                        xp = xgap + i * dw;
                        gc.drawLine((int)xp, h, (int)xp, h - 5);
                        gc.drawString(numberFormat.format(x), (int)xp - 5, h + 20);
                    }

                    org.eclipse.swt.graphics.Color c = gc.getBackground();
                    double yp                        = 0;
                    double ymin                      = minmaxDist[0];
                    double dy                        = minmaxDist[1] - minmaxDist[0];
                    if (dy <= 0)
                        dy = 1;

                    xp = xgap;

                    int barWidth = w / numberOfPoints;
                    if (barWidth <= 0)
                        barWidth = 1;
                    dw = (double)w / (double)numberOfPoints;

                    gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

                    for (int j = 0; j < numberOfPoints; j++) {
                        xp = xgap + j * dw;
                        yp = (int)(h * (dataDist[j] - ymin) / dy);

                        gc.fillRectangle((int)xp, (int)(h - yp), barWidth, (int)yp);
                    }

                    gc.setBackground(c); // set the color back to its default
                }
            });

            org.eclipse.swt.widgets.Group lowerBoundGroup =
                new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
            lowerBoundGroup.setFont(curFont);
            lowerBoundGroup.setText("Lower Bound");
            lowerBoundGroup.setLayout(new GridLayout(1, true));
            lowerBoundGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            minField = new Text(lowerBoundGroup, SWT.SINGLE | SWT.BORDER);
            minField.setFont(curFont);
            minField.setText(String.valueOf(min));
            minField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            minField.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    if (minSlider != null && minSlider.getEnabled()) {
                        double value = Double.parseDouble(((Text)e.widget).getText());

                        if (value > maxOrg) {
                            value = maxOrg;
                            minField.setText(String.valueOf(value));
                        }

                        minSlider.setSelection((int)((value - minOrg) / tickRatio));
                    }
                }
            });

            minSlider = new Slider(lowerBoundGroup, SWT.HORIZONTAL);
            minSlider.setMinimum(0);
            minSlider.setMaximum(nTICKS);
            minSlider.setIncrement(1);
            minSlider.setThumb(1);
            minSlider.setSelection(0);
            minSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            minSlider.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    double value    = minSlider.getSelection();
                    double maxValue = maxSlider.getSelection();
                    if (value > maxValue) {
                        value = maxValue;
                        minSlider.setSelection((int)value);
                    }

                    minField.setText(String.valueOf(value * tickRatio + minOrg));
                }
            });

            org.eclipse.swt.widgets.Group upperBoundGroup =
                new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
            upperBoundGroup.setFont(curFont);
            upperBoundGroup.setText("Upper Bound");
            upperBoundGroup.setLayout(new GridLayout(1, true));
            upperBoundGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            maxField = new Text(upperBoundGroup, SWT.SINGLE | SWT.BORDER);
            maxField.setFont(curFont);
            maxField.setText(String.valueOf(max));
            maxField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            maxField.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    if (maxSlider != null && maxSlider.getEnabled()) {
                        double value = Double.parseDouble(((Text)e.widget).getText());

                        if (value < minOrg) {
                            value = minOrg;
                            maxField.setText(String.valueOf(value));
                        }

                        maxSlider.setSelection((int)((value - minOrg) / tickRatio));
                    }
                }
            });

            maxSlider = new Slider(upperBoundGroup, SWT.HORIZONTAL);
            maxSlider.setMinimum(0);
            maxSlider.setMaximum(nTICKS);
            maxSlider.setIncrement(1);
            maxSlider.setThumb(1);
            maxSlider.setSelection(nTICKS);
            maxSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            maxSlider.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    double value    = maxSlider.getSelection();
                    double minValue = minSlider.getSelection();
                    if (value < minValue) {
                        value = minValue;
                        maxSlider.setSelection((int)value);
                    }

                    maxField.setText(String.valueOf(value * tickRatio + minOrg));
                }
            });

            // Create Ok/Cancel/Apply button region
            Composite buttonComposite = new Composite(shell, SWT.NONE);
            buttonComposite.setLayout(new GridLayout(3, false));
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    rangeMinMaxCurrent[0] = Double.valueOf(minField.getText());
                    rangeMinMaxCurrent[1] = Double.valueOf(maxField.getText());

                    shell.dispose();
                }
            });

            Button cancelButton = new Button(buttonComposite, SWT.PUSH);
            cancelButton.setFont(curFont);
            cancelButton.setText(" &Cancel ");
            cancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
            cancelButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    rangeMinMaxCurrent[0] = minmaxPrevious[0];
                    rangeMinMaxCurrent[1] = minmaxPrevious[1];

                    applyDataRange(minmaxPrevious);

                    shell.dispose();
                }
            });

            Button applyButton = new Button(buttonComposite, SWT.PUSH);
            applyButton.setFont(curFont);
            applyButton.setText("&Apply");
            applyButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            applyButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    minmaxPrevious[0] = rangeMinMaxCurrent[0];
                    minmaxPrevious[1] = rangeMinMaxCurrent[1];

                    rangeMinMaxCurrent[0] = Double.valueOf(minField.getText());
                    rangeMinMaxCurrent[1] = Double.valueOf(maxField.getText());

                    applyDataRange(rangeMinMaxCurrent);
                    rangeMinMaxCurrent[0] = rangeMinMaxCurrent[1] = 0;
                }
            });

            if (min == max) {
                minSlider.setEnabled(false);
                maxSlider.setEnabled(false);
            }

            shell.pack();

            shell.setSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

            org.eclipse.swt.graphics.Rectangle parentBounds = parent.getBounds();
            Point shellSize                                 = shell.getSize();
            shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                              (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            shell.open();

            Display openDisplay = parent.getDisplay();
            while (!shell.isDisposed()) {
                if (!openDisplay.readAndDispatch())
                    openDisplay.sleep();
            }
        }

        public double[] getRange() { return rangeMinMaxCurrent; }
    }

    private class ContrastSlider extends Dialog {
        private Shell shell;
        private Scale brightSlider;
        private Scale cntrastSlider;
        private Text brightField;
        private Text contrastField;
        private String bLabel = "Brightness";
        private String cLabel = "Contrast";

        ImageProducer imageProducer;
        double[] autoGainBias = {0, 0};
        int bLevel            = 0;
        int cLevel            = 0;

        public ContrastSlider(Shell parent, int style, ImageProducer producer)
        {
            super(parent, style);

            imageProducer = producer;
        }

        public void open()
        {
            Shell parent = getParent();
            shell        = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
            shell.setFont(curFont);
            shell.setText("Brightness/Contrast");
            shell.setImages(ViewProperties.getHdfIcons());
            shell.setLayout(new GridLayout(1, true));

            if (doAutoGainContrast && gainBias != null) {
                bLabel = "Bias";
                cLabel = "Gain";
                shell.setText(bLabel + "/" + cLabel);
            }

            org.eclipse.swt.widgets.Group brightnessGroup =
                new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
            brightnessGroup.setFont(curFont);
            brightnessGroup.setText(bLabel + " %");
            brightnessGroup.setLayout(new GridLayout(1, true));
            brightnessGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            brightField = new Text(brightnessGroup, SWT.SINGLE | SWT.BORDER);
            brightField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            brightField.setFont(curFont);
            brightField.setText(String.valueOf(bLevel));
            brightField.addListener(SWT.Traverse, new Listener() {
                @Override
                public void handleEvent(Event e)
                {
                    if (e.detail == SWT.TRAVERSE_RETURN) {
                        if (brightSlider != null) {
                            double value = Double.parseDouble(((Text)e.widget).getText());

                            if (value > 100)
                                value = 100;
                            else if (value < -100)
                                value = -100;

                            brightSlider.setSelection((int)value + 100);
                        }
                    }
                }
            });

            brightSlider = new Scale(brightnessGroup, SWT.HORIZONTAL);
            brightSlider.setMinimum(0);
            brightSlider.setMaximum(200);
            brightSlider.setIncrement(1);
            brightSlider.setSelection(bLevel + 100);
            brightSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            brightSlider.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    int value = ((Scale)e.widget).getSelection();
                    brightField.setText(String.valueOf(value - 100));
                }
            });

            org.eclipse.swt.widgets.Group contrastGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
            contrastGroup.setFont(curFont);
            contrastGroup.setText(cLabel + " %");
            contrastGroup.setLayout(new GridLayout(1, true));
            contrastGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            contrastField = new Text(contrastGroup, SWT.SINGLE | SWT.BORDER);
            contrastField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            contrastField.setFont(curFont);
            contrastField.setText(String.valueOf(cLevel));
            contrastField.addListener(SWT.Traverse, new Listener() {
                @Override
                public void handleEvent(Event e)
                {
                    if (e.detail == SWT.TRAVERSE_RETURN) {
                        if (cntrastSlider != null) {
                            double value = Double.parseDouble(((Text)e.widget).getText());

                            if (value > 100)
                                value = 100;
                            else if (value < -100)
                                value = -100;

                            cntrastSlider.setSelection((int)value + 100);
                        }
                    }
                }
            });

            cntrastSlider = new Scale(contrastGroup, SWT.HORIZONTAL);
            cntrastSlider.setMinimum(0);
            cntrastSlider.setMaximum(200);
            cntrastSlider.setIncrement(1);
            cntrastSlider.setSelection(cLevel + 100);
            cntrastSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            cntrastSlider.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    int value = ((Scale)e.widget).getSelection();
                    contrastField.setText(String.valueOf(value - 100));
                }
            });

            // Create Ok/Cancel/Apply button region
            Composite buttonComposite = new Composite(shell, SWT.NONE);
            buttonComposite.setLayout(new GridLayout(3, false));
            buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            Button okButton = new Button(buttonComposite, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    int b = Integer.parseInt(brightField.getText());
                    int c = Integer.parseInt(contrastField.getText());

                    applyBrightContrast(b, c);

                    bLevel = b;
                    cLevel = c;

                    shell.dispose();
                }
            });

            Button cancelButton = new Button(buttonComposite, SWT.PUSH);
            cancelButton.setFont(curFont);
            cancelButton.setText(" &Cancel ");
            cancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
            cancelButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    applyBrightContrast(bLevel, cLevel);
                    shell.dispose();
                }
            });

            Button applyButton = new Button(buttonComposite, SWT.PUSH);
            applyButton.setFont(curFont);
            applyButton.setText("&Apply");
            applyButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            applyButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    int b = Integer.parseInt(brightField.getText());
                    int c = Integer.parseInt(contrastField.getText());

                    applyBrightContrast(b, c);
                }
            });

            shell.pack();

            shell.setSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

            org.eclipse.swt.graphics.Rectangle parentBounds = parent.getBounds();
            Point shellSize                                 = shell.getSize();
            shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                              (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            shell.open();

            Display openDisplay = parent.getDisplay();
            while (!shell.isDisposed()) {
                if (!openDisplay.readAndDispatch())
                    openDisplay.sleep();
            }
        }

        private void applyBrightContrast(int blevel, int clevel)
        {
            // separate autogain and simple contrast process
            if (doAutoGainContrast && gainBias != null) {
                autoGainBias[0] = gainBias[0] * (1 + (clevel) / 100.0);
                autoGainBias[1] = gainBias[1] * (1 + (blevel) / 100.0);
                applyAutoGain(autoGainBias, null);
            }
            else {
                ImageFilter filter = new BrightnessFilter(blevel, clevel);
                image              = Tools.toBufferedImage(
                                 toolkit.createImage(new FilteredImageSource(imageProducer, filter)));
                imageComponent.setImage(image);
                zoomTo(zoomFactor);
            }
        }
    }
}
