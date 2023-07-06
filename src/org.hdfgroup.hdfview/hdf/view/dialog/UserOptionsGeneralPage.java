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

package hdf.view.dialog;

import java.awt.GraphicsEnvironment;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * UserOptionsGeneralPage.java - Configuration page for general application settings.
 */
public class UserOptionsGeneralPage extends UserOptionsDefaultPage {
    private static final Logger log = LoggerFactory.getLogger(UserOptionsGeneralPage.class);

    private Text UGField, workField, maxMemberField, startMemberField, timerRefreshField;

    private Combo fontSizeChoice, fontTypeChoice, delimiterChoice, imageOriginChoice, indexBaseChoice;

    private Button checkCurrentUserDir, checkUserHomeDir, checkAutoContrast, checkShowValues;
    private Button currentDirButton, userHomeButton, rwButton, helpButton;
    private Button checkReadOnly, checkReadAll;

    private boolean isFontChanged;
    private boolean isUserGuideChanged;
    private boolean isWorkDirChanged;

    private String workDir;

    private static String fontname;

    /**
     * Configuration page for general application settings.
     */
    public UserOptionsGeneralPage() {
        super("General Settings");
        isFontChanged = false;
        isUserGuideChanged = false;
        isWorkDirChanged = false;
    }

    /**
     * Performs special processing when this page's Defaults button has been pressed.
     */
    @Override
    public void performDefaults() {
        super.performDefaults();
    }

