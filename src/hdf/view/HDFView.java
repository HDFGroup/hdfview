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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Font;

import org.eclipse.swt.dnd.*;

import swing2swt.layout.BorderLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * HDFView is the main class of this HDF visual tool. It is used to layout the
 * graphical components of the hdfview. The major GUI components of the HDFView
 * include Menubar, Toolbar, TreeView, ContentView, and MessageArea.
 * <p>
 * The HDFView is designed in such a way that it does not have direct access to
 * the HDF library. All the HDF library access is done through HDF objects.
 * Therefore, the HDFView package depends on the object package but not the
 * library package. The source code of the view package (hdf.view) should
 * be complied with the library package (hdf.hdflib and hdf.hdf5lib).
 * 
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class HDFView implements ViewManager, DropTargetListener {
	private static final long     serialVersionUID = 2211017444445918998L;	
	
	private static Display display = new Display();
	private Shell mainWindow;
	
	/* The directory where HDFView is installed */
	private String					rootDir;
	
	/* The current working directory */
	private String					currentDir;
	
	/* The current working file */
	private String					currentFile;
	
	/* The view properties */
	private ViewProperties			props;
	
	/* A list of tree view implementations. */
    private static List<String>		treeViews;

    /* A list of image view implementations. */
    private static List<String>		imageViews;

    /* A list of tree table implementations. */
    private static List<?>			tableViews;

    /* A list of Text view implementations. */
    private static List<String>		textViews;

    /* A list of metadata view implementations. */
    private static List<?>			metaDataViews;

    /* A list of palette view implementations. */
    private static List<?>			paletteViews;
	
	/* A list of help view implementations. */
	private static List<?>			helpViews;
	
	/* The list of GUI components related to HDF4 */
	private final List<MenuItem> 	h4GUIs;
	
	/* The list of GUI components related to HDF5 */
	private final List<MenuItem> 	h5GUIs;
	
	/* The list of GUI components related to editing */
	//private final List<?>			editGUIs;
	
	/* GUI component: the TreeView */
	private TreeView				treeView;
	
	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HDFView.class);
	
	private static final String 	HDF4_VERSION = "HDF 4.2.10";
	private static final String 	HDF5_VERSION = "HDF5 1.8.13";
	private static final String 	HDFVIEW_VERSION = "HDFView 3.99";
	private static final String 	HDFVIEW_USERSGUIDE_URL = "http://www.hdfgroup.org/products/java/hdfview/UsersGuide/index.html";
	private static final String 	JAVA_COMPILER = "jdk 1.7";
	private static final String		JAVA_VER_INFO = "Compiled at " + JAVA_COMPILER + "\nRunning at " + System.getProperty("java.version");
	
	private static final String		aboutHDFView = "HDF Viewer, " + "Version " + ViewProperties.VERSION + "\n"
    + "For " + System.getProperty("os.name") + "\n\n"
    + "Copyright " + '\u00a9' + " 2006-2015 The HDF Group.\n"
    + "All rights reserved.";
	
	/* String buffer holding the status message */
	private StringBuffer 			message;
	
	/* String buffer holding the metadata information */
	private StringBuffer			metadata;
	
	/* The list of most recent files */
	// private Vector					recentFiles;
	
	/* GUI component: Container for the button toolbar and url toolbar */
	private Composite 				toolbarContainer;
	
	/* GUI component: Area to display data content */
	private Composite				contentArea;
	
	/* GUI component: The text area for showing status messages */
	private Text					statusArea;
	
	/* GUI component: The text area for quick attribute view */
	private Text					attributeArea;
	
	/* GUI component: To add and display URLs */
	private Combo					url_bar;
	
	/* GUI component: A list of current data windows */
	//private final Menu				windowMenu;
	
	/* GUI component: File menu on the menubar */
	//private final Menu				fileMenu;
	
	/* The offset when a new dataview is added into the main window. */
	private int						frameOffset;
	
	private UserOptionsDialog		userOptionDialog;
	
	private Constructor<?>			ctrSrbFileDialog 	= null;
	
	private Dialog					srbFileDialog 		= null;
	
	/**
     * Constructs HDFView with a given root directory, where the HDFView is
     * installed, and opens the given files in the viewer.
     * <p>
     * 
     * @param root
     *            the directory where the HDFView is installed.
     * @param flist
     *            a list of files to open.
     */	
	public HDFView(Display D, String root, List<File> flist, int width, int height, int x, int y) {		
		log.debug("Root is {}", root);
		
		rootDir = root;
		currentFile = null;
		//frameOffset = 0;
		//userOptionsDialog = null;
		//ctrSrbFileDialog = null;
		
		h4GUIs = new Vector<MenuItem>();
		h5GUIs = new Vector<MenuItem>();
		//editGUIs = new Vector<Object>();
		
		ViewProperties.loadIcons();
		ViewProperties.loadExtClass();
		
		props = new ViewProperties(rootDir);
		try {
			props.load();
		} catch (Exception ex) {
			log.debug("Failed to load View Properties from {}", rootDir);
		}
		
		// recentFiles = ViewProperties.getMRF();
		currentDir = ViewProperties.getWorkDir();
		if (currentDir == null)
		   currentDir = System.getProperty("user.home");
		
		log.info("Current directory is {}", currentDir);
		
		treeViews = ViewProperties.getTreeViewList();
        metaDataViews = ViewProperties.getMetaDataViewList();
        textViews = ViewProperties.getTextViewList();
        tableViews = ViewProperties.getTableViewList();
        imageViews = ViewProperties.getImageViewList();
        paletteViews = ViewProperties.getPaletteViewList();
        helpViews = ViewProperties.getHelpViewList();
		
        int n = treeViews.size();
        Class<?> theClass = null;
        for (int i = 0; i < n; i++) {
        	// Use the first available treeview
        	String className = treeViews.get(i);
        	
        	// Enables use of JHDF5 in JNLP (Web Start) applications, the system
        	// class loader with reflection first.
        	try {
        		theClass = Class.forName(className);
        	} catch (Exception ex) {
        		try {
        			theClass = ViewProperties.loadExtClass().loadClass(className);
        		} catch (Exception ex2) {
        			theClass = null;
        		}
        	}
        	
        	if (theClass != null) break;
        }
        
        if (theClass != null) {
        	try {
        		@SuppressWarnings("rawtypes")
        		Class[] paramClass = { Class.forName("hdf.view.ViewManager") };
        		Constructor<?> constructor = theClass.getConstructor(paramClass);
        		Object[] paramObj = { this };
        		treeView = (TreeView) constructor.newInstance(paramObj);
        	} catch (Exception ex) {
        		treeView = null;
        	}
        }
        
        // Could not load user's treeview, use default treeview.
        // if (treeView == null) treeView = new DefaultTreeView(this);
        
        // Initialize all GUI components
		createMainWindow(width, height, x, y);
		
		try {
			Font font = null;
			String fType = ViewProperties.getFontType();
			int fSize = ViewProperties.getFontSize();
			
			try {
				font = new Font(display, fType, fSize, SWT.NORMAL);
			} catch (Exception ex) {
				font = null;
			}
			
			if (font != null)
				updateFontSize(font);
			
		} catch (Exception ex) {
			log.debug("Failed to load Font properties");
		}
		
		// new DropTarget(display, DND.DROP_COPY);
		
		// Make sure all GUI components are in place before
		// opening any files
		mainWindow.pack();
		
		/*
		int nfiles = flist.size();
		File theFile = null;
		for (int i = 0; i < nfiles; i++) {
			theFile = flist.get(i);
			
			if (theFile.isFile()) {
				currentDir = theFile.getParentFile().getAbsolutePath();
				currentFile = theFile.getAbsolutePath();
				
				try {
					treeView.openFile(currentFile, FileFormat.WRITE);
					
					try {
						url_bar.remove(currentFile);
						url_bar.add(currentFile, 0);
						url_bar.select(0);
					} catch (Exception ex) {
						log.info("Failed to update urlBar with {}", currentFile);
					}
				} catch (Exception ex) {
					showStatus(ex.toString());
				}
			} else {
				currentDir = theFile.getAbsolutePath();
			}
			
			log.info("CurrentDir is {}", currentDir);
		} */
		
		
		if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) == null)
			setEnabled(h4GUIs, false);
		
		if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) == null)
			setEnabled(h5GUIs, false);
		
		// Display the window
		mainWindow.open();
		
		while(!mainWindow.isDisposed()) {
			if(!D.readAndDispatch()){
				D.sleep();
			}
		}
		
		try {
			props.save();
		} catch (Exception ex) {
			
		}
		
		// Close all open files
		try {
			List<FileFormat> filelist = treeView.getCurrentFiles();
			
			if((filelist != null) && (filelist.size() > 0)) {
				Object[] files = filelist.toArray();
				
				for (int i = 0; i < files.length; i++) {
					try {
						treeView.closeFile((FileFormat) files[i]);
					} catch (Throwable ex) {
						continue;
					}
				}
			}
		} catch (Exception ex) {
			
		}
		
		display.dispose();
	}
	
	/**
     * Creates and lays out GUI components.
     * 
     * <pre>
     * ||=========||=============================||
     * ||         ||                             ||
     * ||         ||                             ||
     * || TreeView||       ContentPane           ||
     * ||         ||                             ||
     * ||=========||=============================||
     * ||            Message Area                ||
     * ||========================================||
     * </pre>
     */
	private void createMainWindow(int width, int height, int x, int y) {
		
		// Create a new display window
		mainWindow = new Shell(display);
		mainWindow.setImage(ViewProperties.getHdfIcon());
		mainWindow.setText(HDFVIEW_VERSION);
		mainWindow.setLayout(new BorderLayout(0, 0));
		
		try {
			mainWindow.setImage(ViewProperties.getHdfIcon());
		} catch (Exception ex) {
			log.debug("Failed to set window icon");
		}
		
		createMenuBar();
		createToolBar();
		createUrlToolbar();
		createContentArea();
		
		// Set size of main window
		// float inset = 0.17f; // for UG only.
		float inset = 0.04f;
		Point winDim = new Point(width, height);
		
		// If given height and width are too small, adjust accordingly
		if (height <= 300) {
			winDim.y = (int) ((1 - 2 * inset) * mainWindow.getSize().y);
		}
		
		if (width <= 300) {
			winDim.x = (int) (0.9 * (double) mainWindow.getSize().y);
		}
		
		// TEST
        //if (treeView.getClass().getName().startsWith("ext.erdc")) {
        //    topSplitPane.setDividerLocation(500);
        //    winDim.x = (int) (0.9 * mainWindow.getSize().x);
        //    winDim.y = (int) (winDim.x * 0.618);
        //}
        
        // splitPane.setDividerLocation(d.height - 180);	
		mainWindow.setLocation(0, 0);
		mainWindow.setMinimumSize(winDim.x, winDim.y);
		
		log.info("Main Window created");
	}
	
	private void createMenuBar() {
		Menu menu = new Menu(mainWindow, SWT.BAR);
		mainWindow.setMenuBar(menu);
		
		MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText("&File");
		
		Menu menu_2 = new Menu(menuItem);
		menuItem.setMenu(menu_2);
		
		MenuItem menuItem_1 = new MenuItem(menu_2, SWT.NONE);
		menuItem_1.setText("&Open \tCtrl-O");
		menuItem_1.setAccelerator(SWT.MOD1 + 'O');
		menuItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openLocalFile(null, FileFormat.WRITE);
			}
		});
		
		if(!ViewProperties.isReadOnly()) {
			MenuItem menuItem_2 = new MenuItem(menu_2, SWT.NONE);
			menuItem_2.setText("Open &Read-Only");
			menuItem_2.setAccelerator(SWT.MOD1 + 'R');
			menuItem_2.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					openLocalFile(null, FileFormat.READ);
				}
			});
		}
		
		// boolean isSrbSupported = true;
        // try {
        // Class.forName("hdf.srb.H5SRB");
        // Class.forName("hdf.srb.SRBFileDialog");
        // } catch (Throwable ex) {isSrbSupported = false;}
        //
        // if (isSrbSupported) {
        // item = new JMenuItem( "Open from iRODS");
        // item.setMnemonic(KeyEvent.VK_S);
        // item.addActionListener(this);
        // item.setActionCommand("Open from irods");
        // fileMenu.add(item);
        // }

		new MenuItem(menu_2, SWT.SEPARATOR);
		
		MenuItem menuItem_4 = new MenuItem(menu_2, SWT.CASCADE);
		menuItem_4.setText("New");
		
		Menu menu_3 = new Menu(menuItem_4);
		menuItem_4.setMenu(menu_3);
		
		MenuItem menuItem_5 = new MenuItem(menu_3, SWT.NONE);
		menuItem_5.setText("HDF&4");
		h4GUIs.add(menuItem_5);
		menuItem_5.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				NewFileDialog dialog = new NewFileDialog(null, currentDir, FileFormat.FILE_TYPE_HDF4, treeView.getCurrentFiles());
				dialog.setName("newfiledialog");
				String filename = dialog.getFile();
				
				if(!dialog.isFileCreated() || filename == null)
					return;
				
				try {
					treeView.openFile(filename, FileFormat.WRITE);
					currentFile = filename;
					
					try {
						url_bar.remove(filename);
						url_bar.add(filename, 0);
						url_bar.select(0);
					} catch (Exception ex) {
						
					}
				} catch (Exception ex) {
					display.beep();
					MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
					error.setText(mainWindow.getText());
					error.setMessage(ex.getMessage() + "\n" + filename);
					error.open();
				}
			}
		});
		
		MenuItem menuItem_6 = new MenuItem(menu_3, SWT.NONE);
		menuItem_6.setText("HDF&5");
		h5GUIs.add(menuItem_6);
		menuItem_6.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				NewFileDialog dialog = new NewFileDialog(null, currentDir, FileFormat.FILE_TYPE_HDF5, treeView.getCurrentFiles());
				dialog.setName("newfiledialog");
				String filename = dialog.getFile();
				
				if(!dialog.isFileCreated() || filename == null)
					return;
				
				try {
					treeView.openFile(filename, FileFormat.WRITE);
					currentFile = filename;
					
					try {
						url_bar.remove(filename);
						url_bar.add(filename, 0);
						url_bar.select(0);
					} catch (Exception ex) {
						
					}
				} catch (Exception ex) {
					display.beep();
					MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
					error.setText(mainWindow.getText());
					error.setMessage(ex.getMessage() + "\n" + filename);
					error.open();
				}
			}
		});
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		MenuItem menuItem_8 = new MenuItem(menu_2, SWT.NONE);
		menuItem_8.setText("&Close");
		menuItem_8.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				closeFile(treeView.getSelectedFile());
			}
		});
		
		MenuItem menuItem_9 = new MenuItem(menu_2, SWT.NONE);
		menuItem_9.setText("Close &All");
		menuItem_9.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				closeAllWindows();
				
				List<FileFormat> files = treeView.getCurrentFiles();
				while (!files.isEmpty()) {
					try {
						treeView.closeFile(files.get(0));
					} catch (Exception ex) {
						
					}
				}
				
				currentFile = null;
				attributeArea.setText("");
			}
		});
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		MenuItem menuItem_11 = new MenuItem(menu_2, SWT.NONE);
		menuItem_11.setText("&Save");
		/*menuItem_11.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected() {
				// Save what has been changed in memory into file
				try {
					FileFormat file = treeView.getSelectedFile();
					//List<> views = getDataViews(); Was JInternalFrames, need different
					Object theView = null;
					TableView tableView = null;
					TextView textView = null;
					FileFormat theFile = null;
					
					if (views != null) {
						int n = views.size();
						
						for (int i = 0; i < n; i++) {
							theView = views.get(i);
							
							if (theView instanceof TableView) {
								tableView = (TableView) theView;
								theFile = tableView.getDataObject().getFileFormat();
								if (file.equals(theFile))
									tableView.updateValueInFile();
							}
							else if (theView instanceof TextView) {
								textView = (TextView) theView;
								theFile = textView.getDataObject().getFileFormat();
								if (file.equals(theFile))
									textView.updateValueInFile();
							}
						}
					}
				} catch (Exception ex) {
					display.beep();
					MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
					error.setText(mainWindow.getText());
					error.setMessage(ex.getMessage());
					error.open();
				}
			}
		});*/
		
		MenuItem menuItem_12 = new MenuItem(menu_2, SWT.NONE);
		menuItem_12.setText("S&ave As");
		menuItem_12.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					treeView.saveFile(treeView.getSelectedFile());
				} catch (Exception ex) {
					display.beep();
					MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
					error.setText(mainWindow.getText());
					error.setMessage(ex.getMessage());
					error.open();
				}
			}
		});
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		MenuItem menuItem_14 = new MenuItem(menu_2, SWT.NONE);
		menuItem_14.setText("E&xit \tCtrl-Q");
		menuItem_14.setAccelerator(SWT.MOD1 + 'Q');
		menuItem_14.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				display.dispose();
			}
		});
		
		MenuItem menuItem_15 = new MenuItem(menu, SWT.CASCADE);
		menuItem_15.setText("&Window");
		
		Menu menu_4 = new Menu(menuItem_15);
		menuItem_15.setMenu(menu_4);
		
		MenuItem menuItem_16 = new MenuItem(menu_4, SWT.NONE);
		menuItem_16.setText("&Cascade");
		menuItem_16.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				cascadeWindows();
			}
		});
		
		MenuItem menuItem_17 = new MenuItem(menu_4, SWT.NONE);
		menuItem_17.setText("&Tile");
		menuItem_17.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tileWindows();
			}
		});
		
		new MenuItem(menu_4, SWT.SEPARATOR);
		
		MenuItem menuItem_19 = new MenuItem(menu_4, SWT.NONE);
		menuItem_19.setText("Close &Window");
		menuItem_19.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (mainWindow.getShells().length <= 0 || (display.getActiveShell().equals(mainWindow)))
					return;
				
				display.getActiveShell().dispose();
			}
		});
		
		MenuItem menuItem_20 = new MenuItem(menu_4, SWT.NONE);
		menuItem_20.setText("Close &All");
		menuItem_20.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				closeAllWindows();
			}
		});
		
		MenuItem menuItem_21 = new MenuItem(menu, SWT.CASCADE);
		menuItem_21.setText("&Tools");
		
		Menu menu_5 = new Menu(menuItem_21);
		menuItem_21.setMenu(menu_5);
		
		MenuItem menuItem_22 = new MenuItem(menu_5, SWT.CASCADE);
		menuItem_22.setText("Convert Image To");
		
		Menu menu_6 = new Menu(menuItem_22);
		menuItem_22.setMenu(menu_6);
		
		MenuItem menuItem_23 = new MenuItem(menu_6, SWT.NONE);
		menuItem_23.setText("HDF4");
		menuItem_23.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				convertFile(Tools.FILE_TYPE_IMAGE, FileFormat.FILE_TYPE_HDF4);
			}
		});
		h4GUIs.add(menuItem_23);
		
		MenuItem menuItem_24 = new MenuItem(menu_6, SWT.NONE);
		menuItem_24.setText("HDF5");
		menuItem_24.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				convertFile(Tools.FILE_TYPE_IMAGE, FileFormat.FILE_TYPE_HDF5);
			}
		});
		h5GUIs.add(menuItem_24);
		
		new MenuItem(menu_5, SWT.SEPARATOR);
		
		MenuItem menuItem_26 = new MenuItem(menu_5, SWT.NONE);
		menuItem_26.setText("User &Options");
		menuItem_26.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (userOptionDialog == null)
					// userOptionDialog = new UserOptionsDialog(display, rootDir);
				
				userOptionDialog.setVisible(true);
				
				if (userOptionDialog.isWorkDirChanged())
					currentDir = ViewProperties.getWorkDir();
				
				if (userOptionDialog.isFontChanged()) {
					Font font = null;
					
					try {
						font = new Font(display, ViewProperties.getFontType(), ViewProperties.getFontSize(), SWT.NORMAL);
					} catch (Exception ex) {
						font = null;
					}
					
					if (font != null)
						updateFontSize(font);
				}
			}
		});
		
		new MenuItem(menu_5, SWT.SEPARATOR);
		
		MenuItem menuItem_28 = new MenuItem(menu_5, SWT.NONE);
		menuItem_28.setText("&Register File Format");
		menuItem_28.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				registerFileFormat();
			}
		});
		
		MenuItem menuItem_29 = new MenuItem(menu_5, SWT.NONE);
		menuItem_29.setText("&Unregister File Format");
		menuItem_29.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				unregisterFileFormat();
			}
		});
		
		MenuItem menuItem_30 = new MenuItem(menu, SWT.CASCADE);
		menuItem_30.setText("&Help");
		
		Menu menu_7 = new Menu(menuItem_30);
		menuItem_30.setMenu(menu_7);
		
		MenuItem menuItem_31 = new MenuItem(menu_7, SWT.NONE);
		menuItem_31.setText("&User's Guide");
		menuItem_31.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.program.Program.launch(HDFVIEW_USERSGUIDE_URL);
			}
		});
		
		/*
		if ((helpViews != null) && (helpViews.size() > 0)) {
            int n = helpViews.size();
            for (int i = 0; i < n; i++) {
                HelpView theView = (HelpView) helpViews.get(i);
                item = new JMenuItem(theView.getLabel());
                item.setActionCommand(theView.getActionCommand());
                item.addActionListener(this);
                menu.add(item);
            }
            menu.addSeparator();
        }*/
		
		new MenuItem(menu_7, SWT.SEPARATOR);
		
		MenuItem menuItem_33 = new MenuItem(menu_7, SWT.NONE);
		menuItem_33.setText("HDF&4 Library Version");
		h4GUIs.add(menuItem_33);
		menuItem_22.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hdfLibraryVersionInfo(HDF4_VERSION);
			}
		});
		
		MenuItem menuItem_34 = new MenuItem(menu_7, SWT.NONE);
		menuItem_34.setText("HDF&5 Library Version");
		h5GUIs.add(menuItem_34);
		menuItem_34.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hdfLibraryVersionInfo(HDF5_VERSION);
			}
		});
		
		MenuItem menuItem_35 = new MenuItem(menu_7, SWT.NONE);
		menuItem_35.setText("&Java Version");
		menuItem_35.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MessageBox versionInfo = new MessageBox(mainWindow, SWT.ICON_INFORMATION | SWT.OK);
				versionInfo.setText(mainWindow.getText());
				versionInfo.setMessage(JAVA_VER_INFO);
				// Add custom HDF Icon
				versionInfo.open();
			}
		});
		
		new MenuItem(menu_7, SWT.SEPARATOR);
		
		MenuItem menuItem_37 = new MenuItem(menu_7, SWT.NONE);
		menuItem_37.setText("Supported Fi&le Formats");
		menuItem_37.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Enumeration<?> formatKeys = FileFormat.getFileFormatKeys();
				
				String formats = "\nSupported File Formats: \n";
				while (formatKeys.hasMoreElements()) {
					formats += "    " + formatKeys.nextElement() + "\n";
				}
				formats += "\n";
				
				MessageBox message = new MessageBox(mainWindow, SWT.ICON_INFORMATION | SWT.OK);
				message.setText(mainWindow.getText());
				message.setMessage(formats);
				message.open();
			}
		});
		
		new MenuItem(menu_7, SWT.SEPARATOR);
		
		MenuItem menuItem_39 = new MenuItem(menu_7, SWT.NONE);
		menuItem_39.setText("&About...");
		menuItem_39.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MessageBox info = new MessageBox(mainWindow, SWT.ICON_INFORMATION | SWT.OK);
				info.setText(mainWindow.getText());
				info.setMessage(aboutHDFView);
				// Add HDF Icon
				info.open();
			}
		});
		
		log.info("Menubar created");
	}
	
	private void createToolBar() {
		toolbarContainer = new Composite(mainWindow, SWT.NONE);
		toolbarContainer.setFont(Display.getCurrent().getSystemFont());
		toolbarContainer.setLayoutData(BorderLayout.NORTH);
		toolbarContainer.setLayout(new FillLayout(SWT.VERTICAL));
		
		Composite toolbarItemContainer = new Composite(toolbarContainer, SWT.NONE);
		toolbarItemContainer.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		ToolBar toolBar = new ToolBar(toolbarItemContainer, SWT.FLAT | SWT.RIGHT);
		
		ToolItem tltmNewItem = new ToolItem(toolBar, SWT.NONE);
		tltmNewItem.setToolTipText("Open");
		tltmNewItem.setImage(ViewProperties.getFileopenIcon());
		tltmNewItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openLocalFile(null, FileFormat.WRITE);
			}
		});
		
		ToolItem tltmNewItem_1 = new ToolItem(toolBar, SWT.NONE);
		tltmNewItem_1.setImage(ViewProperties.getFilecloseIcon());
		tltmNewItem_1.setToolTipText("Close");
		tltmNewItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				closeFile(treeView.getSelectedFile());
			}
		});
		
		ToolItem toolItem = new ToolItem(toolBar, SWT.SEPARATOR);
		toolItem.setWidth(20);
		
		ToolItem tltmNewItem_2 = new ToolItem(toolBar, SWT.NONE);
		tltmNewItem_2.setImage(ViewProperties.getHelpIcon());
		tltmNewItem_2.setToolTipText("Help");
		tltmNewItem_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String ugPath = ViewProperties.getUsersGuide();
				
				if(ugPath == null || !ugPath.startsWith("http://")) {
					String sep = File.separator;
					File tmpFile = new File(ugPath);
					
					if(!(tmpFile.exists())) {
						ugPath = rootDir + sep + "UsersGuide" + sep + "index.html";
						tmpFile = new File(ugPath);
						
						if(!(tmpFile.exists())) {
							ugPath = HDFVIEW_USERSGUIDE_URL;
						}
						
						ViewProperties.setUsersGuide(ugPath);
					}
				}
				
				try {
					org.eclipse.swt.program.Program.launch(ugPath);
				} catch (Exception ex) {
					MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
					error.setText(mainWindow.getText());
					error.setMessage(ex.getMessage());
					// Add HDF Icon
					error.open();
				}
			}
		});
		
		ToolItem tltmNewItem_3 = new ToolItem(toolBar, SWT.NONE);
		tltmNewItem_3.setImage(ViewProperties.getH4Icon());
		tltmNewItem_3.setToolTipText("HDF4 Library Version");
		tltmNewItem_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hdfLibraryVersionInfo(HDF4_VERSION);
			}
		});
		
		if(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) == null) {
			tltmNewItem_3.setEnabled(false);
		}
		
		ToolItem tltmNewItem_4 = new ToolItem(toolBar, SWT.NONE);
		tltmNewItem_4.setImage(ViewProperties.getH5Icon());
		tltmNewItem_4.setToolTipText("HDF5 Library Version");
		tltmNewItem_4.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hdfLibraryVersionInfo(HDF5_VERSION);
			}
		});
		
		if(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) == null) {
			tltmNewItem_4.setEnabled(false);
		}
		
		log.info("Toolbar created");
	}
	
	private void createUrlToolbar() {
		Composite url_toolbar = new Composite(toolbarContainer, SWT.NONE);
		url_toolbar.setLayout(new BorderLayout(0, 0));
		
		url_bar = new Combo(url_toolbar, SWT.NONE);
		url_bar.setLayoutData(BorderLayout.CENTER);
		url_bar.setItems(ViewProperties.getMRF().toArray(new String[0]));
		url_bar.setText("/root/workspace/hdf-java/build/test/uitest/hdf5_test.h5");
		url_bar.setVisibleItemCount(ViewProperties.MAX_RECENT_FILES);
		url_bar.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.CR) {
					String filename = url_bar.getText();
					if (filename == null || filename.length() < 1 || filename.equals(currentFile))
						return;
					
					if(!(filename.startsWith("http://") || filename.startsWith("ftp://"))) {
						File tmpFile = new File(filename);
						if(!tmpFile.exists())
							return;
						
						if(tmpFile.isDirectory()) {
							currentDir = filename;
							openLocalFile(null, FileFormat.WRITE);
						}
					} else {
						openRemoteFile(filename);
					}
				}
			}
		});
		url_bar.deselectAll();
		
		Button btnRecentFiles = new Button(url_toolbar, SWT.NONE);
		btnRecentFiles.setText("Recent Files");
		btnRecentFiles.setToolTipText("List of recent files");
		btnRecentFiles.setLayoutData(BorderLayout.WEST);
		btnRecentFiles.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				url_bar.setListVisible(true);
			}
		});
		
		Button btnClearText = new Button(url_toolbar, SWT.NONE);
		btnClearText.setToolTipText("Clear current selection");
		btnClearText.setLayoutData(BorderLayout.EAST);
		btnClearText.setText("Clear Text");
		btnClearText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				url_bar.clearSelection();
			}
		});
		
		log.info("URL Toolbar created");
	}
	
	private void createContentArea() {
		Composite content = new Composite(mainWindow, SWT.NONE);
		content.setLayoutData(BorderLayout.CENTER);
		content.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		SashForm sashForm = new SashForm(content, SWT.VERTICAL);
		sashForm.setSashWidth(10);
		
		Composite data = new Composite(sashForm, SWT.NONE);
		data.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		SashForm sashForm_1 = new SashForm(data, SWT.NONE);
		sashForm_1.setSashWidth(10);
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(sashForm_1, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		
		Tree tree = new Tree(scrolledComposite, SWT.NONE);
		tree.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		
		TreeItem trtmNewTreeitem = new TreeItem(tree, SWT.NONE);
		trtmNewTreeitem.setImage(ViewProperties.getH5Icon());
		trtmNewTreeitem.setText("hdf5_test.h5");
		
		TreeItem trtmNotes = new TreeItem(trtmNewTreeitem, SWT.NONE);
		trtmNotes.setImage(ViewProperties.getTextIcon());
		trtmNotes.setText("Notes");
		
		TreeItem trtmNewTreeitem_1 = new TreeItem(trtmNewTreeitem, SWT.NONE);
		trtmNewTreeitem_1.setImage(ViewProperties.getFoldercloseIcon());
		trtmNewTreeitem_1.setText("arrays");
		
		TreeItem trtmdIntArray = new TreeItem(trtmNewTreeitem_1, SWT.NONE);
		trtmdIntArray.setImage(ViewProperties.getDatasetIcon());
		trtmdIntArray.setText("2D int array");
		trtmNewTreeitem_1.setExpanded(true);
		
		TreeItem trtmNewTreeitem_2 = new TreeItem(trtmNewTreeitem, SWT.NONE);
		trtmNewTreeitem_2.setImage(ViewProperties.getFoldercloseIcon());
		trtmNewTreeitem_2.setText("datatypes");
		
		TreeItem trtmNewTreeitem_4 = new TreeItem(trtmNewTreeitem_2, SWT.NONE);
		trtmNewTreeitem_4.setImage(ViewProperties.getDatatypeIcon());
		trtmNewTreeitem_4.setText("H5T_NATIVE_INT");
		trtmNewTreeitem_2.setExpanded(true);
		
		TreeItem trtmNewTreeitem_3 = new TreeItem(trtmNewTreeitem, SWT.NONE);
		trtmNewTreeitem_3.setImage(ViewProperties.getFoldercloseIcon());
		trtmNewTreeitem_3.setText("images");
		
		TreeItem trtmdThg = new TreeItem(trtmNewTreeitem_3, SWT.NONE);
		trtmdThg.setImage(ViewProperties.getImageIconA());
		trtmdThg.setText("3D THG");
		trtmNewTreeitem_3.setExpanded(true);
		trtmNewTreeitem.setExpanded(true);
		scrolledComposite.setContent(tree);
		scrolledComposite.setMinSize(tree.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		contentArea = new Composite(sashForm_1, SWT.BORDER);
		contentArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		sashForm_1.setWeights(new int[] {1, 1});
		
		Composite status = new Composite(sashForm, SWT.NONE);
		status.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		CTabFolder tabFolder = new CTabFolder(status, SWT.BORDER | SWT.FLAT);
		tabFolder.setTabPosition(SWT.BOTTOM);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		CTabItem tbtmLogInfo = new CTabItem(tabFolder, SWT.NONE);
		tbtmLogInfo.setText("Log Info");
		
		statusArea = new Text(tabFolder, SWT.V_SCROLL | SWT.MULTI);
		statusArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
		statusArea.setEditable(false);
		message = new StringBuffer();
		metadata = new StringBuffer();
		showStatus("HDFView root - " + rootDir);
		showStatus("User property file - " + ViewProperties.getPropertyFile());
		
		tbtmLogInfo.setControl(statusArea);
		
		CTabItem tbtmNewItem = new CTabItem(tabFolder, SWT.NONE);
		tbtmNewItem.setText("Metadata");
		
		attributeArea = new Text(tabFolder, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		attributeArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
		attributeArea.setText("/ (928)\n    Group size = 4\n    Number of attributes = 1\n        test = this is a test");
		attributeArea.setEditable(false);
		tbtmNewItem.setControl(attributeArea);
		sashForm.setWeights(new int[] {4, 1});
		
		// Set Log Info to show first in status area
		tabFolder.setSelection(0);
	}
	
	private void createStatusArea() {
		
	}
	
	private void hdfLibraryVersionInfo(final String version) {
		final Shell dialog = new Shell(mainWindow, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
		dialog.setText("HDFView");
		dialog.setSize(mainWindow.getSize().x / 3, mainWindow.getSize().y / 3);
		dialog.setLayout(new BorderLayout(0, 0));
		// Add HDF Icon
		
		Composite canvasComposite = new Composite(dialog, SWT.NONE);
		canvasComposite.setLayoutData(BorderLayout.NORTH);
		canvasComposite.setLayout(new FillLayout());
		
		Composite buttonComposite = new Composite(dialog, SWT.NONE);
		buttonComposite.setLayoutData(BorderLayout.SOUTH);
		RowLayout buttonLayout = new RowLayout();
		buttonLayout.center = true;
		buttonLayout.justify = true;
		buttonLayout.type = SWT.HORIZONTAL;
		buttonComposite.setLayout(buttonLayout);
		
		Canvas canvas = new Canvas(canvasComposite, SWT.NO_REDRAW_RESIZE);
		final Image hdfLarge = new Image(display, HDFView.class.getResourceAsStream("/icons/hdf_large.gif"));
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.drawImage(hdfLarge, 10, 20);
				e.gc.drawText(version, 100, 40);
			}
		});
		
		Button okButton = new Button(buttonComposite, SWT.PUSH);
		okButton.setText("OK");
		okButton.addSelectionListener(new SelectionAdapter() {
			
			public void widgetSelected(SelectionEvent e) {
				dialog.dispose();
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		
		});
		dialog.setDefaultButton(okButton);
		
		// Center the window relative to the main window
		Point winCenter = new Point(mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
				mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));
		
		dialog.setBounds(winCenter.x - (dialog.getSize().x / 2), 
				winCenter.y - (dialog.getSize().y / 2), 
				dialog.getSize().x, dialog.getSize().y);
		
		dialog.open();
	}
	
	private void registerFileFormat() {
		
	}
	
	
	private void unregisterFileFormat() {
		/*
		Enumeration<Object> keys = FileFormat.getFileFormatKeys();
		ArrayList<Object> keyList = new ArrayList<Object>();
		
		while (keys.hasMoreElements())
			keyList.add((Object) keys.nextElement());
		
		// Subclass SWT Dialog and create new Dialog for file format input
		String theKey = (String) JOptionPane.showInputDialog(this, "Unregister a file format",
                "Unregister a file format", JOptionPane.WARNING_MESSAGE, ViewProperties.getLargeHdfIcon(),
                keyList.toArray(), null);
		
		if (theKey == null)
			return;
		
		FileFormat.removeFileFormat(theKey);
		*/
	}
	
	/**
     * @return a list of treeview implementations.
     */
    public static final List<String> getListOfTreeView() {
        return treeViews;
    }

    /**
     * @return a list of imageview implementations.
     */
    public static final List<String> getListOfImageView() {
        return imageViews;
    }

    /**
     * @return a list of tableview implementations.
     */
    public static final List<?> getListOfTableView() {
        return tableViews;
    }

    /**
     * @return a list of textview implementations.
     */
    public static final List<?> getListOfTextView() {
        return textViews;
    }

    /**
     * @return a list of metaDataview implementations.
     */
    public static final List<?> getListOfMetaDataView() {
        return metaDataViews;
    }

    /**
     * @return a list of paletteview implementations.
     */
    public static final List<?> getListOfPaletteView() {
        return paletteViews;
    }
    
	public TreeView getTreeView() {
		return treeView;
	}
    
    /**
     * Display feedback message.
     * 
     * @param msg
     *            the message to display.
     */
    public void showStatus(String msg) {
        message.append(msg);
        message.append("\n");
        statusArea.setText(message.toString());
    }
    
    public void reloadFile() {
    	int temp_index_type = 0;
    	int temp_index_order = 0;
    	
    	FileFormat theFile = treeView.getSelectedFile();
    	if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
    		temp_index_type = theFile.getIndexType(null);
    		temp_index_order = theFile.getIndexOrder(null);
    	}
    	closeFile(theFile);
    	
    	if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
    		theFile.setIndexType(temp_index_type);
    		theFile.setIndexOrder(temp_index_order);
    	}
    	
    	try {
    		treeView.reopenFile(theFile);
    	} catch (Exception ex) {
    		
    	}
    }
    
    public void addDataView(DataView dataView) {
    	
    }
    
    public void removeDataView(DataView dataView) {
    	
    }
    
    public DataView getDataView(HObject dataObject) {
    	
    	return null;
    }
    
    // Switch to SWT when possible (only here for compile reasons)
    public void mouseEventFired(java.awt.event.MouseEvent e) {
    	
    }
    
    public void dragEnter(DropTargetEvent evt) {
    }
    
    public void dragLeave(DropTargetEvent evt) {
    }
    
    public void dragOperationChanged(DropTargetEvent evt) {
    }
    
    public void dragOver(DropTargetEvent evt) {
    }
    
    public void drop(DropTargetEvent evt) {
    	
    }
    
    public void dropAccept(DropTargetEvent evt) {
    }

    /**
     * Set default UI fonts.
     */
    private void updateFontSize(Font font) {
    	if (font == null)
    		return;
    	
    	/*
    	UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        for (Iterator<?> i = defaults.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            if (defaults.getFont(key) != null) {
                UIManager.put(key, new javax.swing.plaf.FontUIResource(font));
            }
        }
        
        SwingUtilities.updateComponentTreeUI(this);
        */
    }
    
    /**
     * Bring window to the front.
     * <p>
     * 
     * @param name
     * 			  the name of the window to show.
     */
    private void showWindow(final Shell shell) {
    	// Return if main window (shell) is the only open shell
    	if (display.getShells().length <= 1)
    		return;
    	
    	shell.getDisplay().asyncExec(new Runnable() {
    		public void run() {
    			shell.setActive();
    		}
    	});
    }
    
    /**
     * Cascade all windows.
     */
    private void cascadeWindows() {
    	int x = 2, y = 2;
    	Shell shell = null;
    	Shell[] sList = display.getShells();
    	
    	// Return if main window (shell) is the only open shell
    	// or if no shells are open
    	if ((sList == null) || (sList.length <= 1))
    		return;
    	
    	Point p = contentArea.getSize();
    	int w = Math.max(50, p.x - 100);
    	int h = Math.max(50, p.y - 100);
    	
    	for (int i = 0; i < sList.length; i++) {
    		shell = sList[i];
    		shell.setBounds(x, y, w, h);
    		shell.setActive();
    		x += 20;
    		y += 20;
    	}
    }
    
    /**
     * Tile all windows.
     */
    private void tileWindows() {
    	int x = 0, y = 0, idx = 0;
    	Shell shell = null;
    	Shell[] sList = display.getShells();
    	
    	// Return if main window (shell) is the only open shell
    	// or if no shells are open
    	if ((sList == null) || (sList.length <= 1))
    		return;
    	
    	int n = sList.length;
    	int cols = (int) Math.sqrt(n);
    	int rows = (int) Math.ceil((double) n / (double) cols);
    	
    	Point p = contentArea.getSize();
    	int w = p.x / cols;
    	int h = p.y / rows;
    	
    	for (int i = 0; i < rows; i++) {
    		x = 0;
    		
    		for (int j = 0; j < cols; j++) {
    			idx = i * cols + j;
    			if (idx >= n)
    				return;
    			
    			shell = sList[idx];
    			shell.setBounds(x, y, w, h);
    			x += w;
    		}
    		
    		y += h;
    	}
    }
    
    /**
     * Closes all windows.
     */
    private void closeAllWindows() {
    	Shell shell = null;
    	Shell[] sList = display.getShells();
    	
    	// Return if main window (shell) is the only open shell
    	// or if no shells are open
    	if ((sList == null) | (sList.length <= 1))
    		return;
    	
    	for (int i = 0; i < sList.length; i++) {
    		shell = sList[i];
    		shell.dispose();
    	}
    	
    	shell = null;
    }
    
    /* Enable and disable GUI components */
    private static void setEnabled(List<MenuItem> list, boolean b) {
    	Iterator<MenuItem> it = list.iterator();
    	
    	while (it.hasNext())
    		it.next().setEnabled(b);
    }
    
    /** Open local file */
    private void openLocalFile(String filename, int fileAccessID) {
    	int accessMode = fileAccessID;
    	if (ViewProperties.isReadOnly()) accessMode = FileFormat.READ;
    	
    	if (filename != null) {
    		File file = new File(filename);
    		if (file == null)
    			return;
    		
    		currentFile = filename;
    	} else {
    		FileDialog fChooser = new FileDialog(mainWindow, SWT.OPEN);
    		//fChooser.setFilterExtensions();
    	
    	
    		File chosenFile = new File(fChooser.open());
    		if (chosenFile == null)
    			return;
    	
    		if (chosenFile.isDirectory()) {
    			currentDir = chosenFile.getPath();
    		} else {
    			currentDir = chosenFile.getParent();
    		}
    		
    		currentFile = chosenFile.getAbsolutePath();
    	}
    		
    	try {
    		url_bar.remove(currentFile);
    		url_bar.add(currentFile, 0);
    		url_bar.select(0);
    	}
    	catch (Exception ex) {
    	}
    	
    	try {
    		treeView.openFile(currentFile, accessMode + FileFormat.OPEN_NEW);
    	}
    	catch (Throwable ex) {
    		try {
    			treeView.openFile(currentFile, FileFormat.READ);
    		}
    		catch (Throwable ex2) {
    			String msg = "Failed to open file " + currentFile + "\n" + ex2;
    			display.beep();
    			currentFile = null;
    			url_bar.deselectAll();
    			MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
    			error.setText(mainWindow.getText());
    			error.setMessage(msg);
    			error.open();
    		}
    	}
    }
    
    /** Load remote file and save it to local temporary directory */
    private String openRemoteFile(String urlStr) {
    	if (urlStr == null)
    		return null;
    	
    	String localFile = null;
    	
    	if(urlStr.startsWith("http://")) {
    		localFile = urlStr.substring(7);
    	}
    	else if (urlStr.startsWith("ftp://")) {
    		localFile = urlStr.substring(6);
    	}
    	else {
    		return null;
    	}
    	
    	localFile = localFile.replace('/', '@');
    	localFile = localFile.replace('\\', '@');
    	
    	// Search the local file cache
    	String tmpDir = System.getProperty("java.io.tmpdir");
    	
    	File tmpFile = new File(tmpDir);
    	if (!tmpFile.canWrite()) tmpDir = System.getProperty("user.home");
    	
    	localFile = tmpDir + localFile;
    	
    	tmpFile = new File(localFile);
    	if (tmpFile.exists())
    		return localFile;
    	
    	URL url = null;
    	
    	try {
    		url = new URL(urlStr);
    	} catch (Exception ex) {
    		url = null;
    		display.beep();
    		MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
    		error.setText(mainWindow.getText());
    		error.setMessage(ex.getMessage());
    		error.open();
    		return null;
    	}
    	
    	BufferedInputStream in = null;
    	BufferedOutputStream out = null;
    	
    	try {
    		in = new BufferedInputStream(url.openStream());
    		out = new BufferedOutputStream(new FileOutputStream(tmpFile));
    	} catch (Exception ex) {
    		in = null;
    		display.beep();
    		MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
    		error.setText(mainWindow.getText());
    		error.setMessage(ex.getMessage());
    		error.open();
    		
    		try {
    			out.close();
    		} catch (Exception ex2) {
    			log.debug("Remote file: ", ex2);
    		}
    		
    		return null;
    	}
    	
    	//setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
    	byte[] buff = new byte[512]; // set default buffer size to 512
    	try {
    		int n = 0;
    		while ((n = in.read(buff)) > 0) {
    			out.write(buff, 0, n);
    		}
    	}
    	catch (Exception ex) {
    		log.debug("Remote file: ", ex);
    	}
    	
    	try {
    		in.close();
    	} catch (Exception ex) {
    		log.debug("Remote file: ", ex);
    	}
    	
    	try {
    		out.close();
    	} catch (Exception ex) {
    		log.debug("Remote file: ", ex);
    	}
    	
    	//setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
    	
    	return localFile;
    }
    
    private void closeFile(FileFormat theFile) {
    	if (theFile == null) {
    		display.beep();
    		MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
    		error.setText(mainWindow.getText());
    		error.setMessage("Select a file to close");
    		error.open();
    		return;
    	}
    	
    	// Close all the data windows of this file
    	/*JInternalFrame[] frames = contentPane.getAllFrames();
        if (frames != null) {
            for (int i = 0; i < frames.length; i++) {
                HObject obj = (HObject) (((DataView) frames[i]).getDataObject());
                if (obj == null) {
                    continue;
                }

                if (obj.getFileFormat().equals(theFile)) {
                    frames[i].dispose();
                    frames[i] = null;
                }
            }
        }
        */
    	
    	String fName = (String) url_bar.getItem(url_bar.getSelectionIndex());
    	if (theFile.getFilePath().equals(fName)) {
    		currentFile = null;
    		url_bar.clearSelection();
    	}
    	
    	try {
    		treeView.closeFile(theFile);
    	} catch (Exception ex) {
    		
    	}
    	
    	theFile = null;
    	attributeArea.setText("");
    	System.gc();
    }
    
    private void convertFile(String typeFrom, String typeTo) {
    	FileConversionDialog dialog = new FileConversionDialog(null, typeFrom, typeTo, currentDir,
    			treeView.getCurrentFiles()); // null should be changed to something useful
    	dialog.setVisible(true);
    	
    	if (dialog.isFileConverted()) {
    		String filename = dialog.getConvertedFile();
    		File theFile = new File(filename);
    		
    		if (!theFile.exists())
    			return;
    			
    		currentDir = theFile.getParentFile().getAbsolutePath();
    		currentFile = theFile.getAbsolutePath();
    		
    		try {
    			treeView.openFile(filename, FileFormat.WRITE);
    			
    			try {
    				url_bar.remove(filename);
    				url_bar.add(filename, 0);
    				url_bar.select(0);
    			} catch (Exception ex) {
    				
    			}
    		} catch (Exception ex) {
    			showStatus(ex.toString());
    		}
    	}
    }
    
    /**
     * The starting point of this application.
     * 
     * <pre>
     * Usage: java(w)
     *        -Dhdf.hdf5lib.H5.hdf5lib="your HDF5 library path"
     *        -Dhdf.hdflib.HDFLibrary.hdflib="your HDF4 library path"
     *        -root "the directory where the HDFView is installed"
     *        [filename] "the file to open"
     * </pre>
     */
	public static void main(String[] args) {
		String rootDir = System.getProperty("hdfview.root");
		if(rootDir == null)
			rootDir = System.getProperty("user.dir");
		
		File tmpFile = null;
		int j = args.length;
		int W = 0, H = 0, X = 0, Y = 0;
		
		for(int i = 0; i < args.length; i++) {
			if ("-root".equalsIgnoreCase(args[i])) {
				j--;
				try {
					j--;
					tmpFile = new File(args[++i]);
					
					if(tmpFile.isDirectory()) {
						rootDir = tmpFile.getPath();
					}
					else if(tmpFile.isFile()) {
						rootDir = tmpFile.getParent();
					}
				} catch (Exception ex) {
					
				}
			}
			else if("-g".equalsIgnoreCase(args[i]) || "-geometry".equalsIgnoreCase(args[i])) {
				j--;
				// -geometry WIDTHxHEIGHT+XOFF+YOFF
				try {
					String geom = args[++i];
					j--;
					
					int idx = 0;
					int idx2 = geom.lastIndexOf('-');
					int idx3 = geom.lastIndexOf('+');
					
					idx = Math.max(idx2, idx3);
					if(idx > 0) {
						Y = Integer.parseInt(geom.substring(idx + 1));
						
						if(idx == idx2)
							Y = -Y;
						
						geom = geom.substring(0, idx);
						idx2 = geom.lastIndexOf('-');
						idx3 = geom.lastIndexOf('+');
						idx = Math.max(idx2, idx3);
						
						if(idx > 0) {
							X = Integer.parseInt(geom.substring(idx + 1));
							
							if(idx == idx2)
								X = -X;
							
							geom = geom.substring(0, idx);
						}
					}
					
					idx = geom.indexOf('x');
					
					if(idx > 0) {
						W = Integer.parseInt(geom.substring(0, idx));
						H = Integer.parseInt(geom.substring(idx + 1));
					}
					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			else if("-java.version".equalsIgnoreCase(args[i])) {
				/* MessageDialog ; */
				
				System.exit(0);
			}
		}
		
		Vector<File> fList = new Vector<File>();
		tmpFile = null;
		
		if(j >= 0) {
			for(int i = args.length - j; i < args.length; i++) {
				tmpFile = new File(args[i]);
				if(tmpFile.exists() && (tmpFile.isFile() || tmpFile.isDirectory()))
					fList.add(new File(tmpFile.getAbsolutePath()));
			}
		}
		
		final Vector<File> the_fList = fList;
		final String the_rootDir = rootDir;
		final int the_X = X, the_Y = Y, the_W = W, the_H = H;
		
		display.syncExec(new Runnable() {
			public void run() {
				/*
				HDFView frame = new HDFView(the_rootDir, the_fList, the_W, the_H, the_X, the_Y);
				*/
				new HDFView(display, null, null, 600, 600, 0, 0);
			}
		});
	}
}
