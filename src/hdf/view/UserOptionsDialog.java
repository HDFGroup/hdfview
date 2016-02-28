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

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 * UserOptionsDialog displays components for choosing user options.
 * 
 * @author Jordan T. Henderson
 * @version 2.4 2/13/2016
 */
public class UserOptionsDialog extends Dialog {
	private Shell shell;

	/**
	 * The main HDFView.
	 */
	//private final JFrame          viewer;

	private String                H4toH5Path;
	private Text                  H4toH5Field, UGField, workField, fileExtField, maxMemberField, startMemberField;
	private Combo                 fontSizeChoice, fontTypeChoice, delimiterChoice, imageOriginChoice, indexBaseChoice;
	private Combo                 choiceTreeView, choiceMetaDataView, choiceTextView, choiceTableView, choiceImageView, choicePaletteView;
	private String                rootDir, workDir;
	private Button                checkCurrentUserDir, checkAutoContrast, checkConvertEnum, checkShowValues, checkShowRegRefValues;
	private Button                currentDirButton;
	private Button                checkReadOnly, checkIndexType, checkIndexOrder, checkIndexNative, checkLibVersion, checkReadAll;

	private int                   fontSize;

	private boolean               isFontChanged;

	private boolean               isUserGuideChanged;

	private boolean               isWorkDirChanged;

	/** Default index type for files */
	private static String         indexType;

	/** Default index ordering for files */
	private static String         indexOrder;

	/** A list of Tree view implementations. */
	private static Vector<String> treeViews;

	/** A list of Image view implementations. */
	private static Vector<String> imageViews;

	/** A list of Table view implementations. */
	private static Vector<String> tableViews;

	/** A list of Text view implementations. */
	private static Vector<String> textViews;

	/** A list of metadata view implementations. */
	private static Vector<String> metaDataViews;

	/** A list of palette view implementations. */
	private static Vector<String> paletteViews;

	// private JList srbJList;
	// private JTextField srbFields[];
	// private Vector srbVector;

	public UserOptionsDialog(Shell parent, String viewRoot) {
		super(parent, SWT.APPLICATION_MODAL);

		rootDir = viewRoot;
		isFontChanged = false;
		isUserGuideChanged = false;
		isWorkDirChanged = false;
		// srbJList = null;
		fontSize = ViewProperties.getFontSize();
		workDir = ViewProperties.getWorkDir();
		if (workDir == null) {
			workDir = rootDir;
		}
		treeViews = ViewProperties.getTreeViewList();
		metaDataViews = ViewProperties.getMetaDataViewList();
		textViews = ViewProperties.getTextViewList();
		tableViews = ViewProperties.getTableViewList();
		imageViews = ViewProperties.getImageViewList();
		paletteViews = ViewProperties.getPaletteViewList();
		// srbVector = ViewProperties.getSrbAccount();
		indexType = ViewProperties.getIndexType();
		indexOrder = ViewProperties.getIndexOrder();
	}

