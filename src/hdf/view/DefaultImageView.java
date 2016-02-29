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

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;

import java.awt.image.ImageFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ColorModel;
import java.awt.Color;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.ViewProperties.BITMASK_OP;

/**
 * ImageView displays an HDF dataset as an image.
 * <p>
 * A scalar dataset in HDF can be displayed in image or table. By default, an
 * HDF4 GR image and HDF5 image is displayed as an image. Other scalar datasets
 * are display in a two-dimensional table.
 * <p>
 * Users can also choose to display a scalar dataset as image. Currently verion
 * of the ImageView only supports 8-bit raster image with indexed RGB color
 * model of 256 colors or 24-bit true color raster image. Data of other type
 * will be converted to 8-bit integer. The simple linear conversion is used for
 * this purpose:
 * 
 * <pre>
 * y = f * (x - min),
 *       where y   = the value of 8-bit integer,
 *             x   = the value of original data,
 *             f   = 255/(max-min), conversion factor,
 *             max = the maximum of the original data,
 *             min = the minimum of the original data.
 * </pre>
 * <p>
 * A default color table is provided for images without palette attached to it.
 * Current choice of default palettes include Gray, Rainbow, Nature and Wave.
 * For more infomation on palette, read <a
 * href="http://hdfgroup.org/HDF5/doc/ADGuide/ImageSpec.html"> HDF5 Image and
 * Palette Specification </a>
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DefaultImageView implements ImageView {
	private static final long serialVersionUID = -6534336542813587242L;

	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultImageView.class);
	
	private final Display display = Display.getCurrent();
	private final Shell shell;

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
	private final ViewManager viewer;

	/**
	 * The Scalar Dataset.
	 */
	private ScalarDS dataset;

	/**
	 * The JComponent containing the image.
	 */
	private ImageComponent imageComponent;

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

	/** Flag to indicate if the image is plane interleaved */
	private boolean isPlaneInterlace;

	private boolean isHorizontalFlipped = false;

	private boolean isVerticalFlipped = false;

	private int rotateCount = 0;

	/** the number type of the image data */
	private char NT;

	/** the raw data of the image */
	private Object data;

	/** flag to indicate if the original data type is unsigned integer */
	private boolean isUnsigned;

	private boolean isUnsignedConverted = false;

	private double[] dataRange;
	private final double[] originalRange = {0,0};

	//private PaletteComponent paletteComponent;

	private int animationSpeed = 2;

	private List rotateRelatedItems;

	private ScrolledComposite imageScroller;

	private Text frameField;

	private long curFrame = 0, maxFrame = 1;

	//private BufferedImage bufferedImage;

	//    private AutoContrastSlider autoContrastSlider;

	private ContrastSlider contrastSlider;

	private int indexBase = 0;
	private int[] dataDist = null;

	/**
	 * equates to brightness
	 */
	private boolean doAutoGainContrast = false;
	private double[] gainBias, gainBias_current;

	/**
	 * int array to hold unsigned short or signed int data from applying the
	 * autogain
	 */
	private Object autoGainData;

	private BitSet bitmask;
	private boolean convertByteData = false;
	private BITMASK_OP bitmaskOP = BITMASK_OP.EXTRACT;

	private enum Origin { UPPER_LEFT, LOWER_LEFT, UPPER_RIGHT, LOWER_RIGHT }
	private Origin imageOrigin = null;

	private List<Integer> invalidValueIndex;

	/**
	 * Constructs an ImageView.
	 * <p>
	 * 
	 * @param theView
	 *            the main HDFView.
	 */
	public DefaultImageView(Shell parent, ViewManager theView) {
		this(parent, theView, null);
	}

	/**
	 * Constructs an ImageView.
	 * <p>
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
	public DefaultImageView(Shell parent, ViewManager theView, HashMap map) {
		shell = new Shell(display);
		shell.setImage(ViewProperties.getImageIcon());

		viewer = theView;
		zoomFactor = 1.0f;
		imageByteData = null;
		imagePalette = null;
		//paletteComponent = null;
		isTrueColor = false;
		is3D = false;
		isPlaneInterlace = false;
		isUnsigned = false;
		data = null;
		NT = 0;
		rotateRelatedItems = new Vector(10);
		imageScroller = null;
		gainBias = null;
		gainBias_current = null;
		autoGainData = null;
		contrastSlider = null;
		//bitmask = null;
		invalidValueIndex = new ArrayList<Integer>();

		String origStr = ViewProperties.getImageOrigin();
		if (ViewProperties.ORIGIN_LL.equalsIgnoreCase(origStr))
			imageOrigin = Origin.LOWER_LEFT;
		else if (ViewProperties.ORIGIN_UR.equalsIgnoreCase(origStr))
			imageOrigin = Origin.UPPER_RIGHT;
		else if (ViewProperties.ORIGIN_LR.equalsIgnoreCase(origStr))
			imageOrigin = Origin.LOWER_RIGHT;

		if (ViewProperties.isIndexBase1())
			indexBase = 1;

		HObject hobject = null;

		if (map != null) {
			hobject = (HObject) map.get(ViewProperties.DATA_VIEW_KEY.OBJECT);
			//bitmask = (BitSet) map.get(ViewProperties.DATA_VIEW_KEY.BITMASK);
			bitmaskOP = (BITMASK_OP)map.get(ViewProperties.DATA_VIEW_KEY.BITMASKOP);

			Boolean b = (Boolean) map.get(ViewProperties.DATA_VIEW_KEY.CONVERTBYTE);
			if (b != null)
				convertByteData = b.booleanValue();

			b = (Boolean) map.get(ViewProperties.DATA_VIEW_KEY.INDEXBASE1);
			if (b != null) {
				if (b.booleanValue())
					indexBase = 1;
				else
					indexBase = 0;
			}            
		}

		if (hobject == null)
			hobject = (HObject) theView.getTreeView().getCurrentObject();

		if ((hobject == null) || !(hobject instanceof ScalarDS)) {
			viewer.showStatus("Display data in image failed for - " + hobject);
			return;
		}

		dataset = (ScalarDS) hobject;
		dataRange = dataset.getImageDataRange();
		if (dataRange == null) {
			dataRange = new double[2];
			dataRange[0] = dataRange[1] = 0;
			if (dataset.getDatatype().getDatatypeSize() == 1
					&& !convertByteData) {
				dataRange[1] = 255; // byte image data rang = [0, 255]
			}
		}
		else {
			if (dataRange[0] < dataRange[1])
				convertByteData = true;
		}

		//JPanel contentPane = (JPanel) getContentPane();
		//contentPane.setName("imagecontentpane");
		//contentPane.setLayout(new BorderLayout());

		// add the text field to display pixel data
		//contentPane.add(valueField = new JTextField(), BorderLayout.SOUTH);
		//valueField.setName("valuefield");
		valueField.setEditable(false);
		valueField.setVisible(ViewProperties.showImageValues());

		if (image == null) {
			getImage();
		}

		if (image == null) {
			viewer.showStatus("Loading image failed - " + dataset.getName());
			dataset = null;
			return;
		}

		originalRange[0] = dataRange[0];
		originalRange[1] = dataRange[1];

		//imageComponent = new ImageComponent(image);
		//ScrolledComposite scroller = new ScrolledComposite(imageComponent);
		//scroller.getVerticalBar().setIncrement(50);
		//scroller.getHorizontalBar().setIncrement(50);
		//imageScroller = scroller;
		//contentPane.add(scroller, BorderLayout.CENTER);

		// add palette canvas to show the palette
		//if (imagePalette != null) {
		//	paletteComponent = new PaletteComponent(imagePalette, dataRange);
		//	contentPane.add(paletteComponent, BorderLayout.EAST);
		//}

		if (imageOrigin == Origin.LOWER_LEFT)
			flip(FLIP_VERTICAL);
		else if (imageOrigin == Origin.UPPER_RIGHT)
			flip(FLIP_HORIZONTAL);
		if (imageOrigin == Origin.LOWER_RIGHT) {
			rotate(ROTATE_CW_90);
			rotate(ROTATE_CW_90);
		}

		// set title
		StringBuffer sb = new StringBuffer(hobject.getName());
		sb.append("  at  ");
		sb.append(hobject.getPath());
		sb.append("  [");
		sb.append(dataset.getFileFormat().getName());
		sb.append("  in  ");
		sb.append(dataset.getFileFormat().getParent());
		sb.append("]");

		shell.setText(sb.toString());

		// setup subset information
		int rank = dataset.getRank();
		int[] selectedIndex = dataset.getSelectedIndex();
		long[] count = dataset.getSelectedDims();
		long[] stride = dataset.getStride();
		long[] dims = dataset.getDims();
		long[] start = dataset.getStartDims();
		int n = Math.min(3, rank);

		if (rank > 2) {
			curFrame = start[selectedIndex[2]]+indexBase;
			maxFrame = dims[selectedIndex[2]];
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

		shell.setMenuBar(createMenuBar());
		viewer.showStatus(sb.toString());

		//int titleJustification = TitledBorder.LEFT;
		//int titlePosition = TitledBorder.TOP;
		//String orgin = ViewProperties.getImageOrigin();
		//if (orgin.equalsIgnoreCase(ViewProperties.ORIGIN_UR))
			//titleJustification = TitledBorder.RIGHT;
		//else if (orgin.equalsIgnoreCase(ViewProperties.ORIGIN_LL))
			//titlePosition = TitledBorder.BOTTOM;
		//else if (orgin.equalsIgnoreCase(ViewProperties.ORIGIN_LR)) {
			//titleJustification = TitledBorder.RIGHT;
			//titlePosition = TitledBorder.BOTTOM;
		//}

		String originTag = "(0,0)";
		if (ViewProperties.isIndexBase1())
			originTag = "(1,1)";

		//Border border = BorderFactory.createCompoundBorder(
		//		BorderFactory.createRaisedBevelBorder(), BorderFactory
		//		.createTitledBorder(BorderFactory
		//				.createLineBorder(Color.lightGray, 1),
		//				originTag,
		//				titleJustification, titlePosition,
		//				this.getFont(), Color.black));
		//contentPane.setBorder(border);

		//        if (imageComponent.getParent() !=null)
		//        	imageComponent.getParent().setBackground(Color.black);
		
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				// reload the data when it is displayed next time
				// because the display type (table or image) may be
				// different.
				if (!dataset.isImage()) {
					dataset.clearData();
				}

				data = null;
				image = null;
				imageByteData = null;
				imageComponent = null;
				autoGainData = null;
				((Vector) rotateRelatedItems).setSize(0);
				System.runFinalization();
				System.gc();

				//viewer.removeDataView(this);
			}
		});
		
		shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		shell.open();
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	private Menu createMenuBar() {
		Menu menuBar = new Menu(shell, SWT.BAR);

		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("Save Image As");

		Menu saveAsMenu = new Menu(item);
		item.setMenu(saveAsMenu);

		item = new MenuItem(saveAsMenu, SWT.NONE);
		item.setText("JPEG");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String filetype = Tools.FILE_TYPE_JPEG;
				
				try {
					saveImageAs(filetype);
				}
				catch (Exception ex) {
					shell.getDisplay().beep();
					showError(ex.getMessage(), shell.getText());
				}
			}
		});

		/*
		 * ImageIO does not support tiff by default
		 */
		item = new MenuItem(saveAsMenu, SWT.NONE);
		item.setText("TIFF");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String filetype = Tools.FILE_TYPE_TIFF;
				
				try {
					saveImageAs(filetype);
				}
				catch (Exception ex) {
					shell.getDisplay().beep();
					showError(ex.getMessage(), shell.getText());
				}
			}
		});
		
		item = new MenuItem(saveAsMenu, SWT.NONE);
		item.setText("PNG");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String filetype = Tools.FILE_TYPE_PNG;
				
				try {
					saveImageAs(filetype);
				}
				catch (Exception ex) {
					showError(ex.getMessage(), shell.getText());
				}
			}
		});
		
		item = new MenuItem(saveAsMenu, SWT.NONE);
		item.setText("GIF");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String filetype = Tools.FILE_TYPE_GIF;
				
				try {
					saveImageAs(filetype);
				}
				catch (Exception ex) {
					showError(ex.getMessage(), shell.getText());
				}
			}
		});
		
		item = new MenuItem(saveAsMenu, SWT.NONE);
		item.setText("BMP");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String filetype = Tools.FILE_TYPE_BMP;
				
				try {
					saveImageAs(filetype);
				}
				catch (Exception ex) {
					showError(ex.getMessage(), shell.getText());
				}
			}
		});

		new MenuItem(menuBar, SWT.SEPARATOR);
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Write Selection to Image");
		item.setEnabled(!dataset.getFileFormat().isReadOnly());
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				if ((getSelectedArea().width <= 0) || (getSelectedArea().height <= 0)) {
					showError("No data to write.\nUse Shift+Mouse_drag to select an image area.", shell.getText());
				}
				
				TreeView treeView = viewer.getTreeView();
				//TreeNode node = treeView.findTreeNode(dataset);
				//Group pGroup = (Group) ((DefaultMutableTreeNode) node
				//		.getParent()).getUserObject();
				HObject root = dataset.getFileFormat().getRootObject();
				
				if (root == null) return;
				
				Vector list = new Vector(dataset.getFileFormat().getNumberOfMembers() + 5);
				DefaultMutableTreeNode theNode = null;
				Enumeration local_enum = root.depthFirstEnumeration();

				while (local_enum.hasMoreElements()) {
					theNode = (DefaultMutableTreeNode) local_enum.nextElement();
					list.add(theNode.getUserObject());
				}

				NewDatasetDialog dialog = new NewDatasetDialog((JFrame) viewer,
						pGroup, list, this);
				dialog.setVisible(true);

				HObject obj = (HObject) dialog.getObject();
				if (obj != null) {
					Group pgroup = dialog.getParentGroup();
					try {
						treeView.addObject(obj, pgroup);
					}
					catch (Exception ex) {
						log.debug("Write selection to image:", ex);
					}
				}

				list.setSize(0);
				*/
			}
		});
		rotateRelatedItems.add(item);
		
		new MenuItem(menuBar, SWT.SEPARATOR);

		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Change Palette");
		item.setEnabled(!isTrueColor);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showColorTable();
			}
		});
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Import Palette");
		item.setEnabled(!isTrueColor);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				JFileChooser fchooser = new JFileChooser(ViewProperties
						.getWorkDir());
				int returnVal = fchooser.showOpenDialog(this);

				if (returnVal != JFileChooser.APPROVE_OPTION) {
					return;
				}

				File choosedFile = fchooser.getSelectedFile();
				if (choosedFile == null || choosedFile.isDirectory()) {
					return;
				}

				Vector<String> palList = ViewProperties.getPaletteList();
				String palPath = choosedFile.getAbsolutePath();
				if(!palList.contains(palList))
					palList.addElement(palPath);
				*/
			}
		});
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Export Palette");
		item.setEnabled(!isTrueColor);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				if (imagePalette == null) return;
				
				String workDir = ViewProperties.getWorkDir() + File.separator;
				JFileChooser fchooser = new JFileChooser(workDir);
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Color lookup table", "lut");
				File pfile = Tools.checkNewFile(workDir, ".lut");
				fchooser.setSelectedFile(pfile);
				fchooser.setFileFilter(filter);
				int returnVal = fchooser.showOpenDialog(this);

				if (returnVal != JFileChooser.APPROVE_OPTION) {
					return;
				}

				File choosedFile = fchooser.getSelectedFile();
				if (choosedFile == null || choosedFile.isDirectory()) {
					return;
				}

				if (choosedFile.exists()) {
					int newFileFlag = JOptionPane.showConfirmDialog(this,
							"File exists. Do you want to replace it ?",
									this.getTitle(),
									JOptionPane.YES_NO_OPTION);
					if (newFileFlag == JOptionPane.NO_OPTION) {
						return;
					}
				}

				PrintWriter out = null;

				try {
					out = new PrintWriter(new BufferedWriter(new FileWriter(choosedFile)));
				} 
				catch (Exception ex) { 
					out = null; 
				}

				if (out == null)
					return;

				int cols = 3;
				int rows = 256;
				int rgb = 0;
				for (int i=0; i<rows; i++) {
					out.print(i);
					for (int j=0; j<cols; j++) {
						out.print(' ');
						rgb = imagePalette[j][i];
						if (rgb<0) rgb += 256;
						out.print(rgb);
					}
					out.println();
				}

				out.flush();
				out.close();
				*/
			}
		});
		
		new MenuItem(menuBar, SWT.SEPARATOR);
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Set Value Range");
		item.setEnabled(!isTrueColor);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				if (originalRange == null || originalRange[0] == originalRange[1]) return;
				
				// Call only once
				if (dataDist == null) {
					dataDist = new int[256];
					Tools.findDataDist(data, dataDist, originalRange);
				}
				
				DataRangeDialog drd = new DataRangeDialog((JFrame) viewer, dataRange, originalRange,dataDist);
				double[] drange = drd.getRange();

				if ((drange == null)
						|| (drange[0] == drange[1])
						|| ((drange[0] == dataRange[0]) && (drange[1] == dataRange[1]))) {
					return;
				}

				applyDataRange(drange);
				*/
			}
		});

		// no need for byte data
		// commented out for 2.6. May also need to apply range filter to byte
		// data.
		// try {
		// String cname = data.getClass().getName();
		// char dname = cname.charAt(cname.lastIndexOf("[")+1);
		// if (dname == 'B') {
		// item.setEnabled(false);
		// }
		// } catch (Exception ex) {}

		new MenuItem(menuBar, SWT.SEPARATOR);

		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Show Histogram");
		item.setEnabled(!isTrueColor);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showHistogram();
			}
		});
		rotateRelatedItems.add(item);

		new MenuItem(menuBar, SWT.SEPARATOR);
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Zoom In");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				zoomIn();
			}
		});
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Zoom Out");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				zoomOut();
			}
		});

		new MenuItem(menuBar, SWT.SEPARATOR);
		
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("Flip Image");
		
		Menu flipMenu = new Menu(item);
		item.setMenu(flipMenu);

		item = new MenuItem(flipMenu, SWT.NONE);
		item.setText("Horizontal");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				flip(FLIP_HORIZONTAL);
			}
		});
		
		item = new MenuItem(flipMenu, SWT.NONE);
		item.setText("Vertical");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				flip(FLIP_VERTICAL);
			}
		});
		
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("Rotate Image");
		
		Menu rotateMenu = new Menu(item);
		item.setMenu(rotateMenu);
		
		char t = 186;
		
		item = new MenuItem(rotateMenu, SWT.NONE);
		item.setText("90" + t + " CW");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				rotate(ROTATE_CW_90);
				
				int n = rotateRelatedItems.size();
				for (int i = 0; i < n; i++) {
					boolean itemState = (rotateCount == 0);
					((Composite) rotateRelatedItems.get(i)).setEnabled(itemState);
				}
			}
		});
		
		item = new MenuItem(rotateMenu, SWT.NONE);
		item.setText("90" + t + " CCW");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				rotate(ROTATE_CCW_90);
				
				int n = rotateRelatedItems.size();
				for (int i = 0; i < n; i++) {
					boolean itemState = (rotateCount == 0);
					((Composite) rotateRelatedItems.get(i)).setEnabled(itemState);
				}
			}
		});
		
		new MenuItem(menuBar, SWT.SEPARATOR);

		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Brightness/Contrast");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				if (contrastSlider == null)
					contrastSlider = new ContrastSlider((JFrame) viewer, image.getSource());
				
				contrastSlider.setVisible(true);
				*/
			}
		});
		
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("Contour");
		
		Menu contourMenu = new Menu(item);
		item.setMenu(contourMenu);
		
		for (int i = 3; i < 10; i += 2) {
			item = new MenuItem(contourMenu, SWT.NONE);
			item.setText(String.valueOf(i));
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					//contour(i);
				}
			});
		}

		new MenuItem(menuBar, SWT.SEPARATOR);
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Show Animation");
		item.setEnabled(is3D);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				new Animation((JFrame) viewer, dataset);
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				*/
			}
		});

		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("Animation Speed (frames/second)");
		item.setEnabled(is3D);
		
		Menu animationSpeedMenu = new Menu(item);
		item.setMenu(animationSpeedMenu);
		
		for (int i = 2; i < 12; i += 2) {
			item = new MenuItem(animationSpeedMenu, SWT.NONE);
			item.setText(String.valueOf(i));
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					MenuItem item = (MenuItem) e.item;
					
					animationSpeed = Integer.parseInt(item.getText());
				}
			});
		}
		
		new MenuItem(menuBar, SWT.SEPARATOR);

		item = new MenuItem(menuBar, SWT.CHECK);
		item.setText("Show Value");
		item.setSelection(ViewProperties.showImageValues());
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setValueVisible(((MenuItem) e.getSource()).getSelection());
			}
		});
		rotateRelatedItems.add(item);
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Show Statistics");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					double[] minmax = new double[2];
					double[] stat = new double[2];

					Object theData = null;
					theData = getSelectedData();

					if (theData == null) {
						theData = data;
					}

					Tools.findMinMax(theData, minmax, dataset.getFillValue());
					if (Tools.computeStatistics(theData, stat, dataset.getFillValue()) > 0) {
						String statistics = "Min                      = "
						+ minmax[0] + "\nMax                      = "
						+ minmax[1] + "\nMean                     = "
						+ stat[0] + "\nStandard deviation = " + stat[1];
						
						MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
						info.setMessage(statistics);
						info.setText("Statistics");
						info.open();
					}
				}
				catch (Exception ex) {
					shell.getDisplay().beep();
					showError(ex.getMessage(), shell.getText());
				}
			}
		});

		new MenuItem(menuBar, SWT.SEPARATOR);
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Select All");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					selectAll();
				}
				catch (Exception ex) {
					shell.getDisplay().beep();
					showError(ex.getMessage(), shell.getText());
				}
			}
		});

		new MenuItem(menuBar, SWT.SEPARATOR);
		
		item = new MenuItem(menuBar, SWT.NONE);
		item.setText("Close");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				display.dispose();
			}
		});

		//bar.add(new JLabel("       "));

		// add icons to the menubar

		//Insets margin = new Insets(0, 2, 0, 2);

		// chart button
		//button = new JButton(ViewProperties.getChartIcon());
		//bar.add(button);
		//button.setToolTipText("Histogram");
		//button.setMargin(margin);
		//button.addActionListener(this);
		//button.setActionCommand("Show chart");
		//button.setEnabled(!isTrueColor);

		// palette button
		//button = new JButton(ViewProperties.getPaletteIcon());
		//bar.add(button);
		//button.setToolTipText("Palette");
		//button.setMargin(margin);
		//button.addActionListener(this);
		//button.setActionCommand("Edit palette");
		//button.setEnabled(!isTrueColor);

		// brightness button
		//button = new JButton(ViewProperties.getBrightIcon());
		//bar.add(button);
		//button.setToolTipText("Brightness");
		//button.setMargin(margin);
		//button.addActionListener(this);
		//button.setActionCommand("Brightness");

		// brightness button
		//        button = new JButton(ViewProperties.getAutocontrastIcon());
		//        bar.add(button);
		//        button.setToolTipText("Calculate AutoGain");
		//        button.setMargin(margin);
		//        button.addActionListener(this);
		//        button.setActionCommand("Calculate AutoGain");
		//        button.setEnabled(ViewProperties.isAutoContrast());

		//button = new JButton(ViewProperties.getZoominIcon());
		//bar.add(button);
		//button.addActionListener(this);
		//button.setMargin(margin);
		//button.setActionCommand("Zoom in");
		//button.setToolTipText("Zoom In");
		//button.setName("zoomin");

		// zoom out button
		//button = new JButton(ViewProperties.getZoomoutIcon());
		//bar.add(button);
		//button.setToolTipText("Zoom Out");
		//button.setMargin(margin);
		//button.addActionListener(this);
		//button.setActionCommand("Zoom out");
		//button.setName("zoomout");

		if (is3D) {
			//bar.add(new JLabel("     "));

			// first button
			//button = new JButton(ViewProperties.getFirstIcon());
			//bar.add(button);
			//button.setToolTipText("First");
			//button.setMargin(margin);
			//button.addActionListener(this);
			//button.setActionCommand("First page");
			//button.setName("firstframebutton");

			// previous button
			//button = new JButton(ViewProperties.getPreviousIcon());
			//bar.add(button);
			//button.setToolTipText("Previous");
			//button.setMargin(margin);
			//button.addActionListener(this);
			//button.setActionCommand("Previous page");
			//button.setName("prevframebutton");

			//frameField = new JTextField(String.valueOf(curFrame));
			//frameField.setMaximumSize(new Dimension(50, 30));
			//bar.add(frameField);
			//frameField.setMargin(margin);
			//frameField.addActionListener(this);
			//frameField.setActionCommand("Go to frame");
			//frameField.setName("enterFrameField");

			//JLabel tmpField = new JLabel(String.valueOf(maxFrame),
			//		SwingConstants.CENTER);
			//tmpField.setMaximumSize(new Dimension(50, 30));
			//bar.add(tmpField);

			// next button
			//button = new JButton(ViewProperties.getNextIcon());
			//bar.add(button);
			//button.setToolTipText("Next");
			//button.setMargin(margin);
			//button.addActionListener(this);
			//button.setActionCommand("Next page");
			//button.setName("nextframebutton");

			// last button
			//button = new JButton(ViewProperties.getLastIcon());
			//bar.add(button);
			//button.setToolTipText("Last");
			//button.setMargin(margin);
			//button.addActionListener(this);
			//button.setActionCommand("Last page");
			//button.setName("lastframebutton");

			//button = new JButton(ViewProperties.getAnimationIcon());
			//bar.add(button);
			//button.setToolTipText("Animation");
			//button.setMargin(margin);
			//button.addActionListener(this);
			//button.setActionCommand("Show animation");

		}

		return menuBar;
	}

	// Implementing DataObserver.
	private void previousPage() {
		int rank = dataset.getRank();

		if (rank < 3) {
			return;
		}

		int[] selectedIndex = dataset.getSelectedIndex();
		long[] selectedDims = dataset.getSelectedDims();

		if (selectedDims[selectedIndex[2]] > 1) {
			return; // it is a true color image with three color components
		}

		long[] start = dataset.getStartDims();
		long[] dims = dataset.getDims();
		long idx = start[selectedIndex[2]];
		if (idx == 0) {
			return; // current page is the first page
		}

		gotoPage(start[selectedIndex[2]] - 1);
	}

	// Implementing DataObserver.
	private void nextPage() {
		int rank = dataset.getRank();

		if (rank < 3) {
			return;
		}

		int[] selectedIndex = dataset.getSelectedIndex();
		long[] selectedDims = dataset.getSelectedDims();

		if (selectedDims[selectedIndex[2]] > 1) {
			return; // it is a true color image with three color components
		}

		long[] start = dataset.getStartDims();
		long[] dims = dataset.getDims();
		long idx = start[selectedIndex[2]];
		if (idx == dims[selectedIndex[2]] - 1) {
			return; // current page is the last page
		}

		gotoPage(start[selectedIndex[2]] + 1);
	}

	// Implementing DataObserver.
	private void firstPage() {
		int rank = dataset.getRank();

		if (rank < 3) {
			return;
		}

		int[] selectedIndex = dataset.getSelectedIndex();
		long[] selectedDims = dataset.getSelectedDims();

		if (selectedDims[selectedIndex[2]] > 1) {
			return; // it is a true color image with three color components
		}

		long[] start = dataset.getStartDims();
		long[] dims = dataset.getDims();
		long idx = start[selectedIndex[2]];
		if (idx == 0) {
			return; // current page is the first page
		}

		gotoPage(0);
	}

	// Implementing DataObserver.
	private void lastPage() {
		int rank = dataset.getRank();

		if (rank < 3) {
			return;
		}

		int[] selectedIndex = dataset.getSelectedIndex();
		long[] selectedDims = dataset.getSelectedDims();

		if (selectedDims[selectedIndex[2]] > 1) {
			return; // it is a true color image with three color components
		}

		long[] start = dataset.getStartDims();
		long[] dims = dataset.getDims();
		long idx = start[selectedIndex[2]];
		if (idx == dims[selectedIndex[2]] - 1) {
			return; // current page is the last page
		}

		gotoPage(dims[selectedIndex[2]] - 1);
	}

	public Image getImage() {
		if (image != null) {
			return image;
		}

		int rank = dataset.getRank();
		if (rank <= 0) {
			dataset.init();
		}
		isTrueColor = dataset.isTrueColor();
		is3D = (dataset.getRank() > 2) && !((ScalarDS) dataset).isTrueColor();

		String strValue = null;
		try {
			if (isTrueColor) {
				getTrueColorImage();
			}
			else {
				getIndexedImage();
			}
		}
		catch (Throwable ex) {
			shell.getDisplay().beep();
			showError("ImageView: " + shell.getText(), shell.getText());
			return null;
		}

		// set number type, ...
		if (data != null) {
			isUnsigned = dataset.isUnsigned();
			String cname = data.getClass().getName();
			NT = cname.charAt(cname.lastIndexOf("[") + 1);
		}

		return image;
	}

	/**
	 * @throws Exception
	 * @throws OutOfMemoryError
	 */
	private void getIndexedImage() throws Exception, OutOfMemoryError {
		if (imagePalette==null)
			imagePalette = dataset.getPalette();

		boolean noPalette = false;
		boolean isLocalFile = dataset.getFileFormat().exists();

		if (imagePalette == null) {
			noPalette = true;
			imagePalette = Tools.createGrayPalette();
			viewer.showStatus("\nNo attached palette found, default grey palette is used to display image");
		}

		data = dataset.getData();
		if (bitmask != null) {
			if (Tools.applyBitmask(data, bitmask, bitmaskOP)) {
				doAutoGainContrast = false;
			}
		}

		int typeClass = dataset.getDatatype().getDatatypeClass();
		if (typeClass == Datatype.CLASS_INTEGER || typeClass == Datatype.CLASS_CHAR) {
			data = dataset.convertFromUnsignedC();
			isUnsignedConverted = true;
			doAutoGainContrast = doAutoGainContrast || 
			(ViewProperties.isAutoContrast() && noPalette && isLocalFile);
		}
		else
			doAutoGainContrast = false;

		boolean isAutoContrastFailed = true;
		if (doAutoGainContrast) {
			isAutoContrastFailed = (!computeAutoGainImageData(gainBias,null));
		}

		int w = dataset.getWidth();
		int h = dataset.getHeight();

		if (isAutoContrastFailed) {
			doAutoGainContrast = false;
			imageByteData = Tools.getBytes(data, dataRange, w, h, !dataset
					.isDefaultImageOrder(), dataset.getFilteredImageValues(),
					convertByteData, imageByteData, invalidValueIndex);
		} else if (dataRange!= null && dataRange[0]==dataRange[1]) {
			Tools.findMinMax(data, dataRange, null);
		}

		image = createIndexedImage(imageByteData, imagePalette, w, h);
	}

	/**
	 * @throws Exception
	 * @throws OutOfMemoryError
	 */
	private void getTrueColorImage() throws Exception, OutOfMemoryError {
		isPlaneInterlace = (dataset.getInterlace() == ScalarDS.INTERLACE_PLANE);

		long[] selected = dataset.getSelectedDims();
		long[] start = dataset.getStartDims();
		int[] selectedIndex = dataset.getSelectedIndex();
		long[] stride = dataset.getStride();

		if (start.length > 2) {
			start[selectedIndex[2]] = 0;
			selected[selectedIndex[2]] = 3;
			stride[selectedIndex[2]] = 1;
		}

		// reload data
		dataset.clearData();
		data = dataset.getData();

		int w = dataset.getWidth();
		int h = dataset.getHeight();

		// converts raw data to image data
		imageByteData = Tools.getBytes(data, dataRange, w, h, false, dataset.getFilteredImageValues(),
				imageByteData);


		image = createTrueColorImage(imageByteData, isPlaneInterlace, w, h);
	}

	/**
	 * Compute image data from autogain
	 * 
	 * @return
	 */
	private boolean computeAutoGainImageData(double[] gb, double[] range) {
		boolean retValue = true;

		// data is unsigned short. Convert image byte data using auto-contrast
		// image algorithm
		boolean isUnsigned = dataset.isUnsigned();

		if (gainBias == null) { // calculate auto_gain only once
			gainBias = new double[2];
			Tools.autoContrastCompute(data, gainBias, isUnsigned);
		}

		if (gb == null)
			gb = gainBias;

		autoGainData = Tools.autoContrastApply(data, autoGainData, gb, range, isUnsigned);

		if (autoGainData != null) {
			if ((imageByteData == null)
					|| (imageByteData.length != Array.getLength(data))) {
				imageByteData = new byte[Array.getLength(data)];
			}
			retValue = (Tools.autoContrastConvertImageBuffer(autoGainData, imageByteData, true) >= 0);
		}
		else
			retValue = false;

		if (gainBias_current == null)
			gainBias_current = new double[2];

		gainBias_current[0] = gb[0];
		gainBias_current[1] = gb[1];

		return retValue;
	}

	// implementing ImageObserver
	private void zoomIn() {
		if (zoomFactor >= 1) {
			zoomTo(zoomFactor + 1.0f);
		}
		else {
			zoomTo(zoomFactor + 0.125f);
		}
	}

	// implementing ImageObserver
	private void zoomOut() {
		if (zoomFactor > 1) {
			zoomTo(zoomFactor - 1.0f);
		}
		else {
			zoomTo(zoomFactor - 0.125f);
		}
	}

	// implementing ImageObserver
	private void zoomTo(float zf) {
		/*
		if (zf > 8)
			zf = 8;
		else if (zf < 0.125)
			zf = 0.125f;

		if (zoomFactor == zf)
			return; // no change in zooming

		zoomFactor = zf;

		Dimension imageSize = new Dimension(
				(int) (imageComponent.originalSize.width * zoomFactor),
				(int) (imageComponent.originalSize.height * zoomFactor)
				);

		this.invalidate();
		imageComponent.invalidate();
		imageComponent.setImageSize(imageSize);
		this.validate();
		updateUI();

		if ((zoomFactor > 0.99) && (zoomFactor < 1.01)) {
			setTitle(frameTitle);
		}
		else {
			setTitle(frameTitle + " - " + 100 * zoomFactor + "%");
		}
		*/
	}

	// implementing ImageObserver
	private void showColorTable() {
		if (imagePalette == null) {
			return;
		}

		String viewName = (String) HDFView.getListOfPaletteView().get(0);

		try {
			Class theClass = Class.forName(viewName);
			if ("hdf.view.DefaultPaletteView".equals(viewName)) {
				Object[] initargs = { viewer, this };
				Tools.newInstance(theClass, initargs);
			}
			else {
				Object[] initargs = { this };
				Tools.newInstance(theClass, initargs);
			}
		}
		catch (Exception ex) {
			viewer.showStatus(ex.toString());
		}
	}

	// implementing ImageObserver
	private void showHistogram() {
		/*
		Rectangle rec = imageComponent.selectedArea;

		if (isTrueColor) {
			shell.getDisplay().beep();
			showError("Unsupported operation: unable to draw histogram for true color image.", shell.getText();
			return;
		}
		
		if ((rec == null) || (rec.getWidth() <= 0) || (rec.getHeight() <= 0)) {
			toolkit.beep();
			JOptionPane
			.showMessageDialog(
					this,
					"No data for histogram.\nUse Shift+Mouse_drag to select an image area.",
					getTitle(), JOptionPane.ERROR_MESSAGE);
			return;
		}

		double chartData[][] = new double[1][256];
		for (int i = 0; i < 256; i++) {
			chartData[0][i] = 0.0;
		}

		int w = dataset.getWidth();
		int x0 = (int) (rec.x / zoomFactor);
		int y0 = (int) (rec.y / zoomFactor);
		int x = x0 + (int) (rec.width / zoomFactor);
		int y = y0 + (int) (rec.height / zoomFactor);
		int arrayIndex = 0;
		for (int i = y0; i < y; i++) {
			for (int j = x0; j < x; j++) {
				arrayIndex = (int) imageByteData[i * w + j];
				if (arrayIndex < 0) {
					arrayIndex += 256;
				}
				chartData[0][arrayIndex] += 1.0;
			}
		}

		// Use original data range
		double[] xRange = originalRange;
		if (xRange == null || xRange[0] == xRange[1]) {
			xRange = new double[2];
			Tools.findMinMax(data, xRange, null);
		}

		// double[] xRange = {0, 255};

		Chart cv = new Chart((JFrame) viewer, "Histogram - "
				+ dataset.getPath() + dataset.getName() + " - by pixel index",
				Chart.HISTOGRAM, chartData, xRange, null);
		cv.setVisible(true);
		*/
	}

	/**
	 * Selects all whole image.
	 * 
	 * @throws Exception
	 */
	private void selectAll() throws Exception {
		//imageComponent.selectAll();
	}

	// implementing ImageObserver
	private void flip(int direction) {
		/*
		ImageFilter filter = new FlipFilter(direction);

		if (applyImageFilter(filter)) {
			// toggle flip flag
			if (direction == FLIP_HORIZONTAL) {
				isHorizontalFlipped = !isHorizontalFlipped;
			}
			else {
				isVerticalFlipped = !isVerticalFlipped;
			}
		}
		*/
	}

	// implementing ImageObserver
	private void rotate(int direction) {
		/*
		if ( !(direction == ROTATE_CW_90 || direction == ROTATE_CCW_90))
			return;

		Rotate90Filter filter = new Rotate90Filter(direction);
		applyImageFilter(filter);

		if (direction == ROTATE_CW_90) {
			rotateCount++;
			if (rotateCount == 4) {
				rotateCount = 0;
			}
		}
		else {
			rotateCount--;
			if (rotateCount == -4) {
				rotateCount = 0;
			}
		}
		*/
	}

	// implementing ImageObserver
	private void contour(int level) {
		//applyImageFilter(new ContourFilter(level));
	}

	/** Apply contrast/brightness to unsigned short integer */
	private void applyAutoGain(double[] gb, double[] range) {

		if (computeAutoGainImageData(gb, range)) {
			int w = dataset.getWidth();
			int h = dataset.getHeight();
			image = createIndexedImage(imageByteData, imagePalette, w, h);
			//imageComponent.setImage(image);
			zoomTo(zoomFactor);
		}
	}

	// implementing ImageObserver
	private void setValueVisible(boolean b) {
		//valueField.setVisible(b);
		//validate();
		// updateUI(); //bug !!! on Windows. gives NullPointerException at
		// javax.swing.plaf.basic.BasicInternalFrameUI$BorderListener.mousePressed(BasicInternalFrameUI.java:693)
	}

	/** change alpha value for a given list of pixel locations */
	/*
	private void adjustAlpha(BufferedImage img, int alpha, List<Integer> idx)
	{
		if (img==null || idx.size()<=0)
			return;

		final int[] pixels = ( (DataBufferInt) img.getRaster().getDataBuffer() ).getData();
		int len = pixels.length;

		alpha = alpha << 24;
		for (Integer i : idx) {
			if (i<len)
				pixels[i] = alpha | (pixels[i] & 0x00ffffff);
		}

		// for test only
		// final int[] pixels = ( (DataBufferInt) img.getRaster().getDataBuffer() ).getData();
		// for (int i=0; i<pixels.length/2; i++) pixels[i] = (pixels[i] & 0x60ffffff);
	}
	*/


	/**
	 * This method returns a buffered image with the contents of an image.
	 * 
	 * @param image
	 *            the plain image object.
	 * @return buffered image for the given image.
	 */
	/*
	private BufferedImage toBufferedImage(Image image) {
		if (image == null) {
			return null;
		}

		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

		// !!!!!!!!!!!!!!!!!! NOTICE !!!!!!!!!!!!!!!!!!!!!
		// the following way of creating a buffered image is using
		// Component.createImage(). This method can be used only if the
		// component is visible on the screen. Also, this method returns
		// buffered images that do not support transparent pixels.
		// The buffered image created by this way works for package
		// com.sun.image.codec.jpeg.*
		// It does not work well with JavaTM Advanced Imaging
		// com.sun.media.jai.codec.*;
		// if the screen setting is less than 32-bit color
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage bimage = (BufferedImage) createImage(w, h);
		Graphics g = bimage.createGraphics();
		g.drawImage(image, 0, 0, null);

		g.dispose();
		return bimage;
	}
    */

	/**
	 * Save the image to an image file.
	 * 
	 * @param type
	 *            the image type.
	 * @throws Exception
	 */
	private void saveImageAs(String type) throws Exception {
		if (image == null) {
			return;
		}

		/*
		final JFileChooser fchooser = new JFileChooser(dataset.getFile());
		if (type.equals(Tools.FILE_TYPE_JPEG)) {
			fchooser.setFileFilter(DefaultFileFilter.getFileFilterJPEG());
			// } else if (type.equals(Tools.FILE_TYPE_TIFF)) {
			// fchooser.setFileFilter(DefaultFileFilter.getFileFilterTIFF());
		}
		else if (type.equals(Tools.FILE_TYPE_PNG)) {
			fchooser.setFileFilter(DefaultFileFilter.getFileFilterPNG());
		}
		else if (type.equals(Tools.FILE_TYPE_GIF)) {
			fchooser.setFileFilter(DefaultFileFilter.getFileFilterGIF());
		}
		else if (type.equals(Tools.FILE_TYPE_BMP)) {
			fchooser.setFileFilter(DefaultFileFilter.getFileFilterBMP());
		}

		// fchooser.changeToParentDirectory();
		fchooser.setDialogTitle("Save Current Image To " + type + " File --- "
				+ dataset.getName());

		File choosedFile = new File(dataset.getName() + "."
				+ type.toLowerCase());
		fchooser.setSelectedFile(choosedFile);

		int returnVal = fchooser.showSaveDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}

		choosedFile = fchooser.getSelectedFile();
		if (choosedFile == null) {
			return;
		}
		String fname = choosedFile.getAbsolutePath();

		if (choosedFile.exists()) {
			int newFileFlag = JOptionPane.showConfirmDialog(this,
					"File exists. Do you want to replace it ?",
							this.getTitle(), JOptionPane.YES_NO_OPTION);
			if (newFileFlag == JOptionPane.NO_OPTION) {
				return;
			}
		}

		BufferedImage bi = null;
		try {
			bi = toBufferedImage(image);
		}
		catch (OutOfMemoryError err) {
			toolkit.beep();
			JOptionPane.showMessageDialog(this, err.getMessage(), getTitle(),
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		Tools.saveImageAs(bi, choosedFile, type);

		bi = null;

		viewer.showStatus("Current image saved to: " + fname);

		try {
			RandomAccessFile rf = new RandomAccessFile(choosedFile, "r");
			long size = rf.length();
			rf.close();
			viewer.showStatus("File size (bytes): " + size);
		}
		catch (Exception ex) {
			log.debug("File {} size:", choosedFile.getName(), ex);
		}
		*/
	}

	public void actionPerformed(ActionEvent e) {
		/*
		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			if (cmd.startsWith("Go to frame")) {
				int page = 0;
				try {
					page = Integer.parseInt(frameField.getText().trim())-indexBase;
				}
				catch (Exception ex) {
					page = -1;
				}

				gotoPage (page);
			}
			else if (cmd.equals("First page")) {
				firstPage();
			}
			else if (cmd.equals("Previous page")) {
				previousPage();
			}
			else if (cmd.equals("Next page")) {
				nextPage();
			}
			else if (cmd.equals("Last page")) {
				lastPage();
			}
		}
		finally {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		*/
	}

	// Implementing DataView.
	public HObject getDataObject() {
		return dataset;
	}

	public byte[] getImageByteData() {
		return imageByteData;
	}

	/**
	 * Returns the selected data values.
	 * 
	 * @return the selected data object.
	 */
	public Object getSelectedData() {
		/*
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
		int w = imageComponent.originalSize.width;
		int h = imageComponent.originalSize.height;

		// transfer location to the original coordinator
		if (isHorizontalFlipped) {
			c0 = w - 1 - c0 - cols;
		}

		if (isVerticalFlipped) {
			r0 = h - 1 - r0 - rows;
		}

		int idx_src = 0, idx_dst = 0;
		if (isTrueColor) {
			int imageSize = w * h;
			if (isPlaneInterlace) {
				for (int j = 0; j < 3; j++) {
					int plane = imageSize * j;
					for (int i = 0; i < rows; i++) {
						idx_src = plane + (r0 + i) * w + c0;
						System.arraycopy(data, idx_src, selectedData, idx_dst,
								cols);
						idx_dst += cols;
					}
				}
			}
			else {
				int numberOfDataPoints = cols * 3;
				for (int i = 0; i < rows; i++) {
					idx_src = (r0 + i) * w + c0;
					System.arraycopy(data, idx_src * 3, selectedData, idx_dst,
							numberOfDataPoints);
					idx_dst += numberOfDataPoints;
				}
			}
		}
		else { // indexed image
			for (int i = 0; i < rows; i++) {
				idx_src = (r0 + i) * w + c0;
				System.arraycopy(data, idx_src, selectedData, idx_dst, cols);
				idx_dst += cols;
			}
		}

		return selectedData;
		*/
		
		return null; // Remove when fixed
	}

	/**
	 * returns the selected area of the image
	 * 
	 * @return the rectangle of the selected image area.
	 */
	public Rectangle getSelectedArea() {
		//return imageComponent.originalSelectedArea;
		
		return null; // Remove when fixed
	}

	/** @return true if the image is a truecolor image. */
	public boolean isTrueColor() {
		return isTrueColor;
	}

	/** @return true if the image interlace is plance interlace. */
	public boolean isPlaneInterlace() {
		return isPlaneInterlace;
	}

	public void setImage(Image img) {
		image = img;
		//imageComponent.setImage(img);

		setImageDirection();
	}

	private void setImageDirection() {
		boolean isHF = isHorizontalFlipped;
		boolean isVF = isVerticalFlipped;
		int rc = rotateCount;

		if (isHF || isVF || rc!=0) {
			isHorizontalFlipped = false;
			isVerticalFlipped = false;
			rotateCount = 0;        

			if (isHF)
				flip(FLIP_HORIZONTAL);

			if (isVF)
				flip(FLIP_VERTICAL);

			while (rc > 0)  {
				rotate(ROTATE_CW_90);
				rc--;
			}

			while (rc < 0)  {
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

	public byte[][] getPalette() {
		return imagePalette;
	}

	public void setPalette(byte[][] pal) {
		imagePalette = pal;
		//paletteComponent.updatePalette(pal);
	}

	private void gotoPage(long idx) {
		if (dataset.getRank() < 3 ||
				idx == (curFrame-indexBase) ) {
			return;
		}

		long[] start = dataset.getStartDims();
		int[] selectedIndex = dataset.getSelectedIndex();
		long[] dims = dataset.getDims();

		if ((idx < 0) || (idx >= dims[selectedIndex[2]])) {
			shell.getDisplay().beep();
			showError("Frame number must be between " + indexBase + 
			         " and " + (dims[selectedIndex[2]] - 1+indexBase), shell.getText());
			return;
		}

		//setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		start[selectedIndex[2]] = idx;
		curFrame = idx+indexBase;
		dataset.clearData();
		image = null;
		gainBias = null;
		//imageComponent.setImage(getImage());
		frameField.setText(String.valueOf(curFrame));

		isHorizontalFlipped = false;
		isVerticalFlipped = false;
		rotateCount = 0;        

		if (imageOrigin == Origin.LOWER_LEFT)
			flip(FLIP_VERTICAL);
		else if (imageOrigin == Origin.UPPER_RIGHT)
			flip(FLIP_HORIZONTAL);
		if (imageOrigin == Origin.LOWER_RIGHT) {
			rotate(ROTATE_CW_90);
			rotate(ROTATE_CW_90);
		}

		//setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

		//updateUI();
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
	 * @return the image.
	 */
	private Image createIndexedImage(byte[] imageData, byte[][] palette, int w, int h) 
	{
		/*
		bufferedImage = (BufferedImage)Tools.createIndexedImage(bufferedImage, imageData, palette, w, h);
		adjustAlpha(bufferedImage, 0, invalidValueIndex);        

		return bufferedImage;
		*/
		return null; // Remove when fixed
	}

	/**
	 * Creates a true color image.
	 * <p>
	 * The data may be arranged in one of two ways: by pixel or by plane. In
	 * both cases, the dataset will have a dataspace with three dimensions,
	 * height, width, and components.
	 * <p>
	 * For HDF4, the interlace modes specify orders for the dimensions as:
	 * 
	 * <pre>
	 * INTERLACE_PIXEL = [width][height][pixel components]
	 *            INTERLACE_PLANE = [pixel components][width][height]
	 * </pre>
	 * <p>
	 * For HDF5, the interlace modes specify orders for the dimensions as:
	 * 
	 * <pre>
	 * INTERLACE_PIXEL = [height][width][pixel components]
	 *            INTERLACE_PLANE = [pixel components][height][width]
	 * </pre>
	 * <p>
	 * 
	 * @param imageData
	 *            the byte array of the image data.
	 * @param planeInterlace
	 *            flag if the image is plane intelace.
	 * @param w
	 *            the width of the image.
	 * @param h
	 *            the height of the image.
	 * @return the image.
	 */
	private Image createTrueColorImage(byte[] imageData, boolean planeInterlace,
			int w, int h) 
	{
		/*
		if (bufferedImage == null)
			bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

		final int[] pixels = ( (DataBufferInt) bufferedImage.getRaster().getDataBuffer() ).getData();
		int len = pixels.length;

		int idx = 0, r = 0, g = 0, b = 0;
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				if (planeInterlace) {
					r = ((int)imageData[idx] & 0xff)<<16;
					g = ((int)imageData[len + idx] & 0xff)<<8;
					b = ((int)imageData[len * 2 + idx] & 0xff);
				}
				else {
					r = ((int)imageData[idx * 3] & 0xff)<<16;
					g = ((int)imageData[idx * 3 + 1] & 0xff)<<8;
					b = ((int)imageData[idx * 3 + 2] & 0xff);
				}
				pixels[idx++] = 0xff000000 | r | g | b;
			} 
		} 

		adjustAlpha(bufferedImage, 0, invalidValueIndex);        
		return bufferedImage;
		*/
		
		return null; // Remove when fixed
	}

	/*
	private boolean applyImageFilter(ImageFilter filter) {
		boolean status = true;
		//ImageProducer imageProducer = image.getSource();

		try {
			//image = createImage(new FilteredImageSource(imageProducer, filter));
			imageComponent.setImage(image);
			zoomTo(zoomFactor);
		}
		catch (Throwable err) {
			shell.getDisplay().beep();
			showError(err.getMessage(), shell.getText());
			status = false;
		}

		return status;
	}
	*/

	private void applyDataRange(double[] newRange) {
		if (doAutoGainContrast && gainBias!= null) {
			applyAutoGain(gainBias_current, newRange);
		} 
		else {
			int w = dataset.getWidth();
			int h = dataset.getHeight();

			invalidValueIndex.clear(); // data range changed. need to reset invalid values
			imageByteData = Tools.getBytes(data, newRange, w, h, !dataset
					.isDefaultImageOrder(), dataset.getFilteredImageValues(), true,
					null, invalidValueIndex);

			image = createIndexedImage(imageByteData, imagePalette, w, h);
			setImage(image);
			zoomTo(zoomFactor);
			//paletteComponent.updateRange(newRange);
		}

		dataRange[0] = newRange[0];
		dataRange[1] = newRange[1];
	}

	/** PaletteCanvas draws the palette on the side of the image. */
	/*
	private class PaletteCanvas extends Canvas {
		private static final long serialVersionUID = -5194383032992628565L;
		private Color[] colors = null;
		private double[] pixelData = null;
		private Dimension paintSize = null;
		java.text.DecimalFormat format;
		double[] dRange = null;

		private PaletteCanvas(byte[][] palette, double[] range) {
			paintSize = new Dimension(25, 2);
			format = new java.text.DecimalFormat("0.00E0");
			dRange = range;
			double unsigned_celling = 0;

			if ((palette != null) && (range != null)) {
				double ratio = (dRange[1] - dRange[0]) / 255;

				pixelData = new double[256];
				for (int i = 0; i < 256; i++) {
					pixelData[i] = (dRange[0] + ratio * i);
				}
			}

			updatePalette(palette);

			setPreferredSize(new Dimension(paintSize.width + 60,
					paintSize.height * 256));
			setVisible(true);
		}

		private void updatePalette(byte[][] palette) {
			if ((palette != null) && (dRange != null)) {
				colors = new Color[256];

				int r, g, b;
				for (int i = 0; i < 256; i++) {
					r = (int) palette[0][i];
					if (r < 0) {
						r += 256;
					}
					g = (int) palette[1][i];
					if (g < 0) {
						g += 256;
					}
					b = (int) palette[2][i];
					if (b < 0) {
						b += 256;
					}

					colors[i] = new Color(r, g, b);
				}
			}

			repaint();
		}

		private void updateRange(double[] newRange) {
			if (newRange == null) {
				return;
			}

			dRange = newRange;
			double ratio = (dRange[1] - dRange[0]) / 255;
			for (int i = 0; i < 256; i++) {
				pixelData[i] = (dRange[0] + ratio * i);
			}

			repaint();
		}

		public void paint(Graphics g) {
			if ((colors == null) && (pixelData == null)) {
				return;
			}

			Font font = g.getFont();
			g.setFont(new Font(font.getName(), font.getStyle(), 12));
			for (int i = 0; i < 256; i++) {
				g.setColor(colors[i]);
				g.fillRect(0, paintSize.height * i, paintSize.width,
						paintSize.height);
			}

			g.setColor(Color.black);
			for (int i = 0; i < 25; i++) {
				g.drawString(format.format(pixelData[i * 10]),
						paintSize.width + 5, 10 + paintSize.height * i * 10);
			}
			g.drawString(format.format(pixelData[255]), paintSize.width + 5,
					paintSize.height * 255);
		}
	}
	*/
	
	private void showError(String errorMsg, String title) {
    	MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
    	error.setText(title);
    	error.setMessage(errorMsg);
    	error.open();
    }

	/** ImageComponent draws the image. */
	private class ImageComponent 
	//extends JComponent
	//implements MouseListener, MouseMotionListener, MouseWheelListener 
	{
		/*
		private static final long serialVersionUID = -2690648149547151532L;
		private Dimension originalSize, imageSize;
		private Image image;
		private Point startPosition, currentPosition; // mouse clicked position
		private Rectangle selectedArea, originalSelectedArea;
		private StringBuffer strBuff; // to hold display value
		private int yMousePosition = 0; // the vertical position of the current mouse
		private Dimension scrollDim = null;
		private JScrollBar hbar = null;
		private JScrollBar vbar = null;
        */

		/*
		private ImageComponent(Image img) {
			image = img;
			//imageSize = new Dimension(image.getWidth(this), image
			//		.getHeight(this));
			originalSize = imageSize;
			//selectedArea = new Rectangle();
			//originalSelectedArea = new Rectangle();
			setPreferredSize(imageSize);
			strBuff = new StringBuffer();

			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
		}
		*/

		/*
		public void paint(Graphics g) {
			if (g instanceof Graphics2D && (zoomFactor<0.99)) {
				Graphics2D g2 = (Graphics2D) g;

				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				Image scaledImg = multiBiliner(image, imageSize.width, imageSize.height, true);
				//g2.drawImage(scaledImg, 0, 0, imageSize.width, imageSize.height, this);

			} 
			else
				//g.drawImage(image, 0, 0, imageSize.width, imageSize.height, this);

			if ((selectedArea.width > 0) && (selectedArea.height > 0)) {
				g.setColor(Color.red);
				g.drawRect(selectedArea.x, selectedArea.y, selectedArea.width,
						selectedArea.height);
			}
		}
		*/

		/**
		 * Create an image using multiple step bilinear, see details at
		 * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
		 *
		 * @param img the original image to be scaled
		 * @param targetWidth the desired width of the scaled instance
		 * @param targetHeight the desired height of the scaled instance,
		 * @return a scaled version of the original 
		 */
		/*
		private Image multiBiliner(Image img, int targetWidth, int targetHeight, boolean highquality)
		{
			Image ret = img;
			//int w = img.getWidth(null)/2;
			//int h = img.getHeight(null)/2;

			// only do multiple step bilinear for down scale more than two times
			//if (!highquality || w <=targetWidth || h <=targetHeight)
			//	return ret;

			int type = BufferedImage.TYPE_INT_RGB;
			//if (image instanceof BufferedImage) {
			//	BufferedImage tmp = (BufferedImage)image;
			//	if (tmp.getColorModel().hasAlpha())
			//		type = BufferedImage.TYPE_INT_ARGB;
			//} 
			//else {
			//	PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
			//	ColorModel cm = pg.getColorModel();
			//	if (cm!=null && cm.hasAlpha())
			//		type = BufferedImage.TYPE_INT_ARGB;
			//}

			//do {
			//	BufferedImage tmp = new BufferedImage(w, h, type);
			//	Graphics2D g2 = tmp.createGraphics();
			//	g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			//	//g2.drawImage(ret, 0, 0, w, h, null);
			//	g2.dispose();
			//	//ret = tmp;

			//	w /= 2;
			//	if (w < targetWidth) {
			//		w = targetWidth;
			//	}

			//	h /= 2;
			//	if (h < targetHeight) {
			//		h = targetHeight;
			//	}

			//} while (w != targetWidth || h != targetHeight);

			return ret;
		}
		*/
		/*
		public void mousePressed(MouseEvent e) {
			startPosition = e.getPoint();
			//selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);
			scrollDim = imageScroller.getSize();
			hbar = imageScroller.getHorizontalScrollBar();
			vbar = imageScroller.getVerticalScrollBar();

			if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK) {
				setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			}
			else {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
		}
		*/

		/*
		public void mouseClicked(MouseEvent e) {
			startPosition = e.getPoint();
			//selectedArea.setBounds(startPosition.x, startPosition.y, 0, 0);

			if (hbar.isVisible()) {
				hbar.setValue(startPosition.x - scrollDim.width / 2);
			}

			if (vbar.isVisible()) {
				vbar.setValue(startPosition.y - scrollDim.height / 2);
			}

			repaint();
		}
		*/

		/*
		public void mouseDragged(MouseEvent e) {
			// don't update too often.
			try {
				Thread.sleep(20);
			}
			catch (Exception ex) {
				log.debug("thread sleep:", ex);
			}
			currentPosition = e.getPoint();

			if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK) {
				int x0 = Math.max(0, Math.min(startPosition.x,
						currentPosition.x));
				int y0 = Math.max(0, Math.min(startPosition.y,
						currentPosition.y));
				int x1 = Math.min(imageSize.width, Math.max(startPosition.x,
						currentPosition.x));
				int y1 = Math.min(imageSize.height, Math.max(startPosition.y,
						currentPosition.y));

				int w = x1 - x0;
				int h = y1 - y0;

				//selectedArea.setBounds(x0, y0, w, h);
				double ratio = 1.0 / zoomFactor;

				//originalSelectedArea.setBounds((int) (x0 * ratio),
				//		(int) (y0 * ratio), (int) (w * ratio),
				//		(int) (h * ratio));

				repaint();
			}
			else {
				if (hbar.isVisible()) {
					int dx = startPosition.x - currentPosition.x;
					hbar.setValue(hbar.getValue() + dx);
				}

				if (vbar.isVisible()) {
					int dy = startPosition.y - currentPosition.y;
					vbar.setValue(vbar.getValue() + dy);
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
			yMousePosition = e.getY();
			showPixelValue(e.getX(), yMousePosition);
		}

		public void mouseWheelMoved(MouseWheelEvent e) {
			JScrollBar jb = imageScroller.getVerticalScrollBar();
			int us = e.getUnitsToScroll();
			int wr = e.getWheelRotation();
			int n = us * jb.getUnitIncrement();
			int y = jb.getValue();

			if (((y <= 0) && (wr < 0))
					|| (y + jb.getVisibleAmount() * wr >= zoomFactor
							* originalSize.height)) {
				return;
			}

			yMousePosition += n;
			jb.setValue(jb.getValue() + n);

			showPixelValue(e.getX(), yMousePosition);
		}

		private void showPixelValue(int x, int y) {
			if (!valueField.isVisible() || rotateCount != 0) {
				return;
			}

			if (data == null) {
				return;
			}

			x = (int) (x / zoomFactor);
			int w = originalSize.width;

			if ((x < 0) || (x >= w)) {
				return; // out of image bound
			}

			y = (int) (y / zoomFactor);
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
			strBuff.append("x=");
			strBuff.append(x+indexBase);
			strBuff.append(",   y=");
			strBuff.append(y+indexBase);
			strBuff.append(",   value=");

			if (isTrueColor) {
				strBuff.append("(");
				int i0, i1, i2;
				String r, g, b;

				if (isPlaneInterlace) {
					i0 = y * w + x; // index for the first plane
					i1 = i0 + w * h; // index for the second plane
					i2 = i0 + 2 * w * h; // index for the third plane
				}
				else {
					i0 = 3 * (y * w + x); // index for the first pixel
					i1 = i0 + 1; // index for the second pixel
					i2 = i0 + 2; // index for the third pixel
				}

				if (isUnsigned && !isUnsignedConverted) {
					r = String.valueOf(convertUnsignedPoint(i0));
					g = String.valueOf(convertUnsignedPoint(i1));
					b = String.valueOf(convertUnsignedPoint(i2));
				}
				else {
					r = String.valueOf(Array.get(data, i0));
					g = String.valueOf(Array.get(data, i1));
					b = String.valueOf(Array.get(data, i2));
				}

				strBuff.append(r + ", " + g + ", " + b);
				strBuff.append(")");
			} // if (isTrueColor)
			else {

				int idx = y * w + x;
				if (!dataset.isDefaultImageOrder())
					idx = x*h+y;

				if (isUnsigned && !isUnsignedConverted) {
					strBuff.append(convertUnsignedPoint(idx));
				}
				else {
					strBuff.append(Array.get(data, idx));
				}
			}

			valueField.setText(strBuff.toString());
		} // private void showPixelValue

		private void selectAll() {
			//selectedArea.setBounds(0, 0, imageSize.width, imageSize.height);
			//originalSelectedArea.setBounds(0, 0, originalSize.width,
			//		originalSize.height);
			repaint();
		}

		private long convertUnsignedPoint(int idx) {
			long l = 0;

			if (NT == 'B') {
				byte b = Array.getByte(data, idx);

				if (b < 0) {
					l = b + 256;
				}
				else {
					l = b;
				}
			}
			else if (NT == 'S') {
				short s = Array.getShort(data, idx);
				if (s < 0) {
					l = s + 65536;
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

		private void setImageSize(Dimension size) {
			imageSize = size;
			setPreferredSize(imageSize);

			int w = selectedArea.width;
			int h = selectedArea.height;
			if ((w > 0) && (h > 0)) {
				// use fixed selected area to reduce the rounding error
				//selectedArea.setBounds(
				//		(int) (originalSelectedArea.x * zoomFactor),
				//		(int) (originalSelectedArea.y * zoomFactor),
				//		(int) (originalSelectedArea.width * zoomFactor),
				//		(int) (originalSelectedArea.height * zoomFactor));
			}

			repaint();
		}

		private void setImage(Image img) {
			image = img;
			//imageSize = new Dimension(image.getWidth(this), image
			//		.getHeight(this));
			originalSize = imageSize;
			//selectedArea.setSize(0, 0);
			setPreferredSize(imageSize);

			setImageSize(new Dimension((int) (originalSize.width * zoomFactor),
					(int) (originalSize.height * zoomFactor)));

			repaint();
		}
		*/
	} // private class ImageComponent extends JComponent

	/**
	 * FlipFilter creates image filter to flip image horizontally or
	 * vertically.
	 */
	private class FlipFilter extends ImageFilter {
		/** flip direction */
		private int direction;

		/** pixel value */
		private int raster[] = null;

		/** width & height */
		private int imageWidth, imageHeight;

		/**
		 * Constructs an image filter to flip horizontally or vertically.
		 * <p>
		 * 
		 * @param d
		 *            the flip direction.
		 */
		private FlipFilter(int d) {
			if (d < FLIP_HORIZONTAL) {
				d = FLIP_HORIZONTAL;
			}
			else if (d > FLIP_VERTICAL) {
				d = FLIP_VERTICAL;
			}

			direction = d;
		}

		public void setDimensions(int w, int h) {
			imageWidth = w;
			imageHeight = h;

			// specify the raster
			if (raster == null) {
				raster = new int[imageWidth * imageHeight];
			}

			consumer.setDimensions(imageWidth, imageHeight);
		}

		public void setPixels(int x, int y, int w, int h, ColorModel model,
				byte pixels[], int off, int scansize) {
			int srcoff = off;
			int dstoff = y * imageWidth + x;
			for (int yc = 0; yc < h; yc++) {
				for (int xc = 0; xc < w; xc++) {
					raster[dstoff++] = model.getRGB(pixels[srcoff++] & 0xff);
				}

				srcoff += (scansize - w);
				dstoff += (imageWidth - w);
			}
		}

		public void setPixels(int x, int y, int w, int h, ColorModel model,
				int pixels[], int off, int scansize) {
			int srcoff = off;
			int dstoff = y * imageWidth + x;

			for (int yc = 0; yc < h; yc++) {
				for (int xc = 0; xc < w; xc++) {
					raster[dstoff++] = model.getRGB(pixels[srcoff++]);
				}
				srcoff += (scansize - w);
				dstoff += (imageWidth - w);
			}
		}

		public void imageComplete(int status) {
			if ((status == IMAGEERROR) || (status == IMAGEABORTED)) {
				consumer.imageComplete(status);
				return;
			}

			int pixels[] = new int[imageWidth];
			for (int y = 0; y < imageHeight; y++) {
				if (direction == FLIP_VERTICAL) {
					// grab pixel values of the target line ...
					int pos = (imageHeight - 1 - y) * imageWidth;
					for (int kk = 0; kk < imageWidth; kk++) {
						pixels[kk] = raster[pos + kk];
					}
				}
				else {
					int pos = y * imageWidth;
					for (int kk = 0; kk < imageWidth; kk++) {
						pixels[kk] = raster[pos + kk];
					}

					// swap the pixel values of the target line
					int hw = imageWidth / 2;
					for (int kk = 0; kk < hw; kk++) {
						int tmp = pixels[kk];
						pixels[kk] = pixels[imageWidth - kk - 1];
						pixels[imageWidth - kk - 1] = tmp;
					}
				}

				// consumer it ....
				consumer.setPixels(0, y, imageWidth, 1, ColorModel
						.getRGBdefault(), pixels, 0, imageWidth);
			} // for (int y = 0; y < imageHeight; y++)

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
	/*
	private class BrightnessFilter extends RGBImageFilter {
		// brightness level = [-200, 200]
		int brightLevel = 0;

		// contrast level [0, 4]
		float contrastLevel = 0;

		public BrightnessFilter(int blevel, int clevel) {
			if (blevel < -100) {
				brightLevel = -100;
			}
			else if (blevel > 100) {
				brightLevel = 100;
			}
			else {
				brightLevel = blevel;
			}
			brightLevel *= 2;

			if (clevel < -100) {
				clevel = -100;
			}
			else if (clevel > 100) {
				clevel = 100;
			}

			if (clevel > 0) {
				contrastLevel = (clevel / 100f + 1) * 2;
			}
			else if (clevel < 0) {
				contrastLevel = (clevel / 100f + 1) / 2;
			}
			else {
				contrastLevel = 0;
			}

			canFilterIndexColorModel = true;
		}

		public int filterRGB(int x, int y, int rgb) {
			// adjust brightness first, then adjust contrast
			// it gives more color depth

			if (brightLevel != 0) {
				int r = (rgb & 0x00ff0000) >> 16;
			int g = (rgb & 0x0000ff00) >> 8;
					int b = (rgb & 0x000000ff);

					r += brightLevel;
					g += brightLevel;
					b += brightLevel;

					if (r < 0) {
						r = 0;
					}
					if (r > 255) {
						r = 255;
					}
					if (g < 0) {
						g = 0;
					}
					if (g > 255) {
						g = 255;
					}
					if (b < 0) {
						b = 0;
					}
					if (b > 255) {
						b = 255;
					}

					r = (r << 16) & 0x00ff0000;
					g = (g << 8) & 0x0000ff00;
					b = b & 0x000000ff;

					rgb = ((rgb & 0xff000000) | r | g | b);
			}

			if (contrastLevel > 0.000001) { // do not compare float using !=0 or
				// ==0
				int r = (rgb & 0x00ff0000) >> 16;
					int g = (rgb & 0x0000ff00) >> 8;
					int b = (rgb & 0x000000ff);

					float f = (float) r / 255f;
					f -= 0.5;
					f *= contrastLevel;
					f += 0.5;
					f *= 255f;
					if (f < 0) {
						f = 0;
					}
					if (f > 255) {
						f = 255;
					}
					r = (int) f;

					f = (float) g / 255f;
					f -= 0.5;
					f *= contrastLevel;
					f += 0.5;
					f *= 255f;
					if (f < 0) {
						f = 0;
					}
					if (f > 255) {
						f = 255;
					}
					g = (int) f;

					f = (float) b / 255f;
					f -= 0.5;
					f *= contrastLevel;
					f += 0.5;
					f *= 255f;
					if (f < 0) {
						f = 0;
					}
					if (f > 255) {
						f = 255;
					}
					b = (int) f;

					r = (r << 16) & 0x00ff0000;
					g = (g << 8) & 0x0000ff00;
					b = b & 0x000000ff;

					rgb = ((rgb & 0xff000000) | r | g | b);
			}

			return rgb;
		}
	}
	*/

	/**
	 * Makes an image filter for contour.
	 */
	private class ContourFilter extends ImageFilter {
		// default color model
		private ColorModel defaultRGB;

		// contour level
		int level;

		// the table of the contour levels
		int levels[];

		// colors for drawable contour line
		int[] levelColors;

		// default RGB

		// pixel value
		private int raster[] = null;

		// width & height
		private int imageWidth, imageHeight;

		/**
		 * Create an contour filter for a given level contouring.
		 * 
		 * @param theLevel
		 *            the contour level.
		 */
		private ContourFilter(int theLevel) {
			defaultRGB = ColorModel.getRGBdefault();

			levelColors = new int[9];
			levelColors[0] = Color.red.getRGB();
			levelColors[1] = Color.green.getRGB();
			levelColors[2] = Color.blue.getRGB();
			levelColors[3] = Color.magenta.getRGB();
			levelColors[4] = Color.orange.getRGB();
			levelColors[5] = Color.cyan.getRGB();
			levelColors[6] = Color.black.getRGB();
			levelColors[7] = Color.pink.getRGB();
			levelColors[8] = Color.yellow.getRGB();


			if (theLevel < 1) {
				theLevel = 1;
			}
			else if (theLevel > 9) {
				theLevel = 9;
			}

			level = theLevel;
			levels = new int[level];

			int dx = 128 / level;
			for (int i = 0; i < level; i++) {
				levels[i] = (i + 1) * dx;
			}
		}

		public void setDimensions(int width, int height) {
			this.imageWidth = width;
			this.imageHeight = height;

			// specify the raster
			if (raster == null) {
				raster = new int[imageWidth * imageHeight];
			}

			consumer.setDimensions(width, height);
		}

		public void setPixels(int x, int y, int w, int h, ColorModel model,
				byte pixels[], int off, int scansize) {
			int rgb = 0;
			int srcoff = off;
			int dstoff = y * imageWidth + x;

			for (int yc = 0; yc < h; yc++) {
				for (int xc = 0; xc < w; xc++) {
					rgb = model.getRGB(pixels[srcoff++] & 0xff);
					raster[dstoff++] = (((rgb >> 16) & 0xff)
							+ ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
				}
				srcoff += (scansize - w);
				dstoff += (imageWidth - w);
			}

		}

		public void setPixels(int x, int y, int w, int h, ColorModel model,
				int pixels[], int off, int scansize) {
			int rgb = 0;
			int srcoff = off;
			int dstoff = y * imageWidth + x;

			for (int yc = 0; yc < h; yc++) {
				for (int xc = 0; xc < w; xc++) {
					rgb = model.getRGB(pixels[srcoff++] & 0xff);
					raster[dstoff++] = (((rgb >> 16) & 0xff)
							+ ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
				}

				srcoff += (scansize - w);
				dstoff += (imageWidth - w);
			}
		}

		public void imageComplete(int status) {
			if ((status == IMAGEERROR) || (status == IMAGEABORTED)) {
				consumer.imageComplete(status);
				return;
			}

			int pixels[] = new int[imageWidth * imageHeight];
			for (int z = 0; z < levels.length; z++) {
				int currentLevel = levels[z];
				int color = levelColors[z];

				setContourLine(raster, pixels, currentLevel, color, imageWidth,
						imageHeight);
			}

			int line[] = new int[imageWidth];
			for (int y = 0; y < imageHeight; y++) {
				for (int x = 0; x < imageWidth; x++) {
					line[x] = pixels[y * imageWidth + x];
				}

				consumer.setPixels(0, y, imageWidth, 1, defaultRGB, line, 0,
						imageWidth);
			} // for (int y = 0; y < imageHeight; y++) {

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
		private void setContourLine(int[] raster, int[] pixels, int level,
				int color, int w, int h) {
			int p = 0; // entrance point
			int q = p + (w * h - 1); // bottom right point
			int u = 0 + (w - 1); // top right point

			// first round
			while (true) {
				while (p < u) {
					int rgb = raster[p];
					if (rgb < level) {
						while ((raster[p] < level) && (p < u)) {
							p++;
						}
						if (raster[p] >= level) {
							pixels[p] = color;
						}
					}
					else if (rgb == level) {
						while ((raster[p] == level) && (p < u)) {
							p++;
						}
						if ((raster[p] < level) || (raster[p] > level)) {
							pixels[p] = color;
						}
					}
					else {
						while ((raster[p] > level) && (p < u)) {
							p++;
						}
						if ((raster[p] <= level)) {
							pixels[p] = color;
						}
					}
				}

				if (u == q) {
					break;
				}
				else {
					u += w;
					p++;
				}
			}
		}

	} // private class ContourFilter extends ImageFilter

	private class Rotate90Filter extends ImageFilter {
		private ColorModel defaultRGB = ColorModel.getRGBdefault();

		private double coord[] = new double[2];

		private int raster[];
		private int xoffset, yoffset;
		private int srcW, srcH;
		private int dstW, dstH;
		private int direction;

		public Rotate90Filter(int dir) {
			direction = dir;
		}

		public void transform(double x, double y, double[] retcoord) {
			if (direction == ROTATE_CW_90) {
				retcoord[0] = -y;
				retcoord[1] = x;
			}
			else {
				retcoord[0] = y;
				retcoord[1] = -x;
			}
		}

		public void itransform(double x, double y, double[] retcoord) {
			if (direction == ROTATE_CCW_90) {
				retcoord[0] = -y;
				retcoord[1] = x;
			}
			else {
				retcoord[0] = y;
				retcoord[1] = -x;
			}
		}

		public void transformBBox(Rectangle rect) {
			double minx = Double.POSITIVE_INFINITY;
			double miny = Double.POSITIVE_INFINITY;
			double maxx = Double.NEGATIVE_INFINITY;
			double maxy = Double.NEGATIVE_INFINITY;
			for (int y = 0; y <= 1; y++) {
				for (int x = 0; x <= 1; x++) {
					transform(rect.x + x * rect.width,
							rect.y + y * rect.height, coord);
					minx = Math.min(minx, coord[0]);
					miny = Math.min(miny, coord[1]);
					maxx = Math.max(maxx, coord[0]);
					maxy = Math.max(maxy, coord[1]);
				}
			}
			rect.x = (int) Math.floor(minx);
			rect.y = (int) Math.floor(miny);
			rect.width = (int) Math.ceil(maxx) - rect.x;
			rect.height = (int) Math.ceil(maxy) - rect.y;
		}

		public void setDimensions(int width, int height) {
			Rectangle rect = new Rectangle(0, 0, width, height);
			transformBBox(rect);
			xoffset = -rect.x;
			yoffset = -rect.y;
			srcW = width;
			srcH = height;
			dstW = rect.width;
			dstH = rect.height;
			raster = new int[srcW * srcH];
			consumer.setDimensions(dstW, dstH);
		}

		public void setProperties(Hashtable props) {
			props = (Hashtable) props.clone();
			Object o = props.get("filters");
			if (o == null) {
				props.put("filters", toString());
			}
			else if (o instanceof String) {
				props.put("filters", ((String) o) + toString());
			}
			consumer.setProperties(props);
		}

		public void setColorModel(ColorModel model) {
			consumer.setColorModel(defaultRGB);
		}

		public void setHints(int hintflags) {
			consumer.setHints(TOPDOWNLEFTRIGHT | COMPLETESCANLINES | SINGLEPASS
					| (hintflags & SINGLEFRAME));
		}

		public void setPixels(int x, int y, int w, int h, ColorModel model,
				byte pixels[], int off, int scansize) {
			int srcoff = off;
			int dstoff = y * srcW + x;
			for (int yc = 0; yc < h; yc++) {
				for (int xc = 0; xc < w; xc++) {
					raster[dstoff++] = model.getRGB(pixels[srcoff++] & 0xff);
				}
				srcoff += (scansize - w);
				dstoff += (srcW - w);
			}
		}

		public void setPixels(int x, int y, int w, int h, ColorModel model,
				int pixels[], int off, int scansize) {
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
					for (int xc = 0; xc < w; xc++) {
						raster[dstoff++] = model.getRGB(pixels[srcoff++]);
					}
					srcoff += (scansize - w);
					dstoff += (srcW - w);
				}
			}
		}

		public void imageComplete(int status) {
			if ((status == IMAGEERROR) || (status == IMAGEABORTED)) {
				consumer.imageComplete(status);
				return;
			}
			int pixels[] = new int[dstW];
			for (int dy = 0; dy < dstH; dy++) {
				itransform(0 - xoffset, dy - yoffset, coord);
				double x1 = coord[0];
				double y1 = coord[1];
				itransform(dstW - xoffset, dy - yoffset, coord);
				double x2 = coord[0];
				double y2 = coord[1];
				double xinc = (x2 - x1) / dstW;
				double yinc = (y2 - y1) / dstW;
				for (int dx = 0; dx < dstW; dx++) {
					int sx = (int) Math.round(x1);
					int sy = (int) Math.round(y1);
					if ((sx < 0) || (sy < 0) || (sx >= srcW) || (sy >= srcH)) {
						pixels[dx] = 0;
					}
					else {
						pixels[dx] = raster[sy * srcW + sx];
					}
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
	private class Animation
	//extends JDialog implements ActionListener, Runnable 
	{
		/*
		private static final long serialVersionUID = 6717628496771098250L;

		private final int MAX_ANIMATION_IMAGE_SIZE = 300;

		private Image[] frames = null; // a list of images for animation
		private JComponent canvas = null; // canvas to draw the image
		private Thread engine = null; // Thread animating the images
		private int numberOfImages = 0;
		private int currentFrame = 0;
		private int sleepTime = 200;
		private Image offScrImage; // Offscreen image
		private Graphics offScrGC; // Offscreen graphics context
		private JFrame owner;
		private int x0 = 0, y0 = 0; // offset of the image drawing
		*/

		/*
		public Animation(JFrame theOwner, ScalarDS dataset) {
			super(theOwner, "Animation", true);
			owner = theOwner;
			setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);

			long[] dims = dataset.getDims();
			long[] stride = dataset.getStride();
			long[] start = dataset.getStartDims();
			long[] selected = dataset.getSelectedDims();
			int[] selectedIndex = dataset.getSelectedIndex();
			int rank = dataset.getRank();
			if (animationSpeed != 0) {
				sleepTime = 1000 / animationSpeed;
			}

			// back up the start and selected size
			long[] tstart = new long[rank];
			long[] tselected = new long[rank];
			long[] tstride = new long[rank];
			System.arraycopy(start, 0, tstart, 0, rank);
			System.arraycopy(selected, 0, tselected, 0, rank);
			System.arraycopy(stride, 0, tstride, 0, rank);

			int stride_n = 1;
			int max_size = (int) Math.max(selected[selectedIndex[0]],
					selected[selectedIndex[1]]);
			if (max_size > MAX_ANIMATION_IMAGE_SIZE) {
				stride_n = (int)( (double)max_size / (double)MAX_ANIMATION_IMAGE_SIZE +0.5);
			}

			start[selectedIndex[0]] = 0;
			start[selectedIndex[1]] = 0;
			start[selectedIndex[2]] = 0;
			selected[selectedIndex[0]] = dims[selectedIndex[0]] / stride_n;
			selected[selectedIndex[1]] = dims[selectedIndex[1]] / stride_n;
			selected[selectedIndex[2]] = 1;
			stride[selectedIndex[0]] = stride_n;
			stride[selectedIndex[1]] = stride_n;
			stride[selectedIndex[2]] = 1;

			Object data3d = null;
			byte[] byteData = null;
			int h = (int) selected[selectedIndex[0]];
			int w = (int) selected[selectedIndex[1]];
			int size = w * h;

			numberOfImages = (int) dims[selectedIndex[2]];
			frames = new Image[numberOfImages];
			BufferedImage mir = bufferedImage;
			try {
				for (int i = 0; i < numberOfImages; i++) {
					bufferedImage = null; // each animation image has its
					// own image resource
					start[selectedIndex[2]] = i;

					dataset.clearData();
					try {
						data3d = dataset.read();
					}
					catch (Throwable err) {
						continue;
					}

					byteData = new byte[size];

					byteData=Tools.getBytes(data3d, dataRange, w, h, false, dataset.getFilteredImageValues(),
							true, byteData);

					frames[i] = createIndexedImage(byteData, imagePalette, w, h);
				}
			}
			finally {
				// set back to original state
				bufferedImage = mir;
				System.arraycopy(tstart, 0, start, 0, rank);
				System.arraycopy(tselected, 0, selected, 0, rank);
				System.arraycopy(tstride, 0, stride, 0, rank);
			}

			//offScrImage = owner.createImage(w, h);
			//offScrGC = offScrImage.getGraphics();
			x0 = Math.max((MAX_ANIMATION_IMAGE_SIZE - w) / 2, 0);
			y0 = Math.max((MAX_ANIMATION_IMAGE_SIZE - h) / 2, 0);

			canvas = new JComponent() {
				private static final long serialVersionUID = -6828735330511795835L;

				public void paint(Graphics g) {
					g.clearRect(0, 0, MAX_ANIMATION_IMAGE_SIZE,
							MAX_ANIMATION_IMAGE_SIZE);

					if ((offScrGC == null) || (frames == null)) {
						return;
					}

					//offScrGC.drawImage(frames[currentFrame], 0, 0, owner);
					//g.drawImage(offScrImage, x0, y0, owner);
				}
			};

			JPanel contentPane = (JPanel) getContentPane();
			contentPane.setPreferredSize(new Dimension(
					MAX_ANIMATION_IMAGE_SIZE, MAX_ANIMATION_IMAGE_SIZE));
			contentPane.setLayout(new BorderLayout());
			JButton b = new JButton("Close");
			b.setActionCommand("Close animation");
			b.addActionListener(this);
			contentPane.add(b, BorderLayout.SOUTH);

			contentPane.add(canvas, BorderLayout.CENTER);

			start();

			Point l = getParent().getLocation();
			l.x += 300;
			l.y += 200;
			setLocation(l);

			pack();
			setVisible(true);
		}
		*/

		/*
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			String cmd = e.getActionCommand();

			if (cmd.equals("Close animation")) {
				dispose(); // terminate the animation
			}
		}
		*/

		/*
		public void dispose() {
			engine = null;
			frames = null;
			super.dispose();
		}
		*/

		/**
		 * No need to clear anything; just paint.
		 */
		/*
		public void update(Graphics g) {
			paint(g);
		}
		*/

		/**
		 * Paint the current frame
		 */
		/*
		public void paint(Graphics g) {
			canvas.paint(g);
		}
		*/

		/**
		 * Start the applet by forking an animation thread.
		 */
		/*
		private void start() {
			engine = new Thread(this);
			engine.start();
		}
		*/

		/**
		 * Run the animation. This method is called by class Thread.
		 * 
		 * @see java.lang.Thread
		 */
		/*
		public void run() {
			Thread me = Thread.currentThread();

			if ((frames == null) || (canvas == null)) {
				return;
			}

			while (me == engine) {
				if (++currentFrame >= numberOfImages)
					currentFrame = 0;
				repaint();
				this.getToolkit().sync(); // Force it to be drawn *now*.
				try {
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e) {
					log.debug("Thread.sleep({}):", sleepTime, e);
				}
			}
		} // public void run() {
		*/
	} // private class Animation extends JDialog

	private class DataRangeDialog
	//extends JDialog implements ActionListener, ChangeListener, PropertyChangeListener 
	{
		/*
		final int NTICKS = 10;
		double tickRatio = 1;
		final int W = 500, H = 400;
		double[] minmax_current = {0, 0};
		double min, max, min_org, max_org;
		final double[] minmax_previous = {0, 0};
		final double[] minmax_dist = {0,0};
		JSlider minSlider, maxSlider;
		JFormattedTextField minField, maxField;
		*/

		/*
		public DataRangeDialog(JFrame theOwner, double[] minmaxCurrent, 
				double[] minmaxOriginal, final int[] dataDist) 
		{
			super(theOwner, "Image Value Range", true);

			Tools.findMinMax(dataDist, minmax_dist, null);

			if ((minmaxCurrent == null) || (minmaxCurrent.length <= 1)) {
				minmax_current[0] = 0;
				minmax_current[1] = 255;
			}
			else {
				if (minmaxCurrent[0] == minmaxCurrent[1]) {
					Tools.findMinMax(data, minmaxCurrent, dataset.getFillValue());
				}

				minmax_current[0] = minmaxCurrent[0];
				minmax_current[1] = minmaxCurrent[1];
			}

			minmax_previous[0] = min = minmax_current[0];
			minmax_previous[1] = max = minmax_current[1];
			min_org = originalRange[0];
			max_org = originalRange[1];

			tickRatio = (max_org-min_org)/(double)NTICKS;

			final DecimalFormat numberFormat = new DecimalFormat("#.##E0");
			NumberFormatter formatter = new NumberFormatter(numberFormat);
			formatter.setMinimum(new Double(min));
			formatter.setMaximum(new Double(max));

			minField = new JFormattedTextField(formatter);
			minField.addPropertyChangeListener(this);
			minField.setValue(new Double(min));
			maxField = new JFormattedTextField(formatter);
			maxField.addPropertyChangeListener(this);
			maxField.setValue(new Double(max));

			minSlider = new JSlider(JSlider.HORIZONTAL, 0, NTICKS, 0);
			minSlider.setMajorTickSpacing(1);
			minSlider.setPaintTicks(true);
			minSlider.setPaintLabels(false);
			minSlider.addChangeListener(this);
			minSlider.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

			maxSlider = new JSlider(JSlider.HORIZONTAL, 0, NTICKS, NTICKS);
			maxSlider.setMajorTickSpacing(1);
			maxSlider.setPaintTicks(true);
			maxSlider.setPaintLabels(false);
			maxSlider.addChangeListener(this);
			maxSlider.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

			JPanel contentPane = (JPanel) getContentPane();
			contentPane.setLayout(new BorderLayout(5, 5));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			contentPane.setPreferredSize(new Dimension(W, H));

			JPanel minPane = new JPanel();
			minPane.setBorder(new TitledBorder("Lower Bound"));
			minPane.setLayout(new BorderLayout());
			minPane.add(minField, BorderLayout.CENTER);
			minPane.add(minSlider, BorderLayout.SOUTH);

			JPanel maxPane = new JPanel();
			maxPane.setBorder(new TitledBorder("Upper Bound"));
			maxPane.setLayout(new BorderLayout());
			maxPane.add(maxField, BorderLayout.CENTER);
			maxPane.add(maxSlider, BorderLayout.SOUTH);

			JPanel chartPane = new JPanel() {
				int numberOfPoints = dataDist.length;
				int gap = 5;
				int xgap = 2 * gap;
				double xmin = originalRange[0];
				double xmax = originalRange[1];

				public void paint(Graphics g) {
					int h = H/3 -50;
					int w = W;
					int xnpoints = Math.min(10, numberOfPoints - 1);

					// draw the X axis
					g.drawLine(xgap, h, w + xgap, h);

					// draw x labels
					double xp = 0, x = xmin;
					double dw = (double) w / (double) xnpoints;
					double dx = (xmax - xmin) / xnpoints;
					for (int i = 0; i <= xnpoints; i++) {
						x = xmin + i * dx;
						xp = xgap + i * dw;
						g.drawLine((int) xp, h, (int) xp, h - 5);
						g.drawString(numberFormat.format(x), (int) xp - 5, h + 20);
					}

					Color c = g.getColor();
					double yp, ymin=minmax_dist[0], dy=minmax_dist[1]-minmax_dist[0];
					if (dy<=0)
						dy =1;

					xp = xgap;
					yp = 0;
					g.setColor(Color.blue);
					int barWidth = w / numberOfPoints;
					if (barWidth <= 0) {
						barWidth = 1;
					}
					dw = (double) w / (double) numberOfPoints;

					for (int j = 0; j < numberOfPoints; j++) {
						xp = xgap + j * dw;
						yp = (int) (h * (dataDist[j] - ymin) / dy);
						g.fillRect((int) xp, (int) (h - yp), barWidth, (int) yp);
					}

					g.setColor(c); // set the color back to its default
				} // public void paint(Graphics g)
			} ;

			JPanel mainPane = new JPanel();
			mainPane.setLayout(new GridLayout(3, 1, 5, 5));
			mainPane.add(chartPane);
			mainPane.add(minPane);
			mainPane.add(maxPane);
			contentPane.add(mainPane, BorderLayout.CENTER);

			// add OK and CANCEL buttons
			JPanel confirmP = new JPanel();
			JButton button = new JButton("   Ok   ");
			button.setMnemonic(KeyEvent.VK_O);
			button.setActionCommand("Ok");
			button.addActionListener(this);
			confirmP.add(button);
			button = new JButton("Cancel");
			button.setMnemonic(KeyEvent.VK_C);
			button.setActionCommand("Cancel");
			button.addActionListener(this);
			confirmP.add(button);
			button = new JButton("Apply");
			button.setMnemonic(KeyEvent.VK_A);
			button.setActionCommand("Apply");
			button.addActionListener(this);
			confirmP.add(button);
			contentPane.add(confirmP, BorderLayout.SOUTH);
			contentPane.add(new JLabel(" "), BorderLayout.NORTH);

			if (min==max) {
				minSlider.setEnabled(false);
				maxSlider.setEnabled(false);
			}

			Point l = getParent().getLocation();
			Dimension d = getParent().getPreferredSize();
			l.x += 300;
			l.y += 200;
			setLocation(l);
			pack();
			setVisible(true);
		}
		*/

		/*
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();

			if (cmd.equals("Ok")) {
				minmax_current[0] = ((Number) minField.getValue()).doubleValue();
				minmax_current[1] = ((Number) maxField.getValue()).doubleValue();

				this.dispose();
			}
			if (cmd.equals("Apply")) {
				minmax_previous[0] = minmax_current[0];
				minmax_previous[1] = minmax_current[1];

				minmax_current[0] = ((Number) minField.getValue()).doubleValue();
				minmax_current[1] = ((Number) maxField.getValue()).doubleValue();

				applyDataRange(minmax_current);
				minmax_current[0] = minmax_current[1] = 0;
			}
			else if (cmd.equals("Cancel")) {

				minmax_current[0] = minmax_previous[0];
				minmax_current[1] = minmax_previous[1];

				applyDataRange(minmax_previous);

				this.dispose();
			}
		}
		*/

		/** Listen to the slider. */
		/*
		public void stateChanged(ChangeEvent e) {
			Object source = e.getSource();

			if (!(source instanceof JSlider)) {
				return;
			}

			JSlider slider = (JSlider) source;
			if (!slider.isEnabled())
				return;

			double value = slider.getValue();
			if (slider.equals(minSlider)) {
				double maxValue = maxSlider.getValue();
				if (value > maxValue) {
					value = maxValue;
					slider.setValue((int)value);
				}

				minField.setValue(new Double(value*tickRatio+min_org));
			}
			else if (slider.equals(maxSlider)) {
				double minValue = minSlider.getValue();
				if (value < minValue) {
					value = minValue;
					slider.setValue((int)value);
				}
				maxField.setValue(new Double(value*tickRatio+min_org));
			}
		}
		*/

		/**
		 * Listen to the text field. This method detects when the value of the
		 * text field changes.
		 */
		/*
		public void propertyChange(PropertyChangeEvent e) {
			Object source = e.getSource();
			if ("value".equals(e.getPropertyName())) {
				Number num = (Number) e.getNewValue();
				if (num == null) {
					return;
				}
				double value = num.doubleValue();

				if (source.equals(minField) && (minSlider != null) && minSlider.isEnabled()) {
					if (value > max_org) {
						value = max_org;
						minField.setText(String.valueOf(value));
					}

					minSlider.setValue((int) ((value-min_org)/tickRatio));
				}
				else if (source.equals(maxField) && (maxSlider != null)  && minSlider.isEnabled()) {
					if (value < min_org) {
						value = min_org;
						maxField.setText(String.valueOf(value));
					}
					//minmax[1] = value;
					maxSlider.setValue((int) ((value-min_org)/tickRatio));
				}
			}
		}
		*/

		/*
		public double[] getRange() {
			return minmax_current;
		}
		*/
	} // private class DataRangeDialog extends JDialog implements ActionListener

	private class ContrastSlider 
	//extends JDialog 
	//implements ActionListener, ChangeListener, PropertyChangeListener 
	{
		/*
		private static final long serialVersionUID = -3002524363351111565L;
		JSlider brightSlider, contrastSlider;
		JFormattedTextField brightField, contrastField;
		ImageProducer imageProducer;
		double[] autoGainBias = {0, 0};
		int bLevel=0, cLevel=0;
		*/

		/*
		public ContrastSlider(JFrame theOwner, ImageProducer producer) 
		{
			super(theOwner, "Brightness/Contrast", true);
			String bLabel = "Brightness", cLabel="Contrast";

			imageProducer = producer;

			if (doAutoGainContrast && gainBias!= null) {
				bLabel = "Bias";
				cLabel="Gain";
				this.setTitle(bLabel+"/"+cLabel);
			}

			java.text.NumberFormat numberFormat = java.text.NumberFormat
			.getNumberInstance();
			NumberFormatter formatter = new NumberFormatter(numberFormat);

			formatter.setMinimum(new Integer(-100));
			formatter.setMaximum(new Integer(100));
			brightField = new JFormattedTextField(formatter);
			brightField.addPropertyChangeListener(this);
			brightField.setValue(new Integer(0));

			brightSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
			brightSlider.setMajorTickSpacing(20);
			brightSlider.setPaintTicks(true);
			brightSlider.setPaintLabels(true);
			brightSlider.addChangeListener(this);
			brightSlider
			.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

			formatter = new NumberFormatter(numberFormat);
			formatter.setMinimum(new Integer(-100));
			formatter.setMaximum(new Integer(100));
			contrastField = new JFormattedTextField(formatter);
			contrastField.addPropertyChangeListener(this);
			contrastField.setValue(new Integer(0));

			contrastSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
			contrastSlider.setMajorTickSpacing(20);
			contrastSlider.setPaintTicks(true);
			contrastSlider.setPaintLabels(true);
			contrastSlider.addChangeListener(this);
			contrastSlider.setBorder(BorderFactory.createEmptyBorder(0, 0, 10,0));

			JPanel contentPane = (JPanel) getContentPane();
			contentPane.setLayout(new BorderLayout(5, 5));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			contentPane.setPreferredSize(new Dimension(500, 300));

			JPanel brightPane = new JPanel();
			brightPane.setBorder(new TitledBorder(bLabel+"%"));
			brightPane.setLayout(new BorderLayout());
			brightPane.add(brightField, BorderLayout.NORTH);
			brightPane.add(brightSlider, BorderLayout.CENTER);

			JPanel contrastPane = new JPanel();
			contrastPane.setBorder(new TitledBorder(cLabel+"%"));
			contrastPane.setLayout(new BorderLayout());
			contrastPane.add(contrastField, BorderLayout.NORTH);
			contrastPane.add(contrastSlider, BorderLayout.CENTER);

			JPanel mainPane = new JPanel();
			mainPane.setLayout(new GridLayout(2, 1, 5, 5));
			mainPane.add(brightPane);
			mainPane.add(contrastPane);
			contentPane.add(mainPane, BorderLayout.CENTER);

			// add OK and CANCEL buttons
			JPanel confirmP = new JPanel();
			JButton button = new JButton("   Ok   ");
			button.setMnemonic(KeyEvent.VK_O);
			button.setActionCommand("Ok_brightness_change");
			button.addActionListener(this);
			confirmP.add(button);
			button = new JButton("Cancel");
			button.setMnemonic(KeyEvent.VK_C);
			button.setActionCommand("Cancel_brightness_change");
			button.addActionListener(this);
			confirmP.add(button);

			button = new JButton("Apply");
			button.setMnemonic(KeyEvent.VK_A);
			button.setActionCommand("Apply_brightness_change");
			button.addActionListener(this);
			confirmP.add(button);

			contentPane.add(confirmP, BorderLayout.SOUTH);
			contentPane.add(new JLabel(" "), BorderLayout.NORTH);

			Point l = getParent().getLocation();
			Dimension d = getParent().getPreferredSize();
			l.x += 300;
			l.y += 200;
			setLocation(l);
			pack();
		}
		*/

		/*
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();

			if (cmd.equals("Ok_brightness_change")
					|| cmd.equals("Apply_brightness_change")) {
				int b = ((Number) brightField.getValue()).intValue();
				int c = ((Number) contrastField.getValue()).intValue();

				applyBrightContrast(b, c);

				if (cmd.startsWith("Ok")) {
					bLevel = b;
					cLevel = c;
					setVisible(false);
				}
			}
			else if (cmd.equals("Cancel_brightness_change")) {
				applyBrightContrast(bLevel, cLevel);
				setVisible(false);
			}
		}
		*/

		/** Listen to the slider. */
		/*
		public void stateChanged(ChangeEvent e) {
			Object source = e.getSource();

			if (!(source instanceof JSlider)) {
				return;
			}

			JSlider slider = (JSlider) source;
			int value = slider.getValue();
			if (slider.equals(brightSlider)) {
				brightField.setValue(new Integer(value));
			}
			else if (slider.equals(contrastSlider)) {
				contrastField.setValue(new Integer(value));
			}
		}
		*/

		/**
		 * Listen to the text field. This method detects when the value of the
		 * text field changes.
		 */
		/*
		public void propertyChange(PropertyChangeEvent e) {
			Object source = e.getSource();
			if ("value".equals(e.getPropertyName())) {
				Number num = (Number) e.getNewValue();
				if (num == null) {
					return;
				}

				double value = num.doubleValue();
				if (value > 100) {
					value = 100;
				}
				else if (value < -100) {
					value = -100;
				}

				if (source.equals(brightField) && (brightSlider != null)) {
					brightSlider.setValue((int) value);
				}
				else if (source.equals(contrastField)
						&& (contrastSlider != null)) {
					contrastSlider.setValue((int) value);
				}
			}
		}
		*/

		/*
		private void applyBrightContrast(int blevel, int clevel) {
			// do not separate autogain and simple contrast process
			//            ImageFilter filter = new BrightnessFilter(blevel, clevel);
			//            image = createImage(new FilteredImageSource(imageProducer, filter));
			//            imageComponent.setImage(image);
			//            zoomTo(zoomFactor);

			// separate autodain and simple contrast process
			if (doAutoGainContrast && gainBias!= null) {
				autoGainBias[0] = gainBias[0]*(1+((double)clevel)/100.0);
				autoGainBias[1] = gainBias[1]*(1+((double)blevel)/100.0);
				applyAutoGain(autoGainBias, null);
			} 
			else {
				ImageFilter filter = new BrightnessFilter(blevel, clevel);
				//image = createImage(new FilteredImageSource(imageProducer, filter));
				imageComponent.setImage(image);
				zoomTo(zoomFactor);           
			}
		}
		*/
	} // private class ContrastSlider extends JDialog implements ActionListener
}
