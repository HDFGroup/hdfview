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

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * NewDatasetDialog shows a message dialog requesting user input for creating a
 * new HDF4/5 dataset.
 *
 * @author Jordan T. Henderson
 * @version 2.4 12/31/2015
 */
public class NewDatasetDialog extends Dialog {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewDatasetDialog.class);

    private Shell             shell;

    private Font              curFont;

    private String            maxSize;

    private Text              nameField, currentSizeField, chunkSizeField,
                              stringLengthField, fillValueField;

    private Combo             parentChoice, classChoice, sizeChoice, endianChoice,
                              rankChoice, compressionLevel;

    private Button            checkUnsigned, checkCompression, checkFillValue;

    private Button            checkContiguous, checkChunked;

    private boolean           isH5;

    /** A list of current groups */
    private List<Group>       groupList;

    private List<?>           objList;

    private HObject           newObject;
    private Group             parentGroup;

    private FileFormat        fileFormat;

    private final DataView    dataView;

    /**
     * Constructs a NewDatasetDialog with specified list of possible parent
     * groups.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    public NewDatasetDialog(Shell parent, Group pGroup, List<?> objs) {
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

        parentGroup = pGroup;
        newObject = null;
        dataView = null;

        objList = objs;

        fileFormat = pGroup.getFileFormat();
        isH5 = pGroup.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
    }

    /**
     * Constructs a NewDatasetDialog with specified list of possible parent
     * groups.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     * @param observer
     *            the Dataview attached to this dialog.
     */
    public NewDatasetDialog(Shell parent, Group pGroup, List<?> objs, DataView observer) {
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

        parentGroup = pGroup;
        newObject = null;
        dataView = observer;

        objList = objs;

        fileFormat = pGroup.getFileFormat();
        isH5 = pGroup.getFileFormat().isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
    }

    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Dataset...");
        shell.setImage(ViewProperties.getHdfIcon());
        shell.setLayout(new GridLayout(1, true));


        // Create Dataset name / Parent Group region
        Composite fieldComposite = new Composite(shell, SWT.NONE);
        fieldComposite.setLayout(new GridLayout(2, false));
        fieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label datasetNameLabel = new Label(fieldComposite, SWT.LEFT);
        datasetNameLabel.setFont(curFont);
        datasetNameLabel.setText("Dataset name: ");

        nameField = new Text(fieldComposite, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.minimumWidth = 250;
        nameField.setLayoutData(data);

        Label parentGroupLabel = new Label(fieldComposite, SWT.LEFT);
        parentGroupLabel.setFont(curFont);
        parentGroupLabel.setText("Parent group: ");

        parentChoice = new Combo(fieldComposite, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        parentChoice.setFont(curFont);
        parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        parentChoice.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                parentGroup = groupList.get(parentChoice.getSelectionIndex());
            }
        });

        groupList = new Vector<Group>();
        Object obj = null;
        Iterator<?> iterator = objList.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(g);
                if (g.isRoot()) {
                    parentChoice.add(HObject.separator);
                }
                else {
                    parentChoice.add(g.getPath() + g.getName() + HObject.separator);
                }
            }
        }

        if (parentGroup.isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.separator));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentGroup.getPath() + parentGroup.getName() + HObject.separator));
        }

        // Create New Dataset from scratch
        if (dataView == null) {
            // Create Datatype region
            org.eclipse.swt.widgets.Group datatypeGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
            datatypeGroup.setFont(curFont);
            datatypeGroup.setText("Datatype");
            datatypeGroup.setLayout(new GridLayout(4, true));
            datatypeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            Label label = new Label(datatypeGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Datatype Class");

            label = new Label(datatypeGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Size (bits)");

            label = new Label(datatypeGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Byte Ordering");

            checkUnsigned = new Button(datatypeGroup, SWT.CHECK);
            checkUnsigned.setFont(curFont);
            checkUnsigned.setText("Unsigned");

            classChoice = new Combo(datatypeGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            classChoice.setFont(curFont);
            classChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            classChoice.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    int idx = classChoice.getSelectionIndex();
                    sizeChoice.select(0);
                    endianChoice.select(0);
                    stringLengthField.setEnabled(false);

                    if ((idx == 0) || (idx == 6)) { // INTEGER
                        sizeChoice.setEnabled(true);
                        endianChoice.setEnabled(isH5);
                        checkUnsigned.setEnabled(true);

                        if (sizeChoice.getItemCount() == 3) {
                            sizeChoice.remove("32");
                            sizeChoice.remove("64");
                            sizeChoice.add("8");
                            sizeChoice.add("16");
                            sizeChoice.add("32");
                            sizeChoice.add("64");
                        }
                    }
                    else if ((idx == 1) || (idx == 7)) { // FLOAT
                        sizeChoice.setEnabled(true);
                        endianChoice.setEnabled(isH5);
                        checkUnsigned.setEnabled(false);

                        if (sizeChoice.getItemCount() == 5) {
                            sizeChoice.remove("16");
                            sizeChoice.remove("8");
                        }
                    }
                    else if (idx == 2) { // CHAR
                        sizeChoice.setEnabled(false);
                        endianChoice.setEnabled(isH5);
                        checkUnsigned.setEnabled(true);
                    }
                    else if (idx == 3) { // STRING
                        sizeChoice.setEnabled(false);
                        endianChoice.setEnabled(false);
                        checkUnsigned.setEnabled(false);
                        stringLengthField.setEnabled(true);
                        stringLengthField.setText("String length");
                    }
                    else if (idx == 4) { // REFERENCE
                        sizeChoice.setEnabled(false);
                        endianChoice.setEnabled(false);
                        checkUnsigned.setEnabled(false);
                        stringLengthField.setEnabled(false);
                    }
                    else if (idx == 5) { // ENUM
                        sizeChoice.setEnabled(true);
                        checkUnsigned.setEnabled(true);
                        stringLengthField.setEnabled(true);
                        stringLengthField.setText("R=0,G=1,B=2,...");
                    }
                    else if (idx == 8) {
                        sizeChoice.setEnabled(false);
                        endianChoice.setEnabled(false);
                        checkUnsigned.setEnabled(false);
                        stringLengthField.setEnabled(false);
                    }
                }
            });

            classChoice.add("INTEGER");
            classChoice.add("FLOAT");
            classChoice.add("CHAR");

            if(isH5) {
                classChoice.add("STRING");
                classChoice.add("REFERENCE");
                classChoice.add("ENUM");
                classChoice.add("VLEN_INTEGER");
                classChoice.add("VLEN_FLOAT");
                classChoice.add("VLEN_STRING");
            }

            sizeChoice = new Combo(datatypeGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            sizeChoice.setFont(curFont);
            sizeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            sizeChoice.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (classChoice.getSelectionIndex() == 0) {
                        checkUnsigned.setEnabled(true);
                    }
                }
            });

            if(isH5) {
                sizeChoice.add("NATIVE");
            } else {
                sizeChoice.add("DEFAULT");
            }

            sizeChoice.add("8");
            sizeChoice.add("16");
            sizeChoice.add("32");
            sizeChoice.add("64");

            endianChoice = new Combo(datatypeGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            endianChoice.setFont(curFont);
            endianChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            endianChoice.setEnabled(isH5);

            if(isH5) {
                endianChoice.add("NATIVE");
                endianChoice.add("LITTLE ENDIAN");
                endianChoice.add("BIG ENDIAN");
            } else {
                endianChoice.add("DEFAULT");
            }

            stringLengthField = new Text(datatypeGroup, SWT.SINGLE | SWT.BORDER);
            stringLengthField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            stringLengthField.setFont(curFont);
            stringLengthField.setText("String Length");
            stringLengthField.setEnabled(false);

            classChoice.select(0);
            sizeChoice.select(0);
            endianChoice.select(0);


            // Create Dataspace region
            org.eclipse.swt.widgets.Group dataspaceGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
            dataspaceGroup.setFont(curFont);
            dataspaceGroup.setText("Dataspace");
            dataspaceGroup.setLayout(new GridLayout(3, true));
            dataspaceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            label = new Label(dataspaceGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("No. of dimensions");

            label = new Label(dataspaceGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Current size");

            // Dummy label
            label = new Label(dataspaceGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("");

            rankChoice = new Combo(dataspaceGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            rankChoice.setFont(curFont);
            rankChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            rankChoice.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    int rank = rankChoice.getSelectionIndex() + 1;
                    String currentSizeStr = "1";

                    for (int i = 1; i < rank; i++) {
                        currentSizeStr += " x 1";
                    }

                    currentSizeField.setText(currentSizeStr);

                    String currentStr = currentSizeField.getText();
                    int idx = currentStr.lastIndexOf("x");
                    String chunkStr = "1";

                    if (rank <= 1) {
                        chunkStr = currentStr;
                    }
                    else {
                        for (int i = 1; i < rank - 1; i++) {
                            chunkStr += " x 1";
                        }
                        if (idx > 0) {
                            chunkStr += " x " + currentStr.substring(idx + 1);
                        }
                    }

                    chunkSizeField.setText(chunkStr);
                }
            });

            for (int i = 1; i < 33; i++) {
                rankChoice.add(String.valueOf(i));
            }
            rankChoice.select(1);

            currentSizeField = new Text(dataspaceGroup, SWT.SINGLE | SWT.BORDER);
            currentSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            currentSizeField.setFont(curFont);
            currentSizeField.setText("1 x 1");

            Button setMaxSizeButton = new Button(dataspaceGroup, SWT.PUSH);
            setMaxSizeButton.setFont(curFont);
            setMaxSizeButton.setText("Set Max Size");
            setMaxSizeButton.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));
            setMaxSizeButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (maxSize == null || maxSize.length() < 1)
                        maxSize = currentSizeField.getText();

                    String msg = new InputDialog(shell, "Set Max Size", "Enter max dimension sizes. \n"
                            + "Use \"unlimited\" for unlimited dimension size.\n\n" + "For example,\n" + "    200 x 100\n"
                            + "    100 x unlimited\n\n", maxSize).open();

                    if (msg == null || msg.length() < 1)
                        maxSize = currentSizeField.getText();
                    else
                        maxSize = msg;

                    checkMaxSize();
                }
            });


            // Create Storage Properties region
            org.eclipse.swt.widgets.Group storagePropertiesGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
            storagePropertiesGroup.setFont(curFont);
            storagePropertiesGroup.setText("Storage Properties");
            storagePropertiesGroup.setLayout(new GridLayout(5, true));
            storagePropertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            label = new Label(storagePropertiesGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Storage layout: ");

            checkContiguous = new Button(storagePropertiesGroup, SWT.RADIO);
            checkContiguous.setFont(curFont);
            checkContiguous.setText("Contiguous");
            checkContiguous.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
            checkContiguous.setSelection(true);
            checkContiguous.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    chunkSizeField.setEnabled(false);
                }
            });

            // Dummy label
            label = new Label(storagePropertiesGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("");
            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            checkChunked = new Button(storagePropertiesGroup, SWT.RADIO);
            checkChunked.setFont(curFont);
            checkChunked.setText("Chunked (size) ");
            checkChunked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
            checkChunked.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    chunkSizeField.setEnabled(true);
                    String chunkStr = "";
                    StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
                    int rank = rankChoice.getSelectionIndex() + 1;
                    while (st.hasMoreTokens()) {
                        long l = Math.max(1, Long.valueOf(st.nextToken().trim()) / (2 * rank));
                        chunkStr += String.valueOf(l) + "x";
                    }
                    chunkStr = chunkStr.substring(0, chunkStr.lastIndexOf('x'));
                    chunkSizeField.setText(chunkStr);
                }
            });

            chunkSizeField = new Text(storagePropertiesGroup, SWT.SINGLE | SWT.BORDER);
            chunkSizeField.setFont(curFont);
            chunkSizeField.setText("1 x 1");
            chunkSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            chunkSizeField.setEnabled(false);

            label = new Label(storagePropertiesGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Compression: ");

            checkCompression = new Button(storagePropertiesGroup, SWT.CHECK);
            checkCompression.setFont(curFont);
            checkCompression.setText("gzip (level) ");
            checkCompression.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
            checkCompression.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    boolean isCompressed = checkCompression.getSelection();

                    if (isCompressed && isH5) {
                        if (!checkChunked.getSelection()) {
                            String currentStr = currentSizeField.getText();
                            int idx = currentStr.lastIndexOf("x");
                            String chunkStr = "1";

                            int rank = rankChoice.getSelectionIndex() + 1;
                            if (rank <= 1) {
                                chunkStr = currentStr;
                            }
                            else {
                                for (int i = 1; i < rank - 1; i++) {
                                    chunkStr += " x 1";
                                }
                                if (idx > 0) {
                                    chunkStr += " x " + currentStr.substring(idx + 1);
                                }
                            }

                            chunkSizeField.setText(chunkStr);
                        }
                        compressionLevel.setEnabled(true);
                        checkContiguous.setEnabled(false);
                        checkContiguous.setSelection(false);
                        checkChunked.setSelection(true);
                        chunkSizeField.setEnabled(true);
                    }
                    else {
                        compressionLevel.setEnabled(isCompressed);
                        checkContiguous.setEnabled(true);
                    }
                }
            });

            compressionLevel = new Combo(storagePropertiesGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            compressionLevel.setFont(curFont);
            compressionLevel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            for (int i = 0; i < 10; i++) {
                compressionLevel.add(String.valueOf(i));
            }
            compressionLevel.select(6);
            compressionLevel.setEnabled(false);

            if(isH5) {
                checkFillValue = new Button(storagePropertiesGroup, SWT.CHECK);
                checkFillValue.setFont(curFont);
                checkFillValue.setText("Fill Value");
                checkFillValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
                checkFillValue.setSelection(false);
                checkFillValue.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        fillValueField.setEnabled(checkFillValue.getSelection());
                    }
                });

                fillValueField = new Text(storagePropertiesGroup, SWT.SINGLE | SWT.BORDER);
                fillValueField.setFont(curFont);
                fillValueField.setText("0");
                fillValueField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                fillValueField.setEnabled(false);
            } else {
                // Add two dummy labels
                label = new Label(storagePropertiesGroup, SWT.LEFT);
                label.setFont(curFont);
                label.setText("");
                label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

                label = new Label(storagePropertiesGroup, SWT.LEFT);
                label.setFont(curFont);
                label.setText("");
                label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            }
        }


        // Create Ok/Cancel/Help button region
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout((dataView == null) ? 3 : 2, false));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("   &OK   ");
        okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        okButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (dataView instanceof TableView) {
                    newObject = createFromTable();
                }
                else if (dataView instanceof ImageView) {
                    newObject = createFromImage();
                }
                else if (dataView == null) {
                    newObject = createFromScratch();
                }

                if (newObject != null) {
                    shell.dispose();
                }
            }
        });

        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setFont(curFont);
        cancelButton.setText(" &Cancel ");
        cancelButton.setLayoutData(new GridData((dataView == null) ? SWT.CENTER : SWT.BEGINNING, SWT.FILL,
                (dataView == null) ? false : true, false));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                newObject = null;
                shell.dispose();
                ((Vector<Group>) groupList).setSize(0);
            }
        });

        if (dataView == null) {
            Button helpButton = new Button(buttonComposite, SWT.PUSH);
            helpButton.setFont(curFont);
            helpButton.setText(" &Help ");
            helpButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            helpButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    new HelpDialog(shell).open();
                }
            });
        }

        shell.pack();

        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null) curFont.dispose();
            }
        });

        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Rectangle parentBounds = parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

        shell.open();

        Display display = shell.getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
    }

    /** Check if the max size is valid */
    private void checkMaxSize() {
        boolean isChunkNeeded = false;
        String dimStr = currentSizeField.getText();
        String maxSizeStr = maxSize;
        StringTokenizer stMax = new StringTokenizer(maxSizeStr, "x");
        StringTokenizer stDim = new StringTokenizer(dimStr, "x");

        if (stMax.countTokens() != stDim.countTokens()) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Wrong number of values in the max dimension size " + maxSize, shell.getText());
            maxSize = null;
            return;
        }

        int rank = stDim.countTokens();
        long max = 0, dim = 0;
        long[] maxdims = new long[rank];
        for (int i = 0; i < rank; i++) {
            String token = stMax.nextToken().trim();

            token = token.toLowerCase();
            if (token.startsWith("u")) {
                max = -1;
                isChunkNeeded = true;
            }
            else {
                try {
                    max = Long.parseLong(token);
                }
                catch (NumberFormatException ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Invalid max dimension size: " + maxSize, shell.getText());
                    maxSize = null;
                    return;
                }
            }

            token = stDim.nextToken().trim();
            try {
                dim = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Invalid dimension size: " + dimStr, shell.getText());
                return;
            }

            if (max != -1 && max < dim) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Invalid max dimension size: " + maxSize, shell.getText());
                maxSize = null;
                return;
            }
            else if (max > dim) {
                isChunkNeeded = true;
            }

            maxdims[i] = max;
        } // for (int i = 0; i < rank; i++)

        if (isH5) {
            if (isChunkNeeded && !checkChunked.getSelection()) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Chunking is required for the max dimensions of " + maxSize, shell.getText());
                checkChunked.setSelection(true);
            }
        }
        else {
            for (int i = 1; i < rank; i++) {
                if (maxdims[i] <= 0) {
                    maxSize = currentSizeField.getText();
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Only dim[0] can be unlimited." + maxSize, shell.getText());
                    return;
                }
            }
        }
    }

    private HObject createFromScratch() {
        String name = null;
        Group pgroup = null;
        boolean isVLen = false;
        int rank = -1, gzip = -1, tclass = -1, tsize = -1, torder = -1, tsign = -1;
        long dims[], maxdims[] = null, chunks[] = null;

        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Dataset name is not specified.", shell.getText());
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Dataset name cannot contain path.", shell.getText());
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Parent group is null.", shell.getText());
            return null;
        }

        // set datatype class
        int idx = classChoice.getSelectionIndex();
        if (idx == 0) {
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.getSelection()) {
                tsign = Datatype.SIGN_NONE;
            }
        }
        else if (idx == 1) {
            tclass = Datatype.CLASS_FLOAT;
        }
        else if (idx == 2) {
            tclass = Datatype.CLASS_CHAR;
            if (checkUnsigned.getSelection()) {
                tsign = Datatype.SIGN_NONE;
            }
        }
        else if (idx == 3) {
            tclass = Datatype.CLASS_STRING;
        }
        else if (idx == 4) {
            tclass = Datatype.CLASS_REFERENCE;
        }
        else if (idx == 5) {
            tclass = Datatype.CLASS_ENUM;
        }
        else if (idx == 6) {
            isVLen = true;
            tclass = Datatype.CLASS_INTEGER;
            if (checkUnsigned.getSelection()) {
                tsign = Datatype.SIGN_NONE;
            }
        }
        else if (idx == 7) {
            isVLen = true;
            tclass = Datatype.CLASS_FLOAT;
        }
        else if (idx == 8) {
            isVLen = true;
            tclass = Datatype.CLASS_STRING;
        }

        // set datatype size/order
        idx = sizeChoice.getSelectionIndex();
        if (tclass == Datatype.CLASS_STRING) {
            if (isVLen) {
                tsize = -1;
            }
            else {
                int stringLength = 0;
                try {
                    stringLength = Integer.parseInt(stringLengthField.getText());
                }
                catch (NumberFormatException ex) {
                    stringLength = -1;
                }

                if (stringLength <= 0) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Invalid string length: " + stringLengthField.getText(), shell.getText());
                    return null;
                }
                tsize = stringLength;
            }
        }
        else if (tclass == Datatype.CLASS_ENUM) {
            String enumStr = stringLengthField.getText();
            if ((enumStr == null) || (enumStr.length() < 1) || enumStr.endsWith("...")) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Invalid member values: " + stringLengthField.getText(), shell.getText());
                return null;
            }
        }
        else if (tclass == Datatype.CLASS_REFERENCE) {
            tsize = 1;
        }
        else if (idx == 0) {
            tsize = Datatype.NATIVE;
        }
        else if (tclass == Datatype.CLASS_FLOAT) {
            tsize = idx * 4;
        }
        else {
            tsize = 1 << (idx - 1);
        }

        if ((tsize == 8) && !isH5 && (tclass == Datatype.CLASS_INTEGER)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "HDF4 does not support 64-bit integer.", shell.getText());
            return null;
        }

        // set order
        idx = endianChoice.getSelectionIndex();
        if (idx == 0) {
            torder = Datatype.NATIVE;
        }
        else if (idx == 1) {
            torder = Datatype.ORDER_LE;
        }
        else {
            torder = Datatype.ORDER_BE;
        }

        rank = rankChoice.getSelectionIndex() + 1;
        StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Number of values in the current dimension size is less than " + rank, shell.getText());
            return null;
        }

        long l = 0;
        dims = new long[rank];
        String token = null;
        for (int i = 0; i < rank; i++) {
            token = st.nextToken().trim();
            try {
                l = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Invalid dimension size: " + currentSizeField.getText(), shell.getText());
                return null;
            }

            if (l <= 0) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Dimension size must be greater than zero.", shell.getText());
                return null;
            }

            dims[i] = l;
        }

        String maxSizeStr = maxSize;
        if (maxSizeStr != null && maxSizeStr.length() > 1) {
            st = new StringTokenizer(maxSizeStr, "x");
            if (st.countTokens() < rank) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Number of values in the max dimension size is less than " + rank, shell.getText());
                return null;
            }

            l = 0;
            maxdims = new long[rank];
            for (int i = 0; i < rank; i++) {
                token = st.nextToken().trim();

                token = token.toLowerCase();
                if (token.startsWith("u"))
                    l = -1;
                else {
                    try {
                        l = Long.parseLong(token);
                    }
                    catch (NumberFormatException ex) {
                        shell.getDisplay().beep();
                        Tools.showError(shell, "Invalid max dimension size: " + maxSize, shell.getText());
                        return null;
                    }
                }

                if (l < -1) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Dimension size cannot be less than -1.", shell.getText());
                    return null;
                }
                else if (l == 0) {
                    l = dims[i];
                }

                maxdims[i] = l;
            }
        }

        chunks = null;
        if (checkChunked.getSelection()) {
            st = new StringTokenizer(chunkSizeField.getText(), "x");
            if (st.countTokens() < rank) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Number of values in the chunk size is less than " + rank, shell.getText());
                return null;
            }

            l = 0;
            chunks = new long[rank];
            for (int i = 0; i < rank; i++) {
                token = st.nextToken().trim();
                try {
                    l = Long.parseLong(token);
                }
                catch (NumberFormatException ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Invalid chunk dimension size: " + chunkSizeField.getText(), shell.getText());
                    return null;
                }

                if (l < 1) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Chunk size cannot be less than 1.", shell.getText());
                    return null;
                }

                chunks[i] = l;
            } // for (int i=0; i<rank; i++)

            long tchunksize = 1, tdimsize = 1;
            for (int i = 0; i < rank; i++) {
                tchunksize *= chunks[i];
                tdimsize *= dims[i];
            }

            if (tchunksize >= tdimsize) {
                shell.getDisplay().beep();
                MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                confirm.setText(shell.getText());
                confirm.setMessage("Chunk size is equal/greater than the current size. "
                        + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() + "?");
                if(confirm.open() == SWT.NO) {
                    return null;
                }
            }

            if (tchunksize == 1) {
                shell.getDisplay().beep();
                MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                confirm.setText(shell.getText());
                confirm.setMessage("Chunk size is one, which may cause large memory overhead for large dataset."
                        + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() + "?");
                if(confirm.open() == SWT.NO) {
                    return null;
                }
            }
        } // if (checkChunked.isSelected())

        if (checkCompression.getSelection()) {
            gzip = compressionLevel.getSelectionIndex();
        }
        else {
            gzip = 0;
        }

        HObject obj = null;
        try {
            Datatype basedatatype = null;
            if (isVLen) {
                basedatatype = fileFormat.createDatatype(tclass, tsize, torder, tsign);
                tclass = Datatype.CLASS_VLEN;
            }
            Datatype datatype = fileFormat.createDatatype(tclass, tsize, torder, tsign, basedatatype);
            if (tclass == Datatype.CLASS_ENUM) {
                datatype.setEnumMembers(stringLengthField.getText());
            }
            String fillValue = null;

            if (fillValueField != null) {
                if (fillValueField.isEnabled()) fillValue = fillValueField.getText();
            }

            obj = fileFormat.createScalarDS(name, pgroup, datatype, dims, maxdims, chunks, gzip, fillValue, null);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, ex.getMessage(), shell.getText());
            return null;
        }

        return obj;
    }

    private HObject createFromTable() {
        HObject obj = null;

        String name = null;
        Group pgroup = null;

        name = nameField.getText();
        if (name == null || name.length() == 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Dataset name is not specified.", shell.getText());
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Dataset name cannot contain path.", shell.getText());
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectionIndex());
        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Parent group is null.", shell.getText());
            return null;
        }

        TableView tableView = (TableView) dataView;
        Object theData = tableView.getSelectedData();
        if (theData == null) {
            return null;
        }

        int w = tableView.getSelectedColumnCount();
        int h = tableView.getSelectedRowCount();
        Dataset dataset = (Dataset) tableView.getDataObject();
        if (dataset instanceof ScalarDS) {
            ScalarDS sd = (ScalarDS) dataset;
            if (sd.isUnsigned()) {
                theData = Dataset.convertToUnsignedC(theData, null);
            }
        }

        try {
            long[] dims = { h, w };
            obj = dataset.copy(pgroup, name, dims, theData);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, ex.getMessage(), shell.getText());
            return null;
        }

        return obj;
    }

    private HObject createFromImage() {
        HObject obj = null;
        String name = null;
        Group pgroup = null;

        name = nameField.getText();
        if (name == null || name.length() == 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Dataset name is not specified.", shell.getText());
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Dataset name cannot contain path.", shell.getText());
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectionIndex());
        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Parent group is null.", shell.getText());
            return null;
        }

        ImageView imageView = (ImageView) dataView;
        ScalarDS dataset = (ScalarDS) imageView.getDataObject();
        Object theData = imageView.getSelectedData();

        if (theData == null) {
            return null;
        }

        // in version 2.4, unsigned image data is converted to signed data
        // to write data, the data needs to be converted back to unsigned.
        if (dataset.isUnsigned()) {
            theData = Dataset.convertToUnsignedC(theData, null);
        }

        int w = imageView.getSelectedArea().width;
        int h = imageView.getSelectedArea().height;

        try {
            long[] dims = null;
            if (isH5) {
                if (imageView.isTrueColor()) {
                    dims = new long[3];
                    if (imageView.isPlaneInterlace()) {
                        dims[0] = 3;
                        dims[1] = h;
                        dims[2] = w;
                    }
                    else {
                        dims[0] = h;
                        dims[1] = w;
                        dims[2] = 3;
                    }
                }
                else {
                    dims = new long[2];
                    dims[0] = h;
                    dims[1] = w;
                }
            }
            else {
                dims = new long[2];
                dims[0] = w;
                dims[1] = h;
            }

            obj = dataset.copy(pgroup, name, dims, theData);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, ex.getMessage(), shell.getText());
            return null;
        }

        return obj;
    }

    private class HelpDialog extends Dialog {
        private Shell helpShell;

        public HelpDialog(Shell parent) {
            super(parent, SWT.APPLICATION_MODAL);
        }

        public void open() {
            Shell parent = getParent();
            helpShell = new Shell(parent, SWT.TITLE | SWT.CLOSE |
                    SWT.RESIZE | SWT.BORDER | SWT.APPLICATION_MODAL);
            shell.setFont(curFont);
            helpShell.setText("Create New Dataset");
            helpShell.setImage(ViewProperties.getHdfIcon());
            helpShell.setLayout(new GridLayout(1, true));

            // Try to create a Browser on platforms that support it
            try {
                Browser browser = new Browser(helpShell, SWT.NONE);
                browser.setFont(curFont);
                browser.setBounds(0, 0, 500, 500);
                browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

                if (ClassLoader.getSystemResource("hdf/view/HDFView.class").toString().startsWith("jar")) {
                    // Attempt to load HTML help file from jar
                    try {
                        InputStream in = getClass().getClassLoader().getResourceAsStream("hdf/view/NewDatasetHelp.html");
                        Scanner scan = new Scanner(in);
                        StringBuffer buffer = new StringBuffer();
                        while(scan.hasNextLine()) {
                            buffer.append(scan.nextLine());
                        }

                        browser.setText(buffer.toString());

                        scan.close();
                        in.close();
                    }
                    catch (Exception e) {
                        StringBuffer buff = new StringBuffer();
                        buff.append("<html>");
                        buff.append("<body>");
                        buff.append("ERROR: cannot load help information.");
                        buff.append("</body>");
                        buff.append("</html>");
                        browser.setText(buff.toString(), true);
                    }
                } else {
                    try {
                        URL url = null, url2 = null, url3 = null;
                        String rootPath = ViewProperties.getViewRoot();

                        try {
                            url = new URL("file://" + rootPath + "/HDFView.jar");
                        }
                        catch (java.net.MalformedURLException mfu) {
                            log.debug("help information:", mfu);
                        }

                        try {
                            url2 = new URL("file://" + rootPath + "/");
                        }
                        catch (java.net.MalformedURLException mfu) {
                            log.debug("help information:", mfu);
                        }

                        try {
                            url3 = new URL("file://" + rootPath + "/src/");
                        }
                        catch (java.net.MalformedURLException mfu) {
                            log.debug("help information:", mfu);
                        }

                        URL uu[] = { url, url2, url3 };
                        URLClassLoader cl = new URLClassLoader(uu);
                        URL u = cl.findResource("hdf/view/NewDatasetHelp.html");

                        browser.setUrl(u.toString());

                        cl.close();
                    }
                    catch (Exception e) {
                        StringBuffer buff = new StringBuffer();
                        buff.append("<html>");
                        buff.append("<body>");
                        buff.append("ERROR: cannot load help information.");
                        buff.append("</body>");
                        buff.append("</html>");
                        browser.setText(buff.toString(), true);
                    }
                }

                Button okButton = new Button(helpShell, SWT.PUSH);
                okButton.setFont(curFont);
                okButton.setText("   &OK   ");
                okButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
                okButton.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        helpShell.dispose();
                    }
                });

                helpShell.pack();

                helpShell.setSize(new Point(500, 500));

                Rectangle parentBounds = parent.getBounds();
                Point shellSize = helpShell.getSize();
                helpShell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                                (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

                helpShell.open();

                Display display = parent.getDisplay();
                while(!helpShell.isDisposed()) {
                    if (!display.readAndDispatch())
                        display.sleep();
                }
            }
            catch (Error er) {
                // Try opening help link in external browser if platform
                // doesn't support SWT browser
                Tools.showError(shell,
                        "Platform doesn't support Browser. Opening external link in web browser...",
                        "Browser support");

                //TODO: Add support for launching in external browser
            }
            catch (Exception ex) {
                log.debug("Open New Dataset Help failure: ", ex);
            }
        }
    }

    /** @return the new dataset created. */
    public DataFormat getObject() {
        return newObject;
    }

    /** @return the parent group of the new dataset. */
    public Group getParentGroup() {
        return parentGroup;
    }
}