    /**
     * Notifies that the OK button of this page's container has been pressed.
     *
     * @return <code>false</code> to abort the container's OK processing and
     * <code>true</code> to allow the OK to happen
     */
    @Override
    public boolean performOk() {
        getPreferenceStore();

        if (UGField != null) {
            String UGPath = UGField.getText();
            if ((UGPath != null) && (UGPath.length() > 0)) {
                UGPath = UGPath.trim();
                isUserGuideChanged = !UGPath.equals(ViewProperties.getUsersGuide());
                ViewProperties.setUsersGuide(UGPath);
            }
        }

        if (workField != null) {
            String workPath = workField.getText();
            if (checkCurrentUserDir.getSelection())
                workPath = System.getProperty("user.dir");
            else if (checkUserHomeDir.getSelection())
                workPath = System.getProperty("user.home");

            if ((workPath != null) && (workPath.length() > 0)) {
                workPath = workPath.trim();
                isWorkDirChanged = !workPath.equals(ViewProperties.getWorkDir());
                ViewProperties.setWorkDir(workPath);
            }
        }

        // set font size and type
        try {
            if (fontTypeChoice != null) {
                String ftype = fontTypeChoice.getItem(fontTypeChoice.getSelectionIndex());
                int fsize = Integer.parseInt(fontSizeChoice.getItem(fontSizeChoice.getSelectionIndex()));
                log.trace("performOk: save font options {} - {}", ftype, fsize);

                if (ViewProperties.getFontSize() != fsize) {
                    ViewProperties.setFontSize(fsize);
                    isFontChanged = true;
                    log.trace("performOk: props font size {}", ViewProperties.getFontSize());
                }

                if (!ftype.equalsIgnoreCase(ViewProperties.getFontType())) {
                    ViewProperties.setFontType(ftype);
                    isFontChanged = true;
                    log.trace("performOk: props font {}", ViewProperties.getFontType());
                }
            }
        }
        catch (Exception ex) {
            isFontChanged = false;
        }

        // set file access
        if (checkReadOnly != null) {
            if (checkReadOnly.getSelection())
                ViewProperties.setReadOnly(true);
            else
                ViewProperties.setReadOnly(false);
        }

        // set timer refresh value (msec)
        try {
            int timermsec = Integer.parseInt(timerRefreshField.getText());
            ViewProperties.setTimerRefresh(timermsec);
        }
        catch (Exception ex) {
        }

        // set data delimiter
        if (delimiterChoice != null)
            ViewProperties.setDataDelimiter(delimiterChoice.getItem(delimiterChoice.getSelectionIndex()));
        if (imageOriginChoice != null)
            ViewProperties.setImageOrigin(imageOriginChoice.getItem(imageOriginChoice.getSelectionIndex()));

        if (checkReadAll != null) {
            if (checkReadAll.getSelection()) {
                ViewProperties.setStartMembers(0);
                ViewProperties.setMaxMembers(-1);
            }
            else {
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
        }

        if (checkAutoContrast != null)
            ViewProperties.setAutoContrast(checkAutoContrast.getSelection());
        if (checkShowValues != null)
            ViewProperties.setShowImageValue(checkShowValues.getSelection());

        if (indexBaseChoice != null) {
            if (indexBaseChoice.getSelectionIndex() == 0)
                ViewProperties.setIndexBase1(false);
            else
                ViewProperties.setIndexBase1(true);
        }

        return true;
    }

    /**
     * Checks if the Font setting changed.
     *
     * @return true if the font changed.
     */
    public boolean isFontChanged() {
        return isFontChanged;
    }

    /**
     * Checks if the location for the UserGuide changed.
     *
     * @return  true if the location of the UserGuide changed.
     */
    public boolean isUserGuideChanged() {
        return isUserGuideChanged;
    }

    /**
     * Checks if the location of the WorkDir changed.
     *
     * @return  true if the working directory changed.
     */
    public boolean isWorkDirChanged() {
        return isWorkDirChanged;
    }

    /**
     * Loads all stored values in the <code>FieldEditor</code>s.
     */
    protected void load() {
        getPreferenceStore();

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

        workDir = ViewProperties.getWorkDir();
        if (workDir == null)
            workDir = rootDir;

        workField.setText(workDir);

        if (workDir.equals(System.getProperty("user.dir"))) {
            checkCurrentUserDir.setSelection(true);
            checkUserHomeDir.setSelection(false);
            workField.setEnabled(false);
        }
        else if (workDir.equals(System.getProperty("user.home"))) {
            checkCurrentUserDir.setSelection(false);
            checkUserHomeDir.setSelection(true);
            workField.setEnabled(false);
        }
        else {
            checkCurrentUserDir.setSelection(false);
            checkUserHomeDir.setSelection(false);
            workField.setEnabled(true);
        }

        log.trace("UserOptionsGeneralPage: workDir={}", workDir);

        UGField.setText(ViewProperties.getUsersGuide());

        checkReadOnly.setSelection(ViewProperties.isReadOnly());

        rwButton.setSelection(!ViewProperties.isReadOnly());

        String fontsize = String.valueOf(ViewProperties.getFontSize());
        log.trace("performOk: load General options fontsize={}", fontsize);
        try {
            int selectionIndex = fontSizeChoice.indexOf(fontsize);
            fontSizeChoice.select(selectionIndex);
        }
        catch (Exception ex) {
            fontSizeChoice.select(0);
        }

        fontname = ViewProperties.getFontType();
        log.trace("performOk: load General options fontname={}", fontname);
        try {
            int selectionIndex = fontTypeChoice.indexOf(fontname);
            fontTypeChoice.select(selectionIndex);
        }
        catch (Exception ex) {
            String sysFontName = Display.getDefault().getSystemFont().getFontData()[0].getName();

            try {
                int selectionIndex = fontTypeChoice.indexOf(sysFontName);
                fontTypeChoice.select(selectionIndex);
            }
            catch (Exception ex2) {
                fontTypeChoice.select(0);
            }
        }

        checkAutoContrast.setSelection(ViewProperties.isAutoContrast());

        checkShowValues.setSelection(ViewProperties.showImageValues());

        String[] imageOriginChoices = { ViewProperties.ORIGIN_UL, ViewProperties.ORIGIN_LL, ViewProperties.ORIGIN_UR,
                ViewProperties.ORIGIN_LR };
        imageOriginChoice.setItems(imageOriginChoices);

        try {
            int selectionIndex = imageOriginChoice.indexOf(ViewProperties.getImageOrigin());
            imageOriginChoice.select(selectionIndex);
        }
        catch (Exception ex) {
            imageOriginChoice.select(0);
        }

        //        helpButton.setImage(ViewProperties.getHelpIcon());

        if (ViewProperties.isIndexBase1())
            indexBaseChoice.select(1);
        else
            indexBaseChoice.select(0);

        String[] delimiterChoices = { ViewProperties.DELIMITER_TAB, ViewProperties.DELIMITER_COMMA,
                ViewProperties.DELIMITER_SPACE, ViewProperties.DELIMITER_COLON, ViewProperties.DELIMITER_SEMI_COLON };
        delimiterChoice.setItems(delimiterChoices);

        try {
            int selectionIndex = delimiterChoice.indexOf(ViewProperties.getDataDelimiter());
            delimiterChoice.select(selectionIndex);
        }
        catch (Exception ex) {
            delimiterChoice.select(0);
        }

        timerRefreshField.setText(String.valueOf(ViewProperties.getTimerRefresh()));

        int nMax = ViewProperties.getMaxMembers();
        checkReadAll.setSelection((nMax<=0) || (nMax==Integer.MAX_VALUE));

        startMemberField.setText(String.valueOf(ViewProperties.getStartMembers()));

        maxMemberField.setText(String.valueOf(ViewProperties.getMaxMembers()));
    }

    /**
     * Creates and returns the SWT control for the customized body of this
     * preference page under the given parent composite.
     *
     * @param parent
     *         the parent composite
     *
     * @return the new control
     */
    @Override
    protected Control createContents(Composite parent) {
        shell = parent.getShell();
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());

        org.eclipse.swt.widgets.Group workingDirectoryGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        workingDirectoryGroup.setLayout(new GridLayout(3, false));
        workingDirectoryGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        workingDirectoryGroup.setFont(curFont);
        workingDirectoryGroup.setText("Default Working Directory");

        checkCurrentUserDir = new Button(workingDirectoryGroup, SWT.CHECK);
        checkCurrentUserDir.setFont(curFont);
        checkCurrentUserDir.setText("\"User Work\" or");
        checkCurrentUserDir.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        checkCurrentUserDir.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isCheckCurrentUserDirSelected = checkCurrentUserDir.getSelection();
                if (isCheckCurrentUserDirSelected)
                    checkUserHomeDir.setSelection(false);
                workField.setEnabled(!isCheckCurrentUserDirSelected);
                currentDirButton.setEnabled(!isCheckCurrentUserDirSelected);
            }
        });

        checkUserHomeDir = new Button(workingDirectoryGroup, SWT.CHECK);
        checkUserHomeDir.setFont(curFont);
        checkUserHomeDir.setText("\"User Home\" or");
        checkUserHomeDir.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        checkUserHomeDir.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isCheckUserHomeDirSelected = checkUserHomeDir.getSelection();
                if (isCheckUserHomeDirSelected)
                    checkCurrentUserDir.setSelection(false);
                workField.setEnabled(!isCheckUserHomeDirSelected);
                currentDirButton.setEnabled(!isCheckUserHomeDirSelected);
            }
        });

        workField = new Text(workingDirectoryGroup, SWT.SINGLE | SWT.BORDER);
        workField.setFont(curFont);
        workField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        currentDirButton = new Button(workingDirectoryGroup, SWT.PUSH);
        currentDirButton.setFont(curFont);
        currentDirButton.setText("Browse...");
        currentDirButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        currentDirButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final DirectoryDialog dChooser = new DirectoryDialog(shell);
                dChooser.setFilterPath(workDir);
                dChooser.setText("Select a Directory");

                String dir = dChooser.open();

                if(dir == null) return;

                workField.setText(dir);
            }
        });

        org.eclipse.swt.widgets.Group helpDocumentGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        helpDocumentGroup.setLayout(new GridLayout(3, false));
        helpDocumentGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        helpDocumentGroup.setFont(curFont);
        helpDocumentGroup.setText("Help Document");

        Label label = new Label(helpDocumentGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("User's Guide:  ");

        UGField = new Text(helpDocumentGroup, SWT.SINGLE | SWT.BORDER);
        UGField.setFont(curFont);
        UGField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Button browseButton = new Button(helpDocumentGroup, SWT.PUSH);
        browseButton.setFont(curFont);
        browseButton.setText("Browse...");
        browseButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
                fChooser.setFilterPath(rootDir);
                fChooser.setFilterExtensions(new String[] {"*"});
                fChooser.setFilterNames(new String[] {"All Files"});
                fChooser.setFilterIndex(0);

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

        org.eclipse.swt.widgets.Group fileAccessModeGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        fileAccessModeGroup.setLayout(new GridLayout(2, true));
        fileAccessModeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        fileAccessModeGroup.setFont(curFont);
        fileAccessModeGroup.setText("Default File Access Mode");

        checkReadOnly = new Button(fileAccessModeGroup, SWT.RADIO);
        checkReadOnly.setFont(curFont);
        checkReadOnly.setText("Read Only");
        checkReadOnly.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));

        rwButton = new Button(fileAccessModeGroup, SWT.RADIO);
        rwButton.setFont(curFont);
        rwButton.setText("Read/Write");
        rwButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));

        org.eclipse.swt.widgets.Group textFontGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        textFontGroup.setLayout(new GridLayout(4, false));
        textFontGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        textFontGroup.setFont(curFont);
        textFontGroup.setText("Text Font");

        label = new Label(textFontGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Font Size: ");

        String[] fontSizeChoices = { "8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "48" };
        fontSizeChoice = new Combo(textFontGroup, SWT.SINGLE | SWT.READ_ONLY);
        fontSizeChoice.setFont(curFont);
        fontSizeChoice.setItems(fontSizeChoices);
        fontSizeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        label = new Label(textFontGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Font Type: ");

        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        fontTypeChoice = new Combo(textFontGroup, SWT.SINGLE | SWT.READ_ONLY);
        fontTypeChoice.setFont(curFont);
        fontTypeChoice.setItems(fontNames);
        fontTypeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        org.eclipse.swt.widgets.Group imageGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        imageGroup.setLayout(new GridLayout(5, false));
        imageGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        imageGroup.setFont(curFont);
        imageGroup.setText("Image");

        helpButton = new Button(imageGroup, SWT.PUSH);
        helpButton.setImage(ViewProperties.getHelpIcon());
        helpButton.setToolTipText("Help on Auto Contrast");
        helpButton.addSelectionListener(new SelectionAdapter() {
            @Override
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

                Tools.showInformation(getShell(), "Help", msg);
            }
        });

        checkAutoContrast = new Button(imageGroup, SWT.CHECK);
        checkAutoContrast.setFont(curFont);
        checkAutoContrast.setText("Autogain Image Contrast");
        checkAutoContrast.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        checkShowValues = new Button(imageGroup, SWT.CHECK);
        checkShowValues.setFont(curFont);
        checkShowValues.setText("Show Values");
        checkShowValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        label = new Label(imageGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Image Origin: ");

        imageOriginChoice = new Combo(imageGroup, SWT.SINGLE | SWT.READ_ONLY);
        imageOriginChoice.setFont(curFont);
        imageOriginChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        org.eclipse.swt.widgets.Group dataGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        dataGroup.setLayout(new GridLayout(4, false));
        dataGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        dataGroup.setFont(curFont);
        dataGroup.setText("Data");

        label = new Label(dataGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Index Base: ");

        String[] indexBaseChoices = { "0-based", "1-based" };
        indexBaseChoice = new Combo(dataGroup, SWT.SINGLE | SWT.READ_ONLY);
        indexBaseChoice.setFont(curFont);
        indexBaseChoice.setItems(indexBaseChoices);
        indexBaseChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Label delimLabel = new Label(dataGroup, SWT.RIGHT);
        delimLabel.setFont(curFont);
        delimLabel.setText("Data Delimiter: ");
        delimLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        delimiterChoice = new Combo(dataGroup, SWT.SINGLE | SWT.READ_ONLY);
        delimiterChoice.setFont(curFont);
        delimiterChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Label timerRefreshLabel = new Label(dataGroup, SWT.RIGHT);
        timerRefreshLabel.setFont(curFont);
        timerRefreshLabel.setText("Timer Refresh (ms): ");

        timerRefreshField = new Text(dataGroup, SWT.SINGLE | SWT.BORDER);
        timerRefreshField.setFont(curFont);
        timerRefreshField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        org.eclipse.swt.widgets.Group objectsGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        objectsGroup.setLayout(new GridLayout(5, true));
        objectsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        objectsGroup.setFont(curFont);
        objectsGroup.setText("Objects to Open");

        checkReadAll = new Button(objectsGroup, SWT.CHECK);
        checkReadAll.setFont(curFont);
        checkReadAll.setText("Open All");
        checkReadAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        checkReadAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                startMemberField.setEnabled(!checkReadAll.getSelection());
                maxMemberField.setEnabled(!checkReadAll.getSelection());
            }
        });

        label = new Label(objectsGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Start Member: ");

        startMemberField = new Text(objectsGroup, SWT.SINGLE | SWT.BORDER);
        startMemberField.setFont(curFont);
        startMemberField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        label = new Label(objectsGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Member Count: ");

        maxMemberField = new Text(objectsGroup, SWT.SINGLE | SWT.BORDER);
        maxMemberField.setFont(curFont);
        maxMemberField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        startMemberField.setEnabled(!checkReadAll.getSelection());
        maxMemberField.setEnabled(!checkReadAll.getSelection());

        load();
        // return scroller;
        return composite;
    }
}