	public void open() {
		Shell parent = getParent();
		shell = new Shell(parent, SWT.TITLE | SWT.CLOSE |
				SWT.BORDER | SWT.APPLICATION_MODAL);
		shell.setText("User Options");
		shell.setImage(ViewProperties.getHdfIcon());
		shell.setLayout(new GridLayout(1, false));


		// Create tabbed region
		TabFolder folder = new TabFolder(shell, SWT.TOP);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.setSelection(0);

		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("General Settings");
		item.setControl(createGeneralSettingsRegion(folder));

		item = new TabItem(folder, SWT.NONE);
		item.setText("Default Modules");
		item.setControl(createDefaultModulesRegion(folder));
		
		/*
		try { Class.forName("hdf.srb.SRBFileDialog");
		    item = new TabItem(folder, SWT.NONE);
		    item.setText("SRB Connection");
		    item.setControl(createSrbConnectionPanel());
		} catch (Exception ex) {;}
		*/


		// Create Ok/Cancel button region
		Composite buttonComposite = new Composite(shell, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, true));
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		Button okButton = new Button(buttonComposite, SWT.PUSH);
		okButton.setText("   &Ok   ");
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setUserOptions();
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
				isFontChanged = false;
				shell.dispose();
			}
		});

		gridData = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
		gridData.widthHint = 70;
		cancelButton.setLayoutData(gridData);

		shell.pack();

		int w = 700 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 550 + (ViewProperties.getFontSize() - 12) * 16;
		shell.setSize(w, h);

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

	public boolean isFontChanged() {
		return isFontChanged;
	}

	public boolean isUserGuideChanged() {
		return isUserGuideChanged;
	}

	public boolean isWorkDirChanged() {
		return isWorkDirChanged;
	}
	
	private void setUserOptions() {
        String UGPath = UGField.getText();
        if ((UGPath != null) && (UGPath.length() > 0)) {
            UGPath = UGPath.trim();
            isUserGuideChanged = !UGPath.equals(ViewProperties.getUsersGuide());
            ViewProperties.setUsersGuide(UGPath);
        }

        String workPath = workField.getText();
        if (checkCurrentUserDir.getSelection()) {
            workPath = "user.home";
        }

        if ((workPath != null) && (workPath.length() > 0)) {
            workPath = workPath.trim();
            isWorkDirChanged = !workPath.equals(ViewProperties.getWorkDir());
            ViewProperties.setWorkDir(workPath);
        }

        String ext = fileExtField.getText();
        if ((ext != null) && (ext.length() > 0)) {
            ext = ext.trim();
            ViewProperties.setFileExtension(ext);
        }

        if (checkReadOnly.getSelection())
            ViewProperties.setReadOnly(true);
        else
            ViewProperties.setReadOnly(false);
        
        if (checkLibVersion.getSelection())
            ViewProperties.setEarlyLib(true);
        else
            ViewProperties.setEarlyLib(false);        

        // set font size
        int fsize = 12;
        try {
            fsize = Integer.parseInt((String) fontSizeChoice.getItem(fontSizeChoice.getSelectionIndex()));
            ViewProperties.setFontSize(fsize);

            if ((fontSize != ViewProperties.getFontSize())) {
                isFontChanged = true;
            }
        }
        catch (Exception ex) {
        }

        // set font type
        String ftype = (String) fontTypeChoice.getItem(fontTypeChoice.getSelectionIndex());
        if (!ftype.equalsIgnoreCase(ViewProperties.getFontType())) {
            isFontChanged = true;
            ViewProperties.setFontType(ftype);
        }

        // set data delimiter
        ViewProperties.setDataDelimiter((String) delimiterChoice.getItem(delimiterChoice.getSelectionIndex()));
        ViewProperties.setImageOrigin((String) imageOriginChoice.getItem(imageOriginChoice.getSelectionIndex()));
        
        // set index type
        if (checkIndexType.getSelection())
            ViewProperties.setIndexType("H5_INDEX_NAME");
        else
            ViewProperties.setIndexType("H5_INDEX_CRT_ORDER");

        // set index order
        if (checkIndexOrder.getSelection())
            ViewProperties.setIndexOrder("H5_ITER_INC");
        else if (checkIndexNative.getSelection())
            ViewProperties.setIndexOrder("H5_ITER_NATIVE");
        else
            ViewProperties.setIndexOrder("H5_ITER_DEC");

        if (checkReadAll.getSelection()) {
        	ViewProperties.setStartMembers(0);
        	ViewProperties.setMaxMembers(-1);
        } else {
            try {
                int maxsize = Integer.parseInt(maxMemberField.getText());
                ViewProperties.setMaxMembers(maxsize);
            }
            catch (Exception ex) {
            }

            try {
                int startsize = Integer.parseInt(startMemberField.getText());
                ViewProperties.setStartMembers(startsize);
            }
            catch (Exception ex) {
            }
        }

        @SuppressWarnings("rawtypes")
        Vector[] moduleList = { treeViews, metaDataViews, textViews, tableViews, imageViews, paletteViews };
        Combo[] choiceList = { choiceTreeView, choiceMetaDataView, choiceTextView, choiceTableView,
                choiceImageView, choicePaletteView };
        for (int i = 0; i < 6; i++) {
            Object theModule = choiceList[i].getItem(choiceList[i].getSelectionIndex());
            moduleList[i].remove(theModule);
            moduleList[i].add(0, theModule);
        }

        ViewProperties.setAutoContrast(checkAutoContrast.getSelection());
        ViewProperties.setShowImageValue(checkShowValues.getSelection());
        ViewProperties.setConvertEnum(checkConvertEnum.getSelection());
        ViewProperties.setShowRegRefValue(checkShowRegRefValues.getSelection());

        if (indexBaseChoice.getSelectionIndex() == 0)
            ViewProperties.setIndexBase1(false);
        else
            ViewProperties.setIndexBase1(true);
    }

	private Composite createGeneralSettingsRegion(TabFolder folder) {
		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		
		org.eclipse.swt.widgets.Group workingDirectoryGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		workingDirectoryGroup.setLayout(new GridLayout(3, false));
		workingDirectoryGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		workingDirectoryGroup.setText("Default Working Directory");
		
		checkCurrentUserDir = new Button(workingDirectoryGroup, SWT.CHECK);
		checkCurrentUserDir.setText("\"Current Working Directory\" or");
		checkCurrentUserDir.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		checkCurrentUserDir.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isCheckCurrentUserDirSelected = checkCurrentUserDir.getSelection();
	            workField.setEnabled(!isCheckCurrentUserDirSelected);
	            currentDirButton.setEnabled(!isCheckCurrentUserDirSelected);
			}
		});
		
		workField = new Text(workingDirectoryGroup, SWT.SINGLE | SWT.BORDER);
		workField.setText(workDir);
		workField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		currentDirButton = new Button(workingDirectoryGroup, SWT.PUSH);
		currentDirButton.setText("Browse...");
		currentDirButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		currentDirButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
				fChooser.setFilterPath(workDir);
				fChooser.setText("Select a Directory");
	            //fchooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				if(fChooser.open() == null) {
					return;
				}
				
				File chosenFile = new File(fChooser.getFilterPath() + File.separator + fChooser.getFileName());
				
				if(!chosenFile.exists()) {
					// Give an error
					return;
				}
				
				workField.setText(chosenFile.getAbsolutePath());
			}
		});
		
		if (workDir.equals(System.getProperty("user.home"))) {
            checkCurrentUserDir.setSelection(true);
            workField.setEnabled(false);
        }
		
		org.eclipse.swt.widgets.Group helpDocumentGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		helpDocumentGroup.setLayout(new GridLayout(3, false));
		helpDocumentGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		helpDocumentGroup.setText("Help Document");
		
		new Label(helpDocumentGroup, SWT.RIGHT).setText("User's Guide:  ");
		
		UGField = new Text(helpDocumentGroup, SWT.SINGLE | SWT.BORDER);
		UGField.setText(ViewProperties.getUsersGuide());
		UGField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button browseButton = new Button(helpDocumentGroup, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
				fChooser.setFilterPath(rootDir);
				
				if(fChooser.open() == null) {
					return;
				}
				
				File chosenFile = new File(fChooser.getFilterPath() + File.separator + fChooser.getFileName());
				
				if(!chosenFile.exists()) {
					// Give an error
					return;
				}
				
	            UGField.setText(chosenFile.getAbsolutePath());
			}
		});
		
		Composite fileOptionComposite = new Composite(composite, SWT.NONE);
		fileOptionComposite.setLayout(new GridLayout(3, true));
		fileOptionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		org.eclipse.swt.widgets.Group fileAccessModeGroup = new org.eclipse.swt.widgets.Group(fileOptionComposite, SWT.NONE);
		fileAccessModeGroup.setLayout(new GridLayout(2, true));
		fileAccessModeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fileAccessModeGroup.setText("Default File Access Mode");
		
		checkReadOnly = new Button(fileAccessModeGroup, SWT.RADIO);
		checkReadOnly.setText("Read Only");
		checkReadOnly.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		checkReadOnly.setSelection(ViewProperties.isReadOnly());
		
		Button rw = new Button(fileAccessModeGroup, SWT.RADIO);
		rw.setText("Read/Write");
		rw.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		rw.setSelection(!ViewProperties.isReadOnly());
		
		
		org.eclipse.swt.widgets.Group fileExtensionGroup = new org.eclipse.swt.widgets.Group(fileOptionComposite, SWT.NONE);
		fileExtensionGroup.setLayout(new GridLayout(2, false));
		fileExtensionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fileExtensionGroup.setText("File Extensions");
		
		new Label(fileExtensionGroup, SWT.RIGHT).setText("Extensions: ");
		
		fileExtField = new Text(fileExtensionGroup, SWT.SINGLE | SWT.BORDER);
		fileExtField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fileExtField.setText(ViewProperties.getFileExtension());
		
		
		org.eclipse.swt.widgets.Group defaultLibVersionGroup = new org.eclipse.swt.widgets.Group(fileOptionComposite, SWT.NONE);
		defaultLibVersionGroup.setLayout(new GridLayout(2, true));
		defaultLibVersionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		defaultLibVersionGroup.setText("Default Lib Version");
		
		checkLibVersion = new Button(defaultLibVersionGroup, SWT.RADIO);
		checkLibVersion.setText("Earliest");
		checkLibVersion.setSelection(ViewProperties.isEarlyLib());
		checkLibVersion.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		
		Button libv = new Button(defaultLibVersionGroup, SWT.RADIO);
		libv.setText("Latest");
		libv.setSelection(!ViewProperties.isEarlyLib());
		libv.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		
		
		org.eclipse.swt.widgets.Group textFontGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		textFontGroup.setLayout(new GridLayout(4, false));
		textFontGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		textFontGroup.setText("Text Font");
		
		new Label(textFontGroup, SWT.RIGHT).setText("Font Size: ");
		
		String[] fontSizeChoices = { "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "48" };
		fontSizeChoice = new Combo(textFontGroup, SWT.SINGLE);
		fontSizeChoice.setItems(fontSizeChoices);
		fontSizeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		//fontSizeChoice.setSelectedItem(String.valueOf(ViewProperties.getFontSize()));
		
		new Label(textFontGroup, SWT.RIGHT).setText("Font Type: ");
		
		String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		String fname = ViewProperties.getFontType();
		
		fontTypeChoice = new Combo(textFontGroup, SWT.SINGLE);
		fontTypeChoice.setItems(fontNames);
		fontTypeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		/*
		boolean isFontValid = false;
        if (fontNames != null) {
            for (int i = 0; i < fontNames.length; i++) {
                if (fontNames[i].equalsIgnoreCase(fname)) {
                    isFontValid = true;
                }
            }
        }
        if (!isFontValid) {
            fname = (viewer).getFont().getFamily();
            ViewProperties.setFontType(fname);
        }
        
        fontTypeChoice.setSelectedItem(fname);
		*/
		
		
		org.eclipse.swt.widgets.Group imageGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		imageGroup.setLayout(new GridLayout(5, false));
		imageGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		imageGroup.setText("Image");
		
		Button helpButton = new Button(imageGroup, SWT.PUSH);
		helpButton.setImage(ViewProperties.getHelpIcon());
		helpButton.setToolTipText("Help on Auto Contrast");
		helpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final String msg = "Auto Contrast does the following to compute a gain/bias \n"
	                    + "that will stretch the pixels in the image to fit the pixel \n"
	                    + "values of the graphics system. For example, it stretches unsigned\n"
	                    + "short data to fit the full range of an unsigned short. Later \n"
	                    + "code simply takes the high order byte and passes it to the graphics\n"
	                    + "system (which expects 0-255). It uses some statistics on the pixels \n"
	                    + "to prevent outliers from throwing off the gain/bias calculations much.\n\n"
	                    + "To compute the gain/bias we... \n"
	                    + "Find the mean and std. deviation of the pixels in the image \n" + "min = mean - 3 * std.dev. \n"
	                    + "max = mean + 3 * std.dev. \n" + "small fudge factor because this tends to overshoot a bit \n"
	                    + "Stretch to 0-USHRT_MAX \n" + "        gain = USHRT_MAX / (max-min) \n"
	                    + "        bias = -min \n" + "\n" + "To apply the gain/bias to a pixel, use the formula \n"
	                    + "data[i] = (data[i] + bias) * gain \n" + "\n"
	                    // +
	                    // "Finally, for auto-ranging the sliders for gain/bias, we do the following \n"
	                    // + "gain_min = 0 \n"
	                    // + "gain_max = gain * 3.0 \n"
	                    // + "bias_min = -fabs(bias) * 3.0 \n"
	                    // + "bias_max = fabs(bias) * 3.0 \n"
	                    + "\n\n";
				
				MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION);
				info.setText(shell.getText());
				info.setMessage(msg);
				info.open();
			}
		});
		
		checkAutoContrast = new Button(imageGroup, SWT.CHECK);
		checkAutoContrast.setText("Autogain Image Contrast");
		checkAutoContrast.setSelection(ViewProperties.isAutoContrast());
		checkAutoContrast.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		checkShowValues = new Button(imageGroup, SWT.CHECK);
		checkShowValues.setText("Show Values");
		checkShowValues.setSelection(ViewProperties.showImageValues());
		checkShowValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		new Label(imageGroup, SWT.RIGHT).setText("Image Origin: ");
		
		String[] imageOriginChoices = { ViewProperties.ORIGIN_UL, ViewProperties.ORIGIN_LL, ViewProperties.ORIGIN_UR,
                ViewProperties.ORIGIN_LR };
		
		imageOriginChoice = new Combo(imageGroup, SWT.SINGLE);
		imageOriginChoice.setItems(imageOriginChoices);
		//imageOriginChoice.setSelectedItem(ViewProperties.getImageOrigin());
		imageOriginChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		org.eclipse.swt.widgets.Group dataGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		dataGroup.setLayout(new GridLayout(4, false));
		dataGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		dataGroup.setText("Data");
		
		helpButton = new Button(dataGroup, SWT.PUSH);
		helpButton.setImage(ViewProperties.getHelpIcon());
		helpButton.setToolTipText("Help on Convert Enum");
		helpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final String msg = "Convert enum data to strings. \n"
	                    + "For example, a dataset of an enum type of (R=0, G=, B=2) \n"
	                    + "has values of (0, 2, 2, 2, 1, 1). With conversion, the data values are \n"
	                    + "shown as (R, B, B, B, G, G).\n\n\n";
				
				MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION);
				info.setText(shell.getText());
				info.setMessage(msg);
				info.open();
			}
		});
		
		checkConvertEnum = new Button(dataGroup, SWT.CHECK);
		checkConvertEnum.setText("Convert Enum");
		checkConvertEnum.setSelection(ViewProperties.isConvertEnum());
		checkConvertEnum.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
		
		checkShowRegRefValues = new Button(dataGroup, SWT.CHECK);
		checkShowRegRefValues.setText("Show RegRef Values");
		checkShowRegRefValues.setSelection(ViewProperties.showRegRefValues());
		checkShowRegRefValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		// Add dummy label
		new Label(dataGroup, SWT.RIGHT).setText("");
		
		new Label(dataGroup, SWT.RIGHT).setText("Index Base: ");
		
		String[] indexBaseChoices = { "0-based", "1-based" };
        indexBaseChoice = new Combo(dataGroup, SWT.SINGLE);
        indexBaseChoice.setItems(indexBaseChoices);
        indexBaseChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        if (ViewProperties.isIndexBase1())
            indexBaseChoice.select(1);
        else
            indexBaseChoice.select(0);
        
        Label delimLabel = new Label(dataGroup, SWT.RIGHT);
        delimLabel.setText("Data Delimiter: ");
        delimLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        
        String[] delimiterChoices = { ViewProperties.DELIMITER_TAB, ViewProperties.DELIMITER_COMMA,
                ViewProperties.DELIMITER_SPACE, ViewProperties.DELIMITER_COLON, ViewProperties.DELIMITER_SEMI_COLON };
        delimiterChoice = new Combo(dataGroup, SWT.SINGLE);
        delimiterChoice.setItems(delimiterChoices);
        //delimiterChoice.setSelectedItem(ViewProperties.getDataDelimiter());
        delimiterChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		org.eclipse.swt.widgets.Group objectsGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		objectsGroup.setLayout(new GridLayout(5, false));
		objectsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		objectsGroup.setText("Objects to Open");
		
		int nMax = ViewProperties.getMaxMembers();
		checkReadAll = new Button(objectsGroup, SWT.CHECK);
		checkReadAll.setText("Open All");
		checkReadAll.setSelection((nMax<=0) || (nMax==Integer.MAX_VALUE));
		checkReadAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		checkReadAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				startMemberField.setEnabled(!checkReadAll.getSelection());
	            maxMemberField.setEnabled(!checkReadAll.getSelection());
			}
		});
		
		new Label(objectsGroup, SWT.RIGHT).setText("Start Member: ");
		
		startMemberField = new Text(objectsGroup, SWT.SINGLE | SWT.BORDER);
		startMemberField.setText(String.valueOf(ViewProperties.getStartMembers()));
		startMemberField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		new Label(objectsGroup, SWT.RIGHT).setText("Member Count: ");
		
		maxMemberField = new Text(objectsGroup, SWT.SINGLE | SWT.BORDER);
		maxMemberField.setText(String.valueOf(ViewProperties.getMaxMembers()));
		maxMemberField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		startMemberField.setEnabled(!checkReadAll.getSelection());
       	maxMemberField.setEnabled(!checkReadAll.getSelection());
		
		
		org.eclipse.swt.widgets.Group displayIndexingGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		displayIndexingGroup.setLayout(new GridLayout(2, true));
		displayIndexingGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		displayIndexingGroup.setText("Display Indexing Options");
		
		org.eclipse.swt.widgets.Group indexingTypeGroup = new org.eclipse.swt.widgets.Group(displayIndexingGroup, SWT.NONE);
		indexingTypeGroup.setLayout(new GridLayout(2, true));
		indexingTypeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		indexingTypeGroup.setText("Indexing Type");
		
		checkIndexType = new Button(indexingTypeGroup, SWT.RADIO);
		checkIndexType.setText("By Name");
		checkIndexType.setSelection(indexType.compareTo("H5_INDEX_NAME") == 0);
		checkIndexType.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		
		Button checkIndexCreateOrder = new Button(indexingTypeGroup, SWT.RADIO);
		checkIndexCreateOrder.setText("By Creation Order");
		checkIndexCreateOrder.setSelection(indexType.compareTo("H5_INDEX_CRT_ORDER") == 0);
		checkIndexCreateOrder.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		
		
		org.eclipse.swt.widgets.Group indexingOrderGroup = new org.eclipse.swt.widgets.Group(displayIndexingGroup, SWT.NONE);
		indexingOrderGroup.setLayout(new GridLayout(3, true));
		indexingOrderGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		indexingOrderGroup.setText("Indexing Order");
		
		checkIndexOrder = new Button(indexingOrderGroup, SWT.RADIO);
		checkIndexOrder.setText("Increments");
		checkIndexOrder.setSelection(indexOrder.compareTo("H5_ITER_INC") == 0);
		checkIndexOrder.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		
		Button decOrder = new Button(indexingOrderGroup, SWT.RADIO);
		decOrder.setText("Decrements");
		decOrder.setSelection(indexOrder.compareTo("H5_ITER_DEC") == 0);
		decOrder.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		
		Button nativeOrder = new Button(indexingOrderGroup, SWT.RADIO);
		nativeOrder.setText("Native");
		nativeOrder.setSelection(indexOrder.compareTo("H5_ITER_NATIVE") == 0);
		nativeOrder.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		
		return composite;
	}

	private Composite createDefaultModulesRegion(TabFolder folder) {
		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		
		org.eclipse.swt.widgets.Group treeViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		treeViewGroup.setLayout(new FillLayout());
		treeViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		treeViewGroup.setText("TreeView");
		
		choiceTreeView = new Combo(treeViewGroup, SWT.SINGLE);
		choiceTreeView.setItems(treeViews.toArray(new String[0]));
		
		org.eclipse.swt.widgets.Group metadataViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		metadataViewGroup.setLayout(new FillLayout());
		metadataViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		metadataViewGroup.setText("MetaDataView");
		
		choiceMetaDataView = new Combo(metadataViewGroup, SWT.SINGLE);
		choiceMetaDataView.setItems(metaDataViews.toArray(new String[0]));
		
		org.eclipse.swt.widgets.Group textViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		textViewGroup.setLayout(new FillLayout());
		textViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		textViewGroup.setText("TextView");
		
		choiceTextView = new Combo(textViewGroup, SWT.SINGLE);
		choiceTextView.setItems(textViews.toArray(new String[0]));
		
		org.eclipse.swt.widgets.Group tableViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		tableViewGroup.setLayout(new FillLayout());
		tableViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		tableViewGroup.setText("TableView");
		
		choiceTableView = new Combo(tableViewGroup, SWT.SINGLE);
		choiceTableView.setItems(tableViews.toArray(new String[0]));
		
		org.eclipse.swt.widgets.Group imageViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		imageViewGroup.setLayout(new FillLayout());
		imageViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		imageViewGroup.setText("ImageView");
		
		choiceImageView = new Combo(imageViewGroup, SWT.SINGLE);
		choiceImageView.setItems(imageViews.toArray(new String[0]));
		
		org.eclipse.swt.widgets.Group paletteViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
		paletteViewGroup.setLayout(new FillLayout());
		paletteViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		paletteViewGroup.setText("PaletteView");
		
		choicePaletteView = new Combo(paletteViewGroup, SWT.SINGLE);
		choicePaletteView.setItems(paletteViews.toArray(new String[0]));

		return composite;
	}
	
	/*
     * private JPanel createSrbConnectionPanel() { JPanel p = new JPanel();
     * p.setLayout(new BorderLayout(5,5)); TitledBorder tborder = new
     * TitledBorder("SRB Connections"); tborder.setTitleColor(Color.darkGray);
     * p.setBorder(tborder);
     * 
     * DefaultListModel listModel = new DefaultListModel(); srbJList = new
     * JList(listModel);
     * srbJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
     * srbJList.addListSelectionListener(this);
     * 
     * srbFields = new JTextField[7];
     * 
     * if (srbVector!= null) { int n=srbVector.size();
     * 
     * String srbaccount[] = null; for (int i=0; i<n; i++) { srbaccount =
     * (String[])srbVector.get(i); if (srbaccount != null) {
     * listModel.addElement(srbaccount[0]); } } }
     * 
     * JPanel cp = new JPanel(); cp.setLayout(new BorderLayout(5,5));
     * 
     * JPanel cpc = new JPanel(); cpc.setLayout(new GridLayout(7,1,5,5));
     * cpc.add(srbFields[0] = new JTextField()); cpc.add(srbFields[1] = new
     * JTextField()); cpc.add(srbFields[2] = new JTextField());
     * cpc.add(srbFields[3] = new JTextField()); cpc.add(srbFields[4] = new
     * JTextField()); cpc.add(srbFields[5] = new JTextField());
     * cpc.add(srbFields[6] = new JTextField()); cp.add(cpc,
     * BorderLayout.CENTER);
     * 
     * JPanel cpl = new JPanel(); cpl.setLayout(new GridLayout(7,1,5,5));
     * cpl.add(new JLabel("Host Machine: ", SwingConstants.RIGHT)); cpl.add(new
     * JLabel("Port Number: ", SwingConstants.RIGHT)); cpl.add(new
     * JLabel("User Name: ", SwingConstants.RIGHT)); cpl.add(new
     * JLabel("Password: ", SwingConstants.RIGHT)); cpl.add(new
     * JLabel("Home Directory: ", SwingConstants.RIGHT)); cpl.add(new
     * JLabel("Domain Name/Zone: ", SwingConstants.RIGHT)); cpl.add(new
     * JLabel(" Default Storage Resource: ", SwingConstants.RIGHT)); cp.add(cpl,
     * BorderLayout.WEST);
     * 
     * JPanel lp = new JPanel(); lp.setLayout(new BorderLayout(5,5)); JPanel lpb
     * = new JPanel(); JButton add = new JButton("Save");
     * add.addActionListener(this); add.setActionCommand("Add srb connsction");
     * lpb.add(add); JButton del = new JButton("Delete");
     * del.addActionListener(this);
     * del.setActionCommand("Delete srb connsction"); lpb.add(del); lp.add(lpb,
     * BorderLayout.SOUTH); JScrollPane listScroller = new
     * JScrollPane(srbJList); int w = 120 +
     * (ViewProperties.getFontSize()-12)*10; int h = 200 +
     * (ViewProperties.getFontSize()-12)*15; listScroller.setPreferredSize(new
     * Dimension(w, h)); lp.add(listScroller, BorderLayout.CENTER);
     * 
     * JPanel sp = new JPanel(); sp.setLayout(new GridLayout(3,1,5,15));
     * sp.add(new JLabel(" "));
     * 
     * p.add(cp, BorderLayout.CENTER); p.add(lp, BorderLayout.WEST); p.add(sp,
     * BorderLayout.SOUTH);
     * 
     * if ((srbVector !=null) && (srbVector.size()>0)) {
     * srbJList.setSelectedIndex(0); }
     * 
     * return p; }
     */
	
	/*
     * else if (cmd.equals("Add srb connection")) { String srbaccount[] =
     * new String[7]; for (int i=0; i<7; i++) { srbaccount[i] =
     * srbFields[i].getText(); if (srbaccount[i] == null) { return; } }
     * DefaultListModel lm = (DefaultListModel)srbJList.getModel();
     * 
     * if (lm.contains(srbaccount[0])) { int n =
     * srbJList.getSelectedIndex(); if ( n<0 ) return; String
     * srbaccountOld[] = (String[])srbVector.get(n); for (int i=0; i<7; i++)
     * srbaccountOld[i] = srbaccount[i]; } else { srbVector.add(srbaccount);
     * lm.addElement(srbaccount[0]);
     * srbJList.setSelectedValue(srbaccount[0], true); } } else if
     * (cmd.equals("Delete srb connsction")) { int n =
     * srbJList.getSelectedIndex(); if (n<0) { return; }
     * 
     * int resp = JOptionPane.showConfirmDialog(this,
     * "Are you sure you want to delete the following SRB connection?\n"+
     * "            \""+srbJList.getSelectedValue()+"\"",
     * "Delete SRB Connection", JOptionPane.YES_NO_OPTION); if (resp ==
     * JOptionPane.NO_OPTION) { return; }
     * 
     * DefaultListModel lm = (DefaultListModel)srbJList.getModel();
     * lm.removeElementAt(n); srbVector.remove(n); for (int i=0; i<7; i++) {
     * srbFields[i].setText(""); } }
     */

/*
 * public void valueChanged(ListSelectionEvent e) { Object src =
 * e.getSource();
 * 
 * if (!src.equals(srbJList)) { return; }
 * 
 * int n = srbJList.getSelectedIndex(); if ( n<0 ) { return; }
 * 
 * String srbaccount[] = (String[])srbVector.get(n); if (srbaccount == null)
 * { return; }
 * 
 * n = Math.min(7, srbaccount.length); for (int i=0; i<n; i++) {
 * srbFields[i].setText(srbaccount[i]); } }
 */
}
